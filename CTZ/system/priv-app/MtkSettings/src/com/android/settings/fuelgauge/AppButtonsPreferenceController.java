package com.android.settings.fuelgauge;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.webkit.IWebViewUpdateService;
import com.android.settings.DeviceAdminAdd;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.fuelgauge.ButtonActionDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ActionButtonPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.ArrayList;
import java.util.HashSet;

/* loaded from: classes.dex */
public class AppButtonsPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, ApplicationsState.Callbacks, LifecycleObserver, OnDestroy, OnResume {
    private final SettingsActivity mActivity;
    ApplicationsState.AppEntry mAppEntry;
    private final ApplicationFeatureProvider mApplicationFeatureProvider;
    private RestrictedLockUtils.EnforcedAdmin mAppsControlDisallowedAdmin;
    private boolean mAppsControlDisallowedBySystem;
    ActionButtonPreference mButtonsPref;
    private final BroadcastReceiver mCheckKillProcessesReceiver;
    boolean mDisableAfterUninstall;
    private final DevicePolicyManager mDpm;
    private boolean mFinishing;
    private final Fragment mFragment;
    final HashSet<String> mHomePackages;
    private boolean mListeningToPackageRemove;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    PackageInfo mPackageInfo;
    String mPackageName;
    private final BroadcastReceiver mPackageRemovedReceiver;
    private final PackageManager mPm;
    private final int mRequestRemoveDeviceAdmin;
    private final int mRequestUninstall;
    private ApplicationsState.Session mSession;
    ApplicationsState mState;
    private boolean mUpdatedSysApp;
    private final int mUserId;
    private final UserManager mUserManager;

