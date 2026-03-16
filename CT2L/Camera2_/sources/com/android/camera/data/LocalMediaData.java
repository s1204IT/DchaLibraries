package com.android.camera.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.camera.Storage;
import com.android.camera.data.LocalData;
import com.android.camera.debug.Log;
import com.android.camera.tinyplanet.TinyPlanetFragment;
import com.android.camera2.R;
import com.android.ex.camera2.portability.Size;
import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public abstract class LocalMediaData implements LocalData {
    private static final String CAMERA_PATH = Storage.DIRECTORY + "%";
    private static final int JPEG_COMPRESS_QUALITY = 90;
    private static final BitmapEncoder JPEG_ENCODER = new BitmapEncoder(Bitmap.CompressFormat.JPEG, JPEG_COMPRESS_QUALITY);
    private static final int MEDIASTORE_THUMB_HEIGHT = 384;
    private static final int MEDIASTORE_THUMB_WIDTH = 512;
    static final int QUERY_ALL_MEDIA_ID = -1;
    private static final String SELECT_BY_PATH = "_data LIKE ?";
    protected final long mContentId;
    protected final long mDateModifiedInSeconds;
    protected final long mDateTakenInMilliSeconds;
    protected final int mHeight;
    protected final double mLatitude;
    protected final double mLongitude;
    protected final String mMimeType;
    protected final String mPath;
    protected final long mSizeInBytes;
    protected final String mTitle;
    protected final int mWidth;
    protected Boolean mUsing = false;
    protected final Bundle mMetaData = new Bundle();

    private interface CursorToLocalData {
        LocalData build(Cursor cursor);
    }

    @Override
    public abstract int getViewType();

    public LocalMediaData(long contentId, String title, String mimeType, long dateTakenInMilliSeconds, long dateModifiedInSeconds, String path, int width, int height, long sizeInBytes, double latitude, double longitude) {
        this.mContentId = contentId;
        this.mTitle = title;
        this.mMimeType = mimeType;
        this.mDateTakenInMilliSeconds = dateTakenInMilliSeconds;
        this.mDateModifiedInSeconds = dateModifiedInSeconds;
        this.mPath = path;
        this.mWidth = width;
        this.mHeight = height;
        this.mSizeInBytes = sizeInBytes;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
    }

    private static List<LocalData> queryLocalMediaData(ContentResolver contentResolver, Uri contentUri, String[] projection, long minimumId, String orderBy, CursorToLocalData builder) {
        String[] selectionArgs = {CAMERA_PATH, Long.toString(minimumId)};
        Cursor cursor = contentResolver.query(contentUri, projection, "_data LIKE ? AND _id > ?", selectionArgs, orderBy);
        List<LocalData> result = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                LocalData data = builder.build(cursor);
                if (data != null) {
                    result.add(data);
                } else {
                    int dataIndex = cursor.getColumnIndexOrThrow("_data");
                    Log.e(TAG, "Error loading data:" + cursor.getString(dataIndex));
                }
            }
            cursor.close();
        }
        return result;
    }

    @Override
    public long getDateTaken() {
        return this.mDateTakenInMilliSeconds;
    }

    @Override
    public long getDateModified() {
        return this.mDateModifiedInSeconds;
    }

    @Override
    public long getContentId() {
        return this.mContentId;
    }

    @Override
    public String getTitle() {
        return this.mTitle;
    }

    @Override
    public int getWidth() {
        return this.mWidth;
    }

    @Override
    public int getHeight() {
        return this.mHeight;
    }

    @Override
    public int getRotation() {
        return 0;
    }

    @Override
    public String getPath() {
        return this.mPath;
    }

    @Override
    public long getSizeInBytes() {
        return this.mSizeInBytes;
    }

    @Override
    public boolean isUIActionSupported(int action) {
        return false;
    }

    @Override
    public boolean isDataActionSupported(int action) {
        return false;
    }

    @Override
    public boolean delete(Context context) {
        File f = new File(this.mPath);
        return f.delete();
    }

    @Override
    public void onFullScreen(boolean fullScreen) {
    }

    @Override
    public boolean canSwipeInFullScreen() {
        return true;
    }

    protected ImageView fillImageView(Context context, ImageView v, int thumbWidth, int thumbHeight, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgress) {
        Glide.with(context).loadFromMediaStore(getUri(), this.mMimeType, this.mDateModifiedInSeconds, 0).fitCenter().placeholder(placeHolderResourceId).into(v);
        v.setContentDescription(context.getResources().getString(R.string.media_date_content_description, getReadableDate(this.mDateModifiedInSeconds)));
        return v;
    }

    @Override
    public View getView(Context context, View recycled, int thumbWidth, int thumbHeight, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgress, LocalData.ActionCallback actionCallback) {
        ImageView imageView;
        if (recycled != null) {
            imageView = (ImageView) recycled;
        } else {
            imageView = (ImageView) LayoutInflater.from(context).inflate(R.layout.filmstrip_image, (ViewGroup) null);
            imageView.setTag(R.id.mediadata_tag_viewtype, Integer.valueOf(getItemViewType().ordinal()));
        }
        return fillImageView(context, imageView, thumbWidth, thumbHeight, placeHolderResourceId, adapter, isInProgress);
    }

    @Override
    public void loadFullImage(Context context, int thumbWidth, int thumbHeight, View view, LocalDataAdapter adapter) {
    }

    @Override
    public void prepare() {
        synchronized (this.mUsing) {
            this.mUsing = true;
        }
    }

    @Override
    public void recycle(View view) {
        synchronized (this.mUsing) {
            this.mUsing = false;
        }
    }

    @Override
    public double[] getLatLong() {
        if (this.mLatitude == 0.0d && this.mLongitude == 0.0d) {
            return null;
        }
        return new double[]{this.mLatitude, this.mLongitude};
    }

    protected boolean isUsing() {
        boolean zBooleanValue;
        synchronized (this.mUsing) {
            zBooleanValue = this.mUsing.booleanValue();
        }
        return zBooleanValue;
    }

    @Override
    public String getMimeType() {
        return this.mMimeType;
    }

    @Override
    public MediaDetails getMediaDetails(Context context) {
        MediaDetails mediaDetails = new MediaDetails();
        mediaDetails.addDetail(1, this.mTitle);
        mediaDetails.addDetail(5, Integer.valueOf(this.mWidth));
        mediaDetails.addDetail(6, Integer.valueOf(this.mHeight));
        mediaDetails.addDetail(MediaDetails.INDEX_PATH, this.mPath);
        mediaDetails.addDetail(3, getReadableDate(this.mDateModifiedInSeconds));
        if (this.mSizeInBytes > 0) {
            mediaDetails.addDetail(10, Long.valueOf(this.mSizeInBytes));
        }
        if (this.mLatitude != 0.0d && this.mLongitude != 0.0d) {
            String locationString = String.format(Locale.getDefault(), "%f, %f", Double.valueOf(this.mLatitude), Double.valueOf(this.mLongitude));
            mediaDetails.addDetail(4, locationString);
        }
        return mediaDetails;
    }

    private static String getReadableDate(long dateInSeconds) {
        DateFormat dateFormatter = DateFormat.getDateTimeInstance();
        return dateFormatter.format(new Date(1000 * dateInSeconds));
    }

    @Override
    public Bundle getMetadata() {
        return this.mMetaData;
    }

    @Override
    public boolean isMetadataUpdated() {
        return MetadataLoader.isMetadataCached(this);
    }

    public static final class PhotoData extends LocalMediaData {
        public static final int COL_DATA = 5;
        public static final int COL_DATE_MODIFIED = 4;
        public static final int COL_DATE_TAKEN = 3;
        public static final int COL_HEIGHT = 8;
        public static final int COL_ID = 0;
        public static final int COL_LATITUDE = 10;
        public static final int COL_LONGITUDE = 11;
        public static final int COL_MIME_TYPE = 2;
        public static final int COL_ORIENTATION = 6;
        public static final int COL_SIZE = 9;
        public static final int COL_TITLE = 1;
        public static final int COL_WIDTH = 7;
        private static final int MAXIMUM_TEXTURE_SIZE = 2048;
        private static final String QUERY_ORDER = "_id DESC";
        private static final int mSupportedDataActions = 14;
        private static final int mSupportedUIActions = 7;
        private final int mOrientation;
        private final String mSignature;
        private static final Log.Tag TAG = new Log.Tag("PhotoData");
        static final Uri CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        private static final String[] QUERY_PROJECTION = {"_id", TinyPlanetFragment.ARGUMENT_TITLE, "mime_type", "datetaken", "date_modified", "_data", "orientation", "width", "height", "_size", "latitude", "longitude"};

        public static LocalData fromContentUri(ContentResolver cr, Uri contentUri) {
            List<LocalData> newPhotos = query(cr, contentUri, -1L);
            if (newPhotos.isEmpty()) {
                return null;
            }
            return newPhotos.get(0);
        }

        public PhotoData(long id, String title, String mimeType, long dateTakenInMilliSeconds, long dateModifiedInSeconds, String path, int orientation, int width, int height, long sizeInBytes, double latitude, double longitude) {
            super(id, title, mimeType, dateTakenInMilliSeconds, dateModifiedInSeconds, path, width, height, sizeInBytes, latitude, longitude);
            this.mOrientation = orientation;
            this.mSignature = mimeType + orientation + dateModifiedInSeconds;
        }

        static List<LocalData> query(ContentResolver cr, Uri uri, long lastId) {
            return LocalMediaData.queryLocalMediaData(cr, uri, QUERY_PROJECTION, lastId, QUERY_ORDER, new PhotoDataBuilder());
        }

        private static PhotoData buildFromCursor(Cursor c) {
            long id = c.getLong(0);
            String title = c.getString(1);
            String mimeType = c.getString(2);
            long dateTakenInMilliSeconds = c.getLong(3);
            long dateModifiedInSeconds = c.getLong(4);
            String path = c.getString(5);
            int orientation = c.getInt(6);
            int width = c.getInt(7);
            int height = c.getInt(8);
            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Zero dimension in ContentResolver for " + path + ":" + width + "x" + height);
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    width = opts.outWidth;
                    height = opts.outHeight;
                } else {
                    Log.w(TAG, "Dimension decode failed for " + path);
                    Bitmap b = BitmapFactory.decodeFile(path);
                    if (b == null) {
                        Log.w(TAG, "PhotoData skipped. Decoding " + path + "failed.");
                        return null;
                    }
                    width = b.getWidth();
                    height = b.getHeight();
                    if (width == 0 || height == 0) {
                        Log.w(TAG, "PhotoData skipped. Bitmap size 0 for " + path);
                        return null;
                    }
                }
            }
            long sizeInBytes = c.getLong(9);
            double latitude = c.getDouble(10);
            double longitude = c.getDouble(11);
            return new PhotoData(id, title, mimeType, dateTakenInMilliSeconds, dateModifiedInSeconds, path, orientation, width, height, sizeInBytes, latitude, longitude);
        }

        @Override
        public int getRotation() {
            return this.mOrientation;
        }

        public String toString() {
            return "Photo:,data=" + this.mPath + ",mimeType=" + this.mMimeType + Size.DELIMITER + this.mWidth + "x" + this.mHeight + ",orientation=" + this.mOrientation + ",date=" + new Date(this.mDateTakenInMilliSeconds);
        }

        @Override
        public int getViewType() {
            return 2;
        }

        @Override
        public boolean isUIActionSupported(int action) {
            return (action & 7) == action;
        }

        @Override
        public boolean isDataActionSupported(int action) {
            return (action & mSupportedDataActions) == action;
        }

        @Override
        public boolean delete(Context context) {
            ContentResolver cr = context.getContentResolver();
            cr.delete(CONTENT_URI, "_id=" + this.mContentId, null);
            return super.delete(context);
        }

        @Override
        public Uri getUri() {
            Uri baseUri = CONTENT_URI;
            return baseUri.buildUpon().appendPath(String.valueOf(this.mContentId)).build();
        }

        @Override
        public MediaDetails getMediaDetails(Context context) {
            MediaDetails mediaDetails = super.getMediaDetails(context);
            MediaDetails.extractExifInfo(mediaDetails, this.mPath);
            mediaDetails.addDetail(7, Integer.valueOf(this.mOrientation));
            return mediaDetails;
        }

        @Override
        public int getLocalDataType() {
            return 3;
        }

        @Override
        public LocalData refresh(Context context) {
            PhotoData newData = null;
            Cursor c = context.getContentResolver().query(getUri(), QUERY_PROJECTION, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    newData = buildFromCursor(c);
                }
                c.close();
            }
            return newData;
        }

        @Override
        public String getSignature() {
            return this.mSignature;
        }

        @Override
        protected ImageView fillImageView(Context context, ImageView v, int thumbWidth, int thumbHeight, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgress) {
            loadImage(context, v, thumbWidth, thumbHeight, placeHolderResourceId, false);
            int stringId = R.string.photo_date_content_description;
            if (PanoramaMetadataLoader.isPanorama(this) || PanoramaMetadataLoader.isPanorama360(this)) {
                stringId = R.string.panorama_date_content_description;
            } else if (PanoramaMetadataLoader.isPanoramaAndUseViewer(this)) {
                stringId = R.string.photosphere_date_content_description;
            } else if (RgbzMetadataLoader.hasRGBZData(this)) {
                stringId = R.string.refocus_date_content_description;
            }
            v.setContentDescription(context.getResources().getString(stringId, LocalMediaData.getReadableDate(this.mDateModifiedInSeconds)));
            return v;
        }

        private void loadImage(Context context, ImageView imageView, int thumbWidth, int thumbHeight, int placeHolderResourceId, boolean full) {
            int overrideWidth;
            int overrideHeight;
            BitmapRequestBuilder<Uri, Bitmap> thumbnailRequest;
            if (thumbWidth > 0 && thumbHeight > 0) {
                if (full) {
                    overrideWidth = Math.min(getWidth(), 2048);
                    overrideHeight = Math.min(getHeight(), 2048);
                    thumbnailRequest = loadUri(context).override(thumbWidth, thumbHeight).fitCenter().thumbnail(loadMediaStoreThumb(context));
                } else {
                    overrideWidth = thumbWidth;
                    overrideHeight = thumbHeight;
                    thumbnailRequest = loadMediaStoreThumb(context);
                }
                loadUri(context).placeholder(placeHolderResourceId).fitCenter().override(overrideWidth, overrideHeight).thumbnail(thumbnailRequest).into(imageView);
            }
        }

        private BitmapRequestBuilder<Uri, Bitmap> loadMediaStoreThumb(Context context) {
            return loadUri(context).override(512, LocalMediaData.MEDIASTORE_THUMB_HEIGHT);
        }

        private BitmapRequestBuilder<Uri, Bitmap> loadUri(Context context) {
            return Glide.with(context).loadFromMediaStore(getUri(), this.mMimeType, this.mDateModifiedInSeconds, this.mOrientation).asBitmap().encoder((ResourceEncoder<Bitmap>) LocalMediaData.JPEG_ENCODER);
        }

        @Override
        public void recycle(View view) {
            super.recycle(view);
            if (view != null) {
                Glide.clear(view);
            }
        }

        @Override
        public LocalDataViewType getItemViewType() {
            return LocalDataViewType.PHOTO;
        }

        @Override
        public void loadFullImage(Context context, int thumbWidth, int thumbHeight, View v, LocalDataAdapter adapter) {
            loadImage(context, (ImageView) v, thumbWidth, thumbHeight, 0, true);
        }

        private static class PhotoDataBuilder implements CursorToLocalData {
            private PhotoDataBuilder() {
            }

            @Override
            public PhotoData build(Cursor cursor) {
                return PhotoData.buildFromCursor(cursor);
            }
        }
    }

    public static final class VideoData extends LocalMediaData {
        public static final int COL_DATA = 5;
        public static final int COL_DATE_MODIFIED = 4;
        public static final int COL_DATE_TAKEN = 3;
        public static final int COL_DURATION = 11;
        public static final int COL_HEIGHT = 7;
        public static final int COL_ID = 0;
        public static final int COL_LATITUDE = 9;
        public static final int COL_LONGITUDE = 10;
        public static final int COL_MIME_TYPE = 2;
        public static final int COL_SIZE = 8;
        public static final int COL_TITLE = 1;
        public static final int COL_WIDTH = 6;
        private static final String QUERY_ORDER = "datetaken DESC, _id DESC";
        private static final int mSupportedDataActions = 11;
        private static final int mSupportedUIActions = 3;
        private final long mDurationInSeconds;
        private final String mSignature;
        static final Uri CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        private static final String[] QUERY_PROJECTION = {"_id", TinyPlanetFragment.ARGUMENT_TITLE, "mime_type", "datetaken", "date_modified", "_data", "width", "height", "_size", "latitude", "longitude", "duration"};

        public VideoData(long id, String title, String mimeType, long dateTakenInMilliSeconds, long dateModifiedInSeconds, String path, int width, int height, long sizeInBytes, double latitude, double longitude, long durationInSeconds) {
            super(id, title, mimeType, dateTakenInMilliSeconds, dateModifiedInSeconds, path, width, height, sizeInBytes, latitude, longitude);
            this.mDurationInSeconds = durationInSeconds;
            this.mSignature = mimeType + dateModifiedInSeconds;
        }

        public static LocalData fromContentUri(ContentResolver cr, Uri contentUri) {
            List<LocalData> newVideos = query(cr, contentUri, -1L);
            if (newVideos.isEmpty()) {
                return null;
            }
            return newVideos.get(0);
        }

        static List<LocalData> query(ContentResolver cr, Uri uri, long lastId) {
            return LocalMediaData.queryLocalMediaData(cr, uri, QUERY_PROJECTION, lastId, QUERY_ORDER, new VideoDataBuilder());
        }

        private int getBestWidth() {
            int metadataWidth = VideoRotationMetadataLoader.getWidth(this);
            return metadataWidth > 0 ? metadataWidth : this.mWidth;
        }

        private int getBestHeight() {
            int metadataHeight = VideoRotationMetadataLoader.getHeight(this);
            return metadataHeight > 0 ? metadataHeight : this.mHeight;
        }

        @Override
        public int getWidth() {
            return VideoRotationMetadataLoader.isRotated(this) ? getBestHeight() : getBestWidth();
        }

        @Override
        public int getHeight() {
            return VideoRotationMetadataLoader.isRotated(this) ? getBestWidth() : getBestHeight();
        }

        private static VideoData buildFromCursor(Cursor c) {
            long id = c.getLong(0);
            String title = c.getString(1);
            String mimeType = c.getString(2);
            long dateTakenInMilliSeconds = c.getLong(3);
            long dateModifiedInSeconds = c.getLong(4);
            String path = c.getString(5);
            int width = c.getInt(6);
            int height = c.getInt(7);
            if (width == 0 || height == 0) {
                Log.w(TAG, "failed to retrieve width and height from the media store, defaulting  to camera profile");
                CamcorderProfile profile = CamcorderProfile.get(1);
                width = profile.videoFrameWidth;
                height = profile.videoFrameHeight;
            }
            long sizeInBytes = c.getLong(8);
            double latitude = c.getDouble(9);
            double longitude = c.getDouble(10);
            long durationInSeconds = c.getLong(11) / 1000;
            VideoData d = new VideoData(id, title, mimeType, dateTakenInMilliSeconds, dateModifiedInSeconds, path, width, height, sizeInBytes, latitude, longitude, durationInSeconds);
            return d;
        }

        public String toString() {
            return "Video:,data=" + this.mPath + ",mimeType=" + this.mMimeType + Size.DELIMITER + this.mWidth + "x" + this.mHeight + ",date=" + new Date(this.mDateTakenInMilliSeconds);
        }

        @Override
        public int getViewType() {
            return 2;
        }

        @Override
        public boolean isUIActionSupported(int action) {
            return (action & 3) == action;
        }

        @Override
        public boolean isDataActionSupported(int action) {
            return (action & 11) == action;
        }

        @Override
        public boolean delete(Context context) {
            ContentResolver cr = context.getContentResolver();
            cr.delete(CONTENT_URI, "_id=" + this.mContentId, null);
            return super.delete(context);
        }

        @Override
        public Uri getUri() {
            Uri baseUri = CONTENT_URI;
            return baseUri.buildUpon().appendPath(String.valueOf(this.mContentId)).build();
        }

        @Override
        public MediaDetails getMediaDetails(Context context) {
            MediaDetails mediaDetails = super.getMediaDetails(context);
            String duration = MediaDetails.formatDuration(context, this.mDurationInSeconds);
            mediaDetails.addDetail(8, duration);
            return mediaDetails;
        }

        @Override
        public int getLocalDataType() {
            return 4;
        }

        @Override
        public LocalData refresh(Context context) {
            Cursor c = context.getContentResolver().query(getUri(), QUERY_PROJECTION, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            return buildFromCursor(c);
        }

        @Override
        public String getSignature() {
            return this.mSignature;
        }

        @Override
        protected ImageView fillImageView(Context context, ImageView v, int thumbWidth, int thumbHeight, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgress) {
            if (thumbWidth > 0 && thumbHeight > 0) {
                Glide.with(context).loadFromMediaStore(getUri(), this.mMimeType, this.mDateModifiedInSeconds, 0).asBitmap().encoder((ResourceEncoder<Bitmap>) LocalMediaData.JPEG_ENCODER).thumbnail(Glide.with(context).loadFromMediaStore(getUri(), this.mMimeType, this.mDateModifiedInSeconds, 0).asBitmap().encoder((ResourceEncoder<Bitmap>) LocalMediaData.JPEG_ENCODER).override(512, LocalMediaData.MEDIASTORE_THUMB_HEIGHT)).placeholder(placeHolderResourceId).fitCenter().override(thumbWidth, thumbHeight).into(v);
            }
            return v;
        }

        @Override
        public View getView(Context context, View recycled, int thumbWidth, int thumbHeight, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgress, final LocalData.ActionCallback actionCallback) {
            View result;
            VideoViewHolder viewHolder;
            if (recycled != null) {
                result = recycled;
                viewHolder = (VideoViewHolder) recycled.getTag(R.id.mediadata_tag_target);
            } else {
                result = LayoutInflater.from(context).inflate(R.layout.filmstrip_video, (ViewGroup) null);
                result.setTag(R.id.mediadata_tag_viewtype, Integer.valueOf(getItemViewType().ordinal()));
                ImageView videoView = (ImageView) result.findViewById(R.id.video_view);
                ImageView playButton = (ImageView) result.findViewById(R.id.play_button);
                viewHolder = new VideoViewHolder(videoView, playButton);
                result.setTag(R.id.mediadata_tag_target, viewHolder);
            }
            fillImageView(context, viewHolder.mVideoView, thumbWidth, thumbHeight, placeHolderResourceId, adapter, isInProgress);
            viewHolder.mPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionCallback.playVideo(VideoData.this.getUri(), VideoData.this.mTitle);
                }
            });
            result.setContentDescription(context.getResources().getString(R.string.video_date_content_description, LocalMediaData.getReadableDate(this.mDateModifiedInSeconds)));
            return result;
        }

        @Override
        public void recycle(View view) {
            super.recycle(view);
            VideoViewHolder videoViewHolder = (VideoViewHolder) view.getTag(R.id.mediadata_tag_target);
            Glide.clear(videoViewHolder.mVideoView);
        }

        @Override
        public LocalDataViewType getItemViewType() {
            return LocalDataViewType.VIDEO;
        }
    }

    private static class VideoDataBuilder implements CursorToLocalData {
        private VideoDataBuilder() {
        }

        @Override
        public VideoData build(Cursor cursor) {
            return VideoData.buildFromCursor(cursor);
        }
    }

    private static class VideoViewHolder {
        private final ImageView mPlayButton;
        private final ImageView mVideoView;

        public VideoViewHolder(ImageView videoView, ImageView playButton) {
            this.mVideoView = videoView;
            this.mPlayButton = playButton;
        }
    }
}
