package com.android.browser;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebView;

public class BrowserWebViewFactory implements WebViewFactory {
    private final Context mContext;

    public BrowserWebViewFactory(Context context) {
        this.mContext = context;
    }

    protected WebView instantiateWebView(AttributeSet attrs, int defStyle, boolean privateBrowsing) {
        return new BrowserWebView(this.mContext, attrs, defStyle, privateBrowsing);
    }

    @Override
    public WebView createWebView(boolean privateBrowsing) {
        WebView w = instantiateWebView(null, android.R.attr.webViewStyle, privateBrowsing);
        initWebViewSettings(w);
        return w;
    }

    protected void initWebViewSettings(WebView w) {
        w.setScrollbarFadingEnabled(true);
        w.setScrollBarStyle(33554432);
        w.setMapTrackballToArrowKeys(false);
        w.getSettings().setBuiltInZoomControls(true);
        PackageManager pm = this.mContext.getPackageManager();
        boolean supportsMultiTouch = pm.hasSystemFeature("android.hardware.touchscreen.multitouch") || pm.hasSystemFeature("android.hardware.faketouch.multitouch.distinct");
        w.getSettings().setDisplayZoomControls(supportsMultiTouch ? false : true);
        BrowserSettings s = BrowserSettings.getInstance();
        s.startManagingSettings(w.getSettings());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(w, cookieManager.acceptCookie());
        if (Build.VERSION.SDK_INT >= 19) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }
}
