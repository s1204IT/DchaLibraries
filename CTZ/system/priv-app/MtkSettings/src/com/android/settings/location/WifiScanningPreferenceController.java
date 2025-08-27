package com.android.settings.location;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class WifiScanningPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public WifiScanningPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "wifi_always_scanning";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) == 1);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if ("wifi_always_scanning".equals(preference.getKey())) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", ((SwitchPreference) preference).isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }
}
