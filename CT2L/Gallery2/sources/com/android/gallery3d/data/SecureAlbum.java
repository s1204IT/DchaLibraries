package com.android.gallery3d.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.util.MediaSetUtils;
import java.util.ArrayList;

public class SecureAlbum extends MediaSet {
    private static final String[] PROJECTION = {"_id"};
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

    public SecureAlbum(Path path, GalleryApp application, MediaItem unlock) {
        super(path, nextVersionNumber());
        this.mMinImageId = Integer.MAX_VALUE;
        this.mMaxImageId = Integer.MIN_VALUE;
        this.mMinVideoId = Integer.MAX_VALUE;
        this.mMaxVideoId = Integer.MIN_VALUE;
        this.mAllItems = new ArrayList<>();
        this.mAllItemTypes = new ArrayList<>();
        this.mExistingItems = new ArrayList<>();
        this.mContext = application.getAndroidContext();
        this.mDataManager = application.getDataManager();
        this.mNotifier = new ChangeNotifier(this, mWatchUris, application);
        this.mUnlockItem = unlock;
        this.mShowUnlockItem = (isCameraBucketEmpty(MediaStore.Images.Media.EXTERNAL_CONTENT_URI) && isCameraBucketEmpty(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)) ? false : true;
    }

    @Override
    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        int existingCount = this.mExistingItems.size();
        if (start >= existingCount + 1) {
            return new ArrayList<>();
        }
        int end = Math.min(start + count, existingCount);
        ArrayList<Path> subset = new ArrayList<>(this.mExistingItems.subList(start, end));
        final MediaItem[] buf = new MediaItem[end - start];
        MediaSet.ItemConsumer consumer = new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                buf[index] = item;
            }
        };
        this.mDataManager.mapMediaItems(subset, consumer, 0);
        ArrayList<MediaItem> result = new ArrayList<>(end - start);
        for (MediaItem mediaItem : buf) {
            result.add(mediaItem);
        }
        if (this.mShowUnlockItem) {
            result.add(this.mUnlockItem);
            return result;
        }
        return result;
    }

    @Override
    public int getMediaItemCount() {
        return (this.mShowUnlockItem ? 1 : 0) + this.mExistingItems.size();
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

    private ArrayList<Integer> queryExistingIds(Uri uri, int minId, int maxId) {
        ArrayList<Integer> ids = new ArrayList<>();
        if (minId != Integer.MAX_VALUE && maxId != Integer.MIN_VALUE) {
            String[] selectionArgs = {String.valueOf(minId), String.valueOf(maxId)};
            Cursor cursor = this.mContext.getContentResolver().query(uri, PROJECTION, "_id BETWEEN ? AND ?", selectionArgs, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    try {
                        ids.add(Integer.valueOf(cursor.getInt(0)));
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
        return ids;
    }

    private boolean isCameraBucketEmpty(Uri baseUri) {
        Uri uri = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
        String[] selection = {String.valueOf(MediaSetUtils.CAMERA_BUCKET_ID)};
        Cursor cursor = this.mContext.getContentResolver().query(uri, PROJECTION, "bucket_id = ?", selection, null);
        if (cursor == null) {
            return true;
        }
        try {
            boolean z = cursor.getCount() == 0;
            cursor.close();
            return z;
        } catch (Throwable th) {
            cursor.close();
            throw th;
        }
    }

    private void updateExistingItems() {
        if (this.mAllItems.size() != 0) {
            ArrayList<Integer> imageIds = queryExistingIds(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, this.mMinImageId, this.mMaxImageId);
            ArrayList<Integer> videoIds = queryExistingIds(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, this.mMinVideoId, this.mMaxVideoId);
            this.mExistingItems.clear();
            for (int i = this.mAllItems.size() - 1; i >= 0; i--) {
                Path path = this.mAllItems.get(i);
                boolean isVideo = this.mAllItemTypes.get(i).booleanValue();
                int id = Integer.parseInt(path.getSuffix());
                if (isVideo) {
                    if (videoIds.contains(Integer.valueOf(id))) {
                        this.mExistingItems.add(path);
                    }
                } else if (imageIds.contains(Integer.valueOf(id))) {
                    this.mExistingItems.add(path);
                }
            }
        }
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }
}
