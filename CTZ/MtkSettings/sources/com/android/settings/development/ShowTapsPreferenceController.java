package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ShowTapsPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int SETTING_VALUE_OFF = 0;
    static final int SETTING_VALUE_ON = 1;

    public ShowTapsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "show_touches";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.System.putInt(this.mContext.getContentResolver(), "show_touches", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(Settings.System.getInt(this.mContext.getContentResolver(), "show_touches", 0) != 0);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.System.putInt(this.mContext.getContentResolver(), "show_touches", 0);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
