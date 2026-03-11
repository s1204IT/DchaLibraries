package com.android.browser;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import com.android.browser.NavTabScroller;
import com.android.browser.TabControl;
import com.android.browser.UI;
import java.util.HashMap;

public class NavScreen extends RelativeLayout implements View.OnClickListener, PopupMenu.OnMenuItemClickListener, TabControl.OnThumbnailUpdatedListener {
    private static final boolean DEBUG = Browser.DEBUG;
    Activity mActivity;
    TabAdapter mAdapter;
    ImageButton mBookmarks;
    ImageButton mMore;
    boolean mNeedsMenu;
    ImageButton mNewTab;
    int mOrientation;
    PopupMenu mPopup;
    NavTabScroller mScroller;
    HashMap<Tab, View> mTabViews;
    PhoneUi mUi;
    UiController mUiController;

    public NavScreen(Activity activity, UiController ctl, PhoneUi ui) {
        super(activity);
        this.mActivity = activity;
        this.mUiController = ctl;
        this.mUi = ui;
        this.mOrientation = activity.getResources().getConfiguration().orientation;
        init();
    }

    public void reload() {
        int sv = this.mScroller.getScrollValue();
        removeAllViews();
        if (this.mPopup != null) {
            this.mPopup.dismiss();
        }
        this.mOrientation = this.mActivity.getResources().getConfiguration().orientation;
        init();
        this.mScroller.setScrollValue(sv);
        this.mAdapter.notifyDataSetChanged();
    }

    protected void showMenu() {
        this.mPopup = new PopupMenu(this.mContext, this.mMore);
        Menu menu = this.mPopup.getMenu();
        this.mPopup.getMenuInflater().inflate(R.menu.browser, menu);
        this.mUiController.updateMenuState(this.mUiController.getCurrentTab(), menu);
        this.mPopup.setOnMenuItemClickListener(this);
        this.mPopup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return this.mUiController.onOptionsItemSelected(item);
    }

    @Override
    protected void onConfigurationChanged(Configuration newconfig) {
        Log.d("NavScreen", "NavScreen.onConfigurationChanged() new orientation = " + newconfig.orientation + ", original orientation = " + this.mOrientation);
        if (newconfig.orientation == this.mOrientation) {
            return;
        }
        int sv = this.mScroller.getScrollValue();
        removeAllViews();
        if (this.mPopup != null) {
            this.mPopup.dismiss();
        }
        this.mOrientation = newconfig.orientation;
        init();
        this.mScroller.setScrollValue(sv);
        this.mAdapter.notifyDataSetChanged();
    }

    public void refreshAdapter() {
        this.mScroller.handleDataChanged(this.mUiController.getTabControl().getTabPosition(this.mUi.getActiveTab()));
    }

    private void init() {
        LayoutInflater.from(this.mContext).inflate(R.layout.nav_screen, this);
        setContentDescription(this.mContext.getResources().getString(R.string.accessibility_transition_navscreen));
        this.mBookmarks = (ImageButton) findViewById(R.id.bookmarks);
        this.mNewTab = (ImageButton) findViewById(R.id.newtab);
        this.mMore = (ImageButton) findViewById(R.id.more);
        this.mBookmarks.setOnClickListener(this);
        this.mNewTab.setOnClickListener(this);
        this.mMore.setOnClickListener(this);
        this.mScroller = (NavTabScroller) findViewById(R.id.scroller);
        TabControl tc = this.mUiController.getTabControl();
        this.mTabViews = new HashMap<>(tc.getTabCount());
        this.mAdapter = new TabAdapter(this.mContext, tc);
        this.mScroller.setOrientation(this.mOrientation == 2 ? 0 : 1);
        this.mScroller.setAdapter(this.mAdapter, this.mUiController.getTabControl().getTabPosition(this.mUi.getActiveTab()));
        this.mScroller.setOnRemoveListener(new NavTabScroller.OnRemoveListener() {
            @Override
            public void onRemovePosition(int pos) {
                Tab tab = NavScreen.this.mAdapter.getItem(pos);
                NavScreen.this.onCloseTab(tab);
                NavScreen.this.mNewTab.setClickable(true);
                NavScreen.this.updateBookMarkButton();
            }
        });
        this.mNeedsMenu = ViewConfiguration.get(getContext()).hasPermanentMenuKey() ? false : true;
        if (!this.mNeedsMenu) {
            this.mMore.setVisibility(8);
        }
        updateBookMarkButton();
    }

