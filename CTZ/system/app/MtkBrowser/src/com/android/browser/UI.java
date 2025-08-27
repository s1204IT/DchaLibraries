package com.android.browser;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import java.util.List;

/* loaded from: classes.dex */
public interface UI {

    public enum ComboViews {
        History,
        Bookmarks,
        Snapshots
    }

    void addTab(Tab tab);

    void attachSubWindow(View view);

    void attachTab(Tab tab);

    void bookmarkedStatusHasChanged(Tab tab);

    void closeTableDelay(Tab tab);

    void createSubWindow(Tab tab, WebView webView);

    void detachTab(Tab tab);

    boolean dispatchKey(int i, KeyEvent keyEvent);

    void editUrl(boolean z, boolean z2);

    Bitmap getDefaultVideoPoster();

    View getVideoLoadingProgressView();

    void hideAutoLogin(Tab tab);

    void hideIME();

    boolean isCustomViewShowing();

    boolean isWebShowing();

    boolean needsRestoreAllTabs();

    void onActionModeFinished(boolean z);

    void onActionModeStarted(ActionMode actionMode);

    boolean onBackKey();

    void onConfigurationChanged(Configuration configuration);

    void onContextMenuClosed(Menu menu, boolean z);

    void onContextMenuCreated(Menu menu);

    void onDestroy();

    void onExtendedMenuClosed(boolean z);

    void onExtendedMenuOpened();

    void onHideCustomView();

    boolean onMenuKey();

    boolean onOptionsItemSelected(MenuItem menuItem);

    void onOptionsMenuClosed(boolean z);

    void onOptionsMenuOpened();

    void onPageStopped(Tab tab);

    void onPause();

    boolean onPrepareOptionsMenu(Menu menu);

    void onProgressChanged(Tab tab);

    void onResume();

    void onSetWebView(Tab tab, WebView webView);

    void onTabDataChanged(Tab tab);

    void onVoiceResult(String str);

    void removeSubWindow(View view);

    void removeTab(Tab tab);

    void setActiveTab(Tab tab);

    void setFullscreen(boolean z);

    void setShouldShowErrorConsole(Tab tab, boolean z);

    void setUseQuickControls(boolean z);

    boolean shouldCaptureThumbnails();

    void showAutoLogin(Tab tab);

    void showComboView(ComboViews comboViews, Bundle bundle);

    void showCustomView(View view, int i, WebChromeClient.CustomViewCallback customViewCallback);

    void showMaxTabsWarning();

    void showWeb(boolean z);

    void updateBottomBarState(boolean z, boolean z2, boolean z3);

    void updateMenuState(Tab tab, Menu menu);

    void updateTabs(List<Tab> list);
}
