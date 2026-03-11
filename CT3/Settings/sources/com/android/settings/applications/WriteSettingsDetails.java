package com.android.settings.applications;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.applications.AppStateWriteSettingsBridge;
import com.android.settingslib.applications.ApplicationsState;

public class WriteSettingsDetails extends AppInfoWithHeader implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final int[] APP_OPS_OP_CODE = {23};
    private AppStateWriteSettingsBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private Intent mSettingsIntent;
    private SwitchPreference mSwitchPref;
    private Preference mWriteSettingsDesc;
    private Preference mWriteSettingsPrefs;
    private AppStateWriteSettingsBridge.WriteSettingsState mWriteSettingsState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        this.mAppBridge = new AppStateWriteSettingsBridge(context, this.mState, null);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        addPreferencesFromResource(R.xml.app_ops_permissions_details);
        this.mSwitchPref = (SwitchPreference) findPreference("app_ops_settings_switch");
        this.mWriteSettingsPrefs = findPreference("app_ops_settings_preference");
        this.mWriteSettingsDesc = findPreference("app_ops_settings_description");
        getPreferenceScreen().setTitle(R.string.write_settings);
        this.mSwitchPref.setTitle(R.string.permit_write_settings);
        this.mWriteSettingsPrefs.setTitle(R.string.write_settings_preference);
        this.mWriteSettingsDesc.setSummary(R.string.write_settings_description);
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        this.mWriteSettingsPrefs.setOnPreferenceClickListener(this);
        this.mSettingsIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.USAGE_ACCESS_CONFIG").setPackage(this.mPackageName);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mWriteSettingsPrefs) {
            if (this.mSettingsIntent != null) {
                try {
                    getActivity().startActivityAsUser(this.mSettingsIntent, new UserHandle(this.mUserId));
                    return true;
                } catch (ActivityNotFoundException e) {
                    Log.w("WriteSettingsDetails", "Unable to launch write system settings " + this.mSettingsIntent, e);
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
        if (this.mWriteSettingsState != null && ((Boolean) newValue).booleanValue() != this.mWriteSettingsState.isPermissible()) {
            setCanWriteSettings(this.mWriteSettingsState.isPermissible() ? false : true);
            refreshUi();
        }
        return true;
    }

    private void setCanWriteSettings(boolean newState) {
        this.mAppOpsManager.setMode(23, this.mPackageInfo.applicationInfo.uid, this.mPackageName, newState ? 0 : 2);
    }

    @Override
    protected boolean refreshUi() {
        this.mWriteSettingsState = this.mAppBridge.getWriteSettingsInfo(this.mPackageName, this.mPackageInfo.applicationInfo.uid);
        boolean canWrite = this.mWriteSettingsState.isPermissible();
        this.mSwitchPref.setChecked(canWrite);
        this.mSwitchPref.setEnabled(this.mWriteSettingsState.permissionDeclared);
        this.mWriteSettingsPrefs.setEnabled(canWrite);
        getPreferenceScreen().removePreference(this.mWriteSettingsPrefs);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    protected int getMetricsCategory() {
        return 221;
    }

    public static CharSequence getSummary(Context context, ApplicationsState.AppEntry entry) {
        AppStateWriteSettingsBridge.WriteSettingsState state;
        if (entry.extraInfo instanceof AppStateWriteSettingsBridge.WriteSettingsState) {
            state = (AppStateWriteSettingsBridge.WriteSettingsState) entry.extraInfo;
        } else if (entry.extraInfo instanceof AppStateAppOpsBridge.PermissionState) {
            state = new AppStateWriteSettingsBridge.WriteSettingsState((AppStateAppOpsBridge.PermissionState) entry.extraInfo);
        } else {
            state = new AppStateWriteSettingsBridge(context, null, null).getWriteSettingsInfo(entry.info.packageName, entry.info.uid);
        }
        return getSummary(context, state);
    }

    public static CharSequence getSummary(Context context, AppStateWriteSettingsBridge.WriteSettingsState writeSettingsState) {
        return context.getString(writeSettingsState.isPermissible() ? R.string.write_settings_on : R.string.write_settings_off);
    }
}
