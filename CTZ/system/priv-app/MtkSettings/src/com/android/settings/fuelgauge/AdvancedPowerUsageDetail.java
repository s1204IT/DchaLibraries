package com.android.settings.fuelgauge;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.LoaderManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.ButtonActionDialogFragment;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDialogFragment;
import com.android.settings.fuelgauge.anomaly.AnomalyLoader;
import com.android.settings.fuelgauge.anomaly.AnomalySummaryPreferenceController;
import com.android.settings.fuelgauge.anomaly.AnomalyUtils;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.StringUtil;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class AdvancedPowerUsageDetail extends DashboardFragment implements LoaderManager.LoaderCallbacks<List<Anomaly>>, ButtonActionDialogFragment.AppButtonsDialogListener, AnomalyDialogFragment.AnomalyDialogListener, BatteryTipPreferenceController.BatteryTipListener {
    private List<Anomaly> mAnomalies;
    AnomalySummaryPreferenceController mAnomalySummaryPreferenceController;
    private AppButtonsPreferenceController mAppButtonsPreferenceController;
    ApplicationsState.AppEntry mAppEntry;
    private BackgroundActivityPreferenceController mBackgroundActivityPreferenceController;
    Preference mBackgroundPreference;
    BatteryUtils mBatteryUtils;
    private DevicePolicyManager mDpm;
    Preference mForegroundPreference;
    LayoutPreference mHeaderPreference;
    private PackageManager mPackageManager;
    private String mPackageName;
    ApplicationsState mState;
    private UserManager mUserManager;

    static void startBatteryDetailPage(Activity activity, BatteryUtils batteryUtils, InstrumentedPreferenceFragment instrumentedPreferenceFragment, BatteryStatsHelper batteryStatsHelper, int i, BatteryEntry batteryEntry, String str, List<Anomaly> list) {
        String str2;
        batteryStatsHelper.getStats();
        Bundle bundle = new Bundle();
        BatterySipper batterySipper = batteryEntry.sipper;
        BatteryStats.Uid uid = batterySipper.uidObj;
        boolean z = batterySipper.drainType == BatterySipper.DrainType.APP;
        long processTimeMs = z ? batteryUtils.getProcessTimeMs(1, uid, i) : batterySipper.usageTimeMs;
        long processTimeMs2 = z ? batteryUtils.getProcessTimeMs(2, uid, i) : 0L;
        if (ArrayUtils.isEmpty(batterySipper.mPackages)) {
            bundle.putString("extra_label", batteryEntry.getLabel());
            bundle.putInt("extra_icon_id", batteryEntry.iconId);
            bundle.putString("extra_package_name", null);
        } else {
            if (batteryEntry.defaultPackageName != null) {
                str2 = batteryEntry.defaultPackageName;
            } else {
                str2 = batterySipper.mPackages[0];
            }
            bundle.putString("extra_package_name", str2);
        }
        bundle.putInt("extra_uid", batterySipper.getUid());
        bundle.putLong("extra_background_time", processTimeMs2);
        bundle.putLong("extra_foreground_time", processTimeMs);
        bundle.putString("extra_power_usage_percent", str);
        bundle.putInt("extra_power_usage_amount", (int) batterySipper.totalPowerMah);
        bundle.putParcelableList("extra_anomaly_list", list);
        new SubSettingLauncher(activity).setDestination(AdvancedPowerUsageDetail.class.getName()).setTitle(R.string.battery_details_title).setArguments(bundle).setSourceMetricsCategory(instrumentedPreferenceFragment.getMetricsCategory()).setUserHandle(new UserHandle(getUserIdToLaunchAdvancePowerUsageDetail(batterySipper))).launch();
    }

    private static int getUserIdToLaunchAdvancePowerUsageDetail(BatterySipper batterySipper) {
        if (batterySipper.drainType == BatterySipper.DrainType.USER) {
            return ActivityManager.getCurrentUser();
        }
        return UserHandle.getUserId(batterySipper.getUid());
    }

    public static void startBatteryDetailPage(Activity activity, InstrumentedPreferenceFragment instrumentedPreferenceFragment, BatteryStatsHelper batteryStatsHelper, int i, BatteryEntry batteryEntry, String str, List<Anomaly> list) {
        startBatteryDetailPage(activity, BatteryUtils.getInstance(activity), instrumentedPreferenceFragment, batteryStatsHelper, i, batteryEntry, str, list);
    }

    public static void startBatteryDetailPage(Activity activity, InstrumentedPreferenceFragment instrumentedPreferenceFragment, String str) {
        Bundle bundle = new Bundle(3);
        PackageManager packageManager = activity.getPackageManager();
        bundle.putString("extra_package_name", str);
        bundle.putString("extra_power_usage_percent", Utils.formatPercentage(0));
        try {
            bundle.putInt("extra_uid", packageManager.getPackageUid(str, 0));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("AdvancedPowerDetail", "Cannot find package: " + str, e);
        }
        new SubSettingLauncher(activity).setDestination(AdvancedPowerUsageDetail.class.getName()).setTitle(R.string.battery_details_title).setArguments(bundle).setSourceMetricsCategory(instrumentedPreferenceFragment.getMetricsCategory()).launch();
    }

    @Override // android.app.Fragment
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mState = ApplicationsState.getInstance(getActivity().getApplication());
        this.mDpm = (DevicePolicyManager) activity.getSystemService("device_policy");
        this.mUserManager = (UserManager) activity.getSystemService("user");
        this.mPackageManager = activity.getPackageManager();
        this.mBatteryUtils = BatteryUtils.getInstance(getContext());
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) throws Resources.NotFoundException {
        super.onCreate(bundle);
        this.mPackageName = getArguments().getString("extra_package_name");
        this.mAnomalySummaryPreferenceController = new AnomalySummaryPreferenceController((SettingsActivity) getActivity(), this);
        this.mForegroundPreference = findPreference("app_usage_foreground");
        this.mBackgroundPreference = findPreference("app_usage_background");
        this.mHeaderPreference = (LayoutPreference) findPreference("header_view");
        if (this.mPackageName != null) {
            this.mAppEntry = this.mState.getEntry(this.mPackageName, UserHandle.myUserId());
            initAnomalyInfo();
        }
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        initHeader();
        initPreference();
    }

    void initAnomalyInfo() throws Resources.NotFoundException {
        this.mAnomalies = getArguments().getParcelableArrayList("extra_anomaly_list");
        if (this.mAnomalies == null) {
            getLoaderManager().initLoader(0, Bundle.EMPTY, this);
        } else if (this.mAnomalies != null) {
            this.mAnomalySummaryPreferenceController.updateAnomalySummaryPreference(this.mAnomalies);
        }
    }

    void initHeader() {
        View viewFindViewById = this.mHeaderPreference.findViewById(R.id.entity_header);
        Activity activity = getActivity();
        Bundle arguments = getArguments();
        EntityHeaderController buttonActions = EntityHeaderController.newInstance(activity, this, viewFindViewById).setRecyclerView(getListView(), getLifecycle()).setButtonActions(0, 0);
        if (this.mAppEntry == null) {
            buttonActions.setLabel(arguments.getString("extra_label"));
            if (arguments.getInt("extra_icon_id", 0) == 0) {
                buttonActions.setIcon(activity.getPackageManager().getDefaultActivityIcon());
            } else {
                buttonActions.setIcon(activity.getDrawable(arguments.getInt("extra_icon_id")));
            }
        } else {
            this.mState.ensureIcon(this.mAppEntry);
            buttonActions.setLabel(this.mAppEntry);
            buttonActions.setIcon(this.mAppEntry);
            String string = AppUtils.isInstant(this.mAppEntry.info) ? null : getString(Utils.getInstallationStatus(this.mAppEntry.info));
            buttonActions.setIsInstantApp(AppUtils.isInstant(this.mAppEntry.info));
            buttonActions.setSummary(string);
        }
        buttonActions.done(activity, true);
    }

    void initPreference() {
        Bundle arguments = getArguments();
        Context context = getContext();
        long j = arguments.getLong("extra_foreground_time");
        long j2 = arguments.getLong("extra_background_time");
        arguments.getString("extra_power_usage_percent");
        arguments.getInt("extra_power_usage_amount");
        this.mForegroundPreference.setSummary(TextUtils.expandTemplate(getText(R.string.battery_used_for), StringUtil.formatElapsedTime(context, j, false)));
        this.mBackgroundPreference.setSummary(TextUtils.expandTemplate(getText(R.string.battery_active_for), StringUtil.formatElapsedTime(context, j2, false)));
    }

    @Override // com.android.settings.dashboard.DashboardFragment, android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), "high_usage")) {
            this.mAnomalySummaryPreferenceController.onPreferenceTreeClick(preference);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 53;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "AdvancedPowerDetail";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_detail;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        Bundle arguments = getArguments();
        int i = arguments.getInt("extra_uid", 0);
        String string = arguments.getString("extra_package_name");
        this.mBackgroundActivityPreferenceController = new BackgroundActivityPreferenceController(context, this, i, string);
        arrayList.add(this.mBackgroundActivityPreferenceController);
        arrayList.add(new BatteryOptimizationPreferenceController((SettingsActivity) getActivity(), this, string));
        this.mAppButtonsPreferenceController = new AppButtonsPreferenceController((SettingsActivity) getActivity(), this, getLifecycle(), string, this.mState, this.mDpm, this.mUserManager, this.mPackageManager, 0, 1);
        arrayList.add(this.mAppButtonsPreferenceController);
        return arrayList;
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (this.mAppButtonsPreferenceController != null) {
            this.mAppButtonsPreferenceController.handleActivityResult(i, i2, intent);
        }
    }

    @Override // com.android.settings.fuelgauge.ButtonActionDialogFragment.AppButtonsDialogListener
    public void handleDialogClick(int i) {
        if (this.mAppButtonsPreferenceController != null) {
            this.mAppButtonsPreferenceController.handleDialogClick(i);
        }
    }

    @Override // com.android.settings.fuelgauge.anomaly.AnomalyDialogFragment.AnomalyDialogListener
    public void onAnomalyHandled(Anomaly anomaly) {
        this.mAnomalySummaryPreferenceController.hideHighUsagePreference();
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public Loader<List<Anomaly>> onCreateLoader(int i, Bundle bundle) {
        return new AnomalyLoader(getContext(), this.mPackageName);
    }

    /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoadFinished(Loader<List<Anomaly>> loader, List<Anomaly> list) throws Resources.NotFoundException {
        AnomalyUtils.getInstance(getContext()).logAnomalies(this.mMetricsFeatureProvider, list, 53);
        this.mAnomalySummaryPreferenceController.updateAnomalySummaryPreference(list);
    }

    @Override // android.app.LoaderManager.LoaderCallbacks
    public void onLoaderReset(Loader<List<Anomaly>> loader) {
    }

    @Override // com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController.BatteryTipListener
    public void onBatteryTipHandled(BatteryTip batteryTip) {
        this.mBackgroundActivityPreferenceController.updateSummary(findPreference(this.mBackgroundActivityPreferenceController.getPreferenceKey()));
    }
}
