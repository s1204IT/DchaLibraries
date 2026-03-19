package com.android.gallery3d.data;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.base.MediaFilterSetting;
import com.mediatek.galleryportable.TraceHelper;
import java.io.File;
import java.util.ArrayList;

public class LocalAlbum extends MediaSet {
    private static final String[] COUNT_PROJECTION = {"count(*)"};
    private final GalleryApp mApplication;
    private final Uri mBaseUri;
    private final int mBucketId;
    private int mCachedCount;
    private String mDefaultWhereClause;
    private final boolean mIsImage;
    private final Path mItemPath;
    private final String mName;
    private final ChangeNotifier mNotifier;
    private final String mOrderClause;
    private final String[] mProjection;
    private final ContentResolver mResolver;
    private String mWhereClause;
    private String mWhereClauseForDelete;

    public LocalAlbum(Path path, GalleryApp galleryApp, int i, boolean z, String str) {
        super(path, nextVersionNumber());
        this.mCachedCount = -1;
        this.mApplication = galleryApp;
        this.mResolver = galleryApp.getContentResolver();
        this.mBucketId = i;
        if (isCameraRoll() && str.equals("")) {
            this.mName = galleryApp.getResources().getString(R.string.folder_camera);
            Log.d("Gallery2/LocalAlbum", "<LocalAlbum> mName = " + this.mName);
        } else {
            this.mName = str;
        }
        this.mIsImage = z;
        if (z) {
            this.mWhereClause = "bucket_id = ?";
            this.mOrderClause = "datetaken DESC, _id DESC";
            this.mBaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            this.mProjection = LocalImage.getProjection();
            this.mItemPath = LocalImage.ITEM_PATH;
        } else {
            this.mWhereClause = "bucket_id = ?";
            this.mOrderClause = "datetaken DESC, _id DESC";
            this.mBaseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            this.mProjection = LocalVideo.getProjection();
            this.mItemPath = LocalVideo.ITEM_PATH;
        }
        exInitializeWhereClause();
        this.mNotifier = new ChangeNotifier(this, this.mBaseUri, galleryApp);
    }

    public LocalAlbum(Path path, GalleryApp galleryApp, int i, boolean z) {
        this(path, galleryApp, i, z, BucketHelper.getBucketName(galleryApp.getContentResolver(), i));
    }

    @Override
    public boolean isCameraRoll() {
        return this.mBucketId == MediaSetUtils.CAMERA_BUCKET_ID;
    }

    @Override
    public Uri getContentUri() {
        if (this.mIsImage) {
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendQueryParameter("bucketId", String.valueOf(this.mBucketId)).build();
        }
        return MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon().appendQueryParameter("bucketId", String.valueOf(this.mBucketId)).build();
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int i, int i2) {
        DataManager dataManager = this.mApplication.getDataManager();
        Uri uriBuild = this.mBaseUri.buildUpon().appendQueryParameter("limit", i + "," + i2).build();
        ArrayList<MediaItem> arrayList = new ArrayList<>();
        GalleryUtils.assertNotInRenderThread();
        TraceHelper.beginSection(">>>>LocalAlbum-query");
        Cursor cursorQuery = this.mResolver.query(uriBuild, this.mProjection, this.mWhereClause, new String[]{String.valueOf(this.mBucketId)}, this.mOrderClause);
        TraceHelper.endSection();
        if (cursorQuery == null) {
            Log.w("Gallery2/LocalAlbum", "query fail: " + uriBuild);
            return arrayList;
        }
        while (cursorQuery.moveToNext()) {
            try {
                Path child = this.mItemPath.getChild(cursorQuery.getInt(0));
                TraceHelper.beginSection(">>>>LocalAlbum-loadOrUpdateItem");
                MediaItem mediaItemLoadOrUpdateItem = loadOrUpdateItem(child, cursorQuery, dataManager, this.mApplication, this.mIsImage);
                TraceHelper.endSection();
                arrayList.add(mediaItemLoadOrUpdateItem);
            } finally {
                cursorQuery.close();
            }
        }
        return arrayList;
    }

