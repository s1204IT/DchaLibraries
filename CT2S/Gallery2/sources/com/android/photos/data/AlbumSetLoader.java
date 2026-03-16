package com.android.photos.data;

import android.database.MatrixCursor;

public class AlbumSetLoader {
    public static final String[] PROJECTION = {"_id", "title", "timestamp", "thumb_uri", "thumb_width", "thumb_height", "count_pending_upload", "_count", "supported_operations"};
    public static final MatrixCursor MOCK = createRandomCursor(30);

    private static MatrixCursor createRandomCursor(int count) {
        MatrixCursor c = new MatrixCursor(PROJECTION, count);
        for (int i = 0; i < count; i++) {
            c.addRow(createRandomRow());
        }
        return c;
    }

    private static Object[] createRandomRow() {
        double random = Math.random();
        int id = (int) (500.0d * random);
        Object[] row = new Object[9];
        row[0] = Integer.valueOf(id);
        row[1] = "Fun times " + id;
        row[2] = Long.valueOf((long) (System.currentTimeMillis() * random));
        row[3] = null;
        row[4] = 0;
        row[5] = 0;
        row[6] = Integer.valueOf(random < 0.3d ? 1 : 0);
        row[7] = 1;
        row[8] = 0;
        return row;
    }
}
