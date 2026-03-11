package com.android.settings.development;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class FreeformWindowsPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int SETTING_VALUE_OFF = 0;
    static final int SETTING_VALUE_ON = 1;
    static final String USER_BUILD_TYPE = "user";

    public FreeformWindowsPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.equals(USER_BUILD_TYPE, getBuildType());
    }

    @Override
    public String getPreferenceKey() {
        return "enable_freeform_support";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "enable_freeform_support", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "enable_freeform_support", 0) != 0);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(this.mContext.getContentResolver(), "enable_freeform_support", 0);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }

    String getBuildType() {
        return Build.TYPE;
    }
}
