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
    public void fixupVariables(Vector vars, int globalsSize) {
        this.m_right.fixupVariables(vars, globalsSize);
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        return this.m_right != null && this.m_right.canTraverseOutsideSubtree();
    }

    public void setRight(Expression r) {
        this.m_right = r;
        r.exprSetParent(this);
    }

    @Override
    public XObject execute(XPathContext xctxt) throws TransformerException {
        return operate(this.m_right.execute(xctxt));
    }

    public Expression getOperand() {
        return this.m_right;
    }

    @Override
    public void callVisitors(ExpressionOwner owner, XPathVisitor visitor) {
        if (visitor.visitUnaryOperation(owner, this)) {
            this.m_right.callVisitors(this, visitor);
        }
    }

    @Override
    public Expression getExpression() {
        return this.m_right;
    }

    @Override
    public void setExpression(Expression exp) {
        exp.exprSetParent(this);
        this.m_right = exp;
    }

    @Override
    public boolean deepEquals(Expression expr) {
        return isSameClass(expr) && this.m_right.deepEquals(((UnaryOperation) expr).m_right);
    }
}
