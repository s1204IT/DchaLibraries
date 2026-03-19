package org.apache.harmony.xml.dom;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.harmony.xml.dom.NodeImpl;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

public final class DocumentImpl extends InnerNodeImpl implements Document {
    private String documentUri;
    private DOMConfigurationImpl domConfiguration;
    private DOMImplementation domImplementation;
    private String inputEncoding;
    private WeakHashMap<NodeImpl, Map<String, NodeImpl.UserData>> nodeToUserData;
    private boolean strictErrorChecking;
    private boolean xmlStandalone;
    private String xmlVersion;

    public DocumentImpl(DOMImplementationImpl dOMImplementationImpl, String str, String str2, DocumentType documentType, String str3) {
        super(null);
        this.xmlVersion = "1.0";
        this.xmlStandalone = false;
        this.strictErrorChecking = true;
        this.document = this;
        this.domImplementation = dOMImplementationImpl;
        this.inputEncoding = str3;
        if (documentType != null) {
            appendChild(documentType);
        }
        if (str2 != null) {
            appendChild(createElementNS(str, str2));
        }
    }

    private static boolean isXMLIdentifierStart(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
    }

    private static boolean isXMLIdentifierPart(char c) {
        return isXMLIdentifierStart(c) || (c >= '0' && c <= '9') || c == '-' || c == '.';
    }

    static boolean isXMLIdentifier(String str) {
        if (str.length() == 0 || !isXMLIdentifierStart(str.charAt(0))) {
            return false;
        }
        for (int i = 1; i < str.length(); i++) {
            if (!isXMLIdentifierPart(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private NodeImpl shallowCopy(short s, Node node) {
        ElementImpl elementImplCreateElement;
        AttrImpl attrImplCreateAttribute;
        switch (node.getNodeType()) {
            case 1:
                ElementImpl elementImpl = (ElementImpl) node;
                if (elementImpl.namespaceAware) {
                    elementImplCreateElement = createElementNS(elementImpl.getNamespaceURI(), elementImpl.getLocalName());
                    elementImplCreateElement.setPrefix(elementImpl.getPrefix());
                } else {
                    elementImplCreateElement = createElement(elementImpl.getTagName());
                }
                NamedNodeMap attributes = elementImpl.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    AttrImpl attrImpl = (AttrImpl) attributes.item(i);
                    AttrImpl attrImpl2 = (AttrImpl) shallowCopy(s, attrImpl);
                    notifyUserDataHandlers(s, attrImpl, attrImpl2);
                    if (attrImpl.namespaceAware) {
                        elementImplCreateElement.setAttributeNodeNS(attrImpl2);
                    } else {
                        elementImplCreateElement.setAttributeNode(attrImpl2);
                    }
                }
                return elementImplCreateElement;
            case 2:
                AttrImpl attrImpl3 = (AttrImpl) node;
                if (attrImpl3.namespaceAware) {
                    attrImplCreateAttribute = createAttributeNS(attrImpl3.getNamespaceURI(), attrImpl3.getLocalName());
                    attrImplCreateAttribute.setPrefix(attrImpl3.getPrefix());
                } else {
                    attrImplCreateAttribute = createAttribute(attrImpl3.getName());
                }
                attrImplCreateAttribute.setNodeValue(attrImpl3.getValue());
                return attrImplCreateAttribute;
            case 3:
                return createTextNode(((Text) node).getData());
            case 4:
                return createCDATASection(((CharacterData) node).getData());
            case 5:
                return createEntityReference(node.getNodeName());
            case 6:
            case 12:
                throw new UnsupportedOperationException();
            case 7:
                ProcessingInstruction processingInstruction = (ProcessingInstruction) node;
                return createProcessingInstruction(processingInstruction.getTarget(), processingInstruction.getData());
            case 8:
                return createComment(((Comment) node).getData());
            case 9:
            case 10:
                throw new DOMException((short) 9, "Cannot copy node of type " + ((int) node.getNodeType()));
            case 11:
                return createDocumentFragment();
            default:
                throw new DOMException((short) 9, "Unsupported node type " + ((int) node.getNodeType()));
        }
    }

    Node cloneOrImportNode(short s, Node node, boolean z) {
        NodeImpl nodeImplShallowCopy = shallowCopy(s, node);
        if (z) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                nodeImplShallowCopy.appendChild(cloneOrImportNode(s, childNodes.item(i), z));
            }
        }
        notifyUserDataHandlers(s, node, nodeImplShallowCopy);
        return nodeImplShallowCopy;
    }

    @Override
    public Node importNode(Node node, boolean z) {
        return cloneOrImportNode((short) 2, node, z);
    }

    @Override
    public Node adoptNode(Node node) {
        if (!(node instanceof NodeImpl)) {
            return null;
        }
        NodeImpl nodeImpl = (NodeImpl) node;
        switch (nodeImpl.getNodeType()) {
            case 1:
            case 3:
            case 4:
            case 5:
            case 7:
            case 8:
            case 11:
                break;
            case 2:
                AttrImpl attrImpl = (AttrImpl) node;
                if (attrImpl.ownerElement != null) {
                    attrImpl.ownerElement.removeAttributeNode(attrImpl);
                }
                break;
            case 6:
            case 9:
            case 10:
            case 12:
                throw new DOMException((short) 9, "Cannot adopt nodes of type " + ((int) nodeImpl.getNodeType()));
            default:
                throw new DOMException((short) 9, "Unsupported node type " + ((int) node.getNodeType()));
        }
        Node parentNode = nodeImpl.getParentNode();
        if (parentNode != null) {
            parentNode.removeChild(nodeImpl);
        }
        changeDocumentToThis(nodeImpl);
        notifyUserDataHandlers((short) 5, node, null);
        return nodeImpl;
    }

    private void changeDocumentToThis(NodeImpl nodeImpl) {
        Map<String, NodeImpl.UserData> userDataMapForRead = nodeImpl.document.getUserDataMapForRead(nodeImpl);
        if (!userDataMapForRead.isEmpty()) {
            getUserDataMap(nodeImpl).putAll(userDataMapForRead);
        }
        nodeImpl.document = this;
        NodeList childNodes = nodeImpl.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            changeDocumentToThis((NodeImpl) childNodes.item(i));
        }
        if (nodeImpl.getNodeType() == 1) {
            NamedNodeMap attributes = nodeImpl.getAttributes();
            for (int i2 = 0; i2 < attributes.getLength(); i2++) {
                changeDocumentToThis((AttrImpl) attributes.item(i2));
            }
        }
    }

