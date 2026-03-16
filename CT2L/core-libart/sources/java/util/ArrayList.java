package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import libcore.util.EmptyArray;

public class ArrayList<E> extends AbstractList<E> implements Cloneable, Serializable, RandomAccess {
    private static final int MIN_CAPACITY_INCREMENT = 12;
    private static final long serialVersionUID = 8683452581122892189L;
    transient Object[] array;
    int size;

    public ArrayList(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        this.array = capacity == 0 ? EmptyArray.OBJECT : new Object[capacity];
    }

    public ArrayList() {
        this.array = EmptyArray.OBJECT;
    }

    public ArrayList(Collection<? extends E> collection) {
        if (collection == null) {
            throw new NullPointerException("collection == null");
        }
        Object[] a = collection.toArray();
        if (a.getClass() != Object[].class) {
            Object[] newArray = new Object[a.length];
            System.arraycopy(a, 0, newArray, 0, a.length);
            a = newArray;
        }
        this.array = a;
        this.size = a.length;
    }

    @Override
    public boolean add(E object) {
        Object[] a = this.array;
        int s = this.size;
        if (s == a.length) {
            Object[] newArray = new Object[(s < 6 ? 12 : s >> 1) + s];
            System.arraycopy(a, 0, newArray, 0, s);
            a = newArray;
            this.array = newArray;
        }
        a[s] = object;
        this.size = s + 1;
        this.modCount++;
        return true;
    }

    @Override
    public void add(int index, E object) {
        Object[] a = this.array;
        int s = this.size;
        if (index > s || index < 0) {
            throwIndexOutOfBoundsException(index, s);
        }
        if (s < a.length) {
            System.arraycopy(a, index, a, index + 1, s - index);
        } else {
            Object[] newArray = new Object[newCapacity(s)];
            System.arraycopy(a, 0, newArray, 0, index);
            System.arraycopy(a, index, newArray, index + 1, s - index);
            a = newArray;
            this.array = newArray;
        }
        a[index] = object;
        this.size = s + 1;
        this.modCount++;
    }

    private static int newCapacity(int currentCapacity) {
        int increment = currentCapacity < 6 ? 12 : currentCapacity >> 1;
        return currentCapacity + increment;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        Object[] newPart = collection.toArray();
        int newPartSize = newPart.length;
        if (newPartSize == 0) {
            return false;
        }
        Object[] a = this.array;
        int s = this.size;
        int newSize = s + newPartSize;
        if (newSize > a.length) {
            int newCapacity = newCapacity(newSize - 1);
            Object[] newArray = new Object[newCapacity];
            System.arraycopy(a, 0, newArray, 0, s);
            a = newArray;
            this.array = newArray;
        }
        System.arraycopy(newPart, 0, a, s, newPartSize);
        this.size = newSize;
        this.modCount++;
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        int s = this.size;
        if (index > s || index < 0) {
            throwIndexOutOfBoundsException(index, s);
        }
        Object[] newPart = collection.toArray();
        int newPartSize = newPart.length;
        if (newPartSize == 0) {
            return false;
        }
        Object[] a = this.array;
        int newSize = s + newPartSize;
        if (newSize <= a.length) {
            System.arraycopy(a, index, a, index + newPartSize, s - index);
        } else {
            int newCapacity = newCapacity(newSize - 1);
            Object[] newArray = new Object[newCapacity];
            System.arraycopy(a, 0, newArray, 0, index);
            System.arraycopy(a, index, newArray, index + newPartSize, s - index);
            a = newArray;
            this.array = newArray;
        }
        System.arraycopy(newPart, 0, a, index, newPartSize);
        this.size = newSize;
        this.modCount++;
        return true;
    }

    static IndexOutOfBoundsException throwIndexOutOfBoundsException(int index, int size) {
        throw new IndexOutOfBoundsException("Invalid index " + index + ", size is " + size);
    }

    @Override
    public void clear() {
        if (this.size != 0) {
            Arrays.fill(this.array, 0, this.size, (Object) null);
            this.size = 0;
            this.modCount++;
        }
    }

