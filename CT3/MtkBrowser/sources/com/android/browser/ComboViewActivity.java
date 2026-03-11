package com.android.browser;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.browser.UI;
import java.util.ArrayList;

public class ComboViewActivity extends Activity implements CombinedBookmarksCallbacks {

    private static final int[] f1comandroidbrowserUI$ComboViewsSwitchesValues = null;
    private TabsAdapter mTabsAdapter;
    private ViewPager mViewPager;

    private static int[] m49getcomandroidbrowserUI$ComboViewsSwitchesValues() {
        if (f1comandroidbrowserUI$ComboViewsSwitchesValues != null) {
            return f1comandroidbrowserUI$ComboViewsSwitchesValues;
        }
        int[] iArr = new int[UI.ComboViews.valuesCustom().length];
        try {
            iArr[UI.ComboViews.Bookmarks.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[UI.ComboViews.History.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[UI.ComboViews.Snapshots.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f1comandroidbrowserUI$ComboViewsSwitchesValues = iArr;
        return iArr;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UI.ComboViews startingView;
        super.onCreate(savedInstanceState);
        setResult(0);
        Bundle extras = getIntent().getExtras();
        Bundle args = extras.getBundle("combo_args");
        String svStr = extras.getString("initial_view", null);
        if (svStr != null) {
            startingView = UI.ComboViews.valueOf(svStr);
        } else {
            startingView = UI.ComboViews.Bookmarks;
        }
        this.mViewPager = new ViewPager(this);
        this.mViewPager.setId(R.id.tab_view);
        setContentView(this.mViewPager);
        ActionBar bar = getActionBar();
        bar.setNavigationMode(2);
        if (BrowserActivity.isTablet(this)) {
            bar.setDisplayOptions(3);
            bar.setHomeButtonEnabled(true);
        } else {
            bar.setDisplayOptions(0);
        }
        this.mTabsAdapter = new TabsAdapter(this, this.mViewPager);
        this.mTabsAdapter.addTab(bar.newTab().setText(R.string.tab_bookmarks), BrowserBookmarksPage.class, args);
        this.mTabsAdapter.addTab(bar.newTab().setText(R.string.tab_history), BrowserHistoryPage.class, args);
        this.mTabsAdapter.addTab(bar.newTab().setText(R.string.tab_snapshots), BrowserSnapshotPage.class, args);
        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }
        switch (m49getcomandroidbrowserUI$ComboViewsSwitchesValues()[startingView.ordinal()]) {
            case 1:
                this.mViewPager.setCurrentItem(0);
                break;
            case 2:
                this.mViewPager.setCurrentItem(1);
                break;
            case 3:
                this.mViewPager.setCurrentItem(2);
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    @Override
    public void openUrl(String url) {
        if (url == null) {
            Toast.makeText(this, R.string.bookmark_url_not_valid, 1).show();
            return;
        }
        Intent i = new Intent(this, (Class<?>) BrowserActivity.class);
        i.setAction("android.intent.action.VIEW");
        i.setData(Uri.parse(url));
        setResult(-1, i);
        finish();
    }

    @Override
    public void openInNewTab(String... urls) {
        Intent i = new Intent();
        i.putExtra("open_all", urls);
        setResult(-1, i);
        finish();
    }

    @Override
    public void openSnapshot(long id, String title, String url) {
        Intent i = new Intent(this, (Class<?>) BrowserActivity.class);
        i.setAction("android.intent.action.VIEW");
        i.setData(Uri.parse(url));
        i.putExtra("snapshot_id", id);
        i.putExtra("snapshot_title", title);
        i.putExtra("snapshot_url", url);
        setResult(0);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.combined, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 16908332) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.preferences_menu_id) {
            String url = getIntent().getStringExtra("url");
            Intent intent = new Intent(this, (Class<?>) BrowserPreferencesPage.class);
            intent.putExtra("currentPage", url);
            startActivityForResult(intent, 3);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class TabsAdapter extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final ActionBar mActionBar;
        private final Context mContext;
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

        public TabsAdapter(Activity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            this.mTabs = new ArrayList<>();
            this.mContext = activity;
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
            return Fragment.instantiate(this.mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            this.mActionBar.setSelectedNavigationItem(position);
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
}
