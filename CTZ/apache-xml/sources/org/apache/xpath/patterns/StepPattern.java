package org.apache.xpath.patterns;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.Axis;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.axes.SubContextList;
import org.apache.xpath.compiler.PsuedoNames;
import org.apache.xpath.objects.XObject;

public class StepPattern extends NodeTest implements SubContextList, ExpressionOwner {
    private static final boolean DEBUG_MATCHES = false;
    static final long serialVersionUID = 9071668960168152644L;
    protected int m_axis;
    Expression[] m_predicates;
    StepPattern m_relativePathPattern;
    String m_targetString;

    public StepPattern(int i, String str, String str2, int i2, int i3) {
        super(i, str, str2);
        this.m_axis = i2;
    }

    public StepPattern(int i, int i2, int i3) {
        super(i);
        this.m_axis = i2;
    }

    public void calcTargetString() {
        int whatToShow = getWhatToShow();
        if (whatToShow == -1) {
            this.m_targetString = "*";
            return;
        }
        if (whatToShow == 1) {
            if ("*" == this.m_name) {
                this.m_targetString = "*";
                return;
            } else {
                this.m_targetString = this.m_name;
                return;
            }
        }
        if (whatToShow == 4 || whatToShow == 8 || whatToShow == 12) {
            this.m_targetString = PsuedoNames.PSEUDONAME_TEXT;
            return;
        }
        if (whatToShow == 128) {
            this.m_targetString = PsuedoNames.PSEUDONAME_COMMENT;
        } else if (whatToShow == 256 || whatToShow == 1280) {
            this.m_targetString = PsuedoNames.PSEUDONAME_ROOT;
        } else {
            this.m_targetString = "*";
        }
    }

    public String getTargetString() {
        return this.m_targetString;
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        super.fixupVariables(vector, i);
        if (this.m_predicates != null) {
            for (int i2 = 0; i2 < this.m_predicates.length; i2++) {
                this.m_predicates[i2].fixupVariables(vector, i);
            }
        }
        if (this.m_relativePathPattern != null) {
            this.m_relativePathPattern.fixupVariables(vector, i);
        }
    }

    public void setRelativePathPattern(StepPattern stepPattern) {
        this.m_relativePathPattern = stepPattern;
        stepPattern.exprSetParent(this);
        calcScore();
    }

    public StepPattern getRelativePathPattern() {
        return this.m_relativePathPattern;
    }

    public Expression[] getPredicates() {
        return this.m_predicates;
    }

    @Override
    public boolean canTraverseOutsideSubtree() {
        int predicateCount = getPredicateCount();
        for (int i = 0; i < predicateCount; i++) {
            if (getPredicate(i).canTraverseOutsideSubtree()) {
                return true;
            }
        }
        return false;
    }

    public Expression getPredicate(int i) {
        return this.m_predicates[i];
    }

    public final int getPredicateCount() {
        if (this.m_predicates == null) {
            return 0;
        }
        return this.m_predicates.length;
    }

    public void setPredicates(Expression[] expressionArr) {
        this.m_predicates = expressionArr;
        if (expressionArr != null) {
            for (Expression expression : expressionArr) {
                expression.exprSetParent(this);
            }
        }
        calcScore();
    }

    @Override
    public void calcScore() {
        if (getPredicateCount() > 0 || this.m_relativePathPattern != null) {
            this.m_score = SCORE_OTHER;
        } else {
            super.calcScore();
        }
        if (this.m_targetString == null) {
            calcTargetString();
        }
    }

    @Override
    public XObject execute(XPathContext xPathContext, int i) throws TransformerException {
        DTM dtm = xPathContext.getDTM(i);
        if (dtm != null) {
            return execute(xPathContext, i, dtm, dtm.getExpandedTypeID(i));
        }
        return NodeTest.SCORE_NONE;
    }

