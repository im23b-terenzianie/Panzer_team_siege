import dev.zwazel.internal.game.utils.Graph;
import dev.zwazel.internal.game.utils.Node;
import dev.zwazel.internal.game.utils.NodeComparator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

// FindPath.java
public class FindPath {
    private final Node root; // Starting node
    private final Node target; // Target node
    private final Graph graph; // Graph containing nodes and edges
    private LinkedList<Node> path = new LinkedList<>(); // List to store the path
    private PriorityQueue<Node> open = new PriorityQueue<>(new NodeComparator()); // Priority queue for open nodes
    private HashSet<Node> closed = new HashSet<>(); // Set for closed nodes

    public FindPath(Node root, Node target, Graph graph) {
        this.root = root;
        this.target = target;
        this.graph = graph;
    }

    // Returns the path from the root to the target
    public LinkedList<Node> findPath() {
        open.add(root); // Add the root node to the open list
        root.setCost(0); // Set the cost of the root node to 0

        Node current;
        while (!open.isEmpty()) {
            current = open.poll(); // Get the node with the lowest cost
            if (current.equals(target)) { // If the target node is reached
                Node node = current;
                while (node != null) { // Reconstruct the path
                    path.addFirst(node);
                    node = node.getParent();
                }
                return path; // Return the path
            }
            expand(current); // Expand the current node
            closed.add(current); // Add current Node to closed list
        }
        return path; // Return the path (empty if no path found)
    }

    // Expands the current node by adding its neighbors to the open list
    private void expand(Node current) {
        for (Node neighbour : current.getNeighbours()) {
            if (closed.contains(neighbour)) { // Skip if the neighbor is in the closed list
                continue;
            }
            double maxHeightDifference = 0.25;
            double heightDifference = Math.abs(neighbour.getHeight() - current.getHeight());
            if (heightDifference > maxHeightDifference) {
                continue;
            }
            double neighbourCost = Math.sqrt(Math.pow(current.getX() - neighbour.getX(), 2)
                    + Math.pow(current.getY() - neighbour.getY(), 2));



            double tentativeG = current.getCost() + neighbourCost ; // Calculate the tentative cost

            if (tentativeG < neighbour.getCost()) { // If the new cost is lower
                neighbour.setCost(tentativeG); // Update the cost
                neighbour.setParent(current); // Set the parent to the current node
            }
            if (!open.contains(neighbour)) { // If the neighbor is not in the open list
                open.add(neighbour); // Add the neighbor to the open list
            } else {
                open.remove(neighbour); //Add input by Lucius
                open.add(neighbour);
            }
        }
    }

}