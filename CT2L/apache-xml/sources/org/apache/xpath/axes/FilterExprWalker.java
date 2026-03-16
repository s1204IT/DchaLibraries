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

    public FilterExprWalker(WalkingIterator locPathIterator) {
        super(locPathIterator, 20);
        this.m_mustHardReset = false;
        this.m_canDetachNodeset = true;
    }

    @Override
    public void init(Compiler compiler, int opPos, int stepType) throws TransformerException {
        super.init(compiler, opPos, stepType);
        switch (stepType) {
            case 22:
            case 23:
                break;
            case 24:
            case 25:
                this.m_mustHardReset = true;
                break;
            default:
                this.m_expr = compiler.compile(opPos + 2);
                this.m_expr.exprSetParent(this);
                return;
        }
        this.m_expr = compiler.compile(opPos);
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
    public void setRoot(int root) {
        super.setRoot(root);
        this.m_exprObj = FilterExprIteratorSimple.executeFilterExpr(root, this.m_lpi.getXPathContext(), this.m_lpi.getPrefixResolver(), this.m_lpi.getIsTopLevel(), this.m_lpi.m_stackFrame, this.m_expr);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        FilterExprWalker clone = (FilterExprWalker) super.clone();
        if (this.m_exprObj != null) {
            clone.m_exprObj = (XNodeSet) this.m_exprObj.clone();
        }
        return clone;
    }

    @Override
    public short acceptNode(int n) {
        try {
            if (getPredicateCount() > 0) {
                countProximityPosition(0);
                if (!executePredicates(n, this.m_lpi.getXPathContext())) {
                    return (short) 3;
                }
            }
            return (short) 1;
        } catch (TransformerException se) {
            throw new RuntimeException(se.getMessage());
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
    public int getLastPos(XPathContext xctxt) {
        return this.m_exprObj.getLength();
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
        return (this.m_expr == null || !(this.m_expr instanceof PathComponent)) ? WalkerFactory.BIT_FILTER : ((PathComponent) this.m_expr).getAnalysisBits();
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
        public void setExpression(Expression exp) {
            exp.exprSetParent(FilterExprWalker.this);
            FilterExprWalker.this.m_expr = exp;
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
        FilterExprWalker walker = (FilterExprWalker) expr;
        return this.m_expr.deepEquals(walker.m_expr);
    }
}
