package com.android.providers.contacts.util;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.util.Log;

public final class UserUtils {
    public static final boolean VERBOSE_LOGGING = Log.isLoggable("ContactsProvider", 2);

    public static UserManager getUserManager(Context context) {
        return (UserManager) context.getSystemService("user");
    }

    private static DevicePolicyManager getDevicePolicyManager(Context context) {
        return (DevicePolicyManager) context.getSystemService("device_policy");
    }

    public static int getCurrentUserHandle(Context context) {
        return getUserManager(context).getUserHandle();
    }

    public static int getCorpUserId(Context context) {
        UserInfo parent;
        UserManager um = getUserManager(context);
        if (um == null) {
            Log.e("ContactsProvider", "No user manager service found");
            return -1;
        }
        int myUser = um.getUserHandle();
        if (VERBOSE_LOGGING) {
            Log.v("ContactsProvider", "getCorpUserId: myUser=" + myUser);
        }
        for (UserInfo ui : um.getUsers()) {
            if (ui.isManagedProfile() && (parent = um.getProfileParent(ui.id)) != null && parent.id == myUser) {
                if (getDevicePolicyManager(context).getCrossProfileCallerIdDisabled(ui.getUserHandle())) {
                    if (!VERBOSE_LOGGING) {
                        return -1;
                    }
                    Log.v("ContactsProvider", "Enterprise caller-id disabled for user " + ui.id);
                    return -1;
                }
                if (VERBOSE_LOGGING) {
                    Log.v("ContactsProvider", "Corp user=" + ui.id);
                }
                return ui.id;
            }
        }
        if (!VERBOSE_LOGGING) {
            return -1;
        }
        Log.v("ContactsProvider", "Corp user not found.");
        return -1;
    }
}
