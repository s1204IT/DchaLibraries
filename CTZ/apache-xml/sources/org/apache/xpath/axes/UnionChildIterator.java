package org.apache.xpath.axes;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xpath.XPathContext;
import org.apache.xpath.patterns.NodeTest;

public class UnionChildIterator extends ChildTestIterator {
    static final long serialVersionUID = 3500298482193003495L;
    private PredicatedNodeTest[] m_nodeTests;

    public UnionChildIterator() {
        super(null);
        this.m_nodeTests = null;
    }

    public void addNodeTest(PredicatedNodeTest predicatedNodeTest) {
        if (this.m_nodeTests == null) {
            this.m_nodeTests = new PredicatedNodeTest[1];
            this.m_nodeTests[0] = predicatedNodeTest;
        } else {
            PredicatedNodeTest[] predicatedNodeTestArr = this.m_nodeTests;
            int length = this.m_nodeTests.length;
            this.m_nodeTests = new PredicatedNodeTest[length + 1];
            System.arraycopy(predicatedNodeTestArr, 0, this.m_nodeTests, 0, length);
            this.m_nodeTests[length] = predicatedNodeTest;
        }
        predicatedNodeTest.exprSetParent(this);
    }

    @Override
    public void fixupVariables(Vector vector, int i) {
        super.fixupVariables(vector, i);
        if (this.m_nodeTests != null) {
            for (int i2 = 0; i2 < this.m_nodeTests.length; i2++) {
                this.m_nodeTests[i2].fixupVariables(vector, i);
            }
        }
    }

    @Override
    public short acceptNode(int i) {
        XPathContext xPathContext = getXPathContext();
        try {
            try {
                xPathContext.pushCurrentNode(i);
                for (int i2 = 0; i2 < this.m_nodeTests.length; i2++) {
                    PredicatedNodeTest predicatedNodeTest = this.m_nodeTests[i2];
                    if (predicatedNodeTest.execute(xPathContext, i) != NodeTest.SCORE_NONE) {
                        if (predicatedNodeTest.getPredicateCount() <= 0) {
                            return (short) 1;
                        }
                        if (predicatedNodeTest.executePredicates(i, xPathContext)) {
                            return (short) 1;
                        }
                    }
                }
                xPathContext.popCurrentNode();
                return (short) 3;
            } catch (TransformerException e) {
                throw new RuntimeException(e.getMessage());
            }
        } finally {
            xPathContext.popCurrentNode();
        }
    }
}
