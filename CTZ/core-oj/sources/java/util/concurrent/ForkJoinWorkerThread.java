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

    protected ForkJoinWorkerThread(ForkJoinPool forkJoinPool) {
        super("aForkJoinWorkerThread");
        this.pool = forkJoinPool;
        this.workQueue = forkJoinPool.registerWorker(this);
    }

    ForkJoinWorkerThread(ForkJoinPool forkJoinPool, ThreadGroup threadGroup, AccessControlContext accessControlContext) {
        super(threadGroup, null, "aForkJoinWorkerThread");
        U.putOrderedObject(this, INHERITEDACCESSCONTROLCONTEXT, accessControlContext);
        eraseThreadLocals();
        this.pool = forkJoinPool;
        this.workQueue = forkJoinPool.registerWorker(this);
    }

    public ForkJoinPool getPool() {
        return this.pool;
    }

    public int getPoolIndex() {
        return this.workQueue.getPoolIndex();
    }

    protected void onStart() {
    }

    protected void onTermination(Throwable th) {
    }

    @Override
    public void run() {
        if (this.workQueue.array == null) {
            Throwable th = null;
            try {
                onStart();
                this.pool.runWorker(this.workQueue);
                try {
                    onTermination(null);
                } catch (Throwable th2) {
                    this.pool.deregisterWorker(this, null);
                    throw th2;
                }
            } catch (Throwable th3) {
                try {
                    onTermination(null);
                } catch (Throwable th4) {
                    this.pool.deregisterWorker(this, null);
                    throw th4;
                }
                this.pool.deregisterWorker(this, th);
                throw th3;
            }
            this.pool.deregisterWorker(this, th);
        }
    }

    final void eraseThreadLocals() {
        U.putObject(this, THREADLOCALS, null);
        U.putObject(this, INHERITABLETHREADLOCALS, null);
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

        InnocuousForkJoinWorkerThread(ForkJoinPool forkJoinPool) {
            super(forkJoinPool, innocuousThreadGroup, INNOCUOUS_ACC);
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
        public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        }

        @Override
        public void setContextClassLoader(ClassLoader classLoader) {
            throw new SecurityException("setContextClassLoader");
        }

        private static ThreadGroup createThreadGroup() {
            try {
                Unsafe unsafe = Unsafe.getUnsafe();
                long jObjectFieldOffset = unsafe.objectFieldOffset(Thread.class.getDeclaredField("group"));
                long jObjectFieldOffset2 = unsafe.objectFieldOffset(ThreadGroup.class.getDeclaredField("parent"));
                ThreadGroup threadGroup = (ThreadGroup) unsafe.getObject(Thread.currentThread(), jObjectFieldOffset);
                while (threadGroup != null) {
                    ThreadGroup threadGroup2 = (ThreadGroup) unsafe.getObject(threadGroup, jObjectFieldOffset2);
                    if (threadGroup2 != null) {
                        threadGroup = threadGroup2;
                    } else {
                        return new ThreadGroup(threadGroup, "InnocuousForkJoinWorkerThreadGroup");
                    }
                }
                throw new Error("Cannot create ThreadGroup");
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }
    }
}
