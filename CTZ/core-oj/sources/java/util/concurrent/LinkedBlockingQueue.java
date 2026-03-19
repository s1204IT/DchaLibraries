package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
    private static final long serialVersionUID = -6903933977591709194L;
    private final int capacity;
    private final AtomicInteger count;
    transient Node<E> head;
    private transient Node<E> last;
    private final Condition notEmpty;
    private final Condition notFull;
    private final ReentrantLock putLock;
    private final ReentrantLock takeLock;

    static class Node<E> {
        E item;
        Node<E> next;

        Node(E e) {
            this.item = e;
        }
    }

    private void signalNotEmpty() {
        ReentrantLock reentrantLock = this.takeLock;
        reentrantLock.lock();
        try {
            this.notEmpty.signal();
        } finally {
            reentrantLock.unlock();
        }
    }

    private void signalNotFull() {
        ReentrantLock reentrantLock = this.putLock;
        reentrantLock.lock();
        try {
            this.notFull.signal();
        } finally {
            reentrantLock.unlock();
        }
    }

    private void enqueue(Node<E> node) {
        this.last.next = node;
        this.last = node;
    }

    private E dequeue() {
        Node<E> node = this.head;
        Node<E> node2 = node.next;
        node.next = node;
        this.head = node2;
        E e = node2.item;
        node2.item = null;
        return e;
    }

    void fullyLock() {
        this.putLock.lock();
        this.takeLock.lock();
    }

    void fullyUnlock() {
        this.takeLock.unlock();
        this.putLock.unlock();
    }

    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public LinkedBlockingQueue(int i) {
        this.count = new AtomicInteger();
        this.takeLock = new ReentrantLock();
        this.notEmpty = this.takeLock.newCondition();
        this.putLock = new ReentrantLock();
        this.notFull = this.putLock.newCondition();
        if (i <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = i;
        Node<E> node = new Node<>(null);
        this.head = node;
        this.last = node;
    }

    public LinkedBlockingQueue(Collection<? extends E> collection) {
        this(Integer.MAX_VALUE);
        ReentrantLock reentrantLock = this.putLock;
        reentrantLock.lock();
        int i = 0;
        try {
            for (E e : collection) {
                if (e == null) {
                    throw new NullPointerException();
                }
                if (i == this.capacity) {
                    throw new IllegalStateException("Queue full");
                }
                enqueue(new Node<>(e));
                i++;
            }
            this.count.set(i);
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public int size() {
        return this.count.get();
    }

    @Override
    public int remainingCapacity() {
        return this.capacity - this.count.get();
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        Node<E> node = new Node<>(e);
        ReentrantLock reentrantLock = this.putLock;
        AtomicInteger atomicInteger = this.count;
        reentrantLock.lockInterruptibly();
        while (atomicInteger.get() == this.capacity) {
            try {
                this.notFull.await();
            } finally {
                reentrantLock.unlock();
            }
        }
        enqueue(node);
        int andIncrement = atomicInteger.getAndIncrement();
        if (andIncrement + 1 < this.capacity) {
            this.notFull.signal();
        }
        if (andIncrement == 0) {
            signalNotEmpty();
        }
    }

    @Override
    public boolean offer(E e, long j, TimeUnit timeUnit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        long nanos = timeUnit.toNanos(j);
        ReentrantLock reentrantLock = this.putLock;
        AtomicInteger atomicInteger = this.count;
        reentrantLock.lockInterruptibly();
        while (atomicInteger.get() == this.capacity) {
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
        enqueue(new Node<>(e));
        int andIncrement = atomicInteger.getAndIncrement();
        if (andIncrement + 1 < this.capacity) {
            this.notFull.signal();
        }
        if (andIncrement == 0) {
            signalNotEmpty();
            return true;
        }
        return true;
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        AtomicInteger atomicInteger = this.count;
        if (atomicInteger.get() == this.capacity) {
            return false;
        }
        int andIncrement = -1;
        Node<E> node = new Node<>(e);
        ReentrantLock reentrantLock = this.putLock;
        reentrantLock.lock();
        try {
            if (atomicInteger.get() < this.capacity) {
                enqueue(node);
                andIncrement = atomicInteger.getAndIncrement();
                if (andIncrement + 1 < this.capacity) {
                    this.notFull.signal();
                }
            }
            if (andIncrement == 0) {
                signalNotEmpty();
            }
            return andIncrement >= 0;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        AtomicInteger atomicInteger = this.count;
        ReentrantLock reentrantLock = this.takeLock;
        reentrantLock.lockInterruptibly();
        while (atomicInteger.get() == 0) {
            try {
                this.notEmpty.await();
            } catch (Throwable th) {
                reentrantLock.unlock();
                throw th;
            }
        }
        E eDequeue = dequeue();
        int andDecrement = atomicInteger.getAndDecrement();
        if (andDecrement > 1) {
            this.notEmpty.signal();
        }
        reentrantLock.unlock();
        if (andDecrement == this.capacity) {
            signalNotFull();
        }
        return eDequeue;
    }

    @Override
    public E poll(long j, TimeUnit timeUnit) throws InterruptedException {
        long nanos = timeUnit.toNanos(j);
        AtomicInteger atomicInteger = this.count;
        ReentrantLock reentrantLock = this.takeLock;
        reentrantLock.lockInterruptibly();
        while (atomicInteger.get() == 0) {
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
        E eDequeue = dequeue();
        int andDecrement = atomicInteger.getAndDecrement();
        if (andDecrement > 1) {
            this.notEmpty.signal();
        }
        reentrantLock.unlock();
        if (andDecrement == this.capacity) {
            signalNotFull();
        }
        return eDequeue;
    }

    @Override
    public E poll() {
        AtomicInteger atomicInteger = this.count;
        E eDequeue = null;
        if (atomicInteger.get() == 0) {
            return null;
        }
        int andDecrement = -1;
        ReentrantLock reentrantLock = this.takeLock;
        reentrantLock.lock();
        try {
            if (atomicInteger.get() > 0) {
                eDequeue = dequeue();
                andDecrement = atomicInteger.getAndDecrement();
                if (andDecrement > 1) {
                    this.notEmpty.signal();
                }
            }
            reentrantLock.unlock();
            if (andDecrement == this.capacity) {
                signalNotFull();
            }
            return eDequeue;
        } catch (Throwable th) {
            reentrantLock.unlock();
            throw th;
        }
    }

    @Override
    public E peek() {
        if (this.count.get() == 0) {
            return null;
        }
        ReentrantLock reentrantLock = this.takeLock;
        reentrantLock.lock();
        try {
            return this.count.get() > 0 ? this.head.next.item : null;
        } finally {
            reentrantLock.unlock();
        }
    }

    void unlink(Node<E> node, Node<E> node2) {
        node.item = null;
        node2.next = node.next;
        if (this.last == node) {
            this.last = node2;
        }
        if (this.count.getAndDecrement() == this.capacity) {
            this.notFull.signal();
        }
    }

    @Override
    public boolean remove(Object obj) {
        if (obj == null) {
            return false;
        }
        fullyLock();
        try {
            Node<E> node = this.head;
            Node<E> node2 = node.next;
            while (true) {
                Node<E> node3 = node2;
                Node<E> node4 = node;
                node = node3;
                if (node == null) {
                    return false;
                }
                if (!obj.equals(node.item)) {
                    node2 = node.next;
                } else {
                    unlink(node, node4);
                    return true;
                }
            }
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }
        fullyLock();
        try {
            Node<E> node = this.head;
            do {
                node = node.next;
                if (node == null) {
                    return false;
                }
            } while (!obj.equals(node.item));
            return true;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public Object[] toArray() {
        fullyLock();
        try {
            Object[] objArr = new Object[this.count.get()];
            int i = 0;
            Node<E> node = this.head.next;
            while (node != null) {
                int i2 = i + 1;
                objArr[i] = node.item;
                node = node.next;
                i = i2;
            }
            return objArr;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        fullyLock();
        try {
            int i = this.count.get();
            if (tArr.length < i) {
                tArr = (T[]) ((Object[]) Array.newInstance(tArr.getClass().getComponentType(), i));
            }
            int i2 = 0;
            Node<E> node = this.head.next;
            while (node != null) {
                tArr[i2] = node.item;
                node = node.next;
                i2++;
            }
            if (tArr.length > i2) {
                tArr[i2] = null;
            }
            return tArr;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public String toString() {
        return Helpers.collectionToString(this);
    }

    @Override
    public void clear() {
        fullyLock();
        try {
            Node<E> node = this.head;
            while (true) {
                Node<E> node2 = node.next;
                if (node2 == null) {
                    break;
                }
                node.next = node;
                node2.item = null;
                node = node2;
            }
            this.head = this.last;
            if (this.count.getAndSet(0) == this.capacity) {
                this.notFull.signal();
            }
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> collection) {
        return drainTo(collection, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> collection, int i) {
        int andAdd;
        int i2;
        if (collection == null) {
            throw new NullPointerException();
        }
        if (collection == this) {
            throw new IllegalArgumentException();
        }
        boolean z = false;
        if (i <= 0) {
            return 0;
        }
        ReentrantLock reentrantLock = this.takeLock;
        reentrantLock.lock();
        try {
            int iMin = Math.min(i, this.count.get());
            Node<E> node = this.head;
            int i3 = 0;
            while (i3 < iMin) {
                try {
                    Node<E> node2 = node.next;
                    collection.add(node2.item);
                    node2.item = null;
                    node.next = node;
                    i3++;
                    node = node2;
                } finally {
                    if (i3 > 0) {
                        this.head = node;
                        if (this.count.getAndAdd(-i3) == this.capacity) {
                        }
                    }
                }
            }
            if (i3 > 0) {
                if (andAdd == i2) {
                    z = true;
                }
            }
            return iMin;
        } finally {
            reentrantLock.unlock();
            if (0 != 0) {
                signalNotFull();
            }
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        private Node<E> current;
        private E currentElement;
        private Node<E> lastRet;

        Itr() {
            LinkedBlockingQueue.this.fullyLock();
            try {
                this.current = LinkedBlockingQueue.this.head.next;
                if (this.current != null) {
                    this.currentElement = this.current.item;
                }
            } finally {
                LinkedBlockingQueue.this.fullyUnlock();
            }
        }

        @Override
        public boolean hasNext() {
            return this.current != null;
        }

        @Override
        public E next() {
            LinkedBlockingQueue.this.fullyLock();
            try {
                if (this.current == null) {
                    throw new NoSuchElementException();
                }
                this.lastRet = this.current;
                E e = null;
                Node<E> node = this.current;
                do {
                    Node<E> node2 = node.next;
                    if (node2 == node) {
                        node = LinkedBlockingQueue.this.head.next;
                    } else {
                        node = node2;
                    }
                    if (node == null) {
                        break;
                    }
                    e = node.item;
                } while (e == null);
                this.current = node;
                E e2 = this.currentElement;
                this.currentElement = e;
                return e2;
            } finally {
                LinkedBlockingQueue.this.fullyUnlock();
            }
        }

        @Override
        public void remove() {
            if (this.lastRet == null) {
                throw new IllegalStateException();
            }
            LinkedBlockingQueue.this.fullyLock();
            try {
                Node<E> node = this.lastRet;
                this.lastRet = null;
                Node<E> node2 = LinkedBlockingQueue.this.head;
                Node<E> node3 = node2.next;
                while (true) {
                    Node<E> node4 = node3;
                    Node<E> node5 = node2;
                    node2 = node4;
                    if (node2 == null) {
                        break;
                    } else if (node2 == node) {
                        break;
                    } else {
                        node3 = node2.next;
                    }
                }
            } finally {
                LinkedBlockingQueue.this.fullyUnlock();
            }
        }
    }

    static final class LBQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 33554432;
        int batch;
        Node<E> current;
        long est;
        boolean exhausted;
        final LinkedBlockingQueue<E> queue;

        LBQSpliterator(LinkedBlockingQueue<E> linkedBlockingQueue) {
            this.queue = linkedBlockingQueue;
            this.est = linkedBlockingQueue.size();
        }

        @Override
        public long estimateSize() {
            return this.est;
        }

        @Override
        public Spliterator<E> trySplit() {
            int i;
            Node<E> node;
            int i2;
            E e;
            LinkedBlockingQueue<E> linkedBlockingQueue = this.queue;
            int i3 = this.batch;
            int i4 = MAX_BATCH;
            if (i3 > 0) {
                if (i3 < MAX_BATCH) {
                    i4 = i3 + 1;
                }
            } else {
                i4 = 1;
            }
            if (this.exhausted) {
                return null;
            }
            Node<E> node2 = this.current;
            if ((node2 != null || (node2 = linkedBlockingQueue.head.next) != null) && node2.next != null) {
                Object[] objArr = new Object[i4];
                Node<E> node3 = this.current;
                linkedBlockingQueue.fullyLock();
                if (node3 != null) {
                    node = node3;
                    i2 = 0;
                    do {
                        e = node.item;
                        objArr[i2] = e;
                        if (e != null) {
                        }
                        node = node.next;
                        if (node != null) {
                        }
                    } while (i2 < i4);
                    i = i2;
                    node3 = node;
                } else {
                    try {
                        node3 = linkedBlockingQueue.head.next;
                        if (node3 != null) {
                            node = node3;
                            i2 = 0;
                            do {
                                e = node.item;
                                objArr[i2] = e;
                                if (e != null) {
                                    i2++;
                                }
                                node = node.next;
                                if (node != null) {
                                    break;
                                }
                            } while (i2 < i4);
                            i = i2;
                            node3 = node;
                        } else {
                            i = 0;
                        }
                    } catch (Throwable th) {
                        linkedBlockingQueue.fullyUnlock();
                        throw th;
                    }
                }
                linkedBlockingQueue.fullyUnlock();
                this.current = node3;
                if (node3 == null) {
                    this.est = 0L;
                    this.exhausted = true;
                } else {
                    long j = this.est - ((long) i);
                    this.est = j;
                    if (j < 0) {
                        this.est = 0L;
                    }
                }
                if (i > 0) {
                    this.batch = i;
                    return Spliterators.spliterator(objArr, 0, i, 4368);
                }
                return null;
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            LinkedBlockingQueue<E> linkedBlockingQueue = this.queue;
            if (!this.exhausted) {
                this.exhausted = true;
                Node<E> node = this.current;
                do {
                    E e = (E) null;
                    linkedBlockingQueue.fullyLock();
                    if (node == null) {
                        try {
                            node = linkedBlockingQueue.head.next;
                        } catch (Throwable th) {
                            linkedBlockingQueue.fullyUnlock();
                            throw th;
                        }
                    }
                    while (node != null) {
                        e = node.item;
                        node = node.next;
                        if (e != null) {
                            break;
                        }
                    }
                    linkedBlockingQueue.fullyUnlock();
                    if (e != null) {
                        consumer.accept(e);
                    }
                } while (node != null);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            LinkedBlockingQueue<E> linkedBlockingQueue = this.queue;
            if (!this.exhausted) {
                E e = (E) null;
                linkedBlockingQueue.fullyLock();
                try {
                    if (this.current == null) {
                        this.current = linkedBlockingQueue.head.next;
                    }
                    while (this.current != null) {
                        e = this.current.item;
                        this.current = this.current.next;
                        if (e != null) {
                            break;
                        }
                    }
                    linkedBlockingQueue.fullyUnlock();
                    if (this.current == null) {
                        this.exhausted = true;
                    }
                    if (e != null) {
                        consumer.accept(e);
                        return true;
                    }
                    return false;
                } catch (Throwable th) {
                    linkedBlockingQueue.fullyUnlock();
                    throw th;
                }
            }
            return false;
        }

        @Override
        public int characteristics() {
            return 4368;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new LBQSpliterator(this);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        fullyLock();
        try {
            objectOutputStream.defaultWriteObject();
            Node<E> node = this.head;
            while (true) {
                node = node.next;
                if (node != null) {
                    objectOutputStream.writeObject(node.item);
                } else {
                    objectOutputStream.writeObject(null);
                    return;
                }
            }
        } finally {
            fullyUnlock();
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        this.count.set(0);
        Node<E> node = new Node<>(null);
        this.head = node;
        this.last = node;
        while (true) {
            Object object = objectInputStream.readObject();
            if (object != null) {
                add(object);
            } else {
                return;
            }
        }
    }
}
