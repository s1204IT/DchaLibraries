package com.android.deskclock.widget.sgv;

import android.content.Context;
import android.util.FloatMath;
import android.util.Log;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class OverScrollerSGV {
    private final boolean mFlywheel;
    private Interpolator mInterpolator;
    private int mMode;
    private final SplineOverScroller mScrollerX;
    private final SplineOverScroller mScrollerY;

    public OverScrollerSGV(Context context) {
        this(context, null);
    }

    public OverScrollerSGV(Context context, Interpolator interpolator) {
        this(context, interpolator, true);
    }

    public OverScrollerSGV(Context context, Interpolator interpolator, boolean flywheel) {
        this.mInterpolator = interpolator;
        this.mFlywheel = flywheel;
        this.mScrollerX = new SplineOverScroller(context);
        this.mScrollerY = new SplineOverScroller(context);
    }

    public final boolean isFinished() {
        return this.mScrollerX.mFinished && this.mScrollerY.mFinished;
    }

    public final int getCurrY() {
        return this.mScrollerY.mCurrentPosition;
    }

    public float getCurrVelocity() {
        float squaredNorm = this.mScrollerX.mCurrVelocity * this.mScrollerX.mCurrVelocity;
        return FloatMath.sqrt(squaredNorm + (this.mScrollerY.mCurrVelocity * this.mScrollerY.mCurrVelocity));
    }

    public boolean computeScrollOffset() {
        float q;
        if (isFinished()) {
            return false;
        }
        switch (this.mMode) {
            case 0:
                long time = AnimationUtils.currentAnimationTimeMillis();
                long elapsedTime = time - this.mScrollerX.mStartTime;
                int duration = this.mScrollerX.mDuration;
                if (elapsedTime < duration) {
                    float q2 = elapsedTime / duration;
                    if (this.mInterpolator == null) {
                        q = this.mInterpolator.getInterpolation(q2);
                    } else {
                        q = this.mInterpolator.getInterpolation(q2);
                    }
                    this.mScrollerX.updateScroll(q);
                    this.mScrollerY.updateScroll(q);
                } else {
                    abortAnimation();
                }
                return true;
            case 1:
                if (!this.mScrollerX.mFinished && !this.mScrollerX.update() && !this.mScrollerX.continueWhenFinished()) {
                    this.mScrollerX.finish();
                }
                if (!this.mScrollerY.mFinished && !this.mScrollerY.update() && !this.mScrollerY.continueWhenFinished()) {
                    this.mScrollerY.finish();
                }
                return true;
            default:
                return true;
        }
    }

    public boolean springBack(int startX, int startY, int minX, int maxX, int minY, int maxY) {
        this.mMode = 1;
        boolean spingbackX = this.mScrollerX.springback(startX, minX, maxX);
        boolean spingbackY = this.mScrollerY.springback(startY, minY, maxY);
        return spingbackX || spingbackY;
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
        fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY, int overX, int overY) {
        if (this.mFlywheel && !isFinished()) {
            float oldVelocityX = this.mScrollerX.mCurrVelocity;
            float oldVelocityY = this.mScrollerY.mCurrVelocity;
            if (Math.signum(velocityX) == Math.signum(oldVelocityX) && Math.signum(velocityY) == Math.signum(oldVelocityY)) {
                velocityX = (int) (velocityX + oldVelocityX);
                velocityY = (int) (velocityY + oldVelocityY);
            }
        }
        this.mMode = 1;
        this.mScrollerX.fling(startX, velocityX, minX, maxX, overX);
        this.mScrollerY.fling(startY, velocityY, minY, maxY, overY);
    }

    public void notifyVerticalEdgeReached(int startY, int finalY, int overY) {
        this.mScrollerY.notifyEdgeReached(startY, finalY, overY);
    }

    public void abortAnimation() {
        this.mScrollerX.finish();
        this.mScrollerY.finish();
    }

    static class SplineOverScroller {
        private static float DECELERATION_RATE = (float) (Math.log(0.78d) / Math.log(0.9d));
        private static final float[] SPLINE_POSITION = new float[101];
        private static final float[] SPLINE_TIME = new float[101];
        private float mCurrVelocity;
        private int mCurrentPosition;
        private float mDeceleration;
        private int mDuration;
        private int mFinal;
        private int mOver;
        private final float mPhysicalCoeff;
        private int mSplineDistance;
        private int mSplineDuration;
        private int mStart;
        private long mStartTime;
        private int mVelocity;
        private float mFlingFriction = ViewConfiguration.getScrollFriction();
        private int mState = 0;
        private boolean mFinished = true;

        static {
            float x;
            float coef;
            float y;
            float coef2;
            float x_min = 0.0f;
            float y_min = 0.0f;
            for (int i = 0; i < 100; i++) {
                float alpha = i / 100.0f;
                float x_max = 1.0f;
                while (true) {
                    x = x_min + ((x_max - x_min) / 2.0f);
                    coef = 3.0f * x * (1.0f - x);
                    float tx = ((((1.0f - x) * 0.175f) + (0.35000002f * x)) * coef) + (x * x * x);
                    if (Math.abs(tx - alpha) < 1.0E-5d) {
                        break;
                    } else if (tx > alpha) {
                        x_max = x;
                    } else {
                        x_min = x;
                    }
                }
                SPLINE_POSITION[i] = ((((1.0f - x) * 0.5f) + x) * coef) + (x * x * x);
                float y_max = 1.0f;
                while (true) {
                    y = y_min + ((y_max - y_min) / 2.0f);
                    coef2 = 3.0f * y * (1.0f - y);
                    float dy = ((((1.0f - y) * 0.5f) + y) * coef2) + (y * y * y);
                    if (Math.abs(dy - alpha) < 1.0E-5d) {
                        break;
                    } else if (dy > alpha) {
                        y_max = y;
                    } else {
                        y_min = y;
                    }
                }
                SPLINE_TIME[i] = ((((1.0f - y) * 0.175f) + (0.35000002f * y)) * coef2) + (y * y * y);
            }
            float[] fArr = SPLINE_POSITION;
            SPLINE_TIME[100] = 1.0f;
            fArr[100] = 1.0f;
        }

        SplineOverScroller(Context context) {
            float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
            this.mPhysicalCoeff = 386.0878f * ppi * 0.84f;
        }

        void updateScroll(float q) {
            this.mCurrentPosition = this.mStart + Math.round((this.mFinal - this.mStart) * q);
        }

        private static float getDeceleration(int velocity) {
            return velocity > 0 ? -2000.0f : 2000.0f;
        }

        private void adjustDuration(int start, int oldFinal, int newFinal) {
            int oldDistance = oldFinal - start;
            int newDistance = newFinal - start;
            float x = Math.abs(newDistance / oldDistance);
            int index = (int) (100.0f * x);
            if (index < 100) {
                float x_inf = index / 100.0f;
                float x_sup = (index + 1) / 100.0f;
                float t_inf = SPLINE_TIME[index];
                float t_sup = SPLINE_TIME[index + 1];
                float timeCoef = t_inf + (((x - x_inf) / (x_sup - x_inf)) * (t_sup - t_inf));
                this.mDuration = (int) (this.mDuration * timeCoef);
            }
        }

        void finish() {
            this.mCurrentPosition = this.mFinal;
            this.mFinished = true;
        }

        boolean springback(int start, int min, int max) {
            this.mFinished = true;
            this.mFinal = start;
            this.mStart = start;
            this.mVelocity = 0;
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mDuration = 0;
            if (start < min) {
                startSpringback(start, min, 0);
            } else if (start > max) {
                startSpringback(start, max, 0);
            }
            return !this.mFinished;
        }

        private void startSpringback(int start, int end, int velocity) {
            this.mFinished = false;
            this.mState = 1;
            this.mStart = start;
            this.mFinal = end;
            int delta = start - end;
            this.mDeceleration = getDeceleration(delta);
            this.mVelocity = -delta;
            this.mOver = Math.abs(delta);
            this.mDuration = (int) (1000.0d * Math.sqrt(((-2.0d) * ((double) delta)) / ((double) this.mDeceleration)));
        }

        void fling(int start, int velocity, int min, int max, int over) {
            this.mOver = over;
            this.mFinished = false;
            this.mVelocity = velocity;
            this.mCurrVelocity = velocity;
            this.mSplineDuration = 0;
            this.mDuration = 0;
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mStart = start;
            this.mCurrentPosition = start;
            if (start > max || start < min) {
                startAfterEdge(start, min, max, velocity);
                return;
            }
            this.mState = 0;
            double totalDistance = 0.0d;
            if (velocity != 0) {
                int splineFlingDuration = getSplineFlingDuration(velocity);
                this.mSplineDuration = splineFlingDuration;
                this.mDuration = splineFlingDuration;
                totalDistance = getSplineFlingDistance(velocity);
            }
            this.mSplineDistance = (int) (((double) Math.signum(velocity)) * totalDistance);
            this.mFinal = this.mSplineDistance + start;
            if (this.mFinal < min) {
                adjustDuration(this.mStart, this.mFinal, min);
                this.mFinal = min;
            }
            if (this.mFinal > max) {
                adjustDuration(this.mStart, this.mFinal, max);
                this.mFinal = max;
            }
        }

        private double getSplineDeceleration(int velocity) {
            return Math.log((0.35f * Math.abs(velocity)) / (this.mFlingFriction * this.mPhysicalCoeff));
        }

        private double getSplineFlingDistance(int velocity) {
            double l = getSplineDeceleration(velocity);
            double decelMinusOne = ((double) DECELERATION_RATE) - 1.0d;
            return ((double) (this.mFlingFriction * this.mPhysicalCoeff)) * Math.exp((((double) DECELERATION_RATE) / decelMinusOne) * l);
        }

        private int getSplineFlingDuration(int velocity) {
            double l = getSplineDeceleration(velocity);
            double decelMinusOne = ((double) DECELERATION_RATE) - 1.0d;
            return (int) (1000.0d * Math.exp(l / decelMinusOne));
        }

        private void fitOnBounceCurve(int start, int end, int velocity) {
            float durationToApex = (-velocity) / this.mDeceleration;
            float distanceToApex = ((velocity * velocity) / 2.0f) / Math.abs(this.mDeceleration);
            float distanceToEdge = Math.abs(end - start);
            float totalDuration = (float) Math.sqrt((2.0d * ((double) (distanceToApex + distanceToEdge))) / ((double) Math.abs(this.mDeceleration)));
            this.mStartTime -= (long) ((int) (1000.0f * (totalDuration - durationToApex)));
            this.mStart = end;
            this.mVelocity = (int) ((-this.mDeceleration) * totalDuration);
        }

        private void startBounceAfterEdge(int start, int end, int velocity) {
            this.mDeceleration = getDeceleration(velocity == 0 ? start - end : velocity);
            fitOnBounceCurve(start, end, velocity);
            onEdgeReached();
        }

        private void startAfterEdge(int start, int min, int max, int velocity) {
            if (start > min && start < max) {
                Log.e("OverScroller", "startAfterEdge called from a valid position");
                this.mFinished = true;
                return;
            }
            boolean positive = start > max;
            int edge = positive ? max : min;
            int overDistance = start - edge;
            boolean keepIncreasing = overDistance * velocity >= 0;
            if (keepIncreasing) {
                startBounceAfterEdge(start, edge, velocity);
            } else {
                getSplineFlingDistance(velocity);
                startSpringback(start, edge, velocity);
            }
        }

        void notifyEdgeReached(int start, int end, int over) {
            if (this.mState == 0) {
                this.mOver = over;
                this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
                startAfterEdge(start, end, end, (int) this.mCurrVelocity);
            }
        }

        private void onEdgeReached() {
            float distance = (this.mVelocity * this.mVelocity) / (Math.abs(this.mDeceleration) * 2.0f);
            float sign = Math.signum(this.mVelocity);
            if (distance > this.mOver) {
                this.mDeceleration = (((-sign) * this.mVelocity) * this.mVelocity) / (this.mOver * 2.0f);
                distance = this.mOver;
            }
            this.mOver = (int) distance;
            this.mState = 2;
            int i = this.mStart;
            if (this.mVelocity <= 0) {
                distance = -distance;
            }
            this.mFinal = i + ((int) distance);
            this.mDuration = -((int) ((1000.0f * this.mVelocity) / this.mDeceleration));
        }

        boolean continueWhenFinished() {
            switch (this.mState) {
                case 0:
                    if (this.mDuration >= this.mSplineDuration) {
                        return false;
                    }
                    this.mStart = this.mFinal;
                    this.mVelocity = (int) this.mCurrVelocity;
                    this.mDeceleration = getDeceleration(this.mVelocity);
                    this.mStartTime += (long) this.mDuration;
                    onEdgeReached();
                    break;
                    break;
                case 1:
                    return false;
                case 2:
                    this.mStartTime += (long) this.mDuration;
                    startSpringback(this.mFinal, this.mStart, 0);
                    break;
            }
            update();
            return true;
        }

        boolean update() {
            long time = AnimationUtils.currentAnimationTimeMillis();
            long currentTime = time - this.mStartTime;
            if (currentTime > this.mDuration) {
                return false;
            }
            double distance = 0.0d;
            switch (this.mState) {
                case 0:
                    float t = currentTime / this.mSplineDuration;
                    int index = (int) (100.0f * t);
                    float distanceCoef = 1.0f;
                    float velocityCoef = 0.0f;
                    if (index < 100) {
                        float t_inf = index / 100.0f;
                        float t_sup = (index + 1) / 100.0f;
                        float d_inf = SPLINE_POSITION[index];
                        float d_sup = SPLINE_POSITION[index + 1];
                        velocityCoef = (d_sup - d_inf) / (t_sup - t_inf);
                        distanceCoef = d_inf + ((t - t_inf) * velocityCoef);
                    }
                    distance = this.mSplineDistance * distanceCoef;
                    this.mCurrVelocity = ((this.mSplineDistance * velocityCoef) / this.mSplineDuration) * 1000.0f;
                    break;
                case 1:
                    float t2 = currentTime / this.mDuration;
                    float t22 = t2 * t2;
                    float sign = Math.signum(this.mVelocity);
                    distance = this.mOver * sign * ((3.0f * t22) - ((2.0f * t2) * t22));
                    this.mCurrVelocity = this.mOver * sign * 6.0f * ((-t2) + t22);
                    break;
                case 2:
                    float t3 = currentTime / 1000.0f;
                    this.mCurrVelocity = this.mVelocity + (this.mDeceleration * t3);
                    distance = (this.mVelocity * t3) + (((this.mDeceleration * t3) * t3) / 2.0f);
                    break;
            }
            this.mCurrentPosition = this.mStart + ((int) Math.round(distance));
            return true;
        }
    }
}
