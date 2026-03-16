package com.android.gallery3d.ui;

class FlingScroller {
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

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
        this.mStartX = startX;
        this.mStartY = startY;
        this.mMinX = minX;
        this.mMinY = minY;
        this.mMaxX = maxX;
        this.mMaxY = maxY;
        double velocity = Math.hypot(velocityX, velocityY);
        this.mSinAngle = ((double) velocityY) / velocity;
        this.mCosAngle = ((double) velocityX) / velocity;
        this.mDuration = (int) Math.round(50.0d * Math.pow(Math.abs(velocity), 0.3333333333333333d));
        this.mDistance = (int) Math.round(((((double) this.mDuration) * velocity) / 4.0d) / 1000.0d);
        this.mFinalX = getX(1.0f);
        this.mFinalY = getY(1.0f);
    }

    public void computeScrollOffset(float progress) {
        float progress2 = Math.min(progress, 1.0f);
        float f = 1.0f - ((float) Math.pow(1.0f - progress2, 4.0d));
        this.mCurrX = getX(f);
        this.mCurrY = getY(f);
        this.mCurrV = getV(progress2);
    }

    private int getX(float f) {
        int r = (int) Math.round(((double) this.mStartX) + (((double) (this.mDistance * f)) * this.mCosAngle));
        if (this.mCosAngle > 0.0d && this.mStartX <= this.mMaxX) {
            return Math.min(r, this.mMaxX);
        }
        if (this.mCosAngle < 0.0d && this.mStartX >= this.mMinX) {
            return Math.max(r, this.mMinX);
        }
        return r;
    }

    private int getY(float f) {
        int r = (int) Math.round(((double) this.mStartY) + (((double) (this.mDistance * f)) * this.mSinAngle));
        if (this.mSinAngle > 0.0d && this.mStartY <= this.mMaxY) {
            return Math.min(r, this.mMaxY);
        }
        if (this.mSinAngle < 0.0d && this.mStartY >= this.mMinY) {
            return Math.max(r, this.mMinY);
        }
        return r;
    }

    private double getV(float progress) {
        return (((double) ((this.mDistance * 4) * 1000)) * Math.pow(1.0f - progress, 3.0d)) / ((double) this.mDuration);
    }
}
