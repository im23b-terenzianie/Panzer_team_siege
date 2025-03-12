package dev.zwazel.internal.game.utils;

import java.util.*;

public class AStar {
    public List<Node> findPath(Grid grid, Node start, Node end) {
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(Node::getCost));
        Set<Node> closedList = new HashSet<>();

        start.setCost(0, heuristic(start, end));
        openList.add(start);

        while (!openList.isEmpty()) {
            Node current = openList.poll();

            if (current.isSameAs(end)) {
                return reconstructPath(current);
            }

            closedList.add(current);

            for (Node neighbor : getNeighbors(grid, current)) {
                if (closedList.contains(neighbor)) {
                    continue;
                }

                double tentativeG = current.g + distance(current, neighbor);

                if (!openList.contains(neighbor) || tentativeG < neighbor.g) {
                    neighbor.parent = current;
                    neighbor.setCost(tentativeG, heuristic(neighbor, end));

                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    private List<Node> getNeighbors(Grid grid, Node node) {
        List<Node> neighbors = new ArrayList<>();
        int[][] directions = {
                {0, 1}, {1, 0}, {0, -1}, {-1, 0}, // Up, Right, Down, Left
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1} // Diagonals
        };

        for (int[] direction : directions) {
            int newX = node.x + direction[0];
            int newY = node.y + direction[1];

            if (newX >= 0 && newX < grid.getWidth() && newY >= 0 && newY < grid.getHeight()) {
                neighbors.add(new Node(newX, newY));
            }
        }

        return neighbors;
    }

    private double heuristic(Node a, Node b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y); // Manhattan distance
    }

    private double distance(Node a, Node b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2)); // Euclidean distance
    }

    private List<Node> reconstructPath(Node node) {
        List<Node> path = new ArrayList<>();
        while (node != null) {
            path.add(node);
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }
}