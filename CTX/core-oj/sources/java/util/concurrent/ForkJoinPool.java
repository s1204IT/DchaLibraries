package java.util.concurrent;

import java.lang.Thread;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

public class ForkJoinPool extends AbstractExecutorService {
    private static final int ABASE;
    private static final long AC_MASK = -281474976710656L;
    private static final int AC_SHIFT = 48;
    private static final long AC_UNIT = 281474976710656L;
    private static final long ADD_WORKER = 140737488355328L;
    private static final int ASHIFT;
    private static final int COMMON_MAX_SPARES;
    static final int COMMON_PARALLELISM;
    private static final long CTL;
    private static final int DEFAULT_COMMON_MAX_SPARES = 256;
    static final int EVENMASK = 65534;
    static final int FIFO_QUEUE = Integer.MIN_VALUE;
    private static final long IDLE_TIMEOUT_MS = 2000;
    static final int IS_OWNED = 1;
    static final int LIFO_QUEUE = 0;
    static final int MAX_CAP = 32767;
    static final int MODE_MASK = -65536;
    static final int POLL_LIMIT = 1023;
    private static final long RUNSTATE;
    private static final int SEED_INCREMENT = -1640531527;
    private static final int SHUTDOWN = Integer.MIN_VALUE;
    static final int SMASK = 65535;
    static final int SPARE_WORKER = 131072;
    private static final long SP_MASK = 4294967295L;
    static final int SQMASK = 126;
    static final int SS_SEQ = 65536;
    private static final int STARTED = 1;
    private static final int STOP = 2;
    private static final long TC_MASK = 281470681743360L;
    private static final int TC_SHIFT = 32;
    private static final long TC_UNIT = 4294967296L;
    private static final int TERMINATED = 4;
    private static final long TIMEOUT_SLOP_MS = 20;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long UC_MASK = -4294967296L;
    static final int UNREGISTERED = 262144;
    static final int UNSIGNALLED = Integer.MIN_VALUE;
    static final ForkJoinPool common;
    public static final ForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory;
    static final RuntimePermission modifyThreadPermission;
    private static int poolNumberSequence;
    AuxState auxState;
    final int config;
    volatile long ctl;
    final ForkJoinWorkerThreadFactory factory;
    volatile int runState;
    final Thread.UncaughtExceptionHandler ueh;
    volatile WorkQueue[] workQueues;
    final String workerNamePrefix;

    public interface ForkJoinWorkerThreadFactory {
        ForkJoinWorkerThread newThread(ForkJoinPool forkJoinPool);
    }

    public interface ManagedBlocker {
        boolean block() throws InterruptedException;

        boolean isReleasable();
    }

