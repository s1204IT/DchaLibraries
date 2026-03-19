package com.android.managedprovisioning.preprovisioning;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.ProvisionLogger;
import com.android.managedprovisioning.common.SettingsFacade;
import com.android.managedprovisioning.common.SetupLayoutActivity;

public class WebActivity extends SetupLayoutActivity {
    private SettingsFacade mSettingsFacade = new SettingsFacade();
    private WebView mWebView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String stringExtra = getIntent().getStringExtra("extra_url");
        if (stringExtra == null) {
            Toast.makeText(this, R.string.url_error, 0).show();
            ProvisionLogger.loge("No url provided to WebActivity.");
            finish();
        }
        Bundle extras = getIntent().getExtras();
        if (extras.containsKey("extra_status_bar_color")) {
            setStatusBarColor(extras.getInt("extra_status_bar_color"));
        }
        this.mWebView = new WebView(this);
        this.mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
                if (!URLUtil.isHttpsUrl(webResourceRequest.getUrl().toString())) {
                    ProvisionLogger.loge("Secure connection required, but insecure URL requested explicitly, or as a part of the page.");
                    return WebActivity.this.createNewSecurityErrorResponse();
                }
                return super.shouldInterceptRequest(webView, webResourceRequest);
            }
        });
        this.mWebView.loadUrl(stringExtra);
        WebSettings settings = this.mWebView.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptEnabled(true);
        if (!this.mSettingsFacade.isUserSetupCompleted(this)) {
            this.mWebView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public final boolean onLongClick(View view) {
                    return WebActivity.lambda$onCreate$0(view);
                }
            });
        }
        setContentView(this.mWebView);
    }

    static boolean lambda$onCreate$0(View view) {
        return true;
    }

    private WebResourceResponse createNewSecurityErrorResponse() {
        WebResourceResponse webResourceResponse = new WebResourceResponse("text/plain", "UTF-8", null);
        webResourceResponse.setStatusCodeAndReasonPhrase(403, "Secure connection required");
        return webResourceResponse;
    }

    @Override
    protected int getMetricsCategory() {
        return 522;
    }

    @Override
    public void onBackPressed() {
        if (this.mWebView.canGoBack()) {
            this.mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public static Intent createIntent(Context context, String str, int i) {
        if (URLUtil.isNetworkUrl(str)) {
            return new Intent(context, (Class<?>) WebActivity.class).putExtra("extra_url", str).putExtra("extra_status_bar_color", i);
        }
        return null;
    }
}
