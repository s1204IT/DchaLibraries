package org.apache.harmony.xml.dom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class InnerNodeImpl extends LeafNodeImpl {
    List<LeafNodeImpl> children;

    protected InnerNodeImpl(DocumentImpl documentImpl) {
        super(documentImpl);
        this.children = new ArrayList();
    }

    @Override
    public Node appendChild(Node node) throws DOMException {
        return insertChildAt(node, this.children.size());
    }

    @Override
    public NodeList getChildNodes() {
        NodeListImpl nodeListImpl = new NodeListImpl();
        Iterator<LeafNodeImpl> it = this.children.iterator();
        while (it.hasNext()) {
            nodeListImpl.add(it.next());
        }
        return nodeListImpl;
    }

    @Override
    public Node getFirstChild() {
        if (this.children.isEmpty()) {
            return null;
        }
        return this.children.get(0);
    }

    @Override
    public Node getLastChild() {
        if (this.children.isEmpty()) {
            return null;
        }
        return this.children.get(this.children.size() - 1);
    }

    @Override
    public Node getNextSibling() {
        if (this.parent == null || this.index + 1 >= this.parent.children.size()) {
            return null;
        }
        return this.parent.children.get(this.index + 1);
    }

    @Override
    public boolean hasChildNodes() {
        return this.children.size() != 0;
    }

    @Override
    public Node insertBefore(Node node, Node node2) throws DOMException {
        LeafNodeImpl leafNodeImpl = (LeafNodeImpl) node2;
        if (leafNodeImpl == null) {
            return appendChild(node);
        }
        if (leafNodeImpl.document != this.document) {
            throw new DOMException((short) 4, null);
        }
        if (leafNodeImpl.parent != this) {
            throw new DOMException((short) 3, null);
        }
        return insertChildAt(node, leafNodeImpl.index);
    }

    Node insertChildAt(Node node, int i) throws DOMException {
        if (node instanceof DocumentFragment) {
            NodeList childNodes = node.getChildNodes();
            for (int i2 = 0; i2 < childNodes.getLength(); i2++) {
                insertChildAt(childNodes.item(i2), i + i2);
            }
            return node;
        }
        LeafNodeImpl leafNodeImpl = (LeafNodeImpl) node;
        if (leafNodeImpl.document != null && this.document != null && leafNodeImpl.document != this.document) {
            throw new DOMException((short) 4, null);
        }
        if (leafNodeImpl.isParentOf(this)) {
            throw new DOMException((short) 3, null);
        }
        if (leafNodeImpl.parent != null) {
            int i3 = leafNodeImpl.index;
            leafNodeImpl.parent.children.remove(i3);
            leafNodeImpl.parent.refreshIndices(i3);
        }
        this.children.add(i, leafNodeImpl);
        leafNodeImpl.parent = this;
        refreshIndices(i);
        return node;
    }

    @Override
    public boolean isParentOf(Node node) {
        for (LeafNodeImpl leafNodeImpl = (LeafNodeImpl) node; leafNodeImpl != null; leafNodeImpl = leafNodeImpl.parent) {
            if (leafNodeImpl == this) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final void normalize() {
        Node firstChild = getFirstChild();
        while (firstChild != null) {
            Node nextSibling = firstChild.getNextSibling();
            firstChild.normalize();
            if (firstChild.getNodeType() == 3) {
                ((TextImpl) firstChild).minimize();
            }
            firstChild = nextSibling;
        }
    }

    private void refreshIndices(int i) {
        while (i < this.children.size()) {
            this.children.get(i).index = i;
            i++;
        }
    }

    @Override
    public Node removeChild(Node node) throws DOMException {
        LeafNodeImpl leafNodeImpl = (LeafNodeImpl) node;
        if (leafNodeImpl.document != this.document) {
            throw new DOMException((short) 4, null);
        }
        if (leafNodeImpl.parent != this) {
            throw new DOMException((short) 3, null);
        }
        int i = leafNodeImpl.index;
        this.children.remove(i);
        leafNodeImpl.parent = null;
        refreshIndices(i);
        return node;
    }

    @Override
    public Node replaceChild(Node node, Node node2) throws DOMException {
        int i = ((LeafNodeImpl) node2).index;
        removeChild(node2);
        insertChildAt(node, i);
        return node2;
    }

    @Override
    public String getTextContent() throws DOMException {
        Node firstChild = getFirstChild();
        if (firstChild == null) {
            return "";
        }
        if (firstChild.getNextSibling() == null) {
            return hasTextContent(firstChild) ? firstChild.getTextContent() : "";
        }
        StringBuilder sb = new StringBuilder();
        getTextContent(sb);
        return sb.toString();
    }

    @Override
    void getTextContent(StringBuilder sb) throws DOMException {
        for (Node firstChild = getFirstChild(); firstChild != null; firstChild = firstChild.getNextSibling()) {
            if (hasTextContent(firstChild)) {
                ((NodeImpl) firstChild).getTextContent(sb);
            }
        }
    }

    final boolean hasTextContent(Node node) {
        return (node.getNodeType() == 8 || node.getNodeType() == 7) ? false : true;
    }

    void getElementsByTagName(NodeListImpl nodeListImpl, String str) {
        for (LeafNodeImpl leafNodeImpl : this.children) {
            if (leafNodeImpl.getNodeType() == 1) {
                ElementImpl elementImpl = (ElementImpl) leafNodeImpl;
                if (matchesNameOrWildcard(str, elementImpl.getNodeName())) {
                    nodeListImpl.add(elementImpl);
                }
                elementImpl.getElementsByTagName(nodeListImpl, str);
            }
        }
    }

    void getElementsByTagNameNS(NodeListImpl nodeListImpl, String str, String str2) {
        for (LeafNodeImpl leafNodeImpl : this.children) {
            if (leafNodeImpl.getNodeType() == 1) {
                ElementImpl elementImpl = (ElementImpl) leafNodeImpl;
                if (matchesNameOrWildcard(str, elementImpl.getNamespaceURI()) && matchesNameOrWildcard(str2, elementImpl.getLocalName())) {
                    nodeListImpl.add(elementImpl);
                }
                elementImpl.getElementsByTagNameNS(nodeListImpl, str, str2);
            }
        }
    }

    private static boolean matchesNameOrWildcard(String str, String str2) {
        return "*".equals(str) || Objects.equals(str, str2);
    }
}
