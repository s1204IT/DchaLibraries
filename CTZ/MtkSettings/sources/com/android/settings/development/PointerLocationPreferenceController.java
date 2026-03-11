package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class PointerLocationPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int SETTING_VALUE_OFF = 0;
    static final int SETTING_VALUE_ON = 1;

    public PointerLocationPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "pointer_location";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.System.putInt(this.mContext.getContentResolver(), "pointer_location", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(Settings.System.getInt(this.mContext.getContentResolver(), "pointer_location", 0) != 0);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.System.putInt(this.mContext.getContentResolver(), "pointer_location", 0);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
