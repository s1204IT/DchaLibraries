package java.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class Vector<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
    private static final int MAX_ARRAY_SIZE = 2147483639;
    private static final long serialVersionUID = -2767605614048989439L;
    protected int capacityIncrement;
    protected int elementCount;
    protected Object[] elementData;

    public Vector(int i, int i2) {
        if (i < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + i);
        }
        this.elementData = new Object[i];
        this.capacityIncrement = i2;
    }

    public Vector(int i) {
        this(i, 0);
    }

    public Vector() {
        this(10);
    }

    public Vector(Collection<? extends E> collection) {
        this.elementData = collection.toArray();
        this.elementCount = this.elementData.length;
        if (this.elementData.getClass() != Object[].class) {
            this.elementData = Arrays.copyOf(this.elementData, this.elementCount, Object[].class);
        }
    }

    public synchronized void copyInto(Object[] objArr) {
        System.arraycopy(this.elementData, 0, objArr, 0, this.elementCount);
    }

    public synchronized void trimToSize() {
        this.modCount++;
        if (this.elementCount < this.elementData.length) {
            this.elementData = Arrays.copyOf(this.elementData, this.elementCount);
        }
    }

    public synchronized void ensureCapacity(int i) {
        if (i > 0) {
            this.modCount++;
            ensureCapacityHelper(i);
        }
    }

    private void ensureCapacityHelper(int i) {
        if (i - this.elementData.length > 0) {
            grow(i);
        }
    }

    private void grow(int i) {
        int length = this.elementData.length;
        int iHugeCapacity = length + (this.capacityIncrement > 0 ? this.capacityIncrement : length);
        if (iHugeCapacity - i < 0) {
            iHugeCapacity = i;
        }
        if (iHugeCapacity - MAX_ARRAY_SIZE > 0) {
            iHugeCapacity = hugeCapacity(i);
        }
        this.elementData = Arrays.copyOf(this.elementData, iHugeCapacity);
    }

    private static int hugeCapacity(int i) {
        if (i < 0) {
            throw new OutOfMemoryError();
        }
        if (i <= MAX_ARRAY_SIZE) {
            return MAX_ARRAY_SIZE;
        }
        return Integer.MAX_VALUE;
    }

    public synchronized void setSize(int i) {
        this.modCount++;
        if (i > this.elementCount) {
            ensureCapacityHelper(i);
        } else {
            for (int i2 = i; i2 < this.elementCount; i2++) {
                this.elementData[i2] = null;
            }
        }
        this.elementCount = i;
    }

    public synchronized int capacity() {
        return this.elementData.length;
    }

    @Override
    public synchronized int size() {
        return this.elementCount;
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.elementCount == 0;
    }

    public Enumeration<E> elements() {
        return new Enumeration<E>() {
            int count = 0;

            @Override
            public boolean hasMoreElements() {
                return this.count < Vector.this.elementCount;
            }

            @Override
            public E nextElement() {
                synchronized (Vector.this) {
                    if (this.count < Vector.this.elementCount) {
                        Vector vector = Vector.this;
                        int i = this.count;
                        this.count = i + 1;
                        return (E) vector.elementData(i);
                    }
                    throw new NoSuchElementException("Vector Enumeration");
                }
            }
        };
    }

    @Override
    public boolean contains(Object obj) {
        return indexOf(obj, 0) >= 0;
    }

    @Override
    public int indexOf(Object obj) {
        return indexOf(obj, 0);
    }

    public synchronized int indexOf(Object obj, int i) {
        if (obj == null) {
            while (i < this.elementCount) {
                if (this.elementData[i] == null) {
                    return i;
                }
                i++;
            }
        } else {
            while (i < this.elementCount) {
                if (obj.equals(this.elementData[i])) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    @Override
    public synchronized int lastIndexOf(Object obj) {
        return lastIndexOf(obj, this.elementCount - 1);
    }

    public synchronized int lastIndexOf(Object obj, int i) {
        if (i >= this.elementCount) {
            throw new IndexOutOfBoundsException(i + " >= " + this.elementCount);
        }
        if (obj == null) {
            while (i >= 0) {
                if (this.elementData[i] == null) {
                    return i;
                }
                i--;
            }
        } else {
            while (i >= 0) {
                if (obj.equals(this.elementData[i])) {
                    return i;
                }
                i--;
            }
        }
        return -1;
    }

    public synchronized E elementAt(int i) {
        if (i >= this.elementCount) {
            throw new ArrayIndexOutOfBoundsException(i + " >= " + this.elementCount);
        }
        return elementData(i);
    }

    public synchronized E firstElement() {
        if (this.elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData(0);
    }

    public synchronized E lastElement() {
        if (this.elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData(this.elementCount - 1);
    }

    public synchronized void setElementAt(E e, int i) {
        if (i >= this.elementCount) {
            throw new ArrayIndexOutOfBoundsException(i + " >= " + this.elementCount);
        }
        this.elementData[i] = e;
    }

    public synchronized void removeElementAt(int i) {
        this.modCount++;
        if (i >= this.elementCount) {
            throw new ArrayIndexOutOfBoundsException(i + " >= " + this.elementCount);
        }
        if (i < 0) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        int i2 = (this.elementCount - i) - 1;
        if (i2 > 0) {
            System.arraycopy(this.elementData, i + 1, this.elementData, i, i2);
        }
        this.elementCount--;
        this.elementData[this.elementCount] = null;
    }

    public synchronized void insertElementAt(E e, int i) {
        this.modCount++;
        if (i > this.elementCount) {
            throw new ArrayIndexOutOfBoundsException(i + " > " + this.elementCount);
        }
        ensureCapacityHelper(this.elementCount + 1);
        System.arraycopy(this.elementData, i, this.elementData, i + 1, this.elementCount - i);
        this.elementData[i] = e;
        this.elementCount++;
    }

    public synchronized void addElement(E e) {
        this.modCount++;
        ensureCapacityHelper(this.elementCount + 1);
        Object[] objArr = this.elementData;
        int i = this.elementCount;
        this.elementCount = i + 1;
        objArr[i] = e;
    }

    public synchronized boolean removeElement(Object obj) {
        this.modCount++;
        int iIndexOf = indexOf(obj);
        if (iIndexOf >= 0) {
            removeElementAt(iIndexOf);
            return true;
        }
        return false;
    }

    public synchronized void removeAllElements() {
        this.modCount++;
        for (int i = 0; i < this.elementCount; i++) {
            this.elementData[i] = null;
        }
        this.elementCount = 0;
    }

    public synchronized Object clone() {
        Vector vector;
        try {
            vector = (Vector) super.clone();
            vector.elementData = Arrays.copyOf(this.elementData, this.elementCount);
            vector.modCount = 0;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
        return vector;
    }

    @Override
    public synchronized Object[] toArray() {
        return Arrays.copyOf(this.elementData, this.elementCount);
    }

    @Override
    public synchronized <T> T[] toArray(T[] tArr) {
        if (tArr.length < this.elementCount) {
            return (T[]) Arrays.copyOf(this.elementData, this.elementCount, tArr.getClass());
        }
        System.arraycopy(this.elementData, 0, tArr, 0, this.elementCount);
        if (tArr.length > this.elementCount) {
            tArr[this.elementCount] = null;
        }
        return tArr;
    }

    E elementData(int i) {
        return (E) this.elementData[i];
    }

    @Override
    public synchronized E get(int i) {
        if (i >= this.elementCount) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        return elementData(i);
    }

    @Override
    public synchronized E set(int i, E e) {
        E eElementData;
        if (i >= this.elementCount) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        eElementData = elementData(i);
        this.elementData[i] = e;
        return eElementData;
    }

    @Override
    public synchronized boolean add(E e) {
        this.modCount++;
        ensureCapacityHelper(this.elementCount + 1);
        Object[] objArr = this.elementData;
        int i = this.elementCount;
        this.elementCount = i + 1;
        objArr[i] = e;
        return true;
    }

    @Override
    public boolean remove(Object obj) {
        return removeElement(obj);
    }

    @Override
    public void add(int i, E e) {
        insertElementAt(e, i);
    }

    @Override
    public synchronized E remove(int i) {
        E eElementData;
        this.modCount++;
        if (i >= this.elementCount) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        eElementData = elementData(i);
        int i2 = (this.elementCount - i) - 1;
        if (i2 > 0) {
            System.arraycopy(this.elementData, i + 1, this.elementData, i, i2);
        }
        Object[] objArr = this.elementData;
        int i3 = this.elementCount - 1;
        this.elementCount = i3;
        objArr[i3] = null;
        return eElementData;
    }

    @Override
    public void clear() {
        removeAllElements();
    }

    @Override
    public synchronized boolean containsAll(Collection<?> collection) {
        return super.containsAll(collection);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> collection) {
        int length;
        this.modCount++;
        Object[] array = collection.toArray();
        length = array.length;
        ensureCapacityHelper(this.elementCount + length);
        System.arraycopy(array, 0, this.elementData, this.elementCount, length);
        this.elementCount += length;
        return length != 0;
    }

    @Override
    public synchronized boolean removeAll(Collection<?> collection) {
        return super.removeAll(collection);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> collection) {
        return super.retainAll(collection);
    }

    @Override
    public synchronized boolean addAll(int i, Collection<? extends E> collection) {
        int length;
        this.modCount++;
        if (i < 0 || i > this.elementCount) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        Object[] array = collection.toArray();
        length = array.length;
        ensureCapacityHelper(this.elementCount + length);
        int i2 = this.elementCount - i;
        if (i2 > 0) {
            System.arraycopy(this.elementData, i, this.elementData, i + length, i2);
        }
        System.arraycopy(array, 0, this.elementData, i, length);
        this.elementCount += length;
        return length != 0;
    }

    @Override
    public synchronized boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public synchronized int hashCode() {
        return super.hashCode();
    }

    @Override
    public synchronized String toString() {
        return super.toString();
    }

    @Override
    public synchronized List<E> subList(int i, int i2) {
        return Collections.synchronizedList(super.subList(i, i2), this);
    }

    @Override
    protected synchronized void removeRange(int i, int i2) {
        this.modCount++;
        System.arraycopy(this.elementData, i2, this.elementData, i, this.elementCount - i2);
        int i3 = this.elementCount - (i2 - i);
        while (this.elementCount != i3) {
            Object[] objArr = this.elementData;
            int i4 = this.elementCount - 1;
            this.elementCount = i4;
            objArr[i4] = null;
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        Object[] objArr;
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        synchronized (this) {
            putFieldPutFields.put("capacityIncrement", this.capacityIncrement);
            putFieldPutFields.put("elementCount", this.elementCount);
            objArr = (Object[]) this.elementData.clone();
        }
        putFieldPutFields.put("elementData", objArr);
        objectOutputStream.writeFields();
    }

    @Override
    public synchronized ListIterator<E> listIterator(int i) {
        if (i >= 0) {
            if (i <= this.elementCount) {
            }
        }
        throw new IndexOutOfBoundsException("Index: " + i);
        return new ListItr(i);
    }

    @Override
    public synchronized ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    @Override
    public synchronized Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        int cursor;
        int expectedModCount;
        int lastRet;
        protected int limit;

        private Itr() {
            this.limit = Vector.this.elementCount;
            this.lastRet = -1;
            this.expectedModCount = Vector.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return this.cursor < this.limit;
        }

        @Override
        public E next() {
            E e;
            synchronized (Vector.this) {
                checkForComodification();
                int i = this.cursor;
                if (i >= this.limit) {
                    throw new NoSuchElementException();
                }
                this.cursor = i + 1;
                Vector vector = Vector.this;
                this.lastRet = i;
                e = (E) vector.elementData(i);
            }
            return e;
        }

        @Override
        public void remove() {
            if (this.lastRet == -1) {
                throw new IllegalStateException();
            }
            synchronized (Vector.this) {
                checkForComodification();
                Vector.this.remove(this.lastRet);
                this.expectedModCount = Vector.this.modCount;
                this.limit--;
            }
            this.cursor = this.lastRet;
            this.lastRet = -1;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            synchronized (Vector.this) {
                int i = this.limit;
                int i2 = this.cursor;
                if (i2 >= i) {
                    return;
                }
                Object[] objArr = Vector.this.elementData;
                if (i2 >= objArr.length) {
                    throw new ConcurrentModificationException();
                }
                while (i2 != i && Vector.this.modCount == this.expectedModCount) {
                    consumer.accept(objArr[i2]);
                    i2++;
                }
                this.cursor = i2;
                this.lastRet = i2 - 1;
                checkForComodification();
            }
        }

        final void checkForComodification() {
            if (Vector.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    final class ListItr extends Vector<E>.Itr implements ListIterator<E> {
        ListItr(int i) {
            super();
            this.cursor = i;
        }

        @Override
        public boolean hasPrevious() {
            return this.cursor != 0;
        }

        @Override
        public int nextIndex() {
            return this.cursor;
        }

        @Override
        public int previousIndex() {
            return this.cursor - 1;
        }

        @Override
        public E previous() {
            E e;
            synchronized (Vector.this) {
                checkForComodification();
                int i = this.cursor - 1;
                if (i < 0) {
                    throw new NoSuchElementException();
                }
                this.cursor = i;
                Vector vector = Vector.this;
                this.lastRet = i;
                e = (E) vector.elementData(i);
            }
            return e;
        }

        @Override
        public void set(E e) {
            if (this.lastRet == -1) {
                throw new IllegalStateException();
            }
            synchronized (Vector.this) {
                checkForComodification();
                Vector.this.set(this.lastRet, e);
            }
        }

        @Override
        public void add(E e) {
            int i = this.cursor;
            synchronized (Vector.this) {
                checkForComodification();
                Vector.this.add(i, e);
                this.expectedModCount = Vector.this.modCount;
                this.limit++;
            }
            this.cursor = i + 1;
            this.lastRet = -1;
        }
    }

    @Override
    public synchronized void forEach(Consumer<? super E> consumer) {
        Objects.requireNonNull(consumer);
        int i = this.modCount;
        Object[] objArr = this.elementData;
        int i2 = this.elementCount;
        for (int i3 = 0; this.modCount == i && i3 < i2; i3++) {
            consumer.accept(objArr[i3]);
        }
        if (this.modCount != i) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public synchronized boolean removeIf(Predicate<? super E> predicate) {
        boolean z;
        Objects.requireNonNull(predicate);
        int i = this.elementCount;
        BitSet bitSet = new BitSet(i);
        int i2 = this.modCount;
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 0; this.modCount == i2 && i5 < i; i5++) {
            if (predicate.test(this.elementData[i5])) {
                bitSet.set(i5);
                i4++;
            }
        }
        if (this.modCount != i2) {
            throw new ConcurrentModificationException();
        }
        z = i4 > 0;
        if (z) {
            int i6 = i - i4;
            for (int i7 = 0; i3 < i && i7 < i6; i7++) {
                int iNextClearBit = bitSet.nextClearBit(i3);
                this.elementData[i7] = this.elementData[iNextClearBit];
                i3 = iNextClearBit + 1;
            }
            for (int i8 = i6; i8 < i; i8++) {
                this.elementData[i8] = null;
            }
            this.elementCount = i6;
            if (this.modCount != i2) {
                throw new ConcurrentModificationException();
            }
            this.modCount++;
        }
        return z;
    }

    @Override
    public synchronized void replaceAll(UnaryOperator<E> unaryOperator) {
        Objects.requireNonNull(unaryOperator);
        int i = this.modCount;
        int i2 = this.elementCount;
        for (int i3 = 0; this.modCount == i && i3 < i2; i3++) {
            this.elementData[i3] = unaryOperator.apply(this.elementData[i3]);
        }
        if (this.modCount != i) {
            throw new ConcurrentModificationException();
        }
        this.modCount++;
    }

    @Override
    public synchronized void sort(Comparator<? super E> comparator) {
        int i = this.modCount;
        Arrays.sort(this.elementData, 0, this.elementCount, comparator);
        if (this.modCount != i) {
            throw new ConcurrentModificationException();
        }
        this.modCount++;
    }

    @Override
    public Spliterator<E> spliterator() {
        return new VectorSpliterator(this, null, 0, -1, 0);
    }

    static final class VectorSpliterator<E> implements Spliterator<E> {
        private Object[] array;
        private int expectedModCount;
        private int fence;
        private int index;
        private final Vector<E> list;

        VectorSpliterator(Vector<E> vector, Object[] objArr, int i, int i2, int i3) {
            this.list = vector;
            this.array = objArr;
            this.index = i;
            this.fence = i2;
            this.expectedModCount = i3;
        }

        private int getFence() {
            int i = this.fence;
            if (i < 0) {
                synchronized (this.list) {
                    this.array = this.list.elementData;
                    this.expectedModCount = this.list.modCount;
                    i = this.list.elementCount;
                    this.fence = i;
                }
            }
            return i;
        }

        @Override
        public Spliterator<E> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = (fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            Vector<E> vector = this.list;
            Object[] objArr = this.array;
            this.index = i2;
            return new VectorSpliterator(vector, objArr, i, i2, this.expectedModCount);
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            int fence = getFence();
            int i = this.index;
            if (fence > i) {
                this.index = i + 1;
                consumer.accept(this.array[i]);
                if (this.list.modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            int i;
            Object[] objArr;
            int i2;
            if (consumer == null) {
                throw new NullPointerException();
            }
            Vector<E> vector = this.list;
            if (vector != null) {
                int i3 = this.fence;
                if (i3 < 0) {
                    synchronized (vector) {
                        this.expectedModCount = vector.modCount;
                        objArr = vector.elementData;
                        this.array = objArr;
                        i = vector.elementCount;
                        this.fence = i;
                    }
                } else {
                    i = i3;
                    objArr = this.array;
                }
                if (objArr != null && (i2 = this.index) >= 0) {
                    this.index = i;
                    if (i <= objArr.length) {
                        for (i2 = this.index; i2 < i; i2++) {
                            consumer.accept(objArr[i2]);
                        }
                        if (vector.modCount == this.expectedModCount) {
                            return;
                        }
                    }
                }
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public long estimateSize() {
            return getFence() - this.index;
        }

        @Override
        public int characteristics() {
            return 16464;
        }
    }
}
