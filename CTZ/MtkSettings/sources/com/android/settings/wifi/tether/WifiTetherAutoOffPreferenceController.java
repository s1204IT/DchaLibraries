package com.android.settings.wifi.tether;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.BasePreferenceController;

public class WifiTetherAutoOffPreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {
    public WifiTetherAutoOffPreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "soft_ap_timeout_enabled", 1) != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "soft_ap_timeout_enabled", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }
}
