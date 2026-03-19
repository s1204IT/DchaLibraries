package com.android.gallery3d.common;

import android.content.Context;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.gallery3d.util.Log;

public class OverScroller {
    private final boolean mFlywheel;
    private Interpolator mInterpolator;
    private int mMode;
    private final SplineOverScroller mScrollerX;
    private final SplineOverScroller mScrollerY;

    public OverScroller(Context context) {
        this(context, null);
    }

    public OverScroller(Context context, Interpolator interpolator) {
        this(context, interpolator, true);
    }

    public OverScroller(Context context, Interpolator interpolator, boolean z) {
        this.mInterpolator = interpolator;
        this.mFlywheel = z;
        this.mScrollerX = new SplineOverScroller();
        this.mScrollerY = new SplineOverScroller();
        SplineOverScroller.initFromContext(context);
    }

    public final boolean isFinished() {
        return this.mScrollerX.mFinished && this.mScrollerY.mFinished;
    }

    public final void forceFinished(boolean z) {
        this.mScrollerX.mFinished = this.mScrollerY.mFinished = z;
    }

    public final int getCurrX() {
        return this.mScrollerX.mCurrentPosition;
    }

    public float getCurrVelocity() {
        return (float) Math.hypot(this.mScrollerX.mCurrVelocity, this.mScrollerY.mCurrVelocity);
    }

    public final int getFinalX() {
        return this.mScrollerX.mFinal;
    }

    public boolean computeScrollOffset() {
        float interpolation;
        if (isFinished()) {
            return false;
        }
        switch (this.mMode) {
            case 0:
                long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis() - this.mScrollerX.mStartTime;
                int i = this.mScrollerX.mDuration;
                if (jCurrentAnimationTimeMillis < i) {
                    float f = jCurrentAnimationTimeMillis / i;
                    if (this.mInterpolator == null) {
                        interpolation = Scroller.viscousFluid(f);
                    } else {
                        interpolation = this.mInterpolator.getInterpolation(f);
                    }
                    this.mScrollerX.updateScroll(interpolation);
                    this.mScrollerY.updateScroll(interpolation);
                    return true;
                }
                abortAnimation();
                return true;
            case 1:
                if (!this.mScrollerX.mFinished && !this.mScrollerX.update() && !this.mScrollerX.continueWhenFinished()) {
                    this.mScrollerX.finish();
                }
                if (!this.mScrollerY.mFinished && !this.mScrollerY.update() && !this.mScrollerY.continueWhenFinished()) {
                    this.mScrollerY.finish();
                    return true;
                }
                return true;
            default:
                return true;
        }
    }

    public void startScroll(int i, int i2, int i3, int i4, int i5) {
        this.mMode = 0;
        this.mScrollerX.startScroll(i, i3, i5);
        this.mScrollerY.startScroll(i2, i4, i5);
    }

    public void fling(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10) {
        int i11;
        int i12;
        int i13;
        int i14;
        if (this.mFlywheel && !isFinished()) {
            float f = this.mScrollerX.mCurrVelocity;
            float f2 = this.mScrollerY.mCurrVelocity;
            i11 = i3;
            float f3 = i11;
            if (Math.signum(f3) == Math.signum(f)) {
                i12 = i4;
                float f4 = i12;
                if (Math.signum(f4) == Math.signum(f2)) {
                    i13 = (int) (f4 + f2);
                    i14 = (int) (f3 + f);
                }
                this.mMode = 1;
                this.mScrollerX.fling(i, i14, i5, i6, i9);
                this.mScrollerY.fling(i2, i13, i7, i8, i10);
            }
            i13 = i12;
            i14 = i11;
            this.mMode = 1;
            this.mScrollerX.fling(i, i14, i5, i6, i9);
            this.mScrollerY.fling(i2, i13, i7, i8, i10);
        }
        i11 = i3;
        i12 = i4;
        i13 = i12;
        i14 = i11;
        this.mMode = 1;
        this.mScrollerX.fling(i, i14, i5, i6, i9);
        this.mScrollerY.fling(i2, i13, i7, i8, i10);
    }

    public void abortAnimation() {
        this.mScrollerX.finish();
        this.mScrollerY.finish();
    }

    static class SplineOverScroller {
        private static float PHYSICAL_COEF;
        private float mCurrVelocity;
        private int mCurrentPosition;
        private float mDeceleration;
        private int mDuration;
        private int mFinal;
        private int mMaxScrollLength;
        private int mMinScrollLength;
        private int mOver;
        private int mSplineDistance;
        private int mSplineDuration;
        private int mStart;
        private long mStartTime;
        private int mVelocity;
        private static float DECELERATION_RATE = (float) (Math.log(0.78d) / Math.log(0.9d));
        private static final float[] SPLINE_POSITION = new float[101];
        private static final float[] SPLINE_TIME = new float[101];
        private float mFlingFriction = ViewConfiguration.getScrollFriction();
        private int mState = 0;
        private boolean mFinished = true;

