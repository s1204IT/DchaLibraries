package com.android.systemui.recents;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import com.android.systemui.R;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.views.DockState;

public class RecentsConfiguration {
    public boolean dragToSplitEnabled;
    public boolean fakeShadows;
    public boolean isGridEnabled;
    public final boolean isLargeScreen;
    public boolean isLowRamDevice;
    public final boolean isXLargeScreen;
    private final Context mAppContext;
    public RecentsActivityLaunchState mLaunchState = new RecentsActivityLaunchState();
    public final int smallestWidth;
    public int svelteLevel;

    public RecentsConfiguration(Context context) {
        SystemServicesProxy systemServices = Recents.getSystemServices();
        this.mAppContext = context.getApplicationContext();
        Resources resources = this.mAppContext.getResources();
        this.fakeShadows = resources.getBoolean(R.bool.config_recents_fake_shadows);
        this.svelteLevel = resources.getInteger(R.integer.recents_svelte_level);
        this.isGridEnabled = SystemProperties.getBoolean("ro.recents.grid", false);
        this.isLowRamDevice = ActivityManager.isLowRamDeviceStatic();
        this.dragToSplitEnabled = !this.isLowRamDevice;
        float f = context.getResources().getDisplayMetrics().density;
        this.smallestWidth = systemServices.getDeviceSmallestWidth();
        this.isLargeScreen = this.smallestWidth >= ((int) (600.0f * f));
        this.isXLargeScreen = this.smallestWidth >= ((int) (f * 720.0f));
    }

    public RecentsActivityLaunchState getLaunchState() {
        return this.mLaunchState;
    }

    public DockState[] getDockStatesForCurrentOrientation() {
        boolean z = this.mAppContext.getResources().getConfiguration().orientation == 2;
        return Recents.getConfiguration().isLargeScreen ? z ? DockRegion.TABLET_LANDSCAPE : DockRegion.TABLET_PORTRAIT : z ? DockRegion.PHONE_LANDSCAPE : DockRegion.PHONE_PORTRAIT;
    }
}
