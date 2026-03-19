package org.apache.xalan.transformer;

import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.ElemNumber;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.XPathContext;

public class Counter {
    static final int MAXCOUNTNODES = 500;
    NodeSetDTM m_countNodes;
    int m_countResult;
    ElemNumber m_numberElem;
    int m_countNodesStartCount = 0;
    int m_fromNode = -1;

    Counter(ElemNumber elemNumber, NodeSetDTM nodeSetDTM) throws TransformerException {
        this.m_countNodes = nodeSetDTM;
        this.m_numberElem = elemNumber;
    }

    int getPreviouslyCounted(XPathContext xPathContext, int i) {
        int size = this.m_countNodes.size();
        this.m_countResult = 0;
        int i2 = size - 1;
        while (true) {
            if (i2 < 0) {
                break;
            }
            int iElementAt = this.m_countNodes.elementAt(i2);
            if (i == iElementAt) {
                this.m_countResult = i2 + 1 + this.m_countNodesStartCount;
                break;
            }
            if (xPathContext.getDTM(iElementAt).isNodeAfter(iElementAt, i)) {
                break;
            }
            i2--;
        }
        return this.m_countResult;
    }

    int getLast() {
        int size = this.m_countNodes.size();
        if (size > 0) {
            return this.m_countNodes.elementAt(size - 1);
        }
        return -1;
    }
}