        static {
            float f;
            float f2;
            float f3;
            float f4;
            float f5;
            float f6;
            float f7;
            float f8;
            float f9;
            float f10;
            float f11 = 0.0f;
            float f12 = 0.0f;
            for (int i = 0; i < 100; i++) {
                float f13 = i / 100.0f;
                float f14 = 1.0f;
                while (true) {
                    f = 2.0f;
                    f2 = ((f14 - f11) / 2.0f) + f11;
                    f3 = 3.0f;
                    f4 = 1.0f - f2;
                    f5 = 3.0f * f2 * f4;
                    f6 = f2 * f2 * f2;
                    float f15 = (((f4 * 0.175f) + (f2 * 0.35000002f)) * f5) + f6;
                    if (Math.abs(f15 - f13) < 1.0E-5d) {
                        break;
                    } else if (f15 > f13) {
                        f14 = f2;
                    } else {
                        f11 = f2;
                    }
                }
                SPLINE_POSITION[i] = (f5 * ((f4 * 0.5f) + f2)) + f6;
                float f16 = 1.0f;
                while (true) {
                    f7 = ((f16 - f12) / f) + f12;
                    f8 = 1.0f - f7;
                    f9 = f3 * f7 * f8;
                    f10 = f7 * f7 * f7;
                    float f17 = (((f8 * 0.5f) + f7) * f9) + f10;
                    if (Math.abs(f17 - f13) < 1.0E-5d) {
                        break;
                    }
                    if (f17 > f13) {
                        f16 = f7;
                    } else {
                        f12 = f7;
                    }
                    f = 2.0f;
                    f3 = 3.0f;
                }
                SPLINE_TIME[i] = (f9 * ((f8 * 0.175f) + (f7 * 0.35000002f))) + f10;
            }
            float[] fArr = SPLINE_POSITION;
            SPLINE_TIME[100] = 1.0f;
            fArr[100] = 1.0f;
        }

        static void initFromContext(Context context) {
            PHYSICAL_COEF = 386.0878f * context.getResources().getDisplayMetrics().density * 160.0f * 0.84f;
        }

        SplineOverScroller() {
        }

        void updateScroll(float f) {
            this.mCurrentPosition = this.mStart + Math.round(f * (this.mFinal - this.mStart));
        }

        private static float getDeceleration(int i) {
            return i > 0 ? -2000.0f : 2000.0f;
        }

        private void adjustDuration(int i, int i2, int i3) {
            float fAbs = Math.abs((i3 - i) / (i2 - i));
            int i4 = (int) (100.0f * fAbs);
            if (i4 < 100) {
                float f = i4 / 100.0f;
                int i5 = i4 + 1;
                float f2 = SPLINE_TIME[i4];
                this.mDuration = (int) (this.mDuration * (f2 + (((fAbs - f) / ((i5 / 100.0f) - f)) * (SPLINE_TIME[i5] - f2))));
            }
        }

        void startScroll(int i, int i2, int i3) {
            this.mFinished = false;
            this.mStart = i;
            this.mFinal = i + i2;
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mDuration = i3;
            this.mDeceleration = 0.0f;
            this.mVelocity = 0;
        }

        void finish() {
            this.mCurrentPosition = this.mFinal;
            this.mFinished = true;
        }

        private void startSpringback(int i, int i2, int i3) {
            this.mFinished = false;
            this.mState = 1;
            this.mStart = i;
            this.mFinal = i2;
            int i4 = i - i2;
            this.mDeceleration = getDeceleration(i4);
            this.mVelocity = -i4;
            this.mOver = Math.abs(i4);
            this.mDuration = (int) (1000.0d * Math.sqrt(((-2.0d) * ((double) i4)) / ((double) this.mDeceleration)));
        }

        void fling(int i, int i2, int i3, int i4, int i5) {
            this.mOver = i5;
            this.mFinished = false;
            this.mVelocity = i2;
            float f = i2;
            this.mCurrVelocity = f;
            this.mSplineDuration = 0;
            this.mDuration = 0;
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mStart = i;
            this.mCurrentPosition = i;
            if (i > i4 || i < i3) {
                startAfterEdge(i, i3, i4, i2);
                return;
            }
            this.mState = 0;
            double splineFlingDistance = 0.0d;
            if (i2 != 0) {
                int splineFlingDuration = getSplineFlingDuration(i2);
                this.mSplineDuration = splineFlingDuration;
                this.mDuration = splineFlingDuration;
                splineFlingDistance = getSplineFlingDistance(i2);
            }
            this.mSplineDistance = (int) (splineFlingDistance * ((double) Math.signum(f)));
            this.mFinal = i + this.mSplineDistance;
            if (this.mFinal < i3) {
                adjustDuration(this.mStart, this.mFinal, i3);
                this.mFinal = i3;
            }
            if (this.mFinal > i4) {
                adjustDuration(this.mStart, this.mFinal, i4);
                this.mFinal = i4;
            }
        }

