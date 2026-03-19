package org.apache.xpath.axes;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.operations.Variable;

public class FilterExprWalker extends AxesWalker {
    static final long serialVersionUID = 5457182471424488375L;
    private boolean m_canDetachNodeset;
    private Expression m_expr;
    private transient XNodeSet m_exprObj;
    private boolean m_mustHardReset;

    public FilterExprWalker(WalkingIterator walkingIterator) {
        super(walkingIterator, 20);
        this.m_mustHardReset = false;
        this.m_canDetachNodeset = true;
    }

    @Override
    public void init(Compiler compiler, int i, int i2) throws TransformerException {
        super.init(compiler, i, i2);
        switch (i2) {
            case 22:
            case 23:
                break;
            case 24:
            case 25:
                this.m_mustHardReset = true;
                break;
            default:
                this.m_expr = compiler.compile(i + 2);
                this.m_expr.exprSetParent(this);
                return;
        }
        this.m_expr = compiler.compile(i);
        this.m_expr.exprSetParent(this);
        if (this.m_expr instanceof Variable) {
            this.m_canDetachNodeset = false;
        }
    }

    @Override
    public void detach() {
        super.detach();
        if (this.m_canDetachNodeset) {
            this.m_exprObj.detach();
        }
        this.m_exprObj = null;
    }

    @Override
    public void setRoot(int i) {
        super.setRoot(i);
        this.m_exprObj = FilterExprIteratorSimple.executeFilterExpr(i, this.m_lpi.getXPathContext(), this.m_lpi.getPrefixResolver(), this.m_lpi.getIsTopLevel(), this.m_lpi.m_stackFrame, this.m_expr);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        FilterExprWalker filterExprWalker = (FilterExprWalker) super.clone();
        if (this.m_exprObj != null) {
            filterExprWalker.m_exprObj = (XNodeSet) this.m_exprObj.clone();
        }
        return filterExprWalker;
    }

    @Override
    public short acceptNode(int i) {
        try {
            if (getPredicateCount() > 0) {
                countProximityPosition(0);
                if (!executePredicates(i, this.m_lpi.getXPathContext())) {
                    return (short) 3;
                }
                return (short) 1;
            }
            return (short) 1;
        } catch (TransformerException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public int getNextNode() {
        if (this.m_exprObj != null) {
            return this.m_exprObj.nextNode();
        }
        return -1;
    }

    @Override
    public int getLastPos(XPathContext xPathContext) {
        return this.m_exprObj.getLength();
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        super.fixupVariables(vector, i);
        this.m_expr.fixupVariables(vector, i);
    }

    public Expression getInnerExpression() {
        return this.m_expr;
    }

    public void setInnerExpression(Expression expression) {
        expression.exprSetParent(this);
        this.m_expr = expression;
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

    @Override
    public int getAxis() {
        return this.m_exprObj.getAxis();
    }

    class filterExprOwner implements ExpressionOwner {
        filterExprOwner() {
        }

        @Override
        public Expression getExpression() {
            return FilterExprWalker.this.m_expr;
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(FilterExprWalker.this);
            FilterExprWalker.this.m_expr = expression;
        }
    }

    @Override
    public void callPredicateVisitors(XPathVisitor xPathVisitor) {
        this.m_expr.callVisitors(new filterExprOwner(), xPathVisitor);
        super.callPredicateVisitors(xPathVisitor);
    }

    @Override
    public boolean deepEquals(Expression expression) {
        return super.deepEquals(expression) && this.m_expr.deepEquals(((FilterExprWalker) expression).m_expr);
    }
}
