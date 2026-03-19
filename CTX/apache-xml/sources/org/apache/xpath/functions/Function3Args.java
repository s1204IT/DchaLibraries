package org.apache.xpath.functions;

import java.util.Vector;
import org.apache.xalan.res.XSLMessages;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathVisitor;

public class Function3Args extends Function2Args {
    static final long serialVersionUID = 7915240747161506646L;
    Expression m_arg2;

    public Expression getArg2() {
        return this.m_arg2;
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        super.fixupVariables(vector, i);
        if (this.m_arg2 != null) {
            this.m_arg2.fixupVariables(vector, i);
        }
    }

    @Override
    public void setArg(Expression expression, int i) throws WrongNumberArgsException {
        if (i < 2) {
            super.setArg(expression, i);
        } else if (2 == i) {
            this.m_arg2 = expression;
            expression.exprSetParent(this);
        } else {
            reportWrongNumberArgs();
        }
    }

    @Override
    public void checkNumberArgs(int i) throws WrongNumberArgsException {
        if (i != 3) {
            reportWrongNumberArgs();
        }
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new WrongNumberArgsException(XSLMessages.createXPATHMessage("three", null));
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        if (super.canTraverseOutsideSubtree()) {
            return true;
        }
        return this.m_arg2.canTraverseOutsideSubtree();
    }

    class Arg2Owner implements ExpressionOwner {
        Arg2Owner() {
        }

        @Override
        public Expression getExpression() {
            return Function3Args.this.m_arg2;
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(Function3Args.this);
            Function3Args.this.m_arg2 = expression;
        }
    }

    @Override
    public void callArgVisitors(XPathVisitor xPathVisitor) {
        super.callArgVisitors(xPathVisitor);
        if (this.m_arg2 != null) {
            this.m_arg2.callVisitors(new Arg2Owner(), xPathVisitor);
        }
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!super.deepEquals(expression)) {
            return false;
        }
        if (this.m_arg2 == null) {
            return ((Function3Args) expression).m_arg2 == null;
        }
        Function3Args function3Args = (Function3Args) expression;
        return function3Args.m_arg2 != null && this.m_arg2.deepEquals(function3Args.m_arg2);
    }
}
