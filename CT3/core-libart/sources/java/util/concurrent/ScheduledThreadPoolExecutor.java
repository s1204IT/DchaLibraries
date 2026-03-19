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

        ScheduledFutureTask(Runnable r, V result, long triggerTime, long sequenceNumber) {
            super(r, result);
            this.outerTask = this;
            this.time = triggerTime;
            this.period = 0L;
            this.sequenceNumber = sequenceNumber;
        }

        ScheduledFutureTask(Runnable r, V result, long triggerTime, long period, long sequenceNumber) {
            super(r, result);
            this.outerTask = this;
            this.time = triggerTime;
            this.period = period;
            this.sequenceNumber = sequenceNumber;
        }

        ScheduledFutureTask(Callable<V> callable, long triggerTime, long sequenceNumber) {
            super(callable);
            this.outerTask = this;
            this.time = triggerTime;
            this.period = 0L;
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(this.time - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) {
                return 0;
            }
            if (other instanceof ScheduledFutureTask) {
                ScheduledFutureTask<?> x = (ScheduledFutureTask) other;
                long diff = this.time - x.time;
                if (diff < 0) {
                    return -1;
                }
                return (diff <= 0 && this.sequenceNumber < x.sequenceNumber) ? -1 : 1;
            }
            long diff2 = getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
            if (diff2 < 0) {
                return -1;
            }
            return diff2 > 0 ? 1 : 0;
        }

        @Override
        public boolean isPeriodic() {
            return this.period != 0;
        }

        private void setNextRunTime() {
            long p = this.period;
            if (p > 0) {
                this.time += p;
            } else {
                this.time = ScheduledThreadPoolExecutor.this.triggerTime(-p);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (cancelled && ScheduledThreadPoolExecutor.this.removeOnCancel && this.heapIndex >= 0) {
                ScheduledThreadPoolExecutor.this.remove(this);
            }
            return cancelled;
        }

        @Override
        public void run() {
            boolean periodic = isPeriodic();
            if (!ScheduledThreadPoolExecutor.this.canRunInCurrentRunState(periodic)) {
                cancel(false);
                return;
            }
            if (!periodic) {
                super.run();
            } else {
                if (!super.runAndReset()) {
                    return;
                }
                setNextRunTime();
                ScheduledThreadPoolExecutor.this.reExecutePeriodic(this.outerTask);
            }
        }
    }

    boolean canRunInCurrentRunState(boolean periodic) {
        boolean z;
        if (periodic) {
            z = this.continueExistingPeriodicTasksAfterShutdown;
        } else {
            z = this.executeExistingDelayedTasksAfterShutdown;
        }
        return isRunningOrShutdown(z);
    }

    private void delayedExecute(RunnableScheduledFuture<?> task) {
        if (isShutdown()) {
            reject(task);
            return;
        }
        super.getQueue().add(task);
        if (isShutdown() && !canRunInCurrentRunState(task.isPeriodic()) && remove(task)) {
            task.cancel(false);
        } else {
            ensurePrestart();
        }
    }

    void reExecutePeriodic(RunnableScheduledFuture<?> task) {
        if (!canRunInCurrentRunState(true)) {
            return;
        }
        super.getQueue().add(task);
        if (!canRunInCurrentRunState(true) && remove(task)) {
            task.cancel(false);
        } else {
            ensurePrestart();
        }
    }

    @Override
    void onShutdown() {
        BlockingQueue<Runnable> q = super.getQueue();
        boolean keepDelayed = getExecuteExistingDelayedTasksAfterShutdownPolicy();
        boolean keepPeriodic = getContinueExistingPeriodicTasksAfterShutdownPolicy();
        if (!keepDelayed && !keepPeriodic) {
            for (Object e : q.toArray()) {
                if (e instanceof RunnableScheduledFuture) {
                    ((RunnableScheduledFuture) e).cancel(false);
                }
            }
            q.clear();
        } else {
            for (Object e2 : q.toArray()) {
                if (e2 instanceof RunnableScheduledFuture) {
                    RunnableScheduledFuture<?> t = (RunnableScheduledFuture) e2;
                    if (!t.isPeriodic() ? keepDelayed : keepPeriodic) {
                        if (t.isCancelled()) {
                        }
                    } else if (q.remove(t)) {
                        t.cancel(false);
                    }
                }
            }
        }
        tryTerminate();
    }

    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        return task;
    }

    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        return task;
    }

    public ScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, TimeUnit.MILLISECONDS, new DelayedWorkQueue());
        this.executeExistingDelayedTasksAfterShutdown = true;
    }

    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, TimeUnit.MILLISECONDS, new DelayedWorkQueue(), threadFactory);
        this.executeExistingDelayedTasksAfterShutdown = true;
    }

    public ScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, TimeUnit.MILLISECONDS, new DelayedWorkQueue(), handler);
        this.executeExistingDelayedTasksAfterShutdown = true;
    }

    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, Integer.MAX_VALUE, DEFAULT_KEEPALIVE_MILLIS, TimeUnit.MILLISECONDS, new DelayedWorkQueue(), threadFactory, handler);
        this.executeExistingDelayedTasksAfterShutdown = true;
    }

    private long triggerTime(long delay, TimeUnit unit) {
        if (delay < 0) {
            delay = 0;
        }
        return triggerTime(unit.toNanos(delay));
    }

    long triggerTime(long delay) {
        long jNanoTime = System.nanoTime();
        if (delay >= 4611686018427387903L) {
            delay = overflowFree(delay);
        }
        return jNanoTime + delay;
    }

    private long overflowFree(long delay) {
        Delayed head = (Delayed) super.getQueue().peek();
        if (head != null) {
            long headDelay = head.getDelay(TimeUnit.NANOSECONDS);
            if (headDelay < 0 && delay - headDelay < 0) {
                return Long.MAX_VALUE + headDelay;
            }
            return delay;
        }
        return delay;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        if (command == null || unit == null) {
            throw new NullPointerException();
        }
        RunnableScheduledFuture<Void> t = decorateTask(command, new ScheduledFutureTask(command, null, triggerTime(delay, unit), sequencer.getAndIncrement()));
        delayedExecute(t);
        return t;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        if (callable == null || unit == null) {
            throw new NullPointerException();
        }
        RunnableScheduledFuture<V> t = decorateTask(callable, new ScheduledFutureTask(callable, triggerTime(delay, unit), sequencer.getAndIncrement()));
        delayedExecute(t);
        return t;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (command == null || unit == null) {
            throw new NullPointerException();
        }
        if (period <= 0) {
            throw new IllegalArgumentException();
        }
        ScheduledFutureTask<Void> sft = new ScheduledFutureTask<>(command, null, triggerTime(initialDelay, unit), unit.toNanos(period), sequencer.getAndIncrement());
        RunnableScheduledFuture<V> runnableScheduledFutureDecorateTask = decorateTask(command, sft);
        sft.outerTask = runnableScheduledFutureDecorateTask;
        delayedExecute(runnableScheduledFutureDecorateTask);
        return runnableScheduledFutureDecorateTask;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (command == null || unit == null) {
            throw new NullPointerException();
        }
        if (delay <= 0) {
            throw new IllegalArgumentException();
        }
        ScheduledFutureTask<Void> sft = new ScheduledFutureTask<>(command, null, triggerTime(initialDelay, unit), -unit.toNanos(delay), sequencer.getAndIncrement());
        RunnableScheduledFuture<V> runnableScheduledFutureDecorateTask = decorateTask(command, sft);
        sft.outerTask = runnableScheduledFutureDecorateTask;
        delayedExecute(runnableScheduledFutureDecorateTask);
        return runnableScheduledFutureDecorateTask;
    }

    @Override
    public void execute(Runnable command) {
        schedule(command, 0L, TimeUnit.NANOSECONDS);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return schedule(task, 0L, TimeUnit.NANOSECONDS);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return schedule(Executors.callable(task, result), 0L, TimeUnit.NANOSECONDS);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return schedule(task, 0L, TimeUnit.NANOSECONDS);
    }

    public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean value) {
        this.continueExistingPeriodicTasksAfterShutdown = value;
        if (value || !isShutdown()) {
            return;
        }
        onShutdown();
    }

    public boolean getContinueExistingPeriodicTasksAfterShutdownPolicy() {
        return this.continueExistingPeriodicTasksAfterShutdown;
    }

    public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean value) {
        this.executeExistingDelayedTasksAfterShutdown = value;
        if (value || !isShutdown()) {
            return;
        }
        onShutdown();
    }

    public boolean getExecuteExistingDelayedTasksAfterShutdownPolicy() {
        return this.executeExistingDelayedTasksAfterShutdown;
    }

    public void setRemoveOnCancelPolicy(boolean value) {
        this.removeOnCancel = value;
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

        private void setIndex(RunnableScheduledFuture<?> f, int idx) {
            if (!(f instanceof ScheduledFutureTask)) {
                return;
            }
            ((ScheduledFutureTask) f).heapIndex = idx;
        }

        private void siftUp(int k, RunnableScheduledFuture<?> key) {
            while (k > 0) {
                int parent = (k - 1) >>> 1;
                RunnableScheduledFuture<?> e = this.queue[parent];
                if (key.compareTo(e) >= 0) {
                    break;
                }
                this.queue[k] = e;
                setIndex(e, k);
                k = parent;
            }
            this.queue[k] = key;
            setIndex(key, k);
        }

        private void siftDown(int k, RunnableScheduledFuture<?> key) {
            int half = this.size >>> 1;
            while (k < half) {
                int child = (k << 1) + 1;
                RunnableScheduledFuture<?> c = this.queue[child];
                int right = child + 1;
                if (right < this.size && c.compareTo(this.queue[right]) > 0) {
                    child = right;
                    c = this.queue[right];
                }
                if (key.compareTo(c) <= 0) {
                    break;
                }
                this.queue[k] = c;
                setIndex(c, k);
                k = child;
            }
            this.queue[k] = key;
            setIndex(key, k);
        }

        private void grow() {
            int oldCapacity = this.queue.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity < 0) {
                newCapacity = Integer.MAX_VALUE;
            }
            this.queue = (RunnableScheduledFuture[]) Arrays.copyOf(this.queue, newCapacity);
        }

        private int indexOf(Object x) {
            if (x != null) {
                if (x instanceof ScheduledFutureTask) {
                    int i = ((ScheduledFutureTask) x).heapIndex;
                    if (i >= 0 && i < this.size && this.queue[i] == x) {
                        return i;
                    }
                    return -1;
                }
                for (int i2 = 0; i2 < this.size; i2++) {
                    if (x.equals(this.queue[i2])) {
                        return i2;
                    }
                }
                return -1;
            }
            return -1;
        }

        @Override
        public boolean contains(Object x) {
            ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return indexOf(x) != -1;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean remove(Object x) {
            ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i = indexOf(x);
                if (i < 0) {
                    return false;
                }
                setIndex(this.queue[i], -1);
                int s = this.size - 1;
                this.size = s;
                RunnableScheduledFuture<?> replacement = this.queue[s];
                this.queue[s] = null;
                if (s != i) {
                    siftDown(i, replacement);
                    if (this.queue[i] == replacement) {
                        siftUp(i, replacement);
                    }
                }
                return true;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public int size() {
            ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return this.size;
            } finally {
                lock.unlock();
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
            ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return this.queue[0];
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean offer(Runnable x) {
            if (x == null) {
                throw new NullPointerException();
            }
            RunnableScheduledFuture<?> e = (RunnableScheduledFuture) x;
            ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i = this.size;
                if (i >= this.queue.length) {
                    grow();
                }
                this.size = i + 1;
                if (i == 0) {
                    this.queue[0] = e;
                    setIndex(e, 0);
                } else {
                    siftUp(i, e);
                }
                if (this.queue[0] == e) {
                    this.leader = null;
                    this.available.signal();
                }
                lock.unlock();
                return true;
            } catch (Throwable th) {
                lock.unlock();
                throw th;
            }
        }

        @Override
        public void put(Runnable e) {
            offer(e);
        }

        @Override
        public boolean add(Runnable e) {
            return offer(e);
        }

        @Override
        public boolean offer(Runnable e, long timeout, TimeUnit unit) {
            return offer(e);
        }

        private RunnableScheduledFuture<?> finishPoll(RunnableScheduledFuture<?> f) {
            int s = this.size - 1;
            this.size = s;
            RunnableScheduledFuture<?> x = this.queue[s];
            this.queue[s] = null;
            if (s != 0) {
                siftDown(0, x);
            }
            setIndex(f, -1);
            return f;
        }

        @Override
        public RunnableScheduledFuture<?> poll() {
            RunnableScheduledFuture<?> runnableScheduledFutureFinishPoll = null;
            ReentrantLock lock = this.lock;
            lock.lock();
            try {
                RunnableScheduledFuture<?> first = this.queue[0];
                if (first != null && first.getDelay(TimeUnit.NANOSECONDS) <= 0) {
                    runnableScheduledFutureFinishPoll = finishPoll(first);
                }
                return runnableScheduledFutureFinishPoll;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public Runnable take() throws InterruptedException {
            RunnableScheduledFuture<?> first;
            ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            while (true) {
                try {
                    first = this.queue[0];
                    if (first == null) {
                        this.available.await();
                    } else {
                        long delay = first.getDelay(TimeUnit.NANOSECONDS);
                        if (delay <= 0) {
                            break;
                        }
                        if (this.leader != null) {
                            this.available.await();
                        } else {
                            Thread thisThread = Thread.currentThread();
                            this.leader = thisThread;
                            try {
                                this.available.awaitNanos(delay);
                                if (this.leader == thisThread) {
                                    this.leader = null;
                                }
                            } catch (Throwable th) {
                                if (this.leader == thisThread) {
                                    this.leader = null;
                                }
                                throw th;
                            }
                        }
                    }
                } finally {
                    if (this.leader == null && this.queue[0] != null) {
                        this.available.signal();
                    }
                    lock.unlock();
                }
            }
            return finishPoll(first);
        }

        @Override
        public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
            long nanos = unit.toNanos(timeout);
            ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            while (true) {
                try {
                    RunnableScheduledFuture<?> first = this.queue[0];
                    if (first != null) {
                        long delay = first.getDelay(TimeUnit.NANOSECONDS);
                        if (delay <= 0) {
                            RunnableScheduledFuture<?> runnableScheduledFutureFinishPoll = finishPoll(first);
                            if (this.leader == null && this.queue[0] != null) {
                                this.available.signal();
                            }
                            lock.unlock();
                            return runnableScheduledFutureFinishPoll;
                        }
                        if (nanos <= 0) {
                            if (this.leader == null && this.queue[0] != null) {
                                this.available.signal();
                            }
                            lock.unlock();
                            return null;
                        }
                        if (nanos < delay || this.leader != null) {
                            nanos = this.available.awaitNanos(nanos);
                        } else {
                            Thread thisThread = Thread.currentThread();
                            this.leader = thisThread;
                            try {
                                long timeLeft = this.available.awaitNanos(delay);
                                nanos -= delay - timeLeft;
                                if (this.leader == thisThread) {
                                    this.leader = null;
                                }
                            } catch (Throwable th) {
                                if (this.leader == thisThread) {
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
                    if (this.leader == null && this.queue[(char) 0] != null) {
                        this.available.signal();
                    }
                    lock.unlock();
                }
            }
        }

        @Override
        public void clear() {
            ReentrantLock lock = this.lock;
            lock.lock();
            for (int i = 0; i < this.size; i++) {
                try {
                    RunnableScheduledFuture<?> t = this.queue[i];
                    if (t != null) {
                        this.queue[i] = null;
                        setIndex(t, -1);
                    }
                } finally {
                    lock.unlock();
                }
            }
            this.size = 0;
        }

        private RunnableScheduledFuture<?> peekExpired() {
            RunnableScheduledFuture<?> first = this.queue[0];
            if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0) {
                return null;
            }
            return first;
        }

        @Override
        public int drainTo(Collection<? super Runnable> c) {
            if (c == null) {
                throw new NullPointerException();
            }
            if (c == this) {
                throw new IllegalArgumentException();
            }
            ReentrantLock lock = this.lock;
            lock.lock();
            int n = 0;
            while (true) {
                try {
                    RunnableScheduledFuture<?> first = peekExpired();
                    if (first != null) {
                        c.add(first);
                        finishPoll(first);
                        n++;
                    } else {
                        return n;
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        @Override
        public int drainTo(Collection<? super Runnable> c, int maxElements) {
            if (c == null) {
                throw new NullPointerException();
            }
            if (c == this) {
                throw new IllegalArgumentException();
            }
            if (maxElements <= 0) {
                return 0;
            }
            ReentrantLock lock = this.lock;
            lock.lock();
            int n = 0;
            while (n < maxElements) {
                try {
                    RunnableScheduledFuture<?> first = peekExpired();
                    if (first == null) {
                        break;
                    }
                    c.add(first);
                    finishPoll(first);
                    n++;
                } finally {
                    lock.unlock();
                }
            }
            return n;
        }

        @Override
        public Object[] toArray() {
            ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return Arrays.copyOf(this.queue, this.size, Object[].class);
            } finally {
                lock.unlock();
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

            Itr(RunnableScheduledFuture<?>[] array) {
                this.array = array;
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
