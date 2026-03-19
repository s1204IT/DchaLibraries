package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.RectF;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.GLCanvas;

public class BitmapScreenNail implements ScreenNail {
    private static final long ANIMATION_DONE = -3;
    private static final long ANIMATION_NEEDED = -2;
    private static final long ANIMATION_NOT_NEEDED = -1;
    private static final int DURATION = 180;
    protected static final int PLACEHOLDER_COLOR = -14540254;
    private static final String TAG = "Gallery2/BitmapScreenNail";
    protected Bitmap mBitmap;
    protected int mHeight;
    private MediaItem mMediaItem;
    protected BitmapTexture mTexture;
    protected int mWidth;
    private static int sMaxSide = 640;
    public static long mWaitFinishedTime = 0;
    protected long mAnimationStartTime = -1;
    private boolean mIsDebugEnable = false;

    public BitmapScreenNail(Bitmap bitmap) {
        this.mWidth = bitmap.getWidth();
        this.mHeight = bitmap.getHeight();
        this.mBitmap = bitmap;
    }

    @Override
    public int getWidth() {
        return this.mWidth;
    }

    @Override
    public int getHeight() {
        return this.mHeight;
    }

    @Override
    public void draw(GLCanvas gLCanvas, int i, int i2, int i3, int i4) {
        if (this.mBitmap == null) {
            if (this.mAnimationStartTime == -1) {
                this.mAnimationStartTime = ANIMATION_NEEDED;
            }
            gLCanvas.fillRect(i, i2, i3, i4, PLACEHOLDER_COLOR);
            mWaitFinishedTime = System.currentTimeMillis();
            return;
        }
        if (this.mTexture == null) {
            this.mTexture = new BitmapTexture(true, this.mBitmap);
        }
        if (this.mAnimationStartTime == ANIMATION_NEEDED) {
            this.mAnimationStartTime = now();
        }
        if (isAnimating()) {
            gLCanvas.drawMixed(this.mTexture, PLACEHOLDER_COLOR, getRatio(), i, i2, i3, i4);
        } else {
            this.mTexture.draw(gLCanvas, i, i2, i3, i4);
        }
        if (this.mIsDebugEnable) {
            gLCanvas.fillRect(i, i2, i3, i4, 1717960704);
        }
    }

    @Override
    public void noDraw() {
    }

    @Override
    public void recycle() {
        if (this.mTexture != null) {
            this.mTexture.recycle();
            this.mTexture = null;
        }
        this.mBitmap = null;
    }

    @Override
    public void draw(GLCanvas gLCanvas, RectF rectF, RectF rectF2) {
        if (this.mBitmap == null) {
            gLCanvas.fillRect(rectF2.left, rectF2.top, rectF2.width(), rectF2.height(), PLACEHOLDER_COLOR);
            return;
        }
        if (this.mTexture == null) {
            this.mTexture = new BitmapTexture(true, this.mBitmap);
        }
        gLCanvas.drawTexture(this.mTexture, rectF, rectF2);
        if (this.mIsDebugEnable) {
            gLCanvas.fillRect(rectF2.left, rectF2.top, rectF2.width(), rectF2.height(), 1717960704);
        }
    }

    public BitmapScreenNail(int i, int i2) {
        setSize(i, i2);
    }

    public void setSize(int i, int i2) {
        if (i == 0 || i2 == 0) {
            i = sMaxSide;
            i2 = (sMaxSide * 3) / 4;
        }
        float fMin = Math.min(1.0f, sMaxSide / Math.max(i, i2));
        this.mWidth = Math.round(i * fMin);
        this.mHeight = Math.round(fMin * i2);
    }

    public ScreenNail combine(ScreenNail screenNail) {
        if (screenNail == 0) {
            return this;
        }
        if (!(screenNail instanceof BitmapScreenNail)) {
            recycle();
            return screenNail;
        }
        this.mWidth = screenNail.mWidth;
        this.mHeight = screenNail.mHeight;
        if (screenNail.mBitmap != null) {
            this.mBitmap = screenNail.mBitmap;
            screenNail.mBitmap = null;
            if (this.mTexture != null) {
                this.mTexture.recycle();
                this.mTexture = null;
            }
        }
        screenNail.recycle();
        return this;
    }

    public void updatePlaceholderSize(int i, int i2) {
        if (this.mBitmap != null || i == 0 || i2 == 0) {
            return;
        }
        setSize(i, i2);
    }

    private static long now() {
        return AnimationTime.get();
    }

    @Override
    public boolean isAnimating() {
        if (this.mAnimationStartTime < 0) {
            return false;
        }
        if (now() - this.mAnimationStartTime >= 180) {
            this.mAnimationStartTime = ANIMATION_DONE;
            return false;
        }
        return true;
    }

    private float getRatio() {
        return Utils.clamp(1.0f - ((now() - this.mAnimationStartTime) / 180.0f), 0.0f, 1.0f);
    }

    public BitmapScreenNail(Bitmap bitmap, MediaItem mediaItem) {
        this.mWidth = bitmap.getWidth();
        this.mHeight = bitmap.getHeight();
        this.mBitmap = bitmap;
        this.mMediaItem = mediaItem;
    }

    public BitmapScreenNail(int i, int i2, MediaItem mediaItem) {
        setSize(i, i2);
        this.mMediaItem = mediaItem;
    }

    @Override
    public MediaItem getMediaItem() {
        return this.mMediaItem;
    }

    public void setDebugEnable(boolean z) {
        this.mIsDebugEnable = z;
    }
}
