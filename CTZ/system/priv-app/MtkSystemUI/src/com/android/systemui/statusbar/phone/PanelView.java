package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import com.android.internal.util.LatencyTracker;
import com.android.systemui.DejankUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.VibratorHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.BiConsumer;

/* loaded from: classes.dex */
public abstract class PanelView extends FrameLayout {
    public static final String TAG = PanelView.class.getSimpleName();
    private boolean mAnimateAfterExpanding;
    private boolean mAnimatingOnDown;
    PanelBar mBar;
    private Interpolator mBounceInterpolator;
    private boolean mClosing;
    private boolean mCollapsedAndHeadsUpOnDown;
    private long mDownTime;
    private boolean mExpandLatencyTracking;
    private float mExpandedFraction;
    protected float mExpandedHeight;
    protected boolean mExpanding;
    private BiConsumer<Float, Boolean> mExpansionListener;
    private FalsingManager mFalsingManager;
    private int mFixedDuration;
    private FlingAnimationUtils mFlingAnimationUtils;
    private FlingAnimationUtils mFlingAnimationUtilsClosing;
    private FlingAnimationUtils mFlingAnimationUtilsDismissing;
    private final Runnable mFlingCollapseRunnable;
    private boolean mGestureWaitForTouchSlop;
    private boolean mHasLayoutedSinceDown;
    protected HeadsUpManagerPhone mHeadsUpManager;
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
    protected boolean mLaunchingNotification;
    private LockscreenGestureLogger mLockscreenGestureLogger;
    private float mMinExpandHeight;
    private boolean mMotionAborted;
    private float mNextCollapseSpeedUpFactor;
    private boolean mNotificationsDragEnabled;
    private boolean mOverExpandedBeforeFling;
    private boolean mPanelClosedOnDown;
    private boolean mPanelUpdateWhenAnimatorEnds;
    private ObjectAnimator mPeekAnimator;
    private float mPeekHeight;
    private boolean mPeekTouching;
    protected final Runnable mPostCollapseRunnable;
    protected StatusBar mStatusBar;
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
    private boolean mVibrateOnOpening;
    private final VibratorHelper mVibratorHelper;
    private String mViewName;

    protected abstract boolean fullyExpandedClearAllVisible();

    protected abstract int getClearAllHeight();

    protected abstract int getMaxPanelHeight();

    protected abstract float getOpeningHeight();

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

    protected abstract boolean shouldUseDismissingAnimation();

