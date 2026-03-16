package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

public abstract class ForkJoinTask<V> implements Future<V>, Serializable {
    static final int CANCELLED = -1073741824;
    static final int DONE_MASK = -268435456;
    static final int EXCEPTIONAL = Integer.MIN_VALUE;
    private static final int EXCEPTION_MAP_CAPACITY = 32;
    static final int NORMAL = -268435456;
    static final int SIGNAL = 65536;
    static final int SMASK = 65535;
    private static final long STATUS;
    private static final Unsafe U;
    private static final long serialVersionUID = -7721805057305804111L;
    volatile int status;
    private static final ReentrantLock exceptionTableLock = new ReentrantLock();
    private static final ReferenceQueue<Object> exceptionTableRefQueue = new ReferenceQueue<>();
    private static final ExceptionNode[] exceptionTable = new ExceptionNode[32];

    protected abstract boolean exec();

    public abstract V getRawResult();

    protected abstract void setRawResult(V v);

    private int setCompletion(int completion) {
        int s;
        do {
            s = this.status;
            if (s < 0) {
                return s;
            }
        } while (!U.compareAndSwapInt(this, STATUS, s, s | completion));
        if ((s >>> 16) != 0) {
            synchronized (this) {
                notifyAll();
            }
        }
        return completion;
    }

    final int doExec() {
        int s = this.status;
        if (s >= 0) {
            try {
                boolean completed = exec();
                if (completed) {
                    s = setCompletion(-268435456);
                }
            } catch (Throwable rex) {
                return setExceptionalCompletion(rex);
            }
        }
        return s;
    }

    final boolean trySetSignal() {
        int s = this.status;
        return s >= 0 && U.compareAndSwapInt(this, STATUS, s, s | 65536);
    }

