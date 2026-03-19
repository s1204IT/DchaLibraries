package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMAxisTraverser;
import org.apache.xpath.VariableStack;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.OpMap;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.patterns.NodeTest;
import org.apache.xpath.patterns.StepPattern;

public class MatchPatternIterator extends LocPathIterator {
    private static final boolean DEBUG = false;
    static final long serialVersionUID = -5201153767396296474L;
    protected StepPattern m_pattern;
    protected int m_superAxis;
    protected DTMAxisTraverser m_traverser;

    MatchPatternIterator(Compiler compiler, int opPos, int analysis) throws TransformerException {
        super(compiler, opPos, analysis, false);
        this.m_superAxis = -1;
        int firstStepPos = OpMap.getFirstChildPos(opPos);
        this.m_pattern = WalkerFactory.loadSteps(this, compiler, firstStepPos, 0);
        boolean fromRoot = (671088640 & analysis) != 0;
        boolean walkBack = (98066432 & analysis) != 0;
        boolean walkDescendants = (458752 & analysis) != 0;
        boolean walkAttributes = (2129920 & analysis) != 0;
        if (fromRoot || walkBack) {
            if (walkAttributes) {
                this.m_superAxis = 16;
                return;
            } else {
                this.m_superAxis = 17;
                return;
            }
        }
        if (walkDescendants) {
            if (walkAttributes) {
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
    public void setRoot(int context, Object environment) {
        super.setRoot(context, environment);
        this.m_traverser = this.m_cdtm.getAxisTraverser(this.m_superAxis);
    }

    @Override
    public void detach() {
        if (!this.m_allowDetach) {
            return;
        }
        this.m_traverser = null;
        super.detach();
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
        VariableStack vars;
        int savedStart;
        int next;
        if (this.m_foundLast) {
            return -1;
        }
        if (-1 != this.m_stackFrame) {
            vars = this.m_execContext.getVarStack();
            savedStart = vars.getStackFrame();
            vars.setStackFrame(this.m_stackFrame);
        } else {
            vars = null;
            savedStart = 0;
        }
        do {
            try {
                next = getNextNode();
                if (-1 == next || 1 == acceptNode(next, this.m_execContext)) {
                    break;
                }
            } finally {
                if (-1 != this.m_stackFrame) {
                    vars.setStackFrame(savedStart);
                }
            }
        } while (next != -1);
        if (-1 != next) {
            incrementCurrentPos();
            return next;
        }
        this.m_foundLast = true;
        if (-1 != this.m_stackFrame) {
            vars.setStackFrame(savedStart);
        }
        return -1;
    }

    public short acceptNode(int n, XPathContext xctxt) {
        try {
            try {
                xctxt.pushCurrentNode(n);
                xctxt.pushIteratorRoot(this.m_context);
                XObject score = this.m_pattern.execute(xctxt);
                return score == NodeTest.SCORE_NONE ? (short) 3 : (short) 1;
            } catch (TransformerException se) {
                throw new RuntimeException(se.getMessage());
            }
        } finally {
            xctxt.popCurrentNode();
            xctxt.popIteratorRoot();
        }
    }
}
