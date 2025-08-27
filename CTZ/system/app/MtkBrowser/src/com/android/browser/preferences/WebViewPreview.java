package com.android.browser.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import com.android.browser.R;

/* loaded from: classes.dex */
public abstract class WebViewPreview extends Preference implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected WebView mWebView;

    protected abstract void updatePreview(boolean z);

    public WebViewPreview(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    public WebViewPreview(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public WebViewPreview(Context context) {
        super(context);
        init(context);
    }

    protected void init(Context context) {
        setLayoutResource(R.layout.webview_preview);
    }

    protected void setupWebView(WebView webView) {
    }

    @Override // android.preference.Preference
    protected View onCreateView(ViewGroup viewGroup) {
        View viewOnCreateView = super.onCreateView(viewGroup);
        WebView webView = (WebView) viewOnCreateView.findViewById(R.id.webview);
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        webView.setClickable(false);
        webView.setLongClickable(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        setupWebView(webView);
        return viewOnCreateView;
    }

    @Override // android.preference.Preference
    protected void onBindView(View view) {
        super.onBindView(view);
        this.mWebView = (WebView) view.findViewById(R.id.webview);
        updatePreview(true);
    }

    @Override // android.preference.Preference
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override // android.preference.Preference
    protected void onPrepareForRemoval() {
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPrepareForRemoval();
    }

    @Override // android.content.SharedPreferences.OnSharedPreferenceChangeListener
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        updatePreview(false);
    }
}
