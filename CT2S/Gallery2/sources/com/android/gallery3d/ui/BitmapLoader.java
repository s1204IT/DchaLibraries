package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.photos.data.GalleryBitmapPool;

public abstract class BitmapLoader implements FutureListener<Bitmap> {
    private Bitmap mBitmap;
    private int mState = 0;
    private Future<Bitmap> mTask;

    protected abstract void onLoadComplete(Bitmap bitmap);

    protected abstract Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> futureListener);

    @Override
    public void onFutureDone(Future<Bitmap> future) {
        synchronized (this) {
            this.mTask = null;
            this.mBitmap = future.get();
            if (this.mState == 4) {
                if (this.mBitmap != null) {
                    GalleryBitmapPool.getInstance().put(this.mBitmap);
                    this.mBitmap = null;
                }
            } else if (future.isCancelled() && this.mBitmap == null) {
                if (this.mState == 1) {
                    this.mTask = submitBitmapTask(this);
                }
            } else {
                this.mState = this.mBitmap == null ? 3 : 2;
                onLoadComplete(this.mBitmap);
            }
        }
    }

    public synchronized void startLoad() {
        if (this.mState == 0) {
            this.mState = 1;
            if (this.mTask == null) {
                this.mTask = submitBitmapTask(this);
            }
        }
    }

    public synchronized void cancelLoad() {
        if (this.mState == 1) {
            this.mState = 0;
            if (this.mTask != null) {
                this.mTask.cancel();
            }
        }
    }

    public synchronized void recycle() {
        this.mState = 4;
        if (this.mBitmap != null) {
            GalleryBitmapPool.getInstance().put(this.mBitmap);
            this.mBitmap = null;
        }
        if (this.mTask != null) {
            this.mTask.cancel();
        }
    }

    public synchronized boolean isRequestInProgress() {
        boolean z;
        synchronized (this) {
            z = this.mState == 1;
        }
        return z;
    }

    public synchronized Bitmap getBitmap() {
        return this.mBitmap;
    }
}
