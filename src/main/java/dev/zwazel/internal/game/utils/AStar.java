package dev.zwazel.internal.game.utils;

import java.util.*;

public class AStar {
    private final List<Node> openList;
    private final List<Node> closedList;
    private final Grid grid;


    public AStar(List<Node> openList, List<Node> closedList, Grid grid) {
        this.openList = openList;
        this.closedList = closedList;
        this.grid = grid;
    }

}
