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
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("packages-version", 1);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
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
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.edit().putInt("packages-version", defaultSharedPreferences.getInt("packages-version", 1) + 1).commit();
        if (intent == null) {
            Log.d("Gallery2/PackagesMonitor", "<onReceiveAsync> intent is null, return");
            return;
        }
        String action = intent.getAction();
        String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
        if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
            PicasaSource.onPackageAdded(context, schemeSpecificPart);
        } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
            PicasaSource.onPackageRemoved(context, schemeSpecificPart);
        } else if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
            PicasaSource.onPackageChanged(context, schemeSpecificPart);
        }
    }
}
