package mf.org.apache.xerces.impl.xs.traversers;

import mf.org.apache.xerces.impl.xpath.XPathException;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.identity.Field;
import mf.org.apache.xerces.impl.xs.identity.IdentityConstraint;
import mf.org.apache.xerces.impl.xs.identity.Selector;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.util.XMLChar;
import mf.org.w3c.dom.Element;

class XSDAbstractIDConstraintTraverser extends XSDAbstractTraverser {
    public XSDAbstractIDConstraintTraverser(XSDHandler handler, XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    boolean traverseIdentityConstraint(IdentityConstraint ic, Element icElem, XSDocumentInfo schemaDoc, Object[] icElemAttrs) {
        Object[] attrValues;
        Object sText;
        Selector.XPath sXpath;
        Object[] attrValues2;
        Selector.XPath sXpath2;
        String sText2;
        Element sElem = DOMUtil.getFirstChildElement(icElem);
        if (sElem == null) {
            reportSchemaError("s4s-elt-must-match.2", new Object[]{"identity constraint", "(annotation?, selector, field+)"}, icElem);
            return false;
        }
        if (DOMUtil.getLocalName(sElem).equals(SchemaSymbols.ELT_ANNOTATION)) {
            ic.addAnnotation(traverseAnnotationDecl(sElem, icElemAttrs, false, schemaDoc));
            sElem = DOMUtil.getNextSiblingElement(sElem);
            if (sElem == null) {
                reportSchemaError("s4s-elt-must-match.2", new Object[]{"identity constraint", "(annotation?, selector, field+)"}, icElem);
                return false;
            }
        } else {
            String text = DOMUtil.getSyntheticAnnotation(icElem);
            if (text != null) {
                ic.addAnnotation(traverseSyntheticAnnotation(icElem, text, icElemAttrs, false, schemaDoc));
            }
        }
        Element sElem2 = sElem;
        if (!DOMUtil.getLocalName(sElem2).equals(SchemaSymbols.ELT_SELECTOR)) {
            reportSchemaError("s4s-elt-must-match.1", new Object[]{"identity constraint", "(annotation?, selector, field+)", SchemaSymbols.ELT_SELECTOR}, sElem2);
            return false;
        }
        Object[] attrValues3 = this.fAttrChecker.checkAttributes(sElem2, false, schemaDoc);
        Element selChild = DOMUtil.getFirstChildElement(sElem2);
        if (selChild != null) {
            if (DOMUtil.getLocalName(selChild).equals(SchemaSymbols.ELT_ANNOTATION)) {
                ic.addAnnotation(traverseAnnotationDecl(selChild, attrValues3, false, schemaDoc));
                selChild = DOMUtil.getNextSiblingElement(selChild);
            } else {
                reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_SELECTOR, "(annotation?)", DOMUtil.getLocalName(selChild)}, selChild);
            }
            if (selChild != null) {
                reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_SELECTOR, "(annotation?)", DOMUtil.getLocalName(selChild)}, selChild);
            }
            attrValues = attrValues3;
        } else {
            String text2 = DOMUtil.getSyntheticAnnotation(sElem2);
            if (text2 != null) {
                attrValues = attrValues3;
                ic.addAnnotation(traverseSyntheticAnnotation(icElem, text2, attrValues3, false, schemaDoc));
            } else {
                attrValues = attrValues3;
            }
        }
        String sText3 = (String) attrValues[XSAttributeChecker.ATTIDX_XPATH];
        if (sText3 == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_SELECTOR, SchemaSymbols.ATT_XPATH}, sElem2);
            return false;
        }
        String sText4 = XMLChar.trim(sText3);
        try {
            sXpath = new Selector.XPath(sText4, this.fSymbolTable, schemaDoc.fNamespaceSupport);
        } catch (XPathException e) {
            e = e;
            sText = sText4;
        }
        try {
            Selector selector = new Selector(sXpath, ic);
            ic.setSelector(selector);
            this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
            Element fElem = DOMUtil.getNextSiblingElement(sElem2);
            if (fElem == null) {
                reportSchemaError("s4s-elt-must-match.2", new Object[]{"identity constraint", "(annotation?, selector, field+)"}, sElem2);
                return false;
            }
            boolean z = true;
            Element fElem2 = fElem;
            while (fElem2 != null) {
                if (!DOMUtil.getLocalName(fElem2).equals(SchemaSymbols.ELT_FIELD)) {
                    reportSchemaError("s4s-elt-must-match.1", new Object[]{"identity constraint", "(annotation?, selector, field+)", SchemaSymbols.ELT_FIELD}, fElem2);
                    fElem2 = DOMUtil.getNextSiblingElement(fElem2);
                } else {
                    Object[] attrValues4 = this.fAttrChecker.checkAttributes(fElem2, false, schemaDoc);
                    Element fieldChild = DOMUtil.getFirstChildElement(fElem2);
                    if (fieldChild != null && DOMUtil.getLocalName(fieldChild).equals(SchemaSymbols.ELT_ANNOTATION)) {
                        ic.addAnnotation(traverseAnnotationDecl(fieldChild, attrValues4, false, schemaDoc));
                        fieldChild = DOMUtil.getNextSiblingElement(fieldChild);
                    }
                    Element fieldChild2 = fieldChild;
                    if (fieldChild2 != null) {
                        reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_FIELD, "(annotation?)", DOMUtil.getLocalName(fieldChild2)}, fieldChild2);
                        attrValues2 = attrValues4;
                        sXpath2 = sXpath;
                        sText2 = sText4;
                    } else {
                        String text3 = DOMUtil.getSyntheticAnnotation(fElem2);
                        if (text3 != null) {
                            attrValues2 = attrValues4;
                            sXpath2 = sXpath;
                            sText2 = sText4;
                            ic.addAnnotation(traverseSyntheticAnnotation(icElem, text3, attrValues2, false, schemaDoc));
                        } else {
                            attrValues2 = attrValues4;
                            sXpath2 = sXpath;
                            sText2 = sText4;
                        }
                    }
                    Object[] attrValues5 = attrValues2;
                    String fText = (String) attrValues5[XSAttributeChecker.ATTIDX_XPATH];
                    if (fText == null) {
                        reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_FIELD, SchemaSymbols.ATT_XPATH}, fElem2);
                        this.fAttrChecker.returnAttrArray(attrValues5, schemaDoc);
                        return false;
                    }
                    String fText2 = XMLChar.trim(fText);
                    try {
                        Field.XPath fXpath = new Field.XPath(fText2, this.fSymbolTable, schemaDoc.fNamespaceSupport);
                        Field field = new Field(fXpath, ic);
                        ic.addField(field);
                        fElem2 = DOMUtil.getNextSiblingElement(fElem2);
                        this.fAttrChecker.returnAttrArray(attrValues5, schemaDoc);
                        sText4 = sText2;
                        sXpath = sXpath2;
                    } catch (XPathException e2) {
                        reportSchemaError(e2.getKey(), new Object[]{fText2}, fElem2);
                        this.fAttrChecker.returnAttrArray(attrValues5, schemaDoc);
                        return false;
                    }
                }
                z = true;
            }
            if (ic.getFieldCount() > 0) {
                return z;
            }
            return false;
        } catch (XPathException e3) {
            e = e3;
            sText = sText4;
            reportSchemaError(e.getKey(), new Object[]{sText}, sElem2);
            this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
            return false;
        }
    }
}
