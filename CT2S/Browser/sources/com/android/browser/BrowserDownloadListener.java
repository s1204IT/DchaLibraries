package com.android.browser;

import android.webkit.DownloadListener;

public abstract class BrowserDownloadListener implements DownloadListener {
    public abstract void onDownloadStart(String str, String str2, String str3, String str4, String str5, long j);

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        onDownloadStart(url, userAgent, contentDisposition, mimetype, null, contentLength);
    }
}
