package java.util.concurrent;

import java.lang.Thread;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import sun.misc.Unsafe;

public class ForkJoinPool extends AbstractExecutorService {
    private static final int ABASE;
    private static final long AC_MASK = -281474976710656L;
    private static final int AC_SHIFT = 48;
    private static final long AC_UNIT = 281474976710656L;
    private static final int ASHIFT;
    private static final long CTL;
    private static final int EC_SHIFT = 16;
    private static final int EVENMASK = 65534;
    private static final int E_MASK = Integer.MAX_VALUE;
    private static final int E_SEQ = 65536;
    private static final long FAST_IDLE_TIMEOUT = 200000000;
    static final int FIFO_QUEUE = 1;
    private static final long IDLE_TIMEOUT = 2000000000;
    private static final long INDEXSEED;
    private static final int INT_SIGN = Integer.MIN_VALUE;
    static final int LIFO_QUEUE = 0;
    private static final int MAX_CAP = 32767;
    private static final int MAX_HELP = 64;
    private static final long PARKBLOCKER;
    private static final long PLOCK;
    private static final int PL_LOCK = 2;
    private static final int PL_SIGNAL = 1;
    private static final int PL_SPINS = 256;
    private static final long QBASE;
    private static final long QLOCK;
    private static final int SEED_INCREMENT = 1640531527;
    static final int SHARED_QUEUE = -1;
    private static final int SHORT_SIGN = 32768;
    private static final int SHUTDOWN = Integer.MIN_VALUE;
    private static final int SMASK = 65535;
    private static final int SQMASK = 126;
    private static final long STEALCOUNT;
    private static final long STOP_BIT = 2147483648L;
    private static final int ST_SHIFT = 31;
    private static final long TC_MASK = 281470681743360L;
    private static final int TC_SHIFT = 32;
    private static final long TC_UNIT = 4294967296L;
    private static final long TIMEOUT_SLOP = 2000000;
    private static final Unsafe U;
    private static final int UAC_MASK = -65536;
    private static final int UAC_SHIFT = 16;
    private static final int UAC_UNIT = 65536;
    private static final int UTC_MASK = 65535;
    private static final int UTC_SHIFT = 0;
    private static final int UTC_UNIT = 1;
    static final ForkJoinPool common;
    static final int commonParallelism;
    public static final ForkJoinWorkerThreadFactory defaultForkJoinWorkerThreadFactory;
    private static final RuntimePermission modifyThreadPermission;
    private static int poolNumberSequence;
    static final ThreadLocal<Submitter> submitters;
    volatile long ctl;
    final ForkJoinWorkerThreadFactory factory;
    volatile int indexSeed;
    final short mode;
    volatile long pad00;
    volatile long pad01;
    volatile long pad02;
    volatile long pad03;
    volatile long pad04;
    volatile long pad05;
    volatile long pad06;
    volatile Object pad10;
    volatile Object pad11;
    volatile Object pad12;
    volatile Object pad13;
    volatile Object pad14;
    volatile Object pad15;
    volatile Object pad16;
    volatile Object pad17;
    volatile Object pad18;
    volatile Object pad19;
    volatile Object pad1a;
    volatile Object pad1b;
    final short parallelism;
    volatile int plock;
    volatile long stealCount;
    final Thread.UncaughtExceptionHandler ueh;
    WorkQueue[] workQueues;
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
        if (security != null) {
            security.checkPermission(modifyThreadPermission);
        }
    }

    static final class DefaultForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
        DefaultForkJoinWorkerThreadFactory() {
        }

        @Override
        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return new ForkJoinWorkerThread(pool);
        }
    }

    static final class EmptyTask extends ForkJoinTask<Void> {
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

    static final class WorkQueue {
        private static final int ABASE;
        private static final int ASHIFT;
        static final int INITIAL_QUEUE_CAPACITY = 8192;
        static final int MAXIMUM_QUEUE_CAPACITY = 67108864;
        private static final long QBASE;
        private static final long QLOCK;
        private static final Unsafe U;
        ForkJoinTask<?>[] array;
        volatile ForkJoinTask<?> currentJoin;
        ForkJoinTask<?> currentSteal;
        volatile int eventCount;
        int hint;
        final short mode;
        int nextWait;
        int nsteals;
        final ForkJoinWorkerThread owner;
        volatile long pad00;
        volatile long pad01;
        volatile long pad02;
        volatile long pad03;
        volatile long pad04;
        volatile long pad05;
        volatile long pad06;
        volatile Object pad10;
        volatile Object pad11;
        volatile Object pad12;
        volatile Object pad13;
        volatile Object pad14;
        volatile Object pad15;
        volatile Object pad16;
        volatile Object pad17;
        volatile Object pad18;
        volatile Object pad19;
        volatile Object pad1a;
        volatile Object pad1b;
        volatile Object pad1c;
        volatile Object pad1d;
        volatile Thread parker;
        final ForkJoinPool pool;
        short poolIndex;
        volatile int qlock;
        int top = 4096;
        volatile int base = 4096;

        WorkQueue(ForkJoinPool pool, ForkJoinWorkerThread owner, int mode, int seed) {
            this.pool = pool;
            this.owner = owner;
            this.mode = (short) mode;
            this.hint = seed;
        }

        final int queueSize() {
            int n = this.base - this.top;
            if (n >= 0) {
                return 0;
            }
            return -n;
        }

        final boolean isEmpty() {
            ForkJoinTask<?>[] a;
            int m;
            int i = this.base;
            int s = this.top;
            int n = i - s;
            return n >= 0 || (n == -1 && ((a = this.array) == null || (m = a.length + (-1)) < 0 || U.getObject(a, ((long) (((s + (-1)) & m) << ASHIFT)) + ((long) ABASE)) == null));
        }

        final void push(ForkJoinTask<?> task) {
            int s = this.top;
            ForkJoinTask<?>[] a = this.array;
            if (a != null) {
                int m = a.length - 1;
                U.putOrderedObject(a, ((m & s) << ASHIFT) + ABASE, task);
                int i = s + 1;
                this.top = i;
                int n = i - this.base;
                if (n <= 2) {
                    ForkJoinPool p = this.pool;
                    p.signalWork(p.workQueues, this);
                } else if (n >= m) {
                    growArray();
                }
            }
        }

        final ForkJoinTask<?>[] growArray() {
            int oldMask;
            ForkJoinTask<?>[] oldA = this.array;
            int size = oldA != null ? oldA.length << 1 : 8192;
            if (size > MAXIMUM_QUEUE_CAPACITY) {
                throw new RejectedExecutionException("Queue capacity exceeded");
            }
            ForkJoinTask<?>[] a = new ForkJoinTask[size];
            this.array = a;
            if (oldA != null && oldA.length - 1 >= 0) {
                int t = this.top;
                int b = this.base;
                if (t - b > 0) {
                    int mask = size - 1;
                    do {
                        int oldj = ((b & oldMask) << ASHIFT) + ABASE;
                        int j = ((b & mask) << ASHIFT) + ABASE;
                        ForkJoinTask<?> x = (ForkJoinTask) U.getObjectVolatile(oldA, oldj);
                        if (x != null && U.compareAndSwapObject(oldA, oldj, x, null)) {
                            U.putObjectVolatile(a, j, x);
                        }
                        b++;
                    } while (b != t);
                }
            }
            return a;
        }

        final ForkJoinTask<?> pop() {
            int m;
            int s;
            long j;
            ForkJoinTask<?> t;
            ForkJoinTask<?>[] a = this.array;
            if (a != null && a.length - 1 >= 0) {
                do {
                    s = this.top - 1;
                    if (s - this.base >= 0) {
                        j = ((m & s) << ASHIFT) + ABASE;
                        t = (ForkJoinTask) U.getObject(a, j);
                        if (t == null) {
                        }
                    }
                } while (!U.compareAndSwapObject(a, j, t, null));
                this.top = s;
                return t;
            }
            return null;
        }

        final ForkJoinTask<?> pollAt(int b) {
            ForkJoinTask<?>[] a = this.array;
            if (a != null) {
                int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, j);
                if (t != null && this.base == b && U.compareAndSwapObject(a, j, t, null)) {
                    U.putOrderedInt(this, QBASE, b + 1);
                    return t;
                }
            }
            return null;
        }

        final ForkJoinTask<?> poll() {
            ForkJoinTask<?>[] a;
            while (true) {
                int b = this.base;
                if (b - this.top >= 0 || (a = this.array) == null) {
                    break;
                }
                int j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, j);
                if (t != null) {
                    if (U.compareAndSwapObject(a, j, t, null)) {
                        U.putOrderedInt(this, QBASE, b + 1);
                        return t;
                    }
                } else if (this.base != b) {
                    continue;
                } else {
                    if (b + 1 == this.top) {
                        break;
                    }
                    Thread.yield();
                }
            }
            return null;
        }

        final ForkJoinTask<?> nextLocalTask() {
            return this.mode == 0 ? pop() : poll();
        }

        final ForkJoinTask<?> peek() {
            int m;
            ForkJoinTask<?>[] a = this.array;
            if (a == null || a.length - 1 < 0) {
                return null;
            }
            int i = this.mode == 0 ? this.top - 1 : this.base;
            int j = ((i & m) << ASHIFT) + ABASE;
            return (ForkJoinTask) U.getObjectVolatile(a, j);
        }

        final boolean tryUnpush(ForkJoinTask<?> t) {
            int s;
            ForkJoinTask<?>[] a = this.array;
            if (a != null && (s = this.top) != this.base) {
                int s2 = s - 1;
                if (U.compareAndSwapObject(a, (((a.length - 1) & s2) << ASHIFT) + ABASE, t, null)) {
                    this.top = s2;
                    return true;
                }
            }
            return false;
        }

        final void cancelAll() {
            ForkJoinTask.cancelIgnoringExceptions(this.currentJoin);
            ForkJoinTask.cancelIgnoringExceptions(this.currentSteal);
            while (true) {
                ForkJoinTask<?> t = poll();
                if (t != null) {
                    ForkJoinTask.cancelIgnoringExceptions(t);
                } else {
                    return;
                }
            }
        }

        final void pollAndExecAll() {
            while (true) {
                ForkJoinTask<?> t = poll();
                if (t != null) {
                    t.doExec();
                } else {
                    return;
                }
            }
        }

        final void runTask(ForkJoinTask<?> task) {
            this.currentSteal = task;
            if (task != null) {
                task.doExec();
                ForkJoinTask<?>[] a = this.array;
                int md = this.mode;
                this.nsteals++;
                this.currentSteal = null;
                if (md != 0) {
                    pollAndExecAll();
                    return;
                }
                if (a != null) {
                    int m = a.length - 1;
                    while (true) {
                        int s = this.top - 1;
                        if (s - this.base >= 0) {
                            long i = ((m & s) << ASHIFT) + ABASE;
                            ForkJoinTask<?> t = (ForkJoinTask) U.getObject(a, i);
                            if (t != null) {
                                if (U.compareAndSwapObject(a, i, t, null)) {
                                    this.top = s;
                                    t.doExec();
                                }
                            } else {
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                }
            }
        }

        final boolean tryRemoveAndExec(ForkJoinTask<?> task) {
            ForkJoinTask<?>[] a;
            int m;
            int b;
            if (task != null && (a = this.array) != null && a.length - 1 >= 0 && (n = (s = this.top) - (b = this.base)) > 0) {
                boolean removed = false;
                boolean empty = true;
                boolean stat = true;
                while (true) {
                    int s = s - 1;
                    long j = ((s & m) << ASHIFT) + ABASE;
                    ForkJoinTask<?> t = (ForkJoinTask) U.getObject(a, j);
                    if (t == null) {
                        break;
                    }
                    if (t == task) {
                        if (s + 1 == this.top) {
                            if (U.compareAndSwapObject(a, j, task, null)) {
                                this.top = s;
                                removed = true;
                            }
                        } else if (this.base == b) {
                            removed = U.compareAndSwapObject(a, j, task, new EmptyTask());
                        }
                    } else {
                        if (t.status >= 0) {
                            empty = false;
                        } else if (s + 1 == this.top) {
                            if (U.compareAndSwapObject(a, j, t, null)) {
                                this.top = s;
                            }
                        }
                        int n = n - 1;
                        if (n == 0) {
                            if (!empty && this.base == b) {
                                stat = false;
                            }
                        }
                    }
                }
                if (removed) {
                    task.doExec();
                    return stat;
                }
                return stat;
            }
            return false;
        }

        final boolean pollAndExecCC(CountedCompleter<?> root) {
            ForkJoinTask<?>[] a;
            int b = this.base;
            if (b - this.top < 0 && (a = this.array) != null) {
                long j = (((a.length - 1) & b) << ASHIFT) + ABASE;
                Object o = U.getObjectVolatile(a, j);
                if (o == null) {
                    return true;
                }
                if (o instanceof CountedCompleter) {
                    CountedCompleter<?> t = (CountedCompleter) o;
                    CountedCompleter<?> r = t;
                    while (r != root) {
                        r = r.completer;
                        if (r == null) {
                        }
                    }
                    if (this.base == b && U.compareAndSwapObject(a, j, t, null)) {
                        U.putOrderedInt(this, QBASE, b + 1);
                        t.doExec();
                    }
                    return true;
                }
            }
            return false;
        }

        final boolean externalPopAndExecCC(CountedCompleter<?> root) {
            ForkJoinTask<?>[] a;
            int i = this.base;
            int s = this.top;
            if (i - s < 0 && (a = this.array) != null) {
                long j = (((a.length - 1) & (s - 1)) << ASHIFT) + ABASE;
                Object o = U.getObject(a, j);
                if (o instanceof CountedCompleter) {
                    CountedCompleter<?> t = (CountedCompleter) o;
                    CountedCompleter<?> r = t;
                    while (r != root) {
                        r = r.completer;
                        if (r == null) {
                        }
                    }
                    if (U.compareAndSwapInt(this, QLOCK, 0, 1)) {
                        if (this.top == s && this.array == a && U.compareAndSwapObject(a, j, t, null)) {
                            this.top = s - 1;
                            this.qlock = 0;
                            t.doExec();
                        } else {
                            this.qlock = 0;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        final boolean internalPopAndExecCC(CountedCompleter<?> root) {
            ForkJoinTask<?>[] a;
            int i = this.base;
            int s = this.top;
            if (i - s < 0 && (a = this.array) != null) {
                long j = (((a.length - 1) & (s - 1)) << ASHIFT) + ABASE;
                Object o = U.getObject(a, j);
                if (o instanceof CountedCompleter) {
                    CountedCompleter<?> t = (CountedCompleter) o;
                    CountedCompleter<?> r = t;
                    while (r != root) {
                        r = r.completer;
                        if (r == null) {
                        }
                    }
                    if (U.compareAndSwapObject(a, j, t, null)) {
                        this.top = s - 1;
                        t.doExec();
                    }
                    return true;
                }
            }
            return false;
        }

        final boolean isApparentlyUnblocked() {
            Thread wt;
            Thread.State s;
            return (this.eventCount < 0 || (wt = this.owner) == null || (s = wt.getState()) == Thread.State.BLOCKED || s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING) ? false : true;
        }

        static {
            try {
                U = Unsafe.getUnsafe();
                QBASE = U.objectFieldOffset(WorkQueue.class.getDeclaredField("base"));
                QLOCK = U.objectFieldOffset(WorkQueue.class.getDeclaredField("qlock"));
                ABASE = U.arrayBaseOffset(ForkJoinTask[].class);
                int scale = U.arrayIndexScale(ForkJoinTask[].class);
                if (((scale - 1) & scale) != 0) {
                    throw new Error("data type scale not a power of two");
                }
                ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
            } catch (Exception e) {
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

    private int acquirePlock() {
        int spins = 256;
        while (true) {
            int ps = this.plock;
            if ((ps & 2) == 0) {
                int nps = ps + 2;
                if (U.compareAndSwapInt(this, PLOCK, ps, nps)) {
                    return nps;
                }
            }
            if (spins >= 0) {
                if (ThreadLocalRandom.current().nextInt() >= 0) {
                    spins--;
                }
            } else if (U.compareAndSwapInt(this, PLOCK, ps, ps | 1)) {
                synchronized (this) {
                    if ((this.plock & 1) != 0) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            try {
                                Thread.currentThread().interrupt();
                            } catch (SecurityException e2) {
                            }
                        }
                    } else {
                        notifyAll();
                    }
                }
            } else {
                continue;
            }
        }
    }

    private void releasePlock(int ps) {
        this.plock = ps;
        synchronized (this) {
            notifyAll();
        }
    }

    private void tryAddWorker() {
        long c;
        int e;
        long nc;
        do {
            c = this.ctl;
            int u = (int) (c >>> 32);
            if (u < 0 && (32768 & u) != 0 && (e = (int) c) >= 0) {
                nc = (((long) (((u + 1) & 65535) | ((65536 + u) & UAC_MASK))) << 32) | ((long) e);
            } else {
                return;
            }
        } while (!U.compareAndSwapLong(this, CTL, c, nc));
        Throwable ex = null;
        ForkJoinWorkerThread wt = null;
        try {
            ForkJoinWorkerThreadFactory fac = this.factory;
            if (fac != null && (wt = fac.newThread(this)) != null) {
                wt.start();
                return;
            }
        } catch (Throwable rex) {
            ex = rex;
        }
        deregisterWorker(wt, ex);
    }

    final WorkQueue registerWorker(ForkJoinWorkerThread wt) {
        int s;
        int ps;
        wt.setDaemon(true);
        Thread.UncaughtExceptionHandler handler = this.ueh;
        if (handler != null) {
            wt.setUncaughtExceptionHandler(handler);
        }
        while (true) {
            Unsafe unsafe = U;
            long j = INDEXSEED;
            int s2 = this.indexSeed;
            s = s2 + SEED_INCREMENT;
            if (unsafe.compareAndSwapInt(this, j, s2, s) && s != 0) {
                break;
            }
        }
        WorkQueue w = new WorkQueue(this, wt, this.mode, s);
        int ps2 = this.plock;
        if ((ps2 & 2) == 0) {
            int ps3 = ps2 + 2;
            ps = !U.compareAndSwapInt(this, PLOCK, ps2, ps3) ? acquirePlock() : ps3;
        }
        int nps = (Integer.MIN_VALUE & ps) | ((ps + 2) & Integer.MAX_VALUE);
        try {
            WorkQueue[] ws = this.workQueues;
            if (ws != null) {
                int n = ws.length;
                int m = n - 1;
                int r = ((s << 1) | 1) & m;
                if (ws[r] != null) {
                    int probes = 0;
                    int step = n <= 4 ? 2 : ((n >>> 1) & EVENMASK) + 2;
                    while (true) {
                        r = (r + step) & m;
                        if (ws[r] == null) {
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
                w.poolIndex = (short) r;
                w.eventCount = r;
                ws[r] = w;
            }
            wt.setName(this.workerNamePrefix.concat(Integer.toString(w.poolIndex >>> 1)));
            return w;
        } finally {
            if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
                releasePlock(nps);
            }
        }
    }

    final void deregisterWorker(ForkJoinWorkerThread wt, Throwable ex) {
        Unsafe unsafe;
        long j;
        long c;
        int e;
        WorkQueue v;
        Unsafe unsafe2;
        long j2;
        long sc;
        int ps;
        int idx;
        WorkQueue[] ws;
        WorkQueue w = null;
        if (wt != null && (w = wt.workQueue) != null) {
            w.qlock = -1;
            do {
                unsafe2 = U;
                j2 = STEALCOUNT;
                sc = this.stealCount;
            } while (!unsafe2.compareAndSwapLong(this, j2, sc, ((long) w.nsteals) + sc));
            int ps2 = this.plock;
            if ((ps2 & 2) == 0) {
                int ps3 = ps2 + 2;
                if (!U.compareAndSwapInt(this, PLOCK, ps2, ps3)) {
                    ps = acquirePlock();
                    int nps = (Integer.MIN_VALUE & ps) | ((ps + 2) & Integer.MAX_VALUE);
                    try {
                        idx = w.poolIndex;
                        ws = this.workQueues;
                        if (ws != null && idx >= 0 && idx < ws.length && ws[idx] == w) {
                            ws[idx] = null;
                        }
                    } finally {
                        if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
                            releasePlock(nps);
                        }
                    }
                } else {
                    ps = ps3;
                    int nps2 = (Integer.MIN_VALUE & ps) | ((ps + 2) & Integer.MAX_VALUE);
                    idx = w.poolIndex;
                    ws = this.workQueues;
                    if (ws != null) {
                        ws[idx] = null;
                    }
                }
            } else {
                ps = acquirePlock();
                int nps22 = (Integer.MIN_VALUE & ps) | ((ps + 2) & Integer.MAX_VALUE);
                idx = w.poolIndex;
                ws = this.workQueues;
                if (ws != null) {
                }
            }
        }
        do {
            unsafe = U;
            j = CTL;
            c = this.ctl;
        } while (!unsafe.compareAndSwapLong(this, j, c, ((c - AC_UNIT) & AC_MASK) | ((c - TC_UNIT) & TC_MASK) | (4294967295L & c)));
        if (!tryTerminate(false, false) && w != null && w.array != null) {
            w.cancelAll();
            while (true) {
                long c2 = this.ctl;
                int u = (int) (c2 >>> 32);
                if (u >= 0 || (e = (int) c2) < 0) {
                    break;
                }
                if (e > 0) {
                    WorkQueue[] ws2 = this.workQueues;
                    if (ws2 == null) {
                        break;
                    }
                    int i = e & 65535;
                    if (i >= ws2.length || (v = ws2[i]) == null) {
                        break;
                    }
                    long nc = ((long) (v.nextWait & Integer.MAX_VALUE)) | (((long) (65536 + u)) << 32);
                    if (v.eventCount != (Integer.MIN_VALUE | e)) {
                        break;
                    }
                    if (U.compareAndSwapLong(this, CTL, c2, nc)) {
                        v.eventCount = (65536 + e) & Integer.MAX_VALUE;
                        Thread p = v.parker;
                        if (p != null) {
                            U.unpark(p);
                        }
                    }
                } else if (((short) u) < 0) {
                    tryAddWorker();
                }
            }
        }
        if (ex == null) {
            ForkJoinTask.helpExpungeStaleExceptions();
        } else {
            ForkJoinTask.rethrow(ex);
        }
    }

    static final class Submitter {
        int seed;

        Submitter(int s) {
            this.seed = s;
        }
    }

    final void externalPush(ForkJoinTask<?> task) {
        int m;
        int am;
        int s;
        int n;
        Submitter z = submitters.get();
        int ps = this.plock;
        WorkQueue[] ws = this.workQueues;
        if (z != null && ps > 0 && ws != null && ws.length - 1 >= 0) {
            int r = z.seed;
            WorkQueue q = ws[m & r & 126];
            if (q != null && r != 0 && U.compareAndSwapInt(q, QLOCK, 0, 1)) {
                ForkJoinTask<?>[] a = q.array;
                if (a != null && a.length - 1 > (n = (s = q.top) - q.base)) {
                    int j = ((am & s) << ASHIFT) + ABASE;
                    U.putOrderedObject(a, j, task);
                    q.top = s + 1;
                    q.qlock = 0;
                    if (n <= 1) {
                        signalWork(ws, q);
                        return;
                    }
                    return;
                }
                q.qlock = 0;
            }
        }
        fullExternalPush(task);
    }

    private void fullExternalPush(ForkJoinTask<?> task) {
        int ps;
        WorkQueue[] ws;
        int nps;
        WorkQueue[] ws2;
        int m;
        int ps2;
        WorkQueue[] ws3;
        int nps2;
        int r = 0;
        Submitter z = submitters.get();
        while (true) {
            if (z == null) {
                Unsafe unsafe = U;
                long j = INDEXSEED;
                int r2 = this.indexSeed;
                int r3 = r2 + SEED_INCREMENT;
                if (!unsafe.compareAndSwapInt(this, j, r2, r3) || r3 == 0) {
                    r = r3;
                } else {
                    ThreadLocal<Submitter> threadLocal = submitters;
                    z = new Submitter(r3);
                    threadLocal.set(z);
                    r = r3;
                }
            } else if (r == 0) {
                int r4 = z.seed;
                int r5 = r4 ^ (r4 << 13);
                int r6 = r5 ^ (r5 >>> 17);
                r = r6 ^ (r6 << 5);
                z.seed = r;
            }
            int ps3 = this.plock;
            if (ps3 < 0) {
                throw new RejectedExecutionException();
            }
            if (ps3 == 0 || (ws2 = this.workQueues) == null || ws2.length - 1 < 0) {
                int p = this.parallelism;
                int n = p > 1 ? p - 1 : 1;
                int n2 = n | (n >>> 1);
                int n3 = n2 | (n2 >>> 2);
                int n4 = n3 | (n3 >>> 4);
                int n5 = n4 | (n4 >>> 8);
                int n6 = ((n5 | (n5 >>> 16)) + 1) << 1;
                WorkQueue[] ws4 = this.workQueues;
                WorkQueue[] nws = (ws4 == null || ws4.length == 0) ? new WorkQueue[n6] : null;
                int ps4 = this.plock;
                if ((ps4 & 2) == 0) {
                    int ps5 = ps4 + 2;
                    if (!U.compareAndSwapInt(this, PLOCK, ps4, ps5)) {
                        ps = acquirePlock();
                        ws = this.workQueues;
                        if ((ws == null || ws.length == 0) && nws != null) {
                            this.workQueues = nws;
                        }
                        nps = (Integer.MIN_VALUE & ps) | ((ps + 2) & Integer.MAX_VALUE);
                        if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
                            releasePlock(nps);
                        }
                    } else {
                        ps = ps5;
                        ws = this.workQueues;
                        if (ws == null) {
                            this.workQueues = nws;
                            nps = (Integer.MIN_VALUE & ps) | ((ps + 2) & Integer.MAX_VALUE);
                            if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
                            }
                        } else {
                            this.workQueues = nws;
                            nps = (Integer.MIN_VALUE & ps) | ((ps + 2) & Integer.MAX_VALUE);
                            if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
                            }
                        }
                    }
                } else {
                    ps = acquirePlock();
                    ws = this.workQueues;
                    if (ws == null) {
                    }
                }
            } else {
                int k = r & m & 126;
                WorkQueue q = ws2[k];
                if (q != null) {
                    if (q.qlock == 0 && U.compareAndSwapInt(q, QLOCK, 0, 1)) {
                        ForkJoinTask<?>[] a = q.array;
                        int s = q.top;
                        boolean submitted = false;
                        if (a != null) {
                            try {
                                if (a.length <= (s + 1) - q.base) {
                                    a = q.growArray();
                                    if (a != null) {
                                        int j2 = (((a.length - 1) & s) << ASHIFT) + ABASE;
                                        U.putOrderedObject(a, j2, task);
                                        q.top = s + 1;
                                        submitted = true;
                                    }
                                    if (submitted) {
                                        signalWork(ws2, q);
                                        return;
                                    }
                                }
                            } finally {
                                q.qlock = 0;
                            }
                        }
                    }
                    r = 0;
                } else if ((this.plock & 2) == 0) {
                    WorkQueue q2 = new WorkQueue(this, null, -1, r);
                    q2.poolIndex = (short) k;
                    int ps6 = this.plock;
                    if ((ps6 & 2) == 0) {
                        int ps7 = ps6 + 2;
                        if (!U.compareAndSwapInt(this, PLOCK, ps6, ps7)) {
                            ps2 = acquirePlock();
                            ws3 = this.workQueues;
                            if (ws3 != null && k < ws3.length && ws3[k] == null) {
                                ws3[k] = q2;
                            }
                            nps2 = (Integer.MIN_VALUE & ps2) | ((ps2 + 2) & Integer.MAX_VALUE);
                            if (!U.compareAndSwapInt(this, PLOCK, ps2, nps2)) {
                                releasePlock(nps2);
                            }
                        } else {
                            ps2 = ps7;
                            ws3 = this.workQueues;
                            if (ws3 != null) {
                                ws3[k] = q2;
                            }
                            nps2 = (Integer.MIN_VALUE & ps2) | ((ps2 + 2) & Integer.MAX_VALUE);
                            if (!U.compareAndSwapInt(this, PLOCK, ps2, nps2)) {
                            }
                        }
                    } else {
                        ps2 = acquirePlock();
                        ws3 = this.workQueues;
                        if (ws3 != null) {
                        }
                        nps2 = (Integer.MIN_VALUE & ps2) | ((ps2 + 2) & Integer.MAX_VALUE);
                        if (!U.compareAndSwapInt(this, PLOCK, ps2, nps2)) {
                        }
                    }
                } else {
                    r = 0;
                }
            }
        }
    }

    final void incrementActiveCount() {
        Unsafe unsafe;
        long j;
        long c;
        do {
            unsafe = U;
            j = CTL;
            c = this.ctl;
        } while (!unsafe.compareAndSwapLong(this, j, c, (281474976710655L & c) | ((AC_MASK & c) + AC_UNIT)));
    }

    final void signalWork(WorkQueue[] ws, WorkQueue q) {
        int i;
        WorkQueue w;
        while (true) {
            long c = this.ctl;
            int u = (int) (c >>> 32);
            if (u < 0) {
                int e = (int) c;
                if (e <= 0) {
                    if (((short) u) < 0) {
                        tryAddWorker();
                        return;
                    }
                    return;
                }
                if (ws != null && ws.length > (i = e & 65535) && (w = ws[i]) != null) {
                    long nc = ((long) (w.nextWait & Integer.MAX_VALUE)) | (((long) (65536 + u)) << 32);
                    int ne = (65536 + e) & Integer.MAX_VALUE;
                    if (w.eventCount == (Integer.MIN_VALUE | e) && U.compareAndSwapLong(this, CTL, c, nc)) {
                        w.eventCount = ne;
                        Thread p = w.parker;
                        if (p != null) {
                            U.unpark(p);
                            return;
                        }
                        return;
                    }
                    if (q != null && q.base >= q.top) {
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

    final void runWorker(WorkQueue w) {
        w.growArray();
        int r = w.hint;
        while (scan(w, r) == 0) {
            int r2 = r ^ (r << 13);
            int r3 = r2 ^ (r2 >>> 17);
            r = r3 ^ (r3 << 5);
        }
    }

    private final int scan(WorkQueue w, int r) {
        int m;
        ForkJoinTask<?>[] a;
        long c = this.ctl;
        WorkQueue[] ws = this.workQueues;
        if (ws != null && ws.length - 1 >= 0 && w != null) {
            int j = m + m + 1;
            int ec = w.eventCount;
            while (true) {
                WorkQueue q = ws[(r - j) & m];
                if (q != null) {
                    int b = q.base;
                    if (b - q.top < 0 && (a = q.array) != null) {
                        long i = (((a.length - 1) & b) << ASHIFT) + ABASE;
                        ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(a, i);
                        if (t != null) {
                            if (ec < 0) {
                                helpRelease(c, ws, w, q, b);
                            } else if (q.base == b && U.compareAndSwapObject(a, i, t, null)) {
                                U.putOrderedInt(q, QBASE, b + 1);
                                if ((b + 1) - q.top < 0) {
                                    signalWork(ws, q);
                                }
                                w.runTask(t);
                            }
                        }
                    } else {
                        j--;
                        if (j < 0) {
                            int e = (int) c;
                            if ((ec | e) < 0) {
                                return awaitWork(w, c, ec);
                            }
                            if (this.ctl == c) {
                                long nc = ((long) ec) | ((c - AC_UNIT) & (-4294967296L));
                                w.nextWait = e;
                                w.eventCount = Integer.MIN_VALUE | ec;
                                if (!U.compareAndSwapLong(this, CTL, c, nc)) {
                                    w.eventCount = ec;
                                }
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    private final int awaitWork(WorkQueue w, long c, int ec) {
        long deadline;
        long parkTime;
        Unsafe unsafe;
        long j;
        long sc;
        int stat = w.qlock;
        if (stat < 0 || w.eventCount != ec || this.ctl != c || Thread.interrupted()) {
            return stat;
        }
        int e = (int) c;
        int u = (int) (c >>> 32);
        int d = (u >> 16) + this.parallelism;
        if (e < 0 || (d <= 0 && tryTerminate(false, false))) {
            w.qlock = -1;
            return -1;
        }
        int ns = w.nsteals;
        if (ns != 0) {
            w.nsteals = 0;
            do {
                unsafe = U;
                j = STEALCOUNT;
                sc = this.stealCount;
            } while (!unsafe.compareAndSwapLong(this, j, sc, ((long) ns) + sc));
            return stat;
        }
        long pc = (d > 0 || ec != (Integer.MIN_VALUE | e)) ? 0L : ((long) (w.nextWait & Integer.MAX_VALUE)) | (((long) (65536 + u)) << 32);
        if (pc != 0) {
            int dc = -((short) (c >>> 32));
            parkTime = dc < 0 ? FAST_IDLE_TIMEOUT : ((long) (dc + 1)) * IDLE_TIMEOUT;
            deadline = (System.nanoTime() + parkTime) - TIMEOUT_SLOP;
        } else {
            deadline = 0;
            parkTime = 0;
        }
        if (w.eventCount != ec || this.ctl != c) {
            return stat;
        }
        Thread wt = Thread.currentThread();
        U.putObject(wt, PARKBLOCKER, this);
        w.parker = wt;
        if (w.eventCount == ec && this.ctl == c) {
            U.park(false, parkTime);
        }
        w.parker = null;
        U.putObject(wt, PARKBLOCKER, null);
        if (parkTime == 0 || this.ctl != c || deadline - System.nanoTime() > 0 || !U.compareAndSwapLong(this, CTL, c, pc)) {
            return stat;
        }
        w.qlock = -1;
        return -1;
    }

    private final void helpRelease(long c, WorkQueue[] ws, WorkQueue w, WorkQueue q, int b) {
        int e;
        int i;
        WorkQueue v;
        if (w != null && w.eventCount < 0 && (e = (int) c) > 0 && ws != null && ws.length > (i = e & 65535) && (v = ws[i]) != null && this.ctl == c) {
            long nc = ((long) (v.nextWait & Integer.MAX_VALUE)) | (((long) (((int) (c >>> 32)) + 65536)) << 32);
            int ne = (65536 + e) & Integer.MAX_VALUE;
            if (q != null && q.base == b && w.eventCount < 0 && v.eventCount == (Integer.MIN_VALUE | e) && U.compareAndSwapLong(this, CTL, c, nc)) {
                v.eventCount = ne;
                Thread p = v.parker;
                if (p != null) {
                    U.unpark(p);
                }
            }
        }
    }

    private int tryHelpStealer(WorkQueue joiner, ForkJoinTask<?> task) {
        int m;
        ForkJoinTask<?>[] forkJoinTaskArr;
        int stat = 0;
        int steps = 0;
        if (task == null || joiner == null || joiner.base - joiner.top < 0) {
            return 0;
        }
        while (true) {
            ForkJoinTask<?> subtask = task;
            WorkQueue j = joiner;
            while (true) {
                int s = task.status;
                if (s < 0) {
                    return s;
                }
                WorkQueue[] ws = this.workQueues;
                if (ws != null && ws.length - 1 > 0) {
                    int h = (j.hint | 1) & m;
                    WorkQueue v = ws[h];
                    if (v == null || v.currentSteal != subtask) {
                        do {
                            h = (h + 2) & m;
                            if ((h & 15) != 1 || (subtask.status >= 0 && j.currentJoin == subtask)) {
                                v = ws[h];
                                if (v != null && v.currentSteal == subtask) {
                                    j.hint = h;
                                    while (subtask.status >= 0) {
                                        int b = v.base;
                                        if (b - v.top < 0 && (forkJoinTaskArr = v.array) != null) {
                                            int i = (((forkJoinTaskArr.length - 1) & b) << ASHIFT) + ABASE;
                                            ForkJoinTask<?> t = (ForkJoinTask) U.getObjectVolatile(forkJoinTaskArr, i);
                                            if (subtask.status < 0 || j.currentJoin != subtask || v.currentSteal != subtask) {
                                                break;
                                            }
                                            stat = 1;
                                            if (v.base == b) {
                                                if (t == null) {
                                                    return 1;
                                                }
                                                if (U.compareAndSwapObject(forkJoinTaskArr, i, t, null)) {
                                                    U.putOrderedInt(v, QBASE, b + 1);
                                                    ForkJoinTask<?> ps = joiner.currentSteal;
                                                    int jt = joiner.top;
                                                    do {
                                                        joiner.currentSteal = t;
                                                        t.doExec();
                                                        if (task.status < 0 || joiner.top == jt) {
                                                            break;
                                                        }
                                                        t = joiner.pop();
                                                    } while (t != null);
                                                    joiner.currentSteal = ps;
                                                    return 1;
                                                }
                                            }
                                        } else {
                                            ForkJoinTask<?> next = v.currentJoin;
                                            if (subtask.status < 0 || j.currentJoin != subtask || v.currentSteal != subtask) {
                                                break;
                                            }
                                            if (next == null) {
                                                return stat;
                                            }
                                            steps++;
                                            if (steps != 64) {
                                                subtask = next;
                                                j = v;
                                            } else {
                                                return stat;
                                            }
                                        }
                                    }
                                }
                            }
                        } while (h != h);
                        return stat;
                    }
                    while (subtask.status >= 0) {
                    }
                } else {
                    return stat;
                }
            }
        }
    }

    private int helpComplete(WorkQueue joiner, CountedCompleter<?> task) {
        int m;
        int s = 0;
        WorkQueue[] ws = this.workQueues;
        if (ws != null && ws.length - 1 >= 0 && joiner != null && task != null) {
            int j = joiner.poolIndex;
            int scans = m + m + 1;
            long c = 0;
            int k = scans;
            while (true) {
                s = task.status;
                if (s < 0) {
                    break;
                }
                if (joiner.internalPopAndExecCC(task)) {
                    k = scans;
                } else {
                    s = task.status;
                    if (s < 0) {
                        break;
                    }
                    WorkQueue q = ws[j & m];
                    if (q != null && q.pollAndExecCC(task)) {
                        k = scans;
                    } else {
                        k--;
                        if (k < 0) {
                            long c2 = this.ctl;
                            if (c == c2) {
                                break;
                            }
                            k = scans;
                            c = c2;
                        } else {
                            continue;
                        }
                    }
                }
                j += 2;
            }
        }
        return s;
    }

    final boolean tryCompensate(long c) {
        int m;
        WorkQueue[] ws = this.workQueues;
        int pc = this.parallelism;
        int e = (int) c;
        if (ws != null && ws.length - 1 >= 0 && e >= 0 && this.ctl == c) {
            WorkQueue w = ws[e & m];
            if (e != 0 && w != null) {
                long nc = ((long) (w.nextWait & Integer.MAX_VALUE)) | ((-4294967296L) & c);
                int ne = (65536 + e) & Integer.MAX_VALUE;
                if (w.eventCount == (Integer.MIN_VALUE | e) && U.compareAndSwapLong(this, CTL, c, nc)) {
                    w.eventCount = ne;
                    Thread p = w.parker;
                    if (p != null) {
                        U.unpark(p);
                    }
                    return true;
                }
            } else {
                int tc = (short) (c >>> 32);
                if (tc >= 0 && ((int) (c >> 48)) + pc > 1) {
                    long nc2 = ((c - AC_UNIT) & AC_MASK) | (281474976710655L & c);
                    if (U.compareAndSwapLong(this, CTL, c, nc2)) {
                        return true;
                    }
                } else if (tc + pc < MAX_CAP) {
                    long nc3 = ((TC_UNIT + c) & TC_MASK) | ((-281470681743361L) & c);
                    if (U.compareAndSwapLong(this, CTL, c, nc3)) {
                        Throwable ex = null;
                        ForkJoinWorkerThread wt = null;
                        try {
                            ForkJoinWorkerThreadFactory fac = this.factory;
                            if (fac != null && (wt = fac.newThread(this)) != null) {
                                wt.start();
                                return true;
                            }
                        } catch (Throwable rex) {
                            ex = rex;
                        }
                        deregisterWorker(wt, ex);
                    }
                }
            }
        }
        return false;
    }

    final int awaitJoin(WorkQueue joiner, ForkJoinTask<?> task) {
        Unsafe unsafe;
        long j;
        long c;
        int s = 0;
        if (task != null && (s = task.status) >= 0 && joiner != null) {
            ForkJoinTask<?> prevJoin = joiner.currentJoin;
            joiner.currentJoin = task;
            while (joiner.tryRemoveAndExec(task) && (s = task.status) >= 0) {
            }
            if (s >= 0 && (task instanceof CountedCompleter)) {
                s = helpComplete(joiner, (CountedCompleter) task);
            }
            long cc = 0;
            while (s >= 0) {
                s = task.status;
                if (s < 0) {
                    break;
                }
                s = tryHelpStealer(joiner, task);
                if (s == 0 && (s = task.status) >= 0) {
                    if (!tryCompensate(cc)) {
                        cc = this.ctl;
                    } else {
                        if (task.trySetSignal() && (s = task.status) >= 0) {
                            synchronized (task) {
                                if (task.status >= 0) {
                                    try {
                                        task.wait();
                                    } catch (InterruptedException e) {
                                    }
                                } else {
                                    task.notifyAll();
                                }
                            }
                        }
                        do {
                            unsafe = U;
                            j = CTL;
                            c = this.ctl;
                        } while (!unsafe.compareAndSwapLong(this, j, c, (281474976710655L & c) | ((AC_MASK & c) + AC_UNIT)));
                    }
                }
            }
            joiner.currentJoin = prevJoin;
        }
        return s;
    }

    final void helpJoinOnce(WorkQueue joiner, ForkJoinTask<?> task) {
        int s;
        if (joiner != null && task != null && (s = task.status) >= 0) {
            ForkJoinTask<?> prevJoin = joiner.currentJoin;
            joiner.currentJoin = task;
            while (joiner.tryRemoveAndExec(task) && (s = task.status) >= 0) {
            }
            if (s >= 0) {
                if (task instanceof CountedCompleter) {
                    helpComplete(joiner, (CountedCompleter) task);
                }
                while (task.status >= 0 && tryHelpStealer(joiner, task) > 0) {
                }
            }
            joiner.currentJoin = prevJoin;
        }
    }

    private WorkQueue findNonEmptyStealQueue() {
        int ps;
        int m;
        int r = ThreadLocalRandom.current().nextInt();
        do {
            ps = this.plock;
            WorkQueue[] ws = this.workQueues;
            if (ws != null && ws.length - 1 >= 0) {
                for (int j = (m + 1) << 2; j >= 0; j--) {
                    WorkQueue q = ws[(((r - j) << 1) | 1) & m];
                    if (q != null && q.base - q.top < 0) {
                        return q;
                    }
                }
            }
        } while (this.plock != ps);
        return null;
    }

    final void helpQuiescePool(WorkQueue w) {
        ForkJoinTask<?> t;
        Unsafe unsafe;
        long j;
        long c;
        ForkJoinTask<?> ps = w.currentSteal;
        boolean active = true;
        while (true) {
            ForkJoinTask<?> t2 = w.nextLocalTask();
            if (t2 != null) {
                t2.doExec();
            } else {
                WorkQueue q = findNonEmptyStealQueue();
                if (q != null) {
                    if (!active) {
                        active = true;
                        do {
                            unsafe = U;
                            j = CTL;
                            c = this.ctl;
                        } while (!unsafe.compareAndSwapLong(this, j, c, (281474976710655L & c) | ((AC_MASK & c) + AC_UNIT)));
                    }
                    int b = q.base;
                    if (b - q.top < 0 && (t = q.pollAt(b)) != null) {
                        w.currentSteal = t;
                        t.doExec();
                        w.currentSteal = ps;
                    }
                } else if (active) {
                    long c2 = this.ctl;
                    long nc = (281474976710655L & c2) | ((AC_MASK & c2) - AC_UNIT);
                    if (((int) (nc >> 48)) + this.parallelism != 0) {
                        if (U.compareAndSwapLong(this, CTL, c2, nc)) {
                            active = false;
                        }
                    } else {
                        return;
                    }
                } else {
                    long c3 = this.ctl;
                    if (((int) (c3 >> 48)) + this.parallelism <= 0 && U.compareAndSwapLong(this, CTL, c3, (281474976710655L & c3) | ((AC_MASK & c3) + AC_UNIT))) {
                        return;
                    }
                }
            }
        }
    }

    final ForkJoinTask<?> nextTaskFor(WorkQueue w) {
        ForkJoinTask<?> t;
        while (true) {
            ForkJoinTask<?> t2 = w.nextLocalTask();
            if (t2 != null) {
                return t2;
            }
            WorkQueue q = findNonEmptyStealQueue();
            if (q == null) {
                return null;
            }
            int b = q.base;
            if (b - q.top < 0 && (t = q.pollAt(b)) != null) {
                return t;
            }
        }
    }

    static int getSurplusQueuedTaskCount() {
        int i = 0;
        Thread t = Thread.currentThread();
        if (!(t instanceof ForkJoinWorkerThread)) {
            return 0;
        }
        ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
        ForkJoinPool pool = wt.pool;
        int p = pool.parallelism;
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
                i = a > p4 ? 2 : a > (p4 >>> 1) ? 4 : 8;
            }
        }
        return n - i;
    }

    private boolean tryTerminate(boolean now, boolean enable) {
        WorkQueue w;
        Thread wt;
        WorkQueue[] ws;
        WorkQueue w2;
        int ps;
        int nps;
        if (this == common) {
            return false;
        }
        int ps2 = this.plock;
        if (ps2 >= 0) {
            if (!enable) {
                return false;
            }
            if ((ps2 & 2) == 0) {
                int ps3 = ps2 + 2;
                if (!U.compareAndSwapInt(this, PLOCK, ps2, ps3)) {
                    ps = acquirePlock();
                    nps = ((ps + 2) & Integer.MAX_VALUE) | Integer.MIN_VALUE;
                    if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
                        releasePlock(nps);
                    }
                } else {
                    ps = ps3;
                    nps = ((ps + 2) & Integer.MAX_VALUE) | Integer.MIN_VALUE;
                    if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
                    }
                }
            } else {
                ps = acquirePlock();
                nps = ((ps + 2) & Integer.MAX_VALUE) | Integer.MIN_VALUE;
                if (!U.compareAndSwapInt(this, PLOCK, ps, nps)) {
                }
            }
        }
        loop0: while (true) {
            long c = this.ctl;
            if ((STOP_BIT & c) != 0) {
                if (((short) (c >>> 32)) + this.parallelism <= 0) {
                    synchronized (this) {
                        notifyAll();
                    }
                }
                return true;
            }
            if (!now) {
                if (((int) (c >> 48)) + this.parallelism > 0) {
                    return false;
                }
                ws = this.workQueues;
                if (ws != null) {
                    for (int i = 0; i < ws.length; i++) {
                        w2 = ws[i];
                        if (w2 != null && (!w2.isEmpty() || ((i & 1) != 0 && w2.eventCount >= 0))) {
                            break loop0;
                        }
                    }
                }
            }
            if (U.compareAndSwapLong(this, CTL, c, c | STOP_BIT)) {
                for (int pass = 0; pass < 3; pass++) {
                    WorkQueue[] ws2 = this.workQueues;
                    if (ws2 != null) {
                        int n = ws2.length;
                        for (WorkQueue w3 : ws2) {
                            if (w3 != null) {
                                w3.qlock = -1;
                                if (pass > 0) {
                                    w3.cancelAll();
                                    if (pass > 1 && (wt = w3.owner) != null) {
                                        if (!wt.isInterrupted()) {
                                            try {
                                                wt.interrupt();
                                            } catch (Throwable th) {
                                            }
                                        }
                                        U.unpark(wt);
                                    }
                                }
                            }
                        }
                        while (true) {
                            long cc = this.ctl;
                            int e = ((int) cc) & Integer.MAX_VALUE;
                            if (e != 0) {
                                int i2 = e & 65535;
                                if (i2 >= n || i2 < 0 || (w = ws2[i2]) == null) {
                                    break;
                                }
                                long nc = ((long) (w.nextWait & Integer.MAX_VALUE)) | ((AC_UNIT + cc) & AC_MASK) | (281472829227008L & cc);
                                if (w.eventCount == (Integer.MIN_VALUE | e) && U.compareAndSwapLong(this, CTL, cc, nc)) {
                                    w.eventCount = (65536 + e) & Integer.MAX_VALUE;
                                    w.qlock = -1;
                                    Thread p = w.parker;
                                    if (p != null) {
                                        U.unpark(p);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        signalWork(ws, w2);
        return false;
    }

    static WorkQueue commonSubmitterQueue() {
        ForkJoinPool p;
        WorkQueue[] ws;
        int m;
        Submitter z = submitters.get();
        if (z == null || (p = common) == null || (ws = p.workQueues) == null || ws.length - 1 < 0) {
            return null;
        }
        return ws[z.seed & m & 126];
    }

    final boolean tryExternalUnpush(ForkJoinTask<?> task) {
        int m;
        WorkQueue joiner;
        ForkJoinTask<?>[] a;
        Submitter z = submitters.get();
        WorkQueue[] ws = this.workQueues;
        boolean popped = false;
        if (z != null && ws != null && ws.length - 1 >= 0 && (joiner = ws[z.seed & m & 126]) != null) {
            int i = joiner.base;
            int s = joiner.top;
            if (i != s && (a = joiner.array) != null) {
                long j = (((a.length - 1) & (s - 1)) << ASHIFT) + ABASE;
                if (U.getObject(a, j) == task && U.compareAndSwapInt(joiner, QLOCK, 0, 1)) {
                    if (joiner.top == s && joiner.array == a && U.compareAndSwapObject(a, j, task, null)) {
                        joiner.top = s - 1;
                        popped = true;
                    }
                    joiner.qlock = 0;
                }
            }
        }
        return popped;
    }

    final int externalHelpComplete(CountedCompleter<?> task) {
        int m;
        Submitter z = submitters.get();
        WorkQueue[] ws = this.workQueues;
        int s = 0;
        if (z != null && ws != null && ws.length - 1 >= 0) {
            int j = z.seed;
            WorkQueue joiner = ws[j & m & 126];
            if (joiner != null && task != null) {
                int scans = m + m + 1;
                long c = 0;
                int j2 = j | 1;
                int k = scans;
                while (true) {
                    s = task.status;
                    if (s < 0) {
                        break;
                    }
                    if (joiner.externalPopAndExecCC(task)) {
                        k = scans;
                    } else {
                        s = task.status;
                        if (s < 0) {
                            break;
                        }
                        WorkQueue q = ws[j2 & m];
                        if (q != null && q.pollAndExecCC(task)) {
                            k = scans;
                        } else {
                            k--;
                            if (k < 0) {
                                long c2 = this.ctl;
                                if (c == c2) {
                                    break;
                                }
                                k = scans;
                                c = c2;
                            } else {
                                continue;
                            }
                        }
                    }
                    j2 += 2;
                }
            }
        }
        return s;
    }

    public ForkJoinPool() {
        this(Math.min(MAX_CAP, Runtime.getRuntime().availableProcessors()), defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int parallelism) {
        this(parallelism, defaultForkJoinWorkerThreadFactory, null, false);
    }

    public ForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, Thread.UncaughtExceptionHandler handler, boolean asyncMode) {
        this(checkParallelism(parallelism), checkFactory(factory), handler, asyncMode ? 1 : 0, "ForkJoinPool-" + nextPoolId() + "-worker-");
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
        this.mode = (short) mode;
        this.parallelism = (short) parallelism;
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
        externalPush(task);
        return task.join();
    }

    public void execute(ForkJoinTask<?> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        externalPush(task);
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
        externalPush(job);
    }

    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        externalPush(task);
        return task;
    }

    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        ForkJoinTask<T> job = new ForkJoinTask.AdaptedCallable<>(task);
        externalPush(job);
        return job;
    }

    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        ForkJoinTask<T> job = new ForkJoinTask.AdaptedRunnable<>(task, result);
        externalPush(job);
        return job;
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
        externalPush(job);
        return job;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        try {
            for (Callable<T> t : tasks) {
                ForkJoinTask<T> f = new ForkJoinTask.AdaptedCallable<>(t);
                futures.add(f);
                externalPush(f);
            }
            int size = futures.size();
            for (int i = 0; i < size; i++) {
                ((ForkJoinTask) futures.get(i)).quietlyJoin();
            }
            if (1 == 0) {
                int size2 = futures.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    futures.get(i2).cancel(false);
                }
            }
            return futures;
        } catch (Throwable th) {
            if (0 != 0) {
                throw th;
            }
            int size3 = futures.size();
            for (int i3 = 0; i3 < size3; i3++) {
                futures.get(i3).cancel(false);
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
        int par = this.parallelism;
        if (par > 0) {
            return par;
        }
        return 1;
    }

    public static int getCommonPoolParallelism() {
        return commonParallelism;
    }

    public int getPoolSize() {
        return this.parallelism + ((short) (this.ctl >>> 32));
    }

    public boolean getAsyncMode() {
        return this.mode == 1;
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
        int r = this.parallelism + ((int) (this.ctl >> 48));
        if (r <= 0) {
            return 0;
        }
        return r;
    }

    public boolean isQuiescent() {
        return this.parallelism + ((int) (this.ctl >> 48)) <= 0;
    }

    public long getStealCount() {
        long count = this.stealCount;
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
        }
        return false;
    }

    protected ForkJoinTask<?> pollSubmission() {
        ForkJoinTask<?> t;
        WorkQueue[] ws = this.workQueues;
        if (ws != null) {
            for (int i = 0; i < ws.length; i += 2) {
                WorkQueue w = ws[i];
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
        String level;
        long qt = 0;
        long qs = 0;
        int rc = 0;
        long st = this.stealCount;
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
        int pc = this.parallelism;
        int tc = pc + ((short) (c >>> 32));
        int ac = pc + ((int) (c >> 48));
        if (ac < 0) {
            ac = 0;
        }
        if ((STOP_BIT & c) != 0) {
            level = tc == 0 ? "Terminated" : "Terminating";
        } else {
            level = this.plock < 0 ? "Shutting down" : "Running";
        }
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
        long c = this.ctl;
        return (STOP_BIT & c) != 0 && ((short) ((int) (c >>> 32))) + this.parallelism <= 0;
    }

    public boolean isTerminating() {
        long c = this.ctl;
        return (STOP_BIT & c) != 0 && ((short) ((int) (c >>> 32))) + this.parallelism > 0;
    }

    @Override
    public boolean isShutdown() {
        return this.plock < 0;
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
        int m;
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
        while (!isQuiescent() && (ws = this.workQueues) != null && ws.length - 1 >= 0) {
            if (!found) {
                if (System.nanoTime() - startTime > nanos) {
                    return false;
                }
                Thread.yield();
            }
            found = false;
            int j = (m + 1) << 2;
            int r2 = r;
            while (true) {
                if (j < 0) {
                    r = r2;
                    break;
                }
                r = r2 + 1;
                WorkQueue q = ws[r2 & m];
                if (q != null) {
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
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            ForkJoinPool p = ((ForkJoinWorkerThread) t).pool;
            while (!blocker.isReleasable()) {
                if (p.tryCompensate(p.ctl)) {
                    while (!blocker.isReleasable() && !blocker.block()) {
                        try {
                        } finally {
                            p.incrementActiveCount();
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
        try {
            U = Unsafe.getUnsafe();
            CTL = U.objectFieldOffset(ForkJoinPool.class.getDeclaredField("ctl"));
            STEALCOUNT = U.objectFieldOffset(ForkJoinPool.class.getDeclaredField("stealCount"));
            PLOCK = U.objectFieldOffset(ForkJoinPool.class.getDeclaredField("plock"));
            INDEXSEED = U.objectFieldOffset(ForkJoinPool.class.getDeclaredField("indexSeed"));
            PARKBLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
            QBASE = U.objectFieldOffset(WorkQueue.class.getDeclaredField("base"));
            QLOCK = U.objectFieldOffset(WorkQueue.class.getDeclaredField("qlock"));
            ABASE = U.arrayBaseOffset(ForkJoinTask[].class);
            int scale = U.arrayIndexScale(ForkJoinTask[].class);
            if (((scale - 1) & scale) != 0) {
                throw new Error("data type scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
            submitters = new ThreadLocal<>();
            defaultForkJoinWorkerThreadFactory = new DefaultForkJoinWorkerThreadFactory();
            modifyThreadPermission = new RuntimePermission("modifyThread");
            common = (ForkJoinPool) AccessController.doPrivileged(new PrivilegedAction<ForkJoinPool>() {
                @Override
                public ForkJoinPool run() {
                    return ForkJoinPool.makeCommonPool();
                }
            });
            int par = common.parallelism;
            if (par <= 0) {
                par = 1;
            }
            commonParallelism = par;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static ForkJoinPool makeCommonPool() {
        int parallelism = -1;
        ForkJoinWorkerThreadFactory factory = defaultForkJoinWorkerThreadFactory;
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
        if (parallelism < 0 && Runtime.getRuntime().availableProcessors() - 1 < 0) {
            parallelism = 0;
        }
        if (parallelism > MAX_CAP) {
            parallelism = MAX_CAP;
        }
        return new ForkJoinPool(parallelism, factory, handler, 0, "ForkJoinPool.commonPool-worker-");
    }
}
