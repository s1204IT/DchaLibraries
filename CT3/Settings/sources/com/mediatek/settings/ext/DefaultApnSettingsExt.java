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
import java.util.ArrayList;

public class DefaultApnSettingsExt implements IApnSettingsExt {
    private static final String TAG = "DefaultApnSettingsExt";

    @Override
    public void onDestroy() {
    }

    @Override
    public void initTetherField(PreferenceFragment pref) {
    }

    @Override
    public boolean isAllowEditPresetApn(String type, String apn, String numeric, int sourcetype) {
        Log.d(TAG, "isAllowEditPresetApn");
        return true;
    }

    @Override
    public void customizeTetherApnSettings(PreferenceScreen root) {
    }

    @Override
    public String getFillListQuery(String where, String mccmnc) {
        return where;
    }

    @Override
    public void updateTetherState() {
    }

    @Override
    public Uri getPreferCarrierUri(Uri defaultUri, int subId) {
        return defaultUri;
    }

    @Override
    public void setApnTypePreferenceState(Preference preference, String apnType) {
    }

    @Override
    public Uri getUriFromIntent(Uri defaultUri, Context context, Intent intent) {
        return defaultUri;
    }

    @Override
    public String[] getApnTypeArray(String[] defaultApnArray, Context context, String apnType) {
        return defaultApnArray;
    }

    @Override
    public boolean isSelectable(String type) {
        return true;
    }

    @Override
    public boolean getScreenEnableState(int subId, Activity activity) {
        return true;
    }

    @Override
    public void updateMenu(Menu menu, int newMenuId, int restoreMenuId, String numeric) {
    }

    @Override
    public void addApnTypeExtra(Intent it) {
    }

    @Override
    public void updateFieldsStatus(int subId, int sourceType, PreferenceScreen root) {
    }

    @Override
    public void setPreferenceTextAndSummary(int subId, String text) {
    }

    @Override
    public void customizePreference(int subId, PreferenceScreen root) {
    }

    @Override
    public String[] customizeApnProjection(String[] projection) {
        return projection;
    }

    @Override
    public void saveApnValues(ContentValues contentValues) {
    }

    @Override
    public String updateApnName(String name, int sourcetype) {
        return name;
    }

    @Override
    public long replaceApn(long defaultReplaceNum, Context context, Uri uri, String apn, String name, ContentValues values, String numeric) {
        return defaultReplaceNum;
    }

    @Override
    public void customizeUnselectableApn(String type, ArrayList<Preference> mnoApnList, ArrayList<Preference> mvnoApnList, int subId) {
    }

    @Override
    public void setMvnoPreferenceState(Preference mvnoType, Preference mvnoMatchData) {
    }

    @Override
    public String getApnSortOrder(String order) {
        return order;
    }

    @Override
    public String getOperatorNumericFromImpi(String defaultValue, int phoneId) {
        return defaultValue;
    }
}
