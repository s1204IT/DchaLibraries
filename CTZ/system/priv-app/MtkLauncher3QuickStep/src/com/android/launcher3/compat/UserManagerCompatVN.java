package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Process;
import android.os.UserHandle;

@TargetApi(24)
/* loaded from: classes.dex */
public class UserManagerCompatVN extends UserManagerCompatVM {
    UserManagerCompatVN(Context context) {
        super(context);
    }

    @Override // com.android.launcher3.compat.UserManagerCompatVL, com.android.launcher3.compat.UserManagerCompat
    public boolean isQuietModeEnabled(UserHandle userHandle) {
        return this.mUserManager.isQuietModeEnabled(userHandle);
    }

    @Override // com.android.launcher3.compat.UserManagerCompatVL, com.android.launcher3.compat.UserManagerCompat
    public boolean isUserUnlocked(UserHandle userHandle) {
        return this.mUserManager.isUserUnlocked(userHandle);
    }

    @Override // com.android.launcher3.compat.UserManagerCompatVL, com.android.launcher3.compat.UserManagerCompat
    public boolean isAnyProfileQuietModeEnabled() {
        for (UserHandle userHandle : getUserProfiles()) {
            if (!Process.myUserHandle().equals(userHandle) && isQuietModeEnabled(userHandle)) {
                return true;
            }
        }
        return false;
    }
}
