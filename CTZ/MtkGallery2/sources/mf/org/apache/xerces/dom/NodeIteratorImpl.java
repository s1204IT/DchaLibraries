package mf.org.apache.xerces.dom;

import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.traversal.NodeFilter;
import mf.org.w3c.dom.traversal.NodeIterator;

public class NodeIteratorImpl implements NodeIterator {
    private DocumentImpl fDocument;
    private boolean fEntityReferenceExpansion;
    private NodeFilter fNodeFilter;
    private Node fRoot;
    private int fWhatToShow;
    private boolean fDetach = false;
    private boolean fForward = true;
    private Node fCurrentNode = null;

    public NodeIteratorImpl(DocumentImpl document, Node root, int whatToShow, NodeFilter nodeFilter, boolean entityReferenceExpansion) {
        this.fWhatToShow = -1;
        this.fDocument = document;
        this.fRoot = root;
        this.fWhatToShow = whatToShow;
        this.fNodeFilter = nodeFilter;
        this.fEntityReferenceExpansion = entityReferenceExpansion;
    }

    public Node getRoot() {
        return this.fRoot;
    }

    public int getWhatToShow() {
        return this.fWhatToShow;
    }

    public NodeFilter getFilter() {
        return this.fNodeFilter;
    }

    public boolean getExpandEntityReferences() {
        return this.fEntityReferenceExpansion;
    }

    public Node nextNode() {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        if (this.fRoot == null) {
            return null;
        }
        Node nextNode = this.fCurrentNode;
        boolean accepted = false;
        while (!accepted) {
            if (!this.fForward && nextNode != null) {
                nextNode = this.fCurrentNode;
            } else if (!this.fEntityReferenceExpansion && nextNode != null && nextNode.getNodeType() == 5) {
                nextNode = nextNode(nextNode, false);
            } else {
                nextNode = nextNode(nextNode, true);
            }
            this.fForward = true;
            if (nextNode == null) {
                return null;
            }
            accepted = acceptNode(nextNode);
            if (accepted) {
                this.fCurrentNode = nextNode;
                return this.fCurrentNode;
            }
        }
        return null;
    }

    public Node previousNode() {
        if (this.fDetach) {
            throw new DOMException((short) 11, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        if (this.fRoot == null || this.fCurrentNode == null) {
            return null;
        }
        Node previousNode = this.fCurrentNode;
        boolean accepted = false;
        while (!accepted) {
            if (this.fForward && previousNode != null) {
                previousNode = this.fCurrentNode;
            } else {
                previousNode = previousNode(previousNode);
            }
            this.fForward = false;
            if (previousNode == null) {
                return null;
            }
            accepted = acceptNode(previousNode);
            if (accepted) {
                this.fCurrentNode = previousNode;
                return this.fCurrentNode;
            }
        }
        return null;
    }

    boolean acceptNode(Node node) {
        return this.fNodeFilter == null ? (this.fWhatToShow & (1 << (node.getNodeType() - 1))) != 0 : (this.fWhatToShow & (1 << (node.getNodeType() - 1))) != 0 && this.fNodeFilter.acceptNode(node) == 1;
    }

    Node matchNodeOrParent(Node node) {
        if (this.fCurrentNode == null) {
            return null;
        }
        for (Node n = this.fCurrentNode; n != this.fRoot; n = n.getParentNode()) {
            if (node == n) {
                return n;
            }
        }
        return null;
    }

    Node nextNode(Node node, boolean visitChildren) {
        if (node == null) {
            return this.fRoot;
        }
        if (visitChildren && node.hasChildNodes()) {
            return node.getFirstChild();
        }
        if (node == this.fRoot) {
            return null;
        }
        Node result = node.getNextSibling();
        if (result != null) {
            return result;
        }
        for (Node parent = node.getParentNode(); parent != null && parent != this.fRoot; parent = parent.getParentNode()) {
            Node result2 = parent.getNextSibling();
            if (result2 != null) {
                return result2;
            }
        }
        return null;
    }

    Node previousNode(Node node) {
        if (node == this.fRoot) {
            return null;
        }
        Node result = node.getPreviousSibling();
        if (result == null) {
            return node.getParentNode();
        }
        if (result.hasChildNodes() && (this.fEntityReferenceExpansion || result == null || result.getNodeType() != 5)) {
            while (result.hasChildNodes()) {
                result = result.getLastChild();
            }
        }
        return result;
    }

    public void removeNode(Node node) {
        Node deleted;
        if (node == null || (deleted = matchNodeOrParent(node)) == null) {
            return;
        }
        if (this.fForward) {
            this.fCurrentNode = previousNode(deleted);
            return;
        }
        Node next = nextNode(deleted, false);
        if (next != null) {
            this.fCurrentNode = next;
        } else {
            this.fCurrentNode = previousNode(deleted);
            this.fForward = true;
        }
    }

    public void detach() {
        this.fDetach = true;
        this.fDocument.removeNodeIterator(this);
    }
}
