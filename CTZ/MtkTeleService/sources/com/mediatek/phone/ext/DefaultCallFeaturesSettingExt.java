package com.mediatek.phone.ext;

import android.content.Context;
import android.os.AsyncResult;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

public class DefaultCallFeaturesSettingExt implements ICallFeaturesSettingExt {
    public static final int DESTROY = 2;
    public static final int PAUSE = 1;
    public static final int RESUME = 0;

    @Override
    public void initOtherCallFeaturesSetting(PreferenceActivity preferenceActivity) {
    }

    @Override
    public void initOtherCallFeaturesSetting(PreferenceFragment preferenceFragment) {
    }

    @Override
    public void initCdmaCallForwardOptionsActivity(PreferenceActivity preferenceActivity, int i) {
    }

    @Override
    public void resetImsPdnOverSSComplete(Context context, int i) {
        Log.d("DefaultCallFeaturesSettingExt", "resetImsPdnOverSSComplete");
    }

    @Override
    public boolean needShowOpenMobileDataDialog(Context context, int i) {
        return true;
    }

    @Override
    public void onError(Preference preference) {
        Log.d("DefaultCallFeaturesSettingExt", "default onError");
    }

    @Override
    public boolean handleErrorDialog(Context context, AsyncResult asyncResult, Preference preference) {
        Log.d("DefaultCallFeaturesSettingExt", "default handleErrorDialog");
        return false;
    }

    @Override
    public void initPlugin(PreferenceActivity preferenceActivity, Preference preference) {
    }

    @Override
    public void onCallFeatureSettingsEvent(int i) {
    }

    @Override
    public String getWfcSummary(Context context, int i) {
        return context.getResources().getString(i);
    }

    @Override
    public void videoPreferenceChange(boolean z) {
        Log.d("DefaultCallFeaturesSettingExt", "videoPreferenceChange");
    }

    @Override
    public void disableCallFwdPref(Context context, Object obj, Preference preference, int i) {
        Log.d("DefaultCallFeaturesSettingExt", "default disableCallFwdPref");
    }

    @Override
    public void customizeAdditionalSettings(PreferenceActivity preferenceActivity, Object obj) {
        Log.d("DefaultCallFeaturesSettingExt", "default customizeAdditionalSettings");
    }

    @Override
    public boolean escapeCLIRInit() {
        Log.d("DefaultCallFeaturesSettingExt", "default escapeCLIRInit");
        return false;
    }
}
