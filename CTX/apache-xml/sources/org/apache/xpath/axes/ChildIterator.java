package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.Compiler;

public class ChildIterator extends LocPathIterator {
    static final long serialVersionUID = -6935428015142993583L;

    ChildIterator(Compiler compiler, int i, int i2) throws TransformerException {
        super(compiler, i, i2, false);
        initNodeTest(-1);
    }

    @Override
    public int asNode(XPathContext xPathContext) throws TransformerException {
        int currentNode = xPathContext.getCurrentNode();
        return xPathContext.getDTM(currentNode).getFirstChild(currentNode);
    }

    @Override
    public int nextNode() {
        int nextSibling;
        if (this.m_foundLast) {
            return -1;
        }
        if (-1 == this.m_lastFetched) {
            nextSibling = this.m_cdtm.getFirstChild(this.m_context);
        } else {
            nextSibling = this.m_cdtm.getNextSibling(this.m_lastFetched);
        }
        this.m_lastFetched = nextSibling;
        if (-1 != nextSibling) {
            this.m_pos++;
            return nextSibling;
        }
        this.m_foundLast = true;
        return -1;
    }

    @Override
    public int getAxis() {
        return 3;
    }
}
