package org.apache.xpath.axes;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.compiler.Compiler;

public class WalkingIteratorSorted extends WalkingIterator {
    static final long serialVersionUID = -4512512007542368213L;
    protected boolean m_inNaturalOrderStatic;

    public WalkingIteratorSorted(PrefixResolver prefixResolver) {
        super(prefixResolver);
        this.m_inNaturalOrderStatic = false;
    }

    WalkingIteratorSorted(Compiler compiler, int i, int i2, boolean z) throws TransformerException {
        super(compiler, i, i2, z);
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
        for (AxesWalker nextWalker = this.m_firstWalker; nextWalker != null; nextWalker = nextWalker.getNextWalker()) {
            int axis = nextWalker.getAxis();
            if (!nextWalker.isDocOrdered()) {
                return false;
            }
            if (!(axis == 3 || axis == 13 || axis == 19) && axis != -1) {
                return (nextWalker.getNextWalker() == null) && ((nextWalker.isDocOrdered() && (axis == 4 || axis == 5 || axis == 17 || axis == 18)) || axis == 2);
            }
        }
        return true;
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        super.fixupVariables(vector, i);
        if (WalkerFactory.isNaturalDocOrder(getAnalysisBits())) {
            this.m_inNaturalOrderStatic = true;
        } else {
            this.m_inNaturalOrderStatic = false;
        }
    }
}
