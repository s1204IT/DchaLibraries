package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements Serializable {
    private static final long HEAD;
    static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000;
    private static final long STATE;
    private static final long TAIL;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = 7373984972572414691L;
    private volatile transient Node head;
    private volatile int state;
    private volatile transient Node tail;

    protected AbstractQueuedSynchronizer() {
    }

    static final class Node {
        static final int CANCELLED = 1;
        static final int CONDITION = -2;
        private static final long NEXT;
        static final long PREV;
        static final int PROPAGATE = -3;
        static final int SIGNAL = -1;
        private static final long THREAD;
        private static final long WAITSTATUS;
        volatile Node next;
        Node nextWaiter;
        volatile Node prev;
        volatile Thread thread;
        volatile int waitStatus;
        static final Node SHARED = new Node();
        static final Node EXCLUSIVE = null;
        private static final Unsafe U = Unsafe.getUnsafe();

        static {
            try {
                NEXT = U.objectFieldOffset(Node.class.getDeclaredField("next"));
                PREV = U.objectFieldOffset(Node.class.getDeclaredField("prev"));
                THREAD = U.objectFieldOffset(Node.class.getDeclaredField("thread"));
                WAITSTATUS = U.objectFieldOffset(Node.class.getDeclaredField("waitStatus"));
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }

        final boolean isShared() {
            return this.nextWaiter == SHARED;
        }

        final Node predecessor() throws NullPointerException {
            Node node = this.prev;
            if (node == null) {
                throw new NullPointerException();
            }
            return node;
        }

        Node() {
        }

        Node(Node node) {
            this.nextWaiter = node;
            U.putObject(this, THREAD, Thread.currentThread());
        }

        Node(int i) {
            U.putInt(this, WAITSTATUS, i);
            U.putObject(this, THREAD, Thread.currentThread());
        }

        final boolean compareAndSetWaitStatus(int i, int i2) {
            return U.compareAndSwapInt(this, WAITSTATUS, i, i2);
        }

        final boolean compareAndSetNext(Node node, Node node2) {
            return U.compareAndSwapObject(this, NEXT, node, node2);
        }
    }

    protected final int getState() {
        return this.state;
    }

    protected final void setState(int i) {
        this.state = i;
    }

    protected final boolean compareAndSetState(int i, int i2) {
        return U.compareAndSwapInt(this, STATE, i, i2);
    }

    private Node enq(Node node) {
        while (true) {
            Node node2 = this.tail;
            if (node2 != null) {
                U.putObject(node, Node.PREV, node2);
                if (compareAndSetTail(node2, node)) {
                    node2.next = node;
                    return node2;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    private Node addWaiter(Node node) {
        Node node2 = new Node(node);
        while (true) {
            Node node3 = this.tail;
            if (node3 != null) {
                U.putObject(node2, Node.PREV, node3);
                if (compareAndSetTail(node3, node2)) {
                    node3.next = node2;
                    return node2;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    private void setHead(Node node) {
        this.head = node;
        node.thread = null;
        node.prev = null;
    }

    private void unparkSuccessor(Node node) {
        int i = node.waitStatus;
        if (i < 0) {
            node.compareAndSetWaitStatus(i, 0);
        }
        Node node2 = node.next;
        if (node2 == null || node2.waitStatus > 0) {
            node2 = null;
            for (Node node3 = this.tail; node3 != node && node3 != null; node3 = node3.prev) {
                if (node3.waitStatus <= 0) {
                    node2 = node3;
                }
            }
        }
        if (node2 != null) {
            LockSupport.unpark(node2.thread);
        }
    }

    private void doReleaseShared() {
        while (true) {
            Node node = this.head;
            if (node != null && node != this.tail) {
                int i = node.waitStatus;
                if (i == -1) {
                    if (node.compareAndSetWaitStatus(-1, 0)) {
                        unparkSuccessor(node);
                    } else {
                        continue;
                    }
                } else if (i != 0 || node.compareAndSetWaitStatus(0, -3)) {
                }
            }
            if (node == this.head) {
                return;
            }
        }
    }

    private void setHeadAndPropagate(Node node, int i) {
        Node node2;
        Node node3 = this.head;
        setHead(node);
        if (i > 0 || node3 == null || node3.waitStatus < 0 || (node2 = this.head) == null || node2.waitStatus < 0) {
            Node node4 = node.next;
            if (node4 == null || node4.isShared()) {
                doReleaseShared();
            }
        }
    }

    private void cancelAcquire(Node node) {
        int i;
        if (node == null) {
            return;
        }
        node.thread = null;
        Node node2 = node.prev;
        while (node2.waitStatus > 0) {
            node2 = node2.prev;
            node.prev = node2;
        }
        Node node3 = node2.next;
        node.waitStatus = 1;
        if (node == this.tail && compareAndSetTail(node, node2)) {
            node2.compareAndSetNext(node3, null);
            return;
        }
        if (node2 != this.head && (((i = node2.waitStatus) == -1 || (i <= 0 && node2.compareAndSetWaitStatus(i, -1))) && node2.thread != null)) {
            Node node4 = node.next;
            if (node4 != null && node4.waitStatus <= 0) {
                node2.compareAndSetNext(node3, node4);
            }
        } else {
            unparkSuccessor(node);
        }
        node.next = node;
    }

    private static boolean shouldParkAfterFailedAcquire(Node node, Node node2) {
        int i = node.waitStatus;
        if (i == -1) {
            return true;
        }
        if (i > 0) {
            do {
                node = node.prev;
                node2.prev = node;
            } while (node.waitStatus > 0);
            node.next = node2;
            return false;
        }
        node.compareAndSetWaitStatus(i, -1);
        return false;
    }

    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    final boolean acquireQueued(Node node, int i) {
        boolean z = false;
        while (true) {
            try {
                Node nodePredecessor = node.predecessor();
                if (nodePredecessor == this.head && tryAcquire(i)) {
                    setHead(node);
                    nodePredecessor.next = null;
                    return z;
                }
                if (shouldParkAfterFailedAcquire(nodePredecessor, node) && parkAndCheckInterrupt()) {
                    z = true;
                }
            } catch (Throwable th) {
                cancelAcquire(node);
                throw th;
            }
        }
    }

    private void doAcquireInterruptibly(int i) throws InterruptedException {
        Node nodeAddWaiter = addWaiter(Node.EXCLUSIVE);
        while (true) {
            try {
                Node nodePredecessor = nodeAddWaiter.predecessor();
                if (nodePredecessor == this.head && tryAcquire(i)) {
                    setHead(nodeAddWaiter);
                    nodePredecessor.next = null;
                    return;
                } else if (shouldParkAfterFailedAcquire(nodePredecessor, nodeAddWaiter) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            } catch (Throwable th) {
                cancelAcquire(nodeAddWaiter);
                throw th;
            }
        }
    }

    private boolean doAcquireNanos(int i, long j) throws InterruptedException {
        if (j <= 0) {
            return false;
        }
        long jNanoTime = System.nanoTime() + j;
        Node nodeAddWaiter = addWaiter(Node.EXCLUSIVE);
        do {
            try {
                Node nodePredecessor = nodeAddWaiter.predecessor();
                if (nodePredecessor == this.head && tryAcquire(i)) {
                    setHead(nodeAddWaiter);
                    nodePredecessor.next = null;
                    return true;
                }
                long jNanoTime2 = jNanoTime - System.nanoTime();
                if (jNanoTime2 <= 0) {
                    return false;
                }
                if (shouldParkAfterFailedAcquire(nodePredecessor, nodeAddWaiter) && jNanoTime2 > SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, jNanoTime2);
                }
            } finally {
                cancelAcquire(nodeAddWaiter);
            }
        } while (!Thread.interrupted());
        throw new InterruptedException();
    }

    private void doAcquireShared(int i) {
        Node nodePredecessor;
        int iTryAcquireShared;
        Node nodeAddWaiter = addWaiter(Node.SHARED);
        boolean z = false;
        while (true) {
            try {
                nodePredecessor = nodeAddWaiter.predecessor();
                if (nodePredecessor == this.head && (iTryAcquireShared = tryAcquireShared(i)) >= 0) {
                    break;
                } else if (shouldParkAfterFailedAcquire(nodePredecessor, nodeAddWaiter) && parkAndCheckInterrupt()) {
                    z = true;
                }
            } catch (Throwable th) {
                cancelAcquire(nodeAddWaiter);
                throw th;
            }
        }
        setHeadAndPropagate(nodeAddWaiter, iTryAcquireShared);
        nodePredecessor.next = null;
        if (z) {
            selfInterrupt();
        }
    }

    private void doAcquireSharedInterruptibly(int i) throws InterruptedException {
        int iTryAcquireShared;
        Node nodeAddWaiter = addWaiter(Node.SHARED);
        while (true) {
            try {
                Node nodePredecessor = nodeAddWaiter.predecessor();
                if (nodePredecessor == this.head && (iTryAcquireShared = tryAcquireShared(i)) >= 0) {
                    setHeadAndPropagate(nodeAddWaiter, iTryAcquireShared);
                    nodePredecessor.next = null;
                    return;
                } else if (shouldParkAfterFailedAcquire(nodePredecessor, nodeAddWaiter) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            } catch (Throwable th) {
                cancelAcquire(nodeAddWaiter);
                throw th;
            }
        }
    }

    private boolean doAcquireSharedNanos(int i, long j) throws InterruptedException {
        int iTryAcquireShared;
        if (j <= 0) {
            return false;
        }
        long jNanoTime = System.nanoTime() + j;
        Node nodeAddWaiter = addWaiter(Node.SHARED);
        do {
            try {
                Node nodePredecessor = nodeAddWaiter.predecessor();
                if (nodePredecessor == this.head && (iTryAcquireShared = tryAcquireShared(i)) >= 0) {
                    setHeadAndPropagate(nodeAddWaiter, iTryAcquireShared);
                    nodePredecessor.next = null;
                    return true;
                }
                long jNanoTime2 = jNanoTime - System.nanoTime();
                if (jNanoTime2 <= 0) {
                    return false;
                }
                if (shouldParkAfterFailedAcquire(nodePredecessor, nodeAddWaiter) && jNanoTime2 > SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, jNanoTime2);
                }
            } finally {
                cancelAcquire(nodeAddWaiter);
            }
        } while (!Thread.interrupted());
        throw new InterruptedException();
    }

    protected boolean tryAcquire(int i) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(int i) {
        throw new UnsupportedOperationException();
    }

    protected int tryAcquireShared(int i) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(int i) {
        throw new UnsupportedOperationException();
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    public final void acquire(int i) {
        if (!tryAcquire(i) && acquireQueued(addWaiter(Node.EXCLUSIVE), i)) {
            selfInterrupt();
        }
    }

    public final void acquireInterruptibly(int i) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!tryAcquire(i)) {
            doAcquireInterruptibly(i);
        }
    }

    public final boolean tryAcquireNanos(int i, long j) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquire(i) || doAcquireNanos(i, j);
    }

    public final boolean release(int i) {
        if (tryRelease(i)) {
            Node node = this.head;
            if (node != null && node.waitStatus != 0) {
                unparkSuccessor(node);
                return true;
            }
            return true;
        }
        return false;
    }

    public final void acquireShared(int i) {
        if (tryAcquireShared(i) < 0) {
            doAcquireShared(i);
        }
    }

    public final void acquireSharedInterruptibly(int i) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(i) < 0) {
            doAcquireSharedInterruptibly(i);
        }
    }

    public final boolean tryAcquireSharedNanos(int i, long j) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquireShared(i) >= 0 || doAcquireSharedNanos(i, j);
    }

    public final boolean releaseShared(int i) {
        if (tryReleaseShared(i)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    public final boolean hasQueuedThreads() {
        return this.head != this.tail;
    }

    public final boolean hasContended() {
        return this.head != null;
    }

    public final Thread getFirstQueuedThread() {
        if (this.head == this.tail) {
            return null;
        }
        return fullGetFirstQueuedThread();
    }

    private Thread fullGetFirstQueuedThread() {
        Node node;
        Node node2;
        Thread thread;
        Node node3;
        Node node4 = this.head;
        if ((node4 != null && (node3 = node4.next) != null && node3.prev == this.head && (thread = node3.thread) != null) || ((node = this.head) != null && (node2 = node.next) != null && node2.prev == this.head && (thread = node2.thread) != null)) {
            return thread;
        }
        Thread thread2 = null;
        for (Node node5 = this.tail; node5 != null && node5 != this.head; node5 = node5.prev) {
            Thread thread3 = node5.thread;
            if (thread3 != null) {
                thread2 = thread3;
            }
        }
        return thread2;
    }

    public final boolean isQueued(Thread thread) {
        if (thread == null) {
            throw new NullPointerException();
        }
        for (Node node = this.tail; node != null; node = node.prev) {
            if (node.thread == thread) {
                return true;
            }
        }
        return false;
    }

    final boolean apparentlyFirstQueuedIsExclusive() {
        Node node;
        Node node2 = this.head;
        return (node2 == null || (node = node2.next) == null || node.isShared() || node.thread == null) ? false : true;
    }

    public final boolean hasQueuedPredecessors() {
        Node node;
        Node node2 = this.tail;
        Node node3 = this.head;
        return node3 != node2 && ((node = node3.next) == null || node.thread != Thread.currentThread());
    }

    public final int getQueueLength() {
        int i = 0;
        for (Node node = this.tail; node != null; node = node.prev) {
            if (node.thread != null) {
                i++;
            }
        }
        return i;
    }

    public final Collection<Thread> getQueuedThreads() {
        ArrayList arrayList = new ArrayList();
        for (Node node = this.tail; node != null; node = node.prev) {
            Thread thread = node.thread;
            if (thread != null) {
                arrayList.add(thread);
            }
        }
        return arrayList;
    }

    public final Collection<Thread> getExclusiveQueuedThreads() {
        Thread thread;
        ArrayList arrayList = new ArrayList();
        for (Node node = this.tail; node != null; node = node.prev) {
            if (!node.isShared() && (thread = node.thread) != null) {
                arrayList.add(thread);
            }
        }
        return arrayList;
    }

    public final Collection<Thread> getSharedQueuedThreads() {
        Thread thread;
        ArrayList arrayList = new ArrayList();
        for (Node node = this.tail; node != null; node = node.prev) {
            if (node.isShared() && (thread = node.thread) != null) {
                arrayList.add(thread);
            }
        }
        return arrayList;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("[State = ");
        sb.append(getState());
        sb.append(", ");
        sb.append(hasQueuedThreads() ? "non" : "");
        sb.append("empty queue]");
        return sb.toString();
    }

    final boolean isOnSyncQueue(Node node) {
        if (node.waitStatus == -2 || node.prev == null) {
            return false;
        }
        if (node.next != null) {
            return true;
        }
        return findNodeFromTail(node);
    }

    private boolean findNodeFromTail(Node node) {
        for (Node node2 = this.tail; node2 != node; node2 = node2.prev) {
            if (node2 == null) {
                return false;
            }
        }
        return true;
    }

    final boolean transferForSignal(Node node) {
        if (!node.compareAndSetWaitStatus(-2, 0)) {
            return false;
        }
        Node nodeEnq = enq(node);
        int i = nodeEnq.waitStatus;
        if (i > 0 || !nodeEnq.compareAndSetWaitStatus(i, -1)) {
            LockSupport.unpark(node.thread);
            return true;
        }
        return true;
    }

    final boolean transferAfterCancelledWait(Node node) {
        if (node.compareAndSetWaitStatus(-2, 0)) {
            enq(node);
            return true;
        }
        while (!isOnSyncQueue(node)) {
            Thread.yield();
        }
        return false;
    }

    final int fullyRelease(Node node) {
        try {
            int state = getState();
            if (release(state)) {
                return state;
            }
            throw new IllegalMonitorStateException();
        } catch (Throwable th) {
            node.waitStatus = 1;
            throw th;
        }
    }

    public final boolean owns(ConditionObject conditionObject) {
        return conditionObject.isOwnedBy(this);
    }

    public final boolean hasWaiters(ConditionObject conditionObject) {
        if (!owns(conditionObject)) {
            throw new IllegalArgumentException("Not owner");
        }
        return conditionObject.hasWaiters();
    }

    public final int getWaitQueueLength(ConditionObject conditionObject) {
        if (!owns(conditionObject)) {
            throw new IllegalArgumentException("Not owner");
        }
        return conditionObject.getWaitQueueLength();
    }

    public final Collection<Thread> getWaitingThreads(ConditionObject conditionObject) {
        if (!owns(conditionObject)) {
            throw new IllegalArgumentException("Not owner");
        }
        return conditionObject.getWaitingThreads();
    }

    public class ConditionObject implements Condition, Serializable {
        private static final int REINTERRUPT = 1;
        private static final int THROW_IE = -1;
        private static final long serialVersionUID = 1173984872572414699L;
        private transient Node firstWaiter;
        private transient Node lastWaiter;

        public ConditionObject() {
        }

        private Node addConditionWaiter() {
            Node node = this.lastWaiter;
            if (node != null && node.waitStatus != -2) {
                unlinkCancelledWaiters();
                node = this.lastWaiter;
            }
            Node node2 = new Node(-2);
            if (node == null) {
                this.firstWaiter = node2;
            } else {
                node.nextWaiter = node2;
            }
            this.lastWaiter = node2;
            return node2;
        }

        private void doSignal(Node node) {
            do {
                Node node2 = node.nextWaiter;
                this.firstWaiter = node2;
                if (node2 == null) {
                    this.lastWaiter = null;
                }
                node.nextWaiter = null;
                if (AbstractQueuedSynchronizer.this.transferForSignal(node)) {
                    return;
                } else {
                    node = this.firstWaiter;
                }
            } while (node != null);
        }

        private void doSignalAll(Node node) {
            this.firstWaiter = null;
            this.lastWaiter = null;
            while (true) {
                Node node2 = node.nextWaiter;
                node.nextWaiter = null;
                AbstractQueuedSynchronizer.this.transferForSignal(node);
                if (node2 != null) {
                    node = node2;
                } else {
                    return;
                }
            }
        }

        private void unlinkCancelledWaiters() {
            Node node = this.firstWaiter;
            Node node2 = null;
            while (node != null) {
                Node node3 = node.nextWaiter;
                if (node.waitStatus != -2) {
                    node.nextWaiter = null;
                    if (node2 == null) {
                        this.firstWaiter = node3;
                    } else {
                        node2.nextWaiter = node3;
                    }
                    if (node3 == null) {
                        this.lastWaiter = node2;
                    }
                } else {
                    node2 = node;
                }
                node = node3;
            }
        }

        @Override
        public final void signal() {
            if (!AbstractQueuedSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            Node node = this.firstWaiter;
            if (node != null) {
                doSignal(node);
            }
        }

        @Override
        public final void signalAll() {
            if (!AbstractQueuedSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            Node node = this.firstWaiter;
            if (node != null) {
                doSignalAll(node);
            }
        }

        @Override
        public final void awaitUninterruptibly() {
            Node nodeAddConditionWaiter = addConditionWaiter();
            int iFullyRelease = AbstractQueuedSynchronizer.this.fullyRelease(nodeAddConditionWaiter);
            boolean z = false;
            while (!AbstractQueuedSynchronizer.this.isOnSyncQueue(nodeAddConditionWaiter)) {
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    z = true;
                }
            }
            if (AbstractQueuedSynchronizer.this.acquireQueued(nodeAddConditionWaiter, iFullyRelease) || z) {
                AbstractQueuedSynchronizer.selfInterrupt();
            }
        }

        private int checkInterruptWhileWaiting(Node node) {
            if (Thread.interrupted()) {
                return AbstractQueuedSynchronizer.this.transferAfterCancelledWait(node) ? -1 : 1;
            }
            return 0;
        }

        private void reportInterruptAfterWait(int i) throws InterruptedException {
            if (i == -1) {
                throw new InterruptedException();
            }
            if (i == 1) {
                AbstractQueuedSynchronizer.selfInterrupt();
            }
        }

        @Override
        public final void await() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node nodeAddConditionWaiter = addConditionWaiter();
            int iFullyRelease = AbstractQueuedSynchronizer.this.fullyRelease(nodeAddConditionWaiter);
            int iCheckInterruptWhileWaiting = 0;
            while (!AbstractQueuedSynchronizer.this.isOnSyncQueue(nodeAddConditionWaiter)) {
                LockSupport.park(this);
                iCheckInterruptWhileWaiting = checkInterruptWhileWaiting(nodeAddConditionWaiter);
                if (iCheckInterruptWhileWaiting != 0) {
                    break;
                }
            }
            if (AbstractQueuedSynchronizer.this.acquireQueued(nodeAddConditionWaiter, iFullyRelease) && iCheckInterruptWhileWaiting != -1) {
                iCheckInterruptWhileWaiting = 1;
            }
            if (nodeAddConditionWaiter.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (iCheckInterruptWhileWaiting != 0) {
                reportInterruptAfterWait(iCheckInterruptWhileWaiting);
            }
        }

        @Override
        public final long awaitNanos(long j) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long jNanoTime = System.nanoTime() + j;
            Node nodeAddConditionWaiter = addConditionWaiter();
            int iFullyRelease = AbstractQueuedSynchronizer.this.fullyRelease(nodeAddConditionWaiter);
            int iCheckInterruptWhileWaiting = 0;
            long jNanoTime2 = j;
            while (true) {
                if (AbstractQueuedSynchronizer.this.isOnSyncQueue(nodeAddConditionWaiter)) {
                    break;
                }
                if (jNanoTime2 <= 0) {
                    AbstractQueuedSynchronizer.this.transferAfterCancelledWait(nodeAddConditionWaiter);
                    break;
                }
                if (jNanoTime2 > AbstractQueuedSynchronizer.SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, jNanoTime2);
                }
                iCheckInterruptWhileWaiting = checkInterruptWhileWaiting(nodeAddConditionWaiter);
                if (iCheckInterruptWhileWaiting != 0) {
                    break;
                }
                jNanoTime2 = jNanoTime - System.nanoTime();
            }
            if (AbstractQueuedSynchronizer.this.acquireQueued(nodeAddConditionWaiter, iFullyRelease) && iCheckInterruptWhileWaiting != -1) {
                iCheckInterruptWhileWaiting = 1;
            }
            if (nodeAddConditionWaiter.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (iCheckInterruptWhileWaiting != 0) {
                reportInterruptAfterWait(iCheckInterruptWhileWaiting);
            }
            long jNanoTime3 = jNanoTime - System.nanoTime();
            if (jNanoTime3 <= j) {
                return jNanoTime3;
            }
            return Long.MIN_VALUE;
        }

        @Override
        public final boolean awaitUntil(Date date) throws InterruptedException {
            boolean zTransferAfterCancelledWait;
            long time = date.getTime();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node nodeAddConditionWaiter = addConditionWaiter();
            int iFullyRelease = AbstractQueuedSynchronizer.this.fullyRelease(nodeAddConditionWaiter);
            int iCheckInterruptWhileWaiting = 0;
            while (!AbstractQueuedSynchronizer.this.isOnSyncQueue(nodeAddConditionWaiter)) {
                if (System.currentTimeMillis() >= time) {
                    zTransferAfterCancelledWait = AbstractQueuedSynchronizer.this.transferAfterCancelledWait(nodeAddConditionWaiter);
                    break;
                }
                LockSupport.parkUntil(this, time);
                iCheckInterruptWhileWaiting = checkInterruptWhileWaiting(nodeAddConditionWaiter);
                if (iCheckInterruptWhileWaiting != 0) {
                    break;
                }
            }
            zTransferAfterCancelledWait = false;
            if (AbstractQueuedSynchronizer.this.acquireQueued(nodeAddConditionWaiter, iFullyRelease) && iCheckInterruptWhileWaiting != -1) {
                iCheckInterruptWhileWaiting = 1;
            }
            if (nodeAddConditionWaiter.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (iCheckInterruptWhileWaiting != 0) {
                reportInterruptAfterWait(iCheckInterruptWhileWaiting);
            }
            return !zTransferAfterCancelledWait;
        }

        @Override
        public final boolean await(long j, TimeUnit timeUnit) throws InterruptedException {
            boolean zTransferAfterCancelledWait;
            long nanos = timeUnit.toNanos(j);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long jNanoTime = System.nanoTime() + nanos;
            Node nodeAddConditionWaiter = addConditionWaiter();
            int iFullyRelease = AbstractQueuedSynchronizer.this.fullyRelease(nodeAddConditionWaiter);
            int iCheckInterruptWhileWaiting = 0;
            while (!AbstractQueuedSynchronizer.this.isOnSyncQueue(nodeAddConditionWaiter)) {
                if (nanos <= 0) {
                    zTransferAfterCancelledWait = AbstractQueuedSynchronizer.this.transferAfterCancelledWait(nodeAddConditionWaiter);
                    break;
                }
                if (nanos > AbstractQueuedSynchronizer.SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanos);
                }
                iCheckInterruptWhileWaiting = checkInterruptWhileWaiting(nodeAddConditionWaiter);
                if (iCheckInterruptWhileWaiting != 0) {
                    break;
                }
                nanos = jNanoTime - System.nanoTime();
            }
            zTransferAfterCancelledWait = false;
            if (AbstractQueuedSynchronizer.this.acquireQueued(nodeAddConditionWaiter, iFullyRelease) && iCheckInterruptWhileWaiting != -1) {
                iCheckInterruptWhileWaiting = 1;
            }
            if (nodeAddConditionWaiter.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (iCheckInterruptWhileWaiting != 0) {
                reportInterruptAfterWait(iCheckInterruptWhileWaiting);
            }
            return !zTransferAfterCancelledWait;
        }

        final boolean isOwnedBy(AbstractQueuedSynchronizer abstractQueuedSynchronizer) {
            return abstractQueuedSynchronizer == AbstractQueuedSynchronizer.this;
        }

        protected final boolean hasWaiters() {
            if (!AbstractQueuedSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            for (Node node = this.firstWaiter; node != null; node = node.nextWaiter) {
                if (node.waitStatus == -2) {
                    return true;
                }
            }
            return false;
        }

        protected final int getWaitQueueLength() {
            if (!AbstractQueuedSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int i = 0;
            for (Node node = this.firstWaiter; node != null; node = node.nextWaiter) {
                if (node.waitStatus == -2) {
                    i++;
                }
            }
            return i;
        }

        protected final Collection<Thread> getWaitingThreads() {
            Thread thread;
            if (!AbstractQueuedSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            ArrayList arrayList = new ArrayList();
            for (Node node = this.firstWaiter; node != null; node = node.nextWaiter) {
                if (node.waitStatus == -2 && (thread = node.thread) != null) {
                    arrayList.add(thread);
                }
            }
            return arrayList;
        }
    }

    static {
        try {
            STATE = U.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            HEAD = U.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            TAIL = U.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private final void initializeSyncQueue() {
        Unsafe unsafe = U;
        long j = HEAD;
        Node node = new Node();
        if (unsafe.compareAndSwapObject(this, j, null, node)) {
            this.tail = node;
        }
    }

    private final boolean compareAndSetTail(Node node, Node node2) {
        return U.compareAndSwapObject(this, TAIL, node, node2);
    }
}
