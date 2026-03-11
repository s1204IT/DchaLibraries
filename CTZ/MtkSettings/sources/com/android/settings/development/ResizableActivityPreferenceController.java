package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ResizableActivityPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int SETTING_VALUE_OFF = 0;
    static final int SETTING_VALUE_ON = 1;

    public ResizableActivityPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "force_resizable_activities";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "force_resizable_activities", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "force_resizable_activities", 0) != 0);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(this.mContext.getContentResolver(), "force_resizable_activities", 0);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
