package com.android.systemui.statusbar.phone;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import com.android.systemui.EventLogTags;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class PanelView extends FrameLayout {
    public static final String TAG = PanelView.class.getSimpleName();
    PanelBar mBar;
    private Interpolator mBounceInterpolator;
    private boolean mClosing;
    private boolean mCollapseAfterPeek;
    private boolean mDozingOnDown;
    private int mEdgeTapAreaWidth;
    private float mExpandedFraction;
    protected float mExpandedHeight;
    private boolean mExpanding;
    private Interpolator mFastOutSlowInInterpolator;
    private FlingAnimationUtils mFlingAnimationUtils;
    private final Runnable mFlingCollapseRunnable;
    private boolean mGestureWaitForTouchSlop;
    private boolean mHasLayoutedSinceDown;
    private ValueAnimator mHeightAnimator;
    protected boolean mHintAnimationRunning;
    private float mHintDistance;
    private float mInitialOffsetOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private boolean mInstantExpanding;
    private boolean mJustPeeked;
    protected KeyguardBottomAreaView mKeyguardBottomArea;
    private Interpolator mLinearOutSlowInInterpolator;
    private boolean mOverExpandedBeforeFling;
    private boolean mPanelClosedOnDown;
    private ObjectAnimator mPeekAnimator;
    private float mPeekHeight;
    private boolean mPeekPending;
    private Runnable mPeekRunnable;
    private boolean mPeekTouching;
    private final Runnable mPostCollapseRunnable;
    protected PhoneStatusBar mStatusBar;
    private boolean mTouchAboveFalsingThreshold;
    private boolean mTouchDisabled;
    protected int mTouchSlop;
    private boolean mTouchSlopExceeded;
    private boolean mTouchStartedInEmptyArea;
    protected boolean mTracking;
    private int mTrackingPointer;
    private int mUnlockFalsingThreshold;
    private boolean mUpdateFlingOnLayout;
    private float mUpdateFlingVelocity;
    private VelocityTrackerInterface mVelocityTracker;
    private String mViewName;

    protected abstract boolean fullyExpandedClearAllVisible();

    protected abstract float getCannedFlingDurationFactor();

    protected abstract int getClearAllHeight();

    protected abstract int getMaxPanelHeight();

    protected abstract float getOverExpansionAmount();

    protected abstract float getOverExpansionPixels();

    protected abstract float getPeekHeight();

    protected abstract boolean hasConflictingGestures();

    protected abstract boolean isClearAllVisible();

    protected abstract boolean isDozing();

    protected abstract boolean isInContentBounds(float f, float f2);

    protected abstract boolean isTrackingBlocked();

    protected abstract void onEdgeClicked(boolean z);

    protected abstract void onHeightUpdated(float f);

    public abstract void resetViews();

    protected abstract void setOverExpansion(float f, boolean z);

    protected void onExpandingFinished() {
        endClosing();
        this.mBar.onExpandingFinished();
    }

    protected void onExpandingStarted() {
    }

    private void notifyExpandingStarted() {
        if (!this.mExpanding) {
            this.mExpanding = true;
            onExpandingStarted();
        }
    }

    private void notifyExpandingFinished() {
        if (this.mExpanding) {
            this.mExpanding = false;
            onExpandingFinished();
        }
    }

    private void schedulePeek() {
        this.mPeekPending = true;
        long timeout = ViewConfiguration.getTapTimeout();
        postOnAnimationDelayed(this.mPeekRunnable, timeout);
        notifyBarPanelExpansionChanged();
    }

    private void runPeekAnimation() {
        this.mPeekHeight = getPeekHeight();
        if (this.mHeightAnimator == null) {
            this.mPeekAnimator = ObjectAnimator.ofFloat(this, "expandedHeight", this.mPeekHeight).setDuration(250L);
            this.mPeekAnimator.setInterpolator(this.mLinearOutSlowInInterpolator);
            this.mPeekAnimator.addListener(new AnimatorListenerAdapter() {
                private boolean mCancelled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    PanelView.this.mPeekAnimator = null;
                    if (PanelView.this.mCollapseAfterPeek && !this.mCancelled) {
                        PanelView.this.postOnAnimation(new Runnable() {
                            @Override
                            public void run() {
                                PanelView.this.collapse(false);
                            }
                        });
                    }
                    PanelView.this.mCollapseAfterPeek = false;
                }
            });
            notifyExpandingStarted();
            this.mPeekAnimator.start();
            this.mJustPeeked = true;
        }
    }

    public PanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mExpandedFraction = 0.0f;
        this.mExpandedHeight = 0.0f;
        this.mPeekRunnable = new Runnable() {
            @Override
            public void run() {
                PanelView.this.mPeekPending = false;
                PanelView.this.runPeekAnimation();
            }
        };
        this.mFlingCollapseRunnable = new Runnable() {
            @Override
            public void run() {
                PanelView.this.fling(0.0f, false);
            }
        };
        this.mPostCollapseRunnable = new Runnable() {
            @Override
            public void run() {
                PanelView.this.collapse(false);
            }
        };
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.6f);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_slow_in);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.linear_out_slow_in);
        this.mBounceInterpolator = new BounceInterpolator();
    }

    protected void loadDimens() {
        Resources res = getContext().getResources();
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mHintDistance = res.getDimension(com.android.systemui.R.dimen.hint_move_distance);
        this.mEdgeTapAreaWidth = res.getDimensionPixelSize(com.android.systemui.R.dimen.edge_tap_area_width);
        this.mUnlockFalsingThreshold = res.getDimensionPixelSize(com.android.systemui.R.dimen.unlock_falsing_threshold);
    }

    private void trackMovement(MotionEvent event) {
        float deltaX = event.getRawX() - event.getX();
        float deltaY = event.getRawY() - event.getY();
        event.offsetLocation(deltaX, deltaY);
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
        }
        event.offsetLocation(-deltaX, -deltaY);
    }

    public void setTouchDisabled(boolean disabled) {
        this.mTouchDisabled = disabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mInstantExpanding || this.mTouchDisabled) {
            return false;
        }
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float y = event.getY(pointerIndex);
        float x = event.getX(pointerIndex);
        if (event.getActionMasked() == 0) {
            this.mGestureWaitForTouchSlop = this.mExpandedHeight == 0.0f;
        }
        boolean waitForTouchSlop = hasConflictingGestures() || this.mGestureWaitForTouchSlop;
        switch (event.getActionMasked()) {
            case 0:
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                this.mInitialOffsetOnTouch = this.mExpandedHeight;
                this.mTouchSlopExceeded = false;
                this.mJustPeeked = false;
                this.mPanelClosedOnDown = this.mExpandedHeight == 0.0f;
                this.mHasLayoutedSinceDown = false;
                this.mUpdateFlingOnLayout = false;
                this.mPeekTouching = this.mPanelClosedOnDown;
                this.mTouchAboveFalsingThreshold = false;
                this.mDozingOnDown = isDozing();
                if (this.mVelocityTracker == null) {
                    initVelocityTracker();
                }
                trackMovement(event);
                if (!waitForTouchSlop || ((this.mHeightAnimator != null && !this.mHintAnimationRunning) || this.mPeekPending || this.mPeekAnimator != null)) {
                    cancelHeightAnimator();
                    cancelPeek();
                    this.mTouchSlopExceeded = ((this.mHeightAnimator == null || this.mHintAnimationRunning) && !this.mPeekPending && this.mPeekAnimator == null) ? false : true;
                    onTrackingStarted();
                }
                if (this.mExpandedHeight == 0.0f) {
                    schedulePeek();
                }
                break;
            case 1:
            case 3:
                this.mTrackingPointer = -1;
                trackMovement(event);
                if ((this.mTracking && this.mTouchSlopExceeded) || Math.abs(x - this.mInitialTouchX) > this.mTouchSlop || Math.abs(y - this.mInitialTouchY) > this.mTouchSlop || event.getActionMasked() == 3) {
                    float vel = 0.0f;
                    float vectorVel = 0.0f;
                    if (this.mVelocityTracker != null) {
                        this.mVelocityTracker.computeCurrentVelocity(1000);
                        vel = this.mVelocityTracker.getYVelocity();
                        vectorVel = (float) Math.hypot(this.mVelocityTracker.getXVelocity(), this.mVelocityTracker.getYVelocity());
                    }
                    boolean expand = flingExpands(vel, vectorVel) || event.getActionMasked() == 3;
                    onTrackingStopped(expand);
                    DozeLog.traceFling(expand, this.mTouchAboveFalsingThreshold, this.mStatusBar.isFalsingThresholdNeeded(), this.mStatusBar.isScreenOnComingFromTouch());
                    if (!expand && this.mStatusBar.getBarState() == 1) {
                        float displayDensity = this.mStatusBar.getDisplayDensity();
                        int heightDp = (int) Math.abs((y - this.mInitialTouchY) / displayDensity);
                        int velocityDp = (int) Math.abs(vel / displayDensity);
                        EventLogTags.writeSysuiLockscreenGesture(1, heightDp, velocityDp);
                    }
                    fling(vel, expand);
                    this.mUpdateFlingOnLayout = expand && this.mPanelClosedOnDown && !this.mHasLayoutedSinceDown;
                    if (this.mUpdateFlingOnLayout) {
                        this.mUpdateFlingVelocity = vel;
                    }
                } else {
                    boolean expands = onEmptySpaceClick(this.mInitialTouchX);
                    onTrackingStopped(expands);
                }
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                }
                this.mPeekTouching = false;
                break;
            case 2:
                float h = y - this.mInitialTouchY;
                if (Math.abs(h) > this.mTouchSlop && (Math.abs(h) > Math.abs(x - this.mInitialTouchX) || this.mInitialOffsetOnTouch == 0.0f)) {
                    this.mTouchSlopExceeded = true;
                    if (waitForTouchSlop && !this.mTracking) {
                        if (!this.mJustPeeked && this.mInitialOffsetOnTouch != 0.0f) {
                            this.mInitialOffsetOnTouch = this.mExpandedHeight;
                            this.mInitialTouchX = x;
                            this.mInitialTouchY = y;
                            h = 0.0f;
                        }
                        cancelHeightAnimator();
                        removeCallbacks(this.mPeekRunnable);
                        this.mPeekPending = false;
                        onTrackingStarted();
                    }
                }
                float newHeight = Math.max(0.0f, this.mInitialOffsetOnTouch + h);
                if (newHeight > this.mPeekHeight) {
                    if (this.mPeekAnimator != null) {
                        this.mPeekAnimator.cancel();
                    }
                    this.mJustPeeked = false;
                }
                if ((-h) >= getFalsingThreshold()) {
                    this.mTouchAboveFalsingThreshold = true;
                }
                if (!this.mJustPeeked && ((!waitForTouchSlop || this.mTracking) && !isTrackingBlocked())) {
                    setExpandedHeightInternal(newHeight);
                }
                trackMovement(event);
                break;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    float newY = event.getY(newIndex);
                    float newX = event.getX(newIndex);
                    this.mTrackingPointer = event.getPointerId(newIndex);
                    this.mInitialOffsetOnTouch = this.mExpandedHeight;
                    this.mInitialTouchY = newY;
                    this.mInitialTouchX = newX;
                }
                break;
        }
        return !waitForTouchSlop || this.mTracking;
    }

    private int getFalsingThreshold() {
        float factor = this.mStatusBar.isScreenOnComingFromTouch() ? 1.5f : 1.0f;
        return (int) (this.mUnlockFalsingThreshold * factor);
    }

    protected void onTrackingStopped(boolean expand) {
        this.mTracking = false;
        this.mBar.onTrackingStopped(this, expand);
    }

    protected void onTrackingStarted() {
        endClosing();
        this.mTracking = true;
        this.mCollapseAfterPeek = false;
        this.mBar.onTrackingStarted(this);
        notifyExpandingStarted();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mInstantExpanding) {
            return false;
        }
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        boolean scrolledToBottom = isScrolledToBottom();
        switch (event.getActionMasked()) {
            case 0:
                this.mStatusBar.userActivity();
                if ((this.mHeightAnimator != null && !this.mHintAnimationRunning) || this.mPeekPending || this.mPeekAnimator != null) {
                    cancelHeightAnimator();
                    cancelPeek();
                    this.mTouchSlopExceeded = true;
                    return true;
                }
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                this.mTouchStartedInEmptyArea = !isInContentBounds(x, y);
                this.mTouchSlopExceeded = false;
                this.mJustPeeked = false;
                this.mPanelClosedOnDown = this.mExpandedHeight == 0.0f;
                this.mHasLayoutedSinceDown = false;
                this.mUpdateFlingOnLayout = false;
                this.mTouchAboveFalsingThreshold = false;
                this.mDozingOnDown = isDozing();
                initVelocityTracker();
                trackMovement(event);
                return false;
            case 2:
                float h = y - this.mInitialTouchY;
                trackMovement(event);
                if ((!scrolledToBottom && !this.mTouchStartedInEmptyArea) || h >= (-this.mTouchSlop) || h >= (-Math.abs(x - this.mInitialTouchX))) {
                    return false;
                }
                cancelHeightAnimator();
                this.mInitialOffsetOnTouch = this.mExpandedHeight;
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                this.mTracking = true;
                this.mTouchSlopExceeded = true;
                onTrackingStarted();
                return true;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer != upPointer) {
                    return false;
                }
                int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                this.mTrackingPointer = event.getPointerId(newIndex);
                this.mInitialTouchX = event.getX(newIndex);
                this.mInitialTouchY = event.getY(newIndex);
                return false;
            default:
                return false;
        }
    }

    private void cancelHeightAnimator() {
        if (this.mHeightAnimator != null) {
            this.mHeightAnimator.cancel();
        }
        endClosing();
    }

    private void endClosing() {
        if (this.mClosing) {
            this.mClosing = false;
            onClosingFinished();
        }
    }

    private void initVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTrackerFactory.obtain(getContext());
    }

    protected boolean isScrolledToBottom() {
        return true;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        loadDimens();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadDimens();
    }

    protected boolean flingExpands(float vel, float vectorVel) {
        if (isBelowFalsingThreshold()) {
            return true;
        }
        return Math.abs(vectorVel) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond() ? getExpandedFraction() > 0.5f : vel > 0.0f;
    }

    private boolean isBelowFalsingThreshold() {
        return !this.mTouchAboveFalsingThreshold && this.mStatusBar.isFalsingThresholdNeeded();
    }

    protected void fling(float vel, boolean expand) {
        cancelPeek();
        float target = expand ? getMaxPanelHeight() : 0.0f;
        final boolean clearAllExpandHack = expand && fullyExpandedClearAllVisible() && this.mExpandedHeight < ((float) (getMaxPanelHeight() - getClearAllHeight())) && !isClearAllVisible();
        if (clearAllExpandHack) {
            target = getMaxPanelHeight() - getClearAllHeight();
        }
        if (target == this.mExpandedHeight || (getOverExpansionAmount() > 0.0f && expand)) {
            notifyExpandingFinished();
            return;
        }
        this.mOverExpandedBeforeFling = getOverExpansionAmount() > 0.0f;
        ValueAnimator animator = createHeightAnimator(target);
        if (expand) {
            boolean belowFalsingThreshold = isBelowFalsingThreshold();
            if (belowFalsingThreshold) {
                vel = 0.0f;
            }
            this.mFlingAnimationUtils.apply(animator, this.mExpandedHeight, target, vel, getHeight());
            if (belowFalsingThreshold) {
                animator.setDuration(350L);
            }
        } else {
            this.mFlingAnimationUtils.applyDismissing(animator, this.mExpandedHeight, target, vel, getHeight());
            if (vel == 0.0f) {
                animator.setDuration((long) (animator.getDuration() * getCannedFlingDurationFactor()));
            }
        }
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (clearAllExpandHack && !this.mCancelled) {
                    PanelView.this.setExpandedHeightInternal(PanelView.this.getMaxPanelHeight());
                }
                PanelView.this.mHeightAnimator = null;
                if (!this.mCancelled) {
                    PanelView.this.notifyExpandingFinished();
                }
            }
        });
        this.mHeightAnimator = animator;
        animator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mViewName = getResources().getResourceName(getId());
    }

    public void setExpandedHeight(float height) {
        setExpandedHeightInternal(getOverExpansionPixels() + height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        requestPanelHeightUpdate();
        this.mHasLayoutedSinceDown = true;
        if (this.mUpdateFlingOnLayout) {
            abortAnimations();
            fling(this.mUpdateFlingVelocity, true);
            this.mUpdateFlingOnLayout = false;
        }
    }

    protected void requestPanelHeightUpdate() {
        float currentMaxPanelHeight = getMaxPanelHeight();
        if ((!this.mTracking || isTrackingBlocked()) && this.mHeightAnimator == null && this.mExpandedHeight > 0.0f && currentMaxPanelHeight != this.mExpandedHeight && !this.mPeekPending && this.mPeekAnimator == null && !this.mPeekTouching) {
            setExpandedHeight(currentMaxPanelHeight);
        }
    }

    public void setExpandedHeightInternal(float h) {
        float fhWithoutOverExpansion = getMaxPanelHeight() - getOverExpansionAmount();
        if (this.mHeightAnimator == null) {
            float overExpansionPixels = Math.max(0.0f, h - fhWithoutOverExpansion);
            if (getOverExpansionPixels() != overExpansionPixels && this.mTracking) {
                setOverExpansion(overExpansionPixels, true);
            }
            this.mExpandedHeight = Math.min(h, fhWithoutOverExpansion) + getOverExpansionAmount();
        } else {
            this.mExpandedHeight = h;
            if (this.mOverExpandedBeforeFling) {
                setOverExpansion(Math.max(0.0f, h - fhWithoutOverExpansion), false);
            }
        }
        this.mExpandedHeight = Math.max(0.0f, this.mExpandedHeight);
        this.mExpandedFraction = Math.min(1.0f, fhWithoutOverExpansion != 0.0f ? this.mExpandedHeight / fhWithoutOverExpansion : 0.0f);
        onHeightUpdated(this.mExpandedHeight);
        notifyBarPanelExpansionChanged();
    }

    public void setExpandedFraction(float frac) {
        setExpandedHeight(getMaxPanelHeight() * frac);
    }

    public float getExpandedHeight() {
        return this.mExpandedHeight;
    }

    public float getExpandedFraction() {
        return this.mExpandedFraction;
    }

    public boolean isFullyExpanded() {
        return this.mExpandedHeight >= ((float) getMaxPanelHeight());
    }

    public boolean isFullyCollapsed() {
        return this.mExpandedHeight <= 0.0f;
    }

    public boolean isCollapsing() {
        return this.mClosing;
    }

    public boolean isTracking() {
        return this.mTracking;
    }

    public void setBar(PanelBar panelBar) {
        this.mBar = panelBar;
    }

    public void collapse(boolean delayed) {
        if (this.mPeekPending || this.mPeekAnimator != null) {
            this.mCollapseAfterPeek = true;
            if (this.mPeekPending) {
                removeCallbacks(this.mPeekRunnable);
                this.mPeekRunnable.run();
                return;
            }
            return;
        }
        if (!isFullyCollapsed() && !this.mTracking && !this.mClosing) {
            cancelHeightAnimator();
            this.mClosing = true;
            notifyExpandingStarted();
            if (delayed) {
                postDelayed(this.mFlingCollapseRunnable, 120L);
            } else {
                fling(0.0f, false);
            }
        }
    }

    public void expand() {
        if (isFullyCollapsed()) {
            this.mBar.startOpeningPanel(this);
            notifyExpandingStarted();
            fling(0.0f, true);
        }
    }

    public void cancelPeek() {
        if (this.mPeekAnimator != null) {
            this.mPeekAnimator.cancel();
        }
        removeCallbacks(this.mPeekRunnable);
        this.mPeekPending = false;
        notifyBarPanelExpansionChanged();
    }

    public void instantExpand() {
        this.mInstantExpanding = true;
        this.mUpdateFlingOnLayout = false;
        abortAnimations();
        cancelPeek();
        if (this.mTracking) {
            onTrackingStopped(true);
        }
        if (this.mExpanding) {
            notifyExpandingFinished();
        }
        setVisibility(0);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (PanelView.this.mStatusBar.getStatusBarWindow().getHeight() != PanelView.this.mStatusBar.getStatusBarHeight()) {
                    PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    PanelView.this.setExpandedFraction(1.0f);
                    PanelView.this.mInstantExpanding = false;
                }
            }
        });
        requestLayout();
    }

    public void instantCollapse() {
        abortAnimations();
        setExpandedFraction(0.0f);
        if (this.mExpanding) {
            notifyExpandingFinished();
        }
    }

    private void abortAnimations() {
        cancelPeek();
        cancelHeightAnimator();
        removeCallbacks(this.mPostCollapseRunnable);
        removeCallbacks(this.mFlingCollapseRunnable);
    }

    protected void onClosingFinished() {
        this.mBar.onClosingFinished();
    }

    protected void startUnlockHintAnimation() {
        if (this.mHeightAnimator == null && !this.mTracking) {
            cancelPeek();
            notifyExpandingStarted();
            startUnlockHintAnimationPhase1(new Runnable() {
                @Override
                public void run() {
                    PanelView.this.notifyExpandingFinished();
                    PanelView.this.mStatusBar.onHintFinished();
                    PanelView.this.mHintAnimationRunning = false;
                }
            });
            this.mStatusBar.onUnlockHintStarted();
            this.mHintAnimationRunning = true;
        }
    }

    private void startUnlockHintAnimationPhase1(final Runnable onAnimationFinished) {
        float target = Math.max(0.0f, getMaxPanelHeight() - this.mHintDistance);
        ValueAnimator animator = createHeightAnimator(target);
        animator.setDuration(250L);
        animator.setInterpolator(this.mFastOutSlowInInterpolator);
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (this.mCancelled) {
                    PanelView.this.mHeightAnimator = null;
                    onAnimationFinished.run();
                } else {
                    PanelView.this.startUnlockHintAnimationPhase2(onAnimationFinished);
                }
            }
        });
        animator.start();
        this.mHeightAnimator = animator;
        this.mKeyguardBottomArea.getIndicationView().animate().translationY(-this.mHintDistance).setDuration(250L).setInterpolator(this.mFastOutSlowInInterpolator).withEndAction(new Runnable() {
            @Override
            public void run() {
                PanelView.this.mKeyguardBottomArea.getIndicationView().animate().translationY(0.0f).setDuration(450L).setInterpolator(PanelView.this.mBounceInterpolator).start();
            }
        }).start();
    }

    private void startUnlockHintAnimationPhase2(final Runnable onAnimationFinished) {
        ValueAnimator animator = createHeightAnimator(getMaxPanelHeight());
        animator.setDuration(450L);
        animator.setInterpolator(this.mBounceInterpolator);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                PanelView.this.mHeightAnimator = null;
                onAnimationFinished.run();
            }
        });
        animator.start();
        this.mHeightAnimator = animator;
    }

    private ValueAnimator createHeightAnimator(float targetHeight) {
        ValueAnimator animator = ValueAnimator.ofFloat(this.mExpandedHeight, targetHeight);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                PanelView.this.setExpandedHeightInternal(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        return animator;
    }

    private void notifyBarPanelExpansionChanged() {
        this.mBar.panelExpansionChanged(this, this.mExpandedFraction, this.mExpandedFraction > 0.0f || this.mPeekPending || this.mPeekAnimator != null);
    }

    protected boolean onEmptySpaceClick(float x) {
        if (this.mHintAnimationRunning) {
            return true;
        }
        if (x < this.mEdgeTapAreaWidth && this.mStatusBar.getBarState() == 1) {
            onEdgeClicked(false);
            return true;
        }
        if (x > getWidth() - this.mEdgeTapAreaWidth && this.mStatusBar.getBarState() == 1) {
            onEdgeClicked(true);
            return true;
        }
        return onMiddleClicked();
    }

    private boolean onMiddleClicked() {
        switch (this.mStatusBar.getBarState()) {
            case 0:
                post(this.mPostCollapseRunnable);
                break;
            case 1:
                if (!this.mDozingOnDown) {
                    EventLogTags.writeSysuiLockscreenGesture(3, 0, 0);
                    startUnlockHintAnimation();
                }
                break;
            case 2:
                this.mStatusBar.goToKeyguard();
                break;
        }
        return true;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Object[] objArr = new Object[11];
        objArr[0] = getClass().getSimpleName();
        objArr[1] = Float.valueOf(getExpandedHeight());
        objArr[2] = Integer.valueOf(getMaxPanelHeight());
        objArr[3] = this.mClosing ? "T" : "f";
        objArr[4] = this.mTracking ? "T" : "f";
        objArr[5] = this.mJustPeeked ? "T" : "f";
        objArr[6] = this.mPeekAnimator;
        objArr[7] = (this.mPeekAnimator == null || !this.mPeekAnimator.isStarted()) ? "" : " (started)";
        objArr[8] = this.mHeightAnimator;
        objArr[9] = (this.mHeightAnimator == null || !this.mHeightAnimator.isStarted()) ? "" : " (started)";
        objArr[10] = this.mTouchDisabled ? "T" : "f";
        pw.println(String.format("[PanelView(%s): expandedHeight=%f maxPanelHeight=%d closing=%s tracking=%s justPeeked=%s peekAnim=%s%s timeAnim=%s%s touchDisabled=%s]", objArr));
    }
}
