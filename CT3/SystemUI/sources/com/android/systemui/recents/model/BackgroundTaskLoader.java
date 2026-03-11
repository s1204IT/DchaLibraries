package com.android.systemui.recents.model;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;

class BackgroundTaskLoader implements Runnable {
    boolean mCancelled;
    Context mContext;
    BitmapDrawable mDefaultIcon;
    Bitmap mDefaultThumbnail;
    TaskKeyLruCache<Drawable> mIconCache;
    TaskResourceLoadQueue mLoadQueue;
    Handler mLoadThreadHandler;
    TaskKeyLruCache<ThumbnailData> mThumbnailCache;
    boolean mWaitingOnLoadQueue;
    static String TAG = "TaskResourceLoader";
    static boolean DEBUG = false;
    Handler mMainThreadHandler = new Handler();
    HandlerThread mLoadThread = new HandlerThread("Recents-TaskResourceLoader", 10);

    public BackgroundTaskLoader(TaskResourceLoadQueue loadQueue, TaskKeyLruCache<Drawable> iconCache, TaskKeyLruCache<ThumbnailData> thumbnailCache, Bitmap defaultThumbnail, BitmapDrawable defaultIcon) {
        this.mLoadQueue = loadQueue;
        this.mIconCache = iconCache;
        this.mThumbnailCache = thumbnailCache;
        this.mDefaultThumbnail = defaultThumbnail;
        this.mDefaultIcon = defaultIcon;
        this.mLoadThread.start();
        this.mLoadThreadHandler = new Handler(this.mLoadThread.getLooper());
        this.mLoadThreadHandler.post(this);
    }

    void start(Context context) {
        this.mContext = context;
        this.mCancelled = false;
        synchronized (this.mLoadThread) {
            this.mLoadThread.notifyAll();
        }
    }

    void stop() {
        this.mCancelled = true;
        if (!this.mWaitingOnLoadQueue) {
            return;
        }
        this.mContext = null;
    }

    @Override
    public void run() {
        final Task t;
        ActivityInfo info;
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
                RecentsConfiguration config = Recents.getConfiguration();
                SystemServicesProxy ssp = Recents.getSystemServices();
                if (ssp != null && (t = this.mLoadQueue.nextTask()) != null) {
                    Drawable cachedIcon = this.mIconCache.get(t.key);
                    ThumbnailData cachedThumbnailData = this.mThumbnailCache.get(t.key);
                    if (cachedIcon == null) {
                        cachedIcon = ssp.getBadgedTaskDescriptionIcon(t.taskDescription, t.key.userId, this.mContext.getResources());
                        if (cachedIcon == null && (info = ssp.getActivityInfo(t.key.getComponent(), t.key.userId)) != null) {
                            if (DEBUG) {
                                Log.d(TAG, "Loading icon: " + t.key);
                            }
                            cachedIcon = ssp.getBadgedActivityIcon(info, t.key.userId);
                        }
                        if (cachedIcon == null) {
                            cachedIcon = this.mDefaultIcon;
                        }
                        this.mIconCache.put(t.key, cachedIcon);
                    }
                    if (cachedThumbnailData == null) {
                        if (config.svelteLevel < 3) {
                            if (DEBUG) {
                                Log.d(TAG, "Loading thumbnail: " + t.key);
                            }
                            cachedThumbnailData = ssp.getTaskThumbnail(t.key.id);
                        }
                        if (cachedThumbnailData.thumbnail == null) {
                            cachedThumbnailData.thumbnail = this.mDefaultThumbnail;
                        }
                        if (config.svelteLevel < 1) {
                            this.mThumbnailCache.put(t.key, cachedThumbnailData);
                        }
                    }
                    if (!this.mCancelled) {
                        final Drawable newIcon = cachedIcon;
                        final ThumbnailData newThumbnailData = cachedThumbnailData;
                        this.mMainThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                t.notifyTaskDataLoaded(newThumbnailData.thumbnail, newIcon, newThumbnailData.thumbnailInfo);
                            }
                        });
                    }
                }
                if (!this.mCancelled && this.mLoadQueue.isEmpty()) {
                    synchronized (this.mLoadQueue) {
                        try {
                            this.mWaitingOnLoadQueue = true;
                            this.mLoadQueue.wait();
                            this.mWaitingOnLoadQueue = false;
                        } catch (InterruptedException ie2) {
                            ie2.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
