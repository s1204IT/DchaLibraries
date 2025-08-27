package com.android.settingslib.wrapper;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import java.util.List;

/* loaded from: classes.dex */
public class PackageManagerWrapper {
    private final PackageManager mPm;

    public PackageManagerWrapper(PackageManager packageManager) {
        this.mPm = packageManager;
    }

    public PackageManager getPackageManager() {
        return this.mPm;
    }

    public List<ApplicationInfo> getInstalledApplicationsAsUser(int i, int i2) {
        return this.mPm.getInstalledApplicationsAsUser(i, i2);
    }

    public List<PackageInfo> getInstalledPackagesAsUser(int i, int i2) {
        return this.mPm.getInstalledPackagesAsUser(i, i2);
    }

    public boolean hasSystemFeature(String str) {
        return this.mPm.hasSystemFeature(str);
    }

    public List<ResolveInfo> queryIntentActivitiesAsUser(Intent intent, int i, int i2) {
        return this.mPm.queryIntentActivitiesAsUser(intent, i, i2);
    }

    public int getInstallReason(String str, UserHandle userHandle) {
        return this.mPm.getInstallReason(str, userHandle);
    }

    public ApplicationInfo getApplicationInfoAsUser(String str, int i, int i2) throws PackageManager.NameNotFoundException {
        return this.mPm.getApplicationInfoAsUser(str, i, i2);
    }

    public boolean setDefaultBrowserPackageNameAsUser(String str, int i) {
        return this.mPm.setDefaultBrowserPackageNameAsUser(str, i);
    }

    public String getDefaultBrowserPackageNameAsUser(int i) {
        return this.mPm.getDefaultBrowserPackageNameAsUser(i);
    }

    public ComponentName getHomeActivities(List<ResolveInfo> list) {
        return this.mPm.getHomeActivities(list);
    }

    public List<ResolveInfo> queryIntentServicesAsUser(Intent intent, int i, int i2) {
        return this.mPm.queryIntentServicesAsUser(intent, i, i2);
    }

    public List<ResolveInfo> queryIntentServices(Intent intent, int i) {
        return this.mPm.queryIntentServices(intent, i);
    }

    public void replacePreferredActivity(IntentFilter intentFilter, int i, ComponentName[] componentNameArr, ComponentName componentName) {
        this.mPm.replacePreferredActivity(intentFilter, i, componentNameArr, componentName);
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, int i) {
        return this.mPm.queryIntentActivities(intent, i);
    }

    public VolumeInfo getPrimaryStorageCurrentVolume() {
        return this.mPm.getPrimaryStorageCurrentVolume();
    }

    public void deletePackageAsUser(String str, IPackageDeleteObserver iPackageDeleteObserver, int i, int i2) {
        this.mPm.deletePackageAsUser(str, iPackageDeleteObserver, i, i2);
    }

    public int getPackageUidAsUser(String str, int i) throws PackageManager.NameNotFoundException {
        return this.mPm.getPackageUidAsUser(str, i);
    }

    public void setApplicationEnabledSetting(String str, int i, int i2) {
        this.mPm.setApplicationEnabledSetting(str, i, i2);
    }

    public int getApplicationEnabledSetting(String str) {
        return this.mPm.getApplicationEnabledSetting(str);
    }

    public ApplicationInfo getApplicationInfo(String str, int i) throws PackageManager.NameNotFoundException {
        return this.mPm.getApplicationInfo(str, i);
    }

    public CharSequence getApplicationLabel(ApplicationInfo applicationInfo) {
        return this.mPm.getApplicationLabel(applicationInfo);
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int i) {
        return this.mPm.queryBroadcastReceivers(intent, i);
    }
}
