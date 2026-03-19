package mf.org.apache.xerces.dom;

import mf.org.apache.xerces.util.URI;
import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.NamedNodeMap;
import mf.org.w3c.dom.Node;
import mf.org.w3c.dom.NodeList;
import mf.org.w3c.dom.Text;
import mf.org.w3c.dom.TypeInfo;

public class ElementImpl extends ParentNode implements Element, TypeInfo {
    static final long serialVersionUID = 3717253516652722278L;
    protected AttributeMap attributes;
    protected String name;

    public ElementImpl(CoreDocumentImpl ownerDoc, String name) {
        super(ownerDoc);
        this.name = name;
        needsSyncData(true);
    }

    protected ElementImpl() {
    }

    void rename(String name) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.ownerDocument.errorChecking) {
            int colon1 = name.indexOf(58);
            if (colon1 != -1) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NAMESPACE_ERR", null);
                throw new DOMException((short) 14, msg);
            }
            if (!CoreDocumentImpl.isXMLName(name, this.ownerDocument.isXML11Version())) {
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
                throw new DOMException((short) 5, msg2);
            }
        }
        this.name = name;
        reconcileDefaultAttributes();
    }

    @Override
    public short getNodeType() {
        return (short) 1;
    }

    @Override
    public String getNodeName() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.name;
    }

    @Override
    public NamedNodeMap getAttributes() {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.attributes == null) {
            this.attributes = new AttributeMap(this, null);
        }
        return this.attributes;
    }

    @Override
    public Node cloneNode(boolean deep) {
        ElementImpl newnode = (ElementImpl) super.cloneNode(deep);
        if (this.attributes != null) {
            newnode.attributes = (AttributeMap) this.attributes.cloneMap(newnode);
        }
        return newnode;
    }

    @Override
    public String getBaseURI() {
        Attr attrNode;
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.attributes != null && (attrNode = getXMLBaseAttribute()) != null) {
            String uri = attrNode.getNodeValue();
            if (uri.length() != 0) {
                try {
                    URI _uri = new URI(uri, true);
                    if (_uri.isAbsoluteURI()) {
                        return _uri.toString();
                    }
                    String parentBaseURI = this.ownerNode != null ? this.ownerNode.getBaseURI() : null;
                    if (parentBaseURI == null) {
                        return null;
                    }
                    try {
                        URI _parentBaseURI = new URI(parentBaseURI);
                        _uri.absolutize(_parentBaseURI);
                        return _uri.toString();
                    } catch (URI.MalformedURIException e) {
                        return null;
                    }
                } catch (URI.MalformedURIException e2) {
                    return null;
                }
            }
        }
        if (this.ownerNode != null) {
            return this.ownerNode.getBaseURI();
        }
        return null;
    }

    protected Attr getXMLBaseAttribute() {
        return (Attr) this.attributes.getNamedItem("xml:base");
    }

    @Override
    protected void setOwnerDocument(CoreDocumentImpl doc) {
        super.setOwnerDocument(doc);
        if (this.attributes != null) {
            this.attributes.setOwnerDocument(doc);
        }
    }

    @Override
    public String getAttribute(String name) {
        Attr attr;
        if (needsSyncData()) {
            synchronizeData();
        }
        return (this.attributes == null || (attr = (Attr) this.attributes.getNamedItem(name)) == null) ? "" : attr.getValue();
    }

    @Override
    public Attr getAttributeNode(String name) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.attributes == null) {
            return null;
        }
        return (Attr) this.attributes.getNamedItem(name);
    }

    public NodeList getElementsByTagName(String tagname) {
        return new DeepNodeListImpl(this, tagname);
    }

    @Override
    public String getTagName() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.name;
    }

    @Override
    public void normalize() {
        if (isNormalized()) {
            return;
        }
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        ChildNode kid = this.firstChild;
        while (kid != null) {
            ChildNode next = kid.nextSibling;
            if (kid.getNodeType() == 3) {
                if (next != null && next.getNodeType() == 3) {
                    ((Text) kid).appendData(next.getNodeValue());
                    removeChild(next);
                    next = kid;
                } else if (kid.getNodeValue() == null || kid.getNodeValue().length() == 0) {
                    removeChild(kid);
                }
            } else if (kid.getNodeType() == 1) {
                kid.normalize();
            }
            kid = next;
        }
        if (this.attributes != null) {
            for (int i = 0; i < this.attributes.getLength(); i++) {
                Node attr = this.attributes.item(i);
                attr.normalize();
            }
        }
        isNormalized(true);
    }

    public void removeAttribute(String name) {
        if (this.ownerDocument.errorChecking && isReadOnly()) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
            throw new DOMException((short) 7, msg);
        }
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.attributes == null) {
            return;
        }
        this.attributes.safeRemoveNamedItem(name);
    }

    @Override
    public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
        if (this.ownerDocument.errorChecking && isReadOnly()) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
            throw new DOMException((short) 7, msg);
        }
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.attributes == null) {
            String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
            throw new DOMException((short) 8, msg2);
        }
        return (Attr) this.attributes.removeItem(oldAttr, true);
    }

    @Override
    public void setAttribute(String name, String value) {
        if (this.ownerDocument.errorChecking && isReadOnly()) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
            throw new DOMException((short) 7, msg);
        }
        if (needsSyncData()) {
            synchronizeData();
        }
        Attr newAttr = getAttributeNode(name);
        if (newAttr == null) {
            Attr newAttr2 = getOwnerDocument().createAttribute(name);
            if (this.attributes == null) {
                this.attributes = new AttributeMap(this, null);
            }
            newAttr2.setNodeValue(value);
            this.attributes.setNamedItem(newAttr2);
            return;
        }
        newAttr.setNodeValue(value);
    }

    @Override
    public Attr setAttributeNode(Attr newAttr) throws DOMException {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.ownerDocument.errorChecking) {
            if (isReadOnly()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException((short) 7, msg);
            }
            if (newAttr.getOwnerDocument() != this.ownerDocument) {
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
                throw new DOMException((short) 4, msg2);
            }
        }
        if (this.attributes == null) {
            this.attributes = new AttributeMap(this, null);
        }
        return (Attr) this.attributes.setNamedItem(newAttr);
    }

    @Override
    public String getAttributeNS(String namespaceURI, String localName) {
        Attr attr;
        if (needsSyncData()) {
            synchronizeData();
        }
        return (this.attributes == null || (attr = (Attr) this.attributes.getNamedItemNS(namespaceURI, localName)) == null) ? "" : attr.getValue();
    }

    @Override
    public void setAttributeNS(String str, String str2, String str3) {
        String strSubstring;
        String strSubstring2;
        ?? r4;
        String str4;
        if (this.ownerDocument.errorChecking && isReadOnly()) {
            throw new DOMException((short) 7, DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null));
        }
        if (needsSyncData()) {
            synchronizeData();
        }
        int iIndexOf = str2.indexOf(58);
        if (iIndexOf < 0) {
            strSubstring = null;
            strSubstring2 = str2;
        } else {
            strSubstring = str2.substring(0, iIndexOf);
            strSubstring2 = str2.substring(iIndexOf + 1);
        }
        ?? attributeNodeNS = getAttributeNodeNS(str, strSubstring2);
        if (attributeNodeNS == 0) {
            Attr attrCreateAttributeNS = getOwnerDocument().createAttributeNS(str, str2);
            if (this.attributes == null) {
                this.attributes = new AttributeMap(this, null);
            }
            attrCreateAttributeNS.setNodeValue(str3);
            this.attributes.setNamedItemNS(attrCreateAttributeNS);
            return;
        }
        if (attributeNodeNS instanceof AttrNSImpl) {
            if (strSubstring != null) {
                str4 = String.valueOf(strSubstring) + ":" + strSubstring2;
            } else {
                str4 = strSubstring2;
            }
            attributeNodeNS.name = str4;
            r4 = attributeNodeNS;
        } else {
            Attr attrCreateAttributeNS2 = ((CoreDocumentImpl) getOwnerDocument()).createAttributeNS(str, str2, strSubstring2);
            this.attributes.setNamedItemNS(attrCreateAttributeNS2);
            r4 = attrCreateAttributeNS2;
        }
        r4.setNodeValue(str3);
    }

    public void removeAttributeNS(String namespaceURI, String localName) {
        if (this.ownerDocument.errorChecking && isReadOnly()) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
            throw new DOMException((short) 7, msg);
        }
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.attributes == null) {
            return;
        }
        this.attributes.safeRemoveNamedItemNS(namespaceURI, localName);
    }

    @Override
    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.attributes == null) {
            return null;
        }
        return (Attr) this.attributes.getNamedItemNS(namespaceURI, localName);
    }

    @Override
    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.ownerDocument.errorChecking) {
            if (isReadOnly()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException((short) 7, msg);
            }
            if (newAttr.getOwnerDocument() != this.ownerDocument) {
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
                throw new DOMException((short) 4, msg2);
            }
        }
        if (this.attributes == null) {
            this.attributes = new AttributeMap(this, null);
        }
        return (Attr) this.attributes.setNamedItemNS(newAttr);
    }

    protected int setXercesAttributeNode(Attr attr) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.attributes == null) {
            this.attributes = new AttributeMap(this, null);
        }
        return this.attributes.addItem(attr);
    }

    protected int getXercesAttribute(String namespaceURI, String localName) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.attributes == null) {
            return -1;
        }
        return this.attributes.getNamedItemIndex(namespaceURI, localName);
    }

    @Override
    public boolean hasAttributes() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return (this.attributes == null || this.attributes.getLength() == 0) ? false : true;
    }

    public boolean hasAttribute(String name) {
        return getAttributeNode(name) != null;
    }

    public boolean hasAttributeNS(String namespaceURI, String localName) {
        return getAttributeNodeNS(namespaceURI, localName) != null;
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        return new DeepNodeListImpl(this, namespaceURI, localName);
    }

    @Override
    public boolean isEqualNode(Node arg) {
        boolean hasAttrs;
        if (!super.isEqualNode(arg) || (hasAttrs = hasAttributes()) != ((Element) arg).hasAttributes()) {
            return false;
        }
        if (hasAttrs) {
            NamedNodeMap map1 = getAttributes();
            NamedNodeMap map2 = ((Element) arg).getAttributes();
            int len = map1.getLength();
            if (len != map2.getLength()) {
                return false;
            }
            for (int i = 0; i < len; i++) {
                Node n1 = map1.item(i);
                if (n1.getLocalName() == null) {
                    Node n2 = map2.getNamedItem(n1.getNodeName());
                    if (n2 == null || !((NodeImpl) n1).isEqualNode(n2)) {
                        return false;
                    }
                } else {
                    Node n22 = map2.getNamedItemNS(n1.getNamespaceURI(), n1.getLocalName());
                    if (n22 == null || !((NodeImpl) n1).isEqualNode(n22)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return true;
    }

    public void setIdAttributeNode(Attr at, boolean makeId) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (this.ownerDocument.errorChecking) {
            if (isReadOnly()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException((short) 7, msg);
            }
            if (at.getOwnerElement() != this) {
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
                throw new DOMException((short) 8, msg2);
            }
        }
        ((AttrImpl) at).isIdAttribute(makeId);
        if (!makeId) {
            this.ownerDocument.removeIdentifier(at.getValue());
        } else {
            this.ownerDocument.putIdentifier(at.getValue(), this);
        }
    }

    public void setIdAttribute(String name, boolean makeId) {
        if (needsSyncData()) {
            synchronizeData();
        }
        Attr at = getAttributeNode(name);
        if (at == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
            throw new DOMException((short) 8, msg);
        }
        if (this.ownerDocument.errorChecking) {
            if (isReadOnly()) {
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException((short) 7, msg2);
            }
            if (at.getOwnerElement() != this) {
                String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
                throw new DOMException((short) 8, msg3);
            }
        }
        ((AttrImpl) at).isIdAttribute(makeId);
        if (!makeId) {
            this.ownerDocument.removeIdentifier(at.getValue());
        } else {
            this.ownerDocument.putIdentifier(at.getValue(), this);
        }
    }

    public void setIdAttributeNS(String namespaceURI, String localName, boolean makeId) {
        if (needsSyncData()) {
            synchronizeData();
        }
        Attr at = getAttributeNodeNS(namespaceURI, localName);
        if (at == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
            throw new DOMException((short) 8, msg);
        }
        if (this.ownerDocument.errorChecking) {
            if (isReadOnly()) {
                String msg2 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException((short) 7, msg2);
            }
            if (at.getOwnerElement() != this) {
                String msg3 = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
                throw new DOMException((short) 8, msg3);
            }
        }
        ((AttrImpl) at).isIdAttribute(makeId);
        if (!makeId) {
            this.ownerDocument.removeIdentifier(at.getValue());
        } else {
            this.ownerDocument.putIdentifier(at.getValue(), this);
        }
    }

    public String getTypeName() {
        return null;
    }

    public String getTypeNamespace() {
        return null;
    }

    public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod) {
        return false;
    }

    public TypeInfo getSchemaTypeInfo() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this;
    }

    @Override
    public void setReadOnly(boolean readOnly, boolean deep) {
        super.setReadOnly(readOnly, deep);
        if (this.attributes != null) {
            this.attributes.setReadOnly(readOnly, true);
        }
    }

    @Override
    protected void synchronizeData() {
        needsSyncData(false);
        boolean orig = this.ownerDocument.getMutationEvents();
        this.ownerDocument.setMutationEvents(false);
        setupDefaultAttributes();
        this.ownerDocument.setMutationEvents(orig);
    }

    void moveSpecifiedAttributes(ElementImpl el) {
        if (needsSyncData()) {
            synchronizeData();
        }
        if (el.hasAttributes()) {
            if (this.attributes == null) {
                this.attributes = new AttributeMap(this, null);
            }
            this.attributes.moveSpecifiedAttributes(el.attributes);
        }
    }

    protected void setupDefaultAttributes() {
        NamedNodeMapImpl defaults = getDefaultAttributes();
        if (defaults != null) {
            this.attributes = new AttributeMap(this, defaults);
        }
    }

    protected void reconcileDefaultAttributes() {
        if (this.attributes != null) {
            NamedNodeMapImpl defaults = getDefaultAttributes();
            this.attributes.reconcileDefaults(defaults);
        }
    }

    protected NamedNodeMapImpl getDefaultAttributes() {
        ElementDefinitionImpl eldef;
        DocumentTypeImpl doctype = (DocumentTypeImpl) this.ownerDocument.getDoctype();
        if (doctype == null || (eldef = (ElementDefinitionImpl) doctype.getElements().getNamedItem(getNodeName())) == null) {
            return null;
        }
        return (NamedNodeMapImpl) eldef.getAttributes();
    }

    public final int getChildElementCount() {
        int count = 0;
        for (Element child = getFirstElementChild(); child != null; child = ((ElementImpl) child).getNextElementSibling()) {
            count++;
        }
        return count;
    }

    public final Element getFirstElementChild() {
        Element e;
        for (Node n = getFirstChild(); n != null; n = n.getNextSibling()) {
            short nodeType = n.getNodeType();
            if (nodeType == 1) {
                return (Element) n;
            }
            if (nodeType == 5 && (e = getFirstElementChild(n)) != null) {
                return e;
            }
        }
        return null;
    }

    public final Element getLastElementChild() {
        Element e;
        for (Node n = getLastChild(); n != null; n = n.getPreviousSibling()) {
            short nodeType = n.getNodeType();
            if (nodeType == 1) {
                return (Element) n;
            }
            if (nodeType == 5 && (e = getLastElementChild(n)) != null) {
                return e;
            }
        }
        return null;
    }

    public final Element getNextElementSibling() {
        Element e;
        Node n = getNextLogicalSibling(this);
        while (n != null) {
            short nodeType = n.getNodeType();
            if (nodeType == 1) {
                return (Element) n;
            }
            if (nodeType == 5 && (e = getFirstElementChild(n)) != null) {
                return e;
            }
            n = getNextLogicalSibling(n);
        }
        return null;
    }

    public final Element getPreviousElementSibling() {
        Element e;
        Node n = getPreviousLogicalSibling(this);
        while (n != null) {
            short nodeType = n.getNodeType();
            if (nodeType == 1) {
                return (Element) n;
            }
            if (nodeType == 5 && (e = getLastElementChild(n)) != null) {
                return e;
            }
            n = getPreviousLogicalSibling(n);
        }
        return null;
    }

    private Element getFirstElementChild(Node n) {
        while (n != null) {
            if (n.getNodeType() == 1) {
                return (Element) n;
            }
            Node next = n.getFirstChild();
            while (next == null && n != n) {
                next = n.getNextSibling();
                if (next == null && ((n = n.getParentNode()) == null || n == n)) {
                    return null;
                }
            }
            n = next;
        }
        return null;
    }

    private Element getLastElementChild(Node n) {
        while (n != null) {
            if (n.getNodeType() == 1) {
                return (Element) n;
            }
            Node next = n.getLastChild();
            while (next == null && n != n) {
                next = n.getPreviousSibling();
                if (next == null && ((n = n.getParentNode()) == null || n == n)) {
                    return null;
                }
            }
            n = next;
        }
        return null;
    }

    private Node getNextLogicalSibling(Node n) {
        Node next = n.getNextSibling();
        if (next == null) {
            for (Node parent = n.getParentNode(); parent != null && parent.getNodeType() == 5; parent = parent.getParentNode()) {
                next = parent.getNextSibling();
                if (next != null) {
                    break;
                }
            }
        }
        return next;
    }

    private Node getPreviousLogicalSibling(Node n) {
        Node prev = n.getPreviousSibling();
        if (prev == null) {
            for (Node parent = n.getParentNode(); parent != null && parent.getNodeType() == 5; parent = parent.getParentNode()) {
                prev = parent.getPreviousSibling();
                if (prev != null) {
                    break;
                }
            }
        }
        return prev;
    }
}
