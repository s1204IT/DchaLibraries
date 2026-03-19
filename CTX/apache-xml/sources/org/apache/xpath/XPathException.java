package org.apache.xpath;

import java.io.PrintStream;
import java.io.PrintWriter;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Node;

public class XPathException extends TransformerException {
    static final long serialVersionUID = 4263549717619045963L;
    protected Exception m_exception;
    Object m_styleNode;

    public Object getStylesheetNode() {
        return this.m_styleNode;
    }

    public void setStylesheetNode(Object obj) {
        this.m_styleNode = obj;
    }

    public XPathException(String str, ExpressionNode expressionNode) {
        super(str);
        this.m_styleNode = null;
        setLocator(expressionNode);
        setStylesheetNode(getStylesheetNode(expressionNode));
    }

    public XPathException(String str) {
        super(str);
        this.m_styleNode = null;
    }

    public Node getStylesheetNode(ExpressionNode expressionNode) {
        ExpressionNode expressionOwner = getExpressionOwner(expressionNode);
        if (expressionOwner != null && (expressionOwner instanceof Node)) {
            return (Node) expressionOwner;
        }
        return null;
    }

    protected ExpressionNode getExpressionOwner(ExpressionNode expressionNode) {
        ExpressionNode expressionNodeExprGetParent = expressionNode.exprGetParent();
        while (expressionNodeExprGetParent != null && (expressionNodeExprGetParent instanceof Expression)) {
            expressionNodeExprGetParent = expressionNodeExprGetParent.exprGetParent();
        }
        return expressionNodeExprGetParent;
    }

    public XPathException(String str, Object obj) {
        super(str);
        this.m_styleNode = null;
        this.m_styleNode = obj;
    }

    public XPathException(String str, Node node, Exception exc) {
        super(str);
        this.m_styleNode = null;
        this.m_styleNode = node;
        this.m_exception = exc;
    }

    public XPathException(String str, Exception exc) {
        super(str);
        this.m_styleNode = null;
        this.m_exception = exc;
    }

    @Override
    public void printStackTrace(PrintStream printStream) {
        if (printStream == null) {
            printStream = System.err;
        }
        try {
            super.printStackTrace(printStream);
        } catch (Exception e) {
        }
        Throwable th = this.m_exception;
        for (int i = 0; i < 10 && th != null; i++) {
            printStream.println("---------");
            th.printStackTrace(printStream);
            if (th instanceof TransformerException) {
                Throwable exception = ((TransformerException) th).getException();
                if (th != exception) {
                    th = exception;
                } else {
                    return;
                }
            } else {
                th = null;
            }
        }
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        Throwable th = this.m_exception;
        while (th != null) {
            String message2 = th.getMessage();
            if (message2 != null) {
                message = message2;
            }
            if (th instanceof TransformerException) {
                Throwable exception = ((TransformerException) th).getException();
                if (th == exception) {
                    break;
                }
                th = exception;
            } else {
                th = null;
            }
        }
        return message != null ? message : "";
    }

    @Override
    public void printStackTrace(PrintWriter printWriter) {
        if (printWriter == null) {
            printWriter = new PrintWriter(System.err);
        }
        try {
            super.printStackTrace(printWriter);
        } catch (Exception e) {
        }
        boolean z = true;
        try {
            Throwable.class.getMethod("getCause", (Class) null);
        } catch (NoSuchMethodException e2) {
            z = false;
        }
        if (!z) {
            Throwable th = this.m_exception;
            for (int i = 0; i < 10 && th != null; i++) {
                printWriter.println("---------");
                try {
                    th.printStackTrace(printWriter);
                } catch (Exception e3) {
                    printWriter.println("Could not print stack trace...");
                }
                if (!(th instanceof TransformerException)) {
                    th = null;
                } else {
                    Throwable exception = ((TransformerException) th).getException();
                    if (th != exception) {
                        th = exception;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public Throwable getException() {
        return this.m_exception;
    }
}
