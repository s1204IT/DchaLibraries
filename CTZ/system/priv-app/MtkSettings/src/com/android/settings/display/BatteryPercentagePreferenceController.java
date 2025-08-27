package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class BatteryPercentagePreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public BatteryPercentagePreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "battery_percentage";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(Settings.System.getInt(this.mContext.getContentResolver(), "status_bar_show_battery_percent", 0) == 1);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.System.putInt(this.mContext.getContentResolver(), "status_bar_show_battery_percent", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }
}
