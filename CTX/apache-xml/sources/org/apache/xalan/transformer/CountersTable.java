package org.apache.xalan.transformer;

import java.util.Hashtable;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.ElemNumber;
import org.apache.xpath.NodeSetDTM;
import org.apache.xpath.XPathContext;

public class CountersTable extends Hashtable {
    static final long serialVersionUID = 2159100770924179875L;
    transient int m_countersMade = 0;
    private transient NodeSetDTM m_newFound;

    Vector getCounters(ElemNumber elemNumber) {
        Vector vector = (Vector) get(elemNumber);
        return vector == null ? putElemNumber(elemNumber) : vector;
    }

    Vector putElemNumber(ElemNumber elemNumber) {
        Vector vector = new Vector();
        put(elemNumber, vector);
        return vector;
    }

    void appendBtoFList(NodeSetDTM nodeSetDTM, NodeSetDTM nodeSetDTM2) {
        for (int size = nodeSetDTM2.size() - 1; size >= 0; size--) {
            nodeSetDTM.addElement(nodeSetDTM2.item(size));
        }
    }

    public int countNode(XPathContext xPathContext, ElemNumber elemNumber, int i) throws TransformerException {
        Vector counters = getCounters(elemNumber);
        int size = counters.size();
        int targetNode = elemNumber.getTargetNode(xPathContext, i);
        if (-1 == targetNode) {
            return 0;
        }
        for (int i2 = 0; i2 < size; i2++) {
            int previouslyCounted = ((Counter) counters.elementAt(i2)).getPreviouslyCounted(xPathContext, targetNode);
            if (previouslyCounted > 0) {
                return previouslyCounted;
            }
        }
        if (this.m_newFound == null) {
            this.m_newFound = new NodeSetDTM(xPathContext.getDTMManager());
        }
        int i3 = 0;
        while (-1 != targetNode) {
            if (i3 != 0) {
                for (int i4 = 0; i4 < size; i4++) {
                    Counter counter = (Counter) counters.elementAt(i4);
                    int size2 = counter.m_countNodes.size();
                    if (size2 > 0 && counter.m_countNodes.elementAt(size2 - 1) == targetNode) {
                        int i5 = i3 + counter.m_countNodesStartCount + size2;
                        if (size2 > 0) {
                            appendBtoFList(counter.m_countNodes, this.m_newFound);
                        }
                        this.m_newFound.removeAllElements();
                        return i5;
                    }
                }
            }
            this.m_newFound.addElement(targetNode);
            i3++;
            targetNode = elemNumber.getPreviousNode(xPathContext, targetNode);
        }
        Counter counter2 = new Counter(elemNumber, new NodeSetDTM(xPathContext.getDTMManager()));
        this.m_countersMade++;
        appendBtoFList(counter2.m_countNodes, this.m_newFound);
        this.m_newFound.removeAllElements();
        counters.addElement(counter2);
        return i3;
    }
}
