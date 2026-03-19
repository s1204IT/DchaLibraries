package org.apache.xpath.functions;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionNode;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.ExtensionsProvider;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.objects.XNull;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.res.XPATHErrorResources;
import org.apache.xpath.res.XPATHMessages;

public class FuncExtFunction extends Function {
    static final long serialVersionUID = 5196115554693708718L;
    Vector m_argVec = new Vector();
    String m_extensionName;
    Object m_methodKey;
    String m_namespace;

    @Override
    public void fixupVariables(Vector vector, int i) {
        if (this.m_argVec != null) {
            int size = this.m_argVec.size();
            for (int i2 = 0; i2 < size; i2++) {
                ((Expression) this.m_argVec.elementAt(i2)).fixupVariables(vector, i);
            }
        }
    }

    public String getNamespace() {
        return this.m_namespace;
    }

    public String getFunctionName() {
        return this.m_extensionName;
    }

    public Object getMethodKey() {
        return this.m_methodKey;
    }

    public Expression getArg(int i) {
        if (i >= 0 && i < this.m_argVec.size()) {
            return (Expression) this.m_argVec.elementAt(i);
        }
        return null;
    }

    public int getArgCount() {
        return this.m_argVec.size();
    }

    public FuncExtFunction(String str, String str2, Object obj) {
        this.m_namespace = str;
        this.m_extensionName = str2;
        this.m_methodKey = obj;
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        if (xPathContext.isSecureProcessing()) {
            throw new TransformerException(XPATHMessages.createXPATHMessage(XPATHErrorResources.ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED, new Object[]{toString()}));
        }
        Vector vector = new Vector();
        int size = this.m_argVec.size();
        for (int i = 0; i < size; i++) {
            XObject xObjectExecute = ((Expression) this.m_argVec.elementAt(i)).execute(xPathContext);
            xObjectExecute.allowDetachToRelease(false);
            vector.addElement(xObjectExecute);
        }
        Object objExtFunction = ((ExtensionsProvider) xPathContext.getOwnerObject()).extFunction(this, vector);
        if (objExtFunction != null) {
            return XObject.create(objExtFunction, xPathContext);
        }
        return new XNull();
    }

    @Override
    public void setArg(Expression expression, int i) throws WrongNumberArgsException {
        this.m_argVec.addElement(expression);
        expression.exprSetParent(this);
    }

    @Override
    public void checkNumberArgs(int i) throws WrongNumberArgsException {
    }

    class ArgExtOwner implements ExpressionOwner {
        Expression m_exp;

        ArgExtOwner(Expression expression) {
            this.m_exp = expression;
        }

        @Override
        public Expression getExpression() {
            return this.m_exp;
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(FuncExtFunction.this);
            this.m_exp = expression;
        }
    }

    @Override
    public void callArgVisitors(XPathVisitor xPathVisitor) {
        for (int i = 0; i < this.m_argVec.size(); i++) {
            Expression expression = (Expression) this.m_argVec.elementAt(i);
            expression.callVisitors(new ArgExtOwner(expression), xPathVisitor);
        }
    }

    @Override
    public void exprSetParent(ExpressionNode expressionNode) {
        super.exprSetParent(expressionNode);
        int size = this.m_argVec.size();
        for (int i = 0; i < size; i++) {
            ((Expression) this.m_argVec.elementAt(i)).exprSetParent(expressionNode);
        }
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{"Programmer's assertion:  the method FunctionMultiArgs.reportWrongNumberArgs() should never be called."}));
    }

    public String toString() {
        if (this.m_namespace != null && this.m_namespace.length() > 0) {
            return "{" + this.m_namespace + "}" + this.m_extensionName;
        }
        return this.m_extensionName;
    }
}
