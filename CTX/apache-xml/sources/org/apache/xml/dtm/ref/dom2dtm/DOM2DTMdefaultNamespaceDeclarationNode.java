package org.apache.xml.dtm.ref.dom2dtm;

import org.apache.xalan.templates.Constants;
import org.apache.xml.dtm.DTMException;
import org.apache.xml.serializer.SerializerConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

public class DOM2DTMdefaultNamespaceDeclarationNode implements Attr, TypeInfo {
    final String NOT_SUPPORTED_ERR = "Unsupported operation on pseudonode";
    int handle;
    String nodename;
    String prefix;
    Element pseudoparent;
    String uri;

    DOM2DTMdefaultNamespaceDeclarationNode(Element element, String str, String str2, int i) {
        this.pseudoparent = element;
        this.prefix = str;
        this.uri = str2;
        this.handle = i;
        this.nodename = Constants.ATTRNAME_XMLNS + str;
    }

    @Override
    public String getNodeName() {
        return this.nodename;
    }

    @Override
    public String getName() {
        return this.nodename;
    }

    @Override
    public String getNamespaceURI() {
        return SerializerConstants.XMLNS_URI;
    }

    @Override
    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public String getLocalName() {
        return this.prefix;
    }

    @Override
    public String getNodeValue() {
        return this.uri;
    }

    @Override
    public String getValue() {
        return this.uri;
    }

    @Override
    public Element getOwnerElement() {
        return this.pseudoparent;
    }

    @Override
    public boolean isSupported(String str, String str2) {
        return false;
    }

    @Override
    public boolean hasChildNodes() {
        return false;
    }

    @Override
    public boolean hasAttributes() {
        return false;
    }

    @Override
    public Node getParentNode() {
        return null;
    }

    @Override
    public Node getFirstChild() {
        return null;
    }

    @Override
    public Node getLastChild() {
        return null;
    }

    @Override
    public Node getPreviousSibling() {
        return null;
    }

    @Override
    public Node getNextSibling() {
        return null;
    }

    @Override
    public boolean getSpecified() {
        return false;
    }

    @Override
    public void normalize() {
    }

    @Override
    public NodeList getChildNodes() {
        return null;
    }

    @Override
    public NamedNodeMap getAttributes() {
        return null;
    }

    @Override
    public short getNodeType() {
        return (short) 2;
    }

    @Override
    public void setNodeValue(String str) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public void setValue(String str) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public void setPrefix(String str) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public Node insertBefore(Node node, Node node2) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public Node replaceChild(Node node, Node node2) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public Node appendChild(Node node) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public Node removeChild(Node node) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public Document getOwnerDocument() {
        return this.pseudoparent.getOwnerDocument();
    }

    @Override
    public Node cloneNode(boolean z) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    public int getHandleOfNode() {
        return this.handle;
    }

    @Override
    public String getTypeName() {
        return null;
    }

    @Override
    public String getTypeNamespace() {
        return null;
    }

    @Override
    public boolean isDerivedFrom(String str, String str2, int i) {
        return false;
    }

    @Override
    public TypeInfo getSchemaTypeInfo() {
        return this;
    }

    @Override
    public boolean isId() {
        return false;
    }

    @Override
    public Object setUserData(String str, Object obj, UserDataHandler userDataHandler) {
        return getOwnerDocument().setUserData(str, obj, userDataHandler);
    }

    @Override
    public Object getUserData(String str) {
        return getOwnerDocument().getUserData(str);
    }

    @Override
    public Object getFeature(String str, String str2) {
        if (isSupported(str, str2)) {
            return this;
        }
        return null;
    }

    @Override
    public boolean isEqualNode(Node node) {
        if (node == this) {
            return true;
        }
        if (node.getNodeType() != getNodeType()) {
            return false;
        }
        if (getNodeName() == null) {
            if (node.getNodeName() != null) {
                return false;
            }
        } else if (!getNodeName().equals(node.getNodeName())) {
            return false;
        }
        if (getLocalName() == null) {
            if (node.getLocalName() != null) {
                return false;
            }
        } else if (!getLocalName().equals(node.getLocalName())) {
            return false;
        }
        if (getNamespaceURI() == null) {
            if (node.getNamespaceURI() != null) {
                return false;
            }
        } else if (!getNamespaceURI().equals(node.getNamespaceURI())) {
            return false;
        }
        if (getPrefix() == null) {
            if (node.getPrefix() != null) {
                return false;
            }
        } else if (!getPrefix().equals(node.getPrefix())) {
            return false;
        }
        if (getNodeValue() == null) {
            if (node.getNodeValue() != null) {
                return false;
            }
        } else if (!getNodeValue().equals(node.getNodeValue())) {
            return false;
        }
        return true;
    }

    @Override
    public String lookupNamespaceURI(String str) {
        short nodeType = getNodeType();
        if (nodeType != 6) {
            switch (nodeType) {
                case 1:
                    String namespaceURI = getNamespaceURI();
                    String prefix = getPrefix();
                    if (namespaceURI != null) {
                        if (str == null && prefix == str) {
                            return namespaceURI;
                        }
                        if (prefix != null && prefix.equals(str)) {
                            return namespaceURI;
                        }
                    }
                    if (hasAttributes()) {
                        NamedNodeMap attributes = getAttributes();
                        int length = attributes.getLength();
                        for (int i = 0; i < length; i++) {
                            Node nodeItem = attributes.item(i);
                            String prefix2 = nodeItem.getPrefix();
                            String nodeValue = nodeItem.getNodeValue();
                            String namespaceURI2 = nodeItem.getNamespaceURI();
                            if (namespaceURI2 != null && namespaceURI2.equals(SerializerConstants.XMLNS_URI)) {
                                if (str == null && nodeItem.getNodeName().equals("xmlns")) {
                                    return nodeValue;
                                }
                                if (prefix2 != null && prefix2.equals("xmlns") && nodeItem.getLocalName().equals(str)) {
                                    return nodeValue;
                                }
                            }
                        }
                    }
                    return null;
                case 2:
                    if (getOwnerElement().getNodeType() == 1) {
                        return getOwnerElement().lookupNamespaceURI(str);
                    }
                    return null;
                default:
                    switch (nodeType) {
                        case 10:
                        case 11:
                        case 12:
                            break;
                        default:
                            return null;
                    }
                    break;
            }
        }
        return null;
    }

    @Override
    public boolean isDefaultNamespace(String str) {
        return false;
    }

    @Override
    public String lookupPrefix(String str) {
        if (str == null) {
            return null;
        }
        short nodeType = getNodeType();
        if (nodeType != 2) {
            if (nodeType != 6) {
                switch (nodeType) {
                }
                return null;
            }
            return null;
        }
        if (getOwnerElement().getNodeType() != 1) {
            return null;
        }
        return getOwnerElement().lookupPrefix(str);
    }

    @Override
    public boolean isSameNode(Node node) {
        return this == node;
    }

    @Override
    public void setTextContent(String str) throws DOMException {
        setNodeValue(str);
    }

    @Override
    public String getTextContent() throws DOMException {
        return getNodeValue();
    }

    @Override
    public short compareDocumentPosition(Node node) throws DOMException {
        return (short) 0;
    }

    @Override
    public String getBaseURI() {
        return null;
    }
}
