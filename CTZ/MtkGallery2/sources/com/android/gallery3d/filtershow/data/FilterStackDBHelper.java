package com.android.gallery3d.filtershow.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.mediatek.gallery3d.video.BookmarkEnhance;

public class FilterStackDBHelper extends SQLiteOpenHelper {
    private static final String[][] CREATE_FILTER_STACK = {new String[]{BookmarkEnhance.COLUMN_ID, "INTEGER PRIMARY KEY AUTOINCREMENT"}, new String[]{"stack_id", "TEXT"}, new String[]{"stack", "BLOB"}};

    public FilterStackDBHelper(Context context, String str, int i) {
        super(context, str, (SQLiteDatabase.CursorFactory) null, i);
    }

    public FilterStackDBHelper(Context context, String str) {
        this(context, str, 1);
    }

    public FilterStackDBHelper(Context context) {
        this(context, "filterstacks.db");
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        createTable(sQLiteDatabase, "filterstack", CREATE_FILTER_STACK);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        dropTable(sQLiteDatabase, "filterstack");
        onCreate(sQLiteDatabase);
    }

    protected static void createTable(SQLiteDatabase sQLiteDatabase, String str, String[][] strArr) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ");
        sb.append(str);
        sb.append('(');
        int length = strArr.length;
        boolean z = true;
        int i = 0;
        while (i < length) {
            String[] strArr2 = strArr[i];
            if (!z) {
                sb.append(',');
            }
            for (String str2 : strArr2) {
                sb.append(str2);
                sb.append(' ');
            }
            i++;
            z = false;
        }
        sb.append(')');
        sQLiteDatabase.beginTransaction();
        try {
            sQLiteDatabase.execSQL(sb.toString());
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }

    protected static void dropTable(SQLiteDatabase sQLiteDatabase, String str) {
        sQLiteDatabase.beginTransaction();
        try {
            sQLiteDatabase.execSQL("drop table if exists " + str);
            sQLiteDatabase.setTransactionSuccessful();
        } finally {
            sQLiteDatabase.endTransaction();
        }
    }
}
