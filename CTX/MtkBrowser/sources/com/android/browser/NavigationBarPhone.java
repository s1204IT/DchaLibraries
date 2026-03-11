package com.android.browser;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import com.android.browser.UrlInputView;
import com.mediatek.browser.ext.IBrowserUrlExt;

public class NavigationBarPhone extends NavigationBarBase implements ViewTreeObserver.OnGlobalLayoutListener, PopupMenu.OnDismissListener, PopupMenu.OnMenuItemClickListener, UrlInputView.StateListener {
    private IBrowserUrlExt mBrowserUrlExt;
    private ImageView mClearButton;
    private View mComboIcon;
    private View mIncognitoIcon;
    private ImageView mMagnify;
    private View mMore;
    private boolean mNeedsMenu;
    private boolean mOverflowMenuShowing;
    private PopupMenu mPopupMenu;
    private String mRefreshDescription;
    private Drawable mRefreshDrawable;
    private ImageView mStopButton;
    private String mStopDescription;
    private Drawable mStopDrawable;
    private View mTabSwitcher;
    private Drawable mTextfieldBgDrawable;
    private View mTitleContainer;

    public NavigationBarPhone(Context context) {
        super(context);
        this.mBrowserUrlExt = null;
    }

    public NavigationBarPhone(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBrowserUrlExt = null;
    }

    public NavigationBarPhone(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mBrowserUrlExt = null;
    }

    private void onMenuHidden() {
        this.mOverflowMenuShowing = false;
        this.mBaseUi.showTitleBarForDuration();
    }

    public void dismissMenuOnly() {
        if (!isMenuShowing() || this.mPopupMenu == null) {
            return;
        }
        this.mPopupMenu.setOnDismissListener(null);
        this.mPopupMenu.dismiss();
        this.mPopupMenu.setOnDismissListener(this);
    }

    @Override
    public boolean isMenuShowing() {
        return super.isMenuShowing() || this.mOverflowMenuShowing;
    }

    @Override
    public void onClick(View view) {
        if (view == this.mStopButton) {
            if (this.mTitleBar.isInLoad()) {
                this.mUiController.stopLoading();
                return;
            }
            WebView webView = this.mBaseUi.getWebView();
            if (webView != null) {
                stopEditingUrl();
                webView.reload();
                return;
            }
            return;
        }
        if (view == this.mTabSwitcher) {
            ((PhoneUi) this.mBaseUi).toggleNavScreen();
            return;
        }
        if (this.mMore == view) {
            showMenu(this.mMore);
            return;
        }
        if (this.mClearButton == view) {
            this.mUrlInput.setText("");
        } else if (this.mComboIcon == view) {
            this.mUiController.showPageInfo();
        } else {
            super.onClick(view);
        }
    }

