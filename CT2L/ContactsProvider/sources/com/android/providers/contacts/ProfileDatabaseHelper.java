package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.android.providers.contacts.ContactsDatabaseHelper;

public class ProfileDatabaseHelper extends ContactsDatabaseHelper {
    private static ProfileDatabaseHelper sSingleton = null;

    public static ProfileDatabaseHelper getNewInstanceForTest(Context context) {
        return new ProfileDatabaseHelper(context, null, false);
    }

    private ProfileDatabaseHelper(Context context, String databaseName, boolean optimizationEnabled) {
        super(context, databaseName, optimizationEnabled);
    }

    public static synchronized ProfileDatabaseHelper getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new ProfileDatabaseHelper(context, "profile.db", true);
        }
        return sSingleton;
    }

    @Override
    protected int dbForProfile() {
        return 1;
    }

    @Override
    protected void initializeAutoIncrementSequences(SQLiteDatabase db) {
        String[] arr$ = ContactsDatabaseHelper.Tables.SEQUENCE_TABLES;
        for (String table : arr$) {
            ContentValues values = new ContentValues();
            values.put("name", table);
            values.put("seq", (Long) 9223372034707292160L);
            db.insert("sqlite_sequence", null, values);
        }
    }
}
