package mf.org.apache.xerces.impl.xs.opti;

import mf.org.w3c.dom.Attr;
import mf.org.w3c.dom.DOMException;
import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.NodeList;
import mf.org.w3c.dom.TypeInfo;

public class DefaultElement extends NodeImpl implements Element {
    public DefaultElement() {
    }

    public DefaultElement(String prefix, String localpart, String rawname, String uri, short nodeType) {
        super(prefix, localpart, rawname, uri, nodeType);
    }

    @Override
    public String getTagName() {
        return null;
    }

    @Override
    public String getAttribute(String name) {
        return null;
    }

    @Override
    public Attr getAttributeNode(String name) {
        return null;
    }

    public NodeList getElementsByTagName(String name) {
        return null;
    }

    @Override
    public String getAttributeNS(String namespaceURI, String localName) {
        return null;
    }

    @Override
    public Attr getAttributeNodeNS(String namespaceURI, String localName) {
        return null;
    }

    public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
        return null;
    }

    public boolean hasAttribute(String name) {
        return false;
    }

    public boolean hasAttributeNS(String namespaceURI, String localName) {
        return false;
    }

    public TypeInfo getSchemaTypeInfo() {
        return null;
    }

    @Override
    public void setAttribute(String name, String value) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public void removeAttribute(String name) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Attr removeAttributeNode(Attr oldAttr) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Attr setAttributeNode(Attr newAttr) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    @Override
    public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public void setIdAttributeNode(Attr at, boolean makeId) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public void setIdAttribute(String name, boolean makeId) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }

    public void setIdAttributeNS(String namespaceURI, String localName, boolean makeId) throws DOMException {
        throw new DOMException((short) 9, "Method not supported");
    }
}
