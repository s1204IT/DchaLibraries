package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.Compiler;

public class SelfIteratorNoPredicate extends LocPathIterator {
    static final long serialVersionUID = -4226887905279814201L;

    SelfIteratorNoPredicate(Compiler compiler, int i, int i2) throws TransformerException {
        super(compiler, i, i2, false);
    }

    public SelfIteratorNoPredicate() throws TransformerException {
        super(null);
    }

    @Override
    public int nextNode() {
        int i;
        if (this.m_foundLast) {
            return -1;
        }
        if (-1 == this.m_lastFetched) {
            i = this.m_context;
        } else {
            i = -1;
        }
        this.m_lastFetched = i;
        if (-1 != i) {
            this.m_pos++;
            return i;
        }
        this.m_foundLast = true;
        return -1;
    }

    @Override
    public int asNode(XPathContext xPathContext) throws TransformerException {
        return xPathContext.getCurrentNode();
    }

    @Override
    public int getLastPos(XPathContext xPathContext) {
        return 1;
    }
}
