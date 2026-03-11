package com.android.settings.fuelgauge;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MenuItem;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.PowerProfile;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.ManageApplications;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settingslib.BatteryInfo;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.fuelgauge.PowerUsageExts;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PowerUsageSummary extends PowerUsageBase implements Preference.OnPreferenceChangeListener {
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader, null);
        }
    };
    private PreferenceGroup mAppListGroup;
    private BatteryHistoryPreference mHistPref;
    PowerUsageExts mPowerUsageExts;
    private int mStatsType = 0;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.PAUSE:
                    BatteryEntry entry = (BatteryEntry) msg.obj;
                    PowerGaugePreference pgp = (PowerGaugePreference) PowerUsageSummary.this.findPreference(Integer.toString(entry.sipper.uidObj.getUid()));
                    if (pgp != null) {
                        int userId = UserHandle.getUserId(entry.sipper.getUid());
                        UserHandle userHandle = new UserHandle(userId);
                        pgp.setIcon(PowerUsageSummary.this.mUm.getBadgedIconForUser(entry.getIcon(), userHandle));
                        pgp.setTitle(entry.name);
                        if (entry.sipper.drainType == BatterySipper.DrainType.APP) {
                            pgp.setContentDescription(entry.name);
                        }
                    }
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    Activity activity = PowerUsageSummary.this.getActivity();
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);
        addPreferencesFromResource(R.xml.power_usage_summary);
        this.mHistPref = (BatteryHistoryPreference) findPreference("battery_history");
        this.mAppListGroup = (PreferenceGroup) findPreference("app_list");
        if (!FeatureOption.MTK_POWER_PERFORMANCE_STRATEGY_SUPPORT) {
            return;
        }
        getPreferenceScreen().findPreference("performance_and_power").setOnPreferenceChangeListener(this);
    }

    @Override
    protected int getMetricsCategory() {
        return 54;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }

    @Override
    public void onPause() {
        BatteryEntry.stopRequestQueue();
        this.mHandler.removeMessages(1);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!getActivity().isChangingConfigurations()) {
            return;
        }
        BatteryEntry.clearUidCache();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (!(preference instanceof PowerGaugePreference)) {
            if (this.mPowerUsageExts.onPowerUsageExtItemsClick(preference)) {
                return super.onPreferenceTreeClick(preference);
            }
            return super.onPreferenceTreeClick(preference);
        }
        PowerGaugePreference pgp = (PowerGaugePreference) preference;
        BatteryEntry entry = pgp.getInfo();
        PowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(), this.mStatsHelper, this.mStatsType, entry, true, true);
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("PowerUsageSummary", "onPreferenceChange");
        return this.mPowerUsageExts.onPreferenceChange(preference, newValue);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_battery;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SettingsActivity sa = (SettingsActivity) getActivity();
        switch (item.getItemId()) {
            case DefaultWfcSettingsExt.PAUSE:
                if (this.mStatsType == 0) {
                    this.mStatsType = 2;
                } else {
                    this.mStatsType = 0;
                }
                refreshStats();
                return true;
            case DefaultWfcSettingsExt.CREATE:
            case DefaultWfcSettingsExt.DESTROY:
            default:
                return super.onOptionsItemSelected(item);
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                Bundle args = new Bundle();
                args.putString("classname", Settings.HighPowerApplicationsActivity.class.getName());
                sa.startPreferencePanel(ManageApplications.class.getName(), args, R.string.high_power_apps, null, null, 0);
                return true;
        }
    }

    private void addNotAvailableMessage() {
        if (getCachedPreference("not_available") != null) {
            return;
        }
        Preference notAvailable = new Preference(getPrefContext());
        notAvailable.setKey("not_available");
        notAvailable.setTitle(R.string.power_usage_not_available);
        this.mAppListGroup.addPreference(notAvailable);
    }

    private static boolean isSharedGid(int uid) {
        return UserHandle.getAppIdFromSharedAppGid(uid) > 0;
    }

    private static boolean isSystemUid(int uid) {
        return uid >= 1000 && uid < 10000;
    }

    private static List<BatterySipper> getCoalescedUsageList(List<BatterySipper> sippers) {
        SparseArray<BatterySipper> uidList = new SparseArray<>();
        ArrayList<BatterySipper> results = new ArrayList<>();
        int numSippers = sippers.size();
        for (int i = 0; i < numSippers; i++) {
            BatterySipper sipper = sippers.get(i);
            if (sipper.getUid() > 0) {
                int realUid = sipper.getUid();
                if (isSharedGid(sipper.getUid())) {
                    realUid = UserHandle.getUid(0, UserHandle.getAppIdFromSharedAppGid(sipper.getUid()));
                }
                if (isSystemUid(realUid) && !"mediaserver".equals(sipper.packageWithHighestDrain)) {
                    realUid = 1000;
                }
                if (realUid != sipper.getUid()) {
                    BatterySipper newSipper = new BatterySipper(sipper.drainType, new FakeUid(realUid), 0.0d);
                    newSipper.add(sipper);
                    newSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    newSipper.mPackages = sipper.mPackages;
                    sipper = newSipper;
                }
                int index = uidList.indexOfKey(realUid);
                if (index < 0) {
                    uidList.put(realUid, sipper);
                } else {
                    BatterySipper existingSipper = uidList.valueAt(index);
                    existingSipper.add(sipper);
                    if (existingSipper.packageWithHighestDrain == null && sipper.packageWithHighestDrain != null) {
                        existingSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    }
                    int existingPackageLen = existingSipper.mPackages != null ? existingSipper.mPackages.length : 0;
                    int newPackageLen = sipper.mPackages != null ? sipper.mPackages.length : 0;
                    if (newPackageLen > 0) {
                        String[] newPackages = new String[existingPackageLen + newPackageLen];
                        if (existingPackageLen > 0) {
                            System.arraycopy(existingSipper.mPackages, 0, newPackages, 0, existingPackageLen);
                        }
                        System.arraycopy(sipper.mPackages, 0, newPackages, existingPackageLen, newPackageLen);
                        existingSipper.mPackages = newPackages;
                    }
                }
            } else {
                results.add(sipper);
            }
        }
        int numUidSippers = uidList.size();
        for (int i2 = 0; i2 < numUidSippers; i2++) {
            results.add(uidList.valueAt(i2));
        }
        Collections.sort(results, new Comparator<BatterySipper>() {
            @Override
            public int compare(BatterySipper a, BatterySipper b) {
                return Double.compare(b.totalPowerMah, a.totalPowerMah);
            }
        });
        return results;
    }

    @Override
    protected void refreshStats() {
        String key;
        super.refreshStats();
        updatePreference(this.mHistPref);
        cacheRemoveAllPrefs(this.mAppListGroup);
        this.mAppListGroup.setOrderingAsAdded(false);
        boolean addedSome = false;
        PowerProfile powerProfile = this.mStatsHelper.getPowerProfile();
        BatteryStats stats = this.mStatsHelper.getStats();
        double averagePower = powerProfile.getAveragePower("screen.full");
        TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorControlNormal, value, true);
        int colorControl = getContext().getColor(value.resourceId);
        if (averagePower >= 10.0d) {
            List<BatterySipper> usageList = getCoalescedUsageList(this.mStatsHelper.getUsageList());
            int dischargeAmount = stats != null ? stats.getDischargeAmount(this.mStatsType) : 0;
            int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                BatterySipper sipper = usageList.get(i);
                if (sipper.totalPowerMah * 3600.0d >= 5.0d) {
                    double totalPower = this.mStatsHelper.getTotalPower();
                    double percentOfTotal = (sipper.totalPowerMah / totalPower) * ((double) dischargeAmount);
                    if (((int) (0.5d + percentOfTotal)) >= 1 && ((sipper.drainType != BatterySipper.DrainType.OVERCOUNTED || (sipper.totalPowerMah >= (this.mStatsHelper.getMaxRealPower() * 2.0d) / 3.0d && percentOfTotal >= 10.0d && !"user".equals(Build.TYPE))) && (sipper.drainType != BatterySipper.DrainType.UNACCOUNTED || (sipper.totalPowerMah >= this.mStatsHelper.getMaxRealPower() / 2.0d && percentOfTotal >= 5.0d && !"user".equals(Build.TYPE))))) {
                        UserHandle userHandle = new UserHandle(UserHandle.getUserId(sipper.getUid()));
                        BatteryEntry entry = new BatteryEntry(getActivity(), this.mHandler, this.mUm, sipper);
                        Drawable badgedIcon = this.mUm.getBadgedIconForUser(entry.getIcon(), userHandle);
                        CharSequence contentDescription = this.mUm.getBadgedLabelForUser(entry.getLabel(), userHandle);
                        if (sipper.drainType != BatterySipper.DrainType.APP) {
                            key = sipper.drainType.toString();
                        } else if (sipper.getPackages() != null) {
                            key = TextUtils.concat(sipper.getPackages()).toString();
                        } else {
                            key = String.valueOf(sipper.getUid());
                        }
                        PowerGaugePreference pref = (PowerGaugePreference) getCachedPreference(key);
                        if (pref == null) {
                            pref = new PowerGaugePreference(getPrefContext(), badgedIcon, contentDescription, entry);
                            pref.setKey(key);
                        }
                        double percentOfMax = (sipper.totalPowerMah * 100.0d) / this.mStatsHelper.getMaxPower();
                        sipper.percent = percentOfTotal;
                        pref.setTitle(entry.getLabel());
                        pref.setOrder(i + 1);
                        pref.setPercent(percentOfMax, percentOfTotal);
                        if (sipper.uidObj != null) {
                            pref.setKey(Integer.toString(sipper.uidObj.getUid()));
                        }
                        if ((sipper.drainType != BatterySipper.DrainType.APP || sipper.uidObj.getUid() == 0) && sipper.drainType != BatterySipper.DrainType.USER) {
                            pref.setTint(colorControl);
                        }
                        addedSome = true;
                        this.mAppListGroup.addPreference(pref);
                        if (this.mAppListGroup.getPreferenceCount() - getCachedCount() > 11) {
                            break;
                        }
                    }
                }
            }
        }
        if (!addedSome) {
            addNotAvailableMessage();
        }
        removeCachedPrefs(this.mAppListGroup);
        BatteryEntry.startRequestQueue();
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;

        SummaryProvider(Context context, SummaryLoader loader, SummaryProvider summaryProvider) {
            this(context, loader);
        }

        private SummaryProvider(Context context, SummaryLoader loader) {
            this.mContext = context;
            this.mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            BatteryInfo.getBatteryInfo(this.mContext, new BatteryInfo.Callback() {
                @Override
                public void onBatteryInfoLoaded(BatteryInfo info) {
                    SummaryProvider.this.mLoader.setSummary(SummaryProvider.this, info.mChargeLabelString);
                }
            });
        }
    }
}
