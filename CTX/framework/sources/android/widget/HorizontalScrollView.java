package android.widget;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.TtmlUtils;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import android.view.ViewHierarchyEncoder;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import java.util.ArrayList;

public class HorizontalScrollView extends FrameLayout {
    private static final int ANIMATED_SCROLL_GAP = 250;
    private static final int INVALID_POINTER = -1;
    private static final float MAX_SCROLL_FACTOR = 0.5f;
    private static final String TAG = "HorizontalScrollView";
    private int mActivePointerId;
    private View mChildToScrollTo;
    private EdgeEffect mEdgeGlowLeft;
    private EdgeEffect mEdgeGlowRight;

    @ViewDebug.ExportedProperty(category = TtmlUtils.TAG_LAYOUT)
    private boolean mFillViewport;
    private float mHorizontalScrollFactor;
    private boolean mIsBeingDragged;
    private boolean mIsLayoutDirty;
    private int mLastMotionX;
    private long mLastScroll;
    private int mMaximumVelocity;
    private int mMinimumVelocity;
    private int mOverflingDistance;
    private int mOverscrollDistance;
    private SavedState mSavedState;
    private OverScroller mScroller;
    private boolean mSmoothScrollingEnabled;
    private final Rect mTempRect;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;

    public HorizontalScrollView(Context context) {
        this(context, null);
    }

