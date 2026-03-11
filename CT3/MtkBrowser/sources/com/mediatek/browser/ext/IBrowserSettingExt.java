package com.mediatek.browser.ext;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.webkit.WebSettings;

public interface IBrowserSettingExt {
    void customizePreference(int i, PreferenceScreen preferenceScreen, Preference.OnPreferenceChangeListener onPreferenceChangeListener, SharedPreferences sharedPreferences, PreferenceFragment preferenceFragment);

    String getCustomerHomepage();

    String getDefaultDownloadFolder();

    String getOperatorUA(String str);

    String getSearchEngine(SharedPreferences sharedPreferences, Context context);

    void setOnlyLandscape(SharedPreferences sharedPreferences, Activity activity);

    void setStandardFontFamily(WebSettings webSettings, SharedPreferences sharedPreferences);

    void setTextEncodingChoices(ListPreference listPreference);

    boolean updatePreferenceItem(Preference preference, Object obj);
}
