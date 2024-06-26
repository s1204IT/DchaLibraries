package com.android.settings.fuelgauge.batterytip.detectors;

import android.content.ContentResolver;
import android.provider.Settings;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SmartBatteryTip;
/* loaded from: classes.dex */
public class SmartBatteryDetector {
    private ContentResolver mContentResolver;
    private BatteryTipPolicy mPolicy;

    public SmartBatteryDetector(BatteryTipPolicy batteryTipPolicy, ContentResolver contentResolver) {
        this.mPolicy = batteryTipPolicy;
        this.mContentResolver = contentResolver;
    }

    public BatteryTip detect() {
        boolean z = true;
        if (Settings.Global.getInt(this.mContentResolver, "adaptive_battery_management_enabled", 1) != 0 && !this.mPolicy.testSmartBatteryTip) {
            z = false;
        }
        return new SmartBatteryTip(z ? 0 : 2);
    }
}
