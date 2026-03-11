package com.android.settings.applications.appinfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Pair;
import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.applications.AppStateWriteSettingsBridge;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState;

public class WriteSettingsDetails extends AppInfoWithHeader implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final int[] APP_OPS_OP_CODE = {23};
    private AppStateWriteSettingsBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private Intent mSettingsIntent;
    private SwitchPreference mSwitchPref;
    private AppStateWriteSettingsBridge.WriteSettingsState mWriteSettingsState;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        this.mAppBridge = new AppStateWriteSettingsBridge(activity, this.mState, null);
        this.mAppOpsManager = (AppOpsManager) activity.getSystemService("appops");
        addPreferencesFromResource(R.xml.write_system_settings_permissions_details);
        this.mSwitchPref = (SwitchPreference) findPreference("app_ops_settings_switch");
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        this.mSettingsIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.USAGE_ACCESS_CONFIG").setPackage(this.mPackageName);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mSwitchPref) {
            if (this.mWriteSettingsState != null && ((Boolean) obj).booleanValue() != this.mWriteSettingsState.isPermissible()) {
                setCanWriteSettings(!this.mWriteSettingsState.isPermissible());
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setCanWriteSettings(boolean z) {
        logSpecialPermissionChange(z, this.mPackageName);
        this.mAppOpsManager.setMode(23, this.mPackageInfo.applicationInfo.uid, this.mPackageName, z ? 0 : 2);
    }

    void logSpecialPermissionChange(boolean z, String str) {
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(), z ? 774 : 775, str, new Pair[0]);
    }

    @Override
    protected boolean refreshUi() {
        this.mWriteSettingsState = this.mAppBridge.getWriteSettingsInfo(this.mPackageName, this.mPackageInfo.applicationInfo.uid);
        this.mSwitchPref.setChecked(this.mWriteSettingsState.isPermissible());
        this.mSwitchPref.setEnabled(this.mWriteSettingsState.permissionDeclared);
        this.mPm.resolveActivityAsUser(this.mSettingsIntent, 128, this.mUserId);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int i, int i2) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return 221;
    }

    public static CharSequence getSummary(Context context, ApplicationsState.AppEntry appEntry) {
        AppStateWriteSettingsBridge.WriteSettingsState writeSettingsInfo;
        if (appEntry.extraInfo instanceof AppStateWriteSettingsBridge.WriteSettingsState) {
            writeSettingsInfo = (AppStateWriteSettingsBridge.WriteSettingsState) appEntry.extraInfo;
        } else if (appEntry.extraInfo instanceof AppStateAppOpsBridge.PermissionState) {
            writeSettingsInfo = new AppStateWriteSettingsBridge.WriteSettingsState((AppStateAppOpsBridge.PermissionState) appEntry.extraInfo);
        } else {
            writeSettingsInfo = new AppStateWriteSettingsBridge(context, null, null).getWriteSettingsInfo(appEntry.info.packageName, appEntry.info.uid);
        }
        return getSummary(context, writeSettingsInfo);
    }

    public static CharSequence getSummary(Context context, AppStateWriteSettingsBridge.WriteSettingsState writeSettingsState) {
        int i;
        if (writeSettingsState.isPermissible()) {
            i = R.string.app_permission_summary_allowed;
        } else {
            i = R.string.app_permission_summary_not_allowed;
        }
        return context.getString(i);
    }
}
