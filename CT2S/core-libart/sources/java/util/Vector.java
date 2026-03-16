package java.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collections;

public class Vector<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
    private static final int DEFAULT_SIZE = 10;
    private static final long serialVersionUID = -2767605614048989439L;
    protected int capacityIncrement;
    protected int elementCount;
    protected Object[] elementData;

    public Vector() {
        this(10, 0);
    }

    public Vector(int capacity) {
        this(capacity, 0);
    }

    public Vector(int capacity, int capacityIncrement) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        this.elementData = newElementArray(capacity);
        this.elementCount = 0;
        this.capacityIncrement = capacityIncrement;
    }

    public Vector(Collection<? extends E> collection) {
        this(collection.size(), 0);
        Iterator<? extends E> it = collection.iterator();
        while (it.hasNext()) {
            Object[] objArr = this.elementData;
            int i = this.elementCount;
            this.elementCount = i + 1;
            objArr[i] = it.next();
        }
    }

    private E[] newElementArray(int i) {
        return (E[]) new Object[i];
    }

    @Override
    public void add(int location, E object) {
        insertElementAt(object, location);
    }

    @Override
    public synchronized boolean add(E object) {
        if (this.elementCount == this.elementData.length) {
            growByOne();
        }
        Object[] objArr = this.elementData;
        int i = this.elementCount;
        this.elementCount = i + 1;
        objArr[i] = object;
        this.modCount++;
        return true;
    }

    @Override
    public synchronized boolean addAll(int location, Collection<? extends E> collection) throws Throwable {
        boolean z;
        if (location >= 0) {
            try {
                if (location <= this.elementCount) {
                    int size = collection.size();
                    if (size == 0) {
                        z = false;
                    } else {
                        int required = size - (this.elementData.length - this.elementCount);
                        if (required > 0) {
                            growBy(required);
                        }
                        int count = this.elementCount - location;
                        if (count > 0) {
                            System.arraycopy(this.elementData, location, this.elementData, location + size, count);
                        }
                        Iterator<? extends E> it = collection.iterator();
                        while (true) {
                            try {
                                int location2 = location;
                                if (!it.hasNext()) {
                                    break;
                                }
                                location = location2 + 1;
                                this.elementData[location2] = it.next();
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        this.elementCount += size;
                        this.modCount++;
                        z = true;
                    }
                    return z;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        throw arrayIndexOutOfBoundsException(location, this.elementCount);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> collection) {
        return addAll(this.elementCount, collection);
    }

    public synchronized void addElement(E object) {
        if (this.elementCount == this.elementData.length) {
            growByOne();
        }
        Object[] objArr = this.elementData;
        int i = this.elementCount;
        this.elementCount = i + 1;
        objArr[i] = object;
        this.modCount++;
    }

    public synchronized int capacity() {
        return this.elementData.length;
    }

    @Override
    public void clear() {
        removeAllElements();
    }

    public synchronized Object clone() {
        Vector<E> vector;
        try {
            vector = (Vector) super.clone();
            vector.elementData = (Object[]) this.elementData.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        return vector;
    }

    @Override
    public boolean contains(Object object) {
        return indexOf(object, 0) != -1;
    }

    @Override
    public synchronized boolean containsAll(Collection<?> collection) {
        return super.containsAll(collection);
    }

    public synchronized void copyInto(Object[] elements) {
        System.arraycopy(this.elementData, 0, elements, 0, this.elementCount);
    }

    public synchronized E elementAt(int i) {
        if (i < this.elementCount) {
        } else {
            throw arrayIndexOutOfBoundsException(i, this.elementCount);
        }
        return (E) this.elementData[i];
    }

    public Enumeration<E> elements() {
        return new Enumeration<E>() {
            int pos = 0;

            @Override
            public boolean hasMoreElements() {
                return this.pos < Vector.this.elementCount;
            }

            @Override
            public E nextElement() {
                synchronized (Vector.this) {
                    if (this.pos < Vector.this.elementCount) {
                        Object[] objArr = Vector.this.elementData;
                        int i = this.pos;
                        this.pos = i + 1;
                        return (E) objArr[i];
                    }
                    throw new NoSuchElementException();
                }
            }
        };
    }

    public synchronized void ensureCapacity(int minimumCapacity) {
        if (this.elementData.length < minimumCapacity) {
            int next = (this.capacityIncrement <= 0 ? this.elementData.length : this.capacityIncrement) + this.elementData.length;
            if (minimumCapacity <= next) {
                minimumCapacity = next;
            }
            grow(minimumCapacity);
        }
    }

    @Override
    public synchronized boolean equals(Object object) {
        boolean z = true;
        synchronized (this) {
            if (this != object) {
                if (object instanceof List) {
                    List<?> list = (List) object;
                    if (list.size() != this.elementCount) {
                        z = false;
                    } else {
                        int index = 0;
                        for (Object e2 : list) {
                            int index2 = index + 1;
                            Object e1 = this.elementData[index];
                            if (e1 == null) {
                                if (e2 != null) {
                                    z = false;
                                    break;
                                }
                                index = index2;
                            } else {
                                if (!e1.equals(e2)) {
                                    z = false;
                                    break;
                                }
                                index = index2;
                            }
                        }
                    }
                } else {
                    z = false;
                }
            }
        }
        return z;
    }

    public synchronized E firstElement() {
        if (this.elementCount > 0) {
        } else {
            throw new NoSuchElementException();
        }
        return (E) this.elementData[0];
    }

    @Override
    public E get(int location) {
        return elementAt(location);
    }

    private void grow(int newCapacity) {
        E[] newData = newElementArray(newCapacity);
        System.arraycopy(this.elementData, 0, newData, 0, this.elementCount);
        this.elementData = newData;
    }

    private void growByOne() {
        int adding;
        if (this.capacityIncrement <= 0) {
            adding = this.elementData.length;
            if (adding == 0) {
                adding = 1;
            }
        } else {
            adding = this.capacityIncrement;
        }
        E[] newData = newElementArray(this.elementData.length + adding);
        System.arraycopy(this.elementData, 0, newData, 0, this.elementCount);
        this.elementData = newData;
    }

    private void growBy(int required) {
        int adding;
        if (this.capacityIncrement <= 0) {
            adding = this.elementData.length;
            if (adding == 0) {
                adding = required;
            }
            while (adding < required) {
                adding += adding;
            }
        } else {
            adding = (required / this.capacityIncrement) * this.capacityIncrement;
            if (adding < required) {
                adding += this.capacityIncrement;
            }
        }
        E[] newData = newElementArray(this.elementData.length + adding);
        System.arraycopy(this.elementData, 0, newData, 0, this.elementCount);
        this.elementData = newData;
    }

    @Override
    public synchronized int hashCode() {
        int result;
        result = 1;
        for (int i = 0; i < this.elementCount; i++) {
            result = (result * 31) + (this.elementData[i] == null ? 0 : this.elementData[i].hashCode());
        }
        return result;
    }

    @Override
    public int indexOf(Object object) {
        return indexOf(object, 0);
    }

    public synchronized int indexOf(Object object, int location) {
        int i;
        if (object != null) {
            for (int i2 = location; i2 < this.elementCount; i2++) {
                if (object.equals(this.elementData[i2])) {
                    i = i2;
                    break;
                }
            }
            i = -1;
        } else {
            for (int i3 = location; i3 < this.elementCount; i3++) {
                if (this.elementData[i3] == null) {
                    i = i3;
                    break;
                }
            }
            i = -1;
        }
        return i;
    }

    public synchronized void insertElementAt(E object, int location) {
        if (location >= 0) {
            if (location <= this.elementCount) {
                if (this.elementCount == this.elementData.length) {
                    growByOne();
                }
                int count = this.elementCount - location;
                if (count > 0) {
                    System.arraycopy(this.elementData, location, this.elementData, location + 1, count);
                }
                this.elementData[location] = object;
                this.elementCount++;
                this.modCount++;
            }
        }
        throw arrayIndexOutOfBoundsException(location, this.elementCount);
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.elementCount == 0;
    }

    public synchronized E lastElement() {
        try {
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException();
        }
        return (E) this.elementData[this.elementCount - 1];
    }

    @Override
    public synchronized int lastIndexOf(Object object) {
        return lastIndexOf(object, this.elementCount - 1);
    }

    public synchronized int lastIndexOf(Object object, int location) {
        int i;
        if (location < this.elementCount) {
            if (object != null) {
                for (int i2 = location; i2 >= 0; i2--) {
                    if (object.equals(this.elementData[i2])) {
                        i = i2;
                        break;
                    }
                }
                i = -1;
            } else {
                for (int i3 = location; i3 >= 0; i3--) {
                    if (this.elementData[i3] == null) {
                        i = i3;
                        break;
                    }
                }
                i = -1;
            }
        } else {
            throw arrayIndexOutOfBoundsException(location, this.elementCount);
        }
        return i;
    }

    @Override
    public synchronized E remove(int i) {
        E e;
        if (i < this.elementCount) {
            e = (E) this.elementData[i];
            this.elementCount--;
            int i2 = this.elementCount - i;
            if (i2 > 0) {
                System.arraycopy(this.elementData, i + 1, this.elementData, i, i2);
            }
            this.elementData[this.elementCount] = null;
            this.modCount++;
        } else {
            throw arrayIndexOutOfBoundsException(i, this.elementCount);
        }
        return e;
    }

    @Override
    public boolean remove(Object object) {
        return removeElement(object);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> collection) {
        return super.removeAll(collection);
    }

    public synchronized void removeAllElements() {
        for (int i = 0; i < this.elementCount; i++) {
            this.elementData[i] = null;
        }
        this.modCount++;
        this.elementCount = 0;
    }

    public synchronized boolean removeElement(Object object) {
        boolean z = false;
        synchronized (this) {
            int index = indexOf(object, 0);
            if (index != -1) {
                removeElementAt(index);
                z = true;
            }
        }
        return z;
    }

    public synchronized void removeElementAt(int location) {
        if (location >= 0) {
            if (location < this.elementCount) {
                this.elementCount--;
                int size = this.elementCount - location;
                if (size > 0) {
                    System.arraycopy(this.elementData, location + 1, this.elementData, location, size);
                }
                this.elementData[this.elementCount] = null;
                this.modCount++;
            }
        }
        throw arrayIndexOutOfBoundsException(location, this.elementCount);
    }

    @Override
    protected void removeRange(int start, int end) {
        if (start >= 0 && start <= end && end <= this.elementCount) {
            if (start != end) {
                if (end != this.elementCount) {
                    System.arraycopy(this.elementData, end, this.elementData, start, this.elementCount - end);
                    int newCount = this.elementCount - (end - start);
                    Arrays.fill(this.elementData, newCount, this.elementCount, (Object) null);
                    this.elementCount = newCount;
                } else {
                    Arrays.fill(this.elementData, start, this.elementCount, (Object) null);
                    this.elementCount = start;
                }
                this.modCount++;
                return;
            }
            return;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public synchronized boolean retainAll(Collection<?> collection) {
        return super.retainAll(collection);
    }

    @Override
    public synchronized E set(int i, E e) {
        E e2;
        if (i < this.elementCount) {
            e2 = (E) this.elementData[i];
            this.elementData[i] = e;
        } else {
            throw arrayIndexOutOfBoundsException(i, this.elementCount);
        }
        return e2;
    }

    public synchronized void setElementAt(E object, int location) {
        if (location < this.elementCount) {
            this.elementData[location] = object;
        } else {
            throw arrayIndexOutOfBoundsException(location, this.elementCount);
        }
    }

    private static ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException(int index, int size) {
        throw new ArrayIndexOutOfBoundsException(size, index);
    }

    public synchronized void setSize(int length) {
        if (length != this.elementCount) {
            ensureCapacity(length);
            if (this.elementCount > length) {
                Arrays.fill(this.elementData, length, this.elementCount, (Object) null);
            }
            this.elementCount = length;
            this.modCount++;
        }
    }

    @Override
    public synchronized int size() {
        return this.elementCount;
    }

    @Override
    public synchronized List<E> subList(int start, int end) {
        return new Collections.SynchronizedRandomAccessList(super.subList(start, end), this);
    }

    @Override
    public synchronized Object[] toArray() {
        Object[] result;
        result = new Object[this.elementCount];
        System.arraycopy(this.elementData, 0, result, 0, this.elementCount);
        return result;
    }

    @Override
    public synchronized <T> T[] toArray(T[] tArr) {
        if (this.elementCount > tArr.length) {
            tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), this.elementCount));
        }
        System.arraycopy(this.elementData, 0, tArr, 0, this.elementCount);
        if (this.elementCount < tArr.length) {
            tArr[this.elementCount] = null;
        }
        return tArr;
    }

    @Override
    public synchronized String toString() {
        String string;
        if (this.elementCount == 0) {
            string = "[]";
        } else {
            int length = this.elementCount - 1;
            StringBuilder buffer = new StringBuilder(this.elementCount * 16);
            buffer.append('[');
            for (int i = 0; i < length; i++) {
                if (this.elementData[i] == this) {
                    buffer.append("(this Collection)");
                } else {
                    buffer.append(this.elementData[i]);
                }
                buffer.append(", ");
            }
            if (this.elementData[length] == this) {
                buffer.append("(this Collection)");
            } else {
                buffer.append(this.elementData[length]);
            }
            buffer.append(']');
            string = buffer.toString();
        }
        return string;
    }

    public synchronized void trimToSize() {
        if (this.elementData.length != this.elementCount) {
            grow(this.elementCount);
        }
    }

    private synchronized void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }
}
