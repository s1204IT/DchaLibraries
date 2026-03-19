package org.apache.xml.dtm.ref;

import org.apache.xml.dtm.DTM;
import org.w3c.dom.Node;

public class DTMChildIterNodeList extends DTMNodeListBase {
    private int m_firstChild;
    private DTM m_parentDTM;

    private DTMChildIterNodeList() {
    }

    public DTMChildIterNodeList(DTM dtm, int i) {
        this.m_parentDTM = dtm;
        this.m_firstChild = dtm.getFirstChild(i);
    }

    @Override
    public Node item(int i) {
        int nextSibling = this.m_firstChild;
        while (true) {
            i--;
            if (i < 0 || nextSibling == -1) {
                break;
            }
            nextSibling = this.m_parentDTM.getNextSibling(nextSibling);
        }
        if (nextSibling == -1) {
            return null;
        }
        return this.m_parentDTM.getNode(nextSibling);
    }

    @Override
    public int getLength() {
        int nextSibling = this.m_firstChild;
        int i = 0;
        while (nextSibling != -1) {
            i++;
            nextSibling = this.m_parentDTM.getNextSibling(nextSibling);
        }
        return i;
    }
}
