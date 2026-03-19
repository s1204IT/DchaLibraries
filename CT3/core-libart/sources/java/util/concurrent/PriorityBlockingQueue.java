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

    public PriorityBlockingQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    public PriorityBlockingQueue(int initialCapacity, Comparator<? super E> comparator) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException();
        }
        this.lock = new ReentrantLock();
        this.notEmpty = this.lock.newCondition();
        this.comparator = comparator;
        this.queue = new Object[initialCapacity];
    }

    public PriorityBlockingQueue(Collection<? extends E> c) {
        this.lock = new ReentrantLock();
        this.notEmpty = this.lock.newCondition();
        boolean heapify = true;
        boolean screen = true;
        if (c instanceof SortedSet) {
            SortedSet<? extends E> ss = (SortedSet) c;
            this.comparator = ss.comparator();
            heapify = false;
        } else if (c instanceof PriorityBlockingQueue) {
            PriorityBlockingQueue<? extends E> pq = (PriorityBlockingQueue) c;
            this.comparator = pq.comparator();
            screen = false;
            if (pq.getClass() == PriorityBlockingQueue.class) {
                heapify = false;
            }
        }
        Object[] a = c.toArray();
        int n = a.length;
        a = a.getClass() != Object[].class ? Arrays.copyOf(a, n, Object[].class) : a;
        if (screen && (n == 1 || this.comparator != null)) {
            for (int i = 0; i < n; i++) {
                if (a[i] == null) {
                    throw new NullPointerException();
                }
            }
        }
        this.queue = a;
        this.size = n;
        if (!heapify) {
            return;
        }
        heapify();
    }

    private void tryGrow(Object[] array, int oldCap) {
        int i;
        this.lock.unlock();
        Object[] newArray = null;
        if (this.allocationSpinLock == 0 && U.compareAndSwapInt(this, ALLOCATIONSPINLOCK, 0, 1)) {
            if (oldCap < 64) {
                i = oldCap + 2;
            } else {
                i = oldCap >> 1;
            }
            int newCap = oldCap + i;
            try {
                if (newCap - MAX_ARRAY_SIZE > 0) {
                    int minCap = oldCap + 1;
                    if (minCap < 0 || minCap > MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError();
                    }
                    newCap = MAX_ARRAY_SIZE;
                }
                if (newCap > oldCap && this.queue == array) {
                    newArray = new Object[newCap];
                }
            } finally {
                this.allocationSpinLock = 0;
            }
        }
        if (newArray == null) {
            Thread.yield();
        }
        this.lock.lock();
        if (newArray == null || this.queue != array) {
            return;
        }
        this.queue = newArray;
        System.arraycopy(array, 0, newArray, 0, oldCap);
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

    private static <T> void siftUpComparable(int k, T x, Object[] objArr) {
        Comparable<? super T> key = (Comparable) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = objArr[parent];
            if (key.compareTo(e) >= 0) {
                break;
            }
            objArr[k] = e;
            k = parent;
        }
        objArr[k] = key;
    }

    private static <T> void siftUpUsingComparator(int k, T x, Object[] array, Comparator<? super T> cmp) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = array[parent];
            if (cmp.compare(x, e) >= 0) {
                break;
            }
            array[k] = e;
            k = parent;
        }
        array[k] = x;
    }

    private static <T> void siftDownComparable(int k, T x, Object[] objArr, int n) {
        if (n <= 0) {
            return;
        }
        Comparable<? super T> key = (Comparable) x;
        int half = n >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            Object c = objArr[child];
            int right = child + 1;
            if (right < n && ((Comparable) c).compareTo(objArr[right]) > 0) {
                child = right;
                c = objArr[right];
            }
            if (key.compareTo(c) <= 0) {
                break;
            }
            objArr[k] = c;
            k = child;
        }
        objArr[k] = key;
    }

    private static <T> void siftDownUsingComparator(int k, T x, Object[] array, int n, Comparator<? super T> cmp) {
        if (n <= 0) {
            return;
        }
        int half = n >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            Object c = array[child];
            int right = child + 1;
            if (right < n && cmp.compare(c, array[right]) > 0) {
                child = right;
                c = array[right];
            }
            if (cmp.compare(x, c) <= 0) {
                break;
            }
            array[k] = c;
            k = child;
        }
        array[k] = x;
    }

    private void heapify() {
        Object[] array = this.queue;
        int n = this.size;
        int half = (n >>> 1) - 1;
        Comparator<? super E> cmp = this.comparator;
        if (cmp == null) {
            for (int i = half; i >= 0; i--) {
                siftDownComparable(i, array[i], array, n);
            }
            return;
        }
        for (int i2 = half; i2 >= 0; i2--) {
            siftDownUsingComparator(i2, array[i2], array, n, cmp);
        }
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean offer(E e) {
        int n;
        Object[] array;
        if (e == null) {
            throw new NullPointerException();
        }
        ReentrantLock lock = this.lock;
        lock.lock();
        while (true) {
            n = this.size;
            array = this.queue;
            int cap = array.length;
            if (n >= cap) {
                tryGrow(array, cap);
            } else {
                try {
                    break;
                } catch (Throwable th) {
                    lock.unlock();
                    throw th;
                }
            }
        }
        Comparator<? super E> cmp = this.comparator;
        if (cmp == null) {
            siftUpComparable(n, e, array);
        } else {
            siftUpUsingComparator(n, e, array, cmp);
        }
        this.size = n + 1;
        this.notEmpty.signal();
        lock.unlock();
        return true;
    }

    @Override
    public void put(E e) {
        offer(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        return offer(e);
    }

    @Override
    public E poll() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        while (true) {
            try {
                E result = dequeue();
                if (result == null) {
                    this.notEmpty.await();
                } else {
                    return result;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E result;
        long nanos = unit.toNanos(timeout);
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        while (true) {
            try {
                result = dequeue();
                if (result != null || nanos <= 0) {
                    break;
                }
                nanos = this.notEmpty.awaitNanos(nanos);
            } finally {
                lock.unlock();
            }
        }
        return result;
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
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.size;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    private int indexOf(Object o) {
        if (o != null) {
            Object[] array = this.queue;
            int n = this.size;
            for (int i = 0; i < n; i++) {
                if (o.equals(array[i])) {
                    return i;
                }
            }
            return -1;
        }
        return -1;
    }

    private void removeAt(int i) {
        Object[] array = this.queue;
        int n = this.size - 1;
        if (n == i) {
            array[i] = null;
        } else {
            Object obj = array[n];
            array[n] = null;
            Comparator<? super E> cmp = this.comparator;
            if (cmp == null) {
                siftDownComparable(i, obj, array, n);
            } else {
                siftDownUsingComparator(i, obj, array, n, cmp);
            }
            if (array[i] == obj) {
                if (cmp == null) {
                    siftUpComparable(i, obj, array);
                } else {
                    siftUpUsingComparator(i, obj, array, cmp);
                }
            }
        }
        this.size = n;
    }

    @Override
    public boolean remove(Object o) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int i = indexOf(o);
            if (i != -1) {
                removeAt(i);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    void removeEQ(Object o) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] array = this.queue;
            int i = 0;
            int n = this.size;
            while (true) {
                if (i >= n) {
                    break;
                } else if (o == array[i]) {
                    break;
                } else {
                    i++;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return indexOf(o) != -1;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return Helpers.collectionToString(this);
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(this.size, maxElements);
            for (int i = 0; i < n; i++) {
                c.add(this.queue[0]);
                dequeue();
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] array = this.queue;
            int n = this.size;
            this.size = 0;
            for (int i = 0; i < n; i++) {
                array[i] = null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return Arrays.copyOf(this.queue, this.size);
        } finally {
            lock.unlock();
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

        Itr(Object[] array) {
            this.array = array;
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

    private void writeObject(ObjectOutputStream s) throws IOException {
        this.lock.lock();
        try {
            this.q = new PriorityQueue<>(Math.max(this.size, 1), this.comparator);
            this.q.addAll(this);
            s.defaultWriteObject();
        } finally {
            this.q = null;
            this.lock.unlock();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        try {
            s.defaultReadObject();
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

        PBQSpliterator(PriorityBlockingQueue<E> queue, Object[] array, int index, int fence) {
            this.queue = queue;
            this.array = array;
            this.index = index;
            this.fence = fence;
        }

        final int getFence() {
            int hi = this.fence;
            if (hi < 0) {
                Object[] array = this.queue.toArray();
                this.array = array;
                int hi2 = array.length;
                this.fence = hi2;
                return hi2;
            }
            return hi;
        }

        @Override
        public PBQSpliterator<E> trySplit() {
            int hi = getFence();
            int lo = this.index;
            int mid = (lo + hi) >>> 1;
            if (lo >= mid) {
                return null;
            }
            PriorityBlockingQueue<E> priorityBlockingQueue = this.queue;
            Object[] objArr = this.array;
            this.index = mid;
            return new PBQSpliterator<>(priorityBlockingQueue, objArr, lo, mid);
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            int i;
            if (action == null) {
                throw new NullPointerException();
            }
            Object[] a = this.array;
            if (a == null) {
                a = this.queue.toArray();
                this.fence = a.length;
            }
            int hi = this.fence;
            if (hi > a.length || (i = this.index) < 0) {
                return;
            }
            this.index = hi;
            if (i < hi) {
                do {
                    action.accept(a[i]);
                    i++;
                } while (i < hi);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            if (getFence() <= this.index || this.index < 0) {
                return false;
            }
            Object[] objArr = this.array;
            int i = this.index;
            this.index = i + 1;
            action.accept(objArr[i]);
            return true;
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
