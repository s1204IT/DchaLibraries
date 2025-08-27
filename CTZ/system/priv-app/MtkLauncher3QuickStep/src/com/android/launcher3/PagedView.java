package com.android.launcher3;

import android.animation.LayoutTransition;
import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.widget.ScrollView;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.compat.AccessibilityManagerCompat;
import com.android.launcher3.pageindicators.PageIndicator;
import com.android.launcher3.touch.OverScroll;
import java.util.ArrayList;

/* loaded from: classes.dex */
public abstract class PagedView<T extends View & PageIndicator> extends ViewGroup {
    private static final boolean DEBUG = false;
    private static final int FLING_THRESHOLD_VELOCITY = 500;
    protected static final int INVALID_PAGE = -1;
    protected static final int INVALID_POINTER = -1;
    public static final int INVALID_RESTORE_PAGE = -1001;
    private static final float MAX_SCROLL_PROGRESS = 1.0f;
    private static final int MIN_FLING_VELOCITY = 250;
    private static final int MIN_SNAP_VELOCITY = 1500;
    private static final int OVERSCROLL_PAGE_SNAP_ANIMATION_DURATION = 270;
    public static final int PAGE_SNAP_ANIMATION_DURATION = 750;
    private static final float RETURN_TO_ORIGINAL_PAGE_THRESHOLD = 0.33f;
    private static final float SIGNIFICANT_MOVE_THRESHOLD = 0.4f;
    public static final int SLOW_PAGE_SNAP_ANIMATION_DURATION = 950;
    private static final String TAG = "PagedView";
    protected static final int TOUCH_STATE_NEXT_PAGE = 3;
    protected static final int TOUCH_STATE_PREV_PAGE = 2;
    protected static final int TOUCH_STATE_REST = 0;
    protected static final int TOUCH_STATE_SCROLLING = 1;
    protected int mActivePointerId;
    protected boolean mAllowOverScroll;

    @ViewDebug.ExportedProperty(category = "launcher")
    protected int mCurrentPage;
    private Interpolator mDefaultInterpolator;
    private float mDownMotionX;
    private float mDownMotionY;
    protected boolean mFirstLayout;
    protected int mFlingThresholdVelocity;
    private boolean mFreeScroll;
    protected final Rect mInsets;
    protected boolean mIsLayoutValid;
    protected boolean mIsPageInTransition;
    protected boolean mIsRtl;
    private float mLastMotionX;
    private float mLastMotionXRemainder;
    protected int mMaxScrollX;
    private int mMaximumVelocity;
    protected int mMinFlingVelocity;
    protected int mMinSnapVelocity;

    @ViewDebug.ExportedProperty(category = "launcher")
    protected int mNextPage;
    protected int mOverScrollX;
    protected T mPageIndicator;
    int mPageIndicatorViewId;
    protected int[] mPageScrolls;
    protected int mPageSpacing;
    protected LauncherScroller mScroller;
    private boolean mSettleOnPageInFreeScroll;
    private int[] mTmpIntPair;
    private float mTotalMotionX;
    protected int mTouchSlop;
    protected int mTouchState;
    protected int mUnboundedScrollX;
    private VelocityTracker mVelocityTracker;
    protected boolean mWasInOverscroll;
    protected static final ComputePageScrollsLogic SIMPLE_SCROLL_LOGIC = new ComputePageScrollsLogic() { // from class: com.android.launcher3.-$$Lambda$PagedView$8WXZR5QBUDgMQgE75xb-eR6qPZg
        @Override // com.android.launcher3.PagedView.ComputePageScrollsLogic
        public final boolean shouldIncludeView(View view) {
            return PagedView.lambda$static$0(view);
        }
    };
    private static final Matrix sTmpInvMatrix = new Matrix();
    private static final float[] sTmpPoint = new float[2];
    private static final Rect sTmpRect = new Rect();

    protected interface ComputePageScrollsLogic {
        boolean shouldIncludeView(View view);
    }

    /* JADX DEBUG: Can't inline method, not implemented redirect type for insn: ?: TERNARY null = ((wrap:int:0x0000: INVOKE (r1v0 android.view.View) VIRTUAL call: android.view.View.getVisibility():int A[MD:():int (c), WRAPPED] (LINE:64)) != (8 int)) ? true : false */
    static /* synthetic */ boolean lambda$static$0(View view) {
        return view.getVisibility() != 8;
    }

    public PagedView(Context context) {
        this(context, null);
    }

