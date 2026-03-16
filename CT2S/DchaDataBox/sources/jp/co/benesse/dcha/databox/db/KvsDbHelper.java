package jp.co.benesse.dcha.databox.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class KvsDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "kvs.db";
    private static final int DB_VERSION = 1;

    public KvsDbHelper(Context context) {
        super(context, DB_NAME, (SQLiteDatabase.CursorFactory) null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(KvsColumns.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
