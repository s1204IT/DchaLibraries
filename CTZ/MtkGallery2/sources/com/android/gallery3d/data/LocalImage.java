package com.android.gallery3d.data;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.provider.MediaStore;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.PanoramaMetadataSupport;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.filtershow.tools.SaveImage;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.UpdateHelper;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.adapter.MediaDataParser;
import com.mediatek.gallery3d.adapter.PhotoPlayFacade;
import com.mediatek.gallery3d.layout.FancyHelper;
import com.mediatek.gallery3d.util.FeatureConfig;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.base.ExtFields;
import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.util.BitmapUtils;
import com.mediatek.gallerybasic.util.DecodeSpecLimitor;
import com.mediatek.gallerybasic.util.ExtFieldsUtils;
import com.mediatek.galleryportable.TraceHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class LocalImage extends LocalMediaItem {
    static final Path ITEM_PATH = Path.fromString("/local/image/item");
    static final String[] PROJECTION = {BookmarkEnhance.COLUMN_ID, "title", BookmarkEnhance.COLUMN_MEDIA_TYPE, "latitude", "longitude", "datetaken", BookmarkEnhance.COLUMN_ADD_DATE, "date_modified", BookmarkEnhance.COLUMN_DATA, ExtFieldsUtils.VIDEO_ROTATION_FIELD, "bucket_id", "_size", SchemaSymbols.ATTVAL_FALSE_0, SchemaSymbols.ATTVAL_FALSE_0};
    private static String[] sExtProjection;
    private final GalleryApp mApplication;
    private PanoramaMetadataSupport mPanoramaMetadata;
    public int rotation;

    static {
        updateWidthAndHeightProjection();
    }

    @TargetApi(16)
    private static void updateWidthAndHeightProjection() {
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            PROJECTION[12] = "width";
            PROJECTION[13] = "height";
        }
    }

    public LocalImage(Path path, GalleryApp galleryApp, Cursor cursor) {
        super(path, nextVersionNumber());
        this.mPanoramaMetadata = new PanoramaMetadataSupport(this);
        this.mApplication = galleryApp;
        loadFromCursor(cursor);
        updateMediaData(cursor);
    }

    public LocalImage(Path path, GalleryApp galleryApp, int i) {
        super(path, nextVersionNumber());
        this.mPanoramaMetadata = new PanoramaMetadataSupport(this);
        this.mApplication = galleryApp;
        Cursor itemCursor = LocalAlbum.getItemCursor(this.mApplication.getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, getProjection(), i);
        if (itemCursor == null) {
            throw new RuntimeException("cannot get cursor for: " + path);
        }
        try {
            if (itemCursor.moveToNext()) {
                loadFromCursor(itemCursor);
                updateMediaData(itemCursor);
            } else {
                throw new RuntimeException("cannot find data for: " + path);
            }
        } finally {
            itemCursor.close();
        }
    }

    private void loadFromCursor(Cursor cursor) {
        this.id = cursor.getInt(0);
        this.caption = cursor.getString(1);
        this.mimeType = cursor.getString(2);
        this.latitude = cursor.getDouble(3);
        this.longitude = cursor.getDouble(4);
        this.dateTakenInMs = cursor.getLong(5);
        this.dateAddedInSec = cursor.getLong(6);
        this.dateModifiedInSec = cursor.getLong(7);
        this.filePath = cursor.getString(8);
        this.rotation = cursor.getInt(9);
        this.bucketId = cursor.getInt(10);
        this.fileSize = cursor.getLong(11);
        this.width = cursor.getInt(12);
        this.height = cursor.getInt(13);
    }

    @Override
    protected boolean updateFromCursor(Cursor cursor) {
        UpdateHelper updateHelper = new UpdateHelper();
        this.id = updateHelper.update(this.id, cursor.getInt(0));
        this.caption = (String) updateHelper.update(this.caption, cursor.getString(1));
        this.mimeType = (String) updateHelper.update(this.mimeType, cursor.getString(2));
        this.latitude = updateHelper.update(this.latitude, cursor.getDouble(3));
        this.longitude = updateHelper.update(this.longitude, cursor.getDouble(4));
        this.dateTakenInMs = updateHelper.update(this.dateTakenInMs, cursor.getLong(5));
        this.dateAddedInSec = updateHelper.update(this.dateAddedInSec, cursor.getLong(6));
        this.dateModifiedInSec = updateHelper.update(this.dateModifiedInSec, cursor.getLong(7));
        this.filePath = (String) updateHelper.update(this.filePath, cursor.getString(8));
        this.rotation = updateHelper.update(this.rotation, cursor.getInt(9));
        this.bucketId = updateHelper.update(this.bucketId, cursor.getInt(10));
        this.fileSize = updateHelper.update(this.fileSize, cursor.getLong(11));
        this.width = updateHelper.update(this.width, cursor.getInt(12));
        this.height = updateHelper.update(this.height, cursor.getInt(13));
        return updateMediaData(cursor) || updateHelper.isUpdated();
    }

    @Override
    public ThreadPool.Job<Bitmap> requestImage(int i) {
        return new LocalImageRequest(this.mApplication, this.mPath, this.dateModifiedInSec, i, this.filePath, this.mExtItem, this.mMediaData);
    }

    public static class LocalImageRequest extends ImageCacheRequest {
        private ExtItem mData;
        private boolean mIsCameraRollCover;
        private boolean mIsScreenShotCover;
        private String mLocalFilePath;
        private MediaData mMediaData;

        @Override
        public Bitmap resizeAndCropFancyThumbnail(Bitmap bitmap, MediaItem mediaItem, int i) {
            return super.resizeAndCropFancyThumbnail(bitmap, mediaItem, i);
        }

        @Override
        public Bitmap run(ThreadPool.JobContext jobContext) {
            return super.run(jobContext);
        }

        LocalImageRequest(GalleryApp galleryApp, Path path, long j, int i, String str, ExtItem extItem, MediaData mediaData) {
            super(galleryApp, path, j, i, MediaItem.getTargetSize(i));
            this.mLocalFilePath = str;
            this.mData = extItem;
            this.mMediaData = mediaData;
            this.mIsCameraRollCover = isCameraRollCover(galleryApp, this.mMediaData, path);
            this.mIsScreenShotCover = isScreenShotCover(galleryApp, this.mMediaData, path);
        }

        @Override
        public Bitmap onDecodeOriginal(ThreadPool.JobContext jobContext, int i) throws Throwable {
            Bitmap bitmapDecodeThumbnail;
            if (this.mData != null) {
                ExtItem.Thumbnail thumbnail = this.mData.getThumbnail(FeatureHelper.convertToThumbType(i));
                if (thumbnail != null && thumbnail.mNeedClearCache) {
                    clearCache();
                }
                if (thumbnail != null && thumbnail.mBitmap != null) {
                    return thumbnail.mBitmap;
                }
                if (thumbnail != null && thumbnail.mBitmap == null && !thumbnail.mStillNeedDecode) {
                    return null;
                }
            } else {
                com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "<onDecodeOriginal> error status, ExtItem is null, localFilePath = " + this.mLocalFilePath);
            }
            if (this.mMediaData != null && DecodeSpecLimitor.isOutOfSpecLimit(this.mMediaData.fileSize, this.mMediaData.width, this.mMediaData.height, this.mMediaData.mimeType)) {
                com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "<LocalImageRequest.onDecodeOriginal> path " + this.mLocalFilePath + ", out of spec limit, abort decoding!");
                return null;
            }
            com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "<LocalImageRequest.onDecodeOriginal> onDecodeOriginal,type:" + i);
            TraceHelper.beginSection(">>>>LocalImage-onDecodeOriginal");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            int targetSize = MediaItem.getTargetSize(i);
            TraceHelper.beginSection(">>>>LocalImage-onDecodeOriginal-decodeThumbnail");
            if (i == 3) {
                if (this.mIsCameraRollCover || this.mIsScreenShotCover) {
                    targetSize = FancyHelper.getScreenWidthAtFancyMode();
                    com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "<onDecodeOri> mIsCameraRollCover or mIsScreenShotCover, targetSize " + targetSize);
                }
                bitmapDecodeThumbnail = FancyHelper.decodeThumbnail(jobContext, this.mLocalFilePath, options, targetSize, i);
            } else {
                if (i == 4 && this.mMediaData != null) {
                    processOptions(this.mMediaData.mimeType, options);
                }
                bitmapDecodeThumbnail = DecodeUtils.decodeThumbnail(jobContext, this.mLocalFilePath, options, targetSize, i);
            }
            if (this.mMediaData != null) {
                bitmapDecodeThumbnail = BitmapUtils.clearAlphaValueIfPng(bitmapDecodeThumbnail, this.mMediaData.mimeType, true);
            }
            TraceHelper.endSection();
            TraceHelper.endSection();
            com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "<LocalImageRequest.onDecodeOriginal> finish, return bitmap: " + bitmapDecodeThumbnail);
            return bitmapDecodeThumbnail;
        }
    }

    @Override
    public ThreadPool.Job<BitmapRegionDecoder> requestLargeImage() {
        return new LocalLargeImageRequest(this.filePath);
    }

    public static class LocalLargeImageRequest implements ThreadPool.Job<BitmapRegionDecoder> {
        String mLocalFilePath;

        public LocalLargeImageRequest(String str) {
            this.mLocalFilePath = str;
        }

        @Override
        public BitmapRegionDecoder run(ThreadPool.JobContext jobContext) {
            return DecodeUtils.createBitmapRegionDecoder(jobContext, this.mLocalFilePath, false);
        }
    }

    @Override
    public int getSupportedOperations() {
        int i;
        if (com.android.gallery3d.common.BitmapUtils.isSupportedByRegionDecoder(this.mimeType)) {
            i = 132717;
            float fMax = sThumbnailTargetSize / Math.max(this.width, this.height);
            int i2 = (int) (this.width * fMax);
            if (Math.max(0, Utils.ceilLog2(this.width / i2)) == 0 || (FeatureConfig.sIsLowRamDevice && this.width * this.height > 12582912)) {
                com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "<getSupportedOperations> item thumbWidth " + i2 + " scale " + fMax);
                com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "<getSupportedOperations> item not support full image, width " + this.width + " sthumbnailsize " + sThumbnailTargetSize);
                com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "<getSupportedOperations> sIsLowRamDevice " + FeatureConfig.sIsLowRamDevice + ", width * height is " + (this.width * this.height));
                i = 132653;
            }
        } else {
            i = 132141;
        }
        if (com.android.gallery3d.common.BitmapUtils.isRotationSupported(this.mimeType)) {
            i |= 2;
        }
        if (GalleryUtils.isValidLocation(this.latitude, this.longitude)) {
            i |= 16;
        }
        return FeatureHelper.mergeSupportOperations(i, this.mExtItem.getSupportedOperations(), this.mExtItem.getNotSupportedOperations());
    }

    @Override
    public void getPanoramaSupport(MediaObject.PanoramaSupportCallback panoramaSupportCallback) {
        this.mPanoramaMetadata.getPanoramaSupport(this.mApplication, panoramaSupportCallback);
    }

    @Override
    public void delete() throws Throwable {
        GalleryUtils.assertNotInRenderThread();
        this.mExtItem.delete();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = this.mApplication.getContentResolver();
        SaveImage.deleteAuxFiles(contentResolver, getContentUri());
        contentResolver.delete(uri, "_id=?", new String[]{String.valueOf(this.id)});
        this.mApplication.getDataManager().broadcastUpdatePicture();
    }

    @Override
    public void rotate(int i) throws Throwable {
        GalleryUtils.assertNotInRenderThread();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentValues contentValues = new ContentValues();
        int i2 = (this.rotation + i) % 360;
        if (i2 < 0) {
            i2 += 360;
        }
        if (this.mimeType.equalsIgnoreCase("image/jpeg")) {
            ExifInterface exifInterface = new ExifInterface();
            ExifTag exifTagBuildTag = exifInterface.buildTag(ExifInterface.TAG_ORIENTATION, Short.valueOf(ExifInterface.getOrientationValueForRotation(i2)));
            if (exifTagBuildTag != null) {
                exifInterface.setTag(exifTagBuildTag);
                try {
                    exifInterface.forceRewriteExif(this.filePath);
                    this.fileSize = new File(this.filePath).length();
                    contentValues.put("_size", Long.valueOf(this.fileSize));
                } catch (FileNotFoundException e) {
                    com.mediatek.gallery3d.util.Log.w("Gallery2/LocalImage", "cannot find file to set exif: " + this.filePath);
                } catch (IOException e2) {
                    com.mediatek.gallery3d.util.Log.w("Gallery2/LocalImage", "cannot set exif data: " + this.filePath);
                    try {
                        String str = this.filePath + "." + Long.toString(System.currentTimeMillis());
                        exifInterface.writeExif(this.filePath, str);
                        File file = new File(str);
                        com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "Temporal file's name: " + file.getName());
                        File file2 = new File(this.filePath);
                        file.renameTo(file2);
                        contentValues.put("_size", Long.valueOf(file2.length()));
                    } catch (FileNotFoundException e3) {
                        com.mediatek.gallery3d.util.Log.w("Gallery2/LocalImage", "cannot find file which has not Exif header: " + this.filePath);
                    } catch (IOException e4) {
                        com.mediatek.gallery3d.util.Log.w("Gallery2/LocalImage", "cannot set exif: " + this.filePath);
                    }
                }
            } else {
                com.mediatek.gallery3d.util.Log.w("Gallery2/LocalImage", "Could not build tag: " + ExifInterface.TAG_ORIENTATION);
            }
        }
        contentValues.put(ExtFieldsUtils.VIDEO_ROTATION_FIELD, Integer.valueOf(i2));
        if (FancyHelper.isFancyLayoutSupported()) {
            this.mApplication.getImageCacheService().clearImageData(this.mPath, this.dateModifiedInSec, 3);
            com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "<rotate> <Fancy> clear FANCYTHUMBNAIL" + this.mPath);
        }
        this.mApplication.getContentResolver().update(uri, contentValues, "_id=?", new String[]{String.valueOf(this.id)});
    }

    @Override
    public Uri getContentUri() {
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(this.id)).build();
    }

    @Override
    public int getMediaType() {
        return 2;
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails mediaDetailsConvertStringArrayToDetails = FeatureHelper.convertStringArrayToDetails(this.mExtItem.getDetails());
        if (mediaDetailsConvertStringArrayToDetails != null) {
            return mediaDetailsConvertStringArrayToDetails;
        }
        MediaDetails details = super.getDetails();
        details.addDetail(7, Integer.valueOf(this.rotation));
        if ("image/jpeg".equals(this.mimeType)) {
            MediaDetails.extractExifInfo(details, this.filePath);
        } else if ("image/x-adobe-dng".equalsIgnoreCase(this.mimeType)) {
            MediaDetails.extractDNGExifInfo(details, this.filePath);
        }
        return details;
    }

    @Override
    public int getRotation() {
        return this.rotation;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public String getFilePath() {
        return this.filePath;
    }

    private boolean updateMediaData(Cursor cursor) {
        MediaData mediaData = this.mMediaData;
        if (cursor == null) {
            com.mediatek.gallery3d.util.Log.d("Gallery2/LocalImage", "<updateMediaData> other, cursor is null, return", new Throwable());
            return false;
        }
        synchronized (this.mMediaDataLock) {
            this.mMediaData = MediaDataParser.parseLocalImageMediaData(cursor);
            this.mExtItem = PhotoPlayFacade.getMediaCenter().getItem(this.mMediaData);
        }
        if (mediaData == null || mediaData.mediaType.equals(this.mMediaData.mediaType)) {
            return false;
        }
        this.mApplication.getImageCacheService().clearImageData(this.mPath, this.dateModifiedInSec, 2);
        return true;
    }

    public static String[] getProjection() {
        if (sExtProjection == null) {
            sExtProjection = ExtFields.getImageProjection(PROJECTION);
        }
        return sExtProjection;
    }
}
