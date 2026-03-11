package com.android.settings.users;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.settings.Utils;

public class ProfileUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("ProfileUpdateReceiver", "Profile photo changed, get the PROFILE_CHANGED receiver.");
        new Thread() {
            @Override
            public void run() {
                Utils.copyMeProfilePhoto(context, null);
                String isGms = SystemProperties.get("ro.com.google.gmsversion", (String) null);
                if (isGms == null || isGms.isEmpty()) {
                    return;
                }
                ProfileUpdateReceiver.copyProfileName(context);
            }
        }.start();
    }

    static void copyProfileName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("profile", 0);
        if (prefs.contains("name_copied_once")) {
            return;
        }
        int userId = UserHandle.myUserId();
        UserManager um = (UserManager) context.getSystemService("user");
        String profileName = Utils.getMeProfileName(context, false);
        if (profileName == null || profileName.length() <= 0) {
            return;
        }
        um.setUserName(userId, profileName);
        prefs.edit().putBoolean("name_copied_once", true).commit();
    }
}
