package java.util.concurrent.locks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

public class StampedLock implements Serializable {
    private static final long ABITS = 255;
    private static final int CANCELLED = 1;
    private static final int HEAD_SPINS;
    private static final long INTERRUPTED = 1;
    private static final int LG_READERS = 7;
    private static final int MAX_HEAD_SPINS;
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final long ORIGIN = 256;
    private static final int OVERFLOW_YIELD_RATE = 7;
    private static final long PARKBLOCKER;
    private static final long RBITS = 127;
    private static final long RFULL = 126;
    private static final int RMODE = 0;
    private static final long RUNIT = 1;
    private static final long SBITS = -128;
    private static final int SPINS;
    private static final long STATE;
    private static final Unsafe U;
    private static final int WAITING = -1;
    private static final long WBIT = 128;
    private static final long WCOWAIT;
    private static final long WHEAD;
    private static final int WMODE = 1;
    private static final long WNEXT;
    private static final long WSTATUS;
    private static final long WTAIL;
    private static final long serialVersionUID = -6001602636862214147L;
    transient ReadLockView readLockView;
    transient ReadWriteLockView readWriteLockView;
    private transient int readerOverflow;
    private volatile transient long state = ORIGIN;
    private volatile transient WNode whead;
    transient WriteLockView writeLockView;
    private volatile transient WNode wtail;

