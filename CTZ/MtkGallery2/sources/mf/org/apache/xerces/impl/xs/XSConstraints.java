package mf.org.apache.xerces.impl.xs;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import mf.org.apache.xerces.impl.XMLErrorReporter;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.xs.models.CMBuilder;
import mf.org.apache.xerces.impl.xs.models.XSCMValidator;
import mf.org.apache.xerces.impl.xs.util.SimpleLocator;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.SymbolHash;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSTypeDefinition;

public class XSConstraints {
    static final int OCCURRENCE_UNKNOWN = -2;
    static final XSSimpleType STRING_TYPE = (XSSimpleType) SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(SchemaSymbols.ATTVAL_STRING);
    private static XSParticleDecl fEmptyParticle = null;
    private static final Comparator ELEMENT_PARTICLE_COMPARATOR = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            XSParticleDecl pDecl1 = (XSParticleDecl) o1;
            XSParticleDecl pDecl2 = (XSParticleDecl) o2;
            XSElementDecl decl1 = (XSElementDecl) pDecl1.fValue;
            XSElementDecl decl2 = (XSElementDecl) pDecl2.fValue;
            String namespace1 = decl1.getNamespace();
            String namespace2 = decl2.getNamespace();
            String name1 = decl1.getName();
            String name2 = decl2.getName();
            boolean sameNamespace = namespace1 == namespace2;
            int namespaceComparison = 0;
            if (!sameNamespace) {
                if (namespace1 != null) {
                    if (namespace2 != null) {
                        namespaceComparison = namespace1.compareTo(namespace2);
                    } else {
                        namespaceComparison = 1;
                    }
                } else {
                    namespaceComparison = -1;
                }
            }
            return namespaceComparison != 0 ? namespaceComparison : name1.compareTo(name2);
        }
    };

    public static XSParticleDecl getEmptySequence() {
        if (fEmptyParticle == null) {
            XSModelGroupImpl group = new XSModelGroupImpl();
            group.fCompositor = (short) 102;
            group.fParticleCount = 0;
            group.fParticles = null;
            group.fAnnotations = XSObjectListImpl.EMPTY_LIST;
            XSParticleDecl particle = new XSParticleDecl();
            particle.fType = (short) 3;
            particle.fValue = group;
            particle.fAnnotations = XSObjectListImpl.EMPTY_LIST;
            fEmptyParticle = particle;
        }
        return fEmptyParticle;
    }

    public static boolean checkTypeDerivationOk(XSTypeDefinition derived, XSTypeDefinition base, short block) {
        if (derived == SchemaGrammar.fAnyType) {
            return derived == base;
        }
        if (derived == SchemaGrammar.fAnySimpleType) {
            return base == SchemaGrammar.fAnyType || base == SchemaGrammar.fAnySimpleType;
        }
        if (derived.getTypeCategory() == 16) {
            if (base.getTypeCategory() == 15) {
                if (base != SchemaGrammar.fAnyType) {
                    return false;
                }
                base = SchemaGrammar.fAnySimpleType;
            }
            return checkSimpleDerivation((XSSimpleType) derived, (XSSimpleType) base, block);
        }
        return checkComplexDerivation((XSComplexTypeDecl) derived, base, block);
    }

    public static boolean checkSimpleDerivationOk(XSSimpleType derived, XSTypeDefinition base, short block) {
        if (derived == SchemaGrammar.fAnySimpleType) {
            return base == SchemaGrammar.fAnyType || base == SchemaGrammar.fAnySimpleType;
        }
        if (base.getTypeCategory() == 15) {
            if (base != SchemaGrammar.fAnyType) {
                return false;
            }
            base = SchemaGrammar.fAnySimpleType;
        }
        return checkSimpleDerivation(derived, (XSSimpleType) base, block);
    }

    public static boolean checkComplexDerivationOk(XSComplexTypeDecl derived, XSTypeDefinition base, short block) {
        if (derived == SchemaGrammar.fAnyType) {
            return derived == base;
        }
        return checkComplexDerivation(derived, base, block);
    }

    private static boolean checkSimpleDerivation(XSSimpleType derived, XSSimpleType base, short block) {
        if (derived == base) {
            return true;
        }
        if ((block & 2) != 0 || (derived.getBaseType().getFinal() & 2) != 0) {
            return false;
        }
        XSSimpleType directBase = (XSSimpleType) derived.getBaseType();
        if (directBase == base) {
            return true;
        }
        if (directBase != SchemaGrammar.fAnySimpleType && checkSimpleDerivation(directBase, base, block)) {
            return true;
        }
        if ((derived.getVariety() == 2 || derived.getVariety() == 3) && base == SchemaGrammar.fAnySimpleType) {
            return true;
        }
        if (base.getVariety() == 3) {
            XSObjectList subUnionMemberDV = base.getMemberTypes();
            int subUnionSize = subUnionMemberDV.getLength();
            for (int i = 0; i < subUnionSize; i++) {
                if (checkSimpleDerivation(derived, (XSSimpleType) subUnionMemberDV.item(i), block)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkComplexDerivation(XSComplexTypeDecl derived, XSTypeDefinition base, short block) {
        if (derived == base) {
            return true;
        }
        if ((derived.fDerivedBy & block) != 0) {
            return false;
        }
        XSTypeDefinition directBase = derived.fBaseType;
        if (directBase == base) {
            return true;
        }
        if (directBase == SchemaGrammar.fAnyType || directBase == SchemaGrammar.fAnySimpleType) {
            return false;
        }
        if (directBase.getTypeCategory() == 15) {
            return checkComplexDerivation((XSComplexTypeDecl) directBase, base, block);
        }
        if (directBase.getTypeCategory() != 16) {
            return false;
        }
        if (base.getTypeCategory() == 15) {
            if (base != SchemaGrammar.fAnyType) {
                return false;
            }
            base = SchemaGrammar.fAnySimpleType;
        }
        return checkSimpleDerivation((XSSimpleType) directBase, (XSSimpleType) base, block);
    }

    public static Object ElementDefaultValidImmediate(XSTypeDefinition type, String value, ValidationContext context, ValidatedInfo vinfo) {
        XSSimpleType dv = null;
        if (type.getTypeCategory() == 16) {
            dv = (XSSimpleType) type;
        } else {
            XSComplexTypeDecl ctype = (XSComplexTypeDecl) type;
            if (ctype.fContentType == 1) {
                dv = ctype.fXSSimpleType;
            } else if (ctype.fContentType != 3 || !((XSParticleDecl) ctype.getParticle()).emptiable()) {
                return null;
            }
        }
        if (dv == null) {
            dv = STRING_TYPE;
        }
        try {
            Object actualValue = dv.validate(value, context, vinfo);
            if (vinfo == null) {
                return actualValue;
            }
            Object actualValue2 = dv.validate(vinfo.stringValue(), context, vinfo);
            return actualValue2;
        } catch (InvalidDatatypeValueException e) {
            return null;
        }
    }

    static void reportSchemaError(XMLErrorReporter errorReporter, SimpleLocator loc, String key, Object[] args) {
        if (loc != null) {
            errorReporter.reportError((XMLLocator) loc, XSMessageFormatter.SCHEMA_DOMAIN, key, args, (short) 1);
        } else {
            errorReporter.reportError(XSMessageFormatter.SCHEMA_DOMAIN, key, args, (short) 1);
        }
    }

    public static void fullSchemaChecking(XSGrammarBucket xSGrammarBucket, SubstitutionGroupHandler substitutionGroupHandler, CMBuilder cMBuilder, XMLErrorReporter xMLErrorReporter) {
        SymbolHash symbolHash;
        boolean zCheckUniqueParticleAttribution;
        XSGroupDecl[] xSGroupDeclArr;
        XSParticleDecl xSParticleDecl;
        SchemaGrammar[] grammars = xSGrammarBucket.getGrammars();
        int i = 1;
        int length = grammars.length - 1;
        while (length >= 0) {
            boolean z = i == true ? 1 : 0;
            substitutionGroupHandler.addSubstitutionGroup(grammars[length].getSubstitutionGroups());
            length--;
            i = z ? 1 : 0;
        }
        XSParticleDecl xSParticleDecl2 = new XSParticleDecl();
        XSParticleDecl xSParticleDecl3 = new XSParticleDecl();
        xSParticleDecl2.fType = (short) 3;
        xSParticleDecl3.fType = (short) 3;
        int length2 = grammars.length - (i == true ? 1 : 0);
        int i2 = i;
        while (length2 >= 0) {
            XSGroupDecl[] redefinedGroupDecls = grammars[length2].getRedefinedGroupDecls();
            SimpleLocator[] rGLocators = grammars[length2].getRGLocators();
            int i3 = 0;
            while (i3 < redefinedGroupDecls.length) {
                int i4 = i3 + 1;
                XSGroupDecl xSGroupDecl = redefinedGroupDecls[i3];
                XSModelGroupImpl xSModelGroupImpl = xSGroupDecl.fModelGroup;
                int i5 = i4 + 1;
                XSModelGroupImpl xSModelGroupImpl2 = redefinedGroupDecls[i4].fModelGroup;
                xSParticleDecl2.fValue = xSModelGroupImpl;
                xSParticleDecl3.fValue = xSModelGroupImpl2;
                if (xSModelGroupImpl2 == null) {
                    if (xSModelGroupImpl != null) {
                        xSGroupDeclArr = redefinedGroupDecls;
                        reportSchemaError(xMLErrorReporter, rGLocators[(i5 / 2) - 1], "src-redefine.6.2.2", new Object[]{xSGroupDecl.fName, "rcase-Recurse.2"});
                        xSParticleDecl = xSParticleDecl2;
                    } else {
                        xSGroupDeclArr = redefinedGroupDecls;
                        xSParticleDecl = xSParticleDecl2;
                    }
                } else {
                    xSGroupDeclArr = redefinedGroupDecls;
                    if (xSModelGroupImpl == null) {
                        if (!xSParticleDecl3.emptiable()) {
                            reportSchemaError(xMLErrorReporter, rGLocators[(i5 / 2) - 1], "src-redefine.6.2.2", new Object[]{xSGroupDecl.fName, "rcase-Recurse.2"});
                        }
                    } else {
                        try {
                            particleValidRestriction(xSParticleDecl2, substitutionGroupHandler, xSParticleDecl3, substitutionGroupHandler);
                        } catch (XMLSchemaException e) {
                            String key = e.getKey();
                            reportSchemaError(xMLErrorReporter, rGLocators[(i5 / 2) - 1], key, e.getArgs());
                            xSParticleDecl = xSParticleDecl2;
                            reportSchemaError(xMLErrorReporter, rGLocators[(i5 / 2) - 1], "src-redefine.6.2.2", new Object[]{xSGroupDecl.fName, key});
                        }
                    }
                    xSParticleDecl = xSParticleDecl2;
                }
                i3 = i5;
                redefinedGroupDecls = xSGroupDeclArr;
                xSParticleDecl2 = xSParticleDecl;
            }
            length2--;
            i2 = 1;
        }
        SymbolHash symbolHash2 = new SymbolHash();
        int length3 = grammars.length - i2;
        ?? r4 = i2;
        while (length3 >= 0) {
            boolean z2 = grammars[length3].fFullChecked;
            XSComplexTypeDecl[] uncheckedComplexTypeDecls = grammars[length3].getUncheckedComplexTypeDecls();
            SimpleLocator[] uncheckedCTLocators = grammars[length3].getUncheckedCTLocators();
            int i6 = 0;
            int i7 = 0;
            boolean z3 = r4;
            while (i6 < uncheckedComplexTypeDecls.length) {
                if (!z2 && uncheckedComplexTypeDecls[i6].fParticle != null) {
                    symbolHash2.clear();
                    try {
                        checkElementDeclsConsistent(uncheckedComplexTypeDecls[i6], uncheckedComplexTypeDecls[i6].fParticle, symbolHash2, substitutionGroupHandler);
                    } catch (XMLSchemaException e2) {
                        reportSchemaError(xMLErrorReporter, uncheckedCTLocators[i6], e2.getKey(), e2.getArgs());
                    }
                }
                if (uncheckedComplexTypeDecls[i6].fBaseType != null && uncheckedComplexTypeDecls[i6].fBaseType != SchemaGrammar.fAnyType && uncheckedComplexTypeDecls[i6].fDerivedBy == 2 && (uncheckedComplexTypeDecls[i6].fBaseType instanceof XSComplexTypeDecl)) {
                    XSParticleDecl xSParticleDecl4 = uncheckedComplexTypeDecls[i6].fParticle;
                    XSParticleDecl xSParticleDecl5 = ((XSComplexTypeDecl) uncheckedComplexTypeDecls[i6].fBaseType).fParticle;
                    if (xSParticleDecl4 != null) {
                        symbolHash = symbolHash2;
                        if (xSParticleDecl5 != null) {
                            try {
                                particleValidRestriction(uncheckedComplexTypeDecls[i6].fParticle, substitutionGroupHandler, ((XSComplexTypeDecl) uncheckedComplexTypeDecls[i6].fBaseType).fParticle, substitutionGroupHandler);
                            } catch (XMLSchemaException e3) {
                                reportSchemaError(xMLErrorReporter, uncheckedCTLocators[i6], e3.getKey(), e3.getArgs());
                                reportSchemaError(xMLErrorReporter, uncheckedCTLocators[i6], "derivation-ok-restriction.5.4.2", new Object[]{uncheckedComplexTypeDecls[i6].fName});
                            }
                        } else {
                            reportSchemaError(xMLErrorReporter, uncheckedCTLocators[i6], "derivation-ok-restriction.5.4.2", new Object[]{uncheckedComplexTypeDecls[i6].fName});
                        }
                    } else if (xSParticleDecl5 != null && !xSParticleDecl5.emptiable()) {
                        symbolHash = symbolHash2;
                        reportSchemaError(xMLErrorReporter, uncheckedCTLocators[i6], "derivation-ok-restriction.5.3.2", new Object[]{uncheckedComplexTypeDecls[i6].fName, uncheckedComplexTypeDecls[i6].fBaseType.getName()});
                    }
                } else {
                    symbolHash = symbolHash2;
                }
                XSCMValidator contentModel = uncheckedComplexTypeDecls[i6].getContentModel(cMBuilder, true);
                if (contentModel != null) {
                    try {
                        zCheckUniqueParticleAttribution = contentModel.checkUniqueParticleAttribution(substitutionGroupHandler);
                    } catch (XMLSchemaException e4) {
                        zCheckUniqueParticleAttribution = false;
                        reportSchemaError(xMLErrorReporter, uncheckedCTLocators[i6], e4.getKey(), e4.getArgs());
                    }
                } else {
                    zCheckUniqueParticleAttribution = false;
                }
                if (!z2 && zCheckUniqueParticleAttribution) {
                    uncheckedComplexTypeDecls[i7] = uncheckedComplexTypeDecls[i6];
                    i7++;
                }
                i6++;
                symbolHash2 = symbolHash;
                z3 = 1;
            }
            if (!z2) {
                grammars[length3].setUncheckedTypeNum(i7);
                grammars[length3].fFullChecked = z3;
            }
            length3--;
            r4 = z3;
        }
    }

    public static void checkElementDeclsConsistent(XSComplexTypeDecl type, XSParticleDecl particle, SymbolHash elemDeclHash, SubstitutionGroupHandler sgHandler) throws XMLSchemaException {
        int pType = particle.fType;
        if (pType == 2) {
            return;
        }
        if (pType == 1) {
            XSElementDecl elem = (XSElementDecl) particle.fValue;
            findElemInTable(type, elem, elemDeclHash);
            if (elem.fScope == 1) {
                XSElementDecl[] subGroup = sgHandler.getSubstitutionGroup(elem);
                for (XSElementDecl xSElementDecl : subGroup) {
                    findElemInTable(type, xSElementDecl, elemDeclHash);
                }
                return;
            }
            return;
        }
        XSModelGroupImpl group = (XSModelGroupImpl) particle.fValue;
        for (int i = 0; i < group.fParticleCount; i++) {
            checkElementDeclsConsistent(type, group.fParticles[i], elemDeclHash, sgHandler);
        }
    }

    public static void findElemInTable(XSComplexTypeDecl type, XSElementDecl elem, SymbolHash elemDeclHash) throws XMLSchemaException {
        String name = String.valueOf(elem.fName) + "," + elem.fTargetNamespace;
        XSElementDecl existingElem = (XSElementDecl) elemDeclHash.get(name);
        if (existingElem == null) {
            elemDeclHash.put(name, elem);
        } else if (elem != existingElem && elem.fType != existingElem.fType) {
            throw new XMLSchemaException("cos-element-consistent", new Object[]{type.fName, elem.fName});
        }
    }

    private static boolean particleValidRestriction(XSParticleDecl dParticle, SubstitutionGroupHandler dSGHandler, XSParticleDecl bParticle, SubstitutionGroupHandler bSGHandler) throws XMLSchemaException {
        return particleValidRestriction(dParticle, dSGHandler, bParticle, bSGHandler, true);
    }

    private static boolean particleValidRestriction(XSParticleDecl dParticle, SubstitutionGroupHandler dSGHandler, XSParticleDecl bParticle, SubstitutionGroupHandler bSGHandler, boolean checkWCOccurrence) throws XMLSchemaException {
        Vector bChildren;
        boolean bExpansionHappened;
        SubstitutionGroupHandler bSGHandler2;
        int dMinEffectiveTotalRange;
        int dMaxEffectiveTotalRange;
        int dMaxEffectiveTotalRange2;
        int max1;
        int dMinEffectiveTotalRange2;
        int dMaxEffectiveTotalRange3;
        short bType;
        XSParticleDecl dParticle2 = dParticle;
        SubstitutionGroupHandler dSGHandler2 = dSGHandler;
        XSParticleDecl bParticle2 = bParticle;
        Vector dChildren = null;
        Vector bChildren2 = null;
        int i = -2;
        int dMaxEffectiveTotalRange4 = -2;
        if (dParticle.isEmpty() && !bParticle.emptiable()) {
            throw new XMLSchemaException("cos-particle-restrict.a", null);
        }
        if (!dParticle.isEmpty() && bParticle.isEmpty()) {
            throw new XMLSchemaException("cos-particle-restrict.b", null);
        }
        short dType = dParticle2.fType;
        if (dType == 3) {
            dType = ((XSModelGroupImpl) dParticle2.fValue).fCompositor;
            XSParticleDecl dtmp = getNonUnaryGroup(dParticle);
            if (dtmp != dParticle2 && (dType = (dParticle2 = dtmp).fType) == 3) {
                dType = ((XSModelGroupImpl) dParticle2.fValue).fCompositor;
            }
            dChildren = removePointlessChildren(dParticle2);
        }
        int dMinOccurs = dParticle2.fMinOccurs;
        int dMaxOccurs = dParticle2.fMaxOccurs;
        if (dSGHandler2 != null && dType == 1) {
            XSElementDecl dElement = (XSElementDecl) dParticle2.fValue;
            if (dElement.fScope == 1) {
                XSElementDecl[] subGroup = dSGHandler2.getSubstitutionGroup(dElement);
                if (subGroup.length > 0) {
                    dChildren = new Vector(subGroup.length + 1);
                    for (XSElementDecl xSElementDecl : subGroup) {
                        addElementToParticleVector(dChildren, xSElementDecl);
                    }
                    addElementToParticleVector(dChildren, dElement);
                    Collections.sort(dChildren, ELEMENT_PARTICLE_COMPARATOR);
                    dSGHandler2 = null;
                    dType = 101;
                    i = dMinOccurs;
                    dMaxEffectiveTotalRange4 = dMaxOccurs;
                }
            }
        }
        short bType2 = bParticle2.fType;
        if (bType2 == 3) {
            bType2 = ((XSModelGroupImpl) bParticle2.fValue).fCompositor;
            XSParticleDecl btmp = getNonUnaryGroup(bParticle);
            if (btmp != bParticle2 && (bType2 = (bParticle2 = btmp).fType) == 3) {
                bType2 = ((XSModelGroupImpl) bParticle2.fValue).fCompositor;
            }
            bChildren2 = removePointlessChildren(bParticle2);
        }
        int bMinOccurs = bParticle2.fMinOccurs;
        int bMaxOccurs = bParticle2.fMaxOccurs;
        if (bSGHandler != null && bType2 == 1) {
            XSElementDecl bElement = (XSElementDecl) bParticle2.fValue;
            bChildren = bChildren2;
            bExpansionHappened = false;
            if (bElement.fScope == 1) {
                XSElementDecl[] bsubGroup = bSGHandler.getSubstitutionGroup(bElement);
                if (bsubGroup.length > 0) {
                    short bType3 = 101;
                    Vector bChildren3 = new Vector(bsubGroup.length + 1);
                    int i2 = 0;
                    while (true) {
                        bType = bType3;
                        if (i2 >= bsubGroup.length) {
                            break;
                        }
                        addElementToParticleVector(bChildren3, bsubGroup[i2]);
                        i2++;
                        bType3 = bType;
                    }
                    addElementToParticleVector(bChildren3, bElement);
                    Collections.sort(bChildren3, ELEMENT_PARTICLE_COMPARATOR);
                    bChildren = bChildren3;
                    bExpansionHappened = true;
                    bSGHandler2 = null;
                    bType2 = bType;
                }
            }
            switch (dType) {
                case 1:
                    switch (bType2) {
                        case 1:
                            checkNameAndTypeOK((XSElementDecl) dParticle2.fValue, dMinOccurs, dMaxOccurs, (XSElementDecl) bParticle2.fValue, bMinOccurs, bMaxOccurs);
                            return bExpansionHappened;
                        case 2:
                            checkNSCompat((XSElementDecl) dParticle2.fValue, dMinOccurs, dMaxOccurs, (XSWildcardDecl) bParticle2.fValue, bMinOccurs, bMaxOccurs, checkWCOccurrence);
                            return bExpansionHappened;
                        default:
                            switch (bType2) {
                                case 101:
                                    Vector dChildren2 = new Vector();
                                    dChildren2.addElement(dParticle2);
                                    checkRecurseLax(dChildren2, 1, 1, dSGHandler2, bChildren, bMinOccurs, bMaxOccurs, bSGHandler2);
                                    return bExpansionHappened;
                                case 102:
                                case 103:
                                    Vector dChildren3 = new Vector();
                                    dChildren3.addElement(dParticle2);
                                    checkRecurse(dChildren3, 1, 1, dSGHandler2, bChildren, bMinOccurs, bMaxOccurs, bSGHandler2);
                                    return bExpansionHappened;
                                default:
                                    throw new XMLSchemaException("Internal-Error", new Object[]{"in particleValidRestriction"});
                            }
                    }
                case 2:
                    switch (bType2) {
                        case 1:
                            break;
                        case 2:
                            checkNSSubset((XSWildcardDecl) dParticle2.fValue, dMinOccurs, dMaxOccurs, (XSWildcardDecl) bParticle2.fValue, bMinOccurs, bMaxOccurs);
                            return bExpansionHappened;
                        default:
                            switch (bType2) {
                                case 101:
                                case 102:
                                case 103:
                                    break;
                                default:
                                    throw new XMLSchemaException("Internal-Error", new Object[]{"in particleValidRestriction"});
                            }
                            break;
                    }
                    throw new XMLSchemaException("cos-particle-restrict.2", new Object[]{"any:choice,sequence,all,elt"});
                default:
                    switch (dType) {
                        case 101:
                            switch (bType2) {
                                case 1:
                                    break;
                                case 2:
                                    if (i == -2) {
                                        dMinEffectiveTotalRange = dParticle2.minEffectiveTotalRange();
                                    } else {
                                        dMinEffectiveTotalRange = i;
                                    }
                                    if (dMaxEffectiveTotalRange4 == -2) {
                                        dMaxEffectiveTotalRange = dParticle2.maxEffectiveTotalRange();
                                    } else {
                                        dMaxEffectiveTotalRange = dMaxEffectiveTotalRange4;
                                    }
                                    checkNSRecurseCheckCardinality(dChildren, dMinEffectiveTotalRange, dMaxEffectiveTotalRange, dSGHandler2, bParticle2, bMinOccurs, bMaxOccurs, checkWCOccurrence);
                                    return bExpansionHappened;
                                default:
                                    switch (bType2) {
                                        case 101:
                                            checkRecurseLax(dChildren, dMinOccurs, dMaxOccurs, dSGHandler2, bChildren, bMinOccurs, bMaxOccurs, bSGHandler2);
                                            return bExpansionHappened;
                                        case 102:
                                        case 103:
                                            break;
                                        default:
                                            throw new XMLSchemaException("Internal-Error", new Object[]{"in particleValidRestriction"});
                                    }
                                    break;
                            }
                            throw new XMLSchemaException("cos-particle-restrict.2", new Object[]{"choice:all,sequence,elt"});
                        case 102:
                            switch (bType2) {
                                case 1:
                                    throw new XMLSchemaException("cos-particle-restrict.2", new Object[]{"seq:elt"});
                                case 2:
                                    if (i == -2) {
                                        i = dParticle2.minEffectiveTotalRange();
                                    }
                                    if (dMaxEffectiveTotalRange4 == -2) {
                                        dMaxEffectiveTotalRange2 = dParticle2.maxEffectiveTotalRange();
                                    } else {
                                        dMaxEffectiveTotalRange2 = dMaxEffectiveTotalRange4;
                                    }
                                    checkNSRecurseCheckCardinality(dChildren, i, dMaxEffectiveTotalRange2, dSGHandler2, bParticle2, bMinOccurs, bMaxOccurs, checkWCOccurrence);
                                    return bExpansionHappened;
                                default:
                                    switch (bType2) {
                                        case 101:
                                            int min1 = dChildren.size() * dMinOccurs;
                                            if (dMaxOccurs != -1) {
                                                max1 = dMaxOccurs * dChildren.size();
                                            } else {
                                                max1 = dMaxOccurs;
                                            }
                                            checkMapAndSum(dChildren, min1, max1, dSGHandler2, bChildren, bMinOccurs, bMaxOccurs, bSGHandler2);
                                            return bExpansionHappened;
                                        case 102:
                                            checkRecurse(dChildren, dMinOccurs, dMaxOccurs, dSGHandler2, bChildren, bMinOccurs, bMaxOccurs, bSGHandler2);
                                            return bExpansionHappened;
                                        case 103:
                                            checkRecurseUnordered(dChildren, dMinOccurs, dMaxOccurs, dSGHandler2, bChildren, bMinOccurs, bMaxOccurs, bSGHandler2);
                                            return bExpansionHappened;
                                        default:
                                            throw new XMLSchemaException("Internal-Error", new Object[]{"in particleValidRestriction"});
                                    }
                            }
                        case 103:
                            switch (bType2) {
                                case 1:
                                    break;
                                case 2:
                                    if (i == -2) {
                                        dMinEffectiveTotalRange2 = dParticle2.minEffectiveTotalRange();
                                    } else {
                                        dMinEffectiveTotalRange2 = i;
                                    }
                                    if (dMaxEffectiveTotalRange4 == -2) {
                                        dMaxEffectiveTotalRange3 = dParticle2.maxEffectiveTotalRange();
                                    } else {
                                        dMaxEffectiveTotalRange3 = dMaxEffectiveTotalRange4;
                                    }
                                    checkNSRecurseCheckCardinality(dChildren, dMinEffectiveTotalRange2, dMaxEffectiveTotalRange3, dSGHandler2, bParticle2, bMinOccurs, bMaxOccurs, checkWCOccurrence);
                                    return bExpansionHappened;
                                default:
                                    switch (bType2) {
                                        case 101:
                                        case 102:
                                            break;
                                        case 103:
                                            checkRecurse(dChildren, dMinOccurs, dMaxOccurs, dSGHandler2, bChildren, bMinOccurs, bMaxOccurs, bSGHandler2);
                                            return bExpansionHappened;
                                        default:
                                            throw new XMLSchemaException("Internal-Error", new Object[]{"in particleValidRestriction"});
                                    }
                                    break;
                            }
                            throw new XMLSchemaException("cos-particle-restrict.2", new Object[]{"all:choice,sequence,elt"});
                        default:
                            return bExpansionHappened;
                    }
            }
        }
        bChildren = bChildren2;
        bExpansionHappened = false;
        bSGHandler2 = bSGHandler;
        switch (dType) {
        }
    }

    private static void addElementToParticleVector(Vector v, XSElementDecl d) {
        XSParticleDecl p = new XSParticleDecl();
        p.fValue = d;
        p.fType = (short) 1;
        v.addElement(p);
    }

    private static XSParticleDecl getNonUnaryGroup(XSParticleDecl p) {
        if (p.fType != 1 && p.fType != 2 && p.fMinOccurs == 1 && p.fMaxOccurs == 1 && p.fValue != null && ((XSModelGroupImpl) p.fValue).fParticleCount == 1) {
            return getNonUnaryGroup(((XSModelGroupImpl) p.fValue).fParticles[0]);
        }
        return p;
    }

    private static Vector removePointlessChildren(XSParticleDecl p) {
        if (p.fType == 1 || p.fType == 2) {
            return null;
        }
        Vector children = new Vector();
        XSModelGroupImpl group = (XSModelGroupImpl) p.fValue;
        for (int i = 0; i < group.fParticleCount; i++) {
            gatherChildren(group.fCompositor, group.fParticles[i], children);
        }
        return children;
    }

    private static void gatherChildren(int parentType, XSParticleDecl p, Vector children) {
        int min = p.fMinOccurs;
        int max = p.fMaxOccurs;
        int type = p.fType;
        if (type == 3) {
            type = ((XSModelGroupImpl) p.fValue).fCompositor;
        }
        if (type == 1 || type == 2) {
            children.addElement(p);
            return;
        }
        if (min != 1 || max != 1) {
            children.addElement(p);
            return;
        }
        if (parentType == type) {
            XSModelGroupImpl group = (XSModelGroupImpl) p.fValue;
            for (int i = 0; i < group.fParticleCount; i++) {
                gatherChildren(type, group.fParticles[i], children);
            }
            return;
        }
        if (!p.isEmpty()) {
            children.addElement(p);
        }
    }

    private static void checkNameAndTypeOK(XSElementDecl dElement, int dMin, int dMax, XSElementDecl bElement, int bMin, int bMax) throws XMLSchemaException {
        if (dElement.fName != bElement.fName || dElement.fTargetNamespace != bElement.fTargetNamespace) {
            throw new XMLSchemaException("rcase-NameAndTypeOK.1", new Object[]{dElement.fName, dElement.fTargetNamespace, bElement.fName, bElement.fTargetNamespace});
        }
        if (!bElement.getNillable() && dElement.getNillable()) {
            throw new XMLSchemaException("rcase-NameAndTypeOK.2", new Object[]{dElement.fName});
        }
        if (!checkOccurrenceRange(dMin, dMax, bMin, bMax)) {
            Object[] objArr = new Object[5];
            objArr[0] = dElement.fName;
            objArr[1] = Integer.toString(dMin);
            objArr[2] = dMax == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(dMax);
            objArr[3] = Integer.toString(bMin);
            objArr[4] = bMax == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(bMax);
            throw new XMLSchemaException("rcase-NameAndTypeOK.3", objArr);
        }
        if (bElement.getConstraintType() == 2) {
            if (dElement.getConstraintType() != 2) {
                throw new XMLSchemaException("rcase-NameAndTypeOK.4.a", new Object[]{dElement.fName, bElement.fDefault.stringValue()});
            }
            boolean isSimple = false;
            if (dElement.fType.getTypeCategory() == 16 || ((XSComplexTypeDecl) dElement.fType).fContentType == 1) {
                isSimple = true;
            }
            if ((!isSimple && !bElement.fDefault.normalizedValue.equals(dElement.fDefault.normalizedValue)) || (isSimple && !bElement.fDefault.actualValue.equals(dElement.fDefault.actualValue))) {
                throw new XMLSchemaException("rcase-NameAndTypeOK.4.b", new Object[]{dElement.fName, dElement.fDefault.stringValue(), bElement.fDefault.stringValue()});
            }
        }
        checkIDConstraintRestriction(dElement, bElement);
        int blockSet1 = dElement.fBlock;
        int blockSet2 = bElement.fBlock;
        if ((blockSet1 & blockSet2) != blockSet2 || (blockSet1 == 0 && blockSet2 != 0)) {
            throw new XMLSchemaException("rcase-NameAndTypeOK.6", new Object[]{dElement.fName});
        }
        if (!checkTypeDerivationOk(dElement.fType, bElement.fType, (short) 25)) {
            throw new XMLSchemaException("rcase-NameAndTypeOK.7", new Object[]{dElement.fName, dElement.fType.getName(), bElement.fType.getName()});
        }
    }

    private static void checkIDConstraintRestriction(XSElementDecl derivedElemDecl, XSElementDecl baseElemDecl) throws XMLSchemaException {
    }

    private static boolean checkOccurrenceRange(int min1, int max1, int min2, int max2) {
        if (min1 >= min2) {
            if (max2 != -1) {
                if (max1 != -1 && max1 <= max2) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    private static void checkNSCompat(XSElementDecl elem, int min1, int max1, XSWildcardDecl wildcard, int min2, int max2, boolean checkWCOccurrence) throws XMLSchemaException {
        if (checkWCOccurrence && !checkOccurrenceRange(min1, max1, min2, max2)) {
            Object[] objArr = new Object[5];
            objArr[0] = elem.fName;
            objArr[1] = Integer.toString(min1);
            objArr[2] = max1 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max1);
            objArr[3] = Integer.toString(min2);
            objArr[4] = max2 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max2);
            throw new XMLSchemaException("rcase-NSCompat.2", objArr);
        }
        if (!wildcard.allowNamespace(elem.fTargetNamespace)) {
            throw new XMLSchemaException("rcase-NSCompat.1", new Object[]{elem.fName, elem.fTargetNamespace});
        }
    }

    private static void checkNSSubset(XSWildcardDecl dWildcard, int min1, int max1, XSWildcardDecl bWildcard, int min2, int max2) throws XMLSchemaException {
        if (!checkOccurrenceRange(min1, max1, min2, max2)) {
            Object[] objArr = new Object[4];
            objArr[0] = Integer.toString(min1);
            objArr[1] = max1 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max1);
            objArr[2] = Integer.toString(min2);
            objArr[3] = max2 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max2);
            throw new XMLSchemaException("rcase-NSSubset.2", objArr);
        }
        if (!dWildcard.isSubsetOf(bWildcard)) {
            throw new XMLSchemaException("rcase-NSSubset.1", null);
        }
        if (dWildcard.weakerProcessContents(bWildcard)) {
            throw new XMLSchemaException("rcase-NSSubset.3", new Object[]{dWildcard.getProcessContentsAsString(), bWildcard.getProcessContentsAsString()});
        }
    }

    private static void checkNSRecurseCheckCardinality(Vector children, int min1, int max1, SubstitutionGroupHandler dSGHandler, XSParticleDecl wildcard, int min2, int max2, boolean checkWCOccurrence) throws XMLSchemaException {
        if (checkWCOccurrence && !checkOccurrenceRange(min1, max1, min2, max2)) {
            Object[] objArr = new Object[4];
            objArr[0] = Integer.toString(min1);
            objArr[1] = max1 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max1);
            objArr[2] = Integer.toString(min2);
            objArr[3] = max2 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max2);
            throw new XMLSchemaException("rcase-NSRecurseCheckCardinality.2", objArr);
        }
        int count = children.size();
        for (int i = 0; i < count; i++) {
            try {
                XSParticleDecl particle1 = (XSParticleDecl) children.elementAt(i);
                particleValidRestriction(particle1, dSGHandler, wildcard, null, false);
            } catch (XMLSchemaException e) {
                throw new XMLSchemaException("rcase-NSRecurseCheckCardinality.1", null);
            }
        }
    }

    private static void checkRecurse(Vector dChildren, int min1, int max1, SubstitutionGroupHandler dSGHandler, Vector bChildren, int min2, int max2, SubstitutionGroupHandler bSGHandler) throws XMLSchemaException {
        int current;
        if (!checkOccurrenceRange(min1, max1, min2, max2)) {
            Object[] objArr = new Object[4];
            objArr[0] = Integer.toString(min1);
            objArr[1] = max1 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max1);
            objArr[2] = Integer.toString(min2);
            objArr[3] = max2 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max2);
            throw new XMLSchemaException("rcase-Recurse.1", objArr);
        }
        int count1 = dChildren.size();
        int count2 = bChildren.size();
        int current2 = 0;
        int i = 0;
        while (true) {
            Object[] objArr2 = null;
            if (i < count1) {
                XSParticleDecl particle1 = (XSParticleDecl) dChildren.elementAt(i);
                int j = current2;
                while (j < count2) {
                    XSParticleDecl particle2 = (XSParticleDecl) bChildren.elementAt(j);
                    current = current2 + 1;
                    try {
                        particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler);
                        break;
                    } catch (XMLSchemaException e) {
                        if (!particle2.emptiable()) {
                            throw new XMLSchemaException("rcase-Recurse.2", null);
                        }
                        j++;
                        current2 = current;
                        objArr2 = null;
                    }
                }
                throw new XMLSchemaException("rcase-Recurse.2", objArr2);
            }
            for (int j2 = current2; j2 < count2; j2++) {
                if (!((XSParticleDecl) bChildren.elementAt(j2)).emptiable()) {
                    throw new XMLSchemaException("rcase-Recurse.2", null);
                }
            }
            return;
            i++;
            current2 = current;
        }
    }

    private static void checkRecurseUnordered(Vector dChildren, int min1, int max1, SubstitutionGroupHandler dSGHandler, Vector bChildren, int min2, int max2, SubstitutionGroupHandler bSGHandler) throws XMLSchemaException {
        boolean z;
        Vector vector = bChildren;
        if (!checkOccurrenceRange(min1, max1, min2, max2)) {
            Object[] objArr = new Object[4];
            objArr[0] = Integer.toString(min1);
            objArr[1] = max1 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max1);
            objArr[2] = Integer.toString(min2);
            objArr[3] = max2 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max2);
            throw new XMLSchemaException("rcase-RecurseUnordered.1", objArr);
        }
        int count1 = dChildren.size();
        int count2 = bChildren.size();
        boolean[] foundIt = new boolean[count2];
        int i = 0;
        while (i < count1) {
            XSParticleDecl particle1 = (XSParticleDecl) dChildren.elementAt(i);
            int j = 0;
            while (j < count2) {
                XSParticleDecl particle2 = (XSParticleDecl) vector.elementAt(j);
                try {
                    particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler);
                    if (foundIt[j]) {
                        z = true;
                        throw new XMLSchemaException("rcase-RecurseUnordered.2", null);
                    }
                    z = true;
                    try {
                        break;
                    } catch (XMLSchemaException e) {
                        j++;
                        vector = bChildren;
                    }
                } catch (XMLSchemaException e2) {
                    z = true;
                }
                j++;
                vector = bChildren;
            }
            throw new XMLSchemaException("rcase-RecurseUnordered.2", null);
        }
        for (int j2 = 0; j2 < count2; j2++) {
            XSParticleDecl particle22 = (XSParticleDecl) vector.elementAt(j2);
            if (!foundIt[j2] && !particle22.emptiable()) {
                throw new XMLSchemaException("rcase-RecurseUnordered.2", null);
            }
        }
    }

    private static void checkRecurseLax(Vector dChildren, int min1, int max1, SubstitutionGroupHandler dSGHandler, Vector bChildren, int min2, int max2, SubstitutionGroupHandler bSGHandler) throws XMLSchemaException {
        if (!checkOccurrenceRange(min1, max1, min2, max2)) {
            Object[] objArr = new Object[4];
            objArr[0] = Integer.toString(min1);
            objArr[1] = max1 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max1);
            objArr[2] = Integer.toString(min2);
            objArr[3] = max2 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max2);
            throw new XMLSchemaException("rcase-RecurseLax.1", objArr);
        }
        int count1 = dChildren.size();
        int count2 = bChildren.size();
        int current = 0;
        for (int i = 0; i < count1; i++) {
            XSParticleDecl particle1 = (XSParticleDecl) dChildren.elementAt(i);
            for (int j = current; j < count2; j++) {
                XSParticleDecl particle2 = (XSParticleDecl) bChildren.elementAt(j);
                current++;
                try {
                    if (particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler)) {
                        current--;
                    }
                } catch (XMLSchemaException e) {
                }
            }
            throw new XMLSchemaException("rcase-RecurseLax.2", null);
        }
    }

    private static void checkMapAndSum(Vector dChildren, int min1, int max1, SubstitutionGroupHandler dSGHandler, Vector bChildren, int min2, int max2, SubstitutionGroupHandler bSGHandler) throws XMLSchemaException {
        if (!checkOccurrenceRange(min1, max1, min2, max2)) {
            Object[] objArr = new Object[4];
            objArr[0] = Integer.toString(min1);
            objArr[1] = max1 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max1);
            objArr[2] = Integer.toString(min2);
            objArr[3] = max2 == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max2);
            throw new XMLSchemaException("rcase-MapAndSum.2", objArr);
        }
        int count1 = dChildren.size();
        int count2 = bChildren.size();
        int i = 0;
        while (i < count1) {
            XSParticleDecl particle1 = (XSParticleDecl) dChildren.elementAt(i);
            for (int j = 0; j < count2; j++) {
                XSParticleDecl particle2 = (XSParticleDecl) bChildren.elementAt(j);
                try {
                    particleValidRestriction(particle1, dSGHandler, particle2, bSGHandler);
                    break;
                } catch (XMLSchemaException e) {
                }
            }
            throw new XMLSchemaException("rcase-MapAndSum.1", null);
        }
    }

    public static boolean overlapUPA(XSElementDecl element1, XSElementDecl element2, SubstitutionGroupHandler sgHandler) {
        if (element1.fName == element2.fName && element1.fTargetNamespace == element2.fTargetNamespace) {
            return true;
        }
        XSElementDecl[] subGroup = sgHandler.getSubstitutionGroup(element1);
        for (int i = subGroup.length - 1; i >= 0; i--) {
            if (subGroup[i].fName == element2.fName && subGroup[i].fTargetNamespace == element2.fTargetNamespace) {
                return true;
            }
        }
        XSElementDecl[] subGroup2 = sgHandler.getSubstitutionGroup(element2);
        for (int i2 = subGroup2.length - 1; i2 >= 0; i2--) {
            if (subGroup2[i2].fName == element1.fName && subGroup2[i2].fTargetNamespace == element1.fTargetNamespace) {
                return true;
            }
        }
        return false;
    }

    public static boolean overlapUPA(XSElementDecl element, XSWildcardDecl wildcard, SubstitutionGroupHandler sgHandler) {
        if (wildcard.allowNamespace(element.fTargetNamespace)) {
            return true;
        }
        XSElementDecl[] subGroup = sgHandler.getSubstitutionGroup(element);
        for (int i = subGroup.length - 1; i >= 0; i--) {
            if (wildcard.allowNamespace(subGroup[i].fTargetNamespace)) {
                return true;
            }
        }
        return false;
    }

    public static boolean overlapUPA(XSWildcardDecl wildcard1, XSWildcardDecl wildcard2) {
        XSWildcardDecl intersect = wildcard1.performIntersectionWith(wildcard2, wildcard1.fProcessContents);
        if (intersect == null || intersect.fType != 3 || intersect.fNamespaceList.length != 0) {
            return true;
        }
        return false;
    }

    public static boolean overlapUPA(Object obj, Object obj2, SubstitutionGroupHandler sgHandler) {
        if (obj instanceof XSElementDecl) {
            if (obj2 instanceof XSElementDecl) {
                return overlapUPA((XSElementDecl) obj, (XSElementDecl) obj2, sgHandler);
            }
            return overlapUPA((XSElementDecl) obj, (XSWildcardDecl) obj2, sgHandler);
        }
        if (obj2 instanceof XSElementDecl) {
            return overlapUPA((XSElementDecl) obj2, (XSWildcardDecl) obj, sgHandler);
        }
        return overlapUPA((XSWildcardDecl) obj, (XSWildcardDecl) obj2);
    }
}
