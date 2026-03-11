package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.lang.Thread;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class ThreadFactoryBuilder {
    private String nameFormat = null;
    private Boolean daemon = null;
    private Integer priority = null;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = null;
    private ThreadFactory backingThreadFactory = null;

    private static ThreadFactory build(ThreadFactoryBuilder threadFactoryBuilder) {
        String str = threadFactoryBuilder.nameFormat;
        return new ThreadFactory(threadFactoryBuilder.backingThreadFactory != null ? threadFactoryBuilder.backingThreadFactory : Executors.defaultThreadFactory(), str, str != null ? new AtomicLong(0L) : null, threadFactoryBuilder.daemon, threadFactoryBuilder.priority, threadFactoryBuilder.uncaughtExceptionHandler) {
            final ThreadFactory val$backingThreadFactory;
            final AtomicLong val$count;
            final Boolean val$daemon;
            final String val$nameFormat;
            final Integer val$priority;
            final Thread.UncaughtExceptionHandler val$uncaughtExceptionHandler;

            {
                this.val$backingThreadFactory = threadFactory;
                this.val$nameFormat = str;
                this.val$count = atomicLong;
                this.val$daemon = bool;
                this.val$priority = num;
                this.val$uncaughtExceptionHandler = uncaughtExceptionHandler;
            }

            @Override
            public Thread newThread(Runnable runnable) {
                Thread threadNewThread = this.val$backingThreadFactory.newThread(runnable);
                if (this.val$nameFormat != null) {
                    threadNewThread.setName(String.format(this.val$nameFormat, Long.valueOf(this.val$count.getAndIncrement())));
                }
                if (this.val$daemon != null) {
                    threadNewThread.setDaemon(this.val$daemon.booleanValue());
                }
                if (this.val$priority != null) {
                    threadNewThread.setPriority(this.val$priority.intValue());
                }
                if (this.val$uncaughtExceptionHandler != null) {
                    threadNewThread.setUncaughtExceptionHandler(this.val$uncaughtExceptionHandler);
                }
                return threadNewThread;
            }
        };
    }

    public ThreadFactory build() {
        return build(this);
    }

    public ThreadFactoryBuilder setNameFormat(String str) {
        String.format(str, 0);
        this.nameFormat = str;
        return this;
    }

    public ThreadFactoryBuilder setThreadFactory(ThreadFactory threadFactory) {
        this.backingThreadFactory = (ThreadFactory) Preconditions.checkNotNull(threadFactory);
        return this;
    }
}
