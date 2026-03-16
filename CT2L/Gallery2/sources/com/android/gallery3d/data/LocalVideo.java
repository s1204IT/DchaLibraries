package com.android.gallery3d.data;

import android.content.ContentResolver;
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

public class LocalVideo extends LocalMediaItem {
    static final Path ITEM_PATH = Path.fromString("/local/video/item");
    static final String[] PROJECTION = {"_id", "title", "mime_type", "latitude", "longitude", "datetaken", "date_added", "date_modified", "_data", "duration", "bucket_id", "_size", "resolution"};
    public int durationInSec;
    private final GalleryApp mApplication;

    public LocalVideo(Path path, GalleryApp application, Cursor cursor) {
        super(path, nextVersionNumber());
        this.mApplication = application;
        loadFromCursor(cursor);
    }

    public LocalVideo(Path path, GalleryApp context, int id) {
        super(path, nextVersionNumber());
        this.mApplication = context;
        ContentResolver resolver = this.mApplication.getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
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
        this.durationInSec = cursor.getInt(9) / 1000;
        this.bucketId = cursor.getInt(10);
        this.fileSize = cursor.getLong(11);
        parseResolution(cursor.getString(12));
    }

    private void parseResolution(String resolution) {
        int m;
        if (resolution != null && (m = resolution.indexOf(120)) != -1) {
            try {
                int w = Integer.parseInt(resolution.substring(0, m));
                int h = Integer.parseInt(resolution.substring(m + 1));
                this.width = w;
                this.height = h;
            } catch (Throwable t) {
                Log.w("LocalVideo", t);
            }
        }
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
        this.durationInSec = uh.update(this.durationInSec, cursor.getInt(9) / 1000);
        this.bucketId = uh.update(this.bucketId, cursor.getInt(10));
        this.fileSize = uh.update(this.fileSize, cursor.getLong(11));
        return uh.isUpdated();
    }

    @Override
    public ThreadPool.Job<Bitmap> requestImage(int type) {
        return new LocalVideoRequest(this.mApplication, getPath(), this.dateModifiedInSec, type, this.filePath);
    }

    public static class LocalVideoRequest extends ImageCacheRequest {
        private String mLocalFilePath;

        @Override
        public Bitmap run(ThreadPool.JobContext jobContext) {
            return super.run(jobContext);
        }

        LocalVideoRequest(GalleryApp application, Path path, long timeModified, int type, String localFilePath) {
            super(application, path, timeModified, type, MediaItem.getTargetSize(type));
            this.mLocalFilePath = localFilePath;
        }

        @Override
        public Bitmap onDecodeOriginal(ThreadPool.JobContext jc, int type) {
            Bitmap bitmap = BitmapUtils.createVideoThumbnail(this.mLocalFilePath);
            if (bitmap == null || jc.isCancelled()) {
                return null;
            }
            return bitmap;
        }
    }

    @Override
    public ThreadPool.Job<BitmapRegionDecoder> requestLargeImage() {
        throw new UnsupportedOperationException("Cannot regquest a large image to a local video!");
    }

    @Override
    public int getSupportedOperations() {
        return 68741;
    }

    @Override
    public void delete() {
        GalleryUtils.assertNotInRenderThread();
        Uri baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        this.mApplication.getContentResolver().delete(baseUri, "_id=?", new String[]{String.valueOf(this.id)});
    }

    @Override
    public void rotate(int degrees) {
    }

    @Override
    public Uri getContentUri() {
        Uri baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        return baseUri.buildUpon().appendPath(String.valueOf(this.id)).build();
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
        int s = this.durationInSec;
        if (s > 0) {
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
}
