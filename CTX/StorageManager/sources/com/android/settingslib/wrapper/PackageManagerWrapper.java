package com.android.settingslib.wrapper;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import java.util.List;

public class PackageManagerWrapper {
    private final PackageManager mPm;

    public PackageManagerWrapper(PackageManager packageManager) {
        this.mPm = packageManager;
    }

    public List<ApplicationInfo> getInstalledApplicationsAsUser(int i, int i2) {
        return this.mPm.getInstalledApplicationsAsUser(i, i2);
    }

    public ComponentName getHomeActivities(List<ResolveInfo> list) {
        return this.mPm.getHomeActivities(list);
    }

    public PackageInfo getPackageInfo(String str, int i) throws PackageManager.NameNotFoundException {
        return this.mPm.getPackageInfo(str, i);
    }

    public Drawable getUserBadgedIcon(ApplicationInfo applicationInfo) {
        return this.mPm.getUserBadgedIcon(this.mPm.loadUnbadgedItemIcon(applicationInfo, applicationInfo), new UserHandle(UserHandle.getUserId(applicationInfo.uid)));
    }

    public CharSequence loadLabel(ApplicationInfo applicationInfo) {
        return applicationInfo.loadLabel(this.mPm);
    }
}
