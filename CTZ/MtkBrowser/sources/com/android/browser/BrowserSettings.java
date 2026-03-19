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
    private Runnable mSetup = new Runnable() {
        @Override
        public void run() {
            DisplayMetrics displayMetrics = BrowserSettings.this.mContext.getResources().getDisplayMetrics();
            BrowserSettings.this.mFontSizeMult = displayMetrics.scaledDensity / displayMetrics.density;
            if (ActivityManager.staticGetMemoryClass() > 16) {
                BrowserSettings.this.mPageCacheCapacity = 5;
            }
            BrowserSettings.this.mWebStorageSizeManager = new WebStorageSizeManager(BrowserSettings.this.mContext, new WebStorageSizeManager.StatFsDiskInfo(BrowserSettings.this.getAppCachePath()), new WebStorageSizeManager.WebKitAppCacheInfo(BrowserSettings.this.getAppCachePath()));
            BrowserSettings.this.mPrefs.registerOnSharedPreferenceChangeListener(BrowserSettings.this);
            if (Build.VERSION.CODENAME.equals("REL")) {
                BrowserSettings.this.setDebugEnabled(false);
            }
            if (BrowserSettings.this.mPrefs.contains("text_size")) {
                switch (AnonymousClass2.$SwitchMap$android$webkit$WebSettings$TextSize[BrowserSettings.this.getTextSize().ordinal()]) {
                    case 1:
                        BrowserSettings.this.setTextZoom(50);
                        break;
                    case 2:
                        BrowserSettings.this.setTextZoom(75);
                        break;
                    case 3:
                        BrowserSettings.this.setTextZoom(150);
                        break;
                    case 4:
                        BrowserSettings.this.setTextZoom(200);
                        break;
                }
                BrowserSettings.this.mPrefs.edit().remove("text_size").apply();
            }
            IBrowserSettingExt unused = BrowserSettings.sBrowserSettingExt = Extensions.getSettingPlugin(BrowserSettings.this.mContext);
            String unused2 = BrowserSettings.sFactoryResetUrl = BrowserSettings.sBrowserSettingExt.getCustomerHomepage();
            if (BrowserSettings.sFactoryResetUrl == null) {
                String unused3 = BrowserSettings.sFactoryResetUrl = BrowserSettings.this.mContext.getResources().getString(R.string.homepage_base);
                if (BrowserSettings.sFactoryResetUrl.indexOf("{CID}") != -1) {
                    String unused4 = BrowserSettings.sFactoryResetUrl = BrowserSettings.sFactoryResetUrl.replace("{CID}", BrowserProvider.getClientId(BrowserSettings.this.mContext.getContentResolver()));
                }
            }
            if (BrowserSettings.DEBUG) {
                Log.d("browser", "BrowserSettings.mSetup()--->run()--->sFactoryResetUrl : " + BrowserSettings.sFactoryResetUrl);
            }
            synchronized (BrowserSettings.class) {
                boolean unused5 = BrowserSettings.sInitialized = true;
                BrowserSettings.class.notifyAll();
            }
        }
    };
    private LinkedList<WeakReference<WebSettings>> mManagedSettings = new LinkedList<>();
    private WeakHashMap<WebSettings, String> mCustomUserAgents = new WeakHashMap<>();

    public static void initialize(Context context) {
        sInstance = new BrowserSettings(context);
    }

    public static BrowserSettings getInstance() {
        return sInstance;
    }

    private BrowserSettings(Context context) {
        this.mContext = context.getApplicationContext();
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        BackgroundHandler.execute(this.mSetup);
    }

    public void setController(Controller controller) {
        this.mController = controller;
        if (sInitialized) {
            syncSharedSettings();
        }
        sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
        sBrowserSettingExt.setOnlyLandscape(this.mPrefs, this.mController.getActivity());
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

    public static String getFactoryResetUrlFromRes(Context context) {
        sBrowserSettingExt = Extensions.getSettingPlugin(context);
        sFactoryResetUrl = sBrowserSettingExt.getCustomerHomepage();
        if (sFactoryResetUrl == null) {
            sFactoryResetUrl = context.getResources().getString(R.string.homepage_base);
        }
        if (sFactoryResetUrl.indexOf("{CID}") != -1) {
            sFactoryResetUrl = sFactoryResetUrl.replace("{CID}", BrowserProvider.getClientId(context.getContentResolver()));
        }
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.getFactoryResetUrlFromRes()--->sFactoryResetUrl : " + sFactoryResetUrl);
        }
        return sFactoryResetUrl;
    }

    private static void requireInitialization() {
        synchronized (BrowserSettings.class) {
            while (!sInitialized) {
                try {
                    BrowserSettings.class.wait();
                } catch (InterruptedException e) {
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
        if (getUserAgent() == 0 && string != null) {
            webSettings.setUserAgentString(string);
        } else {
            webSettings.setUserAgentString(USER_AGENTS[getUserAgent()]);
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
            if (this.mController != null && this.mController.getUi() != null) {
                this.mController.getUi().setFullscreen(useFullscreen());
                return;
            }
            return;
        }
        if ("enable_quick_controls".equals(str)) {
            if (this.mController != null && this.mController.getUi() != null) {
                this.mController.getUi().setUseQuickControls(sharedPreferences.getBoolean(str, false));
                return;
            }
            return;
        }
        if ("link_prefetch_when".equals(str)) {
            updateConnectionType();
        } else if ("landscape_only".equals(str)) {
            sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
            sBrowserSettingExt.setOnlyLandscape(sharedPreferences, this.mController.getActivity());
        }
    }

    public static String getFactoryResetHomeUrl(Context context) {
        requireInitialization();
        return sFactoryResetUrl;
    }

    public WebSettings.LayoutAlgorithm getLayoutAlgorithm() {
        WebSettings.LayoutAlgorithm layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL;
        WebSettings.LayoutAlgorithm layoutAlgorithm2 = Build.VERSION.SDK_INT >= 19 ? WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING : WebSettings.LayoutAlgorithm.NARROW_COLUMNS;
        if (autofitPages()) {
            layoutAlgorithm = layoutAlgorithm2;
        }
        if (isDebugEnabled()) {
            return isNormalLayout() ? WebSettings.LayoutAlgorithm.NORMAL : layoutAlgorithm2;
        }
        return layoutAlgorithm;
    }

    public WebStorageSizeManager getWebStorageSizeManager() {
        requireInitialization();
        return this.mWebStorageSizeManager;
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

    private void updateSearchEngine(boolean z) {
        String searchEngineName = getSearchEngineName();
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.updateSearchEngine()--->searchEngineName:" + searchEngineName);
        }
        if (z || this.mSearchEngine == null || searchEngineName == null || !this.mSearchEngine.getName().equals(searchEngineName)) {
            this.mSearchEngine = SearchEngines.get(this.mContext, searchEngineName);
        }
    }

    public SearchEngine getSearchEngine() {
        if (this.mSearchEngine == null) {
            updateSearchEngine(false);
        }
        return this.mSearchEngine;
    }

    public boolean isDebugEnabled() {
        requireInitialization();
        return this.mPrefs.getBoolean("debug_menu", false);
    }

    public void setDebugEnabled(boolean z) {
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        editorEdit.putBoolean("debug_menu", z);
        if (!z) {
            editorEdit.putBoolean("enable_hardware_accel_skia", false);
        }
        editorEdit.apply();
    }

    public void clearCache() {
        WebView currentWebView;
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearCache()--->");
        }
        WebIconDatabase.getInstance().removeAllIcons();
        if (this.mController != null && (currentWebView = this.mController.getCurrentWebView()) != null) {
            currentWebView.clearCache(true);
        }
    }

    public void clearCookies() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearCookies()--->");
        }
        CookieManager.getInstance().removeAllCookie();
    }

    public void clearHistory() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearHistory()--->");
        }
        BrowserHistoryPage.ClearHistoryTask clearHistoryTask = new BrowserHistoryPage.ClearHistoryTask(this.mContext.getContentResolver());
        if (!clearHistoryTask.isAlive()) {
            clearHistoryTask.start();
        }
    }

    public void clearFormData() {
        WebView currentTopWebView;
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearFormData()--->");
        }
        WebViewDatabase.getInstance(this.mContext).clearFormData();
        if (this.mController != null && (currentTopWebView = this.mController.getCurrentTopWebView()) != null) {
            currentTopWebView.clearFormData();
        }
    }

    public void clearPasswords() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearPasswords()--->");
        }
        WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(this.mContext);
        webViewDatabase.clearUsernamePassword();
        webViewDatabase.clearHttpAuthUsernamePassword();
    }

    public void clearDatabases() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearDatabases()--->");
        }
        WebStorage.getInstance().deleteAllData();
    }

    public void clearLocationAccess() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearLocationAccess()--->");
        }
        GeolocationPermissions.getInstance().clearAll();
    }

    public void resetDefaultPreferences() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.resetDefaultPreferences()--->");
        }
        this.mPrefs.edit().clear().putLong("last_autologin_time", this.mPrefs.getLong("last_autologin_time", -1L)).apply();
        resetCachedValues();
        syncManagedSettings();
    }

    private void resetCachedValues() {
        updateSearchEngine(false);
    }

    public void toggleDebugSettings() {
        setDebugEnabled(!isDebugEnabled());
    }

    public boolean hasDesktopUseragent(WebView webView) {
        return (webView == null || this.mCustomUserAgents.get(webView.getSettings()) == null) ? false : true;
    }

    public boolean isDesktopUserAgent(WebView webView) {
        String userAgentString = webView.getSettings().getUserAgentString();
        if (userAgentString != null) {
            return userAgentString.equals("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36");
        }
        return false;
    }

    public void changeUserAgent(WebView webView, boolean z) {
        String operatorUA;
        if (webView == null) {
            return;
        }
        WebSettings settings = webView.getSettings();
        if (!z) {
            Log.i("Browser/Settings", "UA restore");
            if (this.mCustomUserAgents.get(settings) != null) {
                return;
            }
            String string = CustomProperties.getString("browser", "UserAgent");
            if ((string == null || string.length() == 0) && (operatorUA = Extensions.getSettingPlugin(this.mContext).getOperatorUA(settings.getUserAgentString())) != null && operatorUA.length() > 0) {
                string = operatorUA;
            }
            if (getUserAgent() == 0 && string != null) {
                settings.setUserAgentString(string);
                return;
            } else {
                settings.setUserAgentString(USER_AGENTS[getUserAgent()]);
                return;
            }
        }
        Log.i("Browser/Settings", "UA change to desktop");
        settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36");
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

    public static int getAdjustedMinimumFontSize(int i) {
        int i2 = i + 1;
        if (i2 > 1) {
            return i2 + 3;
        }
        return i2;
    }

    public int getAdjustedTextZoom(int i) {
        return (int) ((((i - 10) * 5) + 100) * this.mFontSizeMult);
    }

    static int getRawTextZoom(int i) {
        return ((i - 100) / 5) + 10;
    }

    public int getAdjustedDoubleTapZoom(int i) {
        return (int) ((((i - 5) * 5) + 100) * this.mFontSizeMult);
    }

    public SharedPreferences getPreferences() {
        return this.mPrefs;
    }

    public void updateConnectionType() {
        int type;
        ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        String linkPrefetchEnabled = getLinkPrefetchEnabled();
        boolean zEquals = linkPrefetchEnabled.equals(getLinkPrefetchAlwaysPreferenceString(this.mContext));
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && ((type = activeNetworkInfo.getType()) == 1 || type == 7 || type == 9)) {
            zEquals |= linkPrefetchEnabled.equals(getLinkPrefetchOnWifiOnlyPreferenceString(this.mContext));
        }
        if (this.mLinkPrefetchAllowed != zEquals) {
            this.mLinkPrefetchAllowed = zEquals;
            syncManagedSettings();
        }
    }

    @Deprecated
    private WebSettings.TextSize getTextSize() {
        return WebSettings.TextSize.valueOf(this.mPrefs.getString("text_size", "NORMAL"));
    }

    public int getMinimumFontSize() {
        return getAdjustedMinimumFontSize(this.mPrefs.getInt("min_font_size", 0));
    }

    public int getTextZoom() {
        requireInitialization();
        return getAdjustedTextZoom(this.mPrefs.getInt("text_zoom", 10));
    }

    public void setTextZoom(int i) {
        this.mPrefs.edit().putInt("text_zoom", getRawTextZoom(i)).apply();
    }

    public int getDoubleTapZoom() {
        requireInitialization();
        return getAdjustedDoubleTapZoom(this.mPrefs.getInt("double_tap_zoom", 5));
    }

    public String getSearchEngineName() {
        String string;
        boolean z;
        SearchEngineManager searchEngineManager = (SearchEngineManager) this.mContext.getSystemService("search_engine_service");
        List availables = searchEngineManager.getAvailables();
        if (availables == null || availables.size() <= 0) {
            return null;
        }
        String name = "google";
        com.mediatek.common.search.SearchEngine searchEngine = searchEngineManager.getDefault();
        if (searchEngine != null) {
            name = searchEngine.getName();
        }
        sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
        String searchEngine2 = sBrowserSettingExt.getSearchEngine(this.mPrefs, this.mContext);
        com.mediatek.common.search.SearchEngine byName = searchEngineManager.getByName(searchEngine2);
        if (byName != null) {
            string = byName.getFaviconUri();
        } else {
            string = this.mPrefs.getString("search_engine_favicon", "");
        }
        int size = availables.size();
        String[] strArr = new String[size];
        String[] strArr2 = new String[size];
        com.mediatek.common.search.SearchEngine bestMatch = searchEngineManager.getBestMatch("", string);
        if (bestMatch == null || searchEngine2.equals(bestMatch.getName())) {
            z = false;
        } else {
            searchEngine2 = bestMatch.getName();
            z = true;
        }
        int i = -1;
        for (int i2 = 0; i2 < size; i2++) {
            strArr[i2] = ((com.mediatek.common.search.SearchEngine) availables.get(i2)).getName();
            strArr2[i2] = ((com.mediatek.common.search.SearchEngine) availables.get(i2)).getFaviconUri();
            if (strArr[i2].equals(searchEngine2)) {
                i = i2;
            }
        }
        if (i == -1) {
            i = 0;
            for (int i3 = 0; i3 < size; i3++) {
                if (strArr[i3].equals(name)) {
                    i = i3;
                }
            }
            z = true;
        }
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.getSearchEngineName-->selectedItem = " + i + "entryValues[" + i + "]=" + strArr);
        }
        if (z && i != -1) {
            SharedPreferences.Editor editorEdit = this.mPrefs.edit();
            editorEdit.putString("search_engine", strArr[i]);
            editorEdit.putString("search_engine_favicon", strArr2[i]);
            editorEdit.commit();
        }
        return strArr[i];
    }

    public boolean allowAppTabs() {
        return this.mPrefs.getBoolean("allow_apptabs", false);
    }

    public boolean openInBackground() {
        return this.mPrefs.getBoolean("open_in_background", false);
    }

    public boolean enableJavascript() {
        return this.mPrefs.getBoolean("enable_javascript", true);
    }

    public WebSettings.PluginState getPluginState() {
        return WebSettings.PluginState.valueOf(this.mPrefs.getString("plugin_state", "ON"));
    }

    public boolean loadPageInOverviewMode() {
        boolean z = this.mPrefs.getBoolean("load_page", true);
        Log.i("Browser/Settings", "loadMode: " + z);
        return z;
    }

    public boolean autofitPages() {
        return this.mPrefs.getBoolean("autofit_pages", true);
    }

    public boolean blockPopupWindows() {
        return this.mPrefs.getBoolean("block_popup_windows", true);
    }

    public boolean loadImages() {
        return this.mPrefs.getBoolean("load_images", true);
    }

    public String getDownloadPath() {
        return this.mPrefs.getString("download_directory_setting", getDefaultDownloadPathWithMultiSDcard());
    }

    public String getDefaultDownloadPathWithMultiSDcard() {
        sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
        if (DEBUG) {
            Log.d("browser", "Default Download Path:" + sBrowserSettingExt.getDefaultDownloadFolder());
        }
        return sBrowserSettingExt.getDefaultDownloadFolder();
    }

    public String getHomePage() {
        return this.mPrefs.getString("homepage", getFactoryResetHomeUrl(this.mContext));
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

    public boolean isHardwareAccelerated() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("enable_hardware_accel", true);
        }
        return true;
    }

    public int getUserAgent() {
        if (!isDebugEnabled()) {
            return 0;
        }
        return Integer.parseInt(this.mPrefs.getString("user_agent", "0"));
    }

    public boolean enableJavascriptConsole() {
        if (!isDebugEnabled()) {
            return false;
        }
        return this.mPrefs.getBoolean("javascript_console", true);
    }

    public boolean isWideViewport() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("wide_viewport", true);
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

    public String getJsEngineFlags() {
        if (!isDebugEnabled()) {
            return "";
        }
        return this.mPrefs.getString("js_engine_flags", "");
    }

    public boolean useQuickControls() {
        return this.mPrefs.getBoolean("enable_quick_controls", false);
    }

    public boolean useMostVisitedHomepage() {
        return "content://com.android.browser.home/".equals(getHomePage());
    }

    public boolean useFullscreen() {
        return this.mPrefs.getBoolean("fullscreen", false);
    }

    public boolean showSecurityWarnings() {
        return this.mPrefs.getBoolean("show_security_warnings", true);
    }

    public boolean acceptCookies() {
        return this.mPrefs.getBoolean("accept_cookies", true);
    }

    public boolean saveFormdata() {
        return this.mPrefs.getBoolean("save_formdata", true);
    }

    public boolean enableGeolocation() {
        return this.mPrefs.getBoolean("enable_geolocation", true);
    }

    public boolean rememberPasswords() {
        return this.mPrefs.getBoolean("remember_passwords", true);
    }

    public static String getPreloadOnWifiOnlyPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_data_preload_value_wifi_only);
    }

    public static String getPreloadAlwaysPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_data_preload_value_always);
    }

    public String getDefaultPreloadSetting() {
        String string = Settings.Secure.getString(this.mContext.getContentResolver(), "browser_default_preload_setting");
        if (string == null) {
            return this.mContext.getResources().getString(R.string.pref_data_preload_default_value);
        }
        return string;
    }

    public String getPreloadEnabled() {
        return this.mPrefs.getString("preload_when", getDefaultPreloadSetting());
    }

    public static String getLinkPrefetchOnWifiOnlyPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_link_prefetch_value_wifi_only);
    }

    public static String getLinkPrefetchAlwaysPreferenceString(Context context) {
        return context.getResources().getString(R.string.pref_link_prefetch_value_always);
    }

    public String getDefaultLinkPrefetchSetting() {
        String string = Settings.Secure.getString(this.mContext.getContentResolver(), "browser_default_link_prefetch_setting");
        if (string == null) {
            return this.mContext.getResources().getString(R.string.pref_link_prefetch_default_value);
        }
        return string;
    }

    public String getLinkPrefetchEnabled() {
        return this.mPrefs.getString("link_prefetch_when", getDefaultLinkPrefetchSetting());
    }

    public long getLastRecovered() {
        return this.mPrefs.getLong("last_recovered", 0L);
    }

    public void setLastRecovered(long j) {
        this.mPrefs.edit().putLong("last_recovered", j).apply();
    }

    public boolean wasLastRunPaused() {
        return this.mPrefs.getBoolean("last_paused", false);
    }

    public void setLastRunPaused(boolean z) {
        this.mPrefs.edit().putBoolean("last_paused", z).apply();
    }

    public void onConfigurationChanged(Configuration configuration) {
        updateSearchEngine(false);
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
}
