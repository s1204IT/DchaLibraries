package java.lang;

import android.system.Os;
import android.system.OsConstants;
import dalvik.system.VMRuntime;
import java.lang.ref.FinalizerReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.util.EmptyArray;

public final class Daemons {
    private static final long MAX_FINALIZE_NANOS = 10000000000L;
    private static final int NANOS_PER_MILLI = 1000000;
    private static final int NANOS_PER_SECOND = 1000000000;

    public static void start() {
        ReferenceQueueDaemon.INSTANCE.start();
        FinalizerDaemon.INSTANCE.start();
        FinalizerWatchdogDaemon.INSTANCE.start();
        HeapTaskDaemon.INSTANCE.start();
    }

    public static void startPostZygoteFork() {
        ReferenceQueueDaemon.INSTANCE.startPostZygoteFork();
        FinalizerDaemon.INSTANCE.startPostZygoteFork();
        FinalizerWatchdogDaemon.INSTANCE.startPostZygoteFork();
        HeapTaskDaemon.INSTANCE.startPostZygoteFork();
    }

    public static void stop() {
        HeapTaskDaemon.INSTANCE.stop();
        ReferenceQueueDaemon.INSTANCE.stop();
        FinalizerDaemon.INSTANCE.stop();
        FinalizerWatchdogDaemon.INSTANCE.stop();
    }

    private static abstract class Daemon implements Runnable {
        private String name;
        private boolean postZygoteFork;
        private Thread thread;

        public abstract void runInternal();

        protected Daemon(String str) {
            this.name = str;
        }

        public synchronized void start() {
            startInternal();
        }

        public synchronized void startPostZygoteFork() {
            this.postZygoteFork = true;
            startInternal();
        }

        public void startInternal() {
            if (this.thread != null) {
                throw new IllegalStateException("already running");
            }
            this.thread = new Thread(ThreadGroup.systemThreadGroup, this, this.name);
            this.thread.setDaemon(true);
            this.thread.start();
        }

        @Override
        public void run() {
            if (this.postZygoteFork) {
                VMRuntime.getRuntime();
                VMRuntime.setSystemDaemonThreadPriority();
            }
            runInternal();
        }

        protected synchronized boolean isRunning() {
            return this.thread != null;
        }

        public synchronized void interrupt() {
            interrupt(this.thread);
        }

        public synchronized void interrupt(Thread thread) {
            if (thread == null) {
                throw new IllegalStateException("not running");
            }
            thread.interrupt();
        }

        public void stop() {
            Thread thread;
            synchronized (this) {
                thread = this.thread;
                this.thread = null;
            }
            if (thread == null) {
                throw new IllegalStateException("not running");
            }
            interrupt(thread);
            while (true) {
                try {
                    thread.join();
                    return;
                } catch (InterruptedException e) {
                } catch (OutOfMemoryError e2) {
                }
            }
        }

        public synchronized StackTraceElement[] getStackTrace() {
            return this.thread != null ? this.thread.getStackTrace() : EmptyArray.STACK_TRACE_ELEMENT;
        }
    }

    private static class ReferenceQueueDaemon extends Daemon {
        private static final ReferenceQueueDaemon INSTANCE = new ReferenceQueueDaemon();

        ReferenceQueueDaemon() {
            super("ReferenceQueueDaemon");
        }

        @Override
        public void runInternal() {
            Reference reference;
            while (isRunning()) {
                try {
                    synchronized (ReferenceQueue.class) {
                        while (ReferenceQueue.unenqueued == null) {
                            ReferenceQueue.class.wait();
                        }
                        reference = ReferenceQueue.unenqueued;
                        ReferenceQueue.unenqueued = null;
                    }
                    ReferenceQueue.enqueuePending(reference);
                } catch (InterruptedException e) {
                } catch (OutOfMemoryError e2) {
                }
            }
        }
    }

    private static class FinalizerDaemon extends Daemon {
        private static final FinalizerDaemon INSTANCE = new FinalizerDaemon();
        private Object finalizingObject;
        private final AtomicInteger progressCounter;
        private final ReferenceQueue<Object> queue;

        FinalizerDaemon() {
            super("FinalizerDaemon");
            this.queue = FinalizerReference.queue;
            this.progressCounter = new AtomicInteger(0);
            this.finalizingObject = null;
        }

        @Override
        public void runInternal() {
            int i = this.progressCounter.get();
            while (isRunning()) {
                try {
                    FinalizerReference<?> finalizerReference = (FinalizerReference) this.queue.poll();
                    if (finalizerReference != null) {
                        this.finalizingObject = finalizerReference.get();
                        i++;
                        this.progressCounter.lazySet(i);
                    } else {
                        this.finalizingObject = null;
                        int i2 = i + 1;
                        this.progressCounter.lazySet(i2);
                        FinalizerWatchdogDaemon.INSTANCE.goToSleep();
                        finalizerReference = (FinalizerReference) this.queue.remove();
                        this.finalizingObject = finalizerReference.get();
                        i = i2 + 1;
                        this.progressCounter.set(i);
                        FinalizerWatchdogDaemon.INSTANCE.wakeUp();
                    }
                    doFinalize(finalizerReference);
                } catch (InterruptedException e) {
                } catch (OutOfMemoryError e2) {
                }
            }
        }

