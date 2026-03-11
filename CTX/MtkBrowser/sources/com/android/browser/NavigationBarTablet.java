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

    public NavigationBarTablet(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public NavigationBarTablet(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    private void clearOrClose() {
        if (TextUtils.isEmpty(this.mUrlInput.getText())) {
            this.mUrlInput.clearFocus();
        } else {
            this.mUrlInput.setText("");
        }
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
        this.mAnimation.addListener(new AnimatorListenerAdapter(this) {
            final NavigationBarTablet this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                this.this$0.mNavButtons.setVisibility(8);
                this.this$0.mAnimation = null;
            }
        });
        this.mAnimation.setDuration(150L);
        this.mAnimation.start();
    }

    private void init(Context context) {
        Resources resources = context.getResources();
        this.mStopDrawable = resources.getDrawable(2130837590);
        this.mReloadDrawable = resources.getDrawable(2130837577);
        this.mStopDescription = resources.getString(2131493294);
        this.mRefreshDescription = resources.getString(2131493293);
        this.mFocusDrawable = resources.getDrawable(2130837610);
        this.mUnfocusDrawable = resources.getDrawable(2130837611);
        this.mHideNavButtons = resources.getBoolean(2131296258);
    }

    private void showHideStar(Tab tab) {
        if (tab == null || !tab.inForeground()) {
            return;
        }
        this.mStar.setVisibility(DataUri.isDataUri(tab.getUrl()) ? 8 : 0);
    }

    private void showNavButtons() {
        if (this.mAnimation != null) {
            this.mAnimation.cancel();
        }
        this.mNavButtons.setVisibility(0);
        this.mNavButtons.setTranslationX(0.0f);
        if (this.mBaseUi.blockFocusAnimations()) {
            this.mNavButtons.setAlpha(1.0f);
            return;
        }
        int measuredWidth = this.mNavButtons.getMeasuredWidth();
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mNavButtons, (Property<View, Float>) View.TRANSLATION_X, -measuredWidth, 0.0f);
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this.mUrlContainer, "left", 0, measuredWidth);
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mNavButtons, (Property<View, Float>) View.ALPHA, 0.0f, 1.0f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfInt, objectAnimatorOfFloat2);
        animatorSet.setDuration(150L);
        animatorSet.start();
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

    @Override
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

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mHideNavButtons = this.mContext.getResources().getBoolean(2131296258);
        if (this.mUrlInput.hasFocus()) {
            if (this.mHideNavButtons && this.mNavButtons.getVisibility() == 0) {
                int measuredWidth = this.mNavButtons.getMeasuredWidth();
                this.mNavButtons.setVisibility(8);
                this.mNavButtons.setAlpha(0.0f);
                this.mNavButtons.setTranslationX(-measuredWidth);
                return;
            }
            if (this.mHideNavButtons || this.mNavButtons.getVisibility() != 8) {
                return;
            }
            this.mNavButtons.setVisibility(0);
            this.mNavButtons.setAlpha(1.0f);
            this.mNavButtons.setTranslationX(0.0f);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mAllButton = findViewById(2131558549);
        this.mNavButtons = findViewById(2131558545);
        this.mBackButton = (ImageButton) findViewById(2131558441);
        this.mForwardButton = (ImageButton) findViewById(2131558442);
        this.mUrlIcon = (ImageView) findViewById(2131558547);
        this.mStar = (ImageView) findViewById(2131558487);
        this.mStopButton = (ImageView) findViewById(2131558541);
        this.mSearchButton = (ImageView) findViewById(2131558548);
        this.mClearButton = findViewById(2131558543);
        this.mUrlContainer = findViewById(2131558546);
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
    public void onProgressStarted() {
        this.mStopButton.setImageDrawable(this.mStopDrawable);
        this.mStopButton.setContentDescription(this.mStopDescription);
    }

    @Override
    public void onProgressStopped() {
        this.mStopButton.setImageDrawable(this.mReloadDrawable);
        this.mStopButton.setContentDescription(this.mRefreshDescription);
    }

    @Override
    public void onStateChanged(int i) {
        switch (i) {
            case 0:
                this.mClearButton.setVisibility(8);
                break;
            case 1:
                this.mClearButton.setVisibility(8);
                if (this.mUiController != null) {
                    this.mUiController.supportsVoice();
                }
                break;
            case 2:
                this.mClearButton.setVisibility(0);
                break;
        }
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        super.onTabDataChanged(tab);
        showHideStar(tab);
    }

    @Override
    public void setCurrentUrlIsBookmark(boolean z) {
        this.mStar.setActivated(z);
    }

    @Override
    public void setFavicon(Bitmap bitmap) {
        this.mFaviconDrawable = this.mBaseUi.getFaviconDrawable(bitmap);
        updateUrlIcon();
    }

    @Override
    protected void setFocusState(boolean z) {
        super.setFocusState(z);
        if (z) {
            if (this.mHideNavButtons) {
                hideNavButtons();
            }
            this.mSearchButton.setVisibility(8);
            this.mStar.setVisibility(8);
            this.mUrlIcon.setImageResource(2130837584);
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

    @Override
    public void setTitleBar(TitleBar titleBar) {
        super.setTitleBar(titleBar);
    }

    void updateNavigationState(Tab tab) {
        if (tab != null) {
            this.mBackButton.setImageResource(tab.canGoBack() ? 2130837534 : 2130837532);
            this.mForwardButton.setImageResource(tab.canGoForward() ? 2130837555 : 2130837554);
        }
        updateUrlIcon();
    }

    void updateUrlIcon() {
        if (this.mUrlInput.hasFocus()) {
            this.mUrlIcon.setImageResource(2130837584);
            return;
        }
        if (this.mFaviconDrawable == null) {
            this.mFaviconDrawable = this.mBaseUi.getFaviconDrawable(null);
        }
        this.mUrlIcon.setImageDrawable(this.mFaviconDrawable);
    }
}
