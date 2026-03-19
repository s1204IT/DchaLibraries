package org.apache.xml.utils;

import org.apache.xml.res.XMLMessages;
import org.apache.xml.serializer.SerializerConstants;
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

public class UnImplNode implements Node, Element, NodeList, Document {
    protected String actualEncoding;
    protected String fDocumentURI;
    private String xmlEncoding;
    private boolean xmlStandalone;
    private String xmlVersion;

    public void error(String str) {
        System.out.println("DOM ERROR! class: " + getClass().getName());
        throw new RuntimeException(XMLMessages.createXMLMessage(str, null));
    }

    public void error(String str, Object[] objArr) {
        System.out.println("DOM ERROR! class: " + getClass().getName());
        throw new RuntimeException(XMLMessages.createXMLMessage(str, objArr));
    }

    @Override
    public Node appendChild(Node node) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public boolean hasChildNodes() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return false;
    }

    @Override
    public short getNodeType() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return (short) 0;
    }

    @Override
    public Node getParentNode() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public NodeList getChildNodes() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Node getFirstChild() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Node getLastChild() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Node getNextSibling() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    public int getLength() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return 0;
    }

    public Node item(int i) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Document getOwnerDocument() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    public String getTagName() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public String getNodeName() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public void normalize() {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    @Override
    public NodeList getElementsByTagName(String str) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Attr removeAttributeNode(Attr attr) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Attr setAttributeNode(Attr attr) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public boolean hasAttribute(String str) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return false;
    }

    @Override
    public boolean hasAttributeNS(String str, String str2) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return false;
    }

    @Override
    public Attr getAttributeNode(String str) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public void removeAttribute(String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    @Override
    public void setAttribute(String str, String str2) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    public String getAttribute(String str) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public boolean hasAttributes() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return false;
    }

    @Override
    public NodeList getElementsByTagNameNS(String str, String str2) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Attr setAttributeNodeNS(Attr attr) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Attr getAttributeNodeNS(String str, String str2) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public void removeAttributeNS(String str, String str2) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    @Override
    public void setAttributeNS(String str, String str2, String str3) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    public String getAttributeNS(String str, String str2) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Node getPreviousSibling() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Node cloneNode(boolean z) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public String getNodeValue() throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public void setNodeValue(String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    public void setValue(String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    public Element getOwnerElement() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    public boolean getSpecified() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return false;
    }

    @Override
    public NamedNodeMap getAttributes() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Node insertBefore(Node node, Node node2) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Node replaceChild(Node node, Node node2) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Node removeChild(Node node) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public boolean isSupported(String str, String str2) {
        return false;
    }

    @Override
    public String getNamespaceURI() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public String getPrefix() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public void setPrefix(String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    @Override
    public String getLocalName() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public DocumentType getDoctype() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public DOMImplementation getImplementation() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Element getDocumentElement() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Element createElement(String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public DocumentFragment createDocumentFragment() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Text createTextNode(String str) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Comment createComment(String str) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public CDATASection createCDATASection(String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public ProcessingInstruction createProcessingInstruction(String str, String str2) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Attr createAttribute(String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public EntityReference createEntityReference(String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Node importNode(Node node, boolean z) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Element createElementNS(String str, String str2) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Attr createAttributeNS(String str, String str2) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Element getElementById(String str) {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    public void setData(String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    public String substringData(int i, int i2) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    public void appendData(String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    public void insertData(int i, String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    public void deleteData(int i, int i2) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    public void replaceData(int i, int i2, String str) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    public Text splitText(int i) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public Node adoptNode(Node node) throws DOMException {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    @Override
    public String getInputEncoding() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return null;
    }

    public void setInputEncoding(String str) {
        error("ER_FUNCTION_NOT_SUPPORTED");
    }

    @Override
    public boolean getStrictErrorChecking() {
        error("ER_FUNCTION_NOT_SUPPORTED");
        return false;
    }

    @Override
    public void setStrictErrorChecking(boolean z) {
        error("ER_FUNCTION_NOT_SUPPORTED");
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

    public Text replaceWholeText(String str) throws DOMException {
        return null;
    }

    public String getWholeText() {
        return null;
    }

    public boolean isWhitespaceInElementContent() {
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
