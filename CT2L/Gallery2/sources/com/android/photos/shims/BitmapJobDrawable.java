package com.android.photos.shims;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.BitmapLoader;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;

public class BitmapJobDrawable extends Drawable implements Runnable {
    private Bitmap mBitmap;
    private MediaItem mItem;
    private ThumbnailLoader mLoader;
    private Paint mPaint = new Paint();
    private Matrix mDrawMatrix = new Matrix();
    private int mRotation = 0;

    public void setMediaItem(MediaItem item) {
        if (this.mItem != item) {
            if (this.mLoader != null) {
                this.mLoader.cancelLoad();
            }
            this.mItem = item;
            if (this.mBitmap != null) {
                GalleryBitmapPool.getInstance().put(this.mBitmap);
                this.mBitmap = null;
            }
            if (this.mItem != null) {
                this.mLoader = new ThumbnailLoader(this);
                this.mLoader.startLoad();
                this.mRotation = this.mItem.getRotation();
            }
            invalidateSelf();
        }
    }

    @Override
    public void run() {
        Bitmap bitmap = this.mLoader.getBitmap();
        if (bitmap != null) {
            this.mBitmap = bitmap;
            updateDrawMatrix();
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        updateDrawMatrix();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (this.mBitmap != null) {
            canvas.save();
            canvas.clipRect(bounds);
            canvas.concat(this.mDrawMatrix);
            canvas.rotate(this.mRotation, bounds.centerX(), bounds.centerY());
            canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, this.mPaint);
            canvas.restore();
            return;
        }
        this.mPaint.setColor(-3355444);
        canvas.drawRect(bounds, this.mPaint);
    }

    private void updateDrawMatrix() {
        float scale;
        Rect bounds = getBounds();
        if (this.mBitmap == null || bounds.isEmpty()) {
            this.mDrawMatrix.reset();
            return;
        }
        float dx = 0.0f;
        float dy = 0.0f;
        int dwidth = this.mBitmap.getWidth();
        int dheight = this.mBitmap.getHeight();
        int vwidth = bounds.width();
        int vheight = bounds.height();
        if (dwidth * vheight > vwidth * dheight) {
            scale = vheight / dheight;
            dx = (vwidth - (dwidth * scale)) * 0.5f;
        } else {
            scale = vwidth / dwidth;
            dy = (vheight - (dheight * scale)) * 0.5f;
        }
        this.mDrawMatrix.setScale(scale, scale);
        this.mDrawMatrix.postTranslate((int) (dx + 0.5f), (int) (0.5f + dy));
        invalidateSelf();
    }

    @Override
    public int getIntrinsicWidth() {
        return MediaItem.getTargetSize(2);
    }

    @Override
    public int getIntrinsicHeight() {
        return MediaItem.getTargetSize(2);
    }

    @Override
    public int getOpacity() {
        Bitmap bm = this.mBitmap;
        return (bm == null || bm.hasAlpha() || this.mPaint.getAlpha() < 255) ? -3 : -1;
    }

    @Override
    public void setAlpha(int alpha) {
        int oldAlpha = this.mPaint.getAlpha();
        if (alpha != oldAlpha) {
            this.mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    private static class ThumbnailLoader extends BitmapLoader {
        private static final ThreadPool sThreadPool = new ThreadPool(0, 2);
        private BitmapJobDrawable mParent;

        public ThumbnailLoader(BitmapJobDrawable parent) {
            this.mParent = parent;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> l) {
            return sThreadPool.submit(this.mParent.mItem.requestImage(2), this);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            this.mParent.scheduleSelf(this.mParent, 0L);
        }
    }
}
