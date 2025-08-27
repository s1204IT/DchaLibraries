package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.lang.Thread;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/* loaded from: classes.dex */
public final class ThreadFactoryBuilder {
    private String nameFormat = null;
    private Boolean daemon = null;
    private Integer priority = null;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = null;
    private ThreadFactory backingThreadFactory = null;

    public ThreadFactoryBuilder setNameFormat(String str) {
        String.format(str, 0);
        this.nameFormat = str;
        return this;
    }

    public ThreadFactoryBuilder setThreadFactory(ThreadFactory threadFactory) {
        this.backingThreadFactory = (ThreadFactory) Preconditions.checkNotNull(threadFactory);
        return this;
    }

    public ThreadFactory build() {
        return build(this);
    }

    private static ThreadFactory build(ThreadFactoryBuilder threadFactoryBuilder) {
        ThreadFactory threadFactoryDefaultThreadFactory;
        final String str = threadFactoryBuilder.nameFormat;
        final Boolean bool = threadFactoryBuilder.daemon;
        final Integer num = threadFactoryBuilder.priority;
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = threadFactoryBuilder.uncaughtExceptionHandler;
        if (threadFactoryBuilder.backingThreadFactory != null) {
            threadFactoryDefaultThreadFactory = threadFactoryBuilder.backingThreadFactory;
        } else {
            threadFactoryDefaultThreadFactory = Executors.defaultThreadFactory();
        }
        final ThreadFactory threadFactory = threadFactoryDefaultThreadFactory;
        final AtomicLong atomicLong = str != null ? new AtomicLong(0L) : null;
        return new ThreadFactory() { // from class: com.google.common.util.concurrent.ThreadFactoryBuilder.1
            @Override // java.util.concurrent.ThreadFactory
            public Thread newThread(Runnable runnable) {
                Thread threadNewThread = threadFactory.newThread(runnable);
                if (str != null) {
                    threadNewThread.setName(String.format(str, Long.valueOf(atomicLong.getAndIncrement())));
                }
                if (bool != null) {
                    threadNewThread.setDaemon(bool.booleanValue());
                }
                if (num != null) {
                    threadNewThread.setPriority(num.intValue());
                }
                if (uncaughtExceptionHandler != null) {
                    threadNewThread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
                }
                return threadNewThread;
            }
        };
    }
}
