package com.android.settingslib.development;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import com.android.settingslib.core.ConfirmationDialogController;

public abstract class AbstractEnableAdbPreferenceController extends DeveloperOptionsPreferenceController implements ConfirmationDialogController {
    protected SwitchPreference mPreference;

    public AbstractEnableAdbPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mPreference = (SwitchPreference) preferenceScreen.findPreference("enable_adb");
        }
    }

    @Override
    public boolean isAvailable() {
        UserManager userManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        return userManager != null && (userManager.isAdminUser() || userManager.isDemoUser());
    }

    @Override
    public String getPreferenceKey() {
        return "enable_adb";
    }

    private boolean isAdbEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0) != 0;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(isAdbEnabled());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (isUserAMonkey() || !TextUtils.equals("enable_adb", preference.getKey())) {
            return false;
        }
        if (!isAdbEnabled()) {
            showConfirmationDialog(preference);
            return true;
        }
        writeAdbSetting(false);
        return true;
    }

    protected void writeAdbSetting(boolean z) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", z ? 1 : 0);
        notifyStateChanged();
    }

    private void notifyStateChanged() {
        LocalBroadcastManager.getInstance(this.mContext).sendBroadcast(new Intent("com.android.settingslib.development.AbstractEnableAdbController.ENABLE_ADB_STATE_CHANGED"));
    }

    boolean isUserAMonkey() {
        return ActivityManager.isUserAMonkey();
    }
}
