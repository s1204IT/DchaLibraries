package com.android.browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import java.util.HashMap;

public class PreloadRequestReceiver extends BroadcastReceiver {
    private ConnectivityManager mConnectivityManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("browser.preloader", "received intent " + intent);
        if (isPreloadEnabledOnCurrentNetwork(context) && intent.getAction().equals("com.android.browser.intent.action.PRELOAD")) {
            handlePreload(context, intent);
        }
    }

    private boolean isPreloadEnabledOnCurrentNetwork(Context context) {
        String preloadEnabled = BrowserSettings.getInstance().getPreloadEnabled();
        Log.d("browser.preloader", "Preload setting: " + preloadEnabled);
        if (BrowserSettings.getPreloadAlwaysPreferenceString(context).equals(preloadEnabled)) {
            return true;
        }
        if (BrowserSettings.getPreloadOnWifiOnlyPreferenceString(context).equals(preloadEnabled)) {
            boolean zIsOnWifi = isOnWifi(context);
            Log.d("browser.preloader", "on wifi:" + zIsOnWifi);
            return zIsOnWifi;
        }
        return false;
    }

    private boolean isOnWifi(Context context) {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        }
        NetworkInfo activeNetworkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null) {
            return false;
        }
        switch (activeNetworkInfo.getType()) {
        }
        return false;
    }

    private void handlePreload(Context context, Intent intent) {
        HashMap map;
        Bundle bundleExtra;
        String strSmartUrlFilter = UrlUtils.smartUrlFilter(intent.getData());
        String stringExtra = intent.getStringExtra("preload_id");
        if (stringExtra == null) {
            Log.d("browser.preloader", "Preload request has no preload_id");
            return;
        }
        if (intent.getBooleanExtra("preload_discard", false)) {
            Log.d("browser.preloader", "Got " + stringExtra + " preload discard request");
            Preloader.getInstance().discardPreload(stringExtra);
            return;
        }
        if (intent.getBooleanExtra("searchbox_cancel", false)) {
            Log.d("browser.preloader", "Got " + stringExtra + " searchbox cancel request");
            Preloader.getInstance().cancelSearchBoxPreload(stringExtra);
            return;
        }
        Log.d("browser.preloader", "Got " + stringExtra + " preload request for " + strSmartUrlFilter);
        if (strSmartUrlFilter != null && strSmartUrlFilter.startsWith("http") && (bundleExtra = intent.getBundleExtra("com.android.browser.headers")) != null && !bundleExtra.isEmpty()) {
            map = new HashMap();
            for (String str : bundleExtra.keySet()) {
                map.put(str, bundleExtra.getString(str));
            }
        } else {
            map = null;
        }
        String stringExtra2 = intent.getStringExtra("searchbox_query");
        if (strSmartUrlFilter != null) {
            Log.d("browser.preloader", "Preload request(" + stringExtra + ", " + strSmartUrlFilter + ", " + map + ", " + stringExtra2 + ")");
            Preloader.getInstance().handlePreloadRequest(stringExtra, strSmartUrlFilter, map, stringExtra2);
        }
    }
}
