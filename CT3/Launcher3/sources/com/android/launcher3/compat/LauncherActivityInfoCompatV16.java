package com.android.launcher3.compat;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class LauncherActivityInfoCompatV16 extends LauncherActivityInfoCompat {
    private final ActivityInfo mActivityInfo;
    private final ComponentName mComponentName;
    private final PackageManager mPm;
    private final ResolveInfo mResolveInfo;

    LauncherActivityInfoCompatV16(Context context, ResolveInfo info) {
        this.mResolveInfo = info;
        this.mActivityInfo = info.activityInfo;
        this.mComponentName = new ComponentName(this.mActivityInfo.packageName, this.mActivityInfo.name);
        this.mPm = context.getPackageManager();
    }

    @Override
    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    @Override
    public UserHandleCompat getUser() {
        return UserHandleCompat.myUserHandle();
    }

    @Override
    public CharSequence getLabel() {
        try {
            return this.mResolveInfo.loadLabel(this.mPm);
        } catch (SecurityException e) {
            Log.e("LAInfoCompat", "Failed to extract app display name from resolve info", e);
            return "";
        }
    }

    @Override
    public Drawable getIcon(int density) {
        int iconRes = this.mResolveInfo.getIconResource();
        Drawable icon = null;
        if (density != 0 && iconRes != 0) {
            try {
                Resources resources = this.mPm.getResourcesForApplication(this.mActivityInfo.applicationInfo);
                icon = resources.getDrawableForDensity(iconRes, density);
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            }
        }
        if (icon == null) {
            icon = this.mResolveInfo.loadIcon(this.mPm);
        }
        if (icon == null) {
            Resources resources2 = Resources.getSystem();
            return resources2.getDrawableForDensity(R.mipmap.sym_def_app_icon, density);
        }
        return icon;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return this.mActivityInfo.applicationInfo;
    }

    @Override
    public long getFirstInstallTime() {
        try {
            PackageInfo info = this.mPm.getPackageInfo(this.mActivityInfo.packageName, 0);
            if (info != null) {
                return info.firstInstallTime;
            }
            return 0L;
        } catch (PackageManager.NameNotFoundException e) {
            return 0L;
        }
    }

    public String getName() {
        return this.mActivityInfo.name;
    }
}