    @Override
    public Node renameNode(Node node, String str, String str2) {
        if (node.getOwnerDocument() != this) {
            throw new DOMException((short) 4, null);
        }
        setNameNS((NodeImpl) node, str, str2);
        notifyUserDataHandlers((short) 4, node, null);
        return node;
    }

    @Override
    public AttrImpl createAttribute(String str) {
        return new AttrImpl(this, str);
    }

    @Override
    public AttrImpl createAttributeNS(String str, String str2) {
        return new AttrImpl(this, str, str2);
    }

    @Override
    public CDATASectionImpl createCDATASection(String str) {
        return new CDATASectionImpl(this, str);
    }

    @Override
    public CommentImpl createComment(String str) {
        return new CommentImpl(this, str);
    }

    @Override
    public DocumentFragmentImpl createDocumentFragment() {
        return new DocumentFragmentImpl(this);
    }

    @Override
    public ElementImpl createElement(String str) {
        return new ElementImpl(this, str);
    }

    @Override
    public ElementImpl createElementNS(String str, String str2) {
        return new ElementImpl(this, str, str2);
    }

    @Override
    public EntityReferenceImpl createEntityReference(String str) {
        return new EntityReferenceImpl(this, str);
    }

    @Override
    public ProcessingInstructionImpl createProcessingInstruction(String str, String str2) {
        return new ProcessingInstructionImpl(this, str, str2);
    }

    @Override
    public TextImpl createTextNode(String str) {
        return new TextImpl(this, str);
    }

    @Override
    public DocumentType getDoctype() {
        for (Node node : this.children) {
            if (node instanceof DocumentType) {
                return (DocumentType) node;
            }
        }
        return null;
    }

    @Override
    public Element getDocumentElement() {
        for (Node node : this.children) {
            if (node instanceof Element) {
                return (Element) node;
            }
        }
        return null;
    }

