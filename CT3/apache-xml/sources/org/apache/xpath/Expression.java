package org.apache.xpath;

import java.io.Serializable;
import java.util.Vector;
import javax.xml.transform.ErrorListener;
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

    public XObject execute(XPathContext xctxt, int currentNode) throws TransformerException {
        return execute(xctxt);
    }

    public XObject execute(XPathContext xctxt, int currentNode, DTM dtm, int expType) throws TransformerException {
        return execute(xctxt);
    }

    public XObject execute(XPathContext xctxt, boolean destructiveOK) throws TransformerException {
        return execute(xctxt);
    }

    public double num(XPathContext xctxt) throws TransformerException {
        return execute(xctxt).num();
    }

    public boolean bool(XPathContext xctxt) throws TransformerException {
        return execute(xctxt).bool();
    }

    public XMLString xstr(XPathContext xctxt) throws TransformerException {
        return execute(xctxt).xstr();
    }

    public boolean isNodesetExpr() {
        return false;
    }

    public int asNode(XPathContext xctxt) throws TransformerException {
        DTMIterator iter = execute(xctxt).iter();
        return iter.nextNode();
    }

    public DTMIterator asIterator(XPathContext xctxt, int contextNode) throws TransformerException {
        try {
            xctxt.pushCurrentNodeAndExpression(contextNode, contextNode);
            return execute(xctxt).iter();
        } finally {
            xctxt.popCurrentNodeAndExpression();
        }
    }

    public DTMIterator asIteratorRaw(XPathContext xctxt, int contextNode) throws TransformerException {
        try {
            xctxt.pushCurrentNodeAndExpression(contextNode, contextNode);
            XNodeSet nodeset = (XNodeSet) execute(xctxt);
            return nodeset.iterRaw();
        } finally {
            xctxt.popCurrentNodeAndExpression();
        }
    }

    public void executeCharsToContentHandler(XPathContext xctxt, ContentHandler handler) throws TransformerException, SAXException {
        XObject obj = execute(xctxt);
        obj.dispatchCharactersEvents(handler);
        obj.detach();
    }

    public boolean isStableNumber() {
        return false;
    }

    protected final boolean isSameClass(Expression expr) {
        return expr != null && getClass() == expr.getClass();
    }

    public void warn(XPathContext xctxt, String msg, Object[] args) throws TransformerException {
        String fmsg = XSLMessages.createXPATHWarning(msg, args);
        if (xctxt == null) {
            return;
        }
        ErrorListener eh = xctxt.getErrorListener();
        eh.warning(new TransformerException(fmsg, xctxt.getSAXLocator()));
    }

    public void assertion(boolean b, String msg) {
        if (b) {
            return;
        }
        String fMsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{msg});
        throw new RuntimeException(fMsg);
    }

    public void error(XPathContext xctxt, String msg, Object[] args) throws TransformerException {
        String fmsg = XSLMessages.createXPATHMessage(msg, args);
        if (xctxt == null) {
            return;
        }
        ErrorListener eh = xctxt.getErrorListener();
        TransformerException te = new TransformerException(fmsg, this);
        eh.fatalError(te);
    }

    public ExpressionNode getExpressionOwner() {
        ExpressionNode parent = exprGetParent();
        while (parent != null && (parent instanceof Expression)) {
            parent = parent.exprGetParent();
        }
        return parent;
    }

    @Override
    public void exprSetParent(ExpressionNode n) {
        assertion(n != this, "Can not parent an expression to itself!");
        this.m_parent = n;
    }

    @Override
    public ExpressionNode exprGetParent() {
        return this.m_parent;
    }

    @Override
    public void exprAddChild(ExpressionNode n, int i) {
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
