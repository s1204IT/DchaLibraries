package org.apache.harmony.xml.dom;

import java.util.ArrayList;
import java.util.List;
import libcore.util.Objects;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

public class ElementImpl extends InnerNodeImpl implements Element {
    private List<AttrImpl> attributes;
    String localName;
    boolean namespaceAware;
    String namespaceURI;
    String prefix;

    ElementImpl(DocumentImpl document, String namespaceURI, String qualifiedName) {
        super(document);
        this.attributes = new ArrayList();
        setNameNS(this, namespaceURI, qualifiedName);
    }

    ElementImpl(DocumentImpl document, String name) {
        super(document);
        this.attributes = new ArrayList();
        setName(this, name);
    }

    private int indexOfAttribute(String name) {
        for (int i = 0; i < this.attributes.size(); i++) {
            AttrImpl attr = this.attributes.get(i);
            if (Objects.equal(name, attr.getNodeName())) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfAttributeNS(String namespaceURI, String localName) {
        for (int i = 0; i < this.attributes.size(); i++) {
            AttrImpl attr = this.attributes.get(i);
            if (Objects.equal(namespaceURI, attr.getNamespaceURI()) && Objects.equal(localName, attr.getLocalName())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getAttribute(String name) {
        Attr attr = getAttributeNode(name);
        return attr == null ? "" : attr.getValue();
    }

    @Override
    public String getAttributeNS(String namespaceURI, String localName) {
        Attr attr = getAttributeNodeNS(namespaceURI, localName);
        return attr == null ? "" : attr.getValue();
    }

    @Override
    public AttrImpl getAttributeNode(String name) {
        int i = indexOfAttribute(name);
        if (i == -1) {
            return null;
        }
        return this.attributes.get(i);
    }

    @Override
    public AttrImpl getAttributeNodeNS(String namespaceURI, String localName) {
        int i = indexOfAttributeNS(namespaceURI, localName);
        if (i == -1) {
            return null;
        }
        return this.attributes.get(i);
    }

    @Override
    public NamedNodeMap getAttributes() {
        return new ElementAttrNamedNodeMapImpl();
    }

    Element getElementById(String name) {
        Element element;
        for (Attr attr : this.attributes) {
            if (attr.isId() && name.equals(attr.getValue())) {
                return this;
            }
        }
        if (!name.equals(getAttribute("id"))) {
            for (NodeImpl node : this.children) {
                if (node.getNodeType() == 1 && (element = ((ElementImpl) node).getElementById(name)) != null) {
                    return element;
                }
            }
            return null;
        }
        return this;
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
    public String getLocalName() {
        if (this.namespaceAware) {
            return this.localName;
        }
        return null;
    }

    @Override
    public String getNamespaceURI() {
        return this.namespaceURI;
    }

    @Override
    public String getNodeName() {
        return getTagName();
    }

    @Override
    public short getNodeType() {
        return (short) 1;
    }

    @Override
    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public String getTagName() {
        return this.prefix != null ? this.prefix + ":" + this.localName : this.localName;
    }

    @Override
    public boolean hasAttribute(String name) {
        return indexOfAttribute(name) != -1;
    }

    @Override
    public boolean hasAttributeNS(String namespaceURI, String localName) {
        return indexOfAttributeNS(namespaceURI, localName) != -1;
    }

    @Override
    public boolean hasAttributes() {
        return !this.attributes.isEmpty();
    }

    @Override
    public void removeAttribute(String name) throws DOMException {
        int i = indexOfAttribute(name);
        if (i != -1) {
            this.attributes.remove(i);
        }
    }

    @Override
    public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
        int i = indexOfAttributeNS(namespaceURI, localName);
        if (i != -1) {
            this.attributes.remove(i);
        }
    }

    @Override
    public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
        AttrImpl oldAttrImpl = (AttrImpl) oldAttr;
        if (oldAttrImpl.getOwnerElement() != this) {
            throw new DOMException((short) 8, null);
        }
        this.attributes.remove(oldAttrImpl);
        oldAttrImpl.ownerElement = null;
        return oldAttrImpl;
    }

    @Override
    public void setAttribute(String name, String value) throws DOMException {
        Attr attr = getAttributeNode(name);
        if (attr == null) {
            attr = this.document.createAttribute(name);
            setAttributeNode(attr);
        }
        attr.setValue(value);
    }

    @Override
    public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
        Attr attr = getAttributeNodeNS(namespaceURI, qualifiedName);
        if (attr == null) {
            attr = this.document.createAttributeNS(namespaceURI, qualifiedName);
            setAttributeNodeNS(attr);
        }
        attr.setValue(value);
    }

    @Override
    public Attr setAttributeNode(Attr newAttr) throws DOMException {
        AttrImpl newAttrImpl = (AttrImpl) newAttr;
        if (newAttrImpl.document != this.document) {
            throw new DOMException((short) 4, null);
        }
        if (newAttrImpl.getOwnerElement() != null) {
            throw new DOMException((short) 10, null);
        }
        AttrImpl oldAttrImpl = null;
        int i = indexOfAttribute(newAttr.getName());
        if (i != -1) {
            AttrImpl oldAttrImpl2 = this.attributes.get(i);
            oldAttrImpl = oldAttrImpl2;
            this.attributes.remove(i);
        }
        this.attributes.add(newAttrImpl);
        newAttrImpl.ownerElement = this;
        return oldAttrImpl;
    }

    @Override
    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
        AttrImpl newAttrImpl = (AttrImpl) newAttr;
        if (newAttrImpl.document != this.document) {
            throw new DOMException((short) 4, null);
        }
        if (newAttrImpl.getOwnerElement() != null) {
            throw new DOMException((short) 10, null);
        }
        AttrImpl oldAttrImpl = null;
        int i = indexOfAttributeNS(newAttr.getNamespaceURI(), newAttr.getLocalName());
        if (i != -1) {
            AttrImpl oldAttrImpl2 = this.attributes.get(i);
            oldAttrImpl = oldAttrImpl2;
            this.attributes.remove(i);
        }
        this.attributes.add(newAttrImpl);
        newAttrImpl.ownerElement = this;
        return oldAttrImpl;
    }

    @Override
    public void setPrefix(String prefix) {
        this.prefix = validatePrefix(prefix, this.namespaceAware, this.namespaceURI);
    }

    public class ElementAttrNamedNodeMapImpl implements NamedNodeMap {
        public ElementAttrNamedNodeMapImpl() {
        }

        @Override
        public int getLength() {
            return ElementImpl.this.attributes.size();
        }

        private int indexOfItem(String name) {
            return ElementImpl.this.indexOfAttribute(name);
        }

        private int indexOfItemNS(String namespaceURI, String localName) {
            return ElementImpl.this.indexOfAttributeNS(namespaceURI, localName);
        }

        @Override
        public Node getNamedItem(String name) {
            return ElementImpl.this.getAttributeNode(name);
        }

        @Override
        public Node getNamedItemNS(String namespaceURI, String localName) {
            return ElementImpl.this.getAttributeNodeNS(namespaceURI, localName);
        }

        @Override
        public Node item(int index) {
            return (Node) ElementImpl.this.attributes.get(index);
        }

        @Override
        public Node removeNamedItem(String name) throws DOMException {
            int i = indexOfItem(name);
            if (i != -1) {
                return (Node) ElementImpl.this.attributes.remove(i);
            }
            throw new DOMException((short) 8, null);
        }

        @Override
        public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
            int i = indexOfItemNS(namespaceURI, localName);
            if (i != -1) {
                return (Node) ElementImpl.this.attributes.remove(i);
            }
            throw new DOMException((short) 8, null);
        }

        @Override
        public Node setNamedItem(Node arg) throws DOMException {
            if (!(arg instanceof Attr)) {
                throw new DOMException((short) 3, null);
            }
            return ElementImpl.this.setAttributeNode((Attr) arg);
        }

        @Override
        public Node setNamedItemNS(Node arg) throws DOMException {
            if (!(arg instanceof Attr)) {
                throw new DOMException((short) 3, null);
            }
            return ElementImpl.this.setAttributeNodeNS((Attr) arg);
        }
    }

    @Override
    public TypeInfo getSchemaTypeInfo() {
        return NULL_TYPE_INFO;
    }

    @Override
    public void setIdAttribute(String name, boolean isId) throws DOMException {
        AttrImpl attr = getAttributeNode(name);
        if (attr == null) {
            throw new DOMException((short) 8, "No such attribute: " + name);
        }
        attr.isId = isId;
    }

    @Override
    public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
        AttrImpl attr = getAttributeNodeNS(namespaceURI, localName);
        if (attr == null) {
            throw new DOMException((short) 8, "No such attribute: " + namespaceURI + " " + localName);
        }
        attr.isId = isId;
    }

    @Override
    public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
        ((AttrImpl) idAttr).isId = isId;
    }
}
