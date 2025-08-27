package com.android.settings.applications;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.applications.AppStateUsageBridge;
import com.android.settings.overlay.FeatureFactory;

/* loaded from: classes.dex */
public class UsageAccessDetails extends AppInfoWithHeader implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private AppOpsManager mAppOpsManager;
    private DevicePolicyManager mDpm;
    private Intent mSettingsIntent;
    private SwitchPreference mSwitchPref;
    private AppStateUsageBridge mUsageBridge;
    private Preference mUsageDesc;
    private AppStateUsageBridge.UsageState mUsageState;

    @Override // com.android.settings.applications.AppInfoBase, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        this.mUsageBridge = new AppStateUsageBridge(activity, this.mState, null);
        this.mAppOpsManager = (AppOpsManager) activity.getSystemService("appops");
        this.mDpm = (DevicePolicyManager) activity.getSystemService(DevicePolicyManager.class);
        addPreferencesFromResource(R.xml.app_ops_permissions_details);
        this.mSwitchPref = (SwitchPreference) findPreference("app_ops_settings_switch");
        this.mUsageDesc = findPreference("app_ops_settings_description");
        getPreferenceScreen().setTitle(R.string.usage_access);
        this.mSwitchPref.setTitle(R.string.permit_usage_access);
        this.mUsageDesc.setSummary(R.string.usage_access_description);
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        this.mSettingsIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.USAGE_ACCESS_CONFIG").setPackage(this.mPackageName);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mSwitchPref) {
            if (this.mUsageState != null && ((Boolean) obj).booleanValue() != this.mUsageState.isPermissible()) {
                if (this.mUsageState.isPermissible() && this.mDpm.isProfileOwnerApp(this.mPackageName)) {
                    new AlertDialog.Builder(getContext()).setIcon(android.R.drawable.focus_event_rotary_input_background).setTitle(android.R.string.dialog_alert_title).setMessage(R.string.work_profile_usage_access_warning).setPositiveButton(R.string.okay, (DialogInterface.OnClickListener) null).show();
                }
                setHasAccess(!this.mUsageState.isPermissible());
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setHasAccess(boolean z) {
        logSpecialPermissionChange(z, this.mPackageName);
        this.mAppOpsManager.setMode(43, this.mPackageInfo.applicationInfo.uid, this.mPackageName, !z ? 1 : 0);
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean z, String str) {
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(), z ? 783 : 784, str, new Pair[0]);
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected boolean refreshUi() {
        retrieveAppEntry();
        if (this.mAppEntry == null || this.mPackageInfo == null) {
            return false;
        }
        this.mUsageState = this.mUsageBridge.getUsageInfo(this.mPackageName, this.mPackageInfo.applicationInfo.uid);
        this.mSwitchPref.setChecked(this.mUsageState.isPermissible());
        this.mSwitchPref.setEnabled(this.mUsageState.permissionDeclared);
        ResolveInfo resolveInfoResolveActivityAsUser = this.mPm.resolveActivityAsUser(this.mSettingsIntent, 128, this.mUserId);
        if (resolveInfoResolveActivityAsUser != null) {
            Bundle bundle = resolveInfoResolveActivityAsUser.activityInfo.metaData;
            this.mSettingsIntent.setComponent(new ComponentName(resolveInfoResolveActivityAsUser.activityInfo.packageName, resolveInfoResolveActivityAsUser.activityInfo.name));
            if (bundle != null && bundle.containsKey("android.settings.metadata.USAGE_ACCESS_REASON")) {
                this.mSwitchPref.setSummary(bundle.getString("android.settings.metadata.USAGE_ACCESS_REASON"));
                return true;
            }
            return true;
        }
        return true;
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected AlertDialog createDialog(int i, int i2) {
        return null;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 183;
    }
}
