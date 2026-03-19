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

public class PreloadController implements WebViewController {
    private Context mContext;

    public PreloadController(Context context) {
        this.mContext = context.getApplicationContext();
    }

    @Override
    public Context getContext() {
        return this.mContext;
    }

    @Override
    public Activity getActivity() {
        return null;
    }

    @Override
    public TabControl getTabControl() {
        return null;
    }

    @Override
    public void onSetWebView(Tab tab, WebView webView) {
    }

    @Override
    public void createSubWindow(Tab tab) {
    }

    @Override
    public void onPageStarted(Tab tab, WebView webView, Bitmap bitmap) {
        if (webView != null) {
            webView.clearHistory();
        }
    }

    @Override
    public void onPageFinished(Tab tab) {
        WebView webView;
        if (tab != null && (webView = tab.getWebView()) != null) {
            webView.clearHistory();
        }
    }

    @Override
    public void onProgressChanged(Tab tab) {
    }

    @Override
    public void onReceivedTitle(Tab tab, String str) {
    }

    @Override
    public void onFavicon(Tab tab, WebView webView, Bitmap bitmap) {
    }

    @Override
    public boolean shouldOverrideUrlLoading(Tab tab, WebView webView, String str) {
        return false;
    }

    @Override
    public void sendErrorCode(int i, String str) {
    }

    @Override
    public boolean shouldOverrideKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean onUnhandledKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void doUpdateVisitedHistory(Tab tab, boolean z) {
    }

    @Override
    public void getVisitedHistory(ValueCallback<String[]> valueCallback) {
    }

    @Override
    public void onReceivedHttpAuthRequest(Tab tab, WebView webView, HttpAuthHandler httpAuthHandler, String str, String str2) {
    }

    @Override
    public void onDownloadStart(Tab tab, String str, String str2, String str3, String str4, String str5, long j) {
    }

    @Override
    public void showCustomView(Tab tab, View view, int i, WebChromeClient.CustomViewCallback customViewCallback) {
    }

    @Override
    public void hideCustomView() {
    }

    @Override
    public Bitmap getDefaultVideoPoster() {
        return null;
    }

    @Override
    public View getVideoLoadingProgressView() {
        return null;
    }

    @Override
    public void showSslCertificateOnError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
    }

    @Override
    public void onUserCanceledSsl(Tab tab) {
    }

    @Override
    public boolean shouldShowErrorConsole() {
        return false;
    }

    @Override
    public void onUpdatedSecurityState(Tab tab) {
    }

    @Override
    public void showFileChooser(ValueCallback<Uri[]> valueCallback, WebChromeClient.FileChooserParams fileChooserParams) {
    }

    @Override
    public void endActionMode() {
    }

    @Override
    public void attachSubWindow(Tab tab) {
    }

    @Override
    public void dismissSubWindow(Tab tab) {
    }

    @Override
    public Tab openTab(String str, Tab tab, boolean z, boolean z2) {
        return null;
    }

    @Override
    public boolean switchToTab(Tab tab) {
        return false;
    }

    @Override
    public void closeTab(Tab tab) {
    }

    @Override
    public void bookmarkedStatusHasChanged(Tab tab) {
    }

    @Override
    public void showAutoLogin(Tab tab) {
    }

    @Override
    public void hideAutoLogin(Tab tab) {
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return false;
    }

    @Override
    public void onShowPopupWindowAttempt(Tab tab, boolean z, Message message) {
    }
}