    @Override
    public XObject execute(XPathContext xPathContext) throws TransformerException {
        return execute(xPathContext, xPathContext.getCurrentNode());
    }

    @Override
    public XObject execute(XPathContext xPathContext, int i, DTM dtm, int i2) throws TransformerException {
        if (this.m_whatToShow == 65536) {
            if (this.m_relativePathPattern != null) {
                return this.m_relativePathPattern.execute(xPathContext);
            }
            return NodeTest.SCORE_NONE;
        }
        XObject xObjectExecute = super.execute(xPathContext, i, dtm, i2);
        if (xObjectExecute == NodeTest.SCORE_NONE) {
            return NodeTest.SCORE_NONE;
        }
        if (getPredicateCount() != 0 && !executePredicates(xPathContext, dtm, i)) {
            return NodeTest.SCORE_NONE;
        }
        if (this.m_relativePathPattern != null) {
            return this.m_relativePathPattern.executeRelativePathPattern(xPathContext, dtm, i);
        }
        return xObjectExecute;
    }

    private final boolean checkProximityPosition(XPathContext xPathContext, int i, DTM dtm, int i2, int i3) {
        boolean z;
        try {
            DTMAxisTraverser axisTraverser = dtm.getAxisTraverser(12);
            int iFirst = axisTraverser.first(i2);
            loop0: while (-1 != iFirst) {
                try {
                    xPathContext.pushCurrentNode(iFirst);
                    if (NodeTest.SCORE_NONE != super.execute(xPathContext, iFirst)) {
                        try {
                            xPathContext.pushSubContextList(this);
                            int i4 = 0;
                            while (true) {
                                if (i4 < i) {
                                    xPathContext.pushPredicatePos(i4);
                                    try {
                                        XObject xObjectExecute = this.m_predicates[i4].execute(xPathContext);
                                        try {
                                            if (2 == xObjectExecute.getType()) {
                                                break loop0;
                                            }
                                            if (!xObjectExecute.boolWithSideEffects()) {
                                                xObjectExecute.detach();
                                                xPathContext.popPredicatePos();
                                                z = false;
                                                break;
                                            }
                                            xObjectExecute.detach();
                                            xPathContext.popPredicatePos();
                                            i4++;
                                        } catch (Throwable th) {
                                            xObjectExecute.detach();
                                            throw th;
                                        }
                                    } catch (Throwable th2) {
                                        xPathContext.popPredicatePos();
                                        throw th2;
                                    }
                                    xPathContext.popPredicatePos();
                                    throw th2;
                                }
                                z = true;
                                break;
                            }
                            if (z) {
                                i3--;
                            }
                            if (i3 < 1) {
                                return false;
                            }
                        } finally {
                            xPathContext.popSubContextList();
                        }
                    }
                    xPathContext.popCurrentNode();
                    iFirst = axisTraverser.next(i2, iFirst);
                } finally {
                    xPathContext.popCurrentNode();
                }
            }
            return i3 == 1;
        } catch (TransformerException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private final int getProximityPosition(XPathContext xPathContext, int i, boolean z) {
        boolean z2;
        int currentNode = xPathContext.getCurrentNode();
        DTM dtm = xPathContext.getDTM(currentNode);
        int parent = dtm.getParent(currentNode);
        try {
            DTMAxisTraverser axisTraverser = dtm.getAxisTraverser(3);
            int i2 = 0;
            for (int iFirst = axisTraverser.first(parent); -1 != iFirst; iFirst = axisTraverser.next(parent, iFirst)) {
                try {
                    xPathContext.pushCurrentNode(iFirst);
                    if (NodeTest.SCORE_NONE != super.execute(xPathContext, iFirst)) {
                        try {
                            xPathContext.pushSubContextList(this);
                            for (int i3 = 0; i3 < i; i3++) {
                                xPathContext.pushPredicatePos(i3);
                                try {
                                    XObject xObjectExecute = this.m_predicates[i3].execute(xPathContext);
                                    try {
                                        if (2 == xObjectExecute.getType()) {
                                            if (i2 + 1 != ((int) xObjectExecute.numWithSideEffects())) {
                                                z2 = false;
                                                break;
                                            }
                                        } else {
                                            if (!xObjectExecute.boolWithSideEffects()) {
                                                xObjectExecute.detach();
                                                z2 = false;
                                                break;
                                            }
                                        }
                                    } finally {
                                    }
                                } finally {
                                    xPathContext.popPredicatePos();
                                }
                            }
                            z2 = true;
                            if (z2) {
                                i2++;
                            }
                            if (!z && iFirst == currentNode) {
                                return i2;
                            }
                        } finally {
                            xPathContext.popSubContextList();
                        }
                    }
                    xPathContext.popCurrentNode();
                } finally {
                    xPathContext.popCurrentNode();
                }
            }
            return i2;
        } catch (TransformerException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public int getProximityPosition(XPathContext xPathContext) {
        return getProximityPosition(xPathContext, xPathContext.getPredicatePos(), false);
    }

    @Override
    public int getLastPos(XPathContext xPathContext) {
        return getProximityPosition(xPathContext, xPathContext.getPredicatePos(), true);
    }

    protected final XObject executeRelativePathPattern(XPathContext xPathContext, DTM dtm, int i) throws TransformerException {
        XObject xObjectExecute = NodeTest.SCORE_NONE;
        DTMAxisTraverser axisTraverser = dtm.getAxisTraverser(this.m_axis);
        int iFirst = axisTraverser.first(i);
        while (true) {
            if (-1 == iFirst) {
                break;
            }
            try {
                xPathContext.pushCurrentNode(iFirst);
                xObjectExecute = execute(xPathContext);
                if (xObjectExecute != NodeTest.SCORE_NONE) {
                    break;
                }
                xPathContext.popCurrentNode();
                iFirst = axisTraverser.next(i, iFirst);
            } finally {
                xPathContext.popCurrentNode();
            }
        }
        return xObjectExecute;
    }

    protected final boolean executePredicates(XPathContext xPathContext, DTM dtm, int i) throws TransformerException {
        int predicateCount = getPredicateCount();
        try {
            xPathContext.pushSubContextList(this);
            boolean z = false;
            for (int i2 = 0; i2 < predicateCount; i2++) {
                xPathContext.pushPredicatePos(i2);
                try {
                    XObject xObjectExecute = this.m_predicates[i2].execute(xPathContext);
                    try {
                        if (2 == xObjectExecute.getType()) {
                            int iNum = (int) xObjectExecute.num();
                            if (z) {
                                z = iNum == 1;
                            } else if (checkProximityPosition(xPathContext, i2, dtm, i, iNum)) {
                                z = true;
                            } else {
                                xObjectExecute.detach();
                            }
                            break;
                        }
                        if (!xObjectExecute.boolWithSideEffects()) {
                            xObjectExecute.detach();
                            break;
                        }
                    } finally {
                        xObjectExecute.detach();
                    }
                } finally {
                    xPathContext.popPredicatePos();
                }
            }
            z = true;
            return z;
        } finally {
            xPathContext.popSubContextList();
        }
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (StepPattern stepPattern = this; stepPattern != null; stepPattern = stepPattern.m_relativePathPattern) {
            if (stepPattern != this) {
                stringBuffer.append(PsuedoNames.PSEUDONAME_ROOT);
            }
            stringBuffer.append(Axis.getNames(stepPattern.m_axis));
            stringBuffer.append("::");
            if (20480 == stepPattern.m_whatToShow) {
                stringBuffer.append("doc()");
            } else if (65536 == stepPattern.m_whatToShow) {
                stringBuffer.append("function()");
            } else if (-1 == stepPattern.m_whatToShow) {
                stringBuffer.append("node()");
            } else if (4 == stepPattern.m_whatToShow) {
                stringBuffer.append("text()");
            } else if (64 == stepPattern.m_whatToShow) {
                stringBuffer.append("processing-instruction(");
                if (stepPattern.m_name != null) {
                    stringBuffer.append(stepPattern.m_name);
                }
                stringBuffer.append(")");
            } else if (128 == stepPattern.m_whatToShow) {
                stringBuffer.append("comment()");
            } else if (stepPattern.m_name != null) {
                if (2 == stepPattern.m_whatToShow) {
                    stringBuffer.append("@");
                }
                if (stepPattern.m_namespace != null) {
                    stringBuffer.append("{");
                    stringBuffer.append(stepPattern.m_namespace);
                    stringBuffer.append("}");
                }
                stringBuffer.append(stepPattern.m_name);
            } else if (2 == stepPattern.m_whatToShow) {
                stringBuffer.append("@");
            } else if (1280 == stepPattern.m_whatToShow) {
                stringBuffer.append("doc-root()");
            } else {
                stringBuffer.append("?" + Integer.toHexString(stepPattern.m_whatToShow));
            }
            if (stepPattern.m_predicates != null) {
                for (int i = 0; i < stepPattern.m_predicates.length; i++) {
                    stringBuffer.append("[");
                    stringBuffer.append(stepPattern.m_predicates[i]);
                    stringBuffer.append("]");
                }
            }
        }
        return stringBuffer.toString();
    }

    public double getMatchScore(XPathContext xPathContext, int i) throws TransformerException {
        xPathContext.pushCurrentNode(i);
        xPathContext.pushCurrentExpressionNode(i);
        try {
            return execute(xPathContext).num();
        } finally {
            xPathContext.popCurrentNode();
            xPathContext.popCurrentExpressionNode();
        }
    }

    public void setAxis(int i) {
        this.m_axis = i;
    }

    public int getAxis() {
        return this.m_axis;
    }

    class PredOwner implements ExpressionOwner {
        int m_index;

        PredOwner(int i) {
            this.m_index = i;
        }

        @Override
        public Expression getExpression() {
            return StepPattern.this.m_predicates[this.m_index];
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(StepPattern.this);
            StepPattern.this.m_predicates[this.m_index] = expression;
        }
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        if (xPathVisitor.visitMatchPattern(expressionOwner, this)) {
            callSubtreeVisitors(xPathVisitor);
        }
    }

    protected void callSubtreeVisitors(XPathVisitor xPathVisitor) {
        if (this.m_predicates != null) {
            int length = this.m_predicates.length;
            for (int i = 0; i < length; i++) {
                PredOwner predOwner = new PredOwner(i);
                if (xPathVisitor.visitPredicate(predOwner, this.m_predicates[i])) {
                    this.m_predicates[i].callVisitors(predOwner, xPathVisitor);
                }
            }
        }
        if (this.m_relativePathPattern != null) {
            this.m_relativePathPattern.callVisitors(this, xPathVisitor);
        }
    }

    @Override
    public Expression getExpression() {
        return this.m_relativePathPattern;
    }

    @Override
    public void setExpression(Expression expression) {
        expression.exprSetParent(this);
        this.m_relativePathPattern = (StepPattern) expression;
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!super.deepEquals(expression)) {
            return false;
        }
        StepPattern stepPattern = (StepPattern) expression;
        if (this.m_predicates != null) {
            int length = this.m_predicates.length;
            if (stepPattern.m_predicates == null || stepPattern.m_predicates.length != length) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (!this.m_predicates[i].deepEquals(stepPattern.m_predicates[i])) {
                    return false;
                }
            }
        } else if (stepPattern.m_predicates != null) {
            return false;
        }
        return this.m_relativePathPattern != null ? this.m_relativePathPattern.deepEquals(stepPattern.m_relativePathPattern) : stepPattern.m_relativePathPattern == null;
    }
}
