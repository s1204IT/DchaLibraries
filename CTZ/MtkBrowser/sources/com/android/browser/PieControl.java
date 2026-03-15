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

    static class TabAdapter extends BaseAdapter implements PieStackView.OnCurrentListener {
        LayoutInflater mInflater;
        UiController mUiController;
        private List<Tab> mTabs = new ArrayList();
        private int mCurrent = -1;

        public TabAdapter(Context context, UiController uiController) {
            this.mInflater = LayoutInflater.from(context);
            this.mUiController = uiController;
        }

        @Override
        public int getCount() {
            return this.mTabs.size();
        }

        @Override
        public Tab getItem(int i) {
            return this.mTabs.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Tab tab = this.mTabs.get(i);
            View viewInflate = this.mInflater.inflate(2130968617, (ViewGroup) null);
            ImageView imageView = (ImageView) viewInflate.findViewById(2131558430);
            TextView textView = (TextView) viewInflate.findViewById(2131558507);
            TextView textView2 = (TextView) viewInflate.findViewById(2131558508);
            Bitmap screenshot = tab.getScreenshot();
            if (screenshot != null) {
                imageView.setImageBitmap(screenshot);
            }
            if (i > this.mCurrent) {
                textView.setVisibility(8);
                textView2.setText(tab.getTitle());
            } else {
                textView2.setVisibility(8);
                textView.setText(tab.getTitle());
            }
            viewInflate.setOnClickListener(new View.OnClickListener(this, tab) {
                final TabAdapter this$0;
                final Tab val$tab;

                {
                    this.this$0 = this;
                    this.val$tab = tab;
                }

                @Override
                public void onClick(View view2) {
                    this.this$0.mUiController.switchToTab(this.val$tab);
                }
            });
            return viewInflate;
        }

        @Override
        public void onSetCurrent(int i) {
            this.mCurrent = i;
        }

        public void setTabs(List<Tab> list) {
            this.mTabs = list;
            notifyDataSetChanged();
        }
    }

    public PieControl(Activity activity, UiController uiController, BaseUi baseUi) {
        this.mActivity = activity;
        this.mUiController = uiController;
        this.mItemSize = (int) activity.getResources().getDimension(2131427350);
        this.mUi = baseUi;
    }

    private void buildTabs() {
        List<Tab> tabs = this.mUiController.getTabs();
        this.mUi.getActiveTab().capture();
        this.mTabAdapter.setTabs(tabs);
        ((PieStackView) this.mShowTabs.getPieView()).setCurrent(this.mUiController.getTabControl().getCurrentPosition());
    }

    protected void attachToContainer(FrameLayout frameLayout) {
        if (this.mPie == null) {
            this.mPie = new PieMenu(this.mActivity);
            this.mPie.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            populateMenu();
            this.mPie.setController(this);
        }
        frameLayout.addView(this.mPie);
    }

    protected void forceToTop(FrameLayout frameLayout) {
        if (this.mPie.getParent() != null) {
            frameLayout.removeView(this.mPie);
            frameLayout.addView(this.mPie);
        }
    }

    protected PieItem makeFiller() {
        return new PieItem(null, 1);
    }

    protected PieItem makeItem(int i, int i2) {
        ImageView imageView = new ImageView(this.mActivity);
        imageView.setImageResource(i);
        imageView.setMinimumWidth(this.mItemSize);
        imageView.setMinimumHeight(this.mItemSize);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(this.mItemSize, this.mItemSize));
        return new PieItem(imageView, i2);
    }

    protected View makeTabsView() {
        View viewInflate = this.mActivity.getLayoutInflater().inflate(2130968618, (ViewGroup) null);
        this.mTabsCount = (TextView) viewInflate.findViewById(2131558424);
        this.mTabsCount.setText("1");
        ImageView imageView = (ImageView) viewInflate.findViewById(2131558509);
        imageView.setImageResource(2130837597);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        viewInflate.setLayoutParams(new ViewGroup.LayoutParams(this.mItemSize, this.mItemSize));
        return viewInflate;
    }

    @Override
    public void onClick(View view) {
        Tab currentTab = this.mUiController.getTabControl().getCurrentTab();
        if (currentTab == null) {
            return;
        }
        WebView webView = currentTab.getWebView();
        if (this.mBack.getView() == view) {
            currentTab.goBack();
            return;
        }
        if (this.mForward.getView() == view) {
            currentTab.goForward();
            return;
        }
        if (this.mRefresh.getView() == view) {
            if (currentTab.inPageLoad()) {
                webView.stopLoading();
                return;
            } else {
                webView.reload();
                return;
            }
        }
        if (this.mUrl.getView() == view) {
            this.mUi.editUrl(false, true);
            return;
        }
        if (this.mBookmarks.getView() == view) {
            this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.Bookmarks);
            return;
        }
        if (this.mHistory.getView() == view) {
            this.mUiController.bookmarksOrHistoryPicker(UI.ComboViews.History);
            return;
        }
        if (this.mAddBookmark.getView() == view) {
            this.mUiController.bookmarkCurrentPage();
            return;
        }
        if (this.mNewTab.getView() == view) {
            this.mUiController.openTab("about:blank", false, true, false);
            this.mUi.editUrl(false, true);
            return;
        }
        if (this.mIncognito.getView() == view) {
            this.mUiController.openIncognitoTab();
            this.mUi.editUrl(false, true);
            return;
        }
        if (this.mClose.getView() == view) {
            this.mUiController.closeCurrentTab();
            return;
        }
        if (this.mOptions.getView() == view) {
            this.mUiController.openPreferences();
            return;
        }
        if (this.mShare.getView() == view) {
            this.mUiController.shareCurrentPage();
            return;
        }
        if (this.mInfo.getView() == view) {
            this.mUiController.showPageInfo();
            return;
        }
        if (this.mFind.getView() == view) {
            this.mUiController.findOnPage();
        } else if (this.mRDS.getView() == view) {
            this.mUiController.toggleUserAgent();
        } else if (this.mShowTabs.getView() == view) {
            ((PhoneUi) this.mUi).showNavScreen();
        }
    }

    @Override
    public boolean onOpen() {
        this.mTabsCount.setText(Integer.toString(this.mUiController.getTabControl().getTabCount()));
        Tab currentTab = this.mUiController.getCurrentTab();
        if (currentTab != null) {
            this.mForward.setEnabled(currentTab.canGoForward());
        }
        WebView currentWebView = this.mUiController.getCurrentWebView();
        if (currentWebView == null) {
            return true;
        }
        ImageView imageView = (ImageView) this.mRDS.getView();
        if (this.mUiController.getSettings().hasDesktopUseragent(currentWebView)) {
            imageView.setImageResource(2130837570);
            return true;
        }
        imageView.setImageResource(2130837546);
        return true;
    }

    protected void populateMenu() {
        this.mBack = makeItem(2130837534, 1);
        this.mUrl = makeItem(2130837596, 1);
        this.mBookmarks = makeItem(2130837541, 1);
        this.mHistory = makeItem(2130837560, 1);
        this.mAddBookmark = makeItem(2130837538, 1);
        this.mRefresh = makeItem(2130837577, 1);
        this.mForward = makeItem(2130837555, 1);
        this.mNewTab = makeItem(2130837572, 1);
        this.mIncognito = makeItem(2130837571, 1);
        this.mClose = makeItem(2130837545, 1);
        this.mInfo = makeItem(android.R.drawable.ic_menu_info_details, 1);
        this.mFind = makeItem(2130837584, 1);
        this.mShare = makeItem(2130837588, 1);
        this.mShowTabs = new PieItem(makeTabsView(), 1);
        this.mOptions = makeItem(2130837587, 1);
        this.mRDS = makeItem(2130837546, 1);
        this.mTabAdapter = new TabAdapter(this.mActivity, this.mUiController);
        PieStackView pieStackView = new PieStackView(this.mActivity);
        pieStackView.setLayoutListener(new PieMenu.PieView.OnLayoutListener(this) {
            final PieControl this$0;

            {
                this.this$0 = this;
            }

            @Override
            public void onLayout(int i, int i2, boolean z) {
                this.this$0.buildTabs();
            }
        });
        pieStackView.setOnCurrentListener(this.mTabAdapter);
        pieStackView.setAdapter(this.mTabAdapter);
        this.mShowTabs.setPieView(pieStackView);
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

    protected void removeFromContainer(FrameLayout frameLayout) {
        frameLayout.removeView(this.mPie);
    }

    protected void setClickListener(View.OnClickListener onClickListener, PieItem... pieItemArr) {
        for (PieItem pieItem : pieItemArr) {
            pieItem.getView().setOnClickListener(onClickListener);
        }
    }

    @Override
    public void stopEditingUrl() {
        this.mUi.stopEditingUrl();
    }
}
