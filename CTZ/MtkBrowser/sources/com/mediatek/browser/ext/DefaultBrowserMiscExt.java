package com.mediatek.browser.ext;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebView;

public class DefaultBrowserMiscExt implements IBrowserMiscExt {
    @Override
    public void onActivityResult(int i, int i2, Intent intent, Object obj) {
        Log.i("@M_DefaultBrowserMiscExt", "Enter: onActivityResult --default implement");
    }

    @Override
    public void processNetworkNotify(WebView webView, Activity activity, boolean z) {
        Log.i("@M_DefaultBrowserMiscExt", "Enter: processNetworkNotify --default implement");
        if (!z) {
            webView.setNetworkAvailable(false);
        }
    }
}