    @Override
    public void onDismiss(PopupMenu popupMenu) {
        if (popupMenu == this.mPopupMenu) {
            onMenuHidden();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mStopButton = (ImageView) findViewById(2131558541);
        this.mStopButton.setOnClickListener(this);
        this.mClearButton = (ImageView) findViewById(2131558543);
        this.mClearButton.setOnClickListener(this);
        this.mMagnify = (ImageView) findViewById(2131558538);
        this.mTabSwitcher = findViewById(2131558544);
        this.mTabSwitcher.setOnClickListener(this);
        this.mMore = findViewById(2131558495);
        this.mMore.setOnClickListener(this);
        this.mComboIcon = findViewById(2131558540);
        this.mComboIcon.setOnClickListener(this);
        this.mTitleContainer = findViewById(2131558537);
        setFocusState(false);
        Resources resources = getContext().getResources();
        this.mStopDrawable = resources.getDrawable(2130837590);
        this.mRefreshDrawable = resources.getDrawable(2130837577);
        this.mStopDescription = resources.getString(2131493294);
        this.mRefreshDescription = resources.getString(2131493293);
        this.mTextfieldBgDrawable = resources.getDrawable(2130837610);
        this.mUrlInput.setContainer(this);
        this.mUrlInput.setStateListener(this);
        this.mNeedsMenu = !ViewConfiguration.get(getContext()).hasPermanentMenuKey();
        this.mIncognitoIcon = findViewById(2131558539);
    }

    @Override
    public void onFocusChange(View view, boolean z) {
        String title;
        String url;
        if (view == this.mUrlInput) {
            Tab activeTab = this.mBaseUi.getActiveTab();
            if (activeTab == null) {
                activeTab = this.mBaseUi.mTabControl.getCurrentTab();
            }
            if (activeTab != null) {
                url = activeTab.getUrl();
                title = activeTab.getTitle();
            } else {
                title = null;
                url = null;
            }
            this.mBrowserUrlExt = Extensions.getUrlPlugin(this.mUiController.getActivity());
            String overrideFocusContent = this.mBrowserUrlExt.getOverrideFocusContent(z, this.mUrlInput.getText().toString(), (String) this.mUrlInput.getTag(), url);
            if (overrideFocusContent != null) {
                this.mUrlInput.setText((CharSequence) overrideFocusContent, false);
                this.mUrlInput.selectAll();
            } else {
                setDisplayTitle(this.mBrowserUrlExt.getOverrideFocusTitle(title, this.mUrlInput.getText().toString()));
            }
        }
        super.onFocusChange(view, z);
    }

    @Override
    public void onGlobalLayout() {
        if (!this.mOverflowMenuShowing || this.mPopupMenu == null) {
            return;
        }
        this.mPopupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return this.mUiController.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onProgressStarted() {
        super.onProgressStarted();
        if (this.mStopButton.getDrawable() != this.mStopDrawable) {
            this.mStopButton.setImageDrawable(this.mStopDrawable);
            this.mStopButton.setContentDescription(this.mStopDescription);
            if (this.mStopButton.getVisibility() != 0) {
                this.mComboIcon.setVisibility(8);
                this.mStopButton.setVisibility(0);
            }
        }
    }

    @Override
    public void onProgressStopped() {
        super.onProgressStopped();
        this.mStopButton.setImageDrawable(this.mRefreshDrawable);
        this.mStopButton.setContentDescription(this.mRefreshDescription);
        if (!isEditingUrl()) {
            this.mComboIcon.setVisibility(0);
        }
        onStateChanged(this.mUrlInput.getState());
    }

    @Override
    public void onStateChanged(int i) {
        switch (i) {
            case 0:
                this.mComboIcon.setVisibility(0);
                this.mStopButton.setVisibility(8);
                this.mClearButton.setVisibility(8);
                this.mMagnify.setVisibility(8);
                this.mTabSwitcher.setVisibility(8);
                this.mTitleContainer.setBackgroundDrawable(null);
                this.mMore.setVisibility(this.mNeedsMenu ? 0 : 8);
                break;
            case 1:
                this.mComboIcon.setVisibility(8);
                this.mStopButton.setVisibility(0);
                this.mClearButton.setVisibility(8);
                this.mMagnify.setVisibility(8);
                this.mTabSwitcher.setVisibility(8);
                this.mMore.setVisibility(8);
                this.mTitleContainer.setBackgroundDrawable(this.mTextfieldBgDrawable);
                break;
            case 2:
                this.mComboIcon.setVisibility(8);
                this.mStopButton.setVisibility(8);
                this.mClearButton.setVisibility(0);
                this.mMagnify.setVisibility(0);
                this.mTabSwitcher.setVisibility(8);
                this.mMore.setVisibility(8);
                this.mTitleContainer.setBackgroundDrawable(this.mTextfieldBgDrawable);
                break;
        }
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        super.onTabDataChanged(tab);
        this.mIncognitoIcon.setVisibility(tab.isPrivateBrowsingEnabled() ? 0 : 8);
    }

    @Override
    void setDisplayTitle(String str) {
        this.mUrlInput.setTag(str);
        if (isEditingUrl()) {
            return;
        }
        if (str == null) {
            this.mUrlInput.setText(2131492949);
        } else if (str.startsWith("about:blank")) {
            this.mUrlInput.setText((CharSequence) UrlUtils.stripUrl("about:blank"), false);
        } else {
            this.mUrlInput.setText((CharSequence) UrlUtils.stripUrl(str), false);
        }
        this.mUrlInput.setSelection(0);
    }

    void showMenu(View view) {
        if (this.mOverflowMenuShowing) {
            return;
        }
        Activity activity = this.mUiController.getActivity();
        if (this.mPopupMenu == null) {
            this.mPopupMenu = new PopupMenu(this.mContext, view);
            this.mPopupMenu.setOnMenuItemClickListener(this);
            this.mPopupMenu.setOnDismissListener(this);
            view.getViewTreeObserver().addOnGlobalLayoutListener(this);
            if (!activity.onCreateOptionsMenu(this.mPopupMenu.getMenu())) {
                this.mPopupMenu = null;
                return;
            }
        }
        if (activity.onPrepareOptionsMenu(this.mPopupMenu.getMenu())) {
            this.mOverflowMenuShowing = true;
            this.mPopupMenu.show();
        }
    }
}
