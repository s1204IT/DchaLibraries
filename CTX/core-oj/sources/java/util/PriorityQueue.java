package java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Consumer;

public class PriorityQueue<E> extends AbstractQueue<E> implements Serializable {
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
    private static final int MAX_ARRAY_SIZE = 2147483639;
    private static final long serialVersionUID = -7720805057305804111L;
    private final Comparator<? super E> comparator;
    transient int modCount;
    transient Object[] queue;
    int size;

    public PriorityQueue() {
        this(11, null);
    }

    public PriorityQueue(int i) {
        this(i, null);
    }

    public PriorityQueue(Comparator<? super E> comparator) {
        this(11, comparator);
    }

    public PriorityQueue(int i, Comparator<? super E> comparator) {
        if (i < 1) {
            throw new IllegalArgumentException();
        }
        this.queue = new Object[i];
        this.comparator = comparator;
    }

    public PriorityQueue(Collection<? extends E> collection) {
        if (collection instanceof SortedSet) {
            SortedSet sortedSet = (SortedSet) collection;
            this.comparator = sortedSet.comparator();
            initElementsFromCollection(sortedSet);
        } else if (collection instanceof PriorityQueue) {
            PriorityQueue<? extends E> priorityQueue = (PriorityQueue) collection;
            this.comparator = priorityQueue.comparator();
            initFromPriorityQueue(priorityQueue);
        } else {
            this.comparator = null;
            initFromCollection(collection);
        }
    }

    public PriorityQueue(PriorityQueue<? extends E> priorityQueue) {
        this.comparator = priorityQueue.comparator();
        initFromPriorityQueue(priorityQueue);
    }

    public PriorityQueue(SortedSet<? extends E> sortedSet) {
        this.comparator = sortedSet.comparator();
        initElementsFromCollection(sortedSet);
    }

    private void initFromPriorityQueue(PriorityQueue<? extends E> priorityQueue) {
        if (priorityQueue.getClass() == PriorityQueue.class) {
            this.queue = priorityQueue.toArray();
            this.size = priorityQueue.size();
        } else {
            initFromCollection(priorityQueue);
        }
    }

    private void initElementsFromCollection(Collection<? extends E> collection) {
        Object[] array = collection.toArray();
        if (array.getClass() != Object[].class) {
            array = Arrays.copyOf(array, array.length, Object[].class);
        }
        if (array.length == 1 || this.comparator != null) {
            for (Object obj : array) {
                if (obj == null) {
                    throw new NullPointerException();
                }
            }
        }
        this.queue = array;
        this.size = array.length;
    }

    private void initFromCollection(Collection<? extends E> collection) {
        initElementsFromCollection(collection);
        heapify();
    }

    private void grow(int i) {
        int i2;
        int length = this.queue.length;
        if (length < 64) {
            i2 = length + 2;
        } else {
            i2 = length >> 1;
        }
        int iHugeCapacity = length + i2;
        if (iHugeCapacity - MAX_ARRAY_SIZE > 0) {
            iHugeCapacity = hugeCapacity(i);
        }
        this.queue = Arrays.copyOf(this.queue, iHugeCapacity);
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
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        this.modCount++;
        int i = this.size;
        if (i >= this.queue.length) {
            grow(i + 1);
        }
        this.size = i + 1;
        if (i == 0) {
            this.queue[0] = e;
        } else {
            siftUp(i, e);
        }
        return true;
    }

    @Override
    public E peek() {
        if (this.size == 0) {
            return null;
        }
        return (E) this.queue[0];
    }

    private int indexOf(Object obj) {
        if (obj != null) {
            for (int i = 0; i < this.size; i++) {
                if (obj.equals(this.queue[i])) {
                    return i;
                }
            }
            return -1;
        }
        return -1;
    }

    @Override
    public boolean remove(Object obj) {
        int iIndexOf = indexOf(obj);
        if (iIndexOf == -1) {
            return false;
        }
        removeAt(iIndexOf);
        return true;
    }

