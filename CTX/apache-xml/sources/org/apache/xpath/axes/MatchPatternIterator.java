package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.OpMap;
import org.apache.xpath.patterns.NodeTest;
import org.apache.xpath.patterns.StepPattern;

public class MatchPatternIterator extends LocPathIterator {
    private static final boolean DEBUG = false;
    static final long serialVersionUID = -5201153767396296474L;
    protected StepPattern m_pattern;
    protected int m_superAxis;
    protected DTMAxisTraverser m_traverser;

    MatchPatternIterator(Compiler compiler, int i, int i2) throws TransformerException {
        boolean z;
        boolean z2;
        boolean z3;
        super(compiler, i, i2, false);
        this.m_superAxis = -1;
        this.m_pattern = WalkerFactory.loadSteps(this, compiler, OpMap.getFirstChildPos(i), 0);
        if ((671088640 & i2) == 0) {
            z = false;
        } else {
            z = true;
        }
        if ((98066432 & i2) == 0) {
            z2 = false;
        } else {
            z2 = true;
        }
        if ((458752 & i2) == 0) {
            z3 = false;
        } else {
            z3 = true;
        }
        boolean z4 = (i2 & 2129920) != 0;
        if (z || z2) {
            if (z4) {
                this.m_superAxis = 16;
                return;
            } else {
                this.m_superAxis = 17;
                return;
            }
        }
        if (z3) {
            if (z4) {
                this.m_superAxis = 14;
                return;
            } else {
                this.m_superAxis = 5;
                return;
            }
        }
        this.m_superAxis = 16;
    }

    @Override
    public void setRoot(int i, Object obj) {
        super.setRoot(i, obj);
        this.m_traverser = this.m_cdtm.getAxisTraverser(this.m_superAxis);
    }

    @Override
    public void detach() {
        if (this.m_allowDetach) {
            this.m_traverser = null;
            super.detach();
        }
    }

    protected int getNextNode() {
        int next;
        if (-1 == this.m_lastFetched) {
            next = this.m_traverser.first(this.m_context);
        } else {
            next = this.m_traverser.next(this.m_context, this.m_lastFetched);
        }
        this.m_lastFetched = next;
        return this.m_lastFetched;
    }

    @Override
    public int nextNode() {
        VariableStack varStack;
        int stackFrame;
        int nextNode;
        if (this.m_foundLast) {
            return -1;
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
                nextNode = getNextNode();
                if (-1 == nextNode || 1 == acceptNode(nextNode, this.m_execContext)) {
                    break;
                }
            } finally {
                if (-1 != this.m_stackFrame) {
                    varStack.setStackFrame(stackFrame);
                }
            }
        } while (nextNode != -1);
        if (-1 != nextNode) {
            incrementCurrentPos();
            return nextNode;
        }
        this.m_foundLast = true;
        if (-1 != this.m_stackFrame) {
            varStack.setStackFrame(stackFrame);
        }
        return -1;
    }

    public short acceptNode(int i, XPathContext xPathContext) {
        try {
            try {
                xPathContext.pushCurrentNode(i);
                xPathContext.pushIteratorRoot(this.m_context);
                return this.m_pattern.execute(xPathContext) == NodeTest.SCORE_NONE ? (short) 3 : (short) 1;
            } catch (TransformerException e) {
                throw new RuntimeException(e.getMessage());
            }
        } finally {
            xPathContext.popCurrentNode();
            xPathContext.popIteratorRoot();
        }
    }
}
