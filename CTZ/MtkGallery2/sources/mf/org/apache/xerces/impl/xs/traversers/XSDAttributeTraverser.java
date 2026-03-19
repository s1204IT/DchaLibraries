package mf.org.apache.xerces.impl.xs.traversers;

import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSAnnotationImpl;
import mf.org.apache.xerces.impl.xs.XSAttributeDecl;
import mf.org.apache.xerces.impl.xs.XSAttributeUseImpl;
import mf.org.apache.xerces.impl.xs.XSComplexTypeDecl;
import mf.org.apache.xerces.impl.xs.util.XInt;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.Element;

class XSDAttributeTraverser extends XSDAbstractTraverser {
    public XSDAttributeTraverser(XSDHandler handler, XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    protected XSAttributeUseImpl traverseLocal(Element attrDecl, XSDocumentInfo schemaDoc, SchemaGrammar grammar, XSComplexTypeDecl enclosingCT) {
        XInt useAtt;
        XSAttributeDecl attribute;
        int i;
        short s;
        XSObjectListImpl xSObjectListImpl;
        int i2;
        Object[] attrValues = this.fAttrChecker.checkAttributes(attrDecl, false, schemaDoc);
        String defaultAtt = (String) attrValues[XSAttributeChecker.ATTIDX_DEFAULT];
        String fixedAtt = (String) attrValues[XSAttributeChecker.ATTIDX_FIXED];
        Object nameAtt = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        QName refAtt = (QName) attrValues[XSAttributeChecker.ATTIDX_REF];
        XInt useAtt2 = (XInt) attrValues[XSAttributeChecker.ATTIDX_USE];
        XSAnnotationImpl annotation = null;
        if (attrDecl.getAttributeNode(SchemaSymbols.ATT_REF) != null) {
            if (refAtt != null) {
                attribute = (XSAttributeDecl) this.fSchemaHandler.getGlobalDecl(schemaDoc, 1, refAtt, attrDecl);
                Element child = DOMUtil.getFirstChildElement(attrDecl);
                if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    XSAnnotationImpl annotation2 = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
                    child = DOMUtil.getNextSiblingElement(child);
                    annotation = annotation2;
                    i2 = 1;
                } else {
                    String text = DOMUtil.getSyntheticAnnotation(attrDecl);
                    if (text != null) {
                        i2 = 1;
                        annotation = traverseSyntheticAnnotation(attrDecl, text, attrValues, false, schemaDoc);
                        child = child;
                    } else {
                        i2 = 1;
                    }
                }
                if (child != null) {
                    Object[] objArr = new Object[i2];
                    objArr[0] = refAtt.rawname;
                    reportSchemaError("src-attribute.3.2", objArr, child);
                }
                nameAtt = refAtt.localpart;
            } else {
                attribute = null;
            }
            useAtt = useAtt2;
        } else {
            useAtt = useAtt2;
            attribute = traverseNamedAttr(attrDecl, attrValues, schemaDoc, grammar, false, enclosingCT);
        }
        XSAttributeDecl attribute2 = attribute;
        XSAnnotationImpl annotation3 = annotation;
        short consType = 0;
        if (defaultAtt != null) {
            consType = 1;
        } else if (fixedAtt != null) {
            consType = 2;
            defaultAtt = fixedAtt;
            fixedAtt = null;
        }
        String defaultAtt2 = defaultAtt;
        XSAttributeUseImpl attrUse = null;
        if (attribute2 != null) {
            if (this.fSchemaHandler.fDeclPool != null) {
                attrUse = this.fSchemaHandler.fDeclPool.getAttributeUse();
            } else {
                attrUse = new XSAttributeUseImpl();
            }
            attrUse.fAttrDecl = attribute2;
            attrUse.fUse = useAtt.shortValue();
            attrUse.fConstraintType = consType;
            if (defaultAtt2 != null) {
                attrUse.fDefault = new ValidatedInfo();
                attrUse.fDefault.normalizedValue = defaultAtt2;
            }
            if (attrDecl.getAttributeNode(SchemaSymbols.ATT_REF) == null) {
                attrUse.fAnnotations = attribute2.getAnnotations();
            } else {
                if (annotation3 != null) {
                    xSObjectListImpl = new XSObjectListImpl();
                    xSObjectListImpl.addXSObject(annotation3);
                } else {
                    xSObjectListImpl = XSObjectListImpl.EMPTY_LIST;
                }
                attrUse.fAnnotations = xSObjectListImpl;
            }
        }
        XSAttributeUseImpl attrUse2 = attrUse;
        if (defaultAtt2 != null && fixedAtt != null) {
            i = 1;
            reportSchemaError("src-attribute.1", new Object[]{nameAtt}, attrDecl);
        } else {
            i = 1;
        }
        if (consType == i && useAtt != null && useAtt.intValue() != 0) {
            Object[] objArr2 = new Object[i];
            objArr2[0] = nameAtt;
            reportSchemaError("src-attribute.2", objArr2, attrDecl);
            attrUse2.fUse = (short) 0;
        }
        if (defaultAtt2 != null && attrUse2 != null) {
            this.fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
            try {
                checkDefaultValid(attrUse2);
                s = 0;
            } catch (InvalidDatatypeValueException ide) {
                reportSchemaError(ide.getKey(), ide.getArgs(), attrDecl);
                s = 0;
                reportSchemaError("a-props-correct.2", new Object[]{nameAtt, defaultAtt2}, attrDecl);
                attrUse2.fDefault = null;
                attrUse2.fConstraintType = (short) 0;
            }
            if (((XSSimpleType) attribute2.getTypeDefinition()).isIDType()) {
                Object[] objArr3 = new Object[1];
                objArr3[s] = nameAtt;
                reportSchemaError("a-props-correct.3", objArr3, attrDecl);
                attrUse2.fDefault = null;
                attrUse2.fConstraintType = s;
            }
            if (attrUse2.fAttrDecl.getConstraintType() == 2 && attrUse2.fConstraintType != 0 && (attrUse2.fConstraintType != 2 || !attrUse2.fAttrDecl.getValInfo().actualValue.equals(attrUse2.fDefault.actualValue))) {
                reportSchemaError("au-props-correct.2", new Object[]{nameAtt, attrUse2.fAttrDecl.getValInfo().stringValue()}, attrDecl);
                attrUse2.fDefault = attrUse2.fAttrDecl.getValInfo();
                attrUse2.fConstraintType = (short) 2;
            }
        }
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return attrUse2;
    }

