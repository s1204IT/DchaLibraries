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

    private CursorBackedSuggestionExtras(Cursor cursor, int i, List<String> list) {
        super(null);
        this.mCursor = cursor;
        this.mCursorPosition = i;
        this.mExtraColumns = list;
    }

    static CursorBackedSuggestionExtras createExtrasIfNecessary(Cursor cursor, int i) {
        List<String> extraColumns = getExtraColumns(cursor);
        if (extraColumns != null) {
            return new CursorBackedSuggestionExtras(cursor, i, extraColumns);
        }
        return null;
    }

    static String[] getCursorColumns(Cursor cursor) {
        try {
            return cursor.getColumnNames();
        } catch (RuntimeException e) {
            Log.e("QSB.CursorBackedSuggestionExtras", "getColumnNames() failed, ", e);
            return null;
        }
    }

    static List<String> getExtraColumns(Cursor cursor) {
        ArrayList arrayList = null;
        String[] cursorColumns = getCursorColumns(cursor);
        if (cursorColumns != null) {
            for (String str : cursorColumns) {
                if (!DEFAULT_COLUMNS.contains(str)) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    arrayList.add(str);
                }
            }
        }
        return arrayList;
    }

    @Override
    public String doGetExtra(String str) {
        try {
            this.mCursor.moveToPosition(this.mCursorPosition);
            int columnIndex = this.mCursor.getColumnIndex(str);
            if (columnIndex < 0) {
                return null;
            }
            return this.mCursor.getString(columnIndex);
        } catch (RuntimeException e) {
            Log.e("QSB.CursorBackedSuggestionExtras", "getExtra(" + str + ") failed, ", e);
            return null;
        }
    }
}
