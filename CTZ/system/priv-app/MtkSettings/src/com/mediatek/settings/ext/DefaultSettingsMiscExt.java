package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.widget.ImageView;

/* loaded from: classes.dex */
public class DefaultSettingsMiscExt extends ContextWrapper implements ISettingsMiscExt {
    private static final String KEY_GPS_SETTINGS_BUTTON = "gps_settings_button";
    static final String TAG = "DefaultSettingsMiscExt";

    public DefaultSettingsMiscExt(Context context) {
        super(context);
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public String customizeSimDisplayString(String str, int i) {
        return str;
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public void initCustomizedLocationSettings(PreferenceScreen preferenceScreen, int i) {
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public void updateCustomizedLocationSettings() {
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public void setFactoryResetTitle(Object obj) {
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public void setTimeoutPrefTitle(Preference preference) {
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public void addCustomizedItem(Object obj, Boolean bool) {
        Log.i(TAG, "DefaultSettingsMisc addCustomizedItem method going");
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public void customizeDashboardTile(Object obj, ImageView imageView) {
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public boolean isWifiOnlyModeSet() {
        return false;
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public String getNetworktypeString(String str, int i) {
        Log.d(TAG, "@M_getNetworktypeString defaultmethod return defaultString = " + str);
        return str;
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public String customizeMacAddressString(String str, String str2) {
        return str;
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public boolean doUpdateTilesList(Activity activity, boolean z, boolean z2) {
        Log.d(TAG, "default doUpdateTilesList");
        return z2;
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public void doWosFactoryReset() {
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public void addPreferenceController(Object obj, Object obj2) {
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public Object createPreferenceController(Context context, Object obj) {
        return null;
    }

    @Override // com.mediatek.settings.ext.ISettingsMiscExt
    public void customizeAGPRS(PreferenceScreen preferenceScreen) {
    }
}
