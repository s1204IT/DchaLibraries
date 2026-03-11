package com.android.launcher3.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.launcher3.compat.PackageInstallerCompat;

public class LauncherEdgeEffect {
    private float mBaseGlowScale;
    private float mDuration;
    private float mGlowAlpha;
    private float mGlowAlphaFinish;
    private float mGlowAlphaStart;
    private float mGlowScaleY;
    private float mGlowScaleYFinish;
    private float mGlowScaleYStart;
    private final Interpolator mInterpolator;
    private float mPullDistance;
    private float mRadius;
    private long mStartTime;
    private static final float SIN = (float) Math.sin(0.5235987755982988d);
    private static final float COS = (float) Math.cos(0.5235987755982988d);
    private int mState = 0;
    private final Rect mBounds = new Rect();
    private final Paint mPaint = new Paint();
    private float mDisplacement = 0.5f;
    private float mTargetDisplacement = 0.5f;

    public LauncherEdgeEffect() {
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mInterpolator = new DecelerateInterpolator();
    }

    public void setSize(int width, int height) {
        float r = (width * 0.5f) / SIN;
        float y = COS * r;
        float h = r - y;
        float or = (height * 0.75f) / SIN;
        float oy = COS * or;
        float oh = or - oy;
        this.mRadius = r;
        this.mBaseGlowScale = h > 0.0f ? Math.min(oh / h, 1.0f) : 1.0f;
        this.mBounds.set(this.mBounds.left, this.mBounds.top, width, (int) Math.min(height, h));
    }

    public boolean isFinished() {
        return this.mState == 0;
    }

    public void onPull(float deltaDistance) {
        onPull(deltaDistance, 0.5f);
    }

    public void onPull(float deltaDistance, float displacement) {
        long now = AnimationUtils.currentAnimationTimeMillis();
        this.mTargetDisplacement = displacement;
        if (this.mState != 4 || now - this.mStartTime >= this.mDuration) {
            if (this.mState != 1) {
                this.mGlowScaleY = Math.max(0.0f, this.mGlowScaleY);
            }
            this.mState = 1;
            this.mStartTime = now;
            this.mDuration = 167.0f;
            this.mPullDistance += deltaDistance;
            float absdd = Math.abs(deltaDistance);
            float fMin = Math.min(0.5f, this.mGlowAlpha + (0.8f * absdd));
            this.mGlowAlphaStart = fMin;
            this.mGlowAlpha = fMin;
            if (this.mPullDistance == 0.0f) {
                this.mGlowScaleYStart = 0.0f;
                this.mGlowScaleY = 0.0f;
            } else {
                float scale = (float) (Math.max(0.0d, (1.0d - (1.0d / Math.sqrt(Math.abs(this.mPullDistance) * this.mBounds.height()))) - 0.3d) / 0.7d);
                this.mGlowScaleYStart = scale;
                this.mGlowScaleY = scale;
            }
            this.mGlowAlphaFinish = this.mGlowAlpha;
            this.mGlowScaleYFinish = this.mGlowScaleY;
        }
    }

    public void onRelease() {
        this.mPullDistance = 0.0f;
        if (this.mState != 1 && this.mState != 4) {
            return;
        }
        this.mState = 3;
        this.mGlowAlphaStart = this.mGlowAlpha;
        this.mGlowScaleYStart = this.mGlowScaleY;
        this.mGlowAlphaFinish = 0.0f;
        this.mGlowScaleYFinish = 0.0f;
        this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
        this.mDuration = 600.0f;
    }

    public void setColor(int color) {
        this.mPaint.setColor(color);
    }

    public boolean draw(Canvas canvas) {
        update();
        float centerX = this.mBounds.centerX();
        float centerY = this.mBounds.height() - this.mRadius;
        canvas.scale(1.0f, Math.min(this.mGlowScaleY, 1.0f) * this.mBaseGlowScale, centerX, 0.0f);
        float displacement = Math.max(0.0f, Math.min(this.mDisplacement, 1.0f)) - 0.5f;
        float translateX = (this.mBounds.width() * displacement) / 2.0f;
        this.mPaint.setAlpha((int) (this.mGlowAlpha * 255.0f));
        canvas.drawCircle(centerX + translateX, centerY, this.mRadius, this.mPaint);
        boolean oneLastFrame = false;
        if (this.mState == 3 && this.mGlowScaleY == 0.0f) {
            this.mState = 0;
            oneLastFrame = true;
        }
        if (this.mState == 0) {
            return oneLastFrame;
        }
        return true;
    }

    private void update() {
        long time = AnimationUtils.currentAnimationTimeMillis();
        float t = Math.min((time - this.mStartTime) / this.mDuration, 1.0f);
        float interp = this.mInterpolator.getInterpolation(t);
        this.mGlowAlpha = this.mGlowAlphaStart + ((this.mGlowAlphaFinish - this.mGlowAlphaStart) * interp);
        this.mGlowScaleY = this.mGlowScaleYStart + ((this.mGlowScaleYFinish - this.mGlowScaleYStart) * interp);
        this.mDisplacement = (this.mDisplacement + this.mTargetDisplacement) / 2.0f;
        if (t < 0.999f) {
        }
        switch (this.mState) {
            case PackageInstallerCompat.STATUS_INSTALLING:
                this.mState = 4;
                this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
                this.mDuration = 2000.0f;
                this.mGlowAlphaStart = this.mGlowAlpha;
                this.mGlowScaleYStart = this.mGlowScaleY;
                this.mGlowAlphaFinish = 0.0f;
                this.mGlowScaleYFinish = 0.0f;
                break;
            case PackageInstallerCompat.STATUS_FAILED:
                this.mState = 3;
                this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
                this.mDuration = 600.0f;
                this.mGlowAlphaStart = this.mGlowAlpha;
                this.mGlowScaleYStart = this.mGlowScaleY;
                this.mGlowAlphaFinish = 0.0f;
                this.mGlowScaleYFinish = 0.0f;
                break;
            case 3:
                this.mState = 0;
                break;
            case 4:
                this.mState = 3;
                break;
        }
    }
}
