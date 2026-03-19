package java.util.concurrent;

import dalvik.annotation.optimization.ReachabilitySensitive;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPoolExecutor extends AbstractExecutorService {
    private static final int CAPACITY = 536870911;
    private static final int COUNT_BITS = 29;
    private static final boolean ONLY_ONE = true;
    private static final int RUNNING = -536870912;
    private static final int SHUTDOWN = 0;
    private static final int STOP = 536870912;
    private static final int TERMINATED = 1610612736;
    private static final int TIDYING = 1073741824;
    private static final RejectedExecutionHandler defaultHandler = new AbortPolicy();
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");
    private volatile boolean allowCoreThreadTimeOut;
    private long completedTaskCount;
    private volatile int corePoolSize;

    @ReachabilitySensitive
    private final AtomicInteger ctl;
    private volatile RejectedExecutionHandler handler;
    private volatile long keepAliveTime;
    private int largestPoolSize;
    private final ReentrantLock mainLock;
    private volatile int maximumPoolSize;
    private final Condition termination;
    private volatile ThreadFactory threadFactory;
    private final BlockingQueue<Runnable> workQueue;

    @ReachabilitySensitive
    private final HashSet<Worker> workers;

    private static int runStateOf(int i) {
        return i & RUNNING;
    }

    private static int workerCountOf(int i) {
        return i & CAPACITY;
    }

    private static int ctlOf(int i, int i2) {
        return i | i2;
    }

    private static boolean runStateLessThan(int i, int i2) {
        if (i < i2) {
            return ONLY_ONE;
        }
        return false;
    }

    private static boolean runStateAtLeast(int i, int i2) {
        if (i >= i2) {
            return ONLY_ONE;
        }
        return false;
    }

    private static boolean isRunning(int i) {
        if (i < 0) {
            return ONLY_ONE;
        }
        return false;
    }

    private boolean compareAndIncrementWorkerCount(int i) {
        return this.ctl.compareAndSet(i, i + 1);
    }

    private boolean compareAndDecrementWorkerCount(int i) {
        return this.ctl.compareAndSet(i, i - 1);
    }

    private void decrementWorkerCount() {
        while (!compareAndDecrementWorkerCount(this.ctl.get())) {
        }
    }

    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {
        private static final long serialVersionUID = 6138294804551838833L;
        volatile long completedTasks;
        Runnable firstTask;
        final Thread thread;

        Worker(Runnable runnable) {
            setState(-1);
            this.firstTask = runnable;
            this.thread = ThreadPoolExecutor.this.getThreadFactory().newThread(this);
        }

        @Override
        public void run() throws Throwable {
            ThreadPoolExecutor.this.runWorker(this);
        }

        @Override
        protected boolean isHeldExclusively() {
            if (getState() != 0) {
                return ThreadPoolExecutor.ONLY_ONE;
            }
            return false;
        }

        @Override
        protected boolean tryAcquire(int i) {
            if (!compareAndSetState(0, 1)) {
                return false;
            }
            setExclusiveOwnerThread(Thread.currentThread());
            return ThreadPoolExecutor.ONLY_ONE;
        }

        @Override
        protected boolean tryRelease(int i) {
            setExclusiveOwnerThread(null);
            setState(0);
            return ThreadPoolExecutor.ONLY_ONE;
        }

        public void lock() {
            acquire(1);
        }

        public boolean tryLock() {
            return tryAcquire(1);
        }

        public void unlock() {
            release(1);
        }

        public boolean isLocked() {
            return isHeldExclusively();
        }

        void interruptIfStarted() {
            Thread thread;
            if (getState() >= 0 && (thread = this.thread) != null && !thread.isInterrupted()) {
                try {
                    thread.interrupt();
                } catch (SecurityException e) {
                }
            }
        }
    }

    private void advanceRunState(int i) {
        int i2;
        do {
            i2 = this.ctl.get();
            if (runStateAtLeast(i2, i)) {
                return;
            }
        } while (!this.ctl.compareAndSet(i2, ctlOf(i, workerCountOf(i2))));
    }

    final void tryTerminate() {
        while (true) {
            int i = this.ctl.get();
            if (!isRunning(i) && !runStateAtLeast(i, TIDYING)) {
                if (runStateOf(i) == 0 && !this.workQueue.isEmpty()) {
                    return;
                }
                if (workerCountOf(i) != 0) {
                    interruptIdleWorkers(ONLY_ONE);
                    return;
                }
                ReentrantLock reentrantLock = this.mainLock;
                reentrantLock.lock();
                try {
                    if (this.ctl.compareAndSet(i, ctlOf(TIDYING, 0))) {
                        try {
                            terminated();
                            return;
                        } finally {
                            this.ctl.set(ctlOf(TERMINATED, 0));
                            this.termination.signalAll();
                        }
                    }
                } finally {
                    reentrantLock.unlock();
                }
            } else {
                return;
            }
        }
    }

    private void checkShutdownAccess() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(shutdownPerm);
            ReentrantLock reentrantLock = this.mainLock;
            reentrantLock.lock();
            try {
                Iterator<Worker> it = this.workers.iterator();
                while (it.hasNext()) {
                    securityManager.checkAccess(it.next().thread);
                }
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    private void interruptWorkers() {
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        try {
            Iterator<Worker> it = this.workers.iterator();
            while (it.hasNext()) {
                it.next().interruptIfStarted();
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    private void interruptIdleWorkers(boolean z) {
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        try {
            Iterator<Worker> it = this.workers.iterator();
            while (it.hasNext()) {
                Worker next = it.next();
                Thread thread = next.thread;
                if (!thread.isInterrupted() && next.tryLock()) {
                    try {
                        thread.interrupt();
                    } catch (SecurityException e) {
                    } finally {
                    }
                }
                if (z) {
                    break;
                }
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    final void reject(Runnable runnable) {
        this.handler.rejectedExecution(runnable, this);
    }

    void onShutdown() {
    }

    final boolean isRunningOrShutdown(boolean z) {
        int iRunStateOf = runStateOf(this.ctl.get());
        if (iRunStateOf == RUNNING || (iRunStateOf == 0 && z)) {
            return ONLY_ONE;
        }
        return false;
    }

    private java.util.List<java.lang.Runnable> drainQueue() {
        throw new UnsupportedOperationException("Method not decompiled: java.util.concurrent.ThreadPoolExecutor.drainQueue():java.util.List");
    }

    private boolean addWorker(Runnable runnable, boolean z) throws Throwable {
        Worker worker;
        boolean z2;
        loop0: while (true) {
            int i = this.ctl.get();
            int iRunStateOf = runStateOf(i);
            if (iRunStateOf >= 0 && (iRunStateOf != 0 || runnable != null || this.workQueue.isEmpty())) {
                break;
            }
            do {
                int iWorkerCountOf = workerCountOf(i);
                if (iWorkerCountOf >= CAPACITY) {
                    break loop0;
                }
                if (iWorkerCountOf >= (z ? this.corePoolSize : this.maximumPoolSize)) {
                    break loop0;
                }
                if (!compareAndIncrementWorkerCount(i)) {
                    i = this.ctl.get();
                } else {
                    try {
                        worker = new Worker(runnable);
                    } catch (Throwable th) {
                        th = th;
                        worker = null;
                    }
                    try {
                        Thread thread = worker.thread;
                        boolean z3 = ONLY_ONE;
                        if (thread != null) {
                            ReentrantLock reentrantLock = this.mainLock;
                            reentrantLock.lock();
                            try {
                                int iRunStateOf2 = runStateOf(this.ctl.get());
                                if (iRunStateOf2 < 0 || (iRunStateOf2 == 0 && runnable == null)) {
                                    if (thread.isAlive()) {
                                        throw new IllegalThreadStateException();
                                    }
                                    this.workers.add(worker);
                                    int size = this.workers.size();
                                    if (size > this.largestPoolSize) {
                                        this.largestPoolSize = size;
                                    }
                                    z2 = true;
                                } else {
                                    z2 = false;
                                }
                                if (z2) {
                                    thread.start();
                                } else {
                                    z3 = false;
                                }
                            } finally {
                                reentrantLock.unlock();
                            }
                        }
                        if (!z3) {
                            addWorkerFailed(worker);
                        }
                        return z3;
                    } catch (Throwable th2) {
                        th = th2;
                        addWorkerFailed(worker);
                        throw th;
                    }
                }
            } while (runStateOf(i) == iRunStateOf);
        }
    }

    private void addWorkerFailed(Worker worker) {
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        if (worker != null) {
            try {
                this.workers.remove(worker);
            } catch (Throwable th) {
                reentrantLock.unlock();
                throw th;
            }
        }
        decrementWorkerCount();
        tryTerminate();
        reentrantLock.unlock();
    }

    private void processWorkerExit(Worker worker, boolean z) throws Throwable {
        int i;
        if (z) {
            decrementWorkerCount();
        }
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        try {
            this.completedTaskCount += worker.completedTasks;
            this.workers.remove(worker);
            reentrantLock.unlock();
            tryTerminate();
            int i2 = this.ctl.get();
            if (runStateLessThan(i2, STOP)) {
                if (!z) {
                    if (!this.allowCoreThreadTimeOut) {
                        i = this.corePoolSize;
                    } else {
                        i = 0;
                    }
                    if (i == 0 && !this.workQueue.isEmpty()) {
                        i = 1;
                    }
                    if (workerCountOf(i2) >= i) {
                        return;
                    }
                }
                addWorker(null, false);
            }
        } catch (Throwable th) {
            reentrantLock.unlock();
            throw th;
        }
    }

    private Runnable getTask() {
        Runnable runnableTake;
        boolean z = false;
        while (true) {
            int i = this.ctl.get();
            int iRunStateOf = runStateOf(i);
            if (iRunStateOf >= 0 && (iRunStateOf >= STOP || this.workQueue.isEmpty())) {
                break;
            }
            int iWorkerCountOf = workerCountOf(i);
            boolean z2 = this.allowCoreThreadTimeOut || iWorkerCountOf > this.corePoolSize;
            if ((iWorkerCountOf > this.maximumPoolSize || (z2 && z)) && (iWorkerCountOf > 1 || this.workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(i)) {
                    return null;
                }
            } else {
                if (z2) {
                    try {
                        runnableTake = this.workQueue.poll(this.keepAliveTime, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        z = false;
                    }
                } else {
                    runnableTake = this.workQueue.take();
                }
                if (runnableTake != null) {
                    return runnableTake;
                }
                z = true;
            }
        }
    }

    final void runWorker(Worker worker) throws Throwable {
        Thread threadCurrentThread = Thread.currentThread();
        Runnable task = worker.firstTask;
        worker.firstTask = null;
        worker.unlock();
        while (true) {
            if (task == null) {
                try {
                    task = getTask();
                    if (task == null) {
                        processWorkerExit(worker, false);
                        return;
                    }
                } catch (Throwable th) {
                    processWorkerExit(worker, ONLY_ONE);
                    throw th;
                }
            }
            worker.lock();
            if ((runStateAtLeast(this.ctl.get(), STOP) || (Thread.interrupted() && runStateAtLeast(this.ctl.get(), STOP))) && !threadCurrentThread.isInterrupted()) {
                threadCurrentThread.interrupt();
            }
            try {
                beforeExecute(threadCurrentThread, task);
                try {
                    try {
                        try {
                            try {
                                task.run();
                                task = null;
                            } catch (Error e) {
                                throw e;
                            }
                        } catch (RuntimeException e2) {
                            throw e2;
                        }
                    } catch (Throwable th2) {
                        throw new Error(th2);
                    }
                } finally {
                    afterExecute(task, null);
                }
            } finally {
                worker.completedTasks++;
                worker.unlock();
            }
        }
    }

    public ThreadPoolExecutor(int i, int i2, long j, TimeUnit timeUnit, BlockingQueue<Runnable> blockingQueue) {
        this(i, i2, j, timeUnit, blockingQueue, Executors.defaultThreadFactory(), defaultHandler);
    }

    public ThreadPoolExecutor(int i, int i2, long j, TimeUnit timeUnit, BlockingQueue<Runnable> blockingQueue, ThreadFactory threadFactory) {
        this(i, i2, j, timeUnit, blockingQueue, threadFactory, defaultHandler);
    }

    public ThreadPoolExecutor(int i, int i2, long j, TimeUnit timeUnit, BlockingQueue<Runnable> blockingQueue, RejectedExecutionHandler rejectedExecutionHandler) {
        this(i, i2, j, timeUnit, blockingQueue, Executors.defaultThreadFactory(), rejectedExecutionHandler);
    }

    public ThreadPoolExecutor(int i, int i2, long j, TimeUnit timeUnit, BlockingQueue<Runnable> blockingQueue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        this.ctl = new AtomicInteger(ctlOf(RUNNING, 0));
        this.mainLock = new ReentrantLock();
        this.workers = new HashSet<>();
        this.termination = this.mainLock.newCondition();
        if (i < 0 || i2 <= 0 || i2 < i || j < 0) {
            throw new IllegalArgumentException();
        }
        if (blockingQueue == null || threadFactory == null || rejectedExecutionHandler == null) {
            throw new NullPointerException();
        }
        this.corePoolSize = i;
        this.maximumPoolSize = i2;
        this.workQueue = blockingQueue;
        this.keepAliveTime = timeUnit.toNanos(j);
        this.threadFactory = threadFactory;
        this.handler = rejectedExecutionHandler;
    }

    @Override
    public void execute(Runnable runnable) throws Throwable {
        if (runnable == null) {
            throw new NullPointerException();
        }
        int i = this.ctl.get();
        if (workerCountOf(i) < this.corePoolSize) {
            if (addWorker(runnable, ONLY_ONE)) {
                return;
            } else {
                i = this.ctl.get();
            }
        }
        if (isRunning(i) && this.workQueue.offer(runnable)) {
            int i2 = this.ctl.get();
            if (!isRunning(i2) && remove(runnable)) {
                reject(runnable);
                return;
            } else {
                if (workerCountOf(i2) == 0) {
                    addWorker(null, false);
                    return;
                }
                return;
            }
        }
        if (!addWorker(runnable, false)) {
            reject(runnable);
        }
    }

    @Override
    public void shutdown() {
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(0);
            interruptIdleWorkers();
            onShutdown();
            reentrantLock.unlock();
            tryTerminate();
        } catch (Throwable th) {
            reentrantLock.unlock();
            throw th;
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            List<Runnable> listDrainQueue = drainQueue();
            reentrantLock.unlock();
            tryTerminate();
            return listDrainQueue;
        } catch (Throwable th) {
            reentrantLock.unlock();
            throw th;
        }
    }

    @Override
    public boolean isShutdown() {
        return isRunning(this.ctl.get()) ^ ONLY_ONE;
    }

    public boolean isTerminating() {
        int i = this.ctl.get();
        if (isRunning(i) || !runStateLessThan(i, TERMINATED)) {
            return false;
        }
        return ONLY_ONE;
    }

    @Override
    public boolean isTerminated() {
        return runStateAtLeast(this.ctl.get(), TERMINATED);
    }

    @Override
    public boolean awaitTermination(long j, TimeUnit timeUnit) throws InterruptedException {
        long nanos = timeUnit.toNanos(j);
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        while (!runStateAtLeast(this.ctl.get(), TERMINATED)) {
            try {
                if (nanos > 0) {
                    nanos = this.termination.awaitNanos(nanos);
                } else {
                    return false;
                }
            } finally {
                reentrantLock.unlock();
            }
        }
        return ONLY_ONE;
    }

    protected void finalize() {
        shutdown();
    }

    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException();
        }
        this.threadFactory = threadFactory;
    }

    public ThreadFactory getThreadFactory() {
        return this.threadFactory;
    }

    public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
        if (rejectedExecutionHandler == null) {
            throw new NullPointerException();
        }
        this.handler = rejectedExecutionHandler;
    }

    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return this.handler;
    }

    public void setCorePoolSize(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        int i2 = i - this.corePoolSize;
        this.corePoolSize = i;
        if (workerCountOf(this.ctl.get()) > i) {
            interruptIdleWorkers();
            return;
        }
        if (i2 > 0) {
            int iMin = Math.min(i2, this.workQueue.size());
            while (true) {
                int i3 = iMin - 1;
                if (iMin > 0 && addWorker(null, ONLY_ONE) && !this.workQueue.isEmpty()) {
                    iMin = i3;
                } else {
                    return;
                }
            }
        }
    }

    public int getCorePoolSize() {
        return this.corePoolSize;
    }

    public boolean prestartCoreThread() {
        if (workerCountOf(this.ctl.get()) >= this.corePoolSize || !addWorker(null, ONLY_ONE)) {
            return false;
        }
        return ONLY_ONE;
    }

    void ensurePrestart() throws Throwable {
        int iWorkerCountOf = workerCountOf(this.ctl.get());
        if (iWorkerCountOf < this.corePoolSize) {
            addWorker(null, ONLY_ONE);
        } else if (iWorkerCountOf == 0) {
            addWorker(null, false);
        }
    }

    public int prestartAllCoreThreads() {
        int i = 0;
        while (addWorker(null, ONLY_ONE)) {
            i++;
        }
        return i;
    }

    public boolean allowsCoreThreadTimeOut() {
        return this.allowCoreThreadTimeOut;
    }

    public void allowCoreThreadTimeOut(boolean z) {
        if (z && this.keepAliveTime <= 0) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        if (z != this.allowCoreThreadTimeOut) {
            this.allowCoreThreadTimeOut = z;
            if (z) {
                interruptIdleWorkers();
            }
        }
    }

    public void setMaximumPoolSize(int i) {
        if (i <= 0 || i < this.corePoolSize) {
            throw new IllegalArgumentException();
        }
        this.maximumPoolSize = i;
        if (workerCountOf(this.ctl.get()) > i) {
            interruptIdleWorkers();
        }
    }

    public int getMaximumPoolSize() {
        return this.maximumPoolSize;
    }

    public void setKeepAliveTime(long j, TimeUnit timeUnit) {
        if (j < 0) {
            throw new IllegalArgumentException();
        }
        if (j == 0 && allowsCoreThreadTimeOut()) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        long nanos = timeUnit.toNanos(j);
        long j2 = nanos - this.keepAliveTime;
        this.keepAliveTime = nanos;
        if (j2 < 0) {
            interruptIdleWorkers();
        }
    }

    public long getKeepAliveTime(TimeUnit timeUnit) {
        return timeUnit.convert(this.keepAliveTime, TimeUnit.NANOSECONDS);
    }

    public BlockingQueue<Runnable> getQueue() {
        return this.workQueue;
    }

    public boolean remove(Runnable runnable) {
        boolean zRemove = this.workQueue.remove(runnable);
        tryTerminate();
        return zRemove;
    }

    public void purge() {
        BlockingQueue<Runnable> blockingQueue = this.workQueue;
        try {
            Iterator<Runnable> it = blockingQueue.iterator();
            while (it.hasNext()) {
                Runnable next = it.next();
                if ((next instanceof Future) && ((Future) next).isCancelled()) {
                    it.remove();
                }
            }
        } catch (ConcurrentModificationException e) {
            for (Object obj : blockingQueue.toArray()) {
                if ((obj instanceof Future) && ((Future) obj).isCancelled()) {
                    blockingQueue.remove(obj);
                }
            }
        }
        tryTerminate();
    }

    public int getPoolSize() {
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        try {
            return runStateAtLeast(this.ctl.get(), TIDYING) ? 0 : this.workers.size();
        } finally {
            reentrantLock.unlock();
        }
    }

    public int getActiveCount() {
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        int i = 0;
        try {
            Iterator<Worker> it = this.workers.iterator();
            while (it.hasNext()) {
                if (it.next().isLocked()) {
                    i++;
                }
            }
            return i;
        } finally {
            reentrantLock.unlock();
        }
    }

    public int getLargestPoolSize() {
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        try {
            return this.largestPoolSize;
        } finally {
            reentrantLock.unlock();
        }
    }

    public long getTaskCount() {
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        try {
            long j = this.completedTaskCount;
            for (Worker worker : this.workers) {
                j += worker.completedTasks;
                if (worker.isLocked()) {
                    j++;
                }
            }
            return j + ((long) this.workQueue.size());
        } finally {
            reentrantLock.unlock();
        }
    }

    public long getCompletedTaskCount() {
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        try {
            long j = this.completedTaskCount;
            Iterator<Worker> it = this.workers.iterator();
            while (it.hasNext()) {
                j += it.next().completedTasks;
            }
            return j;
        } finally {
            reentrantLock.unlock();
        }
    }

    public String toString() {
        String str;
        ReentrantLock reentrantLock = this.mainLock;
        reentrantLock.lock();
        try {
            long j = this.completedTaskCount;
            int size = this.workers.size();
            int i = 0;
            for (Worker worker : this.workers) {
                j += worker.completedTasks;
                if (worker.isLocked()) {
                    i++;
                }
            }
            reentrantLock.unlock();
            int i2 = this.ctl.get();
            if (runStateLessThan(i2, 0)) {
                str = "Running";
            } else {
                str = runStateAtLeast(i2, TERMINATED) ? "Terminated" : "Shutting down";
            }
            return super.toString() + "[" + str + ", pool size = " + size + ", active threads = " + i + ", queued tasks = " + this.workQueue.size() + ", completed tasks = " + j + "]";
        } catch (Throwable th) {
            reentrantLock.unlock();
            throw th;
        }
    }

    protected void beforeExecute(Thread thread, Runnable runnable) {
    }

    protected void afterExecute(Runnable runnable, Throwable th) {
    }

    protected void terminated() {
    }

    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
            if (!threadPoolExecutor.isShutdown()) {
                runnable.run();
            }
        }
    }

    public static class AbortPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
            throw new RejectedExecutionException("Task " + runnable.toString() + " rejected from " + threadPoolExecutor.toString());
        }
    }

    public static class DiscardPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
        }
    }

    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) throws Throwable {
            if (!threadPoolExecutor.isShutdown()) {
                threadPoolExecutor.getQueue().poll();
                threadPoolExecutor.execute(runnable);
            }
        }
    }
}
