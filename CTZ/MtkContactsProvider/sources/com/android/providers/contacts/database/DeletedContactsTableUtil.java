package com.android.providers.contacts.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.android.providers.contacts.util.Clock;

public class DeletedContactsTableUtil {
    public static void create(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE deleted_contacts (contact_id INTEGER PRIMARY KEY,contact_deleted_timestamp INTEGER NOT NULL default 0);");
        sQLiteDatabase.execSQL(MoreDatabaseUtils.buildCreateIndexSql("deleted_contacts", "contact_deleted_timestamp"));
    }

    public static long insertDeletedContact(SQLiteDatabase sQLiteDatabase, long j) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("contact_id", Long.valueOf(j));
        contentValues.put("contact_deleted_timestamp", Long.valueOf(Clock.getInstance().currentTimeMillis()));
        return sQLiteDatabase.insertWithOnConflict("deleted_contacts", null, contentValues, 5);
    }

    public static int deleteOldLogs(SQLiteDatabase sQLiteDatabase) {
        return sQLiteDatabase.delete("deleted_contacts", "contact_deleted_timestamp < ?", new String[]{(Clock.getInstance().currentTimeMillis() - 2592000000L) + ""});
    }
}
