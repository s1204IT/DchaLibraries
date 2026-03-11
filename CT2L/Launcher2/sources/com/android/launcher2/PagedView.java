package com.android.launcher2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import com.android.launcher.R;
import java.util.ArrayList;

public abstract class PagedView extends ViewGroup implements ViewGroup.OnHierarchyChangeListener {
    Runnable hideScrollingIndicatorRunnable;
    protected int mActivePointerId;
    protected boolean mAllowLongPress;
    protected boolean mAllowOverScroll;
    protected int mCellCountX;
    protected int mCellCountY;
    protected boolean mCenterPagesVertically;
    private int[] mChildOffsets;
    private int[] mChildOffsetsWithLayoutScale;
    private int[] mChildRelativeOffsets;
    protected boolean mContentIsRefreshable;
    protected int mCurrentPage;
    private boolean mDeferLoadAssociatedPagesUntilScrollCompletes;
    protected boolean mDeferScrollUpdate;
    protected float mDensity;
    protected ArrayList<Boolean> mDirtyPageContent;
    private float mDownMotionX;
    protected boolean mFadeInAdjacentScreens;
    protected boolean mFirstLayout;
    protected int mFlingThresholdVelocity;
    protected boolean mForceDrawAllChildrenNextFrame;
    protected boolean mForceScreenScrolled;
    private boolean mHasScrollIndicator;
    protected boolean mIsDataReady;
    protected boolean mIsPageMoving;
    protected float mLastMotionX;
    protected float mLastMotionXRemainder;
    protected float mLastMotionY;
    private int mLastScreenCenter;
    protected float mLayoutScale;
    protected View.OnLongClickListener mLongClickListener;
    protected int mMaxScrollX;
    private int mMaximumVelocity;
    protected int mMinFlingVelocity;
    protected int mMinSnapVelocity;
    private int mMinimumWidth;
    protected int mNextPage;
    protected int mOverScrollX;
    protected int mPageLayoutHeightGap;
    protected int mPageLayoutPaddingBottom;
    protected int mPageLayoutPaddingLeft;
    protected int mPageLayoutPaddingRight;
    protected int mPageLayoutPaddingTop;
    protected int mPageLayoutWidthGap;
    protected int mPageSpacing;
    private PageSwitchListener mPageSwitchListener;
    private int mPagingTouchSlop;
    private View mScrollIndicator;
    private ValueAnimator mScrollIndicatorAnimator;
    private int mScrollIndicatorPaddingLeft;
    private int mScrollIndicatorPaddingRight;
    protected Scroller mScroller;
    private boolean mScrollingPaused;
    private boolean mShouldShowScrollIndicator;
    private boolean mShouldShowScrollIndicatorImmediately;
    protected float mSmoothingTime;
    protected int[] mTempVisiblePagesRange;
    protected float mTotalMotionX;
    protected int mTouchSlop;
    protected int mTouchState;
    protected float mTouchX;
    protected int mUnboundedScrollX;
    protected boolean mUsePagingTouchSlop;
    private VelocityTracker mVelocityTracker;

    public interface PageSwitchListener {
        void onPageSwitch(View view, int i);
    }

    public abstract void syncPageItems(int i, boolean z);

    public abstract void syncPages();

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
        this.mAllowLongPress = true;
        this.mCellCountX = 0;
        this.mCellCountY = 0;
        this.mAllowOverScroll = true;
        this.mTempVisiblePagesRange = new int[2];
        this.mLayoutScale = 1.0f;
        this.mActivePointerId = -1;
        this.mContentIsRefreshable = true;
        this.mFadeInAdjacentScreens = true;
        this.mUsePagingTouchSlop = true;
        this.mDeferScrollUpdate = false;
        this.mIsPageMoving = false;
        this.mIsDataReady = false;
        this.mHasScrollIndicator = true;
        this.mShouldShowScrollIndicator = false;
        this.mShouldShowScrollIndicatorImmediately = false;
        this.mScrollingPaused = false;
        this.hideScrollingIndicatorRunnable = new Runnable() {
            @Override
            public void run() {
                PagedView.this.hideScrollingIndicator(false);
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagedView, defStyle, 0);
        setPageSpacing(a.getDimensionPixelSize(6, 0));
        this.mPageLayoutPaddingTop = a.getDimensionPixelSize(2, 0);
        this.mPageLayoutPaddingBottom = a.getDimensionPixelSize(3, 0);
        this.mPageLayoutPaddingLeft = a.getDimensionPixelSize(4, 0);
        this.mPageLayoutPaddingRight = a.getDimensionPixelSize(5, 0);
        this.mPageLayoutWidthGap = a.getDimensionPixelSize(0, 0);
        this.mPageLayoutHeightGap = a.getDimensionPixelSize(1, 0);
        this.mScrollIndicatorPaddingLeft = a.getDimensionPixelSize(7, 0);
        this.mScrollIndicatorPaddingRight = a.getDimensionPixelSize(8, 0);
        a.recycle();
        setHapticFeedbackEnabled(false);
        init();
    }

