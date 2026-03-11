package com.android.browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class PreloadRequestReceiver extends BroadcastReceiver {
    private ConnectivityManager mConnectivityManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("browser.preloader", "received intent " + intent);
        if (!isPreloadEnabledOnCurrentNetwork(context) || !intent.getAction().equals("android.intent.action.PRELOAD")) {
            return;
        }
        handlePreload(context, intent);
    }

    private boolean isPreloadEnabledOnCurrentNetwork(Context context) {
        String preload = BrowserSettings.getInstance().getPreloadEnabled();
        Log.d("browser.preloader", "Preload setting: " + preload);
        if (BrowserSettings.getPreloadAlwaysPreferenceString(context).equals(preload)) {
            return true;
        }
        if (BrowserSettings.getPreloadOnWifiOnlyPreferenceString(context).equals(preload)) {
            boolean onWifi = isOnWifi(context);
            Log.d("browser.preloader", "on wifi:" + onWifi);
            return onWifi;
        }
        return false;
    }

    private boolean isOnWifi(Context context) {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        }
        NetworkInfo ni = this.mConnectivityManager.getActiveNetworkInfo();
        if (ni == null) {
            return false;
        }
        switch (ni.getType()) {
        }
        return false;
    }

    private void handlePreload(Context context, Intent i) {
        Bundle pairs;
        String url = UrlUtils.smartUrlFilter(i.getData());
        String id = i.getStringExtra("preload_id");
        Map<String, String> headers = null;
        if (id == null) {
            Log.d("browser.preloader", "Preload request has no preload_id");
            return;
        }
        if (i.getBooleanExtra("preload_discard", false)) {
            Log.d("browser.preloader", "Got " + id + " preload discard request");
            Preloader.getInstance().discardPreload(id);
            return;
        }
        if (i.getBooleanExtra("searchbox_cancel", false)) {
            Log.d("browser.preloader", "Got " + id + " searchbox cancel request");
            Preloader.getInstance().cancelSearchBoxPreload(id);
            return;
        }
        Log.d("browser.preloader", "Got " + id + " preload request for " + url);
        if (url != null && url.startsWith("http") && (pairs = i.getBundleExtra("com.android.browser.headers")) != null && !pairs.isEmpty()) {
            headers = new HashMap<>();
            for (String key : pairs.keySet()) {
                headers.put(key, pairs.getString(key));
            }
        }
        String sbQuery = i.getStringExtra("searchbox_query");
        if (url == null) {
            return;
        }
        Log.d("browser.preloader", "Preload request(" + id + ", " + url + ", " + headers + ", " + sbQuery + ")");
        Preloader.getInstance().handlePreloadRequest(id, url, headers, sbQuery);
    }
}
