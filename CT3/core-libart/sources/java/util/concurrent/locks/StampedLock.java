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

        WNode(int m, WNode p) {
            this.mode = m;
            this.prev = p;
        }
    }

    public long writeLock() {
        long s = this.state;
        if ((ABITS & s) == 0) {
            Unsafe unsafe = U;
            long j = STATE;
            long next = s + WBIT;
            if (unsafe.compareAndSwapLong(this, j, s, next)) {
                return next;
            }
        }
        return acquireWrite(false, 0L);
    }

    public long tryWriteLock() {
        long s = this.state;
        if ((ABITS & s) == 0) {
            Unsafe unsafe = U;
            long j = STATE;
            long next = s + WBIT;
            if (unsafe.compareAndSwapLong(this, j, s, next)) {
                return next;
            }
        }
        return 0L;
    }

    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long next = tryWriteLock();
            if (next != 0) {
                return next;
            }
            if (nanos <= 0) {
                return 0L;
            }
            long deadline = System.nanoTime() + nanos;
            if (deadline == 0) {
                deadline = 1;
            }
            long next2 = acquireWrite(true, deadline);
            if (next2 != 1) {
                return next2;
            }
        }
        throw new InterruptedException();
    }

    public long writeLockInterruptibly() throws InterruptedException {
        if (!Thread.interrupted()) {
            long next = acquireWrite(true, 0L);
            if (next != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long readLock() {
        long s = this.state;
        if (this.whead == this.wtail && (ABITS & s) < RFULL) {
            long next = s + 1;
            if (U.compareAndSwapLong(this, STATE, s, next)) {
                return next;
            }
        }
        return acquireRead(false, 0L);
    }

    public long tryReadLock() {
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m == WBIT) {
                return 0L;
            }
            if (m < RFULL) {
                long next = s + 1;
                if (U.compareAndSwapLong(this, STATE, s, next)) {
                    return next;
                }
            } else {
                long next2 = tryIncReaderOverflow(s);
                if (next2 != 0) {
                    return next2;
                }
            }
        }
    }

    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long s = this.state;
            long m = s & ABITS;
            if (m != WBIT) {
                if (m < RFULL) {
                    long next = s + 1;
                    if (U.compareAndSwapLong(this, STATE, s, next)) {
                        return next;
                    }
                } else {
                    long next2 = tryIncReaderOverflow(s);
                    if (next2 != 0) {
                        return next2;
                    }
                }
            }
            if (nanos <= 0) {
                return 0L;
            }
            long deadline = System.nanoTime() + nanos;
            if (deadline == 0) {
                deadline = 1;
            }
            long next3 = acquireRead(true, deadline);
            if (next3 != 1) {
                return next3;
            }
        }
        throw new InterruptedException();
    }

    public long readLockInterruptibly() throws InterruptedException {
        if (!Thread.interrupted()) {
            long next = acquireRead(true, 0L);
            if (next != 1) {
                return next;
            }
        }
        throw new InterruptedException();
    }

    public long tryOptimisticRead() {
        long s = this.state;
        if ((WBIT & s) == 0) {
            return SBITS & s;
        }
        return 0L;
    }

    public boolean validate(long stamp) {
        U.loadFence();
        return (stamp & SBITS) == (this.state & SBITS);
    }

    public void unlockWrite(long stamp) {
        if (this.state != stamp || (stamp & WBIT) == 0) {
            throw new IllegalMonitorStateException();
        }
        Unsafe unsafe = U;
        long j = STATE;
        long stamp2 = stamp + WBIT;
        unsafe.putLongVolatile(this, j, stamp2 == 0 ? ORIGIN : stamp2);
        WNode h = this.whead;
        if (h == null || h.status == 0) {
            return;
        }
        release(h);
    }

    public void unlockRead(long stamp) {
        WNode h;
        while (true) {
            long s = this.state;
            if ((SBITS & s) != (SBITS & stamp) || (ABITS & stamp) == 0) {
                break;
            }
            long m = s & ABITS;
            if (m == 0 || m == WBIT) {
                break;
            }
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                    if (m != 1 || (h = this.whead) == null || h.status == 0) {
                        return;
                    }
                    release(h);
                    return;
                }
            } else if (tryDecReaderOverflow(s) != 0) {
                return;
            }
        }
    }

    public void unlock(long stamp) {
        WNode h;
        long a = stamp & ABITS;
        while (true) {
            long s = this.state;
            if ((SBITS & s) != (SBITS & stamp)) {
                break;
            }
            long m = s & ABITS;
            if (m == 0) {
                break;
            }
            if (m == WBIT) {
                if (a == m) {
                    Unsafe unsafe = U;
                    long j = STATE;
                    long s2 = s + WBIT;
                    if (s2 == 0) {
                        s2 = ORIGIN;
                    }
                    unsafe.putLongVolatile(this, j, s2);
                    WNode h2 = this.whead;
                    if (h2 != null && h2.status != 0) {
                        release(h2);
                        return;
                    }
                    return;
                }
            } else {
                if (a == 0 || a >= WBIT) {
                    break;
                }
                if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                        if (m == 1 && (h = this.whead) != null && h.status != 0) {
                            release(h);
                            return;
                        }
                        return;
                    }
                } else if (tryDecReaderOverflow(s) != 0) {
                    return;
                }
            }
        }
    }

    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS;
        while (true) {
            long s = this.state;
            if ((SBITS & s) == (SBITS & stamp)) {
                long m = s & ABITS;
                if (m == 0) {
                    if (a == 0) {
                        Unsafe unsafe = U;
                        long j = STATE;
                        long next = s + WBIT;
                        if (unsafe.compareAndSwapLong(this, j, s, next)) {
                            return next;
                        }
                    } else {
                        return 0L;
                    }
                } else {
                    if (m == WBIT) {
                        if (a == m) {
                            return stamp;
                        }
                        return 0L;
                    }
                    if (m == 1 && a != 0) {
                        Unsafe unsafe2 = U;
                        long j2 = STATE;
                        long next2 = (s - 1) + WBIT;
                        if (unsafe2.compareAndSwapLong(this, j2, s, next2)) {
                            return next2;
                        }
                    } else {
                        return 0L;
                    }
                }
            } else {
                return 0L;
            }
        }
    }

    public long tryConvertToReadLock(long stamp) {
        long a = stamp & ABITS;
        while (true) {
            long s = this.state;
            if ((SBITS & s) == (SBITS & stamp)) {
                long m = s & ABITS;
                if (m != 0) {
                    if (m != WBIT) {
                        if (a != 0 && a < WBIT) {
                            return stamp;
                        }
                        return 0L;
                    }
                    if (a == m) {
                        long next = s + 129;
                        U.putLongVolatile(this, STATE, next);
                        WNode h = this.whead;
                        if (h != null && h.status != 0) {
                            release(h);
                        }
                        return next;
                    }
                    return 0L;
                }
                if (a != 0) {
                    return 0L;
                }
                if (m < RFULL) {
                    long next2 = s + 1;
                    if (U.compareAndSwapLong(this, STATE, s, next2)) {
                        return next2;
                    }
                } else {
                    long next3 = tryIncReaderOverflow(s);
                    if (next3 != 0) {
                        return next3;
                    }
                }
            } else {
                return 0L;
            }
        }
    }

    public long tryConvertToOptimisticRead(long stamp) {
        WNode h;
        long a = stamp & ABITS;
        U.loadFence();
        while (true) {
            long s = this.state;
            if ((SBITS & s) == (SBITS & stamp)) {
                long m = s & ABITS;
                if (m == 0) {
                    if (a == 0) {
                        return s;
                    }
                    return 0L;
                }
                if (m == WBIT) {
                    if (a == m) {
                        Unsafe unsafe = U;
                        long j = STATE;
                        long s2 = s + WBIT;
                        long next = s2 == 0 ? ORIGIN : s2;
                        unsafe.putLongVolatile(this, j, next);
                        WNode h2 = this.whead;
                        if (h2 != null && h2.status != 0) {
                            release(h2);
                        }
                        return next;
                    }
                    return 0L;
                }
                if (a == 0 || a >= WBIT) {
                    return 0L;
                }
                if (m < RFULL) {
                    long next2 = s - 1;
                    if (U.compareAndSwapLong(this, STATE, s, next2)) {
                        if (m == 1 && (h = this.whead) != null && h.status != 0) {
                            release(h);
                        }
                        return SBITS & next2;
                    }
                } else {
                    long next3 = tryDecReaderOverflow(s);
                    if (next3 != 0) {
                        return SBITS & next3;
                    }
                }
            } else {
                return 0L;
            }
        }
    }

    public boolean tryUnlockWrite() {
        long s = this.state;
        if ((s & WBIT) == 0) {
            return false;
        }
        Unsafe unsafe = U;
        long j = STATE;
        long s2 = s + WBIT;
        unsafe.putLongVolatile(this, j, s2 == 0 ? ORIGIN : s2);
        WNode h = this.whead;
        if (h != null && h.status != 0) {
            release(h);
            return true;
        }
        return true;
    }

    public boolean tryUnlockRead() {
        WNode h;
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m == 0 || m >= WBIT) {
                return false;
            }
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                    if (m == 1 && (h = this.whead) != null && h.status != 0) {
                        release(h);
                        return true;
                    }
                    return true;
                }
            } else if (tryDecReaderOverflow(s) != 0) {
                return true;
            }
        }
    }

    private int getReadLockCount(long s) {
        long readers = s & RBITS;
        if (readers >= RFULL) {
            readers = RFULL + ((long) this.readerOverflow);
        }
        return (int) readers;
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
        long s = this.state;
        StringBuilder sbAppend = new StringBuilder().append(super.toString());
        if ((ABITS & s) == 0) {
            str = "[Unlocked]";
        } else {
            str = (WBIT & s) != 0 ? "[Write-locked]" : "[Read-locks:" + getReadLockCount(s) + "]";
        }
        return sbAppend.append(str).toString();
    }

    public Lock asReadLock() {
        ReadLockView v = this.readLockView;
        if (v != null) {
            return v;
        }
        ReadLockView v2 = new ReadLockView();
        this.readLockView = v2;
        return v2;
    }

    public Lock asWriteLock() {
        WriteLockView v = this.writeLockView;
        if (v != null) {
            return v;
        }
        WriteLockView v2 = new WriteLockView();
        this.writeLockView = v2;
        return v2;
    }

    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v = this.readWriteLockView;
        if (v != null) {
            return v;
        }
        ReadWriteLockView v2 = new ReadWriteLockView();
        this.readWriteLockView = v2;
        return v2;
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
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return StampedLock.this.tryReadLock(time, unit) != 0;
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
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return StampedLock.this.tryWriteLock(time, unit) != 0;
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
        long s = this.state;
        if ((s & WBIT) == 0) {
            throw new IllegalMonitorStateException();
        }
        Unsafe unsafe = U;
        long j = STATE;
        long s2 = s + WBIT;
        unsafe.putLongVolatile(this, j, s2 == 0 ? ORIGIN : s2);
        WNode h = this.whead;
        if (h == null || h.status == 0) {
            return;
        }
        release(h);
    }

    final void unstampedUnlockRead() {
        WNode h;
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m == 0 || m >= WBIT) {
                break;
            }
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - 1)) {
                    if (m != 1 || (h = this.whead) == null || h.status == 0) {
                        return;
                    }
                    release(h);
                    return;
                }
            } else if (tryDecReaderOverflow(s) != 0) {
                return;
            }
        }
    }

    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        s.defaultReadObject();
        U.putLongVolatile(this, STATE, ORIGIN);
    }

    private long tryIncReaderOverflow(long s) {
        if ((ABITS & s) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                this.readerOverflow++;
                U.putLongVolatile(this, STATE, s);
                return s;
            }
            return 0L;
        }
        if ((LockSupport.nextSecondarySeed() & 7) == 0) {
            Thread.yield();
            return 0L;
        }
        return 0L;
    }

    private long tryDecReaderOverflow(long s) {
        long next;
        if ((ABITS & s) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, RBITS | s)) {
                int r = this.readerOverflow;
                if (r > 0) {
                    this.readerOverflow = r - 1;
                    next = s;
                } else {
                    next = s - 1;
                }
                U.putLongVolatile(this, STATE, next);
                return next;
            }
            return 0L;
        }
        if ((LockSupport.nextSecondarySeed() & 7) == 0) {
            Thread.yield();
            return 0L;
        }
        return 0L;
    }

    private void release(WNode h) {
        Thread w;
        if (h == null) {
            return;
        }
        U.compareAndSwapInt(h, WSTATUS, -1, 0);
        WNode q = h.next;
        if (q == null || q.status == 1) {
            for (WNode t = this.wtail; t != null && t != h; t = t.prev) {
                if (t.status <= 0) {
                    q = t;
                }
            }
        }
        if (q == null || (w = q.thread) == null) {
            return;
        }
        U.unpark(w);
    }

    private long acquireWrite(boolean interruptible, long deadline) {
        Thread w;
        long time;
        WNode node = null;
        int spins = -1;
        while (true) {
            long s = this.state;
            long m = s & ABITS;
            if (m == 0) {
                Unsafe unsafe = U;
                long j = STATE;
                long ns = s + WBIT;
                if (unsafe.compareAndSwapLong(this, j, s, ns)) {
                    return ns;
                }
            } else if (spins < 0) {
                spins = (m == WBIT && this.wtail == this.whead) ? SPINS : 0;
            } else if (spins > 0) {
                if (LockSupport.nextSecondarySeed() >= 0) {
                    spins--;
                }
            } else {
                WNode p = this.wtail;
                if (p == null) {
                    WNode hd = new WNode(1, null);
                    if (U.compareAndSwapObject(this, WHEAD, (Object) null, hd)) {
                        this.wtail = hd;
                    }
                } else if (node == null) {
                    node = new WNode(1, p);
                } else if (node.prev != p) {
                    node.prev = p;
                } else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                    p.next = node;
                    boolean wasInterrupted = false;
                    int spins2 = -1;
                    while (true) {
                        WNode h = this.whead;
                        if (h == p) {
                            if (spins2 < 0) {
                                spins2 = HEAD_SPINS;
                            } else if (spins2 < MAX_HEAD_SPINS) {
                                spins2 <<= 1;
                            }
                            int k = spins2;
                            while (true) {
                                long s2 = this.state;
                                if ((ABITS & s2) == 0) {
                                    Unsafe unsafe2 = U;
                                    long j2 = STATE;
                                    long ns2 = s2 + WBIT;
                                    if (unsafe2.compareAndSwapLong(this, j2, s2, ns2)) {
                                        this.whead = node;
                                        node.prev = null;
                                        if (wasInterrupted) {
                                            Thread.currentThread().interrupt();
                                        }
                                        return ns2;
                                    }
                                } else if (LockSupport.nextSecondarySeed() >= 0 && k - 1 <= 0) {
                                    break;
                                }
                            }
                        } else if (h != null) {
                            while (true) {
                                WNode c = h.cowait;
                                if (c == null) {
                                    break;
                                }
                                if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) && (w = c.thread) != null) {
                                    U.unpark(w);
                                }
                            }
                        }
                        if (this.whead == h) {
                            WNode np = node.prev;
                            if (np != p) {
                                if (np != null) {
                                    p = np;
                                    np.next = node;
                                }
                            } else {
                                int ps = p.status;
                                if (ps == 0) {
                                    U.compareAndSwapInt(p, WSTATUS, 0, -1);
                                } else if (ps == 1) {
                                    WNode pp = p.prev;
                                    if (pp != null) {
                                        node.prev = pp;
                                        pp.next = node;
                                    }
                                } else {
                                    if (deadline == 0) {
                                        time = 0;
                                    } else {
                                        time = deadline - System.nanoTime();
                                        if (time <= 0) {
                                            return cancelWaiter(node, node, false);
                                        }
                                    }
                                    Thread wt = Thread.currentThread();
                                    U.putObject(wt, PARKBLOCKER, this);
                                    node.thread = wt;
                                    if (p.status < 0 && ((p != h || (this.state & ABITS) != 0) && this.whead == h && node.prev == p)) {
                                        U.park(false, time);
                                    }
                                    node.thread = null;
                                    U.putObject(wt, PARKBLOCKER, (Object) null);
                                    if (!Thread.interrupted()) {
                                        continue;
                                    } else {
                                        if (interruptible) {
                                            return cancelWaiter(node, node, true);
                                        }
                                        wasInterrupted = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private long acquireRead(boolean interruptible, long deadline) {
        WNode h;
        Thread w;
        long time;
        long ns;
        long m;
        long ns2;
        long time2;
        WNode c;
        Thread w2;
        long ns3;
        boolean wasInterrupted = false;
        WNode node = null;
        int spins = -1;
        loop0: while (true) {
            WNode h2 = this.whead;
            WNode p = this.wtail;
            if (h2 == p) {
                while (true) {
                    long s = this.state;
                    long m2 = s & ABITS;
                    if (m2 < RFULL) {
                        ns3 = s + 1;
                        if (U.compareAndSwapLong(this, STATE, s, ns3)) {
                            break loop0;
                        }
                        if (m2 >= WBIT) {
                            if (spins > 0) {
                                if (LockSupport.nextSecondarySeed() >= 0) {
                                    spins--;
                                }
                            } else {
                                if (spins == 0) {
                                    WNode nh = this.whead;
                                    WNode np = this.wtail;
                                    if (nh != h2 || np != p) {
                                        h2 = nh;
                                        p = np;
                                        if (nh != np) {
                                            break;
                                        }
                                    } else {
                                        break;
                                    }
                                }
                                spins = SPINS;
                            }
                        }
                    } else {
                        if (m2 < WBIT) {
                            ns3 = tryIncReaderOverflow(s);
                            if (ns3 != 0) {
                                break loop0;
                            }
                        }
                        if (m2 >= WBIT) {
                        }
                    }
                }
                h = h2;
            } else {
                h = h2;
            }
            if (p == null) {
                WNode hd = new WNode(1, null);
                if (U.compareAndSwapObject(this, WHEAD, (Object) null, hd)) {
                    this.wtail = hd;
                }
            } else if (node == null) {
                node = new WNode(0, p);
            } else if (h == p || p.mode != 0) {
                if (node.prev != p) {
                    node.prev = p;
                } else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                    p.next = node;
                    int spins2 = -1;
                    loop2: while (true) {
                        WNode h3 = this.whead;
                        if (h3 == p) {
                            if (spins2 < 0) {
                                spins2 = HEAD_SPINS;
                            } else if (spins2 < MAX_HEAD_SPINS) {
                                spins2 <<= 1;
                            }
                            int k = spins2;
                            while (true) {
                                long s2 = this.state;
                                long m3 = s2 & ABITS;
                                if (m3 < RFULL) {
                                    ns = s2 + 1;
                                    if (U.compareAndSwapLong(this, STATE, s2, ns)) {
                                        break loop2;
                                    }
                                    if (m3 < WBIT && LockSupport.nextSecondarySeed() >= 0 && k - 1 <= 0) {
                                        break;
                                    }
                                } else {
                                    if (m3 < WBIT) {
                                        ns = tryIncReaderOverflow(s2);
                                        if (ns != 0) {
                                            break loop2;
                                        }
                                    }
                                    if (m3 < WBIT) {
                                    }
                                }
                            }
                        } else if (h3 != null) {
                            while (true) {
                                WNode c2 = h3.cowait;
                                if (c2 == null) {
                                    break;
                                }
                                if (U.compareAndSwapObject(h3, WCOWAIT, c2, c2.cowait) && (w = c2.thread) != null) {
                                    U.unpark(w);
                                }
                            }
                        }
                        if (this.whead == h3) {
                            WNode np2 = node.prev;
                            if (np2 != p) {
                                if (np2 != null) {
                                    p = np2;
                                    np2.next = node;
                                }
                            } else {
                                int ps = p.status;
                                if (ps == 0) {
                                    U.compareAndSwapInt(p, WSTATUS, 0, -1);
                                } else if (ps == 1) {
                                    WNode pp = p.prev;
                                    if (pp != null) {
                                        node.prev = pp;
                                        pp.next = node;
                                    }
                                } else {
                                    if (deadline == 0) {
                                        time = 0;
                                    } else {
                                        time = deadline - System.nanoTime();
                                        if (time <= 0) {
                                            return cancelWaiter(node, node, false);
                                        }
                                    }
                                    Thread wt = Thread.currentThread();
                                    U.putObject(wt, PARKBLOCKER, this);
                                    node.thread = wt;
                                    if (p.status < 0 && ((p != h3 || (this.state & ABITS) == WBIT) && this.whead == h3 && node.prev == p)) {
                                        U.park(false, time);
                                    }
                                    node.thread = null;
                                    U.putObject(wt, PARKBLOCKER, (Object) null);
                                    if (!Thread.interrupted()) {
                                        continue;
                                    } else {
                                        if (interruptible) {
                                            return cancelWaiter(node, node, true);
                                        }
                                        wasInterrupted = true;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Unsafe unsafe = U;
                long j = WCOWAIT;
                WNode wNode = p.cowait;
                node.cowait = wNode;
                if (unsafe.compareAndSwapObject(p, j, wNode, node)) {
                    while (true) {
                        WNode h4 = this.whead;
                        if (h4 != null && (c = h4.cowait) != null && U.compareAndSwapObject(h4, WCOWAIT, c, c.cowait) && (w2 = c.thread) != null) {
                            U.unpark(w2);
                        }
                        WNode pp2 = p.prev;
                        if (h4 == pp2 || h4 == p || pp2 == null) {
                            do {
                                long s3 = this.state;
                                m = s3 & ABITS;
                                if (m >= RFULL) {
                                    if (m < WBIT) {
                                        ns2 = tryIncReaderOverflow(s3);
                                        if (ns2 != 0) {
                                            break loop0;
                                        }
                                    }
                                } else {
                                    ns2 = s3 + 1;
                                    if (U.compareAndSwapLong(this, STATE, s3, ns2)) {
                                        break loop0;
                                    }
                                }
                            } while (m < WBIT);
                        }
                        if (this.whead == h4 && p.prev == pp2) {
                            if (pp2 == null || h4 == p || p.status > 0) {
                                break;
                            }
                            if (deadline == 0) {
                                time2 = 0;
                            } else {
                                time2 = deadline - System.nanoTime();
                                if (time2 <= 0) {
                                    if (wasInterrupted) {
                                        Thread.currentThread().interrupt();
                                    }
                                    return cancelWaiter(node, p, false);
                                }
                            }
                            Thread wt2 = Thread.currentThread();
                            U.putObject(wt2, PARKBLOCKER, this);
                            node.thread = wt2;
                            if ((h4 != pp2 || (this.state & ABITS) == WBIT) && this.whead == h4 && p.prev == pp2) {
                                U.park(false, time2);
                            }
                            node.thread = null;
                            U.putObject(wt2, PARKBLOCKER, (Object) null);
                            if (!Thread.interrupted()) {
                                continue;
                            } else {
                                if (interruptible) {
                                    return cancelWaiter(node, p, true);
                                }
                                wasInterrupted = true;
                            }
                        }
                    }
                } else {
                    node.cowait = null;
                }
            }
        }
    }

    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        WNode succ;
        WNode pp;
        Thread w;
        if (node != null && group != null) {
            node.status = 1;
            WNode p = group;
            while (true) {
                WNode q = p.cowait;
                if (q == null) {
                    break;
                }
                if (q.status == 1) {
                    U.compareAndSwapObject(p, WCOWAIT, q, q.cowait);
                    p = group;
                } else {
                    p = q;
                }
            }
            if (group == node) {
                for (WNode r = group.cowait; r != null; r = r.cowait) {
                    Thread w2 = r.thread;
                    if (w2 != null) {
                        U.unpark(w2);
                    }
                }
                WNode pred = node.prev;
                while (pred != null) {
                    while (true) {
                        succ = node.next;
                        if (succ != null && succ.status != 1) {
                            break;
                        }
                        WNode q2 = null;
                        for (WNode t = this.wtail; t != null && t != node; t = t.prev) {
                            if (t.status != 1) {
                                q2 = t;
                            }
                        }
                        if (succ == q2) {
                            break;
                        }
                        WNode succ2 = q2;
                        if (U.compareAndSwapObject(node, WNEXT, succ, q2)) {
                            succ = succ2;
                            break;
                        }
                    }
                    if (pred.next == node) {
                        U.compareAndSwapObject(pred, WNEXT, node, succ);
                    }
                    if (succ != null && (w = succ.thread) != null) {
                        succ.thread = null;
                        U.unpark(w);
                    }
                    if (pred.status != 1 || (pp = pred.prev) == null) {
                        break;
                    }
                    node.prev = pp;
                    U.compareAndSwapObject(pp, WNEXT, pred, succ);
                    pred = pp;
                }
            }
        }
        while (true) {
            WNode h = this.whead;
            if (h == null) {
                break;
            }
            WNode q3 = h.next;
            if (q3 == null || q3.status == 1) {
                for (WNode t2 = this.wtail; t2 != null && t2 != h; t2 = t2.prev) {
                    if (t2.status <= 0) {
                        q3 = t2;
                    }
                }
            }
            if (h == this.whead) {
                if (q3 != null && h.status == 0) {
                    long s = this.state;
                    if ((ABITS & s) != WBIT && (s == 0 || q3.mode == 0)) {
                        release(h);
                    }
                }
            }
        }
        return (interrupted || Thread.interrupted()) ? 1L : 0L;
    }
}
