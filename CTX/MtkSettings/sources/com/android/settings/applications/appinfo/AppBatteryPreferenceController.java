package com.android.settings.applications.appinfo;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.BatteryEntry;
import com.android.settings.fuelgauge.BatteryStatsHelperLoader;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.ArrayList;
import java.util.List;

public class AppBatteryPreferenceController extends BasePreferenceController implements LoaderManager.LoaderCallbacks<BatteryStatsHelper>, LifecycleObserver, OnPause, OnResume {
    private static final String KEY_BATTERY = "battery";
    BatteryStatsHelper mBatteryHelper;
    private String mBatteryPercent;
    BatteryUtils mBatteryUtils;
    private final String mPackageName;
    private final AppInfoDashboardFragment mParent;
    private Preference mPreference;
    BatterySipper mSipper;

    public AppBatteryPreferenceController(Context context, AppInfoDashboardFragment appInfoDashboardFragment, String str, Lifecycle lifecycle) {
        super(context, KEY_BATTERY);
        this.mParent = appInfoDashboardFragment;
        this.mBatteryUtils = BatteryUtils.getInstance(this.mContext);
        this.mPackageName = str;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (this.mContext.getResources().getBoolean(R.bool.config_show_app_info_settings_battery)) {
            return 0;
        }
        return 1;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
        this.mPreference.setEnabled(false);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!KEY_BATTERY.equals(preference.getKey())) {
            return false;
        }
        if (isBatteryStatsAvailable()) {
            BatteryEntry batteryEntry = new BatteryEntry(this.mContext, null, (UserManager) this.mContext.getSystemService("user"), this.mSipper);
            batteryEntry.defaultPackageName = this.mPackageName;
            AdvancedPowerUsageDetail.startBatteryDetailPage((SettingsActivity) this.mParent.getActivity(), this.mParent, this.mBatteryHelper, 0, batteryEntry, this.mBatteryPercent, null);
            return true;
        }
        AdvancedPowerUsageDetail.startBatteryDetailPage((SettingsActivity) this.mParent.getActivity(), this.mParent, this.mPackageName);
        return true;
    }

    @Override
    public void onResume() {
        LoaderManager loaderManager = this.mParent.getLoaderManager();
        AppInfoDashboardFragment appInfoDashboardFragment = this.mParent;
        loaderManager.restartLoader(4, Bundle.EMPTY, this);
    }

    @Override
    public void onPause() {
        LoaderManager loaderManager = this.mParent.getLoaderManager();
        AppInfoDashboardFragment appInfoDashboardFragment = this.mParent;
        loaderManager.destroyLoader(4);
    }

    @Override
    public Loader<BatteryStatsHelper> onCreateLoader(int i, Bundle bundle) {
        return new BatteryStatsHelperLoader(this.mContext);
    }

    @Override
    public void onLoadFinished(Loader<BatteryStatsHelper> loader, BatteryStatsHelper batteryStatsHelper) {
        this.mBatteryHelper = batteryStatsHelper;
        PackageInfo packageInfo = this.mParent.getPackageInfo();
        if (packageInfo != null) {
            this.mSipper = findTargetSipper(batteryStatsHelper, packageInfo.applicationInfo.uid);
            if (this.mParent.getActivity() != null) {
                updateBattery();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<BatteryStatsHelper> loader) {
    }

    void updateBattery() {
        this.mPreference.setEnabled(true);
        if (isBatteryStatsAvailable()) {
            this.mBatteryPercent = Utils.formatPercentage((int) this.mBatteryUtils.calculateBatteryPercent(this.mSipper.totalPowerMah, this.mBatteryHelper.getTotalPower(), this.mBatteryUtils.removeHiddenBatterySippers(new ArrayList(this.mBatteryHelper.getUsageList())), this.mBatteryHelper.getStats().getDischargeAmount(0)));
            this.mPreference.setSummary(this.mContext.getString(R.string.battery_summary, this.mBatteryPercent));
            return;
        }
        this.mPreference.setSummary(this.mContext.getString(R.string.no_battery_summary));
    }

    boolean isBatteryStatsAvailable() {
        return (this.mBatteryHelper == null || this.mSipper == null) ? false : true;
    }

    BatterySipper findTargetSipper(BatteryStatsHelper batteryStatsHelper, int i) {
        List usageList = batteryStatsHelper.getUsageList();
        int size = usageList.size();
        for (int i2 = 0; i2 < size; i2++) {
            BatterySipper batterySipper = (BatterySipper) usageList.get(i2);
            if (batterySipper.getUid() == i) {
                return batterySipper;
            }
        }
        return null;
    }
}
