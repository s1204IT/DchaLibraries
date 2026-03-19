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

    DOM2DTMdefaultNamespaceDeclarationNode(Element pseudoparent, String prefix, String uri, int handle) {
        this.pseudoparent = pseudoparent;
        this.prefix = prefix;
        this.uri = uri;
        this.handle = handle;
        this.nodename = Constants.ATTRNAME_XMLNS + prefix;
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
    public boolean isSupported(String feature, String version) {
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
    public void setNodeValue(String value) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public void setValue(String value) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public void setPrefix(String value) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public Node insertBefore(Node a, Node b) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public Node replaceChild(Node a, Node b) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public Node appendChild(Node a) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public Node removeChild(Node a) {
        throw new DTMException("Unsupported operation on pseudonode");
    }

    @Override
    public Document getOwnerDocument() {
        return this.pseudoparent.getOwnerDocument();
    }

    @Override
    public Node cloneNode(boolean deep) {
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
    public boolean isDerivedFrom(String ns, String localName, int derivationMethod) {
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
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        return getOwnerDocument().setUserData(key, data, handler);
    }

    @Override
    public Object getUserData(String key) {
        return getOwnerDocument().getUserData(key);
    }

    @Override
    public Object getFeature(String feature, String version) {
        if (isSupported(feature, version)) {
            return this;
        }
        return null;
    }

    @Override
    public boolean isEqualNode(Node arg) {
        if (arg == this) {
            return true;
        }
        if (arg.getNodeType() != getNodeType()) {
            return false;
        }
        if (getNodeName() == null) {
            if (arg.getNodeName() != null) {
                return false;
            }
        } else if (!getNodeName().equals(arg.getNodeName())) {
            return false;
        }
        if (getLocalName() == null) {
            if (arg.getLocalName() != null) {
                return false;
            }
        } else if (!getLocalName().equals(arg.getLocalName())) {
            return false;
        }
        if (getNamespaceURI() == null) {
            if (arg.getNamespaceURI() != null) {
                return false;
            }
        } else if (!getNamespaceURI().equals(arg.getNamespaceURI())) {
            return false;
        }
        if (getPrefix() == null) {
            if (arg.getPrefix() != null) {
                return false;
            }
        } else if (!getPrefix().equals(arg.getPrefix())) {
            return false;
        }
        if (getNodeValue() == null) {
            if (arg.getNodeValue() != null) {
                return false;
            }
        } else if (!getNodeValue().equals(arg.getNodeValue())) {
            return false;
        }
        return true;
    }

    @Override
    public String lookupNamespaceURI(String specifiedPrefix) {
        short type = getNodeType();
        switch (type) {
            case 1:
                String namespace = getNamespaceURI();
                String prefix = getPrefix();
                if (namespace != null) {
                    if (specifiedPrefix == null && prefix == specifiedPrefix) {
                        return namespace;
                    }
                    if (prefix != null && prefix.equals(specifiedPrefix)) {
                        return namespace;
                    }
                }
                if (hasAttributes()) {
                    NamedNodeMap map = getAttributes();
                    int length = map.getLength();
                    for (int i = 0; i < length; i++) {
                        Node attr = map.item(i);
                        String attrPrefix = attr.getPrefix();
                        String value = attr.getNodeValue();
                        String namespace2 = attr.getNamespaceURI();
                        if (namespace2 != null && namespace2.equals(SerializerConstants.XMLNS_URI)) {
                            if (specifiedPrefix == null && attr.getNodeName().equals("xmlns")) {
                                return value;
                            }
                            if (attrPrefix != null && attrPrefix.equals("xmlns") && attr.getLocalName().equals(specifiedPrefix)) {
                                return value;
                            }
                        }
                    }
                }
                return null;
            case 2:
                if (getOwnerElement().getNodeType() == 1) {
                    return getOwnerElement().lookupNamespaceURI(specifiedPrefix);
                }
                return null;
            case 3:
            case 4:
            case 5:
            case 7:
            case 8:
            case 9:
            default:
                return null;
            case 6:
            case 10:
            case 11:
            case 12:
                return null;
        }
    }

    @Override
    public boolean isDefaultNamespace(String namespaceURI) {
        return false;
    }

    @Override
    public String lookupPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            return null;
        }
        short type = getNodeType();
        switch (type) {
            case 2:
                if (getOwnerElement().getNodeType() == 1) {
                }
                break;
        }
        return null;
    }

    @Override
    public boolean isSameNode(Node other) {
        return this == other;
    }

    @Override
    public void setTextContent(String textContent) throws DOMException {
        setNodeValue(textContent);
    }

    @Override
    public String getTextContent() throws DOMException {
        return getNodeValue();
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException {
        return (short) 0;
    }

    @Override
    public String getBaseURI() {
        return null;
    }
}
