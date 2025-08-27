package com.android.settings.development;

import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class BugReportInPowerPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final UserManager mUserManager;
    static int SETTING_VALUE_ON = 1;
    static int SETTING_VALUE_OFF = 0;

    public BugReportInPowerPreferenceController(Context context) {
        super(context);
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !this.mUserManager.hasUserRestriction("no_debugging_features");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bugreport_in_power";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "bugreport_in_power_menu", ((Boolean) obj).booleanValue() ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(Settings.Secure.getInt(this.mContext.getContentResolver(), "bugreport_in_power_menu", SETTING_VALUE_OFF) != SETTING_VALUE_OFF);
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Secure.putInt(this.mContext.getContentResolver(), "bugreport_in_power_menu", SETTING_VALUE_OFF);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
