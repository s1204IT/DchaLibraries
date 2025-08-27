package com.android.systemui.shared.recents.utilities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.util.SparseArray;
import android.util.SparseLongArray;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import java.util.List;

/* loaded from: classes.dex */
public class AnimationProps {
    private Animator.AnimatorListener mListener;
    private SparseLongArray mPropDuration;
    private SparseArray<Interpolator> mPropInterpolators;
    private SparseLongArray mPropStartDelay;
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    public static final AnimationProps IMMEDIATE = new AnimationProps(0, LINEAR_INTERPOLATOR);

    public AnimationProps() {
    }

    public AnimationProps(int i, Interpolator interpolator) {
        this(0, i, interpolator, null);
    }

    public AnimationProps(int i, Interpolator interpolator, Animator.AnimatorListener animatorListener) {
        this(0, i, interpolator, animatorListener);
    }

    public AnimationProps(int i, int i2, Interpolator interpolator) {
        this(i, i2, interpolator, null);
    }

    public AnimationProps(int i, int i2, Interpolator interpolator, Animator.AnimatorListener animatorListener) {
        setStartDelay(0, i);
        setDuration(0, i2);
        setInterpolator(0, interpolator);
        setListener(animatorListener);
    }

    public AnimatorSet createAnimator(List<Animator> list) {
        AnimatorSet animatorSet = new AnimatorSet();
        if (this.mListener != null) {
            animatorSet.addListener(this.mListener);
        }
        animatorSet.playTogether(list);
        return animatorSet;
    }

    public <T extends ValueAnimator> T apply(int i, T t) {
        t.setStartDelay(getStartDelay(i));
        t.setDuration(getDuration(i));
        t.setInterpolator(getInterpolator(i));
        return t;
    }

    public AnimationProps setStartDelay(int i, int i2) {
        if (this.mPropStartDelay == null) {
            this.mPropStartDelay = new SparseLongArray();
        }
        this.mPropStartDelay.append(i, i2);
        return this;
    }

    public long getStartDelay(int i) {
        if (this.mPropStartDelay == null) {
            return 0L;
        }
        long j = this.mPropStartDelay.get(i, -1L);
        if (j != -1) {
            return j;
        }
        return this.mPropStartDelay.get(0, 0L);
    }

    public AnimationProps setDuration(int i, int i2) {
        if (this.mPropDuration == null) {
            this.mPropDuration = new SparseLongArray();
        }
        this.mPropDuration.append(i, i2);
        return this;
    }

    public long getDuration(int i) {
        if (this.mPropDuration == null) {
            return 0L;
        }
        long j = this.mPropDuration.get(i, -1L);
        if (j != -1) {
            return j;
        }
        return this.mPropDuration.get(0, 0L);
    }

    public AnimationProps setInterpolator(int i, Interpolator interpolator) {
        if (this.mPropInterpolators == null) {
            this.mPropInterpolators = new SparseArray<>();
        }
        this.mPropInterpolators.append(i, interpolator);
        return this;
    }

    public Interpolator getInterpolator(int i) {
        if (this.mPropInterpolators != null) {
            Interpolator interpolator = this.mPropInterpolators.get(i);
            if (interpolator != null) {
                return interpolator;
            }
            return this.mPropInterpolators.get(0, LINEAR_INTERPOLATOR);
        }
        return LINEAR_INTERPOLATOR;
    }

    public AnimationProps setListener(Animator.AnimatorListener animatorListener) {
        this.mListener = animatorListener;
        return this;
    }

    public Animator.AnimatorListener getListener() {
        return this.mListener;
    }

    public boolean isImmediate() {
        int size = this.mPropDuration.size();
        for (int i = 0; i < size; i++) {
            if (this.mPropDuration.valueAt(i) > 0) {
                return false;
            }
        }
        return true;
    }
}
