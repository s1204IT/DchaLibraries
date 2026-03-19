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
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.TileImageView;
import com.android.photos.data.GalleryBitmapPool;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallerybasic.base.IDecodeOptionsProcessor;
import com.mediatek.gallerybasic.util.BitmapUtils;
import com.mediatek.gallerybasic.util.DebugUtils;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TileImageViewAdapter implements TileImageView.TileSource {
    private static final String TAG = "Gallery2/TileImageViewAdapter";
    private static IDecodeOptionsProcessor[] sOptionsProcessors;
    private static int sTileDumpNum = 0;
    protected int mImageHeight;
    protected int mImageWidth;
    protected int mLevelCount;
    protected boolean mOwnScreenNail;
    protected BitmapRegionDecoder mRegionDecoder;
    protected ScreenNail mScreenNail;
    private ReadWriteLock mRegionDecoderLock = new ReentrantReadWriteLock();
    public String mMimeType = null;

    public TileImageViewAdapter() {
    }

    public synchronized void clear() {
        this.mScreenNail = null;
        this.mImageWidth = 0;
        this.mImageHeight = 0;
        this.mLevelCount = 0;
        this.mRegionDecoder = null;
    }

    public synchronized void setScreenNail(ScreenNail screenNail, int i, int i2) {
        Utils.checkNotNull(screenNail);
        this.mScreenNail = screenNail;
        this.mImageWidth = i;
        this.mImageHeight = i2;
        this.mRegionDecoder = null;
        this.mLevelCount = 0;
    }

    public synchronized void setRegionDecoder(BitmapRegionDecoder bitmapRegionDecoder) {
        this.mRegionDecoder = (BitmapRegionDecoder) Utils.checkNotNull(bitmapRegionDecoder);
        this.mImageWidth = bitmapRegionDecoder.getWidth();
        this.mImageHeight = bitmapRegionDecoder.getHeight();
        this.mLevelCount = calculateLevelCount();
    }

    public synchronized void setRegionDecoder(BitmapRegionDecoder bitmapRegionDecoder, int i, int i2) {
        this.mRegionDecoder = (BitmapRegionDecoder) Utils.checkNotNull(bitmapRegionDecoder);
        this.mImageWidth = i;
        this.mImageHeight = i2;
        this.mLevelCount = calculateLevelCount();
    }

    private int calculateLevelCount() {
        return Math.max(0, Utils.ceilLog2(this.mImageWidth / this.mScreenNail.getWidth()));
    }

    @Override
    @TargetApi(11)
    public Bitmap getTile(int i, int i2, int i3, int i4) throws Throwable {
        Bitmap bitmapDecodeRegion;
        if (!ApiHelper.HAS_REUSING_BITMAP_IN_BITMAP_REGION_DECODER) {
            return getTileWithoutReusingBitmap(i, i2, i3, i4);
        }
        int i5 = i4 << i;
        Rect rect = new Rect(i2, i3, i2 + i5, i5 + i3);
        synchronized (this) {
            BitmapRegionDecoder bitmapRegionDecoder = this.mRegionDecoder;
            if (bitmapRegionDecoder == null) {
                return null;
            }
            boolean z = !new Rect(0, 0, this.mImageWidth, this.mImageHeight).contains(rect);
            Bitmap bitmapCreateBitmap = GalleryBitmapPool.getInstance().get(i4, i4);
            if (bitmapCreateBitmap == null) {
                bitmapCreateBitmap = Bitmap.createBitmap(i4, i4, Bitmap.Config.ARGB_8888);
            } else if (z) {
                bitmapCreateBitmap.eraseColor(0);
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inPreferQualityOverSpeed = true;
            options.inSampleSize = 1 << i;
            options.inBitmap = bitmapCreateBitmap;
            processOptions(this.mMimeType, options);
            try {
                this.mRegionDecoderLock.readLock().lock();
                bitmapDecodeRegion = bitmapRegionDecoder.decodeRegion(rect, options);
                try {
                    if (DebugUtils.TILE) {
                        if (bitmapDecodeRegion == null) {
                            Log.d(TAG, "<getTile1> decodeRegion l" + i + "-x" + i2 + "-y" + i3 + "-size" + i4 + ", return null");
                        } else {
                            DebugUtils.dumpBitmap(bitmapDecodeRegion, "Tile-l" + i + "-x" + i2 + "-y" + i3 + "-size" + i4 + "-" + sTileDumpNum);
                            sTileDumpNum = sTileDumpNum + 1;
                        }
                    }
                    this.mRegionDecoderLock.readLock().unlock();
                    if (options.inBitmap != bitmapDecodeRegion && options.inBitmap != null) {
                        GalleryBitmapPool.getInstance().put(options.inBitmap);
                        options.inBitmap = null;
                    }
                    if (bitmapDecodeRegion == null) {
                        Log.w(TAG, "fail in decoding region");
                    }
                    return replaceBackgroudForTile(bitmapDecodeRegion, i, i2, i3, i4);
                } catch (Throwable th) {
                    th = th;
                    this.mRegionDecoderLock.readLock().unlock();
                    if (options.inBitmap != bitmapDecodeRegion && options.inBitmap != null) {
                        GalleryBitmapPool.getInstance().put(options.inBitmap);
                        options.inBitmap = null;
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                bitmapDecodeRegion = bitmapCreateBitmap;
            }
        }
    }

    private Bitmap getTileWithoutReusingBitmap(int i, int i2, int i3, int i4) {
        int i5 = i4 << i;
        Rect rect = new Rect(i2, i3, i2 + i5, i5 + i3);
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
            options.inSampleSize = 1 << i;
            processOptions(this.mMimeType, options);
            try {
                this.mRegionDecoderLock.readLock().lock();
                Bitmap bitmapDecodeRegion = bitmapRegionDecoder.decodeRegion(rect, options);
                if (bitmapDecodeRegion == null) {
                    Log.w(TAG, "fail in decoding region");
                }
                Bitmap bitmapReplaceBackgroudForTile = replaceBackgroudForTile(bitmapDecodeRegion, i, i2, i3, i4);
                if (rect.equals(rect2)) {
                    return bitmapReplaceBackgroudForTile;
                }
                Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i4, i4, Bitmap.Config.ARGB_8888);
                new Canvas(bitmapCreateBitmap).drawBitmap(bitmapReplaceBackgroudForTile, (rect2.left - rect.left) >> i, (rect2.top - rect.top) >> i, (Paint) null);
                return bitmapCreateBitmap;
            } finally {
                this.mRegionDecoderLock.readLock().unlock();
            }
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

    public TileImageViewAdapter(Bitmap bitmap, BitmapRegionDecoder bitmapRegionDecoder) {
        Utils.checkNotNull(bitmap);
        updateScreenNail(new BitmapScreenNail(bitmap), true);
        this.mRegionDecoder = bitmapRegionDecoder;
        this.mImageWidth = bitmapRegionDecoder.getWidth();
        this.mImageHeight = bitmapRegionDecoder.getHeight();
        this.mLevelCount = calculateLevelCount();
    }

    private void updateScreenNail(ScreenNail screenNail, boolean z) {
        if (this.mScreenNail != null && this.mOwnScreenNail) {
            this.mScreenNail.recycle();
        }
        this.mScreenNail = screenNail;
        this.mOwnScreenNail = z;
    }

    public synchronized void clearAndRecycle() {
        this.mScreenNail = null;
        this.mImageWidth = 0;
        this.mImageHeight = 0;
        this.mLevelCount = 0;
        if (this.mRegionDecoder != null) {
            this.mRegionDecoderLock.writeLock().lock();
            this.mRegionDecoder.recycle();
            this.mRegionDecoderLock.writeLock().unlock();
            this.mRegionDecoder = null;
        }
    }

    public void updateWidthAndHeight(MediaItem mediaItem) {
        if (mediaItem != null) {
            this.mImageWidth = mediaItem.getWidth();
            this.mImageHeight = mediaItem.getHeight();
            Log.d(TAG, "<updateWidthAndHeight> mImageWidth " + this.mImageWidth + ", mImageHeight " + this.mImageHeight);
        }
    }

    private boolean processOptions(String str, BitmapFactory.Options options) {
        if (sOptionsProcessors == null) {
            sOptionsProcessors = (IDecodeOptionsProcessor[]) FeatureManager.getInstance().getImplement(IDecodeOptionsProcessor.class, new Object[0]);
        }
        boolean z = false;
        for (IDecodeOptionsProcessor iDecodeOptionsProcessor : sOptionsProcessors) {
            z = z || iDecodeOptionsProcessor.processRegionDecodeOptions(str, options);
        }
        return z;
    }

    private Bitmap replaceBackgroudForTile(Bitmap bitmap, int i, int i2, int i3, int i4) {
        int i5 = i4 << i;
        Rect rect = new Rect(i2, i3, i2 + i5, i5 + i3);
        int i6 = rect.right > this.mImageWidth ? (this.mImageWidth - i2) >> i : i4;
        if (rect.bottom > this.mImageHeight) {
            i4 = (this.mImageHeight - i3) >> i;
        }
        return BitmapUtils.replaceBackgroundColor(bitmap, true, new Rect(0, 0, i6, i4));
    }
}
