package com.android.launcher3.compat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import com.android.launcher3.Utilities;
import java.util.List;

public abstract class LauncherAppsCompat {
    public static final String ACTION_MANAGED_PROFILE_ADDED = "android.intent.action.MANAGED_PROFILE_ADDED";
    public static final String ACTION_MANAGED_PROFILE_AVAILABLE = "android.intent.action.MANAGED_PROFILE_AVAILABLE";
    public static final String ACTION_MANAGED_PROFILE_REMOVED = "android.intent.action.MANAGED_PROFILE_REMOVED";
    public static final String ACTION_MANAGED_PROFILE_UNAVAILABLE = "android.intent.action.MANAGED_PROFILE_UNAVAILABLE";
    private static LauncherAppsCompat sInstance;
    private static Object sInstanceLock = new Object();

    public interface OnAppsChangedCallbackCompat {
        void onPackageAdded(String str, UserHandleCompat userHandleCompat);

        void onPackageChanged(String str, UserHandleCompat userHandleCompat);

        void onPackageRemoved(String str, UserHandleCompat userHandleCompat);

        void onPackagesAvailable(String[] strArr, UserHandleCompat userHandleCompat, boolean z);

        void onPackagesSuspended(String[] strArr, UserHandleCompat userHandleCompat);

        void onPackagesUnavailable(String[] strArr, UserHandleCompat userHandleCompat, boolean z);

        void onPackagesUnsuspended(String[] strArr, UserHandleCompat userHandleCompat);
    }

    public abstract void addOnAppsChangedCallback(OnAppsChangedCallbackCompat onAppsChangedCallbackCompat);

    public abstract List<LauncherActivityInfoCompat> getActivityList(String str, UserHandleCompat userHandleCompat);

    public abstract boolean isActivityEnabledForProfile(ComponentName componentName, UserHandleCompat userHandleCompat);

    public abstract boolean isPackageEnabledForProfile(String str, UserHandleCompat userHandleCompat);

    public abstract boolean isPackageSuspendedForProfile(String str, UserHandleCompat userHandleCompat);

    public abstract void removeOnAppsChangedCallback(OnAppsChangedCallbackCompat onAppsChangedCallbackCompat);

    public abstract LauncherActivityInfoCompat resolveActivity(Intent intent, UserHandleCompat userHandleCompat);

    public abstract void showAppDetailsForProfile(ComponentName componentName, UserHandleCompat userHandleCompat);

    public abstract void startActivityForProfile(ComponentName componentName, UserHandleCompat userHandleCompat, Rect rect, Bundle bundle);

    protected LauncherAppsCompat() {
    }

    public static LauncherAppsCompat getInstance(Context context) {
        LauncherAppsCompat launcherAppsCompat;
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                if (Utilities.ATLEAST_LOLLIPOP) {
                    sInstance = new LauncherAppsCompatVL(context.getApplicationContext());
                } else {
                    sInstance = new LauncherAppsCompatV16(context.getApplicationContext());
                }
            }
            launcherAppsCompat = sInstance;
        }
        return launcherAppsCompat;
    }
}
