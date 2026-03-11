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

    class AnonymousClass2 extends WebViewClient {
        private Message mDontResend;
        private Message mResend;
        final Tab this$0;

        AnonymousClass2(Tab tab) {
            this.this$0 = tab;
        }

        @Override
        public void doUpdateVisitedHistory(WebView webView, String str, boolean z) {
            this.this$0.mWebViewController.doUpdateVisitedHistory(this.this$0, z);
        }

        @Override
        public void onFormResubmission(WebView webView, Message message, Message message2) {
            if (!this.this$0.mInForeground) {
                message.sendToTarget();
                return;
            }
            if (this.mDontResend != null) {
                Log.w("Tab", "onFormResubmission should not be called again while dialog is still up");
                message.sendToTarget();
            } else {
                this.mDontResend = message;
                this.mResend = message2;
                new AlertDialog.Builder(this.this$0.mContext).setTitle(2131493197).setMessage(2131493198).setPositiveButton(2131492964, new DialogInterface.OnClickListener(this) {
                    final AnonymousClass2 this$1;

                    {
                        this.this$1 = this;
                    }

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (this.this$1.mResend != null) {
                            this.this$1.mResend.sendToTarget();
                            this.this$1.mResend = null;
                            this.this$1.mDontResend = null;
                        }
                    }
                }).setNegativeButton(2131492963, new DialogInterface.OnClickListener(this) {
                    final AnonymousClass2 this$1;

                    {
                        this.this$1 = this;
                    }

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (this.this$1.mDontResend != null) {
                            this.this$1.mDontResend.sendToTarget();
                            this.this$1.mResend = null;
                            this.this$1.mDontResend = null;
                        }
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener(this) {
                    final AnonymousClass2 this$1;

                    {
                        this.this$1 = this;
                    }

                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if (this.this$1.mDontResend != null) {
                            this.this$1.mDontResend.sendToTarget();
                            this.this$1.mResend = null;
                            this.this$1.mDontResend = null;
                        }
                    }
                }).show();
            }
        }

        @Override
        public void onLoadResource(WebView webView, String str) {
            if (str == null || str.length() <= 0 || this.this$0.mCurrentState.mSecurityState != SecurityState.SECURITY_STATE_SECURE || URLUtil.isHttpsUrl(str) || URLUtil.isDataUrl(str) || URLUtil.isAboutUrl(str)) {
                return;
            }
            this.this$0.mCurrentState.mSecurityState = SecurityState.SECURITY_STATE_MIXED;
        }

        @Override
        public void onPageFinished(WebView webView, String str) {
            TabControl tabControl = this.this$0.mWebViewController.getTabControl();
            if (tabControl != null && Tab.DEBUG) {
                Log.d("browser", "Network_Issue [" + tabControl.getTabPosition(this.this$0) + "/" + tabControl.getTabCount() + "] onPageFinished url=" + str);
            }
            this.this$0.mDisableOverrideUrlLoading = false;
            if (!this.this$0.isPrivateBrowsingEnabled()) {
                LogTag.logPageFinishedLoading(str, SystemClock.uptimeMillis() - this.this$0.mLoadStartTime);
            }
            this.this$0.syncCurrentState(webView, str);
            if (this.this$0.mCurrentState.mIsDownload) {
                this.this$0.mCurrentState.mUrl = this.this$0.mCurrentState.mOriginalUrl;
                if (this.this$0.mCurrentState.mUrl == null) {
                    this.this$0.mCurrentState.mUrl = "";
                }
            }
            if (str != null && str.equals(this.this$0.mSavePageUrl)) {
                this.this$0.mCurrentState.mTitle = this.this$0.mSavePageTitle;
                this.this$0.mCurrentState.mUrl = this.this$0.mSavePageUrl;
            }
            if (str != null && str.startsWith("about:blank")) {
                this.this$0.mCurrentState.mFavicon = Tab.getDefaultFavicon(this.this$0.mContext);
            }
            this.this$0.mWebViewController.onPageFinished(this.this$0);
        }

        @Override
        public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
            TabControl tabControl = this.this$0.mWebViewController.getTabControl();
            if (tabControl != null && Tab.DEBUG) {
                Log.d("browser", "Network_Issue [" + tabControl.getTabPosition(this.this$0) + "/" + tabControl.getTabCount() + "] onPageStarted url=" + str);
            }
            this.this$0.mInPageLoad = true;
            this.this$0.mUpdateThumbnail = true;
            this.this$0.mPageLoadProgress = 5;
            this.this$0.mCurrentState = new PageState(this.this$0.mContext, webView.isPrivateBrowsingEnabled(), str, bitmap);
            this.this$0.mLoadStartTime = SystemClock.uptimeMillis();
            if (this.this$0.mTouchIconLoader != null) {
                this.this$0.mTouchIconLoader.mTab = null;
                this.this$0.mTouchIconLoader = null;
            }
            if (this.this$0.mErrorConsole != null) {
                this.this$0.mErrorConsole.clearErrorMessages();
                if (this.this$0.mWebViewController.shouldShowErrorConsole()) {
                    this.this$0.mErrorConsole.showConsole(2);
                }
            }
            if (this.this$0.mDeviceAccountLogin != null) {
                this.this$0.mDeviceAccountLogin.cancel();
                this.this$0.mDeviceAccountLogin = null;
                this.this$0.mWebViewController.hideAutoLogin(this.this$0);
            }
            this.this$0.mWebViewController.onPageStarted(this.this$0, webView, bitmap);
            this.this$0.updateBookmarkedStatus();
        }

        @Override
        public void onReceivedClientCertRequest(WebView webView, ClientCertRequest clientCertRequest) {
            if (this.this$0.mInForeground) {
                KeyChain.choosePrivateKeyAlias(this.this$0.mWebViewController.getActivity(), new KeyChainAliasCallback(this, clientCertRequest) {
                    final AnonymousClass2 this$1;
                    final ClientCertRequest val$request;

                    {
                        this.this$1 = this;
                        this.val$request = clientCertRequest;
                    }

                    @Override
                    public void alias(String str) {
                        if (str == null) {
                            this.val$request.cancel();
                        } else {
                            new KeyChainLookup(this.this$1.this$0.mContext, this.val$request, str).execute(new Void[0]);
                        }
                    }
                }, clientCertRequest.getKeyTypes(), clientCertRequest.getPrincipals(), clientCertRequest.getHost(), clientCertRequest.getPort(), null);
            } else {
                clientCertRequest.ignore();
            }
        }

        @Override
        public void onReceivedError(WebView webView, int i, String str, String str2) {
            if (Tab.DEBUG) {
                Log.d("Tab", "Network_Issue error code: " + i + " url: " + str2);
            }
            this.this$0.mPageError = i;
            this.this$0.mWebViewController.sendErrorCode(i, str2);
            if (i == -2 || i == -6 || i == -12 || i == -10 || i == -13) {
                return;
            }
            this.this$0.queueError(i, str);
            if (this.this$0.isPrivateBrowsingEnabled() || !Tab.DEBUG) {
                return;
            }
            Log.e("Tab", "onReceivedError " + i + " " + str2 + " " + str);
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView webView, HttpAuthHandler httpAuthHandler, String str, String str2) {
            this.this$0.mWebViewController.onReceivedHttpAuthRequest(this.this$0, webView, httpAuthHandler, str, str2);
        }

        @Override
        public void onReceivedHttpError(WebView webView, WebResourceRequest webResourceRequest, WebResourceResponse webResourceResponse) {
            if (webResourceRequest.isForMainFrame() && Tab.DEBUG && webResourceResponse != null) {
                Log.d("Tab", "Network_Issue http error code: " + webResourceResponse.getStatusCode() + " url: " + webResourceRequest.getUrl());
            }
        }

        @Override
        public void onReceivedLoginRequest(WebView webView, String str, String str2, String str3) {
            new DeviceAccountLogin(this.this$0.mWebViewController.getActivity(), webView, this.this$0, this.this$0.mWebViewController).handleLogin(str, str2, str3);
        }

        @Override
        public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
            if (Tab.DEBUG) {
                Log.d("Tab", "Network_Issue onReceivedSslError: " + sslError.toString());
            }
            if (!this.this$0.mInForeground) {
                sslErrorHandler.cancel();
                this.this$0.setSecurityState(SecurityState.SECURITY_STATE_NOT_SECURE);
            } else if (this.this$0.mSettings.showSecurityWarnings()) {
                new AlertDialog.Builder(this.this$0.mContext).setTitle(2131492971).setMessage(2131492969).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(2131492970, new DialogInterface.OnClickListener(this, sslErrorHandler, sslError) {
                    final AnonymousClass2 this$1;
                    final SslError val$error;
                    final SslErrorHandler val$handler;

                    {
                        this.this$1 = this;
                        this.val$handler = sslErrorHandler;
                        this.val$error = sslError;
                    }

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        this.val$handler.proceed();
                        this.this$1.this$0.handleProceededAfterSslError(this.val$error);
                    }
                }).setNeutralButton(2131492972, new DialogInterface.OnClickListener(this, webView, sslErrorHandler, sslError) {
                    final AnonymousClass2 this$1;
                    final SslError val$error;
                    final SslErrorHandler val$handler;
                    final WebView val$view;

                    {
                        this.this$1 = this;
                        this.val$view = webView;
                        this.val$handler = sslErrorHandler;
                        this.val$error = sslError;
                    }

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        this.this$1.this$0.mWebViewController.showSslCertificateOnError(this.val$view, this.val$handler, this.val$error);
                    }
                }).setNegativeButton(2131492973, new DialogInterface.OnClickListener(this) {
                    final AnonymousClass2 this$1;

                    {
                        this.this$1 = this;
                    }

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener(this, sslErrorHandler) {
                    final AnonymousClass2 this$1;
                    final SslErrorHandler val$handler;

                    {
                        this.this$1 = this;
                        this.val$handler = sslErrorHandler;
                    }

                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        this.val$handler.cancel();
                        this.this$1.this$0.setSecurityState(SecurityState.SECURITY_STATE_NOT_SECURE);
                        this.this$1.this$0.mWebViewController.onUserCanceledSsl(this.this$1.this$0);
                    }
                }).show();
            } else {
                sslErrorHandler.proceed();
            }
        }

        @Override
        public void onUnhandledKeyEvent(WebView webView, KeyEvent keyEvent) {
            if (this.this$0.mInForeground && !this.this$0.mWebViewController.onUnhandledKeyEvent(keyEvent)) {
                super.onUnhandledKeyEvent(webView, keyEvent);
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView webView, String str) {
            return HomeProvider.shouldInterceptRequest(this.this$0.mContext, str);
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView webView, KeyEvent keyEvent) {
            if (this.this$0.mInForeground) {
                return this.this$0.mWebViewController.shouldOverrideKeyEvent(keyEvent);
            }
            return false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String str) {
            if (this.this$0.mDisableOverrideUrlLoading || !this.this$0.mInForeground) {
                return false;
            }
            return this.this$0.mWebViewController.shouldOverrideUrlLoading(this.this$0, webView, str);
        }
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

    private class CancelSavePageTask extends AsyncTask<Void, Void, Void> {
        final Tab this$0;

        private CancelSavePageTask(Tab tab) {
            this.this$0 = tab;
        }

        @Override
        public Void doInBackground(Void... voidArr) {
            if (Tab.DEBUG) {
                Log.d("browser", "Tab()--->CancelSavePageTask()--->doInBackground()");
            }
            Notification.Builder builder = new Notification.Builder(this.this$0.mContext);
            NotificationManager notificationManager = (NotificationManager) this.this$0.mContext.getSystemService("notification");
            ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
            this.this$0.mContext.getContentResolver();
            for (Map.Entry<Integer, Long> entry : this.this$0.mSavePageJob.entrySet()) {
                long jLongValue = entry.getValue().longValue();
                int iIntValue = entry.getKey().intValue();
                builder.setSmallIcon(2130837579);
                builder.setContentText(this.this$0.mContext.getText(2131492919));
                builder.setOngoing(false);
                builder.setContentIntent(null);
                builder.setTicker(this.this$0.mContext.getText(2131492919));
                notificationManager.notify(iIntValue, builder.build());
                arrayList.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(SnapshotProvider.Snapshots.CONTENT_URI, jLongValue)).build());
            }
            try {
                this.this$0.mContext.getContentResolver().applyBatch("com.android.browser.snapshots", arrayList);
            } catch (OperationApplicationException e) {
                Log.e("Tab", "Failed to delete save page. OperationApplicationException: " + e.getMessage());
            } catch (RemoteException e2) {
                Log.e("Tab", "Failed to delete save page. RemoteException: " + e2.getMessage());
            }
            return null;
        }

        @Override
        public void onPostExecute(Void r3) {
            if (this.this$0.mSavePageJob != null) {
                this.this$0.mSavePageJob.clear();
                this.this$0.mSavePageJob = null;
            }
        }
    }

    private class ErrorDialog {
        public final String mDescription;
        public final int mError;
        public final int mTitle;
        final Tab this$0;

        ErrorDialog(Tab tab, int i, String str, int i2) {
            this.this$0 = tab;
            this.mTitle = i;
            this.mDescription = str;
            this.mError = i2;
        }
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
                this.mTitle = context.getString(2131492950);
            } else {
                this.mUrl = "";
                this.mOriginalUrl = "";
                this.mTitle = context.getString(2131492949);
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

    public enum SecurityState {
        SECURITY_STATE_NOT_SECURE,
        SECURITY_STATE_SECURE,
        SECURITY_STATE_MIXED,
        SECURITY_STATE_BAD_CERTIFICATE
    }

    private class SubWindowChromeClient extends WebChromeClient {
        private final WebChromeClient mClient;
        final Tab this$0;

        SubWindowChromeClient(Tab tab, WebChromeClient webChromeClient) {
            this.this$0 = tab;
            this.mClient = webChromeClient;
        }

        @Override
        public void onCloseWindow(WebView webView) {
            if (webView != this.this$0.mSubView) {
                Log.e("Tab", "Can't close the window");
            }
            this.this$0.mWebViewController.dismissSubWindow(this.this$0);
        }

        @Override
        public boolean onCreateWindow(WebView webView, boolean z, boolean z2, Message message) {
            return this.mClient.onCreateWindow(webView, z, z2, message);
        }

        @Override
        public void onProgressChanged(WebView webView, int i) {
            this.mClient.onProgressChanged(webView, i);
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
        public void doUpdateVisitedHistory(WebView webView, String str, boolean z) {
            this.mClient.doUpdateVisitedHistory(webView, str, z);
        }

        @Override
        public void onFormResubmission(WebView webView, Message message, Message message2) {
            this.mClient.onFormResubmission(webView, message, message2);
        }

        @Override
        public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
            this.mController.endActionMode();
        }

        @Override
        public void onReceivedClientCertRequest(WebView webView, ClientCertRequest clientCertRequest) {
            this.mClient.onReceivedClientCertRequest(webView, clientCertRequest);
        }

        @Override
        public void onReceivedError(WebView webView, int i, String str, String str2) {
            this.mClient.onReceivedError(webView, i, str, str2);
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView webView, HttpAuthHandler httpAuthHandler, String str, String str2) {
            this.mClient.onReceivedHttpAuthRequest(webView, httpAuthHandler, str, str2);
        }

        @Override
        public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
            this.mClient.onReceivedSslError(webView, sslErrorHandler, sslError);
        }

        @Override
        public void onUnhandledKeyEvent(WebView webView, KeyEvent keyEvent) {
            this.mClient.onUnhandledKeyEvent(webView, keyEvent);
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView webView, KeyEvent keyEvent) {
            return this.mClient.shouldOverrideKeyEvent(webView, keyEvent);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String str) {
            return this.mClient.shouldOverrideUrlLoading(webView, str);
        }
    }

    static {
        sAlphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        sAlphaPaint.setColor(0);
    }

    Tab(WebViewController webViewController, Bundle bundle) {
        this(webViewController, null, bundle);
    }

    Tab(WebViewController webViewController, WebView webView) {
        this(webViewController, webView, null);
    }

    Tab(WebViewController webViewController, WebView webView, Bundle bundle) {
        this.mWillBeClosed = false;
        this.mPageError = 0;
        this.mId = -1L;
        this.mDialogListener = new DialogInterface.OnDismissListener(this) {
            final Tab this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                this.this$0.mIsErrorDialogShown = false;
                this.this$0.processNextError();
            }
        };
        this.mIsErrorDialogShown = false;
        this.mWebViewClient = new AnonymousClass2(this);
        this.mSubWindowShown = false;
        this.mWebChromeClient = new WebChromeClient(this) {
            final Tab this$0;

            {
                this.this$0 = this;
            }

            private void createWindow(boolean z, Message message) {
                WebView.WebViewTransport webViewTransport = (WebView.WebViewTransport) message.obj;
                if (z) {
                    this.this$0.createSubWindow();
                    this.this$0.mWebViewController.attachSubWindow(this.this$0);
                    webViewTransport.setWebView(this.this$0.mSubView);
                } else {
                    webViewTransport.setWebView(this.this$0.mWebViewController.openTab(null, this.this$0, true, true).getWebView());
                }
                message.sendToTarget();
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                if (this.this$0.mInForeground) {
                    return this.this$0.mWebViewController.getDefaultVideoPoster();
                }
                return null;
            }

            @Override
            public View getVideoLoadingProgressView() {
                if (this.this$0.mInForeground) {
                    return this.this$0.mWebViewController.getVideoLoadingProgressView();
                }
                return null;
            }

            @Override
            public void getVisitedHistory(ValueCallback<String[]> valueCallback) {
                this.this$0.mWebViewController.getVisitedHistory(valueCallback);
            }

            @Override
            public void onCloseWindow(WebView webView2) {
                if (this.this$0.mParent != null) {
                    if (this.this$0.mInForeground) {
                        this.this$0.mWebViewController.switchToTab(this.this$0.mParent);
                    }
                    this.this$0.mWebViewController.closeTab(this.this$0);
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (this.this$0.mInForeground) {
                    ErrorConsoleView errorConsole = this.this$0.getErrorConsole(true);
                    errorConsole.addErrorMessage(consoleMessage);
                    if (this.this$0.mWebViewController.shouldShowErrorConsole() && errorConsole.getShowState() != 1) {
                        errorConsole.showConsole(0);
                    }
                }
                if (!this.this$0.isPrivateBrowsingEnabled() && Tab.DEBUG) {
                    String str = "Console: " + consoleMessage.message() + " " + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber();
                    switch (AnonymousClass9.$SwitchMap$android$webkit$ConsoleMessage$MessageLevel[consoleMessage.messageLevel().ordinal()]) {
                        case 1:
                            Log.v("browser", str);
                            break;
                        case 2:
                            Log.i("browser", str);
                            break;
                        case 3:
                            Log.w("browser", str);
                            break;
                        case 4:
                            Log.e("browser", str);
                            break;
                        case 5:
                            Log.d("browser", str);
                            break;
                    }
                }
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView webView2, boolean z, boolean z2, Message message) {
                if (!this.this$0.mInForeground) {
                    return false;
                }
                if (z && this.this$0.mSubView != null) {
                    new AlertDialog.Builder(this.this$0.mContext).setTitle(2131493216).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(2131493217).setPositiveButton(2131492964, (DialogInterface.OnClickListener) null).show();
                    return false;
                }
                if (!this.this$0.mWebViewController.getTabControl().canCreateNewTab()) {
                    new AlertDialog.Builder(this.this$0.mContext).setTitle(2131493214).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(2131493215).setPositiveButton(2131492964, (DialogInterface.OnClickListener) null).show();
                    return false;
                }
                if (z2) {
                    createWindow(z, message);
                    return true;
                }
                if (this.this$0.mSubWindowShown) {
                    new AlertDialog.Builder(this.this$0.mContext).setTitle(2131493216).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(2131493217).setPositiveButton(2131492964, (DialogInterface.OnClickListener) null).show();
                    return false;
                }
                this.this$0.mWebViewController.onShowPopupWindowAttempt(this.this$0, z, message);
                return true;
            }

            @Override
            public void onExceededDatabaseQuota(String str, String str2, long j, long j2, long j3, WebStorage.QuotaUpdater quotaUpdater) {
                this.this$0.mSettings.getWebStorageSizeManager().onExceededDatabaseQuota(str, str2, j, j2, j3, quotaUpdater);
            }

            @Override
            public void onGeolocationPermissionsHidePrompt() {
                if (!this.this$0.mInForeground || this.this$0.mGeolocationPermissionsPrompt == null) {
                    return;
                }
                this.this$0.mGeolocationPermissionsPrompt.hide();
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String str, GeolocationPermissions.Callback callback) {
                if (this.this$0.mInForeground) {
                    this.this$0.getGeolocationPermissionsPrompt().show(str, callback);
                }
            }

            @Override
            public void onHideCustomView() {
                if (this.this$0.mInForeground) {
                    this.this$0.mWebViewController.hideCustomView();
                }
            }

            @Override
            public boolean onJsAlert(WebView webView2, String str, String str2, JsResult jsResult) {
                this.this$0.mWebViewController.getTabControl().setActiveTab(this.this$0);
                return false;
            }

            @Override
            public boolean onJsConfirm(WebView webView2, String str, String str2, JsResult jsResult) {
                this.this$0.mWebViewController.getTabControl().setActiveTab(this.this$0);
                return false;
            }

            @Override
            public boolean onJsPrompt(WebView webView2, String str, String str2, String str3, JsPromptResult jsPromptResult) {
                this.this$0.mWebViewController.getTabControl().setActiveTab(this.this$0);
                return false;
            }

            @Override
            public void onPermissionRequest(PermissionRequest permissionRequest) {
                if (this.this$0.mInForeground) {
                    this.this$0.getPermissionsPrompt().show(permissionRequest);
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest permissionRequest) {
                if (!this.this$0.mInForeground || this.this$0.mPermissionsPrompt == null) {
                    return;
                }
                this.this$0.mPermissionsPrompt.hide();
            }

            @Override
            public void onProgressChanged(WebView webView2, int i) {
                this.this$0.mPageLoadProgress = i;
                this.this$0.mPageError = 0;
                if (i == 100) {
                    this.this$0.mInPageLoad = false;
                    this.this$0.syncCurrentState(webView2, webView2.getUrl());
                }
                this.this$0.mWebViewController.onProgressChanged(this.this$0);
                if (this.this$0.mUpdateThumbnail && i == 100) {
                    this.this$0.mUpdateThumbnail = false;
                }
            }

            public void onReachedMaxAppCacheSize(long j, long j2, WebStorage.QuotaUpdater quotaUpdater) {
                this.this$0.mSettings.getWebStorageSizeManager().onReachedMaxAppCacheSize(j, j2, quotaUpdater);
            }

            @Override
            public void onReceivedIcon(WebView webView2, Bitmap bitmap) {
                this.this$0.mCurrentState.mFavicon = bitmap;
                this.this$0.mWebViewController.onFavicon(this.this$0, webView2, bitmap);
            }

            @Override
            public void onReceivedTitle(WebView webView2, String str) {
                this.this$0.mCurrentState.mTitle = str;
                this.this$0.mWebViewController.onReceivedTitle(this.this$0, str);
            }

            @Override
            public void onReceivedTouchIconUrl(WebView webView2, String str, boolean z) {
                ContentResolver contentResolver = this.this$0.mContext.getContentResolver();
                synchronized (this.this$0) {
                    if (z) {
                        if (this.this$0.mTouchIconLoader != null) {
                            this.this$0.mTouchIconLoader.cancel(false);
                            this.this$0.mTouchIconLoader = null;
                        }
                        if (this.this$0.mTouchIconLoader == null) {
                            this.this$0.mTouchIconLoader = new DownloadTouchIcon(this.this$0, this.this$0.mContext, contentResolver, webView2);
                            this.this$0.mTouchIconLoader.execute(str);
                        }
                    } else if (this.this$0.mTouchIconLoader == null) {
                    }
                }
            }

            @Override
            public void onRequestFocus(WebView webView2) {
                if (this.this$0.mInForeground) {
                    return;
                }
                this.this$0.mWebViewController.switchToTab(this.this$0);
            }

            @Override
            public void onShowCustomView(View view, int i, WebChromeClient.CustomViewCallback customViewCallback) {
                if (this.this$0.mInForeground) {
                    this.this$0.mWebViewController.showCustomView(this.this$0, view, i, customViewCallback);
                }
            }

            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback customViewCallback) {
                Activity activity = this.this$0.mWebViewController.getActivity();
                if (activity != null) {
                    onShowCustomView(view, activity.getRequestedOrientation(), customViewCallback);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView2, ValueCallback<Uri[]> valueCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (!this.this$0.mInForeground) {
                    return false;
                }
                this.this$0.mWebViewController.showFileChooser(valueCallback, fileChooserParams);
                return true;
            }
        };
        this.mIsBookmarkCallback = new DataController.OnQueryUrlIsBookmark(this) {
            final Tab this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onQueryUrlIsBookmark(String str, boolean z) {
                if (this.this$0.mCurrentState.mUrl.equals(str)) {
                    this.this$0.mCurrentState.mIsBookmarkedSite = z;
                    this.this$0.mWebViewController.bookmarkedStatusHasChanged(this.this$0);
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
        this.mCurrentState = new PageState(this.mContext, webView != null ? webView.isPrivateBrowsingEnabled() : false);
        this.mInPageLoad = false;
        this.mInForeground = false;
        this.mDownloadListener = new BrowserDownloadListener(this) {
            final Tab this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onDownloadStart(String str, String str2, String str3, String str4, String str5, long j) {
                String strRemapGenericMimeTypePublic = MimeTypeMap.getSingleton().remapGenericMimeTypePublic(str4, str, str3);
                this.this$0.mCurrentState.mIsDownload = true;
                this.this$0.mWebViewController.onDownloadStart(this.this$0, str, str2, str3, strRemapGenericMimeTypePublic, str5, j);
            }
        };
        this.mWebBackForwardListClient = new WebBackForwardListClient(this) {
            final Tab this$0;

            {
                this.this$0 = this;
            }
        };
        this.mCaptureWidth = this.mContext.getResources().getDimensionPixelSize(2131427376);
        this.mCaptureHeight = this.mContext.getResources().getDimensionPixelSize(2131427377);
        updateShouldCaptureThumbnails();
        restoreState(bundle);
        if (getId() == -1) {
            this.mId = TabControl.getNextId();
        }
        setWebView(webView);
        this.mHandler = new Handler(this) {
            final Tab this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void handleMessage(Message message) {
                if (message.what != 42) {
                    return;
                }
                this.this$0.capture();
            }
        };
    }

    public static Bitmap getDefaultFavicon(Context context) {
        Bitmap bitmap;
        synchronized (Tab.class) {
            try {
                if (sDefaultFavicon == null) {
                    sDefaultFavicon = BitmapFactory.decodeResource(context.getResources(), 2130837505);
                }
                bitmap = sDefaultFavicon;
            } catch (Throwable th) {
                throw th;
            }
        }
        return bitmap;
    }

    public void handleProceededAfterSslError(SslError sslError) {
        if (sslError.getUrl().equals(this.mCurrentState.mUrl)) {
            setSecurityState(SecurityState.SECURITY_STATE_BAD_CERTIFICATE);
            this.mCurrentState.mSslCertificateError = sslError;
        } else if (getSecurityState() == SecurityState.SECURITY_STATE_SECURE) {
            setSecurityState(SecurityState.SECURITY_STATE_MIXED);
        }
    }

    private void postCapture() {
        if (this.mHandler.hasMessages(42)) {
            return;
        }
        this.mHandler.sendEmptyMessageDelayed(42, 100L);
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

    public void queueError(int i, String str) {
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
            str = this.mContext.getString(2131492918);
        }
        ErrorDialog errorDialog = new ErrorDialog(this, (i == -14 || i == -20000) ? 2131493196 : 2131493195, str, i);
        this.mQueuedErrors.addLast(errorDialog);
        if (this.mQueuedErrors.size() == 1 && this.mInForeground) {
            showError(errorDialog);
        }
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
        if (this.mMainView == null || this.mSavedState == null || this.mSavedState.getBoolean("useragent") == this.mSettings.hasDesktopUseragent(this.mMainView)) {
            return;
        }
        this.mSettings.toggleDesktopUseragent(this.mMainView);
    }

    public void setSecurityState(SecurityState securityState) {
        this.mCurrentState.mSecurityState = securityState;
        this.mCurrentState.mSslCertificateError = null;
        this.mWebViewController.onUpdatedSecurityState(this);
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

    private void showError(ErrorDialog errorDialog) {
        if (!this.mInForeground || this.mIsErrorDialogShown) {
            return;
        }
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setTitle(errorDialog.mTitle).setMessage(errorDialog.mDescription).setPositiveButton(2131492964, (DialogInterface.OnClickListener) null).create();
        alertDialogCreate.setOnDismissListener(this.mDialogListener);
        alertDialogCreate.show();
        this.mIsErrorDialogShown = true;
    }

    public void syncCurrentState(WebView webView, String str) {
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

    public void PopupWindowShown(boolean z) {
        this.mSubWindowShown = z;
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

    void addDatabaseItemId(int i, long j) {
        if (this.mSavePageJob == null) {
            this.mSavePageJob = new HashMap<>();
        }
        this.mSavePageJob.put(Integer.valueOf(i), Long.valueOf(j));
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
        if (tabControl == null || (onThumbnailUpdatedListener = tabControl.getOnThumbnailUpdatedListener()) == null) {
            return;
        }
        onThumbnailUpdatedListener.onThumbnailUpdated(this);
    }

    public void clearTabData() {
        this.mWillBeClosed = true;
        this.mAppId = "";
        this.mCurrentState.mUrl = "";
        this.mCurrentState.mOriginalUrl = "";
        this.mCurrentState.mTitle = "";
    }

    boolean closeOnBack() {
        return this.mCloseOnBack;
    }

    public byte[] compressBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    boolean containsDatabaseItemId(int i) {
        if (this.mSavePageJob != null) {
            return this.mSavePageJob.containsKey(Integer.valueOf(i));
        }
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

    boolean createSubWindow() {
        if (DEBUG) {
            Log.d("browser", "Tab.createSubWindow()--->mSubView = " + this.mSubView);
        }
        if (this.mSubView != null) {
            return false;
        }
        this.mWebViewController.createSubWindow(this);
        this.mSubView.setWebViewClient(new SubWindowClient(this.mWebViewClient, this.mWebViewController));
        this.mSubView.setWebChromeClient(new SubWindowChromeClient(this, this.mWebChromeClient));
        this.mSubView.setDownloadListener(new BrowserDownloadListener(this) {
            final Tab this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onDownloadStart(String str, String str2, String str3, String str4, String str5, long j) {
                this.this$0.mWebViewController.onDownloadStart(this.this$0, str, str2, str3, MimeTypeMap.getSingleton().remapGenericMimeTypePublic(str4, str, str3), str5, j);
                if (this.this$0.mSubView.copyBackForwardList().getSize() == 0) {
                    this.this$0.mWebViewController.dismissSubWindow(this.this$0);
                }
            }
        });
        this.mSubView.setOnCreateContextMenuListener(this.mWebViewController.getActivity());
        return true;
    }

    protected void deleteThumbnail() {
        DataController.getInstance(this.mContext).deleteThumbnail(this);
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
        if (this.mSavePageJob == null || this.mSavePageJob.size() == 0) {
            return;
        }
        Toast.makeText(this.mContext, 2131492919, 1).show();
        new CancelSavePageTask().execute(new Void[0]);
    }

    public void disableUrlOverridingForLoad() {
        this.mDisableOverrideUrlLoading = true;
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

    String getAppId() {
        return this.mAppId;
    }

    DeviceAccountLogin getDeviceAccountLogin() {
        if (DEBUG) {
            Log.d("browser", "Tab.getDeviceAccountLogin()--->");
        }
        return this.mDeviceAccountLogin;
    }

    ErrorConsoleView getErrorConsole(boolean z) {
        if (z && this.mErrorConsole == null) {
            this.mErrorConsole = new ErrorConsoleView(this.mContext);
            this.mErrorConsole.setWebView(this.mMainView);
        }
        return this.mErrorConsole;
    }

    Bitmap getFavicon() {
        return this.mCurrentState.mFavicon != null ? this.mCurrentState.mFavicon : getDefaultFavicon(this.mContext);
    }

    GeolocationPermissionsPrompt getGeolocationPermissionsPrompt() {
        if (this.mGeolocationPermissionsPrompt == null) {
            this.mGeolocationPermissionsPrompt = (GeolocationPermissionsPrompt) ((ViewStub) this.mContainer.findViewById(2131558523)).inflate();
        }
        return this.mGeolocationPermissionsPrompt;
    }

    public long getId() {
        return this.mId;
    }

    int getLoadProgress() {
        if (this.mInPageLoad) {
            return this.mPageLoadProgress;
        }
        return 100;
    }

    String getOriginalUrl() {
        return this.mCurrentState.mOriginalUrl == null ? getUrl() : UrlUtils.filteredUrl(this.mCurrentState.mOriginalUrl);
    }

    public Tab getParent() {
        return this.mParent;
    }

    PermissionsPrompt getPermissionsPrompt() {
        if (this.mPermissionsPrompt == null) {
            this.mPermissionsPrompt = (PermissionsPrompt) ((ViewStub) this.mContainer.findViewById(2131558522)).inflate();
        }
        return this.mPermissionsPrompt;
    }

    public Bitmap getScreenshot() {
        Bitmap bitmap;
        synchronized (this) {
            bitmap = this.mCapture;
        }
        return bitmap;
    }

    SecurityState getSecurityState() {
        return this.mCurrentState.mSecurityState;
    }

    SslError getSslCertificateError() {
        return this.mCurrentState.mSslCertificateError;
    }

    View getSubViewContainer() {
        return this.mSubViewContainer;
    }

    WebView getSubWebView() {
        return this.mSubView;
    }

    String getTitle() {
        return (this.mCurrentState.mTitle == null && this.mInPageLoad) ? this.mContext.getString(2131492965) : this.mCurrentState.mTitle;
    }

    WebView getTopWindow() {
        return this.mSubView != null ? this.mSubView : this.mMainView;
    }

    String getUrl() {
        return UrlUtils.filteredUrl(this.mCurrentState.mUrl);
    }

    View getViewContainer() {
        return this.mContainer;
    }

    WebView getWebView() {
        return this.mMainView;
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

    boolean inForeground() {
        return this.mInForeground;
    }

    boolean inPageLoad() {
        return this.mInPageLoad;
    }

    public boolean isBookmarkedSite() {
        return this.mCurrentState.mIsBookmarkedSite;
    }

    boolean isPrivateBrowsingEnabled() {
        return this.mCurrentState.mIncognito;
    }

    public boolean isSnapshot() {
        return false;
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

    @Override
    public void onNewPicture(WebView webView, Picture picture) {
        if ((this.mWebViewController instanceof Controller) && ((Controller) this.mWebViewController).getUi().isWebShowing()) {
            postCapture();
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

    protected void persistThumbnail() {
        DataController.getInstance(this.mContext).saveThumbnail(this);
    }

    void putInBackground() {
        if (this.mInForeground) {
            capture();
            this.mInForeground = false;
            pause();
            this.mMainView.setOnCreateContextMenuListener(null);
            if (this.mSubView != null) {
                this.mSubView.setOnCreateContextMenuListener(null);
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

    public void refreshIdAfterPreload() {
        this.mId = TabControl.getNextId();
    }

    void removeDatabaseItemId(int i) {
        if (this.mSavePageJob != null) {
            this.mSavePageJob.remove(Integer.valueOf(i));
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

    void resume() {
        if (this.mMainView != null) {
            setupHwAcceleration(this.mMainView);
            this.mMainView.onResume();
            if (this.mSubView != null) {
                this.mSubView.onResume();
            }
        }
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

    public void setAcceptThirdPartyCookies(boolean z) {
        CookieManager cookieManager = CookieManager.getInstance();
        if (this.mMainView != null) {
            cookieManager.setAcceptThirdPartyCookies(this.mMainView, z);
        }
        if (this.mSubView != null) {
            cookieManager.setAcceptThirdPartyCookies(this.mSubView, z);
        }
    }

    void setAppId(String str) {
        this.mAppId = str;
    }

    void setCloseOnBack(boolean z) {
        this.mCloseOnBack = z;
    }

    public void setController(WebViewController webViewController) {
        this.mWebViewController = webViewController;
        updateShouldCaptureThumbnails();
    }

    void setDeviceAccountLogin(DeviceAccountLogin deviceAccountLogin) {
        this.mDeviceAccountLogin = deviceAccountLogin;
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

    void setSubViewContainer(View view) {
        this.mSubViewContainer = view;
    }

    void setSubWebView(WebView webView) {
        this.mSubView = webView;
    }

    void setViewContainer(View view) {
        this.mContainer = view;
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
            if (!z || this.mSavedState == null) {
                return;
            }
            restoreUserAgent();
            WebBackForwardList webBackForwardListRestoreState = this.mMainView.restoreState(this.mSavedState);
            if (webBackForwardListRestoreState == null || webBackForwardListRestoreState.getSize() == 0) {
                Log.w("Tab", "Failed to restore WebView state!");
                loadUrl(this.mCurrentState.mOriginalUrl, null);
            }
            this.mSavedState = null;
        }
    }

    public boolean shouldUpdateThumbnail() {
        return this.mUpdateThumbnail;
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

    public void updateBookmarkedStatus() {
        if (this.mCurrentState.mUrl == null || !this.mCurrentState.mUrl.equals(SiteNavigation.SITE_NAVIGATION_URI.toString())) {
            this.mDataController.queryBookmarkStatus(getUrl(), this.mIsBookmarkCallback);
        } else {
            this.mDataController.queryBookmarkStatus(SiteNavigation.SITE_NAVIGATION_URI.toString(), this.mIsBookmarkCallback);
        }
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

    public void updateShouldCaptureThumbnails() {
        if (!this.mWebViewController.shouldCaptureThumbnails()) {
            synchronized (this) {
                this.mCapture = null;
                deleteThumbnail();
            }
        } else {
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
    }
}
