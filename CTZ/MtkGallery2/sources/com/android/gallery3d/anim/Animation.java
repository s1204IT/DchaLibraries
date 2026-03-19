package com.android.gallery3d.anim;

import android.view.animation.Interpolator;
import com.android.gallery3d.common.Utils;

public abstract class Animation {
    private static final long ANIMATION_START = -1;
    private static final long NO_ANIMATION = -2;
    private int mDuration;
    private Interpolator mInterpolator;
    private long mStartTime = NO_ANIMATION;

    protected abstract void onCalculate(float f);

    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public void setDuration(int i) {
        this.mDuration = i;
    }

    public void start() {
        this.mStartTime = -1L;
    }

    public void setStartTime(long j) {
        this.mStartTime = j;
    }

    public boolean isActive() {
        return this.mStartTime != NO_ANIMATION;
    }

    public void forceStop() {
        this.mStartTime = NO_ANIMATION;
    }

    public boolean calculate(long j) {
        if (this.mStartTime == NO_ANIMATION) {
            return false;
        }
        if (this.mStartTime == -1) {
            this.mStartTime = j;
        }
        int i = (int) (j - this.mStartTime);
        float fClamp = Utils.clamp(i / this.mDuration, 0.0f, 1.0f);
        Interpolator interpolator = this.mInterpolator;
        if (interpolator != null) {
            fClamp = interpolator.getInterpolation(fClamp);
        }
        onCalculate(fClamp);
        if (i >= this.mDuration) {
            this.mStartTime = NO_ANIMATION;
        }
        return this.mStartTime != NO_ANIMATION;
    }
}
