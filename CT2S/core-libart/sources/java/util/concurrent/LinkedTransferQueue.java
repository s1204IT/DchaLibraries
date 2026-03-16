package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.LockSupport;
import sun.misc.Unsafe;

public class LinkedTransferQueue<E> extends AbstractQueue<E> implements TransferQueue<E>, Serializable {
    private static final int ASYNC = 1;
    private static final int CHAINED_SPINS = 64;
    private static final int FRONT_SPINS = 128;
    private static final boolean MP;
    private static final int NOW = 0;
    static final int SWEEP_THRESHOLD = 32;
    private static final int SYNC = 2;
    private static final int TIMED = 3;
    private static final Unsafe UNSAFE;
    private static final long headOffset;
    private static final long serialVersionUID = -3223113410248163686L;
    private static final long sweepVotesOffset;
    private static final long tailOffset;
    volatile transient Node head;
    private volatile transient int sweepVotes;
    private volatile transient Node tail;

    static {
        MP = Runtime.getRuntime().availableProcessors() > 1;
        try {
            UNSAFE = Unsafe.getUnsafe();
            headOffset = UNSAFE.objectFieldOffset(LinkedTransferQueue.class.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(LinkedTransferQueue.class.getDeclaredField("tail"));
            sweepVotesOffset = UNSAFE.objectFieldOffset(LinkedTransferQueue.class.getDeclaredField("sweepVotes"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static final class Node {
        private static final Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;
        private static final long serialVersionUID = -3375979862319811754L;
        private static final long waiterOffset;
        final boolean isData;
        volatile Object item;
        volatile Node next;
        volatile Thread waiter;

        final boolean casNext(Node cmp, Node val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        final boolean casItem(Object cmp, Object val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        Node(Object item, boolean isData) {
            UNSAFE.putObject(this, itemOffset, item);
            this.isData = isData;
        }

        final void forgetNext() {
            UNSAFE.putObject(this, nextOffset, this);
        }

        final void forgetContents() {
            UNSAFE.putObject(this, itemOffset, this);
            UNSAFE.putObject(this, waiterOffset, null);
        }

        final boolean isMatched() {
            Object x = this.item;
            if (x != this) {
                if ((x == null) != this.isData) {
                    return false;
                }
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
            if (x == null || x == this || !casItem(x, null)) {
                return false;
            }
            LockSupport.unpark(this.waiter);
            return true;
        }

        static {
            try {
                UNSAFE = Unsafe.getUnsafe();
                itemOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
                waiterOffset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("waiter"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    private boolean casTail(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casSweepVotes(int cmp, int val) {
        return UNSAFE.compareAndSwapInt(this, sweepVotesOffset, cmp, val);
    }

    static <E> E cast(Object obj) {
        return obj;
    }

    private E xfer(E e, boolean z, int i, long j) {
        Node nodeTryAppend;
        if (z && e == null) {
            throw new NullPointerException();
        }
        Node node = null;
        do {
            Node node2 = this.head;
            Node node3 = node2;
            while (node3 != null) {
                boolean z2 = node3.isData;
                Object obj = node3.item;
                if (obj != node3) {
                    if ((obj != null) == z2) {
                        if (z2 == z) {
                            break;
                        }
                        if (node3.casItem(obj, e)) {
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
                            return (E) cast(obj);
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
            if (i != 0) {
                if (node == null) {
                    node = new Node(e, z);
                }
                nodeTryAppend = tryAppend(node, z);
            } else {
                return e;
            }
        } while (nodeTryAppend == null);
        if (i != 1) {
            return awaitMatch(node, nodeTryAppend, e, i == 3, j);
        }
        return e;
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
            Object obj = node.item;
            if (obj != e) {
                node.forgetContents();
                return (E) cast(obj);
            }
            if ((threadCurrentThread.isInterrupted() || (z && j <= 0)) && node.casItem(e, node)) {
                unsplice(node2, node);
                return e;
            }
            if (iSpinsFor < 0) {
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
        }
        return 0;
    }

    final Node succ(Node p) {
        Node next = p.next;
        return p == next ? this.head : next;
    }

    private Node firstOfMode(boolean isData) {
        Node p = this.head;
        while (p != null) {
            if (p.isMatched()) {
                p = succ(p);
            } else {
                if (p.isData == isData) {
                    return p;
                }
                return null;
            }
        }
        return null;
    }

    private E firstDataItem() {
        Node nodeSucc = this.head;
        while (nodeSucc != null) {
            Object obj = nodeSucc.item;
            if (nodeSucc.isData) {
                if (obj != null && obj != nodeSucc) {
                    return (E) cast(obj);
                }
            } else if (obj == null) {
                return null;
            }
            nodeSucc = succ(nodeSucc);
        }
        return null;
    }

    private int countOfMode(boolean data) {
        int count = 0;
        Node p = this.head;
        while (p != null) {
            if (!p.isMatched()) {
                if (p.isData != data) {
                    return 0;
                }
                count++;
                if (count == Integer.MAX_VALUE) {
                    break;
                }
            }
            Node n = p.next;
            if (n != p) {
                p = n;
            } else {
                count = 0;
                p = this.head;
            }
        }
        return count;
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
                    Object obj = node7.item;
                    if (node7.isData) {
                        if (obj != null && obj != node7) {
                            this.nextItem = (E) LinkedTransferQueue.cast(obj);
                            this.nextNode = node7;
                            return;
                        }
                    } else if (obj == null) {
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
            if (lastRet.tryMatchData()) {
                LinkedTransferQueue.this.unsplice(this.lastPred, lastRet);
            }
        }
    }

    final void unsplice(Node pred, Node s) {
        s.forgetContents();
        if (pred != null && pred != s && pred.next == s) {
            Node n = s.next;
            if (n != null && (n == s || !pred.casNext(s, n) || !pred.isMatched())) {
                return;
            }
            while (true) {
                Node h = this.head;
                if (h != pred && h != s && h != null) {
                    if (h.isMatched()) {
                        Node hn = h.next;
                        if (hn != null) {
                            if (hn != h && casHead(h, hn)) {
                                h.forgetNext();
                            }
                        } else {
                            return;
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
                } else {
                    return;
                }
            }
        }
    }

    private void sweep() {
        Node p = this.head;
        while (p != null) {
            Node s = p.next;
            if (s != null) {
                if (!s.isMatched()) {
                    p = s;
                } else {
                    Node n = s.next;
                    if (n != null) {
                        if (s == n) {
                            p = this.head;
                        } else {
                            p.casNext(s, n);
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
                    break;
                }
                pred = p;
                p = p.next;
                if (p == pred) {
                    pred = null;
                    p = this.head;
                }
            }
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
        if (xfer(e, true, 2, 0L) != null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
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
        return firstDataItem();
    }

    @Override
    public boolean isEmpty() {
        Node p = this.head;
        while (p != null) {
            if (!p.isMatched()) {
                return !p.isData;
            }
            p = succ(p);
        }
        return true;
    }

    @Override
    public boolean hasWaitingConsumer() {
        return firstOfMode(false) != null;
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
        if (o == null) {
            return false;
        }
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

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        while (true) {
            Object object = s.readObject();
            if (object != null) {
                offer(object);
            } else {
                return;
            }
        }
    }
}
