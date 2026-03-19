package com.mediatek.providers.contacts;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemProperties;

public class ContactsProviderUtils {
    public static boolean isVolteEnabled() {
        return "1".equals(SystemProperties.get("persist.vendor.volte_support"));
    }

    public static boolean isImsCallEnabled() {
        return "1".equals(SystemProperties.get("persist.vendor.ims_support"));
    }

    public static boolean hasPresenceRawContact(SQLiteDatabase sQLiteDatabase, long j, long j2) {
        return isRawContactPresence(sQLiteDatabase, j) || isRawContactPresence(sQLiteDatabase, j2);
    }

    private static boolean isRawContactPresence(SQLiteDatabase sQLiteDatabase, long j) {
        Cursor cursorQuery = sQLiteDatabase.query("raw_contacts JOIN accounts ON (accounts._id=raw_contacts.account_id)", new String[]{"account_type"}, "raw_contacts._id=?", new String[]{Long.toString(j)}, null, null, null);
        try {
            if (cursorQuery.moveToFirst()) {
                if (AccountUtils.isPresenceAccount(cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("account_type")))) {
                    return true;
                }
            }
            return false;
        } finally {
            cursorQuery.close();
        }
    }
}
