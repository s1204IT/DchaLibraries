package mf.org.apache.xerces.impl.xs.traversers;

import com.mediatek.plugin.builder.PluginDescriptorBuilder;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSAnnotationImpl;
import mf.org.apache.xerces.impl.xs.XSConstraints;
import mf.org.apache.xerces.impl.xs.XSGroupDecl;
import mf.org.apache.xerces.impl.xs.XSModelGroupImpl;
import mf.org.apache.xerces.impl.xs.XSParticleDecl;
import mf.org.apache.xerces.impl.xs.util.XInt;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xni.QName;
import mf.org.w3c.dom.Element;

class XSDGroupTraverser extends XSDAbstractParticleTraverser {
    XSDGroupTraverser(XSDHandler handler, XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    XSParticleDecl traverseLocal(Element elmNode, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        Element child;
        XSGroupDecl group;
        XSAnnotationImpl annotation;
        XSAnnotationImpl annotation2;
        XSGroupDecl group2;
        XSParticleDecl particle;
        XSGroupDecl group3;
        XSAnnotationImpl annotation3;
        XSObjectListImpl xSObjectListImpl;
        Object[] attrValues = this.fAttrChecker.checkAttributes(elmNode, false, schemaDoc);
        QName refAttr = (QName) attrValues[XSAttributeChecker.ATTIDX_REF];
        XInt minAttr = (XInt) attrValues[XSAttributeChecker.ATTIDX_MINOCCURS];
        XInt maxAttr = (XInt) attrValues[XSAttributeChecker.ATTIDX_MAXOCCURS];
        XSGroupDecl group4 = null;
        if (refAttr == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{"group (local)", "ref"}, elmNode);
        } else {
            group4 = (XSGroupDecl) this.fSchemaHandler.getGlobalDecl(schemaDoc, 4, refAttr, elmNode);
        }
        XSGroupDecl group5 = group4;
        Element child2 = DOMUtil.getFirstChildElement(elmNode);
        if (child2 != null && DOMUtil.getLocalName(child2).equals(SchemaSymbols.ELT_ANNOTATION)) {
            annotation2 = traverseAnnotationDecl(child2, attrValues, false, schemaDoc);
            child = DOMUtil.getNextSiblingElement(child2);
            group = group5;
        } else {
            String text = DOMUtil.getSyntheticAnnotation(elmNode);
            if (text != null) {
                child = child2;
                group = group5;
                annotation2 = traverseSyntheticAnnotation(elmNode, text, attrValues, false, schemaDoc);
            } else {
                child = child2;
                group = group5;
                annotation = null;
                if (child != null) {
                    reportSchemaError("s4s-elt-must-match.1", new Object[]{"group (local)", "(annotation?)", DOMUtil.getLocalName(elmNode)}, elmNode);
                }
                int minOccurs = minAttr.intValue();
                int maxOccurs = maxAttr.intValue();
                XSParticleDecl particle2 = null;
                group2 = group;
                if (group2 == null && group2.fModelGroup != null) {
                    if (minOccurs != 0 || maxOccurs != 0) {
                        if (this.fSchemaHandler.fDeclPool != null) {
                            particle = this.fSchemaHandler.fDeclPool.getParticleDecl();
                        } else {
                            particle = new XSParticleDecl();
                        }
                        XSParticleDecl particle3 = particle;
                        particle3.fType = (short) 3;
                        particle3.fValue = group2.fModelGroup;
                        particle3.fMinOccurs = minOccurs;
                        particle3.fMaxOccurs = maxOccurs;
                        if (group2.fModelGroup.fCompositor == 103) {
                            Long defaultVals = (Long) attrValues[XSAttributeChecker.ATTIDX_FROMDEFAULT];
                            group3 = group2;
                            annotation3 = annotation;
                            particle2 = checkOccurrences(particle3, SchemaSymbols.ELT_GROUP, (Element) elmNode.getParentNode(), 2, defaultVals.longValue());
                        } else {
                            group3 = group2;
                            annotation3 = annotation;
                            particle2 = particle3;
                        }
                        if (refAttr != null) {
                            if (annotation3 != null) {
                                xSObjectListImpl = new XSObjectListImpl();
                                xSObjectListImpl.addXSObject(annotation3);
                            } else {
                                xSObjectListImpl = XSObjectListImpl.EMPTY_LIST;
                            }
                            particle2.fAnnotations = xSObjectListImpl;
                        } else {
                            particle2.fAnnotations = group3.fAnnotations;
                        }
                    }
                }
                this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
                return particle2;
            }
        }
        annotation = annotation2;
        if (child != null) {
        }
        int minOccurs2 = minAttr.intValue();
        int maxOccurs2 = maxAttr.intValue();
        XSParticleDecl particle22 = null;
        group2 = group;
        if (group2 == null) {
        }
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return particle22;
    }

