package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;

public class DefaultApnSettingsExt implements IApnSettingsExt {
    private static final String TAG = "DefaultApnSettingsExt";

    @Override
    public void onDestroy() {
    }

    @Override
    public void initTetherField(PreferenceFragment preferenceFragment) {
    }

    @Override
    public boolean isAllowEditPresetApn(String str, String str2, String str3, int i) {
        Log.d(TAG, "isAllowEditPresetApn");
        return true;
    }

    @Override
    public void customizeTetherApnSettings(PreferenceScreen preferenceScreen) {
    }

    @Override
    public String getFillListQuery(String str, String str2) {
        return str;
    }

    @Override
    public void updateTetherState() {
    }

    @Override
    public Uri getPreferCarrierUri(Uri uri, int i) {
        return uri;
    }

    @Override
    public void setApnTypePreferenceState(Preference preference, String str) {
    }

    @Override
    public Uri getUriFromIntent(Uri uri, Context context, Intent intent) {
        return uri;
    }

    @Override
    public String[] getApnTypeArray(String[] strArr, Context context, String str) {
        return strArr;
    }

    @Override
    public boolean isSelectable(String str) {
        return true;
    }

    @Override
    public boolean getScreenEnableState(int i, Activity activity) {
        return true;
    }

    @Override
    public void updateMenu(Menu menu, int i, int i2, String str) {
    }

    @Override
    public void addApnTypeExtra(Intent intent) {
    }

    @Override
    public void updateFieldsStatus(int i, int i2, PreferenceScreen preferenceScreen, String str) {
    }

    @Override
    public void setPreferenceTextAndSummary(int i, String str) {
    }

    @Override
    public void customizePreference(int i, PreferenceScreen preferenceScreen) {
    }

    @Override
    public String[] customizeApnProjection(String[] strArr) {
        return strArr;
    }

    @Override
    public void saveApnValues(ContentValues contentValues) {
    }

    @Override
    public String updateApnName(String str, int i) {
        return str;
    }

    @Override
    public long replaceApn(long j, Context context, Uri uri, String str, String str2, ContentValues contentValues, String str3) {
        return j;
    }

    @Override
    public void customizeUnselectableApn(String str, String str2, String str3, Object obj, Object obj2, int i) {
    }

    @Override
    public void setMvnoPreferenceState(Preference preference, Preference preference2) {
    }

    @Override
    public String getApnSortOrder(String str) {
        return str;
    }

    @Override
    public String getOperatorNumericFromImpi(String str, int i) {
        return str;
    }

    @Override
    public boolean customerUserEditable(int i) {
        return true;
    }

    @Override
    public boolean shouldSelectFirstApn() {
        Log.d(TAG, "shouldSelectFirstApn");
        return true;
    }

    @Override
    public void onApnSettingsEvent(int i) {
        Log.d(TAG, "onApnSettingsEvent");
    }
}
