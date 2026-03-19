package mf.org.apache.xerces.impl.xs.traversers;

import java.util.ArrayList;
import java.util.Vector;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeFacetException;
import mf.org.apache.xerces.impl.dv.SchemaDVFactory;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSAnnotationImpl;
import mf.org.apache.xerces.impl.xs.traversers.XSDAbstractTraverser;
import mf.org.apache.xerces.impl.xs.util.XInt;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.Element;

class XSDSimpleTypeTraverser extends XSDAbstractTraverser {
    private boolean fIsBuiltIn;

    XSDSimpleTypeTraverser(XSDHandler handler, XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
        this.fIsBuiltIn = false;
    }

    XSSimpleType traverseGlobal(Element elmNode, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        Object[] attrValues = this.fAttrChecker.checkAttributes(elmNode, true, schemaDoc);
        String nameAtt = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        if (nameAtt == null) {
            attrValues[XSAttributeChecker.ATTIDX_NAME] = "(no name)";
        }
        XSSimpleType type = traverseSimpleTypeDecl(elmNode, attrValues, schemaDoc, grammar);
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        if (nameAtt == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, SchemaSymbols.ATT_NAME}, elmNode);
            type = null;
        }
        if (type != null) {
            if (grammar.getGlobalTypeDecl(type.getName()) == null) {
                grammar.addGlobalSimpleTypeDecl(type);
            }
            String loc = this.fSchemaHandler.schemaDocument2SystemId(schemaDoc);
            XSTypeDefinition type2 = grammar.getGlobalTypeDecl(type.getName(), loc);
            if (type2 == null) {
                grammar.addGlobalSimpleTypeDecl(type, loc);
            }
            if (this.fSchemaHandler.fTolerateDuplicates) {
                if (type2 != null && (type2 instanceof XSSimpleType)) {
                    type = (XSSimpleType) type2;
                }
                this.fSchemaHandler.addGlobalTypeDecl(type);
            }
        }
        return type;
    }

    XSSimpleType traverseLocal(Element elmNode, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        Object[] attrValues = this.fAttrChecker.checkAttributes(elmNode, false, schemaDoc);
        String name = genAnonTypeName(elmNode);
        ?? simpleType = getSimpleType(name, elmNode, attrValues, schemaDoc, grammar);
        if (simpleType instanceof XSSimpleTypeDecl) {
            simpleType.setAnonymous(true);
        }
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return simpleType;
    }

    private XSSimpleType traverseSimpleTypeDecl(Element simpleTypeDecl, Object[] attrValues, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        String name = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        return getSimpleType(name, simpleTypeDecl, attrValues, schemaDoc, grammar);
    }

    private String genAnonTypeName(Element simpleTypeDecl) {
        StringBuffer typeName = new StringBuffer("#AnonType_");
        for (Element node = DOMUtil.getParent(simpleTypeDecl); node != null && node != DOMUtil.getRoot(DOMUtil.getDocument(node)); node = DOMUtil.getParent(node)) {
            typeName.append(node.getAttribute(SchemaSymbols.ATT_NAME));
        }
        return typeName.toString();
    }

    private XSSimpleType getSimpleType(String name, Element simpleTypeDecl, Object[] attrValues, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        char c;
        char c2;
        Element child;
        short refType;
        boolean z;
        Object[] contentAttrs;
        Vector memberTypes;
        XSAnnotationImpl[] annotations;
        Element content;
        XSAnnotationImpl[] annotations2;
        XSAnnotationImpl[] annotations3;
        ArrayList dTValidators;
        XSSimpleType baseValidator;
        XSSimpleType baseValidator2;
        XSAnnotationImpl[] annotations4;
        Object[] contentAttrs2;
        XSSimpleType newDecl;
        Element content2;
        XSObjectListImpl xSObjectListImpl;
        short s;
        short s2;
        Object[] contentAttrs3;
        XSObjectListImpl xSObjectListImpl2;
        Object[] contentAttrs4;
        Vector memberTypes2;
        char c3;
        XInt finalAttr = (XInt) attrValues[XSAttributeChecker.ATTIDX_FINAL];
        int finalProperty = finalAttr == null ? schemaDoc.fFinalDefault : finalAttr.intValue();
        Element child2 = DOMUtil.getFirstChildElement(simpleTypeDecl);
        if (child2 == null || !DOMUtil.getLocalName(child2).equals(SchemaSymbols.ELT_ANNOTATION)) {
            String text = DOMUtil.getSyntheticAnnotation(simpleTypeDecl);
            if (text != null) {
                c = 1;
                c2 = 0;
                annotations = new XSAnnotationImpl[]{traverseSyntheticAnnotation(simpleTypeDecl, text, attrValues, false, schemaDoc)};
            }
            XSAnnotationImpl[] xSAnnotationImplArr = annotations;
            child = child2;
            XSAnnotationImpl[] annotations5 = xSAnnotationImplArr;
            if (child != null) {
                Object[] objArr = new Object[2];
                objArr[c2] = SchemaSymbols.ELT_SIMPLETYPE;
                objArr[c] = "(annotation?, (restriction | list | union))";
                reportSchemaError("s4s-elt-must-match.2", objArr, simpleTypeDecl);
                return errorType(name, schemaDoc.fTargetNamespace, (short) 2);
            }
            String varietyProperty = DOMUtil.getLocalName(child);
            boolean restriction = false;
            boolean list = false;
            boolean union = false;
            if (varietyProperty.equals(SchemaSymbols.ELT_RESTRICTION)) {
                refType = 2;
                restriction = true;
            } else if (varietyProperty.equals(SchemaSymbols.ELT_LIST)) {
                refType = 16;
                list = true;
            } else {
                if (!varietyProperty.equals(SchemaSymbols.ELT_UNION)) {
                    reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, "(annotation?, (restriction | list | union))", varietyProperty}, simpleTypeDecl);
                    return errorType(name, schemaDoc.fTargetNamespace, (short) 2);
                }
                refType = 8;
                union = true;
            }
            short refType2 = refType;
            boolean restriction2 = restriction;
            boolean list2 = list;
            boolean union2 = union;
            Element nextChild = DOMUtil.getNextSiblingElement(child);
            if (nextChild != null) {
                z = false;
                reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_SIMPLETYPE, "(annotation?, (restriction | list | union))", DOMUtil.getLocalName(nextChild)}, nextChild);
            } else {
                z = false;
            }
            Object[] contentAttrs5 = this.fAttrChecker.checkAttributes(child, z, schemaDoc);
            QName baseTypeName = (QName) contentAttrs5[restriction2 ? XSAttributeChecker.ATTIDX_BASE : XSAttributeChecker.ATTIDX_ITEMTYPE];
            Vector memberTypes3 = (Vector) contentAttrs5[XSAttributeChecker.ATTIDX_MEMBERTYPES];
            Element content3 = DOMUtil.getFirstChildElement(child);
            if (content3 == null || !DOMUtil.getLocalName(content3).equals(SchemaSymbols.ELT_ANNOTATION)) {
                String text2 = DOMUtil.getSyntheticAnnotation(child);
                if (text2 != null) {
                    contentAttrs = contentAttrs5;
                    memberTypes = memberTypes3;
                    XSAnnotationImpl annotation = traverseSyntheticAnnotation(child, text2, contentAttrs, false, schemaDoc);
                    if (annotations5 == null) {
                        annotations2 = new XSAnnotationImpl[]{annotation};
                    } else {
                        XSAnnotationImpl[] tempArray = new XSAnnotationImpl[2];
                        tempArray[0] = annotations5[0];
                        annotations2 = tempArray;
                        annotations2[1] = annotation;
                    }
                    annotations = annotations2;
                    content = content3;
                } else {
                    contentAttrs = contentAttrs5;
                    memberTypes = memberTypes3;
                    annotations = annotations5;
                    content = content3;
                }
            } else {
                XSAnnotationImpl annotation2 = traverseAnnotationDecl(content3, contentAttrs5, false, schemaDoc);
                if (annotation2 == null) {
                    contentAttrs4 = contentAttrs5;
                    memberTypes2 = memberTypes3;
                    c3 = 2;
                } else if (annotations5 == null) {
                    contentAttrs4 = contentAttrs5;
                    memberTypes2 = memberTypes3;
                    annotations5 = new XSAnnotationImpl[]{annotation2};
                    c3 = 2;
                } else {
                    contentAttrs4 = contentAttrs5;
                    memberTypes2 = memberTypes3;
                    c3 = 2;
                    XSAnnotationImpl[] tempArray2 = new XSAnnotationImpl[2];
                    tempArray2[0] = annotations5[0];
                    annotations5 = tempArray2;
                    annotations5[1] = annotation2;
                }
                contentAttrs = contentAttrs4;
                memberTypes = memberTypes2;
                annotations = annotations5;
                content = DOMUtil.getNextSiblingElement(content3);
            }
            XSSimpleType baseValidator3 = null;
            if ((restriction2 || list2) && baseTypeName != null) {
                annotations3 = annotations;
                baseValidator3 = findDTValidator(child, name, baseTypeName, refType2, schemaDoc);
                if (baseValidator3 == null && this.fIsBuiltIn) {
                    this.fIsBuiltIn = false;
                    return null;
                }
            } else {
                annotations3 = annotations;
            }
            ArrayList dTValidators2 = null;
            if (union2 && memberTypes != null && memberTypes.size() > 0) {
                int size = memberTypes.size();
                ArrayList dTValidators3 = new ArrayList(size);
                int i = 0;
                while (i < size) {
                    int i2 = i;
                    ArrayList dTValidators4 = dTValidators3;
                    int size2 = size;
                    XSSimpleType dv = findDTValidator(child, name, (QName) memberTypes.elementAt(i), (short) 8, schemaDoc);
                    if (dv != null) {
                        if (dv.getVariety() == 3) {
                            XSObjectList dvs = dv.getMemberTypes();
                            for (int j = 0; j < dvs.getLength(); j++) {
                                dTValidators4.add(dvs.item(j));
                            }
                        } else {
                            dTValidators4.add(dv);
                        }
                    }
                    i = i2 + 1;
                    dTValidators3 = dTValidators4;
                    size = size2;
                }
                dTValidators2 = dTValidators3;
            }
            if (content != null && DOMUtil.getLocalName(content).equals(SchemaSymbols.ELT_SIMPLETYPE)) {
                if (restriction2 || list2) {
                    if (baseTypeName != null) {
                        reportSchemaError(list2 ? "src-simple-type.3.a" : "src-simple-type.2.a", null, content);
                    }
                    if (baseValidator3 == null) {
                        baseValidator3 = traverseLocal(content, schemaDoc, grammar);
                    }
                    content = DOMUtil.getNextSiblingElement(content);
                    dTValidators = dTValidators2;
                } else if (union2) {
                    if (dTValidators2 == null) {
                        dTValidators2 = new ArrayList(2);
                    }
                    do {
                        XSSimpleType dv2 = traverseLocal(content, schemaDoc, grammar);
                        if (dv2 != null) {
                            if (dv2.getVariety() == 3) {
                                XSObjectList dvs2 = dv2.getMemberTypes();
                                for (int j2 = 0; j2 < dvs2.getLength(); j2++) {
                                    dTValidators2.add(dvs2.item(j2));
                                }
                            } else {
                                dTValidators2.add(dv2);
                            }
                        }
                        content = DOMUtil.getNextSiblingElement(content);
                        if (content == null) {
                            break;
                        }
                    } while (DOMUtil.getLocalName(content).equals(SchemaSymbols.ELT_SIMPLETYPE));
                    dTValidators = dTValidators2;
                }
                baseValidator = baseValidator3;
                if (restriction2) {
                    this.fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
                    return errorType(name, schemaDoc.fTargetNamespace, restriction2 ? (short) 2 : (short) 16);
                }
                this.fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
                return errorType(name, schemaDoc.fTargetNamespace, restriction2 ? (short) 2 : (short) 16);
                if (!union2) {
                }
                if (!list2) {
                }
                XSSimpleType newDecl2 = null;
                if (restriction2) {
                }
                newDecl = newDecl2;
                if (restriction2) {
                    newDecl = newDecl;
                }
                if (content != null) {
                }
                this.fAttrChecker.returnAttrArray(contentAttrs2, schemaDoc);
                return newDecl;
            }
            if ((!restriction2 && !list2) || baseTypeName != null) {
                if (union2 && (memberTypes == null || memberTypes.size() == 0)) {
                    reportSchemaError("src-union-memberTypes-or-simpleTypes", null, child);
                }
                dTValidators = dTValidators2;
                baseValidator = baseValidator3;
                if ((restriction2 || list2) && baseValidator == null) {
                    this.fAttrChecker.returnAttrArray(contentAttrs, schemaDoc);
                    return errorType(name, schemaDoc.fTargetNamespace, restriction2 ? (short) 2 : (short) 16);
                }
                Object[] contentAttrs6 = contentAttrs;
                if (!union2 && (dTValidators == null || dTValidators.size() == 0)) {
                    this.fAttrChecker.returnAttrArray(contentAttrs6, schemaDoc);
                    return errorType(name, schemaDoc.fTargetNamespace, (short) 8);
                }
                if (!list2 && isListDatatype(baseValidator)) {
                    reportSchemaError("cos-st-restricts.2.1", new Object[]{name, baseValidator.getName()}, child);
                    this.fAttrChecker.returnAttrArray(contentAttrs6, schemaDoc);
                    return errorType(name, schemaDoc.fTargetNamespace, (short) 16);
                }
                XSSimpleType newDecl22 = null;
                if (restriction2) {
                    SchemaDVFactory schemaDVFactory = this.fSchemaHandler.fDVFactory;
                    String str = schemaDoc.fTargetNamespace;
                    short s3 = (short) finalProperty;
                    annotations4 = annotations3;
                    if (annotations4 == null) {
                        s2 = s3;
                        contentAttrs3 = contentAttrs6;
                        xSObjectListImpl2 = null;
                    } else {
                        s2 = s3;
                        contentAttrs3 = contentAttrs6;
                        xSObjectListImpl2 = new XSObjectListImpl(annotations4, annotations4.length);
                    }
                    contentAttrs2 = contentAttrs3;
                    baseValidator2 = baseValidator;
                    newDecl22 = schemaDVFactory.createTypeRestriction(name, str, s2, baseValidator, xSObjectListImpl2);
                } else {
                    ArrayList dTValidators5 = dTValidators;
                    baseValidator2 = baseValidator;
                    annotations4 = annotations3;
                    contentAttrs2 = contentAttrs6;
                    if (list2) {
                        newDecl22 = this.fSchemaHandler.fDVFactory.createTypeList(name, schemaDoc.fTargetNamespace, (short) finalProperty, baseValidator2, annotations4 == null ? null : new XSObjectListImpl(annotations4, annotations4.length));
                    } else if (union2) {
                        XSSimpleType[] memberDecls = (XSSimpleType[]) dTValidators5.toArray(new XSSimpleType[dTValidators5.size()]);
                        newDecl22 = this.fSchemaHandler.fDVFactory.createTypeUnion(name, schemaDoc.fTargetNamespace, (short) finalProperty, memberDecls, annotations4 == null ? null : new XSObjectListImpl(annotations4, annotations4.length));
                    }
                }
                newDecl = newDecl22;
                if (!restriction2 || content == null) {
                    newDecl = newDecl;
                } else {
                    XSSimpleType xSSimpleType = baseValidator2;
                    XSDAbstractTraverser.FacetInfo fi = traverseFacets(content, xSSimpleType, schemaDoc);
                    Element content4 = fi.nodeAfterFacets;
                    try {
                        this.fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
                        content2 = content4;
                        try {
                            newDecl.applyFacets(fi.facetdata, fi.fPresentFacets, fi.fFixedFacets, this.fValidationState);
                            content = content2;
                        } catch (InvalidDatatypeFacetException e) {
                            ex = e;
                            reportSchemaError(ex.getKey(), ex.getArgs(), child);
                            SchemaDVFactory schemaDVFactory2 = this.fSchemaHandler.fDVFactory;
                            String str2 = schemaDoc.fTargetNamespace;
                            short s4 = (short) finalProperty;
                            if (annotations4 == null) {
                                s = s4;
                                xSObjectListImpl = null;
                            } else {
                                s = s4;
                                xSObjectListImpl = new XSObjectListImpl(annotations4, annotations4.length);
                            }
                            newDecl = schemaDVFactory2.createTypeRestriction(name, str2, s, xSSimpleType, xSObjectListImpl);
                            content = content2;
                        }
                    } catch (InvalidDatatypeFacetException e2) {
                        ex = e2;
                        content2 = content4;
                    }
                }
                if (content != null) {
                    if (restriction2) {
                        reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_RESTRICTION, "(annotation?, (simpleType?, (minExclusive | minInclusive | maxExclusive | maxInclusive | totalDigits | fractionDigits | length | minLength | maxLength | enumeration | whiteSpace | pattern)*))", DOMUtil.getLocalName(content)}, content);
                    } else if (list2) {
                        reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_LIST, "(annotation?, (simpleType?))", DOMUtil.getLocalName(content)}, content);
                    } else if (union2) {
                        reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_UNION, "(annotation?, (simpleType*))", DOMUtil.getLocalName(content)}, content);
                    }
                }
                this.fAttrChecker.returnAttrArray(contentAttrs2, schemaDoc);
                return newDecl;
            }
            reportSchemaError(list2 ? "src-simple-type.3.b" : "src-simple-type.2.b", null, child);
            dTValidators = dTValidators2;
            baseValidator = baseValidator3;
            if (restriction2) {
            }
            if (!union2) {
            }
            if (!list2) {
            }
            XSSimpleType newDecl222 = null;
            if (restriction2) {
            }
            newDecl = newDecl222;
            if (restriction2) {
            }
            if (content != null) {
            }
            this.fAttrChecker.returnAttrArray(contentAttrs2, schemaDoc);
            return newDecl;
        }
        XSAnnotationImpl annotation3 = traverseAnnotationDecl(child2, attrValues, false, schemaDoc);
        annotations = annotation3 != null ? new XSAnnotationImpl[]{annotation3} : null;
        child2 = DOMUtil.getNextSiblingElement(child2);
        c2 = 0;
        c = 1;
        XSAnnotationImpl[] xSAnnotationImplArr2 = annotations;
        child = child2;
        XSAnnotationImpl[] annotations52 = xSAnnotationImplArr2;
        if (child != null) {
        }
    }

    private XSSimpleType findDTValidator(Element elm, String refName, QName baseTypeStr, short baseRefContext, XSDocumentInfo schemaDoc) {
        XSTypeDefinition baseType;
        if (baseTypeStr == null || (baseType = (XSTypeDefinition) this.fSchemaHandler.getGlobalDecl(schemaDoc, 7, baseTypeStr, elm)) == null) {
            return null;
        }
        if (baseType.getTypeCategory() != 16) {
            reportSchemaError("cos-st-restricts.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            return null;
        }
        if (baseType == SchemaGrammar.fAnySimpleType && baseRefContext == 2) {
            if (checkBuiltIn(refName, schemaDoc.fTargetNamespace)) {
                return null;
            }
            reportSchemaError("cos-st-restricts.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            return null;
        }
        if ((baseType.getFinal() & baseRefContext) != 0) {
            if (baseRefContext == 2) {
                reportSchemaError("st-props-correct.3", new Object[]{refName, baseTypeStr.rawname}, elm);
            } else if (baseRefContext == 16) {
                reportSchemaError("cos-st-restricts.2.3.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            } else if (baseRefContext == 8) {
                reportSchemaError("cos-st-restricts.3.3.1.1", new Object[]{baseTypeStr.rawname, refName}, elm);
            }
            return null;
        }
        return (XSSimpleType) baseType;
    }

    private final boolean checkBuiltIn(String name, String namespace) {
        if (namespace != SchemaSymbols.URI_SCHEMAFORSCHEMA) {
            return false;
        }
        if (SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(name) != null) {
            this.fIsBuiltIn = true;
        }
        return this.fIsBuiltIn;
    }

    private boolean isListDatatype(XSSimpleType validator) {
        if (validator.getVariety() == 2) {
            return true;
        }
        if (validator.getVariety() == 3) {
            XSObjectList temp = validator.getMemberTypes();
            for (int i = 0; i < temp.getLength(); i++) {
                if (((XSSimpleType) temp.item(i)).getVariety() == 2) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private XSSimpleType errorType(String name, String namespace, short refType) {
        XSSimpleType stringType = (XSSimpleType) SchemaGrammar.SG_SchemaNS.getTypeDefinition(SchemaSymbols.ATTVAL_STRING);
        if (refType == 2) {
            return this.fSchemaHandler.fDVFactory.createTypeRestriction(name, namespace, (short) 0, stringType, null);
        }
        if (refType == 8) {
            return this.fSchemaHandler.fDVFactory.createTypeUnion(name, namespace, (short) 0, new XSSimpleType[]{stringType}, null);
        }
        if (refType == 16) {
            return this.fSchemaHandler.fDVFactory.createTypeList(name, namespace, (short) 0, stringType, null);
        }
        return null;
    }
}
