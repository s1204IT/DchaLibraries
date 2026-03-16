package java.util.prefs;

import java.util.ArrayList;
import java.util.Iterator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class NodeSet implements NodeList {
    ArrayList<Node> list = new ArrayList<>();

    public NodeSet(Iterator<Node> nodes) {
        while (nodes.hasNext()) {
            this.list.add(nodes.next());
        }
    }

    @Override
    public int getLength() {
        return this.list.size();
    }

    @Override
    public Node item(int index) {
        try {
            Node result = this.list.get(index);
            return result;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
}
