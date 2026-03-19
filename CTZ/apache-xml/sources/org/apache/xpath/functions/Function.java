package org.apache.xpath.functions;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.objects.XObject;

public abstract class Function extends Expression {
    static final long serialVersionUID = 6927661240854599768L;

    public void setArg(Expression expression, int i) throws WrongNumberArgsException {
        reportWrongNumberArgs();
    }

    public void checkNumberArgs(int i) throws WrongNumberArgsException {
        if (i != 0) {
            reportWrongNumberArgs();
        }
    }

    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createXPATHMessage("zero", null));
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        System.out.println("Error! Function.execute should not be called!");
        return null;
    }

    public void callArgVisitors(XPathVisitor xPathVisitor) {
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        if (xPathVisitor.visitFunction(expressionOwner, this)) {
            callArgVisitors(xPathVisitor);
        }
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!isSameClass(expression)) {
            return false;
        }
        return true;
    }

    public void postCompileStep(Compiler compiler) {
    }
}
