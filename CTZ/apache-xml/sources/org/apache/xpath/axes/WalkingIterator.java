package org.apache.xpath.axes;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.Expression;
import org.apache.xpath.ExpressionOwner;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathVisitor;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.OpMap;

public class WalkingIterator extends LocPathIterator implements ExpressionOwner {
    static final long serialVersionUID = 9110225941815665906L;
    protected AxesWalker m_firstWalker;
    protected AxesWalker m_lastUsedWalker;

    WalkingIterator(Compiler compiler, int i, int i2, boolean z) throws TransformerException {
        super(compiler, i, i2, z);
        int firstChildPos = OpMap.getFirstChildPos(i);
        if (z) {
            this.m_firstWalker = WalkerFactory.loadWalkers(this, compiler, firstChildPos, 0);
            this.m_lastUsedWalker = this.m_firstWalker;
        }
    }

    public WalkingIterator(PrefixResolver prefixResolver) {
        super(prefixResolver);
    }

    @Override
    public int getAnalysisBits() {
        int analysisBits = 0;
        if (this.m_firstWalker != null) {
            for (AxesWalker nextWalker = this.m_firstWalker; nextWalker != null; nextWalker = nextWalker.getNextWalker()) {
                analysisBits |= nextWalker.getAnalysisBits();
            }
        }
        return analysisBits;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        WalkingIterator walkingIterator = (WalkingIterator) super.clone();
        if (this.m_firstWalker != null) {
            walkingIterator.m_firstWalker = this.m_firstWalker.cloneDeep(walkingIterator, null);
        }
        return walkingIterator;
    }

    @Override
    public void reset() {
        super.reset();
        if (this.m_firstWalker != null) {
            this.m_lastUsedWalker = this.m_firstWalker;
            this.m_firstWalker.setRoot(this.m_context);
        }
    }

    @Override
    public void setRoot(int i, Object obj) {
        super.setRoot(i, obj);
        if (this.m_firstWalker != null) {
            this.m_firstWalker.setRoot(i);
            this.m_lastUsedWalker = this.m_firstWalker;
        }
    }

    @Override
    public int nextNode() {
        if (this.m_foundLast) {
            return -1;
        }
        if (-1 == this.m_stackFrame) {
            return returnNextNode(this.m_firstWalker.nextNode());
        }
        VariableStack varStack = this.m_execContext.getVarStack();
        int stackFrame = varStack.getStackFrame();
        varStack.setStackFrame(this.m_stackFrame);
        int iReturnNextNode = returnNextNode(this.m_firstWalker.nextNode());
        varStack.setStackFrame(stackFrame);
        return iReturnNextNode;
    }

    public final AxesWalker getFirstWalker() {
        return this.m_firstWalker;
    }

    public final void setFirstWalker(AxesWalker axesWalker) {
        this.m_firstWalker = axesWalker;
    }

    public final void setLastUsedWalker(AxesWalker axesWalker) {
        this.m_lastUsedWalker = axesWalker;
    }

    public final AxesWalker getLastUsedWalker() {
        return this.m_lastUsedWalker;
    }

    @Override
    public void detach() {
        if (this.m_allowDetach) {
            for (AxesWalker nextWalker = this.m_firstWalker; nextWalker != null; nextWalker = nextWalker.getNextWalker()) {
                nextWalker.detach();
            }
            this.m_lastUsedWalker = null;
            super.detach();
        }
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        this.m_predicateIndex = -1;
        for (AxesWalker nextWalker = this.m_firstWalker; nextWalker != null; nextWalker = nextWalker.getNextWalker()) {
            nextWalker.fixupVariables(vector, i);
        }
    }

    @Override
    public void callVisitors(ExpressionOwner expressionOwner, XPathVisitor xPathVisitor) {
        if (xPathVisitor.visitLocationPath(expressionOwner, this) && this.m_firstWalker != null) {
            this.m_firstWalker.callVisitors(this, xPathVisitor);
        }
    }

    @Override
    public Expression getExpression() {
        return this.m_firstWalker;
    }

    @Override
    public void setExpression(Expression expression) {
        expression.exprSetParent(this);
        this.m_firstWalker = (AxesWalker) expression;
    }

    @Override
    public boolean deepEquals(Expression expression) {
        if (!super.deepEquals(expression)) {
            return false;
        }
        AxesWalker nextWalker = this.m_firstWalker;
        AxesWalker nextWalker2 = ((WalkingIterator) expression).m_firstWalker;
        while (nextWalker != null && nextWalker2 != null) {
            if (!nextWalker.deepEquals(nextWalker2)) {
                return false;
            }
            nextWalker = nextWalker.getNextWalker();
            nextWalker2 = nextWalker2.getNextWalker();
        }
        return nextWalker == null && nextWalker2 == null;
    }
}
