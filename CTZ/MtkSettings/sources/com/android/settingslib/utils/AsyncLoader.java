package com.android.settingslib.utils;

import android.content.AsyncTaskLoader;
import android.content.Context;

public abstract class AsyncLoader<T> extends AsyncTaskLoader<T> {
    private T mResult;

    protected abstract void onDiscardResult(T t);

    public AsyncLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        if (this.mResult != null) {
            deliverResult(this.mResult);
        }
        if (takeContentChanged() || this.mResult == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
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

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        if (this.mResult != null) {
            onDiscardResult(this.mResult);
        }
        this.mResult = null;
    }

    @Override
    public void onCanceled(T t) {
        super.onCanceled(t);
        if (t != null) {
            onDiscardResult(t);
        }
    }
}