    static {
        SPINS = NCPU > 1 ? 64 : 0;
        HEAD_SPINS = NCPU > 1 ? 1024 : 0;
        MAX_HEAD_SPINS = NCPU > 1 ? 65536 : 0;
        U = Unsafe.getUnsafe();
        try {
            STATE = U.objectFieldOffset(StampedLock.class.getDeclaredField("state"));
            WHEAD = U.objectFieldOffset(StampedLock.class.getDeclaredField("whead"));
            WTAIL = U.objectFieldOffset(StampedLock.class.getDeclaredField("wtail"));
            WSTATUS = U.objectFieldOffset(WNode.class.getDeclaredField("status"));
            WNEXT = U.objectFieldOffset(WNode.class.getDeclaredField("next"));
            WCOWAIT = U.objectFieldOffset(WNode.class.getDeclaredField("cowait"));
            PARKBLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    static final class WNode {
        volatile WNode cowait;
        final int mode;
        volatile WNode next;
        volatile WNode prev;
        volatile int status;
        volatile Thread thread;

        WNode(int i, WNode wNode) {
            this.mode = i;
            this.prev = wNode;
        }
    }

    public long writeLock() {
        long j = this.state;
        if ((ABITS & j) == 0) {
            Unsafe unsafe = U;
            long j2 = STATE;
            long j3 = j + WBIT;
            if (unsafe.compareAndSwapLong(this, j2, j, j3)) {
                return j3;
            }
        }
        return acquireWrite(false, 0L);
    }

    public long tryWriteLock() {
        long j = this.state;
        if ((ABITS & j) != 0) {
            return 0L;
        }
        Unsafe unsafe = U;
        long j2 = STATE;
        long j3 = j + WBIT;
        if (unsafe.compareAndSwapLong(this, j2, j, j3)) {
            return j3;
        }
        return 0L;
    }

    public long tryWriteLock(long j, TimeUnit timeUnit) throws InterruptedException {
        long nanos = timeUnit.toNanos(j);
        if (!Thread.interrupted()) {
            long jTryWriteLock = tryWriteLock();
            if (jTryWriteLock != 0) {
                return jTryWriteLock;
            }
            if (nanos <= 0) {
                return 0L;
            }
            long jNanoTime = System.nanoTime() + nanos;
            if (jNanoTime == 0) {
                jNanoTime = 1;
            }
            long jAcquireWrite = acquireWrite(true, jNanoTime);
            if (jAcquireWrite != 1) {
                return jAcquireWrite;
            }
        }
        throw new InterruptedException();
    }

    public long writeLockInterruptibly() throws InterruptedException {
        if (!Thread.interrupted()) {
            long jAcquireWrite = acquireWrite(true, 0L);
            if (jAcquireWrite != 1) {
                return jAcquireWrite;
            }
        }
        throw new InterruptedException();
    }

    public long readLock() {
        long j = this.state;
        if (this.whead == this.wtail && (ABITS & j) < RFULL) {
            long j2 = j + 1;
            if (U.compareAndSwapLong(this, STATE, j, j2)) {
                return j2;
            }
        }
        return acquireRead(false, 0L);
    }

    public long tryReadLock() {
        while (true) {
            long j = this.state;
            long j2 = ABITS & j;
            if (j2 == WBIT) {
                return 0L;
            }
            if (j2 < RFULL) {
                long j3 = j + 1;
                if (U.compareAndSwapLong(this, STATE, j, j3)) {
                    return j3;
                }
            } else {
                long jTryIncReaderOverflow = tryIncReaderOverflow(j);
                if (jTryIncReaderOverflow != 0) {
                    return jTryIncReaderOverflow;
                }
            }
        }
    }

    public long tryReadLock(long j, TimeUnit timeUnit) throws InterruptedException {
        long nanos = timeUnit.toNanos(j);
        if (!Thread.interrupted()) {
            long j2 = this.state;
            long j3 = ABITS & j2;
            if (j3 != WBIT) {
                if (j3 < RFULL) {
                    long j4 = j2 + 1;
                    if (U.compareAndSwapLong(this, STATE, j2, j4)) {
                        return j4;
                    }
                } else {
                    long jTryIncReaderOverflow = tryIncReaderOverflow(j2);
                    if (jTryIncReaderOverflow != 0) {
                        return jTryIncReaderOverflow;
                    }
                }
            }
            if (nanos <= 0) {
                return 0L;
            }
            long jNanoTime = System.nanoTime() + nanos;
            if (jNanoTime == 0) {
                jNanoTime = 1;
            }
            long jAcquireRead = acquireRead(true, jNanoTime);
            if (jAcquireRead != 1) {
                return jAcquireRead;
            }
        }
        throw new InterruptedException();
    }

    public long readLockInterruptibly() throws InterruptedException {
        if (!Thread.interrupted()) {
            long jAcquireRead = acquireRead(true, 0L);
            if (jAcquireRead != 1) {
                return jAcquireRead;
            }
        }
        throw new InterruptedException();
    }

    public long tryOptimisticRead() {
        long j = this.state;
        if ((WBIT & j) == 0) {
            return j & SBITS;
        }
        return 0L;
    }

    public boolean validate(long j) {
        U.loadFence();
        return (j & SBITS) == (SBITS & this.state);
    }

    public void unlockWrite(long j) {
        if (this.state != j || (j & WBIT) == 0) {
            throw new IllegalMonitorStateException();
        }
        Unsafe unsafe = U;
        long j2 = STATE;
        long j3 = j + WBIT;
        if (j3 == 0) {
            j3 = ORIGIN;
        }
        unsafe.putLongVolatile(this, j2, j3);
        WNode wNode = this.whead;
        if (wNode != null && wNode.status != 0) {
            release(wNode);
        }
    }

    public void unlockRead(long j) {
        WNode wNode;
        while (true) {
            long j2 = this.state;
            if ((j2 & SBITS) != (SBITS & j) || (j & ABITS) == 0) {
                break;
            }
            long j3 = j2 & ABITS;
            if (j3 == 0 || j3 == WBIT) {
                break;
            }
            if (j3 < RFULL) {
                if (U.compareAndSwapLong(this, STATE, j2, j2 - 1)) {
                    if (j3 == 1 && (wNode = this.whead) != null && wNode.status != 0) {
                        release(wNode);
                        return;
                    }
                    return;
                }
            } else if (tryDecReaderOverflow(j2) != 0) {
                return;
            }
        }
    }

    public void unlock(long j) {
        WNode wNode;
        long j2 = j & ABITS;
        while (true) {
            long j3 = this.state;
            if ((j3 & SBITS) != (j & SBITS)) {
                break;
            }
            long j4 = j3 & ABITS;
            if (j4 == 0) {
                break;
            }
            if (j4 == WBIT) {
                if (j2 == j4) {
                    Unsafe unsafe = U;
                    long j5 = STATE;
                    long j6 = j3 + WBIT;
                    if (j6 == 0) {
                        j6 = 256;
                    }
                    unsafe.putLongVolatile(this, j5, j6);
                    WNode wNode2 = this.whead;
                    if (wNode2 != null && wNode2.status != 0) {
                        release(wNode2);
                        return;
                    }
                    return;
                }
            } else {
                if (j2 == 0 || j2 >= WBIT) {
                    break;
                }
                if (j4 < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, j3, j3 - 1)) {
                        if (j4 == 1 && (wNode = this.whead) != null && wNode.status != 0) {
                            release(wNode);
                            return;
                        }
                        return;
                    }
                } else if (tryDecReaderOverflow(j3) != 0) {
                    return;
                }
            }
        }
    }

    public long tryConvertToWriteLock(long j) {
        long j2 = j & ABITS;
        while (true) {
            long j3 = this.state;
            if ((j3 & SBITS) != (j & SBITS)) {
                break;
            }
            long j4 = j3 & ABITS;
            if (j4 == 0) {
                if (j2 != 0) {
                    break;
                }
                Unsafe unsafe = U;
                long j5 = STATE;
                long j6 = WBIT + j3;
                if (unsafe.compareAndSwapLong(this, j5, j3, j6)) {
                    return j6;
                }
            } else {
                if (j4 == WBIT) {
                    if (j2 != j4) {
                        break;
                    }
                    return j;
                }
                if (j4 != 1 || j2 == 0) {
                    break;
                }
                Unsafe unsafe2 = U;
                long j7 = STATE;
                long j8 = WBIT + (j3 - 1);
                if (unsafe2.compareAndSwapLong(this, j7, j3, j8)) {
                    return j8;
                }
            }
        }
    }

