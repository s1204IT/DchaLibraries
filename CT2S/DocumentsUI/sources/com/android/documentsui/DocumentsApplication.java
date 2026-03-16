package com.android.documentsui;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.Uri;
import android.os.RemoteException;

public class DocumentsApplication extends Application {
    private BroadcastReceiver mCacheReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            if (data == null) {
                DocumentsApplication.this.mRoots.updateAsync();
            } else {
                String packageName = data.getSchemeSpecificPart();
                DocumentsApplication.this.mRoots.updatePackageAsync(packageName);
            }
        }
    };
    private RootsCache mRoots;
    private ThumbnailCache mThumbnails;
    private Point mThumbnailsSize;

    public static RootsCache getRootsCache(Context context) {
        return ((DocumentsApplication) context.getApplicationContext()).mRoots;
    }

    public static ThumbnailCache getThumbnailsCache(Context context, Point size) {
        DocumentsApplication app = (DocumentsApplication) context.getApplicationContext();
        ThumbnailCache thumbnails = app.mThumbnails;
        if (!size.equals(app.mThumbnailsSize)) {
            thumbnails.evictAll();
            app.mThumbnailsSize = size;
        }
        return thumbnails;
    }

    public static ContentProviderClient acquireUnstableProviderOrThrow(ContentResolver resolver, String authority) throws RemoteException {
        ContentProviderClient client = resolver.acquireUnstableContentProviderClient(authority);
        if (client == null) {
            throw new RemoteException("Failed to acquire provider for " + authority);
        }
        client.setDetectNotResponding(20000L);
        return client;
    }

    @Override
    public void onCreate() {
        ActivityManager am = (ActivityManager) getSystemService("activity");
        int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
        this.mRoots = new RootsCache(this);
        this.mRoots.updateAsync();
        this.mThumbnails = new ThumbnailCache(memoryClassBytes / 4);
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        packageFilter.addDataScheme("package");
        registerReceiver(this.mCacheReceiver, packageFilter);
        IntentFilter localeFilter = new IntentFilter();
        localeFilter.addAction("android.intent.action.LOCALE_CHANGED");
        registerReceiver(this.mCacheReceiver, localeFilter);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= 60) {
            this.mThumbnails.evictAll();
        } else if (level >= 40) {
            this.mThumbnails.trimToSize(this.mThumbnails.size() / 2);
        }
    }
}
