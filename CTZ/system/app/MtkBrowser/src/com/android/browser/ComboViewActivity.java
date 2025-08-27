package com.android.browser;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.browser.UI;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class ComboViewActivity extends Activity implements CombinedBookmarksCallbacks {
    private TabsAdapter mTabsAdapter;
    private ViewPager mViewPager;

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) throws Resources.NotFoundException {
        UI.ComboViews comboViewsValueOf;
        super.onCreate(bundle);
        setResult(0);
        Bundle extras = getIntent().getExtras();
        Bundle bundle2 = extras.getBundle("combo_args");
        String string = extras.getString("initial_view", null);
        if (string != null) {
            comboViewsValueOf = UI.ComboViews.valueOf(string);
        } else {
            comboViewsValueOf = UI.ComboViews.Bookmarks;
        }
        this.mViewPager = new ViewPager(this);
        this.mViewPager.setId(R.id.tab_view);
        setContentView(this.mViewPager);
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(2);
        if (BrowserActivity.isTablet(this)) {
            actionBar.setDisplayOptions(3);
            actionBar.setHomeButtonEnabled(true);
        } else {
            actionBar.setDisplayOptions(0);
        }
        this.mTabsAdapter = new TabsAdapter(this, this.mViewPager);
        this.mTabsAdapter.addTab(actionBar.newTab().setText(R.string.tab_bookmarks), BrowserBookmarksPage.class, bundle2);
        this.mTabsAdapter.addTab(actionBar.newTab().setText(R.string.tab_history), BrowserHistoryPage.class, bundle2);
        if (bundle == null) {
            switch (comboViewsValueOf) {
                case Bookmarks:
                    this.mViewPager.setCurrentItem(0);
                    break;
                case History:
                    this.mViewPager.setCurrentItem(1);
                    break;
                case Snapshots:
                    this.mViewPager.setCurrentItem(2);
                    break;
            }
            return;
        }
        actionBar.setSelectedNavigationItem(bundle.getInt("tab", 0));
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    @Override // com.android.browser.CombinedBookmarksCallbacks
    public void openUrl(String str) {
        if (str == null) {
            Toast.makeText(this, R.string.bookmark_url_not_valid, 1).show();
            return;
        }
        Intent intent = new Intent(this, (Class<?>) BrowserActivity.class);
        intent.setAction("android.intent.action.VIEW");
        intent.setData(Uri.parse(str));
        setResult(-1, intent);
        finish();
    }

    @Override // com.android.browser.CombinedBookmarksCallbacks
    public void openInNewTab(String... strArr) {
        Intent intent = new Intent();
        intent.putExtra("open_all", strArr);
        setResult(-1, intent);
        finish();
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.combined, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        if (menuItem.getItemId() == R.id.preferences_menu_id) {
            String stringExtra = getIntent().getStringExtra("url");
            Intent intent = new Intent(this, (Class<?>) BrowserPreferencesPage.class);
            intent.putExtra("currentPage", stringExtra);
            startActivityForResult(intent, 3);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static class TabsAdapter extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final ActionBar mActionBar;
        private final Context mContext;
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

        public TabsAdapter(Activity activity, ViewPager viewPager) throws Resources.NotFoundException {
            super(activity.getFragmentManager());
            this.mTabs = new ArrayList<>();
            this.mContext = activity;
            this.mActionBar = activity.getActionBar();
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

        @Override // android.support.v4.view.PagerAdapter
        public int getCount() {
            return this.mTabs.size();
        }

        @Override // android.support.v13.app.FragmentPagerAdapter
        public Fragment getItem(int i) {
            TabInfo tabInfo = this.mTabs.get(i);
            return Fragment.instantiate(this.mContext, tabInfo.clss.getName(), tabInfo.args);
        }

        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageScrolled(int i, float f, int i2) {
        }

        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageSelected(int i) {
            this.mActionBar.setSelectedNavigationItem(i);
        }

        @Override // android.support.v4.view.ViewPager.OnPageChangeListener
        public void onPageScrollStateChanged(int i) {
        }

        @Override // android.app.ActionBar.TabListener
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) throws Resources.NotFoundException {
            Object tag = tab.getTag();
            for (int i = 0; i < this.mTabs.size(); i++) {
                if (this.mTabs.get(i) == tag) {
                    this.mViewPager.setCurrentItem(i);
                }
            }
        }

        @Override // android.app.ActionBar.TabListener
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        }

        @Override // android.app.ActionBar.TabListener
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        }
    }
}
