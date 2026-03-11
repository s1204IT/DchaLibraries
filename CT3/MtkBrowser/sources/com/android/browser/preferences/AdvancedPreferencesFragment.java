package com.android.browser.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebStorage;
import com.android.browser.BrowserActivity;
import com.android.browser.BrowserSettings;
import com.android.browser.Extensions;
import com.android.browser.R;
import com.mediatek.browser.ext.IBrowserFeatureIndexExt;
import com.mediatek.browser.ext.IBrowserMiscExt;
import com.mediatek.browser.ext.IBrowserSettingExt;
import java.util.Map;
import java.util.Set;

public class AdvancedPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private IBrowserSettingExt mBrowserSettingExt = null;
    private IBrowserMiscExt mBrowserMiscExt = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_preferences);
        PreferenceScreen searchEngineSettings = (PreferenceScreen) findPreference("search_engine");
        searchEngineSettings.setFragment(SearchEngineSettings.class.getName());
        PreferenceScreen websiteSettings = (PreferenceScreen) findPreference("website_settings");
        websiteSettings.setFragment(WebsiteSettingsFragment.class.getName());
        Preference e = findPreference("default_text_encoding");
        this.mBrowserSettingExt = Extensions.getSettingPlugin(getActivity());
        this.mBrowserSettingExt.setTextEncodingChoices((ListPreference) e);
        String encoding = getPreferenceScreen().getSharedPreferences().getString("default_text_encoding", "");
        if (encoding != null && encoding.length() != 0 && encoding.equals("auto-detector")) {
            encoding = getString(R.string.pref_default_text_encoding_default);
        }
        e.setSummary(encoding);
        e.setOnPreferenceChangeListener(this);
        findPreference("reset_default_preferences").setOnPreferenceChangeListener(this);
        findPreference("search_engine").setOnPreferenceChangeListener(this);
        Preference e2 = findPreference("plugin_state");
        e2.setOnPreferenceChangeListener(this);
        updateListPreferenceSummary((ListPreference) e2);
        getPreferenceScreen().removePreference(e2);
        this.mBrowserSettingExt.customizePreference(IBrowserFeatureIndexExt.CUSTOM_PREFERENCE_ADVANCED, getPreferenceScreen(), this, BrowserSettings.getInstance().getPreferences(), this);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        CheckBoxPreference cbp = (CheckBoxPreference) findPreference("load_page");
        cbp.setChecked(mPrefs.getBoolean("load_page", true));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.mBrowserMiscExt = Extensions.getMiscPlugin(getActivity());
        this.mBrowserMiscExt.onActivityResult(requestCode, resultCode, data, this);
    }

    void updateListPreferenceSummary(ListPreference e) {
        e.setSummary(e.getEntry());
    }

    @Override
    public void onResume() {
        super.onResume();
        final PreferenceScreen websiteSettings = (PreferenceScreen) findPreference("website_settings");
        websiteSettings.setEnabled(false);
        WebStorage.getInstance().getOrigins(new ValueCallback<Map>() {
            @Override
            public void onReceiveValue(Map webStorageOrigins) {
                if (webStorageOrigins == null || webStorageOrigins.isEmpty()) {
                    return;
                }
                websiteSettings.setEnabled(true);
            }
        });
        GeolocationPermissions.getInstance().getOrigins(new ValueCallback<Set<String>>() {
            @Override
            public void onReceiveValue(Set<String> geolocationOrigins) {
                if (geolocationOrigins == null || geolocationOrigins.isEmpty()) {
                    return;
                }
                websiteSettings.setEnabled(true);
            }
        });
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (getActivity() == null) {
            Log.w("PageContentPreferencesFragment", "onPreferenceChange called from detached fragment!");
            return false;
        }
        if (pref.getKey().equals("default_text_encoding")) {
            String encoding = objValue.toString();
            if (encoding != null && encoding.length() != 0 && encoding.equals("auto-detector")) {
                encoding = getString(R.string.pref_default_text_encoding_default);
            }
            pref.setSummary(encoding);
            return true;
        }
        if (pref.getKey().equals("reset_default_preferences")) {
            Boolean value = (Boolean) objValue;
            if (value.booleanValue()) {
                startActivity(new Intent("--restart--", null, getActivity(), BrowserActivity.class));
                return true;
            }
        } else if (pref.getKey().equals("plugin_state") || pref.getKey().equals("search_engine")) {
            ListPreference lp = (ListPreference) pref;
            lp.setValue((String) objValue);
            updateListPreferenceSummary(lp);
            return false;
        }
        this.mBrowserSettingExt = Extensions.getSettingPlugin(getActivity());
        return this.mBrowserSettingExt.updatePreferenceItem(pref, objValue);
    }
}
