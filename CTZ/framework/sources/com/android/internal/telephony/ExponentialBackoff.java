package com.android.internal.telephony;

import android.os.Handler;
import android.os.Looper;
import com.android.internal.annotations.VisibleForTesting;

public class ExponentialBackoff {
    private long mCurrentDelayMs;
    private final Handler mHandler;
    private HandlerAdapter mHandlerAdapter;
    private long mMaximumDelayMs;
    private int mMultiplier;
    private int mRetryCounter;
    private final Runnable mRunnable;
    private long mStartDelayMs;

    public interface HandlerAdapter {
        boolean postDelayed(Runnable runnable, long j);

        void removeCallbacks(Runnable runnable);
    }

    public ExponentialBackoff(long j, long j2, int i, Looper looper, Runnable runnable) {
        this(j, j2, i, new Handler(looper), runnable);
    }

    public ExponentialBackoff(long j, long j2, int i, Handler handler, Runnable runnable) {
        this.mHandlerAdapter = new HandlerAdapter() {
            @Override
            public boolean postDelayed(Runnable runnable2, long j3) {
                return ExponentialBackoff.this.mHandler.postDelayed(runnable2, j3);
            }

            @Override
            public void removeCallbacks(Runnable runnable2) {
                ExponentialBackoff.this.mHandler.removeCallbacks(runnable2);
            }
        };
        this.mRetryCounter = 0;
        this.mStartDelayMs = j;
        this.mMaximumDelayMs = j2;
        this.mMultiplier = i;
        this.mHandler = handler;
        this.mRunnable = runnable;
    }

    public void start() {
        this.mRetryCounter = 0;
        this.mCurrentDelayMs = this.mStartDelayMs;
        this.mHandlerAdapter.removeCallbacks(this.mRunnable);
        this.mHandlerAdapter.postDelayed(this.mRunnable, this.mCurrentDelayMs);
    }

    public void stop() {
        this.mRetryCounter = 0;
        this.mHandlerAdapter.removeCallbacks(this.mRunnable);
    }

    public void notifyFailed() {
        this.mRetryCounter++;
        this.mCurrentDelayMs = (long) (((1.0d + Math.random()) / 2.0d) * Math.min(this.mMaximumDelayMs, (long) (this.mStartDelayMs * Math.pow(this.mMultiplier, this.mRetryCounter))));
        this.mHandlerAdapter.removeCallbacks(this.mRunnable);
        this.mHandlerAdapter.postDelayed(this.mRunnable, this.mCurrentDelayMs);
    }

    public long getCurrentDelay() {
        return this.mCurrentDelayMs;
    }

    @VisibleForTesting
    public void setHandlerAdapter(HandlerAdapter handlerAdapter) {
        this.mHandlerAdapter = handlerAdapter;
    }
}
