package com.android.settingslib.utils;

import android.content.AsyncTaskLoader;
import android.content.Context;

/* loaded from: classes.dex */
public abstract class AsyncLoader<T> extends AsyncTaskLoader<T> {
    private T mResult;

    protected abstract void onDiscardResult(T t);

    public AsyncLoader(Context context) {
        super(context);
    }

    @Override // android.content.Loader
    protected void onStartLoading() {
        if (this.mResult != null) {
            deliverResult(this.mResult);
        }
        if (takeContentChanged() || this.mResult == null) {
            forceLoad();
        }
    }

    @Override // android.content.Loader
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override // android.content.Loader
    public void deliverResult(T t) {
        if (isReset()) {
            if (t != null) {
                onDiscardResult(t);
                return;
            }
            return;
        }
        T t2 = this.mResult;
        this.mResult = t;
        if (isStarted()) {
            super.deliverResult(t);
        }
        if (t2 != null && t2 != this.mResult) {
            onDiscardResult(t2);
        }
    }

    @Override // android.content.Loader
    protected void onReset() {
        super.onReset();
        onStopLoading();
        if (this.mResult != null) {
            onDiscardResult(this.mResult);
        }
        this.mResult = null;
    }

    @Override // android.content.AsyncTaskLoader
    public void onCanceled(T t) {
        super.onCanceled(t);
        if (t != null) {
            onDiscardResult(t);
        }
    }
}
