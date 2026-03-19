package com.android.providers.contacts;

import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.DatabaseUtils;

public class MemoryCursor extends AbstractWindowedCursor {
    private final String[] mColumnNames;

    public MemoryCursor(String str, String[] strArr) {
        setWindow(new CursorWindow(str));
        this.mColumnNames = strArr;
    }

    public void fillFromCursor(Cursor cursor) {
        DatabaseUtils.cursorFillWindow(cursor, 0, getWindow());
    }

    @Override
    public int getCount() {
        return getWindow().getNumRows();
    }

    @Override
    public String[] getColumnNames() {
        return this.mColumnNames;
    }
}
