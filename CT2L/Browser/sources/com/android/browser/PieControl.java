package com.android.browser;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.browser.UI;
import com.android.browser.view.PieItem;
import com.android.browser.view.PieMenu;
import com.android.browser.view.PieStackView;
import java.util.ArrayList;
import java.util.List;

public class PieControl implements View.OnClickListener, PieMenu.PieController {
    protected Activity mActivity;
    private PieItem mAddBookmark;
    private PieItem mBack;
    private PieItem mBookmarks;
    private PieItem mClose;
    private PieItem mFind;
    private PieItem mForward;
    private PieItem mHistory;
    private PieItem mIncognito;
    private PieItem mInfo;
    protected int mItemSize;
    private PieItem mNewTab;
    private PieItem mOptions;
    protected PieMenu mPie;
    private PieItem mRDS;
    private PieItem mRefresh;
    private PieItem mShare;
    private PieItem mShowTabs;
    private TabAdapter mTabAdapter;
    protected TextView mTabsCount;
    private BaseUi mUi;
    protected UiController mUiController;
    private PieItem mUrl;

    public PieControl(Activity activity, UiController controller, BaseUi ui) {
        this.mActivity = activity;
        this.mUiController = controller;
        this.mItemSize = (int) activity.getResources().getDimension(R.dimen.qc_item_size);
        this.mUi = ui;
    }

    @Override
    public void stopEditingUrl() {
        this.mUi.stopEditingUrl();
    }

    protected void attachToContainer(FrameLayout container) {
        if (this.mPie == null) {
            this.mPie = new PieMenu(this.mActivity);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(-1, -1);
            this.mPie.setLayoutParams(lp);
            populateMenu();
            this.mPie.setController(this);
        }
        container.addView(this.mPie);
    }

    protected void removeFromContainer(FrameLayout container) {
        container.removeView(this.mPie);
    }

    protected void forceToTop(FrameLayout container) {
        if (this.mPie.getParent() != null) {
            container.removeView(this.mPie);
            container.addView(this.mPie);
        }
    }

    protected void setClickListener(View.OnClickListener listener, PieItem... items) {
        for (PieItem item : items) {
            item.getView().setOnClickListener(listener);
        }
    }

    @Override
    public boolean onOpen() {
        int n = this.mUiController.getTabControl().getTabCount();
        this.mTabsCount.setText(Integer.toString(n));
        Tab tab = this.mUiController.getCurrentTab();
        if (tab != null) {
            this.mForward.setEnabled(tab.canGoForward());
        }
        WebView view = this.mUiController.getCurrentWebView();
        if (view != null) {
            ImageView icon = (ImageView) this.mRDS.getView();
            if (this.mUiController.getSettings().hasDesktopUseragent(view)) {
                icon.setImageResource(R.drawable.ic_mobile);
                return true;
            }
            icon.setImageResource(R.drawable.ic_desktop_holo_dark);
            return true;
        }
        return true;
    }

