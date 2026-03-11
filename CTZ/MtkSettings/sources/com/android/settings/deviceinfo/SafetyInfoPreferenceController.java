package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class SafetyInfoPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private static final Intent INTENT_PROBE = new Intent("android.settings.SHOW_SAFETY_AND_REGULATORY_INFO");
    private final PackageManager mPackageManager;

    public SafetyInfoPreferenceController(Context context) {
        super(context);
        this.mPackageManager = this.mContext.getPackageManager();
    }

    @Override
    public boolean isAvailable() {
        return !this.mPackageManager.queryIntentActivities(INTENT_PROBE, 0).isEmpty();
    }

    @Override
    public String getPreferenceKey() {
        return "safety_info";
    }
}
