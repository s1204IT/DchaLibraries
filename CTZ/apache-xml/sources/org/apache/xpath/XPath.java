package org.apache.xpath;

import java.io.Serializable;
import java.util.Vector;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.utils.DefaultErrorHandler;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.FunctionTable;
import org.apache.xpath.compiler.XPathParser;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;
import org.w3c.dom.Node;

public class XPath implements Serializable, ExpressionOwner {
    private static final boolean DEBUG_MATCHES = false;
    public static final int MATCH = 1;
    public static final double MATCH_SCORE_NODETEST = -0.5d;
    public static final double MATCH_SCORE_NONE = Double.NEGATIVE_INFINITY;
    public static final double MATCH_SCORE_NSWILD = -0.25d;
    public static final double MATCH_SCORE_OTHER = 0.5d;
    public static final double MATCH_SCORE_QNAME = 0.0d;
    public static final int SELECT = 0;
    static final long serialVersionUID = 3976493477939110553L;
    private transient FunctionTable m_funcTable;
    private Expression m_mainExp;
    String m_patternString;

    private void initFunctionTable() {
        this.m_funcTable = new FunctionTable();
    }

    @Override
    public Expression getExpression() {
        return this.m_mainExp;
    }

    public void fixupVariables(Vector vector, int i) {
        this.m_mainExp.fixupVariables(vector, i);
    }

    @Override
    public void setExpression(Expression expression) {
        if (this.m_mainExp != null) {
            expression.exprSetParent(this.m_mainExp.exprGetParent());
        }
        this.m_mainExp = expression;
    }

    public SourceLocator getLocator() {
        return this.m_mainExp;
    }

    public String getPatternString() {
        return this.m_patternString;
    }

