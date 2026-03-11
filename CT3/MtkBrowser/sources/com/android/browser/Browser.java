package com.android.browser;

import android.app.Application;
import android.os.Build;
import android.os.SystemProperties;
import android.webkit.CookieSyncManager;
import com.mediatek.browser.ext.IBrowserRegionalPhoneExt;

public class Browser extends Application {
    static final boolean DEBUG;

    static {
        DEBUG = Build.TYPE.equals("eng") ? true : SystemProperties.getBoolean("ro.debug.browser", false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CookieSyncManager.createInstance(this);
        BrowserSettings.initialize(getApplicationContext());
        Preloader.initialize(getApplicationContext());
        IBrowserRegionalPhoneExt browserRegionalPhone = Extensions.getRegionalPhonePlugin(getApplicationContext());
        browserRegionalPhone.updateBookmarks(getApplicationContext());
        BrowserSettings.getInstance().updateSearchEngineSetting();
    }
}
