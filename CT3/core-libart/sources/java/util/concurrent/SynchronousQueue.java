package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

public class SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, Serializable {
    static final int MAX_TIMED_SPINS;
    static final int MAX_UNTIMED_SPINS;
    static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000;
    private static final long serialVersionUID = -3223113410248163686L;
    private ReentrantLock qlock;
    private volatile transient Transferer<E> transferer;
    private WaitQueue waitingConsumers;
    private WaitQueue waitingProducers;

    static abstract class Transferer<E> {
        abstract E transfer(E e, boolean z, long j);

        Transferer() {
        }
    }

    static {
        MAX_TIMED_SPINS = Runtime.getRuntime().availableProcessors() < 2 ? 0 : 32;
        MAX_UNTIMED_SPINS = MAX_TIMED_SPINS * 16;
    }

    static final class TransferStack<E> extends Transferer<E> {
        static final int DATA = 1;
        static final int FULFILLING = 2;
        private static final long HEAD;
        static final int REQUEST = 0;
        private static final Unsafe U = Unsafe.getUnsafe();
        volatile SNode head;

        TransferStack() {
        }

        static boolean isFulfilling(int m) {
            return (m & 2) != 0;
        }

        static final class SNode {
            private static final long MATCH;
            private static final long NEXT;
            private static final Unsafe U = Unsafe.getUnsafe();
            Object item;
            volatile SNode match;
            int mode;
            volatile SNode next;
            volatile Thread waiter;

            SNode(Object item) {
                this.item = item;
            }

            boolean casNext(SNode cmp, SNode val) {
                if (cmp == this.next) {
                    return U.compareAndSwapObject(this, NEXT, cmp, val);
                }
                return false;
            }

            boolean tryMatch(SNode s) {
                if (this.match != null || !U.compareAndSwapObject(this, MATCH, (Object) null, s)) {
                    return this.match == s;
                }
                Thread w = this.waiter;
                if (w != null) {
                    this.waiter = null;
                    LockSupport.unpark(w);
                }
                return true;
            }

            void tryCancel() {
                U.compareAndSwapObject(this, MATCH, (Object) null, this);
            }

            boolean isCancelled() {
                return this.match == this;
            }

            static {
                try {
                    MATCH = U.objectFieldOffset(SNode.class.getDeclaredField("match"));
                    NEXT = U.objectFieldOffset(SNode.class.getDeclaredField("next"));
                } catch (ReflectiveOperationException e) {
                    throw new Error(e);
                }
            }
        }

        boolean casHead(SNode h, SNode nh) {
            if (h == this.head) {
                return U.compareAndSwapObject(this, HEAD, h, nh);
            }
            return false;
        }

        static SNode snode(SNode s, Object e, SNode next, int mode) {
            if (s == null) {
                s = new SNode(e);
            }
            s.mode = mode;
            s.next = next;
            return s;
        }

        @Override
        E transfer(E e, boolean z, long j) {
            SNode snode = null;
            int i = e == null ? 0 : 1;
            while (true) {
                SNode sNode = this.head;
                if (sNode == null || sNode.mode == i) {
                    if (z && j <= 0) {
                        if (sNode == null || !sNode.isCancelled()) {
                            break;
                        }
                        casHead(sNode, sNode.next);
                    } else {
                        snode = snode(snode, e, sNode, i);
                        if (casHead(sNode, snode)) {
                            SNode sNodeAwaitFulfill = awaitFulfill(snode, z, j);
                            if (sNodeAwaitFulfill == snode) {
                                clean(snode);
                                return null;
                            }
                            SNode sNode2 = this.head;
                            if (sNode2 != null && sNode2.next == snode) {
                                casHead(sNode2, snode.next);
                            }
                            return i == 0 ? (E) sNodeAwaitFulfill.item : (E) snode.item;
                        }
                    }
                } else if (!isFulfilling(sNode.mode)) {
                    if (sNode.isCancelled()) {
                        casHead(sNode, sNode.next);
                    } else {
                        snode = snode(snode, e, sNode, i | 2);
                        if (casHead(sNode, snode)) {
                            while (true) {
                                SNode sNode3 = snode.next;
                                if (sNode3 == null) {
                                    casHead(snode, null);
                                    snode = null;
                                    break;
                                }
                                SNode sNode4 = sNode3.next;
                                if (sNode3.tryMatch(snode)) {
                                    casHead(snode, sNode4);
                                    return i == 0 ? (E) sNode3.item : (E) snode.item;
                                }
                                snode.casNext(sNode3, sNode4);
                            }
                        } else {
                            continue;
                        }
                    }
                } else {
                    SNode sNode5 = sNode.next;
                    if (sNode5 == null) {
                        casHead(sNode, null);
                    } else {
                        SNode sNode6 = sNode5.next;
                        if (sNode5.tryMatch(sNode)) {
                            casHead(sNode, sNode6);
                        } else {
                            sNode.casNext(sNode5, sNode6);
                        }
                    }
                }
            }
        }

