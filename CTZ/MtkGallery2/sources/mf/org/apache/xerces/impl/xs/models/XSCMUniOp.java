package mf.org.apache.xerces.impl.xs.models;

import mf.org.apache.xerces.impl.dtd.models.CMNode;
import mf.org.apache.xerces.impl.dtd.models.CMStateSet;

public class XSCMUniOp extends CMNode {
    private CMNode fChild;

    public XSCMUniOp(int type, CMNode childNode) {
        super(type);
        if (type() != 5 && type() != 4 && type() != 6) {
            throw new RuntimeException("ImplementationMessages.VAL_UST");
        }
        this.fChild = childNode;
    }

    final CMNode getChild() {
        return this.fChild;
    }

    @Override
    public boolean isNullable() {
        if (type() == 6) {
            return this.fChild.isNullable();
        }
        return true;
    }

    @Override
    protected void calcFirstPos(CMStateSet toSet) {
        toSet.setTo(this.fChild.firstPos());
    }

    @Override
    protected void calcLastPos(CMStateSet toSet) {
        toSet.setTo(this.fChild.lastPos());
    }
}