    private static void checkPermission() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(modifyThreadPermission);
        }
    }

    private static final class DefaultForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        private DefaultForkJoinWorkerThreadFactory() {
        }

        @Override
        public final ForkJoinWorkerThread newThread(ForkJoinPool forkJoinPool) {
            return new ForkJoinWorkerThread(forkJoinPool);
        }
    }

    private static final class EmptyTask extends ForkJoinTask<Void> {
        private static final long serialVersionUID = -7721805057305804111L;

        EmptyTask() {
            this.status = -268435456;
        }

        @Override
        public final Void getRawResult() {
            return null;
        }

        @Override
        public final void setRawResult(Void r1) {
        }

        @Override
        public final boolean exec() {
            return true;
        }
    }

    private static final class AuxState extends ReentrantLock {
        private static final long serialVersionUID = -6001602636862214147L;
        long indexSeed;
        volatile long stealCount;

        AuxState() {
        }
    }

    static final class WorkQueue {
        private static final int ABASE;
        private static final int ASHIFT;
        static final int INITIAL_QUEUE_CAPACITY = 8192;
        static final int MAXIMUM_QUEUE_CAPACITY = 67108864;
        private static final long QLOCK;
        private static final Unsafe U = Unsafe.getUnsafe();
        ForkJoinTask<?>[] array;
        int config;
        volatile ForkJoinTask<?> currentJoin;
        volatile ForkJoinTask<?> currentSteal;
        int hint;
        int nsteals;
        final ForkJoinWorkerThread owner;
        volatile Thread parker;
        final ForkJoinPool pool;
        volatile int qlock;
        volatile int scanState;
        int stackPred;
        int top = 4096;
        volatile int base = 4096;

        WorkQueue(ForkJoinPool forkJoinPool, ForkJoinWorkerThread forkJoinWorkerThread) {
            this.pool = forkJoinPool;
            this.owner = forkJoinWorkerThread;
        }

        final int getPoolIndex() {
            return (this.config & ForkJoinPool.SMASK) >>> 1;
        }

        final int queueSize() {
            int i = this.base - this.top;
            if (i >= 0) {
                return 0;
            }
            return -i;
        }

        final boolean isEmpty() {
            int length;
            int i = this.base;
            int i2 = this.top;
            int i3 = i - i2;
            if (i3 >= 0) {
                return true;
            }
            if (i3 == -1) {
                ForkJoinTask<?>[] forkJoinTaskArr = this.array;
                if (forkJoinTaskArr == null || (length = forkJoinTaskArr.length) == 0) {
                    return true;
                }
                if (forkJoinTaskArr[(i2 - 1) & (length - 1)] == null) {
                    return true;
                }
            }
            return false;
        }

        final void push(ForkJoinTask<?> forkJoinTask) {
            int length;
            U.storeFence();
            int i = this.top;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            if (forkJoinTaskArr != null && (length = forkJoinTaskArr.length) > 0) {
                forkJoinTaskArr[(length - 1) & i] = forkJoinTask;
                this.top = i + 1;
                ForkJoinPool forkJoinPool = this.pool;
                int i2 = this.base - i;
                if (i2 == 0 && forkJoinPool != null) {
                    U.fullFence();
                    forkJoinPool.signalWork();
                } else if (length + i2 == 1) {
                    growArray();
                }
            }
        }

        final ForkJoinTask<?>[] growArray() {
            int length;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            int length2 = forkJoinTaskArr != null ? forkJoinTaskArr.length << 1 : 8192;
            if (length2 < 8192 || length2 > MAXIMUM_QUEUE_CAPACITY) {
                throw new RejectedExecutionException("Queue capacity exceeded");
            }
            ForkJoinTask<?>[] forkJoinTaskArr2 = new ForkJoinTask[length2];
            this.array = forkJoinTaskArr2;
            if (forkJoinTaskArr != null && forkJoinTaskArr.length - 1 > 0) {
                int i = this.top;
                int i2 = this.base;
                if (i - i2 > 0) {
                    int i3 = length2 - 1;
                    int i4 = i2;
                    do {
                        long j = ((long) ABASE) + (((long) (i4 & length)) << ASHIFT);
                        ForkJoinTask<?> forkJoinTask = (ForkJoinTask) U.getObjectVolatile(forkJoinTaskArr, j);
                        if (forkJoinTask != null && U.compareAndSwapObject(forkJoinTaskArr, j, forkJoinTask, null)) {
                            forkJoinTaskArr2[i4 & i3] = forkJoinTask;
                        }
                        i4++;
                    } while (i4 != i);
                    U.storeFence();
                }
            }
            return forkJoinTaskArr2;
        }

        final ForkJoinTask<?> pop() {
            int length;
            int i = this.base;
            int i2 = this.top;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            if (forkJoinTaskArr != null && i != i2 && (length = forkJoinTaskArr.length) > 0) {
                int i3 = i2 - 1;
                long j = (((long) ((length - 1) & i3)) << ASHIFT) + ((long) ABASE);
                ForkJoinTask<?> forkJoinTask = (ForkJoinTask) U.getObject(forkJoinTaskArr, j);
                if (forkJoinTask != null && U.compareAndSwapObject(forkJoinTaskArr, j, forkJoinTask, null)) {
                    this.top = i3;
                    return forkJoinTask;
                }
                return null;
            }
            return null;
        }

        final ForkJoinTask<?> pollAt(int i) {
            int length;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            if (forkJoinTaskArr != null && (length = forkJoinTaskArr.length) > 0) {
                long j = (((long) ((length - 1) & i)) << ASHIFT) + ((long) ABASE);
                ForkJoinTask<?> forkJoinTask = (ForkJoinTask) U.getObjectVolatile(forkJoinTaskArr, j);
                if (forkJoinTask != null) {
                    int i2 = i + 1;
                    if (i == this.base && U.compareAndSwapObject(forkJoinTaskArr, j, forkJoinTask, null)) {
                        this.base = i2;
                        return forkJoinTask;
                    }
                    return null;
                }
                return null;
            }
            return null;
        }

        final ForkJoinTask<?> poll() {
            int i;
            int length;
            while (true) {
                int i2 = this.base;
                int i3 = this.top;
                ForkJoinTask<?>[] forkJoinTaskArr = this.array;
                if (forkJoinTaskArr != null && (i = i2 - i3) < 0 && (length = forkJoinTaskArr.length) > 0) {
                    long j = (((long) ((length - 1) & i2)) << ASHIFT) + ((long) ABASE);
                    ForkJoinTask<?> forkJoinTask = (ForkJoinTask) U.getObjectVolatile(forkJoinTaskArr, j);
                    int i4 = i2 + 1;
                    if (i2 == this.base) {
                        if (forkJoinTask != null) {
                            if (U.compareAndSwapObject(forkJoinTaskArr, j, forkJoinTask, null)) {
                                this.base = i4;
                                return forkJoinTask;
                            }
                        } else if (i == -1) {
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            }
        }

        final ForkJoinTask<?> nextLocalTask() {
            return this.config < 0 ? poll() : pop();
        }

        final ForkJoinTask<?> peek() {
            int length;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            if (forkJoinTaskArr == null || (length = forkJoinTaskArr.length) <= 0) {
                return null;
            }
            return forkJoinTaskArr[(length - 1) & (this.config < 0 ? this.base : this.top - 1)];
        }

        final boolean tryUnpush(ForkJoinTask<?> forkJoinTask) {
            int length;
            int i = this.base;
            int i2 = this.top;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            if (forkJoinTaskArr != null && i != i2 && (length = forkJoinTaskArr.length) > 0) {
                int i3 = i2 - 1;
                if (U.compareAndSwapObject(forkJoinTaskArr, (((long) ((length - 1) & i3)) << ASHIFT) + ((long) ABASE), forkJoinTask, null)) {
                    this.top = i3;
                    return true;
                }
                return false;
            }
            return false;
        }

        final int sharedPush(ForkJoinTask<?> forkJoinTask) {
            int length;
            if (!U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                return 1;
            }
            int i = this.base;
            int i2 = this.top;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            int i3 = 0;
            if (forkJoinTaskArr == null || (length = forkJoinTaskArr.length) <= 0) {
                growAndSharedPush(forkJoinTask);
            } else {
                int i4 = length - 1;
                int i5 = i - i2;
                if (i4 + i5 > 0) {
                    forkJoinTaskArr[i4 & i2] = forkJoinTask;
                    this.top = i2 + 1;
                    this.qlock = 0;
                    if (i5 < 0 && i == this.base) {
                        i3 = i5;
                    }
                }
            }
            return i3;
        }

        private void growAndSharedPush(ForkJoinTask<?> forkJoinTask) {
            int length;
            try {
                growArray();
                int i = this.top;
                ForkJoinTask<?>[] forkJoinTaskArr = this.array;
                if (forkJoinTaskArr != null && (length = forkJoinTaskArr.length) > 0) {
                    forkJoinTaskArr[(length - 1) & i] = forkJoinTask;
                    this.top = i + 1;
                }
            } finally {
                this.qlock = 0;
            }
        }

        final boolean trySharedUnpush(ForkJoinTask<?> forkJoinTask) {
            int length;
            boolean z = true;
            int i = this.top - 1;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            if (forkJoinTaskArr != null && (length = forkJoinTaskArr.length) > 0) {
                long j = (((long) ((length - 1) & i)) << ASHIFT) + ((long) ABASE);
                if (((ForkJoinTask) U.getObject(forkJoinTaskArr, j)) == forkJoinTask && U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                    if (this.top == i + 1 && this.array == forkJoinTaskArr && U.compareAndSwapObject(forkJoinTaskArr, j, forkJoinTask, null)) {
                        this.top = i;
                    } else {
                        z = false;
                    }
                    U.putOrderedInt(this, QLOCK, 0);
                    return z;
                }
            }
            return false;
        }

        final void cancelAll() {
            ForkJoinTask<?> forkJoinTask = this.currentJoin;
            if (forkJoinTask != null) {
                this.currentJoin = null;
                ForkJoinTask.cancelIgnoringExceptions(forkJoinTask);
            }
            ForkJoinTask<?> forkJoinTask2 = this.currentSteal;
            if (forkJoinTask2 != null) {
                this.currentSteal = null;
                ForkJoinTask.cancelIgnoringExceptions(forkJoinTask2);
            }
            while (true) {
                ForkJoinTask<?> forkJoinTaskPoll = poll();
                if (forkJoinTaskPoll != null) {
                    ForkJoinTask.cancelIgnoringExceptions(forkJoinTaskPoll);
                } else {
                    return;
                }
            }
        }

        final void localPopAndExec() {
            int length;
            int i = 0;
            do {
                int i2 = this.base;
                int i3 = this.top;
                ForkJoinTask<?>[] forkJoinTaskArr = this.array;
                if (forkJoinTaskArr != null && i2 != i3 && (length = forkJoinTaskArr.length) > 0) {
                    int i4 = i3 - 1;
                    ForkJoinTask<?> forkJoinTask = (ForkJoinTask) U.getAndSetObject(forkJoinTaskArr, (((long) ((length - 1) & i4)) << ASHIFT) + ((long) ABASE), null);
                    if (forkJoinTask != null) {
                        this.top = i4;
                        this.currentSteal = forkJoinTask;
                        forkJoinTask.doExec();
                        i++;
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            } while (i <= 1023);
        }

        final void localPollAndExec() {
            int length;
            int i = 0;
            while (true) {
                int i2 = this.base;
                int i3 = this.top;
                ForkJoinTask<?>[] forkJoinTaskArr = this.array;
                if (forkJoinTaskArr != null && i2 != i3 && (length = forkJoinTaskArr.length) > 0) {
                    int i4 = i2 + 1;
                    ForkJoinTask forkJoinTask = (ForkJoinTask) U.getAndSetObject(forkJoinTaskArr, (((long) (i2 & (length - 1))) << ASHIFT) + ((long) ABASE), null);
                    if (forkJoinTask != null) {
                        this.base = i4;
                        forkJoinTask.doExec();
                        i++;
                        if (i > 1023) {
                            return;
                        }
                    }
                } else {
                    return;
                }
            }
        }

        final void runTask(ForkJoinTask<?> forkJoinTask) {
            if (forkJoinTask != null) {
                forkJoinTask.doExec();
                if (this.config < 0) {
                    localPollAndExec();
                } else {
                    localPopAndExec();
                }
                int i = this.nsteals + 1;
                this.nsteals = i;
                ForkJoinWorkerThread forkJoinWorkerThread = this.owner;
                this.currentSteal = null;
                if (i < 0) {
                    transferStealCount(this.pool);
                }
                if (forkJoinWorkerThread != null) {
                    forkJoinWorkerThread.afterTopLevelExec();
                }
            }
        }

        final void transferStealCount(ForkJoinPool forkJoinPool) {
            AuxState auxState;
            if (forkJoinPool != null && (auxState = forkJoinPool.auxState) != null) {
                long j = this.nsteals;
                this.nsteals = 0;
                if (j < 0) {
                    j = 2147483647L;
                }
                auxState.lock();
                try {
                    auxState.stealCount += j;
                } finally {
                    auxState.unlock();
                }
            }
        }

        final boolean tryRemoveAndExec(ForkJoinTask<?> forkJoinTask) {
            ForkJoinTask<?>[] forkJoinTaskArr;
            int length;
            boolean zCompareAndSwapObject;
            if (forkJoinTask != null && forkJoinTask.status >= 0) {
                do {
                    int i = this.base;
                    int i2 = this.top;
                    int i3 = i - i2;
                    if (i3 < 0 && (forkJoinTaskArr = this.array) != null && (length = forkJoinTaskArr.length) > 0) {
                        while (true) {
                            i2--;
                            long j = (((length - 1) & i2) << ASHIFT) + ABASE;
                            ForkJoinTask<?> forkJoinTask2 = (ForkJoinTask) U.getObjectVolatile(forkJoinTaskArr, j);
                            if (forkJoinTask2 == null) {
                                break;
                            }
                            if (forkJoinTask2 == forkJoinTask) {
                                if (i2 + 1 == this.top) {
                                    if (U.compareAndSwapObject(forkJoinTaskArr, j, forkJoinTask2, null)) {
                                        this.top = i2;
                                        zCompareAndSwapObject = true;
                                    } else {
                                        zCompareAndSwapObject = false;
                                    }
                                    if (zCompareAndSwapObject) {
                                        ForkJoinTask<?> forkJoinTask3 = this.currentSteal;
                                        this.currentSteal = forkJoinTask;
                                        forkJoinTask.doExec();
                                        this.currentSteal = forkJoinTask3;
                                    }
                                } else {
                                    if (this.base == i) {
                                        zCompareAndSwapObject = U.compareAndSwapObject(forkJoinTaskArr, j, forkJoinTask2, new EmptyTask());
                                    }
                                    if (zCompareAndSwapObject) {
                                    }
                                }
                            } else if (forkJoinTask2.status < 0 && i2 + 1 == this.top) {
                                if (U.compareAndSwapObject(forkJoinTaskArr, j, forkJoinTask2, null)) {
                                    this.top = i2;
                                }
                            } else {
                                i3++;
                                if (i3 == 0) {
                                    if (this.base == i) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                } while (forkJoinTask.status >= 0);
                return false;
            }
            return true;
        }

        final CountedCompleter<?> popCC(CountedCompleter<?> countedCompleter, int i) {
            int length;
            int i2 = this.base;
            int i3 = this.top;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            if (forkJoinTaskArr != null && i2 != i3 && (length = forkJoinTaskArr.length) > 0) {
                boolean z = true;
                int i4 = i3 - 1;
                long j = (((long) ((length - 1) & i4)) << ASHIFT) + ((long) ABASE);
                ForkJoinTask forkJoinTask = (ForkJoinTask) U.getObjectVolatile(forkJoinTaskArr, j);
                if (forkJoinTask instanceof CountedCompleter) {
                    CountedCompleter<?> countedCompleter2 = (CountedCompleter) forkJoinTask;
                    CountedCompleter<?> countedCompleter3 = countedCompleter2;
                    while (countedCompleter3 != countedCompleter) {
                        countedCompleter3 = countedCompleter3.completer;
                        if (countedCompleter3 == null) {
                            return null;
                        }
                    }
                    if ((i & 1) == 0) {
                        if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                            if (this.top == i3 && this.array == forkJoinTaskArr && U.compareAndSwapObject(forkJoinTaskArr, j, countedCompleter2, null)) {
                                this.top = i4;
                            } else {
                                z = false;
                            }
                            U.putOrderedInt(this, QLOCK, 0);
                            if (z) {
                                return countedCompleter2;
                            }
                            return null;
                        }
                        return null;
                    }
                    if (U.compareAndSwapObject(forkJoinTaskArr, j, countedCompleter2, null)) {
                        this.top = i4;
                        return countedCompleter2;
                    }
                    return null;
                }
                return null;
            }
            return null;
        }

        final int pollAndExecCC(CountedCompleter<?> countedCompleter) {
            int length;
            int i = this.base;
            int i2 = this.top;
            ForkJoinTask<?>[] forkJoinTaskArr = this.array;
            if (forkJoinTaskArr != null && i != i2 && (length = forkJoinTaskArr.length) > 0) {
                long j = ((long) ABASE) + (((long) ((length - 1) & i)) << ASHIFT);
                ForkJoinTask forkJoinTask = (ForkJoinTask) U.getObjectVolatile(forkJoinTaskArr, j);
                if (forkJoinTask != null) {
                    if (!(forkJoinTask instanceof CountedCompleter)) {
                        return -1;
                    }
                    CountedCompleter<?> countedCompleter2 = (CountedCompleter) forkJoinTask;
                    CountedCompleter<?> countedCompleter3 = countedCompleter2;
                    while (countedCompleter3 != countedCompleter) {
                        countedCompleter3 = countedCompleter3.completer;
                        if (countedCompleter3 == null) {
                            return -1;
                        }
                    }
                    int i3 = i + 1;
                    if (i == this.base && U.compareAndSwapObject(forkJoinTaskArr, j, countedCompleter2, null)) {
                        this.base = i3;
                        countedCompleter2.doExec();
                        return 1;
                    }
                }
                return 2;
            }
            return i | Integer.MIN_VALUE;
        }

        final boolean isApparentlyUnblocked() {
            ForkJoinWorkerThread forkJoinWorkerThread;
            Thread.State state;
            return (this.scanState < 0 || (forkJoinWorkerThread = this.owner) == null || (state = forkJoinWorkerThread.getState()) == Thread.State.BLOCKED || state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) ? false : true;
        }

        static {
            try {
                QLOCK = U.objectFieldOffset(WorkQueue.class.getDeclaredField("qlock"));
                ABASE = U.arrayBaseOffset(ForkJoinTask[].class);
                int iArrayIndexScale = U.arrayIndexScale(ForkJoinTask[].class);
                if (((iArrayIndexScale - 1) & iArrayIndexScale) != 0) {
                    throw new Error("array index scale not a power of two");
                }
                ASHIFT = 31 - Integer.numberOfLeadingZeros(iArrayIndexScale);
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }

    private static final synchronized int nextPoolId() {
        int i;
        i = poolNumberSequence + 1;
        poolNumberSequence = i;
        return i;
    }

    private void tryInitialize(boolean z) {
        int i;
        if (this.runState == 0) {
            int i2 = this.config & SMASK;
            if (i2 > 1) {
                i = i2 - 1;
            } else {
                i = 1;
            }
            int i3 = i | (i >>> 1);
            int i4 = i3 | (i3 >>> 2);
            int i5 = i4 | (i4 >>> 4);
            int i6 = i5 | (i5 >>> 8);
            int i7 = (((i6 | (i6 >>> 16)) + 1) << 1) & SMASK;
            AuxState auxState = new AuxState();
            WorkQueue[] workQueueArr = new WorkQueue[i7];
            synchronized (modifyThreadPermission) {
                if (this.runState == 0) {
                    this.workQueues = workQueueArr;
                    this.auxState = auxState;
                    this.runState = 1;
                }
            }
        }
        if (z && this.runState < 0) {
            tryTerminate(false, false);
            throw new RejectedExecutionException();
        }
    }

    private boolean createWorker(boolean z) {
        ForkJoinWorkerThread forkJoinWorkerThreadNewThread;
        ForkJoinWorkerThreadFactory forkJoinWorkerThreadFactory = this.factory;
        Throwable th = null;
        if (forkJoinWorkerThreadFactory != null) {
            try {
                forkJoinWorkerThreadNewThread = forkJoinWorkerThreadFactory.newThread(this);
                if (forkJoinWorkerThreadNewThread != null) {
                    if (z) {
                        try {
                            WorkQueue workQueue = forkJoinWorkerThreadNewThread.workQueue;
                            if (workQueue != null) {
                                workQueue.config |= 131072;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            th = th;
                            deregisterWorker(forkJoinWorkerThreadNewThread, th);
                            return false;
                        }
                    }
                    forkJoinWorkerThreadNewThread.start();
                    return true;
                }
            } catch (Throwable th3) {
                th = th3;
                forkJoinWorkerThreadNewThread = null;
            }
        } else {
            forkJoinWorkerThreadNewThread = null;
        }
        deregisterWorker(forkJoinWorkerThreadNewThread, th);
        return false;
    }

    private void tryAddWorker(long j) {
        long j2 = j;
        do {
            long j3 = (AC_MASK & (AC_UNIT + j2)) | (TC_MASK & (TC_UNIT + j2));
            if (this.ctl == j2 && U.compareAndSwapLong(this, CTL, j2, j3)) {
                createWorker(false);
                return;
            } else {
                j2 = this.ctl;
                if ((ADD_WORKER & j2) == 0) {
                    return;
                }
            }
        } while (((int) j2) == 0);
    }

    final WorkQueue registerWorker(ForkJoinWorkerThread forkJoinWorkerThread) {
        int length;
        forkJoinWorkerThread.setDaemon(true);
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = this.ueh;
        if (uncaughtExceptionHandler != null) {
            forkJoinWorkerThread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        }
        WorkQueue workQueue = new WorkQueue(this, forkJoinWorkerThread);
        int i = this.config & MODE_MASK;
        AuxState auxState = this.auxState;
        int i2 = 0;
        if (auxState != null) {
            auxState.lock();
            try {
                long j = auxState.indexSeed - 1640531527;
                auxState.indexSeed = j;
                int i3 = (int) j;
                WorkQueue[] workQueueArr = this.workQueues;
                if (workQueueArr != null && (length = workQueueArr.length) > 0) {
                    int i4 = length - 1;
                    int i5 = ((i3 << 1) | 1) & i4;
                    if (workQueueArr[i5] != null) {
                        int i6 = 2;
                        if (length > 4) {
                            i6 = 2 + ((length >>> 1) & EVENMASK);
                        }
                        int i7 = length;
                        loop0: while (true) {
                            int i8 = 0;
                            do {
                                i5 = (i5 + i6) & i4;
                                if (workQueueArr[i5] == null) {
                                    break loop0;
                                }
                                i8++;
                            } while (i8 < i7);
                            i7 <<= 1;
                            workQueueArr = (WorkQueue[]) Arrays.copyOf(workQueueArr, i7);
                            this.workQueues = workQueueArr;
                            i4 = i7 - 1;
                        }
                    }
                    workQueue.hint = i3;
                    workQueue.config = i | i5;
                    workQueue.scanState = (2147418112 & i3) | i5;
                    workQueueArr[i5] = workQueue;
                    i2 = i5;
                }
            } finally {
                auxState.unlock();
            }
        }
        forkJoinWorkerThread.setName(this.workerNamePrefix.concat(Integer.toString(i2 >>> 1)));
        return workQueue;
    }

    final void deregisterWorker(ForkJoinWorkerThread forkJoinWorkerThread, Throwable th) {
        WorkQueue workQueue;
        Unsafe unsafe;
        long j;
        long j2;
        WorkQueue[] workQueueArr;
        int length;
        if (forkJoinWorkerThread == null) {
            workQueue = null;
        } else {
            WorkQueue workQueue2 = forkJoinWorkerThread.workQueue;
            if (workQueue2 != null) {
                int i = workQueue2.config & SMASK;
                int i2 = workQueue2.nsteals;
                AuxState auxState = this.auxState;
                if (auxState != null) {
                    auxState.lock();
                    try {
                        WorkQueue[] workQueueArr2 = this.workQueues;
                        if (workQueueArr2 != null && workQueueArr2.length > i && workQueueArr2[i] == workQueue2) {
                            workQueueArr2[i] = null;
                        }
                        auxState.stealCount += (long) i2;
                    } finally {
                        auxState.unlock();
                    }
                }
            }
            workQueue = workQueue2;
        }
        if (workQueue == null || (workQueue.config & 262144) == 0) {
            do {
                unsafe = U;
                j = CTL;
                j2 = this.ctl;
            } while (!unsafe.compareAndSwapLong(this, j, j2, (AC_MASK & (j2 - AC_UNIT)) | (TC_MASK & (j2 - TC_UNIT)) | (SP_MASK & j2)));
        }
        if (workQueue != null) {
            workQueue.currentSteal = null;
            workQueue.qlock = -1;
            workQueue.cancelAll();
        }
        while (true) {
            if (tryTerminate(false, false) < 0 || workQueue == null || workQueue.array == null || (workQueueArr = this.workQueues) == null || (length = workQueueArr.length) <= 0) {
                break;
            }
            long j3 = this.ctl;
            int i3 = (int) j3;
            if (i3 != 0) {
                if (tryRelease(j3, workQueueArr[(length - 1) & i3], AC_UNIT)) {
                    break;
                }
            } else if (th != null && (ADD_WORKER & j3) != 0) {
                tryAddWorker(j3);
            }
        }
        if (th == null) {
            ForkJoinTask.helpExpungeStaleExceptions();
        } else {
            ForkJoinTask.rethrow(th);
        }
    }

    final void signalWork() {
        WorkQueue workQueue;
        while (true) {
            long j = this.ctl;
            if (j < 0) {
                int i = (int) j;
                if (i == 0) {
                    if ((ADD_WORKER & j) != 0) {
                        tryAddWorker(j);
                        return;
                    }
                    return;
                }
                WorkQueue[] workQueueArr = this.workQueues;
                if (workQueueArr != null) {
                    int length = workQueueArr.length;
                    int i2 = SMASK & i;
                    if (length > i2 && (workQueue = workQueueArr[i2]) != null) {
                        int i3 = i & Integer.MAX_VALUE;
                        int i4 = workQueue.scanState;
                        long j2 = (((long) workQueue.stackPred) & SP_MASK) | (UC_MASK & (AC_UNIT + j));
                        if (i == i4 && U.compareAndSwapLong(this, CTL, j, j2)) {
                            workQueue.scanState = i3;
                            LockSupport.unpark(workQueue.parker);
                            return;
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private boolean tryRelease(long j, WorkQueue workQueue, long j2) {
        int i = (int) j;
        int i2 = i & Integer.MAX_VALUE;
        if (workQueue != null) {
            int i3 = workQueue.scanState;
            long j3 = (UC_MASK & (j + j2)) | (((long) workQueue.stackPred) & SP_MASK);
            if (i == i3 && U.compareAndSwapLong(this, CTL, j, j3)) {
                workQueue.scanState = i2;
                LockSupport.unpark(workQueue.parker);
                return true;
            }
            return false;
        }
        return false;
    }

    private void tryReactivate(WorkQueue workQueue, WorkQueue[] workQueueArr, int i) {
        int length;
        WorkQueue workQueue2;
        long j = this.ctl;
        int i2 = (int) j;
        if (i2 != 0 && workQueue != null && workQueueArr != null && (length = workQueueArr.length) > 0 && ((i ^ i2) & 65536) == 0 && (workQueue2 = workQueueArr[(length - 1) & i2]) != null) {
            long j2 = (UC_MASK & (AC_UNIT + j)) | (((long) workQueue2.stackPred) & SP_MASK);
            int i3 = Integer.MAX_VALUE & i2;
            if (workQueue.scanState < 0 && workQueue2.scanState == i2 && U.compareAndSwapLong(this, CTL, j, j2)) {
                workQueue2.scanState = i3;
                LockSupport.unpark(workQueue2.parker);
            }
        }
    }

    private void inactivate(WorkQueue workQueue, int i) {
        long j;
        long j2;
        int i2 = (i + 65536) | Integer.MIN_VALUE;
        long j3 = ((long) i2) & SP_MASK;
        if (workQueue != null) {
            workQueue.scanState = i2;
            do {
                j = this.ctl;
                j2 = j3 | (UC_MASK & (j - AC_UNIT));
                workQueue.stackPred = (int) j;
            } while (!U.compareAndSwapLong(this, CTL, j, j2));
        }
    }

    private int awaitWork(WorkQueue workQueue) {
        if (workQueue != null && workQueue.scanState < 0) {
            long j = this.ctl;
            if (((int) (j >> 48)) + (this.config & SMASK) <= 0) {
                return timedAwaitWork(workQueue, j);
            }
            if ((this.runState & 2) != 0) {
                workQueue.qlock = -1;
                return -1;
            }
            if (workQueue.scanState < 0) {
                workQueue.parker = Thread.currentThread();
                if (workQueue.scanState < 0) {
                    LockSupport.park(this);
                }
                workQueue.parker = null;
                if ((this.runState & 2) != 0) {
                    workQueue.qlock = -1;
                    return -1;
                }
                if (workQueue.scanState < 0) {
                    Thread.interrupted();
                }
            }
        }
        return 0;
    }

    private int timedAwaitWork(WorkQueue workQueue, long j) {
        int i;
        AuxState auxState;
        WorkQueue[] workQueueArr;
        int i2 = 1 - ((short) (j >>> 32));
        if (i2 <= 0) {
            i2 = 1;
        }
        long jCurrentTimeMillis = (((long) i2) * IDLE_TIMEOUT_MS) + System.currentTimeMillis();
        int i3 = -1;
        if (this.runState < 0) {
            int iTryTerminate = tryTerminate(false, false);
            if (iTryTerminate <= 0) {
                return iTryTerminate;
            }
            i = iTryTerminate;
        } else {
            i = 0;
        }
        if (workQueue != null && workQueue.scanState < 0) {
            workQueue.parker = Thread.currentThread();
            if (workQueue.scanState < 0) {
                LockSupport.parkUntil(this, jCurrentTimeMillis);
            }
            workQueue.parker = null;
            if ((this.runState & 2) != 0) {
                workQueue.qlock = -1;
                return -1;
            }
            int i4 = workQueue.scanState;
            if (i4 < 0 && !Thread.interrupted() && ((int) j) == i4 && (auxState = this.auxState) != null && this.ctl == j && jCurrentTimeMillis - System.currentTimeMillis() <= TIMEOUT_SLOP_MS) {
                auxState.lock();
                try {
                    int i5 = workQueue.config;
                    int i6 = i5 & SMASK;
                    long j2 = (UC_MASK & (j - TC_UNIT)) | (SP_MASK & ((long) workQueue.stackPred));
                    if ((this.runState & 2) == 0 && (workQueueArr = this.workQueues) != null && i6 < workQueueArr.length && i6 >= 0 && workQueueArr[i6] == workQueue && U.compareAndSwapLong(this, CTL, j, j2)) {
                        workQueueArr[i6] = null;
                        workQueue.config = 262144 | i5;
                        workQueue.qlock = -1;
                    } else {
                        i3 = i;
                    }
                    return i3;
                } finally {
                    auxState.unlock();
                }
            }
        }
        return i;
    }

    private boolean tryDropSpare(WorkQueue workQueue) {
        WorkQueue[] workQueueArr;
        int length;
        boolean zCompareAndSwapLong;
        long j;
        boolean z;
        if (workQueue != null && workQueue.isEmpty()) {
            do {
                long j2 = this.ctl;
                if (((short) (j2 >> 32)) <= 0) {
                    return false;
                }
                int i = (int) j2;
                if ((i == 0 && ((int) (j2 >> 48)) <= 0) || (workQueueArr = this.workQueues) == null || (length = workQueueArr.length) <= 0) {
                    return false;
                }
                if (i == 0) {
                    zCompareAndSwapLong = U.compareAndSwapLong(this, CTL, j2, (TC_MASK & (j2 - TC_UNIT)) | (AC_MASK & (j2 - AC_UNIT)) | (SP_MASK & j2));
                } else {
                    WorkQueue workQueue2 = workQueueArr[(length - 1) & i];
                    if (workQueue2 != null && workQueue2.scanState == i) {
                        long j3 = ((long) workQueue2.stackPred) & SP_MASK;
                        if (workQueue != workQueue2 && workQueue.scanState < 0) {
                            j = j3 | (AC_MASK & (AC_UNIT + j2)) | (TC_MASK & j2);
                            z = false;
                        } else {
                            j = j3 | (TC_MASK & (j2 - TC_UNIT)) | (AC_MASK & j2);
                            z = true;
                        }
                        if (U.compareAndSwapLong(this, CTL, j2, j)) {
                            workQueue2.scanState = i & Integer.MAX_VALUE;
                            LockSupport.unpark(workQueue2.parker);
                            zCompareAndSwapLong = z;
                        }
                    } else {
                        zCompareAndSwapLong = false;
                    }
                }
            } while (!zCompareAndSwapLong);
            int i2 = workQueue.config;
            int i3 = SMASK & i2;
            if (i3 >= 0 && i3 < workQueueArr.length && workQueueArr[i3] == workQueue) {
                workQueueArr[i3] = null;
            }
            workQueue.config = i2 | 262144;
            workQueue.qlock = -1;
            return true;
        }
        return false;
    }

    final void runWorker(WorkQueue workQueue) {
        workQueue.growArray();
        int i = (workQueue.config & 131072) != 0 ? 0 : 1023;
        long j = ((long) workQueue.hint) * (-2685821657736338717L);
        if ((this.runState & 2) != 0) {
            return;
        }
        if (j == 0) {
            j = 1;
        }
        while (true) {
            if (i != 0 || !tryDropSpare(workQueue)) {
                int i2 = ((int) (j >>> 48)) | 1;
                long j2 = j ^ (j >>> 12);
                long j3 = j2 ^ (j2 << 25);
                j = j3 ^ (j3 >>> 27);
                if (scan(workQueue, i, i2, (int) j) < 0 && awaitWork(workQueue) < 0) {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private int scan(WorkQueue workQueue, int i, int i2, int i3) {
        int length;
        int i4;
        ForkJoinTask<?>[] forkJoinTaskArr;
        int length2;
        WorkQueue[] workQueueArr = this.workQueues;
        if (workQueueArr != null && workQueue != null && (length = workQueueArr.length) > 0) {
            int i5 = length - 1;
            int i6 = i5 & i3;
            int i7 = workQueue.scanState;
            int i8 = i3;
            int i9 = i6;
            int i10 = 0;
            while (true) {
                WorkQueue workQueue2 = workQueueArr[i9];
                if (workQueue2 != null) {
                    int i11 = workQueue2.base;
                    if (i11 - workQueue2.top < 0 && (forkJoinTaskArr = workQueue2.array) != null && (length2 = forkJoinTaskArr.length) > 0) {
                        i4 = i5;
                        long j = ((long) ABASE) + (((long) ((length2 - 1) & i11)) << ASHIFT);
                        ForkJoinTask<?> forkJoinTask = (ForkJoinTask) U.getObjectVolatile(forkJoinTaskArr, j);
                        if (forkJoinTask != null) {
                            int i12 = i11 + 1;
                            if (i11 != workQueue2.base) {
                                break;
                            }
                            if (i7 < 0) {
                                tryReactivate(workQueue, workQueueArr, i8);
                                break;
                            }
                            if (!U.compareAndSwapObject(forkJoinTaskArr, j, forkJoinTask, null)) {
                                break;
                            }
                            workQueue2.base = i12;
                            workQueue.currentSteal = forkJoinTask;
                            if (i12 != workQueue2.top) {
                                signalWork();
                            }
                            workQueue.runTask(forkJoinTask);
                            i10++;
                            if (i10 > i) {
                                break;
                            }
                            i5 = i4;
                        } else {
                            break;
                        }
                    }
                }
                i4 = i5;
                if (i10 != 0) {
                    break;
                }
                i9 = (i9 + i2) & i4;
                if (i9 != i6) {
                    continue;
                } else if (i7 >= 0) {
                    if (i8 >= 0) {
                        inactivate(workQueue, i7);
                        break;
                    }
                    i8 <<= 1;
                } else {
                    return i7;
                }
                i5 = i4;
            }
        }
        return 0;
    }

    final int helpComplete(WorkQueue workQueue, CountedCompleter<?> countedCompleter, int i) {
        int length;
        int i2;
        WorkQueue workQueue2;
        CountedCompleter<?> countedCompleterPopCC;
        WorkQueue[] workQueueArr = this.workQueues;
        if (workQueueArr != null && (length = workQueueArr.length) > 1 && countedCompleter != null && workQueue != null) {
            int i3 = length - 1;
            int i4 = workQueue.config;
            int i5 = ~i4;
            int i6 = i5 & i3;
            int i7 = i;
            int i8 = i5;
            int i9 = i6;
            int i10 = 0;
            int i11 = 3;
            int i12 = 0;
            int iPollAndExecCC = 1;
            while (true) {
                i2 = countedCompleter.status;
                if (i2 < 0) {
                    break;
                }
                if (iPollAndExecCC == 1 && (countedCompleterPopCC = workQueue.popCC(countedCompleter, i4)) != null) {
                    countedCompleterPopCC.doExec();
                    if (i7 != 0 && i7 - 1 == 0) {
                        break;
                    }
                    i9 = i6;
                    i10 = 0;
                    i12 = 0;
                } else {
                    int i13 = i6 | 1;
                    if (i13 >= 0 && i13 <= i3 && (workQueue2 = workQueueArr[i13]) != null) {
                        iPollAndExecCC = workQueue2.pollAndExecCC(countedCompleter);
                        if (iPollAndExecCC < 0) {
                            i10 += iPollAndExecCC;
                        }
                    } else {
                        iPollAndExecCC = 0;
                    }
                    if (iPollAndExecCC <= 0) {
                        i6 = (i6 + i11) & i3;
                        if (i6 != i9) {
                            continue;
                        } else {
                            if (i12 == i10) {
                                break;
                            }
                            i12 = i10;
                            i10 = 0;
                        }
                    } else {
                        if (iPollAndExecCC == 1 && i7 != 0 && i7 - 1 == 0) {
                            break;
                        }
                        int i14 = (i8 << 13) ^ i8;
                        int i15 = i14 ^ (i14 >>> 17);
                        int i16 = i15 ^ (i15 << 5);
                        i11 = (i8 >>> 16) | 3;
                        i8 = i16;
                        i6 = i16 & i3;
                        i9 = i6;
                        i10 = 0;
                        i12 = 0;
                    }
                }
            }
            return i2;
        }
        return 0;
    }

    private void helpStealer(WorkQueue workQueue, ForkJoinTask<?> forkJoinTask) {
        WorkQueue[] workQueueArr;
        int length;
        int i;
        WorkQueue[] workQueueArr2;
        if (forkJoinTask != null && workQueue != null) {
            ForkJoinTask<?> forkJoinTask2 = workQueue.currentSteal;
            int i2 = 0;
            while (workQueue.tryRemoveAndExec(forkJoinTask) && forkJoinTask.status >= 0 && (workQueueArr = this.workQueues) != null && (length = workQueueArr.length) > 0) {
                int i3 = length - 1;
                WorkQueue workQueue2 = workQueue;
                ForkJoinTask<?> forkJoinTask3 = forkJoinTask;
                int i4 = 0;
                while (forkJoinTask3.status >= 0) {
                    int i5 = workQueue2.hint | 1;
                    int i6 = i4;
                    int i7 = 0;
                    while (true) {
                        int i8 = ((i7 << 1) + i5) & i3;
                        WorkQueue workQueue3 = workQueueArr[i8];
                        if (workQueue3 != null) {
                            if (workQueue3.currentSteal == forkJoinTask3) {
                                break;
                            }
                            i = i2;
                            workQueueArr2 = workQueueArr;
                            i6 += workQueue3.base;
                        } else {
                            i = i2;
                            workQueueArr2 = workQueueArr;
                        }
                        i7++;
                        if (i7 <= i3) {
                            i2 = i;
                            workQueueArr = workQueueArr2;
                        } else {
                            return;
                        }
                    }
                }
                int i9 = i2;
                i2 = i9;
            }
        }
    }

    private boolean tryCompensate(WorkQueue workQueue) {
        int length;
        boolean z;
        WorkQueue workQueue2;
        long j = this.ctl;
        WorkQueue[] workQueueArr = this.workQueues;
        int i = this.config & SMASK;
        int i2 = ((int) (j >> 48)) + i;
        int i3 = ((short) (j >> 32)) + i;
        if (workQueue == null || workQueue.qlock < 0 || i == 0 || workQueueArr == null || (length = workQueueArr.length) <= 0) {
            return false;
        }
        int i4 = length - 1;
        int i5 = 0;
        while (true) {
            if (i5 <= i4) {
                int i6 = (i5 << 1) | 1;
                if (i6 > i4 || i6 < 0 || (workQueue2 = workQueueArr[i6]) == null || workQueue2.scanState < 0 || workQueue2.currentSteal != null) {
                    i5++;
                } else {
                    z = false;
                    break;
                }
            } else {
                z = true;
                break;
            }
        }
        if (!z || this.ctl != j) {
            return false;
        }
        int i7 = (int) j;
        if (i7 == 0) {
            if (i3 >= i && i2 > 1 && workQueue.isEmpty()) {
                return U.compareAndSwapLong(this, CTL, j, ((j - AC_UNIT) & AC_MASK) | (281474976710655L & j));
            }
            if (i3 >= MAX_CAP || (this == common && i3 >= COMMON_MAX_SPARES + i)) {
                throw new RejectedExecutionException("Thread limit exceeded replacing blocked worker");
            }
            boolean z2 = i3 >= i;
            if (!U.compareAndSwapLong(this, CTL, j, (AC_MASK & j) | (TC_MASK & (TC_UNIT + j))) || !createWorker(z2)) {
                return false;
            }
            return true;
        }
        return tryRelease(j, workQueueArr[i4 & i7], 0L);
    }

    final int awaitJoin(WorkQueue workQueue, ForkJoinTask<?> forkJoinTask, long j) {
        ForkJoinPool forkJoinPool;
        int i;
        if (workQueue == null) {
            return 0;
        }
        ForkJoinTask<?> forkJoinTask2 = workQueue.currentJoin;
        if (forkJoinTask == null) {
            return 0;
        }
        int i2 = forkJoinTask.status;
        if (i2 >= 0) {
            workQueue.currentJoin = forkJoinTask;
            CountedCompleter<?> countedCompleter = forkJoinTask instanceof CountedCompleter ? (CountedCompleter) forkJoinTask : null;
            do {
                if (countedCompleter != null) {
                    forkJoinPool = this;
                    forkJoinPool.helpComplete(workQueue, countedCompleter, 0);
                } else {
                    forkJoinPool = this;
                    forkJoinPool.helpStealer(workQueue, forkJoinTask);
                }
                i = forkJoinTask.status;
                if (i < 0) {
                    break;
                }
                long j2 = 0;
                if (j != 0) {
                    long jNanoTime = j - System.nanoTime();
                    if (jNanoTime <= 0) {
                        break;
                    }
                    long millis = TimeUnit.NANOSECONDS.toMillis(jNanoTime);
                    if (millis <= 0) {
                        j2 = 1;
                    } else {
                        j2 = millis;
                    }
                    if (forkJoinPool.tryCompensate(workQueue)) {
                    }
                    i = forkJoinTask.status;
                } else {
                    if (forkJoinPool.tryCompensate(workQueue)) {
                        forkJoinTask.internalWait(j2);
                        U.getAndAddLong(forkJoinPool, CTL, AC_UNIT);
                    }
                    i = forkJoinTask.status;
                }
            } while (i >= 0);
            int i3 = i;
            workQueue.currentJoin = forkJoinTask2;
            return i3;
        }
        return i2;
    }

    private WorkQueue findNonEmptyStealQueue() {
        int length;
        int iNextSecondarySeed = ThreadLocalRandom.nextSecondarySeed();
        WorkQueue[] workQueueArr = this.workQueues;
        if (workQueueArr != null && (length = workQueueArr.length) > 0) {
            int i = length - 1;
            int i2 = iNextSecondarySeed & i;
            int i3 = i2;
            int i4 = 0;
            int i5 = 0;
            while (true) {
                WorkQueue workQueue = workQueueArr[i3];
                if (workQueue != null) {
                    int i6 = workQueue.base;
                    if (i6 - workQueue.top < 0) {
                        return workQueue;
                    }
                    i4 += i6;
                }
                i3 = (i3 + 1) & i;
                if (i3 == i2) {
                    if (i5 != i4) {
                        i5 = i4;
                        i4 = 0;
                    } else {
                        return null;
                    }
                }
            }
        } else {
            return null;
        }
    }

    final void helpQuiescePool(WorkQueue workQueue) {
        ForkJoinTask<?> forkJoinTask = workQueue.currentSteal;
        int i = workQueue.config;
        boolean z = true;
        while (true) {
            if (i >= 0) {
                ForkJoinTask<?> forkJoinTaskPop = workQueue.pop();
                if (forkJoinTaskPop != null) {
                    workQueue.currentSteal = forkJoinTaskPop;
                    forkJoinTaskPop.doExec();
                    workQueue.currentSteal = forkJoinTask;
                }
            }
            WorkQueue workQueueFindNonEmptyStealQueue = findNonEmptyStealQueue();
            if (workQueueFindNonEmptyStealQueue != null) {
                if (!z) {
                    U.getAndAddLong(this, CTL, AC_UNIT);
                    z = true;
                }
                ForkJoinTask<?> forkJoinTaskPollAt = workQueueFindNonEmptyStealQueue.pollAt(workQueueFindNonEmptyStealQueue.base);
                if (forkJoinTaskPollAt != null) {
                    workQueue.currentSteal = forkJoinTaskPollAt;
                    forkJoinTaskPollAt.doExec();
                    workQueue.currentSteal = forkJoinTask;
                    int i2 = workQueue.nsteals + 1;
                    workQueue.nsteals = i2;
                    if (i2 < 0) {
                        workQueue.transferStealCount(this);
                    }
                }
            } else if (z) {
                long j = this.ctl;
                if (U.compareAndSwapLong(this, CTL, j, ((j - AC_UNIT) & AC_MASK) | (281474976710655L & j))) {
                    z = false;
                }
            } else {
                long j2 = this.ctl;
                if (((int) (j2 >> 48)) + (this.config & SMASK) <= 0 && U.compareAndSwapLong(this, CTL, j2, j2 + AC_UNIT)) {
                    return;
                }
            }
        }
    }

    final ForkJoinTask<?> nextTaskFor(WorkQueue workQueue) {
        ForkJoinTask<?> forkJoinTaskPollAt;
        do {
            ForkJoinTask<?> forkJoinTaskNextLocalTask = workQueue.nextLocalTask();
            if (forkJoinTaskNextLocalTask != null) {
                return forkJoinTaskNextLocalTask;
            }
            WorkQueue workQueueFindNonEmptyStealQueue = findNonEmptyStealQueue();
            if (workQueueFindNonEmptyStealQueue == null) {
                return null;
            }
            forkJoinTaskPollAt = workQueueFindNonEmptyStealQueue.pollAt(workQueueFindNonEmptyStealQueue.base);
        } while (forkJoinTaskPollAt == null);
        return forkJoinTaskPollAt;
    }

    static int getSurplusQueuedTaskCount() {
        Thread threadCurrentThread = Thread.currentThread();
        int i = 0;
        if (!(threadCurrentThread instanceof ForkJoinWorkerThread)) {
            return 0;
        }
        ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) threadCurrentThread;
        ForkJoinPool forkJoinPool = forkJoinWorkerThread.pool;
        int i2 = forkJoinPool.config & SMASK;
        WorkQueue workQueue = forkJoinWorkerThread.workQueue;
        int i3 = workQueue.top - workQueue.base;
        int i4 = ((int) (forkJoinPool.ctl >> 48)) + i2;
        int i5 = i2 >>> 1;
        if (i4 <= i5) {
            int i6 = i5 >>> 1;
            if (i4 <= i6) {
                int i7 = i6 >>> 1;
                if (i4 > i7) {
                    i = 2;
                } else {
                    i = i4 > (i7 >>> 1) ? 4 : 8;
                }
            } else {
                i = 1;
            }
        }
        return i3 - i;
    }

    private int tryTerminate(boolean z, boolean z2) {
        Unsafe unsafe;
        long j;
        int i;
        while (true) {
            int i2 = this.runState;
            if (i2 >= 0) {
                if (!z2 || this == common) {
                    return 1;
                }
                if (i2 == 0) {
                    tryInitialize(false);
                } else {
                    U.compareAndSwapInt(this, RUNSTATE, i2, i2 | Integer.MIN_VALUE);
                }
            } else {
                long j2 = 0;
                if ((i2 & 2) == 0) {
                    if (!z) {
                        long j3 = 0;
                        loop1: while (true) {
                            long j4 = this.ctl;
                            if (((int) (j4 >> 48)) + (this.config & SMASK) > 0) {
                                return 0;
                            }
                            WorkQueue[] workQueueArr = this.workQueues;
                            if (workQueueArr != null) {
                                long j5 = j4;
                                for (WorkQueue workQueue : workQueueArr) {
                                    if (workQueue != null) {
                                        int i3 = workQueue.base;
                                        j5 += (long) i3;
                                        if (workQueue.currentSteal != null || i3 != workQueue.top) {
                                            break loop1;
                                        }
                                    }
                                }
                                j4 = j5;
                            }
                            if (j3 == j4) {
                                break;
                            }
                            j3 = j4;
                        }
                        return 0;
                    }
                    do {
                        unsafe = U;
                        j = RUNSTATE;
                        i = this.runState;
                    } while (!unsafe.compareAndSwapInt(this, j, i, i | 2));
                }
                while (true) {
                    long j6 = this.ctl;
                    WorkQueue[] workQueueArr2 = this.workQueues;
                    if (workQueueArr2 != null) {
                        long j7 = j6;
                        for (WorkQueue workQueue2 : workQueueArr2) {
                            if (workQueue2 != null) {
                                workQueue2.cancelAll();
                                j7 += (long) workQueue2.base;
                                if (workQueue2.qlock >= 0) {
                                    workQueue2.qlock = -1;
                                    ForkJoinWorkerThread forkJoinWorkerThread = workQueue2.owner;
                                    if (forkJoinWorkerThread != null) {
                                        try {
                                            forkJoinWorkerThread.interrupt();
                                        } catch (Throwable th) {
                                        }
                                    }
                                }
                            }
                        }
                        j6 = j7;
                    }
                    if (j2 == j6) {
                        break;
                    }
                    j2 = j6;
                }
                if (((short) (this.ctl >>> 32)) + (this.config & SMASK) <= 0) {
                    this.runState = -2147483641;
                    synchronized (this) {
                        notifyAll();
                    }
                }
                return -1;
            }
        }
    }

    private void tryCreateExternalQueue(int i) {
        AuxState auxState = this.auxState;
        if (auxState != null && i >= 0) {
            WorkQueue workQueue = new WorkQueue(this, null);
            workQueue.config = i;
            workQueue.scanState = Integer.MAX_VALUE;
            boolean z = true;
            workQueue.qlock = 1;
            auxState.lock();
            try {
                WorkQueue[] workQueueArr = this.workQueues;
                if (workQueueArr != null && i < workQueueArr.length && workQueueArr[i] == null) {
                    workQueueArr[i] = workQueue;
                } else {
                    z = false;
                }
                if (z) {
                    try {
                        workQueue.growArray();
                    } finally {
                        workQueue.qlock = 0;
                    }
                }
            } finally {
                auxState.unlock();
            }
        }
    }

    final void externalPush(ForkJoinTask<?> forkJoinTask) {
        int length;
        int probe = ThreadLocalRandom.getProbe();
        if (probe == 0) {
            ThreadLocalRandom.localInit();
            probe = ThreadLocalRandom.getProbe();
        }
        while (true) {
            int i = this.runState;
            WorkQueue[] workQueueArr = this.workQueues;
            if (i <= 0 || workQueueArr == null || (length = workQueueArr.length) <= 0) {
                tryInitialize(true);
            } else {
                int i2 = (length - 1) & probe & SQMASK;
                WorkQueue workQueue = workQueueArr[i2];
                if (workQueue == null) {
                    tryCreateExternalQueue(i2);
                } else {
                    int iSharedPush = workQueue.sharedPush(forkJoinTask);
                    if (iSharedPush >= 0) {
                        if (iSharedPush == 0) {
                            signalWork();
                            return;
                        }
                        probe = ThreadLocalRandom.advanceProbe(probe);
                    } else {
                        return;
                    }
                }
            }
        }
    }

    private <T> ForkJoinTask<T> externalSubmit(ForkJoinTask<T> forkJoinTask) {
        WorkQueue workQueue;
        if (forkJoinTask == null) {
            throw new NullPointerException();
        }
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) threadCurrentThread;
            if (forkJoinWorkerThread.pool == this && (workQueue = forkJoinWorkerThread.workQueue) != null) {
                workQueue.push(forkJoinTask);
            } else {
                externalPush(forkJoinTask);
            }
        }
        return forkJoinTask;
    }

    static WorkQueue commonSubmitterQueue() {
        WorkQueue[] workQueueArr;
        int length;
        ForkJoinPool forkJoinPool = common;
        int probe = ThreadLocalRandom.getProbe();
        if (forkJoinPool == null || (workQueueArr = forkJoinPool.workQueues) == null || (length = workQueueArr.length) <= 0) {
            return null;
        }
        return workQueueArr[probe & (length - 1) & SQMASK];
    }

    final boolean tryExternalUnpush(ForkJoinTask<?> forkJoinTask) {
        int length;
        WorkQueue workQueue;
        int probe = ThreadLocalRandom.getProbe();
        WorkQueue[] workQueueArr = this.workQueues;
        return workQueueArr != null && (length = workQueueArr.length) > 0 && (workQueue = workQueueArr[(probe & (length - 1)) & SQMASK]) != null && workQueue.trySharedUnpush(forkJoinTask);
    }

    final int externalHelpComplete(CountedCompleter<?> countedCompleter, int i) {
        int length;
        int probe = ThreadLocalRandom.getProbe();
        WorkQueue[] workQueueArr = this.workQueues;
        if (workQueueArr == null || (length = workQueueArr.length) <= 0) {
            return 0;
        }
        return helpComplete(workQueueArr[probe & (length - 1) & SQMASK], countedCompleter, i);
    }

    public ForkJoinPool() {
        this(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()), defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int i) {
        this(i, defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int i, ForkJoinWorkerThreadFactory forkJoinWorkerThreadFactory, Thread.UncaughtExceptionHandler uncaughtExceptionHandler, boolean z) {
        this(checkParallelism(i), checkFactory(forkJoinWorkerThreadFactory), uncaughtExceptionHandler, z ? Integer.MIN_VALUE : 0, "ForkJoinPool-" + nextPoolId() + "-worker-");
        checkPermission();
    }

    private static int checkParallelism(int i) {
        if (i <= 0 || i > MAX_CAP) {
            throw new IllegalArgumentException();
        }
        return i;
    }

    private static ForkJoinWorkerThreadFactory checkFactory(ForkJoinWorkerThreadFactory forkJoinWorkerThreadFactory) {
        if (forkJoinWorkerThreadFactory == null) {
            throw new NullPointerException();
        }
        return forkJoinWorkerThreadFactory;
    }

    private ForkJoinPool(int i, ForkJoinWorkerThreadFactory forkJoinWorkerThreadFactory, Thread.UncaughtExceptionHandler uncaughtExceptionHandler, int i2, String str) {
        this.workerNamePrefix = str;
        this.factory = forkJoinWorkerThreadFactory;
        this.ueh = uncaughtExceptionHandler;
        this.config = (SMASK & i) | i2;
        long j = -i;
        this.ctl = ((j << 32) & TC_MASK) | ((j << 48) & AC_MASK);
    }

    public static ForkJoinPool commonPool() {
        return common;
    }

    public <T> T invoke(ForkJoinTask<T> forkJoinTask) {
        if (forkJoinTask == null) {
            throw new NullPointerException();
        }
        externalSubmit(forkJoinTask);
        return forkJoinTask.join();
    }

    public void execute(ForkJoinTask<?> forkJoinTask) {
        externalSubmit(forkJoinTask);
    }

    @Override
    public void execute(Runnable runnable) {
        ForkJoinTask runnableExecuteAction;
        if (runnable == 0) {
            throw new NullPointerException();
        }
        if (runnable instanceof ForkJoinTask) {
            runnableExecuteAction = (ForkJoinTask) runnable;
        } else {
            runnableExecuteAction = new ForkJoinTask.RunnableExecuteAction(runnable);
        }
        externalSubmit(runnableExecuteAction);
    }

    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> forkJoinTask) {
        return externalSubmit(forkJoinTask);
    }

    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> callable) {
        return externalSubmit(new ForkJoinTask.AdaptedCallable(callable));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Runnable runnable, T t) {
        return externalSubmit(new ForkJoinTask.AdaptedRunnable(runnable, t));
    }

    @Override
    public ForkJoinTask<?> submit(Runnable runnable) {
        ForkJoinTask adaptedRunnableAction;
        if (runnable == 0) {
            throw new NullPointerException();
        }
        if (runnable instanceof ForkJoinTask) {
            adaptedRunnableAction = (ForkJoinTask) runnable;
        } else {
            adaptedRunnableAction = new ForkJoinTask.AdaptedRunnableAction(runnable);
        }
        return externalSubmit(adaptedRunnableAction);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) {
        ArrayList arrayList = new ArrayList(collection.size());
        try {
            Iterator<? extends Callable<T>> it = collection.iterator();
            while (it.hasNext()) {
                ForkJoinTask.AdaptedCallable adaptedCallable = new ForkJoinTask.AdaptedCallable(it.next());
                arrayList.add(adaptedCallable);
                externalSubmit(adaptedCallable);
            }
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                ((ForkJoinTask) arrayList.get(i)).quietlyJoin();
            }
            return arrayList;
        } catch (Throwable th) {
            int size2 = arrayList.size();
            for (int i2 = 0; i2 < size2; i2++) {
                ((Future) arrayList.get(i2)).cancel(false);
            }
            throw th;
        }
    }

    public ForkJoinWorkerThreadFactory getFactory() {
        return this.factory;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return this.ueh;
    }

    public int getParallelism() {
        int i = this.config & SMASK;
        if (i > 0) {
            return i;
        }
        return 1;
    }

    public static int getCommonPoolParallelism() {
        return COMMON_PARALLELISM;
    }

    public int getPoolSize() {
        return (this.config & SMASK) + ((short) (this.ctl >>> 32));
    }

    public boolean getAsyncMode() {
        return (this.config & Integer.MIN_VALUE) != 0;
    }

    public int getRunningThreadCount() {
        WorkQueue[] workQueueArr = this.workQueues;
        int i = 0;
        if (workQueueArr != null) {
            for (int i2 = 1; i2 < workQueueArr.length; i2 += 2) {
                WorkQueue workQueue = workQueueArr[i2];
                if (workQueue != null && workQueue.isApparentlyUnblocked()) {
                    i++;
                }
            }
        }
        return i;
    }

    public int getActiveThreadCount() {
        int i = (this.config & SMASK) + ((int) (this.ctl >> 48));
        if (i <= 0) {
            return 0;
        }
        return i;
    }

    public boolean isQuiescent() {
        return (this.config & SMASK) + ((int) (this.ctl >> 48)) <= 0;
    }

    public long getStealCount() {
        AuxState auxState = this.auxState;
        long j = auxState == null ? 0L : auxState.stealCount;
        WorkQueue[] workQueueArr = this.workQueues;
        if (workQueueArr != null) {
            for (int i = 1; i < workQueueArr.length; i += 2) {
                WorkQueue workQueue = workQueueArr[i];
                if (workQueue != null) {
                    j += (long) workQueue.nsteals;
                }
            }
        }
        return j;
    }

    public long getQueuedTaskCount() {
        WorkQueue[] workQueueArr = this.workQueues;
        long jQueueSize = 0;
        if (workQueueArr != null) {
            for (int i = 1; i < workQueueArr.length; i += 2) {
                WorkQueue workQueue = workQueueArr[i];
                if (workQueue != null) {
                    jQueueSize += (long) workQueue.queueSize();
                }
            }
        }
        return jQueueSize;
    }

    public int getQueuedSubmissionCount() {
        WorkQueue[] workQueueArr = this.workQueues;
        if (workQueueArr == null) {
            return 0;
        }
        int iQueueSize = 0;
        for (int i = 0; i < workQueueArr.length; i += 2) {
            WorkQueue workQueue = workQueueArr[i];
            if (workQueue != null) {
                iQueueSize += workQueue.queueSize();
            }
        }
        return iQueueSize;
    }

    public boolean hasQueuedSubmissions() {
        WorkQueue[] workQueueArr = this.workQueues;
        if (workQueueArr != null) {
            for (int i = 0; i < workQueueArr.length; i += 2) {
                WorkQueue workQueue = workQueueArr[i];
                if (workQueue != null && !workQueue.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected ForkJoinTask<?> pollSubmission() {
        int length;
        ForkJoinTask<?> forkJoinTaskPoll;
        ThreadLocalRandom.nextSecondarySeed();
        WorkQueue[] workQueueArr = this.workQueues;
        if (workQueueArr != null && (length = workQueueArr.length) > 0) {
            int i = length - 1;
            for (int i2 = 0; i2 < length; i2++) {
                WorkQueue workQueue = workQueueArr[(i2 << 1) & i];
                if (workQueue != null && (forkJoinTaskPoll = workQueue.poll()) != null) {
                    return forkJoinTaskPoll;
                }
            }
            return null;
        }
        return null;
    }

    protected int drainTasksTo(Collection<? super ForkJoinTask<?>> collection) {
        WorkQueue[] workQueueArr = this.workQueues;
        if (workQueueArr == null) {
            return 0;
        }
        int i = 0;
        for (WorkQueue workQueue : workQueueArr) {
            if (workQueue != null) {
                while (true) {
                    ForkJoinTask<?> forkJoinTaskPoll = workQueue.poll();
                    if (forkJoinTaskPoll != null) {
                        collection.add(forkJoinTaskPoll);
                        i++;
                    }
                }
            }
        }
        return i;
    }

    public String toString() {
        long j;
        long j2;
        long j3;
        int i;
        String str;
        AuxState auxState = this.auxState;
        long j4 = 0;
        if (auxState != null) {
            j = auxState.stealCount;
        } else {
            j = 0;
        }
        long j5 = this.ctl;
        WorkQueue[] workQueueArr = this.workQueues;
        if (workQueueArr == null) {
            j2 = 0;
            j3 = j;
            i = 0;
        } else {
            long j6 = 0;
            j3 = j;
            i = 0;
            long j7 = 0;
            for (int i2 = 0; i2 < workQueueArr.length; i2++) {
                WorkQueue workQueue = workQueueArr[i2];
                if (workQueue != null) {
                    int iQueueSize = workQueue.queueSize();
                    if ((i2 & 1) == 0) {
                        j7 += (long) iQueueSize;
                    } else {
                        j6 += (long) iQueueSize;
                        j3 += (long) workQueue.nsteals;
                        if (workQueue.isApparentlyUnblocked()) {
                            i++;
                        }
                    }
                }
            }
            j4 = j6;
            j2 = j7;
        }
        int i3 = this.config & SMASK;
        int i4 = ((short) (j5 >>> 32)) + i3;
        int i5 = ((int) (j5 >> 48)) + i3;
        if (i5 < 0) {
            i5 = 0;
        }
        int i6 = this.runState;
        if ((i6 & 4) != 0) {
            str = "Terminated";
        } else if ((i6 & 2) != 0) {
            str = "Terminating";
        } else {
            str = (i6 & Integer.MIN_VALUE) != 0 ? "Shutting down" : "Running";
        }
        return super.toString() + "[" + str + ", parallelism = " + i3 + ", size = " + i4 + ", active = " + i5 + ", running = " + i + ", steals = " + j3 + ", tasks = " + j4 + ", submissions = " + j2 + "]";
    }

    @Override
    public void shutdown() {
        checkPermission();
        tryTerminate(false, true);
    }

    @Override
    public List<Runnable> shutdownNow() {
        checkPermission();
        tryTerminate(true, true);
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminated() {
        return (this.runState & 4) != 0;
    }

    public boolean isTerminating() {
        int i = this.runState;
        return (i & 2) != 0 && (i & 4) == 0;
    }

    @Override
    public boolean isShutdown() {
        return (this.runState & Integer.MIN_VALUE) != 0;
    }

    @Override
    public boolean awaitTermination(long j, TimeUnit timeUnit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (this == common) {
            awaitQuiescence(j, timeUnit);
            return false;
        }
        long nanos = timeUnit.toNanos(j);
        if (isTerminated()) {
            return true;
        }
        if (nanos <= 0) {
            return false;
        }
        long jNanoTime = System.nanoTime() + nanos;
        synchronized (this) {
            while (!isTerminated()) {
                if (nanos <= 0) {
                    return false;
                }
                long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
                if (millis <= 0) {
                    millis = 1;
                }
                wait(millis);
                nanos = jNanoTime - System.nanoTime();
            }
            return true;
        }
    }

    public boolean awaitQuiescence(long j, TimeUnit timeUnit) {
        WorkQueue[] workQueueArr;
        int length;
        WorkQueue workQueue;
        long nanos = timeUnit.toNanos(j);
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) threadCurrentThread;
            if (forkJoinWorkerThread.pool == this) {
                helpQuiescePool(forkJoinWorkerThread.workQueue);
                return true;
            }
        }
        long jNanoTime = System.nanoTime();
        int i = 0;
        boolean z = true;
        while (!isQuiescent() && (workQueueArr = this.workQueues) != null && (length = workQueueArr.length) > 0) {
            if (!z) {
                if (System.nanoTime() - jNanoTime > nanos) {
                    return false;
                }
                Thread.yield();
            }
            int i2 = length - 1;
            int i3 = (i2 + 1) << 2;
            while (true) {
                if (i3 < 0) {
                    z = false;
                    break;
                }
                int i4 = i + 1;
                int i5 = i & i2;
                if (i5 <= i2 && i5 >= 0 && (workQueue = workQueueArr[i5]) != null) {
                    int i6 = workQueue.base;
                    if (i6 - workQueue.top < 0) {
                        ForkJoinTask<?> forkJoinTaskPollAt = workQueue.pollAt(i6);
                        if (forkJoinTaskPollAt != null) {
                            forkJoinTaskPollAt.doExec();
                        }
                        z = true;
                        i = i4;
                    }
                }
                i3--;
                i = i4;
            }
        }
        return true;
    }

    static void quiesceCommonPool() {
        common.awaitQuiescence(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    public static void managedBlock(ManagedBlocker managedBlocker) throws InterruptedException {
        ForkJoinWorkerThread forkJoinWorkerThread;
        ForkJoinPool forkJoinPool;
        Thread threadCurrentThread = Thread.currentThread();
        if ((threadCurrentThread instanceof ForkJoinWorkerThread) && (forkJoinPool = (forkJoinWorkerThread = (ForkJoinWorkerThread) threadCurrentThread).pool) != null) {
            WorkQueue workQueue = forkJoinWorkerThread.workQueue;
            while (!managedBlocker.isReleasable()) {
                if (forkJoinPool.tryCompensate(workQueue)) {
                    while (!managedBlocker.isReleasable() && !managedBlocker.block()) {
                        try {
                        } finally {
                            U.getAndAddLong(forkJoinPool, CTL, AC_UNIT);
                        }
                    }
                    return;
                }
            }
            return;
        }
        while (!managedBlocker.isReleasable() && !managedBlocker.block()) {
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T t) {
        return new ForkJoinTask.AdaptedRunnable(runnable, t);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ForkJoinTask.AdaptedCallable(callable);
    }

    static {
        try {
            CTL = U.objectFieldOffset(ForkJoinPool.class.getDeclaredField("ctl"));
            RUNSTATE = U.objectFieldOffset(ForkJoinPool.class.getDeclaredField("runState"));
            ABASE = U.arrayBaseOffset(ForkJoinTask[].class);
            int iArrayIndexScale = U.arrayIndexScale(ForkJoinTask[].class);
            if (((iArrayIndexScale - 1) & iArrayIndexScale) != 0) {
                throw new Error("array index scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(iArrayIndexScale);
            int i = 256;
            try {
                String property = System.getProperty("java.util.concurrent.ForkJoinPool.common.maximumSpares");
                if (property != null) {
                    i = Integer.parseInt(property);
                }
            } catch (Exception e) {
            }
            COMMON_MAX_SPARES = i;
            defaultForkJoinWorkerThreadFactory = new DefaultForkJoinWorkerThreadFactory();
            modifyThreadPermission = new RuntimePermission("modifyThread");
            common = (ForkJoinPool) AccessController.doPrivileged(new PrivilegedAction<ForkJoinPool>() {
                @Override
                public ForkJoinPool run() {
                    return ForkJoinPool.makeCommonPool();
                }
            });
            COMMON_PARALLELISM = Math.max(common.config & SMASK, 1);
        } catch (ReflectiveOperationException e2) {
            throw new Error(e2);
        }
    }

    static ForkJoinPool makeCommonPool() {
        ForkJoinWorkerThreadFactory innocuousForkJoinWorkerThreadFactory;
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler2;
        try {
            String property = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
            String property2 = System.getProperty("java.util.concurrent.ForkJoinPool.common.threadFactory");
            String property3 = System.getProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler");
            iAvailableProcessors = property != null ? Integer.parseInt(property) : -1;
            if (property2 != null) {
                innocuousForkJoinWorkerThreadFactory = (ForkJoinWorkerThreadFactory) ClassLoader.getSystemClassLoader().loadClass(property2).newInstance();
            } else {
                innocuousForkJoinWorkerThreadFactory = null;
            }
            if (property3 != null) {
                try {
                    uncaughtExceptionHandler2 = (Thread.UncaughtExceptionHandler) ClassLoader.getSystemClassLoader().loadClass(property3).newInstance();
                } catch (Exception e) {
                    uncaughtExceptionHandler = null;
                }
            } else {
                uncaughtExceptionHandler2 = null;
            }
            uncaughtExceptionHandler = uncaughtExceptionHandler2;
        } catch (Exception e2) {
            innocuousForkJoinWorkerThreadFactory = null;
        }
        if (innocuousForkJoinWorkerThreadFactory == null) {
            if (System.getSecurityManager() == null) {
                innocuousForkJoinWorkerThreadFactory = defaultForkJoinWorkerThreadFactory;
            } else {
                innocuousForkJoinWorkerThreadFactory = new InnocuousForkJoinWorkerThreadFactory();
            }
        }
        ForkJoinWorkerThreadFactory forkJoinWorkerThreadFactory = innocuousForkJoinWorkerThreadFactory;
        if (iAvailableProcessors < 0 && (iAvailableProcessors = Runtime.getRuntime().availableProcessors() - 1) <= 0) {
            iAvailableProcessors = 1;
        }
        return new ForkJoinPool(iAvailableProcessors > MAX_CAP ? MAX_CAP : iAvailableProcessors, forkJoinWorkerThreadFactory, uncaughtExceptionHandler, 0, "ForkJoinPool.commonPool-worker-");
    }

    private static final class InnocuousForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        private static final AccessControlContext innocuousAcc;

        private InnocuousForkJoinWorkerThreadFactory() {
        }

        static {
            Permissions permissions = new Permissions();
            permissions.add(ForkJoinPool.modifyThreadPermission);
            permissions.add(new RuntimePermission("enableContextClassLoaderOverride"));
            permissions.add(new RuntimePermission("modifyThreadGroup"));
            innocuousAcc = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, permissions)});
        }

        @Override
        public final ForkJoinWorkerThread newThread(final ForkJoinPool forkJoinPool) {
            return (ForkJoinWorkerThread) AccessController.doPrivileged(new PrivilegedAction<ForkJoinWorkerThread>() {
                @Override
                public ForkJoinWorkerThread run() {
                    return new ForkJoinWorkerThread.InnocuousForkJoinWorkerThread(forkJoinPool);
                }
            }, innocuousAcc);
        }
    }
}
