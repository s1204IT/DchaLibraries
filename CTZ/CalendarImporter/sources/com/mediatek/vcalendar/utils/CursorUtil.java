package com.mediatek.vcalendar.utils;

import android.database.Cursor;
import android.database.MatrixCursor;

public class CursorUtil {
    private CursorUtil() {
    }

    public static MatrixCursor copyOneCursor(Cursor cursor) {
        MatrixCursor matrixCursor = new MatrixCursor(cursor.getColumnNames());
        int columnCount = cursor.getColumnCount();
        String[] strArr = new String[columnCount];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            for (int i = 0; i < columnCount; i++) {
                strArr[i] = cursor.getString(i);
            }
            matrixCursor.addRow(strArr);
        }
        return matrixCursor;
    }

    public static MatrixCursor copyCurrentRow(Cursor cursor) {
        MatrixCursor matrixCursor = new MatrixCursor(cursor.getColumnNames(), 1);
        int columnCount = cursor.getColumnCount();
        String[] strArr = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            strArr[i] = cursor.getString(i);
        }
        matrixCursor.addRow(strArr);
        return matrixCursor;
    }
}
