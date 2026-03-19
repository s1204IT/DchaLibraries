package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import com.android.gallery3d.R;
import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.anim.FloatAnimation;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallerybasic.base.ISlideshowRenderer;
import java.util.Random;

public class SlideshowView extends GLView {
    private static final float MOVE_SPEED = 0.2f;
    private static final float SCALE_SPEED = 0.2f;
    private static final int SLIDESHOW_DURATION = 3500;
    private static final String TAG = "Gallery2/SlideshowView";
    private static final int TRANSITION_DURATION = 1000;
    private Context mContext;
    private SlideshowAnimation mCurrentAnimation;
    private MediaItem mCurrentMediaItem;
    private int mCurrentRotation;
    private BitmapTexture mCurrentTexture;
    private ISlideshowRenderer[] mExtRenders;
    private SlideshowAnimation mPrevAnimation;
    private MediaItem mPrevMediaItem;
    private int mPrevRotation;
    private BitmapTexture mPrevTexture;
    private Texture mVideoPlayIcon;
    private final FloatAnimation mTransitionAnimation = new FloatAnimation(0.0f, 1.0f, TRANSITION_DURATION);
    private Random mRandom = new Random();

    public SlideshowView(AbstractGalleryActivity abstractGalleryActivity) {
        this.mContext = abstractGalleryActivity.getAndroidContext();
        this.mVideoPlayIcon = new ResourceTexture(this.mContext, R.drawable.ic_control_play);
        this.mExtRenders = (ISlideshowRenderer[]) FeatureManager.getInstance().getImplement(ISlideshowRenderer.class, this.mContext, this.mContext.getResources());
    }

