package java.lang;

import dalvik.system.VMRuntime;
import java.lang.Thread;
import java.lang.ref.FinalizerReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import libcore.util.EmptyArray;

public final class Daemons {
    private static final long MAX_FINALIZE_NANOS = 10000000000L;
    private static final int NANOS_PER_MILLI = 1000000;
    private static final int NANOS_PER_SECOND = 1000000000;

    public static void start() {
        ReferenceQueueDaemon.INSTANCE.start();
        FinalizerDaemon.INSTANCE.start();
        FinalizerWatchdogDaemon.INSTANCE.start();
        HeapTrimmerDaemon.INSTANCE.start();
        GCDaemon.INSTANCE.start();
    }

    public static void stop() {
        ReferenceQueueDaemon.INSTANCE.stop();
        FinalizerDaemon.INSTANCE.stop();
        FinalizerWatchdogDaemon.INSTANCE.stop();
        HeapTrimmerDaemon.INSTANCE.stop();
        GCDaemon.INSTANCE.stop();
    }

    private static abstract class Daemon implements Runnable {
        private Thread thread;

        @Override
        public abstract void run();

        private Daemon() {
        }

        public synchronized void start() {
            if (this.thread != null) {
                throw new IllegalStateException("already running");
            }
            this.thread = new Thread(ThreadGroup.systemThreadGroup, this, getClass().getSimpleName());
            this.thread.setDaemon(true);
            this.thread.start();
        }

        protected synchronized boolean isRunning() {
            return this.thread != null;
        }

        public synchronized void interrupt() {
            if (this.thread == null) {
                throw new IllegalStateException("not running");
            }
            this.thread.interrupt();
        }

        public void stop() {
            Thread threadToStop;
            synchronized (this) {
                threadToStop = this.thread;
                this.thread = null;
            }
            if (threadToStop == null) {
                throw new IllegalStateException("not running");
            }
            threadToStop.interrupt();
            while (true) {
                try {
                    threadToStop.join();
                    return;
                } catch (InterruptedException e) {
                }
            }
        }

        public synchronized StackTraceElement[] getStackTrace() {
            return this.thread != null ? this.thread.getStackTrace() : EmptyArray.STACK_TRACE_ELEMENT;
        }
    }

    private static class ReferenceQueueDaemon extends Daemon {
        private static final ReferenceQueueDaemon INSTANCE = new ReferenceQueueDaemon();

        private ReferenceQueueDaemon() {
            super();
        }

        @Override
        public void run() {
            Reference<?> list;
            while (isRunning()) {
                try {
                    synchronized (ReferenceQueue.class) {
                        while (ReferenceQueue.unenqueued == null) {
                            ReferenceQueue.class.wait();
                        }
                        list = ReferenceQueue.unenqueued;
                        ReferenceQueue.unenqueued = null;
                    }
                    enqueue(list);
                } catch (InterruptedException e) {
                }
            }
        }

        private void enqueue(Reference<?> list) {
            Reference<?> reference;
            while (list != null) {
                if (list == list.pendingNext) {
                    reference = list;
                    reference.pendingNext = null;
                    list = null;
                } else {
                    reference = list.pendingNext;
                    list.pendingNext = reference.pendingNext;
                    reference.pendingNext = null;
                }
                reference.enqueueInternal();
            }
        }
    }

    private static class FinalizerDaemon extends Daemon {
        private static final FinalizerDaemon INSTANCE = new FinalizerDaemon();
        private volatile Object finalizingObject;
        private volatile long finalizingStartedNanos;
        private final ReferenceQueue<Object> queue;

        private FinalizerDaemon() {
            super();
            this.queue = FinalizerReference.queue;
        }

        @Override
        public void run() {
            while (isRunning()) {
                try {
                    doFinalize((FinalizerReference) this.queue.remove());
                } catch (InterruptedException e) {
                }
            }
        }

