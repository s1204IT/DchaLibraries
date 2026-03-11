package com.android.settings.fuelgauge.batterytip.tips;

import android.app.AppOpsManager;
import android.content.Context;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import java.util.function.Predicate;

public class AppLabelPredicate implements Predicate<AppInfo> {
    private AppOpsManager mAppOpsManager;
    private Context mContext;

    public AppLabelPredicate(Context context) {
        this.mContext = context;
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
    }

    @Override
    public boolean test(AppInfo appInfo) {
        return Utils.getApplicationLabel(this.mContext, appInfo.packageName) == null;
    }
}
