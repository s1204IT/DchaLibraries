package com.android.browser;

import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

public class WebViewTimersControl {
    private static WebViewTimersControl sInstance;
    private boolean mBrowserActive;
    private boolean mPrerenderActive;

    public static WebViewTimersControl getInstance() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("WebViewTimersControl.get() called on wrong thread");
        }
        if (sInstance == null) {
            sInstance = new WebViewTimersControl();
        }
        return sInstance;
    }

    private WebViewTimersControl() {
    }

    private void resumeTimers(WebView wv) {
        Log.d("WebViewTimersControl", "Resuming webview timers, view=" + wv);
        if (wv == null) {
            return;
        }
        wv.resumeTimers();
    }

    private void maybePauseTimers(WebView wv) {
        if (this.mBrowserActive || this.mPrerenderActive || wv == null) {
            return;
        }
        Log.d("WebViewTimersControl", "Pausing webview timers, view=" + wv);
        wv.pauseTimers();
    }

    public void onBrowserActivityResume(WebView wv) {
        Log.d("WebViewTimersControl", "onBrowserActivityResume");
        this.mBrowserActive = true;
        resumeTimers(wv);
    }

    public void onBrowserActivityPause(WebView wv) {
        Log.d("WebViewTimersControl", "onBrowserActivityPause");
        this.mBrowserActive = false;
        maybePauseTimers(wv);
    }

    public void onPrerenderStart(WebView wv) {
        Log.d("WebViewTimersControl", "onPrerenderStart");
        this.mPrerenderActive = true;
        resumeTimers(wv);
    }

    public void onPrerenderDone(WebView wv) {
        Log.d("WebViewTimersControl", "onPrerenderDone");
        this.mPrerenderActive = false;
        maybePauseTimers(wv);
    }
}
