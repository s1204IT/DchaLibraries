package com.android.gallery3d.filtershow.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FilterStackDBHelper extends SQLiteOpenHelper {
    private static final String[][] CREATE_FILTER_STACK = {new String[]{"_id", "INTEGER PRIMARY KEY AUTOINCREMENT"}, new String[]{"stack_id", "TEXT"}, new String[]{"stack", "BLOB"}};

    public FilterStackDBHelper(Context context, String name, int version) {
        super(context, name, (SQLiteDatabase.CursorFactory) null, version);
    }

    public FilterStackDBHelper(Context context, String name) {
        this(context, name, 1);
    }

    public FilterStackDBHelper(Context context) {
        this(context, "filterstacks.db");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db, "filterstack", CREATE_FILTER_STACK);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTable(db, "filterstack");
        onCreate(db);
    }

    protected static void createTable(SQLiteDatabase db, String table, String[][] columns) {
        StringBuilder create = new StringBuilder("CREATE TABLE ");
        create.append(table).append('(');
        boolean first = true;
        for (String[] column : columns) {
            if (!first) {
                create.append(',');
            }
            first = false;
            for (String val : column) {
                create.append(val).append(' ');
            }
        }
        create.append(')');
        db.beginTransaction();
        try {
            db.execSQL(create.toString());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    protected static void dropTable(SQLiteDatabase db, String table) {
        db.beginTransaction();
        try {
            db.execSQL("drop table if exists " + table);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
