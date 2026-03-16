package org.apache.xml.utils;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class PrefixResolverDefault implements PrefixResolver {
    Node m_context;

    public PrefixResolverDefault(Node xpathExpressionContext) {
        this.m_context = xpathExpressionContext;
    }

    @Override
    public String getNamespaceForPrefix(String prefix) {
        return getNamespaceForPrefix(prefix, this.m_context);
    }

    @Override
    public String getNamespaceForPrefix(String prefix, Node namespaceContext) {
        String namespace = null;
        if (prefix.equals("xml")) {
            namespace = "http://www.w3.org/XML/1998/namespace";
        } else {
            for (Node parent = namespaceContext; parent != null && namespace == null; parent = parent.getParentNode()) {
                int type = parent.getNodeType();
                if (type != 1 && type != 5) {
                    break;
                }
                if (type == 1) {
                    if (parent.getNodeName().indexOf(prefix + ":") == 0) {
                        return parent.getNamespaceURI();
                    }
                    NamedNodeMap nnm = parent.getAttributes();
                    int i = 0;
                    while (true) {
                        if (i >= nnm.getLength()) {
                            break;
                        }
                        Node attr = nnm.item(i);
                        String aname = attr.getNodeName();
                        boolean isPrefix = aname.startsWith(org.apache.xalan.templates.Constants.ATTRNAME_XMLNS);
                        if (isPrefix || aname.equals("xmlns")) {
                            int index = aname.indexOf(58);
                            String p = isPrefix ? aname.substring(index + 1) : "";
                            if (p.equals(prefix)) {
                                namespace = attr.getNodeValue();
                                break;
                            }
                        }
                        i++;
                    }
                }
            }
        }
        return namespace;
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
