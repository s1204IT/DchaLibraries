package com.android.documentsui;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.Bundle;

public class RootCursorWrapper extends AbstractCursor {
    private final String mAuthority;
    private final int mAuthorityIndex;
    private final String[] mColumnNames;
    private final int mCount;
    private final Cursor mCursor;
    private final String mRootId;
    private final int mRootIdIndex;

    public RootCursorWrapper(String authority, String rootId, Cursor cursor, int maxCount) {
        this.mAuthority = authority;
        this.mRootId = rootId;
        this.mCursor = cursor;
        int count = cursor.getCount();
        if (maxCount > 0 && count > maxCount) {
            this.mCount = maxCount;
        } else {
            this.mCount = count;
        }
        if (cursor.getColumnIndex("android:authority") != -1 || cursor.getColumnIndex("android:rootId") != -1) {
            throw new IllegalArgumentException("Cursor contains internal columns!");
        }
        String[] before = cursor.getColumnNames();
        this.mColumnNames = new String[before.length + 2];
        System.arraycopy(before, 0, this.mColumnNames, 0, before.length);
        this.mAuthorityIndex = before.length;
        this.mRootIdIndex = before.length + 1;
        this.mColumnNames[this.mAuthorityIndex] = "android:authority";
        this.mColumnNames[this.mRootIdIndex] = "android:rootId";
    }

    @Override
    public Bundle getExtras() {
        return this.mCursor.getExtras();
    }

    @Override
    public void close() {
        super.close();
        this.mCursor.close();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return this.mCursor.moveToPosition(newPosition);
    }

    @Override
    public String[] getColumnNames() {
        return this.mColumnNames;
    }

    @Override
    public int getCount() {
        return this.mCount;
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
        if (column == this.mAuthorityIndex) {
            return this.mAuthority;
        }
        if (column == this.mRootIdIndex) {
            return this.mRootId;
        }
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
