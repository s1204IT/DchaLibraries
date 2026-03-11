package com.mediatek.browser.ext;

import android.util.Log;

public class DefaultBrowserSiteNavigationExt implements IBrowserSiteNavigationExt {
    @Override
    public CharSequence[] getPredefinedWebsites() {
        Log.i("@M_DefaultBrowserSiteNavigationExt", "Enter: getPredefinedWebsites --default implement");
        return null;
    }

    @Override
    public int getSiteNavigationCount() {
        Log.i("@M_DefaultBrowserSiteNavigationExt", "Enter: getSiteNavigationCount --default implement");
        return 0;
    }
}
