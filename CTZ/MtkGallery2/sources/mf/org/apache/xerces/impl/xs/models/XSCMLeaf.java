package mf.org.apache.xerces.impl.xs.models;

import mf.org.apache.xerces.impl.dtd.models.CMNode;
import mf.org.apache.xerces.impl.dtd.models.CMStateSet;

public class XSCMLeaf extends CMNode {
    private final Object fLeaf;
    private int fParticleId;
    private int fPosition;

    public XSCMLeaf(int type, Object leaf, int id, int position) {
        super(type);
        this.fParticleId = -1;
        this.fPosition = -1;
        this.fLeaf = leaf;
        this.fParticleId = id;
        this.fPosition = position;
    }

    final Object getLeaf() {
        return this.fLeaf;
    }

    final int getParticleId() {
        return this.fParticleId;
    }

    final int getPosition() {
        return this.fPosition;
    }

    final void setPosition(int newPosition) {
        this.fPosition = newPosition;
    }

    @Override
    public boolean isNullable() {
        return this.fPosition == -1;
    }

    public String toString() {
        StringBuffer strRet = new StringBuffer(this.fLeaf.toString());
        if (this.fPosition >= 0) {
            strRet.append(" (Pos:");
            strRet.append(Integer.toString(this.fPosition));
            strRet.append(')');
        }
        return strRet.toString();
    }

    @Override
    protected void calcFirstPos(CMStateSet toSet) {
        if (this.fPosition == -1) {
            toSet.zeroBits();
        } else {
            toSet.setBit(this.fPosition);
        }
    }

    @Override
    protected void calcLastPos(CMStateSet toSet) {
        if (this.fPosition == -1) {
            toSet.zeroBits();
        } else {
            toSet.setBit(this.fPosition);
        }
    }
}
