package com.android.providers.contacts.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.android.common.io.MoreCloseables;
import com.android.providers.contacts.util.Clock;
import java.util.Set;

public class ContactsTableUtil {
    public static void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX contacts_has_phone_index ON contacts (has_phone_number);");
        db.execSQL("CREATE INDEX contacts_name_raw_contact_id_index ON contacts (name_raw_contact_id);");
        db.execSQL(MoreDatabaseUtils.buildCreateIndexSql("contacts", "contact_last_updated_timestamp"));
    }

    public static void updateContactLastUpdateByContactId(SQLiteDatabase db, long contactId) {
        ContentValues values = new ContentValues();
        values.put("contact_last_updated_timestamp", Long.valueOf(Clock.getInstance().currentTimeMillis()));
        db.update("contacts", values, "_id = ?", new String[]{String.valueOf(contactId)});
    }

    public static void updateContactLastUpdateByRawContactId(SQLiteDatabase db, Set<Long> rawContactIds) {
        if (!rawContactIds.isEmpty()) {
            db.execSQL(buildUpdateLastUpdateSql(rawContactIds));
        }
    }

    private static String buildUpdateLastUpdateSql(Set<Long> rawContactIds) {
        String sql = "UPDATE contacts SET contact_last_updated_timestamp = " + Clock.getInstance().currentTimeMillis() + " WHERE _id IN (   SELECT contact_id  FROM raw_contacts  WHERE _id IN (" + TextUtils.join(",", rawContactIds) + ") )";
        return sql;
    }

    public static int deleteContact(SQLiteDatabase db, long contactId) {
        DeletedContactsTableUtil.insertDeletedContact(db, contactId);
        return db.delete("contacts", "_id = ?", new String[]{contactId + ""});
    }

    public static void deleteContactIfSingleton(SQLiteDatabase db, long rawContactId) {
        Cursor cursor = db.rawQuery("select contact_id, count(1) from raw_contacts where contact_id =  (select contact_id   from raw_contacts   where _id = ?) group by contact_id", new String[]{rawContactId + ""});
        try {
            if (cursor.moveToNext()) {
                long contactId = cursor.getLong(0);
                long numRawContacts = cursor.getLong(1);
                if (numRawContacts == 1) {
                    deleteContact(db, contactId);
                }
            }
        } finally {
            MoreCloseables.closeQuietly(cursor);
        }
    }
}
