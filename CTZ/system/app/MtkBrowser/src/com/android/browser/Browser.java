package com.android.browser;

import android.app.Application;
import android.os.Build;
import android.os.SystemProperties;
import com.mediatek.custom.CustomProperties;

/* loaded from: classes.dex */
public class Browser extends Application {
    public static final boolean DEBUG;
    public static final boolean ENGONLY;
    public static final String HEADER = "X_WAP_PROFILE";
    public static final String UAPROF;

    static {
        DEBUG = !Build.TYPE.equals("user") ? true : SystemProperties.getBoolean("ro.mtk_browser_debug_enablelog", false);
        ENGONLY = Build.TYPE.equals("eng") ? true : SystemProperties.getBoolean("ro.mtk_browser_debug_enablelog", false);
        UAPROF = CustomProperties.getString("browser", "UAProfileURL");
    }

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        BrowserSettings.initialize(getApplicationContext());
        Preloader.initialize(getApplicationContext());
        Extensions.getRegionalPhonePlugin(getApplicationContext()).updateBookmarks(getApplicationContext());
        BrowserSettings.getInstance().updateSearchEngineSetting();
    }
}
