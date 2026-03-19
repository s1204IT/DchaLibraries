package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.photos.data.GalleryBitmapPool;

public abstract class BitmapLoader implements FutureListener<Bitmap> {
    private static final int STATE_ERROR = 3;
    private static final int STATE_INIT = 0;
    private static final int STATE_LOADED = 2;
    private static final int STATE_RECYCLED = 4;
    private static final int STATE_REQUESTED = 1;
    private static final String TAG = "Gallery2/BitmapLoader";
    private Bitmap mBitmap;
    private Future<Bitmap> mTask;
    private int mState = 0;
    public boolean mBitmapLoaded = false;

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
        return this.mState == 1;
    }

    public synchronized boolean isRecycled() {
        return this.mState == 4;
    }

    public synchronized Bitmap getBitmap() {
        return this.mBitmap;
    }

    public synchronized boolean isLoadingCompleted() {
        boolean z;
        if (this.mState == 4 || this.mState == 2) {
            z = true;
        } else if (this.mState != 3) {
            z = false;
        }
        return z;
    }
}
