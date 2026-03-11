package com.android.browser.preferences;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

public class InvertedContrastPreview extends WebViewPreview {
    static final String[] THUMBS = {"thumb_google", "thumb_amazon", "thumb_cnn", "thumb_espn", "", "thumb_bbc", "thumb_nytimes", "thumb_weatherchannel", "thumb_picasa"};
    String mHtml;

    public InvertedContrastPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public InvertedContrastPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InvertedContrastPreview(Context context) {
        super(context);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        StringBuilder builder = new StringBuilder("<html><body style=\"width: 1000px\">");
        String[] arr$ = THUMBS;
        for (String thumb : arr$) {
            if (TextUtils.isEmpty(thumb)) {
                builder.append("<br />");
            } else {
                builder.append("<img src=\"");
                builder.append("content://com.android.browser.home/res/raw/");
                builder.append(thumb);
                builder.append("\" />&nbsp;");
            }
        }
        builder.append("</body></html>");
        this.mHtml = builder.toString();
    }

    @Override
    protected void updatePreview(boolean forceReload) {
        if (this.mWebView != null && forceReload) {
            this.mWebView.loadData(this.mHtml, "text/html", null);
        }
    }
}
