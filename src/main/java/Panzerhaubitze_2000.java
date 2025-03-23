import dev.zwazel.GameWorld;
import dev.zwazel.PropertyHandler;
import dev.zwazel.bot.BotInterface;
import dev.zwazel.internal.PublicGameWorld;
import dev.zwazel.internal.config.LobbyConfig;
import dev.zwazel.internal.config.LocalBotConfig;
import dev.zwazel.internal.debug.MapVisualiser;
import dev.zwazel.internal.game.state.ClientState;
import dev.zwazel.internal.game.tank.TankConfig;
import dev.zwazel.internal.game.tank.implemented.SelfPropelledArtillery;
import dev.zwazel.internal.game.transform.Quaternion;
import dev.zwazel.internal.game.utils.Graph;
import dev.zwazel.internal.game.utils.Node;

import java.util.LinkedList;
import java.util.Optional;

public class Panzerhaubitze_2000 implements BotInterface {
    private final PropertyHandler propertyHandler = PropertyHandler.getInstance();
    private MapVisualiser visualiser;

    // New instance variables used for the stepping aiming logic.
    // A negative value indicates that it hasn't been initialized yet.
    private Double currentTargetPitch = null;
    private int shotsFiredAtCurrentAngle = 0;
    private int ticksToWait = 20; // how many ticks to wait before starting to shoot (give the camera some time to get closer)

    public static void main(String[] args) {
        Panzerhaubitze_2000 bot = new Panzerhaubitze_2000();

        // GameWorld.startGame(bot); // This starts the game with a LightTank, and immediately starts the game when connected
        GameWorld.connectToServer(bot); // This connects to the server with a LightTank, but does not immediately start the game
    }

    @Override
    public LocalBotConfig getLocalBotConfig() {
        return LocalBotConfig.builder()
                .debugMode(Optional.ofNullable(propertyHandler.getProperty("debug.mode"))
                        .map(GameWorld.DebugMode::valueOf))
                .botName(propertyHandler.getProperty("bot.name"))
                .tankType(SelfPropelledArtillery.class)
                .serverIp(propertyHandler.getProperty("server.ip"))
                .serverPort(Integer.parseInt(propertyHandler.getProperty("server.port")))
                .lobbyConfig(LobbyConfig.builder()
                        .lobbyName(propertyHandler.getProperty("lobby.name"))
                        .teamName(propertyHandler.getProperty("lobby.name"))
                        .teamName(propertyHandler.getProperty("lobby.team.name"))
                        .mapName(propertyHandler.getProperty("lobby.map.name"))
                        .spawnPoint(Optional.ofNullable(propertyHandler.getProperty("lobby.spawnPoint"))
                                .map(Integer::parseInt))
                        .fillEmptySlots(Boolean.parseBoolean(propertyHandler.getProperty("lobby.fillEmptySlots")))
                        .build()
                )
                .build();
    }

    @Override
    public void setup(PublicGameWorld world) {
        // If in debug, add visualiser
        if (world.isDebug()) {
            // Add visualiser. By pressing space, you can switch between drawing modes.
            visualiser = new MapVisualiser(world);
            visualiser.setDrawingMode(MapVisualiser.DrawingMode.valueOf(propertyHandler.getProperty("debug.visualiser.mode").toUpperCase()));
            world.registerVisualiser(visualiser);
            visualiser.setMaxWindowHeight(1000);
            visualiser.setMaxWindowWidth(1200);
            visualiser.showMap();
        }
    }

    @Override
    public void processTick(PublicGameWorld world) {
        Graph graph = new Graph(world.getGameConfig().mapDefinition(), false);
        LinkedList<Node> path = new LinkedList<>();
        if (visualiser != null) {
            visualiser.setPath(path);
            visualiser.setGraph(graph);
        }

        if (ticksToWait > 0) {
            ticksToWait--;
            return;
        }

        ClientState myClientState = world.getMyState();
        if (myClientState.state() == ClientState.PlayerState.DEAD) {
            System.out.println("I'm dead!");
            return;
        }

        SelfPropelledArtillery tank = (SelfPropelledArtillery) world.getTank();
        TankConfig myTankConfig = tank.getConfig(world);

        // Retrieve turret configuration values.
        double turretMinPitch = myTankConfig.turretMinPitch();
        double turretMaxPitch = myTankConfig.turretMaxPitch();
        double turretPitchRotationSpeed = myTankConfig.turretPitchRotationSpeed();

        // Define steering properties from app.properties with defaults.
        double angleStep = Double.parseDouble(
                propertyHandler.getProperty("turret.angle.step") != null ?
                        propertyHandler.getProperty("turret.angle.step") : "0.1"
        );
        int shotLimit = Integer.parseInt(
                propertyHandler.getProperty("turret.shots.per.angle") != null ?
                        propertyHandler.getProperty("turret.shots.per.angle") : "1"
        );

        // Initialize target pitch if not already done.
        if (currentTargetPitch == null) {
            currentTargetPitch = turretMinPitch;
        }

        Quaternion turretRotation = world.getMyState().transformTurret().getRotation();
        double currentPitch = turretRotation.getPitch();

        System.out.println("Figuring out where to rotate turret to to shoot...");
        double epsilon = 1e-6; // Acceptable error margin
        if (Math.abs(currentTargetPitch - currentPitch) < epsilon) {
            System.out.println("Turret is at target pitch");
            // Shoot if the turret is at the maximum pitch
            if (shotsFiredAtCurrentAngle < shotLimit) {
                if (tank.shoot(world)) {
                    System.out.println("Fired at pitch: " + currentTargetPitch);
                    shotsFiredAtCurrentAngle++;
                }
            } else {
                // Move to the next angle by subtracting as going up is negative pitch.
                System.out.println("Reached shot limit at pitch: " + currentTargetPitch);
                currentTargetPitch -= angleStep;
                shotsFiredAtCurrentAngle = 0;

                // If we have reached the maximum pitch, reset to the minimum pitch.
                if (currentTargetPitch <= turretMaxPitch) {
                    currentTargetPitch = turretMinPitch;
                    System.out.println("Resetting to min pitch");
                } else {
                    System.out.println("next pitch: " + currentTargetPitch);
                }
            }
        } else {
            System.out.println("need to rotate turret");
            double angleDifference = currentTargetPitch - currentPitch;
            double angleToRotate = Math.min(turretPitchRotationSpeed, Math.abs(angleDifference));
            angleToRotate = (angleDifference < 0) ? -angleToRotate : angleToRotate;
            System.out.println("Current pitch: " + currentPitch);
            System.out.println("Target pitch: " + currentTargetPitch);
            System.out.println("Rotating turret by: " + angleToRotate);
            tank.rotateTurretPitch(world, angleToRotate);
        }
        System.out.println("--------------------");
    }
}