    XSGroupDecl traverseGlobal(Element elmNode, XSDocumentInfo schemaDoc, SchemaGrammar grammar) {
        int i;
        Element l_elmChild;
        Element l_elmChild2;
        XSParticleDecl particle;
        Object redefinedGrp;
        XSObjectListImpl xSObjectListImpl;
        Object[] attrValues = this.fAttrChecker.checkAttributes(elmNode, true, schemaDoc);
        String strNameAttr = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        if (strNameAttr == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{"group (global)", PluginDescriptorBuilder.VALUE_NAME}, elmNode);
        }
        XSGroupDecl group = new XSGroupDecl();
        XSParticleDecl particle2 = null;
        Element l_elmChild3 = DOMUtil.getFirstChildElement(elmNode);
        XSAnnotationImpl annotation = null;
        if (l_elmChild3 == null) {
            reportSchemaError("s4s-elt-must-match.2", new Object[]{"group (global)", "(annotation?, (all | choice | sequence))"}, elmNode);
        } else {
            String childName = l_elmChild3.getLocalName();
            if (childName.equals(SchemaSymbols.ELT_ANNOTATION)) {
                XSAnnotationImpl annotation2 = traverseAnnotationDecl(l_elmChild3, attrValues, true, schemaDoc);
                Element l_elmChild4 = DOMUtil.getNextSiblingElement(l_elmChild3);
                if (l_elmChild4 != null) {
                    childName = l_elmChild4.getLocalName();
                }
                annotation = annotation2;
                l_elmChild3 = l_elmChild4;
            } else {
                String text = DOMUtil.getSyntheticAnnotation(elmNode);
                if (text != null) {
                    annotation = traverseSyntheticAnnotation(elmNode, text, attrValues, false, schemaDoc);
                    l_elmChild3 = l_elmChild3;
                    childName = childName;
                }
            }
            if (l_elmChild3 == null) {
                reportSchemaError("s4s-elt-must-match.2", new Object[]{"group (global)", "(annotation?, (all | choice | sequence))"}, elmNode);
                i = 3;
                l_elmChild2 = l_elmChild3;
            } else {
                if (childName.equals(SchemaSymbols.ELT_ALL)) {
                    i = 3;
                    l_elmChild = l_elmChild3;
                    particle = traverseAll(l_elmChild3, schemaDoc, grammar, 4, group);
                } else {
                    i = 3;
                    String childName2 = childName;
                    l_elmChild = l_elmChild3;
                    if (childName2.equals(SchemaSymbols.ELT_CHOICE)) {
                        particle = traverseChoice(l_elmChild, schemaDoc, grammar, 4, group);
                    } else if (childName2.equals(SchemaSymbols.ELT_SEQUENCE)) {
                        particle = traverseSequence(l_elmChild, schemaDoc, grammar, 4, group);
                    } else {
                        l_elmChild2 = l_elmChild;
                        reportSchemaError("s4s-elt-must-match.1", new Object[]{"group (global)", "(annotation?, (all | choice | sequence))", DOMUtil.getLocalName(l_elmChild2)}, l_elmChild2);
                    }
                }
                particle2 = particle;
                l_elmChild2 = l_elmChild;
            }
            if (l_elmChild2 != null && DOMUtil.getNextSiblingElement(l_elmChild2) != null) {
                Object[] objArr = new Object[i];
                objArr[0] = "group (global)";
                objArr[1] = "(annotation?, (all | choice | sequence))";
                objArr[2] = DOMUtil.getLocalName(DOMUtil.getNextSiblingElement(l_elmChild2));
                reportSchemaError("s4s-elt-must-match.1", objArr, DOMUtil.getNextSiblingElement(l_elmChild2));
            }
        }
        XSAnnotationImpl annotation3 = annotation;
        if (strNameAttr != null) {
            group.fName = strNameAttr;
            group.fTargetNamespace = schemaDoc.fTargetNamespace;
            if (particle2 == null) {
                particle2 = XSConstraints.getEmptySequence();
            }
            group.fModelGroup = (XSModelGroupImpl) particle2.fValue;
            if (annotation3 != null) {
                xSObjectListImpl = new XSObjectListImpl();
                xSObjectListImpl.addXSObject(annotation3);
            } else {
                xSObjectListImpl = XSObjectListImpl.EMPTY_LIST;
            }
            group.fAnnotations = xSObjectListImpl;
            if (grammar.getGlobalGroupDecl(group.fName) == null) {
                grammar.addGlobalGroupDecl(group);
            }
            String loc = this.fSchemaHandler.schemaDocument2SystemId(schemaDoc);
            XSGroupDecl group2 = grammar.getGlobalGroupDecl(group.fName, loc);
            if (group2 == null) {
                grammar.addGlobalGroupDecl(group, loc);
            }
            if (this.fSchemaHandler.fTolerateDuplicates) {
                if (group2 != null) {
                    group = group2;
                }
                this.fSchemaHandler.addGlobalGroupDecl(group);
            }
        } else {
            group = null;
        }
        if (group != null && (redefinedGrp = this.fSchemaHandler.getGrpOrAttrGrpRedefinedByRestriction(4, new QName(XMLSymbols.EMPTY_STRING, strNameAttr, strNameAttr, schemaDoc.fTargetNamespace), schemaDoc, elmNode)) != null) {
            grammar.addRedefinedGroupDecl(group, (XSGroupDecl) redefinedGrp, this.fSchemaHandler.element2Locator(elmNode));
        }
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return group;
    }
}
