package com.android.browser;

import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.webkit.WebView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

class TabControl {
    private static final boolean DEBUG = Browser.DEBUG;
    private static long sNextId = 1;
    private final Controller mController;
    private int mCurrentTab = -1;
    private CopyOnWriteArrayList<Integer> mFreeTabIndex = new CopyOnWriteArrayList<>();
    private int mMaxTabs;
    private OnTabCountChangedListener mOnTabCountChangedListener;
    private OnThumbnailUpdatedListener mOnThumbnailUpdatedListener;
    private ArrayList<Tab> mTabQueue;
    private ArrayList<Tab> mTabs;

    public interface OnTabCountChangedListener {
        void onTabCountChanged();
    }

    public interface OnThumbnailUpdatedListener {
        void onThumbnailUpdated(Tab tab);
    }

    TabControl(Controller controller) {
        this.mController = controller;
        this.mMaxTabs = this.mController.getMaxTabs();
        this.mTabs = new ArrayList<>(this.mMaxTabs);
        this.mTabQueue = new ArrayList<>(this.mMaxTabs);
    }

    static synchronized long getNextId() {
        long j;
        j = sNextId;
        sNextId = 1 + j;
        return j;
    }

    WebView getCurrentWebView() {
        Tab t = getTab(this.mCurrentTab);
        if (t == null) {
            return null;
        }
        return t.getWebView();
    }

    WebView getCurrentTopWebView() {
        Tab t = getTab(this.mCurrentTab);
        if (t == null) {
            return null;
        }
        return t.getTopWindow();
    }

    WebView getCurrentSubWindow() {
        Tab t = getTab(this.mCurrentTab);
        if (t == null) {
            return null;
        }
        return t.getSubWebView();
    }

    List<Tab> getTabs() {
        return this.mTabs;
    }

    Tab getTab(int position) {
        if (position >= 0 && position < this.mTabs.size()) {
            return this.mTabs.get(position);
        }
        return null;
    }

    Tab getCurrentTab() {
        return getTab(this.mCurrentTab);
    }

    int getCurrentPosition() {
        return this.mCurrentTab;
    }

    int getTabPosition(Tab tab) {
        if (tab == null) {
            return -1;
        }
        return this.mTabs.indexOf(tab);
    }

    boolean canCreateNewTab() {
        return this.mMaxTabs > this.mTabs.size();
    }

    void addPreloadedTab(Tab tab) {
        for (Tab current : this.mTabs) {
            if (current != null && current.getId() == tab.getId()) {
                throw new IllegalStateException("Tab with id " + tab.getId() + " already exists: " + current.toString());
            }
        }
        this.mTabs.add(tab);
        if (this.mOnTabCountChangedListener != null) {
            this.mOnTabCountChangedListener.onTabCountChanged();
        }
        tab.setController(this.mController);
        this.mController.onSetWebView(tab, tab.getWebView());
        tab.putInBackground();
    }

    Tab createNewTab(boolean privateBrowsing) {
        return createNewTab(null, privateBrowsing);
    }

    Tab createNewTab(Bundle state, boolean privateBrowsing) {
        this.mTabs.size();
        if (!canCreateNewTab()) {
            return null;
        }
        WebView w = createNewWebView(privateBrowsing);
        Tab t = new Tab(this.mController, w, state);
        this.mTabs.add(t);
        if (this.mOnTabCountChangedListener != null) {
            this.mOnTabCountChangedListener.onTabCountChanged();
        }
        t.putInBackground();
        return t;
    }

    void removeParentChildRelationShips() {
        for (Tab tab : this.mTabs) {
            tab.removeFromTree();
        }
    }

    boolean removeTab(Tab t) {
        if (t == null) {
            return false;
        }
        Tab current = getCurrentTab();
        this.mTabs.remove(t);
        if (current == t) {
            t.putInBackground();
            this.mCurrentTab = -1;
        } else {
            this.mCurrentTab = getTabPosition(current);
        }
        t.destroy();
        t.removeFromTree();
        this.mTabQueue.remove(t);
        if (this.mOnTabCountChangedListener != null) {
            this.mOnTabCountChangedListener.onTabCountChanged();
            return true;
        }
        return true;
    }

    void destroy() {
        Log.d("TabControl", "TabControl.destroy()--->Destroy all the tabs");
        for (Tab t : this.mTabs) {
            t.destroy();
        }
        this.mTabs.clear();
        this.mTabQueue.clear();
    }

    int getTabCount() {
        return this.mTabs.size();
    }

