package com.android.camera.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class DataUtils {
    public static String getPathFromURI(ContentResolver contentResolver, Uri contentUri) {
        String string = null;
        String[] proj = {"_data"};
        Cursor cursor = contentResolver.query(contentUri, proj, null, null, null);
        if (cursor != null) {
            try {
                int columnIndex = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    string = cursor.getString(columnIndex);
                }
            } finally {
                cursor.close();
            }
        }
        return string;
    }
}
