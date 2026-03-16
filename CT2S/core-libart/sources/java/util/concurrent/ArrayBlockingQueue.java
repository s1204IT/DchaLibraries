package java.util.concurrent;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
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

    final int inc(int i) {
        int i2 = i + 1;
        if (i2 == this.items.length) {
            return 0;
        }
        return i2;
    }

    final int dec(int i) {
        if (i == 0) {
            i = this.items.length;
        }
        return i - 1;
    }

    final E itemAt(int i) {
        return (E) this.items[i];
    }

    private static void checkNotNull(Object v) {
        if (v == null) {
            throw new NullPointerException();
        }
    }

    private void enqueue(E x) {
        this.items[this.putIndex] = x;
        this.putIndex = inc(this.putIndex);
        this.count++;
        this.notEmpty.signal();
    }

    private E dequeue() {
        Object[] objArr = this.items;
        E e = (E) objArr[this.takeIndex];
        objArr[this.takeIndex] = null;
        this.takeIndex = inc(this.takeIndex);
        this.count--;
        if (this.itrs != null) {
            this.itrs.elementDequeued();
        }
        this.notFull.signal();
        return e;
    }

    void removeAt(int removeIndex) {
        Object[] items = this.items;
        if (removeIndex == this.takeIndex) {
            items[this.takeIndex] = null;
            this.takeIndex = inc(this.takeIndex);
            this.count--;
            if (this.itrs != null) {
                this.itrs.elementDequeued();
            }
        } else {
            int putIndex = this.putIndex;
            int i = removeIndex;
            while (true) {
                int next = inc(i);
                if (next == putIndex) {
                    break;
                }
                items[i] = items[next];
                i = next;
            }
            items[i] = null;
            this.putIndex = i;
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
        this.itrs = null;
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.items = new Object[capacity];
        this.lock = new ReentrantLock(fair);
        this.notEmpty = this.lock.newCondition();
        this.notFull = this.lock.newCondition();
    }

    public ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) throws Throwable {
        Iterator<? extends E> it;
        int i;
        this(capacity, fair);
        ReentrantLock lock = this.lock;
        lock.lock();
        int i2 = 0;
        try {
            try {
                it = c.iterator();
            } catch (ArrayIndexOutOfBoundsException e) {
            }
            while (true) {
                try {
                    try {
                        i = i2;
                        if (!it.hasNext()) {
                            break;
                        }
                        E e2 = it.next();
                        checkNotNull(e2);
                        i2 = i + 1;
                        this.items[i] = e2;
                    } catch (Throwable th) {
                        th = th;
                        lock.unlock();
                        throw th;
                    }
                } catch (ArrayIndexOutOfBoundsException e3) {
                    throw new IllegalArgumentException();
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
        checkNotNull(e);
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
        checkNotNull(e);
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
        checkNotNull(e);
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
        Object[] items = this.items;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (this.count > 0) {
                int putIndex = this.putIndex;
                int i = this.takeIndex;
                while (!o.equals(items[i])) {
                    i = inc(i);
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
        Object[] items = this.items;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (this.count > 0) {
                int putIndex = this.putIndex;
                int i = this.takeIndex;
                while (!o.equals(items[i])) {
                    i = inc(i);
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
        Object[] items = this.items;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int count = this.count;
            Object[] a = new Object[count];
            int n = items.length - this.takeIndex;
            if (count <= n) {
                System.arraycopy(items, this.takeIndex, a, 0, count);
            } else {
                System.arraycopy(items, this.takeIndex, a, 0, n);
                System.arraycopy(items, 0, a, n, count - n);
            }
            return a;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        Object[] objArr = this.items;
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            int i = this.count;
            int length = tArr.length;
            if (length < i) {
                tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), i));
            }
            int length2 = objArr.length - this.takeIndex;
            if (i <= length2) {
                System.arraycopy(objArr, this.takeIndex, tArr, 0, i);
            } else {
                System.arraycopy(objArr, this.takeIndex, tArr, 0, length2);
                System.arraycopy(objArr, 0, tArr, length2, i - length2);
            }
            if (length > i) {
                tArr[i] = null;
            }
            return tArr;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public String toString() {
        String string;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int k = this.count;
            if (k == 0) {
                string = "[]";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                int i = this.takeIndex;
                while (true) {
                    Object e = this.items[i];
                    if (e == this) {
                        e = "(this Collection)";
                    }
                    sb.append(e);
                    k--;
                    if (k == 0) {
                        break;
                    }
                    sb.append(',').append(' ');
                    i = inc(i);
                }
                string = sb.append(']').toString();
            }
            return string;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        Object[] items = this.items;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int k = this.count;
            if (k > 0) {
                int putIndex = this.putIndex;
                int i = this.takeIndex;
                do {
                    items[i] = null;
                    i = inc(i);
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
        checkNotNull(c);
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
                    take = inc(take);
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
        private ArrayBlockingQueue<E>.Itrs.Node head;
        int cycles = 0;
        private ArrayBlockingQueue<E>.Itrs.Node sweeper = null;

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
                p = o.next;
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
            this.sweeper = p != null ? o : null;
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
            if (this.head == null) {
                ArrayBlockingQueue.this.itrs = null;
            }
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
            if (this.head == null) {
                ArrayBlockingQueue.this.itrs = null;
            }
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
            } else if (ArrayBlockingQueue.this.takeIndex == 0) {
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
                    this.prevTakeIndex = -3;
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
            int index2 = ArrayBlockingQueue.this.inc(index);
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
            if (cycles != prevCycles || takeIndex != prevTakeIndex) {
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
        }

        private void detach() {
            if (this.prevTakeIndex >= 0) {
                this.prevTakeIndex = -3;
                ArrayBlockingQueue.this.itrs.doSomeSweeping(true);
            }
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
            this.prevTakeIndex = -3;
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
            int cycles = ArrayBlockingQueue.this.itrs.cycles;
            int takeIndex = ArrayBlockingQueue.this.takeIndex;
            int prevCycles = this.prevCycles;
            int prevTakeIndex = this.prevTakeIndex;
            int len = ArrayBlockingQueue.this.items.length;
            int cycleDiff = cycles - prevCycles;
            if (removedIndex < takeIndex) {
                cycleDiff++;
            }
            int removedDistance = (cycleDiff * len) + (removedIndex - prevTakeIndex);
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
                    this.nextIndex = -2;
                } else if (x3 > removedDistance) {
                    this.nextIndex = ArrayBlockingQueue.this.dec(nextIndex);
                }
            } else if (cursor < 0 && nextIndex < 0 && lastRet < 0) {
                this.prevTakeIndex = -3;
                return true;
            }
            return false;
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
}
