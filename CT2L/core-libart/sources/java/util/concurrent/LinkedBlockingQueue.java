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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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

        Node(E x) {
            this.item = x;
        }
    }

    private void signalNotEmpty() {
        ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            this.notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    private void signalNotFull() {
        ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            this.notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    private void enqueue(Node<E> node) {
        this.last.next = node;
        this.last = node;
    }

    private E dequeue() {
        Node<E> h = this.head;
        Node<E> first = h.next;
        h.next = h;
        this.head = first;
        E x = first.item;
        first.item = null;
        return x;
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

    public LinkedBlockingQueue(int capacity) {
        this.count = new AtomicInteger();
        this.takeLock = new ReentrantLock();
        this.notEmpty = this.takeLock.newCondition();
        this.putLock = new ReentrantLock();
        this.notFull = this.putLock.newCondition();
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        Node<E> node = new Node<>(null);
        this.head = node;
        this.last = node;
    }

    public LinkedBlockingQueue(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        ReentrantLock putLock = this.putLock;
        putLock.lock();
        int n = 0;
        try {
            for (E e : c) {
                if (e == null) {
                    throw new NullPointerException();
                }
                if (n == this.capacity) {
                    throw new IllegalStateException("Queue full");
                }
                enqueue(new Node<>(e));
                n++;
            }
            this.count.set(n);
        } finally {
            putLock.unlock();
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
        ReentrantLock putLock = this.putLock;
        AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        while (count.get() == this.capacity) {
            try {
                this.notFull.await();
            } finally {
                putLock.unlock();
            }
        }
        enqueue(node);
        int c = count.getAndIncrement();
        if (c + 1 < this.capacity) {
            this.notFull.signal();
        }
        if (c == 0) {
            signalNotEmpty();
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        long nanos = unit.toNanos(timeout);
        ReentrantLock putLock = this.putLock;
        AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        while (count.get() == this.capacity) {
            try {
                if (nanos > 0) {
                    nanos = this.notFull.awaitNanos(nanos);
                } else {
                    return false;
                }
            } finally {
                putLock.unlock();
            }
        }
        enqueue(new Node<>(e));
        int c = count.getAndIncrement();
        if (c + 1 < this.capacity) {
            this.notFull.signal();
        }
        if (c == 0) {
            signalNotEmpty();
        }
        return true;
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        AtomicInteger count = this.count;
        if (count.get() == this.capacity) {
            return false;
        }
        int c = -1;
        Node<E> node = new Node<>(e);
        ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            if (count.get() < this.capacity) {
                enqueue(node);
                c = count.getAndIncrement();
                if (c + 1 < this.capacity) {
                    this.notFull.signal();
                }
            }
            if (c == 0) {
                signalNotEmpty();
            }
            return c >= 0;
        } finally {
            putLock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        AtomicInteger count = this.count;
        ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        while (count.get() == 0) {
            try {
                this.notEmpty.await();
            } catch (Throwable th) {
                takeLock.unlock();
                throw th;
            }
        }
        E x = dequeue();
        int c = count.getAndDecrement();
        if (c > 1) {
            this.notEmpty.signal();
        }
        takeLock.unlock();
        if (c == this.capacity) {
            signalNotFull();
        }
        return x;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        AtomicInteger count = this.count;
        ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        while (count.get() == 0) {
            try {
                if (nanos > 0) {
                    nanos = this.notEmpty.awaitNanos(nanos);
                } else {
                    return null;
                }
            } finally {
                takeLock.unlock();
            }
        }
        E x = dequeue();
        int c = count.getAndDecrement();
        if (c > 1) {
            this.notEmpty.signal();
        }
        takeLock.unlock();
        if (c == this.capacity) {
            signalNotFull();
            return x;
        }
        return x;
    }

    @Override
    public E poll() {
        AtomicInteger count = this.count;
        if (count.get() == 0) {
            return null;
        }
        E x = null;
        int c = -1;
        ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            if (count.get() > 0) {
                x = dequeue();
                c = count.getAndDecrement();
                if (c > 1) {
                    this.notEmpty.signal();
                }
            }
            takeLock.unlock();
            if (c == this.capacity) {
                signalNotFull();
                return x;
            }
            return x;
        } catch (Throwable th) {
            takeLock.unlock();
            throw th;
        }
    }

    @Override
    public E peek() {
        E e = null;
        if (this.count.get() != 0) {
            ReentrantLock takeLock = this.takeLock;
            takeLock.lock();
            try {
                Node<E> first = this.head.next;
                if (first != null) {
                    e = first.item;
                }
            } finally {
                takeLock.unlock();
            }
        }
        return e;
    }

    void unlink(Node<E> p, Node<E> trail) {
        p.item = null;
        trail.next = p.next;
        if (this.last == p) {
            this.last = trail;
        }
        if (this.count.getAndDecrement() == this.capacity) {
            this.notFull.signal();
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        fullyLock();
        try {
            Node<E> trail = this.head;
            for (Node<E> p = trail.next; p != null; p = p.next) {
                if (!o.equals(p.item)) {
                    trail = p;
                } else {
                    unlink(p, trail);
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        fullyLock();
        try {
            for (Node<E> p = this.head.next; p != null; p = p.next) {
                if (o.equals(p.item)) {
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public Object[] toArray() {
        fullyLock();
        try {
            int size = this.count.get();
            Object[] a = new Object[size];
            Node<E> p = this.head.next;
            int k = 0;
            while (p != null) {
                int k2 = k + 1;
                a[k] = p.item;
                p = p.next;
                k = k2;
            }
            return a;
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
            Node<E> node = this.head.next;
            int i2 = 0;
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
        String string;
        fullyLock();
        try {
            Node<E> p = this.head.next;
            if (p == null) {
                string = "[]";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                while (true) {
                    Object obj = p.item;
                    if (obj == this) {
                        obj = "(this Collection)";
                    }
                    sb.append(obj);
                    p = p.next;
                    if (p == null) {
                        break;
                    }
                    sb.append(',').append(' ');
                }
                string = sb.append(']').toString();
            }
            return string;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public void clear() {
        fullyLock();
        try {
            Node<E> h = this.head;
            while (true) {
                Node<E> p = h.next;
                if (p == null) {
                    break;
                }
                h.next = h;
                p.item = null;
                h = p;
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
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
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
        ReentrantLock reentrantLock = this.takeLock;
        reentrantLock.lock();
        try {
            int iMin = Math.min(i, this.count.get());
            Node<E> node = this.head;
            int i2 = 0;
            while (i2 < iMin) {
                try {
                    Node<E> node2 = node.next;
                    collection.add(node2.item);
                    node2.item = null;
                    node.next = node;
                    node = node2;
                    i2++;
                } finally {
                    if (i2 > 0) {
                        this.head = node;
                        if (this.count.getAndAdd(-i2) == this.capacity) {
                        }
                    }
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

        private Node<E> nextNode(Node<E> p) {
            while (true) {
                Node<E> s = p.next;
                if (s == p) {
                    return LinkedBlockingQueue.this.head.next;
                }
                if (s == null || s.item != null) {
                    return s;
                }
                p = s;
            }
        }

        @Override
        public E next() {
            LinkedBlockingQueue.this.fullyLock();
            try {
                if (this.current == null) {
                    throw new NoSuchElementException();
                }
                E x = this.currentElement;
                this.lastRet = this.current;
                this.current = nextNode(this.current);
                this.currentElement = this.current == null ? null : this.current.item;
                return x;
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
                Node<E> trail = LinkedBlockingQueue.this.head;
                Node<E> p = trail.next;
                while (true) {
                    if (p == null) {
                        break;
                    }
                    if (p == node) {
                        break;
                    }
                    trail = p;
                    p = p.next;
                }
            } finally {
                LinkedBlockingQueue.this.fullyUnlock();
            }
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        fullyLock();
        try {
            s.defaultWriteObject();
            for (Node<E> p = this.head.next; p != null; p = p.next) {
                s.writeObject(p.item);
            }
            s.writeObject(null);
        } finally {
            fullyUnlock();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.count.set(0);
        Node<E> node = new Node<>(null);
        this.head = node;
        this.last = node;
        while (true) {
            Object object = s.readObject();
            if (object != null) {
                add(object);
            } else {
                return;
            }
        }
    }
}
