package org.apache.xalan.templates;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLTErrorResources;
import org.apache.xml.dtm.DTM;
import org.apache.xml.utils.QName;
import org.apache.xpath.Expression;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.PsuedoNames;
import org.apache.xpath.patterns.NodeTest;
import org.apache.xpath.patterns.StepPattern;
import org.apache.xpath.patterns.UnionPattern;

public class TemplateList implements Serializable {
    static final boolean DEBUG = false;
    static final long serialVersionUID = 5803675288911728791L;
    private Hashtable m_namedTemplates = new Hashtable(89);
    private Hashtable m_patternTable = new Hashtable(89);
    private TemplateSubPatternAssociation m_wildCardPatterns = null;
    private TemplateSubPatternAssociation m_textPatterns = null;
    private TemplateSubPatternAssociation m_docPatterns = null;
    private TemplateSubPatternAssociation m_commentPatterns = null;

    public void setTemplate(ElemTemplate elemTemplate) {
        int importCountComposed;
        int importCountComposed2;
        XPath match = elemTemplate.getMatch();
        if (elemTemplate.getName() == null && match == null) {
            elemTemplate.error(XSLTErrorResources.ER_NEED_NAME_OR_MATCH_ATTRIB, new Object[]{"xsl:template"});
        }
        if (elemTemplate.getName() != null) {
            ElemTemplate elemTemplate2 = (ElemTemplate) this.m_namedTemplates.get(elemTemplate.getName());
            if (elemTemplate2 == null || (importCountComposed2 = elemTemplate.getStylesheetComposed().getImportCountComposed()) > (importCountComposed = elemTemplate2.getStylesheetComposed().getImportCountComposed())) {
                this.m_namedTemplates.put(elemTemplate.getName(), elemTemplate);
            } else if (importCountComposed2 == importCountComposed) {
                elemTemplate.error(XSLTErrorResources.ER_DUPLICATE_NAMED_TEMPLATE, new Object[]{elemTemplate.getName()});
            }
        }
        if (match != null) {
            Expression expression = match.getExpression();
            if (expression instanceof StepPattern) {
                insertPatternInTable((StepPattern) expression, elemTemplate);
                return;
            }
            if (expression instanceof UnionPattern) {
                for (StepPattern stepPattern : ((UnionPattern) expression).getPatterns()) {
                    insertPatternInTable(stepPattern, elemTemplate);
                }
            }
        }
    }

    void dumpAssociationTables() {
        Enumeration enumerationElements = this.m_patternTable.elements();
        while (enumerationElements.hasMoreElements()) {
            for (TemplateSubPatternAssociation next = (TemplateSubPatternAssociation) enumerationElements.nextElement(); next != null; next = next.getNext()) {
                System.out.print("(" + next.getTargetString() + ", " + next.getPattern() + ")");
            }
            System.out.println("\n.....");
        }
        System.out.print("wild card list: ");
        for (TemplateSubPatternAssociation next2 = this.m_wildCardPatterns; next2 != null; next2 = next2.getNext()) {
            System.out.print("(" + next2.getTargetString() + ", " + next2.getPattern() + ")");
        }
        System.out.println("\n.....");
    }

    public void compose(StylesheetRoot stylesheetRoot) {
        if (this.m_wildCardPatterns != null) {
            Enumeration enumerationElements = this.m_patternTable.elements();
            while (enumerationElements.hasMoreElements()) {
                TemplateSubPatternAssociation templateSubPatternAssociationInsertAssociationIntoList = (TemplateSubPatternAssociation) enumerationElements.nextElement();
                for (TemplateSubPatternAssociation next = this.m_wildCardPatterns; next != null; next = next.getNext()) {
                    try {
                        templateSubPatternAssociationInsertAssociationIntoList = insertAssociationIntoList(templateSubPatternAssociationInsertAssociationIntoList, (TemplateSubPatternAssociation) next.clone(), true);
                    } catch (CloneNotSupportedException e) {
                    }
                }
            }
        }
    }

