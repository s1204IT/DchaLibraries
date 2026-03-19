package com.android.internal.util;

import android.os.Process;
import android.util.Slog;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentUtils {
    private ConcurrentUtils() {
    }

    public static ExecutorService newFixedThreadPool(int i, final String str, final int i2) {
        return Executors.newFixedThreadPool(i, new ThreadFactory() {
            private final AtomicInteger threadNum = new AtomicInteger(0);

            @Override
            public Thread newThread(final Runnable runnable) {
                return new Thread(str + this.threadNum.incrementAndGet()) {
                    @Override
                    public void run() {
                        Process.setThreadPriority(i2);
                        runnable.run();
                    }
                };
            }
        });
    }

    public static <T> T waitForFutureNoInterrupt(Future<T> future, String str) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(str + " interrupted");
        } catch (ExecutionException e2) {
            throw new RuntimeException(str + " failed", e2);
        }
    }

    public static void waitForCountDownNoInterrupt(CountDownLatch countDownLatch, long j, String str) {
        try {
            if (!countDownLatch.await(j, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(str + " timed out.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(str + " interrupted.");
        }
    }

    public static void wtfIfLockHeld(String str, Object obj) {
        if (Thread.holdsLock(obj)) {
            Slog.wtf(str, "Lock mustn't be held");
        }
    }

    public static void wtfIfLockNotHeld(String str, Object obj) {
        if (!Thread.holdsLock(obj)) {
            Slog.wtf(str, "Lock must be held");
        }
    }
}
