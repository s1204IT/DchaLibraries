package org.apache.xpath.patterns;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.objects.XObject;

public class UnionPattern extends Expression {
    static final long serialVersionUID = -6670449967116905820L;
    private StepPattern[] m_patterns;

    @Override
    public void fixupVariables(Vector vector, int i) {
        for (int i2 = 0; i2 < this.m_patterns.length; i2++) {
            this.m_patterns[i2].fixupVariables(vector, i);
        }
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        if (this.m_patterns != null) {
            int length = this.m_patterns.length;
            for (int i = 0; i < length; i++) {
                if (this.m_patterns[i].canTraverseOutsideSubtree()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setPatterns(StepPattern[] stepPatternArr) {
        this.m_patterns = stepPatternArr;
        if (stepPatternArr != null) {
            for (StepPattern stepPattern : stepPatternArr) {
                stepPattern.exprSetParent(this);
            }
        }
    }

    public StepPattern[] getPatterns() {
        return this.m_patterns;
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        int length = this.m_patterns.length;
        XObject xObject = null;
        for (int i = 0; i < length; i++) {
            XObject xObjectExecute = this.m_patterns[i].execute(xPathContext);
            if (xObjectExecute != NodeTest.SCORE_NONE && (xObject == null || xObjectExecute.num() > xObject.num())) {
                xObject = xObjectExecute;
            }
        }
        if (xObject == null) {
            return NodeTest.SCORE_NONE;
        }
        return xObject;
    }

    class UnionPathPartOwner implements ExpressionOwner {
        int m_index;

        UnionPathPartOwner(int i) {
            this.m_index = i;
        }

        @Override
        public Expression getExpression() {
            return UnionPattern.this.m_patterns[this.m_index];
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(UnionPattern.this);
            UnionPattern.this.m_patterns[this.m_index] = (StepPattern) expression;
        }
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        xPathVisitor.visitUnionPattern(expressionOwner, this);
        if (this.m_patterns != null) {
            int length = this.m_patterns.length;
            for (int i = 0; i < length; i++) {
                this.m_patterns[i].callVisitors(new UnionPathPartOwner(i), xPathVisitor);
            }
        }
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!isSameClass(expression)) {
            return false;
        }
        UnionPattern unionPattern = (UnionPattern) expression;
        if (this.m_patterns == null) {
            return unionPattern.m_patterns == null;
        }
        int length = this.m_patterns.length;
        if (unionPattern.m_patterns == null || unionPattern.m_patterns.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!this.m_patterns[i].deepEquals(unionPattern.m_patterns[i])) {
                return false;
            }
        }
        return true;
    }
}
