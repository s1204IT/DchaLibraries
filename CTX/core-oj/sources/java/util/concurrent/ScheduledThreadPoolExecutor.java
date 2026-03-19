package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor implements ScheduledExecutorService {
    private static final long DEFAULT_KEEPALIVE_MILLIS = 10;
    private static final AtomicLong sequencer = new AtomicLong();
    private volatile boolean continueExistingPeriodicTasksAfterShutdown;
    private volatile boolean executeExistingDelayedTasksAfterShutdown;
    volatile boolean removeOnCancel;

    private class ScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {
        int heapIndex;
        RunnableScheduledFuture<V> outerTask;
        private final long period;
        private final long sequenceNumber;
        private volatile long time;

        ScheduledFutureTask(Runnable runnable, V v, long j, long j2) {
            super(runnable, v);
            this.outerTask = this;
            this.time = j;
            this.period = 0L;
            this.sequenceNumber = j2;
        }

        ScheduledFutureTask(Runnable runnable, V v, long j, long j2, long j3) {
            super(runnable, v);
            this.outerTask = this;
            this.time = j;
            this.period = j2;
            this.sequenceNumber = j3;
        }

        ScheduledFutureTask(Callable<V> callable, long j, long j2) {
            super(callable);
            this.outerTask = this;
            this.time = j;
            this.period = 0L;
            this.sequenceNumber = j2;
        }

        @Override
        public long getDelay(TimeUnit timeUnit) {
            return timeUnit.convert(this.time - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed delayed) {
            if (delayed == this) {
                return 0;
            }
            if (delayed instanceof ScheduledFutureTask) {
                ScheduledFutureTask scheduledFutureTask = (ScheduledFutureTask) delayed;
                long j = this.time - scheduledFutureTask.time;
                if (j < 0) {
                    return -1;
                }
                return (j <= 0 && this.sequenceNumber < scheduledFutureTask.sequenceNumber) ? -1 : 1;
            }
            long delay = getDelay(TimeUnit.NANOSECONDS) - delayed.getDelay(TimeUnit.NANOSECONDS);
            if (delay < 0) {
                return -1;
            }
            return delay > 0 ? 1 : 0;
        }

        @Override
        public boolean isPeriodic() {
            return this.period != 0;
        }

        private void setNextRunTime() {
            long j = this.period;
            if (j > 0) {
                this.time += j;
            } else {
                this.time = ScheduledThreadPoolExecutor.this.triggerTime(-j);
            }
        }

        @Override
        public boolean cancel(boolean z) {
            boolean zCancel = super.cancel(z);
            if (zCancel && ScheduledThreadPoolExecutor.this.removeOnCancel && this.heapIndex >= 0) {
                ScheduledThreadPoolExecutor.this.remove(this);
            }
            return zCancel;
        }

        @Override
        public void run() {
            boolean zIsPeriodic = isPeriodic();
            if (!ScheduledThreadPoolExecutor.this.canRunInCurrentRunState(zIsPeriodic)) {
                cancel(false);
                return;
            }
            if (!zIsPeriodic) {
                super.run();
            } else if (super.runAndReset()) {
                setNextRunTime();
                ScheduledThreadPoolExecutor.this.reExecutePeriodic(this.outerTask);
            }
        }
    }

    boolean canRunInCurrentRunState(boolean z) {
        boolean z2;
        if (z) {
            z2 = this.continueExistingPeriodicTasksAfterShutdown;
        } else {
            z2 = this.executeExistingDelayedTasksAfterShutdown;
        }
        return isRunningOrShutdown(z2);
    }

    private void delayedExecute(RunnableScheduledFuture<?> runnableScheduledFuture) {
        if (isShutdown()) {
            reject(runnableScheduledFuture);
            return;
        }
        super.getQueue().add(runnableScheduledFuture);
        if (isShutdown() && !canRunInCurrentRunState(runnableScheduledFuture.isPeriodic()) && remove(runnableScheduledFuture)) {
            runnableScheduledFuture.cancel(false);
        } else {
            ensurePrestart();
        }
    }

    void reExecutePeriodic(RunnableScheduledFuture<?> runnableScheduledFuture) {
        if (canRunInCurrentRunState(true)) {
            super.getQueue().add(runnableScheduledFuture);
            if (!canRunInCurrentRunState(true) && remove(runnableScheduledFuture)) {
                runnableScheduledFuture.cancel(false);
            } else {
                ensurePrestart();
            }
        }
    }

    @Override
    void onShutdown() {
        BlockingQueue<Runnable> queue = super.getQueue();
        boolean executeExistingDelayedTasksAfterShutdownPolicy = getExecuteExistingDelayedTasksAfterShutdownPolicy();
        boolean continueExistingPeriodicTasksAfterShutdownPolicy = getContinueExistingPeriodicTasksAfterShutdownPolicy();
        if (!executeExistingDelayedTasksAfterShutdownPolicy && !continueExistingPeriodicTasksAfterShutdownPolicy) {
            for (Object obj : queue.toArray()) {
                if (obj instanceof RunnableScheduledFuture) {
                    ((RunnableScheduledFuture) obj).cancel(false);
                }
            }
            queue.clear();
        } else {
            for (Object obj2 : queue.toArray()) {
                if (obj2 instanceof RunnableScheduledFuture) {
                    RunnableScheduledFuture runnableScheduledFuture = (RunnableScheduledFuture) obj2;
                    if (!runnableScheduledFuture.isPeriodic() ? executeExistingDelayedTasksAfterShutdownPolicy : continueExistingPeriodicTasksAfterShutdownPolicy) {
                        if (runnableScheduledFuture.isCancelled()) {
                        }
                    } else if (queue.remove(runnableScheduledFuture)) {
                        runnableScheduledFuture.cancel(false);
                    }
                }
            }
        }
        tryTerminate();
    }

    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> runnableScheduledFuture) {
        return runnableScheduledFuture;
    }

    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> runnableScheduledFuture) {
        return runnableScheduledFuture;
    }

    public ScheduledThreadPoolExecutor(int i) {
        super(i, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, TimeUnit.MILLISECONDS, new DelayedWorkQueue());
        this.executeExistingDelayedTasksAfterShutdown = true;
    }

    public ScheduledThreadPoolExecutor(int i, ThreadFactory threadFactory) {
        super(i, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, TimeUnit.MILLISECONDS, new DelayedWorkQueue(), threadFactory);
        this.executeExistingDelayedTasksAfterShutdown = true;
    }

    public ScheduledThreadPoolExecutor(int i, RejectedExecutionHandler rejectedExecutionHandler) {
        super(i, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, TimeUnit.MILLISECONDS, new DelayedWorkQueue(), rejectedExecutionHandler);
        this.executeExistingDelayedTasksAfterShutdown = true;
    }

    public ScheduledThreadPoolExecutor(int i, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        super(i, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, TimeUnit.MILLISECONDS, new DelayedWorkQueue(), threadFactory, rejectedExecutionHandler);
        this.executeExistingDelayedTasksAfterShutdown = true;
    }

    private long triggerTime(long j, TimeUnit timeUnit) {
        if (j < 0) {
            j = 0;
        }
        return triggerTime(timeUnit.toNanos(j));
    }

    long triggerTime(long j) {
        long jNanoTime = System.nanoTime();
        if (j >= 4611686018427387903L) {
            j = overflowFree(j);
        }
        return jNanoTime + j;
    }

    private long overflowFree(long j) {
        Delayed delayed = (Delayed) super.getQueue().peek();
        if (delayed != null) {
            long delay = delayed.getDelay(TimeUnit.NANOSECONDS);
            if (delay < 0 && j - delay < 0) {
                return Long.MAX_VALUE + delay;
            }
            return j;
        }
        return j;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long j, TimeUnit timeUnit) {
        if (runnable == null || timeUnit == null) {
            throw new NullPointerException();
        }
        RunnableScheduledFuture<?> runnableScheduledFutureDecorateTask = decorateTask(runnable, new ScheduledFutureTask(runnable, null, triggerTime(j, timeUnit), sequencer.getAndIncrement()));
        delayedExecute(runnableScheduledFutureDecorateTask);
        return runnableScheduledFutureDecorateTask;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long j, TimeUnit timeUnit) {
        if (callable == null || timeUnit == null) {
            throw new NullPointerException();
        }
        RunnableScheduledFuture<V> runnableScheduledFutureDecorateTask = decorateTask(callable, new ScheduledFutureTask(callable, triggerTime(j, timeUnit), sequencer.getAndIncrement()));
        delayedExecute(runnableScheduledFutureDecorateTask);
        return runnableScheduledFutureDecorateTask;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long j, long j2, TimeUnit timeUnit) {
        if (runnable == null || timeUnit == null) {
            throw new NullPointerException();
        }
        if (j2 <= 0) {
            throw new IllegalArgumentException();
        }
        ScheduledFutureTask scheduledFutureTask = new ScheduledFutureTask(runnable, null, triggerTime(j, timeUnit), timeUnit.toNanos(j2), sequencer.getAndIncrement());
        RunnableScheduledFuture<V> runnableScheduledFutureDecorateTask = decorateTask(runnable, scheduledFutureTask);
        scheduledFutureTask.outerTask = runnableScheduledFutureDecorateTask;
        delayedExecute(runnableScheduledFutureDecorateTask);
        return runnableScheduledFutureDecorateTask;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long j, long j2, TimeUnit timeUnit) {
        if (runnable == null || timeUnit == null) {
            throw new NullPointerException();
        }
        if (j2 <= 0) {
            throw new IllegalArgumentException();
        }
        ScheduledFutureTask scheduledFutureTask = new ScheduledFutureTask(runnable, null, triggerTime(j, timeUnit), -timeUnit.toNanos(j2), sequencer.getAndIncrement());
        RunnableScheduledFuture<V> runnableScheduledFutureDecorateTask = decorateTask(runnable, scheduledFutureTask);
        scheduledFutureTask.outerTask = runnableScheduledFutureDecorateTask;
        delayedExecute(runnableScheduledFutureDecorateTask);
        return runnableScheduledFutureDecorateTask;
    }

    @Override
    public void execute(Runnable runnable) {
        schedule(runnable, 0L, TimeUnit.NANOSECONDS);
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return schedule(runnable, 0L, TimeUnit.NANOSECONDS);
    }

    @Override
    public <T> Future<T> submit(Runnable runnable, T t) {
        return schedule(Executors.callable(runnable, t), 0L, TimeUnit.NANOSECONDS);
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        return schedule(callable, 0L, TimeUnit.NANOSECONDS);
    }

    public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean z) {
        this.continueExistingPeriodicTasksAfterShutdown = z;
        if (!z && isShutdown()) {
            onShutdown();
        }
    }

    public boolean getContinueExistingPeriodicTasksAfterShutdownPolicy() {
        return this.continueExistingPeriodicTasksAfterShutdown;
    }

    public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean z) {
        this.executeExistingDelayedTasksAfterShutdown = z;
        if (!z && isShutdown()) {
            onShutdown();
        }
    }

    public boolean getExecuteExistingDelayedTasksAfterShutdownPolicy() {
        return this.executeExistingDelayedTasksAfterShutdown;
    }

    public void setRemoveOnCancelPolicy(boolean z) {
        this.removeOnCancel = z;
    }

    public boolean getRemoveOnCancelPolicy() {
        return this.removeOnCancel;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return super.shutdownNow();
    }

    @Override
    public BlockingQueue<Runnable> getQueue() {
        return super.getQueue();
    }

    static class DelayedWorkQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable> {
        private static final int INITIAL_CAPACITY = 16;
        private Thread leader;
        private int size;
        private RunnableScheduledFuture<?>[] queue = new RunnableScheduledFuture[16];
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition available = this.lock.newCondition();

        DelayedWorkQueue() {
        }

        private void setIndex(RunnableScheduledFuture<?> runnableScheduledFuture, int i) {
            if (runnableScheduledFuture instanceof ScheduledFutureTask) {
                ((ScheduledFutureTask) runnableScheduledFuture).heapIndex = i;
            }
        }

        private void siftUp(int i, RunnableScheduledFuture<?> runnableScheduledFuture) {
            while (i > 0) {
                int i2 = (i - 1) >>> 1;
                RunnableScheduledFuture<?> runnableScheduledFuture2 = this.queue[i2];
                if (runnableScheduledFuture.compareTo(runnableScheduledFuture2) >= 0) {
                    break;
                }
                this.queue[i] = runnableScheduledFuture2;
                setIndex(runnableScheduledFuture2, i);
                i = i2;
            }
            this.queue[i] = runnableScheduledFuture;
            setIndex(runnableScheduledFuture, i);
        }

        private void siftDown(int i, RunnableScheduledFuture<?> runnableScheduledFuture) {
            int i2 = this.size >>> 1;
            while (i < i2) {
                int i3 = (i << 1) + 1;
                RunnableScheduledFuture<?> runnableScheduledFuture2 = this.queue[i3];
                int i4 = i3 + 1;
                if (i4 < this.size && runnableScheduledFuture2.compareTo(this.queue[i4]) > 0) {
                    runnableScheduledFuture2 = this.queue[i4];
                    i3 = i4;
                }
                if (runnableScheduledFuture.compareTo(runnableScheduledFuture2) <= 0) {
                    break;
                }
                this.queue[i] = runnableScheduledFuture2;
                setIndex(runnableScheduledFuture2, i);
                i = i3;
            }
            this.queue[i] = runnableScheduledFuture;
            setIndex(runnableScheduledFuture, i);
        }

        private void grow() {
            int length = this.queue.length;
            int i = length + (length >> 1);
            if (i < 0) {
                i = Integer.MAX_VALUE;
            }
            this.queue = (RunnableScheduledFuture[]) Arrays.copyOf(this.queue, i);
        }

        private int indexOf(Object obj) {
            if (obj != null) {
                if (obj instanceof ScheduledFutureTask) {
                    int i = ((ScheduledFutureTask) obj).heapIndex;
                    if (i >= 0 && i < this.size && this.queue[i] == obj) {
                        return i;
                    }
                    return -1;
                }
                for (int i2 = 0; i2 < this.size; i2++) {
                    if (obj.equals(this.queue[i2])) {
                        return i2;
                    }
                }
                return -1;
            }
            return -1;
        }

        @Override
        public boolean contains(Object obj) {
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            try {
                return indexOf(obj) != -1;
            } finally {
                reentrantLock.unlock();
            }
        }

        @Override
        public boolean remove(Object obj) {
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            try {
                int iIndexOf = indexOf(obj);
                if (iIndexOf >= 0) {
                    setIndex(this.queue[iIndexOf], -1);
                    int i = this.size - 1;
                    this.size = i;
                    RunnableScheduledFuture<?> runnableScheduledFuture = this.queue[i];
                    this.queue[i] = null;
                    if (i != iIndexOf) {
                        siftDown(iIndexOf, runnableScheduledFuture);
                        if (this.queue[iIndexOf] == runnableScheduledFuture) {
                            siftUp(iIndexOf, runnableScheduledFuture);
                        }
                    }
                    return true;
                }
                return false;
            } finally {
                reentrantLock.unlock();
            }
        }

        @Override
        public int size() {
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            try {
                return this.size;
            } finally {
                reentrantLock.unlock();
            }
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public int remainingCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public RunnableScheduledFuture<?> peek() {
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            try {
                return this.queue[0];
            } finally {
                reentrantLock.unlock();
            }
        }

        @Override
        public boolean offer(Runnable runnable) {
            if (runnable == null) {
                throw new NullPointerException();
            }
            RunnableScheduledFuture<?> runnableScheduledFuture = (RunnableScheduledFuture) runnable;
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            try {
                int i = this.size;
                if (i >= this.queue.length) {
                    grow();
                }
                this.size = i + 1;
                if (i == 0) {
                    this.queue[0] = runnableScheduledFuture;
                    setIndex(runnableScheduledFuture, 0);
                } else {
                    siftUp(i, runnableScheduledFuture);
                }
                if (this.queue[0] == runnableScheduledFuture) {
                    this.leader = null;
                    this.available.signal();
                }
                reentrantLock.unlock();
                return true;
            } catch (Throwable th) {
                reentrantLock.unlock();
                throw th;
            }
        }

        @Override
        public void put(Runnable runnable) {
            offer(runnable);
        }

        @Override
        public boolean add(Runnable runnable) {
            return offer(runnable);
        }

        @Override
        public boolean offer(Runnable runnable, long j, TimeUnit timeUnit) {
            return offer(runnable);
        }

        private RunnableScheduledFuture<?> finishPoll(RunnableScheduledFuture<?> runnableScheduledFuture) {
            int i = this.size - 1;
            this.size = i;
            RunnableScheduledFuture<?> runnableScheduledFuture2 = this.queue[i];
            this.queue[i] = null;
            if (i != 0) {
                siftDown(0, runnableScheduledFuture2);
            }
            setIndex(runnableScheduledFuture, -1);
            return runnableScheduledFuture;
        }

        @Override
        public RunnableScheduledFuture<?> poll() {
            RunnableScheduledFuture<?> runnableScheduledFutureFinishPoll;
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            try {
                RunnableScheduledFuture<?> runnableScheduledFuture = this.queue[0];
                if (runnableScheduledFuture == null || runnableScheduledFuture.getDelay(TimeUnit.NANOSECONDS) > 0) {
                    runnableScheduledFutureFinishPoll = null;
                } else {
                    runnableScheduledFutureFinishPoll = finishPoll(runnableScheduledFuture);
                }
                return runnableScheduledFutureFinishPoll;
            } finally {
                reentrantLock.unlock();
            }
        }

        @Override
        public Runnable take() throws InterruptedException {
            RunnableScheduledFuture<?> runnableScheduledFuture;
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lockInterruptibly();
            while (true) {
                try {
                    runnableScheduledFuture = this.queue[0];
                    if (runnableScheduledFuture == null) {
                        this.available.await();
                    } else {
                        long delay = runnableScheduledFuture.getDelay(TimeUnit.NANOSECONDS);
                        if (delay <= 0) {
                            break;
                        }
                        if (this.leader != null) {
                            this.available.await();
                        } else {
                            Thread threadCurrentThread = Thread.currentThread();
                            this.leader = threadCurrentThread;
                            try {
                                this.available.awaitNanos(delay);
                            } finally {
                                if (this.leader == threadCurrentThread) {
                                    this.leader = null;
                                }
                            }
                        }
                    }
                } finally {
                    if (this.leader == null && this.queue[0] != null) {
                        this.available.signal();
                    }
                    reentrantLock.unlock();
                }
            }
            return finishPoll(runnableScheduledFuture);
        }

        @Override
        public Runnable poll(long j, TimeUnit timeUnit) throws InterruptedException {
            long nanos = timeUnit.toNanos(j);
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lockInterruptibly();
            while (true) {
                try {
                    RunnableScheduledFuture<?> runnableScheduledFuture = this.queue[0];
                    if (runnableScheduledFuture != null) {
                        long delay = runnableScheduledFuture.getDelay(TimeUnit.NANOSECONDS);
                        if (delay <= 0) {
                            RunnableScheduledFuture<?> runnableScheduledFutureFinishPoll = finishPoll(runnableScheduledFuture);
                            if (this.leader == null && this.queue[0] != null) {
                                this.available.signal();
                            }
                            reentrantLock.unlock();
                            return runnableScheduledFutureFinishPoll;
                        }
                        if (nanos <= 0) {
                            if (this.leader == null && this.queue[0] != null) {
                                this.available.signal();
                            }
                            reentrantLock.unlock();
                            return null;
                        }
                        if (nanos < delay || this.leader != null) {
                            nanos = this.available.awaitNanos(nanos);
                        } else {
                            Thread threadCurrentThread = Thread.currentThread();
                            this.leader = threadCurrentThread;
                            try {
                                nanos -= delay - this.available.awaitNanos(delay);
                                if (this.leader == threadCurrentThread) {
                                    this.leader = null;
                                }
                            } catch (Throwable th) {
                                if (this.leader == threadCurrentThread) {
                                    this.leader = null;
                                }
                                throw th;
                            }
                        }
                    } else {
                        if (nanos <= 0) {
                            return null;
                        }
                        nanos = this.available.awaitNanos(nanos);
                    }
                } finally {
                    if (this.leader == null && this.queue[0] != null) {
                        this.available.signal();
                    }
                    reentrantLock.unlock();
                }
            }
        }

        @Override
        public void clear() {
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            for (int i = 0; i < this.size; i++) {
                try {
                    RunnableScheduledFuture<?> runnableScheduledFuture = this.queue[i];
                    if (runnableScheduledFuture != null) {
                        this.queue[i] = null;
                        setIndex(runnableScheduledFuture, -1);
                    }
                } finally {
                    reentrantLock.unlock();
                }
            }
            this.size = 0;
        }

        private RunnableScheduledFuture<?> peekExpired() {
            RunnableScheduledFuture<?> runnableScheduledFuture = this.queue[0];
            if (runnableScheduledFuture != null && runnableScheduledFuture.getDelay(TimeUnit.NANOSECONDS) <= 0) {
                return runnableScheduledFuture;
            }
            return null;
        }

        @Override
        public int drainTo(Collection<? super Runnable> collection) {
            if (collection == null) {
                throw new NullPointerException();
            }
            if (collection == this) {
                throw new IllegalArgumentException();
            }
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            int i = 0;
            while (true) {
                try {
                    RunnableScheduledFuture<?> runnableScheduledFuturePeekExpired = peekExpired();
                    if (runnableScheduledFuturePeekExpired != null) {
                        collection.add(runnableScheduledFuturePeekExpired);
                        finishPoll(runnableScheduledFuturePeekExpired);
                        i++;
                    } else {
                        return i;
                    }
                } finally {
                    reentrantLock.unlock();
                }
            }
        }

        @Override
        public int drainTo(Collection<? super Runnable> collection, int i) {
            if (collection == null) {
                throw new NullPointerException();
            }
            if (collection == this) {
                throw new IllegalArgumentException();
            }
            int i2 = 0;
            if (i <= 0) {
                return 0;
            }
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            while (i2 < i) {
                try {
                    RunnableScheduledFuture<?> runnableScheduledFuturePeekExpired = peekExpired();
                    if (runnableScheduledFuturePeekExpired == null) {
                        break;
                    }
                    collection.add(runnableScheduledFuturePeekExpired);
                    finishPoll(runnableScheduledFuturePeekExpired);
                    i2++;
                } finally {
                    reentrantLock.unlock();
                }
            }
            return i2;
        }

        @Override
        public Object[] toArray() {
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            try {
                return Arrays.copyOf(this.queue, this.size, Object[].class);
            } finally {
                reentrantLock.unlock();
            }
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            ReentrantLock reentrantLock = this.lock;
            reentrantLock.lock();
            try {
                if (tArr.length < this.size) {
                    return (T[]) Arrays.copyOf(this.queue, this.size, tArr.getClass());
                }
                System.arraycopy(this.queue, 0, tArr, 0, this.size);
                if (tArr.length > this.size) {
                    tArr[this.size] = null;
                }
                return tArr;
            } finally {
                reentrantLock.unlock();
            }
        }

        @Override
        public Iterator<Runnable> iterator() {
            return new Itr((RunnableScheduledFuture[]) Arrays.copyOf(this.queue, this.size));
        }

        private class Itr implements Iterator<Runnable> {
            final RunnableScheduledFuture<?>[] array;
            int cursor;
            int lastRet = -1;

            Itr(RunnableScheduledFuture<?>[] runnableScheduledFutureArr) {
                this.array = runnableScheduledFutureArr;
            }

            @Override
            public boolean hasNext() {
                return this.cursor < this.array.length;
            }

            @Override
            public Runnable next() {
                if (this.cursor >= this.array.length) {
                    throw new NoSuchElementException();
                }
                this.lastRet = this.cursor;
                RunnableScheduledFuture<?>[] runnableScheduledFutureArr = this.array;
                int i = this.cursor;
                this.cursor = i + 1;
                return runnableScheduledFutureArr[i];
            }

            @Override
            public void remove() {
                if (this.lastRet < 0) {
                    throw new IllegalStateException();
                }
                DelayedWorkQueue.this.remove(this.array[this.lastRet]);
                this.lastRet = -1;
            }
        }
    }
}
