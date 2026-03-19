package java.lang;

import dalvik.annotation.optimization.FastNative;
import dalvik.system.VMStack;
import java.lang.ThreadLocal;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import libcore.util.EmptyArray;
import sun.nio.ch.Interruptible;
import sun.reflect.CallerSensitive;

public class Thread implements Runnable {
    public static final int MAX_PRIORITY = 10;
    public static final int MIN_PRIORITY = 1;
    private static final int NANOS_PER_MILLI = 1000000;
    public static final int NORM_PRIORITY = 5;
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    private static int threadInitNumber;
    private static long threadSeqNumber;
    private static volatile UncaughtExceptionHandler uncaughtExceptionPreHandler;
    private volatile Interruptible blocker;
    private final Object blockerLock;
    private ClassLoader contextClassLoader;
    private boolean daemon;
    private long eetop;
    private ThreadGroup group;
    ThreadLocal.ThreadLocalMap inheritableThreadLocals;
    private AccessControlContext inheritedAccessControlContext;
    private final Object lock;
    private volatile String name;
    private long nativeParkEventPointer;
    private volatile long nativePeer;
    volatile Object parkBlocker;
    private int parkState;
    private int priority;
    private boolean single_step;
    private long stackSize;
    boolean started;
    private boolean stillborn;
    private Runnable target;
    int threadLocalRandomProbe;
    int threadLocalRandomSecondarySeed;
    long threadLocalRandomSeed;
    ThreadLocal.ThreadLocalMap threadLocals;
    private Thread threadQ;
    private volatile int threadStatus;
    private long tid;
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;
    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];
    private static final RuntimePermission SUBCLASS_IMPLEMENTATION_PERMISSION = new RuntimePermission("enableContextClassLoaderOverride");

    public enum State {
        NEW,
        RUNNABLE,
        BLOCKED,
        WAITING,
        TIMED_WAITING,
        TERMINATED
    }

    @FunctionalInterface
    public interface UncaughtExceptionHandler {
        void uncaughtException(Thread thread, Throwable th);
    }

    @FastNative
    public static native Thread currentThread();

    @FastNative
    public static native boolean interrupted();

    private static native void nativeCreate(Thread thread, long j, boolean z);

    private native int nativeGetStatus(boolean z);

    private native boolean nativeHoldsLock(Object obj);

    @FastNative
    private native void nativeInterrupt();

    private native void nativeSetName(String str);

    private native void nativeSetPriority(int i);

    @FastNative
    private static native void sleep(Object obj, long j, int i) throws InterruptedException;

    public static native void yield();

    @FastNative
    public native boolean isInterrupted();

    private static synchronized int nextThreadNum() {
        int i;
        i = threadInitNumber;
        threadInitNumber = i + 1;
        return i;
    }

    private static synchronized long nextThreadID() {
        long j;
        j = threadSeqNumber + 1;
        threadSeqNumber = j;
        return j;
    }

    public void blockedOn(Interruptible interruptible) {
        synchronized (this.blockerLock) {
            this.blocker = interruptible;
        }
    }

    public static void sleep(long j) throws InterruptedException {
        sleep(j, 0);
    }

    public static void sleep(long j, int i) throws InterruptedException {
        if (j < 0) {
            throw new IllegalArgumentException("millis < 0: " + j);
        }
        if (i < 0) {
            throw new IllegalArgumentException("nanos < 0: " + i);
        }
        if (i > 999999) {
            throw new IllegalArgumentException("nanos > 999999: " + i);
        }
        if (j == 0 && i == 0) {
            if (interrupted()) {
                throw new InterruptedException();
            }
            return;
        }
        long jNanoTime = System.nanoTime();
        long j2 = (j * 1000000) + ((long) i);
        Object obj = currentThread().lock;
        synchronized (obj) {
            while (true) {
                sleep(obj, j, i);
                long jNanoTime2 = System.nanoTime();
                long j3 = jNanoTime2 - jNanoTime;
                if (j3 < j2) {
                    j2 -= j3;
                    i = (int) (j2 % 1000000);
                    j = j2 / 1000000;
                    jNanoTime = jNanoTime2;
                }
            }
        }
    }

    private void init(ThreadGroup threadGroup, Runnable runnable, String str, long j) {
        Thread threadCurrentThread = currentThread();
        if (threadGroup == null) {
            threadGroup = threadCurrentThread.getThreadGroup();
        }
        threadGroup.addUnstarted();
        this.group = threadGroup;
        this.target = runnable;
        this.priority = threadCurrentThread.getPriority();
        this.daemon = threadCurrentThread.isDaemon();
        setName(str);
        init2(threadCurrentThread);
        this.stackSize = j;
        this.tid = nextThreadID();
    }

    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public Thread() {
        this.lock = new Object();
        this.started = false;
        this.daemon = false;
        this.stillborn = false;
        this.threadLocals = null;
        this.inheritableThreadLocals = null;
        this.threadStatus = 0;
        this.blockerLock = new Object();
        this.parkState = 1;
        init(null, null, "Thread-" + nextThreadNum(), 0L);
    }

    public Thread(Runnable runnable) {
        this.lock = new Object();
        this.started = false;
        this.daemon = false;
        this.stillborn = false;
        this.threadLocals = null;
        this.inheritableThreadLocals = null;
        this.threadStatus = 0;
        this.blockerLock = new Object();
        this.parkState = 1;
        init(null, runnable, "Thread-" + nextThreadNum(), 0L);
    }

    public Thread(ThreadGroup threadGroup, Runnable runnable) {
        this.lock = new Object();
        this.started = false;
        this.daemon = false;
        this.stillborn = false;
        this.threadLocals = null;
        this.inheritableThreadLocals = null;
        this.threadStatus = 0;
        this.blockerLock = new Object();
        this.parkState = 1;
        init(threadGroup, runnable, "Thread-" + nextThreadNum(), 0L);
    }

    public Thread(String str) {
        this.lock = new Object();
        this.started = false;
        this.daemon = false;
        this.stillborn = false;
        this.threadLocals = null;
        this.inheritableThreadLocals = null;
        this.threadStatus = 0;
        this.blockerLock = new Object();
        this.parkState = 1;
        init(null, null, str, 0L);
    }

    public Thread(ThreadGroup threadGroup, String str) {
        this.lock = new Object();
        this.started = false;
        this.daemon = false;
        this.stillborn = false;
        this.threadLocals = null;
        this.inheritableThreadLocals = null;
        this.threadStatus = 0;
        this.blockerLock = new Object();
        this.parkState = 1;
        init(threadGroup, null, str, 0L);
    }

    Thread(ThreadGroup threadGroup, String str, int i, boolean z) {
        this.lock = new Object();
        this.started = false;
        this.daemon = false;
        this.stillborn = false;
        this.threadLocals = null;
        this.inheritableThreadLocals = null;
        this.threadStatus = 0;
        this.blockerLock = new Object();
        this.parkState = 1;
        this.group = threadGroup;
        this.group.addUnstarted();
        if (str == null) {
            str = "Thread-" + nextThreadNum();
        }
        this.name = str;
        this.priority = i;
        this.daemon = z;
        init2(currentThread());
        this.tid = nextThreadID();
    }

    private void init2(Thread thread) {
        this.contextClassLoader = thread.getContextClassLoader();
        this.inheritedAccessControlContext = AccessController.getContext();
        if (thread.inheritableThreadLocals != null) {
            this.inheritableThreadLocals = ThreadLocal.createInheritedMap(thread.inheritableThreadLocals);
        }
    }

    public Thread(Runnable runnable, String str) {
        this.lock = new Object();
        this.started = false;
        this.daemon = false;
        this.stillborn = false;
        this.threadLocals = null;
        this.inheritableThreadLocals = null;
        this.threadStatus = 0;
        this.blockerLock = new Object();
        this.parkState = 1;
        init(null, runnable, str, 0L);
    }

    public Thread(ThreadGroup threadGroup, Runnable runnable, String str) {
        this.lock = new Object();
        this.started = false;
        this.daemon = false;
        this.stillborn = false;
        this.threadLocals = null;
        this.inheritableThreadLocals = null;
        this.threadStatus = 0;
        this.blockerLock = new Object();
        this.parkState = 1;
        init(threadGroup, runnable, str, 0L);
    }

    public Thread(ThreadGroup threadGroup, Runnable runnable, String str, long j) {
        this.lock = new Object();
        this.started = false;
        this.daemon = false;
        this.stillborn = false;
        this.threadLocals = null;
        this.inheritableThreadLocals = null;
        this.threadStatus = 0;
        this.blockerLock = new Object();
        this.parkState = 1;
        init(threadGroup, runnable, str, j);
    }

    public synchronized void start() {
        if (this.threadStatus != 0 || this.started) {
            throw new IllegalThreadStateException();
        }
        this.group.add(this);
        this.started = false;
        try {
            nativeCreate(this, this.stackSize, this.daemon);
            this.started = true;
        } finally {
            try {
                if (!this.started) {
                    this.group.threadStartFailed(this);
                }
            } catch (Throwable th) {
            }
        }
    }

    @Override
    public void run() {
        if (this.target != null) {
            this.target.run();
        }
    }

    private void exit() {
        if (this.group != null) {
            this.group.threadTerminated(this);
            this.group = null;
        }
        this.target = null;
        this.threadLocals = null;
        this.inheritableThreadLocals = null;
        this.inheritedAccessControlContext = null;
        this.blocker = null;
        this.uncaughtExceptionHandler = null;
    }

    @Deprecated
    public final void stop() {
        stop(new ThreadDeath());
    }

    @Deprecated
    public final void stop(Throwable th) {
        throw new UnsupportedOperationException();
    }

    public void interrupt() {
        if (this != currentThread()) {
            checkAccess();
        }
        synchronized (this.blockerLock) {
            Interruptible interruptible = this.blocker;
            if (interruptible != null) {
                nativeInterrupt();
                interruptible.interrupt(this);
            } else {
                nativeInterrupt();
            }
        }
    }

    @Deprecated
    public void destroy() {
        throw new UnsupportedOperationException();
    }

    public final boolean isAlive() {
        return this.nativePeer != 0;
    }

    @Deprecated
    public final void suspend() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public final void resume() {
        throw new UnsupportedOperationException();
    }

    public final void setPriority(int i) {
        checkAccess();
        if (i > 10 || i < 1) {
            throw new IllegalArgumentException("Priority out of range: " + i);
        }
        ThreadGroup threadGroup = getThreadGroup();
        if (threadGroup != null) {
            if (i > threadGroup.getMaxPriority()) {
                i = threadGroup.getMaxPriority();
            }
            synchronized (this) {
                this.priority = i;
                if (isAlive()) {
                    nativeSetPriority(i);
                }
            }
        }
    }

    public final int getPriority() {
        return this.priority;
    }

    public final void setName(String str) {
        checkAccess();
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        synchronized (this) {
            this.name = str;
            if (isAlive()) {
                nativeSetName(str);
            }
        }
    }

    public final String getName() {
        return this.name;
    }

    public final ThreadGroup getThreadGroup() {
        if (getState() == State.TERMINATED) {
            return null;
        }
        return this.group;
    }

    public static int activeCount() {
        return currentThread().getThreadGroup().activeCount();
    }

    public static int enumerate(Thread[] threadArr) {
        return currentThread().getThreadGroup().enumerate(threadArr);
    }

    @Deprecated
    public int countStackFrames() {
        return getStackTrace().length;
    }

    public final void join(long j) throws InterruptedException {
        synchronized (this.lock) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (j < 0) {
                throw new IllegalArgumentException("timeout value is negative");
            }
            if (j == 0) {
                while (isAlive()) {
                    this.lock.wait(0L);
                }
            } else {
                long jCurrentTimeMillis2 = 0;
                while (isAlive()) {
                    long j2 = j - jCurrentTimeMillis2;
                    if (j2 <= 0) {
                        break;
                    }
                    this.lock.wait(j2);
                    jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
                }
            }
        }
    }

    public final void join(long j, int i) throws InterruptedException {
        synchronized (this.lock) {
            if (j < 0) {
                throw new IllegalArgumentException("timeout value is negative");
            }
            if (i < 0 || i > 999999) {
                throw new IllegalArgumentException("nanosecond timeout value out of range");
            }
            if (i >= 500000 || (i != 0 && j == 0)) {
                j++;
            }
            join(j);
        }
    }

    public final void join() throws InterruptedException {
        join(0L);
    }

    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    public final void setDaemon(boolean z) {
        checkAccess();
        if (isAlive()) {
            throw new IllegalThreadStateException();
        }
        this.daemon = z;
    }

    public final boolean isDaemon() {
        return this.daemon;
    }

    public final void checkAccess() {
    }

    public String toString() {
        ThreadGroup threadGroup = getThreadGroup();
        if (threadGroup != null) {
            return "Thread[" + getName() + "," + getPriority() + "," + threadGroup.getName() + "]";
        }
        return "Thread[" + getName() + "," + getPriority() + ",]";
    }

    @CallerSensitive
    public ClassLoader getContextClassLoader() {
        return this.contextClassLoader;
    }

    public void setContextClassLoader(ClassLoader classLoader) {
        this.contextClassLoader = classLoader;
    }

    public static boolean holdsLock(Object obj) {
        return currentThread().nativeHoldsLock(obj);
    }

    public StackTraceElement[] getStackTrace() {
        StackTraceElement[] threadStackTrace = VMStack.getThreadStackTrace(this);
        return threadStackTrace != null ? threadStackTrace : EmptyArray.STACK_TRACE_ELEMENT;
    }

    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        HashMap map = new HashMap();
        int iActiveCount = ThreadGroup.systemThreadGroup.activeCount();
        Thread[] threadArr = new Thread[iActiveCount + (iActiveCount / 2)];
        int iEnumerate = ThreadGroup.systemThreadGroup.enumerate(threadArr);
        for (int i = 0; i < iEnumerate; i++) {
            map.put(threadArr[i], threadArr[i].getStackTrace());
        }
        return map;
    }

    private static class Caches {
        static final ConcurrentMap<WeakClassKey, Boolean> subclassAudits = new ConcurrentHashMap();
        static final ReferenceQueue<Class<?>> subclassAuditsQueue = new ReferenceQueue<>();

        private Caches() {
        }
    }

    private static boolean isCCLOverridden(Class<?> cls) {
        if (cls == Thread.class) {
            return false;
        }
        processQueue(Caches.subclassAuditsQueue, Caches.subclassAudits);
        WeakClassKey weakClassKey = new WeakClassKey(cls, Caches.subclassAuditsQueue);
        Boolean boolValueOf = Caches.subclassAudits.get(weakClassKey);
        if (boolValueOf == null) {
            boolValueOf = Boolean.valueOf(auditSubclass(cls));
            Caches.subclassAudits.putIfAbsent(weakClassKey, boolValueOf);
        }
        return boolValueOf.booleanValue();
    }

    private static boolean auditSubclass(final Class<?> cls) {
        return ((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                for (Class superclass = cls; superclass != Thread.class; superclass = superclass.getSuperclass()) {
                    try {
                        superclass.getDeclaredMethod("getContextClassLoader", new Class[0]);
                        return Boolean.TRUE;
                    } catch (NoSuchMethodException e) {
                        try {
                            superclass.getDeclaredMethod("setContextClassLoader", ClassLoader.class);
                            return Boolean.TRUE;
                        } catch (NoSuchMethodException e2) {
                        }
                    }
                }
                return Boolean.FALSE;
            }
        })).booleanValue();
    }

    public long getId() {
        return this.tid;
    }

    public State getState() {
        return State.values()[nativeGetStatus(this.started)];
    }

    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        defaultUncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtExceptionHandler;
    }

    public static void setUncaughtExceptionPreHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        uncaughtExceptionPreHandler = uncaughtExceptionHandler;
    }

    public static UncaughtExceptionHandler getUncaughtExceptionPreHandler() {
        return uncaughtExceptionPreHandler;
    }

    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return this.uncaughtExceptionHandler != null ? this.uncaughtExceptionHandler : this.group;
    }

    public void setUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        checkAccess();
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    public final void dispatchUncaughtException(Throwable th) {
        UncaughtExceptionHandler uncaughtExceptionPreHandler2 = getUncaughtExceptionPreHandler();
        if (uncaughtExceptionPreHandler2 != null) {
            try {
                uncaughtExceptionPreHandler2.uncaughtException(this, th);
            } catch (Error | RuntimeException e) {
            }
        }
        getUncaughtExceptionHandler().uncaughtException(this, th);
    }

    static void processQueue(ReferenceQueue<Class<?>> referenceQueue, ConcurrentMap<? extends WeakReference<Class<?>>, ?> concurrentMap) {
        while (true) {
            Reference<? extends Class<?>> referencePoll = referenceQueue.poll();
            if (referencePoll != null) {
                concurrentMap.remove(referencePoll);
            } else {
                return;
            }
        }
    }

    static class WeakClassKey extends WeakReference<Class<?>> {
        private final int hash;

        WeakClassKey(Class<?> cls, ReferenceQueue<Class<?>> referenceQueue) {
            super(cls, referenceQueue);
            this.hash = System.identityHashCode(cls);
        }

        public int hashCode() {
            return this.hash;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof WeakClassKey)) {
                return false;
            }
            Class<?> cls = get();
            return cls != null && cls == ((WeakClassKey) obj).get();
        }
    }

    private static class ParkState {
        private static final int PARKED = 3;
        private static final int PREEMPTIVELY_UNPARKED = 2;
        private static final int UNPARKED = 1;

        private ParkState() {
        }
    }

    public final void unpark$() {
        synchronized (this.lock) {
            switch (this.parkState) {
                case 1:
                    this.parkState = 2;
                    break;
                case 2:
                    break;
                default:
                    this.parkState = 1;
                    this.lock.notifyAll();
                    break;
            }
        }
    }

    public final void parkFor$(long j) {
        synchronized (this.lock) {
            switch (this.parkState) {
                case 1:
                    long j2 = j / 1000000;
                    long j3 = j % 1000000;
                    this.parkState = 3;
                    try {
                        try {
                            this.lock.wait(j2, (int) j3);
                        } finally {
                            if (this.parkState == 3) {
                                this.parkState = 1;
                            }
                        }
                    } catch (InterruptedException e) {
                        interrupt();
                        if (this.parkState == 3) {
                        }
                    }
                    break;
                case 2:
                    this.parkState = 1;
                    break;
                default:
                    throw new AssertionError((Object) "Attempt to repark");
            }
        }
    }

    public final void parkUntil$(long j) {
        synchronized (this.lock) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            if (j <= jCurrentTimeMillis) {
                this.parkState = 1;
            } else {
                long j2 = j - jCurrentTimeMillis;
                if (j2 > 9223372036854L) {
                    j2 = 9223372036854L;
                }
                parkFor$(j2 * 1000000);
            }
        }
    }
}
