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

    protected final void setState(long newState) {
        U.putLongVolatile(this, STATE, newState);
    }

    protected final boolean compareAndSetState(long expect, long update) {
        return U.compareAndSwapLong(this, STATE, expect, update);
    }

    private AbstractQueuedSynchronizer.Node enq(AbstractQueuedSynchronizer.Node node) {
        while (true) {
            AbstractQueuedSynchronizer.Node oldTail = this.tail;
            if (oldTail != null) {
                U.putObject(node, AbstractQueuedSynchronizer.Node.PREV, oldTail);
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node;
                    return oldTail;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    private AbstractQueuedSynchronizer.Node addWaiter(AbstractQueuedSynchronizer.Node mode) {
        AbstractQueuedSynchronizer.Node node = new AbstractQueuedSynchronizer.Node(mode);
        while (true) {
            AbstractQueuedSynchronizer.Node oldTail = this.tail;
            if (oldTail != null) {
                U.putObject(node, AbstractQueuedSynchronizer.Node.PREV, oldTail);
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node;
                    return node;
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
        int ws = node.waitStatus;
        if (ws < 0) {
            node.compareAndSetWaitStatus(ws, 0);
        }
        AbstractQueuedSynchronizer.Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (AbstractQueuedSynchronizer.Node p = this.tail; p != node && p != null; p = p.prev) {
                if (p.waitStatus <= 0) {
                    s = p;
                }
            }
        }
        if (s == null) {
            return;
        }
        LockSupport.unpark(s.thread);
    }

    private void doReleaseShared() {
        while (true) {
            AbstractQueuedSynchronizer.Node h = this.head;
            if (h != null && h != this.tail) {
                int ws = h.waitStatus;
                if (ws == -1) {
                    if (h.compareAndSetWaitStatus(-1, 0)) {
                        unparkSuccessor(h);
                    } else {
                        continue;
                    }
                } else if (ws != 0 || h.compareAndSetWaitStatus(0, -3)) {
                }
            }
            if (h == this.head) {
                return;
            }
        }
    }

    private void setHeadAndPropagate(AbstractQueuedSynchronizer.Node node, long propagate) {
        AbstractQueuedSynchronizer.Node h;
        AbstractQueuedSynchronizer.Node h2 = this.head;
        setHead(node);
        if (propagate <= 0 && h2 != null && h2.waitStatus >= 0 && (h = this.head) != null && h.waitStatus >= 0) {
            return;
        }
        AbstractQueuedSynchronizer.Node s = node.next;
        if (s != null && !s.isShared()) {
            return;
        }
        doReleaseShared();
    }

    private void cancelAcquire(AbstractQueuedSynchronizer.Node node) {
        int ws;
        if (node == null) {
            return;
        }
        node.thread = null;
        AbstractQueuedSynchronizer.Node pred = node.prev;
        while (pred.waitStatus > 0) {
            pred = pred.prev;
            node.prev = pred;
        }
        AbstractQueuedSynchronizer.Node predNext = pred.next;
        node.waitStatus = 1;
        if (node == this.tail && compareAndSetTail(node, pred)) {
            pred.compareAndSetNext(predNext, null);
            return;
        }
        if (pred != this.head && (((ws = pred.waitStatus) == -1 || (ws <= 0 && pred.compareAndSetWaitStatus(ws, -1))) && pred.thread != null)) {
            AbstractQueuedSynchronizer.Node next = node.next;
            if (next != null && next.waitStatus <= 0) {
                pred.compareAndSetNext(predNext, next);
            }
        } else {
            unparkSuccessor(node);
        }
        node.next = node;
    }

    private static boolean shouldParkAfterFailedAcquire(AbstractQueuedSynchronizer.Node pred, AbstractQueuedSynchronizer.Node node) {
        int ws = pred.waitStatus;
        if (ws == -1) {
            return true;
        }
        if (ws > 0) {
            do {
                pred = pred.prev;
                node.prev = pred;
            } while (pred.waitStatus > 0);
            pred.next = node;
        } else {
            pred.compareAndSetWaitStatus(ws, -1);
        }
        return false;
    }

    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    final boolean acquireQueued(AbstractQueuedSynchronizer.Node node, long arg) {
        boolean interrupted = false;
        while (true) {
            try {
                AbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == this.head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    interrupted = true;
                }
            } catch (Throwable t) {
                cancelAcquire(node);
                throw t;
            }
        }
    }

    private void doAcquireInterruptibly(long arg) throws InterruptedException {
        AbstractQueuedSynchronizer.Node node = addWaiter(AbstractQueuedSynchronizer.Node.EXCLUSIVE);
        while (true) {
            try {
                AbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == this.head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    return;
                } else if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            } catch (Throwable t) {
                cancelAcquire(node);
                throw t;
            }
        }
    }

    private boolean doAcquireNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0) {
            return false;
        }
        long deadline = System.nanoTime() + nanosTimeout;
        AbstractQueuedSynchronizer.Node node = addWaiter(AbstractQueuedSynchronizer.Node.EXCLUSIVE);
        do {
            try {
                AbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == this.head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    return true;
                }
                long nanosTimeout2 = deadline - System.nanoTime();
                if (nanosTimeout2 <= 0) {
                    cancelAcquire(node);
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout2 > 1000) {
                    LockSupport.parkNanos(this, nanosTimeout2);
                }
            } catch (Throwable t) {
                cancelAcquire(node);
                throw t;
            }
        } while (!Thread.interrupted());
        throw new InterruptedException();
    }

    private void doAcquireShared(long arg) {
        AbstractQueuedSynchronizer.Node p;
        long r;
        AbstractQueuedSynchronizer.Node node = addWaiter(AbstractQueuedSynchronizer.Node.SHARED);
        boolean interrupted = false;
        while (true) {
            try {
                p = node.predecessor();
                if (p == this.head) {
                    r = tryAcquireShared(arg);
                    if (r >= 0) {
                        break;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    interrupted = true;
                }
            } catch (Throwable t) {
                cancelAcquire(node);
                throw t;
            }
        }
        setHeadAndPropagate(node, r);
        p.next = null;
        if (interrupted) {
            selfInterrupt();
        }
    }

    private void doAcquireSharedInterruptibly(long arg) throws InterruptedException {
        AbstractQueuedSynchronizer.Node node = addWaiter(AbstractQueuedSynchronizer.Node.SHARED);
        while (true) {
            try {
                AbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == this.head) {
                    long r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            } catch (Throwable t) {
                cancelAcquire(node);
                throw t;
            }
        }
    }

    private boolean doAcquireSharedNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0) {
            return false;
        }
        long deadline = System.nanoTime() + nanosTimeout;
        AbstractQueuedSynchronizer.Node node = addWaiter(AbstractQueuedSynchronizer.Node.SHARED);
        do {
            try {
                AbstractQueuedSynchronizer.Node p = node.predecessor();
                if (p == this.head) {
                    long r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null;
                        return true;
                    }
                }
                long nanosTimeout2 = deadline - System.nanoTime();
                if (nanosTimeout2 <= 0) {
                    cancelAcquire(node);
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout2 > 1000) {
                    LockSupport.parkNanos(this, nanosTimeout2);
                }
            } catch (Throwable t) {
                cancelAcquire(node);
                throw t;
            }
        } while (!Thread.interrupted());
        throw new InterruptedException();
    }

    protected boolean tryAcquire(long arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(long arg) {
        throw new UnsupportedOperationException();
    }

    protected long tryAcquireShared(long arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(long arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    public final void acquire(long arg) {
        if (tryAcquire(arg) || !acquireQueued(addWaiter(AbstractQueuedSynchronizer.Node.EXCLUSIVE), arg)) {
            return;
        }
        selfInterrupt();
    }

    public final void acquireInterruptibly(long arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquire(arg)) {
            return;
        }
        doAcquireInterruptibly(arg);
    }

    public final boolean tryAcquireNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquire(arg)) {
            return true;
        }
        return doAcquireNanos(arg, nanosTimeout);
    }

    public final boolean release(long arg) {
        if (!tryRelease(arg)) {
            return false;
        }
        AbstractQueuedSynchronizer.Node h = this.head;
        if (h != null && h.waitStatus != 0) {
            unparkSuccessor(h);
            return true;
        }
        return true;
    }

    public final void acquireShared(long arg) {
        if (tryAcquireShared(arg) >= 0) {
            return;
        }
        doAcquireShared(arg);
    }

    public final void acquireSharedInterruptibly(long arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) >= 0) {
            return;
        }
        doAcquireSharedInterruptibly(arg);
    }

    public final boolean tryAcquireSharedNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) < 0) {
            return doAcquireSharedNanos(arg, nanosTimeout);
        }
        return true;
    }

    public final boolean releaseShared(long arg) {
        if (tryReleaseShared(arg)) {
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
        AbstractQueuedSynchronizer.Node h;
        AbstractQueuedSynchronizer.Node s;
        Thread st;
        AbstractQueuedSynchronizer.Node s2;
        AbstractQueuedSynchronizer.Node h2 = this.head;
        if ((h2 != null && (s2 = h2.next) != null && s2.prev == this.head && (st = s2.thread) != null) || ((h = this.head) != null && (s = h.next) != null && s.prev == this.head && (st = s.thread) != null)) {
            return st;
        }
        Thread firstThread = null;
        for (AbstractQueuedSynchronizer.Node p = this.tail; p != null && p != this.head; p = p.prev) {
            Thread t = p.thread;
            if (t != null) {
                firstThread = t;
            }
        }
        return firstThread;
    }

    public final boolean isQueued(Thread thread) {
        if (thread == null) {
            throw new NullPointerException();
        }
        for (AbstractQueuedSynchronizer.Node p = this.tail; p != null; p = p.prev) {
            if (p.thread == thread) {
                return true;
            }
        }
        return false;
    }

    final boolean apparentlyFirstQueuedIsExclusive() {
        AbstractQueuedSynchronizer.Node s;
        AbstractQueuedSynchronizer.Node h = this.head;
        return (h == null || (s = h.next) == null || s.isShared() || s.thread == null) ? false : true;
    }

    public final boolean hasQueuedPredecessors() {
        AbstractQueuedSynchronizer.Node t = this.tail;
        AbstractQueuedSynchronizer.Node h = this.head;
        if (h == t) {
            return false;
        }
        AbstractQueuedSynchronizer.Node s = h.next;
        return s == null || s.thread != Thread.currentThread();
    }

    public final int getQueueLength() {
        int n = 0;
        for (AbstractQueuedSynchronizer.Node p = this.tail; p != null; p = p.prev) {
            if (p.thread != null) {
                n++;
            }
        }
        return n;
    }

    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for (AbstractQueuedSynchronizer.Node p = this.tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null) {
                list.add(t);
            }
        }
        return list;
    }

    public final Collection<Thread> getExclusiveQueuedThreads() {
        Thread t;
        ArrayList<Thread> list = new ArrayList<>();
        for (AbstractQueuedSynchronizer.Node p = this.tail; p != null; p = p.prev) {
            if (!p.isShared() && (t = p.thread) != null) {
                list.add(t);
            }
        }
        return list;
    }

    public final Collection<Thread> getSharedQueuedThreads() {
        Thread t;
        ArrayList<Thread> list = new ArrayList<>();
        for (AbstractQueuedSynchronizer.Node p = this.tail; p != null; p = p.prev) {
            if (p.isShared() && (t = p.thread) != null) {
                list.add(t);
            }
        }
        return list;
    }

    public String toString() {
        return super.toString() + "[State = " + getState() + ", " + (hasQueuedThreads() ? "non" : "") + "empty queue]";
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
        for (AbstractQueuedSynchronizer.Node p = this.tail; p != node; p = p.prev) {
            if (p == null) {
                return false;
            }
        }
        return true;
    }

    final boolean transferForSignal(AbstractQueuedSynchronizer.Node node) {
        if (!node.compareAndSetWaitStatus(-2, 0)) {
            return false;
        }
        AbstractQueuedSynchronizer.Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !p.compareAndSetWaitStatus(ws, -1)) {
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
            long savedState = getState();
            if (release(savedState)) {
                return savedState;
            }
            throw new IllegalMonitorStateException();
        } catch (Throwable t) {
            node.waitStatus = 1;
            throw t;
        }
    }

    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.hasWaiters();
    }

    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.getWaitQueueLength();
    }

    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition)) {
            throw new IllegalArgumentException("Not owner");
        }
        return condition.getWaitingThreads();
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
            AbstractQueuedSynchronizer.Node t = this.lastWaiter;
            if (t != null && t.waitStatus != -2) {
                unlinkCancelledWaiters();
                t = this.lastWaiter;
            }
            AbstractQueuedSynchronizer.Node node = new AbstractQueuedSynchronizer.Node(-2);
            if (t == null) {
                this.firstWaiter = node;
            } else {
                t.nextWaiter = node;
            }
            this.lastWaiter = node;
            return node;
        }

        private void doSignal(AbstractQueuedSynchronizer.Node first) {
            do {
                AbstractQueuedSynchronizer.Node node = first.nextWaiter;
                this.firstWaiter = node;
                if (node == null) {
                    this.lastWaiter = null;
                }
                first.nextWaiter = null;
                if (AbstractQueuedLongSynchronizer.this.transferForSignal(first)) {
                    return;
                } else {
                    first = this.firstWaiter;
                }
            } while (first != null);
        }

        private void doSignalAll(AbstractQueuedSynchronizer.Node first) {
            AbstractQueuedSynchronizer.Node next;
            this.firstWaiter = null;
            this.lastWaiter = null;
            do {
                next = first.nextWaiter;
                first.nextWaiter = null;
                AbstractQueuedLongSynchronizer.this.transferForSignal(first);
                first = next;
            } while (next != null);
        }

        private void unlinkCancelledWaiters() {
            AbstractQueuedSynchronizer.Node t = this.firstWaiter;
            AbstractQueuedSynchronizer.Node node = null;
            while (t != null) {
                AbstractQueuedSynchronizer.Node next = t.nextWaiter;
                if (t.waitStatus != -2) {
                    t.nextWaiter = null;
                    if (node == null) {
                        this.firstWaiter = next;
                    } else {
                        node.nextWaiter = next;
                    }
                    if (next == null) {
                        this.lastWaiter = node;
                    }
                } else {
                    node = t;
                }
                t = next;
            }
        }

        @Override
        public final void signal() {
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            AbstractQueuedSynchronizer.Node first = this.firstWaiter;
            if (first == null) {
                return;
            }
            doSignal(first);
        }

        @Override
        public final void signalAll() {
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            AbstractQueuedSynchronizer.Node first = this.firstWaiter;
            if (first == null) {
                return;
            }
            doSignalAll(first);
        }

        @Override
        public final void awaitUninterruptibly() {
            AbstractQueuedSynchronizer.Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            boolean interrupted = false;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    interrupted = true;
                }
            }
            if (!AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) && !interrupted) {
                return;
            }
            AbstractQueuedLongSynchronizer.selfInterrupt();
        }

        private int checkInterruptWhileWaiting(AbstractQueuedSynchronizer.Node node) {
            if (Thread.interrupted()) {
                return AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node) ? -1 : 1;
            }
            return 0;
        }

        private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
            if (interruptMode == -1) {
                throw new InterruptedException();
            }
            if (interruptMode != 1) {
                return;
            }
            AbstractQueuedLongSynchronizer.selfInterrupt();
        }

        @Override
        public final void await() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            AbstractQueuedSynchronizer.Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            int interruptMode = 0;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                LockSupport.park(this);
                interruptMode = checkInterruptWhileWaiting(node);
                if (interruptMode != 0) {
                    break;
                }
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
                interruptMode = 1;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode == 0) {
                return;
            }
            reportInterruptAfterWait(interruptMode);
        }

        @Override
        public final long awaitNanos(long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long deadline = System.nanoTime() + nanosTimeout;
            AbstractQueuedSynchronizer.Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            int interruptMode = 0;
            while (true) {
                if (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                    if (nanosTimeout <= 0) {
                        AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node);
                        break;
                    }
                    if (nanosTimeout > 1000) {
                        LockSupport.parkNanos(this, nanosTimeout);
                    }
                    interruptMode = checkInterruptWhileWaiting(node);
                    if (interruptMode != 0) {
                        break;
                    }
                    nanosTimeout = deadline - System.nanoTime();
                } else {
                    break;
                }
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
                interruptMode = 1;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= nanosTimeout) {
                return remaining;
            }
            return Long.MIN_VALUE;
        }

        @Override
        public final boolean awaitUntil(Date deadline) throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            AbstractQueuedSynchronizer.Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (true) {
                if (AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                    break;
                }
                if (System.currentTimeMillis() >= abstime) {
                    timedout = AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                interruptMode = checkInterruptWhileWaiting(node);
                if (interruptMode != 0) {
                    break;
                }
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
                interruptMode = 1;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            return !timedout;
        }

        @Override
        public final boolean await(long time, TimeUnit unit) throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            long deadline = System.nanoTime() + nanosTimeout;
            AbstractQueuedSynchronizer.Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (true) {
                if (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                    if (nanosTimeout <= 0) {
                        timedout = AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node);
                        break;
                    }
                    if (nanosTimeout > 1000) {
                        LockSupport.parkNanos(this, nanosTimeout);
                    }
                    interruptMode = checkInterruptWhileWaiting(node);
                    if (interruptMode != 0) {
                        break;
                    }
                    nanosTimeout = deadline - System.nanoTime();
                } else {
                    break;
                }
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
                interruptMode = 1;
            }
            if (node.nextWaiter != null) {
                unlinkCancelledWaiters();
            }
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
            return !timedout;
        }

        final boolean isOwnedBy(AbstractQueuedLongSynchronizer sync) {
            return sync == AbstractQueuedLongSynchronizer.this;
        }

        protected final boolean hasWaiters() {
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            for (AbstractQueuedSynchronizer.Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == -2) {
                    return true;
                }
            }
            return false;
        }

        protected final int getWaitQueueLength() {
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int n = 0;
            for (AbstractQueuedSynchronizer.Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == -2) {
                    n++;
                }
            }
            return n;
        }

        protected final Collection<Thread> getWaitingThreads() {
            Thread t;
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            ArrayList<Thread> list = new ArrayList<>();
            for (AbstractQueuedSynchronizer.Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == -2 && (t = w.thread) != null) {
                    list.add(t);
                }
            }
            return list;
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
        AbstractQueuedSynchronizer.Node h = new AbstractQueuedSynchronizer.Node();
        if (!unsafe.compareAndSwapObject(this, j, (Object) null, h)) {
            return;
        }
        this.tail = h;
    }

    private final boolean compareAndSetTail(AbstractQueuedSynchronizer.Node expect, AbstractQueuedSynchronizer.Node update) {
        return U.compareAndSwapObject(this, TAIL, expect, update);
    }
}
