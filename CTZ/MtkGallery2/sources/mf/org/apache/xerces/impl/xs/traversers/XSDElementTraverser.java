package mf.org.apache.xerces.impl.xs.traversers;

import java.util.Locale;
import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSAnnotationImpl;
import mf.org.apache.xerces.impl.xs.XSComplexTypeDecl;
import mf.org.apache.xerces.impl.xs.XSConstraints;
import mf.org.apache.xerces.impl.xs.XSElementDecl;
import mf.org.apache.xerces.impl.xs.XSParticleDecl;
import mf.org.apache.xerces.impl.xs.util.XInt;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xs.XSObject;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.Element;

class XSDElementTraverser extends XSDAbstractTraverser {
    boolean fDeferTraversingLocalElements;
    protected final XSElementDecl fTempElementDecl;

    XSDElementTraverser(XSDHandler handler, XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
        this.fTempElementDecl = new XSElementDecl();
    }

    XSParticleDecl traverseLocal(Element elmDecl, XSDocumentInfo schemaDoc, SchemaGrammar grammar, int allContextFlags, XSObject parent) {
        XSParticleDecl particle;
        if (this.fSchemaHandler.fDeclPool != null) {
            particle = this.fSchemaHandler.fDeclPool.getParticleDecl();
        } else {
            particle = new XSParticleDecl();
        }
        if (this.fDeferTraversingLocalElements) {
            particle.fType = (short) 1;
            Attr attr = elmDecl.getAttributeNode(SchemaSymbols.ATT_MINOCCURS);
            if (attr != null) {
                String min = attr.getValue();
                try {
                    int m = Integer.parseInt(XMLChar.trim(min));
                    if (m >= 0) {
                        particle.fMinOccurs = m;
                    }
                } catch (NumberFormatException e) {
                }
            }
            this.fSchemaHandler.fillInLocalElemInfo(elmDecl, schemaDoc, allContextFlags, parent, particle);
            return particle;
        }
        traverseLocal(particle, elmDecl, schemaDoc, grammar, allContextFlags, parent, null);
        if (particle.fType == 0) {
            return null;
        }
        return particle;
    }

