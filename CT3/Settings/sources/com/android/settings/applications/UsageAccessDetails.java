package com.android.settings.applications;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.applications.AppStateUsageBridge;

public class UsageAccessDetails extends AppInfoWithHeader implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private AppOpsManager mAppOpsManager;
    private DevicePolicyManager mDpm;
    private Intent mSettingsIntent;
    private SwitchPreference mSwitchPref;
    private AppStateUsageBridge mUsageBridge;
    private Preference mUsageDesc;
    private Preference mUsagePrefs;
    private AppStateUsageBridge.UsageState mUsageState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        this.mUsageBridge = new AppStateUsageBridge(context, this.mState, null);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mDpm = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        addPreferencesFromResource(R.xml.app_ops_permissions_details);
        this.mSwitchPref = (SwitchPreference) findPreference("app_ops_settings_switch");
        this.mUsagePrefs = findPreference("app_ops_settings_preference");
        this.mUsageDesc = findPreference("app_ops_settings_description");
        getPreferenceScreen().setTitle(R.string.usage_access);
        this.mSwitchPref.setTitle(R.string.permit_usage_access);
        this.mUsagePrefs.setTitle(R.string.app_usage_preference);
        this.mUsageDesc.setSummary(R.string.usage_access_description);
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        this.mUsagePrefs.setOnPreferenceClickListener(this);
        this.mSettingsIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.USAGE_ACCESS_CONFIG").setPackage(this.mPackageName);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mUsagePrefs) {
            if (this.mSettingsIntent != null) {
                try {
                    getActivity().startActivityAsUser(this.mSettingsIntent, new UserHandle(this.mUserId));
                    return true;
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "Unable to launch app usage access settings " + this.mSettingsIntent, e);
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference != this.mSwitchPref) {
            return false;
        }
        if (this.mUsageState != null && ((Boolean) newValue).booleanValue() != this.mUsageState.isPermissible()) {
            if (this.mUsageState.isPermissible() && this.mDpm.isProfileOwnerApp(this.mPackageName)) {
                new AlertDialog.Builder(getContext()).setIcon(android.R.drawable.dropdown_normal_holo_dark).setTitle(android.R.string.dialog_alert_title).setMessage(R.string.work_profile_usage_access_warning).setPositiveButton(R.string.okay, (DialogInterface.OnClickListener) null).show();
            }
            setHasAccess(this.mUsageState.isPermissible() ? false : true);
            refreshUi();
        }
        return true;
    }

    private void setHasAccess(boolean newState) {
        this.mAppOpsManager.setMode(43, this.mPackageInfo.applicationInfo.uid, this.mPackageName, newState ? 0 : 1);
    }

    @Override
    protected boolean refreshUi() {
        this.mUsageState = this.mUsageBridge.getUsageInfo(this.mPackageName, this.mPackageInfo.applicationInfo.uid);
        boolean hasAccess = this.mUsageState.isPermissible();
        this.mSwitchPref.setChecked(hasAccess);
        this.mSwitchPref.setEnabled(this.mUsageState.permissionDeclared);
        this.mUsagePrefs.setEnabled(hasAccess);
        ResolveInfo resolveInfo = this.mPm.resolveActivityAsUser(this.mSettingsIntent, 128, this.mUserId);
        if (resolveInfo != null) {
            if (findPreference("app_ops_settings_preference") == null) {
                getPreferenceScreen().addPreference(this.mUsagePrefs);
            }
            Bundle metaData = resolveInfo.activityInfo.metaData;
            this.mSettingsIntent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
            if (metaData != null && metaData.containsKey("android.settings.metadata.USAGE_ACCESS_REASON")) {
                this.mSwitchPref.setSummary(metaData.getString("android.settings.metadata.USAGE_ACCESS_REASON"));
                return true;
            }
            return true;
        }
        if (findPreference("app_ops_settings_preference") != null) {
            getPreferenceScreen().removePreference(this.mUsagePrefs);
            return true;
        }
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    protected int getMetricsCategory() {
        return 183;
    }
}
