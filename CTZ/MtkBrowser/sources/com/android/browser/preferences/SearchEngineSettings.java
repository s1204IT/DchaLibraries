package com.android.browser.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import com.android.browser.BrowserSettings;
import com.mediatek.common.search.SearchEngine;
import com.mediatek.search.SearchEngineManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SearchEngineSettings extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    private PreferenceActivity mActivity;
    private String[] mEntries;
    private String[] mEntryFavicon;
    private String[] mEntryValues;
    private SharedPreferences mPrefs;
    private List<RadioPreference> mRadioPrefs;

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen preferenceScreenCreatePreferenceScreen = getPreferenceManager().createPreferenceScreen(this.mActivity);
        PreferenceCategory preferenceCategory = new PreferenceCategory(this.mActivity);
        preferenceCategory.setTitle(2131493065);
        preferenceScreenCreatePreferenceScreen.addPreference(preferenceCategory);
        for (int i = 0; i < this.mEntries.length; i++) {
            RadioPreference radioPreference = new RadioPreference(this.mActivity);
            radioPreference.setWidgetLayoutResource(2130968619);
            radioPreference.setTitle(this.mEntries[i]);
            radioPreference.setOrder(i);
            radioPreference.setOnPreferenceClickListener(this);
            preferenceCategory.addPreference(radioPreference);
            this.mRadioPrefs.add(radioPreference);
        }
        return preferenceScreenCreatePreferenceScreen;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mRadioPrefs = new ArrayList();
        this.mActivity = (PreferenceActivity) getActivity();
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mActivity);
        String searchEngineName = BrowserSettings.getInstance().getSearchEngineName();
        if (searchEngineName != null) {
            List availables = ((SearchEngineManager) this.mActivity.getSystemService("search_engine_service")).getAvailables();
            int size = availables.size();
            this.mEntryValues = new String[size];
            this.mEntries = new String[size];
            this.mEntryFavicon = new String[size];
            int i = 0;
            int i2 = -1;
            while (i < size) {
                this.mEntryValues[i] = ((SearchEngine) availables.get(i)).getName();
                this.mEntries[i] = ((SearchEngine) availables.get(i)).getLabel();
                this.mEntryFavicon[i] = ((SearchEngine) availables.get(i)).getFaviconUri();
                int i3 = this.mEntryValues[i].equals(searchEngineName) ? i : i2;
                i++;
                i2 = i3;
            }
            setPreferenceScreen(createPreferenceHierarchy());
            this.mRadioPrefs.get(i2).setChecked(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        editorEdit.putBoolean("syc_search_engine", false);
        editorEdit.commit();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Iterator<RadioPreference> it = this.mRadioPrefs.iterator();
        while (it.hasNext()) {
            it.next().setChecked(false);
        }
        ((RadioPreference) preference).setChecked(true);
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        editorEdit.putString("search_engine", this.mEntryValues[preference.getOrder()]);
        editorEdit.putString("search_engine_favicon", this.mEntryFavicon[preference.getOrder()]);
        editorEdit.commit();
        return true;
    }
}
