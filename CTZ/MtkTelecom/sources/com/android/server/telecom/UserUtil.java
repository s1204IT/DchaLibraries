package com.android.server.telecom;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

public final class UserUtil {
    private static UserInfo getUserInfoFromUserHandle(Context context, UserHandle userHandle) {
        return ((UserManager) context.getSystemService("user")).getUserInfo(userHandle.getIdentifier());
    }

    public static boolean isManagedProfile(Context context, UserHandle userHandle) {
        UserInfo userInfoFromUserHandle = getUserInfoFromUserHandle(context, userHandle);
        return userInfoFromUserHandle != null && userInfoFromUserHandle.isManagedProfile();
    }

    public static boolean isProfile(Context context, UserHandle userHandle) {
        UserInfo userInfoFromUserHandle = getUserInfoFromUserHandle(context, userHandle);
        return (userInfoFromUserHandle == null || userInfoFromUserHandle.profileGroupId == userInfoFromUserHandle.id) ? false : true;
    }
}
