package com.android.contacts.common.util;

import android.database.Cursor;

public class DataStatus {
    private int mPresence = -1;
    private String mStatus = null;
    private long mTimestamp = -1;
    private String mResPackage = null;
    private int mIconRes = -1;
    private int mLabelRes = -1;

    public DataStatus() {
    }

    public DataStatus(Cursor cursor) {
        fromCursor(cursor);
    }

    private void fromCursor(Cursor cursor) {
        this.mPresence = getInt(cursor, "mode", -1);
        this.mStatus = getString(cursor, "status");
        this.mTimestamp = getLong(cursor, "status_ts", -1L);
        this.mResPackage = getString(cursor, "status_res_package");
        this.mIconRes = getInt(cursor, "status_icon", -1);
        this.mLabelRes = getInt(cursor, "status_label", -1);
    }

    private static String getString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    private static int getInt(Cursor cursor, String columnName, int missingValue) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (cursor.isNull(columnIndex)) {
            return missingValue;
        }
        int missingValue2 = cursor.getInt(columnIndex);
        return missingValue2;
    }

    private static long getLong(Cursor cursor, String columnName, long missingValue) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (cursor.isNull(columnIndex)) {
            return missingValue;
        }
        long missingValue2 = cursor.getLong(columnIndex);
        return missingValue2;
    }
}
