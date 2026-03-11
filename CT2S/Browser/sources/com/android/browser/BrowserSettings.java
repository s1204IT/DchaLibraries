package com.android.browser;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import com.android.browser.WebStorageSizeManager;
import com.android.browser.provider.BrowserProvider;
import com.android.browser.search.SearchEngine;
import com.android.browser.search.SearchEngines;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
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
    private static boolean sInitialized = false;
    private boolean mNeedsSharedSync = true;
    private float mFontSizeMult = 1.0f;
    private boolean mLinkPrefetchAllowed = true;
    private int mPageCacheCapacity = 1;
    private Runnable mSetup = new Runnable() {
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
            String unused = BrowserSettings.sFactoryResetUrl = BrowserSettings.this.mContext.getResources().getString(R.string.homepage_base);
            if (BrowserSettings.sFactoryResetUrl.indexOf("{CID}") != -1) {
                String unused2 = BrowserSettings.sFactoryResetUrl = BrowserSettings.sFactoryResetUrl.replace("{CID}", BrowserProvider.getClientId(BrowserSettings.this.mContext.getContentResolver()));
            }
            synchronized (BrowserSettings.class) {
                boolean unused3 = BrowserSettings.sInitialized = true;
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
        Iterator<WeakReference<WebSettings>> iter = this.mManagedSettings.iterator();
        while (iter.hasNext()) {
            WeakReference<WebSettings> ref = iter.next();
            if (ref.get() == settings) {
                iter.remove();
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
        settings.setGeolocationEnabled(enableGeolocation());
        settings.setJavaScriptEnabled(enableJavascript());
        settings.setLightTouchEnabled(enableLightTouch());
        settings.setNavDump(enableNavDump());
        settings.setDefaultTextEncodingName(getDefaultTextEncoding());
        settings.setDefaultZoom(getDefaultZoom());
        settings.setMinimumFontSize(getMinimumFontSize());
        settings.setMinimumLogicalFontSize(getMinimumFontSize());
        settings.setPluginState(getPluginState());
        settings.setTextZoom(getTextZoom());
        settings.setLayoutAlgorithm(getLayoutAlgorithm());
        settings.setJavaScriptCanOpenWindowsAutomatically(!blockPopupWindows());
        settings.setLoadsImagesAutomatically(loadImages());
        settings.setLoadWithOverviewMode(loadPageInOverviewMode());
        settings.setSavePassword(rememberPasswords());
        settings.setSaveFormData(saveFormdata());
        settings.setUseWideViewPort(isWideViewport());
        String ua = this.mCustomUserAgents.get(settings);
        if (ua != null) {
            settings.setUserAgentString(ua);
        } else {
            settings.setUserAgentString(USER_AGENTS[getUserAgent()]);
        }
        String str = "head&" + (useInvertedRendering() ? "1" : "0");
        settings.setStandardFontFamily((str + "#") + Float.toString(getInvertedContrast()));
    }

    private void syncStaticSettings(WebSettings settings) {
        settings.setDefaultFontSize(16);
        settings.setDefaultFixedFontSize(13);
        settings.setNeedInitialFocus(false);
        settings.setSupportMultipleWindows(true);
        settings.setEnableSmoothTransition(true);
        settings.setAllowContentAccess(false);
        settings.setAppCacheEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAppCacheMaxSize(getWebStorageSizeManager().getAppCacheMaxSize());
        settings.setAppCachePath(getAppCachePath());
        settings.setDatabasePath(this.mContext.getDir("databases", 0).getPath());
        settings.setGeolocationDatabasePath(this.mContext.getDir("geolocation", 0).getPath());
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
    }

    private void syncSharedSettings() {
        this.mNeedsSharedSync = false;
        CookieManager.getInstance().setAcceptCookie(acceptCookies());
        if (this.mController != null) {
            for (Tab tab : this.mController.getTabs()) {
                tab.setAcceptThirdPartyCookies(acceptCookies());
            }
            this.mController.setShouldShowErrorConsole(enableJavascriptConsole());
        }
    }

    private void syncManagedSettings() {
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
            return;
        }
        if ("fullscreen".equals(key)) {
            if (this.mController != null && this.mController.getUi() != null) {
                this.mController.getUi().setFullscreen(useFullscreen());
                return;
            }
            return;
        }
        if ("enable_quick_controls".equals(key)) {
            if (this.mController != null && this.mController.getUi() != null) {
                this.mController.getUi().setUseQuickControls(sharedPreferences.getBoolean(key, false));
                return;
            }
            return;
        }
        if ("link_prefetch_when".equals(key)) {
            updateConnectionType();
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
        return this.mAppCachePath;
    }

    private void updateSearchEngine(boolean force) {
        String searchEngineName = getSearchEngineName();
        if (force || this.mSearchEngine == null || !this.mSearchEngine.getName().equals(searchEngineName)) {
            if (this.mSearchEngine != null) {
                this.mSearchEngine.close();
            }
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
        WebIconDatabase.getInstance().removeAllIcons();
        if (this.mController != null && (current = this.mController.getCurrentWebView()) != null) {
            current.clearCache(true);
        }
    }

    public void clearCookies() {
        CookieManager.getInstance().removeAllCookie();
    }

    public void clearHistory() {
        ContentResolver resolver = this.mContext.getContentResolver();
        android.provider.Browser.clearHistory(resolver);
        android.provider.Browser.clearSearches(resolver);
    }

    public void clearFormData() {
        WebView currentTopView;
        WebViewDatabase.getInstance(this.mContext).clearFormData();
        if (this.mController != null && (currentTopView = this.mController.getCurrentTopWebView()) != null) {
            currentTopView.clearFormData();
        }
    }

    public void clearPasswords() {
        WebViewDatabase db = WebViewDatabase.getInstance(this.mContext);
        db.clearUsernamePassword();
        db.clearHttpAuthUsernamePassword();
    }

    public void clearDatabases() {
        WebStorage.getInstance().deleteAllData();
    }

    public void clearLocationAccess() {
        GeolocationPermissions.getInstance().clearAll();
    }

    public void resetDefaultPreferences() {
        long gal = this.mPrefs.getLong("last_autologin_time", -1L);
        this.mPrefs.edit().clear().putLong("last_autologin_time", gal).apply();
        resetCachedValues();
        syncManagedSettings();
    }

    private void resetCachedValues() {
        updateSearchEngine(false);
    }

    public boolean hasDesktopUseragent(WebView view) {
        return (view == null || this.mCustomUserAgents.get(view.getSettings()) == null) ? false : true;
    }

    public void toggleDesktopUseragent(WebView view) {
        if (view != null) {
            WebSettings settings = view.getSettings();
            if (this.mCustomUserAgents.get(settings) != null) {
                this.mCustomUserAgents.remove(settings);
                settings.setUserAgentString(USER_AGENTS[getUserAgent()]);
            } else {
                this.mCustomUserAgents.put(settings, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.34 Safari/534.24");
                settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.24 (KHTML, like Gecko) Chrome/11.0.696.34 Safari/534.24");
            }
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
        if (this.mLinkPrefetchAllowed != linkPrefetchAllowed) {
            this.mLinkPrefetchAllowed = linkPrefetchAllowed;
            syncManagedSettings();
        }
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
        return this.mPrefs.getString("search_engine", "google");
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

    public WebSettings.ZoomDensity getDefaultZoom() {
        String zoom = this.mPrefs.getString("default_zoom", "MEDIUM");
        return WebSettings.ZoomDensity.valueOf(zoom);
    }

    public boolean loadPageInOverviewMode() {
        return this.mPrefs.getBoolean("load_page", true);
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
        return this.mPrefs.getString("default_text_encoding", null);
    }

    public String getHomePage() {
        return this.mPrefs.getString("homepage", getFactoryResetHomeUrl(this.mContext));
    }

    public void setHomePage(String value) {
        this.mPrefs.edit().putString("homepage", value).apply();
    }

    public String getDownloadDir() {
        return this.mPrefs.getString("download", "Download");
    }

    public boolean isHardwareAccelerated() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("enable_hardware_accel", true);
        }
        return true;
    }

    public int getUserAgent() {
        if (isDebugEnabled()) {
            return Integer.parseInt(this.mPrefs.getString("user_agent", "0"));
        }
        return 0;
    }

    public boolean enableJavascriptConsole() {
        if (isDebugEnabled()) {
            return this.mPrefs.getBoolean("javascript_console", true);
        }
        return false;
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
        return !isDebugEnabled() ? "" : this.mPrefs.getString("js_engine_flags", "");
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

    public boolean useInvertedRendering() {
        return this.mPrefs.getBoolean("inverted", false);
    }

    public float getInvertedContrast() {
        return 1.0f + (this.mPrefs.getInt("inverted_contrast", 0) / 10.0f);
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
}
