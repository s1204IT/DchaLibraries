package com.android.browser;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.browser.UrlInputView;
import com.mediatek.browser.ext.IBrowserUrlExt;

public class NavigationBarBase extends LinearLayout implements View.OnClickListener, UrlInputView.UrlInputListener, View.OnFocusChangeListener, TextWatcher {
    protected BaseUi mBaseUi;
    private IBrowserUrlExt mBrowserUrlExt;
    private ImageView mFavicon;
    protected boolean mInVoiceMode;
    private ImageView mLockIcon;
    protected TitleBar mTitleBar;
    protected UiController mUiController;
    protected UrlInputView mUrlInput;

    public NavigationBarBase(Context context) {
        super(context);
        this.mInVoiceMode = false;
        this.mBrowserUrlExt = null;
    }

    public NavigationBarBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mInVoiceMode = false;
        this.mBrowserUrlExt = null;
    }

    public NavigationBarBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mInVoiceMode = false;
        this.mBrowserUrlExt = null;
    }

    public UrlInputView getUrlInputView() {
        return this.mUrlInput;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mLockIcon = (ImageView) findViewById(R.id.lock);
        this.mFavicon = (ImageView) findViewById(R.id.favicon);
        this.mUrlInput = (UrlInputView) findViewById(R.id.url);
        this.mUrlInput.setUrlInputListener(this);
        this.mUrlInput.setOnFocusChangeListener(this);
        this.mUrlInput.setSelectAllOnFocus(true);
        this.mUrlInput.addTextChangedListener(this);
        this.mBrowserUrlExt = Extensions.getUrlPlugin(this.mContext);
        InputFilter[] contentFilters = this.mBrowserUrlExt.checkUrlLengthLimit(this.mContext);
        if (contentFilters == null) {
            return;
        }
        this.mUrlInput.setFilters(contentFilters);
    }

    public void setTitleBar(TitleBar titleBar) {
        this.mTitleBar = titleBar;
        this.mBaseUi = this.mTitleBar.getUi();
        this.mUiController = this.mTitleBar.getUiController();
        this.mUrlInput.setController(this.mUiController);
    }

    public void setLock(Drawable d) {
        if (this.mLockIcon == null) {
            return;
        }
        if (d == null) {
            this.mLockIcon.setVisibility(8);
        } else {
            this.mLockIcon.setImageDrawable(d);
            this.mLockIcon.setVisibility(0);
        }
    }

    public void setFavicon(Bitmap icon) {
        if (this.mFavicon == null) {
            return;
        }
        this.mFavicon.setImageDrawable(this.mBaseUi.getFaviconDrawable(icon));
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        Tab currentTab;
        if (hasFocus || view.isInTouchMode() || this.mUrlInput.needsUpdate()) {
            setFocusState(hasFocus);
        }
        if (hasFocus) {
            this.mBaseUi.showTitleBar();
        } else if (!this.mUrlInput.needsUpdate()) {
            this.mUrlInput.dismissDropDown();
            this.mUrlInput.hideIME();
            if (this.mUrlInput.getText().length() == 0 && (currentTab = this.mUiController.getTabControl().getCurrentTab()) != null) {
                setDisplayTitle(currentTab.getUrl());
            }
            this.mBaseUi.suggestHideTitleBar();
        }
        this.mUrlInput.clearNeedsUpdate();
    }

    protected void setFocusState(boolean focus) {
    }

    public boolean isEditingUrl() {
        return this.mUrlInput.hasFocus();
    }

    void stopEditingUrl() {
        WebView currentTopWebView = this.mUiController.getCurrentTopWebView();
        if (currentTopWebView == null) {
            return;
        }
        currentTopWebView.requestFocus();
    }

    void setDisplayTitle(String title) {
        if (isEditingUrl()) {
            return;
        }
        if (title.startsWith("about:blank")) {
            this.mUrlInput.setText((CharSequence) "about:blank", false);
        } else {
            this.mUrlInput.setText((CharSequence) title, false);
        }
    }

    void setIncognitoMode(boolean incognito) {
        this.mUrlInput.setIncognitoMode(incognito);
    }

    void clearCompletions() {
        this.mUrlInput.dismissDropDown();
    }

    @Override
    public void onAction(String text, String extra, String source) {
        stopEditingUrl();
        if ("browser-type".equals(source)) {
            String url = UrlUtils.smartUrlFilter(text, false);
            Tab t = this.mBaseUi.getActiveTab();
            if (url != null && t != null && url.startsWith("javascript:")) {
                this.mUiController.loadUrl(t, url);
                setDisplayTitle(text);
                return;
            }
        }
        Intent i = new Intent();
        if (text != null && text.startsWith("content://")) {
            i.setAction("android.intent.action.VIEW");
            i.setData(Uri.parse(text));
        } else if (text != null && text.startsWith("rtsp://")) {
            i.setAction("android.intent.action.VIEW");
            i.setData(Uri.parse(text.replaceAll(" ", "%20")));
            i.addFlags(268435456);
        } else if (text != null && text.startsWith("wtai://wp/mc;")) {
            i.setAction("android.intent.action.VIEW");
            i.setData(Uri.parse("tel:" + text.substring("wtai://wp/mc;".length())));
        } else {
            if (text != null && text.startsWith("file://")) {
                return;
            }
            i.setAction("android.intent.action.SEARCH");
            i.putExtra("query", text);
            if (extra != null) {
                i.putExtra("intent_extra_data_key", extra);
            }
            if (source != null) {
                Bundle appData = new Bundle();
                appData.putString("source", source);
                i.putExtra("app_data", appData);
            }
        }
        this.mUiController.handleNewIntent(i);
        setDisplayTitle(text);
    }

    @Override
    public void onDismiss() {
        final Tab currentTab = this.mBaseUi.getActiveTab();
        this.mBaseUi.hideTitleBar();
        post(new Runnable() {
            @Override
            public void run() {
                NavigationBarBase.this.clearFocus();
                if (currentTab == null) {
                    return;
                }
                NavigationBarBase.this.setDisplayTitle(currentTab.getUrl());
            }
        });
    }

    @Override
    public void onCopySuggestion(String text) {
        this.mUrlInput.setText((CharSequence) text, true);
        if (text == null) {
            return;
        }
        this.mUrlInput.setSelection(text.length());
    }

    public void setCurrentUrlIsBookmark(boolean isBookmark) {
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent evt) {
        if (evt.getKeyCode() == 4) {
            stopEditingUrl();
            return true;
        }
        return super.dispatchKeyEventPreIme(evt);
    }

    void startEditingUrl(boolean clearInput, boolean forceIME) {
        setVisibility(0);
        if (this.mTitleBar.useQuickControls()) {
            this.mTitleBar.getProgressView().setVisibility(8);
        }
        if (!this.mUrlInput.hasFocus()) {
            this.mUrlInput.requestFocus();
        }
        if (clearInput) {
            this.mUrlInput.setText("");
        }
        if (!forceIME) {
            return;
        }
        this.mUrlInput.showIME();
    }

    public void onProgressStarted() {
    }

    public void onProgressStopped() {
    }

    public boolean isMenuShowing() {
        return false;
    }

    public void onTabDataChanged(Tab tab) {
    }

    public void onVoiceResult(String s) {
        startEditingUrl(true, true);
        onCopySuggestion(s);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
