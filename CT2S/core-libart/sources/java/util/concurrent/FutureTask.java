package java.util.concurrent;

import java.util.concurrent.locks.LockSupport;
import sun.misc.Unsafe;

public class FutureTask<V> implements RunnableFuture<V> {
    private static final int CANCELLED = 4;
    private static final int COMPLETING = 1;
    private static final int EXCEPTIONAL = 3;
    private static final int INTERRUPTED = 6;
    private static final int INTERRUPTING = 5;
    private static final int NEW = 0;
    private static final int NORMAL = 2;
    private static final Unsafe UNSAFE;
    private static final long runnerOffset;
    private static final long stateOffset;
    private static final long waitersOffset;
    private Callable<V> callable;
    private Object outcome;
    private volatile Thread runner;
    private volatile int state;
    private volatile WaitNode waiters;

    private V report(int i) throws ExecutionException {
        V v = (V) this.outcome;
        if (i == 2) {
            return v;
        }
        if (i >= 4) {
            throw new CancellationException();
        }
        throw new ExecutionException((Throwable) v);
    }

    public FutureTask(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        this.callable = callable;
        this.state = 0;
    }

    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result);
        this.state = 0;
    }

    @Override
    public boolean isCancelled() {
        return this.state >= 4;
    }

    @Override
    public boolean isDone() {
        return this.state != 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (this.state != 0) {
            return false;
        }
        if (!UNSAFE.compareAndSwapInt(this, stateOffset, 0, mayInterruptIfRunning ? 5 : 4)) {
            return false;
        }
        try {
            if (mayInterruptIfRunning) {
                try {
                    Thread t = this.runner;
                    if (t != null) {
                        t.interrupt();
                    }
                } finally {
                    UNSAFE.putOrderedInt(this, stateOffset, 6);
                }
            }
            finishCompletion();
            return true;
        } catch (Throwable th) {
            finishCompletion();
            throw th;
        }
    }

    @Override
    public V get() throws ExecutionException, InterruptedException {
        int s = this.state;
        if (s <= 1) {
            s = awaitDone(false, 0L);
        }
        return report(s);
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        if (unit == null) {
            throw new NullPointerException();
        }
        int s = this.state;
        if (s <= 1 && (s = awaitDone(true, unit.toNanos(timeout))) <= 1) {
            throw new TimeoutException();
        }
        return report(s);
    }

    protected void done() {
    }

    protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, 0, 1)) {
            this.outcome = v;
            UNSAFE.putOrderedInt(this, stateOffset, 2);
            finishCompletion();
        }
    }

    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, 0, 1)) {
            this.outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, 3);
            finishCompletion();
        }
    }

    @Override
    public void run() {
        V result;
        boolean ran;
        if (this.state == 0 && UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread())) {
            try {
                Callable<V> c = this.callable;
                if (c != null && this.state == 0) {
                    try {
                        result = c.call();
                        ran = true;
                    } catch (Throwable ex) {
                        result = null;
                        ran = false;
                        setException(ex);
                    }
                    if (ran) {
                        set(result);
                    }
                }
            } finally {
                this.runner = null;
                int s = this.state;
                if (s >= 5) {
                    handlePossibleCancellationInterrupt(s);
                }
            }
        }
    }

    protected boolean runAndReset() {
        int s;
        if (this.state != 0 || !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread())) {
            return false;
        }
        boolean ran = false;
        int s2 = this.state;
        try {
            Callable<V> c = this.callable;
            if (c != null && s2 == 0) {
                try {
                    c.call();
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
            return ran && s == 0;
        } finally {
            this.runner = null;
            s = this.state;
            if (s >= 5) {
                handlePossibleCancellationInterrupt(s);
            }
        }
    }

    private void handlePossibleCancellationInterrupt(int s) {
        if (s == 5) {
            while (this.state == 5) {
                Thread.yield();
            }
        }
    }

    static final class WaitNode {
        volatile WaitNode next;
        volatile Thread thread = Thread.currentThread();

        WaitNode() {
        }
    }

    private void finishCompletion() {
        while (true) {
            WaitNode q = this.waiters;
            if (q == null) {
                break;
            }
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                while (true) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    if (next == null) {
                        break;
                    }
                    q.next = null;
                    q = next;
                }
            }
        }
        done();
        this.callable = null;
    }

    private int awaitDone(boolean timed, long nanos) throws InterruptedException {
        long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        while (!Thread.interrupted()) {
            int s = this.state;
            if (s > 1) {
                if (q != null) {
                    q.thread = null;
                    return s;
                }
                return s;
            }
            if (s == 1) {
                Thread.yield();
            } else if (q == null) {
                q = new WaitNode();
            } else if (!queued) {
                Unsafe unsafe = UNSAFE;
                long j = waitersOffset;
                WaitNode waitNode = this.waiters;
                q.next = waitNode;
                queued = unsafe.compareAndSwapObject(this, j, waitNode, q);
            } else if (timed) {
                long nanos2 = deadline - System.nanoTime();
                if (nanos2 <= 0) {
                    removeWaiter(q);
                    return this.state;
                }
                LockSupport.parkNanos(this, nanos2);
            } else {
                LockSupport.park(this);
            }
        }
        removeWaiter(q);
        throw new InterruptedException();
    }

    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            while (true) {
                WaitNode pred = null;
                WaitNode q = this.waiters;
                while (q != null) {
                    WaitNode s = q.next;
                    if (q.thread != null) {
                        pred = q;
                    } else if (pred != null) {
                        pred.next = s;
                        if (pred.thread == null) {
                            break;
                        }
                    } else if (!UNSAFE.compareAndSwapObject(this, waitersOffset, q, s)) {
                        break;
                    }
                    q = s;
                }
                return;
            }
        }
    }

    static {
        try {
            UNSAFE = Unsafe.getUnsafe();
            stateOffset = UNSAFE.objectFieldOffset(FutureTask.class.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset(FutureTask.class.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset(FutureTask.class.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
