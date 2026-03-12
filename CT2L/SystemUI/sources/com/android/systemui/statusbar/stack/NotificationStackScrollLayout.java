package com.android.systemui.statusbar.stack;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.OverScroller;
import com.android.systemui.ExpandHelper;
import com.android.systemui.R;
import com.android.systemui.SwipeHelper;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.DismissView;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.SpeedBumpView;
import com.android.systemui.statusbar.StackScrollerDecorView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.ScrollAdapter;
import com.android.systemui.statusbar.stack.StackScrollState;
import java.util.ArrayList;
import java.util.HashSet;

public class NotificationStackScrollLayout extends ViewGroup implements ExpandHelper.Callback, SwipeHelper.Callback, ExpandableView.OnHeightChangedListener, ScrollAdapter {
    private boolean mActivateNeedsAnimation;
    private int mActivePointerId;
    private AmbientState mAmbientState;
    private ArrayList<AnimationEvent> mAnimationEvents;
    private boolean mAnimationsEnabled;
    private int mBottomStackPeekSize;
    private int mBottomStackSlowDownHeight;
    private boolean mChangePositionInProgress;
    private ArrayList<View> mChildrenChangingPositions;
    private ArrayList<View> mChildrenToAddAnimated;
    private ArrayList<View> mChildrenToRemoveAnimated;
    private boolean mChildrenUpdateRequested;
    private ViewTreeObserver.OnPreDrawListener mChildrenUpdater;
    private int mCollapseSecondCardPadding;
    private int mCollapsedSize;
    private int mContentHeight;
    private int mCurrentStackHeight;
    private StackScrollState mCurrentStackScrollState;
    private int mDarkAnimationOriginIndex;
    private boolean mDarkNeedsAnimation;
    private boolean mDelegateToScrollView;
    private boolean mDimmedNeedsAnimation;
    private boolean mDisallowScrollingInThisMotion;
    private boolean mDismissAllInProgress;
    private DismissView mDismissView;
    private boolean mDontReportNextOverScroll;
    private int mDownX;
    private ArrayList<View> mDragAnimPendingChildren;
    private EmptyShadeView mEmptyShadeView;
    private boolean mEverythingNeedsAnimation;
    private ExpandHelper mExpandHelper;
    private boolean mExpandedInThisMotion;
    private boolean mExpandingNotification;
    private HashSet<View> mFromMoreCardAdditions;
    private long mGoToFullShadeDelay;
    private boolean mGoToFullShadeNeedsAnimation;
    private boolean mHideSensitiveNeedsAnimation;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private boolean mInterceptDelegateEnabled;
    private int mIntrinsicPadding;
    private boolean mIsBeingDragged;
    private boolean mIsExpanded;
    private boolean mIsExpansionChanging;
    private int mLastMotionY;
    private float mLastSetStackHeight;
    private OnChildLocationsChangedListener mListener;
    private SwipeHelper.LongPressListener mLongPressListener;
    private int mMaxLayoutHeight;
    private float mMaxOverScroll;
    private int mMaxScrollAfterExpand;
    private int mMaximumVelocity;
    private float mMinTopOverScrollToEscape;
    private int mMinimumVelocity;
    private boolean mNeedViewResizeAnimation;
    private boolean mNeedsAnimation;
    private int mNotificationTopPadding;
    private OnEmptySpaceClickListener mOnEmptySpaceClickListener;
    private ExpandableView.OnHeightChangedListener mOnHeightChangedListener;
    private boolean mOnlyScrollingInThisMotion;
    private float mOverScrolledBottomPixels;
    private float mOverScrolledTopPixels;
    private int mOverflingDistance;
    private OnOverscrollTopChangedListener mOverscrollTopChangedListener;
    private int mOwnScrollY;
    private int mPaddingBetweenElements;
    private int mPaddingBetweenElementsDimmed;
    private int mPaddingBetweenElementsNormal;
    private PhoneStatusBar mPhoneStatusBar;
    private boolean mRequestViewResizeAnimationOnLayout;
    private ViewGroup mScrollView;
    private boolean mScrolledToTopOnFirstDown;
    private OverScroller mScroller;
    private boolean mScrollingEnabled;
    private int mSidePaddings;
    private ArrayList<View> mSnappedBackChildren;
    private SpeedBumpView mSpeedBumpView;
    private StackScrollAlgorithm mStackScrollAlgorithm;
    private final StackStateAnimator mStateAnimator;
    private SwipeHelper mSwipeHelper;
    private ArrayList<View> mSwipedOutViews;
    private boolean mSwipingInProgress;
    private int[] mTempInt2;
    private int mTopPadding;
    private boolean mTopPaddingNeedsAnimation;
    private float mTopPaddingOverflow;
    private boolean mTouchIsClick;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;

    public interface OnChildLocationsChangedListener {
        void onChildLocationsChanged(NotificationStackScrollLayout notificationStackScrollLayout);
    }

    public interface OnEmptySpaceClickListener {
        void onEmptySpaceClicked(float f, float f2);
    }

    public interface OnOverscrollTopChangedListener {
        void flingTopOverscroll(float f, boolean z);

        void onOverscrollTopChanged(float f, boolean z);
    }

    public NotificationStackScrollLayout(Context context) {
        this(context, null);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mCurrentStackHeight = Integer.MAX_VALUE;
        this.mCurrentStackScrollState = new StackScrollState(this);
        this.mAmbientState = new AmbientState();
        this.mChildrenToAddAnimated = new ArrayList<>();
        this.mChildrenToRemoveAnimated = new ArrayList<>();
        this.mSnappedBackChildren = new ArrayList<>();
        this.mDragAnimPendingChildren = new ArrayList<>();
        this.mChildrenChangingPositions = new ArrayList<>();
        this.mFromMoreCardAdditions = new HashSet<>();
        this.mAnimationEvents = new ArrayList<>();
        this.mSwipedOutViews = new ArrayList<>();
        this.mStateAnimator = new StackStateAnimator(this);
        this.mIsExpanded = true;
        this.mChildrenUpdater = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                NotificationStackScrollLayout.this.updateChildren();
                NotificationStackScrollLayout.this.mChildrenUpdateRequested = false;
                NotificationStackScrollLayout.this.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        };
        this.mTempInt2 = new int[2];
        int minHeight = getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        int maxHeight = getResources().getDimensionPixelSize(R.dimen.notification_max_height);
        this.mExpandHelper = new ExpandHelper(getContext(), this, minHeight, maxHeight);
        this.mExpandHelper.setEventSource(this);
        this.mExpandHelper.setScrollAdapter(this);
        this.mSwipeHelper = new SwipeHelper(0, this, getContext());
        this.mSwipeHelper.setLongPressListener(this.mLongPressListener);
        initView(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
    }

    private void initView(Context context) {
        this.mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(262144);
        setClipChildren(false);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mOverflingDistance = configuration.getScaledOverflingDistance();
        this.mSidePaddings = context.getResources().getDimensionPixelSize(R.dimen.notification_side_padding);
        this.mCollapsedSize = context.getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        this.mBottomStackPeekSize = context.getResources().getDimensionPixelSize(R.dimen.bottom_stack_peek_amount);
        this.mStackScrollAlgorithm = new StackScrollAlgorithm(context);
        this.mStackScrollAlgorithm.setDimmed(this.mAmbientState.isDimmed());
        this.mPaddingBetweenElementsDimmed = context.getResources().getDimensionPixelSize(R.dimen.notification_padding_dimmed);
        this.mPaddingBetweenElementsNormal = context.getResources().getDimensionPixelSize(R.dimen.notification_padding);
        updatePadding(this.mAmbientState.isDimmed());
        this.mMinTopOverScrollToEscape = getResources().getDimensionPixelSize(R.dimen.min_top_overscroll_to_qs);
        this.mNotificationTopPadding = getResources().getDimensionPixelSize(R.dimen.notifications_top_padding);
        this.mCollapseSecondCardPadding = getResources().getDimensionPixelSize(R.dimen.notification_collapse_second_card_padding);
    }

    private void updatePadding(boolean dimmed) {
        this.mPaddingBetweenElements = (dimmed && this.mStackScrollAlgorithm.shouldScaleDimmed()) ? this.mPaddingBetweenElementsDimmed : this.mPaddingBetweenElementsNormal;
        this.mBottomStackSlowDownHeight = this.mStackScrollAlgorithm.getBottomStackSlowDownLength();
        updateContentHeight();
        notifyHeightChangeListener(null);
    }

