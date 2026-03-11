package com.android.systemui.shared.recents.model;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.android.systemui.shared.system.ActivityManagerWrapper;

class BackgroundTaskLoader implements Runnable {
    private boolean mCancelled;
    private Context mContext;
    private final IconLoader mIconLoader;
    private final TaskResourceLoadQueue mLoadQueue;
    private final Handler mLoadThreadHandler;
    private final OnIdleChangedListener mOnIdleChangedListener;
    private boolean mStarted;
    private boolean mWaitingOnLoadQueue;
    static String TAG = "BackgroundTaskLoader";
    static boolean DEBUG = false;
    private final Handler mMainThreadHandler = new Handler();
    private final HandlerThread mLoadThread = new HandlerThread("Recents-TaskResourceLoader", 10);

    interface OnIdleChangedListener {
        void onIdleChanged(boolean z);
    }

    public BackgroundTaskLoader(TaskResourceLoadQueue loadQueue, IconLoader iconLoader, OnIdleChangedListener onIdleChangedListener) {
        this.mLoadQueue = loadQueue;
        this.mIconLoader = iconLoader;
        this.mOnIdleChangedListener = onIdleChangedListener;
        this.mLoadThread.start();
        this.mLoadThreadHandler = new Handler(this.mLoadThread.getLooper());
    }

    void start(Context context) {
        this.mContext = context;
        this.mCancelled = false;
        if (!this.mStarted) {
            this.mStarted = true;
            this.mLoadThreadHandler.post(this);
        } else {
            synchronized (this.mLoadThread) {
                this.mLoadThread.notifyAll();
            }
        }
    }

    void stop() {
        this.mCancelled = true;
        if (this.mWaitingOnLoadQueue) {
            this.mContext = null;
        }
    }

    @Override
    public void run() {
        while (true) {
            if (this.mCancelled) {
                this.mContext = null;
                synchronized (this.mLoadThread) {
                    try {
                        this.mLoadThread.wait();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            } else {
                processLoadQueueItem();
                if (!this.mCancelled && this.mLoadQueue.isEmpty()) {
                    synchronized (this.mLoadQueue) {
                        try {
                            this.mWaitingOnLoadQueue = true;
                            this.mMainThreadHandler.post(new Runnable() {
                                @Override
                                public final void run() {
                                    this.f$0.mOnIdleChangedListener.onIdleChanged(true);
                                }
                            });
                            this.mLoadQueue.wait();
                            this.mMainThreadHandler.post(new Runnable() {
                                @Override
                                public final void run() {
                                    this.f$0.mOnIdleChangedListener.onIdleChanged(false);
                                }
                            });
                            this.mWaitingOnLoadQueue = false;
                        } catch (InterruptedException ie2) {
                            ie2.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void processLoadQueueItem() {
        final Task t = this.mLoadQueue.nextTask();
        if (t != null) {
            final Drawable icon = this.mIconLoader.getIcon(t);
            if (DEBUG) {
                Log.d(TAG, "Loading thumbnail: " + t.key);
            }
            final ThumbnailData thumbnailData = ActivityManagerWrapper.getInstance().getTaskThumbnail(t.key.id, true);
            if (!this.mCancelled) {
                this.mMainThreadHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        t.notifyTaskDataLoaded(thumbnailData, icon);
                    }
                });
            }
        }
    }
}
