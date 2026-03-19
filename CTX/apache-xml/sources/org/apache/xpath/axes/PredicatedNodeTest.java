package org.apache.xpath.axes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.WrappedRuntimeException;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.patterns.NodeTest;

public abstract class PredicatedNodeTest extends NodeTest implements SubContextList {
    static final boolean DEBUG_PREDICATECOUNTING = false;
    static final long serialVersionUID = -6193530757296377351L;
    protected LocPathIterator m_lpi;
    private Expression[] m_predicates;
    protected transient int[] m_proximityPositions;
    protected int m_predCount = -1;
    protected transient boolean m_foundLast = false;
    transient int m_predicateIndex = -1;

    public abstract int getLastPos(XPathContext xPathContext);

    PredicatedNodeTest(LocPathIterator locPathIterator) {
        this.m_lpi = locPathIterator;
    }

    PredicatedNodeTest() {
    }

    private void readObject(ObjectInputStream objectInputStream) throws TransformerException, IOException {
        try {
            objectInputStream.defaultReadObject();
            this.m_predicateIndex = -1;
            resetProximityPositions();
        } catch (ClassNotFoundException e) {
            throw new TransformerException(e);
        }
    }

    public Object clone() throws CloneNotSupportedException {
        PredicatedNodeTest predicatedNodeTest = (PredicatedNodeTest) super.clone();
        if (this.m_proximityPositions != null && this.m_proximityPositions == predicatedNodeTest.m_proximityPositions) {
            predicatedNodeTest.m_proximityPositions = new int[this.m_proximityPositions.length];
            System.arraycopy(this.m_proximityPositions, 0, predicatedNodeTest.m_proximityPositions, 0, this.m_proximityPositions.length);
        }
        if (predicatedNodeTest.m_lpi == this) {
            predicatedNodeTest.m_lpi = (LocPathIterator) predicatedNodeTest;
        }
        return predicatedNodeTest;
    }

    public int getPredicateCount() {
        if (-1 == this.m_predCount) {
            if (this.m_predicates == null) {
                return 0;
            }
            return this.m_predicates.length;
        }
        return this.m_predCount;
    }

    public void setPredicateCount(int i) {
        if (i > 0) {
            Expression[] expressionArr = new Expression[i];
            for (int i2 = 0; i2 < i; i2++) {
                expressionArr[i2] = this.m_predicates[i2];
            }
            this.m_predicates = expressionArr;
            return;
        }
        this.m_predicates = null;
    }

    protected void initPredicateInfo(Compiler compiler, int i) throws TransformerException {
        int firstPredicateOpPos = compiler.getFirstPredicateOpPos(i);
        if (firstPredicateOpPos > 0) {
            this.m_predicates = compiler.getCompiledPredicates(firstPredicateOpPos);
            if (this.m_predicates != null) {
                for (int i2 = 0; i2 < this.m_predicates.length; i2++) {
                    this.m_predicates[i2].exprSetParent(this);
                }
            }
        }
    }

    public Expression getPredicate(int i) {
        return this.m_predicates[i];
    }

    public int getProximityPosition() {
        return getProximityPosition(this.m_predicateIndex);
    }

    @Override
    public int getProximityPosition(XPathContext xPathContext) {
        return getProximityPosition();
    }

    protected int getProximityPosition(int i) {
        if (i >= 0) {
            return this.m_proximityPositions[i];
        }
        return 0;
    }

    public void resetProximityPositions() {
        int predicateCount = getPredicateCount();
        if (predicateCount > 0) {
            if (this.m_proximityPositions == null) {
                this.m_proximityPositions = new int[predicateCount];
            }
            for (int i = 0; i < predicateCount; i++) {
                try {
                    initProximityPosition(i);
                } catch (Exception e) {
                    throw new WrappedRuntimeException(e);
                }
            }
        }
    }

    public void initProximityPosition(int i) throws TransformerException {
        this.m_proximityPositions[i] = 0;
    }

    protected void countProximityPosition(int i) {
        int[] iArr = this.m_proximityPositions;
        if (iArr != null && i < iArr.length) {
            iArr[i] = iArr[i] + 1;
        }
    }

    public boolean isReverseAxes() {
        return false;
    }

