package com.android.browser;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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

    public IntentHandler(Activity activity, Controller controller) {
        this.mActivity = activity;
        this.mController = controller;
        this.mTabControl = this.mController.getTabControl();
        this.mSettings = controller.getSettings();
    }

    void onNewIntent(Intent intent) {
        Tab tabFromAppId;
        Tab tabFromAppId2;
        String cookie;
        Uri data = intent.getData();
        if (data != null && isForbiddenUri(data)) {
            if (DEBUG) {
                Log.e("browser", "Aborting intent with forbidden uri, \"" + data + "\"");
                return;
            }
            return;
        }
        if (DEBUG) {
            Log.d("browser", "IntentHandler.onNewIntent--->" + intent);
        }
        Tab currentTab = this.mTabControl.getCurrentTab();
        if (currentTab == null) {
            currentTab = this.mTabControl.getTab(0);
            if (currentTab == null) {
                return;
            } else {
                this.mController.setActiveTab(currentTab);
            }
        }
        String action = intent.getAction();
        if (DEBUG) {
            Log.d("browser", "IntentHandler.onNewIntent--->action: " + action);
        }
        int flags = intent.getFlags();
        if ("android.intent.action.MAIN".equals(action) || (flags & 1048576) != 0) {
            return;
        }
        if ("show_bookmarks".equals(action)) {
            this.mController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
            return;
        }
        ((SearchManager) this.mActivity.getSystemService("search")).stopSearch();
        if ("android.intent.action.VIEW".equals(action) || "android.nfc.action.NDEF_DISCOVERED".equals(action) || "android.intent.action.SEARCH".equals(action) || "android.intent.action.MEDIA_SEARCH".equals(action) || "android.intent.action.WEB_SEARCH".equals(action)) {
            if (data != null && (cookie = CookieManager.getInstance().getCookie(data.toString())) != null) {
                intent.putExtra("url-cookie", cookie);
            }
            if (data != null && (data.toString().startsWith("rtsp://") || data.toString().startsWith("tel:"))) {
                intent.setData(Uri.parse(data.toString().replaceAll(" ", "%20")));
                if (data.toString().startsWith("rtsp://")) {
                    intent.addFlags(268435456);
                }
                this.mActivity.startActivity(intent);
                return;
            }
            if (handleWebSearchIntent(this.mActivity, this.mController, intent)) {
                return;
            }
            UrlData urlDataFromIntent = getUrlDataFromIntent(intent);
            if (urlDataFromIntent.isEmpty()) {
                urlDataFromIntent = new UrlData(this.mSettings.getHomePage());
            }
            if (intent.getBooleanExtra("create_new_tab", false) || urlDataFromIntent.isPreloaded()) {
                this.mController.openTab(urlDataFromIntent);
                return;
            }
            String stringExtra = intent.getStringExtra("com.android.browser.application_id");
            if (DEBUG) {
                Log.d("browser", "IntentHandler.onNewIntent--->appId: " + stringExtra);
            }
            if ("android.intent.action.VIEW".equals(action) && stringExtra != null && stringExtra.startsWith(this.mActivity.getPackageName()) && (tabFromAppId2 = this.mTabControl.getTabFromAppId(stringExtra)) != null && tabFromAppId2 == this.mController.getCurrentTab()) {
                this.mController.switchToTab(tabFromAppId2);
                this.mController.loadUrlDataIn(tabFromAppId2, urlDataFromIntent);
                return;
            }
            if ("android.intent.action.VIEW".equals(action) && !this.mActivity.getPackageName().equals(stringExtra)) {
                if (!BrowserActivity.isTablet(this.mActivity) && !this.mSettings.allowAppTabs() && (tabFromAppId = this.mTabControl.getTabFromAppId(stringExtra)) != null) {
                    this.mController.reuseTab(tabFromAppId, urlDataFromIntent);
                    return;
                }
                if (DEBUG) {
                    Log.d("browser", "IntentHandler.onNewIntent--->urlData.mUrl: " + urlDataFromIntent.mUrl);
                }
                Tab tabFindTabWithUrl = this.mTabControl.findTabWithUrl(urlDataFromIntent.mUrl);
                if (tabFindTabWithUrl != null) {
                    tabFindTabWithUrl.setAppId(stringExtra);
                    if (currentTab != tabFindTabWithUrl) {
                        this.mController.switchToTab(tabFindTabWithUrl);
                    }
                    this.mController.loadUrlDataIn(tabFindTabWithUrl, urlDataFromIntent);
                    return;
                }
                Tab tabOpenTab = this.mController.openTab(urlDataFromIntent);
                if (tabOpenTab != null) {
                    tabOpenTab.setAppId(stringExtra);
                    if ((intent.getFlags() & 4194304) != 0) {
                        tabOpenTab.setCloseOnBack(true);
                        return;
                    }
                    return;
                }
                return;
            }
            if (!urlDataFromIntent.isEmpty() && urlDataFromIntent.mUrl.startsWith("about:debug")) {
                if ("about:debug.dumpmem".equals(urlDataFromIntent.mUrl)) {
                    new OutputMemoryInfo().execute(this.mTabControl, null);
                    return;
                } else if ("about:debug.dumpmem.file".equals(urlDataFromIntent.mUrl)) {
                    new OutputMemoryInfo().execute(this.mTabControl, this.mTabControl);
                    return;
                } else {
                    this.mSettings.toggleDebugSettings();
                    return;
                }
            }
            this.mController.dismissSubWindow(currentTab);
            currentTab.setAppId(null);
            this.mController.loadUrlDataIn(currentTab, urlDataFromIntent);
        }
    }

    protected static UrlData getUrlDataFromIntent(Intent intent) {
        String str;
        HashMap map;
        PreloadedTabControl preloadedTabControl;
        String str2;
        String strSmartUrlFilter;
        HashMap map2;
        String stringExtra;
        Bundle bundleExtra;
        String string;
        String stringExtra2 = "";
        PreloadedTabControl preloadedTab = null;
        if (intent == null || (intent.getFlags() & 1048576) != 0) {
            str = stringExtra2;
            map = null;
            preloadedTabControl = null;
            str2 = null;
        } else {
            String action = intent.getAction();
            if ("android.intent.action.VIEW".equals(action) || "android.nfc.action.NDEF_DISCOVERED".equals(action)) {
                Uri data = intent.getData();
                if (data != null) {
                    strSmartUrlFilter = data.toString();
                } else {
                    strSmartUrlFilter = null;
                }
                if (strSmartUrlFilter != null && !strSmartUrlFilter.startsWith("content://")) {
                    strSmartUrlFilter = UrlUtils.smartUrlFilter(intent.getData());
                }
                if (strSmartUrlFilter != null && strSmartUrlFilter.startsWith("http") && (bundleExtra = intent.getBundleExtra("com.android.browser.headers")) != null && !bundleExtra.isEmpty()) {
                    map2 = new HashMap();
                    for (String str3 : bundleExtra.keySet()) {
                        map2.put(str3, bundleExtra.getString(str3));
                    }
                } else {
                    map2 = null;
                }
                if (intent.hasExtra("preload_id")) {
                    String stringExtra3 = intent.getStringExtra("preload_id");
                    stringExtra = intent.getStringExtra("searchbox_query");
                    preloadedTab = Preloader.getInstance().getPreloadedTab(stringExtra3);
                } else {
                    stringExtra = null;
                }
                str = strSmartUrlFilter;
                preloadedTabControl = preloadedTab;
                str2 = stringExtra;
                map = map2;
            } else {
                if (("android.intent.action.SEARCH".equals(action) || "android.intent.action.MEDIA_SEARCH".equals(action) || "android.intent.action.WEB_SEARCH".equals(action)) && (stringExtra2 = intent.getStringExtra("query")) != null) {
                    stringExtra2 = UrlUtils.smartUrlFilter(UrlUtils.fixUrl(stringExtra2));
                    if (stringExtra2.contains("&source=android-browser-suggest&")) {
                        Bundle bundleExtra2 = intent.getBundleExtra("app_data");
                        if (bundleExtra2 != null) {
                            string = bundleExtra2.getString("source");
                        } else {
                            string = null;
                        }
                        if (TextUtils.isEmpty(string)) {
                            string = "unknown";
                        }
                        stringExtra2 = stringExtra2.replace("&source=android-browser-suggest&", "&source=android-" + string + "&");
                    }
                }
                str = stringExtra2;
                map = null;
                preloadedTabControl = null;
                str2 = null;
            }
        }
        if (DEBUG) {
            Log.d("browser", "IntentHandler.getUrlDataFromIntent----->url : " + str + " headers: " + map);
        }
        return new UrlData(str, map, intent, preloadedTabControl, str2);
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
            stringExtra = data != null ? data.toString() : null;
            if (stringExtra != null && stringExtra.startsWith("content://")) {
                return false;
            }
            if (controller != null && intent.getBooleanExtra("inputUrl", false)) {
                ((BaseUi) controller.getUi()).setInputUrlFlag(true);
                Log.d("browser", "handleWebSearchIntent inputUrl setInputUrlFlag");
            }
        } else if ("android.intent.action.SEARCH".equals(action) || "android.intent.action.MEDIA_SEARCH".equals(action) || "android.intent.action.WEB_SEARCH".equals(action)) {
            stringExtra = intent.getStringExtra("query");
        }
        if (DEBUG) {
            Log.d("browser", "IntentHandler.handleWebSearchIntent()----->url : " + stringExtra);
        }
        return handleWebSearchRequest(activity, controller, stringExtra, intent.getBundleExtra("app_data"), intent.getStringExtra("intent_extra_data_key"));
    }

    private static boolean handleWebSearchRequest(Activity activity, Controller controller, String str, Bundle bundle, String str2) {
        if (DEBUG) {
            Log.d("browser", "IntentHandler.handleWebSearchRequest()----->" + str);
        }
        if (str == null) {
            return false;
        }
        if (DEBUG) {
            Log.d("browser", "IntentHandler.handleWebSearchRequest()----->inUrl : " + str + " extraData : " + str2);
        }
        final String strTrim = UrlUtils.fixUrl(str).trim();
        if (TextUtils.isEmpty(strTrim) || Patterns.WEB_URL.matcher(strTrim).matches() || UrlUtils.ACCEPTED_URI_SCHEMA.matcher(strTrim).matches()) {
            return false;
        }
        final ContentResolver contentResolver = activity.getContentResolver();
        if (DEBUG) {
            Log.d("browser", "IntentHandler.handleWebSearchRequest()----->newUrl : " + strTrim);
        }
        if (controller == null || controller.getTabControl() == null || controller.getTabControl().getCurrentWebView() == null || !controller.getTabControl().getCurrentWebView().isPrivateBrowsingEnabled()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voidArr) {
                    com.android.browser.provider.Browser.addSearchUrl(contentResolver, strTrim);
                    return null;
                }
            }.execute(new Void[0]);
        }
        SearchEngine searchEngine = BrowserSettings.getInstance().getSearchEngine();
        if (searchEngine == null) {
            return false;
        }
        searchEngine.startSearch(activity, strTrim, bundle, str2);
        return true;
    }

    private static boolean isForbiddenUri(Uri uri) {
        for (String str : URI_WHITELIST) {
            if (str.equals(uri.toString())) {
                return false;
            }
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        String lowerCase = scheme.toLowerCase(Locale.US);
        for (String str2 : SCHEME_WHITELIST) {
            if (str2.equals(lowerCase)) {
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

        UrlData(String str) {
            this.mUrl = str;
            this.mHeaders = null;
            this.mPreloadedTab = null;
            this.mSearchBoxQueryToSubmit = null;
            this.mDisableUrlOverride = false;
        }

        UrlData(String str, Map<String, String> map, Intent intent, PreloadedTabControl preloadedTabControl, String str2) {
            this.mUrl = str;
            this.mHeaders = map;
            this.mPreloadedTab = preloadedTabControl;
            this.mSearchBoxQueryToSubmit = str2;
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
