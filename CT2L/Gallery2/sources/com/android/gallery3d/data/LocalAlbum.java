package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;
import java.io.File;
import java.util.ArrayList;

public class LocalAlbum extends MediaSet {
    private static final String[] COUNT_PROJECTION = {"count(*)"};
    private final GalleryApp mApplication;
    private final Uri mBaseUri;
    private final int mBucketId;
    private int mCachedCount;
    private final boolean mIsImage;
    private final Path mItemPath;
    private final String mName;
    private final ChangeNotifier mNotifier;
    private final String mOrderClause;
    private final String[] mProjection;
    private final ContentResolver mResolver;
    private final String mWhereClause;

    public LocalAlbum(Path path, GalleryApp application, int bucketId, boolean isImage, String name) {
        super(path, nextVersionNumber());
        this.mCachedCount = -1;
        this.mApplication = application;
        this.mResolver = application.getContentResolver();
        this.mBucketId = bucketId;
        this.mName = name;
        this.mIsImage = isImage;
        if (isImage) {
            this.mWhereClause = "bucket_id = ?";
            this.mOrderClause = "datetaken DESC, _id DESC";
            this.mBaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            this.mProjection = LocalImage.PROJECTION;
            this.mItemPath = LocalImage.ITEM_PATH;
        } else {
            this.mWhereClause = "bucket_id = ?";
            this.mOrderClause = "datetaken DESC, _id DESC";
            this.mBaseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            this.mProjection = LocalVideo.PROJECTION;
            this.mItemPath = LocalVideo.ITEM_PATH;
        }
        this.mNotifier = new ChangeNotifier(this, this.mBaseUri, application);
    }

    public LocalAlbum(Path path, GalleryApp application, int bucketId, boolean isImage) {
        this(path, application, bucketId, isImage, BucketHelper.getBucketName(application.getContentResolver(), bucketId));
    }

    @Override
    public boolean isCameraRoll() {
        return this.mBucketId == MediaSetUtils.CAMERA_BUCKET_ID;
    }

    @Override
    public Uri getContentUri() {
        return this.mIsImage ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendQueryParameter("bucketId", String.valueOf(this.mBucketId)).build() : MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon().appendQueryParameter("bucketId", String.valueOf(this.mBucketId)).build();
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        DataManager dataManager = this.mApplication.getDataManager();
        Uri uri = this.mBaseUri.buildUpon().appendQueryParameter("limit", start + "," + count).build();
        ArrayList<MediaItem> list = new ArrayList<>();
        GalleryUtils.assertNotInRenderThread();
        Cursor cursor = this.mResolver.query(uri, this.mProjection, this.mWhereClause, new String[]{String.valueOf(this.mBucketId)}, this.mOrderClause);
        if (cursor == null) {
            Log.w("LocalAlbum", "query fail: " + uri);
        } else {
            while (cursor.moveToNext()) {
                try {
                    int id = cursor.getInt(0);
                    Path childPath = this.mItemPath.getChild(id);
                    MediaItem item = loadOrUpdateItem(childPath, cursor, dataManager, this.mApplication, this.mIsImage);
                    list.add(item);
                } finally {
                    cursor.close();
                }
            }
        }
        return list;
    }

    private static MediaItem loadOrUpdateItem(Path path, Cursor cursor, DataManager dataManager, GalleryApp app, boolean isImage) {
        LocalMediaItem item;
        synchronized (DataManager.LOCK) {
            item = (LocalMediaItem) dataManager.peekMediaObject(path);
            if (item == null) {
                if (isImage) {
                    item = new LocalImage(path, app, cursor);
                } else {
                    item = new LocalVideo(path, app, cursor);
                }
            } else {
                item.updateContent(cursor);
            }
        }
        return item;
    }

