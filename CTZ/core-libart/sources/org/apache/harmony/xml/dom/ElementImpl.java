package org.apache.harmony.xml.dom;

import android.icu.impl.number.Padder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    ElementImpl(DocumentImpl documentImpl, String str, String str2) {
        super(documentImpl);
        this.attributes = new ArrayList();
        setNameNS(this, str, str2);
    }

    ElementImpl(DocumentImpl documentImpl, String str) {
        super(documentImpl);
        this.attributes = new ArrayList();
        setName(this, str);
    }

    private int indexOfAttribute(String str) {
        for (int i = 0; i < this.attributes.size(); i++) {
            if (Objects.equals(str, this.attributes.get(i).getNodeName())) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfAttributeNS(String str, String str2) {
        for (int i = 0; i < this.attributes.size(); i++) {
            AttrImpl attrImpl = this.attributes.get(i);
            if (Objects.equals(str, attrImpl.getNamespaceURI()) && Objects.equals(str2, attrImpl.getLocalName())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String getAttribute(String str) {
        AttrImpl attributeNode = getAttributeNode(str);
        if (attributeNode == null) {
            return "";
        }
        return attributeNode.getValue();
    }

    @Override
    public String getAttributeNS(String str, String str2) {
        AttrImpl attributeNodeNS = getAttributeNodeNS(str, str2);
        if (attributeNodeNS == null) {
            return "";
        }
        return attributeNodeNS.getValue();
    }

    @Override
    public AttrImpl getAttributeNode(String str) {
        int iIndexOfAttribute = indexOfAttribute(str);
        if (iIndexOfAttribute == -1) {
            return null;
        }
        return this.attributes.get(iIndexOfAttribute);
    }

    @Override
    public AttrImpl getAttributeNodeNS(String str, String str2) {
        int iIndexOfAttributeNS = indexOfAttributeNS(str, str2);
        if (iIndexOfAttributeNS == -1) {
            return null;
        }
        return this.attributes.get(iIndexOfAttributeNS);
    }

    @Override
    public NamedNodeMap getAttributes() {
        return new ElementAttrNamedNodeMapImpl();
    }

    Element getElementById(String str) {
        Element elementById;
        for (AttrImpl attrImpl : this.attributes) {
            if (attrImpl.isId() && str.equals(attrImpl.getValue())) {
                return this;
            }
        }
        if (str.equals(getAttribute("id"))) {
            return this;
        }
        for (LeafNodeImpl leafNodeImpl : this.children) {
            if (leafNodeImpl.getNodeType() == 1 && (elementById = ((ElementImpl) leafNodeImpl).getElementById(str)) != null) {
                return elementById;
            }
        }
        return null;
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
        if (this.prefix != null) {
            return this.prefix + ":" + this.localName;
        }
        return this.localName;
    }

    @Override
    public boolean hasAttribute(String str) {
        return indexOfAttribute(str) != -1;
    }

    @Override
    public boolean hasAttributeNS(String str, String str2) {
        return indexOfAttributeNS(str, str2) != -1;
    }

    @Override
    public boolean hasAttributes() {
        return !this.attributes.isEmpty();
    }

    @Override
    public void removeAttribute(String str) throws DOMException {
        int iIndexOfAttribute = indexOfAttribute(str);
        if (iIndexOfAttribute != -1) {
            this.attributes.remove(iIndexOfAttribute);
        }
    }

    @Override
    public void removeAttributeNS(String str, String str2) throws DOMException {
        int iIndexOfAttributeNS = indexOfAttributeNS(str, str2);
        if (iIndexOfAttributeNS != -1) {
            this.attributes.remove(iIndexOfAttributeNS);
        }
    }

    @Override
    public Attr removeAttributeNode(Attr attr) throws DOMException {
        AttrImpl attrImpl = (AttrImpl) attr;
        if (attrImpl.getOwnerElement() != this) {
            throw new DOMException((short) 8, null);
        }
        this.attributes.remove(attrImpl);
        attrImpl.ownerElement = null;
        return attrImpl;
    }

    @Override
    public void setAttribute(String str, String str2) throws DOMException {
        AttrImpl attributeNode = getAttributeNode(str);
        if (attributeNode == null) {
            attributeNode = this.document.createAttribute(str);
            setAttributeNode(attributeNode);
        }
        attributeNode.setValue(str2);
    }

    @Override
    public void setAttributeNS(String str, String str2, String str3) throws DOMException {
        AttrImpl attributeNodeNS = getAttributeNodeNS(str, str2);
        if (attributeNodeNS == null) {
            attributeNodeNS = this.document.createAttributeNS(str, str2);
            setAttributeNodeNS(attributeNodeNS);
        }
        attributeNodeNS.setValue(str3);
    }

    @Override
    public Attr setAttributeNode(Attr attr) throws DOMException {
        AttrImpl attrImpl = (AttrImpl) attr;
        AttrImpl attrImpl2 = null;
        if (attrImpl.document != this.document) {
            throw new DOMException((short) 4, null);
        }
        if (attrImpl.getOwnerElement() != null) {
            throw new DOMException((short) 10, null);
        }
        int iIndexOfAttribute = indexOfAttribute(attr.getName());
        if (iIndexOfAttribute != -1) {
            attrImpl2 = this.attributes.get(iIndexOfAttribute);
            this.attributes.remove(iIndexOfAttribute);
        }
        this.attributes.add(attrImpl);
        attrImpl.ownerElement = this;
        return attrImpl2;
    }

    @Override
    public Attr setAttributeNodeNS(Attr attr) throws DOMException {
        AttrImpl attrImpl = (AttrImpl) attr;
        AttrImpl attrImpl2 = null;
        if (attrImpl.document != this.document) {
            throw new DOMException((short) 4, null);
        }
        if (attrImpl.getOwnerElement() != null) {
            throw new DOMException((short) 10, null);
        }
        int iIndexOfAttributeNS = indexOfAttributeNS(attr.getNamespaceURI(), attr.getLocalName());
        if (iIndexOfAttributeNS != -1) {
            attrImpl2 = this.attributes.get(iIndexOfAttributeNS);
            this.attributes.remove(iIndexOfAttributeNS);
        }
        this.attributes.add(attrImpl);
        attrImpl.ownerElement = this;
        return attrImpl2;
    }

    @Override
    public void setPrefix(String str) {
        this.prefix = validatePrefix(str, this.namespaceAware, this.namespaceURI);
    }

    public class ElementAttrNamedNodeMapImpl implements NamedNodeMap {
        public ElementAttrNamedNodeMapImpl() {
        }

        @Override
        public int getLength() {
            return ElementImpl.this.attributes.size();
        }

        private int indexOfItem(String str) {
            return ElementImpl.this.indexOfAttribute(str);
        }

        private int indexOfItemNS(String str, String str2) {
            return ElementImpl.this.indexOfAttributeNS(str, str2);
        }

        @Override
        public Node getNamedItem(String str) {
            return ElementImpl.this.getAttributeNode(str);
        }

        @Override
        public Node getNamedItemNS(String str, String str2) {
            return ElementImpl.this.getAttributeNodeNS(str, str2);
        }

        @Override
        public Node item(int i) {
            return (Node) ElementImpl.this.attributes.get(i);
        }

        @Override
        public Node removeNamedItem(String str) throws DOMException {
            int iIndexOfItem = indexOfItem(str);
            if (iIndexOfItem != -1) {
                return (Node) ElementImpl.this.attributes.remove(iIndexOfItem);
            }
            throw new DOMException((short) 8, null);
        }

        @Override
        public Node removeNamedItemNS(String str, String str2) throws DOMException {
            int iIndexOfItemNS = indexOfItemNS(str, str2);
            if (iIndexOfItemNS != -1) {
                return (Node) ElementImpl.this.attributes.remove(iIndexOfItemNS);
            }
            throw new DOMException((short) 8, null);
        }

        @Override
        public Node setNamedItem(Node node) throws DOMException {
            if (!(node instanceof Attr)) {
                throw new DOMException((short) 3, null);
            }
            return ElementImpl.this.setAttributeNode((Attr) node);
        }

        @Override
        public Node setNamedItemNS(Node node) throws DOMException {
            if (!(node instanceof Attr)) {
                throw new DOMException((short) 3, null);
            }
            return ElementImpl.this.setAttributeNodeNS((Attr) node);
        }
    }

    @Override
    public TypeInfo getSchemaTypeInfo() {
        return NULL_TYPE_INFO;
    }

    @Override
    public void setIdAttribute(String str, boolean z) throws DOMException {
        AttrImpl attributeNode = getAttributeNode(str);
        if (attributeNode == null) {
            throw new DOMException((short) 8, "No such attribute: " + str);
        }
        attributeNode.isId = z;
    }

    @Override
    public void setIdAttributeNS(String str, String str2, boolean z) throws DOMException {
        AttrImpl attributeNodeNS = getAttributeNodeNS(str, str2);
        if (attributeNodeNS == null) {
            throw new DOMException((short) 8, "No such attribute: " + str + Padder.FALLBACK_PADDING_STRING + str2);
        }
        attributeNodeNS.isId = z;
    }

    @Override
    public void setIdAttributeNode(Attr attr, boolean z) throws DOMException {
        ((AttrImpl) attr).isId = z;
    }
}
