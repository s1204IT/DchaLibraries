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
        public void run() throws Throwable {
            T t;
            Bitmap bitmapDecodeByteArray;
            InputStream fallbackImageStream;
            synchronized (AutoThumbnailDrawable.this.mLock) {
                t = AutoThumbnailDrawable.this.mData;
            }
            byte[] preferredImageBytes = AutoThumbnailDrawable.this.getPreferredImageBytes(t);
            boolean z = preferredImageBytes != null && preferredImageBytes.length > 0;
            if (z) {
                AutoThumbnailDrawable.this.mOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(preferredImageBytes, 0, preferredImageBytes.length, AutoThumbnailDrawable.this.mOptions);
                AutoThumbnailDrawable.this.mOptions.inJustDecodeBounds = false;
            }
            synchronized (AutoThumbnailDrawable.this.mLock) {
                if (AutoThumbnailDrawable.this.dataChangedLocked(t)) {
                    return;
                }
                int i = AutoThumbnailDrawable.this.mImageWidth;
                int i2 = AutoThumbnailDrawable.this.mImageHeight;
                int iCalculateSampleSizeLocked = z ? AutoThumbnailDrawable.this.calculateSampleSizeLocked(AutoThumbnailDrawable.this.mOptions.outWidth, AutoThumbnailDrawable.this.mOptions.outHeight) : 1;
                int iCalculateSampleSizeLocked2 = AutoThumbnailDrawable.this.calculateSampleSizeLocked(i, i2);
                AutoThumbnailDrawable.this.mIsQueued = false;
                InputStream inputStream = null;
                if (z) {
                    try {
                        AutoThumbnailDrawable.this.mOptions.inSampleSize = iCalculateSampleSizeLocked;
                        AutoThumbnailDrawable.this.mOptions.inBitmap = AutoThumbnailDrawable.sBitmapPool.get(AutoThumbnailDrawable.this.mOptions.outWidth / iCalculateSampleSizeLocked, AutoThumbnailDrawable.this.mOptions.outHeight / iCalculateSampleSizeLocked);
                        bitmapDecodeByteArray = BitmapFactory.decodeByteArray(preferredImageBytes, 0, preferredImageBytes.length, AutoThumbnailDrawable.this.mOptions);
                        try {
                            try {
                                if (AutoThumbnailDrawable.this.mOptions.inBitmap != null && bitmapDecodeByteArray != AutoThumbnailDrawable.this.mOptions.inBitmap) {
                                    AutoThumbnailDrawable.sBitmapPool.put(AutoThumbnailDrawable.this.mOptions.inBitmap);
                                    AutoThumbnailDrawable.this.mOptions.inBitmap = null;
                                }
                            } catch (Exception e) {
                                e = e;
                                Log.d("AutoThumbnailDrawable", "Failed to fetch bitmap", e);
                                if (inputStream != null) {
                                    try {
                                        inputStream.close();
                                    } catch (Exception e2) {
                                    }
                                }
                                if (bitmapDecodeByteArray != null) {
                                    synchronized (AutoThumbnailDrawable.this.mLock) {
                                        if (!AutoThumbnailDrawable.this.dataChangedLocked(t)) {
                                            AutoThumbnailDrawable.this.setBitmapLocked(bitmapDecodeByteArray);
                                            AutoThumbnailDrawable.this.scheduleSelf(AutoThumbnailDrawable.this.mUpdateBitmap, 0L);
                                        }
                                    }
                                    return;
                                }
                                return;
                            }
                        } catch (Throwable th) {
                            th = th;
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (Exception e3) {
                                }
                            }
                            if (bitmapDecodeByteArray == null) {
                                throw th;
                            }
                            synchronized (AutoThumbnailDrawable.this.mLock) {
                                if (!AutoThumbnailDrawable.this.dataChangedLocked(t)) {
                                    AutoThumbnailDrawable.this.setBitmapLocked(bitmapDecodeByteArray);
                                    AutoThumbnailDrawable.this.scheduleSelf(AutoThumbnailDrawable.this.mUpdateBitmap, 0L);
                                }
                            }
                            throw th;
                        }
                    } catch (Exception e4) {
                        e = e4;
                        bitmapDecodeByteArray = null;
                        Log.d("AutoThumbnailDrawable", "Failed to fetch bitmap", e);
                        if (inputStream != null) {
                        }
                        if (bitmapDecodeByteArray != null) {
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        bitmapDecodeByteArray = null;
                        if (inputStream != null) {
                        }
                        if (bitmapDecodeByteArray == null) {
                        }
                    }
                } else {
                    bitmapDecodeByteArray = null;
                }
                if (bitmapDecodeByteArray == null) {
                    fallbackImageStream = AutoThumbnailDrawable.this.getFallbackImageStream(t);
                    try {
                        AutoThumbnailDrawable.this.mOptions.inSampleSize = iCalculateSampleSizeLocked2;
                        AutoThumbnailDrawable.this.mOptions.inBitmap = AutoThumbnailDrawable.sBitmapPool.get(i / iCalculateSampleSizeLocked2, i2 / iCalculateSampleSizeLocked2);
                        Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(fallbackImageStream, null, AutoThumbnailDrawable.this.mOptions);
                        try {
                            if (AutoThumbnailDrawable.this.mOptions.inBitmap != null && bitmapDecodeStream != AutoThumbnailDrawable.this.mOptions.inBitmap) {
                                AutoThumbnailDrawable.sBitmapPool.put(AutoThumbnailDrawable.this.mOptions.inBitmap);
                                AutoThumbnailDrawable.this.mOptions.inBitmap = null;
                            }
                            bitmapDecodeByteArray = bitmapDecodeStream;
                        } catch (Exception e5) {
                            inputStream = fallbackImageStream;
                            e = e5;
                            bitmapDecodeByteArray = bitmapDecodeStream;
                            Log.d("AutoThumbnailDrawable", "Failed to fetch bitmap", e);
                            if (inputStream != null) {
                            }
                            if (bitmapDecodeByteArray != null) {
                            }
                        } catch (Throwable th3) {
                            inputStream = fallbackImageStream;
                            th = th3;
                            bitmapDecodeByteArray = bitmapDecodeStream;
                            if (inputStream != null) {
                            }
                            if (bitmapDecodeByteArray == null) {
                            }
                        }
                    } catch (Exception e6) {
                        inputStream = fallbackImageStream;
                        e = e6;
                    } catch (Throwable th4) {
                        inputStream = fallbackImageStream;
                        th = th4;
                    }
                } else {
                    fallbackImageStream = null;
                }
                if (fallbackImageStream != null) {
                    try {
                        fallbackImageStream.close();
                    } catch (Exception e7) {
                    }
                }
                if (bitmapDecodeByteArray != null) {
                    synchronized (AutoThumbnailDrawable.this.mLock) {
                        if (!AutoThumbnailDrawable.this.dataChangedLocked(t)) {
                            AutoThumbnailDrawable.this.setBitmapLocked(bitmapDecodeByteArray);
                            AutoThumbnailDrawable.this.scheduleSelf(AutoThumbnailDrawable.this.mUpdateBitmap, 0L);
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

    public void setImage(T t, int i, int i2) {
        if (dataChangedLocked(t)) {
            synchronized (this.mLock) {
                this.mImageWidth = i;
                this.mImageHeight = i2;
                this.mData = t;
                setBitmapLocked(null);
                refreshSampleSizeLocked();
            }
            invalidateSelf();
        }
    }

    private void setBitmapLocked(Bitmap bitmap) {
        if (bitmap == this.mBitmap) {
            return;
        }
        if (this.mBitmap != null) {
            sBitmapPool.put(this.mBitmap);
        }
        this.mBitmap = bitmap;
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        synchronized (this.mLock) {
            this.mBounds.set(rect);
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
        float f;
        float f2;
        if (this.mBitmap == null || this.mBounds.isEmpty()) {
            this.mDrawMatrix.reset();
            return;
        }
        int width = this.mBitmap.getWidth();
        int height = this.mBitmap.getHeight();
        int iWidth = this.mBounds.width();
        int iHeight = this.mBounds.height();
        float f3 = 0.0f;
        if (width * iHeight > iWidth * height) {
            float f4 = iHeight / height;
            f2 = 0.0f;
            f3 = (iWidth - (width * f4)) * 0.5f;
            f = f4;
        } else {
            f = iWidth / width;
            f2 = (iHeight - (height * f)) * 0.5f;
        }
        if (f < 0.8f) {
            Log.w("AutoThumbnailDrawable", "sample size was too small! Overdrawing! " + f + ", " + this.mSampleSize);
        } else if (f > 1.5f) {
            Log.w("AutoThumbnailDrawable", "Potential quality loss! " + f + ", " + this.mSampleSize);
        }
        this.mDrawMatrix.setScale(f, f);
        this.mDrawMatrix.postTranslate((int) (f3 + 0.5f), (int) (f2 + 0.5f));
    }

    private int calculateSampleSizeLocked(int i, int i2) {
        float f;
        int iWidth = this.mBounds.width();
        int iHeight = this.mBounds.height();
        if (i * iHeight > iWidth * i2) {
            f = i2 / iHeight;
        } else {
            f = i / iWidth;
        }
        int iRound = Math.round(f);
        if (iRound > 0) {
            return iRound;
        }
        return 1;
    }

    private void refreshSampleSizeLocked() {
        if (this.mBounds.isEmpty() || this.mImageWidth == 0 || this.mImageHeight == 0) {
            return;
        }
        int iCalculateSampleSizeLocked = calculateSampleSizeLocked(this.mImageWidth, this.mImageHeight);
        if (iCalculateSampleSizeLocked != this.mSampleSize || this.mBitmap == null) {
            this.mSampleSize = iCalculateSampleSizeLocked;
            loadBitmapLocked();
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
}
