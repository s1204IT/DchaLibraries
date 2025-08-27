package com.android.browser.preferences;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.webkit.WebView;
import com.android.browser.BrowserSettings;
import com.android.browser.Extensions;
import com.android.browser.R;
import com.mediatek.browser.ext.IBrowserSettingExt;
import java.text.NumberFormat;

/* loaded from: classes.dex */
public class AccessibilityPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private IBrowserSettingExt mBrowserSettingExt = null;
    WebView mControlWebView;
    NumberFormat mFormat;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mControlWebView = new WebView(getActivity());
        addPreferencesFromResource(R.xml.accessibility_preferences);
        BrowserSettings browserSettings = BrowserSettings.getInstance();
        this.mFormat = NumberFormat.getPercentInstance();
        Preference preferenceFindPreference = findPreference("min_font_size");
        preferenceFindPreference.setOnPreferenceChangeListener(this);
        updateMinFontSummary(preferenceFindPreference, browserSettings.getMinimumFontSize());
        Preference preferenceFindPreference2 = findPreference("text_zoom");
        preferenceFindPreference2.setOnPreferenceChangeListener(this);
        updateTextZoomSummary(preferenceFindPreference2, browserSettings.getTextZoom());
        Preference preferenceFindPreference3 = findPreference("double_tap_zoom");
        preferenceFindPreference3.setOnPreferenceChangeListener(this);
        updateDoubleTapZoomSummary(preferenceFindPreference3, browserSettings.getDoubleTapZoom());
        this.mBrowserSettingExt = Extensions.getSettingPlugin(getActivity());
        this.mBrowserSettingExt.customizePreference(130, getPreferenceScreen(), this, browserSettings.getPreferences(), this);
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mControlWebView.resumeTimers();
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mControlWebView.pauseTimers();
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        this.mControlWebView.destroy();
        this.mControlWebView = null;
    }

    void updateMinFontSummary(Preference preference, int i) {
        preference.setSummary(getActivity().getString(R.string.pref_min_font_size_value, Integer.valueOf(i)));
    }

    void updateTextZoomSummary(Preference preference, int i) {
        preference.setSummary(this.mFormat.format(i / 100.0d));
    }

    void updateDoubleTapZoomSummary(Preference preference, int i) {
        preference.setSummary(this.mFormat.format(i / 100.0d));
    }

    @Override // android.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (getActivity() == null) {
            return false;
        }
        if ("min_font_size".equals(preference.getKey())) {
            updateMinFontSummary(preference, BrowserSettings.getAdjustedMinimumFontSize(((Integer) obj).intValue()));
        }
        if ("text_zoom".equals(preference.getKey())) {
            updateTextZoomSummary(preference, BrowserSettings.getInstance().getAdjustedTextZoom(((Integer) obj).intValue()));
        }
        if ("double_tap_zoom".equals(preference.getKey())) {
            updateDoubleTapZoomSummary(preference, BrowserSettings.getInstance().getAdjustedDoubleTapZoom(((Integer) obj).intValue()));
        }
        this.mBrowserSettingExt = Extensions.getSettingPlugin(getActivity());
        this.mBrowserSettingExt.updatePreferenceItem(preference, obj);
        return true;
    }
}
