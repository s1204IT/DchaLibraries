package org.apache.xpath.jaxp;

import javax.xml.namespace.NamespaceContext;
import org.apache.xalan.templates.Constants;
import org.apache.xml.utils.PrefixResolver;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class JAXPPrefixResolver implements PrefixResolver {
    public static final String S_XMLNAMESPACEURI = "http://www.w3.org/XML/1998/namespace";
    private NamespaceContext namespaceContext;

    public JAXPPrefixResolver(NamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
    }

    @Override
    public String getNamespaceForPrefix(String str) {
        return this.namespaceContext.getNamespaceURI(str);
    }

    @Override
    public String getBaseIdentifier() {
        return null;
    }

    @Override
    public boolean handlesNullPrefixes() {
        return false;
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
                NamedNodeMap attributes = node.getAttributes();
                int i = 0;
                while (true) {
                    if (i >= attributes.getLength()) {
                        break;
                    }
                    Node nodeItem = attributes.item(i);
                    String nodeName = nodeItem.getNodeName();
                    boolean zStartsWith = nodeName.startsWith(Constants.ATTRNAME_XMLNS);
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
}
