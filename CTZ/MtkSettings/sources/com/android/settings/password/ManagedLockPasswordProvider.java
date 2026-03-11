package com.android.settings.password;

import android.content.Context;
import android.content.Intent;

public class ManagedLockPasswordProvider {
    public static ManagedLockPasswordProvider get(Context context, int i) {
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

    CharSequence getPickerOptionTitle(boolean z) {
        return "";
    }

    Intent createIntent(boolean z, String str) {
        return null;
    }
}
