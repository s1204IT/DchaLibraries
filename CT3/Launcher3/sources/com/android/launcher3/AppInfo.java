package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.PackageManagerHelper;
import java.util.ArrayList;
import java.util.Arrays;

public class AppInfo extends ItemInfo {
    public ComponentName componentName;
    int flags;
    public Bitmap iconBitmap;
    public Intent intent;
    int isDisabled;
    boolean usingLowResIcon;

    AppInfo() {
        this.flags = 0;
        this.isDisabled = 0;
        this.itemType = 1;
    }

    @Override
    public Intent getIntent() {
        return this.intent;
    }

    public AppInfo(Context context, LauncherActivityInfoCompat info, UserHandleCompat user, IconCache iconCache) {
        this(context, info, user, iconCache, UserManagerCompat.getInstance(context).isQuietModeEnabled(user));
    }

    public AppInfo(Context context, LauncherActivityInfoCompat info, UserHandleCompat user, IconCache iconCache, boolean quietModeEnabled) {
        this.flags = 0;
        this.isDisabled = 0;
        this.componentName = info.getComponentName();
        this.container = -1L;
        this.flags = initFlags(info);
        if (PackageManagerHelper.isAppSuspended(info.getApplicationInfo())) {
            this.isDisabled |= 4;
        }
        if (quietModeEnabled) {
            this.isDisabled |= 8;
        }
        iconCache.getTitleAndIcon(this, info, true);
        this.intent = makeLaunchIntent(context, info, user);
        this.user = user;
    }

    public static int initFlags(LauncherActivityInfoCompat info) {
        int appFlags = info.getApplicationInfo().flags;
        if ((appFlags & 1) != 0) {
            return 0;
        }
        if ((appFlags & 128) == 0) {
            return 1;
        }
        int flags = 1 | 2;
        return flags;
    }

    @Override
    public String toString() {
        return "ApplicationInfo(title=" + this.title + " id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screenId + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " dropPos=" + Arrays.toString(this.dropPos) + " user=" + this.user + ")";
    }

    public static void dumpApplicationInfoList(String tag, String label, ArrayList<AppInfo> list) {
        Log.d(tag, label + " size=" + list.size());
        for (AppInfo info : list) {
            Log.d(tag, "   title=\"" + info.title + "\" iconBitmap=" + info.iconBitmap + " componentName=" + info.componentName.getPackageName());
        }
    }

    public ShortcutInfo makeShortcut() {
        return new ShortcutInfo(this);
    }

    public ComponentKey toComponentKey() {
        return new ComponentKey(this.componentName, this.user);
    }

    public static Intent makeLaunchIntent(Context context, LauncherActivityInfoCompat info, UserHandleCompat user) {
        long serialNumber = UserManagerCompat.getInstance(context).getSerialNumberForUser(user);
        return new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setComponent(info.getComponentName()).setFlags(270532608).putExtra("profile", serialNumber);
    }

    @Override
    public boolean isDisabled() {
        return this.isDisabled != 0;
    }
}
