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
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mMultiChoiceManager = new MultiChoiceManager(this);
        this.mViewPager = new ViewPager(this);
        this.mViewPager.setId(R.id.viewpager);
        setContentView(this.mViewPager);
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(2);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        this.mTabsAdapter = new TabsAdapter(this, this.mViewPager);
        this.mTabsAdapter.addTab(actionBar.newTab().setText(R.string.tab_photos), PhotoSetFragment.class, null);
        this.mTabsAdapter.addTab(actionBar.newTab().setText(R.string.tab_albums), AlbumSetFragment.class, null);
        if (bundle != null) {
            actionBar.setSelectedNavigationItem(bundle.getInt("tab", 0));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_camera) {
            throw new RuntimeException("Not implemented yet.");
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static class TabsAdapter extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final ActionBar mActionBar;
        private final GalleryActivity mActivity;
        private final ArrayList<TabInfo> mTabs;
        private final ViewPager mViewPager;

        static final class TabInfo {
            private final Bundle args;
            private final Class<?> clss;

            TabInfo(Class<?> cls, Bundle bundle) {
                this.clss = cls;
                this.args = bundle;
            }
        }

        public TabsAdapter(GalleryActivity galleryActivity, ViewPager viewPager) {
            super(galleryActivity.getFragmentManager());
            this.mTabs = new ArrayList<>();
            this.mActivity = galleryActivity;
            this.mActionBar = galleryActivity.getActionBar();
            this.mViewPager = viewPager;
            this.mViewPager.setAdapter(this);
            this.mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> cls, Bundle bundle) {
            TabInfo tabInfo = new TabInfo(cls, bundle);
            tab.setTag(tabInfo);
            tab.setTabListener(this);
            this.mTabs.add(tabInfo);
            this.mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return this.mTabs.size();
        }

        @Override
        public Fragment getItem(int i) {
            TabInfo tabInfo = this.mTabs.get(i);
            return Fragment.instantiate(this.mActivity, tabInfo.clss.getName(), tabInfo.args);
        }

        @Override
        public void onPageScrolled(int i, float f, int i2) {
        }

        @Override
        public void onPageSelected(int i) {
            this.mActionBar.setSelectedNavigationItem(i);
        }

        @Override
        public void setPrimaryItem(ViewGroup viewGroup, int i, Object obj) {
            super.setPrimaryItem(viewGroup, i, obj);
            this.mActivity.mMultiChoiceManager.setDelegate((MultiChoiceManager.Delegate) obj);
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            Object tag = tab.getTag();
            for (int i = 0; i < this.mTabs.size(); i++) {
                if (this.mTabs.get(i) == tag) {
                    this.mViewPager.setCurrentItem(i);
                }
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        }
    }

    @Override
    public MultiChoiceManager getMultiChoiceManager() {
        return this.mMultiChoiceManager;
    }
}
