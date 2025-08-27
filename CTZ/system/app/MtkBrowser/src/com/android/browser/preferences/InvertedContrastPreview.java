package com.android.browser.preferences;

import android.content.Context;
import android.text.TextUtils;

/* loaded from: classes.dex */
public class InvertedContrastPreview extends WebViewPreview {
    static final String[] THUMBS = {"thumb_google", "thumb_amazon", "thumb_cnn", "thumb_espn", "", "thumb_bbc", "thumb_nytimes", "thumb_weatherchannel", "thumb_picasa"};
    String mHtml;

    @Override // com.android.browser.preferences.WebViewPreview
    protected void init(Context context) {
        super.init(context);
        StringBuilder sb = new StringBuilder("<html><body style=\"width: 1000px\">");
        for (String str : THUMBS) {
            if (TextUtils.isEmpty(str)) {
                sb.append("<br />");
            } else {
                sb.append("<img src=\"");
                sb.append("content://com.android.browser.home/res/raw/");
                sb.append(str);
                sb.append("\" />&nbsp;");
            }
        }
        sb.append("</body></html>");
        this.mHtml = sb.toString();
    }

    @Override // com.android.browser.preferences.WebViewPreview
    protected void updatePreview(boolean z) {
    }
}
