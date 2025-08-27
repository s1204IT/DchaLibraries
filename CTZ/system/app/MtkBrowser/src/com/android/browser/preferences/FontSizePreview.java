package com.android.browser.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.android.browser.BrowserSettings;
import com.android.browser.Extensions;
import com.android.browser.R;

/* loaded from: classes.dex */
public class FontSizePreview extends WebViewPreview {
    private Context mContext;
    String mHtml;

    public FontSizePreview(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = null;
    }

    public FontSizePreview(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = null;
    }

    public FontSizePreview(Context context) {
        super(context);
        this.mContext = null;
    }

    @Override // com.android.browser.preferences.WebViewPreview
    protected void init(Context context) {
        super.init(context);
        this.mContext = context;
        this.mHtml = String.format("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\"><style type=\"text/css\">p { margin: 2px auto;}</style><body><p style=\"font-size: 4pt\">%s</p><p style=\"font-size: 8pt\">%s</p><p style=\"font-size: 10pt\">%s</p><p style=\"font-size: 14pt\">%s</p><p style=\"font-size: 18pt\">%s</p></body></html>", context.getResources().getStringArray(R.array.pref_text_size_choices));
    }

    @Override // com.android.browser.preferences.WebViewPreview
    protected void updatePreview(boolean z) {
        if (this.mWebView == null) {
            return;
        }
        WebSettings settings = this.mWebView.getSettings();
        BrowserSettings browserSettings = BrowserSettings.getInstance();
        settings.setMinimumFontSize(browserSettings.getMinimumFontSize());
        settings.setTextZoom(browserSettings.getTextZoom());
        Extensions.getSettingPlugin(this.mContext).setStandardFontFamily(settings, browserSettings.getPreferences());
        this.mWebView.loadDataWithBaseURL(null, this.mHtml, "text/html", "utf-8", null);
    }

    @Override // com.android.browser.preferences.WebViewPreview
    protected void setupWebView(WebView webView) {
        super.setupWebView(webView);
        webView.setLayerType(1, null);
    }
}
