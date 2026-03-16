package com.android.gallery3d.app;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.gallery3d.picasasource.PicasaSource;

public class PackagesMonitor extends BroadcastReceiver {
    public static synchronized int getPackagesVersion(Context context) {
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt("packages-version", 1);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, AsyncService.class);
        context.startService(intent);
    }

    public static class AsyncService extends IntentService {
        public AsyncService() {
            super("GalleryPackagesMonitorAsync");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            PackagesMonitor.onReceiveAsync(this, intent);
        }
    }

    private static void onReceiveAsync(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int version = prefs.getInt("packages-version", 1);
        prefs.edit().putInt("packages-version", version + 1).commit();
        String action = intent.getAction();
        String packageName = intent.getData().getSchemeSpecificPart();
        if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
            PicasaSource.onPackageAdded(context, packageName);
        } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
            PicasaSource.onPackageRemoved(context, packageName);
        } else if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
            PicasaSource.onPackageChanged(context, packageName);
        }
    }
}