    private TemplateSubPatternAssociation insertAssociationIntoList(TemplateSubPatternAssociation templateSubPatternAssociation, TemplateSubPatternAssociation templateSubPatternAssociation2, boolean z) {
        TemplateSubPatternAssociation next;
        double priorityOrScore = getPriorityOrScore(templateSubPatternAssociation2);
        int importLevel = templateSubPatternAssociation2.getImportLevel();
        int docOrderPos = templateSubPatternAssociation2.getDocOrderPos();
        TemplateSubPatternAssociation templateSubPatternAssociation3 = templateSubPatternAssociation;
        while (true) {
            next = templateSubPatternAssociation3.getNext();
            if (next != null) {
                double priorityOrScore2 = getPriorityOrScore(next);
                if (importLevel > next.getImportLevel() || (importLevel >= next.getImportLevel() && (priorityOrScore > priorityOrScore2 || (priorityOrScore >= priorityOrScore2 && docOrderPos >= next.getDocOrderPos())))) {
                    break;
                }
                templateSubPatternAssociation3 = next;
            } else {
                break;
            }
        }
        boolean z2 = false;
        if (next == null || templateSubPatternAssociation3 == templateSubPatternAssociation) {
            double priorityOrScore3 = getPriorityOrScore(templateSubPatternAssociation3);
            if (importLevel > templateSubPatternAssociation3.getImportLevel() || (importLevel >= templateSubPatternAssociation3.getImportLevel() && (priorityOrScore > priorityOrScore3 || (priorityOrScore >= priorityOrScore3 && docOrderPos >= templateSubPatternAssociation3.getDocOrderPos())))) {
                z2 = true;
            }
        }
        if (z) {
            if (z2) {
                templateSubPatternAssociation2.setNext(templateSubPatternAssociation3);
                String targetString = templateSubPatternAssociation3.getTargetString();
                templateSubPatternAssociation2.setTargetString(targetString);
                putHead(targetString, templateSubPatternAssociation2);
                return templateSubPatternAssociation2;
            }
            templateSubPatternAssociation2.setNext(next);
            templateSubPatternAssociation3.setNext(templateSubPatternAssociation2);
            return templateSubPatternAssociation;
        }
        if (z2) {
            templateSubPatternAssociation2.setNext(templateSubPatternAssociation3);
            if (templateSubPatternAssociation3.isWild() || templateSubPatternAssociation2.isWild()) {
                this.m_wildCardPatterns = templateSubPatternAssociation2;
            } else {
                putHead(templateSubPatternAssociation2.getTargetString(), templateSubPatternAssociation2);
            }
            return templateSubPatternAssociation2;
        }
        templateSubPatternAssociation2.setNext(next);
        templateSubPatternAssociation3.setNext(templateSubPatternAssociation2);
        return templateSubPatternAssociation;
    }

    private void insertPatternInTable(StepPattern stepPattern, ElemTemplate elemTemplate) {
        TemplateSubPatternAssociation head;
        String targetString = stepPattern.getTargetString();
        if (targetString != null) {
            TemplateSubPatternAssociation templateSubPatternAssociation = new TemplateSubPatternAssociation(elemTemplate, stepPattern, elemTemplate.getMatch().getPatternString());
            boolean zIsWild = templateSubPatternAssociation.isWild();
            if (zIsWild) {
                head = this.m_wildCardPatterns;
            } else {
                head = getHead(targetString);
            }
            if (head == null) {
                if (zIsWild) {
                    this.m_wildCardPatterns = templateSubPatternAssociation;
                    return;
                } else {
                    putHead(targetString, templateSubPatternAssociation);
                    return;
                }
            }
            insertAssociationIntoList(head, templateSubPatternAssociation, false);
        }
    }

    private double getPriorityOrScore(TemplateSubPatternAssociation templateSubPatternAssociation) {
        double priority = templateSubPatternAssociation.getTemplate().getPriority();
        if (priority == Double.NEGATIVE_INFINITY) {
            StepPattern stepPattern = templateSubPatternAssociation.getStepPattern();
            if (stepPattern instanceof NodeTest) {
                return stepPattern.getDefaultScore();
            }
        }
        return priority;
    }

    public ElemTemplate getTemplate(QName qName) {
        return (ElemTemplate) this.m_namedTemplates.get(qName);
    }

