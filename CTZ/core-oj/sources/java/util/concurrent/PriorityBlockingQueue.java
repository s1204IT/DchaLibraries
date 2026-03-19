package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import sun.misc.Unsafe;

public class PriorityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
    private static final long ALLOCATIONSPINLOCK;
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
    private static final int MAX_ARRAY_SIZE = 2147483639;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = 5595510919245408276L;
    private volatile transient int allocationSpinLock;
    private transient Comparator<? super E> comparator;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private PriorityQueue<E> q;
    private transient Object[] queue;
    private transient int size;

    public PriorityBlockingQueue() {
        this(11, null);
    }

    public PriorityBlockingQueue(int i) {
        this(i, null);
    }

    public PriorityBlockingQueue(int i, Comparator<? super E> comparator) {
        if (i < 1) {
            throw new IllegalArgumentException();
        }
        this.lock = new ReentrantLock();
        this.notEmpty = this.lock.newCondition();
        this.comparator = comparator;
        this.queue = new Object[i];
    }

    public PriorityBlockingQueue(Collection<? extends E> collection) {
        boolean z;
        boolean z2;
        this.lock = new ReentrantLock();
        this.notEmpty = this.lock.newCondition();
        if (collection instanceof SortedSet) {
            this.comparator = ((SortedSet) collection).comparator();
            z2 = false;
            z = true;
        } else {
            if (collection instanceof PriorityBlockingQueue) {
                PriorityBlockingQueue priorityBlockingQueue = (PriorityBlockingQueue) collection;
                this.comparator = priorityBlockingQueue.comparator();
                if (priorityBlockingQueue.getClass() == PriorityBlockingQueue.class) {
                    z = false;
                } else {
                    z = false;
                    z2 = true;
                }
            } else {
                z = true;
            }
            z2 = z;
        }
        Object[] array = collection.toArray();
        int length = array.length;
        array = array.getClass() != Object[].class ? Arrays.copyOf(array, length, Object[].class) : array;
        if (z && (length == 1 || this.comparator != null)) {
            for (int i = 0; i < length; i++) {
                if (array[i] == null) {
                    throw new NullPointerException();
                }
            }
        }
        this.queue = array;
        this.size = length;
        if (z2) {
            heapify();
        }
    }

    private void tryGrow(Object[] objArr, int i) {
        int i2;
        this.lock.unlock();
        Object[] objArr2 = null;
        if (this.allocationSpinLock == 0 && U.compareAndSwapInt(this, ALLOCATIONSPINLOCK, 0, 1)) {
            if (i < 64) {
                i2 = i + 2;
            } else {
                i2 = i >> 1;
            }
            int i3 = i2 + i;
            try {
                if (i3 - MAX_ARRAY_SIZE > 0) {
                    int i4 = i + 1;
                    if (i4 < 0 || i4 > MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError();
                    }
                    i3 = MAX_ARRAY_SIZE;
                }
                if (i3 > i && this.queue == objArr) {
                    objArr2 = new Object[i3];
                }
                this.allocationSpinLock = 0;
            } catch (Throwable th) {
                this.allocationSpinLock = 0;
                throw th;
            }
        }
        if (objArr2 == null) {
            Thread.yield();
        }
        this.lock.lock();
        if (objArr2 != null && this.queue == objArr) {
            this.queue = objArr2;
            System.arraycopy(objArr, 0, objArr2, 0, i);
        }
    }

    private E dequeue() {
        int i = this.size - 1;
        if (i < 0) {
            return null;
        }
        Object[] objArr = this.queue;
        E e = (E) objArr[0];
        Object obj = objArr[i];
        objArr[i] = null;
        Comparator<? super E> comparator = this.comparator;
        if (comparator == null) {
            siftDownComparable(0, obj, objArr, i);
        } else {
            siftDownUsingComparator(0, obj, objArr, i, comparator);
        }
        this.size = i;
        return e;
    }

    private static <T> void siftUpComparable(int i, T t, Object[] objArr) {
        Comparable comparable = (Comparable) t;
        while (i > 0) {
            int i2 = (i - 1) >>> 1;
            Object[] objArr2 = objArr[i2];
            if (comparable.compareTo(objArr2) >= 0) {
                break;
            }
            objArr[i] = objArr2;
            i = i2;
        }
        objArr[i] = comparable;
    }

    private static <T> void siftUpUsingComparator(int i, T t, Object[] objArr, Comparator<? super T> comparator) {
        while (i > 0) {
            int i2 = (i - 1) >>> 1;
            Object obj = objArr[i2];
            if (comparator.compare(t, obj) >= 0) {
                break;
            }
            objArr[i] = obj;
            i = i2;
        }
        objArr[i] = t;
    }

    private static <T> void siftDownComparable(int i, T t, Object[] objArr, int i2) {
        if (i2 > 0) {
            Comparable comparable = (Comparable) t;
            int i3 = i2 >>> 1;
            while (i < i3) {
                int i4 = (i << 1) + 1;
                Object obj = objArr[i4];
                int i5 = i4 + 1;
                Object obj2 = obj;
                if (i5 < i2) {
                    int iCompareTo = ((Comparable) obj).compareTo(objArr[i5]);
                    obj2 = obj;
                    if (iCompareTo > 0) {
                        i4 = i5;
                        obj2 = objArr[i5];
                    }
                }
                if (comparable.compareTo(obj2) <= 0) {
                    break;
                }
                objArr[i] = obj2;
                i = i4;
            }
            objArr[i] = comparable;
        }
    }

    private static <T> void siftDownUsingComparator(int i, T t, Object[] objArr, int i2, Comparator<? super T> comparator) {
        if (i2 > 0) {
            int i3 = i2 >>> 1;
            while (i < i3) {
                int i4 = (i << 1) + 1;
                Object obj = objArr[i4];
                int i5 = i4 + 1;
                if (i5 < i2 && comparator.compare(obj, objArr[i5]) > 0) {
                    obj = objArr[i5];
                    i4 = i5;
                }
                if (comparator.compare(t, obj) <= 0) {
                    break;
                }
                objArr[i] = obj;
                i = i4;
            }
            objArr[i] = t;
        }
    }

    private void heapify() {
        Object[] objArr = this.queue;
        int i = this.size;
        int i2 = (i >>> 1) - 1;
        Comparator<? super E> comparator = this.comparator;
        if (comparator == null) {
            while (i2 >= 0) {
                siftDownComparable(i2, objArr[i2], objArr, i);
                i2--;
            }
        } else {
            while (i2 >= 0) {
                siftDownUsingComparator(i2, objArr[i2], objArr, i, comparator);
                i2--;
            }
        }
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean offer(E e) {
        int i;
        Object[] objArr;
        if (e == null) {
            throw new NullPointerException();
        }
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        while (true) {
            i = this.size;
            objArr = this.queue;
            int length = objArr.length;
            if (i >= length) {
                tryGrow(objArr, length);
            } else {
                try {
                    break;
                } finally {
                    reentrantLock.unlock();
                }
            }
        }
        Comparator<? super E> comparator = this.comparator;
        if (comparator == null) {
            siftUpComparable(i, e, objArr);
        } else {
            siftUpUsingComparator(i, e, objArr, comparator);
        }
        this.size = i + 1;
        this.notEmpty.signal();
        return true;
    }

    @Override
    public void put(E e) {
        offer(e);
    }

    @Override
    public boolean offer(E e, long j, TimeUnit timeUnit) {
        return offer(e);
    }

    @Override
    public E poll() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return dequeue();
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lockInterruptibly();
        while (true) {
            try {
                E eDequeue = dequeue();
                if (eDequeue == null) {
                    this.notEmpty.await();
                } else {
                    return eDequeue;
                }
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    @Override
    public E poll(long j, TimeUnit timeUnit) throws InterruptedException {
        E eDequeue;
        long nanos = timeUnit.toNanos(j);
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lockInterruptibly();
        while (true) {
            try {
                eDequeue = dequeue();
                if (eDequeue != null || nanos <= 0) {
                    break;
                }
                nanos = this.notEmpty.awaitNanos(nanos);
            } finally {
                reentrantLock.unlock();
            }
        }
        return eDequeue;
    }

    @Override
    public E peek() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.size == 0 ? null : (E) this.queue[0];
        } finally {
            reentrantLock.unlock();
        }
    }

    public Comparator<? super E> comparator() {
        return this.comparator;
    }

    @Override
    public int size() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.size;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    private int indexOf(Object obj) {
        if (obj != null) {
            Object[] objArr = this.queue;
            int i = this.size;
            for (int i2 = 0; i2 < i; i2++) {
                if (obj.equals(objArr[i2])) {
                    return i2;
                }
            }
            return -1;
        }
        return -1;
    }

    private void removeAt(int i) {
        Object[] objArr = this.queue;
        int i2 = this.size - 1;
        if (i2 == i) {
            objArr[i] = null;
        } else {
            Object obj = objArr[i2];
            objArr[i2] = null;
            Comparator<? super E> comparator = this.comparator;
            if (comparator == null) {
                siftDownComparable(i, obj, objArr, i2);
            } else {
                siftDownUsingComparator(i, obj, objArr, i2, comparator);
            }
            if (objArr[i] == obj) {
                if (comparator == null) {
                    siftUpComparable(i, obj, objArr);
                } else {
                    siftUpUsingComparator(i, obj, objArr, comparator);
                }
            }
        }
        this.size = i2;
    }

    @Override
    public boolean remove(Object obj) {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            int iIndexOf = indexOf(obj);
            if (iIndexOf != -1) {
                removeAt(iIndexOf);
                return true;
            }
            return false;
        } finally {
            reentrantLock.unlock();
        }
    }

    void removeEQ(Object obj) {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            Object[] objArr = this.queue;
            int i = 0;
            int i2 = this.size;
            while (true) {
                if (i >= i2) {
                    break;
                } else if (obj == objArr[i]) {
                    break;
                } else {
                    i++;
                }
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public boolean contains(Object obj) {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return indexOf(obj) != -1;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public String toString() {
        return Helpers.collectionToString(this);
    }

    @Override
    public int drainTo(Collection<? super E> collection) {
        return drainTo(collection, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> collection, int i) {
        if (collection == null) {
            throw new NullPointerException();
        }
        if (collection == this) {
            throw new IllegalArgumentException();
        }
        if (i <= 0) {
            return 0;
        }
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            int iMin = Math.min(this.size, i);
            for (int i2 = 0; i2 < iMin; i2++) {
                collection.add(this.queue[0]);
                dequeue();
            }
            return iMin;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public void clear() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            Object[] objArr = this.queue;
            int i = this.size;
            this.size = 0;
            for (int i2 = 0; i2 < i; i2++) {
                objArr[i2] = null;
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return Arrays.copyOf(this.queue, this.size);
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            int i = this.size;
            if (tArr.length < i) {
                return (T[]) Arrays.copyOf(this.queue, this.size, tArr.getClass());
            }
            System.arraycopy(this.queue, 0, tArr, 0, i);
            if (tArr.length > i) {
                tArr[i] = null;
            }
            return tArr;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr(toArray());
    }

    final class Itr implements Iterator<E> {
        final Object[] array;
        int cursor;
        int lastRet = -1;

        Itr(Object[] objArr) {
            this.array = objArr;
        }

        @Override
        public boolean hasNext() {
            return this.cursor < this.array.length;
        }

        @Override
        public E next() {
            if (this.cursor >= this.array.length) {
                throw new NoSuchElementException();
            }
            this.lastRet = this.cursor;
            Object[] objArr = this.array;
            int i = this.cursor;
            this.cursor = i + 1;
            return (E) objArr[i];
        }

        @Override
        public void remove() {
            if (this.lastRet < 0) {
                throw new IllegalStateException();
            }
            PriorityBlockingQueue.this.removeEQ(this.array[this.lastRet]);
            this.lastRet = -1;
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        this.lock.lock();
        try {
            this.q = new PriorityQueue<>(Math.max(this.size, 1), this.comparator);
            this.q.addAll(this);
            objectOutputStream.defaultWriteObject();
        } finally {
            this.q = null;
            this.lock.unlock();
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        try {
            objectInputStream.defaultReadObject();
            this.queue = new Object[this.q.size()];
            this.comparator = this.q.comparator();
            addAll(this.q);
        } finally {
            this.q = null;
        }
    }

    static final class PBQSpliterator<E> implements Spliterator<E> {
        Object[] array;
        int fence;
        int index;
        final PriorityBlockingQueue<E> queue;

        PBQSpliterator(PriorityBlockingQueue<E> priorityBlockingQueue, Object[] objArr, int i, int i2) {
            this.queue = priorityBlockingQueue;
            this.array = objArr;
            this.index = i;
            this.fence = i2;
        }

        final int getFence() {
            int i = this.fence;
            if (i < 0) {
                Object[] array = this.queue.toArray();
                this.array = array;
                int length = array.length;
                this.fence = length;
                return length;
            }
            return i;
        }

        @Override
        public PBQSpliterator<E> trySplit() {
            int fence = getFence();
            int i = this.index;
            int i2 = (fence + i) >>> 1;
            if (i >= i2) {
                return null;
            }
            PriorityBlockingQueue<E> priorityBlockingQueue = this.queue;
            Object[] objArr = this.array;
            this.index = i2;
            return new PBQSpliterator<>(priorityBlockingQueue, objArr, i, i2);
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            int i;
            if (consumer == null) {
                throw new NullPointerException();
            }
            Object[] array = this.array;
            if (array == null) {
                array = this.queue.toArray();
                this.fence = array.length;
            }
            int i2 = this.fence;
            if (i2 > array.length || (i = this.index) < 0) {
                return;
            }
            this.index = i2;
            if (i < i2) {
                do {
                    consumer.accept(array[i]);
                    i++;
                } while (i < i2);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (getFence() > this.index && this.index >= 0) {
                Object[] objArr = this.array;
                int i = this.index;
                this.index = i + 1;
                consumer.accept(objArr[i]);
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

    @Override
    public Spliterator<E> spliterator() {
        return new PBQSpliterator(this, null, 0, -1);
    }

    static {
        try {
            ALLOCATIONSPINLOCK = U.objectFieldOffset(PriorityBlockingQueue.class.getDeclaredField("allocationSpinLock"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
