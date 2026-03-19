package com.android.documentsui.roots;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import com.android.documentsui.base.SharedMinimal;

public class RootCursorWrapper extends AbstractCursor {
    private final String mAuthority;
    private final int mAuthorityIndex;
    private final String[] mColumnNames;
    private final int mCount;
    private final Cursor mCursor;
    private final String mRootId;
    private final int mRootIdIndex;

    public RootCursorWrapper(String str, String str2, Cursor cursor, int i) {
        this.mAuthority = str;
        this.mRootId = str2;
        this.mCursor = cursor;
        int count = cursor.getCount();
        if (i > 0 && count > i) {
            this.mCount = i;
        } else {
            this.mCount = count;
        }
        if (cursor.getColumnIndex("android:authority") != -1 || cursor.getColumnIndex("android:rootId") != -1) {
            throw new IllegalArgumentException("Cursor contains internal columns!");
        }
        String[] columnNames = cursor.getColumnNames();
        this.mColumnNames = new String[columnNames.length + 2];
        System.arraycopy(columnNames, 0, this.mColumnNames, 0, columnNames.length);
        this.mAuthorityIndex = columnNames.length;
        this.mRootIdIndex = columnNames.length + 1;
        this.mColumnNames[this.mAuthorityIndex] = "android:authority";
        this.mColumnNames[this.mRootIdIndex] = "android:rootId";
    }

    @Override
    public Bundle getExtras() {
        Bundle extras = this.mCursor.getExtras();
        if (extras == null) {
            if (SharedMinimal.VERBOSE) {
                Log.v("RootCursorWrapper", "Cursor for root " + this.mRootId + " does not have any extras.");
            }
            return Bundle.EMPTY;
        }
        return extras;
    }

    @Override
    public void close() {
        super.close();
        this.mCursor.close();
    }

    @Override
    public boolean onMove(int i, int i2) {
        return this.mCursor.moveToPosition(i2);
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
        if (i == this.mAuthorityIndex) {
            return this.mAuthority;
        }
        if (i == this.mRootIdIndex) {
            return this.mRootId;
        }
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
