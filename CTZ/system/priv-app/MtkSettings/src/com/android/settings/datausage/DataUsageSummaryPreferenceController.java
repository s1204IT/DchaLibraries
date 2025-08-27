package com.android.settings.datausage;

import android.app.Activity;
import android.content.Intent;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.support.v7.preference.Preference;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;
import android.text.TextUtils;
import android.util.Log;
import android.util.RecurrenceRule;
import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.net.DataUsageController;
import java.util.List;

/* loaded from: classes.dex */
public class DataUsageSummaryPreferenceController extends BasePreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnStart {
    private static final String KEY = "status_header";
    private static final long PETA = 1000000000000000L;
    private static final float RELATIVE_SIZE_LARGE = 1.5625f;
    private static final float RELATIVE_SIZE_SMALL = 0.64f;
    private static final String TAG = "DataUsageController";
    private final Activity mActivity;
    private CharSequence mCarrierName;
    private long mCycleEnd;
    private long mCycleStart;
    private long mDataBarSize;
    private final DataUsageInfoController mDataInfoController;
    private final DataUsageController mDataUsageController;
    private final DataUsageSummary mDataUsageSummary;
    private final int mDataUsageTemplate;
    private int mDataplanCount;
    private long mDataplanSize;
    private long mDataplanUse;
    private final NetworkTemplate mDefaultTemplate;
    private final EntityHeaderController mEntityHeaderController;
    private final boolean mHasMobileData;
    private final Lifecycle mLifecycle;
    private Intent mManageSubscriptionIntent;
    private final NetworkPolicyEditor mPolicyEditor;
    private long mSnapshotTime;
    private final SubscriptionManager mSubscriptionManager;

    public DataUsageSummaryPreferenceController(Activity activity, Lifecycle lifecycle, DataUsageSummary dataUsageSummary) {
        super(activity, KEY);
        this.mActivity = activity;
        this.mEntityHeaderController = EntityHeaderController.newInstance(activity, dataUsageSummary, null);
        this.mLifecycle = lifecycle;
        this.mDataUsageSummary = dataUsageSummary;
        int defaultSubscriptionId = DataUsageUtils.getDefaultSubscriptionId(activity);
        this.mDefaultTemplate = DataUsageUtils.getDefaultTemplate(activity, defaultSubscriptionId);
        this.mPolicyEditor = new NetworkPolicyEditor(NetworkPolicyManager.from(activity));
        this.mHasMobileData = DataUsageUtils.hasMobileData(activity) && defaultSubscriptionId != -1;
        this.mDataUsageController = new DataUsageController(activity);
        this.mDataInfoController = new DataUsageInfoController();
        if (this.mHasMobileData) {
            this.mDataUsageTemplate = R.string.cell_data_template;
        } else if (DataUsageUtils.hasWifiRadio(activity)) {
            this.mDataUsageTemplate = R.string.wifi_data_template;
        } else {
            this.mDataUsageTemplate = R.string.ethernet_data_template;
        }
        this.mSubscriptionManager = (SubscriptionManager) this.mContext.getSystemService("telephony_subscription_service");
    }

    DataUsageSummaryPreferenceController(DataUsageController dataUsageController, DataUsageInfoController dataUsageInfoController, NetworkTemplate networkTemplate, NetworkPolicyEditor networkPolicyEditor, int i, boolean z, SubscriptionManager subscriptionManager, Activity activity, Lifecycle lifecycle, EntityHeaderController entityHeaderController, DataUsageSummary dataUsageSummary) {
        super(activity, KEY);
        this.mDataUsageController = dataUsageController;
        this.mDataInfoController = dataUsageInfoController;
        this.mDefaultTemplate = networkTemplate;
        this.mPolicyEditor = networkPolicyEditor;
        this.mDataUsageTemplate = i;
        this.mHasMobileData = z;
        this.mSubscriptionManager = subscriptionManager;
        this.mActivity = activity;
        this.mLifecycle = lifecycle;
        this.mEntityHeaderController = entityHeaderController;
        this.mDataUsageSummary = dataUsageSummary;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        this.mEntityHeaderController.setRecyclerView(this.mDataUsageSummary.getListView(), this.mLifecycle);
        this.mEntityHeaderController.styleActionBar(this.mActivity);
    }

    void setPlanValues(int i, long j, long j2) {
        this.mDataplanCount = i;
        this.mDataplanSize = j;
        this.mDataBarSize = j;
        this.mDataplanUse = j2;
    }

