package com.android.browser.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.android.browser.BrowserSettings;
import com.android.browser.R;

public class FontSizePreview extends WebViewPreview {
    String mHtml;

    public FontSizePreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public FontSizePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FontSizePreview(Context context) {
        super(context);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        Resources res = context.getResources();
        Object[] visualNames = res.getStringArray(R.array.pref_text_size_choices);
        this.mHtml = String.format("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><style type=\"text/css\">p { margin: 2px auto;}</style><body><p style=\"font-size: 4pt\">%s</p><p style=\"font-size: 8pt\">%s</p><p style=\"font-size: 10pt\">%s</p><p style=\"font-size: 14pt\">%s</p><p style=\"font-size: 18pt\">%s</p></body></html>", visualNames);
    }

    @Override
    protected void updatePreview(boolean forceReload) {
        if (this.mWebView != null) {
            WebSettings ws = this.mWebView.getSettings();
            BrowserSettings bs = BrowserSettings.getInstance();
            ws.setMinimumFontSize(bs.getMinimumFontSize());
            ws.setTextZoom(bs.getTextZoom());
            this.mWebView.loadDataWithBaseURL(null, this.mHtml, "text/html", "utf-8", null);
        }
    }

    @Override
    protected void setupWebView(WebView view) {
        super.setupWebView(view);
        view.setLayerType(1, null);
    }
}
