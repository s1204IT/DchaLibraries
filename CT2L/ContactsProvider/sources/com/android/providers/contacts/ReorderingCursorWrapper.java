package com.android.providers.contacts;

import android.database.AbstractCursor;
import android.database.Cursor;

public class ReorderingCursorWrapper extends AbstractCursor {
    private final Cursor mCursor;
    private final int[] mPositionMap;

    public ReorderingCursorWrapper(Cursor cursor, int[] positionMap) {
        if (cursor.getCount() != positionMap.length) {
            throw new IllegalArgumentException("Cursor and position map have different sizes.");
        }
        this.mCursor = cursor;
        this.mPositionMap = positionMap;
    }

    @Override
    public void close() {
        super.close();
        this.mCursor.close();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return this.mCursor.moveToPosition(this.mPositionMap[newPosition]);
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
    public double getDouble(int column) {
        return this.mCursor.getDouble(column);
    }

    @Override
    public float getFloat(int column) {
        return this.mCursor.getFloat(column);
    }

    @Override
    public int getInt(int column) {
        return this.mCursor.getInt(column);
    }

    @Override
    public long getLong(int column) {
        return this.mCursor.getLong(column);
    }

    @Override
    public short getShort(int column) {
        return this.mCursor.getShort(column);
    }

    @Override
    public String getString(int column) {
        return this.mCursor.getString(column);
    }

    @Override
    public int getType(int column) {
        return this.mCursor.getType(column);
    }

    @Override
    public boolean isNull(int column) {
        return this.mCursor.isNull(column);
    }
}
