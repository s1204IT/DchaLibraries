package org.apache.harmony.xml.dom;

import org.w3c.dom.Node;

public abstract class LeafNodeImpl extends NodeImpl {
    int index;
    InnerNodeImpl parent;

    LeafNodeImpl(DocumentImpl document) {
        super(document);
    }

    @Override
    public Node getNextSibling() {
        if (this.parent == null || this.index + 1 >= this.parent.children.size()) {
            return null;
        }
        return this.parent.children.get(this.index + 1);
    }

    @Override
    public Node getParentNode() {
        return this.parent;
    }

    @Override
    public Node getPreviousSibling() {
        if (this.parent == null || this.index == 0) {
            return null;
        }
        return this.parent.children.get(this.index - 1);
    }

    boolean isParentOf(Node node) {
        return false;
    }
}
