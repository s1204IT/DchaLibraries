package org.apache.harmony.xml.dom;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

public abstract class NodeImpl implements Node {
    private static final NodeList EMPTY_LIST = new NodeListImpl();
    static final TypeInfo NULL_TYPE_INFO = new TypeInfo() {
        @Override
        public String getTypeName() {
            return null;
        }

        @Override
        public String getTypeNamespace() {
            return null;
        }

        @Override
        public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod) {
            return false;
        }
    };
    DocumentImpl document;

    @Override
    public abstract short getNodeType();

    NodeImpl(DocumentImpl document) {
        this.document = document;
    }

    @Override
    public Node appendChild(Node newChild) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    @Override
    public final Node cloneNode(boolean deep) {
        return this.document.cloneOrImportNode((short) 1, this, deep);
    }

    @Override
    public NamedNodeMap getAttributes() {
        return null;
    }

    @Override
    public NodeList getChildNodes() {
        return EMPTY_LIST;
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
    public String getLocalName() {
        return null;
    }

    @Override
    public String getNamespaceURI() {
        return null;
    }

    @Override
    public Node getNextSibling() {
        return null;
    }

    @Override
    public String getNodeName() {
        return null;
    }

    @Override
    public String getNodeValue() throws DOMException {
        return null;
    }

    @Override
    public final Document getOwnerDocument() {
        if (this.document == this) {
            return null;
        }
        return this.document;
    }

    @Override
    public Node getParentNode() {
        return null;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public Node getPreviousSibling() {
        return null;
    }

    @Override
    public boolean hasAttributes() {
        return false;
    }

    @Override
    public boolean hasChildNodes() {
        return false;
    }

    @Override
    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    @Override
    public boolean isSupported(String feature, String version) {
        return DOMImplementationImpl.getInstance().hasFeature(feature, version);
    }

    @Override
    public void normalize() {
    }

    @Override
    public Node removeChild(Node oldChild) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    @Override
    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    @Override
    public final void setNodeValue(String nodeValue) throws DOMException {
        switch (getNodeType()) {
            case 1:
            case 5:
            case 6:
            case 9:
            case 10:
            case 11:
            case 12:
                return;
            case 2:
                ((Attr) this).setValue(nodeValue);
                return;
            case 3:
            case 4:
            case 8:
                ((CharacterData) this).setData(nodeValue);
                return;
            case 7:
                ((ProcessingInstruction) this).setData(nodeValue);
                return;
            default:
                throw new DOMException((short) 9, "Unsupported node type " + ((int) getNodeType()));
        }
    }

    @Override
    public void setPrefix(String prefix) throws DOMException {
    }

    static String validatePrefix(String prefix, boolean namespaceAware, String namespaceURI) {
        if (!namespaceAware) {
            throw new DOMException((short) 14, prefix);
        }
        if (prefix != null && (namespaceURI == null || !DocumentImpl.isXMLIdentifier(prefix) || ((XMLConstants.XML_NS_PREFIX.equals(prefix) && !"http://www.w3.org/XML/1998/namespace".equals(namespaceURI)) || (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix) && !XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI))))) {
            throw new DOMException((short) 14, prefix);
        }
        return prefix;
    }

    static void setNameNS(NodeImpl node, String namespaceURI, String qualifiedName) {
        if (qualifiedName == null) {
            throw new DOMException((short) 14, qualifiedName);
        }
        String prefix = null;
        int p = qualifiedName.lastIndexOf(":");
        if (p != -1) {
            prefix = validatePrefix(qualifiedName.substring(0, p), true, namespaceURI);
            qualifiedName = qualifiedName.substring(p + 1);
        }
        if (!DocumentImpl.isXMLIdentifier(qualifiedName)) {
            throw new DOMException((short) 5, qualifiedName);
        }
        switch (node.getNodeType()) {
            case 1:
                ElementImpl element = (ElementImpl) node;
                element.namespaceAware = true;
                element.namespaceURI = namespaceURI;
                element.prefix = prefix;
                element.localName = qualifiedName;
                return;
            case 2:
                if (XMLConstants.XMLNS_ATTRIBUTE.equals(qualifiedName) && !XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
                    throw new DOMException((short) 14, qualifiedName);
                }
                AttrImpl attr = (AttrImpl) node;
                attr.namespaceAware = true;
                attr.namespaceURI = namespaceURI;
                attr.prefix = prefix;
                attr.localName = qualifiedName;
                return;
            default:
                throw new DOMException((short) 9, "Cannot rename nodes of type " + ((int) node.getNodeType()));
        }
    }

    static void setName(NodeImpl node, String name) {
        int prefixSeparator = name.lastIndexOf(":");
        if (prefixSeparator != -1) {
            String prefix = name.substring(0, prefixSeparator);
            String localName = name.substring(prefixSeparator + 1);
            if (!DocumentImpl.isXMLIdentifier(prefix) || !DocumentImpl.isXMLIdentifier(localName)) {
                throw new DOMException((short) 5, name);
            }
        } else if (!DocumentImpl.isXMLIdentifier(name)) {
            throw new DOMException((short) 5, name);
        }
        switch (node.getNodeType()) {
            case 1:
                ElementImpl element = (ElementImpl) node;
                element.namespaceAware = false;
                element.localName = name;
                return;
            case 2:
                AttrImpl attr = (AttrImpl) node;
                attr.namespaceAware = false;
                attr.localName = name;
                return;
            default:
                throw new DOMException((short) 9, "Cannot rename nodes of type " + ((int) node.getNodeType()));
        }
    }

    @Override
    public final String getBaseURI() {
        switch (getNodeType()) {
            case 1:
                Element element = (Element) this;
                String uri = element.getAttributeNS("http://www.w3.org/XML/1998/namespace", "base");
                if (uri != null) {
                    try {
                        if (!uri.isEmpty()) {
                            if (new URI(uri).isAbsolute()) {
                                return uri;
                            }
                            String parentUri = getParentBaseUri();
                            if (parentUri == null) {
                                return null;
                            }
                            return new URI(parentUri).resolve(uri).toString();
                        }
                    } catch (URISyntaxException e) {
                        return null;
                    }
                }
                return getParentBaseUri();
            case 2:
            case 3:
            case 4:
            case 8:
            case 10:
            case 11:
                return null;
            case 5:
                return null;
            case 6:
            case 12:
                return null;
            case 7:
                return getParentBaseUri();
            case 9:
                return sanitizeUri(((Document) this).getDocumentURI());
            default:
                throw new DOMException((short) 9, "Unsupported node type " + ((int) getNodeType()));
        }
    }

    private String getParentBaseUri() {
        Node parentNode = getParentNode();
        if (parentNode != null) {
            return parentNode.getBaseURI();
        }
        return null;
    }

    private String sanitizeUri(String uri) {
        if (uri == null || uri.length() == 0) {
            return null;
        }
        try {
            return new URI(uri).toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTextContent() throws DOMException {
        return getNodeValue();
    }

    void getTextContent(StringBuilder buf) throws DOMException {
        String content = getNodeValue();
        if (content == null) {
            return;
        }
        buf.append(content);
    }

    @Override
    public final void setTextContent(String textContent) throws DOMException {
        switch (getNodeType()) {
            case 1:
            case 5:
            case 6:
            case 11:
                break;
            case 2:
            case 3:
            case 4:
            case 7:
            case 8:
            case 12:
                setNodeValue(textContent);
                return;
            case 9:
            case 10:
                return;
            default:
                throw new DOMException((short) 9, "Unsupported node type " + ((int) getNodeType()));
        }
        while (true) {
            Node child = getFirstChild();
            if (child != null) {
                removeChild(child);
            } else {
                if (textContent != null && textContent.length() != 0) {
                    appendChild(this.document.createTextNode(textContent));
                    return;
                }
                return;
            }
        }
    }

    @Override
    public boolean isSameNode(Node other) {
        return this == other;
    }

    private NodeImpl getNamespacingElement() {
        switch (getNodeType()) {
            case 1:
                return this;
            case 2:
                return (NodeImpl) ((Attr) this).getOwnerElement();
            case 3:
            case 4:
            case 5:
            case 7:
            case 8:
                return getContainingElement();
            case 6:
            case 10:
            case 11:
            case 12:
                return null;
            case 9:
                return (NodeImpl) ((Document) this).getDocumentElement();
            default:
                throw new DOMException((short) 9, "Unsupported node type " + ((int) getNodeType()));
        }
    }

    private NodeImpl getContainingElement() {
        for (Node p = getParentNode(); p != null; p = p.getParentNode()) {
            if (p.getNodeType() == 1) {
                return (NodeImpl) p;
            }
        }
        return null;
    }

    @Override
    public final String lookupPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            return null;
        }
        NodeImpl target = getNamespacingElement();
        for (NodeImpl node = target; node != null; node = node.getContainingElement()) {
            if (namespaceURI.equals(node.getNamespaceURI()) && target.isPrefixMappedToUri(node.getPrefix(), namespaceURI)) {
                return node.getPrefix();
            }
            if (node.hasAttributes()) {
                NamedNodeMap attributes = node.getAttributes();
                int length = attributes.getLength();
                for (int i = 0; i < length; i++) {
                    Node attr = attributes.item(i);
                    if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI()) && XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getPrefix()) && namespaceURI.equals(attr.getNodeValue()) && target.isPrefixMappedToUri(attr.getLocalName(), namespaceURI)) {
                        return attr.getLocalName();
                    }
                }
            }
        }
        return null;
    }

    boolean isPrefixMappedToUri(String prefix, String uri) {
        if (prefix == null) {
            return false;
        }
        String actual = lookupNamespaceURI(prefix);
        return uri.equals(actual);
    }

    @Override
    public final boolean isDefaultNamespace(String namespaceURI) {
        String actual = lookupNamespaceURI(null);
        if (namespaceURI == null) {
            return actual == null;
        }
        return namespaceURI.equals(actual);
    }

    @Override
    public final String lookupNamespaceURI(String prefix) {
        boolean zEquals;
        NodeImpl target = getNamespacingElement();
        for (NodeImpl node = target; node != null; node = node.getContainingElement()) {
            String nodePrefix = node.getPrefix();
            if (node.getNamespaceURI() != null) {
                if (prefix != null) {
                    if (prefix.equals(nodePrefix)) {
                        return node.getNamespaceURI();
                    }
                } else if (nodePrefix == null) {
                    return node.getNamespaceURI();
                }
            }
            if (node.hasAttributes()) {
                NamedNodeMap attributes = node.getAttributes();
                int length = attributes.getLength();
                for (int i = 0; i < length; i++) {
                    Node attr = attributes.item(i);
                    if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) {
                        if (prefix == null) {
                            zEquals = XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getNodeName());
                        } else if (XMLConstants.XMLNS_ATTRIBUTE.equals(attr.getPrefix())) {
                            zEquals = prefix.equals(attr.getLocalName());
                        } else {
                            continue;
                        }
                        if (zEquals) {
                            String value = attr.getNodeValue();
                            if (value.length() > 0) {
                                return value;
                            }
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static List<Object> createEqualityKey(Node node) {
        List<Object> values = new ArrayList<>();
        values.add(Short.valueOf(node.getNodeType()));
        values.add(node.getNodeName());
        values.add(node.getLocalName());
        values.add(node.getNamespaceURI());
        values.add(node.getPrefix());
        values.add(node.getNodeValue());
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            values.add(child);
        }
        switch (node.getNodeType()) {
            case 1:
                Element element = (Element) node;
                values.add(element.getAttributes());
                return values;
            case 10:
                DocumentTypeImpl doctype = (DocumentTypeImpl) node;
                values.add(doctype.getPublicId());
                values.add(doctype.getSystemId());
                values.add(doctype.getInternalSubset());
                values.add(doctype.getEntities());
                values.add(doctype.getNotations());
                return values;
            default:
                return values;
        }
    }

    @Override
    public final boolean isEqualNode(Node arg) {
        if (arg == this) {
            return true;
        }
        List<Object> listA = createEqualityKey(this);
        List<Object> listB = createEqualityKey(arg);
        if (listA.size() != listB.size()) {
            return false;
        }
        for (int i = 0; i < listA.size(); i++) {
            Object a = listA.get(i);
            Object b = listB.get(i);
            if (a != b) {
                if (a == null || b == null) {
                    return false;
                }
                if ((a instanceof String) || (a instanceof Short)) {
                    if (!a.equals(b)) {
                        return false;
                    }
                } else if (a instanceof NamedNodeMap) {
                    if (!(b instanceof NamedNodeMap) || !namedNodeMapsEqual((NamedNodeMap) a, (NamedNodeMap) b)) {
                        return false;
                    }
                } else if (a instanceof Node) {
                    if (!(b instanceof Node) || !((Node) a).isEqualNode((Node) b)) {
                        return false;
                    }
                } else {
                    throw new AssertionError();
                }
            }
        }
        return true;
    }

    private boolean namedNodeMapsEqual(NamedNodeMap a, NamedNodeMap b) {
        Node bNode;
        if (a.getLength() != b.getLength()) {
            return false;
        }
        for (int i = 0; i < a.getLength(); i++) {
            Node aNode = a.item(i);
            if (aNode.getLocalName() == null) {
                bNode = b.getNamedItem(aNode.getNodeName());
            } else {
                bNode = b.getNamedItemNS(aNode.getNamespaceURI(), aNode.getLocalName());
            }
            if (bNode == null || !aNode.isEqualNode(bNode)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final Object getFeature(String feature, String version) {
        if (isSupported(feature, version)) {
            return this;
        }
        return null;
    }

    @Override
    public final Object setUserData(String key, Object data, UserDataHandler handler) {
        UserData previous;
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        Map<String, UserData> map = this.document.getUserDataMap(this);
        if (data == null) {
            previous = map.remove(key);
        } else {
            previous = map.put(key, new UserData(data, handler));
        }
        if (previous != null) {
            return previous.value;
        }
        return null;
    }

    @Override
    public final Object getUserData(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        Map<String, UserData> map = this.document.getUserDataMapForRead(this);
        UserData userData = map.get(key);
        if (userData != null) {
            return userData.value;
        }
        return null;
    }

    static class UserData {
        final UserDataHandler handler;
        final Object value;

        UserData(Object value, UserDataHandler handler) {
            this.value = value;
            this.handler = handler;
        }
    }
}
