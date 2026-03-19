package com.android.gallery3d.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.util.MediaSetUtils;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.base.MediaFilterSetting;
import java.util.ArrayList;
import mf.org.apache.xerces.dom3.as.ASContentModel;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class SecureAlbum extends MediaSet {
    private static final String[] PROJECTION = {BookmarkEnhance.COLUMN_ID};
    private static final Uri[] mWatchUris = {MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI};
    private ArrayList<Boolean> mAllItemTypes;
    private ArrayList<Path> mAllItems;
    private Context mContext;
    private DataManager mDataManager;
    private ArrayList<Path> mExistingItems;
    private int mMaxImageId;
    private int mMaxVideoId;
    private int mMinImageId;
    private int mMinVideoId;
    private final ChangeNotifier mNotifier;
    private boolean mShowUnlockItem;
    private MediaItem mUnlockItem;

    public SecureAlbum(Path path, GalleryApp galleryApp, MediaItem mediaItem) {
        super(path, nextVersionNumber());
        this.mMinImageId = ASContentModel.AS_UNBOUNDED;
        this.mMaxImageId = Integer.MIN_VALUE;
        this.mMinVideoId = ASContentModel.AS_UNBOUNDED;
        this.mMaxVideoId = Integer.MIN_VALUE;
        this.mAllItems = new ArrayList<>();
        this.mAllItemTypes = new ArrayList<>();
        this.mExistingItems = new ArrayList<>();
        this.mContext = galleryApp.getAndroidContext();
        this.mDataManager = galleryApp.getDataManager();
        this.mNotifier = new ChangeNotifier(this, mWatchUris, galleryApp);
        this.mUnlockItem = mediaItem;
        this.mShowUnlockItem = (isCameraBucketEmpty(MediaStore.Images.Media.EXTERNAL_CONTENT_URI) && isCameraBucketEmpty(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)) ? false : true;
    }

    public void addMediaItem(boolean z, int i) {
        Path path;
        if (z) {
            path = LocalVideo.ITEM_PATH;
            this.mMinVideoId = Math.min(this.mMinVideoId, i);
            this.mMaxVideoId = Math.max(this.mMaxVideoId, i);
        } else {
            path = LocalImage.ITEM_PATH;
            this.mMinImageId = Math.min(this.mMinImageId, i);
            this.mMaxImageId = Math.max(this.mMaxImageId, i);
        }
        Path child = path.getChild(i);
        if (!this.mAllItems.contains(child)) {
            this.mAllItems.add(child);
            this.mAllItemTypes.add(Boolean.valueOf(z));
            this.mNotifier.fakeChange();
        }
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int i, int i2) {
        int size = this.mExistingItems.size();
        if (i >= size + 1) {
            return new ArrayList<>();
        }
        int iMin = Math.min(i2 + i, size);
        ArrayList<Path> arrayList = new ArrayList<>(this.mExistingItems.subList(i, iMin));
        int i3 = iMin - i;
        final MediaItem[] mediaItemArr = new MediaItem[i3];
        this.mDataManager.mapMediaItems(arrayList, new MediaSet.ItemConsumer() {
            @Override
            public void consume(int i4, MediaItem mediaItem) {
                mediaItemArr[i4] = mediaItem;
            }

            @Override
            public boolean stopConsume() {
                return false;
            }
        }, 0);
        ArrayList<MediaItem> arrayList2 = new ArrayList<>(i3);
        for (MediaItem mediaItem : mediaItemArr) {
            arrayList2.add(mediaItem);
        }
        if (this.mShowUnlockItem) {
            arrayList2.add(this.mUnlockItem);
        }
        return arrayList2;
    }

    @Override
    public int getMediaItemCount() {
        return this.mExistingItems.size() + (this.mShowUnlockItem ? 1 : 0);
    }

    @Override
    public String getName() {
        return "secure";
    }

    @Override
    public long reload() {
        if (this.mNotifier.isDirty()) {
            this.mDataVersion = nextVersionNumber();
            updateExistingItems();
        }
        return this.mDataVersion;
    }

    private ArrayList<Integer> queryExistingIds(Uri uri, int i, int i2) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        if (i == Integer.MAX_VALUE || i2 == Integer.MIN_VALUE) {
            return arrayList;
        }
        String[] strArr = {String.valueOf(i), String.valueOf(i2)};
        String extWhereClauseForVideo = "_id BETWEEN ? AND ?";
        if (MediaStore.Images.Media.EXTERNAL_CONTENT_URI.equals(uri)) {
            extWhereClauseForVideo = MediaFilterSetting.getExtWhereClauseForImage("_id BETWEEN ? AND ?", MediaSetUtils.CAMERA_BUCKET_ID);
        } else if (MediaStore.Video.Media.EXTERNAL_CONTENT_URI.equals(uri)) {
            extWhereClauseForVideo = MediaFilterSetting.getExtWhereClauseForVideo("_id BETWEEN ? AND ?", MediaSetUtils.CAMERA_BUCKET_ID);
        }
        Cursor cursorQuery = this.mContext.getContentResolver().query(uri, PROJECTION, extWhereClauseForVideo, strArr, null);
        if (cursorQuery == null) {
            return arrayList;
        }
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add(Integer.valueOf(cursorQuery.getInt(0)));
            } finally {
                cursorQuery.close();
            }
        }
        return arrayList;
    }

    private boolean isCameraBucketEmpty(Uri uri) {
        Uri uriBuild = uri.buildUpon().appendQueryParameter("limit", SchemaSymbols.ATTVAL_TRUE_1).build();
        boolean z = true;
        Cursor cursorQuery = this.mContext.getContentResolver().query(uriBuild, PROJECTION, "bucket_id = ?", new String[]{String.valueOf(MediaSetUtils.CAMERA_BUCKET_ID)}, null);
        if (cursorQuery == null) {
            return true;
        }
        try {
            if (cursorQuery.getCount() != 0) {
                z = false;
            }
            return z;
        } finally {
            cursorQuery.close();
        }
    }

    private void updateExistingItems() {
        if (this.mAllItems.size() == 0) {
            return;
        }
        ArrayList<Integer> arrayListQueryExistingIds = queryExistingIds(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, this.mMinImageId, this.mMaxImageId);
        ArrayList<Integer> arrayListQueryExistingIds2 = queryExistingIds(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, this.mMinVideoId, this.mMaxVideoId);
        this.mExistingItems.clear();
        for (int size = this.mAllItems.size() - 1; size >= 0; size--) {
            Path path = this.mAllItems.get(size);
            boolean zBooleanValue = this.mAllItemTypes.get(size).booleanValue();
            int i = Integer.parseInt(path.getSuffix());
            if (zBooleanValue) {
                if (arrayListQueryExistingIds2.contains(Integer.valueOf(i))) {
                    this.mExistingItems.add(path);
                }
            } else if (arrayListQueryExistingIds.contains(Integer.valueOf(i))) {
                this.mExistingItems.add(path);
            }
        }
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    public void clearAll() {
        this.mAllItems.clear();
        this.mAllItemTypes.clear();
        this.mMinVideoId = ASContentModel.AS_UNBOUNDED;
        this.mMaxVideoId = Integer.MIN_VALUE;
        this.mMinImageId = ASContentModel.AS_UNBOUNDED;
        this.mMaxImageId = Integer.MIN_VALUE;
    }
}
