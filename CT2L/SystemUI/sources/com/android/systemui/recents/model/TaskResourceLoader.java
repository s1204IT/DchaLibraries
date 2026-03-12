package com.android.systemui.recents.model;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;

class TaskResourceLoader implements Runnable {
    DrawableLruCache mApplicationIconCache;
    boolean mCancelled;
    Context mContext;
    BitmapDrawable mDefaultApplicationIcon;
    Bitmap mDefaultThumbnail;
    TaskResourceLoadQueue mLoadQueue;
    Handler mLoadThreadHandler;
    SystemServicesProxy mSystemServicesProxy;
    BitmapLruCache mThumbnailCache;
    boolean mWaitingOnLoadQueue;
    static String TAG = "TaskResourceLoader";
    static boolean DEBUG = false;
    Handler mMainThreadHandler = new Handler();
    HandlerThread mLoadThread = new HandlerThread("Recents-TaskResourceLoader", 10);

    public TaskResourceLoader(TaskResourceLoadQueue loadQueue, DrawableLruCache applicationIconCache, BitmapLruCache thumbnailCache, Bitmap defaultThumbnail, BitmapDrawable defaultApplicationIcon) {
        this.mLoadQueue = loadQueue;
        this.mApplicationIconCache = applicationIconCache;
        this.mThumbnailCache = thumbnailCache;
        this.mDefaultThumbnail = defaultThumbnail;
        this.mDefaultApplicationIcon = defaultApplicationIcon;
        this.mLoadThread.start();
        this.mLoadThreadHandler = new Handler(this.mLoadThread.getLooper());
        this.mLoadThreadHandler.post(this);
    }

    void start(Context context) {
        this.mContext = context;
        this.mCancelled = false;
        this.mSystemServicesProxy = new SystemServicesProxy(context);
        synchronized (this.mLoadThread) {
            this.mLoadThread.notifyAll();
        }
    }

    void stop() {
        this.mCancelled = true;
        this.mSystemServicesProxy = null;
        if (this.mWaitingOnLoadQueue) {
            this.mContext = null;
        }
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
                RecentsConfiguration config = RecentsConfiguration.getInstance();
                SystemServicesProxy ssp = this.mSystemServicesProxy;
                if (ssp != null && (t = this.mLoadQueue.nextTask()) != null) {
                    Drawable cachedIcon = this.mApplicationIconCache.get(t.key);
                    Bitmap cachedThumbnail = this.mThumbnailCache.get(t.key);
                    if (cachedIcon == null) {
                        cachedIcon = getTaskDescriptionIcon(t.key, t.icon, t.iconFilename, ssp, this.mContext.getResources());
                        if (cachedIcon == null && (info = ssp.getActivityInfo(t.key.baseIntent.getComponent(), t.key.userId)) != null) {
                            if (DEBUG) {
                                Log.d(TAG, "Loading icon: " + t.key);
                            }
                            cachedIcon = ssp.getActivityIcon(info, t.key.userId);
                        }
                        if (cachedIcon == null) {
                            cachedIcon = this.mDefaultApplicationIcon;
                        }
                        this.mApplicationIconCache.put(t.key, cachedIcon);
                    }
                    if (cachedThumbnail == null) {
                        if (config.svelteLevel < 3) {
                            if (DEBUG) {
                                Log.d(TAG, "Loading thumbnail: " + t.key);
                            }
                            cachedThumbnail = ssp.getTaskThumbnail(t.key.id);
                        }
                        if (cachedThumbnail == null) {
                            cachedThumbnail = this.mDefaultThumbnail;
                        }
                        if (config.svelteLevel < 1) {
                            this.mThumbnailCache.put(t.key, cachedThumbnail);
                        }
                    }
                    if (!this.mCancelled) {
                        final Drawable newIcon = cachedIcon;
                        final Bitmap newThumbnail = cachedThumbnail == this.mDefaultThumbnail ? null : cachedThumbnail;
                        this.mMainThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                t.notifyTaskDataLoaded(newThumbnail, newIcon);
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

    Drawable getTaskDescriptionIcon(Task.TaskKey taskKey, Bitmap iconBitmap, String iconFilename, SystemServicesProxy ssp, Resources res) {
        Bitmap tdIcon = iconBitmap != null ? iconBitmap : ActivityManager.TaskDescription.loadTaskDescriptionIcon(iconFilename);
        if (tdIcon != null) {
            return ssp.getBadgedIcon(new BitmapDrawable(res, tdIcon), taskKey.userId);
        }
        return null;
    }
}
