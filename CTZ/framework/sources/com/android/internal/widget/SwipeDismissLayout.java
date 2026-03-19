package com.android.internal.widget;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import com.android.internal.R;
import com.android.internal.widget.SwipeDismissLayout;

public class SwipeDismissLayout extends FrameLayout {
    private static final float MAX_DIST_THRESHOLD = 0.33f;
    private static final float MIN_DIST_THRESHOLD = 0.1f;
    private static final String TAG = "SwipeDismissLayout";
    private int mActiveTouchId;
    private boolean mActivityTranslucencyConverted;
    private boolean mBlockGesture;
    private boolean mDiscardIntercept;
    private final DismissAnimator mDismissAnimator;
    private boolean mDismissable;
    private boolean mDismissed;
    private OnDismissedListener mDismissedListener;
    private float mDownX;
    private float mDownY;
    private boolean mIsWindowNativelyTranslucent;
    private float mLastX;
    private int mMinFlingVelocity;
    private OnSwipeProgressChangedListener mProgressListener;
    private IntentFilter mScreenOffFilter;
    private BroadcastReceiver mScreenOffReceiver;
    private int mSlop;
    private boolean mSwiping;
    private VelocityTracker mVelocityTracker;

    public interface OnDismissedListener {
        void onDismissed(SwipeDismissLayout swipeDismissLayout);
    }

    public interface OnSwipeProgressChangedListener {
        void onSwipeCancelled(SwipeDismissLayout swipeDismissLayout);

        void onSwipeProgressChanged(SwipeDismissLayout swipeDismissLayout, float f, float f2);
    }

    public SwipeDismissLayout(Context context) {
        super(context);
        this.mBlockGesture = false;
        this.mActivityTranslucencyConverted = false;
        this.mDismissAnimator = new DismissAnimator();
        this.mScreenOffFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        this.mDismissable = true;
        init(context);
    }

