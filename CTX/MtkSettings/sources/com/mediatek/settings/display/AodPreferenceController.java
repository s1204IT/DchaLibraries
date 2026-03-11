package com.mediatek.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.FeatureOption;

public class AodPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public AodPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "always_on_display";
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(Settings.Secure.getInt(this.mContext.getContentResolver(), "doze_enabled", 0) != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "doze_enabled", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override
    public boolean isAvailable() {
        return FeatureOption.MTK_AOD_SUPPORT;
    }
}
