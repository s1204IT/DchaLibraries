package com.android.settings;

import android.content.Context;
import android.content.Intent;

public class ManagedLockPasswordProvider {
    static ManagedLockPasswordProvider get(Context context, int userId) {
        return new ManagedLockPasswordProvider();
    }

    protected ManagedLockPasswordProvider() {
    }

    boolean isSettingManagedPasswordSupported() {
        return false;
    }

    boolean isManagedPasswordChoosable() {
        return false;
    }

    String getPickerOptionTitle(boolean forFingerprint) {
        return "";
    }

    int getResIdForLockUnlockScreen(boolean forProfile) {
        return forProfile ? R.xml.security_settings_password_profile : R.xml.security_settings_password;
    }

    int getResIdForLockUnlockSubScreen() {
        return R.xml.security_settings_password_sub;
    }

    Intent createIntent(boolean requirePasswordToDecrypt, String password) {
        return null;
    }
}
