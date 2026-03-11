package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.UserHandle;

@TargetApi(28)
public class UserManagerCompatVP extends UserManagerCompatVNMr1 {
    UserManagerCompatVP(Context context) {
        super(context);
    }

    @Override
    public boolean requestQuietModeEnabled(boolean z, UserHandle userHandle) {
        return this.mUserManager.requestQuietModeEnabled(z, userHandle);
    }
}
