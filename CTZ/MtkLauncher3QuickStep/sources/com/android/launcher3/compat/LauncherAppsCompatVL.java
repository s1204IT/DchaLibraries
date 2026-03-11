package com.android.launcher3.compat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.ArrayMap;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.ShortcutConfigActivityInfo;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.util.PackageUserKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LauncherAppsCompatVL extends LauncherAppsCompat {
    private final ArrayMap<LauncherAppsCompat.OnAppsChangedCallbackCompat, WrappedCallback> mCallbacks = new ArrayMap<>();
    protected final Context mContext;
    protected final LauncherApps mLauncherApps;

    LauncherAppsCompatVL(Context context) {
        this.mContext = context;
        this.mLauncherApps = (LauncherApps) context.getSystemService("launcherapps");
    }

    @Override
    public List<LauncherActivityInfo> getActivityList(String str, UserHandle userHandle) {
        return this.mLauncherApps.getActivityList(str, userHandle);
    }

    @Override
    public LauncherActivityInfo resolveActivity(Intent intent, UserHandle userHandle) {
        return this.mLauncherApps.resolveActivity(intent, userHandle);
    }

    @Override
    public void startActivityForProfile(ComponentName componentName, UserHandle userHandle, Rect rect, Bundle bundle) {
        this.mLauncherApps.startMainActivity(componentName, userHandle, rect, bundle);
    }

    @Override
    public ApplicationInfo getApplicationInfo(String str, int i, UserHandle userHandle) {
        boolean zEquals = Process.myUserHandle().equals(userHandle);
        if (!zEquals && i == 0) {
            List<LauncherActivityInfo> activityList = this.mLauncherApps.getActivityList(str, userHandle);
            if (activityList.size() > 0) {
                return activityList.get(0).getApplicationInfo();
            }
            return null;
        }
        try {
            ApplicationInfo applicationInfo = this.mContext.getPackageManager().getApplicationInfo(str, i);
            if (!zEquals || (applicationInfo.flags & 8388608) != 0) {
                if (applicationInfo.enabled) {
                    return applicationInfo;
                }
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void showAppDetailsForProfile(ComponentName componentName, UserHandle userHandle, Rect rect, Bundle bundle) {
        this.mLauncherApps.startAppDetailsActivity(componentName, userHandle, rect, bundle);
    }

    @Override
    public void addOnAppsChangedCallback(LauncherAppsCompat.OnAppsChangedCallbackCompat onAppsChangedCallbackCompat) {
        WrappedCallback wrappedCallback = new WrappedCallback(onAppsChangedCallbackCompat);
        synchronized (this.mCallbacks) {
            this.mCallbacks.put(onAppsChangedCallbackCompat, wrappedCallback);
        }
        this.mLauncherApps.registerCallback(wrappedCallback);
    }

    @Override
    public void removeOnAppsChangedCallback(LauncherAppsCompat.OnAppsChangedCallbackCompat onAppsChangedCallbackCompat) {
        WrappedCallback wrappedCallbackRemove;
        synchronized (this.mCallbacks) {
            wrappedCallbackRemove = this.mCallbacks.remove(onAppsChangedCallbackCompat);
        }
        if (wrappedCallbackRemove != null) {
            this.mLauncherApps.unregisterCallback(wrappedCallbackRemove);
        }
    }

    @Override
    public boolean isPackageEnabledForProfile(String str, UserHandle userHandle) {
        return this.mLauncherApps.isPackageEnabled(str, userHandle);
    }

    @Override
    public boolean isActivityEnabledForProfile(ComponentName componentName, UserHandle userHandle) {
        return this.mLauncherApps.isActivityEnabled(componentName, userHandle);
    }

    private static class WrappedCallback extends LauncherApps.Callback {
        private final LauncherAppsCompat.OnAppsChangedCallbackCompat mCallback;

        public WrappedCallback(LauncherAppsCompat.OnAppsChangedCallbackCompat onAppsChangedCallbackCompat) {
            this.mCallback = onAppsChangedCallbackCompat;
        }

        @Override
        public void onPackageRemoved(String str, UserHandle userHandle) {
            this.mCallback.onPackageRemoved(str, userHandle);
        }

        @Override
        public void onPackageAdded(String str, UserHandle userHandle) {
            this.mCallback.onPackageAdded(str, userHandle);
        }

        @Override
        public void onPackageChanged(String str, UserHandle userHandle) {
            this.mCallback.onPackageChanged(str, userHandle);
        }

        @Override
        public void onPackagesAvailable(String[] strArr, UserHandle userHandle, boolean z) {
            this.mCallback.onPackagesAvailable(strArr, userHandle, z);
        }

        @Override
        public void onPackagesUnavailable(String[] strArr, UserHandle userHandle, boolean z) {
            this.mCallback.onPackagesUnavailable(strArr, userHandle, z);
        }

        @Override
        public void onPackagesSuspended(String[] strArr, UserHandle userHandle) {
            this.mCallback.onPackagesSuspended(strArr, userHandle);
        }

        @Override
        public void onPackagesUnsuspended(String[] strArr, UserHandle userHandle) {
            this.mCallback.onPackagesUnsuspended(strArr, userHandle);
        }

        @Override
        public void onShortcutsChanged(@NonNull String str, @NonNull List<ShortcutInfo> list, @NonNull UserHandle userHandle) {
            ArrayList arrayList = new ArrayList(list.size());
            Iterator<ShortcutInfo> it = list.iterator();
            while (it.hasNext()) {
                arrayList.add(new ShortcutInfoCompat(it.next()));
            }
            this.mCallback.onShortcutsChanged(str, arrayList, userHandle);
        }
    }

    @Override
    public List<ShortcutConfigActivityInfo> getCustomShortcutActivityList(@Nullable PackageUserKey packageUserKey) {
        ArrayList arrayList = new ArrayList();
        if (packageUserKey != null && !packageUserKey.mUser.equals(Process.myUserHandle())) {
            return arrayList;
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(new Intent("android.intent.action.CREATE_SHORTCUT"), 0)) {
            if (packageUserKey == null || packageUserKey.mPackageName.equals(resolveInfo.activityInfo.packageName)) {
                arrayList.add(new ShortcutConfigActivityInfo.ShortcutConfigActivityInfoVL(resolveInfo.activityInfo, packageManager));
            }
        }
        return arrayList;
    }
}
