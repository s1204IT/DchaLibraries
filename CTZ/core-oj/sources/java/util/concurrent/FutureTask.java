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
    private static final long RUNNER;
    private static final long STATE;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long WAITERS;
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

    public FutureTask(Runnable runnable, V v) {
        this.callable = Executors.callable(runnable, v);
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
    public boolean cancel(boolean z) {
        if (this.state == 0) {
            if (!U.compareAndSwapInt(this, STATE, 0, z ? 5 : 4)) {
                return false;
            }
            if (z) {
                try {
                    try {
                        Thread thread = this.runner;
                        if (thread != null) {
                            thread.interrupt();
                        }
                        U.putOrderedInt(this, STATE, 6);
                    } catch (Throwable th) {
                        U.putOrderedInt(this, STATE, 6);
                        throw th;
                    }
                } catch (Throwable th2) {
                    finishCompletion();
                    throw th2;
                }
            }
            finishCompletion();
            return true;
        }
        return false;
    }

    @Override
    public V get() throws ExecutionException, InterruptedException {
        int iAwaitDone = this.state;
        if (iAwaitDone <= 1) {
            iAwaitDone = awaitDone(false, 0L);
        }
        return report(iAwaitDone);
    }

    @Override
    public V get(long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        if (timeUnit == null) {
            throw new NullPointerException();
        }
        int iAwaitDone = this.state;
        if (iAwaitDone <= 1 && (iAwaitDone = awaitDone(true, timeUnit.toNanos(j))) <= 1) {
            throw new TimeoutException();
        }
        return report(iAwaitDone);
    }

    protected void done() {
    }

    protected void set(V v) {
        if (U.compareAndSwapInt(this, STATE, 0, 1)) {
            this.outcome = v;
            U.putOrderedInt(this, STATE, 2);
            finishCompletion();
        }
    }

    protected void setException(Throwable th) {
        if (U.compareAndSwapInt(this, STATE, 0, 1)) {
            this.outcome = th;
            U.putOrderedInt(this, STATE, 3);
            finishCompletion();
        }
    }

    @Override
    public void run() {
        boolean z;
        V vCall;
        if (this.state != 0 || !U.compareAndSwapObject(this, RUNNER, null, Thread.currentThread())) {
            return;
        }
        try {
            Callable<V> callable = this.callable;
            if (callable != null && this.state == 0) {
                try {
                    vCall = callable.call();
                    z = true;
                } catch (Throwable th) {
                    z = false;
                    setException(th);
                    vCall = null;
                }
                if (z) {
                    set(vCall);
                }
            }
        } finally {
            this.runner = null;
            int i = this.state;
            if (i >= 5) {
                handlePossibleCancellationInterrupt(i);
            }
        }
    }

    protected boolean runAndReset() {
        int i;
        boolean z;
        if (this.state != 0 || !U.compareAndSwapObject(this, RUNNER, null, Thread.currentThread())) {
            return false;
        }
        int i2 = this.state;
        try {
            Callable<V> callable = this.callable;
            if (callable != null && i2 == 0) {
                try {
                    callable.call();
                    z = true;
                } catch (Throwable th) {
                    setException(th);
                    z = false;
                    return !z ? false : false;
                }
            } else {
                z = false;
            }
            if (!z && i == 0) {
                return true;
            }
        } finally {
            this.runner = null;
            i = this.state;
            if (i >= 5) {
                handlePossibleCancellationInterrupt(i);
            }
        }
    }

    private void handlePossibleCancellationInterrupt(int i) {
        if (i == 5) {
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
            WaitNode waitNode = this.waiters;
            if (waitNode == null) {
                break;
            }
            if (U.compareAndSwapObject(this, WAITERS, waitNode, null)) {
                while (true) {
                    Thread thread = waitNode.thread;
                    if (thread != null) {
                        waitNode.thread = null;
                        LockSupport.unpark(thread);
                    }
                    WaitNode waitNode2 = waitNode.next;
                    if (waitNode2 == null) {
                        break;
                    }
                    waitNode.next = null;
                    waitNode = waitNode2;
                }
            }
        }
        done();
        this.callable = null;
    }

    private int awaitDone(boolean z, long j) throws InterruptedException {
        long j2;
        long j3 = 0;
        boolean zCompareAndSwapObject = false;
        WaitNode waitNode = null;
        while (true) {
            int i = this.state;
            if (i > 1) {
                if (waitNode != null) {
                    waitNode.thread = null;
                }
                return i;
            }
            if (i == 1) {
                Thread.yield();
            } else {
                if (Thread.interrupted()) {
                    removeWaiter(waitNode);
                    throw new InterruptedException();
                }
                if (waitNode == null) {
                    if (z && j <= 0) {
                        return i;
                    }
                    waitNode = new WaitNode();
                } else if (!zCompareAndSwapObject) {
                    Unsafe unsafe = U;
                    long j4 = WAITERS;
                    WaitNode waitNode2 = this.waiters;
                    waitNode.next = waitNode2;
                    zCompareAndSwapObject = unsafe.compareAndSwapObject(this, j4, waitNode2, waitNode);
                } else if (z) {
                    if (j3 == 0) {
                        long jNanoTime = System.nanoTime();
                        if (jNanoTime == 0) {
                            jNanoTime = 1;
                        }
                        j3 = jNanoTime;
                        j2 = j;
                    } else {
                        long jNanoTime2 = System.nanoTime() - j3;
                        if (jNanoTime2 >= j) {
                            removeWaiter(waitNode);
                            return this.state;
                        }
                        j2 = j - jNanoTime2;
                    }
                    if (this.state < 1) {
                        LockSupport.parkNanos(this, j2);
                    }
                } else {
                    LockSupport.park(this);
                }
            }
        }
    }

    private void removeWaiter(WaitNode waitNode) {
        if (waitNode != null) {
            waitNode.thread = null;
            while (true) {
                WaitNode waitNode2 = this.waiters;
                WaitNode waitNode3 = null;
                while (waitNode2 != null) {
                    WaitNode waitNode4 = waitNode2.next;
                    if (waitNode2.thread != null) {
                        waitNode3 = waitNode2;
                    } else if (waitNode3 != null) {
                        waitNode3.next = waitNode4;
                        if (waitNode3.thread == null) {
                            break;
                        }
                    } else if (!U.compareAndSwapObject(this, WAITERS, waitNode2, waitNode4)) {
                        break;
                    }
                    waitNode2 = waitNode4;
                }
                return;
            }
        }
    }

    static {
        try {
            STATE = U.objectFieldOffset(FutureTask.class.getDeclaredField("state"));
            RUNNER = U.objectFieldOffset(FutureTask.class.getDeclaredField("runner"));
            WAITERS = U.objectFieldOffset(FutureTask.class.getDeclaredField("waiters"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