    void saveState(Bundle outState) {
        int numTabs = getTabCount();
        if (numTabs == 0) {
            return;
        }
        long[] ids = new long[numTabs];
        int i = 0;
        for (Tab tab : this.mTabs) {
            Bundle tabState = tab.saveState();
            if (tabState != null) {
                int i2 = i + 1;
                ids[i] = tab.getId();
                String key = Long.toString(tab.getId());
                if (outState.containsKey(key)) {
                    for (Tab dt : this.mTabs) {
                        Log.e("TabControl", dt.toString());
                    }
                    throw new IllegalStateException("Error saving state, duplicate tab ids!");
                }
                outState.putBundle(key, tabState);
                i = i2;
            } else {
                ids[i] = -1;
                tab.deleteThumbnail();
                i++;
            }
        }
        if (outState.isEmpty()) {
            return;
        }
        outState.putLongArray("positions", ids);
        Tab current = getCurrentTab();
        long cid = -1;
        if (current != null) {
            cid = current.getId();
        }
        outState.putLong("current", cid);
    }

    long canRestoreState(Bundle inState, boolean restoreIncognitoTabs) {
        long[] ids = inState != null ? inState.getLongArray("positions") : null;
        if (ids == null) {
            return -1L;
        }
        long oldcurrent = inState.getLong("current");
        if (restoreIncognitoTabs || (hasState(oldcurrent, inState) && !isIncognito(oldcurrent, inState))) {
            return oldcurrent;
        }
        for (long id : ids) {
            if (hasState(id, inState) && !isIncognito(id, inState)) {
                return id;
            }
        }
        return -1L;
    }

    private boolean hasState(long id, Bundle state) {
        Bundle tab;
        return (id == -1 || (tab = state.getBundle(Long.toString(id))) == null || tab.isEmpty()) ? false : true;
    }

    private boolean isIncognito(long id, Bundle state) {
        Bundle tabstate = state.getBundle(Long.toString(id));
        if (tabstate != null && !tabstate.isEmpty()) {
            return tabstate.getBoolean("privateBrowsingEnabled");
        }
        return false;
    }

    void restoreState(Bundle inState, long currentId, boolean restoreIncognitoTabs, boolean restoreAll) {
        Tab parent;
        if (currentId == -1) {
            return;
        }
        long[] ids = inState.getLongArray("positions");
        long maxId = -9223372036854775807L;
        HashMap<Long, Tab> tabMap = new HashMap<>();
        for (long id : ids) {
            if (id > maxId) {
                maxId = id;
            }
            String idkey = Long.toString(id);
            Bundle state = inState.getBundle(idkey);
            if (state != null && !state.isEmpty() && (restoreIncognitoTabs || !state.getBoolean("privateBrowsingEnabled"))) {
                if (id == currentId || restoreAll) {
                    Tab t = createNewTab(state, false);
                    if (t != null) {
                        tabMap.put(Long.valueOf(id), t);
                        if (id == currentId) {
                            setCurrentTab(t);
                        }
                    }
                } else {
                    Tab t2 = new Tab(this.mController, state);
                    tabMap.put(Long.valueOf(id), t2);
                    this.mTabs.add(t2);
                    if (this.mOnTabCountChangedListener != null) {
                        this.mOnTabCountChangedListener.onTabCountChanged();
                    }
                    this.mTabQueue.add(0, t2);
                }
            }
        }
        sNextId = 1 + maxId;
        if (this.mCurrentTab == -1 && getTabCount() > 0) {
            setCurrentTab(getTab(0));
        }
        for (long id2 : ids) {
            Tab tab = tabMap.get(Long.valueOf(id2));
            Bundle b = inState.getBundle(Long.toString(id2));
            if (b != null && tab != null) {
                long parentId = b.getLong("parentTab", -1L);
                if (parentId != -1 && (parent = tabMap.get(Long.valueOf(parentId))) != null) {
                    parent.addChildTab(tab);
                }
            }
        }
    }

    void freeMemory() {
        if (getTabCount() == 0) {
            return;
        }
        String optimize = SystemProperties.get("ro.mtk_gmo_ram_optimize");
        Vector<Tab> tabs = getHalfLeastUsedTabs(getCurrentTab());
        this.mFreeTabIndex.clear();
        if (tabs.size() > 0) {
            Log.w("TabControl", "Free " + tabs.size() + " tabs in the browser");
            for (Tab t : tabs) {
                this.mFreeTabIndex.add(Integer.valueOf(getTabPosition(t) + 1));
                t.saveState();
                t.destroy();
            }
            if (optimize == null || !optimize.equals("1")) {
                return;
            }
        }
        Log.w("TabControl", "Free WebView's unused memory and cache");
        WebView view = getCurrentWebView();
        if (view == null) {
            return;
        }
        view.freeMemory();
    }

    int getVisibleWebviewNums() {
        int visibleWebview = 0;
        if (this.mTabs.size() == 0) {
            return 0;
        }
        for (Tab t : this.mTabs) {
            if (t != null && t.getWebView() != null) {
                visibleWebview++;
            }
        }
        return visibleWebview;
    }

    protected CopyOnWriteArrayList<Integer> getFreeTabIndex() {
        return this.mFreeTabIndex;
    }

