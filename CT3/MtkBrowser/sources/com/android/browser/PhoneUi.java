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

    public PhoneUi(Activity browser, UiController controller) {
        super(browser, controller);
        this.mActionBarHeight = 0;
        this.mLatestOrientation = 0;
        this.mShowNav = false;
        setUseQuickControls(BrowserSettings.getInstance().useQuickControls());
        this.mNavigationBar = (NavigationBarPhone) this.mTitleBar.getNavigationBar();
        if (Build.VERSION.SDK_INT >= 23) {
            return;
        }
        TypedValue heightValue = new TypedValue();
        browser.getTheme().resolveAttribute(android.R.attr.actionBarSize, heightValue, true);
        this.mActionBarHeight = TypedValue.complexToDimensionPixelSize(heightValue.data, browser.getResources().getDisplayMetrics());
    }

    @Override
    public void onDestroy() {
        hideTitleBar();
    }

    @Override
    public void editUrl(boolean clearInput, boolean forceIME) {
        if (this.mShowNav) {
            return;
        }
        super.editUrl(clearInput, forceIME);
    }

    @Override
    public boolean onBackKey() {
        if (showingNavScreen() && this.mUiController.getTabControl().getTabCount() == 0) {
            return false;
        }
        if (showingNavScreen()) {
            this.mNavScreen.close(this.mUiController.getTabControl().getCurrentPosition());
            return true;
        }
        return super.onBackKey();
    }

    private boolean showingNavScreen() {
        return this.mNavScreen != null && this.mNavScreen.getVisibility() == 0;
    }

    @Override
    public boolean dispatchKey(int code, KeyEvent event) {
        return false;
    }

    @Override
    public void onProgressChanged(Tab tab) {
        if (DEBUG && tab != null && tab.getWebView() != null) {
            Log.d("browser", "PhoneUi.onProgressChanged()-->process = " + tab.getWebView().getProgress());
        }
        super.onProgressChanged(tab);
        if (this.mNavScreen != null || getTitleBar().getHeight() <= 0) {
            return;
        }
        this.mHandler.sendEmptyMessage(100);
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        Log.d("PhoneUi", "PhoneUi.onConfigurationChanged(), new orientation = " + config.orientation);
        if (Build.VERSION.SDK_INT < 23) {
            TypedValue heightValue = new TypedValue();
            this.mActivity.getTheme().resolveAttribute(android.R.attr.actionBarSize, heightValue, true);
            this.mActionBarHeight = TypedValue.complexToDimensionPixelSize(heightValue.data, this.mActivity.getResources().getDisplayMetrics());
        }
        if (isEditingUrl() && this.mUiController != null && this.mUiController.isInCustomActionMode()) {
            ((View) this.mTitleBar.getParent()).animate().translationY(this.mActionBarHeight);
        }
        if (this.mNavigationBar.isMenuShowing() && config.orientation != this.mLatestOrientation) {
            this.mNavigationBar.dismissMenuOnly();
        }
        this.mLatestOrientation = config.orientation;
    }

    @Override
    protected void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (msg.what != 100) {
            return;
        }
        if (this.mNavScreen == null) {
            this.mNavScreen = new NavScreen(this.mActivity, this.mUiController, this);
            this.mCustomViewContainer.addView(this.mNavScreen, COVER_SCREEN_PARAMS);
            this.mNavScreen.setVisibility(8);
        }
        if (this.mAnimScreen != null) {
            return;
        }
        this.mAnimScreen = new AnimScreen(this.mActivity);
        this.mAnimScreen.set(getTitleBar(), getWebView());
    }

    @Override
    public void updateBottomBarState(boolean pageCanScroll, boolean back, boolean forward) {
        if (!this.mNeedBottomBar) {
            return;
        }
        this.mBottomBar.changeBottomBarState(back, forward);
        showBottomBarForDuration(2000L);
    }

    @Override
    public void setActiveTab(Tab tab) {
        this.mTitleBar.cancelTitleBarAnimation(true);
        this.mTitleBar.setSkipTitleBarAnimations(true);
        super.setActiveTab(tab);
        if (this.mShowNav) {
            detachTab(this.mActiveTab);
        }
        BrowserWebView view = (BrowserWebView) tab.getWebView();
        if (view == null) {
            Log.e("PhoneUi", "active tab with no webview detected");
            return;
        }
        if (this.mUseQuickControls) {
            this.mPieControl.forceToTop(this.mContentView);
        }
        view.setTitleBar(this.mTitleBar);
        this.mNavigationBar.onStateChanged(0);
        updateLockIconToLatest(tab);
        this.mTitleBar.setSkipTitleBarAnimations(false);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenuState(this.mActiveTab, menu);
        return true;
    }

    public boolean inLockScreenMode() {
        ActivityManager am = (ActivityManager) this.mActivity.getSystemService("activity");
        return am.isInLockTaskMode();
    }

    public boolean noTabInNavScreen() {
        return showingNavScreen() && this.mUiController.getTabControl().getTabCount() == 0;
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
        MenuItem bm = menu.findItem(R.id.bookmarks_menu_id);
        if (bm != null) {
            bm.setVisible(!showingNavScreen());
        }
        MenuItem abm = menu.findItem(R.id.add_bookmark_menu_id);
        if (abm != null) {
            abm.setVisible((tab == null || tab.isSnapshot() || showingNavScreen()) ? false : true);
        }
        MenuItem info = menu.findItem(R.id.page_info_menu_id);
        if (info != null) {
            info.setVisible(false);
        }
        MenuItem newtab = menu.findItem(R.id.new_tab_menu_id);
        if (newtab != null && !this.mUseQuickControls) {
            newtab.setVisible(false);
        }
        MenuItem home = menu.findItem(R.id.home_menu_id);
        if (home != null) {
            home.setVisible((tab == null || tab.isSnapshot() || showingNavScreen()) ? false : true);
        }
        MenuItem close = menu.findItem(R.id.close_browser_menu_id);
        if (close != null) {
            close.setVisible(!showingNavScreen());
            close.setEnabled(!inLockScreenMode());
        }
        MenuItem settings = menu.findItem(R.id.preferences_menu_id);
        if (settings != null) {
            settings.setEnabled(!noTabInNavScreen());
        }
        MenuItem history = menu.findItem(R.id.history_menu_id);
        if (history != null) {
            history.setEnabled(!noTabInNavScreen());
        }
        MenuItem savedPage = menu.findItem(R.id.snapshots_menu_id);
        if (savedPage != null) {
            savedPage.setEnabled(!noTabInNavScreen());
        }
        MenuItem closeOthers = menu.findItem(R.id.close_other_tabs_id);
        if (closeOthers != null) {
            boolean isLastTab = true;
            if (tab != null) {
                isLastTab = this.mTabControl.getTabCount() <= 1;
            }
            closeOthers.setEnabled(!isLastTab);
        }
        if (!showingNavScreen()) {
            return;
        }
        menu.setGroupVisible(R.id.LIVE_MENU, false);
        menu.setGroupVisible(R.id.SNAPSHOT_MENU, false);
        menu.setGroupVisible(R.id.NAV_MENU, false);
        menu.setGroupVisible(R.id.COMBO_MENU, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (showingNavScreen() && item.getItemId() != R.id.history_menu_id && item.getItemId() != R.id.snapshots_menu_id) {
            hideNavScreen(this.mUiController.getTabControl().getCurrentPosition(), false);
        }
        return false;
    }

    @Override
    public void onContextMenuCreated(Menu menu) {
        hideTitleBar();
    }

    @Override
    public void onContextMenuClosed(Menu menu, boolean inLoad) {
        if (!inLoad) {
            return;
        }
        showTitleBar();
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        if (!isEditingUrl()) {
            hideTitleBar();
        } else {
            ((View) this.mTitleBar.getParent()).animate().translationY(this.mActionBarHeight);
        }
    }

    @Override
    public void onActionModeFinished(boolean inLoad) {
        if (isEditingUrl()) {
            final ObjectAnimator anim1 = ObjectAnimator.ofFloat((View) this.mTitleBar.getParent(), "y", this.mActionBarHeight, 0.0f).setDuration(100L);
            anim1.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    PhoneUi.this.mTitleBar.getNavigationBar().getUrlInputView().showDropDown();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationStart(Animator animation) {
                }
            });
            anim1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    PhoneUi.this.mTitleBar.getNavigationBar().getUrlInputView().showDropDown();
                }
            });
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (((View) PhoneUi.this.mTitleBar.getParent()).getY() == 0.0f) {
                        return;
                    }
                    anim1.start();
                }
            }, 300L);
        } else {
            ((View) this.mTitleBar.getParent()).animate().translationY(0.0f);
        }
        if (!inLoad) {
            return;
        }
        showTitleBar();
    }

    @Override
    public boolean isWebShowing() {
        return super.isWebShowing() && !showingNavScreen();
    }

    @Override
    public void showWeb(boolean animate) {
        super.showWeb(animate);
        hideNavScreen(this.mUiController.getTabControl().getCurrentPosition(), animate);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mNavScreen == null) {
            return;
        }
        this.mNavScreen.reload();
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
        this.mAnimScreen.set(getTitleBar(), getWebView());
        if (this.mAnimScreen.mMain.getParent() == null) {
            this.mCustomViewContainer.addView(this.mAnimScreen.mMain, COVER_SCREEN_PARAMS);
        }
        this.mCustomViewContainer.setVisibility(0);
        this.mCustomViewContainer.bringToFront();
        this.mAnimScreen.mMain.layout(0, 0, this.mContentView.getWidth(), this.mContentView.getHeight() + this.mTitleBar.getHeight());
        int fromTop = getTitleBar().getHeight();
        int fromRight = this.mContentView.getWidth();
        int fromBottom = this.mContentView.getHeight() + this.mTitleBar.getHeight();
        int width = this.mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_width);
        int height = this.mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_height);
        int ntth = this.mActivity.getResources().getDimensionPixelSize(R.dimen.nav_tab_titleheight);
        int toLeft = (this.mContentView.getWidth() - width) / 2;
        int toTop = ((fromBottom - (ntth + height)) / 2) + ntth;
        int toRight = toLeft + width;
        int toBottom = toTop + height;
        float scaleFactor = width / this.mContentView.getWidth();
        this.mContentView.setVisibility(8);
        this.mFixedTitlebarContainer.setVisibility(8);
        this.mTitleBar.getNavigationBar().getUrlInputView().setVisibility(8);
        AnimatorSet set1 = new AnimatorSet();
        AnimatorSet inanim = new AnimatorSet();
        ObjectAnimator tx = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "left", 0, toLeft);
        ObjectAnimator ty = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "top", fromTop, toTop);
        ObjectAnimator tr = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "right", fromRight, toRight);
        ObjectAnimator tb = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "bottom", fromBottom, toBottom);
        ObjectAnimator title = ObjectAnimator.ofFloat(this.mAnimScreen.mTitle, "alpha", 1.0f, 0.0f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(this.mAnimScreen, "scaleFactor", 1.0f, scaleFactor);
        ObjectAnimator blend1 = ObjectAnimator.ofFloat(this.mAnimScreen.mMain, "alpha", 1.0f, 0.0f);
        blend1.setDuration(100L);
        inanim.playTogether(tx, ty, tr, tb, sx, title);
        inanim.setDuration(200L);
        set1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator anim) {
                PhoneUi.this.mCustomViewContainer.removeView(PhoneUi.this.mAnimScreen.mMain);
                PhoneUi.this.finishAnimationIn();
                PhoneUi.this.detachTab(PhoneUi.this.mActiveTab);
                PhoneUi.this.mUiController.setBlockEvents(false);
            }
        });
        set1.playSequentially(inanim, blend1);
        set1.start();
    }

    public void finishAnimationIn() {
        if (!showingNavScreen()) {
            return;
        }
        this.mNavScreen.sendAccessibilityEvent(32);
        this.mTabControl.setOnThumbnailUpdatedListener(this.mNavScreen);
    }

    void hideNavScreen(int position, boolean animate) {
        if (DEBUG) {
            Log.d("browser", "PhoneUi.hideNavScreen()--->position = " + position + ", animate = " + animate);
        }
        this.mShowNav = false;
        if (showingNavScreen()) {
            this.mFixedTitlebarContainer.setVisibility(0);
            this.mTitleBar.getNavigationBar().getUrlInputView().setVisibility(0);
            Tab tab = this.mUiController.getTabControl().getTab(position);
            if (tab == null || !animate) {
                if (tab != null) {
                    setActiveTab(tab);
                } else if (this.mTabControl.getTabCount() > 0) {
                    setActiveTab(this.mTabControl.getCurrentTab());
                }
                this.mContentView.setVisibility(0);
                finishAnimateOut();
                return;
            }
            NavTabView tabview = this.mNavScreen.getTabView(position);
            if (tabview == null) {
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
            ImageView target = tabview.mImage;
            int toTop = this.mTitleBar.getHeight();
            int toRight = this.mContentView.getWidth();
            int width = target.getDrawable().getIntrinsicWidth();
            int height = target.getDrawable().getIntrinsicHeight();
            int fromLeft = (tabview.getLeft() + target.getLeft()) - this.mNavScreen.mScroller.getScrollX();
            int fromTop = (tabview.getTop() + target.getTop()) - this.mNavScreen.mScroller.getScrollY();
            int fromRight = fromLeft + width;
            int fromBottom = fromTop + height;
            float scaleFactor = this.mContentView.getWidth() / width;
            int toBottom = toTop + ((int) (height * scaleFactor));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(fromLeft, fromTop, fromRight, fromBottom);
            this.mAnimScreen.mContent.setLayoutParams(lp);
            this.mAnimScreen.setScaleFactor(1.0f);
            AnimatorSet set1 = new AnimatorSet();
            ObjectAnimator fade2 = ObjectAnimator.ofFloat(this.mAnimScreen.mMain, "alpha", 0.0f, 1.0f);
            ObjectAnimator fade1 = ObjectAnimator.ofFloat(this.mNavScreen, "alpha", 1.0f, 0.0f);
            set1.playTogether(fade1, fade2);
            set1.setDuration(100L);
            AnimatorSet set2 = new AnimatorSet();
            ObjectAnimator l = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "left", fromLeft, 0);
            ObjectAnimator t = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "top", fromTop, toTop);
            ObjectAnimator r = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "right", fromRight, toRight);
            ObjectAnimator b = ObjectAnimator.ofInt(this.mAnimScreen.mContent, "bottom", fromBottom, toBottom);
            ObjectAnimator scale = ObjectAnimator.ofFloat(this.mAnimScreen, "scaleFactor", 1.0f, scaleFactor);
            ObjectAnimator otheralpha = ObjectAnimator.ofFloat(this.mCustomViewContainer, "alpha", 1.0f, 0.0f);
            otheralpha.setDuration(100L);
            set2.playTogether(l, t, r, b, scale);
            set2.setDuration(200L);
            AnimatorSet combo = new AnimatorSet();
            combo.playSequentially(set1, set2, otheralpha);
            combo.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator anim) {
                    PhoneUi.this.mCustomViewContainer.removeView(PhoneUi.this.mAnimScreen.mMain);
                    PhoneUi.this.finishAnimateOut();
                    PhoneUi.this.mUiController.setBlockEvents(false);
                }
            });
            combo.start();
        }
    }

    public void finishAnimateOut() {
        this.mTabControl.setOnThumbnailUpdatedListener(null);
        this.mNavScreen.setVisibility(8);
        this.mCustomViewContainer.setAlpha(1.0f);
        this.mCustomViewContainer.setVisibility(8);
    }

    @Override
    public boolean needsRestoreAllTabs() {
        return false;
    }

    public void toggleNavScreen() {
        if (!showingNavScreen()) {
            showNavScreen();
        } else {
            hideNavScreen(this.mUiController.getTabControl().getCurrentPosition(), false);
        }
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return true;
    }

    static class AnimScreen {
        private ImageView mContent;
        private Bitmap mContentBitmap;
        private Context mContext;
        private View mMain;
        private float mScale;
        private ImageView mTitle;
        private Bitmap mTitleBarBitmap;

        public AnimScreen(Context ctx) {
            this.mMain = LayoutInflater.from(ctx).inflate(R.layout.anim_screen, (ViewGroup) null);
            this.mTitle = (ImageView) this.mMain.findViewById(R.id.title);
            this.mContent = (ImageView) this.mMain.findViewById(R.id.content);
            this.mContent.setScaleType(ImageView.ScaleType.MATRIX);
            this.mContent.setImageMatrix(new Matrix());
            this.mScale = 1.0f;
            setScaleFactor(getScaleFactor());
            this.mContext = ctx;
        }

        public void set(TitleBar tbar, WebView web) {
            if (tbar == null || web == null) {
                return;
            }
            if (tbar.getWidth() > 0 && tbar.getEmbeddedHeight() > 0) {
                if (this.mTitleBarBitmap == null || this.mTitleBarBitmap.getWidth() != tbar.getWidth() || this.mTitleBarBitmap.getHeight() != tbar.getEmbeddedHeight()) {
                    this.mTitleBarBitmap = safeCreateBitmap(tbar.getWidth(), tbar.getEmbeddedHeight());
                }
                if (this.mTitleBarBitmap != null) {
                    Canvas c = new Canvas(this.mTitleBarBitmap);
                    tbar.draw(c);
                    c.setBitmap(null);
                }
            } else {
                this.mTitleBarBitmap = null;
            }
            this.mTitle.setImageBitmap(this.mTitleBarBitmap);
            this.mTitle.setVisibility(0);
            int h = web.getHeight() - tbar.getEmbeddedHeight();
            float scale = this.mContext.getResources().getDimensionPixelSize(R.dimen.nav_tab_width) / web.getWidth();
            int imageWidth = (int) Math.floor(web.getWidth() * scale);
            if (this.mContentBitmap == null || this.mContentBitmap.getWidth() != imageWidth || this.mContentBitmap.getHeight() != imageWidth) {
                this.mContentBitmap = safeCreateBitmap(imageWidth, imageWidth);
            }
            if (this.mContentBitmap != null) {
                Canvas c2 = new Canvas(this.mContentBitmap);
                int tx = web.getScrollX();
                int ty = web.getScrollY();
                c2.translate(-tx, (-ty) - tbar.getEmbeddedHeight());
                c2.scale(scale, scale, tx, tbar.getEmbeddedHeight() + ty);
                web.draw(c2);
                c2.setBitmap(null);
            }
            this.mContent.setScaleType(ImageView.ScaleType.FIT_XY);
            this.mContent.setImageBitmap(this.mContentBitmap);
        }

        private Bitmap safeCreateBitmap(int width, int height) {
            if (width <= 0 || height <= 0) {
                Log.w("PhoneUi", "safeCreateBitmap failed! width: " + width + ", height: " + height);
                return null;
            }
            return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        }

        public void set(Bitmap image) {
            this.mTitle.setVisibility(8);
            this.mContent.setImageBitmap(image);
        }

        public void setScaleFactor(float sf) {
            this.mScale = sf;
            Matrix m = new Matrix();
            m.postScale(sf, sf);
            this.mContent.setImageMatrix(m);
        }

        private float getScaleFactor() {
            return this.mScale;
        }
    }

    @Override
    public void hideIME() {
        ((InputMethodManager) this.mActivity.getSystemService("input_method")).hideSoftInputFromWindow(this.mNavigationBar.getWindowToken(), 0);
    }
}