    public void updateBookMarkButton() {
        if (this.mUiController.getTabControl().getTabCount() == 0) {
            this.mBookmarks.setVisibility(8);
        } else {
            this.mBookmarks.setVisibility(0);
        }
    }

    @Override
    public void onClick(View v) {
        if (this.mBookmarks == v) {
            this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
        } else if (this.mNewTab == v) {
            openNewTab();
        } else {
            if (this.mMore != v) {
                return;
            }
            showMenu();
        }
    }

    public void onCloseTab(Tab tab) {
        if (DEBUG) {
            Log.d("browser", "NavScreen.onCloseTab()--->tab : " + tab);
        }
        if (tab != null) {
            if (tab == this.mUiController.getCurrentTab()) {
                this.mUiController.closeCurrentTab();
            } else {
                this.mUiController.closeTab(tab);
            }
            ImageView image = (ImageView) this.mTabViews.get(tab);
            if (image != null) {
                image.setImageBitmap(null);
            }
            this.mTabViews.remove(tab);
        }
        updateBookMarkButton();
    }

    private void openNewTab() {
        final Tab tab = this.mUiController.openTab("about:blank", false, false, false);
        if (tab != null) {
            this.mUiController.setBlockEvents(true);
            int tix = this.mUi.mTabControl.getTabPosition(tab);
            this.mScroller.setOnLayoutListener(new NavTabScroller.OnLayoutListener() {
                @Override
                public void onLayout(int l, int t, int r, int b) {
                    int pos = NavScreen.this.mUi.mTabControl.getTabPosition(tab);
                    NavScreen.this.mUi.hideNavScreen(pos, true);
                    NavScreen.this.switchToTab(tab);
                }
            });
            this.mScroller.handleDataChanged(tix);
            this.mUiController.setBlockEvents(false);
        }
        updateBookMarkButton();
        if (!DEBUG) {
            return;
        }
        Log.d("browser", "NavScreen.openNewTab()--->new tab is " + tab);
    }

    public void switchToTab(Tab tab) {
        if (tab == this.mUi.getActiveTab()) {
            return;
        }
        this.mUiController.setActiveTab(tab);
    }

    protected void close(int position) {
        close(position, true);
    }

    protected void close(int position, boolean animate) {
        this.mUi.hideNavScreen(position, animate);
    }

    protected NavTabView getTabView(int pos) {
        return this.mScroller.getTabView(pos);
    }

    class TabAdapter extends BaseAdapter {
        Context context;
        TabControl tabControl;

        public TabAdapter(Context ctx, TabControl tc) {
            this.context = ctx;
            this.tabControl = tc;
        }

        @Override
        public int getCount() {
            return this.tabControl.getTabCount();
        }

        @Override
        public Tab getItem(int position) {
            return this.tabControl.getTab(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final NavTabView tabview = new NavTabView(NavScreen.this.mActivity);
            final Tab tab = getItem(position);
            tabview.setWebView(tab);
            NavScreen.this.mTabViews.put(tab, tabview.mImage);
            tabview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (tabview.isClose(v)) {
                        NavScreen.this.mNewTab.setClickable(false);
                        NavScreen.this.mScroller.animateOut(tabview);
                        NavScreen.this.mTabViews.remove(tab);
                    } else {
                        if (tabview.isTitle(v)) {
                            NavScreen.this.switchToTab(tab);
                            NavScreen.this.mUi.getTitleBar().setSkipTitleBarAnimations(true);
                            NavScreen.this.close(position, false);
                            NavScreen.this.mUi.editUrl(false, true);
                            NavScreen.this.mUi.getTitleBar().setSkipTitleBarAnimations(false);
                            return;
                        }
                        if (!tabview.isWebView(v)) {
                            return;
                        }
                        NavScreen.this.close(position);
                    }
                }
            });
            return tabview;
        }
    }

    @Override
    public void onThumbnailUpdated(Tab t) {
        View v = this.mTabViews.get(t);
        if (v == null) {
            return;
        }
        v.invalidate();
    }
}
