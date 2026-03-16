package com.android.quicksearchbox;

import android.database.Cursor;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class CursorBackedSuggestionExtras extends AbstractSuggestionExtras {
    private static final HashSet<String> DEFAULT_COLUMNS = new HashSet<>();
    private final Cursor mCursor;
    private final int mCursorPosition;
    private final List<String> mExtraColumns;

    static {
        DEFAULT_COLUMNS.addAll(Arrays.asList(SuggestionCursorBackedCursor.COLUMNS));
    }

    static CursorBackedSuggestionExtras createExtrasIfNecessary(Cursor cursor, int position) {
        List<String> extraColumns = getExtraColumns(cursor);
        if (extraColumns != null) {
            return new CursorBackedSuggestionExtras(cursor, position, extraColumns);
        }
        return null;
    }

    static String[] getCursorColumns(Cursor cursor) {
        try {
            return cursor.getColumnNames();
        } catch (RuntimeException ex) {
            Log.e("QSB.CursorBackedSuggestionExtras", "getColumnNames() failed, ", ex);
            return null;
        }
    }

    static List<String> getExtraColumns(Cursor cursor) {
        String[] columns = getCursorColumns(cursor);
        if (columns == null) {
            return null;
        }
        List<String> extraColumns = null;
        for (String cursorColumn : columns) {
            if (!DEFAULT_COLUMNS.contains(cursorColumn)) {
                if (extraColumns == null) {
                    extraColumns = new ArrayList<>();
                }
                extraColumns.add(cursorColumn);
            }
        }
        return extraColumns;
    }

    private CursorBackedSuggestionExtras(Cursor cursor, int position, List<String> extraColumns) {
        super(null);
        this.mCursor = cursor;
        this.mCursorPosition = position;
        this.mExtraColumns = extraColumns;
    }

    @Override
    public String doGetExtra(String columnName) {
        try {
            this.mCursor.moveToPosition(this.mCursorPosition);
            int columnIdx = this.mCursor.getColumnIndex(columnName);
            if (columnIdx < 0) {
                return null;
            }
            return this.mCursor.getString(columnIdx);
        } catch (RuntimeException ex) {
            Log.e("QSB.CursorBackedSuggestionExtras", "getExtra(" + columnName + ") failed, ", ex);
            return null;
        }
    }
}
