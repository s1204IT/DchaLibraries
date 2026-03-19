package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;

public class SharedBuffer {
    private volatile Buffer mProducer = null;
    private volatile Buffer mConsumer = null;
    private volatile Buffer mIntermediate = null;
    private volatile boolean mNeedsSwap = false;
    private volatile boolean mNeedsRepaint = true;

    public synchronized void setProducer(Bitmap bitmap) {
        if (this.mProducer != null && !this.mProducer.isSameSize(bitmap)) {
            this.mProducer.remove();
            this.mProducer = null;
        }
        if (this.mProducer == null) {
            this.mProducer = new Buffer(bitmap);
        } else {
            this.mProducer.useBitmap(bitmap);
        }
    }

    public synchronized Buffer getProducer() {
        return this.mProducer;
    }

    public synchronized Buffer getConsumer() {
        return this.mConsumer;
    }

    public synchronized void swapProducer() {
        Buffer buffer = this.mIntermediate;
        this.mIntermediate = this.mProducer;
        this.mProducer = buffer;
        this.mNeedsSwap = true;
    }

    public synchronized void swapConsumerIfNeeded() {
        if (this.mNeedsSwap) {
            Buffer buffer = this.mIntermediate;
            this.mIntermediate = this.mConsumer;
            this.mConsumer = buffer;
            this.mNeedsSwap = false;
        }
    }

    public synchronized void invalidate() {
        this.mNeedsRepaint = true;
    }
}
