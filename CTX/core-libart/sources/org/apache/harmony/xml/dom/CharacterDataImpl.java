package org.apache.harmony.xml.dom;

import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;

public abstract class CharacterDataImpl extends LeafNodeImpl implements CharacterData {
    protected StringBuffer buffer;

    CharacterDataImpl(DocumentImpl documentImpl, String str) {
        super(documentImpl);
        setData(str);
    }

    @Override
    public void appendData(String str) throws DOMException {
        this.buffer.append(str);
    }

    @Override
    public void deleteData(int i, int i2) throws DOMException {
        this.buffer.delete(i, i2 + i);
    }

    @Override
    public String getData() throws DOMException {
        return this.buffer.toString();
    }

    public void appendDataTo(StringBuilder sb) {
        sb.append(this.buffer);
    }

    @Override
    public int getLength() {
        return this.buffer.length();
    }

    @Override
    public String getNodeValue() {
        return getData();
    }

    @Override
    public void insertData(int i, String str) throws DOMException {
        try {
            this.buffer.insert(i, str);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new DOMException((short) 1, null);
        }
    }

    @Override
    public void replaceData(int i, int i2, String str) throws DOMException {
        try {
            this.buffer.replace(i, i2 + i, str);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new DOMException((short) 1, null);
        }
    }

    @Override
    public void setData(String str) throws DOMException {
        this.buffer = new StringBuffer(str);
    }

    @Override
    public String substringData(int i, int i2) throws DOMException {
        try {
            return this.buffer.substring(i, i2 + i);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new DOMException((short) 1, null);
        }
    }
}