    public HorizontalScrollView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16843603);
    }

    public HorizontalScrollView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public HorizontalScrollView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTempRect = new Rect();
        this.mIsLayoutDirty = true;
        this.mChildToScrollTo = null;
        this.mIsBeingDragged = false;
        this.mSmoothScrollingEnabled = true;
        this.mActivePointerId = -1;
        initScrollView();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.HorizontalScrollView, i, i2);
        setFillViewport(typedArrayObtainStyledAttributes.getBoolean(0, false));
        typedArrayObtainStyledAttributes.recycle();
        if (context.getResources().getConfiguration().uiMode == 6) {
            setRevealOnFocusHint(false);
        }
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }
        int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength();
        if (this.mScrollX < horizontalFadingEdgeLength) {
            return this.mScrollX / horizontalFadingEdgeLength;
        }
        return 1.0f;
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }
        int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength();
        int right = (getChildAt(0).getRight() - this.mScrollX) - (getWidth() - this.mPaddingRight);
        if (right < horizontalFadingEdgeLength) {
            return right / horizontalFadingEdgeLength;
        }
        return 1.0f;
    }

    public int getMaxScrollAmount() {
        return (int) (MAX_SCROLL_FACTOR * (this.mRight - this.mLeft));
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
        this.mHorizontalScrollFactor = viewConfiguration.getScaledHorizontalScrollFactor();
    }

    @Override
    public void addView(View view) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("HorizontalScrollView can host only one direct child");
        }
        super.addView(view);
    }

    @Override
    public void addView(View view, int i) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("HorizontalScrollView can host only one direct child");
        }
        super.addView(view, i);
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams layoutParams) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("HorizontalScrollView can host only one direct child");
        }
        super.addView(view, layoutParams);
    }

    @Override
    public void addView(View view, int i, ViewGroup.LayoutParams layoutParams) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("HorizontalScrollView can host only one direct child");
        }
        super.addView(view, i, layoutParams);
    }

    private boolean canScroll() {
        View childAt = getChildAt(0);
        if (childAt != null) {
            return getWidth() < (childAt.getWidth() + this.mPaddingLeft) + this.mPaddingRight;
        }
        return false;
    }

    public boolean isFillViewport() {
        return this.mFillViewport;
    }

    public void setFillViewport(boolean z) {
        if (z != this.mFillViewport) {
            this.mFillViewport = z;
            requestLayout();
        }
    }

    public boolean isSmoothScrollingEnabled() {
        return this.mSmoothScrollingEnabled;
    }

    public void setSmoothScrollingEnabled(boolean z) {
        this.mSmoothScrollingEnabled = z;
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int i3;
        int i4;
        super.onMeasure(i, i2);
        if (this.mFillViewport && View.MeasureSpec.getMode(i) != 0 && getChildCount() > 0) {
            View childAt = getChildAt(0);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) childAt.getLayoutParams();
            if (getContext().getApplicationInfo().targetSdkVersion >= 23) {
                i3 = this.mPaddingLeft + this.mPaddingRight + layoutParams.leftMargin + layoutParams.rightMargin;
                i4 = this.mPaddingTop + this.mPaddingBottom + layoutParams.topMargin + layoutParams.bottomMargin;
            } else {
                i3 = this.mPaddingLeft + this.mPaddingRight;
                i4 = this.mPaddingTop + this.mPaddingBottom;
            }
            int measuredWidth = getMeasuredWidth() - i3;
            if (childAt.getMeasuredWidth() < measuredWidth) {
                childAt.measure(View.MeasureSpec.makeMeasureSpec(measuredWidth, 1073741824), getChildMeasureSpec(i2, i4, layoutParams.height));
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
            if (!isFocused()) {
                return false;
            }
            View viewFindFocus = findFocus();
            if (viewFindFocus == this) {
                viewFindFocus = null;
            }
            View viewFindNextFocus = FocusFinder.getInstance().findNextFocus(this, viewFindFocus, 66);
            return (viewFindNextFocus == null || viewFindNextFocus == this || !viewFindNextFocus.requestFocus(66)) ? false : true;
        }
        if (keyEvent.getAction() != 0) {
            return false;
        }
        int keyCode = keyEvent.getKeyCode();
        if (keyCode != 62) {
            switch (keyCode) {
                case 21:
                    if (!keyEvent.isAltPressed()) {
                    }
                    break;
                case 22:
                    if (!keyEvent.isAltPressed()) {
                    }
                    break;
            }
            return false;
        }
        pageScroll(keyEvent.isShiftPressed() ? 17 : 66);
        return false;
    }

    private boolean inChild(int i, int i2) {
        if (getChildCount() <= 0) {
            return false;
        }
        int i3 = this.mScrollX;
        View childAt = getChildAt(0);
        return i2 >= childAt.getTop() && i2 < childAt.getBottom() && i >= childAt.getLeft() - i3 && i < childAt.getRight() - i3;
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
        if ((action == 2 && this.mIsBeingDragged) || super.onInterceptTouchEvent(motionEvent)) {
            return true;
        }
        switch (action & 255) {
            case 0:
                int x = (int) motionEvent.getX();
                if (!inChild(x, (int) motionEvent.getY())) {
                    this.mIsBeingDragged = false;
                    recycleVelocityTracker();
                } else {
                    this.mLastMotionX = x;
                    this.mActivePointerId = motionEvent.getPointerId(0);
                    initOrResetVelocityTracker();
                    this.mVelocityTracker.addMovement(motionEvent);
                    this.mIsBeingDragged = !this.mScroller.isFinished();
                }
                break;
            case 1:
            case 3:
                this.mIsBeingDragged = false;
                this.mActivePointerId = -1;
                if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, getScrollRange(), 0, 0)) {
                    postInvalidateOnAnimation();
                }
                break;
            case 2:
                int i = this.mActivePointerId;
                if (i != -1) {
                    int iFindPointerIndex = motionEvent.findPointerIndex(i);
                    if (iFindPointerIndex == -1) {
                        Log.e(TAG, "Invalid pointerId=" + i + " in onInterceptTouchEvent");
                    } else {
                        int x2 = (int) motionEvent.getX(iFindPointerIndex);
                        if (Math.abs(x2 - this.mLastMotionX) > this.mTouchSlop) {
                            this.mIsBeingDragged = true;
                            this.mLastMotionX = x2;
                            initVelocityTrackerIfNotExists();
                            this.mVelocityTracker.addMovement(motionEvent);
                            if (this.mParent != null) {
                                this.mParent.requestDisallowInterceptTouchEvent(true);
                            }
                        }
                    }
                }
                break;
            case 5:
                int actionIndex = motionEvent.getActionIndex();
                this.mLastMotionX = (int) motionEvent.getX(actionIndex);
                this.mActivePointerId = motionEvent.getPointerId(actionIndex);
                break;
            case 6:
                onSecondaryPointerUp(motionEvent);
                this.mLastMotionX = (int) motionEvent.getX(motionEvent.findPointerIndex(this.mActivePointerId));
                break;
        }
        return this.mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        ViewParent parent;
        initVelocityTrackerIfNotExists();
        this.mVelocityTracker.addMovement(motionEvent);
        int action = motionEvent.getAction() & 255;
        if (action != 6) {
            switch (action) {
                case 0:
                    if (getChildCount() == 0) {
                        return false;
                    }
                    boolean z = !this.mScroller.isFinished();
                    this.mIsBeingDragged = z;
                    if (z && (parent = getParent()) != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    if (!this.mScroller.isFinished()) {
                        this.mScroller.abortAnimation();
                    }
                    this.mLastMotionX = (int) motionEvent.getX();
                    this.mActivePointerId = motionEvent.getPointerId(0);
                    return true;
                case 1:
                    if (this.mIsBeingDragged) {
                        VelocityTracker velocityTracker = this.mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                        int xVelocity = (int) velocityTracker.getXVelocity(this.mActivePointerId);
                        if (getChildCount() > 0) {
                            if (Math.abs(xVelocity) > this.mMinimumVelocity) {
                                fling(-xVelocity);
                            } else if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, getScrollRange(), 0, 0)) {
                                postInvalidateOnAnimation();
                            }
                        }
                        this.mActivePointerId = -1;
                        this.mIsBeingDragged = false;
                        recycleVelocityTracker();
                        if (this.mEdgeGlowLeft != null) {
                            this.mEdgeGlowLeft.onRelease();
                            this.mEdgeGlowRight.onRelease();
                            return true;
                        }
                        return true;
                    }
                    return true;
                case 2:
                    int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                    if (iFindPointerIndex == -1) {
                        Log.e(TAG, "Invalid pointerId=" + this.mActivePointerId + " in onTouchEvent");
                        return true;
                    }
                    int x = (int) motionEvent.getX(iFindPointerIndex);
                    int i = this.mLastMotionX - x;
                    if (!this.mIsBeingDragged && Math.abs(i) > this.mTouchSlop) {
                        ViewParent parent2 = getParent();
                        if (parent2 != null) {
                            parent2.requestDisallowInterceptTouchEvent(true);
                        }
                        this.mIsBeingDragged = true;
                        i = i > 0 ? i - this.mTouchSlop : i + this.mTouchSlop;
                    }
                    int i2 = i;
                    if (this.mIsBeingDragged) {
                        this.mLastMotionX = x;
                        int i3 = this.mScrollX;
                        int i4 = this.mScrollY;
                        int scrollRange = getScrollRange();
                        int overScrollMode = getOverScrollMode();
                        boolean z2 = overScrollMode == 0 || (overScrollMode == 1 && scrollRange > 0);
                        if (overScrollBy(i2, 0, this.mScrollX, 0, scrollRange, 0, this.mOverscrollDistance, 0, true)) {
                            this.mVelocityTracker.clear();
                        }
                        if (z2) {
                            int i5 = i3 + i2;
                            if (i5 < 0) {
                                this.mEdgeGlowLeft.onPull(i2 / getWidth(), 1.0f - (motionEvent.getY(iFindPointerIndex) / getHeight()));
                                if (!this.mEdgeGlowRight.isFinished()) {
                                    this.mEdgeGlowRight.onRelease();
                                }
                            } else if (i5 > scrollRange) {
                                this.mEdgeGlowRight.onPull(i2 / getWidth(), motionEvent.getY(iFindPointerIndex) / getHeight());
                                if (!this.mEdgeGlowLeft.isFinished()) {
                                    this.mEdgeGlowLeft.onRelease();
                                }
                            }
                            if (this.mEdgeGlowLeft != null) {
                                if (!this.mEdgeGlowLeft.isFinished() || !this.mEdgeGlowRight.isFinished()) {
                                    postInvalidateOnAnimation();
                                    return true;
                                }
                                return true;
                            }
                            return true;
                        }
                        return true;
                    }
                    return true;
                case 3:
                    if (this.mIsBeingDragged && getChildCount() > 0) {
                        if (this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, getScrollRange(), 0, 0)) {
                            postInvalidateOnAnimation();
                        }
                        this.mActivePointerId = -1;
                        this.mIsBeingDragged = false;
                        recycleVelocityTracker();
                        if (this.mEdgeGlowLeft != null) {
                            this.mEdgeGlowLeft.onRelease();
                            this.mEdgeGlowRight.onRelease();
                            return true;
                        }
                        return true;
                    }
                    return true;
                default:
                    return true;
            }
        }
        onSecondaryPointerUp(motionEvent);
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent motionEvent) {
        int action = (motionEvent.getAction() & 65280) >> 8;
        if (motionEvent.getPointerId(action) == this.mActivePointerId) {
            int i = action == 0 ? 1 : 0;
            this.mLastMotionX = (int) motionEvent.getX(i);
            this.mActivePointerId = motionEvent.getPointerId(i);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        float axisValue;
        if (motionEvent.getAction() == 8 && !this.mIsBeingDragged) {
            if (motionEvent.isFromSource(2)) {
                if ((motionEvent.getMetaState() & 1) != 0) {
                    axisValue = -motionEvent.getAxisValue(9);
                } else {
                    axisValue = motionEvent.getAxisValue(10);
                }
            } else if (motionEvent.isFromSource(4194304)) {
                axisValue = motionEvent.getAxisValue(26);
            } else {
                axisValue = 0.0f;
            }
            int iRound = Math.round(axisValue * this.mHorizontalScrollFactor);
            if (iRound != 0) {
                int scrollRange = getScrollRange();
                int i = this.mScrollX;
                int i2 = iRound + i;
                if (i2 < 0) {
                    scrollRange = 0;
                } else if (i2 <= scrollRange) {
                    scrollRange = i2;
                }
                if (scrollRange != i) {
                    super.scrollTo(scrollRange, this.mScrollY);
                    return true;
                }
            }
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    protected void onOverScrolled(int i, int i2, boolean z, boolean z2) {
        if (!this.mScroller.isFinished()) {
            int i3 = this.mScrollX;
            int i4 = this.mScrollY;
            this.mScrollX = i;
            this.mScrollY = i2;
            invalidateParentIfNeeded();
            onScrollChanged(this.mScrollX, this.mScrollY, i3, i4);
            if (z) {
                this.mScroller.springBack(this.mScrollX, this.mScrollY, 0, getScrollRange(), 0, 0);
            }
        } else {
            super.scrollTo(i, i2);
        }
        awakenScrollBars();
    }

    @Override
    public boolean performAccessibilityActionInternal(int i, Bundle bundle) {
        if (super.performAccessibilityActionInternal(i, bundle)) {
            return true;
        }
        if (i != 4096) {
            if (i == 8192 || i == 16908345) {
                if (!isEnabled()) {
                    return false;
                }
                int iMax = Math.max(0, this.mScrollX - ((getWidth() - this.mPaddingLeft) - this.mPaddingRight));
                if (iMax == this.mScrollX) {
                    return false;
                }
                smoothScrollTo(iMax, 0);
                return true;
            }
            if (i != 16908347) {
                return false;
            }
        }
        if (!isEnabled()) {
            return false;
        }
        int iMin = Math.min(this.mScrollX + ((getWidth() - this.mPaddingLeft) - this.mPaddingRight), getScrollRange());
        if (iMin == this.mScrollX) {
            return false;
        }
        smoothScrollTo(iMin, 0);
        return true;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return HorizontalScrollView.class.getName();
    }

    @Override
    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        int scrollRange = getScrollRange();
        if (scrollRange > 0) {
            accessibilityNodeInfo.setScrollable(true);
            if (isEnabled() && this.mScrollX > 0) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT);
            }
            if (isEnabled() && this.mScrollX < scrollRange) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT);
            }
        }
    }

    @Override
    public void onInitializeAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEventInternal(accessibilityEvent);
        accessibilityEvent.setScrollable(getScrollRange() > 0);
        accessibilityEvent.setScrollX(this.mScrollX);
        accessibilityEvent.setScrollY(this.mScrollY);
        accessibilityEvent.setMaxScrollX(getScrollRange());
        accessibilityEvent.setMaxScrollY(this.mScrollY);
    }

    private int getScrollRange() {
        if (getChildCount() > 0) {
            return Math.max(0, getChildAt(0).getWidth() - ((getWidth() - this.mPaddingLeft) - this.mPaddingRight));
        }
        return 0;
    }

    private View findFocusableViewInMyBounds(boolean z, int i, View view) {
        int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength() / 2;
        int i2 = i + horizontalFadingEdgeLength;
        int width = (i + getWidth()) - horizontalFadingEdgeLength;
        if (view != null && view.getLeft() < width && view.getRight() > i2) {
            return view;
        }
        return findFocusableViewInBounds(z, i2, width);
    }

    private View findFocusableViewInBounds(boolean z, int i, int i2) {
        ArrayList<View> focusables = getFocusables(2);
        int size = focusables.size();
        boolean z2 = false;
        View view = null;
        for (int i3 = 0; i3 < size; i3++) {
            View view2 = focusables.get(i3);
            int left = view2.getLeft();
            int right = view2.getRight();
            if (i < right && left < i2) {
                boolean z3 = i < left && right < i2;
                if (view == null) {
                    view = view2;
                    z2 = z3;
                } else {
                    boolean z4 = (z && left < view.getLeft()) || (!z && right > view.getRight());
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
        boolean z = i == 66;
        int width = getWidth();
        if (z) {
            this.mTempRect.left = getScrollX() + width;
            if (getChildCount() > 0) {
                View childAt = getChildAt(0);
                if (this.mTempRect.left + width > childAt.getRight()) {
                    this.mTempRect.left = childAt.getRight() - width;
                }
            }
        } else {
            this.mTempRect.left = getScrollX() - width;
            if (this.mTempRect.left < 0) {
                this.mTempRect.left = 0;
            }
        }
        this.mTempRect.right = this.mTempRect.left + width;
        return scrollAndFocus(i, this.mTempRect.left, this.mTempRect.right);
    }

    public boolean fullScroll(int i) {
        boolean z = i == 66;
        int width = getWidth();
        this.mTempRect.left = 0;
        this.mTempRect.right = width;
        if (z && getChildCount() > 0) {
            this.mTempRect.right = getChildAt(0).getRight();
            this.mTempRect.left = this.mTempRect.right - width;
        }
        return scrollAndFocus(i, this.mTempRect.left, this.mTempRect.right);
    }

    private boolean scrollAndFocus(int i, int i2, int i3) {
        int width = getWidth();
        int scrollX = getScrollX();
        int i4 = width + scrollX;
        boolean z = false;
        boolean z2 = i == 17;
        View viewFindFocusableViewInBounds = findFocusableViewInBounds(z2, i2, i3);
        if (viewFindFocusableViewInBounds == null) {
            viewFindFocusableViewInBounds = this;
        }
        if (i2 < scrollX || i3 > i4) {
            doScrollX(z2 ? i2 - scrollX : i3 - i4);
            z = true;
        }
        if (viewFindFocusableViewInBounds != findFocus()) {
            viewFindFocusableViewInBounds.requestFocus(i);
        }
        return z;
    }

    public boolean arrowScroll(int i) {
        int right;
        View viewFindFocus = findFocus();
        if (viewFindFocus == this) {
            viewFindFocus = null;
        }
        View viewFindNextFocus = FocusFinder.getInstance().findNextFocus(this, viewFindFocus, i);
        int maxScrollAmount = getMaxScrollAmount();
        if (viewFindNextFocus != null && isWithinDeltaOfScreen(viewFindNextFocus, maxScrollAmount)) {
            viewFindNextFocus.getDrawingRect(this.mTempRect);
            offsetDescendantRectToMyCoords(viewFindNextFocus, this.mTempRect);
            doScrollX(computeScrollDeltaToGetChildRectOnScreen(this.mTempRect));
            viewFindNextFocus.requestFocus(i);
        } else {
            if (i == 17 && getScrollX() < maxScrollAmount) {
                maxScrollAmount = getScrollX();
            } else if (i == 66 && getChildCount() > 0 && (right = getChildAt(0).getRight() - (getScrollX() + getWidth())) < maxScrollAmount) {
                maxScrollAmount = right;
            }
            if (maxScrollAmount == 0) {
                return false;
            }
            if (i != 66) {
                maxScrollAmount = -maxScrollAmount;
            }
            doScrollX(maxScrollAmount);
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

    private boolean isOffScreen(View view) {
        return !isWithinDeltaOfScreen(view, 0);
    }

    private boolean isWithinDeltaOfScreen(View view, int i) {
        view.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(view, this.mTempRect);
        return this.mTempRect.right + i >= getScrollX() && this.mTempRect.left - i <= getScrollX() + getWidth();
    }

    private void doScrollX(int i) {
        if (i != 0) {
            if (this.mSmoothScrollingEnabled) {
                smoothScrollBy(i, 0);
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
            int iMax = Math.max(0, getChildAt(0).getWidth() - ((getWidth() - this.mPaddingRight) - this.mPaddingLeft));
            int i3 = this.mScrollX;
            this.mScroller.startScroll(i3, this.mScrollY, Math.max(0, Math.min(i + i3, iMax)) - i3, 0);
            postInvalidateOnAnimation();
        } else {
            if (!this.mScroller.isFinished()) {
                this.mScroller.abortAnimation();
            }
            scrollBy(i, i2);
        }
        this.mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    public final void smoothScrollTo(int i, int i2) {
        smoothScrollBy(i - this.mScrollX, i2 - this.mScrollY);
    }

    @Override
    protected int computeHorizontalScrollRange() {
        int childCount = getChildCount();
        int width = (getWidth() - this.mPaddingLeft) - this.mPaddingRight;
        if (childCount == 0) {
            return width;
        }
        int right = getChildAt(0).getRight();
        int i = this.mScrollX;
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
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    @Override
    protected void measureChild(View view, int i, int i2) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        view.measure(View.MeasureSpec.makeSafeMeasureSpec(Math.max(0, View.MeasureSpec.getSize(i) - (this.mPaddingLeft + this.mPaddingRight)), 0), getChildMeasureSpec(i2, this.mPaddingTop + this.mPaddingBottom, layoutParams.height));
    }

    @Override
    protected void measureChildWithMargins(View view, int i, int i2, int i3, int i4) {
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        view.measure(View.MeasureSpec.makeSafeMeasureSpec(Math.max(0, View.MeasureSpec.getSize(i) - ((((this.mPaddingLeft + this.mPaddingRight) + marginLayoutParams.leftMargin) + marginLayoutParams.rightMargin) + i2)), 0), getChildMeasureSpec(i3, this.mPaddingTop + this.mPaddingBottom + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin + i4, marginLayoutParams.height));
    }

    @Override
    public void computeScroll() {
        if (this.mScroller.computeScrollOffset()) {
            int i = this.mScrollX;
            int i2 = this.mScrollY;
            int currX = this.mScroller.getCurrX();
            int currY = this.mScroller.getCurrY();
            if (i != currX || i2 != currY) {
                int scrollRange = getScrollRange();
                int overScrollMode = getOverScrollMode();
                boolean z = true;
                if (overScrollMode != 0 && (overScrollMode != 1 || scrollRange <= 0)) {
                    z = false;
                }
                boolean z2 = z;
                overScrollBy(currX - i, currY - i2, i, i2, scrollRange, 0, this.mOverflingDistance, 0, false);
                onScrollChanged(this.mScrollX, this.mScrollY, i, i2);
                if (z2) {
                    if (currX < 0 && i >= 0) {
                        this.mEdgeGlowLeft.onAbsorb((int) this.mScroller.getCurrVelocity());
                    } else if (currX > scrollRange && i <= scrollRange) {
                        this.mEdgeGlowRight.onAbsorb((int) this.mScroller.getCurrVelocity());
                    }
                }
            }
            if (!awakenScrollBars()) {
                postInvalidateOnAnimation();
            }
        }
    }

    private void scrollToChild(View view) {
        view.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(view, this.mTempRect);
        int iComputeScrollDeltaToGetChildRectOnScreen = computeScrollDeltaToGetChildRectOnScreen(this.mTempRect);
        if (iComputeScrollDeltaToGetChildRectOnScreen != 0) {
            scrollBy(iComputeScrollDeltaToGetChildRectOnScreen, 0);
        }
    }

    private boolean scrollToChildRect(Rect rect, boolean z) {
        int iComputeScrollDeltaToGetChildRectOnScreen = computeScrollDeltaToGetChildRectOnScreen(rect);
        boolean z2 = iComputeScrollDeltaToGetChildRectOnScreen != 0;
        if (z2) {
            if (z) {
                scrollBy(iComputeScrollDeltaToGetChildRectOnScreen, 0);
            } else {
                smoothScrollBy(iComputeScrollDeltaToGetChildRectOnScreen, 0);
            }
        }
        return z2;
    }

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
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
        if (view2 != null && view2.getRevealOnFocusHint()) {
            if (!this.mIsLayoutDirty) {
                scrollToChild(view2);
            } else {
                this.mChildToScrollTo = view2;
            }
        }
        super.requestChildFocus(view, view2);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int i, Rect rect) {
        View viewFindNextFocusFromRect;
        if (i == 2) {
            i = 66;
        } else if (i == 1) {
            i = 17;
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
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int measuredWidth;
        int i5;
        int i6;
        if (getChildCount() > 0) {
            measuredWidth = getChildAt(0).getMeasuredWidth();
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getChildAt(0).getLayoutParams();
            i5 = layoutParams.rightMargin + layoutParams.leftMargin;
        } else {
            measuredWidth = 0;
            i5 = 0;
        }
        int i7 = i3 - i;
        layoutChildren(i, i2, i3, i4, measuredWidth > ((i7 - getPaddingLeftWithForeground()) - getPaddingRightWithForeground()) - i5);
        this.mIsLayoutDirty = false;
        if (this.mChildToScrollTo != null && isViewDescendantOf(this.mChildToScrollTo, this)) {
            scrollToChild(this.mChildToScrollTo);
        }
        this.mChildToScrollTo = null;
        if (!isLaidOut()) {
            int iMax = Math.max(0, measuredWidth - ((i7 - this.mPaddingLeft) - this.mPaddingRight));
            if (this.mSavedState != null) {
                if (isLayoutRtl()) {
                    i6 = iMax - this.mSavedState.scrollOffsetFromStart;
                } else {
                    i6 = this.mSavedState.scrollOffsetFromStart;
                }
                this.mScrollX = i6;
                this.mSavedState = null;
            } else if (isLayoutRtl()) {
                this.mScrollX = iMax - this.mScrollX;
            }
            if (this.mScrollX > iMax) {
                this.mScrollX = iMax;
            } else if (this.mScrollX < 0) {
                this.mScrollX = 0;
            }
        }
        scrollTo(this.mScrollX, this.mScrollY);
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        View viewFindFocus = findFocus();
        if (viewFindFocus != null && this != viewFindFocus && isWithinDeltaOfScreen(viewFindFocus, this.mRight - this.mLeft)) {
            viewFindFocus.getDrawingRect(this.mTempRect);
            offsetDescendantRectToMyCoords(viewFindFocus, this.mTempRect);
            doScrollX(computeScrollDeltaToGetChildRectOnScreen(this.mTempRect));
        }
    }

    private static boolean isViewDescendantOf(View view, View view2) {
        if (view == view2) {
            return true;
        }
        Object parent = view.getParent();
        return (parent instanceof ViewGroup) && isViewDescendantOf((View) parent, view2);
    }

    public void fling(int i) {
        if (getChildCount() > 0) {
            int width = (getWidth() - this.mPaddingRight) - this.mPaddingLeft;
            this.mScroller.fling(this.mScrollX, this.mScrollY, i, 0, 0, Math.max(0, getChildAt(0).getWidth() - width), 0, 0, width / 2, 0);
            boolean z = i > 0;
            View viewFindFocus = findFocus();
            View viewFindFocusableViewInMyBounds = findFocusableViewInMyBounds(z, this.mScroller.getFinalX(), viewFindFocus);
            if (viewFindFocusableViewInMyBounds == null) {
                viewFindFocusableViewInMyBounds = this;
            }
            if (viewFindFocusableViewInMyBounds != viewFindFocus) {
                viewFindFocusableViewInMyBounds.requestFocus(z ? 66 : 17);
            }
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void scrollTo(int i, int i2) {
        if (getChildCount() > 0) {
            View childAt = getChildAt(0);
            int iClamp = clamp(i, (getWidth() - this.mPaddingRight) - this.mPaddingLeft, childAt.getWidth());
            int iClamp2 = clamp(i2, (getHeight() - this.mPaddingBottom) - this.mPaddingTop, childAt.getHeight());
            if (iClamp != this.mScrollX || iClamp2 != this.mScrollY) {
                super.scrollTo(iClamp, iClamp2);
            }
        }
    }

    @Override
    public void setOverScrollMode(int i) {
        if (i != 2) {
            if (this.mEdgeGlowLeft == null) {
                Context context = getContext();
                this.mEdgeGlowLeft = new EdgeEffect(context);
                this.mEdgeGlowRight = new EdgeEffect(context);
            }
        } else {
            this.mEdgeGlowLeft = null;
            this.mEdgeGlowRight = null;
        }
        super.setOverScrollMode(i);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.mEdgeGlowLeft != null) {
            int i = this.mScrollX;
            if (!this.mEdgeGlowLeft.isFinished()) {
                int iSave = canvas.save();
                int height = (getHeight() - this.mPaddingTop) - this.mPaddingBottom;
                canvas.rotate(270.0f);
                canvas.translate((-height) + this.mPaddingTop, Math.min(0, i));
                this.mEdgeGlowLeft.setSize(height, getWidth());
                if (this.mEdgeGlowLeft.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restoreToCount(iSave);
            }
            if (!this.mEdgeGlowRight.isFinished()) {
                int iSave2 = canvas.save();
                int width = getWidth();
                int height2 = (getHeight() - this.mPaddingTop) - this.mPaddingBottom;
                canvas.rotate(90.0f);
                canvas.translate(-this.mPaddingTop, -(Math.max(getScrollRange(), i) + width));
                this.mEdgeGlowRight.setSize(height2, width);
                if (this.mEdgeGlowRight.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restoreToCount(iSave2);
            }
        }
    }

    private static int clamp(int i, int i2, int i3) {
        if (i2 >= i3 || i < 0) {
            return 0;
        }
        if (i2 + i > i3) {
            return i3 - i2;
        }
        return i;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (this.mContext.getApplicationInfo().targetSdkVersion <= 18) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mSavedState = savedState;
        requestLayout();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        if (this.mContext.getApplicationInfo().targetSdkVersion <= 18) {
            return super.onSaveInstanceState();
        }
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.scrollOffsetFromStart = isLayoutRtl() ? -this.mScrollX : this.mScrollX;
        return savedState;
    }

    @Override
    protected void encodeProperties(ViewHierarchyEncoder viewHierarchyEncoder) {
        super.encodeProperties(viewHierarchyEncoder);
        viewHierarchyEncoder.addProperty("layout:fillViewPort", this.mFillViewport);
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        public int scrollOffsetFromStart;

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        public SavedState(Parcel parcel) {
            super(parcel);
            this.scrollOffsetFromStart = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.scrollOffsetFromStart);
        }

        public String toString() {
            return "HorizontalScrollView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " scrollPosition=" + this.scrollOffsetFromStart + "}";
        }
    }
}
