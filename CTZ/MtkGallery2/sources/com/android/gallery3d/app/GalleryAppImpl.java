package com.android.gallery3d.app;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.gadget.WidgetService;
import com.android.gallery3d.gadget.WidgetUtils;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.CacheManager;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.UsageStatistics;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallery3d.util.GalleryPluginUtils;
import com.mediatek.galleryportable.IntentActionUtils;
import java.io.File;

public class GalleryAppImpl extends Application implements GalleryApp {
    private DataManager mDataManager;
    private DownloadCache mDownloadCache;
    private ImageCacheService mImageCacheService;
    private Object mLock = new Object();
    private BroadcastReceiver mStorageReceiver;
    private ThreadPool mThreadPool;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeAsyncTask();
        GalleryUtils.initialize(this);
        WidgetUtils.initialize(this);
        PicasaSource.initialize(this);
        UsageStatistics.initialize(this);
        FeatureManager.setup(getAndroidContext());
        GalleryPluginUtils.initialize(this);
        registerStorageReceiver();
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
            File externalCacheDir = FeatureHelper.getExternalCacheDir(this);
            if (externalCacheDir == null) {
                Log.d("Gallery2/GalleryAppImpl", "<getDownloadCache> failed to get cache dir");
                return null;
            }
            File file = new File(externalCacheDir, "download");
            if (!file.isDirectory()) {
                file.mkdirs();
            }
            if (!file.isDirectory()) {
                throw new RuntimeException("fail to create: " + file.getAbsolutePath());
            }
            this.mDownloadCache = new DownloadCache(this, file, 67108864L);
        }
        return this.mDownloadCache;
    }

    private void initializeAsyncTask() {
        try {
            Class.forName(AsyncTask.class.getName());
        } catch (ClassNotFoundException e) {
        }
    }

    private void registerStorageReceiver() {
        Log.d("Gallery2/GalleryAppImpl", ">> registerStorageReceiver");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_EJECT");
        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction(IntentActionUtils.getMediaUnsharedAction());
        intentFilter.addDataScheme("file");
        this.mStorageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MediaSetUtils.refreshBucketId();
                String action = intent.getAction();
                Log.d("Gallery2/GalleryAppImpl", "BroadcastReceiver onReceive : action=" + action);
                if (IntentActionUtils.getMediaUnsharedAction().equals(action)) {
                    context.startService(new Intent(context, (Class<?>) WidgetService.class));
                }
                GalleryAppImpl.this.handleStorageIntentAsync(intent);
            }
        };
        registerReceiver(this.mStorageReceiver, intentFilter);
        Log.d("Gallery2/GalleryAppImpl", "<< registerStorageReceiver: receiver registered");
    }

    private void handleStorageIntentAsync(final Intent intent) {
        new Thread() {
            @Override
            public void run() {
                String action = intent.getAction();
                String path = "";
                if (intent.getData() != null) {
                    path = intent.getData().getPath();
                }
                String defaultPath = FeatureHelper.getDefaultPath();
                Log.d("Gallery2/GalleryAppImpl", "storage receiver: action=" + action);
                Log.d("Gallery2/GalleryAppImpl", "intent path=" + path + ", default path=" + defaultPath);
                if (path == null || !path.equalsIgnoreCase(defaultPath)) {
                    Log.w("Gallery2/GalleryAppImpl", "ejecting storage is not cache storage!!");
                    return;
                }
                if ("android.intent.action.MEDIA_EJECT".equals(action)) {
                    Log.d("Gallery2/GalleryAppImpl", "-> closing CacheManager");
                    CacheManager.storageStateChanged(false);
                    Log.d("Gallery2/GalleryAppImpl", "<- closing CacheManager");
                    if (GalleryAppImpl.this.mImageCacheService != null) {
                        Log.d("Gallery2/GalleryAppImpl", "-> closing cache service");
                        GalleryAppImpl.this.mImageCacheService.closeCache();
                        Log.d("Gallery2/GalleryAppImpl", "<- closing cache service");
                        return;
                    }
                    return;
                }
                if ("android.intent.action.MEDIA_MOUNTED".equals(action)) {
                    Log.d("Gallery2/GalleryAppImpl", "-> opening CacheManager");
                    CacheManager.storageStateChanged(true);
                    Log.d("Gallery2/GalleryAppImpl", "<- opening CacheManager");
                    if (GalleryAppImpl.this.mImageCacheService != null) {
                        Log.d("Gallery2/GalleryAppImpl", "-> opening cache service");
                        GalleryAppImpl.this.mImageCacheService.openCache();
                        Log.d("Gallery2/GalleryAppImpl", "<- opening cache service");
                        return;
                    }
                    return;
                }
                Log.w("Gallery2/GalleryAppImpl", "undesired action '" + action + "' for storage receiver!");
            }
        }.start();
    }
}
