package com.mediatek.phone.ext;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import com.android.internal.telephony.Phone;

public class DefaultMobileNetworkSettingsExt implements IMobileNetworkSettingsExt {
    @Override
    public void initOtherMobileNetworkSettings(PreferenceActivity preferenceActivity, int i) {
    }

    @Override
    public void initOtherMobileNetworkSettings(PreferenceScreen preferenceScreen, int i) {
    }

    @Override
    public void customizeBasicMobileNetworkSettings(PreferenceScreen preferenceScreen, int i) {
    }

    @Override
    public void initMobileNetworkSettings(PreferenceActivity preferenceActivity, int i) {
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    @Override
    public void updateLTEModeStatus(ListPreference listPreference) {
    }

    @Override
    public void updateNetworkTypeSummary(ListPreference listPreference) {
    }

    @Override
    public void customizeDataRoamingAlertDialog(AlertDialog.Builder builder, int i) {
    }

    @Override
    public void customizePreferredNetworkMode(ListPreference listPreference, int i) {
    }

    @Override
    public void onPreferenceChange(Preference preference, Object obj) {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void unRegister() {
    }

    @Override
    public boolean isCtPlugin() {
        return false;
    }

    @Override
    public void changeEntries(ListPreference listPreference) {
    }

    @Override
    public void updatePreferredNetworkValueAndSummary(ListPreference listPreference, int i) {
    }

    @Override
    public void customizeEnhanced4GLteSwitchPreference(PreferenceScreen preferenceScreen, SwitchPreference switchPreference) {
        Log.d("DefaultMobileNetworkSettingsExt", "customizeEnhanced4GLteSwitchPreference");
    }

    @Override
    public boolean isNetworkUpdateNeeded(ListPreference listPreference, int i, int i2, Phone phone, ContentResolver contentResolver, int i3, Handler handler) {
        return true;
    }

    @Override
    public boolean isNetworkModeSettingNeeded() {
        return true;
    }

    @Override
    public boolean isEnhancedLTENeedToAdd(boolean z, int i) {
        return z;
    }

    @Override
    public boolean customizeDualVolteOpDisable(int i, boolean z) {
        return z;
    }

    @Override
    public void customizeDualVolteIntentFilter(IntentFilter intentFilter) {
    }

    @Override
    public boolean customizeDualVolteReceiveIntent(String str) {
        return false;
    }

    @Override
    public void customizeDualVolteOpHide(PreferenceScreen preferenceScreen, Preference preference, boolean z) {
    }

    @Override
    public boolean customizeCUVolte() {
        return false;
    }

    @Override
    public boolean isNetworkChanged(ListPreference listPreference, int i, int i2, Phone phone) {
        return false;
    }

    @Override
    public void customizeDataEnable(int i, Object obj) {
    }

    @Override
    public void customizeWfcPreference(Context context, PreferenceScreen preferenceScreen, int i) {
    }

    @Override
    public void customizeWfcPreference(Context context, PreferenceScreen preferenceScreen, PreferenceCategory preferenceCategory, int i) {
    }

    @Override
    public boolean isWfcProvisioned(Context context, int i) {
        return true;
    }

    @Override
    public String customizeWfcSummary(Context context, int i, int i2) {
        return context.getResources().getString(i);
    }

    @Override
    public boolean customizeDualCCcard(int i) {
        return true;
    }
}
