package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;
import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.util.FlagOp;
import com.android.launcher3.util.StringFilter;
import com.mediatek.launcher3.LauncherLog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AllAppsList {
    private AppFilter mAppFilter;
    private IconCache mIconCache;
    public ArrayList<AppInfo> data = new ArrayList<>(42);
    public ArrayList<AppInfo> added = new ArrayList<>(42);
    public ArrayList<AppInfo> removed = new ArrayList<>();
    public ArrayList<AppInfo> modified = new ArrayList<>();

    public AllAppsList(IconCache iconCache, AppFilter appFilter) {
        this.mIconCache = iconCache;
        this.mAppFilter = appFilter;
    }

    public void add(AppInfo info) {
        if (this.mAppFilter != null && !this.mAppFilter.shouldShowApp(info.componentName)) {
            return;
        }
        if (findActivity(this.data, info.componentName, info.user)) {
            LauncherLog.d("AllAppsList", "Application " + info + " already exists in app list, app = " + info);
        } else {
            this.data.add(info);
            this.added.add(info);
        }
    }

    public void clear() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("AllAppsList", "clear all data in app list: app size = " + this.data.size());
        }
        this.data.clear();
        this.added.clear();
        this.removed.clear();
        this.modified.clear();
    }

    public void addPackage(Context context, String packageName, UserHandleCompat user) {
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        List<LauncherActivityInfoCompat> matches = launcherApps.getActivityList(packageName, user);
        for (LauncherActivityInfoCompat info : matches) {
            add(new AppInfo(context, info, user, this.mIconCache));
        }
    }

    public void removePackage(String packageName, UserHandleCompat user) {
        List<AppInfo> data = this.data;
        if (LauncherLog.DEBUG) {
            LauncherLog.d("AllAppsList", "removePackage: packageName = " + packageName + ", data size = " + data.size());
        }
        for (int i = data.size() - 1; i >= 0; i--) {
            AppInfo info = data.get(i);
            ComponentName component = info.intent.getComponent();
            if (info.user.equals(user) && packageName.equals(component.getPackageName())) {
                this.removed.add(info);
                data.remove(i);
            }
        }
    }

    public void updatePackageFlags(StringFilter pkgFilter, UserHandleCompat user, FlagOp op) {
        List<AppInfo> data = this.data;
        for (int i = data.size() - 1; i >= 0; i--) {
            AppInfo info = data.get(i);
            ComponentName component = info.intent.getComponent();
            if (info.user.equals(user) && pkgFilter.matches(component.getPackageName())) {
                info.isDisabled = op.apply(info.isDisabled);
                this.modified.add(info);
            }
        }
    }

    public void updateIconsAndLabels(HashSet<String> packages, UserHandleCompat user, ArrayList<AppInfo> outUpdates) {
        for (AppInfo info : this.data) {
            if (info.user.equals(user) && packages.contains(info.componentName.getPackageName())) {
                this.mIconCache.updateTitleAndIcon(info);
                outUpdates.add(info);
            }
        }
    }

    public void updatePackage(Context context, String packageName, UserHandleCompat user) {
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        List<LauncherActivityInfoCompat> matches = launcherApps.getActivityList(packageName, user);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("AllAppsList", "updatePackage: packageName = " + packageName + ", matches = " + matches.size());
        }
        if (matches.size() > 0) {
            for (int i = this.data.size() - 1; i >= 0; i--) {
                AppInfo applicationInfo = this.data.get(i);
                ComponentName component = applicationInfo.intent.getComponent();
                if (user.equals(applicationInfo.user) && packageName.equals(component.getPackageName()) && !findActivity(matches, component)) {
                    this.removed.add(applicationInfo);
                    this.data.remove(i);
                }
            }
            for (LauncherActivityInfoCompat info : matches) {
                AppInfo applicationInfo2 = findApplicationInfoLocked(info.getComponentName().getPackageName(), user, info.getComponentName().getClassName());
                if (applicationInfo2 == null) {
                    add(new AppInfo(context, info, user, this.mIconCache));
                } else {
                    this.mIconCache.getTitleAndIcon(applicationInfo2, info, true);
                    this.modified.add(applicationInfo2);
                }
            }
            return;
        }
        for (int i2 = this.data.size() - 1; i2 >= 0; i2--) {
            AppInfo applicationInfo3 = this.data.get(i2);
            ComponentName component2 = applicationInfo3.intent.getComponent();
            if (user.equals(applicationInfo3.user) && packageName.equals(component2.getPackageName())) {
                if (LauncherLog.DEBUG) {
                    LauncherLog.d("AllAppsList", "Remove application from launcher: component = " + component2);
                }
                this.removed.add(applicationInfo3);
                this.mIconCache.remove(component2, user);
                this.data.remove(i2);
            }
        }
    }

    static boolean findActivity(List<LauncherActivityInfoCompat> apps, ComponentName component) {
        for (LauncherActivityInfoCompat info : apps) {
            if (info.getComponentName().equals(component)) {
                return true;
            }
        }
        return false;
    }

    static boolean packageHasActivities(Context context, String packageName, UserHandleCompat user) {
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        return launcherApps.getActivityList(packageName, user).size() > 0;
    }

    private static boolean findActivity(ArrayList<AppInfo> apps, ComponentName component, UserHandleCompat user) {
        int N = apps.size();
        for (int i = 0; i < N; i++) {
            AppInfo info = apps.get(i);
            if (info.user.equals(user) && info.componentName.equals(component)) {
                return true;
            }
        }
        return false;
    }

    private AppInfo findApplicationInfoLocked(String packageName, UserHandleCompat user, String className) {
        for (AppInfo info : this.data) {
            ComponentName component = info.intent.getComponent();
            if (user.equals(info.user) && packageName.equals(component.getPackageName()) && className.equals(component.getClassName())) {
                return info;
            }
        }
        return null;
    }
}
