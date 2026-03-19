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
        static final Node EXCLUSIVE = null;
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
            Node p = this.prev;
            if (p == null) {
                throw new NullPointerException();
            }
            return p;
        }

        Node() {
        }

        Node(Node nextWaiter) {
            this.nextWaiter = nextWaiter;
            U.putObject(this, THREAD, Thread.currentThread());
        }

        Node(int waitStatus) {
            U.putInt(this, WAITSTATUS, waitStatus);
            U.putObject(this, THREAD, Thread.currentThread());
        }

        final boolean compareAndSetWaitStatus(int expect, int update) {
            return U.compareAndSwapInt(this, WAITSTATUS, expect, update);
        }

        final boolean compareAndSetNext(Node expect, Node update) {
            return U.compareAndSwapObject(this, NEXT, expect, update);
        }
    }

    protected final int getState() {
        return this.state;
    }

    protected final void setState(int newState) {
        this.state = newState;
    }

    protected final boolean compareAndSetState(int expect, int update) {
        return U.compareAndSwapInt(this, STATE, expect, update);
    }

    private Node enq(Node node) {
        while (true) {
            Node oldTail = this.tail;
            if (oldTail != null) {
                U.putObject(node, Node.PREV, oldTail);
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node;
                    return oldTail;
                }
            } else {
                initializeSyncQueue();
            }
        }
    }

    private Node addWaiter(Node mode) {
        Node node = new Node(mode);
        while (true) {
            Node oldTail = this.tail;
            if (oldTail != null) {
                U.putObject(node, Node.PREV, oldTail);
                if (compareAndSetTail(oldTail, node)) {
                    oldTail.next = node;
                    return node;
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
        int ws = node.waitStatus;
        if (ws < 0) {
            node.compareAndSetWaitStatus(ws, 0);
        }
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node p = this.tail; p != node && p != null; p = p.prev) {
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
            Node h = this.head;
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

    private void setHeadAndPropagate(Node node, int propagate) {
        Node h;
        Node h2 = this.head;
        setHead(node);
        if (propagate <= 0 && h2 != null && h2.waitStatus >= 0 && (h = this.head) != null && h.waitStatus >= 0) {
            return;
        }
        Node s = node.next;
        if (s != null && !s.isShared()) {
            return;
        }
        doReleaseShared();
    }

    private void cancelAcquire(Node node) {
        int ws;
        if (node == null) {
            return;
        }
        node.thread = null;
        Node pred = node.prev;
        while (pred.waitStatus > 0) {
            pred = pred.prev;
            node.prev = pred;
        }
        Node predNext = pred.next;
        node.waitStatus = 1;
        if (node == this.tail && compareAndSetTail(node, pred)) {
            pred.compareAndSetNext(predNext, null);
            return;
        }
        if (pred != this.head && (((ws = pred.waitStatus) == -1 || (ws <= 0 && pred.compareAndSetWaitStatus(ws, -1))) && pred.thread != null)) {
            Node next = node.next;
            if (next != null && next.waitStatus <= 0) {
                pred.compareAndSetNext(predNext, next);
            }
        } else {
            unparkSuccessor(node);
        }
        node.next = node;
    }

    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
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

    final boolean acquireQueued(Node node, int arg) {
        boolean interrupted = false;
        while (true) {
            try {
                Node p = node.predecessor();
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

    private void doAcquireInterruptibly(int arg) throws InterruptedException {
        Node node = addWaiter(Node.EXCLUSIVE);
        while (true) {
            try {
                Node p = node.predecessor();
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

    private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0) {
            return false;
        }
        long deadline = System.nanoTime() + nanosTimeout;
        Node node = addWaiter(Node.EXCLUSIVE);
        do {
            try {
                Node p = node.predecessor();
                if (p == this.head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    return true;
                }
                long nanosTimeout2 = deadline - System.nanoTime();
                if (nanosTimeout2 <= 0) {
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout2 > 1000) {
                    LockSupport.parkNanos(this, nanosTimeout2);
                }
            } finally {
                cancelAcquire(node);
            }
        } while (!Thread.interrupted());
        throw new InterruptedException();
    }

    private void doAcquireShared(int arg) {
        Node p;
        int r;
        Node node = addWaiter(Node.SHARED);
        boolean interrupted = false;
        while (true) {
            try {
                p = node.predecessor();
                if (p == this.head && (r = tryAcquireShared(arg)) >= 0) {
                    break;
                } else if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
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

    private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
        int r;
        Node node = addWaiter(Node.SHARED);
        while (true) {
            try {
                Node p = node.predecessor();
                if (p == this.head && (r = tryAcquireShared(arg)) >= 0) {
                    setHeadAndPropagate(node, r);
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

    private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        int r;
        if (nanosTimeout <= 0) {
            return false;
        }
        long deadline = System.nanoTime() + nanosTimeout;
        Node node = addWaiter(Node.SHARED);
        do {
            try {
                Node p = node.predecessor();
                if (p == this.head && (r = tryAcquireShared(arg)) >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null;
                    return true;
                }
                long nanosTimeout2 = deadline - System.nanoTime();
                if (nanosTimeout2 <= 0) {
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout2 > 1000) {
                    LockSupport.parkNanos(this, nanosTimeout2);
                }
            } finally {
                cancelAcquire(node);
            }
        } while (!Thread.interrupted());
        throw new InterruptedException();
    }

    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    public final void acquire(int arg) {
        if (tryAcquire(arg) || !acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) {
            return;
        }
        selfInterrupt();
    }

    public final void acquireInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquire(arg)) {
            return;
        }
        doAcquireInterruptibly(arg);
    }

    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquire(arg)) {
            return true;
        }
        return doAcquireNanos(arg, nanosTimeout);
    }

    public final boolean release(int arg) {
        if (!tryRelease(arg)) {
            return false;
        }
        Node h = this.head;
        if (h != null && h.waitStatus != 0) {
            unparkSuccessor(h);
            return true;
        }
        return true;
    }

    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) >= 0) {
            return;
        }
        doAcquireShared(arg);
    }

    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) >= 0) {
            return;
        }
        doAcquireSharedInterruptibly(arg);
    }

    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) < 0) {
            return doAcquireSharedNanos(arg, nanosTimeout);
        }
        return true;
    }

    public final boolean releaseShared(int arg) {
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
        Node h;
        Node s;
        Thread st;
        Node s2;
        Node h2 = this.head;
        if ((h2 != null && (s2 = h2.next) != null && s2.prev == this.head && (st = s2.thread) != null) || ((h = this.head) != null && (s = h.next) != null && s.prev == this.head && (st = s.thread) != null)) {
            return st;
        }
        Thread firstThread = null;
        for (Node p = this.tail; p != null && p != this.head; p = p.prev) {
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
        for (Node p = this.tail; p != null; p = p.prev) {
            if (p.thread == thread) {
                return true;
            }
        }
        return false;
    }

    final boolean apparentlyFirstQueuedIsExclusive() {
        Node s;
        Node h = this.head;
        return (h == null || (s = h.next) == null || s.isShared() || s.thread == null) ? false : true;
    }

    public final boolean hasQueuedPredecessors() {
        Node t = this.tail;
        Node h = this.head;
        if (h == t) {
            return false;
        }
        Node s = h.next;
        return s == null || s.thread != Thread.currentThread();
    }

    public final int getQueueLength() {
        int n = 0;
        for (Node p = this.tail; p != null; p = p.prev) {
            if (p.thread != null) {
                n++;
            }
        }
        return n;
    }

    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<>();
        for (Node p = this.tail; p != null; p = p.prev) {
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
        for (Node p = this.tail; p != null; p = p.prev) {
            if (!p.isShared() && (t = p.thread) != null) {
                list.add(t);
            }
        }
        return list;
    }

    public final Collection<Thread> getSharedQueuedThreads() {
        Thread t;
        ArrayList<Thread> list = new ArrayList<>();
        for (Node p = this.tail; p != null; p = p.prev) {
            if (p.isShared() && (t = p.thread) != null) {
                list.add(t);
            }
        }
        return list;
    }

    public String toString() {
        return super.toString() + "[State = " + getState() + ", " + (hasQueuedThreads() ? "non" : "") + "empty queue]";
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
        for (Node p = this.tail; p != node; p = p.prev) {
            if (p == null) {
                return false;
            }
        }
        return true;
    }

    final boolean transferForSignal(Node node) {
        if (!node.compareAndSetWaitStatus(-2, 0)) {
            return false;
        }
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !p.compareAndSetWaitStatus(ws, -1)) {
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
            int savedState = getState();
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
        private transient Node firstWaiter;
        private transient Node lastWaiter;

        public ConditionObject() {
        }

        private Node addConditionWaiter() {
            Node t = this.lastWaiter;
            if (t != null && t.waitStatus != -2) {
                unlinkCancelledWaiters();
                t = this.lastWaiter;
            }
            Node node = new Node(-2);
            if (t == null) {
                this.firstWaiter = node;
            } else {
                t.nextWaiter = node;
            }
            this.lastWaiter = node;
            return node;
        }

        private void doSignal(Node first) {
            do {
                Node node = first.nextWaiter;
                this.firstWaiter = node;
                if (node == null) {
                    this.lastWaiter = null;
                }
                first.nextWaiter = null;
                if (AbstractQueuedSynchronizer.this.transferForSignal(first)) {
                    return;
                } else {
                    first = this.firstWaiter;
                }
            } while (first != null);
        }

        private void doSignalAll(Node first) {
            Node next;
            this.firstWaiter = null;
            this.lastWaiter = null;
            do {
                next = first.nextWaiter;
                first.nextWaiter = null;
                AbstractQueuedSynchronizer.this.transferForSignal(first);
                first = next;
            } while (next != null);
        }

        private void unlinkCancelledWaiters() {
            Node t = this.firstWaiter;
            Node node = null;
            while (t != null) {
                Node next = t.nextWaiter;
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
            if (!AbstractQueuedSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            Node first = this.firstWaiter;
            if (first == null) {
                return;
            }
            doSignal(first);
        }

        @Override
        public final void signalAll() {
            if (!AbstractQueuedSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            Node first = this.firstWaiter;
            if (first == null) {
                return;
            }
            doSignalAll(first);
        }

        @Override
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = AbstractQueuedSynchronizer.this.fullyRelease(node);
            boolean interrupted = false;
            while (!AbstractQueuedSynchronizer.this.isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    interrupted = true;
                }
            }
            if (!AbstractQueuedSynchronizer.this.acquireQueued(node, savedState) && !interrupted) {
                return;
            }
            AbstractQueuedSynchronizer.selfInterrupt();
        }

        private int checkInterruptWhileWaiting(Node node) {
            if (Thread.interrupted()) {
                return AbstractQueuedSynchronizer.this.transferAfterCancelledWait(node) ? -1 : 1;
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
            AbstractQueuedSynchronizer.selfInterrupt();
        }

        @Override
        public final void await() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node node = addConditionWaiter();
            int savedState = AbstractQueuedSynchronizer.this.fullyRelease(node);
            int interruptMode = 0;
            while (!AbstractQueuedSynchronizer.this.isOnSyncQueue(node)) {
                LockSupport.park(this);
                interruptMode = checkInterruptWhileWaiting(node);
                if (interruptMode != 0) {
                    break;
                }
            }
            if (AbstractQueuedSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
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
            Node node = addConditionWaiter();
            int savedState = AbstractQueuedSynchronizer.this.fullyRelease(node);
            int interruptMode = 0;
            while (true) {
                if (!AbstractQueuedSynchronizer.this.isOnSyncQueue(node)) {
                    if (nanosTimeout <= 0) {
                        AbstractQueuedSynchronizer.this.transferAfterCancelledWait(node);
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
            if (AbstractQueuedSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
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
            Node node = addConditionWaiter();
            int savedState = AbstractQueuedSynchronizer.this.fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (true) {
                if (AbstractQueuedSynchronizer.this.isOnSyncQueue(node)) {
                    break;
                }
                if (System.currentTimeMillis() >= abstime) {
                    timedout = AbstractQueuedSynchronizer.this.transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                interruptMode = checkInterruptWhileWaiting(node);
                if (interruptMode != 0) {
                    break;
                }
            }
            if (AbstractQueuedSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
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
            Node node = addConditionWaiter();
            int savedState = AbstractQueuedSynchronizer.this.fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (true) {
                if (!AbstractQueuedSynchronizer.this.isOnSyncQueue(node)) {
                    if (nanosTimeout <= 0) {
                        timedout = AbstractQueuedSynchronizer.this.transferAfterCancelledWait(node);
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
            if (AbstractQueuedSynchronizer.this.acquireQueued(node, savedState) && interruptMode != -1) {
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

        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        protected final boolean hasWaiters() {
            if (!AbstractQueuedSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            for (Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == -2) {
                    return true;
                }
            }
            return false;
        }

        protected final int getWaitQueueLength() {
            if (!AbstractQueuedSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int n = 0;
            for (Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == -2) {
                    n++;
                }
            }
            return n;
        }

        protected final Collection<Thread> getWaitingThreads() {
            Thread t;
            if (!AbstractQueuedSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            ArrayList<Thread> list = new ArrayList<>();
            for (Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == -2 && (t = w.thread) != null) {
                    list.add(t);
                }
            }
            return list;
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
        Node h = new Node();
        if (!unsafe.compareAndSwapObject(this, j, (Object) null, h)) {
            return;
        }
        this.tail = h;
    }

    private final boolean compareAndSetTail(Node expect, Node update) {
        return U.compareAndSwapObject(this, TAIL, expect, update);
    }
}
