package com.android.providers.contacts.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.android.providers.contacts.util.Clock;

public class DeletedContactsTableUtil {
    public static void create(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE deleted_contacts (contact_id INTEGER PRIMARY KEY,contact_deleted_timestamp INTEGER NOT NULL default 0);");
        db.execSQL(MoreDatabaseUtils.buildCreateIndexSql("deleted_contacts", "contact_deleted_timestamp"));
    }

    public static long insertDeletedContact(SQLiteDatabase db, long contactId) {
        ContentValues values = new ContentValues();
        values.put("contact_id", Long.valueOf(contactId));
        values.put("contact_deleted_timestamp", Long.valueOf(Clock.getInstance().currentTimeMillis()));
        return db.insertWithOnConflict("deleted_contacts", null, values, 5);
    }

    public static int deleteOldLogs(SQLiteDatabase db) {
        long time = Clock.getInstance().currentTimeMillis() - 2592000000L;
        String[] args = {time + ""};
        return db.delete("deleted_contacts", "contact_deleted_timestamp < ?", args);
    }
}
