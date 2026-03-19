package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.RectF;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.photos.data.GalleryBitmapPool;

public class TiledScreenNail implements ScreenNail {
    private static final long ANIMATION_DONE = -3;
    private static final long ANIMATION_NEEDED = -2;
    private static final long ANIMATION_NOT_NEEDED = -1;
    private static final int DURATION = 180;
    private static final String TAG = "Gallery2/TiledScreenNail";
    private Bitmap mBitmap;
    private int mHeight;
    private MediaItem mMediaItem;
    private TiledTexture mTexture;
    private int mWidth;
    private static int sMaxSide = 640;
    private static int mPlaceholderColor = -14540254;
    private static boolean mDrawPlaceholder = true;
    private long mAnimationStartTime = -1;
    public boolean isHidePlaceHolder = false;
    public ScreenNail mLowQualityScreenNail = null;
    private TiledScreenNail mCombineTarget = null;

    public TiledScreenNail(Bitmap bitmap) {
        this.mWidth = bitmap.getWidth();
        this.mHeight = bitmap.getHeight();
        this.mBitmap = bitmap;
        this.mTexture = new TiledTexture(bitmap);
    }

    public TiledScreenNail(int i, int i2) {
        setSize(i, i2);
    }

    public static void setPlaceholderColor(int i) {
        mPlaceholderColor = i;
    }

    private void setSize(int i, int i2) {
        if (i == 0 || i2 == 0) {
            i = sMaxSide;
            i2 = (sMaxSide * 3) / 4;
        }
        float fMin = Math.min(1.0f, sMaxSide / Math.max(i, i2));
        this.mWidth = Math.round(i * fMin);
        this.mHeight = Math.round(fMin * i2);
    }

    private ScreenNail innerCombine(ScreenNail screenNail) {
        if (screenNail == 0) {
            return this;
        }
        if (!(screenNail instanceof TiledScreenNail)) {
            recycle();
            return screenNail;
        }
        this.mWidth = screenNail.mWidth;
        this.mHeight = screenNail.mHeight;
        if (screenNail.mTexture != null) {
            if (this.mBitmap != null) {
                GalleryBitmapPool.getInstance().put(this.mBitmap);
            }
            if (this.mTexture != null) {
                this.mTexture.recycle();
            }
            this.mBitmap = screenNail.mBitmap;
            this.mTexture = screenNail.mTexture;
            screenNail.mBitmap = null;
            screenNail.mTexture = null;
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
        if (this.mCombineTarget != null) {
            this.mCombineTarget.recycle();
            this.mCombineTarget = null;
        }
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
    public void draw(GLCanvas gLCanvas, int i, int i2, int i3, int i4) throws Throwable {
        doCombine(this.mCombineTarget);
        if (this.mTexture == null || !this.mTexture.isReady()) {
            if (this.isHidePlaceHolder) {
                this.mLowQualityScreenNail.draw(gLCanvas, i, i2, i3, i4);
                return;
            }
            if (this.mAnimationStartTime == -1) {
                this.mAnimationStartTime = ANIMATION_NEEDED;
            }
            if (mDrawPlaceholder) {
                gLCanvas.fillRect(i, i2, i3, i4, mPlaceholderColor);
                return;
            }
            return;
        }
        if (this.mAnimationStartTime == ANIMATION_NEEDED) {
            this.mAnimationStartTime = AnimationTime.get();
        }
        if (isAnimating() && !this.isHidePlaceHolder) {
            this.mTexture.drawMixed(gLCanvas, mPlaceholderColor, getRatio(), i, i2, i3, i4);
        } else {
            this.mTexture.draw(gLCanvas, i, i2, i3, i4);
        }
    }

    @Override
    public void draw(GLCanvas gLCanvas, RectF rectF, RectF rectF2) throws Throwable {
        doCombine(this.mCombineTarget);
        if (this.mTexture == null || !this.mTexture.isReady()) {
            if (this.isHidePlaceHolder) {
                this.mLowQualityScreenNail.draw(gLCanvas, rectF, rectF2);
                return;
            } else {
                gLCanvas.fillRect(rectF2.left, rectF2.top, rectF2.width(), rectF2.height(), mPlaceholderColor);
                return;
            }
        }
        this.mTexture.draw(gLCanvas, rectF, rectF2);
    }

    @Override
    public boolean isAnimating() {
        if (this.isHidePlaceHolder) {
            return false;
        }
        if (this.mTexture == null || !this.mTexture.isReady()) {
            return true;
        }
        if (this.mAnimationStartTime < 0) {
            return false;
        }
        if (AnimationTime.get() - this.mAnimationStartTime < 180) {
            return true;
        }
        this.mAnimationStartTime = ANIMATION_DONE;
        return false;
    }

    private float getRatio() {
        return Utils.clamp(1.0f - ((AnimationTime.get() - this.mAnimationStartTime) / 180.0f), 0.0f, 1.0f);
    }

    public boolean isShowingPlaceholder() {
        return this.mBitmap == null || isAnimating();
    }

    public TiledTexture getTexture() {
        return this.mTexture;
    }

    public static void setMaxSide(int i) {
        sMaxSide = i;
    }

    public void enableDebug(boolean z) {
        if (this.mTexture != null) {
            this.mTexture.mEnableDrawCover = z;
        }
    }

    public void hidePlaceHolder() {
        Log.d(TAG, "<hidePlaceHolder> TiledSceenNail hide place holder =" + this + " isHidePlaceHolder=" + this.isHidePlaceHolder);
        this.isHidePlaceHolder = true;
    }

    public void setLowQualityScreenNail(ScreenNail screenNail) {
        this.mLowQualityScreenNail = screenNail;
    }

    public TiledScreenNail(Bitmap bitmap, MediaItem mediaItem) {
        this.mWidth = bitmap.getWidth();
        this.mHeight = bitmap.getHeight();
        this.mBitmap = bitmap;
        this.mTexture = new TiledTexture(bitmap);
        this.mMediaItem = mediaItem;
    }

    public TiledScreenNail(int i, int i2, MediaItem mediaItem) {
        setSize(i, i2);
        this.mMediaItem = mediaItem;
    }

    @Override
    public MediaItem getMediaItem() {
        return this.mMediaItem;
    }

    public ScreenNail combine(ScreenNail screenNail) {
        if (!(screenNail instanceof TiledScreenNail)) {
            recycle();
            return screenNail;
        }
        if (this.mTexture != null) {
            if (this.mCombineTarget != null) {
                this.mCombineTarget.recycle();
            }
            this.mCombineTarget = screenNail;
            return this;
        }
        return innerCombine(screenNail);
    }

    private void doCombine(ScreenNail screenNail) {
        if (this.mCombineTarget != null) {
            if (this.mCombineTarget.mTexture == null || this.mCombineTarget.mTexture.isReady()) {
                innerCombine(this.mCombineTarget);
                this.mCombineTarget = null;
            }
        }
    }

    boolean isPlaceHolderDrawingEnabled() {
        return mDrawPlaceholder;
    }
}
