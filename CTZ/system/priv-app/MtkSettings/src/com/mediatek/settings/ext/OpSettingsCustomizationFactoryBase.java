package com.mediatek.settings.ext;

import android.content.Context;

/* loaded from: classes.dex */
public class OpSettingsCustomizationFactoryBase {
    private Context mContext;

    public OpSettingsCustomizationFactoryBase(Context context) {
        this.mContext = context;
    }

    public IDataUsageSummaryExt makeDataUsageSummaryExt() {
        return new DefaultDataUsageSummaryExt(this.mContext);
    }

    public IDisplaySettingsExt makeDisplaySettingsExt(Context context) {
        return new DefaultDisplaySettingsExt(context);
    }

    public IApnSettingsExt makeApnSettingsExt(Context context) {
        return new DefaultApnSettingsExt();
    }

    public IRCSSettings makeRCSSettings(Context context) {
        return new DefaultRCSSettings(context);
    }

    public IWWOPJoynSettingsExt makeWWOPJoynSettingsExt(Context context) {
        return new DefaultWWOPJoynSettingsExt(context);
    }

    public ISettingsMiscExt makeSettingsMiscExt(Context context) {
        return new DefaultSettingsMiscExt(context);
    }

    public ISimManagementExt makeSimManagementExt() {
        return new DefaultSimManagementExt();
    }

    public ISimRoamingExt makeSimRoamingExt() {
        return new DefaultSimRoamingExt();
    }

    public ISmsDialogExt makeSmsDialogExt() {
        return new DefaultSmsDialogExt(this.mContext);
    }

    public ISmsPreferenceExt makeSmsPreferenceExt() {
        return new DefaultSmsPreferenceExt();
    }

    public IWfcSettingsExt makeWfcSettingsExt() {
        return new DefaultWfcSettingsExt();
    }

    public IWifiApDialogExt makeWifiApDialogExt() {
        return new DefaultWifiApDialogExt();
    }

    public IWifiTetherSettingsExt makeWifiTetherSettingsExt(Context context) {
        return new DefaultWifiTetherSettingsExt(context);
    }

    public IWifiExt makeWifiExt(Context context) {
        return new DefaultWifiExt(context);
    }

    public IWifiSettingsExt makeWifiSettingsExt() {
        return new DefaultWifiSettingsExt();
    }

    public IAppListExt makeAppListExt(Context context) {
        return new DefaultAppListExt(context);
    }

    public IAppsExt makeAppsExt(Context context) {
        return new DefaultAppsExt(context);
    }

    public IAudioProfileExt makeAudioProfileExt(Context context) {
        return new DefaultAudioProfileExt(context);
    }

    public IDevExt makeDevExt(Context context) {
        return new DefaultDevExt(context);
    }

    public IDeviceInfoSettingsExt makeDeviceInfoSettingsExt() {
        return new DefaultDeviceInfoSettingsExt();
    }

    public IStatusBarPlmnDisplayExt makeStatusBarPlmnDisplayExt(Context context) {
        return new DefaultStatusBarPlmnDisplayExt(context);
    }

    public IRcseOnlyApnExt makeRcseOnlyApnExt() {
        return new DefaultRcseOnlyApnExt();
    }

    public IStatusExt makeStatusExt() {
        return new DefaultStatusExt();
    }
}
