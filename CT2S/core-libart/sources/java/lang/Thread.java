package java.lang;

import dalvik.system.VMStack;
import java.lang.ThreadLocal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import libcore.util.EmptyArray;

public class Thread implements Runnable {
    public static final int MAX_PRIORITY = 10;
    public static final int MIN_PRIORITY = 1;
    private static final int NANOS_PER_MILLI = 1000000;
    public static final int NORM_PRIORITY = 5;
    private static int count = 0;
    private static UncaughtExceptionHandler defaultUncaughtHandler;
    private ClassLoader contextClassLoader;
    volatile boolean daemon;
    volatile ThreadGroup group;
    private long id;
    ThreadLocal.Values inheritableValues;
    ThreadLocal.Values localValues;
    volatile String name;
    private volatile long nativePeer;
    private Object parkBlocker;
    volatile int priority;
    volatile long stackSize;
    Runnable target;
    private UncaughtExceptionHandler uncaughtHandler;
    private final List<Runnable> interruptActions = new ArrayList();
    boolean hasBeenStarted = false;
    private int parkState = 1;
    private final Object lock = new Object();

    public enum State {
        NEW,
        RUNNABLE,
        BLOCKED,
        WAITING,
        TIMED_WAITING,
        TERMINATED
    }

    public interface UncaughtExceptionHandler {
        void uncaughtException(Thread thread, Throwable th);
    }

    public static native Thread currentThread();

    public static native boolean interrupted();

    private static native void nativeCreate(Thread thread, long j, boolean z);

    private native int nativeGetStatus(boolean z);

    private native boolean nativeHoldsLock(Object obj);

    private native void nativeInterrupt();

    private native void nativeSetName(String str);

    private native void nativeSetPriority(int i);

    private static native void sleep(Object obj, long j, int i);

    public static native void yield();

    public native boolean isInterrupted();

    private static class ParkState {
        private static final int PARKED = 3;
        private static final int PREEMPTIVELY_UNPARKED = 2;
        private static final int UNPARKED = 1;

        private ParkState() {
        }
    }

    public Thread() {
        create(null, null, null, 0L);
    }

    public Thread(Runnable runnable) {
        create(null, runnable, null, 0L);
    }

    public Thread(Runnable runnable, String threadName) {
        if (threadName == null) {
            throw new NullPointerException("threadName == null");
        }
        create(null, runnable, threadName, 0L);
    }

    public Thread(String threadName) {
        if (threadName == null) {
            throw new NullPointerException("threadName == null");
        }
        create(null, null, threadName, 0L);
    }

    public Thread(ThreadGroup group, Runnable runnable) {
        create(group, runnable, null, 0L);
    }

    public Thread(ThreadGroup group, Runnable runnable, String threadName) {
        if (threadName == null) {
            throw new NullPointerException("threadName == null");
        }
        create(group, runnable, threadName, 0L);
    }

    public Thread(ThreadGroup group, String threadName) {
        if (threadName == null) {
            throw new NullPointerException("threadName == null");
        }
        create(group, null, threadName, 0L);
    }

    public Thread(ThreadGroup group, Runnable runnable, String threadName, long stackSize) {
        if (threadName == null) {
            throw new NullPointerException("threadName == null");
        }
        create(group, runnable, threadName, stackSize);
    }

    Thread(ThreadGroup group, String name, int priority, boolean daemon) {
        synchronized (Thread.class) {
            int i = count + 1;
            count = i;
            this.id = i;
        }
        if (name == null) {
            this.name = "Thread-" + this.id;
        } else {
            this.name = name;
        }
        if (group == null) {
            throw new InternalError("group == null");
        }
        this.group = group;
        this.target = null;
        this.stackSize = 0L;
        this.priority = priority;
        this.daemon = daemon;
        this.group.addThread(this);
    }

    private void create(ThreadGroup group, Runnable runnable, String threadName, long stackSize) {
        Thread currentThread = currentThread();
        if (group == null) {
            group = currentThread.getThreadGroup();
        }
        if (group.isDestroyed()) {
            throw new IllegalThreadStateException("Group already destroyed");
        }
        this.group = group;
        synchronized (Thread.class) {
            int i = count + 1;
            count = i;
            this.id = i;
        }
        if (threadName == null) {
            this.name = "Thread-" + this.id;
        } else {
            this.name = threadName;
        }
        this.target = runnable;
        this.stackSize = stackSize;
        this.priority = currentThread.getPriority();
        this.contextClassLoader = currentThread.contextClassLoader;
        if (currentThread.inheritableValues != null) {
            this.inheritableValues = new ThreadLocal.Values(currentThread.inheritableValues);
        }
        this.group.addThread(this);
    }