    protected void populateMenu() {
        this.mBack = makeItem(R.drawable.ic_back_holo_dark, 1);
        this.mUrl = makeItem(R.drawable.ic_web_holo_dark, 1);
        this.mBookmarks = makeItem(R.drawable.ic_bookmarks_holo_dark, 1);
        this.mHistory = makeItem(R.drawable.ic_history_holo_dark, 1);
        this.mAddBookmark = makeItem(R.drawable.ic_bookmark_on_holo_dark, 1);
        this.mRefresh = makeItem(R.drawable.ic_refresh_holo_dark, 1);
        this.mForward = makeItem(R.drawable.ic_forward_holo_dark, 1);
        this.mNewTab = makeItem(R.drawable.ic_new_window_holo_dark, 1);
        this.mIncognito = makeItem(R.drawable.ic_new_incognito_holo_dark, 1);
        this.mClose = makeItem(R.drawable.ic_close_window_holo_dark, 1);
        this.mInfo = makeItem(android.R.drawable.ic_menu_info_details, 1);
        this.mFind = makeItem(R.drawable.ic_search_holo_dark, 1);
        this.mShare = makeItem(R.drawable.ic_share_holo_dark, 1);
        View tabs = makeTabsView();
        this.mShowTabs = new PieItem(tabs, 1);
        this.mOptions = makeItem(R.drawable.ic_settings_holo_dark, 1);
        this.mRDS = makeItem(R.drawable.ic_desktop_holo_dark, 1);
        this.mTabAdapter = new TabAdapter(this.mActivity, this.mUiController);
        PieStackView stack = new PieStackView(this.mActivity);
        stack.setLayoutListener(new PieMenu.PieView.OnLayoutListener() {
            @Override
            public void onLayout(int ax, int ay, boolean left) {
                PieControl.this.buildTabs();
            }
        });
        stack.setOnCurrentListener(this.mTabAdapter);
        stack.setAdapter(this.mTabAdapter);
        this.mShowTabs.setPieView(stack);
        setClickListener(this, this.mBack, this.mRefresh, this.mForward, this.mUrl, this.mFind, this.mInfo, this.mShare, this.mBookmarks, this.mNewTab, this.mIncognito, this.mClose, this.mHistory, this.mAddBookmark, this.mOptions, this.mRDS);
        if (!BrowserActivity.isTablet(this.mActivity)) {
            this.mShowTabs.getView().setOnClickListener(this);
        }
        this.mPie.addItem(this.mOptions);
        this.mOptions.addItem(this.mRDS);
        this.mOptions.addItem(makeFiller());
        this.mOptions.addItem(makeFiller());
        this.mOptions.addItem(makeFiller());
        this.mPie.addItem(this.mBack);
        this.mBack.addItem(this.mRefresh);
        this.mBack.addItem(this.mForward);
        this.mBack.addItem(makeFiller());
        this.mBack.addItem(makeFiller());
        this.mPie.addItem(this.mUrl);
        this.mUrl.addItem(this.mFind);
        this.mUrl.addItem(this.mShare);
        this.mUrl.addItem(makeFiller());
        this.mUrl.addItem(makeFiller());
        this.mPie.addItem(this.mShowTabs);
        if (Build.VERSION.SDK_INT >= 19) {
            this.mShowTabs.addItem(makeFiller());
            this.mShowTabs.addItem(this.mClose);
        } else {
            this.mShowTabs.addItem(this.mClose);
            this.mShowTabs.addItem(this.mIncognito);
        }
        this.mShowTabs.addItem(this.mNewTab);
        this.mShowTabs.addItem(makeFiller());
        this.mPie.addItem(this.mBookmarks);
        this.mBookmarks.addItem(makeFiller());
        this.mBookmarks.addItem(makeFiller());
        this.mBookmarks.addItem(this.mAddBookmark);
        this.mBookmarks.addItem(this.mHistory);
    }

