package com.android.documentsui;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;

public abstract class UriDerivativeLoader<P, R> extends AsyncTaskLoader<R> {
    private CancellationSignal mCancellationSignal;
    final Loader<R>.ForceLoadContentObserver mObserver;
    private final P mParam;
    private R mResult;

    public abstract R loadInBackground(P p, CancellationSignal cancellationSignal);

    @Override
    public final R loadInBackground() {
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            this.mCancellationSignal = new CancellationSignal();
        }
        try {
            R rLoadInBackground = loadInBackground(this.mParam, this.mCancellationSignal);
            synchronized (this) {
                this.mCancellationSignal = null;
            }
            return rLoadInBackground;
        } catch (Throwable th) {
            synchronized (this) {
                this.mCancellationSignal = null;
                throw th;
            }
        }
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();
        synchronized (this) {
            if (this.mCancellationSignal != null) {
                this.mCancellationSignal.cancel();
            }
        }
    }

    @Override
    public void deliverResult(R result) {
        if (isReset()) {
            closeQuietly(result);
            return;
        }
        R oldResult = this.mResult;
        this.mResult = result;
        if (isStarted()) {
            super.deliverResult(result);
        }
        if (oldResult != null && oldResult != result) {
            closeQuietly(oldResult);
        }
    }

    public UriDerivativeLoader(Context context, P param) {
        super(context);
        this.mObserver = new Loader.ForceLoadContentObserver(this);
        this.mParam = param;
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
    public void onCanceled(R result) {
        closeQuietly(result);
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        closeQuietly(this.mResult);
        this.mResult = null;
        getContext().getContentResolver().unregisterContentObserver(this.mObserver);
    }

    private void closeQuietly(R result) {
        if (result instanceof AutoCloseable) {
            try {
                ((AutoCloseable) result).close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception e) {
            }
        }
    }
}
