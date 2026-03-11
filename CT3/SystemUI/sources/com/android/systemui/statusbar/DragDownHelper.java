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
        void onCrossedThreshold(boolean z);

        void onDragDownReset();

        boolean onDraggedDown(View view, int i);

        void onTouchSlopExceeded();

        void setEmptyDragAmount(float f);
    }

    public DragDownHelper(Context context, View host, ExpandHelper.Callback callback, DragDownCallback dragDownCallback) {
        this.mMinDragDistance = context.getResources().getDimensionPixelSize(R.dimen.keyguard_drag_down_min_distance);
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mCallback = callback;
        this.mDragDownCallback = dragDownCallback;
        this.mHost = host;
        this.mFalsingManager = FalsingManager.getInstance(context);
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case 0:
                this.mDraggedFarEnough = false;
                this.mDraggingDown = false;
                this.mStartingChild = null;
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                return false;
            case 1:
            default:
                return false;
            case 2:
                float h = y - this.mInitialTouchY;
                if (h > this.mTouchSlop && h > Math.abs(x - this.mInitialTouchX)) {
                    this.mFalsingManager.onNotificatonStartDraggingDown();
                    this.mDraggingDown = true;
                    captureStartingChild(this.mInitialTouchX, this.mInitialTouchY);
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    this.mDragDownCallback.onTouchSlopExceeded();
                    return true;
                }
                return false;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mDraggingDown) {
            return false;
        }
        event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case 1:
                if (!isFalseTouch() && this.mDragDownCallback.onDraggedDown(this.mStartingChild, (int) (y - this.mInitialTouchY))) {
                    if (this.mStartingChild == null) {
                        this.mDragDownCallback.setEmptyDragAmount(0.0f);
                    } else {
                        this.mCallback.setUserLockedChild(this.mStartingChild, false);
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
        return this.mFalsingManager.isFalseTouch() || !this.mDraggedFarEnough;
    }

    private void captureStartingChild(float x, float y) {
        if (this.mStartingChild != null) {
            return;
        }
        this.mStartingChild = findView(x, y);
        if (this.mStartingChild == null) {
            return;
        }
        this.mCallback.setUserLockedChild(this.mStartingChild, true);
    }

    private void handleExpansion(float heightDelta, ExpandableView child) {
        float rubberbandFactor;
        if (heightDelta < 0.0f) {
            heightDelta = 0.0f;
        }
        boolean expandable = child.isContentExpandable();
        if (expandable) {
            rubberbandFactor = 0.5f;
        } else {
            rubberbandFactor = 0.15f;
        }
        float rubberband = heightDelta * rubberbandFactor;
        if (expandable && child.getCollapsedHeight() + rubberband > child.getMaxContentHeight()) {
            float overshoot = (child.getCollapsedHeight() + rubberband) - child.getMaxContentHeight();
            rubberband -= overshoot * 0.85f;
        }
        child.setActualHeight((int) (child.getCollapsedHeight() + rubberband));
    }

    private void cancelExpansion(final ExpandableView child) {
        if (child.getActualHeight() == child.getCollapsedHeight()) {
            this.mCallback.setUserLockedChild(child, false);
            return;
        }
        ObjectAnimator anim = ObjectAnimator.ofInt(child, "actualHeight", child.getActualHeight(), child.getCollapsedHeight());
        anim.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        anim.setDuration(375L);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                DragDownHelper.this.mCallback.setUserLockedChild(child, false);
            }
        });
        anim.start();
    }

    private void cancelExpansion() {
        ValueAnimator anim = ValueAnimator.ofFloat(this.mLastHeight, 0.0f);
        anim.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        anim.setDuration(375L);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                DragDownHelper.this.mDragDownCallback.setEmptyDragAmount(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        anim.start();
    }

    private void stopDragging() {
        this.mFalsingManager.onNotificatonStopDraggingDown();
        if (this.mStartingChild != null) {
            cancelExpansion(this.mStartingChild);
        } else {
            cancelExpansion();
        }
        this.mDraggingDown = false;
        this.mDragDownCallback.onDragDownReset();
    }

    private ExpandableView findView(float x, float y) {
        this.mHost.getLocationOnScreen(this.mTemp2);
        return this.mCallback.getChildAtRawPosition(x + this.mTemp2[0], y + this.mTemp2[1]);
    }
}
