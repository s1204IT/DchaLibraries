package mf.org.apache.xerces.impl.xs.traversers;

import com.mediatek.plugin.preload.SoOperater;
import java.util.Locale;
import java.util.Vector;
import mf.org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import mf.org.apache.xerces.impl.dv.ValidatedInfo;
import mf.org.apache.xerces.impl.dv.ValidationContext;
import mf.org.apache.xerces.impl.dv.XSFacets;
import mf.org.apache.xerces.impl.dv.XSSimpleType;
import mf.org.apache.xerces.impl.validation.ValidationState;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSAnnotationImpl;
import mf.org.apache.xerces.impl.xs.XSAttributeGroupDecl;
import mf.org.apache.xerces.impl.xs.XSAttributeUseImpl;
import mf.org.apache.xerces.impl.xs.XSComplexTypeDecl;
import mf.org.apache.xerces.impl.xs.XSElementDecl;
import mf.org.apache.xerces.impl.xs.XSParticleDecl;
import mf.org.apache.xerces.impl.xs.XSWildcardDecl;
import mf.org.apache.xerces.impl.xs.util.XInt;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.util.NamespaceSupport;
import mf.org.apache.xerces.util.SymbolTable;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xs.XSAttributeUse;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSSimpleTypeDefinition;
import mf.org.apache.xerces.xs.XSTypeDefinition;
import mf.org.w3c.dom.Element;

abstract class XSDAbstractTraverser {
    protected static final int CHILD_OF_GROUP = 4;
    protected static final int GROUP_REF_WITH_ALL = 2;
    protected static final int NOT_ALL_CONTEXT = 0;
    protected static final String NO_NAME = "(no name)";
    protected static final int PROCESSING_ALL_EL = 1;
    protected static final int PROCESSING_ALL_GP = 8;
    private static final XSSimpleType fQNameDV = (XSSimpleType) SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(SchemaSymbols.ATTVAL_QNAME);
    protected XSAttributeChecker fAttrChecker;
    protected XSDHandler fSchemaHandler;
    protected SymbolTable fSymbolTable = null;
    protected boolean fValidateAnnotations = false;
    ValidationState fValidationState = new ValidationState();
    private StringBuffer fPattern = new StringBuffer();
    private final XSFacets xsFacets = new XSFacets();

    XSDAbstractTraverser(XSDHandler handler, XSAttributeChecker attrChecker) {
        this.fSchemaHandler = null;
        this.fAttrChecker = null;
        this.fSchemaHandler = handler;
        this.fAttrChecker = attrChecker;
    }

    void reset(SymbolTable symbolTable, boolean validateAnnotations, Locale locale) {
        this.fSymbolTable = symbolTable;
        this.fValidateAnnotations = validateAnnotations;
        this.fValidationState.setExtraChecking(false);
        this.fValidationState.setSymbolTable(symbolTable);
        this.fValidationState.setLocale(locale);
    }

    XSAnnotationImpl traverseAnnotationDecl(Element annotationDecl, Object[] parentAttrs, boolean isGlobal, XSDocumentInfo schemaDoc) {
        String prefix;
        String prefix2;
        int i;
        Object[] attrValues = this.fAttrChecker.checkAttributes(annotationDecl, isGlobal, schemaDoc);
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        String contents = DOMUtil.getAnnotation(annotationDecl);
        Element child = DOMUtil.getFirstChildElement(annotationDecl);
        if (child != null) {
            do {
                String name = DOMUtil.getLocalName(child);
                if (name.equals(SchemaSymbols.ELT_APPINFO) || name.equals(SchemaSymbols.ELT_DOCUMENTATION)) {
                    attrValues = this.fAttrChecker.checkAttributes(child, true, schemaDoc);
                    this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
                } else {
                    reportSchemaError("src-annotation", new Object[]{name}, child);
                }
                child = DOMUtil.getNextSiblingElement(child);
            } while (child != null);
        }
        XSAnnotationImpl xSAnnotationImpl = null;
        if (contents != null) {
            SchemaGrammar grammar = this.fSchemaHandler.getGrammar(schemaDoc.fTargetNamespace);
            Vector annotationLocalAttrs = (Vector) parentAttrs[XSAttributeChecker.ATTIDX_NONSCHEMA];
            if (annotationLocalAttrs != null && !annotationLocalAttrs.isEmpty()) {
                StringBuffer localStrBuffer = new StringBuffer(64);
                localStrBuffer.append(" ");
                int i2 = 0;
                while (i2 < annotationLocalAttrs.size()) {
                    int i3 = i2 + 1;
                    String rawname = (String) annotationLocalAttrs.elementAt(i2);
                    int colonIndex = rawname.indexOf(58);
                    if (colonIndex == -1) {
                        prefix = rawname;
                        prefix2 = "";
                    } else {
                        String prefix3 = rawname.substring(0, colonIndex);
                        prefix = rawname.substring(colonIndex + 1);
                        prefix2 = prefix3;
                    }
                    String localpart = prefix;
                    Object[] objArr = attrValues;
                    String uri = schemaDoc.fNamespaceSupport.getURI(this.fSymbolTable.addSymbol(prefix2));
                    if (annotationDecl.getAttributeNS(uri, localpart).length() != 0) {
                        i = i3 + 1;
                    } else {
                        localStrBuffer.append(rawname);
                        localStrBuffer.append("=\"");
                        i = i3 + 1;
                        String value = (String) annotationLocalAttrs.elementAt(i3);
                        localStrBuffer.append(processAttValue(value));
                        localStrBuffer.append("\" ");
                    }
                    i2 = i;
                    attrValues = objArr;
                    xSAnnotationImpl = null;
                }
                StringBuffer contentBuffer = new StringBuffer(contents.length() + localStrBuffer.length());
                int annotationTokenEnd = contents.indexOf(SchemaSymbols.ELT_ANNOTATION);
                if (annotationTokenEnd == -1) {
                    return xSAnnotationImpl;
                }
                int annotationTokenEnd2 = annotationTokenEnd + SchemaSymbols.ELT_ANNOTATION.length();
                contentBuffer.append(contents.substring(0, annotationTokenEnd2));
                contentBuffer.append(localStrBuffer.toString());
                contentBuffer.append(contents.substring(annotationTokenEnd2, contents.length()));
                String annotation = contentBuffer.toString();
                if (this.fValidateAnnotations) {
                    schemaDoc.addAnnotation(new XSAnnotationInfo(annotation, annotationDecl));
                }
                return new XSAnnotationImpl(annotation, grammar);
            }
            if (this.fValidateAnnotations) {
                schemaDoc.addAnnotation(new XSAnnotationInfo(contents, annotationDecl));
            }
            return new XSAnnotationImpl(contents, grammar);
        }
        return null;
    }

