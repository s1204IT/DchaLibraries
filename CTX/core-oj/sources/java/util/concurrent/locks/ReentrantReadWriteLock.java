package java.util.concurrent.locks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import sun.misc.Unsafe;

public class ReentrantReadWriteLock implements ReadWriteLock, Serializable {
    private static final long TID;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = -6992448646407690164L;
    private final ReadLock readerLock;
    final Sync sync;
    private final WriteLock writerLock;

    public ReentrantReadWriteLock() {
        this(false);
    }

    public ReentrantReadWriteLock(boolean z) {
        this.sync = z ? new FairSync() : new NonfairSync();
        this.readerLock = new ReadLock(this);
        this.writerLock = new WriteLock(this);
    }

    @Override
    public WriteLock writeLock() {
        return this.writerLock;
    }

    @Override
    public ReadLock readLock() {
        return this.readerLock;
    }

    static abstract class Sync extends AbstractQueuedSynchronizer {
        static final int EXCLUSIVE_MASK = 65535;
        static final int MAX_COUNT = 65535;
        static final int SHARED_SHIFT = 16;
        static final int SHARED_UNIT = 65536;
        private static final long serialVersionUID = 6317671515068378041L;
        private transient HoldCounter cachedHoldCounter;
        private transient Thread firstReader;
        private transient int firstReaderHoldCount;
        private transient ThreadLocalHoldCounter readHolds = new ThreadLocalHoldCounter();

        abstract boolean readerShouldBlock();

        abstract boolean writerShouldBlock();

        static int sharedCount(int i) {
            return i >>> 16;
        }

        static int exclusiveCount(int i) {
            return i & 65535;
        }

        static final class HoldCounter {
            int count;
            final long tid = ReentrantReadWriteLock.getThreadId(Thread.currentThread());

            HoldCounter() {
            }
        }

        static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
            ThreadLocalHoldCounter() {
            }

            @Override
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        Sync() {
            setState(getState());
        }

        @Override
        protected final boolean tryRelease(int i) {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int state = getState() - i;
            boolean z = exclusiveCount(state) == 0;
            if (z) {
                setExclusiveOwnerThread(null);
            }
            setState(state);
            return z;
        }

        @Override
        protected final boolean tryAcquire(int i) {
            Thread threadCurrentThread = Thread.currentThread();
            int state = getState();
            int iExclusiveCount = exclusiveCount(state);
            if (state != 0) {
                if (iExclusiveCount == 0 || threadCurrentThread != getExclusiveOwnerThread()) {
                    return false;
                }
                if (iExclusiveCount + exclusiveCount(i) > 65535) {
                    throw new Error("Maximum lock count exceeded");
                }
                setState(state + i);
                return true;
            }
            if (writerShouldBlock() || !compareAndSetState(state, i + state)) {
                return false;
            }
            setExclusiveOwnerThread(threadCurrentThread);
            return true;
        }

        @Override
        protected final boolean tryReleaseShared(int i) {
            int state;
            int i2;
            Thread threadCurrentThread = Thread.currentThread();
            if (this.firstReader == threadCurrentThread) {
                if (this.firstReaderHoldCount == 1) {
                    this.firstReader = null;
                } else {
                    this.firstReaderHoldCount--;
                }
            } else {
                HoldCounter holdCounter = this.cachedHoldCounter;
                if (holdCounter == null || holdCounter.tid != ReentrantReadWriteLock.getThreadId(threadCurrentThread)) {
                    holdCounter = this.readHolds.get();
                }
                int i3 = holdCounter.count;
                if (i3 <= 1) {
                    this.readHolds.remove();
                    if (i3 <= 0) {
                        throw unmatchedUnlockException();
                    }
                }
                holdCounter.count--;
            }
            do {
                state = getState();
                i2 = state - 65536;
            } while (!compareAndSetState(state, i2));
            return i2 == 0;
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException("attempt to unlock read lock, not locked by current thread");
        }

