package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.PointF;
import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.anim.FloatAnimation;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import java.util.Random;

public class SlideshowView extends GLView {
    private SlideshowAnimation mCurrentAnimation;
    private int mCurrentRotation;
    private BitmapTexture mCurrentTexture;
    private SlideshowAnimation mPrevAnimation;
    private int mPrevRotation;
    private BitmapTexture mPrevTexture;
    private final FloatAnimation mTransitionAnimation = new FloatAnimation(0.0f, 1.0f, 1000);
    private Random mRandom = new Random();

    public void next(Bitmap bitmap, int rotation) {
        this.mTransitionAnimation.start();
        if (this.mPrevTexture != null) {
            this.mPrevTexture.getBitmap().recycle();
            this.mPrevTexture.recycle();
        }
        this.mPrevTexture = this.mCurrentTexture;
        this.mPrevAnimation = this.mCurrentAnimation;
        this.mPrevRotation = this.mCurrentRotation;
        this.mCurrentRotation = rotation;
        this.mCurrentTexture = new BitmapTexture(bitmap);
        if (((rotation / 90) & 1) == 0) {
            this.mCurrentAnimation = new SlideshowAnimation(this.mCurrentTexture.getWidth(), this.mCurrentTexture.getHeight(), this.mRandom);
        } else {
            this.mCurrentAnimation = new SlideshowAnimation(this.mCurrentTexture.getHeight(), this.mCurrentTexture.getWidth(), this.mRandom);
        }
        this.mCurrentAnimation.start();
        invalidate();
    }

    public void release() {
        if (this.mPrevTexture != null) {
            this.mPrevTexture.recycle();
            this.mPrevTexture = null;
        }
        if (this.mCurrentTexture != null) {
            this.mCurrentTexture.recycle();
            this.mCurrentTexture = null;
        }
    }

    @Override
    protected void render(GLCanvas canvas) {
        long animTime = AnimationTime.get();
        boolean requestRender = this.mTransitionAnimation.calculate(animTime);
        float alpha = this.mPrevTexture == null ? 1.0f : this.mTransitionAnimation.get();
        if (this.mPrevTexture != null && alpha != 1.0f) {
            requestRender |= this.mPrevAnimation.calculate(animTime);
            canvas.save(3);
            canvas.setAlpha(1.0f - alpha);
            this.mPrevAnimation.apply(canvas);
            canvas.rotate(this.mPrevRotation, 0.0f, 0.0f, 1.0f);
            this.mPrevTexture.draw(canvas, (-this.mPrevTexture.getWidth()) / 2, (-this.mPrevTexture.getHeight()) / 2);
            canvas.restore();
        }
        if (this.mCurrentTexture != null) {
            requestRender |= this.mCurrentAnimation.calculate(animTime);
            canvas.save(3);
            canvas.setAlpha(alpha);
            this.mCurrentAnimation.apply(canvas);
            canvas.rotate(this.mCurrentRotation, 0.0f, 0.0f, 1.0f);
            this.mCurrentTexture.draw(canvas, (-this.mCurrentTexture.getWidth()) / 2, (-this.mCurrentTexture.getHeight()) / 2);
            canvas.restore();
        }
        if (requestRender) {
            invalidate();
        }
    }

    private class SlideshowAnimation extends CanvasAnimation {
        private final int mHeight;
        private final PointF mMovingVector;
        private float mProgress;
        private final int mWidth;

        public SlideshowAnimation(int width, int height, Random random) {
            this.mWidth = width;
            this.mHeight = height;
            this.mMovingVector = new PointF(this.mWidth * 0.2f * (random.nextFloat() - 0.5f), this.mHeight * 0.2f * (random.nextFloat() - 0.5f));
            setDuration(3500);
        }

        @Override
        public void apply(GLCanvas canvas) {
            int viewWidth = SlideshowView.this.getWidth();
            int viewHeight = SlideshowView.this.getHeight();
            float initScale = Math.min(viewWidth / this.mWidth, viewHeight / this.mHeight);
            float scale = initScale * (1.0f + (0.2f * this.mProgress));
            float centerX = (viewWidth / 2) + (this.mMovingVector.x * this.mProgress);
            float centerY = (viewHeight / 2) + (this.mMovingVector.y * this.mProgress);
            canvas.translate(centerX, centerY);
            canvas.scale(scale, scale, 0.0f);
        }

        @Override
        public int getCanvasSaveFlags() {
            return 2;
        }

        @Override
        protected void onCalculate(float progress) {
            this.mProgress = progress;
        }
    }
}
