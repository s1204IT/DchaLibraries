package com.android.browser;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import android.net.WebAddress;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.telephony.TelephonyManager;
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
import android.webkit.SavePageClient;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import com.android.browser.IntentHandler;
import com.android.browser.PermissionHelper;
import com.android.browser.UI;
import com.android.browser.provider.BrowserContract;
import com.android.browser.provider.BrowserProvider2;
import com.android.browser.provider.SnapshotProvider;
import com.android.browser.sitenavigation.SiteNavigation;
import com.mediatek.browser.ext.IBrowserMiscExt;
import com.mediatek.browser.hotknot.HotKnotHandler;
import com.mediatek.storage.StorageManagerEx;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Controller implements WebViewController, UiController, ActivityController {

    static final boolean f2assertionsDisabled;
    private static final boolean DEBUG;
    private static final String[] IMAGE_VIEWABLE_SCHEMES;
    private static final String SAVE_PAGE_DIR;
    private static final String[] STORAGE_PERMISSIONS;
    private static final int[] WINDOW_SHORTCUT_ID_ARRAY;
    private static String mSavePageFolder;
    private static Bitmap sThumbnailBitmap;
    private static HandlerThread sUpdateSavePageThread;
    private ActionMode mActionMode;
    private Activity mActivity;
    private boolean mBlockEvents;
    private ContentObserver mBookmarksObserver;
    private Notification.Builder mBuilder;
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
    private NotificationManager mNotificationManager;
    private boolean mOptionsMenuOpen;
    private PageDialogsHandler mPageDialogsHandler;
    private UpdateSavePageDBHandler mSavePageHandler;
    private boolean mShouldShowErrorConsole;
    private ContentObserver mSiteNavigationObserver;
    private SystemAllowGeolocationOrigins mSystemAllowGeolocationOrigins;
    private UI mUi;
    private UploadHandler mUploadHandler;
    private UrlHandler mUrlHandler;
    private String mVoiceResult;
    private PowerManager.WakeLock mWakeLock;
    private WallpaperHandler mWallpaperHandler = null;
    private int mCurrentMenuState = 0;
    private int mMenuState = R.id.MAIN_MENU;
    private int mOldMenuState = -1;
    private boolean mActivityPaused = true;
    private IBrowserMiscExt mBrowserMiscExt = null;
    private boolean mDelayRemoveLastTab = false;
    private BrowserSettings mSettings = BrowserSettings.getInstance();
    private TabControl mTabControl = new TabControl(this);

    static {
        f2assertionsDisabled = !Controller.class.desiredAssertionStatus();
        DEBUG = Browser.DEBUG;
        SAVE_PAGE_DIR = File.separator + "Download" + File.separator + "SavedPages";
        sUpdateSavePageThread = new HandlerThread("save_page");
        sUpdateSavePageThread.start();
        WINDOW_SHORTCUT_ID_ARRAY = new int[]{R.id.window_one_menu_id, R.id.window_two_menu_id, R.id.window_three_menu_id, R.id.window_four_menu_id, R.id.window_five_menu_id, R.id.window_six_menu_id, R.id.window_seven_menu_id, R.id.window_eight_menu_id};
        IMAGE_VIEWABLE_SCHEMES = new String[]{"data", "http", "https", "file"};
        STORAGE_PERMISSIONS = new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    }

    public Controller(Activity browser) {
        this.mActivity = browser;
        this.mSettings.setController(this);
        Intent intent = browser.getIntent();
        boolean isHotKnot = intent != null ? intent.getBooleanExtra("HotKnot_Intent", false) : false;
        if (isHotKnot) {
            this.mSettings.setLastRunPaused(true);
        }
        this.mCrashRecoveryHandler = CrashRecoveryHandler.initialize(this);
        this.mCrashRecoveryHandler.preloadCrashState();
        this.mFactory = new BrowserWebViewFactory(browser);
        this.mUrlHandler = new UrlHandler(this);
        this.mIntentHandler = new IntentHandler(this.mActivity, this);
        this.mPageDialogsHandler = new PageDialogsHandler(this.mActivity, this);
        startHandler();
        this.mSavePageHandler = new UpdateSavePageDBHandler(sUpdateSavePageThread.getLooper());
        this.mBuilder = new Notification.Builder(this.mActivity);
        this.mNotificationManager = (NotificationManager) this.mActivity.getSystemService("notification");
        this.mBookmarksObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                int size = Controller.this.mTabControl.getTabCount();
                for (int i = 0; i < size; i++) {
                    Controller.this.mTabControl.getTab(i).updateBookmarkedStatus();
                }
            }
        };
        this.mSiteNavigationObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                Log.d("Controller", "SiteNavigation.SITE_NAVIGATION_URI changed");
                if (Controller.this.getCurrentTopWebView() == null || Controller.this.getCurrentTopWebView().getUrl() == null || !Controller.this.getCurrentTopWebView().getUrl().equals("content://com.android.browser.site_navigation/websites")) {
                    return;
                }
                Log.d("Controller", "start reload");
                Controller.this.getCurrentTopWebView().reload();
            }
        };
        browser.getContentResolver().registerContentObserver(BrowserContract.Bookmarks.CONTENT_URI, true, this.mBookmarksObserver);
        browser.getContentResolver().registerContentObserver(SiteNavigation.SITE_NAVIGATION_URI, true, this.mSiteNavigationObserver);
        this.mNetworkHandler = new NetworkStateHandler(this.mActivity, this);
        this.mSystemAllowGeolocationOrigins = new SystemAllowGeolocationOrigins(this.mActivity.getApplicationContext());
        this.mSystemAllowGeolocationOrigins.start();
        openIconDatabase();
        HotKnotHandler.hotKnotInit(this.mActivity);
    }

    @Override
    public void start(Intent intent) {
        this.mCrashRecoveryHandler.startRecovery(intent);
    }

    void doStart(final Bundle icicle, final Intent intent) {
        Calendar calendar = icicle != null ? (Calendar) icicle.getSerializable("lastActiveDate") : null;
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(5, -1);
        final boolean restoreIncognitoTabs = (calendar == null || calendar.before(yesterday) || calendar.after(today)) ? false : true;
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

    public void onPreloginFinished(Bundle icicle, Intent intent, long currentTabId, boolean restoreIncognitoTabs) {
        IntentHandler.UrlData urlData;
        Tab t;
        int scale;
        if (currentTabId == -1) {
            BackgroundHandler.execute(new PruneThumbnails(this.mActivity, null));
            if (intent == null) {
                openTabToHomePage();
            } else {
                Bundle extra = intent.getExtras();
                if (intent.getData() != null && "android.intent.action.VIEW".equals(intent.getAction()) && intent.getData().toString().startsWith("content://")) {
                    urlData = new IntentHandler.UrlData(intent.getData().toString());
                } else {
                    urlData = IntentHandler.getUrlDataFromIntent(intent);
                }
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
        if (intent == null || !"show_bookmarks".equals(intent.getAction())) {
            return;
        }
        bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
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
        boolean zIsPrivateBrowsingEnabled;
        endActionMode();
        WebView mainView = tab.getWebView();
        WebViewFactory webViewFactory = this.mFactory;
        if (mainView == null) {
            zIsPrivateBrowsingEnabled = false;
        } else {
            zIsPrivateBrowsingEnabled = mainView.isPrivateBrowsingEnabled();
        }
        WebView subView = webViewFactory.createWebView(zIsPrivateBrowsingEnabled);
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

    @Override
    public UI getUi() {
        return this.mUi;
    }

    int getMaxTabs() {
        int num = this.mActivity.getResources().getInteger(R.integer.max_tabs);
        String optimize = SystemProperties.get("ro.mtk_gmo_ram_optimize");
        if (optimize != null && optimize.equals("1")) {
            return num / 2;
        }
        return num;
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
                        String title = (String) msg.getData().get("title");
                        String src = (String) msg.getData().get("src");
                        if (Controller.DEBUG) {
                            Log.i("browser", "Controller.startHandler()--->FOCUS_NODE_HREF----url : " + url + ", title : " + title + ", src : " + src);
                        }
                        if (url == "") {
                            url = src;
                        }
                        if (!TextUtils.isEmpty(url)) {
                            HashMap focusNodeMap = (HashMap) msg.obj;
                            WebView view = (WebView) focusNodeMap.get("webview");
                            if (Controller.this.getCurrentTopWebView() == view) {
                                switch (msg.arg1) {
                                    case R.id.open_context_menu_id:
                                        if (url != null && url.startsWith("rtsp://")) {
                                            Intent i = new Intent();
                                            i.setAction("android.intent.action.VIEW");
                                            i.setData(Uri.parse(url.replaceAll(" ", "%20")));
                                            i.addFlags(268435456);
                                            Controller.this.mActivity.startActivity(i);
                                        } else if (url != null && url.startsWith("wtai://wp/mc;")) {
                                            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("tel:" + url.replaceAll(" ", "%20").substring("wtai://wp/mc;".length())));
                                            Controller.this.mActivity.startActivity(intent);
                                        } else {
                                            Controller.this.loadUrlFromContext(url);
                                        }
                                        break;
                                    case R.id.open_newtab_context_menu_id:
                                        Tab parent = Controller.this.mTabControl.getCurrentTab();
                                        Controller.this.openTab(url, parent, !Controller.this.mSettings.openInBackground(), true);
                                        break;
                                    case R.id.save_link_context_menu_id:
                                    case R.id.download_context_menu_id:
                                        DownloadHandler.onDownloadStartNoStream(Controller.this.mActivity, url, view.getSettings().getUserAgentString(), null, null, null, view.isPrivateBrowsingEnabled(), 0L);
                                        break;
                                    case R.id.copy_link_context_menu_id:
                                        Controller.this.copy(url);
                                        break;
                                    case R.id.save_link_tobookmark_context_menu_id:
                                        Intent bookmarkIntent = Controller.this.createBookmarkLinkIntent(url);
                                        if (bookmarkIntent != null) {
                                            Controller.this.mActivity.startActivity(bookmarkIntent);
                                        }
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
                            if (Controller.DEBUG) {
                                Log.i("browser", "Controller.startHandler()--->RELEASE_WAKELOCK");
                            }
                            Controller.this.mWakeLock.release();
                            Controller.this.mTabControl.stopAllLoading();
                            break;
                        }
                        break;
                    case 108:
                        if (Controller.DEBUG) {
                            Log.i("browser", "Controller.startHandler()--->UPDATE_BOOKMARK_THUMBNAIL");
                        }
                        Tab tab = (Tab) msg.obj;
                        if (tab != null) {
                            Controller.this.updateScreenshot(tab);
                        }
                        break;
                    case 201:
                        if (Controller.DEBUG) {
                            Log.i("browser", "Controller.startHandler()--->OPEN_BOOKMARKS");
                        }
                        Controller.this.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
                        break;
                    case 1001:
                        if (Controller.DEBUG) {
                            Log.i("browser", "Controller.startHandler()--->LOAD_URL");
                        }
                        Controller.this.loadUrlFromContext((String) msg.obj);
                        break;
                    case 1002:
                        if (Controller.DEBUG) {
                            Log.i("browser", "Controller.startHandler()--->STOP_LOAD");
                        }
                        Controller.this.stopLoading();
                        break;
                    case 1100:
                        Controller.this.getTabControl().freeMemory();
                        new CheckMemoryTask(Controller.this.mHandler).execute(Integer.valueOf(Controller.this.getTabControl().getVisibleWebviewNums()), null, false, null, Controller.this.getTabControl().getFreeTabIndex(), false);
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
        if (tab == null) {
            return;
        }
        sharePage(this.mActivity, tab.getTitle(), tab.getUrl(), tab.getFavicon(), createScreenshot(tab.getWebView(), getDesiredThumbnailWidth(this.mActivity), getDesiredThumbnailHeight(this.mActivity)));
    }

    static final void sharePage(Context c, String title, String url, Bitmap favicon, Bitmap screenshot) {
        Intent send = new Intent("android.intent.action.SEND");
        send.setType("text/plain");
        send.putExtra("android.intent.extra.TEXT", url);
        send.putExtra("android.intent.extra.SUBJECT", title);
        if (favicon != null && favicon.getWidth() > 60) {
            favicon = Bitmap.createScaledBitmap(favicon, 60, 60, true);
        }
        send.putExtra("share_favicon", favicon);
        send.putExtra("share_screenshot", screenshot);
        try {
            c.startActivity(Intent.createChooser(send, c.getString(R.string.choosertitle_sharevia)));
        } catch (ActivityNotFoundException e) {
        }
    }

    public void copy(CharSequence text) {
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
        this.mSettings.onConfigurationChanged(config);
    }

    @Override
    public void handleNewIntent(Intent intent) {
        if (getTabControl().getTabCount() == 0) {
            start(intent);
        }
        if (!this.mUi.isWebShowing()) {
            this.mUi.showWeb(false);
        }
        this.mIntentHandler.onNewIntent(intent);
    }

    @Override
    public void onPause() {
        if (this.mCachedMenu != null) {
            this.mCachedMenu.close();
        }
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
            if (!this.mDelayRemoveLastTab && !pauseWebViewTimers(tab)) {
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
        if (sThumbnailBitmap == null) {
            return;
        }
        sThumbnailBitmap.recycle();
        sThumbnailBitmap = null;
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
        if (this.mVoiceResult == null) {
            return;
        }
        this.mUi.onVoiceResult(this.mVoiceResult);
        this.mVoiceResult = null;
    }

    private void releaseWakeLock() {
        if (this.mWakeLock == null || !this.mWakeLock.isHeld()) {
            return;
        }
        this.mHandler.removeMessages(107);
        this.mWakeLock.release();
    }

    private void resumeWebViewTimers(Tab tab) {
        boolean inLoad = tab.inPageLoad();
        if ((this.mActivityPaused || inLoad) && (!this.mActivityPaused || !inLoad)) {
            return;
        }
        CookieSyncManager.getInstance().startSync();
        WebView w = tab.getWebView();
        WebViewTimersControl.getInstance().onBrowserActivityResume(w);
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
        if (this.mPageDialogsHandler != null) {
            this.mPageDialogsHandler.destroyDialogs();
        }
        if (this.mWallpaperHandler != null) {
            this.mWallpaperHandler.destroyDialog();
            this.mWallpaperHandler = null;
        }
        if (this.mUploadHandler != null && !this.mUploadHandler.handled()) {
            this.mUploadHandler.onResult(0, null);
            this.mUploadHandler = null;
        }
        if (this.mTabControl == null) {
            return;
        }
        this.mUi.onDestroy();
        Tab t = this.mTabControl.getCurrentTab();
        if (t != null) {
            dismissSubWindow(t);
            removeTab(t);
        }
        this.mActivity.getContentResolver().unregisterContentObserver(this.mBookmarksObserver);
        this.mActivity.getContentResolver().unregisterContentObserver(this.mSiteNavigationObserver);
        this.mTabControl.destroy();
        WebIconDatabase.getInstance().close();
        this.mSystemAllowGeolocationOrigins.stop();
        this.mSystemAllowGeolocationOrigins = null;
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
        if (show == this.mShouldShowErrorConsole) {
            return;
        }
        this.mShouldShowErrorConsole = show;
        Tab t = this.mTabControl.getCurrentTab();
        if (t == null) {
            return;
        }
        this.mUi.setShouldShowErrorConsole(t, show);
    }

    @Override
    public void stopLoading() {
        this.mLoadStopped = true;
        Tab tab = this.mTabControl.getCurrentTab();
        WebView w = getCurrentTopWebView();
        if (w == null) {
            return;
        }
        w.stopLoading();
        this.mUi.onPageStopped(tab);
    }

    boolean didUserStopLoading() {
        return this.mLoadStopped;
    }

    @Override
    public void onPageStarted(Tab tab, WebView view, Bitmap favicon) {
        this.mHandler.removeMessages(108, tab);
        CookieSyncManager.getInstance().resetSync();
        this.mBrowserMiscExt = Extensions.getMiscPlugin(this.mActivity);
        this.mBrowserMiscExt.processNetworkNotify(view, this.mActivity, this.mNetworkHandler.isNetworkUp());
        if (this.mActivityPaused) {
            resumeWebViewTimers(tab);
        }
        this.mLoadStopped = false;
        endActionMode();
        this.mUi.onTabDataChanged(tab);
        if (tab.inForeground()) {
            this.mUi.updateBottomBarState(true, tab.canGoBack() || tab.getParent() != null, tab.canGoForward());
        }
        String url = tab.getUrl();
        maybeUpdateFavicon(tab, null, url, favicon);
        Performance.tracePageStart(url);
    }

    @Override
    public void onPageFinished(Tab tab) {
        boolean zCanScrollVertically;
        if (!this.mDelayRemoveLastTab) {
            Log.i("Controller", "onPageFinished backupState " + tab.getUrl());
            this.mCrashRecoveryHandler.backupState();
        }
        this.mUi.onTabDataChanged(tab);
        if (this.mActivityPaused && pauseWebViewTimers(tab)) {
            releaseWakeLock();
        }
        if (tab.getWebView() != null && tab.inForeground()) {
            if (tab.getWebView().canScrollVertically(-1)) {
                zCanScrollVertically = true;
            } else {
                zCanScrollVertically = tab.getWebView().canScrollVertically(1);
            }
            this.mUi.updateBottomBarState(zCanScrollVertically, tab.canGoBack() || tab.getParent() != null, tab.canGoForward());
        }
        Performance.tracePageFinished();
        Performance.dumpSystemMemInfo(this.mActivity);
        ArrayList<Integer> currentTabIndex = new ArrayList<>();
        currentTabIndex.add(Integer.valueOf(getTabControl().getTabPosition(tab)));
        new CheckMemoryTask(this.mHandler).execute(Integer.valueOf(getTabControl().getVisibleWebviewNums()), currentTabIndex, true, tab.getUrl(), null, false);
    }

    @Override
    public void onProgressChanged(Tab tab) {
        boolean zCanScrollVertically;
        boolean z = true;
        int newProgress = tab.getLoadProgress();
        Log.i("Controller", "onProgressChanged url: " + tab.getUrl() + " : " + newProgress + "%");
        if (newProgress == 100) {
            CookieSyncManager.getInstance().sync();
            if (!tab.isPrivateBrowsingEnabled() && !TextUtils.isEmpty(tab.getUrl()) && !tab.isSnapshot() && tab.shouldUpdateThumbnail() && (((tab.inForeground() && !didUserStopLoading()) || !tab.inForeground()) && !this.mHandler.hasMessages(108, tab))) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(108, 0, 0, tab), 500L);
            }
            if (tab.getWebView() != null && tab.inForeground()) {
                if (tab.getWebView().canScrollVertically(-1)) {
                    zCanScrollVertically = true;
                } else {
                    zCanScrollVertically = tab.getWebView().canScrollVertically(1);
                }
                UI ui = this.mUi;
                if (!tab.canGoBack() && tab.getParent() == null) {
                    z = false;
                }
                ui.updateBottomBarState(zCanScrollVertically, z, tab.canGoForward());
            }
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
        if (TextUtils.isEmpty(pageUrl) || pageUrl.length() >= 50000 || tab.isPrivateBrowsingEnabled()) {
            return;
        }
        DataController.getInstance(this.mActivity).updateHistoryTitle(pageUrl, title);
    }

    @Override
    public void onFavicon(Tab tab, WebView view, Bitmap icon) {
        this.mUi.onTabDataChanged(tab);
        maybeUpdateFavicon(tab, view.getOriginalUrl(), view.getUrl(), icon);
    }

    @Override
    public boolean shouldOverrideUrlLoading(Tab tab, WebView view, String url) {
        boolean ret = this.mUrlHandler.shouldOverrideUrlLoading(tab, view, url);
        if (tab.inForeground()) {
            this.mUi.updateBottomBarState(true, tab.canGoBack(), tab.canGoForward());
        }
        return ret;
    }

    @Override
    public void sendErrorCode(int error, String url) {
        Intent intent = new Intent("com.android.browser.action.SEND_ERROR");
        intent.putExtra("com.android.browser.error_code_key", error);
        intent.putExtra("com.android.browser.url_key", url);
        intent.putExtra("com.android.browser.homepage_key", this.mSettings.getHomePage());
        this.mActivity.sendBroadcast(intent);
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
        if (isActivityPaused()) {
            return false;
        }
        if (event.getAction() == 0) {
            return this.mActivity.onKeyDown(event.getKeyCode(), event);
        }
        return this.mActivity.onKeyUp(event.getKeyCode(), event);
    }

    @Override
    public void doUpdateVisitedHistory(Tab tab, boolean isReload) {
        if (DEBUG) {
            Log.i("browser", "Controller.doUpdateVisitedHistory()--->tab = " + tab + ", isReload = " + isReload);
        }
        if (tab.isPrivateBrowsingEnabled()) {
            return;
        }
        String url = tab.getOriginalUrl();
        if (TextUtils.isEmpty(url) || url.regionMatches(true, 0, "about:", 0, 6)) {
            return;
        }
        DataController.getInstance(this.mActivity).updateVisitedHistory(url);
        this.mCrashRecoveryHandler.backupState();
    }

    @Override
    public void getVisitedHistory(final ValueCallback<String[]> callback) {
        AsyncTask<Void, Void, String[]> task = new AsyncTask<Void, Void, String[]>() {
            @Override
            public String[] doInBackground(Void... unused) {
                return com.android.browser.provider.Browser.getVisitedHistory(Controller.this.mActivity.getContentResolver());
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
    public void onDownloadStart(final Tab tab, final String url, final String userAgent, final String contentDisposition, String mimetype, String referer, final long contentLength) {
        WebView w = tab.getWebView();
        Log.d("browser/Controller", "onDownloadStart: dispos=" + (contentDisposition == null ? "null" : contentDisposition));
        if (contentDisposition == null || !contentDisposition.regionMatches(true, 0, "attachment", 0, 10)) {
            final Intent intent = new Intent("android.intent.action.VIEW");
            if (url.startsWith("http://vod02.v.vnet.mobi/mobi/vod/st02")) {
                mimetype = "video/3gp";
            }
            intent.setDataAndType(Uri.parse(url), mimetype);
            intent.addFlags(268435456);
            ResolveInfo info = this.mActivity.getPackageManager().resolveActivity(intent, 65536);
            Log.d("browser/Controller", "onDownloadStart: ResolveInfo=" + (info == null ? "null" : info));
            if (info != null) {
                ComponentName myName = this.mActivity.getComponentName();
                Log.d("browser/Controller", "onDownloadStart: myName=" + myName + ", myName.packageName=" + myName.getPackageName() + ", info.packageName=" + info.activityInfo.packageName + ", myName.name=" + myName.getClassName() + ", info.name=" + info.activityInfo.name);
                if (!myName.getPackageName().equals(info.activityInfo.packageName) || !myName.getClassName().equals(info.activityInfo.name)) {
                    Log.d("browser/Controller", "onDownloadStart: mimetype=" + mimetype);
                    if (mimetype.equalsIgnoreCase("application/x-mpegurl") || mimetype.equalsIgnoreCase("application/vnd.apple.mpegurl")) {
                        this.mActivity.startActivity(intent);
                        if (w == null || w.copyBackForwardList().getSize() != 0) {
                            return;
                        }
                        if (tab == this.mTabControl.getCurrentTab()) {
                            goBackOnePageOrQuit();
                            return;
                        } else {
                            closeTab(tab);
                            return;
                        }
                    }
                    try {
                        final Activity activity = this.mActivity;
                        final String downloadMimetype = mimetype;
                        final TabControl downloadTabControl = this.mTabControl;
                        new AlertDialog.Builder(activity).setTitle(R.string.application_name).setIcon(android.R.drawable.ic_dialog_info).setMessage(R.string.download_or_open_content).setPositiveButton(R.string.save_content, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                DownloadHandler.onDownloadStartNoStream(activity, url, userAgent, contentDisposition, downloadMimetype, null, false, contentLength);
                                Log.d("browser/Controller", "User decide to download the content");
                                WebView web = tab.getWebView();
                                if (web != null && web.copyBackForwardList().getSize() == 0) {
                                    if (tab == downloadTabControl.getCurrentTab()) {
                                        Controller.this.goBackOnePageOrQuit();
                                    } else {
                                        Controller.this.closeTab(tab);
                                    }
                                }
                            }
                        }).setNegativeButton(R.string.open_content, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (url != null) {
                                    String urlCookie = CookieManager.getInstance().getCookie(url);
                                    Log.i("browser/Controller", "url: " + url + " url cookie: " + urlCookie);
                                    if (urlCookie != null) {
                                        intent.putExtra("url-cookie", urlCookie);
                                    }
                                }
                                activity.startActivity(intent);
                                Log.d("browser/Controller", "User decide to open the content by startActivity");
                                WebView web = tab.getWebView();
                                if (web != null && web.copyBackForwardList().getSize() == 0) {
                                    if (tab == downloadTabControl.getCurrentTab()) {
                                        Controller.this.goBackOnePageOrQuit();
                                    } else {
                                        Controller.this.closeTab(tab);
                                    }
                                }
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                Log.d("browser/Controller", "User cancel the download action");
                            }
                        }).show();
                        return;
                    } catch (ActivityNotFoundException ex) {
                        Log.d("Controller", "activity not found for " + mimetype + " over " + Uri.parse(url).getScheme(), ex);
                    }
                }
            }
        }
        Log.d("browser/Controller", "onDownloadStart: download directly, mimetype=" + mimetype + ", url=" + url);
        downloadStart(this.mActivity, url, userAgent, contentDisposition, mimetype, referer, false, contentLength, w, tab);
    }

    private void downloadStart(Activity activity, final String url, final String userAgent, final String contentDisposition, final String mimetype, final String referer, boolean privateBrowsing, final long contentLength, final WebView w, final Tab tab) {
        final List<String> ungranted = PermissionHelper.getInstance().getUngrantedPermissions(STORAGE_PERMISSIONS);
        if (ungranted.size() == 0) {
            DownloadHandler.onDownloadStart(this.mActivity, url, userAgent, contentDisposition, mimetype, referer, false, contentLength);
            if (w == null || w.copyBackForwardList().getSize() != 0) {
                return;
            }
            if (tab == this.mTabControl.getCurrentTab()) {
                goBackOnePageOrQuit();
                return;
            } else {
                closeTab(tab);
                return;
            }
        }
        PermissionHelper.getInstance().requestPermissions(ungranted, new PermissionHelper.PermissionCallback() {
            @Override
            public void onPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                Log.d("browser/Controller", " onRequestPermissionsResult " + requestCode);
                if (grantResults == null || grantResults.length <= 0) {
                    return;
                }
                boolean isGranted = true;
                Iterator p$iterator = ungranted.iterator();
                while (true) {
                    if (!p$iterator.hasNext()) {
                        break;
                    }
                    String p = (String) p$iterator.next();
                    boolean match = false;
                    int i = 0;
                    while (true) {
                        if (i < grantResults.length) {
                            if (!p.equalsIgnoreCase(permissions[i]) || grantResults[i] != 0) {
                                i++;
                            } else {
                                match = true;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (!match) {
                        Log.d("browser/Controller", p + " is not granted !");
                        isGranted = false;
                        break;
                    }
                }
                if (isGranted) {
                    DownloadHandler.onDownloadStart(Controller.this.mActivity, url, userAgent, contentDisposition, mimetype, referer, false, contentLength);
                }
                if (w == null || w.copyBackForwardList().getSize() != 0) {
                    return;
                }
                if (tab == Controller.this.mTabControl.getCurrentTab()) {
                    Controller.this.goBackOnePageOrQuit();
                } else {
                    Controller.this.closeTab(tab);
                }
            }
        });
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
        if (!f2assertionsDisabled && !tab.inForeground()) {
            throw new AssertionError();
        }
        this.mUi.showAutoLogin(tab);
    }

    @Override
    public void hideAutoLogin(Tab tab) {
        if (!f2assertionsDisabled && !tab.inForeground()) {
            throw new AssertionError();
        }
        this.mUi.hideAutoLogin(tab);
    }

    private void maybeUpdateFavicon(Tab tab, String originalUrl, String url, Bitmap favicon) {
        if (DEBUG) {
            favicon = null;
            Log.i("browser", "Controller.maybeUpdateFavicon()--->tab = " + tab + ", originalUrl = " + originalUrl + ", url = " + url + ", favicon is null:" + ((Object) null));
        }
        if (favicon == null || tab.isPrivateBrowsingEnabled()) {
            return;
        }
        Bookmarks.updateFavicon(this.mActivity.getContentResolver(), originalUrl, url, favicon);
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
        if (!tab.inForeground()) {
            return;
        }
        if (this.mUi.isCustomViewShowing()) {
            callback.onCustomViewHidden();
            return;
        }
        this.mUi.showCustomView(view, requestedOrientation, callback);
        this.mOldMenuState = this.mMenuState;
        this.mMenuState = -1;
        this.mActivity.invalidateOptionsMenu();
    }

    @Override
    public void hideCustomView() {
        if (!this.mUi.isCustomViewShowing()) {
            return;
        }
        this.mUi.onHideCustomView();
        this.mMenuState = this.mOldMenuState;
        this.mOldMenuState = -1;
        this.mActivity.invalidateOptionsMenu();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (getCurrentTopWebView() == null) {
            return;
        }
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
                        for (String str : urls) {
                            parent = openTab(str, parent, !this.mSettings.openInBackground(), true);
                        }
                    } else if (intent.hasExtra("snapshot_id")) {
                        long id = intent.getLongExtra("snapshot_id", -1L);
                        String url = intent.getStringExtra("snapshot_url");
                        if (url == null) {
                            url = this.mSettings.getHomePage();
                        }
                        if (id >= 0) {
                            Tab t2 = getCurrentTab();
                            t2.mSavePageUrl = url;
                            t2.mSavePageTitle = intent.getStringExtra("snapshot_title");
                            loadUrl(t2, url);
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
        this.mBrowserMiscExt = Extensions.getMiscPlugin(this.mActivity);
        this.mBrowserMiscExt.onActivityResult(requestCode, resultCode, intent, this.mActivity);
    }

    @Override
    public void bookmarksOrHistoryPicker(UI.ComboViews startView) {
        if (DEBUG) {
            Log.i("browser", "Controller.bookmarksOrHistoryPicker()--->startView = " + startView);
        }
        if (this.mTabControl.getCurrentWebView() == null) {
            return;
        }
        if (isInCustomActionMode()) {
            endActionMode();
        }
        Bundle extras = new Bundle();
        extras.putBoolean("disable_new_window", !this.mTabControl.canCreateNewTab());
        this.mUi.showComboView(startView, extras);
    }

    public void onBackKey() {
        if (this.mUi.onBackKey()) {
            return;
        }
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
        int i;
        if ((v instanceof TitleBar) || !(v instanceof WebView) || (result = (webview = (WebView) v).getHitTestResult()) == null) {
            return;
        }
        int type = result.getType();
        if (type == 0) {
            Log.w("Controller", "We should not show context menu when nothing is touched");
            return;
        }
        if (type == 9) {
            return;
        }
        MenuInflater inflater = this.mActivity.getMenuInflater();
        inflater.inflate(R.menu.browsercontext, menu);
        final String extra = result.getExtra();
        String imageAnchorUrlExtra = result.getImageAnchorUrlExtra();
        Log.d("browser/Controller", "sitenavigation onCreateContextMenu imageAnchorUrlExtra is : " + imageAnchorUrlExtra);
        TelephonyManager telephony = (TelephonyManager) getContext().getSystemService("phone");
        boolean mIsVoiceCapable = false;
        if (telephony != null) {
            mIsVoiceCapable = telephony.isVoiceCapable();
        }
        menu.setGroupVisible(R.id.SITE_NAVIGATION_EDIT, false);
        menu.setGroupVisible(R.id.SITE_NAVIGATION_ADD, false);
        if (mIsVoiceCapable) {
            menu.setGroupVisible(R.id.PHONE_MENU, type == 2);
            menu.setGroupVisible(R.id.NO_PHONE_MENU, false);
        } else {
            menu.setGroupVisible(R.id.PHONE_MENU, false);
            menu.setGroupVisible(R.id.NO_PHONE_MENU, type == 2);
        }
        menu.setGroupVisible(R.id.EMAIL_MENU, type == 4);
        menu.setGroupVisible(R.id.GEO_MENU, type == 3);
        boolean z = type == 5 || type == 8;
        menu.setGroupVisible(R.id.IMAGE_MENU, z);
        boolean z2 = type == 7 || type == 8;
        menu.setGroupVisible(R.id.ANCHOR_MENU, z2);
        menu.setGroupVisible(R.id.SELECT_TEXT_MENU, false);
        switch (type) {
            case 2:
                if (Uri.decode(extra).length() <= 128) {
                    menu.setHeaderTitle(Uri.decode(extra));
                } else {
                    menu.setHeaderTitle(Uri.decode(extra).substring(0, 128));
                }
                menu.findItem(R.id.dial_context_menu_id).setIntent(new Intent("android.intent.action.VIEW", Uri.parse("tel:" + extra)));
                Intent addIntent = new Intent("android.intent.action.INSERT_OR_EDIT");
                addIntent.putExtra("phone", Uri.decode(extra));
                addIntent.setType("vnd.android.cursor.item/contact");
                if (mIsVoiceCapable) {
                    menu.findItem(R.id.add_contact_context_menu_id).setIntent(addIntent);
                    menu.findItem(R.id.copy_phone_context_menu_id).setOnMenuItemClickListener(new Copy(extra));
                } else {
                    menu.findItem(R.id.add_contact_no_phone_context_menu_id).setIntent(addIntent);
                    menu.findItem(R.id.copy_no_phone_context_menu_id).setOnMenuItemClickListener(new Copy(extra));
                }
                break;
            case 3:
                if (extra.length() <= 128) {
                    menu.setHeaderTitle(extra);
                } else {
                    menu.setHeaderTitle(extra.substring(0, 128));
                }
                menu.findItem(R.id.map_context_menu_id).setIntent(new Intent("android.intent.action.VIEW", Uri.parse("geo:0,0?q=" + URLEncoder.encode(extra))));
                menu.findItem(R.id.copy_geo_context_menu_id).setOnMenuItemClickListener(new Copy(extra));
                break;
            case 4:
                if (extra.length() <= 128) {
                    menu.setHeaderTitle(extra);
                } else {
                    menu.setHeaderTitle(extra.substring(0, 128));
                }
                menu.findItem(R.id.email_context_menu_id).setIntent(new Intent("android.intent.action.VIEW", Uri.parse("mailto:" + extra)));
                menu.findItem(R.id.copy_mail_context_menu_id).setOnMenuItemClickListener(new Copy(extra));
                break;
            case 5:
                MenuItem shareItem = menu.findItem(R.id.share_link_context_menu_id);
                shareItem.setVisible(type == 5);
                if (type == 5) {
                    if (extra.length() <= 128) {
                        menu.setHeaderTitle(extra);
                    } else {
                        menu.setHeaderTitle(extra.substring(0, 128));
                    }
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
                        if (Controller.isImageViewableUri(Uri.parse(extra))) {
                            Controller.this.openTab(extra, Controller.this.mTabControl.getCurrentTab(), true, true);
                            return false;
                        }
                        Log.e("Controller", "Refusing to view image with invalid URI, \"" + extra + "\"");
                        return false;
                    }
                });
                menu.findItem(R.id.download_context_menu_id).setOnMenuItemClickListener(new Download(this.mActivity, extra, webview.isPrivateBrowsingEnabled(), webview.getSettings().getUserAgentString()));
                this.mWallpaperHandler = new WallpaperHandler(this.mActivity, extra);
                menu.findItem(R.id.set_wallpaper_context_menu_id).setOnMenuItemClickListener(this.mWallpaperHandler);
                break;
            case 6:
            default:
                Log.w("Controller", "We should not get here.");
                break;
            case 7:
            case 8:
                if (extra != null && extra.startsWith("rtsp://")) {
                    menu.findItem(R.id.save_link_context_menu_id).setVisible(false);
                }
                if (extra.length() <= 128) {
                    menu.setHeaderTitle(extra);
                } else {
                    menu.setHeaderTitle(extra.substring(0, 128));
                }
                boolean showNewTab = this.mTabControl.canCreateNewTab();
                MenuItem newTabItem = menu.findItem(R.id.open_newtab_context_menu_id);
                if (getSettings().openInBackground()) {
                    i = R.string.contextmenu_openlink_newwindow_background;
                } else {
                    i = R.string.contextmenu_openlink_newwindow;
                }
                newTabItem.setTitle(i);
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
                                if (extra != null && extra.startsWith("rtsp://")) {
                                    Intent i2 = new Intent();
                                    i2.setAction("android.intent.action.VIEW");
                                    i2.setData(Uri.parse(extra.replaceAll(" ", "%20")));
                                    i2.addFlags(268435456);
                                    Controller.this.mActivity.startActivity(i2);
                                    return true;
                                }
                                if (extra != null && extra.startsWith("wtai://wp/mc;")) {
                                    String uri = extra.replaceAll(" ", "%20");
                                    Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("tel:" + uri.substring("wtai://wp/mc;".length())));
                                    Controller.this.mActivity.startActivity(intent);
                                    return true;
                                }
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

    public static boolean isImageViewableUri(Uri uri) {
        String scheme = uri.getScheme();
        for (String allowed : IMAGE_VIEWABLE_SCHEMES) {
            if (allowed.equals(scheme)) {
                return true;
            }
        }
        return false;
    }

    private void updateShareMenuItems(Menu menu, Tab tab) {
        Log.d("browser/Controller", "updateShareMenuItems start");
        if (menu == null) {
            return;
        }
        MenuItem shareItem = menu.findItem(R.id.share_page_menu_id);
        if (tab == null) {
            Log.d("browser/Controller", "tab == null");
            shareItem.setEnabled(false);
        } else {
            String url = tab.getUrl();
            if (url == null || url.length() == 0) {
                Log.d("browser/Controller", "url == null||url.length() == 0");
                shareItem.setEnabled(false);
            } else {
                Log.d("browser/Controller", "url :" + url);
                shareItem.setEnabled(true);
            }
        }
        Log.d("browser/Controller", "updateShareMenuItems end");
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (getCurrentTab() != null) {
            updateShareMenuItems(menu, getCurrentTab());
        }
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
        MenuItem stop = menu.findItem(R.id.stop_menu_id);
        stop.setEnabled(isInLoad());
        menu.setGroupVisible(R.id.NAV_MENU, isLive);
        if (BrowserSettings.getInstance().useFullscreen() || BrowserSettings.getInstance().useQuickControls()) {
            forward.setVisible(true);
            forward.setEnabled(canGoForward);
        } else {
            forward.setVisible(false);
        }
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
        uaSwitcher.setEnabled(true);
        menu.setGroupVisible(R.id.LIVE_MENU, isLive);
        menu.setGroupVisible(R.id.SNAPSHOT_MENU, !isLive);
        menu.setGroupVisible(R.id.COMBO_MENU, false);
        if (tab != null) {
            WebView view = tab.getWebView();
            boolean useGMS = true;
            Method[] method = view.getClass().getMethods();
            int i = 0;
            while (true) {
                if (i >= method.length) {
                    break;
                }
                if (!method[i].getName().equals("setSavePageClient")) {
                    i++;
                } else {
                    useGMS = false;
                    break;
                }
            }
            Log.d("browser/SavePage", "install GMS: " + useGMS);
            menu.findItem(R.id.save_snapshot_menu_id).setVisible(!useGMS);
            String url = tab.getUrl();
            if (!useGMS && (url.startsWith("about:blank") || url.startsWith("content:") || url.startsWith("file:") || url.length() == 0)) {
                menu.findItem(R.id.save_snapshot_menu_id).setEnabled(false);
            } else {
                menu.findItem(R.id.save_snapshot_menu_id).setEnabled(true);
            }
        } else {
            menu.findItem(R.id.save_snapshot_menu_id).setEnabled(false);
        }
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
            case R.id.reload_menu_id:
                if (getCurrentTopWebView() != null) {
                    getCurrentTopWebView().reload();
                }
                return true;
            case R.id.forward_menu_id:
                getCurrentTab().goForward();
                return true;
            case R.id.stop_menu_id:
                stopLoading();
                return true;
            case R.id.home_menu_id:
            case R.id.homepage_menu_id:
                Tab current = this.mTabControl.getCurrentTab();
                loadUrl(current, this.mSettings.getHomePage());
                return true;
            case R.id.add_bookmark_menu_id:
                bookmarkCurrentPage();
                return true;
            case R.id.close_browser_menu_id:
                showCloseSelectionDialog();
                return true;
            case R.id.LIVE_MENU:
            case R.id.SNAPSHOT_MENU:
            case R.id.COMBO_MENU:
            case R.id.dump_counters_menu_id:
            case R.id.MAIN_SHORTCUT_MENU:
            default:
                return false;
            case R.id.save_snapshot_menu_id:
                Tab source = getTabControl().getCurrentTab();
                if (source != null && checkStorageState() && createSavePageFolder()) {
                    createSavePageNotification();
                    WebView webview = source.getWebView();
                    BrowserSavePageClient savePageClient = new BrowserSavePageClient(source);
                    webview.setSavePageClient(savePageClient);
                    if (!webview.savePage()) {
                        Log.d("browser/SavePage", "webview.savePage() return false.");
                        Toast.makeText(this.mActivity, R.string.saved_page_failed, 1).show();
                    }
                }
                return true;
            case R.id.share_page_menu_id:
                Tab currentTab = this.mTabControl.getCurrentTab();
                if (currentTab == null) {
                    return false;
                }
                shareCurrentPage(currentTab);
                return true;
            case R.id.find_menu_id:
                findOnPage();
                return true;
            case R.id.ua_desktop_menu_id:
                toggleUserAgent();
                return true;
            case R.id.bookmarks_menu_id:
                bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
                return true;
            case R.id.new_tab_menu_id:
                openTab("about:blank", false, true, false);
                return true;
            case R.id.page_info_menu_id:
                showPageInfo();
                return true;
            case R.id.preferences_menu_id:
                openPreferences();
                return true;
            case R.id.snapshot_go_live:
                goLive();
                return true;
            case R.id.close_other_tabs_id:
                closeOtherTabs();
                return true;
            case R.id.history_menu_id:
                bookmarksOrHistoryPicker(UI.ComboViews.History);
                return true;
            case R.id.snapshots_menu_id:
                bookmarksOrHistoryPicker(UI.ComboViews.Snapshots);
                return true;
            case R.id.dump_nav_menu_id:
                getCurrentTopWebView().debugDump();
                return true;
            case R.id.view_downloads_menu_id:
                viewDownloads();
                return true;
            case R.id.zoom_in_menu_id:
                getCurrentTopWebView().zoomIn();
                return true;
            case R.id.zoom_out_menu_id:
                getCurrentTopWebView().zoomOut();
                return true;
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
                }
                return true;
            case R.id.back_menu_id:
                getCurrentTab().goBack();
                return true;
            case R.id.goto_menu_id:
                editUrl();
                return true;
            case R.id.close_menu_id:
                if (this.mTabControl.getCurrentSubWindow() != null) {
                    dismissSubWindow(this.mTabControl.getCurrentTab());
                } else {
                    closeCurrentTab();
                }
                return true;
        }
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
        if (bookmarkIntent == null) {
            return;
        }
        this.mActivity.startActivity(bookmarkIntent);
    }

    private void goLive() {
        Tab t = getCurrentTab();
        t.loadUrl(t.getUrl(), null);
    }

    private void showCloseSelectionDialog() {
        CharSequence[] items = {this.mActivity.getString(R.string.minimize), this.mActivity.getString(R.string.quit)};
        new AlertDialog.Builder(this.mActivity).setTitle(R.string.option).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    Controller.this.mActivity.moveTaskToBack(true);
                    return;
                }
                if (which != 1) {
                    return;
                }
                if (((ActivityManager) Controller.this.mActivity.getSystemService("activity")).isInLockTaskMode()) {
                    Controller.this.mActivity.showLockTaskEscapeMessage();
                    return;
                }
                Controller.this.mNotificationManager.cancelAll();
                Controller.this.mUi.hideIME();
                Controller.this.onDestroy();
                Controller.this.mActivity.finish();
                File state = new File(Controller.this.getActivity().getApplicationContext().getCacheDir(), "browser_state.parcel");
                if (state.exists()) {
                    state.delete();
                }
                Intent intent = new Intent("android.intent.action.stk.BROWSER_TERMINATION");
                Controller.this.mActivity.sendBroadcast(intent);
                int pid = Process.myPid();
                Process.killProcess(pid);
            }
        }).show();
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
            case R.id.save_link_tobookmark_context_menu_id:
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
        if (this.mActionMode == null) {
            return;
        }
        this.mActionMode.finish();
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
        if (tab != null) {
            return tab.inPageLoad();
        }
        return false;
    }

    private Intent createBookmarkPageIntent(boolean editExisting, String url, String title) {
        WebView w = getCurrentTopWebView();
        if (w == null) {
            return null;
        }
        Intent i = new Intent(this.mActivity, (Class<?>) AddBookmarkPage.class);
        if (url != null) {
            i.putExtra("url", url);
        } else {
            i.putExtra("url", w.getUrl());
        }
        if (title != null) {
            i.putExtra("title", title);
        } else {
            i.putExtra("title", w.getTitle());
        }
        String touchIconUrl = w.getTouchIconUrl();
        if (touchIconUrl != null) {
            i.putExtra("touch_icon_url", touchIconUrl);
            WebSettings settings = w.getSettings();
            if (settings != null) {
                i.putExtra("user_agent", settings.getUserAgentString());
            }
        }
        i.putExtra("thumbnail", createScreenshot(w, getDesiredThumbnailWidth(this.mActivity), getDesiredThumbnailHeight(this.mActivity)));
        Bitmap icon = w.getFavicon();
        if (icon != null && icon.getWidth() > 60) {
            icon = Bitmap.createScaledBitmap(icon, 60, 60, true);
        }
        i.putExtra("favicon", icon);
        if (editExisting) {
            i.putExtra("check_for_dupe", true);
        }
        i.putExtra("gravity", 53);
        return i;
    }

    @Override
    public Intent createBookmarkCurrentPageIntent(boolean editExisting) {
        return createBookmarkPageIntent(editExisting, null, null);
    }

    public Intent createBookmarkLinkIntent(String url) {
        return createBookmarkPageIntent(false, url, "");
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
        if (DEBUG) {
            Log.i("browser", "Controller.createScreenshot()--->webView = " + view + ", width = " + width + ", height = " + height);
        }
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
        int left = view.getScrollX();
        int top = view.getScrollY() + view.getVisibleTitleHeight();
        canvas.translate(-left, -top);
        if (DEBUG) {
            Log.d("browser", "createScreenShot()--->left = " + left + ", top = " + top + ", overviewScale = " + overviewScale);
        }
        canvas.scale(overviewScale, overviewScale, left, top);
        if (view instanceof BrowserWebView) {
            ((BrowserWebView) view).drawContent(canvas);
        } else {
            view.draw(canvas);
        }
        Bitmap ret = Bitmap.createScaledBitmap(sThumbnailBitmap, width, height, true);
        canvas.setBitmap(null);
        return ret;
    }

    public void updateScreenshot(Tab tab) {
        final Bitmap bm;
        String urlHost;
        if (DEBUG) {
            Log.i("browser", "Controller.updateScreenshot()--->tab is " + tab);
        }
        WebView view = tab.getWebView();
        if (view == null) {
            return;
        }
        final String url = tab.getUrl();
        String tempUrl = view.getOriginalUrl();
        final String originalUrl = tempUrl == null ? url : tempUrl;
        if (TextUtils.isEmpty(url)) {
            return;
        }
        Log.d("Controller", " originalUrl: " + originalUrl + " url: " + url);
        if (!Patterns.WEB_URL.matcher(url).matches() && !tab.isBookmarkedSite()) {
            return;
        }
        if ((url != null && Patterns.WEB_URL.matcher(url).matches() && ((urlHost = new WebAddress(url).getHost()) == null || urlHost.length() == 0)) || (bm = createScreenshot(view, getDesiredThumbnailWidth(this.mActivity), getDesiredThumbnailHeight(this.mActivity))) == null) {
            return;
        }
        final ContentResolver cr = this.mActivity.getContentResolver();
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... unused) {
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
            DownloadHandler.onDownloadStartNoStream(this.mActivity, this.mText, this.mUserAgent, null, null, null, this.mPrivateBrowsing, 0L);
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
                if (outputStream2 == null) {
                    return;
                }
                try {
                    outputStream2.close();
                } catch (IOException e4) {
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
        if (DEBUG) {
            Log.d("browser", "Controller.addTab()--->tab : " + tab);
        }
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
            WebView wv = tab.getWebView();
            if (DEBUG) {
                Log.d("browser", "Controller.setActiveTab()---> webview : " + wv);
            }
            if (wv == null) {
                return;
            }
            if (this.mSettings.isDesktopUserAgent(wv)) {
                this.mSettings.changeUserAgent(wv, false);
            }
        }
        if (!DEBUG) {
            return;
        }
        Log.d("browser", "Controller.setActiveTab()--->tab : " + tab);
    }

    protected void closeEmptyTab() {
        Tab current = this.mTabControl.getCurrentTab();
        if (current == null || current.getWebView().copyBackForwardList().getSize() != 0) {
            return;
        }
        closeCurrentTab();
    }

    protected void reuseTab(Tab appTab, IntentHandler.UrlData urlData) {
        if (DEBUG) {
            Log.i("browser", "Controller.reuseTab()--->tab : " + appTab + ", urlData : " + urlData);
        }
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
        if (wv == null) {
            return;
        }
        wv.requestFocus();
    }

    @Override
    public void removeSubWindow(Tab t) {
        if (t.getSubWebView() == null) {
            return;
        }
        this.mUi.removeSubWindow(t.getSubViewContainer());
    }

    @Override
    public void attachSubWindow(Tab tab) {
        if (tab.getSubWebView() == null) {
            return;
        }
        this.mUi.attachSubWindow(tab.getSubViewContainer());
        getCurrentTopWebView().requestFocus();
    }

    private Tab showPreloadedTab(IntentHandler.UrlData urlData) {
        Tab leastUsed;
        if (DEBUG) {
            Log.i("browser", "Controller.showPreloadedTab()--->urlData : " + urlData);
        }
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
        if (DEBUG) {
            Log.d("browser", "Controller.openTabToHomePage()--->");
        }
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
        return openTab(url, parent != null ? parent.isPrivateBrowsingEnabled() : false, setActive, useCurrent, parent);
    }

    public Tab openTab(String url, boolean incognito, boolean setActive, boolean useCurrent, Tab parent) {
        if (DEBUG) {
            Log.d("browser", "Controller.openTab()--->url = " + url + ", incognito = " + incognito + ", setActive = " + setActive + ", useCurrent = " + useCurrent + ", tab parent is " + parent);
        }
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
        Tab tab = null;
        if (this.mTabControl.canCreateNewTab()) {
            tab = this.mTabControl.createNewTab(incognito);
            addTab(tab);
            if (setActive) {
                setActiveTab(tab);
            }
        } else if (useCurrent) {
            tab = this.mTabControl.getCurrentTab();
            reuseTab(tab, null);
        } else {
            this.mUi.showMaxTabsWarning();
        }
        if (DEBUG) {
            Log.d("browser", "Controller.createNewTab()--->tab is " + tab);
        }
        return tab;
    }

    @Override
    public boolean switchToTab(Tab tab) {
        if (DEBUG) {
            Log.i("browser", "Controller.switchToTab()--->tab is " + tab);
        }
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

    protected boolean closeCurrentTab(boolean andQuit) {
        if (DEBUG) {
            Log.i("browser", "Controller.closeCurrentTab()--->andQuit : " + andQuit);
        }
        if (this.mTabControl.getTabCount() == 1) {
            this.mCrashRecoveryHandler.clearState();
            if (!andQuit) {
                this.mTabControl.removeTab(getCurrentTab());
            } else {
                this.mDelayRemoveLastTab = true;
            }
            this.mActivity.finish();
            return true;
        }
        Tab current = this.mTabControl.getCurrentTab();
        int pos = this.mTabControl.getCurrentPosition();
        Tab newTab = current.getParent();
        if (newTab == null && (newTab = this.mTabControl.getTab(pos + 1)) == null) {
            newTab = this.mTabControl.getTab(pos - 1);
        }
        if (andQuit) {
            this.mTabControl.setCurrentTab(newTab);
            this.mUi.closeTableDelay(current);
            return false;
        }
        if (switchToTab(newTab)) {
            closeTab(current);
            return false;
        }
        return false;
    }

    @Override
    public void closeTab(Tab tab) {
        if (DEBUG) {
            Log.i("browser", "Controller.closeTab()--->tab is " + tab);
        }
        ArrayList<Integer> closeTabIndex = new ArrayList<>();
        closeTabIndex.add(Integer.valueOf(this.mTabControl.getTabPosition(tab)));
        if (tab == this.mTabControl.getCurrentTab()) {
            closeCurrentTab();
        } else {
            removeTab(tab);
        }
        new CheckMemoryTask(this.mHandler).execute(Integer.valueOf(getTabControl().getVisibleWebviewNums()), closeTabIndex, false, null, null, true);
    }

    public void closeOtherTabs() {
        if (DEBUG) {
            Log.i("browser", "Controller.closeOtherTabs()--->");
        }
        int inactiveTabs = this.mTabControl.getTabCount() - 1;
        ArrayList<Integer> closeTabIndexs = new ArrayList<>();
        for (int i = inactiveTabs; i >= 0; i--) {
            Tab tab = this.mTabControl.getTab(i);
            if (tab != this.mTabControl.getCurrentTab()) {
                int tabIndex = this.mTabControl.getTabPosition(tab);
                closeTabIndexs.add(Integer.valueOf(tabIndex));
                removeTab(tab);
            }
        }
        new CheckMemoryTask(this.mHandler).execute(Integer.valueOf(getTabControl().getVisibleWebviewNums()), closeTabIndexs, false, null, null, true);
    }

    protected void loadUrlFromContext(String url) {
        if (DEBUG) {
            Log.i("browser", "Controller.loadUrlFromContext()--->url : " + url);
        }
        Tab tab = getCurrentTab();
        WebView webView = tab != null ? tab.getWebView() : null;
        if (url == null || url.length() == 0 || tab == null || webView == null) {
            return;
        }
        String url2 = UrlUtils.smartUrlFilter(url);
        if (((BrowserWebView) webView).getWebViewClient().shouldOverrideUrlLoading(webView, url2)) {
            return;
        }
        loadUrl(tab, url2);
    }

    @Override
    public void loadUrl(Tab tab, String url) {
        loadUrl(tab, url, null);
    }

    protected void loadUrl(Tab tab, String url, Map<String, String> headers) {
        if (DEBUG) {
            Log.d("browser", "Controller.loadUrl()--->tab : " + tab + ", url = " + url + ", headers : " + headers);
        }
        if (tab == null) {
            return;
        }
        dismissSubWindow(tab);
        tab.loadUrl(url, headers);
        this.mUi.onProgressChanged(tab);
    }

    protected void loadUrlDataIn(Tab t, IntentHandler.UrlData data) {
        if (DEBUG) {
            Log.i("browser", "Controller.loadUrlDataIn()--->tab : " + t + ", Url Data : " + data);
        }
        if (data == null || data.isPreloaded()) {
            return;
        }
        if (t != null && data.mDisableUrlOverride) {
            t.disableUrlOverridingForLoad();
        }
        loadUrl(t, data.mUrl, data.mHeaders);
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
        } else {
            Tab parent = current.getParent();
            if (parent != null) {
                switchToTab(parent);
                closeTab(current);
            } else {
                if (current.getAppId() != null || current.closeOnBack()) {
                    closeCurrentTab(true);
                }
                this.mActivity.moveTaskToBack(true);
                onPause();
            }
        }
        if (!DEBUG) {
            return;
        }
        Log.i("browser", "Controller.goBackOnePageOrQuit()--->current tab is " + current);
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
                        openTab("about:blank", false, true, false);
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
            case 82:
                this.mActivity.invalidateOptionsMenu();
                break;
            case 84:
                if (!this.mUi.isWebShowing()) {
                    return true;
                }
                break;
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
                return false;
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
                if (event.isTracking() && !event.isCanceled()) {
                    onBackKey();
                    return true;
                }
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

    @Override
    public void onShowPopupWindowAttempt(Tab tab, boolean dialog, Message resultMsg) {
        this.mPageDialogsHandler.showPopupWindowAttempt(tab, dialog, resultMsg);
    }

    long getSavePageDirSize(File file) throws IOException {
        long length;
        long size = 0;
        File[] fileList = file.listFiles();
        if (fileList == null) {
            return 0L;
        }
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].isDirectory()) {
                length = getSavePageDirSize(fileList[i]);
            } else {
                length = fileList[i].length();
            }
            size += length;
        }
        return size;
    }

    class UpdateSavePageDBHandler extends Handler {
        ContentResolver mCr;

        public UpdateSavePageDBHandler(Looper l) {
            super(l);
            this.mCr = Controller.this.mActivity.getContentResolver();
        }

        @Override
        public void handleMessage(Message m) {
            String[] QUERY_TITLE = {"title"};
            String title = null;
            switch (m.what) {
                case 1984:
                    ContentValues insertValue = (ContentValues) m.obj;
                    Uri result = this.mCr.insert(SnapshotProvider.Snapshots.CONTENT_URI, insertValue);
                    long itemId = ContentUris.parseId(result);
                    int jobId = insertValue.getAsInteger("job_id").intValue();
                    Log.d("browser/SavePage", "ADD_SAVE_PAGE: " + jobId);
                    List<Tab> tabList = Controller.this.getTabControl().getTabs();
                    for (Tab t : tabList) {
                        if (t.containsDatabaseItemId(jobId)) {
                            t.addDatabaseItemId(jobId, itemId);
                            break;
                        }
                    }
                    break;
                case 1985:
                    String[] args = {String.valueOf(m.arg2), "0"};
                    Cursor queryUpdateTitle = this.mCr.query(SnapshotProvider.Snapshots.CONTENT_URI, QUERY_TITLE, "job_id = ? and is_done = ?", args, null);
                    while (queryUpdateTitle.moveToNext()) {
                        title = queryUpdateTitle.getString(0);
                    }
                    queryUpdateTitle.close();
                    Controller.this.mBuilder.setContentTitle(title).setProgress(100, m.arg1, false).setContentInfo(m.arg1 + "%").setOngoing(true).setSmallIcon(R.drawable.ic_save_page_notification);
                    Controller.this.mNotificationManager.notify(m.arg2, Controller.this.mBuilder.build());
                    ContentValues updateValue = new ContentValues();
                    updateValue.put("progress", Integer.valueOf(m.arg1));
                    String[] argsUpdate = {String.valueOf(m.arg2), "100"};
                    Log.d("browser/SavePage", "UPDATE_SAVE_PAGE: " + m.arg2);
                    this.mCr.update(SnapshotProvider.Snapshots.CONTENT_URI, updateValue, "job_id = ? and progress < ?", argsUpdate);
                    break;
                case 1986:
                    Notification.Builder builderFinish = new Notification.Builder(Controller.this.mActivity);
                    ContentValues finishValue = new ContentValues();
                    finishValue.put("progress", (Integer) 100);
                    finishValue.put("is_done", (Integer) 1);
                    String[] argsFinish = {String.valueOf(m.arg1), "0"};
                    String[] QUERY_PROJECTION = {"title", "viewstate_path"};
                    Cursor c = this.mCr.query(SnapshotProvider.Snapshots.CONTENT_URI, QUERY_PROJECTION, "job_id = ? and is_done = ?", argsFinish, null);
                    long size = 0;
                    while (c.moveToNext()) {
                        String filename = c.getString(1);
                        title = c.getString(0);
                        if (!TextUtils.isEmpty(filename)) {
                            int position = filename.lastIndexOf(File.separator);
                            String folder = filename.substring(0, position);
                            File f = new File(folder);
                            try {
                                size = Controller.this.getSavePageDirSize(f);
                            } catch (IOException e) {
                                size = 0;
                            }
                            Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
                            intent.setData(Uri.fromFile(f));
                            Controller.this.mActivity.sendBroadcast(intent);
                            break;
                        }
                    }
                    c.close();
                    finishValue.put("viewstate_size", Long.valueOf(size));
                    this.mCr.update(SnapshotProvider.Snapshots.CONTENT_URI, finishValue, "job_id = ? and is_done = ?", argsFinish);
                    builderFinish.setContentIntent(Controller.this.createSavePagePendingIntent()).setAutoCancel(true).setOngoing(false).setContentTitle(title).setSmallIcon(R.drawable.ic_save_page_notification).setContentText(Controller.this.mActivity.getText(R.string.saved_page_complete));
                    Log.d("browser/SavePage", "FINISH_SAVE_PAGE: " + m.arg1);
                    Controller.this.mNotificationManager.notify(m.arg1, builderFinish.build());
                    break;
                case 1987:
                    Notification.Builder builder = new Notification.Builder(Controller.this.mActivity);
                    String[] argsFail = {String.valueOf(m.arg1), "0"};
                    Cursor queryTitle = this.mCr.query(SnapshotProvider.Snapshots.CONTENT_URI, QUERY_TITLE, "job_id = ? and is_done = ?", argsFail, null);
                    while (queryTitle.moveToNext()) {
                        title = queryTitle.getString(0);
                        Log.d("browser/SavePage", "fail title is: " + title);
                    }
                    if (title != null) {
                        builder.setContentTitle(Controller.this.mActivity.getText(R.string.saved_page_fail) + title).setContentIntent(null).setAutoCancel(true).setOngoing(false).setSmallIcon(R.drawable.ic_save_page_notification_fail);
                    } else {
                        builder.setContentTitle(Controller.this.mActivity.getText(R.string.saved_page_fail)).setContentIntent(null).setAutoCancel(true).setOngoing(false).setSmallIcon(R.drawable.ic_save_page_notification_fail);
                    }
                    Log.d("browser/SavePage", "FAIL_SAVE_PAGE: " + m.arg1);
                    Controller.this.mNotificationManager.notify(m.arg1, builder.build());
                    queryTitle.close();
                    this.mCr.delete(SnapshotProvider.Snapshots.CONTENT_URI, "job_id = ? and is_done = ?", argsFail);
                    break;
            }
        }
    }

    class BrowserSavePageClient extends SavePageClient {
        Tab mTab;

        public BrowserSavePageClient(Tab tab) {
            this.mTab = tab;
        }

        public void getSaveDir(ValueCallback<String> callback, boolean canSaveAsComplete) {
            if (this.mTab == null) {
                return;
            }
            String title = this.mTab.getTitle();
            if (title == null) {
                title = "";
            }
            StringBuilder subFolder = new StringBuilder(title.replace(':', '.'));
            subFolder.append(System.currentTimeMillis());
            Log.d("browser/SavePage", "save dir:" + Controller.mSavePageFolder + File.separator + subFolder.toString() + File.separator);
            callback.onReceiveValue(Controller.mSavePageFolder + File.separator + subFolder.toString() + File.separator);
        }

        public void onSavePageStart(int id, String file) {
            Log.d("browser/SavePage", "onSavePageStart: " + id + " " + file);
            if (this.mTab == null) {
                Log.e("Controller", "onSavePageStart: the mTab does not exist!");
                return;
            }
            ContentValues values = this.mTab.createSavePageContentValues(id, file);
            this.mTab.addDatabaseItemId(id, -1L);
            Message addSavePage = Controller.this.mSavePageHandler.obtainMessage(1984, values);
            addSavePage.sendToTarget();
            Controller.this.mNotificationManager.notify(id, Controller.this.mBuilder.build());
        }

        public void onSaveProgressChange(int progress, int id) {
            Log.d("browser/SavePage", "onSaveProgressChange: " + progress + " " + id);
            Message updateSavePage = Controller.this.mSavePageHandler.obtainMessage(1985, progress, id);
            updateSavePage.sendToTarget();
        }

        public void onSaveFinish(int flag, int id) {
            Log.d("browser/SavePage", "onSaveFinish: " + flag + " " + id);
            this.mTab.removeDatabaseItemId(id);
            switch (flag) {
                case 1:
                    Message finishSavePage = Controller.this.mSavePageHandler.obtainMessage(1986, id, 0);
                    finishSavePage.sendToTarget();
                    break;
                default:
                    Toast.makeText(Controller.this.mActivity, R.string.saved_page_failed, 1).show();
                    Message failSavePage = Controller.this.mSavePageHandler.obtainMessage(1987, id, 0);
                    failSavePage.sendToTarget();
                    break;
            }
        }
    }

    private boolean checkStorageState() {
        String msg;
        int title;
        String status = Environment.getExternalStorageState();
        if (!status.equals("mounted")) {
            if (status.equals("shared")) {
                msg = this.mActivity.getString(R.string.download_sdcard_busy_dlg_msg);
                title = R.string.download_sdcard_busy_dlg_title;
            } else {
                msg = this.mActivity.getString(R.string.download_no_sdcard_dlg_msg);
                title = R.string.download_no_sdcard_dlg_title;
            }
            new AlertDialog.Builder(this.mActivity).setTitle(title).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(msg).setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null).show();
            return false;
        }
        return true;
    }

    private boolean createSavePageFolder() {
        String defaultStorage = StorageManagerEx.getDefaultPath();
        if (!new File(defaultStorage).canWrite()) {
            Log.d("browser/SavePage", "default path: " + defaultStorage + " can't write");
            StorageManager sm = (StorageManager) this.mActivity.getSystemService(StorageManager.class);
            StorageVolume vol = sm.getPrimaryVolume();
            defaultStorage = vol.getPath();
        }
        Log.d("browser/SavePage", "default path: " + defaultStorage);
        mSavePageFolder = defaultStorage + SAVE_PAGE_DIR;
        File dir = new File(mSavePageFolder);
        if (dir.exists() || dir.mkdirs()) {
            return true;
        }
        Toast.makeText(this.mActivity, R.string.create_folder_fail, 1).show();
        return false;
    }

    private void createSavePageNotification() {
        this.mBuilder.setContentTitle(getTabControl().getCurrentTab().getTitle());
        this.mBuilder.setSmallIcon(R.drawable.ic_save_page_notification);
        this.mBuilder.setProgress(100, 0, false);
        this.mBuilder.setTicker(this.mActivity.getText(R.string.saving_page));
        this.mBuilder.setOngoing(false);
        this.mBuilder.setContentIntent(createSavePagePendingIntent());
    }

    public PendingIntent createSavePagePendingIntent() {
        Intent intent = new Intent(this.mActivity, (Class<?>) ComboViewActivity.class);
        Bundle b = new Bundle();
        b.putLong("animate_id", 0L);
        b.putBoolean("disable_new_window", !this.mTabControl.canCreateNewTab());
        intent.putExtra("initial_view", UI.ComboViews.Snapshots.name());
        intent.putExtra("combo_args", b);
        return PendingIntent.getActivity(this.mActivity, 0, intent, 134217728);
    }
}
