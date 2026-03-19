package com.android.gallery3d.ui;

class FlingScroller {
    private static final int DECELERATED_FACTOR = 4;
    private static final float FLING_DURATION_PARAM = 50.0f;
    private static final String TAG = "Gallery2/FlingController";
    private double mCosAngle;
    private double mCurrV;
    private int mCurrX;
    private int mCurrY;
    private int mDistance;
    private int mDuration;
    private int mFinalX;
    private int mFinalY;
    private int mMaxX;
    private int mMaxY;
    private int mMinX;
    private int mMinY;
    private double mSinAngle;
    private int mStartX;
    private int mStartY;

    FlingScroller() {
    }

    public int getFinalX() {
        return this.mFinalX;
    }

    public int getFinalY() {
        return this.mFinalY;
    }

    public int getDuration() {
        return this.mDuration;
    }

    public int getCurrX() {
        return this.mCurrX;
    }

    public int getCurrY() {
        return this.mCurrY;
    }

    public int getCurrVelocityX() {
        return (int) Math.round(this.mCurrV * this.mCosAngle);
    }

    public int getCurrVelocityY() {
        return (int) Math.round(this.mCurrV * this.mSinAngle);
    }

    public void fling(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        this.mStartX = i;
        this.mStartY = i2;
        this.mMinX = i5;
        this.mMinY = i7;
        this.mMaxX = i6;
        this.mMaxY = i8;
        double d = i3;
        double d2 = i4;
        double dHypot = Math.hypot(d, d2);
        this.mSinAngle = d2 / dHypot;
        this.mCosAngle = d / dHypot;
        this.mDuration = (int) Math.round(50.0d * Math.pow(Math.abs(dHypot), 0.3333333333333333d));
        this.mDistance = (int) Math.round(((dHypot * ((double) this.mDuration)) / 4.0d) / 1000.0d);
        this.mFinalX = getX(1.0f);
        this.mFinalY = getY(1.0f);
    }

    public void computeScrollOffset(float f) {
        float fMin = Math.min(f, 1.0f);
        float fPow = 1.0f - ((float) Math.pow(1.0f - fMin, 4.0d));
        this.mCurrX = getX(fPow);
        this.mCurrY = getY(fPow);
        this.mCurrV = getV(fMin);
    }

    private int getX(float f) {
        int iRound = (int) Math.round(((double) this.mStartX) + (((double) (f * this.mDistance)) * this.mCosAngle));
        if (this.mCosAngle > 0.0d && this.mStartX <= this.mMaxX) {
            return Math.min(iRound, this.mMaxX);
        }
        if (this.mCosAngle < 0.0d && this.mStartX >= this.mMinX) {
            return Math.max(iRound, this.mMinX);
        }
        return iRound;
    }

    private int getY(float f) {
        int iRound = (int) Math.round(((double) this.mStartY) + (((double) (f * this.mDistance)) * this.mSinAngle));
        if (this.mSinAngle > 0.0d && this.mStartY <= this.mMaxY) {
            return Math.min(iRound, this.mMaxY);
        }
        if (this.mSinAngle < 0.0d && this.mStartY >= this.mMinY) {
            return Math.max(iRound, this.mMinY);
        }
        return iRound;
    }

    private double getV(float f) {
        return (((double) ((4 * this.mDistance) * 1000)) * Math.pow(1.0f - f, 3.0d)) / ((double) this.mDuration);
    }
}
