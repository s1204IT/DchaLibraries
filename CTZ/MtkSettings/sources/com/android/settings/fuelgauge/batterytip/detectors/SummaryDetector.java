package com.android.settings.fuelgauge.batterytip.detectors;

import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SummaryTip;

public class SummaryDetector {
    private long mAverageTimeMs;
    private BatteryTipPolicy mPolicy;

    public SummaryDetector(BatteryTipPolicy batteryTipPolicy, long j) {
        this.mPolicy = batteryTipPolicy;
        this.mAverageTimeMs = j;
    }

    public BatteryTip detect() {
        int i;
        if (this.mPolicy.summaryEnabled) {
            i = 0;
        } else {
            i = 2;
        }
        return new SummaryTip(i, this.mAverageTimeMs);
    }
}
