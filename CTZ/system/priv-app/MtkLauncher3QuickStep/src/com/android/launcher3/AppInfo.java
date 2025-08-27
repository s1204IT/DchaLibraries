package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.os.Process;
import android.os.UserHandle;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.PackageManagerHelper;

/* loaded from: classes.dex */
public class AppInfo extends ItemInfoWithIcon {
    public ComponentName componentName;
    public Intent intent;

    public AppInfo() {
        this.itemType = 0;
    }

    @Override // com.android.launcher3.ItemInfo
    public Intent getIntent() {
        return this.intent;
    }

    public AppInfo(Context context, LauncherActivityInfo launcherActivityInfo, UserHandle userHandle) {
        this(launcherActivityInfo, userHandle, UserManagerCompat.getInstance(context).isQuietModeEnabled(userHandle));
    }

    public AppInfo(LauncherActivityInfo launcherActivityInfo, UserHandle userHandle, boolean z) {
        this.componentName = launcherActivityInfo.getComponentName();
        this.container = -1L;
        this.user = userHandle;
        this.intent = makeLaunchIntent(launcherActivityInfo);
        if (z) {
            this.runtimeStatusFlags |= 8;
        }
        updateRuntimeFlagsForActivityTarget(this, launcherActivityInfo);
    }

    public AppInfo(AppInfo appInfo) {
        super(appInfo);
        this.componentName = appInfo.componentName;
        this.title = Utilities.trim(appInfo.title);
        this.intent = new Intent(appInfo.intent);
    }

    @Override // com.android.launcher3.ItemInfo
    protected String dumpProperties() {
        return super.dumpProperties() + " componentName=" + this.componentName;
    }

    public ShortcutInfo makeShortcut() {
        return new ShortcutInfo(this);
    }

    public ComponentKey toComponentKey() {
        return new ComponentKey(this.componentName, this.user);
    }

    public static Intent makeLaunchIntent(LauncherActivityInfo launcherActivityInfo) {
        return makeLaunchIntent(launcherActivityInfo.getComponentName());
    }

    public static Intent makeLaunchIntent(ComponentName componentName) {
        return new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setComponent(componentName).setFlags(270532608);
    }

    public static void updateRuntimeFlagsForActivityTarget(ItemInfoWithIcon itemInfoWithIcon, LauncherActivityInfo launcherActivityInfo) {
        ApplicationInfo applicationInfo = launcherActivityInfo.getApplicationInfo();
        if (PackageManagerHelper.isAppSuspended(applicationInfo)) {
            itemInfoWithIcon.runtimeStatusFlags |= 4;
        }
        itemInfoWithIcon.runtimeStatusFlags |= (applicationInfo.flags & 1) == 0 ? 128 : 64;
        if (Utilities.ATLEAST_OREO && applicationInfo.targetSdkVersion >= 26 && Process.myUserHandle().equals(launcherActivityInfo.getUser())) {
            itemInfoWithIcon.runtimeStatusFlags |= 256;
        }
    }
}
