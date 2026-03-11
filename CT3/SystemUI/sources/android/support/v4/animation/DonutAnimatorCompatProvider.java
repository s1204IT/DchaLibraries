package android.support.v4.animation;

import android.view.View;
import java.util.ArrayList;
import java.util.List;

class DonutAnimatorCompatProvider implements AnimatorProvider {
    DonutAnimatorCompatProvider() {
    }

    @Override
    public ValueAnimatorCompat emptyValueAnimator() {
        return new DonutFloatValueAnimator();
    }

    private static class DonutFloatValueAnimator implements ValueAnimatorCompat {
        private long mStartTime;
        View mTarget;
        List<AnimatorListenerCompat> mListeners = new ArrayList();
        List<AnimatorUpdateListenerCompat> mUpdateListeners = new ArrayList();
        private long mDuration = 200;
        private float mFraction = 0.0f;
        private boolean mStarted = false;
        private boolean mEnded = false;
        private Runnable mLoopRunnable = new Runnable() {
            @Override
            public void run() {
                long dt = DonutFloatValueAnimator.this.getTime() - DonutFloatValueAnimator.this.mStartTime;
                float fraction = (dt * 1.0f) / DonutFloatValueAnimator.this.mDuration;
                if (fraction > 1.0f || DonutFloatValueAnimator.this.mTarget.getParent() == null) {
                    fraction = 1.0f;
                }
                DonutFloatValueAnimator.this.mFraction = fraction;
                DonutFloatValueAnimator.this.notifyUpdateListeners();
                if (DonutFloatValueAnimator.this.mFraction >= 1.0f) {
                    DonutFloatValueAnimator.this.dispatchEnd();
                } else {
                    DonutFloatValueAnimator.this.mTarget.postDelayed(DonutFloatValueAnimator.this.mLoopRunnable, 16L);
                }
            }
        };

        public void notifyUpdateListeners() {
            for (int i = this.mUpdateListeners.size() - 1; i >= 0; i--) {
                this.mUpdateListeners.get(i).onAnimationUpdate(this);
            }
        }

        @Override
        public void setTarget(View view) {
            this.mTarget = view;
        }

        @Override
        public void addListener(AnimatorListenerCompat listener) {
            this.mListeners.add(listener);
        }

        @Override
        public void setDuration(long duration) {
            if (this.mStarted) {
                return;
            }
            this.mDuration = duration;
        }

        @Override
        public void start() {
            if (this.mStarted) {
                return;
            }
            this.mStarted = true;
            dispatchStart();
            this.mFraction = 0.0f;
            this.mStartTime = getTime();
            this.mTarget.postDelayed(this.mLoopRunnable, 16L);
        }

        public long getTime() {
            return this.mTarget.getDrawingTime();
        }

        private void dispatchStart() {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                this.mListeners.get(i).onAnimationStart(this);
            }
        }

        public void dispatchEnd() {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                this.mListeners.get(i).onAnimationEnd(this);
            }
        }

        private void dispatchCancel() {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                this.mListeners.get(i).onAnimationCancel(this);
            }
        }

        @Override
        public void cancel() {
            if (this.mEnded) {
                return;
            }
            this.mEnded = true;
            if (this.mStarted) {
                dispatchCancel();
            }
            dispatchEnd();
        }

        @Override
        public void addUpdateListener(AnimatorUpdateListenerCompat animatorUpdateListener) {
            this.mUpdateListeners.add(animatorUpdateListener);
        }

        @Override
        public float getAnimatedFraction() {
            return this.mFraction;
        }
    }

    @Override
    public void clearInterpolator(View view) {
    }
}
