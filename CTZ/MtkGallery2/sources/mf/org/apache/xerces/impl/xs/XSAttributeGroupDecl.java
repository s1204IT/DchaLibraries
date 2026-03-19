package mf.org.apache.xerces.impl.xs;

import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.xs.XSAnnotation;
import mf.org.apache.xerces.xs.XSAttributeGroupDefinition;
import mf.org.apache.xerces.xs.XSAttributeUse;
import mf.org.apache.xerces.xs.XSNamespaceItem;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSWildcard;

public class XSAttributeGroupDecl implements XSAttributeGroupDefinition {
    private static final int INITIAL_SIZE = 5;
    public XSObjectList fAnnotations;
    public String fName = null;
    public String fTargetNamespace = null;
    int fAttrUseNum = 0;
    XSAttributeUseImpl[] fAttributeUses = new XSAttributeUseImpl[5];
    public XSWildcardDecl fAttributeWC = null;
    public String fIDAttrName = null;
    protected XSObjectListImpl fAttrUses = null;
    private XSNamespaceItem fNamespaceItem = null;

    public String addAttributeUse(XSAttributeUseImpl attrUse) {
        if (attrUse.fUse != 2 && attrUse.fAttrDecl.fType.isIDType()) {
            if (this.fIDAttrName == null) {
                this.fIDAttrName = attrUse.fAttrDecl.fName;
            } else {
                return this.fIDAttrName;
            }
        }
        if (this.fAttrUseNum == this.fAttributeUses.length) {
            this.fAttributeUses = resize(this.fAttributeUses, this.fAttrUseNum * 2);
        }
        XSAttributeUseImpl[] xSAttributeUseImplArr = this.fAttributeUses;
        int i = this.fAttrUseNum;
        this.fAttrUseNum = i + 1;
        xSAttributeUseImplArr[i] = attrUse;
        return null;
    }

    public void replaceAttributeUse(XSAttributeUse oldUse, XSAttributeUseImpl newUse) {
        for (int i = 0; i < this.fAttrUseNum; i++) {
            if (this.fAttributeUses[i] == oldUse) {
                this.fAttributeUses[i] = newUse;
            }
        }
    }

    public XSAttributeUse getAttributeUse(String namespace, String name) {
        for (int i = 0; i < this.fAttrUseNum; i++) {
            if (this.fAttributeUses[i].fAttrDecl.fTargetNamespace == namespace && this.fAttributeUses[i].fAttrDecl.fName == name) {
                return this.fAttributeUses[i];
            }
        }
        return null;
    }

    public XSAttributeUse getAttributeUseNoProhibited(String namespace, String name) {
        for (int i = 0; i < this.fAttrUseNum; i++) {
            if (this.fAttributeUses[i].fAttrDecl.fTargetNamespace == namespace && this.fAttributeUses[i].fAttrDecl.fName == name && this.fAttributeUses[i].fUse != 2) {
                return this.fAttributeUses[i];
            }
        }
        return null;
    }

    public void removeProhibitedAttrs() {
        if (this.fAttrUseNum == 0) {
            return;
        }
        int count = 0;
        XSAttributeUseImpl[] uses = new XSAttributeUseImpl[this.fAttrUseNum];
        for (int i = 0; i < this.fAttrUseNum; i++) {
            if (this.fAttributeUses[i].fUse != 2) {
                uses[count] = this.fAttributeUses[i];
                count++;
            }
        }
        this.fAttributeUses = uses;
        this.fAttrUseNum = count;
    }

