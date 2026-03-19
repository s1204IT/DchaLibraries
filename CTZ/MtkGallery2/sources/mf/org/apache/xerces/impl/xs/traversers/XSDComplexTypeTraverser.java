package mf.org.apache.xerces.impl.xs.traversers;

import mf.org.apache.xerces.impl.dv.InvalidDatatypeFacetException;
import mf.org.apache.xerces.impl.dv.XSFacets;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSAnnotationImpl;
import mf.org.apache.xerces.impl.xs.XSAttributeGroupDecl;
import mf.org.apache.xerces.impl.xs.XSAttributeUseImpl;
import mf.org.apache.xerces.impl.xs.XSComplexTypeDecl;
import mf.org.apache.xerces.impl.xs.XSConstraints;
import mf.org.apache.xerces.impl.xs.XSModelGroupImpl;
import mf.org.apache.xerces.impl.xs.XSParticleDecl;
import mf.org.apache.xerces.impl.xs.XSWildcardDecl;
import mf.org.apache.xerces.impl.xs.traversers.XSDAbstractTraverser;
import mf.org.apache.xerces.impl.xs.util.XInt;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xs.XSAttributeUse;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.Element;

class XSDComplexTypeTraverser extends XSDAbstractParticleTraverser {
    private static final boolean DEBUG = false;
    private static final int GLOBAL_NUM = 11;
    private static XSParticleDecl fErrorContent = null;
    private static XSWildcardDecl fErrorWildcard = null;
    private XSAnnotationImpl[] fAnnotations;
    private XSAttributeGroupDecl fAttrGrp;
    private XSTypeDefinition fBaseType;
    private short fBlock;
    private XSComplexTypeDecl fComplexTypeDecl;
    private short fContentType;
    private short fDerivedBy;
    private short fFinal;
    private Object[] fGlobalStore;
    private int fGlobalStorePos;
    private boolean fIsAbstract;
    private String fName;
    private XSParticleDecl fParticle;
    private String fTargetNamespace;
    private XSSimpleType fXSSimpleType;

    private static XSParticleDecl getErrorContent() {
        if (fErrorContent == null) {
            XSParticleDecl particle = new XSParticleDecl();
            particle.fType = (short) 2;
            particle.fValue = getErrorWildcard();
            particle.fMinOccurs = 0;
            particle.fMaxOccurs = -1;
            XSModelGroupImpl group = new XSModelGroupImpl();
            group.fCompositor = (short) 102;
            group.fParticleCount = 1;
            group.fParticles = new XSParticleDecl[1];
            group.fParticles[0] = particle;
            XSParticleDecl errorContent = new XSParticleDecl();
            errorContent.fType = (short) 3;
            errorContent.fValue = group;
            fErrorContent = errorContent;
        }
        return fErrorContent;
    }

    private static XSWildcardDecl getErrorWildcard() {
        if (fErrorWildcard == null) {
            XSWildcardDecl wildcard = new XSWildcardDecl();
            wildcard.fProcessContents = (short) 2;
            fErrorWildcard = wildcard;
        }
        return fErrorWildcard;
    }

    XSDComplexTypeTraverser(XSDHandler handler, XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
        this.fName = null;
        this.fTargetNamespace = null;
        this.fDerivedBy = (short) 2;
        this.fFinal = (short) 0;
        this.fBlock = (short) 0;
        this.fContentType = (short) 0;
        this.fBaseType = null;
        this.fAttrGrp = null;
        this.fXSSimpleType = null;
        this.fParticle = null;
        this.fIsAbstract = false;
        this.fComplexTypeDecl = null;
        this.fAnnotations = null;
        this.fGlobalStore = null;
        this.fGlobalStorePos = 0;
    }

    private static final class ComplexTypeRecoverableError extends Exception {
        private static final long serialVersionUID = 6802729912091130335L;
        Element errorElem;
        Object[] errorSubstText;

        ComplexTypeRecoverableError() {
            this.errorSubstText = null;
            this.errorElem = null;
        }

        ComplexTypeRecoverableError(String msgKey, Object[] args, Element e) {
            super(msgKey);
            this.errorSubstText = null;
            this.errorElem = null;
            this.errorSubstText = args;
            this.errorElem = e;
        }
    }

    XSComplexTypeDecl traverseLocal(Element complexTypeNode, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        Object[] attrValues = this.fAttrChecker.checkAttributes(complexTypeNode, false, schemaDoc);
        String complexTypeName = genAnonTypeName(complexTypeNode);
        contentBackup();
        XSComplexTypeDecl type = traverseComplexTypeDecl(complexTypeNode, complexTypeName, attrValues, schemaDoc, grammar);
        contentRestore();
        grammar.addComplexTypeDecl(type, this.fSchemaHandler.element2Locator(complexTypeNode));
        type.setIsAnonymous();
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return type;
    }

