package org.apache.xml.utils;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class PrefixResolverDefault implements PrefixResolver {
    Node m_context;

    public PrefixResolverDefault(Node node) {
        this.m_context = node;
    }

    @Override
    public String getNamespaceForPrefix(String str) {
        return getNamespaceForPrefix(str, this.m_context);
    }

    @Override
    public String getNamespaceForPrefix(String str, Node node) {
        if (str.equals("xml")) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        String nodeValue = null;
        while (node != null && nodeValue == null) {
            short nodeType = node.getNodeType();
            if (nodeType != 1 && nodeType != 5) {
                break;
            }
            if (nodeType == 1) {
                if (node.getNodeName().indexOf(str + ":") == 0) {
                    return node.getNamespaceURI();
                }
                NamedNodeMap attributes = node.getAttributes();
                int i = 0;
                while (true) {
                    if (i >= attributes.getLength()) {
                        break;
                    }
                    Node nodeItem = attributes.item(i);
                    String nodeName = nodeItem.getNodeName();
                    boolean zStartsWith = nodeName.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS);
                    if (zStartsWith || nodeName.equals("xmlns")) {
                        if ((zStartsWith ? nodeName.substring(nodeName.indexOf(58) + 1) : "").equals(str)) {
                            nodeValue = nodeItem.getNodeValue();
                            break;
                        }
                    }
                    i++;
                }
            }
            node = node.getParentNode();
        }
        return nodeValue;
    }

    @Override
    public String getBaseIdentifier() {
        return null;
    }

    @Override
    public boolean handlesNullPrefixes() {
        return false;
    }
}
