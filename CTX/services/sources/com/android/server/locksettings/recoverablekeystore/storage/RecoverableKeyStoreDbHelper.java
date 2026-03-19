package com.android.server.locksettings.recoverablekeystore.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class RecoverableKeyStoreDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "recoverablekeystore.db";
    static final int DATABASE_VERSION = 4;
    private static final String SQL_CREATE_KEYS_ENTRY = "CREATE TABLE keys( _id INTEGER PRIMARY KEY,user_id INTEGER,uid INTEGER,alias TEXT,nonce BLOB,wrapped_key BLOB,platform_key_generation_id INTEGER,last_synced_at INTEGER,recovery_status INTEGER,UNIQUE(uid,alias))";
    private static final String SQL_CREATE_RECOVERY_SERVICE_METADATA_ENTRY = "CREATE TABLE recovery_service_metadata (_id INTEGER PRIMARY KEY,user_id INTEGER,uid INTEGER,snapshot_version INTEGER,should_create_snapshot INTEGER,active_root_of_trust TEXT,public_key BLOB,cert_path BLOB,cert_serial INTEGER,secret_types TEXT,counter_id INTEGER,server_params BLOB,UNIQUE(user_id,uid))";
    private static final String SQL_CREATE_ROOT_OF_TRUST_ENTRY = "CREATE TABLE root_of_trust (_id INTEGER PRIMARY KEY,user_id INTEGER,uid INTEGER,root_alias TEST,cert_path BLOB,cert_serial INTEGER,UNIQUE(user_id,uid,root_alias))";
    private static final String SQL_CREATE_USER_METADATA_ENTRY = "CREATE TABLE user_metadata( _id INTEGER PRIMARY KEY,user_id INTEGER UNIQUE,platform_key_generation_id INTEGER)";
    private static final String SQL_DELETE_KEYS_ENTRY = "DROP TABLE IF EXISTS keys";
    private static final String SQL_DELETE_RECOVERY_SERVICE_METADATA_ENTRY = "DROP TABLE IF EXISTS recovery_service_metadata";
    private static final String SQL_DELETE_ROOT_OF_TRUST_ENTRY = "DROP TABLE IF EXISTS root_of_trust";
    private static final String SQL_DELETE_USER_METADATA_ENTRY = "DROP TABLE IF EXISTS user_metadata";
    private static final String TAG = "RecoverableKeyStoreDbHp";

    RecoverableKeyStoreDbHelper(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(SQL_CREATE_KEYS_ENTRY);
        sQLiteDatabase.execSQL(SQL_CREATE_USER_METADATA_ENTRY);
        sQLiteDatabase.execSQL(SQL_CREATE_RECOVERY_SERVICE_METADATA_ENTRY);
        sQLiteDatabase.execSQL(SQL_CREATE_ROOT_OF_TRUST_ENTRY);
    }

    @Override
    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        Log.e(TAG, "Recreating recoverablekeystore after unexpected version downgrade.");
        dropAllKnownTables(sQLiteDatabase);
        onCreate(sQLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 2) {
            dropAllKnownTables(sQLiteDatabase);
            onCreate(sQLiteDatabase);
            return;
        }
        if (i < 3 && i2 >= 3) {
            upgradeDbForVersion3(sQLiteDatabase);
            i = 3;
        }
        if (i < 4 && i2 >= 4) {
            upgradeDbForVersion4(sQLiteDatabase);
            i = 4;
        }
        if (i != i2) {
            Log.e(TAG, "Failed to update recoverablekeystore database to the most recent version");
        }
    }

    private void dropAllKnownTables(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(SQL_DELETE_KEYS_ENTRY);
        sQLiteDatabase.execSQL(SQL_DELETE_USER_METADATA_ENTRY);
        sQLiteDatabase.execSQL(SQL_DELETE_RECOVERY_SERVICE_METADATA_ENTRY);
        sQLiteDatabase.execSQL(SQL_DELETE_ROOT_OF_TRUST_ENTRY);
    }

    private void upgradeDbForVersion3(SQLiteDatabase sQLiteDatabase) {
        addColumnToTable(sQLiteDatabase, "recovery_service_metadata", "cert_path", "BLOB", null);
        addColumnToTable(sQLiteDatabase, "recovery_service_metadata", "cert_serial", "INTEGER", null);
    }

    private void upgradeDbForVersion4(SQLiteDatabase sQLiteDatabase) {
        Log.d(TAG, "Updating recoverable keystore database to version 4");
        sQLiteDatabase.execSQL(SQL_CREATE_ROOT_OF_TRUST_ENTRY);
        addColumnToTable(sQLiteDatabase, "recovery_service_metadata", "active_root_of_trust", "TEXT", null);
    }

    private static void addColumnToTable(SQLiteDatabase sQLiteDatabase, String str, String str2, String str3, String str4) {
        Log.d(TAG, "Adding column " + str2 + " to " + str + ".");
        String str5 = "ALTER TABLE " + str + " ADD COLUMN " + str2 + " " + str3;
        if (str4 != null && !str4.isEmpty()) {
            str5 = str5 + " DEFAULT " + str4;
        }
        sQLiteDatabase.execSQL(str5 + ";");
    }
}
