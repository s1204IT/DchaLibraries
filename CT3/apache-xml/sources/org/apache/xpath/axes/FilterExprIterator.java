package org.apache.xpath.axes;

import java.util.Vector;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.objects.XNodeSet;

public class FilterExprIterator extends BasicTestIterator {
    static final long serialVersionUID = 2552176105165737614L;
    private boolean m_canDetachNodeset;
    private Expression m_expr;
    private transient XNodeSet m_exprObj;
    private boolean m_mustHardReset;

    public FilterExprIterator() {
        super(null);
        this.m_mustHardReset = false;
        this.m_canDetachNodeset = true;
    }

    public FilterExprIterator(Expression expr) {
        super(null);
        this.m_mustHardReset = false;
        this.m_canDetachNodeset = true;
        this.m_expr = expr;
    }

    @Override
    public void setRoot(int context, Object environment) {
        super.setRoot(context, environment);
        this.m_exprObj = FilterExprIteratorSimple.executeFilterExpr(context, this.m_execContext, getPrefixResolver(), getIsTopLevel(), this.m_stackFrame, this.m_expr);
    }

    @Override
    protected int getNextNode() {
        if (this.m_exprObj != null) {
            this.m_lastFetched = this.m_exprObj.nextNode();
        } else {
            this.m_lastFetched = -1;
        }
        return this.m_lastFetched;
    }

    @Override
    public void detach() {
        super.detach();
        this.m_exprObj.detach();
        this.m_exprObj = null;
    }

    @Override
    public void fixupVariables(Vector vars, int globalsSize) {
        super.fixupVariables(vars, globalsSize);
        this.m_expr.fixupVariables(vars, globalsSize);
    }

    public Expression getInnerExpression() {
        return this.m_expr;
    }

    public void setInnerExpression(Expression expr) {
        expr.exprSetParent(this);
        this.m_expr = expr;
    }

    @Override
    public int getAnalysisBits() {
        if (this.m_expr != null && (this.m_expr instanceof PathComponent)) {
            return ((PathComponent) this.m_expr).getAnalysisBits();
        }
        return WalkerFactory.BIT_FILTER;
    }

    @Override
    public boolean isDocOrdered() {
        return this.m_exprObj.isDocOrdered();
    }

    class filterExprOwner implements ExpressionOwner {
        filterExprOwner() {
        }

        @Override
        public Expression getExpression() {
            return FilterExprIterator.this.m_expr;
        }

        @Override
        public void setExpression(Expression exp) {
            exp.exprSetParent(FilterExprIterator.this);
            FilterExprIterator.this.m_expr = exp;
        }
    }

    @Override
    public void callPredicateVisitors(XPathVisitor visitor) {
        this.m_expr.callVisitors(new filterExprOwner(), visitor);
        super.callPredicateVisitors(visitor);
    }

    @Override
    public boolean deepEquals(Expression expr) {
        if (!super.deepEquals(expr)) {
            return false;
        }
        FilterExprIterator fet = (FilterExprIterator) expr;
        return this.m_expr.deepEquals(fet.m_expr);
    }
}
