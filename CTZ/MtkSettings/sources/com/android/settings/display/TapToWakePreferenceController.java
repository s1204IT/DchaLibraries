package com.android.settings.display;

import android.R;
import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class TapToWakePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public TapToWakePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "tap_to_wake";
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.^attr-private.panelMenuListTheme);
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(Settings.Secure.getInt(this.mContext.getContentResolver(), "double_tap_to_wake", 0) != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "double_tap_to_wake", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }
}
