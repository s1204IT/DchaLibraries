package mf.org.apache.xerces.impl.xs;

import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import mf.org.apache.xerces.impl.xs.models.CMBuilder;
import mf.org.apache.xerces.impl.xs.models.XSCMValidator;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.xs.XSAttributeUse;
import mf.org.apache.xerces.xs.XSComplexTypeDefinition;
import mf.org.apache.xerces.xs.XSNamespaceItem;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSParticle;
import mf.org.apache.xerces.xs.XSSimpleTypeDefinition;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.apache.xerces.xs.XSWildcard;
import mf.org.w3c.dom.TypeInfo;

public class XSComplexTypeDecl implements XSComplexTypeDefinition, TypeInfo {
    private static final short CT_HAS_TYPE_ID = 2;
    private static final short CT_IS_ABSTRACT = 1;
    private static final short CT_IS_ANONYMOUS = 4;
    static final int DERIVATION_ANY = 0;
    static final int DERIVATION_EXTENSION = 2;
    static final int DERIVATION_LIST = 8;
    static final int DERIVATION_RESTRICTION = 1;
    static final int DERIVATION_UNION = 4;
    String fName = null;
    String fTargetNamespace = null;
    XSTypeDefinition fBaseType = null;
    short fDerivedBy = 2;
    short fFinal = 0;
    short fBlock = 0;
    short fMiscFlags = 0;
    XSAttributeGroupDecl fAttrGrp = null;
    short fContentType = 0;
    XSSimpleType fXSSimpleType = null;
    XSParticleDecl fParticle = null;
    XSCMValidator fCMValidator = null;
    XSCMValidator fUPACMValidator = null;
    XSObjectListImpl fAnnotations = null;
    private XSNamespaceItem fNamespaceItem = null;

    public void setValues(String name, String targetNamespace, XSTypeDefinition baseType, short derivedBy, short schemaFinal, short block, short contentType, boolean isAbstract, XSAttributeGroupDecl attrGrp, XSSimpleType simpleType, XSParticleDecl particle, XSObjectListImpl annotations) {
        this.fTargetNamespace = targetNamespace;
        this.fBaseType = baseType;
        this.fDerivedBy = derivedBy;
        this.fFinal = schemaFinal;
        this.fBlock = block;
        this.fContentType = contentType;
        if (isAbstract) {
            this.fMiscFlags = (short) (this.fMiscFlags | 1);
        }
        this.fAttrGrp = attrGrp;
        this.fXSSimpleType = simpleType;
        this.fParticle = particle;
        this.fAnnotations = annotations;
    }

    public void setName(String name) {
        this.fName = name;
    }

    @Override
    public short getTypeCategory() {
        return (short) 15;
    }

    public String getTypeName() {
        return this.fName;
    }

    public short getFinalSet() {
        return this.fFinal;
    }

    public String getTargetNamespace() {
        return this.fTargetNamespace;
    }

    public boolean containsTypeID() {
        return (this.fMiscFlags & 2) != 0;
    }

    public void setIsAbstractType() {
        this.fMiscFlags = (short) (this.fMiscFlags | 1);
    }

    public void setContainsTypeID() {
        this.fMiscFlags = (short) (this.fMiscFlags | 2);
    }

    public void setIsAnonymous() {
        this.fMiscFlags = (short) (this.fMiscFlags | 4);
    }

    public XSCMValidator getContentModel(CMBuilder cmBuilder) {
        return getContentModel(cmBuilder, false);
    }

    public synchronized XSCMValidator getContentModel(CMBuilder cmBuilder, boolean forUPA) {
        if (this.fCMValidator == null) {
            if (forUPA) {
                if (this.fUPACMValidator == null) {
                    this.fUPACMValidator = cmBuilder.getContentModel(this, true);
                    if (this.fUPACMValidator != null && !this.fUPACMValidator.isCompactedForUPA()) {
                        this.fCMValidator = this.fUPACMValidator;
                    }
                }
                return this.fUPACMValidator;
            }
            this.fCMValidator = cmBuilder.getContentModel(this, false);
        }
        return this.fCMValidator;
    }

    public XSAttributeGroupDecl getAttrGrp() {
        return this.fAttrGrp;
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        appendTypeInfo(str);
        return str.toString();
    }

    void appendTypeInfo(StringBuffer str) {
        String[] contentType = {"EMPTY", "SIMPLE", "ELEMENT", "MIXED"};
        String[] derivedBy = {"EMPTY", "EXTENSION", "RESTRICTION"};
        str.append("Complex type name='");
        str.append(this.fTargetNamespace);
        str.append(',');
        str.append(getTypeName());
        str.append("', ");
        if (this.fBaseType != null) {
            str.append(" base type name='");
            str.append(this.fBaseType.getName());
            str.append("', ");
        }
        str.append(" content type='");
        str.append(contentType[this.fContentType]);
        str.append("', ");
        str.append(" isAbstract='");
        str.append(getAbstract());
        str.append("', ");
        str.append(" hasTypeId='");
        str.append(containsTypeID());
        str.append("', ");
        str.append(" final='");
        str.append((int) this.fFinal);
        str.append("', ");
        str.append(" block='");
        str.append((int) this.fBlock);
        str.append("', ");
        if (this.fParticle != null) {
            str.append(" particle='");
            str.append(this.fParticle.toString());
            str.append("', ");
        }
        str.append(" derivedBy='");
        str.append(derivedBy[this.fDerivedBy]);
        str.append("'. ");
    }

