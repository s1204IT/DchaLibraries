package com.android.providers.contacts.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.android.common.io.MoreCloseables;
import com.android.providers.contacts.util.Clock;
import java.util.Set;

public class ContactsTableUtil {
    public static void createIndexes(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE INDEX contacts_has_phone_index ON contacts (has_phone_number);");
        sQLiteDatabase.execSQL("CREATE INDEX contacts_name_raw_contact_id_index ON contacts (name_raw_contact_id);");
        sQLiteDatabase.execSQL(MoreDatabaseUtils.buildCreateIndexSql("contacts", "contact_last_updated_timestamp"));
    }

    public static void updateContactLastUpdateByContactId(SQLiteDatabase sQLiteDatabase, long j) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("contact_last_updated_timestamp", Long.valueOf(Clock.getInstance().currentTimeMillis()));
        sQLiteDatabase.update("contacts", contentValues, "_id = ?", new String[]{String.valueOf(j)});
    }

    public static void updateContactLastUpdateByRawContactId(SQLiteDatabase sQLiteDatabase, Set<Long> set) {
        if (set.isEmpty()) {
            return;
        }
        sQLiteDatabase.execSQL(buildUpdateLastUpdateSql(set));
    }

    private static String buildUpdateLastUpdateSql(Set<Long> set) {
        return "UPDATE contacts SET contact_last_updated_timestamp = " + Clock.getInstance().currentTimeMillis() + " WHERE _id IN (   SELECT contact_id  FROM raw_contacts  WHERE _id IN (" + TextUtils.join(",", set) + ") )";
    }

    public static int deleteContact(SQLiteDatabase sQLiteDatabase, long j) {
        DeletedContactsTableUtil.insertDeletedContact(sQLiteDatabase, j);
        return sQLiteDatabase.delete("contacts", "_id = ?", new String[]{j + ""});
    }

    public static int deleteContactIfSingleton(SQLiteDatabase sQLiteDatabase, long j) {
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("select contact_id, count(1) from raw_contacts where contact_id =  (select contact_id   from raw_contacts   where _id = ?) group by contact_id", new String[]{j + ""});
        try {
            if (cursorRawQuery.moveToNext()) {
                long j2 = cursorRawQuery.getLong(0);
                if (cursorRawQuery.getLong(1) == 1) {
                    return deleteContact(sQLiteDatabase, j2);
                }
            }
            return 0;
        } finally {
            MoreCloseables.closeQuietly(cursorRawQuery);
        }
    }
}