    protected void traverseLocal(XSParticleDecl particle, Element elmDecl, XSDocumentInfo schemaDoc, SchemaGrammar grammar, int allContextFlags, XSObject parent, String[] localNSDecls) {
        XInt maxAtt;
        XSElementDecl element;
        XSObjectListImpl xSObjectListImpl;
        if (localNSDecls != null) {
            schemaDoc.fNamespaceSupport.setEffectiveContext(localNSDecls);
        }
        Object[] attrValues = this.fAttrChecker.checkAttributes(elmDecl, false, schemaDoc);
        QName refAtt = (QName) attrValues[XSAttributeChecker.ATTIDX_REF];
        XInt minAtt = (XInt) attrValues[XSAttributeChecker.ATTIDX_MINOCCURS];
        XInt maxAtt2 = (XInt) attrValues[XSAttributeChecker.ATTIDX_MAXOCCURS];
        XSAnnotationImpl annotation = null;
        if (elmDecl.getAttributeNode(SchemaSymbols.ATT_REF) != null) {
            if (refAtt != null) {
                element = (XSElementDecl) this.fSchemaHandler.getGlobalDecl(schemaDoc, 3, refAtt, elmDecl);
                Element child = DOMUtil.getFirstChildElement(elmDecl);
                if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    XSAnnotationImpl annotation2 = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
                    child = DOMUtil.getNextSiblingElement(child);
                    annotation = annotation2;
                } else {
                    String text = DOMUtil.getSyntheticAnnotation(elmDecl);
                    if (text != null) {
                        annotation = traverseSyntheticAnnotation(elmDecl, text, attrValues, false, schemaDoc);
                        child = child;
                    }
                }
                if (child != null) {
                    reportSchemaError("src-element.2.2", new Object[]{refAtt.rawname, DOMUtil.getLocalName(child)}, child);
                }
            } else {
                element = null;
            }
            maxAtt = maxAtt2;
        } else {
            maxAtt = maxAtt2;
            element = traverseNamedElement(elmDecl, attrValues, schemaDoc, grammar, false, parent);
        }
        XSElementDecl element2 = element;
        XSAnnotationImpl annotation3 = annotation;
        particle.fMinOccurs = minAtt.intValue();
        particle.fMaxOccurs = maxAtt.intValue();
        if (element2 != null) {
            particle.fType = (short) 1;
            particle.fValue = element2;
        } else {
            particle.fType = (short) 0;
        }
        if (refAtt != null) {
            if (annotation3 != null) {
                xSObjectListImpl = new XSObjectListImpl();
                xSObjectListImpl.addXSObject(annotation3);
            } else {
                xSObjectListImpl = XSObjectListImpl.EMPTY_LIST;
            }
            particle.fAnnotations = xSObjectListImpl;
        } else {
            particle.fAnnotations = element2 != null ? element2.fAnnotations : XSObjectListImpl.EMPTY_LIST;
        }
        Long defaultVals = (Long) attrValues[XSAttributeChecker.ATTIDX_FROMDEFAULT];
        checkOccurrences(particle, SchemaSymbols.ELT_ELEMENT, (Element) elmDecl.getParentNode(), allContextFlags, defaultVals.longValue());
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
    }

    XSElementDecl traverseGlobal(Element elmDecl, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        Object[] attrValues = this.fAttrChecker.checkAttributes(elmDecl, true, schemaDoc);
        XSElementDecl element = traverseNamedElement(elmDecl, attrValues, schemaDoc, grammar, true, null);
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return element;
    }

    XSElementDecl traverseNamedElement(Element elmDecl, Object[] attrValues, XSDocumentInfo schemaDoc, SchemaGrammar grammar, boolean isGlobal, XSObject xSObject) {
        XSElementDecl element;
        String nameAtt;
        Element child;
        String text;
        XSElementDecl element2;
        QName subGroupAtt;
        String fixedAtt;
        QName typeAtt;
        String nameAtt2;
        XSAnnotationImpl annotation;
        XSObjectListImpl xSObjectListImpl;
        XSElementDecl element3;
        XSTypeDefinition elementType;
        boolean haveAnonType;
        SchemaGrammar schemaGrammar;
        QName typeAtt2;
        SchemaGrammar schemaGrammar2;
        String defaultAtt;
        QName typeAtt3;
        short s;
        int i;
        int i2;
        String nameAtt3;
        XSDocumentInfo xSDocumentInfo;
        XSElementDecl xSElementDecl;
        String childName;
        XInt finalAtt;
        String defaultAtt2;
        QName typeAtt4;
        XInt blockAtt;
        SchemaGrammar schemaGrammar3;
        Boolean abstractAtt;
        String defaultAtt3;
        XSDocumentInfo xSDocumentInfo2 = schemaDoc;
        Boolean abstractAtt2 = (Boolean) attrValues[XSAttributeChecker.ATTIDX_ABSTRACT];
        XInt blockAtt2 = (XInt) attrValues[XSAttributeChecker.ATTIDX_BLOCK];
        String defaultAtt4 = (String) attrValues[XSAttributeChecker.ATTIDX_DEFAULT];
        XInt finalAtt2 = (XInt) attrValues[XSAttributeChecker.ATTIDX_FINAL];
        String fixedAtt2 = (String) attrValues[XSAttributeChecker.ATTIDX_FIXED];
        XInt formAtt = (XInt) attrValues[XSAttributeChecker.ATTIDX_FORM];
        String nameAtt4 = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        Boolean nillableAtt = (Boolean) attrValues[XSAttributeChecker.ATTIDX_NILLABLE];
        QName subGroupAtt2 = (QName) attrValues[XSAttributeChecker.ATTIDX_SUBSGROUP];
        QName typeAtt5 = (QName) attrValues[XSAttributeChecker.ATTIDX_TYPE];
        if (this.fSchemaHandler.fDeclPool != null) {
            element = this.fSchemaHandler.fDeclPool.getElementDecl();
        } else {
            element = new XSElementDecl();
        }
        if (nameAtt4 != null) {
            element.fName = this.fSymbolTable.addSymbol(nameAtt4);
        }
        if (isGlobal) {
            element.fTargetNamespace = xSDocumentInfo2.fTargetNamespace;
            element.setIsGlobal();
            nameAtt = nameAtt4;
        } else {
            if (xSObject instanceof XSComplexTypeDecl) {
                element.setIsLocal(xSObject);
            }
            if (formAtt != null) {
                nameAtt = nameAtt4;
                if (formAtt.intValue() == 1) {
                    element.fTargetNamespace = xSDocumentInfo2.fTargetNamespace;
                } else {
                    element.fTargetNamespace = null;
                }
            } else {
                nameAtt = nameAtt4;
                if (xSDocumentInfo2.fAreLocalElementsQualified) {
                    element.fTargetNamespace = xSDocumentInfo2.fTargetNamespace;
                } else {
                    element.fTargetNamespace = null;
                    element.fBlock = blockAtt2 != null ? xSDocumentInfo2.fBlockDefault : blockAtt2.shortValue();
                    element.fFinal = finalAtt2 != null ? xSDocumentInfo2.fFinalDefault : finalAtt2.shortValue();
                    element.fBlock = (short) (element.fBlock & 7);
                    XInt blockAtt3 = blockAtt2;
                    element.fFinal = (short) (element.fFinal & 3);
                    if (nillableAtt.booleanValue()) {
                        element.setIsNillable();
                    }
                    if (abstractAtt2 != null && abstractAtt2.booleanValue()) {
                        element.setIsAbstract();
                    }
                    if (fixedAtt2 == null) {
                        element.fDefault = new ValidatedInfo();
                        element.fDefault.normalizedValue = fixedAtt2;
                        element.setConstraintType((short) 2);
                    } else if (defaultAtt4 != null) {
                        element.fDefault = new ValidatedInfo();
                        element.fDefault.normalizedValue = defaultAtt4;
                        element.setConstraintType((short) 1);
                    } else {
                        element.setConstraintType((short) 0);
                    }
                    if (subGroupAtt2 == null) {
                        element.fSubGroup = (XSElementDecl) this.fSchemaHandler.getGlobalDecl(xSDocumentInfo2, 3, subGroupAtt2, elmDecl);
                    }
                    child = DOMUtil.getFirstChildElement(elmDecl);
                    if (child == null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                        XSAnnotationImpl annotation2 = traverseAnnotationDecl(child, attrValues, false, xSDocumentInfo2);
                        child = DOMUtil.getNextSiblingElement(child);
                        element2 = element;
                        subGroupAtt = subGroupAtt2;
                        fixedAtt = fixedAtt2;
                        annotation = annotation2;
                        typeAtt = typeAtt5;
                        nameAtt2 = nameAtt;
                    } else {
                        text = DOMUtil.getSyntheticAnnotation(elmDecl);
                        if (text == null) {
                            element2 = element;
                            typeAtt = typeAtt5;
                            subGroupAtt = subGroupAtt2;
                            nameAtt2 = nameAtt;
                            fixedAtt = fixedAtt2;
                            XSAnnotationImpl annotation3 = traverseSyntheticAnnotation(elmDecl, text, attrValues, false, xSDocumentInfo2);
                            annotation = annotation3;
                            child = child;
                        } else {
                            element2 = element;
                            subGroupAtt = subGroupAtt2;
                            fixedAtt = fixedAtt2;
                            typeAtt = typeAtt5;
                            nameAtt2 = nameAtt;
                            annotation = null;
                        }
                    }
                    if (annotation == null) {
                        xSObjectListImpl = new XSObjectListImpl();
                        xSObjectListImpl.addXSObject(annotation);
                    } else {
                        xSObjectListImpl = XSObjectListImpl.EMPTY_LIST;
                    }
                    element3 = element2;
                    element3.fAnnotations = xSObjectListImpl;
                    elementType = null;
                    haveAnonType = false;
                    if (child == null) {
                        String childName2 = DOMUtil.getLocalName(child);
                        if (childName2.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
                            schemaGrammar = grammar;
                            elementType = this.fSchemaHandler.fComplexTypeTraverser.traverseLocal(child, xSDocumentInfo2, schemaGrammar);
                            haveAnonType = true;
                            child = DOMUtil.getNextSiblingElement(child);
                        } else {
                            schemaGrammar = grammar;
                            if (childName2.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                                elementType = this.fSchemaHandler.fSimpleTypeTraverser.traverseLocal(child, xSDocumentInfo2, schemaGrammar);
                                haveAnonType = true;
                                child = DOMUtil.getNextSiblingElement(child);
                            }
                        }
                    } else {
                        schemaGrammar = grammar;
                    }
                    if (elementType != null) {
                        typeAtt2 = typeAtt;
                        if (typeAtt2 != null && (elementType = (XSTypeDefinition) this.fSchemaHandler.getGlobalDecl(xSDocumentInfo2, 7, typeAtt2, elmDecl)) == null) {
                            element3.fUnresolvedTypeName = typeAtt2;
                        }
                    } else {
                        typeAtt2 = typeAtt;
                    }
                    if (elementType == null && element3.fSubGroup != null) {
                        elementType = element3.fSubGroup.fType;
                    }
                    if (elementType == null) {
                        elementType = SchemaGrammar.fAnyType;
                    }
                    element3.fType = elementType;
                    if (child == null) {
                        String childName3 = DOMUtil.getLocalName(child);
                        while (child != null) {
                            if (childName3.equals(SchemaSymbols.ELT_KEY) || childName3.equals(SchemaSymbols.ELT_KEYREF) || childName3.equals(SchemaSymbols.ELT_UNIQUE)) {
                                if (childName3.equals(SchemaSymbols.ELT_KEY) || childName3.equals(SchemaSymbols.ELT_UNIQUE)) {
                                    DOMUtil.setHidden(child, this.fSchemaHandler.fHiddenNodes);
                                    this.fSchemaHandler.fUniqueOrKeyTraverser.traverse(child, element3, xSDocumentInfo2, schemaGrammar);
                                    if (DOMUtil.getAttrValue(child, SchemaSymbols.ATT_NAME).length() != 0) {
                                        XSDHandler xSDHandler = this.fSchemaHandler;
                                        if (xSDocumentInfo2.fTargetNamespace == null) {
                                            childName = childName3;
                                            defaultAtt3 = "," + DOMUtil.getAttrValue(child, SchemaSymbols.ATT_NAME);
                                        } else {
                                            childName = childName3;
                                            defaultAtt3 = String.valueOf(xSDocumentInfo2.fTargetNamespace) + "," + DOMUtil.getAttrValue(child, SchemaSymbols.ATT_NAME);
                                        }
                                        finalAtt = finalAtt2;
                                        defaultAtt2 = defaultAtt4;
                                        blockAtt = blockAtt3;
                                        abstractAtt = abstractAtt2;
                                        typeAtt4 = typeAtt2;
                                        schemaGrammar3 = grammar;
                                        xSDHandler.checkForDuplicateNames(defaultAtt3, 1, this.fSchemaHandler.getIDRegistry(), this.fSchemaHandler.getIDRegistry_sub(), child, schemaDoc);
                                    } else {
                                        childName = childName3;
                                        finalAtt = finalAtt2;
                                        defaultAtt2 = defaultAtt4;
                                        typeAtt4 = typeAtt2;
                                        blockAtt = blockAtt3;
                                        schemaGrammar3 = schemaGrammar;
                                        abstractAtt = abstractAtt2;
                                    }
                                } else {
                                    if (childName3.equals(SchemaSymbols.ELT_KEYREF)) {
                                        this.fSchemaHandler.storeKeyRef(child, xSDocumentInfo2, element3);
                                    }
                                    childName = childName3;
                                    finalAtt = finalAtt2;
                                    defaultAtt2 = defaultAtt4;
                                    typeAtt4 = typeAtt2;
                                    blockAtt = blockAtt3;
                                    schemaGrammar3 = schemaGrammar;
                                    abstractAtt = abstractAtt2;
                                }
                                child = DOMUtil.getNextSiblingElement(child);
                                if (child != null) {
                                    String childName4 = DOMUtil.getLocalName(child);
                                    xSDocumentInfo2 = schemaDoc;
                                    schemaGrammar = schemaGrammar3;
                                    typeAtt2 = typeAtt4;
                                    childName3 = childName4;
                                    finalAtt2 = finalAtt;
                                    defaultAtt4 = defaultAtt2;
                                    abstractAtt2 = abstractAtt;
                                } else {
                                    xSDocumentInfo2 = schemaDoc;
                                    schemaGrammar = schemaGrammar3;
                                    typeAtt2 = typeAtt4;
                                    finalAtt2 = finalAtt;
                                    defaultAtt4 = defaultAtt2;
                                    abstractAtt2 = abstractAtt;
                                    childName3 = childName;
                                }
                                blockAtt3 = blockAtt;
                            } else {
                                schemaGrammar2 = schemaGrammar;
                                defaultAtt = defaultAtt4;
                                typeAtt3 = typeAtt2;
                                s = 0;
                                break;
                            }
                        }
                        schemaGrammar2 = schemaGrammar;
                        defaultAtt = defaultAtt4;
                        typeAtt3 = typeAtt2;
                        s = 0;
                    } else {
                        schemaGrammar2 = schemaGrammar;
                        defaultAtt = defaultAtt4;
                        typeAtt3 = typeAtt2;
                        s = 0;
                    }
                    if (nameAtt2 != null) {
                        if (isGlobal) {
                            i = 2;
                            Object[] objArr = new Object[2];
                            objArr[s] = SchemaSymbols.ELT_ELEMENT;
                            i2 = 1;
                            objArr[1] = SchemaSymbols.ATT_NAME;
                            reportSchemaError("s4s-att-must-appear", objArr, elmDecl);
                        } else {
                            i = 2;
                            i2 = 1;
                            reportSchemaError("src-element.2.1", null, elmDecl);
                        }
                        nameAtt3 = "(no name)";
                    } else {
                        i = 2;
                        i2 = 1;
                        nameAtt3 = nameAtt2;
                    }
                    if (child != null) {
                        Object[] objArr2 = new Object[3];
                        objArr2[s] = nameAtt3;
                        objArr2[i2] = "(annotation?, (simpleType | complexType)?, (unique | key | keyref)*))";
                        objArr2[i] = DOMUtil.getLocalName(child);
                        reportSchemaError("s4s-elt-must-match.1", objArr2, child);
                    }
                    if (defaultAtt != null && fixedAtt != null) {
                        Object[] objArr3 = new Object[i2];
                        objArr3[s] = nameAtt3;
                        reportSchemaError("src-element.1", objArr3, elmDecl);
                    }
                    if (haveAnonType && typeAtt3 != null) {
                        Object[] objArr4 = new Object[i2];
                        objArr4[s] = nameAtt3;
                        reportSchemaError("src-element.3", objArr4, elmDecl);
                    }
                    checkNotationType(nameAtt3, elementType, elmDecl);
                    if (element3.fDefault == null) {
                        xSDocumentInfo = schemaDoc;
                        this.fValidationState.setNamespaceSupport(xSDocumentInfo.fNamespaceSupport);
                        if (XSConstraints.ElementDefaultValidImmediate(element3.fType, element3.fDefault.normalizedValue, this.fValidationState, element3.fDefault) == null) {
                            Object[] objArr5 = new Object[i];
                            objArr5[s] = nameAtt3;
                            objArr5[1] = element3.fDefault.normalizedValue;
                            reportSchemaError("e-props-correct.2", objArr5, elmDecl);
                            element3.fDefault = null;
                            element3.setConstraintType(s);
                        }
                    } else {
                        xSDocumentInfo = schemaDoc;
                    }
                    if (element3.fSubGroup == null && !XSConstraints.checkTypeDerivationOk(element3.fType, element3.fSubGroup.fType, element3.fSubGroup.fFinal)) {
                        Object[] objArr6 = new Object[i];
                        objArr6[s] = nameAtt3;
                        QName subGroupAtt3 = subGroupAtt;
                        objArr6[1] = String.valueOf(subGroupAtt3.prefix) + ":" + subGroupAtt3.localpart;
                        reportSchemaError("e-props-correct.4", objArr6, elmDecl);
                        element3.fSubGroup = null;
                    }
                    if (element3.fDefault == null && ((elementType.getTypeCategory() == 16 && ((XSSimpleType) elementType).isIDType()) || (elementType.getTypeCategory() == 15 && ((XSComplexTypeDecl) elementType).containsTypeID()))) {
                        Object[] objArr7 = new Object[1];
                        objArr7[s] = element3.fName;
                        reportSchemaError("e-props-correct.5", objArr7, elmDecl);
                        xSElementDecl = null;
                        element3.fDefault = null;
                        element3.setConstraintType(s);
                    } else {
                        xSElementDecl = null;
                    }
                    if (element3.fName != null) {
                        return xSElementDecl;
                    }
                    if (isGlobal) {
                        schemaGrammar2.addGlobalElementDeclAll(element3);
                        if (schemaGrammar2.getGlobalElementDecl(element3.fName) == null) {
                            schemaGrammar2.addGlobalElementDecl(element3);
                        }
                        String loc = this.fSchemaHandler.schemaDocument2SystemId(xSDocumentInfo);
                        XSElementDecl element22 = schemaGrammar2.getGlobalElementDecl(element3.fName, loc);
                        if (element22 == null) {
                            schemaGrammar2.addGlobalElementDecl(element3, loc);
                        }
                        if (this.fSchemaHandler.fTolerateDuplicates) {
                            if (element22 != null) {
                                element3 = element22;
                            }
                            this.fSchemaHandler.addGlobalElementDecl(element3);
                        }
                    }
                    return element3;
                }
            }
        }
        element.fBlock = blockAtt2 != null ? xSDocumentInfo2.fBlockDefault : blockAtt2.shortValue();
        element.fFinal = finalAtt2 != null ? xSDocumentInfo2.fFinalDefault : finalAtt2.shortValue();
        element.fBlock = (short) (element.fBlock & 7);
        XInt blockAtt32 = blockAtt2;
        element.fFinal = (short) (element.fFinal & 3);
        if (nillableAtt.booleanValue()) {
        }
        if (abstractAtt2 != null) {
            element.setIsAbstract();
        }
        if (fixedAtt2 == null) {
        }
        if (subGroupAtt2 == null) {
        }
        child = DOMUtil.getFirstChildElement(elmDecl);
        if (child == null) {
            text = DOMUtil.getSyntheticAnnotation(elmDecl);
            if (text == null) {
            }
        }
        if (annotation == null) {
        }
        element3 = element2;
        element3.fAnnotations = xSObjectListImpl;
        elementType = null;
        haveAnonType = false;
        if (child == null) {
        }
        if (elementType != null) {
        }
        if (elementType == null) {
            elementType = element3.fSubGroup.fType;
        }
        if (elementType == null) {
        }
        element3.fType = elementType;
        if (child == null) {
        }
        if (nameAtt2 != null) {
        }
        if (child != null) {
        }
        if (defaultAtt != null) {
            Object[] objArr32 = new Object[i2];
            objArr32[s] = nameAtt3;
            reportSchemaError("src-element.1", objArr32, elmDecl);
        }
        if (haveAnonType) {
            Object[] objArr42 = new Object[i2];
            objArr42[s] = nameAtt3;
            reportSchemaError("src-element.3", objArr42, elmDecl);
        }
        checkNotationType(nameAtt3, elementType, elmDecl);
        if (element3.fDefault == null) {
        }
        if (element3.fSubGroup == null) {
        }
        if (element3.fDefault == null) {
            xSElementDecl = null;
        }
        if (element3.fName != null) {
        }
    }

    @Override
    void reset(SymbolTable symbolTable, boolean validateAnnotations, Locale locale) {
        super.reset(symbolTable, validateAnnotations, locale);
        this.fDeferTraversingLocalElements = true;
    }
}