        @FindBugsSuppressWarnings({"FI_EXPLICIT_INVOCATION"})
        private void doFinalize(FinalizerReference<?> reference) {
            FinalizerReference.remove(reference);
            Object object = reference.get();
            reference.clear();
            try {
                this.finalizingStartedNanos = System.nanoTime();
                this.finalizingObject = object;
                synchronized (FinalizerWatchdogDaemon.INSTANCE) {
                    FinalizerWatchdogDaemon.INSTANCE.notify();
                }
                object.finalize();
            } catch (Throwable ex) {
                System.logE("Uncaught exception thrown by finalizer", ex);
            } finally {
                this.finalizingObject = null;
            }
        }
    }

    private static class FinalizerWatchdogDaemon extends Daemon {
        private static final FinalizerWatchdogDaemon INSTANCE = new FinalizerWatchdogDaemon();

        private FinalizerWatchdogDaemon() {
            super();
        }

        @Override
        public void run() {
            Object finalizedObject;
            while (isRunning()) {
                boolean waitSuccessful = waitForObject();
                if (waitSuccessful) {
                    boolean finalized = waitForFinalization();
                    if (!finalized && !VMRuntime.getRuntime().isDebuggerActive() && (finalizedObject = FinalizerDaemon.INSTANCE.finalizingObject) != null) {
                        finalizerTimedOut(finalizedObject);
                        return;
                    }
                }
            }
        }

        private boolean waitForObject() {
            while (object == null) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        return false;
                    }
                }
            }
            return true;
        }

        private void sleepFor(long startNanos, long durationNanos) {
            while (true) {
                long elapsedNanos = System.nanoTime() - startNanos;
                long sleepNanos = durationNanos - elapsedNanos;
                long sleepMills = sleepNanos / 1000000;
                if (sleepMills > 0) {
                    try {
                        Thread.sleep(sleepMills);
                    } catch (InterruptedException e) {
                        if (!isRunning()) {
                            return;
                        }
                    }
                } else {
                    return;
                }
            }
        }

        private boolean waitForFinalization() {
            long startTime = FinalizerDaemon.INSTANCE.finalizingStartedNanos;
            sleepFor(startTime, Daemons.MAX_FINALIZE_NANOS);
            return FinalizerDaemon.INSTANCE.finalizingObject == null || FinalizerDaemon.INSTANCE.finalizingStartedNanos != startTime;
        }

        private static void finalizerTimedOut(Object object) {
            String message = object.getClass().getName() + ".finalize() timed out after 10 seconds";
            Exception syntheticException = new TimeoutException(message);
            syntheticException.setStackTrace(FinalizerDaemon.INSTANCE.getStackTrace());
            Thread.UncaughtExceptionHandler h = Thread.getDefaultUncaughtExceptionHandler();
            if (h == null) {
                System.logE(message, syntheticException);
                System.exit(2);
            }
            h.uncaughtException(Thread.currentThread(), syntheticException);
        }
    }

    public static void requestHeapTrim() {
        synchronized (HeapTrimmerDaemon.INSTANCE) {
            HeapTrimmerDaemon.INSTANCE.notify();
        }
    }

    private static class HeapTrimmerDaemon extends Daemon {
        private static final HeapTrimmerDaemon INSTANCE = new HeapTrimmerDaemon();

        private HeapTrimmerDaemon() {
            super();
        }

        @Override
        public void run() {
            while (isRunning()) {
                try {
                    synchronized (this) {
                        wait();
                    }
                    VMRuntime.getRuntime().trimHeap();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public static void requestGC() {
        GCDaemon.INSTANCE.requestGC();
    }

    private static class GCDaemon extends Daemon {
        private static final GCDaemon INSTANCE = new GCDaemon();
        private static final AtomicBoolean atomicBoolean = new AtomicBoolean();

        private GCDaemon() {
            super();
        }

        public void requestGC() {
            if (!atomicBoolean.getAndSet(true)) {
                synchronized (this) {
                    notify();
                }
                atomicBoolean.set(false);
            }
        }

        @Override
        public void run() {
            while (isRunning()) {
                try {
                    synchronized (this) {
                        wait();
                    }
                    VMRuntime.getRuntime().concurrentGC();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