    public long tryConvertToReadLock(long j) {
        long j2 = j & ABITS;
        while (true) {
            long j3 = this.state;
            if ((j3 & SBITS) != (j & SBITS)) {
                break;
            }
            long j4 = j3 & ABITS;
            if (j4 == 0) {
                if (j2 != 0) {
                    break;
                }
                if (j4 < RFULL) {
                    long j5 = j3 + 1;
                    if (U.compareAndSwapLong(this, STATE, j3, j5)) {
                        return j5;
                    }
                } else {
                    long jTryIncReaderOverflow = tryIncReaderOverflow(j3);
                    if (jTryIncReaderOverflow != 0) {
                        return jTryIncReaderOverflow;
                    }
                }
            } else {
                if (j4 != WBIT) {
                    if (j2 == 0 || j2 >= WBIT) {
                        break;
                    }
                    return j;
                }
                if (j2 == j4) {
                    long j6 = 129 + j3;
                    U.putLongVolatile(this, STATE, j6);
                    WNode wNode = this.whead;
                    if (wNode != null && wNode.status != 0) {
                        release(wNode);
                    }
                    return j6;
                }
            }
        }
    }

    public long tryConvertToOptimisticRead(long j) {
        WNode wNode;
        long j2 = j & ABITS;
        U.loadFence();
        while (true) {
            long j3 = this.state;
            if ((j3 & SBITS) != (j & SBITS)) {
                break;
            }
            long j4 = j3 & ABITS;
            if (j4 == 0) {
                if (j2 != 0) {
                    break;
                }
                return j3;
            }
            if (j4 == WBIT) {
                if (j2 == j4) {
                    Unsafe unsafe = U;
                    long j5 = STATE;
                    long j6 = j3 + WBIT;
                    long j7 = j6 == 0 ? 256L : j6;
                    unsafe.putLongVolatile(this, j5, j7);
                    WNode wNode2 = this.whead;
                    if (wNode2 != null && wNode2.status != 0) {
                        release(wNode2);
                    }
                    return j7;
                }
            } else {
                if (j2 == 0 || j2 >= WBIT) {
                    break;
                }
                if (j4 < RFULL) {
                    long j8 = j3 - 1;
                    if (U.compareAndSwapLong(this, STATE, j3, j8)) {
                        if (j4 == 1 && (wNode = this.whead) != null && wNode.status != 0) {
                            release(wNode);
                        }
                        return j8 & SBITS;
                    }
                } else {
                    long jTryDecReaderOverflow = tryDecReaderOverflow(j3);
                    if (jTryDecReaderOverflow != 0) {
                        return jTryDecReaderOverflow & SBITS;
                    }
                }
            }
        }
        return 0L;
    }