    public void notifyHeightChangeListener(ExpandableView view) {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onHeightChanged(view);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int mode = View.MeasureSpec.getMode(widthMeasureSpec);
        int size = View.MeasureSpec.getSize(widthMeasureSpec);
        int childMeasureSpec = View.MeasureSpec.makeMeasureSpec(size - (this.mSidePaddings * 2), mode);
        measureChildren(childMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        float centerX = getWidth() / 2.0f;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            float width = child.getMeasuredWidth();
            float height = child.getMeasuredHeight();
            child.layout((int) (centerX - (width / 2.0f)), 0, (int) ((width / 2.0f) + centerX), (int) height);
        }
        setMaxLayoutHeight(getHeight());
        updateContentHeight();
        clampScrollPosition();
        requestAnimationOnViewResize();
        requestChildrenUpdate();
    }

    private void requestAnimationOnViewResize() {
        if (this.mRequestViewResizeAnimationOnLayout && this.mIsExpanded && this.mAnimationsEnabled) {
            this.mNeedViewResizeAnimation = true;
            this.mNeedsAnimation = true;
        }
        this.mRequestViewResizeAnimationOnLayout = false;
    }

    public void updateSpeedBumpIndex(int newIndex) {
        int currentIndex = indexOfChild(this.mSpeedBumpView);
        boolean validIndex = newIndex > 0;
        if (newIndex > getChildCount() - 1) {
            validIndex = false;
            newIndex = -1;
        }
        if (validIndex && currentIndex != newIndex) {
            changeViewPosition(this.mSpeedBumpView, newIndex);
        }
        updateSpeedBump(validIndex);
        this.mAmbientState.setSpeedBumpIndex(newIndex);
    }

    public void setChildLocationsChangedListener(OnChildLocationsChangedListener listener) {
        this.mListener = listener;
    }

    public int getChildLocation(View child) {
        StackScrollState.ViewState childViewState = this.mCurrentStackScrollState.getViewStateForView(child);
        if (childViewState == null) {
            return 0;
        }
        if (childViewState.gone) {
            return 64;
        }
        return childViewState.location;
    }

    private void setMaxLayoutHeight(int maxLayoutHeight) {
        this.mMaxLayoutHeight = maxLayoutHeight;
        updateAlgorithmHeightAndPadding();
    }

    private void updateAlgorithmHeightAndPadding() {
        this.mStackScrollAlgorithm.setLayoutHeight(getLayoutHeight());
        this.mStackScrollAlgorithm.setTopPadding(this.mTopPadding);
    }

    private boolean needsHeightAdaption() {
        return getNotGoneChildCount() > 1;
    }

    public void updateChildren() {
        this.mAmbientState.setScrollY(this.mOwnScrollY);
        this.mStackScrollAlgorithm.getStackScrollState(this.mAmbientState, this.mCurrentStackScrollState);
        if (!isCurrentlyAnimating() && !this.mNeedsAnimation) {
            applyCurrentState();
        } else {
            startAnimationToState();
        }
    }

    private void requestChildrenUpdate() {
        if (!this.mChildrenUpdateRequested) {
            getViewTreeObserver().addOnPreDrawListener(this.mChildrenUpdater);
            this.mChildrenUpdateRequested = true;
            invalidate();
        }
    }

    private boolean isCurrentlyAnimating() {
        return this.mStateAnimator.isRunning();
    }

    private void clampScrollPosition() {
        int scrollRange = getScrollRange();
        if (scrollRange < this.mOwnScrollY) {
            this.mOwnScrollY = scrollRange;
        }
    }

    public int getTopPadding() {
        return this.mTopPadding;
    }

