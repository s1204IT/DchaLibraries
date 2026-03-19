package com.android.internal.view;

import android.view.SurfaceHolder;

public class SurfaceCallbackHelper {
    int mFinishDrawingCollected = 0;
    int mFinishDrawingExpected = 0;
    private Runnable mFinishDrawingRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (SurfaceCallbackHelper.this) {
                SurfaceCallbackHelper.this.mFinishDrawingCollected++;
                if (SurfaceCallbackHelper.this.mFinishDrawingCollected < SurfaceCallbackHelper.this.mFinishDrawingExpected) {
                    return;
                }
                SurfaceCallbackHelper.this.mRunnable.run();
            }
        }
    };
    Runnable mRunnable;

    public SurfaceCallbackHelper(Runnable runnable) {
        this.mRunnable = runnable;
    }

    public void dispatchSurfaceRedrawNeededAsync(SurfaceHolder surfaceHolder, SurfaceHolder.Callback[] callbackArr) {
        int i;
        if (callbackArr == null || callbackArr.length == 0) {
            this.mRunnable.run();
            return;
        }
        synchronized (this) {
            this.mFinishDrawingExpected = callbackArr.length;
            this.mFinishDrawingCollected = 0;
        }
        for (SurfaceHolder.Callback callback : callbackArr) {
            if (callback instanceof SurfaceHolder.Callback2) {
                ((SurfaceHolder.Callback2) callback).surfaceRedrawNeededAsync(surfaceHolder, this.mFinishDrawingRunnable);
            } else {
                this.mFinishDrawingRunnable.run();
            }
        }
    }
}
