package com.android.browser;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import com.android.browser.UrlInputView;
import com.mediatek.browser.ext.IBrowserUrlExt;
import com.mediatek.browser.hotknot.HotKnotHandler;

public class NavigationBarPhone extends NavigationBarBase implements UrlInputView.StateListener, PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener, ViewTreeObserver.OnGlobalLayoutListener {
    private IBrowserUrlExt mBrowserUrlExt;
    private ImageView mClearButton;
    private View mComboIcon;
    private View mHotKnot;
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

    public NavigationBarPhone(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBrowserUrlExt = null;
    }

    public NavigationBarPhone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mBrowserUrlExt = null;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mStopButton = (ImageView) findViewById(R.id.stop);
        this.mStopButton.setOnClickListener(this);
        this.mClearButton = (ImageView) findViewById(R.id.clear);
        this.mClearButton.setOnClickListener(this);
        this.mMagnify = (ImageView) findViewById(R.id.magnify);
        this.mTabSwitcher = findViewById(R.id.tab_switcher);
        this.mTabSwitcher.setOnClickListener(this);
        this.mHotKnot = findViewById(R.id.hotknot);
        this.mHotKnot.setOnClickListener(this);
        this.mMore = findViewById(R.id.more);
        this.mMore.setOnClickListener(this);
        this.mComboIcon = findViewById(R.id.iconcombo);
        this.mComboIcon.setOnClickListener(this);
        this.mTitleContainer = findViewById(R.id.title_bg);
        setFocusState(false);
        Resources res = getContext().getResources();
        this.mStopDrawable = res.getDrawable(R.drawable.ic_stop_holo_dark);
        this.mRefreshDrawable = res.getDrawable(R.drawable.ic_refresh_holo_dark);
        this.mStopDescription = res.getString(R.string.accessibility_button_stop);
        this.mRefreshDescription = res.getString(R.string.accessibility_button_refresh);
        this.mTextfieldBgDrawable = res.getDrawable(R.drawable.textfield_active_holo_dark);
        this.mUrlInput.setContainer(this);
        this.mUrlInput.setStateListener(this);
        this.mNeedsMenu = !ViewConfiguration.get(getContext()).hasPermanentMenuKey();
        this.mIncognitoIcon = findViewById(R.id.incognito_icon);
    }

    @Override
    public void onProgressStarted() {
        super.onProgressStarted();
        if (this.mStopButton.getDrawable() == this.mStopDrawable) {
            return;
        }
        this.mStopButton.setImageDrawable(this.mStopDrawable);
        this.mStopButton.setContentDescription(this.mStopDescription);
        if (this.mStopButton.getVisibility() == 0) {
            return;
        }
        this.mComboIcon.setVisibility(8);
        this.mStopButton.setVisibility(0);
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
    void setDisplayTitle(String title) {
        this.mUrlInput.setTag(title);
        if (isEditingUrl()) {
            return;
        }
        if (title == null) {
            this.mUrlInput.setText(R.string.new_tab);
        } else if (title.startsWith("about:blank")) {
            this.mUrlInput.setText((CharSequence) UrlUtils.stripUrl("about:blank"), false);
        } else {
            this.mUrlInput.setText((CharSequence) UrlUtils.stripUrl(title), false);
        }
        this.mUrlInput.setSelection(0);
    }

    @Override
    public void onClick(View v) {
        if (v == this.mStopButton) {
            if (this.mTitleBar.isInLoad()) {
                this.mUiController.stopLoading();
                return;
            }
            WebView web = this.mBaseUi.getWebView();
            if (web == null) {
                return;
            }
            stopEditingUrl();
            web.reload();
            return;
        }
        if (v == this.mTabSwitcher) {
            ((PhoneUi) this.mBaseUi).toggleNavScreen();
            return;
        }
        if (this.mHotKnot == v) {
            showHotKnotShare();
            return;
        }
        if (this.mMore == v) {
            showMenu(this.mMore);
            return;
        }
        if (this.mClearButton == v) {
            this.mUrlInput.setText("");
        } else if (this.mComboIcon == v) {
            this.mUiController.showPageInfo();
        } else {
            super.onClick(v);
        }
    }

    @Override
    public boolean isMenuShowing() {
        if (super.isMenuShowing()) {
            return true;
        }
        return this.mOverflowMenuShowing;
    }

    public void dismissMenuOnly() {
        if (!isMenuShowing() || this.mPopupMenu == null) {
            return;
        }
        this.mPopupMenu.setOnDismissListener(null);
        this.mPopupMenu.dismiss();
        this.mPopupMenu.setOnDismissListener(this);
    }

    void showMenu(View anchor) {
        if (this.mOverflowMenuShowing) {
            return;
        }
        Activity activity = this.mUiController.getActivity();
        if (this.mPopupMenu == null) {
            this.mPopupMenu = new PopupMenu(this.mContext, anchor);
            this.mPopupMenu.setOnMenuItemClickListener(this);
            this.mPopupMenu.setOnDismissListener(this);
            anchor.getViewTreeObserver().addOnGlobalLayoutListener(this);
            if (!activity.onCreateOptionsMenu(this.mPopupMenu.getMenu())) {
                this.mPopupMenu = null;
                return;
            }
        }
        Menu menu = this.mPopupMenu.getMenu();
        if (!activity.onPrepareOptionsMenu(menu)) {
            return;
        }
        this.mOverflowMenuShowing = true;
        this.mPopupMenu.show();
    }

    @Override
    public void onDismiss(PopupMenu menu) {
        if (menu != this.mPopupMenu) {
            return;
        }
        onMenuHidden();
    }

    private void onMenuHidden() {
        this.mOverflowMenuShowing = false;
        this.mBaseUi.showTitleBarForDuration();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == this.mUrlInput) {
            String url = null;
            String title = null;
            Tab activeTab = this.mBaseUi.getActiveTab();
            if (activeTab == null) {
                activeTab = this.mBaseUi.mTabControl.getCurrentTab();
            }
            if (activeTab != null) {
                url = activeTab.getUrl();
                title = activeTab.getTitle();
            }
            this.mBrowserUrlExt = Extensions.getUrlPlugin(this.mUiController.getActivity());
            String text = this.mBrowserUrlExt.getOverrideFocusContent(hasFocus, this.mUrlInput.getText().toString(), (String) this.mUrlInput.getTag(), url);
            if (text != null) {
                this.mUrlInput.setText((CharSequence) text, false);
                this.mUrlInput.selectAll();
            } else {
                setDisplayTitle(this.mBrowserUrlExt.getOverrideFocusTitle(title, this.mUrlInput.getText().toString()));
            }
        }
        super.onFocusChange(view, hasFocus);
    }

    @Override
    public void onStateChanged(int state) {
        switch (state) {
            case 0:
                this.mComboIcon.setVisibility(0);
                this.mStopButton.setVisibility(8);
                this.mClearButton.setVisibility(8);
                this.mMagnify.setVisibility(8);
                this.mTabSwitcher.setVisibility(8);
                this.mTitleContainer.setBackgroundDrawable(null);
                showHotKnotButton();
                this.mMore.setVisibility(this.mNeedsMenu ? 0 : 8);
                break;
            case 1:
                this.mComboIcon.setVisibility(8);
                this.mStopButton.setVisibility(0);
                this.mClearButton.setVisibility(8);
                this.mMagnify.setVisibility(8);
                this.mTabSwitcher.setVisibility(8);
                this.mHotKnot.setVisibility(8);
                this.mMore.setVisibility(8);
                this.mTitleContainer.setBackgroundDrawable(this.mTextfieldBgDrawable);
                break;
            case 2:
                this.mComboIcon.setVisibility(8);
                this.mStopButton.setVisibility(8);
                this.mClearButton.setVisibility(0);
                this.mMagnify.setVisibility(0);
                this.mTabSwitcher.setVisibility(8);
                this.mHotKnot.setVisibility(8);
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
    public boolean onMenuItemClick(MenuItem item) {
        return this.mUiController.onOptionsItemSelected(item);
    }

    private void showHotKnotShare() {
        Tab activeTab = this.mBaseUi.getActiveTab();
        if (activeTab == null) {
            activeTab = this.mBaseUi.mTabControl.getCurrentTab();
        }
        if (activeTab == null) {
            return;
        }
        HotKnotHandler.hotKnotStart(activeTab.getUrl());
    }

    private void showHotKnotButton() {
        this.mHotKnot.setVisibility(8);
        if (!HotKnotHandler.isHotKnotSupported() || this.mBaseUi == null) {
            return;
        }
        Tab activeTab = this.mBaseUi.getActiveTab();
        if (activeTab == null) {
            activeTab = this.mBaseUi.mTabControl.getCurrentTab();
        }
        if (activeTab == null || activeTab.getUrl() == null || activeTab.getUrl().length() <= 0) {
            return;
        }
        String url = activeTab.getUrl();
        if (url.startsWith("content:") || url.startsWith("browser:") || url.startsWith("file:")) {
            return;
        }
        this.mHotKnot.setVisibility(0);
    }

    @Override
    public void onGlobalLayout() {
        if (!this.mOverflowMenuShowing || this.mPopupMenu == null) {
            return;
        }
        this.mPopupMenu.show();
    }
}
