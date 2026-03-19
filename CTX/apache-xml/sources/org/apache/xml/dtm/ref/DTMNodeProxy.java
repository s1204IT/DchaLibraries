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

    public DTMNodeProxy(DTM dtm, int i) {
        this.dtm = dtm;
        this.node = i;
    }

    public final DTM getDTM() {
        return this.dtm;
    }

    public final int getDTMNodeNumber() {
        return this.node;
    }

    public final boolean equals(Node node) {
        try {
            DTMNodeProxy dTMNodeProxy = (DTMNodeProxy) node;
            if (dTMNodeProxy.node == this.node) {
                return dTMNodeProxy.dtm == this.dtm;
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public final boolean equals(Object obj) {
        try {
            return equals((Node) obj);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public final boolean sameNodeAs(Node node) {
        if (!(node instanceof DTMNodeProxy)) {
            return false;
        }
        DTMNodeProxy dTMNodeProxy = (DTMNodeProxy) node;
        return this.dtm == dTMNodeProxy.dtm && this.node == dTMNodeProxy.node;
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
    public final void setPrefix(String str) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final String getNamespaceURI() {
        return this.dtm.getNamespaceURI(this.node);
    }

    public final boolean supports(String str, String str2) {
        return implementation.hasFeature(str, str2);
    }

    @Override
    public final boolean isSupported(String str, String str2) {
        return implementation.hasFeature(str, str2);
    }

    @Override
    public final String getNodeValue() throws DOMException {
        return this.dtm.getNodeValue(this.node);
    }

    public final String getStringValue() throws DOMException {
        return this.dtm.getStringValue(this.node).toString();
    }

    @Override
    public final void setNodeValue(String str) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final short getNodeType() {
        return this.dtm.getNodeType(this.node);
    }

    @Override
    public final Node getParentNode() {
        int parent;
        if (getNodeType() == 2 || (parent = this.dtm.getParent(this.node)) == -1) {
            return null;
        }
        return this.dtm.getNode(parent);
    }

    public final Node getOwnerNode() {
        int parent = this.dtm.getParent(this.node);
        if (parent == -1) {
            return null;
        }
        return this.dtm.getNode(parent);
    }

    @Override
    public final NodeList getChildNodes() {
        return new DTMChildIterNodeList(this.dtm, this.node);
    }

    @Override
    public final Node getFirstChild() {
        int firstChild = this.dtm.getFirstChild(this.node);
        if (firstChild == -1) {
            return null;
        }
        return this.dtm.getNode(firstChild);
    }

    @Override
    public final Node getLastChild() {
        int lastChild = this.dtm.getLastChild(this.node);
        if (lastChild == -1) {
            return null;
        }
        return this.dtm.getNode(lastChild);
    }

    @Override
    public final Node getPreviousSibling() {
        int previousSibling = this.dtm.getPreviousSibling(this.node);
        if (previousSibling == -1) {
            return null;
        }
        return this.dtm.getNode(previousSibling);
    }

    @Override
    public final Node getNextSibling() {
        int nextSibling;
        if (this.dtm.getNodeType(this.node) == 2 || (nextSibling = this.dtm.getNextSibling(this.node)) == -1) {
            return null;
        }
        return this.dtm.getNode(nextSibling);
    }

    @Override
    public final NamedNodeMap getAttributes() {
        return new DTMNamedNodeMap(this.dtm, this.node);
    }

    @Override
    public boolean hasAttribute(String str) {
        return -1 != this.dtm.getAttributeNode(this.node, null, str);
    }

    @Override
    public boolean hasAttributeNS(String str, String str2) {
        return -1 != this.dtm.getAttributeNode(this.node, str, str2);
    }

    @Override
    public final Document getOwnerDocument() {
        return (Document) this.dtm.getNode(this.dtm.getOwnerDocument(this.node));
    }

    @Override
    public final Node insertBefore(Node node, Node node2) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final Node replaceChild(Node node, Node node2) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final Node removeChild(Node node) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final Node appendChild(Node node) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final boolean hasChildNodes() {
        return -1 != this.dtm.getFirstChild(this.node);
    }

    @Override
    public final Node cloneNode(boolean z) {
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
        int document = this.dtm.getDocument();
        int firstChild = this.dtm.getFirstChild(document);
        int i = -1;
        while (firstChild != -1) {
            short nodeType = this.dtm.getNodeType(firstChild);
            if (nodeType != 1) {
                if (nodeType != 10) {
                    switch (nodeType) {
                        case 7:
                        case 8:
                            break;
                        default:
                            firstChild = this.dtm.getLastChild(document);
                            i = -1;
                            break;
                    }
                }
            } else if (i == -1) {
                i = firstChild;
            } else {
                firstChild = this.dtm.getLastChild(document);
                i = -1;
            }
            firstChild = this.dtm.getNextSibling(firstChild);
        }
        if (i == -1) {
            throw new DTMDOMException((short) 9);
        }
        return (Element) this.dtm.getNode(i);
    }

    @Override
    public final Element createElement(String str) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final DocumentFragment createDocumentFragment() {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Text createTextNode(String str) {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Comment createComment(String str) {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final CDATASection createCDATASection(String str) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final ProcessingInstruction createProcessingInstruction(String str, String str2) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Attr createAttribute(String str) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final EntityReference createEntityReference(String str) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final NodeList getElementsByTagName(String str) {
        Vector vector = new Vector();
        Node node = this.dtm.getNode(this.node);
        if (node != null) {
            boolean zEquals = "*".equals(str);
            if (1 == node.getNodeType()) {
                NodeList childNodes = node.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    traverseChildren(vector, childNodes.item(i), str, zEquals);
                }
            } else if (9 == node.getNodeType()) {
                traverseChildren(vector, this.dtm.getNode(this.node), str, zEquals);
            }
        }
        int size = vector.size();
        NodeSet nodeSet = new NodeSet(size);
        for (int i2 = 0; i2 < size; i2++) {
            nodeSet.addNode((Node) vector.elementAt(i2));
        }
        return nodeSet;
    }

    private final void traverseChildren(Vector vector, Node node, String str, boolean z) {
        if (node == null) {
            return;
        }
        if (node.getNodeType() == 1 && (z || node.getNodeName().equals(str))) {
            vector.add(node);
        }
        if (node.hasChildNodes()) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                traverseChildren(vector, childNodes.item(i), str, z);
            }
        }
    }

    @Override
    public final Node importNode(Node node, boolean z) throws DOMException {
        throw new DTMDOMException((short) 7);
    }

    @Override
    public final Element createElementNS(String str, String str2) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Attr createAttributeNS(String str, String str2) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final NodeList getElementsByTagNameNS(String str, String str2) {
        Vector vector = new Vector();
        Node node = this.dtm.getNode(this.node);
        if (node != null) {
            boolean zEquals = "*".equals(str);
            boolean zEquals2 = "*".equals(str2);
            if (1 == node.getNodeType()) {
                NodeList childNodes = node.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    traverseChildren(vector, childNodes.item(i), str, str2, zEquals, zEquals2);
                }
            } else if (9 == node.getNodeType()) {
                traverseChildren(vector, this.dtm.getNode(this.node), str, str2, zEquals, zEquals2);
            }
        }
        int size = vector.size();
        NodeSet nodeSet = new NodeSet(size);
        for (int i2 = 0; i2 < size; i2++) {
            nodeSet.addNode((Node) vector.elementAt(i2));
        }
        return nodeSet;
    }

    private final void traverseChildren(Vector vector, Node node, String str, String str2, boolean z, boolean z2) {
        if (node == null) {
            return;
        }
        if (node.getNodeType() == 1 && (z2 || node.getLocalName().equals(str2))) {
            String namespaceURI = node.getNamespaceURI();
            if ((str == null && namespaceURI == null) || z || (str != null && str.equals(namespaceURI))) {
                vector.add(node);
            }
        }
        if (node.hasChildNodes()) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                traverseChildren(vector, childNodes.item(i), str, str2, z, z2);
            }
        }
    }

    @Override
    public final Element getElementById(String str) {
        return (Element) this.dtm.getNode(this.dtm.getElementById(str));
    }

    @Override
    public final Text splitText(int i) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final String getData() throws DOMException {
        return this.dtm.getNodeValue(this.node);
    }

    @Override
    public final void setData(String str) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final int getLength() {
        return this.dtm.getNodeValue(this.node).length();
    }

    @Override
    public final String substringData(int i, int i2) throws DOMException {
        return getData().substring(i, i2 + i);
    }

    @Override
    public final void appendData(String str) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final void insertData(int i, String str) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final void deleteData(int i, int i2) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final void replaceData(int i, int i2, String str) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final String getTagName() {
        return this.dtm.getNodeName(this.node);
    }

    @Override
    public final String getAttribute(String str) {
        Node namedItem = new DTMNamedNodeMap(this.dtm, this.node).getNamedItem(str);
        return namedItem == null ? "" : namedItem.getNodeValue();
    }

    @Override
    public final void setAttribute(String str, String str2) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final void removeAttribute(String str) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Attr getAttributeNode(String str) {
        return (Attr) new DTMNamedNodeMap(this.dtm, this.node).getNamedItem(str);
    }

    @Override
    public final Attr setAttributeNode(Attr attr) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Attr removeAttributeNode(Attr attr) throws DOMException {
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
    public final String getAttributeNS(String str, String str2) {
        Node node;
        int attributeNode = this.dtm.getAttributeNode(this.node, str, str2);
        if (attributeNode != -1) {
            node = this.dtm.getNode(attributeNode);
        } else {
            node = null;
        }
        return node == null ? "" : node.getNodeValue();
    }

    @Override
    public final void setAttributeNS(String str, String str2, String str3) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final void removeAttributeNS(String str, String str2) throws DOMException {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Attr getAttributeNodeNS(String str, String str2) {
        int attributeNode = this.dtm.getAttributeNode(this.node, str, str2);
        if (attributeNode != -1) {
            return (Attr) this.dtm.getNode(attributeNode);
        }
        return null;
    }

    @Override
    public final Attr setAttributeNodeNS(Attr attr) throws DOMException {
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
    public final void setValue(String str) {
        throw new DTMDOMException((short) 9);
    }

    @Override
    public final Element getOwnerElement() {
        int parent;
        if (getNodeType() == 2 && (parent = this.dtm.getParent(this.node)) != -1) {
            return (Element) this.dtm.getNode(parent);
        }
        return null;
    }

    @Override
    public Node adoptNode(Node node) throws DOMException {
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
    public void setStrictErrorChecking(boolean z) {
        throw new DTMDOMException((short) 9);
    }

    static class DTMNodeProxyImplementation implements DOMImplementation {
        DTMNodeProxyImplementation() {
        }

        @Override
        public DocumentType createDocumentType(String str, String str2, String str3) {
            throw new DTMDOMException((short) 9);
        }

        @Override
        public Document createDocument(String str, String str2, DocumentType documentType) {
            throw new DTMDOMException((short) 9);
        }

        @Override
        public boolean hasFeature(String str, String str2) {
            if ("CORE".equals(str.toUpperCase()) || "XML".equals(str.toUpperCase())) {
                if (SerializerConstants.XMLVERSION10.equals(str2) || "2.0".equals(str2)) {
                    return true;
                }
                return false;
            }
            return false;
        }

        @Override
        public Object getFeature(String str, String str2) {
            return null;
        }
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
        return this.dtm.getStringValue(this.node).toString();
    }

    @Override
    public short compareDocumentPosition(Node node) throws DOMException {
        return (short) 0;
    }

    @Override
    public String getBaseURI() {
        return null;
    }

    @Override
    public Node renameNode(Node node, String str, String str2) throws DOMException {
        return node;
    }

    @Override
    public void normalizeDocument() {
    }

    @Override
    public DOMConfiguration getDomConfig() {
        return null;
    }

    @Override
    public void setDocumentURI(String str) {
        this.fDocumentURI = str;
    }

    @Override
    public String getDocumentURI() {
        return this.fDocumentURI;
    }

    public String getActualEncoding() {
        return this.actualEncoding;
    }

    public void setActualEncoding(String str) {
        this.actualEncoding = str;
    }

    @Override
    public Text replaceWholeText(String str) throws DOMException {
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

    public void setIdAttribute(boolean z) {
    }

    @Override
    public void setIdAttribute(String str, boolean z) {
    }

    @Override
    public void setIdAttributeNode(Attr attr, boolean z) {
    }

    @Override
    public void setIdAttributeNS(String str, String str2, boolean z) {
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

    public void setXmlEncoding(String str) {
        this.xmlEncoding = str;
    }

    @Override
    public boolean getXmlStandalone() {
        return this.xmlStandalone;
    }

    @Override
    public void setXmlStandalone(boolean z) throws DOMException {
        this.xmlStandalone = z;
    }

    @Override
    public String getXmlVersion() {
        return this.xmlVersion;
    }

    @Override
    public void setXmlVersion(String str) throws DOMException {
        this.xmlVersion = str;
    }
}