    @Override
    public boolean derivedFromType(XSTypeDefinition ancestor, short derivationMethod) {
        if (ancestor == null) {
            return false;
        }
        if (ancestor == SchemaGrammar.fAnyType) {
            return true;
        }
        XSTypeDefinition type = this;
        while (type != ancestor && type != SchemaGrammar.fAnySimpleType && type != SchemaGrammar.fAnyType) {
            type = type.getBaseType();
        }
        return type == ancestor;
    }

    @Override
    public boolean derivedFrom(String ancestorNS, String ancestorName, short derivationMethod) {
        if (ancestorName == null) {
            return false;
        }
        if (ancestorNS != null && ancestorNS.equals(SchemaSymbols.URI_SCHEMAFORSCHEMA) && ancestorName.equals(SchemaSymbols.ATTVAL_ANYTYPE)) {
            return true;
        }
        XSTypeDefinition type = this;
        while (true) {
            if ((ancestorName.equals(type.getName()) && ((ancestorNS == null && type.getNamespace() == null) || (ancestorNS != null && ancestorNS.equals(type.getNamespace())))) || type == SchemaGrammar.fAnySimpleType || type == SchemaGrammar.fAnyType) {
                break;
            }
            type = type.getBaseType();
        }
        if (type == SchemaGrammar.fAnySimpleType || type == SchemaGrammar.fAnyType) {
            return false;
        }
        return true;
    }

    public boolean isDOMDerivedFrom(String ancestorNS, String ancestorName, int derivationMethod) {
        if (ancestorName == null) {
            return false;
        }
        if (ancestorNS != null && ancestorNS.equals(SchemaSymbols.URI_SCHEMAFORSCHEMA) && ancestorName.equals(SchemaSymbols.ATTVAL_ANYTYPE) && derivationMethod == 1 && derivationMethod == 2) {
            return true;
        }
        if ((derivationMethod & 1) != 0 && isDerivedByRestriction(ancestorNS, ancestorName, derivationMethod, this)) {
            return true;
        }
        if ((derivationMethod & 2) != 0 && isDerivedByExtension(ancestorNS, ancestorName, derivationMethod, this)) {
            return true;
        }
        if (((derivationMethod & 8) != 0 || (derivationMethod & 4) != 0) && (derivationMethod & 1) == 0 && (derivationMethod & 2) == 0) {
            if (ancestorNS.equals(SchemaSymbols.URI_SCHEMAFORSCHEMA) && ancestorName.equals(SchemaSymbols.ATTVAL_ANYTYPE)) {
                ancestorName = SchemaSymbols.ATTVAL_ANYSIMPLETYPE;
            }
            if (!this.fName.equals(SchemaSymbols.ATTVAL_ANYTYPE) || !this.fTargetNamespace.equals(SchemaSymbols.URI_SCHEMAFORSCHEMA)) {
                if (this.fBaseType != null && (this.fBaseType instanceof XSSimpleTypeDecl)) {
                    return ((XSSimpleTypeDecl) this.fBaseType).isDOMDerivedFrom(ancestorNS, ancestorName, derivationMethod);
                }
                if (this.fBaseType != null && (this.fBaseType instanceof XSComplexTypeDecl)) {
                    return ((XSComplexTypeDecl) this.fBaseType).isDOMDerivedFrom(ancestorNS, ancestorName, derivationMethod);
                }
            }
        }
        if ((derivationMethod & 2) != 0 || (derivationMethod & 1) != 0 || (derivationMethod & 8) != 0 || (derivationMethod & 4) != 0) {
            return false;
        }
        return isDerivedByAny(ancestorNS, ancestorName, derivationMethod, this);
    }

    private boolean isDerivedByAny(String ancestorNS, String ancestorName, int derivationMethod, XSTypeDefinition type) {
        XSTypeDefinition oldType = null;
        while (type != null && type != oldType) {
            if ((ancestorName.equals(type.getName()) && ((ancestorNS == null && type.getNamespace() == null) || (ancestorNS != null && ancestorNS.equals(type.getNamespace())))) || isDerivedByRestriction(ancestorNS, ancestorName, derivationMethod, type) || !isDerivedByExtension(ancestorNS, ancestorName, derivationMethod, type)) {
                return true;
            }
            oldType = type;
            type = type.getBaseType();
        }
        return false;
    }

