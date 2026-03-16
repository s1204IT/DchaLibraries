package com.android.managedprovisioning;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends Activity {
    private WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mWebView = new WebView(this);
        String extraUrl = getIntent().getStringExtra("extra_url");
        final String extraAllowedUrlBase = getIntent().getStringExtra("extra_allowed_url_base");
        if (extraUrl == null) {
            ProvisionLogger.loge("No url provided to WebActivity.");
            finish();
        } else {
            this.mWebView.loadUrl(extraUrl);
            this.mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (extraAllowedUrlBase != null && url.startsWith(extraAllowedUrlBase)) {
                        view.loadUrl(url);
                        return true;
                    }
                    return true;
                }
            });
            setContentView(this.mWebView);
        }
    }
}
