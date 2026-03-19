package com.android.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.webkit.ClientCertRequest;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.android.browser.DataController;
import com.android.browser.TabControl;
import com.android.browser.homepages.HomeProvider;
import com.android.browser.provider.SnapshotProvider;
import com.android.browser.sitenavigation.SiteNavigation;
import com.android.browser.util.MimeTypeMap;
import com.mediatek.browser.ext.IBrowserUrlExt;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

class Tab implements WebView.PictureListener {
    private static final boolean DEBUG = Browser.DEBUG;
    private static Paint sAlphaPaint = new Paint();
    private static Bitmap sDefaultFavicon;
    private SaveCallback callback;
    private String mAppId;
    private IBrowserUrlExt mBrowserUrlExt;
    private Bitmap mCapture;
    private int mCaptureHeight;
    private int mCaptureWidth;
    private Vector<Tab> mChildren;
    private boolean mCloseOnBack;
    private View mContainer;
    Context mContext;
    protected PageState mCurrentState;
    private DataController mDataController;
    private DeviceAccountLogin mDeviceAccountLogin;
    private DialogInterface.OnDismissListener mDialogListener;
    private boolean mDisableOverrideUrlLoading;
    private final BrowserDownloadListener mDownloadListener;
    private ErrorConsoleView mErrorConsole;
    private GeolocationPermissionsPrompt mGeolocationPermissionsPrompt;
    private Handler mHandler;
    private long mId;
    private boolean mInForeground;
    private boolean mInPageLoad;
    private DataController.OnQueryUrlIsBookmark mIsBookmarkCallback;
    private boolean mIsErrorDialogShown;
    private long mLoadStartTime;
    private WebView mMainView;
    private int mPageError;
    private int mPageLoadProgress;
    private Tab mParent;
    private PermissionsPrompt mPermissionsPrompt;
    private LinkedList<ErrorDialog> mQueuedErrors;
    HashMap<Integer, Long> mSavePageJob;
    public String mSavePageTitle;
    public String mSavePageUrl;
    private Bundle mSavedState;
    private BrowserSettings mSettings;
    private WebView mSubView;
    private View mSubViewContainer;
    private boolean mSubWindowShown;
    DownloadTouchIcon mTouchIconLoader;
    private boolean mUpdateThumbnail;
    private final WebBackForwardListClient mWebBackForwardListClient;
    private final WebChromeClient mWebChromeClient;
    private final WebViewClient mWebViewClient;
    protected WebViewController mWebViewController;
    private boolean mWillBeClosed;

    public enum SecurityState {
        SECURITY_STATE_NOT_SECURE,
        SECURITY_STATE_SECURE,
        SECURITY_STATE_MIXED,
        SECURITY_STATE_BAD_CERTIFICATE
    }

    static {
        sAlphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        sAlphaPaint.setColor(0);
    }

    private static synchronized Bitmap getDefaultFavicon(Context context) {
        if (sDefaultFavicon == null) {
            sDefaultFavicon = BitmapFactory.decodeResource(context.getResources(), R.drawable.app_web_browser_sm);
        }
        return sDefaultFavicon;
    }

    protected static class PageState {
        Bitmap mFavicon;
        boolean mIncognito;
        boolean mIsBookmarkedSite;
        boolean mIsDownload = false;
        String mOriginalUrl;
        SecurityState mSecurityState;
        SslError mSslCertificateError;
        String mTitle;
        String mUrl;

        PageState(Context context, boolean z) {
            this.mIncognito = z;
            if (this.mIncognito) {
                this.mUrl = "browser:incognito";
                this.mOriginalUrl = "browser:incognito";
                this.mTitle = context.getString(R.string.new_incognito_tab);
            } else {
                this.mUrl = "";
                this.mOriginalUrl = "";
                this.mTitle = context.getString(R.string.new_tab);
            }
            this.mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
        }

        PageState(Context context, boolean z, String str, Bitmap bitmap) {
            this.mIncognito = z;
            this.mUrl = str;
            this.mOriginalUrl = str;
            if (URLUtil.isHttpsUrl(str)) {
                this.mSecurityState = SecurityState.SECURITY_STATE_SECURE;
            } else {
                this.mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
            }
            this.mFavicon = bitmap;
        }
    }

    private class ErrorDialog {
        public final String mDescription;
        public final int mError;
        public final int mTitle;

        ErrorDialog(int i, String str, int i2) {
            this.mTitle = i;
            this.mDescription = str;
            this.mError = i2;
        }
    }

    private void processNextError() {
        if (this.mQueuedErrors == null) {
            return;
        }
        this.mQueuedErrors.removeFirst();
        if (this.mQueuedErrors.size() == 0) {
            this.mQueuedErrors = null;
        } else {
            showError(this.mQueuedErrors.getFirst());
        }
    }

    private void queueError(int i, String str) {
        int i2;
        if (this.mQueuedErrors == null) {
            this.mQueuedErrors = new LinkedList<>();
        }
        Iterator<ErrorDialog> it = this.mQueuedErrors.iterator();
        while (it.hasNext()) {
            if (it.next().mError == i) {
                return;
            }
        }
        if (i == -20000 && (str == null || str.isEmpty())) {
            str = this.mContext.getString(R.string.open_saved_page_failed);
        }
        if (i == -14 || i == -20000) {
            i2 = R.string.browserFrameFileErrorLabel;
        } else {
            i2 = R.string.browserFrameNetworkErrorLabel;
        }
        ErrorDialog errorDialog = new ErrorDialog(i2, str, i);
        this.mQueuedErrors.addLast(errorDialog);
        if (this.mQueuedErrors.size() == 1 && this.mInForeground) {
            showError(errorDialog);
        }
    }

