package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import sun.misc.Unsafe;

public class ConcurrentLinkedDeque<E> extends AbstractCollection<E> implements Deque<E>, Serializable {
    private static final long HEAD;
    private static final int HOPS = 2;
    private static final Node<Object> NEXT_TERMINATOR;
    private static final long TAIL;
    private static final long serialVersionUID = 876323262645176354L;
    private volatile transient Node<E> head;
    private volatile transient Node<E> tail;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final Node<Object> PREV_TERMINATOR = new Node<>();

    Node<E> prevTerminator() {
        return (Node<E>) PREV_TERMINATOR;
    }

    Node<E> nextTerminator() {
        return (Node<E>) NEXT_TERMINATOR;
    }

    static final class Node<E> {
        private static final long ITEM;
        private static final long NEXT;
        private static final long PREV;
        private static final Unsafe U = Unsafe.getUnsafe();
        volatile E item;
        volatile Node<E> next;
        volatile Node<E> prev;

        Node() {
        }

        Node(E e) {
            U.putObject(this, ITEM, e);
        }

        boolean casItem(E e, E e2) {
            return U.compareAndSwapObject(this, ITEM, e, e2);
        }

        void lazySetNext(Node<E> node) {
            U.putOrderedObject(this, NEXT, node);
        }

        boolean casNext(Node<E> node, Node<E> node2) {
            return U.compareAndSwapObject(this, NEXT, node, node2);
        }

        void lazySetPrev(Node<E> node) {
            U.putOrderedObject(this, PREV, node);
        }

        boolean casPrev(Node<E> node, Node<E> node2) {
            return U.compareAndSwapObject(this, PREV, node, node2);
        }

