package com.android.contacts.database;

import android.database.AbstractCursor;
import android.database.CursorIndexOutOfBoundsException;

public final class EmptyCursor extends AbstractCursor {
    private String[] mColumns;

    public EmptyCursor(String[] strArr) {
        this.mColumns = strArr;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public String[] getColumnNames() {
        return this.mColumns;
    }

    @Override
    public String getString(int i) {
        throw cursorException();
    }

    @Override
    public short getShort(int i) {
        throw cursorException();
    }

    @Override
    public int getInt(int i) {
        throw cursorException();
    }

    @Override
    public long getLong(int i) {
        throw cursorException();
    }

    @Override
    public float getFloat(int i) {
        throw cursorException();
    }

    @Override
    public double getDouble(int i) {
        throw cursorException();
    }

    @Override
    public boolean isNull(int i) {
        throw cursorException();
    }

    private CursorIndexOutOfBoundsException cursorException() {
        return new CursorIndexOutOfBoundsException("Operation not permitted on an empty cursor.");
    }
}