    public Object clone() {
        try {
            ArrayList<?> result = (ArrayList) super.clone();
            result.array = (Object[]) this.array.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public void ensureCapacity(int minimumCapacity) {
        Object[] a = this.array;
        if (a.length < minimumCapacity) {
            Object[] newArray = new Object[minimumCapacity];
            System.arraycopy(a, 0, newArray, 0, this.size);
            this.array = newArray;
            this.modCount++;
        }
    }

    @Override
    public E get(int i) {
        if (i >= this.size) {
            throwIndexOutOfBoundsException(i, this.size);
        }
        return (E) this.array[i];
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public boolean contains(Object object) {
        Object[] a = this.array;
        int s = this.size;
        if (object != null) {
            for (int i = 0; i < s; i++) {
                if (object.equals(a[i])) {
                    return true;
                }
            }
        } else {
            for (int i2 = 0; i2 < s; i2++) {
                if (a[i2] == null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int indexOf(Object object) {
        Object[] a = this.array;
        int s = this.size;
        if (object != null) {
            for (int i = 0; i < s; i++) {
                if (object.equals(a[i])) {
                    return i;
                }
            }
        } else {
            for (int i2 = 0; i2 < s; i2++) {
                if (a[i2] == null) {
                    return i2;
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object object) {
        Object[] a = this.array;
        if (object != null) {
            for (int i = this.size - 1; i >= 0; i--) {
                if (object.equals(a[i])) {
                    return i;
                }
            }
        } else {
            for (int i2 = this.size - 1; i2 >= 0; i2--) {
                if (a[i2] == null) {
                    return i2;
                }
            }
        }
        return -1;
    }

    @Override
    public E remove(int i) {
        Object[] objArr = this.array;
        int i2 = this.size;
        if (i >= i2) {
            throwIndexOutOfBoundsException(i, i2);
        }
        E e = (E) objArr[i];
        int i3 = i2 - 1;
        System.arraycopy(objArr, i + 1, objArr, i, i3 - i);
        objArr[i3] = null;
        this.size = i3;
        this.modCount++;
        return e;
    }

    @Override
    public boolean remove(Object object) {
        Object[] a = this.array;
        int s = this.size;
        if (object != null) {
            for (int i = 0; i < s; i++) {
                if (object.equals(a[i])) {
                    int s2 = s - 1;
                    System.arraycopy(a, i + 1, a, i, s2 - i);
                    a[s2] = null;
                    this.size = s2;
                    this.modCount++;
                    return true;
                }
            }
        } else {
            for (int i2 = 0; i2 < s; i2++) {
                if (a[i2] == null) {
                    int s3 = s - 1;
                    System.arraycopy(a, i2 + 1, a, i2, s3 - i2);
                    a[s3] = null;
                    this.size = s3;
                    this.modCount++;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        if (fromIndex != toIndex) {
            Object[] a = this.array;
            int s = this.size;
            if (fromIndex >= s) {
                throw new IndexOutOfBoundsException("fromIndex " + fromIndex + " >= size " + this.size);
            }
            if (toIndex > s) {
                throw new IndexOutOfBoundsException("toIndex " + toIndex + " > size " + this.size);
            }
            if (fromIndex > toIndex) {
                throw new IndexOutOfBoundsException("fromIndex " + fromIndex + " > toIndex " + toIndex);
            }
            System.arraycopy(a, toIndex, a, fromIndex, s - toIndex);
            int rangeSize = toIndex - fromIndex;
            Arrays.fill(a, s - rangeSize, s, (Object) null);
            this.size = s - rangeSize;
            this.modCount++;
        }
    }

    @Override
    public E set(int i, E e) {
        Object[] objArr = this.array;
        if (i >= this.size) {
            throwIndexOutOfBoundsException(i, this.size);
        }
        E e2 = (E) objArr[i];
        objArr[i] = e;
        return e2;
    }

    @Override
    public Object[] toArray() {
        int s = this.size;
        Object[] result = new Object[s];
        System.arraycopy(this.array, 0, result, 0, s);
        return result;
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        int i = this.size;
        if (tArr.length < i) {
            tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), i));
        }
        System.arraycopy(this.array, 0, tArr, 0, i);
        if (tArr.length > i) {
            tArr[i] = null;
        }
        return tArr;
    }

    public void trimToSize() {
        int s = this.size;
        if (s != this.array.length) {
            if (s == 0) {
                this.array = EmptyArray.OBJECT;
            } else {
                Object[] newArray = new Object[s];
                System.arraycopy(this.array, 0, newArray, 0, s);
                this.array = newArray;
            }
            this.modCount++;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new ArrayListIterator();
    }

    private class ArrayListIterator implements Iterator<E> {
        private int expectedModCount;
        private int remaining;
        private int removalIndex;

        private ArrayListIterator() {
            this.remaining = ArrayList.this.size;
            this.removalIndex = -1;
            this.expectedModCount = ArrayList.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return this.remaining != 0;
        }

        @Override
        public E next() {
            ArrayList arrayList = ArrayList.this;
            int i = this.remaining;
            if (arrayList.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (i == 0) {
                throw new NoSuchElementException();
            }
            this.remaining = i - 1;
            Object[] objArr = arrayList.array;
            int i2 = arrayList.size - i;
            this.removalIndex = i2;
            return (E) objArr[i2];
        }

        @Override
        public void remove() {
            Object[] a = ArrayList.this.array;
            int removalIdx = this.removalIndex;
            if (ArrayList.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (removalIdx < 0) {
                throw new IllegalStateException();
            }
            System.arraycopy(a, removalIdx + 1, a, removalIdx, this.remaining);
            ArrayList arrayList = ArrayList.this;
            int i = arrayList.size - 1;
            arrayList.size = i;
            a[i] = null;
            this.removalIndex = -1;
            ArrayList arrayList2 = ArrayList.this;
            int i2 = arrayList2.modCount + 1;
            arrayList2.modCount = i2;
            this.expectedModCount = i2;
        }
    }

    @Override
    public int hashCode() {
        Object[] a = this.array;
        int hashCode = 1;
        int s = this.size;
        for (int i = 0; i < s; i++) {
            Object e = a[i];
            hashCode = (hashCode * 31) + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }
        List<?> that = (List) o;
        int s = this.size;
        if (that.size() != s) {
            return false;
        }
        Object[] a = this.array;
        if (that instanceof RandomAccess) {
            for (int i = 0; i < s; i++) {
                Object eThis = a[i];
                Object ethat = that.get(i);
                if (eThis == null) {
                    if (ethat != null) {
                        return false;
                    }
                } else {
                    if (!eThis.equals(ethat)) {
                        return false;
                    }
                }
            }
            return true;
        }
        Iterator<E> it = that.iterator();
        for (int i2 = 0; i2 < s; i2++) {
            Object eThis2 = a[i2];
            Object eThat = it.next();
            if (eThis2 == null) {
                if (eThat != null) {
                    return false;
                }
            } else {
                if (!eThis2.equals(eThat)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(this.array.length);
        for (int i = 0; i < this.size; i++) {
            stream.writeObject(this.array[i]);
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        int cap = stream.readInt();
        if (cap < this.size) {
            throw new InvalidObjectException("Capacity: " + cap + " < size: " + this.size);
        }
        this.array = cap == 0 ? EmptyArray.OBJECT : new Object[cap];
        for (int i = 0; i < this.size; i++) {
            this.array[i] = stream.readObject();
        }
    }
}
