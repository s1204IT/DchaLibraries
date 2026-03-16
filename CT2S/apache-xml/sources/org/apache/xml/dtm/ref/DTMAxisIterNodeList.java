package org.apache.xml.dtm.ref;

import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMAxisIterator;
import org.apache.xml.utils.IntVector;
import org.w3c.dom.Node;

public class DTMAxisIterNodeList extends DTMNodeListBase {
    private IntVector m_cachedNodes;
    private DTM m_dtm;
    private DTMAxisIterator m_iter;
    private int m_last;

    private DTMAxisIterNodeList() {
        this.m_last = -1;
    }

    public DTMAxisIterNodeList(DTM dtm, DTMAxisIterator dtmAxisIterator) {
        this.m_last = -1;
        if (dtmAxisIterator == null) {
            this.m_last = 0;
        } else {
            this.m_cachedNodes = new IntVector();
            this.m_dtm = dtm;
        }
        this.m_iter = dtmAxisIterator;
    }

    public DTMAxisIterator getDTMAxisIterator() {
        return this.m_iter;
    }

    @Override
    public Node item(int index) {
        int node;
        if (this.m_iter != null) {
            int count = this.m_cachedNodes.size();
            if (count > index) {
                return this.m_dtm.getNode(this.m_cachedNodes.elementAt(index));
            }
            if (this.m_last == -1) {
                while (true) {
                    node = this.m_iter.next();
                    if (node == -1 || count > index) {
                        break;
                    }
                    this.m_cachedNodes.addElement(node);
                    count++;
                }
                if (node == -1) {
                    this.m_last = count;
                } else {
                    return this.m_dtm.getNode(node);
                }
            }
        }
        return null;
    }

    @Override
    public int getLength() {
        if (this.m_last == -1) {
            while (true) {
                int node = this.m_iter.next();
                if (node == -1) {
                    break;
                }
                this.m_cachedNodes.addElement(node);
            }
            this.m_last = this.m_cachedNodes.size();
        }
        return this.m_last;
    }
}
