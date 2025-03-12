package dev.zwazel.internal.game.utils;

public class GridCell {
    private int x, y;
    private Object currentObstacle;

    public GridCell(int x, int y) {
        this.x = x;
        this.y = y;
        this.currentObstacle = null;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Object getCurrentObstacle() {
        return currentObstacle;
    }

    public void setCurrentObstacle(Object currentObstacle) {
        this.currentObstacle = currentObstacle;
    }
}