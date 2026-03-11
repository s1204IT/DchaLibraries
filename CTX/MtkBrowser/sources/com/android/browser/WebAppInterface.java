package com.android.browser;

import android.util.Log;
import android.webkit.JavascriptInterface;

public class WebAppInterface {
    private final BrowserWebView mWebView;

    public WebAppInterface(BrowserWebView browserWebView) {
        this.mWebView = browserWebView;
    }

    @JavascriptInterface
    public void recordURL(String str) {
        Log.d("WebAppInterface", "recordURL: " + str);
        this.mWebView.setSiteNavHitURL(str);
    }
}