    public TemplateSubPatternAssociation getHead(XPathContext xPathContext, int i, DTM dtm) {
        TemplateSubPatternAssociation templateSubPatternAssociation;
        switch (dtm.getNodeType(i)) {
            case 1:
            case 2:
                templateSubPatternAssociation = (TemplateSubPatternAssociation) this.m_patternTable.get(dtm.getLocalName(i));
                break;
            case 3:
            case 4:
                templateSubPatternAssociation = this.m_textPatterns;
                break;
            case 5:
            case 6:
                templateSubPatternAssociation = (TemplateSubPatternAssociation) this.m_patternTable.get(dtm.getNodeName(i));
                break;
            case 7:
                templateSubPatternAssociation = (TemplateSubPatternAssociation) this.m_patternTable.get(dtm.getLocalName(i));
                break;
            case 8:
                templateSubPatternAssociation = this.m_commentPatterns;
                break;
            case 9:
            case 11:
                templateSubPatternAssociation = this.m_docPatterns;
                break;
            case 10:
            default:
                templateSubPatternAssociation = (TemplateSubPatternAssociation) this.m_patternTable.get(dtm.getNodeName(i));
                break;
        }
        return templateSubPatternAssociation == null ? this.m_wildCardPatterns : templateSubPatternAssociation;
    }

    public ElemTemplate getTemplateFast(XPathContext xPathContext, int i, int i2, QName qName, int i3, boolean z, DTM dtm) throws TransformerException {
        TemplateSubPatternAssociation next;
        switch (dtm.getNodeType(i)) {
            case 1:
            case 2:
                next = (TemplateSubPatternAssociation) this.m_patternTable.get(dtm.getLocalNameFromExpandedNameID(i2));
                break;
            case 3:
            case 4:
                next = this.m_textPatterns;
                break;
            case 5:
            case 6:
                next = (TemplateSubPatternAssociation) this.m_patternTable.get(dtm.getNodeName(i));
                break;
            case 7:
                next = (TemplateSubPatternAssociation) this.m_patternTable.get(dtm.getLocalName(i));
                break;
            case 8:
                next = this.m_commentPatterns;
                break;
            case 9:
            case 11:
                next = this.m_docPatterns;
                break;
            case 10:
            default:
                next = (TemplateSubPatternAssociation) this.m_patternTable.get(dtm.getNodeName(i));
                break;
        }
        if (next == null && (next = this.m_wildCardPatterns) == null) {
            return null;
        }
        xPathContext.pushNamespaceContextNull();
        do {
            if (i3 > -1) {
                try {
                    if (next.getImportLevel() <= i3) {
                        ElemTemplate template = next.getTemplate();
                        xPathContext.setNamespaceContext(template);
                        if (next.m_stepPattern.execute(xPathContext, i, dtm, i2) != NodeTest.SCORE_NONE && next.matchMode(qName)) {
                            if (z) {
                                checkConflicts(next, xPathContext, i, qName);
                            }
                            xPathContext.popNamespaceContext();
                            return template;
                        }
                    }
                } catch (Throwable th) {
                    xPathContext.popNamespaceContext();
                    throw th;
                }
            }
            next = next.getNext();
        } while (next != null);
        xPathContext.popNamespaceContext();
        return null;
    }

    public ElemTemplate getTemplate(XPathContext xPathContext, int i, QName qName, boolean z, DTM dtm) throws TransformerException {
        TemplateSubPatternAssociation head = getHead(xPathContext, i, dtm);
        if (head != null) {
            xPathContext.pushNamespaceContextNull();
            xPathContext.pushCurrentNodeAndExpression(i, i);
            do {
                try {
                    ElemTemplate template = head.getTemplate();
                    xPathContext.setNamespaceContext(template);
                    if (head.m_stepPattern.execute(xPathContext, i) != NodeTest.SCORE_NONE && head.matchMode(qName)) {
                        if (z) {
                            checkConflicts(head, xPathContext, i, qName);
                        }
                        return template;
                    }
                    head = head.getNext();
                } finally {
                    xPathContext.popCurrentNodeAndExpression();
                    xPathContext.popNamespaceContext();
                }
            } while (head != null);
            return null;
        }
        return null;
    }

