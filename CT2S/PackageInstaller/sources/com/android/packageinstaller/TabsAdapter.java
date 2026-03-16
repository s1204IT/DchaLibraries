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
    private TabHost.OnTabChangeListener mOnTabChangeListener;
    private final TabHost mTabHost;
    private final ArrayList<TabInfo> mTabs = new ArrayList<>();
    private final Rect mTempRect = new Rect();
    private final ViewPager mViewPager;

    static final class TabInfo {
        private final String tag;
        private final View view;

        TabInfo(String _tag, View _view) {
            this.tag = _tag;
            this.view = _view;
        }
    }

    static class DummyTabFactory implements TabHost.TabContentFactory {
        private final Context mContext;

        public DummyTabFactory(Context context) {
            this.mContext = context;
        }

        @Override
        public View createTabContent(String tag) {
            View v = new View(this.mContext);
            v.setMinimumWidth(0);
            v.setMinimumHeight(0);
            return v;
        }
    }

    public TabsAdapter(Activity activity, TabHost tabHost, ViewPager pager) {
        this.mContext = activity;
        this.mTabHost = tabHost;
        this.mViewPager = pager;
        this.mTabHost.setOnTabChangedListener(this);
        this.mViewPager.setAdapter(this);
        this.mViewPager.setOnPageChangeListener(this);
    }

    public void addTab(TabHost.TabSpec tabSpec, View view) {
        tabSpec.setContent(new DummyTabFactory(this.mContext));
        String tag = tabSpec.getTag();
        TabInfo info = new TabInfo(tag, view);
        this.mTabs.add(info);
        this.mTabHost.addTab(tabSpec);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return this.mTabs.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = this.mTabs.get(position).view;
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public void setOnTabChangedListener(TabHost.OnTabChangeListener listener) {
        this.mOnTabChangeListener = listener;
    }

    @Override
    public void onTabChanged(String tabId) {
        int position = this.mTabHost.getCurrentTab();
        this.mViewPager.setCurrentItem(position);
        if (this.mOnTabChangeListener != null) {
            this.mOnTabChangeListener.onTabChanged(tabId);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        TabWidget widget = this.mTabHost.getTabWidget();
        int oldFocusability = widget.getDescendantFocusability();
        widget.setDescendantFocusability(393216);
        this.mTabHost.setCurrentTab(position);
        widget.setDescendantFocusability(oldFocusability);
        View tab = widget.getChildTabViewAt(position);
        this.mTempRect.set(tab.getLeft(), tab.getTop(), tab.getRight(), tab.getBottom());
        widget.requestRectangleOnScreen(this.mTempRect, false);
        View contentView = this.mTabs.get(position).view;
        if (contentView instanceof CaffeinatedScrollView) {
            ((CaffeinatedScrollView) contentView).awakenScrollBars();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}
