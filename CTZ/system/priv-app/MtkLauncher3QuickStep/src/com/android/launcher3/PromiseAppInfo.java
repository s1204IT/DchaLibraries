package com.android.launcher3;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.util.PackageManagerHelper;

/* loaded from: classes.dex */
public class PromiseAppInfo extends AppInfo {
    public int level = 0;

    public PromiseAppInfo(@NonNull PackageInstallerCompat.PackageInstallInfo packageInstallInfo) {
        this.componentName = packageInstallInfo.componentName;
        this.intent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setComponent(this.componentName).setFlags(270532608);
    }

    @Override // com.android.launcher3.AppInfo
    public ShortcutInfo makeShortcut() {
        ShortcutInfo shortcutInfo = new ShortcutInfo(this);
        shortcutInfo.setInstallProgress(this.level);
        shortcutInfo.status |= 2;
        shortcutInfo.status |= 8;
        return shortcutInfo;
    }

    public Intent getMarketIntent(Context context) {
        return new PackageManagerHelper(context).getMarketIntent(this.componentName.getPackageName());
    }
}
