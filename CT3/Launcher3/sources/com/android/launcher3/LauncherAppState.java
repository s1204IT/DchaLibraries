package com.android.launcher3;

import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.ConfigMonitor;
import java.lang.ref.WeakReference;

public class LauncherAppState {
    private static LauncherAppState INSTANCE;
    private static Context sContext;
    private static WeakReference<LauncherProvider> sLauncherProvider;
    private LauncherAccessibilityDelegate mAccessibilityDelegate;
    private final AppFilter mAppFilter;
    private final IconCache mIconCache;
    private InvariantDeviceProfile mInvariantDeviceProfile;
    final LauncherModel mModel;
    private boolean mWallpaperChangedSinceLastCheck;
    private final WidgetPreviewLoader mWidgetCache;

    public static LauncherAppState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LauncherAppState();
        }
        return INSTANCE;
    }

    public static LauncherAppState getInstanceNoCreate() {
        return INSTANCE;
    }

    public Context getContext() {
        return sContext;
    }

    public static void setApplicationContext(Context context) {
        if (sContext != null) {
            Log.w("Launcher", "setApplicationContext called twice! old=" + sContext + " new=" + context);
        }
        sContext = context.getApplicationContext();
    }

    private LauncherAppState() {
        if (sContext == null) {
            throw new IllegalStateException("LauncherAppState inited before app context set");
        }
        Log.v("Launcher", "LauncherAppState inited");
        this.mInvariantDeviceProfile = new InvariantDeviceProfile(sContext);
        this.mIconCache = new IconCache(sContext, this.mInvariantDeviceProfile);
        this.mWidgetCache = new WidgetPreviewLoader(sContext, this.mIconCache);
        this.mAppFilter = AppFilter.loadByName(sContext.getString(R.string.app_filter_class));
        this.mModel = new LauncherModel(this, this.mIconCache, this.mAppFilter);
        LauncherAppsCompat.getInstance(sContext).addOnAppsChangedCallback(this.mModel);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        filter.addAction("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED");
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_ADDED);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_REMOVED);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(LauncherAppsCompat.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        sContext.registerReceiver(this.mModel, filter);
        UserManagerCompat.getInstance(sContext).enableAndResetCache();
        new ConfigMonitor(sContext).register();
        sContext.registerReceiver(new WallpaperChangedReceiver(), new IntentFilter("android.intent.action.WALLPAPER_CHANGED"));
    }

    public void reloadWorkspace() {
        this.mModel.resetLoadedState(false, true);
        this.mModel.startLoaderFromBackground();
    }

    LauncherModel setLauncher(Launcher launcher) {
        LauncherAccessibilityDelegate launcherAccessibilityDelegate = null;
        getLauncherProvider().setLauncherProviderChangeListener(launcher);
        this.mModel.initialize(launcher);
        if (launcher != null && Utilities.ATLEAST_LOLLIPOP) {
            launcherAccessibilityDelegate = new LauncherAccessibilityDelegate(launcher);
        }
        this.mAccessibilityDelegate = launcherAccessibilityDelegate;
        return this.mModel;
    }

    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return this.mAccessibilityDelegate;
    }

    public IconCache getIconCache() {
        return this.mIconCache;
    }

    public LauncherModel getModel() {
        return this.mModel;
    }

    static void setLauncherProvider(LauncherProvider provider) {
        sLauncherProvider = new WeakReference<>(provider);
    }

    public static LauncherProvider getLauncherProvider() {
        return sLauncherProvider.get();
    }

    public WidgetPreviewLoader getWidgetCache() {
        return this.mWidgetCache;
    }

    public void onWallpaperChanged() {
        this.mWallpaperChangedSinceLastCheck = true;
    }

    public boolean hasWallpaperChangedSinceLastCheck() {
        boolean result = this.mWallpaperChangedSinceLastCheck;
        this.mWallpaperChangedSinceLastCheck = false;
        return result;
    }

    public InvariantDeviceProfile getInvariantDeviceProfile() {
        return this.mInvariantDeviceProfile;
    }

    public static boolean isDogfoodBuild() {
        if (FeatureFlags.IS_ALPHA_BUILD) {
            return true;
        }
        return FeatureFlags.IS_DEV_BUILD;
    }
}
