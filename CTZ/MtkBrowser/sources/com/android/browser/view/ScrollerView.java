package com.android.browser.view;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.StrictMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.OverScroller;
import java.util.ArrayList;

public class ScrollerView extends FrameLayout {
    private int mActivePointerId;
    protected View mChildToScrollTo;
    private PointF mDownCoords;
    private View mDownView;

    @ViewDebug.ExportedProperty(category = "layout")
    private boolean mFillViewport;
    private StrictMode.Span mFlingStrictSpan;
    protected boolean mHorizontal;
    protected boolean mIsBeingDragged;
    private boolean mIsLayoutDirty;
    protected boolean mIsOrthoDragged;
    private float mLastMotionY;
    private float mLastOrthoCoord;
    private long mLastScroll;
    private int mMaximumVelocity;
    protected int mMinimumVelocity;
    private int mOverflingDistance;
    private int mOverscrollDistance;
    private StrictMode.Span mScrollStrictSpan;
    protected OverScroller mScroller;
    private boolean mSmoothScrollingEnabled;
    private final Rect mTempRect;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;

    public ScrollerView(Context context) {
        this(context, null);
    }

    public ScrollerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.scrollViewStyle);
    }

    public ScrollerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mTempRect = new Rect();
        this.mIsLayoutDirty = true;
        this.mChildToScrollTo = null;
        this.mIsBeingDragged = false;
        this.mSmoothScrollingEnabled = true;
        this.mActivePointerId = -1;
        this.mScrollStrictSpan = null;
        this.mFlingStrictSpan = null;
        initScrollView();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, com.android.internal.R.styleable.ScrollView, i, 0);
        setFillViewport(typedArrayObtainStyledAttributes.getBoolean(0, false));
        typedArrayObtainStyledAttributes.recycle();
    }

    private boolean canScroll() {
        View childAt = getChildAt(0);
        if (childAt == null) {
            return false;
        }
        if (this.mHorizontal) {
            if (getWidth() < childAt.getWidth() + this.mPaddingLeft + this.mPaddingRight) {
                return true;
            }
        } else if (getHeight() < childAt.getHeight() + this.mPaddingTop + this.mPaddingBottom) {
            return true;
        }
        return false;
    }

    private int clamp(int i, int i2, int i3) {
        if (i2 >= i3 || i < 0) {
            return 0;
        }
        return i2 + i > i3 ? i3 - i2 : i;
    }

    private int computeScrollDeltaToGetChildRectOnScreenHorizontal(Rect rect) {
        int iMax;
        if (getChildCount() == 0) {
            return 0;
        }
        int width = getWidth();
        int scrollX = getScrollX();
        int i = scrollX + width;
        int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength();
        if (rect.left > 0) {
            scrollX += horizontalFadingEdgeLength;
        }
        if (rect.right < getChildAt(0).getWidth()) {
            i -= horizontalFadingEdgeLength;
        }
        if (rect.right > i && rect.left > scrollX) {
            iMax = Math.min(rect.width() > width ? (rect.left - scrollX) + 0 : (rect.right - i) + 0, getChildAt(0).getRight() - i);
        } else if (rect.left >= scrollX || rect.right >= i) {
            iMax = 0;
        } else {
            iMax = Math.max(rect.width() > width ? 0 - (i - rect.right) : 0 - (scrollX - rect.left), -getScrollX());
        }
        return iMax;
    }

    private int computeScrollDeltaToGetChildRectOnScreenVertical(Rect rect) {
        int iMax;
        if (getChildCount() == 0) {
            return 0;
        }
        int height = getHeight();
        int scrollY = getScrollY();
        int i = scrollY + height;
        int verticalFadingEdgeLength = getVerticalFadingEdgeLength();
        if (rect.top > 0) {
            scrollY += verticalFadingEdgeLength;
        }
        if (rect.bottom < getChildAt(0).getHeight()) {
            i -= verticalFadingEdgeLength;
        }
        if (rect.bottom > i && rect.top > scrollY) {
            iMax = Math.min(rect.height() > height ? (rect.top - scrollY) + 0 : (rect.bottom - i) + 0, getChildAt(0).getBottom() - i);
        } else if (rect.top >= scrollY || rect.bottom >= i) {
            iMax = 0;
        } else {
            iMax = Math.max(rect.height() > height ? 0 - (i - rect.bottom) : 0 - (scrollY - rect.top), -getScrollY());
        }
        return iMax;
    }

    private void doScrollY(int i) {
        if (i != 0) {
            if (this.mSmoothScrollingEnabled) {
                if (this.mHorizontal) {
                    smoothScrollBy(0, i);
                    return;
                } else {
                    smoothScrollBy(i, 0);
                    return;
                }
            }
            if (this.mHorizontal) {
                scrollBy(0, i);
            } else {
                scrollBy(i, 0);
            }
        }
    }

    private void endDrag() {
        this.mIsBeingDragged = false;
        this.mIsOrthoDragged = false;
        this.mDownView = null;
        recycleVelocityTracker();
        if (this.mScrollStrictSpan != null) {
            this.mScrollStrictSpan.finish();
            this.mScrollStrictSpan = null;
        }
    }

    private View findFocusableViewInBounds(boolean z, int i, int i2) {
        boolean z2;
        ArrayList focusables = getFocusables(2);
        int size = focusables.size();
        View view = null;
        boolean z3 = false;
        int i3 = 0;
        while (i3 < size) {
            View view2 = (View) focusables.get(i3);
            int left = this.mHorizontal ? view2.getLeft() : view2.getTop();
            int right = this.mHorizontal ? view2.getRight() : view2.getBottom();
            if (i >= right || left >= i2) {
                z2 = z3;
                view2 = view;
            } else {
                boolean z4 = i < left && right < i2;
                if (view == null) {
                    z2 = z4;
                } else {
                    boolean z5 = (z && left < (this.mHorizontal ? view.getLeft() : view.getTop())) || (!z && right > (this.mHorizontal ? view.getRight() : view.getBottom()));
                    if (z3) {
                        if (z4 && z5) {
                            z2 = z3;
                        }
                    } else if (z4) {
                        z2 = true;
                    } else if (!z5) {
                    }
                }
            }
            z3 = z2;
            i3++;
            view = view2;
        }
        return view;
    }

    private int getScrollRange() {
        if (getChildCount() <= 0) {
            return 0;
        }
        View childAt = getChildAt(0);
        return this.mHorizontal ? Math.max(0, childAt.getWidth() - ((getWidth() - this.mPaddingRight) - this.mPaddingLeft)) : Math.max(0, childAt.getHeight() - ((getHeight() - this.mPaddingBottom) - this.mPaddingTop));
    }

    private boolean inChild(int i, int i2) {
        if (getChildCount() <= 0) {
            return false;
        }
        int i3 = this.mScrollY;
        View childAt = getChildAt(0);
        return i2 >= childAt.getTop() - i3 && i2 < childAt.getBottom() - i3 && i >= childAt.getLeft() && i < childAt.getRight();
    }

    private void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    private void initScrollView() {
        this.mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(262144);
        setWillNotDraw(false);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(this.mContext);
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mOverscrollDistance = viewConfiguration.getScaledOverscrollDistance();
        this.mOverflingDistance = viewConfiguration.getScaledOverflingDistance();
        this.mDownCoords = new PointF();
    }

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private boolean isOffScreen(View view) {
        return this.mHorizontal ? !isWithinDeltaOfScreen(view, getWidth(), 0) : !isWithinDeltaOfScreen(view, 0, getHeight());
    }

    private boolean isOrthoMove(float f, float f2) {
        return (this.mHorizontal && Math.abs(f2) > Math.abs(f)) || (!this.mHorizontal && Math.abs(f) > Math.abs(f2));
    }

    private boolean isViewDescendantOf(View view, View view2) {
        if (view == view2) {
            return true;
        }
        Object parent = view.getParent();
        return (parent instanceof ViewGroup) && isViewDescendantOf((View) parent, view2);
    }

    private boolean isWithinDeltaOfScreen(View view, int i, int i2) {
        view.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(view, this.mTempRect);
        return this.mHorizontal ? this.mTempRect.right + i >= getScrollX() && this.mTempRect.left - i <= getScrollX() + i2 : this.mTempRect.bottom + i >= getScrollY() && this.mTempRect.top - i <= getScrollY() + i2;
    }

    private void onSecondaryPointerUp(MotionEvent motionEvent) {
        int action = (motionEvent.getAction() & 65280) >> 8;
        if (motionEvent.getPointerId(action) == this.mActivePointerId) {
            int i = action == 0 ? 1 : 0;
            this.mLastMotionY = this.mHorizontal ? motionEvent.getX(i) : motionEvent.getY(i);
            this.mActivePointerId = motionEvent.getPointerId(i);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
            this.mLastOrthoCoord = this.mHorizontal ? motionEvent.getY(i) : motionEvent.getX(i);
        }
    }

    private void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private boolean scrollAndFocus(int i, int i2, int i3) {
        boolean z = false;
        int height = getHeight();
        int scrollY = getScrollY();
        int i4 = height + scrollY;
        boolean z2 = i == 33;
        View viewFindFocusableViewInBounds = findFocusableViewInBounds(z2, i2, i3);
        if (viewFindFocusableViewInBounds == null) {
            viewFindFocusableViewInBounds = this;
        }
        if (i2 < scrollY || i3 > i4) {
            doScrollY(z2 ? i2 - scrollY : i3 - i4);
            z = true;
        }
        if (viewFindFocusableViewInBounds != findFocus()) {
            viewFindFocusableViewInBounds.requestFocus(i);
        }
        return z;
    }

    private void scrollToChild(View view) {
        view.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(view, this.mTempRect);
        scrollToChildRect(this.mTempRect, true);
    }

    private boolean scrollToChildRect(Rect rect, boolean z) {
        int iComputeScrollDeltaToGetChildRectOnScreen = computeScrollDeltaToGetChildRectOnScreen(rect);
        boolean z2 = iComputeScrollDeltaToGetChildRectOnScreen != 0;
        if (z2) {
            if (z) {
                if (this.mHorizontal) {
                    scrollBy(iComputeScrollDeltaToGetChildRectOnScreen, 0);
                } else {
                    scrollBy(0, iComputeScrollDeltaToGetChildRectOnScreen);
                }
            } else if (this.mHorizontal) {
                smoothScrollBy(iComputeScrollDeltaToGetChildRectOnScreen, 0);
            } else {
                smoothScrollBy(0, iComputeScrollDeltaToGetChildRectOnScreen);
            }
        }
        return z2;
    }

    @Override
    public void addView(View view) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(view);
    }

    @Override
    public void addView(View view, int i) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(view, i);
    }

    @Override
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(view, i, layoutParams);
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams layoutParams) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(view, layoutParams);
    }

    public boolean arrowScroll(int i) {
        int bottom;
        View viewFindFocus = findFocus();
        if (viewFindFocus == this) {
            viewFindFocus = null;
        }
        View viewFindNextFocus = FocusFinder.getInstance().findNextFocus(this, viewFindFocus, i);
        int maxScrollAmount = getMaxScrollAmount();
        if (viewFindNextFocus == null || !isWithinDeltaOfScreen(viewFindNextFocus, maxScrollAmount, getHeight())) {
            if (i == 33 && getScrollY() < maxScrollAmount) {
                bottom = getScrollY();
            } else if (i != 130 || getChildCount() <= 0 || (bottom = getChildAt(0).getBottom() - ((getScrollY() + getHeight()) - this.mPaddingBottom)) >= maxScrollAmount) {
                bottom = maxScrollAmount;
            }
            if (bottom == 0) {
                return false;
            }
            if (i != 130) {
                bottom = -bottom;
            }
            doScrollY(bottom);
        } else {
            viewFindNextFocus.getDrawingRect(this.mTempRect);
            offsetDescendantRectToMyCoords(viewFindNextFocus, this.mTempRect);
            doScrollY(computeScrollDeltaToGetChildRectOnScreen(this.mTempRect));
            viewFindNextFocus.requestFocus(i);
        }
        if (viewFindFocus != null && viewFindFocus.isFocused() && isOffScreen(viewFindFocus)) {
            int descendantFocusability = getDescendantFocusability();
            setDescendantFocusability(131072);
            requestFocus();
            setDescendantFocusability(descendantFocusability);
        }
        return true;
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    @Override
    protected int computeHorizontalScrollRange() {
        if (!this.mHorizontal) {
            return super.computeHorizontalScrollRange();
        }
        int childCount = getChildCount();
        int width = (getWidth() - this.mPaddingRight) - this.mPaddingLeft;
        if (childCount == 0) {
            return width;
        }
        int right = getChildAt(0).getRight();
        int i = this.mScrollX;
        int iMax = Math.max(0, right - width);
        return i < 0 ? right - i : i > iMax ? right + (i - iMax) : right;
    }

    @Override
    public void computeScroll() {
        if (!this.mScroller.computeScrollOffset()) {
            if (this.mFlingStrictSpan != null) {
                this.mFlingStrictSpan.finish();
                this.mFlingStrictSpan = null;
                return;
            }
            return;
        }
        int i = this.mScrollX;
        int i2 = this.mScrollY;
        int currX = this.mScroller.getCurrX();
        int currY = this.mScroller.getCurrY();
        if (i != currX || i2 != currY) {
            if (this.mHorizontal) {
                overScrollBy(currX - i, currY - i2, i, i2, getScrollRange(), 0, this.mOverflingDistance, 0, false);
            } else {
                overScrollBy(currX - i, currY - i2, i, i2, 0, getScrollRange(), 0, this.mOverflingDistance, false);
            }
            onScrollChanged(this.mScrollX, this.mScrollY, i, i2);
        }
        awakenScrollBars();
        invalidate();
    }

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return this.mHorizontal ? computeScrollDeltaToGetChildRectOnScreenHorizontal(rect) : computeScrollDeltaToGetChildRectOnScreenVertical(rect);
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    protected int computeVerticalScrollRange() {
        if (this.mHorizontal) {
            return super.computeVerticalScrollRange();
        }
        int childCount = getChildCount();
        int height = (getHeight() - this.mPaddingBottom) - this.mPaddingTop;
        if (childCount == 0) {
            return height;
        }
        int bottom = getChildAt(0).getBottom();
        int i = this.mScrollY;
        int iMax = Math.max(0, bottom - height);
        return i < 0 ? bottom - i : i > iMax ? bottom + (i - iMax) : bottom;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return super.dispatchKeyEvent(keyEvent) || executeKeyEvent(keyEvent);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent.getEventType() == 4096) {
            return false;
        }
        super.dispatchPopulateAccessibilityEvent(accessibilityEvent);
        return false;
    }

    public boolean executeKeyEvent(KeyEvent keyEvent) {
        this.mTempRect.setEmpty();
        if (!canScroll()) {
            if (!isFocused() || keyEvent.getKeyCode() == 4) {
                return false;
            }
            View viewFindFocus = findFocus();
            if (viewFindFocus == this) {
                viewFindFocus = null;
            }
            View viewFindNextFocus = FocusFinder.getInstance().findNextFocus(this, viewFindFocus, 130);
            return (viewFindNextFocus == null || viewFindNextFocus == this || !viewFindNextFocus.requestFocus(130)) ? false : true;
        }
        if (keyEvent.getAction() != 0) {
            return false;
        }
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == 62) {
            pageScroll(keyEvent.isShiftPressed() ? 33 : 130);
            return false;
        }
        switch (keyCode) {
            case 19:
                return !keyEvent.isAltPressed() ? arrowScroll(33) : fullScroll(33);
            case 20:
                return !keyEvent.isAltPressed() ? arrowScroll(130) : fullScroll(130);
            default:
                return false;
        }
    }

    protected View findViewAt(int i, int i2) {
        return null;
    }

    public void fling(int i) {
        if (getChildCount() > 0) {
            if (this.mHorizontal) {
                int width = (getWidth() - this.mPaddingRight) - this.mPaddingLeft;
                this.mScroller.fling(this.mScrollX, this.mScrollY, i, 0, 0, Math.max(0, getChildAt(0).getWidth() - width), 0, 0, width / 2, 0);
            } else {
                int height = (getHeight() - this.mPaddingBottom) - this.mPaddingTop;
                this.mScroller.fling(this.mScrollX, this.mScrollY, 0, i, 0, 0, 0, Math.max(0, getChildAt(0).getHeight() - height), 0, height / 2);
            }
            if (this.mFlingStrictSpan == null) {
                this.mFlingStrictSpan = StrictMode.enterCriticalSpan("ScrollView-fling");
            }
            invalidate();
        }
    }

    public boolean fullScroll(int i) {
        int childCount;
        boolean z = i == 130;
        int height = getHeight();
        this.mTempRect.top = 0;
        this.mTempRect.bottom = height;
        if (z && (childCount = getChildCount()) > 0) {
            this.mTempRect.bottom = getChildAt(childCount - 1).getBottom() + this.mPaddingBottom;
            this.mTempRect.top = this.mTempRect.bottom - height;
        }
        return scrollAndFocus(i, this.mTempRect.top, this.mTempRect.bottom);
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }
        if (this.mHorizontal) {
            int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength();
            int right = (getChildAt(0).getRight() - this.mScrollX) - (getWidth() - this.mPaddingRight);
            if (right < horizontalFadingEdgeLength) {
                return right / horizontalFadingEdgeLength;
            }
        } else {
            int verticalFadingEdgeLength = getVerticalFadingEdgeLength();
            int bottom = (getChildAt(0).getBottom() - this.mScrollY) - (getHeight() - this.mPaddingBottom);
            if (bottom < verticalFadingEdgeLength) {
                return bottom / verticalFadingEdgeLength;
            }
        }
        return 1.0f;
    }

    public int getMaxScrollAmount() {
        int i;
        int i2;
        if (this.mHorizontal) {
            i = this.mRight;
            i2 = this.mLeft;
        } else {
            i = this.mBottom;
            i2 = this.mTop;
        }
        return (int) ((i - i2) * 0.5f);
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }
        if (this.mHorizontal) {
            int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength();
            if (this.mScrollX < horizontalFadingEdgeLength) {
                return this.mScrollX / horizontalFadingEdgeLength;
            }
        } else {
            int verticalFadingEdgeLength = getVerticalFadingEdgeLength();
            if (this.mScrollY < verticalFadingEdgeLength) {
                return this.mScrollY / verticalFadingEdgeLength;
            }
        }
        return 1.0f;
    }

    @Override
    protected void measureChild(View view, int i, int i2) {
        int childMeasureSpec;
        int iMakeMeasureSpec;
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (this.mHorizontal) {
            iMakeMeasureSpec = getChildMeasureSpec(i2, this.mPaddingTop + this.mPaddingBottom, layoutParams.height);
            childMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        } else {
            childMeasureSpec = getChildMeasureSpec(i, this.mPaddingLeft + this.mPaddingRight, layoutParams.width);
            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        }
        view.measure(childMeasureSpec, iMakeMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View view, int i, int i2, int i3, int i4) {
        int iMakeMeasureSpec;
        int iMakeMeasureSpec2;
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        if (this.mHorizontal) {
            int childMeasureSpec = getChildMeasureSpec(i3, this.mPaddingTop + this.mPaddingBottom + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin + i4, marginLayoutParams.height);
            iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(marginLayoutParams.rightMargin + marginLayoutParams.leftMargin, 0);
            iMakeMeasureSpec = childMeasureSpec;
        } else {
            int childMeasureSpec2 = getChildMeasureSpec(i, this.mPaddingLeft + this.mPaddingRight + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin + i2, marginLayoutParams.width);
            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(marginLayoutParams.bottomMargin + marginLayoutParams.topMargin, 0);
            iMakeMeasureSpec2 = childMeasureSpec2;
        }
        view.measure(iMakeMeasureSpec2, iMakeMeasureSpec);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mScrollStrictSpan != null) {
            this.mScrollStrictSpan.finish();
            this.mScrollStrictSpan = null;
        }
        if (this.mFlingStrictSpan != null) {
            this.mFlingStrictSpan.finish();
            this.mFlingStrictSpan = null;
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        int i;
        if ((motionEvent.getSource() & 2) != 0 && motionEvent.getAction() == 8 && !this.mIsBeingDragged) {
            if (this.mHorizontal) {
                float axisValue = motionEvent.getAxisValue(10);
                if (axisValue != 0.0f) {
                    int horizontalScrollFactor = (int) (axisValue * getHorizontalScrollFactor());
                    int scrollRange = getScrollRange();
                    int i2 = this.mScrollX;
                    int i3 = i2 - horizontalScrollFactor;
                    i = i3 >= 0 ? i3 > scrollRange ? scrollRange : i3 : 0;
                    if (i != i2) {
                        super.scrollTo(i, this.mScrollY);
                        return true;
                    }
                }
            } else {
                float axisValue2 = motionEvent.getAxisValue(9);
                if (axisValue2 != 0.0f) {
                    int verticalScrollFactor = (int) (axisValue2 * getVerticalScrollFactor());
                    int scrollRange2 = getScrollRange();
                    int i4 = this.mScrollY;
                    int i5 = i4 - verticalScrollFactor;
                    i = i5 >= 0 ? i5 > scrollRange2 ? scrollRange2 : i5 : 0;
                    if (i != i4) {
                        super.scrollTo(this.mScrollX, i);
                        return true;
                    }
                }
            }
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setScrollable(true);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setScrollable(true);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean z = false;
        int action = motionEvent.getAction();
        if (action == 2 && this.mIsBeingDragged) {
            return true;
        }
        if (action == 2 && this.mIsOrthoDragged) {
            return true;
        }
        int i = action & 255;
        if (i != 6) {
            switch (i) {
                case 0:
                    float x = this.mHorizontal ? motionEvent.getX() : motionEvent.getY();
                    this.mDownCoords.x = motionEvent.getX();
                    this.mDownCoords.y = motionEvent.getY();
                    if (!inChild((int) motionEvent.getX(), (int) motionEvent.getY())) {
                        this.mIsBeingDragged = false;
                        recycleVelocityTracker();
                    } else {
                        this.mLastMotionY = x;
                        this.mActivePointerId = motionEvent.getPointerId(0);
                        initOrResetVelocityTracker();
                        this.mVelocityTracker.addMovement(motionEvent);
                        this.mIsBeingDragged = !this.mScroller.isFinished();
                        if (this.mIsBeingDragged && this.mScrollStrictSpan == null) {
                            this.mScrollStrictSpan = StrictMode.enterCriticalSpan("ScrollView-scroll");
                        }
                        this.mIsOrthoDragged = false;
                        this.mLastOrthoCoord = this.mHorizontal ? motionEvent.getY() : motionEvent.getX();
                        this.mDownView = findViewAt((int) motionEvent.getX(), (int) motionEvent.getY());
                    }
                    break;
                case 1:
                case 3:
                    this.mIsBeingDragged = false;
                    this.mIsOrthoDragged = false;
                    this.mActivePointerId = -1;
                    recycleVelocityTracker();
                    if (!this.mHorizontal) {
                        if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, 0, 0, getScrollRange())) {
                            invalidate();
                        }
                    } else if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, getScrollRange(), 0, 0)) {
                        invalidate();
                    }
                    break;
                case 2:
                    int i2 = this.mActivePointerId;
                    if (i2 != -1) {
                        int iFindPointerIndex = motionEvent.findPointerIndex(i2);
                        if (iFindPointerIndex != -1) {
                            float x2 = this.mHorizontal ? motionEvent.getX(iFindPointerIndex) : motionEvent.getY(iFindPointerIndex);
                            if (((int) Math.abs(x2 - this.mLastMotionY)) <= this.mTouchSlop) {
                                float y = this.mHorizontal ? motionEvent.getY(iFindPointerIndex) : motionEvent.getX(iFindPointerIndex);
                                if (Math.abs(y - this.mLastOrthoCoord) > this.mTouchSlop) {
                                    this.mIsOrthoDragged = true;
                                    this.mLastOrthoCoord = y;
                                    initVelocityTrackerIfNotExists();
                                    this.mVelocityTracker.addMovement(motionEvent);
                                }
                            } else {
                                this.mIsBeingDragged = true;
                                this.mLastMotionY = x2;
                                initVelocityTrackerIfNotExists();
                                this.mVelocityTracker.addMovement(motionEvent);
                                if (this.mScrollStrictSpan == null) {
                                    this.mScrollStrictSpan = StrictMode.enterCriticalSpan("ScrollView-scroll");
                                }
                            }
                        } else {
                            Log.e("ScrollerView", "Invalid active pointer index = " + i2 + " at onInterceptTouchEvent ACTION_MOVE");
                        }
                    }
                    break;
            }
        } else {
            onSecondaryPointerUp(motionEvent);
        }
        if (this.mIsBeingDragged || this.mIsOrthoDragged) {
            z = true;
        }
        return z;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mIsLayoutDirty = false;
        if (this.mChildToScrollTo != null && isViewDescendantOf(this.mChildToScrollTo, this)) {
            scrollToChild(this.mChildToScrollTo);
        }
        this.mChildToScrollTo = null;
        scrollTo(this.mScrollX, this.mScrollY);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        if (this.mFillViewport && View.MeasureSpec.getMode(i2) != 0 && getChildCount() > 0) {
            View childAt = getChildAt(0);
            if (!this.mHorizontal) {
                int measuredHeight = getMeasuredHeight();
                if (childAt.getMeasuredHeight() < measuredHeight) {
                    childAt.measure(getChildMeasureSpec(i, this.mPaddingLeft + this.mPaddingRight, ((FrameLayout.LayoutParams) childAt.getLayoutParams()).width), View.MeasureSpec.makeMeasureSpec((measuredHeight - this.mPaddingTop) - this.mPaddingBottom, 1073741824));
                    return;
                }
                return;
            }
            int measuredWidth = getMeasuredWidth();
            if (childAt.getMeasuredWidth() < measuredWidth) {
                childAt.measure(View.MeasureSpec.makeMeasureSpec((measuredWidth - this.mPaddingLeft) - this.mPaddingRight, 1073741824), getChildMeasureSpec(i2, this.mPaddingTop + this.mPaddingBottom, ((FrameLayout.LayoutParams) childAt.getLayoutParams()).height));
            }
        }
    }

    protected void onOrthoDrag(View view, float f) {
    }

    protected void onOrthoDragFinished(View view) {
    }

    protected void onOrthoFling(View view, float f) {
    }

    @Override
    protected void onOverScrolled(int i, int i2, boolean z, boolean z2) {
        if (this.mScroller.isFinished()) {
            super.scrollTo(i, i2);
        } else {
            this.mScrollX = i;
            this.mScrollY = i2;
            invalidateParentIfNeeded();
            if (this.mHorizontal && z) {
                this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, getScrollRange(), 0, 0);
            } else if (!this.mHorizontal && z2) {
                this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, 0, 0, getScrollRange());
            }
        }
        awakenScrollBars();
    }

    protected void onPull(int i) {
    }

    @Override
    protected boolean onRequestFocusInDescendants(int i, Rect rect) {
        if (this.mHorizontal) {
            if (i == 2) {
                i = 66;
            } else if (i == 1) {
                i = 17;
            }
        } else if (i == 2) {
            i = 130;
        } else if (i == 1) {
            i = 33;
        }
        View viewFindNextFocus = rect == null ? FocusFinder.getInstance().findNextFocus(this, null, i) : FocusFinder.getInstance().findNextFocusFromRect(this, rect, i);
        if (viewFindNextFocus == null || isOffScreen(viewFindNextFocus)) {
            return false;
        }
        return viewFindNextFocus.requestFocus(i, rect);
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        View viewFindFocus = findFocus();
        if (viewFindFocus == null || this == viewFindFocus || !isWithinDeltaOfScreen(viewFindFocus, 0, i4)) {
            return;
        }
        viewFindFocus.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(viewFindFocus, this.mTempRect);
        doScrollY(computeScrollDeltaToGetChildRectOnScreen(this.mTempRect));
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        initVelocityTrackerIfNotExists();
        this.mVelocityTracker.addMovement(motionEvent);
        switch (motionEvent.getAction() & 255) {
            case 0:
                this.mIsBeingDragged = getChildCount() != 0;
                if (!this.mIsBeingDragged) {
                    return false;
                }
                if (!this.mScroller.isFinished()) {
                    this.mScroller.abortAnimation();
                    if (this.mFlingStrictSpan != null) {
                        this.mFlingStrictSpan.finish();
                        this.mFlingStrictSpan = null;
                    }
                }
                this.mLastMotionY = this.mHorizontal ? motionEvent.getX() : motionEvent.getY();
                this.mActivePointerId = motionEvent.getPointerId(0);
                return true;
            case 1:
                VelocityTracker velocityTracker = this.mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                if (isOrthoMove(velocityTracker.getXVelocity(this.mActivePointerId), velocityTracker.getYVelocity(this.mActivePointerId))) {
                    if (this.mMinimumVelocity < Math.abs(this.mHorizontal ? velocityTracker.getYVelocity() : velocityTracker.getXVelocity())) {
                        onOrthoFling(this.mDownView, this.mHorizontal ? velocityTracker.getYVelocity() : velocityTracker.getXVelocity());
                    } else if (this.mIsOrthoDragged) {
                        onOrthoDragFinished(this.mDownView);
                        this.mActivePointerId = -1;
                        endDrag();
                    } else if (this.mIsBeingDragged) {
                        VelocityTracker velocityTracker2 = this.mVelocityTracker;
                        velocityTracker2.computeCurrentVelocity(1000, this.mMaximumVelocity);
                        int xVelocity = this.mHorizontal ? (int) velocityTracker2.getXVelocity(this.mActivePointerId) : (int) velocityTracker2.getYVelocity(this.mActivePointerId);
                        if (getChildCount() > 0) {
                            if (Math.abs(xVelocity) > this.mMinimumVelocity) {
                                fling(-xVelocity);
                            } else {
                                int scrollRange = getScrollRange();
                                if (this.mHorizontal) {
                                    if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, scrollRange, 0, 0)) {
                                        invalidate();
                                    }
                                } else if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, 0, 0, scrollRange)) {
                                    invalidate();
                                }
                            }
                            onPull(0);
                        }
                        this.mActivePointerId = -1;
                        endDrag();
                    }
                }
                return true;
            case 2:
                if (this.mIsOrthoDragged) {
                    int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                    if (iFindPointerIndex == -1) {
                        Log.e("ScrollerView", "Invalid active pointer index = " + this.mActivePointerId + " at onTouchEvent ACTION_MOVE");
                    } else {
                        float x = motionEvent.getX(iFindPointerIndex);
                        float y = motionEvent.getY(iFindPointerIndex);
                        if (isOrthoMove(x - this.mDownCoords.x, y - this.mDownCoords.y)) {
                            onOrthoDrag(this.mDownView, this.mHorizontal ? y - this.mDownCoords.y : x - this.mDownCoords.x);
                        }
                    }
                } else if (this.mIsBeingDragged) {
                    int iFindPointerIndex2 = motionEvent.findPointerIndex(this.mActivePointerId);
                    if (iFindPointerIndex2 == -1) {
                        Log.e("ScrollerView", "Invalid active pointer index = " + this.mActivePointerId + " at onTouchEvent ACTION_MOVE begin dragged");
                    } else {
                        float x2 = this.mHorizontal ? motionEvent.getX(iFindPointerIndex2) : motionEvent.getY(iFindPointerIndex2);
                        int i = (int) (this.mLastMotionY - x2);
                        this.mLastMotionY = x2;
                        int i2 = this.mScrollX;
                        int i3 = this.mScrollY;
                        int scrollRange2 = getScrollRange();
                        if (this.mHorizontal) {
                            if (overScrollBy(i, 0, this.mScrollX, 0, scrollRange2, 0, this.mOverscrollDistance, 0, true)) {
                                this.mVelocityTracker.clear();
                            }
                        } else if (overScrollBy(0, i, 0, this.mScrollY, 0, scrollRange2, 0, this.mOverscrollDistance, true)) {
                            this.mVelocityTracker.clear();
                        }
                        onScrollChanged(this.mScrollX, this.mScrollY, i2, i3);
                        int overScrollMode = getOverScrollMode();
                        if (overScrollMode == 0 || (overScrollMode == 1 && scrollRange2 > 0)) {
                            int i4 = this.mHorizontal ? i2 + i : i3 + i;
                            if (i4 < 0) {
                                onPull(i4);
                            } else if (i4 > scrollRange2) {
                                onPull(i4 - scrollRange2);
                            } else {
                                onPull(0);
                            }
                        }
                    }
                }
                return true;
            case 3:
                if (this.mIsOrthoDragged) {
                    onOrthoDragFinished(this.mDownView);
                    this.mActivePointerId = -1;
                    endDrag();
                } else if (this.mIsBeingDragged && getChildCount() > 0) {
                    if (this.mHorizontal) {
                        if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, getScrollRange(), 0, 0)) {
                            invalidate();
                        }
                    } else if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, 0, 0, getScrollRange())) {
                        invalidate();
                    }
                    this.mActivePointerId = -1;
                    endDrag();
                }
                return true;
            case 4:
            default:
                return true;
            case 5:
                int actionIndex = motionEvent.getActionIndex();
                this.mLastMotionY = this.mHorizontal ? motionEvent.getX(actionIndex) : motionEvent.getY(actionIndex);
                this.mLastOrthoCoord = this.mHorizontal ? motionEvent.getY(actionIndex) : motionEvent.getX(actionIndex);
                this.mActivePointerId = motionEvent.getPointerId(actionIndex);
                return true;
            case 6:
                onSecondaryPointerUp(motionEvent);
                int iFindPointerIndex3 = motionEvent.findPointerIndex(this.mActivePointerId);
                if (iFindPointerIndex3 == -1) {
                    Log.e("ScrollerView", "Invalid active pointer index = " + this.mActivePointerId + " at onTouchEvent ACTION_POINTER_UP");
                } else {
                    this.mLastMotionY = this.mHorizontal ? motionEvent.getX(iFindPointerIndex3) : motionEvent.getY(iFindPointerIndex3);
                }
                return true;
        }
    }

    public boolean pageScroll(int i) {
        boolean z = i == 130;
        int height = getHeight();
        if (z) {
            this.mTempRect.top = getScrollY() + height;
            int childCount = getChildCount();
            if (childCount > 0) {
                View childAt = getChildAt(childCount - 1);
                if (this.mTempRect.top + height > childAt.getBottom()) {
                    this.mTempRect.top = childAt.getBottom() - height;
                }
            }
        } else {
            this.mTempRect.top = getScrollY() - height;
            if (this.mTempRect.top < 0) {
                this.mTempRect.top = 0;
            }
        }
        this.mTempRect.bottom = this.mTempRect.top + height;
        return scrollAndFocus(i, this.mTempRect.top, this.mTempRect.bottom);
    }

    @Override
    public void requestChildFocus(View view, View view2) {
        if (this.mIsLayoutDirty) {
            this.mChildToScrollTo = view2;
        } else {
            scrollToChild(view2);
        }
        super.requestChildFocus(view, view2);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View view, Rect rect, boolean z) {
        rect.offset(view.getLeft() - view.getScrollX(), view.getTop() - view.getScrollY());
        return scrollToChildRect(rect, z);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean z) {
        if (z) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(z);
    }

    @Override
    public void requestLayout() {
        this.mIsLayoutDirty = true;
        super.requestLayout();
    }

    @Override
    public void scrollTo(int i, int i2) {
        if (getChildCount() > 0) {
            View childAt = getChildAt(0);
            int iClamp = clamp(i, (getWidth() - this.mPaddingRight) - this.mPaddingLeft, childAt.getWidth());
            int iClamp2 = clamp(i2, (getHeight() - this.mPaddingBottom) - this.mPaddingTop, childAt.getHeight());
            if (iClamp == this.mScrollX && iClamp2 == this.mScrollY) {
                return;
            }
            super.scrollTo(iClamp, iClamp2);
        }
    }

    public void setFillViewport(boolean z) {
        if (z != this.mFillViewport) {
            this.mFillViewport = z;
            requestLayout();
        }
    }

    public void setOrientation(int i) {
        this.mHorizontal = i == 0;
        Log.d("ScrollerView", "ScrollerView.setOrientation(): mHorizontal = " + this.mHorizontal);
        requestLayout();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public final void smoothScrollBy(int i, int i2) {
        if (getChildCount() == 0) {
            return;
        }
        if (AnimationUtils.currentAnimationTimeMillis() - this.mLastScroll > 250) {
            if (this.mHorizontal) {
                int width = getWidth();
                int i3 = this.mPaddingRight;
                int iMax = Math.max(0, getChildAt(0).getWidth() - ((width - i3) - this.mPaddingLeft));
                int i4 = this.mScrollX;
                this.mScroller.startScroll(i4, this.mScrollY, Math.max(0, Math.min(i + i4, iMax)) - i4, 0);
            } else {
                int height = getHeight();
                int i5 = this.mPaddingBottom;
                int iMax2 = Math.max(0, getChildAt(0).getHeight() - ((height - i5) - this.mPaddingTop));
                int i6 = this.mScrollY;
                this.mScroller.startScroll(this.mScrollX, i6, 0, Math.max(0, Math.min(i2 + i6, iMax2)) - i6);
            }
            invalidate();
        } else {
            if (!this.mScroller.isFinished()) {
                this.mScroller.abortAnimation();
                if (this.mFlingStrictSpan != null) {
                    this.mFlingStrictSpan.finish();
                    this.mFlingStrictSpan = null;
                }
            }
            scrollBy(i, i2);
        }
        this.mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    public final void smoothScrollTo(int i, int i2) {
        smoothScrollBy(i - this.mScrollX, i2 - this.mScrollY);
    }
}
