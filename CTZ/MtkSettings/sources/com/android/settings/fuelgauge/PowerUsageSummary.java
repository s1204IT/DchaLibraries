package com.android.settings.fuelgauge;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.display.BatteryPercentagePreferenceController;
import com.android.settings.fuelgauge.BatteryBroadcastReceiver;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDetectionPolicy;
import com.android.settings.fuelgauge.batterytip.BatteryTipLoader;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.utils.PowerUtil;
import com.android.settingslib.utils.StringUtil;
import com.mediatek.settings.fuelguage.BackgroundPowerSavingPreferenceController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PowerUsageSummary extends PowerUsageBase implements View.OnLongClickListener, BatteryTipPreferenceController.BatteryTipListener {
    static final int BATTERY_INFO_LOADER = 1;
    static final int BATTERY_TIP_LOADER = 2;
    static final int MENU_ADVANCED_BATTERY = 2;
    static final int MENU_STATS_TYPE = 1;
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.power_usage_summary;
            return Collections.singletonList(searchIndexableResource);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add("battery_saver_summary");
            return nonIndexableKeys;
        }
    };
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    SparseArray<List<Anomaly>> mAnomalySparseArray;
    BatteryHeaderPreferenceController mBatteryHeaderPreferenceController;
    BatteryInfo mBatteryInfo;
    LayoutPreference mBatteryLayoutPref;
    BatteryTipPreferenceController mBatteryTipPreferenceController;
    BatteryUtils mBatteryUtils;
    PowerGaugePreference mLastFullChargePref;
    boolean mNeedUpdateBatteryTip;
    PowerUsageFeatureProvider mPowerFeatureProvider;
    PowerGaugePreference mScreenUsagePref;
    private int mStatsType = 0;
    LoaderManager.LoaderCallbacks<BatteryInfo> mBatteryInfoLoaderCallbacks = new LoaderManager.LoaderCallbacks<BatteryInfo>() {
        @Override
        public Loader<BatteryInfo> onCreateLoader(int i, Bundle bundle) {
            return new BatteryInfoLoader(PowerUsageSummary.this.getContext(), PowerUsageSummary.this.mStatsHelper);
        }

        @Override
        public void onLoadFinished(Loader<BatteryInfo> loader, BatteryInfo batteryInfo) {
            PowerUsageSummary.this.mBatteryHeaderPreferenceController.updateHeaderPreference(batteryInfo);
            PowerUsageSummary.this.mBatteryInfo = batteryInfo;
            PowerUsageSummary.this.updateLastFullChargePreference();
        }

        @Override
        public void onLoaderReset(Loader<BatteryInfo> loader) {
        }
    };
    LoaderManager.LoaderCallbacks<List<BatteryInfo>> mBatteryInfoDebugLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<BatteryInfo>>() {
        @Override
        public Loader<List<BatteryInfo>> onCreateLoader(int i, Bundle bundle) {
            return new DebugEstimatesLoader(PowerUsageSummary.this.getContext(), PowerUsageSummary.this.mStatsHelper);
        }

        @Override
        public void onLoadFinished(Loader<List<BatteryInfo>> loader, List<BatteryInfo> list) {
            PowerUsageSummary.this.updateViews(list);
        }

        @Override
        public void onLoaderReset(Loader<List<BatteryInfo>> loader) {
        }
    };
    private LoaderManager.LoaderCallbacks<List<BatteryTip>> mBatteryTipsCallbacks = new LoaderManager.LoaderCallbacks<List<BatteryTip>>() {
        @Override
        public Loader<List<BatteryTip>> onCreateLoader(int i, Bundle bundle) {
            return new BatteryTipLoader(PowerUsageSummary.this.getContext(), PowerUsageSummary.this.mStatsHelper);
        }

        @Override
        public void onLoadFinished(Loader<List<BatteryTip>> loader, List<BatteryTip> list) {
            PowerUsageSummary.this.mBatteryTipPreferenceController.updateBatteryTips(list);
        }

        @Override
        public void onLoaderReset(Loader<List<BatteryTip>> loader) {
        }
    };

    protected void updateViews(List<BatteryInfo> list) {
        BatteryMeterView batteryMeterView = (BatteryMeterView) this.mBatteryLayoutPref.findViewById(R.id.battery_header_icon);
        TextView textView = (TextView) this.mBatteryLayoutPref.findViewById(R.id.battery_percent);
        TextView textView2 = (TextView) this.mBatteryLayoutPref.findViewById(R.id.summary1);
        TextView textView3 = (TextView) this.mBatteryLayoutPref.findViewById(R.id.summary2);
        BatteryInfo batteryInfo = list.get(0);
        BatteryInfo batteryInfo2 = list.get(1);
        textView.setText(Utils.formatPercentage(batteryInfo.batteryLevel));
        textView2.setText(this.mPowerFeatureProvider.getOldEstimateDebugString(Formatter.formatShortElapsedTime(getContext(), PowerUtil.convertUsToMs(batteryInfo.remainingTimeUs))));
        textView3.setText(this.mPowerFeatureProvider.getEnhancedEstimateDebugString(Formatter.formatShortElapsedTime(getContext(), PowerUtil.convertUsToMs(batteryInfo2.remainingTimeUs))));
        batteryMeterView.setBatteryLevel(batteryInfo.batteryLevel);
        batteryMeterView.setCharging(!batteryInfo.discharging);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setAnimationAllowed(true);
        initFeatureProvider();
        this.mBatteryLayoutPref = (LayoutPreference) findPreference("battery_header");
        this.mScreenUsagePref = (PowerGaugePreference) findPreference("screen_usage");
        this.mLastFullChargePref = (PowerGaugePreference) findPreference("last_full_charge");
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.battery_footer_summary);
        this.mBatteryUtils = BatteryUtils.getInstance(getContext());
        this.mAnomalySparseArray = new SparseArray<>();
        restartBatteryInfoLoader();
        this.mBatteryTipPreferenceController.restoreInstanceState(bundle);
        updateBatteryTipFlag(bundle);
    }

    @Override
    public int getMetricsCategory() {
        return 1263;
    }

    @Override
    protected String getLogTag() {
        return "PowerUsageSummary";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_summary;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        Lifecycle lifecycle = getLifecycle();
        SettingsActivity settingsActivity = (SettingsActivity) getActivity();
        ArrayList arrayList = new ArrayList();
        this.mBatteryHeaderPreferenceController = new BatteryHeaderPreferenceController(context, settingsActivity, this, lifecycle);
        arrayList.add(this.mBatteryHeaderPreferenceController);
        this.mBatteryTipPreferenceController = new BatteryTipPreferenceController(context, "battery_tip", (SettingsActivity) getActivity(), this, this);
        arrayList.add(this.mBatteryTipPreferenceController);
        arrayList.add(new BatteryPercentagePreferenceController(context));
        arrayList.add(new BackgroundPowerSavingPreferenceController(context));
        return arrayList;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.add(0, 2, 0, R.string.advanced_battery_title);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_battery;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 1:
                if (this.mStatsType == 0) {
                    this.mStatsType = 2;
                } else {
                    this.mStatsType = 0;
                }
                refreshUi(0);
                return true;
            case 2:
                new SubSettingLauncher(getContext()).setDestination(PowerUsageAdvanced.class.getName()).setSourceMetricsCategory(getMetricsCategory()).setTitle(R.string.advanced_battery_title).launch();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected void refreshUi(int i) {
        if (getContext() == null) {
            return;
        }
        if (this.mNeedUpdateBatteryTip && i != 1) {
            restartBatteryTipLoader();
        } else {
            this.mNeedUpdateBatteryTip = true;
        }
        restartBatteryInfoLoader();
        updateLastFullChargePreference();
        this.mScreenUsagePref.setSubtitle(StringUtil.formatElapsedTime(getContext(), this.mBatteryUtils.calculateScreenUsageTime(this.mStatsHelper), false));
    }

    void restartBatteryTipLoader() {
        getLoaderManager().restartLoader(2, Bundle.EMPTY, this.mBatteryTipsCallbacks);
    }

    void setBatteryLayoutPreference(LayoutPreference layoutPreference) {
        this.mBatteryLayoutPref = layoutPreference;
    }

    AnomalyDetectionPolicy getAnomalyDetectionPolicy() {
        return new AnomalyDetectionPolicy(getContext());
    }

    void updateLastFullChargePreference() {
        if (this.mBatteryInfo != null && this.mBatteryInfo.averageTimeToDischarge != -1) {
            this.mLastFullChargePref.setTitle(R.string.battery_full_charge_last);
            this.mLastFullChargePref.setSubtitle(StringUtil.formatElapsedTime(getContext(), this.mBatteryInfo.averageTimeToDischarge, false));
        } else {
            long jCalculateLastFullChargeTime = this.mBatteryUtils.calculateLastFullChargeTime(this.mStatsHelper, System.currentTimeMillis());
            this.mLastFullChargePref.setTitle(R.string.battery_last_full_charge);
            this.mLastFullChargePref.setSubtitle(StringUtil.formatRelativeTime(getContext(), jCalculateLastFullChargeTime, false));
        }
    }

    void showBothEstimates() {
        Context context = getContext();
        if (context == null || !this.mPowerFeatureProvider.isEnhancedBatteryPredictionEnabled(context)) {
            return;
        }
        getLoaderManager().restartLoader(3, Bundle.EMPTY, this.mBatteryInfoDebugLoaderCallbacks);
    }

    void initFeatureProvider() {
        Context context = getContext();
        this.mPowerFeatureProvider = FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);
    }

    void updateAnomalySparseArray(List<Anomaly> list) {
        this.mAnomalySparseArray.clear();
        for (Anomaly anomaly : list) {
            if (this.mAnomalySparseArray.get(anomaly.uid) == null) {
                this.mAnomalySparseArray.append(anomaly.uid, new ArrayList());
            }
            this.mAnomalySparseArray.get(anomaly.uid).add(anomaly);
        }
    }

    void restartBatteryInfoLoader() {
        getLoaderManager().restartLoader(1, Bundle.EMPTY, this.mBatteryInfoLoaderCallbacks);
        if (this.mPowerFeatureProvider.isEstimateDebugEnabled()) {
            this.mBatteryLayoutPref.findViewById(R.id.summary1).setOnLongClickListener(this);
        }
    }

    void updateBatteryTipFlag(Bundle bundle) {
        this.mNeedUpdateBatteryTip = bundle == null || this.mBatteryTipPreferenceController.needUpdate();
    }

    @Override
    public boolean onLongClick(View view) {
        showBothEstimates();
        view.setOnLongClickListener(null);
        return true;
    }

    @Override
    public void restartBatteryStatsLoader(int i) {
        super.restartBatteryStatsLoader(i);
        this.mBatteryHeaderPreferenceController.quickUpdateHeaderPreference();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mBatteryTipPreferenceController.saveInstanceState(bundle);
    }

    @Override
    public void onBatteryTipHandled(BatteryTip batteryTip) {
        restartBatteryTipLoader();
    }

    static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final BatteryBroadcastReceiver mBatteryBroadcastReceiver;
        private final Context mContext;
        private final SummaryLoader mLoader;

        private SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mLoader = summaryLoader;
            this.mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(this.mContext);
            this.mBatteryBroadcastReceiver.setBatteryChangedListener(new BatteryBroadcastReceiver.OnBatteryChangedListener() {
                @Override
                public final void onBatteryChanged(int i) {
                    PowerUsageSummary.SummaryProvider summaryProvider = this.f$0;
                    BatteryInfo.getBatteryInfo(summaryProvider.mContext, new BatteryInfo.Callback() {
                        @Override
                        public void onBatteryInfoLoaded(BatteryInfo batteryInfo) {
                            SummaryProvider.this.mLoader.setSummary(SummaryProvider.this, PowerUsageSummary.getDashboardLabel(SummaryProvider.this.mContext, batteryInfo));
                        }
                    }, true);
                }
            });
        }

        @Override
        public void setListening(boolean z) {
            if (z) {
                this.mBatteryBroadcastReceiver.register();
            } else {
                this.mBatteryBroadcastReceiver.unRegister();
            }
        }
    }

    static CharSequence getDashboardLabel(Context context, BatteryInfo batteryInfo) {
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        if (batteryInfo.remainingLabel == null) {
            return batteryInfo.batteryPercentString;
        }
        return context.getString(R.string.power_remaining_settings_home_page, bidiFormatter.unicodeWrap(batteryInfo.batteryPercentString), bidiFormatter.unicodeWrap(batteryInfo.remainingLabel));
    }
}
