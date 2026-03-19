package org.apache.xml.serializer.utils;

import org.w3c.dom.Node;

public final class DOM2Helper {
    public String getLocalNameOfNode(Node node) {
        String localName = node.getLocalName();
        return localName == null ? getLocalNameOfNodeFallback(node) : localName;
    }

    private String getLocalNameOfNodeFallback(Node node) {
        String nodeName = node.getNodeName();
        int iIndexOf = nodeName.indexOf(58);
        return iIndexOf < 0 ? nodeName : nodeName.substring(iIndexOf + 1);
    }

    public String getNamespaceOfNode(Node node) {
        return node.getNamespaceURI();
    }
}
