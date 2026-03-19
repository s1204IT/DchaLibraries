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

    OneStepIterator(Compiler compiler, int i, int i2) throws TransformerException {
        super(compiler, i, i2);
        this.m_axis = -1;
        this.m_axis = WalkerFactory.getAxisFromStep(compiler, OpMap.getFirstChildPos(i));
    }

    public OneStepIterator(DTMAxisIterator dTMAxisIterator, int i) throws TransformerException {
        super(null);
        this.m_axis = -1;
        this.m_iterator = dTMAxisIterator;
        this.m_axis = i;
        initNodeTest(-1);
    }

    @Override
    public void setRoot(int i, Object obj) {
        super.setRoot(i, obj);
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
        OneStepIterator oneStepIterator = (OneStepIterator) super.clone();
        if (this.m_iterator != null) {
            oneStepIterator.m_iterator = this.m_iterator.cloneIterator();
        }
        return oneStepIterator;
    }

    @Override
    public DTMIterator cloneWithReset() throws CloneNotSupportedException {
        OneStepIterator oneStepIterator = (OneStepIterator) super.cloneWithReset();
        oneStepIterator.m_iterator = this.m_iterator;
        return oneStepIterator;
    }

    @Override
    public boolean isReverseAxes() {
        return this.m_iterator.isReverse();
    }

    @Override
    protected int getProximityPosition(int i) {
        if (!isReverseAxes()) {
            return super.getProximityPosition(i);
        }
        if (i < 0) {
            return -1;
        }
        if (this.m_proximityPositions[i] <= 0) {
            XPathContext xPathContext = getXPathContext();
            try {
                OneStepIterator oneStepIterator = (OneStepIterator) clone();
                int root = getRoot();
                xPathContext.pushCurrentNode(root);
                oneStepIterator.setRoot(root, xPathContext);
                oneStepIterator.m_predCount = i;
                int i2 = 1;
                while (-1 != oneStepIterator.nextNode()) {
                    i2++;
                }
                int[] iArr = this.m_proximityPositions;
                iArr[i] = iArr[i] + i2;
            } catch (CloneNotSupportedException e) {
            } catch (Throwable th) {
                xPathContext.popCurrentNode();
                throw th;
            }
            xPathContext.popCurrentNode();
        }
        return this.m_proximityPositions[i];
    }

    @Override
    public int getLength() {
        if (!isReverseAxes()) {
            return super.getLength();
        }
        int i = 0;
        boolean z = this == this.m_execContext.getSubContextList();
        getPredicateCount();
        if (-1 != this.m_length && z && this.m_predicateIndex < 1) {
            return this.m_length;
        }
        XPathContext xPathContext = getXPathContext();
        try {
            OneStepIterator oneStepIterator = (OneStepIterator) cloneWithReset();
            int root = getRoot();
            xPathContext.pushCurrentNode(root);
            oneStepIterator.setRoot(root, xPathContext);
            oneStepIterator.m_predCount = this.m_predicateIndex;
            while (-1 != oneStepIterator.nextNode()) {
                i++;
            }
        } catch (CloneNotSupportedException e) {
        } catch (Throwable th) {
            xPathContext.popCurrentNode();
            throw th;
        }
        xPathContext.popCurrentNode();
        if (z && this.m_predicateIndex < 1) {
            this.m_length = i;
        }
        return i;
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
    public boolean deepEquals(Expression expression) {
        return super.deepEquals(expression) && this.m_axis == ((OneStepIterator) expression).m_axis;
    }
}
