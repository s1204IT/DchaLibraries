package com.mediatek.phone.ext;

import android.content.Context;
import android.os.AsyncResult;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public interface ICallFeaturesSettingExt {
    void customizeAdditionalSettings(PreferenceActivity preferenceActivity, Object obj);

    void disableCallFwdPref(Context context, Object obj, Preference preference, int i);

    boolean escapeCLIRInit();

    String getWfcSummary(Context context, int i);

    boolean handleErrorDialog(Context context, AsyncResult asyncResult, Preference preference);

    void initCdmaCallForwardOptionsActivity(PreferenceActivity preferenceActivity, int i);

    void initOtherCallFeaturesSetting(PreferenceActivity preferenceActivity);

    void initOtherCallFeaturesSetting(PreferenceFragment preferenceFragment);

    void initPlugin(PreferenceActivity preferenceActivity, Preference preference);

    boolean needShowOpenMobileDataDialog(Context context, int i);

    void onCallFeatureSettingsEvent(int i);

    void onError(Preference preference);

    void resetImsPdnOverSSComplete(Context context, int i);

    void videoPreferenceChange(boolean z);
}
