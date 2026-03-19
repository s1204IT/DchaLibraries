package org.apache.xpath.axes;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.objects.XNodeSet;

public class FilterExprIteratorSimple extends LocPathIterator {
    static final long serialVersionUID = -6978977187025375579L;
    private boolean m_canDetachNodeset;
    private Expression m_expr;
    private transient XNodeSet m_exprObj;
    private boolean m_mustHardReset;

    public FilterExprIteratorSimple() {
        super(null);
        this.m_mustHardReset = false;
        this.m_canDetachNodeset = true;
    }

    public FilterExprIteratorSimple(Expression expression) {
        super(null);
        this.m_mustHardReset = false;
        this.m_canDetachNodeset = true;
        this.m_expr = expression;
    }

    @Override
    public void setRoot(int i, Object obj) {
        super.setRoot(i, obj);
        this.m_exprObj = executeFilterExpr(i, this.m_execContext, getPrefixResolver(), getIsTopLevel(), this.m_stackFrame, this.m_expr);
    }

    public static XNodeSet executeFilterExpr(int i, XPathContext xPathContext, PrefixResolver prefixResolver, boolean z, int i2, Expression expression) throws WrappedRuntimeException {
        XNodeSet xNodeSet;
        PrefixResolver namespaceContext = xPathContext.getNamespaceContext();
        try {
            try {
                xPathContext.pushCurrentNode(i);
                xPathContext.setNamespaceContext(prefixResolver);
                if (z) {
                    VariableStack varStack = xPathContext.getVarStack();
                    int stackFrame = varStack.getStackFrame();
                    varStack.setStackFrame(i2);
                    xNodeSet = (XNodeSet) expression.execute(xPathContext);
                    xNodeSet.setShouldCacheNodes(true);
                    varStack.setStackFrame(stackFrame);
                } else {
                    xNodeSet = (XNodeSet) expression.execute(xPathContext);
                }
                return xNodeSet;
            } catch (TransformerException e) {
                throw new WrappedRuntimeException(e);
            }
        } finally {
            xPathContext.popCurrentNode();
            xPathContext.setNamespaceContext(namespaceContext);
        }
    }

    @Override
    public int nextNode() {
        int iNextNode;
        if (this.m_foundLast) {
            return -1;
        }
        if (this.m_exprObj != null) {
            iNextNode = this.m_exprObj.nextNode();
            this.m_lastFetched = iNextNode;
        } else {
            this.m_lastFetched = -1;
            iNextNode = -1;
        }
        if (-1 != iNextNode) {
            this.m_pos++;
            return iNextNode;
        }
        this.m_foundLast = true;
        return -1;
    }

    @Override
    public void detach() {
        if (this.m_allowDetach) {
            super.detach();
            this.m_exprObj.detach();
            this.m_exprObj = null;
        }
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

    class filterExprOwner implements ExpressionOwner {
        filterExprOwner() {
        }

        @Override
        public Expression getExpression() {
            return FilterExprIteratorSimple.this.m_expr;
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(FilterExprIteratorSimple.this);
            FilterExprIteratorSimple.this.m_expr = expression;
        }
    }

    @Override
    public void callPredicateVisitors(XPathVisitor xPathVisitor) {
        this.m_expr.callVisitors(new filterExprOwner(), xPathVisitor);
        super.callPredicateVisitors(xPathVisitor);
    }

    @Override
    public boolean deepEquals(Expression expression) {
        return super.deepEquals(expression) && this.m_expr.deepEquals(((FilterExprIteratorSimple) expression).m_expr);
    }

    @Override
    public int getAxis() {
        if (this.m_exprObj != null) {
            return this.m_exprObj.getAxis();
        }
        return 20;
    }
}
