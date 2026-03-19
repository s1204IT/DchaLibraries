package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import sun.misc.Unsafe;

public abstract class AbstractQueuedLongSynchronizer extends AbstractOwnableSynchronizer implements Serializable {
    private static final long HEAD;
    static final long SPIN_FOR_TIMEOUT_THRESHOLD = 1000;
    private static final long STATE;
    private static final long TAIL;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = 7373984972572414692L;
    private volatile transient AbstractQueuedSynchronizer.Node head;
    private volatile long state;
    private volatile transient AbstractQueuedSynchronizer.Node tail;

    protected AbstractQueuedLongSynchronizer() {
    }

    protected final long getState() {
        return this.state;
    }

    protected final void setState(long j) {
        U.putLongVolatile(this, STATE, j);
    }

    protected final boolean compareAndSetState(long j, long j2) {
        return U.compareAndSwapLong(this, STATE, j, j2);
    }

    private AbstractQueuedSynchronizer.Node enq(AbstractQueuedSynchronizer.Node node) {
        while (true) {
            AbstractQueuedSynchronizer.Node node2 = this.tail;
            if (node2 != null) {
                U.putObject(node, AbstractQueuedSynchronizer.Node.PREV, node2);
                if (compareAndSetTail(node2, node)) {
                    node2.next = node;
                    return node2;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    private AbstractQueuedSynchronizer.Node addWaiter(AbstractQueuedSynchronizer.Node node) {
        AbstractQueuedSynchronizer.Node node2 = new AbstractQueuedSynchronizer.Node(node);
        while (true) {
            AbstractQueuedSynchronizer.Node node3 = this.tail;
            if (node3 != null) {
                U.putObject(node2, AbstractQueuedSynchronizer.Node.PREV, node3);
                if (compareAndSetTail(node3, node2)) {
                    node3.next = node2;
                    return node2;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    private void setHead(AbstractQueuedSynchronizer.Node node) {
        this.head = node;
        node.thread = null;
        node.prev = null;
    }

    private void unparkSuccessor(AbstractQueuedSynchronizer.Node node) {
        int i = node.waitStatus;
        if (i < 0) {
            node.compareAndSetWaitStatus(i, 0);
        }
        AbstractQueuedSynchronizer.Node node2 = node.next;
        if (node2 == null || node2.waitStatus > 0) {
            node2 = null;
            for (AbstractQueuedSynchronizer.Node node3 = this.tail; node3 != node && node3 != null; node3 = node3.prev) {
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
            AbstractQueuedSynchronizer.Node node = this.head;
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

    private void setHeadAndPropagate(AbstractQueuedSynchronizer.Node node, long j) {
        AbstractQueuedSynchronizer.Node node2;
        AbstractQueuedSynchronizer.Node node3 = this.head;
        setHead(node);
        if (j > 0 || node3 == null || node3.waitStatus < 0 || (node2 = this.head) == null || node2.waitStatus < 0) {
            AbstractQueuedSynchronizer.Node node4 = node.next;
            if (node4 == null || node4.isShared()) {
                doReleaseShared();
            }
        }
    }

    private void cancelAcquire(AbstractQueuedSynchronizer.Node node) {
        int i;
        if (node == null) {
            return;
        }
        node.thread = null;
        AbstractQueuedSynchronizer.Node node2 = node.prev;
        while (node2.waitStatus > 0) {
            node2 = node2.prev;
            node.prev = node2;
        }
        AbstractQueuedSynchronizer.Node node3 = node2.next;
        node.waitStatus = 1;
        if (node == this.tail && compareAndSetTail(node, node2)) {
            node2.compareAndSetNext(node3, null);
            return;
        }
        if (node2 != this.head && (((i = node2.waitStatus) == -1 || (i <= 0 && node2.compareAndSetWaitStatus(i, -1))) && node2.thread != null)) {
            AbstractQueuedSynchronizer.Node node4 = node.next;
            if (node4 != null && node4.waitStatus <= 0) {
                node2.compareAndSetNext(node3, node4);
            }
        } else {
            unparkSuccessor(node);
        }
        node.next = node;
    }

    private static boolean shouldParkAfterFailedAcquire(AbstractQueuedSynchronizer.Node node, AbstractQueuedSynchronizer.Node node2) {
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

    final boolean acquireQueued(AbstractQueuedSynchronizer.Node node, long j) {
        boolean z = false;
        while (true) {
            try {
                AbstractQueuedSynchronizer.Node nodePredecessor = node.predecessor();
                if (nodePredecessor == this.head && tryAcquire(j)) {
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

    private void doAcquireInterruptibly(long j) throws InterruptedException {
        AbstractQueuedSynchronizer.Node nodeAddWaiter = addWaiter(AbstractQueuedSynchronizer.Node.EXCLUSIVE);
        while (true) {
            try {
                AbstractQueuedSynchronizer.Node nodePredecessor = nodeAddWaiter.predecessor();
                if (nodePredecessor == this.head && tryAcquire(j)) {
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

    private boolean doAcquireNanos(long j, long j2) throws InterruptedException {
        if (j2 <= 0) {
            return false;
        }
        long jNanoTime = System.nanoTime() + j2;
        AbstractQueuedSynchronizer.Node nodeAddWaiter = addWaiter(AbstractQueuedSynchronizer.Node.EXCLUSIVE);
        do {
            try {
                AbstractQueuedSynchronizer.Node nodePredecessor = nodeAddWaiter.predecessor();
                if (nodePredecessor == this.head && tryAcquire(j)) {
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

    private void doAcquireShared(long j) {
        AbstractQueuedSynchronizer.Node nodePredecessor;
        long jTryAcquireShared;
        AbstractQueuedSynchronizer.Node nodeAddWaiter = addWaiter(AbstractQueuedSynchronizer.Node.SHARED);
        boolean z = false;
        while (true) {
            try {
                nodePredecessor = nodeAddWaiter.predecessor();
                if (nodePredecessor == this.head) {
                    jTryAcquireShared = tryAcquireShared(j);
                    if (jTryAcquireShared >= 0) {
                        break;
                    }
                }
                if (shouldParkAfterFailedAcquire(nodePredecessor, nodeAddWaiter) && parkAndCheckInterrupt()) {
                    z = true;
                }
            } catch (Throwable th) {
                cancelAcquire(nodeAddWaiter);
                throw th;
            }
        }
        setHeadAndPropagate(nodeAddWaiter, jTryAcquireShared);
        nodePredecessor.next = null;
        if (z) {
            selfInterrupt();
        }
    }

    private void doAcquireSharedInterruptibly(long j) throws InterruptedException {
        AbstractQueuedSynchronizer.Node nodeAddWaiter = addWaiter(AbstractQueuedSynchronizer.Node.SHARED);
        while (true) {
            try {
                AbstractQueuedSynchronizer.Node nodePredecessor = nodeAddWaiter.predecessor();
                if (nodePredecessor == this.head) {
                    long jTryAcquireShared = tryAcquireShared(j);
                    if (jTryAcquireShared >= 0) {
                        setHeadAndPropagate(nodeAddWaiter, jTryAcquireShared);
                        nodePredecessor.next = null;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(nodePredecessor, nodeAddWaiter) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            } catch (Throwable th) {
                cancelAcquire(nodeAddWaiter);
                throw th;
            }
        }
    }

    private boolean doAcquireSharedNanos(long j, long j2) throws InterruptedException {
        if (j2 <= 0) {
            return false;
        }
        long jNanoTime = System.nanoTime() + j2;
        AbstractQueuedSynchronizer.Node nodeAddWaiter = addWaiter(AbstractQueuedSynchronizer.Node.SHARED);
        do {
            try {
                AbstractQueuedSynchronizer.Node nodePredecessor = nodeAddWaiter.predecessor();
                if (nodePredecessor == this.head) {
                    long jTryAcquireShared = tryAcquireShared(j);
                    if (jTryAcquireShared >= 0) {
                        setHeadAndPropagate(nodeAddWaiter, jTryAcquireShared);
                        nodePredecessor.next = null;
                        return true;
                    }
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

    protected boolean tryAcquire(long j) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(long j) {
        throw new UnsupportedOperationException();
    }

    protected long tryAcquireShared(long j) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(long j) {
        throw new UnsupportedOperationException();
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    public final void acquire(long j) {
        if (!tryAcquire(j) && acquireQueued(addWaiter(AbstractQueuedSynchronizer.Node.EXCLUSIVE), j)) {
            selfInterrupt();
        }
    }

    public final void acquireInterruptibly(long j) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!tryAcquire(j)) {
            doAcquireInterruptibly(j);
        }
    }

    public final boolean tryAcquireNanos(long j, long j2) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquire(j) || doAcquireNanos(j, j2);
    }

    public final boolean release(long j) {
        if (tryRelease(j)) {
            AbstractQueuedSynchronizer.Node node = this.head;
            if (node != null && node.waitStatus != 0) {
                unparkSuccessor(node);
                return true;
            }
            return true;
        }
        return false;
    }

    public final void acquireShared(long j) {
        if (tryAcquireShared(j) < 0) {
            doAcquireShared(j);
        }
    }

    public final void acquireSharedInterruptibly(long j) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(j) < 0) {
            doAcquireSharedInterruptibly(j);
        }
    }

    public final boolean tryAcquireSharedNanos(long j, long j2) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquireShared(j) >= 0 || doAcquireSharedNanos(j, j2);
    }

    public final boolean releaseShared(long j) {
        if (tryReleaseShared(j)) {
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
        AbstractQueuedSynchronizer.Node node;
        AbstractQueuedSynchronizer.Node node2;
        Thread thread;
        AbstractQueuedSynchronizer.Node node3;
        AbstractQueuedSynchronizer.Node node4 = this.head;
        if ((node4 != null && (node3 = node4.next) != null && node3.prev == this.head && (thread = node3.thread) != null) || ((node = this.head) != null && (node2 = node.next) != null && node2.prev == this.head && (thread = node2.thread) != null)) {
            return thread;
        }
        Thread thread2 = null;
        for (AbstractQueuedSynchronizer.Node node5 = this.tail; node5 != null && node5 != this.head; node5 = node5.prev) {
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
        for (AbstractQueuedSynchronizer.Node node = this.tail; node != null; node = node.prev) {
            if (node.thread == thread) {
                return true;
            }
        }
        return false;
    }

    final boolean apparentlyFirstQueuedIsExclusive() {
        AbstractQueuedSynchronizer.Node node;
        AbstractQueuedSynchronizer.Node node2 = this.head;
        return (node2 == null || (node = node2.next) == null || node.isShared() || node.thread == null) ? false : true;
    }

    public final boolean hasQueuedPredecessors() {
        AbstractQueuedSynchronizer.Node node;
        AbstractQueuedSynchronizer.Node node2 = this.tail;
        AbstractQueuedSynchronizer.Node node3 = this.head;
        return node3 != node2 && ((node = node3.next) == null || node.thread != Thread.currentThread());
    }

    public final int getQueueLength() {
        int i = 0;
        for (AbstractQueuedSynchronizer.Node node = this.tail; node != null; node = node.prev) {
            if (node.thread != null) {
                i++;
            }
        }
        return i;
    }

    public final Collection<Thread> getQueuedThreads() {
        ArrayList arrayList = new ArrayList();
        for (AbstractQueuedSynchronizer.Node node = this.tail; node != null; node = node.prev) {
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
        for (AbstractQueuedSynchronizer.Node node = this.tail; node != null; node = node.prev) {
            if (!node.isShared() && (thread = node.thread) != null) {
                arrayList.add(thread);
            }
        }
        return arrayList;
    }

    public final Collection<Thread> getSharedQueuedThreads() {
        Thread thread;
        ArrayList arrayList = new ArrayList();
        for (AbstractQueuedSynchronizer.Node node = this.tail; node != null; node = node.prev) {
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

    final boolean isOnSyncQueue(AbstractQueuedSynchronizer.Node node) {
        if (node.waitStatus == -2 || node.prev == null) {
            return false;
        }
        if (node.next != null) {
            return true;
        }
        return findNodeFromTail(node);
    }

    private boolean findNodeFromTail(AbstractQueuedSynchronizer.Node node) {
        for (AbstractQueuedSynchronizer.Node node2 = this.tail; node2 != node; node2 = node2.prev) {
            if (node2 == null) {
                return false;
            }
        }
        return true;
    }

    final boolean transferForSignal(AbstractQueuedSynchronizer.Node node) {
        if (!node.compareAndSetWaitStatus(-2, 0)) {
            return false;
        }
        AbstractQueuedSynchronizer.Node nodeEnq = enq(node);
        int i = nodeEnq.waitStatus;
        if (i > 0 || !nodeEnq.compareAndSetWaitStatus(i, -1)) {
            LockSupport.unpark(node.thread);
            return true;
        }
        return true;
    }

    final boolean transferAfterCancelledWait(AbstractQueuedSynchronizer.Node node) {
        if (node.compareAndSetWaitStatus(-2, 0)) {
            enq(node);
            return true;
        }
        while (!isOnSyncQueue(node)) {
            Thread.yield();
        }
        return false;
    }

    final long fullyRelease(AbstractQueuedSynchronizer.Node node) {
        try {
            long state = getState();
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
        private transient AbstractQueuedSynchronizer.Node firstWaiter;
        private transient AbstractQueuedSynchronizer.Node lastWaiter;

        public ConditionObject() {
        }

        private AbstractQueuedSynchronizer.Node addConditionWaiter() {
            AbstractQueuedSynchronizer.Node node = this.lastWaiter;
            if (node != null && node.waitStatus != -2) {
                unlinkCancelledWaiters();
                node = this.lastWaiter;
            }
            AbstractQueuedSynchronizer.Node node2 = new AbstractQueuedSynchronizer.Node(-2);
            if (node == null) {
                this.firstWaiter = node2;
            } else {
                node.nextWaiter = node2;
            }
            this.lastWaiter = node2;
            return node2;
        }

        private void doSignal(AbstractQueuedSynchronizer.Node node) {
            do {
                AbstractQueuedSynchronizer.Node node2 = node.nextWaiter;
                this.firstWaiter = node2;
                if (node2 == null) {
                    this.lastWaiter = null;
                }
                node.nextWaiter = null;
                if (AbstractQueuedLongSynchronizer.this.transferForSignal(node)) {
                    return;
                } else {
                    node = this.firstWaiter;
                }
            } while (node != null);
        }

        private void doSignalAll(AbstractQueuedSynchronizer.Node node) {
            this.firstWaiter = null;
            this.lastWaiter = null;
            while (true) {
                AbstractQueuedSynchronizer.Node node2 = node.nextWaiter;
                node.nextWaiter = null;
                AbstractQueuedLongSynchronizer.this.transferForSignal(node);
                if (node2 != null) {
                    node = node2;
                } else {
                    return;
                }
            }
        }

        private void unlinkCancelledWaiters() {
            AbstractQueuedSynchronizer.Node node = this.firstWaiter;
            AbstractQueuedSynchronizer.Node node2 = null;
            while (node != null) {
                AbstractQueuedSynchronizer.Node node3 = node.nextWaiter;
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
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            AbstractQueuedSynchronizer.Node node = this.firstWaiter;
            if (node != null) {
                doSignal(node);
            }
        }

        @Override
        public final void signalAll() {
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            AbstractQueuedSynchronizer.Node node = this.firstWaiter;
            if (node != null) {
                doSignalAll(node);
            }
        }

        @Override
        public final void awaitUninterruptibly() {
            AbstractQueuedSynchronizer.Node nodeAddConditionWaiter = addConditionWaiter();
            long jFullyRelease = AbstractQueuedLongSynchronizer.this.fullyRelease(nodeAddConditionWaiter);
            boolean z = false;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(nodeAddConditionWaiter)) {
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    z = true;
                }
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(nodeAddConditionWaiter, jFullyRelease) || z) {
                AbstractQueuedLongSynchronizer.selfInterrupt();
            }
        }

        private int checkInterruptWhileWaiting(AbstractQueuedSynchronizer.Node node) {
            if (Thread.interrupted()) {
                return AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node) ? -1 : 1;
            }
            return 0;
        }

        private void reportInterruptAfterWait(int i) throws InterruptedException {
            if (i == -1) {
                throw new InterruptedException();
            }
            if (i == 1) {
                AbstractQueuedLongSynchronizer.selfInterrupt();
            }
        }

        @Override
        public final void await() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            AbstractQueuedSynchronizer.Node nodeAddConditionWaiter = addConditionWaiter();
            long jFullyRelease = AbstractQueuedLongSynchronizer.this.fullyRelease(nodeAddConditionWaiter);
            int iCheckInterruptWhileWaiting = 0;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(nodeAddConditionWaiter)) {
                LockSupport.park(this);
                iCheckInterruptWhileWaiting = checkInterruptWhileWaiting(nodeAddConditionWaiter);
                if (iCheckInterruptWhileWaiting != 0) {
                    break;
                }
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(nodeAddConditionWaiter, jFullyRelease) && iCheckInterruptWhileWaiting != -1) {
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
            AbstractQueuedSynchronizer.Node nodeAddConditionWaiter = addConditionWaiter();
            long jFullyRelease = AbstractQueuedLongSynchronizer.this.fullyRelease(nodeAddConditionWaiter);
            int iCheckInterruptWhileWaiting = 0;
            long jNanoTime2 = j;
            while (true) {
                if (AbstractQueuedLongSynchronizer.this.isOnSyncQueue(nodeAddConditionWaiter)) {
                    break;
                }
                if (jNanoTime2 <= 0) {
                    AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(nodeAddConditionWaiter);
                    break;
                }
                if (jNanoTime2 > AbstractQueuedLongSynchronizer.SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, jNanoTime2);
                }
                iCheckInterruptWhileWaiting = checkInterruptWhileWaiting(nodeAddConditionWaiter);
                if (iCheckInterruptWhileWaiting != 0) {
                    break;
                }
                jNanoTime2 = jNanoTime - System.nanoTime();
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(nodeAddConditionWaiter, jFullyRelease) && iCheckInterruptWhileWaiting != -1) {
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
            AbstractQueuedSynchronizer.Node nodeAddConditionWaiter = addConditionWaiter();
            long jFullyRelease = AbstractQueuedLongSynchronizer.this.fullyRelease(nodeAddConditionWaiter);
            int iCheckInterruptWhileWaiting = 0;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(nodeAddConditionWaiter)) {
                if (System.currentTimeMillis() >= time) {
                    zTransferAfterCancelledWait = AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(nodeAddConditionWaiter);
                    break;
                }
                LockSupport.parkUntil(this, time);
                iCheckInterruptWhileWaiting = checkInterruptWhileWaiting(nodeAddConditionWaiter);
                if (iCheckInterruptWhileWaiting != 0) {
                    break;
                }
            }
            zTransferAfterCancelledWait = false;
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(nodeAddConditionWaiter, jFullyRelease) && iCheckInterruptWhileWaiting != -1) {
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
            AbstractQueuedSynchronizer.Node nodeAddConditionWaiter = addConditionWaiter();
            long jFullyRelease = AbstractQueuedLongSynchronizer.this.fullyRelease(nodeAddConditionWaiter);
            int iCheckInterruptWhileWaiting = 0;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(nodeAddConditionWaiter)) {
                if (nanos <= 0) {
                    zTransferAfterCancelledWait = AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(nodeAddConditionWaiter);
                    break;
                }
                if (nanos > AbstractQueuedLongSynchronizer.SPIN_FOR_TIMEOUT_THRESHOLD) {
                    LockSupport.parkNanos(this, nanos);
                }
                iCheckInterruptWhileWaiting = checkInterruptWhileWaiting(nodeAddConditionWaiter);
                if (iCheckInterruptWhileWaiting != 0) {
                    break;
                }
                nanos = jNanoTime - System.nanoTime();
            }
            zTransferAfterCancelledWait = false;
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(nodeAddConditionWaiter, jFullyRelease) && iCheckInterruptWhileWaiting != -1) {
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

        final boolean isOwnedBy(AbstractQueuedLongSynchronizer abstractQueuedLongSynchronizer) {
            return abstractQueuedLongSynchronizer == AbstractQueuedLongSynchronizer.this;
        }

        protected final boolean hasWaiters() {
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            for (AbstractQueuedSynchronizer.Node node = this.firstWaiter; node != null; node = node.nextWaiter) {
                if (node.waitStatus == -2) {
                    return true;
                }
            }
            return false;
        }

        protected final int getWaitQueueLength() {
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int i = 0;
            for (AbstractQueuedSynchronizer.Node node = this.firstWaiter; node != null; node = node.nextWaiter) {
                if (node.waitStatus == -2) {
                    i++;
                }
            }
            return i;
        }

        protected final Collection<Thread> getWaitingThreads() {
            Thread thread;
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            ArrayList arrayList = new ArrayList();
            for (AbstractQueuedSynchronizer.Node node = this.firstWaiter; node != null; node = node.nextWaiter) {
                if (node.waitStatus == -2 && (thread = node.thread) != null) {
                    arrayList.add(thread);
                }
            }
            return arrayList;
        }
    }

    static {
        try {
            STATE = U.objectFieldOffset(AbstractQueuedLongSynchronizer.class.getDeclaredField("state"));
            HEAD = U.objectFieldOffset(AbstractQueuedLongSynchronizer.class.getDeclaredField("head"));
            TAIL = U.objectFieldOffset(AbstractQueuedLongSynchronizer.class.getDeclaredField("tail"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private final void initializeSyncQueue() {
        Unsafe unsafe = U;
        long j = HEAD;
        AbstractQueuedSynchronizer.Node node = new AbstractQueuedSynchronizer.Node();
        if (unsafe.compareAndSwapObject(this, j, null, node)) {
            this.tail = node;
        }
    }

    private final boolean compareAndSetTail(AbstractQueuedSynchronizer.Node node, AbstractQueuedSynchronizer.Node node2) {
        return U.compareAndSwapObject(this, TAIL, node, node2);
    }
}
