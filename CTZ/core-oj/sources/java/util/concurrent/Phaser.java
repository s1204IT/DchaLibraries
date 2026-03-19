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

    private static int unarrivedOf(long j) {
        int i = (int) j;
        if (i == 1) {
            return 0;
        }
        return i & 65535;
    }

    private static int partiesOf(long j) {
        return ((int) j) >>> 16;
    }

    private static int phaseOf(long j) {
        return (int) (j >>> 32);
    }

    private static int arrivedOf(long j) {
        int i = (int) j;
        if (i == 1) {
            return 0;
        }
        return (i >>> 16) - (i & 65535);
    }

    private AtomicReference<QNode> queueFor(int i) {
        return (i & 1) == 0 ? this.evenQ : this.oddQ;
    }

    private String badArrive(long j) {
        return "Attempted arrival of unregistered party for " + stateToString(j);
    }

    private String badRegister(long j) {
        return "Attempt to register more than 65535 parties for " + stateToString(j);
    }

    private int doArrive(int i) {
        long jReconcileState;
        int i2;
        int i3;
        long j;
        long j2;
        Phaser phaser = this.root;
        do {
            jReconcileState = phaser == this ? this.state : reconcileState();
            i2 = (int) (jReconcileState >>> 32);
            if (i2 < 0) {
                return i2;
            }
            int i4 = (int) jReconcileState;
            i3 = i4 == 1 ? 0 : i4 & 65535;
            if (i3 <= 0) {
                throw new IllegalStateException(badArrive(jReconcileState));
            }
            j = jReconcileState - ((long) i);
        } while (!U.compareAndSwapLong(this, STATE, jReconcileState, j));
        if (i3 == 1) {
            long j3 = PARTIES_MASK & j;
            int i5 = ((int) j3) >>> 16;
            if (phaser == this) {
                if (onAdvance(i2, i5)) {
                    j2 = j3 | Long.MIN_VALUE;
                } else if (i5 == 0) {
                    j2 = j3 | 1;
                } else {
                    j2 = j3 | ((long) i5);
                }
                U.compareAndSwapLong(this, STATE, j, j2 | (((long) ((i2 + 1) & Integer.MAX_VALUE)) << 32));
                releaseWaiters(i2);
                return i2;
            }
            if (i5 != 0) {
                return this.parent.doArrive(1);
            }
            int iDoArrive = this.parent.doArrive(ONE_DEREGISTER);
            U.compareAndSwapLong(this, STATE, j, j | 1);
            return iDoArrive;
        }
        return i2;
    }

    private int doRegister(int i) {
        int iDoRegister;
        long j = i;
        long j2 = j | (j << 16);
        Phaser phaser = this.parent;
        while (true) {
            long jReconcileState = phaser == null ? this.state : reconcileState();
            int i2 = (int) jReconcileState;
            int i3 = i2 & 65535;
            if (i > 65535 - (i2 >>> 16)) {
                throw new IllegalStateException(badRegister(jReconcileState));
            }
            iDoRegister = (int) (jReconcileState >>> 32);
            if (iDoRegister < 0) {
                break;
            }
            if (i2 != 1) {
                if (phaser == null || reconcileState() == jReconcileState) {
                    if (i3 == 0) {
                        this.root.internalAwaitAdvance(iDoRegister, null);
                    } else if (U.compareAndSwapLong(this, STATE, jReconcileState, jReconcileState + j2)) {
                        break;
                    }
                }
            } else if (phaser == null) {
                if (U.compareAndSwapLong(this, STATE, jReconcileState, (((long) iDoRegister) << 32) | j2)) {
                    break;
                }
            } else {
                synchronized (this) {
                    if (this.state == jReconcileState) {
                        iDoRegister = phaser.doRegister(1);
                        if (iDoRegister >= 0) {
                            while (!U.compareAndSwapLong(this, STATE, jReconcileState, (((long) iDoRegister) << 32) | j2)) {
                                jReconcileState = this.state;
                                iDoRegister = (int) (this.root.state >>> 32);
                            }
                        }
                    }
                }
            }
        }
        return iDoRegister;
    }

    private long reconcileState() {
        long j;
        Phaser phaser = this.root;
        long j2 = this.state;
        if (phaser == this) {
            return j2;
        }
        long j3 = j2;
        while (true) {
            int i = (int) (phaser.state >>> 32);
            if (i == ((int) (j3 >>> 32))) {
                return j3;
            }
            Unsafe unsafe = U;
            long j4 = STATE;
            long j5 = ((long) i) << 32;
            if (i < 0) {
                j = COUNTS_MASK & j3;
            } else {
                int i2 = ((int) j3) >>> 16;
                j = i2 == 0 ? 1L : (PARTIES_MASK & j3) | ((long) i2);
            }
            long j6 = j5 | j;
            if (!unsafe.compareAndSwapLong(this, j4, j3, j6)) {
                j3 = this.state;
            } else {
                return j6;
            }
        }
    }

    public Phaser() {
        this(null, 0);
    }

    public Phaser(int i) {
        this(null, i);
    }

    public Phaser(Phaser phaser) {
        this(phaser, 0);
    }

    public Phaser(Phaser phaser, int i) {
        long j;
        if ((i >>> 16) != 0) {
            throw new IllegalArgumentException("Illegal number of parties");
        }
        int iDoRegister = 0;
        this.parent = phaser;
        if (phaser != null) {
            Phaser phaser2 = phaser.root;
            this.root = phaser2;
            this.evenQ = phaser2.evenQ;
            this.oddQ = phaser2.oddQ;
            if (i != 0) {
                iDoRegister = phaser.doRegister(1);
            }
        } else {
            this.root = this;
            this.evenQ = new AtomicReference<>();
            this.oddQ = new AtomicReference<>();
        }
        if (i == 0) {
            j = 1;
        } else {
            long j2 = i;
            j = j2 | (((long) iDoRegister) << 32) | (j2 << 16);
        }
        this.state = j;
    }

    public int register() {
        return doRegister(1);
    }

    public int bulkRegister(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        if (i == 0) {
            return getPhase();
        }
        return doRegister(i);
    }

    public int arrive() {
        return doArrive(1);
    }

    public int arriveAndDeregister() {
        return doArrive(ONE_DEREGISTER);
    }

    public int arriveAndAwaitAdvance() {
        long jReconcileState;
        long j;
        int i;
        int i2;
        long j2;
        long j3;
        Phaser phaser = this.root;
        do {
            if (phaser == this) {
                jReconcileState = this.state;
            } else {
                jReconcileState = reconcileState();
            }
            j = jReconcileState;
            i = (int) (j >>> 32);
            if (i < 0) {
                return i;
            }
            int i3 = (int) j;
            i2 = i3 == 1 ? 0 : i3 & 65535;
            if (i2 <= 0) {
                throw new IllegalStateException(badArrive(j));
            }
            j2 = j - 1;
        } while (!U.compareAndSwapLong(this, STATE, j, j2));
        if (i2 > 1) {
            return phaser.internalAwaitAdvance(i, null);
        }
        if (phaser != this) {
            return this.parent.arriveAndAwaitAdvance();
        }
        long j4 = j2 & PARTIES_MASK;
        int i4 = ((int) j4) >>> 16;
        if (onAdvance(i, i4)) {
            j3 = j4 | Long.MIN_VALUE;
        } else if (i4 == 0) {
            j3 = j4 | 1;
        } else {
            j3 = j4 | ((long) i4);
        }
        int i5 = (i + 1) & Integer.MAX_VALUE;
        if (!U.compareAndSwapLong(this, STATE, j2, j3 | (((long) i5) << 32))) {
            return (int) (this.state >>> 32);
        }
        releaseWaiters(i);
        return i5;
    }

    public int awaitAdvance(int i) {
        Phaser phaser = this.root;
        int iReconcileState = (int) ((phaser == this ? this.state : reconcileState()) >>> 32);
        if (i < 0) {
            return i;
        }
        if (iReconcileState == i) {
            return phaser.internalAwaitAdvance(i, null);
        }
        return iReconcileState;
    }

    public int awaitAdvanceInterruptibly(int i) throws InterruptedException {
        Phaser phaser = this.root;
        int iReconcileState = (int) ((phaser == this ? this.state : reconcileState()) >>> 32);
        if (i < 0) {
            return i;
        }
        if (iReconcileState == i) {
            QNode qNode = new QNode(this, i, true, false, 0L);
            int iInternalAwaitAdvance = phaser.internalAwaitAdvance(i, qNode);
            if (qNode.wasInterrupted) {
                throw new InterruptedException();
            }
            return iInternalAwaitAdvance;
        }
        return iReconcileState;
    }

    public int awaitAdvanceInterruptibly(int i, long j, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        long nanos = timeUnit.toNanos(j);
        Phaser phaser = this.root;
        int iReconcileState = (int) ((phaser == this ? this.state : reconcileState()) >>> 32);
        if (i < 0) {
            return i;
        }
        if (iReconcileState == i) {
            QNode qNode = new QNode(this, i, true, true, nanos);
            int iInternalAwaitAdvance = phaser.internalAwaitAdvance(i, qNode);
            if (qNode.wasInterrupted) {
                throw new InterruptedException();
            }
            if (iInternalAwaitAdvance != i) {
                return iInternalAwaitAdvance;
            }
            throw new TimeoutException();
        }
        return iReconcileState;
    }

    public void forceTermination() {
        long j;
        Phaser phaser = this.root;
        do {
            j = phaser.state;
            if (j < 0) {
                return;
            }
        } while (!U.compareAndSwapLong(phaser, STATE, j, Long.MIN_VALUE | j));
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

    protected boolean onAdvance(int i, int i2) {
        return i2 == 0;
    }

    public String toString() {
        return stateToString(reconcileState());
    }

    private String stateToString(long j) {
        return super.toString() + "[phase = " + phaseOf(j) + " parties = " + partiesOf(j) + " arrived = " + arrivedOf(j) + "]";
    }

    private void releaseWaiters(int i) {
        Thread thread;
        AtomicReference<QNode> atomicReference = (i & 1) == 0 ? this.evenQ : this.oddQ;
        while (true) {
            QNode qNode = atomicReference.get();
            if (qNode != null && qNode.phase != ((int) (this.root.state >>> 32))) {
                if (atomicReference.compareAndSet(qNode, qNode.next) && (thread = qNode.thread) != null) {
                    qNode.thread = null;
                    LockSupport.unpark(thread);
                }
            } else {
                return;
            }
        }
    }

    private int abortWait(int i) {
        int i2;
        Thread thread;
        AtomicReference<QNode> atomicReference = (i & 1) == 0 ? this.evenQ : this.oddQ;
        while (true) {
            QNode qNode = atomicReference.get();
            i2 = (int) (this.root.state >>> 32);
            if (qNode == null || ((thread = qNode.thread) != null && qNode.phase == i2)) {
                break;
            }
            if (atomicReference.compareAndSet(qNode, qNode.next) && thread != null) {
                qNode.thread = null;
                LockSupport.unpark(thread);
            }
        }
        return i2;
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

    private int internalAwaitAdvance(int i, QNode qNode) {
        int i2;
        releaseWaiters(i - 1);
        int i3 = 0;
        int i4 = SPINS_PER_ARRIVAL;
        boolean zCompareAndSet = false;
        while (true) {
            long j = this.state;
            i2 = (int) (j >>> 32);
            if (i2 != i) {
                break;
            }
            if (qNode == null) {
                int i5 = ((int) j) & 65535;
                if (i5 != i3) {
                    if (i5 < NCPU) {
                        i4 += SPINS_PER_ARRIVAL;
                    }
                    i3 = i5;
                }
                boolean zInterrupted = Thread.interrupted();
                if (zInterrupted || i4 - 1 < 0) {
                    qNode = new QNode(this, i, false, false, 0L);
                    qNode.wasInterrupted = zInterrupted;
                }
            } else {
                if (qNode.isReleasable()) {
                    break;
                }
                if (!zCompareAndSet) {
                    AtomicReference<QNode> atomicReference = (i & 1) == 0 ? this.evenQ : this.oddQ;
                    QNode qNode2 = atomicReference.get();
                    qNode.next = qNode2;
                    if (qNode2 == null || qNode2.phase == i) {
                        if (((int) (this.state >>> 32)) == i) {
                            zCompareAndSet = atomicReference.compareAndSet(qNode2, qNode);
                        }
                    }
                } else {
                    try {
                        ForkJoinPool.managedBlock(qNode);
                    } catch (InterruptedException e) {
                        qNode.wasInterrupted = true;
                    }
                }
            }
        }
        if (qNode != null) {
            if (qNode.thread != null) {
                qNode.thread = null;
            }
            if (qNode.wasInterrupted && !qNode.interruptible) {
                Thread.currentThread().interrupt();
            }
            if (i2 == i && (i2 = (int) (this.state >>> 32)) == i) {
                return abortWait(i);
            }
        }
        releaseWaiters(i);
        return i2;
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

        QNode(Phaser phaser, int i, boolean z, boolean z2, long j) {
            this.phaser = phaser;
            this.phase = i;
            this.interruptible = z;
            this.nanos = j;
            this.timed = z2;
            this.deadline = z2 ? System.nanoTime() + j : 0L;
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
