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
    public void fixupVariables(Vector vars, int globalsSize) {
        super.fixupVariables(vars, globalsSize);
        if (this.m_arg1 == null) {
            return;
        }
        this.m_arg1.fixupVariables(vars, globalsSize);
    }

    @Override
    public void setArg(Expression arg, int argNum) throws WrongNumberArgsException {
        if (argNum == 0) {
            super.setArg(arg, argNum);
        } else if (1 == argNum) {
            this.m_arg1 = arg;
            arg.exprSetParent(this);
        } else {
            reportWrongNumberArgs();
        }
    }

    @Override
    public void checkNumberArgs(int argNum) throws WrongNumberArgsException {
        if (argNum == 2) {
            return;
        }
        reportWrongNumberArgs();
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
        public void setExpression(Expression exp) {
            exp.exprSetParent(Function2Args.this);
            Function2Args.this.m_arg1 = exp;
        }
    }

    @Override
    public void callArgVisitors(XPathVisitor visitor) {
        super.callArgVisitors(visitor);
        if (this.m_arg1 == null) {
            return;
        }
        this.m_arg1.callVisitors(new Arg1Owner(), visitor);
    }

    @Override
    public boolean deepEquals(Expression expr) {
        if (super.deepEquals(expr)) {
            return this.m_arg1 != null ? ((Function2Args) expr).m_arg1 != null && this.m_arg1.deepEquals(((Function2Args) expr).m_arg1) : ((Function2Args) expr).m_arg1 == null;
        }
        return false;
    }
}
