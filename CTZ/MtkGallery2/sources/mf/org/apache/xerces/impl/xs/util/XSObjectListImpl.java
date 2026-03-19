package mf.org.apache.xerces.impl.xs.util;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import mf.org.apache.xerces.xs.XSObject;
import mf.org.apache.xerces.xs.XSObjectList;

public class XSObjectListImpl extends AbstractList implements XSObjectList {
    private static final int DEFAULT_SIZE = 4;
    private XSObject[] fArray;
    private int fLength;
    public static final XSObjectListImpl EMPTY_LIST = new XSObjectListImpl(new XSObject[0], 0);
    private static final ListIterator EMPTY_ITERATOR = new ListIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public Object previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Object object) {
            throw new UnsupportedOperationException();
        }
    };

    public XSObjectListImpl() {
        this.fArray = null;
        this.fLength = 0;
        this.fArray = new XSObject[4];
        this.fLength = 0;
    }

    public XSObjectListImpl(XSObject[] array, int length) {
        this.fArray = null;
        this.fLength = 0;
        this.fArray = array;
        this.fLength = length;
    }

    @Override
    public int getLength() {
        return this.fLength;
    }

    @Override
    public XSObject item(int index) {
        if (index < 0 || index >= this.fLength) {
            return null;
        }
        return this.fArray[index];
    }

    public void clearXSObjectList() {
        for (int i = 0; i < this.fLength; i++) {
            this.fArray[i] = null;
        }
        this.fArray = null;
        this.fLength = 0;
    }

    public void addXSObject(XSObject object) {
        if (this.fLength == this.fArray.length) {
            XSObject[] temp = new XSObject[this.fLength + 4];
            System.arraycopy(this.fArray, 0, temp, 0, this.fLength);
            this.fArray = temp;
        }
        XSObject[] temp2 = this.fArray;
        int i = this.fLength;
        this.fLength = i + 1;
        temp2[i] = object;
    }

    public void addXSObject(int index, XSObject object) {
        this.fArray[index] = object;
    }

    @Override
    public boolean contains(Object value) {
        return value == null ? containsNull() : containsObject(value);
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
    public Iterator iterator() {
        return listIterator0(0);
    }

    @Override
    public ListIterator listIterator() {
        return listIterator0(0);
    }

    @Override
    public ListIterator listIterator(int index) {
        if (index >= 0 && index < this.fLength) {
            return listIterator0(index);
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    private ListIterator listIterator0(int index) {
        return this.fLength == 0 ? EMPTY_ITERATOR : new XSObjectListIterator(index);
    }

    private boolean containsObject(Object value) {
        for (int i = this.fLength - 1; i >= 0; i--) {
            if (value.equals(this.fArray[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean containsNull() {
        for (int i = this.fLength - 1; i >= 0; i--) {
            if (this.fArray[i] == null) {
                return true;
            }
        }
        return false;
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

    private final class XSObjectListIterator implements ListIterator {
        private int index;

        public XSObjectListIterator(int index) {
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return this.index < XSObjectListImpl.this.fLength;
        }

        @Override
        public Object next() {
            if (this.index < XSObjectListImpl.this.fLength) {
                XSObject[] xSObjectArr = XSObjectListImpl.this.fArray;
                int i = this.index;
                this.index = i + 1;
                return xSObjectArr[i];
            }
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasPrevious() {
            return this.index > 0;
        }

        @Override
        public Object previous() {
            if (this.index > 0) {
                XSObject[] xSObjectArr = XSObjectListImpl.this.fArray;
                int i = this.index - 1;
                this.index = i;
                return xSObjectArr[i];
            }
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return this.index;
        }

        @Override
        public int previousIndex() {
            return this.index - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Object o) {
            throw new UnsupportedOperationException();
        }
    }
}
