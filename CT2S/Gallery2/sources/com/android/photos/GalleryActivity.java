package com.android.photos;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import com.android.gallery3d.R;
import com.android.photos.MultiChoiceManager;
import java.util.ArrayList;

public class GalleryActivity extends Activity implements MultiChoiceManager.Provider {
    private MultiChoiceManager mMultiChoiceManager;
    private TabsAdapter mTabsAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mMultiChoiceManager = new MultiChoiceManager(this);
        this.mViewPager = new ViewPager(this);
        this.mViewPager.setId(R.id.viewpager);
        setContentView(this.mViewPager);
        ActionBar ab = getActionBar();
        ab.setNavigationMode(2);
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        this.mTabsAdapter = new TabsAdapter(this, this.mViewPager);
        this.mTabsAdapter.addTab(ab.newTab().setText(R.string.tab_photos), PhotoSetFragment.class, null);
        this.mTabsAdapter.addTab(ab.newTab().setText(R.string.tab_albums), AlbumSetFragment.class, null);
        if (savedInstanceState != null) {
            ab.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_camera:
                throw new RuntimeException("Not implemented yet.");
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class TabsAdapter extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final ActionBar mActionBar;
        private final GalleryActivity mActivity;
        private final ArrayList<TabInfo> mTabs;
        private final ViewPager mViewPager;

        static final class TabInfo {
            private final Bundle args;
            private final Class<?> clss;

            TabInfo(Class<?> _class, Bundle _args) {
                this.clss = _class;
                this.args = _args;
            }
        }

        public TabsAdapter(GalleryActivity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            this.mTabs = new ArrayList<>();
            this.mActivity = activity;
            this.mActionBar = activity.getActionBar();
            this.mViewPager = pager;
            this.mViewPager.setAdapter(this);
            this.mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            tab.setTag(info);
            tab.setTabListener(this);
            this.mTabs.add(info);
            this.mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return this.mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = this.mTabs.get(position);
            return Fragment.instantiate(this.mActivity, info.clss.getName(), info.args);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            this.mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            this.mActivity.mMultiChoiceManager.setDelegate((MultiChoiceManager.Delegate) object);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            Object tag = tab.getTag();
            for (int i = 0; i < this.mTabs.size(); i++) {
                if (this.mTabs.get(i) == tag) {
                    this.mViewPager.setCurrentItem(i);
                }
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }
    }

    @Override
    public MultiChoiceManager getMultiChoiceManager() {
        return this.mMultiChoiceManager;
    }
}
