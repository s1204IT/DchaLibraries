package sun.nio.fs;

import java.util.concurrent.ExecutionException;
import sun.misc.Unsafe;

abstract class Cancellable implements Runnable {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private boolean completed;
    private Throwable exception;
    private final Object lock = new Object();
    private final long pollingAddress = unsafe.allocateMemory(4);

    abstract void implRun() throws Throwable;

    protected Cancellable() {
        unsafe.putIntVolatile(null, this.pollingAddress, 0);
    }

    protected long addressToPollForCancel() {
        return this.pollingAddress;
    }

    protected int cancelValue() {
        return Integer.MAX_VALUE;
    }

    final void cancel() {
        synchronized (this.lock) {
            if (!this.completed) {
                unsafe.putIntVolatile(null, this.pollingAddress, cancelValue());
            }
        }
    }

    private Throwable exception() {
        Throwable th;
        synchronized (this.lock) {
            th = this.exception;
        }
        return th;
    }

    @Override
    public final void run() {
        try {
            try {
                implRun();
                synchronized (this.lock) {
                    this.completed = true;
                    unsafe.freeMemory(this.pollingAddress);
                }
            } catch (Throwable th) {
                synchronized (this.lock) {
                    this.exception = th;
                    synchronized (this.lock) {
                        this.completed = true;
                        unsafe.freeMemory(this.pollingAddress);
                    }
                }
            }
        } catch (Throwable th2) {
            synchronized (this.lock) {
                this.completed = true;
                unsafe.freeMemory(this.pollingAddress);
                throw th2;
            }
        }
    }

    static void runInterruptibly(Cancellable cancellable) throws ExecutionException {
        Thread thread = new Thread(cancellable);
        thread.start();
        boolean z = false;
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                z = true;
                cancellable.cancel();
            }
        }
        if (z) {
            Thread.currentThread().interrupt();
        }
        Throwable thException = cancellable.exception();
        if (thException != null) {
            throw new ExecutionException(thException);
        }
    }
}
