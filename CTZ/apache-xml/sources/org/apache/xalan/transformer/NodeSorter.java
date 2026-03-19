package org.apache.xalan.transformer;

import java.text.CollationKey;
import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XObject;

public class NodeSorter {
    XPathContext m_execContext;
    Vector m_keys;

    public NodeSorter(XPathContext xPathContext) {
        this.m_execContext = xPathContext;
    }

    public void sort(DTMIterator dTMIterator, Vector vector, XPathContext xPathContext) throws TransformerException {
        this.m_keys = vector;
        int length = dTMIterator.getLength();
        Vector vector2 = new Vector();
        for (int i = 0; i < length; i++) {
            vector2.addElement(new NodeCompareElem(dTMIterator.item(i)));
        }
        mergesort(vector2, new Vector(), 0, length - 1, xPathContext);
        for (int i2 = 0; i2 < length; i2++) {
            dTMIterator.setItem(((NodeCompareElem) vector2.elementAt(i2)).m_node, i2);
        }
        dTMIterator.setCurrentPos(0);
    }

    int compare(NodeCompareElem nodeCompareElem, NodeCompareElem nodeCompareElem2, int i, XPathContext xPathContext) throws TransformerException {
        CollationKey collationKey;
        CollationKey collationKey2;
        int iCompare;
        int i2;
        double dDoubleValue;
        double dDoubleValue2;
        double d;
        int i3;
        NodeSortKey nodeSortKey = (NodeSortKey) this.m_keys.elementAt(i);
        int i4 = 0;
        if (nodeSortKey.m_treatAsNumbers) {
            if (i == 0) {
                dDoubleValue = ((Double) nodeCompareElem.m_key1Value).doubleValue();
                dDoubleValue2 = ((Double) nodeCompareElem2.m_key1Value).doubleValue();
            } else if (i == 1) {
                dDoubleValue = ((Double) nodeCompareElem.m_key2Value).doubleValue();
                dDoubleValue2 = ((Double) nodeCompareElem2.m_key2Value).doubleValue();
            } else {
                XObject xObjectExecute = nodeSortKey.m_selectPat.execute(this.m_execContext, nodeCompareElem.m_node, nodeSortKey.m_namespaceContext);
                XObject xObjectExecute2 = nodeSortKey.m_selectPat.execute(this.m_execContext, nodeCompareElem2.m_node, nodeSortKey.m_namespaceContext);
                double dNum = xObjectExecute.num();
                double dNum2 = xObjectExecute2.num();
                dDoubleValue = dNum;
                dDoubleValue2 = dNum2;
            }
            if (dDoubleValue == dDoubleValue2 && (i3 = i + 1) < this.m_keys.size()) {
                iCompare = compare(nodeCompareElem, nodeCompareElem2, i3, xPathContext);
            } else {
                if (Double.isNaN(dDoubleValue)) {
                    if (!Double.isNaN(dDoubleValue2)) {
                        d = -1.0d;
                    } else {
                        d = 0.0d;
                    }
                } else if (Double.isNaN(dDoubleValue2)) {
                    d = 1.0d;
                } else {
                    d = dDoubleValue - dDoubleValue2;
                }
                if (d < XPath.MATCH_SCORE_QNAME) {
                    iCompare = nodeSortKey.m_descending ? 1 : -1;
                } else if (d <= XPath.MATCH_SCORE_QNAME) {
                    iCompare = 0;
                } else if (nodeSortKey.m_descending) {
                }
            }
        } else {
            if (i == 0) {
                collationKey = (CollationKey) nodeCompareElem.m_key1Value;
                collationKey2 = (CollationKey) nodeCompareElem2.m_key1Value;
            } else if (i == 1) {
                collationKey = (CollationKey) nodeCompareElem.m_key2Value;
                collationKey2 = (CollationKey) nodeCompareElem2.m_key2Value;
            } else {
                XObject xObjectExecute3 = nodeSortKey.m_selectPat.execute(this.m_execContext, nodeCompareElem.m_node, nodeSortKey.m_namespaceContext);
                XObject xObjectExecute4 = nodeSortKey.m_selectPat.execute(this.m_execContext, nodeCompareElem2.m_node, nodeSortKey.m_namespaceContext);
                collationKey = nodeSortKey.m_col.getCollationKey(xObjectExecute3.str());
                collationKey2 = nodeSortKey.m_col.getCollationKey(xObjectExecute4.str());
            }
            int iCompareTo = collationKey.compareTo(collationKey2);
            if (nodeSortKey.m_caseOrderUpper && collationKey.getSourceString().toLowerCase().equals(collationKey2.getSourceString().toLowerCase())) {
                if (iCompareTo != 0) {
                    i4 = -iCompareTo;
                }
            } else {
                i4 = iCompareTo;
            }
            iCompare = nodeSortKey.m_descending ? -i4 : i4;
        }
        if (iCompare == 0 && (i2 = i + 1) < this.m_keys.size()) {
            iCompare = compare(nodeCompareElem, nodeCompareElem2, i2, xPathContext);
        }
        return iCompare == 0 ? xPathContext.getDTM(nodeCompareElem.m_node).isNodeAfter(nodeCompareElem.m_node, nodeCompareElem2.m_node) ? -1 : 1 : iCompare;
    }

