package com.bumptech.glide.load.engine;

import android.os.Looper;
import com.bumptech.glide.load.Key;

public abstract class Resource<Z> {
    private volatile int acquired;
    private boolean isCacheable;
    private volatile boolean isRecycled;
    private Key key;
    private ResourceListener listener;

    interface ResourceListener {
        void onResourceReleased(Key key, Resource resource);
    }

    public abstract Z get();

    public abstract int getSize();

    protected abstract void recycleInternal();

    void setResourceListener(Key key, ResourceListener listener) {
        this.key = key;
        this.listener = listener;
    }

    void setCacheable(boolean isCacheable) {
        this.isCacheable = isCacheable;
    }

    boolean isCacheable() {
        return this.isCacheable;
    }

    public void recycle() {
        if (this.acquired > 0) {
            throw new IllegalStateException("Cannot recycle a resource while it is still acquired");
        }
        if (this.isRecycled) {
            throw new IllegalStateException("Cannot recycle a resource that has already been recycled");
        }
        this.isRecycled = true;
        recycleInternal();
    }

    public void acquire(int times) {
        if (this.isRecycled) {
            throw new IllegalStateException("Cannot acquire a recycled resource");
        }
        if (times <= 0) {
            throw new IllegalArgumentException("Must acquire a number of times >= 0");
        }
        if (!Looper.getMainLooper().equals(Looper.myLooper())) {
            throw new IllegalThreadStateException("Must call acquire on the main thread");
        }
        this.acquired += times;
    }

    public void release() {
        if (this.acquired <= 0) {
            throw new IllegalStateException("Cannot release a recycled or not yet acquired resource");
        }
        if (!Looper.getMainLooper().equals(Looper.myLooper())) {
            throw new IllegalThreadStateException("Must call release on the main thread");
        }
        int i = this.acquired - 1;
        this.acquired = i;
        if (i == 0) {
            this.listener.onResourceReleased(this.key, this);
        }
    }
}
