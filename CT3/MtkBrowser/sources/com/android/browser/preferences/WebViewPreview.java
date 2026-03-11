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

public abstract class WebViewPreview extends Preference implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected WebView mWebView;

    protected abstract void updatePreview(boolean z);

    public WebViewPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public WebViewPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WebViewPreview(Context context) {
        super(context);
        init(context);
    }

    protected void init(Context context) {
        setLayoutResource(R.layout.webview_preview);
    }

    protected void setupWebView(WebView view) {
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View root = super.onCreateView(parent);
        WebView webView = (WebView) root.findViewById(R.id.webview);
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        webView.setClickable(false);
        webView.setLongClickable(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        setupWebView(webView);
        return root;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        this.mWebView = (WebView) view.findViewById(R.id.webview);
        updatePreview(true);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPrepareForRemoval() {
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPrepareForRemoval();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreview(false);
    }
}
