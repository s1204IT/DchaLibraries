package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xpath.Expression;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.OpMap;

public class OneStepIterator extends ChildTestIterator {
    static final long serialVersionUID = 4623710779664998283L;
    protected int m_axis;
    protected DTMAxisIterator m_iterator;

    OneStepIterator(Compiler compiler, int opPos, int analysis) throws TransformerException {
        super(compiler, opPos, analysis);
        this.m_axis = -1;
        int firstStepPos = OpMap.getFirstChildPos(opPos);
        this.m_axis = WalkerFactory.getAxisFromStep(compiler, firstStepPos);
    }

    public OneStepIterator(DTMAxisIterator iterator, int axis) throws TransformerException {
        super(null);
        this.m_axis = -1;
        this.m_iterator = iterator;
        this.m_axis = axis;
        initNodeTest(-1);
    }

    @Override
    public void setRoot(int context, Object environment) {
        super.setRoot(context, environment);
        if (this.m_axis > -1) {
            this.m_iterator = this.m_cdtm.getAxisIterator(this.m_axis);
        }
        this.m_iterator.setStartNode(this.m_context);
    }

    @Override
    public void detach() {
        if (this.m_allowDetach) {
            if (this.m_axis > -1) {
                this.m_iterator = null;
            }
            super.detach();
        }
    }

    @Override
    protected int getNextNode() {
        int next = this.m_iterator.next();
        this.m_lastFetched = next;
        return next;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        OneStepIterator clone = (OneStepIterator) super.clone();
        if (this.m_iterator != null) {
            clone.m_iterator = this.m_iterator.cloneIterator();
        }
        return clone;
    }

    @Override
    public DTMIterator cloneWithReset() throws CloneNotSupportedException {
        OneStepIterator clone = (OneStepIterator) super.cloneWithReset();
        clone.m_iterator = this.m_iterator;
        return clone;
    }

    @Override
    public boolean isReverseAxes() {
        return this.m_iterator.isReverse();
    }

    @Override
    protected int getProximityPosition(int predicateIndex) {
        if (!isReverseAxes()) {
            return super.getProximityPosition(predicateIndex);
        }
        if (predicateIndex < 0) {
            return -1;
        }
        if (this.m_proximityPositions[predicateIndex] <= 0) {
            XPathContext xctxt = getXPathContext();
            try {
                OneStepIterator clone = (OneStepIterator) clone();
                int root = getRoot();
                xctxt.pushCurrentNode(root);
                clone.setRoot(root, xctxt);
                clone.m_predCount = predicateIndex;
                int count = 1;
                while (-1 != next) {
                    count++;
                }
                int[] iArr = this.m_proximityPositions;
                iArr[predicateIndex] = iArr[predicateIndex] + count;
            } catch (CloneNotSupportedException e) {
            } finally {
                xctxt.popCurrentNode();
            }
        }
        return this.m_proximityPositions[predicateIndex];
    }

    @Override
    public int getLength() {
        if (!isReverseAxes()) {
            return super.getLength();
        }
        boolean isPredicateTest = this == this.m_execContext.getSubContextList();
        getPredicateCount();
        if (-1 != this.m_length && isPredicateTest && this.m_predicateIndex < 1) {
            return this.m_length;
        }
        int count = 0;
        XPathContext xctxt = getXPathContext();
        try {
            OneStepIterator clone = (OneStepIterator) cloneWithReset();
            int root = getRoot();
            xctxt.pushCurrentNode(root);
            clone.setRoot(root, xctxt);
            clone.m_predCount = this.m_predicateIndex;
            while (-1 != next) {
                count++;
            }
        } catch (CloneNotSupportedException e) {
        } finally {
            xctxt.popCurrentNode();
        }
        if (isPredicateTest && this.m_predicateIndex < 1) {
            this.m_length = count;
            return count;
        }
        return count;
    }

    @Override
    protected void countProximityPosition(int i) {
        if (!isReverseAxes()) {
            super.countProximityPosition(i);
        } else if (i < this.m_proximityPositions.length) {
            this.m_proximityPositions[i] = r0[i] - 1;
        }
    }

    @Override
    public void reset() {
        super.reset();
        if (this.m_iterator != null) {
            this.m_iterator.reset();
        }
    }

    @Override
    public int getAxis() {
        return this.m_axis;
    }

    @Override
    public boolean deepEquals(Expression expr) {
        return super.deepEquals(expr) && this.m_axis == ((OneStepIterator) expr).m_axis;
    }
}
