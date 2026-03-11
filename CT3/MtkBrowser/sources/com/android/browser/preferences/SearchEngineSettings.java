package com.android.browser.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.android.browser.BrowserSettings;
import com.android.browser.R;
import com.mediatek.common.search.SearchEngine;
import com.mediatek.search.SearchEngineManager;
import java.util.ArrayList;
import java.util.List;

public class SearchEngineSettings extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    private PreferenceActivity mActivity;
    private CheckBoxPreference mCheckBoxPref;
    private String[] mEntries;
    private String[] mEntryFavicon;
    private String[] mEntryValues;
    private SharedPreferences mPrefs;
    private List<RadioPreference> mRadioPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mRadioPrefs = new ArrayList();
        this.mActivity = (PreferenceActivity) getActivity();
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mActivity);
        int selectedItem = -1;
        String searchEngineName = BrowserSettings.getInstance().getSearchEngineName();
        if (searchEngineName != null) {
            SearchEngineManager searchEngineManager = (SearchEngineManager) this.mActivity.getSystemService("search_engine");
            List<SearchEngine> searchEngines = searchEngineManager.getAvailables();
            int len = searchEngines.size();
            this.mEntryValues = new String[len];
            this.mEntries = new String[len];
            this.mEntryFavicon = new String[len];
            for (int i = 0; i < len; i++) {
                this.mEntryValues[i] = searchEngines.get(i).getName();
                this.mEntries[i] = searchEngines.get(i).getLabel();
                this.mEntryFavicon[i] = searchEngines.get(i).getFaviconUri();
                if (this.mEntryValues[i].equals(searchEngineName)) {
                    selectedItem = i;
                }
            }
            setPreferenceScreen(createPreferenceHierarchy());
            this.mRadioPrefs.get(selectedItem).setChecked(true);
        }
        if (!this.mCheckBoxPref.isChecked()) {
            return;
        }
        broadcastSearchEngineChangedExternal(this.mActivity);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = this.mPrefs.edit();
        editor.putBoolean("syc_search_engine", this.mCheckBoxPref.isChecked());
        editor.commit();
        if (!this.mCheckBoxPref.isChecked()) {
            return;
        }
        broadcastSearchEngineChangedExternal(this.mActivity);
    }

    private void broadcastSearchEngineChangedExternal(Context context) {
        Intent intent = new Intent("com.android.quicksearchbox.SEARCH_ENGINE_CHANGED");
        intent.setPackage("com.android.quicksearchbox");
        intent.putExtra("search_engine", BrowserSettings.getInstance().getSearchEngineName());
        Log.i("@M_browser/SearchEngineSettings", "Broadcasting: " + intent);
        context.sendBroadcast(intent);
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen preferenceScreenCreatePreferenceScreen = getPreferenceManager().createPreferenceScreen(this.mActivity);
        PreferenceCategory consistencyPref = new PreferenceCategory(this.mActivity);
        consistencyPref.setTitle(R.string.pref_content_search_engine_consistency);
        preferenceScreenCreatePreferenceScreen.addPreference(consistencyPref);
        this.mCheckBoxPref = new CheckBoxPreference(this.mActivity);
        this.mCheckBoxPref.setKey("toggle_consistency");
        this.mCheckBoxPref.setTitle(R.string.pref_search_engine_unify);
        this.mCheckBoxPref.setSummaryOn(R.string.pref_search_engine_unify_summary);
        this.mCheckBoxPref.setSummaryOff(R.string.pref_search_engine_unify_summary);
        consistencyPref.addPreference(this.mCheckBoxPref);
        boolean syncSearchEngine = this.mPrefs.getBoolean("syc_search_engine", true);
        this.mCheckBoxPref.setChecked(syncSearchEngine);
        PreferenceCategory searchEnginesPref = new PreferenceCategory(this.mActivity);
        searchEnginesPref.setTitle(R.string.pref_content_search_engine);
        preferenceScreenCreatePreferenceScreen.addPreference(searchEnginesPref);
        for (int i = 0; i < this.mEntries.length; i++) {
            RadioPreference radioPref = new RadioPreference(this.mActivity);
            radioPref.setWidgetLayoutResource(R.layout.radio_preference);
            radioPref.setTitle(this.mEntries[i]);
            radioPref.setOrder(i);
            radioPref.setOnPreferenceClickListener(this);
            searchEnginesPref.addPreference(radioPref);
            this.mRadioPrefs.add(radioPref);
        }
        return preferenceScreenCreatePreferenceScreen;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        for (RadioPreference radioPref : this.mRadioPrefs) {
            radioPref.setChecked(false);
        }
        ((RadioPreference) preference).setChecked(true);
        SharedPreferences.Editor editor = this.mPrefs.edit();
        editor.putString("search_engine", this.mEntryValues[preference.getOrder()]);
        editor.putString("search_engine_favicon", this.mEntryFavicon[preference.getOrder()]);
        editor.commit();
        return true;
    }
}
