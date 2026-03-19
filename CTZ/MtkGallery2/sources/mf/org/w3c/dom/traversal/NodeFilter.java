package mf.org.w3c.dom.traversal;

import mf.org.w3c.dom.Node;

public interface NodeFilter {
    short acceptNode(Node node);
}
