package com.android.browser;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.android.browser.preferences.AccessibilityPreferencesFragment;
import com.android.browser.preferences.AdvancedPreferencesFragment;
import com.android.browser.preferences.BandwidthPreferencesFragment;
import com.android.browser.preferences.DebugPreferencesFragment;
import com.android.browser.preferences.GeneralPreferencesFragment;
import com.android.browser.preferences.LabPreferencesFragment;
import com.android.browser.preferences.PrivacySecurityPreferencesFragment;
import com.android.browser.preferences.SearchEngineSettings;
import com.android.browser.preferences.WebsiteSettingsFragment;
import java.util.List;

/* loaded from: classes.dex */
public class BrowserPreferencesPage extends PreferenceActivity {
    private List<PreferenceActivity.Header> mHeaders;

    @Override // android.preference.PreferenceActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(4, 4);
        }
    }

    @Override // android.preference.PreferenceActivity
    public void onBuildHeaders(List<PreferenceActivity.Header> list) {
        loadHeadersFromResource(R.xml.preference_headers, list);
        if (BrowserSettings.getInstance().isDebugEnabled()) {
            PreferenceActivity.Header header = new PreferenceActivity.Header();
            header.title = getText(R.string.pref_development_title);
            header.fragment = DebugPreferencesFragment.class.getName();
            list.add(header);
        }
        this.mHeaders = list;
    }

    @Override // android.preference.PreferenceActivity
    public PreferenceActivity.Header onGetInitialHeader() {
        if ("android.intent.action.MANAGE_NETWORK_USAGE".equals(getIntent().getAction())) {
            String name = BandwidthPreferencesFragment.class.getName();
            for (PreferenceActivity.Header header : this.mHeaders) {
                if (name.equals(header.fragment)) {
                    return header;
                }
            }
        }
        return super.onGetInitialHeader();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean onSearchRequested() {
        return false;
    }

    @Override // android.preference.PreferenceActivity
    public Intent onBuildStartFragmentIntent(String str, Bundle bundle, int i, int i2) {
        Intent intentOnBuildStartFragmentIntent = super.onBuildStartFragmentIntent(str, bundle, i, i2);
        intentOnBuildStartFragmentIntent.putExtra("currentPage", getIntent().getStringExtra("currentPage"));
        return intentOnBuildStartFragmentIntent;
    }

    @Override // android.preference.PreferenceActivity
    protected boolean isValidFragment(String str) {
        return AccessibilityPreferencesFragment.class.getName().equals(str) || AdvancedPreferencesFragment.class.getName().equals(str) || BandwidthPreferencesFragment.class.getName().equals(str) || DebugPreferencesFragment.class.getName().equals(str) || GeneralPreferencesFragment.class.getName().equals(str) || LabPreferencesFragment.class.getName().equals(str) || PrivacySecurityPreferencesFragment.class.getName().equals(str) || WebsiteSettingsFragment.class.getName().equals(str) || SearchEngineSettings.class.getName().equals(str) || "com.android.browser.search.SearchEnginePreference".equals(str);
    }
}