        @Override
        protected final int tryAcquireShared(int i) {
            Thread threadCurrentThread = Thread.currentThread();
            int state = getState();
            if (exclusiveCount(state) != 0 && getExclusiveOwnerThread() != threadCurrentThread) {
                return -1;
            }
            int iSharedCount = sharedCount(state);
            if (!readerShouldBlock() && iSharedCount < 65535 && compareAndSetState(state, 65536 + state)) {
                if (iSharedCount == 0) {
                    this.firstReader = threadCurrentThread;
                    this.firstReaderHoldCount = 1;
                } else if (this.firstReader == threadCurrentThread) {
                    this.firstReaderHoldCount++;
                } else {
                    HoldCounter holdCounter = this.cachedHoldCounter;
                    if (holdCounter == null || holdCounter.tid != ReentrantReadWriteLock.getThreadId(threadCurrentThread)) {
                        holdCounter = this.readHolds.get();
                        this.cachedHoldCounter = holdCounter;
                    } else if (holdCounter.count == 0) {
                        this.readHolds.set(holdCounter);
                    }
                    holdCounter.count++;
                }
                return 1;
            }
            return fullTryAcquireShared(threadCurrentThread);
        }

        final int fullTryAcquireShared(Thread thread) {
            int state;
            HoldCounter holdCounter = null;
            do {
                state = getState();
                if (exclusiveCount(state) != 0) {
                    if (getExclusiveOwnerThread() != thread) {
                        return -1;
                    }
                } else if (readerShouldBlock() && this.firstReader != thread) {
                    if (holdCounter == null && ((holdCounter = this.cachedHoldCounter) == null || holdCounter.tid != ReentrantReadWriteLock.getThreadId(thread))) {
                        holdCounter = this.readHolds.get();
                        if (holdCounter.count == 0) {
                            this.readHolds.remove();
                        }
                    }
                    if (holdCounter.count == 0) {
                        return -1;
                    }
                }
                if (sharedCount(state) == 65535) {
                    throw new Error("Maximum lock count exceeded");
                }
            } while (!compareAndSetState(state, 65536 + state));
            if (sharedCount(state) == 0) {
                this.firstReader = thread;
                this.firstReaderHoldCount = 1;
            } else if (this.firstReader == thread) {
                this.firstReaderHoldCount++;
            } else {
                if (holdCounter == null) {
                    holdCounter = this.cachedHoldCounter;
                }
                if (holdCounter == null || holdCounter.tid != ReentrantReadWriteLock.getThreadId(thread)) {
                    holdCounter = this.readHolds.get();
                } else if (holdCounter.count == 0) {
                    this.readHolds.set(holdCounter);
                }
                holdCounter.count++;
                this.cachedHoldCounter = holdCounter;
            }
            return 1;
        }

        final boolean tryWriteLock() {
            Thread threadCurrentThread = Thread.currentThread();
            int state = getState();
            if (state != 0) {
                int iExclusiveCount = exclusiveCount(state);
                if (iExclusiveCount == 0 || threadCurrentThread != getExclusiveOwnerThread()) {
                    return false;
                }
                if (iExclusiveCount == 65535) {
                    throw new Error("Maximum lock count exceeded");
                }
            }
            if (!compareAndSetState(state, state + 1)) {
                return false;
            }
            setExclusiveOwnerThread(threadCurrentThread);
            return true;
        }

        final boolean tryReadLock() {
            int state;
            int iSharedCount;
            Thread threadCurrentThread = Thread.currentThread();
            do {
                state = getState();
                if (exclusiveCount(state) != 0 && getExclusiveOwnerThread() != threadCurrentThread) {
                    return false;
                }
                iSharedCount = sharedCount(state);
                if (iSharedCount == 65535) {
                    throw new Error("Maximum lock count exceeded");
                }
            } while (!compareAndSetState(state, 65536 + state));
            if (iSharedCount == 0) {
                this.firstReader = threadCurrentThread;
                this.firstReaderHoldCount = 1;
            } else if (this.firstReader == threadCurrentThread) {
                this.firstReaderHoldCount++;
            } else {
                HoldCounter holdCounter = this.cachedHoldCounter;
                if (holdCounter == null || holdCounter.tid != ReentrantReadWriteLock.getThreadId(threadCurrentThread)) {
                    holdCounter = this.readHolds.get();
                    this.cachedHoldCounter = holdCounter;
                } else if (holdCounter.count == 0) {
                    this.readHolds.set(holdCounter);
                }
                holdCounter.count++;
            }
            return true;
        }

