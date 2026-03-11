package com.android.settings.notification;

import android.app.INotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settingslib.Utils;

public class NotificationBackend {
    static INotificationManager sINM = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));

    public static class AppRow extends Row {
        public boolean appBypassDnd;
        public int appImportance;
        public int appVisOverride;
        public boolean banned;
        public Drawable icon;
        public CharSequence label;
        public boolean lockScreenSecure;
        public String pkg;
        public Intent settingsIntent;
        public boolean systemApp;
        public int uid;
    }

    public AppRow loadAppRow(Context context, PackageManager pm, ApplicationInfo app) {
        AppRow row = new AppRow();
        row.pkg = app.packageName;
        row.uid = app.uid;
        try {
            row.label = app.loadLabel(pm);
        } catch (Throwable t) {
            Log.e("NotificationBackend", "Error loading application label for " + row.pkg, t);
            row.label = row.pkg;
        }
        row.icon = app.loadIcon(pm);
        row.banned = getNotificationsBanned(row.pkg, row.uid);
        row.appImportance = getImportance(row.pkg, row.uid);
        row.appBypassDnd = getBypassZenMode(row.pkg, row.uid);
        row.appVisOverride = getVisibilityOverride(row.pkg, row.uid);
        row.lockScreenSecure = new LockPatternUtils(context).isSecure(UserHandle.myUserId());
        return row;
    }

    public AppRow loadAppRow(Context context, PackageManager pm, PackageInfo app) {
        AppRow row = loadAppRow(context, pm, app.applicationInfo);
        row.systemApp = Utils.isSystemPackage(pm, app);
        return row;
    }

    public boolean getNotificationsBanned(String pkg, int uid) {
        try {
            boolean enabled = sINM.areNotificationsEnabledForPackage(pkg, uid);
            return !enabled;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean getBypassZenMode(String pkg, int uid) {
        try {
            return sINM.getPriority(pkg, uid) == 2;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setBypassZenMode(String pkg, int uid, boolean bypassZen) {
        try {
            sINM.setPriority(pkg, uid, bypassZen ? 2 : 0);
            return true;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public int getVisibilityOverride(String pkg, int uid) {
        try {
            return sINM.getVisibilityOverride(pkg, uid);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return -1000;
        }
    }

    public boolean setVisibilityOverride(String pkg, int uid, int override) {
        try {
            sINM.setVisibilityOverride(pkg, uid, override);
            return true;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setImportance(String pkg, int uid, int importance) {
        try {
            sINM.setImportance(pkg, uid, importance);
            return true;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public int getImportance(String pkg, int uid) {
        try {
            return sINM.getImportance(pkg, uid);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return -1000;
        }
    }

    static class Row {
        Row() {
        }
    }
}
