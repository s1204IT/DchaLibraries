package com.mediatek.browser.ext;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.webkit.WebSettings;
import com.mediatek.common.search.SearchEngine;
import com.mediatek.search.SearchEngineManager;

public class DefaultBrowserSettingExt implements IBrowserSettingExt {
    @Override
    public void customizePreference(int i, PreferenceScreen preferenceScreen, Preference.OnPreferenceChangeListener onPreferenceChangeListener, SharedPreferences sharedPreferences, PreferenceFragment preferenceFragment) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: customizePreference --default implement");
    }

    @Override
    public String getCustomerHomepage() {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: getCustomerHomepage --default implement");
        return null;
    }

    @Override
    public String getDefaultDownloadFolder() {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: getDefaultDownloadFolder() --default implement");
        String str = "/storage/sdcard0/MyFavorite";
        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (absolutePath != null) {
            str = absolutePath + "/MyFavorite";
        }
        Log.v("@M_DefaultBrowserSettingsExt", "device default storage is: " + absolutePath + " defaultPath is: " + str);
        return str;
    }

    @Override
    public String getOperatorUA(String str) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: getOperatorUA --default implement");
        return null;
    }

    @Override
    public String getSearchEngine(SharedPreferences sharedPreferences, Context context) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: getSearchEngine --default implement");
        SearchEngine searchEngine = ((SearchEngineManager) context.getSystemService("search_engine_service")).getDefault();
        return sharedPreferences.getString("search_engine", searchEngine != null ? searchEngine.getName() : "google");
    }

    @Override
    public void setOnlyLandscape(SharedPreferences sharedPreferences, Activity activity) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: setOnlyLandscape --default implement");
        if (activity != null) {
            activity.setRequestedOrientation(-1);
        }
    }

    @Override
    public void setStandardFontFamily(WebSettings webSettings, SharedPreferences sharedPreferences) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: setStandardFontFamily --default implement");
    }

    @Override
    public boolean updatePreferenceItem(Preference preference, Object obj) {
        Log.i("@M_DefaultBrowserSettingsExt", "Enter: updatePreferenceItem --default implement");
        return false;
    }
}
