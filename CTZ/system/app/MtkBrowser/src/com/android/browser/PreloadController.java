package com.android.browser;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/* loaded from: classes.dex */
public class PreloadController implements WebViewController {
    private Context mContext;

    public PreloadController(Context context) {
        this.mContext = context.getApplicationContext();
    }

    @Override // com.android.browser.WebViewController
    public Context getContext() {
        return this.mContext;
    }

    @Override // com.android.browser.WebViewController
    public Activity getActivity() {
        return null;
    }

    @Override // com.android.browser.WebViewController
    public TabControl getTabControl() {
        return null;
    }

    @Override // com.android.browser.WebViewController
    public void onSetWebView(Tab tab, WebView webView) {
    }

    @Override // com.android.browser.WebViewController
    public void createSubWindow(Tab tab) {
    }

    @Override // com.android.browser.WebViewController
    public void onPageStarted(Tab tab, WebView webView, Bitmap bitmap) {
        if (webView != null) {
            webView.clearHistory();
        }
    }

    @Override // com.android.browser.WebViewController
    public void onPageFinished(Tab tab) {
        WebView webView;
        if (tab != null && (webView = tab.getWebView()) != null) {
            webView.clearHistory();
        }
    }

    @Override // com.android.browser.WebViewController
    public void onProgressChanged(Tab tab) {
    }

    @Override // com.android.browser.WebViewController
    public void onReceivedTitle(Tab tab, String str) {
    }

    @Override // com.android.browser.WebViewController
    public void onFavicon(Tab tab, WebView webView, Bitmap bitmap) {
    }

    @Override // com.android.browser.WebViewController
    public boolean shouldOverrideUrlLoading(Tab tab, WebView webView, String str) {
        return false;
    }

    @Override // com.android.browser.WebViewController
    public void sendErrorCode(int i, String str) {
    }

    @Override // com.android.browser.WebViewController
    public boolean shouldOverrideKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override // com.android.browser.WebViewController
    public boolean onUnhandledKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override // com.android.browser.WebViewController
    public void doUpdateVisitedHistory(Tab tab, boolean z) {
    }

    @Override // com.android.browser.WebViewController
    public void getVisitedHistory(ValueCallback<String[]> valueCallback) {
    }

    @Override // com.android.browser.WebViewController
    public void onReceivedHttpAuthRequest(Tab tab, WebView webView, HttpAuthHandler httpAuthHandler, String str, String str2) {
    }

    @Override // com.android.browser.WebViewController
    public void onDownloadStart(Tab tab, String str, String str2, String str3, String str4, String str5, long j) {
    }

    @Override // com.android.browser.WebViewController
    public void showCustomView(Tab tab, View view, int i, WebChromeClient.CustomViewCallback customViewCallback) {
    }

    @Override // com.android.browser.WebViewController
    public void hideCustomView() {
    }

    @Override // com.android.browser.WebViewController
    public Bitmap getDefaultVideoPoster() {
        return null;
    }

    @Override // com.android.browser.WebViewController
    public View getVideoLoadingProgressView() {
        return null;
    }

    @Override // com.android.browser.WebViewController
    public void showSslCertificateOnError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
    }

    @Override // com.android.browser.WebViewController
    public void onUserCanceledSsl(Tab tab) {
    }

    @Override // com.android.browser.WebViewController
    public boolean shouldShowErrorConsole() {
        return false;
    }

    @Override // com.android.browser.WebViewController
    public void onUpdatedSecurityState(Tab tab) {
    }

    @Override // com.android.browser.WebViewController
    public void showFileChooser(ValueCallback<Uri[]> valueCallback, WebChromeClient.FileChooserParams fileChooserParams) {
    }

    @Override // com.android.browser.WebViewController
    public void endActionMode() {
    }

    @Override // com.android.browser.WebViewController
    public void attachSubWindow(Tab tab) {
    }

    @Override // com.android.browser.WebViewController
    public void dismissSubWindow(Tab tab) {
    }

    @Override // com.android.browser.WebViewController
    public Tab openTab(String str, Tab tab, boolean z, boolean z2) {
        return null;
    }

    @Override // com.android.browser.WebViewController
    public boolean switchToTab(Tab tab) {
        return false;
    }

    @Override // com.android.browser.WebViewController
    public void closeTab(Tab tab) {
    }

    @Override // com.android.browser.WebViewController
    public void bookmarkedStatusHasChanged(Tab tab) {
    }

    @Override // com.android.browser.WebViewController
    public void showAutoLogin(Tab tab) {
    }

    @Override // com.android.browser.WebViewController
    public void hideAutoLogin(Tab tab) {
    }

    @Override // com.android.browser.WebViewController
    public boolean shouldCaptureThumbnails() {
        return false;
    }

    @Override // com.android.browser.WebViewController
    public void onShowPopupWindowAttempt(Tab tab, boolean z, Message message) {
    }
}
