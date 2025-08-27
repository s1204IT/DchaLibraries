package com.mediatek.browser.ext;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebView;

/* loaded from: classes.dex */
public class DefaultBrowserMiscExt implements IBrowserMiscExt {
    @Override // com.mediatek.browser.ext.IBrowserMiscExt
    public void onActivityResult(int i, int i2, Intent intent, Object obj) {
        Log.i("@M_DefaultBrowserMiscExt", "Enter: onActivityResult --default implement");
    }

    @Override // com.mediatek.browser.ext.IBrowserMiscExt
    public void processNetworkNotify(WebView webView, Activity activity, boolean z) {
        Log.i("@M_DefaultBrowserMiscExt", "Enter: processNetworkNotify --default implement");
        if (!z) {
            webView.setNetworkAvailable(false);
        }
    }
}
