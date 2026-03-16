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
    boolean animationFrame(long currentTime) {
        if (this.mListener != null) {
            long totalTime = currentTime - this.mStartTime;
            long deltaTime = this.mPreviousTime >= 0 ? currentTime - this.mPreviousTime : 0L;
            this.mPreviousTime = currentTime;
            this.mListener.onTimeUpdate(this, totalTime, deltaTime);
            return false;
        }
        return false;
    }

    @Override
    public void setCurrentPlayTime(long playTime) {
        long currentTime = AnimationUtils.currentAnimationTimeMillis();
        this.mStartTime = Math.max(this.mStartTime, currentTime - playTime);
        animationFrame(currentTime);
    }

    public void setTimeListener(TimeListener listener) {
        this.mListener = listener;
    }

    @Override
    void animateValue(float fraction) {
    }

    @Override
    void initAnimation() {
    }
}
