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

        static boolean isFulfilling(int i) {
            return (i & 2) != 0;
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

            SNode(Object obj) {
                this.item = obj;
            }

            boolean casNext(SNode sNode, SNode sNode2) {
                return sNode == this.next && U.compareAndSwapObject(this, NEXT, sNode, sNode2);
            }

            boolean tryMatch(SNode sNode) {
                if (this.match != null || !U.compareAndSwapObject(this, MATCH, null, sNode)) {
                    return this.match == sNode;
                }
                Thread thread = this.waiter;
                if (thread != null) {
                    this.waiter = null;
                    LockSupport.unpark(thread);
                }
                return true;
            }

            void tryCancel() {
                U.compareAndSwapObject(this, MATCH, null, this);
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

        boolean casHead(SNode sNode, SNode sNode2) {
            return sNode == this.head && U.compareAndSwapObject(this, HEAD, sNode, sNode2);
        }

        static SNode snode(SNode sNode, Object obj, SNode sNode2, int i) {
            if (sNode == null) {
                sNode = new SNode(obj);
            }
            sNode.mode = i;
            sNode.next = sNode2;
            return sNode;
        }

        @Override
        E transfer(E e, boolean z, long j) {
            int i = e == null ? 0 : 1;
            SNode snode = null;
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
                        snode = snode(snode, e, sNode, 2 | i);
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

        SNode awaitFulfill(SNode sNode, boolean z, long j) {
            int i;
            long jNanoTime = z ? System.nanoTime() + j : 0L;
            Thread threadCurrentThread = Thread.currentThread();
            if (shouldSpin(sNode)) {
                i = z ? SynchronousQueue.MAX_TIMED_SPINS : SynchronousQueue.MAX_UNTIMED_SPINS;
            } else {
                i = 0;
            }
            while (true) {
                if (threadCurrentThread.isInterrupted()) {
                    sNode.tryCancel();
                }
                SNode sNode2 = sNode.match;
                if (sNode2 != null) {
                    return sNode2;
                }
                if (z) {
                    j = jNanoTime - System.nanoTime();
                    if (j <= 0) {
                        sNode.tryCancel();
                    }
                }
                if (i <= 0) {
                    if (sNode.waiter == null) {
                        sNode.waiter = threadCurrentThread;
                    } else if (!z) {
                        LockSupport.park(this);
                    } else if (j > SynchronousQueue.SPIN_FOR_TIMEOUT_THRESHOLD) {
                        LockSupport.parkNanos(this, j);
                    }
                } else {
                    i = shouldSpin(sNode) ? i - 1 : 0;
                }
            }
        }

        boolean shouldSpin(SNode sNode) {
            SNode sNode2 = this.head;
            return sNode2 == sNode || sNode2 == null || isFulfilling(sNode2.mode);
        }

        void clean(SNode sNode) {
            SNode sNode2;
            sNode.item = null;
            sNode.waiter = null;
            SNode sNode3 = sNode.next;
            if (sNode3 != null && sNode3.isCancelled()) {
                sNode3 = sNode3.next;
            }
            while (true) {
                sNode2 = this.head;
                if (sNode2 == null || sNode2 == sNode3 || !sNode2.isCancelled()) {
                    break;
                } else {
                    casHead(sNode2, sNode2.next);
                }
            }
            while (sNode2 != null && sNode2 != sNode3) {
                SNode sNode4 = sNode2.next;
                if (sNode4 != null && sNode4.isCancelled()) {
                    sNode2.casNext(sNode4, sNode4.next);
                } else {
                    sNode2 = sNode4;
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

            QNode(Object obj, boolean z) {
                this.item = obj;
                this.isData = z;
            }

            boolean casNext(QNode qNode, QNode qNode2) {
                return this.next == qNode && U.compareAndSwapObject(this, NEXT, qNode, qNode2);
            }

            boolean casItem(Object obj, Object obj2) {
                return this.item == obj && U.compareAndSwapObject(this, ITEM, obj, obj2);
            }

            void tryCancel(Object obj) {
                U.compareAndSwapObject(this, ITEM, obj, this);
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
            QNode qNode = new QNode(null, false);
            this.head = qNode;
            this.tail = qNode;
        }

        void advanceHead(QNode qNode, QNode qNode2) {
            if (qNode == this.head && U.compareAndSwapObject(this, HEAD, qNode, qNode2)) {
                qNode.next = qNode;
            }
        }

        void advanceTail(QNode qNode, QNode qNode2) {
            if (this.tail == qNode) {
                U.compareAndSwapObject(this, TAIL, qNode, qNode2);
            }
        }

        boolean casCleanMe(QNode qNode, QNode qNode2) {
            return this.cleanMe == qNode && U.compareAndSwapObject(this, CLEANME, qNode, qNode2);
        }

        @Override
        E transfer(E e, boolean z, long j) {
            boolean z2 = e != null;
            QNode qNode = null;
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
                            QNode qNode5 = qNode;
                            if (!qNode2.casNext(null, qNode5)) {
                                qNode = qNode5;
                            } else {
                                advanceTail(qNode2, qNode5);
                                E e2 = (E) awaitFulfill(qNode5, e, z, j);
                                if (e2 == qNode5) {
                                    clean(qNode2, qNode5);
                                    return null;
                                }
                                if (!qNode5.isOffList()) {
                                    advanceHead(qNode2, qNode5);
                                    if (e2 != null) {
                                        qNode5.item = qNode5;
                                    }
                                    qNode5.waiter = null;
                                }
                                return e2 != null ? e2 : e;
                            }
                        }
                    } else {
                        QNode qNode6 = qNode3.next;
                        if (qNode2 == this.tail && qNode6 != null && qNode3 == this.head) {
                            E e3 = (E) qNode6.item;
                            if (z2 == (e3 != null) || e3 == qNode6 || !qNode6.casItem(e3, e)) {
                                advanceHead(qNode3, qNode6);
                            } else {
                                advanceHead(qNode3, qNode6);
                                LockSupport.unpark(qNode6.waiter);
                                return e3 != null ? e3 : e;
                            }
                        }
                    }
                }
            }
        }

        Object awaitFulfill(QNode qNode, E e, boolean z, long j) {
            int i;
            long jNanoTime = z ? System.nanoTime() + j : 0L;
            Thread threadCurrentThread = Thread.currentThread();
            if (this.head.next == qNode) {
                i = z ? SynchronousQueue.MAX_TIMED_SPINS : SynchronousQueue.MAX_UNTIMED_SPINS;
            } else {
                i = 0;
            }
            while (true) {
                if (threadCurrentThread.isInterrupted()) {
                    qNode.tryCancel(e);
                }
                Object obj = qNode.item;
                if (obj != e) {
                    return obj;
                }
                if (z) {
                    j = jNanoTime - System.nanoTime();
                    if (j <= 0) {
                        qNode.tryCancel(e);
                    }
                }
                if (i > 0) {
                    i--;
                } else if (qNode.waiter == null) {
                    qNode.waiter = threadCurrentThread;
                } else if (!z) {
                    LockSupport.park(this);
                } else if (j > SynchronousQueue.SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, j);
                }
            }
        }

        void clean(QNode qNode, QNode qNode2) {
            QNode qNode3;
            QNode qNode4;
            qNode2.waiter = null;
            while (qNode.next == qNode2) {
                QNode qNode5 = this.head;
                QNode qNode6 = qNode5.next;
                if (qNode6 != null && qNode6.isCancelled()) {
                    advanceHead(qNode5, qNode6);
                } else {
                    QNode qNode7 = this.tail;
                    if (qNode7 == qNode5) {
                        return;
                    }
                    QNode qNode8 = qNode7.next;
                    if (qNode7 != this.tail) {
                        continue;
                    } else if (qNode8 != null) {
                        advanceTail(qNode7, qNode8);
                    } else {
                        if (qNode2 != qNode7 && ((qNode4 = qNode2.next) == qNode2 || qNode.casNext(qNode2, qNode4))) {
                            return;
                        }
                        QNode qNode9 = this.cleanMe;
                        if (qNode9 == null) {
                            if (casCleanMe(null, qNode)) {
                                return;
                            }
                        } else {
                            QNode qNode10 = qNode9.next;
                            if (qNode10 == null || qNode10 == qNode9 || !qNode10.isCancelled() || (qNode10 != qNode7 && (qNode3 = qNode10.next) != null && qNode3 != qNode10 && qNode9.casNext(qNode10, qNode3))) {
                                casCleanMe(qNode9, null);
                            }
                            if (qNode9 == qNode) {
                                return;
                            }
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

    public SynchronousQueue(boolean z) {
        this.transferer = z ? new TransferQueue<>() : new TransferStack<>();
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        if (this.transferer.transfer(e, false, 0L) == null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    @Override
    public boolean offer(E e, long j, TimeUnit timeUnit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        if (this.transferer.transfer(e, true, timeUnit.toNanos(j)) != null) {
            return true;
        }
        if (!Thread.interrupted()) {
            return false;
        }
        throw new InterruptedException();
    }

    @Override
    public boolean offer(E e) {
        if (e != null) {
            return this.transferer.transfer(e, true, 0L) != null;
        }
        throw new NullPointerException();
    }

    @Override
    public E take() throws InterruptedException {
        E eTransfer = this.transferer.transfer(null, false, 0L);
        if (eTransfer != null) {
            return eTransfer;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    @Override
    public E poll(long j, TimeUnit timeUnit) throws InterruptedException {
        E eTransfer = this.transferer.transfer(null, true, timeUnit.toNanos(j));
        if (eTransfer != null || !Thread.interrupted()) {
            return eTransfer;
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
    public boolean contains(Object obj) {
        return false;
    }

    @Override
    public boolean remove(Object obj) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return collection.isEmpty();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
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
    public <T> T[] toArray(T[] tArr) {
        if (tArr.length > 0) {
            tArr[0] = null;
        }
        return tArr;
    }

    @Override
    public String toString() {
        return "[]";
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

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        if (this.transferer instanceof TransferQueue) {
            this.qlock = new ReentrantLock(true);
            this.waitingProducers = new FifoWaitQueue();
            this.waitingConsumers = new FifoWaitQueue();
        } else {
            this.qlock = new ReentrantLock();
            this.waitingProducers = new LifoWaitQueue();
            this.waitingConsumers = new LifoWaitQueue();
        }
        objectOutputStream.defaultWriteObject();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        if (this.waitingProducers instanceof FifoWaitQueue) {
            this.transferer = new TransferQueue();
        } else {
            this.transferer = new TransferStack();
        }
    }
}
