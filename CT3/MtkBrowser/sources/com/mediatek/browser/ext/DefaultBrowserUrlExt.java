package com.mediatek.browser.ext;

import android.content.Context;
import android.text.InputFilter;
import android.util.Log;

public class DefaultBrowserUrlExt implements IBrowserUrlExt {
    private static final String TAG = "DefaultBrowserUrlExt";

    @Override
    public InputFilter[] checkUrlLengthLimit(Context context) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: checkUrlLengthLimit --default implement");
        return null;
    }

    @Override
    public String checkAndTrimUrl(String url) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: checkAndTrimUrl --default implement");
        return url;
    }

    @Override
    public String getNavigationBarTitle(String title, String url) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: getNavigationBarTitle --default implement");
        return url;
    }

    @Override
    public String getOverrideFocusContent(boolean hasFocus, String newContent, String oldContent, String url) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: getOverrideFocusContent --default implement");
        if (hasFocus && !newContent.equals(oldContent)) {
            return oldContent;
        }
        return null;
    }

    @Override
    public String getOverrideFocusTitle(String title, String content) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: getOverrideFocusTitle --default implement");
        return content;
    }

    @Override
    public boolean redirectCustomerUrl(String url) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: redirectCustomerUrl --default implement");
        return false;
    }
}