    private void setTopPadding(int topPadding, boolean animate) {
        if (this.mTopPadding != topPadding) {
            this.mTopPadding = topPadding;
            updateAlgorithmHeightAndPadding();
            updateContentHeight();
            if (animate && this.mAnimationsEnabled && this.mIsExpanded) {
                this.mTopPaddingNeedsAnimation = true;
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
            notifyHeightChangeListener(null);
        }
    }

    public void setStackHeight(float height) {
        int stackHeight;
        this.mLastSetStackHeight = height;
        setIsExpanded(height > 0.0f);
        int newStackHeight = (int) height;
        int minStackHeight = getMinStackHeight();
        if ((newStackHeight - this.mTopPadding) - this.mTopPaddingOverflow >= minStackHeight || getNotGoneChildCount() == 0) {
            setTranslationY(this.mTopPaddingOverflow);
            stackHeight = newStackHeight;
        } else {
            float partiallyThere = ((newStackHeight - this.mTopPadding) - this.mTopPaddingOverflow) / minStackHeight;
            int translationY = (int) ((newStackHeight - minStackHeight) + ((1.0f - Math.max(0.0f, partiallyThere)) * (this.mBottomStackPeekSize + this.mCollapseSecondCardPadding)));
            setTranslationY(translationY - this.mTopPadding);
            stackHeight = (int) (height - (translationY - this.mTopPadding));
        }
        if (stackHeight != this.mCurrentStackHeight) {
            this.mCurrentStackHeight = stackHeight;
            updateAlgorithmHeightAndPadding();
            requestChildrenUpdate();
        }
    }

    private int getLayoutHeight() {
        return Math.min(this.mMaxLayoutHeight, this.mCurrentStackHeight);
    }

    public int getItemHeight() {
        return this.mCollapsedSize;
    }

    public int getBottomStackPeekSize() {
        return this.mBottomStackPeekSize;
    }

    public int getCollapseSecondCardPadding() {
        return this.mCollapseSecondCardPadding;
    }

    public void setLongPressListener(SwipeHelper.LongPressListener listener) {
        this.mSwipeHelper.setLongPressListener(listener);
        this.mLongPressListener = listener;
    }

    public void setScrollView(ViewGroup scrollView) {
        this.mScrollView = scrollView;
    }

    public void setInterceptDelegateEnabled(boolean interceptDelegateEnabled) {
        this.mInterceptDelegateEnabled = interceptDelegateEnabled;
    }

    @Override
    public void onChildDismissed(View v) {
        if (!this.mDismissAllInProgress) {
            View veto = v.findViewById(R.id.veto);
            if (veto != null && veto.getVisibility() != 8) {
                veto.performClick();
            }
            setSwipingInProgress(false);
            if (this.mDragAnimPendingChildren.contains(v)) {
                this.mDragAnimPendingChildren.remove(v);
            }
            this.mSwipedOutViews.add(v);
            this.mAmbientState.onDragFinished(v);
        }
    }

    @Override
    public void onChildSnappedBack(View animView) {
        this.mAmbientState.onDragFinished(animView);
        if (!this.mDragAnimPendingChildren.contains(animView)) {
            if (this.mAnimationsEnabled) {
                this.mSnappedBackChildren.add(animView);
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
            return;
        }
        this.mDragAnimPendingChildren.remove(animView);
    }

    @Override
    public boolean updateSwipeProgress(View animView, boolean dismissable, float swipeProgress) {
        return false;
    }

    @Override
    public float getFalsingThresholdFactor() {
        return this.mPhoneStatusBar.isScreenOnComingFromTouch() ? 1.5f : 1.0f;
    }

    @Override
    public void onBeginDrag(View v) {
        setSwipingInProgress(true);
        this.mAmbientState.onBeginDrag(v);
        if (this.mAnimationsEnabled) {
            this.mDragAnimPendingChildren.add(v);
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    @Override
    public void onDragCancelled(View v) {
        setSwipingInProgress(false);
    }

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        return getChildAtPosition(ev.getX(), ev.getY());
    }

    public ExpandableView getClosestChildAtRawPosition(float touchX, float touchY) {
        getLocationOnScreen(this.mTempInt2);
        float localTouchY = touchY - this.mTempInt2[1];
        ExpandableView closestChild = null;
        float minDist = Float.MAX_VALUE;
        int count = getChildCount();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableView slidingChild = (ExpandableView) getChildAt(childIdx);
            if (slidingChild.getVisibility() != 8 && !(slidingChild instanceof StackScrollerDecorView) && slidingChild != this.mSpeedBumpView) {
                float childTop = slidingChild.getTranslationY();
                float top = childTop + slidingChild.getClipTopAmount();
                float bottom = childTop + slidingChild.getActualHeight();
                float dist = Math.min(Math.abs(top - localTouchY), Math.abs(bottom - localTouchY));
                if (dist < minDist) {
                    closestChild = slidingChild;
                    minDist = dist;
                }
            }
        }
        return closestChild;
    }

    @Override
    public ExpandableView getChildAtRawPosition(float touchX, float touchY) {
        getLocationOnScreen(this.mTempInt2);
        return getChildAtPosition(touchX - this.mTempInt2[0], touchY - this.mTempInt2[1]);
    }

    @Override
    public ExpandableView getChildAtPosition(float touchX, float touchY) {
        int count = getChildCount();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableView slidingChild = (ExpandableView) getChildAt(childIdx);
            if (slidingChild.getVisibility() != 8 && !(slidingChild instanceof StackScrollerDecorView) && slidingChild != this.mSpeedBumpView) {
                float childTop = slidingChild.getTranslationY();
                float top = childTop + slidingChild.getClipTopAmount();
                float bottom = childTop + slidingChild.getActualHeight();
                int right = getWidth();
                if (touchY >= top && touchY <= bottom && touchX >= 0 && touchX <= right) {
                    return slidingChild;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canChildBeExpanded(View v) {
        return (v instanceof ExpandableNotificationRow) && ((ExpandableNotificationRow) v).isExpandable();
    }

    @Override
    public void setUserExpandedChild(View v, boolean userExpanded) {
        if (v instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) v).setUserExpanded(userExpanded);
        }
    }

    @Override
    public void setUserLockedChild(View v, boolean userLocked) {
        if (v instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) v).setUserLocked(userLocked);
        }
        removeLongPressCallback();
        requestDisallowInterceptTouchEvent(true);
    }

    @Override
    public void expansionStateChanged(boolean isExpanding) {
        this.mExpandingNotification = isExpanding;
        if (!this.mExpandedInThisMotion) {
            this.mMaxScrollAfterExpand = this.mOwnScrollY;
            this.mExpandedInThisMotion = true;
        }
    }

    public void setScrollingEnabled(boolean enable) {
        this.mScrollingEnabled = enable;
    }

    public void setExpandingEnabled(boolean enable) {
        this.mExpandHelper.setEnabled(enable);
    }

    private boolean isScrollingEnabled() {
        return this.mScrollingEnabled;
    }

    @Override
    public View getChildContentView(View v) {
        return v;
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        View veto = v.findViewById(R.id.veto);
        return (veto == null || veto.getVisibility() == 8) ? false : true;
    }

    @Override
    public boolean isAntiFalsingNeeded() {
        return this.mPhoneStatusBar.getBarState() == 1;
    }

    private void setSwipingInProgress(boolean isSwiped) {
        this.mSwipingInProgress = isSwiped;
        if (isSwiped) {
            requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getResources().getDisplayMetrics().density;
        this.mSwipeHelper.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        this.mSwipeHelper.setPagingTouchSlop(pagingTouchSlop);
        initView(getContext());
    }

    public void dismissViewAnimated(View child, Runnable endRunnable, int delay, long duration) {
        child.setClipBounds(null);
        this.mSwipeHelper.dismissChild(child, 0.0f, endRunnable, delay, true, duration);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean isCancelOrUp = ev.getActionMasked() == 3 || ev.getActionMasked() == 1;
        if (this.mDelegateToScrollView) {
            if (isCancelOrUp) {
                this.mDelegateToScrollView = false;
            }
            transformTouchEvent(ev, this, this.mScrollView);
            return this.mScrollView.onTouchEvent(ev);
        }
        handleEmptySpaceClick(ev);
        boolean expandWantsIt = false;
        if (!this.mSwipingInProgress && !this.mOnlyScrollingInThisMotion && isScrollingEnabled()) {
            if (isCancelOrUp) {
                this.mExpandHelper.onlyObserveMovements(false);
            }
            boolean wasExpandingBefore = this.mExpandingNotification;
            expandWantsIt = this.mExpandHelper.onTouchEvent(ev);
            if (this.mExpandedInThisMotion && !this.mExpandingNotification && wasExpandingBefore && !this.mDisallowScrollingInThisMotion) {
                dispatchDownEventToScroller(ev);
            }
        }
        boolean scrollerWantsIt = false;
        if (!this.mSwipingInProgress && !this.mExpandingNotification && !this.mDisallowScrollingInThisMotion) {
            scrollerWantsIt = onScrollTouch(ev);
        }
        boolean horizontalSwipeWantsIt = false;
        if (!this.mIsBeingDragged && !this.mExpandingNotification && !this.mExpandedInThisMotion && !this.mOnlyScrollingInThisMotion) {
            horizontalSwipeWantsIt = this.mSwipeHelper.onTouchEvent(ev);
        }
        return horizontalSwipeWantsIt || scrollerWantsIt || expandWantsIt || super.onTouchEvent(ev);
    }

    private void dispatchDownEventToScroller(MotionEvent ev) {
        MotionEvent downEvent = MotionEvent.obtain(ev);
        downEvent.setAction(0);
        onScrollTouch(downEvent);
        downEvent.recycle();
    }

    private boolean onScrollTouch(MotionEvent ev) {
        float scrollAmount;
        if (!isScrollingEnabled()) {
            return false;
        }
        initVelocityTrackerIfNotExists();
        this.mVelocityTracker.addMovement(ev);
        int action = ev.getAction();
        switch (action & 255) {
            case 0:
                if (getChildCount() == 0 || !isInContentBounds(ev)) {
                    return false;
                }
                boolean isBeingDragged = !this.mScroller.isFinished();
                setIsBeingDragged(isBeingDragged);
                if (!this.mScroller.isFinished()) {
                    this.mScroller.forceFinished(true);
                }
                this.mLastMotionY = (int) ev.getY();
                this.mDownX = (int) ev.getX();
                this.mActivePointerId = ev.getPointerId(0);
                return true;
            case 1:
                if (this.mIsBeingDragged) {
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity(this.mActivePointerId);
                    if (shouldOverScrollFling(initialVelocity)) {
                        onOverScrollFling(true, initialVelocity);
                    } else if (getChildCount() > 0) {
                        if (Math.abs(initialVelocity) > this.mMinimumVelocity) {
                            float currentOverScrollTop = getCurrentOverScrollAmount(true);
                            if (currentOverScrollTop == 0.0f || initialVelocity > 0) {
                                fling(-initialVelocity);
                            } else {
                                onOverScrollFling(false, initialVelocity);
                            }
                        } else if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                            postInvalidateOnAnimation();
                        }
                    }
                    this.mActivePointerId = -1;
                    endDrag();
                }
                return true;
            case 2:
                int activePointerIndex = ev.findPointerIndex(this.mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e("NotificationStackScrollLayout", "Invalid pointerId=" + this.mActivePointerId + " in onTouchEvent");
                } else {
                    int y = (int) ev.getY(activePointerIndex);
                    int x = (int) ev.getX(activePointerIndex);
                    int deltaY = this.mLastMotionY - y;
                    int xDiff = Math.abs(x - this.mDownX);
                    int yDiff = Math.abs(deltaY);
                    if (!this.mIsBeingDragged && yDiff > this.mTouchSlop && yDiff > xDiff) {
                        setIsBeingDragged(true);
                        deltaY = deltaY > 0 ? deltaY - this.mTouchSlop : deltaY + this.mTouchSlop;
                    }
                    if (this.mIsBeingDragged) {
                        this.mLastMotionY = y;
                        int range = getScrollRange();
                        if (this.mExpandedInThisMotion) {
                            range = Math.min(range, this.mMaxScrollAfterExpand);
                        }
                        if (deltaY < 0) {
                            scrollAmount = overScrollDown(deltaY);
                        } else {
                            scrollAmount = overScrollUp(deltaY, range);
                        }
                        if (scrollAmount != 0.0f) {
                            overScrollBy(0, (int) scrollAmount, 0, this.mOwnScrollY, 0, range, 0, getHeight() / 2, true);
                        }
                    }
                }
                return true;
            case 3:
                if (this.mIsBeingDragged && getChildCount() > 0) {
                    if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                        postInvalidateOnAnimation();
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
                this.mLastMotionY = (int) ev.getY(index);
                this.mDownX = (int) ev.getX(index);
                this.mActivePointerId = ev.getPointerId(index);
                return true;
            case 6:
                onSecondaryPointerUp(ev);
                this.mLastMotionY = (int) ev.getY(ev.findPointerIndex(this.mActivePointerId));
                this.mDownX = (int) ev.getX(ev.findPointerIndex(this.mActivePointerId));
                return true;
        }
    }

    private void onOverScrollFling(boolean open, int initialVelocity) {
        if (this.mOverscrollTopChangedListener != null) {
            this.mOverscrollTopChangedListener.flingTopOverscroll(initialVelocity, open);
        }
        this.mDontReportNextOverScroll = true;
        setOverScrollAmount(0.0f, true, false);
    }

    private float overScrollUp(int deltaY, int range) {
        int deltaY2 = Math.max(deltaY, 0);
        float currentTopAmount = getCurrentOverScrollAmount(true);
        float newTopAmount = currentTopAmount - deltaY2;
        if (currentTopAmount > 0.0f) {
            setOverScrollAmount(newTopAmount, true, false);
        }
        float scrollAmount = newTopAmount < 0.0f ? -newTopAmount : 0.0f;
        float newScrollY = this.mOwnScrollY + scrollAmount;
        if (newScrollY > range) {
            if (!this.mExpandedInThisMotion) {
                float currentBottomPixels = getCurrentOverScrolledPixels(false);
                setOverScrolledPixels((currentBottomPixels + newScrollY) - range, false, false);
            }
            this.mOwnScrollY = range;
            return 0.0f;
        }
        return scrollAmount;
    }

    private float overScrollDown(int deltaY) {
        int deltaY2 = Math.min(deltaY, 0);
        float currentBottomAmount = getCurrentOverScrollAmount(false);
        float newBottomAmount = currentBottomAmount + deltaY2;
        if (currentBottomAmount > 0.0f) {
            setOverScrollAmount(newBottomAmount, false, false);
        }
        float scrollAmount = newBottomAmount < 0.0f ? newBottomAmount : 0.0f;
        float newScrollY = this.mOwnScrollY + scrollAmount;
        if (newScrollY < 0.0f) {
            float currentTopPixels = getCurrentOverScrolledPixels(true);
            setOverScrolledPixels(currentTopPixels - newScrollY, true, false);
            this.mOwnScrollY = 0;
            return 0.0f;
        }
        return scrollAmount;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = (ev.getAction() & 65280) >> 8;
        int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            this.mLastMotionY = (int) ev.getY(newPointerIndex);
            this.mActivePointerId = ev.getPointerId(newPointerIndex);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
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

    private void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    @Override
    public void computeScroll() {
        if (this.mScroller.computeScrollOffset()) {
            int oldX = this.mScrollX;
            int oldY = this.mOwnScrollY;
            int x = this.mScroller.getCurrX();
            int y = this.mScroller.getCurrY();
            if (oldX != x || oldY != y) {
                int range = getScrollRange();
                if ((y < 0 && oldY >= 0) || (y > range && oldY <= range)) {
                    float currVelocity = this.mScroller.getCurrVelocity();
                    if (currVelocity >= this.mMinimumVelocity) {
                        this.mMaxOverScroll = (Math.abs(currVelocity) / 1000.0f) * this.mOverflingDistance;
                    }
                }
                overScrollBy(x - oldX, y - oldY, oldX, oldY, 0, range, 0, (int) this.mMaxOverScroll, false);
                onScrollChanged(this.mScrollX, this.mOwnScrollY, oldX, oldY);
            }
            postInvalidateOnAnimation();
        }
    }

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        int newScrollY = scrollY + deltaY;
        int top = -maxOverScrollY;
        int bottom = maxOverScrollY + scrollRangeY;
        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }
        onOverScrolled(0, newScrollY, false, clampedY);
        return clampedY;
    }

    public void setOverScrolledPixels(float numPixels, boolean onTop, boolean animate) {
        setOverScrollAmount(getRubberBandFactor(onTop) * numPixels, onTop, animate, true);
    }

    public void setOverScrollAmount(float amount, boolean onTop, boolean animate) {
        setOverScrollAmount(amount, onTop, animate, true);
    }

    public void setOverScrollAmount(float amount, boolean onTop, boolean animate, boolean cancelAnimators) {
        setOverScrollAmount(amount, onTop, animate, cancelAnimators, isRubberbanded(onTop));
    }

    public void setOverScrollAmount(float amount, boolean onTop, boolean animate, boolean cancelAnimators, boolean isRubberbanded) {
        if (cancelAnimators) {
            this.mStateAnimator.cancelOverScrollAnimators(onTop);
        }
        setOverScrollAmountInternal(amount, onTop, animate, isRubberbanded);
    }

    private void setOverScrollAmountInternal(float amount, boolean onTop, boolean animate, boolean isRubberbanded) {
        float amount2 = Math.max(0.0f, amount);
        if (animate) {
            this.mStateAnimator.animateOverScrollToAmount(amount2, onTop, isRubberbanded);
            return;
        }
        setOverScrolledPixels(amount2 / getRubberBandFactor(onTop), onTop);
        this.mAmbientState.setOverScrollAmount(amount2, onTop);
        if (onTop) {
            notifyOverscrollTopListener(amount2, isRubberbanded);
        }
        requestChildrenUpdate();
    }

    private void notifyOverscrollTopListener(float amount, boolean isRubberbanded) {
        this.mExpandHelper.onlyObserveMovements(amount > 1.0f);
        if (this.mDontReportNextOverScroll) {
            this.mDontReportNextOverScroll = false;
        } else if (this.mOverscrollTopChangedListener != null) {
            this.mOverscrollTopChangedListener.onOverscrollTopChanged(amount, isRubberbanded);
        }
    }

    public void setOverscrollTopChangedListener(OnOverscrollTopChangedListener overscrollTopChangedListener) {
        this.mOverscrollTopChangedListener = overscrollTopChangedListener;
    }

    public float getCurrentOverScrollAmount(boolean top) {
        return this.mAmbientState.getOverScrollAmount(top);
    }

    public float getCurrentOverScrolledPixels(boolean top) {
        return top ? this.mOverScrolledTopPixels : this.mOverScrolledBottomPixels;
    }

    private void setOverScrolledPixels(float amount, boolean onTop) {
        if (onTop) {
            this.mOverScrolledTopPixels = amount;
        } else {
            this.mOverScrolledBottomPixels = amount;
        }
    }

    private void customScrollTo(int y) {
        this.mOwnScrollY = y;
        updateChildren();
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (!this.mScroller.isFinished()) {
            int oldX = this.mScrollX;
            int oldY = this.mOwnScrollY;
            this.mScrollX = scrollX;
            this.mOwnScrollY = scrollY;
            if (clampedY) {
                springBack();
                return;
            }
            onScrollChanged(this.mScrollX, this.mOwnScrollY, oldX, oldY);
            invalidateParentIfNeeded();
            updateChildren();
            float overScrollTop = getCurrentOverScrollAmount(true);
            if (this.mOwnScrollY < 0) {
                notifyOverscrollTopListener(-this.mOwnScrollY, isRubberbanded(true));
                return;
            } else {
                notifyOverscrollTopListener(overScrollTop, isRubberbanded(true));
                return;
            }
        }
        customScrollTo(scrollY);
        scrollTo(scrollX, this.mScrollY);
    }

    private void springBack() {
        boolean onTop;
        float newAmount;
        int scrollRange = getScrollRange();
        boolean overScrolledTop = this.mOwnScrollY <= 0;
        boolean overScrolledBottom = this.mOwnScrollY >= scrollRange;
        if (overScrolledTop || overScrolledBottom) {
            if (overScrolledTop) {
                onTop = true;
                newAmount = -this.mOwnScrollY;
                this.mOwnScrollY = 0;
                this.mDontReportNextOverScroll = true;
            } else {
                onTop = false;
                newAmount = this.mOwnScrollY - scrollRange;
                this.mOwnScrollY = scrollRange;
            }
            setOverScrollAmount(newAmount, onTop, false);
            setOverScrollAmount(0.0f, onTop, true);
            this.mScroller.forceFinished(true);
        }
    }

    private int getScrollRange() {
        ExpandableView firstChild = (ExpandableView) getFirstChildNotGone();
        if (firstChild == null) {
            return 0;
        }
        int contentHeight = getContentHeight();
        int firstChildMaxExpandHeight = getMaxExpandHeight(firstChild);
        int scrollRange = Math.max(0, (contentHeight - this.mMaxLayoutHeight) + this.mBottomStackPeekSize + this.mBottomStackSlowDownHeight);
        if (scrollRange > 0) {
            getLastChildNotGone();
            return Math.max(scrollRange, firstChildMaxExpandHeight - this.mCollapsedSize);
        }
        return scrollRange;
    }

    private View getFirstChildNotGone() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                return child;
            }
        }
        return null;
    }

    private View getFirstChildBelowTranlsationY(float translationY) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8 && child.getTranslationY() >= translationY) {
                return child;
            }
        }
        return null;
    }

    public View getLastChildNotGone() {
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                return child;
            }
        }
        return null;
    }

    public int getNotGoneChildCount() {
        int childCount = getChildCount();
        int count = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                count++;
            }
        }
        if (this.mDismissView.willBeGone()) {
            count--;
        }
        if (this.mEmptyShadeView.willBeGone()) {
            return count - 1;
        }
        return count;
    }

    private int getMaxExpandHeight(View view) {
        if (!(view instanceof ExpandableNotificationRow)) {
            return view.getHeight();
        }
        ExpandableNotificationRow row = (ExpandableNotificationRow) view;
        return row.getIntrinsicHeight();
    }

    public int getContentHeight() {
        return this.mContentHeight;
    }

    public void updateContentHeight() {
        int height = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                if (height != 0) {
                    height += this.mPaddingBetweenElements;
                }
                if (child instanceof ExpandableNotificationRow) {
                    ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                    height += row.getIntrinsicHeight();
                } else if (child instanceof ExpandableView) {
                    ExpandableView expandableView = (ExpandableView) child;
                    height += expandableView.getActualHeight();
                }
            }
        }
        this.mContentHeight = this.mTopPadding + height;
    }

    private void fling(int velocityY) {
        if (getChildCount() > 0) {
            int scrollRange = getScrollRange();
            float topAmount = getCurrentOverScrollAmount(true);
            float bottomAmount = getCurrentOverScrollAmount(false);
            if (velocityY < 0 && topAmount > 0.0f) {
                this.mOwnScrollY -= (int) topAmount;
                this.mDontReportNextOverScroll = true;
                setOverScrollAmount(0.0f, true, false);
                this.mMaxOverScroll = ((Math.abs(velocityY) / 1000.0f) * getRubberBandFactor(true) * this.mOverflingDistance) + topAmount;
            } else if (velocityY > 0 && bottomAmount > 0.0f) {
                this.mOwnScrollY = (int) (this.mOwnScrollY + bottomAmount);
                setOverScrollAmount(0.0f, false, false);
                this.mMaxOverScroll = ((Math.abs(velocityY) / 1000.0f) * getRubberBandFactor(false) * this.mOverflingDistance) + bottomAmount;
            } else {
                this.mMaxOverScroll = 0.0f;
            }
            this.mScroller.fling(this.mScrollX, this.mOwnScrollY, 1, velocityY, 0, 0, 0, Math.max(0, scrollRange), 0, 1073741823);
            postInvalidateOnAnimation();
        }
    }

    private boolean shouldOverScrollFling(int initialVelocity) {
        float topOverScroll = getCurrentOverScrollAmount(true);
        return this.mScrolledToTopOnFirstDown && !this.mExpandedInThisMotion && topOverScroll > this.mMinTopOverScrollToEscape && initialVelocity > 0;
    }

    public void updateTopPadding(float qsHeight, int scrollY, boolean animate, boolean ignoreIntrinsicPadding) {
        float start = (qsHeight - scrollY) + this.mNotificationTopPadding;
        float stackHeight = getHeight() - start;
        int minStackHeight = getMinStackHeight();
        if (stackHeight <= minStackHeight) {
            float overflow = minStackHeight - stackHeight;
            start = getHeight() - minStackHeight;
            this.mTopPaddingOverflow = overflow;
        } else {
            this.mTopPaddingOverflow = 0.0f;
        }
        setTopPadding(ignoreIntrinsicPadding ? (int) start : clampPadding((int) start), animate);
        setStackHeight(this.mLastSetStackHeight);
    }

    public int getNotificationTopPadding() {
        return this.mNotificationTopPadding;
    }

    public int getMinStackHeight() {
        return this.mCollapsedSize + this.mBottomStackPeekSize + this.mCollapseSecondCardPadding;
    }

    public float getTopPaddingOverflow() {
        return this.mTopPaddingOverflow;
    }

    public int getPeekHeight() {
        return this.mIntrinsicPadding + this.mCollapsedSize + this.mBottomStackPeekSize + this.mCollapseSecondCardPadding;
    }

    private int clampPadding(int desiredPadding) {
        return Math.max(desiredPadding, this.mIntrinsicPadding);
    }

    private float getRubberBandFactor(boolean onTop) {
        if (!onTop) {
            return 0.35f;
        }
        if (this.mExpandedInThisMotion) {
            return 0.15f;
        }
        if (this.mIsExpansionChanging) {
            return 0.21f;
        }
        return this.mScrolledToTopOnFirstDown ? 1.0f : 0.35f;
    }

    private boolean isRubberbanded(boolean onTop) {
        return !onTop || this.mExpandedInThisMotion || this.mIsExpansionChanging || !this.mScrolledToTopOnFirstDown;
    }

    private void endDrag() {
        setIsBeingDragged(false);
        recycleVelocityTracker();
        if (getCurrentOverScrollAmount(true) > 0.0f) {
            setOverScrollAmount(0.0f, true, true);
        }
        if (getCurrentOverScrollAmount(false) > 0.0f) {
            setOverScrollAmount(0.0f, false, true);
        }
    }

    private void transformTouchEvent(MotionEvent ev, View sourceView, View targetView) {
        ev.offsetLocation(sourceView.getX(), sourceView.getY());
        ev.offsetLocation(-targetView.getX(), -targetView.getY());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mInterceptDelegateEnabled) {
            transformTouchEvent(ev, this, this.mScrollView);
            if (this.mScrollView.onInterceptTouchEvent(ev)) {
                this.mDelegateToScrollView = true;
                removeLongPressCallback();
                return true;
            }
            transformTouchEvent(ev, this.mScrollView, this);
        }
        initDownStates(ev);
        handleEmptySpaceClick(ev);
        boolean expandWantsIt = false;
        if (!this.mSwipingInProgress && !this.mOnlyScrollingInThisMotion && isScrollingEnabled()) {
            expandWantsIt = this.mExpandHelper.onInterceptTouchEvent(ev);
        }
        boolean scrollWantsIt = false;
        if (!this.mSwipingInProgress && !this.mExpandingNotification) {
            scrollWantsIt = onInterceptTouchEventScroll(ev);
        }
        boolean swipeWantsIt = false;
        if (!this.mIsBeingDragged && !this.mExpandingNotification && !this.mExpandedInThisMotion && !this.mOnlyScrollingInThisMotion) {
            swipeWantsIt = this.mSwipeHelper.onInterceptTouchEvent(ev);
        }
        return swipeWantsIt || scrollWantsIt || expandWantsIt || super.onInterceptTouchEvent(ev);
    }

    private void handleEmptySpaceClick(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case 1:
                if (this.mPhoneStatusBar.getBarState() != 1 && this.mTouchIsClick && isBelowLastNotification(this.mInitialTouchX, this.mInitialTouchY)) {
                    this.mOnEmptySpaceClickListener.onEmptySpaceClicked(this.mInitialTouchX, this.mInitialTouchY);
                    break;
                }
                break;
            case 2:
                if (this.mTouchIsClick) {
                    if (Math.abs(ev.getY() - this.mInitialTouchY) > this.mTouchSlop || Math.abs(ev.getX() - this.mInitialTouchX) > this.mTouchSlop) {
                        this.mTouchIsClick = false;
                    }
                }
                break;
        }
    }

    private void initDownStates(MotionEvent ev) {
        if (ev.getAction() == 0) {
            this.mExpandedInThisMotion = false;
            this.mOnlyScrollingInThisMotion = !this.mScroller.isFinished();
            this.mDisallowScrollingInThisMotion = false;
            this.mTouchIsClick = true;
            this.mInitialTouchX = ev.getX();
            this.mInitialTouchY = ev.getY();
        }
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        this.mStackScrollAlgorithm.notifyChildrenChanged(this);
        if (!this.mChangePositionInProgress) {
            ((ExpandableView) child).setOnHeightChangedListener(null);
            this.mCurrentStackScrollState.removeViewStateForView(child);
            updateScrollStateForRemovedChild(child);
            boolean animationGenerated = generateRemoveAnimation(child);
            if (animationGenerated && !this.mSwipedOutViews.contains(child)) {
                getOverlay().add(child);
            }
            updateAnimationState(false, child);
            child.setClipBounds(null);
        }
    }

    private boolean generateRemoveAnimation(View child) {
        if (!this.mIsExpanded || !this.mAnimationsEnabled) {
            return false;
        }
        if (!this.mChildrenToAddAnimated.contains(child)) {
            this.mChildrenToRemoveAnimated.add(child);
            this.mNeedsAnimation = true;
            return true;
        }
        this.mChildrenToAddAnimated.remove(child);
        this.mFromMoreCardAdditions.remove(child);
        return false;
    }

    private void updateScrollStateForRemovedChild(View removedChild) {
        int startingPosition = getPositionInLinearLayout(removedChild);
        int childHeight = getIntrinsicHeight(removedChild) + this.mPaddingBetweenElements;
        int endPosition = startingPosition + childHeight;
        if (endPosition <= this.mOwnScrollY) {
            this.mOwnScrollY -= childHeight;
        } else if (startingPosition < this.mOwnScrollY) {
            this.mOwnScrollY = startingPosition;
        }
    }

    private int getIntrinsicHeight(View view) {
        if (!(view instanceof ExpandableView)) {
            return view.getHeight();
        }
        ExpandableView expandableView = (ExpandableView) view;
        return expandableView.getIntrinsicHeight();
    }

    private int getPositionInLinearLayout(View requestedChild) {
        int position = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != requestedChild) {
                if (child.getVisibility() != 8) {
                    position += getIntrinsicHeight(child);
                    if (i < getChildCount() - 1) {
                        position += this.mPaddingBetweenElements;
                    }
                }
            } else {
                return position;
            }
        }
        return 0;
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        this.mStackScrollAlgorithm.notifyChildrenChanged(this);
        ((ExpandableView) child).setOnHeightChangedListener(this);
        generateAddAnimation(child, false);
        updateAnimationState(child);
        if (canChildBeDismissed(child)) {
            this.mDismissView.showClearButton();
        }
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.mAnimationsEnabled = animationsEnabled;
        updateNotificationAnimationStates();
    }

    private void updateNotificationAnimationStates() {
        boolean running = this.mIsExpanded && this.mAnimationsEnabled;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            updateAnimationState(running, child);
        }
    }

    private void updateAnimationState(View child) {
        updateAnimationState(this.mAnimationsEnabled && this.mIsExpanded, child);
    }

    private void updateAnimationState(boolean running, View child) {
        if (child instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) child;
            row.setIconAnimationRunning(running);
        }
    }

    public boolean isAddOrRemoveAnimationPending() {
        return this.mNeedsAnimation && !(this.mChildrenToAddAnimated.isEmpty() && this.mChildrenToRemoveAnimated.isEmpty());
    }

    public void generateAddAnimation(View child, boolean fromMoreCard) {
        if (this.mIsExpanded && this.mAnimationsEnabled && !this.mChangePositionInProgress) {
            this.mChildrenToAddAnimated.add(child);
            if (fromMoreCard) {
                this.mFromMoreCardAdditions.add(child);
            }
            this.mNeedsAnimation = true;
        }
    }

    public void changeViewPosition(View child, int newIndex) {
        int currentIndex = indexOfChild(child);
        if (child != null && child.getParent() == this && currentIndex != newIndex) {
            this.mChangePositionInProgress = true;
            removeView(child);
            addView(child, newIndex);
            this.mChangePositionInProgress = false;
            if (this.mIsExpanded && this.mAnimationsEnabled && child.getVisibility() != 8) {
                this.mChildrenChangingPositions.add(child);
                this.mNeedsAnimation = true;
            }
        }
    }

    private void startAnimationToState() {
        if (this.mNeedsAnimation) {
            generateChildHierarchyEvents();
            this.mNeedsAnimation = false;
        }
        if (!this.mAnimationEvents.isEmpty() || isCurrentlyAnimating()) {
            this.mStateAnimator.startAnimationForEvents(this.mAnimationEvents, this.mCurrentStackScrollState, this.mGoToFullShadeDelay);
            this.mAnimationEvents.clear();
        } else {
            applyCurrentState();
        }
        this.mGoToFullShadeDelay = 0L;
    }

    private void generateChildHierarchyEvents() {
        generateChildRemovalEvents();
        generateChildAdditionEvents();
        generatePositionChangeEvents();
        generateSnapBackEvents();
        generateDragEvents();
        generateTopPaddingEvent();
        generateActivateEvent();
        generateDimmedEvent();
        generateHideSensitiveEvent();
        generateDarkEvent();
        generateGoToFullShadeEvent();
        generateViewResizeEvent();
        generateAnimateEverythingEvent();
        this.mNeedsAnimation = false;
    }

    private void generateViewResizeEvent() {
        if (this.mNeedViewResizeAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 12));
        }
        this.mNeedViewResizeAnimation = false;
    }

    private void generateSnapBackEvents() {
        for (View child : this.mSnappedBackChildren) {
            this.mAnimationEvents.add(new AnimationEvent(child, 5));
        }
        this.mSnappedBackChildren.clear();
    }

    private void generateDragEvents() {
        for (View child : this.mDragAnimPendingChildren) {
            this.mAnimationEvents.add(new AnimationEvent(child, 4));
        }
        this.mDragAnimPendingChildren.clear();
    }

    private void generateChildRemovalEvents() {
        for (View child : this.mChildrenToRemoveAnimated) {
            boolean childWasSwipedOut = this.mSwipedOutViews.contains(child);
            int animationType = childWasSwipedOut ? 2 : 1;
            AnimationEvent event = new AnimationEvent(child, animationType);
            event.viewAfterChangingView = getFirstChildBelowTranlsationY(child.getTranslationY());
            this.mAnimationEvents.add(event);
        }
        this.mSwipedOutViews.clear();
        this.mChildrenToRemoveAnimated.clear();
    }

    private void generatePositionChangeEvents() {
        for (View child : this.mChildrenChangingPositions) {
            this.mAnimationEvents.add(new AnimationEvent(child, 8));
        }
        this.mChildrenChangingPositions.clear();
    }

    private void generateChildAdditionEvents() {
        for (View child : this.mChildrenToAddAnimated) {
            if (this.mFromMoreCardAdditions.contains(child)) {
                this.mAnimationEvents.add(new AnimationEvent(child, 0, 360L));
            } else {
                this.mAnimationEvents.add(new AnimationEvent(child, 0));
            }
        }
        this.mChildrenToAddAnimated.clear();
        this.mFromMoreCardAdditions.clear();
    }

    private void generateTopPaddingEvent() {
        if (this.mTopPaddingNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 3));
        }
        this.mTopPaddingNeedsAnimation = false;
    }

    private void generateActivateEvent() {
        if (this.mActivateNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 6));
        }
        this.mActivateNeedsAnimation = false;
    }

    private void generateAnimateEverythingEvent() {
        if (this.mEverythingNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 13));
        }
        this.mEverythingNeedsAnimation = false;
    }

    private void generateDimmedEvent() {
        if (this.mDimmedNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 7));
        }
        this.mDimmedNeedsAnimation = false;
    }

    private void generateHideSensitiveEvent() {
        if (this.mHideSensitiveNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 11));
        }
        this.mHideSensitiveNeedsAnimation = false;
    }

    private void generateDarkEvent() {
        if (this.mDarkNeedsAnimation) {
            AnimationEvent ev = new AnimationEvent(null, 9);
            ev.darkAnimationOriginIndex = this.mDarkAnimationOriginIndex;
            this.mAnimationEvents.add(ev);
        }
        this.mDarkNeedsAnimation = false;
    }

    private void generateGoToFullShadeEvent() {
        if (this.mGoToFullShadeNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 10));
        }
        this.mGoToFullShadeNeedsAnimation = false;
    }

    private boolean onInterceptTouchEventScroll(MotionEvent ev) {
        if (!isScrollingEnabled()) {
            return false;
        }
        int action = ev.getAction();
        if (action == 2 && this.mIsBeingDragged) {
            return true;
        }
        switch (action & 255) {
            case 0:
                int y = (int) ev.getY();
                if (getChildAtPosition(ev.getX(), y) == null) {
                    setIsBeingDragged(false);
                    recycleVelocityTracker();
                } else {
                    this.mLastMotionY = y;
                    this.mDownX = (int) ev.getX();
                    this.mActivePointerId = ev.getPointerId(0);
                    this.mScrolledToTopOnFirstDown = isScrolledToTop();
                    initOrResetVelocityTracker();
                    this.mVelocityTracker.addMovement(ev);
                    boolean isBeingDragged = !this.mScroller.isFinished();
                    setIsBeingDragged(isBeingDragged);
                }
                break;
            case 1:
            case 3:
                setIsBeingDragged(false);
                this.mActivePointerId = -1;
                recycleVelocityTracker();
                if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                    postInvalidateOnAnimation();
                }
                break;
            case 2:
                int activePointerId = this.mActivePointerId;
                if (activePointerId != -1) {
                    int pointerIndex = ev.findPointerIndex(activePointerId);
                    if (pointerIndex == -1) {
                        Log.e("NotificationStackScrollLayout", "Invalid pointerId=" + activePointerId + " in onInterceptTouchEvent");
                    } else {
                        int y2 = (int) ev.getY(pointerIndex);
                        int x = (int) ev.getX(pointerIndex);
                        int yDiff = Math.abs(y2 - this.mLastMotionY);
                        int xDiff = Math.abs(x - this.mDownX);
                        if (yDiff > this.mTouchSlop && yDiff > xDiff) {
                            setIsBeingDragged(true);
                            this.mLastMotionY = y2;
                            this.mDownX = x;
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
        return this.mIsBeingDragged;
    }

    private boolean isInContentBounds(MotionEvent event) {
        return isInContentBounds(event.getY());
    }

    public boolean isInContentBounds(float y) {
        return y < ((float) (getHeight() - getEmptyBottomMargin()));
    }

    private void setIsBeingDragged(boolean isDragged) {
        this.mIsBeingDragged = isDragged;
        if (isDragged) {
            requestDisallowInterceptTouchEvent(true);
            removeLongPressCallback();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            removeLongPressCallback();
        }
    }

    public void removeLongPressCallback() {
        this.mSwipeHelper.removeLongPressCallback();
    }

    @Override
    public boolean isScrolledToTop() {
        return this.mOwnScrollY == 0;
    }

    public boolean isScrolledToBottom() {
        return this.mOwnScrollY >= getScrollRange();
    }

    @Override
    public View getHostView() {
        return this;
    }

    public int getEmptyBottomMargin() {
        int emptyMargin;
        int emptyMargin2 = (this.mMaxLayoutHeight - this.mContentHeight) - this.mBottomStackPeekSize;
        if (needsHeightAdaption()) {
            emptyMargin = emptyMargin2 - this.mBottomStackSlowDownHeight;
        } else {
            emptyMargin = emptyMargin2 - this.mCollapseSecondCardPadding;
        }
        return Math.max(emptyMargin, 0);
    }

    public void onExpansionStarted() {
        this.mIsExpansionChanging = true;
        this.mStackScrollAlgorithm.onExpansionStarted(this.mCurrentStackScrollState);
    }

    public void onExpansionStopped() {
        this.mIsExpansionChanging = false;
        this.mStackScrollAlgorithm.onExpansionStopped();
        if (!this.mIsExpanded) {
            this.mOwnScrollY = 0;
            getOverlay().clear();
        }
    }

    private void setIsExpanded(boolean isExpanded) {
        boolean changed = isExpanded != this.mIsExpanded;
        this.mIsExpanded = isExpanded;
        this.mStackScrollAlgorithm.setIsExpanded(isExpanded);
        if (changed) {
            updateNotificationAnimationStates();
        }
    }

    @Override
    public void onHeightChanged(ExpandableView view) {
        updateContentHeight();
        updateScrollPositionOnExpandInBottom(view);
        clampScrollPosition();
        notifyHeightChangeListener(view);
        requestChildrenUpdate();
    }

    @Override
    public void onReset(ExpandableView view) {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mRequestViewResizeAnimationOnLayout = true;
        }
        this.mStackScrollAlgorithm.onReset(view);
        updateAnimationState(view);
    }

    private void updateScrollPositionOnExpandInBottom(ExpandableView view) {
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) view;
            if (row.isUserLocked()) {
                float endPosition = row.getTranslationY() + row.getActualHeight();
                int stackEnd = (this.mMaxLayoutHeight - this.mBottomStackPeekSize) - this.mBottomStackSlowDownHeight;
                if (endPosition > stackEnd) {
                    this.mOwnScrollY = (int) (this.mOwnScrollY + (endPosition - stackEnd));
                    this.mDisallowScrollingInThisMotion = true;
                }
            }
        }
    }

    public void setOnHeightChangedListener(ExpandableView.OnHeightChangedListener mOnHeightChangedListener) {
        this.mOnHeightChangedListener = mOnHeightChangedListener;
    }

    public void setOnEmptySpaceClickListener(OnEmptySpaceClickListener listener) {
        this.mOnEmptySpaceClickListener = listener;
    }

    public void onChildAnimationFinished() {
        requestChildrenUpdate();
    }

    public void setDimmed(boolean dimmed, boolean animate) {
        this.mStackScrollAlgorithm.setDimmed(dimmed);
        this.mAmbientState.setDimmed(dimmed);
        updatePadding(dimmed);
        if (animate && this.mAnimationsEnabled) {
            this.mDimmedNeedsAnimation = true;
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public void setHideSensitive(boolean hideSensitive, boolean animate) {
        if (hideSensitive != this.mAmbientState.isHideSensitive()) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                ExpandableView v = (ExpandableView) getChildAt(i);
                v.setHideSensitiveForIntrinsicHeight(hideSensitive);
            }
            this.mAmbientState.setHideSensitive(hideSensitive);
            if (animate && this.mAnimationsEnabled) {
                this.mHideSensitiveNeedsAnimation = true;
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
        }
    }

    public void setActivatedChild(ActivatableNotificationView activatedChild) {
        this.mAmbientState.setActivatedChild(activatedChild);
        if (this.mAnimationsEnabled) {
            this.mActivateNeedsAnimation = true;
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public ActivatableNotificationView getActivatedChild() {
        return this.mAmbientState.getActivatedChild();
    }

    private void applyCurrentState() {
        this.mCurrentStackScrollState.apply();
        if (this.mListener != null) {
            this.mListener.onChildLocationsChanged(this);
        }
    }

    public void setSpeedBumpView(SpeedBumpView speedBumpView) {
        this.mSpeedBumpView = speedBumpView;
        addView(speedBumpView);
    }

    private void updateSpeedBump(boolean visible) {
        boolean notGoneBefore = this.mSpeedBumpView.getVisibility() != 8;
        if (visible != notGoneBefore) {
            int newVisibility = visible ? 0 : 8;
            this.mSpeedBumpView.setVisibility(newVisibility);
            if (visible) {
                this.mSpeedBumpView.setInvisible();
            } else {
                generateRemoveAnimation(this.mSpeedBumpView);
            }
        }
    }

    public void goToFullShade(long delay) {
        updateSpeedBump(true);
        this.mDismissView.setInvisible();
        this.mEmptyShadeView.setInvisible();
        this.mGoToFullShadeNeedsAnimation = true;
        this.mGoToFullShadeDelay = delay;
        this.mNeedsAnimation = true;
        requestChildrenUpdate();
    }

    public void cancelExpandHelper() {
        this.mExpandHelper.cancel();
    }

    public void setIntrinsicPadding(int intrinsicPadding) {
        this.mIntrinsicPadding = intrinsicPadding;
    }

    public int getIntrinsicPadding() {
        return this.mIntrinsicPadding;
    }

    public float getNotificationsTopY() {
        return this.mTopPadding + getTranslationY();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public void setDark(boolean dark, boolean animate, PointF touchWakeUpScreenLocation) {
        this.mAmbientState.setDark(dark);
        if (animate && this.mAnimationsEnabled) {
            this.mDarkNeedsAnimation = true;
            this.mDarkAnimationOriginIndex = findDarkAnimationOriginIndex(touchWakeUpScreenLocation);
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    private int findDarkAnimationOriginIndex(PointF screenLocation) {
        if (screenLocation == null || screenLocation.y < this.mTopPadding + this.mTopPaddingOverflow) {
            return -1;
        }
        if (screenLocation.y > getBottomMostNotificationBottom()) {
            return -2;
        }
        View child = getClosestChildAtRawPosition(screenLocation.x, screenLocation.y);
        if (child != null) {
            return getNotGoneIndex(child);
        }
        return -1;
    }

    private int getNotGoneIndex(View child) {
        int count = getChildCount();
        int notGoneIndex = 0;
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            if (child != v) {
                if (v.getVisibility() != 8) {
                    notGoneIndex++;
                }
            } else {
                return notGoneIndex;
            }
        }
        return -1;
    }

    public void setDismissView(DismissView dismissView) {
        this.mDismissView = dismissView;
        addView(this.mDismissView);
    }

    public void setEmptyShadeView(EmptyShadeView emptyShadeView) {
        this.mEmptyShadeView = emptyShadeView;
        addView(this.mEmptyShadeView);
    }

    public void updateEmptyShadeView(boolean visible) {
        int oldVisibility = this.mEmptyShadeView.willBeGone() ? 8 : this.mEmptyShadeView.getVisibility();
        int newVisibility = visible ? 0 : 8;
        if (oldVisibility != newVisibility) {
            if (newVisibility != 8) {
                if (this.mEmptyShadeView.willBeGone()) {
                    this.mEmptyShadeView.cancelAnimation();
                } else {
                    this.mEmptyShadeView.setInvisible();
                }
                this.mEmptyShadeView.setVisibility(newVisibility);
                this.mEmptyShadeView.setWillBeGone(false);
                updateContentHeight();
                notifyHeightChangeListener(this.mDismissView);
                return;
            }
            Runnable onFinishedRunnable = new Runnable() {
                @Override
                public void run() {
                    NotificationStackScrollLayout.this.mEmptyShadeView.setVisibility(8);
                    NotificationStackScrollLayout.this.mEmptyShadeView.setWillBeGone(false);
                    NotificationStackScrollLayout.this.updateContentHeight();
                    NotificationStackScrollLayout.this.notifyHeightChangeListener(NotificationStackScrollLayout.this.mDismissView);
                }
            };
            if (this.mAnimationsEnabled) {
                this.mEmptyShadeView.setWillBeGone(true);
                this.mEmptyShadeView.performVisibilityAnimation(false, onFinishedRunnable);
            } else {
                this.mEmptyShadeView.setInvisible();
                onFinishedRunnable.run();
            }
        }
    }

    public void updateDismissView(boolean visible) {
        int oldVisibility = this.mDismissView.willBeGone() ? 8 : this.mDismissView.getVisibility();
        int newVisibility = visible ? 0 : 8;
        if (oldVisibility != newVisibility) {
            if (newVisibility != 8) {
                if (this.mDismissView.willBeGone()) {
                    this.mDismissView.cancelAnimation();
                } else {
                    this.mDismissView.setInvisible();
                }
                this.mDismissView.setVisibility(newVisibility);
                this.mDismissView.setWillBeGone(false);
                updateContentHeight();
                notifyHeightChangeListener(this.mDismissView);
                return;
            }
            Runnable dimissHideFinishRunnable = new Runnable() {
                @Override
                public void run() {
                    NotificationStackScrollLayout.this.mDismissView.setVisibility(8);
                    NotificationStackScrollLayout.this.mDismissView.setWillBeGone(false);
                    NotificationStackScrollLayout.this.updateContentHeight();
                    NotificationStackScrollLayout.this.notifyHeightChangeListener(NotificationStackScrollLayout.this.mDismissView);
                }
            };
            if (this.mDismissView.isButtonVisible() && this.mIsExpanded && this.mAnimationsEnabled) {
                this.mDismissView.setWillBeGone(true);
                this.mDismissView.performVisibilityAnimation(false, dimissHideFinishRunnable);
            } else {
                dimissHideFinishRunnable.run();
                this.mDismissView.showClearButton();
            }
        }
    }

    public void setDismissAllInProgress(boolean dismissAllInProgress) {
        this.mDismissAllInProgress = dismissAllInProgress;
        this.mDismissView.setDismissAllInProgress(dismissAllInProgress);
    }

    public boolean isDismissViewNotGone() {
        return (this.mDismissView.getVisibility() == 8 || this.mDismissView.willBeGone()) ? false : true;
    }

    public boolean isDismissViewVisible() {
        return this.mDismissView.isVisible();
    }

    public int getDismissViewHeight() {
        int height = this.mDismissView.getHeight() + this.mPaddingBetweenElementsNormal;
        if (getNotGoneChildCount() == 2 && getLastChildNotGone() == this.mDismissView && (getFirstChildNotGone() instanceof ActivatableNotificationView)) {
            return height + this.mCollapseSecondCardPadding;
        }
        return height;
    }

    public int getEmptyShadeViewHeight() {
        return this.mEmptyShadeView.getHeight();
    }

    public float getBottomMostNotificationBottom() {
        int count = getChildCount();
        float max = 0.0f;
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableView child = (ExpandableView) getChildAt(childIdx);
            if (child.getVisibility() != 8) {
                float bottom = child.getTranslationY() + child.getActualHeight();
                if (bottom > max) {
                    max = bottom;
                }
            }
        }
        return getTranslationY() + max;
    }

    public void updateIsSmallScreen(int qsMinHeight) {
        this.mStackScrollAlgorithm.updateIsSmallScreen(this.mMaxLayoutHeight - qsMinHeight);
    }

    public void setPhoneStatusBar(PhoneStatusBar phoneStatusBar) {
        this.mPhoneStatusBar = phoneStatusBar;
    }

    public void onGoToKeyguard() {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mEverythingNeedsAnimation = true;
            requestChildrenUpdate();
        }
    }

    private boolean isBelowLastNotification(float touchX, float touchY) {
        boolean z = false;
        ExpandableView lastChildNotGone = (ExpandableView) getLastChildNotGone();
        if (lastChildNotGone == null) {
            return touchY > ((float) this.mIntrinsicPadding);
        }
        if (lastChildNotGone != this.mDismissView && lastChildNotGone != this.mEmptyShadeView) {
            return touchY > lastChildNotGone.getY() + ((float) lastChildNotGone.getActualHeight());
        }
        if (lastChildNotGone == this.mEmptyShadeView) {
            return touchY > this.mEmptyShadeView.getY();
        }
        float dismissY = this.mDismissView.getY();
        boolean belowDismissView = touchY > ((float) this.mDismissView.getActualHeight()) + dismissY;
        if (belowDismissView || (touchY > dismissY && this.mDismissView.isOnEmptySpace(touchX - this.mDismissView.getX(), touchY - dismissY))) {
            z = true;
        }
        return z;
    }

    static class AnimationEvent {
        static AnimationFilter[] FILTERS = {new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateDimmed().animateScale().animateZ(), new AnimationFilter().animateAlpha(), new AnimationFilter().animateAlpha().animateHeight(), new AnimationFilter().animateScale().animateAlpha(), new AnimationFilter().animateY().animateScale().animateDimmed(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateDark().hasDelays(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateDimmed().animateScale().animateZ().hasDelays(), new AnimationFilter().animateHideSensitive(), new AnimationFilter().animateAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateDark().animateScale().animateDimmed().animateHideSensitive().animateHeight().animateTopInset().animateY().animateZ()};
        static int[] LENGTHS = {464, 464, 360, 360, 360, 360, 220, 220, 360, 360, 448, 360, 360, 360};
        final int animationType;
        final View changingView;
        int darkAnimationOriginIndex;
        final long eventStartTime;
        final AnimationFilter filter;
        final long length;
        View viewAfterChangingView;

        AnimationEvent(View view, int type) {
            this(view, type, LENGTHS[type]);
        }

        AnimationEvent(View view, int type, long length) {
            this.eventStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.changingView = view;
            this.animationType = type;
            this.filter = FILTERS[type];
            this.length = length;
        }

        static long combineLength(ArrayList<AnimationEvent> events) {
            long length = 0;
            int size = events.size();
            for (int i = 0; i < size; i++) {
                AnimationEvent event = events.get(i);
                length = Math.max(length, event.length);
                if (event.animationType == 10) {
                    return event.length;
                }
            }
            return length;
        }
    }
}
