package java.util.concurrent;

import sun.misc.Unsafe;

public abstract class CountedCompleter<T> extends ForkJoinTask<T> {
    private static final long PENDING;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = 5232453752276485070L;
    final CountedCompleter<?> completer;
    volatile int pending;

    public abstract void compute();

    protected CountedCompleter(CountedCompleter<?> completer, int initialPendingCount) {
        this.completer = completer;
        this.pending = initialPendingCount;
    }

    protected CountedCompleter(CountedCompleter<?> completer) {
        this.completer = completer;
    }

    protected CountedCompleter() {
        this.completer = null;
    }

    public void onCompletion(CountedCompleter<?> caller) {
    }

    public boolean onExceptionalCompletion(Throwable ex, CountedCompleter<?> caller) {
        return true;
    }

    public final CountedCompleter<?> getCompleter() {
        return this.completer;
    }

    public final int getPendingCount() {
        return this.pending;
    }

    public final void setPendingCount(int count) {
        this.pending = count;
    }

    public final void addToPendingCount(int delta) {
        U.getAndAddInt(this, PENDING, delta);
    }

    public final boolean compareAndSetPendingCount(int expected, int count) {
        return U.compareAndSwapInt(this, PENDING, expected, count);
    }

    public final int decrementPendingCountUnlessZero() {
        int c;
        do {
            c = this.pending;
            if (c == 0) {
                break;
            }
        } while (!U.compareAndSwapInt(this, PENDING, c, c - 1));
        return c;
    }

    public final CountedCompleter<?> getRoot() {
        CountedCompleter countedCompleter = this;
        while (true) {
            CountedCompleter p = countedCompleter.completer;
            if (p != null) {
                countedCompleter = p;
            } else {
                return countedCompleter;
            }
        }
    }

    public final void tryComplete() {
        CountedCompleter countedCompleter = this;
        CountedCompleter s = this;
        while (true) {
            int c = countedCompleter.pending;
            if (c == 0) {
                countedCompleter.onCompletion(s);
                s = countedCompleter;
                countedCompleter = countedCompleter.completer;
                if (countedCompleter == null) {
                    s.quietlyComplete();
                    return;
                }
            } else if (U.compareAndSwapInt(countedCompleter, PENDING, c, c - 1)) {
                return;
            }
        }
    }

    public final void propagateCompletion() {
        CountedCompleter countedCompleter = this;
        while (true) {
            int c = countedCompleter.pending;
            if (c == 0) {
                CountedCompleter countedCompleter2 = countedCompleter;
                countedCompleter = countedCompleter.completer;
                if (countedCompleter == null) {
                    countedCompleter2.quietlyComplete();
                    return;
                }
            } else if (U.compareAndSwapInt(countedCompleter, PENDING, c, c - 1)) {
                return;
            }
        }
    }

    @Override
    public void complete(T rawResult) {
        setRawResult(rawResult);
        onCompletion(this);
        quietlyComplete();
        CountedCompleter<?> p = this.completer;
        if (p == null) {
            return;
        }
        p.tryComplete();
    }

    public final CountedCompleter<?> firstComplete() {
        int c;
        do {
            c = this.pending;
            if (c == 0) {
                return this;
            }
        } while (!U.compareAndSwapInt(this, PENDING, c, c - 1));
        return null;
    }

    public final CountedCompleter<?> nextComplete() {
        CountedCompleter<?> p = this.completer;
        if (p != null) {
            return p.firstComplete();
        }
        quietlyComplete();
        return null;
    }

    public final void quietlyCompleteRoot() {
        CountedCompleter countedCompleter = this;
        while (true) {
            CountedCompleter<?> p = countedCompleter.completer;
            if (p == null) {
                countedCompleter.quietlyComplete();
                return;
            }
            countedCompleter = p;
        }
    }

    public final void helpComplete(int maxTasks) {
        if (maxTasks <= 0 || this.status < 0) {
            return;
        }
        ?? CurrentThread = Thread.currentThread();
        if (CurrentThread instanceof ForkJoinWorkerThread) {
            CurrentThread.pool.helpComplete(CurrentThread.workQueue, this, maxTasks);
        } else {
            ForkJoinPool.common.externalHelpComplete(this, maxTasks);
        }
    }

    @Override
    void internalPropagateException(Throwable ex) {
        CountedCompleter countedCompleter = this;
        CountedCompleter s = this;
        while (countedCompleter.onExceptionalCompletion(ex, s)) {
            s = countedCompleter;
            countedCompleter = countedCompleter.completer;
            if (countedCompleter == null || countedCompleter.status < 0 || countedCompleter.recordExceptionalCompletion(ex) != Integer.MIN_VALUE) {
                return;
            }
        }
    }

    @Override
    protected final boolean exec() {
        compute();
        return false;
    }

    @Override
    public T getRawResult() {
        return null;
    }

    @Override
    protected void setRawResult(T t) {
    }

    static {
        try {
            PENDING = U.objectFieldOffset(CountedCompleter.class.getDeclaredField("pending"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
