package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.BytesBufferPool;
import com.android.gallery3d.util.ThreadPool;

abstract class ImageCacheRequest implements ThreadPool.Job<Bitmap> {
    protected GalleryApp mApplication;
    private Path mPath;
    private int mTargetSize;
    private long mTimeModified;
    private int mType;

    public abstract Bitmap onDecodeOriginal(ThreadPool.JobContext jobContext, int i);

    public ImageCacheRequest(GalleryApp application, Path path, long timeModified, int type, int targetSize) {
        this.mApplication = application;
        this.mPath = path;
        this.mType = type;
        this.mTargetSize = targetSize;
        this.mTimeModified = timeModified;
    }

    private String debugTag() {
        return this.mPath + "," + this.mTimeModified + "," + (this.mType == 1 ? "THUMB" : this.mType == 2 ? "MICROTHUMB" : "?");
    }

    @Override
    public Bitmap run(ThreadPool.JobContext jc) {
        ImageCacheService cacheService = this.mApplication.getImageCacheService();
        BytesBufferPool.BytesBuffer buffer = MediaItem.getBytesBufferPool().get();
        try {
            boolean found = cacheService.getImageData(this.mPath, this.mTimeModified, this.mType, buffer);
            if (jc.isCancelled()) {
                return null;
            }
            if (found) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = this.mType == 2 ? DecodeUtils.decodeUsingPool(jc, buffer.data, buffer.offset, buffer.length, options) : DecodeUtils.decodeUsingPool(jc, buffer.data, buffer.offset, buffer.length, options);
                if (bitmap == null && !jc.isCancelled()) {
                    Log.w("ImageCacheRequest", "decode cached failed " + debugTag());
                }
                return bitmap;
            }
            MediaItem.getBytesBufferPool().recycle(buffer);
            Bitmap bitmap2 = onDecodeOriginal(jc, this.mType);
            if (jc.isCancelled()) {
                return null;
            }
            if (bitmap2 == null) {
                Log.w("ImageCacheRequest", "decode orig failed " + debugTag());
                return null;
            }
            Bitmap bitmap3 = this.mType == 2 ? BitmapUtils.resizeAndCropCenter(bitmap2, this.mTargetSize, true) : BitmapUtils.resizeDownBySideLength(bitmap2, this.mTargetSize, true);
            if (jc.isCancelled()) {
                return null;
            }
            byte[] array = BitmapUtils.compressToBytes(bitmap3);
            if (jc.isCancelled()) {
                return null;
            }
            cacheService.putImageData(this.mPath, this.mTimeModified, this.mType, array);
            return bitmap3;
        } finally {
            MediaItem.getBytesBufferPool().recycle(buffer);
        }
    }
}
