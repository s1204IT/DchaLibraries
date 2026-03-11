package com.android.settings.fuelgauge.batterytip;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AnomalyDatabaseHelper extends SQLiteOpenHelper {
    private static AnomalyDatabaseHelper sSingleton;

    public static synchronized AnomalyDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new AnomalyDatabaseHelper(context.getApplicationContext());
        }
        return sSingleton;
    }

    private AnomalyDatabaseHelper(Context context) {
        super(context, "battery_settings.db", (SQLiteDatabase.CursorFactory) null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        bootstrapDB(sQLiteDatabase);
    }

    private void bootstrapDB(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE anomaly(uid INTEGER NOT NULL, package_name TEXT, anomaly_type INTEGER NOT NULL, anomaly_state INTEGER NOT NULL, time_stamp_ms INTEGER NOT NULL,  PRIMARY KEY (uid,anomaly_type,anomaly_state,time_stamp_ms))");
        Log.i("BatteryDatabaseHelper", "Bootstrapped database");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 4) {
            Log.w("BatteryDatabaseHelper", "Detected schema version '" + i + "'. Index needs to be rebuilt for schema version '" + i2 + "'.");
            reconstruct(sQLiteDatabase);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        Log.w("BatteryDatabaseHelper", "Detected schema version '" + i + "'. Index needs to be rebuilt for schema version '" + i2 + "'.");
        reconstruct(sQLiteDatabase);
    }

    public void reconstruct(SQLiteDatabase sQLiteDatabase) {
        dropTables(sQLiteDatabase);
        bootstrapDB(sQLiteDatabase);
    }

    private void dropTables(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS anomaly");
    }
}
