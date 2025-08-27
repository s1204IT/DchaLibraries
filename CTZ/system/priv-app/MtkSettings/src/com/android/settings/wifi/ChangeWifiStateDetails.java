package com.android.settings.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Pair;
import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.AppStateChangeWifiStateBridge;
import com.android.settingslib.applications.ApplicationsState;

/* loaded from: classes.dex */
public class ChangeWifiStateDetails extends AppInfoWithHeader implements Preference.OnPreferenceChangeListener {
    private AppStateChangeWifiStateBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private SwitchPreference mSwitchPref;
    private AppStateChangeWifiStateBridge.WifiSettingsState mWifiSettingsState;

    @Override // com.android.settings.applications.AppInfoBase, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        this.mAppBridge = new AppStateChangeWifiStateBridge(activity, this.mState, null);
        this.mAppOpsManager = (AppOpsManager) activity.getSystemService("appops");
        addPreferencesFromResource(R.xml.change_wifi_state_details);
        this.mSwitchPref = (SwitchPreference) findPreference("app_ops_settings_switch");
        this.mSwitchPref.setTitle(R.string.change_wifi_state_app_detail_switch);
        this.mSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected AlertDialog createDialog(int i, int i2) {
        return null;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 338;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mSwitchPref) {
            if (this.mWifiSettingsState != null && ((Boolean) obj).booleanValue() != this.mWifiSettingsState.isPermissible()) {
                setCanChangeWifiState(!this.mWifiSettingsState.isPermissible());
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setCanChangeWifiState(boolean z) {
        logSpecialPermissionChange(z, this.mPackageName);
        this.mAppOpsManager.setMode(71, this.mPackageInfo.applicationInfo.uid, this.mPackageName, z ? 0 : 1);
    }

    protected void logSpecialPermissionChange(boolean z, String str) {
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(), z ? 774 : 775, str, new Pair[0]);
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected boolean refreshUi() {
        if (this.mPackageInfo == null || this.mPackageInfo.applicationInfo == null) {
            return false;
        }
        this.mWifiSettingsState = this.mAppBridge.getWifiSettingsInfo(this.mPackageName, this.mPackageInfo.applicationInfo.uid);
        this.mSwitchPref.setChecked(this.mWifiSettingsState.isPermissible());
        this.mSwitchPref.setEnabled(this.mWifiSettingsState.permissionDeclared);
        return true;
    }

    public static CharSequence getSummary(Context context, ApplicationsState.AppEntry appEntry) {
        AppStateChangeWifiStateBridge.WifiSettingsState wifiSettingsInfo;
        if (appEntry.extraInfo instanceof AppStateChangeWifiStateBridge.WifiSettingsState) {
            wifiSettingsInfo = (AppStateChangeWifiStateBridge.WifiSettingsState) appEntry.extraInfo;
        } else if (appEntry.extraInfo instanceof AppStateAppOpsBridge.PermissionState) {
            wifiSettingsInfo = new AppStateChangeWifiStateBridge.WifiSettingsState((AppStateAppOpsBridge.PermissionState) appEntry.extraInfo);
        } else {
            wifiSettingsInfo = new AppStateChangeWifiStateBridge(context, null, null).getWifiSettingsInfo(appEntry.info.packageName, appEntry.info.uid);
        }
        return getSummary(context, wifiSettingsInfo);
    }

    public static CharSequence getSummary(Context context, AppStateChangeWifiStateBridge.WifiSettingsState wifiSettingsState) {
        int i;
        if (wifiSettingsState.isPermissible()) {
            i = R.string.app_permission_summary_allowed;
        } else {
            i = R.string.app_permission_summary_not_allowed;
        }
        return context.getString(i);
    }
}
