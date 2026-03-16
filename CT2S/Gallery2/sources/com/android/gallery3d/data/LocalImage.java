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
import android.support.v4.app.NotificationCompat;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.PanoramaMetadataSupport;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.filtershow.tools.SaveImage;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.UpdateHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class LocalImage extends LocalMediaItem {
    static final Path ITEM_PATH = Path.fromString("/local/image/item");
    static final String[] PROJECTION = {"_id", "title", "mime_type", "latitude", "longitude", "datetaken", "date_added", "date_modified", "_data", "orientation", "bucket_id", "_size", "0", "0"};
    private final GalleryApp mApplication;
    private PanoramaMetadataSupport mPanoramaMetadata;
    public int rotation;

    static {
        updateWidthAndHeightProjection();
    }

    @TargetApi(NotificationCompat.FLAG_AUTO_CANCEL)
    private static void updateWidthAndHeightProjection() {
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            PROJECTION[12] = "width";
            PROJECTION[13] = "height";
        }
    }

    public LocalImage(Path path, GalleryApp application, Cursor cursor) {
        super(path, nextVersionNumber());
        this.mPanoramaMetadata = new PanoramaMetadataSupport(this);
        this.mApplication = application;
        loadFromCursor(cursor);
    }

    public LocalImage(Path path, GalleryApp application, int id) {
        super(path, nextVersionNumber());
        this.mPanoramaMetadata = new PanoramaMetadataSupport(this);
        this.mApplication = application;
        ContentResolver resolver = this.mApplication.getContentResolver();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = LocalAlbum.getItemCursor(resolver, uri, PROJECTION, id);
        if (cursor == null) {
            throw new RuntimeException("cannot get cursor for: " + path);
        }
        try {
            if (cursor.moveToNext()) {
                loadFromCursor(cursor);
                return;
            }
            throw new RuntimeException("cannot find data for: " + path);
        } finally {
            cursor.close();
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
        UpdateHelper uh = new UpdateHelper();
        this.id = uh.update(this.id, cursor.getInt(0));
        this.caption = (String) uh.update(this.caption, cursor.getString(1));
        this.mimeType = (String) uh.update(this.mimeType, cursor.getString(2));
        this.latitude = uh.update(this.latitude, cursor.getDouble(3));
        this.longitude = uh.update(this.longitude, cursor.getDouble(4));
        this.dateTakenInMs = uh.update(this.dateTakenInMs, cursor.getLong(5));
        this.dateAddedInSec = uh.update(this.dateAddedInSec, cursor.getLong(6));
        this.dateModifiedInSec = uh.update(this.dateModifiedInSec, cursor.getLong(7));
        this.filePath = (String) uh.update(this.filePath, cursor.getString(8));
        this.rotation = uh.update(this.rotation, cursor.getInt(9));
        this.bucketId = uh.update(this.bucketId, cursor.getInt(10));
        this.fileSize = uh.update(this.fileSize, cursor.getLong(11));
        this.width = uh.update(this.width, cursor.getInt(12));
        this.height = uh.update(this.height, cursor.getInt(13));
        return uh.isUpdated();
    }

    @Override
    public ThreadPool.Job<Bitmap> requestImage(int type) {
        android.util.Log.i("LocalImage", "!!! request image : " + this.filePath);
        return new LocalImageRequest(this.mApplication, this.mPath, this.dateModifiedInSec, type, this.filePath);
    }

    public static class LocalImageRequest extends ImageCacheRequest {
        private String mLocalFilePath;

        @Override
        public Bitmap run(ThreadPool.JobContext jobContext) {
            return super.run(jobContext);
        }

        LocalImageRequest(GalleryApp application, Path path, long timeModified, int type, String localFilePath) {
            super(application, path, timeModified, type, MediaItem.getTargetSize(type));
            this.mLocalFilePath = localFilePath;
        }

        @Override
        public Bitmap onDecodeOriginal(ThreadPool.JobContext jc, int type) {
            Bitmap bitmap;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            int targetSize = MediaItem.getTargetSize(type);
            if (type == 2) {
                ExifInterface exif = new ExifInterface();
                byte[] thumbData = null;
                try {
                    exif.readExif(this.mLocalFilePath);
                    thumbData = exif.getThumbnail();
                } catch (FileNotFoundException e) {
                    android.util.Log.w("LocalImage", "failed to find file to read thumbnail: " + this.mLocalFilePath);
                } catch (IOException e2) {
                    android.util.Log.w("LocalImage", "failed to get thumbnail from: " + this.mLocalFilePath);
                }
                if (thumbData != null && (bitmap = DecodeUtils.decodeIfBigEnough(jc, thumbData, options, targetSize)) != null) {
                    return bitmap;
                }
            }
            return DecodeUtils.decodeThumbnail(jc, this.mLocalFilePath, options, targetSize, type);
        }
    }

    @Override
    public ThreadPool.Job<BitmapRegionDecoder> requestLargeImage() {
        android.util.Log.i("LocalImage", "!!! request large image : " + this.filePath);
        return new LocalLargeImageRequest(this.filePath);
    }

    public static class LocalLargeImageRequest implements ThreadPool.Job<BitmapRegionDecoder> {
        String mLocalFilePath;

        public LocalLargeImageRequest(String localFilePath) {
            this.mLocalFilePath = localFilePath;
        }

        @Override
        public BitmapRegionDecoder run(ThreadPool.JobContext jc) {
            return DecodeUtils.createBitmapRegionDecoder(jc, this.mLocalFilePath, false);
        }
    }

    @Override
    public int getSupportedOperations() {
        int operation = 132141;
        if (BitmapUtils.isSupportedByRegionDecoder(this.mimeType)) {
            operation = 132141 | 576;
        }
        if (BitmapUtils.isRotationSupported(this.mimeType)) {
            operation |= 2;
        }
        if (GalleryUtils.isValidLocation(this.latitude, this.longitude)) {
            return operation | 16;
        }
        return operation;
    }

    @Override
    public void getPanoramaSupport(MediaObject.PanoramaSupportCallback callback) {
        this.mPanoramaMetadata.getPanoramaSupport(this.mApplication, callback);
    }

    @Override
    public void delete() {
        GalleryUtils.assertNotInRenderThread();
        Uri baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = this.mApplication.getContentResolver();
        SaveImage.deleteAuxFiles(contentResolver, getContentUri());
        contentResolver.delete(baseUri, "_id=?", new String[]{String.valueOf(this.id)});
    }

    @Override
    public void rotate(int degrees) throws Throwable {
        GalleryUtils.assertNotInRenderThread();
        Uri baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentValues values = new ContentValues();
        int rotation = (this.rotation + degrees) % 360;
        if (rotation < 0) {
            rotation += 360;
        }
        if (this.mimeType.equalsIgnoreCase("image/jpeg")) {
            ExifInterface exifInterface = new ExifInterface();
            ExifTag tag = exifInterface.buildTag(ExifInterface.TAG_ORIENTATION, Short.valueOf(ExifInterface.getOrientationValueForRotation(rotation)));
            if (tag != null) {
                exifInterface.setTag(tag);
                try {
                    exifInterface.forceRewriteExif(this.filePath);
                    this.fileSize = new File(this.filePath).length();
                    values.put("_size", Long.valueOf(this.fileSize));
                } catch (FileNotFoundException e) {
                    android.util.Log.w("LocalImage", "cannot find file to set exif: " + this.filePath);
                } catch (IOException e2) {
                    android.util.Log.w("LocalImage", "cannot set exif data: " + this.filePath);
                }
            } else {
                android.util.Log.w("LocalImage", "Could not build tag: " + ExifInterface.TAG_ORIENTATION);
            }
        }
        values.put("orientation", Integer.valueOf(rotation));
        this.mApplication.getContentResolver().update(baseUri, values, "_id=?", new String[]{String.valueOf(this.id)});
    }

    @Override
    public Uri getContentUri() {
        Uri baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        return baseUri.buildUpon().appendPath(String.valueOf(this.id)).build();
    }

    @Override
    public int getMediaType() {
        return 2;
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        details.addDetail(7, Integer.valueOf(this.rotation));
        if ("image/jpeg".equals(this.mimeType)) {
            MediaDetails.extractExifInfo(details, this.filePath);
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
}