    protected void onExpandingFinished() {
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

    protected final void notifyExpandingFinished() {
        endClosing();
        if (this.mExpanding) {
            this.mExpanding = false;
            onExpandingFinished();
        }
    }

    private void runPeekAnimation(long j, float f, boolean z) {
        this.mPeekHeight = f;
        if (this.mHeightAnimator != null) {
            return;
        }
        if (this.mPeekAnimator != null) {
            this.mPeekAnimator.cancel();
        }
        this.mPeekAnimator = ObjectAnimator.ofFloat(this, "expandedHeight", this.mPeekHeight).setDuration(j);
        this.mPeekAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        this.mPeekAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.PanelView.1
            private boolean mCancelled;
            final /* synthetic */ boolean val$collapseWhenFinished;

            AnonymousClass1(boolean z2) {
                z = z2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                PanelView.this.mPeekAnimator = null;
                if (!this.mCancelled && z) {
                    PanelView.this.postOnAnimation(PanelView.this.mPostCollapseRunnable);
                }
            }
        });
        notifyExpandingStarted();
        this.mPeekAnimator.start();
        this.mJustPeeked = true;
    }

    /* renamed from: com.android.systemui.statusbar.phone.PanelView$1 */
    class AnonymousClass1 extends AnimatorListenerAdapter {
        private boolean mCancelled;
        final /* synthetic */ boolean val$collapseWhenFinished;

        AnonymousClass1(boolean z2) {
            z = z2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            this.mCancelled = true;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            PanelView.this.mPeekAnimator = null;
            if (!this.mCancelled && z) {
                PanelView.this.postOnAnimation(PanelView.this.mPostCollapseRunnable);
            }
        }
    }

    public PanelView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLockscreenGestureLogger = new LockscreenGestureLogger();
        this.mFixedDuration = -1;
        this.mExpandedFraction = 0.0f;
        this.mExpandedHeight = 0.0f;
        this.mNextCollapseSpeedUpFactor = 1.0f;
        this.mFlingCollapseRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.PanelView.3
            AnonymousClass3() {
            }

            @Override // java.lang.Runnable
            public void run() {
                PanelView.this.fling(0.0f, false, PanelView.this.mNextCollapseSpeedUpFactor, false);
            }
        };
        this.mPostCollapseRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.PanelView.8
            AnonymousClass8() {
            }

            @Override // java.lang.Runnable
            public void run() {
                PanelView.this.collapse(false, 1.0f);
            }
        };
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.6f, 0.6f);
        this.mFlingAnimationUtilsClosing = new FlingAnimationUtils(context, 0.5f, 0.6f);
        this.mFlingAnimationUtilsDismissing = new FlingAnimationUtils(context, 0.5f, 0.2f, 0.6f, 0.84f);
        this.mBounceInterpolator = new BounceInterpolator();
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mNotificationsDragEnabled = getResources().getBoolean(R.bool.config_enableNotificationShadeDrag);
        this.mVibratorHelper = (VibratorHelper) Dependency.get(VibratorHelper.class);
        this.mVibrateOnOpening = this.mContext.getResources().getBoolean(R.bool.config_vibrateOnIconAnimation);
    }

    protected void loadDimens() {
        Resources resources = getContext().getResources();
        this.mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.mHintDistance = resources.getDimension(R.dimen.hint_move_distance);
        this.mUnlockFalsingThreshold = resources.getDimensionPixelSize(R.dimen.unlock_falsing_threshold);
    }

    private void trackMovement(MotionEvent motionEvent) {
        float rawX = motionEvent.getRawX() - motionEvent.getX();
        float rawY = motionEvent.getRawY() - motionEvent.getY();
        motionEvent.offsetLocation(rawX, rawY);
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(motionEvent);
        }
        motionEvent.offsetLocation(-rawX, -rawY);
    }

    public void setTouchDisabled(boolean z) {
        this.mTouchDisabled = z;
        if (this.mTouchDisabled) {
            cancelHeightAnimator();
            if (this.mTracking) {
                onTrackingStopped(true);
            }
            notifyExpandingFinished();
        }
    }

    public void startExpandLatencyTracking() {
        if (LatencyTracker.isEnabled(this.mContext)) {
            LatencyTracker.getInstance(this.mContext).onActionStart(0);
            this.mExpandLatencyTracking = true;
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mInstantExpanding || ((this.mTouchDisabled && motionEvent.getActionMasked() != 3) || (this.mMotionAborted && motionEvent.getActionMasked() != 0))) {
            return false;
        }
        if (!this.mNotificationsDragEnabled) {
            if (this.mTracking) {
                onTrackingStopped(true);
            }
            return false;
        }
        if (isFullyCollapsed() && motionEvent.isFromSource(8194)) {
            if (motionEvent.getAction() == 1) {
                expand(true);
            }
            return true;
        }
        int iFindPointerIndex = motionEvent.findPointerIndex(this.mTrackingPointer);
        if (iFindPointerIndex < 0) {
            this.mTrackingPointer = motionEvent.getPointerId(0);
            iFindPointerIndex = 0;
        }
        float x = motionEvent.getX(iFindPointerIndex);
        float y = motionEvent.getY(iFindPointerIndex);
        if (motionEvent.getActionMasked() == 0) {
            this.mGestureWaitForTouchSlop = isFullyCollapsed() || hasConflictingGestures();
            this.mIgnoreXTouchSlop = isFullyCollapsed() || shouldGestureIgnoreXTouchSlop(x, y);
        }
        switch (motionEvent.getActionMasked()) {
            case 0:
                startExpandMotion(x, y, false, this.mExpandedHeight);
                this.mJustPeeked = false;
                this.mMinExpandHeight = 0.0f;
                this.mPanelClosedOnDown = isFullyCollapsed();
                this.mHasLayoutedSinceDown = false;
                this.mUpdateFlingOnLayout = false;
                this.mMotionAborted = false;
                this.mPeekTouching = this.mPanelClosedOnDown;
                this.mDownTime = SystemClock.uptimeMillis();
                this.mTouchAboveFalsingThreshold = false;
                this.mCollapsedAndHeadsUpOnDown = isFullyCollapsed() && this.mHeadsUpManager.hasPinnedHeadsUp();
                if (this.mVelocityTracker == null) {
                    initVelocityTracker();
                }
                trackMovement(motionEvent);
                if (!this.mGestureWaitForTouchSlop || ((this.mHeightAnimator != null && !this.mHintAnimationRunning) || this.mPeekAnimator != null)) {
                    this.mTouchSlopExceeded = ((this.mHeightAnimator == null || this.mHintAnimationRunning) && this.mPeekAnimator == null) ? false : true;
                    cancelHeightAnimator();
                    cancelPeek();
                    onTrackingStarted();
                }
                if (isFullyCollapsed() && !this.mHeadsUpManager.hasPinnedHeadsUp() && !this.mStatusBar.isBouncerShowing()) {
                    startOpening(motionEvent);
                    break;
                }
                break;
            case 1:
            case 3:
                trackMovement(motionEvent);
                endMotionEvent(motionEvent, x, y, false);
                break;
            case 2:
                trackMovement(motionEvent);
                float f = y - this.mInitialTouchY;
                if (Math.abs(f) > this.mTouchSlop && (Math.abs(f) > Math.abs(x - this.mInitialTouchX) || this.mIgnoreXTouchSlop)) {
                    this.mTouchSlopExceeded = true;
                    if (this.mGestureWaitForTouchSlop && !this.mTracking && !this.mCollapsedAndHeadsUpOnDown) {
                        if (!this.mJustPeeked && this.mInitialOffsetOnTouch != 0.0f) {
                            startExpandMotion(x, y, false, this.mExpandedHeight);
                            f = 0.0f;
                        }
                        cancelHeightAnimator();
                        onTrackingStarted();
                    }
                }
                float fMax = Math.max(0.0f, this.mInitialOffsetOnTouch + f);
                if (fMax > this.mPeekHeight) {
                    if (this.mPeekAnimator != null) {
                        this.mPeekAnimator.cancel();
                    }
                    this.mJustPeeked = false;
                } else if (this.mPeekAnimator == null && this.mJustPeeked) {
                    this.mInitialOffsetOnTouch = this.mExpandedHeight;
                    this.mInitialTouchY = y;
                    this.mMinExpandHeight = this.mExpandedHeight;
                    this.mJustPeeked = false;
                }
                float fMax2 = Math.max(fMax, this.mMinExpandHeight);
                if ((-f) >= getFalsingThreshold()) {
                    this.mTouchAboveFalsingThreshold = true;
                    this.mUpwardsWhenTresholdReached = isDirectionUpwards(x, y);
                }
                if (!this.mJustPeeked && ((!this.mGestureWaitForTouchSlop || this.mTracking) && !isTrackingBlocked())) {
                    setExpandedHeightInternal(fMax2);
                    break;
                }
                break;
            case 5:
                if (this.mStatusBar.getBarState() == 1) {
                    this.mMotionAborted = true;
                    endMotionEvent(motionEvent, x, y, true);
                    return false;
                }
                break;
            case 6:
                int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
                if (this.mTrackingPointer == pointerId) {
                    int i = motionEvent.getPointerId(0) != pointerId ? 0 : 1;
                    float y2 = motionEvent.getY(i);
                    float x2 = motionEvent.getX(i);
                    this.mTrackingPointer = motionEvent.getPointerId(i);
                    startExpandMotion(x2, y2, true, this.mExpandedHeight);
                    break;
                }
                break;
        }
        return !this.mGestureWaitForTouchSlop || this.mTracking;
    }

    private void startOpening(MotionEvent motionEvent) {
        runPeekAnimation(200L, getOpeningHeight(), false);
        notifyBarPanelExpansionChanged();
        if (this.mVibrateOnOpening) {
            this.mVibratorHelper.vibrate(2);
        }
        float displayWidth = this.mStatusBar.getDisplayWidth();
        float displayHeight = this.mStatusBar.getDisplayHeight();
        this.mLockscreenGestureLogger.writeAtFractionalPosition(1328, (int) ((motionEvent.getX() / displayWidth) * 100.0f), (int) ((motionEvent.getY() / displayHeight) * 100.0f), this.mStatusBar.getRotation());
    }

    private boolean isDirectionUpwards(float f, float f2) {
        float f3 = f - this.mInitialTouchX;
        float f4 = f2 - this.mInitialTouchY;
        return f4 < 0.0f && Math.abs(f4) >= Math.abs(f3);
    }

    protected void startExpandingFromPeek() {
        this.mStatusBar.handlePeekToExpandTransistion();
    }

    protected void startExpandMotion(float f, float f2, boolean z, float f3) {
        this.mInitialOffsetOnTouch = f3;
        this.mInitialTouchY = f2;
        this.mInitialTouchX = f;
        if (z) {
            this.mTouchSlopExceeded = true;
            setExpandedHeight(this.mInitialOffsetOnTouch);
            onTrackingStarted();
        }
    }

    private void endMotionEvent(MotionEvent motionEvent, float f, float f2, boolean z) {
        float fHypot;
        this.mTrackingPointer = -1;
        if ((this.mTracking && this.mTouchSlopExceeded) || Math.abs(f - this.mInitialTouchX) > this.mTouchSlop || Math.abs(f2 - this.mInitialTouchY) > this.mTouchSlop || motionEvent.getActionMasked() == 3 || z) {
            float yVelocity = 0.0f;
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.computeCurrentVelocity(1000);
                yVelocity = this.mVelocityTracker.getYVelocity();
                fHypot = (float) Math.hypot(this.mVelocityTracker.getXVelocity(), this.mVelocityTracker.getYVelocity());
            } else {
                fHypot = 0.0f;
            }
            boolean z2 = flingExpands(yVelocity, fHypot, f, f2) || motionEvent.getActionMasked() == 3 || z;
            DozeLog.traceFling(z2, this.mTouchAboveFalsingThreshold, this.mStatusBar.isFalsingThresholdNeeded(), this.mStatusBar.isWakeUpComingFromTouch());
            if (!z2 && this.mStatusBar.getBarState() == 1) {
                float displayDensity = this.mStatusBar.getDisplayDensity();
                this.mLockscreenGestureLogger.write(186, (int) Math.abs((f2 - this.mInitialTouchY) / displayDensity), (int) Math.abs(yVelocity / displayDensity));
            }
            fling(yVelocity, z2, isFalseTouch(f, f2));
            onTrackingStopped(z2);
            this.mUpdateFlingOnLayout = z2 && this.mPanelClosedOnDown && !this.mHasLayoutedSinceDown;
            if (this.mUpdateFlingOnLayout) {
                this.mUpdateFlingVelocity = yVelocity;
            }
        } else if (!this.mPanelClosedOnDown || this.mHeadsUpManager.hasPinnedHeadsUp() || this.mTracking || this.mStatusBar.isBouncerShowing() || this.mStatusBar.isKeyguardFadingAway()) {
            if (!this.mStatusBar.isBouncerShowing()) {
                onTrackingStopped(onEmptySpaceClick(this.mInitialTouchX));
            }
        } else if (SystemClock.uptimeMillis() - this.mDownTime < ViewConfiguration.getLongPressTimeout()) {
            runPeekAnimation(360L, getPeekHeight(), true);
        } else {
            postOnAnimation(this.mPostCollapseRunnable);
        }
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
        this.mPeekTouching = false;
    }

    protected float getCurrentExpandVelocity() {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(1000);
        return this.mVelocityTracker.getYVelocity();
    }

    private int getFalsingThreshold() {
        return (int) (this.mUnlockFalsingThreshold * (this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f));
    }

    protected void onTrackingStopped(boolean z) {
        this.mTracking = false;
        this.mBar.onTrackingStopped(z);
        notifyBarPanelExpansionChanged();
    }

    protected void onTrackingStarted() {
        endClosing();
        this.mTracking = true;
        this.mBar.onTrackingStarted();
        notifyExpandingStarted();
        notifyBarPanelExpansionChanged();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mInstantExpanding || !this.mNotificationsDragEnabled || this.mTouchDisabled || (this.mMotionAborted && motionEvent.getActionMasked() != 0)) {
            return false;
        }
        int iFindPointerIndex = motionEvent.findPointerIndex(this.mTrackingPointer);
        if (iFindPointerIndex < 0) {
            this.mTrackingPointer = motionEvent.getPointerId(0);
            iFindPointerIndex = 0;
        }
        float x = motionEvent.getX(iFindPointerIndex);
        float y = motionEvent.getY(iFindPointerIndex);
        boolean zIsScrolledToBottom = isScrolledToBottom();
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.mStatusBar.userActivity();
                this.mAnimatingOnDown = this.mHeightAnimator != null;
                this.mMinExpandHeight = 0.0f;
                this.mDownTime = SystemClock.uptimeMillis();
                if ((this.mAnimatingOnDown && this.mClosing && !this.mHintAnimationRunning) || this.mPeekAnimator != null) {
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
                this.mMotionAborted = false;
                this.mPanelClosedOnDown = isFullyCollapsed();
                this.mCollapsedAndHeadsUpOnDown = false;
                this.mHasLayoutedSinceDown = false;
                this.mUpdateFlingOnLayout = false;
                this.mTouchAboveFalsingThreshold = false;
                initVelocityTracker();
                trackMovement(motionEvent);
                return false;
            case 1:
            case 3:
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                }
                return false;
            case 2:
                float f = y - this.mInitialTouchY;
                trackMovement(motionEvent);
                if (zIsScrolledToBottom || this.mTouchStartedInEmptyArea || this.mAnimatingOnDown) {
                    float fAbs = Math.abs(f);
                    if ((f < (-this.mTouchSlop) || (this.mAnimatingOnDown && fAbs > this.mTouchSlop)) && fAbs > Math.abs(x - this.mInitialTouchX)) {
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
                int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
                if (this.mTrackingPointer == pointerId) {
                    int i = motionEvent.getPointerId(0) != pointerId ? 0 : 1;
                    this.mTrackingPointer = motionEvent.getPointerId(i);
                    this.mInitialTouchX = motionEvent.getX(i);
                    this.mInitialTouchY = motionEvent.getY(i);
                }
                return false;
        }
    }

    protected void cancelHeightAnimator() {
        if (this.mHeightAnimator != null) {
            if (this.mHeightAnimator.isRunning()) {
                this.mPanelUpdateWhenAnimatorEnds = false;
            }
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

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        loadDimens();
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        loadDimens();
    }

    protected boolean flingExpands(float f, float f2, float f3, float f4) {
        if (isFalseTouch(f3, f4)) {
            return true;
        }
        return Math.abs(f2) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond() ? getExpandedFraction() > 0.5f : f > 0.0f;
    }

    private boolean isFalseTouch(float f, float f2) {
        if (!this.mStatusBar.isFalsingThresholdNeeded()) {
            return false;
        }
        if (this.mFalsingManager.isClassiferEnabled()) {
            return this.mFalsingManager.isFalseTouch();
        }
        if (!this.mTouchAboveFalsingThreshold) {
            return true;
        }
        if (this.mUpwardsWhenTresholdReached) {
            return false;
        }
        return !isDirectionUpwards(f, f2);
    }

    protected void fling(float f, boolean z) {
        fling(f, z, 1.0f, false);
    }

    protected void fling(float f, boolean z, boolean z2) {
        fling(f, z, 1.0f, z2);
    }

    protected void fling(float f, boolean z, float f2, boolean z2) {
        cancelPeek();
        float maxPanelHeight = z ? getMaxPanelHeight() : 0.0f;
        if (!z) {
            this.mClosing = true;
        }
        flingToHeight(f, z, maxPanelHeight, f2, z2);
    }

    protected void flingToHeight(float f, boolean z, float f2, float f3, boolean z2) {
        boolean z3 = z && fullyExpandedClearAllVisible() && this.mExpandedHeight < ((float) (getMaxPanelHeight() - getClearAllHeight())) && !isClearAllVisible();
        if (z3) {
            f2 = getMaxPanelHeight() - getClearAllHeight();
        }
        float f4 = f2;
        if (f4 == this.mExpandedHeight || (getOverExpansionAmount() > 0.0f && z)) {
            notifyExpandingFinished();
            return;
        }
        this.mOverExpandedBeforeFling = getOverExpansionAmount() > 0.0f;
        ValueAnimator valueAnimatorCreateHeightAnimator = createHeightAnimator(f4);
        if (z) {
            if (z2 && f < 0.0f) {
                f = 0.0f;
            }
            this.mFlingAnimationUtils.apply(valueAnimatorCreateHeightAnimator, this.mExpandedHeight, f4, f, getHeight());
            if (f == 0.0f) {
                valueAnimatorCreateHeightAnimator.setDuration(350L);
            }
        } else {
            if (!shouldUseDismissingAnimation()) {
                this.mFlingAnimationUtilsClosing.apply(valueAnimatorCreateHeightAnimator, this.mExpandedHeight, f4, f, getHeight());
                valueAnimatorCreateHeightAnimator.setDuration(48L);
            } else if (f == 0.0f) {
                valueAnimatorCreateHeightAnimator.setInterpolator(Interpolators.PANEL_CLOSE_ACCELERATED);
                valueAnimatorCreateHeightAnimator.setDuration((long) (200.0f + ((this.mExpandedHeight / getHeight()) * 100.0f)));
            } else {
                this.mFlingAnimationUtilsDismissing.apply(valueAnimatorCreateHeightAnimator, this.mExpandedHeight, f4, f, getHeight());
                valueAnimatorCreateHeightAnimator.setDuration(48L);
            }
            if (f == 0.0f) {
                valueAnimatorCreateHeightAnimator.setDuration((long) (valueAnimatorCreateHeightAnimator.getDuration() / f3));
            }
            if (this.mFixedDuration != -1) {
                valueAnimatorCreateHeightAnimator.setDuration(this.mFixedDuration);
            }
        }
        valueAnimatorCreateHeightAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.PanelView.2
            private boolean mCancelled;
            final /* synthetic */ boolean val$clearAllExpandHack;

            AnonymousClass2(boolean z32) {
                z = z32;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (z && !this.mCancelled) {
                    PanelView.this.setExpandedHeightInternal(PanelView.this.getMaxPanelHeight());
                }
                PanelView.this.setAnimator(null);
                if (!this.mCancelled) {
                    PanelView.this.notifyExpandingFinished();
                }
                PanelView.this.notifyBarPanelExpansionChanged();
            }
        });
        setAnimator(valueAnimatorCreateHeightAnimator);
        valueAnimatorCreateHeightAnimator.start();
    }

    /* renamed from: com.android.systemui.statusbar.phone.PanelView$2 */
    class AnonymousClass2 extends AnimatorListenerAdapter {
        private boolean mCancelled;
        final /* synthetic */ boolean val$clearAllExpandHack;

        AnonymousClass2(boolean z32) {
            z = z32;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            this.mCancelled = true;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (z && !this.mCancelled) {
                PanelView.this.setExpandedHeightInternal(PanelView.this.getMaxPanelHeight());
            }
            PanelView.this.setAnimator(null);
            if (!this.mCancelled) {
                PanelView.this.notifyExpandingFinished();
            }
            PanelView.this.notifyBarPanelExpansionChanged();
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mViewName = getResources().getResourceName(getId());
    }

    public void setExpandedHeight(float f) {
        setExpandedHeightInternal(f + getOverExpansionPixels());
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mStatusBar.onPanelLaidOut();
        requestPanelHeightUpdate();
        this.mHasLayoutedSinceDown = true;
        if (this.mUpdateFlingOnLayout) {
            abortAnimations();
            fling(this.mUpdateFlingVelocity, true);
            this.mUpdateFlingOnLayout = false;
        }
    }

    protected void requestPanelHeightUpdate() {
        float maxPanelHeight = getMaxPanelHeight();
        if (isFullyCollapsed() || maxPanelHeight == this.mExpandedHeight || this.mPeekAnimator != null || this.mPeekTouching) {
            return;
        }
        if (this.mTracking && !isTrackingBlocked()) {
            return;
        }
        if (this.mHeightAnimator != null) {
            this.mPanelUpdateWhenAnimatorEnds = true;
        } else {
            setExpandedHeight(maxPanelHeight);
        }
    }

    public void setExpandedHeightInternal(float f) {
        if (this.mExpandLatencyTracking && f != 0.0f) {
            DejankUtils.postAfterTraversal(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$PanelView$NcQsgCLFImw9_GKeELFJ1HpKiII
                @Override // java.lang.Runnable
                public final void run() {
                    LatencyTracker.getInstance(this.f$0.mContext).onActionEnd(0);
                }
            });
            this.mExpandLatencyTracking = false;
        }
        float maxPanelHeight = getMaxPanelHeight() - getOverExpansionAmount();
        if (this.mHeightAnimator == null) {
            float fMax = Math.max(0.0f, f - maxPanelHeight);
            if (getOverExpansionPixels() != fMax && this.mTracking) {
                setOverExpansion(fMax, true);
            }
            this.mExpandedHeight = Math.min(f, maxPanelHeight) + getOverExpansionAmount();
        } else {
            this.mExpandedHeight = f;
            if (this.mOverExpandedBeforeFling) {
                setOverExpansion(Math.max(0.0f, f - maxPanelHeight), false);
            }
        }
        if (this.mExpandedHeight < 1.0f && this.mExpandedHeight != 0.0f && this.mClosing) {
            this.mExpandedHeight = 0.0f;
            if (this.mHeightAnimator != null) {
                this.mHeightAnimator.end();
            }
        }
        this.mExpandedFraction = Math.min(1.0f, maxPanelHeight != 0.0f ? this.mExpandedHeight / maxPanelHeight : 0.0f);
        onHeightUpdated(this.mExpandedHeight);
        notifyBarPanelExpansionChanged();
    }

    public void setExpandedFraction(float f) {
        setExpandedHeight(getMaxPanelHeight() * f);
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
        return this.mExpandedFraction <= 0.0f;
    }

    public boolean isCollapsing() {
        return this.mClosing || this.mLaunchingNotification;
    }

    public boolean isTracking() {
        return this.mTracking;
    }

    public void setBar(PanelBar panelBar) {
        this.mBar = panelBar;
    }

    public void collapse(boolean z, float f) {
        if (canPanelBeCollapsed()) {
            cancelHeightAnimator();
            notifyExpandingStarted();
            this.mClosing = true;
            if (z) {
                this.mNextCollapseSpeedUpFactor = f;
                postDelayed(this.mFlingCollapseRunnable, 120L);
            } else {
                fling(0.0f, false, f, false);
            }
        }
    }

    public boolean canPanelBeCollapsed() {
        return (isFullyCollapsed() || this.mTracking || this.mClosing) ? false : true;
    }

    /* renamed from: com.android.systemui.statusbar.phone.PanelView$3 */
    class AnonymousClass3 implements Runnable {
        AnonymousClass3() {
        }

        @Override // java.lang.Runnable
        public void run() {
            PanelView.this.fling(0.0f, false, PanelView.this.mNextCollapseSpeedUpFactor, false);
        }
    }

    public void cancelPeek() {
        boolean z;
        if (this.mPeekAnimator != null) {
            z = true;
            this.mPeekAnimator.cancel();
        } else {
            z = false;
        }
        if (z) {
            notifyBarPanelExpansionChanged();
        }
    }

    public void expand(boolean z) {
        if (!isFullyCollapsed() && !isCollapsing()) {
            return;
        }
        this.mInstantExpanding = true;
        this.mAnimateAfterExpanding = z;
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
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.systemui.statusbar.phone.PanelView.4
            AnonymousClass4() {
            }

            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                if (!PanelView.this.mInstantExpanding) {
                    PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    return;
                }
                if (PanelView.this.mStatusBar.getStatusBarWindow().getHeight() != PanelView.this.mStatusBar.getStatusBarHeight()) {
                    PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (PanelView.this.mAnimateAfterExpanding) {
                        PanelView.this.notifyExpandingStarted();
                        PanelView.this.fling(0.0f, true);
                    } else {
                        PanelView.this.setExpandedFraction(1.0f);
                    }
                    PanelView.this.mInstantExpanding = false;
                }
            }
        });
        requestLayout();
    }

    /* renamed from: com.android.systemui.statusbar.phone.PanelView$4 */
    class AnonymousClass4 implements ViewTreeObserver.OnGlobalLayoutListener {
        AnonymousClass4() {
        }

        @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
        public void onGlobalLayout() {
            if (!PanelView.this.mInstantExpanding) {
                PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                return;
            }
            if (PanelView.this.mStatusBar.getStatusBarWindow().getHeight() != PanelView.this.mStatusBar.getStatusBarHeight()) {
                PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (PanelView.this.mAnimateAfterExpanding) {
                    PanelView.this.notifyExpandingStarted();
                    PanelView.this.fling(0.0f, true);
                } else {
                    PanelView.this.setExpandedFraction(1.0f);
                }
                PanelView.this.mInstantExpanding = false;
            }
        }
    }

    public void instantCollapse() {
        abortAnimations();
        setExpandedFraction(0.0f);
        if (this.mExpanding) {
            notifyExpandingFinished();
        }
        if (this.mInstantExpanding) {
            this.mInstantExpanding = false;
            notifyBarPanelExpansionChanged();
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
        if (this.mHeightAnimator != null || this.mTracking) {
            return;
        }
        cancelPeek();
        notifyExpandingStarted();
        startUnlockHintAnimationPhase1(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$PanelView$tAljekoGx9mlKIleW6Fmi59MCOs
            @Override // java.lang.Runnable
            public final void run() {
                PanelView.lambda$startUnlockHintAnimation$1(this.f$0);
            }
        });
        onUnlockHintStarted();
        this.mHintAnimationRunning = true;
    }

    public static /* synthetic */ void lambda$startUnlockHintAnimation$1(PanelView panelView) {
        panelView.notifyExpandingFinished();
        panelView.onUnlockHintFinished();
        panelView.mHintAnimationRunning = false;
    }

    protected void onUnlockHintFinished() {
        this.mStatusBar.onHintFinished();
    }

    protected void onUnlockHintStarted() {
        this.mStatusBar.onUnlockHintStarted();
    }

    public boolean isUnlockHintRunning() {
        return this.mHintAnimationRunning;
    }

    private void startUnlockHintAnimationPhase1(Runnable runnable) {
        ValueAnimator valueAnimatorCreateHeightAnimator = createHeightAnimator(Math.max(0.0f, getMaxPanelHeight() - this.mHintDistance));
        valueAnimatorCreateHeightAnimator.setDuration(250L);
        valueAnimatorCreateHeightAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        valueAnimatorCreateHeightAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.PanelView.5
            private boolean mCancelled;
            final /* synthetic */ Runnable val$onAnimationFinished;

            AnonymousClass5(Runnable runnable2) {
                runnable = runnable2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                this.mCancelled = true;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (this.mCancelled) {
                    PanelView.this.setAnimator(null);
                    runnable.run();
                } else {
                    PanelView.this.startUnlockHintAnimationPhase2(runnable);
                }
            }
        });
        valueAnimatorCreateHeightAnimator.start();
        setAnimator(valueAnimatorCreateHeightAnimator);
        for (final View view : new View[]{this.mKeyguardBottomArea.getIndicationArea(), this.mStatusBar.getAmbientIndicationContainer()}) {
            if (view != null) {
                view.animate().translationY(-this.mHintDistance).setDuration(250L).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).withEndAction(new Runnable() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$PanelView$NxwxpLj3ZElyZw-bMmniBqBlhdY
                    @Override // java.lang.Runnable
                    public final void run() {
                        view.animate().translationY(0.0f).setDuration(450L).setInterpolator(this.f$0.mBounceInterpolator).start();
                    }
                }).start();
            }
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.PanelView$5 */
    class AnonymousClass5 extends AnimatorListenerAdapter {
        private boolean mCancelled;
        final /* synthetic */ Runnable val$onAnimationFinished;

        AnonymousClass5(Runnable runnable2) {
            runnable = runnable2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            this.mCancelled = true;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (this.mCancelled) {
                PanelView.this.setAnimator(null);
                runnable.run();
            } else {
                PanelView.this.startUnlockHintAnimationPhase2(runnable);
            }
        }
    }

    private void setAnimator(ValueAnimator valueAnimator) {
        this.mHeightAnimator = valueAnimator;
        if (valueAnimator == null && this.mPanelUpdateWhenAnimatorEnds) {
            this.mPanelUpdateWhenAnimatorEnds = false;
            requestPanelHeightUpdate();
        }
    }

    private void startUnlockHintAnimationPhase2(Runnable runnable) {
        ValueAnimator valueAnimatorCreateHeightAnimator = createHeightAnimator(getMaxPanelHeight());
        valueAnimatorCreateHeightAnimator.setDuration(450L);
        valueAnimatorCreateHeightAnimator.setInterpolator(this.mBounceInterpolator);
        valueAnimatorCreateHeightAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.statusbar.phone.PanelView.6
            final /* synthetic */ Runnable val$onAnimationFinished;

            AnonymousClass6(Runnable runnable2) {
                runnable = runnable2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                PanelView.this.setAnimator(null);
                runnable.run();
                PanelView.this.notifyBarPanelExpansionChanged();
            }
        });
        valueAnimatorCreateHeightAnimator.start();
        setAnimator(valueAnimatorCreateHeightAnimator);
    }

    /* renamed from: com.android.systemui.statusbar.phone.PanelView$6 */
    class AnonymousClass6 extends AnimatorListenerAdapter {
        final /* synthetic */ Runnable val$onAnimationFinished;

        AnonymousClass6(Runnable runnable2) {
            runnable = runnable2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            PanelView.this.setAnimator(null);
            runnable.run();
            PanelView.this.notifyBarPanelExpansionChanged();
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.PanelView$7 */
    class AnonymousClass7 implements ValueAnimator.AnimatorUpdateListener {
        AnonymousClass7() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            PanelView.this.setExpandedHeightInternal(((Float) valueAnimator.getAnimatedValue()).floatValue());
        }
    }

    private ValueAnimator createHeightAnimator(float f) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.mExpandedHeight, f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.phone.PanelView.7
            AnonymousClass7() {
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                PanelView.this.setExpandedHeightInternal(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        return valueAnimatorOfFloat;
    }

    protected void notifyBarPanelExpansionChanged() {
        this.mBar.panelExpansionChanged(this.mExpandedFraction, this.mExpandedFraction > 0.0f || this.mPeekAnimator != null || this.mInstantExpanding || isPanelVisibleBecauseOfHeadsUp() || this.mTracking || this.mHeightAnimator != null);
        if (this.mExpansionListener != null) {
            this.mExpansionListener.accept(Float.valueOf(this.mExpandedFraction), Boolean.valueOf(this.mTracking));
        }
    }

    public void setExpansionListener(BiConsumer<Float, Boolean> biConsumer) {
        this.mExpansionListener = biConsumer;
    }

    protected boolean onEmptySpaceClick(float f) {
        if (this.mHintAnimationRunning) {
            return true;
        }
        return onMiddleClicked();
    }

    /* renamed from: com.android.systemui.statusbar.phone.PanelView$8 */
    class AnonymousClass8 implements Runnable {
        AnonymousClass8() {
        }

        @Override // java.lang.Runnable
        public void run() {
            PanelView.this.collapse(false, 1.0f);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
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
        printWriter.println(String.format("[PanelView(%s): expandedHeight=%f maxPanelHeight=%d closing=%s tracking=%s justPeeked=%s peekAnim=%s%s timeAnim=%s%s touchDisabled=%s]", objArr));
    }

    public void setHeadsUpManager(HeadsUpManagerPhone headsUpManagerPhone) {
        this.mHeadsUpManager = headsUpManagerPhone;
    }

    public void setLaunchingNotification(boolean z) {
        this.mLaunchingNotification = z;
    }

    public void collapseWithDuration(int i) {
        this.mFixedDuration = i;
        collapse(false, 1.0f);
        this.mFixedDuration = -1;
    }
}
