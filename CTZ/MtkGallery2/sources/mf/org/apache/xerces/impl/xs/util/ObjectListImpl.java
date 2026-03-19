package mf.org.apache.xerces.impl.xs.util;

import java.lang.reflect.Array;
import java.util.AbstractList;
import mf.org.apache.xerces.xs.datatypes.ObjectList;

public final class ObjectListImpl extends AbstractList implements ObjectList {
    public static final ObjectListImpl EMPTY_LIST = new ObjectListImpl(new Object[0], 0);
    private final Object[] fArray;
    private final int fLength;

    public ObjectListImpl(Object[] array, int length) {
        this.fArray = array;
        this.fLength = length;
    }

    @Override
    public int getLength() {
        return this.fLength;
    }

    @Override
    public boolean contains(Object item) {
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
    public Object item(int index) {
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        return this.fArray[index];
    }

    @Override
    public Object get(int index) {
        if (index >= 0 && index < this.fLength) {
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
        Object[] a = new Object[this.fLength];
        toArray0(a);
        return a;
    }

    @Override
    public Object[] toArray(Object[] a) {
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
