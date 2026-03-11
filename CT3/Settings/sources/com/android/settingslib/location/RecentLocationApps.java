package com.android.settingslib.location;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class RecentLocationApps {
    private final Context mContext;
    private final PackageManager mPackageManager;
    private static final String TAG = RecentLocationApps.class.getSimpleName();
    private static final int[] LOCATION_OPS = {41, 42};

    public RecentLocationApps(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }

    public List<Request> getAppList() {
        Request request;
        AppOpsManager aoManager = (AppOpsManager) this.mContext.getSystemService("appops");
        List<AppOpsManager.PackageOps> appOps = aoManager.getPackagesForOps(LOCATION_OPS);
        int appOpsCount = appOps != null ? appOps.size() : 0;
        ArrayList<Request> requests = new ArrayList<>(appOpsCount);
        long now = System.currentTimeMillis();
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        List<UserHandle> profiles = um.getUserProfiles();
        for (int i = 0; i < appOpsCount; i++) {
            AppOpsManager.PackageOps ops = appOps.get(i);
            String packageName = ops.getPackageName();
            int uid = ops.getUid();
            int userId = UserHandle.getUserId(uid);
            boolean isAndroidOs = uid == 1000 ? "android".equals(packageName) : false;
            if (!isAndroidOs && profiles.contains(new UserHandle(userId)) && (request = getRequestFromOps(now, ops)) != null) {
                requests.add(request);
            }
        }
        return requests;
    }

    private Request getRequestFromOps(long now, AppOpsManager.PackageOps ops) {
        String packageName = ops.getPackageName();
        List<AppOpsManager.OpEntry> entries = ops.getOps();
        boolean highBattery = false;
        boolean normalBattery = false;
        long recentLocationCutoffTime = now - 900000;
        for (AppOpsManager.OpEntry entry : entries) {
            if (entry.isRunning() || entry.getTime() >= recentLocationCutoffTime) {
                switch (entry.getOp()) {
                    case 41:
                        normalBattery = true;
                        break;
                    case 42:
                        highBattery = true;
                        break;
                }
            }
        }
        if (!highBattery && !normalBattery) {
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, packageName + " hadn't used location within the time interval.");
                return null;
            }
            return null;
        }
        int uid = ops.getUid();
        int userId = UserHandle.getUserId(uid);
        try {
            IPackageManager ipm = AppGlobals.getPackageManager();
            ApplicationInfo appInfo = ipm.getApplicationInfo(packageName, 128, userId);
            if (appInfo == null) {
                Log.w(TAG, "Null application info retrieved for package " + packageName + ", userId " + userId);
                return null;
            }
            UserHandle userHandle = new UserHandle(userId);
            Drawable appIcon = this.mPackageManager.getApplicationIcon(appInfo);
            Drawable icon = this.mPackageManager.getUserBadgedIcon(appIcon, userHandle);
            CharSequence appLabel = this.mPackageManager.getApplicationLabel(appInfo);
            CharSequence badgedAppLabel = this.mPackageManager.getUserBadgedLabel(appLabel, userHandle);
            if (appLabel.toString().contentEquals(badgedAppLabel)) {
                badgedAppLabel = null;
            }
            Request request = new Request(packageName, userHandle, icon, appLabel, highBattery, badgedAppLabel, null);
            return request;
        } catch (RemoteException e) {
            Log.w(TAG, "Error while retrieving application info for package " + packageName + ", userId " + userId, e);
            return null;
        }
    }

    public static class Request {
        public final CharSequence contentDescription;
        public final Drawable icon;
        public final boolean isHighBattery;
        public final CharSequence label;
        public final String packageName;
        public final UserHandle userHandle;

        Request(String packageName, UserHandle userHandle, Drawable icon, CharSequence label, boolean isHighBattery, CharSequence contentDescription, Request request) {
            this(packageName, userHandle, icon, label, isHighBattery, contentDescription);
        }

        private Request(String packageName, UserHandle userHandle, Drawable icon, CharSequence label, boolean isHighBattery, CharSequence contentDescription) {
            this.packageName = packageName;
            this.userHandle = userHandle;
            this.icon = icon;
            this.label = label;
            this.isHighBattery = isHighBattery;
            this.contentDescription = contentDescription;
        }
    }
}
