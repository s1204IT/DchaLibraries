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

    public void setMediaItem(MediaItem mediaItem) {
        if (this.mItem == mediaItem) {
            return;
        }
        if (this.mLoader != null) {
            this.mLoader.cancelLoad();
        }
        this.mItem = mediaItem;
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

    @Override
    public void run() {
        Bitmap bitmap = this.mLoader.getBitmap();
        if (bitmap != null) {
            this.mBitmap = bitmap;
            updateDrawMatrix();
        }
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
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
        float f;
        float f2;
        Rect bounds = getBounds();
        if (this.mBitmap == null || bounds.isEmpty()) {
            this.mDrawMatrix.reset();
            return;
        }
        int width = this.mBitmap.getWidth();
        int height = this.mBitmap.getHeight();
        int iWidth = bounds.width();
        int iHeight = bounds.height();
        float f3 = 0.0f;
        if (width * iHeight > iWidth * height) {
            f = iHeight / height;
            f2 = (iWidth - (width * f)) * 0.5f;
        } else {
            float f4 = iWidth / width;
            f3 = (iHeight - (height * f4)) * 0.5f;
            f = f4;
            f2 = 0.0f;
        }
        this.mDrawMatrix.setScale(f, f);
        this.mDrawMatrix.postTranslate((int) (f2 + 0.5f), (int) (f3 + 0.5f));
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
        Bitmap bitmap = this.mBitmap;
        return (bitmap == null || bitmap.hasAlpha() || this.mPaint.getAlpha() < 255) ? -3 : -1;
    }

    @Override
    public void setAlpha(int i) {
        if (i != this.mPaint.getAlpha()) {
            this.mPaint.setAlpha(i);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    private static class ThumbnailLoader extends BitmapLoader {
        private static final ThreadPool sThreadPool = new ThreadPool(0, 2);
        private BitmapJobDrawable mParent;

        public ThumbnailLoader(BitmapJobDrawable bitmapJobDrawable) {
            this.mParent = bitmapJobDrawable;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> futureListener) {
            return sThreadPool.submit(this.mParent.mItem.requestImage(2), this);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            this.mParent.scheduleSelf(this.mParent, 0L);
        }
    }
}
