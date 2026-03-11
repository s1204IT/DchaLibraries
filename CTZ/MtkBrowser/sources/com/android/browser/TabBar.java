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
        final TabBar this$0;

        public TabView(TabBar tabBar, Context context, Tab tab) {
            super(context);
            this.this$0 = tabBar;
            setWillNotDraw(false);
            this.mPath = new Path();
            this.mFocusPath = new Path();
            this.mWindowPos = new int[2];
            this.mTab = tab;
            setGravity(16);
            setOrientation(0);
            setPadding(tabBar.mTabOverlap, 0, tabBar.mTabSliceWidth, 0);
            this.mTabContent = LayoutInflater.from(getContext()).inflate(2130968629, (ViewGroup) this, true);
            this.mTitle = (TextView) this.mTabContent.findViewById(2131558407);
            this.mIconView = (ImageView) this.mTabContent.findViewById(2131558406);
            this.mLock = (ImageView) this.mTabContent.findViewById(2131558527);
            this.mClose = (ImageView) this.mTabContent.findViewById(2131558499);
            this.mClose.setOnClickListener(this);
            this.mIncognito = this.mTabContent.findViewById(2131558525);
            this.mSnapshot = this.mTabContent.findViewById(2131558526);
            this.mSelected = false;
            updateFromTab();
        }

        private void closeTab() {
            if (this.mTab == this.this$0.mTabControl.getCurrentTab()) {
                this.this$0.mUiController.closeCurrentTab();
            } else {
                this.this$0.mUiController.closeTab(this.mTab);
            }
        }

        private void drawClipped(Canvas canvas, Paint paint, Path path, int i) {
            Matrix matrix = this.mSelected ? this.this$0.mActiveMatrix : this.this$0.mInactiveMatrix;
            matrix.setTranslate(-i, 0.0f);
            BitmapShader bitmapShader = this.mSelected ? this.this$0.mActiveShader : this.this$0.mInactiveShader;
            bitmapShader.setLocalMatrix(matrix);
            paint.setShader(bitmapShader);
            canvas.drawPath(path, paint);
            if (isFocused()) {
                canvas.drawPath(this.mFocusPath, this.this$0.mFocusPaint);
            }
        }

        private void setFocusPath(Path path, int i, int i2, int i3, int i4) {
            path.reset();
            float f = i;
            float f2 = i4;
            path.moveTo(f, f2);
            float f3 = i2;
            path.lineTo(f, f3);
            path.lineTo(i3 - this.this$0.mTabSliceWidth, f3);
            path.lineTo(i3, f2);
        }

        private void setTabPath(Path path, int i, int i2, int i3, int i4) {
            path.reset();
            float f = i;
            float f2 = i4;
            path.moveTo(f, f2);
            float f3 = i2;
            path.lineTo(f, f3);
            path.lineTo(i3 - this.this$0.mTabSliceWidth, f3);
            path.lineTo(i3, f2);
            path.close();
        }

        private void updateFromTab() {
            String title = this.mTab.getTitle();
            if (title == null) {
                title = this.mTab.getUrl();
            }
            setDisplayTitle(title);
            if (this.mTab.getFavicon() != null) {
                setFavicon(this.this$0.mUi.getFaviconDrawable(this.mTab.getFavicon()));
            }
            updateTabIcons();
        }

        public void updateTabIcons() {
            this.mIncognito.setVisibility(this.mTab.isPrivateBrowsingEnabled() ? 0 : 8);
            this.mSnapshot.setVisibility(this.mTab.isSnapshot() ? 0 : 8);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            if (this.this$0.mCurrentTextureWidth != this.this$0.mUi.getContentWidth() || this.this$0.mCurrentTextureHeight != getHeight()) {
                this.this$0.mCurrentTextureWidth = this.this$0.mUi.getContentWidth();
                this.this$0.mCurrentTextureHeight = getHeight();
                if (this.this$0.mCurrentTextureWidth > 0 && this.this$0.mCurrentTextureHeight > 0) {
                    Bitmap drawableAsBitmap = TabBar.getDrawableAsBitmap(this.this$0.mActiveDrawable, this.this$0.mCurrentTextureWidth, this.this$0.mCurrentTextureHeight);
                    Bitmap drawableAsBitmap2 = TabBar.getDrawableAsBitmap(this.this$0.mInactiveDrawable, this.this$0.mCurrentTextureWidth, this.this$0.mCurrentTextureHeight);
                    this.this$0.mActiveShader = new BitmapShader(drawableAsBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    this.this$0.mActiveShaderPaint.setShader(this.this$0.mActiveShader);
                    this.this$0.mInactiveShader = new BitmapShader(drawableAsBitmap2, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    this.this$0.mInactiveShaderPaint.setShader(this.this$0.mInactiveShader);
                }
            }
            if (this.this$0.mActiveShader != null && this.this$0.mInactiveShader != null) {
                int iSave = canvas.save();
                getLocationInWindow(this.mWindowPos);
                drawClipped(canvas, this.mSelected ? this.this$0.mActiveShaderPaint : this.this$0.mInactiveShaderPaint, this.mPath, this.mWindowPos[0]);
                canvas.restoreToCount(iSave);
            }
            super.dispatchDraw(canvas);
        }

        @Override
        public void onClick(View view) {
            if (view == this.mClose) {
                closeTab();
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
        public void setActivated(boolean z) {
            this.mSelected = z;
            this.mClose.setVisibility(this.mSelected ? 0 : 8);
            this.mIconView.setVisibility(this.mSelected ? 8 : 0);
            this.mTitle.setTextAppearance(this.this$0.mActivity, this.mSelected ? 2131689485 : 2131689486);
            setHorizontalFadingEdgeEnabled(!this.mSelected);
            super.setActivated(z);
            updateLayoutParams();
            setFocusable(!z);
            postInvalidate();
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

        public void updateLayoutParams() {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) getLayoutParams();
            layoutParams.width = this.this$0.mTabWidth;
            layoutParams.height = -1;
            setLayoutParams(layoutParams);
        }
    }

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
        this.mTabWidth = (int) resources.getDimension(2131427328);
        this.mActiveDrawable = resources.getDrawable(2130837508);
        this.mInactiveDrawable = resources.getDrawable(2130837522);
        this.mTabMap = new HashMap();
        LayoutInflater.from(activity).inflate(2130968628, this);
        setPadding(0, (int) resources.getDimension(2131427357), 0, 0);
        this.mTabs = (TabScrollView) findViewById(2131558443);
        this.mNewTab = (ImageButton) findViewById(2131558494);
        this.mNewTab.setOnClickListener(this);
        updateTabs(this.mUiController.getTabs());
        this.mButtonWidth = -1;
        this.mTabOverlap = (int) resources.getDimension(2131427330);
        this.mAddTabOverlap = (int) resources.getDimension(2131427331);
        this.mTabSliceWidth = (int) resources.getDimension(2131427332);
        this.mActiveShaderPaint.setStyle(Paint.Style.FILL);
        this.mActiveShaderPaint.setAntiAlias(true);
        this.mInactiveShaderPaint.setStyle(Paint.Style.FILL);
        this.mInactiveShaderPaint.setAntiAlias(true);
        this.mFocusPaint.setStyle(Paint.Style.STROKE);
        this.mFocusPaint.setStrokeWidth(resources.getDimension(2131427333));
        this.mFocusPaint.setAntiAlias(true);
        this.mFocusPaint.setColor(resources.getColor(2131361801));
    }

    private void animateTabIn(Tab tab, TabView tabView) {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(tabView, "scaleX", 0.0f, 1.0f);
        objectAnimatorOfFloat.setDuration(150L);
        objectAnimatorOfFloat.addListener(new Animator.AnimatorListener(this, tab, tabView) {
            final TabBar this$0;
            final Tab val$tab;
            final TabView val$tv;

            {
                this.this$0 = this;
                this.val$tab = tab;
                this.val$tv = tabView;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                this.this$0.mUi.onAddTabCompleted(this.val$tab);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationStart(Animator animator) {
                this.this$0.mTabs.addTab(this.val$tv);
            }
        });
        objectAnimatorOfFloat.start();
    }

    private void animateTabOut(Tab tab, TabView tabView) {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(tabView, "scaleX", 1.0f, 0.0f);
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(tabView, "scaleY", 1.0f, 0.0f);
        ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(tabView, "alpha", 1.0f, 0.0f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimatorOfFloat, objectAnimatorOfFloat2, objectAnimatorOfFloat3);
        animatorSet.setDuration(150L);
        animatorSet.addListener(new Animator.AnimatorListener(this, tabView, tab) {
            final TabBar this$0;
            final Tab val$tab;
            final TabView val$tv;

            {
                this.this$0 = this;
                this.val$tv = tabView;
                this.val$tab = tab;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                this.this$0.mTabs.removeTab(this.val$tv);
                this.this$0.mTabMap.remove(this.val$tab);
                this.this$0.mUi.onRemoveTabCompleted(this.val$tab);
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

    private TabView buildTabView(Tab tab) {
        TabView tabView = new TabView(this, this.mActivity, tab);
        this.mTabMap.put(tab, tabView);
        tabView.setOnClickListener(this);
        return tabView;
    }

    public static Bitmap getDrawableAsBitmap(Drawable drawable, int i, int i2) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        drawable.setBounds(0, 0, i, i2);
        drawable.draw(canvas);
        canvas.setBitmap(null);
        return bitmapCreateBitmap;
    }

    private boolean isLoading() {
        Tab currentTab = this.mTabControl.getCurrentTab();
        if (currentTab != null) {
            return currentTab.inPageLoad();
        }
        return false;
    }

    private void showUrlBar() {
        this.mUi.stopWebViewScrolling();
        this.mUi.showTitleBar();
    }

    @Override
    public void onClick(View view) {
        if (this.mNewTab == view) {
            this.mUiController.openTabToHomePage();
            return;
        }
        if (this.mTabs.getSelectedTab() != view) {
            if (view instanceof TabView) {
                Tab tab = ((TabView) view).mTab;
                int childIndex = this.mTabs.getChildIndex(view);
                if (childIndex >= 0) {
                    this.mTabs.setSelectedTab(childIndex);
                    this.mUiController.switchToTab(tab);
                    return;
                }
                return;
            }
            return;
        }
        if (!this.mUseQuickControls) {
            if (!this.mUi.isTitleBarShowing() || isLoading()) {
                showUrlBar();
                return;
            } else {
                this.mUi.stopEditingUrl();
                this.mUi.hideTitleBar();
                return;
            }
        }
        if (!this.mUi.isTitleBarShowing() || isLoading()) {
            this.mUi.stopWebViewScrolling();
            this.mUi.editUrl(false, false);
        } else {
            this.mUi.stopEditingUrl();
            this.mUi.hideTitleBar();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mTabWidth = (int) this.mActivity.getResources().getDimension(2131427328);
        this.mTabs.updateLayout();
    }

    public void onFavicon(Tab tab, Bitmap bitmap) {
        TabView tabView = this.mTabMap.get(tab);
        if (tabView != null) {
            tabView.setFavicon(this.mUi.getFaviconDrawable(bitmap));
        }
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
        if (this.mUseQuickControls) {
            return;
        }
        this.mNewTab.layout(i6 - this.mAddTabOverlap, paddingTop, (i6 + this.mButtonWidth) - this.mAddTabOverlap, i7);
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

    public void onSetActiveTab(Tab tab) {
        this.mTabs.setSelectedTab(this.mTabControl.getTabPosition(tab));
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
}
