package dev.zwazel.internal.game.utils;

public class Node {
    public int x, y;
    public double g, h, f;
    public Node parent;

    public Node(int x, int y) {
        this.x = x;
        this.y = y;
        this.g = 0;
        this.h = 0;
        this.f = 0;
        this.parent = null;
    }

    public void setCost(double g, double h) {
        this.g = g;
        this.h = h;
        this.f = g + h;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getCost() {
        return f;
    }

    public Node currentNode() {
        return this;
    }
}