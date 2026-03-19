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

        private Node() {
        }
    }

    static <E> Node<E> newNode(E e) {
        Node<E> node = new Node<>();
        U.putObject(node, ITEM, e);
        return node;
    }

    static <E> boolean casItem(Node<E> node, E e, E e2) {
        return U.compareAndSwapObject(node, ITEM, e, e2);
    }

    static <E> void lazySetNext(Node<E> node, Node<E> node2) {
        U.putOrderedObject(node, NEXT, node2);
    }

    static <E> boolean casNext(Node<E> node, Node<E> node2, Node<E> node3) {
        return U.compareAndSwapObject(node, NEXT, node2, node3);
    }

    public ConcurrentLinkedQueue() {
        Node<E> nodeNewNode = newNode(null);
        this.tail = nodeNewNode;
        this.head = nodeNewNode;
    }

    public ConcurrentLinkedQueue(Collection<? extends E> collection) {
        Iterator<? extends E> it = collection.iterator();
        Node<E> nodeNewNode = null;
        Node<E> node = null;
        while (it.hasNext()) {
            Node<E> nodeNewNode2 = newNode(Objects.requireNonNull(it.next()));
            if (nodeNewNode != null) {
                lazySetNext(node, nodeNewNode2);
            } else {
                nodeNewNode = nodeNewNode2;
            }
            node = nodeNewNode2;
        }
        if (nodeNewNode == null) {
            nodeNewNode = newNode(null);
            node = nodeNewNode;
        }
        this.head = nodeNewNode;
        this.tail = node;
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    final void updateHead(Node<E> node, Node<E> node2) {
        if (node != node2 && casHead(node, node2)) {
            lazySetNext(node, node);
        }
    }

    final Node<E> succ(Node<E> node) {
        Node<E> node2 = node.next;
        return node == node2 ? this.head : node2;
    }

    @Override
    public boolean offer(E e) {
        Node<E> node;
        Node<E> nodeNewNode = newNode(Objects.requireNonNull(e));
        Node<E> node2 = this.tail;
        Node<E> node3 = node2;
        while (true) {
            Node<E> node4 = node2.next;
            if (node4 == null) {
                if (casNext(node2, null, nodeNewNode)) {
                    break;
                }
            } else if (node2 == node4) {
                Node<E> node5 = this.tail;
                if (node3 == node5) {
                    node = this.head;
                } else {
                    node = node5;
                }
                Node<E> node6 = node;
                node3 = node5;
                node2 = node6;
            } else {
                if (node2 != node3) {
                    node2 = this.tail;
                    if (node3 != node2) {
                        node3 = node2;
                    }
                } else {
                    node2 = node3;
                }
                node3 = node2;
                node2 = node4;
            }
        }
        if (node2 != node3) {
            casTail(node3, nodeNewNode);
            return true;
        }
        return true;
    }

    @Override
    public E poll() {
        while (true) {
            Node<E> node = this.head;
            Node<E> node2 = node;
            while (true) {
                E e = node2.item;
                if (e != null && casItem(node2, e, null)) {
                    if (node2 != node) {
                        Node<E> node3 = node2.next;
                        if (node3 != null) {
                            node2 = node3;
                        }
                        updateHead(node, node2);
                    }
                    return e;
                }
                Node<E> node4 = node2.next;
                if (node4 == null) {
                    updateHead(node, node2);
                    return null;
                }
                if (node2 == node4) {
                    break;
                }
                node2 = node4;
            }
        }
    }

    @Override
    public E peek() {
        Node<E> node;
        Node<E> node2;
        E e;
        Node<E> node3;
        loop0: while (true) {
            node = this.head;
            node2 = node;
            while (true) {
                e = node2.item;
                if (e != null || (node3 = node2.next) == null) {
                    break loop0;
                }
                if (node2 == node3) {
                    break;
                }
                node2 = node3;
            }
        }
        updateHead(node, node2);
        return e;
    }

    Node<E> first() {
        Node<E> node;
        Node<E> node2;
        boolean z;
        Node<E> node3;
        loop0: while (true) {
            node = this.head;
            node2 = node;
            while (true) {
                z = node2.item != null;
                if (z || (node3 = node2.next) == null) {
                    break loop0;
                }
                if (node2 == node3) {
                    break;
                }
                node2 = node3;
            }
        }
        updateHead(node, node2);
        if (z) {
            return node2;
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return first() == null;
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
    public boolean remove(Object obj) {
        boolean zCasItem;
        Node<E> nodeSucc;
        if (obj != null) {
            Node<E> nodeFirst = first();
            Node<E> node = null;
            while (nodeFirst != null) {
                E e = nodeFirst.item;
                if (e != null) {
                    if (!obj.equals(e)) {
                        nodeSucc = succ(nodeFirst);
                        Node<E> node2 = nodeSucc;
                        node = nodeFirst;
                        nodeFirst = node2;
                    } else {
                        zCasItem = casItem(nodeFirst, e, null);
                    }
                } else {
                    zCasItem = false;
                }
                Node<E> nodeSucc2 = succ(nodeFirst);
                if (node != null && nodeSucc2 != null) {
                    casNext(node, nodeFirst, nodeSucc2);
                }
                if (!zCasItem) {
                    nodeSucc = nodeSucc2;
                    Node<E> node22 = nodeSucc;
                    node = nodeFirst;
                    nodeFirst = node22;
                } else {
                    return true;
                }
            }
        }
        return false;
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
            Node<E> nodeNewNode = newNode(Objects.requireNonNull(it.next()));
            if (node2 != null) {
                lazySetNext(node3, nodeNewNode);
            } else {
                node2 = nodeNewNode;
            }
            node3 = nodeNewNode;
        }
        if (node2 == null) {
            return false;
        }
        Node<E> node4 = this.tail;
        Node<E> node5 = node4;
        while (true) {
            Node<E> node6 = node4.next;
            if (node6 == null) {
                if (casNext(node4, null, node2)) {
                    break;
                }
            } else if (node4 == node6) {
                Node<E> node7 = this.tail;
                if (node5 == node7) {
                    node = this.head;
                } else {
                    node = node7;
                }
                Node<E> node8 = node;
                node5 = node7;
                node4 = node8;
            } else {
                if (node4 != node5) {
                    node4 = this.tail;
                    if (node5 != node4) {
                        node5 = node4;
                    }
                } else {
                    node4 = node5;
                }
                node5 = node4;
                node4 = node6;
            }
        }
        if (!casTail(node5, node3)) {
            Node<E> node9 = this.tail;
            if (node3.next == null) {
                casTail(node9, node3);
                return true;
            }
            return true;
        }
        return true;
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

    private class Itr implements Iterator<E> {
        private Node<E> lastRet;
        private E nextItem;
        private Node<E> nextNode;

        Itr() {
            Node<E> node;
            Node<E> node2;
            loop0: while (true) {
                node = ConcurrentLinkedQueue.this.head;
                node2 = node;
                while (true) {
                    E e = node2.item;
                    if (e != null) {
                        this.nextNode = node2;
                        this.nextItem = e;
                        break loop0;
                    }
                    Node<E> node3 = node2.next;
                    if (node3 == null) {
                        break loop0;
                    } else if (node2 == node3) {
                        break;
                    } else {
                        node2 = node3;
                    }
                }
            }
            ConcurrentLinkedQueue.this.updateHead(node, node2);
        }

        @Override
        public boolean hasNext() {
            return this.nextItem != null;
        }

        @Override
        public E next() {
            Node<E> node = this.nextNode;
            if (node == null) {
                throw new NoSuchElementException();
            }
            this.lastRet = node;
            E e = null;
            Node<E> nodeSucc = ConcurrentLinkedQueue.this.succ(node);
            while (nodeSucc != null) {
                e = nodeSucc.item;
                if (e != null) {
                    break;
                }
                Node<E> nodeSucc2 = ConcurrentLinkedQueue.this.succ(nodeSucc);
                if (nodeSucc2 != null) {
                    ConcurrentLinkedQueue.casNext(node, nodeSucc, nodeSucc2);
                }
                nodeSucc = nodeSucc2;
            }
            this.nextNode = nodeSucc;
            E e2 = this.nextItem;
            this.nextItem = e;
            return e2;
        }

        @Override
        public void remove() {
            Node<E> node = this.lastRet;
            if (node == null) {
                throw new IllegalStateException();
            }
            node.item = null;
            this.lastRet = null;
        }
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
        Node<E> nodeNewNode = null;
        Node<E> node = null;
        while (true) {
            Object object = objectInputStream.readObject();
            if (object == null) {
                break;
            }
            Node<E> nodeNewNode2 = newNode(object);
            if (nodeNewNode != null) {
                lazySetNext(node, nodeNewNode2);
            } else {
                nodeNewNode = nodeNewNode2;
            }
            node = nodeNewNode2;
        }
        if (nodeNewNode == null) {
            nodeNewNode = newNode(null);
            node = nodeNewNode;
        }
        this.head = nodeNewNode;
        this.tail = node;
    }

    static final class CLQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 33554432;
        int batch;
        Node<E> current;
        boolean exhausted;
        final ConcurrentLinkedQueue<E> queue;

        CLQSpliterator(ConcurrentLinkedQueue<E> concurrentLinkedQueue) {
            this.queue = concurrentLinkedQueue;
        }

        @Override
        public Spliterator<E> trySplit() {
            ConcurrentLinkedQueue<E> concurrentLinkedQueue = this.queue;
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
            if ((nodeFirst != null || (nodeFirst = concurrentLinkedQueue.first()) != null) && nodeFirst.next != null) {
                Object[] objArr = new Object[i2];
                Node<E> nodeFirst2 = nodeFirst;
                int i3 = 0;
                do {
                    E e = nodeFirst2.item;
                    objArr[i3] = e;
                    if (e != null) {
                        i3++;
                    }
                    Node<E> node = nodeFirst2.next;
                    if (nodeFirst2 == node) {
                        nodeFirst2 = concurrentLinkedQueue.first();
                    } else {
                        nodeFirst2 = node;
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
            if (nodeFirst != null || (nodeFirst = concurrentLinkedQueue.first()) != null) {
                this.exhausted = true;
                do {
                    E e = nodeFirst.item;
                    Node<E> node = nodeFirst.next;
                    if (nodeFirst == node) {
                        nodeFirst = concurrentLinkedQueue.first();
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
            ConcurrentLinkedQueue<E> concurrentLinkedQueue = this.queue;
            if (this.exhausted) {
                return false;
            }
            Node<E> nodeFirst = this.current;
            if (nodeFirst != null || (nodeFirst = concurrentLinkedQueue.first()) != null) {
                do {
                    e = nodeFirst.item;
                    Node<E> node = nodeFirst.next;
                    if (nodeFirst == node) {
                        nodeFirst = concurrentLinkedQueue.first();
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
        return new CLQSpliterator(this);
    }

    private boolean casTail(Node<E> node, Node<E> node2) {
        return U.compareAndSwapObject(this, TAIL, node, node2);
    }

    private boolean casHead(Node<E> node, Node<E> node2) {
        return U.compareAndSwapObject(this, HEAD, node, node2);
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
