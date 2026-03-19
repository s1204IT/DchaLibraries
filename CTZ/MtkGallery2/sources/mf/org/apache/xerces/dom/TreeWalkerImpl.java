package mf.org.apache.xerces.dom;

import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.traversal.NodeFilter;
import mf.org.w3c.dom.traversal.TreeWalker;

public class TreeWalkerImpl implements TreeWalker {
    Node fCurrentNode;
    private boolean fEntityReferenceExpansion;
    NodeFilter fNodeFilter;
    Node fRoot;
    private boolean fUseIsSameNode;
    int fWhatToShow;

    public TreeWalkerImpl(Node root, int whatToShow, NodeFilter nodeFilter, boolean entityReferenceExpansion) {
        this.fEntityReferenceExpansion = false;
        this.fWhatToShow = -1;
        this.fCurrentNode = root;
        this.fRoot = root;
        this.fUseIsSameNode = useIsSameNode(root);
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

    public void setWhatShow(int whatToShow) {
        this.fWhatToShow = whatToShow;
    }

    public NodeFilter getFilter() {
        return this.fNodeFilter;
    }

    public boolean getExpandEntityReferences() {
        return this.fEntityReferenceExpansion;
    }

    public Node getCurrentNode() {
        return this.fCurrentNode;
    }

    public void setCurrentNode(Node node) {
        if (node == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
            throw new DOMException((short) 9, msg);
        }
        this.fCurrentNode = node;
    }

    public Node parentNode() {
        if (this.fCurrentNode == null) {
            return null;
        }
        Node node = getParentNode(this.fCurrentNode);
        if (node != null) {
            this.fCurrentNode = node;
        }
        return node;
    }

    public Node firstChild() {
        if (this.fCurrentNode == null) {
            return null;
        }
        Node node = getFirstChild(this.fCurrentNode);
        if (node != null) {
            this.fCurrentNode = node;
        }
        return node;
    }

    public Node lastChild() {
        if (this.fCurrentNode == null) {
            return null;
        }
        Node node = getLastChild(this.fCurrentNode);
        if (node != null) {
            this.fCurrentNode = node;
        }
        return node;
    }

    public Node previousSibling() {
        if (this.fCurrentNode == null) {
            return null;
        }
        Node node = getPreviousSibling(this.fCurrentNode);
        if (node != null) {
            this.fCurrentNode = node;
        }
        return node;
    }

    public Node nextSibling() {
        if (this.fCurrentNode == null) {
            return null;
        }
        Node node = getNextSibling(this.fCurrentNode);
        if (node != null) {
            this.fCurrentNode = node;
        }
        return node;
    }

    public Node previousNode() {
        if (this.fCurrentNode == null) {
            return null;
        }
        Node result = getPreviousSibling(this.fCurrentNode);
        if (result == null) {
            Node result2 = getParentNode(this.fCurrentNode);
            if (result2 == null) {
                return null;
            }
            this.fCurrentNode = result2;
            return this.fCurrentNode;
        }
        Node lastChild = getLastChild(result);
        Node prev = lastChild;
        while (lastChild != null) {
            prev = lastChild;
            lastChild = getLastChild(prev);
        }
        Node lastChild2 = prev;
        if (lastChild2 != null) {
            this.fCurrentNode = lastChild2;
            return this.fCurrentNode;
        }
        if (result == null) {
            return null;
        }
        this.fCurrentNode = result;
        return this.fCurrentNode;
    }

    public Node nextNode() {
        if (this.fCurrentNode == null) {
            return null;
        }
        Node result = getFirstChild(this.fCurrentNode);
        if (result != null) {
            this.fCurrentNode = result;
            return result;
        }
        Node result2 = getNextSibling(this.fCurrentNode);
        if (result2 != null) {
            this.fCurrentNode = result2;
            return result2;
        }
        Node parent = getParentNode(this.fCurrentNode);
        while (parent != null) {
            Node result3 = getNextSibling(parent);
            if (result3 != null) {
                this.fCurrentNode = result3;
                return result3;
            }
            parent = getParentNode(parent);
        }
        return null;
    }

    Node getParentNode(Node node) {
        Node newNode;
        if (node == null || isSameNode(node, this.fRoot) || (newNode = node.getParentNode()) == null) {
            return null;
        }
        int accept = acceptNode(newNode);
        if (accept == 1) {
            return newNode;
        }
        return getParentNode(newNode);
    }

    Node getNextSibling(Node node) {
        return getNextSibling(node, this.fRoot);
    }

    Node getNextSibling(Node node, Node root) {
        if (node == null || isSameNode(node, root)) {
            return null;
        }
        Node newNode = node.getNextSibling();
        if (newNode == null) {
            Node newNode2 = node.getParentNode();
            if (newNode2 == null || isSameNode(newNode2, root)) {
                return null;
            }
            int parentAccept = acceptNode(newNode2);
            if (parentAccept != 3) {
                return null;
            }
            return getNextSibling(newNode2, root);
        }
        int accept = acceptNode(newNode);
        if (accept == 1) {
            return newNode;
        }
        if (accept == 3) {
            Node fChild = getFirstChild(newNode);
            if (fChild == null) {
                return getNextSibling(newNode, root);
            }
            return fChild;
        }
        return getNextSibling(newNode, root);
    }

    Node getPreviousSibling(Node node) {
        return getPreviousSibling(node, this.fRoot);
    }

    Node getPreviousSibling(Node node, Node root) {
        if (node == null || isSameNode(node, root)) {
            return null;
        }
        Node newNode = node.getPreviousSibling();
        if (newNode == null) {
            Node newNode2 = node.getParentNode();
            if (newNode2 == null || isSameNode(newNode2, root)) {
                return null;
            }
            int parentAccept = acceptNode(newNode2);
            if (parentAccept != 3) {
                return null;
            }
            return getPreviousSibling(newNode2, root);
        }
        int accept = acceptNode(newNode);
        if (accept == 1) {
            return newNode;
        }
        if (accept == 3) {
            Node fChild = getLastChild(newNode);
            if (fChild == null) {
                return getPreviousSibling(newNode, root);
            }
            return fChild;
        }
        return getPreviousSibling(newNode, root);
    }

    Node getFirstChild(Node node) {
        Node newNode;
        if (node == null) {
            return null;
        }
        if ((!this.fEntityReferenceExpansion && node.getNodeType() == 5) || (newNode = node.getFirstChild()) == null) {
            return null;
        }
        int accept = acceptNode(newNode);
        if (accept == 1) {
            return newNode;
        }
        if (accept == 3 && newNode.hasChildNodes()) {
            Node fChild = getFirstChild(newNode);
            if (fChild == null) {
                return getNextSibling(newNode, node);
            }
            return fChild;
        }
        return getNextSibling(newNode, node);
    }

    Node getLastChild(Node node) {
        Node newNode;
        if (node == null) {
            return null;
        }
        if ((!this.fEntityReferenceExpansion && node.getNodeType() == 5) || (newNode = node.getLastChild()) == null) {
            return null;
        }
        int accept = acceptNode(newNode);
        if (accept == 1) {
            return newNode;
        }
        if (accept == 3 && newNode.hasChildNodes()) {
            Node lChild = getLastChild(newNode);
            if (lChild == null) {
                return getPreviousSibling(newNode, node);
            }
            return lChild;
        }
        return getPreviousSibling(newNode, node);
    }

    short acceptNode(Node node) {
        if (this.fNodeFilter == null) {
            return (this.fWhatToShow & (1 << (node.getNodeType() - 1))) != 0 ? (short) 1 : (short) 3;
        }
        if ((this.fWhatToShow & (1 << (node.getNodeType() - 1))) != 0) {
            return this.fNodeFilter.acceptNode(node);
        }
        return (short) 3;
    }

    private boolean useIsSameNode(Node node) {
        if (node instanceof NodeImpl) {
            return false;
        }
        Document doc = node.getNodeType() == 9 ? (Document) node : node.getOwnerDocument();
        return doc != null && doc.getImplementation().hasFeature("Core", "3.0");
    }

    private boolean isSameNode(Node m, Node n) {
        return this.fUseIsSameNode ? m.isSameNode(n) : m == n;
    }
}
