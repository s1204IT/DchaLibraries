package com.android.internal.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.IntProperty;
import android.util.Log;
import android.util.Property;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.OverScroller;
import android.widget.Toolbar;
import com.android.internal.R;
import com.android.internal.view.menu.MenuPresenter;

public class ActionBarOverlayLayout extends ViewGroup implements DecorContentParent {
    public static final Property<ActionBarOverlayLayout, Integer> ACTION_BAR_HIDE_OFFSET = new IntProperty<ActionBarOverlayLayout>("actionBarHideOffset") {
        @Override
        public void setValue(ActionBarOverlayLayout actionBarOverlayLayout, int i) {
            actionBarOverlayLayout.setActionBarHideOffset(i);
        }

        @Override
        public Integer get(ActionBarOverlayLayout actionBarOverlayLayout) {
            return Integer.valueOf(actionBarOverlayLayout.getActionBarHideOffset());
        }
    };
    static final int[] ATTRS = {16843499, 16842841};
    private static final String TAG = "ActionBarOverlayLayout";
    private final int ACTION_BAR_ANIMATE_DELAY;
    private ActionBarContainer mActionBarBottom;
    private int mActionBarHeight;
    private ActionBarContainer mActionBarTop;
    private ActionBarVisibilityCallback mActionBarVisibilityCallback;
    private final Runnable mAddActionBarHideOffset;
    private boolean mAnimatingForFling;
    private final Rect mBaseContentInsets;
    private WindowInsets mBaseInnerInsets;
    private final Animator.AnimatorListener mBottomAnimatorListener;
    private View mContent;
    private final Rect mContentInsets;
    private ViewPropertyAnimator mCurrentActionBarBottomAnimator;
    private ViewPropertyAnimator mCurrentActionBarTopAnimator;
    private DecorToolbar mDecorToolbar;
    private OverScroller mFlingEstimator;
    private boolean mHasNonEmbeddedTabs;
    private boolean mHideOnContentScroll;
    private int mHideOnContentScrollReference;
    private boolean mIgnoreWindowContentOverlay;
    private WindowInsets mInnerInsets;
    private final Rect mLastBaseContentInsets;
    private WindowInsets mLastBaseInnerInsets;
    private WindowInsets mLastInnerInsets;
    private int mLastSystemUiVisibility;
    private boolean mOverlayMode;
    private final Runnable mRemoveActionBarHideOffset;
    private final Animator.AnimatorListener mTopAnimatorListener;
    private Drawable mWindowContentOverlay;
    private int mWindowVisibility;

    public interface ActionBarVisibilityCallback {
        void enableContentAnimations(boolean z);

        void hideForSystem();

        void onContentScrollStarted();

        void onContentScrollStopped();

        void onWindowVisibilityChanged(int i);

        void showForSystem();
    }

