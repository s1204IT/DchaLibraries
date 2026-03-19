package com.mediatek.calendarimporter.service;

import com.mediatek.calendarimporter.utils.LogUtils;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BaseProcessor implements RunnableFuture<Object> {
    private static final String TAG = "BaseProcessor";

    @Override
    public void run() {
        LogUtils.d(TAG, "Processor:" + getClass().getName() + "begin to run!");
    }

    @Override
    public boolean cancel(boolean z) {
        return false;
    }

    @Override
    public Object get() throws ExecutionException, InterruptedException {
        return null;
    }

    @Override
    public Object get(long j, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        return null;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }
}
