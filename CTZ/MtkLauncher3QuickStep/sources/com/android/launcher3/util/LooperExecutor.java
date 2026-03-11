package com.android.launcher3.util;

import android.os.Handler;
import android.os.Looper;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

public class LooperExecutor extends AbstractExecutorService {
    private final Handler mHandler;

    public LooperExecutor(Looper looper) {
        this.mHandler = new Handler(looper);
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    @Override
    public void execute(Runnable runnable) {
        if (this.mHandler.getLooper() == Looper.myLooper()) {
            runnable.run();
        } else {
            this.mHandler.post(runnable);
        }
    }

    @Override
    @Deprecated
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    @Deprecated
    public boolean awaitTermination(long j, TimeUnit timeUnit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }
}
