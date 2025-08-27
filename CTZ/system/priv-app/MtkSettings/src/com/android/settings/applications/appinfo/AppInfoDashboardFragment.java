package com.android.settings.applications.appinfo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.DeviceAdminAdd;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class AppInfoDashboardFragment extends DashboardFragment implements ApplicationsState.Callbacks {
    static final int REQUEST_UNINSTALL = 0;
    static final int UNINSTALL_ALL_USERS_MENU = 1;
    static final int UNINSTALL_UPDATES = 2;
    private AppActionButtonPreferenceController mAppActionButtonPreferenceController;
    private ApplicationsState.AppEntry mAppEntry;
    private RestrictedLockUtils.EnforcedAdmin mAppsControlDisallowedAdmin;
    private boolean mAppsControlDisallowedBySystem;
    private boolean mDisableAfterUninstall;
    private DevicePolicyManager mDpm;
    private boolean mFinishing;
    private boolean mInitialized;
    private InstantAppButtonsPreferenceController mInstantAppButtonPreferenceController;
    private boolean mListeningToPackageRemove;
    private PackageInfo mPackageInfo;
    private String mPackageName;
    private PackageManager mPm;
    private ApplicationsState.Session mSession;
    private boolean mShowUninstalled;
    private ApplicationsState mState;
    private int mUserId;
    private UserManager mUserManager;
    private boolean mUpdatedSysApp = false;
    private List<Callback> mCallbacks = new ArrayList();
    final BroadcastReceiver mPackageRemovedReceiver = new BroadcastReceiver() { // from class: com.android.settings.applications.appinfo.AppInfoDashboardFragment.4
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
            if (!AppInfoDashboardFragment.this.mFinishing) {
                if (AppInfoDashboardFragment.this.mAppEntry == null || AppInfoDashboardFragment.this.mAppEntry.info == null || TextUtils.equals(AppInfoDashboardFragment.this.mAppEntry.info.packageName, schemeSpecificPart)) {
                    AppInfoDashboardFragment.this.onPackageRemoved();
                }
            }
        }
    };

    public interface Callback {
        void refreshUi();
    }

    private boolean isDisabledUntilUsed() {
        return this.mAppEntry.info.enabledSetting == 4;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        String packageName = getPackageName();
        ((TimeSpentInAppPreferenceController) use(TimeSpentInAppPreferenceController.class)).setPackageName(packageName);
        ((AppDataUsagePreferenceController) use(AppDataUsagePreferenceController.class)).setParentFragment(this);
        AppInstallerInfoPreferenceController appInstallerInfoPreferenceController = (AppInstallerInfoPreferenceController) use(AppInstallerInfoPreferenceController.class);
        appInstallerInfoPreferenceController.setPackageName(packageName);
        appInstallerInfoPreferenceController.setParentFragment(this);
        ((AppInstallerPreferenceCategoryController) use(AppInstallerPreferenceCategoryController.class)).setChildren(Arrays.asList(appInstallerInfoPreferenceController));
        ((AppNotificationPreferenceController) use(AppNotificationPreferenceController.class)).setParentFragment(this);
        ((AppOpenByDefaultPreferenceController) use(AppOpenByDefaultPreferenceController.class)).setParentFragment(this);
        ((AppPermissionPreferenceController) use(AppPermissionPreferenceController.class)).setParentFragment(this);
        ((AppPermissionPreferenceController) use(AppPermissionPreferenceController.class)).setPackageName(packageName);
        ((AppSettingPreferenceController) use(AppSettingPreferenceController.class)).setPackageName(packageName).setParentFragment(this);
        ((AppStoragePreferenceController) use(AppStoragePreferenceController.class)).setParentFragment(this);
        ((AppVersionPreferenceController) use(AppVersionPreferenceController.class)).setParentFragment(this);
        ((InstantAppDomainsPreferenceController) use(InstantAppDomainsPreferenceController.class)).setParentFragment(this);
        WriteSystemSettingsPreferenceController writeSystemSettingsPreferenceController = (WriteSystemSettingsPreferenceController) use(WriteSystemSettingsPreferenceController.class);
        writeSystemSettingsPreferenceController.setParentFragment(this);
        DrawOverlayDetailPreferenceController drawOverlayDetailPreferenceController = (DrawOverlayDetailPreferenceController) use(DrawOverlayDetailPreferenceController.class);
        drawOverlayDetailPreferenceController.setParentFragment(this);
        PictureInPictureDetailPreferenceController pictureInPictureDetailPreferenceController = (PictureInPictureDetailPreferenceController) use(PictureInPictureDetailPreferenceController.class);
        pictureInPictureDetailPreferenceController.setPackageName(packageName);
        pictureInPictureDetailPreferenceController.setParentFragment(this);
        ExternalSourceDetailPreferenceController externalSourceDetailPreferenceController = (ExternalSourceDetailPreferenceController) use(ExternalSourceDetailPreferenceController.class);
        externalSourceDetailPreferenceController.setPackageName(packageName);
        externalSourceDetailPreferenceController.setParentFragment(this);
        ((AdvancedAppInfoPreferenceCategoryController) use(AdvancedAppInfoPreferenceCategoryController.class)).setChildren(Arrays.asList(writeSystemSettingsPreferenceController, drawOverlayDetailPreferenceController, pictureInPictureDetailPreferenceController, externalSourceDetailPreferenceController));
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFinishing = false;
        Activity activity = getActivity();
        this.mDpm = (DevicePolicyManager) activity.getSystemService("device_policy");
        this.mUserManager = (UserManager) activity.getSystemService("user");
        this.mPm = activity.getPackageManager();
        if (!ensurePackageInfoAvailable(activity)) {
            return;
        }
        startListeningToPackageRemove();
        setHasOptionsMenu(true);
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        stopListeningToPackageRemove();
        super.onDestroy();
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 20;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        this.mAppsControlDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(activity, "no_control_apps", this.mUserId);
        this.mAppsControlDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(activity, "no_control_apps", this.mUserId);
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.app_info_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "AppInfoDashboard";
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        retrieveAppEntry();
        if (this.mPackageInfo == null) {
            return null;
        }
        String packageName = getPackageName();
        ArrayList arrayList = new ArrayList();
        Lifecycle lifecycle = getLifecycle();
        arrayList.add(new AppHeaderViewPreferenceController(context, this, packageName, lifecycle));
        this.mAppActionButtonPreferenceController = new AppActionButtonPreferenceController(context, this, packageName);
        arrayList.add(this.mAppActionButtonPreferenceController);
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            this.mCallbacks.add((Callback) ((AbstractPreferenceController) it.next()));
        }
        this.mInstantAppButtonPreferenceController = new InstantAppButtonsPreferenceController(context, this, packageName, lifecycle);
        arrayList.add(this.mInstantAppButtonPreferenceController);
        arrayList.add(new AppBatteryPreferenceController(context, this, packageName, lifecycle));
        arrayList.add(new AppMemoryPreferenceController(context, this, lifecycle));
        arrayList.add(new DefaultHomeShortcutPreferenceController(context, packageName));
        arrayList.add(new DefaultBrowserShortcutPreferenceController(context, packageName));
        arrayList.add(new DefaultPhoneShortcutPreferenceController(context, packageName));
        arrayList.add(new DefaultEmergencyShortcutPreferenceController(context, packageName));
        arrayList.add(new DefaultSmsShortcutPreferenceController(context, packageName));
        return arrayList;
    }

    void addToCallbackList(Callback callback) {
        if (callback != null) {
            this.mCallbacks.add(callback);
        }
    }

    ApplicationsState.AppEntry getAppEntry() {
        return this.mAppEntry;
    }

    PackageInfo getPackageInfo() {
        return this.mPackageInfo;
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageSizeChanged(String str) throws PackageManager.NameNotFoundException {
        if (!TextUtils.equals(str, this.mPackageName)) {
            Log.d("AppInfoDashboard", "Package change irrelevant, skipping");
        } else {
            refreshUi();
        }
    }

    boolean ensurePackageInfoAvailable(Activity activity) {
        if (this.mPackageInfo != null) {
            return true;
        }
        this.mFinishing = true;
        Log.w("AppInfoDashboard", "Package info not available. Is this package already uninstalled?");
        activity.finishAndRemoveTask();
        return false;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menu.add(0, 2, 0, R.string.app_factory_reset).setShowAsAction(0);
        menu.add(0, 1, 1, R.string.uninstall_all_users_text).setShowAsAction(0);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPrepareOptionsMenu(Menu menu) {
        if (this.mFinishing) {
            return;
        }
        super.onPrepareOptionsMenu(menu);
        menu.findItem(1).setVisible(shouldShowUninstallForAll(this.mAppEntry));
        this.mUpdatedSysApp = (this.mAppEntry.info.flags & 128) != 0;
        MenuItem menuItemFindItem = menu.findItem(2);
        menuItemFindItem.setVisible((!this.mUpdatedSysApp || this.mAppsControlDisallowedBySystem || getContext().getResources().getBoolean(R.bool.config_disable_uninstall_update)) ? false : true);
        if (menuItemFindItem.isVisible()) {
            RestrictedLockUtils.setMenuItemAsDisabledByAdmin(getActivity(), menuItemFindItem, this.mAppsControlDisallowedAdmin);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 1:
                uninstallPkg(this.mAppEntry.info.packageName, true, false);
                return true;
            case 2:
                uninstallPkg(this.mAppEntry.info.packageName, false, false);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        switch (i) {
            case 0:
                getActivity().invalidateOptionsMenu();
                if (this.mDisableAfterUninstall) {
                    this.mDisableAfterUninstall = false;
                    new DisableChanger(this, this.mAppEntry.info, 3).execute(null);
                }
                if (!refreshUi()) {
                    onPackageRemoved();
                    break;
                } else {
                    startListeningToPackageRemove();
                    break;
                }
            case 1:
                if (!refreshUi()) {
                    setIntentAndFinish(true, true);
                    break;
                } else {
                    startListeningToPackageRemove();
                    break;
                }
        }
    }

    boolean shouldShowUninstallForAll(ApplicationsState.AppEntry appEntry) {
        if (this.mUpdatedSysApp || appEntry == null || (appEntry.info.flags & 1) != 0 || this.mPackageInfo == null || this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName) || UserHandle.myUserId() != 0 || this.mUserManager.getUsers().size() < 2) {
            return false;
        }
        return (getNumberOfUserWithPackageInstalled(this.mPackageName) >= 2 || (appEntry.info.flags & 8388608) == 0) && !AppUtils.isInstant(appEntry.info);
    }

    boolean refreshUi() throws PackageManager.NameNotFoundException {
        retrieveAppEntry();
        if (this.mAppEntry == null || this.mPackageInfo == null) {
            return false;
        }
        this.mState.ensureIcon(this.mAppEntry);
        Iterator<Callback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().refreshUi();
        }
        if (!this.mInitialized) {
            this.mInitialized = true;
            this.mShowUninstalled = (this.mAppEntry.info.flags & 8388608) == 0;
        } else {
            try {
                ApplicationInfo applicationInfo = getActivity().getPackageManager().getApplicationInfo(this.mAppEntry.info.packageName, 4194816);
                if (!this.mShowUninstalled) {
                    return (applicationInfo.flags & 8388608) != 0;
                }
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    AlertDialog createDialog(int i, int i2) {
        switch (i) {
            case 1:
                return new AlertDialog.Builder(getActivity()).setTitle(getActivity().getText(R.string.force_stop_dlg_title)).setMessage(getActivity().getText(R.string.force_stop_dlg_text)).setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() { // from class: com.android.settings.applications.appinfo.AppInfoDashboardFragment.3
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i3) {
                        AppInfoDashboardFragment.this.forceStopPackage(AppInfoDashboardFragment.this.mAppEntry.info.packageName);
                    }
                }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            case 2:
                return new AlertDialog.Builder(getActivity()).setMessage(getActivity().getText(R.string.app_disable_dlg_text)).setPositiveButton(R.string.app_disable_dlg_positive, new DialogInterface.OnClickListener() { // from class: com.android.settings.applications.appinfo.AppInfoDashboardFragment.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i3) {
                        AppInfoDashboardFragment.this.mMetricsFeatureProvider.action(AppInfoDashboardFragment.this.getContext(), 874, new Pair[0]);
                        new DisableChanger(AppInfoDashboardFragment.this, AppInfoDashboardFragment.this.mAppEntry.info, 3).execute(null);
                    }
                }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            case 3:
                return new AlertDialog.Builder(getActivity()).setMessage(getActivity().getText(R.string.app_disable_dlg_text)).setPositiveButton(R.string.app_disable_dlg_positive, new DialogInterface.OnClickListener() { // from class: com.android.settings.applications.appinfo.AppInfoDashboardFragment.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i3) {
                        AppInfoDashboardFragment.this.mMetricsFeatureProvider.action(AppInfoDashboardFragment.this.getContext(), 874, new Pair[0]);
                        AppInfoDashboardFragment.this.uninstallPkg(AppInfoDashboardFragment.this.mAppEntry.info.packageName, false, true);
                    }
                }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).create();
            default:
                return this.mInstantAppButtonPreferenceController.createDialog(i);
        }
    }

    private void uninstallPkg(String str, boolean z, boolean z2) {
        stopListeningToPackageRemove();
        Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE", Uri.parse("package:" + str));
        intent.putExtra("android.intent.extra.UNINSTALL_ALL_USERS", z);
        this.mMetricsFeatureProvider.action(getContext(), 872, new Pair[0]);
        startActivityForResult(intent, 0);
        this.mDisableAfterUninstall = z2;
    }

    private void forceStopPackage(String str) {
        this.mMetricsFeatureProvider.action(getContext(), 807, str, new Pair[0]);
        ActivityManager activityManager = (ActivityManager) getActivity().getSystemService("activity");
        Log.d("AppInfoDashboard", "Stopping package " + str);
        activityManager.forceStopPackage(str);
        int userId = UserHandle.getUserId(this.mAppEntry.info.uid);
        this.mState.invalidatePackage(str, userId);
        ApplicationsState.AppEntry entry = this.mState.getEntry(str, userId);
        if (entry != null) {
            this.mAppEntry = entry;
        }
        this.mAppActionButtonPreferenceController.checkForceStop(this.mAppEntry, this.mPackageInfo);
    }

    public static void startAppInfoFragment(Class<?> cls, int i, Bundle bundle, SettingsPreferenceFragment settingsPreferenceFragment, ApplicationsState.AppEntry appEntry) {
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putString("package", appEntry.info.packageName);
        bundle.putInt("uid", appEntry.info.uid);
        new SubSettingLauncher(settingsPreferenceFragment.getContext()).setDestination(cls.getName()).setArguments(bundle).setTitle(i).setResultListener(settingsPreferenceFragment, 1).setSourceMetricsCategory(settingsPreferenceFragment.getMetricsCategory()).launch();
    }

    void handleUninstallButtonClick() {
        if (this.mAppEntry == null) {
            setIntentAndFinish(true, true);
            return;
        }
        String str = this.mAppEntry.info.packageName;
        if (this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName)) {
            stopListeningToPackageRemove();
            Activity activity = getActivity();
            Intent intent = new Intent(activity, (Class<?>) DeviceAdminAdd.class);
            intent.putExtra("android.app.extra.DEVICE_ADMIN_PACKAGE_NAME", this.mPackageName);
            this.mMetricsFeatureProvider.action(activity, 873, new Pair[0]);
            activity.startActivityForResult(intent, 1);
            return;
        }
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfUninstallBlocked = RestrictedLockUtils.checkIfUninstallBlocked(getActivity(), str, this.mUserId);
        boolean z = this.mAppsControlDisallowedBySystem || RestrictedLockUtils.hasBaseUserRestriction(getActivity(), str, this.mUserId);
        if (enforcedAdminCheckIfUninstallBlocked != null && !z) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), enforcedAdminCheckIfUninstallBlocked);
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
            this.mMetricsFeatureProvider.action(getActivity(), 875, new Pair[0]);
            new DisableChanger(this, this.mAppEntry.info, 1).execute(null);
            return;
        }
        if ((this.mAppEntry.info.flags & 8388608) == 0) {
            uninstallPkg(str, true, false);
        } else {
            uninstallPkg(str, false, false);
        }
    }

    void handleForceStopButtonClick() {
        if (this.mAppEntry == null) {
            setIntentAndFinish(true, true);
        } else if (this.mAppsControlDisallowedAdmin != null && !this.mAppsControlDisallowedBySystem) {
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

    private void onPackageRemoved() {
        getActivity().finishActivity(1);
        getActivity().finishAndRemoveTask();
    }

    int getNumberOfUserWithPackageInstalled(String str) {
        int i = 0;
        for (UserInfo userInfo : this.mUserManager.getUsers(true)) {
            try {
                if ((8388608 & this.mPm.getApplicationInfoAsUser(str, 128, userInfo.id).flags) != 0) {
                    i++;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("AppInfoDashboard", "Package: " + str + " not found for user: " + userInfo.id);
            }
        }
        return i;
    }

    private static class DisableChanger extends AsyncTask<Object, Object, Object> {
        final WeakReference<AppInfoDashboardFragment> mActivity;
        final ApplicationInfo mInfo;
        final PackageManager mPm;
        final int mState;

        DisableChanger(AppInfoDashboardFragment appInfoDashboardFragment, ApplicationInfo applicationInfo, int i) {
            this.mPm = appInfoDashboardFragment.mPm;
            this.mActivity = new WeakReference<>(appInfoDashboardFragment);
            this.mInfo = applicationInfo;
            this.mState = i;
        }

        @Override // android.os.AsyncTask
        protected Object doInBackground(Object... objArr) {
            this.mPm.setApplicationEnabledSetting(this.mInfo.packageName, this.mState, 0);
            return null;
        }
    }

    private String getPackageName() {
        if (this.mPackageName != null) {
            return this.mPackageName;
        }
        Bundle arguments = getArguments();
        this.mPackageName = arguments != null ? arguments.getString("package") : null;
        if (this.mPackageName == null) {
            Intent intent = arguments == null ? getActivity().getIntent() : (Intent) arguments.getParcelable("intent");
            if (intent != null) {
                this.mPackageName = intent.getData().getSchemeSpecificPart();
            }
        }
        return this.mPackageName;
    }

    void retrieveAppEntry() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (this.mState == null) {
            this.mState = ApplicationsState.getInstance(activity.getApplication());
            this.mSession = this.mState.newSession(this, getLifecycle());
        }
        this.mUserId = UserHandle.myUserId();
        this.mAppEntry = this.mState.getEntry(getPackageName(), UserHandle.myUserId());
        if (this.mAppEntry != null) {
            try {
                this.mPackageInfo = activity.getPackageManager().getPackageInfo(this.mAppEntry.info.packageName, 4198976);
                return;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("AppInfoDashboard", "Exception when retrieving package:" + this.mAppEntry.info.packageName, e);
                return;
            }
        }
        Log.w("AppInfoDashboard", "Missing AppEntry; maybe reinstalling?");
        this.mPackageInfo = null;
    }

    private void setIntentAndFinish(boolean z, boolean z2) {
        Intent intent = new Intent();
        intent.putExtra("chg", z2);
        ((SettingsActivity) getActivity()).finishPreferencePanel(-1, intent);
        this.mFinishing = true;
    }

    void showDialogInner(int i, int i2) {
        MyAlertDialogFragment myAlertDialogFragmentNewInstance = MyAlertDialogFragment.newInstance(i, i2);
        myAlertDialogFragmentNewInstance.setTargetFragment(this, 0);
        myAlertDialogFragmentNewInstance.show(getFragmentManager(), "dialog " + i);
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onRunningStateChanged(boolean z) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> arrayList) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageIconChanged() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onAllSizesComputed() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onLauncherInfoChanged() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onLoadEntriesCompleted() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageListChanged() {
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }

    public static class MyAlertDialogFragment extends InstrumentedDialogFragment {
        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 558;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            int i = getArguments().getInt("id");
            AlertDialog alertDialogCreateDialog = ((AppInfoDashboardFragment) getTargetFragment()).createDialog(i, getArguments().getInt("moveError"));
            if (alertDialogCreateDialog == null) {
                throw new IllegalArgumentException("unknown id " + i);
            }
            return alertDialogCreateDialog;
        }

        public static MyAlertDialogFragment newInstance(int i, int i2) {
            MyAlertDialogFragment myAlertDialogFragment = new MyAlertDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("id", i);
            bundle.putInt("moveError", i2);
            myAlertDialogFragment.setArguments(bundle);
            return myAlertDialogFragment;
        }
    }

    void startListeningToPackageRemove() {
        if (this.mListeningToPackageRemove) {
            return;
        }
        this.mListeningToPackageRemove = true;
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        getContext().registerReceiver(this.mPackageRemovedReceiver, intentFilter);
    }

    private void stopListeningToPackageRemove() {
        if (!this.mListeningToPackageRemove) {
            return;
        }
        this.mListeningToPackageRemove = false;
        getContext().unregisterReceiver(this.mPackageRemovedReceiver);
    }
}
