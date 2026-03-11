package com.android.browser.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public abstract class WebViewPreview extends Preference implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected WebView mWebView;

    public WebViewPreview(Context context) {
        super(context);
        init(context);
    }

    public WebViewPreview(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public WebViewPreview(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    protected void init(Context context) {
        setLayoutResource(2130968637);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        this.mWebView = (WebView) view.findViewById(2131558559);
        updatePreview(true);
    }

    @Override
    protected View onCreateView(ViewGroup viewGroup) {
        View viewOnCreateView = super.onCreateView(viewGroup);
        WebView webView = (WebView) viewOnCreateView.findViewById(2131558559);
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        webView.setClickable(false);
        webView.setLongClickable(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        setupWebView(webView);
        return viewOnCreateView;
    }

    @Override
    protected void onPrepareForRemoval() {
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPrepareForRemoval();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        updatePreview(false);
    }

    protected void setupWebView(WebView webView) {
    }

    protected abstract void updatePreview(boolean z);
}
