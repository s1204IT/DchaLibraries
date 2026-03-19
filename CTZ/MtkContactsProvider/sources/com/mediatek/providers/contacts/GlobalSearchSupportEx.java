package com.mediatek.providers.contacts;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.util.Log;

public class GlobalSearchSupportEx {
    public static Cursor processCursor(Cursor cursor, String[] strArr, String str, String[] strArr2) {
        if (cursor != null) {
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex("suggest_shortcut_id");
                if (columnIndex >= 0) {
                    String string = cursor.getString(columnIndex);
                    StringBuilder sb = new StringBuilder();
                    sb.append("[handleSearchShortcutRefresh]new lookupKey:");
                    sb.append(string);
                    sb.append("||It is NE old:");
                    sb.append((string == null || string.equals(str)) ? false : true);
                    Log.d("GlobalSearchSupportEx", sb.toString());
                    if (string != null && !string.equals(str)) {
                        cursor.close();
                        if (strArr == null) {
                            strArr = strArr2;
                        }
                        return new MatrixCursor(strArr);
                    }
                }
                cursor.moveToPosition(-1);
            }
        }
        return cursor;
    }
}
