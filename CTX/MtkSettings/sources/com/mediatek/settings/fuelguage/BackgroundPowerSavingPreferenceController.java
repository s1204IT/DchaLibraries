package com.mediatek.settings.fuelguage;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.FeatureOption;

public class BackgroundPowerSavingPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public BackgroundPowerSavingPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return FeatureOption.MTK_BG_POWER_SAVING_SUPPORT && FeatureOption.MTK_BG_POWER_SAVING_UI_SUPPORT;
    }

    @Override
    public String getPreferenceKey() {
        return "background_power_saving";
    }

    @Override
    public void updateState(Preference preference) {
        int i = Settings.System.getInt(this.mContext.getContentResolver(), "background_power_saving_enable", 1);
        Log.d("BackgroundPowerSavingPreferenceContr", "update background power saving state: " + i);
        ((SwitchPreference) preference).setChecked(i != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        Log.d("BackgroundPowerSavingPreferenceContr", "set background power saving state: " + (zBooleanValue ? 1 : 0));
        Settings.System.putInt(this.mContext.getContentResolver(), "background_power_saving_enable", zBooleanValue ? 1 : 0);
        return true;
    }
}
