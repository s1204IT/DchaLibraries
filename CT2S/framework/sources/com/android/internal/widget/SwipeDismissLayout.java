package com.android.internal.widget;

import android.animation.TimeInterpolator;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

public class SwipeDismissLayout extends FrameLayout {
    private static final float DISMISS_MIN_DRAG_WIDTH_RATIO = 0.33f;
    private static final String TAG = "SwipeDismissLayout";
    private int mActiveTouchId;
    private long mAnimationTime;
    private TimeInterpolator mCancelInterpolator;
    private boolean mDiscardIntercept;
    private TimeInterpolator mDismissInterpolator;
    private boolean mDismissed;
    private OnDismissedListener mDismissedListener;
    private float mDownX;
    private float mDownY;
    private float mLastX;
    private int mMaxFlingVelocity;
    private int mMinFlingVelocity;
    private ViewTreeObserver.OnEnterAnimationCompleteListener mOnEnterAnimationCompleteListener;
    private OnSwipeProgressChangedListener mProgressListener;
    private int mSlop;
    private boolean mSwiping;
    private float mTranslationX;
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
        this.mOnEnterAnimationCompleteListener = new ViewTreeObserver.OnEnterAnimationCompleteListener() {
            @Override
            public void onEnterAnimationComplete() {
                if (SwipeDismissLayout.this.getContext() instanceof Activity) {
                    ((Activity) SwipeDismissLayout.this.getContext()).convertFromTranslucent();
                }
            }
        };
        init(context);
    }

    public SwipeDismissLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mOnEnterAnimationCompleteListener = new ViewTreeObserver.OnEnterAnimationCompleteListener() {
            @Override
            public void onEnterAnimationComplete() {
                if (SwipeDismissLayout.this.getContext() instanceof Activity) {
                    ((Activity) SwipeDismissLayout.this.getContext()).convertFromTranslucent();
                }
            }
        };
        init(context);
    }

    public SwipeDismissLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mOnEnterAnimationCompleteListener = new ViewTreeObserver.OnEnterAnimationCompleteListener() {
            @Override
            public void onEnterAnimationComplete() {
                if (SwipeDismissLayout.this.getContext() instanceof Activity) {
                    ((Activity) SwipeDismissLayout.this.getContext()).convertFromTranslucent();
                }
            }
        };
        init(context);
    }

    private void init(Context context) {
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        this.mSlop = vc.getScaledTouchSlop();
        this.mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        this.mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        this.mAnimationTime = getContext().getResources().getInteger(17694720);
        this.mCancelInterpolator = new DecelerateInterpolator(1.5f);
        this.mDismissInterpolator = new AccelerateInterpolator(1.5f);
    }

    public void setOnDismissedListener(OnDismissedListener listener) {
        this.mDismissedListener = listener;
    }

    public void setOnSwipeProgressChangedListener(OnSwipeProgressChangedListener listener) {
        this.mProgressListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getContext() instanceof Activity) {
            getViewTreeObserver().addOnEnterAnimationCompleteListener(this.mOnEnterAnimationCompleteListener);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (getContext() instanceof Activity) {
            getViewTreeObserver().removeOnEnterAnimationCompleteListener(this.mOnEnterAnimationCompleteListener);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ev.offsetLocation(this.mTranslationX, 0.0f);
        switch (ev.getActionMasked()) {
            case 0:
                resetMembers();
                this.mDownX = ev.getRawX();
                this.mDownY = ev.getRawY();
                this.mActiveTouchId = ev.getPointerId(0);
                this.mVelocityTracker = VelocityTracker.obtain();
                this.mVelocityTracker.addMovement(ev);
                break;
            case 1:
            case 3:
                resetMembers();
                break;
            case 2:
                if (this.mVelocityTracker != null && !this.mDiscardIntercept) {
                    int pointerIndex = ev.findPointerIndex(this.mActiveTouchId);
                    if (pointerIndex == -1) {
                        Log.e(TAG, "Invalid pointer index: ignoring.");
                        this.mDiscardIntercept = true;
                    } else {
                        float dx = ev.getRawX() - this.mDownX;
                        float x = ev.getX(pointerIndex);
                        float y = ev.getY(pointerIndex);
                        if (dx != 0.0f && canScroll(this, false, dx, x, y)) {
                            this.mDiscardIntercept = true;
                        } else {
                            updateSwiping(ev);
                        }
                    }
                }
                break;
            case 5:
                this.mActiveTouchId = ev.getPointerId(ev.getActionIndex());
                break;
            case 6:
                int actionIndex = ev.getActionIndex();
                int pointerId = ev.getPointerId(actionIndex);
                if (pointerId == this.mActiveTouchId) {
                    int newActionIndex = actionIndex == 0 ? 1 : 0;
                    this.mActiveTouchId = ev.getPointerId(newActionIndex);
                }
                break;
        }
        return !this.mDiscardIntercept && this.mSwiping;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mVelocityTracker == null) {
            return super.onTouchEvent(ev);
        }
        switch (ev.getActionMasked()) {
            case 1:
                updateDismiss(ev);
                if (this.mDismissed) {
                    dismiss();
                } else if (this.mSwiping) {
                    cancel();
                }
                resetMembers();
                return true;
            case 2:
                this.mVelocityTracker.addMovement(ev);
                this.mLastX = ev.getRawX();
                updateSwiping(ev);
                if (this.mSwiping) {
                    if (getContext() instanceof Activity) {
                        ((Activity) getContext()).convertToTranslucent(null, null);
                    }
                    setProgress(ev.getRawX() - this.mDownX);
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

    private void setProgress(float deltaX) {
        this.mTranslationX = deltaX;
        if (this.mProgressListener != null && deltaX >= 0.0f) {
            this.mProgressListener.onSwipeProgressChanged(this, deltaX / getWidth(), deltaX);
        }
    }

    private void dismiss() {
        if (this.mDismissedListener != null) {
            this.mDismissedListener.onDismissed(this);
        }
    }

    protected void cancel() {
        if (getContext() instanceof Activity) {
            ((Activity) getContext()).convertFromTranslucent();
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
        this.mTranslationX = 0.0f;
        this.mDownX = 0.0f;
        this.mDownY = 0.0f;
        this.mSwiping = false;
        this.mDismissed = false;
        this.mDiscardIntercept = false;
    }

    private void updateSwiping(MotionEvent ev) {
        boolean z = false;
        if (!this.mSwiping) {
            float deltaX = ev.getRawX() - this.mDownX;
            float deltaY = ev.getRawY() - this.mDownY;
            if ((deltaX * deltaX) + (deltaY * deltaY) <= this.mSlop * this.mSlop) {
                this.mSwiping = false;
                return;
            }
            if (deltaX > this.mSlop * 2 && Math.abs(deltaY) < this.mSlop * 2) {
                z = true;
            }
            this.mSwiping = z;
        }
    }

    private void updateDismiss(MotionEvent ev) {
        float deltaX = ev.getRawX() - this.mDownX;
        if (!this.mDismissed) {
            this.mVelocityTracker.addMovement(ev);
            this.mVelocityTracker.computeCurrentVelocity(1000);
            if (deltaX > getWidth() * DISMISS_MIN_DRAG_WIDTH_RATIO && ev.getRawX() >= this.mLastX) {
                this.mDismissed = true;
            }
        }
        if (this.mDismissed && this.mSwiping && deltaX < getWidth() * DISMISS_MIN_DRAG_WIDTH_RATIO) {
            this.mDismissed = false;
        }
    }

    protected boolean canScroll(View v, boolean checkV, float dx, float x, float y) {
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            int scrollX = v.getScrollX();
            int scrollY = v.getScrollY();
            int count = group.getChildCount();
            for (int i = count - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                if (scrollX + x >= child.getLeft() && scrollX + x < child.getRight() && scrollY + y >= child.getTop() && scrollY + y < child.getBottom() && canScroll(child, true, dx, (scrollX + x) - child.getLeft(), (scrollY + y) - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && v.canScrollHorizontally((int) (-dx));
    }
}
