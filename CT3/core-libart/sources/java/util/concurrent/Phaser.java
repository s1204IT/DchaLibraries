package java.util.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import sun.misc.Unsafe;

public class Phaser {
    private static final long COUNTS_MASK = 4294967295L;
    private static final int EMPTY = 1;
    private static final int MAX_PARTIES = 65535;
    private static final int MAX_PHASE = Integer.MAX_VALUE;
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final int ONE_ARRIVAL = 1;
    private static final int ONE_DEREGISTER = 65537;
    private static final int ONE_PARTY = 65536;
    private static final long PARTIES_MASK = 4294901760L;
    private static final int PARTIES_SHIFT = 16;
    private static final int PHASE_SHIFT = 32;
    static final int SPINS_PER_ARRIVAL;
    private static final long STATE;
    private static final long TERMINATION_BIT = Long.MIN_VALUE;
    private static final Unsafe U;
    private static final int UNARRIVED_MASK = 65535;
    private final AtomicReference<QNode> evenQ;
    private final AtomicReference<QNode> oddQ;
    private final Phaser parent;
    private final Phaser root;
    private volatile long state;

    private static int unarrivedOf(long s) {
        int counts = (int) s;
        if (counts == 1) {
            return 0;
        }
        return 65535 & counts;
    }

    private static int partiesOf(long s) {
        return ((int) s) >>> 16;
    }

    private static int phaseOf(long s) {
        return (int) (s >>> 32);
    }

    private static int arrivedOf(long s) {
        int counts = (int) s;
        if (counts == 1) {
            return 0;
        }
        return (counts >>> 16) - (65535 & counts);
    }

    private AtomicReference<QNode> queueFor(int phase) {
        return (phase & 1) == 0 ? this.evenQ : this.oddQ;
    }

    private String badArrive(long s) {
        return "Attempted arrival of unregistered party for " + stateToString(s);
    }

    private String badRegister(long s) {
        return "Attempt to register more than 65535 parties for " + stateToString(s);
    }

    private int doArrive(int adjust) {
        long s;
        int phase;
        int unarrived;
        long s2;
        long n;
        Phaser root = this.root;
        do {
            s = root == this ? this.state : reconcileState();
            phase = (int) (s >>> 32);
            if (phase < 0) {
                return phase;
            }
            int counts = (int) s;
            unarrived = counts == 1 ? 0 : counts & 65535;
            if (unarrived <= 0) {
                throw new IllegalStateException(badArrive(s));
            }
            s2 = s - ((long) adjust);
        } while (!U.compareAndSwapLong(this, STATE, s, s2));
        if (unarrived != 1) {
            return phase;
        }
        long n2 = s2 & 4294901760L;
        int nextUnarrived = ((int) n2) >>> 16;
        if (root == this) {
            if (onAdvance(phase, nextUnarrived)) {
                n = n2 | TERMINATION_BIT;
            } else if (nextUnarrived == 0) {
                n = n2 | 1;
            } else {
                n = n2 | ((long) nextUnarrived);
            }
            int nextPhase = (phase + 1) & Integer.MAX_VALUE;
            U.compareAndSwapLong(this, STATE, s2, n | (((long) nextPhase) << 32));
            releaseWaiters(phase);
            return phase;
        }
        if (nextUnarrived == 0) {
            int phase2 = this.parent.doArrive(ONE_DEREGISTER);
            U.compareAndSwapLong(this, STATE, s2, s2 | 1);
            return phase2;
        }
        return this.parent.doArrive(1);
    }

    private int doRegister(int registrations) {
        int phase;
        long adjust = (((long) registrations) << 16) | ((long) registrations);
        Phaser parent = this.parent;
        while (true) {
            long s = parent == null ? this.state : reconcileState();
            int counts = (int) s;
            int parties = counts >>> 16;
            int unarrived = counts & 65535;
            if (registrations > 65535 - parties) {
                throw new IllegalStateException(badRegister(s));
            }
            phase = (int) (s >>> 32);
            if (phase < 0) {
                break;
            }
            if (counts != 1) {
                if (parent == null || reconcileState() == s) {
                    if (unarrived == 0) {
                        this.root.internalAwaitAdvance(phase, null);
                    } else if (U.compareAndSwapLong(this, STATE, s, s + adjust)) {
                        break;
                    }
                }
            } else if (parent == null) {
                long next = (((long) phase) << 32) | adjust;
                if (U.compareAndSwapLong(this, STATE, s, next)) {
                    break;
                }
            } else {
                synchronized (this) {
                    if (this.state == s) {
                        break;
                    }
                }
            }
        }
        return phase;
    }

    private long reconcileState() {
        long j;
        Phaser root = this.root;
        long s = this.state;
        if (root == this) {
            return s;
        }
        while (true) {
            int phase = (int) (root.state >>> 32);
            if (phase != ((int) (s >>> 32))) {
                Unsafe unsafe = U;
                long j2 = STATE;
                long j3 = ((long) phase) << 32;
                if (phase < 0) {
                    j = COUNTS_MASK & s;
                } else {
                    int p = ((int) s) >>> 16;
                    j = p == 0 ? 1L : (4294901760L & s) | ((long) p);
                }
                long s2 = j3 | j;
                if (unsafe.compareAndSwapLong(this, j2, s, s2)) {
                    return s2;
                }
                s = this.state;
            } else {
                return s;
            }
        }
    }

