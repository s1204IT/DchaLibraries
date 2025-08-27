package com.android.browser;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import com.android.browser.UI;
import java.util.List;

/* loaded from: classes.dex */
public interface UiController {
    void attachSubWindow(Tab tab);

    void bookmarkCurrentPage();

    void bookmarksOrHistoryPicker(UI.ComboViews comboViews);

    void closeCurrentTab();

    void closeTab(Tab tab);

    Intent createBookmarkCurrentPageIntent(boolean z);

    void endActionMode();

    void findOnPage();

    Activity getActivity();

    Tab getCurrentTab();

    WebView getCurrentTopWebView();

    WebView getCurrentWebView();

    BrowserSettings getSettings();

    TabControl getTabControl();

    List<Tab> getTabs();

    UI getUi();

    void handleNewIntent(Intent intent);

    void hideCustomView();

    boolean isInCustomActionMode();

    void loadUrl(Tab tab, String str);

    boolean onOptionsItemSelected(MenuItem menuItem);

    Tab openIncognitoTab();

    void openPreferences();

    Tab openTab(String str, boolean z, boolean z2, boolean z3);

    Tab openTabToHomePage();

    void removeSubWindow(Tab tab);

    void setActiveTab(Tab tab);

    void setBlockEvents(boolean z);

    void shareCurrentPage();

    boolean shouldShowErrorConsole();

    void showPageInfo();

    void stopLoading();

    boolean supportsVoice();

    boolean switchToTab(Tab tab);

    void toggleUserAgent();

    void updateMenuState(Tab tab, Menu menu);
}
