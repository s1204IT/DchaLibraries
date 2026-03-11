package com.android.settings.development;

import android.content.Context;
import android.hardware.usb.IUsbManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ClearAdbKeysPreferenceController extends DeveloperOptionsPreferenceController implements PreferenceControllerMixin {
    static final String RO_ADB_SECURE_PROPERTY_KEY = "ro.adb.secure";
    private final DevelopmentSettingsDashboardFragment mFragment;
    private final IUsbManager mUsbManager;

    public ClearAdbKeysPreferenceController(Context context, DevelopmentSettingsDashboardFragment developmentSettingsDashboardFragment) {
        super(context);
        this.mFragment = developmentSettingsDashboardFragment;
        this.mUsbManager = IUsbManager.Stub.asInterface(ServiceManager.getService("usb"));
    }

    @Override
    public boolean isAvailable() {
        return SystemProperties.getBoolean(RO_ADB_SECURE_PROPERTY_KEY, false);
    }

    @Override
    public String getPreferenceKey() {
        return "clear_adb_keys";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (this.mPreference != null && !isAdminUser()) {
            this.mPreference.setEnabled(false);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (Utils.isMonkeyRunning() || !TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        ClearAdbKeysWarningDialog.show(this.mFragment);
        return true;
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        if (isAdminUser()) {
            this.mPreference.setEnabled(true);
        }
    }

    public void onClearAdbKeysConfirmed() {
        try {
            this.mUsbManager.clearUsbDebuggingKeys();
        } catch (RemoteException e) {
            Log.e("ClearAdbPrefCtrl", "Unable to clear adb keys", e);
        }
    }

    boolean isAdminUser() {
        return ((UserManager) this.mContext.getSystemService("user")).isAdminUser();
    }
}
