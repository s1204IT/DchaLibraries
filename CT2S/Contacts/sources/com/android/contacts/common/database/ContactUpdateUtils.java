package com.android.contacts.common.database;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactUpdateUtils {
    private static final String TAG = ContactUpdateUtils.class.getSimpleName();

    public static void setSuperPrimary(Context context, long dataId) {
        if (dataId == -1) {
            Log.e(TAG, "Invalid arguments for setSuperPrimary request");
            return;
        }
        ContentValues values = new ContentValues(2);
        values.put("is_super_primary", (Integer) 1);
        values.put("is_primary", (Integer) 1);
        context.getContentResolver().update(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId), values, null, null);
    }
}
