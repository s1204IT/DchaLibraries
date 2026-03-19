package mf.org.w3c.dom.ls;

import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.Node;

public interface LSSerializer {
    String writeToString(Node node) throws LSException, DOMException;
}
