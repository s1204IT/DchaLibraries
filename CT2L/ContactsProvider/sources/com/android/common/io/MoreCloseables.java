package com.android.common.io;

import android.database.Cursor;

public class MoreCloseables {
    public static void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
