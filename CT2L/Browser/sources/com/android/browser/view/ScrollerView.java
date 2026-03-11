package com.android.browser.view;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.StrictMode;
import android.util.AttributeSet;
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
import java.util.List;

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

    public ScrollerView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.scrollViewStyle);
    }

    public ScrollerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mTempRect = new Rect();
        this.mIsLayoutDirty = true;
        this.mChildToScrollTo = null;
        this.mIsBeingDragged = false;
        this.mSmoothScrollingEnabled = true;
        this.mActivePointerId = -1;
        this.mScrollStrictSpan = null;
        this.mFlingStrictSpan = null;
        initScrollView();
        TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.ScrollView, defStyle, 0);
        setFillViewport(a.getBoolean(0, false));
        a.recycle();
    }

    private void initScrollView() {
        this.mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(262144);
        setWillNotDraw(false);
        ViewConfiguration configuration = ViewConfiguration.get(this.mContext);
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mOverscrollDistance = configuration.getScaledOverscrollDistance();
        this.mOverflingDistance = configuration.getScaledOverflingDistance();
        this.mDownCoords = new PointF();
    }

    public void setOrientation(int orientation) {
        this.mHorizontal = orientation == 0;
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
            int length = getHorizontalFadingEdgeLength();
            if (this.mScrollX < length) {
                return this.mScrollX / length;
            }
        } else {
            int length2 = getVerticalFadingEdgeLength();
            if (this.mScrollY < length2) {
                return this.mScrollY / length2;
            }
        }
        return 1.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }
        if (this.mHorizontal) {
            int length = getHorizontalFadingEdgeLength();
            int bottomEdge = getWidth() - this.mPaddingRight;
            int span = (getChildAt(0).getRight() - this.mScrollX) - bottomEdge;
            if (span < length) {
                return span / length;
            }
        } else {
            int length2 = getVerticalFadingEdgeLength();
            int bottomEdge2 = getHeight() - this.mPaddingBottom;
            int span2 = (getChildAt(0).getBottom() - this.mScrollY) - bottomEdge2;
            if (span2 < length2) {
                return span2 / length2;
            }
        }
        return 1.0f;
    }

    public int getMaxScrollAmount() {
        return (int) ((this.mHorizontal ? this.mRight - this.mLeft : this.mBottom - this.mTop) * 0.5f);
    }

    @Override
    public void addView(View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }
        super.addView(child, index, params);
    }

    private boolean canScroll() {
        View child = getChildAt(0);
        if (child != null) {
            return this.mHorizontal ? getWidth() < (child.getWidth() + this.mPaddingLeft) + this.mPaddingRight : getHeight() < (child.getHeight() + this.mPaddingTop) + this.mPaddingBottom;
        }
        return false;
    }

    public void setFillViewport(boolean fillViewport) {
        if (fillViewport != this.mFillViewport) {
            this.mFillViewport = fillViewport;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mFillViewport) {
            int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
            if (heightMode != 0 && getChildCount() > 0) {
                View child = getChildAt(0);
                if (this.mHorizontal) {
                    int width = getMeasuredWidth();
                    if (child.getMeasuredWidth() < width) {
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
                        int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, this.mPaddingTop + this.mPaddingBottom, lp.height);
                        int childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec((width - this.mPaddingLeft) - this.mPaddingRight, 1073741824);
                        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                        return;
                    }
                    return;
                }
                int height = getMeasuredHeight();
                if (child.getMeasuredHeight() < height) {
                    FrameLayout.LayoutParams lp2 = (FrameLayout.LayoutParams) child.getLayoutParams();
                    int childWidthMeasureSpec2 = getChildMeasureSpec(widthMeasureSpec, this.mPaddingLeft + this.mPaddingRight, lp2.width);
                    int childHeightMeasureSpec2 = View.MeasureSpec.makeMeasureSpec((height - this.mPaddingTop) - this.mPaddingBottom, 1073741824);
                    child.measure(childWidthMeasureSpec2, childHeightMeasureSpec2);
                }
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    public boolean executeKeyEvent(KeyEvent event) {
        this.mTempRect.setEmpty();
        if (!canScroll()) {
            if (!isFocused() || event.getKeyCode() == 4) {
                return false;
            }
            View currentFocused = findFocus();
            if (currentFocused == this) {
                currentFocused = null;
            }
            View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, 130);
            return (nextFocused == null || nextFocused == this || !nextFocused.requestFocus(130)) ? false : true;
        }
        boolean handled = false;
        if (event.getAction() == 0) {
            switch (event.getKeyCode()) {
                case 19:
                    handled = !event.isAltPressed() ? arrowScroll(33) : fullScroll(33);
                    break;
                case 20:
                    handled = !event.isAltPressed() ? arrowScroll(130) : fullScroll(130);
                    break;
                case 62:
                    pageScroll(event.isShiftPressed() ? 33 : 130);
                    break;
            }
        }
        return handled;
    }

    private boolean inChild(int x, int y) {
        if (getChildCount() <= 0) {
            return false;
        }
        int scrollY = this.mScrollY;
        View child = getChildAt(0);
        return y >= child.getTop() - scrollY && y < child.getBottom() - scrollY && x >= child.getLeft() && x < child.getRight();
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
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == 2 && this.mIsBeingDragged) {
            return true;
        }
        if (action == 2 && this.mIsOrthoDragged) {
            return true;
        }
        switch (action & 255) {
            case 0:
                float y = this.mHorizontal ? ev.getX() : ev.getY();
                this.mDownCoords.x = ev.getX();
                this.mDownCoords.y = ev.getY();
                if (!inChild((int) ev.getX(), (int) ev.getY())) {
                    this.mIsBeingDragged = false;
                    recycleVelocityTracker();
                } else {
                    this.mLastMotionY = y;
                    this.mActivePointerId = ev.getPointerId(0);
                    initOrResetVelocityTracker();
                    this.mVelocityTracker.addMovement(ev);
                    this.mIsBeingDragged = !this.mScroller.isFinished();
                    if (this.mIsBeingDragged && this.mScrollStrictSpan == null) {
                        this.mScrollStrictSpan = StrictMode.enterCriticalSpan("ScrollView-scroll");
                    }
                    this.mIsOrthoDragged = false;
                    this.mLastOrthoCoord = this.mHorizontal ? ev.getY() : ev.getX();
                    this.mDownView = findViewAt((int) ev.getX(), (int) ev.getY());
                }
                break;
            case 1:
            case 3:
                this.mIsBeingDragged = false;
                this.mIsOrthoDragged = false;
                this.mActivePointerId = -1;
                recycleVelocityTracker();
                if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, 0, 0, getScrollRange())) {
                    invalidate();
                }
                break;
            case 2:
                int activePointerId = this.mActivePointerId;
                if (activePointerId != -1) {
                    int pointerIndex = ev.findPointerIndex(activePointerId);
                    float y2 = this.mHorizontal ? ev.getX(pointerIndex) : ev.getY(pointerIndex);
                    int yDiff = (int) Math.abs(y2 - this.mLastMotionY);
                    if (yDiff > this.mTouchSlop) {
                        this.mIsBeingDragged = true;
                        this.mLastMotionY = y2;
                        initVelocityTrackerIfNotExists();
                        this.mVelocityTracker.addMovement(ev);
                        if (this.mScrollStrictSpan == null) {
                            this.mScrollStrictSpan = StrictMode.enterCriticalSpan("ScrollView-scroll");
                        }
                    } else {
                        float ocoord = this.mHorizontal ? ev.getY(pointerIndex) : ev.getX(pointerIndex);
                        if (Math.abs(ocoord - this.mLastOrthoCoord) > this.mTouchSlop) {
                            this.mIsOrthoDragged = true;
                            this.mLastOrthoCoord = ocoord;
                            initVelocityTrackerIfNotExists();
                            this.mVelocityTracker.addMovement(ev);
                        }
                    }
                }
                break;
            case 6:
                onSecondaryPointerUp(ev);
                break;
        }
        return this.mIsBeingDragged || this.mIsOrthoDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();
        this.mVelocityTracker.addMovement(ev);
        int action = ev.getAction();
        switch (action & 255) {
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
                this.mLastMotionY = this.mHorizontal ? ev.getX() : ev.getY();
                this.mActivePointerId = ev.getPointerId(0);
                return true;
            case 1:
                VelocityTracker vtracker = this.mVelocityTracker;
                vtracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                if (isOrthoMove(vtracker.getXVelocity(this.mActivePointerId), vtracker.getYVelocity(this.mActivePointerId))) {
                    if (this.mMinimumVelocity < Math.abs(this.mHorizontal ? vtracker.getYVelocity() : vtracker.getXVelocity())) {
                        onOrthoFling(this.mDownView, this.mHorizontal ? vtracker.getYVelocity() : vtracker.getXVelocity());
                    } else if (this.mIsOrthoDragged) {
                        onOrthoDragFinished(this.mDownView);
                        this.mActivePointerId = -1;
                        endDrag();
                    } else if (this.mIsBeingDragged) {
                        VelocityTracker velocityTracker = this.mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                        int initialVelocity = this.mHorizontal ? (int) velocityTracker.getXVelocity(this.mActivePointerId) : (int) velocityTracker.getYVelocity(this.mActivePointerId);
                        if (getChildCount() > 0) {
                            if (Math.abs(initialVelocity) > this.mMinimumVelocity) {
                                fling(-initialVelocity);
                            } else {
                                int bottom = getScrollRange();
                                if (this.mHorizontal) {
                                    if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, bottom, 0, 0)) {
                                        invalidate();
                                    }
                                } else if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, 0, 0, bottom)) {
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
                    int activePointerIndex = ev.findPointerIndex(this.mActivePointerId);
                    float x = ev.getX(activePointerIndex);
                    float y = ev.getY(activePointerIndex);
                    if (isOrthoMove(x - this.mDownCoords.x, y - this.mDownCoords.y)) {
                        onOrthoDrag(this.mDownView, this.mHorizontal ? y - this.mDownCoords.y : x - this.mDownCoords.x);
                    }
                } else if (this.mIsBeingDragged) {
                    int activePointerIndex2 = ev.findPointerIndex(this.mActivePointerId);
                    float y2 = this.mHorizontal ? ev.getX(activePointerIndex2) : ev.getY(activePointerIndex2);
                    int deltaY = (int) (this.mLastMotionY - y2);
                    this.mLastMotionY = y2;
                    int oldX = this.mScrollX;
                    int oldY = this.mScrollY;
                    int range = getScrollRange();
                    if (this.mHorizontal) {
                        if (overScrollBy(deltaY, 0, this.mScrollX, 0, range, 0, this.mOverscrollDistance, 0, true)) {
                            this.mVelocityTracker.clear();
                        }
                    } else if (overScrollBy(0, deltaY, 0, this.mScrollY, 0, range, 0, this.mOverscrollDistance, true)) {
                        this.mVelocityTracker.clear();
                    }
                    onScrollChanged(this.mScrollX, this.mScrollY, oldX, oldY);
                    int overscrollMode = getOverScrollMode();
                    if (overscrollMode == 0 || (overscrollMode == 1 && range > 0)) {
                        int pulledToY = this.mHorizontal ? oldX + deltaY : oldY + deltaY;
                        if (pulledToY < 0) {
                            onPull(pulledToY);
                        } else if (pulledToY > range) {
                            onPull(pulledToY - range);
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
                int index = ev.getActionIndex();
                this.mLastMotionY = this.mHorizontal ? ev.getX(index) : ev.getY(index);
                this.mLastOrthoCoord = this.mHorizontal ? ev.getY(index) : ev.getX(index);
                this.mActivePointerId = ev.getPointerId(index);
                return true;
            case 6:
                onSecondaryPointerUp(ev);
                this.mLastMotionY = this.mHorizontal ? ev.getX(ev.findPointerIndex(this.mActivePointerId)) : ev.getY(ev.findPointerIndex(this.mActivePointerId));
                return true;
        }
    }

    protected View findViewAt(int x, int y) {
        return null;
    }

    protected void onPull(int delta) {
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = (ev.getAction() & 65280) >> 8;
        int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            this.mLastMotionY = this.mHorizontal ? ev.getX(newPointerIndex) : ev.getY(newPointerIndex);
            this.mActivePointerId = ev.getPointerId(newPointerIndex);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
            this.mLastOrthoCoord = this.mHorizontal ? ev.getY(newPointerIndex) : ev.getX(newPointerIndex);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & 2) != 0) {
            switch (event.getAction()) {
                case 8:
                    if (!this.mIsBeingDragged) {
                        if (this.mHorizontal) {
                            float hscroll = event.getAxisValue(10);
                            if (hscroll != 0.0f) {
                                int delta = (int) (getHorizontalScrollFactor() * hscroll);
                                int range = getScrollRange();
                                int oldScrollX = this.mScrollX;
                                int newScrollX = oldScrollX - delta;
                                if (newScrollX < 0) {
                                    newScrollX = 0;
                                } else if (newScrollX > range) {
                                    newScrollX = range;
                                }
                                if (newScrollX != oldScrollX) {
                                    super.scrollTo(newScrollX, this.mScrollY);
                                    return true;
                                }
                            }
                        } else {
                            float vscroll = event.getAxisValue(9);
                            if (vscroll != 0.0f) {
                                int delta2 = (int) (getVerticalScrollFactor() * vscroll);
                                int range2 = getScrollRange();
                                int oldScrollY = this.mScrollY;
                                int newScrollY = oldScrollY - delta2;
                                if (newScrollY < 0) {
                                    newScrollY = 0;
                                } else if (newScrollY > range2) {
                                    newScrollY = range2;
                                }
                                if (newScrollY != oldScrollY) {
                                    super.scrollTo(this.mScrollX, newScrollY);
                                    return true;
                                }
                            }
                        }
                    }
                    break;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    protected void onOrthoDrag(View draggedView, float distance) {
    }

    protected void onOrthoDragFinished(View draggedView) {
    }

    protected void onOrthoFling(View draggedView, float velocity) {
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (!this.mScroller.isFinished()) {
            this.mScrollX = scrollX;
            this.mScrollY = scrollY;
            invalidateParentIfNeeded();
            if (this.mHorizontal && clampedX) {
                this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, getScrollRange(), 0, 0);
            } else if (!this.mHorizontal && clampedY) {
                this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, 0, 0, getScrollRange());
            }
        } else {
            super.scrollTo(scrollX, scrollY);
        }
        awakenScrollBars();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setScrollable(true);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setScrollable(true);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != 4096) {
            super.dispatchPopulateAccessibilityEvent(event);
            return false;
        }
        return false;
    }

    private int getScrollRange() {
        if (getChildCount() <= 0) {
            return 0;
        }
        View child = getChildAt(0);
        if (this.mHorizontal) {
            int scrollRange = Math.max(0, child.getWidth() - ((getWidth() - this.mPaddingRight) - this.mPaddingLeft));
            return scrollRange;
        }
        int scrollRange2 = Math.max(0, child.getHeight() - ((getHeight() - this.mPaddingBottom) - this.mPaddingTop));
        return scrollRange2;
    }

    private View findFocusableViewInBounds(boolean topFocus, int top, int bottom) {
        List<View> focusables = getFocusables(2);
        View focusCandidate = null;
        boolean foundFullyContainedFocusable = false;
        int count = focusables.size();
        for (int i = 0; i < count; i++) {
            View view = focusables.get(i);
            int viewTop = this.mHorizontal ? view.getLeft() : view.getTop();
            int viewBottom = this.mHorizontal ? view.getRight() : view.getBottom();
            if (top < viewBottom && viewTop < bottom) {
                boolean viewIsFullyContained = top < viewTop && viewBottom < bottom;
                if (focusCandidate == null) {
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    int ctop = this.mHorizontal ? focusCandidate.getLeft() : focusCandidate.getTop();
                    int cbot = this.mHorizontal ? focusCandidate.getRight() : focusCandidate.getBottom();
                    boolean viewIsCloserToBoundary = (topFocus && viewTop < ctop) || (!topFocus && viewBottom > cbot);
                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            focusCandidate = view;
                        }
                    } else if (viewIsFullyContained) {
                        focusCandidate = view;
                        foundFullyContainedFocusable = true;
                    } else if (viewIsCloserToBoundary) {
                        focusCandidate = view;
                    }
                }
            }
        }
        return focusCandidate;
    }

    public boolean pageScroll(int direction) {
        boolean down = direction == 130;
        int height = getHeight();
        if (down) {
            this.mTempRect.top = getScrollY() + height;
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                if (this.mTempRect.top + height > view.getBottom()) {
                    this.mTempRect.top = view.getBottom() - height;
                }
            }
        } else {
            this.mTempRect.top = getScrollY() - height;
            if (this.mTempRect.top < 0) {
                this.mTempRect.top = 0;
            }
        }
        this.mTempRect.bottom = this.mTempRect.top + height;
        return scrollAndFocus(direction, this.mTempRect.top, this.mTempRect.bottom);
    }

    public boolean fullScroll(int direction) {
        int count;
        boolean down = direction == 130;
        int height = getHeight();
        this.mTempRect.top = 0;
        this.mTempRect.bottom = height;
        if (down && (count = getChildCount()) > 0) {
            View view = getChildAt(count - 1);
            this.mTempRect.bottom = view.getBottom() + this.mPaddingBottom;
            this.mTempRect.top = this.mTempRect.bottom - height;
        }
        return scrollAndFocus(direction, this.mTempRect.top, this.mTempRect.bottom);
    }

    private boolean scrollAndFocus(int direction, int top, int bottom) {
        boolean handled = true;
        int height = getHeight();
        int containerTop = getScrollY();
        int containerBottom = containerTop + height;
        boolean up = direction == 33;
        View newFocused = findFocusableViewInBounds(up, top, bottom);
        if (newFocused == null) {
            newFocused = this;
        }
        if (top >= containerTop && bottom <= containerBottom) {
            handled = false;
        } else {
            int delta = up ? top - containerTop : bottom - containerBottom;
            doScrollY(delta);
        }
        if (newFocused != findFocus()) {
            newFocused.requestFocus(direction);
        }
        return handled;
    }

    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();
        if (currentFocused == this) {
            currentFocused = null;
        }
        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);
        int maxJump = getMaxScrollAmount();
        if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxJump, getHeight())) {
            nextFocused.getDrawingRect(this.mTempRect);
            offsetDescendantRectToMyCoords(nextFocused, this.mTempRect);
            doScrollY(computeScrollDeltaToGetChildRectOnScreen(this.mTempRect));
            nextFocused.requestFocus(direction);
        } else {
            int scrollDelta = maxJump;
            if (direction == 33 && getScrollY() < scrollDelta) {
                scrollDelta = getScrollY();
            } else if (direction == 130 && getChildCount() > 0) {
                int daBottom = getChildAt(0).getBottom();
                int screenBottom = (getScrollY() + getHeight()) - this.mPaddingBottom;
                if (daBottom - screenBottom < maxJump) {
                    scrollDelta = daBottom - screenBottom;
                }
            }
            if (scrollDelta == 0) {
                return false;
            }
            doScrollY(direction == 130 ? scrollDelta : -scrollDelta);
        }
        if (currentFocused != null && currentFocused.isFocused() && isOffScreen(currentFocused)) {
            int descendantFocusability = getDescendantFocusability();
            setDescendantFocusability(131072);
            requestFocus();
            setDescendantFocusability(descendantFocusability);
        }
        return true;
    }

    private boolean isOrthoMove(float moveX, float moveY) {
        return (this.mHorizontal && Math.abs(moveY) > Math.abs(moveX)) || (!this.mHorizontal && Math.abs(moveX) > Math.abs(moveY));
    }

    private boolean isOffScreen(View descendant) {
        return this.mHorizontal ? !isWithinDeltaOfScreen(descendant, getWidth(), 0) : !isWithinDeltaOfScreen(descendant, 0, getHeight());
    }

    private boolean isWithinDeltaOfScreen(View descendant, int delta, int height) {
        descendant.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(descendant, this.mTempRect);
        return this.mHorizontal ? this.mTempRect.right + delta >= getScrollX() && this.mTempRect.left - delta <= getScrollX() + height : this.mTempRect.bottom + delta >= getScrollY() && this.mTempRect.top - delta <= getScrollY() + height;
    }

    private void doScrollY(int delta) {
        if (delta != 0) {
            if (this.mSmoothScrollingEnabled) {
                if (this.mHorizontal) {
                    smoothScrollBy(0, delta);
                    return;
                } else {
                    smoothScrollBy(delta, 0);
                    return;
                }
            }
            if (this.mHorizontal) {
                scrollBy(0, delta);
            } else {
                scrollBy(delta, 0);
            }
        }
    }

    public final void smoothScrollBy(int dx, int dy) {
        if (getChildCount() != 0) {
            long duration = AnimationUtils.currentAnimationTimeMillis() - this.mLastScroll;
            if (duration > 250) {
                if (this.mHorizontal) {
                    int width = (getWidth() - this.mPaddingRight) - this.mPaddingLeft;
                    int right = getChildAt(0).getWidth();
                    int maxX = Math.max(0, right - width);
                    int scrollX = this.mScrollX;
                    this.mScroller.startScroll(scrollX, this.mScrollY, Math.max(0, Math.min(scrollX + dx, maxX)) - scrollX, 0);
                } else {
                    int height = (getHeight() - this.mPaddingBottom) - this.mPaddingTop;
                    int bottom = getChildAt(0).getHeight();
                    int maxY = Math.max(0, bottom - height);
                    int scrollY = this.mScrollY;
                    this.mScroller.startScroll(this.mScrollX, scrollY, 0, Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY);
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
                scrollBy(dx, dy);
            }
            this.mLastScroll = AnimationUtils.currentAnimationTimeMillis();
        }
    }

    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - this.mScrollX, y - this.mScrollY);
    }

    @Override
    protected int computeVerticalScrollRange() {
        if (this.mHorizontal) {
            return super.computeVerticalScrollRange();
        }
        int count = getChildCount();
        int contentHeight = (getHeight() - this.mPaddingBottom) - this.mPaddingTop;
        if (count != 0) {
            int scrollRange = getChildAt(0).getBottom();
            int scrollY = this.mScrollY;
            int overscrollBottom = Math.max(0, scrollRange - contentHeight);
            if (scrollY < 0) {
                scrollRange -= scrollY;
            } else if (scrollY > overscrollBottom) {
                scrollRange += scrollY - overscrollBottom;
            }
            return scrollRange;
        }
        return contentHeight;
    }

    @Override
    protected int computeHorizontalScrollRange() {
        if (!this.mHorizontal) {
            return super.computeHorizontalScrollRange();
        }
        int count = getChildCount();
        int contentWidth = (getWidth() - this.mPaddingRight) - this.mPaddingLeft;
        if (count != 0) {
            int scrollRange = getChildAt(0).getRight();
            int scrollX = this.mScrollX;
            int overscrollBottom = Math.max(0, scrollRange - contentWidth);
            if (scrollX < 0) {
                scrollRange -= scrollX;
            } else if (scrollX > overscrollBottom) {
                scrollRange += scrollX - overscrollBottom;
            }
            return scrollRange;
        }
        return contentWidth;
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
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (this.mHorizontal) {
            childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, this.mPaddingTop + this.mPaddingBottom, lp.height);
            childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, this.mPaddingLeft + this.mPaddingRight, lp.width);
            childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        }
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        int childWidthMeasureSpec;
        int childHeightMeasureSpec;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        if (this.mHorizontal) {
            childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec, this.mPaddingTop + this.mPaddingBottom + lp.topMargin + lp.bottomMargin + heightUsed, lp.height);
            childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(lp.leftMargin + lp.rightMargin, 0);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, this.mPaddingLeft + this.mPaddingRight + lp.leftMargin + lp.rightMargin + widthUsed, lp.width);
            childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(lp.topMargin + lp.bottomMargin, 0);
        }
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public void computeScroll() {
        if (this.mScroller.computeScrollOffset()) {
            int oldX = this.mScrollX;
            int oldY = this.mScrollY;
            int x = this.mScroller.getCurrX();
            int y = this.mScroller.getCurrY();
            if (oldX != x || oldY != y) {
                if (this.mHorizontal) {
                    overScrollBy(x - oldX, y - oldY, oldX, oldY, getScrollRange(), 0, this.mOverflingDistance, 0, false);
                } else {
                    overScrollBy(x - oldX, y - oldY, oldX, oldY, 0, getScrollRange(), 0, this.mOverflingDistance, false);
                }
                onScrollChanged(this.mScrollX, this.mScrollY, oldX, oldY);
            }
            awakenScrollBars();
            postInvalidate();
            return;
        }
        if (this.mFlingStrictSpan != null) {
            this.mFlingStrictSpan.finish();
            this.mFlingStrictSpan = null;
        }
    }

    private void scrollToChild(View child) {
        child.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(child, this.mTempRect);
        scrollToChildRect(this.mTempRect, true);
    }

    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        boolean scroll = delta != 0;
        if (scroll) {
            if (immediate) {
                if (this.mHorizontal) {
                    scrollBy(delta, 0);
                } else {
                    scrollBy(0, delta);
                }
            } else if (this.mHorizontal) {
                smoothScrollBy(delta, 0);
            } else {
                smoothScrollBy(0, delta);
            }
        }
        return scroll;
    }

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return this.mHorizontal ? computeScrollDeltaToGetChildRectOnScreenHorizontal(rect) : computeScrollDeltaToGetChildRectOnScreenVertical(rect);
    }

    private int computeScrollDeltaToGetChildRectOnScreenVertical(Rect rect) {
        if (getChildCount() == 0) {
            return 0;
        }
        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;
        int fadingEdge = getVerticalFadingEdgeLength();
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }
        if (rect.bottom < getChildAt(0).getHeight()) {
            screenBottom -= fadingEdge;
        }
        if (rect.bottom > screenBottom && rect.top > screenTop) {
            int scrollYDelta = rect.height() > height ? 0 + (rect.top - screenTop) : 0 + (rect.bottom - screenBottom);
            int bottom = getChildAt(0).getBottom();
            int distanceToBottom = bottom - screenBottom;
            return Math.min(scrollYDelta, distanceToBottom);
        }
        if (rect.top >= screenTop || rect.bottom >= screenBottom) {
            return 0;
        }
        int scrollYDelta2 = rect.height() > height ? 0 - (screenBottom - rect.bottom) : 0 - (screenTop - rect.top);
        return Math.max(scrollYDelta2, -getScrollY());
    }

    private int computeScrollDeltaToGetChildRectOnScreenHorizontal(Rect rect) {
        if (getChildCount() == 0) {
            return 0;
        }
        int width = getWidth();
        int screenLeft = getScrollX();
        int screenRight = screenLeft + width;
        int fadingEdge = getHorizontalFadingEdgeLength();
        if (rect.left > 0) {
            screenLeft += fadingEdge;
        }
        if (rect.right < getChildAt(0).getWidth()) {
            screenRight -= fadingEdge;
        }
        if (rect.right > screenRight && rect.left > screenLeft) {
            int scrollXDelta = rect.width() > width ? 0 + (rect.left - screenLeft) : 0 + (rect.right - screenRight);
            int right = getChildAt(0).getRight();
            int distanceToRight = right - screenRight;
            return Math.min(scrollXDelta, distanceToRight);
        }
        if (rect.left >= screenLeft || rect.right >= screenRight) {
            return 0;
        }
        int scrollXDelta2 = rect.width() > width ? 0 - (screenRight - rect.right) : 0 - (screenLeft - rect.left);
        return Math.max(scrollXDelta2, -getScrollX());
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (!this.mIsLayoutDirty) {
            scrollToChild(focused);
        } else {
            this.mChildToScrollTo = focused;
        }
        super.requestChildFocus(child, focused);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (this.mHorizontal) {
            if (direction == 2) {
                direction = 66;
            } else if (direction == 1) {
                direction = 17;
            }
        } else if (direction == 2) {
            direction = 130;
        } else if (direction == 1) {
            direction = 33;
        }
        View nextFocus = previouslyFocusedRect == null ? FocusFinder.getInstance().findNextFocus(this, null, direction) : FocusFinder.getInstance().findNextFocusFromRect(this, previouslyFocusedRect, direction);
        if (nextFocus == null || isOffScreen(nextFocus)) {
            return false;
        }
        return nextFocus.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        rectangle.offset(child.getLeft() - child.getScrollX(), child.getTop() - child.getScrollY());
        return scrollToChildRect(rectangle, immediate);
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
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.mIsLayoutDirty = false;
        if (this.mChildToScrollTo != null && isViewDescendantOf(this.mChildToScrollTo, this)) {
            scrollToChild(this.mChildToScrollTo);
        }
        this.mChildToScrollTo = null;
        scrollTo(this.mScrollX, this.mScrollY);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        View currentFocused = findFocus();
        if (currentFocused != null && this != currentFocused && isWithinDeltaOfScreen(currentFocused, 0, oldh)) {
            currentFocused.getDrawingRect(this.mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, this.mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(this.mTempRect);
            doScrollY(scrollDelta);
        }
    }

    private boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }
        Object parent2 = child.getParent();
        return (parent2 instanceof ViewGroup) && isViewDescendantOf((View) parent2, parent);
    }

    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            if (this.mHorizontal) {
                int width = (getWidth() - this.mPaddingRight) - this.mPaddingLeft;
                int right = getChildAt(0).getWidth();
                this.mScroller.fling(this.mScrollX, this.mScrollY, velocityY, 0, 0, Math.max(0, right - width), 0, 0, width / 2, 0);
            } else {
                int height = (getHeight() - this.mPaddingBottom) - this.mPaddingTop;
                int bottom = getChildAt(0).getHeight();
                this.mScroller.fling(this.mScrollX, this.mScrollY, 0, velocityY, 0, 0, 0, Math.max(0, bottom - height), 0, height / 2);
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
    public void scrollTo(int x, int y) {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            int x2 = clamp(x, (getWidth() - this.mPaddingRight) - this.mPaddingLeft, child.getWidth());
            int y2 = clamp(y, (getHeight() - this.mPaddingBottom) - this.mPaddingTop, child.getHeight());
            if (x2 != this.mScrollX || y2 != this.mScrollY) {
                super.scrollTo(x2, y2);
            }
        }
    }

    private int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            return 0;
        }
        if (my + n > child) {
            return child - my;
        }
        return n;
    }
}
