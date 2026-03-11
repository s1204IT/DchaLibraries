package com.android.settings.applications;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.icu.text.ListFormatter;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.IWebViewUpdateService;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.DeviceAdminAdd;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.PermissionsSummaryHelper;
import com.android.settings.datausage.AppDataUsage;
import com.android.settings.datausage.DataUsageList;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.fuelgauge.BatteryEntry;
import com.android.settings.fuelgauge.PowerUsageDetail;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.AppItem;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.ChartDataLoader;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IAppsExt;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class InstalledAppDetails extends AppInfoBase implements View.OnClickListener, Preference.OnPreferenceClickListener {
    private BatteryStatsHelper mBatteryHelper;
    private Preference mBatteryPreference;
    private ChartData mChartData;
    private Preference mDataPreference;
    private boolean mDisableAfterUninstall;
    private Button mForceStopButton;
    private LayoutPreference mHeader;
    private boolean mInitialized;
    private Preference mLaunchPreference;
    private Preference mMemoryPreference;
    private Preference mNotificationPreference;
    private Preference mPermissionsPreference;
    private boolean mShowUninstalled;
    private BatterySipper mSipper;
    protected ProcStatsPackageEntry mStats;
    protected ProcStatsData mStatsManager;
    private INetworkStatsSession mStatsSession;
    private Preference mStoragePreference;
    private Button mUninstallButton;
    private final HashSet<String> mHomePackages = new HashSet<>();
    private boolean mUpdatedSysApp = false;
    private final NotificationBackend mBackend = new NotificationBackend();
    private final LoaderManager.LoaderCallbacks<ChartData> mDataCallbacks = new LoaderManager.LoaderCallbacks<ChartData>() {
        @Override
        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(InstalledAppDetails.this.getActivity(), InstalledAppDetails.this.mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            InstalledAppDetails.this.mChartData = data;
            InstalledAppDetails.this.mDataPreference.setSummary(InstalledAppDetails.this.getDataSummary());
        }

        @Override
        public void onLoaderReset(Loader<ChartData> loader) {
        }
    };
    private final BroadcastReceiver mCheckKillProcessesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            InstalledAppDetails.this.updateForceStopButton(getResultCode() != 0);
        }
    };
    private final PermissionsSummaryHelper.PermissionsResultCallback mPermissionCallback = new PermissionsSummaryHelper.PermissionsResultCallback() {
        @Override
        public void onPermissionSummaryResult(int standardGrantedPermissionCount, int requestedPermissionCount, int additionalGrantedPermissionCount, List<CharSequence> grantedGroupLabels) {
            CharSequence summary;
            if (InstalledAppDetails.this.getActivity() == null) {
                return;
            }
            Resources res = InstalledAppDetails.this.getResources();
            if (requestedPermissionCount == 0) {
                summary = res.getString(R.string.runtime_permissions_summary_no_permissions_requested);
                InstalledAppDetails.this.mPermissionsPreference.setOnPreferenceClickListener(null);
                InstalledAppDetails.this.mPermissionsPreference.setEnabled(false);
            } else {
                ArrayList<CharSequence> list = new ArrayList<>(grantedGroupLabels);
                if (additionalGrantedPermissionCount > 0) {
                    list.add(res.getQuantityString(R.plurals.runtime_permissions_additional_count, additionalGrantedPermissionCount, Integer.valueOf(additionalGrantedPermissionCount)));
                }
                if (list.size() == 0) {
                    summary = res.getString(R.string.runtime_permissions_summary_no_permissions_granted);
                } else {
                    summary = ListFormatter.getInstance().format(list);
                }
                InstalledAppDetails.this.mPermissionsPreference.setOnPreferenceClickListener(InstalledAppDetails.this);
                InstalledAppDetails.this.mPermissionsPreference.setEnabled(true);
            }
            InstalledAppDetails.this.mPermissionsPreference.setSummary(summary);
        }
    };

    private boolean handleDisableable(Button button) {
        if (this.mHomePackages.contains(this.mAppEntry.info.packageName) || Utils.isSystemPackage(this.mPm, this.mPackageInfo)) {
            button.setText(R.string.disable_text);
            return false;
        }
        if (UtilsExt.disableAppList != null && UtilsExt.disableAppList.contains(this.mAppEntry.info.packageName)) {
            Log.d(TAG, "mDisableAppsList contains :" + this.mAppEntry.info.packageName);
            button.setText(R.string.disable_text);
            return false;
        }
        if (this.mAppEntry.info.enabled && !isDisabledUntilUsed()) {
            button.setText(R.string.disable_text);
            return true;
        }
        button.setText(R.string.enable_text);
        return true;
    }

    private boolean isDisabledUntilUsed() {
        return this.mAppEntry.info.enabledSetting == 4;
    }

    private void initUninstallButtons() {
        boolean isBundled = (this.mAppEntry.info.flags & 1) != 0;
        boolean enabled = true;
        if (isBundled) {
            enabled = handleDisableable(this.mUninstallButton);
        } else {
            if ((this.mPackageInfo.applicationInfo.flags & 8388608) == 0 && this.mUserManager.getUsers().size() >= 2) {
                enabled = false;
            }
            this.mUninstallButton.setText(R.string.uninstall_text);
        }
        if (isBundled && this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName)) {
            enabled = false;
        }
        if (isProfileOrDeviceOwner(this.mPackageInfo.packageName)) {
            enabled = false;
        }
        if (this.mDpm.isUninstallInQueue(this.mPackageName)) {
            enabled = false;
        }
        if (enabled && this.mHomePackages.contains(this.mPackageInfo.packageName)) {
            if (isBundled) {
                enabled = false;
            } else {
                ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
                ComponentName currentDefaultHome = this.mPm.getHomeActivities(homeActivities);
                if (currentDefaultHome == null) {
                    enabled = this.mHomePackages.size() > 1;
                } else {
                    enabled = !this.mPackageInfo.packageName.equals(currentDefaultHome.getPackageName());
                }
            }
        }
        if (this.mAppsControlDisallowedBySystem) {
            enabled = false;
        }
        try {
            IWebViewUpdateService webviewUpdateService = IWebViewUpdateService.Stub.asInterface(ServiceManager.getService("webviewupdate"));
            if (webviewUpdateService.isFallbackPackage(this.mAppEntry.info.packageName)) {
                enabled = false;
            }
            this.mUninstallButton.setEnabled(enabled);
            if (!enabled) {
                return;
            }
            this.mUninstallButton.setOnClickListener(this);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isProfileOrDeviceOwner(String packageName) {
        List<UserInfo> userInfos = this.mUserManager.getUsers();
        DevicePolicyManager dpm = (DevicePolicyManager) getContext().getSystemService("device_policy");
        if (dpm.isDeviceOwnerAppOnAnyUser(packageName)) {
            return true;
        }
        for (UserInfo userInfo : userInfos) {
            ComponentName cn = dpm.getProfileOwnerAsUser(userInfo.id);
            if (cn != null && cn.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (Utils.isMonkeyRunning()) {
            Log.d(TAG, "monkey is running, finish");
            getActivity().finish();
        }
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.installed_app_details);
        addDynamicPrefs();
        if (Utils.isBandwidthControlEnabled()) {
            INetworkStatsService statsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats"));
            try {
                this.mStatsSession = statsService.openSession();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        } else {
            removePreference("data_settings");
        }
        this.mBatteryHelper = new BatteryStatsHelper(getActivity(), true);
    }

    @Override
    protected int getMetricsCategory() {
        return 20;
    }

    @Override
    public void onResume() {
        BatteryUpdater batteryUpdater = null;
        Object[] objArr = 0;
        super.onResume();
        if (this.mFinishing) {
            return;
        }
        this.mState.requestSize(this.mPackageName, this.mUserId);
        AppItem appItem = new AppItem(this.mAppEntry.info.uid);
        appItem.addUid(this.mAppEntry.info.uid);
        if (this.mStatsSession != null) {
            getLoaderManager().restartLoader(2, ChartDataLoader.buildArgs(getTemplate(getContext()), appItem), this.mDataCallbacks);
        }
        new BatteryUpdater(this, batteryUpdater).execute(new Void[0]);
        new MemoryUpdater(this, objArr == true ? 1 : 0).execute(new Void[0]);
        updateDynamicPrefs();
    }

    @Override
    public void onPause() {
        getLoaderManager().destroyLoader(2);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        TrafficStats.closeQuietly(this.mStatsSession);
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (this.mFinishing) {
            return;
        }
        handleHeader();
        this.mNotificationPreference = findPreference("notification_settings");
        this.mNotificationPreference.setOnPreferenceClickListener(this);
        this.mStoragePreference = findPreference("storage_settings");
        this.mStoragePreference.setOnPreferenceClickListener(this);
        this.mPermissionsPreference = findPreference("permission_settings");
        this.mPermissionsPreference.setOnPreferenceClickListener(this);
        this.mDataPreference = findPreference("data_settings");
        if (this.mDataPreference != null) {
            this.mDataPreference.setOnPreferenceClickListener(this);
        }
        this.mBatteryPreference = findPreference("battery");
        this.mBatteryPreference.setEnabled(false);
        this.mBatteryPreference.setOnPreferenceClickListener(this);
        this.mMemoryPreference = findPreference("memory");
        this.mMemoryPreference.setOnPreferenceClickListener(this);
        this.mLaunchPreference = findPreference("preferred_settings");
        if (this.mAppEntry == null || this.mAppEntry.info == null || (this.mAppEntry.info.flags & 8388608) == 0 || !this.mAppEntry.info.enabled) {
            this.mLaunchPreference.setEnabled(false);
        } else {
            this.mLaunchPreference.setOnPreferenceClickListener(this);
        }
        IAppsExt appExt = UtilsExt.getAppsExtPlugin(getActivity());
        if (this.mAppEntry == null || this.mAppEntry.info == null) {
            return;
        }
        appExt.launchApp(getPreferenceScreen(), this.mAppEntry.info.packageName);
    }

    private void handleHeader() {
        this.mHeader = (LayoutPreference) findPreference("header_view");
        View btnPanel = this.mHeader.findViewById(R.id.control_buttons_panel);
        this.mForceStopButton = (Button) btnPanel.findViewById(R.id.right_button);
        this.mForceStopButton.setText(R.string.force_stop);
        this.mUninstallButton = (Button) btnPanel.findViewById(R.id.left_button);
        this.mForceStopButton.setEnabled(false);
        View gear = this.mHeader.findViewById(R.id.gear);
        Intent i = new Intent("android.intent.action.APPLICATION_PREFERENCES");
        i.setPackage(this.mPackageName);
        final Intent intent = resolveIntent(i);
        if (intent != null) {
            gear.setVisibility(0);
            gear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InstalledAppDetails.this.startActivity(intent);
                }
            });
        } else {
            gear.setVisibility(8);
        }
    }

    private Intent resolveIntent(Intent i) {
        ResolveInfo result = getContext().getPackageManager().resolveActivity(i, 0);
        if (result != null) {
            return new Intent(i.getAction()).setClassName(result.activityInfo.packageName, result.activityInfo.name);
        }
        return null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, 2, 0, R.string.app_factory_reset).setShowAsAction(0);
        menu.add(0, 1, 1, R.string.uninstall_all_users_text).setShowAsAction(0);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean z = false;
        if (this.mFinishing) {
            return;
        }
        boolean showIt = true;
        if (this.mUpdatedSysApp || this.mAppEntry == null || (this.mAppEntry.info.flags & 1) != 0 || this.mPackageInfo == null || this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName) || UserHandle.myUserId() != 0 || this.mUserManager.getUsers().size() < 2) {
            showIt = false;
        }
        menu.findItem(1).setVisible(showIt);
        if (this.mAppEntry != null && this.mAppEntry.info != null) {
            this.mUpdatedSysApp = (this.mAppEntry.info.flags & 128) != 0;
        }
        this.mUpdatedSysApp = (this.mAppEntry.info.flags & 128) != 0;
        MenuItem uninstallUpdatesItem = menu.findItem(2);
        if (this.mUpdatedSysApp && !this.mAppsControlDisallowedBySystem) {
            z = true;
        }
        uninstallUpdatesItem.setVisible(z);
        if (!uninstallUpdatesItem.isVisible()) {
            return;
        }
        RestrictedLockUtils.setMenuItemAsDisabledByAdmin(getActivity(), uninstallUpdatesItem, this.mAppsControlDisallowedAdmin);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DefaultWfcSettingsExt.PAUSE:
                uninstallPkg(this.mAppEntry.info.packageName, true, false);
                return true;
            case DefaultWfcSettingsExt.CREATE:
                uninstallPkg(this.mAppEntry.info.packageName, false, false);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (this.mDisableAfterUninstall) {
                this.mDisableAfterUninstall = false;
                new DisableChanger(this, this.mAppEntry.info, 3).execute((Object) null);
            }
            if (!refreshUi()) {
                setIntentAndFinish(true, true);
            }
        }
        if (requestCode != 1 || refreshUi()) {
            return;
        }
        setIntentAndFinish(true, true);
    }

    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        View appSnippet = this.mHeader.findViewById(R.id.app_snippet);
        this.mState.ensureIcon(this.mAppEntry);
        setupAppSnippet(appSnippet, this.mAppEntry.label, this.mAppEntry.icon, pkgInfo != null ? pkgInfo.versionName : null);
    }

    private boolean signaturesMatch(String pkg1, String pkg2) {
        if (pkg1 != null && pkg2 != null) {
            try {
                int match = this.mPm.checkSignatures(pkg1, pkg2);
                if (match >= 0) {
                    return true;
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    @Override
    protected boolean refreshUi() {
        retrieveAppEntry();
        if (this.mAppEntry == null || this.mPackageInfo == null) {
            return false;
        }
        List<ResolveInfo> homeActivities = new ArrayList<>();
        this.mPm.getHomeActivities(homeActivities);
        this.mHomePackages.clear();
        for (int i = 0; i < homeActivities.size(); i++) {
            ResolveInfo ri = homeActivities.get(i);
            String activityPkg = ri.activityInfo.packageName;
            this.mHomePackages.add(activityPkg);
            Bundle metadata = ri.activityInfo.metaData;
            if (metadata != null) {
                String metaPkg = metadata.getString("android.app.home.alternate");
                if (signaturesMatch(metaPkg, activityPkg)) {
                    this.mHomePackages.add(metaPkg);
                }
            }
        }
        checkForceStop();
        setAppLabelAndIcon(this.mPackageInfo);
        initUninstallButtons();
        Activity context = getActivity();
        this.mStoragePreference.setSummary(AppStorageSettings.getSummary(this.mAppEntry, context));
        PermissionsSummaryHelper.getPermissionSummary(getContext(), this.mPackageName, this.mPermissionCallback);
        this.mLaunchPreference.setSummary(AppUtils.getLaunchByDefaultSummary(this.mAppEntry, this.mUsbManager, this.mPm, context));
        this.mNotificationPreference.setSummary(getNotificationSummary(this.mAppEntry, context, this.mBackend));
        if (this.mDataPreference != null) {
            this.mDataPreference.setSummary(getDataSummary());
        }
        updateBattery();
        if (!this.mInitialized) {
            this.mInitialized = true;
            this.mShowUninstalled = (this.mAppEntry.info.flags & 8388608) == 0;
            return true;
        }
        try {
            ApplicationInfo ainfo = context.getPackageManager().getApplicationInfo(this.mAppEntry.info.packageName, 8704);
            if (this.mShowUninstalled) {
                return true;
            }
            return (ainfo.flags & 8388608) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void updateBattery() {
        if (this.mSipper == null) {
            this.mBatteryPreference.setEnabled(false);
            this.mBatteryPreference.setSummary(getString(R.string.no_battery_summary));
        } else {
            this.mBatteryPreference.setEnabled(true);
            int dischargeAmount = this.mBatteryHelper.getStats().getDischargeAmount(0);
            int percentOfMax = (int) (((this.mSipper.totalPowerMah / this.mBatteryHelper.getTotalPower()) * ((double) dischargeAmount)) + 0.5d);
            this.mBatteryPreference.setSummary(getString(R.string.battery_summary, new Object[]{Integer.valueOf(percentOfMax)}));
        }
    }

    public CharSequence getDataSummary() {
        if (this.mChartData != null) {
            long totalBytes = this.mChartData.detail.getTotalBytes();
            if (totalBytes == 0) {
                return getString(R.string.no_data_usage);
            }
            Context context = getActivity();
            return getString(R.string.data_summary_format, new Object[]{Formatter.formatFileSize(context, totalBytes), DateUtils.formatDateTime(context, this.mChartData.detail.getStart(), 65552)});
        }
        return getString(R.string.computing_size);
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        switch (id) {
            case DefaultWfcSettingsExt.PAUSE:
                return new AlertDialog.Builder(getActivity()).setTitle(getActivity().getText(R.string.force_stop_dlg_title)).setMessage(getActivity().getText(R.string.force_stop_dlg_text)).setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InstalledAppDetails.this.forceStopPackage(InstalledAppDetails.this.mAppEntry.info.packageName);
                    }
                }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            case DefaultWfcSettingsExt.CREATE:
                return new AlertDialog.Builder(getActivity()).setMessage(getActivity().getText(R.string.app_disable_dlg_text)).setPositiveButton(R.string.app_disable_dlg_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DisableChanger(InstalledAppDetails.this, InstalledAppDetails.this.mAppEntry.info, 3).execute((Object) null);
                    }
                }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            case DefaultWfcSettingsExt.DESTROY:
                return new AlertDialog.Builder(getActivity()).setMessage(getActivity().getText(R.string.app_disable_dlg_text)).setPositiveButton(R.string.app_disable_dlg_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InstalledAppDetails.this.uninstallPkg(InstalledAppDetails.this.mAppEntry.info.packageName, false, true);
                    }
                }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            default:
                return null;
        }
    }

    public void uninstallPkg(String packageName, boolean allUsers, boolean andDisable) {
        Uri packageURI = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent("android.intent.action.UNINSTALL_PACKAGE", packageURI);
        uninstallIntent.putExtra("android.intent.extra.UNINSTALL_ALL_USERS", allUsers);
        startActivityForResult(uninstallIntent, 0);
        this.mDisableAfterUninstall = andDisable;
    }

    public void forceStopPackage(String pkgName) {
        ActivityManager am = (ActivityManager) getActivity().getSystemService("activity");
        am.forceStopPackage(pkgName);
        int userId = UserHandle.getUserId(this.mAppEntry.info.uid);
        this.mState.invalidatePackage(pkgName, userId);
        ApplicationsState.AppEntry newEnt = this.mState.getEntry(pkgName, userId);
        if (newEnt != null) {
            this.mAppEntry = newEnt;
        }
        checkForceStop();
    }

    public void updateForceStopButton(boolean enabled) {
        if (this.mAppsControlDisallowedBySystem) {
            this.mForceStopButton.setEnabled(false);
        } else {
            this.mForceStopButton.setEnabled(enabled);
            this.mForceStopButton.setOnClickListener(this);
        }
    }

    private void checkForceStop() {
        if (this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName)) {
            updateForceStopButton(false);
            return;
        }
        if ((this.mAppEntry.info.flags & 2097152) == 0) {
            updateForceStopButton(true);
            return;
        }
        Intent intent = new Intent("android.intent.action.QUERY_PACKAGE_RESTART", Uri.fromParts("package", this.mAppEntry.info.packageName, null));
        intent.putExtra("android.intent.extra.PACKAGES", new String[]{this.mAppEntry.info.packageName});
        intent.putExtra("android.intent.extra.UID", this.mAppEntry.info.uid);
        intent.putExtra("android.intent.extra.user_handle", UserHandle.getUserId(this.mAppEntry.info.uid));
        getActivity().sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null, this.mCheckKillProcessesReceiver, null, 0, null, null);
    }

    private void startManagePermissionsActivity() {
        Intent intent = new Intent("android.intent.action.MANAGE_APP_PERMISSIONS");
        intent.putExtra("android.intent.extra.PACKAGE_NAME", this.mAppEntry.info.packageName);
        intent.putExtra("hideInfoButton", true);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w("InstalledAppDetails", "No app can handle android.intent.action.MANAGE_APP_PERMISSIONS");
        }
    }

    public void startAppInfoFragment(Class<?> fragment, CharSequence title) {
        startAppInfoFragment(fragment, title, this, this.mAppEntry);
    }

    public static void startAppInfoFragment(Class<?> fragment, CharSequence title, SettingsPreferenceFragment caller, ApplicationsState.AppEntry appEntry) {
        Bundle args = new Bundle();
        args.putString("package", appEntry.info.packageName);
        args.putInt("uid", appEntry.info.uid);
        args.putBoolean("hideInfoButton", true);
        SettingsActivity sa = (SettingsActivity) caller.getActivity();
        sa.startPreferencePanel(fragment.getName(), args, -1, title, caller, 1);
    }

    @Override
    public void onClick(View v) {
        boolean zHasBaseUserRestriction;
        if (this.mAppEntry == null) {
            setIntentAndFinish(true, true);
            return;
        }
        String packageName = this.mAppEntry.info.packageName;
        if (v == this.mUninstallButton) {
            if (this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName)) {
                Activity activity = getActivity();
                Intent uninstallDAIntent = new Intent(activity, (Class<?>) DeviceAdminAdd.class);
                uninstallDAIntent.putExtra("android.app.extra.DEVICE_ADMIN_PACKAGE_NAME", this.mPackageName);
                activity.startActivityForResult(uninstallDAIntent, 1);
                return;
            }
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfUninstallBlocked(getActivity(), packageName, this.mUserId);
            if (this.mAppsControlDisallowedBySystem) {
                zHasBaseUserRestriction = true;
            } else {
                zHasBaseUserRestriction = RestrictedLockUtils.hasBaseUserRestriction(getActivity(), packageName, this.mUserId);
            }
            if (admin != null && !zHasBaseUserRestriction) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), admin);
                return;
            }
            if ((this.mAppEntry.info.flags & 1) != 0) {
                if (this.mAppEntry.info.enabled && !isDisabledUntilUsed()) {
                    if (this.mUpdatedSysApp && isSingleUser()) {
                        showDialogInner(3, 0);
                        return;
                    } else {
                        showDialogInner(2, 0);
                        return;
                    }
                }
                new DisableChanger(this, this.mAppEntry.info, 0).execute((Object) null);
                return;
            }
            if ((this.mAppEntry.info.flags & 8388608) == 0) {
                uninstallPkg(packageName, true, false);
                return;
            } else {
                uninstallPkg(packageName, false, false);
                return;
            }
        }
        if (v != this.mForceStopButton) {
            return;
        }
        if (this.mAppsControlDisallowedAdmin != null && !this.mAppsControlDisallowedBySystem) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), this.mAppsControlDisallowedAdmin);
        } else {
            showDialogInner(1, 0);
        }
    }

    private boolean isSingleUser() {
        int userCount = this.mUserManager.getUserCount();
        if (userCount == 1) {
            return true;
        }
        UserManager userManager = this.mUserManager;
        return UserManager.isSplitSystemUser() && userCount == 2;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mStoragePreference) {
            startAppInfoFragment(AppStorageSettings.class, this.mStoragePreference.getTitle());
        } else if (preference == this.mNotificationPreference) {
            startAppInfoFragment(AppNotificationSettings.class, getString(R.string.app_notifications_title));
        } else if (preference == this.mPermissionsPreference) {
            startManagePermissionsActivity();
        } else if (preference == this.mLaunchPreference) {
            startAppInfoFragment(AppLaunchSettings.class, this.mLaunchPreference.getTitle());
        } else if (preference == this.mMemoryPreference) {
            ProcessStatsBase.launchMemoryDetail((SettingsActivity) getActivity(), this.mStatsManager.getMemInfo(), this.mStats, false);
        } else if (preference == this.mDataPreference) {
            startAppInfoFragment(AppDataUsage.class, getString(R.string.app_data_usage));
        } else {
            if (preference != this.mBatteryPreference) {
                return false;
            }
            BatteryEntry entry = new BatteryEntry(getActivity(), null, this.mUserManager, this.mSipper);
            PowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(), this.mBatteryHelper, 0, entry, true, false);
        }
        return true;
    }

    private void addDynamicPrefs() {
        if (Utils.isManagedProfile(UserManager.get(getContext()))) {
            return;
        }
        PreferenceScreen screen = getPreferenceScreen();
        if (DefaultHomePreference.hasHomePreference(this.mPackageName, getContext())) {
            screen.addPreference(new ShortcutPreference(getPrefContext(), AdvancedAppSettings.class, "default_home", R.string.home_app, R.string.configure_apps));
        }
        if (DefaultBrowserPreference.hasBrowserPreference(this.mPackageName, getContext())) {
            screen.addPreference(new ShortcutPreference(getPrefContext(), AdvancedAppSettings.class, "default_browser", R.string.default_browser_title, R.string.configure_apps));
        }
        if (DefaultPhonePreference.hasPhonePreference(this.mPackageName, getContext())) {
            screen.addPreference(new ShortcutPreference(getPrefContext(), AdvancedAppSettings.class, "default_phone_app", R.string.default_phone_title, R.string.configure_apps));
        }
        if (DefaultEmergencyPreference.hasEmergencyPreference(this.mPackageName, getContext())) {
            screen.addPreference(new ShortcutPreference(getPrefContext(), AdvancedAppSettings.class, "default_emergency_app", R.string.default_emergency_app, R.string.configure_apps));
        }
        if (DefaultSmsPreference.hasSmsPreference(this.mPackageName, getContext())) {
            screen.addPreference(new ShortcutPreference(getPrefContext(), AdvancedAppSettings.class, "default_sms_app", R.string.sms_application_title, R.string.configure_apps));
        }
        boolean hasDrawOverOtherApps = hasPermission("android.permission.SYSTEM_ALERT_WINDOW");
        boolean hasWriteSettings = hasPermission("android.permission.WRITE_SETTINGS");
        if (hasDrawOverOtherApps || hasWriteSettings) {
            PreferenceCategory category = new PreferenceCategory(getPrefContext());
            category.setTitle(R.string.advanced_apps);
            screen.addPreference(category);
            if (hasDrawOverOtherApps) {
                Preference pref = new Preference(getPrefContext());
                pref.setTitle(R.string.draw_overlay);
                pref.setKey("system_alert_window");
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        InstalledAppDetails.this.startAppInfoFragment(DrawOverlayDetails.class, InstalledAppDetails.this.getString(R.string.draw_overlay));
                        return true;
                    }
                });
                category.addPreference(pref);
            }
            if (hasWriteSettings) {
                Preference pref2 = new Preference(getPrefContext());
                pref2.setTitle(R.string.write_settings);
                pref2.setKey("write_settings_apps");
                pref2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        InstalledAppDetails.this.startAppInfoFragment(WriteSettingsDetails.class, InstalledAppDetails.this.getString(R.string.write_settings));
                        return true;
                    }
                });
                category.addPreference(pref2);
            }
        }
        addAppInstallerInfoPref(screen);
    }

    private void addAppInstallerInfoPref(PreferenceScreen screen) {
        CharSequence installerLabel;
        String installerPackageName = null;
        try {
            installerPackageName = getContext().getPackageManager().getInstallerPackageName(this.mPackageName);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Exception while retrieving the package installer of " + this.mPackageName, e);
        }
        if (installerPackageName == null || (installerLabel = Utils.getApplicationLabel(getContext(), installerPackageName)) == null) {
            return;
        }
        PreferenceCategory category = new PreferenceCategory(getPrefContext());
        category.setTitle(R.string.app_install_details_group_title);
        screen.addPreference(category);
        Preference pref = new Preference(getPrefContext());
        pref.setTitle(R.string.app_install_details_title);
        pref.setKey("app_info_store");
        pref.setSummary(getString(R.string.app_install_details_summary, new Object[]{installerLabel}));
        Intent intent = new Intent("android.intent.action.SHOW_APP_INFO").setPackage(installerPackageName);
        Intent result = resolveIntent(intent);
        if (result != null) {
            result.putExtra("android.intent.extra.PACKAGE_NAME", this.mPackageName);
            pref.setIntent(result);
        } else {
            pref.setEnabled(false);
        }
        category.addPreference(pref);
    }

    private boolean hasPermission(String permission) {
        if (this.mPackageInfo == null || this.mPackageInfo.requestedPermissions == null) {
            return false;
        }
        for (int i = 0; i < this.mPackageInfo.requestedPermissions.length; i++) {
            if (this.mPackageInfo.requestedPermissions[i].equals(permission)) {
                return true;
            }
        }
        return false;
    }

    private void updateDynamicPrefs() {
        int i = R.string.yes;
        Preference pref = findPreference("default_home");
        if (pref != null) {
            pref.setSummary(DefaultHomePreference.isHomeDefault(this.mPackageName, getContext()) ? R.string.yes : R.string.no);
        }
        Preference pref2 = findPreference("default_browser");
        if (pref2 != null) {
            pref2.setSummary(DefaultBrowserPreference.isBrowserDefault(this.mPackageName, getContext()) ? R.string.yes : R.string.no);
        }
        Preference pref3 = findPreference("default_phone_app");
        if (pref3 != null) {
            pref3.setSummary(DefaultPhonePreference.isPhoneDefault(this.mPackageName, getContext()) ? R.string.yes : R.string.no);
        }
        Preference pref4 = findPreference("default_emergency_app");
        if (pref4 != null) {
            pref4.setSummary(DefaultEmergencyPreference.isEmergencyDefault(this.mPackageName, getContext()) ? R.string.yes : R.string.no);
        }
        Preference pref5 = findPreference("default_sms_app");
        if (pref5 != null) {
            if (!DefaultSmsPreference.isSmsDefault(this.mPackageName, getContext())) {
                i = R.string.no;
            }
            pref5.setSummary(i);
        }
        Preference pref6 = findPreference("system_alert_window");
        if (pref6 != null) {
            pref6.setSummary(DrawOverlayDetails.getSummary(getContext(), this.mAppEntry));
        }
        Preference pref7 = findPreference("write_settings_apps");
        if (pref7 == null) {
            return;
        }
        pref7.setSummary(WriteSettingsDetails.getSummary(getContext(), this.mAppEntry));
    }

    public static void setupAppSnippet(View appSnippet, CharSequence label, Drawable icon, CharSequence versionName) {
        LayoutInflater.from(appSnippet.getContext()).inflate(R.layout.widget_text_views, (ViewGroup) appSnippet.findViewById(android.R.id.widget_frame));
        ImageView iconView = (ImageView) appSnippet.findViewById(android.R.id.icon);
        iconView.setImageDrawable(icon);
        TextView labelView = (TextView) appSnippet.findViewById(android.R.id.title);
        labelView.setText(label);
        TextView appVersion = (TextView) appSnippet.findViewById(R.id.widget_text1);
        if (!TextUtils.isEmpty(versionName)) {
            appVersion.setSelected(true);
            appVersion.setVisibility(0);
            appVersion.setText(appSnippet.getContext().getString(R.string.version_text, String.valueOf(versionName)));
            return;
        }
        appVersion.setVisibility(4);
    }

    public static NetworkTemplate getTemplate(Context context) {
        if (DataUsageList.hasReadyMobileRadio(context)) {
            return NetworkTemplate.buildTemplateMobileWildcard();
        }
        if (DataUsageSummary.hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        }
        return NetworkTemplate.buildTemplateEthernet();
    }

    public static CharSequence getNotificationSummary(ApplicationsState.AppEntry appEntry, Context context, NotificationBackend backend) {
        NotificationBackend.AppRow appRow = backend.loadAppRow(context, context.getPackageManager(), appEntry.info);
        return getNotificationSummary(appRow, context);
    }

    public static CharSequence getNotificationSummary(NotificationBackend.AppRow appRow, Context context) {
        boolean showSlider = Settings.Secure.getInt(context.getContentResolver(), "show_importance_slider", 0) == 1;
        List<String> summaryAttributes = new ArrayList<>();
        StringBuffer summary = new StringBuffer();
        if (showSlider) {
            if (appRow.appImportance != -1000) {
                summaryAttributes.add(context.getString(R.string.notification_summary_level, Integer.valueOf(appRow.appImportance)));
            }
        } else if (appRow.banned) {
            summaryAttributes.add(context.getString(R.string.notifications_disabled));
        } else if (appRow.appImportance > 0 && appRow.appImportance < 3) {
            summaryAttributes.add(context.getString(R.string.notifications_silenced));
        }
        boolean lockscreenSecure = new LockPatternUtils(context).isSecure(UserHandle.myUserId());
        if (lockscreenSecure) {
            if (appRow.appVisOverride == 0) {
                summaryAttributes.add(context.getString(R.string.notifications_redacted));
            } else if (appRow.appVisOverride == -1) {
                summaryAttributes.add(context.getString(R.string.notifications_hidden));
            }
        }
        if (appRow.appBypassDnd) {
            summaryAttributes.add(context.getString(R.string.notifications_priority));
        }
        int N = summaryAttributes.size();
        for (int i = 0; i < N; i++) {
            if (i > 0) {
                summary.append(context.getString(R.string.notifications_summary_divider));
            }
            summary.append(summaryAttributes.get(i));
        }
        return summary.toString();
    }

    private class MemoryUpdater extends AsyncTask<Void, Void, ProcStatsPackageEntry> {
        MemoryUpdater(InstalledAppDetails this$0, MemoryUpdater memoryUpdater) {
            this();
        }

        private MemoryUpdater() {
        }

        @Override
        public ProcStatsPackageEntry doInBackground(Void... params) {
            if (InstalledAppDetails.this.getActivity() == null || InstalledAppDetails.this.mPackageInfo == null) {
                return null;
            }
            if (InstalledAppDetails.this.mStatsManager == null) {
                InstalledAppDetails.this.mStatsManager = new ProcStatsData(InstalledAppDetails.this.getActivity(), false);
                InstalledAppDetails.this.mStatsManager.setDuration(ProcessStatsBase.sDurations[0]);
            }
            InstalledAppDetails.this.mStatsManager.refreshStats(true);
            for (ProcStatsPackageEntry pkgEntry : InstalledAppDetails.this.mStatsManager.getEntries()) {
                for (ProcStatsEntry entry : pkgEntry.mEntries) {
                    if (entry.mUid == InstalledAppDetails.this.mPackageInfo.applicationInfo.uid) {
                        pkgEntry.updateMetrics();
                        return pkgEntry;
                    }
                }
            }
            return null;
        }

        @Override
        public void onPostExecute(ProcStatsPackageEntry entry) {
            if (InstalledAppDetails.this.getActivity() == null) {
                return;
            }
            if (entry != null) {
                InstalledAppDetails.this.mStats = entry;
                InstalledAppDetails.this.mMemoryPreference.setEnabled(true);
                double amount = Math.max(entry.mRunWeight, entry.mBgWeight) * InstalledAppDetails.this.mStatsManager.getMemInfo().weightToRam;
                InstalledAppDetails.this.mMemoryPreference.setSummary(InstalledAppDetails.this.getString(R.string.memory_use_summary, new Object[]{Formatter.formatShortFileSize(InstalledAppDetails.this.getContext(), (long) amount)}));
                return;
            }
            InstalledAppDetails.this.mMemoryPreference.setEnabled(false);
            InstalledAppDetails.this.mMemoryPreference.setSummary(InstalledAppDetails.this.getString(R.string.no_memory_use_summary));
        }
    }

    private class BatteryUpdater extends AsyncTask<Void, Void, Void> {
        BatteryUpdater(InstalledAppDetails this$0, BatteryUpdater batteryUpdater) {
            this();
        }

        private BatteryUpdater() {
        }

        @Override
        public Void doInBackground(Void... params) {
            InstalledAppDetails.this.mBatteryHelper.create((Bundle) null);
            InstalledAppDetails.this.mBatteryHelper.refreshStats(0, InstalledAppDetails.this.mUserManager.getUserProfiles());
            List<BatterySipper> usageList = InstalledAppDetails.this.mBatteryHelper.getUsageList();
            int N = usageList.size();
            int i = 0;
            while (true) {
                if (i >= N) {
                    break;
                }
                BatterySipper sipper = usageList.get(i);
                if (sipper.getUid() != InstalledAppDetails.this.mPackageInfo.applicationInfo.uid) {
                    i++;
                } else {
                    InstalledAppDetails.this.mSipper = sipper;
                    break;
                }
            }
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            if (InstalledAppDetails.this.getActivity() == null) {
                return;
            }
            InstalledAppDetails.this.refreshUi();
        }
    }

    private static class DisableChanger extends AsyncTask<Object, Object, Object> {
        final WeakReference<InstalledAppDetails> mActivity;
        final ApplicationInfo mInfo;
        final PackageManager mPm;
        final int mState;

        DisableChanger(InstalledAppDetails activity, ApplicationInfo info, int state) {
            this.mPm = activity.mPm;
            this.mActivity = new WeakReference<>(activity);
            this.mInfo = info;
            this.mState = state;
        }

        @Override
        protected Object doInBackground(Object... params) {
            this.mPm.setApplicationEnabledSetting(this.mInfo.packageName, this.mState, 0);
            return null;
        }
    }
}
