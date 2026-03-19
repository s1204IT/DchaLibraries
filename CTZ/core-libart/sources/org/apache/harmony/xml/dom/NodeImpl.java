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
        public boolean isDerivedFrom(String str, String str2, int i) {
            return false;
        }
    };
    DocumentImpl document;

    @Override
    public abstract short getNodeType();

    NodeImpl(DocumentImpl documentImpl) {
        this.document = documentImpl;
    }

    @Override
    public Node appendChild(Node node) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    @Override
    public final Node cloneNode(boolean z) {
        return this.document.cloneOrImportNode((short) 1, this, z);
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
    public Node insertBefore(Node node, Node node2) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    @Override
    public boolean isSupported(String str, String str2) {
        return DOMImplementationImpl.getInstance().hasFeature(str, str2);
    }

    @Override
    public void normalize() {
    }

    @Override
    public Node removeChild(Node node) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    @Override
    public Node replaceChild(Node node, Node node2) throws DOMException {
        throw new DOMException((short) 3, null);
    }

    @Override
    public final void setNodeValue(String str) throws DOMException {
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
                ((Attr) this).setValue(str);
                return;
            case 3:
            case 4:
            case 8:
                ((CharacterData) this).setData(str);
                return;
            case 7:
                ((ProcessingInstruction) this).setData(str);
                return;
            default:
                throw new DOMException((short) 9, "Unsupported node type " + ((int) getNodeType()));
        }
    }

    @Override
    public void setPrefix(String str) throws DOMException {
    }

    static String validatePrefix(String str, boolean z, String str2) {
        if (!z) {
            throw new DOMException((short) 14, str);
        }
        if (str != null && (str2 == null || !DocumentImpl.isXMLIdentifier(str) || ((XMLConstants.XML_NS_PREFIX.equals(str) && !"http://www.w3.org/XML/1998/namespace".equals(str2)) || (XMLConstants.XMLNS_ATTRIBUTE.equals(str) && !XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(str2))))) {
            throw new DOMException((short) 14, str);
        }
        return str;
    }

    static void setNameNS(NodeImpl nodeImpl, String str, String str2) {
        if (str2 == null) {
            throw new DOMException((short) 14, str2);
        }
        String strValidatePrefix = null;
        int iLastIndexOf = str2.lastIndexOf(":");
        if (iLastIndexOf != -1) {
            strValidatePrefix = validatePrefix(str2.substring(0, iLastIndexOf), true, str);
            str2 = str2.substring(iLastIndexOf + 1);
        }
        if (!DocumentImpl.isXMLIdentifier(str2)) {
            throw new DOMException((short) 5, str2);
        }
        switch (nodeImpl.getNodeType()) {
            case 1:
                ElementImpl elementImpl = (ElementImpl) nodeImpl;
                elementImpl.namespaceAware = true;
                elementImpl.namespaceURI = str;
                elementImpl.prefix = strValidatePrefix;
                elementImpl.localName = str2;
                return;
            case 2:
                if (XMLConstants.XMLNS_ATTRIBUTE.equals(str2) && !XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(str)) {
                    throw new DOMException((short) 14, str2);
                }
                AttrImpl attrImpl = (AttrImpl) nodeImpl;
                attrImpl.namespaceAware = true;
                attrImpl.namespaceURI = str;
                attrImpl.prefix = strValidatePrefix;
                attrImpl.localName = str2;
                return;
            default:
                throw new DOMException((short) 9, "Cannot rename nodes of type " + ((int) nodeImpl.getNodeType()));
        }
    }

    static void setName(NodeImpl nodeImpl, String str) {
        int iLastIndexOf = str.lastIndexOf(":");
        if (iLastIndexOf != -1) {
            String strSubstring = str.substring(0, iLastIndexOf);
            String strSubstring2 = str.substring(iLastIndexOf + 1);
            if (!DocumentImpl.isXMLIdentifier(strSubstring) || !DocumentImpl.isXMLIdentifier(strSubstring2)) {
                throw new DOMException((short) 5, str);
            }
        } else if (!DocumentImpl.isXMLIdentifier(str)) {
            throw new DOMException((short) 5, str);
        }
        switch (nodeImpl.getNodeType()) {
            case 1:
                ElementImpl elementImpl = (ElementImpl) nodeImpl;
                elementImpl.namespaceAware = false;
                elementImpl.localName = str;
                return;
            case 2:
                AttrImpl attrImpl = (AttrImpl) nodeImpl;
                attrImpl.namespaceAware = false;
                attrImpl.localName = str;
                return;
            default:
                throw new DOMException((short) 9, "Cannot rename nodes of type " + ((int) nodeImpl.getNodeType()));
        }
    }

    @Override
    public final String getBaseURI() {
        switch (getNodeType()) {
            case 1:
                String attributeNS = ((Element) this).getAttributeNS("http://www.w3.org/XML/1998/namespace", "base");
                if (attributeNS != null) {
                    try {
                        if (!attributeNS.isEmpty()) {
                            if (new URI(attributeNS).isAbsolute()) {
                                return attributeNS;
                            }
                            String parentBaseUri = getParentBaseUri();
                            if (parentBaseUri == null) {
                                return null;
                            }
                            return new URI(parentBaseUri).resolve(attributeNS).toString();
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

    private String sanitizeUri(String str) {
        if (str == null || str.length() == 0) {
            return null;
        }
        try {
            return new URI(str).toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public short compareDocumentPosition(Node node) throws DOMException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTextContent() throws DOMException {
        return getNodeValue();
    }

    void getTextContent(StringBuilder sb) throws DOMException {
        String nodeValue = getNodeValue();
        if (nodeValue != null) {
            sb.append(nodeValue);
        }
    }

    @Override
    public final void setTextContent(String str) throws DOMException {
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
                setNodeValue(str);
                return;
            case 9:
            case 10:
                return;
            default:
                throw new DOMException((short) 9, "Unsupported node type " + ((int) getNodeType()));
        }
        while (true) {
            Node firstChild = getFirstChild();
            if (firstChild != null) {
                removeChild(firstChild);
            } else {
                if (str != null && str.length() != 0) {
                    appendChild(this.document.createTextNode(str));
                    return;
                }
                return;
            }
        }
    }

    @Override
    public boolean isSameNode(Node node) {
        return this == node;
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
        for (Node parentNode = getParentNode(); parentNode != null; parentNode = parentNode.getParentNode()) {
            if (parentNode.getNodeType() == 1) {
                return (NodeImpl) parentNode;
            }
        }
        return null;
    }

    @Override
    public final String lookupPrefix(String str) {
        if (str == null) {
            return null;
        }
        NodeImpl namespacingElement = getNamespacingElement();
        for (NodeImpl containingElement = namespacingElement; containingElement != null; containingElement = containingElement.getContainingElement()) {
            if (str.equals(containingElement.getNamespaceURI()) && namespacingElement.isPrefixMappedToUri(containingElement.getPrefix(), str)) {
                return containingElement.getPrefix();
            }
            if (containingElement.hasAttributes()) {
                NamedNodeMap attributes = containingElement.getAttributes();
                int length = attributes.getLength();
                for (int i = 0; i < length; i++) {
                    Node nodeItem = attributes.item(i);
                    if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(nodeItem.getNamespaceURI()) && XMLConstants.XMLNS_ATTRIBUTE.equals(nodeItem.getPrefix()) && str.equals(nodeItem.getNodeValue()) && namespacingElement.isPrefixMappedToUri(nodeItem.getLocalName(), str)) {
                        return nodeItem.getLocalName();
                    }
                }
            }
        }
        return null;
    }

    boolean isPrefixMappedToUri(String str, String str2) {
        if (str == null) {
            return false;
        }
        return str2.equals(lookupNamespaceURI(str));
    }

    @Override
    public final boolean isDefaultNamespace(String str) {
        String strLookupNamespaceURI = lookupNamespaceURI(null);
        if (str == null) {
            return strLookupNamespaceURI == null;
        }
        return str.equals(strLookupNamespaceURI);
    }

    @Override
    public final String lookupNamespaceURI(String str) {
        String nodeValue;
        for (NodeImpl namespacingElement = getNamespacingElement(); namespacingElement != null; namespacingElement = namespacingElement.getContainingElement()) {
            String prefix = namespacingElement.getPrefix();
            if (namespacingElement.getNamespaceURI() != null) {
                if (str == null) {
                    if (prefix == null) {
                        return namespacingElement.getNamespaceURI();
                    }
                } else if (str.equals(prefix)) {
                    return namespacingElement.getNamespaceURI();
                }
            }
            if (namespacingElement.hasAttributes()) {
                NamedNodeMap attributes = namespacingElement.getAttributes();
                int length = attributes.getLength();
                for (int i = 0; i < length; i++) {
                    Node nodeItem = attributes.item(i);
                    if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(nodeItem.getNamespaceURI())) {
                        if (str == null) {
                            if (XMLConstants.XMLNS_ATTRIBUTE.equals(nodeItem.getNodeName())) {
                                nodeValue = nodeItem.getNodeValue();
                                if (nodeValue.length() <= 0) {
                                    return nodeValue;
                                }
                                return null;
                            }
                        } else if (XMLConstants.XMLNS_ATTRIBUTE.equals(nodeItem.getPrefix()) && str.equals(nodeItem.getLocalName())) {
                            nodeValue = nodeItem.getNodeValue();
                            if (nodeValue.length() <= 0) {
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static List<Object> createEqualityKey(Node node) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(Short.valueOf(node.getNodeType()));
        arrayList.add(node.getNodeName());
        arrayList.add(node.getLocalName());
        arrayList.add(node.getNamespaceURI());
        arrayList.add(node.getPrefix());
        arrayList.add(node.getNodeValue());
        for (Node firstChild = node.getFirstChild(); firstChild != null; firstChild = firstChild.getNextSibling()) {
            arrayList.add(firstChild);
        }
        short nodeType = node.getNodeType();
        if (nodeType == 1) {
            arrayList.add(((Element) node).getAttributes());
        } else if (nodeType == 10) {
            DocumentTypeImpl documentTypeImpl = (DocumentTypeImpl) node;
            arrayList.add(documentTypeImpl.getPublicId());
            arrayList.add(documentTypeImpl.getSystemId());
            arrayList.add(documentTypeImpl.getInternalSubset());
            arrayList.add(documentTypeImpl.getEntities());
            arrayList.add(documentTypeImpl.getNotations());
        }
        return arrayList;
    }

    @Override
    public final boolean isEqualNode(Node node) {
        if (node == this) {
            return true;
        }
        List<Object> listCreateEqualityKey = createEqualityKey(this);
        List<Object> listCreateEqualityKey2 = createEqualityKey(node);
        if (listCreateEqualityKey.size() != listCreateEqualityKey2.size()) {
            return false;
        }
        for (int i = 0; i < listCreateEqualityKey.size(); i++) {
            Object obj = listCreateEqualityKey.get(i);
            Object obj2 = listCreateEqualityKey2.get(i);
            if (obj != obj2) {
                if (obj == null || obj2 == null) {
                    return false;
                }
                if ((obj instanceof String) || (obj instanceof Short)) {
                    if (!obj.equals(obj2)) {
                        return false;
                    }
                } else if (obj instanceof NamedNodeMap) {
                    if (!(obj2 instanceof NamedNodeMap) || !namedNodeMapsEqual((NamedNodeMap) obj, (NamedNodeMap) obj2)) {
                        return false;
                    }
                } else if (obj instanceof Node) {
                    if (!(obj2 instanceof Node) || !((Node) obj).isEqualNode((Node) obj2)) {
                        return false;
                    }
                } else {
                    throw new AssertionError();
                }
            }
        }
        return true;
    }

    private boolean namedNodeMapsEqual(NamedNodeMap namedNodeMap, NamedNodeMap namedNodeMap2) {
        Node namedItemNS;
        if (namedNodeMap.getLength() != namedNodeMap2.getLength()) {
            return false;
        }
        for (int i = 0; i < namedNodeMap.getLength(); i++) {
            Node nodeItem = namedNodeMap.item(i);
            if (nodeItem.getLocalName() == null) {
                namedItemNS = namedNodeMap2.getNamedItem(nodeItem.getNodeName());
            } else {
                namedItemNS = namedNodeMap2.getNamedItemNS(nodeItem.getNamespaceURI(), nodeItem.getLocalName());
            }
            if (namedItemNS == null || !nodeItem.isEqualNode(namedItemNS)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final Object getFeature(String str, String str2) {
        if (isSupported(str, str2)) {
            return this;
        }
        return null;
    }

    @Override
    public final Object setUserData(String str, Object obj, UserDataHandler userDataHandler) {
        UserData userDataPut;
        if (str == null) {
            throw new NullPointerException("key == null");
        }
        Map<String, UserData> userDataMap = this.document.getUserDataMap(this);
        if (obj == null) {
            userDataPut = userDataMap.remove(str);
        } else {
            userDataPut = userDataMap.put(str, new UserData(obj, userDataHandler));
        }
        if (userDataPut != null) {
            return userDataPut.value;
        }
        return null;
    }

    @Override
    public final Object getUserData(String str) {
        if (str == null) {
            throw new NullPointerException("key == null");
        }
        UserData userData = this.document.getUserDataMapForRead(this).get(str);
        if (userData != null) {
            return userData.value;
        }
        return null;
    }

    static class UserData {
        final UserDataHandler handler;
        final Object value;

        UserData(Object obj, UserDataHandler userDataHandler) {
            this.value = obj;
            this.handler = userDataHandler;
        }
    }
}
