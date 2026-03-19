package com.android.captiveportallogin;

import android.app.Activity;
import android.app.LoadedApk;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.CaptivePortal;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.captiveportal.CaptivePortalProbeSpec;
import android.net.dns.ResolvUtil;
import android.net.http.SslError;
import android.net.wifi.WifiInfo;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import com.android.internal.logging.MetricsLogger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class CaptivePortalLoginActivity extends Activity {
    private CaptivePortal mCaptivePortal;
    private ConnectivityManager mCm;
    private Network mNetwork;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private CaptivePortalProbeSpec mProbeSpec;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private URL mUrl;
    private String mUserAgent;
    private MyWebViewClient mWebViewClient;
    private static final String TAG = CaptivePortalLoginActivity.class.getSimpleName();
    private static final SparseArray<String> SSL_ERRORS = new SparseArray<>();
    private boolean mLaunchBrowser = false;
    private final AtomicBoolean isDone = new AtomicBoolean(false);

    static {
        SSL_ERRORS.put(0, "SSL_NOTYETVALID");
        SSL_ERRORS.put(1, "SSL_EXPIRED");
        SSL_ERRORS.put(2, "SSL_IDMISMATCH");
        SSL_ERRORS.put(3, "SSL_UNTRUSTED");
        SSL_ERRORS.put(4, "SSL_DATE_INVALID");
        SSL_ERRORS.put(5, "SSL_INVALID");
    }

    private enum Result {
        DISMISSED(1005),
        UNWANTED(1006),
        WANTED_AS_IS(1007);

        final int metricsEvent;

        Result(int i) {
            this.metricsEvent = i;
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        logMetricsEvent(1004);
        this.mCm = ConnectivityManager.from(this);
        this.mNetwork = (Network) getIntent().getParcelableExtra("android.net.extra.NETWORK");
        this.mCaptivePortal = (CaptivePortal) getIntent().getParcelableExtra("android.net.extra.CAPTIVE_PORTAL");
        this.mUserAgent = getIntent().getStringExtra("android.net.extra.CAPTIVE_PORTAL_USER_AGENT");
        this.mUrl = getUrl();
        if (this.mUrl == null) {
            done(Result.WANTED_AS_IS);
            return;
        }
        Log.d(TAG, String.format("onCreate for %s", this.mUrl.toString()));
        try {
            this.mProbeSpec = CaptivePortalProbeSpec.parseSpecOrNull(getIntent().getStringExtra("android.net.extra.CAPTIVE_PORTAL_PROBE_SPEC"));
        } catch (Exception e) {
            this.mProbeSpec = null;
        }
        this.mCm.bindProcessToNetwork(this.mNetwork);
        ConnectivityManager connectivityManager = this.mCm;
        ConnectivityManager.setProcessDefaultNetworkForHostResolution(ResolvUtil.getNetworkWithUseLocalNameserversFlag(this.mNetwork));
        setContentView(R.layout.activity_captive_portal_login);
        NetworkCapabilities networkCapabilities = this.mCm.getNetworkCapabilities(this.mNetwork);
        if (networkCapabilities == null) {
            finishAndRemoveTask();
            return;
        }
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(Network network) {
                if (CaptivePortalLoginActivity.this.mNetwork.equals(network)) {
                    CaptivePortalLoginActivity.this.done(Result.UNWANTED);
                }
            }
        };
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        for (int i : networkCapabilities.getTransportTypes()) {
            builder.addTransportType(i);
        }
        this.mCm.registerNetworkCallback(builder.build(), this.mNetworkCallback);
        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setElevation(0.0f);
        getActionBar().setTitle(getHeaderTitle());
        getActionBar().setSubtitle("");
        final WebView webview = getWebview();
        webview.clearCache(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true);
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setMixedContentMode(2);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        this.mWebViewClient = new MyWebViewClient();
        webview.setWebViewClient(this.mWebViewClient);
        webview.setWebChromeClient(new MyWebChromeClient());
        webview.loadData("", "text/html", null);
        this.mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        this.mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public final void onRefresh() {
                CaptivePortalLoginActivity.lambda$onCreate$0(this.f$0, webview);
            }
        });
    }

    public static void lambda$onCreate$0(CaptivePortalLoginActivity captivePortalLoginActivity, WebView webView) {
        webView.reload();
        captivePortalLoginActivity.mSwipeRefreshLayout.setRefreshing(true);
    }

    private void setWebViewProxy() {
        LoadedApk loadedApk = getApplication().mLoadedApk;
        try {
            Field declaredField = LoadedApk.class.getDeclaredField("mReceivers");
            declaredField.setAccessible(true);
            Iterator it = ((ArrayMap) declaredField.get(loadedApk)).values().iterator();
            while (it.hasNext()) {
                for (Object obj : ((ArrayMap) it.next()).keySet()) {
                    Class<?> cls = obj.getClass();
                    if (cls.getName().contains("ProxyChangeListener")) {
                        cls.getDeclaredMethod("onReceive", Context.class, Intent.class).invoke(obj, getApplicationContext(), new Intent("android.intent.action.PROXY_CHANGE"));
                        Log.v(TAG, "Prompting WebView proxy reload.");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while setting WebView proxy: " + e);
        }
    }

    private void done(Result result) {
        if (this.isDone.getAndSet(true)) {
            return;
        }
        Log.d(TAG, String.format("Result %s for %s", result.name(), this.mUrl.toString()));
        logMetricsEvent(result.metricsEvent);
        switch (AnonymousClass3.$SwitchMap$com$android$captiveportallogin$CaptivePortalLoginActivity$Result[result.ordinal()]) {
            case 1:
                this.mCaptivePortal.reportCaptivePortalDismissed();
                break;
            case 2:
                this.mCaptivePortal.ignoreNetwork();
                break;
            case 3:
                this.mCaptivePortal.useNetwork();
                break;
        }
        finishAndRemoveTask();
    }

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$android$captiveportallogin$CaptivePortalLoginActivity$Result = new int[Result.values().length];

        static {
            try {
                $SwitchMap$com$android$captiveportallogin$CaptivePortalLoginActivity$Result[Result.DISMISSED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$captiveportallogin$CaptivePortalLoginActivity$Result[Result.UNWANTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$captiveportallogin$CaptivePortalLoginActivity$Result[Result.WANTED_AS_IS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.captive_portal_login, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        WebView webView = (WebView) findViewById(R.id.webview);
        if (webView.canGoBack() && this.mWebViewClient.allowBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Result result;
        String str;
        switch (menuItem.getItemId()) {
            case R.id.action_do_not_use_network:
                result = Result.UNWANTED;
                str = "DO_NOT_USE_NETWORK";
                break;
            case R.id.action_use_network:
                result = Result.WANTED_AS_IS;
                str = "USE_NETWORK";
                break;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
        Log.d(TAG, String.format("onOptionsItemSelect %s for %s", str, this.mUrl.toString()));
        done(result);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mNetworkCallback != null) {
            this.mCm.unregisterNetworkCallback(this.mNetworkCallback);
        }
        if (this.mLaunchBrowser) {
            for (int i = 0; i < 5 && !this.mNetwork.equals(this.mCm.getActiveNetwork()); i++) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                }
            }
            String string = this.mUrl.toString();
            Log.d(TAG, "starting activity with intent ACTION_VIEW for " + string);
            if (BenesseExtension.getDchaState() == 0) {
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse(string)));
            }
        }
    }

    private URL getUrl() {
        String stringExtra = getIntent().getStringExtra("android.net.extra.CAPTIVE_PORTAL_URL");
        if (stringExtra == null) {
            stringExtra = this.mCm.getCaptivePortalServerUrl();
        }
        return makeURL(stringExtra);
    }

    private static URL makeURL(String str) {
        try {
            return new URL(str);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid URL " + str);
            return null;
        }
    }

    private static String host(URL url) {
        if (url == null) {
            return null;
        }
        return url.getHost();
    }

    private static String sanitizeURL(URL url) {
        return Build.IS_DEBUGGABLE ? Objects.toString(url) : host(url);
    }

    private void testForCaptivePortal() {
        new Thread(new Runnable() {
            @Override
            public void run() throws Throwable {
                HttpURLConnection httpURLConnection;
                String headerField;
                int responseCode;
                String contentType;
                Network networkMakeNetworkWithPrivateDnsBypass = ResolvUtil.makeNetworkWithPrivateDnsBypass(CaptivePortalLoginActivity.this.mNetwork);
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                }
                int i = 204;
                try {
                    httpURLConnection = (HttpURLConnection) networkMakeNetworkWithPrivateDnsBypass.openConnection(CaptivePortalLoginActivity.this.mUrl);
                    try {
                        try {
                            httpURLConnection.setInstanceFollowRedirects(false);
                            httpURLConnection.setConnectTimeout(10000);
                            httpURLConnection.setReadTimeout(10000);
                            httpURLConnection.setUseCaches(false);
                            if (CaptivePortalLoginActivity.this.mUserAgent != null) {
                                httpURLConnection.setRequestProperty("User-Agent", CaptivePortalLoginActivity.this.mUserAgent);
                            }
                            String string = httpURLConnection.getRequestProperties().toString();
                            httpURLConnection.getInputStream();
                            responseCode = httpURLConnection.getResponseCode();
                            try {
                                headerField = httpURLConnection.getHeaderField("Location");
                            } catch (IOException e2) {
                                headerField = null;
                            }
                            try {
                                Log.d(CaptivePortalLoginActivity.TAG, "probe at " + CaptivePortalLoginActivity.this.mUrl + " ret=" + responseCode + " request=" + string + " headers=" + httpURLConnection.getHeaderFields());
                                contentType = httpURLConnection.getContentType();
                            } catch (IOException e3) {
                                i = responseCode;
                                if (httpURLConnection != null) {
                                }
                                if (CaptivePortalLoginActivity.isDismissed(i, headerField, CaptivePortalLoginActivity.this.mProbeSpec)) {
                                }
                            }
                        } catch (IOException e4) {
                            i = 500;
                            headerField = null;
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                        throw th;
                    }
                } catch (IOException e5) {
                    i = 500;
                    httpURLConnection = null;
                    headerField = null;
                } catch (Throwable th2) {
                    th = th2;
                    httpURLConnection = null;
                }
                if (contentType != null) {
                    if (contentType.contains("text/html")) {
                        String line = new BufferedReader(new InputStreamReader((InputStream) httpURLConnection.getContent())).readLine();
                        Log.d(CaptivePortalLoginActivity.TAG, "urlConnection.getContent() = " + line);
                        try {
                            if (responseCode == 200 && line == null) {
                                Log.d(CaptivePortalLoginActivity.TAG, "Internet detected!");
                            } else if (responseCode == 200 && line.contains("Success")) {
                                Log.d(CaptivePortalLoginActivity.TAG, "Internet detected!");
                            }
                        } catch (IOException e6) {
                            if (httpURLConnection != null) {
                            }
                            if (CaptivePortalLoginActivity.isDismissed(i, headerField, CaptivePortalLoginActivity.this.mProbeSpec)) {
                            }
                        }
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                    }
                    if (CaptivePortalLoginActivity.isDismissed(i, headerField, CaptivePortalLoginActivity.this.mProbeSpec)) {
                        return;
                    }
                    CaptivePortalLoginActivity.this.done(Result.DISMISSED);
                    return;
                }
                Log.d(CaptivePortalLoginActivity.TAG, "contentType is null, httpResponseCode = " + responseCode);
                i = responseCode;
                if (httpURLConnection != null) {
                }
                if (CaptivePortalLoginActivity.isDismissed(i, headerField, CaptivePortalLoginActivity.this.mProbeSpec)) {
                }
            }
        }).start();
    }

    private static boolean isDismissed(int i, String str, CaptivePortalProbeSpec captivePortalProbeSpec) {
        if (captivePortalProbeSpec != null) {
            return captivePortalProbeSpec.getResult(i, str).isSuccessful();
        }
        return i == 204;
    }

    private class MyWebViewClient extends WebViewClient {
        private final String mBrowserBailOutToken;
        private final float mDpPerSp;
        private String mHostname;
        private int mPagesLoaded;

        private MyWebViewClient() {
            this.mBrowserBailOutToken = Long.toString(new Random().nextLong());
            this.mDpPerSp = TypedValue.applyDimension(2, 1.0f, CaptivePortalLoginActivity.this.getResources().getDisplayMetrics()) / TypedValue.applyDimension(1, 1.0f, CaptivePortalLoginActivity.this.getResources().getDisplayMetrics());
        }

        public boolean allowBack() {
            return this.mPagesLoaded > 1;
        }

        @Override
        public void onPageStarted(WebView webView, String str, Bitmap bitmap) {
            if (str.contains(this.mBrowserBailOutToken)) {
                CaptivePortalLoginActivity.this.mLaunchBrowser = true;
                CaptivePortalLoginActivity.this.done(Result.WANTED_AS_IS);
                return;
            }
            if (this.mPagesLoaded != 0) {
                URL urlMakeURL = CaptivePortalLoginActivity.makeURL(str);
                Log.d(CaptivePortalLoginActivity.TAG, "onPageStarted: " + CaptivePortalLoginActivity.sanitizeURL(urlMakeURL));
                this.mHostname = CaptivePortalLoginActivity.host(urlMakeURL);
                if (!str.startsWith("file:///android_asset/")) {
                    if (urlMakeURL != null) {
                        str = CaptivePortalLoginActivity.this.getHeaderSubtitle(urlMakeURL);
                    }
                    CaptivePortalLoginActivity.this.getActionBar().setSubtitle(str);
                }
                CaptivePortalLoginActivity.this.getProgressBar().setVisibility(0);
                CaptivePortalLoginActivity.this.testForCaptivePortal();
            }
        }

        @Override
        public void onPageFinished(WebView webView, String str) {
            this.mPagesLoaded++;
            CaptivePortalLoginActivity.this.getProgressBar().setVisibility(4);
            CaptivePortalLoginActivity.this.mSwipeRefreshLayout.setRefreshing(false);
            if (this.mPagesLoaded == 1) {
                CaptivePortalLoginActivity.this.setWebViewProxy();
                webView.loadUrl(CaptivePortalLoginActivity.this.mUrl.toString());
            } else {
                if (this.mPagesLoaded == 2) {
                    webView.requestFocus();
                    webView.clearHistory();
                }
                CaptivePortalLoginActivity.this.testForCaptivePortal();
            }
        }

        private String sp(int i) {
            return Integer.toString((int) ((float) (((double) (i * this.mDpPerSp)) * 1.3d))) + "px";
        }

        @Override
        public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
            URL urlMakeURL = CaptivePortalLoginActivity.makeURL(sslError.getUrl());
            String strHost = CaptivePortalLoginActivity.host(urlMakeURL);
            Log.d(CaptivePortalLoginActivity.TAG, String.format("SSL error: %s, url: %s, certificate: %s", CaptivePortalLoginActivity.sslErrorName(sslError), CaptivePortalLoginActivity.sanitizeURL(urlMakeURL), sslError.getCertificate()));
            if (urlMakeURL != null && Objects.equals(strHost, this.mHostname)) {
                CaptivePortalLoginActivity.this.logMetricsEvent(1013);
                webView.loadDataWithBaseURL("file:///android_asset/", makeSslErrorPage(), "text/HTML", "UTF-8", null);
            } else {
                sslErrorHandler.cancel();
            }
        }

        private String makeSslErrorPage() {
            return String.join("\n", "<html>", "<head>", "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">", "  <style>", "    body {", "      background-color:#fafafa;", "      margin:auto;", "      width:80%;", "      margin-top: 96px", "    }", "    img {", "      height:48px;", "      width:48px;", "    }", "    div.warn {", "      font-size:" + sp(16) + ";", "      line-height:1.28;", "      margin-top:16px;", "      opacity:0.87;", "    }", "    div.example {", "      font-size:" + sp(14) + ";", "      line-height:1.21905;", "      margin-top:16px;", "      opacity:0.54;", "    }", "    a {", "      color:#4285F4;", "      display:inline-block;", "      font-size:" + sp(14) + ";", "      font-weight:bold;", "      height:48px;", "      margin-top:24px;", "      text-decoration:none;", "      text-transform:uppercase;", "    }", "  </style>", "</head>", "<body>", "  <p><img src=quantum_ic_warning_amber_96.png><br>", "  <div class=warn>" + CaptivePortalLoginActivity.this.getString(R.string.ssl_error_warning) + "</div>", "  <div class=example>" + CaptivePortalLoginActivity.this.getString(R.string.ssl_error_example) + "</div>", "  <a href=" + this.mBrowserBailOutToken + ">" + CaptivePortalLoginActivity.this.getString(R.string.ssl_error_continue) + "</a>", "</body>", "</html>");
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String str) {
            if (str.startsWith("tel:")) {
                CaptivePortalLoginActivity.this.startActivity(new Intent("android.intent.action.DIAL", Uri.parse(str)));
                return true;
            }
            return false;
        }
    }

    private class MyWebChromeClient extends WebChromeClient {
        private MyWebChromeClient() {
        }

        @Override
        public void onProgressChanged(WebView webView, int i) {
            CaptivePortalLoginActivity.this.getProgressBar().setProgress(i);
        }
    }

    private ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.progress_bar);
    }

    private WebView getWebview() {
        return (WebView) findViewById(R.id.webview);
    }

    private String getHeaderTitle() {
        NetworkCapabilities networkCapabilities = this.mCm.getNetworkCapabilities(this.mNetwork);
        if (networkCapabilities == null || TextUtils.isEmpty(networkCapabilities.getSSID()) || !networkCapabilities.hasTransport(1)) {
            return getString(R.string.action_bar_label);
        }
        return getString(R.string.action_bar_title, WifiInfo.removeDoubleQuotes(networkCapabilities.getSSID()));
    }

    private String getHeaderSubtitle(URL url) {
        String strHost = host(url);
        if ("https".equals(url.getProtocol())) {
            return "https://" + strHost;
        }
        return strHost;
    }

    private void logMetricsEvent(int i) {
        MetricsLogger.action(this, i, getPackageName());
    }

    private static String sslErrorName(SslError sslError) {
        return SSL_ERRORS.get(sslError.getPrimaryError(), "UNKNOWN");
    }
}
