package com.android.deskclock.uidata;

import android.content.SharedPreferences;
import android.text.TextUtils;
import com.android.deskclock.uidata.UiDataModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

final class TabModel {
    private final SharedPreferences mPrefs;
    private UiDataModel.Tab mSelectedTab;
    private final List<TabListener> mTabListeners = new ArrayList();
    private final List<TabScrollListener> mTabScrollListeners = new ArrayList();
    private final boolean[] mTabScrolledToTop = new boolean[UiDataModel.Tab.values().length];

    TabModel(SharedPreferences sharedPreferences) {
        this.mPrefs = sharedPreferences;
        Arrays.fill(this.mTabScrolledToTop, true);
    }

    void addTabListener(TabListener tabListener) {
        this.mTabListeners.add(tabListener);
    }

    void removeTabListener(TabListener tabListener) {
        this.mTabListeners.remove(tabListener);
    }

    int getTabCount() {
        return UiDataModel.Tab.values().length;
    }

    UiDataModel.Tab getTab(int i) {
        return UiDataModel.Tab.values()[i];
    }

    UiDataModel.Tab getTabAt(int i) {
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1) {
            i = (getTabCount() - i) - 1;
        }
        return getTab(i);
    }

    UiDataModel.Tab getSelectedTab() {
        if (this.mSelectedTab == null) {
            this.mSelectedTab = TabDAO.getSelectedTab(this.mPrefs);
        }
        return this.mSelectedTab;
    }

    void setSelectedTab(UiDataModel.Tab tab) {
        UiDataModel.Tab selectedTab = getSelectedTab();
        if (selectedTab != tab) {
            this.mSelectedTab = tab;
            TabDAO.setSelectedTab(this.mPrefs, tab);
            Iterator<TabListener> it = this.mTabListeners.iterator();
            while (it.hasNext()) {
                it.next().selectedTabChanged(selectedTab, tab);
            }
            boolean zIsTabScrolledToTop = isTabScrolledToTop(tab);
            if (isTabScrolledToTop(selectedTab) != zIsTabScrolledToTop) {
                Iterator<TabScrollListener> it2 = this.mTabScrollListeners.iterator();
                while (it2.hasNext()) {
                    it2.next().selectedTabScrollToTopChanged(tab, zIsTabScrolledToTop);
                }
            }
        }
    }

    void addTabScrollListener(TabScrollListener tabScrollListener) {
        this.mTabScrollListeners.add(tabScrollListener);
    }

    void removeTabScrollListener(TabScrollListener tabScrollListener) {
        this.mTabScrollListeners.remove(tabScrollListener);
    }

    void setTabScrolledToTop(UiDataModel.Tab tab, boolean z) {
        if (isTabScrolledToTop(tab) != z) {
            this.mTabScrolledToTop[tab.ordinal()] = z;
            if (tab == getSelectedTab()) {
                Iterator<TabScrollListener> it = this.mTabScrollListeners.iterator();
                while (it.hasNext()) {
                    it.next().selectedTabScrollToTopChanged(tab, z);
                }
            }
        }
    }

    boolean isTabScrolledToTop(UiDataModel.Tab tab) {
        return this.mTabScrolledToTop[tab.ordinal()];
    }
}
