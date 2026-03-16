package com.android.browser;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.BrowserContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import com.android.browser.IntentHandler;
import com.android.browser.UI;
import com.android.browser.provider.BrowserProvider2;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Controller implements ActivityController, UiController, WebViewController {
    static final boolean $assertionsDisabled;
    private static final int[] WINDOW_SHORTCUT_ID_ARRAY;
    private static Bitmap sThumbnailBitmap;
    private ActionMode mActionMode;
    private Activity mActivity;
    private boolean mBlockEvents;
    private ContentObserver mBookmarksObserver;
    private Menu mCachedMenu;
    private boolean mConfigChanged;
    private CrashRecoveryHandler mCrashRecoveryHandler;
    private boolean mExtendedMenuOpen;
    private WebViewFactory mFactory;
    private Handler mHandler;
    private IntentHandler mIntentHandler;
    private boolean mLoadStopped;
    private boolean mMenuIsDown;
    private NetworkStateHandler mNetworkHandler;
    private boolean mOptionsMenuOpen;
    private PageDialogsHandler mPageDialogsHandler;
    private boolean mShouldShowErrorConsole;
    private SystemAllowGeolocationOrigins mSystemAllowGeolocationOrigins;
    private UI mUi;
    private UploadHandler mUploadHandler;
    private UrlHandler mUrlHandler;
    private String mVoiceResult;
    private PowerManager.WakeLock mWakeLock;
    private int mCurrentMenuState = 0;
    private int mMenuState = R.id.MAIN_MENU;
    private int mOldMenuState = -1;
    private boolean mActivityPaused = true;
    private BrowserSettings mSettings = BrowserSettings.getInstance();
    private TabControl mTabControl = new TabControl(this);

    static {
        $assertionsDisabled = !Controller.class.desiredAssertionStatus();
        WINDOW_SHORTCUT_ID_ARRAY = new int[]{R.id.window_one_menu_id, R.id.window_two_menu_id, R.id.window_three_menu_id, R.id.window_four_menu_id, R.id.window_five_menu_id, R.id.window_six_menu_id, R.id.window_seven_menu_id, R.id.window_eight_menu_id};
    }

    public Controller(Activity browser) {
        this.mActivity = browser;
        this.mSettings.setController(this);
        this.mCrashRecoveryHandler = CrashRecoveryHandler.initialize(this);
        this.mCrashRecoveryHandler.preloadCrashState();
        this.mFactory = new BrowserWebViewFactory(browser);
        this.mUrlHandler = new UrlHandler(this);
        this.mIntentHandler = new IntentHandler(this.mActivity, this);
        this.mPageDialogsHandler = new PageDialogsHandler(this.mActivity, this);
        startHandler();
        this.mBookmarksObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                int size = Controller.this.mTabControl.getTabCount();
                for (int i = 0; i < size; i++) {
                    Controller.this.mTabControl.getTab(i).updateBookmarkedStatus();
                }
            }
        };
        browser.getContentResolver().registerContentObserver(BrowserContract.Bookmarks.CONTENT_URI, true, this.mBookmarksObserver);
        this.mNetworkHandler = new NetworkStateHandler(this.mActivity, this);
        this.mSystemAllowGeolocationOrigins = new SystemAllowGeolocationOrigins(this.mActivity.getApplicationContext());
        this.mSystemAllowGeolocationOrigins.start();
        openIconDatabase();
    }

    @Override
    public void start(Intent intent) {
        this.mCrashRecoveryHandler.startRecovery(intent);
    }

    void doStart(final Bundle icicle, final Intent intent) {
        Calendar lastActiveDate = icicle != null ? (Calendar) icicle.getSerializable("lastActiveDate") : null;
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(5, -1);
        final boolean restoreIncognitoTabs = (lastActiveDate == null || lastActiveDate.before(yesterday) || lastActiveDate.after(today)) ? false : true;
        final long currentTabId = this.mTabControl.canRestoreState(icicle, restoreIncognitoTabs);
        if (currentTabId == -1) {
            CookieManager.getInstance().removeSessionCookie();
        }
        GoogleAccountLogin.startLoginIfNeeded(this.mActivity, new Runnable() {
            @Override
            public void run() {
                Controller.this.onPreloginFinished(icicle, intent, currentTabId, restoreIncognitoTabs);
            }
        });
    }

    private void onPreloginFinished(Bundle icicle, Intent intent, long currentTabId, boolean restoreIncognitoTabs) {
        Tab t;
        int scale;
        if (currentTabId == -1) {
            BackgroundHandler.execute(new PruneThumbnails(this.mActivity, null));
            if (intent == null) {
                openTabToHomePage();
            } else {
                Bundle extra = intent.getExtras();
                IntentHandler.UrlData urlData = IntentHandler.getUrlDataFromIntent(intent);
                if (urlData.isEmpty()) {
                    t = openTabToHomePage();
                } else {
                    t = openTab(urlData);
                }
                if (t != null) {
                    t.setAppId(intent.getStringExtra("com.android.browser.application_id"));
                }
                WebView webView = t.getWebView();
                if (extra != null && (scale = extra.getInt("browser.initialZoomLevel", 0)) > 0 && scale <= 1000) {
                    webView.setInitialScale(scale);
                }
            }
            this.mUi.updateTabs(this.mTabControl.getTabs());
        } else {
            this.mTabControl.restoreState(icicle, currentTabId, restoreIncognitoTabs, this.mUi.needsRestoreAllTabs());
            List<Tab> tabs = this.mTabControl.getTabs();
            ArrayList<Long> restoredTabs = new ArrayList<>(tabs.size());
            for (Tab t2 : tabs) {
                restoredTabs.add(Long.valueOf(t2.getId()));
            }
            BackgroundHandler.execute(new PruneThumbnails(this.mActivity, restoredTabs));
            if (tabs.size() == 0) {
                openTabToHomePage();
            }
            this.mUi.updateTabs(tabs);
            setActiveTab(this.mTabControl.getCurrentTab());
            if (intent != null) {
                this.mIntentHandler.onNewIntent(intent);
            }
        }
        getSettings().getJsEngineFlags();
        if (intent != null && "show_bookmarks".equals(intent.getAction())) {
            bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
        }
    }

    private static class PruneThumbnails implements Runnable {
        private Context mContext;
        private List<Long> mIds;

        PruneThumbnails(Context context, List<Long> preserveIds) {
            this.mContext = context.getApplicationContext();
            this.mIds = preserveIds;
        }

        @Override
        public void run() {
            ContentResolver cr = this.mContext.getContentResolver();
            if (this.mIds == null || this.mIds.size() == 0) {
                cr.delete(BrowserProvider2.Thumbnails.CONTENT_URI, null, null);
                return;
            }
            int length = this.mIds.size();
            StringBuilder where = new StringBuilder();
            where.append("_id");
            where.append(" not in (");
            for (int i = 0; i < length; i++) {
                where.append(this.mIds.get(i));
                if (i < length - 1) {
                    where.append(",");
                }
            }
            where.append(")");
            cr.delete(BrowserProvider2.Thumbnails.CONTENT_URI, where.toString(), null);
        }
    }

    public WebViewFactory getWebViewFactory() {
        return this.mFactory;
    }

    @Override
    public void onSetWebView(Tab tab, WebView view) {
        this.mUi.onSetWebView(tab, view);
    }

    @Override
    public void createSubWindow(Tab tab) {
        endActionMode();
        WebView mainView = tab.getWebView();
        WebView subView = this.mFactory.createWebView(mainView == null ? false : mainView.isPrivateBrowsingEnabled());
        this.mUi.createSubWindow(tab, subView);
    }

    @Override
    public Context getContext() {
        return this.mActivity;
    }

    @Override
    public Activity getActivity() {
        return this.mActivity;
    }

    void setUi(UI ui) {
        this.mUi = ui;
    }

    @Override
    public BrowserSettings getSettings() {
        return this.mSettings;
    }

    public UI getUi() {
        return this.mUi;
    }

    int getMaxTabs() {
        return this.mActivity.getResources().getInteger(R.integer.max_tabs);
    }

    @Override
    public TabControl getTabControl() {
        return this.mTabControl;
    }

    @Override
    public List<Tab> getTabs() {
        return this.mTabControl.getTabs();
    }

    private void openIconDatabase() {
        final WebIconDatabase instance = WebIconDatabase.getInstance();
        BackgroundHandler.execute(new Runnable() {
            @Override
            public void run() {
                instance.open(Controller.this.mActivity.getDir("icons", 0).getPath());
            }
        });
    }

    private void startHandler() {
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 102:
                        String url = (String) msg.getData().get("url");
                        String src = (String) msg.getData().get("src");
                        if (url == "") {
                            url = src;
                        }
                        if (!TextUtils.isEmpty(url)) {
                            HashMap focusNodeMap = (HashMap) msg.obj;
                            WebView view = (WebView) focusNodeMap.get("webview");
                            if (Controller.this.getCurrentTopWebView() == view) {
                                switch (msg.arg1) {
                                    case R.id.open_context_menu_id:
                                        Controller.this.loadUrlFromContext(url);
                                        break;
                                    case R.id.open_newtab_context_menu_id:
                                        Tab parent = Controller.this.mTabControl.getCurrentTab();
                                        Controller.this.openTab(url, parent, !Controller.this.mSettings.openInBackground(), true);
                                        break;
                                    case R.id.save_link_context_menu_id:
                                    case R.id.download_context_menu_id:
                                        DownloadHandler.onDownloadStartNoStream(Controller.this.mActivity, url, view.getSettings().getUserAgentString(), null, null, null, view.isPrivateBrowsingEnabled());
                                        break;
                                    case R.id.copy_link_context_menu_id:
                                        Controller.this.copy(url);
                                        break;
                                    case R.id.view_image_context_menu_id:
                                        Controller.this.loadUrlFromContext(src);
                                        break;
                                }
                            }
                        }
                        break;
                    case 107:
                        if (Controller.this.mWakeLock != null && Controller.this.mWakeLock.isHeld()) {
                            Controller.this.mWakeLock.release();
                            Controller.this.mTabControl.stopAllLoading();
                            break;
                        }
                        break;
                    case 108:
                        Tab tab = (Tab) msg.obj;
                        if (tab != null) {
                            Controller.this.updateScreenshot(tab);
                        }
                        break;
                    case 201:
                        Controller.this.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
                        break;
                    case 1001:
                        Controller.this.loadUrlFromContext((String) msg.obj);
                        break;
                    case 1002:
                        Controller.this.stopLoading();
                        break;
                }
            }
        };
    }

    @Override
    public Tab getCurrentTab() {
        return this.mTabControl.getCurrentTab();
    }

    @Override
    public void shareCurrentPage() {
        shareCurrentPage(this.mTabControl.getCurrentTab());
    }

    private void shareCurrentPage(Tab tab) {
        if (tab != null) {
            sharePage(this.mActivity, tab.getTitle(), tab.getUrl(), tab.getFavicon(), createScreenshot(tab.getWebView(), getDesiredThumbnailWidth(this.mActivity), getDesiredThumbnailHeight(this.mActivity)));
        }
    }

    static final void sharePage(Context c, String title, String url, Bitmap favicon, Bitmap screenshot) {
        Intent send = new Intent("android.intent.action.SEND");
        send.setType("text/plain");
        send.putExtra("android.intent.extra.TEXT", url);
        send.putExtra("android.intent.extra.SUBJECT", title);
        send.putExtra("share_favicon", favicon);
        send.putExtra("share_screenshot", screenshot);
        try {
            c.startActivity(Intent.createChooser(send, c.getString(R.string.choosertitle_sharevia)));
        } catch (ActivityNotFoundException e) {
        }
    }

    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) this.mActivity.getSystemService("clipboard");
        cm.setText(text);
    }

    @Override
    public void onConfgurationChanged(Configuration config) {
        this.mConfigChanged = true;
        this.mActivity.invalidateOptionsMenu();
        if (this.mPageDialogsHandler != null) {
            this.mPageDialogsHandler.onConfigurationChanged(config);
        }
        this.mUi.onConfigurationChanged(config);
    }

    @Override
    public void handleNewIntent(Intent intent) {
        if (!this.mUi.isWebShowing()) {
            this.mUi.showWeb(false);
        }
        this.mIntentHandler.onNewIntent(intent);
    }

    @Override
    public void onPause() {
        if (this.mUi.isCustomViewShowing()) {
            hideCustomView();
        }
        if (this.mActivityPaused) {
            Log.e("Controller", "BrowserActivity is already paused.");
            return;
        }
        this.mActivityPaused = true;
        Tab tab = this.mTabControl.getCurrentTab();
        if (tab != null) {
            tab.pause();
            if (!pauseWebViewTimers(tab)) {
                if (this.mWakeLock == null) {
                    PowerManager pm = (PowerManager) this.mActivity.getSystemService("power");
                    this.mWakeLock = pm.newWakeLock(1, "Browser");
                }
                this.mWakeLock.acquire();
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(107), 300000L);
            }
        }
        this.mUi.onPause();
        this.mNetworkHandler.onPause();
        WebView.disablePlatformNotifications();
        NfcHandler.unregister(this.mActivity);
        if (sThumbnailBitmap != null) {
            sThumbnailBitmap.recycle();
            sThumbnailBitmap = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Bundle saveState = createSaveState();
        this.mCrashRecoveryHandler.writeState(saveState);
        this.mSettings.setLastRunPaused(true);
    }

    Bundle createSaveState() {
        Bundle saveState = new Bundle();
        this.mTabControl.saveState(saveState);
        if (!saveState.isEmpty()) {
            saveState.putSerializable("lastActiveDate", Calendar.getInstance());
        }
        return saveState;
    }

    @Override
    public void onResume() {
        if (!this.mActivityPaused) {
            Log.e("Controller", "BrowserActivity is already resumed.");
            return;
        }
        this.mSettings.setLastRunPaused(false);
        this.mActivityPaused = false;
        Tab current = this.mTabControl.getCurrentTab();
        if (current != null) {
            current.resume();
            resumeWebViewTimers(current);
        }
        releaseWakeLock();
        this.mUi.onResume();
        this.mNetworkHandler.onResume();
        WebView.enablePlatformNotifications();
        NfcHandler.register(this.mActivity, this);
        if (this.mVoiceResult != null) {
            this.mUi.onVoiceResult(this.mVoiceResult);
            this.mVoiceResult = null;
        }
    }

    private void releaseWakeLock() {
        if (this.mWakeLock != null && this.mWakeLock.isHeld()) {
            this.mHandler.removeMessages(107);
            this.mWakeLock.release();
        }
    }

    private void resumeWebViewTimers(Tab tab) {
        boolean inLoad = tab.inPageLoad();
        if ((!this.mActivityPaused && !inLoad) || (this.mActivityPaused && inLoad)) {
            CookieSyncManager.getInstance().startSync();
            WebView w = tab.getWebView();
            WebViewTimersControl.getInstance().onBrowserActivityResume(w);
        }
    }

    private boolean pauseWebViewTimers(Tab tab) {
        if (tab == null) {
            return true;
        }
        if (!tab.inPageLoad()) {
            CookieSyncManager.getInstance().stopSync();
            WebViewTimersControl.getInstance().onBrowserActivityPause(getCurrentWebView());
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        if (this.mUploadHandler != null && !this.mUploadHandler.handled()) {
            this.mUploadHandler.onResult(0, null);
            this.mUploadHandler = null;
        }
        if (this.mTabControl != null) {
            this.mUi.onDestroy();
            Tab t = this.mTabControl.getCurrentTab();
            if (t != null) {
                dismissSubWindow(t);
                removeTab(t);
            }
            this.mActivity.getContentResolver().unregisterContentObserver(this.mBookmarksObserver);
            this.mTabControl.destroy();
            WebIconDatabase.getInstance().close();
            this.mSystemAllowGeolocationOrigins.stop();
            this.mSystemAllowGeolocationOrigins = null;
        }
    }

    protected boolean isActivityPaused() {
        return this.mActivityPaused;
    }

    @Override
    public void onLowMemory() {
        this.mTabControl.freeMemory();
    }

    @Override
    public boolean shouldShowErrorConsole() {
        return this.mShouldShowErrorConsole;
    }

    protected void setShouldShowErrorConsole(boolean show) {
        if (show != this.mShouldShowErrorConsole) {
            this.mShouldShowErrorConsole = show;
            Tab t = this.mTabControl.getCurrentTab();
            if (t != null) {
                this.mUi.setShouldShowErrorConsole(t, show);
            }
        }
    }

    @Override
    public void stopLoading() {
        this.mLoadStopped = true;
        Tab tab = this.mTabControl.getCurrentTab();
        WebView w = getCurrentTopWebView();
        if (w != null) {
            w.stopLoading();
            this.mUi.onPageStopped(tab);
        }
    }

    boolean didUserStopLoading() {
        return this.mLoadStopped;
    }

    @Override
    public void onPageStarted(Tab tab, WebView view, Bitmap favicon) {
        this.mHandler.removeMessages(108, tab);
        CookieSyncManager.getInstance().resetSync();
        if (!this.mNetworkHandler.isNetworkUp()) {
            view.setNetworkAvailable(false);
        }
        if (this.mActivityPaused) {
            resumeWebViewTimers(tab);
        }
        this.mLoadStopped = false;
        endActionMode();
        this.mUi.onTabDataChanged(tab);
        String url = tab.getUrl();
        maybeUpdateFavicon(tab, null, url, favicon);
        Performance.tracePageStart(url);
    }

    @Override
    public void onPageFinished(Tab tab) {
        this.mCrashRecoveryHandler.backupState();
        this.mUi.onTabDataChanged(tab);
        Performance.tracePageFinished();
    }

    @Override
    public void onProgressChanged(Tab tab) {
        int newProgress = tab.getLoadProgress();
        if (newProgress == 100) {
            CookieSyncManager.getInstance().sync();
            if (tab.inPageLoad()) {
                updateInLoadMenuItems(this.mCachedMenu, tab);
            } else if (this.mActivityPaused && pauseWebViewTimers(tab)) {
                releaseWakeLock();
            }
            if (!tab.isPrivateBrowsingEnabled() && !TextUtils.isEmpty(tab.getUrl()) && !tab.isSnapshot() && tab.shouldUpdateThumbnail() && (((tab.inForeground() && !didUserStopLoading()) || !tab.inForeground()) && !this.mHandler.hasMessages(108, tab))) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(108, 0, 0, tab), 500L);
            }
        } else if (!tab.inPageLoad()) {
            updateInLoadMenuItems(this.mCachedMenu, tab);
        }
        this.mUi.onProgressChanged(tab);
    }

    @Override
    public void onUpdatedSecurityState(Tab tab) {
        this.mUi.onTabDataChanged(tab);
    }

    @Override
    public void onReceivedTitle(Tab tab, String title) {
        this.mUi.onTabDataChanged(tab);
        String pageUrl = tab.getOriginalUrl();
        if (!TextUtils.isEmpty(pageUrl) && pageUrl.length() < 50000 && !tab.isPrivateBrowsingEnabled()) {
            DataController.getInstance(this.mActivity).updateHistoryTitle(pageUrl, title);
        }
    }

    @Override
    public void onFavicon(Tab tab, WebView view, Bitmap icon) {
        this.mUi.onTabDataChanged(tab);
        maybeUpdateFavicon(tab, view.getOriginalUrl(), view.getUrl(), icon);
    }

    @Override
    public boolean shouldOverrideUrlLoading(Tab tab, WebView view, String url) {
        return this.mUrlHandler.shouldOverrideUrlLoading(tab, view, url);
    }

    @Override
    public boolean shouldOverrideKeyEvent(KeyEvent event) {
        if (this.mMenuIsDown) {
            return this.mActivity.getWindow().isShortcutKey(event.getKeyCode(), event);
        }
        return false;
    }

    @Override
    public boolean onUnhandledKeyEvent(KeyEvent event) {
        if (!isActivityPaused()) {
            if (event.getAction() == 0) {
                return this.mActivity.onKeyDown(event.getKeyCode(), event);
            }
            return this.mActivity.onKeyUp(event.getKeyCode(), event);
        }
        return false;
    }

    @Override
    public void doUpdateVisitedHistory(Tab tab, boolean isReload) {
        if (!tab.isPrivateBrowsingEnabled()) {
            String url = tab.getOriginalUrl();
            if (!TextUtils.isEmpty(url) && !url.regionMatches(true, 0, "about:", 0, 6)) {
                DataController.getInstance(this.mActivity).updateVisitedHistory(url);
                this.mCrashRecoveryHandler.backupState();
            }
        }
    }

    @Override
    public void getVisitedHistory(final ValueCallback<String[]> callback) {
        AsyncTask<Void, Void, String[]> task = new AsyncTask<Void, Void, String[]>() {
            @Override
            public String[] doInBackground(Void... unused) {
                return android.provider.Browser.getVisitedHistory(Controller.this.mActivity.getContentResolver());
            }

            @Override
            public void onPostExecute(String[] result) {
                callback.onReceiveValue(result);
            }
        };
        task.execute(new Void[0]);
    }

    @Override
    public void onReceivedHttpAuthRequest(Tab tab, WebView view, HttpAuthHandler handler, String host, String realm) {
        String[] credentials;
        String username = null;
        String password = null;
        boolean reuseHttpAuthUsernamePassword = handler.useHttpAuthUsernamePassword();
        if (reuseHttpAuthUsernamePassword && view != null && (credentials = view.getHttpAuthUsernamePassword(host, realm)) != null && credentials.length == 2) {
            username = credentials[0];
            password = credentials[1];
        }
        if (username != null && password != null) {
            handler.proceed(username, password);
        } else if (tab.inForeground() && !handler.suppressDialog()) {
            this.mPageDialogsHandler.showHttpAuthentication(tab, handler, host, realm);
        } else {
            handler.cancel();
        }
    }

    @Override
    public void onDownloadStart(Tab tab, String url, String userAgent, String contentDisposition, String mimetype, String referer, long contentLength) {
        WebView w = tab.getWebView();
        if (w != null) {
            DownloadHandler.onDownloadStart(this.mActivity, url, userAgent, contentDisposition, mimetype, referer, w.isPrivateBrowsingEnabled());
            if (w.copyBackForwardList().getSize() == 0) {
                if (tab == this.mTabControl.getCurrentTab()) {
                    goBackOnePageOrQuit();
                } else {
                    closeTab(tab);
                }
            }
        }
    }

    @Override
    public Bitmap getDefaultVideoPoster() {
        return this.mUi.getDefaultVideoPoster();
    }

    @Override
    public View getVideoLoadingProgressView() {
        return this.mUi.getVideoLoadingProgressView();
    }

    @Override
    public void showSslCertificateOnError(WebView view, SslErrorHandler handler, SslError error) {
        this.mPageDialogsHandler.showSSLCertificateOnError(view, handler, error);
    }

    @Override
    public void showAutoLogin(Tab tab) {
        if (!$assertionsDisabled && !tab.inForeground()) {
            throw new AssertionError();
        }
        this.mUi.showAutoLogin(tab);
    }

    @Override
    public void hideAutoLogin(Tab tab) {
        if (!$assertionsDisabled && !tab.inForeground()) {
            throw new AssertionError();
        }
        this.mUi.hideAutoLogin(tab);
    }

    private void maybeUpdateFavicon(Tab tab, String originalUrl, String url, Bitmap favicon) {
        if (favicon != null && !tab.isPrivateBrowsingEnabled()) {
            Bookmarks.updateFavicon(this.mActivity.getContentResolver(), originalUrl, url, favicon);
        }
    }

    @Override
    public void bookmarkedStatusHasChanged(Tab tab) {
        this.mUi.bookmarkedStatusHasChanged(tab);
    }

    protected void pageUp() {
        getCurrentTopWebView().pageUp(false);
    }

    protected void pageDown() {
        getCurrentTopWebView().pageDown(false);
    }

    public void editUrl() {
        if (this.mOptionsMenuOpen) {
            this.mActivity.closeOptionsMenu();
        }
        this.mUi.editUrl(false, true);
    }

    @Override
    public void showCustomView(Tab tab, View view, int requestedOrientation, WebChromeClient.CustomViewCallback callback) {
        if (tab.inForeground()) {
            if (this.mUi.isCustomViewShowing()) {
                callback.onCustomViewHidden();
                return;
            }
            this.mUi.showCustomView(view, requestedOrientation, callback);
            this.mOldMenuState = this.mMenuState;
            this.mMenuState = -1;
            this.mActivity.invalidateOptionsMenu();
        }
    }

    @Override
    public void hideCustomView() {
        if (this.mUi.isCustomViewShowing()) {
            this.mUi.onHideCustomView();
            this.mMenuState = this.mOldMenuState;
            this.mOldMenuState = -1;
            this.mActivity.invalidateOptionsMenu();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (getCurrentTopWebView() != null) {
            switch (requestCode) {
                case 1:
                    if (intent != null && resultCode == -1) {
                        this.mUi.showWeb(false);
                        if ("android.intent.action.VIEW".equals(intent.getAction())) {
                            Tab t = getCurrentTab();
                            Uri uri = intent.getData();
                            loadUrl(t, uri.toString());
                        } else if (intent.hasExtra("open_all")) {
                            String[] urls = intent.getStringArrayExtra("open_all");
                            Tab parent = getCurrentTab();
                            for (String url : urls) {
                                parent = openTab(url, parent, !this.mSettings.openInBackground(), true);
                            }
                        } else if (intent.hasExtra("snapshot_id")) {
                            long id = intent.getLongExtra("snapshot_id", -1L);
                            if (id >= 0) {
                                Toast.makeText(this.mActivity, "Snapshot Tab no longer supported", 1).show();
                            }
                        }
                    }
                    break;
                case 3:
                    if (resultCode == -1 && intent != null) {
                        String action = intent.getStringExtra("android.intent.extra.TEXT");
                        if ("privacy_clear_history".equals(action)) {
                            this.mTabControl.removeParentChildRelationShips();
                        }
                    }
                    break;
                case 4:
                    if (this.mUploadHandler != null) {
                        this.mUploadHandler.onResult(resultCode, intent);
                    }
                    break;
                case 6:
                    if (resultCode == -1 && intent != null) {
                        ArrayList<String> results = intent.getStringArrayListExtra("android.speech.extra.RESULTS");
                        if (results.size() >= 1) {
                            this.mVoiceResult = results.get(0);
                        }
                    }
                    break;
            }
            getCurrentTopWebView().requestFocus();
        }
    }

    @Override
    public void bookmarksOrHistoryPicker(UI.ComboViews startView) {
        if (this.mTabControl.getCurrentWebView() != null) {
            if (isInCustomActionMode()) {
                endActionMode();
            }
            Bundle extras = new Bundle();
            extras.putBoolean("disable_new_window", !this.mTabControl.canCreateNewTab());
            this.mUi.showComboView(startView, extras);
        }
    }

    protected void onBackKey() {
        if (!this.mUi.onBackKey()) {
            WebView subwindow = this.mTabControl.getCurrentSubWindow();
            if (subwindow != null) {
                if (subwindow.canGoBack()) {
                    subwindow.goBack();
                    return;
                } else {
                    dismissSubWindow(this.mTabControl.getCurrentTab());
                    return;
                }
            }
            goBackOnePageOrQuit();
        }
    }

    protected boolean onMenuKey() {
        return this.mUi.onMenuKey();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.mMenuState == -1) {
            return false;
        }
        MenuInflater inflater = this.mActivity.getMenuInflater();
        inflater.inflate(R.menu.browser, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        final WebView webview;
        WebView.HitTestResult result;
        if (!(v instanceof TitleBar) && (v instanceof WebView) && (result = (webview = (WebView) v).getHitTestResult()) != null) {
            int type = result.getType();
            if (type == 0) {
                Log.w("Controller", "We should not show context menu when nothing is touched");
                return;
            }
            if (type != 9) {
                MenuInflater inflater = this.mActivity.getMenuInflater();
                inflater.inflate(R.menu.browsercontext, menu);
                final String extra = result.getExtra();
                if (extra != null) {
                    menu.setGroupVisible(R.id.PHONE_MENU, type == 2);
                    menu.setGroupVisible(R.id.EMAIL_MENU, type == 4);
                    menu.setGroupVisible(R.id.GEO_MENU, type == 3);
                    menu.setGroupVisible(R.id.IMAGE_MENU, type == 5 || type == 8);
                    menu.setGroupVisible(R.id.ANCHOR_MENU, type == 7 || type == 8);
                    switch (type) {
                        case 2:
                            menu.setHeaderTitle(Uri.decode(extra));
                            menu.findItem(R.id.dial_context_menu_id).setIntent(new Intent("android.intent.action.VIEW", Uri.parse("tel:" + extra)));
                            Intent addIntent = new Intent("android.intent.action.INSERT_OR_EDIT");
                            addIntent.putExtra("phone", Uri.decode(extra));
                            addIntent.setType("vnd.android.cursor.item/contact");
                            menu.findItem(R.id.add_contact_context_menu_id).setIntent(addIntent);
                            menu.findItem(R.id.copy_phone_context_menu_id).setOnMenuItemClickListener(new Copy(extra));
                            break;
                        case 3:
                            menu.setHeaderTitle(extra);
                            menu.findItem(R.id.map_context_menu_id).setIntent(new Intent("android.intent.action.VIEW", Uri.parse("geo:0,0?q=" + URLEncoder.encode(extra))));
                            menu.findItem(R.id.copy_geo_context_menu_id).setOnMenuItemClickListener(new Copy(extra));
                            break;
                        case 4:
                            menu.setHeaderTitle(extra);
                            menu.findItem(R.id.email_context_menu_id).setIntent(new Intent("android.intent.action.VIEW", Uri.parse("mailto:" + extra)));
                            menu.findItem(R.id.copy_mail_context_menu_id).setOnMenuItemClickListener(new Copy(extra));
                            break;
                        case 5:
                            MenuItem shareItem = menu.findItem(R.id.share_link_context_menu_id);
                            shareItem.setVisible(type == 5);
                            if (type == 5) {
                                menu.setHeaderTitle(extra);
                                shareItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        Controller.sharePage(Controller.this.mActivity, null, extra, null, null);
                                        return true;
                                    }
                                });
                            }
                            menu.findItem(R.id.view_image_context_menu_id).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    Controller.this.openTab(extra, Controller.this.mTabControl.getCurrentTab(), true, true);
                                    return false;
                                }
                            });
                            menu.findItem(R.id.download_context_menu_id).setOnMenuItemClickListener(new Download(this.mActivity, extra, webview.isPrivateBrowsingEnabled(), webview.getSettings().getUserAgentString()));
                            menu.findItem(R.id.set_wallpaper_context_menu_id).setOnMenuItemClickListener(new WallpaperHandler(this.mActivity, extra));
                            break;
                        case 6:
                        default:
                            Log.w("Controller", "We should not get here.");
                            break;
                        case 7:
                        case 8:
                            menu.setHeaderTitle(extra);
                            boolean showNewTab = this.mTabControl.canCreateNewTab();
                            MenuItem newTabItem = menu.findItem(R.id.open_newtab_context_menu_id);
                            newTabItem.setTitle(getSettings().openInBackground() ? R.string.contextmenu_openlink_newwindow_background : R.string.contextmenu_openlink_newwindow);
                            newTabItem.setVisible(showNewTab);
                            if (showNewTab) {
                                if (8 == type) {
                                    newTabItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                        @Override
                                        public boolean onMenuItemClick(MenuItem item) {
                                            HashMap<String, WebView> hrefMap = new HashMap<>();
                                            hrefMap.put("webview", webview);
                                            Message msg = Controller.this.mHandler.obtainMessage(102, R.id.open_newtab_context_menu_id, 0, hrefMap);
                                            webview.requestFocusNodeHref(msg);
                                            return true;
                                        }
                                    });
                                } else {
                                    newTabItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                        @Override
                                        public boolean onMenuItemClick(MenuItem item) {
                                            Tab parent = Controller.this.mTabControl.getCurrentTab();
                                            Controller.this.openTab(extra, parent, !Controller.this.mSettings.openInBackground(), true);
                                            return true;
                                        }
                                    });
                                }
                            }
                            if (type != 7) {
                            }
                            break;
                    }
                    this.mUi.onContextMenuCreated(menu);
                }
            }
        }
    }

    private void updateInLoadMenuItems(Menu menu, Tab tab) {
        if (menu != null) {
            MenuItem dest = menu.findItem(R.id.stop_reload_menu_id);
            MenuItem src = (tab == null || !tab.inPageLoad()) ? menu.findItem(R.id.reload_menu_id) : menu.findItem(R.id.stop_menu_id);
            if (src != null) {
                dest.setIcon(src.getIcon());
                dest.setTitle(src.getTitle());
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateInLoadMenuItems(menu, getCurrentTab());
        this.mCachedMenu = menu;
        switch (this.mMenuState) {
            case -1:
                if (this.mCurrentMenuState != this.mMenuState) {
                    menu.setGroupVisible(R.id.MAIN_MENU, false);
                    menu.setGroupEnabled(R.id.MAIN_MENU, false);
                    menu.setGroupEnabled(R.id.MAIN_SHORTCUT_MENU, false);
                }
                break;
            default:
                if (this.mCurrentMenuState != this.mMenuState) {
                    menu.setGroupVisible(R.id.MAIN_MENU, true);
                    menu.setGroupEnabled(R.id.MAIN_MENU, true);
                    menu.setGroupEnabled(R.id.MAIN_SHORTCUT_MENU, true);
                }
                updateMenuState(getCurrentTab(), menu);
                break;
        }
        this.mCurrentMenuState = this.mMenuState;
        return this.mUi.onPrepareOptionsMenu(menu);
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
        boolean canGoBack = false;
        boolean canGoForward = false;
        boolean isHome = false;
        boolean isDesktopUa = false;
        boolean isLive = false;
        if (tab != null) {
            canGoBack = tab.canGoBack();
            canGoForward = tab.canGoForward();
            isHome = this.mSettings.getHomePage().equals(tab.getUrl());
            isDesktopUa = this.mSettings.hasDesktopUseragent(tab.getWebView());
            isLive = !tab.isSnapshot();
        }
        MenuItem back = menu.findItem(R.id.back_menu_id);
        back.setEnabled(canGoBack);
        MenuItem home = menu.findItem(R.id.homepage_menu_id);
        home.setEnabled(!isHome);
        MenuItem forward = menu.findItem(R.id.forward_menu_id);
        forward.setEnabled(canGoForward);
        MenuItem source = menu.findItem(isInLoad() ? R.id.stop_menu_id : R.id.reload_menu_id);
        MenuItem dest = menu.findItem(R.id.stop_reload_menu_id);
        if (source != null && dest != null) {
            dest.setTitle(source.getTitle());
            dest.setIcon(source.getIcon());
        }
        menu.setGroupVisible(R.id.NAV_MENU, isLive);
        PackageManager pm = this.mActivity.getPackageManager();
        Intent send = new Intent("android.intent.action.SEND");
        send.setType("text/plain");
        ResolveInfo ri = pm.resolveActivity(send, 65536);
        menu.findItem(R.id.share_page_menu_id).setVisible(ri != null);
        boolean isNavDump = this.mSettings.enableNavDump();
        MenuItem nav = menu.findItem(R.id.dump_nav_menu_id);
        nav.setVisible(isNavDump);
        nav.setEnabled(isNavDump);
        this.mSettings.isDebugEnabled();
        MenuItem uaSwitcher = menu.findItem(R.id.ua_desktop_menu_id);
        uaSwitcher.setChecked(isDesktopUa);
        menu.setGroupVisible(R.id.LIVE_MENU, isLive);
        menu.setGroupVisible(R.id.SNAPSHOT_MENU, !isLive);
        menu.setGroupVisible(R.id.COMBO_MENU, false);
        this.mUi.updateMenuState(tab, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (getCurrentTopWebView() == null) {
            return false;
        }
        if (this.mMenuIsDown) {
            this.mMenuIsDown = false;
        }
        if (this.mUi.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.stop_reload_menu_id:
                if (isInLoad()) {
                    stopLoading();
                } else {
                    getCurrentTopWebView().reload();
                }
                break;
            case R.id.forward_menu_id:
                getCurrentTab().goForward();
                break;
            case R.id.new_tab_menu_id:
                openTabToHomePage();
                break;
            case R.id.bookmarks_menu_id:
                bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
                break;
            case R.id.add_bookmark_menu_id:
                bookmarkCurrentPage();
                break;
            case R.id.LIVE_MENU:
            case R.id.SNAPSHOT_MENU:
            case R.id.COMBO_MENU:
            case R.id.MAIN_SHORTCUT_MENU:
            default:
                return false;
            case R.id.share_page_menu_id:
                Tab currentTab = this.mTabControl.getCurrentTab();
                if (currentTab == null) {
                    return false;
                }
                shareCurrentPage(currentTab);
                break;
                break;
            case R.id.find_menu_id:
                findOnPage();
                break;
            case R.id.ua_desktop_menu_id:
                toggleUserAgent();
                break;
            case R.id.snapshot_go_live:
                goLive();
                return true;
            case R.id.close_other_tabs_id:
                closeOtherTabs();
                break;
            case R.id.history_menu_id:
                bookmarksOrHistoryPicker(UI.ComboViews.History);
                break;
            case R.id.snapshots_menu_id:
                bookmarksOrHistoryPicker(UI.ComboViews.Snapshots);
                break;
            case R.id.page_info_menu_id:
                showPageInfo();
                break;
            case R.id.preferences_menu_id:
                openPreferences();
                break;
            case R.id.dump_nav_menu_id:
                getCurrentTopWebView().debugDump();
                break;
            case R.id.view_downloads_menu_id:
                viewDownloads();
                break;
            case R.id.homepage_menu_id:
                Tab current = this.mTabControl.getCurrentTab();
                loadUrl(current, this.mSettings.getHomePage());
                break;
            case R.id.zoom_in_menu_id:
                getCurrentTopWebView().zoomIn();
                break;
            case R.id.zoom_out_menu_id:
                getCurrentTopWebView().zoomOut();
                break;
            case R.id.window_one_menu_id:
            case R.id.window_two_menu_id:
            case R.id.window_three_menu_id:
            case R.id.window_four_menu_id:
            case R.id.window_five_menu_id:
            case R.id.window_six_menu_id:
            case R.id.window_seven_menu_id:
            case R.id.window_eight_menu_id:
                int menuid = item.getItemId();
                int id = 0;
                while (true) {
                    if (id < WINDOW_SHORTCUT_ID_ARRAY.length) {
                        if (WINDOW_SHORTCUT_ID_ARRAY[id] != menuid) {
                            id++;
                        } else {
                            Tab desiredTab = this.mTabControl.getTab(id);
                            if (desiredTab != null && desiredTab != this.mTabControl.getCurrentTab()) {
                                switchToTab(desiredTab);
                            }
                        }
                    }
                    break;
                }
                break;
            case R.id.back_menu_id:
                getCurrentTab().goBack();
                break;
            case R.id.goto_menu_id:
                editUrl();
                break;
            case R.id.close_menu_id:
                if (this.mTabControl.getCurrentSubWindow() != null) {
                    dismissSubWindow(this.mTabControl.getCurrentTab());
                } else {
                    closeCurrentTab();
                }
                break;
        }
        return true;
    }

    @Override
    public void toggleUserAgent() {
        WebView web = getCurrentWebView();
        this.mSettings.toggleDesktopUseragent(web);
        web.loadUrl(web.getOriginalUrl());
    }

    @Override
    public void findOnPage() {
        getCurrentTopWebView().showFindDialog(null, true);
    }

    @Override
    public void openPreferences() {
        Intent intent = new Intent(this.mActivity, (Class<?>) BrowserPreferencesPage.class);
        intent.putExtra("currentPage", getCurrentTopWebView().getUrl());
        this.mActivity.startActivityForResult(intent, 3);
    }

    @Override
    public void bookmarkCurrentPage() {
        Intent bookmarkIntent = createBookmarkCurrentPageIntent(false);
        if (bookmarkIntent != null) {
            this.mActivity.startActivity(bookmarkIntent);
        }
    }

    private void goLive() {
        Tab t = getCurrentTab();
        t.loadUrl(t.getUrl(), null);
    }

    @Override
    public void showPageInfo() {
        this.mPageDialogsHandler.showPageInfo(this.mTabControl.getCurrentTab(), false, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == R.id.CONTEXT_MENU) {
            return false;
        }
        int id = item.getItemId();
        switch (id) {
            case R.id.open_context_menu_id:
            case R.id.save_link_context_menu_id:
            case R.id.copy_link_context_menu_id:
                WebView webView = getCurrentTopWebView();
                if (webView == null) {
                    return false;
                }
                HashMap<String, WebView> hrefMap = new HashMap<>();
                hrefMap.put("webview", webView);
                Message msg = this.mHandler.obtainMessage(102, id, 0, hrefMap);
                webView.requestFocusNodeHref(msg);
                return true;
            default:
                boolean result = onOptionsItemSelected(item);
                return result;
        }
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (this.mOptionsMenuOpen) {
            if (this.mConfigChanged) {
                this.mConfigChanged = false;
            } else if (!this.mExtendedMenuOpen) {
                this.mExtendedMenuOpen = true;
                this.mUi.onExtendedMenuOpened();
            } else {
                this.mExtendedMenuOpen = false;
                this.mUi.onExtendedMenuClosed(isInLoad());
            }
        } else {
            this.mOptionsMenuOpen = true;
            this.mConfigChanged = false;
            this.mExtendedMenuOpen = false;
            this.mUi.onOptionsMenuOpened();
        }
        return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        this.mOptionsMenuOpen = false;
        this.mUi.onOptionsMenuClosed(isInLoad());
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        this.mUi.onContextMenuClosed(menu, isInLoad());
    }

    @Override
    public WebView getCurrentTopWebView() {
        return this.mTabControl.getCurrentTopWebView();
    }

    @Override
    public WebView getCurrentWebView() {
        return this.mTabControl.getCurrentWebView();
    }

    void viewDownloads() {
        Intent intent = new Intent("android.intent.action.VIEW_DOWNLOADS");
        this.mActivity.startActivity(intent);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        this.mUi.onActionModeStarted(mode);
        this.mActionMode = mode;
    }

    @Override
    public boolean isInCustomActionMode() {
        return this.mActionMode != null;
    }

    @Override
    public void endActionMode() {
        if (this.mActionMode != null) {
            this.mActionMode.finish();
        }
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        if (isInCustomActionMode()) {
            this.mUi.onActionModeFinished(isInLoad());
            this.mActionMode = null;
        }
    }

    boolean isInLoad() {
        Tab tab = getCurrentTab();
        return tab != null && tab.inPageLoad();
    }

    @Override
    public Intent createBookmarkCurrentPageIntent(boolean editExisting) {
        WebView w = getCurrentTopWebView();
        if (w == null) {
            return null;
        }
        Intent i = new Intent(this.mActivity, (Class<?>) AddBookmarkPage.class);
        i.putExtra("url", w.getUrl());
        i.putExtra("title", w.getTitle());
        String touchIconUrl = w.getTouchIconUrl();
        if (touchIconUrl != null) {
            i.putExtra("touch_icon_url", touchIconUrl);
            WebSettings settings = w.getSettings();
            if (settings != null) {
                i.putExtra("user_agent", settings.getUserAgentString());
            }
        }
        i.putExtra("thumbnail", createScreenshot(w, getDesiredThumbnailWidth(this.mActivity), getDesiredThumbnailHeight(this.mActivity)));
        i.putExtra("favicon", w.getFavicon());
        if (editExisting) {
            i.putExtra("check_for_dupe", true);
        }
        i.putExtra("gravity", 53);
        return i;
    }

    @Override
    public void showFileChooser(ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
        this.mUploadHandler = new UploadHandler(this);
        this.mUploadHandler.openFileChooser(callback, params);
    }

    static int getDesiredThumbnailWidth(Context context) {
        return context.getResources().getDimensionPixelOffset(R.dimen.bookmarkThumbnailWidth);
    }

    static int getDesiredThumbnailHeight(Context context) {
        return context.getResources().getDimensionPixelOffset(R.dimen.bookmarkThumbnailHeight);
    }

    static Bitmap createScreenshot(WebView view, int width, int height) {
        if (view == null || view.getContentHeight() == 0 || view.getContentWidth() == 0) {
            return null;
        }
        int scaledWidth = width * 2;
        int scaledHeight = height * 2;
        if (sThumbnailBitmap == null || sThumbnailBitmap.getWidth() != scaledWidth || sThumbnailBitmap.getHeight() != scaledHeight) {
            if (sThumbnailBitmap != null) {
                sThumbnailBitmap.recycle();
                sThumbnailBitmap = null;
            }
            sThumbnailBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.RGB_565);
        }
        Canvas canvas = new Canvas(sThumbnailBitmap);
        int contentWidth = view.getContentWidth();
        float overviewScale = scaledWidth / (view.getScale() * contentWidth);
        if (view instanceof BrowserWebView) {
            int dy = -((BrowserWebView) view).getTitleHeight();
            canvas.translate(0.0f, dy * overviewScale);
        }
        canvas.scale(overviewScale, overviewScale);
        if (view instanceof BrowserWebView) {
            ((BrowserWebView) view).drawContent(canvas);
        } else {
            view.draw(canvas);
        }
        Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(sThumbnailBitmap, width, height, true);
        canvas.setBitmap(null);
        return bitmapCreateScaledBitmap;
    }

    private void updateScreenshot(Tab tab) {
        final Bitmap bm;
        WebView view = tab.getWebView();
        if (view != null) {
            final String url = tab.getUrl();
            final String originalUrl = view.getOriginalUrl();
            if (!TextUtils.isEmpty(url)) {
                if ((Patterns.WEB_URL.matcher(url).matches() || tab.isBookmarkedSite()) && (bm = createScreenshot(view, getDesiredThumbnailWidth(this.mActivity), getDesiredThumbnailHeight(this.mActivity))) != null) {
                    final ContentResolver cr = this.mActivity.getContentResolver();
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... unused) {
                            Cursor cursor = null;
                            try {
                                try {
                                    cursor = Bookmarks.queryCombinedForUrl(cr, originalUrl, url);
                                    if (cursor != null && cursor.moveToFirst()) {
                                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                                        bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                                        ContentValues values = new ContentValues();
                                        values.put("thumbnail", os.toByteArray());
                                        do {
                                            values.put("url_key", cursor.getString(0));
                                            cr.update(BrowserContract.Images.CONTENT_URI, values, null, null);
                                        } while (cursor.moveToNext());
                                    }
                                } catch (SQLiteException s) {
                                    Log.w("Controller", "Error when running updateScreenshot ", s);
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                } catch (IllegalStateException e) {
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                }
                                return null;
                            } finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        }
                    }.execute(new Void[0]);
                }
            }
        }
    }

    private class Copy implements MenuItem.OnMenuItemClickListener {
        private CharSequence mText;

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Controller.this.copy(this.mText);
            return true;
        }

        public Copy(CharSequence toCopy) {
            this.mText = toCopy;
        }
    }

    private static class Download implements MenuItem.OnMenuItemClickListener {
        private Activity mActivity;
        private boolean mPrivateBrowsing;
        private String mText;
        private String mUserAgent;

        @Override
        public boolean onMenuItemClick(MenuItem item) throws Throwable {
            if (DataUri.isDataUri(this.mText)) {
                saveDataUri();
                return true;
            }
            DownloadHandler.onDownloadStartNoStream(this.mActivity, this.mText, this.mUserAgent, null, null, null, this.mPrivateBrowsing);
            return true;
        }

        public Download(Activity activity, String toDownload, boolean privateBrowsing, String userAgent) {
            this.mActivity = activity;
            this.mText = toDownload;
            this.mPrivateBrowsing = privateBrowsing;
            this.mUserAgent = userAgent;
        }

        private void saveDataUri() throws Throwable {
            DataUri uri;
            File target;
            FileOutputStream outputStream;
            FileOutputStream outputStream2 = null;
            try {
                try {
                    uri = new DataUri(this.mText);
                    target = getTarget(uri);
                    outputStream = new FileOutputStream(target);
                } catch (IOException e) {
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                outputStream.write(uri.getData());
                DownloadManager manager = (DownloadManager) this.mActivity.getSystemService("download");
                manager.addCompletedDownload(target.getName(), this.mActivity.getTitle().toString(), false, uri.getMimeType(), target.getAbsolutePath(), uri.getData().length, true);
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                }
            } catch (IOException e3) {
                outputStream2 = outputStream;
                Log.e("Controller", "Could not save data URL");
                if (outputStream2 != null) {
                    try {
                        outputStream2.close();
                    } catch (IOException e4) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                outputStream2 = outputStream;
                if (outputStream2 != null) {
                    try {
                        outputStream2.close();
                    } catch (IOException e5) {
                    }
                }
                throw th;
            }
        }

        private File getTarget(DataUri uri) throws IOException {
            File dir = this.mActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-", Locale.US);
            String nameBase = format.format(new Date());
            String mimeType = uri.getMimeType();
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String extension = mimeTypeMap.getExtensionFromMimeType(mimeType);
            if (extension == null) {
                Log.w("Controller", "Unknown mime type in data URI" + mimeType);
                extension = "dat";
            }
            File targetFile = File.createTempFile(nameBase, "." + extension, dir);
            return targetFile;
        }
    }

    protected void addTab(Tab tab) {
        this.mUi.addTab(tab);
    }

    protected void removeTab(Tab tab) {
        this.mUi.removeTab(tab);
        this.mTabControl.removeTab(tab);
        this.mCrashRecoveryHandler.backupState();
    }

    @Override
    public void setActiveTab(Tab tab) {
        if (tab != null) {
            this.mTabControl.setCurrentTab(tab);
            this.mUi.setActiveTab(tab);
        }
    }

    protected void closeEmptyTab() {
        Tab current = this.mTabControl.getCurrentTab();
        if (current != null && current.getWebView().copyBackForwardList().getSize() == 0) {
            closeCurrentTab();
        }
    }

    protected void reuseTab(Tab appTab, IntentHandler.UrlData urlData) {
        dismissSubWindow(appTab);
        this.mUi.detachTab(appTab);
        this.mTabControl.recreateWebView(appTab);
        this.mUi.attachTab(appTab);
        if (this.mTabControl.getCurrentTab() != appTab) {
            switchToTab(appTab);
            loadUrlDataIn(appTab, urlData);
        } else {
            setActiveTab(appTab);
            loadUrlDataIn(appTab, urlData);
        }
    }

    @Override
    public void dismissSubWindow(Tab tab) {
        removeSubWindow(tab);
        tab.dismissSubWindow();
        WebView wv = getCurrentTopWebView();
        if (wv != null) {
            wv.requestFocus();
        }
    }

    @Override
    public void removeSubWindow(Tab t) {
        if (t.getSubWebView() != null) {
            this.mUi.removeSubWindow(t.getSubViewContainer());
        }
    }

    @Override
    public void attachSubWindow(Tab tab) {
        if (tab.getSubWebView() != null) {
            this.mUi.attachSubWindow(tab.getSubViewContainer());
            getCurrentTopWebView().requestFocus();
        }
    }

    private Tab showPreloadedTab(IntentHandler.UrlData urlData) {
        Tab leastUsed;
        if (!urlData.isPreloaded()) {
            return null;
        }
        PreloadedTabControl tabControl = urlData.getPreloadedTab();
        String sbQuery = urlData.getSearchBoxQueryToSubmit();
        if (sbQuery != null && !tabControl.searchBoxSubmit(sbQuery, urlData.mUrl, urlData.mHeaders)) {
            tabControl.destroy();
            return null;
        }
        if (!this.mTabControl.canCreateNewTab() && (leastUsed = this.mTabControl.getLeastUsedTab(getCurrentTab())) != null) {
            closeTab(leastUsed);
        }
        Tab t = tabControl.getTab();
        t.refreshIdAfterPreload();
        this.mTabControl.addPreloadedTab(t);
        addTab(t);
        setActiveTab(t);
        return t;
    }

    public Tab openTab(IntentHandler.UrlData urlData) {
        Tab tab = showPreloadedTab(urlData);
        if (tab == null && (tab = createNewTab(false, true, true)) != null && !urlData.isEmpty()) {
            loadUrlDataIn(tab, urlData);
        }
        return tab;
    }

    @Override
    public Tab openTabToHomePage() {
        return openTab(this.mSettings.getHomePage(), false, true, false);
    }

    @Override
    public Tab openIncognitoTab() {
        return openTab("browser:incognito", true, true, false);
    }

    @Override
    public Tab openTab(String url, boolean incognito, boolean setActive, boolean useCurrent) {
        return openTab(url, incognito, setActive, useCurrent, null);
    }

    @Override
    public Tab openTab(String url, Tab parent, boolean setActive, boolean useCurrent) {
        return openTab(url, parent != null && parent.isPrivateBrowsingEnabled(), setActive, useCurrent, parent);
    }

    public Tab openTab(String url, boolean incognito, boolean setActive, boolean useCurrent, Tab parent) {
        Tab tab = createNewTab(incognito, setActive, useCurrent);
        if (tab != null) {
            if (parent != null && parent != tab) {
                parent.addChildTab(tab);
            }
            if (url != null) {
                loadUrl(tab, url);
            }
        }
        return tab;
    }

    private Tab createNewTab(boolean incognito, boolean setActive, boolean useCurrent) {
        if (this.mTabControl.canCreateNewTab()) {
            Tab tab = this.mTabControl.createNewTab(incognito);
            addTab(tab);
            if (setActive) {
                setActiveTab(tab);
                return tab;
            }
            return tab;
        }
        if (useCurrent) {
            Tab tab2 = this.mTabControl.getCurrentTab();
            reuseTab(tab2, null);
            return tab2;
        }
        this.mUi.showMaxTabsWarning();
        return null;
    }

    @Override
    public boolean switchToTab(Tab tab) {
        Tab currentTab = this.mTabControl.getCurrentTab();
        if (tab == null || tab == currentTab) {
            return false;
        }
        setActiveTab(tab);
        return true;
    }

    @Override
    public void closeCurrentTab() {
        closeCurrentTab(false);
    }

    protected void closeCurrentTab(boolean andQuit) {
        if (this.mTabControl.getTabCount() == 1) {
            this.mCrashRecoveryHandler.clearState();
            this.mTabControl.removeTab(getCurrentTab());
            this.mActivity.finish();
            return;
        }
        Tab current = this.mTabControl.getCurrentTab();
        int pos = this.mTabControl.getCurrentPosition();
        Tab newTab = current.getParent();
        if (newTab == null && (newTab = this.mTabControl.getTab(pos + 1)) == null) {
            newTab = this.mTabControl.getTab(pos - 1);
        }
        if (andQuit) {
            this.mTabControl.setCurrentTab(newTab);
            closeTab(current);
        } else if (switchToTab(newTab)) {
            closeTab(current);
        }
    }

    @Override
    public void closeTab(Tab tab) {
        if (tab == this.mTabControl.getCurrentTab()) {
            closeCurrentTab();
        } else {
            removeTab(tab);
        }
    }

    public void closeOtherTabs() {
        int inactiveTabs = this.mTabControl.getTabCount() - 1;
        for (int i = inactiveTabs; i >= 0; i--) {
            Tab tab = this.mTabControl.getTab(i);
            if (tab != this.mTabControl.getCurrentTab()) {
                removeTab(tab);
            }
        }
    }

    protected void loadUrlFromContext(String url) {
        Tab tab = getCurrentTab();
        WebView view = tab != null ? tab.getWebView() : null;
        if (url != null && url.length() != 0 && tab != null && view != null) {
            String url2 = UrlUtils.smartUrlFilter(url);
            if (!((BrowserWebView) view).getWebViewClient().shouldOverrideUrlLoading(view, url2)) {
                loadUrl(tab, url2);
            }
        }
    }

    @Override
    public void loadUrl(Tab tab, String url) {
        loadUrl(tab, url, null);
    }

    protected void loadUrl(Tab tab, String url, Map<String, String> headers) {
        if (tab != null) {
            dismissSubWindow(tab);
            tab.loadUrl(url, headers);
            this.mUi.onProgressChanged(tab);
        }
    }

    protected void loadUrlDataIn(Tab t, IntentHandler.UrlData data) {
        if (data != null && !data.isPreloaded()) {
            if (t != null && data.mDisableUrlOverride) {
                t.disableUrlOverridingForLoad();
            }
            loadUrl(t, data.mUrl, data.mHeaders);
        }
    }

    @Override
    public void onUserCanceledSsl(Tab tab) {
        if (tab.canGoBack()) {
            tab.goBack();
        } else {
            tab.loadUrl(this.mSettings.getHomePage(), null);
        }
    }

    void goBackOnePageOrQuit() {
        Tab current = this.mTabControl.getCurrentTab();
        if (current == null) {
            this.mActivity.moveTaskToBack(true);
            return;
        }
        if (current.canGoBack()) {
            current.goBack();
            return;
        }
        Tab parent = current.getParent();
        if (parent != null) {
            switchToTab(parent);
            closeTab(current);
        } else {
            if (current.getAppId() != null || current.closeOnBack()) {
                closeCurrentTab(true);
            }
            this.mActivity.moveTaskToBack(true);
        }
    }

    private Tab getNextTab() {
        int pos = this.mTabControl.getCurrentPosition() + 1;
        if (pos >= this.mTabControl.getTabCount()) {
            pos = 0;
        }
        return this.mTabControl.getTab(pos);
    }

    private Tab getPrevTab() {
        int pos = this.mTabControl.getCurrentPosition() - 1;
        if (pos < 0) {
            pos = this.mTabControl.getTabCount() - 1;
        }
        return this.mTabControl.getTab(pos);
    }

    boolean isMenuOrCtrlKey(int keyCode) {
        return 82 == keyCode || 113 == keyCode || 114 == keyCode;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean noModifiers = event.hasNoModifiers();
        if (!noModifiers && isMenuOrCtrlKey(keyCode)) {
            this.mMenuIsDown = true;
            return false;
        }
        WebView webView = getCurrentTopWebView();
        Tab tab = getCurrentTab();
        if (webView == null || tab == null) {
            return false;
        }
        boolean ctrl = event.hasModifiers(4096);
        boolean shift = event.hasModifiers(1);
        switch (keyCode) {
            case 4:
                if (noModifiers) {
                    event.startTracking();
                    return true;
                }
                break;
            case 21:
                if (ctrl) {
                    tab.goBack();
                    return true;
                }
                break;
            case 22:
                if (ctrl) {
                    tab.goForward();
                    return true;
                }
                break;
            case 48:
                if (event.isCtrlPressed()) {
                    if (event.isShiftPressed()) {
                        openIncognitoTab();
                    } else {
                        openTabToHomePage();
                    }
                    return true;
                }
                break;
            case 61:
                if (event.isCtrlPressed()) {
                    if (event.isShiftPressed()) {
                        switchToTab(getPrevTab());
                    } else {
                        switchToTab(getNextTab());
                    }
                    return true;
                }
                break;
            case 62:
                if (shift) {
                    pageUp();
                } else if (noModifiers) {
                    pageDown();
                }
                return true;
            case 125:
                if (noModifiers) {
                    tab.goForward();
                    return true;
                }
                break;
        }
        return this.mUi.dispatchKey(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 4:
                if (this.mUi.isWebShowing()) {
                    bookmarksOrHistoryPicker(UI.ComboViews.History);
                    return true;
                }
            default:
                return false;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isMenuOrCtrlKey(keyCode)) {
            this.mMenuIsDown = false;
            if (82 == keyCode && event.isTracking() && !event.isCanceled()) {
                return onMenuKey();
            }
        }
        if (!event.hasNoModifiers()) {
            return false;
        }
        switch (keyCode) {
            case 4:
                if (!event.isTracking() || event.isCanceled()) {
                    return false;
                }
                onBackKey();
                return true;
            default:
                return false;
        }
    }

    public boolean isMenuDown() {
        return this.mMenuIsDown;
    }

    @Override
    public boolean onSearchRequested() {
        this.mUi.editUrl(false, true);
        return true;
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return this.mUi.shouldCaptureThumbnails();
    }

    @Override
    public boolean supportsVoice() {
        PackageManager pm = this.mActivity.getPackageManager();
        return pm.queryIntentActivities(new Intent("android.speech.action.RECOGNIZE_SPEECH"), 0).size() != 0;
    }

    @Override
    public void startVoiceRecognizer() {
        Intent voice = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        voice.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        voice.putExtra("android.speech.extra.MAX_RESULTS", 1);
        this.mActivity.startActivityForResult(voice, 6);
    }

    @Override
    public void setBlockEvents(boolean block) {
        this.mBlockEvents = block;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return this.mBlockEvents;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return this.mBlockEvents;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return this.mBlockEvents;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return this.mBlockEvents;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return this.mBlockEvents;
    }
}
