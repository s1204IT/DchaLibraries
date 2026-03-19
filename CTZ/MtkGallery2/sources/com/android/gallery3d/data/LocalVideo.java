package com.android.gallery3d.data;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.provider.MediaStore;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool;
import com.android.gallery3d.util.UpdateHelper;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.adapter.MediaDataParser;
import com.mediatek.gallery3d.adapter.PhotoPlayFacade;
import com.mediatek.gallery3d.layout.FancyHelper;
import com.mediatek.gallery3d.util.SystemPropertyUtils;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.base.ExtFields;
import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.util.ExtFieldsUtils;
import com.mediatek.galleryportable.PerfServiceUtils;
import com.mediatek.galleryportable.TraceHelper;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class LocalVideo extends LocalMediaItem {
    private static String[] sExtProjection;
    public int durationInSec;
    private final GalleryApp mApplication;
    private boolean mIsFancyLayoutSupported;
    private int mOrientation;
    static final Path ITEM_PATH = Path.fromString("/local/video/item");
    private static final String[] PROJECTION = {BookmarkEnhance.COLUMN_ID, "title", BookmarkEnhance.COLUMN_MEDIA_TYPE, "latitude", "longitude", "datetaken", BookmarkEnhance.COLUMN_ADD_DATE, "date_modified", BookmarkEnhance.COLUMN_DATA, SchemaSymbols.ATTVAL_DURATION, "bucket_id", "_size", "resolution"};
    private static final int BOOST_VIDEO_DECODE_TIME_OUT = SystemPropertyUtils.getInt("debug.gallery.videoboost", 300);

    public LocalVideo(Path path, GalleryApp galleryApp, Cursor cursor) {
        super(path, nextVersionNumber());
        this.mIsFancyLayoutSupported = FancyHelper.isFancyLayoutSupported();
        this.mOrientation = -1;
        this.mApplication = galleryApp;
        loadFromCursor(cursor);
        synchronized (this.mMediaDataLock) {
            this.mMediaData = MediaDataParser.parseLocalVideoMediaData(this, cursor);
            this.mExtItem = PhotoPlayFacade.getMediaCenter().getItem(this.mMediaData);
        }
    }

    public LocalVideo(Path path, GalleryApp galleryApp, int i) {
        super(path, nextVersionNumber());
        this.mIsFancyLayoutSupported = FancyHelper.isFancyLayoutSupported();
        this.mOrientation = -1;
        this.mApplication = galleryApp;
        Cursor itemCursor = LocalAlbum.getItemCursor(this.mApplication.getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI, getProjection(), i);
        if (itemCursor == null) {
            throw new RuntimeException("cannot get cursor for: " + path);
        }
        try {
            if (itemCursor.moveToNext()) {
                loadFromCursor(itemCursor);
                synchronized (this.mMediaDataLock) {
                    this.mMediaData = MediaDataParser.parseLocalVideoMediaData(this, itemCursor);
                    this.mExtItem = PhotoPlayFacade.getMediaCenter().getItem(this.mMediaData);
                }
                return;
            }
            throw new RuntimeException("cannot find data for: " + path);
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
        this.durationInSec = cursor.getInt(9) / 1000;
        this.bucketId = cursor.getInt(10);
        this.fileSize = cursor.getLong(11);
        parseResolution(cursor.getString(12));
    }

    private void parseResolution(String str) {
        int iIndexOf;
        if (str == null || (iIndexOf = str.indexOf(120)) == -1) {
            return;
        }
        try {
            int i = Integer.parseInt(str.substring(0, iIndexOf));
            int i2 = Integer.parseInt(str.substring(iIndexOf + 1));
            this.width = i;
            this.height = i2;
        } catch (Throwable th) {
            Log.w("Gallery2/LocalVideo", th);
        }
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
        this.durationInSec = updateHelper.update(this.durationInSec, cursor.getInt(9) / 1000);
        this.bucketId = updateHelper.update(this.bucketId, cursor.getInt(10));
        this.fileSize = updateHelper.update(this.fileSize, cursor.getLong(11));
        synchronized (this.mMediaDataLock) {
            this.mMediaData = MediaDataParser.parseLocalVideoMediaData(this, cursor);
            this.mExtItem = PhotoPlayFacade.getMediaCenter().getItem(this.mMediaData);
        }
        return updateHelper.isUpdated();
    }

    @Override
    public ThreadPool.Job<Bitmap> requestImage(int i) {
        return new LocalVideoRequest(this.mApplication, getPath(), this.dateModifiedInSec, i, this.filePath, this.mExtItem);
    }

    public static class LocalVideoRequest extends ImageCacheRequest {
        private ExtItem mData;
        private String mLocalFilePath;

        @Override
        public Bitmap resizeAndCropFancyThumbnail(Bitmap bitmap, MediaItem mediaItem, int i) {
            return super.resizeAndCropFancyThumbnail(bitmap, mediaItem, i);
        }

        @Override
        public Bitmap run(ThreadPool.JobContext jobContext) {
            return super.run(jobContext);
        }

        LocalVideoRequest(GalleryApp galleryApp, Path path, long j, int i, String str, ExtItem extItem) {
            super(galleryApp, path, j, i, MediaItem.getTargetSize(i));
            this.mLocalFilePath = str;
            this.mData = extItem;
        }

        @Override
        public Bitmap onDecodeOriginal(ThreadPool.JobContext jobContext, int i) throws Throwable {
            TraceHelper.beginSection(">>>>LocalVideo-onDecodeOriginal");
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
            long jCurrentTimeMillis = System.currentTimeMillis();
            Log.d("Gallery2/LocalVideo", "create video thumb begins at" + jCurrentTimeMillis);
            PerfServiceUtils.boostEnableTimeoutMs(LocalVideo.BOOST_VIDEO_DECODE_TIME_OUT);
            Log.d("Gallery2/LocalVideo", "<boostEnableTimeoutMs> Video decode boost(ms): " + LocalVideo.BOOST_VIDEO_DECODE_TIME_OUT);
            Bitmap bitmapCreateVideoThumbnail = BitmapUtils.createVideoThumbnail(this.mLocalFilePath);
            Log.d("Gallery2/LocalVideo", "create video thumb costs " + (System.currentTimeMillis() - jCurrentTimeMillis));
            if (bitmapCreateVideoThumbnail == null || jobContext.isCancelled()) {
                return null;
            }
            TraceHelper.endSection();
            return bitmapCreateVideoThumbnail;
        }
    }

    @Override
    public ThreadPool.Job<BitmapRegionDecoder> requestLargeImage() {
        throw new UnsupportedOperationException("Cannot regquest a large image to a local video!");
    }

    @Override
    public int getSupportedOperations() {
        return FeatureHelper.mergeSupportOperations(68741, this.mExtItem.getSupportedOperations(), this.mExtItem.getNotSupportedOperations());
    }

    @Override
    public void delete() {
        GalleryUtils.assertNotInRenderThread();
        this.mApplication.getContentResolver().delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "_id=?", new String[]{String.valueOf(this.id)});
        this.mApplication.getDataManager().broadcastUpdatePicture();
    }

    @Override
    public void rotate(int i) {
    }

    @Override
    public Uri getContentUri() {
        return MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(String.valueOf(this.id)).build();
    }

    @Override
    public Uri getPlayUri() {
        return getContentUri();
    }

    @Override
    public int getMediaType() {
        return 4;
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        if (this.durationInSec > 0) {
            details.addDetail(8, GalleryUtils.formatDuration(this.mApplication.getAndroidContext(), this.durationInSec));
        }
        return details;
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

    public int getOrientation() {
        if (this.mOrientation == -1) {
            this.mOrientation = ExtFieldsUtils.getVideoRotation(this.mMediaData);
        }
        Log.d("Gallery2/LocalVideo", "<getOrientation> <Fancy> mOrientation " + this.mOrientation + ", " + getName());
        return this.mOrientation;
    }

    public static String[] getProjection() {
        if (sExtProjection == null) {
            sExtProjection = ExtFields.getVideoProjection(PROJECTION);
        }
        return sExtProjection;
    }
}
