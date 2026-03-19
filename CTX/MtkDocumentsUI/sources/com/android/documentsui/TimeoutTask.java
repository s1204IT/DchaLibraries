package com.android.documentsui;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import com.android.documentsui.base.CheckedTask;

public abstract class TimeoutTask<Input, Output> extends CheckedTask<Input, Output> {
    private long mTimeout;

    public TimeoutTask(CheckedTask.Check check, long j) {
        super(check);
        this.mTimeout = -1L;
        this.mTimeout = j;
    }

    @Override
    protected void prepare() {
        if (this.mTimeout < 0) {
            return;
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public final void run() {
                TimeoutTask.lambda$prepare$0(this.f$0);
            }
        }, this.mTimeout);
    }

    public static void lambda$prepare$0(TimeoutTask timeoutTask) {
        if (timeoutTask.getStatus() == AsyncTask.Status.RUNNING) {
            timeoutTask.onTimeout();
            timeoutTask.cancel(true);
            timeoutTask.finish(null);
        }
    }

    protected void onTimeout() {
    }
}
