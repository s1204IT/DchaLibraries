package com.android.browser.preferences;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.webkit.WebView;
import com.android.browser.BrowserSettings;
import com.android.browser.Extensions;
import com.android.browser.R;
import com.mediatek.browser.ext.IBrowserFeatureIndexExt;
import com.mediatek.browser.ext.IBrowserSettingExt;
import java.text.NumberFormat;

public class AccessibilityPreferencesFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private IBrowserSettingExt mBrowserSettingExt = null;
    WebView mControlWebView;
    NumberFormat mFormat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mControlWebView = new WebView(getActivity());
        addPreferencesFromResource(R.xml.accessibility_preferences);
        BrowserSettings settings = BrowserSettings.getInstance();
        this.mFormat = NumberFormat.getPercentInstance();
        Preference e = findPreference("min_font_size");
        e.setOnPreferenceChangeListener(this);
        updateMinFontSummary(e, settings.getMinimumFontSize());
        Preference e2 = findPreference("text_zoom");
        e2.setOnPreferenceChangeListener(this);
        updateTextZoomSummary(e2, settings.getTextZoom());
        Preference e3 = findPreference("double_tap_zoom");
        e3.setOnPreferenceChangeListener(this);
        updateDoubleTapZoomSummary(e3, settings.getDoubleTapZoom());
        this.mBrowserSettingExt = Extensions.getSettingPlugin(getActivity());
        this.mBrowserSettingExt.customizePreference(IBrowserFeatureIndexExt.CUSTOM_PREFERENCE_ACCESSIBILITY, getPreferenceScreen(), this, settings.getPreferences(), this);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mControlWebView.resumeTimers();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mControlWebView.pauseTimers();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mControlWebView.destroy();
        this.mControlWebView = null;
    }

    void updateMinFontSummary(Preference pref, int minFontSize) {
        Context c = getActivity();
        pref.setSummary(c.getString(R.string.pref_min_font_size_value, Integer.valueOf(minFontSize)));
    }

    void updateTextZoomSummary(Preference pref, int textZoom) {
        pref.setSummary(this.mFormat.format(((double) textZoom) / 100.0d));
    }

    void updateDoubleTapZoomSummary(Preference pref, int doubleTapZoom) {
        pref.setSummary(this.mFormat.format(((double) doubleTapZoom) / 100.0d));
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (getActivity() == null) {
            return false;
        }
        if ("min_font_size".equals(pref.getKey())) {
            updateMinFontSummary(pref, BrowserSettings.getAdjustedMinimumFontSize(((Integer) objValue).intValue()));
        }
        if ("text_zoom".equals(pref.getKey())) {
            BrowserSettings settings = BrowserSettings.getInstance();
            updateTextZoomSummary(pref, settings.getAdjustedTextZoom(((Integer) objValue).intValue()));
        }
        if ("double_tap_zoom".equals(pref.getKey())) {
            BrowserSettings settings2 = BrowserSettings.getInstance();
            updateDoubleTapZoomSummary(pref, settings2.getAdjustedDoubleTapZoom(((Integer) objValue).intValue()));
        }
        this.mBrowserSettingExt = Extensions.getSettingPlugin(getActivity());
        this.mBrowserSettingExt.updatePreferenceItem(pref, objValue);
        return true;
    }
}
