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
                UserSettings.copyMeProfilePhoto(context, null);
                String str = SystemProperties.get("ro.com.google.gmsversion", (String) null);
                if (str != null && !str.isEmpty()) {
                    ProfileUpdateReceiver.copyProfileName(context);
                }
            }
        }.start();
    }

    private static void copyProfileName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("profile", 0);
        if (sharedPreferences.contains("name_copied_once")) {
            return;
        }
        int iMyUserId = UserHandle.myUserId();
        UserManager userManager = (UserManager) context.getSystemService("user");
        String meProfileName = Utils.getMeProfileName(context, false);
        if (meProfileName != null && meProfileName.length() > 0) {
            userManager.setUserName(iMyUserId, meProfileName);
            sharedPreferences.edit().putBoolean("name_copied_once", true).commit();
        }
    }
}
