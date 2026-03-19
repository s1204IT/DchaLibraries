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

    public DTMAxisIterNodeList(DTM dtm, DTMAxisIterator dTMAxisIterator) {
        this.m_last = -1;
        if (dTMAxisIterator == null) {
            this.m_last = 0;
        } else {
            this.m_cachedNodes = new IntVector();
            this.m_dtm = dtm;
        }
        this.m_iter = dTMAxisIterator;
    }

    public DTMAxisIterator getDTMAxisIterator() {
        return this.m_iter;
    }

    @Override
    public Node item(int i) {
        int next;
        if (this.m_iter != null) {
            int size = this.m_cachedNodes.size();
            if (size > i) {
                return this.m_dtm.getNode(this.m_cachedNodes.elementAt(i));
            }
            if (this.m_last == -1) {
                while (true) {
                    next = this.m_iter.next();
                    if (next == -1 || size > i) {
                        break;
                    }
                    this.m_cachedNodes.addElement(next);
                    size++;
                }
                if (next == -1) {
                    this.m_last = size;
                    return null;
                }
                return this.m_dtm.getNode(next);
            }
            return null;
        }
        return null;
    }

    @Override
    public int getLength() {
        if (this.m_last == -1) {
            while (true) {
                int next = this.m_iter.next();
                if (next == -1) {
                    break;
                }
                this.m_cachedNodes.addElement(next);
            }
            this.m_last = this.m_cachedNodes.size();
        }
        return this.m_last;
    }
}
