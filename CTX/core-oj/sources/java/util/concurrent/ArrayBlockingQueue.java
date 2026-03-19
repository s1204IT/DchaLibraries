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

    private void enqueue(E e) {
        Object[] objArr = this.items;
        objArr[this.putIndex] = e;
        int i = this.putIndex + 1;
        this.putIndex = i;
        if (i == objArr.length) {
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

    void removeAt(int i) {
        Object[] objArr = this.items;
        if (i == this.takeIndex) {
            objArr[this.takeIndex] = null;
            int i2 = this.takeIndex + 1;
            this.takeIndex = i2;
            if (i2 == objArr.length) {
                this.takeIndex = 0;
            }
            this.count--;
            if (this.itrs != null) {
                this.itrs.elementDequeued();
            }
        } else {
            int i3 = this.putIndex;
            int i4 = i;
            while (true) {
                int i5 = i4 + 1;
                if (i5 == objArr.length) {
                    i5 = 0;
                }
                if (i5 == i3) {
                    break;
                }
                objArr[i4] = objArr[i5];
                i4 = i5;
            }
            objArr[i4] = null;
            this.putIndex = i4;
            this.count--;
            if (this.itrs != null) {
                this.itrs.removedAt(i);
            }
        }
        this.notFull.signal();
    }

    public ArrayBlockingQueue(int i) {
        this(i, false);
    }

    public ArrayBlockingQueue(int i, boolean z) {
        if (i <= 0) {
            throw new IllegalArgumentException();
        }
        this.items = new Object[i];
        this.lock = new ReentrantLock(z);
        this.notEmpty = this.lock.newCondition();
        this.notFull = this.lock.newCondition();
    }

    public ArrayBlockingQueue(int i, boolean z, Collection<? extends E> collection) {
        this(i, z);
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            try {
                Iterator<? extends E> it = collection.iterator();
                int i2 = 0;
                int i3 = 0;
                while (it.hasNext()) {
                    int i4 = i3 + 1;
                    this.items[i3] = Objects.requireNonNull(it.next());
                    i3 = i4;
                }
                this.count = i3;
                if (i3 != i) {
                    i2 = i3;
                }
                this.putIndex = i2;
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException();
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public boolean add(E e) {
        return super.add(e);
    }

    @Override
    public boolean offer(E e) {
        Objects.requireNonNull(e);
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            if (this.count != this.items.length) {
                enqueue(e);
                return true;
            }
            return false;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public void put(E e) throws InterruptedException {
        Objects.requireNonNull(e);
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lockInterruptibly();
        while (this.count == this.items.length) {
            try {
                this.notFull.await();
            } finally {
                reentrantLock.unlock();
            }
        }
        enqueue(e);
    }

    @Override
    public boolean offer(E e, long j, TimeUnit timeUnit) throws InterruptedException {
        Objects.requireNonNull(e);
        long nanos = timeUnit.toNanos(j);
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lockInterruptibly();
        while (this.count == this.items.length) {
            try {
                if (nanos > 0) {
                    nanos = this.notFull.awaitNanos(nanos);
                } else {
                    return false;
                }
            } finally {
                reentrantLock.unlock();
            }
        }
        enqueue(e);
        return true;
    }

    @Override
    public E poll() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.count == 0 ? null : dequeue();
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lockInterruptibly();
        while (this.count == 0) {
            try {
                this.notEmpty.await();
            } finally {
                reentrantLock.unlock();
            }
        }
        return dequeue();
    }

    @Override
    public E poll(long j, TimeUnit timeUnit) throws InterruptedException {
        long nanos = timeUnit.toNanos(j);
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lockInterruptibly();
        while (this.count == 0) {
            try {
                if (nanos > 0) {
                    nanos = this.notEmpty.awaitNanos(nanos);
                } else {
                    return null;
                }
            } finally {
                reentrantLock.unlock();
            }
        }
        return dequeue();
    }

    @Override
    public E peek() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return itemAt(this.takeIndex);
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public int size() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.count;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.items.length - this.count;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public boolean remove(Object obj) {
        if (obj == null) {
            return false;
        }
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            if (this.count > 0) {
                Object[] objArr = this.items;
                int i = this.putIndex;
                int i2 = this.takeIndex;
                while (!obj.equals(objArr[i2])) {
                    i2++;
                    if (i2 == objArr.length) {
                        i2 = 0;
                    }
                    if (i2 == i) {
                    }
                }
                removeAt(i2);
                reentrantLock.unlock();
                return true;
            }
            return false;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            if (this.count > 0) {
                Object[] objArr = this.items;
                int i = this.putIndex;
                int i2 = this.takeIndex;
                while (!obj.equals(objArr[i2])) {
                    i2++;
                    if (i2 == objArr.length) {
                        i2 = 0;
                    }
                    if (i2 == i) {
                    }
                }
                reentrantLock.unlock();
                return true;
            }
            return false;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public Object[] toArray() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            Object[] objArr = this.items;
            int i = this.takeIndex + this.count;
            Object[] objArrCopyOfRange = Arrays.copyOfRange(objArr, this.takeIndex, i);
            if (i != this.putIndex) {
                System.arraycopy(objArr, 0, objArrCopyOfRange, objArr.length - this.takeIndex, this.putIndex);
            }
            return objArrCopyOfRange;
        } finally {
            reentrantLock.unlock();
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
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            int i = this.count;
            if (i > 0) {
                Object[] objArr = this.items;
                int i2 = this.putIndex;
                int i3 = this.takeIndex;
                do {
                    objArr[i3] = null;
                    i3++;
                    if (i3 == objArr.length) {
                        i3 = 0;
                    }
                } while (i3 != i2);
                this.takeIndex = i2;
                this.count = 0;
                if (this.itrs != null) {
                    this.itrs.queueIsEmpty();
                }
                while (i > 0) {
                    if (!reentrantLock.hasWaiters(this.notFull)) {
                        break;
                    }
                    this.notFull.signal();
                    i--;
                }
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> collection) {
        return drainTo(collection, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> collection, int i) {
        Objects.requireNonNull(collection);
        if (collection == this) {
            throw new IllegalArgumentException();
        }
        if (i <= 0) {
            return 0;
        }
        Object[] objArr = this.items;
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            int iMin = Math.min(i, this.count);
            int i2 = this.takeIndex;
            int i3 = 0;
            while (i3 < iMin) {
                try {
                    collection.add(objArr[i2]);
                    objArr[i2] = null;
                    i2++;
                    if (i2 == objArr.length) {
                        i2 = 0;
                    }
                    i3++;
                } catch (Throwable th) {
                    if (i3 > 0) {
                        this.count -= i3;
                        this.takeIndex = i2;
                        if (this.itrs != null) {
                            if (this.count == 0) {
                                this.itrs.queueIsEmpty();
                            } else if (i3 > i2) {
                                this.itrs.takeIndexWrapped();
                            }
                        }
                        while (i3 > 0 && reentrantLock.hasWaiters(this.notFull)) {
                            this.notFull.signal();
                            i3--;
                        }
                    }
                    throw th;
                }
            }
            if (i3 > 0) {
                this.count -= i3;
                this.takeIndex = i2;
                if (this.itrs != null) {
                    if (this.count == 0) {
                        this.itrs.queueIsEmpty();
                    } else if (i3 > i2) {
                        this.itrs.takeIndexWrapped();
                    }
                }
                while (i3 > 0 && reentrantLock.hasWaiters(this.notFull)) {
                    this.notFull.signal();
                    i3--;
                }
            }
            return iMin;
        } finally {
            reentrantLock.unlock();
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

            Node(ArrayBlockingQueue<E>.Itr itr, ArrayBlockingQueue<E>.Itrs.Node node) {
                super(itr);
                this.next = node;
            }
        }

        Itrs(ArrayBlockingQueue<E>.Itr itr) {
            register(itr);
        }

        void doSomeSweeping(boolean z) {
            int i;
            boolean z2;
            ArrayBlockingQueue<E>.Itrs.Node node;
            ArrayBlockingQueue<E>.Itrs.Node node2;
            if (!z) {
                i = 4;
            } else {
                i = 16;
            }
            ArrayBlockingQueue<E>.Itrs.Node node3 = this.sweeper;
            if (node3 == null) {
                node2 = this.head;
                z2 = true;
                node = null;
            } else {
                z2 = false;
                node = node3;
                node2 = node3.next;
            }
            while (i > 0) {
                if (node2 == null) {
                    if (z2) {
                        break;
                    }
                    node2 = this.head;
                    z2 = true;
                    node = null;
                }
                ArrayBlockingQueue<E>.Itr itr = node2.get();
                ArrayBlockingQueue<E>.Itrs.Node node4 = node2.next;
                if (itr == null || itr.isDetached()) {
                    node2.clear();
                    node2.next = null;
                    if (node == null) {
                        this.head = node4;
                        if (node4 == null) {
                            ArrayBlockingQueue.this.itrs = null;
                            return;
                        }
                    } else {
                        node.next = node4;
                    }
                    i = 16;
                } else {
                    node = node2;
                }
                i--;
                node2 = node4;
            }
            this.sweeper = node2 != null ? node : null;
        }

        void register(ArrayBlockingQueue<E>.Itr itr) {
            this.head = new Node(itr, this.head);
        }

        void takeIndexWrapped() {
            this.cycles++;
            ArrayBlockingQueue<E>.Itrs.Node node = this.head;
            ArrayBlockingQueue<E>.Itrs.Node node2 = null;
            while (node != null) {
                ArrayBlockingQueue<E>.Itr itr = node.get();
                ArrayBlockingQueue<E>.Itrs.Node node3 = node.next;
                if (itr == null || itr.takeIndexWrapped()) {
                    node.clear();
                    node.next = null;
                    if (node2 == null) {
                        this.head = node3;
                    } else {
                        node2.next = node3;
                    }
                } else {
                    node2 = node;
                }
                node = node3;
            }
            if (this.head == null) {
                ArrayBlockingQueue.this.itrs = null;
            }
        }

        void removedAt(int i) {
            ArrayBlockingQueue<E>.Itrs.Node node = this.head;
            ArrayBlockingQueue<E>.Itrs.Node node2 = null;
            while (node != null) {
                ArrayBlockingQueue<E>.Itr itr = node.get();
                ArrayBlockingQueue<E>.Itrs.Node node3 = node.next;
                if (itr == null || itr.removedAt(i)) {
                    node.clear();
                    node.next = null;
                    if (node2 == null) {
                        this.head = node3;
                    } else {
                        node2.next = node3;
                    }
                } else {
                    node2 = node;
                }
                node = node3;
            }
            if (this.head == null) {
                ArrayBlockingQueue.this.itrs = null;
            }
        }

        void queueIsEmpty() {
            for (ArrayBlockingQueue<E>.Itrs.Node node = this.head; node != null; node = node.next) {
                ArrayBlockingQueue<E>.Itr itr = node.get();
                if (itr != null) {
                    node.clear();
                    itr.shutdown();
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

        private int incCursor(int i) {
            int i2 = i + 1;
            if (i2 == ArrayBlockingQueue.this.items.length) {
                i2 = 0;
            }
            if (i2 == ArrayBlockingQueue.this.putIndex) {
                return -1;
            }
            return i2;
        }

        private boolean invalidated(int i, int i2, long j, int i3) {
            if (i < 0) {
                return false;
            }
            int i4 = i - i2;
            if (i4 < 0) {
                i4 += i3;
            }
            return j > ((long) i4);
        }

        private void incorporateDequeues() {
            int i = ArrayBlockingQueue.this.itrs.cycles;
            int i2 = ArrayBlockingQueue.this.takeIndex;
            int i3 = this.prevCycles;
            int i4 = this.prevTakeIndex;
            if (i != i3 || i2 != i4) {
                int length = ArrayBlockingQueue.this.items.length;
                long j = ((i - i3) * length) + (i2 - i4);
                if (invalidated(this.lastRet, i4, j, length)) {
                    this.lastRet = -2;
                }
                if (invalidated(this.nextIndex, i4, j, length)) {
                    this.nextIndex = -2;
                }
                if (invalidated(this.cursor, i4, j, length)) {
                    this.cursor = i2;
                }
                if (this.cursor < 0 && this.nextIndex < 0 && this.lastRet < 0) {
                    detach();
                } else {
                    this.prevCycles = i;
                    this.prevTakeIndex = i2;
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
            ReentrantLock reentrantLock = ArrayBlockingQueue.this.lock;
            reentrantLock.lock();
            try {
                if (!isDetached()) {
                    incorporateDequeues();
                }
                int i = this.lastRet;
                this.lastRet = -1;
                if (i >= 0) {
                    if (!isDetached()) {
                        ArrayBlockingQueue.this.removeAt(i);
                    } else {
                        E e = this.lastItem;
                        this.lastItem = null;
                        if (ArrayBlockingQueue.this.itemAt(i) == e) {
                            ArrayBlockingQueue.this.removeAt(i);
                        }
                    }
                } else if (i == -1) {
                    throw new IllegalStateException();
                }
                if (this.cursor < 0 && this.nextIndex < 0) {
                    detach();
                }
            } finally {
                reentrantLock.unlock();
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

        private int distance(int i, int i2, int i3) {
            int i4 = i - i2;
            if (i4 < 0) {
                return i4 + i3;
            }
            return i4;
        }

        boolean removedAt(int i) {
            if (isDetached()) {
                return true;
            }
            int i2 = ArrayBlockingQueue.this.takeIndex;
            int i3 = this.prevTakeIndex;
            int length = ArrayBlockingQueue.this.items.length;
            int i4 = (((ArrayBlockingQueue.this.itrs.cycles - this.prevCycles) + (i < i2 ? 1 : 0)) * length) + (i - i3);
            int iDec = this.cursor;
            if (iDec >= 0) {
                int iDistance = distance(iDec, i3, length);
                if (iDistance == i4) {
                    if (iDec == ArrayBlockingQueue.this.putIndex) {
                        iDec = -1;
                        this.cursor = -1;
                    }
                } else if (iDistance > i4) {
                    iDec = ArrayBlockingQueue.this.dec(iDec);
                    this.cursor = iDec;
                }
            }
            int iDec2 = this.lastRet;
            int iDec3 = -2;
            if (iDec2 >= 0) {
                int iDistance2 = distance(iDec2, i3, length);
                if (iDistance2 == i4) {
                    this.lastRet = -2;
                    iDec2 = -2;
                } else if (iDistance2 > i4) {
                    iDec2 = ArrayBlockingQueue.this.dec(iDec2);
                    this.lastRet = iDec2;
                }
            }
            int i5 = this.nextIndex;
            if (i5 >= 0) {
                int iDistance3 = distance(i5, i3, length);
                if (iDistance3 == i4) {
                    this.nextIndex = -2;
                } else if (iDistance3 > i4) {
                    iDec3 = ArrayBlockingQueue.this.dec(i5);
                    this.nextIndex = iDec3;
                }
            } else {
                iDec3 = i5;
            }
            if (iDec >= 0 || iDec3 >= 0 || iDec2 >= 0) {
                return false;
            }
            this.prevTakeIndex = -3;
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