    public XPath(String str, SourceLocator sourceLocator, PrefixResolver prefixResolver, int i, ErrorListener errorListener) throws TransformerException {
        this.m_funcTable = null;
        initFunctionTable();
        errorListener = errorListener == null ? new DefaultErrorHandler() : errorListener;
        this.m_patternString = str;
        XPathParser xPathParser = new XPathParser(errorListener, sourceLocator);
        Compiler compiler = new Compiler(errorListener, sourceLocator, this.m_funcTable);
        if (i == 0) {
            xPathParser.initXPath(compiler, str, prefixResolver);
        } else if (1 == i) {
            xPathParser.initMatchPattern(compiler, str, prefixResolver);
        } else {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_CANNOT_DEAL_XPATH_TYPE, new Object[]{Integer.toString(i)}));
        }
        Expression expressionCompile = compiler.compile(0);
        setExpression(expressionCompile);
        if (sourceLocator != null && (sourceLocator instanceof ExpressionNode)) {
            expressionCompile.exprSetParent((ExpressionNode) sourceLocator);
        }
    }

    public XPath(String str, SourceLocator sourceLocator, PrefixResolver prefixResolver, int i, ErrorListener errorListener, FunctionTable functionTable) throws TransformerException {
        this.m_funcTable = null;
        this.m_funcTable = functionTable;
        errorListener = errorListener == null ? new DefaultErrorHandler() : errorListener;
        this.m_patternString = str;
        XPathParser xPathParser = new XPathParser(errorListener, sourceLocator);
        Compiler compiler = new Compiler(errorListener, sourceLocator, this.m_funcTable);
        if (i == 0) {
            xPathParser.initXPath(compiler, str, prefixResolver);
        } else if (1 == i) {
            xPathParser.initMatchPattern(compiler, str, prefixResolver);
        } else {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_CANNOT_DEAL_XPATH_TYPE, new Object[]{Integer.toString(i)}));
        }
        Expression expressionCompile = compiler.compile(0);
        setExpression(expressionCompile);
        if (sourceLocator != null && (sourceLocator instanceof ExpressionNode)) {
            expressionCompile.exprSetParent((ExpressionNode) sourceLocator);
        }
    }

    public XPath(String str, SourceLocator sourceLocator, PrefixResolver prefixResolver, int i) throws TransformerException {
        this(str, sourceLocator, prefixResolver, i, null);
    }

    public XPath(Expression expression) {
        this.m_funcTable = null;
        setExpression(expression);
        initFunctionTable();
    }

    public XObject execute(XPathContext xPathContext, Node node, PrefixResolver prefixResolver) throws TransformerException {
        return execute(xPathContext, xPathContext.getDTMHandleFromNode(node), prefixResolver);
    }

    public XObject execute(XPathContext xPathContext, int i, PrefixResolver prefixResolver) throws TransformerException {
        xPathContext.pushNamespaceContext(prefixResolver);
        xPathContext.pushCurrentNodeAndExpression(i, i);
        try {
            try {
                return this.m_mainExp.execute(xPathContext);
            } catch (TransformerException e) {
                e.setLocator(getLocator());
                ErrorListener errorListener = xPathContext.getErrorListener();
                if (errorListener == null) {
                    throw e;
                }
                errorListener.error(e);
                return null;
            } catch (Exception e2) {
                e = e2;
                while (e instanceof WrappedRuntimeException) {
                    e = ((WrappedRuntimeException) e).getException();
                }
                String message = e.getMessage();
                if (message == null || message.length() == 0) {
                    message = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_XPATH_ERROR, null);
                }
                TransformerException transformerException = new TransformerException(message, getLocator(), e);
                ErrorListener errorListener2 = xPathContext.getErrorListener();
                if (errorListener2 == null) {
                    throw transformerException;
                }
                errorListener2.fatalError(transformerException);
                return null;
            }
        } finally {
            xPathContext.popNamespaceContext();
            xPathContext.popCurrentNodeAndExpression();
        }
    }

    public boolean bool(XPathContext xPathContext, int i, PrefixResolver prefixResolver) throws TransformerException {
        xPathContext.pushNamespaceContext(prefixResolver);
        xPathContext.pushCurrentNodeAndExpression(i, i);
        try {
            try {
                try {
                    return this.m_mainExp.bool(xPathContext);
                } catch (TransformerException e) {
                    e.setLocator(getLocator());
                    ErrorListener errorListener = xPathContext.getErrorListener();
                    if (errorListener == null) {
                        throw e;
                    }
                    errorListener.error(e);
                    xPathContext.popNamespaceContext();
                    xPathContext.popCurrentNodeAndExpression();
                    return false;
                }
            } catch (Exception e2) {
                e = e2;
                while (e instanceof WrappedRuntimeException) {
                    e = ((WrappedRuntimeException) e).getException();
                }
                String message = e.getMessage();
                if (message == null || message.length() == 0) {
                    message = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_XPATH_ERROR, null);
                }
                TransformerException transformerException = new TransformerException(message, getLocator(), e);
                ErrorListener errorListener2 = xPathContext.getErrorListener();
                if (errorListener2 == null) {
                    throw transformerException;
                }
                errorListener2.fatalError(transformerException);
                xPathContext.popNamespaceContext();
                xPathContext.popCurrentNodeAndExpression();
                return false;
            }
        } finally {
            xPathContext.popNamespaceContext();
            xPathContext.popCurrentNodeAndExpression();
        }
    }

    public double getMatchScore(XPathContext xPathContext, int i) throws TransformerException {
        xPathContext.pushCurrentNode(i);
        xPathContext.pushCurrentExpressionNode(i);
        try {
            return this.m_mainExp.execute(xPathContext).num();
        } finally {
            xPathContext.popCurrentNode();
            xPathContext.popCurrentExpressionNode();
        }
    }

    public void warn(XPathContext xPathContext, int i, String str, Object[] objArr) throws TransformerException {
        String strCreateXPATHWarning = XSLMessages.createXPATHWarning(str, objArr);
        ErrorListener errorListener = xPathContext.getErrorListener();
        if (errorListener != null) {
            errorListener.warning(new TransformerException(strCreateXPATHWarning, (SAXSourceLocator) xPathContext.getSAXLocator()));
        }
    }

    public void assertion(boolean z, String str) {
        if (!z) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{str}));
        }
    }

    public void error(XPathContext xPathContext, int i, String str, Object[] objArr) throws TransformerException {
        String strCreateXPATHMessage = XSLMessages.createXPATHMessage(str, objArr);
        ErrorListener errorListener = xPathContext.getErrorListener();
        if (errorListener != null) {
            errorListener.fatalError(new TransformerException(strCreateXPATHMessage, (SAXSourceLocator) xPathContext.getSAXLocator()));
            return;
        }
        SourceLocator sAXLocator = xPathContext.getSAXLocator();
        System.out.println(strCreateXPATHMessage + "; file " + sAXLocator.getSystemId() + "; line " + sAXLocator.getLineNumber() + "; column " + sAXLocator.getColumnNumber());
    }

    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        this.m_mainExp.callVisitors(this, xPathVisitor);
    }
}
