package java.util.concurrent;

import sun.misc.Unsafe;

public abstract class CountedCompleter<T> extends ForkJoinTask<T> {
    private static final long PENDING;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = 5232453752276485070L;
    final CountedCompleter<?> completer;
    volatile int pending;

    public abstract void compute();

    protected CountedCompleter(CountedCompleter<?> countedCompleter, int i) {
        this.completer = countedCompleter;
        this.pending = i;
    }

    protected CountedCompleter(CountedCompleter<?> countedCompleter) {
        this.completer = countedCompleter;
    }

    protected CountedCompleter() {
        this.completer = null;
    }

    public void onCompletion(CountedCompleter<?> countedCompleter) {
    }

    public boolean onExceptionalCompletion(Throwable th, CountedCompleter<?> countedCompleter) {
        return true;
    }

    public final CountedCompleter<?> getCompleter() {
        return this.completer;
    }

    public final int getPendingCount() {
        return this.pending;
    }

    public final void setPendingCount(int i) {
        this.pending = i;
    }

    public final void addToPendingCount(int i) {
        U.getAndAddInt(this, PENDING, i);
    }

    public final boolean compareAndSetPendingCount(int i, int i2) {
        return U.compareAndSwapInt(this, PENDING, i, i2);
    }

    public final int decrementPendingCountUnlessZero() {
        int i;
        do {
            i = this.pending;
            if (i == 0) {
                break;
            }
        } while (!U.compareAndSwapInt(this, PENDING, i, i - 1));
        return i;
    }

    public final CountedCompleter<?> getRoot() {
        CountedCompleter countedCompleter = this;
        while (true) {
            CountedCompleter countedCompleter2 = countedCompleter.completer;
            if (countedCompleter2 != null) {
                countedCompleter = countedCompleter2;
            } else {
                return countedCompleter;
            }
        }
    }

    public final void tryComplete() {
        CountedCompleter countedCompleter = this;
        CountedCompleter countedCompleter2 = countedCompleter;
        while (true) {
            int i = countedCompleter.pending;
            if (i == 0) {
                countedCompleter.onCompletion(countedCompleter2);
                CountedCompleter<?> countedCompleter3 = countedCompleter.completer;
                if (countedCompleter3 != null) {
                    countedCompleter2 = countedCompleter;
                    countedCompleter = countedCompleter3;
                } else {
                    countedCompleter.quietlyComplete();
                    return;
                }
            } else {
                if (U.compareAndSwapInt(countedCompleter, PENDING, i, i - 1)) {
                    return;
                }
            }
        }
    }

    public final void propagateCompletion() {
        CountedCompleter countedCompleter = this;
        while (true) {
            int i = countedCompleter.pending;
            if (i == 0) {
                CountedCompleter<?> countedCompleter2 = countedCompleter.completer;
                if (countedCompleter2 != null) {
                    countedCompleter = countedCompleter2;
                } else {
                    countedCompleter.quietlyComplete();
                    return;
                }
            } else {
                if (U.compareAndSwapInt(countedCompleter, PENDING, i, i - 1)) {
                    return;
                }
            }
        }
    }

    @Override
    public void complete(T t) {
        setRawResult(t);
        onCompletion(this);
        quietlyComplete();
        CountedCompleter<?> countedCompleter = this.completer;
        if (countedCompleter != null) {
            countedCompleter.tryComplete();
        }
    }

    public final CountedCompleter<?> firstComplete() {
        int i;
        do {
            i = this.pending;
            if (i == 0) {
                return this;
            }
        } while (!U.compareAndSwapInt(this, PENDING, i, i - 1));
        return null;
    }

    public final CountedCompleter<?> nextComplete() {
        CountedCompleter<?> countedCompleter = this.completer;
        if (countedCompleter != null) {
            return countedCompleter.firstComplete();
        }
        quietlyComplete();
        return null;
    }

    public final void quietlyCompleteRoot() {
        CountedCompleter countedCompleter = this;
        while (true) {
            CountedCompleter<?> countedCompleter2 = countedCompleter.completer;
            if (countedCompleter2 != null) {
                countedCompleter = countedCompleter2;
            } else {
                countedCompleter.quietlyComplete();
                return;
            }
        }
    }

    public final void helpComplete(int i) {
        if (i > 0 && this.status >= 0) {
            Thread threadCurrentThread = Thread.currentThread();
            if (threadCurrentThread instanceof ForkJoinWorkerThread) {
                ForkJoinWorkerThread forkJoinWorkerThread = (ForkJoinWorkerThread) threadCurrentThread;
                forkJoinWorkerThread.pool.helpComplete(forkJoinWorkerThread.workQueue, this, i);
            } else {
                ForkJoinPool.common.externalHelpComplete(this, i);
            }
        }
    }

    @Override
    void internalPropagateException(Throwable th) {
        CountedCompleter<?> countedCompleter;
        CountedCompleter countedCompleter2 = this;
        CountedCompleter countedCompleter3 = countedCompleter2;
        while (countedCompleter2.onExceptionalCompletion(th, countedCompleter3) && (countedCompleter = countedCompleter2.completer) != null && countedCompleter.status >= 0 && countedCompleter.recordExceptionalCompletion(th) == Integer.MIN_VALUE) {
            countedCompleter3 = countedCompleter2;
            countedCompleter2 = countedCompleter;
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
