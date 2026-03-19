package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.BytesBufferPool;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallery3d.layout.FancyHelper;
import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.base.IDecodeOptionsProcessor;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.ThumbType;
import com.mediatek.gallerybasic.util.DebugUtils;
import com.mediatek.galleryportable.TraceHelper;

abstract class ImageCacheRequest implements ThreadPool.Job<Bitmap> {
    private static IDecodeOptionsProcessor[] sOptionsProcessors;
    protected GalleryApp mApplication;
    private final String mCacheBitmap = "_CacheBitmap";
    private final String mOriginBitmap = "_OriginBitmap";
    private Path mPath;
    private int mTargetSize;
    private long mTimeModified;
    private int mType;

    public abstract Bitmap onDecodeOriginal(ThreadPool.JobContext jobContext, int i);

    public ImageCacheRequest(GalleryApp galleryApp, Path path, long j, int i, int i2) {
        this.mApplication = galleryApp;
        this.mPath = path;
        this.mType = i;
        this.mTargetSize = i2;
        this.mTimeModified = j;
    }

    private String debugTag() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append(this.mPath);
        sb.append(",");
        sb.append(this.mTimeModified);
        sb.append(",");
        if (this.mType == 1) {
            str = "THUMB";
        } else {
            str = this.mType == 2 ? "MICROTHUMB" : "?";
        }
        sb.append(str);
        return sb.toString();
    }

    @Override
    public Bitmap run(ThreadPool.JobContext jobContext) throws Throwable {
        boolean z;
        boolean z2;
        ExtItem extItem;
        String str;
        MediaItem mediaItem;
        ImageCacheService imageCacheService = this.mApplication.getImageCacheService();
        if (!(this.mPath.getObject() instanceof MediaItem) || (mediaItem = (MediaItem) this.mPath.getObject()) == null) {
            z = true;
            z2 = true;
            extItem = null;
            str = null;
        } else {
            extItem = mediaItem.getExtItem();
            String mimeType = mediaItem.getMimeType();
            ThumbType thumbTypeConvertToThumbType = FeatureHelper.convertToThumbType(this.mType);
            z = this.mType != 4 && extItem.isNeedToGetThumbFromCache(thumbTypeConvertToThumbType);
            str = mimeType;
            z2 = this.mType != 4 && extItem.isNeedToCacheThumb(thumbTypeConvertToThumbType);
        }
        if (extItem == null) {
            com.mediatek.gallery3d.util.Log.w("Gallery2/ImageCacheRequest", "extItem is null, maybe because the MediaObject has recycled by GC");
        }
        if (z) {
            BytesBufferPool.BytesBuffer bytesBuffer = MediaItem.getBytesBufferPool().get();
            try {
                TraceHelper.beginSection(">>>>ImageCacheRequest-getImageData");
                boolean imageData = imageCacheService.getImageData(this.mPath, this.mTimeModified, this.mType, bytesBuffer);
                TraceHelper.endSection();
                if (ImageCacheService.sForceObsoletePath != null && ImageCacheService.sForceObsoletePath.equals(this.mPath.toString())) {
                    ImageCacheService.sForceObsoletePath = null;
                    imageData = false;
                }
                if (jobContext.isCancelled()) {
                    return null;
                }
                if (imageData) {
                    TraceHelper.beginSection(">>>>ImageCacheRequest-decodeFromCache");
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    processOptions(str, options);
                    Bitmap bitmapDecodeUsingPool = this.mType == 2 ? DecodeUtils.decodeUsingPool(jobContext, bytesBuffer.data, bytesBuffer.offset, bytesBuffer.length, options) : DecodeUtils.decodeUsingPool(jobContext, bytesBuffer.data, bytesBuffer.offset, bytesBuffer.length, options);
                    if (bitmapDecodeUsingPool == null && !jobContext.isCancelled()) {
                        com.mediatek.gallery3d.util.Log.w("Gallery2/ImageCacheRequest", "decode cached failed " + debugTag());
                    }
                    if (DebugUtils.DUMP) {
                        if (bitmapDecodeUsingPool == null) {
                            dumpBitmap(Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565), "_CacheBitmap");
                            bitmapDecodeUsingPool = null;
                        } else {
                            dumpBitmap(bitmapDecodeUsingPool, "_CacheBitmap");
                        }
                    }
                    TraceHelper.endSection();
                    return bitmapDecodeUsingPool;
                }
            } finally {
                MediaItem.getBytesBufferPool().recycle(bytesBuffer);
            }
        }
        TraceHelper.beginSection(">>>>ImageCacheRequest-decodeFromOriginal");
        Bitmap bitmapOnDecodeOriginal = onDecodeOriginal(jobContext, this.mType);
        TraceHelper.endSection();
        if (jobContext.isCancelled()) {
            return null;
        }
        if (bitmapOnDecodeOriginal == null) {
            com.mediatek.gallery3d.util.Log.w("Gallery2/ImageCacheRequest", "decode orig failed " + debugTag());
            return null;
        }
        if (DebugUtils.DUMP) {
            dumpBitmap(bitmapOnDecodeOriginal, "_OriginBitmap");
        }
        TraceHelper.beginSection(">>>>ImageCacheRequest-resizeAndCrop");
        if (FancyHelper.isFancyLayoutSupported() && this.mType == 3) {
            ?? mediaObject = this.mApplication.getDataManager().getMediaObject(this.mPath);
            if (mediaObject != 0 && (mediaObject instanceof MediaItem)) {
                bitmapOnDecodeOriginal = resizeAndCropFancyThumbnail(bitmapOnDecodeOriginal, mediaObject, this.mTargetSize);
            }
        } else {
            bitmapOnDecodeOriginal = this.mType == 2 ? BitmapUtils.resizeAndCropCenter(bitmapOnDecodeOriginal, this.mTargetSize, true) : BitmapUtils.alignBitmapToEven(BitmapUtils.resizeDownBySideLength(bitmapOnDecodeOriginal, this.mTargetSize, true), true);
        }
        Bitmap bitmap = bitmapOnDecodeOriginal;
        TraceHelper.endSection();
        if (jobContext.isCancelled()) {
            return null;
        }
        if (!z2) {
            return bitmap;
        }
        TraceHelper.beginSection(">>>>ImageCacheRequest-compressToBytes");
        byte[] bArrCompressToBytes = BitmapUtils.compressToBytes(bitmap);
        TraceHelper.endSection();
        if (jobContext.isCancelled()) {
            return null;
        }
        TraceHelper.beginSection(">>>>ImageCacheRequest-writeToCache");
        imageCacheService.putImageData(this.mPath, this.mTimeModified, this.mType, bArrCompressToBytes);
        TraceHelper.endSection();
        BitmapFactory.Options options2 = new BitmapFactory.Options();
        if (!processOptions(str, options2)) {
            return bitmap;
        }
        bitmap.recycle();
        TraceHelper.beginSection(">>>>ImageCacheRequest-decodeFromCacheWithPQ");
        Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(bArrCompressToBytes, 0, bArrCompressToBytes.length, options2);
        TraceHelper.endSection();
        return bitmapDecodeByteArray;
    }

    private void dumpBitmap(Bitmap bitmap, String str) throws Throwable {
        String str2;
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.mType == 2) {
            str2 = "MicroTNail";
        } else if (this.mType == 3) {
            str2 = "FancyTNail";
        } else {
            str2 = "TNail";
        }
        MediaItem mediaItem = (MediaItem) this.mPath.getObject();
        if (mediaItem != null) {
            String str3 = mediaItem.getName() + str + str2;
            com.mediatek.gallery3d.util.Log.d("Gallery2/ImageCacheRequest", "<dumpBitmap> string " + str3);
            DebugUtils.dumpBitmap(bitmap, str3);
            com.mediatek.gallery3d.util.Log.d("Gallery2/ImageCacheRequest", "<dumpBitmap> Dump Bitmap time " + (System.currentTimeMillis() - jCurrentTimeMillis));
        }
    }

    public Bitmap resizeAndCropFancyThumbnail(Bitmap bitmap, MediaItem mediaItem, int i) {
        MediaData mediaData = mediaItem.getMediaData();
        int orientation = mediaData.isVideo ? ((LocalVideo) mediaItem).getOrientation() : mediaItem.getRotation();
        if (isScreenShotCover(this.mApplication, mediaData, mediaItem.getPath())) {
            com.mediatek.gallery3d.util.Log.d("Gallery2/ImageCacheRequest", "<resizeAndCropFancyThumbnail> ScreenShotCover");
            return bitmap;
        }
        if (orientation == -2) {
            com.mediatek.gallery3d.util.Log.d("Gallery2/ImageCacheRequest", "<resizeAndCropFancyThumbnail> crop center on none-mtk platform if video");
            return FancyHelper.resizeAndCropCenter(bitmap, 2 * i, true);
        }
        boolean zIsCameraRollCover = isCameraRollCover(this.mApplication, mediaData, mediaItem.getPath());
        int screenWidthAtFancyMode = FancyHelper.getScreenWidthAtFancyMode();
        if (!zIsCameraRollCover || !FancyHelper.isLandItem(mediaItem)) {
            if (orientation == 90 || orientation == 270) {
                if (mediaItem.getHeight() != 0 && mediaItem.getWidth() / mediaItem.getHeight() > 2.5f) {
                    int i2 = screenWidthAtFancyMode / 2;
                    return FancyHelper.resizeAndCropCenter(bitmap, Math.round(i2 * 2.5f), i2, false, true);
                }
                if (mediaItem.getHeight() != 0 && mediaItem.getWidth() / mediaItem.getHeight() < 0.4f) {
                    int i3 = screenWidthAtFancyMode / 2;
                    return FancyHelper.resizeAndCropCenter(bitmap, Math.round(i3 * 0.4f), i3, true, true);
                }
                if (zIsCameraRollCover && mediaData.isVideo) {
                    return FancyHelper.resizeByWidthOrLength(bitmap, 2 * i, true, true);
                }
                return FancyHelper.resizeByWidthOrLength(bitmap, i, false, true);
            }
            if (mediaItem.getWidth() != 0 && mediaItem.getHeight() / mediaItem.getWidth() > 2.5f) {
                int i4 = screenWidthAtFancyMode / 2;
                return FancyHelper.resizeAndCropCenter(bitmap, i4, Math.round(i4 * 2.5f), true, true);
            }
            if (mediaItem.getWidth() != 0 && mediaItem.getHeight() / mediaItem.getWidth() < 0.4f) {
                int i5 = screenWidthAtFancyMode / 2;
                return FancyHelper.resizeAndCropCenter(bitmap, i5, Math.round(i5 * 0.4f), false, true);
            }
            return FancyHelper.resizeByWidthOrLength(bitmap, i, true, true);
        }
        if (!mediaData.isVideo) {
            if (orientation == 90 || orientation == 270) {
                if (mediaItem.getWidth() != 0 && mediaItem.getHeight() / mediaItem.getWidth() > 1.78f) {
                    return FancyHelper.resizeAndCropCenter(bitmap, Math.round(screenWidthAtFancyMode / 1.78f), screenWidthAtFancyMode, true, true);
                }
                return FancyHelper.resizeAndCropCenter(bitmap, Math.round(screenWidthAtFancyMode / 1.78f), screenWidthAtFancyMode, false, true);
            }
            if (mediaItem.getHeight() != 0 && mediaItem.getWidth() / mediaItem.getHeight() > 1.78f) {
                return FancyHelper.resizeAndCropCenter(bitmap, screenWidthAtFancyMode, Math.round(screenWidthAtFancyMode / 1.78f), false, true);
            }
            return FancyHelper.resizeAndCropCenter(bitmap, screenWidthAtFancyMode, Math.round(screenWidthAtFancyMode / 1.78f), true, true);
        }
        return FancyHelper.resizeByWidthOrLength(bitmap, 2 * i, true, true);
    }

    protected boolean isCameraRollCover(GalleryApp galleryApp, MediaData mediaData, Path path) {
        if (mediaData == null || path == null) {
            com.mediatek.gallery3d.util.Log.d("Gallery2/ImageCacheRequest", "<isCameraRollCover> media data or path is null");
            return false;
        }
        MediaSet mediaSet = galleryApp.getDataManager().getMediaSet(FancyHelper.getMediaSetPath(mediaData));
        if (mediaSet == null || !mediaSet.isCameraRoll() || mediaSet.getCoverMediaItem() == null || !mediaSet.getCoverMediaItem().getPath().equalsIgnoreCase(path.toString())) {
            return false;
        }
        com.mediatek.gallery3d.util.Log.d("Gallery2/ImageCacheRequest", "<isCameraRollCover> " + mediaData.filePath + " is isCameraRollCover");
        return true;
    }

    protected boolean isScreenShotCover(GalleryApp galleryApp, MediaData mediaData, Path path) {
        if (mediaData == null) {
            com.mediatek.gallery3d.util.Log.d("Gallery2/ImageCacheRequest", "<isScreenShotCover> media data is null");
            return false;
        }
        MediaSet mediaSet = galleryApp.getDataManager().getMediaSet(FancyHelper.getMediaSetPath(mediaData));
        if (mediaSet == null) {
            com.mediatek.gallery3d.util.Log.d("Gallery2/ImageCacheRequest", "<isScreenShotCover> mediaSet is null");
            return false;
        }
        if (!(mediaSet.getName() != null ? mediaSet.getName() : "").equals(galleryApp.getResources().getString(R.string.folder_screenshot)) || mediaSet.getCoverMediaItem() == null || !mediaSet.getCoverMediaItem().getPath().equalsIgnoreCase(path.toString())) {
            return false;
        }
        com.mediatek.gallery3d.util.Log.d("Gallery2/ImageCacheRequest", "<isScreenShotCover> " + mediaData.filePath + " is ScreenShotCover");
        return true;
    }

    protected boolean processOptions(String str, BitmapFactory.Options options) {
        if (sOptionsProcessors == null) {
            sOptionsProcessors = (IDecodeOptionsProcessor[]) FeatureManager.getInstance().getImplement(IDecodeOptionsProcessor.class, new Object[0]);
        }
        boolean z = false;
        for (IDecodeOptionsProcessor iDecodeOptionsProcessor : sOptionsProcessors) {
            z = z || iDecodeOptionsProcessor.processThumbDecodeOptions(str, options);
        }
        return z;
    }

    protected void clearCache() {
        com.mediatek.gallery3d.util.Log.d("Gallery2/ImageCacheRequest", "clear cache, path = " + this.mPath);
        this.mApplication.getImageCacheService().clearImageData(this.mPath, this.mTimeModified, 1);
    }
}
