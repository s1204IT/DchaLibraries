package com.android.browser;

import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.webkit.WebView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    private WebView createNewWebView() {
        return createNewWebView(false);
    }

    private WebView createNewWebView(boolean z) {
        return this.mController.getWebViewFactory().createWebView(z);
    }

    private Vector<Tab> getHalfLeastUsedTabs(Tab tab) {
        int i;
        Vector<Tab> vector = new Vector<>();
        if (getTabCount() == 1 || tab == null) {
            return vector;
        }
        if (this.mTabQueue.size() == 0) {
            return vector;
        }
        int i2 = 0;
        Iterator<Tab> it = this.mTabQueue.iterator();
        while (true) {
            i = i2;
            if (!it.hasNext()) {
                break;
            }
            Tab next = it.next();
            if (next != null && next.getWebView() != null) {
                i++;
                if (next != tab && next != tab.getParent()) {
                    vector.add(next);
                }
            }
            i2 = i;
        }
        String str = SystemProperties.get("ro.vendor.gmo.ram_optimize");
        int i3 = (i <= 2 || str == null || !str.equals("1")) ? i / 2 : (i + 1) / 2;
        if (vector.size() > i3) {
            vector.setSize(i3);
        }
        return vector;
    }

    static long getNextId() {
        long j;
        synchronized (TabControl.class) {
            try {
                j = sNextId;
                sNextId = 1 + j;
            } catch (Throwable th) {
                throw th;
            }
        }
        return j;
    }

    private boolean hasState(long j, Bundle bundle) {
        Bundle bundle2;
        return (j == -1 || (bundle2 = bundle.getBundle(Long.toString(j))) == null || bundle2.isEmpty()) ? false : true;
    }

    private boolean isIncognito(long j, Bundle bundle) {
        Bundle bundle2 = bundle.getBundle(Long.toString(j));
        if (bundle2 == null || bundle2.isEmpty()) {
            return false;
        }
        return bundle2.getBoolean("privateBrowsingEnabled");
    }

    private boolean setCurrentTab(Tab tab, boolean z) {
        Tab tab2 = getTab(this.mCurrentTab);
        if (tab2 == tab && !z) {
            return true;
        }
        if (tab2 != null) {
            tab2.putInBackground();
            this.mCurrentTab = -1;
        }
        if (tab == null) {
            return false;
        }
        int iIndexOf = this.mTabQueue.indexOf(tab);
        if (iIndexOf != -1) {
            this.mTabQueue.remove(iIndexOf);
        }
        this.mTabQueue.add(tab);
        this.mCurrentTab = this.mTabs.indexOf(tab);
        if (tab.getWebView() == null) {
            tab.setWebView(createNewWebView());
        }
        tab.putInForeground();
        this.mController.getUi().updateBottomBarState(tab.getWebView().canScrollVertically(-1) || tab.getWebView().canScrollVertically(1), tab.canGoBack() || tab.getParent() != null, tab.canGoForward());
        return true;
    }

    private boolean tabMatchesUrl(Tab tab, String str) {
        return str.equals(tab.getUrl()) || str.equals(tab.getOriginalUrl());
    }

    void addPreloadedTab(Tab tab) {
        for (Tab tab2 : this.mTabs) {
            if (tab2 != null && tab2.getId() == tab.getId()) {
                throw new IllegalStateException("Tab with id " + tab.getId() + " already exists: " + tab2.toString());
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

    boolean canCreateNewTab() {
        return this.mMaxTabs > this.mTabs.size();
    }

    long canRestoreState(Bundle bundle, boolean z) {
        long[] longArray = bundle == null ? null : bundle.getLongArray("positions");
        if (longArray == null) {
            return -1L;
        }
        long j = bundle.getLong("current");
        if (!z && (!hasState(j, bundle) || isIncognito(j, bundle))) {
            int length = longArray.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    j = -1;
                    break;
                }
                j = longArray[i];
                if (hasState(j, bundle) && !isIncognito(j, bundle)) {
                    break;
                }
                i++;
            }
        }
        return j;
    }

    Tab createNewTab(Bundle bundle, boolean z) {
        this.mTabs.size();
        if (!canCreateNewTab()) {
            return null;
        }
        Tab tab = new Tab(this.mController, createNewWebView(z), bundle);
        this.mTabs.add(tab);
        if (this.mOnTabCountChangedListener != null) {
            this.mOnTabCountChangedListener.onTabCountChanged();
        }
        tab.putInBackground();
        return tab;
    }

    Tab createNewTab(boolean z) {
        return createNewTab(null, z);
    }

    void destroy() {
        Log.d("TabControl", "TabControl.destroy()--->Destroy all the tabs");
        Iterator<Tab> it = this.mTabs.iterator();
        while (it.hasNext()) {
            it.next().destroy();
        }
        this.mTabs.clear();
        this.mTabQueue.clear();
    }

    Tab findTabWithUrl(String str) {
        if (str == null) {
            return null;
        }
        Tab currentTab = getCurrentTab();
        if (currentTab != null && tabMatchesUrl(currentTab, str)) {
            return currentTab;
        }
        for (Tab tab : this.mTabs) {
            if (tabMatchesUrl(tab, str)) {
                return tab;
            }
        }
        return null;
    }

    void freeMemory() {
        if (getTabCount() == 0) {
            return;
        }
        String str = SystemProperties.get("ro.vendor.gmo.ram_optimize");
        Vector<Tab> halfLeastUsedTabs = getHalfLeastUsedTabs(getCurrentTab());
        this.mFreeTabIndex.clear();
        if (halfLeastUsedTabs.size() > 0) {
            Log.w("TabControl", "Free " + halfLeastUsedTabs.size() + " tabs in the browser");
            for (Tab tab : halfLeastUsedTabs) {
                this.mFreeTabIndex.add(Integer.valueOf(getTabPosition(tab) + 1));
                tab.saveState();
                tab.destroy();
            }
            if (str == null || !str.equals("1")) {
                return;
            }
        }
        Log.w("TabControl", "Free WebView's unused memory and cache");
        WebView currentWebView = getCurrentWebView();
        if (currentWebView != null) {
            currentWebView.freeMemory();
        }
    }

    int getCurrentPosition() {
        return this.mCurrentTab;
    }

    WebView getCurrentSubWindow() {
        Tab tab = getTab(this.mCurrentTab);
        if (tab == null) {
            return null;
        }
        return tab.getSubWebView();
    }

    Tab getCurrentTab() {
        return getTab(this.mCurrentTab);
    }

    WebView getCurrentTopWebView() {
        Tab tab = getTab(this.mCurrentTab);
        if (tab == null) {
            return null;
        }
        return tab.getTopWindow();
    }

    WebView getCurrentWebView() {
        Tab tab = getTab(this.mCurrentTab);
        if (tab == null) {
            return null;
        }
        return tab.getWebView();
    }

    protected CopyOnWriteArrayList<Integer> getFreeTabIndex() {
        return this.mFreeTabIndex;
    }

    Tab getLeastUsedTab(Tab tab) {
        if (getTabCount() == 1 || tab == null) {
            return null;
        }
        if (this.mTabQueue.size() == 0) {
            return null;
        }
        for (Tab tab2 : this.mTabQueue) {
            if (tab2 != null && tab2.getWebView() != null && tab2 != tab && tab2 != tab.getParent()) {
                return tab2;
            }
        }
        return null;
    }

    public OnThumbnailUpdatedListener getOnThumbnailUpdatedListener() {
        return this.mOnThumbnailUpdatedListener;
    }

    Tab getTab(int i) {
        if (i < 0 || i >= this.mTabs.size()) {
            return null;
        }
        return this.mTabs.get(i);
    }

    int getTabCount() {
        return this.mTabs.size();
    }

    Tab getTabFromAppId(String str) {
        if (str == null) {
            return null;
        }
        for (Tab tab : this.mTabs) {
            if (str.equals(tab.getAppId())) {
                return tab;
            }
        }
        return null;
    }

    Tab getTabFromView(WebView webView) {
        Iterator<Tab> it = this.mTabs.iterator();
        while (it.hasNext()) {
            Tab next = it.next();
            if (next.getSubWebView() == webView || next.getWebView() == webView) {
                return next;
            }
        }
        return null;
    }

    int getTabPosition(Tab tab) {
        if (tab == null) {
            return -1;
        }
        return this.mTabs.indexOf(tab);
    }

    List<Tab> getTabs() {
        return this.mTabs;
    }

    int getVisibleWebviewNums() {
        int i = 0;
        if (this.mTabs.size() == 0) {
            return 0;
        }
        Iterator<Tab> it = this.mTabs.iterator();
        while (true) {
            int i2 = i;
            if (!it.hasNext()) {
                return i2;
            }
            Tab next = it.next();
            if (next != null && next.getWebView() != null) {
                i2++;
            }
            i = i2;
        }
    }

    void recreateWebView(Tab tab) {
        if (tab.getWebView() != null) {
            tab.destroy();
        }
        tab.setWebView(createNewWebView(), false);
        if (getCurrentTab() == tab) {
            setCurrentTab(tab, true);
        }
    }

    void removeParentChildRelationShips() {
        Iterator<Tab> it = this.mTabs.iterator();
        while (it.hasNext()) {
            it.next().removeFromTree();
        }
    }

    boolean removeTab(Tab tab) {
        if (tab == null) {
            return false;
        }
        Tab currentTab = getCurrentTab();
        this.mTabs.remove(tab);
        if (currentTab == tab) {
            tab.putInBackground();
            this.mCurrentTab = -1;
        } else {
            this.mCurrentTab = getTabPosition(currentTab);
        }
        tab.destroy();
        tab.removeFromTree();
        this.mTabQueue.remove(tab);
        if (this.mOnTabCountChangedListener != null) {
            this.mOnTabCountChangedListener.onTabCountChanged();
        }
        return true;
    }

    void restoreState(Bundle bundle, long j, boolean z, boolean z2) {
        Tab tab;
        if (j == -1) {
            return;
        }
        long[] longArray = bundle.getLongArray("positions");
        HashMap map = new HashMap();
        long j2 = -9223372036854775807L;
        for (long j3 : longArray) {
            if (j3 > j2) {
                j2 = j3;
            }
            Bundle bundle2 = bundle.getBundle(Long.toString(j3));
            if (bundle2 != null && !bundle2.isEmpty() && (z || !bundle2.getBoolean("privateBrowsingEnabled"))) {
                if (j3 == j || z2) {
                    Tab tabCreateNewTab = createNewTab(bundle2, false);
                    if (tabCreateNewTab != null) {
                        map.put(Long.valueOf(j3), tabCreateNewTab);
                        if (j3 == j) {
                            setCurrentTab(tabCreateNewTab);
                        }
                    }
                } else {
                    Tab tab2 = new Tab(this.mController, bundle2);
                    map.put(Long.valueOf(j3), tab2);
                    this.mTabs.add(tab2);
                    if (this.mOnTabCountChangedListener != null) {
                        this.mOnTabCountChangedListener.onTabCountChanged();
                    }
                    this.mTabQueue.add(0, tab2);
                }
            }
        }
        sNextId = j2 + 1;
        if (this.mCurrentTab == -1 && getTabCount() > 0) {
            setCurrentTab(getTab(0));
        }
        for (long j4 : longArray) {
            Tab tab3 = (Tab) map.get(Long.valueOf(j4));
            Bundle bundle3 = bundle.getBundle(Long.toString(j4));
            if (bundle3 != null && tab3 != null) {
                long j5 = bundle3.getLong("parentTab", -1L);
                if (j5 != -1 && (tab = (Tab) map.get(Long.valueOf(j5))) != null) {
                    tab.addChildTab(tab3);
                }
            }
        }
    }

    void saveState(Bundle bundle) {
        int tabCount = getTabCount();
        if (tabCount == 0) {
            return;
        }
        long[] jArr = new long[tabCount];
        int i = 0;
        Iterator<Tab> it = this.mTabs.iterator();
        while (true) {
            int i2 = i;
            if (!it.hasNext()) {
                if (bundle.isEmpty()) {
                    return;
                }
                bundle.putLongArray("positions", jArr);
                Tab currentTab = getCurrentTab();
                bundle.putLong("current", currentTab != null ? currentTab.getId() : -1L);
                return;
            }
            Tab next = it.next();
            Bundle bundleSaveState = next.saveState();
            if (bundleSaveState != null) {
                jArr[i2] = next.getId();
                String string = Long.toString(next.getId());
                if (bundle.containsKey(string)) {
                    for (Tab tab : this.mTabs) {
                        if (DEBUG) {
                            Log.e("TabControl", tab.toString());
                        }
                    }
                    throw new IllegalStateException("Error saving state, duplicate tab ids!");
                }
                bundle.putBundle(string, bundleSaveState);
            } else {
                jArr[i2] = -1;
                next.deleteThumbnail();
            }
            i = i2 + 1;
        }
    }

    void setActiveTab(Tab tab) {
        this.mController.setActiveTab(tab);
    }

    boolean setCurrentTab(Tab tab) {
        return setCurrentTab(tab, false);
    }

    public void setOnTabCountChangedListener(OnTabCountChangedListener onTabCountChangedListener) {
        this.mOnTabCountChangedListener = onTabCountChangedListener;
    }

    public void setOnThumbnailUpdatedListener(OnThumbnailUpdatedListener onThumbnailUpdatedListener) {
        this.mOnThumbnailUpdatedListener = onThumbnailUpdatedListener;
        for (Tab tab : this.mTabs) {
            WebView webView = tab.getWebView();
            if (webView != null) {
                if (onThumbnailUpdatedListener == null) {
                    tab = null;
                }
                webView.setPictureListener(tab);
            }
        }
    }

    void stopAllLoading() {
        for (Tab tab : this.mTabs) {
            WebView webView = tab.getWebView();
            if (webView != null) {
                webView.stopLoading();
            }
            WebView subWebView = tab.getSubWebView();
            if (subWebView != null) {
                subWebView.stopLoading();
            }
        }
    }
}
