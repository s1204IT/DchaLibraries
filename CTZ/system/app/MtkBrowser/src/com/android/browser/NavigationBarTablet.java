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

/* loaded from: classes.dex */
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

    public NavigationBarTablet(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public NavigationBarTablet(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
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

    @Override // com.android.browser.NavigationBarBase, android.view.View
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

    @Override // android.view.View
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mHideNavButtons = this.mContext.getResources().getBoolean(R.bool.hide_nav_buttons);
        if (this.mUrlInput.hasFocus()) {
            if (this.mHideNavButtons && this.mNavButtons.getVisibility() == 0) {
                int measuredWidth = this.mNavButtons.getMeasuredWidth();
                this.mNavButtons.setVisibility(8);
                this.mNavButtons.setAlpha(0.0f);
                this.mNavButtons.setTranslationX(-measuredWidth);
                return;
            }
            if (!this.mHideNavButtons && this.mNavButtons.getVisibility() == 8) {
                this.mNavButtons.setVisibility(0);
                this.mNavButtons.setAlpha(1.0f);
                this.mNavButtons.setTranslationX(0.0f);
            }
        }
    }

    @Override // com.android.browser.NavigationBarBase
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

    @Override // com.android.browser.NavigationBarBase
    public void onTabDataChanged(Tab tab) {
        super.onTabDataChanged(tab);
        showHideStar(tab);
    }

    @Override // com.android.browser.NavigationBarBase
    public void setCurrentUrlIsBookmark(boolean z) {
        this.mStar.setActivated(z);
    }

    @Override // com.android.browser.NavigationBarBase, android.view.View.OnClickListener
    public void onClick(View view) {
        if (this.mBackButton == view && this.mUiController.getCurrentTab() != null) {
            this.mUiController.getCurrentTab().goBack();
            return;
        }
        if (this.mForwardButton == view && this.mUiController.getCurrentTab() != null) {
            this.mUiController.getCurrentTab().goForward();
            return;
        }
        if (this.mStar == view) {
            Intent intentCreateBookmarkCurrentPageIntent = this.mUiController.createBookmarkCurrentPageIntent(true);
            if (intentCreateBookmarkCurrentPageIntent != null) {
                getContext().startActivity(intentCreateBookmarkCurrentPageIntent);
                return;
            }
            return;
        }
        if (this.mAllButton == view) {
            this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
            return;
        }
        if (this.mSearchButton == view) {
            this.mBaseUi.editUrl(true, true);
            return;
        }
        if (this.mStopButton == view) {
            stopOrRefresh();
        } else if (this.mClearButton == view) {
            clearOrClose();
        } else {
            super.onClick(view);
        }
    }

    private void clearOrClose() {
        if (TextUtils.isEmpty(this.mUrlInput.getText())) {
            this.mUrlInput.clearFocus();
        } else {
            this.mUrlInput.setText("");
        }
    }

    @Override // com.android.browser.NavigationBarBase
    public void setFavicon(Bitmap bitmap) {
        this.mFaviconDrawable = this.mBaseUi.getFaviconDrawable(bitmap);
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

    @Override // com.android.browser.NavigationBarBase
    protected void setFocusState(boolean z) {
        super.setFocusState(z);
        if (z) {
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
        this.mUrlContainer.setBackgroundDrawable(z ? this.mFocusDrawable : this.mUnfocusDrawable);
    }

    private void stopOrRefresh() {
        if (this.mUiController == null) {
            return;
        }
        if (this.mTitleBar.isInLoad()) {
            this.mUiController.stopLoading();
        } else if (this.mUiController.getCurrentTopWebView() != null) {
            this.mUiController.getCurrentTopWebView().reload();
        }
    }

    @Override // com.android.browser.NavigationBarBase
    public void onProgressStarted() {
        this.mStopButton.setImageDrawable(this.mStopDrawable);
        this.mStopButton.setContentDescription(this.mStopDescription);
    }

    @Override // com.android.browser.NavigationBarBase
    public void onProgressStopped() {
        this.mStopButton.setImageDrawable(this.mReloadDrawable);
        this.mStopButton.setContentDescription(this.mRefreshDescription);
    }

    private void hideNavButtons() {
        if (this.mBaseUi.blockFocusAnimations()) {
            this.mNavButtons.setVisibility(8);
            return;
        }
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mNavButtons, (Property<View, Float>) View.TRANSLATION_X, 0.0f, -this.mNavButtons.getMeasuredWidth());
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this.mUrlContainer, "left", this.mUrlContainer.getLeft(), this.mUrlContainer.getPaddingLeft());
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mNavButtons, (Property<View, Float>) View.ALPHA, 1.0f, 0.0f);
        this.mAnimation = new AnimatorSet();
        this.mAnimation.playTogether(objectAnimatorOfFloat, objectAnimatorOfInt, objectAnimatorOfFloat2);
        this.mAnimation.addListener(new AnimatorListenerAdapter() { // from class: com.android.browser.NavigationBarTablet.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
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
            int measuredWidth = this.mNavButtons.getMeasuredWidth();
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mNavButtons, (Property<View, Float>) View.TRANSLATION_X, -measuredWidth, 0.0f);
            ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this.mUrlContainer, "left", 0, measuredWidth);
            ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mNavButtons, (Property<View, Float>) View.ALPHA, 0.0f, 1.0f);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfInt, objectAnimatorOfFloat2);
            animatorSet.setDuration(150L);
            animatorSet.start();
            return;
        }
        this.mNavButtons.setAlpha(1.0f);
    }

    private void showHideStar(Tab tab) {
        if (tab != null && tab.inForeground()) {
            int i = 0;
            if (DataUri.isDataUri(tab.getUrl())) {
                i = 8;
            }
            this.mStar.setVisibility(i);
        }
    }

    @Override // com.android.browser.UrlInputView.StateListener
    public void onStateChanged(int i) {
        switch (i) {
            case 0:
                this.mClearButton.setVisibility(8);
                break;
            case 1:
                this.mClearButton.setVisibility(8);
                if (this.mUiController != null) {
                    this.mUiController.supportsVoice();
                    break;
                }
                break;
            case 2:
                this.mClearButton.setVisibility(0);
                break;
        }
    }
}
