package org.apache.xpath;

import java.io.Serializable;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public abstract class Expression implements Serializable, ExpressionNode, XPathVisitable {
    static final long serialVersionUID = 565665869777906902L;
    private ExpressionNode m_parent;

    public abstract boolean deepEquals(Expression expression);

    public abstract XObject execute(XPathContext xPathContext) throws TransformerException;

    public abstract void fixupVariables(Vector vector, int i);

    public boolean canTraverseOutsideSubtree() {
        return false;
    }

    public XObject execute(XPathContext xPathContext, int i) throws TransformerException {
        return execute(xPathContext);
    }

    public XObject execute(XPathContext xPathContext, int i, DTM dtm, int i2) throws TransformerException {
        return execute(xPathContext);
    }

    public XObject execute(XPathContext xPathContext, boolean z) throws TransformerException {
        return execute(xPathContext);
    }

    public double num(XPathContext xPathContext) throws TransformerException {
        return execute(xPathContext).num();
    }

    public boolean bool(XPathContext xPathContext) throws TransformerException {
        return execute(xPathContext).bool();
    }

    public XMLString xstr(XPathContext xPathContext) throws TransformerException {
        return execute(xPathContext).xstr();
    }

    public boolean isNodesetExpr() {
        return false;
    }

    public int asNode(XPathContext xPathContext) throws TransformerException {
        return execute(xPathContext).iter().nextNode();
    }

    public DTMIterator asIterator(XPathContext xPathContext, int i) throws TransformerException {
        try {
            xPathContext.pushCurrentNodeAndExpression(i, i);
            return execute(xPathContext).iter();
        } finally {
            xPathContext.popCurrentNodeAndExpression();
        }
    }

    public DTMIterator asIteratorRaw(XPathContext xPathContext, int i) throws TransformerException {
        try {
            xPathContext.pushCurrentNodeAndExpression(i, i);
            return ((XNodeSet) execute(xPathContext)).iterRaw();
        } finally {
            xPathContext.popCurrentNodeAndExpression();
        }
    }

    public void executeCharsToContentHandler(XPathContext xPathContext, ContentHandler contentHandler) throws TransformerException, SAXException {
        XObject xObjectExecute = execute(xPathContext);
        xObjectExecute.dispatchCharactersEvents(contentHandler);
        xObjectExecute.detach();
    }

    public boolean isStableNumber() {
        return false;
    }

    protected final boolean isSameClass(Expression expression) {
        return expression != null && getClass() == expression.getClass();
    }

    public void warn(XPathContext xPathContext, String str, Object[] objArr) throws TransformerException {
        String strCreateXPATHWarning = XSLMessages.createXPATHWarning(str, objArr);
        if (xPathContext != null) {
            xPathContext.getErrorListener().warning(new TransformerException(strCreateXPATHWarning, xPathContext.getSAXLocator()));
        }
    }

    public void assertion(boolean z, String str) {
        if (!z) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{str}));
        }
    }

    public void error(XPathContext xPathContext, String str, Object[] objArr) throws TransformerException {
        String strCreateXPATHMessage = XSLMessages.createXPATHMessage(str, objArr);
        if (xPathContext != null) {
            xPathContext.getErrorListener().fatalError(new TransformerException(strCreateXPATHMessage, this));
        }
    }

    public ExpressionNode getExpressionOwner() {
        ExpressionNode expressionNodeExprGetParent = exprGetParent();
        while (expressionNodeExprGetParent != null && (expressionNodeExprGetParent instanceof Expression)) {
            expressionNodeExprGetParent = expressionNodeExprGetParent.exprGetParent();
        }
        return expressionNodeExprGetParent;
    }

    @Override
    public void exprSetParent(ExpressionNode expressionNode) {
        assertion(expressionNode != this, "Can not parent an expression to itself!");
        this.m_parent = expressionNode;
    }

    @Override
    public ExpressionNode exprGetParent() {
        return this.m_parent;
    }

    @Override
    public void exprAddChild(ExpressionNode expressionNode, int i) {
        assertion(false, "exprAddChild method not implemented!");
    }

    @Override
    public ExpressionNode exprGetChild(int i) {
        return null;
    }

    @Override
    public int exprGetNumChildren() {
        return 0;
    }

    @Override
    public String getPublicId() {
        if (this.m_parent == null) {
            return null;
        }
        return this.m_parent.getPublicId();
    }

    @Override
    public String getSystemId() {
        if (this.m_parent == null) {
            return null;
        }
        return this.m_parent.getSystemId();
    }

    @Override
    public int getLineNumber() {
        if (this.m_parent == null) {
            return 0;
        }
        return this.m_parent.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        if (this.m_parent == null) {
            return 0;
        }
        return this.m_parent.getColumnNumber();
    }
}
