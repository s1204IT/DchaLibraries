package com.android.photos.drawables;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.android.photos.data.GalleryBitmapPool;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AutoThumbnailDrawable<T> extends Drawable {
    private Bitmap mBitmap;
    protected T mData;
    private int mImageHeight;
    private int mImageWidth;
    private boolean mIsQueued;
    private static ExecutorService sThreadPool = Executors.newSingleThreadExecutor();
    private static GalleryBitmapPool sBitmapPool = GalleryBitmapPool.getInstance();
    private static byte[] sTempStorage = new byte[65536];
    private Paint mPaint = new Paint();
    private Matrix mDrawMatrix = new Matrix();
    private BitmapFactory.Options mOptions = new BitmapFactory.Options();
    private Object mLock = new Object();
    private Rect mBounds = new Rect();
    private int mSampleSize = 1;
    private final Runnable mLoadBitmap = new Runnable() {
        @Override
        public void run() {
            T data;
            synchronized (AutoThumbnailDrawable.this.mLock) {
                data = AutoThumbnailDrawable.this.mData;
            }
            int preferredSampleSize = 1;
            byte[] preferred = AutoThumbnailDrawable.this.getPreferredImageBytes(data);
            boolean hasPreferred = preferred != null && preferred.length > 0;
            if (hasPreferred) {
                AutoThumbnailDrawable.this.mOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(preferred, 0, preferred.length, AutoThumbnailDrawable.this.mOptions);
                AutoThumbnailDrawable.this.mOptions.inJustDecodeBounds = false;
            }
            synchronized (AutoThumbnailDrawable.this.mLock) {
                if (!AutoThumbnailDrawable.this.dataChangedLocked(data)) {
                    int width = AutoThumbnailDrawable.this.mImageWidth;
                    int height = AutoThumbnailDrawable.this.mImageHeight;
                    if (hasPreferred) {
                        preferredSampleSize = AutoThumbnailDrawable.this.calculateSampleSizeLocked(AutoThumbnailDrawable.this.mOptions.outWidth, AutoThumbnailDrawable.this.mOptions.outHeight);
                    }
                    int sampleSize = AutoThumbnailDrawable.this.calculateSampleSizeLocked(width, height);
                    AutoThumbnailDrawable.this.mIsQueued = false;
                    Bitmap b = null;
                    InputStream is = null;
                    if (hasPreferred) {
                        try {
                            try {
                                AutoThumbnailDrawable.this.mOptions.inSampleSize = preferredSampleSize;
                                AutoThumbnailDrawable.this.mOptions.inBitmap = AutoThumbnailDrawable.sBitmapPool.get(AutoThumbnailDrawable.this.mOptions.outWidth / preferredSampleSize, AutoThumbnailDrawable.this.mOptions.outHeight / preferredSampleSize);
                                b = BitmapFactory.decodeByteArray(preferred, 0, preferred.length, AutoThumbnailDrawable.this.mOptions);
                                if (AutoThumbnailDrawable.this.mOptions.inBitmap != null && b != AutoThumbnailDrawable.this.mOptions.inBitmap) {
                                    AutoThumbnailDrawable.sBitmapPool.put(AutoThumbnailDrawable.this.mOptions.inBitmap);
                                    AutoThumbnailDrawable.this.mOptions.inBitmap = null;
                                }
                            } catch (Exception e) {
                                Log.d("AutoThumbnailDrawable", "Failed to fetch bitmap", e);
                                if (is != null) {
                                    try {
                                        is.close();
                                    } catch (Exception e2) {
                                    }
                                }
                                if (b != null) {
                                    synchronized (AutoThumbnailDrawable.this.mLock) {
                                        if (!AutoThumbnailDrawable.this.dataChangedLocked(data)) {
                                            AutoThumbnailDrawable.this.setBitmapLocked(b);
                                            AutoThumbnailDrawable.this.scheduleSelf(AutoThumbnailDrawable.this.mUpdateBitmap, 0L);
                                        }
                                        return;
                                    }
                                }
                                return;
                            }
                        } catch (Throwable th) {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (Exception e3) {
                                }
                            }
                            if (b == null) {
                                throw th;
                            }
                            synchronized (AutoThumbnailDrawable.this.mLock) {
                                if (!AutoThumbnailDrawable.this.dataChangedLocked(data)) {
                                    AutoThumbnailDrawable.this.setBitmapLocked(b);
                                    AutoThumbnailDrawable.this.scheduleSelf(AutoThumbnailDrawable.this.mUpdateBitmap, 0L);
                                }
                                throw th;
                            }
                        }
                    }
                    if (b == null) {
                        is = AutoThumbnailDrawable.this.getFallbackImageStream(data);
                        AutoThumbnailDrawable.this.mOptions.inSampleSize = sampleSize;
                        AutoThumbnailDrawable.this.mOptions.inBitmap = AutoThumbnailDrawable.sBitmapPool.get(width / sampleSize, height / sampleSize);
                        b = BitmapFactory.decodeStream(is, null, AutoThumbnailDrawable.this.mOptions);
                        if (AutoThumbnailDrawable.this.mOptions.inBitmap != null && b != AutoThumbnailDrawable.this.mOptions.inBitmap) {
                            AutoThumbnailDrawable.sBitmapPool.put(AutoThumbnailDrawable.this.mOptions.inBitmap);
                            AutoThumbnailDrawable.this.mOptions.inBitmap = null;
                        }
                    }
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception e4) {
                        }
                    }
                    if (b != null) {
                        synchronized (AutoThumbnailDrawable.this.mLock) {
                            if (!AutoThumbnailDrawable.this.dataChangedLocked(data)) {
                                AutoThumbnailDrawable.this.setBitmapLocked(b);
                                AutoThumbnailDrawable.this.scheduleSelf(AutoThumbnailDrawable.this.mUpdateBitmap, 0L);
                            }
                        }
                    }
                }
            }
        }
    };
    private final Runnable mUpdateBitmap = new Runnable() {
        @Override
        public void run() {
            synchronized (AutoThumbnailDrawable.this) {
                AutoThumbnailDrawable.this.updateDrawMatrixLocked();
                AutoThumbnailDrawable.this.invalidateSelf();
            }
        }
    };

    protected abstract boolean dataChangedLocked(T t);

    protected abstract InputStream getFallbackImageStream(T t);

    protected abstract byte[] getPreferredImageBytes(T t);

    public AutoThumbnailDrawable() {
        this.mPaint.setAntiAlias(true);
        this.mPaint.setFilterBitmap(true);
        this.mDrawMatrix.reset();
        this.mOptions.inTempStorage = sTempStorage;
    }

    public void setImage(T data, int width, int height) {
        if (dataChangedLocked(data)) {
            synchronized (this.mLock) {
                this.mImageWidth = width;
                this.mImageHeight = height;
                this.mData = data;
                setBitmapLocked(null);
                refreshSampleSizeLocked();
            }
            invalidateSelf();
        }
    }

    private void setBitmapLocked(Bitmap b) {
        if (b != this.mBitmap) {
            if (this.mBitmap != null) {
                sBitmapPool.put(this.mBitmap);
            }
            this.mBitmap = b;
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        synchronized (this.mLock) {
            this.mBounds.set(bounds);
            if (this.mBounds.isEmpty()) {
                this.mBitmap = null;
            } else {
                refreshSampleSizeLocked();
                updateDrawMatrixLocked();
            }
        }
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mBitmap != null) {
            canvas.save();
            canvas.clipRect(this.mBounds);
            canvas.concat(this.mDrawMatrix);
            canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, this.mPaint);
            canvas.restore();
        }
    }

    private void updateDrawMatrixLocked() {
        float scale;
        if (this.mBitmap == null || this.mBounds.isEmpty()) {
            this.mDrawMatrix.reset();
            return;
        }
        float dx = 0.0f;
        float dy = 0.0f;
        int dwidth = this.mBitmap.getWidth();
        int dheight = this.mBitmap.getHeight();
        int vwidth = this.mBounds.width();
        int vheight = this.mBounds.height();
        if (dwidth * vheight > vwidth * dheight) {
            scale = vheight / dheight;
            dx = (vwidth - (dwidth * scale)) * 0.5f;
        } else {
            scale = vwidth / dwidth;
            dy = (vheight - (dheight * scale)) * 0.5f;
        }
        if (scale < 0.8f) {
            Log.w("AutoThumbnailDrawable", "sample size was too small! Overdrawing! " + scale + ", " + this.mSampleSize);
        } else if (scale > 1.5f) {
            Log.w("AutoThumbnailDrawable", "Potential quality loss! " + scale + ", " + this.mSampleSize);
        }
        this.mDrawMatrix.setScale(scale, scale);
        this.mDrawMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }

    private int calculateSampleSizeLocked(int dwidth, int dheight) {
        float scale;
        int vwidth = this.mBounds.width();
        int vheight = this.mBounds.height();
        if (dwidth * vheight > vwidth * dheight) {
            scale = dheight / vheight;
        } else {
            scale = dwidth / vwidth;
        }
        int result = Math.round(scale);
        if (result > 0) {
            return result;
        }
        return 1;
    }

    private void refreshSampleSizeLocked() {
        if (!this.mBounds.isEmpty() && this.mImageWidth != 0 && this.mImageHeight != 0) {
            int sampleSize = calculateSampleSizeLocked(this.mImageWidth, this.mImageHeight);
            if (sampleSize != this.mSampleSize || this.mBitmap == null) {
                this.mSampleSize = sampleSize;
                loadBitmapLocked();
            }
        }
    }

    private void loadBitmapLocked() {
        if (!this.mIsQueued && !this.mBounds.isEmpty()) {
            unscheduleSelf(this.mUpdateBitmap);
            sThreadPool.execute(this.mLoadBitmap);
            this.mIsQueued = true;
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return -1;
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
}
