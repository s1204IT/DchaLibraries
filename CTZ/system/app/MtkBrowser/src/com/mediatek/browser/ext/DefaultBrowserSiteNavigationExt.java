package com.mediatek.browser.ext;

import android.util.Log;

/* loaded from: classes.dex */
public class DefaultBrowserSiteNavigationExt implements IBrowserSiteNavigationExt {
    @Override // com.mediatek.browser.ext.IBrowserSiteNavigationExt
    public CharSequence[] getPredefinedWebsites() {
        Log.i("@M_DefaultBrowserSiteNavigationExt", "Enter: getPredefinedWebsites --default implement");
        return null;
    }

    @Override // com.mediatek.browser.ext.IBrowserSiteNavigationExt
    public int getSiteNavigationCount() {
        Log.i("@M_DefaultBrowserSiteNavigationExt", "Enter: getSiteNavigationCount --default implement");
        return 0;
    }
}
