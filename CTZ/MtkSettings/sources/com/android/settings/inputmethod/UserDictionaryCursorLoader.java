package com.android.settings.inputmethod;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.UserDictionary;
import android.util.ArraySet;
import java.util.Locale;
import java.util.Objects;

public class UserDictionaryCursorLoader extends CursorLoader {
    static final String[] QUERY_PROJECTION = {"_id", "word", "shortcut"};
    private final String mLocale;

    public UserDictionaryCursorLoader(Context context, String str) {
        super(context);
        this.mLocale = str;
    }

    @Override
    public Cursor loadInBackground() {
        Cursor cursorQuery;
        MatrixCursor matrixCursor = new MatrixCursor(QUERY_PROJECTION);
        if ("".equals(this.mLocale)) {
            cursorQuery = getContext().getContentResolver().query(UserDictionary.Words.CONTENT_URI, QUERY_PROJECTION, "locale is null", null, "UPPER(word)");
        } else {
            cursorQuery = getContext().getContentResolver().query(UserDictionary.Words.CONTENT_URI, QUERY_PROJECTION, "locale=?", new String[]{this.mLocale != null ? this.mLocale : Locale.getDefault().toString()}, "UPPER(word)");
        }
        ArraySet arraySet = new ArraySet();
        cursorQuery.moveToFirst();
        while (!cursorQuery.isAfterLast()) {
            int i = cursorQuery.getInt(0);
            String string = cursorQuery.getString(1);
            String string2 = cursorQuery.getString(2);
            int iHash = Objects.hash(string, string2);
            if (!arraySet.contains(Integer.valueOf(iHash))) {
                arraySet.add(Integer.valueOf(iHash));
                matrixCursor.addRow(new Object[]{Integer.valueOf(i), string, string2});
            }
            cursorQuery.moveToNext();
        }
        return matrixCursor;
    }
}
