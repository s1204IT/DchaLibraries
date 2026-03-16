package com.bumptech.glide.load.engine.executor;

import android.os.Process;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FifoPriorityThreadPoolExecutor extends ThreadPoolExecutor {
    AtomicInteger ordering;

    public FifoPriorityThreadPoolExecutor(int poolSize) {
        this(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new DefaultThreadFactory());
    }

    public FifoPriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAlive, TimeUnit timeUnit, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAlive, timeUnit, new PriorityBlockingQueue(), threadFactory);
        this.ordering = new AtomicInteger();
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FifoPriorityLoadTask(runnable, value, this.ordering.getAndIncrement());
    }

    public static class DefaultThreadFactory implements ThreadFactory {
        int threadNum = 0;

        @Override
        public Thread newThread(Runnable runnable) {
            Thread result = new Thread(runnable, "image-manager-resize-" + this.threadNum) {
                @Override
                public void run() {
                    Process.setThreadPriority(10);
                    super.run();
                }
            };
            this.threadNum++;
            return result;
        }
    }

    private static class FifoPriorityLoadTask<T> extends FutureTask<T> implements Comparable<FifoPriorityLoadTask> {
        private final int order;
        private final int priority;

        public FifoPriorityLoadTask(Runnable runnable, T result, int order) {
            super(runnable, result);
            if (!(runnable instanceof Prioritized)) {
                throw new IllegalArgumentException("FifoPriorityThreadPoolExecutor must be given Runnables that implement Prioritized");
            }
            this.priority = ((Prioritized) runnable).getPriority();
            this.order = order;
        }

        @Override
        public int compareTo(FifoPriorityLoadTask loadTask) {
            int result = this.priority - loadTask.priority;
            if (result == 0 && loadTask != this) {
                return this.order - loadTask.order;
            }
            return result;
        }
    }
}
