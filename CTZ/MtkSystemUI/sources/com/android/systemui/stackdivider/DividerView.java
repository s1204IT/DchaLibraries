package com.android.systemui.stackdivider;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.DividerSnapAlgorithm;
import com.android.internal.policy.DockedDividerUtils;
import com.android.internal.view.SurfaceFlingerVsyncChoreographer;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.activity.UndockingTaskEvent;
import com.android.systemui.recents.events.ui.RecentsGrowingEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.stackdivider.events.StartedDragingEvent;
import com.android.systemui.stackdivider.events.StoppedDragingEvent;
import com.android.systemui.statusbar.FlingAnimationUtils;

public class DividerView extends FrameLayout implements View.OnTouchListener, ViewTreeObserver.OnComputeInternalInsetsListener {
    private boolean mAdjustedForIme;
    private View mBackground;
    private boolean mBackgroundLifted;
    private ValueAnimator mCurrentAnimator;
    private final Display mDefaultDisplay;
    private int mDisplayHeight;
    private int mDisplayRotation;
    private int mDisplayWidth;
    private int mDividerInsets;
    private int mDividerSize;
    private int mDividerWindowWidth;
    private int mDockSide;
    private final Rect mDockedInsetRect;
    private final Rect mDockedRect;
    private boolean mDockedStackMinimized;
    private final Rect mDockedTaskRect;
    private boolean mEntranceAnimationRunning;
    private boolean mExitAnimationRunning;
    private int mExitStartPosition;
    private FlingAnimationUtils mFlingAnimationUtils;
    private boolean mGrowRecents;
    private DividerHandleView mHandle;
    private final View.AccessibilityDelegate mHandleDelegate;
    private final Handler mHandler;
    private boolean mHomeStackResizable;
    private boolean mIsInMinimizeInteraction;
    private final Rect mLastResizeRect;
    private int mLongPressEntraceAnimDuration;
    private MinimizedDockShadow mMinimizedShadow;
    private DividerSnapAlgorithm mMinimizedSnapAlgorithm;
    private boolean mMoving;
    private final Rect mOtherInsetRect;
    private final Rect mOtherRect;
    private final Rect mOtherTaskRect;
    private boolean mRemoved;
    private final Runnable mResetBackgroundRunnable;
    private final SurfaceFlingerVsyncChoreographer mSfChoreographer;
    private DividerSnapAlgorithm mSnapAlgorithm;
    private DividerSnapAlgorithm.SnapTarget mSnapTargetBeforeMinimized;
    private final Rect mStableInsets;
    private int mStartPosition;
    private int mStartX;
    private int mStartY;
    private DividerState mState;
    private final int[] mTempInt2;
    private final Rect mTmpRect;
    private int mTouchElevation;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;
    private DividerWindowManager mWindowManager;
    private final WindowManagerProxy mWindowManagerProxy;
    private static final PathInterpolator SLOWDOWN_INTERPOLATOR = new PathInterpolator(0.5f, 1.0f, 0.5f, 1.0f);
    private static final PathInterpolator DIM_INTERPOLATOR = new PathInterpolator(0.23f, 0.87f, 0.52f, -0.11f);
    private static final Interpolator IME_ADJUST_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.1f, 1.0f);

    public DividerView(Context context) {
        this(context, null);
    }

    public DividerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DividerView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public DividerView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTempInt2 = new int[2];
        this.mDockedRect = new Rect();
        this.mDockedTaskRect = new Rect();
        this.mOtherTaskRect = new Rect();
        this.mOtherRect = new Rect();
        this.mDockedInsetRect = new Rect();
        this.mOtherInsetRect = new Rect();
        this.mLastResizeRect = new Rect();
        this.mTmpRect = new Rect();
        this.mWindowManagerProxy = WindowManagerProxy.getInstance();
        this.mStableInsets = new Rect();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 0) {
                    DividerView.this.resizeStack(message.arg1, message.arg2, (DividerSnapAlgorithm.SnapTarget) message.obj);
                } else {
                    super.handleMessage(message);
                }
            }
        };
        this.mHandleDelegate = new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                DividerSnapAlgorithm snapAlgorithm = DividerView.this.getSnapAlgorithm();
                if (DividerView.this.isHorizontalDivision()) {
                    accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_full, DividerView.this.mContext.getString(R.string.accessibility_action_divider_top_full)));
                    if (snapAlgorithm.isFirstSplitTargetAvailable()) {
                        accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_70, DividerView.this.mContext.getString(R.string.accessibility_action_divider_top_70)));
                    }
                    if (snapAlgorithm.showMiddleSplitTargetForAccessibility()) {
                        accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_50, DividerView.this.mContext.getString(R.string.accessibility_action_divider_top_50)));
                    }
                    if (snapAlgorithm.isLastSplitTargetAvailable()) {
                        accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_30, DividerView.this.mContext.getString(R.string.accessibility_action_divider_top_30)));
                    }
                    accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_rb_full, DividerView.this.mContext.getString(R.string.accessibility_action_divider_bottom_full)));
                    return;
                }
                accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_full, DividerView.this.mContext.getString(R.string.accessibility_action_divider_left_full)));
                if (snapAlgorithm.isFirstSplitTargetAvailable()) {
                    accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_70, DividerView.this.mContext.getString(R.string.accessibility_action_divider_left_70)));
                }
                if (snapAlgorithm.showMiddleSplitTargetForAccessibility()) {
                    accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_50, DividerView.this.mContext.getString(R.string.accessibility_action_divider_left_50)));
                }
                if (snapAlgorithm.isLastSplitTargetAvailable()) {
                    accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_30, DividerView.this.mContext.getString(R.string.accessibility_action_divider_left_30)));
                }
                accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_rb_full, DividerView.this.mContext.getString(R.string.accessibility_action_divider_right_full)));
            }

            @Override
            public boolean performAccessibilityAction(View view, int i3, Bundle bundle) {
                DividerSnapAlgorithm.SnapTarget dismissStartTarget;
                int currentPosition = DividerView.this.getCurrentPosition();
                switch (i3) {
                    case R.id.action_move_rb_full:
                        dismissStartTarget = DividerView.this.mSnapAlgorithm.getDismissStartTarget();
                        break;
                    case R.id.action_move_tl_30:
                        dismissStartTarget = DividerView.this.mSnapAlgorithm.getFirstSplitTarget();
                        break;
                    case R.id.action_move_tl_50:
                        dismissStartTarget = DividerView.this.mSnapAlgorithm.getMiddleTarget();
                        break;
                    case R.id.action_move_tl_70:
                        dismissStartTarget = DividerView.this.mSnapAlgorithm.getLastSplitTarget();
                        break;
                    case R.id.action_move_tl_full:
                        dismissStartTarget = DividerView.this.mSnapAlgorithm.getDismissEndTarget();
                        break;
                    default:
                        dismissStartTarget = null;
                        break;
                }
                DividerSnapAlgorithm.SnapTarget snapTarget = dismissStartTarget;
                if (snapTarget != null) {
                    DividerView.this.startDragging(true, false);
                    DividerView.this.stopDragging(currentPosition, snapTarget, 250L, Interpolators.FAST_OUT_SLOW_IN);
                    return true;
                }
                return super.performAccessibilityAction(view, i3, bundle);
            }
        };
        this.mResetBackgroundRunnable = new Runnable() {
            @Override
            public void run() {
                DividerView.this.resetBackground();
            }
        };
        this.mSfChoreographer = new SurfaceFlingerVsyncChoreographer(this.mHandler, context.getDisplay(), Choreographer.getInstance());
        this.mDefaultDisplay = ((DisplayManager) this.mContext.getSystemService("display")).getDisplay(0);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHandle = (DividerHandleView) findViewById(R.id.docked_divider_handle);
        this.mBackground = findViewById(R.id.docked_divider_background);
        this.mMinimizedShadow = (MinimizedDockShadow) findViewById(R.id.minimized_dock_shadow);
        this.mHandle.setOnTouchListener(this);
        this.mDividerWindowWidth = getResources().getDimensionPixelSize(android.R.dimen.car_borderless_button_horizontal_padding);
        this.mDividerInsets = getResources().getDimensionPixelSize(android.R.dimen.car_body5_size);
        this.mDividerSize = this.mDividerWindowWidth - (this.mDividerInsets * 2);
        this.mTouchElevation = getResources().getDimensionPixelSize(R.dimen.docked_stack_divider_lift_elevation);
        this.mLongPressEntraceAnimDuration = getResources().getInteger(R.integer.long_press_dock_anim_duration);
        this.mGrowRecents = getResources().getBoolean(R.bool.recents_grow_in_multiwindow);
        this.mTouchSlop = ViewConfiguration.get(this.mContext).getScaledTouchSlop();
        this.mFlingAnimationUtils = new FlingAnimationUtils(getContext(), 0.3f);
        updateDisplayInfo();
        this.mHandle.setPointerIcon(PointerIcon.getSystemIcon(getContext(), getResources().getConfiguration().orientation == 2 ? 1014 : 1015));
        getViewTreeObserver().addOnComputeInternalInsetsListener(this);
        this.mHandle.setAccessibilityDelegate(this.mHandleDelegate);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
        if (this.mHomeStackResizable && this.mDockSide != -1 && !this.mIsInMinimizeInteraction) {
            saveSnapTargetBeforeMinimized(this.mSnapTargetBeforeMinimized);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    void onDividerRemoved() {
        this.mRemoved = true;
        this.mHandler.removeMessages(0);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        if (this.mStableInsets.left != windowInsets.getStableInsetLeft() || this.mStableInsets.top != windowInsets.getStableInsetTop() || this.mStableInsets.right != windowInsets.getStableInsetRight() || this.mStableInsets.bottom != windowInsets.getStableInsetBottom()) {
            this.mStableInsets.set(windowInsets.getStableInsetLeft(), windowInsets.getStableInsetTop(), windowInsets.getStableInsetRight(), windowInsets.getStableInsetBottom());
            if (this.mSnapAlgorithm != null || this.mMinimizedSnapAlgorithm != null) {
                this.mSnapAlgorithm = null;
                this.mMinimizedSnapAlgorithm = null;
                initializeSnapAlgorithm();
            }
        }
        return super.onApplyWindowInsets(windowInsets);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int left;
        super.onLayout(z, i, i2, i3, i4);
        int top = 0;
        if (this.mDockSide == 2) {
            top = this.mBackground.getTop();
            left = 0;
        } else {
            left = this.mDockSide == 1 ? this.mBackground.getLeft() : this.mDockSide == 3 ? this.mBackground.getRight() - this.mMinimizedShadow.getWidth() : 0;
        }
        this.mMinimizedShadow.layout(left, top, this.mMinimizedShadow.getMeasuredWidth() + left, this.mMinimizedShadow.getMeasuredHeight() + top);
        if (z) {
            this.mWindowManagerProxy.setTouchRegion(new Rect(this.mHandle.getLeft(), this.mHandle.getTop(), this.mHandle.getRight(), this.mHandle.getBottom()));
        }
    }

    public void injectDependencies(DividerWindowManager dividerWindowManager, DividerState dividerState) {
        this.mWindowManager = dividerWindowManager;
        this.mState = dividerState;
        if (this.mStableInsets.isEmpty()) {
            SystemServicesProxy.getInstance(this.mContext).getStableInsets(this.mStableInsets);
        }
        if (this.mState.mRatioPositionBeforeMinimized == 0.0f) {
            this.mSnapTargetBeforeMinimized = this.mSnapAlgorithm.getMiddleTarget();
        } else {
            repositionSnapTargetBeforeMinimized();
        }
    }

    public WindowManagerProxy getWindowManagerProxy() {
        return this.mWindowManagerProxy;
    }

    public Rect getNonMinimizedSplitScreenSecondaryBounds() {
        calculateBoundsForPosition(this.mSnapTargetBeforeMinimized.position, DockedDividerUtils.invertDockSide(this.mDockSide), this.mOtherTaskRect);
        this.mOtherTaskRect.bottom -= this.mStableInsets.bottom;
        int i = this.mDockSide;
        if (i == 1) {
            this.mOtherTaskRect.top += this.mStableInsets.top;
            this.mOtherTaskRect.right -= this.mStableInsets.right;
        } else if (i == 3) {
            this.mOtherTaskRect.top += this.mStableInsets.top;
            this.mOtherTaskRect.left += this.mStableInsets.left;
        }
        return this.mOtherTaskRect;
    }

    public boolean startDragging(boolean z, boolean z2) {
        cancelFlingAnimation();
        if (z2) {
            this.mHandle.setTouching(true, z);
        }
        this.mDockSide = this.mWindowManagerProxy.getDockSide();
        if (this.mDisplayRotation != this.mDefaultDisplay.getRotation()) {
            updateDisplayInfo();
        }
        initializeSnapAlgorithm();
        this.mWindowManagerProxy.setResizing(true);
        if (z2) {
            this.mWindowManager.setSlippery(false);
            liftBackground();
        }
        EventBus.getDefault().send(new StartedDragingEvent());
        if (this.mDockSide != -1) {
            return true;
        }
        return false;
    }

    public void stopDragging(int i, float f, boolean z, boolean z2) {
        this.mHandle.setTouching(false, true);
        fling(i, f, z, z2);
        this.mWindowManager.setSlippery(true);
        releaseBackground();
    }

    public void stopDragging(int i, DividerSnapAlgorithm.SnapTarget snapTarget, long j, Interpolator interpolator) {
        stopDragging(i, snapTarget, j, 0L, 0L, interpolator);
    }

    public void stopDragging(int i, DividerSnapAlgorithm.SnapTarget snapTarget, long j, Interpolator interpolator, long j2) {
        stopDragging(i, snapTarget, j, 0L, j2, interpolator);
    }

    public void stopDragging(int i, DividerSnapAlgorithm.SnapTarget snapTarget, long j, long j2, long j3, Interpolator interpolator) {
        this.mHandle.setTouching(false, true);
        flingTo(i, snapTarget, j, j2, j3, interpolator);
        this.mWindowManager.setSlippery(true);
        releaseBackground();
    }

    private void stopDragging() {
        this.mHandle.setTouching(false, true);
        this.mWindowManager.setSlippery(true);
        releaseBackground();
    }

    private void updateDockSide() {
        this.mDockSide = this.mWindowManagerProxy.getDockSide();
        this.mMinimizedShadow.setDockSide(this.mDockSide);
    }

    private void initializeSnapAlgorithm() {
        if (this.mSnapAlgorithm == null) {
            this.mSnapAlgorithm = new DividerSnapAlgorithm(getContext().getResources(), this.mDisplayWidth, this.mDisplayHeight, this.mDividerSize, isHorizontalDivision(), this.mStableInsets, this.mDockSide);
        }
        if (this.mMinimizedSnapAlgorithm == null) {
            this.mMinimizedSnapAlgorithm = new DividerSnapAlgorithm(getContext().getResources(), this.mDisplayWidth, this.mDisplayHeight, this.mDividerSize, isHorizontalDivision(), this.mStableInsets, this.mDockSide, this.mDockedStackMinimized && this.mHomeStackResizable);
        }
    }

    public DividerSnapAlgorithm getSnapAlgorithm() {
        initializeSnapAlgorithm();
        return (this.mDockedStackMinimized && this.mHomeStackResizable) ? this.mMinimizedSnapAlgorithm : this.mSnapAlgorithm;
    }

    public int getCurrentPosition() {
        getLocationOnScreen(this.mTempInt2);
        if (isHorizontalDivision()) {
            return this.mTempInt2[1] + this.mDividerInsets;
        }
        return this.mTempInt2[0] + this.mDividerInsets;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        convertToScreenCoordinates(motionEvent);
        switch (motionEvent.getAction() & 255) {
            case 0:
                this.mVelocityTracker = VelocityTracker.obtain();
                this.mVelocityTracker.addMovement(motionEvent);
                this.mStartX = (int) motionEvent.getX();
                this.mStartY = (int) motionEvent.getY();
                boolean zStartDragging = startDragging(true, true);
                if (!zStartDragging) {
                    stopDragging();
                }
                this.mStartPosition = getCurrentPosition();
                this.mMoving = false;
                return zStartDragging;
            case 1:
            case 3:
                this.mVelocityTracker.addMovement(motionEvent);
                int rawX = (int) motionEvent.getRawX();
                int rawY = (int) motionEvent.getRawY();
                this.mVelocityTracker.computeCurrentVelocity(1000);
                stopDragging(calculatePosition(rawX, rawY), isHorizontalDivision() ? this.mVelocityTracker.getYVelocity() : this.mVelocityTracker.getXVelocity(), false, true);
                this.mMoving = false;
                return true;
            case 2:
                this.mVelocityTracker.addMovement(motionEvent);
                int x = (int) motionEvent.getX();
                int y = (int) motionEvent.getY();
                boolean z = (isHorizontalDivision() && Math.abs(y - this.mStartY) > this.mTouchSlop) || (!isHorizontalDivision() && Math.abs(x - this.mStartX) > this.mTouchSlop);
                if (!this.mMoving && z) {
                    this.mStartX = x;
                    this.mStartY = y;
                    this.mMoving = true;
                }
                if (this.mMoving && this.mDockSide != -1) {
                    resizeStackDelayed(calculatePosition(x, y), this.mStartPosition, getSnapAlgorithm().calculateSnapTarget(this.mStartPosition, 0.0f, false));
                }
                return true;
            default:
                return true;
        }
    }

    private void logResizeEvent(DividerSnapAlgorithm.SnapTarget snapTarget) {
        if (snapTarget == this.mSnapAlgorithm.getDismissStartTarget()) {
            MetricsLogger.action(this.mContext, 390, dockSideTopLeft(this.mDockSide) ? 1 : 0);
            return;
        }
        if (snapTarget == this.mSnapAlgorithm.getDismissEndTarget()) {
            MetricsLogger.action(this.mContext, 390, dockSideBottomRight(this.mDockSide) ? 1 : 0);
            return;
        }
        if (snapTarget == this.mSnapAlgorithm.getMiddleTarget()) {
            MetricsLogger.action(this.mContext, 389, 0);
            return;
        }
        if (snapTarget == this.mSnapAlgorithm.getFirstSplitTarget()) {
            MetricsLogger.action(this.mContext, 389, dockSideTopLeft(this.mDockSide) ? 1 : 2);
        } else if (snapTarget == this.mSnapAlgorithm.getLastSplitTarget()) {
            MetricsLogger.action(this.mContext, 389, dockSideTopLeft(this.mDockSide) ? 2 : 1);
        }
    }

    private void convertToScreenCoordinates(MotionEvent motionEvent) {
        motionEvent.setLocation(motionEvent.getRawX(), motionEvent.getRawY());
    }

    private void fling(int i, float f, boolean z, boolean z2) {
        DividerSnapAlgorithm snapAlgorithm = getSnapAlgorithm();
        DividerSnapAlgorithm.SnapTarget snapTargetCalculateSnapTarget = snapAlgorithm.calculateSnapTarget(i, f);
        if (z && snapTargetCalculateSnapTarget == snapAlgorithm.getDismissStartTarget()) {
            snapTargetCalculateSnapTarget = snapAlgorithm.getFirstSplitTarget();
        }
        if (z2) {
            logResizeEvent(snapTargetCalculateSnapTarget);
        }
        ValueAnimator flingAnimator = getFlingAnimator(i, snapTargetCalculateSnapTarget, 0L);
        this.mFlingAnimationUtils.apply(flingAnimator, i, snapTargetCalculateSnapTarget.position, f);
        flingAnimator.start();
    }

    private void flingTo(int i, DividerSnapAlgorithm.SnapTarget snapTarget, long j, long j2, long j3, Interpolator interpolator) {
        ValueAnimator flingAnimator = getFlingAnimator(i, snapTarget, j3);
        flingAnimator.setDuration(j);
        flingAnimator.setStartDelay(j2);
        flingAnimator.setInterpolator(interpolator);
        flingAnimator.start();
    }

    private ValueAnimator getFlingAnimator(int i, final DividerSnapAlgorithm.SnapTarget snapTarget, final long j) {
        if (this.mCurrentAnimator != null) {
            cancelFlingAnimation();
            updateDockSide();
        }
        final boolean z = snapTarget.flag == 0;
        ValueAnimator valueAnimatorOfInt = ValueAnimator.ofInt(i, snapTarget.position);
        valueAnimatorOfInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                DividerView.lambda$getFlingAnimator$0(this.f$0, z, snapTarget, valueAnimator);
            }
        });
        final Runnable runnable = new Runnable() {
            @Override
            public final void run() {
                DividerView.lambda$getFlingAnimator$1(this.f$0, snapTarget);
            }
        };
        final Runnable runnable2 = new Runnable() {
            @Override
            public final void run() {
                DividerView.lambda$getFlingAnimator$2(this.f$0);
            }
        };
        valueAnimatorOfInt.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animator) {
                DividerView.this.mHandler.removeMessages(0);
                this.mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                long surfaceFlingerOffsetMs;
                if (j != 0) {
                    surfaceFlingerOffsetMs = j;
                } else if (!this.mCancelled && DividerView.this.mSfChoreographer.getSurfaceFlingerOffsetMs() > 0) {
                    surfaceFlingerOffsetMs = DividerView.this.mSfChoreographer.getSurfaceFlingerOffsetMs();
                } else {
                    surfaceFlingerOffsetMs = 0;
                }
                if (surfaceFlingerOffsetMs == 0) {
                    if (!this.mCancelled) {
                        runnable2.run();
                    }
                    runnable.run();
                } else {
                    if (!this.mCancelled) {
                        DividerView.this.mHandler.postDelayed(runnable2, surfaceFlingerOffsetMs);
                    }
                    DividerView.this.mHandler.postDelayed(runnable, surfaceFlingerOffsetMs);
                }
            }
        });
        this.mCurrentAnimator = valueAnimatorOfInt;
        return valueAnimatorOfInt;
    }

    public static void lambda$getFlingAnimator$0(DividerView dividerView, boolean z, DividerSnapAlgorithm.SnapTarget snapTarget, ValueAnimator valueAnimator) {
        int i;
        int iIntValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
        if (z && valueAnimator.getAnimatedFraction() == 1.0f) {
            i = Integer.MAX_VALUE;
        } else {
            i = snapTarget.taskPosition;
        }
        dividerView.resizeStackDelayed(iIntValue, i, snapTarget);
    }

    public static void lambda$getFlingAnimator$1(DividerView dividerView, DividerSnapAlgorithm.SnapTarget snapTarget) {
        dividerView.commitSnapFlags(snapTarget);
        dividerView.mWindowManagerProxy.setResizing(false);
        dividerView.updateDockSide();
        dividerView.mCurrentAnimator = null;
        dividerView.mEntranceAnimationRunning = false;
        dividerView.mExitAnimationRunning = false;
        EventBus.getDefault().send(new StoppedDragingEvent());
        if (dividerView.mHomeStackResizable && !dividerView.mIsInMinimizeInteraction) {
            if (snapTarget.position < 0) {
                snapTarget = dividerView.mSnapAlgorithm.getMiddleTarget();
            }
            if (snapTarget.position != dividerView.mSnapAlgorithm.getDismissEndTarget().position && snapTarget.position != dividerView.mSnapAlgorithm.getDismissStartTarget().position) {
                dividerView.saveSnapTargetBeforeMinimized(snapTarget);
            }
        }
    }

    public static void lambda$getFlingAnimator$2(DividerView dividerView) {
        if (!dividerView.mDockedStackMinimized && dividerView.mIsInMinimizeInteraction) {
            dividerView.mIsInMinimizeInteraction = false;
        }
    }

    private void cancelFlingAnimation() {
        if (this.mCurrentAnimator != null) {
            this.mCurrentAnimator.cancel();
        }
    }

    private void commitSnapFlags(DividerSnapAlgorithm.SnapTarget snapTarget) {
        if (snapTarget.flag == 0) {
            return;
        }
        boolean z = true;
        if (snapTarget.flag != 1 ? !(this.mDockSide == 3 || this.mDockSide == 4) : !(this.mDockSide == 1 || this.mDockSide == 2)) {
            z = false;
        }
        if (z) {
            this.mWindowManagerProxy.dismissDockedStack();
        } else {
            this.mWindowManagerProxy.maximizeDockedStack();
        }
        this.mWindowManagerProxy.setResizeDimLayer(false, 0, 0.0f);
    }

    private void liftBackground() {
        if (this.mBackgroundLifted) {
            return;
        }
        if (isHorizontalDivision()) {
            this.mBackground.animate().scaleY(1.4f);
        } else {
            this.mBackground.animate().scaleX(1.4f);
        }
        this.mBackground.animate().setInterpolator(Interpolators.TOUCH_RESPONSE).setDuration(150L).translationZ(this.mTouchElevation).start();
        this.mHandle.animate().setInterpolator(Interpolators.TOUCH_RESPONSE).setDuration(150L).translationZ(this.mTouchElevation).start();
        this.mBackgroundLifted = true;
    }

    private void releaseBackground() {
        if (!this.mBackgroundLifted) {
            return;
        }
        this.mBackground.animate().setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setDuration(200L).translationZ(0.0f).scaleX(1.0f).scaleY(1.0f).start();
        this.mHandle.animate().setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setDuration(200L).translationZ(0.0f).start();
        this.mBackgroundLifted = false;
    }

    public void setMinimizedDockStack(boolean z, boolean z2) {
        float width;
        this.mHomeStackResizable = z2;
        updateDockSide();
        if (!z) {
            resetBackground();
        } else if (!z2) {
            if (this.mDockSide != 2) {
                if (this.mDockSide == 1 || this.mDockSide == 3) {
                    View view = this.mBackground;
                    if (this.mDockSide == 1) {
                        width = 0.0f;
                    } else {
                        width = this.mBackground.getWidth();
                    }
                    view.setPivotX(width);
                    this.mBackground.setScaleX(0.0f);
                }
            } else {
                this.mBackground.setPivotY(0.0f);
                this.mBackground.setScaleY(0.0f);
            }
        }
        this.mMinimizedShadow.setAlpha(z ? 1.0f : 0.0f);
        if (!z2) {
            this.mHandle.setAlpha(z ? 0.0f : 1.0f);
            this.mDockedStackMinimized = z;
            return;
        }
        if (this.mDockedStackMinimized != z) {
            this.mDockedStackMinimized = z;
            if (this.mDisplayRotation != this.mDefaultDisplay.getRotation()) {
                SystemServicesProxy.getInstance(this.mContext).getStableInsets(this.mStableInsets);
                repositionSnapTargetBeforeMinimized();
                updateDisplayInfo();
            } else {
                this.mMinimizedSnapAlgorithm = null;
                initializeSnapAlgorithm();
            }
            if (this.mIsInMinimizeInteraction != z || this.mCurrentAnimator != null) {
                cancelFlingAnimation();
                if (z) {
                    requestLayout();
                    this.mIsInMinimizeInteraction = true;
                    resizeStack(this.mMinimizedSnapAlgorithm.getMiddleTarget());
                } else {
                    resizeStack(this.mSnapTargetBeforeMinimized);
                    this.mIsInMinimizeInteraction = false;
                }
            }
        }
    }

    public void setMinimizedDockStack(boolean z, long j, boolean z2) {
        int currentPosition;
        float width;
        this.mHomeStackResizable = z2;
        updateDockSide();
        if (!z2) {
            this.mMinimizedShadow.animate().alpha(z ? 1.0f : 0.0f).setInterpolator(Interpolators.ALPHA_IN).setDuration(j).start();
            this.mHandle.animate().setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setDuration(j).alpha(z ? 0.0f : 1.0f).start();
            if (this.mDockSide != 2) {
                if (this.mDockSide == 1 || this.mDockSide == 3) {
                    View view = this.mBackground;
                    if (this.mDockSide == 1) {
                        width = 0.0f;
                    } else {
                        width = this.mBackground.getWidth();
                    }
                    view.setPivotX(width);
                    this.mBackground.animate().scaleX(z ? 0.0f : 1.0f);
                }
            } else {
                this.mBackground.setPivotY(0.0f);
                this.mBackground.animate().scaleY(z ? 0.0f : 1.0f);
            }
            this.mDockedStackMinimized = z;
        } else if (this.mDockedStackMinimized != z) {
            this.mIsInMinimizeInteraction = true;
            this.mMinimizedSnapAlgorithm = null;
            this.mDockedStackMinimized = z;
            initializeSnapAlgorithm();
            if (z) {
                currentPosition = this.mSnapTargetBeforeMinimized.position;
            } else {
                currentPosition = getCurrentPosition();
            }
            stopDragging(currentPosition, z ? this.mMinimizedSnapAlgorithm.getMiddleTarget() : this.mSnapTargetBeforeMinimized, j, Interpolators.FAST_OUT_SLOW_IN, 0L);
            setAdjustedForIme(false, j);
        }
        if (!z) {
            this.mBackground.animate().withEndAction(this.mResetBackgroundRunnable);
        }
        this.mBackground.animate().setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setDuration(j).start();
    }

    public void setAdjustedForIme(boolean z) {
        updateDockSide();
        this.mHandle.setAlpha(z ? 0.0f : 1.0f);
        if (!z) {
            resetBackground();
        } else if (this.mDockSide == 2) {
            this.mBackground.setPivotY(0.0f);
            this.mBackground.setScaleY(0.5f);
        }
        this.mAdjustedForIme = z;
    }

    public void setAdjustedForIme(boolean z, long j) {
        updateDockSide();
        this.mHandle.animate().setInterpolator(IME_ADJUST_INTERPOLATOR).setDuration(j).alpha(z ? 0.0f : 1.0f).start();
        if (this.mDockSide == 2) {
            this.mBackground.setPivotY(0.0f);
            this.mBackground.animate().scaleY(z ? 0.5f : 1.0f);
        }
        if (!z) {
            this.mBackground.animate().withEndAction(this.mResetBackgroundRunnable);
        }
        this.mBackground.animate().setInterpolator(IME_ADJUST_INTERPOLATOR).setDuration(j).start();
        this.mAdjustedForIme = z;
    }

    private void saveSnapTargetBeforeMinimized(DividerSnapAlgorithm.SnapTarget snapTarget) {
        this.mSnapTargetBeforeMinimized = snapTarget;
        this.mState.mRatioPositionBeforeMinimized = snapTarget.position / (isHorizontalDivision() ? this.mDisplayHeight : this.mDisplayWidth);
    }

    public void resetBackground() {
        this.mBackground.setPivotX(this.mBackground.getWidth() / 2);
        this.mBackground.setPivotY(this.mBackground.getHeight() / 2);
        this.mBackground.setScaleX(1.0f);
        this.mBackground.setScaleY(1.0f);
        this.mMinimizedShadow.setAlpha(0.0f);
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateDisplayInfo();
    }

    public void notifyDockSideChanged(int i) {
        int i2 = this.mDockSide;
        this.mDockSide = i;
        this.mMinimizedShadow.setDockSide(this.mDockSide);
        requestLayout();
        SystemServicesProxy.getInstance(this.mContext).getStableInsets(this.mStableInsets);
        this.mMinimizedSnapAlgorithm = null;
        initializeSnapAlgorithm();
        if ((i2 == 1 && this.mDockSide == 3) || (i2 == 3 && this.mDockSide == 1)) {
            repositionSnapTargetBeforeMinimized();
        }
        if (this.mHomeStackResizable && this.mDockedStackMinimized) {
            resizeStack(this.mMinimizedSnapAlgorithm.getMiddleTarget());
        }
    }

    private void repositionSnapTargetBeforeMinimized() {
        int i = (int) (this.mState.mRatioPositionBeforeMinimized * (isHorizontalDivision() ? this.mDisplayHeight : this.mDisplayWidth));
        this.mSnapAlgorithm = null;
        initializeSnapAlgorithm();
        this.mSnapTargetBeforeMinimized = this.mSnapAlgorithm.calculateNonDismissingSnapTarget(i);
    }

    private void updateDisplayInfo() {
        this.mDisplayRotation = this.mDefaultDisplay.getRotation();
        DisplayInfo displayInfo = new DisplayInfo();
        this.mDefaultDisplay.getDisplayInfo(displayInfo);
        this.mDisplayWidth = displayInfo.logicalWidth;
        this.mDisplayHeight = displayInfo.logicalHeight;
        this.mSnapAlgorithm = null;
        this.mMinimizedSnapAlgorithm = null;
        initializeSnapAlgorithm();
    }

    private int calculatePosition(int i, int i2) {
        return isHorizontalDivision() ? calculateYPosition(i2) : calculateXPosition(i);
    }

    public boolean isHorizontalDivision() {
        return getResources().getConfiguration().orientation == 1;
    }

    private int calculateXPosition(int i) {
        return (this.mStartPosition + i) - this.mStartX;
    }

    private int calculateYPosition(int i) {
        return (this.mStartPosition + i) - this.mStartY;
    }

    private void alignTopLeft(Rect rect, Rect rect2) {
        rect2.set(rect.left, rect.top, rect.left + rect2.width(), rect.top + rect2.height());
    }

    private void alignBottomRight(Rect rect, Rect rect2) {
        rect2.set(rect.right - rect2.width(), rect.bottom - rect2.height(), rect.right, rect.bottom);
    }

    public void calculateBoundsForPosition(int i, int i2, Rect rect) {
        DockedDividerUtils.calculateBoundsForPosition(i, i2, rect, this.mDisplayWidth, this.mDisplayHeight, this.mDividerSize);
    }

    public void resizeStackDelayed(int i, int i2, DividerSnapAlgorithm.SnapTarget snapTarget) {
        Message messageObtainMessage = this.mHandler.obtainMessage(0, i, i2, snapTarget);
        messageObtainMessage.setAsynchronous(true);
        this.mSfChoreographer.scheduleAtSfVsync(this.mHandler, messageObtainMessage);
    }

    private void resizeStack(DividerSnapAlgorithm.SnapTarget snapTarget) {
        resizeStack(snapTarget.position, snapTarget.position, snapTarget);
    }

    public void resizeStack(int i, int i2, DividerSnapAlgorithm.SnapTarget snapTarget) {
        if (this.mRemoved) {
            return;
        }
        calculateBoundsForPosition(i, this.mDockSide, this.mDockedRect);
        if (this.mDockedRect.equals(this.mLastResizeRect) && !this.mEntranceAnimationRunning) {
            return;
        }
        if (this.mBackground.getZ() > 0.0f) {
            this.mBackground.invalidate();
        }
        this.mLastResizeRect.set(this.mDockedRect);
        if (this.mHomeStackResizable && this.mIsInMinimizeInteraction) {
            calculateBoundsForPosition(this.mSnapTargetBeforeMinimized.position, this.mDockSide, this.mDockedTaskRect);
            calculateBoundsForPosition(this.mSnapTargetBeforeMinimized.position, DockedDividerUtils.invertDockSide(this.mDockSide), this.mOtherTaskRect);
            if (this.mDockSide == 3) {
                this.mDockedTaskRect.offset((Math.max(i, this.mStableInsets.left - this.mDividerSize) - this.mDockedTaskRect.left) + this.mDividerSize, 0);
            }
            this.mWindowManagerProxy.resizeDockedStack(this.mDockedRect, this.mDockedTaskRect, this.mDockedTaskRect, this.mOtherTaskRect, null);
            return;
        }
        if (this.mEntranceAnimationRunning && i2 != Integer.MAX_VALUE) {
            calculateBoundsForPosition(i2, this.mDockSide, this.mDockedTaskRect);
            if (this.mDockSide == 3) {
                this.mDockedTaskRect.offset((Math.max(i, this.mStableInsets.left - this.mDividerSize) - this.mDockedTaskRect.left) + this.mDividerSize, 0);
            }
            calculateBoundsForPosition(i2, DockedDividerUtils.invertDockSide(this.mDockSide), this.mOtherTaskRect);
            this.mWindowManagerProxy.resizeDockedStack(this.mDockedRect, this.mDockedTaskRect, null, this.mOtherTaskRect, null);
        } else if (this.mExitAnimationRunning && i2 != Integer.MAX_VALUE) {
            calculateBoundsForPosition(i2, this.mDockSide, this.mDockedTaskRect);
            this.mDockedInsetRect.set(this.mDockedTaskRect);
            calculateBoundsForPosition(this.mExitStartPosition, DockedDividerUtils.invertDockSide(this.mDockSide), this.mOtherTaskRect);
            this.mOtherInsetRect.set(this.mOtherTaskRect);
            applyExitAnimationParallax(this.mOtherTaskRect, i);
            if (this.mDockSide == 3) {
                this.mDockedTaskRect.offset((i - this.mStableInsets.left) + this.mDividerSize, 0);
            }
            this.mWindowManagerProxy.resizeDockedStack(this.mDockedRect, this.mDockedTaskRect, this.mDockedInsetRect, this.mOtherTaskRect, this.mOtherInsetRect);
        } else if (i2 != Integer.MAX_VALUE) {
            calculateBoundsForPosition(i, DockedDividerUtils.invertDockSide(this.mDockSide), this.mOtherRect);
            int iInvertDockSide = DockedDividerUtils.invertDockSide(this.mDockSide);
            int iRestrictDismissingTaskPosition = restrictDismissingTaskPosition(i2, this.mDockSide, snapTarget);
            int iRestrictDismissingTaskPosition2 = restrictDismissingTaskPosition(i2, iInvertDockSide, snapTarget);
            calculateBoundsForPosition(iRestrictDismissingTaskPosition, this.mDockSide, this.mDockedTaskRect);
            calculateBoundsForPosition(iRestrictDismissingTaskPosition2, iInvertDockSide, this.mOtherTaskRect);
            this.mTmpRect.set(0, 0, this.mDisplayWidth, this.mDisplayHeight);
            alignTopLeft(this.mDockedRect, this.mDockedTaskRect);
            alignTopLeft(this.mOtherRect, this.mOtherTaskRect);
            this.mDockedInsetRect.set(this.mDockedTaskRect);
            this.mOtherInsetRect.set(this.mOtherTaskRect);
            if (dockSideTopLeft(this.mDockSide)) {
                alignTopLeft(this.mTmpRect, this.mDockedInsetRect);
                alignBottomRight(this.mTmpRect, this.mOtherInsetRect);
            } else {
                alignBottomRight(this.mTmpRect, this.mDockedInsetRect);
                alignTopLeft(this.mTmpRect, this.mOtherInsetRect);
            }
            applyDismissingParallax(this.mDockedTaskRect, this.mDockSide, snapTarget, i, iRestrictDismissingTaskPosition);
            applyDismissingParallax(this.mOtherTaskRect, iInvertDockSide, snapTarget, i, iRestrictDismissingTaskPosition2);
            this.mWindowManagerProxy.resizeDockedStack(this.mDockedRect, this.mDockedTaskRect, this.mDockedInsetRect, this.mOtherTaskRect, this.mOtherInsetRect);
        } else {
            this.mWindowManagerProxy.resizeDockedStack(this.mDockedRect, null, null, null, null);
        }
        DividerSnapAlgorithm.SnapTarget closestDismissTarget = getSnapAlgorithm().getClosestDismissTarget(i);
        float dimFraction = getDimFraction(i, closestDismissTarget);
        this.mWindowManagerProxy.setResizeDimLayer(dimFraction != 0.0f, getWindowingModeForDismissTarget(closestDismissTarget), dimFraction);
    }

    private void applyExitAnimationParallax(Rect rect, int i) {
        if (this.mDockSide == 2) {
            rect.offset(0, (int) ((i - this.mExitStartPosition) * 0.25f));
        } else if (this.mDockSide == 1) {
            rect.offset((int) ((i - this.mExitStartPosition) * 0.25f), 0);
        } else if (this.mDockSide == 3) {
            rect.offset((int) ((this.mExitStartPosition - i) * 0.25f), 0);
        }
    }

    private float getDimFraction(int i, DividerSnapAlgorithm.SnapTarget snapTarget) {
        if (this.mEntranceAnimationRunning) {
            return 0.0f;
        }
        float interpolation = DIM_INTERPOLATOR.getInterpolation(Math.max(0.0f, Math.min(getSnapAlgorithm().calculateDismissingFraction(i), 1.0f)));
        if (hasInsetsAtDismissTarget(snapTarget)) {
            return interpolation * 0.8f;
        }
        return interpolation;
    }

    private boolean hasInsetsAtDismissTarget(DividerSnapAlgorithm.SnapTarget snapTarget) {
        return isHorizontalDivision() ? snapTarget == getSnapAlgorithm().getDismissStartTarget() ? this.mStableInsets.top != 0 : this.mStableInsets.bottom != 0 : snapTarget == getSnapAlgorithm().getDismissStartTarget() ? this.mStableInsets.left != 0 : this.mStableInsets.right != 0;
    }

    private int restrictDismissingTaskPosition(int i, int i2, DividerSnapAlgorithm.SnapTarget snapTarget) {
        if (snapTarget.flag == 1 && dockSideTopLeft(i2)) {
            return Math.max(this.mSnapAlgorithm.getFirstSplitTarget().position, this.mStartPosition);
        }
        if (snapTarget.flag == 2 && dockSideBottomRight(i2)) {
            return Math.min(this.mSnapAlgorithm.getLastSplitTarget().position, this.mStartPosition);
        }
        return i;
    }

    private void applyDismissingParallax(Rect rect, int i, DividerSnapAlgorithm.SnapTarget snapTarget, int i2, int i3) {
        int i4;
        DividerSnapAlgorithm.SnapTarget lastSplitTarget;
        float fMin = Math.min(1.0f, Math.max(0.0f, this.mSnapAlgorithm.calculateDismissingFraction(i2)));
        DividerSnapAlgorithm.SnapTarget dismissEndTarget = null;
        if (i2 <= this.mSnapAlgorithm.getLastSplitTarget().position && dockSideTopLeft(i)) {
            dismissEndTarget = this.mSnapAlgorithm.getDismissStartTarget();
            i4 = i3;
            lastSplitTarget = this.mSnapAlgorithm.getFirstSplitTarget();
        } else if (i2 >= this.mSnapAlgorithm.getLastSplitTarget().position && dockSideBottomRight(i)) {
            dismissEndTarget = this.mSnapAlgorithm.getDismissEndTarget();
            lastSplitTarget = this.mSnapAlgorithm.getLastSplitTarget();
            i4 = lastSplitTarget.position;
        } else {
            i4 = 0;
            lastSplitTarget = null;
        }
        if (dismissEndTarget != null && fMin > 0.0f && isDismissing(lastSplitTarget, i2, i)) {
            int iCalculateParallaxDismissingFraction = (int) (i4 + (calculateParallaxDismissingFraction(fMin, i) * (dismissEndTarget.position - lastSplitTarget.position)));
            int iWidth = rect.width();
            int iHeight = rect.height();
            switch (i) {
                case 1:
                    rect.left = iCalculateParallaxDismissingFraction - iWidth;
                    rect.right = iCalculateParallaxDismissingFraction;
                    break;
                case 2:
                    rect.top = iCalculateParallaxDismissingFraction - iHeight;
                    rect.bottom = iCalculateParallaxDismissingFraction;
                    break;
                case 3:
                    rect.left = this.mDividerSize + iCalculateParallaxDismissingFraction;
                    rect.right = iCalculateParallaxDismissingFraction + iWidth + this.mDividerSize;
                    break;
                case 4:
                    rect.top = this.mDividerSize + iCalculateParallaxDismissingFraction;
                    rect.bottom = iCalculateParallaxDismissingFraction + iHeight + this.mDividerSize;
                    break;
            }
        }
    }

    private static float calculateParallaxDismissingFraction(float f, int i) {
        float interpolation = SLOWDOWN_INTERPOLATOR.getInterpolation(f) / 3.5f;
        if (i == 2) {
            return interpolation / 2.0f;
        }
        return interpolation;
    }

    private static boolean isDismissing(DividerSnapAlgorithm.SnapTarget snapTarget, int i, int i2) {
        return (i2 == 2 || i2 == 1) ? i < snapTarget.position : i > snapTarget.position;
    }

    private int getWindowingModeForDismissTarget(DividerSnapAlgorithm.SnapTarget snapTarget) {
        if (snapTarget.flag != 1 || !dockSideTopLeft(this.mDockSide)) {
            if (snapTarget.flag == 2 && dockSideBottomRight(this.mDockSide)) {
                return 3;
            }
            return 4;
        }
        return 3;
    }

    private static boolean dockSideTopLeft(int i) {
        return i == 2 || i == 1;
    }

    private static boolean dockSideBottomRight(int i) {
        return i == 4 || i == 3;
    }

    public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
        internalInsetsInfo.setTouchableInsets(3);
        internalInsetsInfo.touchableRegion.set(this.mHandle.getLeft(), this.mHandle.getTop(), this.mHandle.getRight(), this.mHandle.getBottom());
        internalInsetsInfo.touchableRegion.op(this.mBackground.getLeft(), this.mBackground.getTop(), this.mBackground.getRight(), this.mBackground.getBottom(), Region.Op.UNION);
    }

    public int growsRecents() {
        if (this.mGrowRecents && this.mDockSide == 2 && getCurrentPosition() == getSnapAlgorithm().getLastSplitTarget().position) {
            return getSnapAlgorithm().getMiddleTarget().position;
        }
        return -1;
    }

    public final void onBusEvent(RecentsActivityStartingEvent recentsActivityStartingEvent) {
        if (this.mGrowRecents && this.mDockSide == 2 && getSnapAlgorithm().getMiddleTarget() != getSnapAlgorithm().getLastSplitTarget() && getCurrentPosition() == getSnapAlgorithm().getLastSplitTarget().position) {
            this.mState.growAfterRecentsDrawn = true;
            startDragging(false, false);
        }
    }

    public final void onBusEvent(DockedFirstAnimationFrameEvent dockedFirstAnimationFrameEvent) {
        saveSnapTargetBeforeMinimized(this.mSnapAlgorithm.getMiddleTarget());
    }

    public final void onBusEvent(DockedTopTaskEvent dockedTopTaskEvent) {
        if (dockedTopTaskEvent.dragMode == -1) {
            this.mState.growAfterRecentsDrawn = false;
            this.mState.animateAfterRecentsDrawn = true;
            startDragging(false, false);
        }
        updateDockSide();
        this.mEntranceAnimationRunning = true;
        resizeStack(calculatePositionForInsetBounds(), this.mSnapAlgorithm.getMiddleTarget().position, this.mSnapAlgorithm.getMiddleTarget());
    }

    public void onRecentsDrawn() {
        updateDockSide();
        final int iCalculatePositionForInsetBounds = calculatePositionForInsetBounds();
        if (this.mState.animateAfterRecentsDrawn) {
            this.mState.animateAfterRecentsDrawn = false;
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    DividerView dividerView = this.f$0;
                    dividerView.stopDragging(iCalculatePositionForInsetBounds, dividerView.getSnapAlgorithm().getMiddleTarget(), dividerView.mLongPressEntraceAnimDuration, Interpolators.FAST_OUT_SLOW_IN, 200L);
                }
            });
        }
        if (this.mState.growAfterRecentsDrawn) {
            this.mState.growAfterRecentsDrawn = false;
            updateDockSide();
            EventBus.getDefault().send(new RecentsGrowingEvent());
            stopDragging(iCalculatePositionForInsetBounds, getSnapAlgorithm().getMiddleTarget(), 336L, Interpolators.FAST_OUT_SLOW_IN);
        }
    }

    public final void onBusEvent(UndockingTaskEvent undockingTaskEvent) {
        DividerSnapAlgorithm.SnapTarget dismissStartTarget;
        int dockSide = this.mWindowManagerProxy.getDockSide();
        if (dockSide != -1) {
            if (this.mHomeStackResizable || !this.mDockedStackMinimized) {
                startDragging(false, false);
                if (dockSideTopLeft(dockSide)) {
                    dismissStartTarget = this.mSnapAlgorithm.getDismissEndTarget();
                } else {
                    dismissStartTarget = this.mSnapAlgorithm.getDismissStartTarget();
                }
                DividerSnapAlgorithm.SnapTarget snapTarget = dismissStartTarget;
                this.mExitAnimationRunning = true;
                this.mExitStartPosition = getCurrentPosition();
                stopDragging(this.mExitStartPosition, snapTarget, 336L, 100L, 0L, Interpolators.FAST_OUT_SLOW_IN);
            }
        }
    }

    private int calculatePositionForInsetBounds() {
        this.mTmpRect.set(0, 0, this.mDisplayWidth, this.mDisplayHeight);
        this.mTmpRect.inset(this.mStableInsets);
        return DockedDividerUtils.calculatePositionForBounds(this.mTmpRect, this.mDockSide, this.mDividerSize);
    }
}
