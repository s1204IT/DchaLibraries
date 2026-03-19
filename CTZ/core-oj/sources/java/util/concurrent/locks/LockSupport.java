package java.util.concurrent.locks;

import java.util.concurrent.ThreadLocalRandom;
import sun.misc.Unsafe;

public class LockSupport {
    private static final long PARKBLOCKER;
    private static final long SECONDARY;
    private static final Unsafe U = Unsafe.getUnsafe();

    private LockSupport() {
    }

    private static void setBlocker(Thread thread, Object obj) {
        U.putObject(thread, PARKBLOCKER, obj);
    }

    public static void unpark(Thread thread) {
        if (thread != null) {
            U.unpark(thread);
        }
    }

    public static void park(Object obj) {
        Thread threadCurrentThread = Thread.currentThread();
        setBlocker(threadCurrentThread, obj);
        U.park(false, 0L);
        setBlocker(threadCurrentThread, null);
    }

    public static void parkNanos(Object obj, long j) {
        if (j > 0) {
            Thread threadCurrentThread = Thread.currentThread();
            setBlocker(threadCurrentThread, obj);
            U.park(false, j);
            setBlocker(threadCurrentThread, null);
        }
    }

    public static void parkUntil(Object obj, long j) {
        Thread threadCurrentThread = Thread.currentThread();
        setBlocker(threadCurrentThread, obj);
        U.park(true, j);
        setBlocker(threadCurrentThread, null);
    }

    public static Object getBlocker(Thread thread) {
        if (thread == null) {
            throw new NullPointerException();
        }
        return U.getObjectVolatile(thread, PARKBLOCKER);
    }

    public static void park() {
        U.park(false, 0L);
    }

    public static void parkNanos(long j) {
        if (j > 0) {
            U.park(false, j);
        }
    }

    public static void parkUntil(long j) {
        U.park(true, j);
    }

    static final int nextSecondarySeed() {
        int iNextInt;
        Thread threadCurrentThread = Thread.currentThread();
        int i = U.getInt(threadCurrentThread, SECONDARY);
        if (i != 0) {
            int i2 = i ^ (i << 13);
            int i3 = i2 ^ (i2 >>> 17);
            iNextInt = i3 ^ (i3 << 5);
        } else {
            iNextInt = ThreadLocalRandom.current().nextInt();
            if (iNextInt == 0) {
                iNextInt = 1;
            }
        }
        U.putInt(threadCurrentThread, SECONDARY, iNextInt);
        return iNextInt;
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
