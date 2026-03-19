package com.android.documentsui;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.drm.DrmManagerClient;
import android.net.Uri;
import android.os.RemoteException;
import com.android.documentsui.base.Lookup;
import com.android.documentsui.clipping.ClipStorage;
import com.android.documentsui.clipping.ClipStore;
import com.android.documentsui.clipping.DocumentClipper;
import com.android.documentsui.roots.ProvidersCache;

public class DocumentsApplication extends Application {
    private static boolean sIsLowRamDevice = false;
    private BroadcastReceiver mCacheReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            if (data == null) {
                DocumentsApplication.this.mProviders.updateAsync(true);
            } else {
                DocumentsApplication.this.mProviders.updatePackageAsync(data.getSchemeSpecificPart());
            }
        }
    };
    private ClipStorage mClipStore;
    private DocumentClipper mClipper;
    private DragAndDropManager mDragAndDropManager;
    private Lookup<String, String> mFileTypeLookup;
    private DrmManagerClient mOmaDrmClient;
    private ProvidersCache mProviders;
    private ThumbnailCache mThumbnailCache;

    public static ProvidersCache getProvidersCache(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mProviders;
    }

    public static DrmManagerClient getDrmClient(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mOmaDrmClient;
    }

    public static ThumbnailCache getThumbnailCache(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mThumbnailCache;
    }

    public static ContentProviderClient acquireUnstableProviderOrThrow(ContentResolver contentResolver, String str) throws RemoteException {
        ContentProviderClient contentProviderClientAcquireUnstableContentProviderClient = contentResolver.acquireUnstableContentProviderClient(str);
        if (contentProviderClientAcquireUnstableContentProviderClient == null) {
            throw new RemoteException("Failed to acquire provider for " + str);
        }
        if (!sIsLowRamDevice && !"com.android.externalstorage.documents".equals(str)) {
            contentProviderClientAcquireUnstableContentProviderClient.setDetectNotResponding(20000L);
        }
        return contentProviderClientAcquireUnstableContentProviderClient;
    }

    public static DocumentClipper getDocumentClipper(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mClipper;
    }

    public static ClipStore getClipStore(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mClipStore;
    }

    public static DragAndDropManager getDragAndDropManager(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mDragAndDropManager;
    }

    public static Lookup<String, String> getFileTypeLookup(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mFileTypeLookup;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ActivityManager activityManager = (ActivityManager) getSystemService("activity");
        int memoryClass = activityManager.getMemoryClass() * 1024 * 1024;
        sIsLowRamDevice = activityManager.isLowRamDevice();
        this.mProviders = new ProvidersCache(this);
        this.mProviders.updateAsync(false);
        if (DocumentsFeatureOption.IS_SUPPORT_DRM) {
            this.mOmaDrmClient = new DrmManagerClient(this);
        }
        this.mThumbnailCache = new ThumbnailCache(memoryClass / 4);
        this.mClipStore = new ClipStorage(ClipStorage.prepareStorage(getCacheDir()), getSharedPreferences("ClipStoragePref", 0));
        this.mClipper = DocumentClipper.create(this, this.mClipStore);
        this.mDragAndDropManager = DragAndDropManager.create(this, this.mClipper);
        this.mFileTypeLookup = new FileTypeMap(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        intentFilter.addDataScheme("package");
        registerReceiver(this.mCacheReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.LOCALE_CHANGED");
        registerReceiver(this.mCacheReceiver, intentFilter2);
    }

    @Override
    public void onTrimMemory(int i) {
        super.onTrimMemory(i);
        this.mThumbnailCache.onTrimMemory(i);
    }
}
