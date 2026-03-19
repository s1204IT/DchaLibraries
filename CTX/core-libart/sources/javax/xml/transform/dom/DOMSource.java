package javax.xml.transform.dom;

import javax.xml.transform.Source;
import org.w3c.dom.Node;

public class DOMSource implements Source {
    public static final String FEATURE = "http://javax.xml.transform.dom.DOMSource/feature";
    private Node node;
    private String systemID;

    public DOMSource() {
    }

    public DOMSource(Node node) {
        setNode(node);
    }

    public DOMSource(Node node, String str) {
        setNode(node);
        setSystemId(str);
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return this.node;
    }

    @Override
    public void setSystemId(String str) {
        this.systemID = str;
    }

    @Override
    public String getSystemId() {
        return this.systemID;
    }
}
