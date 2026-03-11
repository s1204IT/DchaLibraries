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

    public TabBar(Activity activity, UiController controller, XLargeUi ui) {
        super(activity);
        this.mCurrentTextureWidth = 0;
        this.mCurrentTextureHeight = 0;
        this.mActiveShaderPaint = new Paint();
        this.mInactiveShaderPaint = new Paint();
        this.mFocusPaint = new Paint();
        this.mActiveMatrix = new Matrix();
        this.mInactiveMatrix = new Matrix();
        this.mActivity = activity;
        this.mUiController = controller;
        this.mTabControl = this.mUiController.getTabControl();
        this.mUi = ui;
        Resources res = activity.getResources();
        this.mTabWidth = (int) res.getDimension(R.dimen.tab_width);
        this.mActiveDrawable = res.getDrawable(R.drawable.bg_urlbar);
        this.mInactiveDrawable = res.getDrawable(R.drawable.browsertab_inactive);
        this.mTabMap = new HashMap();
        LayoutInflater factory = LayoutInflater.from(activity);
        factory.inflate(R.layout.tab_bar, this);
        setPadding(0, (int) res.getDimension(R.dimen.tab_padding_top), 0, 0);
        this.mTabs = (TabScrollView) findViewById(R.id.tabs);
        this.mNewTab = (ImageButton) findViewById(R.id.newtab);
        this.mNewTab.setOnClickListener(this);
        updateTabs(this.mUiController.getTabs());
        this.mButtonWidth = -1;
        this.mTabOverlap = (int) res.getDimension(R.dimen.tab_overlap);
        this.mAddTabOverlap = (int) res.getDimension(R.dimen.tab_addoverlap);
        this.mTabSliceWidth = (int) res.getDimension(R.dimen.tab_slice);
        this.mActiveShaderPaint.setStyle(Paint.Style.FILL);
        this.mActiveShaderPaint.setAntiAlias(true);
        this.mInactiveShaderPaint.setStyle(Paint.Style.FILL);
        this.mInactiveShaderPaint.setAntiAlias(true);
        this.mFocusPaint.setStyle(Paint.Style.STROKE);
        this.mFocusPaint.setStrokeWidth(res.getDimension(R.dimen.tab_focus_stroke));
        this.mFocusPaint.setAntiAlias(true);
        this.mFocusPaint.setColor(res.getColor(R.color.tabFocusHighlight));
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        Resources res = this.mActivity.getResources();
        this.mTabWidth = (int) res.getDimension(R.dimen.tab_width);
        this.mTabs.updateLayout();
    }

    void setUseQuickControls(boolean useQuickControls) {
        this.mUseQuickControls = useQuickControls;
        this.mNewTab.setVisibility(this.mUseQuickControls ? 8 : 0);
    }

    void updateTabs(List<Tab> tabs) {
        this.mTabs.clearTabs();
        this.mTabMap.clear();
        for (Tab tab : tabs) {
            TabView tv = buildTabView(tab);
            this.mTabs.addTab(tv);
        }
        this.mTabs.setSelectedTab(this.mTabControl.getCurrentPosition());
    }

    @Override
    protected void onMeasure(int hspec, int vspec) {
        super.onMeasure(hspec, vspec);
        int w = getMeasuredWidth();
        if (!this.mUseQuickControls) {
            w -= this.mAddTabOverlap;
        }
        setMeasuredDimension(w, getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int pl = getPaddingLeft();
        int pt = getPaddingTop();
        int sw = this.mTabs.getMeasuredWidth();
        int w = (right - left) - pl;
        if (this.mUseQuickControls) {
            this.mButtonWidth = 0;
        } else {
            this.mButtonWidth = this.mNewTab.getMeasuredWidth() - this.mAddTabOverlap;
            if (w - sw < this.mButtonWidth) {
                sw = w - this.mButtonWidth;
            }
        }
        this.mTabs.layout(pl, pt, pl + sw, bottom - top);
        if (this.mUseQuickControls) {
            return;
        }
        this.mNewTab.layout((pl + sw) - this.mAddTabOverlap, pt, ((pl + sw) + this.mButtonWidth) - this.mAddTabOverlap, bottom - top);
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
        if (!(view instanceof TabView)) {
            return;
        }
        Tab tab = ((TabView) view).mTab;
        int ix = this.mTabs.getChildIndex(view);
        if (ix < 0) {
            return;
        }
        this.mTabs.setSelectedTab(ix);
        this.mUiController.switchToTab(tab);
    }

    private void showUrlBar() {
        this.mUi.stopWebViewScrolling();
        this.mUi.showTitleBar();
    }

    private TabView buildTabView(Tab tab) {
        TabView tabview = new TabView(this.mActivity, tab);
        this.mTabMap.put(tab, tabview);
        tabview.setOnClickListener(this);
        return tabview;
    }

    public static Bitmap getDrawableAsBitmap(Drawable drawable, int width, int height) {
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(c);
        c.setBitmap(null);
        return b;
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
            LayoutInflater inflater = LayoutInflater.from(getContext());
            this.mTabContent = inflater.inflate(R.layout.tab_title, (ViewGroup) this, true);
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
        public void onClick(View v) {
            if (v != this.mClose) {
                return;
            }
            closeTab();
        }

        private void updateFromTab() {
            String displayTitle = this.mTab.getTitle();
            if (displayTitle == null) {
                displayTitle = this.mTab.getUrl();
            }
            setDisplayTitle(displayTitle);
            if (this.mTab.getFavicon() != null) {
                setFavicon(TabBar.this.mUi.getFaviconDrawable(this.mTab.getFavicon()));
            }
            updateTabIcons();
        }

        public void updateTabIcons() {
            this.mIncognito.setVisibility(this.mTab.isPrivateBrowsingEnabled() ? 0 : 8);
            this.mSnapshot.setVisibility(this.mTab.isSnapshot() ? 0 : 8);
        }

        @Override
        public void setActivated(boolean selected) {
            this.mSelected = selected;
            this.mClose.setVisibility(this.mSelected ? 0 : 8);
            this.mIconView.setVisibility(this.mSelected ? 8 : 0);
            this.mTitle.setTextAppearance(TabBar.this.mActivity, this.mSelected ? R.style.TabTitleSelected : R.style.TabTitleUnselected);
            setHorizontalFadingEdgeEnabled(!this.mSelected);
            super.setActivated(selected);
            updateLayoutParams();
            setFocusable(selected ? false : true);
            postInvalidate();
        }

        public void updateLayoutParams() {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) getLayoutParams();
            lp.width = TabBar.this.mTabWidth;
            lp.height = -1;
            setLayoutParams(lp);
        }

        void setDisplayTitle(String title) {
            if (title.startsWith("about:blank")) {
                this.mTitle.setText("about:blank");
            } else {
                this.mTitle.setText(title);
            }
        }

        void setFavicon(Drawable d) {
            this.mIconView.setImageDrawable(d);
        }

        private void closeTab() {
            if (this.mTab == TabBar.this.mTabControl.getCurrentTab()) {
                TabBar.this.mUiController.closeCurrentTab();
            } else {
                TabBar.this.mUiController.closeTab(this.mTab);
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            setTabPath(this.mPath, 0, 0, r - l, b - t);
            setFocusPath(this.mFocusPath, 0, 0, r - l, b - t);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            if (TabBar.this.mCurrentTextureWidth != TabBar.this.mUi.getContentWidth() || TabBar.this.mCurrentTextureHeight != getHeight()) {
                TabBar.this.mCurrentTextureWidth = TabBar.this.mUi.getContentWidth();
                TabBar.this.mCurrentTextureHeight = getHeight();
                if (TabBar.this.mCurrentTextureWidth > 0 && TabBar.this.mCurrentTextureHeight > 0) {
                    Bitmap activeTexture = TabBar.getDrawableAsBitmap(TabBar.this.mActiveDrawable, TabBar.this.mCurrentTextureWidth, TabBar.this.mCurrentTextureHeight);
                    Bitmap inactiveTexture = TabBar.getDrawableAsBitmap(TabBar.this.mInactiveDrawable, TabBar.this.mCurrentTextureWidth, TabBar.this.mCurrentTextureHeight);
                    TabBar.this.mActiveShader = new BitmapShader(activeTexture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    TabBar.this.mActiveShaderPaint.setShader(TabBar.this.mActiveShader);
                    TabBar.this.mInactiveShader = new BitmapShader(inactiveTexture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    TabBar.this.mInactiveShaderPaint.setShader(TabBar.this.mInactiveShader);
                }
            }
            if (TabBar.this.mActiveShader != null && TabBar.this.mInactiveShader != null) {
                int state = canvas.save();
                getLocationInWindow(this.mWindowPos);
                Paint paint = this.mSelected ? TabBar.this.mActiveShaderPaint : TabBar.this.mInactiveShaderPaint;
                drawClipped(canvas, paint, this.mPath, this.mWindowPos[0]);
                canvas.restoreToCount(state);
            }
            super.dispatchDraw(canvas);
        }

        private void drawClipped(Canvas canvas, Paint paint, Path clipPath, int left) {
            Matrix matrix = this.mSelected ? TabBar.this.mActiveMatrix : TabBar.this.mInactiveMatrix;
            matrix.setTranslate(-left, 0.0f);
            Shader shader = this.mSelected ? TabBar.this.mActiveShader : TabBar.this.mInactiveShader;
            shader.setLocalMatrix(matrix);
            paint.setShader(shader);
            canvas.drawPath(clipPath, paint);
            if (!isFocused()) {
                return;
            }
            canvas.drawPath(this.mFocusPath, TabBar.this.mFocusPaint);
        }

        private void setTabPath(Path path, int l, int t, int r, int b) {
            path.reset();
            path.moveTo(l, b);
            path.lineTo(l, t);
            path.lineTo(r - TabBar.this.mTabSliceWidth, t);
            path.lineTo(r, b);
            path.close();
        }

        private void setFocusPath(Path path, int l, int t, int r, int b) {
            path.reset();
            path.moveTo(l, b);
            path.lineTo(l, t);
            path.lineTo(r - TabBar.this.mTabSliceWidth, t);
            path.lineTo(r, b);
        }
    }

    private void animateTabOut(final Tab tab, final TabView tv) {
        ObjectAnimator scalex = ObjectAnimator.ofFloat(tv, "scaleX", 1.0f, 0.0f);
        ObjectAnimator scaley = ObjectAnimator.ofFloat(tv, "scaleY", 1.0f, 0.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(tv, "alpha", 1.0f, 0.0f);
        AnimatorSet animator = new AnimatorSet();
        animator.playTogether(scalex, scaley, alpha);
        animator.setDuration(150L);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                TabBar.this.mTabs.removeTab(tv);
                TabBar.this.mTabMap.remove(tab);
                TabBar.this.mUi.onRemoveTabCompleted(tab);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
        });
        animator.start();
    }

    private void animateTabIn(final Tab tab, final TabView tv) {
        ObjectAnimator scalex = ObjectAnimator.ofFloat(tv, "scaleX", 0.0f, 1.0f);
        scalex.setDuration(150L);
        scalex.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                TabBar.this.mUi.onAddTabCompleted(tab);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
                TabBar.this.mTabs.addTab(tv);
            }
        });
        scalex.start();
    }

    public void onSetActiveTab(Tab tab) {
        this.mTabs.setSelectedTab(this.mTabControl.getTabPosition(tab));
    }

    public void onFavicon(Tab tab, Bitmap favicon) {
        TabView tv = this.mTabMap.get(tab);
        if (tv == null) {
            return;
        }
        tv.setFavicon(this.mUi.getFaviconDrawable(favicon));
    }

    public void onNewTab(Tab tab) {
        TabView tv = buildTabView(tab);
        animateTabIn(tab, tv);
    }

    public void onRemoveTab(Tab tab) {
        TabView tv = this.mTabMap.get(tab);
        if (tv != null) {
            animateTabOut(tab, tv);
        } else {
            this.mTabMap.remove(tab);
        }
    }

    public void onUrlAndTitle(Tab tab, String url, String title) {
        TabView tv = this.mTabMap.get(tab);
        if (tv == null) {
            return;
        }
        if (title != null) {
            tv.setDisplayTitle(title);
        } else if (url != null) {
            tv.setDisplayTitle(UrlUtils.stripUrl(url));
        }
        tv.updateTabIcons();
    }

    private boolean isLoading() {
        Tab tab = this.mTabControl.getCurrentTab();
        if (tab != null) {
            return tab.inPageLoad();
        }
        return false;
    }
}
