package com.android.browser.preferences;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebStorage;
import com.android.browser.BrowserActivity;
import com.android.browser.R;
import java.util.Map;
import java.util.Set;

public class AdvancedPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_preferences);
        PreferenceScreen websiteSettings = (PreferenceScreen) findPreference("website_settings");
        websiteSettings.setFragment(WebsiteSettingsFragment.class.getName());
        Preference e = findPreference("default_zoom");
        e.setOnPreferenceChangeListener(this);
        e.setSummary(getVisualDefaultZoomName(getPreferenceScreen().getSharedPreferences().getString("default_zoom", null)));
        findPreference("default_text_encoding").setOnPreferenceChangeListener(this);
        findPreference("reset_default_preferences").setOnPreferenceChangeListener(this);
        Preference e2 = findPreference("search_engine");
        e2.setOnPreferenceChangeListener(this);
        updateListPreferenceSummary((ListPreference) e2);
        Preference e3 = findPreference("plugin_state");
        e3.setOnPreferenceChangeListener(this);
        updateListPreferenceSummary((ListPreference) e3);
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
                if (webStorageOrigins != null && !webStorageOrigins.isEmpty()) {
                    websiteSettings.setEnabled(true);
                }
            }
        });
        GeolocationPermissions.getInstance().getOrigins(new ValueCallback<Set<String>>() {
            @Override
            public void onReceiveValue(Set<String> geolocationOrigins) {
                if (geolocationOrigins != null && !geolocationOrigins.isEmpty()) {
                    websiteSettings.setEnabled(true);
                }
            }
        });
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (getActivity() == null) {
            Log.w("PageContentPreferencesFragment", "onPreferenceChange called from detached fragment!");
            return false;
        }
        if (pref.getKey().equals("default_zoom")) {
            pref.setSummary(getVisualDefaultZoomName((String) objValue));
            return true;
        }
        if (pref.getKey().equals("default_text_encoding")) {
            pref.setSummary((String) objValue);
            return true;
        }
        if (pref.getKey().equals("reset_default_preferences")) {
            Boolean value = (Boolean) objValue;
            if (!value.booleanValue()) {
                return false;
            }
            startActivity(new Intent("--restart--", null, getActivity(), BrowserActivity.class));
            return true;
        }
        if (!pref.getKey().equals("plugin_state") && !pref.getKey().equals("search_engine")) {
            return false;
        }
        ListPreference lp = (ListPreference) pref;
        lp.setValue((String) objValue);
        updateListPreferenceSummary(lp);
        return false;
    }

    private CharSequence getVisualDefaultZoomName(String enumName) {
        Resources res = getActivity().getResources();
        CharSequence[] visualNames = res.getTextArray(R.array.pref_default_zoom_choices);
        CharSequence[] enumNames = res.getTextArray(R.array.pref_default_zoom_values);
        if (visualNames.length != enumNames.length) {
            return "";
        }
        int length = enumNames.length;
        for (int i = 0; i < length; i++) {
            if (enumNames[i].equals(enumName)) {
                return visualNames[i];
            }
        }
        return "";
    }
}
