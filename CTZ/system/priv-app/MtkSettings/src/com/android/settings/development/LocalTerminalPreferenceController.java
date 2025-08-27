package com.android.settings.development;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.wrapper.PackageManagerWrapper;

/* loaded from: classes.dex */
public class LocalTerminalPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final String TERMINAL_APP_PACKAGE = "com.android.terminal";
    private PackageManagerWrapper mPackageManager;
    private UserManager mUserManager;

    public LocalTerminalPreferenceController(Context context) {
        super(context);
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return isPackageInstalled(TERMINAL_APP_PACKAGE);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "enable_terminal";
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPackageManager = getPackageManagerWrapper();
        if (isAvailable() && !isEnabled()) {
            this.mPreference.setEnabled(false);
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        int i;
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        PackageManagerWrapper packageManagerWrapper = this.mPackageManager;
        if (!zBooleanValue) {
            i = 0;
        } else {
            i = 1;
        }
        packageManagerWrapper.setApplicationEnabledSetting(TERMINAL_APP_PACKAGE, i, 0);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(this.mPackageManager.getApplicationEnabledSetting(TERMINAL_APP_PACKAGE) == 1);
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchEnabled() {
        if (isEnabled()) {
            this.mPreference.setEnabled(true);
        }
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        this.mPackageManager.setApplicationEnabledSetting(TERMINAL_APP_PACKAGE, 0, 0);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }

    PackageManagerWrapper getPackageManagerWrapper() {
        return new PackageManagerWrapper(this.mContext.getPackageManager());
    }

    private boolean isPackageInstalled(String str) {
        try {
            return this.mContext.getPackageManager().getPackageInfo(str, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isEnabled() {
        return this.mUserManager.isAdminUser();
    }
}
