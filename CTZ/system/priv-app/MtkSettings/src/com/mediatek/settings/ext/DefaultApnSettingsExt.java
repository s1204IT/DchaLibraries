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

/* loaded from: classes.dex */
public class DefaultApnSettingsExt implements IApnSettingsExt {
    private static final String TAG = "DefaultApnSettingsExt";

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void onDestroy() {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void initTetherField(PreferenceFragment preferenceFragment) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public boolean isAllowEditPresetApn(String str, String str2, String str3, int i) {
        Log.d(TAG, "isAllowEditPresetApn");
        return true;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void customizeTetherApnSettings(PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public String getFillListQuery(String str, String str2) {
        return str;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void updateTetherState() {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public Uri getPreferCarrierUri(Uri uri, int i) {
        return uri;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void setApnTypePreferenceState(Preference preference, String str) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public Uri getUriFromIntent(Uri uri, Context context, Intent intent) {
        return uri;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public String[] getApnTypeArray(String[] strArr, Context context, String str) {
        return strArr;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public boolean isSelectable(String str) {
        return true;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public boolean getScreenEnableState(int i, Activity activity) {
        return true;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void updateMenu(Menu menu, int i, int i2, String str) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void addApnTypeExtra(Intent intent) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void updateFieldsStatus(int i, int i2, PreferenceScreen preferenceScreen, String str) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void setPreferenceTextAndSummary(int i, String str) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void customizePreference(int i, PreferenceScreen preferenceScreen) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public String[] customizeApnProjection(String[] strArr) {
        return strArr;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void saveApnValues(ContentValues contentValues) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public String updateApnName(String str, int i) {
        return str;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public long replaceApn(long j, Context context, Uri uri, String str, String str2, ContentValues contentValues, String str3) {
        return j;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void customizeUnselectableApn(String str, String str2, String str3, Object obj, Object obj2, int i) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void setMvnoPreferenceState(Preference preference, Preference preference2) {
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public String getApnSortOrder(String str) {
        return str;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public String getOperatorNumericFromImpi(String str, int i) {
        return str;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public boolean customerUserEditable(int i) {
        return true;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public boolean shouldSelectFirstApn() {
        Log.d(TAG, "shouldSelectFirstApn");
        return true;
    }

    @Override // com.mediatek.settings.ext.IApnSettingsExt
    public void onApnSettingsEvent(int i) {
        Log.d(TAG, "onApnSettingsEvent");
    }
}
