package com.android.gallery3d.ui;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.gallery3d.common.Utils;

class EdgeAnimation {
    private long mDuration;
    private long mStartTime;
    private float mValue;
    private float mValueFinish;
    private float mValueStart;
    private final Interpolator mInterpolator = new DecelerateInterpolator();
    private int mState = 0;

    private void startAnimation(float start, float finish, long duration, int newState) {
        this.mValueStart = start;
        this.mValueFinish = finish;
        this.mDuration = duration;
        this.mStartTime = now();
        this.mState = newState;
    }

    public void onPull(float deltaDistance) {
        if (this.mState != 2) {
            this.mValue = Utils.clamp(this.mValue + deltaDistance, -1.0f, 1.0f);
            this.mState = 1;
        }
    }

    public void onRelease() {
        if (this.mState != 0 && this.mState != 2) {
            startAnimation(this.mValue, 0.0f, 500L, 3);
        }
    }

    public void onAbsorb(float velocity) {
        float finish = Utils.clamp(this.mValue + (0.1f * velocity), -1.0f, 1.0f);
        startAnimation(this.mValue, finish, 200L, 2);
    }

    public boolean update() {
        if (this.mState == 0) {
            return false;
        }
        if (this.mState == 1) {
            return true;
        }
        float t = Utils.clamp((now() - this.mStartTime) / this.mDuration, 0.0f, 1.0f);
        float interp = this.mState == 2 ? t : this.mInterpolator.getInterpolation(t);
        this.mValue = this.mValueStart + ((this.mValueFinish - this.mValueStart) * interp);
        if (t >= 1.0f) {
            switch (this.mState) {
                case 2:
                    startAnimation(this.mValue, 0.0f, 500L, 3);
                    break;
                case 3:
                    this.mState = 0;
                    break;
            }
        }
        return true;
    }

    public float getValue() {
        return this.mValue;
    }

    private long now() {
        return AnimationTime.get();
    }
}
