package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.Compiler;

public class SelfIteratorNoPredicate extends LocPathIterator {
    static final long serialVersionUID = -4226887905279814201L;

    SelfIteratorNoPredicate(Compiler compiler, int opPos, int analysis) throws TransformerException {
        super(compiler, opPos, analysis, false);
    }

    public SelfIteratorNoPredicate() throws TransformerException {
        super(null);
    }

    @Override
    public int nextNode() {
        if (this.m_foundLast) {
            return -1;
        }
        int next = -1 == this.m_lastFetched ? this.m_context : -1;
        this.m_lastFetched = next;
        if (-1 != next) {
            this.m_pos++;
            return next;
        }
        this.m_foundLast = true;
        return -1;
    }

    @Override
    public int asNode(XPathContext xctxt) throws TransformerException {
        return xctxt.getCurrentNode();
    }

    @Override
    public int getLastPos(XPathContext xctxt) {
        return 1;
    }
}
