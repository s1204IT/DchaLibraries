package org.apache.xpath.axes;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.XPathContext;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.res.XPATHErrorResources;

public class AxesWalker extends PredicatedNodeTest implements Cloneable, PathComponent, ExpressionOwner {
    static final long serialVersionUID = -2966031951306601247L;
    protected int m_axis;
    private transient int m_currentNode;
    private DTM m_dtm;
    transient boolean m_isFresh;
    protected AxesWalker m_nextWalker;
    AxesWalker m_prevWalker;
    transient int m_root;
    protected DTMAxisTraverser m_traverser;

    public AxesWalker(LocPathIterator locPathIterator, int i) {
        super(locPathIterator);
        this.m_root = -1;
        this.m_currentNode = -1;
        this.m_axis = -1;
        this.m_axis = i;
    }

    public final WalkingIterator wi() {
        return (WalkingIterator) this.m_lpi;
    }

    public void init(Compiler compiler, int i, int i2) throws TransformerException {
        initPredicateInfo(compiler, i);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (AxesWalker) super.clone();
    }

    AxesWalker cloneDeep(WalkingIterator walkingIterator, Vector vector) throws CloneNotSupportedException {
        AxesWalker axesWalkerFindClone = findClone(this, vector);
        if (axesWalkerFindClone != null) {
            return axesWalkerFindClone;
        }
        AxesWalker axesWalker = (AxesWalker) clone();
        axesWalker.setLocPathIterator(walkingIterator);
        if (vector != null) {
            vector.addElement(this);
            vector.addElement(axesWalker);
        }
        if (wi().m_lastUsedWalker == this) {
            walkingIterator.m_lastUsedWalker = axesWalker;
        }
        if (this.m_nextWalker != null) {
            axesWalker.m_nextWalker = this.m_nextWalker.cloneDeep(walkingIterator, vector);
        }
        if (vector != null) {
            if (this.m_prevWalker != null) {
                axesWalker.m_prevWalker = this.m_prevWalker.cloneDeep(walkingIterator, vector);
            }
        } else if (this.m_nextWalker != null) {
            axesWalker.m_nextWalker.m_prevWalker = axesWalker;
        }
        return axesWalker;
    }

    static AxesWalker findClone(AxesWalker axesWalker, Vector vector) {
        if (vector != null) {
            int size = vector.size();
            for (int i = 0; i < size; i += 2) {
                if (axesWalker == vector.elementAt(i)) {
                    return (AxesWalker) vector.elementAt(i + 1);
                }
            }
            return null;
        }
        return null;
    }

    public void detach() {
        this.m_currentNode = -1;
        this.m_dtm = null;
        this.m_traverser = null;
        this.m_isFresh = true;
        this.m_root = -1;
    }

    public int getRoot() {
        return this.m_root;
    }

    @Override
    public int getAnalysisBits() {
        return WalkerFactory.getAnalysisBitFromAxes(getAxis());
    }

    public void setRoot(int i) {
        this.m_dtm = wi().getXPathContext().getDTM(i);
        this.m_traverser = this.m_dtm.getAxisTraverser(this.m_axis);
        this.m_isFresh = true;
        this.m_foundLast = false;
        this.m_root = i;
        this.m_currentNode = i;
        if (-1 == i) {
            throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_SETTING_WALKER_ROOT_TO_NULL, null));
        }
        resetProximityPositions();
    }

    public final int getCurrentNode() {
        return this.m_currentNode;
    }

    public void setNextWalker(AxesWalker axesWalker) {
        this.m_nextWalker = axesWalker;
    }

    public AxesWalker getNextWalker() {
        return this.m_nextWalker;
    }

    public void setPrevWalker(AxesWalker axesWalker) {
        this.m_prevWalker = axesWalker;
    }

    public AxesWalker getPrevWalker() {
        return this.m_prevWalker;
    }

    private int returnNextNode(int i) {
        return i;
    }

    protected int getNextNode() {
        if (this.m_foundLast) {
            return -1;
        }
        if (this.m_isFresh) {
            this.m_currentNode = this.m_traverser.first(this.m_root);
            this.m_isFresh = false;
        } else if (-1 != this.m_currentNode) {
            this.m_currentNode = this.m_traverser.next(this.m_root, this.m_currentNode);
        }
        if (-1 == this.m_currentNode) {
            this.m_foundLast = true;
        }
        return this.m_currentNode;
    }

    public int nextNode() {
        AxesWalker lastUsedWalker = wi().getLastUsedWalker();
        int nextNode = -1;
        while (true) {
            if (lastUsedWalker == null) {
                break;
            }
            nextNode = lastUsedWalker.getNextNode();
            if (-1 == nextNode) {
                lastUsedWalker = lastUsedWalker.m_prevWalker;
            } else if (lastUsedWalker.acceptNode(nextNode) != 1) {
                continue;
            } else {
                if (lastUsedWalker.m_nextWalker == null) {
                    wi().setLastUsedWalker(lastUsedWalker);
                    break;
                }
                AxesWalker axesWalker = lastUsedWalker.m_nextWalker;
                axesWalker.setRoot(nextNode);
                axesWalker.m_prevWalker = lastUsedWalker;
                lastUsedWalker = axesWalker;
            }
        }
        return nextNode;
    }

    @Override
    public int getLastPos(XPathContext xPathContext) {
        int proximityPosition = getProximityPosition();
        try {
            AxesWalker axesWalker = (AxesWalker) clone();
            axesWalker.setPredicateCount(this.m_predicateIndex);
            axesWalker.setNextWalker(null);
            axesWalker.setPrevWalker(null);
            WalkingIterator walkingIteratorWi = wi();
            AxesWalker lastUsedWalker = walkingIteratorWi.getLastUsedWalker();
            try {
                walkingIteratorWi.setLastUsedWalker(axesWalker);
                while (-1 != axesWalker.nextNode()) {
                    proximityPosition++;
                }
                return proximityPosition;
            } finally {
                walkingIteratorWi.setLastUsedWalker(lastUsedWalker);
            }
        } catch (CloneNotSupportedException e) {
            return -1;
        }
    }

    public void setDefaultDTM(DTM dtm) {
        this.m_dtm = dtm;
    }

    public DTM getDTM(int i) {
        return wi().getXPathContext().getDTM(i);
    }

    public boolean isDocOrdered() {
        return true;
    }

    public int getAxis() {
        return this.m_axis;
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        if (xPathVisitor.visitStep(expressionOwner, this)) {
            callPredicateVisitors(xPathVisitor);
            if (this.m_nextWalker != null) {
                this.m_nextWalker.callVisitors(this, xPathVisitor);
            }
        }
    }

    @Override
    public Expression getExpression() {
        return this.m_nextWalker;
    }

    @Override
    public void setExpression(Expression expression) {
        expression.exprSetParent(this);
        this.m_nextWalker = (AxesWalker) expression;
    }

    @Override
    public boolean deepEquals(Expression expression) {
        return super.deepEquals(expression) && this.m_axis == ((AxesWalker) expression).m_axis;
    }
}
