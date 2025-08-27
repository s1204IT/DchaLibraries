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
import android.view.Window;
import android.view.WindowManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.applications.AppStateOverlayBridge;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState;

/* loaded from: classes.dex */
public class DrawOverlayDetails extends AppInfoWithHeader implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final int[] APP_OPS_OP_CODE = {24};
    private AppOpsManager mAppOpsManager;
    private AppStateOverlayBridge mOverlayBridge;
    private AppStateOverlayBridge.OverlayState mOverlayState;
    private Intent mSettingsIntent;
    private SwitchPreference mSwitchPref;

    @Override // com.android.settings.applications.AppInfoBase, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        this.mOverlayBridge = new AppStateOverlayBridge(activity, this.mState, null);
        this.mAppOpsManager = (AppOpsManager) activity.getSystemService("appops");
        addPreferencesFromResource(R.xml.draw_overlay_permissions_details);
        this.mSwitchPref = (SwitchPreference) findPreference("app_ops_settings_switch");
        this.mSwitchPref.setOnPreferenceChangeListener(this);
        this.mSettingsIntent = new Intent("android.intent.action.MAIN").setAction("android.settings.action.MANAGE_OVERLAY_PERMISSION");
    }

    @Override // com.android.settings.applications.AppInfoBase, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        getActivity().getWindow().addPrivateFlags(524288);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        Window window = getActivity().getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.privateFlags &= -524289;
        window.setAttributes(attributes);
    }

    @Override // com.android.settings.applications.AppInfoBase, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        this.mOverlayBridge.release();
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mSwitchPref) {
            if (this.mOverlayState != null && ((Boolean) obj).booleanValue() != this.mOverlayState.isPermissible()) {
                setCanDrawOverlay(!this.mOverlayState.isPermissible());
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setCanDrawOverlay(boolean z) {
        logSpecialPermissionChange(z, this.mPackageName);
        this.mAppOpsManager.setMode(24, this.mPackageInfo.applicationInfo.uid, this.mPackageName, z ? 0 : 2);
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean z, String str) {
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(), z ? 770 : 771, str, new Pair[0]);
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected boolean refreshUi() {
        this.mOverlayState = this.mOverlayBridge.getOverlayInfo(this.mPackageName, this.mPackageInfo.applicationInfo.uid);
        this.mSwitchPref.setChecked(this.mOverlayState.isPermissible());
        this.mSwitchPref.setEnabled(this.mOverlayState.permissionDeclared && this.mOverlayState.controlEnabled);
        this.mPm.resolveActivityAsUser(this.mSettingsIntent, 128, this.mUserId);
        return true;
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected AlertDialog createDialog(int i, int i2) {
        return null;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 221;
    }

    public static CharSequence getSummary(Context context, ApplicationsState.AppEntry appEntry) {
        AppStateOverlayBridge.OverlayState overlayInfo;
        if (appEntry.extraInfo instanceof AppStateOverlayBridge.OverlayState) {
            overlayInfo = (AppStateOverlayBridge.OverlayState) appEntry.extraInfo;
        } else if (appEntry.extraInfo instanceof AppStateAppOpsBridge.PermissionState) {
            overlayInfo = new AppStateOverlayBridge.OverlayState((AppStateAppOpsBridge.PermissionState) appEntry.extraInfo);
        } else {
            overlayInfo = new AppStateOverlayBridge(context, null, null).getOverlayInfo(appEntry.info.packageName, appEntry.info.uid);
        }
        return getSummary(context, overlayInfo);
    }

    public static CharSequence getSummary(Context context, AppStateOverlayBridge.OverlayState overlayState) {
        return context.getString(overlayState.isPermissible() ? R.string.app_permission_summary_allowed : R.string.app_permission_summary_not_allowed);
    }
}
