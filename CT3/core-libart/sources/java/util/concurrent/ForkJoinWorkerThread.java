package java.util.concurrent;

import java.lang.Thread;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.concurrent.ForkJoinPool;
import sun.misc.Unsafe;

public class ForkJoinWorkerThread extends Thread {
    private static final long INHERITABLETHREADLOCALS;
    private static final long INHERITEDACCESSCONTROLCONTEXT;
    private static final long THREADLOCALS;
    private static final Unsafe U = Unsafe.getUnsafe();
    final ForkJoinPool pool;
    final ForkJoinPool.WorkQueue workQueue;

    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        super("aForkJoinWorkerThread");
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    ForkJoinWorkerThread(ForkJoinPool pool, ThreadGroup threadGroup, AccessControlContext acc) {
        super(threadGroup, null, "aForkJoinWorkerThread");
        U.putOrderedObject(this, INHERITEDACCESSCONTROLCONTEXT, acc);
        eraseThreadLocals();
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    public ForkJoinPool getPool() {
        return this.pool;
    }

    public int getPoolIndex() {
        return this.workQueue.getPoolIndex();
    }

    protected void onStart() {
    }

    protected void onTermination(Throwable exception) {
    }

    @Override
    public void run() {
        if (this.workQueue.array == null) {
            try {
                onStart();
                this.pool.runWorker(this.workQueue);
                try {
                    onTermination(null);
                    this.pool.deregisterWorker(this, null);
                } catch (Throwable th) {
                    this.pool.deregisterWorker(this, null);
                    throw th;
                }
            } catch (Throwable ex) {
                Throwable exception = ex;
                try {
                    onTermination(ex);
                    this.pool.deregisterWorker(this, ex);
                } catch (Throwable th2) {
                    this.pool.deregisterWorker(this, ex);
                    throw th2;
                }
            }
        }
    }

    final void eraseThreadLocals() {
        U.putObject(this, THREADLOCALS, (Object) null);
        U.putObject(this, INHERITABLETHREADLOCALS, (Object) null);
    }

    void afterTopLevelExec() {
    }

    static {
        try {
            THREADLOCALS = U.objectFieldOffset(Thread.class.getDeclaredField("threadLocals"));
            INHERITABLETHREADLOCALS = U.objectFieldOffset(Thread.class.getDeclaredField("inheritableThreadLocals"));
            INHERITEDACCESSCONTROLCONTEXT = U.objectFieldOffset(Thread.class.getDeclaredField("inheritedAccessControlContext"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    static final class InnocuousForkJoinWorkerThread extends ForkJoinWorkerThread {
        private static final ThreadGroup innocuousThreadGroup = createThreadGroup();
        private static final AccessControlContext INNOCUOUS_ACC = new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, null)});

        InnocuousForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool, innocuousThreadGroup, INNOCUOUS_ACC);
        }

        @Override
        void afterTopLevelExec() {
            eraseThreadLocals();
        }

        @Override
        public ClassLoader getContextClassLoader() {
            return ClassLoader.getSystemClassLoader();
        }

        @Override
        public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler x) {
        }

        @Override
        public void setContextClassLoader(ClassLoader cl) {
            throw new SecurityException("setContextClassLoader");
        }

        private static ThreadGroup createThreadGroup() {
            try {
                Unsafe u = Unsafe.getUnsafe();
                long tg = u.objectFieldOffset(Thread.class.getDeclaredField("group"));
                long gp = u.objectFieldOffset(ThreadGroup.class.getDeclaredField("parent"));
                ThreadGroup group = (ThreadGroup) u.getObject(Thread.currentThread(), tg);
                while (group != null) {
                    ThreadGroup parent = (ThreadGroup) u.getObject(group, gp);
                    if (parent == null) {
                        return new ThreadGroup(group, "InnocuousForkJoinWorkerThreadGroup");
                    }
                    group = parent;
                }
                throw new Error("Cannot create ThreadGroup");
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }
}
