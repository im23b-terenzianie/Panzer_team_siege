package dev.zwazel.internal.game;

import dev.zwazel.internal.PublicGameWorld;
import dev.zwazel.internal.game.map.MapDefinition;
import dev.zwazel.internal.game.state.ClientState;
import dev.zwazel.internal.game.state.FlagBaseState;
import dev.zwazel.internal.game.state.FlagGameState;
import dev.zwazel.internal.game.state.ProjectileState;
import dev.zwazel.internal.game.transform.Vec3;
import dev.zwazel.internal.game.utils.Graph;
import dev.zwazel.internal.game.utils.Node;
import dev.zwazel.internal.message.data.GameState;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MapControl extends JPanel {
    private final PublicGameWorld world;
    private final int TANK_RADIUS = 25;
    private final int PROJECTILE_RADIUS = 5;
    private final int FLAG_RADIUS = 10;
    private final int FLAG_BASE_RADIUS = 20;
    private int CELL_SIZE = 50; // Will be scaled by the map size, to fit the window
    private DrawingMode drawingMode = DrawingMode.HEIGHT;
    private LinkedList<Node> path = new LinkedList<>();
    private Graph graph;
    private int maxWindowWidth = 1000;
    private int maxWindowHeight = 1000;
    private Vec3 movetarget = null;
    private Vec3 shoottarget = null;

    public void showMap() {
        int width = ((int) world.getGameConfig().mapDefinition().width() + 1) * CELL_SIZE;
        int height = ((int) world.getGameConfig().mapDefinition().depth() + 1) * CELL_SIZE;

        // scale down cell_size until we don't go over the max window size (1000x1000)
        while (width > maxWindowWidth || height > maxWindowHeight) {
            CELL_SIZE--;
            width = ((int) world.getGameConfig().mapDefinition().width() + 1) * CELL_SIZE;
            height = ((int) world.getGameConfig().mapDefinition().depth() + 1) * CELL_SIZE;
        }

        JFrame frame = new JFrame("Map Visualiser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.getContentPane().add(this);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        // Add key listener to switch drawing modes
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    toggleDrawingMode();
                }
            }
        });

        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                int tileX = e.getX() / CELL_SIZE;
                int tileY = e.getY() / CELL_SIZE;
                Vec3 clickedTile = world.getGameConfig().mapDefinition().getWorldTileCenter(tileX, tileY);

                if (e.getButton() == MouseEvent.BUTTON1) {
                    movetarget = clickedTile;
                    System.out.println(movetarget);
                }else if (e.getButton() == MouseEvent.BUTTON3) {
                    shoottarget = clickedTile;
                    System.out.println("shoottarget: " + shoottarget);
                }

                }

        });
    }

    // Drawing the map
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        MapDefinition mapDefinition = world.getGameConfig().mapDefinition();

        switch (drawingMode) {
            case HEIGHT -> drawHeightMap(g2d, mapDefinition);
            case PATH -> drawPath(g2d, mapDefinition);
            case ENTITIES -> drawEntities(g2d, mapDefinition);
        }
    }

    private void drawEntities(Graphics2D g2d, MapDefinition mapDefinition) {
        // We need to collect all states of the different types of entities
        // tanks, projectiles, flags, flagbases
        // and draw them on the map
        GameState currentState = world.getGameState();
        List<ClientState> tankStates = currentState.clientStates().values().stream().toList();
        List<ProjectileState> projectileStates = currentState.projectileStates().values().stream().toList();
        List<FlagGameState> flagStates = currentState.flagStates().values().stream().toList();
        List<FlagBaseState> flagBaseStates = currentState.flagBaseStates().values().stream().toList();

        // First pass: Draw all tanks, as a blue circle
        for (ClientState tankState : tankStates) {
            Vec3 tankPos = tankState.transformBody().getTranslation();

            // Turn the position from float to int, so it can be drawn. from units to pixels
            int x = (int) (tankPos.getX() * CELL_SIZE) - TANK_RADIUS / 2;
            int y = (int) (tankPos.getZ() * CELL_SIZE) - TANK_RADIUS / 2;

            g2d.setColor(Color.YELLOW);
            g2d.fillOval(
                    x,
                    y,
                    TANK_RADIUS,
                    TANK_RADIUS
            );
        }

        // Second pass: Draw all projectiles, as a red circle
        for (ProjectileState projectileState : projectileStates) {
            Vec3 projectilePos = projectileState.transform().getTranslation();

            // Turn the position from float to int, so it can be drawn. from units to pixels
            int x = (int) (projectilePos.getX() * CELL_SIZE) - PROJECTILE_RADIUS / 2;
            int y = (int) (projectilePos.getZ() * CELL_SIZE) - PROJECTILE_RADIUS / 2;

            g2d.setColor(Color.RED);
            g2d.fillOval(
                    x,
                    y,
                    PROJECTILE_RADIUS,
                    PROJECTILE_RADIUS
            );
        }

        // Third pass: Draw all flag bases, as a yellow circle
        for (FlagBaseState flagBaseState : flagBaseStates) {
            Vec3 flagBasePos = flagBaseState.transform().getTranslation();

            // Turn the position from float to int, so it can be drawn. from units to pixels
            int x = (int) (flagBasePos.getX() * CELL_SIZE) - FLAG_BASE_RADIUS / 2;
            int y = (int) (flagBasePos.getZ() * CELL_SIZE) - FLAG_BASE_RADIUS / 2;

            g2d.setColor(Color.CYAN);
            g2d.fillOval(
                    x,
                    y,
                    FLAG_BASE_RADIUS,
                    FLAG_BASE_RADIUS
            );
        }

        // Fourth pass: Draw all flags, as a green circle
        for (FlagGameState flagState : flagStates) {
            Vec3 flagPos = flagState.transform().getTranslation();

            // Turn the position from float to int, so it can be drawn. from units to pixels
            int x = (int) (flagPos.getX() * CELL_SIZE) - FLAG_RADIUS / 2;
            int y = (int) (flagPos.getZ() * CELL_SIZE) - FLAG_RADIUS / 2;

            g2d.setColor(Color.GREEN);
            g2d.fillOval(
                    x,
                    y,
                    FLAG_RADIUS,
                    FLAG_RADIUS
            );
        }

        // Fifth pass: Draw cell borders
        drawCellBorders(g2d, mapDefinition);
    }

    private void drawHeightMap(Graphics2D g2d, MapDefinition mapDefinition) {
        // Determine min and max heights to normalize cell colors
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int x = 0; x < mapDefinition.width(); x++) {
            for (int y = 0; y < mapDefinition.depth(); y++) {
                float tileHeight = mapDefinition.tiles()[y][x];
                if (tileHeight < min) {
                    min = tileHeight;
                }
                if (tileHeight > max) {
                    max = tileHeight;
                }
            }
        }

        // First Pass: Draw each cell with a color gradient based on its height.
        for (int x = 0; x < mapDefinition.width(); x++) {
            for (int y = 0; y < mapDefinition.depth(); y++) {
                float tileHeight = mapDefinition.tiles()[y][x];
                float normalizedHeight = (tileHeight - min) / (max - min);
                // Draw cell
                Color cellColor = new Color(normalizedHeight, 1 - normalizedHeight, 0);
                g2d.setColor(cellColor);
                g2d.fillRect(
                        x * CELL_SIZE,
                        y * CELL_SIZE,
                        CELL_SIZE,
                        CELL_SIZE
                );
            }
        }

        // Second Pass: Draw tank
        drawTank(g2d, world);

        // Third Pass: Draw heights
        for (int x = 0; x < mapDefinition.width(); x++) {
            for (int y = 0; y < mapDefinition.depth(); y++) {
                float tileHeight = mapDefinition.tiles()[y][x];
                g2d.setColor(Color.BLACK);
                g2d.drawString(
                        String.format("%.2f", tileHeight),
                        x * CELL_SIZE + 5,
                        y * CELL_SIZE + 15
                );
            }
        }

        // Fourth Pass: Draw cell borders
        drawCellBorders(g2d, mapDefinition);
    }

    private void drawPath(Graphics2D g2d, MapDefinition mapDefinition) {
        // First pass: Draw start and end cells
        if (!path.isEmpty()) {
            Node startNode = path.getFirst();
            Node endNode = path.getLast();

            // Draw start cell in green
            g2d.setColor(Color.GREEN);
            g2d.fillRect(
                    startNode.getX() * CELL_SIZE,
                    startNode.getY() * CELL_SIZE,
                    CELL_SIZE,
                    CELL_SIZE
            );

            // Draw end cell in red
            g2d.setColor(Color.RED);
            g2d.fillRect(
                    endNode.getX() * CELL_SIZE,
                    endNode.getY() * CELL_SIZE,
                    CELL_SIZE,
                    CELL_SIZE
            );
        }

        // Second pass: Draw Tank
        drawTank(g2d, world);

        // Third pass: Draw Path
        g2d.setColor(Color.RED);
        for (int i = 0; i < path.size() - 1; i++) {
            Node node1 = path.get(i);
            Node node2 = path.get(i + 1);
            g2d.drawLine(
                    node1.getX() * CELL_SIZE + CELL_SIZE / 2,
                    node1.getY() * CELL_SIZE + CELL_SIZE / 2,
                    node2.getX() * CELL_SIZE + CELL_SIZE / 2,
                    node2.getY() * CELL_SIZE + CELL_SIZE / 2
            );
        }

        // Fourth pass: Draw costs
        if (graph == null) {
            // Draw costs of the path
            drawCost(g2d, path);
        } else {
            // Draw costs of the entire graph
            drawCost(g2d, graph.getNodes());
        }

        // Fifth pass: Draw cell borders
        drawCellBorders(g2d, mapDefinition);
    }

    private void drawCost(Graphics2D g2d, Node[][] nodes) {
        for (Node[] row : nodes) {
            for (Node node : row) {
                drawCost(g2d, node);
            }
        }
    }

    private void drawCost(Graphics2D g2d, LinkedList<Node> nodes) {
        for (Node node : nodes) {
            drawCost(g2d, node);
        }
    }

    private void drawCost(Graphics2D g2d, Node node) {
        String cost = String.format("%.2f", node.getCost());
        if (node.getCost() == Double.MAX_VALUE) {
            cost = "∞";
        }

        g2d.setColor(Color.BLACK);
        g2d.drawString(
                cost,
                node.getX() * CELL_SIZE + 5,
                node.getY() * CELL_SIZE + 15
        );
    }

    private void drawCellBorders(Graphics2D g2d, MapDefinition mapDefinition) {
        for (int x = 0; x < mapDefinition.width(); x++) {
            for (int y = 0; y < mapDefinition.depth(); y++) {
                // Draw cell border
                g2d.setColor(Color.BLACK);
                g2d.drawRect(
                        x * CELL_SIZE,
                        y * CELL_SIZE,
                        CELL_SIZE,
                        CELL_SIZE
                );
            }
        }
    }

    private void drawTank(Graphics2D g2d, PublicGameWorld world) {
        // Draw location of my tank (drawing the cell blue, and the actual position as a point)
        Vec3 myPosition = world.getMyState().transformBody().getTranslation();
        Vec3 closestTile = world.getGameConfig().mapDefinition().getClosestTileFromWorld(myPosition);

        g2d.setColor(Color.BLUE);
        g2d.fillRect(
                (int) closestTile.getX() * CELL_SIZE,
                (int) closestTile.getZ() * CELL_SIZE,
                CELL_SIZE,
                CELL_SIZE
        );

        // Turn the position from float to int, so it can be drawn. from units to pixels
        int x = (int) (myPosition.getX() * CELL_SIZE) - TANK_RADIUS / 2;
        int y = (int) (myPosition.getZ() * CELL_SIZE) - TANK_RADIUS / 2;

        g2d.setColor(Color.ORANGE);
        g2d.fillOval(
                x,
                y,
                TANK_RADIUS,
                TANK_RADIUS
        );
    }

    private void toggleDrawingMode() {
        DrawingMode[] modes = DrawingMode.values();
        int currentIndex = drawingMode.ordinal();
        int nextIndex = (currentIndex + 1) % modes.length;
        drawingMode = modes[nextIndex];
        repaint();
    }

    // Enum for switching drawing modes
    public enum DrawingMode {
        HEIGHT,
        PATH,
        ENTITIES,
    }
}