    XSComplexTypeDecl traverseGlobal(Element element, XSDocumentInfo xSDocumentInfo, SchemaGrammar schemaGrammar) {
        ?? r2;
        Object[] objArrCheckAttributes = this.fAttrChecker.checkAttributes(element, true, xSDocumentInfo);
        String str = (String) objArrCheckAttributes[XSAttributeChecker.ATTIDX_NAME];
        contentBackup();
        XSComplexTypeDecl xSComplexTypeDeclTraverseComplexTypeDecl = traverseComplexTypeDecl(element, str, objArrCheckAttributes, xSDocumentInfo, schemaGrammar);
        contentRestore();
        schemaGrammar.addComplexTypeDecl(xSComplexTypeDeclTraverseComplexTypeDecl, this.fSchemaHandler.element2Locator(element));
        if (str == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_COMPLEXTYPE, SchemaSymbols.ATT_NAME}, element);
            r2 = 0;
        } else {
            if (schemaGrammar.getGlobalTypeDecl(xSComplexTypeDeclTraverseComplexTypeDecl.getName()) == null) {
                schemaGrammar.addGlobalComplexTypeDecl(xSComplexTypeDeclTraverseComplexTypeDecl);
            }
            String strSchemaDocument2SystemId = this.fSchemaHandler.schemaDocument2SystemId(xSDocumentInfo);
            XSTypeDefinition globalTypeDecl = schemaGrammar.getGlobalTypeDecl(xSComplexTypeDeclTraverseComplexTypeDecl.getName(), strSchemaDocument2SystemId);
            if (globalTypeDecl == null) {
                schemaGrammar.addGlobalComplexTypeDecl(xSComplexTypeDeclTraverseComplexTypeDecl, strSchemaDocument2SystemId);
            }
            r2 = xSComplexTypeDeclTraverseComplexTypeDecl;
            XSTypeDefinition xSTypeDefinition = xSComplexTypeDeclTraverseComplexTypeDecl;
            if (this.fSchemaHandler.fTolerateDuplicates) {
                if (globalTypeDecl != null) {
                    xSTypeDefinition = xSComplexTypeDeclTraverseComplexTypeDecl;
                    if (globalTypeDecl instanceof XSComplexTypeDecl) {
                        xSTypeDefinition = globalTypeDecl;
                    }
                }
                this.fSchemaHandler.addGlobalTypeDecl(xSTypeDefinition);
                r2 = xSTypeDefinition;
            }
        }
        this.fAttrChecker.returnAttrArray(objArrCheckAttributes, xSDocumentInfo);
        return r2;
    }

    private XSComplexTypeDecl traverseComplexTypeDecl(Element element, String str, Object[] objArr, XSDocumentInfo xSDocumentInfo, SchemaGrammar schemaGrammar) {
        int i;
        Object obj;
        ?? r8;
        Element firstChildElement;
        Object obj2;
        int i2;
        int i3;
        Element element2;
        int i4;
        this.fComplexTypeDecl = new XSComplexTypeDecl();
        this.fAttrGrp = new XSAttributeGroupDecl();
        Boolean bool = (Boolean) objArr[XSAttributeChecker.ATTIDX_ABSTRACT];
        XInt xInt = (XInt) objArr[XSAttributeChecker.ATTIDX_BLOCK];
        Boolean bool2 = (Boolean) objArr[XSAttributeChecker.ATTIDX_MIXED];
        XInt xInt2 = (XInt) objArr[XSAttributeChecker.ATTIDX_FINAL];
        this.fName = str;
        this.fComplexTypeDecl.setName(this.fName);
        this.fTargetNamespace = xSDocumentInfo.fTargetNamespace;
        this.fBlock = xInt == null ? xSDocumentInfo.fBlockDefault : xInt.shortValue();
        this.fFinal = xInt2 == null ? xSDocumentInfo.fFinalDefault : xInt2.shortValue();
        this.fBlock = (short) (this.fBlock & 3);
        this.fFinal = (short) (this.fFinal & 3);
        this.fIsAbstract = bool != null && bool.booleanValue();
        Element nextSiblingElement = null;
        this.fAnnotations = null;
        try {
            firstChildElement = DOMUtil.getFirstChildElement(element);
            try {
            } catch (ComplexTypeRecoverableError e) {
                e = e;
                obj = obj2;
            }
        } catch (ComplexTypeRecoverableError e2) {
            e = e2;
            i = 0;
        }
        if (firstChildElement != null) {
            try {
                if (DOMUtil.getLocalName(firstChildElement).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    try {
                        addAnnotation(traverseAnnotationDecl(firstChildElement, objArr, false, xSDocumentInfo));
                        nextSiblingElement = DOMUtil.getNextSiblingElement(firstChildElement);
                        i2 = 2;
                        i = 0;
                    } catch (ComplexTypeRecoverableError e3) {
                        e = e3;
                        i = 0;
                        obj = firstChildElement;
                    }
                } else {
                    String syntheticAnnotation = DOMUtil.getSyntheticAnnotation(element);
                    if (syntheticAnnotation != null) {
                        i3 = 2;
                        element2 = firstChildElement;
                        i = 0;
                        addAnnotation(traverseSyntheticAnnotation(element, syntheticAnnotation, objArr, false, xSDocumentInfo));
                    } else {
                        i3 = 2;
                        element2 = firstChildElement;
                        i = 0;
                    }
                    nextSiblingElement = element2;
                    i2 = i3;
                }
            } catch (ComplexTypeRecoverableError e4) {
                e = e4;
                i = 0;
                obj = firstChildElement;
            }
            if (nextSiblingElement != null) {
                try {
                    if (DOMUtil.getLocalName(nextSiblingElement).equals(SchemaSymbols.ELT_ANNOTATION)) {
                        Object[] objArr2 = new Object[i2];
                        objArr2[i] = this.fName;
                        objArr2[1] = SchemaSymbols.ELT_ANNOTATION;
                        throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", objArr2, nextSiblingElement);
                    }
                } catch (ComplexTypeRecoverableError e5) {
                    e = e5;
                    obj = nextSiblingElement;
                }
                handleComplexTypeError(e.getMessage(), e.errorSubstText, e.errorElem);
                r8 = obj;
                this.fComplexTypeDecl.setValues(this.fName, this.fTargetNamespace, this.fBaseType, this.fDerivedBy, this.fFinal, this.fBlock, this.fContentType, this.fIsAbstract, this.fAttrGrp, this.fXSSimpleType, this.fParticle, new XSObjectListImpl(this.fAnnotations, this.fAnnotations != null ? i : this.fAnnotations.length));
                return this.fComplexTypeDecl;
            }
            obj = nextSiblingElement;
            i4 = i2;
        } else {
            i4 = 2;
            i = 0;
            String syntheticAnnotation2 = DOMUtil.getSyntheticAnnotation(element);
            if (syntheticAnnotation2 != null) {
                addAnnotation(traverseSyntheticAnnotation(element, syntheticAnnotation2, objArr, false, xSDocumentInfo));
            }
            obj = firstChildElement;
        }
        if (obj == null) {
            try {
                this.fBaseType = SchemaGrammar.fAnyType;
                this.fDerivedBy = i4;
                r8 = obj;
                processComplexContent(obj, bool2.booleanValue(), false, xSDocumentInfo, schemaGrammar);
            } catch (ComplexTypeRecoverableError e6) {
                e = e6;
                handleComplexTypeError(e.getMessage(), e.errorSubstText, e.errorElem);
                r8 = obj;
            }
        } else {
            ?? r82 = obj;
            if (DOMUtil.getLocalName(r82).equals(SchemaSymbols.ELT_SIMPLECONTENT)) {
                traverseSimpleContent(r82, xSDocumentInfo, schemaGrammar);
                Element nextSiblingElement2 = DOMUtil.getNextSiblingElement(r82);
                if (nextSiblingElement2 != null) {
                    String localName = DOMUtil.getLocalName(nextSiblingElement2);
                    Object[] objArr3 = new Object[i4];
                    objArr3[i] = this.fName;
                    objArr3[1] = localName;
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", objArr3, nextSiblingElement2);
                }
                r8 = r82;
            } else if (DOMUtil.getLocalName(r82).equals(SchemaSymbols.ELT_COMPLEXCONTENT)) {
                traverseComplexContent(r82, bool2.booleanValue(), xSDocumentInfo, schemaGrammar);
                Element nextSiblingElement3 = DOMUtil.getNextSiblingElement(r82);
                if (nextSiblingElement3 != null) {
                    String localName2 = DOMUtil.getLocalName(nextSiblingElement3);
                    Object[] objArr4 = new Object[i4];
                    objArr4[i] = this.fName;
                    objArr4[1] = localName2;
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", objArr4, nextSiblingElement3);
                }
                r8 = r82;
            } else {
                this.fBaseType = SchemaGrammar.fAnyType;
                this.fDerivedBy = i4;
                processComplexContent(r82, bool2.booleanValue(), false, xSDocumentInfo, schemaGrammar);
                r8 = r82;
            }
        }
        this.fComplexTypeDecl.setValues(this.fName, this.fTargetNamespace, this.fBaseType, this.fDerivedBy, this.fFinal, this.fBlock, this.fContentType, this.fIsAbstract, this.fAttrGrp, this.fXSSimpleType, this.fParticle, new XSObjectListImpl(this.fAnnotations, this.fAnnotations != null ? i : this.fAnnotations.length));
        return this.fComplexTypeDecl;
    }

    private void traverseSimpleContent(Element simpleContentElement, XSDocumentInfo schemaDoc, SchemaGrammar grammar) throws ComplexTypeRecoverableError {
        int baseFinalSet;
        XSComplexTypeDecl baseComplexType;
        int i;
        Object[] derivationTypeAttrValues;
        Element simpleContent;
        Element simpleContent2;
        short fixedFacets;
        Element simpleContent3;
        Element attrNode;
        Element simpleContent4;
        Element simpleContent5;
        Object[] simpleContentAttrValues = this.fAttrChecker.checkAttributes(simpleContentElement, false, schemaDoc);
        this.fContentType = (short) 1;
        this.fParticle = null;
        Element simpleContent6 = DOMUtil.getFirstChildElement(simpleContentElement);
        if (simpleContent6 != null && DOMUtil.getLocalName(simpleContent6).equals(SchemaSymbols.ELT_ANNOTATION)) {
            addAnnotation(traverseAnnotationDecl(simpleContent6, simpleContentAttrValues, false, schemaDoc));
            simpleContent6 = DOMUtil.getNextSiblingElement(simpleContent6);
        } else {
            String text = DOMUtil.getSyntheticAnnotation(simpleContentElement);
            if (text != null) {
                addAnnotation(traverseSyntheticAnnotation(simpleContentElement, text, simpleContentAttrValues, false, schemaDoc));
            }
        }
        if (simpleContent6 == null) {
            this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.2", new Object[]{this.fName, SchemaSymbols.ELT_SIMPLECONTENT}, simpleContentElement);
        }
        String simpleContentName = DOMUtil.getLocalName(simpleContent6);
        if (simpleContentName.equals(SchemaSymbols.ELT_RESTRICTION)) {
            this.fDerivedBy = (short) 2;
        } else if (simpleContentName.equals(SchemaSymbols.ELT_EXTENSION)) {
            this.fDerivedBy = (short) 1;
        } else {
            this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, simpleContentName}, simpleContent6);
        }
        Element elemTmp = DOMUtil.getNextSiblingElement(simpleContent6);
        if (elemTmp != null) {
            this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            String siblingName = DOMUtil.getLocalName(elemTmp);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, siblingName}, elemTmp);
        }
        Object[] derivationTypeAttrValues2 = this.fAttrChecker.checkAttributes(simpleContent6, false, schemaDoc);
        QName baseTypeName = (QName) derivationTypeAttrValues2[XSAttributeChecker.ATTIDX_BASE];
        if (baseTypeName == null) {
            this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            this.fAttrChecker.returnAttrArray(derivationTypeAttrValues2, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-att-must-appear", new Object[]{simpleContentName, "base"}, simpleContent6);
        }
        XSTypeDefinition type = (XSTypeDefinition) this.fSchemaHandler.getGlobalDecl(schemaDoc, 7, baseTypeName, simpleContent6);
        if (type == null) {
            this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            this.fAttrChecker.returnAttrArray(derivationTypeAttrValues2, schemaDoc);
            throw new ComplexTypeRecoverableError();
        }
        this.fBaseType = type;
        XSSimpleType baseValidator = null;
        XSComplexTypeDecl baseComplexType2 = null;
        if (type.getTypeCategory() == 15) {
            baseComplexType2 = (XSComplexTypeDecl) type;
            baseFinalSet = baseComplexType2.getFinal();
            if (baseComplexType2.getContentType() == 1) {
                baseValidator = (XSSimpleType) baseComplexType2.getSimpleType();
            } else {
                int baseFinalSet2 = this.fDerivedBy;
                if (baseFinalSet2 != 2 || baseComplexType2.getContentType() != 3 || !((XSParticleDecl) baseComplexType2.getParticle()).emptiable()) {
                    this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues2, schemaDoc);
                    throw new ComplexTypeRecoverableError("src-ct.2.1", new Object[]{this.fName, baseComplexType2.getName()}, simpleContent6);
                }
            }
        } else {
            baseValidator = (XSSimpleType) type;
            if (this.fDerivedBy == 2) {
                this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                this.fAttrChecker.returnAttrArray(derivationTypeAttrValues2, schemaDoc);
                throw new ComplexTypeRecoverableError("src-ct.2.1", new Object[]{this.fName, baseValidator.getName()}, simpleContent6);
            }
            baseFinalSet = baseValidator.getFinal();
        }
        XSSimpleType baseValidator2 = baseValidator;
        int baseFinalSet3 = baseFinalSet;
        XSComplexTypeDecl baseComplexType3 = baseComplexType2;
        if ((this.fDerivedBy & baseFinalSet3) != 0) {
            this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
            this.fAttrChecker.returnAttrArray(derivationTypeAttrValues2, schemaDoc);
            String errorKey = this.fDerivedBy == 1 ? "cos-ct-extends.1.1" : "derivation-ok-restriction.1";
            throw new ComplexTypeRecoverableError(errorKey, new Object[]{this.fName, this.fBaseType.getName()}, simpleContent6);
        }
        Element scElement = simpleContent6;
        Element simpleContent7 = DOMUtil.getFirstChildElement(simpleContent6);
        if (simpleContent7 != null) {
            if (DOMUtil.getLocalName(simpleContent7).equals(SchemaSymbols.ELT_ANNOTATION)) {
                addAnnotation(traverseAnnotationDecl(simpleContent7, derivationTypeAttrValues2, false, schemaDoc));
                simpleContent5 = DOMUtil.getNextSiblingElement(simpleContent7);
                baseComplexType = baseComplexType3;
                i = 3;
                derivationTypeAttrValues = derivationTypeAttrValues2;
            } else {
                String text2 = DOMUtil.getSyntheticAnnotation(scElement);
                if (text2 != null) {
                    baseComplexType = baseComplexType3;
                    simpleContent4 = simpleContent7;
                    i = 3;
                    derivationTypeAttrValues = derivationTypeAttrValues2;
                    addAnnotation(traverseSyntheticAnnotation(scElement, text2, derivationTypeAttrValues2, false, schemaDoc));
                } else {
                    simpleContent4 = simpleContent7;
                    baseComplexType = baseComplexType3;
                    i = 3;
                    derivationTypeAttrValues = derivationTypeAttrValues2;
                }
                simpleContent5 = simpleContent4;
            }
            if (simpleContent5 != null && DOMUtil.getLocalName(simpleContent5).equals(SchemaSymbols.ELT_ANNOTATION)) {
                this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, SchemaSymbols.ELT_ANNOTATION}, simpleContent5);
            }
            simpleContent = simpleContent5;
        } else {
            baseComplexType = baseComplexType3;
            i = 3;
            derivationTypeAttrValues = derivationTypeAttrValues2;
            String text3 = DOMUtil.getSyntheticAnnotation(scElement);
            if (text3 != null) {
                addAnnotation(traverseSyntheticAnnotation(scElement, text3, derivationTypeAttrValues, false, schemaDoc));
            }
            simpleContent = simpleContent7;
        }
        if (this.fDerivedBy == 2) {
            if (simpleContent != null && DOMUtil.getLocalName(simpleContent).equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                XSSimpleType dv = this.fSchemaHandler.fSimpleTypeTraverser.traverseLocal(simpleContent, schemaDoc, grammar);
                if (dv == null) {
                    this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError();
                }
                if (baseValidator2 != null && !XSConstraints.checkSimpleDerivationOk(dv, baseValidator2, baseValidator2.getFinal())) {
                    this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    Object[] objArr = new Object[i];
                    objArr[0] = this.fName;
                    objArr[1] = dv.getName();
                    objArr[2] = baseValidator2.getName();
                    throw new ComplexTypeRecoverableError("derivation-ok-restriction.5.2.2.1", objArr, simpleContent);
                }
                baseValidator2 = dv;
                simpleContent = DOMUtil.getNextSiblingElement(simpleContent);
            }
            if (baseValidator2 == null) {
                this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw new ComplexTypeRecoverableError("src-ct.2.2", new Object[]{this.fName}, simpleContent);
            }
            Element attrNode2 = null;
            XSFacets facetData = null;
            short presentFacets = 0;
            short fixedFacets2 = 0;
            if (simpleContent != null) {
                XSDAbstractTraverser.FacetInfo fi = traverseFacets(simpleContent, baseValidator2, schemaDoc);
                attrNode2 = fi.nodeAfterFacets;
                facetData = fi.facetdata;
                presentFacets = fi.fPresentFacets;
                fixedFacets2 = fi.fFixedFacets;
            }
            XSFacets facetData2 = facetData;
            Element attrNode3 = attrNode2;
            short fixedFacets3 = fixedFacets2;
            short presentFacets2 = presentFacets;
            String name = genAnonTypeName(simpleContentElement);
            this.fXSSimpleType = this.fSchemaHandler.fDVFactory.createTypeRestriction(name, schemaDoc.fTargetNamespace, (short) 0, baseValidator2, null);
            try {
                this.fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
                this.fXSSimpleType.applyFacets(facetData2, presentFacets2, fixedFacets3, this.fValidationState);
                fixedFacets = fixedFacets3;
            } catch (InvalidDatatypeFacetException ex) {
                fixedFacets = fixedFacets3;
                reportSchemaError(ex.getKey(), ex.getArgs(), simpleContent);
                this.fXSSimpleType = this.fSchemaHandler.fDVFactory.createTypeRestriction(name, schemaDoc.fTargetNamespace, (short) 0, baseValidator2, null);
            }
            if (this.fXSSimpleType instanceof XSSimpleTypeDecl) {
                ((XSSimpleTypeDecl) this.fXSSimpleType).setAnonymous(true);
            }
            if (attrNode3 == null) {
                simpleContent3 = attrNode3;
            } else {
                if (!isAttrOrAttrGroup(attrNode3)) {
                    this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, DOMUtil.getLocalName(attrNode3)}, attrNode3);
                }
                Element node = traverseAttrsAndAttrGrps(attrNode3, this.fAttrGrp, schemaDoc, grammar, this.fComplexTypeDecl);
                if (node != null) {
                    this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, DOMUtil.getLocalName(node)}, node);
                }
                simpleContent3 = attrNode3;
            }
            XSComplexTypeDecl baseComplexType4 = baseComplexType;
            try {
                attrNode = simpleContent3;
            } catch (ComplexTypeRecoverableError e) {
                e = e;
            }
            try {
                mergeAttributes(baseComplexType4.getAttrGrp(), this.fAttrGrp, this.fName, false, simpleContentElement);
                this.fAttrGrp.removeProhibitedAttrs();
                Object[] errArgs = this.fAttrGrp.validRestrictionOf(this.fName, baseComplexType4.getAttrGrp());
                if (errArgs != null) {
                    this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError((String) errArgs[errArgs.length - 1], errArgs, attrNode);
                }
            } catch (ComplexTypeRecoverableError e2) {
                e = e2;
                this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                throw e;
            }
        } else {
            XSComplexTypeDecl baseComplexType5 = baseComplexType;
            this.fXSSimpleType = baseValidator2;
            if (simpleContent != null) {
                Element attrNode4 = simpleContent;
                if (!isAttrOrAttrGroup(attrNode4)) {
                    this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, DOMUtil.getLocalName(attrNode4)}, attrNode4);
                }
                simpleContent2 = simpleContent;
                Element node2 = traverseAttrsAndAttrGrps(attrNode4, this.fAttrGrp, schemaDoc, grammar, this.fComplexTypeDecl);
                if (node2 != null) {
                    this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, DOMUtil.getLocalName(node2)}, node2);
                }
                this.fAttrGrp.removeProhibitedAttrs();
            } else {
                simpleContent2 = simpleContent;
            }
            if (baseComplexType5 != null) {
                try {
                    mergeAttributes(baseComplexType5.getAttrGrp(), this.fAttrGrp, this.fName, true, simpleContentElement);
                } catch (ComplexTypeRecoverableError e3) {
                    this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw e3;
                }
            }
        }
        this.fAttrChecker.returnAttrArray(simpleContentAttrValues, schemaDoc);
        this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
    }

    private void traverseComplexContent(Element complexContentElement, boolean mixedOnType, XSDocumentInfo schemaDoc, SchemaGrammar grammar) throws ComplexTypeRecoverableError {
        Object[] derivationTypeAttrValues;
        XSTypeDefinition xSTypeDefinition;
        Object[] errArgs;
        Object[] derivationTypeAttrValues2;
        Object[] complexContentAttrValues = this.fAttrChecker.checkAttributes(complexContentElement, false, schemaDoc);
        boolean mixedContent = mixedOnType;
        Boolean mixedAtt = (Boolean) complexContentAttrValues[XSAttributeChecker.ATTIDX_MIXED];
        if (mixedAtt != null) {
            mixedContent = mixedAtt.booleanValue();
        }
        boolean mixedContent2 = mixedContent;
        this.fXSSimpleType = null;
        Element complexContent = DOMUtil.getFirstChildElement(complexContentElement);
        if (complexContent == null || !DOMUtil.getLocalName(complexContent).equals(SchemaSymbols.ELT_ANNOTATION)) {
            String text = DOMUtil.getSyntheticAnnotation(complexContentElement);
            if (text != null) {
                addAnnotation(traverseSyntheticAnnotation(complexContentElement, text, complexContentAttrValues, false, schemaDoc));
            }
        } else {
            addAnnotation(traverseAnnotationDecl(complexContent, complexContentAttrValues, false, schemaDoc));
            complexContent = DOMUtil.getNextSiblingElement(complexContent);
        }
        if (complexContent == null) {
            this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.2", new Object[]{this.fName, SchemaSymbols.ELT_COMPLEXCONTENT}, complexContentElement);
        }
        String complexContentName = DOMUtil.getLocalName(complexContent);
        if (complexContentName.equals(SchemaSymbols.ELT_RESTRICTION)) {
            this.fDerivedBy = (short) 2;
        } else {
            if (!complexContentName.equals(SchemaSymbols.ELT_EXTENSION)) {
                this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, complexContentName}, complexContent);
            }
            this.fDerivedBy = (short) 1;
        }
        Element elemTmp = DOMUtil.getNextSiblingElement(complexContent);
        if (elemTmp != null) {
            this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            String siblingName = DOMUtil.getLocalName(elemTmp);
            throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, siblingName}, elemTmp);
        }
        Object[] derivationTypeAttrValues3 = this.fAttrChecker.checkAttributes(complexContent, false, schemaDoc);
        QName baseTypeName = (QName) derivationTypeAttrValues3[XSAttributeChecker.ATTIDX_BASE];
        if (baseTypeName == null) {
            this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            this.fAttrChecker.returnAttrArray(derivationTypeAttrValues3, schemaDoc);
            throw new ComplexTypeRecoverableError("s4s-att-must-appear", new Object[]{complexContentName, "base"}, complexContent);
        }
        XSTypeDefinition type = (XSTypeDefinition) this.fSchemaHandler.getGlobalDecl(schemaDoc, 7, baseTypeName, complexContent);
        if (type == null) {
            this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            this.fAttrChecker.returnAttrArray(derivationTypeAttrValues3, schemaDoc);
            throw new ComplexTypeRecoverableError();
        }
        if (!(type instanceof XSComplexTypeDecl)) {
            this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            this.fAttrChecker.returnAttrArray(derivationTypeAttrValues3, schemaDoc);
            throw new ComplexTypeRecoverableError("src-ct.1", new Object[]{this.fName, type.getName()}, complexContent);
        }
        this.fBaseType = type;
        if ((type.getFinal() & this.fDerivedBy) != 0) {
            this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            this.fAttrChecker.returnAttrArray(derivationTypeAttrValues3, schemaDoc);
            String errorKey = this.fDerivedBy == 1 ? "cos-ct-extends.1.1" : "derivation-ok-restriction.1";
            throw new ComplexTypeRecoverableError(errorKey, new Object[]{this.fName, this.fBaseType.getName()}, complexContent);
        }
        Element complexContent2 = DOMUtil.getFirstChildElement(complexContent);
        if (complexContent2 != null) {
            if (DOMUtil.getLocalName(complexContent2).equals(SchemaSymbols.ELT_ANNOTATION)) {
                addAnnotation(traverseAnnotationDecl(complexContent2, derivationTypeAttrValues3, false, schemaDoc));
                complexContent2 = DOMUtil.getNextSiblingElement(complexContent2);
                derivationTypeAttrValues2 = derivationTypeAttrValues3;
                xSTypeDefinition = type;
            } else {
                String text2 = DOMUtil.getSyntheticAnnotation(complexContent2);
                if (text2 != null) {
                    xSTypeDefinition = type;
                    derivationTypeAttrValues2 = derivationTypeAttrValues3;
                    addAnnotation(traverseSyntheticAnnotation(complexContent2, text2, derivationTypeAttrValues3, false, schemaDoc));
                } else {
                    derivationTypeAttrValues2 = derivationTypeAttrValues3;
                    xSTypeDefinition = type;
                }
            }
            if (complexContent2 != null && DOMUtil.getLocalName(complexContent2).equals(SchemaSymbols.ELT_ANNOTATION)) {
                this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                this.fAttrChecker.returnAttrArray(derivationTypeAttrValues2, schemaDoc);
                throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, SchemaSymbols.ELT_ANNOTATION}, complexContent2);
            }
            derivationTypeAttrValues = derivationTypeAttrValues2;
        } else {
            derivationTypeAttrValues = derivationTypeAttrValues3;
            xSTypeDefinition = type;
            String text3 = DOMUtil.getSyntheticAnnotation(complexContent2);
            if (text3 != null) {
                addAnnotation(traverseSyntheticAnnotation(complexContent2, text3, derivationTypeAttrValues, false, schemaDoc));
            }
        }
        Element complexContent3 = complexContent2;
        try {
            processComplexContent(complexContent3, mixedContent2, true, schemaDoc, grammar);
            ?? r6 = xSTypeDefinition;
            XSParticleDecl baseContent = (XSParticleDecl) r6.getParticle();
            if (this.fDerivedBy != 2) {
                if (this.fParticle == null) {
                    this.fContentType = r6.getContentType();
                    this.fXSSimpleType = (XSSimpleType) r6.getSimpleType();
                    this.fParticle = baseContent;
                } else if (r6.getContentType() != 0) {
                    if (this.fContentType == 2 && r6.getContentType() != 2) {
                        this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                        this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                        throw new ComplexTypeRecoverableError("cos-ct-extends.1.4.3.2.2.1.a", new Object[]{this.fName}, complexContent3);
                    }
                    if (this.fContentType == 3 && r6.getContentType() != 3) {
                        this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                        this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                        throw new ComplexTypeRecoverableError("cos-ct-extends.1.4.3.2.2.1.b", new Object[]{this.fName}, complexContent3);
                    }
                    if ((this.fParticle.fType == 3 && ((XSModelGroupImpl) this.fParticle.fValue).fCompositor == 103) || (((XSParticleDecl) r6.getParticle()).fType == 3 && ((XSModelGroupImpl) ((XSParticleDecl) r6.getParticle()).fValue).fCompositor == 103)) {
                        this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                        this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                        throw new ComplexTypeRecoverableError("cos-all-limited.1.2", new Object[0], complexContent3);
                    }
                    XSModelGroupImpl group = new XSModelGroupImpl();
                    group.fCompositor = (short) 102;
                    group.fParticleCount = 2;
                    group.fParticles = new XSParticleDecl[2];
                    group.fParticles[0] = (XSParticleDecl) r6.getParticle();
                    group.fParticles[1] = this.fParticle;
                    group.fAnnotations = XSObjectListImpl.EMPTY_LIST;
                    XSParticleDecl particle = new XSParticleDecl();
                    particle.fType = (short) 3;
                    particle.fValue = group;
                    particle.fAnnotations = XSObjectListImpl.EMPTY_LIST;
                    this.fParticle = particle;
                }
                this.fAttrGrp.removeProhibitedAttrs();
                try {
                } catch (ComplexTypeRecoverableError e) {
                    e = e;
                }
                try {
                    mergeAttributes(r6.getAttrGrp(), this.fAttrGrp, this.fName, true, complexContent3);
                } catch (ComplexTypeRecoverableError e2) {
                    e = e2;
                    this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw e;
                }
            } else {
                if (this.fContentType == 3 && r6.getContentType() != 3) {
                    this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw new ComplexTypeRecoverableError("derivation-ok-restriction.5.4.1.2", new Object[]{this.fName, r6.getName()}, complexContent3);
                }
                Element complexContent4 = complexContent3;
                try {
                } catch (ComplexTypeRecoverableError e3) {
                    e = e3;
                }
                try {
                    mergeAttributes(r6.getAttrGrp(), this.fAttrGrp, this.fName, false, complexContent4);
                    this.fAttrGrp.removeProhibitedAttrs();
                    if (r6 != SchemaGrammar.fAnyType && (errArgs = this.fAttrGrp.validRestrictionOf(this.fName, r6.getAttrGrp())) != null) {
                        this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                        this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                        throw new ComplexTypeRecoverableError((String) errArgs[errArgs.length - 1], errArgs, complexContent4);
                    }
                } catch (ComplexTypeRecoverableError e4) {
                    e = e4;
                    this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
                    this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
                    throw e;
                }
            }
            this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
        } catch (ComplexTypeRecoverableError e5) {
            this.fAttrChecker.returnAttrArray(complexContentAttrValues, schemaDoc);
            this.fAttrChecker.returnAttrArray(derivationTypeAttrValues, schemaDoc);
            throw e5;
        }
    }

    private void mergeAttributes(XSAttributeGroupDecl fromAttrGrp, XSAttributeGroupDecl toAttrGrp, String typeName, boolean extension, Element elem) throws ComplexTypeRecoverableError {
        XSObjectList attrUseS = fromAttrGrp.getAttributeUses();
        int attrCount = attrUseS.getLength();
        for (int i = 0; i < attrCount; i++) {
            XSAttributeUseImpl oneAttrUse = (XSAttributeUseImpl) attrUseS.item(i);
            XSAttributeUse existingAttrUse = toAttrGrp.getAttributeUse(oneAttrUse.fAttrDecl.getNamespace(), oneAttrUse.fAttrDecl.getName());
            if (existingAttrUse == null) {
                String idName = toAttrGrp.addAttributeUse(oneAttrUse);
                if (idName != null) {
                    throw new ComplexTypeRecoverableError("ct-props-correct.5", new Object[]{typeName, idName, oneAttrUse.fAttrDecl.getName()}, elem);
                }
            } else {
                if (existingAttrUse != oneAttrUse && extension) {
                    reportSchemaError("ct-props-correct.4", new Object[]{typeName, oneAttrUse.fAttrDecl.getName()}, elem);
                    toAttrGrp.replaceAttributeUse(existingAttrUse, oneAttrUse);
                }
            }
        }
        if (extension) {
            if (toAttrGrp.fAttributeWC == null) {
                toAttrGrp.fAttributeWC = fromAttrGrp.fAttributeWC;
            } else if (fromAttrGrp.fAttributeWC != null) {
                toAttrGrp.fAttributeWC = toAttrGrp.fAttributeWC.performUnionWith(fromAttrGrp.fAttributeWC, toAttrGrp.fAttributeWC.fProcessContents);
                if (toAttrGrp.fAttributeWC == null) {
                    throw new ComplexTypeRecoverableError("src-ct.5", new Object[]{typeName}, elem);
                }
            }
        }
    }

    private void processComplexContent(Element complexContentChild, boolean isMixed, boolean isDerivation, XSDocumentInfo schemaDoc, SchemaGrammar grammar) throws ComplexTypeRecoverableError {
        XSDocumentInfo xSDocumentInfo;
        SchemaGrammar schemaGrammar;
        Element attrNode;
        Element attrNode2 = null;
        XSParticleDecl particle = null;
        boolean emptyParticle = false;
        if (complexContentChild != null) {
            String childName = DOMUtil.getLocalName(complexContentChild);
            if (childName.equals(SchemaSymbols.ELT_GROUP)) {
                xSDocumentInfo = schemaDoc;
                schemaGrammar = grammar;
                particle = this.fSchemaHandler.fGroupTraverser.traverseLocal(complexContentChild, xSDocumentInfo, schemaGrammar);
                attrNode = DOMUtil.getNextSiblingElement(complexContentChild);
            } else {
                xSDocumentInfo = schemaDoc;
                schemaGrammar = grammar;
                if (childName.equals(SchemaSymbols.ELT_SEQUENCE)) {
                    particle = traverseSequence(complexContentChild, xSDocumentInfo, schemaGrammar, 0, this.fComplexTypeDecl);
                    if (particle != null) {
                        XSModelGroupImpl group = (XSModelGroupImpl) particle.fValue;
                        if (group.fParticleCount == 0) {
                            emptyParticle = true;
                        }
                    }
                    attrNode = DOMUtil.getNextSiblingElement(complexContentChild);
                } else if (childName.equals(SchemaSymbols.ELT_CHOICE)) {
                    particle = traverseChoice(complexContentChild, xSDocumentInfo, schemaGrammar, 0, this.fComplexTypeDecl);
                    if (particle != null && particle.fMinOccurs == 0) {
                        XSModelGroupImpl group2 = (XSModelGroupImpl) particle.fValue;
                        if (group2.fParticleCount == 0) {
                            emptyParticle = true;
                        }
                    }
                    attrNode = DOMUtil.getNextSiblingElement(complexContentChild);
                } else if (childName.equals(SchemaSymbols.ELT_ALL)) {
                    particle = traverseAll(complexContentChild, xSDocumentInfo, schemaGrammar, 8, this.fComplexTypeDecl);
                    if (particle != null) {
                        XSModelGroupImpl group3 = (XSModelGroupImpl) particle.fValue;
                        if (group3.fParticleCount == 0) {
                            emptyParticle = true;
                        }
                    }
                    attrNode = DOMUtil.getNextSiblingElement(complexContentChild);
                } else {
                    attrNode = complexContentChild;
                }
            }
            attrNode2 = attrNode;
        } else {
            xSDocumentInfo = schemaDoc;
            schemaGrammar = grammar;
        }
        if (emptyParticle) {
            Element child = DOMUtil.getFirstChildElement(complexContentChild);
            if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                child = DOMUtil.getNextSiblingElement(child);
            }
            if (child == null) {
                particle = null;
            }
        }
        if (particle == null && isMixed) {
            particle = XSConstraints.getEmptySequence();
        }
        this.fParticle = particle;
        if (this.fParticle == null) {
            this.fContentType = (short) 0;
        } else if (isMixed) {
            this.fContentType = (short) 3;
        } else {
            this.fContentType = (short) 2;
        }
        if (attrNode2 != null) {
            if (!isAttrOrAttrGroup(attrNode2)) {
                throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, DOMUtil.getLocalName(attrNode2)}, attrNode2);
            }
            Element node = traverseAttrsAndAttrGrps(attrNode2, this.fAttrGrp, xSDocumentInfo, schemaGrammar, this.fComplexTypeDecl);
            if (node != null) {
                throw new ComplexTypeRecoverableError("s4s-elt-invalid-content.1", new Object[]{this.fName, DOMUtil.getLocalName(node)}, node);
            }
            if (!isDerivation) {
                this.fAttrGrp.removeProhibitedAttrs();
            }
        }
    }

    private boolean isAttrOrAttrGroup(Element e) {
        String elementName = DOMUtil.getLocalName(e);
        if (elementName.equals(SchemaSymbols.ELT_ATTRIBUTE) || elementName.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP) || elementName.equals(SchemaSymbols.ELT_ANYATTRIBUTE)) {
            return true;
        }
        return false;
    }

    private void traverseSimpleContentDecl(Element simpleContentDecl) {
    }

    private void traverseComplexContentDecl(Element complexContentDecl, boolean mixedOnComplexTypeDecl) {
    }

    private String genAnonTypeName(Element complexTypeDecl) {
        StringBuffer typeName = new StringBuffer("#AnonType_");
        for (Element node = DOMUtil.getParent(complexTypeDecl); node != null && node != DOMUtil.getRoot(DOMUtil.getDocument(node)); node = DOMUtil.getParent(node)) {
            typeName.append(node.getAttribute(SchemaSymbols.ATT_NAME));
        }
        return typeName.toString();
    }

    private void handleComplexTypeError(String messageId, Object[] args, Element e) {
        if (messageId != null) {
            reportSchemaError(messageId, args, e);
        }
        this.fBaseType = SchemaGrammar.fAnyType;
        this.fContentType = (short) 3;
        this.fXSSimpleType = null;
        this.fParticle = getErrorContent();
        this.fAttrGrp.fAttributeWC = getErrorWildcard();
    }

    private void contentBackup() {
        if (this.fGlobalStore == null) {
            this.fGlobalStore = new Object[11];
            this.fGlobalStorePos = 0;
        }
        if (this.fGlobalStorePos == this.fGlobalStore.length) {
            Object[] newArray = new Object[this.fGlobalStorePos + 11];
            System.arraycopy(this.fGlobalStore, 0, newArray, 0, this.fGlobalStorePos);
            this.fGlobalStore = newArray;
        }
        Object[] newArray2 = this.fGlobalStore;
        int i = this.fGlobalStorePos;
        this.fGlobalStorePos = i + 1;
        newArray2[i] = this.fComplexTypeDecl;
        Object[] objArr = this.fGlobalStore;
        int i2 = this.fGlobalStorePos;
        this.fGlobalStorePos = i2 + 1;
        objArr[i2] = this.fIsAbstract ? Boolean.TRUE : Boolean.FALSE;
        Object[] objArr2 = this.fGlobalStore;
        int i3 = this.fGlobalStorePos;
        this.fGlobalStorePos = i3 + 1;
        objArr2[i3] = this.fName;
        Object[] objArr3 = this.fGlobalStore;
        int i4 = this.fGlobalStorePos;
        this.fGlobalStorePos = i4 + 1;
        objArr3[i4] = this.fTargetNamespace;
        Object[] objArr4 = this.fGlobalStore;
        int i5 = this.fGlobalStorePos;
        this.fGlobalStorePos = i5 + 1;
        objArr4[i5] = new Integer((this.fDerivedBy << 16) + this.fFinal);
        Object[] objArr5 = this.fGlobalStore;
        int i6 = this.fGlobalStorePos;
        this.fGlobalStorePos = i6 + 1;
        objArr5[i6] = new Integer((this.fBlock << 16) + this.fContentType);
        Object[] objArr6 = this.fGlobalStore;
        int i7 = this.fGlobalStorePos;
        this.fGlobalStorePos = i7 + 1;
        objArr6[i7] = this.fBaseType;
        Object[] objArr7 = this.fGlobalStore;
        int i8 = this.fGlobalStorePos;
        this.fGlobalStorePos = i8 + 1;
        objArr7[i8] = this.fAttrGrp;
        Object[] objArr8 = this.fGlobalStore;
        int i9 = this.fGlobalStorePos;
        this.fGlobalStorePos = i9 + 1;
        objArr8[i9] = this.fParticle;
        Object[] objArr9 = this.fGlobalStore;
        int i10 = this.fGlobalStorePos;
        this.fGlobalStorePos = i10 + 1;
        objArr9[i10] = this.fXSSimpleType;
        Object[] objArr10 = this.fGlobalStore;
        int i11 = this.fGlobalStorePos;
        this.fGlobalStorePos = i11 + 1;
        objArr10[i11] = this.fAnnotations;
    }

    private void contentRestore() {
        Object[] objArr = this.fGlobalStore;
        int i = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i;
        this.fAnnotations = (XSAnnotationImpl[]) objArr[i];
        Object[] objArr2 = this.fGlobalStore;
        int i2 = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i2;
        this.fXSSimpleType = (XSSimpleType) objArr2[i2];
        Object[] objArr3 = this.fGlobalStore;
        int i3 = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i3;
        this.fParticle = (XSParticleDecl) objArr3[i3];
        Object[] objArr4 = this.fGlobalStore;
        int i4 = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i4;
        this.fAttrGrp = (XSAttributeGroupDecl) objArr4[i4];
        Object[] objArr5 = this.fGlobalStore;
        int i5 = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i5;
        this.fBaseType = (XSTypeDefinition) objArr5[i5];
        Object[] objArr6 = this.fGlobalStore;
        int i6 = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i6;
        int i7 = ((Integer) objArr6[i6]).intValue();
        this.fBlock = (short) (i7 >> 16);
        this.fContentType = (short) i7;
        Object[] objArr7 = this.fGlobalStore;
        int i8 = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i8;
        int i9 = ((Integer) objArr7[i8]).intValue();
        this.fDerivedBy = (short) (i9 >> 16);
        this.fFinal = (short) i9;
        Object[] objArr8 = this.fGlobalStore;
        int i10 = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i10;
        this.fTargetNamespace = (String) objArr8[i10];
        Object[] objArr9 = this.fGlobalStore;
        int i11 = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i11;
        this.fName = (String) objArr9[i11];
        Object[] objArr10 = this.fGlobalStore;
        int i12 = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i12;
        this.fIsAbstract = ((Boolean) objArr10[i12]).booleanValue();
        Object[] objArr11 = this.fGlobalStore;
        int i13 = this.fGlobalStorePos - 1;
        this.fGlobalStorePos = i13;
        this.fComplexTypeDecl = (XSComplexTypeDecl) objArr11[i13];
    }

    private void addAnnotation(XSAnnotationImpl annotation) {
        if (annotation == null) {
            return;
        }
        if (this.fAnnotations == null) {
            this.fAnnotations = new XSAnnotationImpl[1];
        } else {
            XSAnnotationImpl[] tempArray = new XSAnnotationImpl[this.fAnnotations.length + 1];
            System.arraycopy(this.fAnnotations, 0, tempArray, 0, this.fAnnotations.length);
            this.fAnnotations = tempArray;
        }
        this.fAnnotations[this.fAnnotations.length - 1] = annotation;
    }
}
