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

    private void initScrollView() {
        this.mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(262144);
        setWillNotDraw(false);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(((View) this).mContext);
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mOverscrollDistance = viewConfiguration.getScaledOverscrollDistance();
        this.mOverflingDistance = viewConfiguration.getScaledOverflingDistance();
        this.mDownCoords = new PointF();
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

    @Override
    protected float getTopFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }
        if (this.mHorizontal) {
            int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength();
            if (((View) this).mScrollX < horizontalFadingEdgeLength) {
                return ((View) this).mScrollX / horizontalFadingEdgeLength;
            }
            return 1.0f;
        }
        int verticalFadingEdgeLength = getVerticalFadingEdgeLength();
        if (((View) this).mScrollY < verticalFadingEdgeLength) {
            return ((View) this).mScrollY / verticalFadingEdgeLength;
        }
        return 1.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }
        if (this.mHorizontal) {
            int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength();
            int right = (getChildAt(0).getRight() - ((View) this).mScrollX) - (getWidth() - ((View) this).mPaddingRight);
            if (right < horizontalFadingEdgeLength) {
                return right / horizontalFadingEdgeLength;
            }
            return 1.0f;
        }
        int verticalFadingEdgeLength = getVerticalFadingEdgeLength();
        int bottom = (getChildAt(0).getBottom() - ((View) this).mScrollY) - (getHeight() - ((View) this).mPaddingBottom);
        if (bottom < verticalFadingEdgeLength) {
            return bottom / verticalFadingEdgeLength;
        }
        return 1.0f;
    }

    public int getMaxScrollAmount() {
        int i;
        int i2;
        if (this.mHorizontal) {
            i = ((View) this).mRight;
            i2 = ((View) this).mLeft;
        } else {
            i = ((View) this).mBottom;
            i2 = ((View) this).mTop;
        }
        return (int) (0.5f * (i - i2));
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
    public void addView(View view, ViewGroup.LayoutParams layoutParams) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(view, layoutParams);
    }

    @Override
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(view, i, layoutParams);
    }

    private boolean canScroll() {
        View childAt = getChildAt(0);
        if (childAt != null) {
            return this.mHorizontal ? getWidth() < (childAt.getWidth() + ((View) this).mPaddingLeft) + ((View) this).mPaddingRight : getHeight() < (childAt.getHeight() + ((View) this).mPaddingTop) + ((View) this).mPaddingBottom;
        }
        return false;
    }

    public void setFillViewport(boolean z) {
        if (z != this.mFillViewport) {
            this.mFillViewport = z;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        if (this.mFillViewport && View.MeasureSpec.getMode(i2) != 0 && getChildCount() > 0) {
            View childAt = getChildAt(0);
            if (this.mHorizontal) {
                int measuredWidth = getMeasuredWidth();
                if (childAt.getMeasuredWidth() < measuredWidth) {
                    childAt.measure(View.MeasureSpec.makeMeasureSpec((measuredWidth - ((View) this).mPaddingLeft) - ((View) this).mPaddingRight, 1073741824), getChildMeasureSpec(i2, ((View) this).mPaddingTop + ((View) this).mPaddingBottom, ((ViewGroup.LayoutParams) ((FrameLayout.LayoutParams) childAt.getLayoutParams())).height));
                    return;
                }
                return;
            }
            int measuredHeight = getMeasuredHeight();
            if (childAt.getMeasuredHeight() < measuredHeight) {
                childAt.measure(getChildMeasureSpec(i, ((View) this).mPaddingLeft + ((View) this).mPaddingRight, ((ViewGroup.LayoutParams) ((FrameLayout.LayoutParams) childAt.getLayoutParams())).width), View.MeasureSpec.makeMeasureSpec((measuredHeight - ((View) this).mPaddingTop) - ((View) this).mPaddingBottom, 1073741824));
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return super.dispatchKeyEvent(keyEvent) || executeKeyEvent(keyEvent);
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
        if (keyCode != 62) {
            switch (keyCode) {
                case 19:
                    if (!keyEvent.isAltPressed()) {
                    }
                    break;
                case 20:
                    if (!keyEvent.isAltPressed()) {
                    }
                    break;
            }
            return false;
        }
        pageScroll(keyEvent.isShiftPressed() ? 33 : 130);
        return false;
    }

    private boolean inChild(int i, int i2) {
        if (getChildCount() <= 0) {
            return false;
        }
        int i3 = ((View) this).mScrollY;
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

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean z) {
        if (z) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(z);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
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
                    if (this.mHorizontal) {
                        if (this.mScroller.springBack(((View) this).mScrollX, ((View) this).mScrollY, 0, getScrollRange(), 0, 0)) {
                            invalidate();
                        }
                    } else if (this.mScroller.springBack(((View) this).mScrollX, ((View) this).mScrollY, 0, 0, 0, getScrollRange())) {
                        invalidate();
                    }
                    break;
                case 2:
                    int i2 = this.mActivePointerId;
                    if (i2 != -1) {
                        int iFindPointerIndex = motionEvent.findPointerIndex(i2);
                        if (iFindPointerIndex == -1) {
                            Log.e("ScrollerView", "Invalid active pointer index = " + i2 + " at onInterceptTouchEvent ACTION_MOVE");
                        } else {
                            float x2 = this.mHorizontal ? motionEvent.getX(iFindPointerIndex) : motionEvent.getY(iFindPointerIndex);
                            if (((int) Math.abs(x2 - this.mLastMotionY)) > this.mTouchSlop) {
                                this.mIsBeingDragged = true;
                                this.mLastMotionY = x2;
                                initVelocityTrackerIfNotExists();
                                this.mVelocityTracker.addMovement(motionEvent);
                                if (this.mScrollStrictSpan == null) {
                                    this.mScrollStrictSpan = StrictMode.enterCriticalSpan("ScrollView-scroll");
                                }
                            } else {
                                float y = this.mHorizontal ? motionEvent.getY(iFindPointerIndex) : motionEvent.getX(iFindPointerIndex);
                                if (Math.abs(y - this.mLastOrthoCoord) > this.mTouchSlop) {
                                    this.mIsOrthoDragged = true;
                                    this.mLastOrthoCoord = y;
                                    initVelocityTrackerIfNotExists();
                                    this.mVelocityTracker.addMovement(motionEvent);
                                }
                            }
                        }
                    }
                    break;
            }
        } else {
            onSecondaryPointerUp(motionEvent);
        }
        return this.mIsBeingDragged || this.mIsOrthoDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int yVelocity;
        float y;
        int i;
        int i2;
        float f;
        float y2;
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
                        if (this.mHorizontal) {
                            yVelocity = (int) velocityTracker2.getXVelocity(this.mActivePointerId);
                        } else {
                            yVelocity = (int) velocityTracker2.getYVelocity(this.mActivePointerId);
                        }
                        if (getChildCount() > 0) {
                            if (Math.abs(yVelocity) > this.mMinimumVelocity) {
                                fling(-yVelocity);
                            } else {
                                int scrollRange = getScrollRange();
                                if (this.mHorizontal) {
                                    if (this.mScroller.springBack(((View) this).mScrollX, ((View) this).mScrollY, 0, scrollRange, 0, 0)) {
                                        invalidate();
                                    }
                                } else if (this.mScroller.springBack(((View) this).mScrollX, ((View) this).mScrollY, 0, 0, 0, scrollRange)) {
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
                        float y3 = motionEvent.getY(iFindPointerIndex);
                        if (isOrthoMove(x - this.mDownCoords.x, y3 - this.mDownCoords.y)) {
                            View view = this.mDownView;
                            if (this.mHorizontal) {
                                f = y3 - this.mDownCoords.y;
                            } else {
                                f = x - this.mDownCoords.x;
                            }
                            onOrthoDrag(view, f);
                        }
                    }
                } else if (this.mIsBeingDragged) {
                    int iFindPointerIndex2 = motionEvent.findPointerIndex(this.mActivePointerId);
                    if (iFindPointerIndex2 == -1) {
                        Log.e("ScrollerView", "Invalid active pointer index = " + this.mActivePointerId + " at onTouchEvent ACTION_MOVE begin dragged");
                    } else {
                        if (this.mHorizontal) {
                            y = motionEvent.getX(iFindPointerIndex2);
                        } else {
                            y = motionEvent.getY(iFindPointerIndex2);
                        }
                        int i3 = (int) (this.mLastMotionY - y);
                        this.mLastMotionY = y;
                        int i4 = ((View) this).mScrollX;
                        int i5 = ((View) this).mScrollY;
                        int scrollRange2 = getScrollRange();
                        if (this.mHorizontal) {
                            i = scrollRange2;
                            if (overScrollBy(i3, 0, ((View) this).mScrollX, 0, scrollRange2, 0, this.mOverscrollDistance, 0, true)) {
                                this.mVelocityTracker.clear();
                            }
                        } else {
                            i = scrollRange2;
                            if (overScrollBy(0, i3, 0, ((View) this).mScrollY, 0, i, 0, this.mOverscrollDistance, true)) {
                                this.mVelocityTracker.clear();
                            }
                        }
                        onScrollChanged(((View) this).mScrollX, ((View) this).mScrollY, i4, i5);
                        int overScrollMode = getOverScrollMode();
                        if (overScrollMode == 0) {
                            i2 = i;
                        } else if (overScrollMode == 1 && (i2 = i) > 0) {
                        }
                        int i6 = this.mHorizontal ? i4 + i3 : i5 + i3;
                        if (i6 < 0) {
                            onPull(i6);
                        } else if (i6 > i2) {
                            onPull(i6 - i2);
                        } else {
                            onPull(0);
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
                        if (this.mScroller.springBack(((View) this).mScrollX, ((View) this).mScrollY, 0, getScrollRange(), 0, 0)) {
                            invalidate();
                        }
                    } else if (this.mScroller.springBack(((View) this).mScrollX, ((View) this).mScrollY, 0, 0, 0, getScrollRange())) {
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
                    if (this.mHorizontal) {
                        y2 = motionEvent.getX(iFindPointerIndex3);
                    } else {
                        y2 = motionEvent.getY(iFindPointerIndex3);
                    }
                    this.mLastMotionY = y2;
                }
                return true;
        }
    }

    protected View findViewAt(int i, int i2) {
        return null;
    }

    protected void onPull(int i) {
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

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if ((motionEvent.getSource() & 2) != 0 && motionEvent.getAction() == 8 && !this.mIsBeingDragged) {
            if (this.mHorizontal) {
                float axisValue = motionEvent.getAxisValue(10);
                if (axisValue != 0.0f) {
                    int horizontalScrollFactor = (int) (axisValue * getHorizontalScrollFactor());
                    int scrollRange = getScrollRange();
                    int i = ((View) this).mScrollX;
                    int i2 = i - horizontalScrollFactor;
                    if (i2 >= 0) {
                        if (i2 > scrollRange) {
                            i2 = scrollRange;
                        }
                    } else {
                        i2 = 0;
                    }
                    if (i2 != i) {
                        super.scrollTo(i2, ((View) this).mScrollY);
                        return true;
                    }
                }
            } else {
                float axisValue2 = motionEvent.getAxisValue(9);
                if (axisValue2 != 0.0f) {
                    int verticalScrollFactor = (int) (axisValue2 * getVerticalScrollFactor());
                    int scrollRange2 = getScrollRange();
                    int i3 = ((View) this).mScrollY;
                    int i4 = i3 - verticalScrollFactor;
                    if (i4 >= 0) {
                        if (i4 > scrollRange2) {
                            i4 = scrollRange2;
                        }
                    } else {
                        i4 = 0;
                    }
                    if (i4 != i3) {
                        super.scrollTo(((View) this).mScrollX, i4);
                        return true;
                    }
                }
            }
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    protected void onOrthoDrag(View view, float f) {
    }

    protected void onOrthoDragFinished(View view) {
    }

    protected void onOrthoFling(View view, float f) {
    }

    @Override
    protected void onOverScrolled(int i, int i2, boolean z, boolean z2) {
        if (!this.mScroller.isFinished()) {
            ((View) this).mScrollX = i;
            ((View) this).mScrollY = i2;
            invalidateParentIfNeeded();
            if (this.mHorizontal && z) {
                this.mScroller.springBack(((View) this).mScrollX, ((View) this).mScrollY, 0, getScrollRange(), 0, 0);
            } else if (!this.mHorizontal && z2) {
                this.mScroller.springBack(((View) this).mScrollX, ((View) this).mScrollY, 0, 0, 0, getScrollRange());
            }
        } else {
            super.scrollTo(i, i2);
        }
        awakenScrollBars();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setScrollable(true);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setScrollable(true);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent.getEventType() != 4096) {
            super.dispatchPopulateAccessibilityEvent(accessibilityEvent);
            return false;
        }
        return false;
    }

    private int getScrollRange() {
        if (getChildCount() <= 0) {
            return 0;
        }
        View childAt = getChildAt(0);
        if (this.mHorizontal) {
            return Math.max(0, childAt.getWidth() - ((getWidth() - ((View) this).mPaddingRight) - ((View) this).mPaddingLeft));
        }
        return Math.max(0, childAt.getHeight() - ((getHeight() - ((View) this).mPaddingBottom) - ((View) this).mPaddingTop));
    }

    private View findFocusableViewInBounds(boolean z, int i, int i2) {
        ArrayList<View> focusables = getFocusables(2);
        int size = focusables.size();
        View view = null;
        boolean z2 = false;
        for (int i3 = 0; i3 < size; i3++) {
            View view2 = focusables.get(i3);
            int left = this.mHorizontal ? view2.getLeft() : view2.getTop();
            int right = this.mHorizontal ? view2.getRight() : view2.getBottom();
            if (i < right && left < i2) {
                boolean z3 = i < left && right < i2;
                if (view == null) {
                    view = view2;
                    z2 = z3;
                } else {
                    boolean z4 = (z && left < (this.mHorizontal ? view.getLeft() : view.getTop())) || (!z && right > (this.mHorizontal ? view.getRight() : view.getBottom()));
                    if (z2) {
                        if (z3 && z4) {
                            view = view2;
                        }
                    } else if (z3) {
                        view = view2;
                        z2 = true;
                    } else if (z4) {
                    }
                }
            }
        }
        return view;
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

    public boolean fullScroll(int i) {
        int childCount;
        boolean z = i == 130;
        int height = getHeight();
        this.mTempRect.top = 0;
        this.mTempRect.bottom = height;
        if (z && (childCount = getChildCount()) > 0) {
            this.mTempRect.bottom = getChildAt(childCount - 1).getBottom() + ((View) this).mPaddingBottom;
            this.mTempRect.top = this.mTempRect.bottom - height;
        }
        return scrollAndFocus(i, this.mTempRect.top, this.mTempRect.bottom);
    }

    private boolean scrollAndFocus(int i, int i2, int i3) {
        int height = getHeight();
        int scrollY = getScrollY();
        int i4 = height + scrollY;
        boolean z = false;
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

    public boolean arrowScroll(int i) {
        int bottom;
        View viewFindFocus = findFocus();
        if (viewFindFocus == this) {
            viewFindFocus = null;
        }
        View viewFindNextFocus = FocusFinder.getInstance().findNextFocus(this, viewFindFocus, i);
        int maxScrollAmount = getMaxScrollAmount();
        if (viewFindNextFocus != null && isWithinDeltaOfScreen(viewFindNextFocus, maxScrollAmount, getHeight())) {
            viewFindNextFocus.getDrawingRect(this.mTempRect);
            offsetDescendantRectToMyCoords(viewFindNextFocus, this.mTempRect);
            doScrollY(computeScrollDeltaToGetChildRectOnScreen(this.mTempRect));
            viewFindNextFocus.requestFocus(i);
        } else {
            if (i == 33 && getScrollY() < maxScrollAmount) {
                maxScrollAmount = getScrollY();
            } else if (i == 130 && getChildCount() > 0 && (bottom = getChildAt(0).getBottom() - ((getScrollY() + getHeight()) - ((View) this).mPaddingBottom)) < maxScrollAmount) {
                maxScrollAmount = bottom;
            }
            if (maxScrollAmount == 0) {
                return false;
            }
            if (i != 130) {
                maxScrollAmount = -maxScrollAmount;
            }
            doScrollY(maxScrollAmount);
        }
        if (viewFindFocus != null && viewFindFocus.isFocused() && isOffScreen(viewFindFocus)) {
            int descendantFocusability = getDescendantFocusability();
            setDescendantFocusability(131072);
            requestFocus();
            setDescendantFocusability(descendantFocusability);
            return true;
        }
        return true;
    }

    private boolean isOrthoMove(float f, float f2) {
        return (this.mHorizontal && Math.abs(f2) > Math.abs(f)) || (!this.mHorizontal && Math.abs(f) > Math.abs(f2));
    }

    private boolean isOffScreen(View view) {
        if (this.mHorizontal) {
            return !isWithinDeltaOfScreen(view, getWidth(), 0);
        }
        return !isWithinDeltaOfScreen(view, 0, getHeight());
    }

    private boolean isWithinDeltaOfScreen(View view, int i, int i2) {
        view.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(view, this.mTempRect);
        return this.mHorizontal ? this.mTempRect.right + i >= getScrollX() && this.mTempRect.left - i <= getScrollX() + i2 : this.mTempRect.bottom + i >= getScrollY() && this.mTempRect.top - i <= getScrollY() + i2;
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

    public final void smoothScrollBy(int i, int i2) {
        if (getChildCount() == 0) {
            return;
        }
        if (AnimationUtils.currentAnimationTimeMillis() - this.mLastScroll > 250) {
            if (this.mHorizontal) {
                int iMax = Math.max(0, getChildAt(0).getWidth() - ((getWidth() - ((View) this).mPaddingRight) - ((View) this).mPaddingLeft));
                int i3 = ((View) this).mScrollX;
                this.mScroller.startScroll(i3, ((View) this).mScrollY, Math.max(0, Math.min(i + i3, iMax)) - i3, 0);
            } else {
                int iMax2 = Math.max(0, getChildAt(0).getHeight() - ((getHeight() - ((View) this).mPaddingBottom) - ((View) this).mPaddingTop));
                int i4 = ((View) this).mScrollY;
                this.mScroller.startScroll(((View) this).mScrollX, i4, 0, Math.max(0, Math.min(i2 + i4, iMax2)) - i4);
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
        smoothScrollBy(i - ((View) this).mScrollX, i2 - ((View) this).mScrollY);
    }

    @Override
    protected int computeVerticalScrollRange() {
        if (this.mHorizontal) {
            return super.computeVerticalScrollRange();
        }
        int childCount = getChildCount();
        int height = (getHeight() - ((View) this).mPaddingBottom) - ((View) this).mPaddingTop;
        if (childCount == 0) {
            return height;
        }
        int bottom = getChildAt(0).getBottom();
        int i = ((View) this).mScrollY;
        int iMax = Math.max(0, bottom - height);
        if (i < 0) {
            return bottom - i;
        }
        if (i > iMax) {
            return bottom + (i - iMax);
        }
        return bottom;
    }

    @Override
    protected int computeHorizontalScrollRange() {
        if (!this.mHorizontal) {
            return super.computeHorizontalScrollRange();
        }
        int childCount = getChildCount();
        int width = (getWidth() - ((View) this).mPaddingRight) - ((View) this).mPaddingLeft;
        if (childCount == 0) {
            return width;
        }
        int right = getChildAt(0).getRight();
        int i = ((View) this).mScrollX;
        int iMax = Math.max(0, right - width);
        if (i < 0) {
            return right - i;
        }
        if (i > iMax) {
            return right + (i - iMax);
        }
        return right;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    @Override
    protected void measureChild(View view, int i, int i2) {
        int childMeasureSpec;
        int iMakeMeasureSpec;
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (this.mHorizontal) {
            iMakeMeasureSpec = getChildMeasureSpec(i2, ((View) this).mPaddingTop + ((View) this).mPaddingBottom, layoutParams.height);
            childMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        } else {
            childMeasureSpec = getChildMeasureSpec(i, ((View) this).mPaddingLeft + ((View) this).mPaddingRight, layoutParams.width);
            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        }
        view.measure(childMeasureSpec, iMakeMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View view, int i, int i2, int i3, int i4) {
        int childMeasureSpec;
        int iMakeMeasureSpec;
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        if (this.mHorizontal) {
            iMakeMeasureSpec = getChildMeasureSpec(i3, ((View) this).mPaddingTop + ((View) this).mPaddingBottom + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin + i4, ((ViewGroup.LayoutParams) marginLayoutParams).height);
            childMeasureSpec = View.MeasureSpec.makeMeasureSpec(marginLayoutParams.leftMargin + marginLayoutParams.rightMargin, 0);
        } else {
            childMeasureSpec = getChildMeasureSpec(i, ((View) this).mPaddingLeft + ((View) this).mPaddingRight + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin + i2, ((ViewGroup.LayoutParams) marginLayoutParams).width);
            iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(marginLayoutParams.topMargin + marginLayoutParams.bottomMargin, 0);
        }
        view.measure(childMeasureSpec, iMakeMeasureSpec);
    }

    @Override
    public void computeScroll() {
        if (this.mScroller.computeScrollOffset()) {
            int i = ((View) this).mScrollX;
            int i2 = ((View) this).mScrollY;
            int currX = this.mScroller.getCurrX();
            int currY = this.mScroller.getCurrY();
            if (i != currX || i2 != currY) {
                if (this.mHorizontal) {
                    overScrollBy(currX - i, currY - i2, i, i2, getScrollRange(), 0, this.mOverflingDistance, 0, false);
                } else {
                    overScrollBy(currX - i, currY - i2, i, i2, 0, getScrollRange(), 0, this.mOverflingDistance, false);
                }
                onScrollChanged(((View) this).mScrollX, ((View) this).mScrollY, i, i2);
            }
            awakenScrollBars();
            invalidate();
            return;
        }
        if (this.mFlingStrictSpan != null) {
            this.mFlingStrictSpan.finish();
            this.mFlingStrictSpan = null;
        }
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

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (this.mHorizontal) {
            return computeScrollDeltaToGetChildRectOnScreenHorizontal(rect);
        }
        return computeScrollDeltaToGetChildRectOnScreenVertical(rect);
    }

    private int computeScrollDeltaToGetChildRectOnScreenVertical(Rect rect) {
        int i;
        int i2;
        if (getChildCount() == 0) {
            return 0;
        }
        int height = getHeight();
        int scrollY = getScrollY();
        int i3 = scrollY + height;
        int verticalFadingEdgeLength = getVerticalFadingEdgeLength();
        if (rect.top > 0) {
            scrollY += verticalFadingEdgeLength;
        }
        if (rect.bottom < getChildAt(0).getHeight()) {
            i3 -= verticalFadingEdgeLength;
        }
        if (rect.bottom > i3 && rect.top > scrollY) {
            if (rect.height() > height) {
                i2 = (rect.top - scrollY) + 0;
            } else {
                i2 = (rect.bottom - i3) + 0;
            }
            return Math.min(i2, getChildAt(0).getBottom() - i3);
        }
        if (rect.top >= scrollY || rect.bottom >= i3) {
            return 0;
        }
        if (rect.height() > height) {
            i = 0 - (i3 - rect.bottom);
        } else {
            i = 0 - (scrollY - rect.top);
        }
        return Math.max(i, -getScrollY());
    }

    private int computeScrollDeltaToGetChildRectOnScreenHorizontal(Rect rect) {
        int i;
        int i2;
        if (getChildCount() == 0) {
            return 0;
        }
        int width = getWidth();
        int scrollX = getScrollX();
        int i3 = scrollX + width;
        int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength();
        if (rect.left > 0) {
            scrollX += horizontalFadingEdgeLength;
        }
        if (rect.right < getChildAt(0).getWidth()) {
            i3 -= horizontalFadingEdgeLength;
        }
        if (rect.right > i3 && rect.left > scrollX) {
            if (rect.width() > width) {
                i2 = (rect.left - scrollX) + 0;
            } else {
                i2 = (rect.right - i3) + 0;
            }
            return Math.min(i2, getChildAt(0).getRight() - i3);
        }
        if (rect.left >= scrollX || rect.right >= i3) {
            return 0;
        }
        if (rect.width() > width) {
            i = 0 - (i3 - rect.right);
        } else {
            i = 0 - (scrollX - rect.left);
        }
        return Math.max(i, -getScrollX());
    }

    @Override
    public void requestChildFocus(View view, View view2) {
        if (!this.mIsLayoutDirty) {
            scrollToChild(view2);
        } else {
            this.mChildToScrollTo = view2;
        }
        super.requestChildFocus(view, view2);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int i, Rect rect) {
        View viewFindNextFocusFromRect;
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
        if (rect == null) {
            viewFindNextFocusFromRect = FocusFinder.getInstance().findNextFocus(this, null, i);
        } else {
            viewFindNextFocusFromRect = FocusFinder.getInstance().findNextFocusFromRect(this, rect, i);
        }
        if (viewFindNextFocusFromRect == null || isOffScreen(viewFindNextFocusFromRect)) {
            return false;
        }
        return viewFindNextFocusFromRect.requestFocus(i, rect);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View view, Rect rect, boolean z) {
        rect.offset(view.getLeft() - view.getScrollX(), view.getTop() - view.getScrollY());
        return scrollToChildRect(rect, z);
    }

    @Override
    public void requestLayout() {
        this.mIsLayoutDirty = true;
        super.requestLayout();
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
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mIsLayoutDirty = false;
        if (this.mChildToScrollTo != null && isViewDescendantOf(this.mChildToScrollTo, this)) {
            scrollToChild(this.mChildToScrollTo);
        }
        this.mChildToScrollTo = null;
        scrollTo(((View) this).mScrollX, ((View) this).mScrollY);
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        View viewFindFocus = findFocus();
        if (viewFindFocus != null && this != viewFindFocus && isWithinDeltaOfScreen(viewFindFocus, 0, i4)) {
            viewFindFocus.getDrawingRect(this.mTempRect);
            offsetDescendantRectToMyCoords(viewFindFocus, this.mTempRect);
            doScrollY(computeScrollDeltaToGetChildRectOnScreen(this.mTempRect));
        }
    }

    private boolean isViewDescendantOf(View view, View view2) {
        if (view == view2) {
            return true;
        }
        ?? parent = view.getParent();
        return (parent instanceof ViewGroup) && isViewDescendantOf(parent, view2);
    }

    public void fling(int i) {
        if (getChildCount() > 0) {
            if (this.mHorizontal) {
                int width = (getWidth() - ((View) this).mPaddingRight) - ((View) this).mPaddingLeft;
                this.mScroller.fling(((View) this).mScrollX, ((View) this).mScrollY, i, 0, 0, Math.max(0, getChildAt(0).getWidth() - width), 0, 0, width / 2, 0);
            } else {
                int height = (getHeight() - ((View) this).mPaddingBottom) - ((View) this).mPaddingTop;
                this.mScroller.fling(((View) this).mScrollX, ((View) this).mScrollY, 0, i, 0, 0, 0, Math.max(0, getChildAt(0).getHeight() - height), 0, height / 2);
            }
            if (this.mFlingStrictSpan == null) {
                this.mFlingStrictSpan = StrictMode.enterCriticalSpan("ScrollView-fling");
            }
            invalidate();
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

    @Override
    public void scrollTo(int i, int i2) {
        if (getChildCount() > 0) {
            View childAt = getChildAt(0);
            int iClamp = clamp(i, (getWidth() - ((View) this).mPaddingRight) - ((View) this).mPaddingLeft, childAt.getWidth());
            int iClamp2 = clamp(i2, (getHeight() - ((View) this).mPaddingBottom) - ((View) this).mPaddingTop, childAt.getHeight());
            if (iClamp != ((View) this).mScrollX || iClamp2 != ((View) this).mScrollY) {
                super.scrollTo(iClamp, iClamp2);
            }
        }
    }

    private int clamp(int i, int i2, int i3) {
        if (i2 >= i3 || i < 0) {
            return 0;
        }
        if (i2 + i > i3) {
            return i3 - i2;
        }
        return i;
    }
}