    public Phaser() {
        this(null, 0);
    }

    public Phaser(int parties) {
        this(null, parties);
    }

    public Phaser(Phaser parent) {
        this(parent, 0);
    }

    public Phaser(Phaser parent, int parties) {
        if ((parties >>> 16) != 0) {
            throw new IllegalArgumentException("Illegal number of parties");
        }
        int phase = 0;
        this.parent = parent;
        if (parent != null) {
            Phaser root = parent.root;
            this.root = root;
            this.evenQ = root.evenQ;
            this.oddQ = root.oddQ;
            if (parties != 0) {
                phase = parent.doRegister(1);
            }
        } else {
            this.root = this;
            this.evenQ = new AtomicReference<>();
            this.oddQ = new AtomicReference<>();
        }
        this.state = parties == 0 ? 1L : (((long) phase) << 32) | (((long) parties) << 16) | ((long) parties);
    }

    public int register() {
        return doRegister(1);
    }

    public int bulkRegister(int parties) {
        if (parties < 0) {
            throw new IllegalArgumentException();
        }
        if (parties == 0) {
            return getPhase();
        }
        return doRegister(parties);
    }

    public int arrive() {
        return doArrive(1);
    }

    public int arriveAndDeregister() {
        return doArrive(ONE_DEREGISTER);
    }

    public int arriveAndAwaitAdvance() {
        long s;
        int phase;
        int unarrived;
        long s2;
        long n;
        Phaser root = this.root;
        do {
            s = root == this ? this.state : reconcileState();
            phase = (int) (s >>> 32);
            if (phase < 0) {
                return phase;
            }
            int counts = (int) s;
            unarrived = counts == 1 ? 0 : counts & 65535;
            if (unarrived <= 0) {
                throw new IllegalStateException(badArrive(s));
            }
            s2 = s - 1;
        } while (!U.compareAndSwapLong(this, STATE, s, s2));
        if (unarrived > 1) {
            return root.internalAwaitAdvance(phase, null);
        }
        if (root != this) {
            return this.parent.arriveAndAwaitAdvance();
        }
        long n2 = s2 & 4294901760L;
        int nextUnarrived = ((int) n2) >>> 16;
        if (onAdvance(phase, nextUnarrived)) {
            n = n2 | TERMINATION_BIT;
        } else if (nextUnarrived == 0) {
            n = n2 | 1;
        } else {
            n = n2 | ((long) nextUnarrived);
        }
        int nextPhase = (phase + 1) & Integer.MAX_VALUE;
        if (!U.compareAndSwapLong(this, STATE, s2, n | (((long) nextPhase) << 32))) {
            return (int) (this.state >>> 32);
        }
        releaseWaiters(phase);
        return nextPhase;
    }

    public int awaitAdvance(int phase) {
        Phaser root = this.root;
        long s = root == this ? this.state : reconcileState();
        int p = (int) (s >>> 32);
        if (phase < 0) {
            return phase;
        }
        if (p == phase) {
            return root.internalAwaitAdvance(phase, null);
        }
        return p;
    }

    public int awaitAdvanceInterruptibly(int phase) throws InterruptedException {
        Phaser root = this.root;
        long s = root == this ? this.state : reconcileState();
        int p = (int) (s >>> 32);
        if (phase < 0) {
            return phase;
        }
        if (p == phase) {
            QNode node = new QNode(this, phase, true, false, 0L);
            p = root.internalAwaitAdvance(phase, node);
            if (node.wasInterrupted) {
                throw new InterruptedException();
            }
        }
        return p;
    }

    public int awaitAdvanceInterruptibly(int phase, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        long nanos = unit.toNanos(timeout);
        Phaser root = this.root;
        long s = root == this ? this.state : reconcileState();
        int p = (int) (s >>> 32);
        if (phase < 0) {
            return phase;
        }
        if (p == phase) {
            QNode node = new QNode(this, phase, true, true, nanos);
            p = root.internalAwaitAdvance(phase, node);
            if (node.wasInterrupted) {
                throw new InterruptedException();
            }
            if (p == phase) {
                throw new TimeoutException();
            }
        }
        return p;
    }

    public void forceTermination() {
        long s;
        Phaser root = this.root;
        do {
            s = root.state;
            if (s < 0) {
                return;
            }
        } while (!U.compareAndSwapLong(root, STATE, s, TERMINATION_BIT | s));
        releaseWaiters(0);
        releaseWaiters(1);
    }

    public final int getPhase() {
        return (int) (this.root.state >>> 32);
    }

    public int getRegisteredParties() {
        return partiesOf(this.state);
    }

    public int getArrivedParties() {
        return arrivedOf(reconcileState());
    }

    public int getUnarrivedParties() {
        return unarrivedOf(reconcileState());
    }

