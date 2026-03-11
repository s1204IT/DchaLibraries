package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.widget.ImageView;

public class DefaultSettingsMiscExt extends ContextWrapper implements ISettingsMiscExt {
    private static final String KEY_GPS_SETTINGS_BUTTON = "gps_settings_button";
    static final String TAG = "DefaultSettingsMiscExt";

    public DefaultSettingsMiscExt(Context context) {
        super(context);
    }

    @Override
    public String customizeSimDisplayString(String str, int i) {
        return str;
    }

    @Override
    public void initCustomizedLocationSettings(PreferenceScreen preferenceScreen, int i) {
    }

    @Override
    public void updateCustomizedLocationSettings() {
    }

    @Override
    public void setFactoryResetTitle(Object obj) {
    }

    @Override
    public void setTimeoutPrefTitle(Preference preference) {
    }

    @Override
    public void addCustomizedItem(Object obj, Boolean bool) {
        Log.i(TAG, "DefaultSettingsMisc addCustomizedItem method going");
    }

    @Override
    public void customizeDashboardTile(Object obj, ImageView imageView) {
    }

    @Override
    public boolean isWifiOnlyModeSet() {
        return false;
    }

    @Override
    public String getNetworktypeString(String str, int i) {
        Log.d(TAG, "@M_getNetworktypeString defaultmethod return defaultString = " + str);
        return str;
    }

    @Override
    public String customizeMacAddressString(String str, String str2) {
        return str;
    }

    @Override
    public boolean doUpdateTilesList(Activity activity, boolean z, boolean z2) {
        Log.d(TAG, "default doUpdateTilesList");
        return z2;
    }

    @Override
    public void doWosFactoryReset() {
    }

    @Override
    public void addPreferenceController(Object obj, Object obj2) {
    }

    @Override
    public Object createPreferenceController(Context context, Object obj) {
        return null;
    }

    @Override
    public void customizeAGPRS(PreferenceScreen preferenceScreen) {
    }
}
