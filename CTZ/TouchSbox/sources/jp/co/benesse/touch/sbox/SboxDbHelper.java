package jp.co.benesse.touch.sbox;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SboxDbHelper extends SQLiteOpenHelper {
    public static final String CREATE_TABLE = "CREATE TABLE kvs (appid TEXT NOT NULL, key TEXT NOT NULL, value TEXT, PRIMARY KEY(appid, key));";
    private static final String DB_NAME = "sbox.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE_NAME = "kvs";

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
    }

    public SboxDbHelper(Context context) {
        super(context, DB_NAME, (SQLiteDatabase.CursorFactory) null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(CREATE_TABLE);
    }
}