    @Override
    public void onClick(View v) {
        Tab tab = this.mUiController.getTabControl().getCurrentTab();
        if (tab != null) {
            WebView web = tab.getWebView();
            if (this.mBack.getView() == v) {
                tab.goBack();
                return;
            }
            if (this.mForward.getView() == v) {
                tab.goForward();
                return;
            }
            if (this.mRefresh.getView() == v) {
                if (tab.inPageLoad()) {
                    web.stopLoading();
                    return;
                } else {
                    web.reload();
                    return;
                }
            }
            if (this.mUrl.getView() == v) {
                this.mUi.editUrl(false, true);
                return;
            }
            if (this.mBookmarks.getView() == v) {
                this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
                return;
            }
            if (this.mHistory.getView() == v) {
                this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.History);
                return;
            }
            if (this.mAddBookmark.getView() == v) {
                this.mUiController.bookmarkCurrentPage();
                return;
            }
            if (this.mNewTab.getView() == v) {
                this.mUiController.openTabToHomePage();
                this.mUi.editUrl(false, true);
                return;
            }
            if (this.mIncognito.getView() == v) {
                this.mUiController.openIncognitoTab();
                this.mUi.editUrl(false, true);
                return;
            }
            if (this.mClose.getView() == v) {
                this.mUiController.closeCurrentTab();
                return;
            }
            if (this.mOptions.getView() == v) {
                this.mUiController.openPreferences();
                return;
            }
            if (this.mShare.getView() == v) {
                this.mUiController.shareCurrentPage();
                return;
            }
            if (this.mInfo.getView() == v) {
                this.mUiController.showPageInfo();
                return;
            }
            if (this.mFind.getView() == v) {
                this.mUiController.findOnPage();
            } else if (this.mRDS.getView() == v) {
                this.mUiController.toggleUserAgent();
            } else if (this.mShowTabs.getView() == v) {
                ((PhoneUi) this.mUi).showNavScreen();
            }
        }
    }

    public void buildTabs() {
        List<Tab> tabs = this.mUiController.getTabs();
        this.mUi.getActiveTab().capture();
        this.mTabAdapter.setTabs(tabs);
        PieStackView sym = (PieStackView) this.mShowTabs.getPieView();
        sym.setCurrent(this.mUiController.getTabControl().getCurrentPosition());
    }

    protected PieItem makeItem(int image, int l) {
        ImageView view = new ImageView(this.mActivity);
        view.setImageResource(image);
        view.setMinimumWidth(this.mItemSize);
        view.setMinimumHeight(this.mItemSize);
        view.setScaleType(ImageView.ScaleType.CENTER);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(this.mItemSize, this.mItemSize);
        view.setLayoutParams(lp);
        return new PieItem(view, l);
    }

    protected PieItem makeFiller() {
        return new PieItem(null, 1);
    }

    protected View makeTabsView() {
        View v = this.mActivity.getLayoutInflater().inflate(R.layout.qc_tabs_view, (ViewGroup) null);
        this.mTabsCount = (TextView) v.findViewById(R.id.label);
        this.mTabsCount.setText("1");
        ImageView image = (ImageView) v.findViewById(R.id.icon);
        image.setImageResource(R.drawable.ic_windows_holo_dark);
        image.setScaleType(ImageView.ScaleType.CENTER);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(this.mItemSize, this.mItemSize);
        v.setLayoutParams(lp);
        return v;
    }

    static class TabAdapter extends BaseAdapter implements PieStackView.OnCurrentListener {
        LayoutInflater mInflater;
        UiController mUiController;
        private List<Tab> mTabs = new ArrayList();
        private int mCurrent = -1;

        public TabAdapter(Context ctx, UiController ctl) {
            this.mInflater = LayoutInflater.from(ctx);
            this.mUiController = ctl;
        }

        public void setTabs(List<Tab> tabs) {
            this.mTabs = tabs;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return this.mTabs.size();
        }

        @Override
        public Tab getItem(int position) {
            return this.mTabs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Tab tab = this.mTabs.get(position);
            View view = this.mInflater.inflate(R.layout.qc_tab, (ViewGroup) null);
            ImageView thumb = (ImageView) view.findViewById(R.id.thumb);
            TextView title1 = (TextView) view.findViewById(R.id.title1);
            TextView title2 = (TextView) view.findViewById(R.id.title2);
            Bitmap b = tab.getScreenshot();
            if (b != null) {
                thumb.setImageBitmap(b);
            }
            if (position > this.mCurrent) {
                title1.setVisibility(8);
                title2.setText(tab.getTitle());
            } else {
                title2.setVisibility(8);
                title1.setText(tab.getTitle());
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TabAdapter.this.mUiController.switchToTab(tab);
                }
            });
            return view;
        }

        @Override
        public void onSetCurrent(int index) {
            this.mCurrent = index;
        }
    }
}
