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

    class TabAdapter extends BaseAdapter {
        Context context;
        TabControl tabControl;
        final NavScreen this$0;

        public TabAdapter(NavScreen navScreen, Context context, TabControl tabControl) {
            this.this$0 = navScreen;
            this.context = context;
            this.tabControl = tabControl;
        }

        @Override
        public int getCount() {
            return this.tabControl.getTabCount();
        }

        @Override
        public Tab getItem(int i) {
            return this.tabControl.getTab(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            NavTabView navTabView = new NavTabView(this.this$0.mActivity);
            Tab item = getItem(i);
            navTabView.setWebView(item);
            this.this$0.mTabViews.put(item, navTabView.mImage);
            navTabView.setOnClickListener(new View.OnClickListener(this, navTabView, item, i) {
                final TabAdapter this$1;
                final int val$position;
                final Tab val$tab;
                final NavTabView val$tabview;

                {
                    this.this$1 = this;
                    this.val$tabview = navTabView;
                    this.val$tab = item;
                    this.val$position = i;
                }

                @Override
                public void onClick(View view2) {
                    if (this.val$tabview.isClose(view2)) {
                        this.this$1.this$0.mNewTab.setClickable(false);
                        this.this$1.this$0.mScroller.animateOut(this.val$tabview);
                        this.this$1.this$0.mTabViews.remove(this.val$tab);
                    } else if (!this.val$tabview.isTitle(view2)) {
                        if (this.val$tabview.isWebView(view2)) {
                            this.this$1.this$0.close(this.val$position);
                        }
                    } else {
                        this.this$1.this$0.switchToTab(this.val$tab);
                        this.this$1.this$0.mUi.getTitleBar().setSkipTitleBarAnimations(true);
                        this.this$1.this$0.close(this.val$position, false);
                        this.this$1.this$0.mUi.editUrl(false, true);
                        this.this$1.this$0.mUi.getTitleBar().setSkipTitleBarAnimations(false);
                    }
                }
            });
            return navTabView;
        }
    }

    public NavScreen(Activity activity, UiController uiController, PhoneUi phoneUi) {
        super(activity);
        this.mActivity = activity;
        this.mUiController = uiController;
        this.mUi = phoneUi;
        this.mOrientation = activity.getResources().getConfiguration().orientation;
        init();
    }

    private void init() {
        LayoutInflater.from(this.mContext).inflate(2130968609, this);
        setContentDescription(this.mContext.getResources().getString(2131493309));
        this.mBookmarks = (ImageButton) findViewById(2131558445);
        this.mNewTab = (ImageButton) findViewById(2131558494);
        this.mMore = (ImageButton) findViewById(2131558495);
        this.mBookmarks.setOnClickListener(this);
        this.mNewTab.setOnClickListener(this);
        this.mMore.setOnClickListener(this);
        this.mScroller = (NavTabScroller) findViewById(2131558492);
        TabControl tabControl = this.mUiController.getTabControl();
        this.mTabViews = new HashMap<>(tabControl.getTabCount());
        this.mAdapter = new TabAdapter(this, this.mContext, tabControl);
        this.mScroller.setOrientation(this.mOrientation == 2 ? 0 : 1);
        this.mScroller.setAdapter(this.mAdapter, this.mUiController.getTabControl().getTabPosition(this.mUi.getActiveTab()));
        this.mScroller.setOnRemoveListener(new NavTabScroller.OnRemoveListener(this) {
            final NavScreen this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onRemovePosition(int i) {
                this.this$0.onCloseTab(this.this$0.mAdapter.getItem(i));
                this.this$0.mNewTab.setClickable(true);
                this.this$0.updateBookMarkButton();
            }
        });
        this.mNeedsMenu = !ViewConfiguration.get(getContext()).hasPermanentMenuKey();
        if (!this.mNeedsMenu) {
            this.mMore.setVisibility(8);
        }
        updateBookMarkButton();
    }

    private void onCloseTab(Tab tab) {
        if (DEBUG) {
            Log.d("browser", "NavScreen.onCloseTab()--->tab : " + tab);
        }
        if (tab != null) {
            if (tab == this.mUiController.getCurrentTab()) {
                this.mUiController.closeCurrentTab();
            } else {
                this.mUiController.closeTab(tab);
            }
            ImageView imageView = (ImageView) this.mTabViews.get(tab);
            if (imageView != null) {
                imageView.setImageBitmap(null);
            }
            this.mTabViews.remove(tab);
        }
        updateBookMarkButton();
    }

    private void openNewTab() {
        Tab tabOpenTab = this.mUiController.openTab("about:blank", false, false, false);
        if (tabOpenTab != null) {
            this.mUiController.setBlockEvents(true);
            int tabPosition = this.mUi.mTabControl.getTabPosition(tabOpenTab);
            this.mScroller.setOnLayoutListener(new NavTabScroller.OnLayoutListener(this, tabOpenTab) {
                final NavScreen this$0;
                final Tab val$tab;

                {
                    this.this$0 = this;
                    this.val$tab = tabOpenTab;
                }

                @Override
                public void onLayout(int i, int i2, int i3, int i4) {
                    this.this$0.mUi.hideNavScreen(this.this$0.mUi.mTabControl.getTabPosition(this.val$tab), true);
                    this.this$0.switchToTab(this.val$tab);
                }
            });
            this.mScroller.handleDataChanged(tabPosition);
            this.mUiController.setBlockEvents(false);
        }
        updateBookMarkButton();
        if (DEBUG) {
            Log.d("browser", "NavScreen.openNewTab()--->new tab is " + tabOpenTab);
        }
    }

    private void switchToTab(Tab tab) {
        if (tab != this.mUi.getActiveTab()) {
            this.mUiController.setActiveTab(tab);
        }
    }

    private void updateBookMarkButton() {
        if (this.mUiController.getTabControl().getTabCount() == 0) {
            this.mBookmarks.setVisibility(8);
            this.mNewTab.setVisibility(8);
        } else {
            this.mBookmarks.setVisibility(0);
            this.mNewTab.setVisibility(0);
        }
    }

    protected void close(int i) {
        close(i, true);
    }

    protected void close(int i, boolean z) {
        this.mUi.hideNavScreen(i, z);
    }

    protected NavTabView getTabView(int i) {
        return this.mScroller.getTabView(i);
    }

    @Override
    public void onClick(View view) {
        if (this.mBookmarks == view) {
            this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
        } else if (this.mNewTab == view) {
            openNewTab();
        } else if (this.mMore == view) {
            showMenu();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        Log.d("NavScreen", "NavScreen.onConfigurationChanged() new orientation = " + configuration.orientation + ", original orientation = " + this.mOrientation);
        if (configuration.orientation != this.mOrientation) {
            int scrollValue = this.mScroller.getScrollValue();
            removeAllViews();
            if (this.mPopup != null) {
                this.mPopup.dismiss();
            }
            this.mOrientation = configuration.orientation;
            init();
            this.mScroller.setScrollValue(scrollValue);
            this.mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return this.mUiController.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onThumbnailUpdated(Tab tab) {
        View view = this.mTabViews.get(tab);
        if (view != null) {
            view.invalidate();
        }
    }

    public void refreshAdapter() {
        this.mScroller.handleDataChanged(this.mUiController.getTabControl().getTabPosition(this.mUi.getActiveTab()));
    }

    public void reload() {
        int scrollValue = this.mScroller.getScrollValue();
        removeAllViews();
        if (this.mPopup != null) {
            this.mPopup.dismiss();
        }
        this.mOrientation = this.mActivity.getResources().getConfiguration().orientation;
        init();
        this.mScroller.setScrollValue(scrollValue);
        this.mAdapter.notifyDataSetChanged();
    }

    protected void showMenu() {
        this.mPopup = new PopupMenu(this.mContext, this.mMore);
        Menu menu = this.mPopup.getMenu();
        this.mPopup.getMenuInflater().inflate(2131755010, menu);
        this.mUiController.updateMenuState(this.mUiController.getCurrentTab(), menu);
        this.mPopup.setOnMenuItemClickListener(this);
        this.mPopup.show();
    }
}
