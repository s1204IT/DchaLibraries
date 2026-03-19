package org.apache.xml.dtm.ref;

import org.apache.xml.dtm.DTMDOMException;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.WrappedRuntimeException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

public class DTMNodeIterator implements NodeIterator {
    private DTMIterator dtm_iter;
    private boolean valid = true;

    public DTMNodeIterator(DTMIterator dTMIterator) {
        try {
            this.dtm_iter = (DTMIterator) dTMIterator.clone();
        } catch (CloneNotSupportedException e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public DTMIterator getDTMIterator() {
        return this.dtm_iter;
    }

    public void detach() {
        this.valid = false;
    }

    public boolean getExpandEntityReferences() {
        return false;
    }

    public NodeFilter getFilter() {
        throw new DTMDOMException((short) 9);
    }

    public Node getRoot() {
        int root = this.dtm_iter.getRoot();
        return this.dtm_iter.getDTM(root).getNode(root);
    }

    public int getWhatToShow() {
        return this.dtm_iter.getWhatToShow();
    }

    public Node nextNode() throws DOMException {
        if (!this.valid) {
            throw new DTMDOMException((short) 11);
        }
        int iNextNode = this.dtm_iter.nextNode();
        if (iNextNode == -1) {
            return null;
        }
        return this.dtm_iter.getDTM(iNextNode).getNode(iNextNode);
    }

    public Node previousNode() {
        if (!this.valid) {
            throw new DTMDOMException((short) 11);
        }
        int iPreviousNode = this.dtm_iter.previousNode();
        if (iPreviousNode == -1) {
            return null;
        }
        return this.dtm_iter.getDTM(iPreviousNode).getNode(iPreviousNode);
    }
}