    private static MediaItem loadOrUpdateItem(Path path, Cursor cursor, DataManager dataManager, GalleryApp galleryApp, boolean z) {
        LocalMediaItem localVideo;
        synchronized (DataManager.LOCK) {
            TraceHelper.beginSection(">>>>LocalAlbum-loadOrUpdateItem-peekMediaObject");
            localVideo = (LocalMediaItem) dataManager.peekMediaObject(path);
            TraceHelper.endSection();
            if (localVideo == null) {
                if (z) {
                    TraceHelper.beginSection(">>>>LocalAlbum-loadOrUpdateItem-new LocalImage");
                    localVideo = new LocalImage(path, galleryApp, cursor);
                    TraceHelper.endSection();
                } else {
                    TraceHelper.beginSection(">>>>LocalAlbum-loadOrUpdateItem-new LocalVideo");
                    localVideo = new LocalVideo(path, galleryApp, cursor);
                    TraceHelper.endSection();
                }
            } else {
                TraceHelper.beginSection(">>>>LocalAlbum-loadOrUpdateItem-updateContent");
                localVideo.updateContent(cursor);
                TraceHelper.endSection();
            }
        }
        return localVideo;
    }

    public static MediaItem[] getMediaItemById(GalleryApp galleryApp, boolean z, ArrayList<Integer> arrayList) {
        Uri uri;
        String[] projection;
        Path path;
        MediaItem[] mediaItemArr = new MediaItem[arrayList.size()];
        if (arrayList.isEmpty()) {
            return mediaItemArr;
        }
        int iIntValue = arrayList.get(0).intValue();
        int iIntValue2 = arrayList.get(arrayList.size() - 1).intValue();
        if (z) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            projection = LocalImage.getProjection();
            path = LocalImage.ITEM_PATH;
        } else {
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            projection = LocalVideo.getProjection();
            path = LocalVideo.ITEM_PATH;
        }
        String[] strArr = projection;
        ContentResolver contentResolver = galleryApp.getContentResolver();
        DataManager dataManager = galleryApp.getDataManager();
        TraceHelper.beginSection(">>>>LocalAlbum-getMediaItemById-query");
        Cursor cursorQuery = contentResolver.query(uri, strArr, "_id BETWEEN ? AND ?", new String[]{String.valueOf(iIntValue), String.valueOf(iIntValue2)}, BookmarkEnhance.COLUMN_ID);
        TraceHelper.endSection();
        if (cursorQuery == null) {
            Log.w("Gallery2/LocalAlbum", "query fail" + uri);
            return mediaItemArr;
        }
        try {
            int size = arrayList.size();
            int i = 0;
            while (i < size) {
                if (!cursorQuery.moveToNext()) {
                    break;
                }
                int i2 = cursorQuery.getInt(0);
                if (arrayList.get(i).intValue() <= i2) {
                    while (arrayList.get(i).intValue() < i2) {
                        i++;
                        if (i >= size) {
                            return mediaItemArr;
                        }
                    }
                    mediaItemArr[i] = loadOrUpdateItem(path.getChild(i2), cursorQuery, dataManager, galleryApp, z);
                    i++;
                }
            }
            return mediaItemArr;
        } finally {
            cursorQuery.close();
        }
    }

    public static Cursor getItemCursor(ContentResolver contentResolver, Uri uri, String[] strArr, int i) {
        TraceHelper.beginSection(">>>>LocalAlbum-getItemCursor-query");
        Cursor cursorQuery = contentResolver.query(uri, strArr, "_id=?", new String[]{String.valueOf(i)}, null);
        TraceHelper.endSection();
        return cursorQuery;
    }

    @Override
    public int getMediaItemCount() {
        if (this.mCachedCount == -1) {
            TraceHelper.beginSection(">>>>LocalAlbum-getMediaItemCount-query");
            try {
                Cursor cursorQuery = this.mResolver.query(this.mBaseUri, COUNT_PROJECTION, this.mWhereClause, new String[]{String.valueOf(this.mBucketId)}, null);
                TraceHelper.endSection();
                if (cursorQuery == null) {
                    Log.w("Gallery2/LocalAlbum", "query fail");
                    return 0;
                }
                try {
                    Utils.assertTrue(cursorQuery.moveToNext());
                    this.mCachedCount = cursorQuery.getInt(0);
                } finally {
                    cursorQuery.close();
                }
            } catch (SQLiteException e) {
                Log.w("Gallery2/LocalAlbum", "<getMediaItemCount> query SQLiteException:" + e.getMessage());
                return 0;
            } catch (IllegalStateException e2) {
                Log.w("Gallery2/LocalAlbum", "<getMediaItemCount> query IllegalStateException:" + e2.getMessage());
                return 0;
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
            reloadWhereClause();
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
        this.mResolver.delete(this.mBaseUri, this.mWhereClauseForDelete, new String[]{String.valueOf(this.mBucketId)});
        this.mApplication.getDataManager().broadcastUpdatePicture();
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    public static String getLocalizedName(Resources resources, int i, String str) {
        if (i == MediaSetUtils.CAMERA_BUCKET_ID) {
            return resources.getString(R.string.folder_camera);
        }
        if (i == MediaSetUtils.DOWNLOAD_BUCKET_ID) {
            return resources.getString(R.string.folder_download);
        }
        if (i == MediaSetUtils.IMPORTED_BUCKET_ID) {
            return resources.getString(R.string.folder_imported);
        }
        if (i == MediaSetUtils.SNAPSHOT_BUCKET_ID) {
            return resources.getString(R.string.folder_screenshot);
        }
        if (i == MediaSetUtils.EDITED_ONLINE_PHOTOS_BUCKET_ID) {
            return resources.getString(R.string.folder_edited_online_photos);
        }
        return str;
    }

    public String getRelativePath() {
        String strSubstring = null;
        if (this.mBucketId == MediaSetUtils.CAMERA_BUCKET_ID) {
            strSubstring = "/DCIM/Camera";
        } else if (this.mBucketId == MediaSetUtils.DOWNLOAD_BUCKET_ID) {
            strSubstring = "/download";
        } else if (this.mBucketId == MediaSetUtils.IMPORTED_BUCKET_ID) {
            strSubstring = "/Imported";
        } else if (this.mBucketId == MediaSetUtils.SNAPSHOT_BUCKET_ID) {
            strSubstring = "/Pictures/Screenshots";
        } else if (this.mBucketId == MediaSetUtils.EDITED_ONLINE_PHOTOS_BUCKET_ID) {
            strSubstring = "/EditedOnlinePhotos";
        } else if (this.mBucketId == MediaSetUtils.STEREO_CLIPPINGS_BUCKET_ID) {
            strSubstring = "/Pictures/Clippings";
        } else {
            MediaItem coverMediaItem = getCoverMediaItem();
            File externalStorageDirectory = Environment.getExternalStorageDirectory();
            if (coverMediaItem != null) {
                String absolutePath = externalStorageDirectory.getAbsolutePath();
                String filePath = coverMediaItem.getFilePath();
                Log.d("Gallery2/LocalAlbum", "<getRelativePath> Absolute path of this alum cover is " + filePath);
                if (filePath != null && absolutePath != null && !absolutePath.equals("") && filePath.startsWith(absolutePath)) {
                    String strSubstring2 = filePath.substring(absolutePath.length());
                    strSubstring = strSubstring2.substring(0, strSubstring2.lastIndexOf("/"));
                    Log.d("Gallery2/LocalAlbum", "<getRelativePath> 1.RelativePath for bucket id: " + this.mBucketId + " is " + strSubstring);
                }
            } else {
                String strSearchDirForPath = GalleryUtils.searchDirForPath(externalStorageDirectory, this.mBucketId);
                if (strSearchDirForPath == null) {
                    Log.w("Gallery2/LocalAlbum", "<getRelativePath> 2.Relative path for bucket id: " + this.mBucketId + " is not found.");
                } else {
                    strSubstring = strSearchDirForPath.substring(externalStorageDirectory.getAbsolutePath().length());
                    Log.d("Gallery2/LocalAlbum", "<getRelativePath> 3.RelativePath for bucket id: " + this.mBucketId + " is " + strSubstring);
                }
            }
        }
        Log.d("Gallery2/LocalAlbum", "<getRelativePath> return " + strSubstring);
        return strSubstring;
    }

    private void exInitializeWhereClause() {
        this.mDefaultWhereClause = this.mWhereClause;
        reloadWhereClause();
    }

    private void reloadWhereClause() {
        if (this.mIsImage) {
            this.mWhereClauseForDelete = MediaFilterSetting.getExtDeleteWhereClauseForImage(this.mDefaultWhereClause, this.mBucketId);
            this.mWhereClause = MediaFilterSetting.getExtWhereClauseForImage(this.mDefaultWhereClause, this.mBucketId);
        } else {
            this.mWhereClauseForDelete = MediaFilterSetting.getExtDeleteWhereClauseForVideo(this.mDefaultWhereClause, this.mBucketId);
            this.mWhereClause = MediaFilterSetting.getExtWhereClauseForVideo(this.mDefaultWhereClause, this.mBucketId);
        }
    }
}
