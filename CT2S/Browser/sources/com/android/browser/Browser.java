package com.android.browser;

import android.app.Application;
import android.webkit.CookieSyncManager;

public class Browser extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CookieSyncManager.createInstance(this);
        BrowserSettings.initialize(getApplicationContext());
        Preloader.initialize(getApplicationContext());
    }
}
