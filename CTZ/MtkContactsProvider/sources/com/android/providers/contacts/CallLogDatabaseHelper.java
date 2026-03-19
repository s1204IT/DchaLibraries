package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.providers.contacts.util.PropertyUtils;
import com.mediatek.providers.contacts.CallLogProviderEx;

public class CallLogDatabaseHelper {
    private static CallLogDatabaseHelper sInstance;
    private static CallLogDatabaseHelper sInstanceForShadow;
    private final Context mContext;
    private final OpenHelper mOpenHelper;

    private final class OpenHelper extends SQLiteOpenHelper {
        public OpenHelper(Context context, String str, SQLiteDatabase.CursorFactory cursorFactory, int i) {
            super(context, str, cursorFactory, i);
            setIdleConnectionTimeout(30000L);
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            PropertyUtils.createPropertiesTable(sQLiteDatabase);
            sQLiteDatabase.execSQL("CREATE TABLE calls (_id INTEGER PRIMARY KEY AUTOINCREMENT,number TEXT,presentation INTEGER NOT NULL DEFAULT 1,post_dial_digits TEXT NOT NULL DEFAULT '',via_number TEXT NOT NULL DEFAULT '',date INTEGER,duration INTEGER,data_usage INTEGER,type INTEGER,features INTEGER NOT NULL DEFAULT 0,subscription_component_name TEXT,subscription_id TEXT,phone_account_address TEXT,phone_account_hidden INTEGER NOT NULL DEFAULT 0,sub_id INTEGER DEFAULT -1,new INTEGER,name TEXT,numbertype INTEGER,numberlabel TEXT,countryiso TEXT,voicemail_uri TEXT,is_read INTEGER,geocoded_location TEXT,lookup_uri TEXT,matched_number TEXT,normalized_number TEXT,photo_id INTEGER NOT NULL DEFAULT 0,photo_uri TEXT,formatted_number TEXT,add_for_all_users INTEGER NOT NULL DEFAULT 1,last_modified INTEGER DEFAULT 0,_data TEXT,has_content INTEGER,mime_type TEXT,source_data TEXT,source_package TEXT,transcription TEXT,transcription_state INTEGER NOT NULL DEFAULT 0,state INTEGER,dirty INTEGER NOT NULL DEFAULT 0,deleted INTEGER NOT NULL DEFAULT 0,backed_up INTEGER NOT NULL DEFAULT 0,restored INTEGER NOT NULL DEFAULT 0,archived INTEGER NOT NULL DEFAULT 0,is_omtp_voicemail INTEGER NOT NULL DEFAULT 0,conference_call_id INTEGER NOT NULL DEFAULT -1,indicate_phone_or_sim_contact INTEGER NOT NULL DEFAULT -1,is_sdn_contact INTEGER NOT NULL DEFAULT 0);");
            CallLogProviderEx.createConferenceCallsTable(sQLiteDatabase);
            sQLiteDatabase.execSQL("CREATE TABLE voicemail_status (_id INTEGER PRIMARY KEY AUTOINCREMENT,source_package TEXT NOT NULL,phone_account_component_name TEXT,phone_account_id TEXT,settings_uri TEXT,voicemail_access_uri TEXT,configuration_state INTEGER,data_channel_state INTEGER,notification_channel_state INTEGER,quota_occupied INTEGER DEFAULT -1,quota_total INTEGER DEFAULT -1,source_type TEXT);");
            CallLogDatabaseHelper.this.migrateFromLegacyTables(sQLiteDatabase);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            if (i < 2) {
                CallLogDatabaseHelper.this.upgradeToVersion2(sQLiteDatabase);
            }
            if (i < 3) {
                CallLogDatabaseHelper.this.upgradeToVersion3(sQLiteDatabase);
            }
            if (i < 4) {
                CallLogDatabaseHelper.this.upgradeToVersion4(sQLiteDatabase);
            }
            if (i < 5) {
                CallLogDatabaseHelper.this.upgradeToVersion5(sQLiteDatabase);
            }
        }
    }

