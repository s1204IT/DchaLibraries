package com.android.settings.applications;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.PreferenceFrameLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;
import com.android.settings.applications.AppOpsState;

public class AppOpsSummary extends Fragment {
    static AppOpsState.OpsTemplate[] sPageTemplates = {AppOpsState.LOCATION_TEMPLATE, AppOpsState.PERSONAL_TEMPLATE, AppOpsState.MESSAGING_TEMPLATE, AppOpsState.MEDIA_TEMPLATE, AppOpsState.DEVICE_TEMPLATE};
    private ViewGroup mContentContainer;
    int mCurPos;
    private LayoutInflater mInflater;
    CharSequence[] mPageNames;
    private View mRootView;
    private ViewPager mViewPager;

    class MyPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new AppOpsCategory(AppOpsSummary.sPageTemplates[position]);
        }

        @Override
        public int getCount() {
            return AppOpsSummary.sPageTemplates.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return AppOpsSummary.this.mPageNames[position];
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            AppOpsSummary.this.mCurPos = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == 0) {
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mInflater = inflater;
        View rootView = this.mInflater.inflate(R.layout.app_ops_summary, container, false);
        this.mContentContainer = container;
        this.mRootView = rootView;
        this.mPageNames = getResources().getTextArray(R.array.app_ops_categories);
        this.mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        MyPagerAdapter adapter = new MyPagerAdapter(getChildFragmentManager());
        this.mViewPager.setAdapter(adapter);
        this.mViewPager.setOnPageChangeListener(adapter);
        PagerTabStrip tabs = (PagerTabStrip) rootView.findViewById(R.id.tabs);
        tabs.setTabIndicatorColorResource(R.color.theme_accent);
        if (container instanceof PreferenceFrameLayout) {
            rootView.getLayoutParams().removeBorders = true;
        }
        return rootView;
    }
}
