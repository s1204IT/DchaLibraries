package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

final class SerializingExecutor implements Executor {
    private static final Logger log = Logger.getLogger(SerializingExecutor.class.getName());
    private final Executor executor;
    private final Queue<Runnable> waitQueue = new ArrayDeque();
    private boolean isThreadScheduled = false;
    private final TaskRunner taskRunner = new TaskRunner();
    private final Object internalLock = new Object() {
        public String toString() {
            return "SerializingExecutor lock: " + super.toString();
        }
    };

    public SerializingExecutor(Executor executor) {
        Preconditions.checkNotNull(executor, "'executor' must not be null.");
        this.executor = executor;
    }

    @Override
    public void execute(Runnable runnable) {
        boolean z;
        Preconditions.checkNotNull(runnable, "'r' must not be null.");
        synchronized (this.internalLock) {
            this.waitQueue.add(runnable);
            z = true;
            if (!this.isThreadScheduled) {
                this.isThreadScheduled = true;
            } else {
                z = false;
            }
        }
        if (z) {
            try {
                this.executor.execute(this.taskRunner);
            } catch (Throwable th) {
                synchronized (this.internalLock) {
                    this.isThreadScheduled = false;
                    throw th;
                }
            }
        }
    }

    private class TaskRunner implements Runnable {
        private TaskRunner() {
        }

        @Override
        public void run() {
            while (true) {
                r1 = true;
                com.google.common.base.Preconditions.checkState(com.google.common.util.concurrent.SerializingExecutor.this.isThreadScheduled);
                r2 = com.google.common.util.concurrent.SerializingExecutor.this.internalLock;
                synchronized (r2) {
                    ;
                    r3 = (java.lang.Runnable) com.google.common.util.concurrent.SerializingExecutor.this.waitQueue.poll();
                    if (r3 == null) {
                        com.google.common.util.concurrent.SerializingExecutor.this.isThreadScheduled = false;
                        return;
                    }
                }
                while (true) {
                    throw r3;
                }
            }
        }
    }
}