        SNode awaitFulfill(SNode s, boolean timed, long nanos) {
            int spins;
            long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            if (shouldSpin(s)) {
                spins = timed ? SynchronousQueue.MAX_TIMED_SPINS : SynchronousQueue.MAX_UNTIMED_SPINS;
            } else {
                spins = 0;
            }
            while (true) {
                if (w.isInterrupted()) {
                    s.tryCancel();
                }
                SNode m = s.match;
                if (m != null) {
                    return m;
                }
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0) {
                        s.tryCancel();
                    }
                }
                if (spins > 0) {
                    spins = shouldSpin(s) ? spins - 1 : 0;
                } else if (s.waiter == null) {
                    s.waiter = w;
                } else if (!timed) {
                    LockSupport.park(this);
                } else if (nanos > 1000) {
                    LockSupport.parkNanos(this, nanos);
                }
            }
        }

        boolean shouldSpin(SNode s) {
            SNode h = this.head;
            if (h == s || h == null) {
                return true;
            }
            return isFulfilling(h.mode);
        }

        void clean(SNode s) {
            SNode p;
            s.item = null;
            s.waiter = null;
            SNode past = s.next;
            if (past != null && past.isCancelled()) {
                past = past.next;
            }
            while (true) {
                p = this.head;
                if (p == null || p == past || !p.isCancelled()) {
                    break;
                } else {
                    casHead(p, p.next);
                }
            }
            while (p != null && p != past) {
                SNode n = p.next;
                if (n != null && n.isCancelled()) {
                    p.casNext(n, n.next);
                } else {
                    p = n;
                }
            }
        }

        static {
            try {
                HEAD = U.objectFieldOffset(TransferStack.class.getDeclaredField("head"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    static final class TransferQueue<E> extends Transferer<E> {
        private static final long CLEANME;
        private static final long HEAD;
        private static final long TAIL;
        private static final Unsafe U = Unsafe.getUnsafe();
        volatile transient QNode cleanMe;
        volatile transient QNode head;
        volatile transient QNode tail;

        static final class QNode {
            private static final long ITEM;
            private static final long NEXT;
            private static final Unsafe U = Unsafe.getUnsafe();
            final boolean isData;
            volatile Object item;
            volatile QNode next;
            volatile Thread waiter;

            QNode(Object item, boolean isData) {
                this.item = item;
                this.isData = isData;
            }

            boolean casNext(QNode cmp, QNode val) {
                if (this.next == cmp) {
                    return U.compareAndSwapObject(this, NEXT, cmp, val);
                }
                return false;
            }

            boolean casItem(Object cmp, Object val) {
                if (this.item == cmp) {
                    return U.compareAndSwapObject(this, ITEM, cmp, val);
                }
                return false;
            }

            void tryCancel(Object cmp) {
                U.compareAndSwapObject(this, ITEM, cmp, this);
            }

            boolean isCancelled() {
                return this.item == this;
            }

            boolean isOffList() {
                return this.next == this;
            }

            static {
                try {
                    ITEM = U.objectFieldOffset(QNode.class.getDeclaredField("item"));
                    NEXT = U.objectFieldOffset(QNode.class.getDeclaredField("next"));
                } catch (ReflectiveOperationException e) {
                    throw new Error(e);
                }
            }
        }

        TransferQueue() {
            QNode h = new QNode(null, false);
            this.head = h;
            this.tail = h;
        }

        void advanceHead(QNode h, QNode nh) {
            if (h != this.head || !U.compareAndSwapObject(this, HEAD, h, nh)) {
                return;
            }
            h.next = h;
        }

        void advanceTail(QNode t, QNode nt) {
            if (this.tail != t) {
                return;
            }
            U.compareAndSwapObject(this, TAIL, t, nt);
        }

        boolean casCleanMe(QNode cmp, QNode val) {
            if (this.cleanMe == cmp) {
                return U.compareAndSwapObject(this, CLEANME, cmp, val);
            }
            return false;
        }

        @Override
        E transfer(E e, boolean z, long j) {
            QNode qNode = null;
            boolean z2 = e != null;
            while (true) {
                QNode qNode2 = this.tail;
                QNode qNode3 = this.head;
                if (qNode2 != null && qNode3 != null) {
                    if (qNode3 == qNode2 || qNode2.isData == z2) {
                        QNode qNode4 = qNode2.next;
                        if (qNode2 != this.tail) {
                            continue;
                        } else if (qNode4 != null) {
                            advanceTail(qNode2, qNode4);
                        } else {
                            if (z && j <= 0) {
                                return null;
                            }
                            if (qNode == null) {
                                qNode = new QNode(e, z2);
                            }
                            if (qNode2.casNext(null, qNode)) {
                                advanceTail(qNode2, qNode);
                                E e2 = (E) awaitFulfill(qNode, e, z, j);
                                if (e2 == qNode) {
                                    clean(qNode2, qNode);
                                    return null;
                                }
                                if (!qNode.isOffList()) {
                                    advanceHead(qNode2, qNode);
                                    if (e2 != null) {
                                        qNode.item = qNode;
                                    }
                                    qNode.waiter = null;
                                }
                                return e2 != null ? e2 : e;
                            }
                        }
                    } else {
                        QNode qNode5 = qNode3.next;
                        if (qNode2 == this.tail && qNode5 != null && qNode3 == this.head) {
                            E e3 = (E) qNode5.item;
                            if (z2 == (e3 != null) || e3 == qNode5 || !qNode5.casItem(e3, e)) {
                                advanceHead(qNode3, qNode5);
                            } else {
                                advanceHead(qNode3, qNode5);
                                LockSupport.unpark(qNode5.waiter);
                                return e3 != null ? e3 : e;
                            }
                        }
                    }
                }
            }
        }

        Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {
            int spins;
            long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            if (this.head.next == s) {
                spins = timed ? SynchronousQueue.MAX_TIMED_SPINS : SynchronousQueue.MAX_UNTIMED_SPINS;
            } else {
                spins = 0;
            }
            while (true) {
                if (w.isInterrupted()) {
                    s.tryCancel(e);
                }
                Object x = s.item;
                if (x != e) {
                    return x;
                }
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0) {
                        s.tryCancel(e);
                    }
                }
                if (spins > 0) {
                    spins--;
                } else if (s.waiter == null) {
                    s.waiter = w;
                } else if (!timed) {
                    LockSupport.park(this);
                } else if (nanos > 1000) {
                    LockSupport.parkNanos(this, nanos);
                }
            }
        }

        void clean(QNode pred, QNode s) {
            QNode dn;
            QNode sn;
            s.waiter = null;
            while (pred.next == s) {
                QNode h = this.head;
                QNode hn = h.next;
                if (hn != null && hn.isCancelled()) {
                    advanceHead(h, hn);
                } else {
                    QNode t = this.tail;
                    if (t == h) {
                        return;
                    }
                    QNode tn = t.next;
                    if (t != this.tail) {
                        continue;
                    } else if (tn != null) {
                        advanceTail(t, tn);
                    } else {
                        if (s != t && ((sn = s.next) == s || pred.casNext(s, sn))) {
                            return;
                        }
                        QNode dp = this.cleanMe;
                        if (dp != null) {
                            QNode d = dp.next;
                            if (d == null || d == dp || !d.isCancelled() || (d != t && (dn = d.next) != null && dn != d && dp.casNext(d, dn))) {
                                casCleanMe(dp, null);
                            }
                            if (dp == pred) {
                                return;
                            }
                        } else if (casCleanMe(null, pred)) {
                            return;
                        }
                    }
                }
            }
        }

        static {
            try {
                HEAD = U.objectFieldOffset(TransferQueue.class.getDeclaredField("head"));
                TAIL = U.objectFieldOffset(TransferQueue.class.getDeclaredField("tail"));
                CLEANME = U.objectFieldOffset(TransferQueue.class.getDeclaredField("cleanMe"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    public SynchronousQueue() {
        this(false);
    }

    public SynchronousQueue(boolean fair) {
        this.transferer = fair ? new TransferQueue<>() : new TransferStack<>();
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        if (this.transferer.transfer(e, false, 0L) != null) {
            return;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        if (this.transferer.transfer(e, true, unit.toNanos(timeout)) != null) {
            return true;
        }
        if (!Thread.interrupted()) {
            return false;
        }
        throw new InterruptedException();
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        return this.transferer.transfer(e, true, 0L) != null;
    }

    @Override
    public E take() throws InterruptedException {
        E e = this.transferer.transfer(null, false, 0L);
        if (e != null) {
            return e;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = this.transferer.transfer(null, true, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted()) {
            return e;
        }
        throw new InterruptedException();
    }

    @Override
    public E poll() {
        return this.transferer.transfer(null, true, 0L);
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public int remainingCapacity() {
        return 0;
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public E peek() {
        return null;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.emptySpliterator();
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length > 0) {
            a[0] = null;
        }
        return a;
    }

    @Override
    public String toString() {
        return "[]";
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

    static class WaitQueue implements Serializable {
        WaitQueue() {
        }
    }

    static class LifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3633113410248163686L;

        LifoWaitQueue() {
        }
    }

    static class FifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3623113410248163686L;

        FifoWaitQueue() {
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        boolean fair = this.transferer instanceof TransferQueue;
        if (fair) {
            this.qlock = new ReentrantLock(true);
            this.waitingProducers = new FifoWaitQueue();
            this.waitingConsumers = new FifoWaitQueue();
        } else {
            this.qlock = new ReentrantLock();
            this.waitingProducers = new LifoWaitQueue();
            this.waitingConsumers = new LifoWaitQueue();
        }
        s.defaultWriteObject();
    }

    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        s.defaultReadObject();
        if (this.waitingProducers instanceof FifoWaitQueue) {
            this.transferer = new TransferQueue();
        } else {
            this.transferer = new TransferStack();
        }
    }
}
