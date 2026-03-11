package com.android.settings.fuelgauge.batterytip.tips;

import android.app.AppOpsManager;
import android.content.Context;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import java.util.function.Predicate;

public class AppRestrictionPredicate implements Predicate<AppInfo> {
    private AppOpsManager mAppOpsManager;

    public AppRestrictionPredicate(Context context) {
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
    }

    @Override
    public boolean test(AppInfo appInfo) {
        return this.mAppOpsManager.checkOpNoThrow(70, appInfo.uid, appInfo.packageName) == 1;
    }
}
