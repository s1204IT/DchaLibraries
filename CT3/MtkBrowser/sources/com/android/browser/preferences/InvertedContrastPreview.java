package com.android.browser.preferences;

import android.content.Context;
import android.text.TextUtils;

public class InvertedContrastPreview extends WebViewPreview {
    static final String[] THUMBS = {"thumb_google", "thumb_amazon", "thumb_cnn", "thumb_espn", "", "thumb_bbc", "thumb_nytimes", "thumb_weatherchannel", "thumb_picasa"};
    String mHtml;

    @Override
    protected void init(Context context) {
        super.init(context);
        StringBuilder builder = new StringBuilder("<html><body style=\"width: 1000px\">");
        for (String thumb : THUMBS) {
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
    }
}
