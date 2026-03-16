package com.bumptech.glide.volley;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class VolleyRequestFuture<T> implements Future<T>, Response.Listener<T>, Response.ErrorListener {
    private VolleyError mException;
    private Request<?> mRequest;
    private T mResult;
    private boolean mResultReceived = false;
    private boolean mIsCancelled = false;

    public static <E> VolleyRequestFuture<E> newFuture() {
        return new VolleyRequestFuture<>();
    }

    public synchronized void setRequest(Request<?> request) {
        this.mRequest = request;
        if (this.mIsCancelled && this.mRequest != null) {
            this.mRequest.cancel();
        }
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        boolean z = true;
        synchronized (this) {
            if (isDone()) {
                z = false;
            } else {
                this.mIsCancelled = true;
                if (this.mRequest != null) {
                    this.mRequest.cancel();
                }
                notifyAll();
            }
        }
        return z;
    }

    @Override
    public T get() throws ExecutionException, InterruptedException {
        try {
            return doGet(null);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return doGet(Long.valueOf(TimeUnit.MILLISECONDS.convert(timeout, unit)));
    }

    private synchronized T doGet(Long timeoutMs) throws ExecutionException, InterruptedException, TimeoutException {
        T t;
        if (this.mException != null) {
            throw new ExecutionException(this.mException);
        }
        if (this.mResultReceived) {
            t = this.mResult;
        } else {
            if (isCancelled()) {
                throw new CancellationException();
            }
            if (timeoutMs == null) {
                wait(0L);
            } else if (timeoutMs.longValue() > 0) {
                wait(timeoutMs.longValue());
            }
            if (this.mException != null) {
                throw new ExecutionException(this.mException);
            }
            if (isCancelled()) {
                throw new CancellationException();
            }
            if (!this.mResultReceived) {
                throw new TimeoutException();
            }
            t = this.mResult;
        }
        return t;
    }

    @Override
    public boolean isCancelled() {
        return this.mIsCancelled;
    }

    @Override
    public synchronized boolean isDone() {
        boolean z;
        if (this.mResultReceived || this.mException != null) {
            z = true;
        } else if (!isCancelled()) {
            z = false;
        }
        return z;
    }

    @Override
    public synchronized void onResponse(T response) {
        this.mResultReceived = true;
        this.mResult = response;
        notifyAll();
    }

    @Override
    public synchronized void onErrorResponse(VolleyError error) {
        this.mException = error;
        notifyAll();
    }
}
