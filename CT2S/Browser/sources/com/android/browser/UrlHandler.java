package com.android.browser;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;

public class UrlHandler {
    static final Uri RLZ_PROVIDER_URI = Uri.parse("content://com.google.android.partnersetup.rlzappprovider/");
    Activity mActivity;
    Controller mController;
    private Boolean mIsProviderPresent = null;
    private Uri mRlzUri = null;

    public UrlHandler(Controller controller) {
        this.mController = controller;
        this.mActivity = this.mController.getActivity();
    }

    boolean shouldOverrideUrlLoading(Tab tab, WebView view, String url) {
        if (view.isPrivateBrowsingEnabled()) {
            return false;
        }
        if (url.startsWith("wtai://wp/")) {
            if (url.startsWith("wtai://wp/mc;")) {
                Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("tel:" + url.substring("wtai://wp/mc;".length())));
                this.mActivity.startActivity(intent);
                this.mController.closeEmptyTab();
                return true;
            }
            if (url.startsWith("wtai://wp/sd;") || url.startsWith("wtai://wp/ap;")) {
                return false;
            }
        }
        if (url.startsWith("about:")) {
            return false;
        }
        if (rlzProviderPresent()) {
            Uri siteUri = Uri.parse(url);
            if (needsRlzString(siteUri)) {
                new RLZTask(tab, siteUri, view).execute(new Void[0]);
                return true;
            }
        }
        return startActivityForUrl(tab, url) || handleMenuClick(tab, url);
    }

    boolean startActivityForUrl(Tab tab, String url) {
        try {
            Intent intent = Intent.parseUri(url, 1);
            if (this.mActivity.getPackageManager().resolveActivity(intent, 0) == null) {
                String packagename = intent.getPackage();
                if (packagename == null) {
                    return false;
                }
                Intent intent2 = new Intent("android.intent.action.VIEW", Uri.parse("market://search?q=pname:" + packagename));
                intent2.addCategory("android.intent.category.BROWSABLE");
                try {
                    this.mActivity.startActivity(intent2);
                    this.mController.closeEmptyTab();
                    return true;
                } catch (ActivityNotFoundException e) {
                    Log.w("Browser", "No market was installed or can be handled this url, " + intent2.getData());
                    return true;
                }
            }
            intent.addCategory("android.intent.category.BROWSABLE");
            intent.setComponent(null);
            Intent selector = intent.getSelector();
            if (selector != null) {
                selector.addCategory("android.intent.category.BROWSABLE");
                selector.setComponent(null);
            }
            if (tab != null) {
                if (tab.getAppId() == null) {
                    tab.setAppId(this.mActivity.getPackageName() + "-" + tab.getId());
                }
                intent.putExtra("com.android.browser.application_id", tab.getAppId());
            }
            Matcher m = UrlUtils.ACCEPTED_URI_SCHEMA.matcher(url);
            if (!url.startsWith("mailto:") && m.matches() && !isSpecializedHandlerAvailable(intent)) {
                return false;
            }
            try {
                intent.putExtra("disable_url_override", true);
                if (this.mActivity.startActivityIfNeeded(intent, -1)) {
                    this.mController.closeEmptyTab();
                    return true;
                }
            } catch (ActivityNotFoundException e2) {
            }
            return false;
        } catch (URISyntaxException ex) {
            Log.w("Browser", "Bad URI " + url + ": " + ex.getMessage());
            return false;
        }
    }

    private boolean isSpecializedHandlerAvailable(Intent intent) {
        PackageManager pm = this.mActivity.getPackageManager();
        List<ResolveInfo> handlers = pm.queryIntentActivities(intent, 64);
        if (handlers == null || handlers.size() == 0) {
            return false;
        }
        for (ResolveInfo resolveInfo : handlers) {
            IntentFilter filter = resolveInfo.filter;
            if (filter != null && (filter.countDataAuthorities() != 0 || filter.countDataPaths() != 0)) {
                return true;
            }
        }
        return false;
    }

    boolean handleMenuClick(Tab tab, String url) {
        if (!this.mController.isMenuDown()) {
            return false;
        }
        this.mController.openTab(url, tab != null && tab.isPrivateBrowsingEnabled(), BrowserSettings.getInstance().openInBackground() ? false : true, true);
        this.mActivity.closeOptionsMenu();
        return true;
    }

    private class RLZTask extends AsyncTask<Void, Void, String> {
        private Uri mSiteUri;
        private Tab mTab;
        private WebView mWebView;

        public RLZTask(Tab tab, Uri uri, WebView webView) {
            this.mTab = tab;
            this.mSiteUri = uri;
            this.mWebView = webView;
        }

        @Override
        protected String doInBackground(Void... unused) {
            String result = this.mSiteUri.toString();
            Cursor cur = null;
            try {
                cur = UrlHandler.this.mActivity.getContentResolver().query(UrlHandler.this.getRlzUri(), null, null, null, null);
                if (cur != null && cur.moveToFirst() && !cur.isNull(0)) {
                    result = this.mSiteUri.buildUpon().appendQueryParameter("rlz", cur.getString(0)).build().toString();
                }
                return result;
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (!UrlHandler.this.mController.isActivityPaused() && UrlHandler.this.mController.getTabControl().getTabPosition(this.mTab) != -1 && !UrlHandler.this.startActivityForUrl(this.mTab, result) && !UrlHandler.this.handleMenuClick(this.mTab, result)) {
                UrlHandler.this.mController.loadUrl(this.mTab, result);
            }
        }
    }

    private boolean rlzProviderPresent() {
        if (this.mIsProviderPresent == null) {
            PackageManager pm = this.mActivity.getPackageManager();
            this.mIsProviderPresent = Boolean.valueOf(pm.resolveContentProvider("com.google.android.partnersetup.rlzappprovider", 0) != null);
        }
        return this.mIsProviderPresent.booleanValue();
    }

    private Uri getRlzUri() {
        if (this.mRlzUri == null) {
            String ap = this.mActivity.getResources().getString(R.string.rlz_access_point);
            this.mRlzUri = Uri.withAppendedPath(RLZ_PROVIDER_URI, ap);
        }
        return this.mRlzUri;
    }

    private static boolean needsRlzString(Uri uri) {
        String host;
        String scheme = uri.getScheme();
        if ((!"http".equals(scheme) && !"https".equals(scheme)) || uri.getQueryParameter("q") == null || uri.getQueryParameter("rlz") != null || (host = uri.getHost()) == null) {
            return false;
        }
        String[] hostComponents = host.split("\\.");
        if (hostComponents.length < 2) {
            return false;
        }
        int googleComponent = hostComponents.length - 2;
        String component = hostComponents[googleComponent];
        if (!"google".equals(component)) {
            if (hostComponents.length < 3) {
                return false;
            }
            if (!"co".equals(component) && !"com".equals(component)) {
                return false;
            }
            googleComponent = hostComponents.length - 3;
            if (!"google".equals(hostComponents[googleComponent])) {
                return false;
            }
        }
        return googleComponent <= 0 || !"corp".equals(hostComponents[googleComponent + (-1)]);
    }
}
