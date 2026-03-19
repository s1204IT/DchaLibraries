package mf.org.apache.xerces.impl.xs.traversers;

import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSAnnotationImpl;
import mf.org.apache.xerces.impl.xs.XSNotationDecl;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.w3c.dom.Element;

class XSDNotationTraverser extends XSDAbstractTraverser {
    XSDNotationTraverser(XSDHandler handler, XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    XSNotationDecl traverse(Element elmNode, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        XSNotationDecl notation;
        XSAnnotationImpl annotation;
        XSObjectListImpl xSObjectListImpl;
        Object[] attrValues = this.fAttrChecker.checkAttributes(elmNode, true, schemaDoc);
        String nameAttr = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        String publicAttr = (String) attrValues[XSAttributeChecker.ATTIDX_PUBLIC];
        String systemAttr = (String) attrValues[XSAttributeChecker.ATTIDX_SYSTEM];
        if (nameAttr == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_NOTATION, SchemaSymbols.ATT_NAME}, elmNode);
            this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
            return null;
        }
        if (systemAttr == null && publicAttr == null) {
            reportSchemaError("PublicSystemOnNotation", null, elmNode);
            publicAttr = "missing";
        }
        String publicAttr2 = publicAttr;
        XSNotationDecl notation2 = new XSNotationDecl();
        notation2.fName = nameAttr;
        notation2.fTargetNamespace = schemaDoc.fTargetNamespace;
        notation2.fPublicId = publicAttr2;
        notation2.fSystemId = systemAttr;
        Element content = DOMUtil.getFirstChildElement(elmNode);
        if (content != null && DOMUtil.getLocalName(content).equals(SchemaSymbols.ELT_ANNOTATION)) {
            XSAnnotationImpl annotation2 = traverseAnnotationDecl(content, attrValues, false, schemaDoc);
            content = DOMUtil.getNextSiblingElement(content);
            notation = notation2;
            annotation = annotation2;
        } else {
            String text = DOMUtil.getSyntheticAnnotation(elmNode);
            if (text == null) {
                notation = notation2;
                annotation = null;
            } else {
                notation = notation2;
                XSAnnotationImpl annotation3 = traverseSyntheticAnnotation(elmNode, text, attrValues, false, schemaDoc);
                annotation = annotation3;
                content = content;
            }
        }
        if (annotation != null) {
            xSObjectListImpl = new XSObjectListImpl();
            xSObjectListImpl.addXSObject(annotation);
        } else {
            xSObjectListImpl = XSObjectListImpl.EMPTY_LIST;
        }
        notation.fAnnotations = xSObjectListImpl;
        if (content != null) {
            Object[] args = {SchemaSymbols.ELT_NOTATION, "(annotation?)", DOMUtil.getLocalName(content)};
            reportSchemaError("s4s-elt-must-match.1", args, content);
        }
        if (grammar.getGlobalNotationDecl(notation.fName) == null) {
            grammar.addGlobalNotationDecl(notation);
        }
        String loc = this.fSchemaHandler.schemaDocument2SystemId(schemaDoc);
        XSNotationDecl notation22 = grammar.getGlobalNotationDecl(notation.fName, loc);
        if (notation22 == null) {
            grammar.addGlobalNotationDecl(notation, loc);
        }
        if (this.fSchemaHandler.fTolerateDuplicates) {
            if (notation22 != null) {
                notation = notation22;
            }
            this.fSchemaHandler.addGlobalNotationDecl(notation);
        }
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return notation;
    }
}
