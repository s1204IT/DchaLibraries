package com.android.captiveportallogin;

import android.app.Activity;
import android.app.LoadedApk;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CaptivePortalLoginActivity extends Activity {
    private int mNetId;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private String mResponseToken;
    private URL mURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Uri dataUri;
        super.onCreate(savedInstanceState);
        String server = Settings.Global.getString(getContentResolver(), "captive_portal_server");
        if (server == null) {
            server = "connectivitycheck.android.com";
        }
        try {
            this.mURL = new URL("http", server, "/generate_204");
            dataUri = getIntent().getData();
        } catch (NumberFormatException | MalformedURLException e) {
            done(2);
        }
        if (!dataUri.getScheme().equals("netid")) {
            throw new MalformedURLException();
        }
        this.mNetId = Integer.parseInt(dataUri.getSchemeSpecificPart());
        this.mResponseToken = dataUri.getFragment();
        ConnectivityManager cm = ConnectivityManager.from(this);
        final Network network = new Network(this.mNetId);
        ConnectivityManager.setProcessDefaultNetwork(network);
        setContentView(R.layout.activity_captive_portal_login);
        getActionBar().setDisplayShowHomeEnabled(false);
        NetworkCapabilities networkCapabilities = cm.getNetworkCapabilities(network);
        if (networkCapabilities == null) {
            finish();
            return;
        }
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(Network lostNetwork) {
                if (network.equals(lostNetwork)) {
                    CaptivePortalLoginActivity.this.done(1);
                }
            }
        };
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        int[] arr$ = networkCapabilities.getTransportTypes();
        for (int transportType : arr$) {
            builder.addTransportType(transportType);
        }
        cm.registerNetworkCallback(builder.build(), this.mNetworkCallback);
        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.clearCache(true);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new MyWebViewClient());
        myWebView.setWebChromeClient(new MyWebChromeClient());
        myWebView.loadData("", "text/html", null);
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

    public void done(int result) {
        if (this.mNetworkCallback != null) {
            ConnectivityManager.from(this).unregisterNetworkCallback(this.mNetworkCallback);
        }
        Intent intent = new Intent("android.net.netmon.captive_portal_logged_in");
        intent.putExtra("android.intent.extra.TEXT", String.valueOf(this.mNetId));
        intent.putExtra("result", String.valueOf(result));
        intent.putExtra("response_token", this.mResponseToken);
        sendBroadcast(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.captive_portal_login, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        WebView myWebView = (WebView) findViewById(R.id.webview);
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_use_network) {
            done(2);
            return true;
        }
        if (id == R.id.action_do_not_use_network) {
            done(1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void testForCaptivePortal() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                }
                HttpURLConnection urlConnection = null;
                int httpResponseCode = 500;
                try {
                    urlConnection = (HttpURLConnection) CaptivePortalLoginActivity.this.mURL.openConnection();
                    urlConnection.setInstanceFollowRedirects(false);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);
                    urlConnection.setUseCaches(false);
                    urlConnection.getInputStream();
                    httpResponseCode = urlConnection.getResponseCode();
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                } catch (IOException e2) {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                } catch (Throwable th) {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    throw th;
                }
                if (httpResponseCode == 204) {
                    CaptivePortalLoginActivity.this.done(0);
                }
            }
        }).start();
    }

    private class MyWebViewClient extends WebViewClient {
        private boolean firstPageLoad;

        private MyWebViewClient() {
            this.firstPageLoad = true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (!this.firstPageLoad) {
                CaptivePortalLoginActivity.this.testForCaptivePortal();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!this.firstPageLoad) {
                CaptivePortalLoginActivity.this.testForCaptivePortal();
                return;
            }
            this.firstPageLoad = false;
            CaptivePortalLoginActivity.this.setWebViewProxy();
            view.loadUrl(CaptivePortalLoginActivity.this.mURL.toString());
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.w("CaptivePortalLogin", "SSL error; displaying broken lock icon.");
            view.loadDataWithBaseURL("file:///android_asset/", "<!DOCTYPE html><html><head><style>html { width:100%; height:100%;        background:url(locked_page.png) center center no-repeat; }</style></head><body></body></html>", "text/HTML", "UTF-8", null);
        }
    }

    private class MyWebChromeClient extends WebChromeClient {
        private MyWebChromeClient() {
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            ProgressBar myProgressBar = (ProgressBar) CaptivePortalLoginActivity.this.findViewById(R.id.progress_bar);
            myProgressBar.setProgress(newProgress);
            myProgressBar.setVisibility(newProgress == 100 ? 8 : 0);
        }
    }
}