    public PagedView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public PagedView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mFreeScroll = false;
        this.mSettleOnPageInFreeScroll = false;
        this.mFirstLayout = true;
        this.mNextPage = -1;
        this.mPageSpacing = 0;
        this.mTouchState = 0;
        this.mAllowOverScroll = true;
        this.mActivePointerId = -1;
        this.mIsPageInTransition = false;
        this.mWasInOverscroll = false;
        this.mInsets = new Rect();
        this.mTmpIntPair = new int[2];
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.PagedView, i, 0);
        this.mPageIndicatorViewId = typedArrayObtainStyledAttributes.getResourceId(0, -1);
        typedArrayObtainStyledAttributes.recycle();
        setHapticFeedbackEnabled(false);
        this.mIsRtl = Utilities.isRtl(getResources());
        init();
    }

    protected void init() {
        this.mScroller = new LauncherScroller(getContext());
        setDefaultInterpolator(Interpolators.SCROLL);
        this.mCurrentPage = 0;
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        this.mTouchSlop = viewConfiguration.getScaledPagingTouchSlop();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        float f = getResources().getDisplayMetrics().density;
        this.mFlingThresholdVelocity = (int) (500.0f * f);
        this.mMinFlingVelocity = (int) (250.0f * f);
        this.mMinSnapVelocity = (int) (1500.0f * f);
        if (Utilities.ATLEAST_OREO) {
            setDefaultFocusHighlightEnabled(false);
        }
    }

    protected void setDefaultInterpolator(Interpolator interpolator) {
        this.mDefaultInterpolator = interpolator;
        this.mScroller.setInterpolator(this.mDefaultInterpolator);
    }

    public void initParentViews(View view) {
        if (this.mPageIndicatorViewId > -1) {
            this.mPageIndicator = (T) view.findViewById(this.mPageIndicatorViewId);
            this.mPageIndicator.setMarkersCount(getChildCount());
        }
    }

    public T getPageIndicator() {
        return this.mPageIndicator;
    }

    public int getCurrentPage() {
        return this.mCurrentPage;
    }

    public int getNextPage() {
        return this.mNextPage != -1 ? this.mNextPage : this.mCurrentPage;
    }

    public int getPageCount() {
        return getChildCount();
    }

    public View getPageAt(int i) {
        return getChildAt(i);
    }

    protected int indexToPage(int i) {
        return i;
    }

    protected void scrollAndForceFinish(int i) {
        scrollTo(i, 0);
        this.mScroller.setFinalX(i);
        forceFinishScroller(true);
    }

    protected void updateCurrentPageScroll() {
        int scrollForPage;
        if (this.mCurrentPage >= 0 && this.mCurrentPage < getPageCount()) {
            scrollForPage = getScrollForPage(this.mCurrentPage);
        } else {
            scrollForPage = 0;
        }
        scrollAndForceFinish(scrollForPage);
    }

    private void abortScrollerAnimation(boolean z) {
        this.mScroller.abortAnimation();
        if (z) {
            this.mNextPage = -1;
            pageEndTransition();
        }
    }

    private void forceFinishScroller(boolean z) {
        this.mScroller.forceFinished(true);
        if (z) {
            this.mNextPage = -1;
            pageEndTransition();
        }
    }

    private int validateNewPage(int i) {
        return Utilities.boundToRange(i, 0, getPageCount() - 1);
    }

    public void setCurrentPage(int i) {
        if (!this.mScroller.isFinished()) {
            abortScrollerAnimation(true);
        }
        if (getChildCount() == 0) {
            return;
        }
        int i2 = this.mCurrentPage;
        this.mCurrentPage = validateNewPage(i);
        updateCurrentPageScroll();
        notifyPageSwitchListener(i2);
        invalidate();
    }

    protected void notifyPageSwitchListener(int i) {
        updatePageIndicator();
    }

    private void updatePageIndicator() {
        if (this.mPageIndicator != null) {
            this.mPageIndicator.setActiveMarker(getNextPage());
        }
    }

    protected void pageBeginTransition() {
        if (!this.mIsPageInTransition) {
            this.mIsPageInTransition = true;
            onPageBeginTransition();
        }
    }

    protected void pageEndTransition() {
        if (this.mIsPageInTransition) {
            this.mIsPageInTransition = false;
            onPageEndTransition();
        }
    }

    protected boolean isPageInTransition() {
        return this.mIsPageInTransition;
    }

    protected void onPageBeginTransition() {
    }

    protected void onPageEndTransition() {
        this.mWasInOverscroll = false;
    }

    protected int getUnboundedScrollX() {
        return this.mUnboundedScrollX;
    }

    @Override // android.view.View
    public void scrollBy(int i, int i2) {
        scrollTo(getUnboundedScrollX() + i, getScrollY() + i2);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [380=5] */
    @Override // android.view.View
    public void scrollTo(int i, int i2) {
        if (this.mFreeScroll) {
            if (!this.mScroller.isFinished() && (i > this.mMaxScrollX || i < 0)) {
                forceFinishScroller(false);
            }
            i = Utilities.boundToRange(i, 0, this.mMaxScrollX);
        }
        this.mUnboundedScrollX = i;
        boolean z = !this.mIsRtl ? i >= 0 : i <= this.mMaxScrollX;
        boolean z2 = !this.mIsRtl ? i <= this.mMaxScrollX : i >= 0;
        if (z) {
            super.scrollTo(this.mIsRtl ? this.mMaxScrollX : 0, i2);
            if (this.mAllowOverScroll) {
                this.mWasInOverscroll = true;
                if (this.mIsRtl) {
                    overScroll(i - this.mMaxScrollX);
                    return;
                } else {
                    overScroll(i);
                    return;
                }
            }
            return;
        }
        if (!z2) {
            if (this.mWasInOverscroll) {
                overScroll(0.0f);
                this.mWasInOverscroll = false;
            }
            this.mOverScrollX = i;
            super.scrollTo(i, i2);
            return;
        }
        super.scrollTo(this.mIsRtl ? 0 : this.mMaxScrollX, i2);
        if (this.mAllowOverScroll) {
            this.mWasInOverscroll = true;
            if (this.mIsRtl) {
                overScroll(i);
            } else {
                overScroll(i - this.mMaxScrollX);
            }
        }
    }

    private void sendScrollAccessibilityEvent() {
        if (AccessibilityManagerCompat.isObservedEventType(getContext(), 4096) && this.mCurrentPage != getNextPage()) {
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(4096);
            accessibilityEventObtain.setScrollable(true);
            accessibilityEventObtain.setScrollX(getScrollX());
            accessibilityEventObtain.setScrollY(getScrollY());
            accessibilityEventObtain.setMaxScrollX(this.mMaxScrollX);
            accessibilityEventObtain.setMaxScrollY(0);
            sendAccessibilityEventUnchecked(accessibilityEventObtain);
        }
    }

    protected boolean computeScrollHelper() {
        return computeScrollHelper(true);
    }

    protected void announcePageForAccessibility() {
        if (AccessibilityManagerCompat.isAccessibilityEnabled(getContext())) {
            announceForAccessibility(getCurrentPageDescription());
        }
    }

    protected boolean computeScrollHelper(boolean z) {
        if (this.mScroller.computeScrollOffset()) {
            if (getUnboundedScrollX() != this.mScroller.getCurrX() || getScrollY() != this.mScroller.getCurrY() || this.mOverScrollX != this.mScroller.getCurrX()) {
                scrollTo(this.mScroller.getCurrX(), this.mScroller.getCurrY());
            }
            if (z) {
                invalidate();
                return true;
            }
            return true;
        }
        if (this.mNextPage != -1 && z) {
            sendScrollAccessibilityEvent();
            int i = this.mCurrentPage;
            this.mCurrentPage = validateNewPage(this.mNextPage);
            this.mNextPage = -1;
            notifyPageSwitchListener(i);
            if (this.mTouchState == 0) {
                pageEndTransition();
            }
            if (canAnnouncePageDescription()) {
                announcePageForAccessibility();
                return false;
            }
            return false;
        }
        return false;
    }

    @Override // android.view.View
    public void computeScroll() {
        computeScrollHelper();
    }

    public int getExpectedHeight() {
        return getMeasuredHeight();
    }

    public int getNormalChildHeight() {
        return (((getExpectedHeight() - getPaddingTop()) - getPaddingBottom()) - this.mInsets.top) - this.mInsets.bottom;
    }

    public int getExpectedWidth() {
        return getMeasuredWidth();
    }

    public int getNormalChildWidth() {
        return (((getExpectedWidth() - getPaddingLeft()) - getPaddingRight()) - this.mInsets.left) - this.mInsets.right;
    }

    @Override // android.view.View, android.view.ViewParent
    public void requestLayout() {
        this.mIsLayoutValid = false;
        super.requestLayout();
    }

    @Override // android.view.View
    public void forceLayout() {
        this.mIsLayoutValid = false;
        super.forceLayout();
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        if (getChildCount() == 0) {
            super.onMeasure(i, i2);
            return;
        }
        int mode = View.MeasureSpec.getMode(i);
        int size = View.MeasureSpec.getSize(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int size2 = View.MeasureSpec.getSize(i2);
        if (mode == 0 || mode2 == 0) {
            super.onMeasure(i, i2);
        } else if (size <= 0 || size2 <= 0) {
            super.onMeasure(i, i2);
        } else {
            measureChildren(View.MeasureSpec.makeMeasureSpec((size - this.mInsets.left) - this.mInsets.right, 1073741824), View.MeasureSpec.makeMeasureSpec((size2 - this.mInsets.top) - this.mInsets.bottom, 1073741824));
            setMeasuredDimension(size, size2);
        }
    }

    protected void restoreScrollOnLayout() {
        setCurrentPage(getNextPage());
    }

    @Override // android.view.ViewGroup, android.view.View
    @SuppressLint({"DrawAllocation"})
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        boolean z2;
        boolean z3 = true;
        this.mIsLayoutValid = true;
        int childCount = getChildCount();
        if (this.mPageScrolls == null || childCount != this.mPageScrolls.length) {
            this.mPageScrolls = new int[childCount];
            z2 = true;
        } else {
            z2 = false;
        }
        if (childCount != 0) {
            if (!getPageScrolls(this.mPageScrolls, true, SIMPLE_SCROLL_LOGIC)) {
                z3 = z2;
            }
            LayoutTransition layoutTransition = getLayoutTransition();
            if (layoutTransition != null && layoutTransition.isRunning()) {
                layoutTransition.addTransitionListener(new LayoutTransition.TransitionListener() { // from class: com.android.launcher3.PagedView.1
                    @Override // android.animation.LayoutTransition.TransitionListener
                    public void startTransition(LayoutTransition layoutTransition2, ViewGroup viewGroup, View view, int i5) {
                    }

                    @Override // android.animation.LayoutTransition.TransitionListener
                    public void endTransition(LayoutTransition layoutTransition2, ViewGroup viewGroup, View view, int i5) {
                        if (!layoutTransition2.isRunning()) {
                            layoutTransition2.removeTransitionListener(this);
                            PagedView.this.updateMaxScrollX();
                        }
                    }
                });
            } else {
                updateMaxScrollX();
            }
            if (this.mFirstLayout && this.mCurrentPage >= 0 && this.mCurrentPage < childCount) {
                updateCurrentPageScroll();
                this.mFirstLayout = false;
            }
            if (this.mScroller.isFinished() && z3) {
                restoreScrollOnLayout();
            }
        }
    }

    protected boolean getPageScrolls(int[] iArr, boolean z, ComputePageScrollsLogic computePageScrollsLogic) {
        int childCount = getChildCount();
        boolean z2 = false;
        if (this.mIsRtl) {
            childCount = -1;
        }
        int i = this.mIsRtl ? -1 : 1;
        int paddingTop = ((((getPaddingTop() + getMeasuredHeight()) + this.mInsets.top) - this.mInsets.bottom) - getPaddingBottom()) / 2;
        int paddingLeft = this.mInsets.left + getPaddingLeft();
        int iOffsetForPageScrolls = offsetForPageScrolls() + paddingLeft;
        for (int i2 = this.mIsRtl ? childCount - 1 : 0; i2 != childCount; i2 += i) {
            View pageAt = getPageAt(i2);
            if (computePageScrollsLogic.shouldIncludeView(pageAt)) {
                int measuredHeight = paddingTop - (pageAt.getMeasuredHeight() / 2);
                int measuredWidth = pageAt.getMeasuredWidth();
                if (z) {
                    pageAt.layout(iOffsetForPageScrolls, measuredHeight, iOffsetForPageScrolls + pageAt.getMeasuredWidth(), pageAt.getMeasuredHeight() + measuredHeight);
                }
                int i3 = iOffsetForPageScrolls - paddingLeft;
                if (iArr[i2] != i3) {
                    iArr[i2] = i3;
                    z2 = true;
                }
                iOffsetForPageScrolls += measuredWidth + this.mPageSpacing + getChildGap();
            }
        }
        return z2;
    }

    protected int getChildGap() {
        return 0;
    }

    private void updateMaxScrollX() {
        this.mMaxScrollX = computeMaxScrollX();
    }

    protected int computeMaxScrollX() {
        int childCount = getChildCount();
        if (childCount <= 0) {
            return 0;
        }
        return getScrollForPage(this.mIsRtl ? 0 : childCount - 1);
    }

    protected int offsetForPageScrolls() {
        return 0;
    }

    public void setPageSpacing(int i) {
        this.mPageSpacing = i;
        requestLayout();
    }

    private void dispatchPageCountChanged() {
        if (this.mPageIndicator != null) {
            this.mPageIndicator.setMarkersCount(getChildCount());
        }
        invalidate();
    }

    @Override // android.view.ViewGroup
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        dispatchPageCountChanged();
    }

    @Override // android.view.ViewGroup
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        this.mCurrentPage = validateNewPage(this.mCurrentPage);
        dispatchPageCountChanged();
    }

    protected int getChildOffset(int i) {
        if (i < 0 || i > getChildCount() - 1) {
            return 0;
        }
        return getPageAt(i).getLeft();
    }

    @Override // android.view.ViewGroup, android.view.ViewParent
    public boolean requestChildRectangleOnScreen(View view, Rect rect, boolean z) {
        int iIndexToPage = indexToPage(indexOfChild(view));
        if (iIndexToPage != this.mCurrentPage || !this.mScroller.isFinished()) {
            if (z) {
                setCurrentPage(iIndexToPage);
                return true;
            }
            snapToPage(iIndexToPage);
            return true;
        }
        return false;
    }

    @Override // android.view.ViewGroup
    protected boolean onRequestFocusInDescendants(int i, Rect rect) {
        int i2;
        if (this.mNextPage != -1) {
            i2 = this.mNextPage;
        } else {
            i2 = this.mCurrentPage;
        }
        View pageAt = getPageAt(i2);
        if (pageAt != null) {
            return pageAt.requestFocus(i, rect);
        }
        return false;
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchUnhandledMove(View view, int i) {
        if (super.dispatchUnhandledMove(view, i)) {
            return true;
        }
        if (this.mIsRtl) {
            if (i != 17) {
                if (i == 66) {
                    i = 17;
                }
            } else {
                i = 66;
            }
        }
        if (i == 17) {
            if (getCurrentPage() > 0) {
                snapToPage(getCurrentPage() - 1);
                return true;
            }
            return false;
        }
        if (i == 66 && getCurrentPage() < getPageCount() - 1) {
            snapToPage(getCurrentPage() + 1);
            return true;
        }
        return false;
    }

    @Override // android.view.ViewGroup, android.view.View
    public void addFocusables(ArrayList<View> arrayList, int i, int i2) {
        if (getDescendantFocusability() == 393216) {
            return;
        }
        if (this.mCurrentPage >= 0 && this.mCurrentPage < getPageCount()) {
            getPageAt(this.mCurrentPage).addFocusables(arrayList, i, i2);
        }
        if (i == 17) {
            if (this.mCurrentPage > 0) {
                getPageAt(this.mCurrentPage - 1).addFocusables(arrayList, i, i2);
            }
        } else if (i == 66 && this.mCurrentPage < getPageCount() - 1) {
            getPageAt(this.mCurrentPage + 1).addFocusables(arrayList, i, i2);
        }
    }

    @Override // android.view.ViewGroup, android.view.ViewParent
    public void focusableViewAvailable(View view) {
        View pageAt = getPageAt(this.mCurrentPage);
        for (View view2 = view; view2 != pageAt; view2 = (View) view2.getParent()) {
            if (view2 == this || !(view2.getParent() instanceof View)) {
                return;
            }
        }
        super.focusableViewAvailable(view);
    }

    @Override // android.view.ViewGroup, android.view.ViewParent
    public void requestDisallowInterceptTouchEvent(boolean z) {
        if (z) {
            getPageAt(this.mCurrentPage).cancelLongPress();
        }
        super.requestDisallowInterceptTouchEvent(z);
    }

    private boolean isTouchPointInViewportWithBuffer(int i, int i2) {
        sTmpRect.set((-getMeasuredWidth()) / 2, 0, (3 * getMeasuredWidth()) / 2, getMeasuredHeight());
        return sTmpRect.contains(i, i2);
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        acquireVelocityTrackerAndAddMovement(motionEvent);
        if (getChildCount() <= 0) {
            return super.onInterceptTouchEvent(motionEvent);
        }
        int action = motionEvent.getAction();
        if (action == 2 && this.mTouchState == 1) {
            return true;
        }
        int i = action & 255;
        if (i != 6) {
            switch (i) {
                case 0:
                    float x = motionEvent.getX();
                    float y = motionEvent.getY();
                    this.mDownMotionX = x;
                    this.mDownMotionY = y;
                    this.mLastMotionX = x;
                    this.mLastMotionXRemainder = 0.0f;
                    this.mTotalMotionX = 0.0f;
                    this.mActivePointerId = motionEvent.getPointerId(0);
                    if (this.mScroller.isFinished() || Math.abs(this.mScroller.getFinalX() - this.mScroller.getCurrX()) < this.mTouchSlop / 3) {
                        this.mTouchState = 0;
                        if (!this.mScroller.isFinished() && !this.mFreeScroll) {
                            setCurrentPage(getNextPage());
                            pageEndTransition();
                            break;
                        }
                    } else if (isTouchPointInViewportWithBuffer((int) this.mDownMotionX, (int) this.mDownMotionY)) {
                        this.mTouchState = 1;
                        break;
                    } else {
                        this.mTouchState = 0;
                        break;
                    }
                    break;
                case 1:
                case 3:
                    resetTouchState();
                    break;
                case 2:
                    if (this.mActivePointerId != -1) {
                        determineScrollingStart(motionEvent);
                        break;
                    }
                    break;
            }
        } else {
            onSecondaryPointerUp(motionEvent);
            releaseVelocityTracker();
        }
        return this.mTouchState != 0;
    }

    public boolean isHandlingTouch() {
        return this.mTouchState != 0;
    }

    protected void determineScrollingStart(MotionEvent motionEvent) {
        determineScrollingStart(motionEvent, 1.0f);
    }

    protected void determineScrollingStart(MotionEvent motionEvent, float f) {
        boolean z;
        int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
        if (iFindPointerIndex == -1) {
            return;
        }
        float x = motionEvent.getX(iFindPointerIndex);
        if (isTouchPointInViewportWithBuffer((int) x, (int) motionEvent.getY(iFindPointerIndex))) {
            if (((int) Math.abs(x - this.mLastMotionX)) <= Math.round(f * this.mTouchSlop)) {
                z = false;
            } else {
                z = true;
            }
            if (z) {
                this.mTouchState = 1;
                this.mTotalMotionX += Math.abs(this.mLastMotionX - x);
                this.mLastMotionX = x;
                this.mLastMotionXRemainder = 0.0f;
                onScrollInteractionBegin();
                pageBeginTransition();
                requestDisallowInterceptTouchEvent(true);
            }
        }
    }

    protected void cancelCurrentPageLongPress() {
        View pageAt = getPageAt(this.mCurrentPage);
        if (pageAt != null) {
            pageAt.cancelLongPress();
        }
    }

    protected float getScrollProgress(int i, View view, int i2) {
        int measuredWidth;
        int scrollForPage = i - (getScrollForPage(i2) + (getMeasuredWidth() / 2));
        int childCount = getChildCount();
        int i3 = i2 + 1;
        if ((scrollForPage < 0 && !this.mIsRtl) || (scrollForPage > 0 && this.mIsRtl)) {
            i3 = i2 - 1;
        }
        if (i3 < 0 || i3 > childCount - 1) {
            measuredWidth = view.getMeasuredWidth() + this.mPageSpacing;
        } else {
            measuredWidth = Math.abs(getScrollForPage(i3) - getScrollForPage(i2));
        }
        return Math.max(Math.min(scrollForPage / (measuredWidth * 1.0f), 1.0f), -1.0f);
    }

    public int getScrollForPage(int i) {
        if (this.mPageScrolls == null || i >= this.mPageScrolls.length || i < 0) {
            return 0;
        }
        return this.mPageScrolls[i];
    }

    public int getLayoutTransitionOffsetForPage(int i) {
        if (this.mPageScrolls == null || i >= this.mPageScrolls.length || i < 0) {
            return 0;
        }
        return (int) (getChildAt(i).getX() - (this.mPageScrolls[i] + (this.mIsRtl ? getPaddingRight() : getPaddingLeft())));
    }

    protected void dampedOverScroll(float f) {
        if (Float.compare(f, 0.0f) == 0) {
            return;
        }
        int iDampedScroll = OverScroll.dampedScroll(f, getMeasuredWidth());
        if (f < 0.0f) {
            this.mOverScrollX = iDampedScroll;
            super.scrollTo(this.mOverScrollX, getScrollY());
        } else {
            this.mOverScrollX = this.mMaxScrollX + iDampedScroll;
            super.scrollTo(this.mOverScrollX, getScrollY());
        }
        invalidate();
    }

    protected void overScroll(float f) {
        dampedOverScroll(f);
    }

    protected void enableFreeScroll(boolean z) {
        setEnableFreeScroll(true);
        this.mSettleOnPageInFreeScroll = z;
    }

    private void setEnableFreeScroll(boolean z) {
        boolean z2 = this.mFreeScroll;
        this.mFreeScroll = z;
        if (this.mFreeScroll) {
            setCurrentPage(getNextPage());
        } else if (z2) {
            snapToPage(getNextPage());
        }
        setEnableOverscroll(!z);
    }

    protected void setEnableOverscroll(boolean z) {
        this.mAllowOverScroll = z;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1154=4] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        if (getChildCount() <= 0) {
            return super.onTouchEvent(motionEvent);
        }
        acquireVelocityTrackerAndAddMovement(motionEvent);
        int action = motionEvent.getAction() & 255;
        if (action != 6) {
            switch (action) {
                case 0:
                    if (!this.mScroller.isFinished()) {
                        abortScrollerAnimation(false);
                    }
                    float x = motionEvent.getX();
                    this.mLastMotionX = x;
                    this.mDownMotionX = x;
                    this.mDownMotionY = motionEvent.getY();
                    this.mLastMotionXRemainder = 0.0f;
                    this.mTotalMotionX = 0.0f;
                    this.mActivePointerId = motionEvent.getPointerId(0);
                    if (this.mTouchState == 1) {
                        onScrollInteractionBegin();
                        pageBeginTransition();
                        break;
                    }
                    break;
                case 1:
                    if (this.mTouchState == 1) {
                        int i = this.mActivePointerId;
                        float x2 = motionEvent.getX(motionEvent.findPointerIndex(i));
                        VelocityTracker velocityTracker = this.mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                        int xVelocity = (int) velocityTracker.getXVelocity(i);
                        int i2 = (int) (x2 - this.mDownMotionX);
                        float measuredWidth = getPageAt(this.mCurrentPage).getMeasuredWidth();
                        boolean z = ((float) Math.abs(i2)) > 0.4f * measuredWidth;
                        this.mTotalMotionX += Math.abs((this.mLastMotionX + this.mLastMotionXRemainder) - x2);
                        boolean z2 = this.mTotalMotionX > ((float) this.mTouchSlop) && shouldFlingForVelocity(xVelocity);
                        if (this.mFreeScroll) {
                            if (!this.mScroller.isFinished()) {
                                abortScrollerAnimation(true);
                            }
                            float scaleX = getScaleX();
                            this.mScroller.setInterpolator(this.mDefaultInterpolator);
                            this.mScroller.fling((int) (getScrollX() * scaleX), getScrollY(), (int) ((-xVelocity) * scaleX), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
                            int finalX = (int) (this.mScroller.getFinalX() / scaleX);
                            this.mNextPage = getPageNearestToCenterOfScreen(finalX);
                            int scrollForPage = getScrollForPage(!this.mIsRtl ? 0 : getPageCount() - 1);
                            int scrollForPage2 = getScrollForPage(!this.mIsRtl ? getPageCount() - 1 : 0);
                            if (this.mSettleOnPageInFreeScroll && finalX > 0 && finalX < this.mMaxScrollX) {
                                this.mScroller.setFinalX((int) ((finalX >= scrollForPage / 2 ? finalX > (scrollForPage2 + this.mMaxScrollX) / 2 ? this.mMaxScrollX : getScrollForPage(this.mNextPage) : 0) * getScaleX()));
                                int duration = 270 - this.mScroller.getDuration();
                                if (duration > 0) {
                                    this.mScroller.extendDuration(duration);
                                }
                            }
                            invalidate();
                        } else {
                            boolean z3 = ((float) Math.abs(i2)) > measuredWidth * RETURN_TO_ORIGINAL_PAGE_THRESHOLD && Math.signum((float) xVelocity) != Math.signum((float) i2) && z2;
                            boolean z4 = !this.mIsRtl ? i2 >= 0 : i2 <= 0;
                            if (!this.mIsRtl ? xVelocity < 0 : xVelocity > 0) {
                                i = 1;
                            }
                            if (((z && !z4 && !z2) || (z2 && i == 0)) && this.mCurrentPage > 0) {
                                snapToPageWithVelocity(z3 ? this.mCurrentPage : this.mCurrentPage - 1, xVelocity);
                            } else if ((!(z && z4 && !z2) && (!z2 || i == 0)) || this.mCurrentPage >= getChildCount() - 1) {
                                snapToDestination();
                            } else {
                                snapToPageWithVelocity(z3 ? this.mCurrentPage : this.mCurrentPage + 1, xVelocity);
                            }
                        }
                        onScrollInteractionEnd();
                    } else if (this.mTouchState == 2) {
                        int iMax = Math.max(0, this.mCurrentPage - 1);
                        if (iMax != this.mCurrentPage) {
                            snapToPage(iMax);
                        } else {
                            snapToDestination();
                        }
                    } else if (this.mTouchState == 3) {
                        int iMin = Math.min(getChildCount() - 1, this.mCurrentPage + 1);
                        if (iMin != this.mCurrentPage) {
                            snapToPage(iMin);
                        } else {
                            snapToDestination();
                        }
                    }
                    resetTouchState();
                    break;
                case 2:
                    if (this.mTouchState != 1) {
                        determineScrollingStart(motionEvent);
                        break;
                    } else {
                        int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                        if (iFindPointerIndex != -1) {
                            float x3 = motionEvent.getX(iFindPointerIndex);
                            float f = (this.mLastMotionX + this.mLastMotionXRemainder) - x3;
                            this.mTotalMotionX += Math.abs(f);
                            if (Math.abs(f) < 1.0f) {
                                awakenScrollBars();
                                break;
                            } else {
                                int i3 = (int) f;
                                scrollBy(i3, 0);
                                this.mLastMotionX = x3;
                                this.mLastMotionXRemainder = f - i3;
                                break;
                            }
                        } else {
                            return true;
                        }
                    }
                case 3:
                    if (this.mTouchState == 1) {
                        snapToDestination();
                        onScrollInteractionEnd();
                    }
                    resetTouchState();
                    break;
            }
        } else {
            onSecondaryPointerUp(motionEvent);
            releaseVelocityTracker();
        }
        return true;
    }

    protected boolean shouldFlingForVelocity(int i) {
        return Math.abs(i) > this.mFlingThresholdVelocity;
    }

    private void resetTouchState() {
        releaseVelocityTracker();
        this.mTouchState = 0;
        this.mActivePointerId = -1;
    }

    protected void onScrollInteractionBegin() {
    }

    protected void onScrollInteractionEnd() {
    }

    @Override // android.view.View
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        float f;
        float axisValue;
        if ((motionEvent.getSource() & 2) != 0 && motionEvent.getAction() == 8) {
            if ((motionEvent.getMetaState() & 1) != 0) {
                axisValue = motionEvent.getAxisValue(9);
                f = 0.0f;
            } else {
                f = -motionEvent.getAxisValue(9);
                axisValue = motionEvent.getAxisValue(10);
            }
            if (axisValue != 0.0f || f != 0.0f) {
                boolean z = false;
                if (!this.mIsRtl ? axisValue > 0.0f || f > 0.0f : axisValue < 0.0f || f < 0.0f) {
                    z = true;
                }
                if (z) {
                    scrollRight();
                } else {
                    scrollLeft();
                }
                return true;
            }
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent motionEvent) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
    }

    private void releaseVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.clear();
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void onSecondaryPointerUp(MotionEvent motionEvent) {
        int action = (motionEvent.getAction() & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >> 8;
        if (motionEvent.getPointerId(action) == this.mActivePointerId) {
            int i = action == 0 ? 1 : 0;
            float x = motionEvent.getX(i);
            this.mDownMotionX = x;
            this.mLastMotionX = x;
            this.mLastMotionXRemainder = 0.0f;
            this.mActivePointerId = motionEvent.getPointerId(i);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    @Override // android.view.ViewGroup, android.view.ViewParent
    public void requestChildFocus(View view, View view2) {
        super.requestChildFocus(view, view2);
        int iIndexToPage = indexToPage(indexOfChild(view));
        if (iIndexToPage >= 0 && iIndexToPage != getCurrentPage() && !isInTouchMode()) {
            snapToPage(iIndexToPage);
        }
    }

    public int getPageNearestToCenterOfScreen() {
        return getPageNearestToCenterOfScreen(getScrollX());
    }

    private int getPageNearestToCenterOfScreen(int i) {
        int measuredWidth = i + (getMeasuredWidth() / 2);
        int childCount = getChildCount();
        int i2 = Integer.MAX_VALUE;
        int i3 = -1;
        for (int i4 = 0; i4 < childCount; i4++) {
            int iAbs = Math.abs((getChildOffset(i4) + (getPageAt(i4).getMeasuredWidth() / 2)) - measuredWidth);
            if (iAbs < i2) {
                i3 = i4;
                i2 = iAbs;
            }
        }
        return i3;
    }

    protected void snapToDestination() {
        snapToPage(getPageNearestToCenterOfScreen(), getPageSnapDuration());
    }

    protected boolean isInOverScroll() {
        return this.mOverScrollX > this.mMaxScrollX || this.mOverScrollX < 0;
    }

    protected int getPageSnapDuration() {
        if (isInOverScroll()) {
            return OVERSCROLL_PAGE_SNAP_ANIMATION_DURATION;
        }
        return PAGE_SNAP_ANIMATION_DURATION;
    }

    private float distanceInfluenceForSnapDuration(float f) {
        return (float) Math.sin((float) ((f - 0.5f) * 0.4712389167638204d));
    }

    protected boolean snapToPageWithVelocity(int i, int i2) {
        int iValidateNewPage = validateNewPage(i);
        int measuredWidth = getMeasuredWidth() / 2;
        int scrollForPage = getScrollForPage(iValidateNewPage) - getUnboundedScrollX();
        if (Math.abs(i2) < this.mMinFlingVelocity) {
            return snapToPage(iValidateNewPage, PAGE_SNAP_ANIMATION_DURATION);
        }
        float fMin = Math.min(1.0f, (Math.abs(scrollForPage) * 1.0f) / (2 * measuredWidth));
        float f = measuredWidth;
        return snapToPage(iValidateNewPage, scrollForPage, 4 * Math.round(1000.0f * Math.abs((f + (distanceInfluenceForSnapDuration(fMin) * f)) / Math.max(this.mMinSnapVelocity, Math.abs(i2)))));
    }

    public boolean snapToPage(int i) {
        return snapToPage(i, PAGE_SNAP_ANIMATION_DURATION);
    }

    public boolean snapToPageImmediately(int i) {
        return snapToPage(i, PAGE_SNAP_ANIMATION_DURATION, true, null);
    }

    public boolean snapToPage(int i, int i2) {
        return snapToPage(i, i2, false, null);
    }

    public boolean snapToPage(int i, int i2, TimeInterpolator timeInterpolator) {
        return snapToPage(i, i2, false, timeInterpolator);
    }

    protected boolean snapToPage(int i, int i2, boolean z, TimeInterpolator timeInterpolator) {
        int iValidateNewPage = validateNewPage(i);
        return snapToPage(iValidateNewPage, getScrollForPage(iValidateNewPage) - getUnboundedScrollX(), i2, z, timeInterpolator);
    }

    protected boolean snapToPage(int i, int i2, int i3) {
        return snapToPage(i, i2, i3, false, null);
    }

    protected boolean snapToPage(int i, int i2, int i3, boolean z, TimeInterpolator timeInterpolator) {
        int i4;
        if (this.mFirstLayout) {
            setCurrentPage(i);
            return false;
        }
        this.mNextPage = validateNewPage(i);
        awakenScrollBars(i3);
        if (!z) {
            if (i3 == 0) {
                i3 = Math.abs(i2);
            }
            i4 = i3;
        } else {
            i4 = 0;
        }
        if (i4 != 0) {
            pageBeginTransition();
        }
        if (!this.mScroller.isFinished()) {
            abortScrollerAnimation(false);
        }
        if (timeInterpolator != null) {
            this.mScroller.setInterpolator(timeInterpolator);
        } else {
            this.mScroller.setInterpolator(this.mDefaultInterpolator);
        }
        this.mScroller.startScroll(getUnboundedScrollX(), 0, i2, 0, i4);
        updatePageIndicator();
        if (z) {
            computeScroll();
            pageEndTransition();
        }
        invalidate();
        return Math.abs(i2) > 0;
    }

    public boolean scrollLeft() {
        if (getNextPage() > 0) {
            snapToPage(getNextPage() - 1);
            return true;
        }
        return false;
    }

    public boolean scrollRight() {
        if (getNextPage() < getChildCount() - 1) {
            snapToPage(getNextPage() + 1);
            return true;
        }
        return false;
    }

    @Override // android.view.ViewGroup, android.view.View
    public CharSequence getAccessibilityClassName() {
        return ScrollView.class.getName();
    }

    protected boolean isPageOrderFlipped() {
        return false;
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        boolean zIsPageOrderFlipped = isPageOrderFlipped();
        accessibilityNodeInfo.setScrollable(getPageCount() > 1);
        if (getCurrentPage() < getPageCount() - 1) {
            accessibilityNodeInfo.addAction(zIsPageOrderFlipped ? 8192 : 4096);
        }
        if (getCurrentPage() > 0) {
            accessibilityNodeInfo.addAction(zIsPageOrderFlipped ? 4096 : 8192);
        }
        accessibilityNodeInfo.setLongClickable(false);
        accessibilityNodeInfo.removeAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
    }

    @Override // android.view.View, android.view.accessibility.AccessibilityEventSource
    public void sendAccessibilityEvent(int i) {
        if (i != 4096) {
            super.sendAccessibilityEvent(i);
        }
    }

    @Override // android.view.View
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setScrollable(getPageCount() > 1);
    }

    @Override // android.view.View
    public boolean performAccessibilityAction(int i, Bundle bundle) {
        if (super.performAccessibilityAction(i, bundle)) {
            return true;
        }
        boolean zIsPageOrderFlipped = isPageOrderFlipped();
        if (i == 4096) {
            if (zIsPageOrderFlipped) {
                if (!scrollLeft()) {
                    return false;
                }
            } else if (!scrollRight()) {
                return false;
            }
            return true;
        }
        if (i == 8192) {
            if (zIsPageOrderFlipped) {
                if (!scrollRight()) {
                    return false;
                }
            } else if (!scrollLeft()) {
                return false;
            }
            return true;
        }
        return false;
    }

    protected boolean canAnnouncePageDescription() {
        return true;
    }

    protected String getCurrentPageDescription() {
        return getContext().getString(R.string.default_scroll_format, Integer.valueOf(getNextPage() + 1), Integer.valueOf(getChildCount()));
    }

    protected float getDownMotionX() {
        return this.mDownMotionX;
    }

    protected float getDownMotionY() {
        return this.mDownMotionY;
    }

    public int[] getVisibleChildrenRange() {
        float f = 0.0f;
        float measuredWidth = getMeasuredWidth() + 0.0f;
        float scaleX = getScaleX();
        if (scaleX < 1.0f && scaleX > 0.0f) {
            float measuredWidth2 = getMeasuredWidth() / 2;
            f = measuredWidth2 - ((measuredWidth2 - 0.0f) / scaleX);
            measuredWidth = ((measuredWidth - measuredWidth2) / scaleX) + measuredWidth2;
        }
        int childCount = getChildCount();
        int i = -1;
        int i2 = -1;
        for (int i3 = 0; i3 < childCount; i3++) {
            float left = (r8.getLeft() + getPageAt(i3).getTranslationX()) - getScrollX();
            if (left <= measuredWidth && left + r8.getMeasuredWidth() >= f) {
                if (i == -1) {
                    i = i3;
                }
                i2 = i3;
            }
        }
        this.mTmpIntPair[0] = i;
        this.mTmpIntPair[1] = i2;
        return this.mTmpIntPair;
    }
}