    public Phaser getParent() {
        return this.parent;
    }

    public Phaser getRoot() {
        return this.root;
    }

    public boolean isTerminated() {
        return this.root.state < 0;
    }

    protected boolean onAdvance(int phase, int registeredParties) {
        return registeredParties == 0;
    }

    public String toString() {
        return stateToString(reconcileState());
    }

    private String stateToString(long s) {
        return super.toString() + "[phase = " + phaseOf(s) + " parties = " + partiesOf(s) + " arrived = " + arrivedOf(s) + "]";
    }

    private void releaseWaiters(int phase) {
        Thread t;
        AtomicReference<QNode> head = (phase & 1) == 0 ? this.evenQ : this.oddQ;
        while (true) {
            QNode q = head.get();
            if (q == null || q.phase == ((int) (this.root.state >>> 32))) {
                return;
            }
            if (head.compareAndSet(q, q.next) && (t = q.thread) != null) {
                q.thread = null;
                LockSupport.unpark(t);
            }
        }
    }

    private int abortWait(int phase) {
        int p;
        Thread t;
        AtomicReference<QNode> head = (phase & 1) == 0 ? this.evenQ : this.oddQ;
        while (true) {
            QNode q = head.get();
            p = (int) (this.root.state >>> 32);
            if (q == null || ((t = q.thread) != null && q.phase == p)) {
                break;
            }
            if (head.compareAndSet(q, q.next) && t != null) {
                q.thread = null;
                LockSupport.unpark(t);
            }
        }
        return p;
    }

    static {
        SPINS_PER_ARRIVAL = NCPU < 2 ? 1 : 256;
        U = Unsafe.getUnsafe();
        try {
            STATE = U.objectFieldOffset(Phaser.class.getDeclaredField("state"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private int internalAwaitAdvance(int phase, QNode node) {
        int p;
        releaseWaiters(phase - 1);
        boolean queued = false;
        int lastUnarrived = 0;
        int spins = SPINS_PER_ARRIVAL;
        while (true) {
            long s = this.state;
            p = (int) (s >>> 32);
            if (p != phase) {
                break;
            }
            if (node == null) {
                int unarrived = ((int) s) & 65535;
                if (unarrived != lastUnarrived) {
                    lastUnarrived = unarrived;
                    if (unarrived < NCPU) {
                        spins += SPINS_PER_ARRIVAL;
                    }
                }
                boolean interrupted = Thread.interrupted();
                if (interrupted || spins - 1 < 0) {
                    node = new QNode(this, phase, false, false, 0L);
                    node.wasInterrupted = interrupted;
                }
            } else {
                if (node.isReleasable()) {
                    break;
                }
                if (!queued) {
                    AtomicReference<QNode> head = (phase & 1) == 0 ? this.evenQ : this.oddQ;
                    QNode q = head.get();
                    node.next = q;
                    if (q == null || q.phase == phase) {
                        if (((int) (this.state >>> 32)) == phase) {
                            queued = head.compareAndSet(q, node);
                        }
                    }
                } else {
                    try {
                        ForkJoinPool.managedBlock(node);
                    } catch (InterruptedException e) {
                        node.wasInterrupted = true;
                    }
                }
            }
        }
        if (node != null) {
            if (node.thread != null) {
                node.thread = null;
            }
            if (node.wasInterrupted && !node.interruptible) {
                Thread.currentThread().interrupt();
            }
            if (p == phase && (p = (int) (this.state >>> 32)) == phase) {
                return abortWait(phase);
            }
        }
        releaseWaiters(phase);
        return p;
    }

    static final class QNode implements ForkJoinPool.ManagedBlocker {
        final long deadline;
        final boolean interruptible;
        long nanos;
        QNode next;
        final int phase;
        final Phaser phaser;
        volatile Thread thread;
        final boolean timed;
        boolean wasInterrupted;

        QNode(Phaser phaser, int phase, boolean interruptible, boolean timed, long nanos) {
            this.phaser = phaser;
            this.phase = phase;
            this.interruptible = interruptible;
            this.nanos = nanos;
            this.timed = timed;
            this.deadline = timed ? System.nanoTime() + nanos : 0L;
            this.thread = Thread.currentThread();
        }

        @Override
        public boolean isReleasable() {
            if (this.thread == null) {
                return true;
            }
            if (this.phaser.getPhase() != this.phase) {
                this.thread = null;
                return true;
            }
            if (Thread.interrupted()) {
                this.wasInterrupted = true;
            }
            if (this.wasInterrupted && this.interruptible) {
                this.thread = null;
                return true;
            }
            if (this.timed) {
                if (this.nanos > 0) {
                    long jNanoTime = this.deadline - System.nanoTime();
                    this.nanos = jNanoTime;
                    if (jNanoTime > 0) {
                        return false;
                    }
                }
                this.thread = null;
                return true;
            }
            return false;
        }

        @Override
        public boolean block() {
            while (!isReleasable()) {
                if (this.timed) {
                    LockSupport.parkNanos(this, this.nanos);
                } else {
                    LockSupport.park(this);
                }
            }
            return true;
        }
    }
}
