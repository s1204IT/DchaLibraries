package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import sun.misc.Unsafe;

public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, Serializable {
    private static final Unsafe UNSAFE;
    private static final long headOffset;
    private static final long serialVersionUID = 196745693267521676L;
    private static final long tailOffset;
    private volatile transient Node<E> head;
    private volatile transient Node<E> tail;

    private static class Node<E> {
        private static final Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;
        volatile E item;
        volatile Node<E> next;

        Node(E item) {
            UNSAFE.putObject(this, itemOffset, item);
        }

        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        static {
            try {
                UNSAFE = Unsafe.getUnsafe();
                itemOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    public ConcurrentLinkedQueue() {
        Node<E> node = new Node<>(null);
        this.tail = node;
        this.head = node;
    }

    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        Node<E> h = null;
        Node<E> t = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<>(e);
            if (h == null) {
                t = newNode;
                h = newNode;
            } else {
                t.lazySetNext(newNode);
                t = newNode;
            }
        }
        if (h == null) {
            t = new Node<>(null);
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
        if (h != p && casHead(h, p)) {
            h.lazySetNext(h);
        }
    }

    final Node<E> succ(Node<E> p) {
        Node<E> next = p.next;
        return p == next ? this.head : next;
    }

    @Override
    public boolean offer(E e) {
        checkNotNull(e);
        Node<E> newNode = new Node<>(e);
        Node<E> t = this.tail;
        Node<E> p = t;
        while (true) {
            Node<E> q = p.next;
            if (q == null) {
                if (p.casNext(null, newNode)) {
                    break;
                }
            } else if (p == q) {
                Node<E> t2 = this.tail;
                p = t != t2 ? t2 : this.head;
                t = t2;
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
                if (item != null && p.casItem(item, null)) {
                    if (p != h) {
                        Node<E> q = p.next;
                        if (q == null) {
                            q = p;
                        }
                        updateHead(h, q);
                        return item;
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
        int count = 0;
        Node<E> p = first();
        while (p != null && (p.item == null || (count = count + 1) != Integer.MAX_VALUE)) {
            p = succ(p);
        }
        return count;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
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

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        Node<E> pred = null;
        Node<E> p = first();
        while (p != null) {
            E item = p.item;
            if (item != null && o.equals(item) && p.casItem(item, null)) {
                Node<E> next = succ(p);
                if (pred != null && next != null) {
                    pred.casNext(p, next);
                }
                return true;
            }
            pred = p;
            p = succ(p);
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
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<>(e);
            if (beginningOfTheEnd == null) {
                last = newNode;
                beginningOfTheEnd = newNode;
            } else {
                last.lazySetNext(newNode);
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
                if (p.casNext(null, beginningOfTheEnd)) {
                    break;
                }
            } else if (p == q) {
                Node<E> t2 = this.tail;
                p = t != t2 ? t2 : this.head;
                t = t2;
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
            }
        }
        return true;
    }

    @Override
    public Object[] toArray() {
        ArrayList<E> al = new ArrayList<>();
        Node<E> p = first();
        while (p != null) {
            E item = p.item;
            if (item != null) {
                al.add(item);
            }
            p = succ(p);
        }
        return al.toArray();
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        int i;
        Node<E> nodeFirst = first();
        int i2 = 0;
        while (nodeFirst != null && i2 < tArr.length) {
            E e = nodeFirst.item;
            if (e != null) {
                i = i2 + 1;
                tArr[i2] = e;
            } else {
                i = i2;
            }
            nodeFirst = succ(nodeFirst);
            i2 = i;
        }
        if (nodeFirst == null) {
            if (i2 < tArr.length) {
                tArr[i2] = 0;
                return tArr;
            }
            return tArr;
        }
        ArrayList arrayList = new ArrayList();
        Node<E> nodeFirst2 = first();
        while (nodeFirst2 != null) {
            E e2 = nodeFirst2.item;
            if (e2 != null) {
                arrayList.add(e2);
            }
            nodeFirst2 = succ(nodeFirst2);
        }
        return (T[]) arrayList.toArray(tArr);
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
            advance();
        }

        private E advance() {
            Node<E> pred;
            Node<E> p;
            this.lastRet = this.nextNode;
            E x = this.nextItem;
            if (this.nextNode == null) {
                p = ConcurrentLinkedQueue.this.first();
                pred = null;
            } else {
                pred = this.nextNode;
                p = ConcurrentLinkedQueue.this.succ(this.nextNode);
            }
            while (true) {
                if (p == null) {
                    this.nextNode = null;
                    this.nextItem = null;
                    break;
                }
                E item = p.item;
                if (item != null) {
                    this.nextNode = p;
                    this.nextItem = item;
                    break;
                }
                Node<E> next = ConcurrentLinkedQueue.this.succ(p);
                if (pred != null && next != null) {
                    pred.casNext(p, next);
                }
                p = next;
            }
            return x;
        }

        @Override
        public boolean hasNext() {
            return this.nextNode != null;
        }

        @Override
        public E next() {
            if (this.nextNode == null) {
                throw new NoSuchElementException();
            }
            return (E) advance();
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

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        Node<E> h = null;
        Node<E> t = null;
        while (true) {
            Object item = s.readObject();
            if (item == null) {
                break;
            }
            Node<E> newNode = new Node<>(item);
            if (h == null) {
                t = newNode;
                h = newNode;
            } else {
                t.lazySetNext(newNode);
                t = newNode;
            }
        }
        if (h == null) {
            t = new Node<>(null);
            h = t;
        }
        this.head = h;
        this.tail = t;
    }

    private static void checkNotNull(Object v) {
        if (v == null) {
            throw new NullPointerException();
        }
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    static {
        try {
            UNSAFE = Unsafe.getUnsafe();
            headOffset = UNSAFE.objectFieldOffset(ConcurrentLinkedQueue.class.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(ConcurrentLinkedQueue.class.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
