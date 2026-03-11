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

    public PreloadController(Context ctx) {
        this.mContext = ctx.getApplicationContext();
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
    public void onSetWebView(Tab tab, WebView view) {
    }

    @Override
    public void createSubWindow(Tab tab) {
    }

    @Override
    public void onPageStarted(Tab tab, WebView view, Bitmap favicon) {
        if (view == null) {
            return;
        }
        view.clearHistory();
    }

    @Override
    public void onPageFinished(Tab tab) {
        WebView view;
        if (tab == null || (view = tab.getWebView()) == null) {
            return;
        }
        view.clearHistory();
    }

    @Override
    public void onProgressChanged(Tab tab) {
    }

    @Override
    public void onReceivedTitle(Tab tab, String title) {
    }

    @Override
    public void onFavicon(Tab tab, WebView view, Bitmap icon) {
    }

    @Override
    public boolean shouldOverrideUrlLoading(Tab tab, WebView view, String url) {
        return false;
    }

    @Override
    public void sendErrorCode(int error, String url) {
    }

    @Override
    public boolean shouldOverrideKeyEvent(KeyEvent event) {
        return false;
    }

    @Override
    public boolean onUnhandledKeyEvent(KeyEvent event) {
        return false;
    }

    @Override
    public void doUpdateVisitedHistory(Tab tab, boolean isReload) {
    }

    @Override
    public void getVisitedHistory(ValueCallback<String[]> callback) {
    }

    @Override
    public void onReceivedHttpAuthRequest(Tab tab, WebView view, HttpAuthHandler handler, String host, String realm) {
    }

    @Override
    public void onDownloadStart(Tab tab, String url, String useragent, String contentDisposition, String mimeType, String referer, long contentLength) {
    }

    @Override
    public void showCustomView(Tab tab, View view, int requestedOrientation, WebChromeClient.CustomViewCallback callback) {
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
    public void showSslCertificateOnError(WebView view, SslErrorHandler handler, SslError error) {
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
    public void showFileChooser(ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
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
    public Tab openTab(String url, Tab parent, boolean setActive, boolean useCurrent) {
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
    public void onShowPopupWindowAttempt(Tab tab, boolean dialog, Message resultMsg) {
    }
}
