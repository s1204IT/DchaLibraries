package java.util.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CyclicBarrier {
    private final Runnable barrierCommand;
    private int count;
    private Generation generation;
    private final ReentrantLock lock;
    private final int parties;
    private final Condition trip;

    private static class Generation {
        boolean broken;

        private Generation() {
        }
    }

    private void nextGeneration() {
        this.trip.signalAll();
        this.count = this.parties;
        this.generation = new Generation();
    }

    private void breakBarrier() {
        this.generation.broken = true;
        this.count = this.parties;
        this.trip.signalAll();
    }

    private int dowait(boolean z, long j) throws InterruptedException, TimeoutException, BrokenBarrierException {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            Generation generation = this.generation;
            if (generation.broken) {
                throw new BrokenBarrierException();
            }
            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }
            int i = this.count - 1;
            this.count = i;
            if (i == 0) {
                boolean z2 = false;
                try {
                    Runnable runnable = this.barrierCommand;
                    if (runnable != null) {
                        runnable.run();
                    }
                    try {
                        nextGeneration();
                        return 0;
                    } catch (Throwable th) {
                        th = th;
                        z2 = true;
                        if (!z2) {
                            breakBarrier();
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } else {
                while (true) {
                    if (!z) {
                        try {
                            this.trip.await();
                        } catch (InterruptedException e) {
                            if (generation == this.generation && !generation.broken) {
                                breakBarrier();
                                throw e;
                            }
                            Thread.currentThread().interrupt();
                        }
                    } else if (j > 0) {
                        j = this.trip.awaitNanos(j);
                    }
                    if (generation.broken) {
                        throw new BrokenBarrierException();
                    }
                    if (generation != this.generation) {
                        return i;
                    }
                    if (z && j <= 0) {
                        breakBarrier();
                        throw new TimeoutException();
                    }
                }
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    public CyclicBarrier(int i, Runnable runnable) {
        this.lock = new ReentrantLock();
        this.trip = this.lock.newCondition();
        this.generation = new Generation();
        if (i <= 0) {
            throw new IllegalArgumentException();
        }
        this.parties = i;
        this.count = i;
        this.barrierCommand = runnable;
    }

    public CyclicBarrier(int i) {
        this(i, null);
    }

    public int getParties() {
        return this.parties;
    }

    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException e) {
            throw new Error(e);
        }
    }

    public int await(long j, TimeUnit timeUnit) throws InterruptedException, TimeoutException, BrokenBarrierException {
        return dowait(true, timeUnit.toNanos(j));
    }

    public boolean isBroken() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.generation.broken;
        } finally {
            reentrantLock.unlock();
        }
    }

    public void reset() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            breakBarrier();
            nextGeneration();
        } finally {
            reentrantLock.unlock();
        }
    }

    public int getNumberWaiting() {
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            return this.parties - this.count;
        } finally {
            reentrantLock.unlock();
        }
    }
}
