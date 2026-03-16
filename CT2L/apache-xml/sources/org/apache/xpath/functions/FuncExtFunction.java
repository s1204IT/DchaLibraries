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
    public void fixupVariables(Vector vars, int globalsSize) {
        if (this.m_argVec != null) {
            int nArgs = this.m_argVec.size();
            for (int i = 0; i < nArgs; i++) {
                Expression arg = (Expression) this.m_argVec.elementAt(i);
                arg.fixupVariables(vars, globalsSize);
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

    public Expression getArg(int n) {
        if (n < 0 || n >= this.m_argVec.size()) {
            return null;
        }
        return (Expression) this.m_argVec.elementAt(n);
    }

    public int getArgCount() {
        return this.m_argVec.size();
    }

    public FuncExtFunction(String namespace, String extensionName, Object methodKey) {
        this.m_namespace = namespace;
        this.m_extensionName = extensionName;
        this.m_methodKey = methodKey;
    }

    @Override
    public XObject execute(XPathContext xctxt) throws TransformerException {
        if (xctxt.isSecureProcessing()) {
            throw new TransformerException(XPATHMessages.createXPATHMessage(XPATHErrorResources.ER_EXTENSION_FUNCTION_CANNOT_BE_INVOKED, new Object[]{toString()}));
        }
        Vector argVec = new Vector();
        int nArgs = this.m_argVec.size();
        for (int i = 0; i < nArgs; i++) {
            Expression arg = (Expression) this.m_argVec.elementAt(i);
            XObject xobj = arg.execute(xctxt);
            xobj.allowDetachToRelease(false);
            argVec.addElement(xobj);
        }
        ExtensionsProvider extProvider = (ExtensionsProvider) xctxt.getOwnerObject();
        Object val = extProvider.extFunction(this, argVec);
        if (val != null) {
            XObject result = XObject.create(val, xctxt);
            return result;
        }
        XObject result2 = new XNull();
        return result2;
    }

    @Override
    public void setArg(Expression arg, int argNum) throws WrongNumberArgsException {
        this.m_argVec.addElement(arg);
        arg.exprSetParent(this);
    }

    @Override
    public void checkNumberArgs(int argNum) throws WrongNumberArgsException {
    }

    class ArgExtOwner implements ExpressionOwner {
        Expression m_exp;

        ArgExtOwner(Expression exp) {
            this.m_exp = exp;
        }

        @Override
        public Expression getExpression() {
            return this.m_exp;
        }

        @Override
        public void setExpression(Expression exp) {
            exp.exprSetParent(FuncExtFunction.this);
            this.m_exp = exp;
        }
    }

    @Override
    public void callArgVisitors(XPathVisitor visitor) {
        for (int i = 0; i < this.m_argVec.size(); i++) {
            Expression exp = (Expression) this.m_argVec.elementAt(i);
            exp.callVisitors(new ArgExtOwner(exp), visitor);
        }
    }

    @Override
    public void exprSetParent(ExpressionNode n) {
        super.exprSetParent(n);
        int nArgs = this.m_argVec.size();
        for (int i = 0; i < nArgs; i++) {
            Expression arg = (Expression) this.m_argVec.elementAt(i);
            arg.exprSetParent(n);
        }
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        String fMsg = XSLMessages.createXPATHMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{"Programmer's assertion:  the method FunctionMultiArgs.reportWrongNumberArgs() should never be called."});
        throw new RuntimeException(fMsg);
    }

    public String toString() {
        return (this.m_namespace == null || this.m_namespace.length() <= 0) ? this.m_extensionName : "{" + this.m_namespace + "}" + this.m_extensionName;
    }
}
