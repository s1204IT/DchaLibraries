package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.FloatProperty;
import android.util.Property;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.widget.OverScroller;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.views.lowram.TaskStackLowRamLayoutAlgorithm;
import com.android.systemui.shared.recents.utilities.AnimationProps;
import com.android.systemui.shared.recents.utilities.Utilities;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.io.PrintWriter;

public class TaskStackViewScroller {
    private static final Property<TaskStackViewScroller, Float> STACK_SCROLL = new FloatProperty<TaskStackViewScroller>("stackScroll") {
        @Override
        public void setValue(TaskStackViewScroller taskStackViewScroller, float f) {
            taskStackViewScroller.setStackScroll(f);
        }

        @Override
        public Float get(TaskStackViewScroller taskStackViewScroller) {
            return Float.valueOf(taskStackViewScroller.getStackScroll());
        }
    };
    TaskStackViewScrollerCallbacks mCb;
    Context mContext;
    float mFinalAnimatedScroll;
    final FlingAnimationUtils mFlingAnimationUtils;
    float mFlingDownScrollP;
    int mFlingDownY;

    @ViewDebug.ExportedProperty(category = "recents")
    float mLastDeltaP = 0.0f;
    TaskStackLayoutAlgorithm mLayoutAlgorithm;
    ObjectAnimator mScrollAnimator;
    OverScroller mScroller;

    @ViewDebug.ExportedProperty(category = "recents")
    float mStackScrollP;

    public interface TaskStackViewScrollerCallbacks {
        void onStackScrollChanged(float f, float f2, AnimationProps animationProps);
    }