    private int externalAwaitDone() {
        ForkJoinPool cp = ForkJoinPool.common;
        int s = this.status;
        if (s >= 0) {
            if (cp != null) {
                if (this instanceof CountedCompleter) {
                    s = cp.externalHelpComplete((CountedCompleter) this);
                } else if (cp.tryExternalUnpush(this)) {
                    s = doExec();
                }
            }
            if (s >= 0 && (s = this.status) >= 0) {
                boolean interrupted = false;
                do {
                    if (U.compareAndSwapInt(this, STATUS, s, s | 65536)) {
                        synchronized (this) {
                            if (this.status >= 0) {
                                try {
                                    wait();
                                } catch (InterruptedException e) {
                                    interrupted = true;
                                }
                            } else {
                                notifyAll();
                            }
                        }
                    }
                    s = this.status;
                } while (s >= 0);
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return s;
    }

    private int externalInterruptibleAwaitDone() throws InterruptedException {
        ForkJoinPool cp = ForkJoinPool.common;
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (this.status >= 0 && cp != null) {
            if (this instanceof CountedCompleter) {
                cp.externalHelpComplete((CountedCompleter) this);
            } else if (cp.tryExternalUnpush(this)) {
                doExec();
            }
        }
        while (true) {
            int s = this.status;
            if (s >= 0) {
                if (U.compareAndSwapInt(this, STATUS, s, s | 65536)) {
                    synchronized (this) {
                        if (this.status >= 0) {
                            wait();
                        } else {
                            notifyAll();
                        }
                    }
                }
            } else {
                return s;
            }
        }
    }

    private int doJoin() {
        int s;
        int s2 = this.status;
        if (s2 < 0) {
            return s2;
        }
        Thread t = Thread.currentThread();
        if (!(t instanceof ForkJoinWorkerThread)) {
            return externalAwaitDone();
        }
        ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
        ForkJoinPool.WorkQueue w = wt.workQueue;
        return (!w.tryUnpush(this) || (s = doExec()) >= 0) ? wt.pool.awaitJoin(w, this) : s;
    }

    private int doInvoke() {
        int s = doExec();
        if (s < 0) {
            return s;
        }
        Thread t = Thread.currentThread();
        if (!(t instanceof ForkJoinWorkerThread)) {
            return externalAwaitDone();
        }
        ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
        return wt.pool.awaitJoin(wt.workQueue, this);
    }

    static final class ExceptionNode extends WeakReference<ForkJoinTask<?>> {
        final Throwable ex;
        ExceptionNode next;
        final long thrower;

        ExceptionNode(ForkJoinTask<?> task, Throwable ex, ExceptionNode next) {
            super(task, ForkJoinTask.exceptionTableRefQueue);
            this.ex = ex;
            this.next = next;
            this.thrower = Thread.currentThread().getId();
        }
    }

    final int recordExceptionalCompletion(Throwable ex) {
        int s = this.status;
        if (s >= 0) {
            int h = System.identityHashCode(this);
            ReentrantLock lock = exceptionTableLock;
            lock.lock();
            try {
                expungeStaleExceptions();
                ExceptionNode[] t = exceptionTable;
                int i = h & (t.length - 1);
                ExceptionNode e = t[i];
                while (true) {
                    if (e != null) {
                        if (e.get() == this) {
                            break;
                        }
                        e = e.next;
                    } else {
                        break;
                    }
                }
                lock.unlock();
                return setCompletion(Integer.MIN_VALUE);
            } catch (Throwable th) {
                lock.unlock();
                throw th;
            }
        }
        return s;
    }

    private int setExceptionalCompletion(Throwable ex) {
        int s = recordExceptionalCompletion(ex);
        if (((-268435456) & s) == Integer.MIN_VALUE) {
            internalPropagateException(ex);
        }
        return s;
    }

    void internalPropagateException(Throwable ex) {
    }

    static final void cancelIgnoringExceptions(ForkJoinTask<?> t) {
        if (t != null && t.status >= 0) {
            try {
                t.cancel(false);
            } catch (Throwable th) {
            }
        }
    }

    private void clearExceptionalCompletion() {
        int h = System.identityHashCode(this);
        ReentrantLock lock = exceptionTableLock;
        lock.lock();
        try {
            ExceptionNode[] t = exceptionTable;
            int i = h & (t.length - 1);
            ExceptionNode e = t[i];
            ExceptionNode pred = null;
            while (true) {
                if (e == null) {
                    break;
                }
                ExceptionNode next = e.next;
                if (e.get() == this) {
                    if (pred == null) {
                        t[i] = next;
                    } else {
                        pred.next = next;
                    }
                } else {
                    pred = e;
                    e = next;
                }
            }
            expungeStaleExceptions();
            this.status = 0;
        } finally {
            lock.unlock();
        }
    }

    private Throwable getThrowableException() {
        Throwable ex;
        if ((this.status & (-268435456)) != Integer.MIN_VALUE) {
            return null;
        }
        int h = System.identityHashCode(this);
        ReentrantLock lock = exceptionTableLock;
        lock.lock();
        try {
            expungeStaleExceptions();
            ExceptionNode[] t = exceptionTable;
            ExceptionNode e = t[(t.length - 1) & h];
            while (e != null) {
                if (e.get() == this) {
                    break;
                }
                e = e.next;
            }
            if (e == null || (ex = e.ex) == null) {
                return null;
            }
            return ex;
        } finally {
            lock.unlock();
        }
    }

    private static void expungeStaleExceptions() {
        while (true) {
            Reference<? extends Object> referencePoll = exceptionTableRefQueue.poll();
            if (referencePoll != null) {
                if (referencePoll instanceof ExceptionNode) {
                    ForkJoinTask<?> key = ((ExceptionNode) referencePoll).get();
                    ExceptionNode[] t = exceptionTable;
                    int i = System.identityHashCode(key) & (t.length - 1);
                    ExceptionNode e = t[i];
                    ExceptionNode pred = null;
                    while (true) {
                        if (e != null) {
                            ExceptionNode next = e.next;
                            if (e == referencePoll) {
                                if (pred == null) {
                                    t[i] = next;
                                } else {
                                    pred.next = next;
                                }
                            } else {
                                pred = e;
                                e = next;
                            }
                        }
                    }
                }
            } else {
                return;
            }
        }
    }

    static final void helpExpungeStaleExceptions() {
        ReentrantLock lock = exceptionTableLock;
        if (lock.tryLock()) {
            try {
                expungeStaleExceptions();
            } finally {
                lock.unlock();
            }
        }
    }

    static void rethrow(Throwable ex) {
        if (ex != null) {
            uncheckedThrow(ex);
        }
    }

    static <T extends Throwable> void uncheckedThrow(Throwable t) throws Throwable {
        throw t;
    }

    private void reportException(int s) {
        if (s == CANCELLED) {
            throw new CancellationException();
        }
        if (s == Integer.MIN_VALUE) {
            rethrow(getThrowableException());
        }
    }

    public final ForkJoinTask<V> fork() {
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            ((ForkJoinWorkerThread) t).workQueue.push(this);
        } else {
            ForkJoinPool.common.externalPush(this);
        }
        return this;
    }

    public final V join() {
        int s = doJoin() & (-268435456);
        if (s != -268435456) {
            reportException(s);
        }
        return getRawResult();
    }

    public final V invoke() {
        int s = doInvoke() & (-268435456);
        if (s != -268435456) {
            reportException(s);
        }
        return getRawResult();
    }

    public static void invokeAll(ForkJoinTask<?> t1, ForkJoinTask<?> t2) {
        t2.fork();
        int s1 = t1.doInvoke() & (-268435456);
        if (s1 != -268435456) {
            t1.reportException(s1);
        }
        int s2 = t2.doJoin() & (-268435456);
        if (s2 != -268435456) {
            t2.reportException(s2);
        }
    }

    public static void invokeAll(ForkJoinTask<?>... tasks) {
        Throwable ex = null;
        int last = tasks.length - 1;
        for (int i = last; i >= 0; i--) {
            ForkJoinTask<?> t = tasks[i];
            if (t == null) {
                if (ex == null) {
                    ex = new NullPointerException();
                }
            } else if (i != 0) {
                t.fork();
            } else if (t.doInvoke() < -268435456 && ex == null) {
                ex = t.getException();
            }
        }
        for (int i2 = 1; i2 <= last; i2++) {
            ForkJoinTask<?> t2 = tasks[i2];
            if (t2 != null) {
                if (ex != null) {
                    t2.cancel(false);
                } else if (t2.doJoin() < -268435456) {
                    ex = t2.getException();
                }
            }
        }
        if (ex != null) {
            rethrow(ex);
        }
    }

    public static <T extends ForkJoinTask<?>> Collection<T> invokeAll(Collection<T> tasks) {
        if (!(tasks instanceof RandomAccess) || !(tasks instanceof List)) {
            invokeAll((ForkJoinTask<?>[]) tasks.toArray(new ForkJoinTask[tasks.size()]));
        } else {
            List<? extends ForkJoinTask<?>> ts = (List) tasks;
            Throwable ex = null;
            int last = ts.size() - 1;
            for (int i = last; i >= 0; i--) {
                ForkJoinTask<?> t = ts.get(i);
                if (t == null) {
                    if (ex == null) {
                        ex = new NullPointerException();
                    }
                } else if (i != 0) {
                    t.fork();
                } else if (t.doInvoke() < -268435456 && ex == null) {
                    ex = t.getException();
                }
            }
            for (int i2 = 1; i2 <= last; i2++) {
                ForkJoinTask<?> t2 = ts.get(i2);
                if (t2 != null) {
                    if (ex != null) {
                        t2.cancel(false);
                    } else if (t2.doJoin() < -268435456) {
                        ex = t2.getException();
                    }
                }
            }
            if (ex != null) {
                rethrow(ex);
            }
        }
        return tasks;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return (setCompletion(CANCELLED) & (-268435456)) == CANCELLED;
    }

    @Override
    public final boolean isDone() {
        return this.status < 0;
    }

    @Override
    public final boolean isCancelled() {
        return (this.status & (-268435456)) == CANCELLED;
    }

    public final boolean isCompletedAbnormally() {
        return this.status < -268435456;
    }

    public final boolean isCompletedNormally() {
        return (this.status & (-268435456)) == -268435456;
    }

    public final Throwable getException() {
        int s = this.status & (-268435456);
        if (s >= -268435456) {
            return null;
        }
        return s == CANCELLED ? new CancellationException() : getThrowableException();
    }

    public void completeExceptionally(Throwable ex) {
        if (!(ex instanceof RuntimeException) && !(ex instanceof Error)) {
            ex = new RuntimeException(ex);
        }
        setExceptionalCompletion(ex);
    }

    public void complete(V value) {
        try {
            setRawResult(value);
            setCompletion(-268435456);
        } catch (Throwable rex) {
            setExceptionalCompletion(rex);
        }
    }

    public final void quietlyComplete() {
        setCompletion(-268435456);
    }

    @Override
    public final V get() throws ExecutionException, InterruptedException {
        Throwable ex;
        int s = (Thread.currentThread() instanceof ForkJoinWorkerThread ? doJoin() : externalInterruptibleAwaitDone()) & (-268435456);
        if (s == CANCELLED) {
            throw new CancellationException();
        }
        if (s == Integer.MIN_VALUE && (ex = getThrowableException()) != null) {
            throw new ExecutionException(ex);
        }
        return getRawResult();
    }

    @Override
    public final V get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        long ns = unit.toNanos(timeout);
        int s = this.status;
        if (s >= 0 && ns > 0) {
            long deadline = System.nanoTime() + ns;
            ForkJoinPool p = null;
            ForkJoinPool.WorkQueue w = null;
            Thread t = Thread.currentThread();
            if (t instanceof ForkJoinWorkerThread) {
                ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
                p = wt.pool;
                w = wt.workQueue;
                p.helpJoinOnce(w, this);
            } else {
                ForkJoinPool cp = ForkJoinPool.common;
                if (cp != null) {
                    if (this instanceof CountedCompleter) {
                        cp.externalHelpComplete((CountedCompleter) this);
                    } else if (cp.tryExternalUnpush(this)) {
                        doExec();
                    }
                }
            }
            boolean canBlock = false;
            boolean interrupted = false;
            while (true) {
                try {
                    s = this.status;
                    if (s < 0) {
                        break;
                    }
                    if (w != null && w.qlock < 0) {
                        cancelIgnoringExceptions(this);
                    } else if (!canBlock) {
                        if (p != null) {
                            if (p.tryCompensate(p.ctl)) {
                            }
                        }
                        canBlock = true;
                    } else {
                        long ms = TimeUnit.NANOSECONDS.toMillis(ns);
                        if (ms > 0 && U.compareAndSwapInt(this, STATUS, s, s | 65536)) {
                            synchronized (this) {
                                if (this.status >= 0) {
                                    try {
                                        wait(ms);
                                    } catch (InterruptedException e) {
                                        if (p == null) {
                                            interrupted = true;
                                        }
                                    }
                                } else {
                                    notifyAll();
                                }
                            }
                        }
                        s = this.status;
                        if (s < 0 || interrupted) {
                            break;
                        }
                        ns = deadline - System.nanoTime();
                        if (ns <= 0) {
                            break;
                        }
                    }
                } finally {
                    if (p != null && canBlock) {
                        p.incrementActiveCount();
                    }
                }
            }
            if (interrupted) {
                throw new InterruptedException();
            }
        }
        int s2 = s & (-268435456);
        if (s2 != -268435456) {
            if (s2 == CANCELLED) {
                throw new CancellationException();
            }
            if (s2 != Integer.MIN_VALUE) {
                throw new TimeoutException();
            }
            Throwable ex = getThrowableException();
            if (ex != null) {
                throw new ExecutionException(ex);
            }
        }
        return getRawResult();
    }

