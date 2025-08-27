package com.mediatek.browser.ext;

import android.content.Context;
import android.text.InputFilter;
import android.util.Log;

/* loaded from: classes.dex */
public class DefaultBrowserUrlExt implements IBrowserUrlExt {
    @Override // com.mediatek.browser.ext.IBrowserUrlExt
    public InputFilter[] checkUrlLengthLimit(Context context) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: checkUrlLengthLimit --default implement");
        return null;
    }

    @Override // com.mediatek.browser.ext.IBrowserUrlExt
    public String checkAndTrimUrl(String str) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: checkAndTrimUrl --default implement");
        return str;
    }

    @Override // com.mediatek.browser.ext.IBrowserUrlExt
    public String getNavigationBarTitle(String str, String str2) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: getNavigationBarTitle --default implement");
        return str2;
    }

    @Override // com.mediatek.browser.ext.IBrowserUrlExt
    public String getOverrideFocusContent(boolean z, String str, String str2, String str3) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: getOverrideFocusContent --default implement");
        if (z && !str.equals(str2)) {
            return str2;
        }
        return null;
    }

    @Override // com.mediatek.browser.ext.IBrowserUrlExt
    public String getOverrideFocusTitle(String str, String str2) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: getOverrideFocusTitle --default implement");
        return str2;
    }

    @Override // com.mediatek.browser.ext.IBrowserUrlExt
    public boolean redirectCustomerUrl(String str) {
        Log.i("@M_DefaultBrowserUrlExt", "Enter: redirectCustomerUrl --default implement");
        return false;
    }
}
