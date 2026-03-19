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
    private String xmlEncoding;
    private boolean xmlStandalone;
    private String xmlVersion;

    public DocumentImpl(DOMImplementationImpl impl, String namespaceURI, String qualifiedName, DocumentType doctype, String inputEncoding) {
        super(null);
        this.xmlVersion = "1.0";
        this.xmlStandalone = false;
        this.strictErrorChecking = true;
        this.document = this;
        this.domImplementation = impl;
        this.inputEncoding = inputEncoding;
        if (doctype != null) {
            appendChild(doctype);
        }
        if (qualifiedName == null) {
            return;
        }
        appendChild(createElementNS(namespaceURI, qualifiedName));
    }

    private static boolean isXMLIdentifierStart(char c) {
        if (c < 'A' || c > 'Z') {
            return (c >= 'a' && c <= 'z') || c == '_';
        }
        return true;
    }

    private static boolean isXMLIdentifierPart(char c) {
        if (isXMLIdentifierStart(c)) {
            return true;
        }
        return (c >= '0' && c <= '9') || c == '-' || c == '.';
    }

    static boolean isXMLIdentifier(String s) {
        if (s.length() == 0 || !isXMLIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!isXMLIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private NodeImpl shallowCopy(short operation, Node node) {
        ElementImpl elementCopy;
        AttrImpl attrCopy;
        switch (node.getNodeType()) {
            case 1:
                ElementImpl element = (ElementImpl) node;
                if (element.namespaceAware) {
                    elementCopy = createElementNS(element.getNamespaceURI(), element.getLocalName());
                    elementCopy.setPrefix(element.getPrefix());
                } else {
                    elementCopy = createElement(element.getTagName());
                }
                NamedNodeMap attributes = element.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    AttrImpl elementAttr = (AttrImpl) attributes.item(i);
                    AttrImpl elementAttrCopy = (AttrImpl) shallowCopy(operation, elementAttr);
                    notifyUserDataHandlers(operation, elementAttr, elementAttrCopy);
                    if (elementAttr.namespaceAware) {
                        elementCopy.setAttributeNodeNS(elementAttrCopy);
                    } else {
                        elementCopy.setAttributeNode(elementAttrCopy);
                    }
                }
                return elementCopy;
            case 2:
                AttrImpl attr = (AttrImpl) node;
                if (attr.namespaceAware) {
                    attrCopy = createAttributeNS(attr.getNamespaceURI(), attr.getLocalName());
                    attrCopy.setPrefix(attr.getPrefix());
                } else {
                    attrCopy = createAttribute(attr.getName());
                }
                attrCopy.setNodeValue(attr.getValue());
                return attrCopy;
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
                ProcessingInstruction pi = (ProcessingInstruction) node;
                return createProcessingInstruction(pi.getTarget(), pi.getData());
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

    Node cloneOrImportNode(short operation, Node node, boolean deep) {
        NodeImpl copy = shallowCopy(operation, node);
        if (deep) {
            NodeList list = node.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                copy.appendChild(cloneOrImportNode(operation, list.item(i), deep));
            }
        }
        notifyUserDataHandlers(operation, node, copy);
        return copy;
    }

    @Override
    public Node importNode(Node importedNode, boolean deep) {
        return cloneOrImportNode((short) 2, importedNode, deep);
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
                AttrImpl attr = (AttrImpl) node;
                if (attr.ownerElement != null) {
                    attr.ownerElement.removeAttributeNode(attr);
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
        Node parent = nodeImpl.getParentNode();
        if (parent != null) {
            parent.removeChild(nodeImpl);
        }
        changeDocumentToThis(nodeImpl);
        notifyUserDataHandlers((short) 5, node, null);
        return nodeImpl;
    }

    private void changeDocumentToThis(NodeImpl node) {
        Map<String, NodeImpl.UserData> userData = node.document.getUserDataMapForRead(node);
        if (!userData.isEmpty()) {
            getUserDataMap(node).putAll(userData);
        }
        node.document = this;
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            changeDocumentToThis((NodeImpl) list.item(i));
        }
        if (node.getNodeType() != 1) {
            return;
        }
        NamedNodeMap attributes = node.getAttributes();
        for (int i2 = 0; i2 < attributes.getLength(); i2++) {
            changeDocumentToThis((AttrImpl) attributes.item(i2));
        }
    }

    @Override
    public Node renameNode(Node node, String namespaceURI, String qualifiedName) {
        if (node.getOwnerDocument() != this) {
            throw new DOMException((short) 4, null);
        }
        setNameNS((NodeImpl) node, namespaceURI, qualifiedName);
        notifyUserDataHandlers((short) 4, node, null);
        return node;
    }

    @Override
    public AttrImpl createAttribute(String name) {
        return new AttrImpl(this, name);
    }

    @Override
    public AttrImpl createAttributeNS(String namespaceURI, String qualifiedName) {
        return new AttrImpl(this, namespaceURI, qualifiedName);
    }

    @Override
    public CDATASectionImpl createCDATASection(String data) {
        return new CDATASectionImpl(this, data);
    }

    @Override
    public CommentImpl createComment(String data) {
        return new CommentImpl(this, data);
    }

    @Override
    public DocumentFragmentImpl createDocumentFragment() {
        return new DocumentFragmentImpl(this);
    }

    @Override
    public ElementImpl createElement(String tagName) {
        return new ElementImpl(this, tagName);
    }

    @Override
    public ElementImpl createElementNS(String namespaceURI, String qualifiedName) {
        return new ElementImpl(this, namespaceURI, qualifiedName);
    }

    @Override
    public EntityReferenceImpl createEntityReference(String name) {
        return new EntityReferenceImpl(this, name);
    }

    @Override
    public ProcessingInstructionImpl createProcessingInstruction(String target, String data) {
        return new ProcessingInstructionImpl(this, target, data);
    }

    @Override
    public TextImpl createTextNode(String data) {
        return new TextImpl(this, data);
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
    public Element getElementById(String elementId) {
        ElementImpl root = (ElementImpl) getDocumentElement();
        if (root == null) {
            return null;
        }
        return root.getElementById(elementId);
    }

    @Override
    public NodeList getElementsByTagName(String name) {
        NodeListImpl result = new NodeListImpl();
        getElementsByTagName(result, name);
        return result;
    }

    @Override
    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        NodeListImpl result = new NodeListImpl();
        getElementsByTagNameNS(result, namespaceURI, localName);
        return result;
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
    public Node insertChildAt(Node toInsert, int index) {
        if ((toInsert instanceof Element) && getDocumentElement() != null) {
            throw new DOMException((short) 3, "Only one root element allowed");
        }
        if ((toInsert instanceof DocumentType) && getDoctype() != null) {
            throw new DOMException((short) 3, "Only one DOCTYPE element allowed");
        }
        return super.insertChildAt(toInsert, index);
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
        return this.xmlEncoding;
    }

    @Override
    public boolean getXmlStandalone() {
        return this.xmlStandalone;
    }

    @Override
    public void setXmlStandalone(boolean xmlStandalone) {
        this.xmlStandalone = xmlStandalone;
    }

    @Override
    public String getXmlVersion() {
        return this.xmlVersion;
    }

    @Override
    public void setXmlVersion(String xmlVersion) {
        this.xmlVersion = xmlVersion;
    }

    @Override
    public boolean getStrictErrorChecking() {
        return this.strictErrorChecking;
    }

    @Override
    public void setStrictErrorChecking(boolean strictErrorChecking) {
        this.strictErrorChecking = strictErrorChecking;
    }

    @Override
    public String getDocumentURI() {
        return this.documentUri;
    }

    @Override
    public void setDocumentURI(String documentUri) {
        this.documentUri = documentUri;
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
        Element root = getDocumentElement();
        if (root == null) {
            return;
        }
        ((DOMConfigurationImpl) getDomConfig()).normalize(root);
    }

    Map<String, NodeImpl.UserData> getUserDataMap(NodeImpl node) {
        if (this.nodeToUserData == null) {
            this.nodeToUserData = new WeakHashMap<>();
        }
        Map<String, NodeImpl.UserData> userDataMap = this.nodeToUserData.get(node);
        if (userDataMap == null) {
            Map<String, NodeImpl.UserData> userDataMap2 = new HashMap<>();
            this.nodeToUserData.put(node, userDataMap2);
            return userDataMap2;
        }
        return userDataMap;
    }

    Map<String, NodeImpl.UserData> getUserDataMapForRead(NodeImpl node) {
        if (this.nodeToUserData == null) {
            return Collections.emptyMap();
        }
        Map<String, NodeImpl.UserData> userDataMap = this.nodeToUserData.get(node);
        if (userDataMap != null) {
            return userDataMap;
        }
        return Collections.emptyMap();
    }

    private static void notifyUserDataHandlers(short operation, Node source, NodeImpl destination) {
        if (!(source instanceof NodeImpl)) {
            return;
        }
        NodeImpl srcImpl = (NodeImpl) source;
        if (srcImpl.document == null) {
            return;
        }
        for (Map.Entry<String, NodeImpl.UserData> entry : srcImpl.document.getUserDataMapForRead(srcImpl).entrySet()) {
            NodeImpl.UserData userData = entry.getValue();
            if (userData.handler != null) {
                userData.handler.handle(operation, entry.getKey(), userData.value, source, destination);
            }
        }
    }
}
