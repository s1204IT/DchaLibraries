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
import com.android.internal.telephony.Phone;

public interface IMobileNetworkSettingsExt {
    void changeEntries(ListPreference listPreference);

    void customizeBasicMobileNetworkSettings(PreferenceScreen preferenceScreen, int i);

    boolean customizeCUVolte();

    void customizeDataEnable(int i, Object obj);

    void customizeDataRoamingAlertDialog(AlertDialog.Builder builder, int i);

    boolean customizeDualCCcard(int i);

    void customizeDualVolteIntentFilter(IntentFilter intentFilter);

    boolean customizeDualVolteOpDisable(int i, boolean z);

    void customizeDualVolteOpHide(PreferenceScreen preferenceScreen, Preference preference, boolean z);

    boolean customizeDualVolteReceiveIntent(String str);

    void customizeEnhanced4GLteSwitchPreference(PreferenceScreen preferenceScreen, SwitchPreference switchPreference);

    void customizePreferredNetworkMode(ListPreference listPreference, int i);

    void customizeWfcPreference(Context context, PreferenceScreen preferenceScreen, int i);

    void customizeWfcPreference(Context context, PreferenceScreen preferenceScreen, PreferenceCategory preferenceCategory, int i);

    String customizeWfcSummary(Context context, int i, int i2);

    void initMobileNetworkSettings(PreferenceActivity preferenceActivity, int i);

    void initOtherMobileNetworkSettings(PreferenceActivity preferenceActivity, int i);

    void initOtherMobileNetworkSettings(PreferenceScreen preferenceScreen, int i);

    boolean isCtPlugin();

    boolean isEnhancedLTENeedToAdd(boolean z, int i);

    boolean isNetworkChanged(ListPreference listPreference, int i, int i2, Phone phone);

    boolean isNetworkModeSettingNeeded();

    boolean isNetworkUpdateNeeded(ListPreference listPreference, int i, int i2, Phone phone, ContentResolver contentResolver, int i3, Handler handler);

    boolean isWfcProvisioned(Context context, int i);

    void onPause();

    void onPreferenceChange(Preference preference, Object obj);

    boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference);

    void onResume();

    void unRegister();

    void updateLTEModeStatus(ListPreference listPreference);

    void updateNetworkTypeSummary(ListPreference listPreference);

    void updatePreferredNetworkValueAndSummary(ListPreference listPreference, int i);
}
