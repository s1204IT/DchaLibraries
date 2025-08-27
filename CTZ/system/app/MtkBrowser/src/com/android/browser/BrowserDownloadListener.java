package com.android.browser;

import android.webkit.DownloadListener;

/* loaded from: classes.dex */
public abstract class BrowserDownloadListener implements DownloadListener {
    public abstract void onDownloadStart(String str, String str2, String str3, String str4, String str5, long j);

    @Override // android.webkit.DownloadListener
    public void onDownloadStart(String str, String str2, String str3, String str4, long j) {
        onDownloadStart(str, str2, str3, str4, null, j);
    }
}
