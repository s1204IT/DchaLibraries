package com.android.settings.fuelgauge;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public abstract class PowerUsageBase extends SettingsPreferenceFragment {
    private String mBatteryLevel;
    private String mBatteryStatus;
    protected BatteryStatsHelper mStatsHelper;
    protected UserManager mUm;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    PowerUsageBase.this.mStatsHelper.clearStats();
                    PowerUsageBase.this.refreshStats();
                    break;
            }
        }
    };
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"android.intent.action.BATTERY_CHANGED".equals(action) || !PowerUsageBase.this.updateBatteryStatus(intent) || PowerUsageBase.this.mHandler.hasMessages(100)) {
                return;
            }
            PowerUsageBase.this.mHandler.sendEmptyMessageDelayed(100, 500L);
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mUm = (UserManager) activity.getSystemService("user");
        this.mStatsHelper = new BatteryStatsHelper(activity, true);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mStatsHelper.create(icicle);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mStatsHelper.clearStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        BatteryStatsHelper.dropFile(getActivity(), "tmp_bat_history.bin");
        updateBatteryStatus(getActivity().registerReceiver(this.mBatteryInfoReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED")));
        if (!this.mHandler.hasMessages(100)) {
            return;
        }
        this.mHandler.removeMessages(100);
        this.mStatsHelper.clearStats();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mBatteryInfoReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mHandler.removeMessages(100);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!getActivity().isChangingConfigurations()) {
            return;
        }
        this.mStatsHelper.storeState();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem refresh = menu.add(0, 2, 0, R.string.menu_stats_refresh).setIcon(android.R.drawable.ic_chevron_start).setAlphabeticShortcut('r');
        refresh.setShowAsAction(5);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DefaultWfcSettingsExt.CREATE:
                this.mStatsHelper.clearStats();
                refreshStats();
                this.mHandler.removeMessages(100);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void refreshStats() {
        this.mStatsHelper.refreshStats(0, this.mUm.getUserProfiles());
    }

    protected void updatePreference(BatteryHistoryPreference historyPref) {
        historyPref.setStats(this.mStatsHelper);
    }

    public boolean updateBatteryStatus(Intent intent) {
        if (intent != null) {
            String batteryLevel = Utils.getBatteryPercentage(intent);
            String batteryStatus = Utils.getBatteryStatus(getResources(), intent);
            if (!batteryLevel.equals(this.mBatteryLevel) || !batteryStatus.equals(this.mBatteryStatus)) {
                this.mBatteryLevel = batteryLevel;
                this.mBatteryStatus = batteryStatus;
                return true;
            }
            return false;
        }
        return false;
    }
}
