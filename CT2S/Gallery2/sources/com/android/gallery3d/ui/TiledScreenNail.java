package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.RectF;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.photos.data.GalleryBitmapPool;

public class TiledScreenNail implements ScreenNail {
    private long mAnimationStartTime = -1;
    private Bitmap mBitmap;
    private int mHeight;
    private TiledTexture mTexture;
    private int mWidth;
    private static int sMaxSide = 640;
    private static int mPlaceholderColor = -14540254;
    private static boolean mDrawPlaceholder = true;

    public TiledScreenNail(Bitmap bitmap) {
        this.mWidth = bitmap.getWidth();
        this.mHeight = bitmap.getHeight();
        this.mBitmap = bitmap;
        this.mTexture = new TiledTexture(bitmap);
    }

    public TiledScreenNail(int width, int height) {
        setSize(width, height);
    }

    public static void setPlaceholderColor(int color) {
        mPlaceholderColor = color;
    }

    private void setSize(int width, int height) {
        if (width == 0 || height == 0) {
            width = sMaxSide;
            height = (sMaxSide * 3) / 4;
        }
        float scale = Math.min(1.0f, sMaxSide / Math.max(width, height));
        this.mWidth = Math.round(width * scale);
        this.mHeight = Math.round(height * scale);
    }

    public ScreenNail combine(ScreenNail other) {
        if (other != null) {
            if (!(other instanceof TiledScreenNail)) {
                recycle();
                return other;
            }
            TiledScreenNail newer = (TiledScreenNail) other;
            this.mWidth = newer.mWidth;
            this.mHeight = newer.mHeight;
            if (newer.mTexture != null) {
                if (this.mBitmap != null) {
                    GalleryBitmapPool.getInstance().put(this.mBitmap);
                }
                if (this.mTexture != null) {
                    this.mTexture.recycle();
                }
                this.mBitmap = newer.mBitmap;
                this.mTexture = newer.mTexture;
                newer.mBitmap = null;
                newer.mTexture = null;
            }
            newer.recycle();
            return this;
        }
        return this;
    }

    public void updatePlaceholderSize(int width, int height) {
        if (this.mBitmap == null && width != 0 && height != 0) {
            setSize(width, height);
        }
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
    public void noDraw() {
    }

    @Override
    public void recycle() {
        if (this.mTexture != null) {
            this.mTexture.recycle();
            this.mTexture = null;
        }
        if (this.mBitmap != null) {
            GalleryBitmapPool.getInstance().put(this.mBitmap);
            this.mBitmap = null;
        }
    }

    public static void disableDrawPlaceholder() {
        mDrawPlaceholder = false;
    }

    public static void enableDrawPlaceholder() {
        mDrawPlaceholder = true;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        if (this.mTexture == null || !this.mTexture.isReady()) {
            if (this.mAnimationStartTime == -1) {
                this.mAnimationStartTime = -2L;
            }
            if (mDrawPlaceholder) {
                canvas.fillRect(x, y, width, height, mPlaceholderColor);
                return;
            }
            return;
        }
        if (this.mAnimationStartTime == -2) {
            this.mAnimationStartTime = AnimationTime.get();
        }
        if (isAnimating()) {
            this.mTexture.drawMixed(canvas, mPlaceholderColor, getRatio(), x, y, width, height);
        } else {
            this.mTexture.draw(canvas, x, y, width, height);
        }
    }

    @Override
    public void draw(GLCanvas canvas, RectF source, RectF dest) {
        if (this.mTexture == null || !this.mTexture.isReady()) {
            canvas.fillRect(dest.left, dest.top, dest.width(), dest.height(), mPlaceholderColor);
        } else {
            this.mTexture.draw(canvas, source, dest);
        }
    }

    public boolean isAnimating() {
        if (this.mTexture == null || !this.mTexture.isReady()) {
            return true;
        }
        if (this.mAnimationStartTime < 0) {
            return false;
        }
        if (AnimationTime.get() - this.mAnimationStartTime < 180) {
            return true;
        }
        this.mAnimationStartTime = -3L;
        return false;
    }

    private float getRatio() {
        float r = (AnimationTime.get() - this.mAnimationStartTime) / 180.0f;
        return Utils.clamp(1.0f - r, 0.0f, 1.0f);
    }

    public TiledTexture getTexture() {
        return this.mTexture;
    }

    public static void setMaxSide(int size) {
        sMaxSide = size;
    }
}
