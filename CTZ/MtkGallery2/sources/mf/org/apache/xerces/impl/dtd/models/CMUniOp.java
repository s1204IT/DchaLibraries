package mf.org.apache.xerces.impl.dtd.models;

public class CMUniOp extends CMNode {
    private final CMNode fChild;

    public CMUniOp(int type, CMNode childNode) {
        super(type);
        if (type() != 1 && type() != 2 && type() != 3) {
            throw new RuntimeException("ImplementationMessages.VAL_UST");
        }
        this.fChild = childNode;
    }

    final CMNode getChild() {
        return this.fChild;
    }

    @Override
    public boolean isNullable() {
        if (type() == 3) {
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
