package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import java.util.ArrayList;

public abstract class PagedView extends ViewGroup implements ViewGroup.OnHierarchyChangeListener {
    private int DELETE_SLIDE_IN_SIDE_PAGE_DURATION;
    private int DRAG_TO_DELETE_FADE_OUT_DURATION;
    private int FLING_TO_DELETE_FADE_OUT_DURATION;
    private float FLING_TO_DELETE_FRICTION;
    private float FLING_TO_DELETE_MAX_FLING_DEGREES;
    private int NUM_ANIMATIONS_RUNNING_BEFORE_ZOOM_OUT;
    private long REORDERING_DELETE_DROP_TARGET_FADE_DURATION;
    private int REORDERING_DROP_REPOSITION_DURATION;
    protected int REORDERING_REORDER_REPOSITION_DURATION;
    private float REORDERING_SIDE_PAGE_BUFFER_PERCENTAGE;
    private int REORDERING_SIDE_PAGE_HOVER_TIMEOUT;
    protected int REORDERING_ZOOM_IN_OUT_DURATION;
    Runnable hideScrollingIndicatorRunnable;
    protected int mActivePointerId;
    protected boolean mAllowOverScroll;
    private Rect mAltTmpRect;
    protected int mCellCountX;
    protected int mCellCountY;
    protected int mChildCountOnLastMeasure;
    private int[] mChildOffsets;
    private int[] mChildOffsetsWithLayoutScale;
    private int[] mChildRelativeOffsets;
    protected boolean mContentIsRefreshable;
    protected int mCurrentPage;
    protected boolean mDeferScrollUpdate;
    private boolean mDeferringForDelete;
    private View mDeleteDropTarget;
    private String mDeleteString;
    protected float mDensity;
    protected ArrayList<Boolean> mDirtyPageContent;
    private boolean mDownEventOnEdge;
    private float mDownMotionX;
    private float mDownMotionY;
    private float mDownScrollX;
    protected View mDragView;
    private int mEdgeSwipeRegionSize;
    protected boolean mFadeInAdjacentScreens;
    protected boolean mFirstLayout;
    protected int mFlingThresholdVelocity;
    protected int mFlingToDeleteThresholdVelocity;
    protected boolean mForceDrawAllChildrenNextFrame;
    protected boolean mForceScreenScrolled;
    private boolean mIsCameraEvent;
    protected boolean mIsDataReady;
    protected boolean mIsPageMoving;
    private boolean mIsReordering;
    protected float mLastMotionX;
    protected float mLastMotionXRemainder;
    protected float mLastMotionY;
    private int mLastScreenCenter;
    protected float mLayoutScale;
    protected View.OnLongClickListener mLongClickListener;
    protected int mMaxScrollX;
    private int mMaximumVelocity;
    protected int mMinFlingVelocity;
    private float mMinScale;
    protected int mMinSnapVelocity;
    private int mMinimumWidth;
    protected int mNextPage;
    AnimatorListenerAdapter mOffScreenAnimationListener;
    private boolean mOnPageBeginWarpCalled;
    private boolean mOnPageEndWarpCalled;
    AnimatorListenerAdapter mOnScreenAnimationListener;
    private boolean mOnlyAllowEdgeSwipes;
    protected int mOverScrollX;
    protected int mPageSpacing;
    private int mPageSwapIndex;
    private PageSwitchListener mPageSwitchListener;
    private int mPageWarpIndex;
    private int mPagingTouchSlop;
    private float mParentDownMotionX;
    private float mParentDownMotionY;
    private int mPostReorderingPreZoomInRemainingAnimationCount;
    private Runnable mPostReorderingPreZoomInRunnable;
    private boolean mReorderingStarted;
    private View mScrollIndicator;
    private ValueAnimator mScrollIndicatorAnimator;
    private int mScrollIndicatorPaddingLeft;
    private int mScrollIndicatorPaddingRight;
    protected Scroller mScroller;
    private boolean mShouldShowScrollIndicator;
    private boolean mShouldShowScrollIndicatorImmediately;
    private int mSidePageHoverIndex;
    private Runnable mSidePageHoverRunnable;
    protected float mSmoothingTime;
    protected int[] mTempVisiblePagesRange;
    private Matrix mTmpInvMatrix;
    private float[] mTmpPoint;
    private Rect mTmpRect;
    private boolean mTopAlignPageWhenShrinkingForBouncer;
    protected float mTotalMotionX;
    protected int mTouchSlop;
    protected int mTouchState;
    protected float mTouchX;
    protected int mUnboundedScrollX;
    protected boolean mUsePagingTouchSlop;
    private VelocityTracker mVelocityTracker;
    private Rect mViewport;
    private ViewPropertyAnimator mWarpAnimation;
    private boolean mWarpPageExposed;
    private float mWarpPeekAmount;
    protected AnimatorSet mZoomInOutAnim;

    public interface PageSwitchListener {
        void onPageSwitched(View view, int i);

        void onPageSwitching(View view, int i);
    }

    public abstract void onAddView(View view, int i);

    public abstract void onRemoveView(View view, boolean z);

    public abstract void onRemoveViewAnimationCompleted();

    public PagedView(Context context) {
        this(context, null);
    }