    private void showError(ErrorDialog errorDialog) {
        if (this.mInForeground && !this.mIsErrorDialogShown) {
            AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setTitle(errorDialog.mTitle).setMessage(errorDialog.mDescription).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).create();
            alertDialogCreate.setOnDismissListener(this.mDialogListener);
            alertDialogCreate.show();
            this.mIsErrorDialogShown = true;
        }
    }

    private void syncCurrentState(WebView webView, String str) {
        if (this.mWillBeClosed) {
            return;
        }
        if (DEBUG) {
            Log.d("browser", "Tab.syncCurrentState()()--->url = " + str + ", webview = " + webView);
        }
        this.mCurrentState.mUrl = webView.getUrl();
        if (this.mCurrentState.mUrl == null) {
            this.mCurrentState.mUrl = "";
        }
        if (this.mPageError != 0 && this.mCurrentState.mOriginalUrl != webView.getOriginalUrl()) {
            this.mCurrentState.mUrl = str;
        }
        this.mCurrentState.mOriginalUrl = webView.getOriginalUrl();
        this.mCurrentState.mTitle = webView.getTitle();
        this.mCurrentState.mFavicon = webView.getFavicon();
        if (!URLUtil.isHttpsUrl(this.mCurrentState.mUrl)) {
            this.mCurrentState.mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
            this.mCurrentState.mSslCertificateError = null;
        }
        this.mCurrentState.mIncognito = webView.isPrivateBrowsingEnabled();
    }

    void setDeviceAccountLogin(DeviceAccountLogin deviceAccountLogin) {
        this.mDeviceAccountLogin = deviceAccountLogin;
    }

    DeviceAccountLogin getDeviceAccountLogin() {
        if (DEBUG) {
            Log.d("browser", "Tab.getDeviceAccountLogin()--->");
        }
        return this.mDeviceAccountLogin;
    }

    public void PopupWindowShown(boolean z) {
        this.mSubWindowShown = z;
    }

    static class AnonymousClass9 {
        static final int[] $SwitchMap$android$webkit$ConsoleMessage$MessageLevel = new int[ConsoleMessage.MessageLevel.values().length];

        static {
            try {
                $SwitchMap$android$webkit$ConsoleMessage$MessageLevel[ConsoleMessage.MessageLevel.TIP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$webkit$ConsoleMessage$MessageLevel[ConsoleMessage.MessageLevel.LOG.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$webkit$ConsoleMessage$MessageLevel[ConsoleMessage.MessageLevel.WARNING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$webkit$ConsoleMessage$MessageLevel[ConsoleMessage.MessageLevel.ERROR.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$webkit$ConsoleMessage$MessageLevel[ConsoleMessage.MessageLevel.DEBUG.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    private static class SubWindowClient extends WebViewClient {
        private final WebViewClient mClient;
        private final WebViewController mController;

        SubWindowClient(WebViewClient webViewClient, WebViewController webViewController) {
            this.mClient = webViewClient;
            this.mController = webViewController;
        }

        @Override
        public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
            this.mController.endActionMode();
        }

        @Override
        public void doUpdateVisitedHistory(WebView webView, String str, boolean z) {
            this.mClient.doUpdateVisitedHistory(webView, str, z);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String str) {
            return this.mClient.shouldOverrideUrlLoading(webView, str);
        }

        @Override
        public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
            this.mClient.onReceivedSslError(webView, sslErrorHandler, sslError);
        }

        @Override
        public void onReceivedClientCertRequest(WebView webView, ClientCertRequest clientCertRequest) {
            this.mClient.onReceivedClientCertRequest(webView, clientCertRequest);
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView webView, HttpAuthHandler httpAuthHandler, String str, String str2) {
            this.mClient.onReceivedHttpAuthRequest(webView, httpAuthHandler, str, str2);
        }

        @Override
        public void onFormResubmission(WebView webView, Message message, Message message2) {
            this.mClient.onFormResubmission(webView, message, message2);
        }

        @Override
        public void onReceivedError(WebView webView, int i, String str, String str2) {
            this.mClient.onReceivedError(webView, i, str, str2);
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView webView, KeyEvent keyEvent) {
            return this.mClient.shouldOverrideKeyEvent(webView, keyEvent);
        }

        @Override
        public void onUnhandledKeyEvent(WebView webView, KeyEvent keyEvent) {
            this.mClient.onUnhandledKeyEvent(webView, keyEvent);
        }
    }

    private class SubWindowChromeClient extends WebChromeClient {
        private final WebChromeClient mClient;

        SubWindowChromeClient(WebChromeClient webChromeClient) {
            this.mClient = webChromeClient;
        }

        @Override
        public void onProgressChanged(WebView webView, int i) {
            this.mClient.onProgressChanged(webView, i);
        }

        @Override
        public boolean onCreateWindow(WebView webView, boolean z, boolean z2, Message message) {
            return this.mClient.onCreateWindow(webView, z, z2, message);
        }

        @Override
        public void onCloseWindow(WebView webView) {
            if (webView != Tab.this.mSubView) {
                Log.e("Tab", "Can't close the window");
            }
            Tab.this.mWebViewController.dismissSubWindow(Tab.this);
        }
    }

    Tab(WebViewController webViewController, WebView webView) {
        this(webViewController, webView, null);
    }

    Tab(WebViewController webViewController, Bundle bundle) {
        this(webViewController, null, bundle);
    }

    Tab(WebViewController webViewController, WebView webView, Bundle bundle) {
        boolean zIsPrivateBrowsingEnabled;
        this.mWillBeClosed = false;
        this.mPageError = 0;
        this.mId = -1L;
        this.mDialogListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Tab.this.mIsErrorDialogShown = false;
                Tab.this.processNextError();
            }
        };
        this.mIsErrorDialogShown = false;
        this.mWebViewClient = new WebViewClient() {
            private Message mDontResend;
            private Message mResend;

            @Override
            public void onPageStarted(WebView webView2, String str, Bitmap bitmap) {
                TabControl tabControl = Tab.this.mWebViewController.getTabControl();
                if (tabControl != null && Tab.DEBUG) {
                    Log.d("browser", "Network_Issue [" + tabControl.getTabPosition(Tab.this) + "/" + tabControl.getTabCount() + "] onPageStarted url=" + str);
                }
                Tab.this.mInPageLoad = true;
                Tab.this.mUpdateThumbnail = true;
                Tab.this.mPageLoadProgress = 5;
                Tab.this.mCurrentState = new PageState(Tab.this.mContext, webView2.isPrivateBrowsingEnabled(), str, bitmap);
                Tab.this.mLoadStartTime = SystemClock.uptimeMillis();
                if (Tab.this.mTouchIconLoader != null) {
                    Tab.this.mTouchIconLoader.mTab = null;
                    Tab.this.mTouchIconLoader = null;
                }
                if (Tab.this.mErrorConsole != null) {
                    Tab.this.mErrorConsole.clearErrorMessages();
                    if (Tab.this.mWebViewController.shouldShowErrorConsole()) {
                        Tab.this.mErrorConsole.showConsole(2);
                    }
                }
                if (Tab.this.mDeviceAccountLogin != null) {
                    Tab.this.mDeviceAccountLogin.cancel();
                    Tab.this.mDeviceAccountLogin = null;
                    Tab.this.mWebViewController.hideAutoLogin(Tab.this);
                }
                Tab.this.mWebViewController.onPageStarted(Tab.this, webView2, bitmap);
                Tab.this.updateBookmarkedStatus();
            }

            @Override
            public void onPageFinished(WebView webView2, String str) {
                TabControl tabControl = Tab.this.mWebViewController.getTabControl();
                if (tabControl != null && Tab.DEBUG) {
                    Log.d("browser", "Network_Issue [" + tabControl.getTabPosition(Tab.this) + "/" + tabControl.getTabCount() + "] onPageFinished url=" + str);
                }
                Tab.this.mDisableOverrideUrlLoading = false;
                if (!Tab.this.isPrivateBrowsingEnabled()) {
                    LogTag.logPageFinishedLoading(str, SystemClock.uptimeMillis() - Tab.this.mLoadStartTime);
                }
                Tab.this.syncCurrentState(webView2, str);
                if (Tab.this.mCurrentState.mIsDownload) {
                    Tab.this.mCurrentState.mUrl = Tab.this.mCurrentState.mOriginalUrl;
                    if (Tab.this.mCurrentState.mUrl == null) {
                        Tab.this.mCurrentState.mUrl = "";
                    }
                }
                if (str != null && str.equals(Tab.this.mSavePageUrl)) {
                    Tab.this.mCurrentState.mTitle = Tab.this.mSavePageTitle;
                    Tab.this.mCurrentState.mUrl = Tab.this.mSavePageUrl;
                }
                if (str != null && str.startsWith("about:blank")) {
                    Tab.this.mCurrentState.mFavicon = Tab.getDefaultFavicon(Tab.this.mContext);
                }
                Tab.this.mWebViewController.onPageFinished(Tab.this);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView2, String str) {
                if (!Tab.this.mDisableOverrideUrlLoading && Tab.this.mInForeground) {
                    return Tab.this.mWebViewController.shouldOverrideUrlLoading(Tab.this, webView2, str);
                }
                return false;
            }

            @Override
            public void onLoadResource(WebView webView2, String str) {
                if (str != null && str.length() > 0 && Tab.this.mCurrentState.mSecurityState == SecurityState.SECURITY_STATE_SECURE && !URLUtil.isHttpsUrl(str) && !URLUtil.isDataUrl(str) && !URLUtil.isAboutUrl(str)) {
                    Tab.this.mCurrentState.mSecurityState = SecurityState.SECURITY_STATE_MIXED;
                }
            }

            @Override
            public void onReceivedError(WebView webView2, int i, String str, String str2) {
                if (Tab.DEBUG) {
                    Log.d("Tab", "Network_Issue error code: " + i + " url: " + str2);
                }
                Tab.this.mPageError = i;
                Tab.this.mWebViewController.sendErrorCode(i, str2);
                if (i != -2 && i != -6 && i != -12 && i != -10 && i != -13) {
                    Tab.this.queueError(i, str);
                    if (!Tab.this.isPrivateBrowsingEnabled() && Tab.DEBUG) {
                        Log.e("Tab", "onReceivedError " + i + " " + str2 + " " + str);
                    }
                }
            }

            @Override
            public void onReceivedHttpError(WebView webView2, WebResourceRequest webResourceRequest, WebResourceResponse webResourceResponse) {
                if (webResourceRequest.isForMainFrame() && Tab.DEBUG && webResourceResponse != null) {
                    Log.d("Tab", "Network_Issue http error code: " + webResourceResponse.getStatusCode() + " url: " + webResourceRequest.getUrl());
                }
            }

            @Override
            public void onFormResubmission(WebView webView2, Message message, Message message2) {
                if (!Tab.this.mInForeground) {
                    message.sendToTarget();
                    return;
                }
                if (this.mDontResend != null) {
                    Log.w("Tab", "onFormResubmission should not be called again while dialog is still up");
                    message.sendToTarget();
                } else {
                    this.mDontResend = message;
                    this.mResend = message2;
                    new AlertDialog.Builder(Tab.this.mContext).setTitle(R.string.browserFrameFormResubmitLabel).setMessage(R.string.browserFrameFormResubmitMessage).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (AnonymousClass2.this.mResend != null) {
                                AnonymousClass2.this.mResend.sendToTarget();
                                AnonymousClass2.this.mResend = null;
                                AnonymousClass2.this.mDontResend = null;
                            }
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (AnonymousClass2.this.mDontResend != null) {
                                AnonymousClass2.this.mDontResend.sendToTarget();
                                AnonymousClass2.this.mResend = null;
                                AnonymousClass2.this.mDontResend = null;
                            }
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            if (AnonymousClass2.this.mDontResend != null) {
                                AnonymousClass2.this.mDontResend.sendToTarget();
                                AnonymousClass2.this.mResend = null;
                                AnonymousClass2.this.mDontResend = null;
                            }
                        }
                    }).show();
                }
            }

            @Override
            public void doUpdateVisitedHistory(WebView webView2, String str, boolean z) {
                Tab.this.mWebViewController.doUpdateVisitedHistory(Tab.this, z);
            }

            @Override
            public void onReceivedSslError(final WebView webView2, final SslErrorHandler sslErrorHandler, final SslError sslError) {
                if (Tab.DEBUG) {
                    Log.d("Tab", "Network_Issue onReceivedSslError: " + sslError.toString());
                }
                if (Tab.this.mInForeground) {
                    if (Tab.this.mSettings.showSecurityWarnings()) {
                        new AlertDialog.Builder(Tab.this.mContext).setTitle(R.string.security_warning).setMessage(R.string.ssl_warnings_header).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(R.string.ssl_continue, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sslErrorHandler.proceed();
                                Tab.this.handleProceededAfterSslError(sslError);
                            }
                        }).setNeutralButton(R.string.view_certificate, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Tab.this.mWebViewController.showSslCertificateOnError(webView2, sslErrorHandler, sslError);
                            }
                        }).setNegativeButton(R.string.ssl_go_back, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                sslErrorHandler.cancel();
                                Tab.this.setSecurityState(SecurityState.SECURITY_STATE_NOT_SECURE);
                                Tab.this.mWebViewController.onUserCanceledSsl(Tab.this);
                            }
                        }).show();
                        return;
                    } else {
                        sslErrorHandler.proceed();
                        return;
                    }
                }
                sslErrorHandler.cancel();
                Tab.this.setSecurityState(SecurityState.SECURITY_STATE_NOT_SECURE);
            }

            @Override
            public void onReceivedClientCertRequest(WebView webView2, final ClientCertRequest clientCertRequest) {
                if (!Tab.this.mInForeground) {
                    clientCertRequest.ignore();
                } else {
                    KeyChain.choosePrivateKeyAlias(Tab.this.mWebViewController.getActivity(), new KeyChainAliasCallback() {
                        @Override
                        public void alias(String str) {
                            if (str == null) {
                                clientCertRequest.cancel();
                            } else {
                                new KeyChainLookup(Tab.this.mContext, clientCertRequest, str).execute(new Void[0]);
                            }
                        }
                    }, clientCertRequest.getKeyTypes(), clientCertRequest.getPrincipals(), clientCertRequest.getHost(), clientCertRequest.getPort(), null);
                }
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView webView2, HttpAuthHandler httpAuthHandler, String str, String str2) {
                Tab.this.mWebViewController.onReceivedHttpAuthRequest(Tab.this, webView2, httpAuthHandler, str, str2);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView2, String str) {
                return HomeProvider.shouldInterceptRequest(Tab.this.mContext, str);
            }

            @Override
            public boolean shouldOverrideKeyEvent(WebView webView2, KeyEvent keyEvent) {
                if (!Tab.this.mInForeground) {
                    return false;
                }
                return Tab.this.mWebViewController.shouldOverrideKeyEvent(keyEvent);
            }

            @Override
            public void onUnhandledKeyEvent(WebView webView2, KeyEvent keyEvent) {
                if (Tab.this.mInForeground && !Tab.this.mWebViewController.onUnhandledKeyEvent(keyEvent)) {
                    super.onUnhandledKeyEvent(webView2, keyEvent);
                }
            }

            @Override
            public void onReceivedLoginRequest(WebView webView2, String str, String str2, String str3) {
                new DeviceAccountLogin(Tab.this.mWebViewController.getActivity(), webView2, Tab.this, Tab.this.mWebViewController).handleLogin(str, str2, str3);
            }
        };
        this.mSubWindowShown = false;
        this.mWebChromeClient = new WebChromeClient() {
            private void createWindow(boolean z, Message message) {
                WebView.WebViewTransport webViewTransport = (WebView.WebViewTransport) message.obj;
                if (z) {
                    Tab.this.createSubWindow();
                    Tab.this.mWebViewController.attachSubWindow(Tab.this);
                    webViewTransport.setWebView(Tab.this.mSubView);
                } else {
                    webViewTransport.setWebView(Tab.this.mWebViewController.openTab(null, Tab.this, true, true).getWebView());
                }
                message.sendToTarget();
            }

            @Override
            public boolean onCreateWindow(WebView webView2, boolean z, boolean z2, Message message) {
                if (!Tab.this.mInForeground) {
                    return false;
                }
                if (z && Tab.this.mSubView != null) {
                    new AlertDialog.Builder(Tab.this.mContext).setTitle(R.string.too_many_subwindows_dialog_title).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.too_many_subwindows_dialog_message).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
                    return false;
                }
                if (!Tab.this.mWebViewController.getTabControl().canCreateNewTab()) {
                    new AlertDialog.Builder(Tab.this.mContext).setTitle(R.string.too_many_windows_dialog_title).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.too_many_windows_dialog_message).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
                    return false;
                }
                if (!z2) {
                    if (!Tab.this.mSubWindowShown) {
                        Tab.this.mWebViewController.onShowPopupWindowAttempt(Tab.this, z, message);
                        return true;
                    }
                    new AlertDialog.Builder(Tab.this.mContext).setTitle(R.string.too_many_subwindows_dialog_title).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.too_many_subwindows_dialog_message).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
                    return false;
                }
                createWindow(z, message);
                return true;
            }

            @Override
            public void onRequestFocus(WebView webView2) {
                if (!Tab.this.mInForeground) {
                    Tab.this.mWebViewController.switchToTab(Tab.this);
                }
            }

            @Override
            public void onCloseWindow(WebView webView2) {
                if (Tab.this.mParent != null) {
                    if (Tab.this.mInForeground) {
                        Tab.this.mWebViewController.switchToTab(Tab.this.mParent);
                    }
                    Tab.this.mWebViewController.closeTab(Tab.this);
                }
            }

            @Override
            public boolean onJsAlert(WebView webView2, String str, String str2, JsResult jsResult) {
                Tab.this.mWebViewController.getTabControl().setActiveTab(Tab.this);
                return false;
            }

            @Override
            public boolean onJsConfirm(WebView webView2, String str, String str2, JsResult jsResult) {
                Tab.this.mWebViewController.getTabControl().setActiveTab(Tab.this);
                return false;
            }

            @Override
            public boolean onJsPrompt(WebView webView2, String str, String str2, String str3, JsPromptResult jsPromptResult) {
                Tab.this.mWebViewController.getTabControl().setActiveTab(Tab.this);
                return false;
            }

            @Override
            public void onProgressChanged(WebView webView2, int i) {
                Tab.this.mPageLoadProgress = i;
                Tab.this.mPageError = 0;
                if (i == 100) {
                    Tab.this.mInPageLoad = false;
                    Tab.this.syncCurrentState(webView2, webView2.getUrl());
                }
                Tab.this.mWebViewController.onProgressChanged(Tab.this);
                if (Tab.this.mUpdateThumbnail && i == 100) {
                    Tab.this.mUpdateThumbnail = false;
                }
            }

            @Override
            public void onReceivedTitle(WebView webView2, String str) {
                Tab.this.mCurrentState.mTitle = str;
                Tab.this.mWebViewController.onReceivedTitle(Tab.this, str);
            }

            @Override
            public void onReceivedIcon(WebView webView2, Bitmap bitmap) {
                Tab.this.mCurrentState.mFavicon = bitmap;
                Tab.this.mWebViewController.onFavicon(Tab.this, webView2, bitmap);
            }

            @Override
            public void onReceivedTouchIconUrl(WebView webView2, String str, boolean z) {
                ContentResolver contentResolver = Tab.this.mContext.getContentResolver();
                synchronized (Tab.this) {
                    if (z) {
                        try {
                            if (Tab.this.mTouchIconLoader != null) {
                                Tab.this.mTouchIconLoader.cancel(false);
                                Tab.this.mTouchIconLoader = null;
                            }
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                    if (Tab.this.mTouchIconLoader == null) {
                        Tab.this.mTouchIconLoader = new DownloadTouchIcon(Tab.this, Tab.this.mContext, contentResolver, webView2);
                        Tab.this.mTouchIconLoader.execute(str);
                    }
                }
            }

            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback customViewCallback) {
                Activity activity = Tab.this.mWebViewController.getActivity();
                if (activity != null) {
                    onShowCustomView(view, activity.getRequestedOrientation(), customViewCallback);
                }
            }

            @Override
            public void onShowCustomView(View view, int i, WebChromeClient.CustomViewCallback customViewCallback) {
                if (Tab.this.mInForeground) {
                    Tab.this.mWebViewController.showCustomView(Tab.this, view, i, customViewCallback);
                }
            }

            @Override
            public void onHideCustomView() {
                if (Tab.this.mInForeground) {
                    Tab.this.mWebViewController.hideCustomView();
                }
            }

            @Override
            public void onExceededDatabaseQuota(String str, String str2, long j, long j2, long j3, WebStorage.QuotaUpdater quotaUpdater) {
                Tab.this.mSettings.getWebStorageSizeManager().onExceededDatabaseQuota(str, str2, j, j2, j3, quotaUpdater);
            }

            public void onReachedMaxAppCacheSize(long j, long j2, WebStorage.QuotaUpdater quotaUpdater) {
                Tab.this.mSettings.getWebStorageSizeManager().onReachedMaxAppCacheSize(j, j2, quotaUpdater);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String str, GeolocationPermissions.Callback callback) {
                if (Tab.this.mInForeground) {
                    Tab.this.getGeolocationPermissionsPrompt().show(str, callback);
                }
            }

            @Override
            public void onGeolocationPermissionsHidePrompt() {
                if (Tab.this.mInForeground && Tab.this.mGeolocationPermissionsPrompt != null) {
                    Tab.this.mGeolocationPermissionsPrompt.hide();
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest permissionRequest) {
                if (Tab.this.mInForeground) {
                    Tab.this.getPermissionsPrompt().show(permissionRequest);
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest permissionRequest) {
                if (Tab.this.mInForeground && Tab.this.mPermissionsPrompt != null) {
                    Tab.this.mPermissionsPrompt.hide();
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (Tab.this.mInForeground) {
                    ErrorConsoleView errorConsole = Tab.this.getErrorConsole(true);
                    errorConsole.addErrorMessage(consoleMessage);
                    if (Tab.this.mWebViewController.shouldShowErrorConsole() && errorConsole.getShowState() != 1) {
                        errorConsole.showConsole(0);
                    }
                }
                if (Tab.this.isPrivateBrowsingEnabled() || !Tab.DEBUG) {
                    return true;
                }
                String str = "Console: " + consoleMessage.message() + " " + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber();
                switch (AnonymousClass9.$SwitchMap$android$webkit$ConsoleMessage$MessageLevel[consoleMessage.messageLevel().ordinal()]) {
                    case 1:
                        Log.v("browser", str);
                        return true;
                    case 2:
                        Log.i("browser", str);
                        return true;
                    case 3:
                        Log.w("browser", str);
                        return true;
                    case 4:
                        Log.e("browser", str);
                        return true;
                    case 5:
                        Log.d("browser", str);
                        return true;
                    default:
                        return true;
                }
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                if (Tab.this.mInForeground) {
                    return Tab.this.mWebViewController.getDefaultVideoPoster();
                }
                return null;
            }

            @Override
            public View getVideoLoadingProgressView() {
                if (Tab.this.mInForeground) {
                    return Tab.this.mWebViewController.getVideoLoadingProgressView();
                }
                return null;
            }

            @Override
            public boolean onShowFileChooser(WebView webView2, ValueCallback<Uri[]> valueCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (Tab.this.mInForeground) {
                    Tab.this.mWebViewController.showFileChooser(valueCallback, fileChooserParams);
                    return true;
                }
                return false;
            }

            @Override
            public void getVisitedHistory(ValueCallback<String[]> valueCallback) {
                Tab.this.mWebViewController.getVisitedHistory(valueCallback);
            }
        };
        this.mIsBookmarkCallback = new DataController.OnQueryUrlIsBookmark() {
            @Override
            public void onQueryUrlIsBookmark(String str, boolean z) {
                if (Tab.this.mCurrentState.mUrl.equals(str)) {
                    Tab.this.mCurrentState.mIsBookmarkedSite = z;
                    Tab.this.mWebViewController.bookmarkedStatusHasChanged(Tab.this);
                }
            }
        };
        this.callback = null;
        this.mBrowserUrlExt = null;
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("Tab()--->Constructor()--->WebView is ");
            sb.append(webView == null ? "null" : "not null");
            sb.append(", Bundle is ");
            sb.append(bundle == null ? "null" : "not null");
            Log.d("browser", sb.toString());
        }
        this.mSavePageJob = new HashMap<>();
        this.mWebViewController = webViewController;
        this.mContext = this.mWebViewController.getContext();
        this.mSettings = BrowserSettings.getInstance();
        this.mDataController = DataController.getInstance(this.mContext);
        Context context = this.mContext;
        if (webView == null) {
            zIsPrivateBrowsingEnabled = false;
        } else {
            zIsPrivateBrowsingEnabled = webView.isPrivateBrowsingEnabled();
        }
        this.mCurrentState = new PageState(context, zIsPrivateBrowsingEnabled);
        this.mInPageLoad = false;
        this.mInForeground = false;
        this.mDownloadListener = new BrowserDownloadListener() {
            @Override
            public void onDownloadStart(String str, String str2, String str3, String str4, String str5, long j) {
                String strRemapGenericMimeTypePublic = MimeTypeMap.getSingleton().remapGenericMimeTypePublic(str4, str, str3);
                Tab.this.mCurrentState.mIsDownload = true;
                Tab.this.mWebViewController.onDownloadStart(Tab.this, str, str2, str3, strRemapGenericMimeTypePublic, str5, j);
            }
        };
        this.mWebBackForwardListClient = new WebBackForwardListClient() {
        };
        this.mCaptureWidth = this.mContext.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_width);
        this.mCaptureHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_height);
        updateShouldCaptureThumbnails();
        restoreState(bundle);
        if (getId() == -1) {
            this.mId = TabControl.getNextId();
        }
        setWebView(webView);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 42) {
                    Tab.this.capture();
                }
            }
        };
    }

    public boolean shouldUpdateThumbnail() {
        return this.mUpdateThumbnail;
    }

    public void refreshIdAfterPreload() {
        this.mId = TabControl.getNextId();
    }

    public void updateShouldCaptureThumbnails() {
        if (this.mWebViewController.shouldCaptureThumbnails()) {
            synchronized (this) {
                if (this.mCapture == null) {
                    this.mCapture = Bitmap.createBitmap(this.mCaptureWidth, this.mCaptureHeight, Bitmap.Config.RGB_565);
                    this.mCapture.eraseColor(-1);
                    if (this.mInForeground) {
                        postCapture();
                    }
                }
            }
            return;
        }
        synchronized (this) {
            this.mCapture = null;
            deleteThumbnail();
        }
    }

    public void setController(WebViewController webViewController) {
        this.mWebViewController = webViewController;
        updateShouldCaptureThumbnails();
    }

    public long getId() {
        return this.mId;
    }

    void setWebView(WebView webView) {
        setWebView(webView, true);
    }

    void setWebView(WebView webView, boolean z) {
        if (DEBUG) {
            Log.d("browser", "Tab.setWebView()--->webview = " + webView + ", restore = " + z);
        }
        if (this.mMainView == webView) {
            return;
        }
        if (this.mGeolocationPermissionsPrompt != null) {
            this.mGeolocationPermissionsPrompt.hide();
        }
        if (this.mPermissionsPrompt != null) {
            this.mPermissionsPrompt.hide();
        }
        this.mWebViewController.onSetWebView(this, webView);
        if (this.mMainView != null) {
            this.mMainView.setPictureListener(null);
            if (webView != null) {
                syncCurrentState(webView, null);
            } else {
                this.mCurrentState = new PageState(this.mContext, false);
            }
        }
        this.mMainView = webView;
        if (this.mMainView != null) {
            this.mMainView.setWebViewClient(this.mWebViewClient);
            this.mMainView.setWebChromeClient(this.mWebChromeClient);
            this.mMainView.setDownloadListener(this.mDownloadListener);
            TabControl tabControl = this.mWebViewController.getTabControl();
            if (tabControl != null && tabControl.getOnThumbnailUpdatedListener() != null) {
                this.mMainView.setPictureListener(this);
            }
            if (z && this.mSavedState != null) {
                restoreUserAgent();
                WebBackForwardList webBackForwardListRestoreState = this.mMainView.restoreState(this.mSavedState);
                if (webBackForwardListRestoreState == null || webBackForwardListRestoreState.getSize() == 0) {
                    Log.w("Tab", "Failed to restore WebView state!");
                    loadUrl(this.mCurrentState.mOriginalUrl, null);
                }
                this.mSavedState = null;
            }
        }
    }

    void destroy() {
        if (DEBUG) {
            Log.d("browser", "Tab.destroy--->" + this.mMainView);
        }
        if (this.mMainView != null) {
            dismissSubWindow();
            WebView webView = this.mMainView;
            setWebView(null);
            webView.destroy();
        }
        if (this.mSavePageJob != null && this.mSavePageJob.size() != 0) {
            Toast.makeText(this.mContext, R.string.saved_page_failed, 1).show();
            new CancelSavePageTask().execute(new Void[0]);
        }
    }

    private class CancelSavePageTask extends AsyncTask<Void, Void, Void> {
        private CancelSavePageTask() {
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            if (Tab.DEBUG) {
                Log.d("browser", "Tab()--->CancelSavePageTask()--->doInBackground()");
            }
            Notification.Builder builder = new Notification.Builder(Tab.this.mContext);
            NotificationManager notificationManager = (NotificationManager) Tab.this.mContext.getSystemService("notification");
            ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
            Tab.this.mContext.getContentResolver();
            for (Map.Entry<Integer, Long> entry : Tab.this.mSavePageJob.entrySet()) {
                long jLongValue = entry.getValue().longValue();
                int iIntValue = entry.getKey().intValue();
                builder.setSmallIcon(R.drawable.ic_save_page_notification_fail);
                builder.setContentText(Tab.this.mContext.getText(R.string.saved_page_failed));
                builder.setOngoing(false);
                builder.setContentIntent(null);
                builder.setTicker(Tab.this.mContext.getText(R.string.saved_page_failed));
                notificationManager.notify(iIntValue, builder.build());
                arrayList.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(SnapshotProvider.Snapshots.CONTENT_URI, jLongValue)).build());
            }
            try {
                Tab.this.mContext.getContentResolver().applyBatch("com.android.browser.snapshots", arrayList);
            } catch (OperationApplicationException e) {
                Log.e("Tab", "Failed to delete save page. OperationApplicationException: " + e.getMessage());
            } catch (RemoteException e2) {
                Log.e("Tab", "Failed to delete save page. RemoteException: " + e2.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void r2) {
            if (Tab.this.mSavePageJob != null) {
                Tab.this.mSavePageJob.clear();
                Tab.this.mSavePageJob = null;
            }
        }
    }

    void removeFromTree() {
        if (DEBUG) {
            Log.d("browser", "Tab.removeFromTree()--->tab this = " + this);
        }
        if (this.mChildren != null) {
            Iterator<Tab> it = this.mChildren.iterator();
            while (it.hasNext()) {
                it.next().setParent(null);
            }
        }
        if (this.mParent != null) {
            this.mParent.mChildren.remove(this);
        }
        deleteThumbnail();
    }

    boolean createSubWindow() {
        if (DEBUG) {
            Log.d("browser", "Tab.createSubWindow()--->mSubView = " + this.mSubView);
        }
        if (this.mSubView == null) {
            this.mWebViewController.createSubWindow(this);
            this.mSubView.setWebViewClient(new SubWindowClient(this.mWebViewClient, this.mWebViewController));
            this.mSubView.setWebChromeClient(new SubWindowChromeClient(this.mWebChromeClient));
            this.mSubView.setDownloadListener(new BrowserDownloadListener() {
                @Override
                public void onDownloadStart(String str, String str2, String str3, String str4, String str5, long j) {
                    Tab.this.mWebViewController.onDownloadStart(Tab.this, str, str2, str3, MimeTypeMap.getSingleton().remapGenericMimeTypePublic(str4, str, str3), str5, j);
                    if (Tab.this.mSubView.copyBackForwardList().getSize() == 0) {
                        Tab.this.mWebViewController.dismissSubWindow(Tab.this);
                    }
                }
            });
            this.mSubView.setOnCreateContextMenuListener(this.mWebViewController.getActivity());
            return true;
        }
        return false;
    }

    void dismissSubWindow() {
        if (DEBUG) {
            Log.d("browser", "Tab.dismissSubWindow()--->mSubView = " + this.mSubView);
        }
        if (this.mSubView != null) {
            this.mWebViewController.endActionMode();
            this.mSubView.destroy();
            this.mSubView = null;
            this.mSubViewContainer = null;
        }
    }

    void setParent(Tab tab) {
        if (tab == this) {
            throw new IllegalStateException("Cannot set parent to self!");
        }
        this.mParent = tab;
        if (this.mSavedState != null) {
            if (tab == null) {
                this.mSavedState.remove("parentTab");
            } else {
                this.mSavedState.putLong("parentTab", tab.getId());
            }
        }
        if (tab != null && this.mSettings.hasDesktopUseragent(tab.getWebView()) != this.mSettings.hasDesktopUseragent(getWebView())) {
            this.mSettings.toggleDesktopUseragent(getWebView());
        }
        if (tab != null && tab.getId() == getId()) {
            throw new IllegalStateException("Parent has same ID as child!");
        }
    }

    public Tab getParent() {
        return this.mParent;
    }

    void addChildTab(Tab tab) {
        if (DEBUG) {
            Log.d("browser", "Tab.addChildTab()--->Tab child = " + tab);
        }
        if (this.mChildren == null) {
            this.mChildren = new Vector<>();
        }
        this.mChildren.add(tab);
        tab.setParent(this);
    }

    void resume() {
        if (this.mMainView != null) {
            setupHwAcceleration(this.mMainView);
            this.mMainView.onResume();
            if (this.mSubView != null) {
                this.mSubView.onResume();
            }
        }
    }

    private void setupHwAcceleration(View view) {
        if (view == null) {
            return;
        }
        if (BrowserSettings.getInstance().isHardwareAccelerated()) {
            view.setLayerType(0, null);
        } else {
            view.setLayerType(1, null);
        }
    }

    void pause() {
        if (this.mMainView != null) {
            this.mMainView.onPause();
            if (this.mSubView != null) {
                this.mSubView.onPause();
            }
        }
    }

    void putInForeground() {
        if (this.mInForeground) {
            return;
        }
        this.mInForeground = true;
        resume();
        Activity activity = this.mWebViewController.getActivity();
        this.mMainView.setOnCreateContextMenuListener(activity);
        if (this.mSubView != null) {
            this.mSubView.setOnCreateContextMenuListener(activity);
        }
        if (this.mQueuedErrors != null && this.mQueuedErrors.size() > 0) {
            showError(this.mQueuedErrors.getFirst());
        }
        this.mWebViewController.bookmarkedStatusHasChanged(this);
    }

    void putInBackground() {
        if (!this.mInForeground) {
            return;
        }
        capture();
        this.mInForeground = false;
        pause();
        this.mMainView.setOnCreateContextMenuListener(null);
        if (this.mSubView != null) {
            this.mSubView.setOnCreateContextMenuListener(null);
        }
    }

    boolean inForeground() {
        return this.mInForeground;
    }

    WebView getTopWindow() {
        if (this.mSubView != null) {
            return this.mSubView;
        }
        return this.mMainView;
    }

    WebView getWebView() {
        return this.mMainView;
    }

    void setViewContainer(View view) {
        this.mContainer = view;
    }

    View getViewContainer() {
        return this.mContainer;
    }

    boolean isPrivateBrowsingEnabled() {
        return this.mCurrentState.mIncognito;
    }

    WebView getSubWebView() {
        return this.mSubView;
    }

    void setSubWebView(WebView webView) {
        this.mSubView = webView;
    }

    View getSubViewContainer() {
        return this.mSubViewContainer;
    }

    void setSubViewContainer(View view) {
        this.mSubViewContainer = view;
    }

    GeolocationPermissionsPrompt getGeolocationPermissionsPrompt() {
        if (this.mGeolocationPermissionsPrompt == null) {
            this.mGeolocationPermissionsPrompt = (GeolocationPermissionsPrompt) ((ViewStub) this.mContainer.findViewById(R.id.geolocation_permissions_prompt)).inflate();
        }
        return this.mGeolocationPermissionsPrompt;
    }

    PermissionsPrompt getPermissionsPrompt() {
        if (this.mPermissionsPrompt == null) {
            this.mPermissionsPrompt = (PermissionsPrompt) ((ViewStub) this.mContainer.findViewById(R.id.permissions_prompt)).inflate();
        }
        return this.mPermissionsPrompt;
    }

    String getAppId() {
        return this.mAppId;
    }

    void setAppId(String str) {
        this.mAppId = str;
    }

    boolean closeOnBack() {
        return this.mCloseOnBack;
    }

    void setCloseOnBack(boolean z) {
        this.mCloseOnBack = z;
    }

    String getUrl() {
        return UrlUtils.filteredUrl(this.mCurrentState.mUrl);
    }

    String getOriginalUrl() {
        if (this.mCurrentState.mOriginalUrl == null) {
            return getUrl();
        }
        return UrlUtils.filteredUrl(this.mCurrentState.mOriginalUrl);
    }

    String getTitle() {
        if (this.mCurrentState.mTitle == null && this.mInPageLoad) {
            return this.mContext.getString(R.string.title_bar_loading);
        }
        return this.mCurrentState.mTitle;
    }

    Bitmap getFavicon() {
        if (this.mCurrentState.mFavicon != null) {
            return this.mCurrentState.mFavicon;
        }
        return getDefaultFavicon(this.mContext);
    }

    public boolean isBookmarkedSite() {
        return this.mCurrentState.mIsBookmarkedSite;
    }

    public void clearTabData() {
        this.mWillBeClosed = true;
        this.mAppId = "";
        this.mCurrentState.mUrl = "";
        this.mCurrentState.mOriginalUrl = "";
        this.mCurrentState.mTitle = "";
    }

    ErrorConsoleView getErrorConsole(boolean z) {
        if (z && this.mErrorConsole == null) {
            this.mErrorConsole = new ErrorConsoleView(this.mContext);
            this.mErrorConsole.setWebView(this.mMainView);
        }
        return this.mErrorConsole;
    }

    private void setSecurityState(SecurityState securityState) {
        this.mCurrentState.mSecurityState = securityState;
        this.mCurrentState.mSslCertificateError = null;
        this.mWebViewController.onUpdatedSecurityState(this);
    }

    SecurityState getSecurityState() {
        return this.mCurrentState.mSecurityState;
    }

    SslError getSslCertificateError() {
        return this.mCurrentState.mSslCertificateError;
    }

    int getLoadProgress() {
        if (this.mInPageLoad) {
            return this.mPageLoadProgress;
        }
        return 100;
    }

    boolean inPageLoad() {
        return this.mInPageLoad;
    }

    public Bundle saveState() {
        if (this.mMainView == null) {
            return this.mSavedState;
        }
        if (TextUtils.isEmpty(this.mCurrentState.mUrl)) {
            return null;
        }
        this.mSavedState = new Bundle();
        WebBackForwardList webBackForwardListSaveState = this.mMainView.saveState(this.mSavedState);
        if ((webBackForwardListSaveState == null || webBackForwardListSaveState.getSize() == 0) && DEBUG) {
            Log.w("Tab", "Failed to save back/forward list for " + this.mCurrentState.mUrl);
        }
        this.mSavedState.putLong("ID", this.mId);
        this.mSavedState.putString("currentUrl", this.mCurrentState.mUrl);
        this.mSavedState.putString("currentTitle", this.mCurrentState.mTitle);
        this.mSavedState.putBoolean("privateBrowsingEnabled", this.mMainView.isPrivateBrowsingEnabled());
        if (this.mAppId != null) {
            this.mSavedState.putString("appid", this.mAppId);
        }
        this.mSavedState.putBoolean("closeOnBack", this.mCloseOnBack);
        if (this.mParent != null) {
            this.mSavedState.putLong("parentTab", this.mParent.mId);
        }
        this.mSavedState.putBoolean("useragent", this.mSettings.hasDesktopUseragent(getWebView()));
        return this.mSavedState;
    }

    private void restoreState(Bundle bundle) {
        if (DEBUG) {
            Log.d("browser", "Tab.restoreState()()---> bundle is " + bundle);
        }
        this.mSavedState = bundle;
        if (this.mSavedState == null) {
            return;
        }
        this.mId = bundle.getLong("ID");
        this.mAppId = bundle.getString("appid");
        this.mCloseOnBack = bundle.getBoolean("closeOnBack");
        restoreUserAgent();
        String string = bundle.getString("currentUrl");
        String string2 = bundle.getString("currentTitle");
        this.mCurrentState = new PageState(this.mContext, bundle.getBoolean("privateBrowsingEnabled"), string, null);
        this.mCurrentState.mTitle = string2;
        synchronized (this) {
            if (this.mCapture != null) {
                DataController.getInstance(this.mContext).loadThumbnail(this);
            }
        }
    }

    private void restoreUserAgent() {
        if (this.mMainView != null && this.mSavedState != null && this.mSavedState.getBoolean("useragent") != this.mSettings.hasDesktopUseragent(this.mMainView)) {
            this.mSettings.toggleDesktopUseragent(this.mMainView);
        }
    }

    public void updateBookmarkedStatus() {
        if (this.mCurrentState.mUrl != null && this.mCurrentState.mUrl.equals(SiteNavigation.SITE_NAVIGATION_URI.toString())) {
            this.mDataController.queryBookmarkStatus(SiteNavigation.SITE_NAVIGATION_URI.toString(), this.mIsBookmarkCallback);
        } else {
            this.mDataController.queryBookmarkStatus(getUrl(), this.mIsBookmarkCallback);
        }
    }

    public Bitmap getScreenshot() {
        Bitmap bitmap;
        synchronized (this) {
            bitmap = this.mCapture;
        }
        return bitmap;
    }

    public boolean isSnapshot() {
        return false;
    }

    public ContentValues createSavePageContentValues(int i, String str) {
        if (DEBUG) {
            Log.d("browser", "Tab.createSavePageContentValues()()--->id = " + i + ", file = " + str);
        }
        if (this.mMainView == null) {
            return null;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", this.mCurrentState.mTitle);
        contentValues.put("url", this.mCurrentState.mUrl);
        contentValues.put("date_created", Long.valueOf(System.currentTimeMillis()));
        contentValues.put("favicon", compressBitmap(getFavicon()));
        contentValues.put("thumbnail", compressBitmap(Controller.createScreenshot(this.mMainView, Controller.getDesiredThumbnailWidth(this.mContext), Controller.getDesiredThumbnailHeight(this.mContext))));
        contentValues.put("progress", (Integer) 0);
        contentValues.put("is_done", (Integer) 0);
        contentValues.put("job_id", Integer.valueOf(i));
        contentValues.put("viewstate_path", str);
        return contentValues;
    }

    private static class SaveCallback implements ValueCallback<String> {
        String mResult;

        @Override
        public void onReceiveValue(String str) {
            this.mResult = str;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public byte[] compressBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public void loadUrl(String str, Map<String, String> map) {
        if (DEBUG) {
            Log.d("browser", "Tab.loadUrl()()--->url = " + str + ", headers = " + map);
        }
        if (this.mMainView != null) {
            this.mBrowserUrlExt = Extensions.getUrlPlugin(this.mContext);
            String strCheckAndTrimUrl = this.mBrowserUrlExt.checkAndTrimUrl(str);
            this.mPageLoadProgress = 5;
            this.mInPageLoad = true;
            this.mCurrentState = new PageState(this.mContext, false, strCheckAndTrimUrl, null);
            this.mWebViewController.onPageStarted(this, this.mMainView, null);
            if (map == null) {
                map = new HashMap<>();
            }
            map.put(Browser.HEADER, Browser.UAPROF);
            this.mMainView.loadUrl(strCheckAndTrimUrl, map);
        }
    }

    public void disableUrlOverridingForLoad() {
        this.mDisableOverrideUrlLoading = true;
    }

    protected void capture() {
        TabControl.OnThumbnailUpdatedListener onThumbnailUpdatedListener;
        if (this.mMainView == null || this.mCapture == null || this.mMainView.getContentWidth() <= 0 || this.mMainView.getContentHeight() <= 0) {
            return;
        }
        Canvas canvas = new Canvas(this.mCapture);
        int scrollX = this.mMainView.getScrollX();
        int scrollY = this.mMainView.getScrollY() + this.mMainView.getVisibleTitleHeight();
        int iSave = canvas.save();
        canvas.translate(-scrollX, -scrollY);
        float width = this.mCaptureWidth / this.mMainView.getWidth();
        if (DEBUG) {
            Log.d("browser", "Tab.capture()--->left = " + scrollX + ", top = " + scrollY + ", scale = " + width);
        }
        canvas.scale(width, width, scrollX, scrollY);
        if (this.mMainView instanceof BrowserWebView) {
            ((BrowserWebView) this.mMainView).drawContent(canvas);
        } else {
            this.mMainView.draw(canvas);
        }
        canvas.restoreToCount(iSave);
        canvas.drawRect(0.0f, 0.0f, 1.0f, this.mCapture.getHeight(), sAlphaPaint);
        canvas.drawRect(this.mCapture.getWidth() - 1, 0.0f, this.mCapture.getWidth(), this.mCapture.getHeight(), sAlphaPaint);
        canvas.drawRect(0.0f, 0.0f, this.mCapture.getWidth(), 1.0f, sAlphaPaint);
        canvas.drawRect(0.0f, this.mCapture.getHeight() - 1, this.mCapture.getWidth(), this.mCapture.getHeight(), sAlphaPaint);
        canvas.setBitmap(null);
        this.mHandler.removeMessages(42);
        persistThumbnail();
        TabControl tabControl = this.mWebViewController.getTabControl();
        if (tabControl != null && (onThumbnailUpdatedListener = tabControl.getOnThumbnailUpdatedListener()) != null) {
            onThumbnailUpdatedListener.onThumbnailUpdated(this);
        }
    }

    @Override
    public void onNewPicture(WebView webView, Picture picture) {
        if ((this.mWebViewController instanceof Controller) && ((Controller) this.mWebViewController).getUi().isWebShowing()) {
            postCapture();
        }
    }

    private void postCapture() {
        if (!this.mHandler.hasMessages(42)) {
            this.mHandler.sendEmptyMessageDelayed(42, 100L);
        }
    }

    public boolean canGoBack() {
        if (this.mMainView != null) {
            return this.mMainView.canGoBack();
        }
        return false;
    }

    public boolean canGoForward() {
        if (this.mMainView != null) {
            return this.mMainView.canGoForward();
        }
        return false;
    }

    public void goBack() {
        if (this.mMainView != null) {
            this.mMainView.goBack();
        }
    }

    public void goForward() {
        if (this.mMainView != null) {
            this.mMainView.goForward();
        }
    }

    protected void persistThumbnail() {
        DataController.getInstance(this.mContext).saveThumbnail(this);
    }

    protected void deleteThumbnail() {
        DataController.getInstance(this.mContext).deleteThumbnail(this);
    }

    void updateCaptureFromBlob(byte[] bArr) {
        synchronized (this) {
            if (this.mCapture == null) {
                return;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
            Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(byteBufferWrap.array(), byteBufferWrap.arrayOffset(), byteBufferWrap.capacity(), options);
            if (bitmapDecodeByteArray == null) {
                return;
            }
            try {
                this.mCapture = Bitmap.createScaledBitmap(bitmapDecodeByteArray, this.mCapture.getWidth(), this.mCapture.getHeight(), true);
            } catch (RuntimeException e) {
                Log.e("Tab", "Load capture has mismatched sizes; buffer: " + byteBufferWrap.capacity() + " blob: " + bArr.length + "capture: " + this.mCapture.getByteCount());
                throw e;
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(this.mId);
        sb.append(") has parent: ");
        if (getParent() != null) {
            sb.append("true[");
            sb.append(getParent().getId());
            sb.append("]");
        } else {
            sb.append("false");
        }
        sb.append(", incog: ");
        sb.append(isPrivateBrowsingEnabled());
        if (!isPrivateBrowsingEnabled()) {
            sb.append(", title: ");
            sb.append(getTitle());
            sb.append(", url: ");
            sb.append(getUrl());
        }
        return sb.toString();
    }

    private void handleProceededAfterSslError(SslError sslError) {
        if (sslError.getUrl().equals(this.mCurrentState.mUrl)) {
            setSecurityState(SecurityState.SECURITY_STATE_BAD_CERTIFICATE);
            this.mCurrentState.mSslCertificateError = sslError;
        } else if (getSecurityState() == SecurityState.SECURITY_STATE_SECURE) {
            setSecurityState(SecurityState.SECURITY_STATE_MIXED);
        }
    }

    public void setAcceptThirdPartyCookies(boolean z) {
        CookieManager cookieManager = CookieManager.getInstance();
        if (this.mMainView != null) {
            cookieManager.setAcceptThirdPartyCookies(this.mMainView, z);
        }
        if (this.mSubView != null) {
            cookieManager.setAcceptThirdPartyCookies(this.mSubView, z);
        }
    }

    void addDatabaseItemId(int i, long j) {
        if (this.mSavePageJob == null) {
            this.mSavePageJob = new HashMap<>();
        }
        this.mSavePageJob.put(Integer.valueOf(i), Long.valueOf(j));
    }

    void removeDatabaseItemId(int i) {
        if (this.mSavePageJob != null) {
            this.mSavePageJob.remove(Integer.valueOf(i));
        }
    }

    boolean containsDatabaseItemId(int i) {
        if (this.mSavePageJob != null) {
            return this.mSavePageJob.containsKey(Integer.valueOf(i));
        }
        return false;
    }
}
