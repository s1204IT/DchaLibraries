package com.android.contacts.database;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract;
import com.mediatek.contacts.util.Log;

public class ContactUpdateUtils {
    private static final String TAG = ContactUpdateUtils.class.getSimpleName();

    public static void setSuperPrimary(Context context, long j) {
        if (j == -1) {
            Log.e(TAG, "Invalid arguments for setSuperPrimary request");
            return;
        }
        ContentValues contentValues = new ContentValues(2);
        contentValues.put("is_super_primary", (Integer) 1);
        contentValues.put("is_primary", (Integer) 1);
        context.getContentResolver().update(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, j), contentValues, null, null);
    }
}
