package mf.org.apache.xerces.impl.xs;

import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.xs.ShortList;
import mf.org.apache.xerces.xs.XSAttributeDeclaration;
import mf.org.apache.xerces.xs.XSAttributeUse;
import mf.org.apache.xerces.xs.XSNamespaceItem;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSValue;

public class XSAttributeUseImpl implements XSAttributeUse {
    public XSAttributeDecl fAttrDecl = null;
    public short fUse = 0;
    public short fConstraintType = 0;
    public ValidatedInfo fDefault = null;
    public XSObjectList fAnnotations = null;

    public void reset() {
        this.fDefault = null;
        this.fAttrDecl = null;
        this.fUse = (short) 0;
        this.fConstraintType = (short) 0;
        this.fAnnotations = null;
    }

    @Override
    public short getType() {
        return (short) 4;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public boolean getRequired() {
        return this.fUse == 1;
    }

    @Override
    public XSAttributeDeclaration getAttrDeclaration() {
        return this.fAttrDecl;
    }

    @Override
    public short getConstraintType() {
        return this.fConstraintType;
    }

    @Override
    public String getConstraintValue() {
        if (getConstraintType() == 0) {
            return null;
        }
        return this.fDefault.stringValue();
    }

    @Override
    public XSNamespaceItem getNamespaceItem() {
        return null;
    }

    @Override
    public Object getActualVC() {
        if (getConstraintType() == 0) {
            return null;
        }
        return this.fDefault.actualValue;
    }

    @Override
    public short getActualVCType() {
        if (getConstraintType() == 0) {
            return (short) 45;
        }
        return this.fDefault.actualValueType;
    }

    @Override
    public ShortList getItemValueTypes() {
        if (getConstraintType() == 0) {
            return null;
        }
        return this.fDefault.itemValueTypes;
    }

    @Override
    public XSValue getValueConstraintValue() {
        return this.fDefault;
    }

    @Override
    public XSObjectList getAnnotations() {
        return this.fAnnotations != null ? this.fAnnotations : XSObjectListImpl.EMPTY_LIST;
    }
}
