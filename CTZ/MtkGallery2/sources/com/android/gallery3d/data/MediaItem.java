package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.ThumbType;
import com.mediatek.gallerybasic.util.Utils;

public abstract class MediaItem extends MediaObject {
    protected ExtItem mExtItem;
    protected MediaData mMediaData;
    protected Object mMediaDataLock;
    private static int sMicrothumbnailTargetSize = 200;
    private static final BytesBufferPool sMicroThumbBufferPool = new BytesBufferPool(4, 204800);
    public static int sThumbnailTargetSize = 640;
    protected static int sFancyThumbnailSize = 360;
    public static int sHighQualityThumbnailSize = GalleryUtils.sRealResolutionMaxSize;

    public abstract int getHeight();

    public abstract String getMimeType();

    public abstract int getWidth();

    public abstract ThreadPool.Job<Bitmap> requestImage(int i);

    public abstract ThreadPool.Job<BitmapRegionDecoder> requestLargeImage();

    public MediaItem(Path path, long j) {
        super(path, j);
        this.mExtItem = null;
        this.mMediaData = null;
        this.mMediaDataLock = new Object();
    }

    public long getDateInMs() {
        return 0L;
    }

    public String getName() {
        return null;
    }

    public void getLatLong(double[] dArr) {
        dArr[0] = 0.0d;
        dArr[1] = 0.0d;
    }

    public String[] getTags() {
        return null;
    }

    public Face[] getFaces() {
        return null;
    }

    public int getFullImageRotation() {
        return getRotation();
    }

    public int getRotation() {
        return 0;
    }

    public long getSize() {
        return 0L;
    }

    public String getFilePath() {
        return "";
    }

    public ScreenNail getScreenNail() {
        return null;
    }

    public static int getTargetSize(int i) {
        switch (i) {
            case 1:
                return sThumbnailTargetSize;
            case 2:
                return sMicrothumbnailTargetSize;
            case 3:
                return sFancyThumbnailSize;
            case 4:
                return sHighQualityThumbnailSize;
            default:
                throw new RuntimeException("should only request thumb/microthumb from cache");
        }
    }

    public static BytesBufferPool getBytesBufferPool() {
        return sMicroThumbBufferPool;
    }

    public static void setThumbnailSizes(int i, int i2) {
        sThumbnailTargetSize = i;
        if (sMicrothumbnailTargetSize != i2) {
            if (i2 % 256 == 0) {
                i2 -= 2;
            }
            sMicrothumbnailTargetSize = i2;
        }
        if (Utils.getDeviceRam() > 0 && Utils.getDeviceRam() <= 1048576) {
            sThumbnailTargetSize = Math.min(sThumbnailTargetSize, 592);
            sMicrothumbnailTargetSize = Math.min(sMicrothumbnailTargetSize, 236);
        }
        Log.d("Gallery2/MediaItem", "<setThumbnailSizes> sThumbnailTargetSize = " + sThumbnailTargetSize + ", sMicrothumbnailTargetSize = " + sMicrothumbnailTargetSize);
    }

    public ExtItem getExtItem() {
        return this.mExtItem;
    }

    public MediaData getMediaData() {
        MediaData mediaData;
        synchronized (this.mMediaDataLock) {
            mediaData = this.mMediaData;
        }
        return mediaData;
    }

    public static void setFancyThumbnailSizes(int i) {
        sFancyThumbnailSize = i;
        if (Utils.getDeviceRam() > 0 && Utils.getDeviceRam() <= 1048576) {
            sFancyThumbnailSize = Math.min(sFancyThumbnailSize, 240);
        }
        ThumbType.FANCY.setTargetSize(sFancyThumbnailSize);
        Log.d("Gallery2/MediaItem", "<setFancyThumbnailSizes> <Fancy> sFancyThumbnailSize = " + sFancyThumbnailSize);
    }
}
