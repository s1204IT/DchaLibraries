package com.android.gallery3d.ui;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.ui.TileImageView;
import com.android.photos.data.GalleryBitmapPool;

public class TileImageViewAdapter implements TileImageView.TileSource {
    protected int mImageHeight;
    protected int mImageWidth;
    protected int mLevelCount;
    protected BitmapRegionDecoder mRegionDecoder;
    protected ScreenNail mScreenNail;

    public synchronized void clear() {
        this.mScreenNail = null;
        this.mImageWidth = 0;
        this.mImageHeight = 0;
        this.mLevelCount = 0;
        this.mRegionDecoder = null;
    }

    public synchronized void setScreenNail(ScreenNail screenNail, int width, int height) {
        Utils.checkNotNull(screenNail);
        this.mScreenNail = screenNail;
        this.mImageWidth = width;
        this.mImageHeight = height;
        this.mRegionDecoder = null;
        this.mLevelCount = 0;
    }

    public synchronized void setRegionDecoder(BitmapRegionDecoder decoder) {
        this.mRegionDecoder = (BitmapRegionDecoder) Utils.checkNotNull(decoder);
        this.mImageWidth = decoder.getWidth();
        this.mImageHeight = decoder.getHeight();
        this.mLevelCount = calculateLevelCount();
    }

    private int calculateLevelCount() {
        return Math.max(0, Utils.ceilLog2(this.mImageWidth / this.mScreenNail.getWidth()));
    }

    @Override
    @TargetApi(11)
    public Bitmap getTile(int level, int x, int y, int tileSize) {
        Bitmap bitmap;
        if (!ApiHelper.HAS_REUSING_BITMAP_IN_BITMAP_REGION_DECODER) {
            return getTileWithoutReusingBitmap(level, x, y, tileSize);
        }
        int t = tileSize << level;
        Rect wantRegion = new Rect(x, y, x + t, y + t);
        synchronized (this) {
            BitmapRegionDecoder regionDecoder = this.mRegionDecoder;
            if (regionDecoder == null) {
                bitmap = null;
            } else {
                boolean needClear = !new Rect(0, 0, this.mImageWidth, this.mImageHeight).contains(wantRegion);
                bitmap = GalleryBitmapPool.getInstance().get(tileSize, tileSize);
                if (bitmap != null) {
                    if (needClear) {
                        bitmap.eraseColor(0);
                    }
                } else {
                    bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inPreferQualityOverSpeed = true;
                options.inSampleSize = 1 << level;
                options.inBitmap = bitmap;
                try {
                    synchronized (regionDecoder) {
                        bitmap = regionDecoder.decodeRegion(wantRegion, options);
                    }
                    if (options.inBitmap != bitmap && options.inBitmap != null) {
                        GalleryBitmapPool.getInstance().put(options.inBitmap);
                        options.inBitmap = null;
                    }
                    if (bitmap == null) {
                        Log.w("TileImageViewAdapter", "fail in decoding region");
                    }
                } catch (Throwable th) {
                    if (options.inBitmap != bitmap && options.inBitmap != null) {
                        GalleryBitmapPool.getInstance().put(options.inBitmap);
                        options.inBitmap = null;
                    }
                    throw th;
                }
            }
        }
        return bitmap;
    }

    private Bitmap getTileWithoutReusingBitmap(int level, int x, int y, int tileSize) {
        Bitmap bitmap;
        int t = tileSize << level;
        Rect rect = new Rect(x, y, x + t, y + t);
        synchronized (this) {
            BitmapRegionDecoder bitmapRegionDecoder = this.mRegionDecoder;
            if (bitmapRegionDecoder == null) {
                return null;
            }
            Rect rect2 = new Rect(0, 0, this.mImageWidth, this.mImageHeight);
            Utils.assertTrue(rect2.intersect(rect));
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inPreferQualityOverSpeed = true;
            options.inSampleSize = 1 << level;
            synchronized (bitmapRegionDecoder) {
                bitmap = bitmapRegionDecoder.decodeRegion(rect2, options);
            }
            if (bitmap == null) {
                Log.w("TileImageViewAdapter", "fail in decoding region");
            }
            if (!rect.equals(rect2)) {
                Bitmap result = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(result);
                canvas.drawBitmap(bitmap, (rect2.left - rect.left) >> level, (rect2.top - rect.top) >> level, (Paint) null);
                return result;
            }
            return bitmap;
        }
    }

    @Override
    public ScreenNail getScreenNail() {
        return this.mScreenNail;
    }

    @Override
    public int getImageHeight() {
        return this.mImageHeight;
    }

    @Override
    public int getImageWidth() {
        return this.mImageWidth;
    }

    @Override
    public int getLevelCount() {
        return this.mLevelCount;
    }
}