    public TaskStackViewScroller(Context context, TaskStackViewScrollerCallbacks taskStackViewScrollerCallbacks, TaskStackLayoutAlgorithm taskStackLayoutAlgorithm) {
        this.mContext = context;
        this.mCb = taskStackViewScrollerCallbacks;
        this.mScroller = new OverScroller(context);
        if (Recents.getConfiguration().isLowRamDevice) {
            this.mScroller.setFriction(0.06f);
        }
        this.mLayoutAlgorithm = taskStackLayoutAlgorithm;
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.3f);
    }

    void reset() {
        this.mStackScrollP = 0.0f;
        this.mLastDeltaP = 0.0f;
    }

    void resetDeltaScroll() {
        this.mLastDeltaP = 0.0f;
    }

    public float getStackScroll() {
        return this.mStackScrollP;
    }

    public void setStackScroll(float f) {
        setStackScroll(f, AnimationProps.IMMEDIATE);
    }

    public float setDeltaStackScroll(float f, float f2) {
        float f3 = f + f2;
        float fUpdateFocusStateOnScroll = this.mLayoutAlgorithm.updateFocusStateOnScroll(f + this.mLastDeltaP, f3, this.mStackScrollP);
        setStackScroll(fUpdateFocusStateOnScroll, AnimationProps.IMMEDIATE);
        this.mLastDeltaP = f2;
        return fUpdateFocusStateOnScroll - f3;
    }

    public void setStackScroll(float f, AnimationProps animationProps) {
        float f2 = this.mStackScrollP;
        this.mStackScrollP = f;
        if (this.mCb != null) {
            this.mCb.onStackScrollChanged(f2, this.mStackScrollP, animationProps);
        }
    }

    public boolean setStackScrollToInitialState() {
        float f = this.mStackScrollP;
        setStackScroll(this.mLayoutAlgorithm.mInitialScrollP);
        return Float.compare(f, this.mStackScrollP) != 0;
    }

    public void fling(float f, int i, int i2, int i3, int i4, int i5, int i6) {
        this.mFlingDownScrollP = f;
        this.mFlingDownY = i;
        this.mScroller.fling(0, i2, 0, i3, 0, 0, i4, i5, 0, i6);
    }

    public boolean boundScroll() {
        float stackScroll = getStackScroll();
        float boundedStackScroll = getBoundedStackScroll(stackScroll);
        if (Float.compare(boundedStackScroll, stackScroll) != 0) {
            setStackScroll(boundedStackScroll);
            return true;
        }
        return false;
    }

    float getBoundedStackScroll(float f) {
        return Utilities.clamp(f, this.mLayoutAlgorithm.mMinScrollP, this.mLayoutAlgorithm.mMaxScrollP);
    }

    float getScrollAmountOutOfBounds(float f) {
        if (f < this.mLayoutAlgorithm.mMinScrollP) {
            return Math.abs(f - this.mLayoutAlgorithm.mMinScrollP);
        }
        if (f > this.mLayoutAlgorithm.mMaxScrollP) {
            return Math.abs(f - this.mLayoutAlgorithm.mMaxScrollP);
        }
        return 0.0f;
    }

    boolean isScrollOutOfBounds() {
        return Float.compare(getScrollAmountOutOfBounds(this.mStackScrollP), 0.0f) != 0;
    }

    void scrollToClosestTask(int i) {
        float stackScroll = getStackScroll();
        if (!Recents.getConfiguration().isLowRamDevice || stackScroll < this.mLayoutAlgorithm.mMinScrollP || stackScroll > this.mLayoutAlgorithm.mMaxScrollP) {
            return;
        }
        TaskStackLowRamLayoutAlgorithm taskStackLowRamLayoutAlgorithm = this.mLayoutAlgorithm.mTaskStackLowRamLayoutAlgorithm;
        if (Math.abs(i) > ViewConfiguration.get(this.mContext).getScaledMinimumFlingVelocity()) {
            fling(0.0f, 0, taskStackLowRamLayoutAlgorithm.percentageToScroll(stackScroll), -i, taskStackLowRamLayoutAlgorithm.percentageToScroll(this.mLayoutAlgorithm.mMinScrollP), taskStackLowRamLayoutAlgorithm.percentageToScroll(this.mLayoutAlgorithm.mMaxScrollP), 0);
            float closestTaskP = taskStackLowRamLayoutAlgorithm.getClosestTaskP(taskStackLowRamLayoutAlgorithm.scrollToPercentage(this.mScroller.getFinalY()), this.mLayoutAlgorithm.mNumStackTasks, i);
            ValueAnimator valueAnimatorOfFloat = ObjectAnimator.ofFloat(stackScroll, closestTaskP);
            this.mFlingAnimationUtils.apply(valueAnimatorOfFloat, taskStackLowRamLayoutAlgorithm.percentageToScroll(stackScroll), taskStackLowRamLayoutAlgorithm.percentageToScroll(closestTaskP), i);
            animateScroll(closestTaskP, (int) valueAnimatorOfFloat.getDuration(), valueAnimatorOfFloat.getInterpolator(), null);
            return;
        }
        animateScroll(taskStackLowRamLayoutAlgorithm.getClosestTaskP(stackScroll, this.mLayoutAlgorithm.mNumStackTasks, i), 300, Interpolators.ACCELERATE_DECELERATE, null);
    }

    ObjectAnimator animateBoundScroll() {
        float stackScroll = getStackScroll();
        float boundedStackScroll = getBoundedStackScroll(stackScroll);
        if (Float.compare(boundedStackScroll, stackScroll) != 0) {
            animateScroll(boundedStackScroll, null);
        }
        return this.mScrollAnimator;
    }

    void animateScroll(float f, Runnable runnable) {
        animateScroll(f, this.mContext.getResources().getInteger(R.integer.recents_animate_task_stack_scroll_duration), runnable);
    }

    void animateScroll(float f, int i, Runnable runnable) {
        animateScroll(f, i, Interpolators.LINEAR_OUT_SLOW_IN, runnable);
    }

    void animateScroll(float f, int i, TimeInterpolator timeInterpolator, Runnable runnable) {
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, STACK_SCROLL, getStackScroll(), f);
        objectAnimatorOfFloat.setDuration(i);
        objectAnimatorOfFloat.setInterpolator(timeInterpolator);
        animateScroll(f, objectAnimatorOfFloat, runnable);
    }

    private void animateScroll(float f, ObjectAnimator objectAnimator, final Runnable runnable) {
        if (this.mScrollAnimator != null && this.mScrollAnimator.isRunning()) {
            setStackScroll(this.mFinalAnimatedScroll);
            this.mScroller.forceFinished(true);
        }
        stopScroller();
        stopBoundScrollAnimation();
        if (Float.compare(this.mStackScrollP, f) != 0) {
            this.mFinalAnimatedScroll = f;
            this.mScrollAnimator = objectAnimator;
            this.mScrollAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    if (runnable != null) {
                        runnable.run();
                    }
                    TaskStackViewScroller.this.mScrollAnimator.removeAllListeners();
                }
            });
            this.mScrollAnimator.start();
            return;
        }
        if (runnable != null) {
            runnable.run();
        }
    }

    void stopBoundScrollAnimation() {
        Utilities.cancelAnimationWithoutCallbacks(this.mScrollAnimator);
    }

    boolean computeScroll() {
        if (this.mScroller.computeScrollOffset()) {
            this.mFlingDownScrollP += setDeltaStackScroll(this.mFlingDownScrollP, this.mLayoutAlgorithm.getDeltaPForY(this.mFlingDownY, this.mScroller.getCurrY()));
            return true;
        }
        return false;
    }

    float getScrollVelocity() {
        return this.mScroller.getCurrVelocity();
    }

    void stopScroller() {
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("TaskStackViewScroller");
        printWriter.print(" stackScroll:");
        printWriter.print(this.mStackScrollP);
        printWriter.println();
    }
}
