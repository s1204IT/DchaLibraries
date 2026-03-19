package java.util.concurrent;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class Semaphore implements Serializable {
    private static final long serialVersionUID = -3222578661600680210L;
    private final Sync sync;

    static abstract class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int i) {
            setState(i);
        }

        final int getPermits() {
            return getState();
        }

        final int nonfairTryAcquireShared(int i) {
            int state;
            int i2;
            do {
                state = getState();
                i2 = state - i;
                if (i2 < 0) {
                    break;
                }
            } while (!compareAndSetState(state, i2));
            return i2;
        }

        @Override
        protected final boolean tryReleaseShared(int i) {
            int state;
            int i2;
            do {
                state = getState();
                i2 = state + i;
                if (i2 < state) {
                    throw new Error("Maximum permit count exceeded");
                }
            } while (!compareAndSetState(state, i2));
            return true;
        }

        final void reducePermits(int i) {
            int state;
            int i2;
            do {
                state = getState();
                i2 = state - i;
                if (i2 > state) {
                    throw new Error("Permit count underflow");
                }
            } while (!compareAndSetState(state, i2));
        }

        final int drainPermits() {
            int state;
            do {
                state = getState();
                if (state == 0) {
                    break;
                }
            } while (!compareAndSetState(state, 0));
            return state;
        }
    }

    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int i) {
            super(i);
        }

        @Override
        protected int tryAcquireShared(int i) {
            return nonfairTryAcquireShared(i);
        }
    }

    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int i) {
            super(i);
        }

        @Override
        protected int tryAcquireShared(int i) {
            while (!hasQueuedPredecessors()) {
                int state = getState();
                int i2 = state - i;
                if (i2 < 0 || compareAndSetState(state, i2)) {
                    return i2;
                }
            }
            return -1;
        }
    }

    public Semaphore(int i) {
        this.sync = new NonfairSync(i);
    }

    public Semaphore(int i, boolean z) {
        this.sync = z ? new FairSync(i) : new NonfairSync(i);
    }

    public void acquire() throws InterruptedException {
        this.sync.acquireSharedInterruptibly(1);
    }

    public void acquireUninterruptibly() {
        this.sync.acquireShared(1);
    }

    public boolean tryAcquire() {
        return this.sync.nonfairTryAcquireShared(1) >= 0;
    }

    public boolean tryAcquire(long j, TimeUnit timeUnit) throws InterruptedException {
        return this.sync.tryAcquireSharedNanos(1, timeUnit.toNanos(j));
    }

    public void release() {
        this.sync.releaseShared(1);
    }

    public void acquire(int i) throws InterruptedException {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        this.sync.acquireSharedInterruptibly(i);
    }

    public void acquireUninterruptibly(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        this.sync.acquireShared(i);
    }

    public boolean tryAcquire(int i) {
        if (i >= 0) {
            return this.sync.nonfairTryAcquireShared(i) >= 0;
        }
        throw new IllegalArgumentException();
    }

    public boolean tryAcquire(int i, long j, TimeUnit timeUnit) throws InterruptedException {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return this.sync.tryAcquireSharedNanos(i, timeUnit.toNanos(j));
    }

    public void release(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        this.sync.releaseShared(i);
    }

    public int availablePermits() {
        return this.sync.getPermits();
    }

    public int drainPermits() {
        return this.sync.drainPermits();
    }

    protected void reducePermits(int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        this.sync.reducePermits(i);
    }

    public boolean isFair() {
        return this.sync instanceof FairSync;
    }

    public final boolean hasQueuedThreads() {
        return this.sync.hasQueuedThreads();
    }

    public final int getQueueLength() {
        return this.sync.getQueueLength();
    }

    protected Collection<Thread> getQueuedThreads() {
        return this.sync.getQueuedThreads();
    }

    public String toString() {
        return super.toString() + "[Permits = " + this.sync.getPermits() + "]";
    }
}
