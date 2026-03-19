package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import sun.misc.Unsafe;

public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, Serializable {
    private static final long HEAD;
    private static final long ITEM;
    private static final long NEXT;
    private static final long TAIL;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = 196745693267521676L;
    volatile transient Node<E> head;
    private volatile transient Node<E> tail;

    private static class Node<E> {
        volatile E item;
        volatile Node<E> next;

        Node(Node node) {
            this();
        }

        private Node() {
        }
    }

    static <E> Node<E> newNode(E item) {
        Node<E> node = new Node<>(null);
        U.putObject(node, ITEM, item);
        return node;
    }

    static <E> boolean casItem(Node<E> node, E cmp, E val) {
        return U.compareAndSwapObject(node, ITEM, cmp, val);
    }

    static <E> void lazySetNext(Node<E> node, Node<E> val) {
        U.putOrderedObject(node, NEXT, val);
    }

    static <E> boolean casNext(Node<E> node, Node<E> cmp, Node<E> val) {
        return U.compareAndSwapObject(node, NEXT, cmp, val);
    }

    public ConcurrentLinkedQueue() {
        Node<E> nodeNewNode = newNode(null);
        this.tail = nodeNewNode;
        this.head = nodeNewNode;
    }

    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        Node<E> h = null;
        Node<E> t = null;
        Iterator e$iterator = c.iterator();
        while (e$iterator.hasNext()) {
            Node<E> newNode = newNode(Objects.requireNonNull(e$iterator.next()));
            if (h == null) {
                t = newNode;
                h = newNode;
            } else {
                lazySetNext(t, newNode);
                t = newNode;
            }
        }
        if (h == null) {
            t = newNode(null);
            h = t;
        }
        this.head = h;
        this.tail = t;
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    final void updateHead(Node<E> h, Node<E> p) {
        if (h == p || !casHead(h, p)) {
            return;
        }
        lazySetNext(h, h);
    }

    final Node<E> succ(Node<E> p) {
        Node<E> next = p.next;
        return p == next ? this.head : next;
    }

    @Override
    public boolean offer(E e) {
        Node<E> newNode = newNode(Objects.requireNonNull(e));
        Node<E> t = this.tail;
        Node<E> p = t;
        while (true) {
            Node<E> q = p.next;
            if (q == null) {
                if (casNext(p, null, newNode)) {
                    break;
                }
            } else if (p == q) {
                Node<E> t2 = this.tail;
                if (t != t2) {
                    p = t2;
                    t = t2;
                } else {
                    p = this.head;
                    t = t2;
                }
            } else {
                if (p != t) {
                    Node<E> t3 = this.tail;
                    if (t != t3) {
                        p = t3;
                        t = t3;
                    } else {
                        t = t3;
                    }
                }
                p = q;
            }
        }
        if (p != t) {
            casTail(t, newNode);
            return true;
        }
        return true;
    }

    @Override
    public E poll() {
        while (true) {
            Node<E> h = this.head;
            Node<E> p = h;
            while (true) {
                E item = p.item;
                if (item != null && casItem(p, item, null)) {
                    if (p != h) {
                        Node<E> q = p.next;
                        if (q == null) {
                            q = p;
                        }
                        updateHead(h, q);
                    }
                    return item;
                }
                Node<E> q2 = p.next;
                if (q2 == null) {
                    updateHead(h, p);
                    return null;
                }
                if (p != q2) {
                    p = q2;
                }
            }
        }
    }

    @Override
    public E peek() {
        Node<E> h;
        Node<E> p;
        E item;
        Node<E> q;
        loop0: while (true) {
            h = this.head;
            p = h;
            while (true) {
                item = p.item;
                if (item != null || (q = p.next) == null) {
                    break loop0;
                }
                if (p != q) {
                    p = q;
                }
            }
        }
        updateHead(h, p);
        return item;
    }

    Node<E> first() {
        Node<E> h;
        Node<E> p;
        boolean hasItem;
        Node<E> q;
        loop0: while (true) {
            h = this.head;
            p = h;
            while (true) {
                hasItem = p.item != null;
                if (hasItem || (q = p.next) == null) {
                    break loop0;
                }
                if (p != q) {
                    p = q;
                }
            }
        }
        updateHead(h, p);
        if (hasItem) {
            return p;
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return first() == null;
    }

    @Override
    public int size() {
        int count;
        loop0: while (true) {
            count = 0;
            Node<E> p = first();
            Node<E> p2 = p;
            while (p2 != null && (p2.item == null || (count = count + 1) != Integer.MAX_VALUE)) {
                Node<E> p3 = p2.next;
                if (p2 != p3) {
                    p2 = p3;
                }
            }
        }
        return count;
    }

    @Override
    public boolean contains(Object o) {
        if (o != null) {
            Node<E> p = first();
            while (p != null) {
                E item = p.item;
                if (item == null || !o.equals(item)) {
                    p = succ(p);
                } else {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        Node<E> next;
        if (o != null) {
            Node<E> pred = null;
            Node<E> p = first();
            while (p != null) {
                boolean removed = false;
                E item = p.item;
                if (item != null) {
                    if (!o.equals(item)) {
                        next = succ(p);
                    } else {
                        removed = casItem(p, item, null);
                        next = succ(p);
                        if (pred != null) {
                            casNext(pred, p, next);
                        }
                        if (!removed) {
                        }
                    }
                } else {
                    next = succ(p);
                    if (pred != null && next != null) {
                        casNext(pred, p, next);
                    }
                    if (!removed) {
                        return true;
                    }
                }
                pred = p;
                p = next;
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == this) {
            throw new IllegalArgumentException();
        }
        Node<E> beginningOfTheEnd = null;
        Node<E> last = null;
        Iterator e$iterator = c.iterator();
        while (e$iterator.hasNext()) {
            Node<E> newNode = newNode(Objects.requireNonNull(e$iterator.next()));
            if (beginningOfTheEnd == null) {
                last = newNode;
                beginningOfTheEnd = newNode;
            } else {
                lazySetNext(last, newNode);
                last = newNode;
            }
        }
        if (beginningOfTheEnd == null) {
            return false;
        }
        Node<E> t = this.tail;
        Node<E> p = t;
        while (true) {
            Node<E> q = p.next;
            if (q == null) {
                if (casNext(p, null, beginningOfTheEnd)) {
                    break;
                }
            } else if (p == q) {
                Node<E> t2 = this.tail;
                if (t != t2) {
                    p = t2;
                    t = t2;
                } else {
                    p = this.head;
                    t = t2;
                }
            } else {
                if (p != t) {
                    Node<E> t3 = this.tail;
                    if (t != t3) {
                        p = t3;
                        t = t3;
                    } else {
                        t = t3;
                    }
                }
                p = q;
            }
        }
        if (!casTail(t, last)) {
            Node<E> t4 = this.tail;
            if (last.next == null) {
                casTail(t4, last);
                return true;
            }
            return true;
        }
        return true;
    }

    @Override
    public String toString() {
        int charLength;
        int size;
        int size2;
        String[] strArr = null;
        loop0: while (true) {
            charLength = 0;
            Node<E> p = first();
            Node<E> p2 = p;
            size = 0;
            while (p2 != null) {
                E item = p2.item;
                if (item != null) {
                    if (strArr == null) {
                        strArr = new String[4];
                    } else if (size == strArr.length) {
                        strArr = (String[]) Arrays.copyOf(strArr, size * 2);
                    }
                    String s = item.toString();
                    size2 = size + 1;
                    strArr[size] = s;
                    charLength += s.length();
                } else {
                    size2 = size;
                }
                Node<E> p3 = p2.next;
                if (p2 != p3) {
                    p2 = p3;
                    size = size2;
                }
            }
        }
        if (size == 0) {
            return "[]";
        }
        return Helpers.toString(strArr, size, charLength);
    }

    private Object[] toArrayInternal(Object[] a) {
        int size;
        int size2;
        Object[] x = a;
        loop0: while (true) {
            Node<E> p = first();
            Node<E> p2 = p;
            size = 0;
            while (p2 != null) {
                E item = p2.item;
                if (item != null) {
                    if (x == null) {
                        x = new Object[4];
                    } else if (size == x.length) {
                        x = Arrays.copyOf(x, (size + 4) * 2);
                    }
                    size2 = size + 1;
                    x[size] = item;
                } else {
                    size2 = size;
                }
                Node<E> p3 = p2.next;
                if (p2 != p3) {
                    p2 = p3;
                    size = size2;
                }
            }
        }
        if (x == null) {
            return new Object[0];
        }
        if (a == null || size > a.length) {
            return size == x.length ? x : Arrays.copyOf(x, size);
        }
        if (a != x) {
            System.arraycopy(x, 0, a, 0, size);
        }
        if (size < a.length) {
            a[size] = null;
        }
        return a;
    }

    @Override
    public Object[] toArray() {
        return toArrayInternal(null);
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        if (tArr == null) {
            throw new NullPointerException();
        }
        return (T[]) toArrayInternal(tArr);
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        private Node<E> lastRet;
        private E nextItem;
        private Node<E> nextNode;

        Itr() {
            Node<E> h;
            Node<E> p;
            loop0: while (true) {
                h = ConcurrentLinkedQueue.this.head;
                p = h;
                while (true) {
                    E item = p.item;
                    if (item != null) {
                        this.nextNode = p;
                        this.nextItem = item;
                        break loop0;
                    } else {
                        Node<E> q = p.next;
                        if (q == null) {
                            break loop0;
                        } else {
                            p = p != q ? q : p;
                        }
                    }
                }
            }
            ConcurrentLinkedQueue.this.updateHead(h, p);
        }

        @Override
        public boolean hasNext() {
            return this.nextItem != null;
        }

        @Override
        public E next() {
            Node<E> pred = this.nextNode;
            if (pred == null) {
                throw new NoSuchElementException();
            }
            this.lastRet = pred;
            E item = null;
            Node<E> p = ConcurrentLinkedQueue.this.succ(pred);
            while (p != null) {
                item = p.item;
                if (item != null) {
                    break;
                }
                Node<E> q = ConcurrentLinkedQueue.this.succ(p);
                if (q != null) {
                    ConcurrentLinkedQueue.casNext(pred, p, q);
                }
                p = q;
            }
            this.nextNode = p;
            E x = this.nextItem;
            this.nextItem = item;
            return x;
        }

        @Override
        public void remove() {
            Node<E> l = this.lastRet;
            if (l == null) {
                throw new IllegalStateException();
            }
            l.item = null;
            this.lastRet = null;
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        Node<E> p = first();
        while (p != null) {
            Object item = p.item;
            if (item != null) {
                s.writeObject(item);
            }
            p = succ(p);
        }
        s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        s.defaultReadObject();
        Node<E> h = null;
        Node<E> t = null;
        while (true) {
            Object item = s.readObject();
            if (item == null) {
                break;
            }
            Node<E> newNode = newNode(item);
            if (h == null) {
                t = newNode;
                h = newNode;
            } else {
                lazySetNext(t, newNode);
                t = newNode;
            }
        }
        if (h == null) {
            t = newNode(null);
            h = t;
        }
        this.head = h;
        this.tail = t;
    }

    static final class CLQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 33554432;
        int batch;
        Node<E> current;
        boolean exhausted;
        final ConcurrentLinkedQueue<E> queue;

        CLQSpliterator(ConcurrentLinkedQueue<E> queue) {
            this.queue = queue;
        }

        @Override
        public Spliterator<E> trySplit() {
            int n;
            Node<E> p;
            ConcurrentLinkedQueue<E> q = this.queue;
            int b = this.batch;
            if (b <= 0) {
                n = 1;
            } else {
                n = b >= MAX_BATCH ? MAX_BATCH : b + 1;
            }
            if (!this.exhausted && (((p = this.current) != null || (p = q.first()) != null) && p.next != null)) {
                Object[] a = new Object[n];
                int i = 0;
                do {
                    E e = p.item;
                    a[i] = e;
                    if (e != null) {
                        i++;
                    }
                    Node<E> p2 = p.next;
                    p = p == p2 ? q.first() : p2;
                    if (p == null) {
                        break;
                    }
                } while (i < n);
                this.current = p;
                if (p == null) {
                    this.exhausted = true;
                }
                if (i > 0) {
                    this.batch = i;
                    return Spliterators.spliterator(a, 0, i, 4368);
                }
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> consumer) {
            if (consumer == null) {
                throw new NullPointerException();
            }
            ConcurrentLinkedQueue<E> concurrentLinkedQueue = this.queue;
            if (this.exhausted) {
                return;
            }
            Node<E> nodeFirst = this.current;
            if (nodeFirst == null && (nodeFirst = concurrentLinkedQueue.first()) == null) {
                return;
            }
            this.exhausted = true;
            do {
                E e = nodeFirst.item;
                Node<E> node = nodeFirst.next;
                nodeFirst = nodeFirst == node ? concurrentLinkedQueue.first() : node;
                if (e != null) {
                    consumer.accept(e);
                }
            } while (nodeFirst != null);
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            E e;
            if (consumer == null) {
                throw new NullPointerException();
            }
            ConcurrentLinkedQueue<E> concurrentLinkedQueue = this.queue;
            if (!this.exhausted) {
                Node<E> nodeFirst = this.current;
                if (nodeFirst != null || (nodeFirst = concurrentLinkedQueue.first()) != null) {
                    do {
                        e = nodeFirst.item;
                        Node<E> node = nodeFirst.next;
                        nodeFirst = nodeFirst == node ? concurrentLinkedQueue.first() : node;
                        if (e != null) {
                            break;
                        }
                    } while (nodeFirst != null);
                    this.current = nodeFirst;
                    if (nodeFirst == null) {
                        this.exhausted = true;
                    }
                    if (e != null) {
                        consumer.accept(e);
                        return true;
                    }
                    return false;
                }
                return false;
            }
            return false;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return 4368;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new CLQSpliterator(this);
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return U.compareAndSwapObject(this, TAIL, cmp, val);
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return U.compareAndSwapObject(this, HEAD, cmp, val);
    }

    static {
        try {
            HEAD = U.objectFieldOffset(ConcurrentLinkedQueue.class.getDeclaredField("head"));
            TAIL = U.objectFieldOffset(ConcurrentLinkedQueue.class.getDeclaredField("tail"));
            ITEM = U.objectFieldOffset(Node.class.getDeclaredField("item"));
            NEXT = U.objectFieldOffset(Node.class.getDeclaredField("next"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
