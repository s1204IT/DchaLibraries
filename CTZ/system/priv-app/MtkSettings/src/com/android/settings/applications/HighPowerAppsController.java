package com.android.settings.applications;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/* loaded from: classes.dex */
public class HighPowerAppsController extends BasePreferenceController {
    static final String KEY_HIGH_POWER_APPS = "high_power_apps";

    public HighPowerAppsController(Context context) {
        super(context, KEY_HIGH_POWER_APPS);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (this.mContext.getResources().getBoolean(R.bool.config_show_high_power_apps)) {
            return 0;
        }
        return 2;
    }
}
