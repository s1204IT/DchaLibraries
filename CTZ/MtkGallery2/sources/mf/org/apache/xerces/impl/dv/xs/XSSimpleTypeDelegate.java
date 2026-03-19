package mf.org.apache.xerces.impl.dv.xs;

import mf.org.apache.xerces.impl.dv.DatatypeException;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeFacetException;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.XSFacets;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.xs.StringList;
import mf.org.apache.xerces.xs.XSNamespaceItem;
import mf.org.apache.xerces.xs.XSObject;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSSimpleTypeDefinition;
import mf.org.apache.xerces.xs.XSTypeDefinition;

public class XSSimpleTypeDelegate implements XSSimpleType {
    protected final XSSimpleType type;

    public XSSimpleTypeDelegate(XSSimpleType type) {
        if (type == null) {
            throw new NullPointerException();
        }
        this.type = type;
    }

    public XSSimpleType getWrappedXSSimpleType() {
        return this.type;
    }

    @Override
    public XSObjectList getAnnotations() {
        return this.type.getAnnotations();
    }

    @Override
    public boolean getBounded() {
        return this.type.getBounded();
    }

    @Override
    public short getBuiltInKind() {
        return this.type.getBuiltInKind();
    }

    @Override
    public short getDefinedFacets() {
        return this.type.getDefinedFacets();
    }

    @Override
    public XSObjectList getFacets() {
        return this.type.getFacets();
    }

    @Override
    public XSObject getFacet(int facetType) {
        return this.type.getFacet(facetType);
    }

    @Override
    public boolean getFinite() {
        return this.type.getFinite();
    }

    @Override
    public short getFixedFacets() {
        return this.type.getFixedFacets();
    }

    @Override
    public XSSimpleTypeDefinition getItemType() {
        return this.type.getItemType();
    }

    @Override
    public StringList getLexicalEnumeration() {
        return this.type.getLexicalEnumeration();
    }

    @Override
    public String getLexicalFacetValue(short facetName) {
        return this.type.getLexicalFacetValue(facetName);
    }

    @Override
    public StringList getLexicalPattern() {
        return this.type.getLexicalPattern();
    }

    @Override
    public XSObjectList getMemberTypes() {
        return this.type.getMemberTypes();
    }

    @Override
    public XSObjectList getMultiValueFacets() {
        return this.type.getMultiValueFacets();
    }

    @Override
    public boolean getNumeric() {
        return this.type.getNumeric();
    }

    @Override
    public short getOrdered() {
        return this.type.getOrdered();
    }

    @Override
    public XSSimpleTypeDefinition getPrimitiveType() {
        return this.type.getPrimitiveType();
    }

    @Override
    public short getVariety() {
        return this.type.getVariety();
    }

    @Override
    public boolean isDefinedFacet(short facetName) {
        return this.type.isDefinedFacet(facetName);
    }

    @Override
    public boolean isFixedFacet(short facetName) {
        return this.type.isFixedFacet(facetName);
    }

    @Override
    public boolean derivedFrom(String namespace, String name, short derivationMethod) {
        return this.type.derivedFrom(namespace, name, derivationMethod);
    }

    @Override
    public boolean derivedFromType(XSTypeDefinition ancestorType, short derivationMethod) {
        return this.type.derivedFromType(ancestorType, derivationMethod);
    }

    @Override
    public boolean getAnonymous() {
        return this.type.getAnonymous();
    }

    @Override
    public XSTypeDefinition getBaseType() {
        return this.type.getBaseType();
    }

    @Override
    public short getFinal() {
        return this.type.getFinal();
    }

    @Override
    public short getTypeCategory() {
        return this.type.getTypeCategory();
    }

    @Override
    public boolean isFinal(short restriction) {
        return this.type.isFinal(restriction);
    }

    @Override
    public String getName() {
        return this.type.getName();
    }

    @Override
    public String getNamespace() {
        return this.type.getNamespace();
    }

    @Override
    public XSNamespaceItem getNamespaceItem() {
        return this.type.getNamespaceItem();
    }

    @Override
    public short getType() {
        return this.type.getType();
    }

    @Override
    public void applyFacets(XSFacets facets, short presentFacet, short fixedFacet, ValidationContext context) throws InvalidDatatypeFacetException {
        this.type.applyFacets(facets, presentFacet, fixedFacet, context);
    }

    @Override
    public short getPrimitiveKind() {
        return this.type.getPrimitiveKind();
    }

    @Override
    public short getWhitespace() throws DatatypeException {
        return this.type.getWhitespace();
    }

    @Override
    public boolean isEqual(Object value1, Object value2) {
        return this.type.isEqual(value1, value2);
    }

    @Override
    public boolean isIDType() {
        return this.type.isIDType();
    }

    @Override
    public void validate(ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {
        this.type.validate(context, validatedInfo);
    }

    @Override
    public Object validate(String content, ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {
        return this.type.validate(content, context, validatedInfo);
    }

    @Override
    public Object validate(Object content, ValidationContext context, ValidatedInfo validatedInfo) throws InvalidDatatypeValueException {
        return this.type.validate(content, context, validatedInfo);
    }

    public String toString() {
        return this.type.toString();
    }
}
