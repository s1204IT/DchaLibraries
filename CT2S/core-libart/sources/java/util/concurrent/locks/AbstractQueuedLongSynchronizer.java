package java.util.concurrent.locks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

public abstract class AbstractQueuedLongSynchronizer extends AbstractOwnableSynchronizer implements Serializable {
    private static final long headOffset;
    private static final long nextOffset;
    private static final long serialVersionUID = 7373984972572414692L;
    static final long spinForTimeoutThreshold = 1000;
    private static final long stateOffset;
    private static final long tailOffset;
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long waitStatusOffset;
    private volatile transient Node head;
    private volatile long state;
    private volatile transient Node tail;

    protected AbstractQueuedLongSynchronizer() {
    }

    static final class Node {
        static final int CANCELLED = 1;
        static final int CONDITION = -2;
        static final int PROPAGATE = -3;
        static final int SIGNAL = -1;
        volatile Node next;
        Node nextWaiter;
        volatile Node prev;
        volatile Thread thread;
        volatile int waitStatus;
        static final Node SHARED = new Node();
        static final Node EXCLUSIVE = null;

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

        Node(Thread thread, Node mode) {
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) {
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    protected final long getState() {
        return this.state;
    }

    protected final void setState(long newState) {
        this.state = newState;
    }

    protected final boolean compareAndSetState(long expect, long update) {
        return unsafe.compareAndSwapLong(this, stateOffset, expect, update);
    }

    private Node enq(Node node) {
        while (true) {
            Node t = this.tail;
            if (t == null) {
                if (compareAndSetHead(new Node())) {
                    this.tail = this.head;
                }
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        Node pred = this.tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
            } else {
                enq(node);
            }
        }
        return node;
    }

    private void setHead(Node node) {
        this.head = node;
        node.thread = null;
        node.prev = null;
    }

    private void unparkSuccessor(Node node) {
        int ws = node.waitStatus;
        if (ws < 0) {
            compareAndSetWaitStatus(node, ws, 0);
        }
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            for (Node t = this.tail; t != null && t != node; t = t.prev) {
                if (t.waitStatus <= 0) {
                    s = t;
                }
            }
        }
        if (s != null) {
            LockSupport.unpark(s.thread);
        }
    }

    private void doReleaseShared() {
        while (true) {
            Node h = this.head;
            if (h != null && h != this.tail) {
                int ws = h.waitStatus;
                if (ws == -1) {
                    if (compareAndSetWaitStatus(h, -1, 0)) {
                        unparkSuccessor(h);
                    } else {
                        continue;
                    }
                } else if (ws != 0 || compareAndSetWaitStatus(h, 0, -3)) {
                }
            }
            if (h == this.head) {
                return;
            }
        }
    }

    private void setHeadAndPropagate(Node node, long propagate) {
        Node h = this.head;
        setHead(node);
        if (propagate > 0 || h == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared()) {
                doReleaseShared();
            }
        }
    }

