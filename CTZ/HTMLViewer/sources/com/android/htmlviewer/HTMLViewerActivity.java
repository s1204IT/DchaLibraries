package com.android.htmlviewer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.zip.GZIPInputStream;

public class HTMLViewerActivity extends Activity {
    static final boolean $assertionsDisabled = false;
    private Intent mIntent;
    private View mLoading;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.main);
        this.mWebView = (WebView) findViewById(R.id.webview);
        this.mLoading = findViewById(R.id.loading);
        this.mWebView.setWebChromeClient(new ChromeClient());
        this.mWebView.setWebViewClient(new ViewClient());
        WebSettings settings = this.mWebView.getSettings();
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSavePassword(false);
        settings.setSaveFormData(false);
        settings.setBlockNetworkLoads(true);
        settings.setJavaScriptEnabled(false);
        settings.setDefaultTextEncodingName("utf-8");
        this.mIntent = getIntent();
        requestPermissionAndLoad();
    }

    private void loadUrl() {
        if (this.mIntent.hasExtra("android.intent.extra.TITLE")) {
            setTitle(this.mIntent.getStringExtra("android.intent.extra.TITLE"));
        }
        this.mWebView.loadUrl(String.valueOf(this.mIntent.getData()));
    }

    private void requestPermissionAndLoad() {
        Uri data = this.mIntent.getData();
        if (data != null) {
            if ("file".equals(data.getScheme()) && -1 == checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE")) {
                requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 0);
            } else {
                loadUrl();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (iArr.length == 1 && iArr[0] == 0) {
            loadUrl();
        } else {
            Toast.makeText(this, R.string.turn_on_storage_permission, 0).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mWebView.destroy();
    }

    private class ChromeClient extends WebChromeClient {
        private ChromeClient() {
        }

        @Override
        public void onReceivedTitle(WebView webView, String str) {
            if (!HTMLViewerActivity.this.getIntent().hasExtra("android.intent.extra.TITLE")) {
                HTMLViewerActivity.this.setTitle(str);
            }
        }
    }

    private class ViewClient extends WebViewClient {
        private ViewClient() {
        }

        @Override
        public void onPageFinished(WebView webView, String str) {
            HTMLViewerActivity.this.mLoading.setVisibility(8);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String str) {
            try {
                Intent uri = Intent.parseUri(str, 1);
                uri.addCategory("android.intent.category.BROWSABLE");
                uri.setComponent(null);
                Intent selector = uri.getSelector();
                if (selector != null) {
                    selector.addCategory("android.intent.category.BROWSABLE");
                    selector.setComponent(null);
                }
                uri.putExtra("com.android.browser.application_id", webView.getContext().getPackageName());
                try {
                    webView.getContext().startActivity(uri);
                } catch (ActivityNotFoundException e) {
                    Log.w("HTMLViewer", "No application can handle " + str);
                    Toast.makeText(HTMLViewerActivity.this, R.string.cannot_open_link, 0).show();
                }
                return true;
            } catch (URISyntaxException e2) {
                Log.w("HTMLViewer", "Bad URI " + str + ": " + e2.getMessage());
                Toast.makeText(HTMLViewerActivity.this, R.string.cannot_open_link, 0).show();
                return true;
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
            Uri url = webResourceRequest.getUrl();
            if ("file".equals(url.getScheme()) && url.getPath().endsWith(".gz")) {
                Log.d("HTMLViewer", "Trying to decompress " + url + " on the fly");
                try {
                    WebResourceResponse webResourceResponse = new WebResourceResponse(HTMLViewerActivity.this.getIntent().getType(), "utf-8", new GZIPInputStream(HTMLViewerActivity.this.getContentResolver().openInputStream(url)));
                    webResourceResponse.setStatusCodeAndReasonPhrase(200, "OK");
                    return webResourceResponse;
                } catch (IOException e) {
                    Log.w("HTMLViewer", "Failed to decompress; falling back", e);
                    return null;
                }
            }
            return null;
        }
    }
}
