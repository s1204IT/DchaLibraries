package com.android.browser;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
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

public class BrowserPreferencesPage extends PreferenceActivity {
    private List<PreferenceActivity.Header> mHeaders;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setDisplayOptions(4, 4);
    }

    @Override
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
        if (BrowserSettings.getInstance().isDebugEnabled()) {
            PreferenceActivity.Header debug = new PreferenceActivity.Header();
            debug.title = getText(R.string.pref_development_title);
            debug.fragment = DebugPreferencesFragment.class.getName();
            target.add(debug);
        }
        this.mHeaders = target;
    }

    @Override
    public PreferenceActivity.Header onGetInitialHeader() {
        String action = getIntent().getAction();
        if ("android.intent.action.MANAGE_NETWORK_USAGE".equals(action)) {
            String fragName = BandwidthPreferencesFragment.class.getName();
            for (PreferenceActivity.Header h : this.mHeaders) {
                if (fragName.equals(h.fragment)) {
                    return h;
                }
            }
        }
        return super.onGetInitialHeader();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                    return true;
                }
                finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    @Override
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args, int titleRes, int shortTitleRes) {
        Intent intent = super.onBuildStartFragmentIntent(fragmentName, args, titleRes, shortTitleRes);
        String url = getIntent().getStringExtra("currentPage");
        intent.putExtra("currentPage", url);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (AccessibilityPreferencesFragment.class.getName().equals(fragmentName) || AdvancedPreferencesFragment.class.getName().equals(fragmentName) || BandwidthPreferencesFragment.class.getName().equals(fragmentName) || DebugPreferencesFragment.class.getName().equals(fragmentName) || GeneralPreferencesFragment.class.getName().equals(fragmentName) || LabPreferencesFragment.class.getName().equals(fragmentName) || PrivacySecurityPreferencesFragment.class.getName().equals(fragmentName) || WebsiteSettingsFragment.class.getName().equals(fragmentName) || SearchEngineSettings.class.getName().equals(fragmentName)) {
            return true;
        }
        return "com.android.browser.search.SearchEnginePreference".equals(fragmentName);
    }
}