        private double getSplineDeceleration(int i) {
            return Math.log((0.35f * Math.abs(i)) / (this.mFlingFriction * PHYSICAL_COEF));
        }

        private double getSplineFlingDistance(int i) {
            return ((double) (this.mFlingFriction * PHYSICAL_COEF)) * Math.exp((((double) DECELERATION_RATE) / (((double) DECELERATION_RATE) - 1.0d)) * getSplineDeceleration(i));
        }

        private int getSplineFlingDuration(int i) {
            return (int) (1000.0d * Math.exp(getSplineDeceleration(i) / (((double) DECELERATION_RATE) - 1.0d)));
        }

        private void fitOnBounceCurve(int i, int i2, int i3) {
            float f = (-i3) / this.mDeceleration;
            float fSqrt = (float) Math.sqrt((2.0d * ((double) ((((i3 * i3) / 2.0f) / Math.abs(this.mDeceleration)) + Math.abs(i2 - i)))) / ((double) Math.abs(this.mDeceleration)));
            this.mStartTime -= (long) ((int) (1000.0f * (fSqrt - f)));
            this.mStart = i2;
            this.mVelocity = (int) ((-this.mDeceleration) * fSqrt);
        }

        private void startBounceAfterEdge(int i, int i2, int i3) {
            this.mDeceleration = getDeceleration(i3 == 0 ? i - i2 : i3);
            fitOnBounceCurve(i, i2, i3);
            onEdgeReached();
        }

        private void startAfterEdge(int i, int i2, int i3, int i4) {
            boolean z = true;
            if (i > i2 && i < i3) {
                Log.e("OverScroller", "startAfterEdge called from a valid position");
                this.mFinished = true;
                return;
            }
            boolean z2 = i > i3;
            int i5 = z2 ? i3 : i2;
            if ((i - i5) * i4 < 0) {
                z = false;
            }
            if (z) {
                startBounceAfterEdge(i, i5, i4);
            } else if (getSplineFlingDistance(i4) > Math.abs(r4)) {
                fling(i, i4, z2 ? i2 : i, z2 ? i : i3, this.mOver);
            } else {
                startSpringback(i, i5, i4);
            }
        }

        private void onEdgeReached() {
            float fAbs = (this.mVelocity * this.mVelocity) / (Math.abs(this.mDeceleration) * 2.0f);
            float fSignum = Math.signum(this.mVelocity);
            if (fAbs > this.mOver) {
                this.mDeceleration = (((-fSignum) * this.mVelocity) * this.mVelocity) / (2.0f * this.mOver);
                int i = this.mOver;
            }
            this.mState = 2;
            this.mFinal = this.mVelocity > 0 ? this.mMaxScrollLength : this.mMinScrollLength;
            this.mOver = Math.abs(this.mStart - this.mFinal);
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
            long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis() - this.mStartTime;
            if (jCurrentAnimationTimeMillis > this.mDuration) {
                return false;
            }
            double d = 0.0d;
            switch (this.mState) {
                case 0:
                    float f = jCurrentAnimationTimeMillis / this.mSplineDuration;
                    int i = (int) (100.0f * f);
                    float f2 = 1.0f;
                    float f3 = 0.0f;
                    if (i < 100) {
                        float f4 = i / 100.0f;
                        int i2 = i + 1;
                        float f5 = SPLINE_POSITION[i];
                        f3 = (SPLINE_POSITION[i2] - f5) / ((i2 / 100.0f) - f4);
                        f2 = f5 + ((f - f4) * f3);
                    }
                    d = f2 * this.mSplineDistance;
                    this.mCurrVelocity = ((f3 * this.mSplineDistance) / this.mSplineDuration) * 1000.0f;
                    break;
                case 1:
                    float f6 = jCurrentAnimationTimeMillis / this.mDuration;
                    float f7 = f6 * f6;
                    float fSignum = Math.signum(this.mVelocity);
                    this.mCurrVelocity = fSignum * this.mOver * 6.0f * ((-f6) + f7);
                    d = this.mOver * fSignum * ((3.0f * f7) - ((2.0f * f6) * f7));
                    break;
                case 2:
                    float f8 = jCurrentAnimationTimeMillis / 1000.0f;
                    this.mCurrVelocity = this.mVelocity + (this.mDeceleration * f8);
                    d = (this.mVelocity * f8) + (((this.mDeceleration * f8) * f8) / 2.0f);
                    break;
            }
            this.mCurrentPosition = this.mStart + ((int) Math.round(d));
            return true;
        }
    }

    public void setMaxScrollLength(int i) {
        this.mScrollerX.mMaxScrollLength = i;
    }

    public void setMinScrollLength(int i) {
        this.mScrollerX.mMinScrollLength = i;
    }
}
