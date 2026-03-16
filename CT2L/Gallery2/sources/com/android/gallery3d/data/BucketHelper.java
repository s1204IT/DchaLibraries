package com.android.gallery3d.data;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.ThreadPool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

class BucketHelper {
    private static final String[] PROJECTION_BUCKET = {"bucket_id", "media_type", "bucket_display_name"};
    private static final String[] PROJECTION_BUCKET_IN_ONE_TABLE = {"bucket_id", "MAX(datetaken)", "bucket_display_name"};

    public static BucketEntry[] loadBucketEntries(ThreadPool.JobContext jc, ContentResolver resolver, int type) {
        return ApiHelper.HAS_MEDIA_PROVIDER_FILES_TABLE ? loadBucketEntriesFromFilesTable(jc, resolver, type) : loadBucketEntriesFromImagesAndVideoTable(jc, resolver, type);
    }

    private static void updateBucketEntriesFromTable(ThreadPool.JobContext jc, ContentResolver resolver, Uri tableUri, HashMap<Integer, BucketEntry> buckets) {
        Cursor cursor = resolver.query(tableUri, PROJECTION_BUCKET_IN_ONE_TABLE, "1) GROUP BY (1", null, null);
        if (cursor == null) {
            android.util.Log.w("BucketHelper", "cannot open media database: " + tableUri);
            return;
        }
        while (cursor.moveToNext()) {
            try {
                int bucketId = cursor.getInt(0);
                int dateTaken = cursor.getInt(1);
                BucketEntry entry = buckets.get(Integer.valueOf(bucketId));
                if (entry == null) {
                    BucketEntry entry2 = new BucketEntry(bucketId, cursor.getString(2));
                    buckets.put(Integer.valueOf(bucketId), entry2);
                    entry2.dateTaken = dateTaken;
                } else {
                    entry.dateTaken = Math.max(entry.dateTaken, dateTaken);
                }
            } finally {
                Utils.closeSilently(cursor);
            }
        }
    }

    private static BucketEntry[] loadBucketEntriesFromImagesAndVideoTable(ThreadPool.JobContext jc, ContentResolver resolver, int type) {
        HashMap<Integer, BucketEntry> buckets = new HashMap<>(64);
        if ((type & 2) != 0) {
            updateBucketEntriesFromTable(jc, resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, buckets);
        }
        if ((type & 4) != 0) {
            updateBucketEntriesFromTable(jc, resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, buckets);
        }
        BucketEntry[] entries = (BucketEntry[]) buckets.values().toArray(new BucketEntry[buckets.size()]);
        Arrays.sort(entries, new Comparator<BucketEntry>() {
            @Override
            public int compare(BucketEntry a, BucketEntry b) {
                return b.dateTaken - a.dateTaken;
            }
        });
        return entries;
    }

    private static BucketEntry[] loadBucketEntriesFromFilesTable(ThreadPool.JobContext jc, ContentResolver resolver, int type) {
        Uri uri = getFilesContentUri();
        Cursor cursor = resolver.query(uri, PROJECTION_BUCKET, "1) GROUP BY 1,(2", null, "MAX(datetaken) DESC");
        if (cursor == null) {
            android.util.Log.w("BucketHelper", "cannot open local database: " + uri);
            return new BucketEntry[0];
        }
        ArrayList<BucketEntry> buffer = new ArrayList<>();
        int typeBits = 0;
        if ((type & 2) != 0) {
            typeBits = 0 | 2;
        }
        if ((type & 4) != 0) {
            typeBits |= 8;
        }
        do {
            try {
                if (cursor.moveToNext()) {
                    if (((1 << cursor.getInt(1)) & typeBits) != 0) {
                        BucketEntry entry = new BucketEntry(cursor.getInt(0), cursor.getString(2));
                        if (!buffer.contains(entry)) {
                            buffer.add(entry);
                        }
                    }
                } else {
                    Utils.closeSilently(cursor);
                    return (BucketEntry[]) buffer.toArray(new BucketEntry[buffer.size()]);
                }
            } finally {
                Utils.closeSilently(cursor);
            }
        } while (!jc.isCancelled());
        return null;
    }

    private static String getBucketNameInTable(ContentResolver resolver, Uri tableUri, int bucketId) {
        String string = null;
        String[] selectionArgs = {String.valueOf(bucketId)};
        Uri uri = tableUri.buildUpon().appendQueryParameter("limit", "1").build();
        Cursor cursor = resolver.query(uri, PROJECTION_BUCKET_IN_ONE_TABLE, "bucket_id = ?", selectionArgs, null);
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    string = cursor.getString(2);
                }
            } finally {
                Utils.closeSilently(cursor);
            }
        }
        return string;
    }

    @TargetApi(11)
    private static Uri getFilesContentUri() {
        return MediaStore.Files.getContentUri("external");
    }

    public static String getBucketName(ContentResolver resolver, int bucketId) {
        if (ApiHelper.HAS_MEDIA_PROVIDER_FILES_TABLE) {
            String result = getBucketNameInTable(resolver, getFilesContentUri(), bucketId);
            return result == null ? "" : result;
        }
        String result2 = getBucketNameInTable(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, bucketId);
        if (result2 != null) {
            return result2;
        }
        String result3 = getBucketNameInTable(resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, bucketId);
        return result3 == null ? "" : result3;
    }

    public static class BucketEntry {
        public int bucketId;
        public String bucketName;
        public int dateTaken;

        public BucketEntry(int id, String name) {
            this.bucketId = id;
            this.bucketName = Utils.ensureNotNull(name);
        }

        public int hashCode() {
            return this.bucketId;
        }

        public boolean equals(Object object) {
            if (!(object instanceof BucketEntry)) {
                return false;
            }
            BucketEntry entry = (BucketEntry) object;
            return this.bucketId == entry.bucketId;
        }
    }
}