    public ActionBarOverlayLayout(Context context) {
        super(context);
        this.mWindowVisibility = 0;
        this.mBaseContentInsets = new Rect();
        this.mLastBaseContentInsets = new Rect();
        this.mContentInsets = new Rect();
        this.mBaseInnerInsets = WindowInsets.CONSUMED;
        this.mLastBaseInnerInsets = WindowInsets.CONSUMED;
        this.mInnerInsets = WindowInsets.CONSUMED;
        this.mLastInnerInsets = WindowInsets.CONSUMED;
        this.ACTION_BAR_ANIMATE_DELAY = 600;
        this.mTopAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                ActionBarOverlayLayout.this.mCurrentActionBarTopAnimator = null;
                ActionBarOverlayLayout.this.mAnimatingForFling = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                ActionBarOverlayLayout.this.mCurrentActionBarTopAnimator = null;
                ActionBarOverlayLayout.this.mAnimatingForFling = false;
            }
        };
        this.mBottomAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                ActionBarOverlayLayout.this.mCurrentActionBarBottomAnimator = null;
                ActionBarOverlayLayout.this.mAnimatingForFling = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                ActionBarOverlayLayout.this.mCurrentActionBarBottomAnimator = null;
                ActionBarOverlayLayout.this.mAnimatingForFling = false;
            }
        };
        this.mRemoveActionBarHideOffset = new Runnable() {
            @Override
            public void run() {
                ActionBarOverlayLayout.this.haltActionBarHideOffsetAnimations();
                ActionBarOverlayLayout.this.mCurrentActionBarTopAnimator = ActionBarOverlayLayout.this.mActionBarTop.animate().translationY(0.0f).setListener(ActionBarOverlayLayout.this.mTopAnimatorListener);
                if (ActionBarOverlayLayout.this.mActionBarBottom != null && ActionBarOverlayLayout.this.mActionBarBottom.getVisibility() != 8) {
                    ActionBarOverlayLayout.this.mCurrentActionBarBottomAnimator = ActionBarOverlayLayout.this.mActionBarBottom.animate().translationY(0.0f).setListener(ActionBarOverlayLayout.this.mBottomAnimatorListener);
                }
            }
        };
        this.mAddActionBarHideOffset = new Runnable() {
            @Override
            public void run() {
                ActionBarOverlayLayout.this.haltActionBarHideOffsetAnimations();
                ActionBarOverlayLayout.this.mCurrentActionBarTopAnimator = ActionBarOverlayLayout.this.mActionBarTop.animate().translationY(-ActionBarOverlayLayout.this.mActionBarTop.getHeight()).setListener(ActionBarOverlayLayout.this.mTopAnimatorListener);
                if (ActionBarOverlayLayout.this.mActionBarBottom != null && ActionBarOverlayLayout.this.mActionBarBottom.getVisibility() != 8) {
                    ActionBarOverlayLayout.this.mCurrentActionBarBottomAnimator = ActionBarOverlayLayout.this.mActionBarBottom.animate().translationY(ActionBarOverlayLayout.this.mActionBarBottom.getHeight()).setListener(ActionBarOverlayLayout.this.mBottomAnimatorListener);
                }
            }
        };
        init(context);
    }

    public ActionBarOverlayLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mWindowVisibility = 0;
        this.mBaseContentInsets = new Rect();
        this.mLastBaseContentInsets = new Rect();
        this.mContentInsets = new Rect();
        this.mBaseInnerInsets = WindowInsets.CONSUMED;
        this.mLastBaseInnerInsets = WindowInsets.CONSUMED;
        this.mInnerInsets = WindowInsets.CONSUMED;
        this.mLastInnerInsets = WindowInsets.CONSUMED;
        this.ACTION_BAR_ANIMATE_DELAY = 600;
        this.mTopAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                ActionBarOverlayLayout.this.mCurrentActionBarTopAnimator = null;
                ActionBarOverlayLayout.this.mAnimatingForFling = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                ActionBarOverlayLayout.this.mCurrentActionBarTopAnimator = null;
                ActionBarOverlayLayout.this.mAnimatingForFling = false;
            }
        };
        this.mBottomAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                ActionBarOverlayLayout.this.mCurrentActionBarBottomAnimator = null;
                ActionBarOverlayLayout.this.mAnimatingForFling = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                ActionBarOverlayLayout.this.mCurrentActionBarBottomAnimator = null;
                ActionBarOverlayLayout.this.mAnimatingForFling = false;
            }
        };
        this.mRemoveActionBarHideOffset = new Runnable() {
            @Override
            public void run() {
                ActionBarOverlayLayout.this.haltActionBarHideOffsetAnimations();
                ActionBarOverlayLayout.this.mCurrentActionBarTopAnimator = ActionBarOverlayLayout.this.mActionBarTop.animate().translationY(0.0f).setListener(ActionBarOverlayLayout.this.mTopAnimatorListener);
                if (ActionBarOverlayLayout.this.mActionBarBottom != null && ActionBarOverlayLayout.this.mActionBarBottom.getVisibility() != 8) {
                    ActionBarOverlayLayout.this.mCurrentActionBarBottomAnimator = ActionBarOverlayLayout.this.mActionBarBottom.animate().translationY(0.0f).setListener(ActionBarOverlayLayout.this.mBottomAnimatorListener);
                }
            }
        };
        this.mAddActionBarHideOffset = new Runnable() {
            @Override
            public void run() {
                ActionBarOverlayLayout.this.haltActionBarHideOffsetAnimations();
                ActionBarOverlayLayout.this.mCurrentActionBarTopAnimator = ActionBarOverlayLayout.this.mActionBarTop.animate().translationY(-ActionBarOverlayLayout.this.mActionBarTop.getHeight()).setListener(ActionBarOverlayLayout.this.mTopAnimatorListener);
                if (ActionBarOverlayLayout.this.mActionBarBottom != null && ActionBarOverlayLayout.this.mActionBarBottom.getVisibility() != 8) {
                    ActionBarOverlayLayout.this.mCurrentActionBarBottomAnimator = ActionBarOverlayLayout.this.mActionBarBottom.animate().translationY(ActionBarOverlayLayout.this.mActionBarBottom.getHeight()).setListener(ActionBarOverlayLayout.this.mBottomAnimatorListener);
                }
            }
        };
        init(context);
    }

    private void init(Context context) {
        boolean z;
        TypedArray typedArrayObtainStyledAttributes = getContext().getTheme().obtainStyledAttributes(ATTRS);
        boolean z2 = false;
        this.mActionBarHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(0, 0);
        this.mWindowContentOverlay = typedArrayObtainStyledAttributes.getDrawable(1);
        if (this.mWindowContentOverlay != null) {
            z = false;
        } else {
            z = true;
        }
        setWillNotDraw(z);
        typedArrayObtainStyledAttributes.recycle();
        if (context.getApplicationInfo().targetSdkVersion < 19) {
            z2 = true;
        }
        this.mIgnoreWindowContentOverlay = z2;
        this.mFlingEstimator = new OverScroller(context);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        haltActionBarHideOffsetAnimations();
    }

    public void setActionBarVisibilityCallback(ActionBarVisibilityCallback actionBarVisibilityCallback) {
        this.mActionBarVisibilityCallback = actionBarVisibilityCallback;
        if (getWindowToken() != null) {
            this.mActionBarVisibilityCallback.onWindowVisibilityChanged(this.mWindowVisibility);
            if (this.mLastSystemUiVisibility != 0) {
                onWindowSystemUiVisibilityChanged(this.mLastSystemUiVisibility);
                requestApplyInsets();
            }
        }
    }

    public void setOverlayMode(boolean z) {
        this.mOverlayMode = z;
        this.mIgnoreWindowContentOverlay = z && getContext().getApplicationInfo().targetSdkVersion < 19;
    }

    public boolean isInOverlayMode() {
        return this.mOverlayMode;
    }

    public void setHasNonEmbeddedTabs(boolean z) {
        this.mHasNonEmbeddedTabs = z;
    }

    public void setShowingForActionMode(boolean z) {
        if (z) {
            if ((getWindowSystemUiVisibility() & 1280) == 1280) {
                setDisabledSystemUiVisibility(4);
                return;
            }
            return;
        }
        setDisabledSystemUiVisibility(0);
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        init(getContext());
        requestApplyInsets();
    }

    @Override
    public void onWindowSystemUiVisibilityChanged(int i) {
        super.onWindowSystemUiVisibilityChanged(i);
        pullChildren();
        int i2 = this.mLastSystemUiVisibility ^ i;
        this.mLastSystemUiVisibility = i;
        boolean z = (i & 4) == 0;
        boolean z2 = (i & 256) != 0;
        if (this.mActionBarVisibilityCallback != null) {
            this.mActionBarVisibilityCallback.enableContentAnimations(!z2);
            if (z || !z2) {
                this.mActionBarVisibilityCallback.showForSystem();
            } else {
                this.mActionBarVisibilityCallback.hideForSystem();
            }
        }
        if ((i2 & 256) != 0 && this.mActionBarVisibilityCallback != null) {
            requestApplyInsets();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        this.mWindowVisibility = i;
        if (this.mActionBarVisibilityCallback != null) {
            this.mActionBarVisibilityCallback.onWindowVisibilityChanged(i);
        }
    }

    private boolean applyInsets(View view, Rect rect, boolean z, boolean z2, boolean z3, boolean z4) {
        boolean z5;
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        if (!z || layoutParams.leftMargin == rect.left) {
            z5 = false;
        } else {
            layoutParams.leftMargin = rect.left;
            z5 = true;
        }
        if (z2 && layoutParams.topMargin != rect.top) {
            layoutParams.topMargin = rect.top;
            z5 = true;
        }
        if (z4 && layoutParams.rightMargin != rect.right) {
            layoutParams.rightMargin = rect.right;
            z5 = true;
        }
        if (!z3 || layoutParams.bottomMargin == rect.bottom) {
            return z5;
        }
        layoutParams.bottomMargin = rect.bottom;
        return true;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        pullChildren();
        int windowSystemUiVisibility = getWindowSystemUiVisibility() & 256;
        Rect systemWindowInsets = windowInsets.getSystemWindowInsets();
        boolean zApplyInsets = applyInsets(this.mActionBarTop, systemWindowInsets, true, true, false, true);
        if (this.mActionBarBottom != null) {
            zApplyInsets |= applyInsets(this.mActionBarBottom, systemWindowInsets, true, false, true, true);
        }
        computeSystemWindowInsets(windowInsets, this.mBaseContentInsets);
        this.mBaseInnerInsets = windowInsets.inset(this.mBaseContentInsets);
        boolean z = true;
        if (!this.mLastBaseInnerInsets.equals(this.mBaseInnerInsets)) {
            this.mLastBaseInnerInsets = this.mBaseInnerInsets;
            zApplyInsets = true;
        }
        if (!this.mLastBaseContentInsets.equals(this.mBaseContentInsets)) {
            this.mLastBaseContentInsets.set(this.mBaseContentInsets);
        } else {
            z = zApplyInsets;
        }
        if (z) {
            requestLayout();
        }
        return WindowInsets.CONSUMED;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int measuredHeight;
        int measuredHeight2;
        pullChildren();
        measureChildWithMargins(this.mActionBarTop, i, 0, i2, 0);
        LayoutParams layoutParams = (LayoutParams) this.mActionBarTop.getLayoutParams();
        int iMax = Math.max(0, this.mActionBarTop.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin);
        int iMax2 = Math.max(0, this.mActionBarTop.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin);
        int iCombineMeasuredStates = combineMeasuredStates(0, this.mActionBarTop.getMeasuredState());
        if (this.mActionBarBottom != null) {
            measureChildWithMargins(this.mActionBarBottom, i, 0, i2, 0);
            LayoutParams layoutParams2 = (LayoutParams) this.mActionBarBottom.getLayoutParams();
            iMax = Math.max(iMax, this.mActionBarBottom.getMeasuredWidth() + layoutParams2.leftMargin + layoutParams2.rightMargin);
            iMax2 = Math.max(iMax2, this.mActionBarBottom.getMeasuredHeight() + layoutParams2.topMargin + layoutParams2.bottomMargin);
            iCombineMeasuredStates = combineMeasuredStates(iCombineMeasuredStates, this.mActionBarBottom.getMeasuredState());
        }
        boolean z = (getWindowSystemUiVisibility() & 256) != 0;
        if (z) {
            measuredHeight = this.mActionBarHeight;
            if (this.mHasNonEmbeddedTabs && this.mActionBarTop.getTabContainer() != null) {
                measuredHeight += this.mActionBarHeight;
            }
        } else {
            measuredHeight = this.mActionBarTop.getVisibility() != 8 ? this.mActionBarTop.getMeasuredHeight() : 0;
        }
        if (this.mDecorToolbar.isSplit() && this.mActionBarBottom != null) {
            if (z) {
                measuredHeight2 = this.mActionBarHeight;
            } else {
                measuredHeight2 = this.mActionBarBottom.getMeasuredHeight();
            }
        } else {
            measuredHeight2 = 0;
        }
        this.mContentInsets.set(this.mBaseContentInsets);
        this.mInnerInsets = this.mBaseInnerInsets;
        if (!this.mOverlayMode && !z) {
            this.mContentInsets.top += measuredHeight;
            this.mContentInsets.bottom += measuredHeight2;
            this.mInnerInsets = this.mInnerInsets.inset(0, measuredHeight, 0, measuredHeight2);
        } else {
            this.mInnerInsets = this.mInnerInsets.replaceSystemWindowInsets(this.mInnerInsets.getSystemWindowInsetLeft(), this.mInnerInsets.getSystemWindowInsetTop() + measuredHeight, this.mInnerInsets.getSystemWindowInsetRight(), this.mInnerInsets.getSystemWindowInsetBottom() + measuredHeight2);
        }
        applyInsets(this.mContent, this.mContentInsets, true, true, true, true);
        if (!this.mLastInnerInsets.equals(this.mInnerInsets)) {
            this.mLastInnerInsets = this.mInnerInsets;
            this.mContent.dispatchApplyWindowInsets(this.mInnerInsets);
        }
        measureChildWithMargins(this.mContent, i, 0, i2, 0);
        LayoutParams layoutParams3 = (LayoutParams) this.mContent.getLayoutParams();
        int iMax3 = Math.max(iMax, this.mContent.getMeasuredWidth() + layoutParams3.leftMargin + layoutParams3.rightMargin);
        int iMax4 = Math.max(iMax2, this.mContent.getMeasuredHeight() + layoutParams3.topMargin + layoutParams3.bottomMargin);
        int iCombineMeasuredStates2 = combineMeasuredStates(iCombineMeasuredStates, this.mContent.getMeasuredState());
        setMeasuredDimension(resolveSizeAndState(Math.max(iMax3 + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth()), i, iCombineMeasuredStates2), resolveSizeAndState(Math.max(iMax4 + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight()), i2, iCombineMeasuredStates2 << 16));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5;
        int childCount = getChildCount();
        int paddingLeft = getPaddingLeft();
        getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = (i4 - i2) - getPaddingBottom();
        for (int i6 = 0; i6 < childCount; i6++) {
            View childAt = getChildAt(i6);
            if (childAt.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                int measuredWidth = childAt.getMeasuredWidth();
                int measuredHeight = childAt.getMeasuredHeight();
                int i7 = layoutParams.leftMargin + paddingLeft;
                if (childAt == this.mActionBarBottom) {
                    i5 = (paddingBottom - measuredHeight) - layoutParams.bottomMargin;
                } else {
                    i5 = paddingTop + layoutParams.topMargin;
                }
                childAt.layout(i7, i5, measuredWidth + i7, measuredHeight + i5);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int bottom;
        super.draw(canvas);
        if (this.mWindowContentOverlay != null && !this.mIgnoreWindowContentOverlay) {
            if (this.mActionBarTop.getVisibility() == 0) {
                bottom = (int) (this.mActionBarTop.getBottom() + this.mActionBarTop.getTranslationY() + 0.5f);
            } else {
                bottom = 0;
            }
            this.mWindowContentOverlay.setBounds(0, bottom, getWidth(), this.mWindowContentOverlay.getIntrinsicHeight() + bottom);
            this.mWindowContentOverlay.draw(canvas);
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public boolean onStartNestedScroll(View view, View view2, int i) {
        if ((i & 2) == 0 || this.mActionBarTop.getVisibility() != 0) {
            return false;
        }
        return this.mHideOnContentScroll;
    }

    @Override
    public void onNestedScrollAccepted(View view, View view2, int i) {
        super.onNestedScrollAccepted(view, view2, i);
        this.mHideOnContentScrollReference = getActionBarHideOffset();
        haltActionBarHideOffsetAnimations();
        if (this.mActionBarVisibilityCallback != null) {
            this.mActionBarVisibilityCallback.onContentScrollStarted();
        }
    }

    @Override
    public void onNestedScroll(View view, int i, int i2, int i3, int i4) {
        this.mHideOnContentScrollReference += i2;
        setActionBarHideOffset(this.mHideOnContentScrollReference);
    }

    @Override
    public void onStopNestedScroll(View view) {
        super.onStopNestedScroll(view);
        if (this.mHideOnContentScroll && !this.mAnimatingForFling) {
            if (this.mHideOnContentScrollReference <= this.mActionBarTop.getHeight()) {
                postRemoveActionBarHideOffset();
            } else {
                postAddActionBarHideOffset();
            }
        }
        if (this.mActionBarVisibilityCallback != null) {
            this.mActionBarVisibilityCallback.onContentScrollStopped();
        }
    }

    @Override
    public boolean onNestedFling(View view, float f, float f2, boolean z) {
        if (!this.mHideOnContentScroll || !z) {
            return false;
        }
        if (shouldHideActionBarOnFling(f, f2)) {
            addActionBarHideOffset();
        } else {
            removeActionBarHideOffset();
        }
        this.mAnimatingForFling = true;
        return true;
    }

    void pullChildren() {
        if (this.mContent == null) {
            this.mContent = findViewById(16908290);
            this.mActionBarTop = (ActionBarContainer) findViewById(R.id.action_bar_container);
            this.mDecorToolbar = getDecorToolbar(findViewById(R.id.action_bar));
            this.mActionBarBottom = (ActionBarContainer) findViewById(R.id.split_action_bar);
        }
    }

    private DecorToolbar getDecorToolbar(View view) {
        if (view instanceof DecorToolbar) {
            return (DecorToolbar) view;
        }
        if (view instanceof Toolbar) {
            return ((Toolbar) view).getWrapper();
        }
        throw new IllegalStateException("Can't make a decor toolbar out of " + view.getClass().getSimpleName());
    }

    public void setHideOnContentScrollEnabled(boolean z) {
        if (z != this.mHideOnContentScroll) {
            this.mHideOnContentScroll = z;
            if (!z) {
                stopNestedScroll();
                haltActionBarHideOffsetAnimations();
                setActionBarHideOffset(0);
            }
        }
    }

    public boolean isHideOnContentScrollEnabled() {
        return this.mHideOnContentScroll;
    }

    public int getActionBarHideOffset() {
        if (this.mActionBarTop != null) {
            return -((int) this.mActionBarTop.getTranslationY());
        }
        return 0;
    }

    public void setActionBarHideOffset(int i) {
        haltActionBarHideOffsetAnimations();
        int iMax = Math.max(0, Math.min(i, this.mActionBarTop.getHeight()));
        this.mActionBarTop.setTranslationY(-iMax);
        if (this.mActionBarBottom != null && this.mActionBarBottom.getVisibility() != 8) {
            this.mActionBarBottom.setTranslationY((int) (this.mActionBarBottom.getHeight() * (iMax / r0)));
        }
    }

    private void haltActionBarHideOffsetAnimations() {
        removeCallbacks(this.mRemoveActionBarHideOffset);
        removeCallbacks(this.mAddActionBarHideOffset);
        if (this.mCurrentActionBarTopAnimator != null) {
            this.mCurrentActionBarTopAnimator.cancel();
        }
        if (this.mCurrentActionBarBottomAnimator != null) {
            this.mCurrentActionBarBottomAnimator.cancel();
        }
    }

    private void postRemoveActionBarHideOffset() {
        haltActionBarHideOffsetAnimations();
        postDelayed(this.mRemoveActionBarHideOffset, 600L);
    }

    private void postAddActionBarHideOffset() {
        haltActionBarHideOffsetAnimations();
        postDelayed(this.mAddActionBarHideOffset, 600L);
    }

    private void removeActionBarHideOffset() {
        haltActionBarHideOffsetAnimations();
        this.mRemoveActionBarHideOffset.run();
    }

    private void addActionBarHideOffset() {
        haltActionBarHideOffsetAnimations();
        this.mAddActionBarHideOffset.run();
    }

    private boolean shouldHideActionBarOnFling(float f, float f2) {
        this.mFlingEstimator.fling(0, 0, 0, (int) f2, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        return this.mFlingEstimator.getFinalY() > this.mActionBarTop.getHeight();
    }

    @Override
    public void setWindowCallback(Window.Callback callback) {
        pullChildren();
        this.mDecorToolbar.setWindowCallback(callback);
    }

    @Override
    public void setWindowTitle(CharSequence charSequence) {
        pullChildren();
        this.mDecorToolbar.setWindowTitle(charSequence);
    }

    @Override
    public CharSequence getTitle() {
        pullChildren();
        return this.mDecorToolbar.getTitle();
    }

    @Override
    public void initFeature(int i) {
        pullChildren();
        if (i == 2) {
            this.mDecorToolbar.initProgress();
        } else if (i == 5) {
            this.mDecorToolbar.initIndeterminateProgress();
        } else if (i == 9) {
            setOverlayMode(true);
        }
    }

    @Override
    public void setUiOptions(int i) {
        boolean z = true;
        if ((i & 1) == 0) {
            z = false;
        }
        boolean z2 = z ? getContext().getResources().getBoolean(R.bool.split_action_bar_is_narrow) : false;
        if (z2) {
            pullChildren();
            if (this.mActionBarBottom == null || !this.mDecorToolbar.canSplit()) {
                if (z2) {
                    Log.e(TAG, "Requested split action bar with incompatible window decor! Ignoring request.");
                    return;
                }
                return;
            }
            this.mDecorToolbar.setSplitView(this.mActionBarBottom);
            this.mDecorToolbar.setSplitToolbar(z2);
            this.mDecorToolbar.setSplitWhenNarrow(z);
            ActionBarContextView actionBarContextView = (ActionBarContextView) findViewById(R.id.action_context_bar);
            actionBarContextView.setSplitView(this.mActionBarBottom);
            actionBarContextView.setSplitToolbar(z2);
            actionBarContextView.setSplitWhenNarrow(z);
        }
    }

    @Override
    public boolean hasIcon() {
        pullChildren();
        return this.mDecorToolbar.hasIcon();
    }

    @Override
    public boolean hasLogo() {
        pullChildren();
        return this.mDecorToolbar.hasLogo();
    }

    @Override
    public void setIcon(int i) {
        pullChildren();
        this.mDecorToolbar.setIcon(i);
    }

    @Override
    public void setIcon(Drawable drawable) {
        pullChildren();
        this.mDecorToolbar.setIcon(drawable);
    }

    @Override
    public void setLogo(int i) {
        pullChildren();
        this.mDecorToolbar.setLogo(i);
    }

    @Override
    public boolean canShowOverflowMenu() {
        pullChildren();
        return this.mDecorToolbar.canShowOverflowMenu();
    }

    @Override
    public boolean isOverflowMenuShowing() {
        pullChildren();
        return this.mDecorToolbar.isOverflowMenuShowing();
    }

    @Override
    public boolean isOverflowMenuShowPending() {
        pullChildren();
        return this.mDecorToolbar.isOverflowMenuShowPending();
    }

    @Override
    public boolean showOverflowMenu() {
        pullChildren();
        return this.mDecorToolbar.showOverflowMenu();
    }

    @Override
    public boolean hideOverflowMenu() {
        pullChildren();
        return this.mDecorToolbar.hideOverflowMenu();
    }

    @Override
    public void setMenuPrepared() {
        pullChildren();
        this.mDecorToolbar.setMenuPrepared();
    }

    @Override
    public void setMenu(Menu menu, MenuPresenter.Callback callback) {
        pullChildren();
        this.mDecorToolbar.setMenu(menu, callback);
    }

    @Override
    public void saveToolbarHierarchyState(SparseArray<Parcelable> sparseArray) {
        pullChildren();
        this.mDecorToolbar.saveHierarchyState(sparseArray);
    }

    @Override
    public void restoreToolbarHierarchyState(SparseArray<Parcelable> sparseArray) {
        pullChildren();
        this.mDecorToolbar.restoreHierarchyState(sparseArray);
    }

    @Override
    public void dismissPopups() {
        pullChildren();
        this.mDecorToolbar.dismissPopupMenus();
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
        }
    }
}