    public SwipeDismissLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBlockGesture = false;
        this.mActivityTranslucencyConverted = false;
        this.mDismissAnimator = new DismissAnimator();
        this.mScreenOffFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        this.mDismissable = true;
        init(context);
    }

    public SwipeDismissLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mBlockGesture = false;
        this.mActivityTranslucencyConverted = false;
        this.mDismissAnimator = new DismissAnimator();
        this.mScreenOffFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        this.mDismissable = true;
        init(context);
    }

    private void init(Context context) {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mSlop = viewConfiguration.getScaledTouchSlop();
        this.mMinFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        TypedArray typedArrayObtainStyledAttributes = context.getTheme().obtainStyledAttributes(R.styleable.Theme);
        this.mIsWindowNativelyTranslucent = typedArrayObtainStyledAttributes.getBoolean(5, false);
        typedArrayObtainStyledAttributes.recycle();
    }

    public void setOnDismissedListener(OnDismissedListener onDismissedListener) {
        this.mDismissedListener = onDismissedListener;
    }

    public void setOnSwipeProgressChangedListener(OnSwipeProgressChangedListener onSwipeProgressChangedListener) {
        this.mProgressListener = onSwipeProgressChangedListener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            this.mScreenOffReceiver = new AnonymousClass1();
            getContext().registerReceiver(this.mScreenOffReceiver, this.mScreenOffFilter);
        } catch (ReceiverCallNotAllowedException e) {
            this.mScreenOffReceiver = null;
        }
    }

    class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SwipeDismissLayout.this.post(new Runnable() {
                @Override
                public final void run() {
                    SwipeDismissLayout.AnonymousClass1.lambda$onReceive$0(this.f$0);
                }
            });
        }

        public static void lambda$onReceive$0(AnonymousClass1 anonymousClass1) {
            if (SwipeDismissLayout.this.mDismissed) {
                SwipeDismissLayout.this.dismiss();
            } else {
                SwipeDismissLayout.this.cancel();
            }
            SwipeDismissLayout.this.resetMembers();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this.mScreenOffReceiver != null) {
            getContext().unregisterReceiver(this.mScreenOffReceiver);
            this.mScreenOffReceiver = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        checkGesture(motionEvent);
        if (this.mBlockGesture) {
            return true;
        }
        if (!this.mDismissable) {
            return super.onInterceptTouchEvent(motionEvent);
        }
        motionEvent.offsetLocation(motionEvent.getRawX() - motionEvent.getX(), 0.0f);
        switch (motionEvent.getActionMasked()) {
            case 0:
                resetMembers();
                this.mDownX = motionEvent.getRawX();
                this.mDownY = motionEvent.getRawY();
                this.mActiveTouchId = motionEvent.getPointerId(0);
                this.mVelocityTracker = VelocityTracker.obtain("int1");
                this.mVelocityTracker.addMovement(motionEvent);
                break;
            case 1:
            case 3:
                resetMembers();
                break;
            case 2:
                if (this.mVelocityTracker != null && !this.mDiscardIntercept) {
                    int iFindPointerIndex = motionEvent.findPointerIndex(this.mActiveTouchId);
                    if (iFindPointerIndex == -1) {
                        Log.e(TAG, "Invalid pointer index: ignoring.");
                        this.mDiscardIntercept = true;
                    } else {
                        float rawX = motionEvent.getRawX() - this.mDownX;
                        float x = motionEvent.getX(iFindPointerIndex);
                        float y = motionEvent.getY(iFindPointerIndex);
                        if (rawX != 0.0f && canScroll(this, false, rawX, x, y)) {
                            this.mDiscardIntercept = true;
                        } else {
                            updateSwiping(motionEvent);
                        }
                    }
                }
                break;
            case 5:
                this.mActiveTouchId = motionEvent.getPointerId(motionEvent.getActionIndex());
                break;
            case 6:
                int actionIndex = motionEvent.getActionIndex();
                if (motionEvent.getPointerId(actionIndex) == this.mActiveTouchId) {
                    this.mActiveTouchId = motionEvent.getPointerId(actionIndex == 0 ? 1 : 0);
                }
                break;
        }
        return !this.mDiscardIntercept && this.mSwiping;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        checkGesture(motionEvent);
        if (this.mBlockGesture) {
            return true;
        }
        if (this.mVelocityTracker == null || !this.mDismissable) {
            return super.onTouchEvent(motionEvent);
        }
        motionEvent.offsetLocation(motionEvent.getRawX() - motionEvent.getX(), 0.0f);
        switch (motionEvent.getActionMasked()) {
            case 1:
                updateDismiss(motionEvent);
                if (this.mDismissed) {
                    this.mDismissAnimator.animateDismissal(motionEvent.getRawX() - this.mDownX);
                } else if (this.mSwiping && this.mLastX != -2.1474836E9f) {
                    this.mDismissAnimator.animateRecovery(motionEvent.getRawX() - this.mDownX);
                }
                resetMembers();
                return true;
            case 2:
                this.mVelocityTracker.addMovement(motionEvent);
                this.mLastX = motionEvent.getRawX();
                updateSwiping(motionEvent);
                if (this.mSwiping) {
                    setProgress(motionEvent.getRawX() - this.mDownX);
                }
                return true;
            case 3:
                cancel();
                resetMembers();
                return true;
            default:
                return true;
        }
    }

    private void setProgress(float f) {
        if (this.mProgressListener != null && f >= 0.0f) {
            this.mProgressListener.onSwipeProgressChanged(this, progressToAlpha(f / getWidth()), f);
        }
    }

    private void dismiss() {
        if (this.mDismissedListener != null) {
            this.mDismissedListener.onDismissed(this);
        }
    }

    protected void cancel() {
        Activity activityFindActivity;
        if (!this.mIsWindowNativelyTranslucent && (activityFindActivity = findActivity()) != null && this.mActivityTranslucencyConverted) {
            activityFindActivity.convertFromTranslucent();
            this.mActivityTranslucencyConverted = false;
        }
        if (this.mProgressListener != null) {
            this.mProgressListener.onSwipeCancelled(this);
        }
    }

    private void resetMembers() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = null;
        this.mDownX = 0.0f;
        this.mLastX = -2.1474836E9f;
        this.mDownY = 0.0f;
        this.mSwiping = false;
        this.mDismissed = false;
        this.mDiscardIntercept = false;
    }

    private void updateSwiping(MotionEvent motionEvent) {
        Activity activityFindActivity;
        boolean z = this.mSwiping;
        if (!this.mSwiping) {
            float rawX = motionEvent.getRawX() - this.mDownX;
            float rawY = motionEvent.getRawY() - this.mDownY;
            boolean z2 = false;
            if ((rawX * rawX) + (rawY * rawY) > this.mSlop * this.mSlop) {
                if (rawX > this.mSlop * 2 && Math.abs(rawY) < Math.abs(rawX)) {
                    z2 = true;
                }
                this.mSwiping = z2;
            } else {
                this.mSwiping = false;
            }
        }
        if (this.mSwiping && !z && !this.mIsWindowNativelyTranslucent && (activityFindActivity = findActivity()) != null) {
            this.mActivityTranslucencyConverted = activityFindActivity.convertToTranslucent(null, null);
        }
    }

    private void updateDismiss(MotionEvent motionEvent) {
        float rawX = motionEvent.getRawX() - this.mDownX;
        this.mVelocityTracker.computeCurrentVelocity(1000);
        float xVelocity = this.mVelocityTracker.getXVelocity();
        if (this.mLastX == -2.1474836E9f) {
            xVelocity = rawX / ((motionEvent.getEventTime() - motionEvent.getDownTime()) / 1000);
        }
        if (!this.mDismissed && ((rawX > getWidth() * Math.max(Math.min((((-0.23000002f) * xVelocity) / this.mMinFlingVelocity) + MAX_DIST_THRESHOLD, MAX_DIST_THRESHOLD), MIN_DIST_THRESHOLD) && motionEvent.getRawX() >= this.mLastX) || xVelocity >= this.mMinFlingVelocity)) {
            this.mDismissed = true;
        }
        if (this.mDismissed && this.mSwiping && xVelocity < (-this.mMinFlingVelocity)) {
            this.mDismissed = false;
        }
    }

    protected boolean canScroll(View view, boolean z, float f, float f2, float f3) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int scrollX = view.getScrollX();
            int scrollY = view.getScrollY();
            for (int childCount = viewGroup.getChildCount() - 1; childCount >= 0; childCount--) {
                View childAt = viewGroup.getChildAt(childCount);
                float f4 = f2 + scrollX;
                if (f4 >= childAt.getLeft() && f4 < childAt.getRight()) {
                    float f5 = f3 + scrollY;
                    if (f5 >= childAt.getTop() && f5 < childAt.getBottom() && canScroll(childAt, true, f, f4 - childAt.getLeft(), f5 - childAt.getTop())) {
                        return true;
                    }
                }
            }
        }
        return z && view.canScrollHorizontally((int) (-f));
    }

    public void setDismissable(boolean z) {
        if (!z && this.mDismissable) {
            cancel();
            resetMembers();
        }
        this.mDismissable = z;
    }

    private void checkGesture(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0) {
            this.mBlockGesture = this.mDismissAnimator.isAnimating();
        }
    }

    private float progressToAlpha(float f) {
        return 1.0f - ((f * f) * f);
    }

    private Activity findActivity() {
        for (Context context = getContext(); context instanceof ContextWrapper; context = ((ContextWrapper) context).getBaseContext()) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
        }
        return null;
    }

    private class DismissAnimator implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        private final TimeInterpolator DISMISS_INTERPOLATOR = new DecelerateInterpolator(1.5f);
        private final long DISMISS_DURATION = 250;
        private final ValueAnimator mDismissAnimator = new ValueAnimator();
        private boolean mWasCanceled = false;
        private boolean mDismissOnComplete = false;

        DismissAnimator() {
            this.mDismissAnimator.addUpdateListener(this);
            this.mDismissAnimator.addListener(this);
        }

        void animateDismissal(float f) {
            animate(f / SwipeDismissLayout.this.getWidth(), 1.0f, 250L, this.DISMISS_INTERPOLATOR, true);
        }

        void animateRecovery(float f) {
            animate(f / SwipeDismissLayout.this.getWidth(), 0.0f, 250L, this.DISMISS_INTERPOLATOR, false);
        }

        boolean isAnimating() {
            return this.mDismissAnimator.isStarted();
        }

        private void animate(float f, float f2, long j, TimeInterpolator timeInterpolator, boolean z) {
            this.mDismissAnimator.cancel();
            this.mDismissOnComplete = z;
            this.mDismissAnimator.setFloatValues(f, f2);
            this.mDismissAnimator.setDuration(j);
            this.mDismissAnimator.setInterpolator(timeInterpolator);
            this.mDismissAnimator.start();
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            SwipeDismissLayout.this.setProgress(((Float) valueAnimator.getAnimatedValue()).floatValue() * SwipeDismissLayout.this.getWidth());
        }

        @Override
        public void onAnimationStart(Animator animator) {
            this.mWasCanceled = false;
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            this.mWasCanceled = true;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (!this.mWasCanceled) {
                if (this.mDismissOnComplete) {
                    SwipeDismissLayout.this.dismiss();
                } else {
                    SwipeDismissLayout.this.cancel();
                }
            }
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }
    }
}
