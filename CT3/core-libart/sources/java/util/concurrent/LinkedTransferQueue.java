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

        final boolean casNext(Node cmp, Node val) {
            return U.compareAndSwapObject(this, NEXT, cmp, val);
        }

        final boolean casItem(Object cmp, Object val) {
            return U.compareAndSwapObject(this, ITEM, cmp, val);
        }

        Node(Object item, boolean isData) {
            U.putObject(this, ITEM, item);
            this.isData = isData;
        }

        final void forgetNext() {
            U.putObject(this, NEXT, this);
        }

        final void forgetContents() {
            U.putObject(this, ITEM, this);
            U.putObject(this, WAITER, (Object) null);
        }

        final boolean isMatched() {
            Object x = this.item;
            if (x != this) {
                return (x == null) == this.isData;
            }
            return true;
        }

        final boolean isUnmatchedRequest() {
            return !this.isData && this.item == null;
        }

        final boolean cannotPrecede(boolean haveData) {
            Object x;
            boolean d = this.isData;
            if (d != haveData && (x = this.item) != this) {
                if ((x != null) == d) {
                    return true;
                }
            }
            return false;
        }

        final boolean tryMatchData() {
            Object x = this.item;
            if (x != null && x != this && casItem(x, null)) {
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

    private boolean casTail(Node cmp, Node val) {
        return U.compareAndSwapObject(this, TAIL, cmp, val);
    }

    private boolean casHead(Node cmp, Node val) {
        return U.compareAndSwapObject(this, HEAD, cmp, val);
    }

    private boolean casSweepVotes(int cmp, int val) {
        return U.compareAndSwapInt(this, SWEEPVOTES, cmp, val);
    }

    private E xfer(E e, boolean z, int i, long j) {
        if (z && e == null) {
            throw new NullPointerException();
        }
        Node node = null;
        while (true) {
            Node node2 = this.head;
            Node node3 = node2;
            while (node3 != null) {
                boolean z2 = node3.isData;
                E e2 = (E) node3.item;
                if (e2 != node3) {
                    if ((e2 != null) == z2) {
                        if (z2 == z) {
                            break;
                        }
                        if (node3.casItem(e2, e)) {
                            Node node4 = node3;
                            while (true) {
                                if (node4 == node2) {
                                    break;
                                }
                                Node node5 = node4.next;
                                if (this.head != node2) {
                                    node2 = this.head;
                                    if (node2 == null || (node4 = node2.next) == null || !node4.isMatched()) {
                                        break;
                                    }
                                } else {
                                    if (node5 == null) {
                                        node5 = node4;
                                    }
                                    if (casHead(node2, node5)) {
                                        node2.forgetNext();
                                        break;
                                    }
                                }
                            }
                            LockSupport.unpark(node3.waiter);
                            return e2;
                        }
                    }
                }
                Node node6 = node3.next;
                if (node3 != node6) {
                    node3 = node6;
                } else {
                    node2 = this.head;
                    node3 = node2;
                }
            }
            if (i == 0) {
                break;
            }
            if (node == null) {
                node = new Node(e, z);
            }
            Node nodeTryAppend = tryAppend(node, z);
            if (nodeTryAppend != null) {
                if (i != 1) {
                    return awaitMatch(node, nodeTryAppend, e, i == 3, j);
                }
            }
        }
    }

    private Node tryAppend(Node s, boolean haveData) {
        Node u;
        Node s2;
        Node t = this.tail;
        Node p = t;
        while (true) {
            if (p == null) {
                p = this.head;
                if (p == null) {
                    if (casHead(null, s)) {
                        return s;
                    }
                }
            }
            if (p.cannotPrecede(haveData)) {
                return null;
            }
            Node n = p.next;
            if (n != null) {
                if (p == t || t == (u = this.tail)) {
                    p = p != n ? n : null;
                } else {
                    t = u;
                    p = u;
                }
            } else if (!p.casNext(null, s)) {
                p = p.next;
            } else {
                if (p != t) {
                    do {
                        if ((this.tail == t && casTail(t, s)) || (t = this.tail) == null || (s2 = t.next) == null || (s = s2.next) == null) {
                            break;
                        }
                    } while (s != t);
                }
                return p;
            }
        }
    }

    private E awaitMatch(Node node, Node node2, E e, boolean z, long j) {
        long jNanoTime = z ? System.nanoTime() + j : 0L;
        Thread threadCurrentThread = Thread.currentThread();
        int iSpinsFor = -1;
        ThreadLocalRandom threadLocalRandomCurrent = null;
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

    private static int spinsFor(Node pred, boolean haveData) {
        if (MP && pred != null) {
            if (pred.isData != haveData) {
                return 192;
            }
            if (pred.isMatched()) {
                return 128;
            }
            if (pred.waiter == null) {
                return 64;
            }
            return 0;
        }
        return 0;
    }

    final Node succ(Node p) {
        Node next = p.next;
        return p == next ? this.head : next;
    }

    final Node firstDataNode() {
        loop0: while (true) {
            Node p = this.head;
            Node p2 = p;
            while (p2 != null) {
                Object item = p2.item;
                if (p2.isData) {
                    if (item != null && item != p2) {
                        return p2;
                    }
                } else if (item == null) {
                    break loop0;
                }
                Node p3 = p2.next;
                if (p2 != p3) {
                    p2 = p3;
                }
            }
        }
        return null;
    }

    private int countOfMode(boolean data) {
        int count;
        loop0: while (true) {
            count = 0;
            Node p = this.head;
            Node p2 = p;
            while (p2 != null) {
                if (!p2.isMatched()) {
                    if (p2.isData != data) {
                        return 0;
                    }
                    count++;
                    if (count == Integer.MAX_VALUE) {
                        break loop0;
                    }
                }
                Node p3 = p2.next;
                if (p2 != p3) {
                    p2 = p3;
                }
            }
        }
        return count;
    }

    @Override
    public String toString() {
        int charLength;
        int size;
        int size2;
        String[] strArr = null;
        loop0: while (true) {
            charLength = 0;
            Node p = this.head;
            Node p2 = p;
            size = 0;
            while (p2 != null) {
                Object item = p2.item;
                if (p2.isData) {
                    if (item == null || item == p2) {
                        size2 = size;
                    } else {
                        if (strArr == null) {
                            strArr = new String[4];
                        } else if (size == strArr.length) {
                            strArr = (String[]) Arrays.copyOf(strArr, size * 2);
                        }
                        String s = item.toString();
                        size2 = size + 1;
                        strArr[size] = s;
                        charLength += s.length();
                    }
                } else {
                    if (item == null) {
                        break loop0;
                    }
                    size2 = size;
                }
                Node p3 = p2.next;
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
            Node p = this.head;
            Node p2 = p;
            size = 0;
            while (p2 != null) {
                Object item = p2.item;
                if (p2.isData) {
                    if (item == null || item == p2) {
                        size2 = size;
                    } else {
                        if (x == null) {
                            x = new Object[4];
                        } else if (size == x.length) {
                            x = Arrays.copyOf(x, (size + 4) * 2);
                        }
                        size2 = size + 1;
                        x[size] = item;
                    }
                } else {
                    if (item == null) {
                        break loop0;
                    }
                    size2 = size;
                }
                Node p3 = p2.next;
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
            Node node6 = node;
            while (true) {
                Node node7 = node6 == null ? LinkedTransferQueue.this.head : node6.next;
                if (node7 == null) {
                    break;
                }
                if (node7 == node6) {
                    node6 = null;
                } else {
                    E e = (E) node7.item;
                    if (node7.isData) {
                        if (e != null && e != node7) {
                            this.nextItem = e;
                            this.nextNode = node7;
                            return;
                        }
                    } else if (e == null) {
                        break;
                    }
                    if (node6 == null) {
                        node6 = node7;
                    } else {
                        Node node8 = node7.next;
                        if (node8 == null) {
                            break;
                        } else if (node7 == node8) {
                            node6 = null;
                        } else {
                            node6.casNext(node7, node8);
                        }
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
            Node p = this.nextNode;
            if (p == null) {
                throw new NoSuchElementException();
            }
            E e = this.nextItem;
            advance(p);
            return e;
        }

        @Override
        public final void remove() {
            Node lastRet = this.lastRet;
            if (lastRet == null) {
                throw new IllegalStateException();
            }
            this.lastRet = null;
            if (!lastRet.tryMatchData()) {
                return;
            }
            LinkedTransferQueue.this.unsplice(this.lastPred, lastRet);
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
            int n;
            Node p;
            int b = this.batch;
            if (b <= 0) {
                n = 1;
            } else {
                n = b >= MAX_BATCH ? MAX_BATCH : b + 1;
            }
            if (!this.exhausted && (((p = this.current) != null || (p = LinkedTransferQueue.this.firstDataNode()) != null) && p.next != null)) {
                Object[] a = new Object[n];
                int i = 0;
                do {
                    Object e = p.item;
                    if (e != p) {
                        a[i] = e;
                        if (e != null) {
                            i++;
                        }
                    }
                    Node p2 = p.next;
                    p = p == p2 ? LinkedTransferQueue.this.firstDataNode() : p2;
                    if (p == null || i >= n) {
                        break;
                    }
                } while (p.isData);
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
        public void forEachRemaining(Consumer<? super E> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            if (this.exhausted) {
                return;
            }
            Node p = this.current;
            if (p == null && (p = LinkedTransferQueue.this.firstDataNode()) == null) {
                return;
            }
            this.exhausted = true;
            do {
                Object e = p.item;
                if (e != null && e != p) {
                    action.accept(e);
                }
                Node p2 = p.next;
                p = p == p2 ? LinkedTransferQueue.this.firstDataNode() : p2;
                if (p == null) {
                    return;
                }
            } while (p.isData);
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> action) {
            Object e;
            if (action == null) {
                throw new NullPointerException();
            }
            if (!this.exhausted) {
                Node p = this.current;
                if (p != null || (p = LinkedTransferQueue.this.firstDataNode()) != null) {
                    do {
                        e = p.item;
                        if (e == p) {
                            e = null;
                        }
                        Node p2 = p.next;
                        p = p == p2 ? LinkedTransferQueue.this.firstDataNode() : p2;
                        if (e != null || p == null) {
                            break;
                        }
                    } while (p.isData);
                    this.current = p;
                    if (p == null) {
                        this.exhausted = true;
                    }
                    if (e != null) {
                        action.accept(e);
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
        return new LTQSpliterator();
    }

    final void unsplice(Node pred, Node s) {
        s.waiter = null;
        if (pred == null || pred == s || pred.next != s) {
            return;
        }
        Node n = s.next;
        if (n != null && (n == s || !pred.casNext(s, n) || !pred.isMatched())) {
            return;
        }
        while (true) {
            Node h = this.head;
            if (h == pred || h == s || h == null) {
                return;
            }
            if (h.isMatched()) {
                Node hn = h.next;
                if (hn == null) {
                    return;
                }
                if (hn != h && casHead(h, hn)) {
                    h.forgetNext();
                }
            } else {
                if (pred.next == pred || s.next == s) {
                    return;
                }
                while (true) {
                    int v = this.sweepVotes;
                    if (v < 32) {
                        if (casSweepVotes(v, v + 1)) {
                            return;
                        }
                    } else if (casSweepVotes(v, 0)) {
                        sweep();
                        return;
                    }
                }
            }
        }
    }

    private void sweep() {
        Node p = this.head;
        while (p != null) {
            Node s = p.next;
            if (s == null) {
                return;
            }
            if (!s.isMatched()) {
                p = s;
            } else {
                Node n = s.next;
                if (n == null) {
                    return;
                }
                if (s == n) {
                    p = this.head;
                } else {
                    p.casNext(s, n);
                }
            }
        }
    }

    private boolean findAndRemove(Object e) {
        if (e != null) {
            Node pred = null;
            Node p = this.head;
            while (p != null) {
                Object item = p.item;
                if (p.isData) {
                    if (item != null && item != p && e.equals(item) && p.tryMatchData()) {
                        unsplice(pred, p);
                        return true;
                    }
                } else if (item == null) {
                    return false;
                }
                pred = p;
                p = p.next;
                if (p == pred) {
                    pred = null;
                    p = this.head;
                }
            }
            return false;
        }
        return false;
    }

    public LinkedTransferQueue() {
    }

    public LinkedTransferQueue(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    @Override
    public void put(E e) {
        xfer(e, true, 1, 0L);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
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
        if (xfer(e, true, 2, 0L) == null) {
            return;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    @Override
    public boolean tryTransfer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (xfer(e, true, 3, unit.toNanos(timeout)) == null) {
            return true;
        }
        if (!Thread.interrupted()) {
            return false;
        }
        throw new InterruptedException();
    }

    @Override
    public E take() throws InterruptedException {
        E e = xfer(null, false, 2, 0L);
        if (e != null) {
            return e;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = xfer(null, false, 3, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted()) {
            return e;
        }
        throw new InterruptedException();
    }

    @Override
    public E poll() {
        return xfer(null, false, 0, 0L);
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        int n = 0;
        while (true) {
            E e = poll();
            if (e != null) {
                c.add(e);
                n++;
            } else {
                return n;
            }
        }
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        int n = 0;
        while (n < maxElements) {
            E e = poll();
            if (e == null) {
                break;
            }
            c.add(e);
            n++;
        }
        return n;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public E peek() {
        loop0: while (true) {
            Node node = this.head;
            while (node != null) {
                E e = (E) node.item;
                if (node.isData) {
                    if (e != null && e != node) {
                        return e;
                    }
                } else if (e == null) {
                    break loop0;
                }
                Node node2 = node.next;
                if (node != node2) {
                    node = node2;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return firstDataNode() == null;
    }

    @Override
    public boolean hasWaitingConsumer() {
        while (true) {
            Node p = this.head;
            Node p2 = p;
            while (p2 != null) {
                Object item = p2.item;
                if (p2.isData) {
                    if (item != null && item != p2) {
                        return false;
                    }
                } else if (item == null) {
                    return true;
                }
                Node p3 = p2.next;
                if (p2 != p3) {
                    p2 = p3;
                }
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
    public boolean remove(Object o) {
        return findAndRemove(o);
    }

    @Override
    public boolean contains(Object o) {
        if (o != null) {
            Node p = this.head;
            while (p != null) {
                Object item = p.item;
                if (p.isData) {
                    if (item != null && item != p && o.equals(item)) {
                        return true;
                    }
                } else if (item == null) {
                    return false;
                }
                p = succ(p);
            }
            return false;
        }
        return false;
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        for (E e : this) {
            s.writeObject(e);
        }
        s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        s.defaultReadObject();
        while (true) {
            Object object = s.readObject();
            if (object == null) {
                return;
            } else {
                offer(object);
            }
        }
    }
}
