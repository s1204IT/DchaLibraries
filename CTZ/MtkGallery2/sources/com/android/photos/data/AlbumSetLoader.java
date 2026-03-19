package com.android.photos.data;

import android.database.MatrixCursor;
import com.mediatek.gallery3d.video.BookmarkEnhance;

public class AlbumSetLoader {
    public static final String[] PROJECTION = {BookmarkEnhance.COLUMN_ID, "title", "timestamp", "thumb_uri", "thumb_width", "thumb_height", "count_pending_upload", "_count", "supported_operations"};
    public static final MatrixCursor MOCK = createRandomCursor(30);

    private static MatrixCursor createRandomCursor(int i) {
        MatrixCursor matrixCursor = new MatrixCursor(PROJECTION, i);
        for (int i2 = 0; i2 < i; i2++) {
            matrixCursor.addRow(createRandomRow());
        }
        return matrixCursor;
    }

    private static Object[] createRandomRow() {
        double dRandom = Math.random();
        int i = (int) (500.0d * dRandom);
        Object[] objArr = new Object[9];
        objArr[0] = Integer.valueOf(i);
        objArr[1] = "Fun times " + i;
        objArr[2] = Long.valueOf((long) (((double) System.currentTimeMillis()) * dRandom));
        objArr[3] = null;
        objArr[4] = 0;
        objArr[5] = 0;
        objArr[6] = Integer.valueOf(dRandom < 0.3d ? 1 : 0);
        objArr[7] = 1;
        objArr[8] = 0;
        return objArr;
    }
}
