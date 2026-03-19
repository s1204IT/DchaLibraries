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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import sun.misc.Unsafe;

public class LinkedTransferQueue<E> extends AbstractQueue<E> implements TransferQueue<E>, Serializable {
    private static final int ASYNC = 1;
    private static final int CHAINED_SPINS = 64;
    private static final int FRONT_SPINS = 128;
    private static final long HEAD;
    private static final boolean MP;
    private static final int NOW = 0;
    private static final long SWEEPVOTES;
    static final int SWEEP_THRESHOLD = 32;
    private static final int SYNC = 2;
    private static final long TAIL;
    private static final int TIMED = 3;
    private static final Unsafe U;
    private static final long serialVersionUID = -3223113410248163686L;
    volatile transient Node head;
    private volatile transient int sweepVotes;
    private volatile transient Node tail;

    static {
        MP = Runtime.getRuntime().availableProcessors() > 1;
        U = Unsafe.getUnsafe();
        try {
            HEAD = U.objectFieldOffset(LinkedTransferQueue.class.getDeclaredField("head"));
            TAIL = U.objectFieldOffset(LinkedTransferQueue.class.getDeclaredField("tail"));
            SWEEPVOTES = U.objectFieldOffset(LinkedTransferQueue.class.getDeclaredField("sweepVotes"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    static final class Node {
        private static final long ITEM;
        private static final long NEXT;
        private static final Unsafe U = Unsafe.getUnsafe();
        private static final long WAITER;
        private static final long serialVersionUID = -3375979862319811754L;
        final boolean isData;
        volatile Object item;
        volatile Node next;
        volatile Thread waiter;

        final boolean casNext(Node node, Node node2) {
            return U.compareAndSwapObject(this, NEXT, node, node2);
        }

        final boolean casItem(Object obj, Object obj2) {
            return U.compareAndSwapObject(this, ITEM, obj, obj2);
        }

        Node(Object obj, boolean z) {
            U.putObject(this, ITEM, obj);
            this.isData = z;
        }

        final void forgetNext() {
            U.putObject(this, NEXT, this);
        }

        final void forgetContents() {
            U.putObject(this, ITEM, this);
            U.putObject(this, WAITER, null);
        }

        final boolean isMatched() {
            Object obj = this.item;
            if (obj != this) {
                if ((obj == null) != this.isData) {
                    return false;
                }
            }
            return true;
        }

        final boolean isUnmatchedRequest() {
            return !this.isData && this.item == null;
        }

        final boolean cannotPrecede(boolean z) {
            Object obj;
            boolean z2 = this.isData;
            if (z2 == z || (obj = this.item) == this) {
                return false;
            }
            return (obj != null) == z2;
        }

        final boolean tryMatchData() {
            Object obj = this.item;
            if (obj != null && obj != this && casItem(obj, null)) {
                LockSupport.unpark(this.waiter);
                return true;
            }
            return false;
        }

        static {
            try {
                ITEM = U.objectFieldOffset(Node.class.getDeclaredField("item"));
                NEXT = U.objectFieldOffset(Node.class.getDeclaredField("next"));
                WAITER = U.objectFieldOffset(Node.class.getDeclaredField("waiter"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    private boolean casTail(Node node, Node node2) {
        return U.compareAndSwapObject(this, TAIL, node, node2);
    }

    private boolean casHead(Node node, Node node2) {
        return U.compareAndSwapObject(this, HEAD, node, node2);
    }

    private boolean casSweepVotes(int i, int i2) {
        return U.compareAndSwapInt(this, SWEEPVOTES, i, i2);
    }

    private E xfer(E e, boolean z, int i, long j) {
        if (z && e == null) {
            throw new NullPointerException();
        }
        Node node = null;
        while (true) {
            Node node2 = this.head;
            Node node3 = node2;
            while (true) {
                boolean z2 = true;
                if (node2 == null) {
                    break;
                }
                boolean z3 = node2.isData;
                E e2 = (E) node2.item;
                if (e2 != node2) {
                    if ((e2 != null) == z3) {
                        if (z3 == z) {
                            break;
                        }
                        if (node2.casItem(e2, e)) {
                            Node node4 = node2;
                            while (true) {
                                if (node4 == node3) {
                                    break;
                                }
                                Node node5 = node4.next;
                                if (this.head != node3) {
                                    node3 = this.head;
                                    if (node3 == null || (node4 = node3.next) == null || !node4.isMatched()) {
                                        break;
                                    }
                                } else {
                                    if (node5 != null) {
                                        node4 = node5;
                                    }
                                    if (casHead(node3, node4)) {
                                        node3.forgetNext();
                                        break;
                                    }
                                }
                            }
                            LockSupport.unpark(node2.waiter);
                            return e2;
                        }
                    }
                }
                Node node6 = node2.next;
                if (node2 == node6) {
                    node2 = this.head;
                    node3 = node2;
                } else {
                    node2 = node6;
                }
            }
        }
        return e;
    }

    private Node tryAppend(Node node, boolean z) {
        Node node2;
        Node node3;
        Node node4 = this.tail;
        Node node5 = node4;
        while (true) {
            Node node6 = null;
            if (node4 == null && (node4 = this.head) == null) {
                if (casHead(null, node)) {
                    return node;
                }
            } else {
                if (node4.cannotPrecede(z)) {
                    return null;
                }
                Node node7 = node4.next;
                if (node7 != null) {
                    if (node4 != node5 && node5 != (node2 = this.tail)) {
                        node5 = node2;
                        node6 = node5;
                    } else if (node4 != node7) {
                        node6 = node7;
                    }
                    node4 = node6;
                } else {
                    if (node4.casNext(null, node)) {
                        if (node4 != node5) {
                            do {
                                if ((this.tail == node5 && casTail(node5, node)) || (node5 = this.tail) == null || (node3 = node5.next) == null || (node = node3.next) == null) {
                                    break;
                                }
                            } while (node != node5);
                        }
                        return node4;
                    }
                    node4 = node4.next;
                }
            }
        }
    }

    private E awaitMatch(Node node, Node node2, E e, boolean z, long j) {
        long jNanoTime = z ? System.nanoTime() + j : 0L;
        Thread threadCurrentThread = Thread.currentThread();
        ThreadLocalRandom threadLocalRandomCurrent = null;
        int iSpinsFor = -1;
        while (true) {
            E e2 = (E) node.item;
            if (e2 != e) {
                node.forgetContents();
                return e2;
            }
            if (threadCurrentThread.isInterrupted() || (z && j <= 0)) {
                unsplice(node2, node);
                if (node.casItem(e, node)) {
                    return e;
                }
            } else if (iSpinsFor < 0) {
                iSpinsFor = spinsFor(node2, node.isData);
                if (iSpinsFor > 0) {
                    threadLocalRandomCurrent = ThreadLocalRandom.current();
                }
            } else if (iSpinsFor > 0) {
                iSpinsFor--;
                if (threadLocalRandomCurrent.nextInt(64) == 0) {
                    Thread.yield();
                }
            } else if (node.waiter == null) {
                node.waiter = threadCurrentThread;
            } else if (z) {
                j = jNanoTime - System.nanoTime();
                if (j > 0) {
                    LockSupport.parkNanos(this, j);
                }
            } else {
                LockSupport.park(this);
            }
        }
    }

    private static int spinsFor(Node node, boolean z) {
        if (MP && node != null) {
            if (node.isData != z) {
                return 192;
            }
            if (node.isMatched()) {
                return 128;
            }
            if (node.waiter == null) {
                return 64;
            }
            return 0;
        }
        return 0;
    }

    final Node succ(Node node) {
        Node node2 = node.next;
        return node == node2 ? this.head : node2;
    }

    final Node firstDataNode() {
        while (true) {
            Node node = this.head;
            while (node != null) {
                Object obj = node.item;
                if (node.isData) {
                    if (obj != null && obj != node) {
                        return node;
                    }
                } else if (obj == null) {
                    return null;
                }
                Node node2 = node.next;
                if (node == node2) {
                    break;
                }
                node = node2;
            }
            return null;
        }
    }

    private int countOfMode(boolean z) {
        int i;
        loop0: while (true) {
            Node node = this.head;
            i = 0;
            while (node != null) {
                if (!node.isMatched()) {
                    if (node.isData == z) {
                        i++;
                        if (i == Integer.MAX_VALUE) {
                            break loop0;
                        }
                    } else {
                        return 0;
                    }
                }
                Node node2 = node.next;
                if (node == node2) {
                    break;
                }
                node = node2;
            }
        }
        return i;
    }

    @Override
    public String toString() {
        int length;
        int i;
        String[] strArr = null;
        loop0: while (true) {
            Node node = this.head;
            length = 0;
            i = 0;
            while (node != null) {
                Object obj = node.item;
                if (node.isData) {
                    if (obj != null && obj != node) {
                        if (strArr == null) {
                            strArr = new String[4];
                        } else if (i == strArr.length) {
                            strArr = (String[]) Arrays.copyOf(strArr, 2 * i);
                        }
                        String string = obj.toString();
                        strArr[i] = string;
                        length += string.length();
                        i++;
                    }
                } else if (obj == null) {
                    break loop0;
                }
                Node node2 = node.next;
                if (node == node2) {
                    break;
                }
                node = node2;
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
            Node node = this.head;
            i = 0;
            while (node != null) {
                Object obj = node.item;
                if (node.isData) {
                    if (obj != null && obj != node) {
                        if (objArrCopyOf == null) {
                            objArrCopyOf = new Object[4];
                        } else if (i == objArrCopyOf.length) {
                            objArrCopyOf = Arrays.copyOf(objArrCopyOf, 2 * (i + 4));
                        }
                        objArrCopyOf[i] = obj;
                        i++;
                    }
                } else if (obj == null) {
                    break loop0;
                }
                Node node2 = node.next;
                if (node == node2) {
                    break;
                }
                node = node2;
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

    final class Itr implements Iterator<E> {
        private Node lastPred;
        private Node lastRet;
        private E nextItem;
        private Node nextNode;

        private void advance(Node node) {
            Node node2;
            Node node3 = this.lastRet;
            if (node3 != null && !node3.isMatched()) {
                this.lastPred = node3;
            } else {
                Node node4 = this.lastPred;
                if (node4 == null || node4.isMatched()) {
                    this.lastPred = null;
                } else {
                    while (true) {
                        Node node5 = node4.next;
                        if (node5 == null || node5 == node4 || !node5.isMatched() || (node2 = node5.next) == null || node2 == node5) {
                            break;
                        } else {
                            node4.casNext(node5, node2);
                        }
                    }
                }
            }
            this.lastRet = node;
            while (true) {
                Node node6 = node == null ? LinkedTransferQueue.this.head : node.next;
                if (node6 == null) {
                    break;
                }
                if (node6 == node) {
                    node = null;
                } else {
                    E e = (E) node6.item;
                    if (node6.isData) {
                        if (e != null && e != node6) {
                            this.nextItem = e;
                            this.nextNode = node6;
                            return;
                        }
                    } else {
                        if (e != null) {
                            break;
                            break;
                        }
                        break;
                    }
                    if (node != null) {
                        Node node7 = node6.next;
                        if (node7 == null) {
                            break;
                        } else if (node6 != node7) {
                            node.casNext(node6, node7);
                        } else {
                            node = null;
                        }
                    } else {
                        node = node6;
                    }
                }
            }
            this.nextNode = null;
            this.nextItem = null;
        }

        Itr() {
            advance(null);
        }

        @Override
        public final boolean hasNext() {
            return this.nextNode != null;
        }

        @Override
        public final E next() {
            Node node = this.nextNode;
            if (node == null) {
                throw new NoSuchElementException();
            }
            E e = this.nextItem;
            advance(node);
            return e;
        }

        @Override
        public final void remove() {
            Node node = this.lastRet;
            if (node == null) {
                throw new IllegalStateException();
            }
            this.lastRet = null;
            if (node.tryMatchData()) {
                LinkedTransferQueue.this.unsplice(this.lastPred, node);
            }
        }
    }

    final class LTQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 33554432;
        int batch;
        Node current;
        boolean exhausted;

        LTQSpliterator() {
        }

        @Override
        public Spliterator<E> trySplit() {
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
            Node nodeFirstDataNode = this.current;
            if ((nodeFirstDataNode != null || (nodeFirstDataNode = LinkedTransferQueue.this.firstDataNode()) != null) && nodeFirstDataNode.next != null) {
                Object[] objArr = new Object[i2];
                int i3 = 0;
                do {
                    Object obj = nodeFirstDataNode.item;
                    if (obj != nodeFirstDataNode) {
                        objArr[i3] = obj;
                        if (obj != null) {
                            i3++;
                        }
                    }
                    Node node = nodeFirstDataNode.next;
                    if (nodeFirstDataNode == node) {
                        nodeFirstDataNode = LinkedTransferQueue.this.firstDataNode();
                    } else {
                        nodeFirstDataNode = node;
                    }
                    if (nodeFirstDataNode == null || i3 >= i2) {
                        break;
                    }
                } while (nodeFirstDataNode.isData);
                this.current = nodeFirstDataNode;
                if (nodeFirstDataNode == null) {
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
            if (this.exhausted) {
                return;
            }
            Node nodeFirstDataNode = this.current;
            if (nodeFirstDataNode != null || (nodeFirstDataNode = LinkedTransferQueue.this.firstDataNode()) != null) {
                this.exhausted = true;
                do {
                    Node node = (Object) nodeFirstDataNode.item;
                    if (node != null && node != nodeFirstDataNode) {
                        consumer.accept(node);
                    }
                    Node node2 = nodeFirstDataNode.next;
                    if (nodeFirstDataNode == node2) {
                        nodeFirstDataNode = LinkedTransferQueue.this.firstDataNode();
                    } else {
                        nodeFirstDataNode = node2;
                    }
                    if (nodeFirstDataNode == null) {
                        return;
                    }
                } while (nodeFirstDataNode.isData);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> consumer) {
            Node node;
            if (consumer == null) {
                throw new NullPointerException();
            }
            if (this.exhausted) {
                return false;
            }
            Node nodeFirstDataNode = this.current;
            if (nodeFirstDataNode != null || (nodeFirstDataNode = LinkedTransferQueue.this.firstDataNode()) != null) {
                do {
                    node = (Object) nodeFirstDataNode.item;
                    if (node == nodeFirstDataNode) {
                        node = null;
                    }
                    Node node2 = nodeFirstDataNode.next;
                    if (nodeFirstDataNode == node2) {
                        nodeFirstDataNode = LinkedTransferQueue.this.firstDataNode();
                    } else {
                        nodeFirstDataNode = node2;
                    }
                    if (node != null || nodeFirstDataNode == null) {
                        break;
                    }
                } while (nodeFirstDataNode.isData);
                this.current = nodeFirstDataNode;
                if (nodeFirstDataNode == null) {
                    this.exhausted = true;
                }
                if (node != null) {
                    consumer.accept(node);
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
        return new LTQSpliterator();
    }

    final void unsplice(Node node, Node node2) {
        node2.waiter = null;
        if (node != null && node != node2 && node.next == node2) {
            Node node3 = node2.next;
            if (node3 != null && (node3 == node2 || !node.casNext(node2, node3) || !node.isMatched())) {
                return;
            }
            while (true) {
                Node node4 = this.head;
                if (node4 == node || node4 == node2 || node4 == null) {
                    return;
                }
                if (node4.isMatched()) {
                    Node node5 = node4.next;
                    if (node5 == null) {
                        return;
                    }
                    if (node5 != node4 && casHead(node4, node5)) {
                        node4.forgetNext();
                    }
                } else {
                    if (node.next == node || node2.next == node2) {
                        return;
                    }
                    while (true) {
                        int i = this.sweepVotes;
                        if (i < 32) {
                            if (casSweepVotes(i, i + 1)) {
                                return;
                            }
                        } else if (casSweepVotes(i, 0)) {
                            sweep();
                            return;
                        }
                    }
                }
            }
        }
    }

    private void sweep() {
        Node node = this.head;
        while (node != null) {
            Node node2 = node.next;
            if (node2 != null) {
                if (!node2.isMatched()) {
                    node = node2;
                } else {
                    Node node3 = node2.next;
                    if (node3 != null) {
                        if (node2 == node3) {
                            node = this.head;
                        } else {
                            node.casNext(node2, node3);
                        }
                    } else {
                        return;
                    }
                }
            } else {
                return;
            }
        }
    }

    private boolean findAndRemove(Object obj) {
        if (obj != null) {
            Node node = this.head;
            Node node2 = null;
            while (node != null) {
                Object obj2 = node.item;
                if (node.isData) {
                    if (obj2 != null && obj2 != node && obj.equals(obj2) && node.tryMatchData()) {
                        unsplice(node2, node);
                        return true;
                    }
                } else if (obj2 == null) {
                    return false;
                }
                Node node3 = node.next;
                if (node3 == node) {
                    node = this.head;
                    node2 = null;
                } else {
                    node2 = node;
                    node = node3;
                }
            }
            return false;
        }
        return false;
    }

    public LinkedTransferQueue() {
    }

    public LinkedTransferQueue(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }

    @Override
    public void put(E e) {
        xfer(e, true, 1, 0L);
    }

    @Override
    public boolean offer(E e, long j, TimeUnit timeUnit) {
        xfer(e, true, 1, 0L);
        return true;
    }

    @Override
    public boolean offer(E e) {
        xfer(e, true, 1, 0L);
        return true;
    }

    @Override
    public boolean add(E e) {
        xfer(e, true, 1, 0L);
        return true;
    }

    @Override
    public boolean tryTransfer(E e) {
        return xfer(e, true, 0, 0L) == null;
    }

    @Override
    public void transfer(E e) throws InterruptedException {
        if (xfer(e, true, 2, 0L) != null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    @Override
    public boolean tryTransfer(E e, long j, TimeUnit timeUnit) throws InterruptedException {
        if (xfer(e, true, 3, timeUnit.toNanos(j)) == null) {
            return true;
        }
        if (!Thread.interrupted()) {
            return false;
        }
        throw new InterruptedException();
    }

    @Override
    public E take() throws InterruptedException {
        E eXfer = xfer(null, false, 2, 0L);
        if (eXfer != null) {
            return eXfer;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    @Override
    public E poll(long j, TimeUnit timeUnit) throws InterruptedException {
        E eXfer = xfer(null, false, 3, timeUnit.toNanos(j));
        if (eXfer != null || !Thread.interrupted()) {
            return eXfer;
        }
        throw new InterruptedException();
    }

    @Override
    public E poll() {
        return xfer(null, false, 0, 0L);
    }

    @Override
    public int drainTo(Collection<? super E> collection) {
        if (collection == null) {
            throw new NullPointerException();
        }
        if (collection == this) {
            throw new IllegalArgumentException();
        }
        int i = 0;
        while (true) {
            E ePoll = poll();
            if (ePoll != null) {
                collection.add(ePoll);
                i++;
            } else {
                return i;
            }
        }
    }

    @Override
    public int drainTo(Collection<? super E> collection, int i) {
        if (collection == null) {
            throw new NullPointerException();
        }
        if (collection == this) {
            throw new IllegalArgumentException();
        }
        int i2 = 0;
        while (i2 < i) {
            E ePoll = poll();
            if (ePoll == null) {
                break;
            }
            collection.add(ePoll);
            i2++;
        }
        return i2;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public E peek() {
        while (true) {
            Node node = this.head;
            while (node != null) {
                E e = (E) node.item;
                if (node.isData) {
                    if (e != null && e != node) {
                        return e;
                    }
                } else if (e == null) {
                    return null;
                }
                Node node2 = node.next;
                if (node == node2) {
                    break;
                }
                node = node2;
            }
            return null;
        }
    }

    @Override
    public boolean isEmpty() {
        return firstDataNode() == null;
    }

    @Override
    public boolean hasWaitingConsumer() {
        while (true) {
            Node node = this.head;
            while (node != null) {
                Object obj = node.item;
                if (node.isData) {
                    if (obj != null && obj != node) {
                        return false;
                    }
                } else if (obj == null) {
                    return true;
                }
                Node node2 = node.next;
                if (node == node2) {
                    break;
                }
                node = node2;
            }
            return false;
        }
    }

    @Override
    public int size() {
        return countOfMode(true);
    }

    @Override
    public int getWaitingConsumerCount() {
        return countOfMode(false);
    }

    @Override
    public boolean remove(Object obj) {
        return findAndRemove(obj);
    }

    @Override
    public boolean contains(Object obj) {
        if (obj != null) {
            Node nodeSucc = this.head;
            while (nodeSucc != null) {
                Object obj2 = nodeSucc.item;
                if (nodeSucc.isData) {
                    if (obj2 != null && obj2 != nodeSucc && obj.equals(obj2)) {
                        return true;
                    }
                } else if (obj2 == null) {
                    return false;
                }
                nodeSucc = succ(nodeSucc);
            }
            return false;
        }
        return false;
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            objectOutputStream.writeObject(it.next());
        }
        objectOutputStream.writeObject(null);
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        while (true) {
            Object object = objectInputStream.readObject();
            if (object != null) {
                offer(object);
            } else {
                return;
            }
        }
    }
}
