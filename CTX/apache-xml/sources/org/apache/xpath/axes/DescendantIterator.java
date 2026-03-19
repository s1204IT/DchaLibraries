package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xpath.Expression;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.OpMap;

public class DescendantIterator extends LocPathIterator {
    static final long serialVersionUID = -1190338607743976938L;
    protected int m_axis;
    protected int m_extendedTypeID;
    protected transient DTMAxisTraverser m_traverser;

    DescendantIterator(Compiler compiler, int i, int i2) throws TransformerException {
        int nextStepPos;
        int whatToShow;
        super(compiler, i, i2, false);
        int firstChildPos = OpMap.getFirstChildPos(i);
        int op = compiler.getOp(firstChildPos);
        boolean z = true;
        boolean z2 = 42 == op;
        if (48 != op) {
            if (50 == op) {
                if (compiler.getOp(compiler.getNextStepPos(firstChildPos)) == 42) {
                    z2 = true;
                }
            }
            while (true) {
                nextStepPos = compiler.getNextStepPos(firstChildPos);
                if (nextStepPos <= 0 || -1 == compiler.getOp(nextStepPos)) {
                    break;
                } else {
                    firstChildPos = nextStepPos;
                }
            }
            boolean z3 = (i2 & 65536) == 0 ? z2 : false;
            if (!z) {
                if (z3) {
                    this.m_axis = 18;
                } else {
                    this.m_axis = 17;
                }
            } else if (z3) {
                this.m_axis = 5;
            } else {
                this.m_axis = 4;
            }
            whatToShow = compiler.getWhatToShow(firstChildPos);
            if ((whatToShow & 67) != 0 || whatToShow == -1) {
                initNodeTest(whatToShow);
            } else {
                initNodeTest(whatToShow, compiler.getStepNS(firstChildPos), compiler.getStepLocalName(firstChildPos));
            }
            initPredicateInfo(compiler, firstChildPos);
        }
        z2 = true;
        z = false;
        while (true) {
            nextStepPos = compiler.getNextStepPos(firstChildPos);
            if (nextStepPos <= 0) {
                break;
            }
            break;
            break;
            firstChildPos = nextStepPos;
        }
        if ((i2 & 65536) == 0) {
        }
        if (!z) {
        }
        whatToShow = compiler.getWhatToShow(firstChildPos);
        if ((whatToShow & 67) != 0) {
            initNodeTest(whatToShow);
        }
        initPredicateInfo(compiler, firstChildPos);
    }

    public DescendantIterator() {
        super(null);
        this.m_axis = 18;
        initNodeTest(-1);
    }

    @Override
    public DTMIterator cloneWithReset() throws CloneNotSupportedException {
        DescendantIterator descendantIterator = (DescendantIterator) super.cloneWithReset();
        descendantIterator.m_traverser = this.m_traverser;
        descendantIterator.resetProximityPositions();
        return descendantIterator;
    }

    @Override
    public int nextNode() {
        VariableStack varStack;
        int stackFrame;
        int next;
        if (this.m_foundLast) {
            return -1;
        }
        if (-1 == this.m_lastFetched) {
            resetProximityPositions();
        }
        if (-1 != this.m_stackFrame) {
            varStack = this.m_execContext.getVarStack();
            stackFrame = varStack.getStackFrame();
            varStack.setStackFrame(this.m_stackFrame);
        } else {
            varStack = null;
            stackFrame = 0;
        }
        do {
            try {
                if (this.m_extendedTypeID == 0) {
                    if (-1 == this.m_lastFetched) {
                        next = this.m_traverser.first(this.m_context);
                    } else {
                        next = this.m_traverser.next(this.m_context, this.m_lastFetched);
                    }
                    this.m_lastFetched = next;
                } else {
                    if (-1 == this.m_lastFetched) {
                        next = this.m_traverser.first(this.m_context, this.m_extendedTypeID);
                    } else {
                        next = this.m_traverser.next(this.m_context, this.m_lastFetched, this.m_extendedTypeID);
                    }
                    this.m_lastFetched = next;
                }
                if (-1 == next || 1 == acceptNode(next)) {
                    break;
                }
            } finally {
                if (-1 != this.m_stackFrame) {
                    varStack.setStackFrame(stackFrame);
                }
            }
        } while (next != -1);
        if (-1 != next) {
            this.m_pos++;
            return next;
        }
        this.m_foundLast = true;
        if (-1 != this.m_stackFrame) {
            varStack.setStackFrame(stackFrame);
        }
        return -1;
    }

    @Override
    public void setRoot(int i, Object obj) {
        super.setRoot(i, obj);
        this.m_traverser = this.m_cdtm.getAxisTraverser(this.m_axis);
        String localName = getLocalName();
        String namespace = getNamespace();
        int i2 = this.m_whatToShow;
        if (-1 == i2 || "*".equals(localName) || "*".equals(namespace)) {
            this.m_extendedTypeID = 0;
        } else {
            this.m_extendedTypeID = this.m_cdtm.getExpandedTypeID(namespace, localName, getNodeTypeTest(i2));
        }
    }

    @Override
    public int asNode(XPathContext xPathContext) throws TransformerException {
        if (getPredicateCount() > 0) {
            return super.asNode(xPathContext);
        }
        int currentNode = xPathContext.getCurrentNode();
        DTM dtm = xPathContext.getDTM(currentNode);
        DTMAxisTraverser axisTraverser = dtm.getAxisTraverser(this.m_axis);
        String localName = getLocalName();
        String namespace = getNamespace();
        int i = this.m_whatToShow;
        if (-1 == i || localName == "*" || namespace == "*") {
            return axisTraverser.first(currentNode);
        }
        return axisTraverser.first(currentNode, dtm.getExpandedTypeID(namespace, localName, getNodeTypeTest(i)));
    }

    @Override
    public void detach() {
        if (this.m_allowDetach) {
            this.m_traverser = null;
            this.m_extendedTypeID = 0;
            super.detach();
        }
    }

    @Override
    public int getAxis() {
        return this.m_axis;
    }

    @Override
    public boolean deepEquals(Expression expression) {
        return super.deepEquals(expression) && this.m_axis == ((DescendantIterator) expression).m_axis;
    }
}