    public ElemTemplate getTemplate(XPathContext xPathContext, int i, QName qName, int i2, int i3, boolean z, DTM dtm) throws TransformerException {
        TemplateSubPatternAssociation head = getHead(xPathContext, i, dtm);
        if (head != null) {
            xPathContext.pushNamespaceContextNull();
            xPathContext.pushCurrentNodeAndExpression(i, i);
            do {
                if (i2 > -1) {
                    try {
                        if (head.getImportLevel() <= i2) {
                            if (head.getImportLevel() <= i2 - i3) {
                                xPathContext.popCurrentNodeAndExpression();
                                xPathContext.popNamespaceContext();
                                return null;
                            }
                            ElemTemplate template = head.getTemplate();
                            xPathContext.setNamespaceContext(template);
                            if (head.m_stepPattern.execute(xPathContext, i) != NodeTest.SCORE_NONE && head.matchMode(qName)) {
                                if (z) {
                                    checkConflicts(head, xPathContext, i, qName);
                                }
                                xPathContext.popCurrentNodeAndExpression();
                                xPathContext.popNamespaceContext();
                                return template;
                            }
                        }
                        head = head.getNext();
                    } catch (Throwable th) {
                        xPathContext.popCurrentNodeAndExpression();
                        xPathContext.popNamespaceContext();
                        throw th;
                    }
                }
            } while (head != null);
            xPathContext.popCurrentNodeAndExpression();
            xPathContext.popNamespaceContext();
        }
        return null;
    }

    public TemplateWalker getWalker() {
        return new TemplateWalker();
    }

    private void checkConflicts(TemplateSubPatternAssociation templateSubPatternAssociation, XPathContext xPathContext, int i, QName qName) {
    }

    private void addObjectIfNotFound(Object obj, Vector vector) {
        int size = vector.size();
        boolean z = false;
        int i = 0;
        while (true) {
            if (i < size) {
                if (vector.elementAt(i) == obj) {
                    break;
                } else {
                    i++;
                }
            } else {
                z = true;
                break;
            }
        }
        if (z) {
            vector.addElement(obj);
        }
    }

    private Hashtable getNamedTemplates() {
        return this.m_namedTemplates;
    }

    private void setNamedTemplates(Hashtable hashtable) {
        this.m_namedTemplates = hashtable;
    }

    private TemplateSubPatternAssociation getHead(String str) {
        return (TemplateSubPatternAssociation) this.m_patternTable.get(str);
    }

    private void putHead(String str, TemplateSubPatternAssociation templateSubPatternAssociation) {
        if (str.equals(PsuedoNames.PSEUDONAME_TEXT)) {
            this.m_textPatterns = templateSubPatternAssociation;
        } else if (str.equals(PsuedoNames.PSEUDONAME_ROOT)) {
            this.m_docPatterns = templateSubPatternAssociation;
        } else if (str.equals(PsuedoNames.PSEUDONAME_COMMENT)) {
            this.m_commentPatterns = templateSubPatternAssociation;
        }
        this.m_patternTable.put(str, templateSubPatternAssociation);
    }

    public class TemplateWalker {
        private TemplateSubPatternAssociation curPattern;
        private Enumeration hashIterator;
        private boolean inPatterns;
        private Hashtable m_compilerCache;

        private TemplateWalker() {
            this.m_compilerCache = new Hashtable();
            this.hashIterator = TemplateList.this.m_patternTable.elements();
            this.inPatterns = true;
            this.curPattern = null;
        }

        public ElemTemplate next() {
            ElemTemplate template = null;
            do {
                if (this.inPatterns) {
                    if (this.curPattern != null) {
                        this.curPattern = this.curPattern.getNext();
                    }
                    if (this.curPattern != null) {
                        template = this.curPattern.getTemplate();
                    } else if (this.hashIterator.hasMoreElements()) {
                        this.curPattern = (TemplateSubPatternAssociation) this.hashIterator.nextElement();
                        template = this.curPattern.getTemplate();
                    } else {
                        this.inPatterns = false;
                        this.hashIterator = TemplateList.this.m_namedTemplates.elements();
                    }
                }
                if (!this.inPatterns) {
                    if (!this.hashIterator.hasMoreElements()) {
                        return null;
                    }
                    template = (ElemTemplate) this.hashIterator.nextElement();
                }
            } while (((ElemTemplate) this.m_compilerCache.get(new Integer(template.getUid()))) != null);
            this.m_compilerCache.put(new Integer(template.getUid()), template);
            return template;
        }
    }
}
