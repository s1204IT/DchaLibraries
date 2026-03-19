package com.android.server;

import android.os.Build;
import android.util.Slog;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.Preconditions;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SystemServerInitThreadPool {
    private static final int SHUTDOWN_TIMEOUT_MILLIS = 20000;
    private static SystemServerInitThreadPool sInstance;
    private ExecutorService mService = ConcurrentUtils.newFixedThreadPool(4, "system-server-init-thread", -2);
    private static final String TAG = SystemServerInitThreadPool.class.getSimpleName();
    private static final boolean IS_DEBUGGABLE = Build.IS_DEBUGGABLE;

    public static synchronized SystemServerInitThreadPool get() {
        if (sInstance == null) {
            sInstance = new SystemServerInitThreadPool();
        }
        Preconditions.checkState(sInstance.mService != null, "Cannot get " + TAG + " - it has been shut down");
        return sInstance;
    }

    public Future<?> submit(final Runnable runnable, final String str) {
        if (IS_DEBUGGABLE) {
            return this.mService.submit(new Runnable() {
                @Override
                public final void run() {
                    SystemServerInitThreadPool.lambda$submit$0(str, runnable);
                }
            });
        }
        return this.mService.submit(runnable);
    }

    static void lambda$submit$0(String str, Runnable runnable) {
        Slog.d(TAG, "Started executing " + str);
        try {
            runnable.run();
            Slog.d(TAG, "Finished executing " + str);
        } catch (RuntimeException e) {
            Slog.e(TAG, "Failure in " + str + ": " + e, e);
            throw e;
        }
    }

    static synchronized void shutdown() {
        if (sInstance != null && sInstance.mService != null) {
            sInstance.mService.shutdown();
            try {
                boolean zAwaitTermination = sInstance.mService.awaitTermination(20000L, TimeUnit.MILLISECONDS);
                List<Runnable> listShutdownNow = sInstance.mService.shutdownNow();
                if (!zAwaitTermination) {
                    throw new IllegalStateException("Cannot shutdown. Unstarted tasks " + listShutdownNow);
                }
                sInstance.mService = null;
                Slog.d(TAG, "Shutdown successful");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(TAG + " init interrupted");
            }
        }
    }
}
