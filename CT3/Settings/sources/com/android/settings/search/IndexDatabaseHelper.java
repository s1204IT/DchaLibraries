package com.android.settings.search;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

public class IndexDatabaseHelper extends SQLiteOpenHelper {
    private static final String INSERT_BUILD_VERSION = "INSERT INTO meta_index VALUES ('" + Build.VERSION.INCREMENTAL + "');";
    private static IndexDatabaseHelper sSingleton;
    private final Context mContext;

    public static synchronized IndexDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new IndexDatabaseHelper(context);
        }
        return sSingleton;
    }

    public IndexDatabaseHelper(Context context) {
        super(context, "search_index.db", (SQLiteDatabase.CursorFactory) null, 115);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        bootstrapDB(db);
    }

    private void bootstrapDB(SQLiteDatabase db) {
        db.execSQL("CREATE VIRTUAL TABLE prefs_index USING fts4(locale, data_rank, data_title, data_title_normalized, data_summary_on, data_summary_on_normalized, data_summary_off, data_summary_off_normalized, data_entries, data_keywords, screen_title, class_name, icon, intent_action, intent_target_package, intent_target_class, enabled, data_key_reference, user_id);");
        db.execSQL("CREATE TABLE meta_index(build VARCHAR(32) NOT NULL)");
        db.execSQL("CREATE TABLE saved_queries(query VARCHAR(64) NOT NULL, timestamp INTEGER)");
        db.execSQL(INSERT_BUILD_VERSION);
        Log.i("IndexDatabaseHelper", "Bootstrapped database");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        Log.i("IndexDatabaseHelper", "Using schema version: " + db.getVersion());
        if (!Build.VERSION.INCREMENTAL.equals(getBuildVersion(db))) {
            Log.w("IndexDatabaseHelper", "Index needs to be rebuilt as build-version is not the same");
            reconstruct(db);
        } else {
            Log.i("IndexDatabaseHelper", "Index is fine");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion >= 115) {
            return;
        }
        Log.w("IndexDatabaseHelper", "Detected schema version '" + oldVersion + "'. Index needs to be rebuilt for schema version '" + newVersion + "'.");
        reconstruct(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("IndexDatabaseHelper", "Detected schema version '" + oldVersion + "'. Index needs to be rebuilt for schema version '" + newVersion + "'.");
        reconstruct(db);
    }

    private void reconstruct(SQLiteDatabase db) {
        dropTables(db);
        bootstrapDB(db);
    }

    private String getBuildVersion(SQLiteDatabase db) {
        String version = null;
        Cursor cursor = null;
        try {
            try {
                cursor = db.rawQuery("SELECT build FROM meta_index LIMIT 1;", null);
                if (cursor.moveToFirst()) {
                    version = cursor.getString(0);
                }
            } catch (Exception e) {
                Log.e("IndexDatabaseHelper", "Cannot get build version from Index metadata");
                if (cursor != null) {
                    cursor.close();
                }
            }
            return version;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void clearLocalesIndexed(Context context) {
        context.getSharedPreferences("index", 0).edit().clear().commit();
    }

    public static void setLocaleIndexed(Context context, String locale) {
        context.getSharedPreferences("index", 0).edit().putBoolean(locale, true).commit();
    }

    public static boolean isLocaleAlreadyIndexed(Context context, String locale) {
        return context.getSharedPreferences("index", 0).getBoolean(locale, false);
    }

    private void dropTables(SQLiteDatabase db) {
        clearLocalesIndexed(this.mContext);
        db.execSQL("DROP TABLE IF EXISTS meta_index");
        db.execSQL("DROP TABLE IF EXISTS prefs_index");
        db.execSQL("DROP TABLE IF EXISTS saved_queries");
    }
}
