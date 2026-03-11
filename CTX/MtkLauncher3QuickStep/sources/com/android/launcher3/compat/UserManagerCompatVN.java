package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Process;
import android.os.UserHandle;

@TargetApi(24)
public class UserManagerCompatVN extends UserManagerCompatVM {
    UserManagerCompatVN(Context context) {
        super(context);
    }

    @Override
    public boolean isQuietModeEnabled(UserHandle userHandle) {
        return this.mUserManager.isQuietModeEnabled(userHandle);
    }

    @Override
    public boolean isUserUnlocked(UserHandle userHandle) {
        return this.mUserManager.isUserUnlocked(userHandle);
    }

    @Override
    public boolean isAnyProfileQuietModeEnabled() {
        for (UserHandle userHandle : getUserProfiles()) {
            if (!Process.myUserHandle().equals(userHandle) && isQuietModeEnabled(userHandle)) {
                return true;
            }
        }
        return false;
    }
}