    boolean removeEq(Object obj) {
        for (int i = 0; i < this.size; i++) {
            if (obj == this.queue[i]) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(Object obj) {
        return indexOf(obj) >= 0;
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(this.queue, this.size);
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        int i = this.size;
        if (tArr.length < i) {
            return (T[]) Arrays.copyOf(this.queue, i, tArr.getClass());
        }
        System.arraycopy(this.queue, 0, tArr, 0, i);
        if (tArr.length > i) {
            tArr[i] = null;
        }
        return tArr;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private final class Itr implements Iterator<E> {
        private int cursor;
        private int expectedModCount;
        private ArrayDeque<E> forgetMeNot;
        private int lastRet;
        private E lastRetElt;

        private Itr() {
            this.lastRet = -1;
            this.expectedModCount = PriorityQueue.this.modCount;
        }

        @Override
        public boolean hasNext() {
            return this.cursor < PriorityQueue.this.size || !(this.forgetMeNot == null || this.forgetMeNot.isEmpty());
        }

        @Override
        public E next() {
            if (this.expectedModCount != PriorityQueue.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (this.cursor < PriorityQueue.this.size) {
                Object[] objArr = PriorityQueue.this.queue;
                int i = this.cursor;
                this.cursor = i + 1;
                this.lastRet = i;
                return (E) objArr[i];
            }
            if (this.forgetMeNot != null) {
                this.lastRet = -1;
                this.lastRetElt = this.forgetMeNot.poll();
                if (this.lastRetElt != null) {
                    return this.lastRetElt;
                }
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            if (this.expectedModCount != PriorityQueue.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (this.lastRet != -1) {
                Object objRemoveAt = PriorityQueue.this.removeAt(this.lastRet);
                this.lastRet = -1;
                if (objRemoveAt == null) {
                    this.cursor--;
                } else {
                    if (this.forgetMeNot == null) {
                        this.forgetMeNot = new ArrayDeque<>();
                    }
                    this.forgetMeNot.add((E) objRemoveAt);
                }
            } else if (this.lastRetElt != null) {
                PriorityQueue.this.removeEq(this.lastRetElt);
                this.lastRetElt = null;
            } else {
                throw new IllegalStateException();
            }
            this.expectedModCount = PriorityQueue.this.modCount;
        }
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void clear() {
        this.modCount++;
        for (int i = 0; i < this.size; i++) {
            this.queue[i] = null;
        }
        this.size = 0;
    }

    @Override
    public E poll() {
        if (this.size == 0) {
            return null;
        }
        int i = this.size - 1;
        this.size = i;
        this.modCount++;
        E e = (E) this.queue[0];
        Object obj = this.queue[i];
        this.queue[i] = null;
        if (i != 0) {
            siftDown(0, obj);
        }
        return e;
    }

    E removeAt(int i) {
        this.modCount++;
        int i2 = this.size - 1;
        this.size = i2;
        if (i2 == i) {
            this.queue[i] = null;
        } else {
            E e = (E) this.queue[i2];
            this.queue[i2] = null;
            siftDown(i, e);
            if (this.queue[i] == e) {
                siftUp(i, e);
                if (this.queue[i] != e) {
                    return e;
                }
            }
        }
        return null;
    }

    private void siftUp(int i, E e) {
        if (this.comparator != null) {
            siftUpUsingComparator(i, e);
        } else {
            siftUpComparable(i, e);
        }
    }

    private void siftUpComparable(int i, E e) {
        Comparable comparable = (Comparable) e;
        while (i > 0) {
            int i2 = (i - 1) >>> 1;
            Object obj = this.queue[i2];
            if (comparable.compareTo(obj) >= 0) {
                break;
            }
            this.queue[i] = obj;
            i = i2;
        }
        this.queue[i] = comparable;
    }

    private void siftUpUsingComparator(int i, E e) {
        while (i > 0) {
            int i2 = (i - 1) >>> 1;
            Object obj = this.queue[i2];
            if (this.comparator.compare(e, obj) >= 0) {
                break;
            }
            this.queue[i] = obj;
            i = i2;
        }
        this.queue[i] = e;
    }

    private void siftDown(int i, E e) {
        if (this.comparator != null) {
            siftDownUsingComparator(i, e);
        } else {
            siftDownComparable(i, e);
        }
    }

    private void siftDownComparable(int i, E e) {
        Comparable comparable = (Comparable) e;
        int i2 = this.size >>> 1;
        while (i < i2) {
            int i3 = (i << 1) + 1;
            Object obj = this.queue[i3];
            int i4 = i3 + 1;
            if (i4 < this.size && ((Comparable) obj).compareTo(this.queue[i4]) > 0) {
                obj = this.queue[i4];
                i3 = i4;
            }
            if (comparable.compareTo(obj) <= 0) {
                break;
            }
            this.queue[i] = obj;
            i = i3;
        }
        this.queue[i] = comparable;
    }

    private void siftDownUsingComparator(int i, E e) {
        int i2 = this.size >>> 1;
        while (i < i2) {
            int i3 = (i << 1) + 1;
            Object obj = this.queue[i3];
            int i4 = i3 + 1;
            if (i4 < this.size && this.comparator.compare(obj, this.queue[i4]) > 0) {
                obj = this.queue[i4];
                i3 = i4;
            }
            if (this.comparator.compare(e, obj) <= 0) {
                break;
            }
            this.queue[i] = obj;
            i = i3;
        }
        this.queue[i] = e;
    }

    private void heapify() {
        for (int i = (this.size >>> 1) - 1; i >= 0; i--) {
            siftDown(i, this.queue[i]);
        }
    }

    public Comparator<? super E> comparator() {
        return this.comparator;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(Math.max(2, this.size + 1));
        for (int i = 0; i < this.size; i++) {
            objectOutputStream.writeObject(this.queue[i]);
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        objectInputStream.readInt();
        this.queue = new Object[this.size];
        for (int i = 0; i < this.size; i++) {
            this.queue[i] = objectInputStream.readObject();
        }
        heapify();
    }

    @Override
    public final Spliterator<E> spliterator() {
        return new PriorityQueueSpliterator(this, 0, -1, 0);
    }

    static final class PriorityQueueSpliterator<E> implements Spliterator<E> {
        private int expectedModCount;
        private int fence;
        private int index;
        private final PriorityQueue<E> pq;

        PriorityQueueSpliterator(PriorityQueue<E> priorityQueue, int i, int i2, int i3) {
            this.pq = priorityQueue;
            this.index = i;
            this.fence = i2;
            this.expectedModCount = i3;
        }

        private int getFence() {
            int i = this.fence;
            if (i < 0) {
                this.expectedModCount = this.pq.modCount;
                int i2 = this.pq.size;
                this.fence = i2;
                return i2;
            }
            return i;
        }

        @Override
        public PriorityQueueSpliterator<E> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = (fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            PriorityQueue<E> priorityQueue = this.pq;
            this.index = i2;
            return new PriorityQueueSpliterator<>(priorityQueue, i, i2, this.expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            Object[] objArr;
            int i;
            if (consumer == null) {
                throw new NullPointerException();
            }
            PriorityQueue<E> priorityQueue = this.pq;
            if (priorityQueue != null && (objArr = priorityQueue.queue) != null) {
                int i2 = this.fence;
                if (i2 < 0) {
                    i = priorityQueue.modCount;
                    i2 = priorityQueue.size;
                } else {
                    i = this.expectedModCount;
                }
                int i3 = this.index;
                if (i3 >= 0) {
                    this.index = i2;
                    if (i2 <= objArr.length) {
                        while (true) {
                            if (i3 < i2) {
                                Object obj = objArr[i3];
                                if (obj == null) {
                                    break;
                                }
                                consumer.accept(obj);
                                i3++;
                            } else if (priorityQueue.modCount == i) {
                                return;
                            }
                        }
                    }
                }
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            int fence = getFence();
            int i = this.index;
            if (i >= 0 && i < fence) {
                this.index = i + 1;
                Object obj = this.pq.queue[i];
                if (obj == null) {
                    throw new ConcurrentModificationException();
                }
                consumer.accept(obj);
                if (this.pq.modCount != this.expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                return true;
            }
            return false;
        }

        @Override
        public long estimateSize() {
            return getFence() - this.index;
        }

        @Override
        public int characteristics() {
            return 16704;
        }
    }
}
