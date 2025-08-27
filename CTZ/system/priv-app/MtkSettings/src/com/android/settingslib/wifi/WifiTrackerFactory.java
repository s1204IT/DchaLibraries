package com.android.settingslib.wifi;

import android.content.Context;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.wifi.WifiTracker;

/* loaded from: classes.dex */
public class WifiTrackerFactory {
    private static WifiTracker sTestingWifiTracker;

    public static void setTestingWifiTracker(WifiTracker wifiTracker) {
        sTestingWifiTracker = wifiTracker;
    }

    public static WifiTracker create(Context context, WifiTracker.WifiListener wifiListener, Lifecycle lifecycle, boolean z, boolean z2) {
        if (sTestingWifiTracker != null) {
            return sTestingWifiTracker;
        }
        return new WifiTracker(context, wifiListener, lifecycle, z, z2);
    }
}
