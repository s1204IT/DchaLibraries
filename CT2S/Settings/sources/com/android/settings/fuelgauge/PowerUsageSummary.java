package com.android.settings.fuelgauge;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.os.PowerProfile;
import com.android.settings.HelpUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import java.util.List;

public class PowerUsageSummary extends PreferenceFragment {
    private PreferenceGroup mAppListGroup;
    private String mBatteryLevel;
    private String mBatteryStatus;
    private BatteryHistoryPreference mHistPref;
    private BatteryStatsHelper mStatsHelper;
    private UserManager mUm;
    private int mStatsType = 0;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.BATTERY_CHANGED".equals(action) && PowerUsageSummary.this.updateBatteryStatus(intent) && !PowerUsageSummary.this.mHandler.hasMessages(100)) {
                PowerUsageSummary.this.mHandler.sendEmptyMessageDelayed(100, 500L);
            }
        }
    };
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    BatteryEntry entry = (BatteryEntry) msg.obj;
                    PowerGaugePreference pgp = (PowerGaugePreference) PowerUsageSummary.this.findPreference(Integer.toString(entry.sipper.uidObj.getUid()));
                    if (pgp != null) {
                        int userId = UserHandle.getUserId(entry.sipper.getUid());
                        UserHandle userHandle = new UserHandle(userId);
                        pgp.setIcon(PowerUsageSummary.this.mUm.getBadgedIconForUser(entry.getIcon(), userHandle));
                        pgp.setTitle(entry.name);
                    }
                    break;
                case 2:
                    Activity activity = PowerUsageSummary.this.getActivity();
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
                case 100:
                    PowerUsageSummary.this.mStatsHelper.clearStats();
                    PowerUsageSummary.this.refreshStats();
                    break;
            }
            super.handleMessage(msg);
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
        addPreferencesFromResource(R.xml.power_usage_summary);
        this.mAppListGroup = (PreferenceGroup) findPreference("app_list");
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
        if (this.mHandler.hasMessages(100)) {
            this.mHandler.removeMessages(100);
            this.mStatsHelper.clearStats();
        }
        refreshStats();
    }

    @Override
    public void onPause() {
        BatteryEntry.stopRequestQueue();
        this.mHandler.removeMessages(1);
        getActivity().unregisterReceiver(this.mBatteryInfoReceiver);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mHandler.removeMessages(100);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            this.mStatsHelper.storeState();
            BatteryEntry.clearUidCache();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof BatteryHistoryPreference) {
            this.mStatsHelper.storeStatsHistoryInFile("tmp_bat_history.bin");
            Bundle args = new Bundle();
            args.putString("stats", "tmp_bat_history.bin");
            args.putParcelable("broadcast", this.mStatsHelper.getBatteryBroadcast());
            SettingsActivity sa = (SettingsActivity) getActivity();
            sa.startPreferencePanel(BatteryHistoryDetail.class.getName(), args, R.string.history_details_title, null, null, 0);
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        if (!(preference instanceof PowerGaugePreference)) {
            return false;
        }
        PowerGaugePreference pgp = (PowerGaugePreference) preference;
        BatteryEntry entry = pgp.getInfo();
        PowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(), this.mStatsHelper, this.mStatsType, entry, true);
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem refresh = menu.add(0, 2, 0, R.string.menu_stats_refresh).setIcon(android.R.drawable.ic_collapse_bundle).setAlphabeticShortcut('r');
        refresh.setShowAsAction(5);
        MenuItem batterySaver = menu.add(0, 3, 0, R.string.battery_saver);
        batterySaver.setShowAsAction(0);
        String helpUrl = getResources().getString(R.string.help_url_battery);
        if (!TextUtils.isEmpty(helpUrl)) {
            MenuItem help = menu.add(0, 4, 0, R.string.help_label);
            HelpUtils.prepareHelpMenuItem(getActivity(), help, helpUrl);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                if (this.mStatsType == 0) {
                    this.mStatsType = 2;
                } else {
                    this.mStatsType = 0;
                }
                refreshStats();
                return true;
            case 2:
                this.mStatsHelper.clearStats();
                refreshStats();
                this.mHandler.removeMessages(100);
                return true;
            case 3:
                SettingsActivity sa = (SettingsActivity) getActivity();
                sa.startPreferencePanel(BatterySaverSettings.class.getName(), null, R.string.battery_saver, null, null, 0);
                return true;
            default:
                return false;
        }
    }

    private void addNotAvailableMessage() {
        Preference notAvailable = new Preference(getActivity());
        notAvailable.setTitle(R.string.power_usage_not_available);
        this.mHistPref.setHideLabels(true);
        this.mAppListGroup.addPreference(notAvailable);
    }

    private boolean updateBatteryStatus(Intent intent) {
        if (intent != null) {
            String batteryLevel = Utils.getBatteryPercentage(intent);
            String batteryStatus = Utils.getBatteryStatus(getResources(), intent);
            if (!batteryLevel.equals(this.mBatteryLevel) || !batteryStatus.equals(this.mBatteryStatus)) {
                this.mBatteryLevel = batteryLevel;
                this.mBatteryStatus = batteryStatus;
                return true;
            }
        }
        return false;
    }

    private void refreshStats() {
        this.mAppListGroup.removeAll();
        this.mAppListGroup.setOrderingAsAdded(false);
        this.mHistPref = new BatteryHistoryPreference(getActivity(), this.mStatsHelper.getStats(), this.mStatsHelper.getBatteryBroadcast());
        this.mHistPref.setOrder(-1);
        this.mAppListGroup.addPreference(this.mHistPref);
        boolean addedSome = false;
        PowerProfile powerProfile = this.mStatsHelper.getPowerProfile();
        BatteryStats stats = this.mStatsHelper.getStats();
        double averagePower = powerProfile.getAveragePower("screen.full");
        if (averagePower >= 10.0d) {
            List<UserHandle> profiles = this.mUm.getUserProfiles();
            this.mStatsHelper.refreshStats(0, profiles);
            List<BatterySipper> usageList = this.mStatsHelper.getUsageList();
            int dischargeAmount = stats != null ? stats.getDischargeAmount(this.mStatsType) : 0;
            int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                BatterySipper sipper = usageList.get(i);
                if (sipper.value * 3600.0d >= 5.0d) {
                    double percentOfTotal = (sipper.value / this.mStatsHelper.getTotalPower()) * ((double) dischargeAmount);
                    if (((int) (0.5d + percentOfTotal)) >= 1 && ((sipper.drainType != BatterySipper.DrainType.OVERCOUNTED || (sipper.value >= (this.mStatsHelper.getMaxRealPower() * 2.0d) / 3.0d && percentOfTotal >= 10.0d && !"user".equals(Build.TYPE))) && (sipper.drainType != BatterySipper.DrainType.UNACCOUNTED || (sipper.value >= this.mStatsHelper.getMaxRealPower() / 2.0d && percentOfTotal >= 5.0d && !"user".equals(Build.TYPE))))) {
                        UserHandle userHandle = new UserHandle(UserHandle.getUserId(sipper.getUid()));
                        BatteryEntry entry = new BatteryEntry(getActivity(), this.mHandler, this.mUm, sipper);
                        Drawable badgedIcon = this.mUm.getBadgedIconForUser(entry.getIcon(), userHandle);
                        CharSequence contentDescription = this.mUm.getBadgedLabelForUser(entry.getLabel(), userHandle);
                        PowerGaugePreference pref = new PowerGaugePreference(getActivity(), badgedIcon, contentDescription, entry);
                        double percentOfMax = (sipper.value * 100.0d) / this.mStatsHelper.getMaxPower();
                        sipper.percent = percentOfTotal;
                        pref.setTitle(entry.getLabel());
                        pref.setOrder(i + 1);
                        pref.setPercent(percentOfMax, percentOfTotal);
                        if (sipper.uidObj != null) {
                            pref.setKey(Integer.toString(sipper.uidObj.getUid()));
                        }
                        addedSome = true;
                        this.mAppListGroup.addPreference(pref);
                        if (this.mAppListGroup.getPreferenceCount() > 11) {
                            break;
                        }
                    }
                }
            }
        }
        if (!addedSome) {
            addNotAvailableMessage();
        }
        BatteryEntry.startRequestQueue();
    }
}