    @VisibleForTesting
    CallLogDatabaseHelper(Context context, String str) {
        this.mContext = context;
        this.mOpenHelper = new OpenHelper(this.mContext, str, null, 5);
    }

    public static synchronized CallLogDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CallLogDatabaseHelper(context, "calllog.db");
        }
        return sInstance;
    }

    public static synchronized CallLogDatabaseHelper getInstanceForShadow(Context context) {
        if (sInstanceForShadow == null) {
            sInstanceForShadow = new CallLogDatabaseHelper(context.createDeviceProtectedStorageContext(), "calllog_shadow.db");
        }
        return sInstanceForShadow;
    }

    public SQLiteDatabase getReadableDatabase() {
        return this.mOpenHelper.getReadableDatabase();
    }

    public SQLiteDatabase getWritableDatabase() {
        return this.mOpenHelper.getWritableDatabase();
    }

    public String getProperty(String str, String str2) {
        return PropertyUtils.getProperty(getReadableDatabase(), str, str2);
    }

    public void setProperty(String str, String str2) {
        PropertyUtils.setProperty(getWritableDatabase(), str, str2);
    }

    private void upgradeToVersion2(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD via_number TEXT NOT NULL DEFAULT ''");
    }

    private void upgradeToVersion3(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE voicemail_status ADD source_type TEXT");
        sQLiteDatabase.execSQL("ALTER TABLE conference_calls ADD conference_duration INTEGER DEFAULT -1");
        CallLogProviderEx.updateConferenceDuration(sQLiteDatabase);
    }

    private void upgradeToVersion4(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD backed_up INTEGER NOT NULL DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD restored INTEGER NOT NULL DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD archived INTEGER NOT NULL DEFAULT 0");
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD is_omtp_voicemail INTEGER NOT NULL DEFAULT 0");
    }

    private void upgradeToVersion5(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("ALTER TABLE calls ADD transcription_state INTEGER NOT NULL DEFAULT 0");
    }

    private void migrateFromLegacyTables(SQLiteDatabase sQLiteDatabase) {
        SQLiteDatabase contactsWritableDatabaseForMigration = getContactsWritableDatabaseForMigration();
        if (contactsWritableDatabaseForMigration == null) {
            Log.w("CallLogDatabaseHelper", "Contacts DB == null, skipping migration. (running tests?)");
            return;
        }
        if ("1".equals(PropertyUtils.getProperty(sQLiteDatabase, "migrated", ""))) {
            return;
        }
        Log.i("CallLogDatabaseHelper", "Migrating from old tables...");
        contactsWritableDatabaseForMigration.beginTransaction();
        try {
            try {
            } catch (RuntimeException e) {
                Log.w("CallLogDatabaseHelper", "Exception caught during migration", e);
            }
            if (tableExists(contactsWritableDatabaseForMigration, "calls") && tableExists(contactsWritableDatabaseForMigration, "voicemail_status")) {
                sQLiteDatabase.beginTransaction();
                try {
                    ContentValues contentValues = new ContentValues();
                    Throwable th = null;
                    Cursor cursorRawQuery = contactsWritableDatabaseForMigration.rawQuery("SELECT * FROM calls", null);
                    while (cursorRawQuery.moveToNext()) {
                        try {
                            try {
                                contentValues.clear();
                                DatabaseUtils.cursorRowToContentValues(cursorRawQuery, contentValues);
                                contentValues.remove("raw_contact_id");
                                contentValues.remove("data_id");
                                sQLiteDatabase.insertOrThrow("calls", null, contentValues);
                            } finally {
                                if (cursorRawQuery != null) {
                                    $closeResource(th, cursorRawQuery);
                                }
                            }
                        } finally {
                        }
                    }
                    if (cursorRawQuery != null) {
                        $closeResource(null, cursorRawQuery);
                    }
                    Cursor cursorRawQuery2 = contactsWritableDatabaseForMigration.rawQuery("SELECT * FROM voicemail_status", null);
                    while (cursorRawQuery2.moveToNext()) {
                        try {
                            try {
                                contentValues.clear();
                                DatabaseUtils.cursorRowToContentValues(cursorRawQuery2, contentValues);
                                sQLiteDatabase.insertOrThrow("voicemail_status", null, contentValues);
                            } finally {
                            }
                        } finally {
                            if (cursorRawQuery2 != null) {
                                $closeResource(th, cursorRawQuery2);
                            }
                        }
                    }
                    contactsWritableDatabaseForMigration.execSQL("DROP TABLE calls;");
                    contactsWritableDatabaseForMigration.execSQL("DROP TABLE voicemail_status;");
                    if (tableExists(contactsWritableDatabaseForMigration, "conference_calls")) {
                        cursorRawQuery2 = contactsWritableDatabaseForMigration.rawQuery("SELECT * FROM conference_calls ", null);
                        while (cursorRawQuery2.moveToNext()) {
                            try {
                                try {
                                    contentValues.clear();
                                    DatabaseUtils.cursorRowToContentValues(cursorRawQuery2, contentValues);
                                    sQLiteDatabase.insertOrThrow("conference_calls", null, contentValues);
                                } finally {
                                }
                            } finally {
                                if (cursorRawQuery2 != null) {
                                    $closeResource(th, cursorRawQuery2);
                                }
                            }
                        }
                        if (cursorRawQuery2 != null) {
                            $closeResource(null, cursorRawQuery2);
                        }
                        contactsWritableDatabaseForMigration.execSQL("DROP TABLE conference_calls;");
                    }
                    CallLogProviderEx.dropDialerSearchTables(contactsWritableDatabaseForMigration);
                    PropertyUtils.setProperty(sQLiteDatabase, "call_log_last_synced", PropertyUtils.getProperty(contactsWritableDatabaseForMigration, "call_log_last_synced", null));
                    Log.i("CallLogDatabaseHelper", "Migration completed.");
                    sQLiteDatabase.setTransactionSuccessful();
                    sQLiteDatabase.endTransaction();
                    contactsWritableDatabaseForMigration.setTransactionSuccessful();
                    contactsWritableDatabaseForMigration.endTransaction();
                    PropertyUtils.setProperty(sQLiteDatabase, "migrated", "1");
                    return;
                } catch (Throwable th2) {
                    sQLiteDatabase.endTransaction();
                    throw th2;
                }
            }
            Log.i("CallLogDatabaseHelper", "Source tables don't exist.");
        } finally {
            contactsWritableDatabaseForMigration.endTransaction();
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    @VisibleForTesting
    static boolean tableExists(SQLiteDatabase sQLiteDatabase, String str) {
        return DatabaseUtils.longForQuery(sQLiteDatabase, "select count(*) from sqlite_master where type='table' and name=?", new String[]{str}) > 0;
    }

    @VisibleForTesting
    SQLiteDatabase getContactsWritableDatabaseForMigration() {
        return ContactsDatabaseHelper.getInstance(this.mContext).getWritableDatabase();
    }

    public ArraySet<String> selectDistinctColumn(String str, String str2) {
        ArraySet<String> arraySet = new ArraySet<>();
        Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT DISTINCT " + str2 + " FROM " + str, null);
        try {
            cursorRawQuery.moveToPosition(-1);
            while (cursorRawQuery.moveToNext()) {
                if (!cursorRawQuery.isNull(0)) {
                    String string = cursorRawQuery.getString(0);
                    if (!TextUtils.isEmpty(string)) {
                        arraySet.add(string);
                    }
                }
            }
            return arraySet;
        } finally {
            cursorRawQuery.close();
        }
    }

    @VisibleForTesting
    void closeForTest() {
        this.mOpenHelper.close();
    }
}
