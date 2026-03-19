package mf.org.apache.xerces.dom;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Hashtable;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.Document;
import mf.org.w3c.dom.DocumentType;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.NodeList;
import mf.org.w3c.dom.UserDataHandler;
import mf.org.w3c.dom.events.Event;
import mf.org.w3c.dom.events.EventListener;
import mf.org.w3c.dom.events.EventTarget;

public abstract class NodeImpl implements Serializable, Cloneable, Node, NodeList, EventTarget {
    public static final short DOCUMENT_POSITION_CONTAINS = 8;
    public static final short DOCUMENT_POSITION_DISCONNECTED = 1;
    public static final short DOCUMENT_POSITION_FOLLOWING = 4;
    public static final short DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 32;
    public static final short DOCUMENT_POSITION_IS_CONTAINED = 16;
    public static final short DOCUMENT_POSITION_PRECEDING = 2;
    public static final short ELEMENT_DEFINITION_NODE = 21;
    protected static final short FIRSTCHILD = 16;
    protected static final short HASSTRING = 128;
    protected static final short ID = 512;
    protected static final short IGNORABLEWS = 64;
    protected static final short NORMALIZED = 256;
    protected static final short OWNED = 8;
    protected static final short READONLY = 1;
    protected static final short SPECIFIED = 32;
    protected static final short SYNCCHILDREN = 4;
    protected static final short SYNCDATA = 2;
    public static final short TREE_POSITION_ANCESTOR = 4;
    public static final short TREE_POSITION_DESCENDANT = 8;
    public static final short TREE_POSITION_DISCONNECTED = 0;
    public static final short TREE_POSITION_EQUIVALENT = 16;
    public static final short TREE_POSITION_FOLLOWING = 2;
    public static final short TREE_POSITION_PRECEDING = 1;
    public static final short TREE_POSITION_SAME_NODE = 32;
    static final long serialVersionUID = -6316591992167219696L;
    protected short flags;
    protected NodeImpl ownerNode;

    public abstract String getNodeName();

    public abstract short getNodeType();

    protected NodeImpl(CoreDocumentImpl ownerDocument) {
        this.ownerNode = ownerDocument;
    }

    public NodeImpl() {
    }

    public String getNodeValue() throws DOMException {
        return null;
    }

    public void setNodeValue(String x) throws DOMException {
    }

    @Override
    public Node appendChild(Node newChild) throws DOMException {
        return insertBefore(newChild, null);
    }

    public Node cloneNode(boolean deep) {
        if (needsSyncData()) {
            synchronizeData();
        }
        try {
            NodeImpl newnode = (NodeImpl) clone();
            newnode.ownerNode = ownerDocument();
            newnode.isOwned(false);
            newnode.isReadOnly(false);
            ownerDocument().callUserDataHandlers(this, newnode, (short) 1);
            return newnode;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("**Internal Error**" + e);
        }
    }

    public Document getOwnerDocument() {
        if (isOwned()) {
            return this.ownerNode.ownerDocument();
        }
        return (Document) this.ownerNode;
    }

    CoreDocumentImpl ownerDocument() {
        if (isOwned()) {
            return this.ownerNode.ownerDocument();
        }
        return (CoreDocumentImpl) this.ownerNode;
    }

