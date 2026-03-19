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
        SecurityManager security = System.getSecurityManager();
        if (security == null) {
            return;
        }
        security.checkPermission(modifyThreadPermission);
    }

    private static final class DefaultForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        DefaultForkJoinWorkerThreadFactory(DefaultForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory) {
            this();
        }

        private DefaultForkJoinWorkerThreadFactory() {
        }

        @Override
        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return new ForkJoinWorkerThread(pool);
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
        public final void setRawResult(Void x) {
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

        WorkQueue(ForkJoinPool pool, ForkJoinWorkerThread owner) {
            this.pool = pool;
            this.owner = owner;
        }

        final int getPoolIndex() {
            return (this.config & 65535) >>> 1;
        }

        final int queueSize() {
            int n = this.base - this.top;
            if (n >= 0) {
                return 0;
            }
            return -n;
        }

        final boolean isEmpty() {
            int al;
            int i = this.base;
            int s = this.top;
            int n = i - s;
            if (n >= 0) {
                return true;
            }
            if (n != -1) {
                return false;
            }
            ForkJoinTask<?>[] a = this.array;
            return a == null || (al = a.length) == 0 || a[(al + (-1)) & (s + (-1))] == null;
        }

        final void push(ForkJoinTask<?> task) {
            int al;
            U.storeFence();
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (a == null || (al = a.length) <= 0) {
                return;
            }
            a[(al - 1) & s] = task;
            this.top = s + 1;
            ForkJoinPool p = this.pool;
            int d = this.base - s;
            if (d == 0 && p != null) {
                U.fullFence();
                p.signalWork();
            } else {
                if (al + d != 1) {
                    return;
                }
                growArray();
            }
        }

        final ForkJoinTask<?>[] growArray() {
            int oldMask;
            ForkJoinTask<?>[] oldA = this.array;
            int size = oldA != null ? oldA.length << 1 : 8192;
            if (size < 8192 || size > 67108864) {
                throw new RejectedExecutionException("Queue capacity exceeded");
            }
            ForkJoinTask<?>[] a = new ForkJoinTask[size];
            this.array = a;
            if (oldA != null && oldA.length - 1 > 0) {
                int t = this.top;
                int b = this.base;
                if (t - b > 0) {
                    int mask = size - 1;
                    do {
                        int index = b & oldMask;
                        long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                        ForkJoinTask<?> x = (ForkJoinTask) U.getObjectVolatile(oldA, offset);
                        if (x != null && U.compareAndSwapObject(oldA, offset, x, (Object) null)) {
                            a[b & mask] = x;
                        }
                        b++;
                    } while (b != t);
                    U.storeFence();
                }
            }
            return a;
        }

        final ForkJoinTask<?> pop() {
            int al;
            int b = this.base;
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (a != null && b != s && (al = a.length) > 0) {
                int s2 = s - 1;
                int index = (al - 1) & s2;
                long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                ForkJoinTask<?> t = (ForkJoinTask) U.getObject(a, offset);
                if (t != null && U.compareAndSwapObject(a, offset, t, (Object) null)) {
                    this.top = s2;
                    return t;
                }
            }
            return null;
        }

        final ForkJoinTask<?> pollAt(int b) {
            int al;
            ForkJoinTask<?>[] a = this.array;
            if (a != null && (al = a.length) > 0) {
                int index = (al - 1) & b;
                long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                if (t != null) {
                    int b2 = b + 1;
                    if (b == this.base && U.compareAndSwapObject(a, offset, t, (Object) null)) {
                        this.base = b2;
                        return t;
                    }
                }
            }
            return null;
        }

        final ForkJoinTask<?> poll() {
            int d;
            int al;
            while (true) {
                int b = this.base;
                int s = this.top;
                ForkJoinTask<?>[] a = this.array;
                if (a != null && (d = b - s) < 0 && (al = a.length) > 0) {
                    int index = (al - 1) & b;
                    long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                    ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                    int b2 = b + 1;
                    if (b == this.base) {
                        if (t != null) {
                            if (U.compareAndSwapObject(a, offset, t, (Object) null)) {
                                this.base = b2;
                                return t;
                            }
                        } else if (d == -1) {
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
            int al;
            ForkJoinTask<?>[] a = this.array;
            if (a == null || (al = a.length) <= 0) {
                return null;
            }
            return a[(this.config < 0 ? this.base : this.top - 1) & (al - 1)];
        }

        final boolean tryUnpush(ForkJoinTask<?> task) {
            int al;
            int b = this.base;
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (a != null && b != s && (al = a.length) > 0) {
                int s2 = s - 1;
                int index = (al - 1) & s2;
                long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                if (U.compareAndSwapObject(a, offset, task, (Object) null)) {
                    this.top = s2;
                    return true;
                }
                return false;
            }
            return false;
        }

        final int sharedPush(ForkJoinTask<?> task) {
            int al;
            if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                int b = this.base;
                int s = this.top;
                ForkJoinTask<?>[] a = this.array;
                if (a != null && (al = a.length) > 0) {
                    int d = b - s;
                    if ((al - 1) + d > 0) {
                        a[(al - 1) & s] = task;
                        this.top = s + 1;
                        this.qlock = 0;
                        if (d >= 0 || b != this.base) {
                            return 0;
                        }
                        return d;
                    }
                }
                growAndSharedPush(task);
                return 0;
            }
            return 1;
        }

        private void growAndSharedPush(ForkJoinTask<?> task) {
            int al;
            try {
                growArray();
                int s = this.top;
                ForkJoinTask<?>[] a = this.array;
                if (a != null && (al = a.length) > 0) {
                    a[(al - 1) & s] = task;
                    this.top = s + 1;
                }
            } finally {
                this.qlock = 0;
            }
        }

        final boolean trySharedUnpush(ForkJoinTask<?> task) {
            int al;
            boolean popped = false;
            int s = this.top - 1;
            ForkJoinTask<?>[] a = this.array;
            if (a != null && (al = a.length) > 0) {
                int index = (al - 1) & s;
                long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                ForkJoinTask<?> t = (ForkJoinTask) U.getObject(a, offset);
                if (t == task && U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                    if (this.top == s + 1 && this.array == a && U.compareAndSwapObject(a, offset, task, (Object) null)) {
                        popped = true;
                        this.top = s;
                    }
                    U.putOrderedInt(this, QLOCK, 0);
                }
            }
            return popped;
        }

        final void cancelAll() {
            ForkJoinTask<?> t = this.currentJoin;
            if (t != null) {
                this.currentJoin = null;
                ForkJoinTask.cancelIgnoringExceptions(t);
            }
            ForkJoinTask<?> t2 = this.currentSteal;
            if (t2 != null) {
                this.currentSteal = null;
                ForkJoinTask.cancelIgnoringExceptions(t2);
            }
            while (true) {
                ForkJoinTask<?> t3 = poll();
                if (t3 == null) {
                    return;
                } else {
                    ForkJoinTask.cancelIgnoringExceptions(t3);
                }
            }
        }

        final void localPopAndExec() {
            int al;
            int nexec = 0;
            do {
                int b = this.base;
                int s = this.top;
                ForkJoinTask<?>[] a = this.array;
                if (a == null || b == s || (al = a.length) <= 0) {
                    return;
                }
                int s2 = s - 1;
                int index = (al - 1) & s2;
                long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                ForkJoinTask<?> t = (ForkJoinTask) U.getAndSetObject(a, offset, (Object) null);
                if (t == null) {
                    return;
                }
                this.top = s2;
                this.currentSteal = t;
                t.doExec();
                nexec++;
            } while (nexec <= 1023);
        }

        final void localPollAndExec() {
            int al;
            int nexec = 0;
            while (true) {
                int b = this.base;
                int s = this.top;
                ForkJoinTask<?>[] a = this.array;
                if (a == null || b == s || (al = a.length) <= 0) {
                    return;
                }
                int b2 = b + 1;
                int index = (al - 1) & b;
                long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                ForkJoinTask<?> t = (ForkJoinTask) U.getAndSetObject(a, offset, (Object) null);
                if (t != null) {
                    this.base = b2;
                    t.doExec();
                    nexec++;
                    if (nexec > 1023) {
                        return;
                    }
                }
            }
        }

        final void runTask(ForkJoinTask<?> task) {
            if (task == null) {
                return;
            }
            task.doExec();
            if (this.config < 0) {
                localPollAndExec();
            } else {
                localPopAndExec();
            }
            int ns = this.nsteals + 1;
            this.nsteals = ns;
            ForkJoinWorkerThread thread = this.owner;
            this.currentSteal = null;
            if (ns < 0) {
                transferStealCount(this.pool);
            }
            if (thread == null) {
                return;
            }
            thread.afterTopLevelExec();
        }

        final void transferStealCount(ForkJoinPool p) {
            AuxState aux;
            if (p == null || (aux = p.auxState) == null) {
                return;
            }
            long s = this.nsteals;
            this.nsteals = 0;
            if (s < 0) {
                s = 2147483647L;
            }
            aux.lock();
            try {
                aux.stealCount += s;
            } finally {
                aux.unlock();
            }
        }

        final boolean tryRemoveAndExec(ForkJoinTask<?> task) {
            ForkJoinTask<?>[] a;
            int al;
            if (task != null && task.status >= 0) {
                do {
                    int b = this.base;
                    int s = this.top;
                    int d = b - s;
                    if (d < 0 && (a = this.array) != null && (al = a.length) > 0) {
                        while (true) {
                            s--;
                            int index = s & (al - 1);
                            long offset = (index << ASHIFT) + ABASE;
                            ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                            if (t == null) {
                                break;
                            }
                            if (t == task) {
                                boolean removed = false;
                                if (s + 1 == this.top) {
                                    if (U.compareAndSwapObject(a, offset, t, (Object) null)) {
                                        this.top = s;
                                        removed = true;
                                    }
                                } else if (this.base == b) {
                                    removed = U.compareAndSwapObject(a, offset, t, new EmptyTask());
                                }
                                if (removed) {
                                    ForkJoinTask<?> ps = this.currentSteal;
                                    this.currentSteal = task;
                                    task.doExec();
                                    this.currentSteal = ps;
                                }
                            } else if (t.status < 0 && s + 1 == this.top) {
                                if (U.compareAndSwapObject(a, offset, t, (Object) null)) {
                                    this.top = s;
                                }
                            } else {
                                d++;
                                if (d == 0) {
                                    if (this.base == b) {
                                        return false;
                                    }
                                }
                            }
                        }
                    } else {
                        return true;
                    }
                } while (task.status >= 0);
                return false;
            }
            return true;
        }

        final CountedCompleter<?> popCC(CountedCompleter<?> task, int mode) {
            int al;
            int b = this.base;
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (a != null && b != s && (al = a.length) > 0) {
                int index = (al - 1) & (s - 1);
                long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                ?? r12 = (ForkJoinTask) U.getObjectVolatile(a, offset);
                if (r12 instanceof CountedCompleter) {
                    ?? r16 = r12;
                    while (r16 != task) {
                        r16 = r16.completer;
                        if (r16 == 0) {
                            return null;
                        }
                    }
                    if ((mode & 1) == 0) {
                        boolean popped = false;
                        if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                            if (this.top == s && this.array == a && U.compareAndSwapObject(a, offset, (Object) r12, (Object) null)) {
                                popped = true;
                                this.top = s - 1;
                            }
                            U.putOrderedInt(this, QLOCK, 0);
                            if (popped) {
                                return r12;
                            }
                            return null;
                        }
                        return null;
                    }
                    if (U.compareAndSwapObject(a, offset, (Object) r12, (Object) null)) {
                        this.top = s - 1;
                        return r12;
                    }
                    return null;
                }
                return null;
            }
            return null;
        }

        final int pollAndExecCC(CountedCompleter<?> task) {
            int al;
            int h;
            int b = this.base;
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (a != null && b != s && (al = a.length) > 0) {
                int index = (al - 1) & b;
                long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                ForkJoinTask<?> o = (ForkJoinTask) U.getObjectVolatile(a, offset);
                if (o == null) {
                    return 2;
                }
                if (!(o instanceof CountedCompleter)) {
                    return -1;
                }
                ?? r14 = o;
                while (r14 != task) {
                    r14 = r14.completer;
                    if (r14 == 0) {
                        return -1;
                    }
                }
                int b2 = b + 1;
                if (b == this.base && U.compareAndSwapObject(a, offset, o, (Object) null)) {
                    this.base = b2;
                    o.doExec();
                    h = 1;
                } else {
                    h = 2;
                }
                return h;
            }
            int h2 = b | Integer.MIN_VALUE;
            return h2;
        }

        final boolean isApparentlyUnblocked() {
            Thread wt;
            Thread.State s;
            return (this.scanState < 0 || (wt = this.owner) == null || (s = wt.getState()) == Thread.State.BLOCKED || s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING) ? false : true;
        }

        static {
            try {
                QLOCK = U.objectFieldOffset(WorkQueue.class.getDeclaredField("qlock"));
                ABASE = U.arrayBaseOffset(ForkJoinTask[].class);
                int scale = U.arrayIndexScale(ForkJoinTask[].class);
                if (((scale - 1) & scale) != 0) {
                    throw new Error("array index scale not a power of two");
                }
                ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
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

    private void tryInitialize(boolean checkTermination) {
        if (this.runState == 0) {
            int p = this.config & 65535;
            int n = p > 1 ? p - 1 : 1;
            int n2 = n | (n >>> 1);
            int n3 = n2 | (n2 >>> 2);
            int n4 = n3 | (n3 >>> 4);
            int n5 = n4 | (n4 >>> 8);
            AuxState aux = new AuxState();
            WorkQueue[] ws = new WorkQueue[(((n5 | (n5 >>> 16)) + 1) << 1) & 65535];
            synchronized (modifyThreadPermission) {
                if (this.runState == 0) {
                    this.workQueues = ws;
                    this.auxState = aux;
                    this.runState = 1;
                }
            }
        }
        if (!checkTermination || this.runState >= 0) {
            return;
        }
        tryTerminate(false, false);
        throw new RejectedExecutionException();
    }

    private boolean createWorker(boolean isSpare) {
        WorkQueue q;
        ForkJoinWorkerThreadFactory fac = this.factory;
        Throwable ex = null;
        ForkJoinWorkerThread wt = null;
        if (fac != null) {
            try {
                wt = fac.newThread(this);
                if (wt != null) {
                    if (isSpare && (q = wt.workQueue) != null) {
                        q.config |= 131072;
                    }
                    wt.start();
                    return true;
                }
            } catch (Throwable rex) {
                ex = rex;
            }
        }
        deregisterWorker(wt, ex);
        return false;
    }

    private void tryAddWorker(long c) {
        do {
            long nc = ((AC_UNIT + c) & AC_MASK) | ((TC_UNIT + c) & TC_MASK);
            if (this.ctl == c && U.compareAndSwapLong(this, CTL, c, nc)) {
                createWorker(false);
                return;
            } else {
                c = this.ctl;
                if ((ADD_WORKER & c) == 0) {
                    return;
                }
            }
        } while (((int) c) == 0);
    }

    final WorkQueue registerWorker(ForkJoinWorkerThread wt) {
        int n;
        wt.setDaemon(true);
        Thread.UncaughtExceptionHandler handler = this.ueh;
        if (handler != null) {
            wt.setUncaughtExceptionHandler(handler);
        }
        WorkQueue w = new WorkQueue(this, wt);
        int i = 0;
        int mode = this.config & MODE_MASK;
        AuxState aux = this.auxState;
        if (aux != null) {
            aux.lock();
            try {
                long j = aux.indexSeed - 1640531527;
                aux.indexSeed = j;
                int s = (int) j;
                WorkQueue[] ws = this.workQueues;
                if (ws != null && (n = ws.length) > 0) {
                    int m = n - 1;
                    i = m & ((s << 1) | 1);
                    if (ws[i] != null) {
                        int probes = 0;
                        int step = n <= 4 ? 2 : ((n >>> 1) & EVENMASK) + 2;
                        while (true) {
                            i = (i + step) & m;
                            if (ws[i] == null) {
                                break;
                            }
                            probes++;
                            if (probes >= n) {
                                n <<= 1;
                                ws = (WorkQueue[]) Arrays.copyOf(ws, n);
                                this.workQueues = ws;
                                m = n - 1;
                                probes = 0;
                            }
                        }
                    }
                    w.hint = s;
                    w.config = i | mode;
                    w.scanState = (2147418112 & s) | i;
                    ws[i] = w;
                }
            } finally {
                aux.unlock();
            }
        }
        wt.setName(this.workerNamePrefix.concat(Integer.toString(i >>> 1)));
        return w;
    }

    final void deregisterWorker(ForkJoinWorkerThread wt, Throwable ex) {
        Unsafe unsafe;
        long j;
        long c;
        WorkQueue[] ws;
        int wl;
        WorkQueue w = null;
        if (wt != null && (w = wt.workQueue) != null) {
            int idx = w.config & 65535;
            int ns = w.nsteals;
            AuxState aux = this.auxState;
            if (aux != null) {
                aux.lock();
                try {
                    WorkQueue[] ws2 = this.workQueues;
                    if (ws2 != null && ws2.length > idx && ws2[idx] == w) {
                        ws2[idx] = null;
                    }
                    aux.stealCount += (long) ns;
                } finally {
                    aux.unlock();
                }
            }
        }
        if (w == null || (w.config & 262144) == 0) {
            do {
                unsafe = U;
                j = CTL;
                c = this.ctl;
            } while (!unsafe.compareAndSwapLong(this, j, c, ((c - AC_UNIT) & AC_MASK) | ((c - TC_UNIT) & TC_MASK) | (SP_MASK & c)));
        }
        if (w != null) {
            w.currentSteal = null;
            w.qlock = -1;
            w.cancelAll();
        }
        while (true) {
            if (tryTerminate(false, false) < 0 || w == null || w.array == null || (ws = this.workQueues) == null || (wl = ws.length) <= 0) {
                break;
            }
            long c2 = this.ctl;
            int sp = (int) c2;
            if (sp != 0) {
                if (tryRelease(c2, ws[(wl - 1) & sp], AC_UNIT)) {
                    break;
                }
            } else if (ex != null && (ADD_WORKER & c2) != 0) {
                tryAddWorker(c2);
            }
        }
        if (ex == null) {
            ForkJoinTask.helpExpungeStaleExceptions();
        } else {
            ForkJoinTask.rethrow(ex);
        }
    }

    final void signalWork() {
        int i;
        WorkQueue v;
        while (true) {
            long c = this.ctl;
            if (c >= 0) {
                return;
            }
            int sp = (int) c;
            if (sp == 0) {
                if ((ADD_WORKER & c) == 0) {
                    return;
                }
                tryAddWorker(c);
                return;
            }
            WorkQueue[] ws = this.workQueues;
            if (ws == null || ws.length <= (i = sp & 65535) || (v = ws[i]) == null) {
                return;
            }
            int ns = sp & Integer.MAX_VALUE;
            int vs = v.scanState;
            long nc = (((long) v.stackPred) & SP_MASK) | ((AC_UNIT + c) & UC_MASK);
            if (sp == vs && U.compareAndSwapLong(this, CTL, c, nc)) {
                v.scanState = ns;
                LockSupport.unpark(v.parker);
                return;
            }
        }
    }

    private boolean tryRelease(long c, WorkQueue v, long inc) {
        int sp = (int) c;
        int ns = sp & Integer.MAX_VALUE;
        if (v != null) {
            int vs = v.scanState;
            long nc = (((long) v.stackPred) & SP_MASK) | ((c + inc) & UC_MASK);
            if (sp == vs && U.compareAndSwapLong(this, CTL, c, nc)) {
                v.scanState = ns;
                LockSupport.unpark(v.parker);
                return true;
            }
            return false;
        }
        return false;
    }

    private void tryReactivate(WorkQueue w, WorkQueue[] ws, int r) {
        int wl;
        WorkQueue v;
        long c = this.ctl;
        int sp = (int) c;
        if (sp == 0 || w == null || ws == null || (wl = ws.length) <= 0 || ((sp ^ r) & 65536) != 0 || (v = ws[(wl - 1) & sp]) == null) {
            return;
        }
        long nc = (((long) v.stackPred) & SP_MASK) | ((AC_UNIT + c) & UC_MASK);
        int ns = sp & Integer.MAX_VALUE;
        if (w.scanState >= 0 || v.scanState != sp || !U.compareAndSwapLong(this, CTL, c, nc)) {
            return;
        }
        v.scanState = ns;
        LockSupport.unpark(v.parker);
    }

    private void inactivate(WorkQueue w, int ss) {
        long c;
        long nc;
        int ns = (65536 + ss) | Integer.MIN_VALUE;
        long lc = ((long) ns) & SP_MASK;
        if (w == null) {
            return;
        }
        w.scanState = ns;
        do {
            c = this.ctl;
            nc = lc | ((c - AC_UNIT) & UC_MASK);
            w.stackPred = (int) c;
        } while (!U.compareAndSwapLong(this, CTL, c, nc));
    }

    private int awaitWork(WorkQueue w) {
        if (w == null || w.scanState >= 0) {
            return 0;
        }
        long c = this.ctl;
        if (((int) (c >> 48)) + (this.config & 65535) <= 0) {
            int stat = timedAwaitWork(w, c);
            return stat;
        }
        if ((this.runState & 2) != 0) {
            w.qlock = -1;
            return -1;
        }
        if (w.scanState >= 0) {
            return 0;
        }
        w.parker = Thread.currentThread();
        if (w.scanState < 0) {
            LockSupport.park(this);
        }
        w.parker = null;
        if ((this.runState & 2) != 0) {
            w.qlock = -1;
            return -1;
        }
        if (w.scanState >= 0) {
            return 0;
        }
        Thread.interrupted();
        return 0;
    }

    private int timedAwaitWork(WorkQueue w, long c) {
        AuxState aux;
        WorkQueue[] ws;
        int stat = 0;
        int scale = 1 - ((short) (c >>> 32));
        if (scale <= 0) {
            scale = 1;
        }
        long deadline = (((long) scale) * IDLE_TIMEOUT_MS) + System.currentTimeMillis();
        if (this.runState >= 0 || (stat = tryTerminate(false, false)) > 0) {
            if (w != null && w.scanState < 0) {
                w.parker = Thread.currentThread();
                if (w.scanState < 0) {
                    LockSupport.parkUntil(this, deadline);
                }
                w.parker = null;
                if ((this.runState & 2) != 0) {
                    w.qlock = -1;
                    return -1;
                }
                int ss = w.scanState;
                if (ss < 0 && !Thread.interrupted() && ((int) c) == ss && (aux = this.auxState) != null && this.ctl == c && deadline - System.currentTimeMillis() <= TIMEOUT_SLOP_MS) {
                    aux.lock();
                    try {
                        int cfg = w.config;
                        int idx = cfg & 65535;
                        long nc = ((c - TC_UNIT) & UC_MASK) | (((long) w.stackPred) & SP_MASK);
                        if ((this.runState & 2) == 0 && (ws = this.workQueues) != null && idx < ws.length && idx >= 0 && ws[idx] == w && U.compareAndSwapLong(this, CTL, c, nc)) {
                            ws[idx] = null;
                            w.config = 262144 | cfg;
                            w.qlock = -1;
                            stat = -1;
                        }
                        return stat;
                    } finally {
                        aux.unlock();
                    }
                }
                return stat;
            }
            return stat;
        }
        return stat;
    }

    private boolean tryDropSpare(WorkQueue w) {
        WorkQueue[] ws;
        int wl;
        boolean zCompareAndSwapLong;
        boolean canDrop;
        long nc;
        if (w != null && w.isEmpty()) {
            do {
                long c = this.ctl;
                if (((short) (c >> 32)) > 0) {
                    int sp = (int) c;
                    if ((sp != 0 || ((int) (c >> 48)) > 0) && (ws = this.workQueues) != null && (wl = ws.length) > 0) {
                        if (sp == 0) {
                            zCompareAndSwapLong = U.compareAndSwapLong(this, CTL, c, ((c - AC_UNIT) & AC_MASK) | ((c - TC_UNIT) & TC_MASK) | (SP_MASK & c));
                        } else {
                            WorkQueue v = ws[(wl - 1) & sp];
                            if (v == null || v.scanState != sp) {
                                zCompareAndSwapLong = false;
                            } else {
                                long nc2 = ((long) v.stackPred) & SP_MASK;
                                if (w == v || w.scanState >= 0) {
                                    canDrop = true;
                                    nc = nc2 | (AC_MASK & c) | ((c - TC_UNIT) & TC_MASK);
                                } else {
                                    canDrop = false;
                                    nc = nc2 | ((AC_UNIT + c) & AC_MASK) | (TC_MASK & c);
                                }
                                if (U.compareAndSwapLong(this, CTL, c, nc)) {
                                    v.scanState = Integer.MAX_VALUE & sp;
                                    LockSupport.unpark(v.parker);
                                    zCompareAndSwapLong = canDrop;
                                } else {
                                    zCompareAndSwapLong = false;
                                }
                            }
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } while (!zCompareAndSwapLong);
            int cfg = w.config;
            int idx = cfg & 65535;
            if (idx >= 0 && idx < ws.length && ws[idx] == w) {
                ws[idx] = null;
            }
            w.config = 262144 | cfg;
            w.qlock = -1;
            return true;
        }
        return false;
    }

    final void runWorker(WorkQueue w) {
        w.growArray();
        int bound = (w.config & 131072) != 0 ? 0 : 1023;
        long seed = ((long) w.hint) * (-2685821657736338717L);
        if ((this.runState & 2) != 0) {
            return;
        }
        long r = seed == 0 ? 1L : seed;
        while (true) {
            if (bound == 0 && tryDropSpare(w)) {
                return;
            }
            int step = ((int) (r >>> 48)) | 1;
            long r2 = r ^ (r >>> 12);
            long r3 = r2 ^ (r2 << 25);
            r = r3 ^ (r3 >>> 27);
            if (scan(w, bound, step, (int) r) < 0 && awaitWork(w) < 0) {
                return;
            }
        }
    }

    private int scan(WorkQueue w, int bound, int step, int r) {
        int wl;
        ForkJoinTask<?>[] a;
        int al;
        WorkQueue[] ws = this.workQueues;
        if (ws == null || w == null || (wl = ws.length) <= 0) {
            return 0;
        }
        int m = wl - 1;
        int origin = m & r;
        int idx = origin;
        int npolls = 0;
        int ss = w.scanState;
        while (true) {
            WorkQueue q = ws[idx];
            if (q != null) {
                int b = q.base;
                if (b - q.top < 0 && (a = q.array) != null && (al = a.length) > 0) {
                    int index = (al - 1) & b;
                    long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                    ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                    if (t == null) {
                        return 0;
                    }
                    int b2 = b + 1;
                    if (b != q.base) {
                        return 0;
                    }
                    if (ss < 0) {
                        tryReactivate(w, ws, r);
                        return 0;
                    }
                    if (!U.compareAndSwapObject(a, offset, t, (Object) null)) {
                        return 0;
                    }
                    q.base = b2;
                    w.currentSteal = t;
                    if (b2 != q.top) {
                        signalWork();
                    }
                    w.runTask(t);
                    npolls++;
                    if (npolls > bound) {
                        return 0;
                    }
                }
            }
            if (npolls != 0) {
                return 0;
            }
            idx = (idx + step) & m;
            if (idx != origin) {
                continue;
            } else {
                if (ss < 0) {
                    return ss;
                }
                if (r >= 0) {
                    inactivate(w, ss);
                    return 0;
                }
                r <<= 1;
            }
        }
    }

    final int helpComplete(WorkQueue w, CountedCompleter<?> task, int maxTasks) {
        WorkQueue q;
        CountedCompleter<?> p;
        int s = 0;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            int wl = ws.length;
            if (wl > 1 && task != null && w != null) {
                int m = wl - 1;
                int mode = w.config;
                int r = ~mode;
                int origin = r & m;
                int k = origin;
                int step = 3;
                int h = 1;
                int oldSum = 0;
                int checkSum = 0;
                while (true) {
                    s = task.status;
                    if (s < 0) {
                        break;
                    }
                    if (h == 1 && (p = w.popCC(task, mode)) != null) {
                        p.doExec();
                        if (maxTasks != 0 && maxTasks - 1 == 0) {
                            break;
                        }
                        origin = k;
                        checkSum = 0;
                        oldSum = 0;
                    } else {
                        int i = k | 1;
                        if (i < 0 || i > m || (q = ws[i]) == null) {
                            h = 0;
                        } else {
                            h = q.pollAndExecCC(task);
                            if (h < 0) {
                                checkSum += h;
                            }
                        }
                        if (h > 0) {
                            if (h == 1 && maxTasks != 0 && maxTasks - 1 == 0) {
                                break;
                            }
                            step = (r >>> 16) | 3;
                            int r2 = r ^ (r << 13);
                            int r3 = r2 ^ (r2 >>> 17);
                            r = r3 ^ (r3 << 5);
                            origin = r & m;
                            k = origin;
                            checkSum = 0;
                            oldSum = 0;
                        } else {
                            k = (k + step) & m;
                            if (k == origin) {
                                int oldSum2 = checkSum;
                                if (oldSum == checkSum) {
                                    break;
                                }
                                checkSum = 0;
                                oldSum = oldSum2;
                            } else {
                                continue;
                            }
                        }
                    }
                }
            }
        }
        return s;
    }

    private void helpStealer(WorkQueue w, ForkJoinTask<?> task) {
        WorkQueue[] ws;
        int wl;
        WorkQueue v;
        int al;
        if (task == null || w == null) {
            return;
        }
        ForkJoinTask<?> ps = w.currentSteal;
        int oldSum = 0;
        while (w.tryRemoveAndExec(task) && task.status >= 0 && (ws = this.workQueues) != null && (wl = ws.length) > 0) {
            int m = wl - 1;
            int checkSum = 0;
            WorkQueue j = w;
            ForkJoinTask<?> subtask = task;
            while (true) {
                if (subtask.status >= 0) {
                    int h = j.hint | 1;
                    int k = 0;
                    do {
                        int i = ((k << 1) + h) & m;
                        v = ws[i];
                        if (v != null) {
                            if (v.currentSteal == subtask) {
                                j.hint = i;
                                while (true) {
                                    if (subtask.status >= 0) {
                                        int b = v.base;
                                        checkSum += b;
                                        ForkJoinTask<?> next = v.currentJoin;
                                        ForkJoinTask<?> t = null;
                                        ForkJoinTask<?>[] a = v.array;
                                        if (a != null && (al = a.length) > 0) {
                                            int index = (al - 1) & b;
                                            long offset = (((long) index) << ASHIFT) + ((long) ABASE);
                                            t = (ForkJoinTask) U.getObjectVolatile(a, offset);
                                            if (t != null) {
                                                int b2 = b + 1;
                                                if (b != v.base) {
                                                    b = b2;
                                                } else {
                                                    if (j.currentJoin != subtask || v.currentSteal != subtask || subtask.status < 0) {
                                                        break;
                                                    }
                                                    if (U.compareAndSwapObject(a, offset, t, (Object) null)) {
                                                        v.base = b2;
                                                        w.currentSteal = t;
                                                        int top = w.top;
                                                        while (true) {
                                                            t.doExec();
                                                            w.currentSteal = ps;
                                                            if (task.status < 0) {
                                                                return;
                                                            }
                                                            if (w.top == top) {
                                                                b = b2;
                                                                break;
                                                            } else {
                                                                t = w.pop();
                                                                if (t != null) {
                                                                    w.currentSteal = t;
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        b = b2;
                                                    }
                                                }
                                                if (t != null) {
                                                }
                                            }
                                        } else if (t != null && b == v.base && b - v.top >= 0) {
                                            break;
                                        }
                                    }
                                }
                            } else {
                                checkSum += v.base;
                            }
                        }
                        k++;
                    } while (k <= m);
                    return;
                }
                j = v;
            }
        }
    }

    private boolean tryCompensate(WorkQueue w) {
        int wl;
        WorkQueue v;
        long c = this.ctl;
        WorkQueue[] ws = this.workQueues;
        int pc = this.config & 65535;
        int ac = pc + ((int) (c >> 48));
        int tc = pc + ((short) (c >> 32));
        if (w == null || w.qlock < 0 || pc == 0 || ws == null || (wl = ws.length) <= 0) {
            return false;
        }
        int m = wl - 1;
        boolean busy = true;
        int i = 0;
        while (true) {
            if (i > m) {
                break;
            }
            int k = (i << 1) | 1;
            if (k > m || k < 0 || (v = ws[k]) == null || v.scanState < 0 || v.currentSteal != null) {
                i++;
            } else {
                busy = false;
                break;
            }
        }
        if (!busy || this.ctl != c) {
            return false;
        }
        int sp = (int) c;
        if (sp != 0) {
            return tryRelease(c, ws[m & sp], 0L);
        }
        if (tc >= pc && ac > 1 && w.isEmpty()) {
            long nc = ((c - AC_UNIT) & AC_MASK) | (281474976710655L & c);
            return U.compareAndSwapLong(this, CTL, c, nc);
        }
        if (tc >= MAX_CAP || (this == common && tc >= COMMON_MAX_SPARES + pc)) {
            throw new RejectedExecutionException("Thread limit exceeded replacing blocked worker");
        }
        boolean isSpare = tc >= pc;
        long nc2 = (AC_MASK & c) | ((TC_UNIT + c) & TC_MASK);
        if (!U.compareAndSwapLong(this, CTL, c, nc2)) {
            return false;
        }
        return createWorker(isSpare);
    }

    final int awaitJoin(WorkQueue w, ForkJoinTask<?> task, long deadline) {
        long ms;
        int s = 0;
        if (w != null) {
            ForkJoinTask<?> prevJoin = w.currentJoin;
            if (task != null && (s = task.status) >= 0) {
                w.currentJoin = task;
                CountedCompleter<?> countedCompleter = task instanceof CountedCompleter ? (CountedCompleter) task : null;
                do {
                    if (countedCompleter != null) {
                        helpComplete(w, countedCompleter, 0);
                    } else {
                        helpStealer(w, task);
                    }
                    s = task.status;
                    if (s < 0) {
                        break;
                    }
                    if (deadline == 0) {
                        ms = 0;
                    } else {
                        long ns = deadline - System.nanoTime();
                        if (ns <= 0) {
                            break;
                        }
                        ms = TimeUnit.NANOSECONDS.toMillis(ns);
                        if (ms <= 0) {
                            ms = 1;
                        }
                    }
                    if (tryCompensate(w)) {
                        task.internalWait(ms);
                        U.getAndAddLong(this, CTL, AC_UNIT);
                    }
                    s = task.status;
                } while (s >= 0);
                w.currentJoin = prevJoin;
            }
        }
        return s;
    }

    private WorkQueue findNonEmptyStealQueue() {
        int wl;
        int r = ThreadLocalRandom.nextSecondarySeed();
        WorkQueue[] ws = this.workQueues;
        if (ws != null && (wl = ws.length) > 0) {
            int m = wl - 1;
            int origin = r & m;
            int k = origin;
            int oldSum = 0;
            int checkSum = 0;
            while (true) {
                WorkQueue q = ws[k];
                if (q != null) {
                    int b = q.base;
                    if (b - q.top < 0) {
                        return q;
                    }
                    checkSum += b;
                }
                k = (k + 1) & m;
                if (k == origin) {
                    int oldSum2 = checkSum;
                    if (oldSum == checkSum) {
                        break;
                    }
                    checkSum = 0;
                    oldSum = oldSum2;
                }
            }
        }
        return null;
    }

    final void helpQuiescePool(WorkQueue w) {
        ForkJoinTask<?> ps = w.currentSteal;
        int wc = w.config;
        boolean active = true;
        while (true) {
            if (wc >= 0) {
                ForkJoinTask<?> t = w.pop();
                if (t != null) {
                    w.currentSteal = t;
                    t.doExec();
                    w.currentSteal = ps;
                }
            }
            WorkQueue q = findNonEmptyStealQueue();
            if (q != null) {
                if (!active) {
                    active = true;
                    U.getAndAddLong(this, CTL, AC_UNIT);
                }
                ForkJoinTask<?> t2 = q.pollAt(q.base);
                if (t2 != null) {
                    w.currentSteal = t2;
                    t2.doExec();
                    w.currentSteal = ps;
                    int i = w.nsteals + 1;
                    w.nsteals = i;
                    if (i < 0) {
                        w.transferStealCount(this);
                    }
                }
            } else if (active) {
                long c = this.ctl;
                long nc = ((c - AC_UNIT) & AC_MASK) | (281474976710655L & c);
                if (U.compareAndSwapLong(this, CTL, c, nc)) {
                    active = false;
                }
            } else {
                long c2 = this.ctl;
                if (((int) (c2 >> 48)) + (this.config & 65535) <= 0 && U.compareAndSwapLong(this, CTL, c2, c2 + AC_UNIT)) {
                    return;
                }
            }
        }
    }

    final ForkJoinTask<?> nextTaskFor(WorkQueue w) {
        ForkJoinTask<?> t;
        do {
            ForkJoinTask<?> t2 = w.nextLocalTask();
            if (t2 != null) {
                return t2;
            }
            WorkQueue q = findNonEmptyStealQueue();
            if (q == null) {
                return null;
            }
            t = q.pollAt(q.base);
        } while (t == null);
        return t;
    }

    static int getSurplusQueuedTaskCount() {
        int i = 0;
        Thread t = Thread.currentThread();
        if (!(t instanceof ForkJoinWorkerThread)) {
            return 0;
        }
        ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
        ForkJoinPool pool = wt.pool;
        int p = pool.config & 65535;
        WorkQueue q = wt.workQueue;
        int n = q.top - q.base;
        int a = ((int) (pool.ctl >> 48)) + p;
        int p2 = p >>> 1;
        if (a <= p2) {
            int p3 = p2 >>> 1;
            if (a > p3) {
                i = 1;
            } else {
                int p4 = p3 >>> 1;
                if (a > p4) {
                    i = 2;
                } else {
                    i = a > (p4 >>> 1) ? 4 : 8;
                }
            }
        }
        return n - i;
    }

    private int tryTerminate(boolean now, boolean enable) {
        Unsafe unsafe;
        long j;
        int rs;
        while (true) {
            int rs2 = this.runState;
            if (rs2 >= 0) {
                if (!enable || this == common) {
                    return 1;
                }
                if (rs2 == 0) {
                    tryInitialize(false);
                } else {
                    U.compareAndSwapInt(this, RUNSTATE, rs2, rs2 | Integer.MIN_VALUE);
                }
            } else {
                if ((rs2 & 2) == 0) {
                    if (!now) {
                        long oldSum = 0;
                        while (true) {
                            long checkSum = this.ctl;
                            if (((int) (checkSum >> 48)) + (this.config & 65535) > 0) {
                                return 0;
                            }
                            WorkQueue[] ws = this.workQueues;
                            if (ws != null) {
                                for (WorkQueue w : ws) {
                                    if (w != null) {
                                        int b = w.base;
                                        checkSum += (long) b;
                                        if (w.currentSteal != null || b != w.top) {
                                            return 0;
                                        }
                                    }
                                }
                            }
                            long oldSum2 = checkSum;
                            if (oldSum == checkSum) {
                                break;
                            }
                            oldSum = oldSum2;
                        }
                    }
                    do {
                        unsafe = U;
                        j = RUNSTATE;
                        rs = this.runState;
                    } while (!unsafe.compareAndSwapInt(this, j, rs, rs | 2));
                }
                long oldSum3 = 0;
                while (true) {
                    long checkSum2 = this.ctl;
                    WorkQueue[] ws2 = this.workQueues;
                    if (ws2 != null) {
                        for (WorkQueue w2 : ws2) {
                            if (w2 != null) {
                                w2.cancelAll();
                                checkSum2 += (long) w2.base;
                                if (w2.qlock >= 0) {
                                    w2.qlock = -1;
                                    ForkJoinWorkerThread wt = w2.owner;
                                    if (wt != null) {
                                        try {
                                            wt.interrupt();
                                        } catch (Throwable th) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                    long oldSum4 = checkSum2;
                    if (oldSum3 == checkSum2) {
                        break;
                    }
                    oldSum3 = oldSum4;
                }
                if (((short) (this.ctl >>> 32)) + (this.config & 65535) <= 0) {
                    this.runState = -2147483641;
                    synchronized (this) {
                        notifyAll();
                    }
                    return -1;
                }
                return -1;
            }
        }
    }

    private void tryCreateExternalQueue(int index) {
        AuxState aux = this.auxState;
        if (aux == null || index < 0) {
            return;
        }
        WorkQueue q = new WorkQueue(this, null);
        q.config = index;
        q.scanState = Integer.MAX_VALUE;
        q.qlock = 1;
        boolean installed = false;
        aux.lock();
        try {
            WorkQueue[] ws = this.workQueues;
            if (ws != null && index < ws.length && ws[index] == null) {
                ws[index] = q;
                installed = true;
            }
            if (!installed) {
                return;
            }
            try {
                q.growArray();
            } finally {
                q.qlock = 0;
            }
        } finally {
            aux.unlock();
        }
    }

    final void externalPush(ForkJoinTask<?> task) {
        int wl;
        int r = ThreadLocalRandom.getProbe();
        if (r == 0) {
            ThreadLocalRandom.localInit();
            r = ThreadLocalRandom.getProbe();
        }
        while (true) {
            int rs = this.runState;
            WorkQueue[] ws = this.workQueues;
            if (rs <= 0 || ws == null || (wl = ws.length) <= 0) {
                tryInitialize(true);
            } else {
                int k = (wl - 1) & r & 126;
                WorkQueue q = ws[k];
                if (q == null) {
                    tryCreateExternalQueue(k);
                } else {
                    int stat = q.sharedPush(task);
                    if (stat < 0) {
                        return;
                    }
                    if (stat == 0) {
                        signalWork();
                        return;
                    }
                    r = ThreadLocalRandom.advanceProbe(r);
                }
            }
        }
    }

    private <T> ForkJoinTask<T> externalSubmit(ForkJoinTask<T> task) {
        WorkQueue q;
        if (task == null) {
            throw new NullPointerException();
        }
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread w = (ForkJoinWorkerThread) t;
            if (w.pool == this && (q = w.workQueue) != null) {
                q.push(task);
            } else {
                externalPush(task);
            }
        }
        return task;
    }

    static WorkQueue commonSubmitterQueue() {
        WorkQueue[] ws;
        int wl;
        ForkJoinPool p = common;
        int r = ThreadLocalRandom.getProbe();
        if (p == null || (ws = p.workQueues) == null || (wl = ws.length) <= 0) {
            return null;
        }
        return ws[(wl - 1) & r & 126];
    }

    final boolean tryExternalUnpush(ForkJoinTask<?> task) {
        int wl;
        WorkQueue w;
        int r = ThreadLocalRandom.getProbe();
        WorkQueue[] ws = this.workQueues;
        if (ws == null || (wl = ws.length) <= 0 || (w = ws[(wl - 1) & r & 126]) == null) {
            return false;
        }
        return w.trySharedUnpush(task);
    }

    final int externalHelpComplete(CountedCompleter<?> task, int maxTasks) {
        int wl;
        int r = ThreadLocalRandom.getProbe();
        WorkQueue[] ws = this.workQueues;
        if (ws == null || (wl = ws.length) <= 0) {
            return 0;
        }
        return helpComplete(ws[(wl - 1) & r & 126], task, maxTasks);
    }

    public ForkJoinPool() {
        this(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()), defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int parallelism) {
        this(parallelism, defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, Thread.UncaughtExceptionHandler handler, boolean asyncMode) {
        this(checkParallelism(parallelism), checkFactory(factory), handler, asyncMode ? Integer.MIN_VALUE : 0, "ForkJoinPool-" + nextPoolId() + "-worker-");
        checkPermission();
    }

    private static int checkParallelism(int parallelism) {
        if (parallelism <= 0 || parallelism > MAX_CAP) {
            throw new IllegalArgumentException();
        }
        return parallelism;
    }

    private static ForkJoinWorkerThreadFactory checkFactory(ForkJoinWorkerThreadFactory factory) {
        if (factory == null) {
            throw new NullPointerException();
        }
        return factory;
    }

    private ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, Thread.UncaughtExceptionHandler handler, int mode, String workerNamePrefix) {
        this.workerNamePrefix = workerNamePrefix;
        this.factory = factory;
        this.ueh = handler;
        this.config = (65535 & parallelism) | mode;
        long np = -parallelism;
        this.ctl = ((np << 48) & AC_MASK) | ((np << 32) & TC_MASK);
    }

    public static ForkJoinPool commonPool() {
        return common;
    }

    public <T> T invoke(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        externalSubmit(task);
        return task.join();
    }

    public void execute(ForkJoinTask<?> task) {
        externalSubmit(task);
    }

    @Override
    public void execute(Runnable runnable) {
        ForkJoinTask<?> job;
        if (runnable == 0) {
            throw new NullPointerException();
        }
        if (runnable instanceof ForkJoinTask) {
            job = (ForkJoinTask) runnable;
        } else {
            job = new ForkJoinTask.RunnableExecuteAction(runnable);
        }
        externalSubmit(job);
    }

    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        return externalSubmit(task);
    }

    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        return externalSubmit(new ForkJoinTask.AdaptedCallable(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        return externalSubmit(new ForkJoinTask.AdaptedRunnable(task, result));
    }

    @Override
    public ForkJoinTask<?> submit(Runnable runnable) {
        ForkJoinTask<?> job;
        if (runnable == 0) {
            throw new NullPointerException();
        }
        if (runnable instanceof ForkJoinTask) {
            job = (ForkJoinTask) runnable;
        } else {
            job = new ForkJoinTask.AdaptedRunnableAction(runnable);
        }
        return externalSubmit(job);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            for (Callable<T> t : tasks) {
                ForkJoinTask.AdaptedCallable adaptedCallable = new ForkJoinTask.AdaptedCallable(t);
                futures.add(adaptedCallable);
                externalSubmit(adaptedCallable);
            }
            int size = futures.size();
            for (int i = 0; i < size; i++) {
                ((ForkJoinTask) futures.get(i)).quietlyJoin();
            }
            return futures;
        } catch (Throwable t2) {
            int size2 = futures.size();
            for (int i2 = 0; i2 < size2; i2++) {
                futures.get(i2).cancel(false);
            }
            throw t2;
        }
    }

    public ForkJoinWorkerThreadFactory getFactory() {
        return this.factory;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return this.ueh;
    }

    public int getParallelism() {
        int par = this.config & 65535;
        if (par > 0) {
            return par;
        }
        return 1;
    }

    public static int getCommonPoolParallelism() {
        return COMMON_PARALLELISM;
    }

    public int getPoolSize() {
        return (this.config & 65535) + ((short) (this.ctl >>> 32));
    }

    public boolean getAsyncMode() {
        return (this.config & Integer.MIN_VALUE) != 0;
    }

    public int getRunningThreadCount() {
        int rc = 0;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 1; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
                if (w != null && w.isApparentlyUnblocked()) {
                    rc++;
                }
            }
        }
        return rc;
    }

    public int getActiveThreadCount() {
        int r = (this.config & 65535) + ((int) (this.ctl >> 48));
        if (r <= 0) {
            return 0;
        }
        return r;
    }

    public boolean isQuiescent() {
        return (this.config & 65535) + ((int) (this.ctl >> 48)) <= 0;
    }

    public long getStealCount() {
        AuxState sc = this.auxState;
        long count = sc == null ? 0L : sc.stealCount;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 1; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
                if (w != null) {
                    count += (long) w.nsteals;
                }
            }
        }
        return count;
    }

    public long getQueuedTaskCount() {
        long count = 0;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 1; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
                if (w != null) {
                    count += (long) w.queueSize();
                }
            }
        }
        return count;
    }

    public int getQueuedSubmissionCount() {
        int count = 0;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 0; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
                if (w != null) {
                    count += w.queueSize();
                }
            }
        }
        return count;
    }

    public boolean hasQueuedSubmissions() {
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 0; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
                if (w != null && !w.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    protected ForkJoinTask<?> pollSubmission() {
        int wl;
        ForkJoinTask<?> t;
        ThreadLocalRandom.nextSecondarySeed();
        WorkQueue[] ws = this.workQueues;
        if (ws != null && (wl = ws.length) > 0) {
            int m = wl - 1;
            for (int i = 0; i < wl; i++) {
                WorkQueue w = ws[(i << 1) & m];
                if (w != null && (t = w.poll()) != null) {
                    return t;
                }
            }
        }
        return null;
    }

    protected int drainTasksTo(Collection<? super ForkJoinTask<?>> c) {
        int count = 0;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (WorkQueue w : ws) {
                if (w != null) {
                    while (true) {
                        ForkJoinTask<?> t = w.poll();
                        if (t != null) {
                            c.add(t);
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    public String toString() {
        long qt = 0;
        long qs = 0;
        int rc = 0;
        AuxState sc = this.auxState;
        long st = sc == null ? 0L : sc.stealCount;
        long c = this.ctl;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 0; i < ws.length; i++) {
                WorkQueue w = ws[i];
                if (w != null) {
                    int size = w.queueSize();
                    if ((i & 1) == 0) {
                        qs += (long) size;
                    } else {
                        qt += (long) size;
                        st += (long) w.nsteals;
                        if (w.isApparentlyUnblocked()) {
                            rc++;
                        }
                    }
                }
            }
        }
        int pc = this.config & 65535;
        int tc = pc + ((short) (c >>> 32));
        int ac = pc + ((int) (c >> 48));
        if (ac < 0) {
            ac = 0;
        }
        int rs = this.runState;
        String level = (rs & 4) != 0 ? "Terminated" : (rs & 2) != 0 ? "Terminating" : (Integer.MIN_VALUE & rs) != 0 ? "Shutting down" : "Running";
        return super.toString() + "[" + level + ", parallelism = " + pc + ", size = " + tc + ", active = " + ac + ", running = " + rc + ", steals = " + st + ", tasks = " + qt + ", submissions = " + qs + "]";
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
        int rs = this.runState;
        return (rs & 2) != 0 && (rs & 4) == 0;
    }

    @Override
    public boolean isShutdown() {
        return (this.runState & Integer.MIN_VALUE) != 0;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (this == common) {
            awaitQuiescence(timeout, unit);
            return false;
        }
        long nanos = unit.toNanos(timeout);
        if (isTerminated()) {
            return true;
        }
        if (nanos <= 0) {
            return false;
        }
        long deadline = System.nanoTime() + nanos;
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
                nanos = deadline - System.nanoTime();
            }
            return true;
        }
    }

    public boolean awaitQuiescence(long timeout, TimeUnit unit) {
        WorkQueue[] ws;
        int wl;
        WorkQueue q;
        long nanos = unit.toNanos(timeout);
        Thread thread = Thread.currentThread();
        if (thread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread) thread;
            if (wt.pool == this) {
                helpQuiescePool(wt.workQueue);
                return true;
            }
        }
        long startTime = System.nanoTime();
        int r = 0;
        boolean found = true;
        while (!isQuiescent() && (ws = this.workQueues) != null && (wl = ws.length) > 0) {
            if (!found) {
                if (System.nanoTime() - startTime > nanos) {
                    return false;
                }
                Thread.yield();
            }
            found = false;
            int m = wl - 1;
            int j = (m + 1) << 2;
            int r2 = r;
            while (true) {
                if (j < 0) {
                    r = r2;
                    break;
                }
                r = r2 + 1;
                int k = r2 & m;
                if (k <= m && k >= 0 && (q = ws[k]) != null) {
                    int b = q.base;
                    if (b - q.top < 0) {
                        found = true;
                        ForkJoinTask<?> t = q.pollAt(b);
                        if (t != null) {
                            t.doExec();
                        }
                    }
                }
                j--;
                r2 = r;
            }
        }
        return true;
    }

    static void quiesceCommonPool() {
        common.awaitQuiescence(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    public static void managedBlock(ManagedBlocker blocker) throws InterruptedException {
        ForkJoinWorkerThread wt;
        ForkJoinPool p;
        Thread t = Thread.currentThread();
        if ((t instanceof ForkJoinWorkerThread) && (p = (wt = (ForkJoinWorkerThread) t).pool) != null) {
            WorkQueue w = wt.workQueue;
            while (!blocker.isReleasable()) {
                if (p.tryCompensate(w)) {
                    while (!blocker.isReleasable() && !blocker.block()) {
                        try {
                        } finally {
                            U.getAndAddLong(p, CTL, AC_UNIT);
                        }
                    }
                    return;
                }
            }
            return;
        }
        while (!blocker.isReleasable() && !blocker.block()) {
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ForkJoinTask.AdaptedRunnable(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ForkJoinTask.AdaptedCallable(callable);
    }

    static {
        DefaultForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory2 = null;
        try {
            CTL = U.objectFieldOffset(ForkJoinPool.class.getDeclaredField("ctl"));
            RUNSTATE = U.objectFieldOffset(ForkJoinPool.class.getDeclaredField("runState"));
            ABASE = U.arrayBaseOffset(ForkJoinTask[].class);
            int scale = U.arrayIndexScale(ForkJoinTask[].class);
            if (((scale - 1) & scale) != 0) {
                throw new Error("array index scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
            int commonMaxSpares = 256;
            try {
                String p = System.getProperty("java.util.concurrent.ForkJoinPool.common.maximumSpares");
                if (p != null) {
                    commonMaxSpares = Integer.parseInt(p);
                }
            } catch (Exception e) {
            }
            COMMON_MAX_SPARES = commonMaxSpares;
            defaultForkJoinWorkerThreadFactory = new DefaultForkJoinWorkerThreadFactory(defaultForkJoinWorkerThreadFactory2);
            modifyThreadPermission = new RuntimePermission("modifyThread");
            common = (ForkJoinPool) AccessController.doPrivileged(new PrivilegedAction<ForkJoinPool>() {
                @Override
                public ForkJoinPool run() {
                    return ForkJoinPool.makeCommonPool();
                }
            });
            COMMON_PARALLELISM = Math.max(common.config & 65535, 1);
        } catch (ReflectiveOperationException e2) {
            throw new Error(e2);
        }
    }

    static ForkJoinPool makeCommonPool() {
        InnocuousForkJoinWorkerThreadFactory innocuousForkJoinWorkerThreadFactory = null;
        int parallelism = -1;
        ForkJoinWorkerThreadFactory factory = null;
        Thread.UncaughtExceptionHandler handler = null;
        try {
            String pp = System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism");
            String fp = System.getProperty("java.util.concurrent.ForkJoinPool.common.threadFactory");
            String hp = System.getProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler");
            if (pp != null) {
                parallelism = Integer.parseInt(pp);
            }
            if (fp != null) {
                factory = (ForkJoinWorkerThreadFactory) ClassLoader.getSystemClassLoader().loadClass(fp).newInstance();
            }
            if (hp != null) {
                handler = (Thread.UncaughtExceptionHandler) ClassLoader.getSystemClassLoader().loadClass(hp).newInstance();
            }
        } catch (Exception e) {
        }
        if (factory == null) {
            if (System.getSecurityManager() == null) {
                factory = defaultForkJoinWorkerThreadFactory;
            } else {
                factory = new InnocuousForkJoinWorkerThreadFactory(innocuousForkJoinWorkerThreadFactory);
            }
        }
        if (parallelism < 0 && Runtime.getRuntime().availableProcessors() - 1 <= 0) {
            parallelism = 1;
        }
        if (parallelism > MAX_CAP) {
            parallelism = MAX_CAP;
        }
        return new ForkJoinPool(parallelism, factory, handler, 0, "ForkJoinPool.commonPool-worker-");
    }

    private static final class InnocuousForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        private static final AccessControlContext innocuousAcc;

        InnocuousForkJoinWorkerThreadFactory(InnocuousForkJoinWorkerThreadFactory innocuousForkJoinWorkerThreadFactory) {
            this();
        }

        private InnocuousForkJoinWorkerThreadFactory() {
        }

        static {
            Permissions innocuousPerms = new Permissions();
            innocuousPerms.add(ForkJoinPool.modifyThreadPermission);
            innocuousPerms.add(new RuntimePermission("enableContextClassLoaderOverride"));
            innocuousPerms.add(new RuntimePermission("modifyThreadGroup"));
            innocuousAcc = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, innocuousPerms)});
        }

        @Override
        public final ForkJoinWorkerThread newThread(final ForkJoinPool pool) {
            return (ForkJoinWorkerThread) AccessController.doPrivileged(new PrivilegedAction<ForkJoinWorkerThread>() {
                @Override
                public ForkJoinWorkerThread run() {
                    return new ForkJoinWorkerThread.InnocuousForkJoinWorkerThread(pool);
                }
            }, innocuousAcc);
        }
    }
}
