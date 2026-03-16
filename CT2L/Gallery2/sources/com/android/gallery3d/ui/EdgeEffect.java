package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.gallery3d.R;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;

public class EdgeEffect {
    private float mDuration;
    private final Drawable mEdge;
    private float mEdgeAlpha;
    private float mEdgeAlphaFinish;
    private float mEdgeAlphaStart;
    private float mEdgeScaleY;
    private float mEdgeScaleYFinish;
    private float mEdgeScaleYStart;
    private final Drawable mGlow;
    private float mGlowAlpha;
    private float mGlowAlphaFinish;
    private float mGlowAlphaStart;
    private float mGlowScaleY;
    private float mGlowScaleYFinish;
    private float mGlowScaleYStart;
    private int mHeight;
    private final int mMinWidth;
    private float mPullDistance;
    private long mStartTime;
    private int mWidth;
    private final int MIN_WIDTH = 300;
    private int mState = 0;
    private final Interpolator mInterpolator = new DecelerateInterpolator();

    public EdgeEffect(Context context) {
        this.mEdge = new Drawable(context, R.drawable.overscroll_edge);
        this.mGlow = new Drawable(context, R.drawable.overscroll_glow);
        this.mMinWidth = (int) ((context.getResources().getDisplayMetrics().density * 300.0f) + 0.5f);
    }

    public void setSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    public boolean isFinished() {
        return this.mState == 0;
    }

    public void onPull(float deltaDistance) {
        long now = AnimationTime.get();
        if (this.mState != 4 || now - this.mStartTime >= this.mDuration) {
            if (this.mState != 1) {
                this.mGlowScaleY = 1.0f;
            }
            this.mState = 1;
            this.mStartTime = now;
            this.mDuration = 167.0f;
            this.mPullDistance += deltaDistance;
            float distance = Math.abs(this.mPullDistance);
            float fMax = Math.max(0.6f, Math.min(distance, 0.8f));
            this.mEdgeAlphaStart = fMax;
            this.mEdgeAlpha = fMax;
            float fMax2 = Math.max(0.5f, Math.min(distance * 7.0f, 1.0f));
            this.mEdgeScaleYStart = fMax2;
            this.mEdgeScaleY = fMax2;
            float fMin = Math.min(0.8f, this.mGlowAlpha + (Math.abs(deltaDistance) * 1.1f));
            this.mGlowAlphaStart = fMin;
            this.mGlowAlpha = fMin;
            float glowChange = Math.abs(deltaDistance);
            if (deltaDistance > 0.0f && this.mPullDistance < 0.0f) {
                glowChange = -glowChange;
            }
            if (this.mPullDistance == 0.0f) {
                this.mGlowScaleY = 0.0f;
            }
            float fMin2 = Math.min(4.0f, Math.max(0.0f, this.mGlowScaleY + (glowChange * 7.0f)));
            this.mGlowScaleYStart = fMin2;
            this.mGlowScaleY = fMin2;
            this.mEdgeAlphaFinish = this.mEdgeAlpha;
            this.mEdgeScaleYFinish = this.mEdgeScaleY;
            this.mGlowAlphaFinish = this.mGlowAlpha;
            this.mGlowScaleYFinish = this.mGlowScaleY;
        }
    }

