package mf.org.apache.xerces.impl.xs.traversers;

import com.mediatek.plugin.builder.PluginDescriptorBuilder;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSAnnotationImpl;
import mf.org.apache.xerces.impl.xs.XSAttributeGroupDecl;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.QName;
import mf.org.w3c.dom.Element;

class XSDAttributeGroupTraverser extends XSDAbstractTraverser {
    XSDAttributeGroupTraverser(XSDHandler handler, XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    XSAttributeGroupDecl traverseLocal(Element elmNode, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        Object[] attrValues = this.fAttrChecker.checkAttributes(elmNode, false, schemaDoc);
        QName refAttr = (QName) attrValues[XSAttributeChecker.ATTIDX_REF];
        if (refAttr == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{"attributeGroup (local)", "ref"}, elmNode);
            this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
            return null;
        }
        XSAttributeGroupDecl attrGrp = (XSAttributeGroupDecl) this.fSchemaHandler.getGlobalDecl(schemaDoc, 2, refAttr, elmNode);
        Element child = DOMUtil.getFirstChildElement(elmNode);
        if (child != null) {
            String childName = DOMUtil.getLocalName(child);
            if (childName.equals(SchemaSymbols.ELT_ANNOTATION)) {
                traverseAnnotationDecl(child, attrValues, false, schemaDoc);
                child = DOMUtil.getNextSiblingElement(child);
            } else {
                String text = DOMUtil.getSyntheticAnnotation(child);
                if (text != null) {
                    traverseSyntheticAnnotation(child, text, attrValues, false, schemaDoc);
                }
            }
            if (child != null) {
                Object[] args = {refAttr.rawname, "(annotation?)", DOMUtil.getLocalName(child)};
                reportSchemaError("s4s-elt-must-match.1", args, child);
            }
        }
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return attrGrp;
    }

    XSAttributeGroupDecl traverseGlobal(Element elmNode, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        XSAnnotationImpl annotation;
        XSObjectListImpl xSObjectListImpl;
        Object[] errArgs;
        XSAttributeGroupDecl attrGrp = new XSAttributeGroupDecl();
        Object[] attrValues = this.fAttrChecker.checkAttributes(elmNode, true, schemaDoc);
        String nameAttr = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        if (nameAttr == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{"attributeGroup (global)", PluginDescriptorBuilder.VALUE_NAME}, elmNode);
            nameAttr = "(no name)";
        }
        String nameAttr2 = nameAttr;
        attrGrp.fName = nameAttr2;
        attrGrp.fTargetNamespace = schemaDoc.fTargetNamespace;
        Element child = DOMUtil.getFirstChildElement(elmNode);
        if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
            XSAnnotationImpl annotation2 = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            child = DOMUtil.getNextSiblingElement(child);
            annotation = annotation2;
        } else {
            String text = DOMUtil.getSyntheticAnnotation(elmNode);
            if (text == null) {
                annotation = null;
            } else {
                XSAnnotationImpl annotation3 = traverseSyntheticAnnotation(elmNode, text, attrValues, false, schemaDoc);
                annotation = annotation3;
                child = child;
            }
        }
        XSAnnotationImpl annotation4 = annotation;
        Element child2 = child;
        Element nextNode = traverseAttrsAndAttrGrps(child, attrGrp, schemaDoc, grammar, null);
        if (nextNode != null) {
            Object[] args = {nameAttr2, "(annotation?, ((attribute | attributeGroup)*, anyAttribute?))", DOMUtil.getLocalName(nextNode)};
            reportSchemaError("s4s-elt-must-match.1", args, nextNode);
        }
        if (nameAttr2.equals("(no name)")) {
            this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
            return null;
        }
        attrGrp.removeProhibitedAttrs();
        XSAttributeGroupDecl redefinedAttrGrp = (XSAttributeGroupDecl) this.fSchemaHandler.getGrpOrAttrGrpRedefinedByRestriction(2, new QName(XMLSymbols.EMPTY_STRING, nameAttr2, nameAttr2, schemaDoc.fTargetNamespace), schemaDoc, elmNode);
        if (redefinedAttrGrp != null && (errArgs = attrGrp.validRestrictionOf(nameAttr2, redefinedAttrGrp)) != null) {
            reportSchemaError((String) errArgs[errArgs.length - 1], errArgs, child2);
            reportSchemaError("src-redefine.7.2.2", new Object[]{nameAttr2, errArgs[errArgs.length - 1]}, child2);
        }
        if (annotation4 != null) {
            xSObjectListImpl = new XSObjectListImpl();
            xSObjectListImpl.addXSObject(annotation4);
        } else {
            xSObjectListImpl = XSObjectListImpl.EMPTY_LIST;
        }
        attrGrp.fAnnotations = xSObjectListImpl;
        if (grammar.getGlobalAttributeGroupDecl(attrGrp.fName) == null) {
            grammar.addGlobalAttributeGroupDecl(attrGrp);
        }
        String loc = this.fSchemaHandler.schemaDocument2SystemId(schemaDoc);
        XSAttributeGroupDecl attrGrp2 = grammar.getGlobalAttributeGroupDecl(attrGrp.fName, loc);
        if (attrGrp2 == null) {
            grammar.addGlobalAttributeGroupDecl(attrGrp, loc);
        }
        if (this.fSchemaHandler.fTolerateDuplicates) {
            if (attrGrp2 != null) {
                attrGrp = attrGrp2;
            }
            this.fSchemaHandler.addGlobalAttributeGroupDecl(attrGrp);
        }
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return attrGrp;
    }
}
