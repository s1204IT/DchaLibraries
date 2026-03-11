package com.android.browser;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.util.Map;

public class PreloadedTabControl {
    private boolean mDestroyed;
    final Tab mTab;

    public PreloadedTabControl(Tab tab) {
        Log.d("PreloadedTabControl", "PreloadedTabControl.<init>");
        this.mTab = tab;
    }

    public void destroy() {
        Log.d("PreloadedTabControl", "PreloadedTabControl.destroy");
        this.mDestroyed = true;
        this.mTab.destroy();
    }

    public Tab getTab() {
        return this.mTab;
    }

    public void loadUrl(String str, Map<String, String> map) {
        Log.d("PreloadedTabControl", "Preloading " + str);
        this.mTab.loadUrl(str, map);
    }

    public void loadUrlIfChanged(String str, Map<String, String> map) {
        String url = this.mTab.getUrl();
        if (!TextUtils.isEmpty(url)) {
            try {
                url = Uri.parse(url).buildUpon().fragment(null).build().toString();
            } catch (UnsupportedOperationException e) {
            }
        }
        Log.d("PreloadedTabControl", "loadUrlIfChanged\nnew: " + str + "\nold: " + url);
        if (TextUtils.equals(str, url)) {
            return;
        }
        loadUrl(str, map);
    }

    public void searchBoxCancel() {
    }

    public boolean searchBoxSubmit(String str, String str2, Map<String, String> map) {
        return false;
    }

    public void setQuery(String str) {
        Log.d("PreloadedTabControl", "Cannot set query: no searchbox interface");
    }
}
