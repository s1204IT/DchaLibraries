package mf.org.w3c.dom.ranges;

import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.Node;

public interface Range {
    Node getEndContainer() throws DOMException;

    int getEndOffset() throws DOMException;

    Node getStartContainer() throws DOMException;

    int getStartOffset() throws DOMException;

    void setEnd(Node node, int i) throws DOMException, RangeException;

    void setStart(Node node, int i) throws DOMException, RangeException;
}
