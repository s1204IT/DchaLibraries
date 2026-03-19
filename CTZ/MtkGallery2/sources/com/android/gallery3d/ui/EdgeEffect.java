package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.gallery3d.R;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;

public class EdgeEffect {
    private static final float EPSILON = 0.001f;
    private static final float HELD_EDGE_ALPHA = 0.7f;
    private static final float HELD_EDGE_SCALE_Y = 0.5f;
    private static final float HELD_GLOW_ALPHA = 0.5f;
    private static final float HELD_GLOW_SCALE_Y = 0.5f;
    private static final float MAX_ALPHA = 0.8f;
    private static final float MAX_GLOW_HEIGHT = 4.0f;
    private static final int MIN_VELOCITY = 100;
    private static final int PULL_DECAY_TIME = 1000;
    private static final float PULL_DISTANCE_ALPHA_GLOW_FACTOR = 1.1f;
    private static final int PULL_DISTANCE_EDGE_FACTOR = 7;
    private static final int PULL_DISTANCE_GLOW_FACTOR = 7;
    private static final float PULL_EDGE_BEGIN = 0.6f;
    private static final float PULL_GLOW_BEGIN = 1.0f;
    private static final int PULL_TIME = 167;
    private static final int RECEDE_TIME = 1000;
    private static final int STATE_ABSORB = 2;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PULL = 1;
    private static final int STATE_PULL_DECAY = 4;
    private static final int STATE_RECEDE = 3;
    private static final String TAG = "Gallery2/EdgeEffect";
    private static final int VELOCITY_EDGE_FACTOR = 8;
    private static final int VELOCITY_GLOW_FACTOR = 16;
    private static Drawable mEdge;
    private static Drawable mGlow;
    private float mDuration;
    private float mEdgeAlpha;
    private float mEdgeAlphaFinish;
    private float mEdgeAlphaStart;
    private float mEdgeScaleY;
    private float mEdgeScaleYFinish;
    private float mEdgeScaleYStart;
    private float mGlowAlpha;
    private float mGlowAlphaFinish;
    private float mGlowAlphaStart;
    private float mGlowScaleY;
    private float mGlowScaleYFinish;
    private float mGlowScaleYStart;
    private int mHeight;
    private final Interpolator mInterpolator;
    private final int mMinWidth;
    private float mPullDistance;
    private long mStartTime;
    private int mWidth;
    private final int MIN_WIDTH = 300;
    private int mState = 0;

    public EdgeEffect(Context context) {
        mEdge = new Drawable(context, R.drawable.overscroll_edge);
        mGlow = new Drawable(context, R.drawable.overscroll_glow);
        this.mMinWidth = (int) ((context.getResources().getDisplayMetrics().density * 300.0f) + 0.5f);
        this.mInterpolator = new DecelerateInterpolator();
    }

