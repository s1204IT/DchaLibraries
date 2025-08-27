package com.android.settings.fuelgauge.batterytip.detectors;

import android.R;
import android.content.Context;
import android.os.PowerManager;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class LowBatteryDetector {
    private BatteryInfo mBatteryInfo;
    private BatteryTipPolicy mPolicy;
    private PowerManager mPowerManager;
    private int mWarningLevel;

    public LowBatteryDetector(Context context, BatteryTipPolicy batteryTipPolicy, BatteryInfo batteryInfo) {
        this.mPolicy = batteryTipPolicy;
        this.mBatteryInfo = batteryInfo;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mWarningLevel = context.getResources().getInteger(R.integer.config_defaultNotificationLedOff);
    }

    public BatteryTip detect() {
        boolean zIsPowerSaveMode = this.mPowerManager.isPowerSaveMode();
        boolean z = this.mBatteryInfo.batteryLevel <= this.mWarningLevel || (this.mBatteryInfo.discharging && this.mBatteryInfo.remainingTimeUs < TimeUnit.HOURS.toMicros((long) this.mPolicy.lowBatteryHour));
        int i = 2;
        if (this.mPolicy.lowBatteryEnabled) {
            if (!zIsPowerSaveMode) {
                if (this.mPolicy.testLowBatteryTip || (this.mBatteryInfo.discharging && z)) {
                    i = 0;
                }
            } else {
                i = 1;
            }
        }
        return new LowBatteryTip(i, zIsPowerSaveMode, this.mBatteryInfo.remainingLabel);
    }
}
