package com.android.browser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import java.util.Map;

public class Preloader {
    private static Preloader sInstance;
    private final Context mContext;
    private final BrowserWebViewFactory mFactory;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private volatile PreloaderSession mSession = null;

    public static void initialize(Context context) {
        sInstance = new Preloader(context);
    }

    public static Preloader getInstance() {
        return sInstance;
    }

    private Preloader(Context context) {
        this.mContext = context.getApplicationContext();
        this.mFactory = new BrowserWebViewFactory(context);
    }

    private PreloaderSession getSession(String id) {
        if (this.mSession == null) {
            Log.d("browser.preloader", "Create new preload session " + id);
            this.mSession = new PreloaderSession(id);
            WebViewTimersControl.getInstance().onPrerenderStart(this.mSession.getWebView());
            return this.mSession;
        }
        if (this.mSession.mId.equals(id)) {
            Log.d("browser.preloader", "Returning existing preload session " + id);
            return this.mSession;
        }
        Log.d("browser.preloader", "Existing session in progress : " + this.mSession.mId + " returning null.");
        return null;
    }

    private PreloaderSession takeSession(String id) {
        PreloaderSession s = null;
        if (this.mSession != null && this.mSession.mId.equals(id)) {
            s = this.mSession;
            this.mSession = null;
        }
        if (s != null) {
            s.cancelTimeout();
        }
        return s;
    }

    public void handlePreloadRequest(String id, String url, Map<String, String> headers, String searchBoxQuery) {
        PreloaderSession s = getSession(id);
        if (s == null) {
            Log.d("browser.preloader", "Discarding preload request, existing session in progress");
            return;
        }
        s.touch();
        PreloadedTabControl tab = s.getTabControl();
        if (searchBoxQuery != null) {
            tab.loadUrlIfChanged(url, headers);
            tab.setQuery(searchBoxQuery);
        } else {
            tab.loadUrl(url, headers);
        }
    }

    public void cancelSearchBoxPreload(String id) {
        PreloaderSession s = getSession(id);
        if (s == null) {
            return;
        }
        s.touch();
        PreloadedTabControl tab = s.getTabControl();
        tab.searchBoxCancel();
    }

    public void discardPreload(String id) {
        PreloaderSession s = takeSession(id);
        if (s != null) {
            Log.d("browser.preloader", "Discard preload session " + id);
            WebViewTimersControl.getInstance().onPrerenderDone(s != null ? s.getWebView() : null);
            PreloadedTabControl t = s.getTabControl();
            t.destroy();
            return;
        }
        Log.d("browser.preloader", "Ignored discard request " + id);
    }

    public PreloadedTabControl getPreloadedTab(String id) {
        PreloaderSession s = takeSession(id);
        Log.d("browser.preloader", "Showing preload session " + id + "=" + s);
        if (s == null) {
            return null;
        }
        return s.getTabControl();
    }

    private class PreloaderSession {
        private final String mId;
        private final PreloadedTabControl mTabControl;
        private final Runnable mTimeoutTask = new Runnable() {
            @Override
            public void run() {
                Log.d("browser.preloader", "Preload session timeout " + PreloaderSession.this.mId);
                Preloader.this.discardPreload(PreloaderSession.this.mId);
            }
        };

        public PreloaderSession(String id) {
            this.mId = id;
            this.mTabControl = new PreloadedTabControl(new Tab(new PreloadController(Preloader.this.mContext), Preloader.this.mFactory.createWebView(false)));
            touch();
        }

        public void cancelTimeout() {
            Preloader.this.mHandler.removeCallbacks(this.mTimeoutTask);
        }

        public void touch() {
            cancelTimeout();
            Preloader.this.mHandler.postDelayed(this.mTimeoutTask, 30000L);
        }

        public PreloadedTabControl getTabControl() {
            return this.mTabControl;
        }

        public WebView getWebView() {
            Tab t = this.mTabControl.getTab();
            if (t == null) {
                return null;
            }
            return t.getWebView();
        }
    }
}
