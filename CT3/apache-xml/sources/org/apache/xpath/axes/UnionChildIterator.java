package org.apache.xpath.axes;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.patterns.NodeTest;

public class UnionChildIterator extends ChildTestIterator {
    static final long serialVersionUID = 3500298482193003495L;
    private PredicatedNodeTest[] m_nodeTests;

    public UnionChildIterator() {
        super(null);
        this.m_nodeTests = null;
    }

    public void addNodeTest(PredicatedNodeTest test) {
        if (this.m_nodeTests == null) {
            this.m_nodeTests = new PredicatedNodeTest[1];
            this.m_nodeTests[0] = test;
        } else {
            PredicatedNodeTest[] tests = this.m_nodeTests;
            int len = this.m_nodeTests.length;
            this.m_nodeTests = new PredicatedNodeTest[len + 1];
            System.arraycopy(tests, 0, this.m_nodeTests, 0, len);
            this.m_nodeTests[len] = test;
        }
        test.exprSetParent(this);
    }

    @Override
    public void fixupVariables(Vector vars, int globalsSize) {
        super.fixupVariables(vars, globalsSize);
        if (this.m_nodeTests == null) {
            return;
        }
        for (int i = 0; i < this.m_nodeTests.length; i++) {
            this.m_nodeTests[i].fixupVariables(vars, globalsSize);
        }
    }

    @Override
    public short acceptNode(int n) {
        XPathContext xctxt = getXPathContext();
        try {
            try {
                xctxt.pushCurrentNode(n);
                for (int i = 0; i < this.m_nodeTests.length; i++) {
                    PredicatedNodeTest pnt = this.m_nodeTests[i];
                    XObject score = pnt.execute(xctxt, n);
                    if (score != NodeTest.SCORE_NONE) {
                        if (pnt.getPredicateCount() <= 0) {
                            return (short) 1;
                        }
                        if (pnt.executePredicates(n, xctxt)) {
                            return (short) 1;
                        }
                    }
                }
                xctxt.popCurrentNode();
                return (short) 3;
            } catch (TransformerException se) {
                throw new RuntimeException(se.getMessage());
            }
        } finally {
            xctxt.popCurrentNode();
        }
    }
}
