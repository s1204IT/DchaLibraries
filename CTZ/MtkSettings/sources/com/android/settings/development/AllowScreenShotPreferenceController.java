package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class AllowScreenShotPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    public AllowScreenShotPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "allow_screen_shot";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.System.putInt(this.mContext.getContentResolver(), "allow_screen_shot", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(Settings.System.getInt(this.mContext.getContentResolver(), "allow_screen_shot", 1) == 1);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.System.putInt(this.mContext.getContentResolver(), "allow_screen_shot", 0);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
