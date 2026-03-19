package org.apache.xpath.axes;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.compiler.Compiler;

public class WalkingIteratorSorted extends WalkingIterator {
    static final long serialVersionUID = -4512512007542368213L;
    protected boolean m_inNaturalOrderStatic;

    public WalkingIteratorSorted(PrefixResolver nscontext) {
        super(nscontext);
        this.m_inNaturalOrderStatic = false;
    }

    WalkingIteratorSorted(Compiler compiler, int opPos, int analysis, boolean shouldLoadWalkers) throws TransformerException {
        super(compiler, opPos, analysis, shouldLoadWalkers);
        this.m_inNaturalOrderStatic = false;
    }

    @Override
    public boolean isDocOrdered() {
        return this.m_inNaturalOrderStatic;
    }

    boolean canBeWalkedInNaturalDocOrderStatic() {
        if (this.m_firstWalker == null) {
            return false;
        }
        AxesWalker walker = this.m_firstWalker;
        int i = 0;
        while (walker != null) {
            int axis = walker.getAxis();
            if (!walker.isDocOrdered()) {
                return false;
            }
            boolean isSimpleDownAxis = axis == 3 || axis == 13 || axis == 19;
            if (isSimpleDownAxis || axis == -1) {
                walker = walker.getNextWalker();
                i++;
            } else {
                boolean isLastWalker = walker.getNextWalker() == null;
                return isLastWalker && ((walker.isDocOrdered() && (axis == 4 || axis == 5 || axis == 17 || axis == 18)) || axis == 2);
            }
        }
        return true;
    }

    @Override
    public void fixupVariables(Vector vars, int globalsSize) {
        super.fixupVariables(vars, globalsSize);
        int analysis = getAnalysisBits();
        if (WalkerFactory.isNaturalDocOrder(analysis)) {
            this.m_inNaturalOrderStatic = true;
        } else {
            this.m_inNaturalOrderStatic = false;
        }
    }
}
