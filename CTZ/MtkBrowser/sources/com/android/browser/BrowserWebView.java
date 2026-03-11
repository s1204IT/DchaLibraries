package com.android.browser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.RenderNode;
import android.view.View;
import android.view.ViewRootImpl;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserWebView extends WebView {
    private String LOGTAG;
    private boolean mBackgroundRemoved;
    private OnScrollChangedListener mOnScrollChangedListener;
    private String mSiteNavHitURL;
    private TitleBar mTitleBar;
    private WebChromeClient mWebChromeClient;
    private WebViewClient mWebViewClient;

    public interface OnScrollChangedListener {
        void onScrollChanged(int i, int i2, int i3, int i4);
    }

    public BrowserWebView(Context context, AttributeSet attributeSet, int i, boolean z) {
        super(context, attributeSet, i, z);
        this.mBackgroundRemoved = false;
        this.LOGTAG = "BrowserWebView";
    }

    @Override
    public void destroy() {
        BrowserSettings.getInstance().stopManagingSettings(getSettings());
        super.destroy();
        RenderNode renderNodeUpdateDisplayListIfDirty = updateDisplayListIfDirty();
        if (renderNodeUpdateDisplayListIfDirty != null) {
            renderNodeUpdateDisplayListIfDirty.discardDisplayList();
        }
    }

    public void drawContent(Canvas canvas) {
        onDraw(canvas);
    }

    public String getSiteNavHitURL() {
        String str;
        synchronized (this) {
            str = this.mSiteNavHitURL;
        }
        return str;
    }

    public int getTitleHeight() {
        if (this.mTitleBar != null) {
            return this.mTitleBar.getEmbeddedHeight();
        }
        return 0;
    }

    @Override
    public WebChromeClient getWebChromeClient() {
        return this.mWebChromeClient;
    }

    @Override
    public WebViewClient getWebViewClient() {
        return this.mWebViewClient;
    }

    protected void onDetachedFromWindowInternal() {
        super.onDetachedFromWindowInternal();
        ViewRootImpl viewRootImpl = getViewRootImpl();
        if (viewRootImpl != null) {
            viewRootImpl.detachFunctor(0L);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mBackgroundRemoved || getRootView().getBackground() == null) {
            return;
        }
        this.mBackgroundRemoved = true;
        post(new Runnable(this) {
            final BrowserWebView this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void run() {
                this.this$0.getRootView().setBackgroundDrawable(null);
            }
        });
    }

    @Override
    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        if (this.mTitleBar != null) {
            this.mTitleBar.onScrollChanged();
        }
        if (this.mOnScrollChangedListener != null) {
            this.mOnScrollChangedListener.onScrollChanged(i, i2, i3, i4);
        }
    }

    @Override
    public void setLayerType(int i, Paint paint) {
        if (getWebViewProvider() == null) {
            return;
        }
        super.setLayerType(i, paint);
    }

    public void setOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        this.mOnScrollChangedListener = onScrollChangedListener;
    }

    public void setSiteNavHitURL(String str) {
        synchronized (this) {
            this.mSiteNavHitURL = str;
        }
    }

    public void setTitleBar(TitleBar titleBar) {
        this.mTitleBar = titleBar;
    }

    @Override
    public void setWebChromeClient(WebChromeClient webChromeClient) {
        this.mWebChromeClient = webChromeClient;
        super.setWebChromeClient(webChromeClient);
    }

    @Override
    public void setWebViewClient(WebViewClient webViewClient) {
        this.mWebViewClient = webViewClient;
        super.setWebViewClient(webViewClient);
    }

    @Override
    public boolean showContextMenuForChild(View view) {
        return false;
    }
}
