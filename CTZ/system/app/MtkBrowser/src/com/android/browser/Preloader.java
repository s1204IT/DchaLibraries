package com.android.browser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import java.util.Map;

/* loaded from: classes.dex */
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

    private PreloaderSession getSession(String str) {
        if (this.mSession == null) {
            Log.d("browser.preloader", "Create new preload session " + str);
            this.mSession = new PreloaderSession(str);
            WebViewTimersControl.getInstance().onPrerenderStart(this.mSession.getWebView());
            return this.mSession;
        }
        if (this.mSession.mId.equals(str)) {
            Log.d("browser.preloader", "Returning existing preload session " + str);
            return this.mSession;
        }
        Log.d("browser.preloader", "Existing session in progress : " + this.mSession.mId + " returning null.");
        return null;
    }

    private PreloaderSession takeSession(String str) {
        PreloaderSession preloaderSession;
        if (this.mSession != null && this.mSession.mId.equals(str)) {
            preloaderSession = this.mSession;
            this.mSession = null;
        } else {
            preloaderSession = null;
        }
        if (preloaderSession != null) {
            preloaderSession.cancelTimeout();
        }
        return preloaderSession;
    }

    public void handlePreloadRequest(String str, String str2, Map<String, String> map, String str3) {
        PreloaderSession session = getSession(str);
        if (session == null) {
            Log.d("browser.preloader", "Discarding preload request, existing session in progress");
            return;
        }
        session.touch();
        PreloadedTabControl tabControl = session.getTabControl();
        if (str3 != null) {
            tabControl.loadUrlIfChanged(str2, map);
            tabControl.setQuery(str3);
        } else {
            tabControl.loadUrl(str2, map);
        }
    }

    public void cancelSearchBoxPreload(String str) {
        PreloaderSession session = getSession(str);
        if (session != null) {
            session.touch();
            session.getTabControl().searchBoxCancel();
        }
    }

    public void discardPreload(String str) {
        PreloaderSession preloaderSessionTakeSession = takeSession(str);
        if (preloaderSessionTakeSession != null) {
            Log.d("browser.preloader", "Discard preload session " + str);
            WebViewTimersControl.getInstance().onPrerenderDone(preloaderSessionTakeSession == null ? null : preloaderSessionTakeSession.getWebView());
            preloaderSessionTakeSession.getTabControl().destroy();
            return;
        }
        Log.d("browser.preloader", "Ignored discard request " + str);
    }

    public PreloadedTabControl getPreloadedTab(String str) {
        PreloaderSession preloaderSessionTakeSession = takeSession(str);
        Log.d("browser.preloader", "Showing preload session " + str + "=" + preloaderSessionTakeSession);
        if (preloaderSessionTakeSession == null) {
            return null;
        }
        return preloaderSessionTakeSession.getTabControl();
    }

    private class PreloaderSession {
        private final String mId;
        private final PreloadedTabControl mTabControl;
        private final Runnable mTimeoutTask = new Runnable() { // from class: com.android.browser.Preloader.PreloaderSession.1
            @Override // java.lang.Runnable
            public void run() {
                Log.d("browser.preloader", "Preload session timeout " + PreloaderSession.this.mId);
                Preloader.this.discardPreload(PreloaderSession.this.mId);
            }
        };

        public PreloaderSession(String str) {
            this.mId = str;
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
            Tab tab = this.mTabControl.getTab();
            if (tab == null) {
                return null;
            }
            return tab.getWebView();
        }
    }
}
