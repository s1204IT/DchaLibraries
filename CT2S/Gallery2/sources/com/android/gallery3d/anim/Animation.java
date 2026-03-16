package com.android.gallery3d.anim;

import android.view.animation.Interpolator;
import com.android.gallery3d.common.Utils;

public abstract class Animation {
    private int mDuration;
    private Interpolator mInterpolator;
    private long mStartTime = -2;

    protected abstract void onCalculate(float f);

    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    public void start() {
        this.mStartTime = -1L;
    }

    public void setStartTime(long time) {
        this.mStartTime = time;
    }

    public boolean isActive() {
        return this.mStartTime != -2;
    }

    public boolean calculate(long currentTimeMillis) {
        if (this.mStartTime == -2) {
            return false;
        }
        if (this.mStartTime == -1) {
            this.mStartTime = currentTimeMillis;
        }
        int elapse = (int) (currentTimeMillis - this.mStartTime);
        float x = Utils.clamp(elapse / this.mDuration, 0.0f, 1.0f);
        Interpolator i = this.mInterpolator;
        if (i != null) {
            x = i.getInterpolation(x);
        }
        onCalculate(x);
        if (elapse >= this.mDuration) {
            this.mStartTime = -2L;
        }
        return this.mStartTime != -2;
    }
}
