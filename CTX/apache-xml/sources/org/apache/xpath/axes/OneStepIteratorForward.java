package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xpath.Expression;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.compiler.OpMap;

public class OneStepIteratorForward extends ChildTestIterator {
    static final long serialVersionUID = -1576936606178190566L;
    protected int m_axis;

    OneStepIteratorForward(Compiler compiler, int i, int i2) throws TransformerException {
        super(compiler, i, i2);
        this.m_axis = -1;
        this.m_axis = WalkerFactory.getAxisFromStep(compiler, OpMap.getFirstChildPos(i));
    }

    public OneStepIteratorForward(int i) {
        super(null);
        this.m_axis = -1;
        this.m_axis = i;
        initNodeTest(-1);
    }

    @Override
    public void setRoot(int i, Object obj) {
        super.setRoot(i, obj);
        this.m_traverser = this.m_cdtm.getAxisTraverser(this.m_axis);
    }

    @Override
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
    public int getAxis() {
        return this.m_axis;
    }

    @Override
    public boolean deepEquals(Expression expression) {
        return super.deepEquals(expression) && this.m_axis == ((OneStepIteratorForward) expression).m_axis;
    }
}
