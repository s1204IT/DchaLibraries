package org.apache.xpath.functions;

import java.util.Vector;
import org.apache.xalan.res.XSLMessages;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathVisitor;

public class FunctionOneArg extends Function implements ExpressionOwner {
    static final long serialVersionUID = -5180174180765609758L;
    Expression m_arg0;

    public Expression getArg0() {
        return this.m_arg0;
    }

    @Override
    public void setArg(Expression expression, int i) throws WrongNumberArgsException {
        if (i == 0) {
            this.m_arg0 = expression;
            expression.exprSetParent(this);
        } else {
            reportWrongNumberArgs();
        }
    }

    @Override
    public void checkNumberArgs(int i) throws WrongNumberArgsException {
        if (i != 1) {
            reportWrongNumberArgs();
        }
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createXPATHMessage("one", null));
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        return this.m_arg0.canTraverseOutsideSubtree();
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        if (this.m_arg0 != null) {
            this.m_arg0.fixupVariables(vector, i);
        }
    }

    @Override
    public void callArgVisitors(XPathVisitor xPathVisitor) {
        if (this.m_arg0 != null) {
            this.m_arg0.callVisitors(this, xPathVisitor);
        }
    }

    @Override
    public Expression getExpression() {
        return this.m_arg0;
    }

    @Override
    public void setExpression(Expression expression) {
        expression.exprSetParent(this);
        this.m_arg0 = expression;
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!super.deepEquals(expression)) {
            return false;
        }
        if (this.m_arg0 == null) {
            return ((FunctionOneArg) expression).m_arg0 == null;
        }
        FunctionOneArg functionOneArg = (FunctionOneArg) expression;
        return functionOneArg.m_arg0 != null && this.m_arg0.deepEquals(functionOneArg.m_arg0);
    }
}
