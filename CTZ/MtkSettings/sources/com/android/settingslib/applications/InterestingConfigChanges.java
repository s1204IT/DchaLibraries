package com.android.settingslib.applications;

import android.content.res.Configuration;
import android.content.res.Resources;

public class InterestingConfigChanges {
    private final int mFlags;
    private final Configuration mLastConfiguration;
    private int mLastDensity;

    public InterestingConfigChanges() {
        this(-2147482876);
    }

    public InterestingConfigChanges(int i) {
        this.mLastConfiguration = new Configuration();
        this.mFlags = i;
    }

    public boolean applyNewConfig(Resources resources) {
        int iUpdateFrom = this.mLastConfiguration.updateFrom(Configuration.generateDelta(this.mLastConfiguration, resources.getConfiguration()));
        if (!(this.mLastDensity != resources.getDisplayMetrics().densityDpi) && (iUpdateFrom & this.mFlags) == 0) {
            return false;
        }
        this.mLastDensity = resources.getDisplayMetrics().densityDpi;
        return true;
    }
}
