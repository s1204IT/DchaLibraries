package com.android.settings.fuelgauge.batterytip.detectors;

import android.content.Context;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.AppLabelPredicate;
import com.android.settings.fuelgauge.batterytip.tips.AppRestrictionPredicate;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class RestrictAppDetector {
    static final boolean USE_FAKE_DATA = false;
    private AppLabelPredicate mAppLabelPredicate;
    private AppRestrictionPredicate mAppRestrictionPredicate;
    BatteryDatabaseManager mBatteryDatabaseManager;
    private Context mContext;
    private BatteryTipPolicy mPolicy;

    public RestrictAppDetector(Context context, BatteryTipPolicy batteryTipPolicy) {
        this.mContext = context;
        this.mPolicy = batteryTipPolicy;
        this.mBatteryDatabaseManager = BatteryDatabaseManager.getInstance(context);
        this.mAppRestrictionPredicate = new AppRestrictionPredicate(context);
        this.mAppLabelPredicate = new AppLabelPredicate(context);
    }

    public BatteryTip detect() {
        if (this.mPolicy.appRestrictionEnabled) {
            long jCurrentTimeMillis = System.currentTimeMillis() - 86400000;
            List<AppInfo> listQueryAllAnomalies = this.mBatteryDatabaseManager.queryAllAnomalies(jCurrentTimeMillis, 0);
            listQueryAllAnomalies.removeIf(this.mAppLabelPredicate.or(this.mAppRestrictionPredicate));
            if (!listQueryAllAnomalies.isEmpty()) {
                return new RestrictAppTip(0, listQueryAllAnomalies);
            }
            List<AppInfo> listQueryAllAnomalies2 = this.mBatteryDatabaseManager.queryAllAnomalies(jCurrentTimeMillis, 2);
            listQueryAllAnomalies2.removeIf(this.mAppLabelPredicate.or(this.mAppRestrictionPredicate.negate()));
            return new RestrictAppTip(listQueryAllAnomalies2.isEmpty() ? 2 : 1, listQueryAllAnomalies2);
        }
        return new RestrictAppTip(2, new ArrayList());
    }
}