    protected void init() {
        this.mDirtyPageContent = new ArrayList<>();
        this.mDirtyPageContent.ensureCapacity(32);
        this.mScroller = new Scroller(getContext(), new ScrollInterpolator());
        this.mCurrentPage = 0;
        this.mCenterPagesVertically = true;
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mPagingTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mDensity = getResources().getDisplayMetrics().density;
        this.mFlingThresholdVelocity = (int) (500.0f * this.mDensity);
        this.mMinFlingVelocity = (int) (250.0f * this.mDensity);
        this.mMinSnapVelocity = (int) (1500.0f * this.mDensity);
        setOnHierarchyChangeListener(this);
    }

    public boolean isLayoutRtl() {
        return getLayoutDirection() == 1;
    }

    protected void setDataIsReady() {
        this.mIsDataReady = true;
    }

    protected boolean isDataReady() {
        return this.mIsDataReady;
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
        int newX = 0;
        if (this.mCurrentPage >= 0 && this.mCurrentPage < getPageCount()) {
            int offset = getChildOffset(this.mCurrentPage);
            int relOffset = getRelativeChildOffset(this.mCurrentPage);
            newX = offset - relOffset;
        }
        scrollTo(newX, 0);
        this.mScroller.setFinalX(newX);
        this.mScroller.forceFinished(true);
    }

    void pauseScrolling() {
        this.mScroller.forceFinished(true);
        cancelScrollingIndicatorAnimations();
        this.mScrollingPaused = true;
    }

    void resumeScrolling() {
        this.mScrollingPaused = false;
    }

    void setCurrentPage(int currentPage) {
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
        if (getChildCount() != 0) {
            this.mCurrentPage = Math.max(0, Math.min(currentPage, getPageCount() - 1));
            updateCurrentPageScroll();
            updateScrollingIndicator();
            notifyPageSwitchListener();
            invalidate();
        }
    }

    protected void notifyPageSwitchListener() {
        if (this.mPageSwitchListener != null) {
            this.mPageSwitchListener.onPageSwitch(getPageAt(this.mCurrentPage), this.mCurrentPage);
        }
    }

    protected void pageBeginMoving() {
        if (!this.mIsPageMoving) {
            this.mIsPageMoving = true;
            onPageBeginMoving();
        }
    }

