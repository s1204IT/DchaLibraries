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

    AttrImpl(DocumentImpl documentImpl, String str, String str2) {
        super(documentImpl);
        this.value = "";
        setNameNS(this, str, str2);
    }

    AttrImpl(DocumentImpl documentImpl, String str) {
        super(documentImpl);
        this.value = "";
        setName(this, str);
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
    public void setPrefix(String str) {
        this.prefix = validatePrefix(str, this.namespaceAware, this.namespaceURI);
    }

    @Override
    public void setValue(String str) throws DOMException {
        this.value = str;
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
