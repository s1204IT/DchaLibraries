package com.android.providers.contacts;

import android.database.AbstractCursor;
import android.database.Cursor;

public class ReorderingCursorWrapper extends AbstractCursor {
    private final Cursor mCursor;
    private final int[] mPositionMap;

    public ReorderingCursorWrapper(Cursor cursor, int[] iArr) {
        if (cursor.getCount() != iArr.length) {
            throw new IllegalArgumentException("Cursor and position map have different sizes.");
        }
        this.mCursor = cursor;
        this.mPositionMap = iArr;
    }

    @Override
    public void close() {
        super.close();
        this.mCursor.close();
    }

    @Override
    public boolean onMove(int i, int i2) {
        return this.mCursor.moveToPosition(this.mPositionMap[i2]);
    }

    @Override
    public String[] getColumnNames() {
        return this.mCursor.getColumnNames();
    }

    @Override
    public int getCount() {
        return this.mCursor.getCount();
    }

    @Override
    public double getDouble(int i) {
        return this.mCursor.getDouble(i);
    }

    @Override
    public float getFloat(int i) {
        return this.mCursor.getFloat(i);
    }

    @Override
    public int getInt(int i) {
        return this.mCursor.getInt(i);
    }

    @Override
    public long getLong(int i) {
        return this.mCursor.getLong(i);
    }

    @Override
    public short getShort(int i) {
        return this.mCursor.getShort(i);
    }

    @Override
    public String getString(int i) {
        return this.mCursor.getString(i);
    }

    @Override
    public int getType(int i) {
        return this.mCursor.getType(i);
    }

    @Override
    public boolean isNull(int i) {
        return this.mCursor.isNull(i);
    }
}
