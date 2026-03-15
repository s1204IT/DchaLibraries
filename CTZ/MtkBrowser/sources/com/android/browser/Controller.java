package com.android.browser;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
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
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
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
import com.android.browser.PermissionHelper;
import com.android.browser.UI;
import com.android.browser.provider.BrowserContract;
import com.android.browser.provider.BrowserProvider2;
import com.android.browser.provider.SnapshotProvider;
import com.android.browser.sitenavigation.SiteNavigation;
import com.mediatek.browser.ext.IBrowserMiscExt;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Controller implements ActivityController, UiController, WebViewController {
    static final boolean $assertionsDisabled = false;
    private static final String[] IMAGE_VIEWABLE_SCHEMES;
    private static final String[] STORAGE_PERMISSIONS;
    private static final int[] WINDOW_SHORTCUT_ID_ARRAY;
    private static String mSavePageFolder;
    private static Bitmap sThumbnailBitmap;
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
    private static final boolean DEBUG = Browser.DEBUG;
    private static final String SAVE_PAGE_DIR = File.separator + "Download" + File.separator + "SavedPages";
    private static HandlerThread sUpdateSavePageThread = new HandlerThread("save_page");
    private HashMap<Integer, Integer> mProgress = new HashMap<>();
    private WallpaperHandler mWallpaperHandler = null;
    private int mCurrentMenuState = 0;
    private int mMenuState = 2131558570;
    private int mOldMenuState = -1;
    private boolean mActivityPaused = true;
    private IBrowserMiscExt mBrowserMiscExt = null;
    private boolean mDelayRemoveLastTab = false;
    private BrowserSettings mSettings = BrowserSettings.getInstance();
    private TabControl mTabControl = new TabControl(this);

    class BrowserSavePageClient extends SavePageClient {
        Tab mTab;
        final Controller this$0;

        public BrowserSavePageClient(Controller controller, Tab tab) {
            this.this$0 = controller;
            this.mTab = tab;
        }

        @Override
        public void getSaveDir(ValueCallback<String> valueCallback, boolean z) {
            if (this.mTab != null) {
                String title = this.mTab.getTitle();
                if (title == null) {
                    title = "";
                }
                StringBuilder sb = new StringBuilder(title.replace(':', '.'));
                sb.append(System.currentTimeMillis());
                Log.d("browser/SavePage", "save dir:" + Controller.mSavePageFolder + File.separator + sb.toString() + File.separator);
                StringBuilder sb2 = new StringBuilder();
                sb2.append(Controller.mSavePageFolder);
                sb2.append(File.separator);
                sb2.append(sb.toString());
                sb2.append(File.separator);
                valueCallback.onReceiveValue(sb2.toString());
            }
        }

        @Override
        public void onSaveFinish(int i, int i2) {
            Log.d("browser/SavePage", "onSaveFinish: " + i + " " + i2);
            this.this$0.mProgress.remove(Integer.valueOf(i2));
            this.mTab.removeDatabaseItemId(i2);
            if (i == 1) {
                this.this$0.mSavePageHandler.obtainMessage(1986, i2, 0).sendToTarget();
            } else {
                Toast.makeText(this.this$0.mActivity, 2131492919, 1).show();
                this.this$0.mSavePageHandler.obtainMessage(1987, i2, 0).sendToTarget();
            }
        }

        @Override
        public void onSavePageStart(int i, String str) {
            Log.d("browser/SavePage", "onSavePageStart: " + i + " " + str);
            if (this.mTab == null) {
                Log.e("Controller", "onSavePageStart: the mTab does not exist!");
                return;
            }
            ContentValues contentValuesCreateSavePageContentValues = this.mTab.createSavePageContentValues(i, str);
            this.mTab.addDatabaseItemId(i, -1L);
            this.this$0.mSavePageHandler.obtainMessage(1984, contentValuesCreateSavePageContentValues).sendToTarget();
            this.this$0.mNotificationManager.notify(i, this.this$0.mBuilder.build());
            this.this$0.mProgress.put(Integer.valueOf(i), 0);
        }

        @Override
        public void onSaveProgressChange(int i, int i2) {
            Log.d("browser/SavePage", "onSaveProgressChange: " + i + " " + i2);
            int iIntValue = ((Integer) this.this$0.mProgress.get(Integer.valueOf(i2))).intValue();
            if ((i - iIntValue >= 25 || i == 100) && iIntValue != i) {
                this.this$0.mProgress.put(Integer.valueOf(i2), Integer.valueOf(i));
                this.this$0.mSavePageHandler.obtainMessage(1985, i, i2).sendToTarget();
            }
        }
    }

    private class Copy implements MenuItem.OnMenuItemClickListener {
        private CharSequence mText;
        final Controller this$0;

        public Copy(Controller controller, CharSequence charSequence) {
            this.this$0 = controller;
            this.mText = charSequence;
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            this.this$0.copy(this.mText);
            return true;
        }
    }

    private static class Download implements MenuItem.OnMenuItemClickListener {
        private Activity mActivity;
        private boolean mPrivateBrowsing;
        private String mText;
        private String mUserAgent;

        public Download(Activity activity, String str, boolean z, String str2) {
            this.mActivity = activity;
            this.mText = str;
            this.mPrivateBrowsing = z;
            this.mUserAgent = str2;
        }

        private File getTarget(DataUri dataUri) throws IOException {
            File externalFilesDir = this.mActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            String str = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-", Locale.US).format(new Date());
            String mimeType = dataUri.getMimeType();
            String extensionFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extensionFromMimeType == null) {
                Log.w("Controller", "Unknown mime type in data URI" + mimeType);
                extensionFromMimeType = "dat";
            }
            return File.createTempFile(str, "." + extensionFromMimeType, externalFilesDir);
        }

        private void saveDataUri() throws java.lang.Throwable {
            throw new UnsupportedOperationException("Method not decompiled: com.android.browser.Controller.Download.saveDataUri():void");
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) throws Throwable {
            if (DataUri.isDataUri(this.mText)) {
                saveDataUri();
                return true;
            }
            DownloadHandler.onDownloadStartNoStream(this.mActivity, this.mText, this.mUserAgent, null, null, null, this.mPrivateBrowsing, 0L);
            return true;
        }
    }

    private static class PruneThumbnails implements Runnable {
        private Context mContext;
        private List<Long> mIds;

        PruneThumbnails(Context context, List<Long> list) {
            this.mContext = context.getApplicationContext();
            this.mIds = list;
        }

        @Override
        public void run() {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            if (this.mIds == null || this.mIds.size() == 0) {
                contentResolver.delete(BrowserProvider2.Thumbnails.CONTENT_URI, null, null);
                return;
            }
            int size = this.mIds.size();
            StringBuilder sb = new StringBuilder();
            sb.append("_id");
            sb.append(" not in (");
            for (int i = 0; i < size; i++) {
                sb.append(this.mIds.get(i));
                if (i < size - 1) {
                    sb.append(",");
                }
            }
            sb.append(")");
            contentResolver.delete(BrowserProvider2.Thumbnails.CONTENT_URI, sb.toString(), null);
        }
    }

    class UpdateSavePageDBHandler extends Handler {
        ContentResolver mCr;
        final Controller this$0;

        public UpdateSavePageDBHandler(Controller controller, Looper looper) {
            super(looper);
            this.this$0 = controller;
            this.mCr = controller.mActivity.getContentResolver();
        }

        @Override
        public void handleMessage(Message message) {
            String string = null;
            String[] strArr = {"title"};
            switch (message.what) {
                case 1984:
                    ContentValues contentValues = (ContentValues) message.obj;
                    long id = ContentUris.parseId(this.mCr.insert(SnapshotProvider.Snapshots.CONTENT_URI, contentValues));
                    int iIntValue = contentValues.getAsInteger("job_id").intValue();
                    Log.d("browser/SavePage", "ADD_SAVE_PAGE: " + iIntValue);
                    for (Tab tab : this.this$0.getTabControl().getTabs()) {
                        if (tab.containsDatabaseItemId(iIntValue)) {
                            tab.addDatabaseItemId(iIntValue, id);
                            break;
                        }
                    }
                    break;
                case 1985:
                    Cursor cursorQuery = this.mCr.query(SnapshotProvider.Snapshots.CONTENT_URI, strArr, "job_id = ? and is_done = ?", new String[]{String.valueOf(message.arg2), "0"}, null);
                    while (cursorQuery.moveToNext()) {
                        string = cursorQuery.getString(0);
                    }
                    cursorQuery.close();
                    this.this$0.mBuilder.setContentTitle(string).setProgress(100, message.arg1, false).setContentInfo(message.arg1 + "%").setOngoing(true).setSmallIcon(2130837578);
                    this.this$0.mNotificationManager.notify(message.arg2, this.this$0.mBuilder.build());
                    ContentValues contentValues2 = new ContentValues();
                    contentValues2.put("progress", Integer.valueOf(message.arg1));
                    int i = message.arg2;
                    Log.d("browser/SavePage", "UPDATE_SAVE_PAGE: " + message.arg2);
                    this.mCr.update(SnapshotProvider.Snapshots.CONTENT_URI, contentValues2, "job_id = ? and progress < ?", new String[]{String.valueOf(i), "100"});
                    break;
                case 1986:
                    Notification.Builder builder = new Notification.Builder(this.this$0.mActivity);
                    ContentValues contentValues3 = new ContentValues();
                    contentValues3.put("progress", (Integer) 100);
                    contentValues3.put("is_done", (Integer) 1);
                    String[] strArr2 = {String.valueOf(message.arg1), "0"};
                    Cursor cursorQuery2 = this.mCr.query(SnapshotProvider.Snapshots.CONTENT_URI, new String[]{"title", "viewstate_path"}, "job_id = ? and is_done = ?", strArr2, null);
                    long savePageDirSize = 0;
                    while (cursorQuery2.moveToNext()) {
                        String string2 = cursorQuery2.getString(1);
                        string = cursorQuery2.getString(0);
                        if (!TextUtils.isEmpty(string2)) {
                            File file = new File(string2.substring(0, string2.lastIndexOf(File.separator)));
                            try {
                                savePageDirSize = this.this$0.getSavePageDirSize(file);
                            } catch (IOException e) {
                                savePageDirSize = 0;
                            }
                            Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
                            intent.setData(Uri.fromFile(file));
                            this.this$0.mActivity.sendBroadcast(intent);
                            break;
                        }
                    }
                    cursorQuery2.close();
                    contentValues3.put("viewstate_size", Long.valueOf(savePageDirSize));
                    this.mCr.update(SnapshotProvider.Snapshots.CONTENT_URI, contentValues3, "job_id = ? and is_done = ?", strArr2);
                    builder.setContentIntent(this.this$0.createSavePagePendingIntent()).setAutoCancel(true).setOngoing(false).setContentTitle(string).setSmallIcon(2130837578).setContentText(this.this$0.mActivity.getText(2131492920));
                    Log.d("browser/SavePage", "FINISH_SAVE_PAGE: " + message.arg1);
                    this.this$0.mNotificationManager.notify(message.arg1, builder.build());
                    break;
                case 1987:
                    Notification.Builder builder2 = new Notification.Builder(this.this$0.mActivity);
                    String[] strArr3 = {String.valueOf(message.arg1), "0"};
                    Cursor cursorQuery3 = this.mCr.query(SnapshotProvider.Snapshots.CONTENT_URI, strArr, "job_id = ? and is_done = ?", strArr3, null);
                    String string3 = null;
                    while (cursorQuery3.moveToNext()) {
                        string3 = cursorQuery3.getString(0);
                        if (Controller.DEBUG) {
                            Log.d("browser/SavePage", "fail title is: " + string3);
                        }
                    }
                    if (string3 != null) {
                        builder2.setContentTitle(((Object) this.this$0.mActivity.getText(2131492921)) + string3).setContentIntent(null).setAutoCancel(true).setOngoing(false).setSmallIcon(2130837579);
                    } else {
                        builder2.setContentTitle(this.this$0.mActivity.getText(2131492921)).setContentIntent(null).setAutoCancel(true).setOngoing(false).setSmallIcon(2130837579);
                    }
                    Log.d("browser/SavePage", "FAIL_SAVE_PAGE: " + message.arg1);
                    this.this$0.mNotificationManager.notify(message.arg1, builder2.build());
                    cursorQuery3.close();
                    this.mCr.delete(SnapshotProvider.Snapshots.CONTENT_URI, "job_id = ? and is_done = ?", strArr3);
                    break;
            }
        }
    }

    static {
        sUpdateSavePageThread.start();
        WINDOW_SHORTCUT_ID_ARRAY = new int[]{2131558599, 2131558600, 2131558601, 2131558602, 2131558603, 2131558604, 2131558605, 2131558606};
        IMAGE_VIEWABLE_SCHEMES = new String[]{"data", "http", "https", "file"};
        STORAGE_PERMISSIONS = new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    }

    public Controller(Activity activity) {
        this.mActivity = activity;
        this.mSettings.setController(this);
        this.mCrashRecoveryHandler = CrashRecoveryHandler.initialize(this);
        this.mCrashRecoveryHandler.preloadCrashState();
        this.mFactory = new BrowserWebViewFactory(activity);
        this.mUrlHandler = new UrlHandler(this);
        this.mIntentHandler = new IntentHandler(this.mActivity, this);
        this.mPageDialogsHandler = new PageDialogsHandler(this.mActivity, this);
        startHandler();
        this.mSavePageHandler = new UpdateSavePageDBHandler(this, sUpdateSavePageThread.getLooper());
        this.mBuilder = new Notification.Builder(this.mActivity);
        this.mNotificationManager = (NotificationManager) this.mActivity.getSystemService("notification");
        this.mBookmarksObserver = new ContentObserver(this, this.mHandler) {
            final Controller this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onChange(boolean z) {
                int tabCount = this.this$0.mTabControl.getTabCount();
                for (int i = 0; i < tabCount; i++) {
                    this.this$0.mTabControl.getTab(i).updateBookmarkedStatus();
                }
            }
        };
        this.mSiteNavigationObserver = new ContentObserver(this, this.mHandler) {
            final Controller this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onChange(boolean z) {
                Log.d("Controller", "SiteNavigation.SITE_NAVIGATION_URI changed");
                if (this.this$0.getCurrentTopWebView() == null || this.this$0.getCurrentTopWebView().getUrl() == null || !this.this$0.getCurrentTopWebView().getUrl().equals("content://com.android.browser.site_navigation/websites")) {
                    return;
                }
                Log.d("Controller", "start reload");
                this.this$0.getCurrentTopWebView().reload();
            }
        };
        activity.getContentResolver().registerContentObserver(BrowserContract.Bookmarks.CONTENT_URI, true, this.mBookmarksObserver);
        activity.getContentResolver().registerContentObserver(SiteNavigation.SITE_NAVIGATION_URI, true, this.mSiteNavigationObserver);
        this.mNetworkHandler = new NetworkStateHandler(this.mActivity, this);
        this.mSystemAllowGeolocationOrigins = new SystemAllowGeolocationOrigins(this.mActivity.getApplicationContext());
        this.mSystemAllowGeolocationOrigins.start();
        openIconDatabase();
    }

    private void copy(CharSequence charSequence) {
        ((ClipboardManager) this.mActivity.getSystemService("clipboard")).setText(charSequence);
    }

    private Intent createBookmarkPageIntent(boolean z, String str, String str2) {
        WebView currentTopWebView = getCurrentTopWebView();
        if (currentTopWebView == null) {
            return null;
        }
        Intent intent = new Intent(this.mActivity, (Class<?>) AddBookmarkPage.class);
        if (str != null) {
            intent.putExtra("url", str);
        } else {
            intent.putExtra("url", currentTopWebView.getUrl());
        }
        if (str2 != null) {
            intent.putExtra("title", str2);
        } else {
            intent.putExtra("title", currentTopWebView.getTitle());
        }
        String touchIconUrl = currentTopWebView.getTouchIconUrl();
        if (touchIconUrl != null) {
            intent.putExtra("touch_icon_url", touchIconUrl);
            WebSettings settings = currentTopWebView.getSettings();
            if (settings != null) {
                intent.putExtra("user_agent", settings.getUserAgentString());
            }
        }
        intent.putExtra("thumbnail", createScreenshot(currentTopWebView, getDesiredThumbnailWidth(this.mActivity), getDesiredThumbnailHeight(this.mActivity)));
        Bitmap favicon = currentTopWebView.getFavicon();
        if (favicon != null && favicon.getWidth() > 60) {
            favicon = Bitmap.createScaledBitmap(favicon, 60, 60, true);
        }
        intent.putExtra("favicon", favicon);
        if (z) {
            intent.putExtra("check_for_dupe", true);
        }
        intent.putExtra("gravity", 53);
        return intent;
    }

    private Tab createNewTab(boolean z, boolean z2, boolean z3) {
        Tab currentTab;
        if (this.mTabControl.canCreateNewTab()) {
            currentTab = this.mTabControl.createNewTab(z);
            addTab(currentTab);
            if (z2) {
                setActiveTab(currentTab);
            }
        } else if (z3) {
            currentTab = this.mTabControl.getCurrentTab();
            reuseTab(currentTab, null);
        } else {
            this.mUi.showMaxTabsWarning();
            currentTab = null;
        }
        if (DEBUG) {
            Log.d("browser", "Controller.createNewTab()--->tab is " + currentTab);
        }
        return currentTab;
    }

    private PendingIntent createSavePagePendingIntent() {
        Intent intent = new Intent(this.mActivity, (Class<?>) ComboViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putLong("animate_id", 0L);
        bundle.putBoolean("disable_new_window", !this.mTabControl.canCreateNewTab());
        intent.putExtra("initial_view", UI.ComboViews.Snapshots.name());
        intent.putExtra("combo_args", bundle);
        return PendingIntent.getActivity(this.mActivity, 0, intent, 134217728);
    }

    static Bitmap createScreenshot(WebView webView, int i, int i2) {
        if (DEBUG) {
            Log.i("browser", "Controller.createScreenshot()--->webView = " + webView + ", width = " + i + ", height = " + i2);
        }
        if (webView == null || webView.getContentHeight() == 0 || webView.getContentWidth() == 0) {
            return null;
        }
        int i3 = i * 2;
        int i4 = i2 * 2;
        if (sThumbnailBitmap == null || sThumbnailBitmap.getWidth() != i3 || sThumbnailBitmap.getHeight() != i4) {
            if (sThumbnailBitmap != null) {
                sThumbnailBitmap.recycle();
                sThumbnailBitmap = null;
            }
            sThumbnailBitmap = Bitmap.createBitmap(i3, i4, Bitmap.Config.RGB_565);
        }
        Canvas canvas = new Canvas(sThumbnailBitmap);
        float contentWidth = i3 / (webView.getContentWidth() * webView.getScale());
        boolean z = webView instanceof BrowserWebView;
        if (z) {
            canvas.translate(0.0f, (-((BrowserWebView) webView).getTitleHeight()) * contentWidth);
        }
        int scrollX = webView.getScrollX();
        int scrollY = webView.getScrollY() + webView.getVisibleTitleHeight();
        canvas.translate(-scrollX, -scrollY);
        if (DEBUG) {
            Log.d("browser", "createScreenShot()--->left = " + scrollX + ", top = " + scrollY + ", overviewScale = " + contentWidth);
        }
        canvas.scale(contentWidth, contentWidth, scrollX, scrollY);
        if (z) {
            ((BrowserWebView) webView).drawContent(canvas);
        } else {
            webView.draw(canvas);
        }
        Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(sThumbnailBitmap, i, i2, true);
        canvas.setBitmap(null);
        return bitmapCreateScaledBitmap;
    }

    private void downloadStart(Activity activity, String str, String str2, String str3, String str4, String str5, boolean z, long j, WebView webView, Tab tab) {
        List<String> ungrantedPermissions = PermissionHelper.getInstance().getUngrantedPermissions(STORAGE_PERMISSIONS);
        if (ungrantedPermissions.size() != 0) {
            PermissionHelper.getInstance().requestPermissions(ungrantedPermissions, new PermissionHelper.PermissionCallback(this, ungrantedPermissions, str, str2, str3, str4, str5, j, webView, tab) {
                final Controller this$0;
                final String val$contentDisposition;
                final long val$contentLength;
                final String val$mimetype;
                final String val$referer;
                final Tab val$tab;
                final List val$ungranted;
                final String val$url;
                final String val$userAgent;
                final WebView val$w;

                {
                    this.this$0 = this;
                    this.val$ungranted = ungrantedPermissions;
                    this.val$url = str;
                    this.val$userAgent = str2;
                    this.val$contentDisposition = str3;
                    this.val$mimetype = str4;
                    this.val$referer = str5;
                    this.val$contentLength = j;
                    this.val$w = webView;
                    this.val$tab = tab;
                }

                @Override
                public void onPermissionsResult(int i, String[] strArr, int[] iArr) {
                    boolean z2;
                    boolean z3;
                    Log.d("browser/Controller", " onRequestPermissionsResult " + i);
                    if (iArr == null || iArr.length <= 0) {
                        return;
                    }
                    Iterator it = this.val$ungranted.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            z2 = true;
                            break;
                        }
                        String str6 = (String) it.next();
                        int i2 = 0;
                        while (true) {
                            if (i2 >= iArr.length) {
                                z3 = false;
                                break;
                            } else {
                                if (str6.equalsIgnoreCase(strArr[i2]) && iArr[i2] == 0) {
                                    z3 = true;
                                    break;
                                }
                                i2++;
                            }
                        }
                        if (!z3) {
                            Log.d("browser/Controller", str6 + " is not granted !");
                            z2 = false;
                            break;
                        }
                    }
                    if (z2) {
                        DownloadHandler.onDownloadStart(this.this$0.mActivity, this.val$url, this.val$userAgent, this.val$contentDisposition, this.val$mimetype, this.val$referer, false, this.val$contentLength);
                    }
                    if (this.val$w == null || this.val$w.copyBackForwardList().getSize() != 0) {
                        return;
                    }
                    if (this.val$tab == this.this$0.mTabControl.getCurrentTab()) {
                        this.this$0.goBackOnePageOrQuit();
                    } else {
                        this.this$0.closeTab(this.val$tab);
                    }
                }
            });
            return;
        }
        DownloadHandler.onDownloadStart(this.mActivity, str, str2, str3, str4, str5, false, j);
        if (webView == null || webView.copyBackForwardList().getSize() != 0) {
            return;
        }
        if (tab != this.mTabControl.getCurrentTab()) {
            closeTab(tab);
        } else {
            tab.setCloseOnBack(true);
            goBackOnePageOrQuit();
        }
    }

    static int getDesiredThumbnailHeight(Context context) {
        return context.getResources().getDimensionPixelOffset(2131427336);
    }

    static int getDesiredThumbnailWidth(Context context) {
        return context.getResources().getDimensionPixelOffset(2131427335);
    }

    private Tab getNextTab() {
        int currentPosition = this.mTabControl.getCurrentPosition() + 1;
        if (currentPosition >= this.mTabControl.getTabCount()) {
            currentPosition = 0;
        }
        return this.mTabControl.getTab(currentPosition);
    }

    private Tab getPrevTab() {
        int currentPosition = this.mTabControl.getCurrentPosition() - 1;
        if (currentPosition < 0) {
            currentPosition = this.mTabControl.getTabCount() - 1;
        }
        return this.mTabControl.getTab(currentPosition);
    }

    private void goLive() {
        Tab currentTab = getCurrentTab();
        currentTab.loadUrl(currentTab.getUrl(), null);
    }

    private boolean isDefaultBookmarks(String str) {
        CharSequence[] textArray = this.mActivity.getResources().getTextArray(2131230834);
        int length = textArray.length;
        for (int i = 0; i < length; i += 2) {
            if (str.equalsIgnoreCase(textArray[i + 1].toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isImageViewableUri(Uri uri) {
        String scheme = uri.getScheme();
        for (String str : IMAGE_VIEWABLE_SCHEMES) {
            if (str.equals(scheme)) {
                return true;
            }
        }
        return false;
    }

    private void maybeUpdateFavicon(Tab tab, String str, String str2, Bitmap bitmap) {
        if (DEBUG) {
            Log.i("browser", "Controller.maybeUpdateFavicon()--->tab = " + tab + ", originalUrl = " + str + ", url = " + str2 + ", favicon is null:" + ((Object) null));
            bitmap = null;
        }
        if (bitmap == null || tab.isPrivateBrowsingEnabled()) {
            return;
        }
        Bookmarks.updateFavicon(this.mActivity.getContentResolver(), str, str2, bitmap);
    }

    private boolean needToIgnore(String str, String str2) {
        return str.equalsIgnoreCase("http://www.wo.com.cn/") || str.equalsIgnoreCase("http://www.wo.com.cn") || str2.equalsIgnoreCase("http://m.wo.cn/") || str2.equalsIgnoreCase("http://m.wo.cn");
    }

    private void onPreloginFinished(Bundle bundle, Intent intent, long j, boolean z) {
        int i;
        if (j == -1) {
            BackgroundHandler.execute(new PruneThumbnails(this.mActivity, null));
            if (intent == null) {
                openTabToHomePage();
            } else {
                Bundle extras = intent.getExtras();
                IntentHandler.UrlData urlData = (intent.getData() != null && "android.intent.action.VIEW".equals(intent.getAction()) && intent.getData().toString().startsWith("content://")) ? new IntentHandler.UrlData(intent.getData().toString()) : IntentHandler.getUrlDataFromIntent(intent);
                Tab tabOpenTabToHomePage = urlData.isEmpty() ? openTabToHomePage() : openTab(urlData);
                if (tabOpenTabToHomePage != null) {
                    tabOpenTabToHomePage.setAppId(intent.getStringExtra("com.android.browser.application_id"));
                }
                WebView webView = tabOpenTabToHomePage.getWebView();
                if (extras != null && (i = extras.getInt("browser.initialZoomLevel", 0)) > 0 && i <= 1000) {
                    webView.setInitialScale(i);
                }
            }
            this.mUi.updateTabs(this.mTabControl.getTabs());
        } else {
            this.mTabControl.restoreState(bundle, j, z, this.mUi.needsRestoreAllTabs());
            List<Tab> tabs = this.mTabControl.getTabs();
            ArrayList arrayList = new ArrayList(tabs.size());
            Iterator<Tab> it = tabs.iterator();
            while (it.hasNext()) {
                arrayList.add(Long.valueOf(it.next().getId()));
            }
            BackgroundHandler.execute(new PruneThumbnails(this.mActivity, arrayList));
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

    private void openIconDatabase() {
        BackgroundHandler.execute(new Runnable(this, WebIconDatabase.getInstance()) {
            final Controller this$0;
            final WebIconDatabase val$instance;

            {
                this.this$0 = this;
                this.val$instance = webIconDatabase;
            }

            @Override
            public void run() {
                this.val$instance.open(this.this$0.mActivity.getDir("icons", 0).getPath());
            }
        });
    }

    private boolean pauseWebViewTimers(Tab tab) {
        if (tab == null) {
            return true;
        }
        if (tab.inPageLoad()) {
            return false;
        }
        WebViewTimersControl.getInstance().onBrowserActivityPause(getCurrentWebView(), this);
        return true;
    }

    private void releaseWakeLock() {
        if (this.mWakeLock == null || !this.mWakeLock.isHeld()) {
            return;
        }
        this.mHandler.removeMessages(107);
        this.mWakeLock.release();
    }

    private void resumeWebViewTimers(Tab tab) {
        boolean zInPageLoad = tab.inPageLoad();
        if ((this.mActivityPaused || zInPageLoad) && !(this.mActivityPaused && zInPageLoad)) {
            return;
        }
        WebViewTimersControl.getInstance().onBrowserActivityResume(tab.getWebView(), this);
    }

    private void shareCurrentPage(Tab tab) {
        if (tab != null) {
            sharePage(this.mActivity, tab.getTitle(), tab.getUrl(), tab.getFavicon(), createScreenshot(tab.getWebView(), getDesiredThumbnailWidth(this.mActivity), getDesiredThumbnailHeight(this.mActivity)));
        }
    }

    static final void sharePage(Context context, String str, String str2, Bitmap bitmap, Bitmap bitmap2) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        intent.putExtra("android.intent.extra.TEXT", str2);
        intent.putExtra("android.intent.extra.SUBJECT", str);
        if (bitmap != null && bitmap.getWidth() > 60) {
            bitmap = Bitmap.createScaledBitmap(bitmap, 60, 60, true);
        }
        intent.putExtra("share_favicon", bitmap);
        intent.putExtra("share_screenshot", bitmap2);
        try {
            context.startActivity(Intent.createChooser(intent, context.getString(2131493051)));
        } catch (ActivityNotFoundException e) {
        }
    }

    private void showCloseSelectionDialog() {
        new AlertDialog.Builder(this.mActivity).setTitle(2131492874).setItems(new CharSequence[]{this.mActivity.getString(2131492875), this.mActivity.getString(2131492876)}, new DialogInterface.OnClickListener(this) {
            final Controller this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    this.this$0.mActivity.moveTaskToBack(true);
                    return;
                }
                if (i == 1) {
                    if (((ActivityManager) this.this$0.mActivity.getSystemService("activity")).isInLockTaskMode()) {
                        this.this$0.mActivity.showLockTaskEscapeMessage();
                        return;
                    }
                    this.this$0.mNotificationManager.cancelAll();
                    this.this$0.mUi.hideIME();
                    this.this$0.onDestroy();
                    this.this$0.mActivity.finish();
                    File file = new File(this.this$0.getActivity().getApplicationContext().getCacheDir(), "browser_state.parcel");
                    if (file.exists()) {
                        file.delete();
                    }
                    Intent intent = new Intent("mediatek.intent.action.stk.BROWSER_TERMINATION");
                    intent.setComponent(ComponentName.unflattenFromString("com.android.stk/.EventReceiver"));
                    this.this$0.mActivity.sendBroadcast(intent);
                    Process.killProcess(Process.myPid());
                }
            }
        }).show();
    }

    private Tab showPreloadedTab(IntentHandler.UrlData urlData) {
        Tab leastUsedTab;
        if (DEBUG) {
            Log.i("browser", "Controller.showPreloadedTab()--->urlData : " + urlData);
        }
        if (!urlData.isPreloaded()) {
            return null;
        }
        PreloadedTabControl preloadedTab = urlData.getPreloadedTab();
        String searchBoxQueryToSubmit = urlData.getSearchBoxQueryToSubmit();
        if (searchBoxQueryToSubmit != null && !preloadedTab.searchBoxSubmit(searchBoxQueryToSubmit, urlData.mUrl, urlData.mHeaders)) {
            preloadedTab.destroy();
            return null;
        }
        if (!this.mTabControl.canCreateNewTab() && (leastUsedTab = this.mTabControl.getLeastUsedTab(getCurrentTab())) != null) {
            closeTab(leastUsedTab);
        }
        Tab tab = preloadedTab.getTab();
        tab.refreshIdAfterPreload();
        this.mTabControl.addPreloadedTab(tab);
        addTab(tab);
        setActiveTab(tab);
        return tab;
    }

    private void startHandler() {
        this.mHandler = new Handler(this) {
            final Controller this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void handleMessage(Message message) {
                WebView webView;
                switch (message.what) {
                    case 102:
                        String str = (String) message.getData().get("url");
                        String str2 = (String) message.getData().get("title");
                        String str3 = (String) message.getData().get("src");
                        if (Controller.DEBUG) {
                            Log.i("browser", "Controller.startHandler()--->FOCUS_NODE_HREF----url : " + str + ", title : " + str2 + ", src : " + str3);
                        }
                        String str4 = str == "" ? str3 : str;
                        if (!TextUtils.isEmpty(str4) && this.this$0.getCurrentTopWebView() == (webView = (WebView) ((HashMap) message.obj).get("webview"))) {
                            int i = message.arg1;
                            if (i != 2131558433) {
                                switch (i) {
                                    case 2131558624:
                                        this.this$0.openTab(str4, this.this$0.mTabControl.getCurrentTab(), !this.this$0.mSettings.openInBackground(), true);
                                        break;
                                    case 2131558625:
                                        break;
                                    case 2131558626:
                                        this.this$0.copy(str4);
                                        break;
                                    case 2131558627:
                                        Intent intentCreateBookmarkLinkIntent = this.this$0.createBookmarkLinkIntent(str4);
                                        if (intentCreateBookmarkLinkIntent != null) {
                                            this.this$0.mActivity.startActivity(intentCreateBookmarkLinkIntent);
                                        }
                                        break;
                                    default:
                                        switch (i) {
                                            case 2131558630:
                                                this.this$0.loadUrlFromContext(str3);
                                                break;
                                        }
                                        break;
                                }
                                DownloadHandler.onDownloadStartNoStream(this.this$0.mActivity, str4, webView.getSettings().getUserAgentString(), null, null, null, webView.isPrivateBrowsingEnabled(), 0L);
                            } else if (str4 != null && str4.startsWith("rtsp://")) {
                                Intent intent = new Intent();
                                intent.setAction("android.intent.action.VIEW");
                                intent.setData(Uri.parse(str4.replaceAll(" ", "%20")));
                                intent.addFlags(268435456);
                                this.this$0.mActivity.startActivity(intent);
                            } else if (str4 != null && str4.startsWith("wtai://wp/mc;")) {
                                this.this$0.mActivity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("tel:" + str4.replaceAll(" ", "%20").substring("wtai://wp/mc;".length()))));
                            } else {
                                this.this$0.loadUrlFromContext(str4);
                            }
                            break;
                        }
                        break;
                    case 107:
                        if (this.this$0.mWakeLock != null && this.this$0.mWakeLock.isHeld()) {
                            if (Controller.DEBUG) {
                                Log.i("browser", "Controller.startHandler()--->RELEASE_WAKELOCK");
                            }
                            this.this$0.mWakeLock.release();
                            this.this$0.mTabControl.stopAllLoading();
                            break;
                        }
                        break;
                    case 108:
                        if (Controller.DEBUG) {
                            Log.i("browser", "Controller.startHandler()--->UPDATE_BOOKMARK_THUMBNAIL");
                        }
                        Tab tab = (Tab) message.obj;
                        if (tab != null) {
                            this.this$0.updateScreenshot(tab);
                        }
                        break;
                    case 201:
                        if (Controller.DEBUG) {
                            Log.i("browser", "Controller.startHandler()--->OPEN_BOOKMARKS");
                        }
                        this.this$0.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
                        break;
                    case 1001:
                        if (Controller.DEBUG) {
                            Log.i("browser", "Controller.startHandler()--->LOAD_URL");
                        }
                        this.this$0.loadUrlFromContext((String) message.obj);
                        break;
                    case 1002:
                        if (Controller.DEBUG) {
                            Log.i("browser", "Controller.startHandler()--->STOP_LOAD");
                        }
                        this.this$0.stopLoading();
                        break;
                    case 1100:
                        this.this$0.getTabControl().freeMemory();
                        new CheckMemoryTask(this.this$0.mHandler).execute(Integer.valueOf(this.this$0.getTabControl().getVisibleWebviewNums()), null, false, null, this.this$0.getTabControl().getFreeTabIndex(), false);
                        break;
                }
            }
        };
    }

    private void updateScreenshot(Tab tab) {
        Bitmap bitmapCreateScreenshot;
        String host;
        if (DEBUG) {
            Log.i("browser", "Controller.updateScreenshot()--->tab is " + tab);
        }
        WebView webView = tab.getWebView();
        if (webView == null) {
            return;
        }
        String url = tab.getUrl();
        String originalUrl = webView.getOriginalUrl();
        if (originalUrl == null) {
            originalUrl = url;
        }
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (DEBUG) {
            Log.d("Controller", " originalUrl: " + originalUrl + " url: " + url);
        }
        if ((Patterns.WEB_URL.matcher(url).matches() || tab.isBookmarkedSite() || isDefaultBookmarks(originalUrl)) && !needToIgnore(originalUrl, url)) {
            if ((url != null && Patterns.WEB_URL.matcher(url).matches() && ((host = new WebAddress(url).getHost()) == null || host.length() == 0)) || (bitmapCreateScreenshot = createScreenshot(webView, getDesiredThumbnailWidth(this.mActivity), getDesiredThumbnailHeight(this.mActivity))) == null) {
                return;
            }
            new AsyncTask<Void, Void, Void>(this, this.mActivity.getContentResolver(), originalUrl, url, bitmapCreateScreenshot) {
                final Controller this$0;
                final Bitmap val$bm;
                final ContentResolver val$cr;
                final String val$originalUrl;
                final String val$url;

                {
                    this.this$0 = this;
                    this.val$cr = contentResolver;
                    this.val$originalUrl = originalUrl;
                    this.val$url = url;
                    this.val$bm = bitmapCreateScreenshot;
                }

                @Override
                protected Void doInBackground(Void... voidArr) throws Throwable {
                    Cursor cursorQueryCombinedForUrl;
                    Cursor cursor;
                    Cursor cursor2 = null;
                    try {
                        try {
                            cursorQueryCombinedForUrl = Bookmarks.queryCombinedForUrl(this.val$cr, this.val$originalUrl, this.val$url);
                            if (cursorQueryCombinedForUrl != null) {
                                try {
                                    if (cursorQueryCombinedForUrl.moveToFirst()) {
                                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                        this.val$bm.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                                        ContentValues contentValues = new ContentValues();
                                        contentValues.put("thumbnail", byteArrayOutputStream.toByteArray());
                                        do {
                                            contentValues.put("url_key", cursorQueryCombinedForUrl.getString(0));
                                            this.val$cr.update(BrowserContract.Images.CONTENT_URI, contentValues, null, null);
                                        } while (cursorQueryCombinedForUrl.moveToNext());
                                    }
                                } catch (SQLiteException e) {
                                    e = e;
                                    Log.w("Controller", "Error when running updateScreenshot ", e);
                                    if (cursorQueryCombinedForUrl != null) {
                                    }
                                    return null;
                                } catch (IllegalStateException e2) {
                                    if (cursorQueryCombinedForUrl != null) {
                                    }
                                    return null;
                                }
                            }
                        } catch (Throwable th) {
                            th = th;
                            cursor2 = cursor;
                            if (cursor2 != null) {
                                cursor2.close();
                            }
                            throw th;
                        }
                    } catch (SQLiteException e3) {
                        e = e3;
                        cursorQueryCombinedForUrl = null;
                    } catch (IllegalStateException e4) {
                        cursorQueryCombinedForUrl = null;
                    } catch (Throwable th2) {
                        th = th2;
                        if (cursor2 != null) {
                        }
                        throw th;
                    }
                    if (cursorQueryCombinedForUrl != null) {
                        cursorQueryCombinedForUrl.close();
                    }
                    return null;
                }
            }.execute(new Void[0]);
        }
    }

    private void updateShareMenuItems(Menu menu, Tab tab) {
        Log.d("browser/Controller", "updateShareMenuItems start");
        if (menu == null) {
            return;
        }
        MenuItem menuItemFindItem = menu.findItem(2131558579);
        if (tab == null) {
            Log.d("browser/Controller", "tab == null");
            menuItemFindItem.setEnabled(false);
        } else {
            String url = tab.getUrl();
            if (url == null || url.length() == 0) {
                Log.d("browser/Controller", "url == null||url.length() == 0");
                menuItemFindItem.setEnabled(false);
            } else {
                if (DEBUG) {
                    Log.d("browser/Controller", "url :" + url);
                }
                menuItemFindItem.setEnabled(true);
            }
        }
        Log.d("browser/Controller", "updateShareMenuItems end");
    }

    protected void addTab(Tab tab) {
        if (DEBUG) {
            Log.d("browser", "Controller.addTab()--->tab : " + tab);
        }
        this.mUi.addTab(tab);
    }

    @Override
    public void attachSubWindow(Tab tab) {
        if (tab.getSubWebView() != null) {
            this.mUi.attachSubWindow(tab.getSubViewContainer());
            getCurrentTopWebView().requestFocus();
        }
    }

    @Override
    public void bookmarkCurrentPage() {
        Intent intentCreateBookmarkCurrentPageIntent = createBookmarkCurrentPageIntent(false);
        if (intentCreateBookmarkCurrentPageIntent != null) {
            this.mActivity.startActivity(intentCreateBookmarkCurrentPageIntent);
        }
    }

    @Override
    public void bookmarkedStatusHasChanged(Tab tab) {
        this.mUi.bookmarkedStatusHasChanged(tab);
    }

    @Override
    public void bookmarksOrHistoryPicker(UI.ComboViews comboViews) {
        if (DEBUG) {
            Log.i("browser", "Controller.bookmarksOrHistoryPicker()--->startView = " + comboViews);
        }
        if (this.mTabControl.getCurrentWebView() == null) {
            return;
        }
        if (isInCustomActionMode()) {
            endActionMode();
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean("disable_new_window", !this.mTabControl.canCreateNewTab());
        this.mUi.showComboView(comboViews, bundle);
    }

    @Override
    public void closeCurrentTab() {
        closeCurrentTab(false);
    }

    protected boolean closeCurrentTab(boolean z) {
        if (DEBUG) {
            Log.i("browser", "Controller.closeCurrentTab()--->andQuit : " + z);
        }
        if (this.mTabControl.getTabCount() == 1) {
            this.mCrashRecoveryHandler.clearState();
            if (z) {
                this.mDelayRemoveLastTab = true;
            } else {
                this.mTabControl.removeTab(getCurrentTab());
            }
            this.mActivity.finish();
            return true;
        }
        Tab currentTab = this.mTabControl.getCurrentTab();
        int currentPosition = this.mTabControl.getCurrentPosition();
        Tab parent = currentTab.getParent();
        if (parent == null && (parent = this.mTabControl.getTab(currentPosition + 1)) == null) {
            parent = this.mTabControl.getTab(currentPosition - 1);
        }
        if (z) {
            this.mTabControl.setCurrentTab(parent);
            this.mUi.closeTableDelay(currentTab);
        } else if (switchToTab(parent)) {
            closeTab(currentTab);
        }
        return false;
    }

    protected void closeEmptyTab() {
        Tab currentTab = this.mTabControl.getCurrentTab();
        if (currentTab == null || currentTab.getWebView().copyBackForwardList().getSize() != 0) {
            return;
        }
        closeCurrentTab();
    }

    public void closeOtherTabs() {
        if (DEBUG) {
            Log.i("browser", "Controller.closeOtherTabs()--->");
        }
        ArrayList arrayList = new ArrayList();
        for (int tabCount = this.mTabControl.getTabCount() - 1; tabCount >= 0; tabCount--) {
            Tab tab = this.mTabControl.getTab(tabCount);
            if (tab != this.mTabControl.getCurrentTab()) {
                arrayList.add(Integer.valueOf(this.mTabControl.getTabPosition(tab)));
                removeTab(tab);
            }
        }
        new CheckMemoryTask(this.mHandler).execute(Integer.valueOf(getTabControl().getVisibleWebviewNums()), arrayList, false, null, null, true);
    }

    @Override
    public void closeTab(Tab tab) {
        if (DEBUG) {
            Log.i("browser", "Controller.closeTab()--->tab is " + tab);
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(Integer.valueOf(this.mTabControl.getTabPosition(tab)));
        if (tab == this.mTabControl.getCurrentTab()) {
            closeCurrentTab();
        } else {
            removeTab(tab);
        }
        new CheckMemoryTask(this.mHandler).execute(Integer.valueOf(getTabControl().getVisibleWebviewNums()), arrayList, false, null, null, true);
    }

    @Override
    public Intent createBookmarkCurrentPageIntent(boolean z) {
        return createBookmarkPageIntent(z, null, null);
    }

    public Intent createBookmarkLinkIntent(String str) {
        return createBookmarkPageIntent(false, str, "");
    }

    Bundle createSaveState() {
        Bundle bundle = new Bundle();
        this.mTabControl.saveState(bundle);
        if (!bundle.isEmpty()) {
            bundle.putSerializable("lastActiveDate", Calendar.getInstance());
        }
        return bundle;
    }

    @Override
    public void createSubWindow(Tab tab) {
        endActionMode();
        WebView webView = tab.getWebView();
        this.mUi.createSubWindow(tab, this.mFactory.createWebView(webView == null ? false : webView.isPrivateBrowsingEnabled()));
    }

    boolean didUserStopLoading() {
        return this.mLoadStopped;
    }

    @Override
    public void dismissSubWindow(Tab tab) {
        removeSubWindow(tab);
        tab.dismissSubWindow();
        WebView currentTopWebView = getCurrentTopWebView();
        if (currentTopWebView != null) {
            currentTopWebView.requestFocus();
        }
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        return this.mBlockEvents;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return this.mBlockEvents;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        return this.mBlockEvents;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        return this.mBlockEvents;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        return this.mBlockEvents;
    }

    void doStart(Bundle bundle, Intent intent) {
        Calendar calendar = bundle != null ? (Calendar) bundle.getSerializable("lastActiveDate") : null;
        Calendar calendar2 = Calendar.getInstance();
        Calendar calendar3 = Calendar.getInstance();
        calendar3.add(5, -1);
        boolean z = (calendar == null || calendar.before(calendar3) || calendar.after(calendar2)) ? false : true;
        long jCanRestoreState = this.mTabControl.canRestoreState(bundle, z);
        if (jCanRestoreState == -1) {
            CookieManager.getInstance().removeSessionCookies(null);
        }
        GoogleAccountLogin.startLoginIfNeeded(this.mActivity, new Runnable(this, bundle, intent, jCanRestoreState, z) {
            final Controller this$0;
            final long val$currentTabId;
            final Bundle val$icicle;
            final Intent val$intent;
            final boolean val$restoreIncognitoTabs;

            {
                this.this$0 = this;
                this.val$icicle = bundle;
                this.val$intent = intent;
                this.val$currentTabId = jCanRestoreState;
                this.val$restoreIncognitoTabs = z;
            }

            @Override
            public void run() {
                this.this$0.onPreloginFinished(this.val$icicle, this.val$intent, this.val$currentTabId, this.val$restoreIncognitoTabs);
            }
        });
    }

    @Override
    public void doUpdateVisitedHistory(Tab tab, boolean z) {
        if (DEBUG) {
            Log.i("browser", "Controller.doUpdateVisitedHistory()--->tab = " + tab + ", isReload = " + z);
        }
        if (tab.isPrivateBrowsingEnabled()) {
            return;
        }
        String originalUrl = tab.getOriginalUrl();
        if (TextUtils.isEmpty(originalUrl) || originalUrl.regionMatches(true, 0, "about:", 0, 6)) {
            return;
        }
        DataController.getInstance(this.mActivity).updateVisitedHistory(originalUrl);
        this.mCrashRecoveryHandler.backupState();
    }

    public void editUrl() {
        if (this.mOptionsMenuOpen) {
            this.mActivity.closeOptionsMenu();
        }
        this.mUi.editUrl(false, true);
    }

    @Override
    public void endActionMode() {
        if (this.mActionMode != null) {
            this.mActionMode.finish();
        }
    }

    @Override
    public void findOnPage() {
        getCurrentTopWebView().showFindDialog(null, true);
    }

    @Override
    public Activity getActivity() {
        return this.mActivity;
    }

    @Override
    public Context getContext() {
        return this.mActivity;
    }

    @Override
    public Tab getCurrentTab() {
        return this.mTabControl.getCurrentTab();
    }

    @Override
    public WebView getCurrentTopWebView() {
        return this.mTabControl.getCurrentTopWebView();
    }

    @Override
    public WebView getCurrentWebView() {
        return this.mTabControl.getCurrentWebView();
    }

    @Override
    public Bitmap getDefaultVideoPoster() {
        return this.mUi.getDefaultVideoPoster();
    }

    int getMaxTabs() {
        int integer = this.mActivity.getResources().getInteger(2131623938);
        String str = SystemProperties.get("ro.vendor.gmo.ram_optimize");
        return (str == null || !str.equals("1")) ? integer : integer / 2;
    }

    long getSavePageDirSize(File file) throws IOException {
        long savePageDirSize = 0;
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (int i = 0; i < fileArrListFiles.length; i++) {
                savePageDirSize += fileArrListFiles[i].isDirectory() ? getSavePageDirSize(fileArrListFiles[i]) : fileArrListFiles[i].length();
            }
        }
        return savePageDirSize;
    }

    @Override
    public BrowserSettings getSettings() {
        return this.mSettings;
    }

    @Override
    public TabControl getTabControl() {
        return this.mTabControl;
    }

    @Override
    public List<Tab> getTabs() {
        return this.mTabControl.getTabs();
    }

    @Override
    public UI getUi() {
        return this.mUi;
    }

    @Override
    public View getVideoLoadingProgressView() {
        return this.mUi.getVideoLoadingProgressView();
    }

    @Override
    public void getVisitedHistory(ValueCallback<String[]> valueCallback) {
        new AsyncTask<Void, Void, String[]>(this, valueCallback) {
            final Controller this$0;
            final ValueCallback val$callback;

            {
                this.this$0 = this;
                this.val$callback = valueCallback;
            }

            @Override
            public String[] doInBackground(Void... voidArr) {
                return com.android.browser.provider.Browser.getVisitedHistory(this.this$0.mActivity.getContentResolver());
            }

            @Override
            public void onPostExecute(String[] strArr) {
                this.val$callback.onReceiveValue(strArr);
            }
        }.execute(new Void[0]);
    }

    public WebViewFactory getWebViewFactory() {
        return this.mFactory;
    }

    void goBackOnePageOrQuit() {
        Tab currentTab = this.mTabControl.getCurrentTab();
        if (currentTab == null) {
            this.mActivity.moveTaskToBack(true);
            return;
        }
        if (currentTab.canGoBack()) {
            currentTab.goBack();
        } else {
            Tab parent = currentTab.getParent();
            if (parent != null) {
                switchToTab(parent);
                closeTab(currentTab);
            } else {
                if (currentTab.getAppId() != null || currentTab.closeOnBack()) {
                    closeCurrentTab(true);
                }
                boolean zMoveTaskToBack = this.mActivity.moveTaskToBack(true);
                Log.d("Controller", "moveTaskToBack: " + zMoveTaskToBack);
                if (zMoveTaskToBack) {
                    onPause();
                }
            }
        }
        if (DEBUG) {
            Log.i("browser", "Controller.goBackOnePageOrQuit()--->current tab is " + currentTab);
        }
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
    public void hideAutoLogin(Tab tab) {
        this.mUi.hideAutoLogin(tab);
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

    protected boolean isActivityPaused() {
        return this.mActivityPaused;
    }

    @Override
    public boolean isInCustomActionMode() {
        return this.mActionMode != null;
    }

    boolean isInLoad() {
        Tab currentTab = getCurrentTab();
        return currentTab != null && currentTab.inPageLoad();
    }

    public boolean isMenuDown() {
        return this.mMenuIsDown;
    }

    boolean isMenuOrCtrlKey(int i) {
        return 82 == i || 113 == i || 114 == i;
    }

    @Override
    public void loadUrl(Tab tab, String str) {
        loadUrl(tab, str, null);
    }

    protected void loadUrl(Tab tab, String str, Map<String, String> map) {
        if (DEBUG) {
            Log.d("browser", "Controller.loadUrl()--->tab : " + tab + ", url = " + str + ", headers : " + map);
        }
        if (tab != null) {
            dismissSubWindow(tab);
            tab.loadUrl(str, map);
            this.mUi.onProgressChanged(tab);
        }
    }

    protected void loadUrlDataIn(Tab tab, IntentHandler.UrlData urlData) {
        if (DEBUG) {
            Log.i("browser", "Controller.loadUrlDataIn()--->tab : " + tab + ", Url Data : " + urlData);
        }
        if (urlData == null || urlData.isPreloaded()) {
            return;
        }
        if (tab != null && urlData.mDisableUrlOverride) {
            tab.disableUrlOverridingForLoad();
        }
        loadUrl(tab, urlData.mUrl, urlData.mHeaders);
    }

    protected void loadUrlFromContext(String str) {
        if (DEBUG) {
            Log.i("browser", "Controller.loadUrlFromContext()--->url : " + str);
        }
        Tab currentTab = getCurrentTab();
        WebView webView = currentTab != null ? currentTab.getWebView() : null;
        if (str == null || str.length() == 0 || currentTab == null || webView == null) {
            return;
        }
        String strSmartUrlFilter = UrlUtils.smartUrlFilter(str);
        if (((BrowserWebView) webView).getWebViewClient().shouldOverrideUrlLoading(webView, strSmartUrlFilter)) {
            return;
        }
        loadUrl(currentTab, strSmartUrlFilter);
    }

    @Override
    public void onActionModeFinished(ActionMode actionMode) {
        if (isInCustomActionMode()) {
            this.mUi.onActionModeFinished(isInLoad());
            this.mActionMode = null;
        }
    }

    @Override
    public void onActionModeStarted(ActionMode actionMode) {
        this.mUi.onActionModeStarted(actionMode);
        this.mActionMode = actionMode;
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (getCurrentTopWebView() == null) {
            return;
        }
        switch (i) {
            case 1:
                if (intent != null && i2 == -1) {
                    this.mUi.showWeb(false);
                    if ("android.intent.action.VIEW".equals(intent.getAction())) {
                        loadUrl(getCurrentTab(), intent.getData().toString());
                    } else if (intent.hasExtra("open_all")) {
                        String[] stringArrayExtra = intent.getStringArrayExtra("open_all");
                        Tab currentTab = getCurrentTab();
                        for (String str : stringArrayExtra) {
                            currentTab = openTab(str, currentTab, !this.mSettings.openInBackground(), true);
                        }
                    } else if (intent.hasExtra("snapshot_id")) {
                        long longExtra = intent.getLongExtra("snapshot_id", -1L);
                        String stringExtra = intent.getStringExtra("snapshot_url");
                        if (stringExtra == null) {
                            stringExtra = this.mSettings.getHomePage();
                        }
                        if (longExtra >= 0) {
                            Tab currentTab2 = getCurrentTab();
                            currentTab2.mSavePageUrl = stringExtra;
                            currentTab2.mSavePageTitle = intent.getStringExtra("snapshot_title");
                            loadUrl(currentTab2, stringExtra);
                        }
                    }
                }
                break;
            case 3:
                if (i2 == -1 && intent != null && "privacy_clear_history".equals(intent.getStringExtra("android.intent.extra.TEXT"))) {
                    this.mTabControl.removeParentChildRelationShips();
                }
                break;
            case 4:
                if (this.mUploadHandler != null) {
                    this.mUploadHandler.onResult(i2, intent);
                }
                break;
            case 6:
                if (i2 == -1 && intent != null) {
                    ArrayList<String> stringArrayListExtra = intent.getStringArrayListExtra("android.speech.extra.RESULTS");
                    if (stringArrayListExtra.size() >= 1) {
                        this.mVoiceResult = stringArrayListExtra.get(0);
                    }
                }
                break;
        }
        getCurrentTopWebView().requestFocus();
        this.mBrowserMiscExt = Extensions.getMiscPlugin(this.mActivity);
        this.mBrowserMiscExt.onActivityResult(i, i2, intent, this.mActivity);
    }

    public void onBackKey() {
        if (this.mUi.onBackKey()) {
            return;
        }
        WebView currentSubWindow = this.mTabControl.getCurrentSubWindow();
        if (currentSubWindow == null) {
            goBackOnePageOrQuit();
        } else if (currentSubWindow.canGoBack()) {
            currentSubWindow.goBack();
        } else {
            dismissSubWindow(this.mTabControl.getCurrentTab());
        }
    }

    @Override
    public void onConfgurationChanged(Configuration configuration) {
        this.mConfigChanged = true;
        this.mActivity.invalidateOptionsMenu();
        if (this.mPageDialogsHandler != null) {
            this.mPageDialogsHandler.onConfigurationChanged(configuration);
        }
        this.mUi.onConfigurationChanged(configuration);
        this.mSettings.onConfigurationChanged(configuration);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        if (menuItem.getGroupId() == 2131558640) {
            return false;
        }
        int itemId = menuItem.getItemId();
        if (itemId != 2131558433) {
            switch (itemId) {
                case 2131558625:
                case 2131558626:
                case 2131558627:
                    break;
                default:
                    return onOptionsItemSelected(menuItem);
            }
        }
        WebView currentTopWebView = getCurrentTopWebView();
        if (currentTopWebView == null) {
            return false;
        }
        HashMap map = new HashMap();
        map.put("webview", currentTopWebView);
        currentTopWebView.requestFocusNodeHref(this.mHandler.obtainMessage(102, itemId, 0, map));
        return true;
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        this.mUi.onContextMenuClosed(menu, isInLoad());
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        WebView webView;
        WebView.HitTestResult hitTestResult;
        if ((view instanceof TitleBar) || !(view instanceof WebView) || (hitTestResult = (webView = (WebView) view).getHitTestResult()) == null) {
            return;
        }
        int type = hitTestResult.getType();
        if (DEBUG) {
            Log.d("browser/Controller", "onCreateContextMenu type is : " + type);
        }
        if (type == 0) {
            Log.w("Controller", "We should not show context menu when nothing is touched");
            return;
        }
        if (type != 9) {
            this.mActivity.getMenuInflater().inflate(2131755011, contextMenu);
            String extra = hitTestResult.getExtra();
            String siteNavHitURL = view instanceof BrowserWebView ? ((BrowserWebView) webView).getSiteNavHitURL() : null;
            if (DEBUG) {
                Log.d("browser/Controller", "sitenavigation onCreateContextMenu imageAnchorUrlExtra is : " + siteNavHitURL);
            }
            TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService("phone");
            boolean zIsVoiceCapable = telephonyManager != null ? telephonyManager.isVoiceCapable() : false;
            contextMenu.setGroupVisible(2131558634, false);
            contextMenu.setGroupVisible(2131558637, false);
            if (zIsVoiceCapable) {
                contextMenu.setGroupVisible(2131558610, type == 2);
                contextMenu.setGroupVisible(2131558614, false);
            } else {
                contextMenu.setGroupVisible(2131558610, false);
                contextMenu.setGroupVisible(2131558614, type == 2);
            }
            contextMenu.setGroupVisible(2131558617, type == 4);
            contextMenu.setGroupVisible(2131558620, type == 3);
            contextMenu.setGroupVisible(2131558628, type == 5 || type == 8);
            contextMenu.setGroupVisible(2131558623, type == 7 || type == 8);
            contextMenu.setGroupVisible(2131558632, false);
            switch (type) {
                case 2:
                    if (Uri.decode(extra).length() <= 128) {
                        contextMenu.setHeaderTitle(Uri.decode(extra));
                    } else {
                        contextMenu.setHeaderTitle(Uri.decode(extra).substring(0, 128));
                    }
                    contextMenu.findItem(2131558611).setIntent(new Intent("android.intent.action.VIEW", Uri.parse("tel:" + extra)));
                    Intent intent = new Intent("android.intent.action.INSERT_OR_EDIT");
                    intent.putExtra("phone", Uri.decode(extra));
                    intent.setType("vnd.android.cursor.item/contact");
                    if (!zIsVoiceCapable) {
                        contextMenu.findItem(2131558615).setIntent(intent);
                        contextMenu.findItem(2131558616).setOnMenuItemClickListener(new Copy(this, extra));
                    } else {
                        contextMenu.findItem(2131558612).setIntent(intent);
                        contextMenu.findItem(2131558613).setOnMenuItemClickListener(new Copy(this, extra));
                    }
                    break;
                case 3:
                    if (extra.length() <= 128) {
                        contextMenu.setHeaderTitle(extra);
                    } else {
                        contextMenu.setHeaderTitle(extra.substring(0, 128));
                    }
                    contextMenu.findItem(2131558621).setIntent(new Intent("android.intent.action.VIEW", Uri.parse("geo:0,0?q=" + URLEncoder.encode(extra))));
                    contextMenu.findItem(2131558622).setOnMenuItemClickListener(new Copy(this, extra));
                    break;
                case 4:
                    if (extra.length() <= 128) {
                        contextMenu.setHeaderTitle(extra);
                    } else {
                        contextMenu.setHeaderTitle(extra.substring(0, 128));
                    }
                    contextMenu.findItem(2131558618).setIntent(new Intent("android.intent.action.VIEW", Uri.parse("mailto:" + extra)));
                    contextMenu.findItem(2131558619).setOnMenuItemClickListener(new Copy(this, extra));
                    break;
                case 5:
                    MenuItem menuItemFindItem = contextMenu.findItem(2131558565);
                    menuItemFindItem.setVisible(type == 5);
                    if (type == 5) {
                        if (extra.length() <= 128) {
                            contextMenu.setHeaderTitle(extra);
                        } else {
                            contextMenu.setHeaderTitle(extra.substring(0, 128));
                        }
                        menuItemFindItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(this, extra) {
                            final Controller this$0;
                            final String val$extra;

                            {
                                this.this$0 = this;
                                this.val$extra = extra;
                            }

                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                Controller.sharePage(this.this$0.mActivity, null, this.val$extra, null, null);
                                return true;
                            }
                        });
                    }
                    contextMenu.findItem(2131558630).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(this, extra) {
                        final Controller this$0;
                        final String val$extra;

                        {
                            this.this$0 = this;
                            this.val$extra = extra;
                        }

                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            if (Controller.isImageViewableUri(Uri.parse(this.val$extra))) {
                                this.this$0.openTab(this.val$extra, this.this$0.mTabControl.getCurrentTab(), true, true);
                                return false;
                            }
                            if (!Controller.DEBUG) {
                                return false;
                            }
                            Log.e("Controller", "Refusing to view image with invalid URI, \"" + this.val$extra + "\"");
                            return false;
                        }
                    });
                    contextMenu.findItem(2131558629).setOnMenuItemClickListener(new Download(this.mActivity, extra, webView.isPrivateBrowsingEnabled(), webView.getSettings().getUserAgentString()));
                    this.mWallpaperHandler = new WallpaperHandler(this.mActivity, extra);
                    contextMenu.findItem(2131558631).setOnMenuItemClickListener(this.mWallpaperHandler);
                    break;
                case 6:
                default:
                    Log.w("Controller", "We should not get here.");
                    break;
                case 7:
                case 8:
                    if (extra != null && extra.startsWith("rtsp://")) {
                        contextMenu.findItem(2131558625).setVisible(false);
                    }
                    if (extra.length() <= 128) {
                        contextMenu.setHeaderTitle(extra);
                    } else {
                        contextMenu.setHeaderTitle(extra.substring(0, 128));
                    }
                    boolean zCanCreateNewTab = this.mTabControl.canCreateNewTab();
                    MenuItem menuItemFindItem2 = contextMenu.findItem(2131558624);
                    menuItemFindItem2.setTitle(getSettings().openInBackground() ? 2131493039 : 2131493038);
                    menuItemFindItem2.setVisible(zCanCreateNewTab);
                    if (zCanCreateNewTab) {
                        if (8 == type) {
                            menuItemFindItem2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(this, webView) {
                                final Controller this$0;
                                final WebView val$webview;

                                {
                                    this.this$0 = this;
                                    this.val$webview = webView;
                                }

                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    HashMap map = new HashMap();
                                    map.put("webview", this.val$webview);
                                    this.val$webview.requestFocusNodeHref(this.this$0.mHandler.obtainMessage(102, 2131558624, 0, map));
                                    return true;
                                }
                            });
                        } else {
                            menuItemFindItem2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(this, extra) {
                                final Controller this$0;
                                final String val$extra;

                                {
                                    this.this$0 = this;
                                    this.val$extra = extra;
                                }

                                @Override
                                public boolean onMenuItemClick(MenuItem menuItem) {
                                    if (this.val$extra != null && this.val$extra.startsWith("rtsp://")) {
                                        Intent intent2 = new Intent();
                                        intent2.setAction("android.intent.action.VIEW");
                                        intent2.setData(Uri.parse(this.val$extra.replaceAll(" ", "%20")));
                                        intent2.addFlags(268435456);
                                        this.this$0.mActivity.startActivity(intent2);
                                    } else if (this.val$extra == null || !this.val$extra.startsWith("wtai://wp/mc;")) {
                                        this.this$0.openTab(this.val$extra, this.this$0.mTabControl.getCurrentTab(), !this.this$0.mSettings.openInBackground(), true);
                                    } else {
                                        this.this$0.mActivity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("tel:" + this.val$extra.replaceAll(" ", "%20").substring("wtai://wp/mc;".length()))));
                                    }
                                    return true;
                                }
                            });
                        }
                    }
                    if (type != 7) {
                    }
                    break;
            }
            this.mUi.onContextMenuCreated(contextMenu);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.mMenuState == -1) {
            return false;
        }
        this.mActivity.getMenuInflater().inflate(2131755010, menu);
        return true;
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
        Tab currentTab = this.mTabControl.getCurrentTab();
        if (currentTab != null) {
            dismissSubWindow(currentTab);
            removeTab(currentTab);
        }
        this.mActivity.getContentResolver().unregisterContentObserver(this.mBookmarksObserver);
        this.mActivity.getContentResolver().unregisterContentObserver(this.mSiteNavigationObserver);
        this.mTabControl.destroy();
        WebIconDatabase.getInstance().close();
        this.mSystemAllowGeolocationOrigins.stop();
        this.mSystemAllowGeolocationOrigins = null;
    }

    @Override
    public void onDownloadStart(Tab tab, String str, String str2, String str3, String str4, String str5, long j) {
        String str6;
        WebView webView;
        WebView webView2 = tab.getWebView();
        StringBuilder sb = new StringBuilder();
        sb.append("onDownloadStart: dispos=");
        sb.append(str3 == null ? "null" : str3);
        Log.d("browser/Controller", sb.toString());
        if (str3 == null || !str3.regionMatches(true, 0, "attachment", 0, 10)) {
            Intent intent = new Intent("android.intent.action.VIEW");
            String str7 = str.startsWith("http://vod02.v.vnet.mobi/mobi/vod/st02") ? "video/3gp" : str4;
            intent.setDataAndType(Uri.parse(str), str7);
            intent.addFlags(268435456);
            ResolveInfo resolveInfoResolveActivity = this.mActivity.getPackageManager().resolveActivity(intent, 65536);
            StringBuilder sb2 = new StringBuilder();
            sb2.append("onDownloadStart: ResolveInfo=");
            sb2.append(resolveInfoResolveActivity == null ? "null" : resolveInfoResolveActivity);
            Log.d("browser/Controller", sb2.toString());
            if (resolveInfoResolveActivity != null) {
                ComponentName componentName = this.mActivity.getComponentName();
                Log.d("browser/Controller", "onDownloadStart: myName=" + componentName + ", myName.packageName=" + componentName.getPackageName() + ", info.packageName=" + resolveInfoResolveActivity.activityInfo.packageName + ", myName.name=" + componentName.getClassName() + ", info.name=" + resolveInfoResolveActivity.activityInfo.name);
                if (componentName.getPackageName().equals(resolveInfoResolveActivity.activityInfo.packageName) && componentName.getClassName().equals(resolveInfoResolveActivity.activityInfo.name)) {
                    str6 = str7;
                    webView = webView2;
                } else {
                    Log.d("browser/Controller", "onDownloadStart: mimetype=" + str7);
                    if (str7.equalsIgnoreCase("application/x-mpegurl") || str7.equalsIgnoreCase("application/vnd.apple.mpegurl")) {
                        this.mActivity.startActivity(intent);
                        if (webView2 == null || webView2.copyBackForwardList().getSize() != 0) {
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
                        Activity activity = this.mActivity;
                        TabControl tabControl = this.mTabControl;
                        try {
                            new AlertDialog.Builder(activity).setTitle(2131492946).setIcon(android.R.drawable.ic_dialog_info).setMessage(2131492892).setPositiveButton(2131492893, new DialogInterface.OnClickListener(this, activity, str, str2, str3, str7, j, tab, tabControl) {
                                final Controller this$0;
                                final Activity val$activity;
                                final String val$downloadContentDisposition;
                                final long val$downloadContentLength;
                                final String val$downloadMimetype;
                                final Tab val$downloadTab;
                                final TabControl val$downloadTabControl;
                                final String val$downloadUrl;
                                final String val$downloadUserAgent;

                                {
                                    this.this$0 = this;
                                    this.val$activity = activity;
                                    this.val$downloadUrl = str;
                                    this.val$downloadUserAgent = str2;
                                    this.val$downloadContentDisposition = str3;
                                    this.val$downloadMimetype = str7;
                                    this.val$downloadContentLength = j;
                                    this.val$downloadTab = tab;
                                    this.val$downloadTabControl = tabControl;
                                }

                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    DownloadHandler.onDownloadStartNoStream(this.val$activity, this.val$downloadUrl, this.val$downloadUserAgent, this.val$downloadContentDisposition, this.val$downloadMimetype, null, false, this.val$downloadContentLength);
                                    Log.d("browser/Controller", "User decide to download the content");
                                    WebView webView3 = this.val$downloadTab.getWebView();
                                    if (webView3 == null || webView3.copyBackForwardList().getSize() != 0) {
                                        return;
                                    }
                                    if (this.val$downloadTab == this.val$downloadTabControl.getCurrentTab()) {
                                        this.this$0.goBackOnePageOrQuit();
                                    } else {
                                        this.this$0.closeTab(this.val$downloadTab);
                                    }
                                }
                            }).setNegativeButton(2131492894, new DialogInterface.OnClickListener(this, str, intent, activity, tab, tabControl) {
                                final Controller this$0;
                                final Activity val$activity;
                                final Intent val$downloadIntent;
                                final Tab val$downloadTab;
                                final TabControl val$downloadTabControl;
                                final String val$downloadUrl;

                                {
                                    this.this$0 = this;
                                    this.val$downloadUrl = str;
                                    this.val$downloadIntent = intent;
                                    this.val$activity = activity;
                                    this.val$downloadTab = tab;
                                    this.val$downloadTabControl = tabControl;
                                }

                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (this.val$downloadUrl != null) {
                                        String cookie = CookieManager.getInstance().getCookie(this.val$downloadUrl);
                                        if (Controller.DEBUG) {
                                            Log.i("browser/Controller", "url: " + this.val$downloadUrl + " url cookie: " + cookie);
                                        }
                                        if (cookie != null) {
                                            this.val$downloadIntent.putExtra("url-cookie", cookie);
                                        }
                                    }
                                    this.val$activity.startActivity(this.val$downloadIntent);
                                    Log.d("browser/Controller", "User decide to open the content by startActivity");
                                    WebView webView3 = this.val$downloadTab.getWebView();
                                    if (webView3 == null || webView3.copyBackForwardList().getSize() != 0) {
                                        return;
                                    }
                                    if (this.val$downloadTab == this.val$downloadTabControl.getCurrentTab()) {
                                        this.this$0.goBackOnePageOrQuit();
                                    } else {
                                        this.this$0.closeTab(this.val$downloadTab);
                                    }
                                }
                            }).setOnCancelListener(new DialogInterface.OnCancelListener(this) {
                                final Controller this$0;

                                {
                                    this.this$0 = this;
                                }

                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    Log.d("browser/Controller", "User cancel the download action");
                                }
                            }).show();
                            return;
                        } catch (ActivityNotFoundException e) {
                            e = e;
                            Log.d("Controller", "activity not found for " + str7 + " over " + Uri.parse(str).getScheme(), e);
                            str6 = str7;
                            webView = webView2;
                            if (DEBUG) {
                            }
                            downloadStart(this.mActivity, str, str2, str3, str6, str5, false, j, webView, tab);
                        }
                    } catch (ActivityNotFoundException e2) {
                        e = e2;
                    }
                }
            }
        } else {
            str6 = str4;
            webView = webView2;
        }
        if (DEBUG) {
            Log.d("browser/Controller", "onDownloadStart: download directly, mimetype=" + str6 + ", url=" + str);
        }
        downloadStart(this.mActivity, str, str2, str3, str6, str5, false, j, webView, tab);
    }

    @Override
    public void onFavicon(Tab tab, WebView webView, Bitmap bitmap) {
        this.mUi.onTabDataChanged(tab);
        maybeUpdateFavicon(tab, webView.getOriginalUrl(), webView.getUrl(), bitmap);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        boolean zHasNoModifiers = keyEvent.hasNoModifiers();
        if (!zHasNoModifiers && isMenuOrCtrlKey(i)) {
            this.mMenuIsDown = true;
            return false;
        }
        WebView currentTopWebView = getCurrentTopWebView();
        Tab currentTab = getCurrentTab();
        if (currentTopWebView == null || currentTab == null) {
            return false;
        }
        boolean zHasModifiers = keyEvent.hasModifiers(4096);
        boolean zHasModifiers2 = keyEvent.hasModifiers(1);
        switch (i) {
            case 4:
                if (zHasNoModifiers) {
                    keyEvent.startTracking();
                    return true;
                }
                break;
            case 21:
                if (zHasModifiers) {
                    currentTab.goBack();
                    return true;
                }
                break;
            case 22:
                if (zHasModifiers) {
                    currentTab.goForward();
                    return true;
                }
                break;
            case 48:
                if (keyEvent.isCtrlPressed()) {
                    if (keyEvent.isShiftPressed()) {
                        openIncognitoTab();
                    } else {
                        openTab("about:blank", false, true, false);
                    }
                    return true;
                }
                break;
            case 61:
                if (keyEvent.isCtrlPressed()) {
                    if (keyEvent.isShiftPressed()) {
                        switchToTab(getPrevTab());
                    } else {
                        switchToTab(getNextTab());
                    }
                    return true;
                }
                break;
            case 62:
                if (zHasModifiers2) {
                    pageUp();
                } else if (zHasNoModifiers) {
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
                if (zHasNoModifiers) {
                    currentTab.goForward();
                    return true;
                }
                break;
        }
        return this.mUi.dispatchKey(i, keyEvent);
    }

    @Override
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        if (i != 4 || !this.mUi.isWebShowing()) {
            return false;
        }
        bookmarksOrHistoryPicker(UI.ComboViews.History);
        return true;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (isMenuOrCtrlKey(i)) {
            this.mMenuIsDown = false;
            if (82 == i && keyEvent.isTracking() && !keyEvent.isCanceled()) {
                return onMenuKey();
            }
        }
        if (!keyEvent.hasNoModifiers() || i != 4 || !keyEvent.isTracking() || keyEvent.isCanceled()) {
            return false;
        }
        onBackKey();
        return true;
    }

    @Override
    public void onLowMemory() {
        this.mTabControl.freeMemory();
    }

    protected boolean onMenuKey() {
        return this.mUi.onMenuKey();
    }

    @Override
    public boolean onMenuOpened(int i, Menu menu) {
        if (!this.mOptionsMenuOpen) {
            this.mOptionsMenuOpen = true;
            this.mConfigChanged = false;
            this.mExtendedMenuOpen = false;
            this.mUi.onOptionsMenuOpened();
        } else if (this.mConfigChanged) {
            this.mConfigChanged = false;
        } else if (this.mExtendedMenuOpen) {
            this.mExtendedMenuOpen = false;
            this.mUi.onExtendedMenuClosed(isInLoad());
        } else {
            this.mExtendedMenuOpen = true;
            this.mUi.onExtendedMenuOpened();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int i = 0;
        if (getCurrentTopWebView() == null) {
            return false;
        }
        if (this.mMenuIsDown) {
            this.mMenuIsDown = false;
        }
        if (this.mUi.onOptionsItemSelected(menuItem)) {
            return true;
        }
        switch (menuItem.getItemId()) {
            case 2131558572:
                if (getCurrentTopWebView() != null) {
                    getCurrentTopWebView().reload();
                }
                break;
            case 2131558573:
                getCurrentTab().goForward();
                break;
            case 2131558574:
                stopLoading();
                break;
            case 2131558575:
            case 2131558596:
                loadUrl(this.mTabControl.getCurrentTab(), this.mSettings.getHomePage());
                break;
            case 2131558576:
                bookmarkCurrentPage();
                break;
            case 2131558577:
                showCloseSelectionDialog();
                break;
            case 2131558578:
            case 2131558586:
            case 2131558588:
            case 2131558593:
            case 2131558594:
            default:
                return false;
            case 2131558579:
                Tab currentTab = this.mTabControl.getCurrentTab();
                if (currentTab == null) {
                    return false;
                }
                shareCurrentPage(currentTab);
                break;
                break;
            case 2131558580:
                findOnPage();
                break;
            case 2131558581:
                toggleUserAgent();
                break;
            case 2131558582:
                bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
                break;
            case 2131558583:
                openTab("about:blank", false, true, false);
                break;
            case 2131558584:
                showPageInfo();
                break;
            case 2131558585:
                openPreferences();
                break;
            case 2131558587:
                goLive();
                return true;
            case 2131558589:
                closeOtherTabs();
                break;
            case 2131558590:
                bookmarksOrHistoryPicker(UI.ComboViews.History);
                break;
            case 2131558591:
                bookmarksOrHistoryPicker(UI.ComboViews.Snapshots);
                break;
            case 2131558592:
                getCurrentTopWebView().debugDump();
                break;
            case 2131558595:
                viewDownloads();
                break;
            case 2131558597:
                getCurrentTopWebView().zoomIn();
                break;
            case 2131558598:
                getCurrentTopWebView().zoomOut();
                break;
            case 2131558599:
            case 2131558600:
            case 2131558601:
            case 2131558602:
            case 2131558603:
            case 2131558604:
            case 2131558605:
            case 2131558606:
                int itemId = menuItem.getItemId();
                while (true) {
                    if (i < WINDOW_SHORTCUT_ID_ARRAY.length) {
                        if (WINDOW_SHORTCUT_ID_ARRAY[i] == itemId) {
                            Tab tab = this.mTabControl.getTab(i);
                            if (tab != null && tab != this.mTabControl.getCurrentTab()) {
                                switchToTab(tab);
                            }
                        } else {
                            i++;
                        }
                    }
                    break;
                }
                break;
            case 2131558607:
                getCurrentTab().goBack();
                break;
            case 2131558608:
                editUrl();
                break;
            case 2131558609:
                if (this.mTabControl.getCurrentSubWindow() == null) {
                    closeCurrentTab();
                } else {
                    dismissSubWindow(this.mTabControl.getCurrentTab());
                }
                break;
        }
        return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        this.mOptionsMenuOpen = false;
        this.mUi.onOptionsMenuClosed(isInLoad());
    }

    @Override
    public void onPageFinished(Tab tab) {
        if (!this.mDelayRemoveLastTab) {
            if (DEBUG) {
                Log.i("Controller", "onPageFinished backupState " + tab.getUrl());
            }
            this.mCrashRecoveryHandler.backupState();
        }
        this.mUi.onTabDataChanged(tab);
        if (this.mActivityPaused && pauseWebViewTimers(tab)) {
            releaseWakeLock();
        }
        if (tab.getWebView() != null && tab.inForeground()) {
            this.mUi.updateBottomBarState(tab.getWebView().canScrollVertically(-1) || tab.getWebView().canScrollVertically(1), tab.canGoBack() || tab.getParent() != null, tab.canGoForward());
        }
        Performance.tracePageFinished();
        Performance.dumpSystemMemInfo(this.mActivity);
        ArrayList arrayList = new ArrayList();
        arrayList.add(Integer.valueOf(getTabControl().getTabPosition(tab)));
        new CheckMemoryTask(this.mHandler).execute(Integer.valueOf(getTabControl().getVisibleWebviewNums()), arrayList, true, tab.getUrl(), null, false);
    }

    @Override
    public void onPageStarted(Tab tab, WebView webView, Bitmap bitmap) {
        this.mHandler.removeMessages(108, tab);
        this.mBrowserMiscExt = Extensions.getMiscPlugin(this.mActivity);
        this.mBrowserMiscExt.processNetworkNotify(webView, this.mActivity, this.mNetworkHandler.isNetworkUp());
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
        maybeUpdateFavicon(tab, null, url, bitmap);
        Performance.tracePageStart(url);
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
        Tab currentTab = this.mTabControl.getCurrentTab();
        if (currentTab != null) {
            currentTab.pause();
            if (!this.mDelayRemoveLastTab && !pauseWebViewTimers(currentTab)) {
                if (this.mWakeLock == null) {
                    this.mWakeLock = ((PowerManager) this.mActivity.getSystemService("power")).newWakeLock(1, "Browser");
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (getCurrentTab() != null) {
            updateShareMenuItems(menu, getCurrentTab());
        }
        this.mCachedMenu = menu;
        if (this.mMenuState != -1) {
            if (this.mCurrentMenuState != this.mMenuState) {
                menu.setGroupVisible(2131558570, true);
                menu.setGroupEnabled(2131558570, true);
                menu.setGroupEnabled(2131558594, true);
            }
            updateMenuState(getCurrentTab(), menu);
        } else if (this.mCurrentMenuState != this.mMenuState) {
            menu.setGroupVisible(2131558570, false);
            menu.setGroupEnabled(2131558570, false);
            menu.setGroupEnabled(2131558594, false);
        }
        this.mCurrentMenuState = this.mMenuState;
        return this.mUi.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onProgressChanged(Tab tab) {
        int loadProgress = tab.getLoadProgress();
        if (DEBUG) {
            Log.i("Controller", "Network_Issue onProgressChanged url: " + tab.getUrl() + " : " + loadProgress + "%");
        }
        if (loadProgress == 100) {
            if (!tab.isPrivateBrowsingEnabled() && !TextUtils.isEmpty(tab.getUrl()) && !tab.isSnapshot() && tab.shouldUpdateThumbnail() && (((tab.inForeground() && !didUserStopLoading()) || !tab.inForeground()) && !this.mHandler.hasMessages(108, tab))) {
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(108, 0, 0, tab), 500L);
            }
            if (tab.getWebView() != null && tab.inForeground()) {
                this.mUi.updateBottomBarState(tab.getWebView().canScrollVertically(-1) || tab.getWebView().canScrollVertically(1), tab.canGoBack() || tab.getParent() != null, tab.canGoForward());
            }
        }
        this.mUi.onProgressChanged(tab);
    }

    @Override
    public void onReceivedHttpAuthRequest(Tab tab, WebView webView, HttpAuthHandler httpAuthHandler, String str, String str2) {
        String str3;
        String str4;
        String[] httpAuthUsernamePassword;
        if (!httpAuthHandler.useHttpAuthUsernamePassword() || webView == null || (httpAuthUsernamePassword = webView.getHttpAuthUsernamePassword(str, str2)) == null || httpAuthUsernamePassword.length != 2) {
            str3 = null;
            str4 = null;
        } else {
            str3 = httpAuthUsernamePassword[0];
            str4 = httpAuthUsernamePassword[1];
        }
        if (str3 != null && str4 != null) {
            httpAuthHandler.proceed(str3, str4);
        } else if (tab.inForeground()) {
            this.mPageDialogsHandler.showHttpAuthentication(tab, httpAuthHandler, str, str2);
        } else {
            httpAuthHandler.cancel();
        }
    }

    @Override
    public void onReceivedTitle(Tab tab, String str) {
        this.mUi.onTabDataChanged(tab);
        String originalUrl = tab.getOriginalUrl();
        if (TextUtils.isEmpty(originalUrl) || originalUrl.length() >= 50000 || tab.isPrivateBrowsingEnabled()) {
            return;
        }
        DataController.getInstance(this.mActivity).updateHistoryTitle(originalUrl, str);
    }

    @Override
    public void onResume() {
        if (!this.mActivityPaused) {
            Log.e("Controller", "BrowserActivity is already resumed.");
            return;
        }
        this.mSettings.setLastRunPaused(false);
        this.mActivityPaused = false;
        Tab currentTab = this.mTabControl.getCurrentTab();
        if (currentTab != null) {
            currentTab.resume();
            resumeWebViewTimers(currentTab);
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

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        this.mCrashRecoveryHandler.writeState(createSaveState());
        this.mSettings.setLastRunPaused(true);
    }

    @Override
    public boolean onSearchRequested() {
        this.mUi.editUrl(false, true);
        return true;
    }

    @Override
    public void onSetWebView(Tab tab, WebView webView) {
        this.mUi.onSetWebView(tab, webView);
    }

    @Override
    public void onShowPopupWindowAttempt(Tab tab, boolean z, Message message) {
        this.mPageDialogsHandler.showPopupWindowAttempt(tab, z, message);
    }

    @Override
    public boolean onUnhandledKeyEvent(KeyEvent keyEvent) {
        if (isActivityPaused()) {
            return false;
        }
        return keyEvent.getAction() == 0 ? this.mActivity.onKeyDown(keyEvent.getKeyCode(), keyEvent) : this.mActivity.onKeyUp(keyEvent.getKeyCode(), keyEvent);
    }

    @Override
    public void onUpdatedSecurityState(Tab tab) {
        this.mUi.onTabDataChanged(tab);
    }

    @Override
    public void onUserCanceledSsl(Tab tab) {
        if (tab.canGoBack()) {
            tab.goBack();
        } else {
            tab.loadUrl(this.mSettings.getHomePage(), null);
        }
    }

    @Override
    public Tab openIncognitoTab() {
        return openTab("browser:incognito", true, true, false);
    }

    @Override
    public void openPreferences() {
        Intent intent = new Intent(this.mActivity, (Class<?>) BrowserPreferencesPage.class);
        intent.putExtra("currentPage", getCurrentTopWebView().getUrl());
        this.mActivity.startActivityForResult(intent, 3);
    }

    public Tab openTab(IntentHandler.UrlData urlData) {
        Tab tabShowPreloadedTab = showPreloadedTab(urlData);
        if (tabShowPreloadedTab == null && (tabShowPreloadedTab = createNewTab(false, true, true)) != null && !urlData.isEmpty()) {
            loadUrlDataIn(tabShowPreloadedTab, urlData);
        }
        return tabShowPreloadedTab;
    }

    @Override
    public Tab openTab(String str, Tab tab, boolean z, boolean z2) {
        return openTab(str, tab != null && tab.isPrivateBrowsingEnabled(), z, z2, tab);
    }

    @Override
    public Tab openTab(String str, boolean z, boolean z2, boolean z3) {
        return openTab(str, z, z2, z3, null);
    }

    public Tab openTab(String str, boolean z, boolean z2, boolean z3, Tab tab) {
        if (DEBUG) {
            Log.d("browser", "Controller.openTab()--->url = " + str + ", incognito = " + z + ", setActive = " + z2 + ", useCurrent = " + z3 + ", tab parent is " + tab);
        }
        Tab tabCreateNewTab = createNewTab(z, z2, z3);
        if (tabCreateNewTab != null) {
            if (tab != null && tab != tabCreateNewTab) {
                tab.addChildTab(tabCreateNewTab);
            }
            if (str != null) {
                loadUrl(tabCreateNewTab, str);
            }
        }
        return tabCreateNewTab;
    }

    @Override
    public Tab openTabToHomePage() {
        if (DEBUG) {
            Log.d("browser", "Controller.openTabToHomePage()--->");
        }
        return openTab(this.mSettings.getHomePage(), false, true, false);
    }

    protected void pageDown() {
        getCurrentTopWebView().pageDown(false);
    }

    protected void pageUp() {
        getCurrentTopWebView().pageUp(false);
    }

    @Override
    public void removeSubWindow(Tab tab) {
        if (tab.getSubWebView() != null) {
            WebView webView = tab.getWebView();
            if (webView != null) {
                webView.requestFocus();
            }
            this.mUi.removeSubWindow(tab.getSubViewContainer());
        }
    }

    protected void removeTab(Tab tab) {
        this.mUi.removeTab(tab);
        this.mTabControl.removeTab(tab);
        this.mCrashRecoveryHandler.backupState();
    }

    protected void reuseTab(Tab tab, IntentHandler.UrlData urlData) {
        if (DEBUG) {
            Log.i("browser", "Controller.reuseTab()--->tab : " + tab + ", urlData : " + urlData);
        }
        dismissSubWindow(tab);
        this.mUi.detachTab(tab);
        this.mTabControl.recreateWebView(tab);
        this.mUi.attachTab(tab);
        if (this.mTabControl.getCurrentTab() != tab) {
            switchToTab(tab);
            loadUrlDataIn(tab, urlData);
        } else {
            setActiveTab(tab);
            loadUrlDataIn(tab, urlData);
        }
    }

    @Override
    public void sendErrorCode(int i, String str) {
        Intent intent = new Intent("com.android.browser.action.SEND_ERROR");
        intent.putExtra("com.android.browser.error_code_key", i);
        intent.putExtra("com.android.browser.url_key", str);
        intent.putExtra("com.android.browser.homepage_key", this.mSettings.getHomePage());
        this.mActivity.sendBroadcast(intent);
    }

    @Override
    public void setActiveTab(Tab tab) {
        if (tab != null) {
            this.mTabControl.setCurrentTab(tab);
            this.mUi.setActiveTab(tab);
            WebView webView = tab.getWebView();
            if (DEBUG) {
                Log.d("browser", "Controller.setActiveTab()---> webview : " + webView);
            }
            if (webView == null) {
                return;
            }
            if (this.mSettings.isDesktopUserAgent(webView)) {
                this.mSettings.changeUserAgent(webView, false);
            }
        }
        if (DEBUG) {
            Log.d("browser", "Controller.setActiveTab()--->tab : " + tab);
        }
    }

    @Override
    public void setBlockEvents(boolean z) {
        this.mBlockEvents = z;
    }

    protected void setShouldShowErrorConsole(boolean z) {
        if (z == this.mShouldShowErrorConsole) {
            return;
        }
        this.mShouldShowErrorConsole = z;
        Tab currentTab = this.mTabControl.getCurrentTab();
        if (currentTab != null) {
            this.mUi.setShouldShowErrorConsole(currentTab, z);
        }
    }

    void setUi(UI ui) {
        this.mUi = ui;
    }

    @Override
    public void shareCurrentPage() {
        shareCurrentPage(this.mTabControl.getCurrentTab());
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return this.mUi.shouldCaptureThumbnails();
    }

    @Override
    public boolean shouldOverrideKeyEvent(KeyEvent keyEvent) {
        if (this.mMenuIsDown) {
            return this.mActivity.getWindow().isShortcutKey(keyEvent.getKeyCode(), keyEvent);
        }
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(Tab tab, WebView webView, String str) {
        boolean zShouldOverrideUrlLoading = this.mUrlHandler.shouldOverrideUrlLoading(tab, webView, str);
        if (tab.inForeground()) {
            this.mUi.updateBottomBarState(true, tab.canGoBack(), tab.canGoForward());
        }
        return zShouldOverrideUrlLoading;
    }

    @Override
    public boolean shouldShowErrorConsole() {
        return this.mShouldShowErrorConsole;
    }

    @Override
    public void showAutoLogin(Tab tab) {
        this.mUi.showAutoLogin(tab);
    }

    @Override
    public void showCustomView(Tab tab, View view, int i, WebChromeClient.CustomViewCallback customViewCallback) {
        if (tab.inForeground()) {
            if (this.mUi.isCustomViewShowing()) {
                customViewCallback.onCustomViewHidden();
                return;
            }
            this.mUi.showCustomView(view, i, customViewCallback);
            this.mOldMenuState = this.mMenuState;
            this.mMenuState = -1;
            this.mActivity.invalidateOptionsMenu();
        }
    }

    @Override
    public void showFileChooser(ValueCallback<Uri[]> valueCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        this.mUploadHandler = new UploadHandler(this);
        this.mUploadHandler.openFileChooser(valueCallback, fileChooserParams);
    }

    @Override
    public void showPageInfo() {
        this.mPageDialogsHandler.showPageInfo(this.mTabControl.getCurrentTab(), false, null);
    }

    @Override
    public void showSslCertificateOnError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
        this.mPageDialogsHandler.showSSLCertificateOnError(webView, sslErrorHandler, sslError);
    }

    @Override
    public void start(Intent intent) {
        this.mCrashRecoveryHandler.startRecovery(intent);
    }

    @Override
    public void stopLoading() {
        this.mLoadStopped = true;
        Tab currentTab = this.mTabControl.getCurrentTab();
        WebView currentTopWebView = getCurrentTopWebView();
        if (currentTopWebView != null) {
            currentTopWebView.stopLoading();
            this.mUi.onPageStopped(currentTab);
        }
    }

    @Override
    public boolean supportsVoice() {
        return this.mActivity.getPackageManager().queryIntentActivities(new Intent("android.speech.action.RECOGNIZE_SPEECH"), 0).size() != 0;
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
    public void toggleUserAgent() {
        WebView currentWebView = getCurrentWebView();
        this.mSettings.toggleDesktopUseragent(currentWebView);
        if (this.mSettings.hasDesktopUseragent(currentWebView)) {
            currentWebView.loadUrl(currentWebView.getOriginalUrl());
            return;
        }
        HashMap map = new HashMap();
        map.put(Browser.HEADER, Browser.UAPROF);
        currentWebView.loadUrl(currentWebView.getOriginalUrl(), map);
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
        boolean z;
        boolean zHasDesktopUseragent;
        boolean zEquals;
        boolean zCanGoForward;
        boolean zCanGoBack;
        if (tab != null) {
            zCanGoBack = tab.canGoBack();
            zCanGoForward = tab.canGoForward();
            zEquals = this.mSettings.getHomePage().equals(tab.getUrl());
            zHasDesktopUseragent = this.mSettings.hasDesktopUseragent(tab.getWebView());
            z = !tab.isSnapshot();
        } else {
            z = false;
            zHasDesktopUseragent = false;
            zEquals = false;
            zCanGoForward = false;
            zCanGoBack = false;
        }
        menu.findItem(2131558607).setEnabled(zCanGoBack);
        menu.findItem(2131558596).setEnabled(!zEquals);
        MenuItem menuItemFindItem = menu.findItem(2131558573);
        menuItemFindItem.setEnabled(zCanGoForward);
        menu.findItem(2131558574).setEnabled(isInLoad());
        menu.setGroupVisible(2131558571, z);
        if (BrowserSettings.getInstance().useFullscreen() || BrowserSettings.getInstance().useQuickControls()) {
            menuItemFindItem.setVisible(false);
            menuItemFindItem.setEnabled(zCanGoForward);
        } else {
            menuItemFindItem.setVisible(false);
        }
        PackageManager packageManager = this.mActivity.getPackageManager();
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/plain");
        menu.findItem(2131558579).setVisible(packageManager.resolveActivity(intent, 65536) != null);
        boolean zEnableNavDump = this.mSettings.enableNavDump();
        MenuItem menuItemFindItem2 = menu.findItem(2131558592);
        menuItemFindItem2.setVisible(zEnableNavDump);
        menuItemFindItem2.setEnabled(zEnableNavDump);
        this.mSettings.isDebugEnabled();
        MenuItem menuItemFindItem3 = menu.findItem(2131558581);
        menuItemFindItem3.setChecked(zHasDesktopUseragent);
        menuItemFindItem3.setEnabled(true);
        menu.setGroupVisible(2131558578, z);
        menu.setGroupVisible(2131558586, z ? false : true);
        menu.setGroupVisible(2131558588, false);
        this.mUi.updateMenuState(tab, menu);
    }

    void viewDownloads() {
        this.mActivity.startActivity(new Intent("android.intent.action.VIEW_DOWNLOADS"));
    }
}
