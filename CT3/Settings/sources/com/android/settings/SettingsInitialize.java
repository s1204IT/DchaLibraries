package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import java.util.List;

public class SettingsInitialize extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent broadcast) {
        UserManager um = (UserManager) context.getSystemService("user");
        UserInfo userInfo = um.getUserInfo(UserHandle.myUserId());
        PackageManager pm = context.getPackageManager();
        managedProfileSetup(context, pm, broadcast, userInfo);
        webviewSettingSetup(context, pm, userInfo);
    }

    private void managedProfileSetup(Context context, PackageManager pm, Intent broadcast, UserInfo userInfo) {
        if (userInfo == null || !userInfo.isManagedProfile()) {
            return;
        }
        Log.i("Settings", "Received broadcast: " + broadcast.getAction() + ". Setting up intent forwarding for managed profile.");
        pm.clearCrossProfileIntentFilters(userInfo.id);
        Intent intent = new Intent();
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setPackage(context.getPackageName());
        List<ResolveInfo> resolvedIntents = pm.queryIntentActivities(intent, 705);
        int count = resolvedIntents.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = resolvedIntents.get(i);
            if (info.filter != null && info.activityInfo != null && info.activityInfo.metaData != null) {
                boolean shouldForward = info.activityInfo.metaData.getBoolean("com.android.settings.PRIMARY_PROFILE_CONTROLLED");
                if (shouldForward) {
                    pm.addCrossProfileIntentFilter(info.filter, userInfo.id, userInfo.profileGroupId, 2);
                }
            }
        }
        ComponentName settingsComponentName = new ComponentName(context, (Class<?>) Settings.class);
        pm.setComponentEnabledSetting(settingsComponentName, 2, 1);
    }

    private void webviewSettingSetup(Context context, PackageManager pm, UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        ComponentName settingsComponentName = new ComponentName(context, (Class<?>) WebViewImplementation.class);
        pm.setComponentEnabledSetting(settingsComponentName, userInfo.isAdmin() ? 1 : 2, 1);
    }
}
