package com.android.settings.notification;

import android.R;
import android.app.INotificationManager;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.graphics.drawable.Drawable;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.notification.NotifyingApp;
import android.util.IconDrawableFactory;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.Utils;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class NotificationBackend {
    static INotificationManager sINM = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));

    public static class AppRow extends Row {
        public boolean banned;
        public int blockedChannelCount;
        public int channelCount;
        public Drawable icon;
        public CharSequence label;
        public String lockedChannelId;
        public boolean lockedImportance;
        public String pkg;
        public Intent settingsIntent;
        public boolean showBadge;
        public boolean systemApp;
        public int uid;
        public int userId;
    }

    public AppRow loadAppRow(Context context, PackageManager packageManager, ApplicationInfo applicationInfo) {
        AppRow appRow = new AppRow();
        appRow.pkg = applicationInfo.packageName;
        appRow.uid = applicationInfo.uid;
        try {
            appRow.label = applicationInfo.loadLabel(packageManager);
        } catch (Throwable th) {
            Log.e("NotificationBackend", "Error loading application label for " + appRow.pkg, th);
            appRow.label = appRow.pkg;
        }
        appRow.icon = IconDrawableFactory.newInstance(context).getBadgedIcon(applicationInfo);
        appRow.banned = getNotificationsBanned(appRow.pkg, appRow.uid);
        appRow.showBadge = canShowBadge(appRow.pkg, appRow.uid);
        appRow.userId = UserHandle.getUserId(appRow.uid);
        appRow.blockedChannelCount = getBlockedChannelCount(appRow.pkg, appRow.uid);
        appRow.channelCount = getChannelCount(appRow.pkg, appRow.uid);
        return appRow;
    }

    public AppRow loadAppRow(Context context, PackageManager packageManager, PackageInfo packageInfo) {
        AppRow appRowLoadAppRow = loadAppRow(context, packageManager, packageInfo.applicationInfo);
        recordCanBeBlocked(context, packageManager, packageInfo, appRowLoadAppRow);
        return appRowLoadAppRow;
    }

    void recordCanBeBlocked(Context context, PackageManager packageManager, PackageInfo packageInfo, AppRow appRow) {
        appRow.systemApp = Utils.isSystemPackage(context.getResources(), packageManager, packageInfo);
        markAppRowWithBlockables(context.getResources().getStringArray(R.array.config_defaultCloudSearchServices), appRow, packageInfo.packageName);
    }

    @VisibleForTesting
    static void markAppRowWithBlockables(String[] strArr, AppRow appRow, String str) {
        if (strArr != null) {
            int length = strArr.length;
            for (int i = 0; i < length; i++) {
                String str2 = strArr[i];
                if (str2 != null) {
                    if (str2.contains(":")) {
                        if (str.equals(str2.split(":", 2)[0])) {
                            appRow.lockedChannelId = str2.split(":", 2)[1];
                        }
                    } else if (str.equals(strArr[i])) {
                        appRow.lockedImportance = true;
                        appRow.systemApp = true;
                    }
                }
            }
        }
    }

    public boolean isSystemApp(Context context, ApplicationInfo applicationInfo) throws PackageManager.NameNotFoundException {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(applicationInfo.packageName, 64);
            AppRow appRow = new AppRow();
            recordCanBeBlocked(context, context.getPackageManager(), packageInfo, appRow);
            return appRow.systemApp;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean getNotificationsBanned(String str, int i) {
        try {
            return !sINM.areNotificationsEnabledForPackage(str, i);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setNotificationsEnabledForPackage(String str, int i, boolean z) {
        try {
            if (onlyHasDefaultChannel(str, i)) {
                NotificationChannel channel = getChannel(str, i, "miscellaneous");
                channel.setImportance(z ? -1000 : 0);
                updateChannel(str, i, channel);
            }
            sINM.setNotificationsEnabledForPackage(str, i, z);
            return true;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean canShowBadge(String str, int i) {
        try {
            return sINM.canShowBadge(str, i);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public boolean setShowBadge(String str, int i, boolean z) {
        try {
            sINM.setShowBadge(str, i, z);
            return true;
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public NotificationChannel getChannel(String str, int i, String str2) {
        if (str2 == null) {
            return null;
        }
        try {
            return sINM.getNotificationChannelForPackage(str, i, str2, true);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return null;
        }
    }

    public NotificationChannelGroup getGroup(String str, int i, String str2) {
        if (str2 == null) {
            return null;
        }
        try {
            return sINM.getNotificationChannelGroupForPackage(str2, str, i);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return null;
        }
    }

    public ParceledListSlice<NotificationChannelGroup> getGroups(String str, int i) {
        try {
            return sINM.getNotificationChannelGroupsForPackage(str, i, false);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return ParceledListSlice.emptyList();
        }
    }

    public void updateChannel(String str, int i, NotificationChannel notificationChannel) {
        try {
            sINM.updateNotificationChannelForPackage(str, i, notificationChannel);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
        }
    }

    public void updateChannelGroup(String str, int i, NotificationChannelGroup notificationChannelGroup) {
        try {
            sINM.updateNotificationChannelGroupForPackage(str, i, notificationChannelGroup);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
        }
    }

    public int getDeletedChannelCount(String str, int i) {
        try {
            return sINM.getDeletedChannelCount(str, i);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return 0;
        }
    }

    public int getBlockedChannelCount(String str, int i) {
        try {
            return sINM.getBlockedChannelCount(str, i);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return 0;
        }
    }

    public boolean onlyHasDefaultChannel(String str, int i) {
        try {
            return sINM.onlyHasDefaultChannel(str, i);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return false;
        }
    }

    public int getChannelCount(String str, int i) {
        try {
            return sINM.getNumNotificationChannelsForPackage(str, i, false);
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return 0;
        }
    }

    public List<NotifyingApp> getRecentApps() {
        try {
            return sINM.getRecentNotifyingAppsForUser(UserHandle.myUserId()).getList();
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return new ArrayList();
        }
    }

    public int getBlockedAppCount() {
        try {
            return sINM.getBlockedAppCount(UserHandle.myUserId());
        } catch (Exception e) {
            Log.w("NotificationBackend", "Error calling NoMan", e);
            return 0;
        }
    }

    static class Row {
        Row() {
        }
    }
}
