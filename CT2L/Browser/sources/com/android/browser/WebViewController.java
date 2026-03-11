package com.android.browser;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public interface WebViewController {
    void attachSubWindow(Tab tab);

    void bookmarkedStatusHasChanged(Tab tab);

    void closeTab(Tab tab);

    void createSubWindow(Tab tab);

    void dismissSubWindow(Tab tab);

    void doUpdateVisitedHistory(Tab tab, boolean z);

    void endActionMode();

    Activity getActivity();

    Context getContext();

    Bitmap getDefaultVideoPoster();

    TabControl getTabControl();

    View getVideoLoadingProgressView();

    void getVisitedHistory(ValueCallback<String[]> valueCallback);

    void hideAutoLogin(Tab tab);

    void hideCustomView();

    void onDownloadStart(Tab tab, String str, String str2, String str3, String str4, String str5, long j);

    void onFavicon(Tab tab, WebView webView, Bitmap bitmap);

    void onPageFinished(Tab tab);

    void onPageStarted(Tab tab, WebView webView, Bitmap bitmap);

    void onProgressChanged(Tab tab);

    void onReceivedHttpAuthRequest(Tab tab, WebView webView, HttpAuthHandler httpAuthHandler, String str, String str2);

    void onReceivedTitle(Tab tab, String str);

    void onSetWebView(Tab tab, WebView webView);

    boolean onUnhandledKeyEvent(KeyEvent keyEvent);

    void onUpdatedSecurityState(Tab tab);

    void onUserCanceledSsl(Tab tab);

    Tab openTab(String str, Tab tab, boolean z, boolean z2);

    boolean shouldCaptureThumbnails();

    boolean shouldOverrideKeyEvent(KeyEvent keyEvent);

    boolean shouldOverrideUrlLoading(Tab tab, WebView webView, String str);

    boolean shouldShowErrorConsole();

    void showAutoLogin(Tab tab);

    void showCustomView(Tab tab, View view, int i, WebChromeClient.CustomViewCallback customViewCallback);

    void showFileChooser(ValueCallback<Uri[]> valueCallback, WebChromeClient.FileChooserParams fileChooserParams);

    void showSslCertificateOnError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError);

    boolean switchToTab(Tab tab);
}