    @Override
    public Element getElementById(String str) {
        ElementImpl elementImpl = (ElementImpl) getDocumentElement();
        if (elementImpl == null) {
            return null;
        }
        return elementImpl.getElementById(str);
    }

    @Override
    public NodeList getElementsByTagName(String str) {
        NodeListImpl nodeListImpl = new NodeListImpl();
        getElementsByTagName(nodeListImpl, str);
        return nodeListImpl;
    }

    @Override
    public NodeList getElementsByTagNameNS(String str, String str2) {
        NodeListImpl nodeListImpl = new NodeListImpl();
        getElementsByTagNameNS(nodeListImpl, str, str2);
        return nodeListImpl;
    }

    @Override
    public DOMImplementation getImplementation() {
        return this.domImplementation;
    }

    @Override
    public String getNodeName() {
        return "#document";
    }

    @Override
    public short getNodeType() {
        return (short) 9;
    }

    @Override
    public Node insertChildAt(Node node, int i) {
        if ((node instanceof Element) && getDocumentElement() != null) {
            throw new DOMException((short) 3, "Only one root element allowed");
        }
        if ((node instanceof DocumentType) && getDoctype() != null) {
            throw new DOMException((short) 3, "Only one DOCTYPE element allowed");
        }
        return super.insertChildAt(node, i);
    }

    @Override
    public String getTextContent() {
        return null;
    }

    @Override
    public String getInputEncoding() {
        return this.inputEncoding;
    }

    @Override
    public String getXmlEncoding() {
        return null;
    }

    @Override
    public boolean getXmlStandalone() {
        return this.xmlStandalone;
    }

    @Override
    public void setXmlStandalone(boolean z) {
        this.xmlStandalone = z;
    }

    @Override
    public String getXmlVersion() {
        return this.xmlVersion;
    }

    @Override
    public void setXmlVersion(String str) {
        this.xmlVersion = str;
    }

    @Override
    public boolean getStrictErrorChecking() {
        return this.strictErrorChecking;
    }

    @Override
    public void setStrictErrorChecking(boolean z) {
        this.strictErrorChecking = z;
    }

    @Override
    public String getDocumentURI() {
        return this.documentUri;
    }

    @Override
    public void setDocumentURI(String str) {
        this.documentUri = str;
    }

    @Override
    public DOMConfiguration getDomConfig() {
        if (this.domConfiguration == null) {
            this.domConfiguration = new DOMConfigurationImpl();
        }
        return this.domConfiguration;
    }

    @Override
    public void normalizeDocument() {
        Element documentElement = getDocumentElement();
        if (documentElement == null) {
            return;
        }
        ((DOMConfigurationImpl) getDomConfig()).normalize(documentElement);
    }

    Map<String, NodeImpl.UserData> getUserDataMap(NodeImpl nodeImpl) {
        if (this.nodeToUserData == null) {
            this.nodeToUserData = new WeakHashMap<>();
        }
        Map<String, NodeImpl.UserData> map = this.nodeToUserData.get(nodeImpl);
        if (map == null) {
            HashMap map2 = new HashMap();
            this.nodeToUserData.put(nodeImpl, map2);
            return map2;
        }
        return map;
    }

    Map<String, NodeImpl.UserData> getUserDataMapForRead(NodeImpl nodeImpl) {
        if (this.nodeToUserData == null) {
            return Collections.emptyMap();
        }
        Map<String, NodeImpl.UserData> map = this.nodeToUserData.get(nodeImpl);
        if (map != null) {
            return map;
        }
        return Collections.emptyMap();
    }

    private static void notifyUserDataHandlers(short s, Node node, NodeImpl nodeImpl) {
        if (!(node instanceof NodeImpl)) {
            return;
        }
        NodeImpl nodeImpl2 = (NodeImpl) node;
        if (nodeImpl2.document == null) {
            return;
        }
        for (Map.Entry<String, NodeImpl.UserData> entry : nodeImpl2.document.getUserDataMapForRead(nodeImpl2).entrySet()) {
            NodeImpl.UserData value = entry.getValue();
            if (value.handler != null) {
                value.handler.handle(s, entry.getKey(), value.value, node, nodeImpl);
            }
        }
    }
}
