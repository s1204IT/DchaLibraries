package mf.org.apache.xerces.dom;

import java.util.ArrayList;
import mf.org.apache.xerces.dom3.as.ASContentModel;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.NodeList;

public class DeepNodeListImpl implements NodeList {
    protected int changes;
    protected boolean enableNS;
    protected ArrayList nodes;
    protected String nsName;
    protected NodeImpl rootNode;
    protected String tagName;

    public DeepNodeListImpl(NodeImpl rootNode, String tagName) {
        this.changes = 0;
        this.enableNS = false;
        this.rootNode = rootNode;
        this.tagName = tagName;
        this.nodes = new ArrayList();
    }

    public DeepNodeListImpl(NodeImpl rootNode, String nsName, String tagName) {
        this(rootNode, tagName);
        this.nsName = (nsName == null || nsName.length() == 0) ? null : nsName;
        this.enableNS = true;
    }

    @Override
    public int getLength() {
        item(ASContentModel.AS_UNBOUNDED);
        return this.nodes.size();
    }

    @Override
    public Node item(int index) {
        Node thisNode;
        if (this.rootNode.changes() != this.changes) {
            this.nodes = new ArrayList();
            this.changes = this.rootNode.changes();
        }
        int currentSize = this.nodes.size();
        if (index < currentSize) {
            return (Node) this.nodes.get(index);
        }
        if (currentSize == 0) {
            thisNode = this.rootNode;
        } else {
            thisNode = (NodeImpl) this.nodes.get(currentSize - 1);
        }
        while (thisNode != null && index >= this.nodes.size()) {
            thisNode = nextMatchingElementAfter(thisNode);
            if (thisNode != null) {
                this.nodes.add(thisNode);
            }
        }
        return thisNode;
    }

    protected Node nextMatchingElementAfter(Node current) {
        Node next;
        while (current != null) {
            if (current.hasChildNodes()) {
                current = current.getFirstChild();
            } else if (current != this.rootNode && (next = current.getNextSibling()) != null) {
                current = next;
            } else {
                Node next2 = null;
                while (current != this.rootNode && (next2 = current.getNextSibling()) == null) {
                    current = current.getParentNode();
                }
                current = next2;
            }
            Node next3 = this.rootNode;
            if (current != next3 && current != null && current.getNodeType() == 1) {
                if (!this.enableNS) {
                    if (this.tagName.equals("*") || ((ElementImpl) current).getTagName().equals(this.tagName)) {
                        return current;
                    }
                } else if (this.tagName.equals("*")) {
                    if (this.nsName != null && this.nsName.equals("*")) {
                        return current;
                    }
                    ElementImpl el = (ElementImpl) current;
                    if ((this.nsName == null && el.getNamespaceURI() == null) || (this.nsName != null && this.nsName.equals(el.getNamespaceURI()))) {
                        return current;
                    }
                } else {
                    ElementImpl el2 = (ElementImpl) current;
                    if (el2.getLocalName() != null && el2.getLocalName().equals(this.tagName)) {
                        if (this.nsName != null && this.nsName.equals("*")) {
                            return current;
                        }
                        if ((this.nsName == null && el2.getNamespaceURI() == null) || (this.nsName != null && this.nsName.equals(el2.getNamespaceURI()))) {
                            return current;
                        }
                    }
                }
            }
        }
        return null;
    }
}
