package jp.co.benesse.dcha.databox.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class KvsDbHelper extends SQLiteOpenHelper {
    public static final String CREATE_TABLE = "CREATE TABLE " + ContractKvs.KVS.pathName + " (" + KvsColumns.APP_ID + " TEXT NOT NULL, key TEXT NOT NULL, value TEXT, PRIMARY KEY(" + KvsColumns.APP_ID + ", key));";
    private static final String DB_NAME = "kvs.db";
    private static final int DB_VERSION = 1;

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
    }

    public KvsDbHelper(Context context) {
        super(context, DB_NAME, (SQLiteDatabase.CursorFactory) null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(CREATE_TABLE);
    }
}
