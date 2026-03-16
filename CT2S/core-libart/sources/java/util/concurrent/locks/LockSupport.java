package java.util.concurrent.locks;

import sun.misc.Unsafe;

public class LockSupport {
    private static final long parkBlockerOffset;
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    private LockSupport() {
    }

    static {
        try {
            parkBlockerOffset = unsafe.objectFieldOffset(Thread.class.getDeclaredField("parkBlocker"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private static void setBlocker(Thread t, Object arg) {
        unsafe.putObject(t, parkBlockerOffset, arg);
    }

    public static void unpark(Thread thread) {
        if (thread != null) {
            unsafe.unpark(thread);
        }
    }

    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        unsafe.park(false, 0L);
        setBlocker(t, null);
    }

    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            unsafe.park(false, nanos);
            setBlocker(t, null);
        }
    }

    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        unsafe.park(true, deadline);
        setBlocker(t, null);
    }

    public static Object getBlocker(Thread t) {
        if (t == null) {
            throw new NullPointerException();
        }
        return unsafe.getObjectVolatile(t, parkBlockerOffset);
    }

    public static void park() {
        unsafe.park(false, 0L);
    }

    public static void parkNanos(long nanos) {
        if (nanos > 0) {
            unsafe.park(false, nanos);
        }
    }

    public static void parkUntil(long deadline) {
        unsafe.park(true, deadline);
    }
}
