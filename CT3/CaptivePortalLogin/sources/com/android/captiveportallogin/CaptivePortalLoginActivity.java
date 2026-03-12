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
import android.net.http.SslError;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public class CaptivePortalLoginActivity extends Activity {

    private static final int[] f0xfaee33a0 = null;
    private CaptivePortal mCaptivePortal;
    private ConnectivityManager mCm;
    private boolean mLaunchBrowser = false;
    private Network mNetwork;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private URL mURL;
    private MyWebViewClient mWebViewClient;

    private static int[] m2xe4d667c() {
        if (f0xfaee33a0 != null) {
            return f0xfaee33a0;
        }
        int[] iArr = new int[Result.valuesCustom().length];
        try {
            iArr[Result.DISMISSED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Result.UNWANTED.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Result.WANTED_AS_IS.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f0xfaee33a0 = iArr;
        return iArr;
    }

    private enum Result {
        DISMISSED,
        UNWANTED,
        WANTED_AS_IS;

        public static Result[] valuesCustom() {
            return values();
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        MyWebViewClient myWebViewClient = null;
        Object[] objArr = 0;
        super.onCreate(bundle);
        this.mCm = ConnectivityManager.from(this);
        String stringExtra = getIntent().getStringExtra("android.net.extra.CAPTIVE_PORTAL_URL");
        if (stringExtra == null) {
            stringExtra = this.mCm.getCaptivePortalServerUrl();
        }
        try {
            this.mURL = new URL(stringExtra);
            this.mNetwork = (Network) getIntent().getParcelableExtra("android.net.extra.NETWORK");
            this.mCaptivePortal = (CaptivePortal) getIntent().getParcelableExtra("android.net.extra.CAPTIVE_PORTAL");
            this.mCm.bindProcessToNetwork(this.mNetwork);
            setContentView(R.layout.activity_captive_portal_login);
            getActionBar().setDisplayShowHomeEnabled(false);
            NetworkCapabilities networkCapabilities = this.mCm.getNetworkCapabilities(this.mNetwork);
            if (networkCapabilities == null) {
                finish();
                return;
            }
            this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(Network lostNetwork) {
                    if (CaptivePortalLoginActivity.this.mNetwork.equals(lostNetwork)) {
                        CaptivePortalLoginActivity.this.done(Result.UNWANTED);
                    }
                }
            };
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            for (int i : networkCapabilities.getTransportTypes()) {
                builder.addTransportType(i);
            }
            this.mCm.registerNetworkCallback(builder.build(), this.mNetworkCallback);
            WebView webView = (WebView) findViewById(R.id.webview);
            webView.clearCache(true);
            WebSettings settings = webView.getSettings();
            settings.setUseWideViewPort(true);
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setLoadWithOverviewMode(true);
            settings.setMixedContentMode(2);
            settings.setJavaScriptEnabled(true);
            this.mWebViewClient = new MyWebViewClient(this, myWebViewClient);
            webView.setWebViewClient(this.mWebViewClient);
            webView.setWebChromeClient(new MyWebChromeClient(this, objArr == true ? 1 : 0));
            webView.loadData("", "text/html", null);
        } catch (MalformedURLException e) {
            Log.e("CaptivePortalLogin", "Invalid captive portal URL, url=" + stringExtra);
            done(Result.WANTED_AS_IS);
            finish();
        }
    }

    public void setWebViewProxy() {
        LoadedApk loadedApk = getApplication().mLoadedApk;
        try {
            Field receiversField = LoadedApk.class.getDeclaredField("mReceivers");
            receiversField.setAccessible(true);
            ArrayMap receivers = (ArrayMap) receiversField.get(loadedApk);
            for (Object receiverMap : receivers.values()) {
                for (Object rec : ((ArrayMap) receiverMap).keySet()) {
                    Class<?> cls = rec.getClass();
                    if (cls.getName().contains("ProxyChangeListener")) {
                        Method onReceiveMethod = cls.getDeclaredMethod("onReceive", Context.class, Intent.class);
                        Intent intent = new Intent("android.intent.action.PROXY_CHANGE");
                        onReceiveMethod.invoke(rec, getApplicationContext(), intent);
                        Log.v("CaptivePortalLogin", "Prompting WebView proxy reload.");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CaptivePortalLogin", "Exception while setting WebView proxy: " + e);
        }
    }

    public void done(Result result) {
        if (this.mNetworkCallback != null) {
            this.mCm.unregisterNetworkCallback(this.mNetworkCallback);
            this.mNetworkCallback = null;
        }
        switch (m2xe4d667c()[result.ordinal()]) {
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
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onBackPressed() {
        WebView myWebView = (WebView) findViewById(R.id.webview);
        if (myWebView.canGoBack() && this.mWebViewClient.allowBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mNetworkCallback != null) {
            this.mCm.unregisterNetworkCallback(this.mNetworkCallback);
            this.mNetworkCallback = null;
        }
        if (!this.mLaunchBrowser) {
            return;
        }
        for (int i = 0; i < 5 && !this.mNetwork.equals(this.mCm.getActiveNetwork()); i++) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
            }
        }
        int dcha_state = BenesseExtension.getDchaState();
        if (dcha_state != 0) {
            return;
        }
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse(this.mURL.toString())));
    }

    public void testForCaptivePortal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                }
                HttpURLConnection httpURLConnection = null;
                int httpResponseCode = 500;
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) CaptivePortalLoginActivity.this.mURL.openConnection();
                    urlConnection.setInstanceFollowRedirects(false);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setUseCaches(false);
                    urlConnection.getInputStream();
                    httpResponseCode = urlConnection.getResponseCode();
                    if (httpResponseCode == 200) {
                        String contentType = urlConnection.getContentType();
                        if (contentType == null) {
                            Log.e("CaptivePortalLogin", "contentType is null");
                        } else if (contentType.contains("text/html")) {
                            InputStreamReader in = new InputStreamReader((InputStream) urlConnection.getContent());
                            BufferedReader buff = new BufferedReader(in);
                            String line = buff.readLine();
                            if (line.contains("Success")) {
                                httpResponseCode = 204;
                                Log.v("CaptivePortalLogin", "Internet detected!");
                            }
                        }
                    }
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                } catch (IOException e2) {
                    if (0 != 0) {
                        httpURLConnection.disconnect();
                    }
                } catch (Throwable th) {
                    if (0 != 0) {
                        httpURLConnection.disconnect();
                    }
                    throw th;
                }
                if (httpResponseCode != 204) {
                    return;
                }
                CaptivePortalLoginActivity.this.done(Result.DISMISSED);
            }
        }).start();
    }

    private class MyWebViewClient extends WebViewClient {
        private final String SSL_ERROR_HTML;
        private final String mBrowserBailOutToken;
        private final float mDpPerSp;
        private int mPagesLoaded;

        MyWebViewClient(CaptivePortalLoginActivity this$0, MyWebViewClient myWebViewClient) {
            this();
        }

        private MyWebViewClient() {
            this.mBrowserBailOutToken = Long.toString(new Random().nextLong());
            this.mDpPerSp = TypedValue.applyDimension(2, 1.0f, CaptivePortalLoginActivity.this.getResources().getDisplayMetrics()) / TypedValue.applyDimension(1, 1.0f, CaptivePortalLoginActivity.this.getResources().getDisplayMetrics());
            this.SSL_ERROR_HTML = "<html><head><style>body { margin-left:" + dp(48) + "; margin-right:" + dp(48) + "; margin-top:" + dp(96) + "; background-color:#fafafa; }img { width:" + dp(48) + "; height:" + dp(48) + "; }div.warn { font-size:" + sp(16) + "; margin-top:" + dp(16) + ";            opacity:0.87; line-height:1.28; }div.example { font-size:" + sp(14) + "; margin-top:" + dp(16) + ";               opacity:0.54; line-height:1.21905; }a { font-size:" + sp(14) + "; text-decoration:none; text-transform:uppercase;     margin-top:" + dp(24) + "; display:inline-block; color:#4285F4;     height:" + dp(48) + "; font-weight:bold; }</style></head><body><p><img src=quantum_ic_warning_amber_96.png><br><div class=warn>%s</div><div class=example>%s</div><a href=%s>%s</a></body></html>";
        }

        public boolean allowBack() {
            return this.mPagesLoaded > 1;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (url.contains(this.mBrowserBailOutToken)) {
                CaptivePortalLoginActivity.this.mLaunchBrowser = true;
                CaptivePortalLoginActivity.this.done(Result.WANTED_AS_IS);
            } else {
                if (this.mPagesLoaded == 0) {
                    return;
                }
                if (!url.startsWith("file:///android_asset/")) {
                    TextView myUrlBar = (TextView) CaptivePortalLoginActivity.this.findViewById(R.id.url_bar);
                    myUrlBar.setText(url);
                }
                CaptivePortalLoginActivity.this.testForCaptivePortal();
                Log.e("CaptivePortalLogin", "onPageStarted: firstPageLoad=" + this.mPagesLoaded + ", url=" + url);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            this.mPagesLoaded++;
            if (this.mPagesLoaded == 1) {
                Log.e("CaptivePortalLogin", "onPageFinished: firstPageLoad=" + this.mPagesLoaded + ", url=" + url);
                CaptivePortalLoginActivity.this.setWebViewProxy();
                view.loadUrl(CaptivePortalLoginActivity.this.mURL.toString());
            } else {
                if (this.mPagesLoaded == 2) {
                    view.clearHistory();
                }
                CaptivePortalLoginActivity.this.testForCaptivePortal();
            }
        }

        private String dp(int dp) {
            return Integer.toString(dp) + "px";
        }

        private String sp(int sp) {
            float dp = sp * this.mDpPerSp;
            return dp((int) (((double) dp) * 1.3d));
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.w("CaptivePortalLogin", "SSL error (error: " + error.getPrimaryError() + " host: " + Uri.parse(error.getUrl()).getHost() + " certificate: " + error.getCertificate() + "); displaying SSL warning.");
            String html = String.format(this.SSL_ERROR_HTML, CaptivePortalLoginActivity.this.getString(R.string.ssl_error_warning), CaptivePortalLoginActivity.this.getString(R.string.ssl_error_example), this.mBrowserBailOutToken, CaptivePortalLoginActivity.this.getString(R.string.ssl_error_continue));
            view.loadDataWithBaseURL("file:///android_asset/", html, "text/HTML", "UTF-8", null);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("tel:")) {
                CaptivePortalLoginActivity.this.startActivity(new Intent("android.intent.action.DIAL", Uri.parse(url)));
                return true;
            }
            return false;
        }
    }

    private class MyWebChromeClient extends WebChromeClient {
        MyWebChromeClient(CaptivePortalLoginActivity this$0, MyWebChromeClient myWebChromeClient) {
            this();
        }

        private MyWebChromeClient() {
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            ProgressBar myProgressBar = (ProgressBar) CaptivePortalLoginActivity.this.findViewById(R.id.progress_bar);
            myProgressBar.setProgress(newProgress);
        }
    }
}
