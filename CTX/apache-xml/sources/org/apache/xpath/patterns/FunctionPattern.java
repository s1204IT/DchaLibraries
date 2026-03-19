package org.apache.xpath.patterns;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XObject;

public class FunctionPattern extends StepPattern {
    static final long serialVersionUID = -5426793413091209944L;
    Expression m_functionExpr;

    public FunctionPattern(Expression expression, int i, int i2) {
        super(0, null, null, i, i2);
        this.m_functionExpr = expression;
    }

    @Override
    public final void calcScore() {
        this.m_score = SCORE_OTHER;
        if (this.m_targetString == null) {
            calcTargetString();
        }
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        super.fixupVariables(vector, i);
        this.m_functionExpr.fixupVariables(vector, i);
    }

    @Override
    public XObject execute(XPathContext xPathContext, int i) throws TransformerException {
        DTMIterator dTMIteratorAsIterator = this.m_functionExpr.asIterator(xPathContext, i);
        XNumber xNumber = SCORE_NONE;
        if (dTMIteratorAsIterator != null) {
            do {
                int iNextNode = dTMIteratorAsIterator.nextNode();
                if (-1 == iNextNode) {
                    break;
                }
                xNumber = iNextNode == i ? SCORE_OTHER : SCORE_NONE;
            } while (xNumber != SCORE_OTHER);
        }
        dTMIteratorAsIterator.detach();
        return xNumber;
    }

    @Override
    public XObject execute(XPathContext xPathContext, int i, DTM dtm, int i2) throws TransformerException {
        DTMIterator dTMIteratorAsIterator = this.m_functionExpr.asIterator(xPathContext, i);
        XNumber xNumber = SCORE_NONE;
        if (dTMIteratorAsIterator != null) {
            do {
                int iNextNode = dTMIteratorAsIterator.nextNode();
                if (-1 == iNextNode) {
                    break;
                }
                xNumber = iNextNode == i ? SCORE_OTHER : SCORE_NONE;
            } while (xNumber != SCORE_OTHER);
            dTMIteratorAsIterator.detach();
        }
        return xNumber;
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        int currentNode = xPathContext.getCurrentNode();
        DTMIterator dTMIteratorAsIterator = this.m_functionExpr.asIterator(xPathContext, currentNode);
        XNumber xNumber = SCORE_NONE;
        if (dTMIteratorAsIterator != null) {
            do {
                int iNextNode = dTMIteratorAsIterator.nextNode();
                if (-1 == iNextNode) {
                    break;
                }
                xNumber = iNextNode == currentNode ? SCORE_OTHER : SCORE_NONE;
            } while (xNumber != SCORE_OTHER);
            dTMIteratorAsIterator.detach();
        }
        return xNumber;
    }

    class FunctionOwner implements ExpressionOwner {
        FunctionOwner() {
        }

        @Override
        public Expression getExpression() {
            return FunctionPattern.this.m_functionExpr;
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(FunctionPattern.this);
            FunctionPattern.this.m_functionExpr = expression;
        }
    }

    @Override
    protected void callSubtreeVisitors(XPathVisitor xPathVisitor) {
        this.m_functionExpr.callVisitors(new FunctionOwner(), xPathVisitor);
        super.callSubtreeVisitors(xPathVisitor);
    }
}
