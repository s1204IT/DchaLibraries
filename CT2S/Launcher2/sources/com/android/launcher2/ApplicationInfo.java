package com.android.launcher2;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.graphics.Bitmap;
import android.os.UserHandle;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;

class ApplicationInfo extends ItemInfo {
    ComponentName componentName;
    long firstInstallTime;
    int flags = 0;
    Bitmap iconBitmap;
    Intent intent;

    ApplicationInfo() {
        this.itemType = 1;
    }

    public ApplicationInfo(LauncherActivityInfo info, UserHandle user, IconCache iconCache, HashMap<Object, CharSequence> labelCache) {
        this.componentName = info.getComponentName();
        this.container = -1L;
        int appFlags = info.getApplicationInfo().flags;
        if ((appFlags & 1) == 0) {
            this.flags |= 1;
        }
        if ((appFlags & 128) != 0) {
            this.flags |= 2;
        }
        this.firstInstallTime = info.getFirstInstallTime();
        iconCache.getTitleAndIcon(this, info, labelCache);
        this.intent = new Intent("android.intent.action.MAIN");
        this.intent.addCategory("android.intent.category.LAUNCHER");
        this.intent.setComponent(info.getComponentName());
        this.intent.putExtra("profile", user);
        this.intent.setFlags(270532608);
        this.itemType = 0;
        updateUser(this.intent);
    }

    @Override
    public String toString() {
        return "ApplicationInfo(title=" + this.title.toString() + " P=" + this.user + ")";
    }

    public static void dumpApplicationInfoList(String tag, String label, ArrayList<ApplicationInfo> list) {
        Log.d(tag, label + " size=" + list.size());
        for (ApplicationInfo info : list) {
            Log.d(tag, "   title=\"" + ((Object) info.title) + "\" iconBitmap=" + info.iconBitmap + " firstInstallTime=" + info.firstInstallTime);
        }
    }

    public ShortcutInfo makeShortcut() {
        return new ShortcutInfo(this);
    }
}
