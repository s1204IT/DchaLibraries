package com.android.launcher3.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.android.launcher3.Utilities;

public abstract class NoLocaleSQLiteHelper extends SQLiteOpenHelper {
    public NoLocaleSQLiteHelper(Context context, String str, int i) {
        super(Utilities.ATLEAST_P ? context : new NoLocalContext(context), str, (SQLiteDatabase.CursorFactory) null, i);
        if (Utilities.ATLEAST_P) {
            setOpenParams(new SQLiteDatabase.OpenParams.Builder().addOpenFlags(16).build());
        }
    }

    private static class NoLocalContext extends ContextWrapper {
        public NoLocalContext(Context context) {
            super(context);
        }

        @Override
        public SQLiteDatabase openOrCreateDatabase(String str, int i, SQLiteDatabase.CursorFactory cursorFactory, DatabaseErrorHandler databaseErrorHandler) {
            return super.openOrCreateDatabase(str, i | 16, cursorFactory, databaseErrorHandler);
        }
    }
}
