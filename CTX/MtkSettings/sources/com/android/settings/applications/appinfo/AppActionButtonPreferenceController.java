package com.android.settings.applications.appinfo;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.webkit.IWebViewUpdateService;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ActionButtonPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;
import java.util.HashSet;

public class AppActionButtonPreferenceController extends BasePreferenceController implements AppInfoDashboardFragment.Callback {
    private static final String KEY_ACTION_BUTTONS = "action_buttons";
    private static final String TAG = "AppActionButtonControl";
    ActionButtonPreference mActionButtons;
    private final ApplicationFeatureProvider mApplicationFeatureProvider;
    private final BroadcastReceiver mCheckKillProcessesReceiver;
    private DevicePolicyManager mDpm;
    private final HashSet<String> mHomePackages;
    private final String mPackageName;
    private final AppInfoDashboardFragment mParent;
    private PackageManager mPm;
    private int mUserId;
    private UserManager mUserManager;

    public AppActionButtonPreferenceController(Context context, AppInfoDashboardFragment appInfoDashboardFragment, String str) {
        super(context, KEY_ACTION_BUTTONS);
        this.mHomePackages = new HashSet<>();
        this.mCheckKillProcessesReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                boolean z = getResultCode() != 0;
                Log.d(AppActionButtonPreferenceController.TAG, "Got broadcast response: Restart status for " + AppActionButtonPreferenceController.this.mParent.getAppEntry().info.packageName + " " + z);
                AppActionButtonPreferenceController.this.updateForceStopButton(z);
            }
        };
        this.mParent = appInfoDashboardFragment;
        this.mPackageName = str;
        this.mUserId = UserHandle.myUserId();
        this.mApplicationFeatureProvider = FeatureFactory.getFactory(context).getApplicationFeatureProvider(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AppUtils.isInstant(this.mParent.getPackageInfo().applicationInfo) ? 3 : 0;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mActionButtons = ((ActionButtonPreference) preferenceScreen.findPreference(KEY_ACTION_BUTTONS)).setButton2Text(R.string.force_stop).setButton2Positive(false).setButton2Enabled(false);
    }

    @Override
    public void refreshUi() {
        if (this.mPm == null) {
            this.mPm = this.mContext.getPackageManager();
        }
        if (this.mDpm == null) {
            this.mDpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        }
        if (this.mUserManager == null) {
            this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        }
        ApplicationsState.AppEntry appEntry = this.mParent.getAppEntry();
        PackageInfo packageInfo = this.mParent.getPackageInfo();
        ArrayList arrayList = new ArrayList();
        this.mPm.getHomeActivities(arrayList);
        this.mHomePackages.clear();
        for (int i = 0; i < arrayList.size(); i++) {
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
        checkForceStop(appEntry, packageInfo);
        initUninstallButtons(appEntry, packageInfo);
    }

    void initUninstallButtons(ApplicationsState.AppEntry appEntry, PackageInfo packageInfo) {
        boolean zInitUninstallButtonForUserApp;
        boolean z;
        boolean z2 = (appEntry.info.flags & 1) != 0;
        if (z2) {
            zInitUninstallButtonForUserApp = handleDisableable(appEntry, packageInfo);
        } else {
            zInitUninstallButtonForUserApp = initUninstallButtonForUserApp();
        }
        if (z2 && this.mDpm.packageHasActiveAdmins(packageInfo.packageName)) {
            zInitUninstallButtonForUserApp = false;
        }
        if (Utils.isProfileOrDeviceOwner(this.mUserManager, this.mDpm, packageInfo.packageName)) {
            zInitUninstallButtonForUserApp = false;
        }
        if (Utils.isDeviceProvisioningPackage(this.mContext.getResources(), appEntry.info.packageName)) {
            zInitUninstallButtonForUserApp = false;
        }
        if (this.mDpm.isUninstallInQueue(this.mPackageName)) {
            zInitUninstallButtonForUserApp = false;
        }
        if (!zInitUninstallButtonForUserApp || !this.mHomePackages.contains(packageInfo.packageName)) {
            z = zInitUninstallButtonForUserApp;
        } else if (!z2) {
            ComponentName homeActivities = this.mPm.getHomeActivities(new ArrayList());
            if (homeActivities == null) {
                z = this.mHomePackages.size() > 1;
            } else {
                z = !packageInfo.packageName.equals(homeActivities.getPackageName());
            }
        } else {
            z = false;
        }
        if (RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_control_apps", this.mUserId)) {
            z = false;
        }
        try {
            if (IWebViewUpdateService.Stub.asInterface(ServiceManager.getService("webviewupdate")).isFallbackPackage(appEntry.info.packageName)) {
                z = false;
            }
            this.mActionButtons.setButton1Enabled(z);
            if (z) {
                this.mActionButtons.setButton1OnClickListener(new View.OnClickListener() {
                    @Override
                    public final void onClick(View view) {
                        this.f$0.mParent.handleUninstallButtonClick();
                    }
                });
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    boolean initUninstallButtonForUserApp() {
        boolean z;
        PackageInfo packageInfo = this.mParent.getPackageInfo();
        if ((packageInfo.applicationInfo.flags & 8388608) != 0 || this.mUserManager.getUsers().size() < 2) {
            if (AppUtils.isInstant(packageInfo.applicationInfo)) {
                this.mActionButtons.setButton1Visible(false);
                z = false;
            } else {
                z = true;
            }
        } else {
            z = false;
        }
        this.mActionButtons.setButton1Text(R.string.uninstall_text).setButton1Positive(false);
        return z;
    }

    boolean handleDisableable(ApplicationsState.AppEntry appEntry, PackageInfo packageInfo) {
        if (this.mHomePackages.contains(appEntry.info.packageName) || Utils.isSystemPackage(this.mContext.getResources(), this.mPm, packageInfo)) {
            this.mActionButtons.setButton1Text(R.string.disable_text).setButton1Positive(false);
            return false;
        }
        if (appEntry.info.enabled && appEntry.info.enabledSetting != 4) {
            this.mActionButtons.setButton1Text(R.string.disable_text).setButton1Positive(false);
            return true ^ this.mApplicationFeatureProvider.getKeepEnabledPackages().contains(appEntry.info.packageName);
        }
        this.mActionButtons.setButton1Text(R.string.enable_text).setButton1Positive(true);
        return true;
    }

    private void updateForceStopButton(boolean z) {
        boolean zHasBaseUserRestriction = RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_control_apps", this.mUserId);
        ActionButtonPreference actionButtonPreference = this.mActionButtons;
        if (zHasBaseUserRestriction) {
            z = false;
        }
        actionButtonPreference.setButton2Enabled(z).setButton2OnClickListener(zHasBaseUserRestriction ? null : new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.mParent.handleForceStopButtonClick();
            }
        });
    }

    void checkForceStop(ApplicationsState.AppEntry appEntry, PackageInfo packageInfo) {
        if (this.mDpm.packageHasActiveAdmins(packageInfo.packageName)) {
            Log.w(TAG, "User can't force stop device admin");
            updateForceStopButton(false);
            return;
        }
        if (this.mPm.isPackageStateProtected(packageInfo.packageName, UserHandle.getUserId(appEntry.info.uid))) {
            Log.w(TAG, "User can't force stop protected packages");
            updateForceStopButton(false);
            return;
        }
        if (AppUtils.isInstant(packageInfo.applicationInfo)) {
            updateForceStopButton(false);
            this.mActionButtons.setButton2Visible(false);
            return;
        }
        if ((appEntry.info.flags & 2097152) == 0) {
            Log.w(TAG, "App is not explicitly stopped");
            updateForceStopButton(true);
            return;
        }
        Intent intent = new Intent("android.intent.action.QUERY_PACKAGE_RESTART", Uri.fromParts("package", appEntry.info.packageName, null));
        intent.putExtra("android.intent.extra.PACKAGES", new String[]{appEntry.info.packageName});
        intent.putExtra("android.intent.extra.UID", appEntry.info.uid);
        intent.putExtra("android.intent.extra.user_handle", UserHandle.getUserId(appEntry.info.uid));
        Log.d(TAG, "Sending broadcast to query restart status for " + appEntry.info.packageName);
        this.mContext.sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT, null, this.mCheckKillProcessesReceiver, null, 0, null, null);
    }

    private boolean signaturesMatch(String str, String str2) {
        if (str != null && str2 != null) {
            try {
                return this.mPm.checkSignatures(str, str2) >= 0;
            } catch (Exception e) {
            }
        }
        return false;
    }
}
