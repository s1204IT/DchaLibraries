package com.android.browser;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import com.android.browser.UI;
import java.util.List;

public class XLargeUi extends BaseUi {
    private ActionBar mActionBar;
    private PaintDrawable mFaviconBackground;
    private Handler mHandler;
    private NavigationBarTablet mNavBar;
    private TabBar mTabBar;

    public XLargeUi(Activity browser, UiController controller) {
        super(browser, controller);
        this.mHandler = new Handler();
        this.mNavBar = (NavigationBarTablet) this.mTitleBar.getNavigationBar();
        this.mTabBar = new TabBar(this.mActivity, this.mUiController, this);
        this.mActionBar = this.mActivity.getActionBar();
        setupActionBar();
        setUseQuickControls(BrowserSettings.getInstance().useQuickControls());
    }

    private void setupActionBar() {
        this.mActionBar.setNavigationMode(0);
        this.mActionBar.setDisplayOptions(16);
        this.mActionBar.setCustomView(this.mTabBar);
    }

    @Override
    public void showComboView(UI.ComboViews startWith, Bundle extras) {
        super.showComboView(startWith, extras);
        if (!this.mUseQuickControls) {
            return;
        }
        this.mActionBar.show();
    }

    @Override
    public void setUseQuickControls(boolean useQuickControls) {
        super.setUseQuickControls(useQuickControls);
        checkHideActionBar();
        if (!useQuickControls) {
            this.mActionBar.show();
        }
        this.mTabBar.setUseQuickControls(this.mUseQuickControls);
        for (Tab t : this.mTabControl.getTabs()) {
            t.updateShouldCaptureThumbnails();
        }
    }

    private void checkHideActionBar() {
        if (!this.mUseQuickControls) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                XLargeUi.this.mActionBar.hide();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mNavBar.clearCompletions();
        checkHideActionBar();
    }

    @Override
    public void onDestroy() {
        hideTitleBar();
    }

    void stopWebViewScrolling() {
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem bm = menu.findItem(R.id.bookmarks_menu_id);
        if (bm != null) {
            bm.setVisible(false);
            return true;
        }
        return true;
    }

    @Override
    public void addTab(Tab tab) {
        this.mTabBar.onNewTab(tab);
    }

    protected void onAddTabCompleted(Tab tab) {
        checkHideActionBar();
    }

    @Override
    public void setActiveTab(Tab tab) {
        this.mTitleBar.cancelTitleBarAnimation(true);
        this.mTitleBar.setSkipTitleBarAnimations(true);
        super.setActiveTab(tab);
        BrowserWebView view = (BrowserWebView) tab.getWebView();
        if (view == null) {
            Log.e("XLargeUi", "active tab with no webview detected");
            return;
        }
        this.mTabBar.onSetActiveTab(tab);
        updateLockIconToLatest(tab);
        this.mTitleBar.setSkipTitleBarAnimations(false);
    }

    @Override
    public void updateTabs(List<Tab> tabs) {
        this.mTabBar.updateTabs(tabs);
        checkHideActionBar();
    }

    @Override
    public void removeTab(Tab tab) {
        this.mTitleBar.cancelTitleBarAnimation(true);
        this.mTitleBar.setSkipTitleBarAnimations(true);
        super.removeTab(tab);
        this.mTabBar.onRemoveTab(tab);
        this.mTitleBar.setSkipTitleBarAnimations(false);
    }

    protected void onRemoveTabCompleted(Tab tab) {
        checkHideActionBar();
    }

    int getContentWidth() {
        if (this.mContentView != null) {
            return this.mContentView.getWidth();
        }
        return 0;
    }

    @Override
    public void editUrl(boolean clearInput, boolean forceIME) {
        super.editUrl(clearInput, forceIME);
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        if (this.mTitleBar.isEditingUrl()) {
            return;
        }
        hideTitleBar();
    }

    @Override
    public void onActionModeFinished(boolean inLoad) {
        checkHideActionBar();
        if (!inLoad) {
            return;
        }
        showTitleBar();
    }

    @Override
    protected void updateNavigationState(Tab tab) {
        this.mNavBar.updateNavigationState(tab);
    }

    @Override
    public void setUrlTitle(Tab tab) {
        super.setUrlTitle(tab);
        this.mTabBar.onUrlAndTitle(tab, tab.getUrl(), tab.getTitle());
    }

    @Override
    public void setFavicon(Tab tab) {
        super.setFavicon(tab);
        this.mTabBar.onFavicon(tab, tab.getFavicon());
    }

    @Override
    public void showCustomView(View view, int requestedOrientation, WebChromeClient.CustomViewCallback callback) {
        super.showCustomView(view, requestedOrientation, callback);
        this.mActionBar.hide();
    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();
        if (!this.mUseQuickControls) {
            this.mActionBar.show();
        }
        checkHideActionBar();
    }

    @Override
    public boolean dispatchKey(int code, KeyEvent event) {
        if (this.mActiveTab != null) {
            WebView web = this.mActiveTab.getWebView();
            if (event.getAction() == 0) {
                switch (code) {
                    case 19:
                    case 21:
                    case 61:
                        if (web != null && web.hasFocus() && !this.mTitleBar.hasFocus()) {
                            editUrl(false, false);
                            return true;
                        }
                        break;
                }
                boolean ctrl = event.hasModifiers(4096);
                if (!ctrl && isTypingKey(event) && !this.mTitleBar.isEditingUrl()) {
                    editUrl(true, false);
                    return this.mContentView.dispatchKeyEvent(event);
                }
            }
        }
        return false;
    }

    private boolean isTypingKey(KeyEvent evt) {
        return evt.getUnicodeChar() > 0;
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return this.mUseQuickControls;
    }

    private Drawable getFaviconBackground() {
        if (this.mFaviconBackground == null) {
            this.mFaviconBackground = new PaintDrawable();
            Resources res = this.mActivity.getResources();
            this.mFaviconBackground.getPaint().setColor(res.getColor(R.color.tabFaviconBackground));
            this.mFaviconBackground.setCornerRadius(res.getDimension(R.dimen.tab_favicon_corner_radius));
        }
        return this.mFaviconBackground;
    }

    @Override
    public Drawable getFaviconDrawable(Bitmap icon) {
        Drawable[] array = new Drawable[2];
        array[0] = getFaviconBackground();
        if (icon == null) {
            array[1] = this.mGenericFavicon;
        } else {
            array[1] = new BitmapDrawable(this.mActivity.getResources(), icon);
        }
        LayerDrawable d = new LayerDrawable(array);
        d.setLayerInset(1, 2, 2, 2, 2);
        return d;
    }

    @Override
    public void hideIME() {
    }

    @Override
    public void onProgressChanged(Tab tab) {
        super.onProgressChanged(tab);
        if (!tab.inForeground()) {
            return;
        }
        tab.updateBookmarkedStatus();
    }
}
