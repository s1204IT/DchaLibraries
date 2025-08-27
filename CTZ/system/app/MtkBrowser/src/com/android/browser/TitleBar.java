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

/* loaded from: classes.dex */
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

    public TitleBar(Context context, UiController uiController, BaseUi baseUi, FrameLayout frameLayout) throws Resources.NotFoundException {
        super(context, null);
        this.mHideTileBarAnimatorListener = new Animator.AnimatorListener() { // from class: com.android.browser.TitleBar.1
            AnonymousClass1() {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) throws Resources.NotFoundException {
                TitleBar.this.onScrollChanged();
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
            }
        };
        this.mUiController = uiController;
        this.mBaseUi = baseUi;
        this.mContentView = frameLayout;
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        this.mSlop = ViewConfiguration.get(this.mUiController.getActivity()).getScaledTouchSlop();
        initLayout(context);
        setFixedTitleBar();
    }

    private void initLayout(Context context) {
        LayoutInflater.from(context).inflate(R.layout.title_bar, this);
        this.mProgress = (PageProgressView) findViewById(R.id.progress);
        this.mNavBar = (NavigationBarBase) findViewById(R.id.taburlbar);
        this.mNavBar.setTitleBar(this);
    }

    private void inflateAutoLoginBar() {
        if (this.mAutoLogin != null) {
            return;
        }
        this.mAutoLogin = (AutologinBar) ((ViewStub) findViewById(R.id.autologin_stub)).inflate();
        this.mAutoLogin.setTitleBar(this);
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration configuration) throws Resources.NotFoundException {
        super.onConfigurationChanged(configuration);
        setFixedTitleBar();
    }

    @Override // android.widget.RelativeLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        if (this.mIsFixedTitleBar) {
            this.mBaseUi.setContentViewMarginTop(-(getMeasuredHeight() - calculateEmbeddedMeasuredHeight()));
        } else {
            this.mBaseUi.setContentViewMarginTop(0);
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean gatherTransparentRegion(Region region) {
        if (region != null) {
            int[] iArr = new int[2];
            getLocationInWindow(iArr);
            region.op(0, 0, (iArr[0] + this.mRight) - this.mLeft, (iArr[1] + this.mBottom) - this.mTop, Region.Op.DIFFERENCE);
        }
        return true;
    }

    private void setFixedTitleBar() throws Resources.NotFoundException {
        ViewGroup viewGroup = (ViewGroup) getParent();
        if (!this.mIsFixedTitleBar || viewGroup == null) {
            this.mIsFixedTitleBar = true;
            setSkipTitleBarAnimations(true);
            show();
            setSkipTitleBarAnimations(false);
            if (viewGroup != null) {
                viewGroup.removeView(this);
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

    void setSkipTitleBarAnimations(boolean z) {
        this.mSkipTitleBarAnimations = z;
    }

    void setupTitleBarAnimator(Animator animator) throws Resources.NotFoundException {
        int integer = this.mContext.getResources().getInteger(R.integer.titlebar_animation_duration);
        animator.setInterpolator(new DecelerateInterpolator(2.5f));
        animator.setDuration(integer);
    }

    void show() throws Resources.NotFoundException {
        cancelTitleBarAnimation(false);
        setLayerType(2, null);
        if (this.mSkipTitleBarAnimations) {
            setVisibility(0);
            setTranslationY(0.0f);
        } else {
            float visibleTitleHeight = (-getEmbeddedHeight()) + getVisibleTitleHeight();
            if (getTranslationY() != 0.0f) {
                visibleTitleHeight = Math.max(visibleTitleHeight, getTranslationY());
            }
            this.mTitleBarAnimator = ObjectAnimator.ofFloat(this, "translationY", visibleTitleHeight, 0.0f);
            setupTitleBarAnimator(this.mTitleBarAnimator);
            this.mTitleBarAnimator.start();
        }
        this.mShowing = true;
    }

    void hide() throws Resources.NotFoundException {
        if (this.mIsFixedTitleBar) {
            return;
        }
        if (this.mUseQuickControls) {
            setVisibility(8);
        } else if (!this.mSkipTitleBarAnimations) {
            cancelTitleBarAnimation(false);
            this.mTitleBarAnimator = ObjectAnimator.ofFloat(this, "translationY", getTranslationY(), (-getEmbeddedHeight()) + getVisibleTitleHeight());
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

    void cancelTitleBarAnimation(boolean z) {
        if (this.mTitleBarAnimator != null) {
            this.mTitleBarAnimator.cancel();
            this.mTitleBarAnimator = null;
        }
        if (z) {
            setTranslationY(0.0f);
        }
    }

    /* renamed from: com.android.browser.TitleBar$1 */
    class AnonymousClass1 implements Animator.AnimatorListener {
        AnonymousClass1() {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) throws Resources.NotFoundException {
            TitleBar.this.onScrollChanged();
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
        }
    }

    public int getVisibleTitleHeight() {
        Tab activeTab = this.mBaseUi.getActiveTab();
        WebView webView = activeTab != null ? activeTab.getWebView() : null;
        if (webView != null) {
            return webView.getVisibleTitleHeight();
        }
        return 0;
    }

    public void setProgress(int i) throws Resources.NotFoundException {
        if (i >= 100) {
            this.mProgress.setProgress(10000);
            this.mProgress.setVisibility(8);
            this.mInLoad = false;
            this.mNavBar.onProgressStopped();
            if (!isEditingUrl() && !wantsToBeVisible() && !this.mUseQuickControls) {
                this.mBaseUi.showTitleBarForDuration();
                return;
            }
            return;
        }
        if (!this.mInLoad) {
            this.mProgress.setVisibility(0);
            this.mInLoad = true;
            this.mNavBar.onProgressStarted();
        }
        this.mProgress.setProgress((i * 10000) / 100);
        if (!this.mShowing) {
            show();
        }
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

    private int calculateEmbeddedMeasuredHeight() {
        int measuredHeight = this.mNavBar.getMeasuredHeight();
        if (this.mAutoLogin != null && this.mAutoLogin.getVisibility() == 0) {
            return measuredHeight + this.mAutoLogin.getMeasuredHeight();
        }
        return measuredHeight;
    }

    public void updateAutoLogin(Tab tab, boolean z) {
        if (this.mAutoLogin == null) {
            if (tab.getDeviceAccountLogin() == null) {
                return;
            } else {
                inflateAutoLoginBar();
            }
        }
        this.mAutoLogin.updateAutoLogin(tab, z);
    }

    public void showAutoLogin(boolean z) {
        if (this.mUseQuickControls) {
            this.mBaseUi.showTitleBar();
        }
        if (this.mAutoLogin == null) {
            inflateAutoLoginBar();
        }
        this.mAutoLogin.setVisibility(0);
        if (z) {
            this.mAutoLogin.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.autologin_enter));
        }
    }

    public void hideAutoLogin(boolean z) throws Resources.NotFoundException {
        if (this.mUseQuickControls) {
            this.mAutoLogin.setVisibility(8);
            this.mBaseUi.refreshWebView();
        } else if (z) {
            Animation animationLoadAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.autologin_exit);
            animationLoadAnimation.setAnimationListener(new Animation.AnimationListener() { // from class: com.android.browser.TitleBar.2
                AnonymousClass2() {
                }

                @Override // android.view.animation.Animation.AnimationListener
                public void onAnimationEnd(Animation animation) {
                    TitleBar.this.mAutoLogin.setVisibility(8);
                    TitleBar.this.mBaseUi.refreshWebView();
                }

                @Override // android.view.animation.Animation.AnimationListener
                public void onAnimationStart(Animation animation) {
                }

                @Override // android.view.animation.Animation.AnimationListener
                public void onAnimationRepeat(Animation animation) {
                }
            });
            this.mAutoLogin.startAnimation(animationLoadAnimation);
        } else if (this.mAutoLogin.getAnimation() == null) {
            this.mAutoLogin.setVisibility(8);
            this.mBaseUi.refreshWebView();
        }
    }

    /* renamed from: com.android.browser.TitleBar$2 */
    class AnonymousClass2 implements Animation.AnimationListener {
        AnonymousClass2() {
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationEnd(Animation animation) {
            TitleBar.this.mAutoLogin.setVisibility(8);
            TitleBar.this.mBaseUi.refreshWebView();
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationStart(Animation animation) {
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationRepeat(Animation animation) {
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
        Tab activeTab = this.mBaseUi.getActiveTab();
        if (activeTab != null) {
            return activeTab.getWebView();
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

    @Override // android.view.ViewGroup, android.view.ViewParent
    public View focusSearch(View view, int i) {
        WebView currentWebView = getCurrentWebView();
        if (130 == i && hasFocus() && currentWebView != null && currentWebView.hasFocusable() && currentWebView.getParent() != null) {
            return currentWebView;
        }
        return super.focusSearch(view, i);
    }

    public void onTabDataChanged(Tab tab) {
        this.mNavBar.setVisibility(0);
    }

    public void onScrollChanged() throws Resources.NotFoundException {
        if (!this.mShowing && !this.mIsFixedTitleBar) {
            int visibleTitleHeight = getVisibleTitleHeight() - getEmbeddedHeight();
            setTranslationY(visibleTitleHeight);
            if (visibleTitleHeight > (-this.mSlop)) {
                show();
                this.mBaseUi.showBottomBarForDuration(2000L);
            } else if (visibleTitleHeight < (-this.mSlop)) {
                this.mBaseUi.hideBottomBar();
            }
        }
    }

    public void onResume() throws Resources.NotFoundException {
        setFixedTitleBar();
    }
}
