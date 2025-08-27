package com.android.browser;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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

/* loaded from: classes.dex */
public class NavigationBarBase extends LinearLayout implements TextWatcher, View.OnClickListener, View.OnFocusChangeListener, UrlInputView.UrlInputListener {
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

    public NavigationBarBase(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mInVoiceMode = false;
        this.mBrowserUrlExt = null;
    }

    public NavigationBarBase(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mInVoiceMode = false;
        this.mBrowserUrlExt = null;
    }

    public UrlInputView getUrlInputView() {
        return this.mUrlInput;
    }

    @Override // android.view.View
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
        InputFilter[] inputFilterArrCheckUrlLengthLimit = this.mBrowserUrlExt.checkUrlLengthLimit(this.mContext);
        if (inputFilterArrCheckUrlLengthLimit != null) {
            this.mUrlInput.setFilters(inputFilterArrCheckUrlLengthLimit);
        }
    }

    public void setTitleBar(TitleBar titleBar) {
        this.mTitleBar = titleBar;
        this.mBaseUi = this.mTitleBar.getUi();
        this.mUiController = this.mTitleBar.getUiController();
        this.mUrlInput.setController(this.mUiController);
    }

    public void setLock(Drawable drawable) {
        if (this.mLockIcon == null) {
            return;
        }
        if (drawable == null) {
            this.mLockIcon.setVisibility(8);
        } else {
            this.mLockIcon.setImageDrawable(drawable);
            this.mLockIcon.setVisibility(0);
        }
    }

    public void setFavicon(Bitmap bitmap) {
        if (this.mFavicon == null) {
            return;
        }
        this.mFavicon.setImageDrawable(this.mBaseUi.getFaviconDrawable(bitmap));
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
    }

    @Override // android.view.View.OnFocusChangeListener
    public void onFocusChange(View view, boolean z) throws Resources.NotFoundException {
        Tab currentTab;
        if (z || view.isInTouchMode() || this.mUrlInput.needsUpdate()) {
            setFocusState(z);
        }
        if (z) {
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

    protected void setFocusState(boolean z) {
    }

    public boolean isEditingUrl() {
        return this.mUrlInput.hasFocus();
    }

    void stopEditingUrl() {
        WebView currentTopWebView = this.mUiController.getCurrentTopWebView();
        if (currentTopWebView != null) {
            currentTopWebView.requestFocus();
        }
    }

    void setDisplayTitle(String str) {
        if (!isEditingUrl()) {
            if (str.startsWith("about:blank")) {
                this.mUrlInput.setText((CharSequence) "about:blank", false);
            } else {
                this.mUrlInput.setText((CharSequence) str, false);
            }
        }
    }

    void setIncognitoMode(boolean z) {
        this.mUrlInput.setIncognitoMode(z);
    }

    void clearCompletions() {
        this.mUrlInput.dismissDropDown();
    }

    @Override // com.android.browser.UrlInputView.UrlInputListener
    public void onAction(String str, String str2, String str3) {
        stopEditingUrl();
        if ("browser-type".equals(str3)) {
            String strSmartUrlFilter = UrlUtils.smartUrlFilter(str, false);
            Tab activeTab = this.mBaseUi.getActiveTab();
            if (strSmartUrlFilter != null && activeTab != null && strSmartUrlFilter.startsWith("javascript:")) {
                this.mUiController.loadUrl(activeTab, strSmartUrlFilter);
                setDisplayTitle(str);
                return;
            }
        }
        Intent intent = new Intent();
        if (str != null && str.startsWith("content://")) {
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse(str));
        } else if (str != null && str.startsWith("rtsp://")) {
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse(str.replaceAll(" ", "%20")));
            intent.addFlags(268435456);
        } else if (str != null && str.startsWith("wtai://wp/mc;")) {
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("tel:" + str.substring("wtai://wp/mc;".length())));
        } else {
            if (str != null && str.startsWith("file://")) {
                return;
            }
            intent.setAction("android.intent.action.SEARCH");
            intent.putExtra("query", str);
            if (str2 != null) {
                intent.putExtra("intent_extra_data_key", str2);
            }
            if (str3 != null) {
                Bundle bundle = new Bundle();
                bundle.putString("source", str3);
                intent.putExtra("app_data", bundle);
            }
        }
        this.mUiController.handleNewIntent(intent);
        setDisplayTitle(str);
    }

    @Override // com.android.browser.UrlInputView.UrlInputListener
    public void onDismiss() throws Resources.NotFoundException {
        final Tab activeTab = this.mBaseUi.getActiveTab();
        this.mBaseUi.hideTitleBar();
        post(new Runnable() { // from class: com.android.browser.NavigationBarBase.1
            @Override // java.lang.Runnable
            public void run() {
                NavigationBarBase.this.clearFocus();
                if (activeTab != null) {
                    NavigationBarBase.this.setDisplayTitle(activeTab.getUrl());
                }
            }
        });
    }

    @Override // com.android.browser.UrlInputView.UrlInputListener
    public void onCopySuggestion(String str) {
        this.mUrlInput.setText((CharSequence) str, true);
        if (str != null) {
            this.mUrlInput.setSelection(str.length());
        }
    }

    public void setCurrentUrlIsBookmark(boolean z) {
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchKeyEventPreIme(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == 4) {
            stopEditingUrl();
            return true;
        }
        return super.dispatchKeyEventPreIme(keyEvent);
    }

    void startEditingUrl(boolean z, boolean z2) {
        setVisibility(0);
        if (this.mTitleBar.useQuickControls()) {
            this.mTitleBar.getProgressView().setVisibility(8);
        }
        if (!this.mUrlInput.hasFocus()) {
            this.mUrlInput.requestFocus();
        }
        if (z) {
            this.mUrlInput.setText("");
        }
        if (z2) {
            this.mUrlInput.showIME();
        }
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

    public void onVoiceResult(String str) {
        startEditingUrl(true, true);
        onCopySuggestion(str);
    }

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
    }
}