    XSAnnotationImpl traverseSyntheticAnnotation(Element annotationParent, String initialContent, Object[] parentAttrs, boolean isGlobal, XSDocumentInfo schemaDoc) {
        String prefix;
        SchemaGrammar grammar = this.fSchemaHandler.getGrammar(schemaDoc.fTargetNamespace);
        Vector annotationLocalAttrs = (Vector) parentAttrs[XSAttributeChecker.ATTIDX_NONSCHEMA];
        if (annotationLocalAttrs != null && !annotationLocalAttrs.isEmpty()) {
            StringBuffer localStrBuffer = new StringBuffer(64);
            localStrBuffer.append(" ");
            int i = 0;
            while (i < annotationLocalAttrs.size()) {
                int i2 = i + 1;
                String rawname = (String) annotationLocalAttrs.elementAt(i);
                int colonIndex = rawname.indexOf(58);
                if (colonIndex == -1) {
                    prefix = "";
                } else {
                    prefix = rawname.substring(0, colonIndex);
                    rawname.substring(colonIndex + 1);
                }
                schemaDoc.fNamespaceSupport.getURI(this.fSymbolTable.addSymbol(prefix));
                localStrBuffer.append(rawname);
                localStrBuffer.append("=\"");
                int i3 = i2 + 1;
                String value = (String) annotationLocalAttrs.elementAt(i2);
                localStrBuffer.append(processAttValue(value));
                localStrBuffer.append("\" ");
                i = i3;
            }
            StringBuffer contentBuffer = new StringBuffer(initialContent.length() + localStrBuffer.length());
            int annotationTokenEnd = initialContent.indexOf(SchemaSymbols.ELT_ANNOTATION);
            if (annotationTokenEnd == -1) {
                return null;
            }
            int annotationTokenEnd2 = annotationTokenEnd + SchemaSymbols.ELT_ANNOTATION.length();
            contentBuffer.append(initialContent.substring(0, annotationTokenEnd2));
            contentBuffer.append(localStrBuffer.toString());
            contentBuffer.append(initialContent.substring(annotationTokenEnd2, initialContent.length()));
            String annotation = contentBuffer.toString();
            if (this.fValidateAnnotations) {
                schemaDoc.addAnnotation(new XSAnnotationInfo(annotation, annotationParent));
            }
            return new XSAnnotationImpl(annotation, grammar);
        }
        if (this.fValidateAnnotations) {
            schemaDoc.addAnnotation(new XSAnnotationInfo(initialContent, annotationParent));
        }
        return new XSAnnotationImpl(initialContent, grammar);
    }

    static final class FacetInfo {
        final short fFixedFacets;
        final short fPresentFacets;
        final XSFacets facetdata;
        final Element nodeAfterFacets;

        FacetInfo(XSFacets facets, Element nodeAfterFacets, short presentFacets, short fixedFacets) {
            this.facetdata = facets;
            this.nodeAfterFacets = nodeAfterFacets;
            this.fPresentFacets = presentFacets;
            this.fFixedFacets = fixedFacets;
        }
    }

