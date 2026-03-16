package com.android.gallery3d.app;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.gadget.WidgetUtils;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.UsageStatistics;
import java.io.File;

public class GalleryAppImpl extends Application implements GalleryApp {
    private DataManager mDataManager;
    private DownloadCache mDownloadCache;
    private ImageCacheService mImageCacheService;
    private Object mLock = new Object();
    private ThreadPool mThreadPool;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeAsyncTask();
        GalleryUtils.initialize(this);
        WidgetUtils.initialize(this);
        PicasaSource.initialize(this);
        UsageStatistics.initialize(this);
    }

    @Override
    public Context getAndroidContext() {
        return this;
    }

    @Override
    public synchronized DataManager getDataManager() {
        if (this.mDataManager == null) {
            this.mDataManager = new DataManager(this);
            this.mDataManager.initializeSourceMap();
        }
        return this.mDataManager;
    }

    @Override
    public ImageCacheService getImageCacheService() {
        ImageCacheService imageCacheService;
        synchronized (this.mLock) {
            if (this.mImageCacheService == null) {
                this.mImageCacheService = new ImageCacheService(getAndroidContext());
            }
            imageCacheService = this.mImageCacheService;
        }
        return imageCacheService;
    }

    @Override
    public synchronized ThreadPool getThreadPool() {
        if (this.mThreadPool == null) {
            this.mThreadPool = new ThreadPool();
        }
        return this.mThreadPool;
    }

    @Override
    public synchronized DownloadCache getDownloadCache() {
        if (this.mDownloadCache == null) {
            File cacheDir = new File(getExternalCacheDir(), "download");
            if (!cacheDir.isDirectory()) {
                cacheDir.mkdirs();
            }
            if (!cacheDir.isDirectory()) {
                throw new RuntimeException("fail to create: " + cacheDir.getAbsolutePath());
            }
            this.mDownloadCache = new DownloadCache(this, cacheDir, 67108864L);
        }
        return this.mDownloadCache;
    }

    private void initializeAsyncTask() {
        try {
            Class.forName(AsyncTask.class.getName());
        } catch (ClassNotFoundException e) {
        }
    }
}
