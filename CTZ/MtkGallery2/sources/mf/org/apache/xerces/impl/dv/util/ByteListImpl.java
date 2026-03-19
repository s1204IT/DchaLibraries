package mf.org.apache.xerces.impl.dv.util;

import java.util.AbstractList;
import mf.org.apache.xerces.xs.XSException;
import mf.org.apache.xerces.xs.datatypes.ByteList;

public class ByteListImpl extends AbstractList implements ByteList {
    protected String canonical;
    protected final byte[] data;

    public ByteListImpl(byte[] data) {
        this.data = data;
    }

    @Override
    public int getLength() {
        return this.data.length;
    }

    @Override
    public boolean contains(byte item) {
        for (int i = 0; i < this.data.length; i++) {
            if (this.data[i] == item) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte item(int index) throws XSException {
        if (index < 0 || index > this.data.length - 1) {
            throw new XSException((short) 2, null);
        }
        return this.data[index];
    }

    @Override
    public Object get(int index) {
        if (index >= 0 && index < this.data.length) {
            return new Byte(this.data[index]);
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Override
    public int size() {
        return getLength();
    }

    @Override
    public byte[] toByteArray() {
        byte[] ret = new byte[this.data.length];
        System.arraycopy(this.data, 0, ret, 0, this.data.length);
        return ret;
    }
}
