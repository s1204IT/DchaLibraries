package org.apache.harmony.xml.dom;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;

public final class AttrImpl extends NodeImpl implements Attr {
    boolean isId;
    String localName;
    boolean namespaceAware;
    String namespaceURI;
    ElementImpl ownerElement;
    String prefix;
    private String value;

    AttrImpl(DocumentImpl document, String namespaceURI, String qualifiedName) {
        super(document);
        this.value = "";
        setNameNS(this, namespaceURI, qualifiedName);
    }

    AttrImpl(DocumentImpl document, String name) {
        super(document);
        this.value = "";
        setName(this, name);
    }

    @Override
    public String getLocalName() {
        if (this.namespaceAware) {
            return this.localName;
        }
        return null;
    }

    @Override
    public String getName() {
        if (this.prefix != null) {
            return this.prefix + ":" + this.localName;
        }
        return this.localName;
    }

    @Override
    public String getNamespaceURI() {
        return this.namespaceURI;
    }

    @Override
    public String getNodeName() {
        return getName();
    }

    @Override
    public short getNodeType() {
        return (short) 2;
    }

    @Override
    public String getNodeValue() {
        return getValue();
    }

    @Override
    public Element getOwnerElement() {
        return this.ownerElement;
    }

    @Override
    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public boolean getSpecified() {
        return this.value != null;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public void setPrefix(String prefix) {
        this.prefix = validatePrefix(prefix, this.namespaceAware, this.namespaceURI);
    }

    @Override
    public void setValue(String value) throws DOMException {
        this.value = value;
    }

    @Override
    public TypeInfo getSchemaTypeInfo() {
        return NULL_TYPE_INFO;
    }

    @Override
    public boolean isId() {
        return this.isId;
    }
}
