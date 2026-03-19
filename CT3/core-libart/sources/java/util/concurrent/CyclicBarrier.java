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

        Generation(Generation generation) {
            this();
        }

        private Generation() {
        }
    }

    private void nextGeneration() {
        this.trip.signalAll();
        this.count = this.parties;
        this.generation = new Generation(null);
    }

    private void breakBarrier() {
        this.generation.broken = true;
        this.count = this.parties;
        this.trip.signalAll();
    }

    private int dowait(boolean timed, long nanos) throws InterruptedException, TimeoutException, BrokenBarrierException {
        Generation g;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            g = this.generation;
        } finally {
            lock.unlock();
        }
        if (g.broken) {
            throw new BrokenBarrierException();
        }
        if (Thread.interrupted()) {
            breakBarrier();
            throw new InterruptedException();
        }
        int index = this.count - 1;
        this.count = index;
        if (index == 0) {
            boolean ranAction = false;
            try {
                Runnable command = this.barrierCommand;
                if (command != null) {
                    command.run();
                }
                ranAction = true;
                nextGeneration();
                return 0;
            } finally {
                if (!ranAction) {
                    breakBarrier();
                }
            }
        }
        while (true) {
            if (!timed) {
                try {
                    this.trip.await();
                } catch (InterruptedException ie) {
                    if (g == this.generation && !g.broken) {
                        breakBarrier();
                        throw ie;
                    }
                    Thread.currentThread().interrupt();
                }
            } else if (nanos > 0) {
                nanos = this.trip.awaitNanos(nanos);
            }
            if (g.broken) {
                throw new BrokenBarrierException();
            }
            if (g != this.generation) {
                return index;
            }
            if (timed && nanos <= 0) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
        lock.unlock();
    }

    public CyclicBarrier(int parties, Runnable barrierAction) {
        this.lock = new ReentrantLock();
        this.trip = this.lock.newCondition();
        this.generation = new Generation(null);
        if (parties <= 0) {
            throw new IllegalArgumentException();
        }
        this.parties = parties;
        this.count = parties;
        this.barrierCommand = barrierAction;
    }

    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    public int getParties() {
        return this.parties;
    }

    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe);
        }
    }

    public int await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, BrokenBarrierException {
        return dowait(true, unit.toNanos(timeout));
    }

    public boolean isBroken() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.generation.broken;
        } finally {
            lock.unlock();
        }
    }

    public void reset() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();
            nextGeneration();
        } finally {
            lock.unlock();
        }
    }

    public int getNumberWaiting() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return this.parties - this.count;
        } finally {
            lock.unlock();
        }
    }
}
