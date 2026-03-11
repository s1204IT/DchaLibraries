package com.android.launcher2;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.UserHandle;
import java.util.ArrayList;
import java.util.List;

class AllAppsList {
    private IconCache mIconCache;
    public ArrayList<ApplicationInfo> data = new ArrayList<>(42);
    public ArrayList<ApplicationInfo> added = new ArrayList<>(42);
    public ArrayList<ApplicationInfo> removed = new ArrayList<>();
    public ArrayList<ApplicationInfo> modified = new ArrayList<>();

    public AllAppsList(IconCache iconCache) {
        this.mIconCache = iconCache;
    }

    public void add(ApplicationInfo info) {
        if (!findActivity(this.data, info.componentName, info.user)) {
            this.data.add(info);
            this.added.add(info);
        }
    }

    public void clear() {
        this.data.clear();
        this.added.clear();
        this.removed.clear();
        this.modified.clear();
    }

    public void addPackage(Context context, String packageName, UserHandle user) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService("launcherapps");
        List<LauncherActivityInfo> matches = launcherApps.getActivityList(packageName, user);
        for (LauncherActivityInfo info : matches) {
            add(new ApplicationInfo(info, user, this.mIconCache, null));
        }
    }

    public void removePackage(String packageName, UserHandle user) {
        List<ApplicationInfo> data = this.data;
        for (int i = data.size() - 1; i >= 0; i--) {
            ApplicationInfo info = data.get(i);
            ComponentName component = info.intent.getComponent();
            if (info.user.equals(user) && packageName.equals(component.getPackageName())) {
                this.removed.add(info);
                data.remove(i);
            }
        }
        this.mIconCache.flush();
    }

    public void updatePackage(Context context, String packageName, UserHandle user) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService("launcherapps");
        List<LauncherActivityInfo> matches = launcherApps.getActivityList(packageName, user);
        if (matches.size() > 0) {
            for (int i = this.data.size() - 1; i >= 0; i--) {
                ApplicationInfo applicationInfo = this.data.get(i);
                ComponentName component = applicationInfo.intent.getComponent();
                if (user.equals(applicationInfo.user) && packageName.equals(component.getPackageName()) && !findActivity(matches, component, user)) {
                    this.removed.add(applicationInfo);
                    this.mIconCache.remove(component);
                    this.data.remove(i);
                }
            }
            int count = matches.size();
            for (int i2 = 0; i2 < count; i2++) {
                LauncherActivityInfo info = matches.get(i2);
                ApplicationInfo applicationInfo2 = findApplicationInfoLocked(info.getComponentName().getPackageName(), info.getComponentName().getShortClassName(), user);
                if (applicationInfo2 == null) {
                    add(new ApplicationInfo(info, user, this.mIconCache, null));
                } else {
                    this.mIconCache.remove(applicationInfo2.componentName);
                    this.mIconCache.getTitleAndIcon(applicationInfo2, info, null);
                    this.modified.add(applicationInfo2);
                }
            }
            return;
        }
        for (int i3 = this.data.size() - 1; i3 >= 0; i3--) {
            ApplicationInfo applicationInfo3 = this.data.get(i3);
            ComponentName component2 = applicationInfo3.intent.getComponent();
            if (user.equals(applicationInfo3.user) && packageName.equals(component2.getPackageName())) {
                this.removed.add(applicationInfo3);
                this.mIconCache.remove(component2);
                this.data.remove(i3);
            }
        }
    }

    private static boolean findActivity(List<LauncherActivityInfo> apps, ComponentName component, UserHandle user) {
        for (LauncherActivityInfo info : apps) {
            if (info.getUser().equals(user) && info.getComponentName().equals(component)) {
                return true;
            }
        }
        return false;
    }

    private static boolean findActivity(ArrayList<ApplicationInfo> apps, ComponentName component, UserHandle user) {
        int N = apps.size();
        for (int i = 0; i < N; i++) {
            ApplicationInfo info = apps.get(i);
            if (info.user.equals(user) && info.componentName.equals(component)) {
                return true;
            }
        }
        return false;
    }

    private ApplicationInfo findApplicationInfoLocked(String packageName, String className, UserHandle user) {
        for (ApplicationInfo info : this.data) {
            ComponentName component = info.intent.getComponent();
            if (user.equals(info.user) && packageName.equals(component.getPackageName()) && className.equals(component.getClassName())) {
                return info;
            }
        }
        return null;
    }
}
