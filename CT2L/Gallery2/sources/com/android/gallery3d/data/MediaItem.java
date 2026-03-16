package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.util.ThreadPool;

public abstract class MediaItem extends MediaObject {
    private static int sMicrothumbnailTargetSize = 200;
    private static final BytesBufferPool sMicroThumbBufferPool = new BytesBufferPool(4, 204800);
    private static int sThumbnailTargetSize = 640;

    public abstract int getHeight();

    public abstract String getMimeType();

    public abstract int getWidth();

    public abstract ThreadPool.Job<Bitmap> requestImage(int i);

    public abstract ThreadPool.Job<BitmapRegionDecoder> requestLargeImage();

    public MediaItem(Path path, long version) {
        super(path, version);
    }

    public long getDateInMs() {
        return 0L;
    }

    public String getName() {
        return null;
    }

    public void getLatLong(double[] latLong) {
        latLong[0] = 0.0d;
        latLong[1] = 0.0d;
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

    public static int getTargetSize(int type) {
        switch (type) {
            case 1:
                return sThumbnailTargetSize;
            case 2:
                return sMicrothumbnailTargetSize;
            default:
                throw new RuntimeException("should only request thumb/microthumb from cache");
        }
    }

    public static BytesBufferPool getBytesBufferPool() {
        return sMicroThumbBufferPool;
    }

    public static void setThumbnailSizes(int size, int microSize) {
        sThumbnailTargetSize = size;
        if (sMicrothumbnailTargetSize != microSize) {
            sMicrothumbnailTargetSize = microSize;
        }
    }
}
