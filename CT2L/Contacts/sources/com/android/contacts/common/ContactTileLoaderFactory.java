package com.android.contacts.common;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract;

public final class ContactTileLoaderFactory {
    public static final Uri ADN_URI = Uri.parse("content://icc/adn");
    public static final Uri ADN_GRP_URI = Uri.parse("content://icc/grp");
    public static final Uri ADN_URI_SUB = Uri.parse("content://icc/adn/subId");
    public static final Uri ADN_GRP_URI_SUB = Uri.parse("content://icc/grp/subId");
    private static final String[] COLUMNS = {"_id", "display_name", "starred", "photo_uri", "lookup", "contact_presence", "contact_status"};
    public static final String[] COLUMNS_PHONE_ONLY = {"_id", "display_name", "starred", "photo_uri", "lookup", "data1", "data2", "data3", "is_super_primary", "pinned", "contact_id"};

    public static CursorLoader createStrequentLoader(Context context) {
        return new CursorLoader(context, ContactsContract.Contacts.CONTENT_STREQUENT_URI, COLUMNS, null, null, "display_name COLLATE NOCASE ASC");
    }

    public static CursorLoader createStarredLoader(Context context) {
        return new CursorLoader(context, ContactsContract.Contacts.CONTENT_URI, COLUMNS, "starred=?", new String[]{"1"}, "display_name COLLATE NOCASE ASC");
    }

    public static CursorLoader createFrequentLoader(Context context) {
        return new CursorLoader(context, ContactsContract.Contacts.CONTENT_FREQUENT_URI, COLUMNS, "starred=?", new String[]{"0"}, null);
    }
}
