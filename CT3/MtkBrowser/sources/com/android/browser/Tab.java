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
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
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
import com.mediatek.browser.ext.IBrowserUrlExt;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
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

    static {
        sAlphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        sAlphaPaint.setColor(0);
    }

    public enum SecurityState {
        SECURITY_STATE_NOT_SECURE,
        SECURITY_STATE_SECURE,
        SECURITY_STATE_MIXED,
        SECURITY_STATE_BAD_CERTIFICATE;

        public static SecurityState[] valuesCustom() {
            return values();
        }
    }

    public static synchronized Bitmap getDefaultFavicon(Context context) {
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

        PageState(Context c, boolean incognito) {
            this.mIncognito = incognito;
            if (this.mIncognito) {
                this.mUrl = "browser:incognito";
                this.mOriginalUrl = "browser:incognito";
                this.mTitle = c.getString(R.string.new_incognito_tab);
            } else {
                this.mUrl = "";
                this.mOriginalUrl = "";
                this.mTitle = c.getString(R.string.new_tab);
            }
            this.mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
        }

        PageState(Context c, boolean incognito, String url, Bitmap favicon) {
            this.mIncognito = incognito;
            this.mUrl = url;
            this.mOriginalUrl = url;
            if (URLUtil.isHttpsUrl(url)) {
                this.mSecurityState = SecurityState.SECURITY_STATE_SECURE;
            } else {
                this.mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
            }
            this.mFavicon = favicon;
        }
    }

    private class ErrorDialog {
        public final String mDescription;
        public final int mError;
        public final int mTitle;

        ErrorDialog(int title, String desc, int error) {
            this.mTitle = title;
            this.mDescription = desc;
            this.mError = error;
        }
    }

    public void processNextError() {
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

    public void queueError(int err, String desc) {
        int i;
        if (this.mQueuedErrors == null) {
            this.mQueuedErrors = new LinkedList<>();
        }
        for (ErrorDialog d : this.mQueuedErrors) {
            if (d.mError == err) {
                return;
            }
        }
        if (err == -20000 && (desc == null || desc.isEmpty())) {
            desc = this.mContext.getString(R.string.open_saved_page_failed);
        }
        if (err == -14 || err == -20000) {
            i = R.string.browserFrameFileErrorLabel;
        } else {
            i = R.string.browserFrameNetworkErrorLabel;
        }
        ErrorDialog errDialog = new ErrorDialog(i, desc, err);
        this.mQueuedErrors.addLast(errDialog);
        if (this.mQueuedErrors.size() != 1 || !this.mInForeground) {
            return;
        }
        showError(errDialog);
    }

    private void showError(ErrorDialog errDialog) {
        if (!this.mInForeground || this.mIsErrorDialogShown) {
            return;
        }
        AlertDialog d = new AlertDialog.Builder(this.mContext).setTitle(errDialog.mTitle).setMessage(errDialog.mDescription).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).create();
        d.setOnDismissListener(this.mDialogListener);
        d.show();
        this.mIsErrorDialogShown = true;
    }

    public void syncCurrentState(WebView view, String url) {
        if (this.mWillBeClosed) {
            return;
        }
        if (DEBUG) {
            Log.d("browser", "Tab.syncCurrentState()()--->url = " + url + ", webview = " + view);
        }
        this.mCurrentState.mUrl = view.getUrl();
        if (this.mCurrentState.mUrl == null) {
            this.mCurrentState.mUrl = "";
        }
        if (this.mPageError != 0 && this.mCurrentState.mOriginalUrl != view.getOriginalUrl()) {
            this.mCurrentState.mUrl = url;
        }
        this.mCurrentState.mOriginalUrl = view.getOriginalUrl();
        this.mCurrentState.mTitle = view.getTitle();
        this.mCurrentState.mFavicon = view.getFavicon();
        if (!URLUtil.isHttpsUrl(this.mCurrentState.mUrl)) {
            this.mCurrentState.mSecurityState = SecurityState.SECURITY_STATE_NOT_SECURE;
            this.mCurrentState.mSslCertificateError = null;
        }
        this.mCurrentState.mIncognito = view.isPrivateBrowsingEnabled();
    }

    void setDeviceAccountLogin(DeviceAccountLogin login) {
        this.mDeviceAccountLogin = login;
    }

    DeviceAccountLogin getDeviceAccountLogin() {
        if (DEBUG) {
            Log.d("browser", "Tab.getDeviceAccountLogin()--->");
        }
        return this.mDeviceAccountLogin;
    }

    public void PopupWindowShown(boolean shown) {
        this.mSubWindowShown = shown;
    }

    private static class SubWindowClient extends WebViewClient {
        private final WebViewClient mClient;
        private final WebViewController mController;

        SubWindowClient(WebViewClient client, WebViewController controller) {
            this.mClient = client;
            this.mController = controller;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            this.mController.endActionMode();
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            this.mClient.doUpdateVisitedHistory(view, url, isReload);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return this.mClient.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            this.mClient.onReceivedSslError(view, handler, error);
        }

        @Override
        public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
            this.mClient.onReceivedClientCertRequest(view, request);
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            this.mClient.onReceivedHttpAuthRequest(view, handler, host, realm);
        }

        @Override
        public void onFormResubmission(WebView view, Message dontResend, Message resend) {
            this.mClient.onFormResubmission(view, dontResend, resend);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            this.mClient.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
            return this.mClient.shouldOverrideKeyEvent(view, event);
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            this.mClient.onUnhandledKeyEvent(view, event);
        }
    }

    private class SubWindowChromeClient extends WebChromeClient {
        private final WebChromeClient mClient;

        SubWindowChromeClient(WebChromeClient client) {
            this.mClient = client;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            this.mClient.onProgressChanged(view, newProgress);
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
            return this.mClient.onCreateWindow(view, dialog, userGesture, resultMsg);
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (window != Tab.this.mSubView) {
                Log.e("Tab", "Can't close the window");
            }
            Tab.this.mWebViewController.dismissSubWindow(Tab.this);
        }
    }

    Tab(WebViewController wvcontroller, WebView w) {
        this(wvcontroller, w, null);
    }

    Tab(WebViewController wvcontroller, Bundle state) {
        this(wvcontroller, null, state);
    }

    Tab(WebViewController wvcontroller, WebView w, Bundle state) {
        this.mWillBeClosed = false;
        this.mPageError = 0;
        this.mId = -1L;
        this.mDialogListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                Tab.this.mIsErrorDialogShown = false;
                Tab.this.processNextError();
            }
        };
        this.mIsErrorDialogShown = false;
        this.mWebViewClient = new WebViewClient() {
            private Message mDontResend;
            private Message mResend;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                TabControl tc = Tab.this.mWebViewController.getTabControl();
                if (tc != null) {
                    Log.d("browser", "[" + tc.getTabPosition(Tab.this) + "/" + tc.getTabCount() + "] onPageStarted url=" + url);
                }
                Tab.this.mInPageLoad = true;
                Tab.this.mUpdateThumbnail = true;
                Tab.this.mPageLoadProgress = 5;
                Tab.this.mCurrentState = new PageState(Tab.this.mContext, view.isPrivateBrowsingEnabled(), url, favicon);
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
                Tab.this.mWebViewController.onPageStarted(Tab.this, view, favicon);
                Tab.this.updateBookmarkedStatus();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                TabControl tc = Tab.this.mWebViewController.getTabControl();
                if (tc != null) {
                    Log.d("browser", "[" + tc.getTabPosition(Tab.this) + "/" + tc.getTabCount() + "] onPageFinished url=" + url);
                }
                Tab.this.mDisableOverrideUrlLoading = false;
                if (!Tab.this.isPrivateBrowsingEnabled()) {
                    LogTag.logPageFinishedLoading(url, SystemClock.uptimeMillis() - Tab.this.mLoadStartTime);
                }
                Tab.this.syncCurrentState(view, url);
                if (Tab.this.mCurrentState.mIsDownload) {
                    Tab.this.mCurrentState.mUrl = Tab.this.mCurrentState.mOriginalUrl;
                    if (Tab.this.mCurrentState.mUrl == null) {
                        Tab.this.mCurrentState.mUrl = "";
                    }
                }
                if (url != null && url.equals(Tab.this.mSavePageUrl)) {
                    Tab.this.mCurrentState.mTitle = Tab.this.mSavePageTitle;
                    Tab.this.mCurrentState.mUrl = Tab.this.mSavePageUrl;
                }
                if (url != null && url.startsWith("about:blank")) {
                    Tab.this.mCurrentState.mFavicon = Tab.getDefaultFavicon(Tab.this.mContext);
                }
                Tab.this.mWebViewController.onPageFinished(Tab.this);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!Tab.this.mDisableOverrideUrlLoading && Tab.this.mInForeground) {
                    return Tab.this.mWebViewController.shouldOverrideUrlLoading(Tab.this, view, url);
                }
                return false;
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                boolean zIsAboutUrl;
                if (url == null || url.length() <= 0 || Tab.this.mCurrentState.mSecurityState != SecurityState.SECURITY_STATE_SECURE) {
                    return;
                }
                if (URLUtil.isHttpsUrl(url) || URLUtil.isDataUrl(url)) {
                    zIsAboutUrl = true;
                } else {
                    zIsAboutUrl = URLUtil.isAboutUrl(url);
                }
                if (zIsAboutUrl) {
                    return;
                }
                Tab.this.mCurrentState.mSecurityState = SecurityState.SECURITY_STATE_MIXED;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d("Tab", "error code: " + errorCode + " url: " + failingUrl);
                Tab.this.mPageError = errorCode;
                Tab.this.mWebViewController.sendErrorCode(errorCode, failingUrl);
                if (errorCode == -2 || errorCode == -6 || errorCode == -12 || errorCode == -10 || errorCode == -13) {
                    return;
                }
                Tab.this.queueError(errorCode, description);
                if (Tab.this.isPrivateBrowsingEnabled()) {
                    return;
                }
                Log.e("Tab", "onReceivedError " + errorCode + " " + failingUrl + " " + description);
            }

            @Override
            public void onFormResubmission(WebView view, Message dontResend, Message resend) {
                if (!Tab.this.mInForeground) {
                    dontResend.sendToTarget();
                    return;
                }
                if (this.mDontResend != null) {
                    Log.w("Tab", "onFormResubmission should not be called again while dialog is still up");
                    dontResend.sendToTarget();
                } else {
                    this.mDontResend = dontResend;
                    this.mResend = resend;
                    new AlertDialog.Builder(Tab.this.mContext).setTitle(R.string.browserFrameFormResubmitLabel).setMessage(R.string.browserFrameFormResubmitMessage).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (AnonymousClass2.this.mResend == null) {
                                return;
                            }
                            AnonymousClass2.this.mResend.sendToTarget();
                            AnonymousClass2.this.mResend = null;
                            AnonymousClass2.this.mDontResend = null;
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (AnonymousClass2.this.mDontResend == null) {
                                return;
                            }
                            AnonymousClass2.this.mDontResend.sendToTarget();
                            AnonymousClass2.this.mResend = null;
                            AnonymousClass2.this.mDontResend = null;
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (AnonymousClass2.this.mDontResend == null) {
                                return;
                            }
                            AnonymousClass2.this.mDontResend.sendToTarget();
                            AnonymousClass2.this.mResend = null;
                            AnonymousClass2.this.mDontResend = null;
                        }
                    }).show();
                }
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                Tab.this.mWebViewController.doUpdateVisitedHistory(Tab.this, isReload);
            }

            @Override
            public void onReceivedSslError(final WebView view, final SslErrorHandler handler, final SslError error) {
                if (!Tab.this.mInForeground) {
                    handler.cancel();
                    Tab.this.setSecurityState(SecurityState.SECURITY_STATE_NOT_SECURE);
                } else if (Tab.this.mSettings.showSecurityWarnings()) {
                    new AlertDialog.Builder(Tab.this.mContext).setTitle(R.string.security_warning).setMessage(R.string.ssl_warnings_header).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(R.string.ssl_continue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            handler.proceed();
                            Tab.this.handleProceededAfterSslError(error);
                        }
                    }).setNeutralButton(R.string.view_certificate, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Tab.this.mWebViewController.showSslCertificateOnError(view, handler, error);
                        }
                    }).setNegativeButton(R.string.ssl_go_back, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            handler.cancel();
                            Tab.this.setSecurityState(SecurityState.SECURITY_STATE_NOT_SECURE);
                            Tab.this.mWebViewController.onUserCanceledSsl(Tab.this);
                        }
                    }).show();
                } else {
                    handler.proceed();
                }
            }

            @Override
            public void onReceivedClientCertRequest(WebView view, final ClientCertRequest request) {
                if (!Tab.this.mInForeground) {
                    request.ignore();
                } else {
                    KeyChain.choosePrivateKeyAlias(Tab.this.mWebViewController.getActivity(), new KeyChainAliasCallback() {
                        @Override
                        public void alias(String alias) {
                            if (alias == null) {
                                request.cancel();
                            } else {
                                new KeyChainLookup(Tab.this.mContext, request, alias).execute(new Void[0]);
                            }
                        }
                    }, request.getKeyTypes(), request.getPrincipals(), request.getHost(), request.getPort(), null);
                }
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                Tab.this.mWebViewController.onReceivedHttpAuthRequest(Tab.this, view, handler, host, realm);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return HomeProvider.shouldInterceptRequest(Tab.this.mContext, url);
            }

            @Override
            public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
                if (!Tab.this.mInForeground) {
                    return false;
                }
                return Tab.this.mWebViewController.shouldOverrideKeyEvent(event);
            }

            @Override
            public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
                if (!Tab.this.mInForeground || Tab.this.mWebViewController.onUnhandledKeyEvent(event)) {
                    return;
                }
                super.onUnhandledKeyEvent(view, event);
            }

            @Override
            public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
                new DeviceAccountLogin(Tab.this.mWebViewController.getActivity(), view, Tab.this, Tab.this.mWebViewController).handleLogin(realm, account, args);
            }
        };
        this.mSubWindowShown = false;
        this.mWebChromeClient = new WebChromeClient() {

            private static final int[] f4androidwebkitConsoleMessage$MessageLevelSwitchesValues = null;

            private static int[] m182getandroidwebkitConsoleMessage$MessageLevelSwitchesValues() {
                if (f4androidwebkitConsoleMessage$MessageLevelSwitchesValues != null) {
                    return f4androidwebkitConsoleMessage$MessageLevelSwitchesValues;
                }
                int[] iArr = new int[ConsoleMessage.MessageLevel.values().length];
                try {
                    iArr[ConsoleMessage.MessageLevel.DEBUG.ordinal()] = 1;
                } catch (NoSuchFieldError e) {
                }
                try {
                    iArr[ConsoleMessage.MessageLevel.ERROR.ordinal()] = 2;
                } catch (NoSuchFieldError e2) {
                }
                try {
                    iArr[ConsoleMessage.MessageLevel.LOG.ordinal()] = 3;
                } catch (NoSuchFieldError e3) {
                }
                try {
                    iArr[ConsoleMessage.MessageLevel.TIP.ordinal()] = 4;
                } catch (NoSuchFieldError e4) {
                }
                try {
                    iArr[ConsoleMessage.MessageLevel.WARNING.ordinal()] = 5;
                } catch (NoSuchFieldError e5) {
                }
                f4androidwebkitConsoleMessage$MessageLevelSwitchesValues = iArr;
                return iArr;
            }

            private void createWindow(boolean dialog, Message msg) {
                WebView.WebViewTransport transport = (WebView.WebViewTransport) msg.obj;
                if (dialog) {
                    Tab.this.createSubWindow();
                    Tab.this.mWebViewController.attachSubWindow(Tab.this);
                    transport.setWebView(Tab.this.mSubView);
                } else {
                    Tab newTab = Tab.this.mWebViewController.openTab(null, Tab.this, true, true);
                    transport.setWebView(newTab.getWebView());
                }
                msg.sendToTarget();
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {
                if (!Tab.this.mInForeground) {
                    return false;
                }
                if (dialog && Tab.this.mSubView != null) {
                    new AlertDialog.Builder(Tab.this.mContext).setTitle(R.string.too_many_subwindows_dialog_title).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.too_many_subwindows_dialog_message).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
                    return false;
                }
                if (!Tab.this.mWebViewController.getTabControl().canCreateNewTab()) {
                    new AlertDialog.Builder(Tab.this.mContext).setTitle(R.string.too_many_windows_dialog_title).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.too_many_windows_dialog_message).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
                    return false;
                }
                if (userGesture) {
                    createWindow(dialog, resultMsg);
                    return true;
                }
                if (!Tab.this.mSubWindowShown) {
                    Tab.this.mWebViewController.onShowPopupWindowAttempt(Tab.this, dialog, resultMsg);
                    return true;
                }
                new AlertDialog.Builder(Tab.this.mContext).setTitle(R.string.too_many_subwindows_dialog_title).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.too_many_subwindows_dialog_message).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
                return false;
            }

            @Override
            public void onRequestFocus(WebView view) {
                if (Tab.this.mInForeground) {
                    return;
                }
                Tab.this.mWebViewController.switchToTab(Tab.this);
            }

            @Override
            public void onCloseWindow(WebView window) {
                if (Tab.this.mParent == null) {
                    return;
                }
                if (Tab.this.mInForeground) {
                    Tab.this.mWebViewController.switchToTab(Tab.this.mParent);
                }
                Tab.this.mWebViewController.closeTab(Tab.this);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Tab.this.mWebViewController.getTabControl().setActiveTab(Tab.this);
                return false;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                Tab.this.mWebViewController.getTabControl().setActiveTab(Tab.this);
                return false;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                Tab.this.mWebViewController.getTabControl().setActiveTab(Tab.this);
                return false;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Tab.this.mPageLoadProgress = newProgress;
                Tab.this.mPageError = 0;
                if (newProgress == 100) {
                    Tab.this.mInPageLoad = false;
                    Tab.this.syncCurrentState(view, view.getUrl());
                }
                Tab.this.mWebViewController.onProgressChanged(Tab.this);
                if (!Tab.this.mUpdateThumbnail || newProgress != 100) {
                    return;
                }
                Tab.this.mUpdateThumbnail = false;
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                Tab.this.mCurrentState.mTitle = title;
                Tab.this.mWebViewController.onReceivedTitle(Tab.this, title);
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                Tab.this.mCurrentState.mFavicon = icon;
                Tab.this.mWebViewController.onFavicon(Tab.this, view, icon);
            }

            @Override
            public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
                ContentResolver cr = Tab.this.mContext.getContentResolver();
                if (precomposed && Tab.this.mTouchIconLoader != null) {
                    Tab.this.mTouchIconLoader.cancel(false);
                    Tab.this.mTouchIconLoader = null;
                }
                if (Tab.this.mTouchIconLoader != null) {
                    return;
                }
                Tab.this.mTouchIconLoader = new DownloadTouchIcon(Tab.this, Tab.this.mContext, cr, view);
                Tab.this.mTouchIconLoader.execute(url);
            }

            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                Activity activity = Tab.this.mWebViewController.getActivity();
                if (activity == null) {
                    return;
                }
                onShowCustomView(view, activity.getRequestedOrientation(), callback);
            }

            @Override
            public void onShowCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback callback) {
                if (Tab.this.mInForeground) {
                    Tab.this.mWebViewController.showCustomView(Tab.this, view, requestedOrientation, callback);
                }
            }

            @Override
            public void onHideCustomView() {
                if (Tab.this.mInForeground) {
                    Tab.this.mWebViewController.hideCustomView();
                }
            }

            @Override
            public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota, long estimatedSize, long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
                Tab.this.mSettings.getWebStorageSizeManager().onExceededDatabaseQuota(url, databaseIdentifier, currentQuota, estimatedSize, totalUsedQuota, quotaUpdater);
            }

            public void onReachedMaxAppCacheSize(long spaceNeeded, long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
                Tab.this.mSettings.getWebStorageSizeManager().onReachedMaxAppCacheSize(spaceNeeded, totalUsedQuota, quotaUpdater);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (!Tab.this.mInForeground) {
                    return;
                }
                Tab.this.getGeolocationPermissionsPrompt().show(origin, callback);
            }

            @Override
            public void onGeolocationPermissionsHidePrompt() {
                if (!Tab.this.mInForeground || Tab.this.mGeolocationPermissionsPrompt == null) {
                    return;
                }
                Tab.this.mGeolocationPermissionsPrompt.hide();
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (Tab.this.mInForeground) {
                    Tab.this.getPermissionsPrompt().show(request);
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (!Tab.this.mInForeground || Tab.this.mPermissionsPrompt == null) {
                    return;
                }
                Tab.this.mPermissionsPrompt.hide();
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
                if (Tab.this.isPrivateBrowsingEnabled()) {
                    return true;
                }
                String message = "Console: " + consoleMessage.message() + " " + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber();
                switch (m182getandroidwebkitConsoleMessage$MessageLevelSwitchesValues()[consoleMessage.messageLevel().ordinal()]) {
                    case 1:
                        Log.d("browser", message);
                        return true;
                    case 2:
                        Log.e("browser", message);
                        return true;
                    case 3:
                        Log.i("browser", message);
                        return true;
                    case 4:
                        Log.v("browser", message);
                        return true;
                    case 5:
                        Log.w("browser", message);
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
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
                if (Tab.this.mInForeground) {
                    Tab.this.mWebViewController.showFileChooser(callback, params);
                    return true;
                }
                return false;
            }

            @Override
            public void getVisitedHistory(ValueCallback<String[]> callback) {
                Tab.this.mWebViewController.getVisitedHistory(callback);
            }
        };
        this.mIsBookmarkCallback = new DataController.OnQueryUrlIsBookmark() {
            @Override
            public void onQueryUrlIsBookmark(String url, boolean isBookmark) {
                if (!Tab.this.mCurrentState.mUrl.equals(url)) {
                    return;
                }
                Tab.this.mCurrentState.mIsBookmarkedSite = isBookmark;
                Tab.this.mWebViewController.bookmarkedStatusHasChanged(Tab.this);
            }
        };
        this.callback = null;
        this.mBrowserUrlExt = null;
        if (DEBUG) {
            Log.d("browser", "Tab()--->Constructor()--->WebView is " + (w == null ? "null" : "not null") + ", Bundle is " + (state == null ? "null" : "not null"));
        }
        this.mSavePageJob = new HashMap<>();
        this.mWebViewController = wvcontroller;
        this.mContext = this.mWebViewController.getContext();
        this.mSettings = BrowserSettings.getInstance();
        this.mDataController = DataController.getInstance(this.mContext);
        this.mCurrentState = new PageState(this.mContext, w != null ? w.isPrivateBrowsingEnabled() : false);
        this.mInPageLoad = false;
        this.mInForeground = false;
        this.mDownloadListener = new BrowserDownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, String referer, long contentLength) {
                String mimetype2 = MimeTypeMap.getSingleton().remapGenericMimeTypePublic(mimetype, url, contentDisposition);
                Tab.this.mCurrentState.mIsDownload = true;
                Tab.this.mWebViewController.onDownloadStart(Tab.this, url, userAgent, contentDisposition, mimetype2, referer, contentLength);
            }
        };
        this.mWebBackForwardListClient = new WebBackForwardListClient() {
        };
        this.mCaptureWidth = this.mContext.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_width);
        this.mCaptureHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.tab_thumbnail_height);
        updateShouldCaptureThumbnails();
        restoreState(state);
        if (getId() == -1) {
            this.mId = TabControl.getNextId();
        }
        setWebView(w);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                switch (m.what) {
                    case 42:
                        Tab.this.capture();
                        break;
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
        }
        synchronized (this) {
            this.mCapture = null;
            deleteThumbnail();
        }
    }

    public void setController(WebViewController ctl) {
        this.mWebViewController = ctl;
        updateShouldCaptureThumbnails();
    }

    public long getId() {
        return this.mId;
    }

    void setWebView(WebView w) {
        setWebView(w, true);
    }

    void setWebView(WebView w, boolean restore) {
        if (DEBUG) {
            Log.d("browser", "Tab.setWebView()--->webview = " + w + ", restore = " + restore);
        }
        if (this.mMainView == w) {
            return;
        }
        if (this.mGeolocationPermissionsPrompt != null) {
            this.mGeolocationPermissionsPrompt.hide();
        }
        if (this.mPermissionsPrompt != null) {
            this.mPermissionsPrompt.hide();
        }
        this.mWebViewController.onSetWebView(this, w);
        if (this.mMainView != null) {
            this.mMainView.setPictureListener(null);
            if (w != null) {
                syncCurrentState(w, null);
            } else {
                this.mCurrentState = new PageState(this.mContext, false);
            }
        }
        this.mMainView = w;
        if (this.mMainView == null) {
            return;
        }
        this.mMainView.setWebViewClient(this.mWebViewClient);
        this.mMainView.setWebChromeClient(this.mWebChromeClient);
        this.mMainView.setDownloadListener(this.mDownloadListener);
        TabControl tc = this.mWebViewController.getTabControl();
        if (tc != null && tc.getOnThumbnailUpdatedListener() != null) {
            this.mMainView.setPictureListener(this);
        }
        if (!restore || this.mSavedState == null) {
            return;
        }
        restoreUserAgent();
        WebBackForwardList restoredState = this.mMainView.restoreState(this.mSavedState);
        if (restoredState == null || restoredState.getSize() == 0) {
            Log.w("Tab", "Failed to restore WebView state!");
            loadUrl(this.mCurrentState.mOriginalUrl, null);
        }
        this.mSavedState = null;
    }

    void destroy() {
        CancelSavePageTask cancelSavePageTask = null;
        if (DEBUG) {
            Log.d("browser", "Tab.destroy--->" + this.mMainView);
        }
        if (this.mMainView != null) {
            dismissSubWindow();
            WebView webView = this.mMainView;
            setWebView(null);
            webView.destroy();
        }
        if (this.mSavePageJob == null || this.mSavePageJob.size() == 0) {
            return;
        }
        Toast.makeText(this.mContext, R.string.saved_page_failed, 1).show();
        new CancelSavePageTask(this, cancelSavePageTask).execute(new Void[0]);
    }

    private class CancelSavePageTask extends AsyncTask<Void, Void, Void> {
        CancelSavePageTask(Tab this$0, CancelSavePageTask cancelSavePageTask) {
            this();
        }

        private CancelSavePageTask() {
        }

        @Override
        public Void doInBackground(Void... params) {
            if (Tab.DEBUG) {
                Log.d("browser", "Tab()--->CancelSavePageTask()--->doInBackground()");
            }
            Notification.Builder builder = new Notification.Builder(Tab.this.mContext);
            NotificationManager nm = (NotificationManager) Tab.this.mContext.getSystemService("notification");
            Set<Map.Entry<Integer, Long>> items = Tab.this.mSavePageJob.entrySet();
            ArrayList<ContentProviderOperation> opList = new ArrayList<>();
            Tab.this.mContext.getContentResolver();
            for (Map.Entry<Integer, Long> entry : items) {
                long id = entry.getValue().longValue();
                int job = entry.getKey().intValue();
                builder.setSmallIcon(R.drawable.ic_save_page_notification_fail);
                builder.setContentText(Tab.this.mContext.getText(R.string.saved_page_failed));
                builder.setOngoing(false);
                builder.setContentIntent(null);
                builder.setTicker(Tab.this.mContext.getText(R.string.saved_page_failed));
                nm.notify(job, builder.build());
                opList.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(SnapshotProvider.Snapshots.CONTENT_URI, id)).build());
            }
            try {
                Tab.this.mContext.getContentResolver().applyBatch("com.android.browser.snapshots", opList);
                return null;
            } catch (OperationApplicationException ope) {
                Log.e("Tab", "Failed to delete save page. OperationApplicationException: " + ope.getMessage());
                return null;
            } catch (RemoteException re) {
                Log.e("Tab", "Failed to delete save page. RemoteException: " + re.getMessage());
                return null;
            }
        }

        @Override
        public void onPostExecute(Void result) {
            if (Tab.this.mSavePageJob == null) {
                return;
            }
            Tab.this.mSavePageJob.clear();
            Tab.this.mSavePageJob = null;
        }
    }

    void removeFromTree() {
        if (DEBUG) {
            Log.d("browser", "Tab.removeFromTree()--->tab this = " + this);
        }
        if (this.mChildren != null) {
            for (Tab t : this.mChildren) {
                t.setParent(null);
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
                public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, String referer, long contentLength) {
                    Tab.this.mWebViewController.onDownloadStart(Tab.this, url, userAgent, contentDisposition, MimeTypeMap.getSingleton().remapGenericMimeTypePublic(mimetype, url, contentDisposition), referer, contentLength);
                    if (Tab.this.mSubView.copyBackForwardList().getSize() != 0) {
                        return;
                    }
                    Tab.this.mWebViewController.dismissSubWindow(Tab.this);
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
        if (this.mSubView == null) {
            return;
        }
        this.mWebViewController.endActionMode();
        this.mSubView.destroy();
        this.mSubView = null;
        this.mSubViewContainer = null;
    }

    void setParent(Tab parent) {
        if (parent == this) {
            throw new IllegalStateException("Cannot set parent to self!");
        }
        this.mParent = parent;
        if (this.mSavedState != null) {
            if (parent == null) {
                this.mSavedState.remove("parentTab");
            } else {
                this.mSavedState.putLong("parentTab", parent.getId());
            }
        }
        if (parent != null && this.mSettings.hasDesktopUseragent(parent.getWebView()) != this.mSettings.hasDesktopUseragent(getWebView())) {
            this.mSettings.toggleDesktopUseragent(getWebView());
        }
        if (parent == null || parent.getId() != getId()) {
        } else {
            throw new IllegalStateException("Parent has same ID as child!");
        }
    }

    public Tab getParent() {
        return this.mParent;
    }

    void addChildTab(Tab child) {
        if (DEBUG) {
            Log.d("browser", "Tab.addChildTab()--->Tab child = " + child);
        }
        if (this.mChildren == null) {
            this.mChildren = new Vector<>();
        }
        this.mChildren.add(child);
        child.setParent(this);
    }

    void resume() {
        if (this.mMainView == null) {
            return;
        }
        setupHwAcceleration(this.mMainView);
        this.mMainView.onResume();
        if (this.mSubView == null) {
            return;
        }
        this.mSubView.onResume();
    }

    private void setupHwAcceleration(View web) {
        if (web == null) {
            return;
        }
        BrowserSettings settings = BrowserSettings.getInstance();
        if (settings.isHardwareAccelerated()) {
            web.setLayerType(0, null);
        } else {
            web.setLayerType(1, null);
        }
    }

    void pause() {
        if (this.mMainView == null) {
            return;
        }
        this.mMainView.onPause();
        if (this.mSubView == null) {
            return;
        }
        this.mSubView.onPause();
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
        if (this.mSubView == null) {
            return;
        }
        this.mSubView.setOnCreateContextMenuListener(null);
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

    void setViewContainer(View container) {
        this.mContainer = container;
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

    void setSubWebView(WebView subView) {
        this.mSubView = subView;
    }

    View getSubViewContainer() {
        return this.mSubViewContainer;
    }

    void setSubViewContainer(View subViewContainer) {
        this.mSubViewContainer = subViewContainer;
    }

    GeolocationPermissionsPrompt getGeolocationPermissionsPrompt() {
        if (this.mGeolocationPermissionsPrompt == null) {
            ViewStub stub = (ViewStub) this.mContainer.findViewById(R.id.geolocation_permissions_prompt);
            this.mGeolocationPermissionsPrompt = (GeolocationPermissionsPrompt) stub.inflate();
        }
        return this.mGeolocationPermissionsPrompt;
    }

    PermissionsPrompt getPermissionsPrompt() {
        if (this.mPermissionsPrompt == null) {
            ViewStub stub = (ViewStub) this.mContainer.findViewById(R.id.permissions_prompt);
            this.mPermissionsPrompt = (PermissionsPrompt) stub.inflate();
        }
        return this.mPermissionsPrompt;
    }

    String getAppId() {
        return this.mAppId;
    }

    void setAppId(String id) {
        this.mAppId = id;
    }

    boolean closeOnBack() {
        return this.mCloseOnBack;
    }

    void setCloseOnBack(boolean close) {
        this.mCloseOnBack = close;
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

    ErrorConsoleView getErrorConsole(boolean createIfNecessary) {
        if (createIfNecessary && this.mErrorConsole == null) {
            this.mErrorConsole = new ErrorConsoleView(this.mContext);
            this.mErrorConsole.setWebView(this.mMainView);
        }
        return this.mErrorConsole;
    }

    public void setSecurityState(SecurityState securityState) {
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
        WebBackForwardList savedList = this.mMainView.saveState(this.mSavedState);
        if (savedList == null || savedList.getSize() == 0) {
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

    private void restoreState(Bundle b) {
        if (DEBUG) {
            Log.d("browser", "Tab.restoreState()()---> bundle is " + b);
        }
        this.mSavedState = b;
        if (this.mSavedState == null) {
            return;
        }
        this.mId = b.getLong("ID");
        this.mAppId = b.getString("appid");
        this.mCloseOnBack = b.getBoolean("closeOnBack");
        restoreUserAgent();
        String url = b.getString("currentUrl");
        String title = b.getString("currentTitle");
        boolean incognito = b.getBoolean("privateBrowsingEnabled");
        this.mCurrentState = new PageState(this.mContext, incognito, url, null);
        this.mCurrentState.mTitle = title;
        synchronized (this) {
            if (this.mCapture != null) {
                DataController.getInstance(this.mContext).loadThumbnail(this);
            }
        }
    }

    private void restoreUserAgent() {
        if (this.mMainView == null || this.mSavedState == null || this.mSavedState.getBoolean("useragent") == this.mSettings.hasDesktopUseragent(this.mMainView)) {
            return;
        }
        this.mSettings.toggleDesktopUseragent(this.mMainView);
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

    public ContentValues createSavePageContentValues(int id, String file) {
        if (DEBUG) {
            Log.d("browser", "Tab.createSavePageContentValues()()--->id = " + id + ", file = " + file);
        }
        if (this.mMainView == null) {
            return null;
        }
        ContentValues values = new ContentValues();
        values.put("title", this.mCurrentState.mTitle);
        values.put("url", this.mCurrentState.mUrl);
        values.put("date_created", Long.valueOf(System.currentTimeMillis()));
        values.put("favicon", compressBitmap(getFavicon()));
        Bitmap screenshot = Controller.createScreenshot(this.mMainView, Controller.getDesiredThumbnailWidth(this.mContext), Controller.getDesiredThumbnailHeight(this.mContext));
        values.put("thumbnail", compressBitmap(screenshot));
        values.put("progress", (Integer) 0);
        values.put("is_done", (Integer) 0);
        values.put("job_id", Integer.valueOf(id));
        values.put("viewstate_path", file);
        return values;
    }

    private static class SaveCallback implements ValueCallback<String> {
        String mResult;

        private SaveCallback() {
        }

        @Override
        public void onReceiveValue(String value) {
            this.mResult = value;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public byte[] compressBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public void loadUrl(String url, Map<String, String> headers) {
        if (DEBUG) {
            Log.d("browser", "Tab.loadUrl()()--->url = " + url + ", headers = " + headers);
        }
        if (this.mMainView == null) {
            return;
        }
        this.mBrowserUrlExt = Extensions.getUrlPlugin(this.mContext);
        String url2 = this.mBrowserUrlExt.checkAndTrimUrl(url);
        this.mPageLoadProgress = 5;
        this.mInPageLoad = true;
        this.mCurrentState = new PageState(this.mContext, false, url2, null);
        this.mWebViewController.onPageStarted(this, this.mMainView, null);
        this.mMainView.loadUrl(url2, headers);
    }

    public void disableUrlOverridingForLoad() {
        this.mDisableOverrideUrlLoading = true;
    }

    protected void capture() {
        TabControl.OnThumbnailUpdatedListener updateListener;
        if (this.mMainView == null || this.mCapture == null || this.mMainView.getContentWidth() <= 0 || this.mMainView.getContentHeight() <= 0) {
            return;
        }
        Canvas c = new Canvas(this.mCapture);
        int left = this.mMainView.getScrollX();
        int top = this.mMainView.getScrollY() + this.mMainView.getVisibleTitleHeight();
        int state = c.save();
        c.translate(-left, -top);
        float scale = this.mCaptureWidth / this.mMainView.getWidth();
        if (DEBUG) {
            Log.d("browser", "Tab.capture()--->left = " + left + ", top = " + top + ", scale = " + scale);
        }
        c.scale(scale, scale, left, top);
        if (this.mMainView instanceof BrowserWebView) {
            ((BrowserWebView) this.mMainView).drawContent(c);
        } else {
            this.mMainView.draw(c);
        }
        c.restoreToCount(state);
        c.drawRect(0.0f, 0.0f, 1.0f, this.mCapture.getHeight(), sAlphaPaint);
        c.drawRect(this.mCapture.getWidth() - 1, 0.0f, this.mCapture.getWidth(), this.mCapture.getHeight(), sAlphaPaint);
        c.drawRect(0.0f, 0.0f, this.mCapture.getWidth(), 1.0f, sAlphaPaint);
        c.drawRect(0.0f, this.mCapture.getHeight() - 1, this.mCapture.getWidth(), this.mCapture.getHeight(), sAlphaPaint);
        c.setBitmap(null);
        this.mHandler.removeMessages(42);
        persistThumbnail();
        TabControl tc = this.mWebViewController.getTabControl();
        if (tc == null || (updateListener = tc.getOnThumbnailUpdatedListener()) == null) {
            return;
        }
        updateListener.onThumbnailUpdated(this);
    }

    @Override
    public void onNewPicture(WebView view, Picture picture) {
        postCapture();
    }

    private void postCapture() {
        if (this.mHandler.hasMessages(42)) {
            return;
        }
        this.mHandler.sendEmptyMessageDelayed(42, 100L);
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
        if (this.mMainView == null) {
            return;
        }
        this.mMainView.goBack();
    }

    public void goForward() {
        if (this.mMainView == null) {
            return;
        }
        this.mMainView.goForward();
    }

    protected void persistThumbnail() {
        DataController.getInstance(this.mContext).saveThumbnail(this);
    }

    protected void deleteThumbnail() {
        DataController.getInstance(this.mContext).deleteThumbnail(this);
    }

    void updateCaptureFromBlob(byte[] blob) {
        synchronized (this) {
            if (this.mCapture == null) {
                return;
            }
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inMutable = true;
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            ByteBuffer buffer = ByteBuffer.wrap(blob);
            Bitmap temp = BitmapFactory.decodeByteArray(buffer.array(), buffer.arrayOffset(), buffer.capacity(), opt);
            if (temp == null) {
                return;
            }
            try {
                this.mCapture = Bitmap.createScaledBitmap(temp, this.mCapture.getWidth(), this.mCapture.getHeight(), true);
            } catch (RuntimeException rex) {
                Log.e("Tab", "Load capture has mismatched sizes; buffer: " + buffer.capacity() + " blob: " + blob.length + "capture: " + this.mCapture.getByteCount());
                throw rex;
            }
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(100);
        builder.append(this.mId);
        builder.append(") has parent: ");
        if (getParent() != null) {
            builder.append("true[");
            builder.append(getParent().getId());
            builder.append("]");
        } else {
            builder.append("false");
        }
        builder.append(", incog: ");
        builder.append(isPrivateBrowsingEnabled());
        if (!isPrivateBrowsingEnabled()) {
            builder.append(", title: ");
            builder.append(getTitle());
            builder.append(", url: ");
            builder.append(getUrl());
        }
        return builder.toString();
    }

    public void handleProceededAfterSslError(SslError error) {
        if (error.getUrl().equals(this.mCurrentState.mUrl)) {
            setSecurityState(SecurityState.SECURITY_STATE_BAD_CERTIFICATE);
            this.mCurrentState.mSslCertificateError = error;
        } else {
            if (getSecurityState() != SecurityState.SECURITY_STATE_SECURE) {
                return;
            }
            setSecurityState(SecurityState.SECURITY_STATE_MIXED);
        }
    }

    public void setAcceptThirdPartyCookies(boolean accept) {
        CookieManager cookieManager = CookieManager.getInstance();
        if (this.mMainView != null) {
            cookieManager.setAcceptThirdPartyCookies(this.mMainView, accept);
        }
        if (this.mSubView == null) {
            return;
        }
        cookieManager.setAcceptThirdPartyCookies(this.mSubView, accept);
    }

    void addDatabaseItemId(int job, long id) {
        if (this.mSavePageJob == null) {
            this.mSavePageJob = new HashMap<>();
        }
        this.mSavePageJob.put(Integer.valueOf(job), Long.valueOf(id));
    }

    void removeDatabaseItemId(int job) {
        if (this.mSavePageJob == null) {
            return;
        }
        this.mSavePageJob.remove(Integer.valueOf(job));
    }

    boolean containsDatabaseItemId(int job) {
        if (this.mSavePageJob != null) {
            return this.mSavePageJob.containsKey(Integer.valueOf(job));
        }
        return false;
    }
}