    public final void quietlyJoin() {
        doJoin();
    }

    public final void quietlyInvoke() {
        doInvoke();
    }

    public static void helpQuiesce() {
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
            wt.pool.helpQuiescePool(wt.workQueue);
        } else {
            ForkJoinPool.quiesceCommonPool();
        }
    }

    public void reinitialize() {
        if ((this.status & (-268435456)) == Integer.MIN_VALUE) {
            clearExceptionalCompletion();
        } else {
            this.status = 0;
        }
    }

    public static ForkJoinPool getPool() {
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            return ((ForkJoinWorkerThread) t).pool;
        }
        return null;
    }

    public static boolean inForkJoinPool() {
        return Thread.currentThread() instanceof ForkJoinWorkerThread;
    }

    public boolean tryUnfork() {
        Thread t = Thread.currentThread();
        return t instanceof ForkJoinWorkerThread ? ((ForkJoinWorkerThread) t).workQueue.tryUnpush(this) : ForkJoinPool.common.tryExternalUnpush(this);
    }

    public static int getQueuedTaskCount() {
        ForkJoinPool.WorkQueue q;
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            q = ((ForkJoinWorkerThread) t).workQueue;
        } else {
            q = ForkJoinPool.commonSubmitterQueue();
        }
        if (q == null) {
            return 0;
        }
        return q.queueSize();
    }

    public static int getSurplusQueuedTaskCount() {
        return ForkJoinPool.getSurplusQueuedTaskCount();
    }

    protected static ForkJoinTask<?> peekNextLocalTask() {
        ForkJoinPool.WorkQueue q;
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            q = ((ForkJoinWorkerThread) t).workQueue;
        } else {
            q = ForkJoinPool.commonSubmitterQueue();
        }
        if (q == null) {
            return null;
        }
        return q.peek();
    }

    protected static ForkJoinTask<?> pollNextLocalTask() {
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            return ((ForkJoinWorkerThread) t).workQueue.nextLocalTask();
        }
        return null;
    }

    protected static ForkJoinTask<?> pollTask() {
        Thread t = Thread.currentThread();
        if (!(t instanceof ForkJoinWorkerThread)) {
            return null;
        }
        ForkJoinWorkerThread wt = (ForkJoinWorkerThread) t;
        return wt.pool.nextTaskFor(wt.workQueue);
    }

    public final short getForkJoinTaskTag() {
        return (short) this.status;
    }

    public final short setForkJoinTaskTag(short tag) {
        Unsafe unsafe;
        long j;
        int s;
        do {
            unsafe = U;
            j = STATUS;
            s = this.status;
        } while (!unsafe.compareAndSwapInt(this, j, s, (65535 & tag) | ((-65536) & s)));
        return (short) s;
    }

    public final boolean compareAndSetForkJoinTaskTag(short e, short tag) {
        int s;
        do {
            s = this.status;
            if (((short) s) != e) {
                return false;
            }
        } while (!U.compareAndSwapInt(this, STATUS, s, (65535 & tag) | ((-65536) & s)));
        return true;
    }

    static final class AdaptedRunnable<T> extends ForkJoinTask<T> implements RunnableFuture<T> {
        private static final long serialVersionUID = 5232453952276885070L;
        T result;
        final Runnable runnable;

        AdaptedRunnable(Runnable runnable, T result) {
            if (runnable == null) {
                throw new NullPointerException();
            }
            this.runnable = runnable;
            this.result = result;
        }

        @Override
        public final T getRawResult() {
            return this.result;
        }

        @Override
        public final void setRawResult(T v) {
            this.result = v;
        }

        @Override
        public final boolean exec() {
            this.runnable.run();
            return true;
        }

        @Override
        public final void run() {
            invoke();
        }
    }

    static final class AdaptedRunnableAction extends ForkJoinTask<Void> implements RunnableFuture<Void> {
        private static final long serialVersionUID = 5232453952276885070L;
        final Runnable runnable;

        AdaptedRunnableAction(Runnable runnable) {
            if (runnable == null) {
                throw new NullPointerException();
            }
            this.runnable = runnable;
        }

        @Override
        public final Void getRawResult() {
            return null;
        }

        @Override
        public final void setRawResult(Void v) {
        }

        @Override
        public final boolean exec() {
            this.runnable.run();
            return true;
        }

        @Override
        public final void run() {
            invoke();
        }
    }

    static final class RunnableExecuteAction extends ForkJoinTask<Void> {
        private static final long serialVersionUID = 5232453952276885070L;
        final Runnable runnable;

        RunnableExecuteAction(Runnable runnable) {
            if (runnable == null) {
                throw new NullPointerException();
            }
            this.runnable = runnable;
        }

        @Override
        public final Void getRawResult() {
            return null;
        }

        @Override
        public final void setRawResult(Void v) {
        }

        @Override
        public final boolean exec() {
            this.runnable.run();
            return true;
        }

        @Override
        void internalPropagateException(Throwable ex) {
            rethrow(ex);
        }
    }

    static final class AdaptedCallable<T> extends ForkJoinTask<T> implements RunnableFuture<T> {
        private static final long serialVersionUID = 2838392045355241008L;
        final Callable<? extends T> callable;
        T result;

        AdaptedCallable(Callable<? extends T> callable) {
            if (callable == null) {
                throw new NullPointerException();
            }
            this.callable = callable;
        }

        @Override
        public final T getRawResult() {
            return this.result;
        }

        @Override
        public final void setRawResult(T v) {
            this.result = v;
        }

        @Override
        public final boolean exec() {
            try {
                this.result = this.callable.call();
                return true;
            } catch (Error err) {
                throw err;
            } catch (RuntimeException rex) {
                throw rex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public final void run() {
            invoke();
        }
    }

    public static ForkJoinTask<?> adapt(Runnable runnable) {
        return new AdaptedRunnableAction(runnable);
    }

    public static <T> ForkJoinTask<T> adapt(Runnable runnable, T result) {
        return new AdaptedRunnable(runnable, result);
    }

    public static <T> ForkJoinTask<T> adapt(Callable<? extends T> callable) {
        return new AdaptedCallable(callable);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeObject(getException());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        Object ex = s.readObject();
        if (ex != null) {
            setExceptionalCompletion((Throwable) ex);
        }
    }

    static {
        try {
            U = Unsafe.getUnsafe();
            STATUS = U.objectFieldOffset(ForkJoinTask.class.getDeclaredField("status"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
