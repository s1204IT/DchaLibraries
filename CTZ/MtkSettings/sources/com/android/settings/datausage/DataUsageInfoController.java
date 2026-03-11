package com.android.settings.datausage;

import android.net.NetworkPolicy;
import com.android.settingslib.net.DataUsageController;

public class DataUsageInfoController {
    public void updateDataLimit(DataUsageController.DataUsageInfo dataUsageInfo, NetworkPolicy networkPolicy) {
        if (dataUsageInfo == null || networkPolicy == null) {
            return;
        }
        if (networkPolicy.warningBytes >= 0) {
            dataUsageInfo.warningLevel = networkPolicy.warningBytes;
        }
        if (networkPolicy.limitBytes >= 0) {
            dataUsageInfo.limitLevel = networkPolicy.limitBytes;
        }
    }

    public long getSummaryLimit(DataUsageController.DataUsageInfo dataUsageInfo) {
        long j = dataUsageInfo.limitLevel;
        if (j <= 0) {
            j = dataUsageInfo.warningLevel;
        }
        if (dataUsageInfo.usageLevel > j) {
            return dataUsageInfo.usageLevel;
        }
        return j;
    }
}
