package com.android.statementservice;

import android.util.Log;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

final class ExceptionLoggingFutureTask<V> extends FutureTask<V> {
    private final String mTag;

    public ExceptionLoggingFutureTask(Callable<V> callable, String str) {
        super(callable);
        this.mTag = str;
    }

    @Override
    protected void done() {
        try {
            get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e(this.mTag, "Uncaught exception.", e);
            throw new RuntimeException(e);
        }
    }
}
