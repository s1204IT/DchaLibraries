package org.apache.xml.dtm.ref;

import org.apache.xml.dtm.DTMIterator;
import org.w3c.dom.Node;

public class DTMNodeList extends DTMNodeListBase {
    private DTMIterator m_iter;

    private DTMNodeList() {
    }

    public DTMNodeList(DTMIterator dTMIterator) {
        if (dTMIterator != null) {
            int currentPos = dTMIterator.getCurrentPos();
            try {
                this.m_iter = dTMIterator.cloneWithReset();
            } catch (CloneNotSupportedException e) {
                this.m_iter = dTMIterator;
            }
            this.m_iter.setShouldCacheNodes(true);
            this.m_iter.runTo(-1);
            this.m_iter.setCurrentPos(currentPos);
        }
    }

    public DTMIterator getDTMIterator() {
        return this.m_iter;
    }

    @Override
    public Node item(int i) {
        int iItem;
        if (this.m_iter == null || (iItem = this.m_iter.item(i)) == -1) {
            return null;
        }
        return this.m_iter.getDTM(iItem).getNode(iItem);
    }

    @Override
    public int getLength() {
        if (this.m_iter != null) {
            return this.m_iter.getLength();
        }
        return 0;
    }
}