    public void next(Bitmap bitmap, int i) {
        this.mTransitionAnimation.start();
        if (this.mPrevTexture != null) {
            this.mPrevTexture.getBitmap().recycle();
            this.mPrevTexture.recycle();
        }
        this.mPrevTexture = this.mCurrentTexture;
        this.mPrevAnimation = this.mCurrentAnimation;
        this.mPrevRotation = this.mCurrentRotation;
        this.mCurrentRotation = i;
        this.mCurrentTexture = new BitmapTexture(bitmap);
        if (((i / 90) & 1) == 0) {
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
    protected void render(GLCanvas gLCanvas) {
        float f;
        long j = AnimationTime.get();
        boolean zCalculate = this.mTransitionAnimation.calculate(j);
        if (this.mPrevTexture != null) {
            f = this.mTransitionAnimation.get();
        } else {
            f = 1.0f;
        }
        if (this.mPrevTexture != null && f != 1.0f) {
            zCalculate |= this.mPrevAnimation.calculate(j);
            gLCanvas.save(3);
            gLCanvas.setAlpha(1.0f - f);
            this.mPrevAnimation.apply(gLCanvas);
            gLCanvas.rotate(this.mPrevRotation, 0.0f, 0.0f, 1.0f);
            boolean zRenderPrevContent = false;
            for (ISlideshowRenderer iSlideshowRenderer : this.mExtRenders) {
                zRenderPrevContent = iSlideshowRenderer.renderPrevContent(gLCanvas.getMGLCanvas(), this.mPrevTexture.getWidth(), this.mPrevTexture.getHeight(), this.mPrevMediaItem.getMediaData());
            }
            if (!zRenderPrevContent) {
                this.mPrevTexture.draw(gLCanvas, (-this.mPrevTexture.getWidth()) / 2, (-this.mPrevTexture.getHeight()) / 2);
            }
            for (ISlideshowRenderer iSlideshowRenderer2 : this.mExtRenders) {
                iSlideshowRenderer2.renderPrevCover(gLCanvas.getMGLCanvas(), (-this.mPrevTexture.getWidth()) / 2, (-this.mPrevTexture.getHeight()) / 2, this.mPrevTexture.getWidth(), this.mPrevTexture.getHeight(), this.mPrevMediaItem.getMediaData(), 1.0f / this.mPrevAnimation.getCurrentScale());
            }
            gLCanvas.restore();
            if (this.mPrevMediaItem.getMediaType() == 4) {
                drawVideoOverlay(gLCanvas);
            }
        }
        if (this.mCurrentTexture != null) {
            zCalculate |= this.mCurrentAnimation.calculate(j);
            gLCanvas.save(3);
            gLCanvas.setAlpha(f);
            this.mCurrentAnimation.apply(gLCanvas);
            gLCanvas.rotate(this.mCurrentRotation, 0.0f, 0.0f, 1.0f);
            boolean zRenderCurrentContent = false;
            for (ISlideshowRenderer iSlideshowRenderer3 : this.mExtRenders) {
                zRenderCurrentContent = iSlideshowRenderer3.renderCurrentContent(gLCanvas.getMGLCanvas(), this.mCurrentTexture.getWidth(), this.mCurrentTexture.getHeight(), this.mCurrentMediaItem.getMediaData());
            }
            if (!zRenderCurrentContent) {
                this.mCurrentTexture.draw(gLCanvas, (-this.mCurrentTexture.getWidth()) / 2, (-this.mCurrentTexture.getHeight()) / 2);
            }
            for (ISlideshowRenderer iSlideshowRenderer4 : this.mExtRenders) {
                iSlideshowRenderer4.renderCurrentCover(gLCanvas.getMGLCanvas(), (-this.mCurrentTexture.getWidth()) / 2, (-this.mCurrentTexture.getHeight()) / 2, this.mCurrentTexture.getWidth(), this.mCurrentTexture.getHeight(), this.mCurrentMediaItem.getMediaData(), 1.0f / this.mCurrentAnimation.getCurrentScale());
            }
            gLCanvas.restore();
            if (this.mCurrentMediaItem.getMediaType() == 4) {
                drawVideoOverlay(gLCanvas);
            }
        }
        if (zCalculate) {
            invalidate();
        }
    }

    private class SlideshowAnimation extends CanvasAnimation {
        private float mCurrentScale;
        private final int mHeight;
        private MediaItem mMediaItem;
        private final PointF mMovingVector;
        private float mProgress;
        private final int mWidth;

        public SlideshowAnimation(int i, int i2, Random random) {
            this.mCurrentScale = 1.0f;
            this.mWidth = i;
            this.mHeight = i2;
            this.mMovingVector = new PointF(this.mWidth * 0.2f * (random.nextFloat() - 0.5f), 0.2f * this.mHeight * (random.nextFloat() - 0.5f));
            setDuration(SlideshowView.SLIDESHOW_DURATION);
        }

        public SlideshowAnimation(SlideshowView slideshowView, int i, int i2, Random random, MediaItem mediaItem) {
            this(i, i2, random);
            this.mMediaItem = mediaItem;
        }

        @Override
        public void apply(GLCanvas gLCanvas) {
            float fMin = Math.min(SlideshowView.this.getWidth() / this.mWidth, SlideshowView.this.getHeight() / this.mHeight);
            float f = (1.0f + (0.2f * this.mProgress)) * fMin;
            this.mCurrentScale = fMin;
            gLCanvas.translate((r0 / 2) + (this.mMovingVector.x * this.mProgress), (r1 / 2) + (this.mMovingVector.y * this.mProgress));
            gLCanvas.scale(f, f, 0.0f);
        }

        @Override
        public int getCanvasSaveFlags() {
            return 2;
        }

        @Override
        protected void onCalculate(float f) {
            this.mProgress = f;
        }

        public float getCurrentScale() {
            return this.mCurrentScale;
        }
    }

    public void next(Bitmap bitmap, int i, MediaItem mediaItem) {
        this.mTransitionAnimation.start();
        if (this.mPrevTexture != null) {
            this.mPrevTexture.getBitmap().recycle();
            this.mPrevTexture.recycle();
        }
        this.mPrevTexture = this.mCurrentTexture;
        this.mPrevAnimation = this.mCurrentAnimation;
        this.mPrevRotation = this.mCurrentRotation;
        this.mPrevMediaItem = this.mCurrentMediaItem;
        this.mCurrentMediaItem = mediaItem;
        this.mCurrentRotation = i;
        this.mCurrentTexture = new BitmapTexture(bitmap);
        if (((i / 90) & 1) == 0) {
            this.mCurrentAnimation = new SlideshowAnimation(this, this.mCurrentTexture.getWidth(), this.mCurrentTexture.getHeight(), this.mRandom, this.mCurrentMediaItem);
        } else {
            this.mCurrentAnimation = new SlideshowAnimation(this, this.mCurrentTexture.getHeight(), this.mCurrentTexture.getWidth(), this.mRandom, this.mCurrentMediaItem);
        }
        this.mCurrentAnimation.start();
        invalidate();
    }

    protected void drawVideoOverlay(GLCanvas gLCanvas) {
        int iMin = ((int) (Math.min(r0, r1) + 0.5f)) / 6;
        this.mVideoPlayIcon.draw(gLCanvas, (getWidth() - iMin) / 2, (getHeight() - iMin) / 2, iMin, iMin);
    }
}
