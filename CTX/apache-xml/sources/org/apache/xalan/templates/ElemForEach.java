package org.apache.xalan.templates;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.NodeSorter;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.utils.IntStack;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;

public class ElemForEach extends ElemTemplateElement implements ExpressionOwner {
    static final boolean DEBUG = false;
    static final long serialVersionUID = 6018140636363583690L;
    public boolean m_doc_cache_off = false;
    protected Expression m_selectExpression = null;
    protected XPath m_xpath = null;
    protected Vector m_sortElems = null;

    public void setSelect(XPath xPath) {
        this.m_selectExpression = xPath.getExpression();
        this.m_xpath = xPath;
    }

    public Expression getSelect() {
        return this.m_selectExpression;
    }

    @Override
    public void compose(StylesheetRoot stylesheetRoot) throws TransformerException {
        super.compose(stylesheetRoot);
        int sortElemCount = getSortElemCount();
        for (int i = 0; i < sortElemCount; i++) {
            getSortElem(i).compose(stylesheetRoot);
        }
        Vector variableNames = stylesheetRoot.getComposeState().getVariableNames();
        if (this.m_selectExpression != null) {
            this.m_selectExpression.fixupVariables(variableNames, stylesheetRoot.getComposeState().getGlobalsSize());
        } else {
            this.m_selectExpression = getStylesheetRoot().m_selectDefault.getExpression();
        }
    }

    @Override
    public void endCompose(StylesheetRoot stylesheetRoot) throws TransformerException {
        int sortElemCount = getSortElemCount();
        for (int i = 0; i < sortElemCount; i++) {
            getSortElem(i).endCompose(stylesheetRoot);
        }
        super.endCompose(stylesheetRoot);
    }

    public int getSortElemCount() {
        if (this.m_sortElems == null) {
            return 0;
        }
        return this.m_sortElems.size();
    }

    public ElemSort getSortElem(int i) {
        return (ElemSort) this.m_sortElems.elementAt(i);
    }

    public void setSortElem(ElemSort elemSort) {
        if (this.m_sortElems == null) {
            this.m_sortElems = new Vector();
        }
        this.m_sortElems.addElement(elemSort);
    }

    @Override
    public int getXSLToken() {
        return 28;
    }

    @Override
    public String getNodeName() {
        return Constants.ELEMNAME_FOREACH_STRING;
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        transformerImpl.pushCurrentTemplateRuleIsNull(true);
        try {
            transformSelectedNodes(transformerImpl);
        } finally {
            transformerImpl.popCurrentTemplateRuleIsNull();
        }
    }

    protected ElemTemplateElement getTemplateMatch() {
        return this;
    }

    public DTMIterator sortNodes(XPathContext xPathContext, Vector vector, DTMIterator dTMIterator) throws TransformerException {
        NodeSorter nodeSorter = new NodeSorter(xPathContext);
        dTMIterator.setShouldCacheNodes(true);
        dTMIterator.runTo(-1);
        xPathContext.pushContextNodeList(dTMIterator);
        try {
            nodeSorter.sort(dTMIterator, vector, xPathContext);
            dTMIterator.setCurrentPos(0);
            return dTMIterator;
        } finally {
            xPathContext.popContextNodeList();
        }
    }

    public void transformSelectedNodes(TransformerImpl transformerImpl) throws TransformerException {
        Vector vectorProcessSortKeys;
        XPathContext xPathContext = transformerImpl.getXPathContext();
        int currentNode = xPathContext.getCurrentNode();
        DTMIterator dTMIteratorAsIterator = this.m_selectExpression.asIterator(xPathContext, currentNode);
        try {
            if (this.m_sortElems != null) {
                vectorProcessSortKeys = transformerImpl.processSortKeys(this, currentNode);
            } else {
                vectorProcessSortKeys = null;
            }
            if (vectorProcessSortKeys != null) {
                dTMIteratorAsIterator = sortNodes(xPathContext, vectorProcessSortKeys, dTMIteratorAsIterator);
            }
            xPathContext.pushCurrentNode(-1);
            IntStack currentNodeStack = xPathContext.getCurrentNodeStack();
            xPathContext.pushCurrentExpressionNode(-1);
            IntStack currentExpressionNodeStack = xPathContext.getCurrentExpressionNodeStack();
            xPathContext.pushSAXLocatorNull();
            xPathContext.pushContextNodeList(dTMIteratorAsIterator);
            transformerImpl.pushElemTemplateElement(null);
            DTM dtm = xPathContext.getDTM(currentNode);
            int i = currentNode & DTMManager.IDENT_DTM_DEFAULT;
            while (true) {
                int iNextNode = dTMIteratorAsIterator.nextNode();
                if (-1 != iNextNode) {
                    currentNodeStack.setTop(iNextNode);
                    currentExpressionNodeStack.setTop(iNextNode);
                    int i2 = iNextNode & DTMManager.IDENT_DTM_DEFAULT;
                    if (i2 != i) {
                        dtm = xPathContext.getDTM(iNextNode);
                        i = i2;
                    }
                    dtm.getNodeType(iNextNode);
                    for (ElemTemplateElement elemTemplateElement = this.m_firstChild; elemTemplateElement != null; elemTemplateElement = elemTemplateElement.m_nextSibling) {
                        xPathContext.setSAXLocator(elemTemplateElement);
                        transformerImpl.setCurrentElement(elemTemplateElement);
                        elemTemplateElement.execute(transformerImpl);
                    }
                    if (this.m_doc_cache_off) {
                        xPathContext.getSourceTreeManager().removeDocumentFromCache(dtm.getDocument());
                        xPathContext.release(dtm, false);
                    }
                } else {
                    return;
                }
            }
        } finally {
            xPathContext.popSAXLocator();
            xPathContext.popContextNodeList();
            transformerImpl.popElemTemplateElement();
            xPathContext.popCurrentExpressionNode();
            xPathContext.popCurrentNode();
            dTMIteratorAsIterator.detach();
        }
    }

    @Override
    public ElemTemplateElement appendChild(ElemTemplateElement elemTemplateElement) {
        if (64 == elemTemplateElement.getXSLToken()) {
            setSortElem((ElemSort) elemTemplateElement);
            return elemTemplateElement;
        }
        return super.appendChild(elemTemplateElement);
    }

    @Override
    public void callChildVisitors(XSLTVisitor xSLTVisitor, boolean z) {
        if (z && this.m_selectExpression != null) {
            this.m_selectExpression.callVisitors(this, xSLTVisitor);
        }
        int sortElemCount = getSortElemCount();
        for (int i = 0; i < sortElemCount; i++) {
            getSortElem(i).callVisitors(xSLTVisitor);
        }
        super.callChildVisitors(xSLTVisitor, z);
    }

    @Override
    public Expression getExpression() {
        return this.m_selectExpression;
    }

    @Override
    public void setExpression(Expression expression) {
        expression.exprSetParent(this);
        this.m_selectExpression = expression;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.m_xpath = null;
    }
}
