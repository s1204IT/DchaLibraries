package com.android.settings.applications;

import android.content.res.Configuration;
import android.content.res.Resources;

class InterestingConfigChanges {
    final Configuration mLastConfiguration = new Configuration();
    int mLastDensity;

    InterestingConfigChanges() {
    }

    boolean applyNewConfig(Resources res) {
        int configChanges = this.mLastConfiguration.updateFrom(res.getConfiguration());
        boolean densityChanged = this.mLastDensity != res.getDisplayMetrics().densityDpi;
        if (!densityChanged && (configChanges & 772) == 0) {
            return false;
        }
        this.mLastDensity = res.getDisplayMetrics().densityDpi;
        return true;
    }
}
