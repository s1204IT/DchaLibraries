package org.apache.xpath.functions;

import java.util.Vector;
import org.apache.xalan.res.XSLMessages;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.res.XPATHErrorResources;

public class FunctionMultiArgs extends Function3Args {
    static final long serialVersionUID = 7117257746138417181L;
    Expression[] m_args;

    public Expression[] getArgs() {
        return this.m_args;
    }

    @Override
    public void setArg(Expression expression, int i) throws WrongNumberArgsException {
        if (i < 3) {
            super.setArg(expression, i);
            return;
        }
        if (this.m_args == null) {
            this.m_args = new Expression[1];
            this.m_args[0] = expression;
        } else {
            Expression[] expressionArr = new Expression[this.m_args.length + 1];
            System.arraycopy(this.m_args, 0, expressionArr, 0, this.m_args.length);
            expressionArr[this.m_args.length] = expression;
            this.m_args = expressionArr;
        }
        expression.exprSetParent(this);
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        super.fixupVariables(vector, i);
        if (this.m_args != null) {
            for (int i2 = 0; i2 < this.m_args.length; i2++) {
                this.m_args[i2].fixupVariables(vector, i);
            }
        }
    }

    @Override
    public void checkNumberArgs(int i) throws WrongNumberArgsException {
    }

    @Override
    protected void reportWrongNumberArgs() throws WrongNumberArgsException {
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION, new Object[]{"Programmer's assertion:  the method FunctionMultiArgs.reportWrongNumberArgs() should never be called."}));
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        if (super.canTraverseOutsideSubtree()) {
            return true;
        }
        int length = this.m_args.length;
        for (int i = 0; i < length; i++) {
            if (this.m_args[i].canTraverseOutsideSubtree()) {
                return true;
            }
        }
        return false;
    }

    class ArgMultiOwner implements ExpressionOwner {
        int m_argIndex;

        ArgMultiOwner(int i) {
            this.m_argIndex = i;
        }

        @Override
        public Expression getExpression() {
            return FunctionMultiArgs.this.m_args[this.m_argIndex];
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(FunctionMultiArgs.this);
            FunctionMultiArgs.this.m_args[this.m_argIndex] = expression;
        }
    }

    @Override
    public void callArgVisitors(XPathVisitor xPathVisitor) {
        super.callArgVisitors(xPathVisitor);
        if (this.m_args != null) {
            int length = this.m_args.length;
            for (int i = 0; i < length; i++) {
                this.m_args[i].callVisitors(new ArgMultiOwner(i), xPathVisitor);
            }
        }
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!super.deepEquals(expression)) {
            return false;
        }
        FunctionMultiArgs functionMultiArgs = (FunctionMultiArgs) expression;
        if (this.m_args == null) {
            return functionMultiArgs.m_args == null;
        }
        int length = this.m_args.length;
        if (functionMultiArgs == null || functionMultiArgs.m_args.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!this.m_args[i].deepEquals(functionMultiArgs.m_args[i])) {
                return false;
            }
        }
        return true;
    }
}
