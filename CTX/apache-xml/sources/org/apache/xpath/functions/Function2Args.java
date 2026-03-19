package org.apache.xpath.functions;

import java.util.Vector;
import org.apache.xalan.res.XSLMessages;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathVisitor;

public class Function2Args extends FunctionOneArg {
    static final long serialVersionUID = 5574294996842710641L;
    Expression m_arg1;

    public Expression getArg1() {
        return this.m_arg1;
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        super.fixupVariables(vector, i);
        if (this.m_arg1 != null) {
            this.m_arg1.fixupVariables(vector, i);
        }
    }

    @Override
    public void setArg(Expression expression, int i) throws WrongNumberArgsException {
        if (i == 0) {
            super.setArg(expression, i);
        } else if (1 == i) {
            this.m_arg1 = expression;
            expression.exprSetParent(this);
        } else {
            reportWrongNumberArgs();
        }
    }

    @Override
    public void checkNumberArgs(int i) throws WrongNumberArgsException {
        if (i != 2) {
            reportWrongNumberArgs();
        }
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createXPATHMessage("two", null));
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        if (super.canTraverseOutsideSubtree()) {
            return true;
        }
        return this.m_arg1.canTraverseOutsideSubtree();
    }

    class Arg1Owner implements ExpressionOwner {
        Arg1Owner() {
        }

        @Override
        public Expression getExpression() {
            return Function2Args.this.m_arg1;
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(Function2Args.this);
            Function2Args.this.m_arg1 = expression;
        }
    }

    @Override
    public void callArgVisitors(XPathVisitor xPathVisitor) {
        super.callArgVisitors(xPathVisitor);
        if (this.m_arg1 != null) {
            this.m_arg1.callVisitors(new Arg1Owner(), xPathVisitor);
        }
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!super.deepEquals(expression)) {
            return false;
        }
        if (this.m_arg1 == null) {
            return ((Function2Args) expression).m_arg1 == null;
        }
        Function2Args function2Args = (Function2Args) expression;
        return function2Args.m_arg1 != null && this.m_arg1.deepEquals(function2Args.m_arg1);
    }
}
