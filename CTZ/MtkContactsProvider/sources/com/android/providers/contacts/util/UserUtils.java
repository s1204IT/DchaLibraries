package com.android.providers.contacts.util;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.util.Log;

public final class UserUtils {
    public static final boolean VERBOSE_LOGGING = Log.isLoggable("ContactsProvider", 2);

    public static UserManager getUserManager(Context context) {
        return (UserManager) context.getSystemService("user");
    }

    public static int getCurrentUserHandle(Context context) {
        return getUserManager(context).getUserHandle();
    }

    private static UserInfo getCorpUserInfo(Context context) {
        UserInfo profileParent;
        UserManager userManager = getUserManager(context);
        int userHandle = userManager.getUserHandle();
        for (UserInfo userInfo : userManager.getUsers()) {
            if (userInfo.isManagedProfile() && (profileParent = userManager.getProfileParent(userInfo.id)) != null && profileParent.id == userHandle) {
                return userInfo;
            }
        }
        return null;
    }

    public static int getCorpUserId(Context context) {
        UserInfo corpUserInfo = getCorpUserInfo(context);
        if (corpUserInfo == null) {
            return -1;
        }
        return corpUserInfo.id;
    }
}
