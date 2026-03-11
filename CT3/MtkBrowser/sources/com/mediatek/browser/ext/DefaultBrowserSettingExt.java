package com.mediatek.browser.ext;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.webkit.WebSettings;
import com.mediatek.common.search.SearchEngine;
import com.mediatek.search.SearchEngineManager;
import com.mediatek.storage.StorageManagerEx;

public class DefaultBrowserSettingExt implements IBrowserSettingExt {
    private static final String DEFAULT_DOWNLOAD_DIRECTORY = "/storage/sdcard0/MyFavorite";
    private static final String DEFAULT_MY_FAVORITE_FOLDER = "/MyFavorite";
    private static final String DEFAULT_SEARCH_ENGIN = "google";
    private static final String PREF_SEARCH_ENGINE = "search_engine";
    private static final String TAG = "DefaultBrowserSettingsExt";

    @Override
    public void customizePreference(int index, PreferenceScreen prefSc, Preference.OnPreferenceChangeListener onPreferenceChangeListener, SharedPreferences sharedPref, PreferenceFragment prefFrag) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: customizePreference --default implement");
    }

    @Override
    public boolean updatePreferenceItem(Preference pref, Object objValue) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: updatePreferenceItem --default implement");
        return false;
    }

    @Override
    public String getCustomerHomepage() {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: getCustomerHomepage --default implement");
        return null;
    }

    @Override
    public String getDefaultDownloadFolder() {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: getDefaultDownloadFolder() --default implement");
        String defaultDownloadPath = DEFAULT_DOWNLOAD_DIRECTORY;
        String defaultStorage = StorageManagerEx.getDefaultPath();
        if (defaultStorage != null) {
            defaultDownloadPath = defaultStorage + DEFAULT_MY_FAVORITE_FOLDER;
        }
        Log.v("@M_DefaultBrowserSettingsExt", "device default storage is: " + defaultStorage + " defaultPath is: " + defaultDownloadPath);
        return defaultDownloadPath;
    }

    @Override
    public String getOperatorUA(String defaultUA) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: getOperatorUA --default implement");
        return null;
    }

    @Override
    public String getSearchEngine(SharedPreferences pref, Context context) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: getSearchEngine --default implement");
        SearchEngineManager searchEngineManager = (SearchEngineManager) context.getSystemService(PREF_SEARCH_ENGINE);
        SearchEngine info = searchEngineManager.getDefault();
        String defaultSearchEngine = DEFAULT_SEARCH_ENGIN;
        if (info != null) {
            defaultSearchEngine = info.getName();
        }
        return pref.getString(PREF_SEARCH_ENGINE, defaultSearchEngine);
    }

    @Override
    public void setOnlyLandscape(SharedPreferences pref, Activity activity) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: setOnlyLandscape --default implement");
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(-1);
    }

    @Override
    public void setStandardFontFamily(WebSettings settings, SharedPreferences pref) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: setStandardFontFamily --default implement");
    }

    @Override
    public void setTextEncodingChoices(ListPreference pref) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: setTextEncodingChoices --default implement");
    }
}