    FacetInfo traverseFacets(Element content, XSSimpleType baseValidator, XSDocumentInfo schemaDoc) {
        short facetsPresent;
        XSObjectListImpl enumAnnotations;
        Vector enumData;
        short facetsPresent2;
        boolean hasQName;
        int currentFacet;
        Object[] attrs;
        int i;
        XSAnnotationImpl annotation;
        Vector enumData2;
        XSObjectListImpl enumAnnotations2;
        char c;
        String facet;
        Vector enumData3;
        XSObjectListImpl enumAnnotations3;
        Element child;
        Vector enumData4;
        short facetsFixed;
        boolean z;
        String text;
        Element child2;
        short facetsPresent3;
        String facet2;
        short facetsFixed2;
        XSObjectListImpl enumAnnotations4;
        Element child3;
        boolean hasQName2 = containsQName(baseValidator);
        Vector enumNSDecls = hasQName2 ? new Vector() : null;
        this.xsFacets.reset();
        Element content2 = content;
        Vector enumData5 = null;
        XSObjectListImpl patternAnnotations = null;
        int currentFacet2 = 0;
        short facetsPresent4 = 0;
        XSObjectListImpl enumAnnotations5 = null;
        short facetsFixed3 = 0;
        while (true) {
            if (content2 == null) {
                facetsPresent = facetsPresent4;
                enumAnnotations = enumAnnotations5;
                enumData = enumData5;
            } else {
                String facet3 = DOMUtil.getLocalName(content2);
                XSObjectListImpl enumAnnotations6 = enumAnnotations5;
                if (facet3.equals(SchemaSymbols.ELT_ENUMERATION)) {
                    Object[] attrs2 = this.fAttrChecker.checkAttributes(content2, false, schemaDoc, hasQName2);
                    String enumVal = (String) attrs2[XSAttributeChecker.ATTIDX_VALUE];
                    if (enumVal == null) {
                        reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_ENUMERATION, SchemaSymbols.ATT_VALUE}, content2);
                        this.fAttrChecker.returnAttrArray(attrs2, schemaDoc);
                        content2 = DOMUtil.getNextSiblingElement(content2);
                        enumAnnotations5 = enumAnnotations6;
                    } else {
                        int currentFacet3 = currentFacet2;
                        NamespaceSupport nsDecls = (NamespaceSupport) attrs2[XSAttributeChecker.ATTIDX_ENUMNSDECLS];
                        if (baseValidator.getVariety() == 1 && baseValidator.getPrimitiveKind() == 20) {
                            schemaDoc.fValidationContext.setNamespaceSupport(nsDecls);
                            Object notation = null;
                            try {
                                facet = facet3;
                            } catch (InvalidDatatypeValueException e) {
                                ex = e;
                                facet = facet3;
                            }
                            try {
                                QName temp = (QName) fQNameDV.validate(enumVal, (ValidationContext) schemaDoc.fValidationContext, (ValidatedInfo) null);
                                notation = this.fSchemaHandler.getGlobalDecl(schemaDoc, 6, temp, content2);
                            } catch (InvalidDatatypeValueException e2) {
                                ex = e2;
                                reportSchemaError(ex.getKey(), ex.getArgs(), content2);
                            }
                            if (notation == null) {
                                this.fAttrChecker.returnAttrArray(attrs2, schemaDoc);
                                content2 = DOMUtil.getNextSiblingElement(content2);
                                enumAnnotations5 = enumAnnotations6;
                                currentFacet2 = currentFacet3;
                            } else {
                                schemaDoc.fValidationContext.setNamespaceSupport(schemaDoc.fNamespaceSupport);
                                if (enumData5 != null) {
                                }
                                enumData3.addElement(enumVal);
                                enumAnnotations3.addXSObject(null);
                                if (hasQName2) {
                                }
                                child = DOMUtil.getFirstChildElement(content2);
                                if (child == null) {
                                }
                                text = DOMUtil.getSyntheticAnnotation(content2);
                                if (text == null) {
                                }
                                child3 = child2;
                                if (child3 != null) {
                                }
                                facetsPresent4 = facetsPresent3;
                                enumAnnotations5 = enumAnnotations4;
                                attrs = attrs2;
                                currentFacet2 = currentFacet3;
                                enumData5 = enumData4;
                                facetsFixed3 = facetsFixed2;
                            }
                        } else {
                            facet = facet3;
                            if (enumData5 != null) {
                                Vector enumData6 = new Vector();
                                XSObjectListImpl enumAnnotations7 = new XSObjectListImpl();
                                enumAnnotations3 = enumAnnotations7;
                                enumData3 = enumData6;
                            } else {
                                enumData3 = enumData5;
                                enumAnnotations3 = enumAnnotations6;
                            }
                            enumData3.addElement(enumVal);
                            enumAnnotations3.addXSObject(null);
                            if (hasQName2) {
                                enumNSDecls.addElement(nsDecls);
                            }
                            child = DOMUtil.getFirstChildElement(content2);
                            if (child == null) {
                                enumData4 = enumData3;
                                if (DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                                    enumAnnotations3.addXSObject(enumAnnotations3.getLength() - 1, traverseAnnotationDecl(child, attrs2, false, schemaDoc));
                                    child3 = DOMUtil.getNextSiblingElement(child);
                                    facetsPresent3 = facetsPresent4;
                                    hasQName = hasQName2;
                                    facet2 = facet;
                                    facetsFixed2 = facetsFixed3;
                                    enumAnnotations4 = enumAnnotations3;
                                    if (child3 != null) {
                                        reportSchemaError("s4s-elt-must-match.1", new Object[]{"enumeration", "(annotation?)", DOMUtil.getLocalName(child3)}, child3);
                                    }
                                    facetsPresent4 = facetsPresent3;
                                    enumAnnotations5 = enumAnnotations4;
                                    attrs = attrs2;
                                    currentFacet2 = currentFacet3;
                                    enumData5 = enumData4;
                                    facetsFixed3 = facetsFixed2;
                                } else {
                                    facetsFixed = facetsFixed3;
                                    z = false;
                                }
                            } else {
                                enumData4 = enumData3;
                                facetsFixed = facetsFixed3;
                                z = false;
                            }
                            text = DOMUtil.getSyntheticAnnotation(content2);
                            if (text == null) {
                                facet2 = facet;
                                child2 = child;
                                facetsFixed2 = facetsFixed;
                                facetsPresent3 = facetsPresent4;
                                hasQName = hasQName2;
                                enumAnnotations4 = enumAnnotations3;
                                enumAnnotations4.addXSObject(enumAnnotations3.getLength() - 1, traverseSyntheticAnnotation(content2, text, attrs2, false, schemaDoc));
                            } else {
                                child2 = child;
                                facetsPresent3 = facetsPresent4;
                                hasQName = hasQName2;
                                facet2 = facet;
                                facetsFixed2 = facetsFixed;
                                enumAnnotations4 = enumAnnotations3;
                            }
                            child3 = child2;
                            if (child3 != null) {
                            }
                            facetsPresent4 = facetsPresent3;
                            enumAnnotations5 = enumAnnotations4;
                            attrs = attrs2;
                            currentFacet2 = currentFacet3;
                            enumData5 = enumData4;
                            facetsFixed3 = facetsFixed2;
                        }
                    }
                } else {
                    short facetsFixed4 = facetsFixed3;
                    facetsPresent = facetsPresent4;
                    hasQName = hasQName2;
                    int currentFacet4 = currentFacet2;
                    if (facet3.equals(SchemaSymbols.ELT_PATTERN)) {
                        attrs = this.fAttrChecker.checkAttributes(content2, false, schemaDoc);
                        String patternVal = (String) attrs[XSAttributeChecker.ATTIDX_VALUE];
                        if (patternVal == null) {
                            reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_PATTERN, SchemaSymbols.ATT_VALUE}, content2);
                            this.fAttrChecker.returnAttrArray(attrs, schemaDoc);
                            content2 = DOMUtil.getNextSiblingElement(content2);
                            facetsPresent4 = facetsPresent;
                            enumAnnotations5 = enumAnnotations6;
                            currentFacet2 = currentFacet4;
                            facetsFixed3 = facetsFixed4;
                            hasQName2 = hasQName;
                        } else {
                            if (this.fPattern.length() == 0) {
                                this.fPattern.append(patternVal);
                            } else {
                                this.fPattern.append("|");
                                this.fPattern.append(patternVal);
                            }
                            Element child4 = DOMUtil.getFirstChildElement(content2);
                            if (child4 == null || !DOMUtil.getLocalName(child4).equals(SchemaSymbols.ELT_ANNOTATION)) {
                                String text2 = DOMUtil.getSyntheticAnnotation(content2);
                                if (text2 != null) {
                                    if (patternAnnotations == null) {
                                        patternAnnotations = new XSObjectListImpl();
                                    }
                                    c = 0;
                                    enumAnnotations2 = enumAnnotations6;
                                    enumData2 = enumData5;
                                    patternAnnotations.addXSObject(traverseSyntheticAnnotation(content2, text2, attrs, false, schemaDoc));
                                } else {
                                    enumData2 = enumData5;
                                    enumAnnotations2 = enumAnnotations6;
                                    c = 0;
                                }
                            } else {
                                if (patternAnnotations == null) {
                                    patternAnnotations = new XSObjectListImpl();
                                }
                                patternAnnotations.addXSObject(traverseAnnotationDecl(child4, attrs, false, schemaDoc));
                                child4 = DOMUtil.getNextSiblingElement(child4);
                                enumData2 = enumData5;
                                enumAnnotations2 = enumAnnotations6;
                                c = 0;
                            }
                            if (child4 != null) {
                                Object[] objArr = new Object[3];
                                objArr[c] = "pattern";
                                objArr[1] = "(annotation?)";
                                objArr[2] = DOMUtil.getLocalName(child4);
                                reportSchemaError("s4s-elt-must-match.1", objArr, child4);
                            }
                            facetsPresent4 = facetsPresent;
                            currentFacet2 = currentFacet4;
                            facetsFixed3 = facetsFixed4;
                            enumAnnotations5 = enumAnnotations2;
                            enumData5 = enumData2;
                        }
                    } else {
                        enumData = enumData5;
                        enumAnnotations = enumAnnotations6;
                        if (facet3.equals(SchemaSymbols.ELT_MINLENGTH)) {
                            currentFacet = 2;
                        } else if (facet3.equals(SchemaSymbols.ELT_MAXLENGTH)) {
                            currentFacet = 4;
                        } else if (facet3.equals(SchemaSymbols.ELT_MAXEXCLUSIVE)) {
                            currentFacet = 64;
                        } else if (facet3.equals(SchemaSymbols.ELT_MAXINCLUSIVE)) {
                            currentFacet = 32;
                        } else if (facet3.equals(SchemaSymbols.ELT_MINEXCLUSIVE)) {
                            currentFacet = 128;
                        } else if (facet3.equals(SchemaSymbols.ELT_MININCLUSIVE)) {
                            currentFacet = 256;
                        } else if (facet3.equals(SchemaSymbols.ELT_TOTALDIGITS)) {
                            currentFacet = 512;
                        } else if (facet3.equals(SchemaSymbols.ELT_FRACTIONDIGITS)) {
                            currentFacet = SoOperater.STEP;
                        } else if (facet3.equals(SchemaSymbols.ELT_WHITESPACE)) {
                            currentFacet = 16;
                        } else if (facet3.equals(SchemaSymbols.ELT_LENGTH)) {
                            currentFacet = 1;
                        } else {
                            facetsFixed3 = facetsFixed4;
                        }
                        currentFacet2 = currentFacet;
                        attrs = this.fAttrChecker.checkAttributes(content2, false, schemaDoc);
                        if ((facetsPresent & currentFacet2) != 0) {
                            reportSchemaError("src-single-facet-value", new Object[]{facet3}, content2);
                            this.fAttrChecker.returnAttrArray(attrs, schemaDoc);
                            content2 = DOMUtil.getNextSiblingElement(content2);
                        } else if (attrs[XSAttributeChecker.ATTIDX_VALUE] == null) {
                            if (content2.getAttributeNodeNS(null, "value") == null) {
                                reportSchemaError("s4s-att-must-appear", new Object[]{content2.getLocalName(), SchemaSymbols.ATT_VALUE}, content2);
                            }
                            this.fAttrChecker.returnAttrArray(attrs, schemaDoc);
                            content2 = DOMUtil.getNextSiblingElement(content2);
                        } else {
                            short facetsPresent5 = (short) (facetsPresent | currentFacet2);
                            short facetsFixed5 = ((Boolean) attrs[XSAttributeChecker.ATTIDX_FIXED]).booleanValue() ? (short) (facetsFixed4 | currentFacet2) : facetsFixed4;
                            if (currentFacet2 == 4) {
                                this.xsFacets.maxLength = ((XInt) attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                            } else if (currentFacet2 == 16) {
                                this.xsFacets.whiteSpace = ((XInt) attrs[XSAttributeChecker.ATTIDX_VALUE]).shortValue();
                            } else if (currentFacet2 == 32) {
                                this.xsFacets.maxInclusive = (String) attrs[XSAttributeChecker.ATTIDX_VALUE];
                            } else if (currentFacet2 == 64) {
                                this.xsFacets.maxExclusive = (String) attrs[XSAttributeChecker.ATTIDX_VALUE];
                            } else if (currentFacet2 == 128) {
                                this.xsFacets.minExclusive = (String) attrs[XSAttributeChecker.ATTIDX_VALUE];
                            } else if (currentFacet2 == 256) {
                                this.xsFacets.minInclusive = (String) attrs[XSAttributeChecker.ATTIDX_VALUE];
                            } else if (currentFacet2 == 512) {
                                this.xsFacets.totalDigits = ((XInt) attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                            } else if (currentFacet2 != 1024) {
                                switch (currentFacet2) {
                                    case 1:
                                        this.xsFacets.length = ((XInt) attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                                        break;
                                    case 2:
                                        this.xsFacets.minLength = ((XInt) attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                                        break;
                                }
                            } else {
                                this.xsFacets.fractionDigits = ((XInt) attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                            }
                            Element child5 = DOMUtil.getFirstChildElement(content2);
                            if (child5 == null || !DOMUtil.getLocalName(child5).equals(SchemaSymbols.ELT_ANNOTATION)) {
                                String text3 = DOMUtil.getSyntheticAnnotation(content2);
                                if (text3 != null) {
                                    i = 4;
                                    XSAnnotationImpl annotation2 = traverseSyntheticAnnotation(content2, text3, attrs, false, schemaDoc);
                                    annotation = annotation2;
                                    child5 = child5;
                                } else {
                                    i = 4;
                                    annotation = null;
                                }
                            } else {
                                XSAnnotationImpl annotation3 = traverseAnnotationDecl(child5, attrs, false, schemaDoc);
                                child5 = DOMUtil.getNextSiblingElement(child5);
                                annotation = annotation3;
                                i = 4;
                            }
                            if (currentFacet2 == i) {
                                this.xsFacets.maxLengthAnnotation = annotation;
                            } else if (currentFacet2 == 16) {
                                this.xsFacets.whiteSpaceAnnotation = annotation;
                            } else if (currentFacet2 == 32) {
                                this.xsFacets.maxInclusiveAnnotation = annotation;
                            } else if (currentFacet2 == 64) {
                                this.xsFacets.maxExclusiveAnnotation = annotation;
                            } else if (currentFacet2 == 128) {
                                this.xsFacets.minExclusiveAnnotation = annotation;
                            } else if (currentFacet2 == 256) {
                                this.xsFacets.minInclusiveAnnotation = annotation;
                            } else if (currentFacet2 == 512) {
                                this.xsFacets.totalDigitsAnnotation = annotation;
                            } else if (currentFacet2 != 1024) {
                                switch (currentFacet2) {
                                    case 1:
                                        this.xsFacets.lengthAnnotation = annotation;
                                        break;
                                    case 2:
                                        this.xsFacets.minLengthAnnotation = annotation;
                                        break;
                                }
                            } else {
                                this.xsFacets.fractionDigitsAnnotation = annotation;
                            }
                            if (child5 != null) {
                                reportSchemaError("s4s-elt-must-match.1", new Object[]{facet3, "(annotation?)", DOMUtil.getLocalName(child5)}, child5);
                            }
                            facetsPresent4 = facetsPresent5;
                            facetsFixed3 = facetsFixed5;
                            enumAnnotations5 = enumAnnotations;
                            enumData5 = enumData;
                            this.fAttrChecker.returnAttrArray(attrs, schemaDoc);
                            content2 = DOMUtil.getNextSiblingElement(content2);
                            hasQName2 = hasQName;
                        }
                        facetsPresent4 = facetsPresent;
                        facetsFixed3 = facetsFixed4;
                        hasQName2 = hasQName;
                        enumAnnotations5 = enumAnnotations;
                        enumData5 = enumData;
                    }
                }
                this.fAttrChecker.returnAttrArray(attrs, schemaDoc);
                content2 = DOMUtil.getNextSiblingElement(content2);
                hasQName2 = hasQName;
            }
        }
        Vector enumData7 = enumData;
        if (enumData7 != null) {
            facetsPresent2 = (short) (facetsPresent | XSSimpleTypeDefinition.FACET_ENUMERATION);
            this.xsFacets.enumeration = enumData7;
            this.xsFacets.enumNSDecls = enumNSDecls;
            this.xsFacets.enumAnnotations = enumAnnotations;
        } else {
            facetsPresent2 = facetsPresent;
        }
        if (this.fPattern.length() != 0) {
            facetsPresent2 = (short) (facetsPresent2 | 8);
            this.xsFacets.pattern = this.fPattern.toString();
            this.xsFacets.patternAnnotations = patternAnnotations;
        }
        this.fPattern.setLength(0);
        return new FacetInfo(this.xsFacets, content2, facetsPresent2, facetsFixed3);
    }

    private boolean containsQName(XSSimpleType type) {
        if (type.getVariety() == 1) {
            short primitive = type.getPrimitiveKind();
            return primitive == 18 || primitive == 20;
        }
        if (type.getVariety() == 2) {
            return containsQName((XSSimpleType) type.getItemType());
        }
        if (type.getVariety() == 3) {
            XSObjectList members = type.getMemberTypes();
            for (int i = 0; i < members.getLength(); i++) {
                if (containsQName((XSSimpleType) members.item(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    Element traverseAttrsAndAttrGrps(Element firstAttr, XSAttributeGroupDecl attrGrp, XSDocumentInfo schemaDoc, SchemaGrammar grammar, XSComplexTypeDecl enclosingCT) {
        XSAttributeUseImpl tempAttrUse;
        String childName;
        XSAttributeGroupDecl tempAttrGrp;
        String name;
        XSAttributeUseImpl tempAttrUse2 = null;
        Element child = firstAttr;
        while (true) {
            int i = 1;
            if (child == null) {
                break;
            }
            String childName2 = DOMUtil.getLocalName(child);
            if (!childName2.equals(SchemaSymbols.ELT_ATTRIBUTE)) {
                if (!childName2.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                    break;
                }
                XSAttributeGroupDecl tempAttrGrp2 = this.fSchemaHandler.fAttributeGroupTraverser.traverseLocal(child, schemaDoc, grammar);
                if (tempAttrGrp2 != null) {
                    XSObjectList attrUseS = tempAttrGrp2.getAttributeUses();
                    int attrCount = attrUseS.getLength();
                    int i2 = 0;
                    while (i2 < attrCount) {
                        XSAttributeUseImpl oneAttrUse = (XSAttributeUseImpl) attrUseS.item(i2);
                        if (oneAttrUse.fUse == 2) {
                            attrGrp.addAttributeUse(oneAttrUse);
                            tempAttrUse = tempAttrUse2;
                            childName = childName2;
                            tempAttrGrp = tempAttrGrp2;
                        } else {
                            XSAttributeUse otherUse = attrGrp.getAttributeUseNoProhibited(oneAttrUse.fAttrDecl.getNamespace(), oneAttrUse.fAttrDecl.getName());
                            if (otherUse == null) {
                                String idName = attrGrp.addAttributeUse(oneAttrUse);
                                if (idName != null) {
                                    String code = enclosingCT == null ? "ag-props-correct.3" : "ct-props-correct.5";
                                    if (enclosingCT == null) {
                                        tempAttrUse = tempAttrUse2;
                                        name = attrGrp.fName;
                                    } else {
                                        tempAttrUse = tempAttrUse2;
                                        name = enclosingCT.getName();
                                    }
                                    childName = childName2;
                                    tempAttrGrp = tempAttrGrp2;
                                    reportSchemaError(code, new Object[]{name, oneAttrUse.fAttrDecl.getName(), idName}, child);
                                } else {
                                    tempAttrUse = tempAttrUse2;
                                    childName = childName2;
                                    tempAttrGrp = tempAttrGrp2;
                                }
                            } else {
                                tempAttrUse = tempAttrUse2;
                                childName = childName2;
                                tempAttrGrp = tempAttrGrp2;
                                if (oneAttrUse != otherUse) {
                                    String code2 = enclosingCT == null ? "ag-props-correct.2" : "ct-props-correct.4";
                                    String name2 = enclosingCT == null ? attrGrp.fName : enclosingCT.getName();
                                    reportSchemaError(code2, new Object[]{name2, oneAttrUse.fAttrDecl.getName()}, child);
                                }
                            }
                            i2++;
                            tempAttrUse2 = tempAttrUse;
                            childName2 = childName;
                            tempAttrGrp2 = tempAttrGrp;
                            i = 1;
                        }
                        i2++;
                        tempAttrUse2 = tempAttrUse;
                        childName2 = childName;
                        tempAttrGrp2 = tempAttrGrp;
                        i = 1;
                    }
                    if (tempAttrGrp2.fAttributeWC != null) {
                        if (attrGrp.fAttributeWC == null) {
                            attrGrp.fAttributeWC = tempAttrGrp2.fAttributeWC;
                        } else {
                            attrGrp.fAttributeWC = attrGrp.fAttributeWC.performIntersectionWith(tempAttrGrp2.fAttributeWC, attrGrp.fAttributeWC.fProcessContents);
                            if (attrGrp.fAttributeWC == null) {
                                String code3 = enclosingCT == null ? "src-attribute_group.2" : "src-ct.4";
                                String name3 = enclosingCT == null ? attrGrp.fName : enclosingCT.getName();
                                Object[] objArr = new Object[i];
                                objArr[0] = name3;
                                reportSchemaError(code3, objArr, child);
                            }
                        }
                    }
                }
            } else {
                tempAttrUse2 = this.fSchemaHandler.fAttributeTraverser.traverseLocal(child, schemaDoc, grammar, enclosingCT);
                if (tempAttrUse2 != null) {
                    if (tempAttrUse2.fUse == 2) {
                        attrGrp.addAttributeUse(tempAttrUse2);
                    } else {
                        XSAttributeUse otherUse2 = attrGrp.getAttributeUseNoProhibited(tempAttrUse2.fAttrDecl.getNamespace(), tempAttrUse2.fAttrDecl.getName());
                        if (otherUse2 == null) {
                            String idName2 = attrGrp.addAttributeUse(tempAttrUse2);
                            if (idName2 != null) {
                                String code4 = enclosingCT == null ? "ag-props-correct.3" : "ct-props-correct.5";
                                String name4 = enclosingCT == null ? attrGrp.fName : enclosingCT.getName();
                                reportSchemaError(code4, new Object[]{name4, tempAttrUse2.fAttrDecl.getName(), idName2}, child);
                            }
                        } else if (otherUse2 != tempAttrUse2) {
                            String code5 = enclosingCT == null ? "ag-props-correct.2" : "ct-props-correct.4";
                            String name5 = enclosingCT == null ? attrGrp.fName : enclosingCT.getName();
                            reportSchemaError(code5, new Object[]{name5, tempAttrUse2.fAttrDecl.getName()}, child);
                        }
                    }
                }
            }
            child = DOMUtil.getNextSiblingElement(child);
        }
        if (child == null || !DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANYATTRIBUTE)) {
            return child;
        }
        XSWildcardDecl tempAttrWC = this.fSchemaHandler.fWildCardTraverser.traverseAnyAttribute(child, schemaDoc, grammar);
        if (attrGrp.fAttributeWC == null) {
            attrGrp.fAttributeWC = tempAttrWC;
        } else {
            attrGrp.fAttributeWC = tempAttrWC.performIntersectionWith(attrGrp.fAttributeWC, tempAttrWC.fProcessContents);
            if (attrGrp.fAttributeWC == null) {
                String code6 = enclosingCT == null ? "src-attribute_group.2" : "src-ct.4";
                String name6 = enclosingCT == null ? attrGrp.fName : enclosingCT.getName();
                reportSchemaError(code6, new Object[]{name6}, child);
            }
        }
        return DOMUtil.getNextSiblingElement(child);
    }

    void reportSchemaError(String key, Object[] args, Element ele) {
        this.fSchemaHandler.reportSchemaError(key, args, ele);
    }

    void checkNotationType(String refName, XSTypeDefinition typeDecl, Element elem) {
        if (typeDecl.getTypeCategory() == 16 && ((XSSimpleType) typeDecl).getVariety() == 1 && ((XSSimpleType) typeDecl).getPrimitiveKind() == 20 && (((XSSimpleType) typeDecl).getDefinedFacets() & XSSimpleTypeDefinition.FACET_ENUMERATION) == 0) {
            reportSchemaError("enumeration-required-notation", new Object[]{typeDecl.getName(), refName, DOMUtil.getLocalName(elem)}, elem);
        }
    }

    protected XSParticleDecl checkOccurrences(XSParticleDecl particle, String particleName, Element parent, int allContextFlags, long defaultVals) {
        int min = particle.fMinOccurs;
        int max = particle.fMaxOccurs;
        boolean defaultMin = (defaultVals & ((long) (1 << XSAttributeChecker.ATTIDX_MINOCCURS))) != 0;
        boolean defaultMax = (defaultVals & ((long) (1 << XSAttributeChecker.ATTIDX_MAXOCCURS))) != 0;
        boolean processingAllEl = (allContextFlags & 1) != 0;
        boolean processingAllGP = (allContextFlags & 8) != 0;
        boolean groupRefWithAll = (allContextFlags & 2) != 0;
        boolean isGroupChild = (allContextFlags & 4) != 0;
        if (isGroupChild) {
            if (!defaultMin) {
                Object[] args = {particleName, "minOccurs"};
                reportSchemaError("s4s-att-not-allowed", args, parent);
                min = 1;
            }
            if (!defaultMax) {
                Object[] args2 = {particleName, "maxOccurs"};
                reportSchemaError("s4s-att-not-allowed", args2, parent);
                max = 1;
            }
        }
        if (min == 0 && max == 0) {
            particle.fType = (short) 0;
            return null;
        }
        if (processingAllEl) {
            if (max != 1) {
                Object[] objArr = new Object[2];
                objArr[0] = max == -1 ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max);
                objArr[1] = ((XSElementDecl) particle.fValue).getName();
                reportSchemaError("cos-all-limited.2", objArr, parent);
                max = 1;
                if (min > 1) {
                    min = 1;
                }
            }
        } else if ((processingAllGP || groupRefWithAll) && max != 1) {
            reportSchemaError("cos-all-limited.1.2", null, parent);
            if (min > 1) {
                min = 1;
            }
            max = 1;
        }
        particle.fMinOccurs = min;
        particle.fMaxOccurs = max;
        return particle;
    }

    private static String processAttValue(String original) {
        int length = original.length();
        for (int i = 0; i < length; i++) {
            char currChar = original.charAt(i);
            if (currChar == '\"' || currChar == '<' || currChar == '&' || currChar == '\t' || currChar == '\n' || currChar == '\r') {
                return escapeAttValue(original, i);
            }
        }
        return original;
    }

    private static String escapeAttValue(String original, int from) {
        int length = original.length();
        StringBuffer newVal = new StringBuffer(length);
        newVal.append(original.substring(0, from));
        for (int i = from; i < length; i++) {
            char currChar = original.charAt(i);
            if (currChar == '\"') {
                newVal.append("&quot;");
            } else if (currChar == '<') {
                newVal.append("&lt;");
            } else if (currChar == '&') {
                newVal.append("&amp;");
            } else if (currChar == '\t') {
                newVal.append("&#x9;");
            } else if (currChar == '\n') {
                newVal.append("&#xA;");
            } else if (currChar == '\r') {
                newVal.append("&#xD;");
            } else {
                newVal.append(currChar);
            }
        }
        return newVal.toString();
    }
}
