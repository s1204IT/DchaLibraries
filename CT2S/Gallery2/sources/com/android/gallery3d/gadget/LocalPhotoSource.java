package com.android.gallery3d.gadget;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ContentListener;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.GalleryUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

public class LocalPhotoSource implements WidgetSource {
    private ContentListener mContentListener;
    private Context mContext;
    private DataManager mDataManager;
    private static final Uri CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private static final String[] PROJECTION = {"_id"};
    private static final String[] COUNT_PROJECTION = {"count(*)"};
    private static final String SELECTION = String.format("%s != %s", "bucket_id", Integer.valueOf(getDownloadBucketId()));
    private static final String ORDER = String.format("%s DESC", "datetaken");
    private static final Path LOCAL_IMAGE_ROOT = Path.fromString("/local/image/item");
    private ArrayList<Long> mPhotos = new ArrayList<>();
    private boolean mContentDirty = true;
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            LocalPhotoSource.this.mContentDirty = true;
            if (LocalPhotoSource.this.mContentListener != null) {
                LocalPhotoSource.this.mContentListener.onContentDirty();
            }
        }
    };

    public LocalPhotoSource(Context context) {
        this.mContext = context;
        this.mDataManager = ((GalleryApp) context.getApplicationContext()).getDataManager();
        this.mContext.getContentResolver().registerContentObserver(CONTENT_URI, true, this.mContentObserver);
    }

    @Override
    public void close() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
    }

    @Override
    public Uri getContentUri(int index) {
        if (index < this.mPhotos.size()) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(this.mPhotos.get(index))).build();
        }
        return null;
    }

    @Override
    public Bitmap getImage(int index) {
        if (index >= this.mPhotos.size()) {
            return null;
        }
        long id = this.mPhotos.get(index).longValue();
        MediaItem image = (MediaItem) this.mDataManager.getMediaObject(LOCAL_IMAGE_ROOT.getChild(id));
        if (image == null) {
            return null;
        }
        return WidgetUtils.createWidgetBitmap(image);
    }

    private int[] getExponentialIndice(int total, int count) {
        Random random = new Random();
        if (count > total) {
            count = total;
        }
        HashSet<Integer> selected = new HashSet<>(count);
        while (selected.size() < count) {
            int row = (int) (((-Math.log(random.nextDouble())) * ((double) total)) / 2.0d);
            if (row < total) {
                selected.add(Integer.valueOf(row));
            }
        }
        int[] values = new int[count];
        int index = 0;
        Iterator<Integer> it = selected.iterator();
        while (it.hasNext()) {
            int value = it.next().intValue();
            values[index] = value;
            index++;
        }
        return values;
    }

    private int getPhotoCount(ContentResolver resolver) {
        Cursor cursor = resolver.query(CONTENT_URI, COUNT_PROJECTION, SELECTION, null, null);
        if (cursor == null) {
            return 0;
        }
        try {
            Utils.assertTrue(cursor.moveToNext());
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }

    private boolean isContentSound(int totalCount) {
        if (this.mPhotos.size() < Math.min(totalCount, 128)) {
            return false;
        }
        if (this.mPhotos.size() == 0) {
            return true;
        }
        StringBuilder builder = new StringBuilder();
        for (Long imageId : this.mPhotos) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(imageId);
        }
        Cursor cursor = this.mContext.getContentResolver().query(CONTENT_URI, COUNT_PROJECTION, String.format("%s in (%s)", "_id", builder.toString()), null, null);
        if (cursor == null) {
            return false;
        }
        try {
            Utils.assertTrue(cursor.moveToNext());
            boolean z = cursor.getInt(0) == this.mPhotos.size();
            cursor.close();
            return z;
        } catch (Throwable th) {
            cursor.close();
            throw th;
        }
    }

    @Override
    public void reload() {
        if (this.mContentDirty) {
            this.mContentDirty = false;
            ContentResolver resolver = this.mContext.getContentResolver();
            int photoCount = getPhotoCount(resolver);
            if (!isContentSound(photoCount)) {
                int[] choosedIds = getExponentialIndice(photoCount, 128);
                Arrays.sort(choosedIds);
                this.mPhotos.clear();
                Cursor cursor = this.mContext.getContentResolver().query(CONTENT_URI, PROJECTION, SELECTION, null, ORDER);
                if (cursor != null) {
                    try {
                        for (int index : choosedIds) {
                            if (cursor.moveToPosition(index)) {
                                this.mPhotos.add(Long.valueOf(cursor.getLong(0)));
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
    }

    @Override
    public int size() {
        reload();
        return this.mPhotos.size();
    }

    private static int getDownloadBucketId() {
        String downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        return GalleryUtils.getBucketId(downloadsPath);
    }

    @Override
    public void setContentListener(ContentListener listener) {
        this.mContentListener = listener;
    }
}
