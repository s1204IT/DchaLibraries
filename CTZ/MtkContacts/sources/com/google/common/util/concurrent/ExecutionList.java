package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ExecutionList {
    static final Logger log = Logger.getLogger(ExecutionList.class.getName());
    private boolean executed;
    private RunnableExecutorPair runnables;

    public void add(Runnable runnable, Executor executor) {
        Preconditions.checkNotNull(runnable, "Runnable was null.");
        Preconditions.checkNotNull(executor, "Executor was null.");
        synchronized (this) {
            if (!this.executed) {
                this.runnables = new RunnableExecutorPair(runnable, executor, this.runnables);
            } else {
                executeListener(runnable, executor);
            }
        }
    }

    public void execute() {
        synchronized (this) {
            if (this.executed) {
                return;
            }
            this.executed = true;
            RunnableExecutorPair runnableExecutorPair = this.runnables;
            this.runnables = null;
            RunnableExecutorPair runnableExecutorPair2 = runnableExecutorPair;
            RunnableExecutorPair runnableExecutorPair3 = null;
            while (runnableExecutorPair2 != null) {
                RunnableExecutorPair runnableExecutorPair4 = runnableExecutorPair2.next;
                runnableExecutorPair2.next = runnableExecutorPair3;
                runnableExecutorPair3 = runnableExecutorPair2;
                runnableExecutorPair2 = runnableExecutorPair4;
            }
            while (runnableExecutorPair3 != null) {
                executeListener(runnableExecutorPair3.runnable, runnableExecutorPair3.executor);
                runnableExecutorPair3 = runnableExecutorPair3.next;
            }
        }
    }

    private static void executeListener(Runnable runnable, Executor executor) {
        try {
            executor.execute(runnable);
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "RuntimeException while executing runnable " + runnable + " with executor " + executor, (Throwable) e);
        }
    }

    private static final class RunnableExecutorPair {
        final Executor executor;
        RunnableExecutorPair next;
        final Runnable runnable;

        RunnableExecutorPair(Runnable runnable, Executor executor, RunnableExecutorPair runnableExecutorPair) {
            this.runnable = runnable;
            this.executor = executor;
            this.next = runnableExecutorPair;
        }
    }
}