    public int getPredicateIndex() {
        return this.m_predicateIndex;
    }

    boolean executePredicates(int i, XPathContext xPathContext) throws TransformerException {
        int predicateCount = getPredicateCount();
        if (predicateCount == 0) {
            return true;
        }
        xPathContext.getNamespaceContext();
        try {
            this.m_predicateIndex = 0;
            xPathContext.pushSubContextList(this);
            xPathContext.pushNamespaceContext(this.m_lpi.getPrefixResolver());
            xPathContext.pushCurrentNode(i);
            for (int i2 = 0; i2 < predicateCount; i2++) {
                XObject xObjectExecute = this.m_predicates[i2].execute(xPathContext);
                if (2 == xObjectExecute.getType()) {
                    if (getProximityPosition(this.m_predicateIndex) != ((int) xObjectExecute.num())) {
                        return false;
                    }
                    if (this.m_predicates[i2].isStableNumber() && i2 == predicateCount - 1) {
                        this.m_foundLast = true;
                    }
                } else if (!xObjectExecute.bool()) {
                    return false;
                }
                int i3 = this.m_predicateIndex + 1;
                this.m_predicateIndex = i3;
                countProximityPosition(i3);
            }
            return true;
        } finally {
            xPathContext.popCurrentNode();
            xPathContext.popNamespaceContext();
            xPathContext.popSubContextList();
            this.m_predicateIndex = -1;
        }
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        super.fixupVariables(vector, i);
        int predicateCount = getPredicateCount();
        for (int i2 = 0; i2 < predicateCount; i2++) {
            this.m_predicates[i2].fixupVariables(vector, i);
        }
    }

    protected String nodeToString(int i) {
        if (-1 != i) {
            return this.m_lpi.getXPathContext().getDTM(i).getNodeName(i) + "{" + (i + 1) + "}";
        }
        return "null";
    }

    public short acceptNode(int i) {
        XPathContext xPathContext = this.m_lpi.getXPathContext();
        try {
            try {
                xPathContext.pushCurrentNode(i);
                if (execute(xPathContext, i) == NodeTest.SCORE_NONE) {
                    return (short) 3;
                }
                if (getPredicateCount() > 0) {
                    countProximityPosition(0);
                    if (!executePredicates(i, xPathContext)) {
                        return (short) 3;
                    }
                }
                return (short) 1;
            } catch (TransformerException e) {
                throw new RuntimeException(e.getMessage());
            }
        } finally {
            xPathContext.popCurrentNode();
        }
    }

    public LocPathIterator getLocPathIterator() {
        return this.m_lpi;
    }

    public void setLocPathIterator(LocPathIterator locPathIterator) {
        this.m_lpi = locPathIterator;
        if (this != locPathIterator) {
            locPathIterator.exprSetParent(this);
        }
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

    public void callPredicateVisitors(XPathVisitor xPathVisitor) {
        if (this.m_predicates != null) {
            int length = this.m_predicates.length;
            for (int i = 0; i < length; i++) {
                PredOwner predOwner = new PredOwner(i);
                if (xPathVisitor.visitPredicate(predOwner, this.m_predicates[i])) {
                    this.m_predicates[i].callVisitors(predOwner, xPathVisitor);
                }
            }
        }
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!super.deepEquals(expression)) {
            return false;
        }
        PredicatedNodeTest predicatedNodeTest = (PredicatedNodeTest) expression;
        if (this.m_predicates == null) {
            return predicatedNodeTest.m_predicates == null;
        }
        int length = this.m_predicates.length;
        if (predicatedNodeTest.m_predicates == null || predicatedNodeTest.m_predicates.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!this.m_predicates[i].deepEquals(predicatedNodeTest.m_predicates[i])) {
                return false;
            }
        }
        return true;
    }

    class PredOwner implements ExpressionOwner {
        int m_index;

        PredOwner(int i) {
            this.m_index = i;
        }

        @Override
        public Expression getExpression() {
            return PredicatedNodeTest.this.m_predicates[this.m_index];
        }

        @Override
        public void setExpression(Expression expression) {
            expression.exprSetParent(PredicatedNodeTest.this);
            PredicatedNodeTest.this.m_predicates[this.m_index] = expression;
        }
    }
}
