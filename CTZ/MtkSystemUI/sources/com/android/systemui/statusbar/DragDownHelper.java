package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.ExpandHelper;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;

public class DragDownHelper {
    private ExpandHelper.Callback mCallback;
    private DragDownCallback mDragDownCallback;
    private boolean mDraggedFarEnough;
    private boolean mDraggingDown;
    private FalsingManager mFalsingManager;
    private View mHost;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private float mLastHeight;
    private int mMinDragDistance;
    private ExpandableView mStartingChild;
    private final int[] mTemp2 = new int[2];
    private float mTouchSlop;

    public interface DragDownCallback {
        boolean isFalsingCheckNeeded();

        void onCrossedThreshold(boolean z);

        void onDragDownReset();

        boolean onDraggedDown(View view, int i);

        void onTouchSlopExceeded();

        void setEmptyDragAmount(float f);
    }

    public DragDownHelper(Context context, View view, ExpandHelper.Callback callback, DragDownCallback dragDownCallback) {
        this.mMinDragDistance = context.getResources().getDimensionPixelSize(R.dimen.keyguard_drag_down_min_distance);
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mCallback = callback;
        this.mDragDownCallback = dragDownCallback;
        this.mHost = view;
        this.mFalsingManager = FalsingManager.getInstance(context);
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mDraggedFarEnough = false;
            this.mDraggingDown = false;
            this.mStartingChild = null;
            this.mInitialTouchY = y;
            this.mInitialTouchX = x;
        } else if (actionMasked == 2) {
            float f = y - this.mInitialTouchY;
            if (f > this.mTouchSlop && f > Math.abs(x - this.mInitialTouchX)) {
                this.mFalsingManager.onNotificatonStartDraggingDown();
                this.mDraggingDown = true;
                captureStartingChild(this.mInitialTouchX, this.mInitialTouchY);
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                this.mDragDownCallback.onTouchSlopExceeded();
                return true;
            }
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mDraggingDown) {
            return false;
        }
        motionEvent.getX();
        float y = motionEvent.getY();
        switch (motionEvent.getActionMasked()) {
            case 1:
                if (!isFalseTouch() && this.mDragDownCallback.onDraggedDown(this.mStartingChild, (int) (y - this.mInitialTouchY))) {
                    if (this.mStartingChild == null) {
                        this.mDragDownCallback.setEmptyDragAmount(0.0f);
                    } else {
                        this.mCallback.setUserLockedChild(this.mStartingChild, false);
                        this.mStartingChild = null;
                    }
                    this.mDraggingDown = false;
                    return false;
                }
                stopDragging();
                return false;
            case 2:
                this.mLastHeight = y - this.mInitialTouchY;
                captureStartingChild(this.mInitialTouchX, this.mInitialTouchY);
                if (this.mStartingChild != null) {
                    handleExpansion(this.mLastHeight, this.mStartingChild);
                } else {
                    this.mDragDownCallback.setEmptyDragAmount(this.mLastHeight);
                }
                if (this.mLastHeight > this.mMinDragDistance) {
                    if (!this.mDraggedFarEnough) {
                        this.mDraggedFarEnough = true;
                        this.mDragDownCallback.onCrossedThreshold(true);
                    }
                } else if (this.mDraggedFarEnough) {
                    this.mDraggedFarEnough = false;
                    this.mDragDownCallback.onCrossedThreshold(false);
                }
                return true;
            case 3:
                stopDragging();
                return false;
            default:
                return false;
        }
    }

    private boolean isFalseTouch() {
        if (this.mDragDownCallback.isFalsingCheckNeeded()) {
            return this.mFalsingManager.isFalseTouch() || !this.mDraggedFarEnough;
        }
        return false;
    }

    private void captureStartingChild(float f, float f2) {
        if (this.mStartingChild == null) {
            this.mStartingChild = findView(f, f2);
            if (this.mStartingChild != null) {
                this.mCallback.setUserLockedChild(this.mStartingChild, true);
            }
        }
    }

    private void handleExpansion(float f, ExpandableView expandableView) {
        float f2;
        if (f < 0.0f) {
            f = 0.0f;
        }
        boolean zIsContentExpandable = expandableView.isContentExpandable();
        if (zIsContentExpandable) {
            f2 = 0.5f;
        } else {
            f2 = 0.15f;
        }
        float collapsedHeight = f * f2;
        if (zIsContentExpandable && expandableView.getCollapsedHeight() + collapsedHeight > expandableView.getMaxContentHeight()) {
            collapsedHeight -= ((expandableView.getCollapsedHeight() + collapsedHeight) - expandableView.getMaxContentHeight()) * 0.85f;
        }
        expandableView.setActualHeight((int) (expandableView.getCollapsedHeight() + collapsedHeight));
    }

    private void cancelExpansion(final ExpandableView expandableView) {
        if (expandableView.getActualHeight() == expandableView.getCollapsedHeight()) {
            this.mCallback.setUserLockedChild(expandableView, false);
            return;
        }
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(expandableView, "actualHeight", expandableView.getActualHeight(), expandableView.getCollapsedHeight());
        objectAnimatorOfInt.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        objectAnimatorOfInt.setDuration(375L);
        objectAnimatorOfInt.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                DragDownHelper.this.mCallback.setUserLockedChild(expandableView, false);
            }
        });
        objectAnimatorOfInt.start();
    }

    private void cancelExpansion() {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.mLastHeight, 0.0f);
        valueAnimatorOfFloat.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        valueAnimatorOfFloat.setDuration(375L);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                DragDownHelper.this.mDragDownCallback.setEmptyDragAmount(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        valueAnimatorOfFloat.start();
    }

    private void stopDragging() {
        this.mFalsingManager.onNotificatonStopDraggingDown();
        if (this.mStartingChild != null) {
            cancelExpansion(this.mStartingChild);
            this.mStartingChild = null;
        } else {
            cancelExpansion();
        }
        this.mDraggingDown = false;
        this.mDragDownCallback.onDragDownReset();
    }

    private ExpandableView findView(float f, float f2) {
        this.mHost.getLocationOnScreen(this.mTemp2);
        return this.mCallback.getChildAtRawPosition(f + this.mTemp2[0], f2 + this.mTemp2[1]);
    }

    public boolean isDraggingDown() {
        return this.mDraggingDown;
    }
}
