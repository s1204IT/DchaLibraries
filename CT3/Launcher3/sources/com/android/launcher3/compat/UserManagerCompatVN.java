package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;

@TargetApi(24)
public class UserManagerCompatVN extends UserManagerCompatVL {
    private static final String TAG = "UserManagerCompatVN";

    UserManagerCompatVN(Context context) {
        super(context);
    }

    @Override
    public boolean isQuietModeEnabled(UserHandleCompat user) {
        if (user != null) {
            try {
                return this.mUserManager.isQuietModeEnabled(user.getUser());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }
}
