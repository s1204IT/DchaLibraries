package com.google.common.util.concurrent;

import com.google.common.collect.Lists;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ExecutionList {
    private static final Logger log = Logger.getLogger(ExecutionList.class.getName());
    private final Queue<RunnableExecutorPair> runnables = Lists.newLinkedList();
    private boolean executed = false;

    public void execute() {
        synchronized (this.runnables) {
            if (!this.executed) {
                this.executed = true;
                while (!this.runnables.isEmpty()) {
                    this.runnables.poll().execute();
                }
            }
        }
    }

    private static class RunnableExecutorPair {
        final Executor executor;
        final Runnable runnable;

        void execute() {
            try {
                this.executor.execute(this.runnable);
            } catch (RuntimeException e) {
                ExecutionList.log.log(Level.SEVERE, "RuntimeException while executing runnable " + this.runnable + " with executor " + this.executor, (Throwable) e);
            }
        }
    }
}
