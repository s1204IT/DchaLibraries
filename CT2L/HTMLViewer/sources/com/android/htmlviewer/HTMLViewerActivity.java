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
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.zip.GZIPInputStream;

public class HTMLViewerActivity extends Activity {
    private View mLoading;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.mWebView = (WebView) findViewById(R.id.webview);
        this.mLoading = findViewById(R.id.loading);
        this.mWebView.setWebChromeClient(new ChromeClient());
        this.mWebView.setWebViewClient(new ViewClient());
        WebSettings s = this.mWebView.getSettings();
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSavePassword(false);
        s.setSaveFormData(false);
        s.setBlockNetworkLoads(true);
        s.setJavaScriptEnabled(false);
        s.setDefaultTextEncodingName("utf-8");
        Intent intent = getIntent();
        if (intent.hasExtra("android.intent.extra.TITLE")) {
            setTitle(intent.getStringExtra("android.intent.extra.TITLE"));
        }
        this.mWebView.loadUrl(String.valueOf(intent.getData()));
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
        public void onReceivedTitle(WebView view, String title) {
            if (!HTMLViewerActivity.this.getIntent().hasExtra("android.intent.extra.TITLE")) {
                HTMLViewerActivity.this.setTitle(title);
            }
        }
    }

    private class ViewClient extends WebViewClient {
        private ViewClient() {
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            HTMLViewerActivity.this.mLoading.setVisibility(8);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if ("file".equals(uri.getScheme()) && uri.getPath().endsWith(".gz")) {
                Log.d("HTMLViewer", "Trying to decompress " + uri + " on the fly");
                try {
                    InputStream in = new GZIPInputStream(HTMLViewerActivity.this.getContentResolver().openInputStream(uri));
                    WebResourceResponse resp = new WebResourceResponse(HTMLViewerActivity.this.getIntent().getType(), "utf-8", in);
                    resp.setStatusCodeAndReasonPhrase(200, "OK");
                    return resp;
                } catch (IOException e) {
                    Log.w("HTMLViewer", "Failed to decompress; falling back", e);
                }
            }
            return null;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                Intent intent = Intent.parseUri(url, 1);
                intent.addCategory("android.intent.category.BROWSABLE");
                intent.setComponent(null);
                Intent selector = intent.getSelector();
                if (selector != null) {
                    selector.addCategory("android.intent.category.BROWSABLE");
                    selector.setComponent(null);
                }
                intent.putExtra("com.android.browser.application_id", view.getContext().getPackageName());
                try {
                    view.getContext().startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    Log.w("HTMLViewer", "No application can handle " + url);
                    return false;
                }
            } catch (URISyntaxException ex) {
                Log.w("HTMLViewer", "Bad URI " + url + ": " + ex.getMessage());
                return false;
            }
        }
    }
}
