package com.android.browser;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import com.android.browser.UI;
import com.android.browser.search.SearchEngine;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class IntentHandler {
    static final UrlData EMPTY_URL_DATA = new UrlData(null);
    private static final String[] SCHEME_WHITELIST = {"http", "https", "about"};
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
        String url;
        Uri uri = intent.getData();
        if (uri != null && isForbiddenUri(uri)) {
            Log.e("IntentHandler", "Aborting intent with forbidden uri, \"" + uri + "\"");
            return;
        }
        Tab current = this.mTabControl.getCurrentTab();
        if (current == null) {
            current = this.mTabControl.getTab(0);
            if (current != null) {
                this.mController.setActiveTab(current);
            } else {
                return;
            }
        }
        String action = intent.getAction();
        int flags = intent.getFlags();
        if (!"android.intent.action.MAIN".equals(action) && (1048576 & flags) == 0) {
            if ("show_bookmarks".equals(action)) {
                this.mController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
                return;
            }
            ((SearchManager) this.mActivity.getSystemService("search")).stopSearch();
            if (("android.intent.action.VIEW".equals(action) || "android.nfc.action.NDEF_DISCOVERED".equals(action) || "android.intent.action.SEARCH".equals(action) || "android.intent.action.MEDIA_SEARCH".equals(action) || "android.intent.action.WEB_SEARCH".equals(action)) && !handleWebSearchIntent(this.mActivity, this.mController, intent)) {
                if (intent != null && "android.intent.action.SEARCH".equals(action) && (url = intent.getStringExtra("query")) != null && url.toLowerCase().startsWith("rtsp://")) {
                    String url2 = UrlUtils.smartUrlFilter(url);
                    try {
                        Intent SendIntent = Intent.parseUri(url2, 1);
                        try {
                            if (this.mActivity.startActivityIfNeeded(SendIntent, -1)) {
                                this.mController.closeEmptyTab();
                                return;
                            }
                        } catch (ActivityNotFoundException e) {
                        }
                    } catch (URISyntaxException ex) {
                        Log.w("Browser", "Bad URI " + url2 + ": " + ex.getMessage());
                        return;
                    }
                }
                UrlData urlData = getUrlDataFromIntent(intent);
                if (urlData.isEmpty()) {
                    urlData = new UrlData(this.mSettings.getHomePage());
                }
                if (intent.getBooleanExtra("create_new_tab", false) || urlData.isPreloaded()) {
                    this.mController.openTab(urlData);
                    return;
                }
                String appId = intent.getStringExtra("com.android.browser.application_id");
                if ("android.intent.action.VIEW".equals(action) && appId != null && appId.startsWith(this.mActivity.getPackageName()) && (appTab2 = this.mTabControl.getTabFromAppId(appId)) != null && appTab2 == this.mController.getCurrentTab()) {
                    this.mController.switchToTab(appTab2);
                    this.mController.loadUrlDataIn(appTab2, urlData);
                    return;
                }
                if ("android.intent.action.VIEW".equals(action) && !this.mActivity.getPackageName().equals(appId)) {
                    if (!BrowserActivity.isTablet(this.mActivity) && !this.mSettings.allowAppTabs() && (appTab = this.mTabControl.getTabFromAppId(appId)) != null) {
                        this.mController.reuseTab(appTab, urlData);
                        return;
                    }
                    Tab appTab3 = this.mTabControl.findTabWithUrl(urlData.mUrl);
                    if (appTab3 != null) {
                        appTab3.setAppId(appId);
                        if (current != appTab3) {
                            this.mController.switchToTab(appTab3);
                        }
                        this.mController.loadUrlDataIn(appTab3, urlData);
                        return;
                    }
                    Tab tab = this.mController.openTab(urlData);
                    if (tab != null) {
                        tab.setAppId(appId);
                        if ((intent.getFlags() & 4194304) != 0) {
                            tab.setCloseOnBack(true);
                            return;
                        }
                        return;
                    }
                    return;
                }
                this.mController.dismissSubWindow(current);
                current.setAppId(null);
                this.mController.loadUrlDataIn(current, urlData);
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
                url = UrlUtils.smartUrlFilter(intent.getData());
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
        return new UrlData(url, headers, intent, preloaded, preloadedSearchBoxQuery);
    }

    static boolean handleWebSearchIntent(Activity activity, Controller controller, Intent intent) {
        if (intent == null) {
            return false;
        }
        String url = null;
        String action = intent.getAction();
        if ("android.intent.action.VIEW".equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                url = data.toString();
            }
        } else if ("android.intent.action.SEARCH".equals(action) || "android.intent.action.MEDIA_SEARCH".equals(action) || "android.intent.action.WEB_SEARCH".equals(action)) {
            url = intent.getStringExtra("query");
        }
        return handleWebSearchRequest(activity, controller, url, intent.getBundleExtra("app_data"), intent.getStringExtra("intent_extra_data_key"));
    }

    private static boolean handleWebSearchRequest(Activity activity, Controller controller, String inUrl, Bundle appData, String extraData) {
        if (inUrl == null) {
            return false;
        }
        final String url = UrlUtils.fixUrl(inUrl).trim();
        if (TextUtils.isEmpty(url) || Patterns.WEB_URL.matcher(url).matches() || UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url).matches()) {
            return false;
        }
        final ContentResolver cr = activity.getContentResolver();
        if (controller == null || controller.getTabControl() == null || controller.getTabControl().getCurrentWebView() == null || !controller.getTabControl().getCurrentWebView().isPrivateBrowsingEnabled()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... unused) {
                    android.provider.Browser.addSearchUrl(cr, url);
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
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        String scheme2 = scheme.toLowerCase(Locale.US);
        String[] arr$ = SCHEME_WHITELIST;
        for (String allowed : arr$) {
            if (allowed.equals(scheme2)) {
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