        static {
            try {
                PREV = U.objectFieldOffset(Node.class.getDeclaredField("prev"));
                ITEM = U.objectFieldOffset(Node.class.getDeclaredField("item"));
                NEXT = U.objectFieldOffset(Node.class.getDeclaredField("next"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    private void linkFirst(E e) {
        Node<E> node;
        Node<E> node2;
        Node<E> node3 = new Node<>(Objects.requireNonNull(e));
        loop0: while (true) {
            node = this.head;
            node2 = node;
            while (true) {
                Node<E> node4 = node.prev;
                if (node4 != null) {
                    node = node4.prev;
                    if (node != null) {
                        Node<E> node5 = this.head;
                        if (node2 != node5) {
                            node = node5;
                        }
                        node2 = node5;
                    } else {
                        node = node4;
                    }
                }
                if (node.next == node) {
                    break;
                }
                node3.lazySetNext(node);
                if (node.casPrev(null, node3)) {
                    break loop0;
                }
            }
        }
        if (node != node2) {
            casHead(node2, node3);
        }
    }

    private void linkLast(E e) {
        Node<E> node;
        Node<E> node2;
        Node<E> node3 = new Node<>(Objects.requireNonNull(e));
        loop0: while (true) {
            node = this.tail;
            node2 = node;
            while (true) {
                Node<E> node4 = node.next;
                if (node4 != null) {
                    node = node4.next;
                    if (node != null) {
                        Node<E> node5 = this.tail;
                        if (node2 != node5) {
                            node = node5;
                        }
                        node2 = node5;
                    } else {
                        node = node4;
                    }
                }
                if (node.prev == node) {
                    break;
                }
                node3.lazySetPrev(node);
                if (node.casNext(null, node3)) {
                    break loop0;
                }
            }
        }
        if (node != node2) {
            casTail(node2, node3);
        }
    }

    void unlink(Node<E> node) {
        boolean z;
        Node<E> node2 = node.prev;
        Node<E> node3 = node.next;
        if (node2 == null) {
            unlinkFirst(node, node3);
            return;
        }
        if (node3 == null) {
            unlinkLast(node, node2);
            return;
        }
        boolean z2 = true;
        int i = 1;
        while (true) {
            if (node2.item == null) {
                Node<E> node4 = node2.prev;
                if (node4 == null) {
                    if (node2.next == node2) {
                        return;
                    } else {
                        z = true;
                    }
                } else if (node2 != node4) {
                    i++;
                    node2 = node4;
                } else {
                    return;
                }
            } else {
                z = false;
                break;
            }
        }
    }

    private void unlinkFirst(Node<E> node, Node<E> node2) {
        Node<E> node3;
        Node<E> node4 = null;
        Node<E> node5 = node2;
        while (node5.item == null && (node3 = node5.next) != null) {
            if (node5 != node3) {
                node4 = node5;
                node5 = node3;
            } else {
                return;
            }
        }
        if (node4 != null && node5.prev != node5 && node.casNext(node2, node5)) {
            skipDeletedPredecessors(node5);
            if (node.prev == null) {
                if ((node5.next == null || node5.item != null) && node5.prev == node) {
                    updateHead();
                    updateTail();
                    node4.lazySetNext(node4);
                    node4.lazySetPrev(prevTerminator());
                }
            }
        }
    }

    private void unlinkLast(Node<E> node, Node<E> node2) {
        Node<E> node3;
        Node<E> node4 = null;
        Node<E> node5 = node2;
        while (node5.item == null && (node3 = node5.prev) != null) {
            if (node5 != node3) {
                node4 = node5;
                node5 = node3;
            } else {
                return;
            }
        }
        if (node4 != null && node5.next != node5 && node.casPrev(node2, node5)) {
            skipDeletedSuccessors(node5);
            if (node.next == null) {
                if ((node5.prev == null || node5.item != null) && node5.next == node) {
                    updateHead();
                    updateTail();
                    node4.lazySetPrev(node4);
                    node4.lazySetNext(nextTerminator());
                }
            }
        }
    }

    private final void updateHead() {
        Node<E> node;
        while (true) {
            Node<E> node2 = this.head;
            if (node2.item == null && (node = node2.prev) != null) {
                do {
                    Node<E> node3 = node.prev;
                    if (node3 != null) {
                        node = node3.prev;
                        if (node == null) {
                            node = node3;
                        }
                    }
                    if (casHead(node2, node)) {
                        return;
                    }
                } while (node2 == this.head);
            } else {
                return;
            }
        }
    }

    private final void updateTail() {
        Node<E> node;
        while (true) {
            Node<E> node2 = this.tail;
            if (node2.item == null && (node = node2.next) != null) {
                do {
                    Node<E> node3 = node.next;
                    if (node3 != null) {
                        node = node3.next;
                        if (node == null) {
                            node = node3;
                        }
                    }
                    if (casTail(node2, node)) {
                        return;
                    }
                } while (node2 == this.tail);
            } else {
                return;
            }
        }
    }

    private void skipDeletedPredecessors(Node<E> node) {
        while (true) {
            Node<E> node2 = node.prev;
            Node<E> node3 = node2;
            while (true) {
                if (node3.item != null) {
                    break;
                }
                Node<E> node4 = node3.prev;
                if (node4 == null) {
                    if (node3.next != node3) {
                        break;
                    }
                } else if (node3 == node4) {
                    break;
                } else {
                    node3 = node4;
                }
            }
            if (node.item == null && node.next != null) {
                return;
            }
        }
    }

    private void skipDeletedSuccessors(Node<E> node) {
        while (true) {
            Node<E> node2 = node.next;
            Node<E> node3 = node2;
            while (true) {
                if (node3.item != null) {
                    break;
                }
                Node<E> node4 = node3.next;
                if (node4 == null) {
                    if (node3.prev != node3) {
                        break;
                    }
                } else if (node3 == node4) {
                    break;
                } else {
                    node3 = node4;
                }
            }
            if (node.item == null && node.prev != null) {
                return;
            }
        }
    }

    final Node<E> succ(Node<E> node) {
        Node<E> node2 = node.next;
        return node == node2 ? first() : node2;
    }

    final Node<E> pred(Node<E> node) {
        Node<E> node2 = node.prev;
        return node == node2 ? last() : node2;
    }

    Node<E> first() {
        Node<E> node;
        Node<E> node2;
        do {
            node = this.head;
            node2 = node;
            while (true) {
                Node<E> node3 = node.prev;
                if (node3 != null) {
                    node = node3.prev;
                    if (node != null) {
                        Node<E> node4 = this.head;
                        if (node2 != node4) {
                            node = node4;
                        }
                        node2 = node4;
                    } else {
                        node = node3;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (node == node2) {
                break;
            }
        } while (!casHead(node2, node));
        return node;
    }

    Node<E> last() {
        Node<E> node;
        Node<E> node2;
        do {
            node = this.tail;
            node2 = node;
            while (true) {
                Node<E> node3 = node.next;
                if (node3 != null) {
                    node = node3.next;
                    if (node != null) {
                        Node<E> node4 = this.tail;
                        if (node2 != node4) {
                            node = node4;
                        }
                        node2 = node4;
                    } else {
                        node = node3;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (node == node2) {
                break;
            }
        } while (!casTail(node2, node));
        return node;
    }

    private E screenNullResult(E e) {
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    public ConcurrentLinkedDeque() {
        Node<E> node = new Node<>(null);
        this.tail = node;
        this.head = node;
    }

    public ConcurrentLinkedDeque(Collection<? extends E> collection) {
        Iterator<? extends E> it = collection.iterator();
        Node<E> node = null;
        Node<E> node2 = null;
        while (it.hasNext()) {
            Node<E> node3 = new Node<>(Objects.requireNonNull(it.next()));
            if (node != null) {
                node2.lazySetNext(node3);
                node3.lazySetPrev(node2);
            } else {
                node = node3;
            }
            node2 = node3;
        }
        initHeadTail(node, node2);
    }

    private void initHeadTail(Node<E> node, Node<E> node2) {
        if (node == node2) {
            if (node == null) {
                node = new Node<>(null);
                node2 = node;
            } else {
                Node<E> node3 = new Node<>(null);
                node2.lazySetNext(node3);
                node3.lazySetPrev(node2);
                node2 = node3;
            }
        }
        this.head = node;
        this.tail = node2;
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
        Node<E> nodeFirst = first();
        while (nodeFirst != null) {
            E e = nodeFirst.item;
            if (e == null) {
                nodeFirst = succ(nodeFirst);
            } else {
                return e;
            }
        }
        return null;
    }

    @Override
    public E peekLast() {
        Node<E> nodeLast = last();
        while (nodeLast != null) {
            E e = nodeLast.item;
            if (e == null) {
                nodeLast = pred(nodeLast);
            } else {
                return e;
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
        Node<E> nodeFirst = first();
        while (nodeFirst != null) {
            E e = nodeFirst.item;
            if (e == null || !nodeFirst.casItem(e, null)) {
                nodeFirst = succ(nodeFirst);
            } else {
                unlink(nodeFirst);
                return e;
            }
        }
        return null;
    }

    @Override
    public E pollLast() {
        Node<E> nodeLast = last();
        while (nodeLast != null) {
            E e = nodeLast.item;
            if (e == null || !nodeLast.casItem(e, null)) {
                nodeLast = pred(nodeLast);
            } else {
                unlink(nodeLast);
                return e;
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
    public E peek() {
        return peekFirst();
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public E pop() {
        return removeFirst();
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
    public boolean removeFirstOccurrence(Object obj) {
        Objects.requireNonNull(obj);
        Node<E> nodeFirst = first();
        while (nodeFirst != null) {
            E e = nodeFirst.item;
            if (e == null || !obj.equals(e) || !nodeFirst.casItem(e, null)) {
                nodeFirst = succ(nodeFirst);
            } else {
                unlink(nodeFirst);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object obj) {
        Objects.requireNonNull(obj);
        Node<E> nodeLast = last();
        while (nodeLast != null) {
            E e = nodeLast.item;
            if (e == null || !obj.equals(e) || !nodeLast.casItem(e, null)) {
                nodeLast = pred(nodeLast);
            } else {
                unlink(nodeLast);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(Object obj) {
        if (obj != null) {
            Node<E> nodeFirst = first();
            while (nodeFirst != null) {
                E e = nodeFirst.item;
                if (e == null || !obj.equals(e)) {
                    nodeFirst = succ(nodeFirst);
                } else {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return peekFirst() == null;
    }

    @Override
    public int size() {
        int i;
        loop0: while (true) {
            i = 0;
            Node<E> nodeFirst = first();
            while (nodeFirst != null && (nodeFirst.item == null || (i = i + 1) != Integer.MAX_VALUE)) {
                Node<E> node = nodeFirst.next;
                if (nodeFirst == node) {
                    break;
                }
                nodeFirst = node;
            }
        }
        return i;
    }

    @Override
    public boolean remove(Object obj) {
        return removeFirstOccurrence(obj);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        Node<E> node;
        if (collection == this) {
            throw new IllegalArgumentException();
        }
        Iterator<? extends E> it = collection.iterator();
        Node<E> node2 = null;
        Node<E> node3 = null;
        while (it.hasNext()) {
            Node<E> node4 = new Node<>(Objects.requireNonNull(it.next()));
            if (node2 != null) {
                node3.lazySetNext(node4);
                node4.lazySetPrev(node3);
            } else {
                node2 = node4;
            }
            node3 = node4;
        }
        if (node2 == null) {
            return false;
        }
        loop1: while (true) {
            Node<E> node5 = this.tail;
            node = node5;
            while (true) {
                Node<E> node6 = node5.next;
                if (node6 != null) {
                    node5 = node6.next;
                    if (node5 != null) {
                        Node<E> node7 = this.tail;
                        if (node != node7) {
                            node5 = node7;
                        }
                        node = node7;
                    } else {
                        node5 = node6;
                    }
                }
                if (node5.prev == node5) {
                    break;
                }
                node2.lazySetPrev(node5);
                if (node5.casNext(null, node2)) {
                    break loop1;
                }
            }
        }
        if (!casTail(node, node3)) {
            Node<E> node8 = this.tail;
            if (node3.next == null) {
                casTail(node8, node3);
                return true;
            }
            return true;
        }
        return true;
    }

    @Override
    public void clear() {
        while (pollFirst() != null) {
        }
    }

    @Override
    public String toString() {
        int i;
        int length;
        String[] strArr = null;
        loop0: while (true) {
            Node<E> nodeFirst = first();
            i = 0;
            length = 0;
            while (nodeFirst != null) {
                E e = nodeFirst.item;
                if (e != null) {
                    if (strArr == null) {
                        strArr = new String[4];
                    } else if (i == strArr.length) {
                        strArr = (String[]) Arrays.copyOf(strArr, 2 * i);
                    }
                    String string = e.toString();
                    strArr[i] = string;
                    length += string.length();
                    i++;
                }
                Node<E> node = nodeFirst.next;
                if (nodeFirst == node) {
                    break;
                }
                nodeFirst = node;
            }
        }
        if (i == 0) {
            return "[]";
        }
        return Helpers.toString(strArr, i, length);
    }

    private Object[] toArrayInternal(Object[] objArr) {
        int i;
        Object[] objArrCopyOf = objArr;
        loop0: while (true) {
            Node<E> nodeFirst = first();
            i = 0;
            while (nodeFirst != null) {
                E e = nodeFirst.item;
                if (e != null) {
                    if (objArrCopyOf == null) {
                        objArrCopyOf = new Object[4];
                    } else if (i == objArrCopyOf.length) {
                        objArrCopyOf = Arrays.copyOf(objArrCopyOf, 2 * (i + 4));
                    }
                    objArrCopyOf[i] = e;
                    i++;
                }
                Node<E> node = nodeFirst.next;
                if (nodeFirst == node) {
                    break;
                }
                nodeFirst = node;
            }
        }
        if (objArrCopyOf == null) {
            return new Object[0];
        }
        if (objArr == null || i > objArr.length) {
            return i == objArrCopyOf.length ? objArrCopyOf : Arrays.copyOf(objArrCopyOf, i);
        }
        if (objArr != objArrCopyOf) {
            System.arraycopy(objArrCopyOf, 0, objArr, 0, i);
        }
        if (i < objArr.length) {
            objArr[i] = null;
        }
        return objArr;
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
            Node<E> nodeStartNode = this.nextNode == null ? startNode() : nextNode(this.nextNode);
            while (nodeStartNode != null) {
                E e = nodeStartNode.item;
                if (e == null) {
                    nodeStartNode = nextNode(nodeStartNode);
                } else {
                    this.nextNode = nodeStartNode;
                    this.nextItem = e;
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
            E e = this.nextItem;
            if (e == null) {
                throw new NoSuchElementException();
            }
            advance();
            return e;
        }

        @Override
        public void remove() {
            Node<E> node = this.lastRet;
            if (node == null) {
                throw new IllegalStateException();
            }
            node.item = null;
            ConcurrentLinkedDeque.this.unlink(node);
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
        Node<E> nextNode(Node<E> node) {
            return ConcurrentLinkedDeque.this.succ(node);
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
        Node<E> nextNode(Node<E> node) {
            return ConcurrentLinkedDeque.this.pred(node);
        }
    }

    static final class CLDSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 33554432;
        int batch;
        Node<E> current;
        boolean exhausted;
        final ConcurrentLinkedDeque<E> queue;

        CLDSpliterator(ConcurrentLinkedDeque<E> concurrentLinkedDeque) {
            this.queue = concurrentLinkedDeque;
        }

        @Override
        public Spliterator<E> trySplit() {
            ConcurrentLinkedDeque<E> concurrentLinkedDeque = this.queue;
            int i = this.batch;
            int i2 = MAX_BATCH;
            if (i > 0) {
                if (i < MAX_BATCH) {
                    i2 = i + 1;
                }
            } else {
                i2 = 1;
            }
            if (this.exhausted) {
                return null;
            }
            Node<E> nodeFirst = this.current;
            if (nodeFirst != null || (nodeFirst = concurrentLinkedDeque.first()) != null) {
                if (nodeFirst.item == null) {
                    Node<E> node = nodeFirst.next;
                    if (nodeFirst == node) {
                        nodeFirst = concurrentLinkedDeque.first();
                        this.current = nodeFirst;
                    } else {
                        nodeFirst = node;
                    }
                }
                if (nodeFirst != null && nodeFirst.next != null) {
                    Object[] objArr = new Object[i2];
                    Node<E> nodeFirst2 = nodeFirst;
                    int i3 = 0;
                    do {
                        E e = nodeFirst2.item;
                        objArr[i3] = e;
                        if (e != null) {
                            i3++;
                        }
                        Node<E> node2 = nodeFirst2.next;
                        if (nodeFirst2 == node2) {
                            nodeFirst2 = concurrentLinkedDeque.first();
                        } else {
                            nodeFirst2 = node2;
                        }
                        if (nodeFirst2 == null) {
                            break;
                        }
                    } while (i3 < i2);
                    this.current = nodeFirst2;
                    if (nodeFirst2 == null) {
                        this.exhausted = true;
                    }
                    if (i3 > 0) {
                        this.batch = i3;
                        return Spliterators.spliterator(objArr, 0, i3, 4368);
                    }
                    return null;
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
            ConcurrentLinkedDeque<E> concurrentLinkedDeque = this.queue;
            if (this.exhausted) {
                return;
            }
            Node<E> nodeFirst = this.current;
            if (nodeFirst != null || (nodeFirst = concurrentLinkedDeque.first()) != null) {
                this.exhausted = true;
                do {
                    E e = nodeFirst.item;
                    Node<E> node = nodeFirst.next;
                    if (nodeFirst == node) {
                        nodeFirst = concurrentLinkedDeque.first();
                    } else {
                        nodeFirst = node;
                    }
                    if (e != null) {
                        consumer.accept(e);
                    }
                } while (nodeFirst != null);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            E e;
            if (consumer == null) {
                throw new NullPointerException();
            }
            ConcurrentLinkedDeque<E> concurrentLinkedDeque = this.queue;
            if (this.exhausted) {
                return false;
            }
            Node<E> nodeFirst = this.current;
            if (nodeFirst != null || (nodeFirst = concurrentLinkedDeque.first()) != null) {
                do {
                    e = nodeFirst.item;
                    Node<E> node = nodeFirst.next;
                    if (nodeFirst == node) {
                        nodeFirst = concurrentLinkedDeque.first();
                    } else {
                        nodeFirst = node;
                    }
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
        return new CLDSpliterator(this);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        Node<E> nodeFirst = first();
        while (nodeFirst != null) {
            E e = nodeFirst.item;
            if (e != null) {
                objectOutputStream.writeObject(e);
            }
            nodeFirst = succ(nodeFirst);
        }
        objectOutputStream.writeObject(null);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        Node<E> node = null;
        Node<E> node2 = null;
        while (true) {
            Object object = objectInputStream.readObject();
            if (object != null) {
                Node<E> node3 = new Node<>(object);
                if (node != null) {
                    node2.lazySetNext(node3);
                    node3.lazySetPrev(node2);
                } else {
                    node = node3;
                }
                node2 = node3;
            } else {
                initHeadTail(node, node2);
                return;
            }
        }
    }

    private boolean casHead(Node<E> node, Node<E> node2) {
        return U.compareAndSwapObject(this, HEAD, node, node2);
    }

    private boolean casTail(Node<E> node, Node<E> node2) {
        return U.compareAndSwapObject(this, TAIL, node, node2);
    }

    static {
        PREV_TERMINATOR.next = (Node<E>) PREV_TERMINATOR;
        NEXT_TERMINATOR = new Node<>();
        NEXT_TERMINATOR.prev = (Node<E>) NEXT_TERMINATOR;
        try {
            HEAD = U.objectFieldOffset(ConcurrentLinkedDeque.class.getDeclaredField("head"));
            TAIL = U.objectFieldOffset(ConcurrentLinkedDeque.class.getDeclaredField("tail"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
