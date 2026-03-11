package com.android.settings.fuelgauge;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.os.UserManager;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.BatteryBroadcastReceiver;

public abstract class PowerUsageBase extends DashboardFragment {
    static final int MENU_STATS_REFRESH = 2;
    private BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    protected BatteryStatsHelper mStatsHelper;
    protected UserManager mUm;

    protected abstract void refreshUi(int i);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mUm = (UserManager) activity.getSystemService("user");
        this.mStatsHelper = new BatteryStatsHelper(activity, true);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mStatsHelper.create(bundle);
        setHasOptionsMenu(true);
        this.mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(getContext());
        this.mBatteryBroadcastReceiver.setBatteryChangedListener(new BatteryBroadcastReceiver.OnBatteryChangedListener() {
            @Override
            public final void onBatteryChanged(int i) {
                this.f$0.restartBatteryStatsLoader(i);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        BatteryStatsHelper.dropFile(getActivity(), "tmp_bat_history.bin");
        this.mBatteryBroadcastReceiver.register();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mBatteryBroadcastReceiver.unRegister();
    }

    public void restartBatteryStatsLoader(int i) {
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

        @Override
        public Loader<BatteryStatsHelper> onCreateLoader(int i, Bundle bundle) {
            this.mRefreshType = bundle.getInt("refresh_type");
            return new BatteryStatsHelperLoader(PowerUsageBase.this.getContext());
        }

        @Override
        public void onLoadFinished(Loader<BatteryStatsHelper> loader, BatteryStatsHelper batteryStatsHelper) {
            PowerUsageBase.this.mStatsHelper = batteryStatsHelper;
            PowerUsageBase.this.refreshUi(this.mRefreshType);
        }

        @Override
        public void onLoaderReset(Loader<BatteryStatsHelper> loader) {
        }
    }
}
