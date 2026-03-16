package com.android.launcher2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import com.android.launcher.R;
import com.android.launcher2.AppsCustomizePagedView;
import java.util.ArrayList;

public class AppsCustomizeTabHost extends TabHost implements TabHost.OnTabChangeListener, LauncherTransitionable {
    private FrameLayout mAnimationBuffer;
    private AppsCustomizePagedView mAppsCustomizePane;
    private LinearLayout mContent;
    private boolean mInTransition;
    private final LayoutInflater mLayoutInflater;
    private Runnable mRelayoutAndMakeVisible;
    private boolean mResetAfterTransition;
    private ViewGroup mTabs;
    private ViewGroup mTabsContainer;
    private boolean mTransitioningToWorkspace;

    public AppsCustomizeTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mRelayoutAndMakeVisible = new Runnable() {
            @Override
            public void run() {
                AppsCustomizeTabHost.this.mTabs.requestLayout();
                AppsCustomizeTabHost.this.mTabsContainer.setAlpha(1.0f);
            }
        };
    }

    void setContentTypeImmediate(AppsCustomizePagedView.ContentType type) {
        setOnTabChangedListener(null);
        onTabChangedStart();
        onTabChangedEnd(type);
        setCurrentTabByTag(getTabTagForContentType(type));
        setOnTabChangedListener(this);
    }

    @Override
    protected void onFinishInflate() {
        setup();
        ViewGroup tabsContainer = (ViewGroup) findViewById(R.id.tabs_container);
        TabWidget tabs = getTabWidget();
        final AppsCustomizePagedView appsCustomizePane = (AppsCustomizePagedView) findViewById(R.id.apps_customize_pane_content);
        this.mTabs = tabs;
        this.mTabsContainer = tabsContainer;
        this.mAppsCustomizePane = appsCustomizePane;
        this.mAnimationBuffer = (FrameLayout) findViewById(R.id.animation_buffer);
        this.mContent = (LinearLayout) findViewById(R.id.apps_customize_content);
        if (tabs == null || this.mAppsCustomizePane == null) {
            throw new Resources.NotFoundException();
        }
        TabHost.TabContentFactory contentFactory = new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                return appsCustomizePane;
            }
        };
        String label = getContext().getString(R.string.all_apps_button_label);
        TextView tabView = (TextView) this.mLayoutInflater.inflate(R.layout.tab_widget_indicator, (ViewGroup) tabs, false);
        tabView.setText(label);
        tabView.setContentDescription(label);
        addTab(newTabSpec("APPS").setIndicator(tabView).setContent(contentFactory));
        String label2 = getContext().getString(R.string.widgets_tab_label);
        TextView tabView2 = (TextView) this.mLayoutInflater.inflate(R.layout.tab_widget_indicator, (ViewGroup) tabs, false);
        tabView2.setText(label2);
        tabView2.setContentDescription(label2);
        addTab(newTabSpec("WIDGETS").setIndicator(tabView2).setContent(contentFactory));
        setOnTabChangedListener(this);
        AppsCustomizeTabKeyEventListener keyListener = new AppsCustomizeTabKeyEventListener();
        View lastTab = tabs.getChildTabViewAt(tabs.getTabCount() - 1);
        lastTab.setOnKeyListener(keyListener);
        View shopButton = findViewById(R.id.market_button);
        shopButton.setOnKeyListener(keyListener);
        this.mTabsContainer.setAlpha(0.0f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean remeasureTabWidth = this.mTabs.getLayoutParams().width <= 0;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (remeasureTabWidth) {
            int contentWidth = this.mAppsCustomizePane.getPageContentWidth();
            if (contentWidth > 0 && this.mTabs.getLayoutParams().width != contentWidth) {
                this.mTabs.getLayoutParams().width = contentWidth;
                this.mRelayoutAndMakeVisible.run();
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mInTransition && this.mTransitioningToWorkspace) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mInTransition && this.mTransitioningToWorkspace) {
            return super.onTouchEvent(event);
        }
        if (event.getY() < this.mAppsCustomizePane.getBottom()) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void onTabChangedStart() {
        this.mAppsCustomizePane.hideScrollingIndicator(false);
    }

    private void reloadCurrentPage() {
        if (!LauncherApplication.isScreenLarge()) {
            this.mAppsCustomizePane.flashScrollingIndicator(true);
        }
        this.mAppsCustomizePane.loadAssociatedPages(this.mAppsCustomizePane.getCurrentPage());
        this.mAppsCustomizePane.requestFocus();
    }

    private void onTabChangedEnd(AppsCustomizePagedView.ContentType type) {
        this.mAppsCustomizePane.setContentType(type);
    }

    @Override
    public void onTabChanged(String tabId) {
        final AppsCustomizePagedView.ContentType type = getContentTypeForTabTag(tabId);
        Resources res = getResources();
        final int duration = res.getInteger(R.integer.config_tabTransitionDuration);
        post(new Runnable() {
            @Override
            public void run() {
                if (AppsCustomizeTabHost.this.mAppsCustomizePane.getMeasuredWidth() <= 0 || AppsCustomizeTabHost.this.mAppsCustomizePane.getMeasuredHeight() <= 0) {
                    AppsCustomizeTabHost.this.reloadCurrentPage();
                    return;
                }
                int[] visiblePageRange = new int[2];
                AppsCustomizeTabHost.this.mAppsCustomizePane.getVisiblePages(visiblePageRange);
                if (visiblePageRange[0] == -1 && visiblePageRange[1] == -1) {
                    AppsCustomizeTabHost.this.reloadCurrentPage();
                    return;
                }
                ArrayList<View> visiblePages = new ArrayList<>();
                for (int i = visiblePageRange[0]; i <= visiblePageRange[1]; i++) {
                    visiblePages.add(AppsCustomizeTabHost.this.mAppsCustomizePane.getPageAt(i));
                }
                AppsCustomizeTabHost.this.mAnimationBuffer.scrollTo(AppsCustomizeTabHost.this.mAppsCustomizePane.getScrollX(), 0);
                for (int i2 = visiblePages.size() - 1; i2 >= 0; i2--) {
                    View child = visiblePages.get(i2);
                    if (child instanceof PagedViewCellLayout) {
                        ((PagedViewCellLayout) child).resetChildrenOnKeyListeners();
                    } else if (child instanceof PagedViewGridLayout) {
                        ((PagedViewGridLayout) child).resetChildrenOnKeyListeners();
                    }
                    PagedViewWidget.setDeletePreviewsWhenDetachedFromWindow(false);
                    AppsCustomizeTabHost.this.mAppsCustomizePane.removeView(child);
                    PagedViewWidget.setDeletePreviewsWhenDetachedFromWindow(true);
                    AppsCustomizeTabHost.this.mAnimationBuffer.setAlpha(1.0f);
                    AppsCustomizeTabHost.this.mAnimationBuffer.setVisibility(0);
                    FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(child.getMeasuredWidth(), child.getMeasuredHeight());
                    p.setMargins(child.getLeft(), child.getTop(), 0, 0);
                    AppsCustomizeTabHost.this.mAnimationBuffer.addView(child, p);
                }
                AppsCustomizeTabHost.this.onTabChangedStart();
                AppsCustomizeTabHost.this.onTabChangedEnd(type);
                ObjectAnimator outAnim = LauncherAnimUtils.ofFloat(AppsCustomizeTabHost.this.mAnimationBuffer, "alpha", 0.0f);
                outAnim.addListener(new AnimatorListenerAdapter() {
                    private void clearAnimationBuffer() {
                        AppsCustomizeTabHost.this.mAnimationBuffer.setVisibility(8);
                        PagedViewWidget.setRecyclePreviewsWhenDetachedFromWindow(false);
                        AppsCustomizeTabHost.this.mAnimationBuffer.removeAllViews();
                        PagedViewWidget.setRecyclePreviewsWhenDetachedFromWindow(true);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        clearAnimationBuffer();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        clearAnimationBuffer();
                    }
                });
                ObjectAnimator inAnim = LauncherAnimUtils.ofFloat(AppsCustomizeTabHost.this.mAppsCustomizePane, "alpha", 1.0f);
                inAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        AppsCustomizeTabHost.this.reloadCurrentPage();
                    }
                });
                AnimatorSet animSet = LauncherAnimUtils.createAnimatorSet();
                animSet.playTogether(outAnim, inAnim);
                animSet.setDuration(duration);
                animSet.start();
            }
        });
    }

    public void setCurrentTabFromContent(AppsCustomizePagedView.ContentType type) {
        setOnTabChangedListener(null);
        setCurrentTabByTag(getTabTagForContentType(type));
        setOnTabChangedListener(this);
    }

    public AppsCustomizePagedView.ContentType getContentTypeForTabTag(String tag) {
        if (tag.equals("APPS")) {
            return AppsCustomizePagedView.ContentType.Applications;
        }
        if (tag.equals("WIDGETS")) {
            return AppsCustomizePagedView.ContentType.Widgets;
        }
        return AppsCustomizePagedView.ContentType.Applications;
    }

    public String getTabTagForContentType(AppsCustomizePagedView.ContentType type) {
        if (type != AppsCustomizePagedView.ContentType.Applications && type == AppsCustomizePagedView.ContentType.Widgets) {
            return "WIDGETS";
        }
        return "APPS";
    }

    @Override
    public int getDescendantFocusability() {
        if (getVisibility() != 0) {
            return 393216;
        }
        return super.getDescendantFocusability();
    }

    void reset() {
        if (this.mInTransition) {
            this.mResetAfterTransition = true;
        } else {
            this.mAppsCustomizePane.reset();
        }
    }

    private void enableAndBuildHardwareLayer() {
        if (isHardwareAccelerated()) {
            setLayerType(2, null);
            buildLayer();
        }
    }

    @Override
    public View getContent() {
        return this.mContent;
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        this.mAppsCustomizePane.onLauncherTransitionPrepare(l, animated, toWorkspace);
        this.mInTransition = true;
        this.mTransitioningToWorkspace = toWorkspace;
        if (toWorkspace) {
            setVisibilityOfSiblingsWithLowerZOrder(0);
            this.mAppsCustomizePane.cancelScrollingIndicatorAnimations();
        } else {
            this.mContent.setVisibility(0);
            this.mAppsCustomizePane.loadAssociatedPages(this.mAppsCustomizePane.getCurrentPage(), true);
            if (!LauncherApplication.isScreenLarge()) {
                this.mAppsCustomizePane.showScrollingIndicator(true);
            }
        }
        if (this.mResetAfterTransition) {
            this.mAppsCustomizePane.reset();
            this.mResetAfterTransition = false;
        }
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
        if (animated) {
            enableAndBuildHardwareLayer();
        }
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        this.mAppsCustomizePane.onLauncherTransitionEnd(l, animated, toWorkspace);
        this.mInTransition = false;
        if (animated) {
            setLayerType(0, null);
        }
        if (!toWorkspace) {
            l.dismissWorkspaceCling(null);
            this.mAppsCustomizePane.showAllAppsCling();
            this.mAppsCustomizePane.loadAssociatedPages(this.mAppsCustomizePane.getCurrentPage());
            if (!LauncherApplication.isScreenLarge()) {
                this.mAppsCustomizePane.hideScrollingIndicator(false);
            }
            setVisibilityOfSiblingsWithLowerZOrder(4);
        }
    }

    private void setVisibilityOfSiblingsWithLowerZOrder(int visibility) {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            int count = parent.getChildCount();
            if (!isChildrenDrawingOrderEnabled()) {
                for (int i = 0; i < count; i++) {
                    View child = parent.getChildAt(i);
                    if (child != this) {
                        if (child.getVisibility() != 8) {
                            child.setVisibility(visibility);
                        }
                    } else {
                        return;
                    }
                }
                return;
            }
            throw new RuntimeException("Failed; can't get z-order of views");
        }
    }

    public void onWindowVisible() {
        if (getVisibility() == 0) {
            this.mContent.setVisibility(0);
            this.mAppsCustomizePane.loadAssociatedPages(this.mAppsCustomizePane.getCurrentPage(), true);
            this.mAppsCustomizePane.loadAssociatedPages(this.mAppsCustomizePane.getCurrentPage());
        }
    }

    public void onTrimMemory() {
        this.mContent.setVisibility(8);
        this.mAppsCustomizePane.clearAllWidgetPages();
    }

    boolean isTransitioning() {
        return this.mInTransition;
    }
}
