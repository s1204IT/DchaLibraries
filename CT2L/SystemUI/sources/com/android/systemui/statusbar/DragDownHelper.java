package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.ExpandHelper;
import com.android.systemui.R;

public class DragDownHelper {
    private ExpandHelper.Callback mCallback;
    private DragDownCallback mDragDownCallback;
    private boolean mDraggedFarEnough;
    private boolean mDraggingDown;
    private View mHost;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private Interpolator mInterpolator;
    private float mLastHeight;
    private int mMinDragDistance;
    private ExpandableView mStartingChild;
    private final int[] mTemp2 = new int[2];
    private float mTouchSlop;

    public interface DragDownCallback {
        void onDragDownReset();

        boolean onDraggedDown(View view, int i);

        void onThresholdReached();

        void onTouchSlopExceeded();

        void setEmptyDragAmount(float f);
    }

    public DragDownHelper(Context context, View host, ExpandHelper.Callback callback, DragDownCallback dragDownCallback) {
        this.mMinDragDistance = context.getResources().getDimensionPixelSize(R.dimen.keyguard_drag_down_min_distance);
        this.mInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in);
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mCallback = callback;
        this.mDragDownCallback = dragDownCallback;
        this.mHost = host;
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
                break;
            case 2:
                float h = y - this.mInitialTouchY;
                if (h > this.mTouchSlop && h > Math.abs(x - this.mInitialTouchX)) {
                    this.mDraggingDown = true;
                    captureStartingChild(this.mInitialTouchX, this.mInitialTouchY);
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    this.mDragDownCallback.onTouchSlopExceeded();
                    return true;
                }
                break;
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mDraggingDown) {
            return false;
        }
        event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case 1:
                if (this.mDraggedFarEnough && this.mDragDownCallback.onDraggedDown(this.mStartingChild, (int) (y - this.mInitialTouchY))) {
                    if (this.mStartingChild == null) {
                        this.mDragDownCallback.setEmptyDragAmount(0.0f);
                    }
                    this.mDraggingDown = false;
                } else {
                    stopDragging();
                }
                break;
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
                        this.mDragDownCallback.onThresholdReached();
                    }
                } else if (this.mDraggedFarEnough) {
                    this.mDraggedFarEnough = false;
                    this.mDragDownCallback.onDragDownReset();
                }
                break;
            case 3:
                stopDragging();
                break;
        }
        return false;
    }

    private void captureStartingChild(float x, float y) {
        if (this.mStartingChild == null) {
            this.mStartingChild = findView(x, y);
            if (this.mStartingChild != null) {
                this.mCallback.setUserLockedChild(this.mStartingChild, true);
            }
        }
    }

    private void handleExpansion(float heightDelta, ExpandableView child) {
        if (heightDelta < 0.0f) {
            heightDelta = 0.0f;
        }
        boolean expandable = child.isContentExpandable();
        float rubberbandFactor = expandable ? 0.5f : 0.15f;
        float rubberband = heightDelta * rubberbandFactor;
        if (expandable && child.getMinHeight() + rubberband > child.getMaxHeight()) {
            float overshoot = (child.getMinHeight() + rubberband) - child.getMaxHeight();
            rubberband -= overshoot * 0.85f;
        }
        child.setActualHeight((int) (child.getMinHeight() + rubberband));
    }

    private void cancelExpansion(final ExpandableView child) {
        if (child.getActualHeight() != child.getMinHeight()) {
            ObjectAnimator anim = ObjectAnimator.ofInt(child, "actualHeight", child.getActualHeight(), child.getMinHeight());
            anim.setInterpolator(this.mInterpolator);
            anim.setDuration(375L);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    DragDownHelper.this.mCallback.setUserLockedChild(child, false);
                }
            });
            anim.start();
        }
    }

    private void cancelExpansion() {
        ValueAnimator anim = ValueAnimator.ofFloat(this.mLastHeight, 0.0f);
        anim.setInterpolator(this.mInterpolator);
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
