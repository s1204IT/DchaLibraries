package com.android.browser;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import com.android.browser.BrowserHistoryPage;
import com.android.browser.WebStorageSizeManager;
import com.android.browser.provider.BrowserProvider;
import com.android.browser.search.SearchEngine;
import com.android.browser.search.SearchEngines;
import com.mediatek.browser.ext.IBrowserSettingExt;
import com.mediatek.custom.CustomProperties;
import com.mediatek.search.SearchEngineManager;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

public class BrowserSettings implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static String sFactoryResetUrl;
    private static BrowserSettings sInstance;
    private String mAppCachePath;
    private Context mContext;
    private Controller mController;
    private SharedPreferences mPrefs;
    private SearchEngine mSearchEngine;
    private WebStorageSizeManager mWebStorageSizeManager;
    private static final String[] USER_AGENTS = {null, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_0 like Mac OS X; en-us) AppleWebKit/532.9 (KHTML, like Gecko) Version/4.0.5 Mobile/8A293 Safari/6531.22.7", "Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Version/4.0.4 Mobile/7B367 Safari/531.21.10", "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1", "Mozilla/5.0 (Linux; U; Android 3.1; en-us; Xoom Build/HMJ25) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13"};
    private static final boolean DEBUG = Browser.DEBUG;
    private static boolean sInitialized = false;
    private static IBrowserSettingExt sBrowserSettingExt = null;
    private boolean mNeedsSharedSync = true;
    private float mFontSizeMult = 1.0f;
    private boolean mLinkPrefetchAllowed = true;
    private int mPageCacheCapacity = 1;
    private Runnable mSetup = new Runnable(this) {
        final BrowserSettings this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void run() {
            DisplayMetrics displayMetrics = this.this$0.mContext.getResources().getDisplayMetrics();
            this.this$0.mFontSizeMult = displayMetrics.scaledDensity / displayMetrics.density;
            if (ActivityManager.staticGetMemoryClass() > 16) {
                this.this$0.mPageCacheCapacity = 5;
            }
            this.this$0.mWebStorageSizeManager = new WebStorageSizeManager(this.this$0.mContext, new WebStorageSizeManager.StatFsDiskInfo(this.this$0.getAppCachePath()), new WebStorageSizeManager.WebKitAppCacheInfo(this.this$0.getAppCachePath()));
            this.this$0.mPrefs.registerOnSharedPreferenceChangeListener(this.this$0);
            if (Build.VERSION.CODENAME.equals("REL")) {
                this.this$0.setDebugEnabled(false);
            }
            if (this.this$0.mPrefs.contains("text_size")) {
                switch (AnonymousClass2.$SwitchMap$android$webkit$WebSettings$TextSize[this.this$0.getTextSize().ordinal()]) {
                    case 1:
                        this.this$0.setTextZoom(50);
                        break;
                    case 2:
                        this.this$0.setTextZoom(75);
                        break;
                    case 3:
                        this.this$0.setTextZoom(150);
                        break;
                    case 4:
                        this.this$0.setTextZoom(200);
                        break;
                }
                this.this$0.mPrefs.edit().remove("text_size").apply();
            }
            IBrowserSettingExt unused = BrowserSettings.sBrowserSettingExt = Extensions.getSettingPlugin(this.this$0.mContext);
            String unused2 = BrowserSettings.sFactoryResetUrl = BrowserSettings.sBrowserSettingExt.getCustomerHomepage();
            if (BrowserSettings.sFactoryResetUrl == null) {
                String unused3 = BrowserSettings.sFactoryResetUrl = this.this$0.mContext.getResources().getString(2131493225);
                if (BrowserSettings.sFactoryResetUrl.indexOf("{CID}") != -1) {
                    String unused4 = BrowserSettings.sFactoryResetUrl = BrowserSettings.sFactoryResetUrl.replace("{CID}", BrowserProvider.getClientId(this.this$0.mContext.getContentResolver()));
                }
            }
            if (BrowserSettings.DEBUG) {
                Log.d("browser", "BrowserSettings.mSetup()--->run()--->sFactoryResetUrl : " + BrowserSettings.sFactoryResetUrl);
            }
            synchronized (BrowserSettings.class) {
                try {
                    boolean unused5 = BrowserSettings.sInitialized = true;
                    BrowserSettings.class.notifyAll();
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    };
    private LinkedList<WeakReference<WebSettings>> mManagedSettings = new LinkedList<>();
    private WeakHashMap<WebSettings, String> mCustomUserAgents = new WeakHashMap<>();

    static class AnonymousClass2 {
        static final int[] $SwitchMap$android$webkit$WebSettings$TextSize = new int[WebSettings.TextSize.values().length];

        static {
            try {
                $SwitchMap$android$webkit$WebSettings$TextSize[WebSettings.TextSize.SMALLEST.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$webkit$WebSettings$TextSize[WebSettings.TextSize.SMALLER.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$webkit$WebSettings$TextSize[WebSettings.TextSize.LARGER.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$webkit$WebSettings$TextSize[WebSettings.TextSize.LARGEST.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private BrowserSettings(Context context) {
        this.mContext = context.getApplicationContext();
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        BackgroundHandler.execute(this.mSetup);
    }

    public static int getAdjustedMinimumFontSize(int i) {
        int i2 = i + 1;
        return i2 > 1 ? i2 + 3 : i2;
    }

    private String getAppCachePath() {
        if (this.mAppCachePath == null) {
            this.mAppCachePath = this.mContext.getDir("appcache", 0).getPath();
        }
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.getAppCachePath()--->mAppCachePath:" + this.mAppCachePath);
        }
        return this.mAppCachePath;
    }

    public static String getFactoryResetHomeUrl(Context context) {
        requireInitialization();
        return sFactoryResetUrl;
    }

    public static String getFactoryResetUrlFromRes(Context context) {
        sBrowserSettingExt = Extensions.getSettingPlugin(context);
        sFactoryResetUrl = sBrowserSettingExt.getCustomerHomepage();
        if (sFactoryResetUrl == null) {
            sFactoryResetUrl = context.getResources().getString(2131493225);
        }
        if (sFactoryResetUrl.indexOf("{CID}") != -1) {
            sFactoryResetUrl = sFactoryResetUrl.replace("{CID}", BrowserProvider.getClientId(context.getContentResolver()));
        }
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.getFactoryResetUrlFromRes()--->sFactoryResetUrl : " + sFactoryResetUrl);
        }
        return sFactoryResetUrl;
    }

    public static BrowserSettings getInstance() {
        return sInstance;
    }

    public static String getLinkPrefetchAlwaysPreferenceString(Context context) {
        return context.getResources().getString(2131493193);
    }

    public static String getLinkPrefetchOnWifiOnlyPreferenceString(Context context) {
        return context.getResources().getString(2131493192);
    }

    public static String getPreloadAlwaysPreferenceString(Context context) {
        return context.getResources().getString(2131493187);
    }

    public static String getPreloadOnWifiOnlyPreferenceString(Context context) {
        return context.getResources().getString(2131493186);
    }

    static int getRawTextZoom(int i) {
        return ((i - 100) / 5) + 10;
    }

    @Deprecated
    private WebSettings.TextSize getTextSize() {
        return WebSettings.TextSize.valueOf(this.mPrefs.getString("text_size", "NORMAL"));
    }

    public static void initialize(Context context) {
        sInstance = new BrowserSettings(context);
    }

    private static void requireInitialization() {
        synchronized (BrowserSettings.class) {
            while (!sInitialized) {
                try {
                    try {
                        BrowserSettings.class.wait();
                    } catch (InterruptedException e) {
                    }
                } finally {
                }
            }
        }
    }

    private void resetCachedValues() {
        updateSearchEngine(false);
    }

    private void syncManagedSettings() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.syncManagedSettings()--->");
        }
        syncSharedSettings();
        synchronized (this.mManagedSettings) {
            Iterator<WeakReference<WebSettings>> it = this.mManagedSettings.iterator();
            while (it.hasNext()) {
                WebSettings webSettings = it.next().get();
                if (webSettings == null) {
                    it.remove();
                } else {
                    syncSetting(webSettings);
                }
            }
        }
    }

    private void syncSetting(WebSettings webSettings) {
        String operatorUA;
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.syncSetting()--->");
        }
        webSettings.setGeolocationEnabled(enableGeolocation());
        webSettings.setJavaScriptEnabled(enableJavascript());
        webSettings.setLightTouchEnabled(enableLightTouch());
        webSettings.setNavDump(enableNavDump());
        webSettings.setMinimumFontSize(getMinimumFontSize());
        webSettings.setMinimumLogicalFontSize(getMinimumFontSize());
        webSettings.setPluginState(getPluginState());
        webSettings.setTextZoom(getTextZoom());
        setDoubleTapZoom(webSettings, getDoubleTapZoom());
        webSettings.setLayoutAlgorithm(getLayoutAlgorithm());
        webSettings.setJavaScriptCanOpenWindowsAutomatically(!blockPopupWindows());
        webSettings.setLoadsImagesAutomatically(loadImages());
        webSettings.setLoadWithOverviewMode(loadPageInOverviewMode());
        webSettings.setSavePassword(rememberPasswords());
        webSettings.setSaveFormData(saveFormdata());
        webSettings.setUseWideViewPort(isWideViewport());
        sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
        sBrowserSettingExt.setStandardFontFamily(webSettings, this.mPrefs);
        String str = this.mCustomUserAgents.get(webSettings);
        if (str != null) {
            webSettings.setUserAgentString(str);
            return;
        }
        String string = CustomProperties.getString("browser", "UserAgent");
        if ((string == null || string.length() == 0) && (operatorUA = Extensions.getSettingPlugin(this.mContext).getOperatorUA(webSettings.getUserAgentString())) != null && operatorUA.length() > 0) {
            string = operatorUA;
        }
        if (getUserAgent() != 0 || string == null) {
            webSettings.setUserAgentString(USER_AGENTS[getUserAgent()]);
        } else {
            webSettings.setUserAgentString(string);
        }
    }

    private void syncSharedSettings() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.syncSharedSettings()--->");
        }
        this.mNeedsSharedSync = false;
        CookieManager.getInstance().setAcceptCookie(acceptCookies());
        if (this.mController != null) {
            Iterator<Tab> it = this.mController.getTabs().iterator();
            while (it.hasNext()) {
                it.next().setAcceptThirdPartyCookies(acceptCookies());
            }
            this.mController.setShouldShowErrorConsole(enableJavascriptConsole());
        }
    }

    private void syncStaticSettings(WebSettings webSettings) {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.syncStaticSettings()--->");
        }
        webSettings.setDefaultFontSize(16);
        webSettings.setDefaultFixedFontSize(13);
        webSettings.setNeedInitialFocus(false);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setEnableSmoothTransition(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheMaxSize(getWebStorageSizeManager().getAppCacheMaxSize());
        webSettings.setAppCachePath(getAppCachePath());
        webSettings.setDatabasePath(this.mContext.getDir("databases", 0).getPath());
        webSettings.setGeolocationDatabasePath(this.mContext.getDir("geolocation", 0).getPath());
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setMixedContentMode(2);
    }

    private void updateSearchEngine(boolean z) {
        String searchEngineName = getSearchEngineName();
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.updateSearchEngine()--->searchEngineName:" + searchEngineName);
        }
        if (z || this.mSearchEngine == null || searchEngineName == null || !this.mSearchEngine.getName().equals(searchEngineName)) {
            this.mSearchEngine = SearchEngines.get(this.mContext, searchEngineName);
        }
    }

    public boolean acceptCookies() {
        return this.mPrefs.getBoolean("accept_cookies", true);
    }

    public boolean allowAppTabs() {
        return this.mPrefs.getBoolean("allow_apptabs", false);
    }

    public boolean autofitPages() {
        return this.mPrefs.getBoolean("autofit_pages", true);
    }

    public boolean blockPopupWindows() {
        return this.mPrefs.getBoolean("block_popup_windows", true);
    }

    public void changeUserAgent(WebView webView, boolean z) {
        String operatorUA;
        if (webView == null) {
            return;
        }
        WebSettings settings = webView.getSettings();
        if (z) {
            Log.i("Browser/Settings", "UA change to desktop");
            settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36");
            return;
        }
        Log.i("Browser/Settings", "UA restore");
        if (this.mCustomUserAgents.get(settings) == null) {
            String string = CustomProperties.getString("browser", "UserAgent");
            if ((string == null || string.length() == 0) && (operatorUA = Extensions.getSettingPlugin(this.mContext).getOperatorUA(settings.getUserAgentString())) != null && operatorUA.length() > 0) {
                string = operatorUA;
            }
            if (getUserAgent() != 0 || string == null) {
                settings.setUserAgentString(USER_AGENTS[getUserAgent()]);
            } else {
                settings.setUserAgentString(string);
            }
        }
    }

    public void clearCache() {
        WebView currentWebView;
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearCache()--->");
        }
        WebIconDatabase.getInstance().removeAllIcons();
        if (this.mController == null || (currentWebView = this.mController.getCurrentWebView()) == null) {
            return;
        }
        currentWebView.clearCache(true);
    }

    public void clearCookies() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearCookies()--->");
        }
        CookieManager.getInstance().removeAllCookie();
    }

    public void clearDatabases() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearDatabases()--->");
        }
        WebStorage.getInstance().deleteAllData();
    }

    public void clearFormData() {
        WebView currentTopWebView;
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearFormData()--->");
        }
        WebViewDatabase.getInstance(this.mContext).clearFormData();
        if (this.mController == null || (currentTopWebView = this.mController.getCurrentTopWebView()) == null) {
            return;
        }
        currentTopWebView.clearFormData();
    }

    public void clearHistory() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearHistory()--->");
        }
        BrowserHistoryPage.ClearHistoryTask clearHistoryTask = new BrowserHistoryPage.ClearHistoryTask(this.mContext.getContentResolver());
        if (clearHistoryTask.isAlive()) {
            return;
        }
        clearHistoryTask.start();
    }

    public void clearLocationAccess() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearLocationAccess()--->");
        }
        GeolocationPermissions.getInstance().clearAll();
    }

    public void clearPasswords() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearPasswords()--->");
        }
        WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(this.mContext);
        webViewDatabase.clearUsernamePassword();
        webViewDatabase.clearHttpAuthUsernamePassword();
    }

    public boolean enableGeolocation() {
        return this.mPrefs.getBoolean("enable_geolocation", true);
    }

    public boolean enableJavascript() {
        return this.mPrefs.getBoolean("enable_javascript", true);
    }

    public boolean enableJavascriptConsole() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("javascript_console", true);
        }
        return false;
    }

    public boolean enableLightTouch() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("enable_light_touch", false);
        }
        return false;
    }

    public boolean enableNavDump() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("enable_nav_dump", false);
        }
        return false;
    }

    public int getAdjustedDoubleTapZoom(int i) {
        return (int) ((((i - 5) * 5) + 100) * this.mFontSizeMult);
    }

    public int getAdjustedTextZoom(int i) {
        return (int) ((((i - 10) * 5) + 100) * this.mFontSizeMult);
    }

    public String getDefaultDownloadPathWithMultiSDcard() {
        sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
        if (DEBUG) {
            Log.d("browser", "Default Download Path:" + sBrowserSettingExt.getDefaultDownloadFolder());
        }
        return sBrowserSettingExt.getDefaultDownloadFolder();
    }

    public String getDefaultLinkPrefetchSetting() {
        String string = Settings.Secure.getString(this.mContext.getContentResolver(), "browser_default_link_prefetch_setting");
        return string == null ? this.mContext.getResources().getString(2131493194) : string;
    }

    public String getDefaultPreloadSetting() {
        String string = Settings.Secure.getString(this.mContext.getContentResolver(), "browser_default_preload_setting");
        return string == null ? this.mContext.getResources().getString(2131493188) : string;
    }

    public int getDoubleTapZoom() {
        requireInitialization();
        return getAdjustedDoubleTapZoom(this.mPrefs.getInt("double_tap_zoom", 5));
    }

    public String getDownloadPath() {
        return this.mPrefs.getString("download_directory_setting", getDefaultDownloadPathWithMultiSDcard());
    }

    public String getHomePage() {
        return this.mPrefs.getString("homepage", getFactoryResetHomeUrl(this.mContext));
    }

    public String getJsEngineFlags() {
        return !isDebugEnabled() ? "" : this.mPrefs.getString("js_engine_flags", "");
    }

    public long getLastRecovered() {
        return this.mPrefs.getLong("last_recovered", 0L);
    }

    public WebSettings.LayoutAlgorithm getLayoutAlgorithm() {
        WebSettings.LayoutAlgorithm layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL;
        WebSettings.LayoutAlgorithm layoutAlgorithm2 = Build.VERSION.SDK_INT >= 19 ? WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING : WebSettings.LayoutAlgorithm.NARROW_COLUMNS;
        if (autofitPages()) {
            layoutAlgorithm = layoutAlgorithm2;
        }
        return isDebugEnabled() ? isNormalLayout() ? WebSettings.LayoutAlgorithm.NORMAL : layoutAlgorithm2 : layoutAlgorithm;
    }

    public String getLinkPrefetchEnabled() {
        return this.mPrefs.getString("link_prefetch_when", getDefaultLinkPrefetchSetting());
    }

    public int getMinimumFontSize() {
        return getAdjustedMinimumFontSize(this.mPrefs.getInt("min_font_size", 0));
    }

    public WebSettings.PluginState getPluginState() {
        return WebSettings.PluginState.valueOf(this.mPrefs.getString("plugin_state", "ON"));
    }

    public SharedPreferences getPreferences() {
        return this.mPrefs;
    }

    public String getPreloadEnabled() {
        return this.mPrefs.getString("preload_when", getDefaultPreloadSetting());
    }

    public SearchEngine getSearchEngine() {
        if (this.mSearchEngine == null) {
            updateSearchEngine(false);
        }
        return this.mSearchEngine;
    }

    public String getSearchEngineName() {
        boolean z;
        int i;
        boolean z2 = true;
        SearchEngineManager searchEngineManager = (SearchEngineManager) this.mContext.getSystemService("search_engine_service");
        List availables = searchEngineManager.getAvailables();
        if (availables == null || availables.size() <= 0) {
            return null;
        }
        com.mediatek.common.search.SearchEngine searchEngine = searchEngineManager.getDefault();
        String name = searchEngine != null ? searchEngine.getName() : "google";
        sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
        String searchEngine2 = sBrowserSettingExt.getSearchEngine(this.mPrefs, this.mContext);
        com.mediatek.common.search.SearchEngine byName = searchEngineManager.getByName(searchEngine2);
        String faviconUri = byName != null ? byName.getFaviconUri() : this.mPrefs.getString("search_engine_favicon", "");
        int size = availables.size();
        String[] strArr = new String[size];
        String[] strArr2 = new String[size];
        com.mediatek.common.search.SearchEngine bestMatch = searchEngineManager.getBestMatch("", faviconUri);
        if (bestMatch == null || searchEngine2.equals(bestMatch.getName())) {
            z = false;
        } else {
            z = true;
            searchEngine2 = bestMatch.getName();
        }
        int i2 = -1;
        int i3 = 0;
        while (i3 < size) {
            strArr[i3] = ((com.mediatek.common.search.SearchEngine) availables.get(i3)).getName();
            strArr2[i3] = ((com.mediatek.common.search.SearchEngine) availables.get(i3)).getFaviconUri();
            int i4 = strArr[i3].equals(searchEngine2) ? i3 : i2;
            i3++;
            i2 = i4;
        }
        if (i2 == -1) {
            i = 0;
            for (int i5 = 0; i5 < size; i5++) {
                if (strArr[i5].equals(name)) {
                    i = i5;
                }
            }
        } else {
            z2 = z;
            i = i2;
        }
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.getSearchEngineName-->selectedItem = " + i + "entryValues[" + i + "]=" + strArr);
        }
        if (z2 && i != -1) {
            SharedPreferences.Editor editorEdit = this.mPrefs.edit();
            editorEdit.putString("search_engine", strArr[i]);
            editorEdit.putString("search_engine_favicon", strArr2[i]);
            editorEdit.commit();
        }
        return strArr[i];
    }

    public int getTextZoom() {
        requireInitialization();
        return getAdjustedTextZoom(this.mPrefs.getInt("text_zoom", 10));
    }

    public int getUserAgent() {
        if (isDebugEnabled()) {
            return Integer.parseInt(this.mPrefs.getString("user_agent", "0"));
        }
        return 0;
    }

    public WebStorageSizeManager getWebStorageSizeManager() {
        requireInitialization();
        return this.mWebStorageSizeManager;
    }

    public boolean hasDesktopUseragent(WebView webView) {
        return (webView == null || this.mCustomUserAgents.get(webView.getSettings()) == null) ? false : true;
    }

    public boolean isDebugEnabled() {
        requireInitialization();
        return this.mPrefs.getBoolean("debug_menu", false);
    }

    public boolean isDesktopUserAgent(WebView webView) {
        String userAgentString = webView.getSettings().getUserAgentString();
        if (userAgentString != null) {
            return userAgentString.equals("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36");
        }
        return false;
    }

    public boolean isHardwareAccelerated() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("enable_hardware_accel", true);
        }
        return true;
    }

    public boolean isNormalLayout() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("normal_layout", false);
        }
        return false;
    }

    public boolean isTracing() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("enable_tracing", false);
        }
        return false;
    }

    public boolean isWideViewport() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("wide_viewport", true);
        }
        return true;
    }

    public boolean loadImages() {
        return this.mPrefs.getBoolean("load_images", true);
    }

    public boolean loadPageInOverviewMode() {
        boolean z = this.mPrefs.getBoolean("load_page", true);
        Log.i("Browser/Settings", "loadMode: " + z);
        return z;
    }

    public void onConfigurationChanged(Configuration configuration) {
        updateSearchEngine(false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
        syncManagedSettings();
        if ("search_engine".equals(str)) {
            updateSearchEngine(false);
        }
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("BrowserSettings.onSharedPreferenceChanged()--->");
            sb.append(str);
            sb.append(" mControll is null:");
            sb.append(this.mController == null);
            Log.d("browser", sb.toString());
        }
        if (this.mController == null) {
            return;
        }
        if ("fullscreen".equals(str)) {
            if (this.mController == null || this.mController.getUi() == null) {
                return;
            }
            this.mController.getUi().setFullscreen(useFullscreen());
            return;
        }
        if ("enable_quick_controls".equals(str)) {
            if (this.mController == null || this.mController.getUi() == null) {
                return;
            }
            this.mController.getUi().setUseQuickControls(sharedPreferences.getBoolean(str, false));
            return;
        }
        if ("link_prefetch_when".equals(str)) {
            updateConnectionType();
        } else if ("landscape_only".equals(str)) {
            sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
            sBrowserSettingExt.setOnlyLandscape(sharedPreferences, this.mController.getActivity());
        }
    }

    public boolean openInBackground() {
        return this.mPrefs.getBoolean("open_in_background", false);
    }

    public boolean rememberPasswords() {
        return this.mPrefs.getBoolean("remember_passwords", true);
    }

    public void resetDefaultPreferences() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.resetDefaultPreferences()--->");
        }
        this.mPrefs.edit().clear().putLong("last_autologin_time", this.mPrefs.getLong("last_autologin_time", -1L)).apply();
        resetCachedValues();
        syncManagedSettings();
    }

    public boolean saveFormdata() {
        return this.mPrefs.getBoolean("save_formdata", true);
    }

    public void setController(Controller controller) {
        this.mController = controller;
        if (sInitialized) {
            syncSharedSettings();
        }
        sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
        sBrowserSettingExt.setOnlyLandscape(this.mPrefs, this.mController.getActivity());
    }

    public void setDebugEnabled(boolean z) {
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        editorEdit.putBoolean("debug_menu", z);
        if (!z) {
            editorEdit.putBoolean("enable_hardware_accel_skia", false);
        }
        editorEdit.apply();
    }

    public void setDoubleTapZoom(WebSettings webSettings, int i) {
        try {
            Method declaredMethod = webSettings.getClass().getDeclaredMethod("getAwSettings", new Class[0]);
            declaredMethod.setAccessible(true);
            Object objInvoke = declaredMethod.invoke(webSettings, new Object[0]);
            objInvoke.getClass().getMethod("setDoubleTapZoom", Integer.TYPE).invoke(objInvoke, Integer.valueOf(i));
        } catch (IllegalAccessException e) {
            Log.e("WebSettings", "Illegal access for setDoubleTapZoom:" + e);
        } catch (NoSuchMethodException e2) {
            Log.e("WebSettings", "No such method for setDoubleTapZoom: " + e2);
        } catch (NullPointerException e3) {
            Log.e("WebSettings", "Null pointer for setDoubleTapZoom: " + e3);
        } catch (InvocationTargetException e4) {
            Log.e("WebSettings", "Invocation target exception for setDoubleTapZoom: " + e4);
        }
    }

    public void setHomePage(String str) {
        this.mPrefs.edit().putString("homepage", str).apply();
        if (DEBUG) {
            Log.i("Browser/Settings", "BrowserSettings: setHomePage : " + str);
        }
    }

    public void setHomePagePicker(String str) {
        this.mPrefs.edit().putString("homepage_picker", str).apply();
        Log.i("Browser/Settings", "BrowserSettings: setHomePagePicker : " + str);
    }

    public void setLastRecovered(long j) {
        this.mPrefs.edit().putLong("last_recovered", j).apply();
    }

    public void setLastRunPaused(boolean z) {
        this.mPrefs.edit().putBoolean("last_paused", z).apply();
    }

    public void setTextZoom(int i) {
        this.mPrefs.edit().putInt("text_zoom", getRawTextZoom(i)).apply();
    }

    public boolean showSecurityWarnings() {
        return this.mPrefs.getBoolean("show_security_warnings", true);
    }

    public void startManagingSettings(WebSettings webSettings) {
        if (this.mNeedsSharedSync) {
            syncSharedSettings();
        }
        synchronized (this.mManagedSettings) {
            syncStaticSettings(webSettings);
            syncSetting(webSettings);
            this.mManagedSettings.add(new WeakReference<>(webSettings));
        }
    }

    public void stopManagingSettings(WebSettings webSettings) {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.stopManagingSettings()--->");
        }
        Iterator<WeakReference<WebSettings>> it = this.mManagedSettings.iterator();
        while (it.hasNext()) {
            if (it.next().get() == webSettings) {
                it.remove();
                return;
            }
        }
    }

    public void toggleDebugSettings() {
        setDebugEnabled(!isDebugEnabled());
    }

    public void toggleDesktopUseragent(WebView webView) {
        if (webView == null) {
            return;
        }
        WebSettings settings = webView.getSettings();
        if (this.mCustomUserAgents.get(settings) != null) {
            this.mCustomUserAgents.remove(settings);
            settings.setUserAgentString(USER_AGENTS[getUserAgent()]);
        } else {
            this.mCustomUserAgents.put(settings, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36");
            settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36");
        }
    }

    public void updateConnectionType() {
        boolean zEquals;
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        String linkPrefetchEnabled = getLinkPrefetchEnabled();
        boolean zEquals2 = linkPrefetchEnabled.equals(getLinkPrefetchAlwaysPreferenceString(this.mContext));
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            int type = activeNetworkInfo.getType();
            zEquals = (type == 1 || type == 7 || type == 9) ? linkPrefetchEnabled.equals(getLinkPrefetchOnWifiOnlyPreferenceString(this.mContext)) | zEquals2 : zEquals2;
        } else {
            zEquals = zEquals2;
        }
        if (this.mLinkPrefetchAllowed != zEquals) {
            this.mLinkPrefetchAllowed = zEquals;
            syncManagedSettings();
        }
    }

    public void updateSearchEngineSetting() {
        String searchEngine = Extensions.getRegionalPhonePlugin(this.mContext).getSearchEngine(this.mPrefs, this.mContext);
        if (searchEngine == null) {
            Log.i("Browser/Settings", "updateSearchEngineSetting ---no change");
            return;
        }
        com.mediatek.common.search.SearchEngine byName = ((SearchEngineManager) this.mContext.getSystemService("search_engine_service")).getByName(searchEngine);
        if (byName == null) {
            Log.i("Browser/Settings", "updateSearchEngineSetting ---" + searchEngine + " not found");
            return;
        }
        String faviconUri = byName.getFaviconUri();
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        editorEdit.putString("search_engine", searchEngine);
        editorEdit.putString("search_engine_favicon", faviconUri);
        editorEdit.commit();
        Log.i("Browser/Settings", "updateSearchEngineSetting --" + searchEngine + "--" + faviconUri);
    }

    public boolean useFullscreen() {
        return this.mPrefs.getBoolean("fullscreen", false);
    }

    public boolean useMostVisitedHomepage() {
        return "content://com.android.browser.home/".equals(getHomePage());
    }

    public boolean useQuickControls() {
        return this.mPrefs.getBoolean("enable_quick_controls", false);
    }

    public boolean wasLastRunPaused() {
        return this.mPrefs.getBoolean("last_paused", false);
    }
}
