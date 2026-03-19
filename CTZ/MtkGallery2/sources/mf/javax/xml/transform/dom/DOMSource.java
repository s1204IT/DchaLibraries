package mf.javax.xml.transform.dom;

import mf.javax.xml.transform.Source;
import mf.org.w3c.dom.Node;

public class DOMSource implements Source {
    private Node node;
    private String systemID;

    public Node getNode() {
        return this.node;
    }

    public String getSystemId() {
        return this.systemID;
    }
}