    protected void setOwnerDocument(CoreDocumentImpl doc) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (!isOwned()) {
            this.ownerNode = doc;
        }
    }

    protected int getNodeNumber() {
        CoreDocumentImpl cd = (CoreDocumentImpl) getOwnerDocument();
        int nodeNumber = cd.getNodeNumber(this);
        return nodeNumber;
    }

    public Node getParentNode() {
        return null;
    }

    NodeImpl parentNode() {
        return null;
    }

    public Node getNextSibling() {
        return null;
    }

    public Node getPreviousSibling() {
        return null;
    }

    ChildNode previousSibling() {
        return null;
    }

    public NamedNodeMap getAttributes() {
        return null;
    }

    public boolean hasAttributes() {
        return false;
    }

    public boolean hasChildNodes() {
        return false;
    }

    public NodeList getChildNodes() {
        return this;
    }

    public Node getFirstChild() {
        return null;
    }

    public Node getLastChild() {
        return null;
    }

    public Node insertBefore(Node newChild, Node refChild) throws DOMException {
        throw new DOMException((short) 3, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null));
    }

    public Node removeChild(Node oldChild) throws DOMException {
        throw new DOMException((short) 8, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null));
    }

    public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
        throw new DOMException((short) 3, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null));
    }

    public int getLength() {
        return 0;
    }

    public Node item(int index) {
        return null;
    }

    public void normalize() {
    }

    public boolean isSupported(String feature, String version) {
        return ownerDocument().getImplementation().hasFeature(feature, version);
    }

    public String getNamespaceURI() {
        return null;
    }

    public String getPrefix() {
        return null;
    }

    public void setPrefix(String prefix) throws DOMException {
        throw new DOMException((short) 14, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null));
    }

    public String getLocalName() {
        return null;
    }

    public void addEventListener(String type, EventListener listener, boolean useCapture) {
        ownerDocument().addEventListener(this, type, listener, useCapture);
    }

    public void removeEventListener(String type, EventListener listener, boolean useCapture) {
        ownerDocument().removeEventListener(this, type, listener, useCapture);
    }

    public boolean dispatchEvent(Event event) {
        return ownerDocument().dispatchEvent(this, event);
    }

    public String getBaseURI() {
        return null;
    }

    public short compareTreePosition(Node other) {
        if (this == other) {
            return (short) 48;
        }
        short thisType = getNodeType();
        short otherType = other.getNodeType();
        short s = 0;
        if (thisType == 6 || thisType == 12 || otherType == 6 || otherType == 12) {
            return (short) 0;
        }
        Node thisAncestor = this;
        Node otherAncestor = other;
        int thisDepth = 0;
        int otherDepth = 0;
        Node node = this;
        while (node != null) {
            thisDepth++;
            if (node == other) {
                return (short) 5;
            }
            thisAncestor = node;
            node = node.getParentNode();
            s = 0;
        }
        Node node2 = other;
        while (node2 != null) {
            otherDepth++;
            if (node2 == this) {
                return (short) 10;
            }
            otherAncestor = node2;
            node2 = node2.getParentNode();
            s = 0;
        }
        Node thisNode = this;
        Node otherNode = other;
        int thisAncestorType = thisAncestor.getNodeType();
        int otherAncestorType = otherAncestor.getNodeType();
        if (thisAncestorType == 2) {
            thisNode = ((AttrImpl) thisAncestor).getOwnerElement();
        }
        if (otherAncestorType == 2) {
            otherNode = ((AttrImpl) otherAncestor).getOwnerElement();
        }
        if (thisAncestorType != 2 || otherAncestorType != 2 || thisNode != otherNode) {
            if (thisAncestorType == 2) {
                thisDepth = 0;
                node2 = thisNode;
                while (node2 != null) {
                    thisDepth++;
                    if (node2 == otherNode) {
                        return (short) 1;
                    }
                    thisAncestor = node2;
                    node2 = node2.getParentNode();
                }
            }
            if (otherAncestorType == 2) {
                otherDepth = 0;
                node2 = otherNode;
                while (node2 != null) {
                    otherDepth++;
                    if (node2 == thisNode) {
                        return (short) 2;
                    }
                    otherAncestor = node2;
                    node2 = node2.getParentNode();
                }
            }
            if (thisAncestor != otherAncestor) {
                return s;
            }
            if (thisDepth > otherDepth) {
                for (int i = 0; i < thisDepth - otherDepth; i++) {
                    thisNode = thisNode.getParentNode();
                }
                if (thisNode == otherNode) {
                    return (short) 1;
                }
            } else {
                for (int i2 = 0; i2 < otherDepth - thisDepth; i2++) {
                    otherNode = otherNode.getParentNode();
                }
                if (otherNode == thisNode) {
                    return (short) 2;
                }
            }
            Node thisNodeP = thisNode.getParentNode();
            for (Node otherNodeP = otherNode.getParentNode(); thisNodeP != otherNodeP; otherNodeP = otherNodeP.getParentNode()) {
                thisNode = thisNodeP;
                otherNode = otherNodeP;
                thisNodeP = thisNodeP.getParentNode();
            }
            Node current = thisNodeP.getFirstChild();
            for (Node current2 = current; current2 != null; current2 = current2.getNextSibling()) {
                if (current2 == otherNode) {
                    return (short) 1;
                }
                if (current2 == thisNode) {
                    return (short) 2;
                }
            }
            return (short) 0;
        }
        return (short) 16;
    }

    @Override
    public short compareDocumentPosition(Node node) throws DOMException {
        Node parentNode;
        ?? parentNode2;
        ?? r6;
        int i;
        Node node2;
        int i2;
        boolean z = false;
        if (this == node) {
            return (short) 0;
        }
        if (node != null && !(node instanceof NodeImpl)) {
            throw new DOMException((short) 9, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null));
        }
        Document ownerDocument = getNodeType() == 9 ? (Document) this : getOwnerDocument();
        Document ownerDocument2 = node.getNodeType() == 9 ? (Document) node : node.getOwnerDocument();
        char c = '#';
        char c2 = '%';
        if (ownerDocument != ownerDocument2 && ownerDocument != null && ownerDocument2 != null) {
            return ((CoreDocumentImpl) ownerDocument2).getNodeNumber() > ((CoreDocumentImpl) ownerDocument).getNodeNumber() ? (short) 37 : (short) 35;
        }
        ?? r7 = this;
        Node node3 = node;
        int i3 = 0;
        int i4 = 0;
        for (?? parentNode3 = this; parentNode3 != 0; parentNode3 = parentNode3.getParentNode()) {
            boolean z2 = z;
            char c3 = c;
            char c4 = c2;
            i3++;
            if (parentNode3 == node) {
                return (short) 10;
            }
            r7 = parentNode3;
            c2 = c4;
            c = c3;
            z = z2;
        }
        Node parentNode4 = node;
        while (parentNode4 != null) {
            boolean z3 = z;
            char c5 = c;
            char c6 = c2;
            i4++;
            if (parentNode4 == this) {
                return (short) 20;
            }
            node3 = parentNode4;
            parentNode4 = parentNode4.getParentNode();
            c2 = c6;
            c = c5;
            z = z3;
        }
        short nodeType = r7.getNodeType();
        short nodeType2 = node3.getNodeType();
        if (nodeType != 2) {
            if (nodeType != 6) {
                if (nodeType == 10) {
                    parentNode = node;
                    if (parentNode == ownerDocument) {
                        return (short) 10;
                    }
                    if (ownerDocument != null && ownerDocument == ownerDocument2) {
                        return (short) 4;
                    }
                } else if (nodeType != 12) {
                    parentNode = node;
                } else {
                    parentNode = node;
                }
                r6 = r7;
                i = i3;
                parentNode2 = this;
            } else {
                parentNode = node;
            }
            DocumentType doctype = ownerDocument.getDoctype();
            if (doctype == node3) {
                return (short) 10;
            }
            if (nodeType2 == 6 || nodeType2 == 12) {
                return nodeType != nodeType2 ? nodeType > nodeType2 ? (short) 2 : (short) 4 : nodeType == 12 ? ((NamedNodeMapImpl) doctype.getNotations()).precedes(node3, r7) ? (short) 34 : (short) 36 : ((NamedNodeMapImpl) doctype.getEntities()).precedes(node3, r7) ? (short) 34 : (short) 36;
            }
            r6 = ownerDocument;
            i = i3;
            parentNode2 = ownerDocument;
        } else {
            parentNode = node;
            Element ownerElement = ((AttrImpl) r7).getOwnerElement();
            if (nodeType2 == 2) {
                Element ownerElement2 = ((AttrImpl) node3).getOwnerElement();
                if (ownerElement2 == ownerElement) {
                    return ((NamedNodeMapImpl) ownerElement.getAttributes()).precedes(node, this) ? (short) 34 : (short) 36;
                }
                parentNode = ownerElement2;
            }
            parentNode4 = ownerElement;
            int i5 = 0;
            ?? r72 = r7;
            while (parentNode4 != null) {
                i5++;
                if (parentNode4 == parentNode) {
                    return (short) 10;
                }
                r72 = parentNode4;
                parentNode4 = parentNode4.getParentNode();
            }
            parentNode2 = ownerElement;
            r6 = r72;
            i = i5;
        }
        if (nodeType2 == 2) {
            Element ownerElement3 = ((AttrImpl) node3).getOwnerElement();
            int i6 = 0;
            for (Node parentNode5 = ownerElement3; parentNode5 != null; parentNode5 = parentNode5.getParentNode()) {
                i6++;
                if (parentNode5 == parentNode2) {
                    return (short) 20;
                }
                node3 = parentNode5;
            }
            node2 = node3;
            i2 = i6;
            parentNode = ownerElement3;
        } else if (nodeType2 != 6) {
            if (nodeType2 == 10) {
                if (parentNode2 == ownerDocument2) {
                    return (short) 20;
                }
                if (ownerDocument2 != null && ownerDocument == ownerDocument2) {
                    return (short) 2;
                }
            } else if (nodeType2 == 12) {
            }
            node2 = node3;
            i2 = i4;
        } else {
            if (ownerDocument.getDoctype() == this) {
                return (short) 20;
            }
            node3 = ownerDocument;
            parentNode = ownerDocument;
            node2 = node3;
            i2 = i4;
        }
        if (r6 != node2) {
            return ((NodeImpl) r6).getNodeNumber() > ((NodeImpl) node2).getNodeNumber() ? (short) 37 : (short) 35;
        }
        if (i > i2) {
            int i7 = 0;
            parentNode2 = parentNode2;
            while (i7 < i - i2) {
                i7++;
                parentNode2 = parentNode2.getParentNode();
            }
            if (parentNode2 == parentNode) {
                return (short) 2;
            }
        } else {
            for (int i8 = 0; i8 < i2 - i; i8++) {
                parentNode = parentNode.getParentNode();
            }
            if (parentNode == parentNode2) {
                return (short) 4;
            }
        }
        Node parentNode6 = parentNode2.getParentNode();
        ?? r5 = parentNode2;
        for (Node parentNode7 = parentNode.getParentNode(); parentNode6 != parentNode7; parentNode7 = parentNode7.getParentNode()) {
            r5 = parentNode6;
            parentNode = parentNode7;
            parentNode6 = parentNode6.getParentNode();
        }
        for (Node firstChild = parentNode6.getFirstChild(); firstChild != null; firstChild = firstChild.getNextSibling()) {
            if (firstChild == parentNode) {
                return (short) 2;
            }
            if (firstChild == r5) {
                return (short) 4;
            }
        }
        return (short) 0;
    }

    public String getTextContent() throws DOMException {
        return getNodeValue();
    }

    void getTextContent(StringBuffer buf) throws DOMException {
        String content = getNodeValue();
        if (content != null) {
            buf.append(content);
        }
    }

    public void setTextContent(String textContent) throws DOMException {
        setNodeValue(textContent);
    }

    @Override
    public boolean isSameNode(Node other) {
        return this == other;
    }

    public boolean isDefaultNamespace(String namespaceURI) {
        short type = getNodeType();
        if (type != 6) {
            switch (type) {
                case 1:
                    String namespace = getNamespaceURI();
                    String prefix = getPrefix();
                    if (prefix == null || prefix.length() == 0) {
                        if (namespaceURI == null) {
                            return namespace == namespaceURI;
                        }
                        return namespaceURI.equals(namespace);
                    }
                    if (hasAttributes()) {
                        ElementImpl elem = (ElementImpl) this;
                        NodeImpl attr = (NodeImpl) elem.getAttributeNodeNS("http://www.w3.org/2000/xmlns/", "xmlns");
                        if (attr != null) {
                            String value = attr.getNodeValue();
                            if (namespaceURI == null) {
                                return namespace == value;
                            }
                            return namespaceURI.equals(value);
                        }
                    }
                    NodeImpl ancestor = (NodeImpl) getElementAncestor(this);
                    if (ancestor != null) {
                        return ancestor.isDefaultNamespace(namespaceURI);
                    }
                    return false;
                case 2:
                    if (this.ownerNode.getNodeType() == 1) {
                        return this.ownerNode.isDefaultNamespace(namespaceURI);
                    }
                    return false;
                default:
                    switch (type) {
                        case 9:
                            return ((NodeImpl) ((Document) this).getDocumentElement()).isDefaultNamespace(namespaceURI);
                        case 10:
                        case 11:
                        case 12:
                            break;
                        default:
                            NodeImpl ancestor2 = (NodeImpl) getElementAncestor(this);
                            if (ancestor2 != null) {
                                return ancestor2.isDefaultNamespace(namespaceURI);
                            }
                            return false;
                    }
                    break;
            }
        }
        return false;
    }

    public String lookupPrefix(String namespaceURI) {
        short type;
        if (namespaceURI != null && (type = getNodeType()) != 6) {
            switch (type) {
                case 1:
                    getNamespaceURI();
                    return lookupNamespacePrefix(namespaceURI, (ElementImpl) this);
                case 2:
                    if (this.ownerNode.getNodeType() != 1) {
                        return null;
                    }
                    return this.ownerNode.lookupPrefix(namespaceURI);
                default:
                    switch (type) {
                        case 9:
                            return ((NodeImpl) ((Document) this).getDocumentElement()).lookupPrefix(namespaceURI);
                        case 10:
                        case 11:
                        case 12:
                            break;
                        default:
                            NodeImpl ancestor = (NodeImpl) getElementAncestor(this);
                            if (ancestor == null) {
                                return null;
                            }
                            return ancestor.lookupPrefix(namespaceURI);
                    }
                    break;
            }
        }
        return null;
    }

    public String lookupNamespaceURI(String specifiedPrefix) {
        short type = getNodeType();
        if (type != 6) {
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
                            if (namespace2 != null && namespace2.equals("http://www.w3.org/2000/xmlns/")) {
                                if (specifiedPrefix == null && attr.getNodeName().equals("xmlns")) {
                                    if (value.length() > 0) {
                                        return value;
                                    }
                                    return null;
                                }
                                if (attrPrefix != null && attrPrefix.equals("xmlns") && attr.getLocalName().equals(specifiedPrefix)) {
                                    if (value.length() > 0) {
                                        return value;
                                    }
                                    return null;
                                }
                            }
                        }
                    }
                    NodeImpl ancestor = (NodeImpl) getElementAncestor(this);
                    if (ancestor != null) {
                        return ancestor.lookupNamespaceURI(specifiedPrefix);
                    }
                    return null;
                case 2:
                    if (this.ownerNode.getNodeType() == 1) {
                        return this.ownerNode.lookupNamespaceURI(specifiedPrefix);
                    }
                    return null;
                default:
                    switch (type) {
                        case 9:
                            return ((NodeImpl) ((Document) this).getDocumentElement()).lookupNamespaceURI(specifiedPrefix);
                        case 10:
                        case 11:
                        case 12:
                            break;
                        default:
                            NodeImpl ancestor2 = (NodeImpl) getElementAncestor(this);
                            if (ancestor2 != null) {
                                return ancestor2.lookupNamespaceURI(specifiedPrefix);
                            }
                            return null;
                    }
                    break;
            }
        }
        return null;
    }

    Node getElementAncestor(Node currentNode) {
        for (Node parent = currentNode.getParentNode(); parent != null; parent = parent.getParentNode()) {
            short type = parent.getNodeType();
            if (type == 1) {
                return parent;
            }
        }
        return null;
    }

    String lookupNamespacePrefix(String namespaceURI, ElementImpl el) {
        String localname;
        String foundNamespace;
        String foundNamespace2;
        String namespace = getNamespaceURI();
        String prefix = getPrefix();
        if (namespace != null && namespace.equals(namespaceURI) && prefix != null && (foundNamespace2 = el.lookupNamespaceURI(prefix)) != null && foundNamespace2.equals(namespaceURI)) {
            return prefix;
        }
        if (hasAttributes()) {
            NamedNodeMap map = getAttributes();
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                String attrPrefix = attr.getPrefix();
                String value = attr.getNodeValue();
                String namespace2 = attr.getNamespaceURI();
                if (namespace2 != null && namespace2.equals("http://www.w3.org/2000/xmlns/") && ((attr.getNodeName().equals("xmlns") || (attrPrefix != null && attrPrefix.equals("xmlns") && value.equals(namespaceURI))) && (foundNamespace = el.lookupNamespaceURI((localname = attr.getLocalName()))) != null && foundNamespace.equals(namespaceURI))) {
                    return localname;
                }
            }
        }
        NodeImpl ancestor = (NodeImpl) getElementAncestor(this);
        if (ancestor != null) {
            return ancestor.lookupNamespacePrefix(namespaceURI, el);
        }
        return null;
    }

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

    public Object getFeature(String feature, String version) {
        if (isSupported(feature, version)) {
            return this;
        }
        return null;
    }

    public Object setUserData(String key, Object data, UserDataHandler handler) {
        return ownerDocument().setUserData(this, key, data, handler);
    }

    public Object getUserData(String key) {
        return ownerDocument().getUserData(this, key);
    }

    protected Hashtable getUserDataRecord() {
        return ownerDocument().getUserDataRecord(this);
    }

    public void setReadOnly(boolean readOnly, boolean deep) {
        if (needsSyncData()) {
            synchronizeData();
        }
        isReadOnly(readOnly);
    }

    public boolean getReadOnly() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return isReadOnly();
    }

    public void setUserData(Object data) {
        ownerDocument().setUserData(this, data);
    }

    public Object getUserData() {
        return ownerDocument().getUserData(this);
    }

    protected void changed() {
        ownerDocument().changed();
    }

    protected int changes() {
        return ownerDocument().changes();
    }

    protected void synchronizeData() {
        needsSyncData(false);
    }

    protected Node getContainer() {
        return null;
    }

    final boolean isReadOnly() {
        return (this.flags & 1) != 0;
    }

    final void isReadOnly(boolean value) {
        this.flags = (short) (value ? this.flags | 1 : this.flags & (-2));
    }

    final boolean needsSyncData() {
        return (this.flags & 2) != 0;
    }

    final void needsSyncData(boolean value) {
        this.flags = (short) (value ? this.flags | 2 : this.flags & (-3));
    }

    final boolean needsSyncChildren() {
        return (this.flags & 4) != 0;
    }

    public final void needsSyncChildren(boolean value) {
        this.flags = (short) (value ? this.flags | 4 : this.flags & (-5));
    }

    final boolean isOwned() {
        return (this.flags & 8) != 0;
    }

    final void isOwned(boolean value) {
        this.flags = (short) (value ? this.flags | 8 : this.flags & (-9));
    }

    final boolean isFirstChild() {
        return (this.flags & 16) != 0;
    }

    final void isFirstChild(boolean value) {
        this.flags = (short) (value ? this.flags | 16 : this.flags & (-17));
    }

    final boolean isSpecified() {
        return (this.flags & 32) != 0;
    }

    final void isSpecified(boolean value) {
        this.flags = (short) (value ? this.flags | 32 : this.flags & (-33));
    }

    final boolean internalIsIgnorableWhitespace() {
        return (this.flags & 64) != 0;
    }

    final void isIgnorableWhitespace(boolean value) {
        this.flags = (short) (value ? this.flags | 64 : this.flags & (-65));
    }

    final boolean hasStringValue() {
        return (this.flags & 128) != 0;
    }

    final void hasStringValue(boolean value) {
        this.flags = (short) (value ? this.flags | 128 : this.flags & (-129));
    }

    final boolean isNormalized() {
        return (this.flags & 256) != 0;
    }

    final void isNormalized(boolean value) {
        if (!value && isNormalized() && this.ownerNode != null) {
            this.ownerNode.isNormalized(false);
        }
        this.flags = (short) (value ? this.flags | 256 : this.flags & (-257));
    }

    final boolean isIdAttribute() {
        return (this.flags & 512) != 0;
    }

    final void isIdAttribute(boolean value) {
        this.flags = (short) (value ? this.flags | 512 : this.flags & (-513));
    }

    public String toString() {
        return "[" + getNodeName() + ": " + getNodeValue() + "]";
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (needsSyncData()) {
            synchronizeData();
        }
        out.defaultWriteObject();
    }
}
