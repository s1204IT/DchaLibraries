package com.android.browser;

import android.webkit.DownloadListener;

public abstract class BrowserDownloadListener implements DownloadListener {
    public abstract void onDownloadStart(String str, String str2, String str3, String str4, String str5, long j);

    @Override
    public void onDownloadStart(String str, String str2, String str3, String str4, long j) {
        onDownloadStart(str, str2, str3, str4, null, j);
    }
}