    public boolean tryUnlockWrite() {
        long j = this.state;
        if ((j & WBIT) != 0) {
            Unsafe unsafe = U;
            long j2 = STATE;
            long j3 = j + WBIT;
            if (j3 == 0) {
                j3 = ORIGIN;
            }
            unsafe.putLongVolatile(this, j2, j3);
            WNode wNode = this.whead;
            if (wNode != null && wNode.status != 0) {
                release(wNode);
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean tryUnlockRead() {
        WNode wNode;
        while (true) {
            long j = this.state;
            long j2 = j & ABITS;
            if (j2 == 0 || j2 >= WBIT) {
                return false;
            }
            if (j2 < RFULL) {
                if (U.compareAndSwapLong(this, STATE, j, j - 1)) {
                    if (j2 == 1 && (wNode = this.whead) != null && wNode.status != 0) {
                        release(wNode);
                    }
                    return true;
                }
            } else if (tryDecReaderOverflow(j) != 0) {
                return true;
            }
        }
    }

    private int getReadLockCount(long j) {
        long j2 = j & RBITS;
        if (j2 >= RFULL) {
            j2 = ((long) this.readerOverflow) + RFULL;
        }
        return (int) j2;
    }

    public boolean isWriteLocked() {
        return (this.state & WBIT) != 0;
    }

    public boolean isReadLocked() {
        return (this.state & RBITS) != 0;
    }

    public int getReadLockCount() {
        return getReadLockCount(this.state);
    }

    public String toString() {
        String str;
        long j = this.state;
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        if ((ABITS & j) == 0) {
            str = "[Unlocked]";
        } else if ((WBIT & j) != 0) {
            str = "[Write-locked]";
        } else {
            str = "[Read-locks:" + getReadLockCount(j) + "]";
        }
        sb.append(str);
        return sb.toString();
    }

    public Lock asReadLock() {
        ReadLockView readLockView = this.readLockView;
        if (readLockView != null) {
            return readLockView;
        }
        ReadLockView readLockView2 = new ReadLockView();
        this.readLockView = readLockView2;
        return readLockView2;
    }

    public Lock asWriteLock() {
        WriteLockView writeLockView = this.writeLockView;
        if (writeLockView != null) {
            return writeLockView;
        }
        WriteLockView writeLockView2 = new WriteLockView();
        this.writeLockView = writeLockView2;
        return writeLockView2;
    }

    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView readWriteLockView = this.readWriteLockView;
        if (readWriteLockView != null) {
            return readWriteLockView;
        }
        ReadWriteLockView readWriteLockView2 = new ReadWriteLockView();
        this.readWriteLockView = readWriteLockView2;
        return readWriteLockView2;
    }

    final class ReadLockView implements Lock {
        ReadLockView() {
        }

        @Override
        public void lock() {
            StampedLock.this.readLock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            StampedLock.this.readLockInterruptibly();
        }

        @Override
        public boolean tryLock() {
            return StampedLock.this.tryReadLock() != 0;
        }

        @Override
        public boolean tryLock(long j, TimeUnit timeUnit) throws InterruptedException {
            return StampedLock.this.tryReadLock(j, timeUnit) != 0;
        }

        @Override
        public void unlock() {
            StampedLock.this.unstampedUnlockRead();
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class WriteLockView implements Lock {
        WriteLockView() {
        }

        @Override
        public void lock() {
            StampedLock.this.writeLock();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            StampedLock.this.writeLockInterruptibly();
        }

        @Override
        public boolean tryLock() {
            return StampedLock.this.tryWriteLock() != 0;
        }

        @Override
        public boolean tryLock(long j, TimeUnit timeUnit) throws InterruptedException {
            return StampedLock.this.tryWriteLock(j, timeUnit) != 0;
        }

        @Override
        public void unlock() {
            StampedLock.this.unstampedUnlockWrite();
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteLockView implements ReadWriteLock {
        ReadWriteLockView() {
        }

        @Override
        public Lock readLock() {
            return StampedLock.this.asReadLock();
        }

        @Override
        public Lock writeLock() {
            return StampedLock.this.asWriteLock();
        }
    }

    final void unstampedUnlockWrite() {
        long j = this.state;
        if ((j & WBIT) == 0) {
            throw new IllegalMonitorStateException();
        }
        Unsafe unsafe = U;
        long j2 = STATE;
        long j3 = j + WBIT;
        if (j3 == 0) {
            j3 = ORIGIN;
        }
        unsafe.putLongVolatile(this, j2, j3);
        WNode wNode = this.whead;
        if (wNode != null && wNode.status != 0) {
            release(wNode);
        }
    }

    final void unstampedUnlockRead() {
        WNode wNode;
        while (true) {
            long j = this.state;
            long j2 = j & ABITS;
            if (j2 == 0 || j2 >= WBIT) {
                break;
            }
            if (j2 < RFULL) {
                if (U.compareAndSwapLong(this, STATE, j, j - 1)) {
                    if (j2 == 1 && (wNode = this.whead) != null && wNode.status != 0) {
                        release(wNode);
                        return;
                    }
                    return;
                }
            } else if (tryDecReaderOverflow(j) != 0) {
                return;
            }
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        U.putLongVolatile(this, STATE, ORIGIN);
    }

    private long tryIncReaderOverflow(long j) {
        if ((ABITS & j) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, j, j | RBITS)) {
                this.readerOverflow++;
                U.putLongVolatile(this, STATE, j);
                return j;
            }
            return 0L;
        }
        if ((LockSupport.nextSecondarySeed() & 7) == 0) {
            Thread.yield();
            return 0L;
        }
        return 0L;
    }

    private long tryDecReaderOverflow(long j) {
        if ((ABITS & j) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, j, j | RBITS)) {
                int i = this.readerOverflow;
                if (i > 0) {
                    this.readerOverflow = i - 1;
                } else {
                    j--;
                }
                U.putLongVolatile(this, STATE, j);
                return j;
            }
            return 0L;
        }
        if ((LockSupport.nextSecondarySeed() & 7) == 0) {
            Thread.yield();
            return 0L;
        }
        return 0L;
    }

    private void release(WNode wNode) {
        Thread thread;
        if (wNode != null) {
            U.compareAndSwapInt(wNode, WSTATUS, -1, 0);
            WNode wNode2 = wNode.next;
            if (wNode2 == null || wNode2.status == 1) {
                for (WNode wNode3 = this.wtail; wNode3 != null && wNode3 != wNode; wNode3 = wNode3.prev) {
                    if (wNode3.status <= 0) {
                        wNode2 = wNode3;
                    }
                }
            }
            if (wNode2 != null && (thread = wNode2.thread) != null) {
                U.unpark(thread);
            }
        }
    }

    private long acquireWrite(boolean z, long j) {
        WNode wNode;
        Thread thread;
        boolean z2;
        long j2;
        long jNanoTime;
        WNode wNode2 = null;
        int i = -1;
        while (true) {
            long j3 = this.state;
            long j4 = ABITS;
            long j5 = j3 & ABITS;
            if (j5 == 0) {
                Unsafe unsafe = U;
                long j6 = STATE;
                long j7 = j3 + WBIT;
                if (unsafe.compareAndSwapLong(this, j6, j3, j7)) {
                    return j7;
                }
            } else {
                int i2 = 0;
                if (i >= 0) {
                    if (i > 0) {
                        if (LockSupport.nextSecondarySeed() >= 0) {
                            i--;
                        }
                    } else {
                        WNode wNode3 = this.wtail;
                        if (wNode3 == null) {
                            WNode wNode4 = new WNode(1, null);
                            if (U.compareAndSwapObject(this, WHEAD, null, wNode4)) {
                                this.wtail = wNode4;
                            }
                        } else if (wNode2 == null) {
                            wNode2 = new WNode(1, wNode3);
                        } else if (wNode2.prev != wNode3) {
                            wNode2.prev = wNode3;
                        } else if (U.compareAndSwapObject(this, WTAIL, wNode3, wNode2)) {
                            wNode3.next = wNode2;
                            boolean z3 = false;
                            WNode wNode5 = wNode3;
                            int i3 = -1;
                            while (true) {
                                WNode wNode6 = this.whead;
                                if (wNode6 == wNode5) {
                                    if (i3 < 0) {
                                        i3 = HEAD_SPINS;
                                    } else if (i3 < MAX_HEAD_SPINS) {
                                        i3 <<= 1;
                                    }
                                    int i4 = i3;
                                    int i5 = i4;
                                    while (true) {
                                        long j8 = this.state;
                                        if ((j8 & j4) == 0) {
                                            Unsafe unsafe2 = U;
                                            long j9 = STATE;
                                            long j10 = j8 + WBIT;
                                            wNode = wNode6;
                                            if (unsafe2.compareAndSwapLong(this, j9, j8, j10)) {
                                                this.whead = wNode2;
                                                wNode2.prev = null;
                                                if (z3) {
                                                    Thread.currentThread().interrupt();
                                                }
                                                return j10;
                                            }
                                        } else {
                                            wNode = wNode6;
                                            if (LockSupport.nextSecondarySeed() >= 0 && i5 - 1 <= 0) {
                                                i3 = i4;
                                                break;
                                            }
                                        }
                                        wNode6 = wNode;
                                        j4 = ABITS;
                                    }
                                } else {
                                    wNode = wNode6;
                                    if (wNode != null) {
                                        while (true) {
                                            WNode wNode7 = wNode.cowait;
                                            if (wNode7 == null) {
                                                break;
                                            }
                                            if (U.compareAndSwapObject(wNode, WCOWAIT, wNode7, wNode7.cowait) && (thread = wNode7.thread) != null) {
                                                U.unpark(thread);
                                            }
                                        }
                                    }
                                }
                                if (this.whead != wNode) {
                                    z2 = false;
                                    j2 = ABITS;
                                } else {
                                    WNode wNode8 = wNode2.prev;
                                    if (wNode8 != wNode5) {
                                        if (wNode8 != null) {
                                            wNode8.next = wNode2;
                                            wNode5 = wNode8;
                                        }
                                    } else {
                                        int i6 = wNode5.status;
                                        if (i6 == 0) {
                                            U.compareAndSwapInt(wNode5, WSTATUS, 0, -1);
                                        } else if (i6 == 1) {
                                            WNode wNode9 = wNode5.prev;
                                            if (wNode9 != null) {
                                                wNode2.prev = wNode9;
                                                wNode9.next = wNode2;
                                            }
                                        } else {
                                            if (j != 0) {
                                                jNanoTime = j - System.nanoTime();
                                                if (jNanoTime <= 0) {
                                                    return cancelWaiter(wNode2, wNode2, false);
                                                }
                                            } else {
                                                jNanoTime = 0;
                                            }
                                            z2 = false;
                                            Thread threadCurrentThread = Thread.currentThread();
                                            U.putObject(threadCurrentThread, PARKBLOCKER, this);
                                            wNode2.thread = threadCurrentThread;
                                            if (wNode5.status < 0) {
                                                if (wNode5 == wNode) {
                                                    long j11 = this.state;
                                                    j2 = ABITS;
                                                    if ((j11 & ABITS) != 0) {
                                                    }
                                                } else {
                                                    j2 = ABITS;
                                                }
                                                if (this.whead == wNode && wNode2.prev == wNode5) {
                                                    U.park(false, jNanoTime);
                                                }
                                            } else {
                                                j2 = ABITS;
                                            }
                                            wNode2.thread = null;
                                            U.putObject(threadCurrentThread, PARKBLOCKER, null);
                                            if (!Thread.interrupted()) {
                                                continue;
                                            } else {
                                                if (z) {
                                                    return cancelWaiter(wNode2, wNode2, true);
                                                }
                                                z3 = true;
                                            }
                                        }
                                    }
                                    z2 = false;
                                    j2 = ABITS;
                                }
                                j4 = j2;
                            }
                        }
                    }
                } else {
                    if (j5 == WBIT && this.wtail == this.whead) {
                        i2 = SPINS;
                    }
                    i = i2;
                }
            }
        }
    }

    private long acquireRead(boolean z, long j) {
        WNode wNode;
        int i;
        WNode wNode2;
        WNode wNode3;
        Thread thread;
        long jNanoTime;
        long jTryIncReaderOverflow;
        WNode wNode4;
        WNode wNode5;
        WNode wNode6;
        long jTryIncReaderOverflow2;
        long jNanoTime2;
        WNode wNode7;
        Thread thread2;
        WNode wNode8;
        long jTryIncReaderOverflow3;
        WNode wNode9 = null;
        WNode wNode10 = null;
        int i2 = -1;
        boolean z2 = false;
        loop0: while (true) {
            WNode wNode11 = this.whead;
            WNode wNode12 = this.wtail;
            if (wNode11 == wNode12) {
                i = i2;
                WNode wNode13 = wNode11;
                WNode wNode14 = wNode12;
                while (true) {
                    long j2 = this.state;
                    long j3 = j2 & ABITS;
                    if (j3 < RFULL) {
                        jTryIncReaderOverflow3 = j2 + 1;
                        wNode2 = wNode14;
                        wNode = wNode10;
                        wNode8 = wNode13;
                        if (U.compareAndSwapLong(this, STATE, j2, jTryIncReaderOverflow3)) {
                            break loop0;
                        }
                        if (j3 < WBIT) {
                            wNode14 = wNode2;
                            wNode13 = wNode8;
                        } else if (i > 0) {
                            if (LockSupport.nextSecondarySeed() >= 0) {
                                i--;
                            }
                            wNode14 = wNode2;
                            wNode13 = wNode8;
                        } else {
                            if (i == 0) {
                                wNode11 = this.whead;
                                WNode wNode15 = this.wtail;
                                if (wNode11 == wNode8 && wNode15 == wNode2) {
                                    wNode11 = wNode8;
                                    break;
                                }
                                if (wNode11 != wNode15) {
                                    wNode2 = wNode15;
                                    break;
                                }
                                wNode14 = wNode15;
                                wNode13 = wNode11;
                            } else {
                                wNode14 = wNode2;
                                wNode13 = wNode8;
                            }
                            i = SPINS;
                        }
                        wNode10 = wNode;
                    } else {
                        wNode2 = wNode14;
                        wNode = wNode10;
                        wNode8 = wNode13;
                        if (j3 < WBIT) {
                            jTryIncReaderOverflow3 = tryIncReaderOverflow(j2);
                            if (jTryIncReaderOverflow3 != 0) {
                                break loop0;
                            }
                        }
                        if (j3 < WBIT) {
                        }
                        wNode10 = wNode;
                    }
                }
            } else {
                wNode = wNode10;
                i = i2;
                wNode2 = wNode12;
            }
            if (wNode2 == null) {
                WNode wNode16 = new WNode(1, wNode9);
                if (U.compareAndSwapObject(this, WHEAD, null, wNode16)) {
                    this.wtail = wNode16;
                }
                wNode4 = wNode9;
                wNode3 = wNode;
            } else {
                WNode wNode17 = wNode;
                if (wNode17 == null) {
                    wNode10 = new WNode(0, wNode2);
                    wNode4 = wNode9;
                } else if (wNode11 == wNode2 || wNode2.mode != 0) {
                    wNode3 = wNode17;
                    if (wNode3.prev != wNode2) {
                        wNode3.prev = wNode2;
                    } else if (U.compareAndSwapObject(this, WTAIL, wNode2, wNode3)) {
                        wNode2.next = wNode3;
                        int i3 = -1;
                        loop4: while (true) {
                            WNode wNode18 = this.whead;
                            if (wNode18 == wNode2) {
                                if (i3 < 0) {
                                    i3 = HEAD_SPINS;
                                } else if (i3 < MAX_HEAD_SPINS) {
                                    i3 <<= 1;
                                }
                                int i4 = i3;
                                int i5 = i4;
                                while (true) {
                                    long j4 = this.state;
                                    long j5 = j4 & ABITS;
                                    if (j5 < RFULL) {
                                        jTryIncReaderOverflow = j4 + 1;
                                        if (U.compareAndSwapLong(this, STATE, j4, jTryIncReaderOverflow)) {
                                            break loop4;
                                        }
                                        if (j5 < WBIT && LockSupport.nextSecondarySeed() >= 0 && i5 - 1 <= 0) {
                                            i3 = i4;
                                            break;
                                        }
                                    } else {
                                        if (j5 < WBIT) {
                                            jTryIncReaderOverflow = tryIncReaderOverflow(j4);
                                            if (jTryIncReaderOverflow != 0) {
                                                break loop4;
                                            }
                                        }
                                        if (j5 < WBIT) {
                                        }
                                    }
                                }
                            } else if (wNode18 != null) {
                                while (true) {
                                    WNode wNode19 = wNode18.cowait;
                                    if (wNode19 == null) {
                                        break;
                                    }
                                    if (U.compareAndSwapObject(wNode18, WCOWAIT, wNode19, wNode19.cowait) && (thread = wNode19.thread) != null) {
                                        U.unpark(thread);
                                    }
                                }
                            }
                            if (this.whead == wNode18) {
                                WNode wNode20 = wNode3.prev;
                                if (wNode20 == wNode2) {
                                    int i6 = wNode2.status;
                                    if (i6 == 0) {
                                        U.compareAndSwapInt(wNode2, WSTATUS, 0, -1);
                                    } else if (i6 == 1) {
                                        WNode wNode21 = wNode2.prev;
                                        if (wNode21 != null) {
                                            wNode3.prev = wNode21;
                                            wNode21.next = wNode3;
                                        }
                                    } else {
                                        if (j == 0) {
                                            jNanoTime = 0;
                                        } else {
                                            jNanoTime = j - System.nanoTime();
                                            if (jNanoTime <= 0) {
                                                return cancelWaiter(wNode3, wNode3, false);
                                            }
                                        }
                                        Thread threadCurrentThread = Thread.currentThread();
                                        U.putObject(threadCurrentThread, PARKBLOCKER, this);
                                        wNode3.thread = threadCurrentThread;
                                        if (wNode2.status < 0 && ((wNode2 != wNode18 || (this.state & ABITS) == WBIT) && this.whead == wNode18 && wNode3.prev == wNode2)) {
                                            U.park(false, jNanoTime);
                                        }
                                        wNode3.thread = null;
                                        U.putObject(threadCurrentThread, PARKBLOCKER, null);
                                        if (Thread.interrupted()) {
                                            if (z) {
                                                return cancelWaiter(wNode3, wNode3, true);
                                            }
                                            z2 = true;
                                        }
                                    }
                                } else if (wNode20 != null) {
                                    wNode20.next = wNode3;
                                    wNode2 = wNode20;
                                }
                            }
                        }
                    }
                    wNode4 = null;
                } else {
                    Unsafe unsafe = U;
                    long j6 = WCOWAIT;
                    WNode wNode22 = wNode2.cowait;
                    wNode17.cowait = wNode22;
                    if (unsafe.compareAndSwapObject(wNode2, j6, wNode22, wNode17)) {
                        boolean z3 = z2;
                        while (true) {
                            WNode wNode23 = this.whead;
                            if (wNode23 != null && (wNode7 = wNode23.cowait) != null && U.compareAndSwapObject(wNode23, WCOWAIT, wNode7, wNode7.cowait) && (thread2 = wNode7.thread) != null) {
                                U.unpark(thread2);
                            }
                            WNode wNode24 = wNode2.prev;
                            if (wNode23 == wNode24 || wNode23 == wNode2 || wNode24 == null) {
                                while (true) {
                                    long j7 = this.state;
                                    long j8 = j7 & ABITS;
                                    if (j8 < RFULL) {
                                        jTryIncReaderOverflow2 = j7 + 1;
                                        wNode5 = wNode24;
                                        wNode6 = wNode17;
                                        if (U.compareAndSwapLong(this, STATE, j7, jTryIncReaderOverflow2)) {
                                            break loop0;
                                        }
                                        if (j8 < WBIT) {
                                            break;
                                        }
                                        wNode24 = wNode5;
                                        wNode17 = wNode6;
                                    } else {
                                        wNode5 = wNode24;
                                        wNode6 = wNode17;
                                        if (j8 < WBIT) {
                                            jTryIncReaderOverflow2 = tryIncReaderOverflow(j7);
                                            if (jTryIncReaderOverflow2 != 0) {
                                                break loop0;
                                            }
                                        }
                                        if (j8 < WBIT) {
                                        }
                                    }
                                }
                            } else {
                                wNode5 = wNode24;
                                wNode6 = wNode17;
                            }
                            if (this.whead == wNode23 && wNode2.prev == wNode5) {
                                if (wNode5 == null || wNode23 == wNode2 || wNode2.status > 0) {
                                    break;
                                }
                                if (j == 0) {
                                    jNanoTime2 = 0;
                                } else {
                                    jNanoTime2 = j - System.nanoTime();
                                    if (jNanoTime2 <= 0) {
                                        if (z3) {
                                            Thread.currentThread().interrupt();
                                        }
                                        return cancelWaiter(wNode6, wNode2, false);
                                    }
                                }
                                Thread threadCurrentThread2 = Thread.currentThread();
                                U.putObject(threadCurrentThread2, PARKBLOCKER, this);
                                wNode6.thread = threadCurrentThread2;
                                if ((wNode23 != wNode5 || (this.state & ABITS) == WBIT) && this.whead == wNode23 && wNode2.prev == wNode5) {
                                    U.park(false, jNanoTime2);
                                }
                                wNode6.thread = null;
                                U.putObject(threadCurrentThread2, PARKBLOCKER, null);
                                if (!Thread.interrupted()) {
                                    continue;
                                } else {
                                    if (z) {
                                        return cancelWaiter(wNode6, wNode2, true);
                                    }
                                    z3 = true;
                                }
                            }
                            wNode17 = wNode6;
                        }
                    } else {
                        wNode17.cowait = wNode9;
                        wNode4 = wNode9;
                        wNode3 = wNode17;
                        wNode10 = wNode3;
                    }
                }
                wNode9 = wNode4;
                i2 = i;
            }
            wNode10 = wNode3;
            wNode9 = wNode4;
            i2 = i;
        }
    }

    private long cancelWaiter(WNode wNode, WNode wNode2, boolean z) {
        WNode wNode3;
        WNode wNode4;
        WNode wNode5;
        Thread thread;
        if (wNode != null && wNode2 != null) {
            wNode.status = 1;
            loop0: while (true) {
                WNode wNode6 = wNode2;
                while (true) {
                    wNode3 = wNode6.cowait;
                    if (wNode3 == null) {
                        break loop0;
                    }
                    if (wNode3.status == 1) {
                        break;
                    }
                    wNode6 = wNode3;
                }
                U.compareAndSwapObject(wNode6, WCOWAIT, wNode3, wNode3.cowait);
            }
            if (wNode2 == wNode) {
                while (true) {
                    wNode2 = wNode2.cowait;
                    if (wNode2 == null) {
                        break;
                    }
                    Thread thread2 = wNode2.thread;
                    if (thread2 != null) {
                        U.unpark(thread2);
                    }
                }
                WNode wNode7 = wNode.prev;
                while (wNode7 != null) {
                    while (true) {
                        WNode wNode8 = wNode.next;
                        if (wNode8 == null || wNode8.status == 1) {
                            wNode4 = null;
                            for (WNode wNode9 = this.wtail; wNode9 != null && wNode9 != wNode; wNode9 = wNode9.prev) {
                                if (wNode9.status != 1) {
                                    wNode4 = wNode9;
                                }
                            }
                            if (wNode8 != wNode4) {
                                if (U.compareAndSwapObject(wNode, WNEXT, wNode8, wNode4)) {
                                    break;
                                }
                            } else {
                                wNode4 = wNode8;
                                break;
                            }
                        } else {
                            wNode4 = wNode8;
                            break;
                        }
                    }
                    if (wNode7.next == wNode) {
                        U.compareAndSwapObject(wNode7, WNEXT, wNode, wNode4);
                    }
                    if (wNode4 != null && (thread = wNode4.thread) != null) {
                        wNode4.thread = null;
                        U.unpark(thread);
                    }
                    if (wNode7.status != 1 || (wNode5 = wNode7.prev) == null) {
                        break;
                    }
                    wNode.prev = wNode5;
                    U.compareAndSwapObject(wNode5, WNEXT, wNode7, wNode4);
                    wNode7 = wNode5;
                }
            }
        }
        while (true) {
            WNode wNode10 = this.whead;
            if (wNode10 == null) {
                break;
            }
            WNode wNode11 = wNode10.next;
            if (wNode11 == null || wNode11.status == 1) {
                for (WNode wNode12 = this.wtail; wNode12 != null && wNode12 != wNode10; wNode12 = wNode12.prev) {
                    if (wNode12.status <= 0) {
                        wNode11 = wNode12;
                    }
                }
            }
            if (wNode10 == this.whead) {
                if (wNode11 != null && wNode10.status == 0) {
                    long j = this.state;
                    if ((ABITS & j) != WBIT && (j == 0 || wNode11.mode == 0)) {
                        release(wNode10);
                    }
                }
            }
        }
        return (z || Thread.interrupted()) ? 1L : 0L;
    }
}