    public void onRelease() {
        this.mPullDistance = 0.0f;
        if (this.mState == 1 || this.mState == 4) {
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
    }

    public void onAbsorb(int velocity) {
        this.mState = 2;
        int velocity2 = Math.max(100, Math.abs(velocity));
        this.mStartTime = AnimationTime.get();
        this.mDuration = 0.1f + (velocity2 * 0.03f);
        this.mEdgeAlphaStart = 0.0f;
        this.mEdgeScaleYStart = 0.0f;
        this.mEdgeScaleY = 0.0f;
        this.mGlowAlphaStart = 0.5f;
        this.mGlowScaleYStart = 0.0f;
        this.mEdgeAlphaFinish = Math.max(0, Math.min(velocity2 * 8, 1));
        this.mEdgeScaleYFinish = Math.max(0.5f, Math.min(velocity2 * 8, 1.0f));
        this.mGlowScaleYFinish = Math.min(0.025f + ((velocity2 / 100) * velocity2 * 1.5E-4f), 1.75f);
        this.mGlowAlphaFinish = Math.max(this.mGlowAlphaStart, Math.min(velocity2 * 16 * 1.0E-5f, 0.8f));
    }

    public boolean draw(GLCanvas canvas) {
        update();
        int edgeHeight = this.mEdge.getIntrinsicHeight();
        this.mEdge.getIntrinsicWidth();
        int glowHeight = this.mGlow.getIntrinsicHeight();
        int glowWidth = this.mGlow.getIntrinsicWidth();
        this.mGlow.setAlpha((int) (Math.max(0.0f, Math.min(this.mGlowAlpha, 1.0f)) * 255.0f));
        int glowBottom = (int) Math.min((((glowHeight * this.mGlowScaleY) * glowHeight) / glowWidth) * 0.6f, glowHeight * 4.0f);
        if (this.mWidth < this.mMinWidth) {
            int glowLeft = (this.mWidth - this.mMinWidth) / 2;
            this.mGlow.setBounds(glowLeft, 0, this.mWidth - glowLeft, glowBottom);
        } else {
            this.mGlow.setBounds(0, 0, this.mWidth, glowBottom);
        }
        this.mGlow.draw(canvas);
        this.mEdge.setAlpha((int) (Math.max(0.0f, Math.min(this.mEdgeAlpha, 1.0f)) * 255.0f));
        int edgeBottom = (int) (edgeHeight * this.mEdgeScaleY);
        if (this.mWidth < this.mMinWidth) {
            int edgeLeft = (this.mWidth - this.mMinWidth) / 2;
            this.mEdge.setBounds(edgeLeft, 0, this.mWidth - edgeLeft, edgeBottom);
        } else {
            this.mEdge.setBounds(0, 0, this.mWidth, edgeBottom);
        }
        this.mEdge.draw(canvas);
        return this.mState != 0;
    }

    private void update() {
        long time = AnimationTime.get();
        float t = Math.min((time - this.mStartTime) / this.mDuration, 1.0f);
        float interp = this.mInterpolator.getInterpolation(t);
        this.mEdgeAlpha = this.mEdgeAlphaStart + ((this.mEdgeAlphaFinish - this.mEdgeAlphaStart) * interp);
        this.mEdgeScaleY = this.mEdgeScaleYStart + ((this.mEdgeScaleYFinish - this.mEdgeScaleYStart) * interp);
        this.mGlowAlpha = this.mGlowAlphaStart + ((this.mGlowAlphaFinish - this.mGlowAlphaStart) * interp);
        this.mGlowScaleY = this.mGlowScaleYStart + ((this.mGlowScaleYFinish - this.mGlowScaleYStart) * interp);
        if (t >= 0.999f) {
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
                    float factor = this.mGlowScaleYFinish != 0.0f ? 1.0f / (this.mGlowScaleYFinish * this.mGlowScaleYFinish) : Float.MAX_VALUE;
                    this.mEdgeScaleY = this.mEdgeScaleYStart + ((this.mEdgeScaleYFinish - this.mEdgeScaleYStart) * interp * factor);
                    this.mState = 3;
                    break;
            }
        }
    }

    private static class Drawable extends ResourceTexture {
        private int mAlpha;
        private Rect mBounds;

        public Drawable(Context context, int resId) {
            super(context, resId);
            this.mBounds = new Rect();
            this.mAlpha = 255;
        }

        public int getIntrinsicWidth() {
            return getWidth();
        }

        public int getIntrinsicHeight() {
            return getHeight();
        }

        public void setBounds(int left, int top, int right, int bottom) {
            this.mBounds.set(left, top, right, bottom);
        }

        public void setAlpha(int alpha) {
            this.mAlpha = alpha;
        }

        public void draw(GLCanvas canvas) {
            canvas.save(1);
            canvas.multiplyAlpha(this.mAlpha / 255.0f);
            Rect b = this.mBounds;
            draw(canvas, b.left, b.top, b.width(), b.height());
            canvas.restore();
        }
    }
}
