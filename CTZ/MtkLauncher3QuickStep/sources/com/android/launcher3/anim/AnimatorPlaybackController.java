package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class AnimatorPlaybackController implements ValueAnimator.AnimatorUpdateListener {
    protected final AnimatorSet mAnim;
    protected float mCurrentFraction;
    private final long mDuration;
    private Runnable mEndAction;
    protected Runnable mOnCancelRunnable;
    protected boolean mTargetCancelled = false;
    private final ValueAnimator mAnimationPlayer = ValueAnimator.ofFloat(0.0f, 1.0f);

    public abstract void setPlayFraction(float f);

    public static AnimatorPlaybackController wrap(AnimatorSet animatorSet, long j) {
        return wrap(animatorSet, j, null);
    }

    public static AnimatorPlaybackController wrap(AnimatorSet animatorSet, long j, Runnable runnable) {
        return new AnimatorPlaybackControllerVL(animatorSet, j, runnable);
    }

    protected AnimatorPlaybackController(AnimatorSet animatorSet, long j, Runnable runnable) {
        this.mAnim = animatorSet;
        this.mDuration = j;
        this.mOnCancelRunnable = runnable;
        this.mAnimationPlayer.setInterpolator(Interpolators.LINEAR);
        this.mAnimationPlayer.addListener(new OnAnimationEndDispatcher());
        this.mAnimationPlayer.addUpdateListener(this);
        this.mAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animator) {
                AnimatorPlaybackController.this.mTargetCancelled = true;
                if (AnimatorPlaybackController.this.mOnCancelRunnable != null) {
                    AnimatorPlaybackController.this.mOnCancelRunnable.run();
                    AnimatorPlaybackController.this.mOnCancelRunnable = null;
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                AnimatorPlaybackController.this.mTargetCancelled = false;
                AnimatorPlaybackController.this.mOnCancelRunnable = null;
            }

            @Override
            public void onAnimationStart(Animator animator) {
                AnimatorPlaybackController.this.mTargetCancelled = false;
            }
        });
    }

    public AnimatorSet getTarget() {
        return this.mAnim;
    }

    public long getDuration() {
        return this.mDuration;
    }

    public void start() {
        this.mAnimationPlayer.setFloatValues(this.mCurrentFraction, 1.0f);
        this.mAnimationPlayer.setDuration(clampDuration(1.0f - this.mCurrentFraction));
        this.mAnimationPlayer.start();
    }

    public void reverse() {
        this.mAnimationPlayer.setFloatValues(this.mCurrentFraction, 0.0f);
        this.mAnimationPlayer.setDuration(clampDuration(this.mCurrentFraction));
        this.mAnimationPlayer.start();
    }

    public void pause() {
        this.mAnimationPlayer.cancel();
    }

    public ValueAnimator getAnimationPlayer() {
        return this.mAnimationPlayer;
    }

    public float getProgressFraction() {
        return this.mCurrentFraction;
    }

    public void setEndAction(Runnable runnable) {
        this.mEndAction = runnable;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        setPlayFraction(((Float) valueAnimator.getAnimatedValue()).floatValue());
    }

    protected long clampDuration(float f) {
        float f2 = this.mDuration * f;
        if (f2 <= 0.0f) {
            return 0L;
        }
        return Math.min((long) f2, this.mDuration);
    }

    public void dispatchOnStart() {
        dispatchOnStartRecursively(this.mAnim);
    }

    private void dispatchOnStartRecursively(Animator animator) {
        Iterator it = nonNullList(animator.getListeners()).iterator();
        while (it.hasNext()) {
            ((Animator.AnimatorListener) it.next()).onAnimationStart(animator);
        }
        if (animator instanceof AnimatorSet) {
            Iterator it2 = nonNullList(((AnimatorSet) animator).getChildAnimations()).iterator();
            while (it2.hasNext()) {
                dispatchOnStartRecursively((Animator) it2.next());
            }
        }
    }

    public void dispatchOnCancel() {
        dispatchOnCancelRecursively(this.mAnim);
    }

    private void dispatchOnCancelRecursively(Animator animator) {
        Iterator it = nonNullList(animator.getListeners()).iterator();
        while (it.hasNext()) {
            ((Animator.AnimatorListener) it.next()).onAnimationCancel(animator);
        }
        if (animator instanceof AnimatorSet) {
            Iterator it2 = nonNullList(((AnimatorSet) animator).getChildAnimations()).iterator();
            while (it2.hasNext()) {
                dispatchOnCancelRecursively((Animator) it2.next());
            }
        }
    }

    public void setOnCancelRunnable(Runnable runnable) {
        this.mOnCancelRunnable = runnable;
    }

    public Runnable getOnCancelRunnable() {
        return this.mOnCancelRunnable;
    }

    public static class AnimatorPlaybackControllerVL extends AnimatorPlaybackController {
        private final ValueAnimator[] mChildAnimations;

        private AnimatorPlaybackControllerVL(AnimatorSet animatorSet, long j, Runnable runnable) {
            super(animatorSet, j, runnable);
            ArrayList<ValueAnimator> arrayList = new ArrayList<>();
            getAnimationsRecur(this.mAnim, arrayList);
            this.mChildAnimations = (ValueAnimator[]) arrayList.toArray(new ValueAnimator[arrayList.size()]);
        }

        private void getAnimationsRecur(AnimatorSet animatorSet, ArrayList<ValueAnimator> arrayList) {
            long duration = animatorSet.getDuration();
            TimeInterpolator interpolator = animatorSet.getInterpolator();
            for (Animator animator : animatorSet.getChildAnimations()) {
                if (duration > 0) {
                    animator.setDuration(duration);
                }
                if (interpolator != null) {
                    animator.setInterpolator(interpolator);
                }
                if (animator instanceof ValueAnimator) {
                    arrayList.add((ValueAnimator) animator);
                } else if (animator instanceof AnimatorSet) {
                    getAnimationsRecur((AnimatorSet) animator, arrayList);
                } else {
                    throw new RuntimeException("Unknown animation type " + animator);
                }
            }
        }

        @Override
        public void setPlayFraction(float f) {
            this.mCurrentFraction = f;
            if (this.mTargetCancelled) {
                return;
            }
            long jClampDuration = clampDuration(f);
            for (ValueAnimator valueAnimator : this.mChildAnimations) {
                valueAnimator.setCurrentPlayTime(Math.min(jClampDuration, valueAnimator.getDuration()));
            }
        }
    }

    private class OnAnimationEndDispatcher extends AnimationSuccessListener {
        private OnAnimationEndDispatcher() {
        }

        @Override
        public void onAnimationStart(Animator animator) {
            this.mCancelled = false;
        }

        @Override
        public void onAnimationSuccess(Animator animator) {
            dispatchOnEndRecursively(AnimatorPlaybackController.this.mAnim);
            if (AnimatorPlaybackController.this.mEndAction != null) {
                AnimatorPlaybackController.this.mEndAction.run();
            }
        }

        private void dispatchOnEndRecursively(Animator animator) {
            Iterator it = AnimatorPlaybackController.nonNullList(animator.getListeners()).iterator();
            while (it.hasNext()) {
                ((Animator.AnimatorListener) it.next()).onAnimationEnd(animator);
            }
            if (animator instanceof AnimatorSet) {
                Iterator it2 = AnimatorPlaybackController.nonNullList(((AnimatorSet) animator).getChildAnimations()).iterator();
                while (it2.hasNext()) {
                    dispatchOnEndRecursively((Animator) it2.next());
                }
            }
        }
    }

    private static <T> List<T> nonNullList(ArrayList<T> arrayList) {
        return arrayList == null ? Collections.emptyList() : arrayList;
    }
}
