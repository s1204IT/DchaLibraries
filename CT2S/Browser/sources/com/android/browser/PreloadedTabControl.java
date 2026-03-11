package com.android.browser;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.util.Map;

public class PreloadedTabControl {
    private boolean mDestroyed;
    final Tab mTab;

    public PreloadedTabControl(Tab t) {
        Log.d("PreloadedTabControl", "PreloadedTabControl.<init>");
        this.mTab = t;
    }

    public void setQuery(String query) {
        Log.d("PreloadedTabControl", "Cannot set query: no searchbox interface");
    }

    public boolean searchBoxSubmit(String query, String fallbackUrl, Map<String, String> fallbackHeaders) {
        return false;
    }

    public void searchBoxCancel() {
    }

    public void loadUrlIfChanged(String url, Map<String, String> headers) {
        String currentUrl = this.mTab.getUrl();
        if (!TextUtils.isEmpty(currentUrl)) {
            try {
                currentUrl = Uri.parse(currentUrl).buildUpon().fragment(null).build().toString();
            } catch (UnsupportedOperationException e) {
            }
        }
        Log.d("PreloadedTabControl", "loadUrlIfChanged\nnew: " + url + "\nold: " + currentUrl);
        if (!TextUtils.equals(url, currentUrl)) {
            loadUrl(url, headers);
        }
    }

    public void loadUrl(String url, Map<String, String> headers) {
        Log.d("PreloadedTabControl", "Preloading " + url);
        this.mTab.loadUrl(url, headers);
    }

    public void destroy() {
        Log.d("PreloadedTabControl", "PreloadedTabControl.destroy");
        this.mDestroyed = true;
        this.mTab.destroy();
    }

    public Tab getTab() {
        return this.mTab;
    }
}
