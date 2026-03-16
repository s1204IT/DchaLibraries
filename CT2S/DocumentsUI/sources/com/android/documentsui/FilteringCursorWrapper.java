package com.android.documentsui;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import com.android.documentsui.model.DocumentInfo;

public class FilteringCursorWrapper extends AbstractCursor {
    private int mCount;
    private final Cursor mCursor;
    private final int[] mPosition;

    public FilteringCursorWrapper(Cursor cursor, String[] acceptMimes, String[] rejectMimes) {
        this(cursor, acceptMimes, rejectMimes, Long.MIN_VALUE);
    }

    public FilteringCursorWrapper(Cursor cursor, String[] acceptMimes, String[] rejectMimes, long rejectBefore) {
        this.mCursor = cursor;
        int count = cursor.getCount();
        this.mPosition = new int[count];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext() && this.mCount < count) {
            String mimeType = DocumentInfo.getCursorString(cursor, "mime_type");
            long lastModified = DocumentInfo.getCursorLong(cursor, "last_modified");
            if (rejectMimes == null || !MimePredicate.mimeMatches(rejectMimes, mimeType)) {
                if (lastModified >= rejectBefore && MimePredicate.mimeMatches(acceptMimes, mimeType)) {
                    int[] iArr = this.mPosition;
                    int i = this.mCount;
                    this.mCount = i + 1;
                    iArr[i] = cursor.getPosition();
                }
            }
        }
        Log.d("Documents", "Before filtering " + cursor.getCount() + ", after " + this.mCount);
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
        return this.mCursor.moveToPosition(this.mPosition[newPosition]);
    }

    @Override
    public String[] getColumnNames() {
        return this.mCursor.getColumnNames();
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
