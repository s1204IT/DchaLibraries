package com.android.settings.applications.appinfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateInstallAppsBridge;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.applications.ApplicationsState;

public class ExternalSourcesDetails extends AppInfoWithHeader implements Preference.OnPreferenceChangeListener {
    private AppStateInstallAppsBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private AppStateInstallAppsBridge.InstallAppsState mInstallAppsState;
    private RestrictedSwitchPreference mSwitchPref;
    private UserManager mUserManager;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        this.mAppBridge = new AppStateInstallAppsBridge(activity, this.mState, null);
        this.mAppOpsManager = (AppOpsManager) activity.getSystemService("appops");
        this.mUserManager = UserManager.get(activity);
        addPreferencesFromResource(R.xml.external_sources_details);
        this.mSwitchPref = (RestrictedSwitchPreference) findPreference("external_sources_settings_switch");
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        if (!this.mPackageName.equals("com.android.cts.verifier")) {
            getPreferenceScreen().removePreference(this.mSwitchPref);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        if (preference != this.mSwitchPref) {
            return false;
        }
        if (this.mInstallAppsState != null && zBooleanValue != this.mInstallAppsState.canInstallApps()) {
            if (Settings.ManageAppExternalSourcesActivity.class.getName().equals(getIntent().getComponent().getClassName())) {
                setResult(zBooleanValue ? -1 : 0);
            }
            setCanInstallApps(zBooleanValue);
            refreshUi();
            return true;
        }
        return true;
    }

    public static CharSequence getPreferenceSummary(Context context, ApplicationsState.AppEntry appEntry) {
        int i;
        int userRestrictionSource = UserManager.get(context).getUserRestrictionSource("no_install_unknown_sources", UserHandle.getUserHandleForUid(appEntry.info.uid));
        if (userRestrictionSource != 4) {
            switch (userRestrictionSource) {
                case 1:
                    return context.getString(R.string.disabled);
                case 2:
                    break;
                default:
                    if (new AppStateInstallAppsBridge(context, null, null).createInstallAppsStateFor(appEntry.info.packageName, appEntry.info.uid).canInstallApps()) {
                        i = R.string.app_permission_summary_allowed;
                    } else {
                        i = R.string.app_permission_summary_not_allowed;
                    }
                    return context.getString(i);
            }
        }
        return context.getString(R.string.disabled_by_admin);
    }

    private void setCanInstallApps(boolean z) {
        this.mAppOpsManager.setMode(66, this.mPackageInfo.applicationInfo.uid, this.mPackageName, z ? 0 : 2);
    }

    @Override
    protected boolean refreshUi() {
        if (this.mPackageInfo == null || this.mPackageInfo.applicationInfo == null) {
            return false;
        }
        if (this.mUserManager.hasBaseUserRestriction("no_install_unknown_sources", UserHandle.of(UserHandle.myUserId()))) {
            this.mSwitchPref.setChecked(false);
            this.mSwitchPref.setSummary(R.string.disabled);
            this.mSwitchPref.setEnabled(false);
            return true;
        }
        this.mSwitchPref.checkRestrictionAndSetDisabled("no_install_unknown_sources");
        if (this.mSwitchPref.isDisabledByAdmin()) {
            return true;
        }
        this.mInstallAppsState = this.mAppBridge.createInstallAppsStateFor(this.mPackageName, this.mPackageInfo.applicationInfo.uid);
        if (!this.mInstallAppsState.isPotentialAppSource()) {
            this.mSwitchPref.setEnabled(false);
            return true;
        }
        this.mSwitchPref.setChecked(this.mInstallAppsState.canInstallApps());
        return true;
    }

    @Override
    protected AlertDialog createDialog(int i, int i2) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return 808;
    }
}
