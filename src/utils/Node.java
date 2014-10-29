package utils;
import java.util.ArrayList;

public class Node {
    String name;
    ArrayList<Node> children = null;

    public Node() {
    	this.name = "";
    }
    Node(String name) {
        this.name = name;
    }

    public void addChild(Node child) {
        if (children == null)
            children = new ArrayList<Node>();
        children.add(child);
    }

    public void setChild(int index, Node child) {
        if (index >= 0 && index < children.size())
            children.set(index, child);
    }
    public Node getChild(int index) {
        if (index >= 0 && index < children.size())
            return children.get(index);
        return null;
    }
    public Node findChild(String name) {
        for (Node n : children) {
            if (n.name.equals(name))
                return n;
        }
        return null;
    }
}