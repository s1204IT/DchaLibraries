package com.android.browser;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.webkit.CookieManager;
import com.android.browser.UI;
import com.android.browser.search.SearchEngine;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class IntentHandler {
    private static final boolean DEBUG = Browser.DEBUG;
    static final UrlData EMPTY_URL_DATA = new UrlData(null);
    private static final String[] SCHEME_WHITELIST = {"http", "https", "about", "file", "rtsp", "tel"};
    private static final String[] URI_WHITELIST = {"content://com.android.browser.site_navigation/websites", "content://com.android.browser.home/"};
    private Activity mActivity;
    private Controller mController;
    private BrowserSettings mSettings;
    private TabControl mTabControl;

    public IntentHandler(Activity browser, Controller controller) {
        this.mActivity = browser;
        this.mController = controller;
        this.mTabControl = this.mController.getTabControl();
        this.mSettings = controller.getSettings();
    }

    void onNewIntent(Intent intent) {
        Tab appTab;
        Tab appTab2;
        String urlCookie;
        Uri uri = intent.getData();
        int dcha_state = BenesseExtension.getDchaState();
        if (uri != null && isForbiddenUri(uri)) {
            Log.e("browser", "Aborting intent with forbidden uri, \"" + uri + "\"");
            return;
        }
        if (DEBUG) {
            Log.d("browser", "IntentHandler.onNewIntent--->" + intent);
        }
        Tab current = this.mTabControl.getCurrentTab();
        if (current == null) {
            current = this.mTabControl.getTab(0);
            if (current == null) {
                return;
            } else {
                this.mController.setActiveTab(current);
            }
        }
        String action = intent.getAction();
        if (DEBUG) {
            Log.d("browser", "IntentHandler.onNewIntent--->action: " + action);
        }
        int flags = intent.getFlags();
        if ("android.intent.action.MAIN".equals(action) || (1048576 & flags) != 0) {
            return;
        }
        if ("show_bookmarks".equals(action)) {
            if (dcha_state == 0) {
                this.mController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
                return;
            }
            return;
        }
        ((SearchManager) this.mActivity.getSystemService("search")).stopSearch();
        if ("android.intent.action.VIEW".equals(action) || "android.nfc.action.NDEF_DISCOVERED".equals(action) || "android.intent.action.SEARCH".equals(action) || "android.intent.action.MEDIA_SEARCH".equals(action) || "android.intent.action.WEB_SEARCH".equals(action)) {
            if (uri != null && (urlCookie = CookieManager.getInstance().getCookie(uri.toString())) != null) {
                intent.putExtra("url-cookie", urlCookie);
            }
            if (uri != null && (uri.toString().startsWith("rtsp://") || uri.toString().startsWith("tel:"))) {
                intent.setData(Uri.parse(uri.toString().replaceAll(" ", "%20")));
                if (uri.toString().startsWith("rtsp://")) {
                    intent.addFlags(268435456);
                }
                if (dcha_state == 0) {
                    this.mActivity.startActivity(intent);
                    return;
                }
                return;
            }
            if (handleWebSearchIntent(this.mActivity, this.mController, intent)) {
                return;
            }
            UrlData urlData = getUrlDataFromIntent(intent);
            if (urlData.isEmpty()) {
                urlData = new UrlData(this.mSettings.getHomePage());
            }
            if (intent.getBooleanExtra("create_new_tab", false) || urlData.isPreloaded()) {
                if (dcha_state == 0) {
                    this.mController.openTab(urlData);
                    return;
                }
                return;
            }
            String appId = intent.getStringExtra("com.android.browser.application_id");
            if (DEBUG) {
                Log.d("browser", "IntentHandler.onNewIntent--->appId: " + appId);
            }
            if ("android.intent.action.VIEW".equals(action) && appId != null && appId.startsWith(this.mActivity.getPackageName()) && (appTab2 = this.mTabControl.getTabFromAppId(appId)) != null && appTab2 == this.mController.getCurrentTab()) {
                this.mController.switchToTab(appTab2);
                if (dcha_state == 0) {
                    this.mController.loadUrlDataIn(appTab2, urlData);
                    return;
                }
                return;
            }
            if (!"android.intent.action.VIEW".equals(action) || this.mActivity.getPackageName().equals(appId)) {
                if (urlData.isEmpty() || !urlData.mUrl.startsWith("about:debug")) {
                    this.mController.dismissSubWindow(current);
                    current.setAppId(null);
                    if (dcha_state == 0) {
                        this.mController.loadUrlDataIn(current, urlData);
                        return;
                    }
                    return;
                }
                if ("about:debug.dumpmem".equals(urlData.mUrl)) {
                    new OutputMemoryInfo().execute(this.mTabControl, null);
                    return;
                } else if ("about:debug.dumpmem.file".equals(urlData.mUrl)) {
                    new OutputMemoryInfo().execute(this.mTabControl, this.mTabControl);
                    return;
                } else {
                    this.mSettings.toggleDebugSettings();
                    return;
                }
            }
            if (!BrowserActivity.isTablet(this.mActivity) && !this.mSettings.allowAppTabs() && (appTab = this.mTabControl.getTabFromAppId(appId)) != null) {
                this.mController.reuseTab(appTab, urlData);
                return;
            }
            if (DEBUG) {
                Log.d("browser", "IntentHandler.onNewIntent--->urlData.mUrl: " + urlData.mUrl);
            }
            Tab appTab3 = this.mTabControl.findTabWithUrl(urlData.mUrl);
            if (appTab3 != null) {
                appTab3.setAppId(appId);
                if (current != appTab3) {
                    this.mController.switchToTab(appTab3);
                }
                if (dcha_state == 0) {
                    this.mController.loadUrlDataIn(appTab3, urlData);
                    return;
                }
                return;
            }
            Tab tab = dcha_state == 0 ? this.mController.openTab(urlData) : null;
            if (tab != null) {
                tab.setAppId(appId);
                if ((intent.getFlags() & 4194304) != 0) {
                    tab.setCloseOnBack(true);
                }
            }
        }
    }

    protected static UrlData getUrlDataFromIntent(Intent intent) {
        Bundle pairs;
        String url = "";
        Map<String, String> headers = null;
        PreloadedTabControl preloaded = null;
        String preloadedSearchBoxQuery = null;
        if (intent != null && (intent.getFlags() & 1048576) == 0) {
            String action = intent.getAction();
            if ("android.intent.action.VIEW".equals(action) || "android.nfc.action.NDEF_DISCOVERED".equals(action)) {
                Uri data = intent.getData();
                String originalUrl = null;
                if (data != null) {
                    originalUrl = data.toString();
                }
                if (originalUrl != null && !originalUrl.startsWith("content://")) {
                    url = UrlUtils.smartUrlFilter(intent.getData());
                } else {
                    url = originalUrl;
                }
                if (url != null && url.startsWith("http") && (pairs = intent.getBundleExtra("com.android.browser.headers")) != null && !pairs.isEmpty()) {
                    headers = new HashMap<>();
                    for (String key : pairs.keySet()) {
                        headers.put(key, pairs.getString(key));
                    }
                }
                if (intent.hasExtra("preload_id")) {
                    String id = intent.getStringExtra("preload_id");
                    preloadedSearchBoxQuery = intent.getStringExtra("searchbox_query");
                    preloaded = Preloader.getInstance().getPreloadedTab(id);
                }
            } else if (("android.intent.action.SEARCH".equals(action) || "android.intent.action.MEDIA_SEARCH".equals(action) || "android.intent.action.WEB_SEARCH".equals(action)) && (url = intent.getStringExtra("query")) != null) {
                url = UrlUtils.smartUrlFilter(UrlUtils.fixUrl(url));
                if (url.contains("&source=android-browser-suggest&")) {
                    String source = null;
                    Bundle appData = intent.getBundleExtra("app_data");
                    if (appData != null) {
                        source = appData.getString("source");
                    }
                    if (TextUtils.isEmpty(source)) {
                        source = "unknown";
                    }
                    url = url.replace("&source=android-browser-suggest&", "&source=android-" + source + "&");
                }
            }
        }
        if (DEBUG) {
            Log.d("browser", "IntentHandler.getUrlDataFromIntent----->url : " + url + " headers: " + headers);
        }
        return new UrlData(url, headers, intent, preloaded, preloadedSearchBoxQuery);
    }

    static boolean handleWebSearchIntent(Activity activity, Controller controller, Intent intent) {
        if (DEBUG) {
            Log.d("browser", "IntentHandler.handleWebSearchIntent()----->" + intent);
        }
        if (intent == null) {
            return false;
        }
        String action = intent.getAction();
        if (DEBUG) {
            Log.d("browser", "IntentHandler.handleWebSearchIntent()----->action : " + action);
        }
        if ("android.intent.action.VIEW".equals(action)) {
            Uri data = intent.getData();
            url = data != null ? data.toString() : null;
            if (url != null && url.startsWith("content://")) {
                return false;
            }
            if (controller != null && intent.getBooleanExtra("inputUrl", false)) {
                BaseUi ui = (BaseUi) controller.getUi();
                ui.setInputUrlFlag(true);
                Log.d("browser", "handleWebSearchIntent inputUrl setInputUrlFlag");
            }
        } else if ("android.intent.action.SEARCH".equals(action) || "android.intent.action.MEDIA_SEARCH".equals(action) || "android.intent.action.WEB_SEARCH".equals(action)) {
            url = intent.getStringExtra("query");
        }
        if (DEBUG) {
            Log.d("browser", "IntentHandler.handleWebSearchIntent()----->url : " + url);
        }
        return handleWebSearchRequest(activity, controller, url, intent.getBundleExtra("app_data"), intent.getStringExtra("intent_extra_data_key"));
    }

    private static boolean handleWebSearchRequest(Activity activity, Controller controller, String inUrl, Bundle appData, String extraData) {
        if (DEBUG) {
            Log.d("browser", "IntentHandler.handleWebSearchRequest()----->" + inUrl);
        }
        if (inUrl == null) {
            return false;
        }
        if (DEBUG) {
            Log.d("browser", "IntentHandler.handleWebSearchRequest()----->inUrl : " + inUrl + " extraData : " + extraData);
        }
        final String url = UrlUtils.fixUrl(inUrl).trim();
        if (TextUtils.isEmpty(url) || Patterns.WEB_URL.matcher(url).matches() || UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url).matches()) {
            return false;
        }
        final ContentResolver cr = activity.getContentResolver();
        if (DEBUG) {
            Log.d("browser", "IntentHandler.handleWebSearchRequest()----->newUrl : " + url);
        }
        if (controller == null || controller.getTabControl() == null || controller.getTabControl().getCurrentWebView() == null || !controller.getTabControl().getCurrentWebView().isPrivateBrowsingEnabled()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                public Void doInBackground(Void... unused) {
                    com.android.browser.provider.Browser.addSearchUrl(cr, url);
                    return null;
                }
            }.execute(new Void[0]);
        }
        SearchEngine searchEngine = BrowserSettings.getInstance().getSearchEngine();
        if (searchEngine == null) {
            return false;
        }
        searchEngine.startSearch(activity, url, appData, extraData);
        return true;
    }

    private static boolean isForbiddenUri(Uri uri) {
        for (String allowed : URI_WHITELIST) {
            if (allowed.equals(uri.toString())) {
                return false;
            }
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        String scheme2 = scheme.toLowerCase(Locale.US);
        for (String allowed2 : SCHEME_WHITELIST) {
            if (allowed2.equals(scheme2)) {
                return false;
            }
        }
        return true;
    }

    static class UrlData {
        final boolean mDisableUrlOverride;
        final Map<String, String> mHeaders;
        final PreloadedTabControl mPreloadedTab;
        final String mSearchBoxQueryToSubmit;
        final String mUrl;

        UrlData(String url) {
            this.mUrl = url;
            this.mHeaders = null;
            this.mPreloadedTab = null;
            this.mSearchBoxQueryToSubmit = null;
            this.mDisableUrlOverride = false;
        }

        UrlData(String url, Map<String, String> headers, Intent intent, PreloadedTabControl preloaded, String searchBoxQueryToSubmit) {
            this.mUrl = url;
            this.mHeaders = headers;
            this.mPreloadedTab = preloaded;
            this.mSearchBoxQueryToSubmit = searchBoxQueryToSubmit;
            if (intent != null) {
                this.mDisableUrlOverride = intent.getBooleanExtra("disable_url_override", false);
            } else {
                this.mDisableUrlOverride = false;
            }
        }

        boolean isEmpty() {
            return this.mUrl == null || this.mUrl.length() == 0;
        }

        boolean isPreloaded() {
            return this.mPreloadedTab != null;
        }

        PreloadedTabControl getPreloadedTab() {
            return this.mPreloadedTab;
        }

        String getSearchBoxQueryToSubmit() {
            return this.mSearchBoxQueryToSubmit;
        }
    }
}
