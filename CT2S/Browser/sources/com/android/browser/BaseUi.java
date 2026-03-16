package com.android.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.android.browser.Tab;
import com.android.browser.UI;
import java.util.List;

public abstract class BaseUi implements UI {
    protected Tab mActiveTab;
    Activity mActivity;
    private boolean mActivityPaused;
    private boolean mBlockFocusAnimations;
    protected FrameLayout mContentView;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    protected FrameLayout mCustomViewContainer;
    private Bitmap mDefaultVideoPoster;
    private LinearLayout mErrorConsoleContainer;
    private FrameLayout mFixedTitlebarContainer;
    protected FrameLayout mFullscreenContainer;
    protected Drawable mGenericFavicon;
    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                BaseUi.this.suggestHideTitleBar();
            }
            BaseUi.this.handleMessage(msg);
        }
    };
    private InputMethodManager mInputManager;
    private Drawable mLockIconMixed;
    private Drawable mLockIconSecure;
    private NavigationBarBase mNavigationBar;
    private int mOriginalOrientation;
    protected PieControl mPieControl;
    private Toast mStopToast;
    TabControl mTabControl;
    protected TitleBar mTitleBar;
    UiController mUiController;
    private UrlBarAutoShowManager mUrlBarAutoShowManager;
    protected boolean mUseQuickControls;
    private View mVideoProgressView;
    protected static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(-1, -1);
    protected static final FrameLayout.LayoutParams COVER_SCREEN_GRAVITY_CENTER = new FrameLayout.LayoutParams(-1, -1, 17);

    public BaseUi(Activity browser, UiController controller) {
        this.mErrorConsoleContainer = null;
        this.mActivity = browser;
        this.mUiController = controller;
        this.mTabControl = controller.getTabControl();
        Resources res = this.mActivity.getResources();
        this.mInputManager = (InputMethodManager) browser.getSystemService("input_method");
        this.mLockIconSecure = res.getDrawable(R.drawable.ic_secure_holo_dark);
        this.mLockIconMixed = res.getDrawable(R.drawable.ic_secure_partial_holo_dark);
        FrameLayout frameLayout = (FrameLayout) this.mActivity.getWindow().getDecorView().findViewById(android.R.id.content);
        LayoutInflater.from(this.mActivity).inflate(R.layout.custom_screen, frameLayout);
        this.mFixedTitlebarContainer = (FrameLayout) frameLayout.findViewById(R.id.fixed_titlebar_container);
        this.mContentView = (FrameLayout) frameLayout.findViewById(R.id.main_content);
        this.mCustomViewContainer = (FrameLayout) frameLayout.findViewById(R.id.fullscreen_custom_content);
        this.mErrorConsoleContainer = (LinearLayout) frameLayout.findViewById(R.id.error_console);
        setFullscreen(BrowserSettings.getInstance().useFullscreen());
        this.mGenericFavicon = res.getDrawable(R.drawable.app_web_browser_sm);
        this.mTitleBar = new TitleBar(this.mActivity, this.mUiController, this, this.mContentView);
        this.mTitleBar.setProgress(100);
        this.mNavigationBar = this.mTitleBar.getNavigationBar();
        this.mUrlBarAutoShowManager = new UrlBarAutoShowManager(this);
    }

    private void cancelStopToast() {
        if (this.mStopToast != null) {
            this.mStopToast.cancel();
            this.mStopToast = null;
        }
    }

    @Override
    public void onPause() {
        if (isCustomViewShowing()) {
            onHideCustomView();
        }
        cancelStopToast();
        this.mActivityPaused = true;
    }

    @Override
    public void onResume() {
        this.mActivityPaused = false;
        Tab ct = this.mTabControl.getCurrentTab();
        if (ct != null) {
            setActiveTab(ct);
        }
        this.mTitleBar.onResume();
    }

    protected boolean isActivityPaused() {
        return this.mActivityPaused;
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
    }

    public Activity getActivity() {
        return this.mActivity;
    }

    @Override
    public boolean onBackKey() {
        if (this.mCustomView == null) {
            return false;
        }
        this.mUiController.hideCustomView();
        return true;
    }

    @Override
    public boolean onMenuKey() {
        return false;
    }

    @Override
    public void setUseQuickControls(boolean useQuickControls) {
        this.mUseQuickControls = useQuickControls;
        this.mTitleBar.setUseQuickControls(this.mUseQuickControls);
        if (useQuickControls) {
            this.mPieControl = new PieControl(this.mActivity, this.mUiController, this);
            this.mPieControl.attachToContainer(this.mContentView);
        } else if (this.mPieControl != null) {
            this.mPieControl.removeFromContainer(this.mContentView);
        }
        updateUrlBarAutoShowManagerTarget();
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        setUrlTitle(tab);
        setFavicon(tab);
        updateLockIconToLatest(tab);
        updateNavigationState(tab);
        this.mTitleBar.onTabDataChanged(tab);
        this.mNavigationBar.onTabDataChanged(tab);
        onProgressChanged(tab);
    }

    @Override
    public void onProgressChanged(Tab tab) {
        int progress = tab.getLoadProgress();
        if (tab.inForeground()) {
            this.mTitleBar.setProgress(progress);
        }
    }

    @Override
    public void bookmarkedStatusHasChanged(Tab tab) {
        if (tab.inForeground()) {
            boolean isBookmark = tab.isBookmarkedSite();
            this.mNavigationBar.setCurrentUrlIsBookmark(isBookmark);
        }
    }

    @Override
    public void onPageStopped(Tab tab) {
        cancelStopToast();
        if (tab.inForeground()) {
            this.mStopToast = Toast.makeText(this.mActivity, R.string.stopping, 0);
            this.mStopToast.show();
        }
    }

    @Override
    public boolean needsRestoreAllTabs() {
        return true;
    }

    @Override
    public void addTab(Tab tab) {
    }

    @Override
    public void setActiveTab(Tab tab) {
        if (tab != null) {
            this.mBlockFocusAnimations = true;
            this.mHandler.removeMessages(1);
            if (tab != this.mActiveTab && this.mActiveTab != null) {
                removeTabFromContentView(this.mActiveTab);
                WebView web = this.mActiveTab.getWebView();
                if (web != null) {
                    web.setOnTouchListener(null);
                }
            }
            this.mActiveTab = tab;
            BrowserWebView web2 = (BrowserWebView) this.mActiveTab.getWebView();
            updateUrlBarAutoShowManagerTarget();
            attachTabToContentView(tab);
            if (web2 != null) {
                if (this.mUseQuickControls) {
                    this.mPieControl.forceToTop(this.mContentView);
                    web2.setTitleBar(null);
                    this.mTitleBar.hide();
                } else {
                    web2.setTitleBar(this.mTitleBar);
                    this.mTitleBar.onScrollChanged();
                }
            }
            this.mTitleBar.bringToFront();
            tab.getTopWindow().requestFocus();
            setShouldShowErrorConsole(tab, this.mUiController.shouldShowErrorConsole());
            onTabDataChanged(tab);
            onProgressChanged(tab);
            this.mNavigationBar.setIncognitoMode(tab.isPrivateBrowsingEnabled());
            updateAutoLogin(tab, false);
            this.mBlockFocusAnimations = false;
        }
    }

    protected void updateUrlBarAutoShowManagerTarget() {
        WebView web = this.mActiveTab != null ? this.mActiveTab.getWebView() : null;
        if (!this.mUseQuickControls && (web instanceof BrowserWebView)) {
            this.mUrlBarAutoShowManager.setTarget((BrowserWebView) web);
        } else {
            this.mUrlBarAutoShowManager.setTarget(null);
        }
    }

    Tab getActiveTab() {
        return this.mActiveTab;
    }

    @Override
    public void updateTabs(List<Tab> tabs) {
    }

    @Override
    public void removeTab(Tab tab) {
        if (this.mActiveTab == tab) {
            removeTabFromContentView(tab);
            this.mActiveTab = null;
        }
    }

    @Override
    public void detachTab(Tab tab) {
        removeTabFromContentView(tab);
    }

    @Override
    public void attachTab(Tab tab) {
        attachTabToContentView(tab);
    }

    protected void attachTabToContentView(Tab tab) {
        if (tab != null && tab.getWebView() != null) {
            View container = tab.getViewContainer();
            WebView mainView = tab.getWebView();
            FrameLayout wrapper = (FrameLayout) container.findViewById(R.id.webview_wrapper);
            ViewGroup parent = (ViewGroup) mainView.getParent();
            if (parent != wrapper) {
                if (parent != null) {
                    parent.removeView(mainView);
                }
                wrapper.addView(mainView);
            }
            ViewGroup parent2 = (ViewGroup) container.getParent();
            if (parent2 != this.mContentView) {
                if (parent2 != null) {
                    parent2.removeView(container);
                }
                this.mContentView.addView(container, COVER_SCREEN_PARAMS);
            }
            this.mUiController.attachSubWindow(tab);
        }
    }

    private void removeTabFromContentView(Tab tab) {
        hideTitleBar();
        WebView mainView = tab.getWebView();
        View container = tab.getViewContainer();
        if (mainView != null) {
            FrameLayout wrapper = (FrameLayout) container.findViewById(R.id.webview_wrapper);
            wrapper.removeView(mainView);
            this.mContentView.removeView(container);
            this.mUiController.endActionMode();
            this.mUiController.removeSubWindow(tab);
            ErrorConsoleView errorConsole = tab.getErrorConsole(false);
            if (errorConsole != null) {
                this.mErrorConsoleContainer.removeView(errorConsole);
            }
        }
    }

    @Override
    public void onSetWebView(Tab tab, WebView webView) {
        View container = tab.getViewContainer();
        if (container == null) {
            container = this.mActivity.getLayoutInflater().inflate(R.layout.tab, (ViewGroup) this.mContentView, false);
            tab.setViewContainer(container);
        }
        if (tab.getWebView() != webView) {
            FrameLayout wrapper = (FrameLayout) container.findViewById(R.id.webview_wrapper);
            wrapper.removeView(tab.getWebView());
        }
    }

    @Override
    public void createSubWindow(Tab tab, final WebView subView) {
        View subViewContainer = this.mActivity.getLayoutInflater().inflate(R.layout.browser_subwindow, (ViewGroup) null);
        ViewGroup inner = (ViewGroup) subViewContainer.findViewById(R.id.inner_container);
        inner.addView(subView, new ViewGroup.LayoutParams(-1, -1));
        ImageButton cancel = (ImageButton) subViewContainer.findViewById(R.id.subwindow_close);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BrowserWebView) subView).getWebChromeClient().onCloseWindow(subView);
            }
        });
        tab.setSubWebView(subView);
        tab.setSubViewContainer(subViewContainer);
    }

    @Override
    public void removeSubWindow(View subviewContainer) {
        this.mContentView.removeView(subviewContainer);
        this.mUiController.endActionMode();
    }

    @Override
    public void attachSubWindow(View container) {
        if (container.getParent() != null) {
            ((ViewGroup) container.getParent()).removeView(container);
        }
        this.mContentView.addView(container, COVER_SCREEN_PARAMS);
    }

    protected void refreshWebView() {
        WebView web = getWebView();
        if (web != null) {
            web.invalidate();
        }
    }

    @Override
    public void editUrl(boolean clearInput, boolean forceIME) {
        if (this.mUiController.isInCustomActionMode()) {
            this.mUiController.endActionMode();
        }
        showTitleBar();
        if (getActiveTab() != null && !getActiveTab().isSnapshot()) {
            this.mNavigationBar.startEditingUrl(clearInput, forceIME);
        }
    }

    boolean canShowTitleBar() {
        return (isTitleBarShowing() || isActivityPaused() || getActiveTab() == null || getWebView() == null || this.mUiController.isInCustomActionMode()) ? false : true;
    }

    protected void showTitleBar() {
        this.mHandler.removeMessages(1);
        if (canShowTitleBar()) {
            this.mTitleBar.show();
        }
    }

    protected void hideTitleBar() {
        if (this.mTitleBar.isShowing()) {
            this.mTitleBar.hide();
        }
    }

    protected boolean isTitleBarShowing() {
        return this.mTitleBar.isShowing();
    }

    public boolean isEditingUrl() {
        return this.mTitleBar.isEditingUrl();
    }

    public void stopEditingUrl() {
        this.mTitleBar.getNavigationBar().stopEditingUrl();
    }

    public TitleBar getTitleBar() {
        return this.mTitleBar;
    }

    @Override
    public void showComboView(UI.ComboViews startingView, Bundle extras) {
        Intent intent = new Intent(this.mActivity, (Class<?>) ComboViewActivity.class);
        intent.putExtra("initial_view", startingView.name());
        intent.putExtra("combo_args", extras);
        Tab t = getActiveTab();
        if (t != null) {
            intent.putExtra("url", t.getUrl());
        }
        this.mActivity.startActivityForResult(intent, 1);
    }

    @Override
    public void showCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback callback) {
        if (this.mCustomView != null) {
            callback.onCustomViewHidden();
            return;
        }
        this.mOriginalOrientation = this.mActivity.getRequestedOrientation();
        FrameLayout decor = (FrameLayout) this.mActivity.getWindow().getDecorView();
        this.mFullscreenContainer = new FullscreenHolder(this.mActivity);
        this.mFullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
        decor.addView(this.mFullscreenContainer, COVER_SCREEN_PARAMS);
        this.mCustomView = view;
        setFullscreen(true);
        ((BrowserWebView) getWebView()).setVisibility(4);
        this.mCustomViewCallback = callback;
        this.mActivity.setRequestedOrientation(requestedOrientation);
    }

    @Override
    public void onHideCustomView() {
        ((BrowserWebView) getWebView()).setVisibility(0);
        if (this.mCustomView != null) {
            setFullscreen(false);
            FrameLayout decor = (FrameLayout) this.mActivity.getWindow().getDecorView();
            decor.removeView(this.mFullscreenContainer);
            this.mFullscreenContainer = null;
            this.mCustomView = null;
            this.mCustomViewCallback.onCustomViewHidden();
            this.mActivity.setRequestedOrientation(this.mOriginalOrientation);
        }
    }

    @Override
    public boolean isCustomViewShowing() {
        return this.mCustomView != null;
    }

    @Override
    public boolean isWebShowing() {
        return this.mCustomView == null;
    }

    @Override
    public void showAutoLogin(Tab tab) {
        updateAutoLogin(tab, true);
    }

    @Override
    public void hideAutoLogin(Tab tab) {
        updateAutoLogin(tab, true);
    }

    protected void updateNavigationState(Tab tab) {
    }

    protected void updateAutoLogin(Tab tab, boolean animate) {
        this.mTitleBar.updateAutoLogin(tab, animate);
    }

    protected void updateLockIconToLatest(Tab t) {
        if (t != null && t.inForeground()) {
            updateLockIconImage(t.getSecurityState());
        }
    }

    private void updateLockIconImage(Tab.SecurityState securityState) {
        Drawable d = null;
        if (securityState == Tab.SecurityState.SECURITY_STATE_SECURE) {
            d = this.mLockIconSecure;
        } else if (securityState == Tab.SecurityState.SECURITY_STATE_MIXED || securityState == Tab.SecurityState.SECURITY_STATE_BAD_CERTIFICATE) {
            d = this.mLockIconMixed;
        }
        this.mNavigationBar.setLock(d);
    }

    protected void setUrlTitle(Tab tab) {
        String url = tab.getUrl();
        String title = tab.getTitle();
        if (TextUtils.isEmpty(title)) {
        }
        if (tab.inForeground()) {
            this.mNavigationBar.setDisplayTitle(url);
        }
    }

    protected void setFavicon(Tab tab) {
        if (tab.inForeground()) {
            Bitmap icon = tab.getFavicon();
            this.mNavigationBar.setFavicon(icon);
        }
    }

    @Override
    public void onActionModeFinished(boolean inLoad) {
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
    }

    @Override
    public void onOptionsMenuOpened() {
    }

    @Override
    public void onExtendedMenuOpened() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onOptionsMenuClosed(boolean inLoad) {
    }

    @Override
    public void onExtendedMenuClosed(boolean inLoad) {
    }

    @Override
    public void onContextMenuCreated(Menu menu) {
    }

    @Override
    public void onContextMenuClosed(Menu menu, boolean inLoad) {
    }

    @Override
    public void setShouldShowErrorConsole(Tab tab, boolean flag) {
        if (tab != null) {
            ErrorConsoleView errorConsole = tab.getErrorConsole(true);
            if (flag) {
                if (errorConsole.numberOfErrors() > 0) {
                    errorConsole.showConsole(0);
                } else {
                    errorConsole.showConsole(2);
                }
                if (errorConsole.getParent() != null) {
                    this.mErrorConsoleContainer.removeView(errorConsole);
                }
                this.mErrorConsoleContainer.addView(errorConsole, new LinearLayout.LayoutParams(-1, -2));
                return;
            }
            this.mErrorConsoleContainer.removeView(errorConsole);
        }
    }

    @Override
    public Bitmap getDefaultVideoPoster() {
        if (this.mDefaultVideoPoster == null) {
            this.mDefaultVideoPoster = BitmapFactory.decodeResource(this.mActivity.getResources(), R.drawable.default_video_poster);
        }
        return this.mDefaultVideoPoster;
    }

    @Override
    public View getVideoLoadingProgressView() {
        if (this.mVideoProgressView == null) {
            LayoutInflater inflater = LayoutInflater.from(this.mActivity);
            this.mVideoProgressView = inflater.inflate(R.layout.video_loading_progress, (ViewGroup) null);
        }
        return this.mVideoProgressView;
    }

    @Override
    public void showMaxTabsWarning() {
        Toast warning = Toast.makeText(this.mActivity, this.mActivity.getString(R.string.max_tabs_warning), 0);
        warning.show();
    }

    protected WebView getWebView() {
        if (this.mActiveTab != null) {
            return this.mActiveTab.getWebView();
        }
        return null;
    }

    @Override
    public void setFullscreen(boolean enabled) {
        Window win = this.mActivity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (enabled) {
            winParams.flags |= 1024;
        } else {
            winParams.flags &= -1025;
            if (this.mCustomView != null) {
                this.mCustomView.setSystemUiVisibility(0);
            } else {
                this.mContentView.setSystemUiVisibility(0);
            }
        }
        win.setAttributes(winParams);
    }

    public Drawable getFaviconDrawable(Bitmap icon) {
        Drawable[] array = new Drawable[3];
        array[0] = new PaintDrawable(-16777216);
        PaintDrawable p = new PaintDrawable(-1);
        array[1] = p;
        if (icon == null) {
            array[2] = this.mGenericFavicon;
        } else {
            array[2] = new BitmapDrawable(icon);
        }
        LayerDrawable d = new LayerDrawable(array);
        d.setLayerInset(1, 1, 1, 1, 1);
        d.setLayerInset(2, 2, 2, 2, 2);
        return d;
    }

    public boolean isLoading() {
        if (this.mActiveTab != null) {
            return this.mActiveTab.inPageLoad();
        }
        return false;
    }

    public void suggestHideTitleBar() {
        if (!isLoading() && !isEditingUrl() && !this.mTitleBar.wantsToBeVisible() && !this.mNavigationBar.isMenuShowing()) {
            hideTitleBar();
        }
    }

    protected final void showTitleBarForDuration() {
        showTitleBarForDuration(1500L);
    }

    protected final void showTitleBarForDuration(long duration) {
        showTitleBar();
        Message msg = Message.obtain(this.mHandler, 1);
        this.mHandler.sendMessageDelayed(msg, duration);
    }

    protected void handleMessage(Message msg) {
    }

    @Override
    public void showWeb(boolean animate) {
        this.mUiController.hideCustomView();
    }

    static class FullscreenHolder extends FrameLayout {
        public FullscreenHolder(Context ctx) {
            super(ctx);
            setBackgroundColor(ctx.getResources().getColor(R.color.black));
        }

        @Override
        public boolean onTouchEvent(MotionEvent evt) {
            return true;
        }
    }

    public void addFixedTitleBar(View view) {
        this.mFixedTitlebarContainer.addView(view);
    }

    public void setContentViewMarginTop(int margin) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) this.mContentView.getLayoutParams();
        if (params.topMargin != margin) {
            params.topMargin = margin;
            this.mContentView.setLayoutParams(params);
        }
    }

    public boolean blockFocusAnimations() {
        return this.mBlockFocusAnimations;
    }

    @Override
    public void onVoiceResult(String result) {
        this.mNavigationBar.onVoiceResult(result);
    }
}
