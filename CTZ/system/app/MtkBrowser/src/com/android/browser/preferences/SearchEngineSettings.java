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
import com.android.browser.R;
import com.mediatek.common.search.SearchEngine;
import com.mediatek.search.SearchEngineManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class SearchEngineSettings extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    private PreferenceActivity mActivity;
    private String[] mEntries;
    private String[] mEntryFavicon;
    private String[] mEntryValues;
    private SharedPreferences mPrefs;
    private List<RadioPreference> mRadioPrefs;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
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
            int i = -1;
            for (int i2 = 0; i2 < size; i2++) {
                this.mEntryValues[i2] = ((SearchEngine) availables.get(i2)).getName();
                this.mEntries[i2] = ((SearchEngine) availables.get(i2)).getLabel();
                this.mEntryFavicon[i2] = ((SearchEngine) availables.get(i2)).getFaviconUri();
                if (this.mEntryValues[i2].equals(searchEngineName)) {
                    i = i2;
                }
            }
            setPreferenceScreen(createPreferenceHierarchy());
            this.mRadioPrefs.get(i).setChecked(true);
        }
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        editorEdit.putBoolean("syc_search_engine", false);
        editorEdit.commit();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen preferenceScreenCreatePreferenceScreen = getPreferenceManager().createPreferenceScreen(this.mActivity);
        PreferenceCategory preferenceCategory = new PreferenceCategory(this.mActivity);
        preferenceCategory.setTitle(R.string.pref_content_search_engine);
        preferenceScreenCreatePreferenceScreen.addPreference(preferenceCategory);
        for (int i = 0; i < this.mEntries.length; i++) {
            RadioPreference radioPreference = new RadioPreference(this.mActivity);
            radioPreference.setWidgetLayoutResource(R.layout.radio_preference);
            radioPreference.setTitle(this.mEntries[i]);
            radioPreference.setOrder(i);
            radioPreference.setOnPreferenceClickListener(this);
            preferenceCategory.addPreference(radioPreference);
            this.mRadioPrefs.add(radioPreference);
        }
        return preferenceScreenCreatePreferenceScreen;
    }

    @Override // android.preference.Preference.OnPreferenceClickListener
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
