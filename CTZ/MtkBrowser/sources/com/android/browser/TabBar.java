package com.android.browser;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TabBar extends LinearLayout implements View.OnClickListener {
    private Drawable mActiveDrawable;
    private final Matrix mActiveMatrix;
    private BitmapShader mActiveShader;
    private final Paint mActiveShaderPaint;
    private Activity mActivity;
    private int mAddTabOverlap;
    private int mButtonWidth;
    private int mCurrentTextureHeight;
    private int mCurrentTextureWidth;
    private final Paint mFocusPaint;
    private Drawable mInactiveDrawable;
    private final Matrix mInactiveMatrix;
    private BitmapShader mInactiveShader;
    private final Paint mInactiveShaderPaint;
    private ImageButton mNewTab;
    private TabControl mTabControl;
    private Map<Tab, TabView> mTabMap;
    private int mTabOverlap;
    private int mTabSliceWidth;
    private int mTabWidth;
    private TabScrollView mTabs;
    private XLargeUi mUi;
    private UiController mUiController;
    private boolean mUseQuickControls;

    public TabBar(Activity activity, UiController uiController, XLargeUi xLargeUi) {
        super(activity);
        this.mCurrentTextureWidth = 0;
        this.mCurrentTextureHeight = 0;
        this.mActiveShaderPaint = new Paint();
        this.mInactiveShaderPaint = new Paint();
        this.mFocusPaint = new Paint();
        this.mActiveMatrix = new Matrix();
        this.mInactiveMatrix = new Matrix();
        this.mActivity = activity;
        this.mUiController = uiController;
        this.mTabControl = this.mUiController.getTabControl();
        this.mUi = xLargeUi;
        Resources resources = activity.getResources();
        this.mTabWidth = (int) resources.getDimension(R.dimen.tab_width);
        this.mActiveDrawable = resources.getDrawable(R.drawable.bg_urlbar);
        this.mInactiveDrawable = resources.getDrawable(R.drawable.browsertab_inactive);
        this.mTabMap = new HashMap();
        LayoutInflater.from(activity).inflate(R.layout.tab_bar, this);
        setPadding(0, (int) resources.getDimension(R.dimen.tab_padding_top), 0, 0);
        this.mTabs = (TabScrollView) findViewById(R.id.tabs);
        this.mNewTab = (ImageButton) findViewById(R.id.newtab);
        this.mNewTab.setOnClickListener(this);
        updateTabs(this.mUiController.getTabs());
        this.mButtonWidth = -1;
        this.mTabOverlap = (int) resources.getDimension(R.dimen.tab_overlap);
        this.mAddTabOverlap = (int) resources.getDimension(R.dimen.tab_addoverlap);
        this.mTabSliceWidth = (int) resources.getDimension(R.dimen.tab_slice);
        this.mActiveShaderPaint.setStyle(Paint.Style.FILL);
        this.mActiveShaderPaint.setAntiAlias(true);
        this.mInactiveShaderPaint.setStyle(Paint.Style.FILL);
        this.mInactiveShaderPaint.setAntiAlias(true);
        this.mFocusPaint.setStyle(Paint.Style.STROKE);
        this.mFocusPaint.setStrokeWidth(resources.getDimension(R.dimen.tab_focus_stroke));
        this.mFocusPaint.setAntiAlias(true);
        this.mFocusPaint.setColor(resources.getColor(R.color.tabFocusHighlight));
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mTabWidth = (int) this.mActivity.getResources().getDimension(R.dimen.tab_width);
        this.mTabs.updateLayout();
    }

    void setUseQuickControls(boolean z) {
        this.mUseQuickControls = z;
        this.mNewTab.setVisibility(this.mUseQuickControls ? 8 : 0);
    }

    void updateTabs(List<Tab> list) {
        this.mTabs.clearTabs();
        this.mTabMap.clear();
        Iterator<Tab> it = list.iterator();
        while (it.hasNext()) {
            this.mTabs.addTab(buildTabView(it.next()));
        }
        this.mTabs.setSelectedTab(this.mTabControl.getCurrentPosition());
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int measuredWidth = getMeasuredWidth();
        if (!this.mUseQuickControls) {
            measuredWidth -= this.mAddTabOverlap;
        }
        setMeasuredDimension(measuredWidth, getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int measuredWidth = this.mTabs.getMeasuredWidth();
        int i5 = (i3 - i) - paddingLeft;
        if (this.mUseQuickControls) {
            this.mButtonWidth = 0;
        } else {
            this.mButtonWidth = this.mNewTab.getMeasuredWidth() - this.mAddTabOverlap;
            if (i5 - measuredWidth < this.mButtonWidth) {
                measuredWidth = i5 - this.mButtonWidth;
            }
        }
        int i6 = measuredWidth + paddingLeft;
        int i7 = i4 - i2;
        this.mTabs.layout(paddingLeft, paddingTop, i6, i7);
        if (!this.mUseQuickControls) {
            this.mNewTab.layout(i6 - this.mAddTabOverlap, paddingTop, (i6 + this.mButtonWidth) - this.mAddTabOverlap, i7);
        }
    }

    @Override
    public void onClick(View view) {
        if (this.mNewTab == view) {
            this.mUiController.openTabToHomePage();
            return;
        }
        if (this.mTabs.getSelectedTab() == view) {
            if (this.mUseQuickControls) {
                if (this.mUi.isTitleBarShowing() && !isLoading()) {
                    this.mUi.stopEditingUrl();
                    this.mUi.hideTitleBar();
                    return;
                } else {
                    this.mUi.stopWebViewScrolling();
                    this.mUi.editUrl(false, false);
                    return;
                }
            }
            if (this.mUi.isTitleBarShowing() && !isLoading()) {
                this.mUi.stopEditingUrl();
                this.mUi.hideTitleBar();
                return;
            } else {
                showUrlBar();
                return;
            }
        }
        if (view instanceof TabView) {
            Tab tab = view.mTab;
            int childIndex = this.mTabs.getChildIndex(view);
            if (childIndex >= 0) {
                this.mTabs.setSelectedTab(childIndex);
                this.mUiController.switchToTab(tab);
            }
        }
    }

    private void showUrlBar() {
        this.mUi.stopWebViewScrolling();
        this.mUi.showTitleBar();
    }

    private TabView buildTabView(Tab tab) {
        TabView tabView = new TabView(this.mActivity, tab);
        this.mTabMap.put(tab, tabView);
        tabView.setOnClickListener(this);
        return tabView;
    }

    private static Bitmap getDrawableAsBitmap(Drawable drawable, int i, int i2) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        drawable.setBounds(0, 0, i, i2);
        drawable.draw(canvas);
        canvas.setBitmap(null);
        return bitmapCreateBitmap;
    }

    class TabView extends LinearLayout implements View.OnClickListener {
        ImageView mClose;
        Path mFocusPath;
        ImageView mIconView;
        View mIncognito;
        ImageView mLock;
        Path mPath;
        boolean mSelected;
        View mSnapshot;
        Tab mTab;
        View mTabContent;
        TextView mTitle;
        int[] mWindowPos;

        public TabView(Context context, Tab tab) {
            super(context);
            setWillNotDraw(false);
            this.mPath = new Path();
            this.mFocusPath = new Path();
            this.mWindowPos = new int[2];
            this.mTab = tab;
            setGravity(16);
            setOrientation(0);
            setPadding(TabBar.this.mTabOverlap, 0, TabBar.this.mTabSliceWidth, 0);
            this.mTabContent = LayoutInflater.from(getContext()).inflate(R.layout.tab_title, (ViewGroup) this, true);
            this.mTitle = (TextView) this.mTabContent.findViewById(R.id.title);
            this.mIconView = (ImageView) this.mTabContent.findViewById(R.id.favicon);
            this.mLock = (ImageView) this.mTabContent.findViewById(R.id.lock);
            this.mClose = (ImageView) this.mTabContent.findViewById(R.id.close);
            this.mClose.setOnClickListener(this);
            this.mIncognito = this.mTabContent.findViewById(R.id.incognito);
            this.mSnapshot = this.mTabContent.findViewById(R.id.snapshot);
            this.mSelected = false;
            updateFromTab();
        }

        @Override
        public void onClick(View view) {
            if (view == this.mClose) {
                closeTab();
            }
        }

        private void updateFromTab() {
            String title = this.mTab.getTitle();
            if (title == null) {
                title = this.mTab.getUrl();
            }
            setDisplayTitle(title);
            if (this.mTab.getFavicon() != null) {
                setFavicon(TabBar.this.mUi.getFaviconDrawable(this.mTab.getFavicon()));
            }
            updateTabIcons();
        }

        private void updateTabIcons() {
            this.mIncognito.setVisibility(this.mTab.isPrivateBrowsingEnabled() ? 0 : 8);
            this.mSnapshot.setVisibility(this.mTab.isSnapshot() ? 0 : 8);
        }

        @Override
        public void setActivated(boolean z) {
            this.mSelected = z;
            this.mClose.setVisibility(this.mSelected ? 0 : 8);
            this.mIconView.setVisibility(this.mSelected ? 8 : 0);
            this.mTitle.setTextAppearance(TabBar.this.mActivity, this.mSelected ? R.style.TabTitleSelected : R.style.TabTitleUnselected);
            setHorizontalFadingEdgeEnabled(!this.mSelected);
            super.setActivated(z);
            updateLayoutParams();
            setFocusable(!z);
            postInvalidate();
        }

        public void updateLayoutParams() {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) getLayoutParams();
            ((ViewGroup.LayoutParams) layoutParams).width = TabBar.this.mTabWidth;
            ((ViewGroup.LayoutParams) layoutParams).height = -1;
            setLayoutParams(layoutParams);
        }

        void setDisplayTitle(String str) {
            if (str.startsWith("about:blank")) {
                this.mTitle.setText("about:blank");
            } else {
                this.mTitle.setText(str);
            }
        }

        void setFavicon(Drawable drawable) {
            this.mIconView.setImageDrawable(drawable);
        }

        private void closeTab() {
            if (this.mTab == TabBar.this.mTabControl.getCurrentTab()) {
                TabBar.this.mUiController.closeCurrentTab();
            } else {
                TabBar.this.mUiController.closeTab(this.mTab);
            }
        }

        @Override
        protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
            super.onLayout(z, i, i2, i3, i4);
            int i5 = i3 - i;
            int i6 = i4 - i2;
            setTabPath(this.mPath, 0, 0, i5, i6);
            setFocusPath(this.mFocusPath, 0, 0, i5, i6);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            if (TabBar.this.mCurrentTextureWidth != TabBar.this.mUi.getContentWidth() || TabBar.this.mCurrentTextureHeight != getHeight()) {
                TabBar.this.mCurrentTextureWidth = TabBar.this.mUi.getContentWidth();
                TabBar.this.mCurrentTextureHeight = getHeight();
                if (TabBar.this.mCurrentTextureWidth > 0 && TabBar.this.mCurrentTextureHeight > 0) {
                    Bitmap drawableAsBitmap = TabBar.getDrawableAsBitmap(TabBar.this.mActiveDrawable, TabBar.this.mCurrentTextureWidth, TabBar.this.mCurrentTextureHeight);
                    Bitmap drawableAsBitmap2 = TabBar.getDrawableAsBitmap(TabBar.this.mInactiveDrawable, TabBar.this.mCurrentTextureWidth, TabBar.this.mCurrentTextureHeight);
                    TabBar.this.mActiveShader = new BitmapShader(drawableAsBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    TabBar.this.mActiveShaderPaint.setShader(TabBar.this.mActiveShader);
                    TabBar.this.mInactiveShader = new BitmapShader(drawableAsBitmap2, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    TabBar.this.mInactiveShaderPaint.setShader(TabBar.this.mInactiveShader);
                }
            }
            if (TabBar.this.mActiveShader != null && TabBar.this.mInactiveShader != null) {
                int iSave = canvas.save();
                getLocationInWindow(this.mWindowPos);
                drawClipped(canvas, this.mSelected ? TabBar.this.mActiveShaderPaint : TabBar.this.mInactiveShaderPaint, this.mPath, this.mWindowPos[0]);
                canvas.restoreToCount(iSave);
            }
            super.dispatchDraw(canvas);
        }

        private void drawClipped(Canvas canvas, Paint paint, Path path, int i) {
            Matrix matrix = this.mSelected ? TabBar.this.mActiveMatrix : TabBar.this.mInactiveMatrix;
            matrix.setTranslate(-i, 0.0f);
            BitmapShader bitmapShader = this.mSelected ? TabBar.this.mActiveShader : TabBar.this.mInactiveShader;
            bitmapShader.setLocalMatrix(matrix);
            paint.setShader(bitmapShader);
            canvas.drawPath(path, paint);
            if (isFocused()) {
                canvas.drawPath(this.mFocusPath, TabBar.this.mFocusPaint);
            }
        }

        private void setTabPath(Path path, int i, int i2, int i3, int i4) {
            path.reset();
            float f = i;
            float f2 = i4;
            path.moveTo(f, f2);
            float f3 = i2;
            path.lineTo(f, f3);
            path.lineTo(i3 - TabBar.this.mTabSliceWidth, f3);
            path.lineTo(i3, f2);
            path.close();
        }

        private void setFocusPath(Path path, int i, int i2, int i3, int i4) {
            path.reset();
            float f = i;
            float f2 = i4;
            path.moveTo(f, f2);
            float f3 = i2;
            path.lineTo(f, f3);
            path.lineTo(i3 - TabBar.this.mTabSliceWidth, f3);
            path.lineTo(i3, f2);
        }
    }

    private void animateTabOut(final Tab tab, final TabView tabView) {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(tabView, "scaleX", 1.0f, 0.0f);
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(tabView, "scaleY", 1.0f, 0.0f);
        ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(tabView, "alpha", 1.0f, 0.0f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfFloat2, objectAnimatorOfFloat3);
        animatorSet.setDuration(150L);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                TabBar.this.mTabs.removeTab(tabView);
                TabBar.this.mTabMap.remove(tab);
                TabBar.this.mUi.onRemoveTabCompleted(tab);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }
        });
        animatorSet.start();
    }

    private void animateTabIn(final Tab tab, final TabView tabView) {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(tabView, "scaleX", 0.0f, 1.0f);
        objectAnimatorOfFloat.setDuration(150L);
        objectAnimatorOfFloat.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                TabBar.this.mUi.onAddTabCompleted(tab);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationStart(Animator animator) {
                TabBar.this.mTabs.addTab(tabView);
            }
        });
        objectAnimatorOfFloat.start();
    }

    public void onSetActiveTab(Tab tab) {
        this.mTabs.setSelectedTab(this.mTabControl.getTabPosition(tab));
    }

    public void onFavicon(Tab tab, Bitmap bitmap) {
        TabView tabView = this.mTabMap.get(tab);
        if (tabView != null) {
            tabView.setFavicon(this.mUi.getFaviconDrawable(bitmap));
        }
    }

    public void onNewTab(Tab tab) {
        animateTabIn(tab, buildTabView(tab));
    }

    public void onRemoveTab(Tab tab) {
        TabView tabView = this.mTabMap.get(tab);
        if (tabView != null) {
            animateTabOut(tab, tabView);
        } else {
            this.mTabMap.remove(tab);
        }
    }

    public void onUrlAndTitle(Tab tab, String str, String str2) {
        TabView tabView = this.mTabMap.get(tab);
        if (tabView != null) {
            if (str2 != null) {
                tabView.setDisplayTitle(str2);
            } else if (str != null) {
                tabView.setDisplayTitle(UrlUtils.stripUrl(str));
            }
            tabView.updateTabIcons();
        }
    }

    private boolean isLoading() {
        Tab currentTab = this.mTabControl.getCurrentTab();
        if (currentTab != null) {
            return currentTab.inPageLoad();
        }
        return false;
    }
}
