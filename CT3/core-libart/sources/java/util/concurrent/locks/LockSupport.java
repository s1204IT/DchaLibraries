package java.util.concurrent.locks;

import java.util.concurrent.ThreadLocalRandom;
import sun.misc.Unsafe;

public class LockSupport {
    private static final long PARKBLOCKER;
    private static final long SECONDARY;
    private static final Unsafe U = Unsafe.getUnsafe();

    private LockSupport() {
    }

    private static void setBlocker(Thread t, Object arg) {
        U.putObject(t, PARKBLOCKER, arg);
    }

    public static void unpark(Thread thread) {
        if (thread == null) {
            return;
        }
        U.unpark(thread);
    }

    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        U.park(false, 0L);
        setBlocker(t, null);
    }

    public static void parkNanos(Object blocker, long nanos) {
        if (nanos <= 0) {
            return;
        }
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        U.park(false, nanos);
        setBlocker(t, null);
    }

    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        U.park(true, deadline);
        setBlocker(t, null);
    }

    public static Object getBlocker(Thread t) {
        if (t == null) {
            throw new NullPointerException();
        }
        return U.getObjectVolatile(t, PARKBLOCKER);
    }

    public static void park() {
        U.park(false, 0L);
    }

    public static void parkNanos(long nanos) {
        if (nanos <= 0) {
            return;
        }
        U.park(false, nanos);
    }

    public static void parkUntil(long deadline) {
        U.park(true, deadline);
    }

    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        int r2 = U.getInt(t, SECONDARY);
        if (r2 != 0) {
            int r3 = r2 ^ (r2 << 13);
            int r4 = r3 ^ (r3 >>> 17);
            r = r4 ^ (r4 << 5);
        } else {
            r = ThreadLocalRandom.current().nextInt();
            if (r == 0) {
                r = 1;
            }
        }
        U.putInt(t, SECONDARY, r);
        return r;
    }

    static {
        try {
            PARKBLOCKER = U.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
            SECONDARY = U.objectFieldOffset(Thread.class.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }
}