    void mergesort(Vector vector, Vector vector2, int i, int i2, XPathContext xPathContext) throws TransformerException {
        int iCompare;
        if (i2 - i > 0) {
            int i3 = (i2 + i) / 2;
            mergesort(vector, vector2, i, i3, xPathContext);
            int i4 = i3 + 1;
            mergesort(vector, vector2, i4, i2, xPathContext);
            for (int i5 = i3; i5 >= i; i5--) {
                if (i5 >= vector2.size()) {
                    vector2.insertElementAt(vector.elementAt(i5), i5);
                } else {
                    vector2.setElementAt(vector.elementAt(i5), i5);
                }
            }
            while (i4 <= i2) {
                int i6 = ((i2 + i3) + 1) - i4;
                if (i6 >= vector2.size()) {
                    vector2.insertElementAt(vector.elementAt(i4), i6);
                } else {
                    vector2.setElementAt(vector.elementAt(i4), i6);
                }
                i4++;
            }
            int i7 = i;
            int i8 = i2;
            while (i <= i2) {
                if (i7 != i8) {
                    iCompare = compare((NodeCompareElem) vector2.elementAt(i7), (NodeCompareElem) vector2.elementAt(i8), 0, xPathContext);
                } else {
                    iCompare = -1;
                }
                if (iCompare < 0) {
                    vector.setElementAt(vector2.elementAt(i7), i);
                    i7++;
                } else if (iCompare > 0) {
                    vector.setElementAt(vector2.elementAt(i8), i);
                    i8--;
                }
                i++;
            }
        }
    }

    class NodeCompareElem {
        Object m_key1Value;
        Object m_key2Value;
        int m_node;
        int maxkey = 2;

        NodeCompareElem(int i) throws TransformerException {
            this.m_node = i;
            if (!NodeSorter.this.m_keys.isEmpty()) {
                NodeSortKey nodeSortKey = (NodeSortKey) NodeSorter.this.m_keys.elementAt(0);
                XObject xObjectExecute = nodeSortKey.m_selectPat.execute(NodeSorter.this.m_execContext, i, nodeSortKey.m_namespaceContext);
                if (nodeSortKey.m_treatAsNumbers) {
                    this.m_key1Value = new Double(xObjectExecute.num());
                } else {
                    this.m_key1Value = nodeSortKey.m_col.getCollationKey(xObjectExecute.str());
                }
                if (xObjectExecute.getType() == 4) {
                    DTMIterator dTMIteratorIterRaw = ((XNodeSet) xObjectExecute).iterRaw();
                    if (-1 == dTMIteratorIterRaw.getCurrentNode()) {
                        dTMIteratorIterRaw.nextNode();
                    }
                }
                if (NodeSorter.this.m_keys.size() > 1) {
                    NodeSortKey nodeSortKey2 = (NodeSortKey) NodeSorter.this.m_keys.elementAt(1);
                    XObject xObjectExecute2 = nodeSortKey2.m_selectPat.execute(NodeSorter.this.m_execContext, i, nodeSortKey2.m_namespaceContext);
                    if (nodeSortKey2.m_treatAsNumbers) {
                        this.m_key2Value = new Double(xObjectExecute2.num());
                    } else {
                        this.m_key2Value = nodeSortKey2.m_col.getCollationKey(xObjectExecute2.str());
                    }
                }
            }
        }
    }
}