    public static int activeCount() {
        return currentThread().getThreadGroup().activeCount();
    }

    public final void checkAccess() {
    }

    @Deprecated
    public int countStackFrames() {
        return getStackTrace().length;
    }

    @Deprecated
    public void destroy() {
        throw new UnsupportedOperationException();
    }

    public static void dumpStack() {
        new Throwable("stack dump").printStackTrace();
    }

    public static int enumerate(Thread[] threads) {
        Thread thread = currentThread();
        return thread.getThreadGroup().enumerate(threads);
    }

    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        Map<Thread, StackTraceElement[]> map = new HashMap<>();
        int count2 = ThreadGroup.systemThreadGroup.activeCount();
        Thread[] threads = new Thread[(count2 / 2) + count2];
        int count3 = ThreadGroup.systemThreadGroup.enumerate(threads);
        for (int i = 0; i < count3; i++) {
            map.put(threads[i], threads[i].getStackTrace());
        }
        return map;
    }

    public ClassLoader getContextClassLoader() {
        return this.contextClassLoader;
    }

    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtHandler;
    }

    public long getId() {
        return this.id;
    }

    public final String getName() {
        return this.name;
    }

    public final int getPriority() {
        return this.priority;
    }

    public StackTraceElement[] getStackTrace() {
        StackTraceElement[] ste = VMStack.getThreadStackTrace(this);
        return ste != null ? ste : EmptyArray.STACK_TRACE_ELEMENT;
    }

    public State getState() {
        return State.values()[nativeGetStatus(this.hasBeenStarted)];
    }

    public final ThreadGroup getThreadGroup() {
        if (getState() == State.TERMINATED) {
            return null;
        }
        return this.group;
    }

    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return this.uncaughtHandler != null ? this.uncaughtHandler : this.group;
    }

    public void interrupt() {
        nativeInterrupt();
        synchronized (this.interruptActions) {
            for (int i = this.interruptActions.size() - 1; i >= 0; i--) {
                this.interruptActions.get(i).run();
            }
        }
    }

    public final boolean isAlive() {
        return this.nativePeer != 0;
    }

    public final boolean isDaemon() {
        return this.daemon;
    }

    public final void join() throws InterruptedException {
        synchronized (this.lock) {
            while (isAlive()) {
                this.lock.wait();
            }
        }
    }

    public final void join(long millis) throws InterruptedException {
        join(millis, 0);
    }

    public final void join(long millis, int nanos) throws InterruptedException {
        if (millis < 0 || nanos < 0 || nanos >= NANOS_PER_MILLI) {
            throw new IllegalArgumentException("bad timeout: millis=" + millis + ",nanos=" + nanos);
        }
        boolean overflow = millis >= (Long.MAX_VALUE - ((long) nanos)) / 1000000;
        boolean forever = (((long) nanos) | millis) == 0;
        if (forever | overflow) {
            join();
            return;
        }
        synchronized (this.lock) {
            if (isAlive()) {
                long nanosToWait = (1000000 * millis) + ((long) nanos);
                long start = System.nanoTime();
                while (true) {
                    this.lock.wait(millis, nanos);
                    if (!isAlive()) {
                        break;
                    }
                    long nanosElapsed = System.nanoTime() - start;
                    long nanosRemaining = nanosToWait - nanosElapsed;
                    if (nanosRemaining <= 0) {
                        break;
                    }
                    millis = nanosRemaining / 1000000;
                    nanos = (int) (nanosRemaining - (1000000 * millis));
                }
            }
        }
    }

    @Deprecated
    public final void resume() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void run() {
        if (this.target != null) {
            this.target.run();
        }
    }

    public void setContextClassLoader(ClassLoader cl) {
        this.contextClassLoader = cl;
    }

    public final void setDaemon(boolean isDaemon) {
        checkNotStarted();
        if (this.nativePeer == 0) {
            this.daemon = isDaemon;
        }
    }

    private void checkNotStarted() {
        if (this.hasBeenStarted) {
            throw new IllegalThreadStateException("Thread already started");
        }
    }

    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        defaultUncaughtHandler = handler;
    }

    public final void pushInterruptAction$(Runnable interruptAction) {
        synchronized (this.interruptActions) {
            this.interruptActions.add(interruptAction);
        }
        if (interruptAction != null && isInterrupted()) {
            interruptAction.run();
        }
    }

    public final void popInterruptAction$(Runnable interruptAction) {
        synchronized (this.interruptActions) {
            Runnable removed = this.interruptActions.remove(this.interruptActions.size() - 1);
            if (interruptAction != removed) {
                throw new IllegalArgumentException("Expected " + interruptAction + " but was " + removed);
            }
        }
    }

    public final void setName(String threadName) {
        if (threadName == null) {
            throw new NullPointerException("threadName == null");
        }
        synchronized (this) {
            this.name = threadName;
            if (isAlive()) {
                nativeSetName(threadName);
            }
        }
    }

    public final void setPriority(int priority) {
        if (priority < 1 || priority > 10) {
            throw new IllegalArgumentException("Priority out of range: " + priority);
        }
        if (priority > this.group.getMaxPriority()) {
            priority = this.group.getMaxPriority();
        }
        synchronized (this) {
            this.priority = priority;
            if (isAlive()) {
                nativeSetPriority(priority);
            }
        }
    }

    public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        this.uncaughtHandler = handler;
    }

    public static void sleep(long time) throws InterruptedException {
        sleep(time, 0);
    }

    public static void sleep(long millis, int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("millis < 0: " + millis);
        }
        if (nanos < 0) {
            throw new IllegalArgumentException("nanos < 0: " + nanos);
        }
        if (nanos > 999999) {
            throw new IllegalArgumentException("nanos > 999999: " + nanos);
        }
        if (millis == 0 && nanos == 0) {
            if (interrupted()) {
                throw new InterruptedException();
            }
            return;
        }
        long start = System.nanoTime();
        long duration = (1000000 * millis) + ((long) nanos);
        Object lock = currentThread().lock;
        synchronized (lock) {
            while (true) {
                sleep(lock, millis, nanos);
                long now = System.nanoTime();
                long elapsed = now - start;
                if (elapsed < duration) {
                    duration -= elapsed;
                    start = now;
                    millis = duration / 1000000;
                    nanos = (int) (duration % 1000000);
                }
            }
        }
    }

    public synchronized void start() {
        checkNotStarted();
        this.hasBeenStarted = true;
        nativeCreate(this, this.stackSize, this.daemon);
    }

    @Deprecated
    public final void stop() {
        stop(new ThreadDeath());
    }

    @Deprecated
    public final synchronized void stop(Throwable throwable) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public final void suspend() {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return "Thread[" + this.name + "," + this.priority + "," + this.group.getName() + "]";
    }

    public static boolean holdsLock(Object object) {
        return currentThread().nativeHoldsLock(object);
    }

    public void unpark() {
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

    public void parkFor(long nanos) {
        synchronized (this.lock) {
            switch (this.parkState) {
                case 1:
                    long millis = nanos / 1000000;
                    long nanos2 = nanos % 1000000;
                    this.parkState = 3;
                    try {
                        try {
                            this.lock.wait(millis, (int) nanos2);
                            if (this.parkState == 3) {
                                this.parkState = 1;
                            }
                        } catch (InterruptedException e) {
                            interrupt();
                            if (this.parkState == 3) {
                                this.parkState = 1;
                            }
                        }
                    } catch (Throwable th) {
                        if (this.parkState == 3) {
                            this.parkState = 1;
                        }
                        throw th;
                    }
                    break;
                case 2:
                    this.parkState = 1;
                    break;
                default:
                    throw new AssertionError("Attempt to repark");
            }
        }
    }

    public void parkUntil(long time) {
        synchronized (this.lock) {
            long delayMillis = time - System.currentTimeMillis();
            if (delayMillis <= 0) {
                this.parkState = 1;
            } else {
                parkFor(1000000 * delayMillis);
            }
        }
    }
}
