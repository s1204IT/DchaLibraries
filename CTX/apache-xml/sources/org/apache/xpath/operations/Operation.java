package org.apache.xpath.operations;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.objects.XObject;

public class Operation extends Expression implements ExpressionOwner {
    static final long serialVersionUID = -3037139537171050430L;
    protected Expression m_left;
    protected Expression m_right;

    @Override
    public void fixupVariables(Vector vector, int i) {
        this.m_left.fixupVariables(vector, i);
        this.m_right.fixupVariables(vector, i);
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        if (this.m_left == null || !this.m_left.canTraverseOutsideSubtree()) {
            return this.m_right != null && this.m_right.canTraverseOutsideSubtree();
        }
        return true;
    }

    public void setLeftRight(Expression expression, Expression expression2) {
        this.m_left = expression;
        this.m_right = expression2;
        expression.exprSetParent(this);
        expression2.exprSetParent(this);
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        XObject xObjectExecute = this.m_left.execute(xPathContext, true);
        XObject xObjectExecute2 = this.m_right.execute(xPathContext, true);
        XObject xObjectOperate = operate(xObjectExecute, xObjectExecute2);
        xObjectExecute.detach();
        xObjectExecute2.detach();
        return xObjectOperate;
    }

    public XObject operate(XObject xObject, XObject xObject2) throws TransformerException {
        return null;
    }

    public Expression getLeftOperand() {
        return this.m_left;
    }

    public Expression getRightOperand() {
        return this.m_right;
    }

    class LeftExprOwner implements ExpressionOwner {
        LeftExprOwner() {
        }

        @Override
        public Expression getExpression() {
            return Operation.this.m_left;
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(Operation.this);
            Operation.this.m_left = expression;
        }
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        if (xPathVisitor.visitBinaryOperation(expressionOwner, this)) {
            this.m_left.callVisitors(new LeftExprOwner(), xPathVisitor);
            this.m_right.callVisitors(this, xPathVisitor);
        }
    }

    @Override
    public Expression getExpression() {
        return this.m_right;
    }

    @Override
    public void setExpression(Expression expression) {
        expression.exprSetParent(this);
        this.m_right = expression;
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!isSameClass(expression)) {
            return false;
        }
        Operation operation = (Operation) expression;
        return this.m_left.deepEquals(operation.m_left) && this.m_right.deepEquals(operation.m_right);
    }
}
