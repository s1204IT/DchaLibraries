package com.android.browser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class PhoneUi extends BaseUi {
    private static final boolean DEBUG = Browser.DEBUG;
    private int mActionBarHeight;
    private AnimScreen mAnimScreen;
    private int mLatestOrientation;
    private NavScreen mNavScreen;
    private NavigationBarPhone mNavigationBar;
    boolean mShowNav;

    static class AnimScreen {
        private ImageView mContent;
        private Bitmap mContentBitmap;
        private Context mContext;
        private View mMain;
        private float mScale;
        private ImageView mTitle;
        private Bitmap mTitleBarBitmap;

        public AnimScreen(Context context) {
            this.mMain = LayoutInflater.from(context).inflate(2130968578, (ViewGroup) null);
            this.mTitle = (ImageView) this.mMain.findViewById(2131558407);
            this.mContent = (ImageView) this.mMain.findViewById(2131558410);
            this.mContent.setScaleType(ImageView.ScaleType.MATRIX);
            this.mContent.setImageMatrix(new Matrix());
            this.mScale = 1.0f;
            setScaleFactor(getScaleFactor());
            this.mContext = context;
        }

        private float getScaleFactor() {
            return this.mScale;
        }

        private Bitmap safeCreateBitmap(int i, int i2) {
            Log.w("PhoneUi", "safeCreateBitmap() width: " + i + ", height: " + i2);
            if (i <= 0 || i2 <= 0) {
                return null;
            }
            return Bitmap.createBitmap(i, i2, Bitmap.Config.RGB_565);
        }

        public void setScaleFactor(float f) {
            this.mScale = f;
            Matrix matrix = new Matrix();
            matrix.postScale(f, f);
            this.mContent.setImageMatrix(matrix);
        }

        public void set(Bitmap bitmap) {
            this.mTitle.setVisibility(8);
            this.mContent.setImageBitmap(bitmap);
        }

        public void set(TitleBar titleBar, WebView webView, View view) {
            if (titleBar == null || webView == null) {
                return;
            }
            if (titleBar.getWidth() <= 0 || titleBar.getEmbeddedHeight() <= 0) {
                this.mTitleBarBitmap = null;
            } else {
                if (this.mTitleBarBitmap == null || this.mTitleBarBitmap.getWidth() != titleBar.getWidth() || this.mTitleBarBitmap.getHeight() != titleBar.getEmbeddedHeight()) {
                    this.mTitleBarBitmap = safeCreateBitmap(titleBar.getWidth(), titleBar.getEmbeddedHeight());
                }
                if (this.mTitleBarBitmap != null) {
                    Canvas canvas = new Canvas(this.mTitleBarBitmap);
                    titleBar.draw(canvas);
                    canvas.setBitmap(null);
                }
            }
            this.mTitle.setImageBitmap(this.mTitleBarBitmap);
            this.mTitle.setVisibility(0);
            int height = webView.getHeight();
            int embeddedHeight = titleBar.getEmbeddedHeight();
            float dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(2131427378) / webView.getWidth();
            float f = dimensionPixelSize <= 1.0f ? dimensionPixelSize : 1.0f;
            int iFloor = (int) Math.floor(webView.getWidth() * f);
            Math.floor((height - embeddedHeight) * f);
            if (view.getHeight() < view.getWidth()) {
                int iFloor2 = (int) Math.floor(view.getWidth() * f);
                int iFloor3 = (int) Math.floor(view.getHeight() * f);
                if (this.mContentBitmap == null || this.mContentBitmap.getWidth() != iFloor2 || this.mContentBitmap.getHeight() != iFloor3) {
                    this.mContentBitmap = safeCreateBitmap(iFloor2, iFloor3);
                }
            } else if (this.mContentBitmap == null || this.mContentBitmap.getWidth() != iFloor || this.mContentBitmap.getHeight() != iFloor) {
                this.mContentBitmap = safeCreateBitmap(iFloor, iFloor);
            }
            if (this.mContentBitmap != null) {
                Canvas canvas2 = new Canvas(this.mContentBitmap);
                int scrollX = webView.getScrollX();
                int scrollY = webView.getScrollY();
                canvas2.translate(-scrollX, (-scrollY) - titleBar.getEmbeddedHeight());
                canvas2.scale(f, f, scrollX, scrollY + titleBar.getEmbeddedHeight());
                webView.draw(canvas2);
                canvas2.setBitmap(null);
            }
            this.mContent.setScaleType(ImageView.ScaleType.FIT_XY);
            this.mContent.setImageBitmap(this.mContentBitmap);
        }
    }

    public PhoneUi(Activity activity, UiController uiController) {
        super(activity, uiController);
        this.mActionBarHeight = 0;
        this.mLatestOrientation = 0;
        this.mShowNav = false;
        setUseQuickControls(BrowserSettings.getInstance().useQuickControls());
        this.mNavigationBar = (NavigationBarPhone) this.mTitleBar.getNavigationBar();
        if (Build.VERSION.SDK_INT < 23) {
            TypedValue typedValue = new TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true);
            this.mActionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, activity.getResources().getDisplayMetrics());
        }
    }

    public void finishAnimateOut() {
        this.mTabControl.setOnThumbnailUpdatedListener(null);
        this.mNavScreen.setVisibility(8);
        this.mCustomViewContainer.setAlpha(1.0f);
        this.mCustomViewContainer.setVisibility(8);
    }

    public void finishAnimationIn() {
        if (showingNavScreen()) {
            this.mNavScreen.sendAccessibilityEvent(32);
            this.mTabControl.setOnThumbnailUpdatedListener(this.mNavScreen);
        }
    }

    private boolean showingNavScreen() {
        return this.mNavScreen != null && this.mNavScreen.getVisibility() == 0;
    }

    @Override
    public boolean dispatchKey(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void editUrl(boolean z, boolean z2) {
        if (this.mShowNav) {
            return;
        }
        super.editUrl(z, z2);
    }

    @Override
    protected void handleMessage(Message message) {
        super.handleMessage(message);
        if (message.what == 100) {
            if (this.mNavScreen == null) {
                this.mNavScreen = new NavScreen(this.mActivity, this.mUiController, this);
                this.mCustomViewContainer.addView(this.mNavScreen, COVER_SCREEN_PARAMS);
                this.mNavScreen.setVisibility(8);
            }
            if (this.mAnimScreen == null) {
                this.mAnimScreen = new AnimScreen(this.mActivity);
                this.mAnimScreen.set(getTitleBar(), getWebView(), this.mContentView);
            }
        }
    }

    @Override
    public void hideIME() {
        ((InputMethodManager) this.mActivity.getSystemService("input_method")).hideSoftInputFromWindow(this.mNavigationBar.getWindowToken(), 0);
    }

    void hideNavScreen(int i, boolean z) {
        if (DEBUG) {
            Log.d("browser", "PhoneUi.hideNavScreen()--->position = " + i + ", animate = " + z);
        }
        this.mShowNav = false;
        if (showingNavScreen()) {
            this.mFixedTitlebarContainer.setVisibility(0);
            this.mTitleBar.getNavigationBar().getUrlInputView().setVisibility(0);
            Tab tab = this.mUiController.getTabControl().getTab(i);
            if (tab == null || !z) {
                if (tab != null) {
                    setActiveTab(tab);
                } else if (this.mTabControl.getTabCount() > 0) {
                    setActiveTab(this.mTabControl.getCurrentTab());
                }
                this.mContentView.setVisibility(0);
                finishAnimateOut();
                return;
            }
            NavTabView tabView = this.mNavScreen.getTabView(i);
            if (tabView == null) {
                if (this.mTabControl.getTabCount() > 0) {
                    setActiveTab(this.mTabControl.getCurrentTab());
                }
                this.mContentView.setVisibility(0);
                finishAnimateOut();
                return;
            }
            this.mUiController.setBlockEvents(true);
            this.mUiController.setActiveTab(tab);
            this.mContentView.setVisibility(0);
            if (this.mAnimScreen == null) {
                this.mAnimScreen = new AnimScreen(this.mActivity);
            }
            this.mAnimScreen.set(tab.getScreenshot());
            if (this.mAnimScreen.mMain.getParent() == null) {
                this.mCustomViewContainer.addView(this.mAnimScreen.mMain, COVER_SCREEN_PARAMS);
            }
            this.mAnimScreen.mMain.layout(0, 0, this.mContentView.getWidth(), this.mContentView.getHeight());
            this.mNavScreen.mScroller.finishScroller();
            ImageView imageView = tabView.mImage;
            int height = this.mTitleBar.getHeight();
            int width = this.mContentView.getWidth();
            int intrinsicWidth = imageView.getDrawable().getIntrinsicWidth();
            int intrinsicHeight = imageView.getDrawable().getIntrinsicHeight();
            int left = (tabView.getLeft() + imageView.getLeft()) - this.mNavScreen.mScroller.getScrollX();
            int top = (imageView.getTop() + tabView.getTop()) - this.mNavScreen.mScroller.getScrollY();
            int i2 = left + intrinsicWidth;
            int i3 = top + intrinsicHeight;
            float width2 = this.mContentView.getWidth() / intrinsicWidth;
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
            layoutParams.setMargins(left, top, i2, i3);
            this.mAnimScreen.mContent.setLayoutParams(layoutParams);
            this.mAnimScreen.setScaleFactor(1.0f);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(ObjectAnimator.ofFloat(this.mNavScreen, "alpha", 1.0f, 0.0f), ObjectAnimator.ofFloat(this.mAnimScreen.mMain, "alpha", 0.0f, 1.0f));
            animatorSet.setDuration(100L);
            AnimatorSet animatorSet2 = new AnimatorSet();
            ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "left", left, 0);
            ObjectAnimator objectAnimatorOfInt2 = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "top", top, height);
            ObjectAnimator objectAnimatorOfInt3 = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "right", i2, width);
            ObjectAnimator objectAnimatorOfInt4 = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "bottom", i3, height + ((int) (intrinsicHeight * width2)));
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mAnimScreen, "scaleFactor", 1.0f, width2);
            ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mCustomViewContainer, "alpha", 1.0f, 0.0f);
            objectAnimatorOfFloat2.setDuration(100L);
            animatorSet2.playTogether(objectAnimatorOfInt, objectAnimatorOfInt2, objectAnimatorOfInt3, objectAnimatorOfInt4, objectAnimatorOfFloat);
            animatorSet2.setDuration(200L);
            AnimatorSet animatorSet3 = new AnimatorSet();
            animatorSet3.playSequentially(animatorSet, animatorSet2, objectAnimatorOfFloat2);
            animatorSet3.addListener(new AnimatorListenerAdapter(this) {
                final PhoneUi this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    this.this$0.mCustomViewContainer.removeView(this.this$0.mAnimScreen.mMain);
                    this.this$0.finishAnimateOut();
                    this.this$0.mUiController.setBlockEvents(false);
                }
            });
            animatorSet3.start();
        }
    }

    public boolean inLockScreenMode() {
        return ((ActivityManager) this.mActivity.getSystemService("activity")).isInLockTaskMode();
    }

    @Override
    public boolean isWebShowing() {
        return super.isWebShowing() && !showingNavScreen();
    }

    @Override
    public boolean needsRestoreAllTabs() {
        return false;
    }

    public boolean noTabInNavScreen() {
        return showingNavScreen() && this.mUiController.getTabControl().getTabCount() == 0;
    }

    @Override
    public void onActionModeFinished(boolean z) {
        if (isEditingUrl()) {
            ObjectAnimator duration = ObjectAnimator.ofFloat((View) this.mTitleBar.getParent(), "y", this.mActionBarHeight, 0.0f).setDuration(100L);
            duration.addListener(new Animator.AnimatorListener(this) {
                final PhoneUi this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    this.this$0.mTitleBar.getNavigationBar().getUrlInputView().showDropDown();
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }

                @Override
                public void onAnimationStart(Animator animator) {
                }
            });
            duration.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(this) {
                final PhoneUi this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    this.this$0.mTitleBar.getNavigationBar().getUrlInputView().showDropDown();
                }
            });
            this.mHandler.postDelayed(new Runnable(this, duration) {
                final PhoneUi this$0;
                final ObjectAnimator val$anim1;

                {
                    this.this$0 = this;
                    this.val$anim1 = duration;
                }

                @Override
                public void run() {
                    if (((View) this.this$0.mTitleBar.getParent()).getY() != 0.0f) {
                        this.val$anim1.start();
                    }
                }
            }, 300L);
        } else {
            ((View) this.mTitleBar.getParent()).animate().translationY(0.0f);
        }
        if (z) {
            showTitleBar();
        }
    }

    @Override
    public void onActionModeStarted(ActionMode actionMode) {
        if (isEditingUrl()) {
            ((View) this.mTitleBar.getParent()).animate().translationY(this.mActionBarHeight);
        } else {
            hideTitleBar();
        }
    }

    @Override
    public boolean onBackKey() {
        if (showingNavScreen() && this.mUiController.getTabControl().getTabCount() == 0) {
            return false;
        }
        if (!showingNavScreen()) {
            return super.onBackKey();
        }
        this.mNavScreen.close(this.mUiController.getTabControl().getCurrentPosition());
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        Log.d("PhoneUi", "PhoneUi.onConfigurationChanged(), new orientation = " + configuration.orientation);
        if (Build.VERSION.SDK_INT < 23) {
            TypedValue typedValue = new TypedValue();
            this.mActivity.getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true);
            this.mActionBarHeight = TypedValue.complexToDimensionPixelSize(typedValue.data, this.mActivity.getResources().getDisplayMetrics());
        }
        if (isEditingUrl() && this.mUiController != null && this.mUiController.isInCustomActionMode()) {
            ((View) this.mTitleBar.getParent()).animate().translationY(this.mActionBarHeight);
        }
        if (this.mNavigationBar.isMenuShowing() && configuration.orientation != this.mLatestOrientation) {
            this.mNavigationBar.dismissMenuOnly();
        }
        this.mLatestOrientation = configuration.orientation;
    }

    @Override
    public void onContextMenuClosed(Menu menu, boolean z) {
        if (z) {
            showTitleBar();
        }
    }

    @Override
    public void onContextMenuCreated(Menu menu) {
        hideTitleBar();
    }

    @Override
    public void onDestroy() {
        hideTitleBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (showingNavScreen() && menuItem.getItemId() != 2131558590 && menuItem.getItemId() != 2131558591) {
            hideNavScreen(this.mUiController.getTabControl().getCurrentPosition(), false);
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenuState(this.mActiveTab, menu);
        return true;
    }

    @Override
    public void onProgressChanged(Tab tab) {
        super.onProgressChanged(tab);
        if (this.mNavScreen != null || getTitleBar().getHeight() <= 0) {
            return;
        }
        this.mHandler.sendEmptyMessage(100);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mNavScreen != null) {
            this.mNavScreen.reload();
        }
    }

    @Override
    public void setActiveTab(Tab tab) {
        this.mTitleBar.cancelTitleBarAnimation(true);
        this.mTitleBar.setSkipTitleBarAnimations(true);
        super.setActiveTab(tab);
        if (this.mShowNav) {
            detachTab(this.mActiveTab);
        }
        BrowserWebView browserWebView = (BrowserWebView) tab.getWebView();
        if (browserWebView == null) {
            Log.e("PhoneUi", "active tab with no webview detected");
            return;
        }
        if (this.mUseQuickControls) {
            this.mPieControl.forceToTop(this.mContentView);
        }
        browserWebView.setTitleBar(this.mTitleBar);
        this.mNavigationBar.onStateChanged(0);
        updateLockIconToLatest(tab);
        this.mTitleBar.setSkipTitleBarAnimations(false);
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return true;
    }

    void showNavScreen() {
        if (DEBUG) {
            Log.d("browser", "PhoneUi.showNavScreen()--->");
        }
        if (this.mActiveTab == null) {
            return;
        }
        this.mShowNav = true;
        this.mUiController.setBlockEvents(true);
        if (this.mNavScreen == null) {
            this.mNavScreen = new NavScreen(this.mActivity, this.mUiController, this);
            this.mCustomViewContainer.addView(this.mNavScreen, COVER_SCREEN_PARAMS);
        } else {
            this.mNavScreen.setVisibility(0);
            this.mNavScreen.setAlpha(1.0f);
            this.mNavScreen.refreshAdapter();
        }
        this.mActiveTab.capture();
        if (this.mAnimScreen == null) {
            this.mAnimScreen = new AnimScreen(this.mActivity);
        } else {
            this.mAnimScreen.mMain.setAlpha(1.0f);
            this.mAnimScreen.mTitle.setAlpha(1.0f);
            this.mAnimScreen.setScaleFactor(1.0f);
        }
        this.mAnimScreen.set(getTitleBar(), getWebView(), this.mContentView);
        if (this.mAnimScreen.mMain.getParent() == null) {
            this.mCustomViewContainer.addView(this.mAnimScreen.mMain, COVER_SCREEN_PARAMS);
        }
        this.mCustomViewContainer.setVisibility(0);
        this.mCustomViewContainer.bringToFront();
        this.mAnimScreen.mMain.layout(0, 0, this.mContentView.getWidth(), this.mContentView.getHeight() + this.mTitleBar.getHeight());
        int height = getTitleBar().getHeight();
        int width = this.mContentView.getWidth();
        int height2 = this.mContentView.getHeight() + this.mTitleBar.getHeight();
        int dimensionPixelSize = this.mActivity.getResources().getDimensionPixelSize(2131427378);
        int dimensionPixelSize2 = this.mActivity.getResources().getDimensionPixelSize(2131427379);
        int dimensionPixelSize3 = this.mActivity.getResources().getDimensionPixelSize(2131427380);
        int width2 = (this.mContentView.getWidth() - dimensionPixelSize) / 2;
        int i = dimensionPixelSize3 + ((height2 - (dimensionPixelSize3 + dimensionPixelSize2)) / 2);
        float width3 = dimensionPixelSize / this.mContentView.getWidth();
        this.mContentView.setVisibility(8);
        this.mFixedTitlebarContainer.setVisibility(8);
        this.mTitleBar.getNavigationBar().getUrlInputView().setVisibility(8);
        AnimatorSet animatorSet = new AnimatorSet();
        AnimatorSet animatorSet2 = new AnimatorSet();
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "left", 0, width2);
        ObjectAnimator objectAnimatorOfInt2 = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "top", height, i);
        ObjectAnimator objectAnimatorOfInt3 = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "right", width, dimensionPixelSize + width2);
        ObjectAnimator objectAnimatorOfInt4 = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "bottom", height2, dimensionPixelSize2 + i);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mAnimScreen.mTitle, "alpha", 1.0f, 0.0f);
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mAnimScreen, "scaleFactor", 1.0f, width3);
        ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(this.mAnimScreen.mMain, "alpha", 1.0f, 0.0f);
        objectAnimatorOfFloat3.setDuration(100L);
        animatorSet2.playTogether(objectAnimatorOfInt, objectAnimatorOfInt2, objectAnimatorOfInt3, objectAnimatorOfInt4, objectAnimatorOfFloat2, objectAnimatorOfFloat);
        animatorSet2.setDuration(200L);
        animatorSet.addListener(new AnimatorListenerAdapter(this) {
            final PhoneUi this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                this.this$0.mCustomViewContainer.removeView(this.this$0.mAnimScreen.mMain);
                this.this$0.finishAnimationIn();
                this.this$0.detachTab(this.this$0.mActiveTab);
                this.this$0.mUiController.setBlockEvents(false);
            }
        });
        animatorSet.playSequentially(animatorSet2, objectAnimatorOfFloat3);
        animatorSet.start();
    }

    @Override
    public void showWeb(boolean z) {
        super.showWeb(z);
        hideNavScreen(this.mUiController.getTabControl().getCurrentPosition(), z);
    }

    public void toggleNavScreen() {
        if (showingNavScreen()) {
            hideNavScreen(this.mUiController.getTabControl().getCurrentPosition(), false);
        } else {
            showNavScreen();
        }
    }

    @Override
    public void updateBottomBarState(boolean z, boolean z2, boolean z3) {
        if (this.mNeedBottomBar) {
            this.mBottomBar.changeBottomBarState(z2, z3);
            showBottomBarForDuration(2000L);
        }
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
        MenuItem menuItemFindItem = menu.findItem(2131558582);
        if (menuItemFindItem != null) {
            menuItemFindItem.setVisible(!showingNavScreen());
        }
        MenuItem menuItemFindItem2 = menu.findItem(2131558576);
        if (menuItemFindItem2 != null) {
            menuItemFindItem2.setVisible((tab == null || tab.isSnapshot() || showingNavScreen()) ? false : true);
        }
        MenuItem menuItemFindItem3 = menu.findItem(2131558584);
        if (menuItemFindItem3 != null) {
            menuItemFindItem3.setVisible(false);
        }
        MenuItem menuItemFindItem4 = menu.findItem(2131558583);
        if (menuItemFindItem4 != null && !this.mUseQuickControls) {
            menuItemFindItem4.setVisible(false);
        }
        MenuItem menuItemFindItem5 = menu.findItem(2131558575);
        if (menuItemFindItem5 != null) {
            menuItemFindItem5.setVisible((tab == null || tab.isSnapshot() || showingNavScreen()) ? false : true);
        }
        MenuItem menuItemFindItem6 = menu.findItem(2131558577);
        if (menuItemFindItem6 != null) {
            menuItemFindItem6.setVisible(!showingNavScreen());
            menuItemFindItem6.setEnabled(!inLockScreenMode());
        }
        MenuItem menuItemFindItem7 = menu.findItem(2131558585);
        if (menuItemFindItem7 != null) {
            menuItemFindItem7.setEnabled(!noTabInNavScreen());
        }
        MenuItem menuItemFindItem8 = menu.findItem(2131558590);
        if (menuItemFindItem8 != null) {
            menuItemFindItem8.setEnabled(!noTabInNavScreen());
        }
        MenuItem menuItemFindItem9 = menu.findItem(2131558591);
        if (menuItemFindItem9 != null) {
            menuItemFindItem9.setEnabled(!noTabInNavScreen());
        }
        MenuItem menuItemFindItem10 = menu.findItem(2131558589);
        if (menuItemFindItem10 != null) {
            menuItemFindItem10.setEnabled(!(tab == null || this.mTabControl.getTabCount() <= 1));
        }
        if (showingNavScreen()) {
            menu.setGroupVisible(2131558578, false);
            menu.setGroupVisible(2131558586, false);
            menu.setGroupVisible(2131558571, false);
            menu.setGroupVisible(2131558588, true);
        }
    }
}
