package mf.org.apache.xerces.util;

import mf.org.apache.xerces.impl.Constants;
import mf.org.apache.xerces.xni.XMLAttributes;
import org.xml.sax.AttributeList;
import org.xml.sax.ext.Attributes2;

public final class AttributesProxy implements AttributeList, Attributes2 {
    private XMLAttributes fAttributes;

    public AttributesProxy(XMLAttributes attributes) {
        this.fAttributes = attributes;
    }

    public void setAttributes(XMLAttributes attributes) {
        this.fAttributes = attributes;
    }

    public XMLAttributes getAttributes() {
        return this.fAttributes;
    }

    @Override
    public int getLength() {
        return this.fAttributes.getLength();
    }

    @Override
    public String getQName(int index) {
        return this.fAttributes.getQName(index);
    }

    @Override
    public String getURI(int index) {
        String uri = this.fAttributes.getURI(index);
        return uri != null ? uri : XMLSymbols.EMPTY_STRING;
    }

    @Override
    public String getLocalName(int index) {
        return this.fAttributes.getLocalName(index);
    }

    @Override
    public String getType(int i) {
        return this.fAttributes.getType(i);
    }

    @Override
    public String getType(String name) {
        return this.fAttributes.getType(name);
    }

    @Override
    public String getType(String uri, String localName) {
        if (uri.equals(XMLSymbols.EMPTY_STRING)) {
            return this.fAttributes.getType(null, localName);
        }
        return this.fAttributes.getType(uri, localName);
    }

    @Override
    public String getValue(int i) {
        return this.fAttributes.getValue(i);
    }

    @Override
    public String getValue(String name) {
        return this.fAttributes.getValue(name);
    }

    @Override
    public String getValue(String uri, String localName) {
        if (uri.equals(XMLSymbols.EMPTY_STRING)) {
            return this.fAttributes.getValue(null, localName);
        }
        return this.fAttributes.getValue(uri, localName);
    }

    @Override
    public int getIndex(String qName) {
        return this.fAttributes.getIndex(qName);
    }

    @Override
    public int getIndex(String uri, String localPart) {
        if (uri.equals(XMLSymbols.EMPTY_STRING)) {
            return this.fAttributes.getIndex(null, localPart);
        }
        return this.fAttributes.getIndex(uri, localPart);
    }

    @Override
    public boolean isDeclared(int index) {
        if (index < 0 || index >= this.fAttributes.getLength()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return Boolean.TRUE.equals(this.fAttributes.getAugmentations(index).getItem(Constants.ATTRIBUTE_DECLARED));
    }

    @Override
    public boolean isDeclared(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            throw new IllegalArgumentException(qName);
        }
        return Boolean.TRUE.equals(this.fAttributes.getAugmentations(index).getItem(Constants.ATTRIBUTE_DECLARED));
    }

    @Override
    public boolean isDeclared(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index == -1) {
            throw new IllegalArgumentException(localName);
        }
        return Boolean.TRUE.equals(this.fAttributes.getAugmentations(index).getItem(Constants.ATTRIBUTE_DECLARED));
    }

    @Override
    public boolean isSpecified(int index) {
        if (index < 0 || index >= this.fAttributes.getLength()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return this.fAttributes.isSpecified(index);
    }

    @Override
    public boolean isSpecified(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            throw new IllegalArgumentException(qName);
        }
        return this.fAttributes.isSpecified(index);
    }

    @Override
    public boolean isSpecified(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index == -1) {
            throw new IllegalArgumentException(localName);
        }
        return this.fAttributes.isSpecified(index);
    }

    @Override
    public String getName(int i) {
        return this.fAttributes.getQName(i);
    }
}
