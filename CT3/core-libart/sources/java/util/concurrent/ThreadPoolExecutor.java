package java.util.concurrent;

import java.util.ArrayList;
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
    private final AtomicInteger ctl;
    private volatile RejectedExecutionHandler handler;
    private volatile long keepAliveTime;
    private int largestPoolSize;
    private final ReentrantLock mainLock;
    private volatile int maximumPoolSize;
    private final Condition termination;
    private volatile ThreadFactory threadFactory;
    private final BlockingQueue<Runnable> workQueue;
    private final HashSet<Worker> workers;

    private static int runStateOf(int c) {
        return RUNNING & c;
    }

    private static int workerCountOf(int c) {
        return CAPACITY & c;
    }

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < 0;
    }

    private boolean compareAndIncrementWorkerCount(int expect) {
        return this.ctl.compareAndSet(expect, expect + 1);
    }

    private boolean compareAndDecrementWorkerCount(int expect) {
        return this.ctl.compareAndSet(expect, expect - 1);
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

        Worker(Runnable firstTask) {
            setState(-1);
            this.firstTask = firstTask;
            this.thread = ThreadPoolExecutor.this.getThreadFactory().newThread(this);
        }

        @Override
        public void run() throws Throwable {
            ThreadPoolExecutor.this.runWorker(this);
        }

        @Override
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        @Override
        protected boolean tryAcquire(int unused) {
            if (!compareAndSetState(0, 1)) {
                return false;
            }
            setExclusiveOwnerThread(Thread.currentThread());
            return true;
        }

        @Override
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
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
            Thread t;
            if (getState() < 0 || (t = this.thread) == null || t.isInterrupted()) {
                return;
            }
            try {
                t.interrupt();
            } catch (SecurityException e) {
            }
        }
    }

    private void advanceRunState(int targetState) {
        int c;
        do {
            c = this.ctl.get();
            if (runStateAtLeast(c, targetState)) {
                return;
            }
        } while (!this.ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))));
    }

    final void tryTerminate() {
        while (true) {
            int c = this.ctl.get();
            if (!isRunning(c) && !runStateAtLeast(c, 1073741824)) {
                if (runStateOf(c) == 0 && !this.workQueue.isEmpty()) {
                    return;
                }
                if (workerCountOf(c) != 0) {
                    interruptIdleWorkers(true);
                    return;
                }
                ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    if (this.ctl.compareAndSet(c, ctlOf(1073741824, 0))) {
                        try {
                            terminated();
                            return;
                        } finally {
                            this.ctl.set(ctlOf(TERMINATED, 0));
                            this.termination.signalAll();
                        }
                    }
                } finally {
                    mainLock.unlock();
                }
            } else {
                return;
            }
        }
    }

    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security == null) {
            return;
        }
        security.checkPermission(shutdownPerm);
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : this.workers) {
                security.checkAccess(w.thread);
            }
        } finally {
            mainLock.unlock();
        }
    }

    private void interruptWorkers() {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : this.workers) {
                w.interruptIfStarted();
            }
        } finally {
            mainLock.unlock();
        }
    }

    private void interruptIdleWorkers(boolean onlyOne) {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : this.workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException e) {
                    } finally {
                    }
                }
                if (onlyOne) {
                    break;
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    final void reject(Runnable command) {
        this.handler.rejectedExecution(command, this);
    }

    void onShutdown() {
    }

    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(this.ctl.get());
        if (rs == RUNNING) {
            return true;
        }
        if (rs == 0) {
            return shutdownOK;
        }
        return false;
    }

    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = this.workQueue;
        ArrayList<Runnable> taskList = new ArrayList<>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : (Runnable[]) q.toArray(new Runnable[0])) {
                if (q.remove(r)) {
                    taskList.add(r);
                }
            }
        }
        return taskList;
    }

    private boolean addWorker(Runnable firstTask, boolean core) throws Throwable {
        loop0: while (true) {
            int c = this.ctl.get();
            int rs = runStateOf(c);
            if (rs >= 0 && (rs != 0 || firstTask != null || this.workQueue.isEmpty())) {
                break;
            }
            do {
                int wc = workerCountOf(c);
                if (wc >= CAPACITY) {
                    break loop0;
                }
                if (wc >= (core ? this.corePoolSize : this.maximumPoolSize)) {
                    break loop0;
                }
                if (!compareAndIncrementWorkerCount(c)) {
                    c = this.ctl.get();
                } else {
                    boolean workerStarted = false;
                    boolean workerAdded = false;
                    Worker worker = null;
                    try {
                        Worker w = new Worker(firstTask);
                        try {
                            Thread t = w.thread;
                            if (t != null) {
                                ReentrantLock mainLock = this.mainLock;
                                mainLock.lock();
                                try {
                                    int rs2 = runStateOf(this.ctl.get());
                                    if (rs2 < 0 || (rs2 == 0 && firstTask == null)) {
                                        if (t.isAlive()) {
                                            throw new IllegalThreadStateException();
                                        }
                                        this.workers.add(w);
                                        int s = this.workers.size();
                                        if (s > this.largestPoolSize) {
                                            this.largestPoolSize = s;
                                        }
                                        workerAdded = true;
                                    }
                                    if (workerAdded) {
                                        t.start();
                                        workerStarted = true;
                                    }
                                } finally {
                                    mainLock.unlock();
                                }
                            }
                            if (!workerStarted) {
                                addWorkerFailed(w);
                            }
                            return workerStarted;
                        } catch (Throwable th) {
                            th = th;
                            worker = w;
                            if (0 == 0) {
                                addWorkerFailed(worker);
                            }
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            } while (runStateOf(c) == rs);
        }
    }

    private void addWorkerFailed(Worker w) {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        if (w != null) {
            try {
                this.workers.remove(w);
            } finally {
                mainLock.unlock();
            }
        }
        decrementWorkerCount();
        tryTerminate();
    }

    private void processWorkerExit(Worker w, boolean completedAbruptly) throws Throwable {
        if (completedAbruptly) {
            decrementWorkerCount();
        }
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            this.completedTaskCount += w.completedTasks;
            this.workers.remove(w);
            mainLock.unlock();
            tryTerminate();
            int c = this.ctl.get();
            if (!runStateLessThan(c, 536870912)) {
                return;
            }
            if (!completedAbruptly) {
                int min = this.allowCoreThreadTimeOut ? 0 : this.corePoolSize;
                if (min == 0 && !this.workQueue.isEmpty()) {
                    min = 1;
                }
                if (workerCountOf(c) >= min) {
                    return;
                }
            }
            addWorker(null, false);
        } catch (Throwable th) {
            mainLock.unlock();
            throw th;
        }
    }

    private Runnable getTask() {
        Runnable r;
        boolean timedOut = false;
        while (true) {
            int c = this.ctl.get();
            int rs = runStateOf(c);
            if (rs >= 0 && (rs >= 536870912 || this.workQueue.isEmpty())) {
                break;
            }
            int wc = workerCountOf(c);
            boolean timed = this.allowCoreThreadTimeOut || wc > this.corePoolSize;
            if ((wc > this.maximumPoolSize || (timed && timedOut)) && (wc > 1 || this.workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c)) {
                    return null;
                }
            } else {
                if (timed) {
                    try {
                        r = this.workQueue.poll(this.keepAliveTime, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        timedOut = false;
                    }
                } else {
                    r = this.workQueue.take();
                }
                if (r != null) {
                    return r;
                }
                timedOut = true;
            }
        }
    }

    final void runWorker(Worker w) throws Throwable {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock();
        boolean completedAbruptly = true;
        while (true) {
            if (task == null) {
                try {
                    task = getTask();
                    if (task == null) {
                        completedAbruptly = false;
                        return;
                    }
                } finally {
                    processWorkerExit(w, completedAbruptly);
                }
            }
            w.lock();
            if ((runStateAtLeast(this.ctl.get(), 536870912) || (Thread.interrupted() && runStateAtLeast(this.ctl.get(), 536870912))) && !wt.isInterrupted()) {
                wt.interrupt();
            }
            try {
                beforeExecute(wt, task);
                try {
                    try {
                        try {
                            task.run();
                            task = null;
                            w.completedTasks++;
                            w.unlock();
                        } catch (RuntimeException x) {
                            throw x;
                        }
                    } catch (Error x2) {
                        throw x2;
                    } catch (Throwable x3) {
                        throw new Error(x3);
                    }
                } finally {
                    afterExecute(task, null);
                }
            } catch (Throwable th) {
                w.completedTasks++;
                w.unlock();
                throw th;
            }
        }
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), defaultHandler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), handler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        this.ctl = new AtomicInteger(ctlOf(RUNNING, 0));
        this.mainLock = new ReentrantLock();
        this.workers = new HashSet<>();
        this.termination = this.mainLock.newCondition();
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize || keepAliveTime < 0) {
            throw new IllegalArgumentException();
        }
        if (workQueue == null || threadFactory == null || handler == null) {
            throw new NullPointerException();
        }
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    @Override
    public void execute(Runnable command) throws Throwable {
        if (command == null) {
            throw new NullPointerException();
        }
        int c = this.ctl.get();
        if (workerCountOf(c) < this.corePoolSize) {
            if (addWorker(command, true)) {
                return;
            } else {
                c = this.ctl.get();
            }
        }
        if (isRunning(c) && this.workQueue.offer(command)) {
            int recheck = this.ctl.get();
            if (!isRunning(recheck) && remove(command)) {
                reject(command);
                return;
            } else {
                if (workerCountOf(recheck) != 0) {
                    return;
                }
                addWorker(null, false);
                return;
            }
        }
        if (addWorker(command, false)) {
            return;
        }
        reject(command);
    }

    @Override
    public void shutdown() {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(0);
            interruptIdleWorkers();
            onShutdown();
            mainLock.unlock();
            tryTerminate();
        } catch (Throwable th) {
            mainLock.unlock();
            throw th;
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(536870912);
            interruptWorkers();
            List<Runnable> tasks = drainQueue();
            mainLock.unlock();
            tryTerminate();
            return tasks;
        } catch (Throwable th) {
            mainLock.unlock();
            throw th;
        }
    }

    @Override
    public boolean isShutdown() {
        return !isRunning(this.ctl.get());
    }

    public boolean isTerminating() {
        int c = this.ctl.get();
        if (isRunning(c)) {
            return false;
        }
        return runStateLessThan(c, TERMINATED);
    }

    @Override
    public boolean isTerminated() {
        return runStateAtLeast(this.ctl.get(), TERMINATED);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        while (!runStateAtLeast(this.ctl.get(), TERMINATED)) {
            try {
                if (nanos > 0) {
                    nanos = this.termination.awaitNanos(nanos);
                } else {
                    return false;
                }
            } finally {
                mainLock.unlock();
            }
        }
        return true;
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

    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null) {
            throw new NullPointerException();
        }
        this.handler = handler;
    }

    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return this.handler;
    }

    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0) {
            throw new IllegalArgumentException();
        }
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(this.ctl.get()) > corePoolSize) {
            interruptIdleWorkers();
            return;
        }
        if (delta <= 0) {
            return;
        }
        int k = Math.min(delta, this.workQueue.size());
        do {
            int k2 = k;
            k = k2 - 1;
            if (k2 <= 0 || !addWorker(null, true)) {
                return;
            }
        } while (!this.workQueue.isEmpty());
    }

    public int getCorePoolSize() {
        return this.corePoolSize;
    }

    public boolean prestartCoreThread() {
        if (workerCountOf(this.ctl.get()) < this.corePoolSize) {
            return addWorker(null, true);
        }
        return false;
    }

    void ensurePrestart() throws Throwable {
        int wc = workerCountOf(this.ctl.get());
        if (wc < this.corePoolSize) {
            addWorker(null, true);
        } else {
            if (wc != 0) {
                return;
            }
            addWorker(null, false);
        }
    }

    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true)) {
            n++;
        }
        return n;
    }

    public boolean allowsCoreThreadTimeOut() {
        return this.allowCoreThreadTimeOut;
    }

    public void allowCoreThreadTimeOut(boolean value) {
        if (value && this.keepAliveTime <= 0) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        if (value == this.allowCoreThreadTimeOut) {
            return;
        }
        this.allowCoreThreadTimeOut = value;
        if (!value) {
            return;
        }
        interruptIdleWorkers();
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < this.corePoolSize) {
            throw new IllegalArgumentException();
        }
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(this.ctl.get()) <= maximumPoolSize) {
            return;
        }
        interruptIdleWorkers();
    }

    public int getMaximumPoolSize() {
        return this.maximumPoolSize;
    }

    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0) {
            throw new IllegalArgumentException();
        }
        if (time == 0 && allowsCoreThreadTimeOut()) {
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        }
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta >= 0) {
            return;
        }
        interruptIdleWorkers();
    }

    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(this.keepAliveTime, TimeUnit.NANOSECONDS);
    }

    public BlockingQueue<Runnable> getQueue() {
        return this.workQueue;
    }

    public boolean remove(Runnable task) {
        boolean removed = this.workQueue.remove(task);
        tryTerminate();
        return removed;
    }

    public void purge() {
        BlockingQueue<Runnable> q = this.workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if ((r instanceof Future) && ((Future) r).isCancelled()) {
                    it.remove();
                }
            }
        } catch (ConcurrentModificationException e) {
            for (Object r2 : q.toArray()) {
                if ((r2 instanceof Future) && ((Future) r2).isCancelled()) {
                    q.remove(r2);
                }
            }
        }
        tryTerminate();
    }

    public int getPoolSize() {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return runStateAtLeast(this.ctl.get(), 1073741824) ? 0 : this.workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    public int getActiveCount() {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        int n = 0;
        try {
            for (Worker w : this.workers) {
                if (w.isLocked()) {
                    n++;
                }
            }
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    public int getLargestPoolSize() {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return this.largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    public long getTaskCount() {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = this.completedTaskCount;
            for (Worker w : this.workers) {
                n += w.completedTasks;
                if (w.isLocked()) {
                    n++;
                }
            }
            return ((long) this.workQueue.size()) + n;
        } finally {
            mainLock.unlock();
        }
    }

    public long getCompletedTaskCount() {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = this.completedTaskCount;
            for (Worker w : this.workers) {
                n += w.completedTasks;
            }
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    public String toString() {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long ncompleted = this.completedTaskCount;
            int nactive = 0;
            int nworkers = this.workers.size();
            for (Worker w : this.workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked()) {
                    nactive++;
                }
            }
            mainLock.unlock();
            int c = this.ctl.get();
            String runState = runStateLessThan(c, 0) ? "Running" : runStateAtLeast(c, TERMINATED) ? "Terminated" : "Shutting down";
            return super.toString() + "[" + runState + ", pool size = " + nworkers + ", active threads = " + nactive + ", queued tasks = " + this.workQueue.size() + ", completed tasks = " + ncompleted + "]";
        } catch (Throwable th) {
            mainLock.unlock();
            throw th;
        }
    }

    protected void beforeExecute(Thread t, Runnable r) {
    }

    protected void afterExecute(Runnable r, Throwable t) {
    }

    protected void terminated() {
    }

    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (e.isShutdown()) {
                return;
            }
            r.run();
        }
    }

    public static class AbortPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
        }
    }

    public static class DiscardPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) throws Throwable {
            if (e.isShutdown()) {
                return;
            }
            e.getQueue().poll();
            e.execute(r);
        }
    }
}
