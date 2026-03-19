package java.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
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
    private static final long serialVersionUID = -7721805057305804111L;
    volatile int status;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final ReentrantLock exceptionTableLock = new ReentrantLock();
    private static final ReferenceQueue<Object> exceptionTableRefQueue = new ReferenceQueue<>();
    private static final ExceptionNode[] exceptionTable = new ExceptionNode[32];

    protected abstract boolean exec();

    public abstract V getRawResult();

    protected abstract void setRawResult(V v);

    private int setCompletion(int i) {
        int i2;
        do {
            i2 = this.status;
            if (i2 < 0) {
                return i2;
            }
        } while (!U.compareAndSwapInt(this, STATUS, i2, i2 | i));
        if ((i2 >>> 16) != 0) {
            synchronized (this) {
                notifyAll();
            }
        }
        return i;
    }

    final int doExec() {
        int i = this.status;
        if (i >= 0) {
            try {
                if (exec()) {
                    return setCompletion(-268435456);
                }
                return i;
            } catch (Throwable th) {
                return setExceptionalCompletion(th);
            }
        }
        return i;
    }

    final void internalWait(long j) {
        int i = this.status;
        if (i >= 0 && U.compareAndSwapInt(this, STATUS, i, i | 65536)) {
            synchronized (this) {
                if (this.status >= 0) {
                    try {
                        wait(j);
                    } catch (InterruptedException e) {
                    }
                } else {
                    notifyAll();
                }
            }
        }
    }

    private int externalAwaitDone() {
        int iDoExec;
        boolean z = false;
        if (this instanceof CountedCompleter) {
            iDoExec = ForkJoinPool.common.externalHelpComplete((CountedCompleter) this, 0);
        } else {
            iDoExec = ForkJoinPool.common.tryExternalUnpush(this) ? doExec() : 0;
        }
        if (iDoExec < 0) {
            return iDoExec;
        }
        int i = this.status;
        if (i < 0) {
            return i;
        }
        int i2 = i;
        do {
            if (U.compareAndSwapInt(this, STATUS, i2, i2 | 65536)) {
                synchronized (this) {
                    if (this.status >= 0) {
                        try {
                            wait(0L);
                        } catch (InterruptedException e) {
                            z = true;
                        }
                    } else {
                        notifyAll();
                    }
                }
            }
            i2 = this.status;
        } while (i2 >= 0);
        if (z) {
            Thread.currentThread().interrupt();
        }
        return i2;
    }

    private int externalInterruptibleAwaitDone() throws InterruptedException {
        int iDoExec;
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        int i = this.status;
        if (i < 0) {
            return i;
        }
        if (this instanceof CountedCompleter) {
            iDoExec = ForkJoinPool.common.externalHelpComplete((CountedCompleter) this, 0);
        } else {
            iDoExec = ForkJoinPool.common.tryExternalUnpush(this) ? doExec() : 0;
        }
        if (iDoExec < 0) {
            return iDoExec;
        }
        while (true) {
            int i2 = this.status;
            if (i2 >= 0) {
                if (U.compareAndSwapInt(this, STATUS, i2, i2 | 65536)) {
                    synchronized (this) {
                        if (this.status >= 0) {
                            wait(0L);
                        } else {
                            notifyAll();
                        }
                    }
                }
            } else {
                return i2;
            }
        }
    }

    private int doJoin() {
        int iDoExec;
        int i = this.status;
        if (i < 0) {
            return i;
        }
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) threadCurrentThread;
            ForkJoinPool.WorkQueue workQueue = forkJoinWorkerThread.workQueue;
            if (workQueue.tryUnpush(this) && (iDoExec = doExec()) < 0) {
                return iDoExec;
            }
            return forkJoinWorkerThread.pool.awaitJoin(workQueue, this, 0L);
        }
        return externalAwaitDone();
    }

    private int doInvoke() {
        int iDoExec = doExec();
        if (iDoExec < 0) {
            return iDoExec;
        }
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) threadCurrentThread;
            return forkJoinWorkerThread.pool.awaitJoin(forkJoinWorkerThread.workQueue, this, 0L);
        }
        return externalAwaitDone();
    }

    static final class ExceptionNode extends WeakReference<ForkJoinTask<?>> {
        final Throwable ex;
        final int hashCode;
        ExceptionNode next;
        final long thrower;

        ExceptionNode(ForkJoinTask<?> forkJoinTask, Throwable th, ExceptionNode exceptionNode, ReferenceQueue<Object> referenceQueue) {
            super(forkJoinTask, referenceQueue);
            this.ex = th;
            this.next = exceptionNode;
            this.thrower = Thread.currentThread().getId();
            this.hashCode = System.identityHashCode(forkJoinTask);
        }
    }

    final int recordExceptionalCompletion(Throwable th) {
        int i = this.status;
        if (i >= 0) {
            int iIdentityHashCode = System.identityHashCode(this);
            ReentrantLock reentrantLock = exceptionTableLock;
            reentrantLock.lock();
            try {
                expungeStaleExceptions();
                ExceptionNode[] exceptionNodeArr = exceptionTable;
                int length = iIdentityHashCode & (exceptionNodeArr.length - 1);
                ExceptionNode exceptionNode = exceptionNodeArr[length];
                while (true) {
                    if (exceptionNode == null) {
                        break;
                    }
                    if (exceptionNode.get() == this) {
                        break;
                    }
                    exceptionNode = exceptionNode.next;
                }
                reentrantLock.unlock();
                return setCompletion(Integer.MIN_VALUE);
            } catch (Throwable th2) {
                reentrantLock.unlock();
                throw th2;
            }
        }
        return i;
    }

    private int setExceptionalCompletion(Throwable th) {
        int iRecordExceptionalCompletion = recordExceptionalCompletion(th);
        if (((-268435456) & iRecordExceptionalCompletion) == Integer.MIN_VALUE) {
            internalPropagateException(th);
        }
        return iRecordExceptionalCompletion;
    }

    void internalPropagateException(Throwable th) {
    }

    static final void cancelIgnoringExceptions(ForkJoinTask<?> forkJoinTask) {
        if (forkJoinTask != null && forkJoinTask.status >= 0) {
            try {
                forkJoinTask.cancel(false);
            } catch (Throwable th) {
            }
        }
    }

    private void clearExceptionalCompletion() {
        int iIdentityHashCode = System.identityHashCode(this);
        ReentrantLock reentrantLock = exceptionTableLock;
        reentrantLock.lock();
        try {
            ExceptionNode[] exceptionNodeArr = exceptionTable;
            int length = iIdentityHashCode & (exceptionNodeArr.length - 1);
            ExceptionNode exceptionNode = exceptionNodeArr[length];
            ExceptionNode exceptionNode2 = null;
            while (true) {
                if (exceptionNode == null) {
                    break;
                }
                ExceptionNode exceptionNode3 = exceptionNode.next;
                if (exceptionNode.get() != this) {
                    exceptionNode2 = exceptionNode;
                    exceptionNode = exceptionNode3;
                } else if (exceptionNode2 == null) {
                    exceptionNodeArr[length] = exceptionNode3;
                } else {
                    exceptionNode2.next = exceptionNode3;
                }
            }
            expungeStaleExceptions();
            this.status = 0;
        } finally {
            reentrantLock.unlock();
        }
    }

    private Throwable getThrowableException() {
        Throwable th;
        int iIdentityHashCode = System.identityHashCode(this);
        ReentrantLock reentrantLock = exceptionTableLock;
        reentrantLock.lock();
        try {
            expungeStaleExceptions();
            ExceptionNode[] exceptionNodeArr = exceptionTable;
            ExceptionNode exceptionNode = exceptionNodeArr[iIdentityHashCode & (exceptionNodeArr.length - 1)];
            while (exceptionNode != null) {
                if (exceptionNode.get() == this) {
                    break;
                }
                exceptionNode = exceptionNode.next;
            }
            reentrantLock.unlock();
            if (exceptionNode == null || (th = exceptionNode.ex) == null) {
                return null;
            }
            if (exceptionNode.thrower != Thread.currentThread().getId()) {
                try {
                    Constructor<?> constructor = null;
                    for (Constructor<?> constructor2 : th.getClass().getConstructors()) {
                        Class<?>[] parameterTypes = constructor2.getParameterTypes();
                        if (parameterTypes.length == 0) {
                            constructor = constructor2;
                        } else if (parameterTypes.length == 1 && parameterTypes[0] == Throwable.class) {
                            return (Throwable) constructor2.newInstance(th);
                        }
                    }
                    if (constructor != null) {
                        Throwable th2 = (Throwable) constructor.newInstance(new Object[0]);
                        th2.initCause(th);
                        return th2;
                    }
                } catch (Exception e) {
                }
            }
            return th;
        } catch (Throwable th3) {
            reentrantLock.unlock();
            throw th3;
        }
    }

    private static void expungeStaleExceptions() {
        while (true) {
            Reference<? extends Object> referencePoll = exceptionTableRefQueue.poll();
            if (referencePoll != null) {
                if (referencePoll instanceof ExceptionNode) {
                    int i = ((ExceptionNode) referencePoll).hashCode;
                    ExceptionNode[] exceptionNodeArr = exceptionTable;
                    int length = i & (exceptionNodeArr.length - 1);
                    ExceptionNode exceptionNode = exceptionNodeArr[length];
                    ExceptionNode exceptionNode2 = null;
                    while (true) {
                        if (exceptionNode != null) {
                            ExceptionNode exceptionNode3 = exceptionNode.next;
                            if (exceptionNode != referencePoll) {
                                exceptionNode2 = exceptionNode;
                                exceptionNode = exceptionNode3;
                            } else if (exceptionNode2 == null) {
                                exceptionNodeArr[length] = exceptionNode3;
                            } else {
                                exceptionNode2.next = exceptionNode3;
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
        ReentrantLock reentrantLock = exceptionTableLock;
        if (reentrantLock.tryLock()) {
            try {
                expungeStaleExceptions();
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    static void rethrow(Throwable th) {
        uncheckedThrow(th);
    }

    static <T extends Throwable> void uncheckedThrow(Throwable th) throws Throwable {
        if (th != null) {
            throw th;
        }
        throw new Error("Unknown Exception");
    }

    private void reportException(int i) {
        if (i == CANCELLED) {
            throw new CancellationException();
        }
        if (i == Integer.MIN_VALUE) {
            rethrow(getThrowableException());
        }
    }

    public final ForkJoinTask<V> fork() {
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            ((ForkJoinWorkerThread) threadCurrentThread).workQueue.push(this);
        } else {
            ForkJoinPool.common.externalPush(this);
        }
        return this;
    }

    public final V join() {
        int iDoJoin = doJoin() & (-268435456);
        if (iDoJoin != -268435456) {
            reportException(iDoJoin);
        }
        return getRawResult();
    }

    public final V invoke() {
        int iDoInvoke = doInvoke() & (-268435456);
        if (iDoInvoke != -268435456) {
            reportException(iDoInvoke);
        }
        return getRawResult();
    }

    public static void invokeAll(ForkJoinTask<?> forkJoinTask, ForkJoinTask<?> forkJoinTask2) {
        forkJoinTask2.fork();
        int iDoInvoke = forkJoinTask.doInvoke() & (-268435456);
        if (iDoInvoke != -268435456) {
            forkJoinTask.reportException(iDoInvoke);
        }
        int iDoJoin = forkJoinTask2.doJoin() & (-268435456);
        if (iDoJoin != -268435456) {
            forkJoinTask2.reportException(iDoJoin);
        }
    }

    public static void invokeAll(ForkJoinTask<?>... forkJoinTaskArr) {
        int length = forkJoinTaskArr.length - 1;
        Throwable exception = null;
        for (int i = length; i >= 0; i--) {
            ForkJoinTask<?> forkJoinTask = forkJoinTaskArr[i];
            if (forkJoinTask == null) {
                if (exception == null) {
                    exception = new NullPointerException();
                }
            } else if (i != 0) {
                forkJoinTask.fork();
            } else if (forkJoinTask.doInvoke() < -268435456 && exception == null) {
                exception = forkJoinTask.getException();
            }
        }
        for (int i2 = 1; i2 <= length; i2++) {
            ForkJoinTask<?> forkJoinTask2 = forkJoinTaskArr[i2];
            if (forkJoinTask2 != null) {
                if (exception != null) {
                    forkJoinTask2.cancel(false);
                } else if (forkJoinTask2.doJoin() < -268435456) {
                    exception = forkJoinTask2.getException();
                }
            }
        }
        if (exception != null) {
            rethrow(exception);
        }
    }

    public static <T extends ForkJoinTask<?>> Collection<T> invokeAll(Collection<T> collection) {
        if (!(collection instanceof RandomAccess) || !(collection instanceof List)) {
            invokeAll((ForkJoinTask<?>[]) collection.toArray(new ForkJoinTask[collection.size()]));
            return collection;
        }
        List list = (List) collection;
        int size = list.size() - 1;
        Throwable exception = null;
        for (int i = size; i >= 0; i--) {
            ForkJoinTask forkJoinTask = (ForkJoinTask) list.get(i);
            if (forkJoinTask == null) {
                if (exception == null) {
                    exception = new NullPointerException();
                }
            } else if (i != 0) {
                forkJoinTask.fork();
            } else if (forkJoinTask.doInvoke() < -268435456 && exception == null) {
                exception = forkJoinTask.getException();
            }
        }
        for (int i2 = 1; i2 <= size; i2++) {
            ForkJoinTask forkJoinTask2 = (ForkJoinTask) list.get(i2);
            if (forkJoinTask2 != null) {
                if (exception != null) {
                    forkJoinTask2.cancel(false);
                } else if (forkJoinTask2.doJoin() < -268435456) {
                    exception = forkJoinTask2.getException();
                }
            }
        }
        if (exception != null) {
            rethrow(exception);
        }
        return collection;
    }

    @Override
    public boolean cancel(boolean z) {
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
        int i = this.status & (-268435456);
        if (i >= -268435456) {
            return null;
        }
        return i == CANCELLED ? new CancellationException() : getThrowableException();
    }

    public void completeExceptionally(Throwable th) {
        if (!(th instanceof RuntimeException) && !(th instanceof Error)) {
            th = new RuntimeException(th);
        }
        setExceptionalCompletion(th);
    }

    public void complete(V v) {
        try {
            setRawResult(v);
            setCompletion(-268435456);
        } catch (Throwable th) {
            setExceptionalCompletion(th);
        }
    }

    public final void quietlyComplete() {
        setCompletion(-268435456);
    }

    @Override
    public final V get() throws ExecutionException, InterruptedException {
        int iDoJoin = (Thread.currentThread() instanceof ForkJoinWorkerThread ? doJoin() : externalInterruptibleAwaitDone()) & (-268435456);
        if (iDoJoin == CANCELLED) {
            throw new CancellationException();
        }
        if (iDoJoin == Integer.MIN_VALUE) {
            throw new ExecutionException(getThrowableException());
        }
        return getRawResult();
    }

    @Override
    public final V get(long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        int iDoExec;
        int i;
        long nanos = timeUnit.toNanos(j);
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        int iAwaitJoin = this.status;
        if (iAwaitJoin >= 0 && nanos > 0) {
            long jNanoTime = System.nanoTime() + nanos;
            if (jNanoTime == 0) {
                jNanoTime = 1;
            }
            Thread threadCurrentThread = Thread.currentThread();
            if (threadCurrentThread instanceof ForkJoinWorkerThread) {
                ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) threadCurrentThread;
                iAwaitJoin = forkJoinWorkerThread.pool.awaitJoin(forkJoinWorkerThread.workQueue, this, jNanoTime);
            } else {
                if (this instanceof CountedCompleter) {
                    iDoExec = ForkJoinPool.common.externalHelpComplete((CountedCompleter) this, 0);
                } else if (ForkJoinPool.common.tryExternalUnpush(this)) {
                    iDoExec = doExec();
                } else {
                    iAwaitJoin = 0;
                    if (iAwaitJoin >= 0) {
                        while (true) {
                            i = this.status;
                            if (i < 0) {
                                break;
                            }
                            long jNanoTime2 = jNanoTime - System.nanoTime();
                            if (jNanoTime2 <= 0) {
                                break;
                            }
                            long millis = TimeUnit.NANOSECONDS.toMillis(jNanoTime2);
                            if (millis > 0 && U.compareAndSwapInt(this, STATUS, i, i | 65536)) {
                                synchronized (this) {
                                    if (this.status >= 0) {
                                        wait(millis);
                                    } else {
                                        notifyAll();
                                    }
                                }
                            }
                        }
                        iAwaitJoin = i;
                    }
                }
                iAwaitJoin = iDoExec;
                if (iAwaitJoin >= 0) {
                }
            }
        }
        if (iAwaitJoin >= 0) {
            iAwaitJoin = this.status;
        }
        int i2 = iAwaitJoin & (-268435456);
        if (i2 != -268435456) {
            if (i2 == CANCELLED) {
                throw new CancellationException();
            }
            if (i2 != Integer.MIN_VALUE) {
                throw new TimeoutException();
            }
            throw new ExecutionException(getThrowableException());
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
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) threadCurrentThread;
            forkJoinWorkerThread.pool.helpQuiescePool(forkJoinWorkerThread.workQueue);
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
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            return ((ForkJoinWorkerThread) threadCurrentThread).pool;
        }
        return null;
    }

    public static boolean inForkJoinPool() {
        return Thread.currentThread() instanceof ForkJoinWorkerThread;
    }

    public boolean tryUnfork() {
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            return ((ForkJoinWorkerThread) threadCurrentThread).workQueue.tryUnpush(this);
        }
        return ForkJoinPool.common.tryExternalUnpush(this);
    }

    public static int getQueuedTaskCount() {
        ForkJoinPool.WorkQueue workQueueCommonSubmitterQueue;
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            workQueueCommonSubmitterQueue = ((ForkJoinWorkerThread) threadCurrentThread).workQueue;
        } else {
            workQueueCommonSubmitterQueue = ForkJoinPool.commonSubmitterQueue();
        }
        if (workQueueCommonSubmitterQueue == null) {
            return 0;
        }
        return workQueueCommonSubmitterQueue.queueSize();
    }

    public static int getSurplusQueuedTaskCount() {
        return ForkJoinPool.getSurplusQueuedTaskCount();
    }

    protected static ForkJoinTask<?> peekNextLocalTask() {
        ForkJoinPool.WorkQueue workQueueCommonSubmitterQueue;
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            workQueueCommonSubmitterQueue = ((ForkJoinWorkerThread) threadCurrentThread).workQueue;
        } else {
            workQueueCommonSubmitterQueue = ForkJoinPool.commonSubmitterQueue();
        }
        if (workQueueCommonSubmitterQueue == null) {
            return null;
        }
        return workQueueCommonSubmitterQueue.peek();
    }

    protected static ForkJoinTask<?> pollNextLocalTask() {
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            return ((ForkJoinWorkerThread) threadCurrentThread).workQueue.nextLocalTask();
        }
        return null;
    }

    protected static ForkJoinTask<?> pollTask() {
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) threadCurrentThread;
            return forkJoinWorkerThread.pool.nextTaskFor(forkJoinWorkerThread.workQueue);
        }
        return null;
    }

    protected static ForkJoinTask<?> pollSubmission() {
        Thread threadCurrentThread = Thread.currentThread();
        if (threadCurrentThread instanceof ForkJoinWorkerThread) {
            return ((ForkJoinWorkerThread) threadCurrentThread).pool.pollSubmission();
        }
        return null;
    }

    public final short getForkJoinTaskTag() {
        return (short) this.status;
    }

    public final short setForkJoinTaskTag(short s) {
        Unsafe unsafe;
        long j;
        int i;
        do {
            unsafe = U;
            j = STATUS;
            i = this.status;
        } while (!unsafe.compareAndSwapInt(this, j, i, ((-65536) & i) | (SMASK & s)));
        return (short) i;
    }

    public final boolean compareAndSetForkJoinTaskTag(short s, short s2) {
        int i;
        do {
            i = this.status;
            if (((short) i) != s) {
                return false;
            }
        } while (!U.compareAndSwapInt(this, STATUS, i, (SMASK & s2) | ((-65536) & i)));
        return true;
    }

    static final class AdaptedRunnable<T> extends ForkJoinTask<T> implements RunnableFuture<T> {
        private static final long serialVersionUID = 5232453952276885070L;
        T result;
        final Runnable runnable;

        AdaptedRunnable(Runnable runnable, T t) {
            if (runnable == null) {
                throw new NullPointerException();
            }
            this.runnable = runnable;
            this.result = t;
        }

        @Override
        public final T getRawResult() {
            return this.result;
        }

        @Override
        public final void setRawResult(T t) {
            this.result = t;
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
        public final void setRawResult(Void r1) {
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
        public final void setRawResult(Void r1) {
        }

        @Override
        public final boolean exec() {
            this.runnable.run();
            return true;
        }

        @Override
        void internalPropagateException(Throwable th) {
            rethrow(th);
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
        public final void setRawResult(T t) {
            this.result = t;
        }

        @Override
        public final boolean exec() {
            try {
                this.result = this.callable.call();
                return true;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e2) {
                throw new RuntimeException(e2);
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

    public static <T> ForkJoinTask<T> adapt(Runnable runnable, T t) {
        return new AdaptedRunnable(runnable, t);
    }

    public static <T> ForkJoinTask<T> adapt(Callable<? extends T> callable) {
        return new AdaptedCallable(callable);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(getException());
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        Object object = objectInputStream.readObject();
        if (object != null) {
            setExceptionalCompletion((Throwable) object);
        }
    }

    static {
        try {
            STATUS = U.objectFieldOffset(ForkJoinTask.class.getDeclaredField("status"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
