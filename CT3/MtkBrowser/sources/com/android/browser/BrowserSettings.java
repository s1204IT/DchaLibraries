package com.android.browser;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
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
import com.mediatek.browser.ext.IBrowserFeatureIndexExt;
import com.mediatek.browser.ext.IBrowserRegionalPhoneExt;
import com.mediatek.browser.ext.IBrowserSettingExt;
import com.mediatek.custom.CustomProperties;
import com.mediatek.search.SearchEngineManager;
import java.lang.ref.WeakReference;
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
    private static final String[] USER_AGENTS = {null, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.34 Safari/534.24", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_0 like Mac OS X; en-us) AppleWebKit/532.9 (KHTML, like Gecko) Version/4.0.5 Mobile/8A293 Safari/6531.22.7", "Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Version/4.0.4 Mobile/7B367 Safari/531.21.10", "Mozilla/5.0 (Linux; U; Android 2.2; en-us; Nexus One Build/FRF91) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1", "Mozilla/5.0 (Linux; U; Android 3.1; en-us; Xoom Build/HMJ25) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13"};
    private static final boolean DEBUG = Browser.DEBUG;
    private static boolean sInitialized = false;
    private static IBrowserSettingExt sBrowserSettingExt = null;
    private boolean mNeedsSharedSync = true;
    private float mFontSizeMult = 1.0f;
    private boolean mLinkPrefetchAllowed = true;
    private int mPageCacheCapacity = 1;
    private Runnable mSetup = new Runnable() {

        private static final int[] f0androidwebkitWebSettings$TextSizeSwitchesValues = null;

        private static int[] m46getandroidwebkitWebSettings$TextSizeSwitchesValues() {
            if (f0androidwebkitWebSettings$TextSizeSwitchesValues != null) {
                return f0androidwebkitWebSettings$TextSizeSwitchesValues;
            }
            int[] iArr = new int[WebSettings.TextSize.values().length];
            try {
                iArr[WebSettings.TextSize.LARGER.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[WebSettings.TextSize.LARGEST.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[WebSettings.TextSize.NORMAL.ordinal()] = 5;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[WebSettings.TextSize.SMALLER.ordinal()] = 3;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[WebSettings.TextSize.SMALLEST.ordinal()] = 4;
            } catch (NoSuchFieldError e5) {
            }
            f0androidwebkitWebSettings$TextSizeSwitchesValues = iArr;
            return iArr;
        }

        @Override
        public void run() {
            DisplayMetrics metrics = BrowserSettings.this.mContext.getResources().getDisplayMetrics();
            BrowserSettings.this.mFontSizeMult = metrics.scaledDensity / metrics.density;
            if (ActivityManager.staticGetMemoryClass() > 16) {
                BrowserSettings.this.mPageCacheCapacity = 5;
            }
            BrowserSettings.this.mWebStorageSizeManager = new WebStorageSizeManager(BrowserSettings.this.mContext, new WebStorageSizeManager.StatFsDiskInfo(BrowserSettings.this.getAppCachePath()), new WebStorageSizeManager.WebKitAppCacheInfo(BrowserSettings.this.getAppCachePath()));
            BrowserSettings.this.mPrefs.registerOnSharedPreferenceChangeListener(BrowserSettings.this);
            if (Build.VERSION.CODENAME.equals("REL")) {
                BrowserSettings.this.setDebugEnabled(false);
            }
            if (BrowserSettings.this.mPrefs.contains("text_size")) {
                switch (m46getandroidwebkitWebSettings$TextSizeSwitchesValues()[BrowserSettings.this.getTextSize().ordinal()]) {
                    case 1:
                        BrowserSettings.this.setTextZoom(IBrowserFeatureIndexExt.CUSTOM_PREFERENCE_BANDWIDTH);
                        break;
                    case 2:
                        BrowserSettings.this.setTextZoom(200);
                        break;
                    case 3:
                        BrowserSettings.this.setTextZoom(75);
                        break;
                    case 4:
                        BrowserSettings.this.setTextZoom(50);
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

    public void startManagingSettings(WebSettings settings) {
        if (this.mNeedsSharedSync) {
            syncSharedSettings();
        }
        synchronized (this.mManagedSettings) {
            syncStaticSettings(settings);
            syncSetting(settings);
            this.mManagedSettings.add(new WeakReference<>(settings));
        }
    }

    public void stopManagingSettings(WebSettings settings) {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.stopManagingSettings()--->");
        }
        Iterator<WeakReference<WebSettings>> iter = this.mManagedSettings.iterator();
        while (iter.hasNext()) {
            WeakReference<WebSettings> ref = iter.next();
            if (ref.get() == settings) {
                iter.remove();
                return;
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

    private void syncSetting(WebSettings settings) {
        String pluginUA;
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.syncSetting()--->");
        }
        settings.setGeolocationEnabled(enableGeolocation());
        settings.setJavaScriptEnabled(enableJavascript());
        settings.setLightTouchEnabled(enableLightTouch());
        settings.setNavDump(enableNavDump());
        settings.setDefaultTextEncodingName(getDefaultTextEncoding());
        settings.setMinimumFontSize(getMinimumFontSize());
        settings.setMinimumLogicalFontSize(getMinimumFontSize());
        settings.setPluginState(getPluginState());
        settings.setTextZoom(getTextZoom());
        settings.setDoubleTapZoom(getDoubleTapZoom());
        settings.setLayoutAlgorithm(getLayoutAlgorithm());
        settings.setJavaScriptCanOpenWindowsAutomatically(blockPopupWindows() ? false : true);
        settings.setLoadsImagesAutomatically(loadImages());
        settings.setLoadWithOverviewMode(loadPageInOverviewMode());
        settings.setSavePassword(rememberPasswords());
        settings.setSaveFormData(saveFormdata());
        settings.setUseWideViewPort(isWideViewport());
        sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
        sBrowserSettingExt.setStandardFontFamily(settings, this.mPrefs);
        String ua = this.mCustomUserAgents.get(settings);
        if (ua != null) {
            settings.setUserAgentString(ua);
            return;
        }
        String operatorUA = CustomProperties.getString("browser", "UserAgent");
        if ((operatorUA == null || operatorUA.length() == 0) && (pluginUA = sBrowserSettingExt.getOperatorUA(settings.getUserAgentString())) != null && pluginUA.length() > 0) {
            operatorUA = pluginUA;
        }
        if (getUserAgent() == 0 && operatorUA != null) {
            settings.setUserAgentString(operatorUA);
        } else {
            settings.setUserAgentString(USER_AGENTS[getUserAgent()]);
        }
    }

    private void syncStaticSettings(WebSettings settings) {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.syncStaticSettings()--->");
        }
        settings.setDefaultFontSize(16);
        settings.setDefaultFixedFontSize(13);
        settings.setNeedInitialFocus(false);
        settings.setSupportMultipleWindows(true);
        settings.setEnableSmoothTransition(true);
        settings.setAllowContentAccess(true);
        settings.setAppCacheEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAppCacheMaxSize(getWebStorageSizeManager().getAppCacheMaxSize());
        settings.setAppCachePath(getAppCachePath());
        settings.setDatabasePath(this.mContext.getDir("databases", 0).getPath());
        settings.setGeolocationDatabasePath(this.mContext.getDir("geolocation", 0).getPath());
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setMixedContentMode(2);
    }

    private void syncSharedSettings() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.syncSharedSettings()--->");
        }
        this.mNeedsSharedSync = false;
        CookieManager.getInstance().setAcceptCookie(acceptCookies());
        if (this.mController == null) {
            return;
        }
        for (Tab tab : this.mController.getTabs()) {
            tab.setAcceptThirdPartyCookies(acceptCookies());
        }
        this.mController.setShouldShowErrorConsole(enableJavascriptConsole());
    }

    private void syncManagedSettings() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.syncManagedSettings()--->");
        }
        syncSharedSettings();
        synchronized (this.mManagedSettings) {
            Iterator<WeakReference<WebSettings>> iter = this.mManagedSettings.iterator();
            while (iter.hasNext()) {
                WeakReference<WebSettings> ref = iter.next();
                WebSettings settings = ref.get();
                if (settings == null) {
                    iter.remove();
                } else {
                    syncSetting(settings);
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        syncManagedSettings();
        if ("search_engine".equals(key)) {
            updateSearchEngine(false);
        }
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.onSharedPreferenceChanged()--->" + key + " mControll is null:" + (this.mController == null));
        }
        if (this.mController == null) {
            return;
        }
        if ("fullscreen".equals(key)) {
            if (this.mController == null || this.mController.getUi() == null) {
                return;
            }
            this.mController.getUi().setFullscreen(useFullscreen());
            return;
        }
        if ("enable_quick_controls".equals(key)) {
            if (this.mController == null || this.mController.getUi() == null) {
                return;
            }
            this.mController.getUi().setUseQuickControls(sharedPreferences.getBoolean(key, false));
            return;
        }
        if ("link_prefetch_when".equals(key)) {
            updateConnectionType();
        } else {
            if (!"landscape_only".equals(key)) {
                return;
            }
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
        WebSettings.LayoutAlgorithm autosize = Build.VERSION.SDK_INT >= 19 ? WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING : WebSettings.LayoutAlgorithm.NARROW_COLUMNS;
        if (autofitPages()) {
            layoutAlgorithm = autosize;
        }
        if (isDebugEnabled()) {
            if (isNormalLayout()) {
                WebSettings.LayoutAlgorithm layoutAlgorithm2 = WebSettings.LayoutAlgorithm.NORMAL;
                return layoutAlgorithm2;
            }
            WebSettings.LayoutAlgorithm layoutAlgorithm3 = autosize;
            return layoutAlgorithm3;
        }
        return layoutAlgorithm;
    }

    public WebStorageSizeManager getWebStorageSizeManager() {
        requireInitialization();
        return this.mWebStorageSizeManager;
    }

    public String getAppCachePath() {
        if (this.mAppCachePath == null) {
            this.mAppCachePath = this.mContext.getDir("appcache", 0).getPath();
        }
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.getAppCachePath()--->mAppCachePath:" + this.mAppCachePath);
        }
        return this.mAppCachePath;
    }

    private void updateSearchEngine(boolean force) {
        String searchEngineName = getSearchEngineName();
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.updateSearchEngine()--->searchEngineName:" + searchEngineName);
        }
        if (!force && this.mSearchEngine != null && searchEngineName != null && this.mSearchEngine.getName().equals(searchEngineName)) {
            return;
        }
        this.mSearchEngine = SearchEngines.get(this.mContext, searchEngineName);
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

    public void setDebugEnabled(boolean value) {
        SharedPreferences.Editor edit = this.mPrefs.edit();
        edit.putBoolean("debug_menu", value);
        if (!value) {
            edit.putBoolean("enable_hardware_accel_skia", false);
        }
        edit.apply();
    }

    public void clearCache() {
        WebView current;
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearCache()--->");
        }
        WebIconDatabase.getInstance().removeAllIcons();
        if (this.mController == null || (current = this.mController.getCurrentWebView()) == null) {
            return;
        }
        current.clearCache(true);
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
        ContentResolver resolver = this.mContext.getContentResolver();
        BrowserHistoryPage.ClearHistoryTask clear = new BrowserHistoryPage.ClearHistoryTask(resolver);
        if (clear.isAlive()) {
            return;
        }
        clear.start();
    }

    public void clearFormData() {
        WebView currentTopView;
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearFormData()--->");
        }
        WebViewDatabase.getInstance(this.mContext).clearFormData();
        if (this.mController == null || (currentTopView = this.mController.getCurrentTopWebView()) == null) {
            return;
        }
        currentTopView.clearFormData();
    }

    public void clearPasswords() {
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.clearPasswords()--->");
        }
        WebViewDatabase db = WebViewDatabase.getInstance(this.mContext);
        db.clearUsernamePassword();
        db.clearHttpAuthUsernamePassword();
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
        long gal = this.mPrefs.getLong("last_autologin_time", -1L);
        this.mPrefs.edit().clear().putLong("last_autologin_time", gal).apply();
        resetCachedValues();
        syncManagedSettings();
    }

    private void resetCachedValues() {
        updateSearchEngine(false);
    }

    public void toggleDebugSettings() {
        setDebugEnabled(!isDebugEnabled());
    }

    public boolean hasDesktopUseragent(WebView view) {
        return (view == null || this.mCustomUserAgents.get(view.getSettings()) == null) ? false : true;
    }

    public boolean isDesktopUserAgent(WebView view) {
        WebSettings settings = view.getSettings();
        String ua = settings.getUserAgentString();
        if (ua != null) {
            return ua.equals("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.34 Safari/534.24");
        }
        return false;
    }

    public void changeUserAgent(WebView view, boolean isDesktop) {
        if (view == null) {
            return;
        }
        WebSettings settings = view.getSettings();
        if (!isDesktop) {
            Log.i("Browser/Settings", "UA restore");
            if (this.mCustomUserAgents.get(settings) != null) {
                return;
            }
            String operatorUA = CustomProperties.getString("browser", "UserAgent");
            if (operatorUA == null || operatorUA.length() == 0) {
                sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
                String pluginUA = sBrowserSettingExt.getOperatorUA(settings.getUserAgentString());
                if (pluginUA != null && pluginUA.length() > 0) {
                    operatorUA = pluginUA;
                }
            }
            if (getUserAgent() == 0 && operatorUA != null) {
                settings.setUserAgentString(operatorUA);
                return;
            } else {
                settings.setUserAgentString(USER_AGENTS[getUserAgent()]);
                return;
            }
        }
        Log.i("Browser/Settings", "UA change to desktop");
        settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.34 Safari/534.24");
    }

    public void toggleDesktopUseragent(WebView view) {
        if (view == null) {
            return;
        }
        WebSettings settings = view.getSettings();
        if (this.mCustomUserAgents.get(settings) != null) {
            this.mCustomUserAgents.remove(settings);
            settings.setUserAgentString(USER_AGENTS[getUserAgent()]);
        } else {
            this.mCustomUserAgents.put(settings, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.34 Safari/534.24");
            settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.34 Safari/534.24");
        }
    }

    public static int getAdjustedMinimumFontSize(int rawValue) {
        int rawValue2 = rawValue + 1;
        if (rawValue2 > 1) {
            return rawValue2 + 3;
        }
        return rawValue2;
    }

    public int getAdjustedTextZoom(int rawValue) {
        return (int) ((((rawValue - 10) * 5) + 100) * this.mFontSizeMult);
    }

    static int getRawTextZoom(int percent) {
        return ((percent - 100) / 5) + 10;
    }

    public int getAdjustedDoubleTapZoom(int rawValue) {
        return (int) ((((rawValue - 5) * 5) + 100) * this.mFontSizeMult);
    }

    public SharedPreferences getPreferences() {
        return this.mPrefs;
    }

    public void updateConnectionType() {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        String linkPrefetchPreference = getLinkPrefetchEnabled();
        boolean linkPrefetchAllowed = linkPrefetchPreference.equals(getLinkPrefetchAlwaysPreferenceString(this.mContext));
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null) {
            switch (ni.getType()) {
                case 1:
                case 7:
                case 9:
                    linkPrefetchAllowed |= linkPrefetchPreference.equals(getLinkPrefetchOnWifiOnlyPreferenceString(this.mContext));
                    break;
            }
        }
        if (this.mLinkPrefetchAllowed == linkPrefetchAllowed) {
            return;
        }
        this.mLinkPrefetchAllowed = linkPrefetchAllowed;
        syncManagedSettings();
    }

    @Deprecated
    public WebSettings.TextSize getTextSize() {
        String textSize = this.mPrefs.getString("text_size", "NORMAL");
        return WebSettings.TextSize.valueOf(textSize);
    }

    public int getMinimumFontSize() {
        int minFont = this.mPrefs.getInt("min_font_size", 0);
        return getAdjustedMinimumFontSize(minFont);
    }

    public int getTextZoom() {
        requireInitialization();
        int textZoom = this.mPrefs.getInt("text_zoom", 10);
        return getAdjustedTextZoom(textZoom);
    }

    public void setTextZoom(int percent) {
        this.mPrefs.edit().putInt("text_zoom", getRawTextZoom(percent)).apply();
    }

    public int getDoubleTapZoom() {
        requireInitialization();
        int doubleTapZoom = this.mPrefs.getInt("double_tap_zoom", 5);
        return getAdjustedDoubleTapZoom(doubleTapZoom);
    }

    public String getSearchEngineName() {
        String searchEngineFavicon;
        SearchEngineManager searchEngineManager = (SearchEngineManager) this.mContext.getSystemService("search_engine");
        List<com.mediatek.common.search.SearchEngine> searchEngines = searchEngineManager.getAvailables();
        if (searchEngines == null || searchEngines.size() <= 0) {
            return null;
        }
        String defaultSearchEngine = "google";
        com.mediatek.common.search.SearchEngine searchEngine = searchEngineManager.getDefault();
        if (searchEngine != null) {
            defaultSearchEngine = searchEngine.getName();
        }
        sBrowserSettingExt = Extensions.getSettingPlugin(this.mContext);
        String searchEngineName = sBrowserSettingExt.getSearchEngine(this.mPrefs, this.mContext);
        com.mediatek.common.search.SearchEngine searchEngine2 = searchEngineManager.getByName(searchEngineName);
        if (searchEngine2 != null) {
            searchEngineFavicon = searchEngine2.getFaviconUri();
        } else {
            searchEngineFavicon = this.mPrefs.getString("search_engine_favicon", "");
        }
        int selectedItem = -1;
        boolean need_refresh = false;
        int len = searchEngines.size();
        String[] entryValues = new String[len];
        String[] entryFavicon = new String[len];
        com.mediatek.common.search.SearchEngine searchEngine3 = searchEngineManager.getBestMatch("", searchEngineFavicon);
        if (searchEngine3 != null && !searchEngineName.equals(searchEngine3.getName())) {
            searchEngineName = searchEngine3.getName();
            need_refresh = true;
        }
        for (int i = 0; i < len; i++) {
            entryValues[i] = searchEngines.get(i).getName();
            entryFavicon[i] = searchEngines.get(i).getFaviconUri();
            if (entryValues[i].equals(searchEngineName)) {
                selectedItem = i;
            }
        }
        if (selectedItem == -1) {
            selectedItem = 0;
            for (int i2 = 0; i2 < len; i2++) {
                if (entryValues[i2].equals(defaultSearchEngine)) {
                    selectedItem = i2;
                }
            }
            need_refresh = true;
        }
        if (DEBUG) {
            Log.d("browser", "BrowserSettings.getSearchEngineName-->selectedItem = " + selectedItem + "entryValues[" + selectedItem + "]=" + entryValues);
        }
        if (need_refresh && selectedItem != -1) {
            SharedPreferences.Editor editor = this.mPrefs.edit();
            editor.putString("search_engine", entryValues[selectedItem]);
            editor.putString("search_engine_favicon", entryFavicon[selectedItem]);
            editor.commit();
        }
        return entryValues[selectedItem];
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
        String state = this.mPrefs.getString("plugin_state", "ON");
        return WebSettings.PluginState.valueOf(state);
    }

    public boolean loadPageInOverviewMode() {
        boolean loadMode = this.mPrefs.getBoolean("load_page", true);
        Log.i("Browser/Settings", "loadMode: " + loadMode);
        return loadMode;
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

    public String getDefaultTextEncoding() {
        String encoding = this.mPrefs.getString("default_text_encoding", null);
        if (TextUtils.isEmpty(encoding)) {
            return this.mContext.getString(R.string.pref_default_text_encoding_default_choice);
        }
        return encoding;
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

    public void setHomePage(String value) {
        this.mPrefs.edit().putString("homepage", value).apply();
        Log.i("Browser/Settings", "BrowserSettings: setHomePage : " + value);
    }

    public void setHomePagePicker(String value) {
        this.mPrefs.edit().putString("homepage_picker", value).apply();
        Log.i("Browser/Settings", "BrowserSettings: setHomePagePicker : " + value);
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
        String preload = Settings.Secure.getString(this.mContext.getContentResolver(), "browser_default_preload_setting");
        if (preload == null) {
            return this.mContext.getResources().getString(R.string.pref_data_preload_default_value);
        }
        return preload;
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
        String preload = Settings.Secure.getString(this.mContext.getContentResolver(), "browser_default_link_prefetch_setting");
        if (preload == null) {
            return this.mContext.getResources().getString(R.string.pref_link_prefetch_default_value);
        }
        return preload;
    }

    public String getLinkPrefetchEnabled() {
        return this.mPrefs.getString("link_prefetch_when", getDefaultLinkPrefetchSetting());
    }

    public long getLastRecovered() {
        return this.mPrefs.getLong("last_recovered", 0L);
    }

    public void setLastRecovered(long time) {
        this.mPrefs.edit().putLong("last_recovered", time).apply();
    }

    public boolean wasLastRunPaused() {
        return this.mPrefs.getBoolean("last_paused", false);
    }

    public void setLastRunPaused(boolean isPaused) {
        this.mPrefs.edit().putBoolean("last_paused", isPaused).apply();
    }

    public void onConfigurationChanged(Configuration config) {
        updateSearchEngine(false);
    }

    public void updateSearchEngineSetting() {
        IBrowserRegionalPhoneExt browserRegionalPhone = Extensions.getRegionalPhonePlugin(this.mContext);
        String searchEngineName = browserRegionalPhone.getSearchEngine(this.mPrefs, this.mContext);
        if (searchEngineName == null) {
            Log.i("Browser/Settings", "updateSearchEngineSetting ---no change");
            return;
        }
        SearchEngineManager searchEngineManager = (SearchEngineManager) this.mContext.getSystemService("search_engine");
        com.mediatek.common.search.SearchEngine searchEngineInfo = searchEngineManager.getByName(searchEngineName);
        if (searchEngineInfo == null) {
            Log.i("Browser/Settings", "updateSearchEngineSetting ---" + searchEngineName + " not found");
            return;
        }
        String searchEngineFavicon = searchEngineInfo.getFaviconUri();
        SharedPreferences.Editor editor = this.mPrefs.edit();
        editor.putString("search_engine", searchEngineName);
        editor.putString("search_engine_favicon", searchEngineFavicon);
        editor.commit();
        Log.i("Browser/Settings", "updateSearchEngineSetting --" + searchEngineName + "--" + searchEngineFavicon);
    }
}
