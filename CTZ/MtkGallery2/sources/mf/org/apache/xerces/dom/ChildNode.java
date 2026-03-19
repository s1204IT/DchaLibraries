package mf.org.apache.xerces.dom;

import mf.org.w3c.dom.Node;

public abstract class ChildNode extends NodeImpl {
    static final long serialVersionUID = -6112455738802414002L;
    protected ChildNode nextSibling;
    protected ChildNode previousSibling;

    protected ChildNode(CoreDocumentImpl ownerDocument) {
        super(ownerDocument);
    }

    public ChildNode() {
    }

    @Override
    public Node cloneNode(boolean deep) {
        ChildNode newnode = (ChildNode) super.cloneNode(deep);
        newnode.previousSibling = null;
        newnode.nextSibling = null;
        newnode.isFirstChild(false);
        return newnode;
    }

    @Override
    public Node getParentNode() {
        if (isOwned()) {
            return this.ownerNode;
        }
        return null;
    }

    @Override
    final NodeImpl parentNode() {
        if (isOwned()) {
            return this.ownerNode;
        }
        return null;
    }

    @Override
    public Node getNextSibling() {
        return this.nextSibling;
    }

    @Override
    public Node getPreviousSibling() {
        if (isFirstChild()) {
            return null;
        }
        return this.previousSibling;
    }

    @Override
    final ChildNode previousSibling() {
        if (isFirstChild()) {
            return null;
        }
        return this.previousSibling;
    }
}
