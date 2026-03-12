package com.android.settings.intelligence.search.indexing;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class IndexDatabaseHelper extends SQLiteOpenHelper {
    private static final String INSERT_BUILD_VERSION = "INSERT INTO meta_index VALUES ('" + Build.VERSION.INCREMENTAL + "');";
    static final String SHARED_PREFS_TAG = "indexing_manager";
    private static IndexDatabaseHelper sSingleton;
    private final Context mContext;

    public static synchronized IndexDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new IndexDatabaseHelper(context);
        }
        return sSingleton;
    }

    public IndexDatabaseHelper(Context context) {
        super(context, "search_index.db", (SQLiteDatabase.CursorFactory) null, 119);
        this.mContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        bootstrapDB(sQLiteDatabase);
    }

    private void bootstrapDB(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE VIRTUAL TABLE prefs_index USING fts4(data_title, data_title_normalized, data_summary_on, data_summary_on_normalized, data_summary_off, data_summary_off_normalized, data_entries, data_keywords, package, screen_title, class_name, icon, intent_action, intent_target_package, intent_target_class, enabled, data_key_reference, payload_type, payload);");
        sQLiteDatabase.execSQL("CREATE TABLE meta_index(build VARCHAR(32) NOT NULL)");
        sQLiteDatabase.execSQL("CREATE TABLE saved_queries(query VARCHAR(64) NOT NULL, timestamp INTEGER)");
        sQLiteDatabase.execSQL("CREATE VIRTUAL TABLE site_map USING fts4(parent_class, child_class, parent_title, child_title)");
        sQLiteDatabase.execSQL(INSERT_BUILD_VERSION);
        Log.i("IndexDatabaseHelper", "Bootstrapped database");
    }

    @Override
    public void onOpen(SQLiteDatabase sQLiteDatabase) {
        super.onOpen(sQLiteDatabase);
        Log.i("IndexDatabaseHelper", "Using schema version: " + sQLiteDatabase.getVersion());
        if (!Build.VERSION.INCREMENTAL.equals(getBuildVersion(sQLiteDatabase))) {
            Log.w("IndexDatabaseHelper", "Index needs to be rebuilt as build-version is not the same");
            reconstruct(sQLiteDatabase);
        } else {
            Log.i("IndexDatabaseHelper", "Index is fine");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 119) {
            Log.w("IndexDatabaseHelper", "Detected schema version '" + i + "'. Index needs to be rebuilt for schema version '" + i2 + "'.");
            reconstruct(sQLiteDatabase);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        Log.w("IndexDatabaseHelper", "Detected schema version '" + i + "'. Index needs to be rebuilt for schema version '" + i2 + "'.");
        reconstruct(sQLiteDatabase);
    }

    public void reconstruct(SQLiteDatabase sQLiteDatabase) {
        this.mContext.getSharedPreferences(SHARED_PREFS_TAG, 0).edit().clear().commit();
        dropTables(sQLiteDatabase);
        bootstrapDB(sQLiteDatabase);
    }

    private String getBuildVersion(SQLiteDatabase sQLiteDatabase) throws Throwable {
        Throwable th;
        Cursor cursorRawQuery;
        try {
            cursorRawQuery = sQLiteDatabase.rawQuery("SELECT build FROM meta_index LIMIT 1;", null);
            try {
                try {
                    string = cursorRawQuery.moveToFirst() ? cursorRawQuery.getString(0) : null;
                } catch (Exception e) {
                    Log.e("IndexDatabaseHelper", "Cannot get build version from Index metadata");
                    if (cursorRawQuery != null) {
                    }
                    return string;
                }
            } catch (Throwable th2) {
                th = th2;
                if (cursorRawQuery != null) {
                    cursorRawQuery.close();
                }
                throw th;
            }
        } catch (Exception e2) {
            cursorRawQuery = null;
        } catch (Throwable th3) {
            th = th3;
            cursorRawQuery = null;
            if (cursorRawQuery != null) {
            }
            throw th;
        }
        if (cursorRawQuery != null) {
            cursorRawQuery.close();
        }
        return string;
    }

    static String buildProviderVersionedNames(Context context, List<ResolveInfo> list) {
        try {
            StringBuilder sb = new StringBuilder();
            Iterator<ResolveInfo> it = list.iterator();
            while (it.hasNext()) {
                String str = it.next().providerInfo.packageName;
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(str, 0);
                sb.append(str);
                sb.append(':');
                sb.append(packageInfo.versionCode);
                sb.append(',');
            }
            sb.append(context.getPackageName());
            sb.append(':');
            sb.append(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
            return sb.toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("IndexDatabaseHelper", "Could not find package name in provider", e);
            return "";
        }
    }

    static void setIndexed(Context context, List<ResolveInfo> list) {
        String string = Locale.getDefault().toString();
        String str = Build.FINGERPRINT;
        context.getSharedPreferences(SHARED_PREFS_TAG, 0).edit().putBoolean(string, true).putBoolean(str, true).putString("indexed_providers", buildProviderVersionedNames(context, list)).apply();
    }

    static boolean isFullIndex(Context context, List<ResolveInfo> list) {
        String string = Locale.getDefault().toString();
        String str = Build.FINGERPRINT;
        String strBuildProviderVersionedNames = buildProviderVersionedNames(context, list);
        boolean z = false;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_TAG, 0);
        if (sharedPreferences.getBoolean(str, false) && sharedPreferences.getBoolean(string, false) && TextUtils.equals(sharedPreferences.getString("indexed_providers", null), strBuildProviderVersionedNames)) {
            z = true;
        }
        return !z;
    }

    private void dropTables(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS meta_index");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS prefs_index");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS saved_queries");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS site_map");
    }
}