    void setCarrierValues(String str, long j, long j2, Intent intent) {
        this.mCarrierName = str;
        this.mSnapshotTime = j;
        this.mCycleEnd = j2;
        this.mManageSubscriptionIntent = intent;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return (DataUsageUtils.hasSim(this.mActivity) || DataUsageUtils.hasWifiRadio(this.mContext)) ? 0 : 1;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        DataUsageSummaryPreference dataUsageSummaryPreference = (DataUsageSummaryPreference) preference;
        if (DataUsageUtils.hasSim(this.mActivity)) {
            DataUsageController.DataUsageInfo dataUsageInfo = this.mDataUsageController.getDataUsageInfo(this.mDefaultTemplate);
            this.mDataInfoController.updateDataLimit(dataUsageInfo, this.mPolicyEditor.getPolicy(this.mDefaultTemplate));
            dataUsageSummaryPreference.setWifiMode(false, null);
            if (this.mSubscriptionManager != null) {
                refreshDataplanInfo(dataUsageInfo);
            }
            if (dataUsageInfo.warningLevel > 0 && dataUsageInfo.limitLevel > 0) {
                dataUsageSummaryPreference.setLimitInfo(TextUtils.expandTemplate(this.mContext.getText(R.string.cell_data_warning_and_limit), DataUsageUtils.formatDataUsage(this.mContext, dataUsageInfo.warningLevel), DataUsageUtils.formatDataUsage(this.mContext, dataUsageInfo.limitLevel)).toString());
            } else if (dataUsageInfo.warningLevel > 0) {
                dataUsageSummaryPreference.setLimitInfo(TextUtils.expandTemplate(this.mContext.getText(R.string.cell_data_warning), DataUsageUtils.formatDataUsage(this.mContext, dataUsageInfo.warningLevel)).toString());
            } else if (dataUsageInfo.limitLevel > 0) {
                dataUsageSummaryPreference.setLimitInfo(TextUtils.expandTemplate(this.mContext.getText(R.string.cell_data_limit), DataUsageUtils.formatDataUsage(this.mContext, dataUsageInfo.limitLevel)).toString());
            } else {
                dataUsageSummaryPreference.setLimitInfo(null);
            }
            dataUsageSummaryPreference.setUsageNumbers(this.mDataplanUse, this.mDataplanSize, this.mHasMobileData);
            if (this.mDataBarSize <= 0) {
                dataUsageSummaryPreference.setChartEnabled(false);
            } else {
                dataUsageSummaryPreference.setChartEnabled(true);
                dataUsageSummaryPreference.setLabels(DataUsageUtils.formatDataUsage(this.mContext, 0L), DataUsageUtils.formatDataUsage(this.mContext, this.mDataBarSize));
                dataUsageSummaryPreference.setProgress(this.mDataplanUse / this.mDataBarSize);
            }
            dataUsageSummaryPreference.setUsageInfo(this.mCycleEnd, this.mSnapshotTime, this.mCarrierName, this.mDataplanCount, this.mManageSubscriptionIntent);
            return;
        }
        DataUsageController.DataUsageInfo dataUsageInfo2 = this.mDataUsageController.getDataUsageInfo(NetworkTemplate.buildTemplateWifiWildcard());
        dataUsageSummaryPreference.setWifiMode(true, dataUsageInfo2.period);
        dataUsageSummaryPreference.setLimitInfo(null);
        dataUsageSummaryPreference.setUsageNumbers(dataUsageInfo2.usageLevel, -1L, true);
        dataUsageSummaryPreference.setChartEnabled(false);
        dataUsageSummaryPreference.setUsageInfo(dataUsageInfo2.cycleEnd, -1L, null, 0, null);
    }

    private void refreshDataplanInfo(DataUsageController.DataUsageInfo dataUsageInfo) {
        this.mCarrierName = null;
        this.mDataplanCount = 0;
        this.mDataplanSize = -1L;
        this.mDataBarSize = this.mDataInfoController.getSummaryLimit(dataUsageInfo);
        this.mDataplanUse = dataUsageInfo.usageLevel;
        this.mCycleStart = dataUsageInfo.cycleStart;
        this.mCycleEnd = dataUsageInfo.cycleEnd;
        this.mSnapshotTime = -1L;
        int defaultSubscriptionId = SubscriptionManager.getDefaultSubscriptionId();
        SubscriptionInfo defaultDataSubscriptionInfo = this.mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (defaultDataSubscriptionInfo != null && this.mHasMobileData) {
            this.mCarrierName = defaultDataSubscriptionInfo.getCarrierName();
            List<SubscriptionPlan> subscriptionPlans = this.mSubscriptionManager.getSubscriptionPlans(defaultSubscriptionId);
            SubscriptionPlan primaryPlan = getPrimaryPlan(this.mSubscriptionManager, defaultSubscriptionId);
            if (primaryPlan != null) {
                this.mDataplanCount = subscriptionPlans.size();
                this.mDataplanSize = primaryPlan.getDataLimitBytes();
                if (unlimited(this.mDataplanSize)) {
                    this.mDataplanSize = -1L;
                }
                this.mDataBarSize = this.mDataplanSize;
                this.mDataplanUse = primaryPlan.getDataUsageBytes();
                RecurrenceRule cycleRule = primaryPlan.getCycleRule();
                if (cycleRule != null && cycleRule.start != null && cycleRule.end != null) {
                    this.mCycleStart = cycleRule.start.toEpochSecond() * 1000;
                    this.mCycleEnd = cycleRule.end.toEpochSecond() * 1000;
                }
                this.mSnapshotTime = primaryPlan.getDataUsageTime();
            }
        }
        this.mManageSubscriptionIntent = this.mSubscriptionManager.createManageSubscriptionIntent(defaultSubscriptionId);
        Log.i(TAG, "Have " + this.mDataplanCount + " plans, dflt sub-id " + defaultSubscriptionId + ", intent " + this.mManageSubscriptionIntent);
    }

    public static SubscriptionPlan getPrimaryPlan(SubscriptionManager subscriptionManager, int i) {
        List<SubscriptionPlan> subscriptionPlans = subscriptionManager.getSubscriptionPlans(i);
        if (CollectionUtils.isEmpty(subscriptionPlans)) {
            return null;
        }
        SubscriptionPlan subscriptionPlan = subscriptionPlans.get(0);
        if (subscriptionPlan.getDataLimitBytes() <= 0 || !saneSize(subscriptionPlan.getDataUsageBytes()) || subscriptionPlan.getCycleRule() == null) {
            return null;
        }
        return subscriptionPlan;
    }

    private static boolean saneSize(long j) {
        return j >= 0 && j < PETA;
    }

    public static boolean unlimited(long j) {
        return j == Long.MAX_VALUE;
    }
}
