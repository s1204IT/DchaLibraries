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
import java.util.Iterator;
import java.util.List;

public class XLargeUi extends BaseUi {
    private ActionBar mActionBar;
    private PaintDrawable mFaviconBackground;
    private Handler mHandler;
    private NavigationBarTablet mNavBar;
    private TabBar mTabBar;

    public XLargeUi(Activity activity, UiController uiController) {
        super(activity, uiController);
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
    public void showComboView(UI.ComboViews comboViews, Bundle bundle) {
        super.showComboView(comboViews, bundle);
        if (this.mUseQuickControls) {
            this.mActionBar.show();
        }
    }

    @Override
    public void setUseQuickControls(boolean z) {
        super.setUseQuickControls(z);
        checkHideActionBar();
        if (!z) {
            this.mActionBar.show();
        }
        this.mTabBar.setUseQuickControls(this.mUseQuickControls);
        Iterator<Tab> it = this.mTabControl.getTabs().iterator();
        while (it.hasNext()) {
            it.next().updateShouldCaptureThumbnails();
        }
    }

    private void checkHideActionBar() {
        if (this.mUseQuickControls) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    XLargeUi.this.mActionBar.hide();
                }
            });
        }
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
        MenuItem menuItemFindItem = menu.findItem(R.id.bookmarks_menu_id);
        if (menuItemFindItem != null) {
            menuItemFindItem.setVisible(false);
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
        if (((BrowserWebView) tab.getWebView()) == null) {
            Log.e("XLargeUi", "active tab with no webview detected");
            return;
        }
        this.mTabBar.onSetActiveTab(tab);
        updateLockIconToLatest(tab);
        this.mTitleBar.setSkipTitleBarAnimations(false);
    }

    @Override
    public void updateTabs(List<Tab> list) {
        this.mTabBar.updateTabs(list);
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
    public void editUrl(boolean z, boolean z2) {
        super.editUrl(z, z2);
    }

    @Override
    public void onActionModeStarted(ActionMode actionMode) {
        if (!this.mTitleBar.isEditingUrl()) {
            hideTitleBar();
        }
    }

    @Override
    public void onActionModeFinished(boolean z) {
        checkHideActionBar();
        if (z) {
            showTitleBar();
        }
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
    public void showCustomView(View view, int i, WebChromeClient.CustomViewCallback customViewCallback) {
        super.showCustomView(view, i, customViewCallback);
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
    public boolean dispatchKey(int i, KeyEvent keyEvent) {
        if (this.mActiveTab != null) {
            WebView webView = this.mActiveTab.getWebView();
            if (keyEvent.getAction() == 0) {
                if ((i == 19 || i == 21 || i == 61) && webView != null && webView.hasFocus() && !this.mTitleBar.hasFocus()) {
                    editUrl(false, false);
                    return true;
                }
                if (!keyEvent.hasModifiers(4096) && isTypingKey(keyEvent) && !this.mTitleBar.isEditingUrl()) {
                    editUrl(true, false);
                    return this.mContentView.dispatchKeyEvent(keyEvent);
                }
            }
        }
        return false;
    }

    private boolean isTypingKey(KeyEvent keyEvent) {
        return keyEvent.getUnicodeChar() > 0;
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return this.mUseQuickControls;
    }

    private Drawable getFaviconBackground() {
        if (this.mFaviconBackground == null) {
            this.mFaviconBackground = new PaintDrawable();
            Resources resources = this.mActivity.getResources();
            this.mFaviconBackground.getPaint().setColor(resources.getColor(R.color.tabFaviconBackground));
            this.mFaviconBackground.setCornerRadius(resources.getDimension(R.dimen.tab_favicon_corner_radius));
        }
        return this.mFaviconBackground;
    }

    @Override
    public Drawable getFaviconDrawable(Bitmap bitmap) {
        Drawable[] drawableArr = new Drawable[2];
        drawableArr[0] = getFaviconBackground();
        if (bitmap == null) {
            drawableArr[1] = this.mGenericFavicon;
        } else {
            drawableArr[1] = new BitmapDrawable(this.mActivity.getResources(), bitmap);
        }
        LayerDrawable layerDrawable = new LayerDrawable(drawableArr);
        layerDrawable.setLayerInset(1, 2, 2, 2, 2);
        return layerDrawable;
    }

    @Override
    public void hideIME() {
    }

    @Override
    public void onProgressChanged(Tab tab) {
        super.onProgressChanged(tab);
        if (tab.inForeground()) {
            tab.updateBookmarkedStatus();
        }
    }
}
