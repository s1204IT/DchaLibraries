package java.util.concurrent;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ArrayBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
    private static final long serialVersionUID = -817911632652898426L;
    int count;
    final Object[] items;
    transient ArrayBlockingQueue<E>.Itrs itrs;
    final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;
    int putIndex;
    int takeIndex;

    final int dec(int i) {
        if (i == 0) {
            i = this.items.length;
        }
        return i - 1;
    }

    final E itemAt(int i) {
        return (E) this.items[i];
    }

    private void enqueue(E x) {
        Object[] items = this.items;
        items[this.putIndex] = x;
        int i = this.putIndex + 1;
        this.putIndex = i;
        if (i == items.length) {
            this.putIndex = 0;
        }
        this.count++;
        this.notEmpty.signal();
    }

    private E dequeue() {
        Object[] objArr = this.items;
        E e = (E) objArr[this.takeIndex];
        objArr[this.takeIndex] = null;
        int i = this.takeIndex + 1;
        this.takeIndex = i;
        if (i == objArr.length) {
            this.takeIndex = 0;
        }
        this.count--;
        if (this.itrs != null) {
            this.itrs.elementDequeued();
        }
        this.notFull.signal();
        return e;
    }

    void removeAt(int removeIndex) {
        int pred;
        Object[] items = this.items;
        if (removeIndex == this.takeIndex) {
            items[this.takeIndex] = null;
            int i = this.takeIndex + 1;
            this.takeIndex = i;
            if (i == items.length) {
                this.takeIndex = 0;
            }
            this.count--;
            if (this.itrs != null) {
                this.itrs.elementDequeued();
            }
        } else {
            int i2 = removeIndex;
            int putIndex = this.putIndex;
            while (true) {
                pred = i2;
                i2++;
                if (i2 == items.length) {
                    i2 = 0;
                }
                if (i2 == putIndex) {
                    break;
                } else {
                    items[pred] = items[i2];
                }
            }
            items[pred] = null;
            this.putIndex = pred;
            this.count--;
            if (this.itrs != null) {
                this.itrs.removedAt(removeIndex);
            }
        }
        this.notFull.signal();
    }

    public ArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }

    public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.items = new Object[capacity];
        this.lock = new ReentrantLock(fair);
        this.notEmpty = this.lock.newCondition();
        this.notFull = this.lock.newCondition();
    }

    public ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) throws Throwable {
        Iterator e$iterator;
        int i;
        this(capacity, fair);
        ReentrantLock lock = this.lock;
        lock.lock();
        int i2 = 0;
        try {
            try {
                e$iterator = c.iterator();
            } catch (ArrayIndexOutOfBoundsException e) {
            }
            while (true) {
                try {
                    try {
                        i = i2;
                        if (!e$iterator.hasNext()) {
                            break;
                        }
                        i2 = i + 1;
                        this.items[i] = Objects.requireNonNull(e$iterator.next());
                    } catch (ArrayIndexOutOfBoundsException e2) {
                        throw new IllegalArgumentException();
                    }
                } catch (Throwable th) {
                    th = th;
                    lock.unlock();
                    throw th;
                }
                throw new IllegalArgumentException();
            }
            this.count = i;
            this.putIndex = i == capacity ? 0 : i;
            lock.unlock();
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override
    public boolean add(E e) {
        return super.add(e);
    }

    @Override
    public boolean offer(E e) {
        Objects.requireNonNull(e);
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (this.count != this.items.length) {
                enqueue(e);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(E e) throws InterruptedException {
        Objects.requireNonNull(e);
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        while (this.count == this.items.length) {
            try {
                this.notFull.await();
            } finally {
                lock.unlock();
            }
        }
        enqueue(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(e);
        long nanos = unit.toNanos(timeout);
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        while (this.count == this.items.length) {
            try {
                if (nanos > 0) {
                    nanos = this.notFull.awaitNanos(nanos);
                } else {
                    return false;
                }
            } finally {
                lock.unlock();
            }
        }
        enqueue(e);
        return true;
    }

    @Override
    public E poll() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.count == 0 ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        while (this.count == 0) {
            try {
                this.notEmpty.await();
            } finally {
                lock.unlock();
            }
        }
        return dequeue();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        while (this.count == 0) {
            try {
                if (nanos > 0) {
                    nanos = this.notEmpty.awaitNanos(nanos);
                } else {
                    return null;
                }
            } finally {
                lock.unlock();
            }
        }
        return dequeue();
    }

    @Override
    public E peek() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return itemAt(this.takeIndex);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.items.length - this.count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (this.count > 0) {
                Object[] items = this.items;
                int putIndex = this.putIndex;
                int i = this.takeIndex;
                while (!o.equals(items[i])) {
                    i++;
                    if (i == items.length) {
                        i = 0;
                    }
                    if (i == putIndex) {
                    }
                }
                removeAt(i);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (this.count > 0) {
                Object[] items = this.items;
                int putIndex = this.putIndex;
                int i = this.takeIndex;
                while (!o.equals(items[i])) {
                    i++;
                    if (i == items.length) {
                        i = 0;
                    }
                    if (i == putIndex) {
                    }
                }
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] items = this.items;
            int end = this.takeIndex + this.count;
            Object[] a = Arrays.copyOfRange(items, this.takeIndex, end);
            if (end != this.putIndex) {
                System.arraycopy(items, 0, a, items.length - this.takeIndex, this.putIndex);
            }
            return a;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            Object[] objArr = this.items;
            int i = this.count;
            int iMin = Math.min(objArr.length - this.takeIndex, i);
            if (tArr.length < i) {
                tArr = (T[]) Arrays.copyOfRange(objArr, this.takeIndex, this.takeIndex + i, tArr.getClass());
            } else {
                System.arraycopy(objArr, this.takeIndex, tArr, 0, iMin);
                if (tArr.length > i) {
                    tArr[i] = null;
                }
            }
            if (iMin < i) {
                System.arraycopy(objArr, 0, tArr, iMin, this.putIndex);
            }
            return tArr;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public String toString() {
        return Helpers.collectionToString(this);
    }

    @Override
    public void clear() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int k = this.count;
            if (k > 0) {
                Object[] items = this.items;
                int putIndex = this.putIndex;
                int i = this.takeIndex;
                do {
                    items[i] = null;
                    i++;
                    if (i == items.length) {
                        i = 0;
                    }
                } while (i != putIndex);
                this.takeIndex = putIndex;
                this.count = 0;
                if (this.itrs != null) {
                    this.itrs.queueIsEmpty();
                }
                while (k > 0) {
                    if (!lock.hasWaiters(this.notFull)) {
                        break;
                    }
                    this.notFull.signal();
                    k--;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        Objects.requireNonNull(c);
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        Object[] items = this.items;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int n = Math.min(maxElements, this.count);
            int take = this.takeIndex;
            int i = 0;
            while (i < n) {
                try {
                    c.add(items[take]);
                    items[take] = null;
                    take++;
                    if (take == items.length) {
                        take = 0;
                    }
                    i++;
                } catch (Throwable th) {
                    if (i > 0) {
                        this.count -= i;
                        this.takeIndex = take;
                        if (this.itrs != null) {
                            if (this.count == 0) {
                                this.itrs.queueIsEmpty();
                            } else if (i > take) {
                                this.itrs.takeIndexWrapped();
                            }
                        }
                        while (i > 0 && lock.hasWaiters(this.notFull)) {
                            this.notFull.signal();
                            i--;
                        }
                    }
                    throw th;
                }
            }
            if (i > 0) {
                this.count -= i;
                this.takeIndex = take;
                if (this.itrs != null) {
                    if (this.count == 0) {
                        this.itrs.queueIsEmpty();
                    } else if (i > take) {
                        this.itrs.takeIndexWrapped();
                    }
                }
                while (i > 0 && lock.hasWaiters(this.notFull)) {
                    this.notFull.signal();
                    i--;
                }
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    class Itrs {
        private static final int LONG_SWEEP_PROBES = 16;
        private static final int SHORT_SWEEP_PROBES = 4;
        int cycles;
        private ArrayBlockingQueue<E>.Itrs.Node head;
        private ArrayBlockingQueue<E>.Itrs.Node sweeper;

        private class Node extends WeakReference<ArrayBlockingQueue<E>.Itr> {
            ArrayBlockingQueue<E>.Itrs.Node next;

            Node(ArrayBlockingQueue<E>.Itr iterator, ArrayBlockingQueue<E>.Itrs.Node next) {
                super(iterator);
                this.next = next;
            }
        }

        Itrs(ArrayBlockingQueue<E>.Itr initial) {
            register(initial);
        }

        void doSomeSweeping(boolean tryHarder) {
            ArrayBlockingQueue<E>.Itrs.Node o;
            ArrayBlockingQueue<E>.Itrs.Node p;
            boolean passedGo;
            int probes = tryHarder ? 16 : 4;
            ArrayBlockingQueue<E>.Itrs.Node sweeper = this.sweeper;
            if (sweeper == null) {
                o = null;
                p = this.head;
                passedGo = true;
            } else {
                o = sweeper;
                p = sweeper.next;
                passedGo = false;
            }
            while (probes > 0) {
                if (p == null) {
                    if (passedGo) {
                        break;
                    }
                    o = null;
                    p = this.head;
                    passedGo = true;
                }
                ArrayBlockingQueue<E>.Itr it = p.get();
                ArrayBlockingQueue<E>.Itrs.Node next = p.next;
                if (it == null || it.isDetached()) {
                    probes = 16;
                    p.clear();
                    p.next = null;
                    if (o == null) {
                        this.head = next;
                        if (next == null) {
                            ArrayBlockingQueue.this.itrs = null;
                            return;
                        }
                    } else {
                        o.next = next;
                    }
                } else {
                    o = p;
                }
                p = next;
                probes--;
            }
            if (p == null) {
                o = null;
            }
            this.sweeper = o;
        }

        void register(ArrayBlockingQueue<E>.Itr itr) {
            this.head = new Node(itr, this.head);
        }

        void takeIndexWrapped() {
            this.cycles++;
            ArrayBlockingQueue<E>.Itrs.Node o = null;
            ArrayBlockingQueue<E>.Itrs.Node p = this.head;
            while (p != null) {
                ArrayBlockingQueue<E>.Itr it = p.get();
                ArrayBlockingQueue<E>.Itrs.Node next = p.next;
                if (it == null || it.takeIndexWrapped()) {
                    p.clear();
                    p.next = null;
                    if (o == null) {
                        this.head = next;
                    } else {
                        o.next = next;
                    }
                } else {
                    o = p;
                }
                p = next;
            }
            if (this.head != null) {
                return;
            }
            ArrayBlockingQueue.this.itrs = null;
        }

        void removedAt(int removedIndex) {
            ArrayBlockingQueue<E>.Itrs.Node o = null;
            ArrayBlockingQueue<E>.Itrs.Node p = this.head;
            while (p != null) {
                ArrayBlockingQueue<E>.Itr it = p.get();
                ArrayBlockingQueue<E>.Itrs.Node next = p.next;
                if (it == null || it.removedAt(removedIndex)) {
                    p.clear();
                    p.next = null;
                    if (o == null) {
                        this.head = next;
                    } else {
                        o.next = next;
                    }
                } else {
                    o = p;
                }
                p = next;
            }
            if (this.head != null) {
                return;
            }
            ArrayBlockingQueue.this.itrs = null;
        }

        void queueIsEmpty() {
            for (ArrayBlockingQueue<E>.Itrs.Node p = this.head; p != null; p = p.next) {
                ArrayBlockingQueue<E>.Itr it = p.get();
                if (it != null) {
                    p.clear();
                    it.shutdown();
                }
            }
            this.head = null;
            ArrayBlockingQueue.this.itrs = null;
        }

        void elementDequeued() {
            if (ArrayBlockingQueue.this.count == 0) {
                queueIsEmpty();
            } else {
                if (ArrayBlockingQueue.this.takeIndex != 0) {
                    return;
                }
                takeIndexWrapped();
            }
        }
    }

    private class Itr implements Iterator<E> {
        private static final int DETACHED = -3;
        private static final int NONE = -1;
        private static final int REMOVED = -2;
        private int cursor;
        private E lastItem;
        private int lastRet = -1;
        private int nextIndex;
        private E nextItem;
        private int prevCycles;
        private int prevTakeIndex;

        Itr() {
            ReentrantLock reentrantLock = ArrayBlockingQueue.this.lock;
            reentrantLock.lock();
            try {
                if (ArrayBlockingQueue.this.count == 0) {
                    this.cursor = -1;
                    this.nextIndex = -1;
                    this.prevTakeIndex = DETACHED;
                } else {
                    int i = ArrayBlockingQueue.this.takeIndex;
                    this.prevTakeIndex = i;
                    this.nextIndex = i;
                    this.nextItem = (E) ArrayBlockingQueue.this.itemAt(i);
                    this.cursor = incCursor(i);
                    if (ArrayBlockingQueue.this.itrs == null) {
                        ArrayBlockingQueue.this.itrs = new Itrs(this);
                    } else {
                        ArrayBlockingQueue.this.itrs.register(this);
                        ArrayBlockingQueue.this.itrs.doSomeSweeping(false);
                    }
                    this.prevCycles = ArrayBlockingQueue.this.itrs.cycles;
                }
            } finally {
                reentrantLock.unlock();
            }
        }

        boolean isDetached() {
            return this.prevTakeIndex < 0;
        }

        private int incCursor(int index) {
            int index2 = index + 1;
            if (index2 == ArrayBlockingQueue.this.items.length) {
                index2 = 0;
            }
            if (index2 == ArrayBlockingQueue.this.putIndex) {
                return -1;
            }
            return index2;
        }

        private boolean invalidated(int index, int prevTakeIndex, long dequeues, int length) {
            if (index < 0) {
                return false;
            }
            int distance = index - prevTakeIndex;
            if (distance < 0) {
                distance += length;
            }
            return dequeues > ((long) distance);
        }

        private void incorporateDequeues() {
            int cycles = ArrayBlockingQueue.this.itrs.cycles;
            int takeIndex = ArrayBlockingQueue.this.takeIndex;
            int prevCycles = this.prevCycles;
            int prevTakeIndex = this.prevTakeIndex;
            if (cycles == prevCycles && takeIndex == prevTakeIndex) {
                return;
            }
            int len = ArrayBlockingQueue.this.items.length;
            long dequeues = ((cycles - prevCycles) * len) + (takeIndex - prevTakeIndex);
            if (invalidated(this.lastRet, prevTakeIndex, dequeues, len)) {
                this.lastRet = -2;
            }
            if (invalidated(this.nextIndex, prevTakeIndex, dequeues, len)) {
                this.nextIndex = -2;
            }
            if (invalidated(this.cursor, prevTakeIndex, dequeues, len)) {
                this.cursor = takeIndex;
            }
            if (this.cursor < 0 && this.nextIndex < 0 && this.lastRet < 0) {
                detach();
            } else {
                this.prevCycles = cycles;
                this.prevTakeIndex = takeIndex;
            }
        }

        private void detach() {
            if (this.prevTakeIndex < 0) {
                return;
            }
            this.prevTakeIndex = DETACHED;
            ArrayBlockingQueue.this.itrs.doSomeSweeping(true);
        }

        @Override
        public boolean hasNext() {
            if (this.nextItem != null) {
                return true;
            }
            noNext();
            return false;
        }

        private void noNext() {
            ReentrantLock reentrantLock = ArrayBlockingQueue.this.lock;
            reentrantLock.lock();
            try {
                if (!isDetached()) {
                    incorporateDequeues();
                    if (this.lastRet >= 0) {
                        this.lastItem = (E) ArrayBlockingQueue.this.itemAt(this.lastRet);
                        detach();
                    }
                }
            } finally {
                reentrantLock.unlock();
            }
        }

        @Override
        public E next() {
            E e = this.nextItem;
            if (e == null) {
                throw new NoSuchElementException();
            }
            ReentrantLock reentrantLock = ArrayBlockingQueue.this.lock;
            reentrantLock.lock();
            try {
                if (!isDetached()) {
                    incorporateDequeues();
                }
                this.lastRet = this.nextIndex;
                int i = this.cursor;
                if (i >= 0) {
                    ArrayBlockingQueue arrayBlockingQueue = ArrayBlockingQueue.this;
                    this.nextIndex = i;
                    this.nextItem = (E) arrayBlockingQueue.itemAt(i);
                    this.cursor = incCursor(i);
                } else {
                    this.nextIndex = -1;
                    this.nextItem = null;
                }
                return e;
            } finally {
                reentrantLock.unlock();
            }
        }

        @Override
        public void remove() {
            ReentrantLock lock = ArrayBlockingQueue.this.lock;
            lock.lock();
            try {
                if (!isDetached()) {
                    incorporateDequeues();
                }
                int lastRet = this.lastRet;
                this.lastRet = -1;
                if (lastRet >= 0) {
                    if (!isDetached()) {
                        ArrayBlockingQueue.this.removeAt(lastRet);
                    } else {
                        E lastItem = this.lastItem;
                        this.lastItem = null;
                        if (ArrayBlockingQueue.this.itemAt(lastRet) == lastItem) {
                            ArrayBlockingQueue.this.removeAt(lastRet);
                        }
                    }
                } else if (lastRet == -1) {
                    throw new IllegalStateException();
                }
                if (this.cursor < 0 && this.nextIndex < 0) {
                    detach();
                }
            } finally {
                lock.unlock();
            }
        }

        void shutdown() {
            this.cursor = -1;
            if (this.nextIndex >= 0) {
                this.nextIndex = -2;
            }
            if (this.lastRet >= 0) {
                this.lastRet = -2;
                this.lastItem = null;
            }
            this.prevTakeIndex = DETACHED;
        }

        private int distance(int index, int prevTakeIndex, int length) {
            int distance = index - prevTakeIndex;
            if (distance < 0) {
                return distance + length;
            }
            return distance;
        }

        boolean removedAt(int removedIndex) {
            if (isDetached()) {
                return true;
            }
            int takeIndex = ArrayBlockingQueue.this.takeIndex;
            int prevTakeIndex = this.prevTakeIndex;
            int len = ArrayBlockingQueue.this.items.length;
            int removedDistance = (((removedIndex < takeIndex ? 1 : 0) + (ArrayBlockingQueue.this.itrs.cycles - this.prevCycles)) * len) + (removedIndex - prevTakeIndex);
            int cursor = this.cursor;
            if (cursor >= 0) {
                int x = distance(cursor, prevTakeIndex, len);
                if (x == removedDistance) {
                    if (cursor == ArrayBlockingQueue.this.putIndex) {
                        cursor = -1;
                        this.cursor = -1;
                    }
                } else if (x > removedDistance) {
                    cursor = ArrayBlockingQueue.this.dec(cursor);
                    this.cursor = cursor;
                }
            }
            int lastRet = this.lastRet;
            if (lastRet >= 0) {
                int x2 = distance(lastRet, prevTakeIndex, len);
                if (x2 == removedDistance) {
                    lastRet = -2;
                    this.lastRet = -2;
                } else if (x2 > removedDistance) {
                    lastRet = ArrayBlockingQueue.this.dec(lastRet);
                    this.lastRet = lastRet;
                }
            }
            int nextIndex = this.nextIndex;
            if (nextIndex >= 0) {
                int x3 = distance(nextIndex, prevTakeIndex, len);
                if (x3 == removedDistance) {
                    nextIndex = -2;
                    this.nextIndex = -2;
                } else if (x3 > removedDistance) {
                    nextIndex = ArrayBlockingQueue.this.dec(nextIndex);
                    this.nextIndex = nextIndex;
                }
            }
            if (cursor >= 0 || nextIndex >= 0 || lastRet >= 0) {
                return false;
            }
            this.prevTakeIndex = DETACHED;
            return true;
        }

        boolean takeIndexWrapped() {
            if (isDetached()) {
                return true;
            }
            if (ArrayBlockingQueue.this.itrs.cycles - this.prevCycles > 1) {
                shutdown();
                return true;
            }
            return false;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, 4368);
    }
}