    public static MediaItem[] getMediaItemById(GalleryApp application, boolean isImage, ArrayList<Integer> ids) {
        Uri baseUri;
        String[] projection;
        Path itemPath;
        MediaItem[] result = new MediaItem[ids.size()];
        if (!ids.isEmpty()) {
            int idLow = ids.get(0).intValue();
            int idHigh = ids.get(ids.size() - 1).intValue();
            if (isImage) {
                baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                projection = LocalImage.PROJECTION;
                itemPath = LocalImage.ITEM_PATH;
            } else {
                baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                projection = LocalVideo.PROJECTION;
                itemPath = LocalVideo.ITEM_PATH;
            }
            ContentResolver resolver = application.getContentResolver();
            DataManager dataManager = application.getDataManager();
            Cursor cursor = resolver.query(baseUri, projection, "_id BETWEEN ? AND ?", new String[]{String.valueOf(idLow), String.valueOf(idHigh)}, "_id");
            if (cursor == null) {
                Log.w("LocalAlbum", "query fail" + baseUri);
            } else {
                try {
                    int n = ids.size();
                    int i = 0;
                    loop0: while (i < n) {
                        if (!cursor.moveToNext()) {
                            break;
                        }
                        int id = cursor.getInt(0);
                        if (ids.get(i).intValue() <= id) {
                            while (ids.get(i).intValue() < id) {
                                i++;
                                if (i >= n) {
                                    break loop0;
                                }
                            }
                            Path childPath = itemPath.getChild(id);
                            MediaItem item = loadOrUpdateItem(childPath, cursor, dataManager, application, isImage);
                            result[i] = item;
                            i++;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return result;
    }

    public static Cursor getItemCursor(ContentResolver resolver, Uri uri, String[] projection, int id) {
        return resolver.query(uri, projection, "_id=?", new String[]{String.valueOf(id)}, null);
    }

    @Override
    public int getMediaItemCount() {
        if (this.mCachedCount == -1) {
            Cursor cursor = this.mResolver.query(this.mBaseUri, COUNT_PROJECTION, this.mWhereClause, new String[]{String.valueOf(this.mBucketId)}, null);
            if (cursor == null) {
                Log.w("LocalAlbum", "query fail");
                return 0;
            }
            try {
                Utils.assertTrue(cursor.moveToNext());
                this.mCachedCount = cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }
        return this.mCachedCount;
    }

    @Override
    public String getName() {
        return getLocalizedName(this.mApplication.getResources(), this.mBucketId, this.mName);
    }

    @Override
    public long reload() {
        if (this.mNotifier.isDirty()) {
            this.mDataVersion = nextVersionNumber();
            this.mCachedCount = -1;
        }
        return this.mDataVersion;
    }

    @Override
    public int getSupportedOperations() {
        return 1029;
    }

    @Override
    public void delete() {
        GalleryUtils.assertNotInRenderThread();
        this.mResolver.delete(this.mBaseUri, this.mWhereClause, new String[]{String.valueOf(this.mBucketId)});
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    public static String getLocalizedName(Resources res, int bucketId, String name) {
        if (bucketId == MediaSetUtils.CAMERA_BUCKET_ID) {
            String name2 = res.getString(R.string.folder_camera);
            return name2;
        }
        if (bucketId == MediaSetUtils.DOWNLOAD_BUCKET_ID) {
            String name3 = res.getString(R.string.folder_download);
            return name3;
        }
        if (bucketId == MediaSetUtils.IMPORTED_BUCKET_ID) {
            String name4 = res.getString(R.string.folder_imported);
            return name4;
        }
        if (bucketId == MediaSetUtils.SNAPSHOT_BUCKET_ID) {
            String name5 = res.getString(R.string.folder_screenshot);
            return name5;
        }
        if (bucketId == MediaSetUtils.EDITED_ONLINE_PHOTOS_BUCKET_ID) {
            String name6 = res.getString(R.string.folder_edited_online_photos);
            return name6;
        }
        return name;
    }

    public static String getRelativePath(int bucketId) {
        if (bucketId == MediaSetUtils.CAMERA_BUCKET_ID) {
            String relativePath = "/DCIM/Camera";
            return relativePath;
        }
        if (bucketId == MediaSetUtils.DOWNLOAD_BUCKET_ID) {
            String relativePath2 = "/download";
            return relativePath2;
        }
        if (bucketId == MediaSetUtils.IMPORTED_BUCKET_ID) {
            String relativePath3 = "/Imported";
            return relativePath3;
        }
        if (bucketId == MediaSetUtils.SNAPSHOT_BUCKET_ID) {
            String relativePath4 = "/Pictures/Screenshots";
            return relativePath4;
        }
        if (bucketId == MediaSetUtils.EDITED_ONLINE_PHOTOS_BUCKET_ID) {
            String relativePath5 = "/EditedOnlinePhotos";
            return relativePath5;
        }
        File extStorage = Environment.getExternalStorageDirectory();
        String path = GalleryUtils.searchDirForPath(extStorage, bucketId);
        if (path == null) {
            Log.w("LocalAlbum", "Relative path for bucket id: " + bucketId + " is not found.");
            return null;
        }
        String relativePath6 = path.substring(extStorage.getAbsolutePath().length());
        return relativePath6;
    }
}