    public void setSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
    }

    public boolean isFinished() {
        return this.mState == 0;
    }

    public void finish() {
        this.mState = 0;
    }

    public void onPull(float f) {
        long j = AnimationTime.get();
        if (this.mState == 4 && j - this.mStartTime < this.mDuration) {
            return;
        }
        if (this.mState != 1) {
            this.mGlowScaleY = PULL_GLOW_BEGIN;
        }
        this.mState = 1;
        this.mStartTime = j;
        this.mDuration = 167.0f;
        this.mPullDistance += f;
        float fAbs = Math.abs(this.mPullDistance);
        float fMax = Math.max(PULL_EDGE_BEGIN, Math.min(fAbs, MAX_ALPHA));
        this.mEdgeAlphaStart = fMax;
        this.mEdgeAlpha = fMax;
        float fMax2 = Math.max(0.5f, Math.min(fAbs * 7.0f, PULL_GLOW_BEGIN));
        this.mEdgeScaleYStart = fMax2;
        this.mEdgeScaleY = fMax2;
        float fMin = Math.min(MAX_ALPHA, this.mGlowAlpha + (Math.abs(f) * PULL_DISTANCE_ALPHA_GLOW_FACTOR));
        this.mGlowAlphaStart = fMin;
        this.mGlowAlpha = fMin;
        float fAbs2 = Math.abs(f);
        if (f > 0.0f && this.mPullDistance < 0.0f) {
            fAbs2 = -fAbs2;
        }
        if (this.mPullDistance == 0.0f) {
            this.mGlowScaleY = 0.0f;
        }
        float fMin2 = Math.min(4.0f, Math.max(0.0f, this.mGlowScaleY + (fAbs2 * 7.0f)));
        this.mGlowScaleYStart = fMin2;
        this.mGlowScaleY = fMin2;
        this.mEdgeAlphaFinish = this.mEdgeAlpha;
        this.mEdgeScaleYFinish = this.mEdgeScaleY;
        this.mGlowAlphaFinish = this.mGlowAlpha;
        this.mGlowScaleYFinish = this.mGlowScaleY;
    }

    public void onRelease() {
        this.mPullDistance = 0.0f;
        if (this.mState != 1 && this.mState != 4) {
            return;
        }
        this.mState = 3;
        this.mEdgeAlphaStart = this.mEdgeAlpha;
        this.mEdgeScaleYStart = this.mEdgeScaleY;
        this.mGlowAlphaStart = this.mGlowAlpha;
        this.mGlowScaleYStart = this.mGlowScaleY;
        this.mEdgeAlphaFinish = 0.0f;
        this.mEdgeScaleYFinish = 0.0f;
        this.mGlowAlphaFinish = 0.0f;
        this.mGlowScaleYFinish = 0.0f;
        this.mStartTime = AnimationTime.get();
        this.mDuration = 1000.0f;
    }

    public void onAbsorb(int i) {
        this.mState = 2;
        int iMax = Math.max(MIN_VELOCITY, Math.abs(i));
        this.mStartTime = AnimationTime.get();
        this.mDuration = 0.1f + (iMax * 0.03f);
        this.mEdgeAlphaStart = 0.0f;
        this.mEdgeScaleYStart = 0.0f;
        this.mEdgeScaleY = 0.0f;
        this.mGlowAlphaStart = 0.5f;
        this.mGlowScaleYStart = 0.0f;
        this.mEdgeAlphaFinish = Math.max(0, Math.min(r0, 1));
        this.mEdgeScaleYFinish = Math.max(0.5f, Math.min(iMax * 8, PULL_GLOW_BEGIN));
        this.mGlowScaleYFinish = Math.min(0.025f + ((iMax / MIN_VELOCITY) * iMax * 1.5E-4f), 1.75f);
        this.mGlowAlphaFinish = Math.max(this.mGlowAlphaStart, Math.min(iMax * 16 * 1.0E-5f, MAX_ALPHA));
    }

    public boolean draw(GLCanvas gLCanvas) {
        update();
        int intrinsicHeight = mEdge.getIntrinsicHeight();
        mEdge.getIntrinsicWidth();
        int intrinsicHeight2 = mGlow.getIntrinsicHeight();
        int intrinsicWidth = mGlow.getIntrinsicWidth();
        mGlow.setAlpha((int) (Math.max(0.0f, Math.min(this.mGlowAlpha, PULL_GLOW_BEGIN)) * 255.0f));
        float f = intrinsicHeight2;
        int iMin = (int) Math.min((((this.mGlowScaleY * f) * f) / intrinsicWidth) * PULL_EDGE_BEGIN, f * 4.0f);
        if (this.mWidth < this.mMinWidth) {
            int i = (this.mWidth - this.mMinWidth) / 2;
            mGlow.setBounds(i, 0, this.mWidth - i, iMin);
        } else {
            mGlow.setBounds(0, 0, this.mWidth, iMin);
        }
        mGlow.draw(gLCanvas);
        mEdge.setAlpha((int) (Math.max(0.0f, Math.min(this.mEdgeAlpha, PULL_GLOW_BEGIN)) * 255.0f));
        int i2 = (int) (intrinsicHeight * this.mEdgeScaleY);
        if (this.mWidth < this.mMinWidth) {
            int i3 = (this.mWidth - this.mMinWidth) / 2;
            mEdge.setBounds(i3, 0, this.mWidth - i3, i2);
        } else {
            mEdge.setBounds(0, 0, this.mWidth, i2);
        }
        mEdge.draw(gLCanvas);
        return this.mState != 0;
    }

    private void update() {
        float f;
        float fMin = Math.min((AnimationTime.get() - this.mStartTime) / this.mDuration, PULL_GLOW_BEGIN);
        float interpolation = this.mInterpolator.getInterpolation(fMin);
        this.mEdgeAlpha = this.mEdgeAlphaStart + ((this.mEdgeAlphaFinish - this.mEdgeAlphaStart) * interpolation);
        this.mEdgeScaleY = this.mEdgeScaleYStart + ((this.mEdgeScaleYFinish - this.mEdgeScaleYStart) * interpolation);
        this.mGlowAlpha = this.mGlowAlphaStart + ((this.mGlowAlphaFinish - this.mGlowAlphaStart) * interpolation);
        this.mGlowScaleY = this.mGlowScaleYStart + ((this.mGlowScaleYFinish - this.mGlowScaleYStart) * interpolation);
        if (fMin >= 0.999f) {
            switch (this.mState) {
                case 1:
                    this.mState = 4;
                    this.mStartTime = AnimationTime.get();
                    this.mDuration = 1000.0f;
                    this.mEdgeAlphaStart = this.mEdgeAlpha;
                    this.mEdgeScaleYStart = this.mEdgeScaleY;
                    this.mGlowAlphaStart = this.mGlowAlpha;
                    this.mGlowScaleYStart = this.mGlowScaleY;
                    this.mEdgeAlphaFinish = 0.0f;
                    this.mEdgeScaleYFinish = 0.0f;
                    this.mGlowAlphaFinish = 0.0f;
                    this.mGlowScaleYFinish = 0.0f;
                    break;
                case 2:
                    this.mState = 3;
                    this.mStartTime = AnimationTime.get();
                    this.mDuration = 1000.0f;
                    this.mEdgeAlphaStart = this.mEdgeAlpha;
                    this.mEdgeScaleYStart = this.mEdgeScaleY;
                    this.mGlowAlphaStart = this.mGlowAlpha;
                    this.mGlowScaleYStart = this.mGlowScaleY;
                    this.mEdgeAlphaFinish = 0.0f;
                    this.mEdgeScaleYFinish = 0.0f;
                    this.mGlowAlphaFinish = 0.0f;
                    this.mGlowScaleYFinish = 0.0f;
                    break;
                case 3:
                    this.mState = 0;
                    break;
                case 4:
                    if (this.mGlowScaleYFinish != 0.0f) {
                        f = PULL_GLOW_BEGIN / (this.mGlowScaleYFinish * this.mGlowScaleYFinish);
                    } else {
                        f = Float.MAX_VALUE;
                    }
                    this.mEdgeScaleY = this.mEdgeScaleYStart + ((this.mEdgeScaleYFinish - this.mEdgeScaleYStart) * interpolation * f);
                    this.mState = 3;
                    break;
            }
        }
    }

    private static class Drawable extends ResourceTexture {
        private int mAlpha;
        private Rect mBounds;

        public Drawable(Context context, int i) {
            super(context, i);
            this.mBounds = new Rect();
            this.mAlpha = 255;
        }

        public int getIntrinsicWidth() {
            return getWidth();
        }

        public int getIntrinsicHeight() {
            return getHeight();
        }

        public void setBounds(int i, int i2, int i3, int i4) {
            this.mBounds.set(i, i2, i3, i4);
        }

        public void setAlpha(int i) {
            this.mAlpha = i;
        }

        public void draw(GLCanvas gLCanvas) {
            gLCanvas.save(1);
            gLCanvas.multiplyAlpha(this.mAlpha / 255.0f);
            Rect rect = this.mBounds;
            draw(gLCanvas, rect.left, rect.top, rect.width(), rect.height());
            gLCanvas.restore();
        }
    }
}
