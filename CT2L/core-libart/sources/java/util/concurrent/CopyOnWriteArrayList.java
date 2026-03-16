package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import libcore.util.EmptyArray;
import libcore.util.Objects;

public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
    private static final long serialVersionUID = 8673264195747942595L;
    private volatile transient Object[] elements;

    public CopyOnWriteArrayList() {
        this.elements = EmptyArray.OBJECT;
    }

    public CopyOnWriteArrayList(Collection<? extends E> collection) {
        this(collection.toArray());
    }

    public CopyOnWriteArrayList(E[] array) {
        this.elements = Arrays.copyOf(array, array.length, Object[].class);
    }

    public Object clone() {
        try {
            CopyOnWriteArrayList result = (CopyOnWriteArrayList) super.clone();
            result.elements = (Object[]) result.elements.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public int size() {
        return this.elements.length;
    }

    @Override
    public E get(int i) {
        return (E) this.elements[i];
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        Object[] snapshot = this.elements;
        return containsAll(collection, snapshot, 0, snapshot.length);
    }

    static boolean containsAll(Collection<?> collection, Object[] snapshot, int from, int to) {
        for (Object o : collection) {
            if (indexOf(o, snapshot, from, to) == -1) {
                return false;
            }
        }
        return true;
    }

    public int indexOf(E object, int from) {
        Object[] snapshot = this.elements;
        return indexOf(object, snapshot, from, snapshot.length);
    }

    @Override
    public int indexOf(Object object) {
        Object[] snapshot = this.elements;
        return indexOf(object, snapshot, 0, snapshot.length);
    }

    public int lastIndexOf(E object, int to) {
        Object[] snapshot = this.elements;
        return lastIndexOf(object, snapshot, 0, to);
    }

    @Override
    public int lastIndexOf(Object object) {
        Object[] snapshot = this.elements;
        return lastIndexOf(object, snapshot, 0, snapshot.length);
    }

    @Override
    public boolean isEmpty() {
        return this.elements.length == 0;
    }

    @Override
    public Iterator<E> iterator() {
        Object[] snapshot = this.elements;
        return new CowIterator(snapshot, 0, snapshot.length);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        Object[] snapshot = this.elements;
        if (index < 0 || index > snapshot.length) {
            throw new IndexOutOfBoundsException("index=" + index + ", length=" + snapshot.length);
        }
        CowIterator<E> result = new CowIterator<>(snapshot, 0, snapshot.length);
        ((CowIterator) result).index = index;
        return result;
    }

    @Override
    public ListIterator<E> listIterator() {
        Object[] snapshot = this.elements;
        return new CowIterator(snapshot, 0, snapshot.length);
    }

    @Override
    public List<E> subList(int from, int to) {
        Object[] snapshot = this.elements;
        if (from < 0 || from > to || to > snapshot.length) {
            throw new IndexOutOfBoundsException("from=" + from + ", to=" + to + ", list size=" + snapshot.length);
        }
        return new CowSubList(snapshot, from, to);
    }

    @Override
    public Object[] toArray() {
        return (Object[]) this.elements.clone();
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        Object[] objArr = this.elements;
        if (objArr.length > tArr.length) {
            return (T[]) Arrays.copyOf(objArr, objArr.length, tArr.getClass());
        }
        System.arraycopy(objArr, 0, tArr, 0, objArr.length);
        if (objArr.length < tArr.length) {
            tArr[objArr.length] = null;
        }
        return tArr;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CopyOnWriteArrayList) {
            return this == other || Arrays.equals(this.elements, ((CopyOnWriteArrayList) other).elements);
        }
        if (!(other instanceof List)) {
            return false;
        }
        Object[] snapshot = this.elements;
        Iterator<E> it = ((List) other).iterator();
        for (Object o : snapshot) {
            if (!it.hasNext() || !Objects.equal(o, it.next())) {
                return false;
            }
        }
        return it.hasNext() ? false : true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.elements);
    }

    public String toString() {
        return Arrays.toString(this.elements);
    }

    @Override
    public synchronized boolean add(E e) {
        Object[] newElements = new Object[this.elements.length + 1];
        System.arraycopy(this.elements, 0, newElements, 0, this.elements.length);
        newElements[this.elements.length] = e;
        this.elements = newElements;
        return true;
    }

    @Override
    public synchronized void add(int index, E e) {
        Object[] newElements = new Object[this.elements.length + 1];
        System.arraycopy(this.elements, 0, newElements, 0, index);
        newElements[index] = e;
        System.arraycopy(this.elements, index, newElements, index + 1, this.elements.length - index);
        this.elements = newElements;
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> collection) {
        return addAll(this.elements.length, collection);
    }

    @Override
    public synchronized boolean addAll(int index, Collection<? extends E> collection) {
        boolean z;
        synchronized (this) {
            Object[] toAdd = collection.toArray();
            Object[] newElements = new Object[this.elements.length + toAdd.length];
            System.arraycopy(this.elements, 0, newElements, 0, index);
            System.arraycopy(toAdd, 0, newElements, index, toAdd.length);
            System.arraycopy(this.elements, index, newElements, toAdd.length + index, this.elements.length - index);
            this.elements = newElements;
            z = toAdd.length > 0;
        }
        return z;
    }

    public synchronized int addAllAbsent(Collection<? extends E> collection) {
        int addedCount;
        int addedCount2;
        Object[] toAdd = collection.toArray();
        Object[] newElements = new Object[this.elements.length + toAdd.length];
        System.arraycopy(this.elements, 0, newElements, 0, this.elements.length);
        int len$ = toAdd.length;
        int i$ = 0;
        addedCount = 0;
        while (i$ < len$) {
            Object o = toAdd[i$];
            if (indexOf(o, newElements, 0, this.elements.length + addedCount) == -1) {
                addedCount2 = addedCount + 1;
                newElements[this.elements.length + addedCount] = o;
            } else {
                addedCount2 = addedCount;
            }
            i$++;
            addedCount = addedCount2;
        }
        if (addedCount < toAdd.length) {
            newElements = Arrays.copyOfRange(newElements, 0, this.elements.length + addedCount);
        }
        this.elements = newElements;
        return addedCount;
    }

    public synchronized boolean addIfAbsent(E object) {
        boolean z;
        if (contains(object)) {
            z = false;
        } else {
            add(object);
            z = true;
        }
        return z;
    }

    @Override
    public synchronized void clear() {
        this.elements = EmptyArray.OBJECT;
    }

    @Override
    public synchronized E remove(int i) {
        E e;
        e = (E) this.elements[i];
        removeRange(i, i + 1);
        return e;
    }

    @Override
    public synchronized boolean remove(Object o) {
        boolean z;
        int index = indexOf(o);
        if (index == -1) {
            z = false;
        } else {
            remove(index);
            z = true;
        }
        return z;
    }

    @Override
    public synchronized boolean removeAll(Collection<?> collection) {
        boolean z;
        synchronized (this) {
            z = removeOrRetain(collection, false, 0, this.elements.length) != 0;
        }
        return z;
    }

    @Override
    public synchronized boolean retainAll(Collection<?> collection) {
        boolean z;
        synchronized (this) {
            z = removeOrRetain(collection, true, 0, this.elements.length) != 0;
        }
        return z;
    }

    private int removeOrRetain(Collection<?> collection, boolean retain, int from, int to) {
        int newSize;
        for (int i = from; i < to; i++) {
            if (collection.contains(this.elements[i]) != retain) {
                Object[] newElements = new Object[this.elements.length - 1];
                System.arraycopy(this.elements, 0, newElements, 0, i);
                int newSize2 = i;
                int j = i + 1;
                int newSize3 = newSize2;
                while (j < to) {
                    if (collection.contains(this.elements[j]) == retain) {
                        newSize = newSize3 + 1;
                        newElements[newSize3] = this.elements[j];
                    } else {
                        newSize = newSize3;
                    }
                    j++;
                    newSize3 = newSize;
                }
                System.arraycopy(this.elements, to, newElements, newSize3, this.elements.length - to);
                int newSize4 = newSize3 + (this.elements.length - to);
                if (newSize4 < newElements.length) {
                    newElements = Arrays.copyOfRange(newElements, 0, newSize4);
                }
                int removed = this.elements.length - newElements.length;
                this.elements = newElements;
                return removed;
            }
        }
        return 0;
    }

    @Override
    public synchronized E set(int i, E e) {
        E e2;
        Object[] objArr = (Object[]) this.elements.clone();
        e2 = (E) objArr[i];
        objArr[i] = e;
        this.elements = objArr;
        return e2;
    }

    private void removeRange(int from, int to) {
        Object[] newElements = new Object[this.elements.length - (to - from)];
        System.arraycopy(this.elements, 0, newElements, 0, from);
        System.arraycopy(this.elements, to, newElements, from, this.elements.length - to);
        this.elements = newElements;
    }

    static int lastIndexOf(Object o, Object[] data, int from, int to) {
        if (o == null) {
            for (int i = to - 1; i >= from; i--) {
                if (data[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i2 = to - 1; i2 >= from; i2--) {
                if (o.equals(data[i2])) {
                    return i2;
                }
            }
        }
        return -1;
    }

    static int indexOf(Object o, Object[] data, int from, int to) {
        if (o == null) {
            for (int i = from; i < to; i++) {
                if (data[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i2 = from; i2 < to; i2++) {
                if (o.equals(data[i2])) {
                    return i2;
                }
            }
        }
        return -1;
    }

    final Object[] getArray() {
        return this.elements;
    }

    class CowSubList extends AbstractList<E> {
        private volatile Slice slice;

        public CowSubList(Object[] expectedElements, int from, int to) {
            this.slice = new Slice(expectedElements, from, to);
        }

        @Override
        public int size() {
            Slice slice = this.slice;
            return slice.to - slice.from;
        }

        @Override
        public boolean isEmpty() {
            Slice slice = this.slice;
            return slice.from == slice.to;
        }

        @Override
        public E get(int i) {
            Slice slice = this.slice;
            Object[] objArr = CopyOnWriteArrayList.this.elements;
            slice.checkElementIndex(i);
            slice.checkConcurrentModification(objArr);
            return (E) objArr[slice.from + i];
        }

        @Override
        public Iterator<E> iterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            Slice slice = this.slice;
            Object[] snapshot = CopyOnWriteArrayList.this.elements;
            slice.checkPositionIndex(index);
            slice.checkConcurrentModification(snapshot);
            CowIterator<E> result = new CowIterator<>(snapshot, slice.from, slice.to);
            ((CowIterator) result).index = slice.from + index;
            return result;
        }

        @Override
        public int indexOf(Object object) {
            Slice slice = this.slice;
            Object[] snapshot = CopyOnWriteArrayList.this.elements;
            slice.checkConcurrentModification(snapshot);
            int result = CopyOnWriteArrayList.indexOf(object, snapshot, slice.from, slice.to);
            if (result != -1) {
                return result - slice.from;
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object object) {
            Slice slice = this.slice;
            Object[] snapshot = CopyOnWriteArrayList.this.elements;
            slice.checkConcurrentModification(snapshot);
            int result = CopyOnWriteArrayList.lastIndexOf(object, snapshot, slice.from, slice.to);
            if (result != -1) {
                return result - slice.from;
            }
            return -1;
        }

        @Override
        public boolean contains(Object object) {
            return indexOf(object) != -1;
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            Slice slice = this.slice;
            Object[] snapshot = CopyOnWriteArrayList.this.elements;
            slice.checkConcurrentModification(snapshot);
            return CopyOnWriteArrayList.containsAll(collection, snapshot, slice.from, slice.to);
        }

        @Override
        public List<E> subList(int from, int to) {
            Slice slice = this.slice;
            if (from < 0 || from > to || to > size()) {
                throw new IndexOutOfBoundsException("from=" + from + ", to=" + to + ", list size=" + size());
            }
            return new CowSubList(slice.expectedElements, slice.from + from, slice.from + to);
        }

        @Override
        public E remove(int i) {
            E e;
            synchronized (CopyOnWriteArrayList.this) {
                this.slice.checkElementIndex(i);
                this.slice.checkConcurrentModification(CopyOnWriteArrayList.this.elements);
                e = (E) CopyOnWriteArrayList.this.remove(this.slice.from + i);
                this.slice = new Slice(CopyOnWriteArrayList.this.elements, this.slice.from, this.slice.to - 1);
            }
            return e;
        }

        @Override
        public void clear() {
            synchronized (CopyOnWriteArrayList.this) {
                this.slice.checkConcurrentModification(CopyOnWriteArrayList.this.elements);
                CopyOnWriteArrayList.this.removeRange(this.slice.from, this.slice.to);
                this.slice = new Slice(CopyOnWriteArrayList.this.elements, this.slice.from, this.slice.from);
            }
        }

        @Override
        public void add(int index, E object) {
            synchronized (CopyOnWriteArrayList.this) {
                this.slice.checkPositionIndex(index);
                this.slice.checkConcurrentModification(CopyOnWriteArrayList.this.elements);
                CopyOnWriteArrayList.this.add(this.slice.from + index, object);
                this.slice = new Slice(CopyOnWriteArrayList.this.elements, this.slice.from, this.slice.to + 1);
            }
        }

        @Override
        public boolean add(E object) {
            synchronized (CopyOnWriteArrayList.this) {
                add(this.slice.to - this.slice.from, object);
            }
            return true;
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> collection) {
            boolean result;
            synchronized (CopyOnWriteArrayList.this) {
                this.slice.checkPositionIndex(index);
                this.slice.checkConcurrentModification(CopyOnWriteArrayList.this.elements);
                int oldSize = CopyOnWriteArrayList.this.elements.length;
                result = CopyOnWriteArrayList.this.addAll(this.slice.from + index, collection);
                this.slice = new Slice(CopyOnWriteArrayList.this.elements, this.slice.from, this.slice.to + (CopyOnWriteArrayList.this.elements.length - oldSize));
            }
            return result;
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            boolean zAddAll;
            synchronized (CopyOnWriteArrayList.this) {
                zAddAll = addAll(size(), collection);
            }
            return zAddAll;
        }

        @Override
        public E set(int i, E e) {
            E e2;
            synchronized (CopyOnWriteArrayList.this) {
                this.slice.checkElementIndex(i);
                this.slice.checkConcurrentModification(CopyOnWriteArrayList.this.elements);
                e2 = (E) CopyOnWriteArrayList.this.set(this.slice.from + i, e);
                this.slice = new Slice(CopyOnWriteArrayList.this.elements, this.slice.from, this.slice.to);
            }
            return e2;
        }

        @Override
        public boolean remove(Object object) {
            boolean z;
            synchronized (CopyOnWriteArrayList.this) {
                int index = indexOf(object);
                if (index == -1) {
                    z = false;
                } else {
                    remove(index);
                    z = true;
                }
            }
            return z;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            boolean z;
            synchronized (CopyOnWriteArrayList.this) {
                this.slice.checkConcurrentModification(CopyOnWriteArrayList.this.elements);
                int removed = CopyOnWriteArrayList.this.removeOrRetain(collection, false, this.slice.from, this.slice.to);
                this.slice = new Slice(CopyOnWriteArrayList.this.elements, this.slice.from, this.slice.to - removed);
                z = removed != 0;
            }
            return z;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            boolean z;
            synchronized (CopyOnWriteArrayList.this) {
                this.slice.checkConcurrentModification(CopyOnWriteArrayList.this.elements);
                int removed = CopyOnWriteArrayList.this.removeOrRetain(collection, true, this.slice.from, this.slice.to);
                this.slice = new Slice(CopyOnWriteArrayList.this.elements, this.slice.from, this.slice.to - removed);
                z = removed != 0;
            }
            return z;
        }
    }

    static class Slice {
        private final Object[] expectedElements;
        private final int from;
        private final int to;

        Slice(Object[] expectedElements, int from, int to) {
            this.expectedElements = expectedElements;
            this.from = from;
            this.to = to;
        }

        void checkElementIndex(int index) {
            if (index < 0 || index >= this.to - this.from) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + (this.to - this.from));
            }
        }

        void checkPositionIndex(int index) {
            if (index < 0 || index > this.to - this.from) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + (this.to - this.from));
            }
        }

        void checkConcurrentModification(Object[] snapshot) {
            if (this.expectedElements != snapshot) {
                throw new ConcurrentModificationException();
            }
        }
    }

    static class CowIterator<E> implements ListIterator<E> {
        private final int from;
        private int index;
        private final Object[] snapshot;
        private final int to;

        CowIterator(Object[] snapshot, int from, int to) {
            this.index = 0;
            this.snapshot = snapshot;
            this.from = from;
            this.to = to;
            this.index = from;
        }

        @Override
        public void add(E object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return this.index < this.to;
        }

        @Override
        public boolean hasPrevious() {
            return this.index > this.from;
        }

        @Override
        public E next() {
            if (this.index < this.to) {
                Object[] objArr = this.snapshot;
                int i = this.index;
                this.index = i + 1;
                return (E) objArr[i];
            }
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return this.index;
        }

        @Override
        public E previous() {
            if (this.index > this.from) {
                Object[] objArr = this.snapshot;
                int i = this.index - 1;
                this.index = i;
                return (E) objArr[i];
            }
            throw new NoSuchElementException();
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
        public void set(E object) {
            throw new UnsupportedOperationException();
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        Object[] snapshot = this.elements;
        out.defaultWriteObject();
        out.writeInt(snapshot.length);
        for (Object o : snapshot) {
            out.writeObject(o);
        }
    }

    private synchronized void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Object[] snapshot = new Object[in.readInt()];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = in.readObject();
        }
        this.elements = snapshot;
    }
}