    public AppButtonsPreferenceController(SettingsActivity settingsActivity, Fragment fragment, Lifecycle lifecycle, String str, ApplicationsState applicationsState, DevicePolicyManager devicePolicyManager, UserManager userManager, PackageManager packageManager, int i, int i2) {
        super(settingsActivity);
        this.mHomePackages = new HashSet<>();
        this.mDisableAfterUninstall = false;
        this.mUpdatedSysApp = false;
        this.mListeningToPackageRemove = false;
        this.mFinishing = false;
        this.mCheckKillProcessesReceiver = new BroadcastReceiver() { // from class: com.android.settings.fuelgauge.AppButtonsPreferenceController.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                boolean z = getResultCode() != 0;
                Log.d("AppButtonsPrefCtl", "Got broadcast response: Restart status for " + AppButtonsPreferenceController.this.mAppEntry.info.packageName + " " + z);
                AppButtonsPreferenceController.this.updateForceStopButtonInner(z);
            }
        };
        this.mPackageRemovedReceiver = new BroadcastReceiver() { // from class: com.android.settings.fuelgauge.AppButtonsPreferenceController.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
                if (!AppButtonsPreferenceController.this.mFinishing && AppButtonsPreferenceController.this.mAppEntry.info.packageName.equals(schemeSpecificPart)) {
                    AppButtonsPreferenceController.this.mActivity.finishAndRemoveTask();
                }
            }
        };
        if (!(fragment instanceof ButtonActionDialogFragment.AppButtonsDialogListener)) {
            throw new IllegalArgumentException("Fragment should implement AppButtonsDialogListener");
        }
        FeatureFactory factory = FeatureFactory.getFactory(settingsActivity);
        this.mMetricsFeatureProvider = factory.getMetricsFeatureProvider();
        this.mApplicationFeatureProvider = factory.getApplicationFeatureProvider(settingsActivity);
        this.mState = applicationsState;
        this.mDpm = devicePolicyManager;
        this.mUserManager = userManager;
        this.mPm = packageManager;
        this.mPackageName = str;
        this.mActivity = settingsActivity;
        this.mFragment = fragment;
        this.mUserId = UserHandle.myUserId();
        this.mRequestUninstall = i;
        this.mRequestRemoveDeviceAdmin = i2;
        if (str != null) {
            this.mAppEntry = this.mState.getEntry(str, this.mUserId);
            this.mSession = this.mState.newSession(this, lifecycle);
            lifecycle.addObserver(this);
            return;
        }
        this.mFinishing = true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return (this.mAppEntry == null || AppUtils.isInstant(this.mAppEntry.info)) ? false : true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mButtonsPref = ((ActionButtonPreference) preferenceScreen.findPreference("action_buttons")).setButton1Text(R.string.uninstall_text).setButton2Text(R.string.force_stop).setButton1OnClickListener(new UninstallAndDisableButtonListener()).setButton2OnClickListener(new ForceStopButtonListener()).setButton1Positive(false).setButton2Positive(false).setButton2Enabled(false);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "action_buttons";
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (isAvailable() && !this.mFinishing) {
            this.mAppsControlDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(this.mActivity, "no_control_apps", this.mUserId);
            this.mAppsControlDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(this.mActivity, "no_control_apps", this.mUserId);
            if (!refreshUi()) {
                setIntentAndFinish(true);
            }
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnDestroy
    public void onDestroy() {
        stopListeningToPackageRemove();
    }

    private class UninstallAndDisableButtonListener implements View.OnClickListener {
        private UninstallAndDisableButtonListener() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            int i;
            String str = AppButtonsPreferenceController.this.mAppEntry.info.packageName;
            if (AppButtonsPreferenceController.this.mDpm.packageHasActiveAdmins(AppButtonsPreferenceController.this.mPackageInfo.packageName)) {
                AppButtonsPreferenceController.this.stopListeningToPackageRemove();
                Intent intent = new Intent(AppButtonsPreferenceController.this.mActivity, (Class<?>) DeviceAdminAdd.class);
                intent.putExtra("android.app.extra.DEVICE_ADMIN_PACKAGE_NAME", str);
                AppButtonsPreferenceController.this.mMetricsFeatureProvider.action(AppButtonsPreferenceController.this.mActivity, 873, new Pair[0]);
                AppButtonsPreferenceController.this.mFragment.startActivityForResult(intent, AppButtonsPreferenceController.this.mRequestRemoveDeviceAdmin);
                return;
            }
            RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfUninstallBlocked = RestrictedLockUtils.checkIfUninstallBlocked(AppButtonsPreferenceController.this.mActivity, str, AppButtonsPreferenceController.this.mUserId);
            boolean z = AppButtonsPreferenceController.this.mAppsControlDisallowedBySystem || RestrictedLockUtils.hasBaseUserRestriction(AppButtonsPreferenceController.this.mActivity, str, AppButtonsPreferenceController.this.mUserId);
            if (enforcedAdminCheckIfUninstallBlocked != null && !z) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(AppButtonsPreferenceController.this.mActivity, enforcedAdminCheckIfUninstallBlocked);
                return;
            }
            if ((AppButtonsPreferenceController.this.mAppEntry.info.flags & 1) != 0) {
                if (!AppButtonsPreferenceController.this.mAppEntry.info.enabled || AppButtonsPreferenceController.this.isDisabledUntilUsed()) {
                    MetricsFeatureProvider metricsFeatureProvider = AppButtonsPreferenceController.this.mMetricsFeatureProvider;
                    SettingsActivity settingsActivity = AppButtonsPreferenceController.this.mActivity;
                    if (AppButtonsPreferenceController.this.mAppEntry.info.enabled) {
                        i = 874;
                    } else {
                        i = 875;
                    }
                    metricsFeatureProvider.action(settingsActivity, i, new Pair[0]);
                    AsyncTask.execute(AppButtonsPreferenceController.this.new DisableChangerRunnable(AppButtonsPreferenceController.this.mPm, AppButtonsPreferenceController.this.mAppEntry.info.packageName, 0));
                    return;
                }
                if (!AppButtonsPreferenceController.this.mUpdatedSysApp || !AppButtonsPreferenceController.this.isSingleUser()) {
                    AppButtonsPreferenceController.this.showDialogInner(0);
                    return;
                } else {
                    AppButtonsPreferenceController.this.showDialogInner(1);
                    return;
                }
            }
            if ((AppButtonsPreferenceController.this.mAppEntry.info.flags & 8388608) == 0) {
                AppButtonsPreferenceController.this.uninstallPkg(str, true, false);
            } else {
                AppButtonsPreferenceController.this.uninstallPkg(str, false, false);
            }
        }
    }

    private class ForceStopButtonListener implements View.OnClickListener {
        private ForceStopButtonListener() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            if (AppButtonsPreferenceController.this.mAppsControlDisallowedAdmin == null || AppButtonsPreferenceController.this.mAppsControlDisallowedBySystem) {
                AppButtonsPreferenceController.this.showDialogInner(2);
            } else {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(AppButtonsPreferenceController.this.mActivity, AppButtonsPreferenceController.this.mAppsControlDisallowedAdmin);
            }
        }
    }

    public void handleActivityResult(int i, int i2, Intent intent) {
        if (i == this.mRequestUninstall) {
            if (this.mDisableAfterUninstall) {
                this.mDisableAfterUninstall = false;
                AsyncTask.execute(new DisableChangerRunnable(this.mPm, this.mAppEntry.info.packageName, 3));
            }
            refreshAndFinishIfPossible();
            return;
        }
        if (i == this.mRequestRemoveDeviceAdmin) {
            refreshAndFinishIfPossible();
        }
    }

    public void handleDialogClick(int i) {
        switch (i) {
            case 0:
                this.mMetricsFeatureProvider.action(this.mActivity, 874, new Pair[0]);
                AsyncTask.execute(new DisableChangerRunnable(this.mPm, this.mAppEntry.info.packageName, 3));
                break;
            case 1:
                this.mMetricsFeatureProvider.action(this.mActivity, 874, new Pair[0]);
                uninstallPkg(this.mAppEntry.info.packageName, false, true);
                break;
            case 2:
                forceStopPackage(this.mAppEntry.info.packageName);
                break;
        }
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onRunningStateChanged(boolean z) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageListChanged() {
        if (isAvailable()) {
            refreshUi();
        }
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> arrayList) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageIconChanged() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageSizeChanged(String str) {
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

    void retrieveAppEntry() {
        this.mAppEntry = this.mState.getEntry(this.mPackageName, this.mUserId);
        if (this.mAppEntry != null) {
            try {
                this.mPackageInfo = this.mPm.getPackageInfo(this.mAppEntry.info.packageName, 4198976);
                this.mPackageName = this.mAppEntry.info.packageName;
                return;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("AppButtonsPrefCtl", "Exception when retrieving package:" + this.mAppEntry.info.packageName, e);
                this.mPackageInfo = null;
                return;
            }
        }
        this.mPackageInfo = null;
    }

    void updateUninstallButton() {
        boolean zHandleDisableable;
        boolean z;
        boolean z2 = (this.mAppEntry.info.flags & 1) != 0;
        if (z2) {
            zHandleDisableable = handleDisableable();
        } else {
            zHandleDisableable = (this.mPackageInfo.applicationInfo.flags & 8388608) != 0 || this.mUserManager.getUsers().size() < 2;
        }
        if (z2 && this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName)) {
            zHandleDisableable = false;
        }
        if (Utils.isProfileOrDeviceOwner(this.mUserManager, this.mDpm, this.mPackageInfo.packageName)) {
            zHandleDisableable = false;
        }
        if (Utils.isDeviceProvisioningPackage(this.mContext.getResources(), this.mAppEntry.info.packageName)) {
            zHandleDisableable = false;
        }
        if (this.mDpm.isUninstallInQueue(this.mPackageName)) {
            zHandleDisableable = false;
        }
        if (!zHandleDisableable || !this.mHomePackages.contains(this.mPackageInfo.packageName)) {
            z = zHandleDisableable;
        } else if (!z2) {
            ComponentName homeActivities = this.mPm.getHomeActivities(new ArrayList());
            if (homeActivities == null) {
                z = this.mHomePackages.size() > 1;
            } else {
                z = !this.mPackageInfo.packageName.equals(homeActivities.getPackageName());
            }
        } else {
            z = false;
        }
        if (this.mAppsControlDisallowedBySystem) {
            z = false;
        }
        if (isFallbackPackage(this.mAppEntry.info.packageName)) {
            z = false;
        }
        this.mButtonsPref.setButton1Enabled(z);
    }

    private void setIntentAndFinish(boolean z) {
        Intent intent = new Intent();
        intent.putExtra("chg", z);
        this.mActivity.finishPreferencePanel(-1, intent);
        this.mFinishing = true;
    }

    private void refreshAndFinishIfPossible() {
        if (!refreshUi()) {
            setIntentAndFinish(true);
        } else {
            startListeningToPackageRemove();
        }
    }

    boolean isFallbackPackage(String str) {
        try {
            if (IWebViewUpdateService.Stub.asInterface(ServiceManager.getService("webviewupdate")).isFallbackPackage(str)) {
                return true;
            }
            return false;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    void updateForceStopButton() {
        if (this.mDpm.packageHasActiveAdmins(this.mPackageInfo.packageName)) {
            Log.w("AppButtonsPrefCtl", "User can't force stop device admin");
            updateForceStopButtonInner(false);
            return;
        }
        if ((this.mAppEntry.info.flags & 2097152) == 0) {
            Log.w("AppButtonsPrefCtl", "App is not explicitly stopped");
            updateForceStopButtonInner(true);
            return;
        }
        Intent intent = new Intent("android.intent.action.QUERY_PACKAGE_RESTART", Uri.fromParts("package", this.mAppEntry.info.packageName, null));
        intent.putExtra("android.intent.extra.PACKAGES", new String[]{this.mAppEntry.info.packageName});
        intent.putExtra("android.intent.extra.UID", this.mAppEntry.info.uid);
        intent.putExtra("android.intent.extra.user_handle", UserHandle.getUserId(this.mAppEntry.info.uid));
        Log.d("AppButtonsPrefCtl", "Sending broadcast to query restart status for " + this.mAppEntry.info.packageName);
        this.mActivity.sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null, this.mCheckKillProcessesReceiver, null, 0, null, null);
    }

    void updateForceStopButtonInner(boolean z) {
        if (this.mAppsControlDisallowedBySystem) {
            this.mButtonsPref.setButton2Enabled(false);
        } else {
            this.mButtonsPref.setButton2Enabled(z);
        }
    }

    void uninstallPkg(String str, boolean z, boolean z2) {
        stopListeningToPackageRemove();
        Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE", Uri.parse("package:" + str));
        intent.putExtra("android.intent.extra.UNINSTALL_ALL_USERS", z);
        this.mMetricsFeatureProvider.action(this.mActivity, 872, new Pair[0]);
        this.mFragment.startActivityForResult(intent, this.mRequestUninstall);
        this.mDisableAfterUninstall = z2;
    }

    void forceStopPackage(String str) {
        FeatureFactory.getFactory(this.mContext).getMetricsFeatureProvider().action(this.mContext, 807, str, new Pair[0]);
        ActivityManager activityManager = (ActivityManager) this.mActivity.getSystemService("activity");
        Log.d("AppButtonsPrefCtl", "Stopping package " + str);
        activityManager.forceStopPackage(str);
        int userId = UserHandle.getUserId(this.mAppEntry.info.uid);
        this.mState.invalidatePackage(str, userId);
        ApplicationsState.AppEntry entry = this.mState.getEntry(str, userId);
        if (entry != null) {
            this.mAppEntry = entry;
        }
        updateForceStopButton();
    }

    boolean handleDisableable() {
        if (this.mHomePackages.contains(this.mAppEntry.info.packageName) || isSystemPackage(this.mActivity.getResources(), this.mPm, this.mPackageInfo)) {
            this.mButtonsPref.setButton1Text(R.string.disable_text).setButton1Positive(false);
            return false;
        }
        if (this.mAppEntry.info.enabled && !isDisabledUntilUsed()) {
            this.mButtonsPref.setButton1Text(R.string.disable_text).setButton1Positive(false);
            return true ^ this.mApplicationFeatureProvider.getKeepEnabledPackages().contains(this.mAppEntry.info.packageName);
        }
        this.mButtonsPref.setButton1Text(R.string.enable_text).setButton1Positive(true);
        return true;
    }

    boolean isSystemPackage(Resources resources, PackageManager packageManager, PackageInfo packageInfo) {
        return Utils.isSystemPackage(resources, packageManager, packageInfo);
    }

    private boolean isDisabledUntilUsed() {
        return this.mAppEntry.info.enabledSetting == 4;
    }

    private void showDialogInner(int i) {
        ButtonActionDialogFragment buttonActionDialogFragmentNewInstance = ButtonActionDialogFragment.newInstance(i);
        buttonActionDialogFragmentNewInstance.setTargetFragment(this.mFragment, 0);
        buttonActionDialogFragmentNewInstance.show(this.mActivity.getFragmentManager(), "dialog " + i);
    }

    private boolean isSingleUser() {
        int userCount = this.mUserManager.getUserCount();
        if (userCount == 1) {
            return true;
        }
        UserManager userManager = this.mUserManager;
        return UserManager.isSplitSystemUser() && userCount == 2;
    }

    private boolean signaturesMatch(String str, String str2) {
        if (str != null && str2 != null) {
            try {
                if (this.mPm.checkSignatures(str, str2) >= 0) {
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    boolean refreshUi() {
        if (this.mPackageName == null) {
            return false;
        }
        retrieveAppEntry();
        if (this.mAppEntry == null || this.mPackageInfo == null) {
            return false;
        }
        ArrayList arrayList = new ArrayList();
        this.mPm.getHomeActivities(arrayList);
        this.mHomePackages.clear();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = (ResolveInfo) arrayList.get(i);
            String str = resolveInfo.activityInfo.packageName;
            this.mHomePackages.add(str);
            Bundle bundle = resolveInfo.activityInfo.metaData;
            if (bundle != null) {
                String string = bundle.getString("android.app.home.alternate");
                if (signaturesMatch(string, str)) {
                    this.mHomePackages.add(string);
                }
            }
        }
        updateUninstallButton();
        updateForceStopButton();
        return true;
    }

    private void startListeningToPackageRemove() {
        if (this.mListeningToPackageRemove) {
            return;
        }
        this.mListeningToPackageRemove = true;
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        this.mActivity.registerReceiver(this.mPackageRemovedReceiver, intentFilter);
    }

    private void stopListeningToPackageRemove() {
        if (!this.mListeningToPackageRemove) {
            return;
        }
        this.mListeningToPackageRemove = false;
        this.mActivity.unregisterReceiver(this.mPackageRemovedReceiver);
    }

    private class DisableChangerRunnable implements Runnable {
        final String mPackageName;
        final PackageManager mPm;
        final int mState;

        public DisableChangerRunnable(PackageManager packageManager, String str, int i) {
            this.mPm = packageManager;
            this.mPackageName = str;
            this.mState = i;
        }

        @Override // java.lang.Runnable
        public void run() {
            this.mPm.setApplicationEnabledSetting(this.mPackageName, this.mState, 0);
        }
    }
}