    private Vector<Tab> getHalfLeastUsedTabs(Tab current) {
        Vector<Tab> tabsToGo = new Vector<>();
        if (getTabCount() == 1 || current == null || this.mTabQueue.size() == 0) {
            return tabsToGo;
        }
        int openTabCount = 0;
        for (Tab t : this.mTabQueue) {
            if (t != null && t.getWebView() != null) {
                openTabCount++;
                if (t != current && t != current.getParent()) {
                    tabsToGo.add(t);
                }
            }
        }
        int openTabCount2 = openTabCount / 2;
        if (tabsToGo.size() > openTabCount2) {
            tabsToGo.setSize(openTabCount2);
        }
        return tabsToGo;
    }

    Tab getLeastUsedTab(Tab current) {
        if (getTabCount() == 1 || current == null || this.mTabQueue.size() == 0) {
            return null;
        }
        for (Tab t : this.mTabQueue) {
            if (t != null && t.getWebView() != null && t != current && t != current.getParent()) {
                return t;
            }
        }
        return null;
    }

    Tab getTabFromView(WebView view) {
        for (Tab t : this.mTabs) {
            if (t.getSubWebView() == view || t.getWebView() == view) {
                return t;
            }
        }
        return null;
    }

    Tab getTabFromAppId(String id) {
        if (id == null) {
            return null;
        }
        for (Tab t : this.mTabs) {
            if (id.equals(t.getAppId())) {
                return t;
            }
        }
        return null;
    }

    void stopAllLoading() {
        for (Tab t : this.mTabs) {
            WebView webview = t.getWebView();
            if (webview != null) {
                webview.stopLoading();
            }
            WebView subview = t.getSubWebView();
            if (subview != null) {
                subview.stopLoading();
            }
        }
    }

    private boolean tabMatchesUrl(Tab t, String url) {
        if (url.equals(t.getUrl())) {
            return true;
        }
        return url.equals(t.getOriginalUrl());
    }

    Tab findTabWithUrl(String url) {
        if (url == null) {
            return null;
        }
        Tab currentTab = getCurrentTab();
        if (currentTab != null && tabMatchesUrl(currentTab, url)) {
            return currentTab;
        }
        for (Tab tab : this.mTabs) {
            if (tabMatchesUrl(tab, url)) {
                return tab;
            }
        }
        return null;
    }

    void recreateWebView(Tab t) {
        WebView w = t.getWebView();
        if (w != null) {
            t.destroy();
        }
        t.setWebView(createNewWebView(), false);
        if (getCurrentTab() != t) {
            return;
        }
        setCurrentTab(t, true);
    }

    private WebView createNewWebView() {
        return createNewWebView(false);
    }

    private WebView createNewWebView(boolean privateBrowsing) {
        return this.mController.getWebViewFactory().createWebView(privateBrowsing);
    }

    boolean setCurrentTab(Tab newTab) {
        return setCurrentTab(newTab, false);
    }

    private boolean setCurrentTab(Tab newTab, boolean force) {
        boolean zCanScrollVertically;
        Tab current = getTab(this.mCurrentTab);
        if (current == newTab && !force) {
            return true;
        }
        if (current != null) {
            current.putInBackground();
            this.mCurrentTab = -1;
        }
        if (newTab == null) {
            return false;
        }
        int index = this.mTabQueue.indexOf(newTab);
        if (index != -1) {
            this.mTabQueue.remove(index);
        }
        this.mTabQueue.add(newTab);
        this.mCurrentTab = this.mTabs.indexOf(newTab);
        WebView mainView = newTab.getWebView();
        boolean needRestore = mainView == null;
        if (needRestore) {
            WebView mainView2 = createNewWebView();
            newTab.setWebView(mainView2);
        }
        newTab.putInForeground();
        if (newTab.getWebView().canScrollVertically(-1)) {
            zCanScrollVertically = true;
        } else {
            zCanScrollVertically = newTab.getWebView().canScrollVertically(1);
        }
        this.mController.getUi().updateBottomBarState(zCanScrollVertically, newTab.canGoBack() || newTab.getParent() != null, newTab.canGoForward());
        return true;
    }

    void setActiveTab(Tab tab) {
        this.mController.setActiveTab(tab);
    }

    public void setOnThumbnailUpdatedListener(OnThumbnailUpdatedListener listener) {
        this.mOnThumbnailUpdatedListener = listener;
        for (Tab t : this.mTabs) {
            WebView web = t.getWebView();
            if (web != null) {
                if (listener == null) {
                    t = null;
                }
                web.setPictureListener(t);
            }
        }
    }

    public OnThumbnailUpdatedListener getOnThumbnailUpdatedListener() {
        return this.mOnThumbnailUpdatedListener;
    }

    public void setOnTabCountChangedListener(OnTabCountChangedListener listener) {
        this.mOnTabCountChangedListener = listener;
    }
}
