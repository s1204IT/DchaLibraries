package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.os.Process;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.util.FlagOp;
import com.android.launcher3.util.ItemInfoMatcher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes.dex */
public class AllAppsList {
    public static final int DEFAULT_APPLICATIONS_NUMBER = 42;
    private static final String TAG = "AllAppsList";
    private AppFilter mAppFilter;
    private IconCache mIconCache;
    public final ArrayList<AppInfo> data = new ArrayList<>(42);
    public ArrayList<AppInfo> added = new ArrayList<>(42);
    public ArrayList<AppInfo> removed = new ArrayList<>();
    public ArrayList<AppInfo> modified = new ArrayList<>();

    public AllAppsList(IconCache iconCache, AppFilter appFilter) {
        this.mIconCache = iconCache;
        this.mAppFilter = appFilter;
    }

    public void add(AppInfo appInfo, LauncherActivityInfo launcherActivityInfo) {
        if (!this.mAppFilter.shouldShowApp(appInfo.componentName) || findAppInfo(appInfo.componentName, appInfo.user) != null) {
            return;
        }
        this.mIconCache.getTitleAndIcon(appInfo, launcherActivityInfo, true);
        this.data.add(appInfo);
        this.added.add(appInfo);
    }

    public void addPromiseApp(Context context, PackageInstallerCompat.PackageInstallInfo packageInstallInfo) {
        if (LauncherAppsCompat.getInstance(context).getApplicationInfo(packageInstallInfo.packageName, 0, Process.myUserHandle()) == null) {
            PromiseAppInfo promiseAppInfo = new PromiseAppInfo(packageInstallInfo);
            this.mIconCache.getTitleAndIcon(promiseAppInfo, promiseAppInfo.usingLowResIcon);
            this.data.add(promiseAppInfo);
            this.added.add(promiseAppInfo);
        }
    }

    public void removePromiseApp(AppInfo appInfo) {
        this.data.remove(appInfo);
    }

    public void clear() {
        this.data.clear();
        this.added.clear();
        this.removed.clear();
        this.modified.clear();
    }

    public int size() {
        return this.data.size();
    }

    public AppInfo get(int i) {
        return this.data.get(i);
    }

    public void addPackage(Context context, String str, UserHandle userHandle) {
        for (LauncherActivityInfo launcherActivityInfo : LauncherAppsCompat.getInstance(context).getActivityList(str, userHandle)) {
            add(new AppInfo(context, launcherActivityInfo, userHandle), launcherActivityInfo);
        }
    }

    public void removePackage(String str, UserHandle userHandle) {
        ArrayList<AppInfo> arrayList = this.data;
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            AppInfo appInfo = arrayList.get(size);
            if (appInfo.user.equals(userHandle) && str.equals(appInfo.componentName.getPackageName())) {
                this.removed.add(appInfo);
                arrayList.remove(size);
            }
        }
    }

    public void updateDisabledFlags(ItemInfoMatcher itemInfoMatcher, FlagOp flagOp) {
        ArrayList<AppInfo> arrayList = this.data;
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            AppInfo appInfo = arrayList.get(size);
            if (itemInfoMatcher.matches(appInfo, appInfo.componentName)) {
                appInfo.runtimeStatusFlags = flagOp.apply(appInfo.runtimeStatusFlags);
                this.modified.add(appInfo);
            }
        }
    }

    public void updateIconsAndLabels(HashSet<String> hashSet, UserHandle userHandle, ArrayList<AppInfo> arrayList) {
        Iterator<AppInfo> it = this.data.iterator();
        while (it.hasNext()) {
            AppInfo next = it.next();
            if (next.user.equals(userHandle) && hashSet.contains(next.componentName.getPackageName())) {
                this.mIconCache.updateTitleAndIcon(next);
                arrayList.add(next);
            }
        }
    }

    public void updatePackage(Context context, String str, UserHandle userHandle) {
        List<LauncherActivityInfo> activityList = LauncherAppsCompat.getInstance(context).getActivityList(str, userHandle);
        if (activityList.size() > 0) {
            for (int size = this.data.size() - 1; size >= 0; size--) {
                AppInfo appInfo = this.data.get(size);
                if (userHandle.equals(appInfo.user) && str.equals(appInfo.componentName.getPackageName()) && !findActivity(activityList, appInfo.componentName)) {
                    Log.w(TAG, "Shortcut will be removed due to app component name change.");
                    this.removed.add(appInfo);
                    this.data.remove(size);
                }
            }
            for (LauncherActivityInfo launcherActivityInfo : activityList) {
                AppInfo findAppInfo = findAppInfo(launcherActivityInfo.getComponentName(), userHandle);
                if (findAppInfo == null) {
                    add(new AppInfo(context, launcherActivityInfo, userHandle), launcherActivityInfo);
                } else {
                    this.mIconCache.getTitleAndIcon(findAppInfo, launcherActivityInfo, true);
                    this.modified.add(findAppInfo);
                }
            }
            return;
        }
        for (int size2 = this.data.size() - 1; size2 >= 0; size2--) {
            AppInfo appInfo2 = this.data.get(size2);
            if (userHandle.equals(appInfo2.user) && str.equals(appInfo2.componentName.getPackageName())) {
                this.removed.add(appInfo2);
                this.mIconCache.remove(appInfo2.componentName, userHandle);
                this.data.remove(size2);
            }
        }
    }

    private static boolean findActivity(List<LauncherActivityInfo> list, ComponentName componentName) {
        for (LauncherActivityInfo launcherActivityInfo : list) {
            if (launcherActivityInfo.getComponentName().equals(componentName)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private AppInfo findAppInfo(@NonNull ComponentName componentName, @NonNull UserHandle userHandle) {
        Iterator<AppInfo> it = this.data.iterator();
        while (it.hasNext()) {
            AppInfo next = it.next();
            if (componentName.equals(next.componentName) && userHandle.equals(next.user)) {
                return next;
            }
        }
        return null;
    }
}
