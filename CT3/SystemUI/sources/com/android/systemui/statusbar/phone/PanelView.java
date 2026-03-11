package com.android.systemui.statusbar.phone;

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
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import com.android.systemui.EventLogTags;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class PanelView extends FrameLayout {
    public static final String TAG = PanelView.class.getSimpleName();
    private boolean mAnimatingOnDown;
    PanelBar mBar;
    private Interpolator mBounceInterpolator;
    private boolean mClosing;
    private boolean mCollapseAfterPeek;
    private boolean mCollapsedAndHeadsUpOnDown;
    private float mExpandedFraction;
    protected float mExpandedHeight;
    protected boolean mExpanding;
    private FalsingManager mFalsingManager;
    private FlingAnimationUtils mFlingAnimationUtils;
    private final Runnable mFlingCollapseRunnable;
    private boolean mGestureWaitForTouchSlop;
    private boolean mHasLayoutedSinceDown;
    protected HeadsUpManager mHeadsUpManager;
    private ValueAnimator mHeightAnimator;
    protected boolean mHintAnimationRunning;
    private float mHintDistance;
    private boolean mIgnoreXTouchSlop;
    private float mInitialOffsetOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private boolean mInstantExpanding;
    private boolean mJustPeeked;
    protected KeyguardBottomAreaView mKeyguardBottomArea;
    private boolean mMotionAborted;
    private float mNextCollapseSpeedUpFactor;
    private boolean mOverExpandedBeforeFling;
    private boolean mPanelClosedOnDown;
    private ObjectAnimator mPeekAnimator;
    private float mPeekHeight;
    private boolean mPeekPending;
    private Runnable mPeekRunnable;
    private boolean mPeekTouching;
    protected final Runnable mPostCollapseRunnable;
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
    private boolean mUpwardsWhenTresholdReached;
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

    protected abstract boolean isInContentBounds(float f, float f2);

    protected abstract boolean isPanelVisibleBecauseOfHeadsUp();

    protected abstract boolean isTrackingBlocked();

    protected abstract void onHeightUpdated(float f);

    protected abstract boolean onMiddleClicked();

    public abstract void resetViews();

    protected abstract void setOverExpansion(float f, boolean z);

    protected abstract boolean shouldGestureIgnoreXTouchSlop(float f, float f2);

    protected void onExpandingFinished() {
        this.mBar.onExpandingFinished();
    }

    protected void onExpandingStarted() {
    }

    public void notifyExpandingStarted() {
        if (this.mExpanding) {
            return;
        }
        this.mExpanding = true;
        onExpandingStarted();
    }

    protected final void notifyExpandingFinished() {
        endClosing();
        if (!this.mExpanding) {
            return;
        }
        this.mExpanding = false;
        onExpandingFinished();
    }

    private void schedulePeek() {
        this.mPeekPending = true;
        long timeout = ViewConfiguration.getTapTimeout();
        postOnAnimationDelayed(this.mPeekRunnable, timeout);
        notifyBarPanelExpansionChanged();
    }

    public void runPeekAnimation() {
        this.mPeekHeight = getPeekHeight();
        if (this.mHeightAnimator != null) {
            return;
        }
        this.mPeekAnimator = ObjectAnimator.ofFloat(this, "expandedHeight", this.mPeekHeight).setDuration(250L);
        this.mPeekAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
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
                    PanelView.this.postOnAnimation(PanelView.this.mPostCollapseRunnable);
                }
                PanelView.this.mCollapseAfterPeek = false;
            }
        });
        notifyExpandingStarted();
        this.mPeekAnimator.start();
        this.mJustPeeked = true;
    }

    public PanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mExpandedFraction = 0.0f;
        this.mExpandedHeight = 0.0f;
        this.mNextCollapseSpeedUpFactor = 1.0f;
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
                PanelView.this.fling(0.0f, false, PanelView.this.mNextCollapseSpeedUpFactor, false);
            }
        };
        this.mPostCollapseRunnable = new Runnable() {
            @Override
            public void run() {
                PanelView.this.collapse(false, 1.0f);
            }
        };
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.6f);
        this.mBounceInterpolator = new BounceInterpolator();
        this.mFalsingManager = FalsingManager.getInstance(context);
    }

    protected void loadDimens() {
        Resources res = getContext().getResources();
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mHintDistance = res.getDimension(R.dimen.hint_move_distance);
        this.mUnlockFalsingThreshold = res.getDimensionPixelSize(R.dimen.unlock_falsing_threshold);
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
        boolean z = false;
        if (this.mInstantExpanding || this.mTouchDisabled || (this.mMotionAborted && event.getActionMasked() != 0)) {
            return false;
        }
        if (isFullyCollapsed() && event.isFromSource(8194)) {
            if (event.getAction() == 1) {
                expand(true);
            }
            return true;
        }
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        if (event.getActionMasked() == 0) {
            this.mGestureWaitForTouchSlop = !isFullyCollapsed() ? hasConflictingGestures() : true;
            this.mIgnoreXTouchSlop = !isFullyCollapsed() ? shouldGestureIgnoreXTouchSlop(x, y) : true;
        }
        switch (event.getActionMasked()) {
            case 0:
                startExpandMotion(x, y, false, this.mExpandedHeight);
                this.mJustPeeked = false;
                this.mPanelClosedOnDown = isFullyCollapsed();
                this.mHasLayoutedSinceDown = false;
                this.mUpdateFlingOnLayout = false;
                this.mMotionAborted = false;
                this.mPeekTouching = this.mPanelClosedOnDown;
                this.mTouchAboveFalsingThreshold = false;
                this.mCollapsedAndHeadsUpOnDown = isFullyCollapsed() ? this.mHeadsUpManager.hasPinnedHeadsUp() : false;
                if (this.mVelocityTracker == null) {
                    initVelocityTracker();
                }
                trackMovement(event);
                if (!this.mGestureWaitForTouchSlop || ((this.mHeightAnimator != null && !this.mHintAnimationRunning) || this.mPeekPending || this.mPeekAnimator != null)) {
                    cancelHeightAnimator();
                    cancelPeek();
                    if ((this.mHeightAnimator != null && !this.mHintAnimationRunning) || this.mPeekPending || this.mPeekAnimator != null) {
                        z = true;
                    }
                    this.mTouchSlopExceeded = z;
                    onTrackingStarted();
                }
                if (isFullyCollapsed() && !this.mHeadsUpManager.hasPinnedHeadsUp()) {
                    schedulePeek();
                }
                break;
            case 1:
            case 3:
                trackMovement(event);
                endMotionEvent(event, x, y, false);
                break;
            case 2:
                float h = y - this.mInitialTouchY;
                if (Math.abs(h) > this.mTouchSlop && (Math.abs(h) > Math.abs(x - this.mInitialTouchX) || this.mIgnoreXTouchSlop)) {
                    this.mTouchSlopExceeded = true;
                    if (this.mGestureWaitForTouchSlop && !this.mTracking && !this.mCollapsedAndHeadsUpOnDown) {
                        if (!this.mJustPeeked && this.mInitialOffsetOnTouch != 0.0f) {
                            startExpandMotion(x, y, false, this.mExpandedHeight);
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
                    this.mUpwardsWhenTresholdReached = isDirectionUpwards(x, y);
                }
                if (!this.mJustPeeked && ((!this.mGestureWaitForTouchSlop || this.mTracking) && !isTrackingBlocked())) {
                    setExpandedHeightInternal(newHeight);
                }
                trackMovement(event);
                break;
            case 5:
                if (this.mStatusBar.getBarState() == 1) {
                    this.mMotionAborted = true;
                    endMotionEvent(event, x, y, true);
                    return false;
                }
                break;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    float newY = event.getY(newIndex);
                    float newX = event.getX(newIndex);
                    this.mTrackingPointer = event.getPointerId(newIndex);
                    startExpandMotion(newX, newY, true, this.mExpandedHeight);
                }
                break;
        }
        if (this.mGestureWaitForTouchSlop) {
            return this.mTracking;
        }
        return true;
    }

    private boolean isDirectionUpwards(float x, float y) {
        float xDiff = x - this.mInitialTouchX;
        float yDiff = y - this.mInitialTouchY;
        return yDiff < 0.0f && Math.abs(yDiff) >= Math.abs(xDiff);
    }

    protected void startExpandMotion(float newX, float newY, boolean startTracking, float expandedHeight) {
        this.mInitialOffsetOnTouch = expandedHeight;
        this.mInitialTouchY = newY;
        this.mInitialTouchX = newX;
        if (!startTracking) {
            return;
        }
        this.mTouchSlopExceeded = true;
        setExpandedHeight(this.mInitialOffsetOnTouch);
        onTrackingStarted();
    }

    private void endMotionEvent(MotionEvent event, float x, float y, boolean forceCancel) {
        this.mTrackingPointer = -1;
        if ((this.mTracking && this.mTouchSlopExceeded) || Math.abs(x - this.mInitialTouchX) > this.mTouchSlop || Math.abs(y - this.mInitialTouchY) > this.mTouchSlop || event.getActionMasked() == 3 || forceCancel) {
            float vel = 0.0f;
            float vectorVel = 0.0f;
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.computeCurrentVelocity(1000);
                vel = this.mVelocityTracker.getYVelocity();
                vectorVel = (float) Math.hypot(this.mVelocityTracker.getXVelocity(), this.mVelocityTracker.getYVelocity());
            }
            boolean z = ((!flingExpands(vel, vectorVel, x, y) || this.mStatusBar.getBarState() == 1) && event.getActionMasked() != 3) ? forceCancel : true;
            DozeLog.traceFling(z, this.mTouchAboveFalsingThreshold, this.mStatusBar.isFalsingThresholdNeeded(), this.mStatusBar.isWakeUpComingFromTouch());
            if (!z && this.mStatusBar.getBarState() == 1) {
                float displayDensity = this.mStatusBar.getDisplayDensity();
                int heightDp = (int) Math.abs((y - this.mInitialTouchY) / displayDensity);
                int velocityDp = (int) Math.abs(vel / displayDensity);
                EventLogTags.writeSysuiLockscreenGesture(1, heightDp, velocityDp);
            }
            fling(vel, z, isFalseTouch(x, y));
            onTrackingStopped(z);
            this.mUpdateFlingOnLayout = z && this.mPanelClosedOnDown && !this.mHasLayoutedSinceDown;
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
    }

    private int getFalsingThreshold() {
        float factor = this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
        return (int) (this.mUnlockFalsingThreshold * factor);
    }

    protected void onTrackingStopped(boolean expand) {
        this.mTracking = false;
        this.mBar.onTrackingStopped(expand);
        notifyBarPanelExpansionChanged();
    }

    protected void onTrackingStarted() {
        endClosing();
        this.mTracking = true;
        this.mCollapseAfterPeek = false;
        this.mBar.onTrackingStarted();
        notifyExpandingStarted();
        notifyBarPanelExpansionChanged();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mInstantExpanding || (this.mMotionAborted && event.getActionMasked() != 0)) {
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
                this.mAnimatingOnDown = this.mHeightAnimator != null;
                if ((this.mAnimatingOnDown && this.mClosing && !this.mHintAnimationRunning) || this.mPeekPending || this.mPeekAnimator != null) {
                    cancelHeightAnimator();
                    cancelPeek();
                    this.mTouchSlopExceeded = true;
                    return true;
                }
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                this.mTouchStartedInEmptyArea = isInContentBounds(x, y) ? false : true;
                this.mTouchSlopExceeded = false;
                this.mJustPeeked = false;
                this.mMotionAborted = false;
                this.mPanelClosedOnDown = isFullyCollapsed();
                this.mCollapsedAndHeadsUpOnDown = false;
                this.mHasLayoutedSinceDown = false;
                this.mUpdateFlingOnLayout = false;
                this.mTouchAboveFalsingThreshold = false;
                initVelocityTracker();
                trackMovement(event);
                return false;
            case 1:
            case 3:
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                }
                return false;
            case 2:
                float h = y - this.mInitialTouchY;
                trackMovement(event);
                if (scrolledToBottom || this.mTouchStartedInEmptyArea || this.mAnimatingOnDown) {
                    float hAbs = Math.abs(h);
                    if ((h < (-this.mTouchSlop) || (this.mAnimatingOnDown && hAbs > this.mTouchSlop)) && hAbs > Math.abs(x - this.mInitialTouchX)) {
                        cancelHeightAnimator();
                        startExpandMotion(x, y, true, this.mExpandedHeight);
                        return true;
                    }
                }
                return false;
            case 4:
            default:
                return false;
            case 5:
                if (this.mStatusBar.getBarState() == 1) {
                    this.mMotionAborted = true;
                    if (this.mVelocityTracker != null) {
                        this.mVelocityTracker.recycle();
                        this.mVelocityTracker = null;
                    }
                }
                return false;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    this.mTrackingPointer = event.getPointerId(newIndex);
                    this.mInitialTouchX = event.getX(newIndex);
                    this.mInitialTouchY = event.getY(newIndex);
                }
                return false;
        }
    }

    protected void cancelHeightAnimator() {
        if (this.mHeightAnimator != null) {
            this.mHeightAnimator.cancel();
        }
        endClosing();
    }

    private void endClosing() {
        if (!this.mClosing) {
            return;
        }
        this.mClosing = false;
        onClosingFinished();
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

    protected boolean flingExpands(float vel, float vectorVel, float x, float y) {
        if (isFalseTouch(x, y)) {
            return true;
        }
        return Math.abs(vectorVel) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond() ? getExpandedFraction() > 0.5f : vel > 0.0f;
    }

    private boolean isFalseTouch(float x, float y) {
        if (!this.mStatusBar.isFalsingThresholdNeeded()) {
            return false;
        }
        if (this.mFalsingManager.isClassiferEnabled()) {
            return this.mFalsingManager.isFalseTouch();
        }
        if (this.mTouchAboveFalsingThreshold) {
            return (this.mUpwardsWhenTresholdReached || isDirectionUpwards(x, y)) ? false : true;
        }
        return true;
    }

    protected void fling(float vel, boolean expand) {
        fling(vel, expand, 1.0f, false);
    }

    protected void fling(float vel, boolean expand, boolean expandBecauseOfFalsing) {
        fling(vel, expand, 1.0f, expandBecauseOfFalsing);
    }

    protected void fling(float vel, boolean expand, float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        cancelPeek();
        float target = expand ? getMaxPanelHeight() : 0.0f;
        if (!expand) {
            this.mClosing = true;
        }
        flingToHeight(vel, expand, target, collapseSpeedUpFactor, expandBecauseOfFalsing);
    }

    protected void flingToHeight(float vel, boolean expand, float target, float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        final boolean clearAllExpandHack;
        if (expand && fullyExpandedClearAllVisible() && this.mExpandedHeight < getMaxPanelHeight() - getClearAllHeight()) {
            clearAllExpandHack = !isClearAllVisible();
        } else {
            clearAllExpandHack = false;
        }
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
            if (expandBecauseOfFalsing) {
                vel = 0.0f;
            }
            this.mFlingAnimationUtils.apply(animator, this.mExpandedHeight, target, vel, getHeight());
            if (expandBecauseOfFalsing) {
                animator.setDuration(350L);
            }
        } else {
            this.mFlingAnimationUtils.applyDismissing(animator, this.mExpandedHeight, target, vel, getHeight());
            animator.setDuration(48L);
            if (vel == 0.0f) {
                animator.setDuration((long) ((animator.getDuration() * getCannedFlingDurationFactor()) / collapseSpeedUpFactor));
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
                PanelView.this.notifyBarPanelExpansionChanged();
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
        this.mStatusBar.onPanelLaidOut();
        requestPanelHeightUpdate();
        this.mHasLayoutedSinceDown = true;
        if (!this.mUpdateFlingOnLayout) {
            return;
        }
        abortAnimations();
        fling(this.mUpdateFlingVelocity, true);
        this.mUpdateFlingOnLayout = false;
    }

    protected void requestPanelHeightUpdate() {
        float currentMaxPanelHeight = getMaxPanelHeight();
        if ((this.mTracking && !isTrackingBlocked()) || this.mHeightAnimator != null || isFullyCollapsed() || currentMaxPanelHeight == this.mExpandedHeight || this.mPeekPending || this.mPeekAnimator != null || this.mPeekTouching) {
            return;
        }
        setExpandedHeight(currentMaxPanelHeight);
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

    public void collapse(boolean delayed, float speedUpFactor) {
        if (this.mPeekPending || this.mPeekAnimator != null) {
            this.mCollapseAfterPeek = true;
            if (!this.mPeekPending) {
                return;
            }
            removeCallbacks(this.mPeekRunnable);
            this.mPeekRunnable.run();
            return;
        }
        if (isFullyCollapsed() || this.mTracking || this.mClosing) {
            return;
        }
        cancelHeightAnimator();
        notifyExpandingStarted();
        this.mClosing = true;
        if (delayed) {
            this.mNextCollapseSpeedUpFactor = speedUpFactor;
            postDelayed(this.mFlingCollapseRunnable, 120L);
        } else {
            fling(0.0f, false, speedUpFactor, false);
        }
    }

    public void cancelPeek() {
        boolean cancelled = this.mPeekPending;
        if (this.mPeekAnimator != null) {
            cancelled = true;
            this.mPeekAnimator.cancel();
        }
        removeCallbacks(this.mPeekRunnable);
        this.mPeekPending = false;
        if (!cancelled) {
            return;
        }
        notifyBarPanelExpansionChanged();
    }

    public void expand(final boolean animate) {
        if (!isFullyCollapsed() && !isCollapsing()) {
            return;
        }
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
        notifyBarPanelExpansionChanged();
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!PanelView.this.mInstantExpanding) {
                    PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    return;
                }
                if (PanelView.this.mStatusBar.getStatusBarWindow().getHeight() == PanelView.this.mStatusBar.getStatusBarHeight()) {
                    return;
                }
                PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (animate) {
                    PanelView.this.notifyExpandingStarted();
                    PanelView.this.fling(0.0f, true);
                } else {
                    PanelView.this.setExpandedFraction(1.0f);
                }
                PanelView.this.mInstantExpanding = false;
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
        if (!this.mInstantExpanding) {
            return;
        }
        this.mInstantExpanding = false;
        notifyBarPanelExpansionChanged();
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
        if (this.mHeightAnimator != null || this.mTracking) {
            return;
        }
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

    private void startUnlockHintAnimationPhase1(final Runnable onAnimationFinished) {
        float target = Math.max(0.0f, getMaxPanelHeight() - this.mHintDistance);
        ValueAnimator animator = createHeightAnimator(target);
        animator.setDuration(250L);
        animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
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
        this.mKeyguardBottomArea.getIndicationView().animate().translationY(-this.mHintDistance).setDuration(250L).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).withEndAction(new Runnable() {
            @Override
            public void run() {
                PanelView.this.mKeyguardBottomArea.getIndicationView().animate().translationY(0.0f).setDuration(450L).setInterpolator(PanelView.this.mBounceInterpolator).start();
            }
        }).start();
    }

    public void startUnlockHintAnimationPhase2(final Runnable onAnimationFinished) {
        ValueAnimator animator = createHeightAnimator(getMaxPanelHeight());
        animator.setDuration(450L);
        animator.setInterpolator(this.mBounceInterpolator);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                PanelView.this.mHeightAnimator = null;
                onAnimationFinished.run();
                PanelView.this.notifyBarPanelExpansionChanged();
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

    protected void notifyBarPanelExpansionChanged() {
        boolean z = true;
        PanelBar panelBar = this.mBar;
        float f = this.mExpandedFraction;
        if (this.mExpandedFraction <= 0.0f && !this.mPeekPending && this.mPeekAnimator == null && !this.mInstantExpanding && !isPanelVisibleBecauseOfHeadsUp() && !this.mTracking && this.mHeightAnimator == null) {
            z = false;
        }
        panelBar.panelExpansionChanged(f, z);
    }

    protected boolean onEmptySpaceClick(float x) {
        if (this.mHintAnimationRunning) {
            return true;
        }
        return onMiddleClicked();
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

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }
}
