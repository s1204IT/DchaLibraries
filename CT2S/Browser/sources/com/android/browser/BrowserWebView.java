package com.android.browser;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserWebView extends WebView {
    private boolean mBackgroundRemoved;
    private OnScrollChangedListener mOnScrollChangedListener;
    private TitleBar mTitleBar;
    private WebChromeClient mWebChromeClient;
    private WebViewClient mWebViewClient;

    public interface OnScrollChangedListener {
        void onScrollChanged(int i, int i2, int i3, int i4);
    }

    public BrowserWebView(Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing) {
        super(context, attrs, defStyle, privateBrowsing);
        this.mBackgroundRemoved = false;
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        this.mWebChromeClient = client;
        super.setWebChromeClient(client);
    }

    @Override
    public WebChromeClient getWebChromeClient() {
        return this.mWebChromeClient;
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        this.mWebViewClient = client;
        super.setWebViewClient(client);
    }

    @Override
    public WebViewClient getWebViewClient() {
        return this.mWebViewClient;
    }

    public void setTitleBar(TitleBar title) {
        this.mTitleBar = title;
    }

    public int getTitleHeight() {
        if (this.mTitleBar != null) {
            return this.mTitleBar.getEmbeddedHeight();
        }
        return 0;
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (!this.mBackgroundRemoved && getRootView().getBackground() != null) {
            this.mBackgroundRemoved = true;
            post(new Runnable() {
                @Override
                public void run() {
                    BrowserWebView.this.getRootView().setBackgroundDrawable(null);
                }
            });
        }
    }

    public void drawContent(Canvas c) {
        onDraw(c);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (this.mTitleBar != null) {
            this.mTitleBar.onScrollChanged();
        }
        if (this.mOnScrollChangedListener != null) {
            this.mOnScrollChangedListener.onScrollChanged(l, t, oldl, oldt);
        }
    }

    public void setOnScrollChangedListener(OnScrollChangedListener listener) {
        this.mOnScrollChangedListener = listener;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        return false;
    }

    @Override
    public void destroy() {
        BrowserSettings.getInstance().stopManagingSettings(getSettings());
        super.destroy();
    }
}
