package com.android.launcher2;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.database.ContentObserver;
import android.os.Handler;
import com.android.launcher.R;
import com.android.launcher2.LauncherSettings;
import com.android.launcher2.WidgetPreviewLoader;
import java.lang.ref.WeakReference;

public class LauncherApplication extends Application {
    private static boolean sIsScreenLarge;
    private static int sLongPressTimeout = 300;
    private static float sScreenDensity;
    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            LauncherApplication.this.mModel.resetLoadedState(false, true);
            LauncherApplication.this.mModel.startLoaderFromBackground();
        }
    };
    private IconCache mIconCache;
    WeakReference<LauncherProvider> mLauncherProvider;
    private LauncherModel mModel;
    private WidgetPreviewLoader.CacheDb mWidgetPreviewCacheDb;

    @Override
    public void onCreate() {
        super.onCreate();
        sIsScreenLarge = getResources().getBoolean(R.bool.is_large_screen);
        sScreenDensity = getResources().getDisplayMetrics().density;
        recreateWidgetPreviewDb();
        this.mIconCache = new IconCache(this);
        this.mModel = new LauncherModel(this, this.mIconCache);
        LauncherApps launcherApps = (LauncherApps) getSystemService("launcherapps");
        launcherApps.registerCallback(this.mModel.getLauncherAppsCallback());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        registerReceiver(this.mModel, filter);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED");
        registerReceiver(this.mModel, filter2);
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction("android.search.action.SEARCHABLES_CHANGED");
        registerReceiver(this.mModel, filter3);
        IntentFilter filter4 = new IntentFilter();
        filter4.addAction("com.android.launcher.action.COMPLETE_SETUP_DEVICE_OWNER");
        registerReceiver(this.mModel, filter4);
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true, this.mFavoritesObserver);
    }

    public void recreateWidgetPreviewDb() {
        this.mWidgetPreviewCacheDb = new WidgetPreviewLoader.CacheDb(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(this.mModel);
        ContentResolver resolver = getContentResolver();
        resolver.unregisterContentObserver(this.mFavoritesObserver);
    }

    LauncherModel setLauncher(Launcher launcher) {
        this.mModel.initialize(launcher);
        return this.mModel;
    }

    IconCache getIconCache() {
        return this.mIconCache;
    }

    LauncherModel getModel() {
        return this.mModel;
    }

    WidgetPreviewLoader.CacheDb getWidgetPreviewCacheDb() {
        return this.mWidgetPreviewCacheDb;
    }

    void setLauncherProvider(LauncherProvider provider) {
        this.mLauncherProvider = new WeakReference<>(provider);
    }

    LauncherProvider getLauncherProvider() {
        return this.mLauncherProvider.get();
    }

    public static String getSharedPreferencesKey() {
        return "com.android.launcher2.prefs";
    }

    public static boolean isScreenLarge() {
        return sIsScreenLarge;
    }

    public static boolean isScreenLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == 2;
    }

    public static float getScreenDensity() {
        return sScreenDensity;
    }

    public static int getLongPressTimeout() {
        return sLongPressTimeout;
    }
}
