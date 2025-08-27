package com.android.settings.fuelgauge.batterytip.detectors;

import android.content.Context;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.HighUsageDataParser;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/* loaded from: classes.dex */
public class HighUsageDetector {
    private BatteryStatsHelper mBatteryStatsHelper;
    BatteryUtils mBatteryUtils;
    HighUsageDataParser mDataParser;
    boolean mDischarging;
    private List<AppInfo> mHighUsageAppList = new ArrayList();
    private BatteryTipPolicy mPolicy;

    public HighUsageDetector(Context context, BatteryTipPolicy batteryTipPolicy, BatteryStatsHelper batteryStatsHelper, boolean z) {
        this.mPolicy = batteryTipPolicy;
        this.mBatteryStatsHelper = batteryStatsHelper;
        this.mBatteryUtils = BatteryUtils.getInstance(context);
        this.mDataParser = new HighUsageDataParser(this.mPolicy.highUsagePeriodMs, this.mPolicy.highUsageBatteryDraining);
        this.mDischarging = z;
    }

    public BatteryTip detect() {
        long jCalculateLastFullChargeTime = this.mBatteryUtils.calculateLastFullChargeTime(this.mBatteryStatsHelper, System.currentTimeMillis());
        if (this.mPolicy.highUsageEnabled && this.mDischarging) {
            parseBatteryData();
            if (this.mDataParser.isDeviceHeavilyUsed() || this.mPolicy.testHighUsageTip) {
                List usageList = this.mBatteryStatsHelper.getUsageList();
                int size = usageList.size();
                for (int i = 0; i < size; i++) {
                    BatterySipper batterySipper = (BatterySipper) usageList.get(i);
                    if (!this.mBatteryUtils.shouldHideSipper(batterySipper)) {
                        long processTimeMs = this.mBatteryUtils.getProcessTimeMs(1, batterySipper.uidObj, 0);
                        if (processTimeMs >= 60000) {
                            this.mHighUsageAppList.add(new AppInfo.Builder().setUid(batterySipper.getUid()).setPackageName(this.mBatteryUtils.getPackageName(batterySipper.getUid())).setScreenOnTimeMs(processTimeMs).build());
                        }
                    }
                }
                if (this.mPolicy.testHighUsageTip && this.mHighUsageAppList.isEmpty()) {
                    this.mHighUsageAppList.add(new AppInfo.Builder().setPackageName("com.android.settings").setScreenOnTimeMs(TimeUnit.HOURS.toMillis(3L)).build());
                }
                Collections.sort(this.mHighUsageAppList, Collections.reverseOrder());
                this.mHighUsageAppList = this.mHighUsageAppList.subList(0, Math.min(this.mPolicy.highUsageAppCount, this.mHighUsageAppList.size()));
            }
        }
        return new HighUsageTip(jCalculateLastFullChargeTime, this.mHighUsageAppList);
    }

    void parseBatteryData() {
        BatteryInfo.parse(this.mBatteryStatsHelper.getStats(), this.mDataParser);
    }
}