    protected XSAttributeDecl traverseGlobal(Element attrDecl, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        Object[] attrValues = this.fAttrChecker.checkAttributes(attrDecl, true, schemaDoc);
        XSAttributeDecl attribute = traverseNamedAttr(attrDecl, attrValues, schemaDoc, grammar, true, null);
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return attribute;
    }

    XSAttributeDecl traverseNamedAttr(Element attrDecl, Object[] attrValues, XSDocumentInfo schemaDoc, SchemaGrammar grammar, boolean isGlobal, XSComplexTypeDecl enclosingCT) {
        XSAttributeDecl attribute;
        String tnsAtt;
        String tnsAtt2;
        XSComplexTypeDecl enclCT;
        short scope;
        String tnsAtt3;
        ValidatedInfo attDefault;
        Element child;
        String tnsAtt4;
        boolean z;
        String text;
        ?? r35;
        ValidatedInfo attDefault2;
        XSAttributeDecl attribute2;
        String tnsAtt5;
        QName typeAtt;
        XSAnnotationImpl annotation;
        XSSimpleType attrType;
        boolean haveAnonType;
        XSSimpleType attrType2;
        XSAttributeDecl attribute3;
        XSSimpleType attrType3;
        XSObjectListImpl xSObjectListImpl;
        XSObjectList annotations;
        String nameAtt;
        ValidatedInfo attDefault3;
        ValidatedInfo attDefault4;
        String tnsAtt6;
        XSAttributeDecl attribute4;
        String defaultAtt = (String) attrValues[XSAttributeChecker.ATTIDX_DEFAULT];
        String fixedAtt = (String) attrValues[XSAttributeChecker.ATTIDX_FIXED];
        XInt formAtt = (XInt) attrValues[XSAttributeChecker.ATTIDX_FORM];
        String nameAtt2 = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        QName typeAtt2 = (QName) attrValues[XSAttributeChecker.ATTIDX_TYPE];
        if (this.fSchemaHandler.fDeclPool != null) {
            attribute = this.fSchemaHandler.fDeclPool.getAttributeDecl();
        } else {
            attribute = new XSAttributeDecl();
        }
        XSAttributeDecl attribute5 = attribute;
        if (nameAtt2 != null) {
            nameAtt2 = this.fSymbolTable.addSymbol(nameAtt2);
        }
        XSComplexTypeDecl enclCT2 = null;
        short scope2 = 0;
        if (isGlobal) {
            tnsAtt2 = schemaDoc.fTargetNamespace;
            scope2 = 1;
        } else {
            if (enclosingCT != null) {
                enclCT2 = enclosingCT;
                scope2 = 2;
            }
            if (formAtt == null) {
                tnsAtt = null;
                if (schemaDoc.fAreLocalAttributesQualified) {
                    tnsAtt2 = schemaDoc.fTargetNamespace;
                }
                enclCT = enclCT2;
                String str = tnsAtt;
                scope = scope2;
                tnsAtt3 = str;
            } else {
                tnsAtt = null;
                if (formAtt.intValue() == 1) {
                    tnsAtt2 = schemaDoc.fTargetNamespace;
                }
                enclCT = enclCT2;
                String str2 = tnsAtt;
                scope = scope2;
                tnsAtt3 = str2;
            }
            short constraintType = 0;
            if (isGlobal) {
                if (fixedAtt != null) {
                    attDefault = new ValidatedInfo();
                    attDefault.normalizedValue = fixedAtt;
                    constraintType = 2;
                } else if (defaultAtt != null) {
                    attDefault = new ValidatedInfo();
                    attDefault.normalizedValue = defaultAtt;
                    constraintType = 1;
                }
            } else {
                attDefault = null;
            }
            short constraintType2 = constraintType;
            child = DOMUtil.getFirstChildElement(attrDecl);
            if (child == null) {
                String localName = DOMUtil.getLocalName(child);
                tnsAtt4 = tnsAtt3;
                String tnsAtt7 = SchemaSymbols.ELT_ANNOTATION;
                if (localName.equals(tnsAtt7)) {
                    XSAnnotationImpl annotation2 = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
                    child = DOMUtil.getNextSiblingElement(child);
                    r35 = 0;
                    attDefault2 = attDefault;
                    attribute2 = attribute5;
                    annotation = annotation2;
                    tnsAtt5 = tnsAtt4;
                    typeAtt = typeAtt2;
                    attrType = null;
                    haveAnonType = false;
                    if (child != null) {
                        String childName = DOMUtil.getLocalName(child);
                        if (childName.equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                            attrType = this.fSchemaHandler.fSimpleTypeTraverser.traverseLocal(child, schemaDoc, grammar);
                            haveAnonType = true;
                            child = DOMUtil.getNextSiblingElement(child);
                        }
                    }
                    if (attrType != null || typeAtt == null) {
                        attrType2 = attrType;
                        attribute3 = attribute2;
                    } else {
                        XSTypeDefinition type = (XSTypeDefinition) this.fSchemaHandler.getGlobalDecl(schemaDoc, 7, typeAtt, attrDecl);
                        if (type != null) {
                            attrType2 = attrType;
                            if (type.getTypeCategory() == 16) {
                                attrType2 = (XSSimpleType) type;
                                attribute3 = attribute2;
                            }
                        } else {
                            attrType2 = attrType;
                        }
                        Object[] objArr = new Object[2];
                        objArr[r35] = typeAtt.rawname;
                        objArr[1] = "simpleType definition";
                        reportSchemaError("src-resolve", objArr, attrDecl);
                        if (type == null) {
                            attribute3 = attribute2;
                            attribute3.fUnresolvedTypeName = typeAtt;
                        } else {
                            attribute3 = attribute2;
                        }
                    }
                    if (attrType2 == null) {
                        attrType3 = SchemaGrammar.fAnySimpleType;
                    } else {
                        attrType3 = attrType2;
                    }
                    if (annotation != null) {
                        xSObjectListImpl = new XSObjectListImpl();
                        xSObjectListImpl.addXSObject(annotation);
                    } else {
                        xSObjectListImpl = XSObjectListImpl.EMPTY_LIST;
                    }
                    annotations = xSObjectListImpl;
                    attribute3.setValues(nameAtt2, tnsAtt5, attrType3, constraintType2, scope, attDefault2, enclCT, annotations);
                    if (nameAtt2 == null) {
                        if (isGlobal) {
                            Object[] objArr2 = new Object[2];
                            objArr2[r35] = SchemaSymbols.ELT_ATTRIBUTE;
                            objArr2[1] = SchemaSymbols.ATT_NAME;
                            reportSchemaError("s4s-att-must-appear", objArr2, attrDecl);
                        } else {
                            reportSchemaError("src-attribute.3.1", null, attrDecl);
                        }
                        nameAtt = "(no name)";
                    } else {
                        nameAtt = nameAtt2;
                    }
                    if (child != null) {
                        Object[] objArr3 = new Object[3];
                        objArr3[r35] = nameAtt;
                        objArr3[1] = "(annotation?, (simpleType?))";
                        objArr3[2] = DOMUtil.getLocalName(child);
                        reportSchemaError("s4s-elt-must-match.1", objArr3, child);
                    }
                    if (defaultAtt != null && fixedAtt != null) {
                        Object[] objArr4 = new Object[1];
                        objArr4[r35] = nameAtt;
                        reportSchemaError("src-attribute.1", objArr4, attrDecl);
                    }
                    if (haveAnonType && typeAtt != null) {
                        Object[] objArr5 = new Object[1];
                        objArr5[r35] = nameAtt;
                        reportSchemaError("src-attribute.4", objArr5, attrDecl);
                    }
                    checkNotationType(nameAtt, attrType3, attrDecl);
                    attDefault3 = attDefault2;
                    if (attDefault3 != null) {
                        this.fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
                        try {
                            checkDefaultValid(attribute3);
                        } catch (InvalidDatatypeValueException ide) {
                            reportSchemaError(ide.getKey(), ide.getArgs(), attrDecl);
                            Object[] objArr6 = new Object[2];
                            objArr6[r35] = nameAtt;
                            objArr6[1] = attDefault3.normalizedValue;
                            reportSchemaError("a-props-correct.2", objArr6, attrDecl);
                            attDefault4 = null;
                            attribute3.setValues(nameAtt, tnsAtt5, attrType3, (short) 0, scope, null, enclCT, annotations);
                        }
                    }
                    attDefault4 = attDefault3;
                    if (attDefault4 != null && attrType3.isIDType()) {
                        Object[] objArr7 = new Object[1];
                        objArr7[r35] = nameAtt;
                        reportSchemaError("a-props-correct.3", objArr7, attrDecl);
                        attDefault4 = null;
                        attribute3.setValues(nameAtt, tnsAtt5, attrType3, (short) 0, scope, null, enclCT, annotations);
                    }
                    if (nameAtt == null && nameAtt.equals(XMLSymbols.PREFIX_XMLNS)) {
                        reportSchemaError("no-xmlns", null, attrDecl);
                        return null;
                    }
                    tnsAtt6 = tnsAtt5;
                    if (tnsAtt6 == null && tnsAtt6.equals(SchemaSymbols.URI_XSI)) {
                        Object[] objArr8 = new Object[1];
                        objArr8[r35] = SchemaSymbols.URI_XSI;
                        reportSchemaError("no-xsi", objArr8, attrDecl);
                        return null;
                    }
                    if (nameAtt.equals("(no name)")) {
                        return null;
                    }
                    if (isGlobal) {
                        if (grammar.getGlobalAttributeDecl(nameAtt) == null) {
                            grammar.addGlobalAttributeDecl(attribute3);
                        }
                        String loc = this.fSchemaHandler.schemaDocument2SystemId(schemaDoc);
                        XSAttributeDecl attribute22 = grammar.getGlobalAttributeDecl(nameAtt, loc);
                        if (attribute22 == null) {
                            grammar.addGlobalAttributeDecl(attribute3, loc);
                        }
                        if (this.fSchemaHandler.fTolerateDuplicates) {
                            if (attribute22 != null) {
                                attribute4 = attribute22;
                            } else {
                                attribute4 = attribute3;
                            }
                            this.fSchemaHandler.addGlobalAttributeDecl(attribute4);
                            return attribute4;
                        }
                    }
                    return attribute3;
                }
                z = false;
            } else {
                tnsAtt4 = tnsAtt3;
                z = false;
            }
            text = DOMUtil.getSyntheticAnnotation(attrDecl);
            if (text != null) {
                r35 = z;
                attDefault2 = attDefault;
                attribute2 = attribute5;
                tnsAtt5 = tnsAtt4;
                typeAtt = typeAtt2;
                annotation = null;
            } else {
                r35 = z;
                tnsAtt5 = tnsAtt4;
                attDefault2 = attDefault;
                attribute2 = attribute5;
                typeAtt = typeAtt2;
                XSAnnotationImpl annotation3 = traverseSyntheticAnnotation(attrDecl, text, attrValues, false, schemaDoc);
                annotation = annotation3;
                child = child;
            }
            attrType = null;
            haveAnonType = false;
            if (child != null) {
            }
            if (attrType != null) {
                attrType2 = attrType;
                attribute3 = attribute2;
            }
            if (attrType2 == null) {
            }
            if (annotation != null) {
            }
            annotations = xSObjectListImpl;
            attribute3.setValues(nameAtt2, tnsAtt5, attrType3, constraintType2, scope, attDefault2, enclCT, annotations);
            if (nameAtt2 == null) {
            }
            if (child != null) {
            }
            if (defaultAtt != null) {
            }
            if (haveAnonType) {
                Object[] objArr52 = new Object[1];
                objArr52[r35] = nameAtt;
                reportSchemaError("src-attribute.4", objArr52, attrDecl);
            }
            checkNotationType(nameAtt, attrType3, attrDecl);
            attDefault3 = attDefault2;
            if (attDefault3 != null) {
            }
            attDefault4 = attDefault3;
            if (attDefault4 != null) {
                Object[] objArr72 = new Object[1];
                objArr72[r35] = nameAtt;
                reportSchemaError("a-props-correct.3", objArr72, attrDecl);
                attDefault4 = null;
                attribute3.setValues(nameAtt, tnsAtt5, attrType3, (short) 0, scope, null, enclCT, annotations);
            }
            if (nameAtt == null) {
            }
            tnsAtt6 = tnsAtt5;
            if (tnsAtt6 == null) {
            }
            if (nameAtt.equals("(no name)")) {
            }
        }
        enclCT = enclCT2;
        scope = scope2;
        tnsAtt3 = tnsAtt2;
        short constraintType3 = 0;
        if (isGlobal) {
        }
        short constraintType22 = constraintType3;
        child = DOMUtil.getFirstChildElement(attrDecl);
        if (child == null) {
        }
        text = DOMUtil.getSyntheticAnnotation(attrDecl);
        if (text != null) {
        }
        attrType = null;
        haveAnonType = false;
        if (child != null) {
        }
        if (attrType != null) {
        }
        if (attrType2 == null) {
        }
        if (annotation != null) {
        }
        annotations = xSObjectListImpl;
        attribute3.setValues(nameAtt2, tnsAtt5, attrType3, constraintType22, scope, attDefault2, enclCT, annotations);
        if (nameAtt2 == null) {
        }
        if (child != null) {
        }
        if (defaultAtt != null) {
        }
        if (haveAnonType) {
        }
        checkNotationType(nameAtt, attrType3, attrDecl);
        attDefault3 = attDefault2;
        if (attDefault3 != null) {
        }
        attDefault4 = attDefault3;
        if (attDefault4 != null) {
        }
        if (nameAtt == null) {
        }
        tnsAtt6 = tnsAtt5;
        if (tnsAtt6 == null) {
        }
        if (nameAtt.equals("(no name)")) {
        }
    }

    void checkDefaultValid(XSAttributeDecl attribute) throws InvalidDatatypeValueException {
        ((XSSimpleType) attribute.getTypeDefinition()).validate(attribute.getValInfo().normalizedValue, (ValidationContext) this.fValidationState, attribute.getValInfo());
        ((XSSimpleType) attribute.getTypeDefinition()).validate(attribute.getValInfo().stringValue(), (ValidationContext) this.fValidationState, attribute.getValInfo());
    }

    void checkDefaultValid(XSAttributeUseImpl attrUse) throws InvalidDatatypeValueException {
        ((XSSimpleType) attrUse.fAttrDecl.getTypeDefinition()).validate(attrUse.fDefault.normalizedValue, (ValidationContext) this.fValidationState, attrUse.fDefault);
        ((XSSimpleType) attrUse.fAttrDecl.getTypeDefinition()).validate(attrUse.fDefault.stringValue(), (ValidationContext) this.fValidationState, attrUse.fDefault);
    }
}
