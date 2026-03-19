package mf.org.apache.xerces.impl.xs.util;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Vector;
import mf.org.apache.xerces.xs.StringList;

public final class StringListImpl extends AbstractList implements StringList {
    public static final StringListImpl EMPTY_LIST = new StringListImpl(new String[0], 0);
    private final String[] fArray;
    private final int fLength;
    private final Vector fVector;

    public StringListImpl(Vector v) {
        this.fVector = v;
        this.fLength = v == null ? 0 : v.size();
        this.fArray = null;
    }

    public StringListImpl(String[] array, int length) {
        this.fArray = array;
        this.fLength = length;
        this.fVector = null;
    }

    @Override
    public int getLength() {
        return this.fLength;
    }

    @Override
    public boolean contains(String item) {
        if (this.fVector != null) {
            return this.fVector.contains(item);
        }
        if (item == null) {
            for (int i = 0; i < this.fLength; i++) {
                if (this.fArray[i] == null) {
                    return true;
                }
            }
            return false;
        }
        for (int i2 = 0; i2 < this.fLength; i2++) {
            if (item.equals(this.fArray[i2])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String item(int index) {
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        if (this.fVector != null) {
            return (String) this.fVector.elementAt(index);
        }
        return this.fArray[index];
    }

    @Override
    public Object get(int index) {
        if (index >= 0 && index < this.fLength) {
            if (this.fVector != null) {
                return this.fVector.elementAt(index);
            }
            return this.fArray[index];
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Override
    public int size() {
        return getLength();
    }

    @Override
    public Object[] toArray() {
        if (this.fVector != null) {
            return this.fVector.toArray();
        }
        Object[] a = new Object[this.fLength];
        toArray0(a);
        return a;
    }

    @Override
    public Object[] toArray(Object[] a) {
        if (this.fVector != null) {
            return this.fVector.toArray(a);
        }
        if (a.length < this.fLength) {
            a = (Object[]) Array.newInstance(a.getClass().getComponentType(), this.fLength);
        }
        toArray0(a);
        if (a.length > this.fLength) {
            a[this.fLength] = null;
        }
        return a;
    }

    private void toArray0(Object[] a) {
        if (this.fLength > 0) {
            System.arraycopy(this.fArray, 0, a, 0, this.fLength);
        }
    }
}
