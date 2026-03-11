package com.android.settings.display;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class VrDisplayPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public VrDisplayPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.vr.high_performance");
    }

    @Override
    public String getPreferenceKey() {
        return "vr_display_pref";
    }

    @Override
    public void updateState(Preference preference) {
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "vr_display_mode", 0, ActivityManager.getCurrentUser()) == 0) {
            preference.setSummary(R.string.display_vr_pref_low_persistence);
        } else {
            preference.setSummary(R.string.display_vr_pref_off);
        }
    }
}
