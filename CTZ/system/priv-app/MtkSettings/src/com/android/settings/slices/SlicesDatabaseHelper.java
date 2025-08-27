package com.android.settings.slices;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;
import java.util.Locale;

/* loaded from: classes.dex */
public class SlicesDatabaseHelper extends SQLiteOpenHelper {
    private static SlicesDatabaseHelper sSingleton;
    private final Context mContext;

    public static synchronized SlicesDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new SlicesDatabaseHelper(context.getApplicationContext());
        }
        return sSingleton;
    }

    private SlicesDatabaseHelper(Context context) {
        super(context, "slices_index.db", (SQLiteDatabase.CursorFactory) null, 2);
        this.mContext = context;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) throws SQLException {
        createDatabases(sQLiteDatabase);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws SQLException {
        if (i < 2) {
            Log.d("SlicesDatabaseHelper", "Reconstructing DB from " + i + "to " + i2);
            reconstruct(sQLiteDatabase);
        }
    }

    void reconstruct(SQLiteDatabase sQLiteDatabase) throws SQLException {
        this.mContext.getSharedPreferences("slices_shared_prefs", 0).edit().clear().apply();
        dropTables(sQLiteDatabase);
        createDatabases(sQLiteDatabase);
    }

    public void setIndexedState() {
        setBuildIndexed();
        setLocaleIndexed();
    }

    public boolean isSliceDataIndexed() {
        return isBuildIndexed() && isLocaleIndexed();
    }

    private void createDatabases(SQLiteDatabase sQLiteDatabase) throws SQLException {
        sQLiteDatabase.execSQL("CREATE VIRTUAL TABLE slices_index USING fts4(key, title, summary, screentitle, keywords, icon, fragment, controller, platform_slice, slice_type);");
        Log.d("SlicesDatabaseHelper", "Created databases");
    }

    private void dropTables(SQLiteDatabase sQLiteDatabase) throws SQLException {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS slices_index");
    }

    private void setBuildIndexed() {
        this.mContext.getSharedPreferences("slices_shared_prefs", 0).edit().putBoolean(getBuildTag(), true).apply();
    }

    private void setLocaleIndexed() {
        this.mContext.getSharedPreferences("slices_shared_prefs", 0).edit().putBoolean(Locale.getDefault().toString(), true).apply();
    }

    private boolean isBuildIndexed() {
        return this.mContext.getSharedPreferences("slices_shared_prefs", 0).getBoolean(getBuildTag(), false);
    }

    private boolean isLocaleIndexed() {
        return this.mContext.getSharedPreferences("slices_shared_prefs", 0).getBoolean(Locale.getDefault().toString(), false);
    }

    String getBuildTag() {
        return Build.FINGERPRINT;
    }
}
