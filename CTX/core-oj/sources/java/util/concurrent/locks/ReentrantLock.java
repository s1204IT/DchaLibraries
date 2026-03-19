package java.util.concurrent.locks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class ReentrantLock implements Lock, Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    private final Sync sync;

    static abstract class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        abstract void lock();

        Sync() {
        }

        final boolean nonfairTryAcquire(int i) {
            Thread threadCurrentThread = Thread.currentThread();
            int state = getState();
            if (state == 0) {
                if (compareAndSetState(0, i)) {
                    setExclusiveOwnerThread(threadCurrentThread);
                    return true;
                }
            } else if (threadCurrentThread == getExclusiveOwnerThread()) {
                int i2 = state + i;
                if (i2 < 0) {
                    throw new Error("Maximum lock count exceeded");
                }
                setState(i2);
                return true;
            }
            return false;
        }

        @Override
        protected final boolean tryRelease(int i) {
            int state = getState() - i;
            if (Thread.currentThread() != getExclusiveOwnerThread()) {
                throw new IllegalMonitorStateException();
            }
            boolean z = false;
            if (state == 0) {
                z = true;
                setExclusiveOwnerThread(null);
            }
            setState(state);
            return z;
        }

        @Override
        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final AbstractQueuedSynchronizer.ConditionObject newCondition() {
            return new AbstractQueuedSynchronizer.ConditionObject();
        }

        final Thread getOwner() {
            if (getState() == 0) {
                return null;
            }
            return getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            if (isHeldExclusively()) {
                return getState();
            }
            return 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
            objectInputStream.defaultReadObject();
            setState(0);
        }
    }

    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        NonfairSync() {
        }

        @Override
        final void lock() {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
            } else {
                acquire(1);
            }
        }

        @Override
        protected final boolean tryAcquire(int i) {
            return nonfairTryAcquire(i);
        }
    }

    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        FairSync() {
        }

        @Override
        final void lock() {
            acquire(1);
        }

        @Override
        protected final boolean tryAcquire(int i) {
            Thread threadCurrentThread = Thread.currentThread();
            int state = getState();
            if (state == 0) {
                if (!hasQueuedPredecessors() && compareAndSetState(0, i)) {
                    setExclusiveOwnerThread(threadCurrentThread);
                    return true;
                }
            } else if (threadCurrentThread == getExclusiveOwnerThread()) {
                int i2 = state + i;
                if (i2 < 0) {
                    throw new Error("Maximum lock count exceeded");
                }
                setState(i2);
                return true;
            }
            return false;
        }
    }

    public ReentrantLock() {
        this.sync = new NonfairSync();
    }

    public ReentrantLock(boolean z) {
        this.sync = z ? new FairSync() : new NonfairSync();
    }

    @Override
    public void lock() {
        this.sync.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return this.sync.nonfairTryAcquire(1);
    }

    @Override
    public boolean tryLock(long j, TimeUnit timeUnit) throws InterruptedException {
        return this.sync.tryAcquireNanos(1, timeUnit.toNanos(j));
    }

    @Override
    public void unlock() {
        this.sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return this.sync.newCondition();
    }

    public int getHoldCount() {
        return this.sync.getHoldCount();
    }

    public boolean isHeldByCurrentThread() {
        return this.sync.isHeldExclusively();
    }

    public boolean isLocked() {
        return this.sync.isLocked();
    }

    public final boolean isFair() {
        return this.sync instanceof FairSync;
    }

    protected Thread getOwner() {
        return this.sync.getOwner();
    }

    public final boolean hasQueuedThreads() {
        return this.sync.hasQueuedThreads();
    }

    public final boolean hasQueuedThread(Thread thread) {
        return this.sync.isQueued(thread);
    }

    public final int getQueueLength() {
        return this.sync.getQueueLength();
    }

    protected Collection<Thread> getQueuedThreads() {
        return this.sync.getQueuedThreads();
    }

    public boolean hasWaiters(Condition condition) {
        if (condition == null) {
            throw new NullPointerException();
        }
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        return this.sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    public int getWaitQueueLength(Condition condition) {
        if (condition == null) {
            throw new NullPointerException();
        }
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        return this.sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null) {
            throw new NullPointerException();
        }
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject)) {
            throw new IllegalArgumentException("not owner");
        }
        return this.sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject) condition);
    }

    public String toString() {
        String str;
        Thread owner = this.sync.getOwner();
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        if (owner == null) {
            str = "[Unlocked]";
        } else {
            str = "[Locked by thread " + owner.getName() + "]";
        }
        sb.append(str);
        return sb.toString();
    }
}
