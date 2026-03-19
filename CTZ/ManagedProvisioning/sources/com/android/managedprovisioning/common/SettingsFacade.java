package com.android.managedprovisioning.common;

import android.content.Context;
import android.provider.Settings;

public class SettingsFacade {
    public void setUserSetupCompleted(Context context, int i) {
        ProvisionLogger.logd("Setting USER_SETUP_COMPLETE to 1 for user " + i);
        Settings.Secure.putIntForUser(context.getContentResolver(), "user_setup_complete", 1, i);
    }

    public boolean isUserSetupCompleted(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "user_setup_complete", 0) != 0;
    }

    public boolean isDeviceProvisioned(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) != 0;
    }

    public void setProfileContactRemoteSearch(Context context, boolean z, int i) {
        Settings.Secure.putIntForUser(context.getContentResolver(), "managed_profile_contact_remote_search", z ? 1 : 0, i);
    }
}
