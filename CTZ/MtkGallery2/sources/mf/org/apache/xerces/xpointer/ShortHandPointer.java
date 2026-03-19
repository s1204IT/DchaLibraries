package mf.org.apache.xerces.xpointer;

import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.xni.Augmentations;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xni.XMLAttributes;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xs.AttributePSVI;
import mf.org.apache.xerces.xs.XSTypeDefinition;

final class ShortHandPointer implements XPointerPart {
    private boolean fIsFragmentResolved = false;
    int fMatchingChildCount = 0;
    private String fShortHandPointer;
    private SymbolTable fSymbolTable;

    public ShortHandPointer() {
    }

    public ShortHandPointer(SymbolTable symbolTable) {
        this.fSymbolTable = symbolTable;
    }

    @Override
    public void parseXPointer(String part) throws XNIException {
        this.fShortHandPointer = part;
        this.fIsFragmentResolved = false;
    }

    @Override
    public boolean resolveXPointer(QName element, XMLAttributes attributes, Augmentations augs, int event) throws XNIException {
        if (this.fMatchingChildCount == 0) {
            this.fIsFragmentResolved = false;
        }
        if (event == 0) {
            if (this.fMatchingChildCount == 0) {
                this.fIsFragmentResolved = hasMatchingIdentifier(element, attributes, augs, event);
            }
            if (this.fIsFragmentResolved) {
                this.fMatchingChildCount++;
            }
        } else if (event == 2) {
            if (this.fMatchingChildCount == 0) {
                this.fIsFragmentResolved = hasMatchingIdentifier(element, attributes, augs, event);
            }
        } else if (this.fIsFragmentResolved) {
            this.fMatchingChildCount--;
        }
        return this.fIsFragmentResolved;
    }

    private boolean hasMatchingIdentifier(QName element, XMLAttributes attributes, Augmentations augs, int event) throws XNIException {
        String normalizedValue = null;
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength() && (normalizedValue = getSchemaDeterminedID(attributes, i)) == null && (normalizedValue = getChildrenSchemaDeterminedID(attributes, i)) == null && (normalizedValue = getDTDDeterminedID(attributes, i)) == null; i++) {
            }
        }
        if (normalizedValue != null && normalizedValue.equals(this.fShortHandPointer)) {
            return true;
        }
        return false;
    }

    public String getDTDDeterminedID(XMLAttributes attributes, int index) throws XNIException {
        if (attributes.getType(index).equals(SchemaSymbols.ATTVAL_ID)) {
            return attributes.getValue(index);
        }
        return null;
    }

    public String getSchemaDeterminedID(XMLAttributes attributes, int index) throws XNIException {
        Augmentations augs = attributes.getAugmentations(index);
        AttributePSVI attrPSVI = (AttributePSVI) augs.getItem(Constants.ATTRIBUTE_PSVI);
        if (attrPSVI != null) {
            XSTypeDefinition typeDef = attrPSVI.getMemberTypeDefinition();
            if (typeDef != null) {
                typeDef = attrPSVI.getTypeDefinition();
            }
            if (typeDef != null && ((XSSimpleType) typeDef).isIDType()) {
                return attrPSVI.getSchemaNormalizedValue();
            }
            return null;
        }
        return null;
    }

    public String getChildrenSchemaDeterminedID(XMLAttributes attributes, int index) throws XNIException {
        return null;
    }

    @Override
    public boolean isFragmentResolved() {
        return this.fIsFragmentResolved;
    }

    @Override
    public boolean isChildFragmentResolved() {
        return this.fIsFragmentResolved && this.fMatchingChildCount > 0;
    }

    @Override
    public String getSchemeName() {
        return this.fShortHandPointer;
    }

    @Override
    public String getSchemeData() {
        return null;
    }

    @Override
    public void setSchemeName(String schemeName) {
        this.fShortHandPointer = schemeName;
    }

    @Override
    public void setSchemeData(String schemeData) {
    }
}
