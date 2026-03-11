package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.IconDrawableFactory;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipDialogFragment;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;
import com.android.settings.widget.AppCheckBoxPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.FooterPreferenceMixin;
import java.util.List;

public class RestrictedAppDetails extends DashboardFragment implements BatteryTipPreferenceController.BatteryTipListener {

    @VisibleForTesting
    static final String EXTRA_APP_INFO_LIST = "app_info_list";

    @VisibleForTesting
    List<AppInfo> mAppInfos;

    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    private final FooterPreferenceMixin mFooterPreferenceMixin = new FooterPreferenceMixin(this, getLifecycle());

    @VisibleForTesting
    IconDrawableFactory mIconDrawableFactory;

    @VisibleForTesting
    PackageManager mPackageManager;

    @VisibleForTesting
    PreferenceGroup mRestrictedAppListGroup;

    public static void startRestrictedAppDetails(InstrumentedPreferenceFragment instrumentedPreferenceFragment, List<AppInfo> list) {
        Bundle bundle = new Bundle();
        bundle.putParcelableList(EXTRA_APP_INFO_LIST, list);
        new SubSettingLauncher(instrumentedPreferenceFragment.getContext()).setDestination(RestrictedAppDetails.class.getName()).setArguments(bundle).setTitle(R.string.restricted_app_title).setSourceMetricsCategory(instrumentedPreferenceFragment.getMetricsCategory()).launch();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Context context = getContext();
        this.mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.restricted_app_detail_footer);
        this.mRestrictedAppListGroup = (PreferenceGroup) findPreference("restrict_app_list");
        this.mAppInfos = getArguments().getParcelableArrayList(EXTRA_APP_INFO_LIST);
        this.mPackageManager = context.getPackageManager();
        this.mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        this.mBatteryUtils = BatteryUtils.getInstance(context);
        refreshUi();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected String getLogTag() {
        return "RestrictedAppDetails";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.restricted_apps_detail;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return 1285;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_restricted_apps;
    }

    @VisibleForTesting
    void refreshUi() {
        this.mRestrictedAppListGroup.removeAll();
        Context prefContext = getPrefContext();
        int size = this.mAppInfos.size();
        for (int i = 0; i < size; i++) {
            AppCheckBoxPreference appCheckBoxPreference = new AppCheckBoxPreference(prefContext);
            final AppInfo appInfo = this.mAppInfos.get(i);
            try {
                ApplicationInfo applicationInfoAsUser = this.mPackageManager.getApplicationInfoAsUser(appInfo.packageName, 0, UserHandle.getUserId(appInfo.uid));
                appCheckBoxPreference.setChecked(this.mBatteryUtils.isForceAppStandbyEnabled(appInfo.uid, appInfo.packageName));
                appCheckBoxPreference.setTitle(this.mPackageManager.getApplicationLabel(applicationInfoAsUser));
                appCheckBoxPreference.setIcon(Utils.getBadgedIcon(this.mIconDrawableFactory, this.mPackageManager, appInfo.packageName, UserHandle.getUserId(appInfo.uid)));
                appCheckBoxPreference.setKey(getKeyFromAppInfo(appInfo));
                appCheckBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public final boolean onPreferenceChange(Preference preference, Object obj) {
                        return RestrictedAppDetails.lambda$refreshUi$0(this.f$0, appInfo, preference, obj);
                    }
                });
                this.mRestrictedAppListGroup.addPreference(appCheckBoxPreference);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean lambda$refreshUi$0(RestrictedAppDetails restrictedAppDetails, AppInfo appInfo, Preference preference, Object obj) {
        BatteryTipDialogFragment batteryTipDialogFragmentCreateDialogFragment = restrictedAppDetails.createDialogFragment(appInfo, ((Boolean) obj).booleanValue());
        batteryTipDialogFragmentCreateDialogFragment.setTargetFragment(restrictedAppDetails, 0);
        batteryTipDialogFragmentCreateDialogFragment.show(restrictedAppDetails.getFragmentManager(), "RestrictedAppDetails");
        return false;
    }

    @Override
    public void onBatteryTipHandled(BatteryTip batteryTip) {
        AppInfo unrestrictAppInfo;
        boolean z = batteryTip instanceof RestrictAppTip;
        if (z) {
            unrestrictAppInfo = ((RestrictAppTip) batteryTip).getRestrictAppList().get(0);
        } else {
            unrestrictAppInfo = ((UnrestrictAppTip) batteryTip).getUnrestrictAppInfo();
        }
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) this.mRestrictedAppListGroup.findPreference(getKeyFromAppInfo(unrestrictAppInfo));
        if (checkBoxPreference != null) {
            checkBoxPreference.setChecked(z);
        }
    }

    @VisibleForTesting
    BatteryTipDialogFragment createDialogFragment(AppInfo appInfo, boolean z) {
        BatteryTip unrestrictAppTip;
        if (z) {
            unrestrictAppTip = new RestrictAppTip(0, appInfo);
        } else {
            unrestrictAppTip = new UnrestrictAppTip(0, appInfo);
        }
        return BatteryTipDialogFragment.newInstance(unrestrictAppTip, getMetricsCategory());
    }

    @VisibleForTesting
    String getKeyFromAppInfo(AppInfo appInfo) {
        return appInfo.uid + "," + appInfo.packageName;
    }
}