    protected void pageEndMoving() {
        if (this.mIsPageMoving) {
            this.mIsPageMoving = false;
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
        boolean isXAfterLastPage = true;
        boolean isRtl = isLayoutRtl();
        this.mUnboundedScrollX = x;
        boolean isXBeforeFirstPage = isRtl ? x > this.mMaxScrollX : x < 0;
        if (isRtl) {
            if (x >= 0) {
                isXAfterLastPage = false;
            }
        } else if (x <= this.mMaxScrollX) {
            isXAfterLastPage = false;
        }
        if (isXBeforeFirstPage) {
            super.scrollTo(0, y);
            if (this.mAllowOverScroll) {
                if (isRtl) {
                    overScroll(x - this.mMaxScrollX);
                } else {
                    overScroll(x);
                }
            }
        } else if (isXAfterLastPage) {
            super.scrollTo(this.mMaxScrollX, y);
            if (this.mAllowOverScroll) {
                if (isRtl) {
                    overScroll(x);
                } else {
                    overScroll(x - this.mMaxScrollX);
                }
            }
        } else {
            this.mOverScrollX = x;
            super.scrollTo(x, y);
        }
        this.mTouchX = x;
        this.mSmoothingTime = System.nanoTime() / 1.0E9f;
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
        notifyPageSwitchListener();
        if (this.mDeferLoadAssociatedPagesUntilScrollCompletes) {
            loadAssociatedPages(this.mCurrentPage);
            this.mDeferLoadAssociatedPagesUntilScrollCompletes = false;
        }
        if (this.mTouchState == 0) {
            pageEndMoving();
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService("accessibility");
        if (!accessibilityManager.isEnabled()) {
            return true;
        }
        AccessibilityEvent ev = AccessibilityEvent.obtain(4096);
        ev.getText().add(getCurrentPageDescription());
        sendAccessibilityEventUnchecked(ev);
        return true;
    }

    @Override
    public void computeScroll() {
        computeScrollHelper();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childWidthMode;
        int childHeightMode;
        if (!this.mIsDataReady) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode != 1073741824) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }
        if (widthSize <= 0 || heightSize <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int maxChildHeight = 0;
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
            maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
        }
        if (heightMode == Integer.MIN_VALUE) {
            heightSize = maxChildHeight + verticalPadding;
        }
        setMeasuredDimension(widthSize, heightSize);
        invalidateCachedOffsets();
        if (childCount > 0 && this.mPageSpacing == -1) {
            int offset = getRelativeChildOffset(0);
            int spacing = Math.max(offset, (widthSize - offset) - getChildAt(0).getMeasuredWidth());
            setPageSpacing(spacing);
        }
        updateScrollingIndicatorPosition();
        if (childCount > 0) {
            int index = isLayoutRtl() ? 0 : childCount - 1;
            this.mMaxScrollX = getChildOffset(index) - getRelativeChildOffset(index);
        } else {
            this.mMaxScrollX = 0;
        }
    }

    protected void scrollToNewPageWithoutMovingPages(int newCurrentPage) {
        int newX = getChildOffset(newCurrentPage) - getRelativeChildOffset(newCurrentPage);
        int delta = newX - getScrollX();
        int pageCount = getChildCount();
        for (int i = 0; i < pageCount; i++) {
            View page = getPageAt(i);
            page.setX(page.getX() + delta);
        }
        setCurrentPage(newCurrentPage);
    }

    public void setLayoutScale(float childrenScale) {
        this.mLayoutScale = childrenScale;
        invalidateCachedOffsets();
        int childCount = getChildCount();
        float[] childrenX = new float[childCount];
        float[] childrenY = new float[childCount];
        for (int i = 0; i < childCount; i++) {
            View child = getPageAt(i);
            childrenX[i] = child.getX();
            childrenY[i] = child.getY();
        }
        int widthSpec = View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(getMeasuredHeight(), 1073741824);
        requestLayout();
        measure(widthSpec, heightSpec);
        layout(getLeft(), getTop(), getRight(), getBottom());
        for (int i2 = 0; i2 < childCount; i2++) {
            View child2 = getPageAt(i2);
            child2.setX(childrenX[i2]);
            child2.setY(childrenY[i2]);
        }
        scrollToNewPageWithoutMovingPages(this.mCurrentPage);
    }

