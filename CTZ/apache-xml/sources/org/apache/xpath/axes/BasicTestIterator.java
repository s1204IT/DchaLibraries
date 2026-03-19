package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.VariableStack;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.OpMap;

public abstract class BasicTestIterator extends LocPathIterator {
    static final long serialVersionUID = 3505378079378096623L;

    protected abstract int getNextNode();

    protected BasicTestIterator() {
    }

    protected BasicTestIterator(PrefixResolver prefixResolver) {
        super(prefixResolver);
    }

    protected BasicTestIterator(Compiler compiler, int i, int i2) throws TransformerException {
        super(compiler, i, i2, false);
        int firstChildPos = OpMap.getFirstChildPos(i);
        int whatToShow = compiler.getWhatToShow(firstChildPos);
        if ((whatToShow & 4163) == 0 || whatToShow == -1) {
            initNodeTest(whatToShow);
        } else {
            initNodeTest(whatToShow, compiler.getStepNS(firstChildPos), compiler.getStepLocalName(firstChildPos));
        }
        initPredicateInfo(compiler, firstChildPos);
    }

    protected BasicTestIterator(Compiler compiler, int i, int i2, boolean z) throws TransformerException {
        super(compiler, i, i2, z);
    }

    @Override
    public int nextNode() {
        VariableStack varStack;
        int stackFrame;
        int nextNode;
        if (this.m_foundLast) {
            this.m_lastFetched = -1;
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
                nextNode = getNextNode();
                if (-1 == nextNode || 1 == acceptNode(nextNode)) {
                    break;
                }
            } finally {
                if (-1 != this.m_stackFrame) {
                    varStack.setStackFrame(stackFrame);
                }
            }
        } while (nextNode != -1);
        if (-1 != nextNode) {
            this.m_pos++;
            return nextNode;
        }
        this.m_foundLast = true;
        if (-1 != this.m_stackFrame) {
            varStack.setStackFrame(stackFrame);
        }
        return -1;
    }

    @Override
    public DTMIterator cloneWithReset() throws CloneNotSupportedException {
        ChildTestIterator childTestIterator = (ChildTestIterator) super.cloneWithReset();
        childTestIterator.resetProximityPositions();
        return childTestIterator;
    }
}
