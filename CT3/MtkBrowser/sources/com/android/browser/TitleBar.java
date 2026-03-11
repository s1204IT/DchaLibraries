package com.android.browser;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Region;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class TitleBar extends RelativeLayout {
    private AccessibilityManager mAccessibilityManager;
    private AutologinBar mAutoLogin;
    private BaseUi mBaseUi;
    private FrameLayout mContentView;
    private Animator.AnimatorListener mHideTileBarAnimatorListener;
    private boolean mInLoad;
    private boolean mIsFixedTitleBar;
    private NavigationBarBase mNavBar;
    private PageProgressView mProgress;
    private boolean mShowing;
    private boolean mSkipTitleBarAnimations;
    private int mSlop;
    private Animator mTitleBarAnimator;
    private UiController mUiController;
    private boolean mUseQuickControls;

    public TitleBar(Context context, UiController controller, BaseUi ui, FrameLayout contentView) {
        super(context, null);
        this.mHideTileBarAnimatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                TitleBar.this.onScrollChanged();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        };
        this.mUiController = controller;
        this.mBaseUi = ui;
        this.mContentView = contentView;
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        ViewConfiguration config = ViewConfiguration.get(this.mUiController.getActivity());
        this.mSlop = config.getScaledTouchSlop();
        initLayout(context);
        setFixedTitleBar();
    }

    private void initLayout(Context context) {
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.title_bar, this);
        this.mProgress = (PageProgressView) findViewById(R.id.progress);
        this.mNavBar = (NavigationBarBase) findViewById(R.id.taburlbar);
        this.mNavBar.setTitleBar(this);
    }

    private void inflateAutoLoginBar() {
        if (this.mAutoLogin != null) {
            return;
        }
        ViewStub stub = (ViewStub) findViewById(R.id.autologin_stub);
        this.mAutoLogin = (AutologinBar) stub.inflate();
        this.mAutoLogin.setTitleBar(this);
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        setFixedTitleBar();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mIsFixedTitleBar) {
            int margin = getMeasuredHeight() - calculateEmbeddedHeight();
            this.mBaseUi.setContentViewMarginTop(-margin);
        } else {
            this.mBaseUi.setContentViewMarginTop(0);
        }
    }

    @Override
    public boolean gatherTransparentRegion(Region region) {
        if (region != null) {
            int[] location = new int[2];
            getLocationInWindow(location);
            region.op(0, 0, (location[0] + this.mRight) - this.mLeft, (location[1] + this.mBottom) - this.mTop, Region.Op.DIFFERENCE);
        }
        return true;
    }

    private void setFixedTitleBar() {
        ViewGroup parent = (ViewGroup) getParent();
        if (!this.mIsFixedTitleBar || parent == null) {
            this.mIsFixedTitleBar = true;
            setSkipTitleBarAnimations(true);
            show();
            setSkipTitleBarAnimations(false);
            if (parent != null) {
                parent.removeView(this);
            }
            if (this.mIsFixedTitleBar) {
                this.mBaseUi.addFixedTitleBar(this);
            } else {
                this.mContentView.addView(this, makeLayoutParams());
                this.mBaseUi.setContentViewMarginTop(0);
            }
        }
    }

    public BaseUi getUi() {
        return this.mBaseUi;
    }

    public UiController getUiController() {
        return this.mUiController;
    }

    void setSkipTitleBarAnimations(boolean skip) {
        this.mSkipTitleBarAnimations = skip;
    }

    void setupTitleBarAnimator(Animator animator) {
        Resources res = this.mContext.getResources();
        int duration = res.getInteger(R.integer.titlebar_animation_duration);
        animator.setInterpolator(new DecelerateInterpolator(2.5f));
        animator.setDuration(duration);
    }

    void show() {
        cancelTitleBarAnimation(false);
        setLayerType(2, null);
        if (this.mSkipTitleBarAnimations) {
            setVisibility(0);
            setTranslationY(0.0f);
        } else {
            int visibleHeight = getVisibleTitleHeight();
            float startPos = (-getEmbeddedHeight()) + visibleHeight;
            if (getTranslationY() != 0.0f) {
                startPos = Math.max(startPos, getTranslationY());
            }
            this.mTitleBarAnimator = ObjectAnimator.ofFloat(this, "translationY", startPos, 0.0f);
            setupTitleBarAnimator(this.mTitleBarAnimator);
            this.mTitleBarAnimator.start();
        }
        this.mShowing = true;
    }

    void hide() {
        if (this.mIsFixedTitleBar) {
            return;
        }
        if (this.mUseQuickControls) {
            setVisibility(8);
        } else if (!this.mSkipTitleBarAnimations) {
            cancelTitleBarAnimation(false);
            int visibleHeight = getVisibleTitleHeight();
            this.mTitleBarAnimator = ObjectAnimator.ofFloat(this, "translationY", getTranslationY(), (-getEmbeddedHeight()) + visibleHeight);
            this.mTitleBarAnimator.addListener(this.mHideTileBarAnimatorListener);
            setupTitleBarAnimator(this.mTitleBarAnimator);
            this.mTitleBarAnimator.start();
        } else {
            onScrollChanged();
        }
        this.mShowing = false;
    }

    boolean isShowing() {
        return this.mShowing;
    }

    void cancelTitleBarAnimation(boolean reset) {
        if (this.mTitleBarAnimator != null) {
            this.mTitleBarAnimator.cancel();
            this.mTitleBarAnimator = null;
        }
        if (!reset) {
            return;
        }
        setTranslationY(0.0f);
    }

    public int getVisibleTitleHeight() {
        Tab tab = this.mBaseUi.getActiveTab();
        WebView webview = tab != null ? tab.getWebView() : null;
        if (webview != null) {
            return webview.getVisibleTitleHeight();
        }
        return 0;
    }

    public void setProgress(int newProgress) {
        if (newProgress >= 100) {
            this.mProgress.setProgress(10000);
            this.mProgress.setVisibility(8);
            this.mInLoad = false;
            this.mNavBar.onProgressStopped();
            if (isEditingUrl() || wantsToBeVisible() || this.mUseQuickControls) {
                return;
            }
            this.mBaseUi.showTitleBarForDuration();
            return;
        }
        if (!this.mInLoad) {
            this.mProgress.setVisibility(0);
            this.mInLoad = true;
            this.mNavBar.onProgressStarted();
        }
        this.mProgress.setProgress((newProgress * 10000) / 100);
        if (this.mShowing) {
            return;
        }
        show();
    }

    public int getEmbeddedHeight() {
        if (this.mIsFixedTitleBar) {
            return 0;
        }
        return calculateEmbeddedHeight();
    }

    private int calculateEmbeddedHeight() {
        int height = this.mNavBar.getHeight();
        if (this.mAutoLogin != null && this.mAutoLogin.getVisibility() == 0) {
            return height + this.mAutoLogin.getHeight();
        }
        return height;
    }

    public void updateAutoLogin(Tab tab, boolean animate) {
        if (this.mAutoLogin == null) {
            if (tab.getDeviceAccountLogin() == null) {
                return;
            } else {
                inflateAutoLoginBar();
            }
        }
        this.mAutoLogin.updateAutoLogin(tab, animate);
    }

    public void showAutoLogin(boolean animate) {
        if (this.mUseQuickControls) {
            this.mBaseUi.showTitleBar();
        }
        if (this.mAutoLogin == null) {
            inflateAutoLoginBar();
        }
        this.mAutoLogin.setVisibility(0);
        if (!animate) {
            return;
        }
        this.mAutoLogin.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.autologin_enter));
    }

    public void hideAutoLogin(boolean animate) {
        if (this.mUseQuickControls) {
            this.mAutoLogin.setVisibility(8);
            this.mBaseUi.refreshWebView();
        } else if (animate) {
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.autologin_exit);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation a) {
                    TitleBar.this.mAutoLogin.setVisibility(8);
                    TitleBar.this.mBaseUi.refreshWebView();
                }

                @Override
                public void onAnimationStart(Animation a) {
                }

                @Override
                public void onAnimationRepeat(Animation a) {
                }
            });
            this.mAutoLogin.startAnimation(anim);
        } else {
            if (this.mAutoLogin.getAnimation() != null) {
                return;
            }
            this.mAutoLogin.setVisibility(8);
            this.mBaseUi.refreshWebView();
        }
    }

    public boolean wantsToBeVisible() {
        return inAutoLogin();
    }

    private boolean inAutoLogin() {
        return this.mAutoLogin != null && this.mAutoLogin.getVisibility() == 0;
    }

    public boolean isEditingUrl() {
        return this.mNavBar.isEditingUrl();
    }

    public WebView getCurrentWebView() {
        Tab t = this.mBaseUi.getActiveTab();
        if (t != null) {
            return t.getWebView();
        }
        return null;
    }

    public PageProgressView getProgressView() {
        return this.mProgress;
    }

    public NavigationBarBase getNavigationBar() {
        return this.mNavBar;
    }

    public boolean useQuickControls() {
        return this.mUseQuickControls;
    }

    public boolean isInLoad() {
        return this.mInLoad;
    }

    private ViewGroup.LayoutParams makeLayoutParams() {
        return new FrameLayout.LayoutParams(-1, -2);
    }

    @Override
    public View focusSearch(View focused, int dir) {
        WebView web = getCurrentWebView();
        if (130 == dir && hasFocus() && web != null && web.hasFocusable() && web.getParent() != null) {
            return web;
        }
        return super.focusSearch(focused, dir);
    }

    public void onTabDataChanged(Tab tab) {
        this.mNavBar.setVisibility(0);
    }

    public void onScrollChanged() {
        if (this.mShowing || this.mIsFixedTitleBar) {
            return;
        }
        int unVisibleHeight = getVisibleTitleHeight() - getEmbeddedHeight();
        setTranslationY(unVisibleHeight);
        if (unVisibleHeight > (-this.mSlop)) {
            show();
            this.mBaseUi.showBottomBarForDuration(2000L);
        } else {
            if (unVisibleHeight >= (-this.mSlop)) {
                return;
            }
            this.mBaseUi.hideBottomBar();
        }
    }

    public void onResume() {
        setFixedTitleBar();
    }
}
