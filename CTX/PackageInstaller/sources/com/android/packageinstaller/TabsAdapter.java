package com.android.packageinstaller;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;
import java.util.ArrayList;

public class TabsAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener, TabHost.OnTabChangeListener {
    private final Context mContext;
    private final TabHost mTabHost;
    private final ArrayList<TabInfo> mTabs = new ArrayList<>();
    private final Rect mTempRect = new Rect();
    private final ViewPager mViewPager;

    static final class TabInfo {
        private final String tag;
        private final View view;

        TabInfo(String str, View view) {
            this.tag = str;
            this.view = view;
        }
    }

    static class DummyTabFactory implements TabHost.TabContentFactory {
        private final Context mContext;

        public DummyTabFactory(Context context) {
            this.mContext = context;
        }

        @Override
        public View createTabContent(String str) {
            View view = new View(this.mContext);
            view.setMinimumWidth(0);
            view.setMinimumHeight(0);
            return view;
        }
    }

    public TabsAdapter(Activity activity, TabHost tabHost, ViewPager viewPager) {
        this.mContext = activity;
        this.mTabHost = tabHost;
        this.mViewPager = viewPager;
        this.mTabHost.setOnTabChangedListener(this);
        this.mViewPager.setAdapter(this);
        this.mViewPager.setOnPageChangeListener(this);
    }

    public void addTab(TabHost.TabSpec tabSpec, View view) {
        tabSpec.setContent(new DummyTabFactory(this.mContext));
        this.mTabs.add(new TabInfo(tabSpec.getTag(), view));
        this.mTabHost.addTab(tabSpec);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return this.mTabs.size();
    }

    @Override
    public Object instantiateItem(ViewGroup viewGroup, int i) {
        View view = this.mTabs.get(i).view;
        viewGroup.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        viewGroup.removeView((View) obj);
    }

    @Override
    public boolean isViewFromObject(View view, Object obj) {
        return view == obj;
    }

    @Override
    public void onTabChanged(String str) {
        this.mViewPager.setCurrentItem(this.mTabHost.getCurrentTab());
    }

    @Override
    public void onPageScrolled(int i, float f, int i2) {
    }

    @Override
    public void onPageSelected(int i) {
        TabWidget tabWidget = this.mTabHost.getTabWidget();
        int descendantFocusability = tabWidget.getDescendantFocusability();
        tabWidget.setDescendantFocusability(393216);
        this.mTabHost.setCurrentTab(i);
        tabWidget.setDescendantFocusability(descendantFocusability);
        View childTabViewAt = tabWidget.getChildTabViewAt(i);
        this.mTempRect.set(childTabViewAt.getLeft(), childTabViewAt.getTop(), childTabViewAt.getRight(), childTabViewAt.getBottom());
        tabWidget.requestRectangleOnScreen(this.mTempRect, false);
        View view = this.mTabs.get(i).view;
        if (view instanceof CaffeinatedScrollView) {
            ((CaffeinatedScrollView) view).awakenScrollBars();
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }
}