        @Override
        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final AbstractQueuedSynchronizer.ConditionObject newCondition() {
            return new AbstractQueuedSynchronizer.ConditionObject();
        }

        final Thread getOwner() {
            if (exclusiveCount(getState()) == 0) {
                return null;
            }
            return getExclusiveOwnerThread();
        }

        final int getReadLockCount() {
            return sharedCount(getState());
        }

        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        final int getWriteHoldCount() {
            if (isHeldExclusively()) {
                return exclusiveCount(getState());
            }
            return 0;
        }

        final int getReadHoldCount() {
            if (getReadLockCount() == 0) {
                return 0;
            }
            Thread threadCurrentThread = Thread.currentThread();
            if (this.firstReader == threadCurrentThread) {
                return this.firstReaderHoldCount;
            }
            HoldCounter holdCounter = this.cachedHoldCounter;
            if (holdCounter != null && holdCounter.tid == ReentrantReadWriteLock.getThreadId(threadCurrentThread)) {
                return holdCounter.count;
            }
            int i = this.readHolds.get().count;
            if (i == 0) {
                this.readHolds.remove();
            }
            return i;
        }

        private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
            objectInputStream.defaultReadObject();
            this.readHolds = new ThreadLocalHoldCounter();
            setState(0);
        }

        final int getCount() {
            return getState();
        }
    }

    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -8159625535654395037L;

        NonfairSync() {
        }

        @Override
        final boolean writerShouldBlock() {
            return false;
        }

        @Override
        final boolean readerShouldBlock() {
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    static final class FairSync extends Sync {
        private static final long serialVersionUID = -2274990926593161451L;

        FairSync() {
        }

        @Override
        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }

        @Override
        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    public static class ReadLock implements Lock, Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;

        protected ReadLock(ReentrantReadWriteLock reentrantReadWriteLock) {
            this.sync = reentrantReadWriteLock.sync;
        }

        @Override
        public void lock() {
            this.sync.acquireShared(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            this.sync.acquireSharedInterruptibly(1);
        }

        @Override
        public boolean tryLock() {
            return this.sync.tryReadLock();
        }

        @Override
        public boolean tryLock(long j, TimeUnit timeUnit) throws InterruptedException {
            return this.sync.tryAcquireSharedNanos(1, timeUnit.toNanos(j));
        }

        @Override
        public void unlock() {
            this.sync.releaseShared(1);
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            return super.toString() + "[Read locks = " + this.sync.getReadLockCount() + "]";
        }
    }

    public static class WriteLock implements Lock, Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final Sync sync;

        protected WriteLock(ReentrantReadWriteLock reentrantReadWriteLock) {
            this.sync = reentrantReadWriteLock.sync;
        }

        @Override
        public void lock() {
            this.sync.acquire(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            this.sync.acquireInterruptibly(1);
        }

        @Override
        public boolean tryLock() {
            return this.sync.tryWriteLock();
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

        public boolean isHeldByCurrentThread() {
            return this.sync.isHeldExclusively();
        }

        public int getHoldCount() {
            return this.sync.getWriteHoldCount();
        }
    }

    public final boolean isFair() {
        return this.sync instanceof FairSync;
    }

    protected Thread getOwner() {
        return this.sync.getOwner();
    }

    public int getReadLockCount() {
        return this.sync.getReadLockCount();
    }

    public boolean isWriteLocked() {
        return this.sync.isWriteLocked();
    }

    public boolean isWriteLockedByCurrentThread() {
        return this.sync.isHeldExclusively();
    }

    public int getWriteHoldCount() {
        return this.sync.getWriteHoldCount();
    }

    public int getReadHoldCount() {
        return this.sync.getReadHoldCount();
    }

    protected Collection<Thread> getQueuedWriterThreads() {
        return this.sync.getExclusiveQueuedThreads();
    }

    protected Collection<Thread> getQueuedReaderThreads() {
        return this.sync.getSharedQueuedThreads();
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
        int count = this.sync.getCount();
        return super.toString() + "[Write locks = " + Sync.exclusiveCount(count) + ", Read locks = " + Sync.sharedCount(count) + "]";
    }

    static final long getThreadId(Thread thread) {
        return U.getLongVolatile(thread, TID);
    }

    static {
        try {
            TID = U.objectFieldOffset(Thread.class.getDeclaredField("tid"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
