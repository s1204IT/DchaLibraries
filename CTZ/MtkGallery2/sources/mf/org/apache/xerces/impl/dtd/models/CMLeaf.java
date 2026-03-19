package mf.org.apache.xerces.impl.dtd.models;

import mf.org.apache.xerces.xni.QName;

public class CMLeaf extends CMNode {
    private final QName fElement;
    private int fPosition;

    public CMLeaf(QName element, int position) {
        super(0);
        this.fElement = new QName();
        this.fPosition = -1;
        this.fElement.setValues(element);
        this.fPosition = position;
    }

    public CMLeaf(QName element) {
        super(0);
        this.fElement = new QName();
        this.fPosition = -1;
        this.fElement.setValues(element);
    }

    final QName getElement() {
        return this.fElement;
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
        StringBuffer strRet = new StringBuffer(this.fElement.toString());
        strRet.append(" (");
        strRet.append(this.fElement.uri);
        strRet.append(',');
        strRet.append(this.fElement.localpart);
        strRet.append(')');
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