    public PagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFirstLayout = true;
        this.mNextPage = -1;
        this.mLastScreenCenter = -1;
        this.mTouchState = 0;
        this.mForceScreenScrolled = false;
        this.mCellCountX = 0;
        this.mCellCountY = 0;
        this.mAllowOverScroll = true;
        this.mTempVisiblePagesRange = new int[2];
        this.mLayoutScale = 1.0f;
        this.mActivePointerId = -1;
        this.mContentIsRefreshable = true;
        this.mFadeInAdjacentScreens = false;
        this.mUsePagingTouchSlop = true;
        this.mDeferScrollUpdate = false;
        this.mIsPageMoving = false;
        this.mIsDataReady = true;
        this.mShouldShowScrollIndicator = false;
        this.mShouldShowScrollIndicatorImmediately = false;
        this.mViewport = new Rect();
        this.REORDERING_DROP_REPOSITION_DURATION = 200;
        this.REORDERING_REORDER_REPOSITION_DURATION = 300;
        this.REORDERING_ZOOM_IN_OUT_DURATION = 250;
        this.REORDERING_SIDE_PAGE_HOVER_TIMEOUT = 300;
        this.REORDERING_SIDE_PAGE_BUFFER_PERCENTAGE = 0.1f;
        this.REORDERING_DELETE_DROP_TARGET_FADE_DURATION = 150L;
        this.mMinScale = 1.0f;
        this.mSidePageHoverIndex = -1;
        this.mReorderingStarted = false;
        this.NUM_ANIMATIONS_RUNNING_BEFORE_ZOOM_OUT = 2;
        this.mOnlyAllowEdgeSwipes = false;
        this.mDownEventOnEdge = false;
        this.mEdgeSwipeRegionSize = 0;
        this.mTmpInvMatrix = new Matrix();
        this.mTmpPoint = new float[2];
        this.mTmpRect = new Rect();
        this.mAltTmpRect = new Rect();
        this.FLING_TO_DELETE_FADE_OUT_DURATION = 350;
        this.FLING_TO_DELETE_FRICTION = 0.035f;
        this.FLING_TO_DELETE_MAX_FLING_DEGREES = 65.0f;
        this.mFlingToDeleteThresholdVelocity = -1400;
        this.mDeferringForDelete = false;
        this.DELETE_SLIDE_IN_SIDE_PAGE_DURATION = 250;
        this.DRAG_TO_DELETE_FADE_OUT_DURATION = 350;
        this.mTopAlignPageWhenShrinkingForBouncer = false;
        this.mPageSwapIndex = -1;
        this.mPageWarpIndex = -1;
        this.hideScrollingIndicatorRunnable = new Runnable() {
            @Override
            public void run() {
                PagedView.this.hideScrollingIndicator(false);
            }
        };
        this.mOnScreenAnimationListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                PagedView.this.mWarpAnimation = null;
                if (PagedView.this.mTouchState != 1 && PagedView.this.mTouchState != 5) {
                    PagedView.this.animateWarpPageOffScreen("onScreen end", true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                PagedView.this.mWarpAnimation = null;
            }
        };
        this.mOffScreenAnimationListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                PagedView.this.mWarpAnimation = null;
                PagedView.this.mWarpPageExposed = false;
                KeyguardWidgetFrame v = (KeyguardWidgetFrame) PagedView.this.getPageAt(PagedView.this.getPageWarpIndex());
                v.setTranslationX(0.0f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                PagedView.this.mWarpAnimation = null;
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagedView, defStyle, 0);
        setPageSpacing(a.getDimensionPixelSize(R.styleable.PagedView_pageSpacing, 0));
        this.mScrollIndicatorPaddingLeft = a.getDimensionPixelSize(R.styleable.PagedView_scrollIndicatorPaddingLeft, 0);
        this.mScrollIndicatorPaddingRight = a.getDimensionPixelSize(R.styleable.PagedView_scrollIndicatorPaddingRight, 0);
        a.recycle();
        Resources r = getResources();
        this.mEdgeSwipeRegionSize = r.getDimensionPixelSize(R.dimen.kg_edge_swipe_region_size);
        this.mTopAlignPageWhenShrinkingForBouncer = r.getBoolean(R.bool.kg_top_align_page_shrink_on_bouncer_visible);
        setHapticFeedbackEnabled(false);
        init();
    }

    protected void init() {
        this.mDirtyPageContent = new ArrayList<>();
        this.mDirtyPageContent.ensureCapacity(32);
        this.mScroller = new Scroller(getContext(), new ScrollInterpolator());
        this.mCurrentPage = 0;
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mPagingTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mDensity = getResources().getDisplayMetrics().density;
        this.mWarpPeekAmount = this.mDensity * (-75.0f);
        this.mFlingToDeleteThresholdVelocity = (int) (this.mFlingToDeleteThresholdVelocity * this.mDensity);
        this.mFlingThresholdVelocity = (int) (this.mDensity * 1500.0f);
        this.mMinFlingVelocity = (int) (500.0f * this.mDensity);
        this.mMinSnapVelocity = (int) (this.mDensity * 1500.0f);
        setOnHierarchyChangeListener(this);
    }

    void setDeleteDropTarget(View v) {
        this.mDeleteDropTarget = v;
    }

    float[] mapPointFromViewToParent(View v, float x, float y) {
        this.mTmpPoint[0] = x;
        this.mTmpPoint[1] = y;
        v.getMatrix().mapPoints(this.mTmpPoint);
        float[] fArr = this.mTmpPoint;
        fArr[0] = fArr[0] + v.getLeft();
        float[] fArr2 = this.mTmpPoint;
        fArr2[1] = fArr2[1] + v.getTop();
        return this.mTmpPoint;
    }

    float[] mapPointFromParentToView(View v, float x, float y) {
        this.mTmpPoint[0] = x - v.getLeft();
        this.mTmpPoint[1] = y - v.getTop();
        v.getMatrix().invert(this.mTmpInvMatrix);
        this.mTmpInvMatrix.mapPoints(this.mTmpPoint);
        return this.mTmpPoint;
    }

    void updateDragViewTranslationDuringDrag() {
        float x = ((this.mLastMotionX - this.mDownMotionX) + getScrollX()) - this.mDownScrollX;
        float y = this.mLastMotionY - this.mDownMotionY;
        this.mDragView.setTranslationX(x);
        this.mDragView.setTranslationY(y);
    }

    public void setMinScale(float f) {
        this.mMinScale = f;
        requestLayout();
    }

    @Override
    public void setScaleX(float scaleX) {
        super.setScaleX(scaleX);
        if (isReordering(true)) {
            float[] p = mapPointFromParentToView(this, this.mParentDownMotionX, this.mParentDownMotionY);
            this.mLastMotionX = p[0];
            this.mLastMotionY = p[1];
            updateDragViewTranslationDuringDrag();
        }
    }

    int getViewportWidth() {
        return this.mViewport.width();
    }

    int getViewportHeight() {
        return this.mViewport.height();
    }

    int getViewportOffsetX() {
        return (getMeasuredWidth() - getViewportWidth()) / 2;
    }

    int getViewportOffsetY() {
        return (getMeasuredHeight() - getViewportHeight()) / 2;
    }

    public void setPageSwitchListener(PageSwitchListener pageSwitchListener) {
        this.mPageSwitchListener = pageSwitchListener;
        if (this.mPageSwitchListener != null) {
            this.mPageSwitchListener.onPageSwitched(getPageAt(this.mCurrentPage), this.mCurrentPage);
        }
    }

    int getCurrentPage() {
        return this.mCurrentPage;
    }

    int getNextPage() {
        return this.mNextPage != -1 ? this.mNextPage : this.mCurrentPage;
    }

    int getPageCount() {
        return getChildCount();
    }

    View getPageAt(int index) {
        return getChildAt(index);
    }

    protected int indexToPage(int index) {
        return index;
    }

    protected void updateCurrentPageScroll() {
        int offset = getChildOffset(this.mCurrentPage);
        int relOffset = getRelativeChildOffset(this.mCurrentPage);
        int newX = offset - relOffset;
        scrollTo(newX, 0);
        this.mScroller.setFinalX(newX);
        this.mScroller.forceFinished(true);
    }

    void setCurrentPage(int currentPage) {
        notifyPageSwitching(currentPage);
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
        if (getChildCount() != 0) {
            this.mForceScreenScrolled = true;
            this.mCurrentPage = Math.max(0, Math.min(currentPage, getPageCount() - 1));
            updateCurrentPageScroll();
            updateScrollingIndicator();
            notifyPageSwitched();
            invalidate();
        }
    }

    public void setOnlyAllowEdgeSwipes(boolean enable) {
        this.mOnlyAllowEdgeSwipes = enable;
    }

    protected void notifyPageSwitching(int whichPage) {
        if (this.mPageSwitchListener != null) {
            this.mPageSwitchListener.onPageSwitching(getPageAt(whichPage), whichPage);
        }
    }

    protected void notifyPageSwitched() {
        if (this.mPageSwitchListener != null) {
            this.mPageSwitchListener.onPageSwitched(getPageAt(this.mCurrentPage), this.mCurrentPage);
        }
    }

    protected void pageBeginMoving() {
        if (!this.mIsPageMoving) {
            this.mIsPageMoving = true;
            if (isWarping()) {
                dispatchOnPageBeginWarp();
            }
            onPageBeginMoving();
        }
    }

    private void dispatchOnPageBeginWarp() {
        if (!this.mOnPageBeginWarpCalled) {
            onPageBeginWarp();
            this.mOnPageBeginWarpCalled = true;
        }
        this.mOnPageEndWarpCalled = false;
    }

    private void dispatchOnPageEndWarp() {
        if (!this.mOnPageEndWarpCalled) {
            onPageEndWarp();
            this.mOnPageEndWarpCalled = true;
        }
        this.mOnPageBeginWarpCalled = false;
    }

    protected void pageEndMoving() {
        if (this.mIsPageMoving) {
            this.mIsPageMoving = false;
            if (isWarping()) {
                dispatchOnPageEndWarp();
                this.mWarpPageExposed = false;
            }
            onPageEndMoving();
        }
    }

    protected boolean isPageMoving() {
        return this.mIsPageMoving;
    }

    protected void onPageBeginMoving() {
    }

    protected void onPageEndMoving() {
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener l) {
        this.mLongClickListener = l;
        int count = getPageCount();
        for (int i = 0; i < count; i++) {
            getPageAt(i).setOnLongClickListener(l);
        }
    }

    @Override
    public void scrollBy(int x, int y) {
        scrollTo(this.mUnboundedScrollX + x, getScrollY() + y);
    }

    @Override
    public void scrollTo(int x, int y) {
        this.mUnboundedScrollX = x;
        if (x < 0) {
            super.scrollTo(0, y);
            if (this.mAllowOverScroll) {
                overScroll(x);
            }
        } else if (x > this.mMaxScrollX) {
            super.scrollTo(this.mMaxScrollX, y);
            if (this.mAllowOverScroll) {
                overScroll(x - this.mMaxScrollX);
            }
        } else {
            this.mOverScrollX = x;
            super.scrollTo(x, y);
        }
        this.mTouchX = x;
        this.mSmoothingTime = System.nanoTime() / 1.0E9f;
        if (isReordering(true)) {
            float[] p = mapPointFromParentToView(this, this.mParentDownMotionX, this.mParentDownMotionY);
            this.mLastMotionX = p[0];
            this.mLastMotionY = p[1];
            updateDragViewTranslationDuringDrag();
        }
    }

    protected boolean computeScrollHelper() {
        if (this.mScroller.computeScrollOffset()) {
            if (getScrollX() != this.mScroller.getCurrX() || getScrollY() != this.mScroller.getCurrY() || this.mOverScrollX != this.mScroller.getCurrX()) {
                scrollTo(this.mScroller.getCurrX(), this.mScroller.getCurrY());
            }
            invalidate();
            return true;
        }
        if (this.mNextPage == -1) {
            return false;
        }
        this.mCurrentPage = Math.max(0, Math.min(this.mNextPage, getPageCount() - 1));
        this.mNextPage = -1;
        notifyPageSwitched();
        if (this.mTouchState == 0) {
            pageEndMoving();
        }
        onPostReorderingAnimationCompleted();
        return true;
    }

    @Override
    public void computeScroll() {
        computeScrollHelper();
    }

    protected boolean shouldSetTopAlignedPivotForWidget(int childIndex) {
        return this.mTopAlignPageWhenShrinkingForBouncer;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childWidthMode;
        int childHeightMode;
        if (!this.mIsDataReady || getChildCount() == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int maxSize = Math.max(dm.widthPixels, dm.heightPixels);
        int parentWidthSize = (int) (1.5f * maxSize);
        int scaledWidthSize = (int) (parentWidthSize / this.mMinScale);
        int scaledHeightSize = (int) (maxSize / this.mMinScale);
        this.mViewport.set(0, 0, widthSize, heightSize);
        if (widthMode == 0 || heightMode == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (widthSize <= 0 || heightSize <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int verticalPadding = getPaddingTop() + getPaddingBottom();
        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getPageAt(i);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp.width == -2) {
                childWidthMode = Integer.MIN_VALUE;
            } else {
                childWidthMode = 1073741824;
            }
            if (lp.height == -2) {
                childHeightMode = Integer.MIN_VALUE;
            } else {
                childHeightMode = 1073741824;
            }
            int childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(widthSize - horizontalPadding, childWidthMode);
            int childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(heightSize - verticalPadding, childHeightMode);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
        setMeasuredDimension(scaledWidthSize, scaledHeightSize);
        invalidateCachedOffsets();
        if (this.mChildCountOnLastMeasure != getChildCount() && !this.mDeferringForDelete) {
            setCurrentPage(this.mCurrentPage);
        }
        this.mChildCountOnLastMeasure = getChildCount();
        if (childCount > 0 && this.mPageSpacing == -1) {
            int offset = getRelativeChildOffset(0);
            int spacing = Math.max(offset, (widthSize - offset) - getChildAt(0).getMeasuredWidth());
            setPageSpacing(spacing);
        }
        updateScrollingIndicatorPosition();
        if (childCount > 0) {
            this.mMaxScrollX = getChildOffset(childCount - 1) - getRelativeChildOffset(childCount - 1);
        } else {
            this.mMaxScrollX = 0;
        }
    }

    public void setPageSpacing(int pageSpacing) {
        this.mPageSpacing = pageSpacing;
        invalidateCachedOffsets();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mIsDataReady && getChildCount() != 0) {
            int childCount = getChildCount();
            int offsetX = getViewportOffsetX();
            int offsetY = getViewportOffsetY();
            this.mViewport.offset(offsetX, offsetY);
            int childLeft = offsetX + getRelativeChildOffset(0);
            for (int i = 0; i < childCount; i++) {
                View child = getPageAt(i);
                int childTop = offsetY + getPaddingTop();
                if (child.getVisibility() != 8) {
                    int childWidth = getScaledMeasuredWidth(child);
                    int childHeight = child.getMeasuredHeight();
                    child.layout(childLeft, childTop, child.getMeasuredWidth() + childLeft, childTop + childHeight);
                    childLeft += this.mPageSpacing + childWidth;
                }
            }
            if (this.mFirstLayout && this.mCurrentPage >= 0 && this.mCurrentPage < getChildCount()) {
                setHorizontalScrollBarEnabled(false);
                updateCurrentPageScroll();
                setHorizontalScrollBarEnabled(true);
                this.mFirstLayout = false;
            }
        }
    }

    protected void screenScrolled(int screenCenter) {
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        this.mForceScreenScrolled = true;
        invalidate();
        invalidateCachedOffsets();
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        this.mForceScreenScrolled = true;
        invalidate();
        invalidateCachedOffsets();
    }

    protected void invalidateCachedOffsets() {
        int count = getChildCount();
        if (count == 0) {
            this.mChildOffsets = null;
            this.mChildRelativeOffsets = null;
            this.mChildOffsetsWithLayoutScale = null;
            return;
        }
        this.mChildOffsets = new int[count];
        this.mChildRelativeOffsets = new int[count];
        this.mChildOffsetsWithLayoutScale = new int[count];
        for (int i = 0; i < count; i++) {
            this.mChildOffsets[i] = -1;
            this.mChildRelativeOffsets[i] = -1;
            this.mChildOffsetsWithLayoutScale[i] = -1;
        }
    }

    protected int getChildOffset(int index) {
        if (index < 0 || index > getChildCount() - 1) {
            return 0;
        }
        int[] childOffsets = Float.compare(this.mLayoutScale, 1.0f) == 0 ? this.mChildOffsets : this.mChildOffsetsWithLayoutScale;
        if (childOffsets != null && childOffsets[index] != -1) {
            return childOffsets[index];
        }
        if (getChildCount() == 0) {
            return 0;
        }
        int offset = getRelativeChildOffset(0);
        for (int i = 0; i < index; i++) {
            offset += getScaledMeasuredWidth(getPageAt(i)) + this.mPageSpacing;
        }
        if (childOffsets != null) {
            childOffsets[index] = offset;
            return offset;
        }
        return offset;
    }

    protected int getRelativeChildOffset(int index) {
        if (index < 0 || index > getChildCount() - 1) {
            return 0;
        }
        if (this.mChildRelativeOffsets != null && this.mChildRelativeOffsets[index] != -1) {
            return this.mChildRelativeOffsets[index];
        }
        int padding = getPaddingLeft() + getPaddingRight();
        int offset = getPaddingLeft() + (((getViewportWidth() - padding) - getChildWidth(index)) / 2);
        if (this.mChildRelativeOffsets != null) {
            this.mChildRelativeOffsets[index] = offset;
            return offset;
        }
        return offset;
    }

    protected int getScaledMeasuredWidth(View child) {
        int measuredWidth = child.getMeasuredWidth();
        int minWidth = this.mMinimumWidth;
        int maxWidth = minWidth > measuredWidth ? minWidth : measuredWidth;
        return (int) ((maxWidth * this.mLayoutScale) + 0.5f);
    }

    void boundByReorderablePages(boolean isReordering, int[] range) {
    }

    protected void getVisiblePages(int[] range) {
        range[0] = 0;
        range[1] = getPageCount() - 1;
    }

    protected boolean shouldDrawChild(View child) {
        return child.getAlpha() > 0.0f;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int halfScreenSize = getViewportWidth() / 2;
        int screenCenter = this.mOverScrollX + halfScreenSize;
        if (screenCenter != this.mLastScreenCenter || this.mForceScreenScrolled) {
            this.mForceScreenScrolled = false;
            screenScrolled(screenCenter);
            this.mLastScreenCenter = screenCenter;
        }
        int pageCount = getChildCount();
        if (pageCount > 0) {
            getVisiblePages(this.mTempVisiblePagesRange);
            int leftScreen = this.mTempVisiblePagesRange[0];
            int rightScreen = this.mTempVisiblePagesRange[1];
            if (leftScreen != -1 && rightScreen != -1) {
                long drawingTime = getDrawingTime();
                canvas.save();
                canvas.clipRect(getScrollX(), getScrollY(), (getScrollX() + getRight()) - getLeft(), (getScrollY() + getBottom()) - getTop());
                for (int i = pageCount - 1; i >= 0; i--) {
                    View v = getPageAt(i);
                    if (v != this.mDragView && (this.mForceDrawAllChildrenNextFrame || (leftScreen <= i && i <= rightScreen && shouldDrawChild(v)))) {
                        drawChild(canvas, v, drawingTime);
                    }
                }
                if (this.mDragView != null) {
                    drawChild(canvas, this.mDragView, drawingTime);
                }
                this.mForceDrawAllChildrenNextFrame = false;
                canvas.restore();
            }
        }
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        int page = indexToPage(indexOfChild(child));
        if (page == this.mCurrentPage && this.mScroller.isFinished()) {
            return false;
        }
        snapToPage(page);
        return true;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        int focusablePage;
        if (this.mNextPage != -1) {
            focusablePage = this.mNextPage;
        } else {
            focusablePage = this.mCurrentPage;
        }
        View v = getPageAt(focusablePage);
        if (v != null) {
            return v.requestFocus(direction, previouslyFocusedRect);
        }
        return false;
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (direction == 17) {
            if (getCurrentPage() > 0) {
                snapToPage(getCurrentPage() - 1);
                return true;
            }
        } else if (direction == 66 && getCurrentPage() < getPageCount() - 1) {
            snapToPage(getCurrentPage() + 1);
            return true;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (this.mCurrentPage >= 0 && this.mCurrentPage < getPageCount()) {
            getPageAt(this.mCurrentPage).addFocusables(views, direction, focusableMode);
        }
        if (direction == 17) {
            if (this.mCurrentPage > 0) {
                getPageAt(this.mCurrentPage - 1).addFocusables(views, direction, focusableMode);
            }
        } else if (direction == 66 && this.mCurrentPage < getPageCount() - 1) {
            getPageAt(this.mCurrentPage + 1).addFocusables(views, direction, focusableMode);
        }
    }

    @Override
    public void focusableViewAvailable(View focused) {
        View current = getPageAt(this.mCurrentPage);
        for (View v = focused; v != current; v = (View) v.getParent()) {
            if (v != this) {
                ViewParent parent = v.getParent();
                if (!(parent instanceof View)) {
                    return;
                }
            } else {
                return;
            }
        }
        super.focusableViewAvailable(focused);
    }

    private boolean isTouchPointInViewportWithBuffer(int x, int y) {
        this.mTmpRect.set(this.mViewport.left - (this.mViewport.width() / 2), this.mViewport.top, this.mViewport.right + (this.mViewport.width() / 2), this.mViewport.bottom);
        return this.mTmpRect.contains(x, y);
    }

    private boolean isTouchPointInCurrentPage(int x, int y) {
        View v = getPageAt(getCurrentPage());
        if (v == null) {
            return false;
        }
        this.mTmpRect.set(v.getLeft() - getScrollX(), 0, v.getRight() - getScrollX(), v.getBottom());
        return this.mTmpRect.contains(x, y);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        acquireVelocityTrackerAndAddMovement(ev);
        if (getChildCount() <= 0) {
            return super.onInterceptTouchEvent(ev);
        }
        int action = ev.getAction();
        if (action == 2 && this.mTouchState == 1) {
            return true;
        }
        switch (action & 255) {
            case 0:
                if (this.mIsCameraEvent) {
                    animateWarpPageOnScreen("interceptTouch(): DOWN");
                }
                saveDownState(ev);
                int xDist = Math.abs(this.mScroller.getFinalX() - this.mScroller.getCurrX());
                boolean finishedScrolling = this.mScroller.isFinished() || xDist < this.mTouchSlop;
                if (finishedScrolling) {
                    setTouchState(0);
                    this.mScroller.abortAnimation();
                } else if (this.mIsCameraEvent || isTouchPointInViewportWithBuffer((int) this.mDownMotionX, (int) this.mDownMotionY)) {
                    setTouchState(1);
                } else {
                    setTouchState(0);
                }
                break;
            case 1:
            case 3:
                resetTouchState();
                if (!isTouchPointInCurrentPage((int) this.mLastMotionX, (int) this.mLastMotionY)) {
                    return true;
                }
                break;
            case 2:
                if (this.mActivePointerId != -1 && (this.mIsCameraEvent || determineScrollingStart(ev))) {
                    startScrolling(ev);
                }
                break;
            case 6:
                onSecondaryPointerUp(ev);
                releaseVelocityTracker();
                break;
        }
        return this.mTouchState != 0;
    }

    private void setTouchState(int touchState) {
        if (this.mTouchState != touchState) {
            onTouchStateChanged(touchState);
            this.mTouchState = touchState;
        }
    }

    void onTouchStateChanged(int newTouchState) {
    }

    private void saveDownState(MotionEvent ev) {
        float x = ev.getX();
        this.mLastMotionX = x;
        this.mDownMotionX = x;
        float y = ev.getY();
        this.mLastMotionY = y;
        this.mDownMotionY = y;
        this.mDownScrollX = getScrollX();
        float[] p = mapPointFromViewToParent(this, this.mLastMotionX, this.mLastMotionY);
        this.mParentDownMotionX = p[0];
        this.mParentDownMotionY = p[1];
        this.mLastMotionXRemainder = 0.0f;
        this.mTotalMotionX = 0.0f;
        this.mActivePointerId = ev.getPointerId(0);
        int leftEdgeBoundary = getViewportOffsetX() + this.mEdgeSwipeRegionSize;
        int rightEdgeBoundary = (getMeasuredWidth() - getViewportOffsetX()) - this.mEdgeSwipeRegionSize;
        if (this.mDownMotionX <= leftEdgeBoundary || this.mDownMotionX >= rightEdgeBoundary) {
            this.mDownEventOnEdge = true;
        }
    }

    private boolean isHorizontalCameraScroll(MotionEvent ev) {
        boolean z = true;
        int pointerIndex = ev.findPointerIndex(this.mActivePointerId);
        if (pointerIndex == -1) {
            return false;
        }
        if (this.mOnlyAllowEdgeSwipes && !this.mDownEventOnEdge) {
            return false;
        }
        float x = ev.getX(pointerIndex);
        int xDiff = (int) Math.abs(x - this.mDownMotionX);
        int touchSlop = Math.round(1.0f * this.mTouchSlop);
        boolean xPaged = xDiff > this.mPagingTouchSlop;
        boolean xMoved = xDiff > touchSlop;
        if (!this.mIsCameraEvent || (!this.mUsePagingTouchSlop ? !xMoved : !xPaged)) {
            z = false;
        }
        return z;
    }

    protected boolean determineScrollingStart(MotionEvent ev) {
        int pointerIndex = ev.findPointerIndex(this.mActivePointerId);
        if (pointerIndex == -1) {
            return false;
        }
        float x = ev.getX(pointerIndex);
        float y = ev.getY(pointerIndex);
        if (!isTouchPointInViewportWithBuffer((int) x, (int) y)) {
            return false;
        }
        if (this.mOnlyAllowEdgeSwipes && !this.mDownEventOnEdge) {
            return false;
        }
        int xDiff = (int) Math.abs(x - this.mLastMotionX);
        int touchSlop = Math.round(1.0f * this.mTouchSlop);
        boolean xPaged = xDiff > this.mPagingTouchSlop;
        boolean xMoved = xDiff > touchSlop;
        if (!this.mUsePagingTouchSlop) {
            xPaged = xMoved;
        }
        return xPaged;
    }

    private void startScrolling(MotionEvent ev) {
        int pointerIndex = ev.findPointerIndex(this.mActivePointerId);
        if (pointerIndex != -1) {
            float x = ev.getX(pointerIndex);
            setTouchState(1);
            this.mTotalMotionX += Math.abs(this.mLastMotionX - x);
            this.mLastMotionX = x;
            this.mLastMotionXRemainder = 0.0f;
            this.mTouchX = getViewportOffsetX() + getScrollX();
            this.mSmoothingTime = System.nanoTime() / 1.0E9f;
            pageBeginMoving();
        }
    }

    protected float getMaxScrollProgress() {
        return 1.0f;
    }

    protected float getBoundedScrollProgress(int screenCenter, View v, int page) {
        int halfScreenSize = getViewportWidth() / 2;
        return getScrollProgress(Math.max(halfScreenSize, Math.min(this.mScrollX + halfScreenSize, screenCenter)), v, page);
    }

    protected float getScrollProgress(int screenCenter, View v, int page) {
        int halfScreenSize = getViewportWidth() / 2;
        int totalDistance = getScaledMeasuredWidth(v) + this.mPageSpacing;
        int delta = screenCenter - ((getChildOffset(page) - getRelativeChildOffset(page)) + halfScreenSize);
        float scrollProgress = delta / (totalDistance * 1.0f);
        return Math.max(Math.min(scrollProgress, getMaxScrollProgress()), -getMaxScrollProgress());
    }

    private float overScrollInfluenceCurve(float f) {
        float f2 = f - 1.0f;
        return (f2 * f2 * f2) + 1.0f;
    }

    protected void acceleratedOverScroll(float amount) {
        int screenSize = getViewportWidth();
        float f = 2.0f * (amount / screenSize);
        if (f != 0.0f) {
            if (Math.abs(f) >= 1.0f) {
                f /= Math.abs(f);
            }
            int overScrollAmount = Math.round(screenSize * f);
            if (amount < 0.0f) {
                this.mOverScrollX = overScrollAmount;
                super.scrollTo(0, getScrollY());
            } else {
                this.mOverScrollX = this.mMaxScrollX + overScrollAmount;
                super.scrollTo(this.mMaxScrollX, getScrollY());
            }
            invalidate();
        }
    }

    protected void dampedOverScroll(float amount) {
        int screenSize = getViewportWidth();
        float f = amount / screenSize;
        if (f != 0.0f) {
            float f2 = (f / Math.abs(f)) * overScrollInfluenceCurve(Math.abs(f));
            if (Math.abs(f2) >= 1.0f) {
                f2 /= Math.abs(f2);
            }
            int overScrollAmount = Math.round(0.14f * f2 * screenSize);
            if (amount < 0.0f) {
                this.mOverScrollX = overScrollAmount;
                super.scrollTo(0, getScrollY());
            } else {
                this.mOverScrollX = this.mMaxScrollX + overScrollAmount;
                super.scrollTo(this.mMaxScrollX, getScrollY());
            }
            invalidate();
        }
    }

    protected void overScroll(float amount) {
        dampedOverScroll(amount);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int finalPage;
        if (getChildCount() <= 0) {
            return super.onTouchEvent(ev);
        }
        acquireVelocityTrackerAndAddMovement(ev);
        int action = ev.getAction();
        switch (action & 255) {
            case 0:
                if (!this.mScroller.isFinished()) {
                    this.mScroller.abortAnimation();
                }
                saveDownState(ev);
                if (this.mTouchState == 1) {
                    pageBeginMoving();
                } else {
                    setTouchState(5);
                }
                if (this.mIsCameraEvent) {
                    animateWarpPageOnScreen("onTouch(): DOWN");
                }
                return true;
            case 1:
                if (this.mTouchState == 1) {
                    int activePointerId = this.mActivePointerId;
                    int pointerIndex = ev.findPointerIndex(activePointerId);
                    if (pointerIndex == -1) {
                        return true;
                    }
                    float x = ev.getX(pointerIndex);
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                    int velocityX = (int) velocityTracker.getXVelocity(activePointerId);
                    int deltaX = (int) (x - this.mDownMotionX);
                    int pageWidth = getScaledMeasuredWidth(getPageAt(this.mCurrentPage));
                    boolean isSignificantMove = ((float) Math.abs(deltaX)) > ((float) pageWidth) * 0.5f;
                    this.mTotalMotionX += Math.abs((this.mLastMotionX + this.mLastMotionXRemainder) - x);
                    boolean isFling = this.mTotalMotionX > 25.0f && Math.abs(velocityX) > this.mFlingThresholdVelocity;
                    boolean returnToOriginalPage = false;
                    if (Math.abs(deltaX) > pageWidth * 0.33f && Math.signum(velocityX) != Math.signum(deltaX) && isFling) {
                        returnToOriginalPage = true;
                    }
                    if (((isSignificantMove && deltaX > 0 && !isFling) || (isFling && velocityX > 0)) && this.mCurrentPage > 0) {
                        int finalPage2 = (returnToOriginalPage || isWarping()) ? this.mCurrentPage : this.mCurrentPage - 1;
                        snapToPageWithVelocity(finalPage2, velocityX);
                    } else if (((isSignificantMove && deltaX < 0 && !isFling) || (isFling && velocityX < 0)) && this.mCurrentPage < getChildCount() - 1) {
                        if (returnToOriginalPage) {
                            finalPage = this.mCurrentPage;
                        } else {
                            finalPage = isWarping() ? getPageWarpIndex() : this.mCurrentPage + 1;
                        }
                        snapToPageWithVelocity(finalPage, velocityX);
                    } else {
                        snapToDestination();
                    }
                } else if (this.mTouchState == 2) {
                    int nextPage = Math.max(0, this.mCurrentPage - 1);
                    if (nextPage != this.mCurrentPage) {
                        snapToPage(nextPage);
                    } else {
                        snapToDestination();
                    }
                } else if (this.mTouchState == 3) {
                    int nextPage2 = Math.min(getChildCount() - 1, this.mCurrentPage + 1);
                    if (nextPage2 != this.mCurrentPage) {
                        snapToPage(nextPage2);
                    } else {
                        snapToDestination();
                    }
                } else if (this.mTouchState == 4) {
                    this.mLastMotionX = ev.getX();
                    this.mLastMotionY = ev.getY();
                    float[] pt = mapPointFromViewToParent(this, this.mLastMotionX, this.mLastMotionY);
                    this.mParentDownMotionX = pt[0];
                    this.mParentDownMotionY = pt[1];
                    updateDragViewTranslationDuringDrag();
                    boolean handledFling = false;
                    PointF flingToDeleteVector = isFlingingToDelete();
                    if (flingToDeleteVector != null) {
                        onFlingToDelete(flingToDeleteVector);
                        handledFling = true;
                    }
                    if (!handledFling && isHoveringOverDeleteDropTarget((int) this.mParentDownMotionX, (int) this.mParentDownMotionY)) {
                        onDropToDelete();
                    }
                } else {
                    if (this.mWarpPageExposed && !isAnimatingWarpPage()) {
                        animateWarpPageOffScreen("unhandled tap", true);
                    }
                    onUnhandledTap(ev);
                }
                removeCallbacks(this.mSidePageHoverRunnable);
                resetTouchState();
                return true;
            case 2:
                if (this.mTouchState == 1) {
                    int pointerIndex2 = ev.findPointerIndex(this.mActivePointerId);
                    if (pointerIndex2 == -1) {
                        return true;
                    }
                    float x2 = ev.getX(pointerIndex2);
                    float deltaX2 = (this.mLastMotionX + this.mLastMotionXRemainder) - x2;
                    this.mTotalMotionX += Math.abs(deltaX2);
                    if (Math.abs(deltaX2) >= 1.0f) {
                        this.mTouchX += deltaX2;
                        this.mSmoothingTime = System.nanoTime() / 1.0E9f;
                        if (isWarping()) {
                            KeyguardWidgetFrame v = (KeyguardWidgetFrame) getPageAt(getPageWarpIndex());
                            v.setTranslationX(v.getTranslationX() - deltaX2);
                        } else if (!this.mDeferScrollUpdate) {
                            scrollBy((int) deltaX2, 0);
                        } else {
                            invalidate();
                        }
                        this.mLastMotionX = x2;
                        this.mLastMotionXRemainder = deltaX2 - ((int) deltaX2);
                    } else {
                        awakenScrollBars();
                    }
                } else if (this.mTouchState == 4) {
                    this.mLastMotionX = ev.getX();
                    this.mLastMotionY = ev.getY();
                    float[] pt2 = mapPointFromViewToParent(this, this.mLastMotionX, this.mLastMotionY);
                    this.mParentDownMotionX = pt2[0];
                    this.mParentDownMotionY = pt2[1];
                    updateDragViewTranslationDuringDrag();
                    final int dragViewIndex = indexOfChild(this.mDragView);
                    int bufferSize = (int) (this.REORDERING_SIDE_PAGE_BUFFER_PERCENTAGE * getViewportWidth());
                    int leftBufferEdge = (int) (mapPointFromViewToParent(this, this.mViewport.left, 0.0f)[0] + bufferSize);
                    int rightBufferEdge = (int) (mapPointFromViewToParent(this, this.mViewport.right, 0.0f)[0] - bufferSize);
                    boolean isHoveringOverDelete = isHoveringOverDeleteDropTarget((int) this.mParentDownMotionX, (int) this.mParentDownMotionY);
                    setPageHoveringOverDeleteDropTarget(dragViewIndex, isHoveringOverDelete);
                    float parentX = this.mParentDownMotionX;
                    int pageIndexToSnapTo = -1;
                    if (parentX < leftBufferEdge && dragViewIndex > 0) {
                        pageIndexToSnapTo = dragViewIndex - 1;
                    } else if (parentX > rightBufferEdge && dragViewIndex < getChildCount() - 1) {
                        pageIndexToSnapTo = dragViewIndex + 1;
                    }
                    final int pageUnderPointIndex = pageIndexToSnapTo;
                    if (pageUnderPointIndex > -1 && !isHoveringOverDelete) {
                        this.mTempVisiblePagesRange[0] = 0;
                        this.mTempVisiblePagesRange[1] = getPageCount() - 1;
                        boundByReorderablePages(true, this.mTempVisiblePagesRange);
                        if (this.mTempVisiblePagesRange[0] <= pageUnderPointIndex && pageUnderPointIndex <= this.mTempVisiblePagesRange[1] && pageUnderPointIndex != this.mSidePageHoverIndex && this.mScroller.isFinished()) {
                            this.mSidePageHoverIndex = pageUnderPointIndex;
                            this.mSidePageHoverRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    PagedView.this.mDownScrollX = PagedView.this.getChildOffset(pageUnderPointIndex) - PagedView.this.getRelativeChildOffset(pageUnderPointIndex);
                                    PagedView.this.snapToPage(pageUnderPointIndex);
                                    int shiftDelta = dragViewIndex < pageUnderPointIndex ? -1 : 1;
                                    int lowerIndex = dragViewIndex < pageUnderPointIndex ? dragViewIndex + 1 : pageUnderPointIndex;
                                    int upperIndex = dragViewIndex > pageUnderPointIndex ? dragViewIndex - 1 : pageUnderPointIndex;
                                    for (int i = lowerIndex; i <= upperIndex; i++) {
                                        View v2 = PagedView.this.getChildAt(i);
                                        int oldX = PagedView.this.getViewportOffsetX() + PagedView.this.getChildOffset(i);
                                        int newX = PagedView.this.getViewportOffsetX() + PagedView.this.getChildOffset(i + shiftDelta);
                                        AnimatorSet anim = (AnimatorSet) v2.getTag();
                                        if (anim != null) {
                                            anim.cancel();
                                        }
                                        v2.setTranslationX(oldX - newX);
                                        AnimatorSet anim2 = new AnimatorSet();
                                        anim2.setDuration(PagedView.this.REORDERING_REORDER_REPOSITION_DURATION);
                                        anim2.playTogether(ObjectAnimator.ofFloat(v2, "translationX", 0.0f));
                                        anim2.start();
                                        v2.setTag(anim2);
                                    }
                                    PagedView.this.removeView(PagedView.this.mDragView);
                                    PagedView.this.onRemoveView(PagedView.this.mDragView, false);
                                    PagedView.this.addView(PagedView.this.mDragView, pageUnderPointIndex);
                                    PagedView.this.onAddView(PagedView.this.mDragView, pageUnderPointIndex);
                                    PagedView.this.mSidePageHoverIndex = -1;
                                }
                            };
                            postDelayed(this.mSidePageHoverRunnable, this.REORDERING_SIDE_PAGE_HOVER_TIMEOUT);
                        }
                    } else {
                        removeCallbacks(this.mSidePageHoverRunnable);
                        this.mSidePageHoverIndex = -1;
                    }
                } else if (determineScrollingStart(ev)) {
                    startScrolling(ev);
                } else if (isHorizontalCameraScroll(ev)) {
                    startScrolling(ev);
                    ((KeyguardWidgetFrame) getPageAt(getPageWarpIndex())).animate().cancel();
                }
                return true;
            case 3:
                if (this.mTouchState == 1) {
                    snapToDestination();
                }
                resetTouchState();
                return true;
            case 4:
            case 5:
            default:
                return true;
            case 6:
                onSecondaryPointerUp(ev);
                return true;
        }
    }

    private void resetTouchState() {
        releaseVelocityTracker();
        endReordering();
        setTouchState(0);
        this.mActivePointerId = -1;
        this.mDownEventOnEdge = false;
    }

    protected void onUnhandledTap(MotionEvent ev) {
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        float vscroll;
        float hscroll;
        if ((event.getSource() & 2) != 0) {
            switch (event.getAction()) {
                case 8:
                    if ((event.getMetaState() & 1) != 0) {
                        vscroll = 0.0f;
                        hscroll = event.getAxisValue(9);
                    } else {
                        vscroll = -event.getAxisValue(9);
                        hscroll = event.getAxisValue(10);
                    }
                    if (hscroll != 0.0f || vscroll != 0.0f) {
                        if (hscroll > 0.0f || vscroll > 0.0f) {
                            scrollRight();
                        } else {
                            scrollLeft();
                        }
                        return true;
                    }
                    break;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = (ev.getAction() & 65280) >> 8;
        int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            float x = ev.getX(newPointerIndex);
            this.mDownMotionX = x;
            this.mLastMotionX = x;
            this.mLastMotionY = ev.getY(newPointerIndex);
            this.mLastMotionXRemainder = 0.0f;
            this.mActivePointerId = ev.getPointerId(newPointerIndex);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        int page = indexToPage(indexOfChild(child));
        if (page >= 0 && page != getCurrentPage() && !isInTouchMode()) {
            snapToPage(page);
        }
    }

    protected int getChildWidth(int index) {
        int measuredWidth = getPageAt(index).getMeasuredWidth();
        int minWidth = this.mMinimumWidth;
        return minWidth > measuredWidth ? minWidth : measuredWidth;
    }

    int getPageNearestToCenterOfScreen() {
        int minDistanceFromScreenCenter = Integer.MAX_VALUE;
        int minDistanceFromScreenCenterIndex = -1;
        int screenCenter = getViewportOffsetX() + getScrollX() + (getViewportWidth() / 2);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View layout = getPageAt(i);
            int childWidth = getScaledMeasuredWidth(layout);
            int halfChildWidth = childWidth / 2;
            int childCenter = getViewportOffsetX() + getChildOffset(i) + halfChildWidth;
            int distanceFromScreenCenter = Math.abs(childCenter - screenCenter);
            if (distanceFromScreenCenter < minDistanceFromScreenCenter) {
                minDistanceFromScreenCenter = distanceFromScreenCenter;
                minDistanceFromScreenCenterIndex = i;
            }
        }
        return minDistanceFromScreenCenterIndex;
    }

    protected void snapToDestination() {
        int newPage = getPageNearestToCenterOfScreen();
        if (isWarping()) {
            cancelWarpAnimation("snapToDestination", this.mCurrentPage != newPage);
        }
        snapToPage(newPage, getPageSnapDuration());
    }

    private int getPageSnapDuration() {
        return isWarping() ? 160 : 750;
    }

    private static class ScrollInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            return (t2 * t2 * t2 * t2 * t2) + 1.0f;
        }
    }

    float distanceInfluenceForSnapDuration(float f) {
        return (float) Math.sin((float) (((double) (f - 0.5f)) * 0.4712389167638204d));
    }

    protected void snapToPageWithVelocity(int whichPage, int velocity) {
        int whichPage2 = Math.max(0, Math.min(whichPage, getChildCount() - 1));
        int halfScreenSize = getViewportWidth() / 2;
        if (isWarping()) {
            cancelWarpAnimation("snapToPageWithVelocity", this.mCurrentPage != whichPage2);
        }
        int newX = getChildOffset(whichPage2) - getRelativeChildOffset(whichPage2);
        int delta = newX - this.mUnboundedScrollX;
        if (Math.abs(velocity) < this.mMinFlingVelocity) {
            snapToPage(whichPage2, getPageSnapDuration());
            return;
        }
        float distanceRatio = Math.min(1.0f, (Math.abs(delta) * 1.0f) / (halfScreenSize * 2));
        float distance = halfScreenSize + (halfScreenSize * distanceInfluenceForSnapDuration(distanceRatio));
        int duration = Math.round(1000.0f * Math.abs(distance / Math.max(this.mMinSnapVelocity, Math.abs(velocity)))) * 4;
        snapToPage(whichPage2, delta, duration);
    }

    protected void snapToPage(int whichPage) {
        snapToPage(whichPage, getPageSnapDuration());
    }

    protected void snapToPageImmediately(int whichPage) {
        snapToPage(whichPage, getPageSnapDuration(), true);
    }

    protected void snapToPage(int whichPage, int duration) {
        snapToPage(whichPage, duration, false);
    }

    protected void snapToPage(int whichPage, int duration, boolean immediate) {
        int whichPage2 = Math.max(0, Math.min(whichPage, getPageCount() - 1));
        int newX = getChildOffset(whichPage2) - getRelativeChildOffset(whichPage2);
        int sX = this.mUnboundedScrollX;
        int delta = newX - sX;
        snapToPage(whichPage2, delta, duration, immediate);
    }

    protected void snapToPage(int whichPage, int delta, int duration) {
        snapToPage(whichPage, delta, duration, false);
    }

    protected void snapToPage(int whichPage, int delta, int duration, boolean immediate) {
        if (isWarping() && whichPage == this.mCurrentPage + 1) {
            this.mNextPage = getPageWarpIndex();
        } else {
            this.mNextPage = whichPage;
        }
        if (this.mWarpPageExposed) {
            dispatchOnPageEndWarp();
            this.mWarpPageExposed = false;
        }
        notifyPageSwitching(whichPage);
        View focusedChild = getFocusedChild();
        if (focusedChild != null && whichPage != this.mCurrentPage && focusedChild == getPageAt(this.mCurrentPage)) {
            focusedChild.clearFocus();
        }
        pageBeginMoving();
        awakenScrollBars(duration);
        if (immediate) {
            duration = 0;
        } else if (duration == 0) {
            duration = Math.abs(delta);
        }
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
        this.mScroller.startScroll(this.mUnboundedScrollX, 0, delta, 0, duration);
        notifyPageSwitched();
        if (immediate) {
            computeScroll();
        }
        this.mForceScreenScrolled = true;
        invalidate();
    }

    protected boolean isWarping() {
        return this.mWarpPageExposed;
    }

    public void scrollLeft() {
        if (this.mScroller.isFinished()) {
            if (this.mCurrentPage > 0) {
                snapToPage(this.mCurrentPage - 1);
            }
        } else if (this.mNextPage > 0) {
            snapToPage(this.mNextPage - 1);
        }
    }

    public void scrollRight() {
        if (this.mScroller.isFinished()) {
            if (this.mCurrentPage < getChildCount() - 1) {
                snapToPage(this.mCurrentPage + 1);
            }
        } else if (this.mNextPage < getChildCount() - 1) {
            snapToPage(this.mNextPage + 1);
        }
    }

    public static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int currentPage;

        private SavedState(Parcel in) {
            super(in);
            this.currentPage = -1;
            this.currentPage = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.currentPage);
        }
    }

    protected View getScrollingIndicator() {
        return null;
    }

    protected boolean isScrollingIndicatorEnabled() {
        return false;
    }

    protected void showScrollingIndicator(boolean immediately) {
        this.mShouldShowScrollIndicator = true;
        this.mShouldShowScrollIndicatorImmediately = true;
        if (getChildCount() > 1 && isScrollingIndicatorEnabled()) {
            this.mShouldShowScrollIndicator = false;
            getScrollingIndicator();
            if (this.mScrollIndicator != null) {
                updateScrollingIndicatorPosition();
                this.mScrollIndicator.setVisibility(0);
                cancelScrollingIndicatorAnimations();
                if (immediately) {
                    this.mScrollIndicator.setAlpha(1.0f);
                    return;
                }
                this.mScrollIndicatorAnimator = ObjectAnimator.ofFloat(this.mScrollIndicator, "alpha", 1.0f);
                this.mScrollIndicatorAnimator.setDuration(150L);
                this.mScrollIndicatorAnimator.start();
            }
        }
    }

    protected void cancelScrollingIndicatorAnimations() {
        if (this.mScrollIndicatorAnimator != null) {
            this.mScrollIndicatorAnimator.cancel();
        }
    }

    protected void hideScrollingIndicator(boolean immediately) {
        if (getChildCount() > 1 && isScrollingIndicatorEnabled()) {
            getScrollingIndicator();
            if (this.mScrollIndicator != null) {
                updateScrollingIndicatorPosition();
                cancelScrollingIndicatorAnimations();
                if (immediately) {
                    this.mScrollIndicator.setVisibility(4);
                    this.mScrollIndicator.setAlpha(0.0f);
                } else {
                    this.mScrollIndicatorAnimator = ObjectAnimator.ofFloat(this.mScrollIndicator, "alpha", 0.0f);
                    this.mScrollIndicatorAnimator.setDuration(650L);
                    this.mScrollIndicatorAnimator.addListener(new AnimatorListenerAdapter() {
                        private boolean cancelled = false;

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            this.cancelled = true;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!this.cancelled) {
                                PagedView.this.mScrollIndicator.setVisibility(4);
                            }
                        }
                    });
                    this.mScrollIndicatorAnimator.start();
                }
            }
        }
    }

    protected boolean hasElasticScrollIndicator() {
        return true;
    }

    private void updateScrollingIndicator() {
        if (getChildCount() > 1 && isScrollingIndicatorEnabled()) {
            getScrollingIndicator();
            if (this.mScrollIndicator != null) {
                updateScrollingIndicatorPosition();
            }
            if (this.mShouldShowScrollIndicator) {
                showScrollingIndicator(this.mShouldShowScrollIndicatorImmediately);
            }
        }
    }

    private void updateScrollingIndicatorPosition() {
        if (isScrollingIndicatorEnabled() && this.mScrollIndicator != null) {
            int numPages = getChildCount();
            int pageWidth = getViewportWidth();
            int lastChildIndex = Math.max(0, getChildCount() - 1);
            int maxScrollX = getChildOffset(lastChildIndex) - getRelativeChildOffset(lastChildIndex);
            int trackWidth = (pageWidth - this.mScrollIndicatorPaddingLeft) - this.mScrollIndicatorPaddingRight;
            int indicatorWidth = (this.mScrollIndicator.getMeasuredWidth() - this.mScrollIndicator.getPaddingLeft()) - this.mScrollIndicator.getPaddingRight();
            float offset = Math.max(0.0f, Math.min(1.0f, getScrollX() / maxScrollX));
            int indicatorSpace = trackWidth / numPages;
            int indicatorPos = ((int) ((trackWidth - indicatorSpace) * offset)) + this.mScrollIndicatorPaddingLeft;
            if (hasElasticScrollIndicator()) {
                if (this.mScrollIndicator.getMeasuredWidth() != indicatorSpace) {
                    this.mScrollIndicator.getLayoutParams().width = indicatorSpace;
                    this.mScrollIndicator.requestLayout();
                }
            } else {
                int indicatorCenterOffset = (indicatorSpace / 2) - (indicatorWidth / 2);
                indicatorPos += indicatorCenterOffset;
            }
            this.mScrollIndicator.setTranslationX(indicatorPos);
        }
    }

    void animateDragViewToOriginalPosition() {
        if (this.mDragView != null) {
            AnimatorSet anim = new AnimatorSet();
            anim.setDuration(this.REORDERING_DROP_REPOSITION_DURATION);
            anim.playTogether(ObjectAnimator.ofFloat(this.mDragView, "translationX", 0.0f), ObjectAnimator.ofFloat(this.mDragView, "translationY", 0.0f));
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    PagedView.this.onPostReorderingAnimationCompleted();
                }
            });
            anim.start();
        }
    }

    protected boolean zoomOut() {
        if (this.mZoomInOutAnim != null && this.mZoomInOutAnim.isRunning()) {
            this.mZoomInOutAnim.cancel();
        }
        if (getScaleX() < 1.0f || getScaleY() < 1.0f) {
            return false;
        }
        this.mZoomInOutAnim = new AnimatorSet();
        this.mZoomInOutAnim.setDuration(this.REORDERING_ZOOM_IN_OUT_DURATION);
        this.mZoomInOutAnim.playTogether(ObjectAnimator.ofFloat(this, "scaleX", this.mMinScale), ObjectAnimator.ofFloat(this, "scaleY", this.mMinScale));
        this.mZoomInOutAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (PagedView.this.mDeleteDropTarget != null) {
                    PagedView.this.mDeleteDropTarget.setVisibility(0);
                    PagedView.this.mDeleteDropTarget.animate().alpha(1.0f).setDuration(PagedView.this.REORDERING_DELETE_DROP_TARGET_FADE_DURATION).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation2) {
                            PagedView.this.mDeleteDropTarget.setAlpha(0.0f);
                        }
                    });
                }
            }
        });
        this.mZoomInOutAnim.start();
        return true;
    }

    protected void onStartReordering() {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            announceForAccessibility(this.mContext.getString(R.string.keyguard_accessibility_widget_reorder_start));
        }
        setTouchState(4);
        this.mIsReordering = true;
        getVisiblePages(this.mTempVisiblePagesRange);
        boundByReorderablePages(true, this.mTempVisiblePagesRange);
        for (int i = 0; i < getPageCount(); i++) {
            if (i < this.mTempVisiblePagesRange[0] || i > this.mTempVisiblePagesRange[1]) {
                getPageAt(i).setAlpha(0.0f);
            }
        }
        invalidate();
    }

    private void onPostReorderingAnimationCompleted() {
        this.mPostReorderingPreZoomInRemainingAnimationCount--;
        if (this.mPostReorderingPreZoomInRunnable != null && this.mPostReorderingPreZoomInRemainingAnimationCount == 0) {
            this.mPostReorderingPreZoomInRunnable.run();
            this.mPostReorderingPreZoomInRunnable = null;
        }
    }

    protected void onEndReordering() {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            if (this.mDeleteString != null) {
                announceForAccessibility(this.mDeleteString);
                this.mDeleteString = null;
            } else {
                announceForAccessibility(this.mContext.getString(R.string.keyguard_accessibility_widget_reorder_end));
            }
        }
        this.mIsReordering = false;
        getVisiblePages(this.mTempVisiblePagesRange);
        boundByReorderablePages(true, this.mTempVisiblePagesRange);
        for (int i = 0; i < getPageCount(); i++) {
            if (i < this.mTempVisiblePagesRange[0] || i > this.mTempVisiblePagesRange[1]) {
                getPageAt(i).setAlpha(1.0f);
            }
        }
    }

    public boolean startReordering() {
        int dragViewIndex = getPageNearestToCenterOfScreen();
        this.mTempVisiblePagesRange[0] = 0;
        this.mTempVisiblePagesRange[1] = getPageCount() - 1;
        boundByReorderablePages(true, this.mTempVisiblePagesRange);
        if (this.mTempVisiblePagesRange[0] > dragViewIndex || dragViewIndex > this.mTempVisiblePagesRange[1]) {
            return false;
        }
        this.mReorderingStarted = true;
        if (!zoomOut()) {
            return true;
        }
        this.mDragView = getChildAt(dragViewIndex);
        onStartReordering();
        return true;
    }

    boolean isReordering(boolean testTouchState) {
        boolean state = this.mIsReordering;
        if (testTouchState) {
            return state & (this.mTouchState == 4);
        }
        return state;
    }

    void endReordering() {
        if (this.mReorderingStarted) {
            this.mReorderingStarted = false;
            final Runnable onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    PagedView.this.onEndReordering();
                }
            };
            if (!this.mDeferringForDelete) {
                this.mPostReorderingPreZoomInRunnable = new Runnable() {
                    @Override
                    public void run() {
                        PagedView.this.zoomIn(onCompleteRunnable);
                    }
                };
                this.mPostReorderingPreZoomInRemainingAnimationCount = this.NUM_ANIMATIONS_RUNNING_BEFORE_ZOOM_OUT;
                snapToPage(indexOfChild(this.mDragView), 0);
                animateDragViewToOriginalPosition();
            }
        }
    }

    protected boolean zoomIn(final Runnable onCompleteRunnable) {
        if (this.mZoomInOutAnim != null && this.mZoomInOutAnim.isRunning()) {
            this.mZoomInOutAnim.cancel();
        }
        if (getScaleX() < 1.0f || getScaleY() < 1.0f) {
            this.mZoomInOutAnim = new AnimatorSet();
            this.mZoomInOutAnim.setDuration(this.REORDERING_ZOOM_IN_OUT_DURATION);
            this.mZoomInOutAnim.playTogether(ObjectAnimator.ofFloat(this, "scaleX", 1.0f), ObjectAnimator.ofFloat(this, "scaleY", 1.0f));
            this.mZoomInOutAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (PagedView.this.mDeleteDropTarget != null) {
                        PagedView.this.mDeleteDropTarget.animate().alpha(0.0f).setDuration(PagedView.this.REORDERING_DELETE_DROP_TARGET_FADE_DURATION).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation2) {
                                PagedView.this.mDeleteDropTarget.setVisibility(8);
                            }
                        });
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    PagedView.this.mDragView = null;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    PagedView.this.mDragView = null;
                    if (onCompleteRunnable != null) {
                        onCompleteRunnable.run();
                    }
                }
            });
            this.mZoomInOutAnim.start();
            return true;
        }
        if (onCompleteRunnable == null) {
            return false;
        }
        onCompleteRunnable.run();
        return false;
    }

    private PointF isFlingingToDelete() {
        ViewConfiguration config = ViewConfiguration.get(getContext());
        this.mVelocityTracker.computeCurrentVelocity(1000, config.getScaledMaximumFlingVelocity());
        if (this.mVelocityTracker.getYVelocity() < this.mFlingToDeleteThresholdVelocity) {
            PointF vel = new PointF(this.mVelocityTracker.getXVelocity(), this.mVelocityTracker.getYVelocity());
            PointF upVec = new PointF(0.0f, -1.0f);
            float theta = (float) Math.acos(((vel.x * upVec.x) + (vel.y * upVec.y)) / (vel.length() * upVec.length()));
            if (theta <= Math.toRadians(this.FLING_TO_DELETE_MAX_FLING_DEGREES)) {
                return vel;
            }
        }
        return null;
    }

    private static class FlingAlongVectorAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private final TimeInterpolator mAlphaInterpolator = new DecelerateInterpolator(0.75f);
        private View mDragView;
        private float mFriction;
        private Rect mFrom;
        private long mPrevTime;
        private PointF mVelocity;

        public FlingAlongVectorAnimatorUpdateListener(View dragView, PointF vel, Rect from, long startTime, float friction) {
            this.mDragView = dragView;
            this.mVelocity = vel;
            this.mFrom = from;
            this.mPrevTime = startTime;
            this.mFriction = 1.0f - (this.mDragView.getResources().getDisplayMetrics().density * friction);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float t = ((Float) animation.getAnimatedValue()).floatValue();
            long curTime = AnimationUtils.currentAnimationTimeMillis();
            this.mFrom.left = (int) (r3.left + ((this.mVelocity.x * (curTime - this.mPrevTime)) / 1000.0f));
            this.mFrom.top = (int) (r3.top + ((this.mVelocity.y * (curTime - this.mPrevTime)) / 1000.0f));
            this.mDragView.setTranslationX(this.mFrom.left);
            this.mDragView.setTranslationY(this.mFrom.top);
            this.mDragView.setAlpha(1.0f - this.mAlphaInterpolator.getInterpolation(t));
            this.mVelocity.x *= this.mFriction;
            this.mVelocity.y *= this.mFriction;
            this.mPrevTime = curTime;
        }
    }

    class AnonymousClass9 implements Runnable {
        final View val$dragView;

        AnonymousClass9(View view) {
            this.val$dragView = view;
        }

        @Override
        public void run() {
            int oldX;
            int newX;
            int dragViewIndex = PagedView.this.indexOfChild(this.val$dragView);
            PagedView.this.getVisiblePages(PagedView.this.mTempVisiblePagesRange);
            PagedView.this.boundByReorderablePages(true, PagedView.this.mTempVisiblePagesRange);
            boolean isLastWidgetPage = PagedView.this.mTempVisiblePagesRange[0] == PagedView.this.mTempVisiblePagesRange[1];
            boolean slideFromLeft = isLastWidgetPage || dragViewIndex > PagedView.this.mTempVisiblePagesRange[0];
            if (slideFromLeft) {
                PagedView.this.snapToPageImmediately(dragViewIndex - 1);
            }
            int firstIndex = isLastWidgetPage ? 0 : PagedView.this.mTempVisiblePagesRange[0];
            int lastIndex = Math.min(PagedView.this.mTempVisiblePagesRange[1], PagedView.this.getPageCount() - 1);
            int lowerIndex = slideFromLeft ? firstIndex : dragViewIndex + 1;
            int upperIndex = slideFromLeft ? dragViewIndex - 1 : lastIndex;
            ArrayList<Animator> animations = new ArrayList<>();
            for (int i = lowerIndex; i <= upperIndex; i++) {
                View v = PagedView.this.getChildAt(i);
                if (slideFromLeft) {
                    if (i == 0) {
                        oldX = ((PagedView.this.getViewportOffsetX() + PagedView.this.getChildOffset(i)) - PagedView.this.getChildWidth(i)) - PagedView.this.mPageSpacing;
                    } else {
                        oldX = PagedView.this.getViewportOffsetX() + PagedView.this.getChildOffset(i - 1);
                    }
                    newX = PagedView.this.getViewportOffsetX() + PagedView.this.getChildOffset(i);
                } else {
                    oldX = PagedView.this.getChildOffset(i) - PagedView.this.getChildOffset(i - 1);
                    newX = 0;
                }
                AnimatorSet anim = (AnimatorSet) v.getTag();
                if (anim != null) {
                    anim.cancel();
                }
                v.setAlpha(Math.max(v.getAlpha(), 0.01f));
                v.setTranslationX(oldX - newX);
                AnimatorSet anim2 = new AnimatorSet();
                anim2.playTogether(ObjectAnimator.ofFloat(v, "translationX", 0.0f), ObjectAnimator.ofFloat(v, "alpha", 1.0f));
                animations.add(anim2);
                v.setTag(anim2);
            }
            AnimatorSet slideAnimations = new AnimatorSet();
            slideAnimations.playTogether(animations);
            slideAnimations.setDuration(PagedView.this.DELETE_SLIDE_IN_SIDE_PAGE_DURATION);
            slideAnimations.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Runnable onCompleteRunnable = new Runnable() {
                        @Override
                        public void run() {
                            PagedView.this.mDeferringForDelete = false;
                            PagedView.this.onEndReordering();
                            PagedView.this.onRemoveViewAnimationCompleted();
                        }
                    };
                    PagedView.this.zoomIn(onCompleteRunnable);
                }
            });
            slideAnimations.start();
            PagedView.this.removeView(this.val$dragView);
            PagedView.this.onRemoveView(this.val$dragView, true);
        }
    }

    private Runnable createPostDeleteAnimationRunnable(View dragView) {
        return new AnonymousClass9(dragView);
    }

    public void onFlingToDelete(PointF vel) {
        final long startTime = AnimationUtils.currentAnimationTimeMillis();
        TimeInterpolator tInterpolator = new TimeInterpolator() {
            private int mCount = -1;
            private float mOffset;
            private long mStartTime;

            {
                this.mStartTime = startTime;
            }

            @Override
            public float getInterpolation(float t) {
                if (this.mCount < 0) {
                    this.mCount++;
                } else if (this.mCount == 0) {
                    this.mOffset = Math.min(0.5f, (AnimationUtils.currentAnimationTimeMillis() - this.mStartTime) / PagedView.this.FLING_TO_DELETE_FADE_OUT_DURATION);
                    this.mCount++;
                }
                return Math.min(1.0f, this.mOffset + t);
            }
        };
        Rect from = new Rect();
        View dragView = this.mDragView;
        from.left = (int) dragView.getTranslationX();
        from.top = (int) dragView.getTranslationY();
        ValueAnimator.AnimatorUpdateListener updateCb = new FlingAlongVectorAnimatorUpdateListener(dragView, vel, from, startTime, this.FLING_TO_DELETE_FRICTION);
        this.mDeleteString = getContext().getResources().getString(R.string.keyguard_accessibility_widget_deleted, this.mDragView.getContentDescription());
        final Runnable onAnimationEndRunnable = createPostDeleteAnimationRunnable(dragView);
        ValueAnimator mDropAnim = new ValueAnimator();
        mDropAnim.setInterpolator(tInterpolator);
        mDropAnim.setDuration(this.FLING_TO_DELETE_FADE_OUT_DURATION);
        mDropAnim.setFloatValues(0.0f, 1.0f);
        mDropAnim.addUpdateListener(updateCb);
        mDropAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onAnimationEndRunnable.run();
            }
        });
        mDropAnim.start();
        this.mDeferringForDelete = true;
    }

    private boolean isHoveringOverDeleteDropTarget(int x, int y) {
        if (this.mDeleteDropTarget == null) {
            return false;
        }
        this.mAltTmpRect.set(0, 0, 0, 0);
        View parent = (View) this.mDeleteDropTarget.getParent();
        if (parent != null) {
            parent.getGlobalVisibleRect(this.mAltTmpRect);
        }
        this.mDeleteDropTarget.getGlobalVisibleRect(this.mTmpRect);
        this.mTmpRect.offset(-this.mAltTmpRect.left, -this.mAltTmpRect.top);
        return this.mTmpRect.contains(x, y);
    }

    protected void setPageHoveringOverDeleteDropTarget(int viewIndex, boolean isHovering) {
    }

    private void onDropToDelete() {
        View dragView = this.mDragView;
        ArrayList<Animator> animations = new ArrayList<>();
        AnimatorSet motionAnim = new AnimatorSet();
        motionAnim.setInterpolator(new DecelerateInterpolator(2.0f));
        motionAnim.playTogether(ObjectAnimator.ofFloat(dragView, "scaleX", 0.0f), ObjectAnimator.ofFloat(dragView, "scaleY", 0.0f));
        animations.add(motionAnim);
        AnimatorSet alphaAnim = new AnimatorSet();
        alphaAnim.setInterpolator(new LinearInterpolator());
        alphaAnim.playTogether(ObjectAnimator.ofFloat(dragView, "alpha", 0.0f));
        animations.add(alphaAnim);
        this.mDeleteString = getContext().getResources().getString(R.string.keyguard_accessibility_widget_deleted, this.mDragView.getContentDescription());
        final Runnable onAnimationEndRunnable = createPostDeleteAnimationRunnable(dragView);
        AnimatorSet anim = new AnimatorSet();
        anim.playTogether(animations);
        anim.setDuration(this.DRAG_TO_DELETE_FADE_OUT_DURATION);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onAnimationEndRunnable.run();
            }
        });
        anim.start();
        this.mDeferringForDelete = true;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setScrollable(getPageCount() > 1);
        if (getCurrentPage() < getPageCount() - 1) {
            info.addAction(4096);
        }
        if (getCurrentPage() > 0) {
            info.addAction(8192);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setScrollable(true);
        if (event.getEventType() == 4096) {
            event.setFromIndex(this.mCurrentPage);
            event.setToIndex(this.mCurrentPage);
            event.setItemCount(getChildCount());
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (super.performAccessibilityAction(action, arguments)) {
            return true;
        }
        switch (action) {
            case 4096:
                if (getCurrentPage() < getPageCount() - 1) {
                    scrollRight();
                    return true;
                }
                return false;
            case 8192:
                if (getCurrentPage() > 0) {
                    scrollLeft();
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        return true;
    }

    private void cancelWarpAnimation(String msg, boolean abortAnimation) {
        if (abortAnimation) {
            KeyguardWidgetFrame v = (KeyguardWidgetFrame) getPageAt(getPageWarpIndex());
            v.animate().cancel();
            scrollBy(Math.round(-v.getTranslationX()), 0);
            v.setTranslationX(0.0f);
            return;
        }
        animateWarpPageOffScreen("canceled", true);
    }

    private boolean isAnimatingWarpPage() {
        return this.mWarpAnimation != null;
    }

    private void animateWarpPageOnScreen(String reason) {
        if (!this.mWarpPageExposed) {
            this.mWarpPageExposed = true;
            dispatchOnPageBeginWarp();
            KeyguardWidgetFrame v = (KeyguardWidgetFrame) getPageAt(getPageWarpIndex());
            DecelerateInterpolator interp = new DecelerateInterpolator(1.5f);
            int totalOffset = getCurrentWarpOffset();
            v.setTranslationX(totalOffset);
            this.mWarpAnimation = v.animate();
            this.mWarpAnimation.translationX(this.mWarpPeekAmount + totalOffset).setInterpolator(interp).setDuration(150L).setListener(this.mOnScreenAnimationListener);
        }
    }

    private int getCurrentWarpOffset() {
        View viewRight;
        View warpView;
        if (this.mCurrentPage == getPageWarpIndex() || (viewRight = getPageAt(this.mCurrentPage + 1)) == (warpView = getPageAt(getPageWarpIndex())) || viewRight == null || warpView == null) {
            return 0;
        }
        return viewRight.getLeft() - warpView.getLeft();
    }

    private void animateWarpPageOffScreen(String reason, boolean animate) {
        if (this.mWarpPageExposed) {
            dispatchOnPageEndWarp();
            KeyguardWidgetFrame v = (KeyguardWidgetFrame) getPageAt(getPageWarpIndex());
            AccelerateInterpolator interp = new AccelerateInterpolator(1.5f);
            int totalOffset = getCurrentWarpOffset();
            v.animate().translationX(totalOffset).setInterpolator(interp).setDuration(animate ? 150L : 0L).setListener(this.mOffScreenAnimationListener);
        }
    }

    protected int getPageWarpIndex() {
        return getPageCount() - 1;
    }

    public void onPageBeginWarp() {
    }

    public void onPageEndWarp() {
    }
}
