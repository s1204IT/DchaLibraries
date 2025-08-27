package com.android.settingslib.development;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;

/* loaded from: classes.dex */
public class DevelopmentSettingsEnabler {
    public static void setDevelopmentSettingsEnabled(Context context, boolean z) {
        Settings.Global.putInt(context.getContentResolver(), "development_settings_enabled", z ? 1 : 0);
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("com.android.settingslib.development.DevelopmentSettingsEnabler.SETTINGS_CHANGED"));
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v3, resolved type: byte */
    /* JADX DEBUG: Multi-variable search result rejected for r0v4, resolved type: byte */
    /* JADX DEBUG: Multi-variable search result rejected for r0v6, resolved type: byte */
    /* JADX DEBUG: Multi-variable search result rejected for r5v3, resolved type: byte */
    /* JADX DEBUG: Multi-variable search result rejected for r5v4, resolved type: byte */
    /* JADX DEBUG: Multi-variable search result rejected for r5v5, resolved type: byte */
    /* JADX WARN: Multi-variable type inference failed */
    public static boolean isDevelopmentSettingsEnabled(Context context) {
        UserManager userManager = (UserManager) context.getSystemService("user");
        byte b = Settings.Global.getInt(context.getContentResolver(), "development_settings_enabled", Build.TYPE.equals("eng") ? 1 : 0) != 0;
        return (userManager.isAdminUser() || userManager.isDemoUser()) == true && !userManager.hasUserRestriction("no_debugging_features") && b == true;
    }
}
