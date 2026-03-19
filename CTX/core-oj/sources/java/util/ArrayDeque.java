package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Consumer;

public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E>, Cloneable, Serializable {
    static final boolean $assertionsDisabled = false;
    private static final int MIN_INITIAL_CAPACITY = 8;
    private static final long serialVersionUID = 2340985798034038923L;
    transient Object[] elements;
    transient int head;
    transient int tail;

    private void allocateElements(int i) {
        int i2 = 8;
        if (i >= 8) {
            int i3 = i | (i >>> 1);
            int i4 = i3 | (i3 >>> 2);
            int i5 = i4 | (i4 >>> 4);
            int i6 = i5 | (i5 >>> 8);
            i2 = (i6 | (i6 >>> 16)) + 1;
            if (i2 < 0) {
                i2 >>>= 1;
            }
        }
        this.elements = new Object[i2];
    }

    private void doubleCapacity() {
        int i = this.head;
        int length = this.elements.length;
        int i2 = length - i;
        int i3 = length << 1;
        if (i3 < 0) {
            throw new IllegalStateException("Sorry, deque too big");
        }
        Object[] objArr = new Object[i3];
        System.arraycopy(this.elements, i, objArr, 0, i2);
        System.arraycopy(this.elements, 0, objArr, i2, i);
        this.elements = objArr;
        this.head = 0;
        this.tail = length;
    }

    public ArrayDeque() {
        this.elements = new Object[16];
    }

    public ArrayDeque(int i) {
        allocateElements(i);
    }

    public ArrayDeque(Collection<? extends E> collection) {
        allocateElements(collection.size());
        addAll(collection);
    }

    @Override
    public void addFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        Object[] objArr = this.elements;
        int length = (this.head - 1) & (this.elements.length - 1);
        this.head = length;
        objArr[length] = e;
        if (this.head == this.tail) {
            doubleCapacity();
        }
    }

    @Override
    public void addLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        this.elements[this.tail] = e;
        int length = (this.tail + 1) & (this.elements.length - 1);
        this.tail = length;
        if (length == this.head) {
            doubleCapacity();
        }
    }

    @Override
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    @Override
    public E removeFirst() {
        E ePollFirst = pollFirst();
        if (ePollFirst == null) {
            throw new NoSuchElementException();
        }
        return ePollFirst;
    }

    @Override
    public E removeLast() {
        E ePollLast = pollLast();
        if (ePollLast == null) {
            throw new NoSuchElementException();
        }
        return ePollLast;
    }

    @Override
    public E pollFirst() {
        Object[] objArr = this.elements;
        int i = this.head;
        E e = (E) objArr[i];
        if (e != null) {
            objArr[i] = null;
            this.head = (objArr.length - 1) & (i + 1);
        }
        return e;
    }

    @Override
    public E pollLast() {
        Object[] objArr = this.elements;
        int length = (this.tail - 1) & (objArr.length - 1);
        E e = (E) objArr[length];
        if (e != null) {
            objArr[length] = null;
            this.tail = length;
        }
        return e;
    }

    @Override
    public E getFirst() {
        E e = (E) this.elements[this.head];
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    @Override
    public E getLast() {
        E e = (E) this.elements[(this.tail - 1) & (this.elements.length - 1)];
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    @Override
    public E peekFirst() {
        return (E) this.elements[this.head];
    }

    @Override
    public E peekLast() {
        return (E) this.elements[(this.tail - 1) & (this.elements.length - 1)];
    }

    @Override
    public boolean removeFirstOccurrence(Object obj) {
        if (obj != null) {
            int length = this.elements.length - 1;
            int i = this.head;
            while (true) {
                Object obj2 = this.elements[i];
                if (obj2 != null) {
                    if (!obj.equals(obj2)) {
                        i = (i + 1) & length;
                    } else {
                        delete(i);
                        return true;
                    }
                } else {
                    return $assertionsDisabled;
                }
            }
        } else {
            return $assertionsDisabled;
        }
    }

    @Override
    public boolean removeLastOccurrence(Object obj) {
        if (obj != null) {
            int length = this.elements.length - 1;
            int i = this.tail - 1;
            while (true) {
                int i2 = i & length;
                Object obj2 = this.elements[i2];
                if (obj2 != null) {
                    if (!obj.equals(obj2)) {
                        i = i2 - 1;
                    } else {
                        delete(i2);
                        return true;
                    }
                } else {
                    return $assertionsDisabled;
                }
            }
        } else {
            return $assertionsDisabled;
        }
    }

    @Override
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public E poll() {
        return pollFirst();
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public E peek() {
        return peekFirst();
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    private void checkInvariants() {
    }

    boolean delete(int i) {
        checkInvariants();
        Object[] objArr = this.elements;
        int length = objArr.length - 1;
        int i2 = this.head;
        int i3 = this.tail;
        int i4 = (i - i2) & length;
        int i5 = (i3 - i) & length;
        if (i4 >= ((i3 - i2) & length)) {
            throw new ConcurrentModificationException();
        }
        if (i4 < i5) {
            if (i2 <= i) {
                System.arraycopy(objArr, i2, objArr, i2 + 1, i4);
            } else {
                System.arraycopy(objArr, 0, objArr, 1, i);
                objArr[0] = objArr[length];
                System.arraycopy(objArr, i2, objArr, i2 + 1, length - i2);
            }
            objArr[i2] = null;
            this.head = (i2 + 1) & length;
            return $assertionsDisabled;
        }
        if (i < i3) {
            System.arraycopy(objArr, i + 1, objArr, i, i5);
            this.tail = i3 - 1;
        } else {
            System.arraycopy(objArr, i + 1, objArr, i, length - i);
            objArr[length] = objArr[0];
            System.arraycopy(objArr, 1, objArr, 0, i3);
            this.tail = (i3 - 1) & length;
        }
        return true;
    }

    @Override
    public int size() {
        return (this.tail - this.head) & (this.elements.length - 1);
    }

    @Override
    public boolean isEmpty() {
        if (this.head == this.tail) {
            return true;
        }
        return $assertionsDisabled;
    }

    @Override
    public Iterator<E> iterator() {
        return new DeqIterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    private class DeqIterator implements Iterator<E> {
        private int cursor;
        private int fence;
        private int lastRet;

        private DeqIterator() {
            this.cursor = ArrayDeque.this.head;
            this.fence = ArrayDeque.this.tail;
            this.lastRet = -1;
        }

        @Override
        public boolean hasNext() {
            if (this.cursor != this.fence) {
                return true;
            }
            return ArrayDeque.$assertionsDisabled;
        }

        @Override
        public E next() {
            if (this.cursor == this.fence) {
                throw new NoSuchElementException();
            }
            E e = (E) ArrayDeque.this.elements[this.cursor];
            if (ArrayDeque.this.tail != this.fence || e == null) {
                throw new ConcurrentModificationException();
            }
            this.lastRet = this.cursor;
            this.cursor = (this.cursor + 1) & (ArrayDeque.this.elements.length - 1);
            return e;
        }

        @Override
        public void remove() {
            if (this.lastRet < 0) {
                throw new IllegalStateException();
            }
            if (ArrayDeque.this.delete(this.lastRet)) {
                this.cursor = (this.cursor - 1) & (ArrayDeque.this.elements.length - 1);
                this.fence = ArrayDeque.this.tail;
            }
            this.lastRet = -1;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            Object[] objArr = ArrayDeque.this.elements;
            int length = objArr.length - 1;
            int i = this.fence;
            int i2 = this.cursor;
            this.cursor = i;
            while (i2 != i) {
                Object obj = objArr[i2];
                i2 = (i2 + 1) & length;
                if (obj == null) {
                    throw new ConcurrentModificationException();
                }
                consumer.accept(obj);
            }
        }
    }

    private class DescendingIterator implements Iterator<E> {
        private int cursor;
        private int fence;
        private int lastRet;

        private DescendingIterator() {
            this.cursor = ArrayDeque.this.tail;
            this.fence = ArrayDeque.this.head;
            this.lastRet = -1;
        }

        @Override
        public boolean hasNext() {
            if (this.cursor != this.fence) {
                return true;
            }
            return ArrayDeque.$assertionsDisabled;
        }

        @Override
        public E next() {
            if (this.cursor == this.fence) {
                throw new NoSuchElementException();
            }
            this.cursor = (this.cursor - 1) & (ArrayDeque.this.elements.length - 1);
            E e = (E) ArrayDeque.this.elements[this.cursor];
            if (ArrayDeque.this.head != this.fence || e == null) {
                throw new ConcurrentModificationException();
            }
            this.lastRet = this.cursor;
            return e;
        }

        @Override
        public void remove() {
            if (this.lastRet < 0) {
                throw new IllegalStateException();
            }
            if (!ArrayDeque.this.delete(this.lastRet)) {
                this.cursor = (this.cursor + 1) & (ArrayDeque.this.elements.length - 1);
                this.fence = ArrayDeque.this.head;
            }
            this.lastRet = -1;
        }
    }

    @Override
    public boolean contains(Object obj) {
        if (obj != null) {
            int length = this.elements.length - 1;
            int i = this.head;
            while (true) {
                Object obj2 = this.elements[i];
                if (obj2 != null) {
                    if (obj.equals(obj2)) {
                        return true;
                    }
                    i = (i + 1) & length;
                } else {
                    return $assertionsDisabled;
                }
            }
        } else {
            return $assertionsDisabled;
        }
    }

    @Override
    public boolean remove(Object obj) {
        return removeFirstOccurrence(obj);
    }

    @Override
    public void clear() {
        int i = this.head;
        int i2 = this.tail;
        if (i != i2) {
            this.tail = 0;
            this.head = 0;
            int length = this.elements.length - 1;
            do {
                this.elements[i] = null;
                i = (i + 1) & length;
            } while (i != i2);
        }
    }

    @Override
    public Object[] toArray() {
        int i = this.head;
        int i2 = this.tail;
        boolean z = i2 < i;
        Object[] objArrCopyOfRange = Arrays.copyOfRange(this.elements, i, z ? this.elements.length + i2 : i2);
        if (z) {
            System.arraycopy(this.elements, 0, objArrCopyOfRange, this.elements.length - i, i2);
        }
        return objArrCopyOfRange;
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        int i = this.head;
        int i2 = this.tail;
        boolean z = i2 < i;
        int length = (i2 - i) + (z ? this.elements.length : 0);
        int i3 = length - (z ? i2 : 0);
        int length2 = tArr.length;
        if (length <= length2) {
            System.arraycopy(this.elements, i, tArr, 0, i3);
            if (length < length2) {
                tArr[length] = null;
            }
        } else {
            tArr = (T[]) Arrays.copyOfRange(this.elements, i, length + i, tArr.getClass());
        }
        if (z) {
            System.arraycopy(this.elements, 0, tArr, i3, i2);
        }
        return tArr;
    }

    public ArrayDeque<E> clone() {
        try {
            ArrayDeque<E> arrayDeque = (ArrayDeque) super.clone();
            arrayDeque.elements = Arrays.copyOf(this.elements, this.elements.length);
            return arrayDeque;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(size());
        int length = this.elements.length - 1;
        for (int i = this.head; i != this.tail; i = (i + 1) & length) {
            objectOutputStream.writeObject(this.elements[i]);
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        int i = objectInputStream.readInt();
        allocateElements(i);
        this.head = 0;
        this.tail = i;
        for (int i2 = 0; i2 < i; i2++) {
            this.elements[i2] = objectInputStream.readObject();
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new DeqSpliterator(this, -1, -1);
    }

    static final class DeqSpliterator<E> implements Spliterator<E> {
        private final ArrayDeque<E> deq;
        private int fence;
        private int index;

        DeqSpliterator(ArrayDeque<E> arrayDeque, int i, int i2) {
            this.deq = arrayDeque;
            this.index = i;
            this.fence = i2;
        }

        private int getFence() {
            int i = this.fence;
            if (i < 0) {
                int i2 = this.deq.tail;
                this.fence = i2;
                this.index = this.deq.head;
                return i2;
            }
            return i;
        }

        @Override
        public DeqSpliterator<E> trySplit() {
            int fence = getFence();
            int i = this.index;
            int length = this.deq.elements.length;
            if (i == fence) {
                return null;
            }
            int i2 = length - 1;
            if (((i + 1) & i2) != fence) {
                if (i > fence) {
                    fence += length;
                }
                int i3 = ((fence + i) >>> 1) & i2;
                ArrayDeque<E> arrayDeque = this.deq;
                this.index = i3;
                return new DeqSpliterator<>(arrayDeque, i, i3);
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Object[] objArr = this.deq.elements;
            int length = objArr.length - 1;
            int fence = getFence();
            int i = this.index;
            this.index = fence;
            while (i != fence) {
                Object obj = objArr[i];
                i = (i + 1) & length;
                if (obj == null) {
                    throw new ConcurrentModificationException();
                }
                consumer.accept(obj);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            Object[] objArr = this.deq.elements;
            int length = objArr.length - 1;
            int fence = getFence();
            int i = this.index;
            if (i != fence) {
                Object obj = objArr[i];
                this.index = length & (i + 1);
                if (obj == null) {
                    throw new ConcurrentModificationException();
                }
                consumer.accept(obj);
                return true;
            }
            return ArrayDeque.$assertionsDisabled;
        }

        @Override
        public long estimateSize() {
            int fence = getFence() - this.index;
            if (fence < 0) {
                fence += this.deq.elements.length;
            }
            return fence;
        }

        @Override
        public int characteristics() {
            return 16720;
        }
    }
}