    private void cancelAcquire(Node node) {
        int ws;
        if (node != null) {
            node.thread = null;
            Node pred = node.prev;
            while (pred.waitStatus > 0) {
                pred = pred.prev;
                node.prev = pred;
            }
            Node predNext = pred.next;
            node.waitStatus = 1;
            if (node == this.tail && compareAndSetTail(node, pred)) {
                compareAndSetNext(pred, predNext, null);
                return;
            }
            if (pred != this.head && (((ws = pred.waitStatus) == -1 || (ws <= 0 && compareAndSetWaitStatus(pred, ws, -1))) && pred.thread != null)) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0) {
                    compareAndSetNext(pred, predNext, next);
                }
            } else {
                unparkSuccessor(node);
            }
            node.next = node;
        }
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
            compareAndSetWaitStatus(pred, ws, -1);
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

    final boolean acquireQueued(Node node, long arg) {
        Node p;
        boolean failed = true;
        boolean interrupted = false;
        while (true) {
            try {
                p = node.predecessor();
                if (p == this.head && tryAcquire(arg)) {
                    break;
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    interrupted = true;
                }
            } finally {
                if (failed) {
                    cancelAcquire(node);
                }
            }
        }
        setHead(node);
        p.next = null;
        failed = false;
        return interrupted;
    }

    private void doAcquireInterruptibly(long arg) throws InterruptedException {
        Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        while (true) {
            try {
                Node p = node.predecessor();
                if (p == this.head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    failed = false;
                    if (failed) {
                        return;
                    } else {
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            } finally {
                if (failed) {
                    cancelAcquire(node);
                }
            }
        }
    }

    private boolean doAcquireNanos(long arg, long nanosTimeout) throws InterruptedException {
        boolean z = false;
        if (nanosTimeout > 0) {
            long deadline = System.nanoTime() + nanosTimeout;
            Node node = addWaiter(Node.EXCLUSIVE);
            do {
                try {
                    Node p = node.predecessor();
                    if (p == this.head && tryAcquire(arg)) {
                        setHead(node);
                        p.next = null;
                        z = true;
                        if (0 != 0) {
                            cancelAcquire(node);
                        }
                    } else {
                        long nanosTimeout2 = deadline - System.nanoTime();
                        if (nanosTimeout2 <= 0) {
                            if (1 != 0) {
                                cancelAcquire(node);
                            }
                        } else if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout2 > spinForTimeoutThreshold) {
                            LockSupport.parkNanos(this, nanosTimeout2);
                        }
                    }
                } catch (Throwable th) {
                    if (1 != 0) {
                        cancelAcquire(node);
                    }
                    throw th;
                }
            } while (!Thread.interrupted());
            throw new InterruptedException();
        }
        return z;
    }

    private void doAcquireShared(long arg) {
        Node p;
        long r;
        Node node = addWaiter(Node.SHARED);
        boolean failed = true;
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
            } finally {
                if (failed) {
                    cancelAcquire(node);
                }
            }
        }
        setHeadAndPropagate(node, r);
        p.next = null;
        if (interrupted) {
            selfInterrupt();
        }
        failed = false;
    }

    private void doAcquireSharedInterruptibly(long arg) throws InterruptedException {
        Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        while (true) {
            try {
                Node p = node.predecessor();
                if (p == this.head) {
                    long r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null;
                        failed = false;
                        if (failed) {
                            return;
                        } else {
                            return;
                        }
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) {
                    throw new InterruptedException();
                }
            } finally {
                if (failed) {
                    cancelAcquire(node);
                }
            }
        }
    }

    private boolean doAcquireSharedNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0) {
            return false;
        }
        long deadline = System.nanoTime() + nanosTimeout;
        Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        do {
            try {
                Node p = node.predecessor();
                if (p == this.head) {
                    long r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null;
                        failed = false;
                    }
                }
                long nanosTimeout2 = deadline - System.nanoTime();
                if (nanosTimeout2 <= 0) {
                    if (!failed) {
                        return false;
                    }
                    cancelAcquire(node);
                    return false;
                }
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout2 > spinForTimeoutThreshold) {
                    LockSupport.parkNanos(this, nanosTimeout2);
                }
            } finally {
                if (failed) {
                    cancelAcquire(node);
                }
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
        if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) {
            selfInterrupt();
        }
    }

    public final void acquireInterruptibly(long arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!tryAcquire(arg)) {
            doAcquireInterruptibly(arg);
        }
    }

    public final boolean tryAcquireNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
    }

    public final boolean release(long arg) {
        if (!tryRelease(arg)) {
            return false;
        }
        Node h = this.head;
        if (h != null && h.waitStatus != 0) {
            unparkSuccessor(h);
        }
        return true;
    }

    public final void acquireShared(long arg) {
        if (tryAcquireShared(arg) < 0) {
            doAcquireShared(arg);
        }
    }

    public final void acquireSharedInterruptibly(long arg) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (tryAcquireShared(arg) < 0) {
            doAcquireSharedInterruptibly(arg);
        }
    }

    public final boolean tryAcquireSharedNanos(long arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
    }

    public final boolean releaseShared(long arg) {
        if (!tryReleaseShared(arg)) {
            return false;
        }
        doReleaseShared();
        return true;
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
        for (Node t = this.tail; t != null && t != this.head; t = t.prev) {
            Thread tt = t.thread;
            if (tt != null) {
                firstThread = tt;
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
        Node s;
        Node t = this.tail;
        Node h = this.head;
        return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
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
        long s = getState();
        String q = hasQueuedThreads() ? "non" : "";
        return super.toString() + "[State = " + s + ", " + q + "empty queue]";
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
        for (Node t = this.tail; t != node; t = t.prev) {
            if (t == null) {
                return false;
            }
        }
        return true;
    }

    final boolean transferForSignal(Node node) {
        if (!compareAndSetWaitStatus(node, -2, 0)) {
            return false;
        }
        Node p = enq(node);
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, -1)) {
            LockSupport.unpark(node.thread);
        }
        return true;
    }

    final boolean transferAfterCancelledWait(Node node) {
        if (compareAndSetWaitStatus(node, -2, 0)) {
            enq(node);
            return true;
        }
        while (!isOnSyncQueue(node)) {
            Thread.yield();
        }
        return false;
    }

    final long fullyRelease(Node node) {
        boolean failed = true;
        try {
            long savedState = getState();
            if (release(savedState)) {
                failed = false;
                return savedState;
            }
            throw new IllegalMonitorStateException();
        } finally {
            if (failed) {
                node.waitStatus = 1;
            }
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
            Node node = new Node(Thread.currentThread(), -2);
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
                if (AbstractQueuedLongSynchronizer.this.transferForSignal(first)) {
                    return;
                } else {
                    first = this.firstWaiter;
                }
            } while (first != null);
        }

        private void doSignalAll(Node first) {
            this.firstWaiter = null;
            this.lastWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                AbstractQueuedLongSynchronizer.this.transferForSignal(first);
                first = next;
            } while (first != null);
        }

        private void unlinkCancelledWaiters() {
            Node t = this.firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                if (t.waitStatus != -2) {
                    t.nextWaiter = null;
                    if (trail == null) {
                        this.firstWaiter = next;
                    } else {
                        trail.nextWaiter = next;
                    }
                    if (next == null) {
                        this.lastWaiter = trail;
                    }
                } else {
                    trail = t;
                }
                t = next;
            }
        }

        @Override
        public final void signal() {
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            Node first = this.firstWaiter;
            if (first != null) {
                doSignal(first);
            }
        }

        @Override
        public final void signalAll() {
            if (!AbstractQueuedLongSynchronizer.this.isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            Node first = this.firstWaiter;
            if (first != null) {
                doSignalAll(first);
            }
        }

        @Override
        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            boolean interrupted = false;
            while (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted()) {
                    interrupted = true;
                }
            }
            if (AbstractQueuedLongSynchronizer.this.acquireQueued(node, savedState) || interrupted) {
                AbstractQueuedLongSynchronizer.selfInterrupt();
            }
        }

        private int checkInterruptWhileWaiting(Node node) {
            if (Thread.interrupted()) {
                return AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node) ? -1 : 1;
            }
            return 0;
        }

        private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
            if (interruptMode == -1) {
                throw new InterruptedException();
            }
            if (interruptMode == 1) {
                AbstractQueuedLongSynchronizer.selfInterrupt();
            }
        }

        @Override
        public final void await() throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node node = addConditionWaiter();
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
            if (interruptMode != 0) {
                reportInterruptAfterWait(interruptMode);
            }
        }

        @Override
        public final long awaitNanos(long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (true) {
                if (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                    if (nanosTimeout <= 0) {
                        AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node);
                        break;
                    }
                    if (nanosTimeout >= AbstractQueuedLongSynchronizer.spinForTimeoutThreshold) {
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
            return deadline - System.nanoTime();
        }

        @Override
        public final boolean awaitUntil(Date deadline) throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (true) {
                if (AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                    break;
                }
                if (System.currentTimeMillis() > abstime) {
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
            Node node = addConditionWaiter();
            long savedState = AbstractQueuedLongSynchronizer.this.fullyRelease(node);
            long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (true) {
                if (!AbstractQueuedLongSynchronizer.this.isOnSyncQueue(node)) {
                    if (nanosTimeout <= 0) {
                        timedout = AbstractQueuedLongSynchronizer.this.transferAfterCancelledWait(node);
                        break;
                    }
                    if (nanosTimeout >= AbstractQueuedLongSynchronizer.spinForTimeoutThreshold) {
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
            for (Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
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
            for (Node w = this.firstWaiter; w != null; w = w.nextWaiter) {
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
            stateOffset = unsafe.objectFieldOffset(AbstractQueuedLongSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset(AbstractQueuedLongSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(AbstractQueuedLongSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    private static final boolean compareAndSetWaitStatus(Node node, int expect, int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset, expect, update);
    }

    private static final boolean compareAndSetNext(Node node, Node expect, Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
