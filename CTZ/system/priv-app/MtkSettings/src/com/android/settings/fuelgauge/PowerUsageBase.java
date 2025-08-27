package com.android.settings.fuelgauge;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.os.UserManager;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.BatteryBroadcastReceiver;

/* loaded from: classes.dex */
public abstract class PowerUsageBase extends DashboardFragment {
    static final int MENU_STATS_REFRESH = 2;
    private BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    protected BatteryStatsHelper mStatsHelper;
    protected UserManager mUm;

    protected abstract void refreshUi(int i);

    @Override // android.app.Fragment
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mUm = (UserManager) activity.getSystemService("user");
        this.mStatsHelper = new BatteryStatsHelper(activity, true);
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mStatsHelper.create(bundle);
        setHasOptionsMenu(true);
        this.mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(getContext());
        this.mBatteryBroadcastReceiver.setBatteryChangedListener(new BatteryBroadcastReceiver.OnBatteryChangedListener() { // from class: com.android.settings.fuelgauge.-$$Lambda$PowerUsageBase$FbH3lnw7c_hajFOBNpt07exnLiM
            @Override // com.android.settings.fuelgauge.BatteryBroadcastReceiver.OnBatteryChangedListener
            public final void onBatteryChanged(int i) {
                this.f$0.restartBatteryStatsLoader(i);
            }
        });
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onStart() {
        super.onStart();
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        BatteryStatsHelper.dropFile(getActivity(), "tmp_bat_history.bin");
        this.mBatteryBroadcastReceiver.register();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mBatteryBroadcastReceiver.unRegister();
    }

    protected void restartBatteryStatsLoader(int i) {
        Bundle bundle = new Bundle();
        bundle.putInt("refresh_type", i);
        getLoaderManager().restartLoader(0, bundle, new PowerLoaderCallback());
    }

    protected void updatePreference(BatteryHistoryPreference batteryHistoryPreference) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        batteryHistoryPreference.setStats(this.mStatsHelper);
        BatteryUtils.logRuntime("PowerUsageBase", "updatePreference", jCurrentTimeMillis);
    }

    public class PowerLoaderCallback implements LoaderManager.LoaderCallbacks<BatteryStatsHelper> {
        private int mRefreshType;

        public PowerLoaderCallback() {
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public Loader<BatteryStatsHelper> onCreateLoader(int i, Bundle bundle) {
            this.mRefreshType = bundle.getInt("refresh_type");
            return new BatteryStatsHelperLoader(PowerUsageBase.this.getContext());
        }

        /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoadFinished(Loader<BatteryStatsHelper> loader, BatteryStatsHelper batteryStatsHelper) {
            PowerUsageBase.this.mStatsHelper = batteryStatsHelper;
            PowerUsageBase.this.refreshUi(this.mRefreshType);
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoaderReset(Loader<BatteryStatsHelper> loader) {
        }
    }
}
