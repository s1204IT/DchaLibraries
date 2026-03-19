package org.apache.xpath.operations;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.objects.XObject;

public abstract class UnaryOperation extends Expression implements ExpressionOwner {
    static final long serialVersionUID = 6536083808424286166L;
    protected Expression m_right;

    public abstract XObject operate(XObject xObject) throws TransformerException;

    @Override
    public void fixupVariables(Vector vector, int i) {
        this.m_right.fixupVariables(vector, i);
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        if (this.m_right != null && this.m_right.canTraverseOutsideSubtree()) {
            return true;
        }
        return false;
    }

    public void setRight(Expression expression) {
        this.m_right = expression;
        expression.exprSetParent(this);
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        return operate(this.m_right.execute(xPathContext));
    }

    public Expression getOperand() {
        return this.m_right;
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        if (xPathVisitor.visitUnaryOperation(expressionOwner, this)) {
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
        return isSameClass(expression) && this.m_right.deepEquals(((UnaryOperation) expression).m_right);
    }
}
