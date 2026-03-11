package com.android.browser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.android.browser.UI;
import com.android.browser.UrlInputView;

public class NavigationBarTablet extends NavigationBarBase implements UrlInputView.StateListener {
    private View mAllButton;
    private AnimatorSet mAnimation;
    private ImageButton mBackButton;
    private View mClearButton;
    private Drawable mFaviconDrawable;
    private Drawable mFocusDrawable;
    private ImageButton mForwardButton;
    private boolean mHideNavButtons;
    private View mNavButtons;
    private String mRefreshDescription;
    private Drawable mReloadDrawable;
    private ImageView mSearchButton;
    private ImageView mStar;
    private ImageView mStopButton;
    private String mStopDescription;
    private Drawable mStopDrawable;
    private Drawable mUnfocusDrawable;
    private View mUrlContainer;
    private ImageView mUrlIcon;

    public NavigationBarTablet(Context context) {
        super(context);
        init(context);
    }

    public NavigationBarTablet(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NavigationBarTablet(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        Resources resources = context.getResources();
        this.mStopDrawable = resources.getDrawable(R.drawable.ic_stop_holo_dark);
        this.mReloadDrawable = resources.getDrawable(R.drawable.ic_refresh_holo_dark);
        this.mStopDescription = resources.getString(R.string.accessibility_button_stop);
        this.mRefreshDescription = resources.getString(R.string.accessibility_button_refresh);
        this.mFocusDrawable = resources.getDrawable(R.drawable.textfield_active_holo_dark);
        this.mUnfocusDrawable = resources.getDrawable(R.drawable.textfield_default_holo_dark);
        this.mHideNavButtons = resources.getBoolean(R.bool.hide_nav_buttons);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAllButton = findViewById(R.id.all_btn);
        this.mNavButtons = findViewById(R.id.navbuttons);
        this.mBackButton = (ImageButton) findViewById(R.id.back);
        this.mForwardButton = (ImageButton) findViewById(R.id.forward);
        this.mUrlIcon = (ImageView) findViewById(R.id.url_icon);
        this.mStar = (ImageView) findViewById(R.id.star);
        this.mStopButton = (ImageView) findViewById(R.id.stop);
        this.mSearchButton = (ImageView) findViewById(R.id.search);
        this.mClearButton = findViewById(R.id.clear);
        this.mUrlContainer = findViewById(R.id.urlbar_focused);
        this.mBackButton.setOnClickListener(this);
        this.mForwardButton.setOnClickListener(this);
        this.mStar.setOnClickListener(this);
        this.mAllButton.setOnClickListener(this);
        this.mStopButton.setOnClickListener(this);
        this.mSearchButton.setOnClickListener(this);
        this.mClearButton.setOnClickListener(this);
        this.mUrlInput.setContainer(this.mUrlContainer);
        this.mUrlInput.setStateListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        Resources res = this.mContext.getResources();
        this.mHideNavButtons = res.getBoolean(R.bool.hide_nav_buttons);
        if (!this.mUrlInput.hasFocus()) {
            return;
        }
        if (this.mHideNavButtons && this.mNavButtons.getVisibility() == 0) {
            int aw = this.mNavButtons.getMeasuredWidth();
            this.mNavButtons.setVisibility(8);
            this.mNavButtons.setAlpha(0.0f);
            this.mNavButtons.setTranslationX(-aw);
            return;
        }
        if (this.mHideNavButtons || this.mNavButtons.getVisibility() != 8) {
            return;
        }
        this.mNavButtons.setVisibility(0);
        this.mNavButtons.setAlpha(1.0f);
        this.mNavButtons.setTranslationX(0.0f);
    }

    @Override
    public void setTitleBar(TitleBar titleBar) {
        super.setTitleBar(titleBar);
    }

    void updateNavigationState(Tab tab) {
        int i;
        int i2;
        if (tab != null) {
            ImageButton imageButton = this.mBackButton;
            if (tab.canGoBack()) {
                i = R.drawable.ic_back_holo_dark;
            } else {
                i = R.drawable.ic_back_disabled_holo_dark;
            }
            imageButton.setImageResource(i);
            ImageButton imageButton2 = this.mForwardButton;
            if (tab.canGoForward()) {
                i2 = R.drawable.ic_forward_holo_dark;
            } else {
                i2 = R.drawable.ic_forward_disabled_holo_dark;
            }
            imageButton2.setImageResource(i2);
        }
        updateUrlIcon();
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        super.onTabDataChanged(tab);
        showHideStar(tab);
    }

    @Override
    public void setCurrentUrlIsBookmark(boolean isBookmark) {
        this.mStar.setActivated(isBookmark);
    }

    @Override
    public void onClick(View v) {
        if (this.mBackButton == v && this.mUiController.getCurrentTab() != null) {
            this.mUiController.getCurrentTab().goBack();
            return;
        }
        if (this.mForwardButton == v && this.mUiController.getCurrentTab() != null) {
            this.mUiController.getCurrentTab().goForward();
            return;
        }
        if (this.mStar == v) {
            Intent intent = this.mUiController.createBookmarkCurrentPageIntent(true);
            if (intent == null) {
                return;
            }
            getContext().startActivity(intent);
            return;
        }
        if (this.mAllButton == v) {
            this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
            return;
        }
        if (this.mSearchButton == v) {
            this.mBaseUi.editUrl(true, true);
            return;
        }
        if (this.mStopButton == v) {
            stopOrRefresh();
        } else if (this.mClearButton == v) {
            clearOrClose();
        } else {
            super.onClick(v);
        }
    }

    private void clearOrClose() {
        if (TextUtils.isEmpty(this.mUrlInput.getText())) {
            this.mUrlInput.clearFocus();
        } else {
            this.mUrlInput.setText("");
        }
    }

    @Override
    public void setFavicon(Bitmap icon) {
        this.mFaviconDrawable = this.mBaseUi.getFaviconDrawable(icon);
        updateUrlIcon();
    }

    void updateUrlIcon() {
        if (this.mUrlInput.hasFocus()) {
            this.mUrlIcon.setImageResource(R.drawable.ic_search_holo_dark);
            return;
        }
        if (this.mFaviconDrawable == null) {
            this.mFaviconDrawable = this.mBaseUi.getFaviconDrawable(null);
        }
        this.mUrlIcon.setImageDrawable(this.mFaviconDrawable);
    }

    @Override
    protected void setFocusState(boolean focus) {
        super.setFocusState(focus);
        if (focus) {
            if (this.mHideNavButtons) {
                hideNavButtons();
            }
            this.mSearchButton.setVisibility(8);
            this.mStar.setVisibility(8);
            this.mUrlIcon.setImageResource(R.drawable.ic_search_holo_dark);
        } else {
            if (this.mHideNavButtons) {
                showNavButtons();
            }
            showHideStar(this.mUiController.getCurrentTab());
            if (this.mTitleBar.useQuickControls()) {
                this.mSearchButton.setVisibility(8);
            } else {
                this.mSearchButton.setVisibility(0);
            }
            updateUrlIcon();
        }
        this.mUrlContainer.setBackgroundDrawable(focus ? this.mFocusDrawable : this.mUnfocusDrawable);
    }

    private void stopOrRefresh() {
        if (this.mUiController == null) {
            return;
        }
        if (this.mTitleBar.isInLoad()) {
            this.mUiController.stopLoading();
        } else {
            if (this.mUiController.getCurrentTopWebView() == null) {
                return;
            }
            this.mUiController.getCurrentTopWebView().reload();
        }
    }

    @Override
    public void onProgressStarted() {
        this.mStopButton.setImageDrawable(this.mStopDrawable);
        this.mStopButton.setContentDescription(this.mStopDescription);
    }

    @Override
    public void onProgressStopped() {
        this.mStopButton.setImageDrawable(this.mReloadDrawable);
        this.mStopButton.setContentDescription(this.mRefreshDescription);
    }

    private void hideNavButtons() {
        if (this.mBaseUi.blockFocusAnimations()) {
            this.mNavButtons.setVisibility(8);
            return;
        }
        int awidth = this.mNavButtons.getMeasuredWidth();
        Animator anim1 = ObjectAnimator.ofFloat(this.mNavButtons, (Property<View, Float>) View.TRANSLATION_X, 0.0f, -awidth);
        Animator anim2 = ObjectAnimator.ofInt(this.mUrlContainer, "left", this.mUrlContainer.getLeft(), this.mUrlContainer.getPaddingLeft());
        Animator anim3 = ObjectAnimator.ofFloat(this.mNavButtons, (Property<View, Float>) View.ALPHA, 1.0f, 0.0f);
        this.mAnimation = new AnimatorSet();
        this.mAnimation.playTogether(anim1, anim2, anim3);
        this.mAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                NavigationBarTablet.this.mNavButtons.setVisibility(8);
                NavigationBarTablet.this.mAnimation = null;
            }
        });
        this.mAnimation.setDuration(150L);
        this.mAnimation.start();
    }

    private void showNavButtons() {
        if (this.mAnimation != null) {
            this.mAnimation.cancel();
        }
        this.mNavButtons.setVisibility(0);
        this.mNavButtons.setTranslationX(0.0f);
        if (!this.mBaseUi.blockFocusAnimations()) {
            int awidth = this.mNavButtons.getMeasuredWidth();
            Animator anim1 = ObjectAnimator.ofFloat(this.mNavButtons, (Property<View, Float>) View.TRANSLATION_X, -awidth, 0.0f);
            Animator anim2 = ObjectAnimator.ofInt(this.mUrlContainer, "left", 0, awidth);
            Animator anim3 = ObjectAnimator.ofFloat(this.mNavButtons, (Property<View, Float>) View.ALPHA, 0.0f, 1.0f);
            AnimatorSet combo = new AnimatorSet();
            combo.playTogether(anim1, anim2, anim3);
            combo.setDuration(150L);
            combo.start();
            return;
        }
        this.mNavButtons.setAlpha(1.0f);
    }

    private void showHideStar(Tab tab) {
        if (tab == null || !tab.inForeground()) {
            return;
        }
        int starVisibility = 0;
        String url = tab.getUrl();
        if (DataUri.isDataUri(url)) {
            starVisibility = 8;
        }
        this.mStar.setVisibility(starVisibility);
    }

    @Override
    public void onStateChanged(int state) {
        switch (state) {
            case 0:
                this.mClearButton.setVisibility(8);
                break;
            case 1:
                this.mClearButton.setVisibility(8);
                if (this.mUiController == null || this.mUiController.supportsVoice()) {
                }
                break;
            case 2:
                this.mClearButton.setVisibility(0);
                break;
        }
    }
}
