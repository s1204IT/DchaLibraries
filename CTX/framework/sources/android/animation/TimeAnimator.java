package android.animation;

import android.view.animation.AnimationUtils;

public class TimeAnimator extends ValueAnimator {
    private TimeListener mListener;
    private long mPreviousTime = -1;

    public interface TimeListener {
        void onTimeUpdate(TimeAnimator timeAnimator, long j, long j2);
    }

    @Override
    public void start() {
        this.mPreviousTime = -1L;
        super.start();
    }

    @Override
    boolean animateBasedOnTime(long j) {
        long j2;
        if (this.mListener != null) {
            long j3 = j - this.mStartTime;
            if (this.mPreviousTime >= 0) {
                j2 = j - this.mPreviousTime;
            } else {
                j2 = 0;
            }
            this.mPreviousTime = j;
            this.mListener.onTimeUpdate(this, j3, j2);
            return false;
        }
        return false;
    }

    @Override
    public void setCurrentPlayTime(long j) {
        long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis();
        this.mStartTime = Math.max(this.mStartTime, jCurrentAnimationTimeMillis - j);
        this.mStartTimeCommitted = true;
        animateBasedOnTime(jCurrentAnimationTimeMillis);
    }

    public void setTimeListener(TimeListener timeListener) {
        this.mListener = timeListener;
    }

    @Override
    void animateValue(float f) {
    }

    @Override
    void initAnimation() {
    }
}
