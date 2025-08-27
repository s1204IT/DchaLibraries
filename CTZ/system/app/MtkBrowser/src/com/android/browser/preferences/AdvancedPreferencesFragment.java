package com.android.browser.preferences;

import android.content.Intent;
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
import com.mediatek.browser.ext.IBrowserMiscExt;
import com.mediatek.browser.ext.IBrowserSettingExt;
import java.util.Map;
import java.util.Set;

/* loaded from: classes.dex */
public class AdvancedPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private IBrowserSettingExt mBrowserSettingExt = null;
    private IBrowserMiscExt mBrowserMiscExt = null;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.advanced_preferences);
        ((PreferenceScreen) findPreference("search_engine")).setFragment(SearchEngineSettings.class.getName());
        ((PreferenceScreen) findPreference("website_settings")).setFragment(WebsiteSettingsFragment.class.getName());
        this.mBrowserSettingExt = Extensions.getSettingPlugin(getActivity());
        findPreference("reset_default_preferences").setOnPreferenceChangeListener(this);
        findPreference("search_engine").setOnPreferenceChangeListener(this);
        Preference preferenceFindPreference = findPreference("plugin_state");
        preferenceFindPreference.setOnPreferenceChangeListener(this);
        updateListPreferenceSummary((ListPreference) preferenceFindPreference);
        getPreferenceScreen().removePreference(preferenceFindPreference);
        this.mBrowserSettingExt.customizePreference(140, getPreferenceScreen(), this, BrowserSettings.getInstance().getPreferences(), this);
        ((CheckBoxPreference) findPreference("load_page")).setChecked(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("load_page", true));
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        this.mBrowserMiscExt = Extensions.getMiscPlugin(getActivity());
        this.mBrowserMiscExt.onActivityResult(i, i2, intent, this);
    }

    void updateListPreferenceSummary(ListPreference listPreference) {
        listPreference.setSummary(listPreference.getEntry());
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        final PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("website_settings");
        preferenceScreen.setEnabled(false);
        WebStorage.getInstance().getOrigins(new ValueCallback<Map>() { // from class: com.android.browser.preferences.AdvancedPreferencesFragment.1
            /* JADX DEBUG: Method merged with bridge method: onReceiveValue(Ljava/lang/Object;)V */
            @Override // android.webkit.ValueCallback
            public void onReceiveValue(Map map) {
                if (map != null && !map.isEmpty()) {
                    preferenceScreen.setEnabled(true);
                }
            }
        });
        GeolocationPermissions.getInstance().getOrigins(new ValueCallback<Set<String>>() { // from class: com.android.browser.preferences.AdvancedPreferencesFragment.2
            /* JADX DEBUG: Method merged with bridge method: onReceiveValue(Ljava/lang/Object;)V */
            @Override // android.webkit.ValueCallback
            public void onReceiveValue(Set<String> set) {
                if (set != null && !set.isEmpty()) {
                    preferenceScreen.setEnabled(true);
                }
            }
        });
    }

    @Override // android.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (getActivity() == null) {
            Log.w("PageContentPreferences", "onPreferenceChange called from detached fragment!");
            return false;
        }
        if (preference.getKey().equals("reset_default_preferences")) {
            if (((Boolean) obj).booleanValue()) {
                startActivity(new Intent("--restart--", null, getActivity(), BrowserActivity.class));
                return true;
            }
        } else if (preference.getKey().equals("plugin_state") || preference.getKey().equals("search_engine")) {
            ListPreference listPreference = (ListPreference) preference;
            listPreference.setValue((String) obj);
            updateListPreferenceSummary(listPreference);
            return false;
        }
        this.mBrowserSettingExt = Extensions.getSettingPlugin(getActivity());
        return this.mBrowserSettingExt.updatePreferenceItem(preference, obj);
    }
}
