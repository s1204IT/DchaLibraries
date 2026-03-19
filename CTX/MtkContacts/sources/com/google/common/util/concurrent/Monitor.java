package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class Monitor {
    private Guard activeGuards;
    private final boolean fair;
    private final ReentrantLock lock;

    public static abstract class Guard {
        final Condition condition;
        final Monitor monitor;
        Guard next;
        int waiterCount = 0;

        public abstract boolean isSatisfied();

        protected Guard(Monitor monitor) {
            this.monitor = (Monitor) Preconditions.checkNotNull(monitor, "monitor");
            this.condition = monitor.lock.newCondition();
        }
    }

    public Monitor() {
        this(false);
    }

    public Monitor(boolean z) {
        this.activeGuards = null;
        this.fair = z;
        this.lock = new ReentrantLock(z);
    }

    public void enter() {
        this.lock.lock();
    }

    public void enterInterruptibly() throws InterruptedException {
        this.lock.lockInterruptibly();
    }

    public boolean enter(long j, TimeUnit timeUnit) throws Throwable {
        boolean zTryLock;
        long nanos = timeUnit.toNanos(j);
        ReentrantLock reentrantLock = this.lock;
        if (!this.fair && reentrantLock.tryLock()) {
            return true;
        }
        long jNanoTime = System.nanoTime() + nanos;
        boolean zInterrupted = Thread.interrupted();
        while (true) {
            try {
                zTryLock = reentrantLock.tryLock(nanos, TimeUnit.NANOSECONDS);
                break;
            } catch (InterruptedException e) {
                try {
                    nanos = jNanoTime - System.nanoTime();
                    zInterrupted = true;
                } catch (Throwable th) {
                    th = th;
                    zInterrupted = true;
                    if (zInterrupted) {
                        Thread.currentThread().interrupt();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                if (zInterrupted) {
                }
                throw th;
            }
        }
        if (zInterrupted) {
            Thread.currentThread().interrupt();
        }
        return zTryLock;
    }

    public boolean enterInterruptibly(long j, TimeUnit timeUnit) throws InterruptedException {
        return this.lock.tryLock(j, timeUnit);
    }

    public boolean tryEnter() {
        return this.lock.tryLock();
    }

    public void enterWhen(Guard guard) throws InterruptedException {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        boolean zIsHeldByCurrentThread = reentrantLock.isHeldByCurrentThread();
        reentrantLock.lockInterruptibly();
        try {
            if (!guard.isSatisfied()) {
                await(guard, zIsHeldByCurrentThread);
            }
        } catch (Throwable th) {
            leave();
            throw th;
        }
    }

    public void enterWhenUninterruptibly(Guard guard) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        boolean zIsHeldByCurrentThread = reentrantLock.isHeldByCurrentThread();
        reentrantLock.lock();
        try {
            if (!guard.isSatisfied()) {
                awaitUninterruptibly(guard, zIsHeldByCurrentThread);
            }
        } catch (Throwable th) {
            leave();
            throw th;
        }
    }

    public boolean enterWhen(Guard guard, long j, TimeUnit timeUnit) throws InterruptedException {
        boolean z;
        long nanos = timeUnit.toNanos(j);
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        boolean zIsHeldByCurrentThread = reentrantLock.isHeldByCurrentThread();
        if (this.fair || !reentrantLock.tryLock()) {
            long jNanoTime = System.nanoTime() + nanos;
            if (!reentrantLock.tryLock(j, timeUnit)) {
                return false;
            }
            nanos = jNanoTime - System.nanoTime();
        }
        try {
            if (!guard.isSatisfied()) {
                z = awaitNanos(guard, nanos, zIsHeldByCurrentThread);
            }
            if (!z) {
            }
            return z;
        } catch (Throwable th) {
            if (!zIsHeldByCurrentThread) {
                try {
                    signalNextWaiter();
                } finally {
                    reentrantLock.unlock();
                }
            }
            throw th;
        }
    }

    public boolean enterWhenUninterruptibly(Guard guard, long j, TimeUnit timeUnit) {
        boolean zTryLock;
        long nanos = timeUnit.toNanos(j);
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        long jNanoTime = System.nanoTime() + nanos;
        boolean zIsHeldByCurrentThread = reentrantLock.isHeldByCurrentThread();
        boolean zInterrupted = Thread.interrupted();
        try {
            boolean z = true;
            if (this.fair || !reentrantLock.tryLock()) {
                boolean z2 = false;
                do {
                    try {
                        zTryLock = reentrantLock.tryLock(nanos, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException e) {
                        zInterrupted = true;
                    }
                    if (!zTryLock) {
                        return false;
                    }
                    z2 = zTryLock;
                    nanos = jNanoTime - System.nanoTime();
                } while (!z2);
            }
            while (true) {
                try {
                    if (guard.isSatisfied()) {
                        break;
                    }
                    if (!awaitNanos(guard, nanos, zIsHeldByCurrentThread)) {
                        z = false;
                    }
                } catch (InterruptedException e2) {
                    try {
                        nanos = jNanoTime - System.nanoTime();
                        zInterrupted = true;
                        zIsHeldByCurrentThread = false;
                    } catch (Throwable th) {
                        th = th;
                        reentrantLock.unlock();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    reentrantLock.unlock();
                    throw th;
                }
            }
            if (!z) {
                reentrantLock.unlock();
            }
            if (zInterrupted) {
                Thread.currentThread().interrupt();
            }
            return z;
        } finally {
            if (zInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean enterIf(Guard guard) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lock();
        try {
            boolean zIsSatisfied = guard.isSatisfied();
            if (!zIsSatisfied) {
            }
            return zIsSatisfied;
        } finally {
            reentrantLock.unlock();
        }
    }

    public boolean enterIfInterruptibly(Guard guard) throws InterruptedException {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        reentrantLock.lockInterruptibly();
        try {
            boolean zIsSatisfied = guard.isSatisfied();
            if (!zIsSatisfied) {
            }
            return zIsSatisfied;
        } finally {
            reentrantLock.unlock();
        }
    }

    public boolean enterIf(Guard guard, long j, TimeUnit timeUnit) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        if (!enter(j, timeUnit)) {
            return false;
        }
        try {
            boolean zIsSatisfied = guard.isSatisfied();
            if (!zIsSatisfied) {
            }
            return zIsSatisfied;
        } finally {
            this.lock.unlock();
        }
    }

    public boolean enterIfInterruptibly(Guard guard, long j, TimeUnit timeUnit) throws InterruptedException {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        if (!reentrantLock.tryLock(j, timeUnit)) {
            return false;
        }
        try {
            boolean zIsSatisfied = guard.isSatisfied();
            if (!zIsSatisfied) {
            }
            return zIsSatisfied;
        } finally {
            reentrantLock.unlock();
        }
    }

    public boolean tryEnterIf(Guard guard) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        ReentrantLock reentrantLock = this.lock;
        if (!reentrantLock.tryLock()) {
            return false;
        }
        try {
            boolean zIsSatisfied = guard.isSatisfied();
            if (!zIsSatisfied) {
            }
            return zIsSatisfied;
        } finally {
            reentrantLock.unlock();
        }
    }

    public void waitFor(Guard guard) throws InterruptedException {
        if (!((guard.monitor == this) & this.lock.isHeldByCurrentThread())) {
            throw new IllegalMonitorStateException();
        }
        if (!guard.isSatisfied()) {
            await(guard, true);
        }
    }

    public void waitForUninterruptibly(Guard guard) {
        if (!((guard.monitor == this) & this.lock.isHeldByCurrentThread())) {
            throw new IllegalMonitorStateException();
        }
        if (!guard.isSatisfied()) {
            awaitUninterruptibly(guard, true);
        }
    }

    public boolean waitFor(Guard guard, long j, TimeUnit timeUnit) throws InterruptedException {
        long nanos = timeUnit.toNanos(j);
        if ((guard.monitor == this) && this.lock.isHeldByCurrentThread()) {
            return guard.isSatisfied() || awaitNanos(guard, nanos, true);
        }
        throw new IllegalMonitorStateException();
    }

    public boolean waitForUninterruptibly(Guard guard, long j, TimeUnit timeUnit) throws Throwable {
        long nanos = timeUnit.toNanos(j);
        boolean z = true;
        if (!(guard.monitor == this) || !this.lock.isHeldByCurrentThread()) {
            throw new IllegalMonitorStateException();
        }
        if (guard.isSatisfied()) {
            return true;
        }
        long jNanoTime = System.nanoTime() + nanos;
        boolean zInterrupted = Thread.interrupted();
        boolean z2 = true;
        while (true) {
            try {
                boolean zAwaitNanos = awaitNanos(guard, nanos, z2);
                if (zInterrupted) {
                    Thread.currentThread().interrupt();
                }
                return zAwaitNanos;
            } catch (InterruptedException e) {
                try {
                    if (guard.isSatisfied()) {
                        Thread.currentThread().interrupt();
                        return true;
                    }
                    nanos = jNanoTime - System.nanoTime();
                    z2 = false;
                    zInterrupted = true;
                } catch (Throwable th) {
                    th = th;
                    if (z) {
                        Thread.currentThread().interrupt();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                z = zInterrupted;
                if (z) {
                }
                throw th;
            }
        }
    }

    public void leave() {
        ReentrantLock reentrantLock = this.lock;
        try {
            if (reentrantLock.getHoldCount() == 1) {
                signalNextWaiter();
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    public boolean isFair() {
        return this.fair;
    }

    public boolean isOccupied() {
        return this.lock.isLocked();
    }

    public boolean isOccupiedByCurrentThread() {
        return this.lock.isHeldByCurrentThread();
    }

    public int getOccupiedDepth() {
        return this.lock.getHoldCount();
    }

    public int getQueueLength() {
        return this.lock.getQueueLength();
    }

    public boolean hasQueuedThreads() {
        return this.lock.hasQueuedThreads();
    }

    public boolean hasQueuedThread(Thread thread) {
        return this.lock.hasQueuedThread(thread);
    }

    public boolean hasWaiters(Guard guard) {
        return getWaitQueueLength(guard) > 0;
    }

    public int getWaitQueueLength(Guard guard) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        }
        this.lock.lock();
        try {
            return guard.waiterCount;
        } finally {
            this.lock.unlock();
        }
    }

    private void signalNextWaiter() {
        for (Guard guard = this.activeGuards; guard != null; guard = guard.next) {
            if (isSatisfied(guard)) {
                guard.condition.signal();
                return;
            }
        }
    }

    private boolean isSatisfied(Guard guard) {
        try {
            return guard.isSatisfied();
        } catch (Throwable th) {
            signalAllWaiters();
            throw Throwables.propagate(th);
        }
    }

    private void signalAllWaiters() {
        for (Guard guard = this.activeGuards; guard != null; guard = guard.next) {
            guard.condition.signalAll();
        }
    }

    private void beginWaitingFor(Guard guard) {
        int i = guard.waiterCount;
        guard.waiterCount = i + 1;
        if (i == 0) {
            guard.next = this.activeGuards;
            this.activeGuards = guard;
        }
    }

    private void endWaitingFor(Guard guard) {
        int i = guard.waiterCount - 1;
        guard.waiterCount = i;
        if (i == 0) {
            Guard guard2 = this.activeGuards;
            Guard guard3 = null;
            while (guard2 != guard) {
                guard3 = guard2;
                guard2 = guard2.next;
            }
            if (guard3 == null) {
                this.activeGuards = guard2.next;
            } else {
                guard3.next = guard2.next;
            }
            guard2.next = null;
        }
    }

    private void await(Guard guard, boolean z) throws InterruptedException {
        if (z) {
            signalNextWaiter();
        }
        beginWaitingFor(guard);
        do {
            try {
                guard.condition.await();
            } finally {
                endWaitingFor(guard);
            }
        } while (!guard.isSatisfied());
    }

    private void awaitUninterruptibly(Guard guard, boolean z) {
        if (z) {
            signalNextWaiter();
        }
        beginWaitingFor(guard);
        do {
            try {
                guard.condition.awaitUninterruptibly();
            } finally {
                endWaitingFor(guard);
            }
        } while (!guard.isSatisfied());
    }

    private boolean awaitNanos(Guard guard, long j, boolean z) throws InterruptedException {
        if (z) {
            signalNextWaiter();
        }
        beginWaitingFor(guard);
        while (j >= 0) {
            try {
                j = guard.condition.awaitNanos(j);
                if (guard.isSatisfied()) {
                    return true;
                }
            } finally {
                endWaitingFor(guard);
            }
        }
        return false;
    }
}
