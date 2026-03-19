package mf.org.apache.xerces.impl.xs.models;

import mf.org.apache.xerces.impl.dtd.models.CMNode;
import mf.org.apache.xerces.impl.dtd.models.CMStateSet;

public class XSCMBinOp extends CMNode {
    private CMNode fLeftChild;
    private CMNode fRightChild;

    public XSCMBinOp(int type, CMNode leftNode, CMNode rightNode) {
        super(type);
        if (type() != 101 && type() != 102) {
            throw new RuntimeException("ImplementationMessages.VAL_BST");
        }
        this.fLeftChild = leftNode;
        this.fRightChild = rightNode;
    }

    final CMNode getLeft() {
        return this.fLeftChild;
    }

    final CMNode getRight() {
        return this.fRightChild;
    }

    @Override
    public boolean isNullable() {
        if (type() == 101) {
            return this.fLeftChild.isNullable() || this.fRightChild.isNullable();
        }
        if (type() == 102) {
            return this.fLeftChild.isNullable() && this.fRightChild.isNullable();
        }
        throw new RuntimeException("ImplementationMessages.VAL_BST");
    }

    @Override
    protected void calcFirstPos(CMStateSet toSet) {
        if (type() == 101) {
            toSet.setTo(this.fLeftChild.firstPos());
            toSet.union(this.fRightChild.firstPos());
        } else {
            if (type() == 102) {
                toSet.setTo(this.fLeftChild.firstPos());
                if (this.fLeftChild.isNullable()) {
                    toSet.union(this.fRightChild.firstPos());
                    return;
                }
                return;
            }
            throw new RuntimeException("ImplementationMessages.VAL_BST");
        }
    }

    @Override
    protected void calcLastPos(CMStateSet toSet) {
        if (type() == 101) {
            toSet.setTo(this.fLeftChild.lastPos());
            toSet.union(this.fRightChild.lastPos());
        } else {
            if (type() == 102) {
                toSet.setTo(this.fRightChild.lastPos());
                if (this.fRightChild.isNullable()) {
                    toSet.union(this.fLeftChild.lastPos());
                    return;
                }
                return;
            }
            throw new RuntimeException("ImplementationMessages.VAL_BST");
        }
    }
}
