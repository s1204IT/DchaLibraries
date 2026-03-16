package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import sun.misc.Unsafe;

public class ConcurrentLinkedDeque<E> extends AbstractCollection<E> implements Deque<E>, Serializable {
    private static final int HOPS = 2;
    private static final Node<Object> NEXT_TERMINATOR;
    private static final Node<Object> PREV_TERMINATOR = new Node<>();
    private static final Unsafe UNSAFE;
    private static final long headOffset;
    private static final long serialVersionUID = 876323262645176354L;
    private static final long tailOffset;
    private volatile transient Node<E> head;
    private volatile transient Node<E> tail;

    Node<E> prevTerminator() {
        return (Node<E>) PREV_TERMINATOR;
    }

    Node<E> nextTerminator() {
        return (Node<E>) NEXT_TERMINATOR;
    }

    static final class Node<E> {
        private static final Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;
        private static final long prevOffset;
        volatile E item;
        volatile Node<E> next;
        volatile Node<E> prev;

        Node() {
        }

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

        void lazySetPrev(Node<E> val) {
            UNSAFE.putOrderedObject(this, prevOffset, val);
        }

        boolean casPrev(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, prevOffset, cmp, val);
        }

        static {
            try {
                UNSAFE = Unsafe.getUnsafe();
                prevOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("prev"));
                itemOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    private void linkFirst(E e) {
        Node<E> h;
        Node<E> p;
        checkNotNull(e);
        Node<E> newNode = new Node<>(e);
        loop0: while (true) {
            h = this.head;
            p = h;
            while (true) {
                Node<E> q = p.prev;
                if (q != null) {
                    p = q;
                    Node<E> q2 = q.prev;
                    if (q2 != null) {
                        Node<E> h2 = this.head;
                        p = h != h2 ? h2 : q2;
                        h = h2;
                    }
                }
                if (p.next != p) {
                    newNode.lazySetNext(p);
                    if (p.casPrev(null, newNode)) {
                        break loop0;
                    }
                }
            }
        }
        if (p != h) {
            casHead(h, newNode);
        }
    }

    private void linkLast(E e) {
        Node<E> t;
        Node<E> p;
        checkNotNull(e);
        Node<E> newNode = new Node<>(e);
        loop0: while (true) {
            t = this.tail;
            p = t;
            while (true) {
                Node<E> q = p.next;
                if (q != null) {
                    p = q;
                    Node<E> q2 = q.next;
                    if (q2 != null) {
                        Node<E> t2 = this.tail;
                        p = t != t2 ? t2 : q2;
                        t = t2;
                    }
                }
                if (p.prev != p) {
                    newNode.lazySetPrev(p);
                    if (p.casNext(null, newNode)) {
                        break loop0;
                    }
                }
            }
        }
        if (p != t) {
            casTail(t, newNode);
        }
    }

    void unlink(Node<E> x) {
        Node<E> activePred;
        boolean isFirst;
        Node<E> prev = x.prev;
        Node<E> next = x.next;
        if (prev == null) {
            unlinkFirst(x, next);
            return;
        }
        if (next == null) {
            unlinkLast(x, prev);
            return;
        }
        int hops = 1;
        Node<E> p = prev;
        while (true) {
            if (p.item != null) {
                activePred = p;
                isFirst = false;
                break;
            }
            Node<E> q = p.prev;
            if (q == null) {
                if (p.next != p) {
                    activePred = p;
                    isFirst = true;
                } else {
                    return;
                }
            } else if (p != q) {
                p = q;
                hops++;
            } else {
                return;
            }
        }
    }

    private void unlinkFirst(Node<E> first, Node<E> next) {
        Node<E> q;
        Node<E> o = null;
        Node<E> p = next;
        while (p.item == null && (q = p.next) != null) {
            if (p == q) {
                return;
            }
            o = p;
            p = q;
        }
        if (o != null && p.prev != p && first.casNext(next, p)) {
            skipDeletedPredecessors(p);
            if (first.prev == null) {
                if ((p.next == null || p.item != null) && p.prev == first) {
                    updateHead();
                    updateTail();
                    o.lazySetNext(o);
                    o.lazySetPrev(prevTerminator());
                }
            }
        }
    }

    private void unlinkLast(Node<E> last, Node<E> prev) {
        Node<E> q;
        Node<E> o = null;
        Node<E> p = prev;
        while (p.item == null && (q = p.prev) != null) {
            if (p == q) {
                return;
            }
            o = p;
            p = q;
        }
        if (o != null && p.next != p && last.casPrev(prev, p)) {
            skipDeletedSuccessors(p);
            if (last.next == null) {
                if ((p.prev == null || p.item != null) && p.next == last) {
                    updateHead();
                    updateTail();
                    o.lazySetPrev(o);
                    o.lazySetNext(nextTerminator());
                }
            }
        }
    }

    private final void updateHead() {
        Node<E> p;
        while (true) {
            Node<E> h = this.head;
            if (h.item == null && (p = h.prev) != null) {
                while (true) {
                    Node<E> q = p.prev;
                    if (q == null) {
                        break;
                    }
                    p = q;
                    Node<E> q2 = q.prev;
                    if (q2 == null) {
                        break;
                    } else if (h == this.head) {
                        p = q2;
                    }
                }
            } else {
                return;
            }
        }
    }

    private final void updateTail() {
        Node<E> p;
        while (true) {
            Node<E> t = this.tail;
            if (t.item == null && (p = t.next) != null) {
                while (true) {
                    Node<E> q = p.next;
                    if (q == null) {
                        break;
                    }
                    p = q;
                    Node<E> q2 = q.next;
                    if (q2 == null) {
                        break;
                    } else if (t == this.tail) {
                        p = q2;
                    }
                }
            } else {
                return;
            }
        }
    }

    private void skipDeletedPredecessors(Node<E> x) {
        while (true) {
            Node<E> prev = x.prev;
            Node<E> p = prev;
            while (true) {
                if (p.item != null) {
                    break;
                }
                Node<E> q = p.prev;
                if (q == null) {
                    if (p.next != p) {
                        break;
                    }
                } else if (p == q) {
                    break;
                } else {
                    p = q;
                }
            }
            if (prev == p || x.casPrev(prev, p)) {
                return;
            }
            if (x.item == null && x.next != null) {
                return;
            }
        }
    }

    private void skipDeletedSuccessors(Node<E> x) {
        while (true) {
            Node<E> next = x.next;
            Node<E> p = next;
            while (true) {
                if (p.item != null) {
                    break;
                }
                Node<E> q = p.next;
                if (q == null) {
                    if (p.prev != p) {
                        break;
                    }
                } else if (p == q) {
                    break;
                } else {
                    p = q;
                }
            }
            if (next == p || x.casNext(next, p)) {
                return;
            }
            if (x.item == null && x.prev != null) {
                return;
            }
        }
    }

    final Node<E> succ(Node<E> p) {
        Node<E> q = p.next;
        return p == q ? first() : q;
    }

    final Node<E> pred(Node<E> p) {
        Node<E> q = p.prev;
        return p == q ? last() : q;
    }

    Node<E> first() {
        Node<E> h;
        Node<E> p;
        do {
            h = this.head;
            p = h;
            while (true) {
                Node<E> q = p.prev;
                if (q == null) {
                    break;
                }
                p = q;
                Node<E> q2 = q.prev;
                if (q2 == null) {
                    break;
                }
                Node<E> h2 = this.head;
                p = h != h2 ? h2 : q2;
                h = h2;
            }
            if (p == h) {
                break;
            }
        } while (!casHead(h, p));
        return p;
    }

    Node<E> last() {
        Node<E> t;
        Node<E> p;
        do {
            t = this.tail;
            p = t;
            while (true) {
                Node<E> q = p.next;
                if (q == null) {
                    break;
                }
                p = q;
                Node<E> q2 = q.next;
                if (q2 == null) {
                    break;
                }
                Node<E> t2 = this.tail;
                p = t != t2 ? t2 : q2;
                t = t2;
            }
            if (p == t) {
                break;
            }
        } while (!casTail(t, p));
        return p;
    }

    private static void checkNotNull(Object v) {
        if (v == null) {
            throw new NullPointerException();
        }
    }

    private E screenNullResult(E v) {
        if (v == null) {
            throw new NoSuchElementException();
        }
        return v;
    }

    private ArrayList<E> toArrayList() {
        ArrayList<E> list = new ArrayList<>();
        Node<E> p = first();
        while (p != null) {
            E item = p.item;
            if (item != null) {
                list.add(item);
            }
            p = succ(p);
        }
        return list;
    }

    public ConcurrentLinkedDeque() {
        Node<E> node = new Node<>(null);
        this.tail = node;
        this.head = node;
    }

    public ConcurrentLinkedDeque(Collection<? extends E> c) {
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
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        initHeadTail(h, t);
    }

    private void initHeadTail(Node<E> h, Node<E> t) {
        if (h == t) {
            if (h == null) {
                t = new Node<>(null);
                h = t;
            } else {
                Node<E> newNode = new Node<>(null);
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        this.head = h;
        this.tail = t;
    }

    @Override
    public void addFirst(E e) {
        linkFirst(e);
    }

    @Override
    public void addLast(E e) {
        linkLast(e);
    }

    @Override
    public boolean offerFirst(E e) {
        linkFirst(e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        linkLast(e);
        return true;
    }

    @Override
    public E peekFirst() {
        Node<E> p = first();
        while (p != null) {
            E item = p.item;
            if (item == null) {
                p = succ(p);
            } else {
                return item;
            }
        }
        return null;
    }

    @Override
    public E peekLast() {
        Node<E> p = last();
        while (p != null) {
            E item = p.item;
            if (item == null) {
                p = pred(p);
            } else {
                return item;
            }
        }
        return null;
    }

    @Override
    public E getFirst() {
        return screenNullResult(peekFirst());
    }

    @Override
    public E getLast() {
        return screenNullResult(peekLast());
    }

    @Override
    public E pollFirst() {
        Node<E> p = first();
        while (p != null) {
            E item = p.item;
            if (item == null || !p.casItem(item, null)) {
                p = succ(p);
            } else {
                unlink(p);
                return item;
            }
        }
        return null;
    }

    @Override
    public E pollLast() {
        Node<E> p = last();
        while (p != null) {
            E item = p.item;
            if (item == null || !p.casItem(item, null)) {
                p = pred(p);
            } else {
                unlink(p);
                return item;
            }
        }
        return null;
    }

    @Override
    public E removeFirst() {
        return screenNullResult(pollFirst());
    }

    @Override
    public E removeLast() {
        return screenNullResult(pollLast());
    }

    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }

    @Override
    public boolean add(E e) {
        return offerLast(e);
    }

    @Override
    public E poll() {
        return pollFirst();
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public E peek() {
        return peekFirst();
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        checkNotNull(o);
        Node<E> p = first();
        while (p != null) {
            E item = p.item;
            if (item == null || !o.equals(item) || !p.casItem(item, null)) {
                p = succ(p);
            } else {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        checkNotNull(o);
        Node<E> p = last();
        while (p != null) {
            E item = p.item;
            if (item == null || !o.equals(item) || !p.casItem(item, null)) {
                p = pred(p);
            } else {
                unlink(p);
                return true;
            }
        }
        return false;
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
    public boolean isEmpty() {
        return peekFirst() == null;
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
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        Node<E> t;
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
                newNode.lazySetPrev(last);
                last = newNode;
            }
        }
        if (beginningOfTheEnd == null) {
            return false;
        }
        loop1: while (true) {
            t = this.tail;
            Node<E> p = t;
            while (true) {
                Node<E> q = p.next;
                if (q != null) {
                    p = q;
                    Node<E> q2 = q.next;
                    if (q2 != null) {
                        Node<E> t2 = this.tail;
                        p = t != t2 ? t2 : q2;
                        t = t2;
                    }
                }
                if (p.prev == p) {
                    break;
                }
                beginningOfTheEnd.lazySetPrev(p);
                if (p.casNext(null, beginningOfTheEnd)) {
                    break loop1;
                }
            }
        }
        if (!casTail(t, last)) {
            Node<E> t3 = this.tail;
            if (last.next == null) {
                casTail(t3, last);
            }
        }
        return true;
    }

    @Override
    public void clear() {
        while (pollFirst() != null) {
        }
    }

    @Override
    public Object[] toArray() {
        return toArrayList().toArray();
    }

    @Override
    public <T> T[] toArray(T[] tArr) {
        return (T[]) toArrayList().toArray(tArr);
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DescendingItr();
    }

    private abstract class AbstractItr implements Iterator<E> {
        private Node<E> lastRet;
        private E nextItem;
        private Node<E> nextNode;

        abstract Node<E> nextNode(Node<E> node);

        abstract Node<E> startNode();

        AbstractItr() {
            advance();
        }

        private void advance() {
            this.lastRet = this.nextNode;
            Node<E> p = this.nextNode == null ? startNode() : nextNode(this.nextNode);
            while (p != null) {
                E item = p.item;
                if (item == null) {
                    p = nextNode(p);
                } else {
                    this.nextNode = p;
                    this.nextItem = item;
                    return;
                }
            }
            this.nextNode = null;
            this.nextItem = null;
        }

        @Override
        public boolean hasNext() {
            return this.nextItem != null;
        }

        @Override
        public E next() {
            E item = this.nextItem;
            if (item == null) {
                throw new NoSuchElementException();
            }
            advance();
            return item;
        }

        @Override
        public void remove() {
            Node<E> l = this.lastRet;
            if (l == null) {
                throw new IllegalStateException();
            }
            l.item = null;
            ConcurrentLinkedDeque.this.unlink(l);
            this.lastRet = null;
        }
    }

    private class Itr extends ConcurrentLinkedDeque<E>.AbstractItr {
        private Itr() {
            super();
        }

        @Override
        Node<E> startNode() {
            return ConcurrentLinkedDeque.this.first();
        }

        @Override
        Node<E> nextNode(Node<E> p) {
            return ConcurrentLinkedDeque.this.succ(p);
        }
    }

    private class DescendingItr extends ConcurrentLinkedDeque<E>.AbstractItr {
        private DescendingItr() {
            super();
        }

        @Override
        Node<E> startNode() {
            return ConcurrentLinkedDeque.this.last();
        }

        @Override
        Node<E> nextNode(Node<E> p) {
            return ConcurrentLinkedDeque.this.pred(p);
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        Node<E> p = first();
        while (p != null) {
            E item = p.item;
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
            if (item != null) {
                Node<E> newNode = new Node<>(item);
                if (h == null) {
                    t = newNode;
                    h = newNode;
                } else {
                    t.lazySetNext(newNode);
                    newNode.lazySetPrev(t);
                    t = newNode;
                }
            } else {
                initHeadTail(h, t);
                return;
            }
        }
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    static {
        PREV_TERMINATOR.next = (Node<E>) PREV_TERMINATOR;
        NEXT_TERMINATOR = new Node<>();
        NEXT_TERMINATOR.prev = (Node<E>) NEXT_TERMINATOR;
        try {
            UNSAFE = Unsafe.getUnsafe();
            headOffset = UNSAFE.objectFieldOffset(ConcurrentLinkedDeque.class.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(ConcurrentLinkedDeque.class.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