    private boolean isDerivedByRestriction(String ancestorNS, String ancestorName, int derivationMethod, XSTypeDefinition type) {
        XSTypeDefinition oldType = null;
        while (type != null && type != oldType) {
            if (ancestorNS != null && ancestorNS.equals(SchemaSymbols.URI_SCHEMAFORSCHEMA) && ancestorName.equals(SchemaSymbols.ATTVAL_ANYSIMPLETYPE)) {
                return false;
            }
            if (!ancestorName.equals(type.getName()) || ancestorNS == null || !ancestorNS.equals(type.getNamespace())) {
                if (type.getNamespace() == null && ancestorNS == null) {
                    return true;
                }
                if (type instanceof XSSimpleTypeDecl) {
                    if (ancestorNS.equals(SchemaSymbols.URI_SCHEMAFORSCHEMA) && ancestorName.equals(SchemaSymbols.ATTVAL_ANYTYPE)) {
                        ancestorName = SchemaSymbols.ATTVAL_ANYSIMPLETYPE;
                    }
                    return type.isDOMDerivedFrom(ancestorNS, ancestorName, derivationMethod);
                }
                if (((XSComplexTypeDecl) type).getDerivationMethod() != 2) {
                    return false;
                }
                oldType = type;
                type = type.getBaseType();
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean isDerivedByExtension(String ancestorNS, String ancestorName, int derivationMethod, XSTypeDefinition type) {
        boolean extension = false;
        XSTypeDefinition oldType = null;
        while (type != null && type != oldType) {
            if (ancestorNS == null || !ancestorNS.equals(SchemaSymbols.URI_SCHEMAFORSCHEMA) || !ancestorName.equals(SchemaSymbols.ATTVAL_ANYSIMPLETYPE) || !SchemaSymbols.URI_SCHEMAFORSCHEMA.equals(type.getNamespace()) || !SchemaSymbols.ATTVAL_ANYTYPE.equals(type.getName())) {
                if (ancestorName.equals(type.getName()) && ((ancestorNS == null && type.getNamespace() == null) || (ancestorNS != null && ancestorNS.equals(type.getNamespace())))) {
                    return extension;
                }
                if (type instanceof XSSimpleTypeDecl) {
                    if (ancestorNS.equals(SchemaSymbols.URI_SCHEMAFORSCHEMA) && ancestorName.equals(SchemaSymbols.ATTVAL_ANYTYPE)) {
                        ancestorName = SchemaSymbols.ATTVAL_ANYSIMPLETYPE;
                    }
                    if ((derivationMethod & 2) != 0) {
                        return type.isDOMDerivedFrom(ancestorNS, ancestorName, derivationMethod & 1) & extension;
                    }
                    return type.isDOMDerivedFrom(ancestorNS, ancestorName, derivationMethod) & extension;
                }
                if (((XSComplexTypeDecl) type).getDerivationMethod() == 1) {
                    extension |= true;
                }
                oldType = type;
                type = type.getBaseType();
            } else {
                return false;
            }
        }
        return false;
    }

    public void reset() {
        this.fName = null;
        this.fTargetNamespace = null;
        this.fBaseType = null;
        this.fDerivedBy = (short) 2;
        this.fFinal = (short) 0;
        this.fBlock = (short) 0;
        this.fMiscFlags = (short) 0;
        this.fAttrGrp.reset();
        this.fContentType = (short) 0;
        this.fXSSimpleType = null;
        this.fParticle = null;
        this.fCMValidator = null;
        this.fUPACMValidator = null;
        if (this.fAnnotations != null) {
            this.fAnnotations.clearXSObjectList();
        }
        this.fAnnotations = null;
    }

    @Override
    public short getType() {
        return (short) 3;
    }

    @Override
    public String getName() {
        if (getAnonymous()) {
            return null;
        }
        return this.fName;
    }

    @Override
    public boolean getAnonymous() {
        return (this.fMiscFlags & 4) != 0;
    }

    @Override
    public String getNamespace() {
        return this.fTargetNamespace;
    }

    @Override
    public XSTypeDefinition getBaseType() {
        return this.fBaseType;
    }

    @Override
    public short getDerivationMethod() {
        return this.fDerivedBy;
    }

    @Override
    public boolean isFinal(short derivation) {
        return (this.fFinal & derivation) != 0;
    }

    @Override
    public short getFinal() {
        return this.fFinal;
    }

    @Override
    public boolean getAbstract() {
        return (this.fMiscFlags & 1) != 0;
    }

    @Override
    public XSObjectList getAttributeUses() {
        return this.fAttrGrp.getAttributeUses();
    }

    @Override
    public XSWildcard getAttributeWildcard() {
        return this.fAttrGrp.getAttributeWildcard();
    }

    @Override
    public short getContentType() {
        return this.fContentType;
    }

    @Override
    public XSSimpleTypeDefinition getSimpleType() {
        return this.fXSSimpleType;
    }

    @Override
    public XSParticle getParticle() {
        return this.fParticle;
    }

    @Override
    public boolean isProhibitedSubstitution(short prohibited) {
        return (this.fBlock & prohibited) != 0;
    }

    @Override
    public short getProhibitedSubstitutions() {
        return this.fBlock;
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

    public XSAttributeUse getAttributeUse(String namespace, String name) {
        return this.fAttrGrp.getAttributeUse(namespace, name);
    }

    public String getTypeNamespace() {
        return getNamespace();
    }

    public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod) {
        return isDOMDerivedFrom(typeNamespaceArg, typeNameArg, derivationMethod);
    }
}
