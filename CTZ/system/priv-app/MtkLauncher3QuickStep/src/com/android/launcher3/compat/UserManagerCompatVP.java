package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.UserHandle;

@TargetApi(28)
/* loaded from: classes.dex */
public class UserManagerCompatVP extends UserManagerCompatVNMr1 {
    UserManagerCompatVP(Context context) {
        super(context);
    }

    @Override // com.android.launcher3.compat.UserManagerCompatVL, com.android.launcher3.compat.UserManagerCompat
    public boolean requestQuietModeEnabled(boolean z, UserHandle userHandle) {
        return this.mUserManager.requestQuietModeEnabled(z, userHandle);
    }
}
