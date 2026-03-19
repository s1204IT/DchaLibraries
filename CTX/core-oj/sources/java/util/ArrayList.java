package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
    private static final int DEFAULT_CAPACITY = 10;
    private static final int MAX_ARRAY_SIZE = 2147483639;
    private static final long serialVersionUID = 8683452581122892189L;
    transient Object[] elementData;
    private int size;
    private static final Object[] EMPTY_ELEMENTDATA = new Object[0];
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = new Object[0];

    public ArrayList(int i) {
        if (i > 0) {
            this.elementData = new Object[i];
        } else {
            if (i == 0) {
                this.elementData = EMPTY_ELEMENTDATA;
                return;
            }
            throw new IllegalArgumentException("Illegal Capacity: " + i);
        }
    }

    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    public ArrayList(Collection<? extends E> collection) {
        this.elementData = collection.toArray();
        int length = this.elementData.length;
        this.size = length;
        if (length != 0) {
            if (this.elementData.getClass() != Object[].class) {
                this.elementData = Arrays.copyOf(this.elementData, this.size, Object[].class);
                return;
            }
            return;
        }
        this.elementData = EMPTY_ELEMENTDATA;
    }

    public void trimToSize() {
        Object[] objArrCopyOf;
        this.modCount++;
        if (this.size < this.elementData.length) {
            if (this.size == 0) {
                objArrCopyOf = EMPTY_ELEMENTDATA;
            } else {
                objArrCopyOf = Arrays.copyOf(this.elementData, this.size);
            }
            this.elementData = objArrCopyOf;
        }
    }

    public void ensureCapacity(int i) {
        int i2;
        if (this.elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            i2 = 0;
        } else {
            i2 = 10;
        }
        if (i > i2) {
            ensureExplicitCapacity(i);
        }
    }

    private void ensureCapacityInternal(int i) {
        if (this.elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            i = Math.max(10, i);
        }
        ensureExplicitCapacity(i);
    }

    private void ensureExplicitCapacity(int i) {
        this.modCount++;
        if (i - this.elementData.length > 0) {
            grow(i);
        }
    }

    private void grow(int i) {
        int length = this.elementData.length;
        int iHugeCapacity = length + (length >> 1);
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

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public boolean contains(Object obj) {
        return indexOf(obj) >= 0;
    }

    @Override
    public int indexOf(Object obj) {
        int i = 0;
        if (obj == null) {
            while (i < this.size) {
                if (this.elementData[i] != null) {
                    i++;
                } else {
                    return i;
                }
            }
            return -1;
        }
        while (i < this.size) {
            if (!obj.equals(this.elementData[i])) {
                i++;
            } else {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object obj) {
        if (obj == null) {
            for (int i = this.size - 1; i >= 0; i--) {
                if (this.elementData[i] == null) {
                    return i;
                }
            }
            return -1;
        }
        for (int i2 = this.size - 1; i2 >= 0; i2--) {
            if (obj.equals(this.elementData[i2])) {
                return i2;
            }
        }
        return -1;
    }

    public Object clone() {
        try {
            ArrayList arrayList = (ArrayList) super.clone();
            arrayList.elementData = Arrays.copyOf(this.elementData, this.size);
            arrayList.modCount = 0;
            return arrayList;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(this.elementData, this.size);
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        if (tArr.length < this.size) {
            return (T[]) Arrays.copyOf(this.elementData, this.size, tArr.getClass());
        }
        System.arraycopy(this.elementData, 0, tArr, 0, this.size);
        if (tArr.length > this.size) {
            tArr[this.size] = null;
        }
        return tArr;
    }

    @Override
    public E get(int i) {
        if (i >= this.size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
        }
        return (E) this.elementData[i];
    }

    @Override
    public E set(int i, E e) {
        if (i >= this.size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
        }
        E e2 = (E) this.elementData[i];
        this.elementData[i] = e;
        return e2;
    }

    @Override
    public boolean add(E e) {
        ensureCapacityInternal(this.size + 1);
        Object[] objArr = this.elementData;
        int i = this.size;
        this.size = i + 1;
        objArr[i] = e;
        return true;
    }

    @Override
    public void add(int i, E e) {
        if (i > this.size || i < 0) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
        }
        ensureCapacityInternal(this.size + 1);
        System.arraycopy(this.elementData, i, this.elementData, i + 1, this.size - i);
        this.elementData[i] = e;
        this.size++;
    }

    @Override
    public E remove(int i) {
        if (i >= this.size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
        }
        this.modCount++;
        E e = (E) this.elementData[i];
        int i2 = (this.size - i) - 1;
        if (i2 > 0) {
            System.arraycopy(this.elementData, i + 1, this.elementData, i, i2);
        }
        Object[] objArr = this.elementData;
        int i3 = this.size - 1;
        this.size = i3;
        objArr[i3] = null;
        return e;
    }

    @Override
    public boolean remove(Object obj) {
        if (obj == null) {
            for (int i = 0; i < this.size; i++) {
                if (this.elementData[i] == null) {
                    fastRemove(i);
                    return true;
                }
            }
        } else {
            for (int i2 = 0; i2 < this.size; i2++) {
                if (obj.equals(this.elementData[i2])) {
                    fastRemove(i2);
                    return true;
                }
            }
        }
        return false;
    }

    private void fastRemove(int i) {
        this.modCount++;
        int i2 = (this.size - i) - 1;
        if (i2 > 0) {
            System.arraycopy(this.elementData, i + 1, this.elementData, i, i2);
        }
        Object[] objArr = this.elementData;
        int i3 = this.size - 1;
        this.size = i3;
        objArr[i3] = null;
    }

    @Override
    public void clear() {
        this.modCount++;
        for (int i = 0; i < this.size; i++) {
            this.elementData[i] = null;
        }
        this.size = 0;
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        Object[] array = collection.toArray();
        int length = array.length;
        ensureCapacityInternal(this.size + length);
        System.arraycopy(array, 0, this.elementData, this.size, length);
        this.size += length;
        return length != 0;
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> collection) {
        if (i > this.size || i < 0) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
        }
        Object[] array = collection.toArray();
        int length = array.length;
        ensureCapacityInternal(this.size + length);
        int i2 = this.size - i;
        if (i2 > 0) {
            System.arraycopy(this.elementData, i, this.elementData, i + length, i2);
        }
        System.arraycopy(array, 0, this.elementData, i, length);
        this.size += length;
        return length != 0;
    }

    @Override
    protected void removeRange(int i, int i2) {
        if (i2 < i) {
            throw new IndexOutOfBoundsException("toIndex < fromIndex");
        }
        this.modCount++;
        System.arraycopy(this.elementData, i2, this.elementData, i, this.size - i2);
        int i3 = this.size - (i2 - i);
        for (int i4 = i3; i4 < this.size; i4++) {
            this.elementData[i4] = null;
        }
        this.size = i3;
    }

    private String outOfBoundsMsg(int i) {
        return "Index: " + i + ", Size: " + this.size;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        Objects.requireNonNull(collection);
        return batchRemove(collection, false);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        Objects.requireNonNull(collection);
        return batchRemove(collection, true);
    }

    private boolean batchRemove(Collection<?> collection, boolean z) throws Throwable {
        Object[] objArr = this.elementData;
        int i = 0;
        int i2 = 0;
        while (i < this.size) {
            try {
                if (collection.contains(objArr[i]) == z) {
                    int i3 = i2 + 1;
                    try {
                        objArr[i2] = objArr[i];
                        i2 = i3;
                    } catch (Throwable th) {
                        th = th;
                        i2 = i3;
                        if (i != this.size) {
                            System.arraycopy(objArr, i, objArr, i2, this.size - i);
                            i2 += this.size - i;
                        }
                        if (i2 != this.size) {
                            for (int i4 = i2; i4 < this.size; i4++) {
                                objArr[i4] = null;
                            }
                            this.modCount += this.size - i2;
                            this.size = i2;
                        }
                        throw th;
                    }
                }
                i++;
            } catch (Throwable th2) {
                th = th2;
            }
        }
        if (i != this.size) {
            System.arraycopy(objArr, i, objArr, i2, this.size - i);
            i2 += this.size - i;
        }
        if (i2 == this.size) {
            return false;
        }
        for (int i5 = i2; i5 < this.size; i5++) {
            objArr[i5] = null;
        }
        this.modCount += this.size - i2;
        this.size = i2;
        return true;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        int i = this.modCount;
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(this.size);
        for (int i2 = 0; i2 < this.size; i2++) {
            objectOutputStream.writeObject(this.elementData[i2]);
        }
        if (this.modCount != i) {
            throw new ConcurrentModificationException();
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        this.elementData = EMPTY_ELEMENTDATA;
        objectInputStream.defaultReadObject();
        objectInputStream.readInt();
        if (this.size > 0) {
            ensureCapacityInternal(this.size);
            Object[] objArr = this.elementData;
            for (int i = 0; i < this.size; i++) {
                objArr[i] = objectInputStream.readObject();
            }
        }
    }

    @Override
    public ListIterator<E> listIterator(int i) {
        if (i < 0 || i > this.size) {
            throw new IndexOutOfBoundsException("Index: " + i);
        }
        return new ListItr(i);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        int cursor;
        int expectedModCount;
        int lastRet;
        protected int limit;

        private Itr() {
            this.limit = ArrayList.this.size;
            this.lastRet = -1;
            this.expectedModCount = ArrayList.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return this.cursor < this.limit;
        }

        @Override
        public E next() {
            if (ArrayList.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            int i = this.cursor;
            if (i >= this.limit) {
                throw new NoSuchElementException();
            }
            Object[] objArr = ArrayList.this.elementData;
            if (i >= objArr.length) {
                throw new ConcurrentModificationException();
            }
            this.cursor = i + 1;
            this.lastRet = i;
            return (E) objArr[i];
        }

        @Override
        public void remove() {
            if (this.lastRet < 0) {
                throw new IllegalStateException();
            }
            if (ArrayList.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            try {
                ArrayList.this.remove(this.lastRet);
                this.cursor = this.lastRet;
                this.lastRet = -1;
                this.expectedModCount = ArrayList.this.modCount;
                this.limit--;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            int i = ArrayList.this.size;
            int i2 = this.cursor;
            if (i2 >= i) {
                return;
            }
            Object[] objArr = ArrayList.this.elementData;
            if (i2 >= objArr.length) {
                throw new ConcurrentModificationException();
            }
            while (i2 != i && ArrayList.this.modCount == this.expectedModCount) {
                consumer.accept(objArr[i2]);
                i2++;
            }
            this.cursor = i2;
            this.lastRet = i2 - 1;
            if (ArrayList.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private class ListItr extends ArrayList<E>.Itr implements ListIterator<E> {
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
            if (ArrayList.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            int i = this.cursor - 1;
            if (i < 0) {
                throw new NoSuchElementException();
            }
            Object[] objArr = ArrayList.this.elementData;
            if (i >= objArr.length) {
                throw new ConcurrentModificationException();
            }
            this.cursor = i;
            this.lastRet = i;
            return (E) objArr[i];
        }

        @Override
        public void set(E e) {
            if (this.lastRet < 0) {
                throw new IllegalStateException();
            }
            if (ArrayList.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            try {
                ArrayList.this.set(this.lastRet, e);
            } catch (IndexOutOfBoundsException e2) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void add(E e) {
            if (ArrayList.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
            try {
                int i = this.cursor;
                ArrayList.this.add(i, e);
                this.cursor = i + 1;
                this.lastRet = -1;
                this.expectedModCount = ArrayList.this.modCount;
                this.limit++;
            } catch (IndexOutOfBoundsException e2) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public List<E> subList(int i, int i2) {
        subListRangeCheck(i, i2, this.size);
        return new SubList(this, 0, i, i2);
    }

    static void subListRangeCheck(int i, int i2, int i3) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + i);
        }
        if (i2 > i3) {
            throw new IndexOutOfBoundsException("toIndex = " + i2);
        }
        if (i > i2) {
            throw new IllegalArgumentException("fromIndex(" + i + ") > toIndex(" + i2 + ")");
        }
    }

    private class SubList extends AbstractList<E> implements RandomAccess {
        private final int offset;
        private final AbstractList<E> parent;
        private final int parentOffset;
        int size;

        SubList(AbstractList<E> abstractList, int i, int i2, int i3) {
            this.parent = abstractList;
            this.parentOffset = i2;
            this.offset = i + i2;
            this.size = i3 - i2;
            this.modCount = ArrayList.this.modCount;
        }

        @Override
        public E set(int i, E e) {
            if (i < 0 || i >= this.size) {
                throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
            }
            if (ArrayList.this.modCount != this.modCount) {
                throw new ConcurrentModificationException();
            }
            E e2 = (E) ArrayList.this.elementData[this.offset + i];
            ArrayList.this.elementData[this.offset + i] = e;
            return e2;
        }

        @Override
        public E get(int i) {
            if (i < 0 || i >= this.size) {
                throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
            }
            if (ArrayList.this.modCount != this.modCount) {
                throw new ConcurrentModificationException();
            }
            return (E) ArrayList.this.elementData[this.offset + i];
        }

        @Override
        public int size() {
            if (ArrayList.this.modCount != this.modCount) {
                throw new ConcurrentModificationException();
            }
            return this.size;
        }

        @Override
        public void add(int i, E e) {
            if (i < 0 || i > this.size) {
                throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
            }
            if (ArrayList.this.modCount != this.modCount) {
                throw new ConcurrentModificationException();
            }
            this.parent.add(this.parentOffset + i, e);
            this.modCount = this.parent.modCount;
            this.size++;
        }

        @Override
        public E remove(int i) {
            if (i < 0 || i >= this.size) {
                throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
            }
            if (ArrayList.this.modCount != this.modCount) {
                throw new ConcurrentModificationException();
            }
            E eRemove = this.parent.remove(this.parentOffset + i);
            this.modCount = this.parent.modCount;
            this.size--;
            return eRemove;
        }

        @Override
        protected void removeRange(int i, int i2) {
            if (ArrayList.this.modCount != this.modCount) {
                throw new ConcurrentModificationException();
            }
            this.parent.removeRange(this.parentOffset + i, this.parentOffset + i2);
            this.modCount = this.parent.modCount;
            this.size -= i2 - i;
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            return addAll(this.size, collection);
        }

        @Override
        public boolean addAll(int i, Collection<? extends E> collection) {
            if (i < 0 || i > this.size) {
                throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
            }
            int size = collection.size();
            if (size == 0) {
                return false;
            }
            if (ArrayList.this.modCount != this.modCount) {
                throw new ConcurrentModificationException();
            }
            this.parent.addAll(this.parentOffset + i, collection);
            this.modCount = this.parent.modCount;
            this.size += size;
            return true;
        }

        @Override
        public Iterator<E> iterator() {
            return listIterator();
        }

        @Override
        public ListIterator<E> listIterator(final int i) {
            if (ArrayList.this.modCount != this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (i < 0 || i > this.size) {
                throw new IndexOutOfBoundsException(outOfBoundsMsg(i));
            }
            final int i2 = this.offset;
            return new ListIterator<E>() {
                int cursor;
                int expectedModCount;
                int lastRet = -1;

                {
                    this.cursor = i;
                    this.expectedModCount = ArrayList.this.modCount;
                }

                @Override
                public boolean hasNext() {
                    return this.cursor != SubList.this.size;
                }

                @Override
                public E next() {
                    if (this.expectedModCount != ArrayList.this.modCount) {
                        throw new ConcurrentModificationException();
                    }
                    int i3 = this.cursor;
                    if (i3 >= SubList.this.size) {
                        throw new NoSuchElementException();
                    }
                    Object[] objArr = ArrayList.this.elementData;
                    if (i2 + i3 >= objArr.length) {
                        throw new ConcurrentModificationException();
                    }
                    this.cursor = i3 + 1;
                    int i4 = i2;
                    this.lastRet = i3;
                    return (E) objArr[i4 + i3];
                }

                @Override
                public boolean hasPrevious() {
                    return this.cursor != 0;
                }

                @Override
                public E previous() {
                    if (this.expectedModCount != ArrayList.this.modCount) {
                        throw new ConcurrentModificationException();
                    }
                    int i3 = this.cursor - 1;
                    if (i3 < 0) {
                        throw new NoSuchElementException();
                    }
                    Object[] objArr = ArrayList.this.elementData;
                    if (i2 + i3 >= objArr.length) {
                        throw new ConcurrentModificationException();
                    }
                    this.cursor = i3;
                    int i4 = i2;
                    this.lastRet = i3;
                    return (E) objArr[i4 + i3];
                }

                @Override
                public void forEachRemaining(Consumer<? super E> consumer) {
                    Objects.requireNonNull(consumer);
                    int i3 = SubList.this.size;
                    int i4 = this.cursor;
                    if (i4 >= i3) {
                        return;
                    }
                    Object[] objArr = ArrayList.this.elementData;
                    if (i2 + i4 >= objArr.length) {
                        throw new ConcurrentModificationException();
                    }
                    while (i4 != i3 && SubList.this.modCount == this.expectedModCount) {
                        consumer.accept(objArr[i2 + i4]);
                        i4++;
                    }
                    this.cursor = i4;
                    this.lastRet = i4;
                    if (this.expectedModCount != ArrayList.this.modCount) {
                        throw new ConcurrentModificationException();
                    }
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
                public void remove() {
                    if (this.lastRet < 0) {
                        throw new IllegalStateException();
                    }
                    if (this.expectedModCount != ArrayList.this.modCount) {
                        throw new ConcurrentModificationException();
                    }
                    try {
                        SubList.this.remove(this.lastRet);
                        this.cursor = this.lastRet;
                        this.lastRet = -1;
                        this.expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException e) {
                        throw new ConcurrentModificationException();
                    }
                }

                @Override
                public void set(E e) {
                    if (this.lastRet < 0) {
                        throw new IllegalStateException();
                    }
                    if (this.expectedModCount != ArrayList.this.modCount) {
                        throw new ConcurrentModificationException();
                    }
                    try {
                        ArrayList.this.set(i2 + this.lastRet, e);
                    } catch (IndexOutOfBoundsException e2) {
                        throw new ConcurrentModificationException();
                    }
                }

                @Override
                public void add(E e) {
                    if (this.expectedModCount != ArrayList.this.modCount) {
                        throw new ConcurrentModificationException();
                    }
                    try {
                        int i3 = this.cursor;
                        SubList.this.add(i3, e);
                        this.cursor = i3 + 1;
                        this.lastRet = -1;
                        this.expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException e2) {
                        throw new ConcurrentModificationException();
                    }
                }
            };
        }

        @Override
        public List<E> subList(int i, int i2) {
            ArrayList.subListRangeCheck(i, i2, this.size);
            return new SubList(this, this.offset, i, i2);
        }

        private String outOfBoundsMsg(int i) {
            return "Index: " + i + ", Size: " + this.size;
        }

        @Override
        public Spliterator<E> spliterator() {
            if (this.modCount != ArrayList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            return new ArrayListSpliterator(ArrayList.this, this.offset, this.offset + this.size, this.modCount);
        }
    }

    @Override
    public void forEach(Consumer<? super E> consumer) {
        Objects.requireNonNull(consumer);
        int i = this.modCount;
        Object[] objArr = this.elementData;
        int i2 = this.size;
        for (int i3 = 0; this.modCount == i && i3 < i2; i3++) {
            consumer.accept(objArr[i3]);
        }
        if (this.modCount != i) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new ArrayListSpliterator(this, 0, -1, 0);
    }

    static final class ArrayListSpliterator<E> implements Spliterator<E> {
        private int expectedModCount;
        private int fence;
        private int index;
        private final ArrayList<E> list;

        ArrayListSpliterator(ArrayList<E> arrayList, int i, int i2, int i3) {
            this.list = arrayList;
            this.index = i;
            this.fence = i2;
            this.expectedModCount = i3;
        }

        private int getFence() {
            int i = this.fence;
            if (i < 0) {
                ArrayList<E> arrayList = this.list;
                if (arrayList == null) {
                    this.fence = 0;
                    return 0;
                }
                this.expectedModCount = arrayList.modCount;
                int i2 = ((ArrayList) arrayList).size;
                this.fence = i2;
                return i2;
            }
            return i;
        }

        @Override
        public ArrayListSpliterator<E> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = (fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            ArrayList<E> arrayList = this.list;
            this.index = i2;
            return new ArrayListSpliterator<>(arrayList, i, i2, this.expectedModCount);
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            int fence = getFence();
            int i = this.index;
            if (i < fence) {
                this.index = i + 1;
                consumer.accept(this.list.elementData[i]);
                if (this.list.modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Object[] objArr;
            int i;
            if (consumer == null) {
                throw new NullPointerException();
            }
            ArrayList<E> arrayList = this.list;
            if (arrayList != null && (objArr = arrayList.elementData) != null) {
                int i2 = this.fence;
                if (i2 < 0) {
                    i = arrayList.modCount;
                    i2 = ((ArrayList) arrayList).size;
                } else {
                    i = this.expectedModCount;
                }
                int i3 = this.index;
                if (i3 >= 0) {
                    this.index = i2;
                    if (i2 <= objArr.length) {
                        while (i3 < i2) {
                            consumer.accept(objArr[i3]);
                            i3++;
                        }
                        if (arrayList.modCount == i) {
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

    @Override
    public boolean removeIf(Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate);
        BitSet bitSet = new BitSet(this.size);
        int i = this.modCount;
        int i2 = this.size;
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 0; this.modCount == i && i5 < i2; i5++) {
            if (predicate.test(this.elementData[i5])) {
                bitSet.set(i5);
                i4++;
            }
        }
        if (this.modCount != i) {
            throw new ConcurrentModificationException();
        }
        boolean z = i4 > 0;
        if (z) {
            int i6 = i2 - i4;
            for (int i7 = 0; i3 < i2 && i7 < i6; i7++) {
                int iNextClearBit = bitSet.nextClearBit(i3);
                this.elementData[i7] = this.elementData[iNextClearBit];
                i3 = iNextClearBit + 1;
            }
            for (int i8 = i6; i8 < i2; i8++) {
                this.elementData[i8] = null;
            }
            this.size = i6;
            if (this.modCount != i) {
                throw new ConcurrentModificationException();
            }
            this.modCount++;
        }
        return z;
    }

    @Override
    public void replaceAll(UnaryOperator<E> unaryOperator) {
        Objects.requireNonNull(unaryOperator);
        int i = this.modCount;
        int i2 = this.size;
        for (int i3 = 0; this.modCount == i && i3 < i2; i3++) {
            this.elementData[i3] = unaryOperator.apply(this.elementData[i3]);
        }
        if (this.modCount != i) {
            throw new ConcurrentModificationException();
        }
        this.modCount++;
    }

    @Override
    public void sort(Comparator<? super E> comparator) {
        int i = this.modCount;
        Arrays.sort(this.elementData, 0, this.size, comparator);
        if (this.modCount != i) {
            throw new ConcurrentModificationException();
        }
        this.modCount++;
    }
}
