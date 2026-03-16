package java.util.concurrent;

import java.util.concurrent.ForkJoinPool;

public class ForkJoinWorkerThread extends Thread {
    final ForkJoinPool pool;
    final ForkJoinPool.WorkQueue workQueue;

    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        super("aForkJoinWorkerThread");
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    public ForkJoinPool getPool() {
        return this.pool;
    }

    public int getPoolIndex() {
        return this.workQueue.poolIndex >>> 1;
    }

    protected void onStart() {
    }

    protected void onTermination(Throwable exception) {
    }

    @Override
    public void run() {
        try {
            onStart();
            this.pool.runWorker(this.workQueue);
            try {
                onTermination(null);
                this.pool.deregisterWorker(this, null);
            } catch (Throwable th) {
                this.pool.deregisterWorker(this, null);
                throw th;
            }
        } catch (Throwable ex) {
            Throwable exception = ex;
            try {
                onTermination(exception);
                this.pool.deregisterWorker(this, exception);
            } catch (Throwable th2) {
                this.pool.deregisterWorker(this, exception);
                throw th2;
            }
        }
    }
}
