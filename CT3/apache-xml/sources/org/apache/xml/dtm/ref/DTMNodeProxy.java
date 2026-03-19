package org.apache.xml.dtm.ref;

import java.util.Vector;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMDOMException;
import org.apache.xml.serializer.SerializerConstants;
import org.apache.xpath.NodeSet;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

public class DTMNodeProxy implements Node, Document, Text, Element, Attr, ProcessingInstruction, Comment, DocumentFragment {
    private static final String EMPTYSTRING = "";
    static final DOMImplementation implementation = new DTMNodeProxyImplementation();
    protected String actualEncoding;
    public DTM dtm;
    protected String fDocumentURI;
    int node;
    private String xmlEncoding;
    private boolean xmlStandalone;
    private String xmlVersion;

    public DTMNodeProxy(DTM dtm, int node) {
        this.dtm = dtm;
        this.node = node;
    }

    public final DTM getDTM() {
        return this.dtm;
    }

    public final int getDTMNodeNumber() {
        return this.node;
    }

    public final boolean equals(Node node) {
        try {
            DTMNodeProxy dtmp = (DTMNodeProxy) node;
            if (dtmp.node == this.node) {
                return dtmp.dtm == this.dtm;
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public final boolean equals(Object node) {
        try {
            return equals((Node) node);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public final boolean sameNodeAs(Node node) {
        return (node instanceof DTMNodeProxy) && this.dtm == node.dtm && this.node == node.node;
    }

    @Override
    public final String getNodeName() {
        return this.dtm.getNodeName(this.node);
    }

    @Override
    public final String getTarget() {
        return this.dtm.getNodeName(this.node);
    }

    @Override
    public final String getLocalName() {
        return this.dtm.getLocalName(this.node);
    }

    @Override
    public final String getPrefix() {
        return this.dtm.getPrefix(this.node);
    }

    @Override
    public final void setPrefix(String prefix) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final String getNamespaceURI() {
        return this.dtm.getNamespaceURI(this.node);
    }

    public final boolean supports(String feature, String version) {
        return implementation.hasFeature(feature, version);
    }

    @Override
    public final boolean isSupported(String feature, String version) {
        return implementation.hasFeature(feature, version);
    }

    @Override
    public final String getNodeValue() throws DOMException {
        return this.dtm.getNodeValue(this.node);
    }

    public final String getStringValue() throws DOMException {
        return this.dtm.getStringValue(this.node).toString();
    }

    @Override
    public final void setNodeValue(String nodeValue) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final short getNodeType() {
        return this.dtm.getNodeType(this.node);
    }

    @Override
    public final Node getParentNode() {
        int newnode;
        if (getNodeType() == 2 || (newnode = this.dtm.getParent(this.node)) == -1) {
            return null;
        }
        return this.dtm.getNode(newnode);
    }

    public final Node getOwnerNode() {
        int newnode = this.dtm.getParent(this.node);
        if (newnode == -1) {
            return null;
        }
        return this.dtm.getNode(newnode);
    }

    @Override
    public final NodeList getChildNodes() {
        return new DTMChildIterNodeList(this.dtm, this.node);
    }

    @Override
    public final Node getFirstChild() {
        int newnode = this.dtm.getFirstChild(this.node);
        if (newnode == -1) {
            return null;
        }
        return this.dtm.getNode(newnode);
    }

    @Override
    public final Node getLastChild() {
        int newnode = this.dtm.getLastChild(this.node);
        if (newnode == -1) {
            return null;
        }
        return this.dtm.getNode(newnode);
    }

    @Override
    public final Node getPreviousSibling() {
        int newnode = this.dtm.getPreviousSibling(this.node);
        if (newnode == -1) {
            return null;
        }
        return this.dtm.getNode(newnode);
    }

    @Override
    public final Node getNextSibling() {
        int newnode;
        if (this.dtm.getNodeType(this.node) == 2 || (newnode = this.dtm.getNextSibling(this.node)) == -1) {
            return null;
        }
        return this.dtm.getNode(newnode);
    }

    @Override
    public final NamedNodeMap getAttributes() {
        return new DTMNamedNodeMap(this.dtm, this.node);
    }

    @Override
    public boolean hasAttribute(String name) {
        return -1 != this.dtm.getAttributeNode(this.node, null, name);
    }

    @Override
    public boolean hasAttributeNS(String namespaceURI, String localName) {
        return -1 != this.dtm.getAttributeNode(this.node, namespaceURI, localName);
    }

    @Override
    public final Document getOwnerDocument() {
        return (Document) this.dtm.getNode(this.dtm.getOwnerDocument(this.node));
    }

    @Override
    public final Node insertBefore(Node newChild, Node refChild) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final Node removeChild(Node oldChild) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final Node appendChild(Node newChild) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final boolean hasChildNodes() {
        return -1 != this.dtm.getFirstChild(this.node);
    }

    @Override
    public final Node cloneNode(boolean deep) {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final DocumentType getDoctype() {
        return null;
    }

    @Override
    public final DOMImplementation getImplementation() {
        return implementation;
    }

    @Override
    public final Element getDocumentElement() {
        int dochandle = this.dtm.getDocument();
        int elementhandle = -1;
        int kidhandle = this.dtm.getFirstChild(dochandle);
        while (kidhandle != -1) {
            switch (this.dtm.getNodeType(kidhandle)) {
                case 1:
                    if (elementhandle != -1) {
                        elementhandle = -1;
                        kidhandle = this.dtm.getLastChild(dochandle);
                    } else {
                        elementhandle = kidhandle;
                    }
                    break;
                case 7:
                case 8:
                case 10:
                    break;
                default:
                    elementhandle = -1;
                    kidhandle = this.dtm.getLastChild(dochandle);
                    break;
            }
            kidhandle = this.dtm.getNextSibling(kidhandle);
        }
        if (elementhandle == -1) {
            throw new DTMDOMException((short) 9);
        }
        return (Element) this.dtm.getNode(elementhandle);
    }

    @Override
    public final Element createElement(String tagName) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final DocumentFragment createDocumentFragment() {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Text createTextNode(String data) {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Comment createComment(String data) {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final CDATASection createCDATASection(String data) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Attr createAttribute(String name) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final EntityReference createEntityReference(String name) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final NodeList getElementsByTagName(String tagname) {
        Vector listVector = new Vector();
        Node retNode = this.dtm.getNode(this.node);
        if (retNode != null) {
            boolean isTagNameWildCard = "*".equals(tagname);
            if (1 == retNode.getNodeType()) {
                NodeList nodeList = retNode.getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    traverseChildren(listVector, nodeList.item(i), tagname, isTagNameWildCard);
                }
            } else if (9 == retNode.getNodeType()) {
                traverseChildren(listVector, this.dtm.getNode(this.node), tagname, isTagNameWildCard);
            }
        }
        int size = listVector.size();
        NodeSet nodeSet = new NodeSet(size);
        for (int i2 = 0; i2 < size; i2++) {
            nodeSet.addNode((Node) listVector.elementAt(i2));
        }
        return nodeSet;
    }

    private final void traverseChildren(Vector listVector, Node tempNode, String tagname, boolean isTagNameWildCard) {
        if (tempNode == null) {
            return;
        }
        if (tempNode.getNodeType() == 1 && (isTagNameWildCard || tempNode.getNodeName().equals(tagname))) {
            listVector.add(tempNode);
        }
        if (!tempNode.hasChildNodes()) {
            return;
        }
        NodeList nodeList = tempNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            traverseChildren(listVector, nodeList.item(i), tagname, isTagNameWildCard);
        }
    }

    @Override
    public final Node importNode(Node importedNode, boolean deep) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        Vector listVector = new Vector();
        Node retNode = this.dtm.getNode(this.node);
        if (retNode != null) {
            boolean isNamespaceURIWildCard = "*".equals(namespaceURI);
            boolean isLocalNameWildCard = "*".equals(localName);
            if (1 == retNode.getNodeType()) {
                NodeList nodeList = retNode.getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    traverseChildren(listVector, nodeList.item(i), namespaceURI, localName, isNamespaceURIWildCard, isLocalNameWildCard);
                }
            } else if (9 == retNode.getNodeType()) {
                traverseChildren(listVector, this.dtm.getNode(this.node), namespaceURI, localName, isNamespaceURIWildCard, isLocalNameWildCard);
            }
        }
        int size = listVector.size();
        NodeSet nodeSet = new NodeSet(size);
        for (int i2 = 0; i2 < size; i2++) {
            nodeSet.addNode((Node) listVector.elementAt(i2));
        }
        return nodeSet;
    }

    private final void traverseChildren(Vector listVector, Node tempNode, String namespaceURI, String localname, boolean isNamespaceURIWildCard, boolean isLocalNameWildCard) {
        if (tempNode == null) {
            return;
        }
        if (tempNode.getNodeType() == 1 && (isLocalNameWildCard || tempNode.getLocalName().equals(localname))) {
            String nsURI = tempNode.getNamespaceURI();
            if ((namespaceURI == null && nsURI == null) || isNamespaceURIWildCard || (namespaceURI != null && namespaceURI.equals(nsURI))) {
                listVector.add(tempNode);
            }
        }
        if (!tempNode.hasChildNodes()) {
            return;
        }
        NodeList nl = tempNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            traverseChildren(listVector, nl.item(i), namespaceURI, localname, isNamespaceURIWildCard, isLocalNameWildCard);
        }
    }

    @Override
    public final Element getElementById(String elementId) {
        return (Element) this.dtm.getNode(this.dtm.getElementById(elementId));
    }

    @Override
    public final Text splitText(int offset) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final String getData() throws DOMException {
        return this.dtm.getNodeValue(this.node);
    }

    @Override
    public final void setData(String data) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final int getLength() {
        return this.dtm.getNodeValue(this.node).length();
    }

    @Override
    public final String substringData(int offset, int count) throws DOMException {
        return getData().substring(offset, offset + count);
    }

    @Override
    public final void appendData(String arg) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final void insertData(int offset, String arg) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final void deleteData(int offset, int count) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final void replaceData(int offset, int count, String arg) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final String getTagName() {
        return this.dtm.getNodeName(this.node);
    }

    @Override
    public final String getAttribute(String name) {
        DTMNamedNodeMap map = new DTMNamedNodeMap(this.dtm, this.node);
        Node node = map.getNamedItem(name);
        return node == null ? "" : node.getNodeValue();
    }

    @Override
    public final void setAttribute(String name, String value) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final void removeAttribute(String name) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Attr getAttributeNode(String name) {
        DTMNamedNodeMap map = new DTMNamedNodeMap(this.dtm, this.node);
        return (Attr) map.getNamedItem(name);
    }

    @Override
    public final Attr setAttributeNode(Attr newAttr) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Attr removeAttributeNode(Attr oldAttr) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public boolean hasAttributes() {
        return -1 != this.dtm.getFirstAttribute(this.node);
    }

    @Override
    public final void normalize() {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final String getAttributeNS(String namespaceURI, String localName) {
        Node retNode = null;
        int n = this.dtm.getAttributeNode(this.node, namespaceURI, localName);
        if (n != -1) {
            retNode = this.dtm.getNode(n);
        }
        return retNode == null ? "" : retNode.getNodeValue();
    }

    @Override
    public final void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Attr getAttributeNodeNS(String namespaceURI, String localName) {
        int n = this.dtm.getAttributeNode(this.node, namespaceURI, localName);
        if (n == -1) {
            return null;
        }
        Attr retAttr = (Attr) this.dtm.getNode(n);
        return retAttr;
    }

    @Override
    public final Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final String getName() {
        return this.dtm.getNodeName(this.node);
    }

    @Override
    public final boolean getSpecified() {
        return true;
    }

    @Override
    public final String getValue() {
        return this.dtm.getNodeValue(this.node);
    }

    @Override
    public final void setValue(String value) {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Element getOwnerElement() {
        int newnode;
        if (getNodeType() == 2 && (newnode = this.dtm.getParent(this.node)) != -1) {
            return (Element) this.dtm.getNode(newnode);
        }
        return null;
    }

    @Override
    public Node adoptNode(Node source) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public String getInputEncoding() {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public boolean getStrictErrorChecking() {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public void setStrictErrorChecking(boolean strictErrorChecking) {
        throw new DTMDOMException((short) 9);
    }

    static class DTMNodeProxyImplementation implements DOMImplementation {
        DTMNodeProxyImplementation() {
        }

        @Override
        public DocumentType createDocumentType(String qualifiedName, String publicId, String systemId) {
            throw new DTMDOMException((short) 9);
        }

        @Override
        public Document createDocument(String namespaceURI, String qualfiedName, DocumentType doctype) {
            throw new DTMDOMException((short) 9);
        }

        @Override
        public boolean hasFeature(String feature, String version) {
            if ("CORE".equals(feature.toUpperCase()) || "XML".equals(feature.toUpperCase())) {
                if (SerializerConstants.XMLVERSION10.equals(version) || "2.0".equals(version)) {
                    return true;
                }
                return false;
            }
            return false;
        }

        @Override
        public Object getFeature(String feature, String version) {
            return null;
        }
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
        return this.dtm.getStringValue(this.node).toString();
    }

    @Override
    public short compareDocumentPosition(Node other) throws DOMException {
        return (short) 0;
    }

    @Override
    public String getBaseURI() {
        return null;
    }

    @Override
    public Node renameNode(Node n, String namespaceURI, String name) throws DOMException {
        return n;
    }

    @Override
    public void normalizeDocument() {
    }

    @Override
    public DOMConfiguration getDomConfig() {
        return null;
    }

    @Override
    public void setDocumentURI(String documentURI) {
        this.fDocumentURI = documentURI;
    }

    @Override
    public String getDocumentURI() {
        return this.fDocumentURI;
    }

    public String getActualEncoding() {
        return this.actualEncoding;
    }

    public void setActualEncoding(String value) {
        this.actualEncoding = value;
    }

    @Override
    public Text replaceWholeText(String content) throws DOMException {
        return null;
    }

    @Override
    public String getWholeText() {
        return null;
    }

    @Override
    public boolean isElementContentWhitespace() {
        return false;
    }

    public void setIdAttribute(boolean id) {
    }

    @Override
    public void setIdAttribute(String name, boolean makeId) {
    }

    @Override
    public void setIdAttributeNode(Attr at, boolean makeId) {
    }

    @Override
    public void setIdAttributeNS(String namespaceURI, String localName, boolean makeId) {
    }

    @Override
    public TypeInfo getSchemaTypeInfo() {
        return null;
    }

    @Override
    public boolean isId() {
        return false;
    }

    @Override
    public String getXmlEncoding() {
        return this.xmlEncoding;
    }

    public void setXmlEncoding(String xmlEncoding) {
        this.xmlEncoding = xmlEncoding;
    }

    @Override
    public boolean getXmlStandalone() {
        return this.xmlStandalone;
    }

    @Override
    public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
        this.xmlStandalone = xmlStandalone;
    }

    @Override
    public String getXmlVersion() {
        return this.xmlVersion;
    }

    @Override
    public void setXmlVersion(String xmlVersion) throws DOMException {
        this.xmlVersion = xmlVersion;
    }
}
