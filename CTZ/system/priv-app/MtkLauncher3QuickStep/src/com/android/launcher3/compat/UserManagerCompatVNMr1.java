package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;

@TargetApi(25)
/* loaded from: classes.dex */
public class UserManagerCompatVNMr1 extends UserManagerCompatVN {
    UserManagerCompatVNMr1(Context context) {
        super(context);
    }

    @Override // com.android.launcher3.compat.UserManagerCompatVL, com.android.launcher3.compat.UserManagerCompat
    public boolean isDemoUser() {
        return this.mUserManager.isDemoUser();
    }
}
