package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class DisableAutomaticUpdatesPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int DISABLE_UPDATES_SETTING = 1;
    static final int ENABLE_UPDATES_SETTING = 0;

    public DisableAutomaticUpdatesPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "ota_disable_automatic_update";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "ota_disable_automatic_update", !((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "ota_disable_automatic_update", 0) != 1);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(this.mContext.getContentResolver(), "ota_disable_automatic_update", 1);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