        @FindBugsSuppressWarnings({"FI_EXPLICIT_INVOCATION"})
        private void doFinalize(FinalizerReference<?> finalizerReference) {
            FinalizerReference.remove(finalizerReference);
            Object obj = finalizerReference.get();
            finalizerReference.clear();
            try {
                try {
                    obj.finalize();
                } catch (Throwable th) {
                    System.logE("Uncaught exception thrown by finalizer", th);
                }
            } finally {
                this.finalizingObject = null;
            }
        }
    }

    private static class FinalizerWatchdogDaemon extends Daemon {
        private static final FinalizerWatchdogDaemon INSTANCE = new FinalizerWatchdogDaemon();
        private boolean needToWork;

        FinalizerWatchdogDaemon() {
            super("FinalizerWatchdogDaemon");
            this.needToWork = true;
        }

        @Override
        public void runInternal() {
            Object objWaitForFinalization;
            while (isRunning()) {
                if (sleepUntilNeeded() && (objWaitForFinalization = waitForFinalization()) != null && !VMRuntime.getRuntime().isDebuggerActive()) {
                    finalizerTimedOut(objWaitForFinalization);
                    return;
                }
            }
        }

        private synchronized boolean sleepUntilNeeded() {
            while (!this.needToWork) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return false;
                } catch (OutOfMemoryError e2) {
                    return false;
                }
            }
            return true;
        }

        private synchronized void goToSleep() {
            this.needToWork = false;
        }

        private synchronized void wakeUp() {
            this.needToWork = true;
            notify();
        }

        private synchronized boolean getNeedToWork() {
            return this.needToWork;
        }

        private boolean sleepFor(long j) {
            long jNanoTime = System.nanoTime();
            while (true) {
                long jNanoTime2 = (j - (System.nanoTime() - jNanoTime)) / 1000000;
                if (jNanoTime2 <= 0) {
                    return true;
                }
                try {
                    Thread.sleep(jNanoTime2);
                } catch (InterruptedException e) {
                    if (!isRunning()) {
                        return false;
                    }
                } catch (OutOfMemoryError e2) {
                    if (!isRunning()) {
                        return false;
                    }
                }
            }
        }

        private Object waitForFinalization() {
            long j = FinalizerDaemon.INSTANCE.progressCounter.get();
            if (sleepFor(Daemons.MAX_FINALIZE_NANOS) && getNeedToWork() && FinalizerDaemon.INSTANCE.progressCounter.get() == j) {
                Object obj = FinalizerDaemon.INSTANCE.finalizingObject;
                sleepFor(500000000L);
                if (getNeedToWork() && FinalizerDaemon.INSTANCE.progressCounter.get() == j) {
                    return obj;
                }
            }
            return null;
        }

        private static void finalizerTimedOut(Object obj) {
            String str = obj.getClass().getName() + ".finalize() timed out after 10 seconds";
            TimeoutException timeoutException = new TimeoutException(str);
            timeoutException.setStackTrace(FinalizerDaemon.INSTANCE.getStackTrace());
            try {
                Os.kill(Os.getpid(), OsConstants.SIGQUIT);
                Thread.sleep(5000L);
            } catch (Exception e) {
                System.logE("failed to send SIGQUIT", e);
            } catch (OutOfMemoryError e2) {
            }
            if (Thread.getUncaughtExceptionPreHandler() == null && Thread.getDefaultUncaughtExceptionHandler() == null) {
                System.logE(str, timeoutException);
                System.exit(2);
            }
            Thread.currentThread().dispatchUncaughtException(timeoutException);
        }
    }

    public static void requestHeapTrim() {
        VMRuntime.getRuntime().requestHeapTrim();
    }

    public static void requestGC() {
        VMRuntime.getRuntime().requestConcurrentGC();
    }

    private static class HeapTaskDaemon extends Daemon {
        private static final HeapTaskDaemon INSTANCE = new HeapTaskDaemon();

        HeapTaskDaemon() {
            super("HeapTaskDaemon");
        }

        @Override
        public synchronized void interrupt(Thread thread) {
            VMRuntime.getRuntime().stopHeapTaskProcessor();
        }

        @Override
        public void runInternal() {
            synchronized (this) {
                if (isRunning()) {
                    VMRuntime.getRuntime().startHeapTaskProcessor();
                }
            }
            VMRuntime.getRuntime().runHeapTasks();
        }
    }
}
