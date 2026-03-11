package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.UserHandle;
import com.android.launcher3.compat.LauncherAppsCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TargetApi(21)
public class LauncherAppsCompatVL extends LauncherAppsCompat {
    private Map<LauncherAppsCompat.OnAppsChangedCallbackCompat, WrappedCallback> mCallbacks = new HashMap();
    protected LauncherApps mLauncherApps;

    LauncherAppsCompatVL(Context context) {
        this.mLauncherApps = (LauncherApps) context.getSystemService("launcherapps");
    }

    @Override
    public List<LauncherActivityInfoCompat> getActivityList(String packageName, UserHandleCompat user) {
        List<LauncherActivityInfo> list = this.mLauncherApps.getActivityList(packageName, user.getUser());
        if (list.size() == 0) {
            return Collections.emptyList();
        }
        ArrayList<LauncherActivityInfoCompat> compatList = new ArrayList<>(list.size());
        for (LauncherActivityInfo info : list) {
            compatList.add(new LauncherActivityInfoCompatVL(info));
        }
        return compatList;
    }

    @Override
    public LauncherActivityInfoCompat resolveActivity(Intent intent, UserHandleCompat user) {
        LauncherActivityInfo activity = this.mLauncherApps.resolveActivity(intent, user.getUser());
        if (activity != null) {
            return new LauncherActivityInfoCompatVL(activity);
        }
        return null;
    }

    @Override
    public void startActivityForProfile(ComponentName component, UserHandleCompat user, Rect sourceBounds, Bundle opts) {
        this.mLauncherApps.startMainActivity(component, user.getUser(), sourceBounds, opts);
    }

    @Override
    public void showAppDetailsForProfile(ComponentName component, UserHandleCompat user) {
        this.mLauncherApps.startAppDetailsActivity(component, user.getUser(), null, null);
    }

    @Override
    public void addOnAppsChangedCallback(LauncherAppsCompat.OnAppsChangedCallbackCompat callback) {
        WrappedCallback wrappedCallback = new WrappedCallback(callback);
        synchronized (this.mCallbacks) {
            this.mCallbacks.put(callback, wrappedCallback);
        }
        this.mLauncherApps.registerCallback(wrappedCallback);
    }

    @Override
    public void removeOnAppsChangedCallback(LauncherAppsCompat.OnAppsChangedCallbackCompat callback) {
        WrappedCallback wrappedCallback;
        synchronized (this.mCallbacks) {
            wrappedCallback = this.mCallbacks.remove(callback);
        }
        if (wrappedCallback == null) {
            return;
        }
        this.mLauncherApps.unregisterCallback(wrappedCallback);
    }

    @Override
    public boolean isPackageEnabledForProfile(String packageName, UserHandleCompat user) {
        return this.mLauncherApps.isPackageEnabled(packageName, user.getUser());
    }

    @Override
    public boolean isActivityEnabledForProfile(ComponentName component, UserHandleCompat user) {
        return this.mLauncherApps.isActivityEnabled(component, user.getUser());
    }

    @Override
    public boolean isPackageSuspendedForProfile(String packageName, UserHandleCompat user) {
        return false;
    }

    private static class WrappedCallback extends LauncherApps.Callback {
        private LauncherAppsCompat.OnAppsChangedCallbackCompat mCallback;

        public WrappedCallback(LauncherAppsCompat.OnAppsChangedCallbackCompat callback) {
            this.mCallback = callback;
        }

        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            this.mCallback.onPackageRemoved(packageName, UserHandleCompat.fromUser(user));
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            this.mCallback.onPackageAdded(packageName, UserHandleCompat.fromUser(user));
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            this.mCallback.onPackageChanged(packageName, UserHandleCompat.fromUser(user));
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            this.mCallback.onPackagesAvailable(packageNames, UserHandleCompat.fromUser(user), replacing);
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
            this.mCallback.onPackagesUnavailable(packageNames, UserHandleCompat.fromUser(user), replacing);
        }

        @Override
        public void onPackagesSuspended(String[] packageNames, UserHandle user) {
            this.mCallback.onPackagesSuspended(packageNames, UserHandleCompat.fromUser(user));
        }

        @Override
        public void onPackagesUnsuspended(String[] packageNames, UserHandle user) {
            this.mCallback.onPackagesUnsuspended(packageNames, UserHandleCompat.fromUser(user));
        }
    }
}
