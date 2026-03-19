package mf.org.apache.xerces.impl.xs.traversers;

import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;
import mf.org.apache.xerces.impl.xs.XSAnnotationImpl;
import mf.org.apache.xerces.impl.xs.XSModelGroupImpl;
import mf.org.apache.xerces.impl.xs.XSParticleDecl;
import mf.org.apache.xerces.impl.xs.util.XInt;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.DOMUtil;
import mf.org.apache.xerces.xs.XSObject;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.w3c.dom.Element;

abstract class XSDAbstractParticleTraverser extends XSDAbstractTraverser {
    ParticleArray fPArray;

    XSDAbstractParticleTraverser(XSDHandler handler, XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
        this.fPArray = new ParticleArray();
    }

    XSParticleDecl traverseAll(Element allDecl, XSDocumentInfo schemaDoc, SchemaGrammar grammar, int allContextFlags, XSObject parent) {
        XSObjectListImpl xSObjectListImpl;
        Object[] attrValues = this.fAttrChecker.checkAttributes(allDecl, false, schemaDoc);
        Element child = DOMUtil.getFirstChildElement(allDecl);
        XSAnnotationImpl annotation = null;
        if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
            annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            child = DOMUtil.getNextSiblingElement(child);
        } else {
            String text = DOMUtil.getSyntheticAnnotation(allDecl);
            if (text != null) {
                annotation = traverseSyntheticAnnotation(allDecl, text, attrValues, false, schemaDoc);
            }
        }
        this.fPArray.pushContext();
        while (child != null) {
            XSParticleDecl particle = null;
            String childName = DOMUtil.getLocalName(child);
            if (childName.equals(SchemaSymbols.ELT_ELEMENT)) {
                particle = this.fSchemaHandler.fElementTraverser.traverseLocal(child, schemaDoc, grammar, 1, parent);
            } else {
                Object[] args = {"all", "(annotation?, element*)", DOMUtil.getLocalName(child)};
                reportSchemaError("s4s-elt-must-match.1", args, child);
            }
            if (particle != null) {
                this.fPArray.addParticle(particle);
            }
            child = DOMUtil.getNextSiblingElement(child);
        }
        XInt minAtt = (XInt) attrValues[XSAttributeChecker.ATTIDX_MINOCCURS];
        XInt maxAtt = (XInt) attrValues[XSAttributeChecker.ATTIDX_MAXOCCURS];
        Long defaultVals = (Long) attrValues[XSAttributeChecker.ATTIDX_FROMDEFAULT];
        XSModelGroupImpl group = new XSModelGroupImpl();
        group.fCompositor = (short) 103;
        group.fParticleCount = this.fPArray.getParticleCount();
        group.fParticles = this.fPArray.popContext();
        if (annotation != null) {
            xSObjectListImpl = new XSObjectListImpl();
            xSObjectListImpl.addXSObject(annotation);
        } else {
            xSObjectListImpl = XSObjectListImpl.EMPTY_LIST;
        }
        XSObjectList annotations = xSObjectListImpl;
        group.fAnnotations = annotations;
        XSParticleDecl particle2 = new XSParticleDecl();
        particle2.fType = (short) 3;
        particle2.fMinOccurs = minAtt.intValue();
        particle2.fMaxOccurs = maxAtt.intValue();
        particle2.fValue = group;
        particle2.fAnnotations = annotations;
        XSParticleDecl particle3 = checkOccurrences(particle2, SchemaSymbols.ELT_ALL, (Element) allDecl.getParentNode(), allContextFlags, defaultVals.longValue());
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return particle3;
    }

    XSParticleDecl traverseSequence(Element seqDecl, XSDocumentInfo schemaDoc, SchemaGrammar grammar, int allContextFlags, XSObject parent) {
        return traverseSeqChoice(seqDecl, schemaDoc, grammar, allContextFlags, false, parent);
    }

    XSParticleDecl traverseChoice(Element choiceDecl, XSDocumentInfo schemaDoc, SchemaGrammar grammar, int allContextFlags, XSObject parent) {
        return traverseSeqChoice(choiceDecl, schemaDoc, grammar, allContextFlags, true, parent);
    }

    private XSParticleDecl traverseSeqChoice(Element decl, XSDocumentInfo schemaDoc, SchemaGrammar grammar, int allContextFlags, boolean choice, XSObject parent) {
        XSObjectListImpl xSObjectListImpl;
        Object[] args;
        XSParticleDecl particle;
        Object[] attrValues = this.fAttrChecker.checkAttributes(decl, false, schemaDoc);
        Element child = DOMUtil.getFirstChildElement(decl);
        XSAnnotationImpl annotation = null;
        if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
            annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            child = DOMUtil.getNextSiblingElement(child);
        } else {
            String text = DOMUtil.getSyntheticAnnotation(decl);
            if (text != null) {
                annotation = traverseSyntheticAnnotation(decl, text, attrValues, false, schemaDoc);
            }
        }
        this.fPArray.pushContext();
        while (child != null) {
            XSParticleDecl particle2 = null;
            String childName = DOMUtil.getLocalName(child);
            if (childName.equals(SchemaSymbols.ELT_ELEMENT)) {
                particle2 = this.fSchemaHandler.fElementTraverser.traverseLocal(child, schemaDoc, grammar, 0, parent);
            } else if (childName.equals(SchemaSymbols.ELT_GROUP)) {
                particle = this.fSchemaHandler.fGroupTraverser.traverseLocal(child, schemaDoc, grammar);
                if (hasAllContent(particle)) {
                    particle2 = null;
                    reportSchemaError("cos-all-limited.1.2", null, child);
                }
                if (particle == null) {
                    this.fPArray.addParticle(particle);
                }
                child = DOMUtil.getNextSiblingElement(child);
            } else if (childName.equals(SchemaSymbols.ELT_CHOICE)) {
                particle2 = traverseChoice(child, schemaDoc, grammar, 0, parent);
            } else if (childName.equals(SchemaSymbols.ELT_SEQUENCE)) {
                particle2 = traverseSequence(child, schemaDoc, grammar, 0, parent);
            } else if (childName.equals(SchemaSymbols.ELT_ANY)) {
                particle2 = this.fSchemaHandler.fWildCardTraverser.traverseAny(child, schemaDoc, grammar);
            } else {
                if (choice) {
                    args = new Object[]{"choice", "(annotation?, (element | group | choice | sequence | any)*)", DOMUtil.getLocalName(child)};
                } else {
                    args = new Object[]{"sequence", "(annotation?, (element | group | choice | sequence | any)*)", DOMUtil.getLocalName(child)};
                }
                reportSchemaError("s4s-elt-must-match.1", args, child);
            }
            particle = particle2;
            if (particle == null) {
            }
            child = DOMUtil.getNextSiblingElement(child);
        }
        XInt minAtt = (XInt) attrValues[XSAttributeChecker.ATTIDX_MINOCCURS];
        XInt maxAtt = (XInt) attrValues[XSAttributeChecker.ATTIDX_MAXOCCURS];
        Long defaultVals = (Long) attrValues[XSAttributeChecker.ATTIDX_FROMDEFAULT];
        XSModelGroupImpl group = new XSModelGroupImpl();
        group.fCompositor = choice ? (short) 101 : (short) 102;
        group.fParticleCount = this.fPArray.getParticleCount();
        group.fParticles = this.fPArray.popContext();
        if (annotation != null) {
            xSObjectListImpl = new XSObjectListImpl();
            xSObjectListImpl.addXSObject(annotation);
        } else {
            xSObjectListImpl = XSObjectListImpl.EMPTY_LIST;
        }
        group.fAnnotations = xSObjectListImpl;
        XSParticleDecl particle3 = new XSParticleDecl();
        particle3.fType = (short) 3;
        particle3.fMinOccurs = minAtt.intValue();
        particle3.fMaxOccurs = maxAtt.intValue();
        particle3.fValue = group;
        particle3.fAnnotations = xSObjectListImpl;
        XSParticleDecl particle4 = checkOccurrences(particle3, choice ? SchemaSymbols.ELT_CHOICE : SchemaSymbols.ELT_SEQUENCE, (Element) decl.getParentNode(), allContextFlags, defaultVals.longValue());
        this.fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return particle4;
    }

    protected boolean hasAllContent(XSParticleDecl particle) {
        return particle != null && particle.fType == 3 && ((XSModelGroupImpl) particle.fValue).fCompositor == 103;
    }

    static class ParticleArray {
        XSParticleDecl[] fParticles = new XSParticleDecl[10];
        int[] fPos = new int[5];
        int fContextCount = 0;

        ParticleArray() {
        }

        void pushContext() {
            this.fContextCount++;
            if (this.fContextCount == this.fPos.length) {
                int newSize = this.fContextCount * 2;
                int[] newArray = new int[newSize];
                System.arraycopy(this.fPos, 0, newArray, 0, this.fContextCount);
                this.fPos = newArray;
            }
            this.fPos[this.fContextCount] = this.fPos[this.fContextCount - 1];
        }

        int getParticleCount() {
            return this.fPos[this.fContextCount] - this.fPos[this.fContextCount - 1];
        }

        void addParticle(XSParticleDecl particle) {
            if (this.fPos[this.fContextCount] == this.fParticles.length) {
                int newSize = this.fPos[this.fContextCount] * 2;
                XSParticleDecl[] newArray = new XSParticleDecl[newSize];
                System.arraycopy(this.fParticles, 0, newArray, 0, this.fPos[this.fContextCount]);
                this.fParticles = newArray;
            }
            XSParticleDecl[] xSParticleDeclArr = this.fParticles;
            int[] iArr = this.fPos;
            int i = this.fContextCount;
            int i2 = iArr[i];
            iArr[i] = i2 + 1;
            xSParticleDeclArr[i2] = particle;
        }

        XSParticleDecl[] popContext() {
            int count = this.fPos[this.fContextCount] - this.fPos[this.fContextCount - 1];
            XSParticleDecl[] array = null;
            if (count != 0) {
                XSParticleDecl[] array2 = new XSParticleDecl[count];
                XSParticleDecl[] array3 = this.fParticles;
                System.arraycopy(array3, this.fPos[this.fContextCount - 1], array2, 0, count);
                for (int i = this.fPos[this.fContextCount - 1]; i < this.fPos[this.fContextCount]; i++) {
                    this.fParticles[i] = null;
                }
                array = array2;
            }
            this.fContextCount--;
            return array;
        }
    }
}
