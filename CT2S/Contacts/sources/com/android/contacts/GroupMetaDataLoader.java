package com.android.contacts;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract;

public final class GroupMetaDataLoader extends CursorLoader {
    private static final String[] COLUMNS = {"account_name", "account_type", "data_set", "_id", "title", "auto_add", "favorites", "group_is_read_only", "deleted"};

    public GroupMetaDataLoader(Context context, Uri groupUri) {
        super(context, ensureIsGroupUri(groupUri), COLUMNS, "account_type NOT NULL AND account_name NOT NULL", null, null);
    }

    private static Uri ensureIsGroupUri(Uri groupUri) {
        if (groupUri == null) {
            throw new IllegalArgumentException("Uri must not be null");
        }
        if (!groupUri.toString().startsWith(ContactsContract.Groups.CONTENT_URI.toString())) {
            throw new IllegalArgumentException("Invalid group Uri: " + groupUri);
        }
        return groupUri;
    }
}
