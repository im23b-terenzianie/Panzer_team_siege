package dev.zwazel.internal.game.utils;

public class Grid {
    private final GridCell[][] gridCells;
    private final int width;
    private final int height;

    public Grid(int width, int height) {
        this.width = width;
        this.height = height;
        gridCells = new GridCell[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                gridCells[x][y] = new GridCell(x, y);
            }
        }
    }

    public GridCell getCell(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return gridCells[x][y];
        }
        return null;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}