    public void setPageSpacing(int pageSpacing) {
        this.mPageSpacing = pageSpacing;
        invalidateCachedOffsets();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mIsDataReady) {
            int verticalPadding = getPaddingTop() + getPaddingBottom();
            int childCount = getChildCount();
            boolean isRtl = isLayoutRtl();
            int startIndex = isRtl ? childCount - 1 : 0;
            int endIndex = isRtl ? -1 : childCount;
            int delta = isRtl ? -1 : 1;
            int childLeft = getRelativeChildOffset(startIndex);
            for (int i = startIndex; i != endIndex; i += delta) {
                View child = getPageAt(i);
                if (child.getVisibility() != 8) {
                    int childWidth = getScaledMeasuredWidth(child);
                    int childHeight = child.getMeasuredHeight();
                    int childTop = getPaddingTop();
                    if (this.mCenterPagesVertically) {
                        childTop += ((getMeasuredHeight() - verticalPadding) - childHeight) / 2;
                    }
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
        if (isScrollingIndicatorEnabled()) {
            updateScrollingIndicator();
        }
        boolean isInOverscroll = this.mOverScrollX < 0 || this.mOverScrollX > this.mMaxScrollX;
        if (this.mFadeInAdjacentScreens && !isInOverscroll) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child != null) {
                    float scrollProgress = getScrollProgress(screenCenter, child, i);
                    float alpha = 1.0f - Math.abs(scrollProgress);
                    child.setAlpha(alpha);
                }
            }
            invalidate();
        }
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        this.mForceScreenScrolled = true;
        invalidate();
        invalidateCachedOffsets();
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
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
        boolean isRtl = isLayoutRtl();
        int[] childOffsets = Float.compare(this.mLayoutScale, 1.0f) == 0 ? this.mChildOffsets : this.mChildOffsetsWithLayoutScale;
        if (childOffsets != null && childOffsets[index] != -1) {
            return childOffsets[index];
        }
        if (getChildCount() == 0) {
            return 0;
        }
        int startIndex = isRtl ? getChildCount() - 1 : 0;
        int endIndex = isRtl ? index : index;
        int delta = isRtl ? -1 : 1;
        int offset = getRelativeChildOffset(startIndex);
        for (int i = startIndex; i != endIndex; i += delta) {
            offset += getScaledMeasuredWidth(getPageAt(i)) + this.mPageSpacing;
        }
        if (childOffsets != null) {
            childOffsets[index] = offset;
            return offset;
        }
        return offset;
    }

    protected int getRelativeChildOffset(int index) {
        if (this.mChildRelativeOffsets != null && this.mChildRelativeOffsets[index] != -1) {
            return this.mChildRelativeOffsets[index];
        }
        int padding = getPaddingLeft() + getPaddingRight();
        int offset = getPaddingLeft() + (((getMeasuredWidth() - padding) - getChildWidth(index)) / 2);
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

    protected void getVisiblePages(int[] range) {
        boolean isRtl = isLayoutRtl();
        int pageCount = getChildCount();
        if (pageCount > 0) {
            int screenWidth = getMeasuredWidth();
            int leftScreen = isRtl ? pageCount - 1 : 0;
            int endIndex = isRtl ? 0 : pageCount - 1;
            int delta = isRtl ? -1 : 1;
            View currPage = getPageAt(leftScreen);
            while (leftScreen != endIndex && (currPage.getX() + currPage.getWidth()) - currPage.getPaddingRight() < getScrollX()) {
                leftScreen += delta;
                currPage = getPageAt(leftScreen);
            }
            int rightScreen = leftScreen;
            View currPage2 = getPageAt(rightScreen + delta);
            while (rightScreen != endIndex && currPage2.getX() - currPage2.getPaddingLeft() < getScrollX() + screenWidth) {
                rightScreen += delta;
                currPage2 = getPageAt(rightScreen + delta);
            }
            range[0] = Math.min(leftScreen, rightScreen);
            range[1] = Math.max(leftScreen, rightScreen);
            return;
        }
        range[0] = -1;
        range[1] = -1;
    }

    protected boolean shouldDrawChild(View child) {
        return child.getAlpha() > 0.0f;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int halfScreenSize = getMeasuredWidth() / 2;
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
                for (int i = getChildCount() - 1; i >= 0; i--) {
                    View v = getPageAt(i);
                    if (this.mForceDrawAllChildrenNextFrame || (leftScreen <= i && i <= rightScreen && shouldDrawChild(v))) {
                        drawChild(canvas, v, drawingTime);
                    }
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

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            View currentPage = getPageAt(this.mCurrentPage);
            currentPage.cancelLongPress();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    protected boolean hitsPreviousPage(float x, float y) {
        return isLayoutRtl() ? x > ((float) ((getMeasuredWidth() - getRelativeChildOffset(this.mCurrentPage)) + this.mPageSpacing)) : x < ((float) (getRelativeChildOffset(this.mCurrentPage) - this.mPageSpacing));
    }

    protected boolean hitsNextPage(float x, float y) {
        return isLayoutRtl() ? x < ((float) (getRelativeChildOffset(this.mCurrentPage) - this.mPageSpacing)) : x > ((float) ((getMeasuredWidth() - getRelativeChildOffset(this.mCurrentPage)) + this.mPageSpacing));
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
                float x = ev.getX();
                float y = ev.getY();
                this.mDownMotionX = x;
                this.mLastMotionX = x;
                this.mLastMotionY = y;
                this.mLastMotionXRemainder = 0.0f;
                this.mTotalMotionX = 0.0f;
                this.mActivePointerId = ev.getPointerId(0);
                this.mAllowLongPress = true;
                int xDist = Math.abs(this.mScroller.getFinalX() - this.mScroller.getCurrX());
                boolean finishedScrolling = this.mScroller.isFinished() || xDist < this.mTouchSlop;
                if (finishedScrolling) {
                    this.mTouchState = 0;
                    this.mScroller.abortAnimation();
                } else {
                    this.mTouchState = 1;
                }
                if (this.mTouchState != 2 && this.mTouchState != 3 && getChildCount() > 0) {
                    if (hitsPreviousPage(x, y)) {
                        this.mTouchState = 2;
                    } else if (hitsNextPage(x, y)) {
                        this.mTouchState = 3;
                    }
                }
                break;
            case 1:
            case 3:
                this.mTouchState = 0;
                this.mAllowLongPress = false;
                this.mActivePointerId = -1;
                releaseVelocityTracker();
                break;
            case 2:
                if (this.mActivePointerId != -1) {
                    determineScrollingStart(ev);
                    break;
                }
                break;
            case 6:
                onSecondaryPointerUp(ev);
                releaseVelocityTracker();
                break;
        }
        return this.mTouchState != 0;
    }

    protected void determineScrollingStart(MotionEvent ev) {
        determineScrollingStart(ev, 1.0f);
    }

    protected void determineScrollingStart(MotionEvent ev, float touchSlopScale) {
        int pointerIndex = ev.findPointerIndex(this.mActivePointerId);
        if (pointerIndex != -1) {
            float x = ev.getX(pointerIndex);
            float y = ev.getY(pointerIndex);
            int xDiff = (int) Math.abs(x - this.mLastMotionX);
            int yDiff = (int) Math.abs(y - this.mLastMotionY);
            int touchSlop = Math.round(this.mTouchSlop * touchSlopScale);
            boolean xPaged = xDiff > this.mPagingTouchSlop;
            boolean xMoved = xDiff > touchSlop;
            boolean yMoved = yDiff > touchSlop;
            if (xMoved || xPaged || yMoved) {
                if (!this.mUsePagingTouchSlop ? xMoved : xPaged) {
                    this.mTouchState = 1;
                    this.mTotalMotionX += Math.abs(this.mLastMotionX - x);
                    this.mLastMotionX = x;
                    this.mLastMotionXRemainder = 0.0f;
                    this.mTouchX = getScrollX();
                    this.mSmoothingTime = System.nanoTime() / 1.0E9f;
                    pageBeginMoving();
                }
                cancelCurrentPageLongPress();
            }
        }
    }

    protected void cancelCurrentPageLongPress() {
        if (this.mAllowLongPress) {
            this.mAllowLongPress = false;
            View currentPage = getPageAt(this.mCurrentPage);
            if (currentPage != null) {
                currentPage.cancelLongPress();
            }
        }
    }

    protected float getScrollProgress(int screenCenter, View v, int page) {
        int halfScreenSize = getMeasuredWidth() / 2;
        int totalDistance = getScaledMeasuredWidth(v) + this.mPageSpacing;
        int delta = screenCenter - ((getChildOffset(page) - getRelativeChildOffset(page)) + halfScreenSize);
        float scrollProgress = delta / (totalDistance * 1.0f);
        return Math.max(Math.min(scrollProgress, 1.0f), -1.0f);
    }

    private float overScrollInfluenceCurve(float f) {
        float f2 = f - 1.0f;
        return (f2 * f2 * f2) + 1.0f;
    }

    protected void acceleratedOverScroll(float amount) {
        int screenSize = getMeasuredWidth();
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
        int screenSize = getMeasuredWidth();
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
        boolean isDeltaXLeft;
        boolean isVelocityXLeft;
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
                float x = ev.getX();
                this.mLastMotionX = x;
                this.mDownMotionX = x;
                this.mLastMotionXRemainder = 0.0f;
                this.mTotalMotionX = 0.0f;
                this.mActivePointerId = ev.getPointerId(0);
                if (this.mTouchState == 1) {
                    pageBeginMoving();
                }
                return true;
            case 1:
                if (this.mTouchState == 1) {
                    int activePointerId = this.mActivePointerId;
                    int pointerIndex = ev.findPointerIndex(activePointerId);
                    float x2 = ev.getX(pointerIndex);
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                    int velocityX = (int) velocityTracker.getXVelocity(activePointerId);
                    int deltaX = (int) (x2 - this.mDownMotionX);
                    int pageWidth = getScaledMeasuredWidth(getPageAt(this.mCurrentPage));
                    boolean isSignificantMove = ((float) Math.abs(deltaX)) > ((float) pageWidth) * 0.4f;
                    this.mTotalMotionX += Math.abs((this.mLastMotionX + this.mLastMotionXRemainder) - x2);
                    boolean isFling = this.mTotalMotionX > 25.0f && Math.abs(velocityX) > this.mFlingThresholdVelocity;
                    boolean returnToOriginalPage = false;
                    if (Math.abs(deltaX) > pageWidth * 0.33f && Math.signum(velocityX) != Math.signum(deltaX) && isFling) {
                        returnToOriginalPage = true;
                    }
                    boolean isRtl = isLayoutRtl();
                    if (isRtl) {
                        isDeltaXLeft = deltaX > 0;
                    } else {
                        isDeltaXLeft = deltaX < 0;
                    }
                    if (isRtl) {
                        isVelocityXLeft = velocityX > 0;
                    } else {
                        isVelocityXLeft = velocityX < 0;
                    }
                    if (((isSignificantMove && !isDeltaXLeft && !isFling) || (isFling && !isVelocityXLeft)) && this.mCurrentPage > 0) {
                        int finalPage = returnToOriginalPage ? this.mCurrentPage : this.mCurrentPage - 1;
                        snapToPageWithVelocity(finalPage, velocityX);
                    } else if (((isSignificantMove && isDeltaXLeft && !isFling) || (isFling && isVelocityXLeft)) && this.mCurrentPage < getChildCount() - 1) {
                        int finalPage2 = returnToOriginalPage ? this.mCurrentPage : this.mCurrentPage + 1;
                        snapToPageWithVelocity(finalPage2, velocityX);
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
                } else {
                    onUnhandledTap(ev);
                }
                this.mTouchState = 0;
                this.mActivePointerId = -1;
                releaseVelocityTracker();
                return true;
            case 2:
                if (this.mTouchState == 1) {
                    int pointerIndex2 = ev.findPointerIndex(this.mActivePointerId);
                    float x3 = ev.getX(pointerIndex2);
                    float deltaX2 = (this.mLastMotionX + this.mLastMotionXRemainder) - x3;
                    this.mTotalMotionX += Math.abs(deltaX2);
                    if (Math.abs(deltaX2) >= 1.0f) {
                        this.mTouchX += deltaX2;
                        this.mSmoothingTime = System.nanoTime() / 1.0E9f;
                        if (!this.mDeferScrollUpdate) {
                            scrollBy((int) deltaX2, 0);
                        } else {
                            invalidate();
                        }
                        this.mLastMotionX = x3;
                        this.mLastMotionXRemainder = deltaX2 - ((int) deltaX2);
                    } else {
                        awakenScrollBars();
                    }
                } else {
                    determineScrollingStart(ev);
                }
                return true;
            case 3:
                if (this.mTouchState == 1) {
                    snapToDestination();
                }
                this.mTouchState = 0;
                this.mActivePointerId = -1;
                releaseVelocityTracker();
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

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        float vscroll;
        float hscroll;
        boolean isForwardScroll = false;
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
                        if (isLayoutRtl()) {
                            if (hscroll < 0.0f || vscroll < 0.0f) {
                                isForwardScroll = true;
                            }
                        } else if (hscroll > 0.0f || vscroll > 0.0f) {
                            isForwardScroll = true;
                        }
                        if (isForwardScroll) {
                            scrollRight();
                            return true;
                        }
                        scrollLeft();
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

    protected void onUnhandledTap(MotionEvent ev) {
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
        int screenCenter = getScrollX() + (getMeasuredWidth() / 2);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View layout = getPageAt(i);
            int childWidth = getScaledMeasuredWidth(layout);
            int halfChildWidth = childWidth / 2;
            int childCenter = getChildOffset(i) + halfChildWidth;
            int distanceFromScreenCenter = Math.abs(childCenter - screenCenter);
            if (distanceFromScreenCenter < minDistanceFromScreenCenter) {
                minDistanceFromScreenCenter = distanceFromScreenCenter;
                minDistanceFromScreenCenterIndex = i;
            }
        }
        return minDistanceFromScreenCenterIndex;
    }

    protected void snapToDestination() {
        snapToPage(getPageNearestToCenterOfScreen(), 550);
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
        int halfScreenSize = getMeasuredWidth() / 2;
        int newX = getChildOffset(whichPage2) - getRelativeChildOffset(whichPage2);
        int delta = newX - this.mUnboundedScrollX;
        if (Math.abs(velocity) < this.mMinFlingVelocity) {
            snapToPage(whichPage2, 550);
            return;
        }
        float distanceRatio = Math.min(1.0f, (Math.abs(delta) * 1.0f) / (halfScreenSize * 2));
        float distance = halfScreenSize + (halfScreenSize * distanceInfluenceForSnapDuration(distanceRatio));
        int duration = Math.round(1000.0f * Math.abs(distance / Math.max(this.mMinSnapVelocity, Math.abs(velocity)))) * 4;
        snapToPage(whichPage2, delta, Math.min(duration, 750));
    }

    protected void snapToPage(int whichPage) {
        snapToPage(whichPage, 550);
    }

    protected void snapToPage(int whichPage, int duration) {
        int whichPage2 = Math.max(0, Math.min(whichPage, getPageCount() - 1));
        int newX = getChildOffset(whichPage2) - getRelativeChildOffset(whichPage2);
        int sX = this.mUnboundedScrollX;
        int delta = newX - sX;
        snapToPage(whichPage2, delta, duration);
    }

    protected void snapToPage(int whichPage, int delta, int duration) {
        this.mNextPage = whichPage;
        View focusedChild = getFocusedChild();
        if (focusedChild != null && whichPage != this.mCurrentPage && focusedChild == getPageAt(this.mCurrentPage)) {
            focusedChild.clearFocus();
        }
        pageBeginMoving();
        awakenScrollBars(duration);
        if (duration == 0) {
            duration = Math.abs(delta);
        }
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
        this.mScroller.startScroll(this.mUnboundedScrollX, 0, delta, 0, duration);
        if (this.mDeferScrollUpdate) {
            loadAssociatedPages(this.mNextPage);
        } else {
            this.mDeferLoadAssociatedPagesUntilScrollCompletes = true;
        }
        notifyPageSwitchListener();
        invalidate();
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

    public int getPageForView(View v) {
        if (v != null) {
            ViewParent vp = v.getParent();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                if (vp == getPageAt(i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean allowLongPress() {
        return this.mAllowLongPress;
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

    protected void loadAssociatedPages(int page) {
        loadAssociatedPages(page, false);
    }

    protected void loadAssociatedPages(int page, boolean immediateAndOnly) {
        int count;
        if (this.mContentIsRefreshable && page < (count = getChildCount())) {
            int lowerPageBound = getAssociatedLowerPageBound(page);
            int upperPageBound = getAssociatedUpperPageBound(page);
            for (int i = 0; i < count; i++) {
                Page layout = (Page) getPageAt(i);
                if (i < lowerPageBound || i > upperPageBound) {
                    if (layout.getPageChildCount() > 0) {
                        layout.removeAllViewsOnPage();
                    }
                    this.mDirtyPageContent.set(i, true);
                }
            }
            int i2 = 0;
            while (i2 < count) {
                if ((i2 == page || !immediateAndOnly) && lowerPageBound <= i2 && i2 <= upperPageBound && this.mDirtyPageContent.get(i2).booleanValue()) {
                    syncPageItems(i2, i2 == page && immediateAndOnly);
                    this.mDirtyPageContent.set(i2, false);
                }
                i2++;
            }
        }
    }

    protected int getAssociatedLowerPageBound(int page) {
        return Math.max(0, page - 1);
    }

    protected int getAssociatedUpperPageBound(int page) {
        int count = getChildCount();
        return Math.min(page + 1, count - 1);
    }

    protected void invalidatePageData() {
        invalidatePageData(-1, false);
    }

    protected void invalidatePageData(int currentPage) {
        invalidatePageData(currentPage, false);
    }

    protected void invalidatePageData(int currentPage, boolean immediateAndOnly) {
        if (this.mIsDataReady && this.mContentIsRefreshable) {
            this.mScroller.forceFinished(true);
            this.mNextPage = -1;
            syncPages();
            measure(View.MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(getMeasuredHeight(), 1073741824));
            if (currentPage > -1) {
                setCurrentPage(Math.min(getPageCount() - 1, currentPage));
            }
            int count = getChildCount();
            this.mDirtyPageContent.clear();
            for (int i = 0; i < count; i++) {
                this.mDirtyPageContent.add(true);
            }
            loadAssociatedPages(this.mCurrentPage, immediateAndOnly);
            requestLayout();
        }
    }

    protected View getScrollingIndicator() {
        ViewGroup parent;
        if (this.mHasScrollIndicator && this.mScrollIndicator == null && (parent = (ViewGroup) getParent()) != null) {
            this.mScrollIndicator = parent.findViewById(R.id.paged_view_indicator);
            this.mHasScrollIndicator = this.mScrollIndicator != null;
            if (this.mHasScrollIndicator) {
                this.mScrollIndicator.setVisibility(0);
            }
        }
        return this.mScrollIndicator;
    }

    protected boolean isScrollingIndicatorEnabled() {
        return true;
    }

    protected void flashScrollingIndicator(boolean animated) {
        removeCallbacks(this.hideScrollingIndicatorRunnable);
        showScrollingIndicator(!animated);
        postDelayed(this.hideScrollingIndicatorRunnable, 650L);
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
                if (immediately || this.mScrollingPaused) {
                    this.mScrollIndicator.setAlpha(1.0f);
                    return;
                }
                this.mScrollIndicatorAnimator = LauncherAnimUtils.ofFloat(this.mScrollIndicator, "alpha", 1.0f);
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
                if (immediately || this.mScrollingPaused) {
                    this.mScrollIndicator.setVisibility(4);
                    this.mScrollIndicator.setAlpha(0.0f);
                } else {
                    this.mScrollIndicatorAnimator = LauncherAnimUtils.ofFloat(this.mScrollIndicator, "alpha", 0.0f);
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
        boolean isRtl = isLayoutRtl();
        if (isScrollingIndicatorEnabled() && this.mScrollIndicator != null) {
            int numPages = getChildCount();
            int pageWidth = getMeasuredWidth();
            int trackWidth = (pageWidth - this.mScrollIndicatorPaddingLeft) - this.mScrollIndicatorPaddingRight;
            int indicatorWidth = (this.mScrollIndicator.getMeasuredWidth() - this.mScrollIndicator.getPaddingLeft()) - this.mScrollIndicator.getPaddingRight();
            float scrollPos = isRtl ? this.mMaxScrollX - getScrollX() : getScrollX();
            float offset = Math.max(0.0f, Math.min(1.0f, scrollPos / this.mMaxScrollX));
            if (isRtl) {
                offset = 1.0f - offset;
            }
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

    protected String getCurrentPageDescription() {
        return String.format(getContext().getString(R.string.default_scroll_format), Integer.valueOf(getNextPage() + 1), Integer.valueOf(getChildCount()));
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        return true;
    }
}
