package org.apache.xpath.jaxp;

import javax.xml.namespace.NamespaceContext;
import org.apache.xalan.templates.Constants;
import org.apache.xml.utils.PrefixResolver;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class JAXPPrefixResolver implements PrefixResolver {
    public static final String S_XMLNAMESPACEURI = "http://www.w3.org/XML/1998/namespace";
    private NamespaceContext namespaceContext;

    public JAXPPrefixResolver(NamespaceContext nsContext) {
        this.namespaceContext = nsContext;
    }

    @Override
    public String getNamespaceForPrefix(String prefix) {
        return this.namespaceContext.getNamespaceURI(prefix);
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
    public String getNamespaceForPrefix(String prefix, Node namespaceContext) {
        String namespace = null;
        if (prefix.equals("xml")) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        for (Node parent = namespaceContext; parent != null && namespace == null; parent = parent.getParentNode()) {
            int type = parent.getNodeType();
            if (type == 1 || type == 5) {
                if (type == 1) {
                    NamedNodeMap nnm = parent.getAttributes();
                    int i = 0;
                    while (true) {
                        if (i >= nnm.getLength()) {
                            break;
                        }
                        Node attr = nnm.item(i);
                        String aname = attr.getNodeName();
                        boolean isPrefix = aname.startsWith(Constants.ATTRNAME_XMLNS);
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
            } else {
                return namespace;
            }
        }
        return namespace;
    }
}
