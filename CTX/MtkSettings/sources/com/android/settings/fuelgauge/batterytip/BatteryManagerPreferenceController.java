package com.android.settings.fuelgauge.batterytip;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

public class BatteryManagerPreferenceController extends BasePreferenceController {
    private static final String KEY_BATTERY_MANAGER = "smart_battery_manager";
    private static final int ON = 1;
    private AppOpsManager mAppOpsManager;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private UserManager mUserManager;

    public BatteryManagerPreferenceController(Context context) {
        super(context, KEY_BATTERY_MANAGER);
        this.mPowerUsageFeatureProvider = FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override
    public void updateState(Preference preference) {
        String str;
        super.updateState(preference);
        int size = BatteryTipUtils.getRestrictedAppsList(this.mAppOpsManager, this.mUserManager).size();
        if (this.mPowerUsageFeatureProvider.isSmartBatterySupported()) {
            str = "adaptive_battery_management_enabled";
        } else {
            str = "app_auto_restriction_enabled";
        }
        updateSummary(preference, Settings.Global.getInt(this.mContext.getContentResolver(), str, 1) == 1, size);
    }

    void updateSummary(Preference preference, boolean z, int i) {
        if (i > 0) {
            preference.setSummary(this.mContext.getResources().getQuantityString(R.plurals.battery_manager_app_restricted, i, Integer.valueOf(i)));
        } else if (z) {
            preference.setSummary(R.string.battery_manager_on);
        } else {
            preference.setSummary(R.string.battery_manager_off);
        }
    }
}
