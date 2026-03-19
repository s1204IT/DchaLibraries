package com.android.gallery3d.data;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallerybasic.base.MediaFilterSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

class BucketHelper {
    private static final String[] PROJECTION_BUCKET = {"bucket_id", "media_type", "bucket_display_name"};
    private static final String[] PROJECTION_BUCKET_IN_ONE_TABLE = {"bucket_id", "MAX(datetaken)", "bucket_display_name"};

    public static BucketEntry[] loadBucketEntries(ThreadPool.JobContext jobContext, ContentResolver contentResolver, int i) {
        if (ApiHelper.HAS_MEDIA_PROVIDER_FILES_TABLE) {
            return loadBucketEntriesFromFilesTable(jobContext, contentResolver, i);
        }
        return loadBucketEntriesFromImagesAndVideoTable(jobContext, contentResolver, i);
    }

    private static void updateBucketEntriesFromTable(ThreadPool.JobContext jobContext, ContentResolver contentResolver, Uri uri, HashMap<Integer, BucketEntry> map) {
        String str;
        String extWhereClause = MediaFilterSetting.getExtWhereClause(null);
        if (extWhereClause == null || extWhereClause.equals("")) {
            str = ") GROUP BY (1";
        } else {
            str = "(" + extWhereClause + ")) GROUP BY (1";
        }
        Cursor cursorQuery = contentResolver.query(uri, PROJECTION_BUCKET_IN_ONE_TABLE, str, null, null);
        if (cursorQuery == null) {
            com.mediatek.gallery3d.util.Log.w("Gallery2/BucketHelper", "cannot open media database: " + uri);
            return;
        }
        while (cursorQuery.moveToNext()) {
            try {
                int i = cursorQuery.getInt(0);
                int i2 = cursorQuery.getInt(1);
                BucketEntry bucketEntry = map.get(Integer.valueOf(i));
                if (bucketEntry == null) {
                    BucketEntry bucketEntry2 = new BucketEntry(i, cursorQuery.getString(2));
                    map.put(Integer.valueOf(i), bucketEntry2);
                    bucketEntry2.dateTaken = i2;
                } else {
                    bucketEntry.dateTaken = Math.max(bucketEntry.dateTaken, i2);
                }
            } finally {
                Utils.closeSilently(cursorQuery);
            }
        }
    }

    private static BucketEntry[] loadBucketEntriesFromImagesAndVideoTable(ThreadPool.JobContext jobContext, ContentResolver contentResolver, int i) {
        HashMap map = new HashMap(64);
        if ((i & 2) != 0) {
            updateBucketEntriesFromTable(jobContext, contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, map);
        }
        if ((i & 4) != 0) {
            updateBucketEntriesFromTable(jobContext, contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, map);
        }
        BucketEntry[] bucketEntryArr = (BucketEntry[]) map.values().toArray(new BucketEntry[map.size()]);
        Arrays.sort(bucketEntryArr, new Comparator<BucketEntry>() {
            @Override
            public int compare(BucketEntry bucketEntry, BucketEntry bucketEntry2) {
                return bucketEntry2.dateTaken - bucketEntry.dateTaken;
            }
        });
        return bucketEntryArr;
    }

    private static BucketEntry[] loadBucketEntriesFromFilesTable(ThreadPool.JobContext jobContext, ContentResolver contentResolver, int i) {
        String str;
        int i2;
        Uri filesContentUri = getFilesContentUri();
        String extWhereClause = MediaFilterSetting.getExtWhereClause(null);
        if (extWhereClause == null || extWhereClause.equals("")) {
            str = "media_type=1 OR media_type=3) GROUP BY 1,(2";
        } else {
            str = "(media_type=1 OR media_type=3) AND (" + extWhereClause + ")) GROUP BY 1,(2";
        }
        Cursor cursorQuery = contentResolver.query(filesContentUri, PROJECTION_BUCKET, str, null, "MAX(datetaken) DESC");
        if (cursorQuery == null) {
            com.mediatek.gallery3d.util.Log.w("Gallery2/BucketHelper", "cannot open local database: " + filesContentUri);
            return new BucketEntry[0];
        }
        ArrayList arrayList = new ArrayList();
        if ((i & 2) == 0) {
            i2 = 0;
        } else {
            i2 = 2;
        }
        if ((i & 4) != 0) {
            i2 |= 8;
        }
        while (cursorQuery.moveToNext()) {
            try {
                if (((1 << cursorQuery.getInt(1)) & i2) != 0) {
                    BucketEntry bucketEntry = new BucketEntry(cursorQuery.getInt(0), cursorQuery.getString(2));
                    if (!arrayList.contains(bucketEntry)) {
                        arrayList.add(bucketEntry);
                    }
                }
                if (jobContext.isCancelled()) {
                    return null;
                }
            } finally {
                Utils.closeSilently(cursorQuery);
            }
        }
        Utils.closeSilently(cursorQuery);
        return (BucketEntry[]) arrayList.toArray(new BucketEntry[arrayList.size()]);
    }

    private static String getBucketNameInTable(ContentResolver contentResolver, Uri uri, int i) {
        String[] strArr = {String.valueOf(i)};
        Uri uriBuild = uri.buildUpon().appendQueryParameter("limit", SchemaSymbols.ATTVAL_TRUE_1).build();
        Cursor cursorQuery = contentResolver.query(uriBuild, PROJECTION_BUCKET_IN_ONE_TABLE, "bucket_id = ?", strArr, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToNext()) {
                    if (cursorQuery.getString(2) == null) {
                        com.mediatek.gallery3d.util.Log.d("Gallery2/BucketHelper", "<getBucketNameInTable> bucket name not found, uri=" + uriBuild + " bucket_id=" + i);
                    }
                    return cursorQuery.getString(2);
                }
            } finally {
                Utils.closeSilently(cursorQuery);
            }
        }
        Utils.closeSilently(cursorQuery);
        com.mediatek.gallery3d.util.Log.d("Gallery2/BucketHelper", "<getBucketNameInTable> getBucketNameInTable return null, uri=" + uriBuild + " bucket_id=" + i);
        return null;
    }

    @TargetApi(11)
    private static Uri getFilesContentUri() {
        return MediaStore.Files.getContentUri("external");
    }

    public static String getBucketName(ContentResolver contentResolver, int i) {
        if (ApiHelper.HAS_MEDIA_PROVIDER_FILES_TABLE) {
            String bucketNameInTable = getBucketNameInTable(contentResolver, getFilesContentUri(), i);
            return bucketNameInTable == null ? "" : bucketNameInTable;
        }
        String bucketNameInTable2 = getBucketNameInTable(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, i);
        if (bucketNameInTable2 != null) {
            return bucketNameInTable2;
        }
        String bucketNameInTable3 = getBucketNameInTable(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, i);
        return bucketNameInTable3 == null ? "" : bucketNameInTable3;
    }

    public static class BucketEntry {
        public int bucketId;
        public String bucketName;
        public int dateTaken;

        public BucketEntry(int i, String str) {
            this.bucketId = i;
            this.bucketName = Utils.ensureNotNull(str);
        }

        public int hashCode() {
            return this.bucketId;
        }

        public boolean equals(Object obj) {
            return (obj instanceof BucketEntry) && this.bucketId == ((BucketEntry) obj).bucketId;
        }
    }
}