    public Object[] validRestrictionOf(String typeName, XSAttributeGroupDecl baseGroup) {
        for (int i = 0; i < this.fAttrUseNum; i++) {
            XSAttributeUseImpl attrUse = this.fAttributeUses[i];
            XSAttributeDecl attrDecl = attrUse.fAttrDecl;
            XSAttributeUseImpl baseAttrUse = (XSAttributeUseImpl) baseGroup.getAttributeUse(attrDecl.fTargetNamespace, attrDecl.fName);
            if (baseAttrUse != null) {
                if (baseAttrUse.getRequired() && !attrUse.getRequired()) {
                    Object[] errorArgs = new Object[4];
                    errorArgs[0] = typeName;
                    errorArgs[1] = attrDecl.fName;
                    errorArgs[2] = attrUse.fUse == 0 ? SchemaSymbols.ATTVAL_OPTIONAL : SchemaSymbols.ATTVAL_PROHIBITED;
                    errorArgs[3] = "derivation-ok-restriction.2.1.1";
                    return errorArgs;
                }
                if (attrUse.fUse == 2) {
                    continue;
                } else {
                    XSAttributeDecl baseAttrDecl = baseAttrUse.fAttrDecl;
                    if (!XSConstraints.checkSimpleDerivationOk(attrDecl.fType, baseAttrDecl.fType, baseAttrDecl.fType.getFinal())) {
                        Object[] errorArgs2 = {typeName, attrDecl.fName, attrDecl.fType.getName(), baseAttrDecl.fType.getName(), "derivation-ok-restriction.2.1.2"};
                        return errorArgs2;
                    }
                    int baseConsType = baseAttrUse.fConstraintType != 0 ? baseAttrUse.fConstraintType : baseAttrDecl.getConstraintType();
                    int thisConstType = attrUse.fConstraintType != 0 ? attrUse.fConstraintType : attrDecl.getConstraintType();
                    if (baseConsType != 2) {
                        continue;
                    } else {
                        if (thisConstType != 2) {
                            Object[] errorArgs3 = {typeName, attrDecl.fName, "derivation-ok-restriction.2.1.3.a"};
                            return errorArgs3;
                        }
                        ValidatedInfo baseFixedValue = baseAttrUse.fDefault != null ? baseAttrUse.fDefault : baseAttrDecl.fDefault;
                        ValidatedInfo thisFixedValue = attrUse.fDefault != null ? attrUse.fDefault : attrDecl.fDefault;
                        if (!baseFixedValue.actualValue.equals(thisFixedValue.actualValue)) {
                            Object[] errorArgs4 = {typeName, attrDecl.fName, thisFixedValue.stringValue(), baseFixedValue.stringValue(), "derivation-ok-restriction.2.1.3.b"};
                            return errorArgs4;
                        }
                    }
                }
            } else {
                if (baseGroup.fAttributeWC == null) {
                    Object[] errorArgs5 = {typeName, attrDecl.fName, "derivation-ok-restriction.2.2.a"};
                    return errorArgs5;
                }
                if (!baseGroup.fAttributeWC.allowNamespace(attrDecl.fTargetNamespace)) {
                    Object[] errorArgs6 = new Object[4];
                    errorArgs6[0] = typeName;
                    errorArgs6[1] = attrDecl.fName;
                    errorArgs6[2] = attrDecl.fTargetNamespace == null ? "" : attrDecl.fTargetNamespace;
                    errorArgs6[3] = "derivation-ok-restriction.2.2.b";
                    return errorArgs6;
                }
            }
        }
        for (int i2 = 0; i2 < baseGroup.fAttrUseNum; i2++) {
            XSAttributeUseImpl baseAttrUse2 = baseGroup.fAttributeUses[i2];
            if (baseAttrUse2.fUse == 1) {
                XSAttributeDecl baseAttrDecl2 = baseAttrUse2.fAttrDecl;
                if (getAttributeUse(baseAttrDecl2.fTargetNamespace, baseAttrDecl2.fName) == null) {
                    Object[] errorArgs7 = {typeName, baseAttrUse2.fAttrDecl.fName, "derivation-ok-restriction.3"};
                    return errorArgs7;
                }
            }
        }
        if (this.fAttributeWC != null) {
            if (baseGroup.fAttributeWC != null) {
                if (this.fAttributeWC.isSubsetOf(baseGroup.fAttributeWC)) {
                    if (this.fAttributeWC.weakerProcessContents(baseGroup.fAttributeWC)) {
                        Object[] errorArgs8 = {typeName, this.fAttributeWC.getProcessContentsAsString(), baseGroup.fAttributeWC.getProcessContentsAsString(), "derivation-ok-restriction.4.3"};
                        return errorArgs8;
                    }
                    return null;
                }
                Object[] errorArgs9 = {typeName, "derivation-ok-restriction.4.2"};
                return errorArgs9;
            }
            Object[] errorArgs10 = {typeName, "derivation-ok-restriction.4.1"};
            return errorArgs10;
        }
        return null;
    }

    static final XSAttributeUseImpl[] resize(XSAttributeUseImpl[] oldArray, int newSize) {
        XSAttributeUseImpl[] newArray = new XSAttributeUseImpl[newSize];
        System.arraycopy(oldArray, 0, newArray, 0, Math.min(oldArray.length, newSize));
        return newArray;
    }

    public void reset() {
        this.fName = null;
        this.fTargetNamespace = null;
        for (int i = 0; i < this.fAttrUseNum; i++) {
            this.fAttributeUses[i] = null;
        }
        this.fAttrUseNum = 0;
        this.fAttributeWC = null;
        this.fAnnotations = null;
        this.fIDAttrName = null;
    }

    @Override
    public short getType() {
        return (short) 5;
    }

    @Override
    public String getName() {
        return this.fName;
    }

    @Override
    public String getNamespace() {
        return this.fTargetNamespace;
    }

    @Override
    public XSObjectList getAttributeUses() {
        if (this.fAttrUses == null) {
            this.fAttrUses = new XSObjectListImpl(this.fAttributeUses, this.fAttrUseNum);
        }
        return this.fAttrUses;
    }

    @Override
    public XSWildcard getAttributeWildcard() {
        return this.fAttributeWC;
    }

    @Override
    public XSAnnotation getAnnotation() {
        if (this.fAnnotations != null) {
            return (XSAnnotation) this.fAnnotations.item(0);
        }
        return null;
    }

    @Override
    public XSObjectList getAnnotations() {
        return this.fAnnotations != null ? this.fAnnotations : XSObjectListImpl.EMPTY_LIST;
    }

    @Override
    public XSNamespaceItem getNamespaceItem() {
        return this.fNamespaceItem;
    }

    void setNamespaceItem(XSNamespaceItem namespaceItem) {
        this.fNamespaceItem = namespaceItem;
    }
}
